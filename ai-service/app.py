import hmac
import os
from pathlib import Path
from threading import Lock, RLock

import joblib
import pandas as pd
from dotenv import load_dotenv
from flask import Flask, jsonify, request
from flask_cors import CORS

from retrain_models import (
    RetrainingValidationError,
    retrain_all_models,
)


BASE_DIR = Path(__file__).resolve().parent

load_dotenv(BASE_DIR / ".env")


app = Flask(__name__)
CORS(app)


MODELS_DIR = BASE_DIR / "models"

TRAFFIC_MODEL_FILE = MODELS_DIR / "traffic_model.pkl"
STAFF_MODEL_FILE = MODELS_DIR / "staff_model.pkl"
DELAY_MODEL_FILE = MODELS_DIR / "delay_model.pkl"


FEATURE_COLUMNS = [
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


model_lock = RLock()
retraining_lock = Lock()

traffic_model = None
staff_model = None
delay_model = None


def reload_models():
    global traffic_model
    global staff_model
    global delay_model

    new_traffic_model = joblib.load(
        TRAFFIC_MODEL_FILE
    )

    new_staff_model = joblib.load(
        STAFF_MODEL_FILE
    )

    new_delay_model = joblib.load(
        DELAY_MODEL_FILE
    )

    with model_lock:
        traffic_model = new_traffic_model
        staff_model = new_staff_model
        delay_model = new_delay_model


reload_models()


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "AI Service is running",
        "modelsLoaded": (
                traffic_model is not None
                and staff_model is not None
                and delay_model is not None
        ),
    })


@app.route("/predict/all", methods=["POST"])
def predict_all():
    data = request.get_json(silent=True)

    if data is None:
        return jsonify({
            "error": (
                "Request body is missing or invalid"
            )
        }), 400

    missing_fields = [
        column
        for column in FEATURE_COLUMNS
        if column not in data
    ]

    if missing_fields:
        return jsonify({
            "error": "Missing required fields",
            "missingFields": missing_fields,
        }), 400

    try:
        input_data = pd.DataFrame(
            [data],
            columns=FEATURE_COLUMNS,
        )

        with model_lock:
            traffic_prediction = (
                traffic_model.predict(input_data)[0]
            )

            staff_prediction = (
                staff_model.predict(input_data)[0]
            )

            delay_prediction = (
                delay_model.predict(input_data)[0]
            )

        recommended_waiters = max(
            0,
            int(round(staff_prediction[0])),
        )

        recommended_kitchen_staff = max(
            0,
            int(round(staff_prediction[1])),
        )

        recommended_bar_staff = max(
            0,
            int(round(staff_prediction[2])),
        )

        response = {
            "trafficLevel": str(
                traffic_prediction
            ),
            "recommendedWaiters": (
                recommended_waiters
            ),
            "recommendedKitchenStaff": (
                recommended_kitchen_staff
            ),
            "recommendedBarStaff": (
                recommended_bar_staff
            ),
            "delayRisk": str(
                delay_prediction
            ),
        }

        return jsonify(response)

    except Exception as exception:
        app.logger.exception(
            "Eroare la realizarea predictiei."
        )

        return jsonify({
            "error": "Prediction failed",
            "message": str(exception),
        }), 500


@app.route("/retrain", methods=["POST"])
def retrain():
    configured_token = os.getenv(
        "RETRAIN_TOKEN"
    )

    if not configured_token:
        return jsonify({
            "error": (
                "RETRAIN_TOKEN nu este configurat."
            )
        }), 503

    received_token = request.headers.get(
        "X-Retrain-Token",
        "",
    )

    if not hmac.compare_digest(
            received_token,
            configured_token,
    ):
        return jsonify({
            "error": "Acces neautorizat."
        }), 401

    if not retraining_lock.acquire(blocking=False):
        return jsonify({
            "error": (
                "O reantrenare este deja in curs."
            )
        }), 409

    try:
        result = retrain_all_models()

        if result.get("modelsReplaced"):
            reload_models()

        return jsonify(result)

    except RetrainingValidationError as exception:
        return jsonify({
            "status": "blocked",
            "modelsReplaced": False,
            "message": str(exception),
        }), 400

    except Exception as exception:
        app.logger.exception(
            "Eroare la reantrenarea modelelor."
        )

        return jsonify({
            "status": "error",
            "modelsReplaced": False,
            "message": str(exception),
        }), 500

    finally:
        retraining_lock.release()


if __name__ == "__main__":
    app.run(
        host="127.0.0.1",
        port=5000,
        debug=(
                os.getenv(
                    "FLASK_DEBUG",
                    "false",
                ).lower()
                == "true"
        ),
    )