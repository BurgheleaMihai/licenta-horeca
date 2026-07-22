"""
Serviciul Flask care expune modelele AI ale aplicatiei HoReCa.

Fluxul principal este:
1. validarea datelor primite de la backend;
2. predictia nivelului de trafic;
3. estimarea personalului necesar;
4. calcularea deficitului fata de personalul activ;
5. predictia riscului de intarziere.

Modelele sunt incarcate atomic si sunt protejate de un RLock,
astfel incat predictiile si reantrenarea sa nu foloseasca stari
intermediare sau modele partial actualizate.
"""

from __future__ import annotations

import hmac
import logging
import os
from dataclasses import dataclass
from pathlib import Path
from threading import Lock, RLock
from typing import Any, Mapping, Protocol, Sequence, TypeAlias, cast

import joblib
import numpy as np
import pandas as pd
from dotenv import load_dotenv
from flask import Flask, Response, jsonify, request
from flask_cors import CORS
from waitress import serve

from retrain_models import RetrainingValidationError, retrain_all_models


# ---------------------------------------------------------------------------
# Tipuri folosite de serviciu
# ---------------------------------------------------------------------------

JsonObject: TypeAlias = dict[str, Any]
NumericRecord: TypeAlias = dict[str, int | float]
RouteResponse: TypeAlias = Response | tuple[Response, int]


class PredictiveModel(Protocol):
    """Interfata minima necesara pentru un model incarcat cu joblib."""

    def predict(self, features: pd.DataFrame) -> Any:
        """Returneaza predictiile pentru randurile primite."""


@dataclass(frozen=True)
class ModelBundle:
    """Pastreaza impreuna cele trei modele active ale serviciului."""

    traffic: PredictiveModel
    staff: PredictiveModel
    delay: PredictiveModel


# ---------------------------------------------------------------------------
# Configurare aplicatie si logging
# ---------------------------------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"

TRAFFIC_MODEL_FILE = MODELS_DIR / "traffic_model.pkl"
STAFF_MODEL_FILE = MODELS_DIR / "staff_model.pkl"
DELAY_MODEL_FILE = MODELS_DIR / "delay_model.pkl"

load_dotenv(BASE_DIR / ".env")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-5s %(name)s - %(message)s",
    datefmt="%H:%M:%S",
    force=True,
)
logging.getLogger("waitress").setLevel(logging.WARNING)

app = Flask(__name__)
CORS(app)
app.logger.setLevel(logging.INFO)


# ---------------------------------------------------------------------------
# Structura datelor de intrare
# ---------------------------------------------------------------------------

# Caracteristicile comune modelului de trafic si modelului de personal.
BASE_FEATURE_COLUMNS = [
    "day_of_week",
    "hour",
    "active_orders",
    "occupied_tables",
    "estimated_occupancy",
    "kitchen_load",
    "bar_load",
    "avg_preparation_time",
    "orders_last_30_min",
    "order_age_minutes",
    "item_count",
]

# Personalul activ este trimis de backend si este optional doar pentru
# compatibilitatea temporara cu cereri mai vechi.
ACTIVE_STAFF_FIELDS = [
    "active_waiters",
    "active_kitchen",
    "active_bar",
]

# Modelul de intarziere foloseste atat datele operationale initiale, cat si
# deficitul de personal si rapoartele de incarcare calculate in serviciu.
DELAY_FEATURE_COLUMNS = [
    *BASE_FEATURE_COLUMNS,
    "active_waiters",
    "active_kitchen",
    "active_bar",
    "waiter_deficit",
    "kitchen_deficit",
    "bar_deficit",
    "orders_per_waiter",
    "kitchen_items_per_employee",
    "bar_items_per_employee",
    "occupancy_per_waiter",
]


# ---------------------------------------------------------------------------
# Starea modelelor si sincronizare
# ---------------------------------------------------------------------------

model_lock = RLock()
retraining_lock = Lock()
_active_models: ModelBundle | None = None


def _load_model(model_file: Path) -> PredictiveModel:
    """
    Incarca un model si il marcheaza explicit drept PredictiveModel.

    joblib.load() returneaza Any, iar conversia explicita elimina avertismentele
    IDE fara a modifica obiectul incarcat sau predictiile sale.
    """

    if not model_file.exists():
        raise FileNotFoundError(f"Modelul nu exista: {model_file}")

    return cast(PredictiveModel, joblib.load(model_file))


def reload_models() -> None:
    """
    Incarca toate modelele si le inlocuieste atomic pe cele active.

    Incarcarea se face in afara blocarii, iar schimbarea bundle-ului se face
    intr-o singura operatie. O predictie nu poate vedea doar o parte dintre
    modelele noi.
    """

    global _active_models

    new_bundle = ModelBundle(
        traffic=_load_model(TRAFFIC_MODEL_FILE),
        staff=_load_model(STAFF_MODEL_FILE),
        delay=_load_model(DELAY_MODEL_FILE),
    )

    with model_lock:
        _active_models = new_bundle

    app.logger.info("Modelele AI au fost incarcate.")


def _require_models() -> ModelBundle:
    """
    Returneaza modelele active sau opreste predictia cu o eroare clara.

    Functia elimina posibilitatea ca IDE-ul sa considere modelele None in
    momentul apelului predict().
    """

    models = _active_models
    if models is None:
        raise RuntimeError("Modelele AI nu sunt incarcate.")

    return models


# ---------------------------------------------------------------------------
# Validarea si transformarea datelor
# ---------------------------------------------------------------------------


def read_integer(
    data: Mapping[str, Any],
    field_name: str,
    *,
    minimum: int | None = None,
    maximum: int | None = None,
    default_value: int | None = None,
) -> int:
    """Citeste si valideaza un camp numeric intreg din request."""

    if field_name not in data:
        if default_value is None:
            raise ValueError(f"Campul {field_name} este obligatoriu.")
        value: Any = default_value
    else:
        value = data[field_name]

    # bool este subclasa a lui int in Python, de aceea trebuie respins separat.
    if isinstance(value, bool):
        raise ValueError(f"Campul {field_name} trebuie sa fie un numar intreg.")

    # Acceptam valori precum 3.0, dar nu valori fractionare precum 3.5.
    if isinstance(value, float):
        if not value.is_integer():
            raise ValueError(f"Campul {field_name} trebuie sa fie un numar intreg.")
        value = int(value)

    if not isinstance(value, int):
        raise ValueError(f"Campul {field_name} trebuie sa fie un numar intreg.")

    if minimum is not None and value < minimum:
        raise ValueError(f"Campul {field_name} trebuie sa fie cel putin {minimum}.")

    if maximum is not None and value > maximum:
        raise ValueError(f"Campul {field_name} trebuie sa fie cel mult {maximum}.")

    return value


def validate_and_build_base_input(data: Mapping[str, Any]) -> dict[str, int]:
    """Valideaza cele 11 caracteristici comune modelelor de baza."""

    return {
        "day_of_week": read_integer(data, "day_of_week", minimum=0, maximum=6),
        "hour": read_integer(data, "hour", minimum=0, maximum=23),
        "active_orders": read_integer(data, "active_orders", minimum=0),
        "occupied_tables": read_integer(data, "occupied_tables", minimum=0),
        "estimated_occupancy": read_integer(
            data,
            "estimated_occupancy",
            minimum=0,
            maximum=100,
        ),
        "kitchen_load": read_integer(data, "kitchen_load", minimum=0),
        "bar_load": read_integer(data, "bar_load", minimum=0),
        "avg_preparation_time": read_integer(
            data,
            "avg_preparation_time",
            minimum=0,
        ),
        "orders_last_30_min": read_integer(
            data,
            "orders_last_30_min",
            minimum=0,
        ),
        "order_age_minutes": read_integer(
            data,
            "order_age_minutes",
            minimum=0,
        ),
        "item_count": read_integer(data, "item_count", minimum=0),
    }


def read_active_staff(data: Mapping[str, Any]) -> tuple[int, int, int]:
    """
    Citeste personalul activ.

    Valorile implicite zero pastreaza compatibilitatea cu request-urile vechi.
    Backend-ul actual trimite intotdeauna aceste trei campuri.
    """

    return (
        read_integer(data, "active_waiters", minimum=0, default_value=0),
        read_integer(data, "active_kitchen", minimum=0, default_value=0),
        read_integer(data, "active_bar", minimum=0, default_value=0),
    )


def build_dataframe(
    values: Mapping[str, int | float],
    columns: Sequence[str],
) -> pd.DataFrame:
    """Construieste un DataFrame cu ordinea exacta folosita la antrenare."""

    return pd.DataFrame([dict(values)], columns=list(columns))


def normalize_staff_prediction(raw_prediction: Any) -> tuple[int, int, int]:
    """
    Converteste predictia de personal in trei valori intregi nenegative.

    Modelul poate returna lista, ndarray sau alta structura compatibila NumPy.
    """

    values = np.asarray(raw_prediction, dtype=float).reshape(-1)
    if values.size < 3:
        raise ValueError("Modelul de personal trebuie sa returneze trei valori.")

    recommendations = tuple(max(0, int(round(float(value)))) for value in values[:3])

    return cast(tuple[int, int, int], recommendations)


def build_delay_input(
    base_input: Mapping[str, int],
    active_waiters: int,
    active_kitchen: int,
    active_bar: int,
    recommended_waiters: int,
    recommended_kitchen_staff: int,
    recommended_bar_staff: int,
) -> tuple[NumericRecord, int, int, int]:
    """Construieste cele 21 de caracteristici ale modelului de intarziere."""

    waiter_deficit = recommended_waiters - active_waiters
    kitchen_deficit = recommended_kitchen_staff - active_kitchen
    bar_deficit = recommended_bar_staff - active_bar

    # Impartirea la minimum 1 evita impartirea la zero cand nu exista personal
    # activ in sectia respectiva.
    orders_per_waiter = base_input["active_orders"] / max(active_waiters, 1)
    kitchen_items_per_employee = base_input["kitchen_load"] / max(active_kitchen, 1)
    bar_items_per_employee = base_input["bar_load"] / max(active_bar, 1)
    occupancy_per_waiter = base_input["occupied_tables"] / max(active_waiters, 1)

    delay_values: NumericRecord = {
        **base_input,
        "active_waiters": active_waiters,
        "active_kitchen": active_kitchen,
        "active_bar": active_bar,
        "waiter_deficit": waiter_deficit,
        "kitchen_deficit": kitchen_deficit,
        "bar_deficit": bar_deficit,
        "orders_per_waiter": orders_per_waiter,
        "kitchen_items_per_employee": kitchen_items_per_employee,
        "bar_items_per_employee": bar_items_per_employee,
        "occupancy_per_waiter": occupancy_per_waiter,
    }

    return delay_values, waiter_deficit, kitchen_deficit, bar_deficit


# Modelele sunt incarcate la pornirea serviciului. Daca un fisier lipseste sau
# este incompatibil, serviciul se opreste imediat in loc sa porneasca partial.
reload_models()


# ---------------------------------------------------------------------------
# Endpointuri HTTP
# ---------------------------------------------------------------------------


@app.get("/health")
def health() -> Response:
    """Returneaza starea serviciului si structura modelelor active."""

    with model_lock:
        models_loaded = _active_models is not None

    return jsonify(
        {
            "status": "AI Service is running",
            "modelsLoaded": models_loaded,
            "trafficFeatureCount": len(BASE_FEATURE_COLUMNS),
            "staffFeatureCount": len(BASE_FEATURE_COLUMNS),
            "delayFeatureCount": len(DELAY_FEATURE_COLUMNS),
            "operationalContextFields": ACTIVE_STAFF_FIELDS,
        }
    )


@app.post("/predict/all")
def predict_all() -> RouteResponse:
    """Ruleaza intregul lant de predictie pentru o stare operationala."""

    payload = request.get_json(silent=True)
    if not isinstance(payload, dict):
        return jsonify({"error": "Request body is missing or invalid"}), 400

    try:
        base_input = validate_and_build_base_input(payload)
        active_waiters, active_kitchen, active_bar = read_active_staff(payload)
        base_dataframe = build_dataframe(base_input, BASE_FEATURE_COLUMNS)

        # Ordinea este importanta:
        # 1. trafic;
        # 2. personal recomandat;
        # 3. deficit fata de personalul activ;
        # 4. risc de intarziere.
        with model_lock:
            models = _require_models()

            traffic_prediction = models.traffic.predict(base_dataframe)[0]
            raw_staff_prediction = models.staff.predict(base_dataframe)[0]

            (
                recommended_waiters,
                recommended_kitchen_staff,
                recommended_bar_staff,
            ) = normalize_staff_prediction(raw_staff_prediction)

            (
                delay_input,
                waiter_deficit,
                kitchen_deficit,
                bar_deficit,
            ) = build_delay_input(
                base_input=base_input,
                active_waiters=active_waiters,
                active_kitchen=active_kitchen,
                active_bar=active_bar,
                recommended_waiters=recommended_waiters,
                recommended_kitchen_staff=recommended_kitchen_staff,
                recommended_bar_staff=recommended_bar_staff,
            )

            delay_dataframe = build_dataframe(
                delay_input,
                DELAY_FEATURE_COLUMNS,
            )
            delay_prediction = models.delay.predict(delay_dataframe)[0]

        response: JsonObject = {
            "trafficLevel": str(traffic_prediction),
            "recommendedWaiters": recommended_waiters,
            "recommendedKitchenStaff": recommended_kitchen_staff,
            "recommendedBarStaff": recommended_bar_staff,
            "delayRisk": str(delay_prediction),
        }

        app.logger.info(
            "Predictia AI a fost realizata. "
            "Activ: waiters=%s, kitchen=%s, bar=%s. "
            "Recomandat: waiters=%s, kitchen=%s, bar=%s. "
            "Deficit: waiters=%s, kitchen=%s, bar=%s. "
            "Trafic=%s, risc=%s.",
            active_waiters,
            active_kitchen,
            active_bar,
            recommended_waiters,
            recommended_kitchen_staff,
            recommended_bar_staff,
            waiter_deficit,
            kitchen_deficit,
            bar_deficit,
            traffic_prediction,
            delay_prediction,
        )

        return jsonify(response)

    except ValueError as exception:
        app.logger.warning("Date invalide pentru predictie: %s", exception)
        return jsonify(
            {
                "error": "Invalid prediction data",
                "message": str(exception),
            }
        ), 400

    except Exception as exception:
        app.logger.exception("Eroare la realizarea predictiei.")
        return jsonify(
            {
                "error": "Prediction failed",
                "message": str(exception),
            }
        ), 500


@app.post("/retrain")
def retrain() -> RouteResponse:
    """Reantreneaza modelele, apoi reincarca doar artefactele acceptate."""

    configured_token = os.getenv("RETRAIN_TOKEN")
    if not configured_token:
        return jsonify({"error": "RETRAIN_TOKEN nu este configurat."}), 503

    received_token = request.headers.get("X-Retrain-Token", "")
    if not hmac.compare_digest(received_token, configured_token):
        return jsonify({"error": "Acces neautorizat."}), 401

    if not retraining_lock.acquire(blocking=False):
        return jsonify({"error": "O reantrenare este deja in curs."}), 409

    app.logger.info("A inceput reantrenarea modelelor.")

    try:
        result = cast(JsonObject, retrain_all_models())

        if bool(result.get("modelsReplaced")):
            reload_models()

        app.logger.info("Reantrenarea modelelor s-a incheiat.")
        return jsonify(result)

    except RetrainingValidationError as exception:
        app.logger.warning("Reantrenarea a fost blocata: %s", exception)
        return jsonify(
            {
                "status": "blocked",
                "modelsReplaced": False,
                "message": str(exception),
            }
        ), 400

    except Exception as exception:
        app.logger.exception("Eroare la reantrenarea modelelor.")
        return jsonify(
            {
                "status": "error",
                "modelsReplaced": False,
                "message": str(exception),
            }
        ), 500

    finally:
        retraining_lock.release()


def main() -> None:
    """Porneste serviciul Waitress pentru utilizare locala."""

    host = "127.0.0.1"
    port = 5000

    app.logger.info("AI Service pornit pe http://%s:%s", host, port)
    serve(app, host=host, port=port, threads=4)


if __name__ == "__main__":
    main()
