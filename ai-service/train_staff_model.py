"""
Antreneaza modelul specializat pentru recomandarea personalului HoReCa.

Modelul contine cate un regresor separat pentru:
- ospatari;
- personalul din bucatarie;
- personalul de la bar.

Scriptul:
1. valideaza datasetul;
2. imparte datele in train, validation si test;
3. evalueaza modelul pe validation;
4. reantreneaza modelul final pe train + validation;
5. verifica pragurile minime de acceptare;
6. face backup artefactelor curente;
7. salveaza atomic modelul, metadatele si raportul text.

Logica si pragurile modelului au fost pastrate. Modificarile sunt de tipizare,
comentare, validare si formatare.
"""

from __future__ import annotations

import json
import shutil
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Mapping, TypeAlias, cast

import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import (
    mean_absolute_error,
    mean_squared_error,
    r2_score,
)
from sklearn.model_selection import train_test_split

from staff_model_architecture import (
    FEATURE_COLUMNS,
    ROLE_FEATURE_COLUMNS,
    TARGET_COLUMNS,
    RoleSpecificStaffRegressor,
)


JsonObject: TypeAlias = dict[str, Any]
Metrics: TypeAlias = dict[str, Any]
EvaluationResult: TypeAlias = dict[str, Any]


# ---------------------------------------------------------------------------
# Fisiere si praguri de acceptare
# ---------------------------------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent
DATA_FILE = BASE_DIR / "data" / "synthetic_horeca_dataset.csv"

MODELS_DIR = BASE_DIR / "models"
REPORTS_DIR = BASE_DIR / "reports"
BACKUPS_DIR = MODELS_DIR / "backups"

MODEL_FILE = MODELS_DIR / "staff_model.pkl"
METADATA_FILE = MODELS_DIR / "staff_model_metadata.json"
REPORT_FILE = REPORTS_DIR / "staff_metrics.txt"

RANDOM_STATE = 42

# Modelul nou este salvat numai daca indeplineste simultan toate criteriile.
MAX_ALLOWED_R2_DEGRADATION = 0.10
MAX_ALLOWED_OVERALL_MAE = 0.50
MIN_REQUIRED_WITHIN_ONE_RATE = 0.95


class StaffTrainingError(Exception):
    """Eroare controlata aparuta la validare sau acceptarea modelului."""


# ---------------------------------------------------------------------------
# Validarea datasetului
# ---------------------------------------------------------------------------


def validate_dataset(dataframe: pd.DataFrame) -> None:
    """Verifica structura si valorile obligatorii ale datasetului."""

    required_columns = [*FEATURE_COLUMNS, *TARGET_COLUMNS]

    missing_columns = [
        column for column in required_columns if column not in dataframe.columns
    ]
    if missing_columns:
        raise StaffTrainingError(
            f"Datasetul nu contine coloanele obligatorii: {missing_columns}"
        )

    if dataframe.empty:
        raise StaffTrainingError("Datasetul este gol.")

    columns_with_missing_values = [
        column for column in required_columns if bool(dataframe[column].isnull().any())
    ]
    if columns_with_missing_values:
        raise StaffTrainingError(
            "Datasetul contine valori lipsa in coloanele: "
            f"{columns_with_missing_values}"
        )

    negative_target_columns = [
        column for column in TARGET_COLUMNS if bool((dataframe[column] < 0).any())
    ]
    if negative_target_columns:
        raise StaffTrainingError(
            f"Recomandarile de personal nu pot fi negative: {negative_target_columns}"
        )


# ---------------------------------------------------------------------------
# Metrici
# ---------------------------------------------------------------------------


def calculate_metrics(
    expected_values: pd.DataFrame | np.ndarray,
    predictions: np.ndarray,
) -> Metrics:
    """Calculeaza metricile generale si metricile fiecarui rol."""

    expected_array = np.asarray(expected_values, dtype=float)
    prediction_array = np.asarray(predictions, dtype=float)

    if expected_array.shape != prediction_array.shape:
        raise StaffTrainingError(
            "Forma predictiilor nu corespunde formei valorilor asteptate."
        )

    if expected_array.ndim != 2 or expected_array.shape[1] != len(TARGET_COLUMNS):
        raise StaffTrainingError("Evaluarea necesita exact trei coloane tinta.")

    rounded_expected = np.rint(expected_array).astype(int)
    rounded_predictions = np.maximum(
        0,
        np.rint(prediction_array).astype(int),
    )

    absolute_errors = np.abs(prediction_array - expected_array)
    rounded_errors = rounded_predictions - rounded_expected

    # sklearn poate declara aceste rezultate drept float sau ndarray.
    # Conversia explicita la ndarray elimina ambiguitatea pentru IntelliJ si
    # garanteaza indexarea corecta a metricilor pe fiecare rol.
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

    per_target: JsonObject = {}

    for target_index, target_column in enumerate(TARGET_COLUMNS):
        target_rounded_errors = rounded_errors[:, target_index]
        target_absolute_errors = absolute_errors[:, target_index]

        per_target[target_column] = {
            "mae": float(mae_by_target[target_index]),
            "rmse": float(rmse_by_target[target_index]),
            "r2": float(r2_by_target[target_index]),
            "roundedExactRate": float(np.mean(target_rounded_errors == 0)),
            "roundedWithinOneRate": float(np.mean(np.abs(target_rounded_errors) <= 1)),
            "p90AbsoluteError": float(
                np.percentile(
                    target_absolute_errors,
                    90,
                )
            ),
            "maximumRoundedError": int(np.max(np.abs(target_rounded_errors))),
        }

    return {
        "overallMae": float(
            mean_absolute_error(
                expected_array,
                prediction_array,
            )
        ),
        "overallRmse": float(
            mean_squared_error(
                expected_array,
                prediction_array,
            )
            ** 0.5
        ),
        "overallR2": float(
            r2_score(
                expected_array,
                prediction_array,
                multioutput="uniform_average",
            )
        ),
        "roundedExactCellRate": float(np.mean(rounded_errors == 0)),
        "roundedWithinOneCellRate": float(np.mean(np.abs(rounded_errors) <= 1)),
        "allThreeRoundedExactRate": float(
            np.mean(
                np.all(
                    rounded_errors == 0,
                    axis=1,
                )
            )
        ),
        "underestimateCellRate": float(np.mean(rounded_errors < 0)),
        "overestimateCellRate": float(np.mean(rounded_errors > 0)),
        "maximumRoundedError": int(np.max(np.abs(rounded_errors))),
        "perTarget": per_target,
    }


def calculate_generalization(
    train_metrics: Mapping[str, Any],
    evaluation_metrics: Mapping[str, Any],
) -> JsonObject:
    """Calculeaza degradarea R2 dintre train si setul de evaluare."""

    train_per_target = cast(
        Mapping[str, Mapping[str, Any]],
        train_metrics["perTarget"],
    )
    evaluation_per_target = cast(
        Mapping[str, Mapping[str, Any]],
        evaluation_metrics["perTarget"],
    )

    per_target: JsonObject = {}

    for target_column in TARGET_COLUMNS:
        train_r2 = float(train_per_target[target_column]["r2"])
        evaluation_r2 = float(evaluation_per_target[target_column]["r2"])

        denominator = max(abs(train_r2), 1e-9)
        relative_degradation = (train_r2 - evaluation_r2) / denominator

        per_target[target_column] = {
            "trainR2": train_r2,
            "evaluationR2": evaluation_r2,
            "relativeDegradation": float(relative_degradation),
        }

    maximum_degradation = max(
        float(values["relativeDegradation"]) for values in per_target.values()
    )

    return {
        "maximumRelativeR2Degradation": float(maximum_degradation),
        "perTarget": per_target,
    }


def evaluate_model(
    model: RoleSpecificStaffRegressor,
    train_features: pd.DataFrame,
    train_targets: pd.DataFrame,
    evaluation_features: pd.DataFrame,
    evaluation_targets: pd.DataFrame,
) -> EvaluationResult:
    """Evalueaza acelasi model pe train si pe un set separat."""

    train_predictions = np.asarray(
        model.predict(train_features),
        dtype=float,
    )
    evaluation_predictions = np.asarray(
        model.predict(evaluation_features),
        dtype=float,
    )

    train_metrics = calculate_metrics(
        train_targets,
        train_predictions,
    )
    evaluation_metrics = calculate_metrics(
        evaluation_targets,
        evaluation_predictions,
    )

    return {
        "trainMetrics": train_metrics,
        "evaluationMetrics": evaluation_metrics,
        "generalization": calculate_generalization(
            train_metrics,
            evaluation_metrics,
        ),
    }


def check_acceptance(
    test_evaluation: Mapping[str, Any],
) -> dict[str, bool]:
    """Verifica daca modelul nou poate inlocui modelul existent."""

    metrics = cast(
        Mapping[str, Any],
        test_evaluation["evaluationMetrics"],
    )
    generalization = cast(
        Mapping[str, Any],
        test_evaluation["generalization"],
    )

    checks = {
        "overallMaePassed": (float(metrics["overallMae"]) <= MAX_ALLOWED_OVERALL_MAE),
        "withinOneRatePassed": (
            float(metrics["roundedWithinOneCellRate"]) >= MIN_REQUIRED_WITHIN_ONE_RATE
        ),
        "generalizationPassed": (
            float(generalization["maximumRelativeR2Degradation"])
            <= MAX_ALLOWED_R2_DEGRADATION
        ),
    }
    checks["accepted"] = all(checks.values())

    return checks


# ---------------------------------------------------------------------------
# Backup si salvare atomica
# ---------------------------------------------------------------------------


def backup_current_artifacts(
    timestamp: str,
) -> Path | None:
    """Copiaza artefactele existente inaintea inlocuirii lor."""

    existing_files = [
        file_path
        for file_path in [
            MODEL_FILE,
            METADATA_FILE,
            REPORT_FILE,
        ]
        if file_path.exists()
    ]

    if not existing_files:
        return None

    backup_directory = BACKUPS_DIR / timestamp
    backup_directory.mkdir(
        parents=True,
        exist_ok=True,
    )

    for file_path in existing_files:
        shutil.copy2(
            file_path,
            backup_directory / file_path.name,
        )

    return backup_directory


def save_model_atomically(
    model: RoleSpecificStaffRegressor,
) -> None:
    """
    Salveaza modelul intr-un fisier temporar si il valideaza inainte de replace.
    """

    temporary_file = MODEL_FILE.with_suffix(".pkl.tmp")
    joblib.dump(model, temporary_file)

    try:
        loaded_model = cast(
            RoleSpecificStaffRegressor,
            joblib.load(temporary_file),
        )

        test_frame = pd.DataFrame(
            [{column: 0 for column in FEATURE_COLUMNS}],
            columns=FEATURE_COLUMNS,
        )
        test_prediction = np.asarray(
            loaded_model.predict(test_frame),
            dtype=float,
        )

        if test_prediction.shape != (1, 3):
            raise StaffTrainingError("Modelul salvat nu returneaza exact trei valori.")

        temporary_file.replace(MODEL_FILE)

    except Exception:
        temporary_file.unlink(missing_ok=True)
        raise


# ---------------------------------------------------------------------------
# Formatarea raportului
# ---------------------------------------------------------------------------


def format_evaluation(
    title: str,
    evaluation: Mapping[str, Any],
) -> str:
    """Formateaza metricile si generalizarea pentru raportul text."""

    metrics = cast(
        Mapping[str, Any],
        evaluation["evaluationMetrics"],
    )
    generalization = cast(
        Mapping[str, Any],
        evaluation["generalization"],
    )
    metrics_per_target = cast(
        Mapping[str, Mapping[str, Any]],
        metrics["perTarget"],
    )
    generalization_per_target = cast(
        Mapping[str, Mapping[str, Any]],
        generalization["perTarget"],
    )

    exact_rate_line = f"Exact dupa rotunjire: {metrics['roundedExactCellRate']:.2%}"
    within_one_line = (
        f"In limita +/-1 angajat: {metrics['roundedWithinOneCellRate']:.2%}"
    )
    degradation_line = (
        f"Degradare R2 maxima: {generalization['maximumRelativeR2Degradation']:.2%}"
    )

    lines = [
        title,
        "-" * len(title),
        f"MAE: {metrics['overallMae']:.4f}",
        f"RMSE: {metrics['overallRmse']:.4f}",
        f"R2: {metrics['overallR2']:.4f}",
        exact_rate_line,
        within_one_line,
        degradation_line,
        "",
        "Metrici pe rol:",
    ]

    for target_column in TARGET_COLUMNS:
        target_metrics = metrics_per_target[target_column]
        target_generalization = generalization_per_target[target_column]

        role_degradation_line = (
            f"  Degradare R2: {target_generalization['relativeDegradation']:.2%}"
        )

        lines.extend(
            [
                f"- {target_column}",
                f"  MAE: {target_metrics['mae']:.4f}",
                f"  RMSE: {target_metrics['rmse']:.4f}",
                f"  R2: {target_metrics['r2']:.4f}",
                role_degradation_line,
            ]
        )

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Antrenarea modelului
# ---------------------------------------------------------------------------


def train_staff_model() -> None:
    """Antreneaza, valideaza si salveaza modelul specializat de personal."""

    dataframe = pd.read_csv(DATA_FILE)
    validate_dataset(dataframe)

    features = dataframe[FEATURE_COLUMNS]
    targets = dataframe[TARGET_COLUMNS]

    first_split = train_test_split(
        features,
        targets,
        test_size=0.30,
        random_state=RANDOM_STATE,
    )
    features_train = cast(
        pd.DataFrame,
        first_split[0],
    )
    features_temporary = cast(
        pd.DataFrame,
        first_split[1],
    )
    targets_train = cast(
        pd.DataFrame,
        first_split[2],
    )
    targets_temporary = cast(
        pd.DataFrame,
        first_split[3],
    )

    second_split = train_test_split(
        features_temporary,
        targets_temporary,
        test_size=0.50,
        random_state=RANDOM_STATE,
    )
    features_validation = cast(
        pd.DataFrame,
        second_split[0],
    )
    features_test = cast(
        pd.DataFrame,
        second_split[1],
    )
    targets_validation = cast(
        pd.DataFrame,
        second_split[2],
    )
    targets_test = cast(
        pd.DataFrame,
        second_split[3],
    )

    # Prima evaluare foloseste exclusiv train pentru fit si validation pentru
    # verificarea intermediara a generalizarii.
    validation_model = RoleSpecificStaffRegressor(random_state=RANDOM_STATE)
    validation_model.fit(
        features_train,
        targets_train,
    )
    validation_evaluation = evaluate_model(
        validation_model,
        features_train,
        targets_train,
        features_validation,
        targets_validation,
    )

    # Modelul final foloseste train + validation. Testul ramane separat pana
    # la verdictul final.
    features_train_final = pd.concat(
        [
            features_train,
            features_validation,
        ],
        ignore_index=True,
    )
    targets_train_final = pd.concat(
        [
            targets_train,
            targets_validation,
        ],
        ignore_index=True,
    )

    final_model = RoleSpecificStaffRegressor(random_state=RANDOM_STATE)
    final_model.fit(
        features_train_final,
        targets_train_final,
    )

    test_evaluation = evaluate_model(
        final_model,
        features_train_final,
        targets_train_final,
        features_test,
        targets_test,
    )
    acceptance = check_acceptance(test_evaluation)

    if not acceptance["accepted"]:
        raise StaffTrainingError(
            "Modelul nou nu indeplineste criteriile minime. "
            "Modelul existent nu a fost inlocuit. "
            f"Verificari: {acceptance}"
        )

    MODELS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )
    REPORTS_DIR.mkdir(
        parents=True,
        exist_ok=True,
    )

    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")

    backup_directory = backup_current_artifacts(timestamp)
    save_model_atomically(final_model)

    metadata: JsonObject = {
        "modelName": "RoleSpecificStaffRegressor",
        "architecture": "three_role_specific_cloneable_regressors",
        "trainedAtUtc": datetime.now(timezone.utc).isoformat(),
        "dataset": str(DATA_FILE),
        "datasetRows": int(len(dataframe)),
        "featureColumns": FEATURE_COLUMNS,
        "targetColumns": TARGET_COLUMNS,
        "roleFeatureColumns": ROLE_FEATURE_COLUMNS,
        "split": {
            "trainRows": int(len(features_train)),
            "validationRows": int(len(features_validation)),
            "testRows": int(len(features_test)),
        },
        "acceptanceCriteria": {
            "maximumRelativeR2Degradation": MAX_ALLOWED_R2_DEGRADATION,
            "maximumOverallMae": MAX_ALLOWED_OVERALL_MAE,
            "minimumWithinOneRate": MIN_REQUIRED_WITHIN_ONE_RATE,
        },
        "validationEvaluation": validation_evaluation,
        "testEvaluation": test_evaluation,
        "acceptance": acceptance,
        "backupDirectory": (
            str(backup_directory) if backup_directory is not None else None
        ),
    }

    METADATA_FILE.write_text(
        json.dumps(
            metadata,
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    report_text = f"""
MODEL RECOMANDARE PERSONAL - ARHITECTURA SPECIALIZATA SI CLONABILA

Arhitectura:
- un regresor separat pentru fiecare rol;
- selectie de caracteristici specifica rolului;
- regularizare suplimentara pentru bar;
- model compatibil cu sklearn.clone si cross-validation;
- predict() returneaza aceleasi trei valori ca pana acum.

Dataset:
{DATA_FILE}

Randuri:
{len(dataframe)}

Caracteristici pe rol:
{json.dumps(ROLE_FEATURE_COLUMNS, ensure_ascii=False, indent=2)}

{format_evaluation("TRAIN -> VALIDATION", validation_evaluation)}

{format_evaluation("TRAIN+VALIDATION -> TEST", test_evaluation)}

Criterii de acceptare:
{json.dumps(acceptance, ensure_ascii=False, indent=2)}
""".strip()

    REPORT_FILE.write_text(
        report_text,
        encoding="utf-8",
    )

    final_metrics = cast(
        Mapping[str, Any],
        test_evaluation["evaluationMetrics"],
    )
    final_generalization = cast(
        Mapping[str, Any],
        test_evaluation["generalization"],
    )

    print("Modelul specializat si clonabil de personal a fost acceptat.")
    print(f"Model salvat in: {MODEL_FILE}")
    print(f"Raport salvat in: {REPORT_FILE}")
    print(f"MAE final personal: {final_metrics['overallMae']:.4f}")
    print(f"RMSE final personal: {final_metrics['overallRmse']:.4f}")
    print(f"R2 final personal: {final_metrics['overallR2']:.4f}")
    print(
        "Degradare R2 maxima: "
        f"{final_generalization['maximumRelativeR2Degradation']:.2%}"
    )
    print(f"Precizie in limita +/-1: {final_metrics['roundedWithinOneCellRate']:.2%}")


def main() -> None:
    """Punctul de intrare al scriptului."""

    try:
        train_staff_model()
    except StaffTrainingError as caught_error:
        print(f"Antrenarea a fost oprita: {caught_error}")
        raise SystemExit(1) from caught_error


if __name__ == "__main__":
    main()
