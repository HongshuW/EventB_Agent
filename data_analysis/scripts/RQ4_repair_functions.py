import os
import pandas as pd
from collections import Counter

# -----------------------
# Parsing helpers
# -----------------------

def _parse_fix_proof_message(raw_field):
    """
    Return:
      kind: {"function", "po_discharged", "max_attempts", "noise"}
      name: normalized function name if kind == "function"
      extra: optional payload (for "others")
    """
    parts = raw_field.split(":")
    head = parts[0].strip()
    tail = ":".join(parts[1:]).strip() if len(parts) > 1 else ""

    # Terminal outcomes
    if head == "PO discharged,":
        return "po_discharged", None, None
    if head.startswith("FIX_PROOF"):
        return "max_attempts", None, None

    # Ignore obvious noise
    # noise_starts = ("java.", "llm returns", "All ", "FIX_PROOF", "ERROR", "WARN", "")
    # if any(head.startswith(ns) for ns in noise_starts):
    #     return "noise", None, None

    # applyProofTactic → extract tactic name if present
    if head == "applyProofTactic":
        tactic = None
        marker = '"proof_tactic"'
        if marker in tail:
            after = tail.split(marker, 1)[1]
            after = after.split(":", 1)[-1].strip()
            if after.startswith('"'):
                after = after[1:]
            tactic = after.split('"', 1)[0].strip()
        else:
            if ":" in tail:
                tactic = tail.split(":")[-1].strip().strip('{}[]"\'')
        name = f"applyProofTactic:{tactic}" if tactic else "applyProofTactic"
        return "function", name, None

    # "others" – keep payload
    if head == "others":
        return "function", "others", tail

    # Everything else is treated as an actionable function label
    # (e.g., ok_not_all_nodes_considered, PO not found, strengthenGuard, etc.)
    return "function", head, tail


def _is_fix_proof_line(line):
    return line.startswith("FIX_PROOF,")


def _split_csv_loose(line):
    parts = line.split(",")
    if len(parts) < 6:
        return None
    head = parts[:5]
    tail = ",".join(parts[5:])
    return head + [tail]


# -----------------------
# Core analysis per file
# -----------------------

def analyze_file(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        lines = f.read().strip().splitlines()

    # Functions/tactics that *occurred in sessions which ended with PO discharged*
    counted_functions = []
    # "others" payloads that occurred in successful sessions
    counted_others_payloads = []

    session_active = False
    session_actions = []  # list of (name, payload) for this session

    for raw in lines[:-1]:  # exclude final 'total time' line if present
        if not _is_fix_proof_line(raw):
            continue

        fields = _split_csv_loose(raw)
        if not fields:
            continue

        msg_field = fields[5].strip()

        # Start new session if none active
        if not session_active:
            session_active = True
            session_actions = []

        kind, name, extra = _parse_fix_proof_message(msg_field)

        print(kind)

        if kind == "function":
            session_actions.append((name, extra))

        elif kind == "po_discharged":
            # Count ALL actionable functions in this successful session
            for name_i, extra_i in session_actions:
                counted_functions.append(name_i)
                if name_i == "others" and extra_i:
                    counted_others_payloads.append(extra_i)
            # End session
            session_active = False
            session_actions = []

        elif kind == "max_attempts":
            # Failed session – discard collected actions
            session_active = False
            session_actions = []

        else:
            # noise – ignore
            pass

    message_counter = Counter(counted_functions)
    return {
        "Model Name": os.path.splitext(os.path.basename(file_path))[0],
        "FIX_PO_Message_Distribution": dict(message_counter),
        "Other_Fixes": counted_others_payloads,
    }


# -----------------------
# Batch utilities
# -----------------------

def get_all_txt_files(input_dir):
    txt_files = []
    for fname in os.listdir(input_dir):
        file_path = os.path.join(input_dir, fname)
        if os.path.isfile(file_path) and fname.endswith(".txt"):
            txt_files.append(file_path)
    return txt_files


def batch_analyze(input_dir):
    results = []
    txt_files = get_all_txt_files(input_dir)
    for file_path in txt_files:
        try:
            results.append(analyze_file(file_path))
        except Exception as e:
            print(f"⚠️ An error occurred while processing the file {file_path}: {e}")

    if results:
        # Expand the (successful) message distribution into columns
        all_messages = set()
        for r in results:
            all_messages.update(r["FIX_PO_Message_Distribution"].keys())
        all_messages = sorted(all_messages)

        rows = []
        for r in results:
            row = {"Model Name": r["Model Name"]}
            for msg in all_messages:
                row[msg] = r["FIX_PO_Message_Distribution"].get(msg, 0)
            rows.append(row)

        df_result = pd.DataFrame(rows)

        # "Other_Fixes" only from successful sessions
        other_fixes_rows = []
        for r in results:
            for fix in r["Other_Fixes"]:
                other_fixes_rows.append({
                    "Model Name": r["Model Name"],
                    "Other_Fix": fix
                })
        df_other_fixes = pd.DataFrame(other_fixes_rows)

        output_path = os.path.join("RQ3_individual.xlsx")
        with pd.ExcelWriter(output_path) as writer:
            df_result.to_excel(writer, sheet_name="Distribution", index=False)
            df_other_fixes.to_excel(writer, sheet_name="Other_Fixes", index=False)
        print(f"✅ Analysis completed, results saved to {output_path}")
    else:
        print("❌ No log files meeting the criteria were found!")


def batch_analyze_sum(input_dir):
    """
    Sums successful functions/tactics across all models.
    Counts *every* actionable function within sessions that end with 'PO discharged'.
    """
    total_counter = Counter()
    txt_files = get_all_txt_files(input_dir)
    for file_path in txt_files:
        try:
            result = analyze_file(file_path)
            total_counter.update(result["FIX_PO_Message_Distribution"])
        except Exception as e:
            print(f"⚠️ An error occurred while processing the file {file_path}: {e}")

    if total_counter:
        df_sum = pd.DataFrame(
            [{"Proof Tactic": k, "Count": v} for k, v in total_counter.items()]
        ).sort_values("Count", ascending=False)
        output_path = os.path.join("RQ3_new.xlsx")
        df_sum.to_excel(output_path, index=False)
        print(f"✅ Summed distribution saved to {output_path}")
    else:
        print("❌ No successful FIX_PROOF functions found (no sessions ending with 'PO discharged').")


if __name__ == "__main__":
    # Modify the path here to the directory where the txt log files are stored
    input_dir = "./ablation_refine_proofstrategy_5_5"
    batch_analyze(input_dir)
    batch_analyze_sum(input_dir)
