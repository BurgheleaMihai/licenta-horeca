from pathlib import Path

import joblib
import pandas as pd

from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error
from sklearn.model_selection import train_test_split


DATA_FILE = Path("data") / "synthetic_horeca_dataset.csv"
MODEL_FILE = Path("models") / "staff_model.pkl"
REPORT_FILE = Path("reports") / "staff_metrics.txt"


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
    "item_count"
]


TARGET_COLUMNS = [
    "recommended_waiters",
    "recommended_kitchen_staff",
    "recommended_bar_staff"
]


def train_staff_model():
    df = pd.read_csv(DATA_FILE)

    x = df[FEATURE_COLUMNS]
    y = df[TARGET_COLUMNS]

    x_train, x_test, y_train, y_test = train_test_split(
        x,
        y,
        test_size=0.2,
        random_state=42
    )

    model = RandomForestRegressor(
        n_estimators=100,
        random_state=42,
        max_depth=10
    )

    model.fit(x_train, y_train)

    predictions = model.predict(x_test)

    mae = mean_absolute_error(y_test, predictions)

    MODEL_FILE.parent.mkdir(parents=True, exist_ok=True)
    REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)

    joblib.dump(model, MODEL_FILE)

    report_text = f"""
Model recomandare personal - Random Forest Regressor

Scop:
Modelul recomanda numarul de angajati necesari pentru activitatea curenta.

Output:
- numar recomandat de ospatari
- numar recomandat pentru bucatarie
- numar recomandat pentru bar

Dataset:
{DATA_FILE}

Numar total randuri:
{len(df)}

Mean Absolute Error:
{mae:.4f}

Observatie:
Eroarea medie arata cu cat greseste modelul, in medie, fata de valoarea recomandata din dataset.
Pentru o varianta demonstrativa, o eroare mica este suficienta.
"""

    REPORT_FILE.write_text(report_text, encoding="utf-8")

    print("Modelul de recomandare personal a fost antrenat.")
    print(f"Model salvat in: {MODEL_FILE}")
    print(f"Raport salvat in: {REPORT_FILE}")
    print(f"Eroare medie personal: {mae:.4f}")


if __name__ == "__main__":
    train_staff_model()