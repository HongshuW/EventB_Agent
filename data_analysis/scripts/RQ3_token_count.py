import argparse
from pathlib import Path
import sys
import re, sys

path = "C:\\Users\\admin\\Downloads\\data_analysis\\ablation_refine_proofstrategy_5_5"

low = {'ch4_simple_file_transfer_protocol', 'ch11_tree_shaped_network', 'ClockProject',
       '1_division0', '1_square0', 'Bakery', 'BridgeModels',
       'ch2_car_on_bridge', 'ch6_bounded_retransmission_protocol'}

mid = {'LivenessModels', '1_square1', 'ch3_mechanical_press_controller', '1_division1',
       '2_search_array', '2_search_matrix', '3_maxi1', '4_revarray', '90_gcd'}

high = {'3_maxi2', '3_mini1', '3_mini2', '5_partitioning', '8_sorting', '7_Inversing',
        'ch16_location_access_controller', '9_pointer', '6_binsearch'}

def read_last_line(path, encoding='utf-8'):
    path = Path(path)
    try:
        with path.open('rb') as f:
            f.seek(0, 2)
            size = f.tell()
            if size == 0:
                return ''
            # start from last byte
            pos = size - 1
            # skip trailing newline characters
            while pos >= 0:
                f.seek(pos)
                b = f.read(1)
                if b not in (b'\n', b'\r'):
                    break
                pos -= 1
            # find preceding newline (start of last line)
            while pos >= 0:
                f.seek(pos)
                if f.read(1) in (b'\n', b'\r'):
                    pos += 1
                    break
                pos -= 1
            if pos < 0:
                pos = 0
            f.seek(pos)
            data = f.read()
        return data.decode(encoding, errors='replace').rstrip('\r\n')
    except Exception as e:
        return f"<error reading file: {e}>"

def read_all_lines(path, index=None, sep=',', filter0=None, encoding='utf-8'):
    """Read all lines from `path`.

    If `index` is provided, return the `index`-th column (0-based) from each
    (optionally filtered) line split by `sep`. If `filter0` is provided,
    it may be a single string or an iterable of strings; only lines whose
    column 0 equals one of the `filter0` values are considered. Missing
    columns are returned as None. If `index` is None, return the full lines
    (after filtering, if applied).
    """
    path = Path(path)
    try:
        with path.open('r', encoding=encoding, errors='replace') as f:
            raw_lines = [line.rstrip('\r\n') for line in f]

        # apply filter on column 0 if requested. Accept a single string or
        # an iterable/list of strings for backwards compatibility.
        if filter0 is None:
            filtered = raw_lines
        else:
            # normalize filter0 to a set of allowed values
            if isinstance(filter0, str):
                allowed = {filter0}
            else:
                try:
                    allowed = set(filter0)
                except Exception:
                    allowed = {str(filter0)}
            filtered = []
            for ln in raw_lines:
                parts0 = ln.split(sep)
                if parts0 and parts0[0] in allowed:
                    filtered.append(ln)

        if index is None:
            return filtered

        cols = []
        for ln in filtered:
            parts = ln.split(sep)
            try:
                if index < 0:
                    cols.append(parts[index] if -len(parts) <= index else None)
                else:
                    cols.append(parts[index] if index < len(parts) else None)
            except Exception:
                cols.append(None)
        return cols
    except Exception as e:
        return [f"<error reading file: {e}>"]

def main():
    folder = Path(path)
    if not folder.is_dir():
        print(f"Not a directory: {folder}", file=sys.stderr)
        sys.exit(1)

    parts = {'low': low, 'mid': mid, 'high': high}
    tasks = [["REFINE"], ["SYNTHESIS", "FIX_COMPILATION"], ["FIX_MODEL_CHECKING", "FIX_PROOF"]]
    # task_values[task_idx][partition] = list of sum_token values
    task_values = [{k: [] for k in parts} for _ in tasks]
    for d in task_values:
        d['other'] = []

    for file in sorted(folder.glob("*")):
        if not file.is_file():
            continue
        
        task_token_data = []

        for task_tags in tasks:
            # read the 5th CSV column (index 4) from each line if available
            # filter0 accepts a list of allowed values for column 0
            token_data = read_all_lines(file, index=4, filter0=task_tags)
            if not token_data:
                print(f"No lines in {file}", file=sys.stderr)
                continue
            # compute sum of numeric entries in token_data (skip None/non-numeric)
            sum_token = 0.0
            count_token = 0
            for entry in token_data:
                if entry is None:
                    continue
                s = entry.strip()
                if s == '':
                    continue
                try:
                    v = float(s)
                except Exception:
                    m = re.search(r'[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?', s)
                    if m:
                        try:
                            v = float(m.group(0))
                        except Exception:
                            continue
                    else:
                        continue
                sum_token += v
                count_token += 1
            
            task_token_data.append((task_tags, sum_token, count_token))

        # determine partition by file stem
        stem = file.stem
        part = next((k for k, s in parts.items() if stem in s), 'other')
        # store sum_token for this file when numeric entries were found
        for entry in task_token_data:
            task_tags, sum_token, count_token = entry
            # Find which task index this entry corresponds to
            task_idx = next((i for i, t in enumerate(tasks) if t == task_tags), None)
            if task_idx is not None and count_token:
                task_values[task_idx][part].append(sum_token)

    # print task-specific averages
    for task_idx, task_tags in enumerate(tasks):
        print(f"\n--- Task {task_idx} ({task_tags}): ---")
        for part in ('low', 'mid', 'high', 'other'):
            svals = task_values[task_idx][part]
            if svals:
                avg_sum = sum(svals) / len(svals)
                print(f"{part}: {len(svals)} files, average sum_token = {avg_sum}")
            else:
                print(f"{part}: no sum_token entries", file=sys.stderr)
        # overall average for this task
        all_task_vals = [v for lst in task_values[task_idx].values() for v in lst]
        if all_task_vals:
            overall_task = sum(all_task_vals) / len(all_task_vals)
            print(f"Overall sum_token average ({len(all_task_vals)} files): {overall_task}")

if __name__ == "__main__":
    main()
