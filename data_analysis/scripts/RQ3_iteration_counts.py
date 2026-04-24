from pathlib import Path
import sys

path = "C:\\Users\\admin\\Downloads\\data_analysis\\ablation_refine_proofstrategy_5_5"

low = {'ch4_simple_file_transfer_protocol', 'ch11_tree_shaped_network', 'ClockProject',
       '1_division0', '1_square0', 'Bakery', 'BridgeModels',
       'ch2_car_on_bridge', 'ch6_bounded_retransmission_protocol'}

mid = {'LivenessModels', '1_square1', 'ch3_mechanical_press_controller', '1_division1',
       '2_search_array', '2_search_matrix', '3_maxi1', '4_revarray', '90_gcd'}

high = {'3_maxi2', '3_mini1', '3_mini2', '5_partitioning', '8_sorting', '7_Inversing',
        'ch16_location_access_controller', '9_pointer', '6_binsearch'}


def read_and_split(path):
    """Read file and split each line by comma."""
    with open(path, 'r') as f:
        lines = f.readlines()
    return [line.strip().split(',') for line in lines if line.strip()]


def count_consecutive(lines):
    """Count consecutive occurrences of the first element in each line."""
    if not lines:
        return []
    
    result = []
    i = 0
    while i < len(lines):
        current_val = lines[i][0]
        count = 1
        while i + 1 < len(lines) and lines[i + 1][0] == current_val:
            count += 1
            i += 1
        result.append((current_val, count))
        i += 1
    
    return result


def compute_average_occurrences(folder_path):
    """Compute total count for each task within each file."""
    from collections import defaultdict
    folder = Path(folder_path)
    file_task_totals = {}
    
    # Only count these specific tasks
    allowed_tasks = {'REFINE', 'SYNTHESIS', 'FIX_COMPILATION', 'FIX_MODEL_CHECKING', 'FIX_PROOF'}
    
    for file in sorted(folder.glob("*")):
        if not file.is_file():
            continue
        result = read_and_split(file)
        consecutive_counts = count_consecutive(result)
        
        # Sum counts by task within this file (only allowed tasks)
        task_totals = defaultdict(int)
        for task, count in consecutive_counts:
            if task in allowed_tasks:
                task_totals[task] += count
        
        if task_totals:
            file_task_totals[file.name] = dict(task_totals)
    
    return file_task_totals


def compute_category_averages(file_task_totals):
    """Compute average occurrences for each task by category."""
    from collections import defaultdict
    
    # Create mapping of file names (without .txt) to categories
    category_map = {}
    for file_name in low:
        category_map[file_name] = 'low'
    for file_name in mid:
        category_map[file_name] = 'mid'
    for file_name in high:
        category_map[file_name] = 'high'
    
    # Group task totals by category
    category_task_data = defaultdict(lambda: defaultdict(list))
    all_task_data = defaultdict(list)
    
    for file_name, task_totals in file_task_totals.items():
        # Remove .txt extension to match category sets
        file_base = file_name.replace('.txt', '')
        category = category_map.get(file_base)
        
        if category:
            for task, count in task_totals.items():
                category_task_data[category][task].append(count)
                all_task_data[task].append(count)
    
    # Calculate averages for each category
    category_averages = {}
    for category in ['low', 'mid', 'high']:
        category_averages[category] = {}
        for task, counts in category_task_data[category].items():
            category_averages[category][task] = sum(counts) / len(counts)
    
    # Calculate overall averages
    overall_averages = {}
    for task, counts in all_task_data.items():
        overall_averages[task] = sum(counts) / len(counts)
    
    return category_averages, overall_averages


def main():
    folder = Path(path)
    if not folder.is_dir():
        print(f"Not a directory: {folder}", file=sys.stderr)
        sys.exit(1)

    file_task_totals = compute_average_occurrences(path)
    category_averages, overall_averages = compute_category_averages(file_task_totals)
    
    # Print category averages
    print("Average occurrences by category:")
    for category in ['low', 'mid', 'high']:
        print(f"\n{category.upper()}:")
        sum = 0
        for task in sorted(category_averages[category].keys()):
            avg = category_averages[category][task]
            sum += avg
            print(f"  {task}: {avg:.2f}")
        print(f"  Total Average: {sum:.2f}")

    # Print overall averages
    print("\n\nOVERALL AVERAGES:")
    sum = 0
    for task in sorted(overall_averages.keys()):
        avg = overall_averages[task]
        sum += avg
        print(f"  {task}: {avg:.2f}")
    print(f"  Total Average: {sum:.2f}")

if __name__ == "__main__":
    main()