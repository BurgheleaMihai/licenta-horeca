import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import pandas as pd
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
    matthews_corrcoef,
    precision_score,
    recall_score,
)
from sklearn.model_selection import train_test_split


BASE_DIR = Path(__file__).resolve().parent

DATA_FILE = BASE_DIR / "data" / "synthetic_horeca_dataset.csv"

MODEL_FILE = BASE_DIR / "models" / "delay_model.pkl"

REPORT_FILE = BASE_DIR / "reports" / "delay_metrics.txt"

METADATA_FILE = BASE_DIR / "models" / "delay_model_metadata.json"

#
# Toate aceste caracteristici pot fi obtinute
# in momentul predictiei:
#
# - primele 11 vin direct din backend;
# - personalul activ vine din ture;
# - personalul recomandat este prezis mai intai
#   de modelul de personal;
# - deficitele si rapoartele sunt calculate
#   apoi in Flask.
#
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

TARGET_COLUMN = "delay_risk"

CLASS_LABELS = [
    "SCAZUT",
    "MEDIU",
    "RIDICAT",
]

RANDOM_STATE = 42


def validate_dataset(
        dataframe,
):
    required_columns = FEATURE_COLUMNS + [TARGET_COLUMN]

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
        column for column in required_columns if dataframe[column].isnull().any()
    ]

    if columns_with_missing_values:
        raise ValueError(
            "Datasetul contine valori lipsa "
            f"in coloanele: "
            f"{columns_with_missing_values}"
        )

    invalid_labels = sorted(set(dataframe[TARGET_COLUMN]) - set(CLASS_LABELS))

    if invalid_labels:
        raise ValueError(f"Datasetul contine etichete neacceptate: {invalid_labels}")

    class_counts = dataframe[TARGET_COLUMN].value_counts()

    missing_classes = [
        label for label in CLASS_LABELS if label not in class_counts.index
    ]

    if missing_classes:
        raise ValueError(f"Datasetul nu contine toate clasele: {missing_classes}")


def build_candidate_models():
    return {
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
    }


def calculate_metrics(
        expected_values,
        predictions,
):
    report_dictionary = classification_report(
        expected_values,
        predictions,
        labels=CLASS_LABELS,
        output_dict=True,
        zero_division=0,
    )

    class_metrics = {}

    for label in CLASS_LABELS:
        label_metrics = report_dictionary.get(label)

        if not isinstance(label_metrics, dict):
            raise TypeError(f"Metricile clasei {label} au un format invalid.")

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

    return {
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
    }


def format_confusion_matrix(
        matrix,
):
    header = "real\\prezis" + "".join(f"{label:>12}" for label in CLASS_LABELS)

    rows = [header]

    for label, values in zip(
            CLASS_LABELS,
            matrix,
    ):
        row = f"{label:<12}" + "".join(f"{value:>12}" for value in values)

        rows.append(row)

    return "\n".join(rows)


def format_metrics(
        model_name,
        metrics,
):
    lines = [
        f"Model: {model_name}",
        f"Accuracy: {metrics['accuracy']:.4f}",
        f"Macro precision: {metrics['macroPrecision']:.4f}",
        f"Macro recall: {metrics['macroRecall']:.4f}",
        f"Macro F1: {metrics['macroF1']:.4f}",
        f"Weighted F1: {metrics['weightedF1']:.4f}",
        "",
        "Metrici pe clasa:",
    ]

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


def extract_feature_importance(
        model,
):
    if not hasattr(
            model,
            "feature_importances_",
    ):
        return []

    importance_pairs = list(
        zip(
            FEATURE_COLUMNS,
            model.feature_importances_,
        )
    )

    importance_pairs.sort(
        key=lambda pair: pair[1],
        reverse=True,
    )

    return [
        {
            "feature": feature,
            "importance": float(importance),
        }
        for feature, importance in importance_pairs
    ]


def train_delay_model():
    dataframe = pd.read_csv(DATA_FILE)

    validate_dataset(dataframe)

    features = dataframe[FEATURE_COLUMNS]

    target = dataframe[TARGET_COLUMN]

    #
    # 70% train, 15% validation, 15% test.
    #
    # Modelul este ales pe validation. Setul
    # de test ramane separat pentru evaluarea
    # finala.
    #
    (
        features_train,
        features_temporary,
        target_train,
        target_temporary,
    ) = train_test_split(
        features,
        target,
        test_size=0.30,
        random_state=RANDOM_STATE,
        stratify=target,
    )

    (
        features_validation,
        features_test,
        target_validation,
        target_test,
    ) = train_test_split(
        features_temporary,
        target_temporary,
        test_size=0.50,
        random_state=RANDOM_STATE,
        stratify=target_temporary,
    )

    candidate_models = build_candidate_models()

    validation_results = {}

    best_model_name = None
    best_macro_f1 = float("-inf")
    best_accuracy = float("-inf")

    for model_name, model in candidate_models.items():
        model.fit(
            features_train,
            target_train,
        )

        validation_predictions = model.predict(features_validation)

        validation_metrics = calculate_metrics(
            target_validation,
            validation_predictions,
        )

        validation_results[model_name] = validation_metrics

        current_macro_f1 = validation_metrics["macroF1"]

        current_accuracy = validation_metrics["accuracy"]

        #
        # Selectam dupa Macro F1 pentru a acorda
        # aceeasi importanta tuturor claselor.
        # Accuracy este folosita doar la egalitate.
        #
        if current_macro_f1 > best_macro_f1 or (
                current_macro_f1 == best_macro_f1 and current_accuracy > best_accuracy
        ):
            best_macro_f1 = current_macro_f1

            best_accuracy = current_accuracy

            best_model_name = model_name

    if best_model_name is None:
        raise RuntimeError("Nu a putut fi selectat un model de intarziere.")

    #
    # Reantrenam modelul castigator folosind
    # train + validation.
    #
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

    best_model = build_candidate_models()[best_model_name]

    best_model.fit(
        features_train_final,
        target_train_final,
    )

    test_predictions = best_model.predict(features_test)

    test_metrics = calculate_metrics(
        target_test,
        test_predictions,
    )

    feature_importance = extract_feature_importance(best_model)

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

    metadata = {
        "modelName": best_model_name,
        "trainedAtUtc": trained_at,
        "dataset": str(DATA_FILE),
        "datasetRows": int(len(dataframe)),
        "featureColumns": FEATURE_COLUMNS,
        "targetColumn": TARGET_COLUMN,
        "classLabels": CLASS_LABELS,
        "split": {
            "trainRows": int(len(features_train)),
            "validationRows": int(len(features_validation)),
            "testRows": int(len(features_test)),
        },
        "validationResults": validation_results,
        "testMetrics": test_metrics,
        "featureImportance": feature_importance,
    }

    METADATA_FILE.write_text(
        json.dumps(
            metadata,
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    validation_sections = []

    for model_name, metrics in validation_results.items():
        validation_sections.append(
            format_metrics(
                model_name,
                metrics,
            )
        )

    validation_report = "\n\n".join(validation_sections)

    importance_lines = []

    if feature_importance:
        for entry in feature_importance:
            importance_lines.append(f"- {entry['feature']}: {entry['importance']:.4f}")

    else:
        importance_lines.append("- Modelul selectat nu expune feature_importances_.")

    importance_report = "\n".join(importance_lines)

    report_text = f"""
MODEL RISC INTARZIERE

Scop:
Modelul estimeaza riscul ca activitatea
curenta sa produca intarzieri.

Output:
- SCAZUT;
- MEDIU;
- RIDICAT.

Dataset:
{DATA_FILE}

Numar total randuri:
{len(dataframe)}

Caracteristici:
{", ".join(FEATURE_COLUMNS)}

Observatie de arhitectura:
Modelul foloseste doar date care pot fi
obtinute si la predictia reala. Deficitele
si rapoartele sunt calculate in Flask dupa
predictia necesarului de personal.

Impartire dataset:
- train: {len(features_train)}
- validation: {len(features_validation)}
- test: {len(features_test)}

Criteriu de selectie:
Macro F1 pe setul de validation.
Accuracy este criteriu secundar la egalitate.

COMPARATIE PE SETUL DE VALIDATION

{validation_report}

MODEL SELECTAT:
{best_model_name}

EVALUARE FINALA PE SETUL DE TEST

{format_metrics(best_model_name, test_metrics)}

IMPORTANTA CARACTERISTICILOR

{importance_report}

Interpretare:
- Accuracy reprezinta proportia totala de
  predictii corecte;
- Macro F1 acorda aceeasi importanta tuturor
  claselor, indiferent de frecventa lor;
- matricea de confuzie arata intre ce niveluri
  de risc apar erorile;
- modelul a fost selectat pe validation, nu
  pe setul de test.
""".strip()

    REPORT_FILE.write_text(
        report_text,
        encoding="utf-8",
    )

    print("Modelul de risc intarziere a fost antrenat.")

    print(f"Model selectat: {best_model_name}")

    print(f"Model salvat in: {MODEL_FILE}")

    print(f"Metadata salvata in: {METADATA_FILE}")

    print(f"Raport salvat in: {REPORT_FILE}")

    print(f"Accuracy finala: {test_metrics['accuracy']:.4f}")

    print(f"Macro F1 final: {test_metrics['macroF1']:.4f}")

    print(f"Weighted F1 final: {test_metrics['weightedF1']:.4f}")

    print(f"Balanced accuracy finala: {test_metrics['balancedAccuracy']:.4f}")

    print(f"MCC final: {test_metrics['matthewsCorrelationCoefficient']:.4f}")


if __name__ == "__main__":
    train_delay_model()