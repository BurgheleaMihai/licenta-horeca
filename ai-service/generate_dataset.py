"""
Genereaza datasetul sintetic folosit pentru antrenarea modelelor HoReCa.

Scenariile simuleaza un restaurant amplasat intr-un complex studentesc:
- miercuri si joi sunt, in general, mai aglomerate;
- weekendul are trafic mai redus;
- pranzul si seara reprezinta intervalele de varf;
- vacantele, examenele si evenimentele din campus modifica cererea;
- absentele si deficitul de personal influenteaza riscul de intarziere.

Scriptul pastreaza aceeasi logica de generare, aceleasi praguri si aceleasi
probabilitati. Modificarile sunt exclusiv de formatare si documentare.
"""

import math
from pathlib import Path

import numpy as np
import pandas as pd


# ---------------------------------------------------------------------------
# Configurare
# ---------------------------------------------------------------------------

BASE_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = BASE_DIR / "data"
OUTPUT_FILE = OUTPUT_DIR / "synthetic_horeca_dataset.csv"

RANDOM_SEED = 42
ROW_COUNT = 5000
TOTAL_TABLES = 12

rng = np.random.default_rng(RANDOM_SEED)


# ---------------------------------------------------------------------------
# Functii generale
# ---------------------------------------------------------------------------


def clamp(value, minimum, maximum):
    """Limiteaza o valoare la intervalul [minimum, maximum]."""

    return max(minimum, min(value, maximum))


# ---------------------------------------------------------------------------
# Trafic si context calendaristic
# ---------------------------------------------------------------------------


def get_base_traffic_score(
    day_of_week,
    hour,
    is_exam_period,
    is_holiday_period,
    campus_event_level,
):
    """
    Calculeaza scorul de baza al traficului.

    Codificarea zilelor:
    0 = luni, 1 = marti, 2 = miercuri, 3 = joi,
    4 = vineri, 5 = sambata, 6 = duminica.
    """

    score = 0.0

    # Restaurantul este amplasat intr-un complex studentesc. Miercuri si joi
    # sunt zilele cele mai aglomerate, iar weekendul are trafic mai redus.
    if day_of_week in [2, 3]:
        score += 2.2
    elif day_of_week in [0, 1]:
        score += 0.8
    elif day_of_week == 4:
        score += 0.2
    else:
        score -= 1.8

    # Varfurile uzuale apar la pranz si seara.
    if 11 <= hour <= 14:
        score += 2.8
    elif 18 <= hour <= 21:
        score += 2.3
    elif hour < 10 or hour > 22:
        score -= 1.7

    # In vacante sunt mai putini studenti in complex.
    if is_holiday_period:
        score -= 2.4

    # In sesiune, traficul poate creste dupa anumite intervale de examen si
    # poate scadea in restul zilei.
    if is_exam_period:
        if hour in [13, 14, 19, 20]:
            score += 0.6
        else:
            score -= 0.5

    # Evenimentele din campus pot modifica puternic traficul obisnuit.
    score += campus_event_level * 1.1

    return score


def generate_calendar_context(day_of_week, hour):
    """Genereaza contextul academic si nivelul unui posibil eveniment."""

    context = str(
        rng.choice(
            ["NORMAL", "EXAM", "HOLIDAY"],
            p=[0.72, 0.18, 0.10],
        )
    )

    is_exam_period = int(context == "EXAM")
    is_holiday_period = int(context == "HOLIDAY")

    # Evenimentele importante sunt mai probabile miercuri sau joi seara.
    if day_of_week in [2, 3] and 18 <= hour <= 22:
        campus_event_level = int(
            rng.choice(
                [0, 1, 2, 3],
                p=[0.35, 0.35, 0.22, 0.08],
            )
        )
    else:
        campus_event_level = int(
            rng.choice(
                [0, 1, 2, 3],
                p=[0.72, 0.22, 0.05, 0.01],
            )
        )

    return is_exam_period, is_holiday_period, campus_event_level


def label_traffic(traffic_score):
    """Transforma scorul continuu intr-un nivel discret de trafic."""

    if traffic_score < 0.8:
        return "SCAZUT"

    if traffic_score < 4.6:
        return "MEDIU"

    return "RIDICAT"


def calculate_demand_intensity(operational_score):
    """Transforma scorul operational intr-o intensitate intre 0 si 1."""

    intensity = 1.0 / (1.0 + math.exp(-(operational_score - 2.0) / 1.8))

    return float(clamp(intensity, 0.0, 1.0))


# ---------------------------------------------------------------------------
# Recomandarea si prezenta personalului
# ---------------------------------------------------------------------------


def apply_managerial_variation(recommendation, minimum, maximum):
    """
    Adauga o mica variatie manageriala recomandarii calculate.

    Aproximativ 16% dintre valori sunt ajustate cu -1 sau +1, astfel incat
    situatii operationale apropiate sa nu produca mereu exact acelasi rezultat.
    """

    if rng.random() < 0.16:
        recommendation += int(rng.choice([-1, 1]))

    return int(clamp(recommendation, minimum, maximum))


def recommend_staff(
    hour,
    active_orders,
    occupied_tables,
    kitchen_load,
    bar_load,
    orders_last_30_min,
    campus_event_level,
):
    """Estimeaza necesarul de ospatari, personal de bucatarie si bar."""

    is_peak_interval = int(11 <= hour <= 14 or 18 <= hour <= 21)

    waiter_pressure = (
        occupied_tables / 4.0
        + active_orders / 10.0
        + orders_last_30_min / 18.0
        + is_peak_interval * 0.20
        + campus_event_level * 0.15
    )

    kitchen_pressure = (
        kitchen_load / 7.0 + active_orders / 15.0 + is_peak_interval * 0.15
    )

    bar_pressure = bar_load / 7.5 + campus_event_level * 0.20 + is_peak_interval * 0.10

    recommended_waiters = math.ceil(waiter_pressure + rng.normal(0.0, 0.18))
    recommended_kitchen_staff = math.ceil(kitchen_pressure + rng.normal(0.0, 0.18))
    recommended_bar_staff = math.ceil(bar_pressure + rng.normal(0.0, 0.16))

    recommended_waiters = apply_managerial_variation(
        recommended_waiters,
        1,
        6,
    )
    recommended_kitchen_staff = apply_managerial_variation(
        recommended_kitchen_staff,
        1,
        5,
    )
    recommended_bar_staff = apply_managerial_variation(
        recommended_bar_staff,
        1,
        4,
    )

    return (
        recommended_waiters,
        recommended_kitchen_staff,
        recommended_bar_staff,
    )


def generate_planned_staff(recommended_staff):
    """
    Genereaza personalul programat pornind de la necesarul recomandat.

    Programarea poate fi insuficienta, exacta sau usor mai mare decat necesarul.
    """

    difference = int(
        rng.choice(
            [-1, 0, 1],
            p=[0.26, 0.62, 0.12],
        )
    )

    return max(1, recommended_staff + difference)


def generate_active_staff(
    planned_staff,
    is_exam_period,
    is_holiday_period,
    recommended_staff,
):
    """Genereaza personalul prezent dupa absente si interventia managerului."""

    absence_probability = 0.05 + is_exam_period * 0.03 + is_holiday_period * 0.02

    absent_staff = int(rng.binomial(planned_staff, absence_probability))
    active_staff = planned_staff - absent_staff

    # Uneori managerul reuseste sa cheme un angajat suplimentar.
    if active_staff < recommended_staff and rng.random() < 0.15:
        active_staff += 1

    return active_staff, absent_staff


# ---------------------------------------------------------------------------
# Risc de intarziere
# ---------------------------------------------------------------------------


def label_delay_risk(
    orders_per_waiter,
    kitchen_items_per_employee,
    bar_items_per_employee,
    waiter_deficit,
    kitchen_deficit,
    bar_deficit,
    avg_preparation_time,
    order_age_minutes,
    recent_missed_shifts,
):
    """Calculeaza si eticheteaza riscul operational de intarziere."""

    risk_score = (
        orders_per_waiter * 0.45
        + kitchen_items_per_employee * 0.45
        + bar_items_per_employee * 0.35
        + max(waiter_deficit, 0) * 0.90
        + max(kitchen_deficit, 0) * 1.10
        + max(bar_deficit, 0) * 0.70
        + avg_preparation_time / 11.0
        + order_age_minutes / 16.0
        + recent_missed_shifts * 0.35
        + rng.normal(0.0, 0.95)
    )

    if risk_score < 5.0:
        return "SCAZUT"

    if risk_score < 8.0:
        return "MEDIU"

    return "RIDICAT"


# ---------------------------------------------------------------------------
# Generarea unui rand
# ---------------------------------------------------------------------------


def generate_row():
    """Genereaza o singura situatie operationala completa."""

    day_of_week = int(rng.integers(0, 7))
    hour = int(rng.integers(8, 24))

    (
        is_exam_period,
        is_holiday_period,
        campus_event_level,
    ) = generate_calendar_context(day_of_week, hour)

    base_traffic_score = get_base_traffic_score(
        day_of_week,
        hour,
        is_exam_period,
        is_holiday_period,
        campus_event_level,
    )

    # Cererea latenta nu este observata direct de model. Ea introduce variatii
    # pe care regulile calendaristice nu le pot explica perfect.
    latent_demand_score = base_traffic_score + rng.normal(0.0, 1.35)

    # Eticheta si datele operationale primesc zgomot separat. Astfel,
    # distributiile claselor se suprapun intr-un mod mai realist.
    traffic_label_score = latent_demand_score + rng.normal(0.0, 0.55)
    traffic_level = label_traffic(traffic_label_score)

    operational_score = latent_demand_score + rng.normal(0.0, 0.90)
    demand_intensity = calculate_demand_intensity(operational_score)

    active_orders = int(
        clamp(
            round(
                rng.normal(
                    0.5 + 14.0 * demand_intensity,
                    1.8,
                )
            ),
            0,
            20,
        )
    )

    occupied_probability = float(
        clamp(
            0.03 + 0.85 * demand_intensity + rng.normal(0.0, 0.04),
            0.02,
            0.98,
        )
    )
    occupied_tables = int(rng.binomial(TOTAL_TABLES, occupied_probability))

    estimated_occupancy = int(
        clamp(
            round(occupied_tables * 100 / TOTAL_TABLES + rng.normal(0.0, 7.0)),
            0,
            100,
        )
    )

    orders_last_30_min = int(
        clamp(
            round(
                rng.normal(
                    0.5 + 20.0 * demand_intensity,
                    3.0,
                )
            ),
            0,
            30,
        )
    )

    item_count = int(
        clamp(
            round(active_orders * rng.uniform(1.0, 2.2) + rng.normal(0.0, 2.0)),
            0,
            40,
        )
    )

    if item_count == 0:
        kitchen_load = 0
        bar_load = 0
    else:
        kitchen_share = float(
            clamp(
                rng.normal(0.65, 0.08),
                0.45,
                0.85,
            )
        )
        kitchen_load = int(rng.binomial(item_count, kitchen_share))
        bar_load = item_count - kitchen_load

    (
        recommended_waiters,
        recommended_kitchen_staff,
        recommended_bar_staff,
    ) = recommend_staff(
        hour,
        active_orders,
        occupied_tables,
        kitchen_load,
        bar_load,
        orders_last_30_min,
        campus_event_level,
    )

    planned_waiters = generate_planned_staff(recommended_waiters)
    planned_kitchen = generate_planned_staff(recommended_kitchen_staff)
    planned_bar = generate_planned_staff(recommended_bar_staff)

    (
        active_waiters,
        absent_waiters,
    ) = generate_active_staff(
        planned_waiters,
        is_exam_period,
        is_holiday_period,
        recommended_waiters,
    )

    (
        active_kitchen,
        absent_kitchen,
    ) = generate_active_staff(
        planned_kitchen,
        is_exam_period,
        is_holiday_period,
        recommended_kitchen_staff,
    )

    (
        active_bar,
        absent_bar,
    ) = generate_active_staff(
        planned_bar,
        is_exam_period,
        is_holiday_period,
        recommended_bar_staff,
    )

    waiter_deficit = recommended_waiters - active_waiters
    kitchen_deficit = recommended_kitchen_staff - active_kitchen
    bar_deficit = recommended_bar_staff - active_bar

    orders_per_waiter = active_orders / max(active_waiters, 1)
    kitchen_items_per_employee = kitchen_load / max(active_kitchen, 1)
    bar_items_per_employee = bar_load / max(active_bar, 1)
    occupancy_per_waiter = occupied_tables / max(active_waiters, 1)

    total_planned_staff = planned_waiters + planned_kitchen + planned_bar
    total_active_staff = active_waiters + active_kitchen + active_bar
    scheduled_vs_present_ratio = total_active_staff / max(total_planned_staff, 1)

    recent_missed_shifts = int(
        clamp(
            rng.poisson(
                0.15
                + max(
                    0.0,
                    1.0 - scheduled_vs_present_ratio,
                )
                * 2.5
                + is_exam_period * 0.15
            ),
            0,
            5,
        )
    )

    avg_preparation_time = int(
        clamp(
            round(
                7.0
                + kitchen_items_per_employee * 2.2
                + max(kitchen_deficit, 0) * 2.3
                + campus_event_level * 0.5
                + rng.normal(0.0, 2.8)
            ),
            6,
            45,
        )
    )

    positive_total_deficit = (
        max(waiter_deficit, 0) + max(kitchen_deficit, 0) + max(bar_deficit, 0)
    )

    order_age_minutes = int(
        clamp(
            round(
                2.0
                + active_orders * 1.0
                + positive_total_deficit * 2.4
                + rng.normal(0.0, 6.0)
            ),
            0,
            60,
        )
    )

    delay_risk = label_delay_risk(
        orders_per_waiter,
        kitchen_items_per_employee,
        bar_items_per_employee,
        waiter_deficit,
        kitchen_deficit,
        bar_deficit,
        avg_preparation_time,
        order_age_minutes,
        recent_missed_shifts,
    )

    return {
        # Caracteristici operationale folosite de modelele de baza.
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
        # Programarea, prezenta si deficitul de personal.
        "planned_waiters": planned_waiters,
        "planned_kitchen": planned_kitchen,
        "planned_bar": planned_bar,
        "active_waiters": active_waiters,
        "active_kitchen": active_kitchen,
        "active_bar": active_bar,
        "absent_waiters": absent_waiters,
        "absent_kitchen": absent_kitchen,
        "absent_bar": absent_bar,
        "waiter_deficit": waiter_deficit,
        "kitchen_deficit": kitchen_deficit,
        "bar_deficit": bar_deficit,
        # Rapoarte operationale folosite de modelul de intarziere.
        "orders_per_waiter": round(orders_per_waiter, 3),
        "kitchen_items_per_employee": round(
            kitchen_items_per_employee,
            3,
        ),
        "bar_items_per_employee": round(
            bar_items_per_employee,
            3,
        ),
        "occupancy_per_waiter": round(
            occupancy_per_waiter,
            3,
        ),
        "scheduled_vs_present_ratio": round(
            scheduled_vs_present_ratio,
            3,
        ),
        # Context academic si operational suplimentar.
        "recent_missed_shifts": recent_missed_shifts,
        "is_exam_period": is_exam_period,
        "is_holiday_period": is_holiday_period,
        "campus_event_level": campus_event_level,
        # Etichete si rezultate asteptate.
        "traffic_level": traffic_level,
        "recommended_waiters": recommended_waiters,
        "recommended_kitchen_staff": recommended_kitchen_staff,
        "recommended_bar_staff": recommended_bar_staff,
        "delay_risk": delay_risk,
    }


# ---------------------------------------------------------------------------
# Afisare si salvare
# ---------------------------------------------------------------------------


def print_dataset_summary(dataframe):
    """Afiseaza distributiile si cateva statistici de control."""

    print(f"Dataset generat cu succes: {OUTPUT_FILE}")
    print(f"Numar randuri: {len(dataframe)}")
    print()

    print("Distributie trafic:")
    print(dataframe["traffic_level"].value_counts())
    print()

    print("Distributie risc intarziere:")
    print(dataframe["delay_risk"].value_counts())
    print()

    print("Medie personal recomandat:")
    print(
        dataframe[
            [
                "recommended_waiters",
                "recommended_kitchen_staff",
                "recommended_bar_staff",
            ]
        ]
        .mean()
        .round(3)
    )
    print()

    print("Medie personal activ:")
    print(
        dataframe[
            [
                "active_waiters",
                "active_kitchen",
                "active_bar",
            ]
        ]
        .mean()
        .round(3)
    )
    print()

    rows_with_staff_deficit = (
        dataframe[
            [
                "waiter_deficit",
                "kitchen_deficit",
                "bar_deficit",
            ]
        ]
        .gt(0)
        .any(axis=1)
        .sum()
    )

    print(f"Randuri cu cel putin un deficit de personal: {rows_with_staff_deficit}")
    print(
        "Procent randuri cu deficit: "
        f"{rows_with_staff_deficit / len(dataframe) * 100:.2f}%"
    )


def main():
    """Genereaza datasetul complet si il salveaza in format CSV."""

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    rows = [generate_row() for _ in range(ROW_COUNT)]
    dataframe = pd.DataFrame(rows)
    dataframe.to_csv(OUTPUT_FILE, index=False)

    print_dataset_summary(dataframe)


if __name__ == "__main__":
    main()
