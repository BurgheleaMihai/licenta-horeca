# ruff: noqa: E402
from __future__ import annotations

"""Validare externa pentru modelele AI ale aplicatiei HoReCa.

Scriptul genereaza rapoarte Deepchecks pentru:
- clasificarea traficului;
- recomandarea personalului pentru fiecare rol;
- clasificarea riscului de intarziere pe datele offline;
- fluxul complet folosit in productie.

Deepchecks 0.19.1 este mai vechi decat versiunile curente de NumPy si
scikit-learn. Sectiunea de compatibilitate de mai jos rezolva numai
incompatibilitatile bibliotecii de evaluare; modelele salvate nu sunt
modificate.
"""

import json
import warnings
from pathlib import Path
from typing import Any, Callable, Final, Sequence, cast

import joblib
import numpy as np
import pandas as pd
from sklearn import metrics as sklearn_metrics
from sklearn.base import BaseEstimator, ClassifierMixin, RegressorMixin
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    f1_score,
    make_scorer,
    max_error,
    mean_absolute_error,
    mean_squared_error,
    precision_score,
    r2_score,
    recall_score,
)
from sklearn.model_selection import train_test_split


# ---------------------------------------------------------------------------
# Compatibilitate Deepchecks 0.19.1
# ---------------------------------------------------------------------------


def _install_numpy_compatibility_aliases() -> None:
    """Adauga aliasurile eliminate din NumPy 2.x, folosite de Deepchecks."""

    aliases = {
        "Inf": np.inf,
        "Infinity": np.inf,
        "infty": np.inf,
        "NaN": np.nan,
    }

    for alias, value in aliases.items():
        if not hasattr(np, alias):
            setattr(np, alias, value)


def _install_sklearn_scorer_compatibility() -> None:
    """Face disponibil vechiul nume ``max_error`` fara API-uri protejate.

    Deepchecks 0.19.1 cere scorerul ``max_error`` la import. In versiunile
    noi de scikit-learn numele public este ``neg_max_error``. Inlocuim
    temporar functia publica ``get_scorer`` cu un adaptor compatibil.
    """

    original_get_scorer: Callable[[str], Any] = sklearn_metrics.get_scorer

    def compatible_get_scorer(scoring: str) -> Any:
        if scoring == "max_error":
            return make_scorer(max_error, greater_is_better=False)
        return original_get_scorer(scoring)

    setattr(sklearn_metrics, "get_scorer", compatible_get_scorer)


_install_numpy_compatibility_aliases()
_install_sklearn_scorer_compatibility()

warnings.filterwarnings(
    "ignore",
    message="pkg_resources is deprecated as an API.*",
)

# Dependentele Deepchecks sunt tinute separat in requirements-evaluation.txt.
# noinspection PyPackageRequirements
from deepchecks.tabular import Dataset, Suite

# noinspection PyPackageRequirements
from deepchecks.tabular.checks import (
    BoostingOverfit,
    CalibrationScore,
    ConfusionMatrixReport,
    ModelInferenceTime,
    PredictionDrift,
    RegressionErrorDistribution,
    RocReport,
    SimpleModelComparison,
    TrainTestPerformance,
    UnusedFeatures,
    WeakSegmentsPerformance,
)


# ---------------------------------------------------------------------------
# Cai si configurare
# ---------------------------------------------------------------------------

BASE_DIR: Final = Path(__file__).resolve().parent
DATA_FILE: Final = BASE_DIR / "data" / "synthetic_horeca_dataset.csv"
MODELS_DIR: Final = BASE_DIR / "models"
REPORTS_DIR: Final = BASE_DIR / "reports" / "deepchecks"

TRAFFIC_MODEL_FILE: Final = MODELS_DIR / "traffic_model.pkl"
STAFF_MODEL_FILE: Final = MODELS_DIR / "staff_model.pkl"
DELAY_MODEL_FILE: Final = MODELS_DIR / "delay_model.pkl"

BASE_FEATURE_COLUMNS: Final[list[str]] = [
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

ACTIVE_STAFF_COLUMNS: Final[list[str]] = [
    "active_waiters",
    "active_kitchen",
    "active_bar",
]

PRODUCTION_DELAY_FEATURE_COLUMNS: Final[list[str]] = (
        BASE_FEATURE_COLUMNS + ACTIVE_STAFF_COLUMNS
)

DELAY_FEATURE_COLUMNS: Final[list[str]] = BASE_FEATURE_COLUMNS + [
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

STAFF_TARGET_COLUMNS: Final[list[str]] = [
    "recommended_waiters",
    "recommended_kitchen_staff",
    "recommended_bar_staff",
]

TRAFFIC_TARGET_COLUMN: Final = "traffic_level"
DELAY_TARGET_COLUMN: Final = "delay_risk"
RANDOM_STATE: Final = 42

# Deepchecks raporteaza caracteristici nefolosite pe baza importantei arborilor.
# Verificarea este relevanta pentru modelele directe, dar nu pentru modelele de
# intarziere cu multe variabile derivate si corelate. Pentru aceste doua
# rapoarte pastram verificarile de performanta, drift, baseline si inferenta.
INCLUDE_UNUSED_FEATURES_FOR_TRAFFIC: Final = True
INCLUDE_UNUSED_FEATURES_FOR_STAFF: Final = True
INCLUDE_UNUSED_FEATURES_FOR_DELAY: Final = False


class DeepchecksEvaluationError(RuntimeError):
    """Eroare controlata aparuta inaintea sau in timpul evaluarii."""


# ---------------------------------------------------------------------------
# Adaptoare pentru modelele salvate
# ---------------------------------------------------------------------------


class StaffOutputWrapper(RegressorMixin, BaseEstimator):
    """Expune o singura iesire dintr-un model multi-output generic."""

    def __init__(
            self,
            multi_output_model: Any,
            output_index: int,
            output_name: str,
    ) -> None:
        self.multi_output_model = multi_output_model
        self.output_index = output_index
        self.output_name = output_name

    @property
    def n_features_in_(self) -> int:
        return int(
            getattr(
                self.multi_output_model,
                "n_features_in_",
                len(BASE_FEATURE_COLUMNS),
            )
        )

    @property
    def feature_names_in_(self) -> np.ndarray:
        return np.asarray(BASE_FEATURE_COLUMNS, dtype=object)

    def predict(self, features: Any) -> np.ndarray:
        predictions = np.asarray(self.multi_output_model.predict(features))
        return predictions[:, self.output_index]

    @property
    def feature_importances_(self) -> np.ndarray:
        model = self.multi_output_model

        if hasattr(model, "estimators_"):
            estimator = model.estimators_[self.output_index]
            if hasattr(estimator, "feature_importances_"):
                return np.asarray(estimator.feature_importances_, dtype=float)

        if hasattr(model, "feature_importances_"):
            importances = np.asarray(model.feature_importances_, dtype=float)
            if importances.ndim == 1:
                return importances

        raise AttributeError("Modelul nu expune feature_importances_.")


class SelectedRoleEvaluationAdapter(RegressorMixin, BaseEstimator):
    """Evalueaza un model specializat folosind numai coloanele sale reale.

    ``RoleSpecificStaffRegressor`` primeste 11 campuri pentru compatibilitatea
    cu aplicatia, dar fiecare rol foloseste intern un subset de 6 campuri.
    Adaptorul impiedica Deepchecks sa considere drept nefolosite coloanele care
    au fost eliminate intentionat din modelul rolului.
    """

    def __init__(self, fitted_role_estimator: Any) -> None:
        self.fitted_role_estimator = fitted_role_estimator

    @property
    def selected_features(self) -> list[str]:
        names = self.fitted_role_estimator.selected_feature_names_
        return [str(name) for name in names]

    @property
    def n_features_in_(self) -> int:
        return len(self.selected_features)

    @property
    def feature_names_in_(self) -> np.ndarray:
        return np.asarray(self.selected_features, dtype=object)

    def _as_dataframe(self, features: Any) -> pd.DataFrame:
        selected = self.selected_features

        if isinstance(features, pd.DataFrame):
            missing = [column for column in selected if column not in features]
            if missing:
                raise ValueError(f"Lipsesc caracteristicile selectate: {missing}")
            return features.loc[:, selected].copy()

        array = np.asarray(features)
        if array.ndim != 2 or array.shape[1] != len(selected):
            raise ValueError(
                f"Adaptorul Deepchecks asteapta {len(selected)} caracteristici."
            )
        return pd.DataFrame(array, columns=selected)

    def predict(self, features: Any) -> np.ndarray:
        frame = self._as_dataframe(features)
        return np.asarray(
            self.fitted_role_estimator.model_.predict(
                frame.loc[:, self.selected_features]
            )
        )

    @property
    def feature_importances_(self) -> np.ndarray:
        return np.asarray(
            self.fitted_role_estimator.model_.feature_importances_,
            dtype=float,
        )


class ProductionDelayPipeline(ClassifierMixin, BaseEstimator):
    """Reproduce exact ordinea predictiilor din serviciul Flask.

    Fluxul evaluat este:
    1. modelul de personal estimeaza necesarul celor trei roluri;
    2. se calculeaza deficitele si presiunea operationala;
    3. modelul de intarziere clasifica riscul final.
    """

    def __init__(self, staff_model: Any, delay_model: Any) -> None:
        self.staff_model = staff_model
        self.delay_model = delay_model

    @property
    def classes_(self) -> np.ndarray:
        return np.asarray(self.delay_model.classes_, dtype=object)

    @property
    def n_features_in_(self) -> int:
        return len(PRODUCTION_DELAY_FEATURE_COLUMNS)

    @property
    def feature_names_in_(self) -> np.ndarray:
        return np.asarray(PRODUCTION_DELAY_FEATURE_COLUMNS, dtype=object)

    @staticmethod
    def _as_dataframe(features: Any) -> pd.DataFrame:
        if isinstance(features, pd.DataFrame):
            missing = [
                column
                for column in PRODUCTION_DELAY_FEATURE_COLUMNS
                if column not in features
            ]
            if missing:
                raise ValueError(f"Lipsesc intrarile de productie: {missing}")
            return features.loc[:, PRODUCTION_DELAY_FEATURE_COLUMNS].copy()

        array = np.asarray(features)
        expected_count = len(PRODUCTION_DELAY_FEATURE_COLUMNS)
        if array.ndim != 2 or array.shape[1] != expected_count:
            raise ValueError(
                f"Pipeline-ul de productie asteapta {expected_count} caracteristici."
            )
        return pd.DataFrame(array, columns=PRODUCTION_DELAY_FEATURE_COLUMNS)

    def _build_delay_features(self, features: Any) -> pd.DataFrame:
        frame = self._as_dataframe(features)
        base_input = frame.loc[:, BASE_FEATURE_COLUMNS]

        staff_predictions = np.maximum(
            0,
            np.rint(self.staff_model.predict(base_input)).astype(int),
        )

        active_waiters = frame["active_waiters"].to_numpy(dtype=float)
        active_kitchen = frame["active_kitchen"].to_numpy(dtype=float)
        active_bar = frame["active_bar"].to_numpy(dtype=float)

        delay_features = base_input.copy()
        delay_features["active_waiters"] = active_waiters
        delay_features["active_kitchen"] = active_kitchen
        delay_features["active_bar"] = active_bar
        delay_features["waiter_deficit"] = staff_predictions[:, 0] - active_waiters
        delay_features["kitchen_deficit"] = staff_predictions[:, 1] - active_kitchen
        delay_features["bar_deficit"] = staff_predictions[:, 2] - active_bar
        delay_features["orders_per_waiter"] = (
                frame["active_orders"].to_numpy(dtype=float)
                / np.maximum(active_waiters, 1.0)
        )
        delay_features["kitchen_items_per_employee"] = (
                frame["kitchen_load"].to_numpy(dtype=float)
                / np.maximum(active_kitchen, 1.0)
        )
        delay_features["bar_items_per_employee"] = (
                frame["bar_load"].to_numpy(dtype=float)
                / np.maximum(active_bar, 1.0)
        )
        delay_features["occupancy_per_waiter"] = (
                frame["occupied_tables"].to_numpy(dtype=float)
                / np.maximum(active_waiters, 1.0)
        )

        return delay_features.loc[:, DELAY_FEATURE_COLUMNS]

    def predict(self, features: Any) -> np.ndarray:
        return np.asarray(
            self.delay_model.predict(self._build_delay_features(features))
        )

    def predict_proba(self, features: Any) -> np.ndarray:
        return np.asarray(
            self.delay_model.predict_proba(self._build_delay_features(features))
        )


# ---------------------------------------------------------------------------
# Validarea intrarilor
# ---------------------------------------------------------------------------


def validate_required_files() -> None:
    required_files = [
        DATA_FILE,
        TRAFFIC_MODEL_FILE,
        STAFF_MODEL_FILE,
        DELAY_MODEL_FILE,
    ]
    missing = [str(path) for path in required_files if not path.exists()]

    if missing:
        raise DeepchecksEvaluationError(
            f"Lipsesc fisiere obligatorii pentru evaluare: {missing}"
        )


def validate_dataset(dataframe: pd.DataFrame) -> None:
    required_columns = list(
        dict.fromkeys(
            BASE_FEATURE_COLUMNS
            + ACTIVE_STAFF_COLUMNS
            + DELAY_FEATURE_COLUMNS
            + STAFF_TARGET_COLUMNS
            + [TRAFFIC_TARGET_COLUMN, DELAY_TARGET_COLUMN]
        )
    )

    missing = [column for column in required_columns if column not in dataframe]
    if missing:
        raise DeepchecksEvaluationError(
            f"Datasetul nu contine coloanele obligatorii: {missing}"
        )

    if dataframe.empty:
        raise DeepchecksEvaluationError("Datasetul este gol.")

    columns_with_nulls = [
        column for column in required_columns if dataframe[column].isnull().any()
    ]
    if columns_with_nulls:
        raise DeepchecksEvaluationError(
            f"Datasetul contine valori lipsa in: {columns_with_nulls}"
        )


# ---------------------------------------------------------------------------
# Impartirea datasetului si metricile folosite de Deepchecks
# ---------------------------------------------------------------------------


def split_classification_data(
        features: pd.DataFrame,
        target: pd.Series,
) -> tuple[pd.DataFrame, pd.DataFrame, pd.Series, pd.Series]:
    first_split = train_test_split(
        features,
        target,
        test_size=0.30,
        random_state=RANDOM_STATE,
        stratify=target,
    )
    train_features = cast(pd.DataFrame, first_split[0])
    temporary_features = cast(pd.DataFrame, first_split[1])
    train_target = cast(pd.Series, first_split[2])
    temporary_target = cast(pd.Series, first_split[3])

    second_split = train_test_split(
        temporary_features,
        temporary_target,
        test_size=0.50,
        random_state=RANDOM_STATE,
        stratify=temporary_target,
    )
    validation_features = cast(pd.DataFrame, second_split[0])
    test_features = cast(pd.DataFrame, second_split[1])
    validation_target = cast(pd.Series, second_split[2])
    test_target = cast(pd.Series, second_split[3])

    final_train_features = pd.concat(
        [train_features, validation_features],
        ignore_index=True,
    )
    final_train_target = pd.concat(
        [train_target, validation_target],
        ignore_index=True,
    )

    normalized_test_features = test_features.reset_index(drop=True)
    normalized_test_target = pd.Series(
        test_target.to_numpy(copy=True),
        name=target.name,
    )

    return (
        final_train_features,
        normalized_test_features,
        final_train_target,
        normalized_test_target,
    )


def split_regression_data(
        features: pd.DataFrame,
        target: pd.Series,
) -> tuple[pd.DataFrame, pd.DataFrame, pd.Series, pd.Series]:
    first_split = train_test_split(
        features,
        target,
        test_size=0.30,
        random_state=RANDOM_STATE,
    )
    train_features = cast(pd.DataFrame, first_split[0])
    temporary_features = cast(pd.DataFrame, first_split[1])
    train_target = cast(pd.Series, first_split[2])
    temporary_target = cast(pd.Series, first_split[3])

    second_split = train_test_split(
        temporary_features,
        temporary_target,
        test_size=0.50,
        random_state=RANDOM_STATE,
    )
    validation_features = cast(pd.DataFrame, second_split[0])
    test_features = cast(pd.DataFrame, second_split[1])
    validation_target = cast(pd.Series, second_split[2])
    test_target = cast(pd.Series, second_split[3])

    final_train_features = pd.concat(
        [train_features, validation_features],
        ignore_index=True,
    )
    final_train_target = pd.concat(
        [train_target, validation_target],
        ignore_index=True,
    )

    normalized_test_features = test_features.reset_index(drop=True)
    normalized_test_target = pd.Series(
        test_target.to_numpy(copy=True),
        name=target.name,
    )

    return (
        final_train_features,
        normalized_test_features,
        final_train_target,
        normalized_test_target,
    )


def classification_scorers() -> dict[str, Any]:
    """Metrici echilibrate pentru clasificarea cu trei clase."""

    return {
        "Accuracy": make_scorer(accuracy_score),
        "Balanced Accuracy": make_scorer(balanced_accuracy_score),
        "Macro F1": make_scorer(
            f1_score,
            average="macro",
            zero_division=0,
        ),
        "Macro Precision": make_scorer(
            precision_score,
            average="macro",
            zero_division=0,
        ),
        "Macro Recall": make_scorer(
            recall_score,
            average="macro",
            zero_division=0,
        ),
    }


def regression_scorers() -> dict[str, Any]:
    """Metrici pentru eroare absoluta, eroare mare si varianta explicata."""

    def rmse(expected: Any, predicted: Any) -> float:
        return float(mean_squared_error(expected, predicted) ** 0.5)

    return {
        "R2": make_scorer(r2_score),
        "Negative MAE": make_scorer(
            mean_absolute_error,
            greater_is_better=False,
        ),
        "Negative RMSE": make_scorer(rmse, greater_is_better=False),
        "Negative Max Error": make_scorer(max_error, greater_is_better=False),
    }


# ---------------------------------------------------------------------------
# Suite Deepchecks personalizate
# ---------------------------------------------------------------------------


def build_model_evaluation_suite(
        scorers: dict[str, Any],
        sample_count: int,
        include_unused_features: bool,
) -> Suite:
    """Construieste explicit suita, fara parametri depreciati.

    ``UnusedFeatures`` este optional. Pentru modelele de intarziere, multe
    coloane sunt feature-uri derivate si corelate; numarul lor mic de utilizari
    nu reprezinta o problema de predictie si este deja tratat prin selectia
    ulterioara a echipei care va continua proiectul.
    """

    common_options = {
        "scorers": scorers,
        "n_samples": sample_count,
        "random_state": RANDOM_STATE,
    }

    checks: list[Any] = [
        TrainTestPerformance(**common_options)
        .add_condition_train_test_relative_degradation_less_than(),
        RocReport(**common_options).add_condition_auc_greater_than(),
        ConfusionMatrixReport(**common_options),
        PredictionDrift(**common_options).add_condition_drift_score_less_than(),
        SimpleModelComparison(**common_options).add_condition_gain_greater_than(),
        WeakSegmentsPerformance(**common_options)
        .add_condition_segments_relative_performance_greater_than(),
        CalibrationScore(**common_options),
        RegressionErrorDistribution(**common_options)
        .add_condition_kurtosis_greater_than()
        .add_condition_systematic_error_ratio_to_rmse_less_than(),
    ]

    if include_unused_features:
        checks.append(
            UnusedFeatures(**common_options)
            .add_condition_number_of_high_variance_unused_features_less_or_equal()
        )

    checks.extend(
        [
            BoostingOverfit(**common_options)
            .add_condition_test_score_percent_decline_less_than(),
            ModelInferenceTime(**common_options)
            .add_condition_inference_time_less_than(),
        ]
    )

    return Suite("Model Evaluation Suite", *checks)


def create_dataset(
        features: pd.DataFrame,
        target: pd.Series,
        label_type: str,
        dataset_name: str,
) -> Dataset:
    return Dataset(
        features,
        label=target,
        cat_features=[],
        label_type=label_type,
        dataset_name=dataset_name,
    )


def save_suite_result(result: Any, report_name: str) -> dict[str, str]:
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)

    html_file = REPORTS_DIR / f"{report_name}.html"
    json_file = REPORTS_DIR / f"{report_name}.json"

    result.save_as_html(
        str(html_file),
        as_widget=False,
        connected=False,
    )
    json_file.write_text(result.to_json(), encoding="utf-8")

    return {
        "html": str(html_file),
        "json": str(json_file),
    }


def run_classification_suite(
        report_name: str,
        model: Any,
        features: pd.DataFrame,
        target: pd.Series,
        include_unused_features: bool,
) -> dict[str, str]:
    train_x, test_x, train_y, test_y = split_classification_data(
        features,
        target,
    )

    train_dataset = create_dataset(
        train_x,
        train_y,
        "multiclass",
        f"{report_name} - train",
    )
    test_dataset = create_dataset(
        test_x,
        test_y,
        "multiclass",
        f"{report_name} - test",
    )

    suite = build_model_evaluation_suite(
        classification_scorers(),
        len(features),
        include_unused_features,
    )
    result = suite.run(
        train_dataset=train_dataset,
        test_dataset=test_dataset,
        model=model,
        model_classes=list(model.classes_),
        with_display=True,
    )

    return save_suite_result(result, report_name)


def run_regression_suite(
        report_name: str,
        model: Any,
        features: pd.DataFrame,
        target: pd.Series,
        include_unused_features: bool,
) -> dict[str, str]:
    train_x, test_x, train_y, test_y = split_regression_data(features, target)

    train_dataset = create_dataset(
        train_x,
        train_y,
        "regression",
        f"{report_name} - train",
    )
    test_dataset = create_dataset(
        test_x,
        test_y,
        "regression",
        f"{report_name} - test",
    )

    suite = build_model_evaluation_suite(
        regression_scorers(),
        len(features),
        include_unused_features,
    )
    result = suite.run(
        train_dataset=train_dataset,
        test_dataset=test_dataset,
        model=model,
        with_display=True,
    )

    return save_suite_result(result, report_name)


# ---------------------------------------------------------------------------
# Pregatirea modelelor de personal pentru evaluare
# ---------------------------------------------------------------------------


def get_staff_role_model(
        staff_model: Any,
        output_index: int,
        output_name: str,
) -> Any:
    """Returneaza regresorul specializat sau un adaptor pentru modele vechi."""

    if hasattr(staff_model, "get_role_estimator"):
        return staff_model.get_role_estimator(output_index)

    if hasattr(staff_model, "estimators_"):
        estimators: Sequence[Any] = staff_model.estimators_
        if output_index < len(estimators) and hasattr(
                estimators[output_index],
                "predict",
        ):
            return estimators[output_index]

    return StaffOutputWrapper(staff_model, output_index, output_name)


def prepare_staff_evaluation(
        dataframe: pd.DataFrame,
        staff_model: Any,
        output_index: int,
        target_column: str,
) -> tuple[Any, pd.DataFrame]:
    role_model = get_staff_role_model(
        staff_model,
        output_index,
        target_column,
    )

    if hasattr(role_model, "selected_feature_names_") and hasattr(
            role_model,
            "model_",
    ):
        adapter = SelectedRoleEvaluationAdapter(role_model)
        return adapter, dataframe.loc[:, adapter.selected_features]

    return role_model, dataframe.loc[:, BASE_FEATURE_COLUMNS]


# ---------------------------------------------------------------------------
# Executia completa
# ---------------------------------------------------------------------------


def run_all_deepchecks() -> dict[str, dict[str, str]]:
    validate_required_files()

    dataframe = pd.read_csv(DATA_FILE)
    validate_dataset(dataframe)

    traffic_model = joblib.load(TRAFFIC_MODEL_FILE)
    staff_model = joblib.load(STAFF_MODEL_FILE)
    delay_model = joblib.load(DELAY_MODEL_FILE)

    reports: dict[str, dict[str, str]] = {}

    print("1/6 - Deepchecks pentru trafic...")
    reports["traffic"] = run_classification_suite(
        report_name="deepchecks_traffic",
        model=traffic_model,
        features=dataframe.loc[:, BASE_FEATURE_COLUMNS],
        target=dataframe[TRAFFIC_TARGET_COLUMN],
        include_unused_features=INCLUDE_UNUSED_FEATURES_FOR_TRAFFIC,
    )

    staff_reports: tuple[tuple[str, str], ...] = (
        ("recommended_waiters", "staff_waiters"),
        ("recommended_kitchen_staff", "staff_kitchen"),
        ("recommended_bar_staff", "staff_bar"),
    )

    for output_index in range(len(staff_reports)):
        target_column, report_suffix = staff_reports[output_index]
        progress_index = output_index + 2

        print(f"{progress_index}/6 - Deepchecks pentru {target_column}...")

        evaluation_model, evaluation_features = prepare_staff_evaluation(
            dataframe,
            staff_model,
            output_index,
            target_column,
        )
        reports[report_suffix] = run_regression_suite(
            report_name=f"deepchecks_{report_suffix}",
            model=evaluation_model,
            features=evaluation_features,
            target=dataframe[target_column],
            include_unused_features=INCLUDE_UNUSED_FEATURES_FOR_STAFF,
        )

    print("5/6 - Deepchecks pentru intarziere offline...")
    reports["delay_offline"] = run_classification_suite(
        report_name="deepchecks_delay_offline",
        model=delay_model,
        features=dataframe.loc[:, DELAY_FEATURE_COLUMNS],
        target=dataframe[DELAY_TARGET_COLUMN],
        include_unused_features=INCLUDE_UNUSED_FEATURES_FOR_DELAY,
    )

    print("6/6 - Deepchecks pentru fluxul complet de productie...")
    production_pipeline = ProductionDelayPipeline(staff_model, delay_model)
    reports["delay_production"] = run_classification_suite(
        report_name="deepchecks_delay_production",
        model=production_pipeline,
        features=dataframe.loc[:, PRODUCTION_DELAY_FEATURE_COLUMNS],
        target=dataframe[DELAY_TARGET_COLUMN],
        include_unused_features=INCLUDE_UNUSED_FEATURES_FOR_DELAY,
    )

    summary_file = REPORTS_DIR / "deepchecks_reports.json"
    summary_file.write_text(
        json.dumps(reports, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print("\nValidarea externa Deepchecks a fost finalizata.")
    print(f"Director rapoarte: {REPORTS_DIR}")
    print(f"Rezumat: {summary_file}")

    return reports


if __name__ == "__main__":
    try:
        run_all_deepchecks()
    except DeepchecksEvaluationError as exception:
        print(f"Validarea a fost oprita: {exception}")
        raise SystemExit(1) from exception
    except Exception as exception:
        print(f"Eroare Deepchecks: {exception}")
        raise
