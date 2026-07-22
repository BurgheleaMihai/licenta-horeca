"""
Arhitectura modelului specializat pentru recomandarea personalului HoReCa.

Modelul multi-output contine cate un regresor Gradient Boosting pentru:
- ospatari;
- personalul din bucatarie;
- personalul de la bar.

Fiecare rol foloseste numai caracteristicile relevante pentru activitatea sa.
Clasele respecta interfata scikit-learn si pot fi folosite cu clone(),
cross-validation, Deepchecks si procesul de reantrenare.
"""

from __future__ import annotations

from typing import Any, Self, TypeAlias

import numpy as np
import pandas as pd
from sklearn.base import BaseEstimator, RegressorMixin
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.utils.validation import check_is_fitted


FeatureInput: TypeAlias = pd.DataFrame | np.ndarray
TargetInput: TypeAlias = pd.DataFrame | pd.Series | np.ndarray


# ---------------------------------------------------------------------------
# Coloanele modelului
# ---------------------------------------------------------------------------

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

TARGET_COLUMNS = [
    "recommended_waiters",
    "recommended_kitchen_staff",
    "recommended_bar_staff",
]

ROLE_FEATURE_COLUMNS = {
    "recommended_waiters": [
        "day_of_week",
        "hour",
        "active_orders",
        "occupied_tables",
        "estimated_occupancy",
        "orders_last_30_min",
    ],
    "recommended_kitchen_staff": [
        "day_of_week",
        "hour",
        "active_orders",
        "kitchen_load",
        "item_count",
        "avg_preparation_time",
    ],
    "recommended_bar_staff": [
        "day_of_week",
        "hour",
        "bar_load",
        "item_count",
        "estimated_occupancy",
        "orders_last_30_min",
    ],
}


# ---------------------------------------------------------------------------
# Regresor specializat pentru un singur rol
# ---------------------------------------------------------------------------


class RoleStaffRegressor(RegressorMixin, BaseEstimator):
    """Regresor pentru un singur rol, cu selectie explicita de caracteristici."""

    def __init__(self, role: str, random_state: int = 42):
        # Parametrii constructorului raman nemodificati pentru compatibilitatea
        # cu sklearn.clone().
        self.role = role
        self.random_state = random_state

        # Atribute invatate in fit(). Initializarea explicita elimina
        # warning-urile IntelliJ. __sklearn_is_fitted__ pastreaza detectarea
        # corecta a starii antrenat/neantrenat.
        self.model_: GradientBoostingRegressor | None = None
        self.feature_names_in_: np.ndarray | None = None
        self.n_features_in_: int | None = None
        self.selected_feature_names_: np.ndarray | None = None

    def __sklearn_is_fitted__(self) -> bool:
        """Indica scikit-learn daca regresorul a fost antrenat."""

        return self.model_ is not None and self.selected_feature_names_ is not None

    @staticmethod
    def _as_dataframe(features: FeatureInput) -> pd.DataFrame:
        """Converteste intrarea intr-un DataFrame cu cele 11 coloane asteptate."""

        if isinstance(features, pd.DataFrame):
            missing_columns = [
                column for column in FEATURE_COLUMNS if column not in features.columns
            ]

            if missing_columns:
                raise ValueError(
                    f"Lipsesc caracteristicile obligatorii: {missing_columns}"
                )

            return features.loc[:, FEATURE_COLUMNS].copy()

        feature_array = np.asarray(features, dtype=float)

        if feature_array.ndim != 2 or feature_array.shape[1] != len(FEATURE_COLUMNS):
            raise ValueError(
                f"Modelul de personal asteapta {len(FEATURE_COLUMNS)} caracteristici."
            )

        return pd.DataFrame(
            feature_array,
            columns=FEATURE_COLUMNS,
        )

    def _build_regressor(self) -> GradientBoostingRegressor:
        """Construieste regresorul potrivit rolului curent."""

        # Modelul pentru bar este regularizat mai puternic deoarece tinta are
        # o variatie mai mica si necesita protectie suplimentara la overfitting.
        if self.role == "recommended_bar_staff":
            return GradientBoostingRegressor(
                n_estimators=80,
                learning_rate=0.04,
                max_depth=2,
                min_samples_leaf=20,
                min_samples_split=40,
                subsample=0.80,
                loss="squared_error",
                random_state=self.random_state,
            )

        return GradientBoostingRegressor(
            n_estimators=200,
            learning_rate=0.05,
            max_depth=3,
            min_samples_leaf=1,
            min_samples_split=2,
            subsample=1.0,
            loss="squared_error",
            random_state=self.random_state,
        )

    def _require_fitted_state(
            self,
    ) -> tuple[GradientBoostingRegressor, np.ndarray]:
        """Returneaza atributele invatate dupa verificarea starii modelului."""

        check_is_fitted(self)

        if self.model_ is None or self.selected_feature_names_ is None:
            raise RuntimeError("Regresorul pentru rol nu este antrenat.")

        return self.model_, self.selected_feature_names_

    def fit(self, features: FeatureInput, target: Any) -> Self:
        """Antreneaza regresorul folosind caracteristicile rolului."""

        if self.role not in ROLE_FEATURE_COLUMNS:
            raise ValueError(f"Rol necunoscut: {self.role}")

        feature_frame = self._as_dataframe(features)
        selected_columns = ROLE_FEATURE_COLUMNS[self.role]
        target_array = np.asarray(target, dtype=float).reshape(-1)

        if len(feature_frame) != len(target_array):
            raise ValueError(
                "Numarul de randuri al caracteristicilor trebuie sa fie "
                "egal cu numarul valorilor tinta."
            )

        fitted_model = self._build_regressor()
        fitted_model.fit(
            feature_frame[selected_columns],
            target_array,
        )

        self.model_ = fitted_model
        self.feature_names_in_ = np.asarray(
            FEATURE_COLUMNS,
            dtype=object,
        )
        self.n_features_in_ = len(FEATURE_COLUMNS)
        self.selected_feature_names_ = np.asarray(
            selected_columns,
            dtype=object,
        )

        return self

    def predict(self, features: FeatureInput) -> np.ndarray:
        """Prezice necesarul de personal pentru rolul curent."""

        model, selected_feature_names = self._require_fitted_state()
        feature_frame = self._as_dataframe(features)
        selected_columns = [str(column) for column in selected_feature_names]

        return np.asarray(
            model.predict(feature_frame[selected_columns]),
            dtype=float,
        )

    @property
    def feature_importances_(self) -> np.ndarray:
        """Returneaza importanta tuturor celor 11 caracteristici."""

        model, selected_feature_names = self._require_fitted_state()

        full_importances = np.zeros(
            len(FEATURE_COLUMNS),
            dtype=float,
        )
        index_by_name = {name: index for index, name in enumerate(FEATURE_COLUMNS)}

        for name, importance in zip(
                selected_feature_names,
                model.feature_importances_,
                strict=True,
        ):
            full_importances[index_by_name[str(name)]] = float(importance)

        return full_importances


# ---------------------------------------------------------------------------
# Model multi-output pentru toate cele trei roluri
# ---------------------------------------------------------------------------


class RoleSpecificStaffRegressor(RegressorMixin, BaseEstimator):
    """Model clonabil cu un regresor specializat pentru fiecare rol."""

    def __init__(self, random_state: int = 42):
        # Constructorul contine numai parametrul necesar pentru clonare.
        self.random_state = random_state

        # Atribute invatate in fit(), initializate explicit pentru IntelliJ.
        self.estimators_: list[RoleStaffRegressor] | None = None
        self.feature_names_in_: np.ndarray | None = None
        self.n_features_in_: int | None = None
        self.n_outputs_: int | None = None

    def __sklearn_is_fitted__(self) -> bool:
        """Indica scikit-learn daca toate regresorarele au fost create."""

        return self.estimators_ is not None and len(self.estimators_) == len(
            TARGET_COLUMNS
        )

    @staticmethod
    def _as_target_array(target: TargetInput) -> np.ndarray:
        """Converteste tintele intr-o matrice numerica cu trei coloane."""

        if isinstance(target, pd.DataFrame):
            if all(column in target.columns for column in TARGET_COLUMNS):
                values = target.loc[
                         :,
                         TARGET_COLUMNS,
                         ].to_numpy(dtype=float)
            elif target.shape[1] == len(TARGET_COLUMNS):
                # La reantrenare, coloanele pot avea denumiri precum
                # actual_waiters, actual_kitchen_staff si actual_bar_staff.
                # Ordinea lor devine ordinea celor trei iesiri.
                values = target.to_numpy(dtype=float)
            else:
                raise ValueError("Modelul necesita exact trei tinte de personal.")
        else:
            values = np.asarray(target, dtype=float)

        if values.ndim != 2 or values.shape[1] != len(TARGET_COLUMNS):
            raise ValueError("Modelul necesita exact trei tinte de personal.")

        return values

    def _require_estimators(self) -> list[RoleStaffRegressor]:
        """Returneaza regresorarele dupa verificarea starii modelului."""

        check_is_fitted(self)

        if self.estimators_ is None:
            raise RuntimeError("Modelul de personal nu este antrenat.")

        return self.estimators_

    def fit(
            self,
            features: FeatureInput,
            target: TargetInput,
    ) -> Self:
        """Antreneaza separat regresorul fiecarui rol."""

        target_array = self._as_target_array(target)
        fitted_estimators: list[RoleStaffRegressor] = []

        for target_index, role in enumerate(TARGET_COLUMNS):
            estimator = RoleStaffRegressor(
                role=role,
                random_state=self.random_state,
            )
            estimator.fit(
                features,
                target_array[:, target_index],
            )
            fitted_estimators.append(estimator)

        self.estimators_ = fitted_estimators
        self.feature_names_in_ = np.asarray(
            FEATURE_COLUMNS,
            dtype=object,
        )
        self.n_features_in_ = len(FEATURE_COLUMNS)
        self.n_outputs_ = len(TARGET_COLUMNS)

        return self

    def predict(self, features: FeatureInput) -> np.ndarray:
        """Returneaza cele trei recomandari in ordinea TARGET_COLUMNS."""

        estimators = self._require_estimators()
        predictions = [estimator.predict(features) for estimator in estimators]

        return np.column_stack(predictions)

    def get_role_estimator(
            self,
            role_or_index: str | int,
    ) -> RoleStaffRegressor:
        """Returneaza regresorul asociat unui rol sau unui index."""

        estimators = self._require_estimators()

        if isinstance(role_or_index, str):
            try:
                estimator_index = TARGET_COLUMNS.index(role_or_index)
            except ValueError as caught_error:
                raise ValueError(f"Rol necunoscut: {role_or_index}") from caught_error
        else:
            estimator_index = int(role_or_index)

        if not 0 <= estimator_index < len(estimators):
            raise IndexError(
                f"Indexul rolului trebuie sa fie intre 0 si {len(estimators) - 1}."
            )

        return estimators[estimator_index]

    def role_feature_importances(self) -> dict[str, np.ndarray]:
        """Returneaza importanta caracteristicilor pentru fiecare rol."""

        estimators = self._require_estimators()

        return {
            role: estimator.feature_importances_.copy()
            for role, estimator in zip(
                TARGET_COLUMNS,
                estimators,
                strict=True,
            )
        }