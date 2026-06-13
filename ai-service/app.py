from pathlib import Path

import joblib
import pandas as pd
from flask import Flask, jsonify, request
from flask_cors import CORS


app = Flask(__name__)
CORS(app)


MODELS_DIR = Path("models")

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
    "item_count"
]


traffic_model = joblib.load(TRAFFIC_MODEL_FILE)
staff_model = joblib.load(STAFF_MODEL_FILE)
delay_model = joblib.load(DELAY_MODEL_FILE)


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "AI Service is running"
    })


@app.route("/predict/all", methods=["POST"])
def predict_all():
    data = request.get_json()

    if data is None:
        return jsonify({
            "error": "Request body is missing or invalid"
        }), 400

    missing_fields = []

    for column in FEATURE_COLUMNS:
        if column not in data:
            missing_fields.append(column)

    if missing_fields:
        return jsonify({
            "error": "Missing required fields",
            "missingFields": missing_fields
        }), 400

    input_data = pd.DataFrame([data], columns=FEATURE_COLUMNS)

    traffic_prediction = traffic_model.predict(input_data)[0]
    staff_prediction = staff_model.predict(input_data)[0]
    delay_prediction = delay_model.predict(input_data)[0]

    recommended_waiters = round(staff_prediction[0])
    recommended_kitchen_staff = round(staff_prediction[1])
    recommended_bar_staff = round(staff_prediction[2])

    response = {
        "trafficLevel": traffic_prediction,
        "recommendedWaiters": int(recommended_waiters),
        "recommendedKitchenStaff": int(recommended_kitchen_staff),
        "recommendedBarStaff": int(recommended_bar_staff),
        "delayRisk": delay_prediction
    }

    return jsonify(response)


if __name__ == "__main__":
    app.run(
        host="localhost",
        port=5000,
        debug=True
    )