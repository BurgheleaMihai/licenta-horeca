from pathlib import Path

import joblib
import pandas as pd

from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
from sklearn.model_selection import train_test_split


DATA_FILE = Path("data") / "synthetic_horeca_dataset.csv"
MODEL_FILE = Path("models") / "delay_model.pkl"
REPORT_FILE = Path("reports") / "delay_metrics.txt"


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


def train_delay_model():
    df = pd.read_csv(DATA_FILE)

    x = df[FEATURE_COLUMNS]
    y = df["delay_risk"]

    x_train, x_test, y_train, y_test = train_test_split(
        x,
        y,
        test_size=0.2,
        random_state=42,
        stratify=y
    )

    model = RandomForestClassifier(
        n_estimators=100,
        random_state=42,
        max_depth=10
    )

    model.fit(x_train, y_train)

    predictions = model.predict(x_test)

    accuracy = accuracy_score(y_test, predictions)
    report = classification_report(y_test, predictions)

    MODEL_FILE.parent.mkdir(parents=True, exist_ok=True)
    REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)

    joblib.dump(model, MODEL_FILE)

    report_text = f"""
Model risc intarziere - Random Forest Classifier

Scop:
Modelul prezice riscul ca o comanda sau activitatea curenta sa produca intarzieri.

Output:
- SCAZUT
- MEDIU
- RIDICAT

Dataset:
{DATA_FILE}

Numar total randuri:
{len(df)}

Acuratete:
{accuracy:.4f}

Raport clasificare:
{report}
"""

    REPORT_FILE.write_text(report_text, encoding="utf-8")

    print("Modelul de risc intarziere a fost antrenat.")
    print(f"Model salvat in: {MODEL_FILE}")
    print(f"Raport salvat in: {REPORT_FILE}")
    print(f"Acuratete risc intarziere: {accuracy:.4f}")


if __name__ == "__main__":
    train_delay_model()