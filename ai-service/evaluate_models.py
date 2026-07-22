"""
Evaluare extinsa pentru cele trei modele AI ale aplicatiei HoReCa.

Scriptul verifica:
- integritatea datasetului si a fisierelor de model;
- performanta modelului de trafic;
- performanta modelului de personal;
- performanta modelului de intarziere in regim offline;
- performanta modelului de intarziere intr-un flux apropiat de productie;
- comparatia cu modele Dummy;
- cross-validation, calibrare, erori grave si feature importance;
- criteriile interne de acceptare ale proiectului.

Rapoartele sunt salvate in format JSON si text in directorul ``reports``.
"""

from __future__ import annotations

import hashlib
import json
import platform
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Mapping, Sequence, TypeAlias, cast

import joblib
import numpy as np
import pandas as pd
import sklearn
from sklearn.base import clone
from sklearn.dummy import DummyClassifier, DummyRegressor
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    log_loss,
    make_scorer,
    matthews_corrcoef,
    mean_absolute_error,
    mean_squared_error,
    median_absolute_error,
    precision_score,
    r2_score,
    recall_score,
    roc_auc_score,
    top_k_accuracy_score,
)
from sklearn.model_selection import (
    KFold,
    StratifiedKFold,
    cross_validate,
    train_test_split,
)
from sklearn.preprocessing import label_binarize


JsonObject: TypeAlias = dict[str, Any]
ClassifierSplit: TypeAlias = tuple[
    pd.DataFrame,
    pd.DataFrame,
    pd.Series,
    pd.Series,
]
RegressionSplit: TypeAlias = tuple[
    pd.DataFrame,
    pd.DataFrame,
    pd.DataFrame,
    pd.DataFrame,
]


# ---------------------------------------------------------------------------
# Fisiere, coloane si parametri
# ---------------------------------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent
DATA_FILE = BASE_DIR / "data" / "synthetic_horeca_dataset.csv"

MODELS_DIR = BASE_DIR / "models"
REPORTS_DIR = BASE_DIR / "reports"

TRAFFIC_MODEL_FILE = MODELS_DIR / "traffic_model.pkl"
STAFF_MODEL_FILE = MODELS_DIR / "staff_model.pkl"
DELAY_MODEL_FILE = MODELS_DIR / "delay_model.pkl"

JSON_REPORT_FILE = REPORTS_DIR / "model_evaluation_bia.json"
TEXT_REPORT_FILE = REPORTS_DIR / "model_evaluation_bia.txt"

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
    "recommended_waiters",
    "recommended_kitchen_staff",
    "recommended_bar_staff",
]

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

TRAFFIC_TARGET_COLUMN = "traffic_level"
DELAY_TARGET_COLUMN = "delay_risk"

CLASS_LABELS = ["SCAZUT", "MEDIU", "RIDICAT"]
PROBABILITY_LABELS = sorted(CLASS_LABELS)

RANDOM_STATE = 42
CROSS_VALIDATION_FOLDS = 5
CALIBRATION_BINS = 10


class EvaluationError(Exception):
    """Eroare controlata aparuta in timpul evaluarii modelelor."""


# ---------------------------------------------------------------------------
# Validarea fisierelor, datasetului si modelelor
# ---------------------------------------------------------------------------

def sha256_file(file_path: Path) -> str:
    """Calculeaza amprenta SHA-256 a unui fisier."""

    digest = hashlib.sha256()

    with file_path.open("rb") as file:
        while chunk := file.read(1024 * 1024):
            digest.update(chunk)

    return digest.hexdigest()


def validate_files() -> None:
    """Verifica existenta datasetului si a celor trei modele."""

    required_files = [
        DATA_FILE,
        TRAFFIC_MODEL_FILE,
        STAFF_MODEL_FILE,
        DELAY_MODEL_FILE,
    ]
    missing_files = [
        str(file_path)
        for file_path in required_files
        if not file_path.exists()
    ]

    if missing_files:
        raise EvaluationError(
            f"Lipsesc fisiere obligatorii: {missing_files}"
        )


def validate_dataset(dataframe: pd.DataFrame) -> None:
    """Verifica structura, valorile lipsa si etichetele datasetului."""

    required_columns = list(
        dict.fromkeys(
            [
                *BASE_FEATURE_COLUMNS,
                *STAFF_TARGET_COLUMNS,
                *DELAY_FEATURE_COLUMNS,
                TRAFFIC_TARGET_COLUMN,
                DELAY_TARGET_COLUMN,
            ]
        )
    )

    missing_columns = [
        column
        for column in required_columns
        if column not in dataframe.columns
    ]
    if missing_columns:
        raise EvaluationError(
            "Datasetul nu contine coloanele obligatorii: "
            f"{missing_columns}"
        )

    if dataframe.empty:
        raise EvaluationError("Datasetul este gol.")

    missing_value_columns = [
        column
        for column in required_columns
        if dataframe[column].isnull().any()
    ]
    if missing_value_columns:
        raise EvaluationError(
            "Datasetul contine valori lipsa in coloanele: "
            f"{missing_value_columns}"
        )

    for target_column in [TRAFFIC_TARGET_COLUMN, DELAY_TARGET_COLUMN]:
        labels = set(dataframe[target_column].astype(str))
        invalid_labels = sorted(labels - set(CLASS_LABELS))

        if invalid_labels:
            raise EvaluationError(
                f"Coloana {target_column} contine etichete invalide: "
                f"{invalid_labels}"
            )


def load_model(model_file: Path) -> Any:
    """Incarca un model joblib dupa verificarea existentei fisierului."""

    if not model_file.exists():
        raise EvaluationError(f"Modelul nu exista: {model_file}")

    return joblib.load(model_file)


def validate_model_feature_count(
        model: Any,
        expected_count: int,
        model_name: str,
) -> None:
    """Verifica numarul de caracteristici declarat de model."""

    actual_count = getattr(model, "n_features_in_", None)

    if actual_count is not None and int(actual_count) != expected_count:
        raise EvaluationError(
            f"Modelul {model_name} asteapta {int(actual_count)} "
            f"caracteristici, dar evaluarea foloseste {expected_count}."
        )


# ---------------------------------------------------------------------------
# Utilitare pentru clasificare
# ---------------------------------------------------------------------------

def align_probabilities(
        model: Any,
        features: pd.DataFrame,
) -> np.ndarray | None:
    """
    Reordoneaza probabilitatile modelului dupa ordinea PROBABILITY_LABELS.

    Modelele pot salva clasele intr-o ordine diferita. Alinierea este
    obligatorie pentru ROC-AUC, log loss, Brier si top-k accuracy.
    """

    predict_proba = getattr(model, "predict_proba", None)
    if not callable(predict_proba):
        return None

    raw_probabilities = np.asarray(
        predict_proba(features),
        dtype=float,
    )
    model_classes = [str(label) for label in model.classes_]

    aligned_probabilities = np.zeros(
        (len(features), len(PROBABILITY_LABELS)),
        dtype=float,
    )

    for target_index, label in enumerate(PROBABILITY_LABELS):
        if label not in model_classes:
            raise EvaluationError(
                f"Modelul nu contine clasa obligatorie: {label}"
            )

        source_index = model_classes.index(label)
        aligned_probabilities[:, target_index] = raw_probabilities[
                                                 :,
                                                 source_index,
                                                 ]

    return aligned_probabilities


def calculate_ece(
        expected_values: Sequence[Any] | pd.Series,
        predictions: Sequence[Any] | np.ndarray,
        probabilities: np.ndarray,
        number_of_bins: int = CALIBRATION_BINS,
) -> tuple[float, list[JsonObject]]:
    """Calculeaza Expected Calibration Error si detaliile fiecarui interval."""

    confidences = np.asarray(
        probabilities.max(axis=1),
        dtype=float,
    )
    expected_array = np.asarray(expected_values, dtype=object)
    prediction_array = np.asarray(predictions, dtype=object)
    correctness = np.asarray(
        np.equal(expected_array, prediction_array),
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
                (confidences >= lower_bound)
                & (confidences <= upper_bound),
                dtype=bool,
                )
        else:
            in_bin = np.asarray(
                (confidences > lower_bound)
                & (confidences <= upper_bound),
                dtype=bool,
                )

        count = int(np.count_nonzero(in_bin))
        if count == 0:
            continue

        bin_accuracy = float(np.mean(correctness[in_bin]))
        bin_confidence = float(np.mean(confidences[in_bin]))
        weight = count / len(confidences)
        absolute_gap = abs(bin_accuracy - bin_confidence)

        calibration_error += weight * absolute_gap
        bin_details.append(
            {
                "lowerBound": lower_bound,
                "upperBound": upper_bound,
                "count": count,
                "accuracy": bin_accuracy,
                "meanConfidence": bin_confidence,
                "absoluteGap": absolute_gap,
            }
        )

    return float(calibration_error), bin_details


def calculate_multiclass_brier(
        expected_values: Sequence[Any] | pd.Series,
        probabilities: np.ndarray,
) -> float:
    """Calculeaza scorul Brier pentru clasificarea multiclass."""

    encoded_expected = np.asarray(
        label_binarize(expected_values, classes=PROBABILITY_LABELS),
        dtype=float,
    )
    squared_errors = np.square(probabilities - encoded_expected)

    return float(np.mean(np.sum(squared_errors, axis=1)))


def calculate_severe_errors(
        expected_values: Sequence[Any] | pd.Series,
        predictions: Sequence[Any] | np.ndarray,
) -> JsonObject:
    """Numara confuziile directe SCAZUT <-> RIDICAT."""

    expected_array = np.asarray(expected_values, dtype=object)
    prediction_array = np.asarray(predictions, dtype=object)

    severe_mask = np.asarray(
        (
                (expected_array == "SCAZUT")
                & (prediction_array == "RIDICAT")
        )
        | (
                (expected_array == "RIDICAT")
                & (prediction_array == "SCAZUT")
        ),
        dtype=bool,
        )

    count = int(np.count_nonzero(severe_mask))

    return {
        "count": count,
        "rate": float(count / len(expected_array)),
    }


def calculate_classifier_metrics(
        model: Any,
        features: pd.DataFrame,
        expected_values: pd.Series,
) -> tuple[JsonObject, np.ndarray]:
    """Calculeaza toate metricile folosite pentru un model clasificator."""

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

    metrics: JsonObject = {
        "accuracy": float(accuracy_score(expected_values, predictions)),
        "balancedAccuracy": float(
            balanced_accuracy_score(expected_values, predictions)
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
        "mcc": float(matthews_corrcoef(expected_values, predictions)),
        "confusionMatrix": confusion_matrix(
            expected_values,
            predictions,
            labels=CLASS_LABELS,
        ).tolist(),
        "classMetrics": class_metrics,
        "severeErrors": calculate_severe_errors(
            expected_values,
            predictions,
        ),
    }

    if probabilities is not None:
        ece, calibration_bins = calculate_ece(
            expected_values,
            predictions,
            probabilities,
        )
        confidences = np.asarray(
            probabilities.max(axis=1),
            dtype=float,
        )
        correct_mask = np.asarray(
            np.equal(
                np.asarray(expected_values, dtype=object),
                predictions,
            ),
            dtype=bool,
        )
        incorrect_mask = np.logical_not(correct_mask)

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
                "logLoss": float(
                    log_loss(
                        expected_values,
                        probabilities,
                        labels=PROBABILITY_LABELS,
                    )
                ),
                "multiclassBrierScore": calculate_multiclass_brier(
                    expected_values,
                    probabilities,
                ),
                "ece": ece,
                "top2Accuracy": float(
                    top_k_accuracy_score(
                        expected_values,
                        probabilities,
                        k=2,
                        labels=PROBABILITY_LABELS,
                    )
                ),
                "meanConfidence": float(np.mean(confidences)),
                "meanConfidenceCorrect": (
                    float(np.mean(confidences[correct_mask]))
                    if np.any(correct_mask)
                    else None
                ),
                "meanConfidenceIncorrect": (
                    float(np.mean(confidences[incorrect_mask]))
                    if np.any(incorrect_mask)
                    else None
                ),
                "calibrationBins": calibration_bins,
            }
        )

    return metrics, predictions


def build_classifier_cv_results(
        model: Any,
        features: pd.DataFrame,
        target: pd.Series,
) -> JsonObject:
    """Ruleaza cross-validation stratificat pentru un clasificator."""

    cross_validation = StratifiedKFold(
        n_splits=CROSS_VALIDATION_FOLDS,
        shuffle=True,
        random_state=RANDOM_STATE,
    )
    scoring = {
        "accuracy": "accuracy",
        "balancedAccuracy": "balanced_accuracy",
        "macroF1": "f1_macro",
        "mcc": make_scorer(matthews_corrcoef),
    }

    results = cross_validate(
        clone(model),
        features,
        target,
        cv=cross_validation,
        scoring=scoring,
        n_jobs=-1,
        error_score="raise",
    )

    formatted_results: JsonObject = {}
    for metric_name in scoring:
        values = np.asarray(
            results[f"test_{metric_name}"],
            dtype=float,
        )
        formatted_results[metric_name] = {
            "mean": float(np.mean(values)),
            "std": float(np.std(values)),
            "foldValues": [float(value) for value in values],
        }

    return formatted_results


# ---------------------------------------------------------------------------
# Utilitare pentru regresia multi-output a personalului
# ---------------------------------------------------------------------------

def calculate_staff_metrics(
        expected_values: pd.DataFrame | np.ndarray,
        predictions: np.ndarray,
) -> JsonObject:
    """Calculeaza metrici generale si individuale pentru cele trei roluri."""

    expected_array = np.asarray(expected_values, dtype=float)
    prediction_array = np.asarray(predictions, dtype=float)

    rounded_expected = np.rint(expected_array).astype(int)
    rounded_predictions = np.maximum(
        0,
        np.rint(prediction_array).astype(int),
    )
    absolute_errors = np.abs(prediction_array - expected_array)
    rounded_errors = rounded_predictions - rounded_expected

    per_target: JsonObject = {}

    for index, target_column in enumerate(STAFF_TARGET_COLUMNS):
        target_expected = expected_array[:, index]
        target_predictions = prediction_array[:, index]
        target_absolute_errors = absolute_errors[:, index]
        target_rounded_errors = rounded_errors[:, index]

        per_target[target_column] = {
            "mae": float(
                mean_absolute_error(target_expected, target_predictions)
            ),
            "rmse": float(
                mean_squared_error(target_expected, target_predictions)
                ** 0.5
            ),
            "r2": float(r2_score(target_expected, target_predictions)),
            "medianAbsoluteError": float(
                median_absolute_error(target_expected, target_predictions)
            ),
            "p90AbsoluteError": float(
                np.percentile(target_absolute_errors, 90)
            ),
            "p95AbsoluteError": float(
                np.percentile(target_absolute_errors, 95)
            ),
            "roundedExactRate": float(
                np.mean(target_rounded_errors == 0)
            ),
            "roundedWithinOneRate": float(
                np.mean(np.abs(target_rounded_errors) <= 1)
            ),
            "underestimateRate": float(
                np.mean(target_rounded_errors < 0)
            ),
            "overestimateRate": float(
                np.mean(target_rounded_errors > 0)
            ),
            "maximumRoundedError": int(
                np.max(np.abs(target_rounded_errors))
            ),
        }

    return {
        "overallMae": float(
            mean_absolute_error(expected_array, prediction_array)
        ),
        "overallRmse": float(
            mean_squared_error(expected_array, prediction_array) ** 0.5
        ),
        "overallR2": float(
            r2_score(
                expected_array,
                prediction_array,
                multioutput="uniform_average",
            )
        ),
        "medianAbsoluteError": float(np.median(absolute_errors)),
        "p90AbsoluteError": float(np.percentile(absolute_errors, 90)),
        "p95AbsoluteError": float(np.percentile(absolute_errors, 95)),
        "roundedExactCellRate": float(np.mean(rounded_errors == 0)),
        "roundedWithinOneCellRate": float(
            np.mean(np.abs(rounded_errors) <= 1)
        ),
        "allThreeRoundedExactRate": float(
            np.mean(np.all(rounded_errors == 0, axis=1))
        ),
        "underestimateCellRate": float(np.mean(rounded_errors < 0)),
        "overestimateCellRate": float(np.mean(rounded_errors > 0)),
        "maximumRoundedError": int(np.max(np.abs(rounded_errors))),
        "perTarget": per_target,
    }


def build_staff_cv_results(
        model: Any,
        features: pd.DataFrame,
        target: pd.DataFrame,
) -> JsonObject:
    """Ruleaza cross-validation pentru modelul multi-output de personal."""

    cross_validation = KFold(
        n_splits=CROSS_VALIDATION_FOLDS,
        shuffle=True,
        random_state=RANDOM_STATE,
    )

    def rmse_metric(
            expected_values: np.ndarray,
            predictions: np.ndarray,
    ) -> float:
        return float(
            mean_squared_error(expected_values, predictions) ** 0.5
        )

    scoring = {
        "mae": make_scorer(
            mean_absolute_error,
            greater_is_better=False,
        ),
        "rmse": make_scorer(
            rmse_metric,
            greater_is_better=False,
        ),
        "r2": "r2",
    }

    results = cross_validate(
        clone(model),
        features,
        target,
        cv=cross_validation,
        scoring=scoring,
        n_jobs=-1,
        error_score="raise",
    )

    formatted_results: JsonObject = {}
    for metric_name in scoring:
        values = np.asarray(
            results[f"test_{metric_name}"],
            dtype=float,
        )

        if metric_name in {"mae", "rmse"}:
            values = -values

        formatted_results[metric_name] = {
            "mean": float(np.mean(values)),
            "std": float(np.std(values)),
            "foldValues": [float(value) for value in values],
        }

    return formatted_results


# ---------------------------------------------------------------------------
# Feature importance
# ---------------------------------------------------------------------------

def _extract_single_estimator_importance(
        estimator: Any,
        feature_columns: Sequence[str],
) -> np.ndarray | None:
    """Extrage si aliniaza importanta unui estimator individual."""

    raw_importances = getattr(estimator, "feature_importances_", None)
    if raw_importances is None:
        return None

    importances = np.asarray(raw_importances, dtype=float).reshape(-1)
    selected_names = getattr(estimator, "selected_feature_names_", None)

    if selected_names is not None:
        selected_names = [str(name) for name in selected_names]

        if len(selected_names) != len(importances):
            return None

        aligned = np.zeros(len(feature_columns), dtype=float)
        feature_index = {
            feature: index
            for index, feature in enumerate(feature_columns)
        }

        for feature_name, importance in zip(
                selected_names,
                importances,
                strict=True,
        ):
            index = feature_index.get(feature_name)
            if index is not None:
                aligned[index] = float(importance)

        return aligned

    if len(importances) == len(feature_columns):
        return importances

    return None


def extract_feature_importance(
        model: Any,
        feature_columns: Sequence[str],
) -> list[JsonObject]:
    """Extrage feature importance pentru modele simple sau multi-output."""

    direct_importance = _extract_single_estimator_importance(
        model,
        feature_columns,
    )

    if direct_importance is not None:
        importances = direct_importance
    else:
        estimators = getattr(model, "estimators_", None)
        if estimators is None:
            return []

        estimator_importances = [
            importance
            for estimator in estimators
            if (
                   importance := _extract_single_estimator_importance(
                       estimator,
                       feature_columns,
                   )
               )
               is not None
        ]

        if not estimator_importances:
            return []

        importances = np.mean(
            np.vstack(estimator_importances),
            axis=0,
        )

    pairs = sorted(
        zip(feature_columns, importances, strict=True),
        key=lambda pair: pair[1],
        reverse=True,
    )

    return [
        {
            "feature": feature,
            "importance": float(importance),
        }
        for feature, importance in pairs
    ]


# ---------------------------------------------------------------------------
# Impartirea determinista a datasetului
# ---------------------------------------------------------------------------

def split_classifier_data(
        features: pd.DataFrame,
        target: pd.Series,
) -> ClassifierSplit:
    """Imparte datele de clasificare in 70% train, 15% validation, 15% test."""

    first_split = train_test_split(
        features,
        target,
        test_size=0.30,
        random_state=RANDOM_STATE,
        stratify=target,
    )
    features_train = cast(pd.DataFrame, first_split[0])
    features_temporary = cast(pd.DataFrame, first_split[1])
    target_train = cast(pd.Series, first_split[2])
    target_temporary = cast(pd.Series, first_split[3])

    second_split = train_test_split(
        features_temporary,
        target_temporary,
        test_size=0.50,
        random_state=RANDOM_STATE,
        stratify=target_temporary,
    )
    features_validation = cast(pd.DataFrame, second_split[0])
    features_test = cast(pd.DataFrame, second_split[1])
    target_validation = cast(pd.Series, second_split[2])
    target_test = cast(pd.Series, second_split[3])

    final_train_features = pd.concat(
        [features_train, features_validation],
        ignore_index=True,
    )
    combined_target = pd.concat(
        [target_train, target_validation],
        ignore_index=True,
    )
    final_train_target = pd.Series(
        combined_target.to_numpy(copy=True),
        name=target.name,
    )
    normalized_test_target = pd.Series(
        target_test.to_numpy(copy=True),
        index=features_test.index,
        name=target.name,
    )

    return (
        final_train_features,
        features_test,
        final_train_target,
        normalized_test_target,
    )


def split_regression_data(
        features: pd.DataFrame,
        target: pd.DataFrame,
) -> RegressionSplit:
    """Imparte datele de regresie in 70% train, 15% validation, 15% test."""

    first_split = train_test_split(
        features,
        target,
        test_size=0.30,
        random_state=RANDOM_STATE,
    )
    features_train = cast(pd.DataFrame, first_split[0])
    features_temporary = cast(pd.DataFrame, first_split[1])
    target_train = cast(pd.DataFrame, first_split[2])
    target_temporary = cast(pd.DataFrame, first_split[3])

    second_split = train_test_split(
        features_temporary,
        target_temporary,
        test_size=0.50,
        random_state=RANDOM_STATE,
    )
    features_validation = cast(pd.DataFrame, second_split[0])
    features_test = cast(pd.DataFrame, second_split[1])
    target_validation = cast(pd.DataFrame, second_split[2])
    target_test = cast(pd.DataFrame, second_split[3])

    final_train_features = pd.concat(
        [features_train, features_validation],
        ignore_index=True,
    )
    combined_target = pd.concat(
        [target_train, target_validation],
        ignore_index=True,
    )
    final_train_target = pd.DataFrame(
        combined_target.to_numpy(copy=True),
        columns=target.columns,
    )
    normalized_test_target = pd.DataFrame(
        target_test.to_numpy(copy=True),
        index=features_test.index,
        columns=target.columns,
    )

    return (
        final_train_features,
        features_test,
        final_train_target,
        normalized_test_target,
    )


# ---------------------------------------------------------------------------
# Evaluarea celor trei modele
# ---------------------------------------------------------------------------

def evaluate_traffic(
        dataframe: pd.DataFrame,
        model: Any,
) -> JsonObject:
    """Evalueaza modelul de trafic si modelul Dummy de referinta."""

    features = dataframe[BASE_FEATURE_COLUMNS]
    target = dataframe[TRAFFIC_TARGET_COLUMN]

    (
        features_train,
        features_test,
        target_train,
        target_test,
    ) = split_classifier_data(features, target)

    metrics, _ = calculate_classifier_metrics(
        model,
        features_test,
        target_test,
    )

    dummy_model = DummyClassifier(
        strategy="prior",
        random_state=RANDOM_STATE,
    )
    dummy_model.fit(features_train, target_train)
    dummy_metrics, _ = calculate_classifier_metrics(
        dummy_model,
        features_test,
        target_test,
    )

    return {
        "modelClass": type(model).__name__,
        "featureCount": len(BASE_FEATURE_COLUMNS),
        "testRows": int(len(features_test)),
        "metrics": metrics,
        "dummyBaseline": dummy_metrics,
        "crossValidation": build_classifier_cv_results(
            model,
            features,
            target,
        ),
        "featureImportance": extract_feature_importance(
            model,
            BASE_FEATURE_COLUMNS,
        ),
    }


def evaluate_staff(
        dataframe: pd.DataFrame,
        model: Any,
) -> JsonObject:
    """Evalueaza modelul de personal si regresorul Dummy de referinta."""

    features = dataframe[BASE_FEATURE_COLUMNS]
    target = dataframe[STAFF_TARGET_COLUMNS]

    (
        features_train,
        features_test,
        target_train,
        target_test,
    ) = split_regression_data(features, target)

    predictions = np.asarray(
        model.predict(features_test),
        dtype=float,
    )
    metrics = calculate_staff_metrics(target_test, predictions)

    dummy_model = DummyRegressor(strategy="mean")
    dummy_model.fit(features_train, target_train)
    dummy_predictions = np.asarray(
        dummy_model.predict(features_test),
        dtype=float,
    )
    dummy_metrics = calculate_staff_metrics(
        target_test,
        dummy_predictions,
    )

    return {
        "modelClass": type(model).__name__,
        "featureCount": len(BASE_FEATURE_COLUMNS),
        "testRows": int(len(features_test)),
        "metrics": metrics,
        "dummyBaseline": dummy_metrics,
        "crossValidation": build_staff_cv_results(
            model,
            features,
            target,
        ),
        "featureImportance": extract_feature_importance(
            model,
            BASE_FEATURE_COLUMNS,
        ),
    }


def build_production_delay_features(
        dataframe: pd.DataFrame,
        staff_model: Any,
) -> tuple[pd.DataFrame, np.ndarray]:
    """
    Recalculeaza feature-urile de intarziere exact ca serviciul Flask.

    Necesarul de personal este prezis, rotunjit si comparat cu personalul activ.
    """

    base_features = dataframe[BASE_FEATURE_COLUMNS].copy()
    raw_staff_predictions = np.asarray(
        staff_model.predict(base_features),
        dtype=float,
    )
    rounded_staff_predictions = np.maximum(
        0,
        np.rint(raw_staff_predictions).astype(int),
    )

    production_features = dataframe[DELAY_FEATURE_COLUMNS].copy()

    active_waiters = dataframe["active_waiters"].to_numpy(dtype=float)
    active_kitchen = dataframe["active_kitchen"].to_numpy(dtype=float)
    active_bar = dataframe["active_bar"].to_numpy(dtype=float)

    production_features["waiter_deficit"] = (
            rounded_staff_predictions[:, 0] - active_waiters
    )
    production_features["kitchen_deficit"] = (
            rounded_staff_predictions[:, 1] - active_kitchen
    )
    production_features["bar_deficit"] = (
            rounded_staff_predictions[:, 2] - active_bar
    )
    production_features["orders_per_waiter"] = (
            dataframe["active_orders"].to_numpy(dtype=float)
            / np.maximum(active_waiters, 1.0)
    )
    production_features["kitchen_items_per_employee"] = (
            dataframe["kitchen_load"].to_numpy(dtype=float)
            / np.maximum(active_kitchen, 1.0)
    )
    production_features["bar_items_per_employee"] = (
            dataframe["bar_load"].to_numpy(dtype=float)
            / np.maximum(active_bar, 1.0)
    )
    production_features["occupancy_per_waiter"] = (
            dataframe["occupied_tables"].to_numpy(dtype=float)
            / np.maximum(active_waiters, 1.0)
    )

    return production_features, rounded_staff_predictions


def calculate_skew_details(
        offline_features: pd.DataFrame,
        production_features: pd.DataFrame,
) -> JsonObject:
    """Compara feature-urile offline cu cele reconstruite ca in productie."""

    columns = [
        "waiter_deficit",
        "kitchen_deficit",
        "bar_deficit",
        "orders_per_waiter",
        "kitchen_items_per_employee",
        "bar_items_per_employee",
        "occupancy_per_waiter",
    ]
    skew_details: JsonObject = {}

    for column in columns:
        offline_values = offline_features[column].to_numpy(dtype=float)
        production_values = production_features[column].to_numpy(dtype=float)
        differences = production_values - offline_values

        skew_details[column] = {
            "differentRate": float(
                np.mean(
                    np.logical_not(
                        np.isclose(
                            offline_values,
                            production_values,
                            rtol=1e-9,
                            atol=1e-9,
                        )
                    )
                )
            ),
            "meanAbsoluteDifference": float(
                np.mean(np.abs(differences))
            ),
            "maximumAbsoluteDifference": float(
                np.max(np.abs(differences))
            ),
        }

    return skew_details


def evaluate_delay(
        dataframe: pd.DataFrame,
        delay_model: Any,
        staff_model: Any,
) -> JsonObject:
    """Evalueaza modelul de intarziere offline si in simularea de productie."""

    features = dataframe[DELAY_FEATURE_COLUMNS]
    target = dataframe[DELAY_TARGET_COLUMN]

    (
        features_train,
        features_test,
        target_train,
        target_test,
    ) = split_classifier_data(features, target)

    offline_metrics, offline_predictions = calculate_classifier_metrics(
        delay_model,
        features_test,
        target_test,
    )

    dummy_model = DummyClassifier(
        strategy="prior",
        random_state=RANDOM_STATE,
    )
    dummy_model.fit(features_train, target_train)
    dummy_metrics, _ = calculate_classifier_metrics(
        dummy_model,
        features_test,
        target_test,
    )

    production_source = dataframe.loc[features_test.index].copy()
    (
        production_features,
        rounded_staff_predictions,
    ) = build_production_delay_features(
        production_source,
        staff_model,
    )

    production_metrics, production_predictions = (
        calculate_classifier_metrics(
            delay_model,
            production_features,
            target_test,
        )
    )
    prediction_changed_rate = float(
        np.mean(offline_predictions != production_predictions)
    )

    staff_expected = production_source[
        STAFF_TARGET_COLUMNS
    ].to_numpy(dtype=float)
    staff_prediction_metrics = calculate_staff_metrics(
        staff_expected,
        rounded_staff_predictions,
    )

    return {
        "modelClass": type(delay_model).__name__,
        "featureCount": len(DELAY_FEATURE_COLUMNS),
        "testRows": int(len(features_test)),
        "offlineEvaluation": {
            "description": (
                "Evaluare cu feature-urile existente in dataset."
            ),
            "metrics": offline_metrics,
        },
        "productionLikeEvaluation": {
            "description": (
                "Evaluare dupa predictia personalului si recalcularea "
                "deficitelor, exact ca in app.py."
            ),
            "metrics": production_metrics,
            "predictionChangedRate": prediction_changed_rate,
            "staffPredictionMetricsOnDelayTest": staff_prediction_metrics,
            "trainServingSkew": calculate_skew_details(
                features_test,
                production_features,
            ),
        },
        "dummyBaseline": dummy_metrics,
        "crossValidationOffline": build_classifier_cv_results(
            delay_model,
            features,
            target,
        ),
        "featureImportance": extract_feature_importance(
            delay_model,
            DELAY_FEATURE_COLUMNS,
        ),
    }


# ---------------------------------------------------------------------------
# Calitatea datasetului si verdictele proiectului
# ---------------------------------------------------------------------------

def distribution_as_dictionary(series: pd.Series) -> dict[str, int]:
    """Transforma value_counts intr-un dictionar JSON serializabil."""

    return {
        str(value): int(count)
        for value, count in series.value_counts().items()
    }


def evaluate_dataset_quality(dataframe: pd.DataFrame) -> JsonObject:
    """Descrie dimensiunea, duplicatele si distributiile datasetului."""

    staff_distributions = {
        target_column: {
            str(value): int(count)
            for value, count in (
                dataframe[target_column]
                .value_counts()
                .sort_index()
                .items()
            )
        }
        for target_column in STAFF_TARGET_COLUMNS
    }

    return {
        "rows": int(len(dataframe)),
        "columns": int(len(dataframe.columns)),
        "missingValues": int(dataframe.isnull().sum().sum()),
        "duplicateRows": int(dataframe.duplicated().sum()),
        "rowsInDuplicateBaseFeatureGroups": int(
            dataframe.duplicated(
                subset=BASE_FEATURE_COLUMNS,
                keep=False,
            ).sum()
        ),
        "trafficDistribution": distribution_as_dictionary(
            dataframe[TRAFFIC_TARGET_COLUMN]
        ),
        "delayDistribution": distribution_as_dictionary(
            dataframe[DELAY_TARGET_COLUMN]
        ),
        "staffTargetDistribution": staff_distributions,
    }


def build_project_verdicts(
        traffic_results: Mapping[str, Any],
        staff_results: Mapping[str, Any],
        delay_results: Mapping[str, Any],
) -> JsonObject:
    """Aplica pragurile interne stabilite pentru proiect."""

    traffic_metrics = traffic_results["metrics"]
    staff_metrics = staff_results["metrics"]
    delay_offline_metrics = delay_results["offlineEvaluation"]["metrics"]
    delay_production_metrics = delay_results[
        "productionLikeEvaluation"
    ]["metrics"]

    traffic_pass = (
            traffic_metrics["macroF1"] >= 0.75
            and traffic_metrics["balancedAccuracy"] >= 0.75
            and traffic_metrics.get("ece", 1.0) <= 0.05
    )
    staff_pass = (
            staff_metrics["overallMae"] <= 0.50
            and staff_metrics["roundedWithinOneCellRate"] >= 0.95
    )
    delay_offline_pass = (
            delay_offline_metrics["macroF1"] >= 0.75
            and delay_offline_metrics["severeErrors"]["rate"] <= 0.01
            and delay_offline_metrics.get("ece", 1.0) <= 0.05
    )
    delay_production_pass = (
            delay_production_metrics["macroF1"] >= 0.73
            and delay_production_metrics["severeErrors"]["rate"] <= 0.02
    )

    return {
        "criteriaNotice": (
            "Aceste praguri sunt criterii interne pentru proiect, "
            "nu standarde universale."
        ),
        "traffic": {
            "passed": traffic_pass,
            "criteria": {
                "macroF1Minimum": 0.75,
                "balancedAccuracyMinimum": 0.75,
                "eceMaximum": 0.05,
            },
        },
        "staff": {
            "passed": staff_pass,
            "criteria": {
                "overallMaeMaximum": 0.50,
                "withinOneCellRateMinimum": 0.95,
            },
        },
        "delayOffline": {
            "passed": delay_offline_pass,
            "criteria": {
                "macroF1Minimum": 0.75,
                "severeErrorRateMaximum": 0.01,
                "eceMaximum": 0.05,
            },
        },
        "delayProductionLike": {
            "passed": delay_production_pass,
            "criteria": {
                "macroF1Minimum": 0.73,
                "severeErrorRateMaximum": 0.02,
            },
        },
    }


# ---------------------------------------------------------------------------
# Construirea raportului text
# ---------------------------------------------------------------------------

def format_percentage(value: float) -> str:
    """Formateaza o proportie ca procent cu doua zecimale."""

    return f"{value * 100:.2f}%"


def format_classifier_section(
        title: str,
        evaluation: Mapping[str, Any],
) -> str:
    """Formateaza sectiunea text pentru un clasificator."""

    metrics = evaluation["metrics"]
    lines = [
        title,
        "-" * len(title),
        f"Accuracy: {metrics['accuracy']:.4f}",
        f"Balanced accuracy: {metrics['balancedAccuracy']:.4f}",
        f"Macro precision: {metrics['macroPrecision']:.4f}",
        f"Macro recall: {metrics['macroRecall']:.4f}",
        f"Macro F1: {metrics['macroF1']:.4f}",
        f"Weighted F1: {metrics['weightedF1']:.4f}",
        f"MCC: {metrics['mcc']:.4f}",
        ]

    optional_metrics = [
        ("ROC-AUC macro OVR", "rocAucMacroOvr"),
        ("Log loss", "logLoss"),
        ("Brier multiclass", "multiclassBrierScore"),
        ("ECE", "ece"),
        ("Top-2 accuracy", "top2Accuracy"),
    ]

    for label, key in optional_metrics:
        if key in metrics:
            lines.append(f"{label}: {metrics[key]:.4f}")

    severe_errors = metrics["severeErrors"]
    lines.extend(
        [
            (
                "Erori grave SCAZUT <-> RIDICAT: "
                f"{severe_errors['count']} "
                f"({format_percentage(severe_errors['rate'])})"
            ),
            "",
            "Metrici pe clase:",
        ]
    )

    for label in CLASS_LABELS:
        class_metrics = metrics["classMetrics"][label]
        lines.append(
            f"- {label}: "
            f"precision={class_metrics['precision']:.4f}, "
            f"recall={class_metrics['recall']:.4f}, "
            f"F1={class_metrics['f1']:.4f}, "
            f"support={class_metrics['support']}"
        )

    lines.extend(
        [
            "",
            "Matrice de confuzie (rand real, coloana prezis):",
            json.dumps(
                metrics["confusionMatrix"],
                ensure_ascii=False,
            ),
        ]
    )

    return "\n".join(lines)


def build_text_report(report: Mapping[str, Any]) -> str:
    """Construieste raportul complet in format text."""

    dataset_quality = report["datasetQuality"]
    traffic = report["traffic"]
    staff = report["staff"]
    delay = report["delay"]

    staff_metrics = staff["metrics"]
    offline_delay = delay["offlineEvaluation"]
    production_delay = delay["productionLikeEvaluation"]

    lines = [
        "EVALUARE MODELE AI - FORMAT EXTINS TIP BIA",
        "=" * 48,
        "",
        f"Generat la: {report['generatedAtUtc']}",
        (
            "Versiuni: "
            f"Python {report['environment']['python']}, "
            f"scikit-learn {report['environment']['scikitLearn']}, "
            f"pandas {report['environment']['pandas']}, "
            f"numpy {report['environment']['numpy']}"
        ),
        "",
        "CALITATE DATASET",
        "----------------",
        f"Randuri: {dataset_quality['rows']}",
        f"Coloane: {dataset_quality['columns']}",
        f"Valori lipsa: {dataset_quality['missingValues']}",
        f"Randuri duplicate: {dataset_quality['duplicateRows']}",
        (
            "Randuri in grupuri cu aceleasi 11 caracteristici de baza: "
            f"{dataset_quality['rowsInDuplicateBaseFeatureGroups']}"
        ),
        (
            "Distributie trafic: "
            f"{dataset_quality['trafficDistribution']}"
        ),
        (
            "Distributie intarziere: "
            f"{dataset_quality['delayDistribution']}"
        ),
        "",
        format_classifier_section("MODEL TRAFIC", traffic),
        "",
        "Comparatie cu DummyClassifier:",
        (
            "Accuracy dummy: "
            f"{traffic['dummyBaseline']['accuracy']:.4f}"
        ),
        (
            "Macro F1 dummy: "
            f"{traffic['dummyBaseline']['macroF1']:.4f}"
        ),
        "",
        "Cross-validation trafic (5 folduri):",
        json.dumps(
            traffic["crossValidation"],
            ensure_ascii=False,
            indent=2,
        ),
        "",
        "MODEL PERSONAL",
        "--------------",
        f"MAE general: {staff_metrics['overallMae']:.4f}",
        f"RMSE general: {staff_metrics['overallRmse']:.4f}",
        f"R2 general: {staff_metrics['overallR2']:.4f}",
        (
            "Median absolute error: "
            f"{staff_metrics['medianAbsoluteError']:.4f}"
        ),
        (
            "Percentila 90 eroare absoluta: "
            f"{staff_metrics['p90AbsoluteError']:.4f}"
        ),
        (
            "Exact dupa rotunjire, per valoare: "
            f"{format_percentage(staff_metrics['roundedExactCellRate'])}"
        ),
        (
            "In limita +/-1 angajat, per valoare: "
            f"{format_percentage(staff_metrics['roundedWithinOneCellRate'])}"
        ),
        (
            "Toate cele 3 valori exacte simultan: "
            f"{format_percentage(staff_metrics['allThreeRoundedExactRate'])}"
        ),
        (
            "Subestimare: "
            f"{format_percentage(staff_metrics['underestimateCellRate'])}"
        ),
        (
            "Supraestimare: "
            f"{format_percentage(staff_metrics['overestimateCellRate'])}"
        ),
        (
            "Eroare rotunjita maxima: "
            f"{staff_metrics['maximumRoundedError']}"
        ),
        "",
        "Metrici pe rol:",
        json.dumps(
            staff_metrics["perTarget"],
            ensure_ascii=False,
            indent=2,
        ),
        "",
        "Comparatie cu DummyRegressor:",
        (
            "MAE dummy: "
            f"{staff['dummyBaseline']['overallMae']:.4f}"
        ),
        (
            "RMSE dummy: "
            f"{staff['dummyBaseline']['overallRmse']:.4f}"
        ),
        (
            "R2 dummy: "
            f"{staff['dummyBaseline']['overallR2']:.4f}"
        ),
        "",
        "Cross-validation personal (5 folduri):",
        json.dumps(
            staff["crossValidation"],
            ensure_ascii=False,
            indent=2,
        ),
        "",
        format_classifier_section(
            "MODEL INTARZIERE - EVALUARE OFFLINE",
            offline_delay,
        ),
        "",
        format_classifier_section(
            "MODEL INTARZIERE - SIMULARE PRODUCTIE",
            production_delay,
        ),
        "",
        (
            "Predictii schimbate intre offline si productie: "
            f"{format_percentage(production_delay['predictionChangedRate'])}"
        ),
        "",
        "Train-serving skew:",
        json.dumps(
            production_delay["trainServingSkew"],
            ensure_ascii=False,
            indent=2,
        ),
        "",
        "Cross-validation intarziere offline (5 folduri):",
        json.dumps(
            delay["crossValidationOffline"],
            ensure_ascii=False,
            indent=2,
        ),
        "",
        "VERDICTE PE CRITERIILE INTERNE ALE PROIECTULUI",
        "----------------------------------------------",
        json.dumps(
            report["projectVerdicts"],
            ensure_ascii=False,
            indent=2,
        ),
        "",
        "Observatie:",
        (
            "Evaluarea offline arata performanta pe feature-urile din "
            "dataset. Simularea de productie este mai importanta pentru "
            "modelul de intarziere, deoarece foloseste necesarul de "
            "personal prezis de modelul de personal, exact ca aplicatia."
        ),
        ]

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Orchestrarea evaluarii si salvarea rapoartelor
# ---------------------------------------------------------------------------

def build_file_metadata() -> JsonObject:
    """Construieste metadatele si hash-urile fisierelor evaluate."""

    return {
        "dataset": {
            "path": str(DATA_FILE),
            "sha256": sha256_file(DATA_FILE),
        },
        "trafficModel": {
            "path": str(TRAFFIC_MODEL_FILE),
            "sha256": sha256_file(TRAFFIC_MODEL_FILE),
        },
        "staffModel": {
            "path": str(STAFF_MODEL_FILE),
            "sha256": sha256_file(STAFF_MODEL_FILE),
        },
        "delayModel": {
            "path": str(DELAY_MODEL_FILE),
            "sha256": sha256_file(DELAY_MODEL_FILE),
        },
    }


def evaluate_all_models() -> JsonObject:
    """Ruleaza evaluarea completa si scrie cele doua rapoarte."""

    validate_files()

    dataframe = pd.read_csv(DATA_FILE)
    validate_dataset(dataframe)

    traffic_model = load_model(TRAFFIC_MODEL_FILE)
    staff_model = load_model(STAFF_MODEL_FILE)
    delay_model = load_model(DELAY_MODEL_FILE)

    validate_model_feature_count(
        traffic_model,
        len(BASE_FEATURE_COLUMNS),
        "de trafic",
    )
    validate_model_feature_count(
        staff_model,
        len(BASE_FEATURE_COLUMNS),
        "de personal",
    )
    validate_model_feature_count(
        delay_model,
        len(DELAY_FEATURE_COLUMNS),
        "de intarziere",
    )

    traffic_results = evaluate_traffic(dataframe, traffic_model)
    staff_results = evaluate_staff(dataframe, staff_model)
    delay_results = evaluate_delay(
        dataframe,
        delay_model,
        staff_model,
    )

    project_verdicts = build_project_verdicts(
        traffic_results,
        staff_results,
        delay_results,
    )

    report: JsonObject = {
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "environment": {
            "python": platform.python_version(),
            "scikitLearn": sklearn.__version__,
            "pandas": pd.__version__,
            "numpy": np.__version__,
        },
        "files": build_file_metadata(),
        "datasetQuality": evaluate_dataset_quality(dataframe),
        "traffic": traffic_results,
        "staff": staff_results,
        "delay": delay_results,
        "projectVerdicts": project_verdicts,
    }

    REPORTS_DIR.mkdir(parents=True, exist_ok=True)

    JSON_REPORT_FILE.write_text(
        json.dumps(
            report,
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    TEXT_REPORT_FILE.write_text(
        build_text_report(report),
        encoding="utf-8",
    )

    print("Evaluarea extinsa a fost finalizata.")
    print(f"Raport JSON: {JSON_REPORT_FILE}")
    print(f"Raport text: {TEXT_REPORT_FILE}")
    print()
    print(
        "Trafic - Macro F1: "
        f"{traffic_results['metrics']['macroF1']:.4f}"
    )
    print(
        "Personal - MAE: "
        f"{staff_results['metrics']['overallMae']:.4f}"
    )
    print(
        "Intarziere offline - Macro F1: "
        f"{delay_results['offlineEvaluation']['metrics']['macroF1']:.4f}"
    )
    print(
        "Intarziere productie - Macro F1: "
        f"{delay_results['productionLikeEvaluation']['metrics']['macroF1']:.4f}"
    )

    return report


def main() -> None:
    """Punctul de intrare al scriptului."""

    try:
        evaluate_all_models()
    except EvaluationError as exception:
        print(f"Evaluarea a fost oprita: {exception}")
        raise SystemExit(1) from exception
    except Exception as exception:
        print(f"Eroare la evaluarea modelelor: {exception}")
        raise


if __name__ == "__main__":
    main()