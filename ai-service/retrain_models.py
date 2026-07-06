import json
import math
import os
import shutil
from datetime import datetime
from pathlib import Path

import joblib
import mysql.connector
import pandas as pd
from dotenv import load_dotenv
from sklearn.ensemble import (
    RandomForestClassifier,
    RandomForestRegressor,
)
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    mean_absolute_error,
)
from sklearn.model_selection import train_test_split


BASE_DIR = Path(__file__).resolve().parent

load_dotenv(BASE_DIR / ".env")


MODELS_DIR = BASE_DIR / "models"
REPORTS_DIR = BASE_DIR / "reports"
BACKUPS_DIR = MODELS_DIR / "backups"

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


STAFF_TARGET_COLUMNS = [
    "actual_waiters",
    "actual_kitchen_staff",
    "actual_bar_staff",
]


MIN_LABELED_RECORDS = int(
    os.getenv("MIN_LABELED_RECORDS", "30")
)

TEST_SIZE = 0.2
RANDOM_STATE = 42

ACCURACY_TOLERANCE = 0.02
MAE_TOLERANCE = 0.10


class RetrainingValidationError(Exception):
    pass


def get_database_connection():
    database_password = os.getenv("DB_PASSWORD")

    if not database_password:
        raise RetrainingValidationError(
            "Variabila DB_PASSWORD nu este configurata."
        )

    return mysql.connector.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "3306")),
        database=os.getenv("DB_NAME", "horeca_db"),
        user=os.getenv("DB_USER", "root"),
        password=database_password,
    )


def load_labeled_records():
    query = """
        SELECT
            day_of_week,
            hour,
            active_orders,
            occupied_tables,
            estimated_occupancy,
            kitchen_load,
            bar_load,
            avg_preparation_time,
            orders_last30min AS orders_last_30_min,
            order_age_minutes,
            item_count,
            observed_traffic_level,
            observed_delay_risk,
            actual_waiters,
            actual_kitchen_staff,
            actual_bar_staff
        FROM decision_training_records
        WHERE labeled_at IS NOT NULL
          AND observed_traffic_level IS NOT NULL
          AND observed_delay_risk IS NOT NULL
          AND actual_waiters IS NOT NULL
          AND actual_kitchen_staff IS NOT NULL
          AND actual_bar_staff IS NOT NULL
        ORDER BY created_at
    """

    connection = None
    cursor = None

    try:
        connection = get_database_connection()
        cursor = connection.cursor(dictionary=True)

        cursor.execute(query)
        rows = cursor.fetchall()

        return pd.DataFrame(rows)
    finally:
        if cursor is not None:
            cursor.close()

        if (
                connection is not None
                and connection.is_connected()
        ):
            connection.close()


def validate_classification_target(
        target,
        target_name,
):
    class_counts = target.value_counts()

    if len(class_counts) < 2:
        raise RetrainingValidationError(
            f"Pentru modelul de {target_name} trebuie "
            "sa existe cel putin doua clase diferite."
        )

    if class_counts.min() < 2:
        raise RetrainingValidationError(
            f"Fiecare clasa pentru {target_name} trebuie "
            "sa apara de cel putin doua ori."
        )

    test_records = math.ceil(
        len(target) * TEST_SIZE
    )

    if test_records < len(class_counts):
        raise RetrainingValidationError(
            f"Nu exista suficiente date pentru evaluarea "
            f"modelului de {target_name}."
        )


def clean_and_validate_data(dataframe):
    if dataframe.empty:
        raise RetrainingValidationError(
            "Nu exista inregistrari etichetate."
        )

    numeric_columns = (
            FEATURE_COLUMNS
            + STAFF_TARGET_COLUMNS
    )

    for column in numeric_columns:
        dataframe[column] = pd.to_numeric(
            dataframe[column],
            errors="coerce",
        )

    dataframe["observed_traffic_level"] = (
        dataframe["observed_traffic_level"]
        .astype(str)
        .str.strip()
        .str.upper()
    )

    dataframe["observed_delay_risk"] = (
        dataframe["observed_delay_risk"]
        .astype(str)
        .str.strip()
        .str.upper()
    )

    required_columns = (
            FEATURE_COLUMNS
            + STAFF_TARGET_COLUMNS
            + [
                "observed_traffic_level",
                "observed_delay_risk",
            ]
    )

    dataframe = dataframe.dropna(
        subset=required_columns
    ).copy()

    if len(dataframe) < MIN_LABELED_RECORDS:
        raise RetrainingValidationError(
            f"Sunt necesare cel putin "
            f"{MIN_LABELED_RECORDS} inregistrari "
            f"etichetate. Momentan exista "
            f"{len(dataframe)}."
        )

    allowed_levels = {
        "SCAZUT",
        "MEDIU",
        "RIDICAT",
    }

    traffic_levels = set(
        dataframe[
            "observed_traffic_level"
        ].unique()
    )

    delay_levels = set(
        dataframe[
            "observed_delay_risk"
        ].unique()
    )

    invalid_traffic_levels = (
            traffic_levels - allowed_levels
    )

    invalid_delay_levels = (
            delay_levels - allowed_levels
    )

    if invalid_traffic_levels:
        raise RetrainingValidationError(
            "Exista niveluri de trafic invalide: "
            f"{sorted(invalid_traffic_levels)}"
        )

    if invalid_delay_levels:
        raise RetrainingValidationError(
            "Exista niveluri de risc invalide: "
            f"{sorted(invalid_delay_levels)}"
        )

    validate_classification_target(
        dataframe["observed_traffic_level"],
        "trafic",
    )

    validate_classification_target(
        dataframe["observed_delay_risk"],
        "risc de intarziere",
    )

    return dataframe


def load_current_model(model_file):
    if not model_file.exists():
        return None

    return joblib.load(model_file)


def evaluate_classifier(
        current_model,
        candidate_model,
        x_train,
        x_test,
        y_train,
        y_test,
):
    candidate_model.fit(
        x_train,
        y_train,
    )

    candidate_predictions = (
        candidate_model.predict(x_test)
    )

    new_accuracy = float(
        accuracy_score(
            y_test,
            candidate_predictions,
        )
    )

    report = classification_report(
        y_test,
        candidate_predictions,
        output_dict=True,
        zero_division=0,
    )

    current_accuracy = None

    if current_model is not None:
        current_predictions = (
            current_model.predict(x_test)
        )

        current_accuracy = float(
            accuracy_score(
                y_test,
                current_predictions,
            )
        )

    return {
        "currentAccuracy": current_accuracy,
        "newAccuracy": new_accuracy,
        "classificationReport": report,
    }


def evaluate_regressor(
        current_model,
        candidate_model,
        x_train,
        x_test,
        y_train,
        y_test,
):
    candidate_model.fit(
        x_train,
        y_train,
    )

    candidate_predictions = (
        candidate_model.predict(x_test)
    )

    new_mae = float(
        mean_absolute_error(
            y_test,
            candidate_predictions,
        )
    )

    current_mae = None

    if current_model is not None:
        current_predictions = (
            current_model.predict(x_test)
        )

        current_mae = float(
            mean_absolute_error(
                y_test,
                current_predictions,
            )
        )

    return {
        "currentMae": current_mae,
        "newMae": new_mae,
    }


def classifier_is_accepted(metrics):
    current_accuracy = metrics[
        "currentAccuracy"
    ]

    new_accuracy = metrics["newAccuracy"]

    if current_accuracy is None:
        return True

    return (
            new_accuracy + ACCURACY_TOLERANCE
            >= current_accuracy
    )


def regressor_is_accepted(metrics):
    current_mae = metrics["currentMae"]
    new_mae = metrics["newMae"]

    if current_mae is None:
        return True

    return (
            new_mae
            <= current_mae + MAE_TOLERANCE
    )


def save_report(report, timestamp):
    REPORTS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    report_file = (
            REPORTS_DIR
            / f"retraining_metrics_{timestamp}.json"
    )

    latest_report_file = (
            REPORTS_DIR
            / "latest_retraining_metrics.json"
    )

    report_text = json.dumps(
        report,
        ensure_ascii=False,
        indent=2,
    )

    report_file.write_text(
        report_text,
        encoding="utf-8",
    )

    latest_report_file.write_text(
        report_text,
        encoding="utf-8",
    )

    return report_file


def replace_models(
        traffic_model,
        staff_model,
        delay_model,
        timestamp,
):
    MODELS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    backup_directory = (
            BACKUPS_DIR / timestamp
    )

    backup_directory.mkdir(
        parents=True,
        exist_ok=True,
    )

    model_files = [
        TRAFFIC_MODEL_FILE,
        STAFF_MODEL_FILE,
        DELAY_MODEL_FILE,
    ]

    for model_file in model_files:
        if model_file.exists():
            shutil.copy2(
                model_file,
                backup_directory
                / model_file.name,
                )

    temporary_directory = (
            MODELS_DIR
            / f".retrain_{timestamp}"
    )

    temporary_directory.mkdir(
        parents=True,
        exist_ok=True,
    )

    temporary_traffic_file = (
            temporary_directory
            / TRAFFIC_MODEL_FILE.name
    )

    temporary_staff_file = (
            temporary_directory
            / STAFF_MODEL_FILE.name
    )

    temporary_delay_file = (
            temporary_directory
            / DELAY_MODEL_FILE.name
    )

    joblib.dump(
        traffic_model,
        temporary_traffic_file,
    )

    joblib.dump(
        staff_model,
        temporary_staff_file,
    )

    joblib.dump(
        delay_model,
        temporary_delay_file,
    )

    try:
        os.replace(
            temporary_traffic_file,
            TRAFFIC_MODEL_FILE,
        )

        os.replace(
            temporary_staff_file,
            STAFF_MODEL_FILE,
        )

        os.replace(
            temporary_delay_file,
            DELAY_MODEL_FILE,
        )
    except Exception:
        for model_file in model_files:
            backup_file = (
                    backup_directory
                    / model_file.name
            )

            if backup_file.exists():
                shutil.copy2(
                    backup_file,
                    model_file,
                )

        raise
    finally:
        shutil.rmtree(
            temporary_directory,
            ignore_errors=True,
        )

    return backup_directory


def retrain_all_models():
    timestamp = datetime.now().strftime(
        "%Y%m%d_%H%M%S"
    )

    dataframe = load_labeled_records()

    dataframe = clean_and_validate_data(
        dataframe
    )

    x = dataframe[FEATURE_COLUMNS]

    traffic_target = dataframe[
        "observed_traffic_level"
    ]

    delay_target = dataframe[
        "observed_delay_risk"
    ]

    staff_target = dataframe[
        STAFF_TARGET_COLUMNS
    ]

    current_traffic_model = (
        load_current_model(
            TRAFFIC_MODEL_FILE
        )
    )

    current_staff_model = (
        load_current_model(
            STAFF_MODEL_FILE
        )
    )

    current_delay_model = (
        load_current_model(
            DELAY_MODEL_FILE
        )
    )

    traffic_candidate = (
        RandomForestClassifier(
            n_estimators=100,
            random_state=RANDOM_STATE,
            max_depth=10,
        )
    )

    staff_candidate = (
        RandomForestRegressor(
            n_estimators=100,
            random_state=RANDOM_STATE,
            max_depth=10,
        )
    )

    delay_candidate = (
        RandomForestClassifier(
            n_estimators=100,
            random_state=RANDOM_STATE,
            max_depth=10,
        )
    )

    (
        x_traffic_train,
        x_traffic_test,
        y_traffic_train,
        y_traffic_test,
    ) = train_test_split(
        x,
        traffic_target,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
        stratify=traffic_target,
    )

    traffic_metrics = evaluate_classifier(
        current_traffic_model,
        traffic_candidate,
        x_traffic_train,
        x_traffic_test,
        y_traffic_train,
        y_traffic_test,
    )

    (
        x_delay_train,
        x_delay_test,
        y_delay_train,
        y_delay_test,
    ) = train_test_split(
        x,
        delay_target,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
        stratify=delay_target,
    )

    delay_metrics = evaluate_classifier(
        current_delay_model,
        delay_candidate,
        x_delay_train,
        x_delay_test,
        y_delay_train,
        y_delay_test,
    )

    (
        x_staff_train,
        x_staff_test,
        y_staff_train,
        y_staff_test,
    ) = train_test_split(
        x,
        staff_target,
        test_size=TEST_SIZE,
        random_state=RANDOM_STATE,
    )

    staff_metrics = evaluate_regressor(
        current_staff_model,
        staff_candidate,
        x_staff_train,
        x_staff_test,
        y_staff_train,
        y_staff_test,
    )

    traffic_accepted = (
        classifier_is_accepted(
            traffic_metrics
        )
    )

    staff_accepted = (
        regressor_is_accepted(
            staff_metrics
        )
    )

    delay_accepted = (
        classifier_is_accepted(
            delay_metrics
        )
    )

    all_models_accepted = (
            traffic_accepted
            and staff_accepted
            and delay_accepted
    )

    report = {
        "trainedAt": (
            datetime.now().isoformat()
        ),
        "labeledRecords": len(dataframe),
        "minimumRequired": (
            MIN_LABELED_RECORDS
        ),
        "metrics": {
            "traffic": traffic_metrics,
            "staff": staff_metrics,
            "delay": delay_metrics,
        },
        "accepted": {
            "traffic": traffic_accepted,
            "staff": staff_accepted,
            "delay": delay_accepted,
        },
        "modelsReplaced": False,
    }

    if not all_models_accepted:
        report["status"] = "rejected"

        report["message"] = (
            "Modelele noi nu au indeplinit "
            "conditiile necesare pentru inlocuire."
        )

        report_file = save_report(
            report,
            timestamp,
        )

        report["reportFile"] = str(
            report_file
        )

        return report

    final_traffic_model = (
        RandomForestClassifier(
            n_estimators=100,
            random_state=RANDOM_STATE,
            max_depth=10,
        )
    )

    final_staff_model = (
        RandomForestRegressor(
            n_estimators=100,
            random_state=RANDOM_STATE,
            max_depth=10,
        )
    )

    final_delay_model = (
        RandomForestClassifier(
            n_estimators=100,
            random_state=RANDOM_STATE,
            max_depth=10,
        )
    )

    final_traffic_model.fit(
        x,
        traffic_target,
    )

    final_staff_model.fit(
        x,
        staff_target,
    )

    final_delay_model.fit(
        x,
        delay_target,
    )

    backup_directory = replace_models(
        final_traffic_model,
        final_staff_model,
        final_delay_model,
        timestamp,
    )

    report["status"] = "success"

    report["message"] = (
        "Modelele au fost reantrenate "
        "si inlocuite cu succes."
    )

    report["modelsReplaced"] = True

    report["backupDirectory"] = str(
        backup_directory
    )

    report_file = save_report(
        report,
        timestamp,
    )

    report["reportFile"] = str(
        report_file
    )

    return report


if __name__ == "__main__":
    try:
        result = retrain_all_models()

        print(
            json.dumps(
                result,
                ensure_ascii=False,
                indent=2,
            )
        )
    except RetrainingValidationError as exception:
        print(
            f"Reantrenare oprita: {exception}"
        )
    except Exception as exception:
        print(
            f"Eroare la reantrenare: {exception}"
        )