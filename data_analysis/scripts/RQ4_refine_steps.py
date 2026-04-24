import csv
from collections import defaultdict
from typing import Dict, List, Optional, Tuple
import math

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt


# ---------- parsing & metrics helpers ----------

def to_float(x: str) -> Optional[float]:
    try:
        return float(x.strip())
    except Exception:
        return None

def safe_div(num: Optional[float], den: Optional[float]) -> Optional[float]:
    if num is None or den is None or den == 0:
        return None
    return num / den

def compute_metrics(parts: List[str]) -> Tuple[str, Optional[float], Optional[float], Optional[float]]:
    """
    Field layout:
      project_name = parts[1]
      PDR = parts[3] / parts[2]
      RC  = parts[5] / parts[4]
      RF  = parts[6] / parts[4]
    """
    project_name = parts[1] if len(parts) > 1 else "UNKNOWN"

    n2 = to_float(parts[2]) if len(parts) > 2 else None
    n3 = to_float(parts[3]) if len(parts) > 3 else None
    n4 = to_float(parts[4]) if len(parts) > 4 else None
    n5 = to_float(parts[5]) if len(parts) > 5 else None
    n6 = to_float(parts[6]) if len(parts) > 6 else None

    pdr = safe_div(n3, n2)
    rc  = safe_div(n5, n4)
    rf  = safe_div(n6, n4)

    return project_name, pdr, rc, rf


# ---------- core logic ----------

def read_and_collect(file_path: str) -> Dict[str, List[Tuple[Optional[float], Optional[float], Optional[float]]]]:
    """
    Returns: { project_name: [(PDR, RC, RF), ...] } in the order lines appear per project.
    """
    per_project: Dict[str, List[Tuple[Optional[float], Optional[float], Optional[float]]]] = defaultdict(list)

    with open(file_path, "r", encoding="utf-8", newline="") as f:
        reader = csv.reader(f)
        for row in reader:
            if not row or all(cell.strip() == "" for cell in row):
                continue
            project_name, pdr, rc, rf = compute_metrics(row)
            per_project[project_name].append((pdr, rc, rf))

    return per_project

def mean_ignore_none(values: List[Optional[float]]) -> Optional[float]:
    nums = [v for v in values if v is not None and not (isinstance(v, float) and math.isnan(v))]
    return sum(nums) / len(nums) if nums else None

def per_step_averages(per_project: Dict[str, List[Tuple[Optional[float], Optional[float], Optional[float]]]]):
    """
    Align by step index and compute averages at each step across projects.
    If a project finishes early, reuse its last (PDR, RC, RF) for later steps.
    Returns: pandas DataFrame with columns: step, avg_PDR, avg_RC, avg_RF, n_projects
    """
    if not per_project:
        return pd.DataFrame(columns=["step", "avg_PDR", "avg_RC", "avg_RF", "n_projects"])

    max_steps = max(len(steps) for steps in per_project.values())
    rows = []

    for k in range(1, max_steps + 1):
        pdr_vals, rc_vals, rf_vals = [], [], []

        for steps in per_project.values():
            if len(steps) >= k:
                pdr, rc, rf = steps[k - 1]
            else:
                # project finished early → reuse last values
                pdr, rc, rf = steps[-1]

            pdr_vals.append(pdr)
            rc_vals.append(rc)
            rf_vals.append(rf)

        rows.append({
            "step": k,
            "avg_PDR": mean_ignore_none(pdr_vals),
            "avg_RC":  mean_ignore_none(rc_vals),
            "avg_RF":  mean_ignore_none(rf_vals),
            "n_projects": len(per_project),
        })

    df = pd.DataFrame(rows)
    # Convert None to NaN for plotting
    return df.replace({None: np.nan})


# ---------- plotting ----------

def plot_averages(df: pd.DataFrame, title: str = "Evolution of Metrics Across Refinement Steps"):
    if df.empty:
        print("No data to plot.")
        return
    plt.figure(figsize=(5, 3))
    plt.plot(df["step"], df["avg_PDR"], marker="o", label="PDR")
    plt.plot(df["step"], df["avg_RC"],  marker="s", label="RC")
    plt.plot(df["step"], df["avg_RF"],  marker="^", label="RF")
    plt.xlabel("Step")
    plt.ylabel("Average")
    plt.title(title)
    plt.legend()
    plt.grid(True, linestyle="--", alpha=0.6)
    plt.tight_layout()
    plt.show()


# ---------- script entry ----------

if __name__ == "__main__":
    file_path = r"C:\\Users\\admin\\Downloads\\data_analysis\\EventBAgent.txt"

    # 1) read & group
    per_project = read_and_collect(file_path)

    # 2) compute per-step averages (with last-value carry-forward)
    df = per_step_averages(per_project)
    print(df)

    # 3) optional: save a CSV for later use
    # df.to_csv("per_step_averages.csv", index=False)

    # 4) plot
    plot_averages(df)
