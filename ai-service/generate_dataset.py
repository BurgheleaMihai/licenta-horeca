import random
from pathlib import Path

import numpy as np
import pandas as pd


OUTPUT_DIR = Path("data")
OUTPUT_FILE = OUTPUT_DIR / "synthetic_horeca_dataset.csv"

random.seed(42)
np.random.seed(42)


def get_base_traffic_score(day_of_week, hour):
    """
    day_of_week:
    0 = luni
    1 = marti
    2 = miercuri
    3 = joi
    4 = vineri
    5 = sambata
    6 = duminica
    """

    score = 0

    # Locatie din complex studentesc:
    # miercuri si joi sunt de obicei mai aglomerate
    if day_of_week in [2, 3]:
        score += 3

    # luni si marti sunt zile normale
    if day_of_week in [0, 1]:
        score += 1

    # vineri incepe sa scada, pentru ca o parte dintre studenti pleaca
    if day_of_week == 4:
        score += 0

    # weekend mai slab, pentru ca multi studenti pleaca acasa
    if day_of_week in [5, 6]:
        score -= 2

    # interval de pranz
    if 11 <= hour <= 14:
        score += 3

    # interval de seara
    if 18 <= hour <= 21:
        score += 2

    # dimineata devreme si seara tarziu sunt intervale mai slabe
    if hour < 10 or hour > 22:
        score -= 2

    return score


def label_traffic(score):
    if score <= 2:
        return "SCAZUT"
    if score <= 6:
        return "MEDIU"
    return "RIDICAT"


def label_delay_risk(active_orders, kitchen_load, bar_load, order_age_minutes, item_count, avg_preparation_time):
    risk_score = 0

    if active_orders >= 8:
        risk_score += 2
    elif active_orders >= 4:
        risk_score += 1

    if kitchen_load >= 10:
        risk_score += 2
    elif kitchen_load >= 5:
        risk_score += 1

    if bar_load >= 8:
        risk_score += 1

    if order_age_minutes >= 25:
        risk_score += 2
    elif order_age_minutes >= 15:
        risk_score += 1

    if item_count >= 6:
        risk_score += 1

    if avg_preparation_time >= 25:
        risk_score += 1

    if risk_score <= 2:
        return "SCAZUT"
    if risk_score <= 5:
        return "MEDIU"
    return "RIDICAT"


def recommend_staff(traffic_level, active_orders, kitchen_load, bar_load, occupied_tables):
    waiters = 1
    kitchen_staff = 1
    bar_staff = 1

    if traffic_level == "MEDIU":
        waiters += 1
        kitchen_staff += 1

    if traffic_level == "RIDICAT":
        waiters += 2
        kitchen_staff += 2
        bar_staff += 1

    if occupied_tables >= 8:
        waiters += 1

    if active_orders >= 8:
        waiters += 1

    if kitchen_load >= 10:
        kitchen_staff += 1

    if bar_load >= 8:
        bar_staff += 1

    return waiters, kitchen_staff, bar_staff


def generate_row():
    day_of_week = random.randint(0, 6)

    # Program estimat pentru restaurant
    hour = random.randint(8, 23)

    base_score = get_base_traffic_score(day_of_week, hour)

    # Variatie mica, ca datele sa nu fie perfect mecanice
    noise = random.randint(-1, 2)
    traffic_score = base_score + noise

    traffic_level = label_traffic(traffic_score)

    if traffic_level == "SCAZUT":
        active_orders = random.randint(0, 3)
        occupied_tables = random.randint(0, 3)
        estimated_occupancy = random.randint(5, 30)
        orders_last_30_min = random.randint(0, 5)

    elif traffic_level == "MEDIU":
        active_orders = random.randint(3, 8)
        occupied_tables = random.randint(3, 8)
        estimated_occupancy = random.randint(30, 65)
        orders_last_30_min = random.randint(5, 14)

    else:
        active_orders = random.randint(8, 15)
        occupied_tables = random.randint(7, 12)
        estimated_occupancy = random.randint(65, 100)
        orders_last_30_min = random.randint(12, 25)

    kitchen_load = max(0, int(active_orders * random.uniform(0.8, 1.7)))
    bar_load = max(0, int(active_orders * random.uniform(0.4, 1.2)))

    avg_preparation_time = random.randint(8, 30)
    order_age_minutes = random.randint(0, 40)
    item_count = random.randint(1, 8)

    delay_risk = label_delay_risk(
        active_orders,
        kitchen_load,
        bar_load,
        order_age_minutes,
        item_count,
        avg_preparation_time
    )

    recommended_waiters, recommended_kitchen_staff, recommended_bar_staff = recommend_staff(
        traffic_level,
        active_orders,
        kitchen_load,
        bar_load,
        occupied_tables
    )

    return {
        "day_of_week": day_of_week,
        "hour": hour,
        "active_orders": active_orders,
        "occupied_tables": occupied_tables,
        "estimated_occupancy": estimated_occupancy,
        "kitchen_load": kitchen_load,
        "bar_load": bar_load,
        "avg_preparation_time": avg_preparation_time,
        "orders_last_30_min": orders_last_30_min,
        "order_age_minutes": order_age_minutes,
        "item_count": item_count,
        "traffic_level": traffic_level,
        "recommended_waiters": recommended_waiters,
        "recommended_kitchen_staff": recommended_kitchen_staff,
        "recommended_bar_staff": recommended_bar_staff,
        "delay_risk": delay_risk
    }


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    rows = [generate_row() for _ in range(3000)]

    df = pd.DataFrame(rows)
    df.to_csv(OUTPUT_FILE, index=False)

    print(f"Dataset generat cu succes: {OUTPUT_FILE}")
    print(f"Numar randuri: {len(df)}")
    print()
    print("Distributie trafic:")
    print(df["traffic_level"].value_counts())
    print()
    print("Distributie risc intarziere:")
    print(df["delay_risk"].value_counts())


if __name__ == "__main__":
    main()