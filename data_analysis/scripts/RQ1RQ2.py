import os
import pandas as pd

# Directory containing your txt files
input_dir = '.'  # Change if needed

# Methods/files (explicit order!)
# txt_files = [
#     ("LLM Synthesis + SMT proof fixing", "SMT.txt"),
#     ("Cursor Agent", "cursor.txt"),
#     ("PAT Agent", "pat.txt"),
#     ("Event-B Agent", "EventBAgent.txt"),
# ]

txt_files = [
    ("w.o. refine, w.o. repair strategy", "ablation_norefine_noproofstrategy.txt"),
    ("w. refine, w.o. repair strategy", "ablation_refine_noproofstrategy.txt"),
    ("w.o. refine, w. repair strategy", "ablation_norefine_proofstrategy.txt"),
    ("w. refine, w. repair strategy (ours)", "ablation_refine_proofstrategy.txt"),
]

# txt_files = [
#     # ("w.o. refine, w.o. repair strategy", "ablation_norefine_noproofstrategy.txt"),
#     ("w. refine, w.o. repair strategy", "ablation_refine_noproofstrategy.txt"),
#     # ("w.o. refine, w. repair strategy", "ablation_norefine_proofstrategy.txt"),
#     ("w. refine, w. repair strategy (ours)", "ablation_refine_proofstrategy.txt"),
# ]

# Buckets by project name
low = {'ch4_simple_file_transfer_protocol', 'ch11_tree_shaped_network', 'ClockProject',
       '1_division0', '1_square0', 'Bakery', 'BridgeModels',
       'ch2_car_on_bridge', 'ch6_bounded_retransmission_protocol'}

mid = {'LivenessModels', '1_square1', 'ch3_mechanical_press_controller', '1_division1',
       '2_search_array', '2_search_matrix', '3_maxi1', '4_revarray', '90_gcd'}

high = {'3_maxi2', '3_mini1', '3_mini2', '5_partitioning', '8_sorting', '7_Inversing',
        'ch16_location_access_controller', '9_pointer', '6_binsearch'}



GROUPS = {'Low': low, 'Mid': mid, 'High': high}

METRIC_COLS = ['PDR', 'RC', 'RF']
REFINE_COL = 'RefinePDR'

def read_latest_per_project(path):
    """Read the txt file and keep the latest line per project (by appearance order)."""
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.read().strip().split('\n')
    data = [line.split(',') for line in lines if line.strip()]

    project_latest = {}
    for row in data:
        # Expect at least the first 7 columns for base metrics; 9 columns if refine counts present
        if len(row) < 7:
            continue
        project = row[1].strip()
        project_latest[project] = [col.strip() for col in row]  # last one wins
    return project_latest

def compute_metrics_per_project(project_latest):
    """Return dict: project -> (po_rate, coverage, fulfillment)."""
    per_project = {}
    for project, row in project_latest.items():
        try:
            total_pos = int(row[2])
            discharged_pos = int(row[3])
            total_requirements = int(row[4])
            covered_requirements = int(row[5])
            fulfilled_requirements = int(row[6])
        except (ValueError, IndexError):
            continue

        po_rate = discharged_pos / total_pos if total_pos else 0.0
        coverage = covered_requirements / total_requirements if total_requirements else 1.0
        fulfillment = fulfilled_requirements / total_requirements if total_requirements else 1.0
        per_project[project] = (po_rate, coverage, fulfillment)
    return per_project

def compute_refine_pdr_per_project(project_latest):
    """
    Return dict: project -> refine_pdr (float or None).
    Uses row[7] = total refine POs, row[8] = discharged refine POs when len(row) >= 9.
    """
    per_project = {}
    for project, row in project_latest.items():
        refine_pdr = None
        if len(row) >= 9:
            try:
                total_refine = int(row[7])
                discharged_refine = int(row[8])
                refine_pdr = (discharged_refine / total_refine) if total_refine else 1.0
            except ValueError:
                refine_pdr = None
        per_project[project] = refine_pdr
    return per_project

def avg_tuple(values):
    """Average a list of 3-tuples elementwise. Return (None,None,None) if empty."""
    if not values:
        return (None, None, None)
    n = len(values)
    s0 = sum(v[0] for v in values)
    s1 = sum(v[1] for v in values)
    s2 = sum(v[2] for v in values)
    return (s0 / n, s1 / n, s2 / n)

def avg_list(values):
    """Average a list of floats ignoring None. Return None if no valid values."""
    vals = [v for v in values if v is not None]
    if not vals:
        return None
    return sum(vals) / len(vals)

def bucket_avgs_for_method(per_project, group_names):
    """Average (po, cov, ful) over projects whose name is in group_names."""
    vals = [per_project[p] for p in per_project if p in group_names]
    return avg_tuple(vals)

def bucket_avg_refine_for_method(per_project_refine, group_names):
    """Average refine PDR over projects in group_names, ignoring None."""
    vals = [per_project_refine[p] for p in per_project_refine if p in group_names]
    return avg_list(vals)

def overall_avgs_for_method(per_project):
    """Average (po, cov, ful) over all projects for the method."""
    vals = list(per_project.values())
    return avg_tuple(vals)

def overall_avg_refine_for_method(per_project_refine):
    """Average refine PDR over all projects for the method, ignoring None."""
    vals = list(per_project_refine.values())
    return avg_list(vals)

def main():
    # method_name -> per_project metrics dicts
    method_metrics = {}
    method_refine = {}

    for method, file_name in txt_files:
        path = os.path.join(input_dir, file_name)
        project_latest = read_latest_per_project(path)
        per_project = compute_metrics_per_project(project_latest)
        per_project_refine = compute_refine_pdr_per_project(project_latest)
        method_metrics[method] = per_project
        method_refine[method] = per_project_refine

    # ---- Main table (PDR/RC/RF) ----
    rows = []
    dataset_order = ['Low', 'Mid', 'High', 'Overall']

    for dataset in dataset_order:
        for method, _ in txt_files:  # preserves desired order
            if dataset == 'Overall':
                pdr, rc, rf = overall_avgs_for_method(method_metrics[method])
            else:
                pdr, rc, rf = bucket_avgs_for_method(method_metrics[method], GROUPS[dataset])

            rows.append({
                'Dataset': dataset,
                'Method': method,
                'PDR': pdr,
                'RC': rc,
                'RF': rf,
            })

    df = pd.DataFrame(rows, columns=['Dataset', 'Method'] + METRIC_COLS)

    # Optional: round numeric metrics to 4 decimals
    for c in METRIC_COLS:
        df[c] = df[c].round(4)

    df.to_excel('RQ2_new.xlsx', index=False)

    # ---- Refinement-only table (RefinePDR) ----
    # refine_rows = []
    # for dataset in dataset_order:
    #     for method, _ in txt_files:
    #         if dataset == 'Overall':
    #             r_pdr = overall_avg_refine_for_method(method_refine[method])
    #         else:
    #             r_pdr = bucket_avg_refine_for_method(method_refine[method], GROUPS[dataset])

    #         refine_rows.append({
    #             'Dataset': dataset,
    #             'Method': method,
    #             REFINE_COL: None if r_pdr is None else round(r_pdr, 4),
    #         })

    # df_ref = pd.DataFrame(refine_rows, columns=['Dataset', 'Method', REFINE_COL])
    # df_ref.to_excel('RQ2_refine.xlsx', index=False)

    # print("Results saved to RQ1.xlsx and RQ1_refine.xlsx")

if __name__ == "__main__":
    main()
