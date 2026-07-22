"""
Antreneaza modelul pentru estimarea nivelului de trafic HoReCa.

Scriptul:
1. valideaza datasetul;
2. imparte datele in train, validation si test;
3. compara modelele candidate cu un DummyClassifier;
4. foloseste cross-validation stratificat pentru selectia finala;
5. evalueaza modelul ales pe setul de test;
6. calculeaza calibrarea, ROC-AUC, MCC si importanta caracteristicilor;
7. salveaza modelul, metadatele si raportul text.

Logica, parametrii modelelor si criteriile de selectie au fost pastrate.
Modificarile sunt de tipizare, comentare, validare si formatare.
"""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Mapping, Protocol, Sequence, TypeAlias, cast

import joblib
import numpy as np
import pandas as pd
from sklearn.base import clone
from sklearn.dummy import DummyClassifier
from sklearn.ensemble import (
    ExtraTreesClassifier,
    GradientBoostingClassifier,
    RandomForestClassifier,
)
from sklearn.inspection import permutation_importance
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
    log_loss,
    matthews_corrcoef,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import (
    StratifiedKFold,
    cross_validate,
    train_test_split,
)


JsonObject: TypeAlias = dict[str, Any]
Metrics: TypeAlias = dict[str, Any]
CrossValidationMetrics: TypeAlias = dict[str, dict[str, Any]]


class ClassifierModel(Protocol):
    """Interfata minima folosita de clasificatoarele din acest script."""

    classes_: Any

    def fit(self, features: Any, target: Any) -> Any:
        """Antreneaza clasificatorul."""

    def predict(self, features: Any) -> Any:
        """Returneaza clasele prezise."""

    def predict_proba(self, features: Any) -> Any:
        """Returneaza probabilitatile claselor."""


CandidateModels: TypeAlias = dict[str, ClassifierModel]


# ---------------------------------------------------------------------------
# Fisiere si configurare
# ---------------------------------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent

DATA_FILE = BASE_DIR / "data" / "synthetic_horeca_dataset.csv"
MODEL_FILE = BASE_DIR / "models" / "traffic_model.pkl"
REPORT_FILE = BASE_DIR / "reports" / "traffic_metrics.txt"
METADATA_FILE = BASE_DIR / "models" / "traffic_model_metadata.json"

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

TARGET_COLUMN = "traffic_level"

CLASS_LABELS = [
    "SCAZUT",
    "MEDIU",
    "RIDICAT",
]

# Scikit-learn foloseste ordinea lexicografica a claselor pentru coloanele
# probabilitatilor. Reordonarea explicita evita interpretarea gresita a AUC.
PROBABILITY_LABELS = sorted(CLASS_LABELS)

RANDOM_STATE = 42
CALIBRATION_BINS = 10
CROSS_VALIDATION_FOLDS = 5


# ---------------------------------------------------------------------------
# Validarea datasetului
# ---------------------------------------------------------------------------


def validate_dataset(dataframe: pd.DataFrame) -> None:
    """Verifica structura, tipurile si distributia claselor."""

    required_columns = [*FEATURE_COLUMNS, TARGET_COLUMN]

    missing_columns = [
        column for column in required_columns if column not in dataframe.columns
    ]
    if missing_columns:
        raise ValueError(
            f"Datasetul nu contine coloanele obligatorii: {missing_columns}"
        )

    if dataframe.empty:
        raise ValueError("Datasetul este gol.")

    columns_with_missing_values = [
        column for column in required_columns if bool(dataframe[column].isnull().any())
    ]
    if columns_with_missing_values:
        raise ValueError(
            "Datasetul contine valori lipsa in coloanele: "
            f"{columns_with_missing_values}"
        )

    non_numeric_features = [
        column
        for column in FEATURE_COLUMNS
        if not pd.api.types.is_numeric_dtype(dataframe[column])
    ]
    if non_numeric_features:
        raise ValueError(
            "Caracteristicile trebuie sa fie numerice. "
            f"Coloane invalide: {non_numeric_features}"
        )

    target = cast(pd.Series, dataframe[TARGET_COLUMN])
    invalid_labels = sorted(set(target.astype(str)) - set(CLASS_LABELS))
    if invalid_labels:
        raise ValueError(f"Datasetul contine etichete neacceptate: {invalid_labels}")

    class_counts = target.value_counts()

    missing_classes = [
        label for label in CLASS_LABELS if label not in class_counts.index
    ]
    if missing_classes:
        raise ValueError(f"Datasetul nu contine toate clasele: {missing_classes}")

    too_small_classes = {
        label: int(class_counts[label])
        for label in CLASS_LABELS
        if int(class_counts[label]) < 20
    }
    if too_small_classes:
        raise ValueError(
            "Fiecare clasa trebuie sa contina cel putin 20 de exemple. "
            f"Clase insuficiente: {too_small_classes}"
        )


# ---------------------------------------------------------------------------
# Modelele candidate
# ---------------------------------------------------------------------------


def build_candidate_models() -> CandidateModels:
    """Construieste cele trei clasificatoare comparate la antrenare."""

    # cast() este folosit numai pentru analiza statica din IntelliJ.
    # Toate modelele concrete implementeaza metodele protocolului.
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


def clone_classifier(model: ClassifierModel) -> ClassifierModel:
    """Cloneaza un estimator pastrand tipul necesar analizei statice."""

    return cast(ClassifierModel, clone(model))


# ---------------------------------------------------------------------------
# Probabilitati si calibrare
# ---------------------------------------------------------------------------


def align_probabilities(
    model: ClassifierModel,
    features: pd.DataFrame,
) -> np.ndarray:
    """Reordoneaza probabilitatile dupa PROBABILITY_LABELS."""

    raw_probabilities = np.asarray(
        model.predict_proba(features),
        dtype=float,
    )
    model_classes = [str(label) for label in np.asarray(model.classes_, dtype=object)]

    if raw_probabilities.ndim != 2:
        raise ValueError("Modelul trebuie sa returneze o matrice de probabilitati.")

    aligned_probabilities = np.zeros(
        (len(features), len(PROBABILITY_LABELS)),
        dtype=float,
    )

    for target_index, label in enumerate(PROBABILITY_LABELS):
        if label not in model_classes:
            raise ValueError(f"Modelul nu contine clasa obligatorie: {label}")

        source_index = model_classes.index(label)
        aligned_probabilities[:, target_index] = raw_probabilities[
            :,
            source_index,
        ]

    return aligned_probabilities


def calculate_expected_calibration_error(
    expected_values: pd.Series | Sequence[str],
    predictions: Sequence[str] | np.ndarray,
    probabilities: np.ndarray,
    number_of_bins: int = CALIBRATION_BINS,
) -> tuple[float, list[JsonObject]]:
    """Calculeaza Expected Calibration Error si detaliile intervalelor."""

    confidences = np.asarray(
        probabilities.max(axis=1),
        dtype=float,
    )
    expected_array = np.asarray(
        expected_values,
        dtype=object,
    )
    prediction_array = np.asarray(
        predictions,
        dtype=object,
    )
    correctness = np.asarray(
        np.equal(expected_array, prediction_array),
        dtype=bool,
    )

    bin_edges = np.linspace(
        0.0,
        1.0,
        number_of_bins + 1,
    )
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
    expected_values: pd.Series | Sequence[str],
    probabilities: np.ndarray,
) -> float:
    """Calculeaza scorul Brier pentru clasificarea cu trei clase."""

    label_to_index = {label: index for index, label in enumerate(PROBABILITY_LABELS)}

    one_hot_expected = np.zeros_like(
        probabilities,
        dtype=float,
    )

    for row_index, label in enumerate(expected_values):
        normalized_label = str(label)

        if normalized_label not in label_to_index:
            raise ValueError(f"Eticheta necunoscuta: {normalized_label}")

        one_hot_expected[
            row_index,
            label_to_index[normalized_label],
        ] = 1.0

    squared_errors = np.square(probabilities - one_hot_expected)

    return float(np.mean(np.sum(squared_errors, axis=1)))


# ---------------------------------------------------------------------------
# Metricile clasificatorului
# ---------------------------------------------------------------------------


def calculate_metrics(
    model: ClassifierModel,
    features: pd.DataFrame,
    expected_values: pd.Series,
) -> Metrics:
    """Calculeaza toate metricile folosite pentru evaluarea traficului."""

    predictions = np.asarray(
        model.predict(features),
        dtype=object,
    )
    probabilities = align_probabilities(
        model,
        features,
    )

    report_output = classification_report(
        expected_values,
        predictions,
        labels=CLASS_LABELS,
        output_dict=True,
        zero_division=0,
    )

    # Stubs-urile scikit-learn declara si varianta str. Cu output_dict=True,
    # rezultatul real este un dictionar.
    report_dictionary = cast(
        dict[str, Any],
        report_output,
    )

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

    expected_calibration_error, calibration_bins = calculate_expected_calibration_error(
        expected_values,
        predictions,
        probabilities,
    )

    metrics: Metrics = {
        "accuracy": float(
            accuracy_score(
                expected_values,
                predictions,
            )
        ),
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
        "logLoss": float(
            log_loss(
                expected_values,
                probabilities,
                labels=PROBABILITY_LABELS,
            )
        ),
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
        "multiclassBrierScore": (
            calculate_multiclass_brier_score(
                expected_values,
                probabilities,
            )
        ),
        "expectedCalibrationError": (expected_calibration_error),
        "averageConfidence": float(np.mean(probabilities.max(axis=1))),
        "calibrationBins": calibration_bins,
    }

    return metrics


# ---------------------------------------------------------------------------
# Formatarea rapoartelor
# ---------------------------------------------------------------------------


def format_confusion_matrix(
    matrix: Sequence[Sequence[Any]],
) -> str:
    """Formateaza matricea de confuzie ca tabel text."""

    header = "real\\prezis" + "".join(f"{label:>12}" for label in CLASS_LABELS)
    rows = [header]

    for label, values in zip(
        CLASS_LABELS,
        matrix,
        strict=True,
    ):
        row = f"{label:<12}" + "".join(f"{value:>12}" for value in values)
        rows.append(row)

    return "\n".join(rows)


def format_metrics(
    model_name: str,
    metrics: Mapping[str, Any],
) -> str:
    """Formateaza metricile unui model pentru raportul text."""

    balanced_accuracy_line = f"Balanced accuracy: {metrics['balancedAccuracy']:.4f}"
    mcc_line = (
        "Matthews Correlation Coefficient: "
        f"{metrics['matthewsCorrelationCoefficient']:.4f}"
    )
    weighted_auc_line = f"ROC-AUC weighted OVR: {metrics['rocAucWeightedOvr']:.4f}"
    brier_line = f"Multiclass Brier score: {metrics['multiclassBrierScore']:.4f}"
    calibration_line = (
        f"Expected Calibration Error: {metrics['expectedCalibrationError']:.4f}"
    )
    confidence_line = f"Incredere medie: {metrics['averageConfidence']:.4f}"

    lines = [
        f"Model: {model_name}",
        f"Accuracy: {metrics['accuracy']:.4f}",
        balanced_accuracy_line,
        f"Macro precision: {metrics['macroPrecision']:.4f}",
        f"Macro recall: {metrics['macroRecall']:.4f}",
        f"Macro F1: {metrics['macroF1']:.4f}",
        f"Weighted F1: {metrics['weightedF1']:.4f}",
        mcc_line,
        f"ROC-AUC macro OVR: {metrics['rocAucMacroOvr']:.4f}",
        weighted_auc_line,
        f"Log loss: {metrics['logLoss']:.4f}",
        brier_line,
        calibration_line,
        confidence_line,
    ]

    severe_errors = cast(
        Mapping[str, Any],
        metrics["severeErrors"],
    )
    severe_errors_line = (
        "Erori severe SCAZUT <-> RIDICAT: "
        f"{severe_errors['count']} "
        f"({severe_errors['rate']:.4%})"
    )

    lines.extend(
        [
            severe_errors_line,
            "",
            "Metrici pe clasa:",
        ]
    )

    metrics_by_class = cast(
        Mapping[str, Mapping[str, Any]],
        metrics["classMetrics"],
    )

    for label in CLASS_LABELS:
        class_values = metrics_by_class[label]

        lines.extend(
            [
                f"  {label}:",
                f"    Precision: {class_values['precision']:.4f}",
                f"    Recall: {class_values['recall']:.4f}",
                f"    F1: {class_values['f1']:.4f}",
                f"    Support: {class_values['support']}",
            ]
        )

    confusion_matrix_values = cast(
        Sequence[Sequence[Any]],
        metrics["confusionMatrix"],
    )

    lines.extend(
        [
            "",
            "Matrice de confuzie:",
            format_confusion_matrix(confusion_matrix_values),
        ]
    )

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Cross-validation si importanta caracteristicilor
# ---------------------------------------------------------------------------


def calculate_cross_validation_metrics(
    model: ClassifierModel,
    features: pd.DataFrame,
    target: pd.Series,
) -> CrossValidationMetrics:
    """Calculeaza mediile train/validation si gap-ul de generalizare."""

    cross_validation = StratifiedKFold(
        n_splits=CROSS_VALIDATION_FOLDS,
        shuffle=True,
        random_state=RANDOM_STATE,
    )

    scoring = {
        "accuracy": "accuracy",
        "balancedAccuracy": "balanced_accuracy",
        "macroF1": "f1_macro",
    }

    scores = cross_validate(
        clone_classifier(model),
        features,
        target,
        cv=cross_validation,
        scoring=scoring,
        n_jobs=1,
        return_train_score=True,
    )

    result: CrossValidationMetrics = {}

    for metric_name in scoring:
        test_values = np.asarray(
            scores[f"test_{metric_name}"],
            dtype=float,
        )
        train_values = np.asarray(
            scores[f"train_{metric_name}"],
            dtype=float,
        )

        validation_mean = float(np.mean(test_values))
        train_mean = float(np.mean(train_values))

        result[metric_name] = {
            "validationMean": validation_mean,
            "validationStd": float(np.std(test_values)),
            "trainMean": train_mean,
            "trainStd": float(np.std(train_values)),
            "generalizationGap": (train_mean - validation_mean),
            "foldValues": [float(value) for value in test_values],
        }

    return result


def extract_native_feature_importance(
    model: ClassifierModel,
) -> list[JsonObject]:
    """Extrage feature importance nativa, daca modelul o expune."""

    raw_importances = getattr(
        model,
        "feature_importances_",
        None,
    )
    if raw_importances is None:
        return []

    importances = np.asarray(
        raw_importances,
        dtype=float,
    ).reshape(-1)

    if len(importances) != len(FEATURE_COLUMNS):
        return []

    entries = [
        {
            "feature": feature,
            "importance": float(importance),
        }
        for feature, importance in zip(
            FEATURE_COLUMNS,
            importances,
            strict=True,
        )
    ]

    return sorted(
        entries,
        key=lambda entry: entry["importance"],
        reverse=True,
    )


def extract_permutation_importance(
    model: ClassifierModel,
    features_test: pd.DataFrame,
    target_test: pd.Series,
) -> list[JsonObject]:
    """Calculeaza permutation importance folosind Macro F1."""

    importance_result = permutation_importance(
        model,
        features_test,
        target_test,
        scoring="f1_macro",
        n_repeats=5,
        random_state=RANDOM_STATE,
        n_jobs=1,
    )

    importance_means = np.asarray(
        importance_result.importances_mean,
        dtype=float,
    )
    importance_stds = np.asarray(
        importance_result.importances_std,
        dtype=float,
    )

    entries = [
        {
            "feature": feature,
            "importanceMean": float(mean_value),
            "importanceStd": float(std_value),
        }
        for feature, mean_value, std_value in zip(
            FEATURE_COLUMNS,
            importance_means,
            importance_stds,
            strict=True,
        )
    ]

    return sorted(
        entries,
        key=lambda entry: entry["importanceMean"],
        reverse=True,
    )


# ---------------------------------------------------------------------------
# Antrenarea modelului
# ---------------------------------------------------------------------------


def train_traffic_model() -> None:
    """Antreneaza, selecteaza, evalueaza si salveaza modelul de trafic."""

    dataframe = pd.read_csv(DATA_FILE)
    validate_dataset(dataframe)

    features = dataframe[FEATURE_COLUMNS]
    target = cast(
        pd.Series,
        dataframe[TARGET_COLUMN],
    )

    first_split = train_test_split(
        features,
        target,
        test_size=0.30,
        random_state=RANDOM_STATE,
        stratify=target,
    )
    features_train = cast(
        pd.DataFrame,
        first_split[0],
    )
    features_temporary = cast(
        pd.DataFrame,
        first_split[1],
    )
    target_train = cast(
        pd.Series,
        first_split[2],
    )
    target_temporary = cast(
        pd.Series,
        first_split[3],
    )

    second_split = train_test_split(
        features_temporary,
        target_temporary,
        test_size=0.50,
        random_state=RANDOM_STATE,
        stratify=target_temporary,
    )
    features_validation = cast(
        pd.DataFrame,
        second_split[0],
    )
    features_test = cast(
        pd.DataFrame,
        second_split[1],
    )
    target_validation = cast(
        pd.Series,
        second_split[2],
    )
    target_test = cast(
        pd.Series,
        second_split[3],
    )

    baseline_model = cast(
        ClassifierModel,
        DummyClassifier(
            strategy="prior",
            random_state=RANDOM_STATE,
        ),
    )
    baseline_model.fit(
        features_train,
        target_train,
    )
    baseline_metrics = calculate_metrics(
        baseline_model,
        features_validation,
        target_validation,
    )

    candidate_models = build_candidate_models()
    validation_results: dict[str, Metrics] = {}

    for model_name, model in candidate_models.items():
        model.fit(
            features_train,
            target_train,
        )

        validation_results[model_name] = calculate_metrics(
            model,
            features_validation,
            target_validation,
        )

    features_train_final = pd.concat(
        [
            features_train,
            features_validation,
        ],
        ignore_index=True,
    )
    target_train_final = pd.concat(
        [
            target_train,
            target_validation,
        ],
        ignore_index=True,
    )

    # Selectia finala nu depinde de o singura impartire. Fiecare candidat este
    # evaluat prin cross-validation pe setul train + validation.
    candidate_cross_validation: dict[
        str,
        CrossValidationMetrics,
    ] = {}
    best_model_name: str | None = None
    best_selection_key: (
        tuple[
            float,
            float,
            float,
            float,
        ]
        | None
    ) = None

    for model_name, model in build_candidate_models().items():
        cross_validation_metrics = calculate_cross_validation_metrics(
            model,
            features_train_final,
            target_train_final,
        )
        candidate_cross_validation[model_name] = cross_validation_metrics

        macro_f1 = cross_validation_metrics["macroF1"]
        balanced_accuracy = cross_validation_metrics["balancedAccuracy"]
        validation_metrics = validation_results[model_name]

        selection_key = (
            float(macro_f1["validationMean"]),
            float(balanced_accuracy["validationMean"]),
            -float(macro_f1["generalizationGap"]),
            -float(
                validation_metrics.get(
                    "logLoss",
                    float("inf"),
                )
            ),
        )

        if best_selection_key is None or selection_key > best_selection_key:
            best_selection_key = selection_key
            best_model_name = model_name

    if best_model_name is None:
        raise RuntimeError("Nu a putut fi selectat un model de trafic.")

    best_model = build_candidate_models()[best_model_name]
    best_model.fit(
        features_train_final,
        target_train_final,
    )

    test_metrics = calculate_metrics(
        best_model,
        features_test,
        target_test,
    )
    selected_cross_validation_metrics = candidate_cross_validation[best_model_name]

    native_feature_importance = extract_native_feature_importance(best_model)
    permutation_feature_importance = extract_permutation_importance(
        best_model,
        features_test,
        target_test,
    )

    MODEL_FILE.parent.mkdir(
        parents=True,
        exist_ok=True,
    )
    REPORT_FILE.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    joblib.dump(
        best_model,
        MODEL_FILE,
    )

    trained_at = datetime.now(timezone.utc).isoformat()

    class_counts = target.value_counts()
    class_distribution = {
        label: int(class_counts.get(label, 0)) for label in CLASS_LABELS
    }

    metadata: JsonObject = {
        "modelName": best_model_name,
        "trainedAtUtc": trained_at,
        "dataset": str(DATA_FILE),
        "datasetRows": int(len(dataframe)),
        "featureColumns": FEATURE_COLUMNS,
        "targetColumn": TARGET_COLUMN,
        "classLabels": CLASS_LABELS,
        "classDistribution": class_distribution,
        "split": {
            "trainRows": int(len(features_train)),
            "validationRows": int(len(features_validation)),
            "testRows": int(len(features_test)),
        },
        "selectionCriterion": [
            "crossValidationMacroF1Mean",
            "crossValidationBalancedAccuracyMean",
            "lowerCrossValidationGeneralizationGap",
            "validationLogLoss",
        ],
        "baselineValidationMetrics": (baseline_metrics),
        "validationResults": validation_results,
        "testMetrics": test_metrics,
        "candidateCrossValidation": (candidate_cross_validation),
        "selectedModelCrossValidation": (selected_cross_validation_metrics),
        "nativeFeatureImportance": (native_feature_importance),
        "permutationFeatureImportance": (permutation_feature_importance),
    }

    METADATA_FILE.write_text(
        json.dumps(
            metadata,
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    validation_report = "\n\n".join(
        format_metrics(
            model_name,
            metrics,
        )
        for model_name, metrics in validation_results.items()
    )

    cross_validation_lines = [
        (
            f"- {metric_name}: "
            f"validation={values['validationMean']:.4f} "
            f"+/- {values['validationStd']:.4f}, "
            f"train={values['trainMean']:.4f}, "
            f"gap={values['generalizationGap']:.4f}"
        )
        for metric_name, values in selected_cross_validation_metrics.items()
    ]

    native_importance_lines = [
        (f"- {entry['feature']}: {entry['importance']:.4f}")
        for entry in native_feature_importance
    ]
    if not native_importance_lines:
        native_importance_lines = [
            ("- Modelul selectat nu expune feature_importances_.")
        ]

    permutation_importance_lines = [
        (
            f"- {entry['feature']}: "
            f"{entry['importanceMean']:.4f} "
            f"+/- {entry['importanceStd']:.4f}"
        )
        for entry in permutation_feature_importance
    ]

    report_text = f"""
MODEL TRAFIC

Scop:
Modelul prezice nivelul de trafic operational al restaurantului.

Output:
- SCAZUT;
- MEDIU;
- RIDICAT.

Dataset:
{DATA_FILE}

Numar total randuri:
{len(dataframe)}

Distributie clase:
- SCAZUT: {class_distribution["SCAZUT"]}
- MEDIU: {class_distribution["MEDIU"]}
- RIDICAT: {class_distribution["RIDICAT"]}

Caracteristici:
{", ".join(FEATURE_COLUMNS)}

Impartire dataset:
- train: {len(features_train)}
- validation: {len(features_validation)}
- test: {len(features_test)}

Criteriu de selectie:
1. media Macro F1 din cross-validation;
2. media Balanced accuracy din cross-validation;
3. gap de generalizare mai mic;
4. Log loss pe validation, unde valoarea mai mica este mai buna.

BASELINE PE SETUL DE VALIDATION

{format_metrics("DummyClassifier - prior", baseline_metrics)}

COMPARATIE PE SETUL DE VALIDATION

{validation_report}

MODEL SELECTAT:
{best_model_name}

EVALUARE FINALA PE SETUL DE TEST

{format_metrics(best_model_name, test_metrics)}

CROSS-VALIDATION STRATIFICAT PENTRU MODELUL SELECTAT - {CROSS_VALIDATION_FOLDS} FOLDURI

{chr(10).join(cross_validation_lines)}

IMPORTANTA NATIVA A CARACTERISTICILOR

{chr(10).join(native_importance_lines)}

PERMUTATION IMPORTANCE - SCOR MACRO F1

{chr(10).join(permutation_importance_lines)}

Interpretare:
- Accuracy arata proportia totala de predictii corecte;
- Balanced accuracy trateaza egal toate clasele;
- Macro F1 acorda aceeasi importanta claselor;
- MCC este o masura robusta a corelatiei dintre valorile reale si predictii;
- ROC-AUC masoara separarea claselor pe baza probabilitatilor;
- Log loss si Brier score penalizeaza probabilitatile gresite si prea sigure;
- Expected Calibration Error compara increderea modelului cu acuratetea reala;
- erorile severe sunt confuzii directe intre SCAZUT si RIDICAT;
- modelul este selectat pe validation, iar testul este folosit o singura data.
""".strip()

    REPORT_FILE.write_text(
        report_text,
        encoding="utf-8",
    )

    print("Modelul de trafic a fost antrenat.")
    print(f"Model selectat: {best_model_name}")
    print(f"Model salvat in: {MODEL_FILE}")
    print(f"Metadata salvata in: {METADATA_FILE}")
    print(f"Raport salvat in: {REPORT_FILE}")
    print(f"Accuracy finala: {test_metrics['accuracy']:.4f}")
    print(f"Balanced accuracy finala: {test_metrics['balancedAccuracy']:.4f}")
    print(f"Macro F1 final: {test_metrics['macroF1']:.4f}")
    print(f"MCC final: {test_metrics['matthewsCorrelationCoefficient']:.4f}")
    print(f"ROC-AUC macro final: {test_metrics['rocAucMacroOvr']:.4f}")
    print(f"Log loss final: {test_metrics['logLoss']:.4f}")
    print(f"ECE final: {test_metrics['expectedCalibrationError']:.4f}")


def main() -> None:
    """Punctul de intrare al scriptului."""

    train_traffic_model()


if __name__ == "__main__":
    main()
