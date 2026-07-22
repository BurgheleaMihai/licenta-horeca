"""
Reantrenarea controlata a modelelor AI pentru aplicatia HoReCa.

Procesul foloseste inregistrarile etichetate din baza de date si, optional,
datasetul sintetic. Fiecare model candidat este evaluat, comparat cu modelul
curent si salvat numai daca trece criteriile de acceptare. Inlocuirea
artefactelor este atomica si include backup, metadate si rapoarte.
"""

from __future__ import annotations

import json
import math
import os
import shutil
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Mapping, Protocol, Sequence, TypeAlias, cast

import joblib
import mysql.connector
import numpy as np
import pandas as pd
from dotenv import load_dotenv
from sklearn.base import clone
from sklearn.ensemble import (
    ExtraTreesClassifier,
    GradientBoostingClassifier,
    HistGradientBoostingClassifier,
    RandomForestClassifier,
)
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    log_loss,
    matthews_corrcoef,
    mean_absolute_error,
    mean_squared_error,
    median_absolute_error,
    precision_score,
    r2_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import (
    KFold,
    StratifiedKFold,
    cross_validate,
    train_test_split,
)
from staff_model_architecture import RoleSpecificStaffRegressor

JsonObject: TypeAlias = dict[str, Any]
Metrics: TypeAlias = dict[str, Any]


class TrainablePredictor(Protocol):
    """Interfata comuna necesara modelelor scikit-learn folosite aici."""

    def fit(self, features: Any, target: Any) -> "TrainablePredictor":
        """Antreneaza modelul si returneaza estimatorul curent."""

    def predict(self, features: Any) -> Any:
        """Returneaza predictiile pentru datele primite."""


CandidateModels: TypeAlias = dict[str, TrainablePredictor]


def clone_model(model: TrainablePredictor) -> TrainablePredictor:
    """Cloneaza un estimator pastrand tipul necesar pentru analiza statica."""

    return cast(TrainablePredictor, clone(model))


BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")

DATA_FILE = BASE_DIR / "data" / "synthetic_horeca_dataset.csv"
MODELS_DIR = BASE_DIR / "models"
REPORTS_DIR = BASE_DIR / "reports"
BACKUPS_DIR = MODELS_DIR / "backups"

TRAFFIC_MODEL_FILE = MODELS_DIR / "traffic_model.pkl"
STAFF_MODEL_FILE = MODELS_DIR / "staff_model.pkl"
DELAY_MODEL_FILE = MODELS_DIR / "delay_model.pkl"

TRAFFIC_METADATA_FILE = MODELS_DIR / "traffic_model_metadata.json"
STAFF_METADATA_FILE = MODELS_DIR / "staff_model_metadata.json"
DELAY_METADATA_FILE = MODELS_DIR / "delay_model_metadata.json"

TRAFFIC_REPORT_FILE = REPORTS_DIR / "traffic_metrics.txt"
STAFF_REPORT_FILE = REPORTS_DIR / "staff_metrics.txt"
DELAY_REPORT_FILE = REPORTS_DIR / "delay_metrics.txt"

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

STAFF_TARGET_COLUMNS = [
    "actual_waiters",
    "actual_kitchen_staff",
    "actual_bar_staff",
]

ACTIVE_STAFF_COLUMNS = [
    "active_waiters",
    "active_kitchen",
    "active_bar",
]

DELAY_FEATURE_COLUMNS = BASE_FEATURE_COLUMNS + [
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

TRAFFIC_TARGET = "observed_traffic_level"
DELAY_TARGET = "observed_delay_risk"
CLASS_LABELS = ["SCAZUT", "MEDIU", "RIDICAT"]
PROBABILITY_LABELS = sorted(CLASS_LABELS)

RANDOM_STATE = 42
TEST_SIZE = float(os.getenv("RETRAIN_TEST_SIZE", "0.20"))
MIN_LABELED_RECORDS = int(os.getenv("MIN_LABELED_RECORDS", "30"))
MIN_CLASS_RECORDS = int(os.getenv("MIN_CLASS_RECORDS", "5"))
REAL_DATA_REPEAT = max(1, int(os.getenv("REAL_DATA_REPEAT", "5")))
INCLUDE_SYNTHETIC_DATA = os.getenv(
    "INCLUDE_SYNTHETIC_RETRAINING_DATA",
    "true",
).strip().lower() in {"1", "true", "yes", "da"}
SYNTHETIC_MAX_ROWS = int(os.getenv("SYNTHETIC_MAX_ROWS", "5000"))

CLASSIFIER_F1_TOLERANCE = float(os.getenv("CLASSIFIER_F1_TOLERANCE", "0.02"))
CLASSIFIER_BALANCED_ACCURACY_TOLERANCE = float(
    os.getenv("CLASSIFIER_BALANCED_ACCURACY_TOLERANCE", "0.02")
)
REGRESSOR_MAE_TOLERANCE = float(os.getenv("REGRESSOR_MAE_TOLERANCE", "0.10"))
REGRESSOR_R2_TOLERANCE = float(os.getenv("REGRESSOR_R2_TOLERANCE", "0.08"))

MIN_CLASSIFIER_MACRO_F1 = float(os.getenv("MIN_CLASSIFIER_MACRO_F1", "0.55"))
MIN_CLASSIFIER_BALANCED_ACCURACY = float(
    os.getenv("MIN_CLASSIFIER_BALANCED_ACCURACY", "0.55")
)
MAX_STAFF_MAE = float(os.getenv("MAX_STAFF_MAE", "1.00"))
MIN_STAFF_R2 = float(os.getenv("MIN_STAFF_R2", "0.20"))
CALIBRATION_BINS = 10


class RetrainingValidationError(Exception):
    """Raised when the labeled data cannot safely be used for retraining."""


COLUMN_ALIASES = {
    "orders_last30min": "orders_last_30_min",
    "traffic_level": TRAFFIC_TARGET,
    "delay_risk": DELAY_TARGET,
    "recommended_waiters": "actual_waiters",
    "recommended_kitchen_staff": "actual_kitchen_staff",
    "recommended_bar_staff": "actual_bar_staff",
    "active_kitchen_staff": "active_kitchen",
    "active_bar_staff": "active_bar",
}


# ---------------------------------------------------------------------------
# Acces la date
# ---------------------------------------------------------------------------


def get_database_connection():
    """Deschide conexiunea MySQL folosind variabilele de mediu."""
    database_password = os.getenv("DB_PASSWORD")

    if not database_password:
        raise RetrainingValidationError("Variabila DB_PASSWORD nu este configurata.")

    return mysql.connector.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "3306")),
        database=os.getenv("DB_NAME", "horeca_db"),
        user=os.getenv("DB_USER", "root"),
        password=database_password,
    )


def normalize_column_names(dataframe):
    """Normalizeaza denumirile coloanelor si aplica aliasurile."""
    renamed_columns = {}

    for column in dataframe.columns:
        normalized = str(column).strip().lower()
        renamed_columns[column] = COLUMN_ALIASES.get(normalized, normalized)

    return dataframe.rename(columns=renamed_columns)


def load_labeled_records():
    """Citeste inregistrarile etichetate din baza de date."""
    query = """
        SELECT *
        FROM decision_training_records
        WHERE labeled_at IS NOT NULL
        ORDER BY created_at
    """

    connection = None
    cursor = None

    try:
        connection = get_database_connection()
        cursor = connection.cursor(dictionary=True)
        cursor.execute(query)
        rows = cursor.fetchall()
        return normalize_column_names(pd.DataFrame(rows))
    finally:
        if cursor is not None:
            cursor.close()

        if connection is not None and connection.is_connected():
            connection.close()


def load_synthetic_records() -> pd.DataFrame:
    """Incarca optional datasetul sintetic si limiteaza numarul de randuri."""

    if not INCLUDE_SYNTHETIC_DATA or not DATA_FILE.exists():
        return pd.DataFrame()

    dataframe = normalize_column_names(pd.read_csv(DATA_FILE))

    if 0 < SYNTHETIC_MAX_ROWS < len(dataframe):
        dataframe = dataframe.sample(
            n=SYNTHETIC_MAX_ROWS,
            random_state=RANDOM_STATE,
        )

    dataframe = dataframe.copy()
    dataframe["data_source"] = "synthetic"
    return dataframe


# ---------------------------------------------------------------------------
# Validarea si pregatirea datelor
# ---------------------------------------------------------------------------


def validate_classification_target(target, target_name):
    """Valideaza distributia unei tinte de clasificare."""
    class_counts = target.value_counts()

    missing_classes = [
        label for label in CLASS_LABELS if label not in class_counts.index
    ]

    if missing_classes:
        raise RetrainingValidationError(
            f"Pentru {target_name} lipsesc clasele: {missing_classes}."
        )

    insufficient_classes = {
        label: int(class_counts[label])
        for label in CLASS_LABELS
        if class_counts[label] < MIN_CLASS_RECORDS
    }

    if insufficient_classes:
        raise RetrainingValidationError(
            f"Pentru {target_name}, fiecare clasa trebuie sa aiba cel putin "
            f"{MIN_CLASS_RECORDS} exemple. Clase insuficiente: "
            f"{insufficient_classes}."
        )

    test_records = math.ceil(len(target) * TEST_SIZE)

    if test_records < len(CLASS_LABELS):
        raise RetrainingValidationError(
            f"Nu exista suficiente date pentru evaluarea stratificata a "
            f"modelului de {target_name}."
        )


def prepare_real_records(dataframe):
    """Curata si valideaza inregistrarile reale etichetate."""
    if dataframe.empty:
        raise RetrainingValidationError(
            "Nu exista inregistrari etichetate in baza de date."
        )

    required_columns = (
        BASE_FEATURE_COLUMNS + STAFF_TARGET_COLUMNS + [TRAFFIC_TARGET, DELAY_TARGET]
    )

    missing_columns = [
        column for column in required_columns if column not in dataframe.columns
    ]

    if missing_columns:
        raise RetrainingValidationError(
            "Tabelul decision_training_records nu contine coloanele "
            f"obligatorii: {missing_columns}."
        )

    dataframe = dataframe.copy()

    numeric_columns = list(
        dict.fromkeys(
            BASE_FEATURE_COLUMNS
            + STAFF_TARGET_COLUMNS
            + [column for column in ACTIVE_STAFF_COLUMNS if column in dataframe.columns]
        )
    )

    for column in numeric_columns:
        dataframe[column] = pd.to_numeric(
            dataframe[column],
            errors="coerce",
        )

    dataframe[TRAFFIC_TARGET] = (
        dataframe[TRAFFIC_TARGET].astype(str).str.strip().str.upper()
    )
    dataframe[DELAY_TARGET] = (
        dataframe[DELAY_TARGET].astype(str).str.strip().str.upper()
    )

    dataframe = dataframe.dropna(subset=required_columns).copy()

    if len(dataframe) < MIN_LABELED_RECORDS:
        raise RetrainingValidationError(
            f"Sunt necesare cel putin {MIN_LABELED_RECORDS} inregistrari "
            f"etichetate. Momentan exista {len(dataframe)}."
        )

    invalid_traffic = sorted(set(dataframe[TRAFFIC_TARGET]) - set(CLASS_LABELS))
    invalid_delay = sorted(set(dataframe[DELAY_TARGET]) - set(CLASS_LABELS))

    if invalid_traffic:
        raise RetrainingValidationError(
            f"Exista niveluri de trafic invalide: {invalid_traffic}."
        )

    if invalid_delay:
        raise RetrainingValidationError(
            f"Exista niveluri de risc invalide: {invalid_delay}."
        )

    negative_targets = [
        column for column in STAFF_TARGET_COLUMNS if (dataframe[column] < 0).any()
    ]

    if negative_targets:
        raise RetrainingValidationError(
            "Valorile reale de personal nu pot fi negative. Coloane: "
            f"{negative_targets}."
        )

    validate_classification_target(
        dataframe[TRAFFIC_TARGET],
        "trafic",
    )
    validate_classification_target(
        dataframe[DELAY_TARGET],
        "risc de intarziere",
    )

    active_source = "stored_active_staff"

    for active_column, fallback_column in zip(
        ACTIVE_STAFF_COLUMNS,
        STAFF_TARGET_COLUMNS,
    ):
        if active_column not in dataframe.columns:
            dataframe[active_column] = dataframe[fallback_column]
            active_source = "actual_staff_fallback"
        else:
            dataframe[active_column] = dataframe[active_column].fillna(
                dataframe[fallback_column]
            )

    dataframe["data_source"] = "real"
    dataframe.reset_index(drop=True, inplace=True)

    return dataframe, active_source


def prepare_synthetic_records(dataframe):
    """Normalizeaza datele sintetice pentru reantrenare."""
    if dataframe.empty:
        return dataframe

    required_columns = (
        BASE_FEATURE_COLUMNS
        + STAFF_TARGET_COLUMNS
        + ACTIVE_STAFF_COLUMNS
        + [TRAFFIC_TARGET, DELAY_TARGET]
    )

    missing_columns = [
        column for column in required_columns if column not in dataframe.columns
    ]

    if missing_columns:
        raise RetrainingValidationError(
            "Datasetul sintetic nu contine coloanele necesare pentru "
            f"reantrenare: {missing_columns}."
        )

    dataframe = dataframe.copy()

    for column in BASE_FEATURE_COLUMNS + STAFF_TARGET_COLUMNS + ACTIVE_STAFF_COLUMNS:
        dataframe[column] = pd.to_numeric(dataframe[column], errors="coerce")

    dataframe[TRAFFIC_TARGET] = (
        dataframe[TRAFFIC_TARGET].astype(str).str.strip().str.upper()
    )
    dataframe[DELAY_TARGET] = (
        dataframe[DELAY_TARGET].astype(str).str.strip().str.upper()
    )

    dataframe = dataframe.dropna(subset=required_columns).copy()
    dataframe.reset_index(drop=True, inplace=True)
    return dataframe


def repeat_real_training_rows(dataframe):
    """Creste ponderea datelor reale in setul de antrenare."""
    if REAL_DATA_REPEAT <= 1:
        return dataframe.copy()

    return pd.concat(
        [dataframe.copy() for _ in range(REAL_DATA_REPEAT)],
        ignore_index=True,
    )


def build_training_frame(synthetic_data, real_training_data):
    """Combina si amesteca datele reale cu cele sintetice."""
    frames = []

    if not synthetic_data.empty:
        frames.append(synthetic_data.copy())

    frames.append(repeat_real_training_rows(real_training_data))

    combined = pd.concat(frames, ignore_index=True)
    return combined.sample(
        frac=1.0,
        random_state=RANDOM_STATE,
    ).reset_index(drop=True)


# ---------------------------------------------------------------------------
# Modele candidate
# ---------------------------------------------------------------------------


def build_traffic_candidates() -> CandidateModels:
    """Construieste candidatii modelului de trafic."""

    # Conversia este folosita doar pentru analiza statica din IntelliJ.
    # Estimatorii scikit-learn implementeaza metodele fit() si predict()
    # cerute de protocolul TrainablePredictor.
    return cast(
        CandidateModels,
        {
            "RandomForestClassifier": RandomForestClassifier(
                n_estimators=250,
                max_depth=14,
                min_samples_leaf=2,
                class_weight="balanced",
                random_state=RANDOM_STATE,
                n_jobs=-1,
            ),
            "ExtraTreesClassifier": ExtraTreesClassifier(
                n_estimators=250,
                max_depth=14,
                min_samples_leaf=2,
                class_weight="balanced",
                random_state=RANDOM_STATE,
                n_jobs=-1,
            ),
            "GradientBoostingClassifier": GradientBoostingClassifier(
                n_estimators=180,
                learning_rate=0.05,
                max_depth=3,
                min_samples_leaf=2,
                random_state=RANDOM_STATE,
            ),
        },
    )


def build_staff_candidates() -> CandidateModels:
    """Pastreaza arhitectura specializata a modelului de personal."""

    # Reantrenarea foloseste aceeasi arhitectura specializata ca antrenarea
    # initiala. Un model generic nu poate inlocui accidental varianta stabila
    # pentru ospatari, bucatarie si bar.
    return cast(
        CandidateModels,
        {
            "RoleSpecificStaffRegressor": RoleSpecificStaffRegressor(
                random_state=RANDOM_STATE,
            ),
        },
    )


def build_delay_candidates() -> CandidateModels:
    """Construieste candidatii modelului de intarziere."""

    return cast(
        CandidateModels,
        {
            "RandomForestClassifier": RandomForestClassifier(
                n_estimators=350,
                max_depth=14,
                min_samples_leaf=2,
                class_weight="balanced",
                random_state=RANDOM_STATE,
                n_jobs=-1,
            ),
            "ExtraTreesClassifier": ExtraTreesClassifier(
                n_estimators=350,
                max_depth=14,
                min_samples_leaf=2,
                class_weight="balanced",
                random_state=RANDOM_STATE,
                n_jobs=-1,
            ),
            "GradientBoostingClassifier": GradientBoostingClassifier(
                n_estimators=220,
                learning_rate=0.05,
                max_depth=3,
                min_samples_leaf=2,
                random_state=RANDOM_STATE,
            ),
            "HistGradientBoostingClassifier": HistGradientBoostingClassifier(
                learning_rate=0.08,
                max_iter=250,
                max_depth=8,
                min_samples_leaf=20,
                random_state=RANDOM_STATE,
            ),
        },
    )


# ---------------------------------------------------------------------------
# Metrici si selectie de modele
# ---------------------------------------------------------------------------


def align_probabilities(
    model: Any,
    features: pd.DataFrame,
) -> np.ndarray | None:
    """Aliniaza probabilitatile modelului la ordinea claselor proiectului."""

    predict_proba = getattr(model, "predict_proba", None)
    if not callable(predict_proba):
        return None

    raw_probabilities = np.asarray(predict_proba(features), dtype=float)
    model_classes = [str(label) for label in model.classes_]
    aligned = np.zeros(
        (len(features), len(PROBABILITY_LABELS)),
        dtype=float,
    )

    for target_index, label in enumerate(PROBABILITY_LABELS):
        if label not in model_classes:
            raise ValueError(f"Modelul nu contine clasa obligatorie: {label}.")

        source_index = model_classes.index(label)
        aligned[:, target_index] = raw_probabilities[:, source_index]

    return aligned


def calculate_expected_calibration_error(
    expected_values: Sequence[Any] | pd.Series,
    predictions: Sequence[Any] | np.ndarray,
    probabilities: np.ndarray,
    number_of_bins: int = CALIBRATION_BINS,
) -> tuple[float, list[JsonObject]]:
    """Calculeaza Expected Calibration Error si detaliile intervalelor."""

    confidences = np.asarray(probabilities.max(axis=1), dtype=float)
    correctness = np.asarray(
        np.equal(
            np.asarray(expected_values, dtype=object),
            np.asarray(predictions, dtype=object),
        ),
        dtype=bool,
    )
    bin_edges = np.linspace(0.0, 1.0, number_of_bins + 1)
    calibration_error = 0.0
    bin_details: list[JsonObject] = []

    for bin_index in range(number_of_bins):
        lower_bound = float(bin_edges[bin_index])
        upper_bound = float(bin_edges[bin_index + 1])

        if bin_index == 0:
            in_bin = np.asarray(
                (confidences >= lower_bound) & (confidences <= upper_bound),
                dtype=bool,
            )
        else:
            in_bin = np.asarray(
                (confidences > lower_bound) & (confidences <= upper_bound),
                dtype=bool,
            )

        count = int(np.count_nonzero(in_bin))
        if count == 0:
            continue

        bin_accuracy = float(np.mean(correctness[in_bin]))
        bin_confidence = float(np.mean(confidences[in_bin]))
        weight = count / len(confidences)
        calibration_error += weight * abs(bin_accuracy - bin_confidence)

        bin_details.append(
            {
                "lowerBound": lower_bound,
                "upperBound": upper_bound,
                "count": count,
                "accuracy": bin_accuracy,
                "averageConfidence": bin_confidence,
            }
        )

    return float(calibration_error), bin_details


def calculate_multiclass_brier_score(
    expected_values: Sequence[Any] | pd.Series,
    probabilities: np.ndarray,
) -> float:
    """Calculeaza scorul Brier pentru clasificarea multiclass."""

    label_to_index = {label: index for index, label in enumerate(PROBABILITY_LABELS)}
    one_hot_expected = np.zeros_like(probabilities, dtype=float)

    for row_index, label in enumerate(expected_values):
        one_hot_expected[
            row_index,
            label_to_index[str(label)],
        ] = 1.0

    squared_errors = np.square(probabilities - one_hot_expected)
    return float(np.mean(np.sum(squared_errors, axis=1)))


def calculate_classifier_metrics(
    model: Any,
    features: pd.DataFrame,
    expected_values: pd.Series,
) -> Metrics:
    """Calculeaza metricile complete pentru un clasificator."""

    predictions = np.asarray(model.predict(features), dtype=object)
    probabilities = align_probabilities(model, features)

    report_output = classification_report(
        expected_values,
        predictions,
        labels=CLASS_LABELS,
        output_dict=True,
        zero_division=0,
    )
    report_dictionary = cast(dict[str, Any], report_output)

    class_metrics: JsonObject = {}
    for label in CLASS_LABELS:
        label_metrics = cast(
            Mapping[str, Any],
            report_dictionary[label],
        )
        class_metrics[label] = {
            "precision": float(label_metrics["precision"]),
            "recall": float(label_metrics["recall"]),
            "f1": float(label_metrics["f1-score"]),
            "support": int(label_metrics["support"]),
        }

    matrix = confusion_matrix(
        expected_values,
        predictions,
        labels=CLASS_LABELS,
    )
    normalized_matrix = confusion_matrix(
        expected_values,
        predictions,
        labels=CLASS_LABELS,
        normalize="true",
    )

    label_positions = {label: index for index, label in enumerate(CLASS_LABELS)}
    expected_positions = np.asarray(
        [label_positions[str(label)] for label in expected_values],
        dtype=int,
    )
    predicted_positions = np.asarray(
        [label_positions[str(label)] for label in predictions],
        dtype=int,
    )
    severe_error_mask = np.asarray(
        np.abs(expected_positions - predicted_positions) == 2,
        dtype=bool,
    )

    metrics: Metrics = {
        "accuracy": float(accuracy_score(expected_values, predictions)),
        "balancedAccuracy": float(
            balanced_accuracy_score(
                expected_values,
                predictions,
            )
        ),
        "macroPrecision": float(
            precision_score(
                expected_values,
                predictions,
                labels=CLASS_LABELS,
                average="macro",
                zero_division=0,
            )
        ),
        "macroRecall": float(
            recall_score(
                expected_values,
                predictions,
                labels=CLASS_LABELS,
                average="macro",
                zero_division=0,
            )
        ),
        "macroF1": float(
            f1_score(
                expected_values,
                predictions,
                labels=CLASS_LABELS,
                average="macro",
                zero_division=0,
            )
        ),
        "weightedF1": float(
            f1_score(
                expected_values,
                predictions,
                labels=CLASS_LABELS,
                average="weighted",
                zero_division=0,
            )
        ),
        "matthewsCorrelationCoefficient": float(
            matthews_corrcoef(
                expected_values,
                predictions,
            )
        ),
        "classMetrics": class_metrics,
        "confusionMatrix": matrix.tolist(),
        "normalizedConfusionMatrix": normalized_matrix.tolist(),
        "severeErrors": {
            "count": int(np.count_nonzero(severe_error_mask)),
            "rate": float(np.mean(severe_error_mask)),
        },
    }

    if probabilities is not None:
        expected_calibration_error, calibration_bins = (
            calculate_expected_calibration_error(
                expected_values,
                predictions,
                probabilities,
            )
        )
        confidence_values = np.asarray(
            probabilities.max(axis=1),
            dtype=float,
        )

        metrics.update(
            {
                "rocAucMacroOvr": float(
                    roc_auc_score(
                        expected_values,
                        probabilities,
                        labels=PROBABILITY_LABELS,
                        multi_class="ovr",
                        average="macro",
                    )
                ),
                "rocAucWeightedOvr": float(
                    roc_auc_score(
                        expected_values,
                        probabilities,
                        labels=PROBABILITY_LABELS,
                        multi_class="ovr",
                        average="weighted",
                    )
                ),
                "logLoss": float(
                    log_loss(
                        expected_values,
                        probabilities,
                        labels=PROBABILITY_LABELS,
                    )
                ),
                "multiclassBrierScore": (
                    calculate_multiclass_brier_score(
                        expected_values,
                        probabilities,
                    )
                ),
                "expectedCalibrationError": expected_calibration_error,
                "averageConfidence": float(np.mean(confidence_values)),
                "calibrationBins": calibration_bins,
            }
        )

    return metrics


def calculate_regression_metrics(
    expected_values: pd.DataFrame | np.ndarray,
    predictions: np.ndarray,
) -> Metrics:
    """Calculeaza metrici generale si pe fiecare rol."""

    expected_array = np.asarray(expected_values, dtype=float)
    prediction_array = np.asarray(predictions, dtype=float)
    absolute_errors = np.abs(expected_array - prediction_array)

    overall_mae = float(mean_absolute_error(expected_array, prediction_array))
    overall_rmse = float(mean_squared_error(expected_array, prediction_array) ** 0.5)
    overall_r2 = float(
        r2_score(
            expected_array,
            prediction_array,
            multioutput="uniform_average",
        )
    )
    overall_median_ae = float(
        median_absolute_error(
            expected_array,
            prediction_array,
            multioutput="uniform_average",
        )
    )

    mae_by_target = np.asarray(
        mean_absolute_error(
            expected_array,
            prediction_array,
            multioutput="raw_values",
        ),
        dtype=float,
    )
    rmse_by_target = np.sqrt(
        np.asarray(
            mean_squared_error(
                expected_array,
                prediction_array,
                multioutput="raw_values",
            ),
            dtype=float,
        )
    )
    r2_by_target = np.asarray(
        r2_score(
            expected_array,
            prediction_array,
            multioutput="raw_values",
        ),
        dtype=float,
    )

    rounded_expected = np.rint(expected_array).astype(int)
    rounded_predictions = np.maximum(
        0,
        np.rint(prediction_array).astype(int),
    )
    rounded_errors = rounded_predictions - rounded_expected
    absolute_rounded_errors = np.abs(rounded_errors)

    per_target: JsonObject = {}
    for index, target_name in enumerate(STAFF_TARGET_COLUMNS):
        target_errors = absolute_rounded_errors[:, index]
        per_target[target_name] = {
            "mae": float(mae_by_target[index]),
            "rmse": float(rmse_by_target[index]),
            "r2": float(r2_by_target[index]),
            "exactRoundedRate": float(np.mean(target_errors == 0)),
            "withinOneEmployeeRate": float(np.mean(target_errors <= 1)),
            "underestimateRate": float(np.mean(rounded_errors[:, index] < 0)),
            "overestimateRate": float(np.mean(rounded_errors[:, index] > 0)),
            "maximumRoundedError": int(np.max(target_errors)),
        }

    return {
        "overallMae": overall_mae,
        "overallRmse": overall_rmse,
        "overallR2": overall_r2,
        "overallMedianAbsoluteError": overall_median_ae,
        "exactRoundedRate": float(np.mean(absolute_rounded_errors == 0)),
        "withinOneEmployeeRate": float(np.mean(absolute_rounded_errors <= 1)),
        "allOutputsExactRate": float(
            np.mean(
                np.all(
                    absolute_rounded_errors == 0,
                    axis=1,
                )
            )
        ),
        "underestimateRate": float(np.mean(rounded_errors < 0)),
        "overestimateRate": float(np.mean(rounded_errors > 0)),
        "maximumRoundedError": int(np.max(absolute_rounded_errors)),
        "meanAbsoluteErrorPerCell": float(np.mean(absolute_errors)),
        "perTarget": per_target,
    }


def safe_current_classifier_metrics(
    model: Any | None,
    features: pd.DataFrame,
    expected_values: pd.Series,
) -> tuple[Metrics | None, str | None]:
    """Evalueaza modelul curent fara a opri intreaga reantrenare."""

    if model is None:
        return None, None

    try:
        return (
            calculate_classifier_metrics(
                model,
                features,
                expected_values,
            ),
            None,
        )
    except Exception as caught_error:
        return None, str(caught_error)


def safe_current_regression_metrics(
    model: Any | None,
    features: pd.DataFrame,
    expected_values: pd.DataFrame,
) -> tuple[Metrics | None, str | None]:
    """Evalueaza regresorul curent fara a opri intreaga reantrenare."""

    if model is None:
        return None, None

    try:
        predictions = np.asarray(
            model.predict(features),
            dtype=float,
        )
        return (
            calculate_regression_metrics(
                expected_values,
                predictions,
            ),
            None,
        )
    except Exception as caught_error:
        return None, str(caught_error)


def select_classifier_candidate(
    candidates: CandidateModels,
    training_features: pd.DataFrame,
    training_target: pd.Series,
    validation_features: pd.DataFrame,
    validation_target: pd.Series,
) -> tuple[str, TrainablePredictor, dict[str, Metrics]]:
    """Antreneaza candidatii si selecteaza cel mai bun clasificator."""

    candidate_results: dict[str, Metrics] = {}
    best_name: str | None = None
    best_model: TrainablePredictor | None = None
    best_score: tuple[float, float, float, float] | None = None

    for model_name, candidate_model in candidates.items():
        candidate_model.fit(
            training_features,
            training_target,
        )
        candidate_metrics = calculate_classifier_metrics(
            candidate_model,
            validation_features,
            validation_target,
        )
        candidate_results[model_name] = candidate_metrics

        candidate_score = (
            float(candidate_metrics["macroF1"]),
            float(candidate_metrics["balancedAccuracy"]),
            float(candidate_metrics["matthewsCorrelationCoefficient"]),
            float(candidate_metrics["accuracy"]),
        )

        if best_score is None or candidate_score > best_score:
            best_score = candidate_score
            best_name = model_name
            best_model = candidate_model

    if best_name is None or best_model is None:
        raise RetrainingValidationError(
            "Nu a putut fi selectat niciun clasificator candidat."
        )

    return best_name, best_model, candidate_results


def select_staff_candidate(
    candidates: CandidateModels,
    training_features: pd.DataFrame,
    training_target: pd.DataFrame,
    validation_features: pd.DataFrame,
    validation_target: pd.DataFrame,
) -> tuple[str, TrainablePredictor, dict[str, Metrics]]:
    """Antreneaza si selecteaza modelul de personal."""

    candidate_results: dict[str, Metrics] = {}
    best_name: str | None = None
    best_model: TrainablePredictor | None = None
    best_score: tuple[float, float, float, float] | None = None

    for model_name, candidate_model in candidates.items():
        candidate_model.fit(
            training_features,
            training_target,
        )
        predictions = np.asarray(
            candidate_model.predict(validation_features),
            dtype=float,
        )
        candidate_metrics = calculate_regression_metrics(
            validation_target,
            predictions,
        )
        candidate_results[model_name] = candidate_metrics

        candidate_score = (
            -float(candidate_metrics["overallMae"]),
            -float(candidate_metrics["overallRmse"]),
            float(candidate_metrics["overallR2"]),
            float(candidate_metrics["allOutputsExactRate"]),
        )

        if best_score is None or candidate_score > best_score:
            best_score = candidate_score
            best_name = model_name
            best_model = candidate_model

    if best_name is None or best_model is None:
        raise RetrainingValidationError("Nu a putut fi selectat modelul de personal.")

    return best_name, best_model, candidate_results


# ---------------------------------------------------------------------------
# Cross-validation si predictii out-of-fold
# ---------------------------------------------------------------------------


def calculate_classifier_cross_validation(
    model: TrainablePredictor,
    features: pd.DataFrame,
    target: pd.Series,
) -> JsonObject:
    """Calculeaza performanta si generalizarea clasificatorului."""

    smallest_class = int(target.value_counts().min())
    folds = min(5, smallest_class)

    if folds < 2:
        return {
            "available": False,
            "reason": "Nu exista suficiente exemple pe clasa.",
        }

    splitter = StratifiedKFold(
        n_splits=folds,
        shuffle=True,
        random_state=RANDOM_STATE,
    )
    scoring = {
        "accuracy": "accuracy",
        "balancedAccuracy": "balanced_accuracy",
        "macroF1": "f1_macro",
    }
    scores = cross_validate(
        clone_model(model),
        features,
        target,
        cv=splitter,
        scoring=scoring,
        n_jobs=1,
        return_train_score=True,
    )

    metric_results: JsonObject = {}
    for metric_name in scoring:
        test_values = np.asarray(
            scores[f"test_{metric_name}"],
            dtype=float,
        )
        train_values = np.asarray(
            scores[f"train_{metric_name}"],
            dtype=float,
        )
        metric_results[metric_name] = {
            "validationMean": float(np.mean(test_values)),
            "validationStd": float(np.std(test_values)),
            "trainMean": float(np.mean(train_values)),
            "trainStd": float(np.std(train_values)),
            "generalizationGap": float(np.mean(train_values) - np.mean(test_values)),
            "foldValues": [float(value) for value in test_values],
        }

    return {
        "available": True,
        "folds": folds,
        "metrics": metric_results,
    }


def calculate_staff_cross_validation(
    model: TrainablePredictor,
    features: pd.DataFrame,
    target: pd.DataFrame,
) -> JsonObject:
    """Calculeaza performanta si generalizarea modelului de personal."""

    folds = min(5, max(2, len(features) // 20))

    if len(features) < folds:
        return {
            "available": False,
            "reason": "Nu exista suficiente exemple.",
        }

    splitter = KFold(
        n_splits=folds,
        shuffle=True,
        random_state=RANDOM_STATE,
    )
    scoring = {
        "mae": "neg_mean_absolute_error",
        "rmse": "neg_root_mean_squared_error",
        "r2": "r2",
    }
    scores = cross_validate(
        clone_model(model),
        features,
        target,
        cv=splitter,
        scoring=scoring,
        n_jobs=1,
        return_train_score=True,
    )

    metric_results: JsonObject = {}
    for metric_name in scoring:
        test_values = np.asarray(
            scores[f"test_{metric_name}"],
            dtype=float,
        )
        train_values = np.asarray(
            scores[f"train_{metric_name}"],
            dtype=float,
        )

        if metric_name in {"mae", "rmse"}:
            test_values = -test_values
            train_values = -train_values
            generalization_gap = float(np.mean(test_values) - np.mean(train_values))
        else:
            generalization_gap = float(np.mean(train_values) - np.mean(test_values))

        metric_results[metric_name] = {
            "validationMean": float(np.mean(test_values)),
            "validationStd": float(np.std(test_values)),
            "trainMean": float(np.mean(train_values)),
            "trainStd": float(np.std(train_values)),
            "generalizationGap": generalization_gap,
            "foldValues": [float(value) for value in test_values],
        }

    return {
        "available": True,
        "folds": folds,
        "metrics": metric_results,
    }


def generate_oof_staff_predictions(
    model: TrainablePredictor,
    features: pd.DataFrame,
    target: pd.DataFrame,
) -> np.ndarray:
    """Genereaza predictii out-of-fold pentru modelul de personal."""

    folds = min(5, max(2, len(features) // 20))

    if len(features) < folds:
        fitted_model = clone_model(model)
        fitted_model.fit(features, target)
        return np.asarray(
            fitted_model.predict(features),
            dtype=float,
        )

    splitter = KFold(
        n_splits=folds,
        shuffle=True,
        random_state=RANDOM_STATE,
    )
    predictions = np.zeros(
        (len(features), len(STAFF_TARGET_COLUMNS)),
        dtype=float,
    )

    for train_indices, validation_indices in splitter.split(features):
        fold_model = clone_model(model)
        fold_model.fit(
            features.iloc[train_indices],
            target.iloc[train_indices],
        )
        predictions[validation_indices] = np.asarray(
            fold_model.predict(features.iloc[validation_indices]),
            dtype=float,
        )

    return predictions


# ---------------------------------------------------------------------------
# Caracteristicile modelului de intarziere
# ---------------------------------------------------------------------------


def build_delay_features(
    dataframe: pd.DataFrame,
    recommended_staff: np.ndarray,
) -> pd.DataFrame:
    """Construieste cele 21 de caracteristici ale modelului de intarziere."""

    recommended = np.maximum(
        0,
        np.rint(
            np.asarray(
                recommended_staff,
                dtype=float,
            )
        ).astype(int),
    )

    active_waiters = dataframe["active_waiters"].to_numpy(dtype=float)
    active_kitchen = dataframe["active_kitchen"].to_numpy(dtype=float)
    active_bar = dataframe["active_bar"].to_numpy(dtype=float)

    delay_frame = dataframe[
        [
            *BASE_FEATURE_COLUMNS,
            *ACTIVE_STAFF_COLUMNS,
        ]
    ].copy()

    delay_frame["waiter_deficit"] = np.maximum(
        0,
        recommended[:, 0] - active_waiters,
    )
    delay_frame["kitchen_deficit"] = np.maximum(
        0,
        recommended[:, 1] - active_kitchen,
    )
    delay_frame["bar_deficit"] = np.maximum(
        0,
        recommended[:, 2] - active_bar,
    )
    delay_frame["orders_per_waiter"] = dataframe["active_orders"].to_numpy(
        dtype=float
    ) / np.maximum(active_waiters, 1.0)
    delay_frame["kitchen_items_per_employee"] = dataframe["kitchen_load"].to_numpy(
        dtype=float
    ) / np.maximum(active_kitchen, 1.0)
    delay_frame["bar_items_per_employee"] = dataframe["bar_load"].to_numpy(
        dtype=float
    ) / np.maximum(active_bar, 1.0)
    delay_frame["occupancy_per_waiter"] = dataframe["estimated_occupancy"].to_numpy(
        dtype=float
    ) / np.maximum(active_waiters, 1.0)

    return delay_frame[DELAY_FEATURE_COLUMNS]


# ---------------------------------------------------------------------------
# Criterii de acceptare
# ---------------------------------------------------------------------------


def classifier_is_accepted(candidate_metrics, current_metrics):
    """Verifica daca un clasificator poate inlocui modelul curent."""
    candidate_quality_ok = (
        candidate_metrics["macroF1"] >= MIN_CLASSIFIER_MACRO_F1
        and candidate_metrics["balancedAccuracy"] >= MIN_CLASSIFIER_BALANCED_ACCURACY
    )

    if not candidate_quality_ok:
        return False

    if current_metrics is None:
        return True

    return (
        candidate_metrics["macroF1"] + CLASSIFIER_F1_TOLERANCE
        >= current_metrics["macroF1"]
        and candidate_metrics["balancedAccuracy"]
        + CLASSIFIER_BALANCED_ACCURACY_TOLERANCE
        >= current_metrics["balancedAccuracy"]
    )


def regressor_is_accepted(
    candidate_metrics,
    current_metrics,
    cross_validation=None,
):
    """Verifica daca modelul de personal poate fi acceptat."""
    candidate_quality_ok = (
        candidate_metrics["overallMae"] <= MAX_STAFF_MAE
        and candidate_metrics["overallR2"] >= MIN_STAFF_R2
    )

    if not candidate_quality_ok:
        return False

    #
    # Modelul nu este acceptat daca diferenta medie
    # train-validation pentru R2 depaseste 10%.
    # Aceasta regula previne revenirea problemei de
    # generalizare observate anterior la modelul de bar.
    #
    if cross_validation is not None and cross_validation.get("available"):
        r2_gap = (
            cross_validation.get("metrics", {}).get("r2", {}).get("generalizationGap")
        )

        if r2_gap is not None and r2_gap > 0.10:
            return False

    if current_metrics is None:
        return True

    return (
        candidate_metrics["overallMae"]
        <= current_metrics["overallMae"] + REGRESSOR_MAE_TOLERANCE
        and candidate_metrics["overallR2"] + REGRESSOR_R2_TOLERANCE
        >= current_metrics["overallR2"]
    )


def load_current_model(model_file: Path) -> Any | None:
    """Incarca modelul curent daca fisierul exista."""
    if not model_file.exists():
        return None

    return joblib.load(model_file)


# ---------------------------------------------------------------------------
# Rapoarte si persistenta
# ---------------------------------------------------------------------------


def format_confusion_matrix(matrix):
    """Formateaza matricea de confuzie ca tabel text."""
    header = "real\\prezis" + "".join(f"{label:>12}" for label in CLASS_LABELS)
    rows = [header]

    for label, values in zip(CLASS_LABELS, matrix):
        rows.append(f"{label:<12}" + "".join(f"{value:>12}" for value in values))

    return "\n".join(rows)


def format_classifier_metrics(metrics):
    """Formateaza metricile unui clasificator."""
    if metrics is None:
        return "Metrici indisponibile."

    lines = [
        f"Accuracy: {metrics['accuracy']:.4f}",
        f"Balanced accuracy: {metrics['balancedAccuracy']:.4f}",
        f"Macro precision: {metrics['macroPrecision']:.4f}",
        f"Macro recall: {metrics['macroRecall']:.4f}",
        f"Macro F1: {metrics['macroF1']:.4f}",
        f"Weighted F1: {metrics['weightedF1']:.4f}",
        f"Matthews Correlation Coefficient: {metrics['matthewsCorrelationCoefficient']:.4f}",
    ]

    if "rocAucMacroOvr" in metrics:
        lines.extend(
            [
                f"ROC-AUC macro OVR: {metrics['rocAucMacroOvr']:.4f}",
                f"ROC-AUC weighted OVR: {metrics['rocAucWeightedOvr']:.4f}",
                f"Log loss: {metrics['logLoss']:.4f}",
                f"Multiclass Brier score: {metrics['multiclassBrierScore']:.4f}",
                f"Expected Calibration Error: {metrics['expectedCalibrationError']:.4f}",
                f"Incredere medie: {metrics['averageConfidence']:.4f}",
            ]
        )

    lines.append(
        "Erori severe SCAZUT <-> RIDICAT: "
        f"{metrics['severeErrors']['count']} "
        f"({metrics['severeErrors']['rate']:.2%})"
    )
    lines.append("")
    lines.append("Metrici pe clasa:")

    for label in CLASS_LABELS:
        class_metrics = metrics["classMetrics"][label]
        lines.extend(
            [
                f"  {label}:",
                f"    Precision: {class_metrics['precision']:.4f}",
                f"    Recall: {class_metrics['recall']:.4f}",
                f"    F1: {class_metrics['f1']:.4f}",
                f"    Support: {class_metrics['support']}",
            ]
        )

    lines.extend(
        [
            "",
            "Matrice de confuzie:",
            format_confusion_matrix(metrics["confusionMatrix"]),
        ]
    )
    return "\n".join(lines)


def format_regression_metrics(metrics):
    """Formateaza metricile modelului de personal."""
    if metrics is None:
        return "Metrici indisponibile."

    lines = [
        f"MAE general: {metrics['overallMae']:.4f}",
        f"RMSE general: {metrics['overallRmse']:.4f}",
        f"R2 general: {metrics['overallR2']:.4f}",
        f"Median absolute error: {metrics['overallMedianAbsoluteError']:.4f}",
        f"Exact dupa rotunjire: {metrics['exactRoundedRate']:.2%}",
        f"In limita +/-1 angajat: {metrics['withinOneEmployeeRate']:.2%}",
        f"Toate cele 3 iesiri exacte: {metrics['allOutputsExactRate']:.2%}",
        f"Subestimare: {metrics['underestimateRate']:.2%}",
        f"Supraestimare: {metrics['overestimateRate']:.2%}",
        f"Eroare rotunjita maxima: {metrics['maximumRoundedError']}",
        "",
        "Metrici pe rol:",
    ]

    for target in STAFF_TARGET_COLUMNS:
        target_metrics = metrics["perTarget"][target]
        lines.extend(
            [
                f"  {target}:",
                f"    MAE: {target_metrics['mae']:.4f}",
                f"    RMSE: {target_metrics['rmse']:.4f}",
                f"    R2: {target_metrics['r2']:.4f}",
                f"    Exact dupa rotunjire: {target_metrics['exactRoundedRate']:.2%}",
                f"    In limita +/-1: {target_metrics['withinOneEmployeeRate']:.2%}",
            ]
        )

    return "\n".join(lines)


def format_cross_validation(cross_validation):
    """Formateaza rezultatele cross-validation."""
    if not cross_validation.get("available"):
        return (
            "Cross-validation indisponibil: "
            f"{cross_validation.get('reason', 'motiv necunoscut')}"
        )

    lines = [
        f"Numar folduri: {cross_validation['folds']}",
    ]

    for metric_name, values in cross_validation["metrics"].items():
        lines.append(
            f"{metric_name}: mean={values['validationMean']:.4f}, "
            f"std={values['validationStd']:.4f}, "
            f"gap={values['generalizationGap']:.4f}"
        )

    return "\n".join(lines)


def create_classifier_report(
    title,
    model_name,
    accepted,
    candidate_metrics,
    current_metrics,
    cross_validation,
    candidate_results,
    feature_columns,
    real_rows,
    synthetic_rows,
    current_error=None,
    extra_notes=None,
):
    """Construieste raportul text pentru un clasificator."""
    comparison_sections = []

    for candidate_name, metrics in candidate_results.items():
        comparison_sections.append(
            f"MODEL CANDIDAT: {candidate_name}\n{format_classifier_metrics(metrics)}"
        )

    notes = "\n".join(f"- {note}" for note in (extra_notes or []))

    return f"""
{title}

Model selectat: {model_name}
Model acceptat pentru inlocuire: {accepted}

Date reale etichetate: {real_rows}
Date sintetice folosite la antrenare: {synthetic_rows}
Caracteristici ({len(feature_columns)}): {", ".join(feature_columns)}

EVALUAREA MODELULUI CURENT PE SETUL REAL DE TEST
{format_classifier_metrics(current_metrics)}
{f"Eroare evaluare model curent: {current_error}" if current_error else ""}

COMPARATIA MODELELOR CANDIDATE

{chr(10).join(comparison_sections)}

MODEL CANDIDAT SELECTAT - SET REAL DE TEST
{format_classifier_metrics(candidate_metrics)}

CROSS-VALIDATION
{format_cross_validation(cross_validation)}

OBSERVATII
{notes or "- Nu exista observatii suplimentare."}
""".strip()


def create_staff_report(
    model_name,
    accepted,
    candidate_metrics,
    current_metrics,
    cross_validation,
    candidate_results,
    real_rows,
    synthetic_rows,
    current_error=None,
):
    """Construieste raportul text pentru modelul de personal."""
    comparison_sections = []

    for candidate_name, metrics in candidate_results.items():
        comparison_sections.append(
            f"MODEL CANDIDAT: {candidate_name}\n{format_regression_metrics(metrics)}"
        )

    return f"""
MODEL RECOMANDARE PERSONAL - REANTRENARE

Model selectat: {model_name}
Model acceptat pentru inlocuire: {accepted}

Date reale etichetate: {real_rows}
Date sintetice folosite la antrenare: {synthetic_rows}
Caracteristici ({len(BASE_FEATURE_COLUMNS)}): {", ".join(BASE_FEATURE_COLUMNS)}

EVALUAREA MODELULUI CURENT PE SETUL REAL DE TEST
{format_regression_metrics(current_metrics)}
{f"Eroare evaluare model curent: {current_error}" if current_error else ""}

COMPARATIA MODELELOR CANDIDATE

{chr(10).join(comparison_sections)}

MODEL CANDIDAT SELECTAT - SET REAL DE TEST
{format_regression_metrics(candidate_metrics)}

CROSS-VALIDATION
{format_cross_validation(cross_validation)}

INTERPRETARE
- MAE si RMSE trebuie sa fie cat mai mici;
- R2 trebuie sa fie cat mai apropiat de 1;
- exact match si +/-1 angajat descriu direct utilitatea operationala;
- acceptarea se face separat de celelalte doua modele.
""".strip()


def save_global_report(report, timestamp):
    """Salveaza raportul global si copia latest."""
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    report_file = REPORTS_DIR / f"retraining_metrics_{timestamp}.json"
    latest_report_file = REPORTS_DIR / "latest_retraining_metrics.json"
    text = json.dumps(report, ensure_ascii=False, indent=2)
    report_file.write_text(text, encoding="utf-8")
    latest_report_file.write_text(text, encoding="utf-8")
    return report_file


def backup_files(timestamp, files_to_backup):
    """Copiaza artefactele curente in directorul de backup."""
    backup_directory = BACKUPS_DIR / timestamp
    backup_directory.mkdir(parents=True, exist_ok=True)

    for source_file in files_to_backup:
        if source_file.exists():
            destination = backup_directory / source_file.name
            shutil.copy2(source_file, destination)

    return backup_directory


def atomic_replace_artifacts(timestamp, artifacts):
    """Inlocuieste atomic artefactele si restaureaza backup-ul la eroare."""
    if not artifacts:
        return None

    files_to_backup = []

    for artifact in artifacts:
        files_to_backup.extend(
            [
                artifact["modelFile"],
                artifact["metadataFile"],
                artifact["reportFile"],
            ]
        )

    backup_directory = backup_files(timestamp, files_to_backup)
    temporary_directory = BASE_DIR / f".retrain_{timestamp}"
    temporary_directory.mkdir(parents=True, exist_ok=True)
    staged_files = []

    try:
        for artifact in artifacts:
            staged_model = temporary_directory / artifact["modelFile"].name
            staged_metadata = temporary_directory / artifact["metadataFile"].name
            staged_report = temporary_directory / artifact["reportFile"].name

            joblib.dump(artifact["model"], staged_model)
            staged_metadata.write_text(
                json.dumps(
                    artifact["metadata"],
                    ensure_ascii=False,
                    indent=2,
                ),
                encoding="utf-8",
            )
            staged_report.write_text(
                artifact["reportText"],
                encoding="utf-8",
            )

            staged_files.extend(
                [
                    (staged_model, artifact["modelFile"]),
                    (staged_metadata, artifact["metadataFile"]),
                    (staged_report, artifact["reportFile"]),
                ]
            )

        for _, destination in staged_files:
            destination.parent.mkdir(parents=True, exist_ok=True)

        for source, destination in staged_files:
            os.replace(source, destination)

        return backup_directory
    except Exception:
        for destination in files_to_backup:
            backup_file = backup_directory / destination.name

            if backup_file.exists():
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(backup_file, destination)

        raise
    finally:
        shutil.rmtree(temporary_directory, ignore_errors=True)


# ---------------------------------------------------------------------------
# Orchestrarea reantrenarii
# ---------------------------------------------------------------------------


def retrain_all_models():
    """Ruleaza procesul complet de reantrenare."""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    trained_at_utc = datetime.now(timezone.utc).isoformat()

    real_data_raw = load_labeled_records()
    real_data, active_staff_source = prepare_real_records(real_data_raw)
    synthetic_data = prepare_synthetic_records(load_synthetic_records())

    traffic_train_real, traffic_test_real = train_test_split(
        real_data,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
        stratify=real_data[TRAFFIC_TARGET],
    )
    staff_train_real, staff_test_real = train_test_split(
        real_data,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
    )
    delay_train_real, delay_test_real = train_test_split(
        real_data,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
        stratify=real_data[DELAY_TARGET],
    )

    traffic_training_frame = build_training_frame(
        synthetic_data,
        traffic_train_real,
    )
    staff_training_frame = build_training_frame(
        synthetic_data,
        staff_train_real,
    )
    delay_training_frame = build_training_frame(
        synthetic_data,
        delay_train_real,
    )

    current_traffic_model = load_current_model(TRAFFIC_MODEL_FILE)
    current_staff_model = load_current_model(STAFF_MODEL_FILE)
    current_delay_model = load_current_model(DELAY_MODEL_FILE)

    traffic_name, traffic_selected_model, traffic_candidate_results = (
        select_classifier_candidate(
            build_traffic_candidates(),
            traffic_training_frame[BASE_FEATURE_COLUMNS],
            traffic_training_frame[TRAFFIC_TARGET],
            traffic_test_real[BASE_FEATURE_COLUMNS],
            traffic_test_real[TRAFFIC_TARGET],
        )
    )
    traffic_candidate_metrics = traffic_candidate_results[traffic_name]
    traffic_current_metrics, traffic_current_error = safe_current_classifier_metrics(
        current_traffic_model,
        traffic_test_real[BASE_FEATURE_COLUMNS],
        traffic_test_real[TRAFFIC_TARGET],
    )
    traffic_accepted = classifier_is_accepted(
        traffic_candidate_metrics,
        traffic_current_metrics,
    )
    traffic_cross_validation = calculate_classifier_cross_validation(
        clone_model(traffic_selected_model),
        traffic_training_frame[BASE_FEATURE_COLUMNS],
        traffic_training_frame[TRAFFIC_TARGET],
    )

    staff_name, staff_selected_model, staff_candidate_results = select_staff_candidate(
        build_staff_candidates(),
        staff_training_frame[BASE_FEATURE_COLUMNS],
        staff_training_frame[STAFF_TARGET_COLUMNS],
        staff_test_real[BASE_FEATURE_COLUMNS],
        staff_test_real[STAFF_TARGET_COLUMNS],
    )
    staff_candidate_metrics = staff_candidate_results[staff_name]
    staff_current_metrics, staff_current_error = safe_current_regression_metrics(
        current_staff_model,
        staff_test_real[BASE_FEATURE_COLUMNS],
        staff_test_real[STAFF_TARGET_COLUMNS],
    )
    staff_cross_validation = calculate_staff_cross_validation(
        clone_model(staff_selected_model),
        staff_training_frame[BASE_FEATURE_COLUMNS],
        staff_training_frame[STAFF_TARGET_COLUMNS],
    )
    staff_accepted = regressor_is_accepted(
        staff_candidate_metrics,
        staff_current_metrics,
        staff_cross_validation,
    )

    delay_staff_model = clone_model(staff_selected_model)
    delay_staff_model.fit(
        delay_training_frame[BASE_FEATURE_COLUMNS],
        delay_training_frame[STAFF_TARGET_COLUMNS],
    )
    delay_test_recommended = delay_staff_model.predict(
        delay_test_real[BASE_FEATURE_COLUMNS]
    )
    delay_test_features = build_delay_features(
        delay_test_real,
        delay_test_recommended,
    )

    delay_oof_recommended = generate_oof_staff_predictions(
        clone_model(staff_selected_model),
        delay_training_frame[BASE_FEATURE_COLUMNS],
        delay_training_frame[STAFF_TARGET_COLUMNS],
    )
    delay_training_features = build_delay_features(
        delay_training_frame,
        delay_oof_recommended,
    )

    delay_name, delay_selected_model, delay_candidate_results = (
        select_classifier_candidate(
            build_delay_candidates(),
            delay_training_features,
            delay_training_frame[DELAY_TARGET],
            delay_test_features,
            delay_test_real[DELAY_TARGET],
        )
    )
    delay_candidate_metrics = delay_candidate_results[delay_name]
    delay_current_metrics, delay_current_error = safe_current_classifier_metrics(
        current_delay_model,
        delay_test_features,
        delay_test_real[DELAY_TARGET],
    )
    delay_accepted = classifier_is_accepted(
        delay_candidate_metrics,
        delay_current_metrics,
    )
    delay_cross_validation = calculate_classifier_cross_validation(
        clone_model(delay_selected_model),
        delay_training_features,
        delay_training_frame[DELAY_TARGET],
    )

    final_training_frame = build_training_frame(
        synthetic_data,
        real_data,
    )

    final_traffic_model = clone_model(traffic_selected_model)
    final_traffic_model.fit(
        final_training_frame[BASE_FEATURE_COLUMNS],
        final_training_frame[TRAFFIC_TARGET],
    )

    final_staff_model = clone_model(staff_selected_model)
    final_staff_model.fit(
        final_training_frame[BASE_FEATURE_COLUMNS],
        final_training_frame[STAFF_TARGET_COLUMNS],
    )

    final_delay_oof_recommended = generate_oof_staff_predictions(
        clone_model(staff_selected_model),
        final_training_frame[BASE_FEATURE_COLUMNS],
        final_training_frame[STAFF_TARGET_COLUMNS],
    )
    final_delay_features = build_delay_features(
        final_training_frame,
        final_delay_oof_recommended,
    )
    final_delay_model = clone_model(delay_selected_model)
    final_delay_model.fit(
        final_delay_features,
        final_training_frame[DELAY_TARGET],
    )

    common_metadata = {
        "trainedAtUtc": trained_at_utc,
        "trainingType": "database_retraining",
        "realLabeledRows": int(len(real_data)),
        "syntheticRows": int(len(synthetic_data)),
        "realDataRepeat": REAL_DATA_REPEAT,
        "includeSyntheticData": INCLUDE_SYNTHETIC_DATA,
        "testSize": TEST_SIZE,
        "activeStaffSourceForRealRows": active_staff_source,
    }

    traffic_metadata = {
        **common_metadata,
        "modelName": traffic_name,
        "featureColumns": BASE_FEATURE_COLUMNS,
        "targetColumn": TRAFFIC_TARGET,
        "classLabels": CLASS_LABELS,
        "candidateResults": traffic_candidate_results,
        "currentModelTestMetrics": traffic_current_metrics,
        "candidateTestMetrics": traffic_candidate_metrics,
        "crossValidation": traffic_cross_validation,
        "accepted": traffic_accepted,
    }
    staff_metadata = {
        **common_metadata,
        "modelName": staff_name,
        "featureColumns": BASE_FEATURE_COLUMNS,
        "targetColumns": STAFF_TARGET_COLUMNS,
        "candidateResults": staff_candidate_results,
        "currentModelTestMetrics": staff_current_metrics,
        "candidateTestMetrics": staff_candidate_metrics,
        "crossValidation": staff_cross_validation,
        "accepted": staff_accepted,
    }
    delay_metadata = {
        **common_metadata,
        "modelName": delay_name,
        "featureColumns": DELAY_FEATURE_COLUMNS,
        "targetColumn": DELAY_TARGET,
        "classLabels": CLASS_LABELS,
        "staffPredictionStrategy": (
            "out_of_fold_predictions_for_training_and_held_out_predictions_for_testing"
        ),
        "candidateResults": delay_candidate_results,
        "currentModelTestMetrics": delay_current_metrics,
        "candidateTestMetrics": delay_candidate_metrics,
        "crossValidation": delay_cross_validation,
        "accepted": delay_accepted,
    }

    traffic_report = create_classifier_report(
        "MODEL TRAFIC - REANTRENARE",
        traffic_name,
        traffic_accepted,
        traffic_candidate_metrics,
        traffic_current_metrics,
        traffic_cross_validation,
        traffic_candidate_results,
        BASE_FEATURE_COLUMNS,
        len(real_data),
        len(synthetic_data),
        current_error=traffic_current_error,
        extra_notes=[
            "Selectia se face dupa Macro F1, Balanced Accuracy, MCC si Accuracy.",
            "Evaluarea principala este realizata pe date reale etichetate.",
        ],
    )
    staff_report = create_staff_report(
        staff_name,
        staff_accepted,
        staff_candidate_metrics,
        staff_current_metrics,
        staff_cross_validation,
        staff_candidate_results,
        len(real_data),
        len(synthetic_data),
        current_error=staff_current_error,
    )
    delay_report = create_classifier_report(
        "MODEL RISC INTARZIERE - REANTRENARE",
        delay_name,
        delay_accepted,
        delay_candidate_metrics,
        delay_current_metrics,
        delay_cross_validation,
        delay_candidate_results,
        DELAY_FEATURE_COLUMNS,
        len(real_data),
        len(synthetic_data),
        current_error=delay_current_error,
        extra_notes=[
            "Modelul foloseste 21 de caracteristici.",
            (
                "Necesarul de personal folosit la construirea caracteristicilor "
                "de delay este prezis out-of-fold, reducand train-serving skew."
            ),
            f"Sursa personalului activ pentru datele reale: {active_staff_source}.",
        ],
    )

    artifacts = []

    if traffic_accepted:
        artifacts.append(
            {
                "model": final_traffic_model,
                "modelFile": TRAFFIC_MODEL_FILE,
                "metadata": traffic_metadata,
                "metadataFile": TRAFFIC_METADATA_FILE,
                "reportText": traffic_report,
                "reportFile": TRAFFIC_REPORT_FILE,
            }
        )

    if staff_accepted:
        artifacts.append(
            {
                "model": final_staff_model,
                "modelFile": STAFF_MODEL_FILE,
                "metadata": staff_metadata,
                "metadataFile": STAFF_METADATA_FILE,
                "reportText": staff_report,
                "reportFile": STAFF_REPORT_FILE,
            }
        )

    if delay_accepted:
        artifacts.append(
            {
                "model": final_delay_model,
                "modelFile": DELAY_MODEL_FILE,
                "metadata": delay_metadata,
                "metadataFile": DELAY_METADATA_FILE,
                "reportText": delay_report,
                "reportFile": DELAY_REPORT_FILE,
            }
        )

    backup_directory = atomic_replace_artifacts(timestamp, artifacts)
    replaced_models = []

    if traffic_accepted:
        replaced_models.append("traffic")
    if staff_accepted:
        replaced_models.append("staff")
    if delay_accepted:
        replaced_models.append("delay")

    report = {
        "status": "success" if replaced_models else "rejected",
        "message": (
            "Reantrenarea s-a incheiat. Modelele acceptate au fost inlocuite."
            if replaced_models
            else "Niciun model candidat nu a indeplinit criteriile de acceptare."
        ),
        "trainedAtUtc": trained_at_utc,
        "labeledRecords": int(len(real_data)),
        "syntheticRecords": int(len(synthetic_data)),
        "minimumRequired": MIN_LABELED_RECORDS,
        "modelsReplaced": bool(replaced_models),
        "replacedModels": replaced_models,
        "backupDirectory": (
            str(backup_directory) if backup_directory is not None else None
        ),
        "activeStaffSourceForRealRows": active_staff_source,
        "accepted": {
            "traffic": traffic_accepted,
            "staff": staff_accepted,
            "delay": delay_accepted,
        },
        "selectedModels": {
            "traffic": traffic_name,
            "staff": staff_name,
            "delay": delay_name,
        },
        "metrics": {
            "traffic": {
                "current": traffic_current_metrics,
                "candidate": traffic_candidate_metrics,
                "currentEvaluationError": traffic_current_error,
                "crossValidation": traffic_cross_validation,
            },
            "staff": {
                "current": staff_current_metrics,
                "candidate": staff_candidate_metrics,
                "currentEvaluationError": staff_current_error,
                "crossValidation": staff_cross_validation,
            },
            "delay": {
                "current": delay_current_metrics,
                "candidate": delay_candidate_metrics,
                "currentEvaluationError": delay_current_error,
                "crossValidation": delay_cross_validation,
            },
        },
    }

    report_file = save_global_report(report, timestamp)
    report["reportFile"] = str(report_file)
    return report


if __name__ == "__main__":
    try:
        retraining_result = retrain_all_models()
        print(json.dumps(retraining_result, ensure_ascii=False, indent=2))
    except RetrainingValidationError as validation_error:
        print(f"Reantrenare oprita: {validation_error}")
    except Exception as unexpected_error:
        print(f"Eroare la reantrenare: {unexpected_error}")
