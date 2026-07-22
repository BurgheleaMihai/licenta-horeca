"""
Ruleaza in ordine toate etapele necesare antrenarii modelelor AI HoReCa.

Fluxul executat este:
1. generarea datasetului sintetic;
2. antrenarea modelului de trafic;
3. antrenarea modelului de personal;
4. antrenarea modelului de intarziere;
5. afisarea duratelor si a metricilor disponibile in fisierele metadata.
"""

from __future__ import annotations

import json
import subprocess
import sys
import time
from pathlib import Path
from typing import Any, Final, Mapping, Sequence, TypeAlias


JsonObject: TypeAlias = dict[str, Any]
MetricKeys: TypeAlias = Sequence[str]
MetricDefinition: TypeAlias = tuple[str, MetricKeys]
MetricsMapping: TypeAlias = Mapping[str, Any]


BASE_DIR: Final[Path] = Path(__file__).resolve().parent
MODELS_DIR: Final[Path] = BASE_DIR / "models"

TRAFFIC_METADATA_FILE: Final[Path] = MODELS_DIR / "traffic_model_metadata.json"
STAFF_METADATA_FILE: Final[Path] = MODELS_DIR / "staff_model_metadata.json"
DELAY_METADATA_FILE: Final[Path] = MODELS_DIR / "delay_model_metadata.json"


def find_dataset_generator() -> Path:
    """Returneaza generatorul de dataset disponibil in proiect."""

    candidates = (
        BASE_DIR / "generate_dataset.py",
        BASE_DIR / "generate_synthetic_dataset.py",
    )

    for candidate in candidates:
        if candidate.exists():
            return candidate

    raise FileNotFoundError(
        "Nu a fost gasit nici generate_dataset.py, nici generate_synthetic_dataset.py."
    )


def run_script(script_file: Path) -> float:
    """Ruleaza un script Python si returneaza durata executiei."""

    if not script_file.exists():
        raise FileNotFoundError(f"Fisierul nu exista: {script_file}")

    print()
    print("=" * 72)
    print(f"Ruleaza: {script_file.name}")
    print("=" * 72)

    started_at = time.perf_counter()

    process = subprocess.run(
        [
            sys.executable,
            str(script_file),
        ],
        cwd=BASE_DIR,
        check=False,
    )

    duration_seconds = time.perf_counter() - started_at

    if process.returncode != 0:
        raise RuntimeError(
            f"{script_file.name} s-a oprit cu exit code {process.returncode}."
        )

    print(f"{script_file.name} finalizat in {duration_seconds:.2f} secunde.")

    return duration_seconds


def read_metadata(metadata_file: Path) -> JsonObject | None:
    """Citeste si valideaza un fisier metadata JSON."""

    if not metadata_file.exists():
        return None

    try:
        parsed_content = json.loads(metadata_file.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None

    if not isinstance(parsed_content, dict):
        return None

    return parsed_content


def get_first_value(
    dictionary: Mapping[str, Any] | None,
    keys: Sequence[str],
) -> Any | None:
    """Returneaza prima valoare existenta dintre cheile primite."""

    if dictionary is None:
        return None

    for key in keys:
        if key in dictionary:
            return dictionary[key]

    return None


def get_model_metrics(
    metadata: Mapping[str, Any],
) -> MetricsMapping | None:
    """
    Extrage metricile finale din structurile metadata folosite in proiect.

    Traficul si intarzierea salveaza metricile direct in testMetrics,
    finalMetrics sau metrics. Modelul de personal le salveaza in
    testEvaluation -> evaluationMetrics.
    """

    direct_metrics = get_first_value(
        metadata,
        (
            "testMetrics",
            "finalMetrics",
            "metrics",
        ),
    )

    if isinstance(direct_metrics, dict):
        return direct_metrics

    test_evaluation = metadata.get("testEvaluation")

    if not isinstance(test_evaluation, dict):
        return None

    evaluation_metrics = test_evaluation.get("evaluationMetrics")

    if isinstance(evaluation_metrics, dict):
        return evaluation_metrics

    return None


def format_number(value: Any) -> str:
    """Formateaza o valoare numerica folosind patru zecimale."""

    if isinstance(value, bool):
        return "indisponibil"

    if isinstance(value, (int, float)):
        return f"{float(value):.4f}"

    return "indisponibil"


def print_model_summary(
    title: str,
    metadata_file: Path,
    metric_definitions: Sequence[MetricDefinition],
) -> None:
    """Afiseaza modelul selectat si metricile finale disponibile."""

    metadata = read_metadata(metadata_file)

    print()
    print(title)

    if metadata is None:
        print(f"  Metadata indisponibila: {metadata_file}")
        return

    model_name = get_first_value(
        metadata,
        (
            "modelName",
            "selectedModel",
            "algorithm",
        ),
    )

    metrics = get_model_metrics(metadata)

    print(f"  Model selectat: {model_name or 'indisponibil'}")

    if metrics is None:
        print("  Metricile finale nu au fost gasite in metadata.")
        return

    for label, possible_keys in metric_definitions:
        value = get_first_value(
            metrics,
            possible_keys,
        )

        print(f"  {label}: {format_number(value)}")


def print_final_summary(
    durations: Mapping[str, float],
) -> None:
    """Afiseaza duratele si metricile celor trei modele."""

    total_duration = sum(durations.values())

    print()
    print("=" * 72)
    print("REZUMAT FINAL")
    print("=" * 72)

    for script_name, duration in durations.items():
        print(f"{script_name}: {duration:.2f} secunde")

    print(f"Durata totala: {total_duration:.2f} secunde")

    print_model_summary(
        "MODEL TRAFIC",
        TRAFFIC_METADATA_FILE,
        (
            (
                "Accuracy",
                (
                    "accuracy",
                    "testAccuracy",
                ),
            ),
            (
                "Balanced accuracy",
                (
                    "balancedAccuracy",
                    "balanced_accuracy",
                ),
            ),
            (
                "Macro F1",
                (
                    "macroF1",
                    "macro_f1",
                ),
            ),
            (
                "MCC",
                (
                    "mcc",
                    "matthewsCorrelationCoefficient",
                ),
            ),
            (
                "ROC-AUC macro",
                (
                    "rocAucMacroOvr",
                    "rocAucMacro",
                    "roc_auc_macro",
                ),
            ),
        ),
    )

    print_model_summary(
        "MODEL PERSONAL",
        STAFF_METADATA_FILE,
        (
            (
                "MAE",
                (
                    "overallMae",
                    "overall_mae",
                    "mae",
                ),
            ),
            (
                "RMSE",
                (
                    "overallRmse",
                    "overall_rmse",
                    "rmse",
                ),
            ),
            (
                "R2",
                (
                    "overallR2",
                    "overall_r2",
                    "r2",
                ),
            ),
            (
                "Precizie in limita +/-1",
                (
                    "roundedWithinOneCellRate",
                    "rounded_within_one_cell_rate",
                ),
            ),
        ),
    )

    print_model_summary(
        "MODEL INTARZIERE",
        DELAY_METADATA_FILE,
        (
            (
                "Accuracy",
                ("accuracy",),
            ),
            (
                "Macro F1",
                (
                    "macroF1",
                    "macro_f1",
                ),
            ),
            (
                "Weighted F1",
                (
                    "weightedF1",
                    "weighted_f1",
                ),
            ),
            (
                "Balanced accuracy",
                (
                    "balancedAccuracy",
                    "balanced_accuracy",
                ),
            ),
            (
                "MCC",
                (
                    "mcc",
                    "matthewsCorrelationCoefficient",
                ),
            ),
        ),
    )

    print()
    print("Toate scripturile au fost executate cu succes.")


def train_all_models() -> None:
    """Ruleaza generatorul si cele trei scripturi de antrenare."""

    dataset_generator = find_dataset_generator()

    scripts = (
        dataset_generator,
        BASE_DIR / "train_traffic_model.py",
        BASE_DIR / "train_staff_model.py",
        BASE_DIR / "train_delay_model.py",
    )

    durations: dict[str, float] = {}

    for script_file in scripts:
        durations[script_file.name] = run_script(script_file)

    print_final_summary(durations)


def main() -> None:
    """Punctul de intrare al scriptului."""

    try:
        train_all_models()
    except KeyboardInterrupt:
        print()
        print("Executia a fost oprita de utilizator.")
        raise SystemExit(130)
    except Exception as caught_error:
        print()
        print("Antrenarea tuturor modelelor a esuat.")
        print(f"Motiv: {caught_error}")
        raise SystemExit(1) from caught_error


if __name__ == "__main__":
    main()
