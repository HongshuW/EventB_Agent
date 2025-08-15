###### pipline
import json
import time
import datetime
import os
from pathlib import Path
import shutil
from openai import OpenAI

import anthropic
import subprocess
import re

from sklearn.metrics.pairwise import cosine_similarity
from sklearn.feature_extraction.text import TfidfVectorizer

openai_key = os.environ["OPENAI_API_KEY"]
claude_key = os.environ["CLAUDE_API_KEY"]

client = OpenAI(
    api_key=openai_key,
)

client_claude = anthropic.Anthropic(api_key=claude_key)


### read ./test-automated-pipeline.json

# index.html: lines 383 - 484: based on input, process constants and variables
# const-and-vars.html: lines 117 - 131, 152 - 158, 209 - 299: based on input & index.html's llm output, process actions
# action.html: nothing (?)
# assertion.html: fully replaced by just directly reading from PAT-Examples.json (?)
# nl-instruct.html: lines 56 - 151: based on index.html's llm output, const-and-vars's llm output, and input data (assertions), 
# codegen.html: lines 108 - 243 + 271 - 306 **automatically select the longest chunk of code** and proceed to verify.html (if no code blocks or syntax error: trigger regeneration, regeneration constrained to 3 times?): based on nl-instruct.html's processed result & input data (overall description) [and RAG/syntax]:
# verify.html: **save each round's verification results!!!** (e.g., verifications/modelName/round_{i}.json) + only retain: lines 149 - 174 auto save upon success lines 252 - 288 / save mismatch traces for refine.html lines 175 - 197, 205 - 230, 238 - 246: based on codegen.html's verification results
# refine.html: lines 208 - 233 + lines 237 - 305 + lines 319 - 350, similarly, **automatically select the longest chunk of code** and proceed to verify.html (if no code blocks or syntax error: trigger regeneration, regeneration constrained to 3 times?): based on the previous code and verification results

def get_LLM_answers(question, context, history):
    if question:
        try:
            # Get model response
            completion = client.chat.completions.create(
                model="o3-mini-2025-01-31",
                reasoning_effort="high",
                messages=[
                    {
                        "role": "user",
                        "content": question
                    }
                ]
            )
            answer = completion.choices[0].message.content
            
            # Create interaction record
            current_time = datetime.datetime.now()
            formatted_time = current_time.strftime("%Y-%m-%d %H:%M:%S")
            
            interaction = {
                'timestamp': formatted_time,
                'question': question,
                'answerGPT': answer,
                'context': context,
            }
            history = history.strip()  # 去除空格和换行符

            if history == 'skip':
                return interaction

            if history == 'context':
                msg_history_path = './history/1context-history.json'
            elif history == 'machine':
                msg_history_path = './history/2machine-history.json'
            elif history == 'instruct-part':
                msg_history_path = './history/3nl-instruction-part.json'            
            else:
                msg_history_path = './history/history.json'
            try:
                with open(msg_history_path, 'r') as file:
                    history = json.load(file)
            except FileNotFoundError:
                history = []

            history.append(interaction)

            with open(msg_history_path, 'w') as file:
                json.dump(history, file, indent=4)
            
            return interaction
        except Exception as e:
            print(f"Error: {str(e)}")

def save_run_time(model_name, stage,run_time, hasMismatch=None, codegenFailed=None):
    run_time_record_path = f'./run_time_record/{model_name}.json'
    # Make sure the directory exists
    os.makedirs('./run_time_record', exist_ok=True)
    try:
        with open(run_time_record_path, 'r') as file:
            run_time_data = json.load(file)
    except FileNotFoundError:
        print("File not found. Creating a new one.")
        run_time_data = {}
    # Create the data to save
    save_content = {"runTime": run_time}
    if hasMismatch is not None and hasMismatch != "":
        save_content["hasMismatch"] = hasMismatch
    if codegenFailed is not None and codegenFailed != "":
        save_content["codegenFailed"] = codegenFailed
    # Update the dictionary
    run_time_data[stage] = save_content

    # Save back to file
    with open(run_time_record_path, 'w') as file:
        json.dump(run_time_data, file, indent=4)

    return {"message": "Run time saved successfully."}



# save same foloder save in piple
def gen_context(structuredData):
    # modelDescription = structuredData['description'] # read in the description from structuredData
    functionalities = " ".join(v for k, v in structuredData.items() if k.startswith("FUN-"))
    eqp_requirements    = " ".join(v for k, v in structuredData.items() if k.startswith("EQP-"))
    env_requirements    = " ".join(v for k, v in structuredData.items() if k.startswith("ENV-"))
    saf_requirements    = " ".join(v for k, v in structuredData.items() if k.startswith("SAF-"))

    modelDescription = (
        f"Functionality: {functionalities}\n"
        f"Equipment Requirements: {eqp_requirements}\n"
        f"Environment Requirements: {env_requirements}\n"
        f"Safety Requirements: {saf_requirements}\n"
    )
    
    prompt_gen_context = f"""As an expert in Event-B context modeling, can you extract all the constants from the following description?
{modelDescription}

Please structure your analyzed results as a single JSON object with the following schema:
{{
    "contextName": "counterContext",
    "constants": [
    {{
        "name": "maxValue",
        "value": "5",
        "type": "integer"
    }}
    /* more constants… */
    ]
}}

Requirements:
1. **contextName**: a descriptive name for the described system context.
2. **name**: a unique identifier for each constant (e.g. `maxValue`).
3. **type**: one of Event-B's basic types (e.g. `int`, `bool`, `real`).
4. **value**: a realistic value matching the declared type.
5. For each constant, ensure that all three fields are filled in, and do not include any other fields.
6. Your output should be a valid JSON string that can be parsed directly."""

    start = time.perf_counter()
    ##### use LLM
    print(f"analyzing context...")
    print("prompt_gen_context", prompt_gen_context)
    get_LLM_answers(prompt_gen_context, structuredData, 'context')
    end = time.perf_counter()
    run_time = end - start
    save_run_time(structuredData['modelName'], 'context-time', run_time)

def _get_last_context_json():
    context_json_path = './history/1context-history.json'
    try:
        with open(context_json_path, 'r', encoding='utf-8') as file:
            data = json.load(file)
        
        if isinstance(data, list) and data:
            return data[-1]['answerGPT']  # Last entry in the list
        elif isinstance(data, dict):
            return data['answerGPT']  # Single object in file, just return it
        else:
            print("Context JSON is empty or in an unexpected format.")
            return None

    except FileNotFoundError:
        print("Context JSON file not found. Please run gen_context first.")
        return None
    except json.JSONDecodeError:
        print("Error decoding JSON from context file.")
        return None

def gen_machine(structured_data):
    contextJSON = _get_last_context_json()

    functionalities = " ".join(v for k, v in structured_data.items() if k.startswith("FUN-"))
    eqp_requirements    = " ".join(v for k, v in structured_data.items() if k.startswith("EQP-"))
    env_requirements    = " ".join(v for k, v in structured_data.items() if k.startswith("ENV-"))
    saf_requirements    = " ".join(v for k, v in structured_data.items() if k.startswith("SAF-"))
    modelDescription = (
        f"Functionality: {functionalities}\n"
        f"Equipment Requirements: {eqp_requirements}\n"
        f"Environment Requirements: {env_requirements}\n"
        f"Safety Requirements: {saf_requirements}\n"
    )

    prompt_gen_machine = f"""As an expert in Event-B machine modeling, analyze the following system description and previously extracted context (constants), then produce the machine components: variables, invariants, and events.

System description:
{modelDescription}

Context (constants):
{contextJSON}

Output a single JSON object with this exact schema:
{{
  "machineName": "counterMachine",
  "machine": {{
    "variables": [
      {{
        "name": "carsOnBridge",
        "type": "int",
        "initialValue": 0
      }}
    ],
    "invariants": [
      {{
        "predicate": "carsOnBridge >= 0"
      }}
    ],
    "events": [
      {{
        "name": "enterBridge",
        "guards": ["lightMainland = GREEN", "carsOnBridge < MAX_CARS_BRIDGE"],
        "actions": ["carsOnBridge := carsOnBridge + 1"]
      }}
    ]
  }}
}}

Requirements:
- machineName: a descriptive name for the machine (e.g. `counterMachine`).
- Variables: each has name, type (must be one of Event-B's basic types), and initialValue.
- Invariants: Use only declared variables/constants to specify the predicate.
- Events: fields = name, guards[], actions[]; guards are state predicates (conditions for the event to occur); actions are assignments to declared variables.
- Consistency: initialValue should satisfy invariants; events should preserve typing; output JSON only, no extra fields.

Note: The JSON shown above is illustrative only—populate all fields case by case based on the provided description and context.
"""

    start = time.perf_counter()
    print(f"analyzing machine...")
    print("prompt_gen_machine", prompt_gen_machine)
    get_LLM_answers(prompt_gen_machine, structured_data, 'machine')
    end = time.perf_counter()
    run_time = end - start
    
    model_name = structured_data.get('modelName', 'unknown_model')
    save_run_time(model_name, 'machine-time', run_time)

def _get_last_machine_json():
    machine_json_path = './history/2machine-history.json'
    try:
        with open(machine_json_path, 'r', encoding='utf-8') as file:
            data = json.load(file)
        
        if isinstance(data, list) and data:
            return data[-1]['answerGPT']  # Last entry in the list
        elif isinstance(data, dict):
            return data['answerGPT']  # Single object in file, just return it
        else:
            print("Context JSON is empty or in an unexpected format.")
            return None

    except FileNotFoundError:
        print("Context JSON file not found. Please run gen_context first.")
        return None
    except json.JSONDecodeError:
        print("Error decoding JSON from context file.")
        return None
    
def _process_instruction_helper(description, context, machine):
    instruction = (
        "We are generating the Event-B schema necessary and sufficient to represent the following system:\n"
        f"System description:\n{description}\n\n"
        "Through our analysis, the **Context** contains the following constants:\n"
        f"{context}\n\n"
        "The **Machine** should contain the following variables, invariants, and events:\n"
        f"{machine}\n"
    )
    return instruction

def load_eventb_schema():
    """Load the JSON schema that defines the constrained output."""
    path = "./eventb_base_schema.json"
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def generate_eventb_baseline(prompt):
    schema = load_eventb_schema()
    response = client.responses.create(
        model="o3-mini-2025-01-31",
        reasoning={"effort": "high"},
        input=[
            {
                "role": "system",
                "content": (
                    "You are an Event-B assistant. Respond ONLY with JSON that "
                    "validates against the provided schema."
                ),
            },
            {"role": "user", "content": prompt},
        ],
        text={
            "format": schema
        },
    )

    answer = response.output_text
    return answer

def gen_schema(structured_data):
    start_time = time.perf_counter()

    context = _get_last_context_json()
    machine = _get_last_machine_json()

    # 1. RAG: Get most relevant example
    print("Retrieving RAG example...")
    
    functionalities = " ".join(v for k, v in structured_data.items() if k.startswith("FUN-"))
    eqp_requirements    = " ".join(v for k, v in structured_data.items() if k.startswith("EQP-"))
    env_requirements    = " ".join(v for k, v in structured_data.items() if k.startswith("ENV-"))
    saf_requirements    = " ".join(v for k, v in structured_data.items() if k.startswith("SAF-"))
    modelDescription = (
        f"Functionality: {functionalities}\n"
        f"Equipment Requirements: {eqp_requirements}\n"
        f"Environment Requirements: {env_requirements}\n"
        f"Safety Requirements: {saf_requirements}\n"
    )
    instruction = _process_instruction_helper(modelDescription, context, machine)
    print("instruction", instruction)

    retrieved_example = _get_most_relevant_rag_example_basic(instruction)
    retrieved_nl = retrieved_example['nl']
    retrieved_code = retrieved_example['code']

    # 2. Read Syntax Data
    syntax_general_info = ""
    syntax_pitfalls_rules = ""
    try:
        with open('./syntax-dataset.json', 'r') as f:
            syntax_data = json.load(f)
        syntax_general_info = syntax_data.get("general_info", "")
        syntax_pitfalls_rules = syntax_data.get("pitfalls_rules", "")
        # print("Syntax data loaded.")
    except FileNotFoundError:
        print("Error: syntax-dataset.json not found.")
    except json.JSONDecodeError:
        print("Error: Could not decode syntax-dataset.json.")

    # 3. Generate Schema
    prompt_gen_schema = f"""You are an expert in Event-B modeling and fully understand Event-B syntax, semantics, and best practices.

--- Quick Reference ---
General Information:
{syntax_general_info}

Pitfalls and Syntax Guidelines:
{syntax_pitfalls_rules}

Your task is to generate the Event-B schema (following the **strict schema format**) corresponding to a given natural language instruction.

### Example:
**Input Instruction:**
{retrieved_nl}
**Expected Output Schema:**
{retrieved_code}

Now, generate the Event-B schema for the **following instruction**:

### Instruction:
{instruction}

--- Additional **Mandatory** Rules (not explicitly stated in the instruction) ---
1. **Context**:
   - Each constant **must** have a corresponding axiom specifying its type (use the type from the analysis of constants in the instruction).
2. **Machine**:
   - The machine **must** reference the context using its `contextName`.
   - Each variable **must** have a corresponding invariant specifying its type (use the type from the analysis of machine variables in the instruction).
   - For each variable, there **must** be an initialization event in `Events` that sets the variable to its analyzed `initialValue`.

### Response:"""
    print("Generating schema...")
    print("prompt_gen_schema", prompt_gen_schema)

    generated_schema = generate_eventb_baseline(prompt_gen_schema)

    current_time = datetime.datetime.now()
    formatted_time = current_time.strftime("%Y-%m-%d %H:%M:%S")
    interaction = {
        'timestamp': formatted_time,
        'answerGPT': generated_schema,
    }

    if not generated_schema:
        print("Error: Schema generation failed.")
        return None
    else:
        # save the generated schema
        path = './history/3schema.json'
        try:
            with open(path, 'r') as file:
                history = json.load(file)
        except FileNotFoundError:
            history = []

        history.append(interaction)

        with open(path, 'w') as file:
            json.dump(history, file, indent=4)

    end_time = time.perf_counter()
    run_time = end_time - start_time
    
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d-%H-%M-%S")
    model_name = structured_data.get('modelName', 'unknown_model')
    save_run_time(model_name, f'genschema-time_{timestamp}', run_time)
    
    return generated_schema


def _get_most_relevant_rag_example_basic(instruction, rag_database_path='./database-rag-claude.json'):
    try:
        with open(rag_database_path, 'r') as f:
            database = json.load(f)
        
        if not instruction:
            print("Warning: RAG instruction is empty. Returning no example.")
            return {"nl": "", "code": ""}

        nls = [entry["nl"] for entry in database if entry.get("nl")]
        if not nls:
            print("Warning: No valid RAG database.")
            return {"nl": "", "code": ""}
        
        vectorizer = TfidfVectorizer().fit(nls + [instruction])
        vectors = vectorizer.transform(nls + [instruction])

        similarity_scores = cosine_similarity(vectors[-1], vectors[:-1])[0]
        most_similar_idx = similarity_scores.argmax()

        matched_entry = database[most_similar_idx]
        return {"nl": matched_entry["nl"], "code": matched_entry["code"]}
            
    except FileNotFoundError:
        print(f"Error: RAG database file not found at {rag_database_path}")
        return {"nl": "", "code": ""}
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from RAG database file {rag_database_path}")
        return {"nl": "", "code": ""}

def _get_last_schema_json():
    machine_json_path = './history/3schema.json'
    try:
        with open(machine_json_path, 'r', encoding='utf-8') as file:
            data = json.load(file)
        
        if isinstance(data, list) and data:
            raw_schema = data[-1]['answerGPT']  # Last entry in the list
        elif isinstance(data, dict):
            raw_schema = data['answerGPT']  # Single object in file, just return it
        else:
            print("Context JSON is empty or in an unexpected format.")
            return None
        
        if isinstance(raw_schema, str):
            try:
                raw_schema = json.loads(raw_schema)
            except json.JSONDecodeError:
                print("Error decoding JSON from raw schema string.")
                return None
            
        return raw_schema

    except FileNotFoundError:
        print("Context JSON file not found. Please run gen_context first.")
        return None
    except json.JSONDecodeError:
        print("Error decoding JSON from context file.")
        return None

def _archive_model_version(model_name: str, model_file: Path) -> Path:
    """
    Copy model_file to ./history/model_versions/<model_name>/<basename>__YYYY-mm-dd-HH-MM-SS<ext>
    Returns the archived file path.
    """
    ts = datetime.datetime.now().strftime("%Y-%m-%d-%H-%M-%S")
    hist_dir = Path(f"./history/model_versions/{model_name}").resolve()
    hist_dir.mkdir(parents=True, exist_ok=True)

    stem = model_file.stem
    ext = model_file.suffix  # e.g., ".eventb" or ".mch"
    archived = hist_dir / f"{stem}__{ts}{ext}"

    shutil.copy2(str(model_file), str(archived))
    print(f"[archive] Saved version: {archived}")
    return archived

def gen_code(structured_data):
    json_schema = _get_last_schema_json()
    if not json_schema:
        raise ValueError("No valid schema found.")
    
    # Save the schema to a temporary file
    model_name = structured_data.get("modelName", "default_model")
    schema_path = Path(f"./generated_code/{model_name}/schema.json").resolve()
    schema_path.parent.mkdir(parents=True, exist_ok=True)
    with open(schema_path, 'w', encoding='utf-8') as f:
        json.dump(json_schema, f, ensure_ascii=False, indent=2)
    
    converter_dir = Path("./fileconverter/eclipse").resolve()
    workspace_dir = Path("./workspace").resolve()
    cmd = [
        str(converter_dir / "eclipse"),
        "-application", "EventBFileConverter.app",
        "-data", str(workspace_dir),
        "-nosplash",
        "-json", str(schema_path),
        "-projectname", str(model_name)
    ]
    # to write the code to convert the schema to Event-B model
    try:
        print("Running converter command:", " ".join(cmd))
        result = subprocess.run(
            cmd,
            cwd=str(converter_dir),
            capture_output=True,
            text=True,
            check=False
        )
        print("Converter output:", result)
    except FileNotFoundError as e:
        raise RuntimeError(
            f"Failed to launch converter. Expected 'eclipse' under {converter_dir}. "
            f"Original error: {e}"
        )
    
    src_dir = Path(f"./workspace/{model_name}/exports").resolve()
    if not src_dir.exists():
        raise FileNotFoundError(f"Source export folder not found: {src_dir}")

    model_path = Path(f"./generated_code/{model_name}/").resolve()
    model_path.mkdir(parents=True, exist_ok=True)  # ✅ Create if not exists

    # Move all files from src_dir to dest_dir
    moved_files = []
    for file in src_dir.iterdir():
        if file.is_file():
            dest = model_path / file.name
            if dest.exists():  # replace working copy
                dest.unlink()
            shutil.move(str(file), str(dest))
            moved_files.append(dest)

    if not moved_files:
        raise FileNotFoundError(f"No files found to move in {src_dir}")

    for mf in moved_files:
        # Only archive model files of interest
        if mf.suffix in (".eventb", ".mch"):
            _archive_model_version(model_name, mf)

    # Prefer flattened B generated by Rodin/ProB
    for ext in (".eventb", ".mch"):
        hit = next(model_path.glob(f"*{ext}"), None)
        if hit:
            print(f"Found model file: {hit.name}")
            return str(hit)

    # Last resort: .bum (XML). probcli can’t parse these directly in your case.
    bum = next(model_path.glob("*.bum"), None)
    if bum:
        raise ValueError(
            f"Only .bum found ({bum.name}). This is XML and not accepted here.\n"
            f"Please use the flattened B files (.bcm/.mch)."
        )

    raise FileNotFoundError(f"No .bcm/.mch (or .bum) files found in {model_path}")

def verify_code(model_file):
    p = Path(model_file)
    if not p.exists():
        raise FileNotFoundError(f"Model file not found: {model_file}")

    # Run from the directory so relative includes (.bcc/.bpo/…) work
    cwd = str(p.parent)
    target = p.name

    cmd = ["probcli", target, "--model_check"]

    try:
        result = subprocess.run(
            cmd, cwd=cwd, capture_output=True, text=True, check=False
        )
    except FileNotFoundError:
        raise RuntimeError("probcli not found in PATH")

    out, err, rc = result.stdout, result.stderr, result.returncode

    # Simple outcome heuristic
    failed_markers = (
        "INVARIANT VIOLATION",
        "invariant violated",
        "deadlock found",
        "Loading Specification Failed",
        "parse_error",
        "*** error occurred ***",
    )
    failed = any(m in out or m in err for m in failed_markers)
    status = "OK" if (rc == 0 and not failed) else "FAIL"

    # Optional: print for quick debugging
    print("=== probcli CMD ===", " ".join(cmd), f"(cwd={cwd})")
    print("=== STDOUT ===\n", out)
    print("=== STDERR ===\n", err)

    return {
        "status": status,
        "returncode": rc,
        "model_file": str(p.resolve()),
        "stdout": out,
        "stderr": err,
        "cmd": cmd,
        "cwd": cwd,
    }

def _parse_trace(stdout: str) -> dict:
    trace_text = ""
    last_event = None
    involved_names = set()

    # Grab the TRACE block (if present)
    m = re.search(r"\*\*\* TRACE.*?:\s*(.+?)(?:\n\s*\n|\Z)", stdout, flags=re.S)
    if m:
        trace_text = m.group(1).strip()
        print("Found TRACE block in output: ", trace_text)

        current_time = datetime.datetime.now()
        formatted_time = current_time.strftime("%Y-%m-%d %H:%M:%S")
        interaction = {
            'timestamp': formatted_time,
            'mismatches': trace_text,
        }
        # save the generated schema
        path = './history/6mismatch_traces.json'
        try:
            with open(path, 'r') as file:
                history = json.load(file)
        except FileNotFoundError:
            history = []
        history.append(interaction)
        with open(path, 'w') as file:
            json.dump(history, file, indent=4)

        # Lines like: " 2: INITIALISATION(counter=0)" or " 3: Increment(n=1)"
        for line in trace_text.splitlines():
            line = line.strip()
            # event name before '(' or end-of-line after index "k: "
            em = re.search(r"^\d+:\s*([A-Za-z_][A-Za-z_0-9]*)", line)
            if em:
                last_event = em.group(1)
            # identifiers inside (...) before '='
            for name in re.findall(r"([A-Za-z_][A-Za-z_0-9]*)\s*=", line):
                involved_names.add(name)

    print("Parsed trace info:")
    print(f"  Trace Text: {trace_text}")
    print(f"  Last Event: {last_event}")
    print(f"  Involved Names: {involved_names}")

    return {
        "trace_text": trace_text,
        "last_event": last_event,
        "involved_names": sorted(involved_names)
    }

def _classify_error(stdout: str, stderr: str) -> str:
    combo = (stdout + "\n" + stderr).lower()
    if "deadlock" in combo:
        return "deadlock"
    if "invariant violation" in combo or "invariant violated" in combo or "invariant_violation" in combo:
        return "invariant_violation"
    if "parse_error" in combo or "loading specification failed" in combo:
        return "parse_error"
    return "unknown"

def _build_repair_prompt(original_schema: dict, verification_result: dict) -> str:
    err_type = _classify_error(verification_result.get("stdout",""), verification_result.get("stderr",""))
    trace_info = _parse_trace(verification_result.get("stdout",""))
    trace_text = trace_info["trace_text"] or "(no explicit trace provided)"
    last_event = trace_info["last_event"]
    involved = trace_info["involved_names"]

    # High-level guidance per error
    if err_type == "deadlock":
        guidance = [
            "Consider the initialization value/range for the constants/variables appearing in the trace.",
            "Please consider constraining the range of the constants through adding a corresponding axiom. For example, if a constant should not be equal or less than 0, you can add an axiom to state that the constant is greater than 0."
        ]
        if last_event and last_event.upper() != "INITIALISATION":
            guidance.append(f"Check the enabling conditions (guards) of event `{last_event}`.")
        if involved:
            guidance.append(f"Pay special attention to: {', '.join(involved)}.")
    elif err_type == "invariant_violation":
        target_event = last_event or "<event name unknown>"
        guidance = [
            f"Consider tightening the guard of the involved event `{target_event}` if necessary, so that the invariant holds after its execution, and ensure that the action performed within the event is correct.",
        ]
    elif err_type == "parse_error":
        guidance = [
            "Fix syntax issues first so the model can be parsed.",
            "Ensure every constant/variable is declared with a type and every identifier used in guards/actions is declared.",
        ]
    else:
        guidance = [
            "Apply minimal, safe changes to eliminate the reported issue.",
            "Prefer guard/initialisation adjustments over structural rewrites unless necessary.",
        ]
    
    guidance_text = "\n".join(f"- {g}" for g in guidance)

    # # Schema rules you asked us to enforce (from earlier conventions)
    # schema_rules = [
    #     "Each constant must have a corresponding axiom specifying its type.",
    #     "Each variable must have a corresponding invariant specifying its type.",
    #     "A machine should see its context (with contextName).",
    #     "Preserve existing names and structure unless a change is necessary for correctness.",
    #     "Keep changes minimal and justified.",
    # ]

    # Compose the instruction prompt
    prompt = f"""
You are an Event-B expert. Your task is to REPAIR the following schema so that it passes ProB model checking.

## Schema to be Repaired (JSON)
{json.dumps(original_schema, ensure_ascii=False, indent=2)}

## Current Verification Outcome (summary)
- error_type: {err_type}
- last_event_in_trace: {last_event or "N/A"}
- involved_identifiers: {', '.join(involved) if involved else "N/A"}
- trace:
{trace_text}

## Repair Objectives
- Resolve the above issue while preserving model intent and identifiers as much as possible.
- Prioritize minimal edits: prefer adjusting initialisation values/ranges or event guards over large structural changes.

## Heuristics to Apply
{guidance_text}

## Output Requirements
- Return ONLY the repaired schema as pure JSON (no markdown fences, no commentary).
- Do NOT add or remove top-level fields; keep the schema structure unchanged.
- Modify only what is necessary to address the issue.
""".strip()

    print("Generated repair prompt:")
    print(prompt)

    return prompt

def repair_code(structured_data, verification_result):
    start_time = time.perf_counter()
    original_schema = _get_last_schema_json()
    repair_prompt = _build_repair_prompt(original_schema, verification_result)
    repaired_schema = generate_eventb_baseline(repair_prompt)
    end_time = time.perf_counter()
    current_time = datetime.datetime.now()
    formatted_time = current_time.strftime("%Y-%m-%d %H:%M:%S")
    interaction = {
        'timestamp': formatted_time,
        'answerGPT': repaired_schema,
    }

    if not repaired_schema:
        print("Error: Schema generation failed.")
        return None
    else:
        # save the generated schema
        path = './history/5repair.json'
        try:
            with open(path, 'r') as file:
                history = json.load(file)
        except FileNotFoundError:
            history = []

        history.append(interaction)

        with open(path, 'w') as file:
            json.dump(history, file, indent=4)

        # also save to 3schema.json
        path = './history/3schema.json'
        try:
            with open(path, 'r') as file:
                history = json.load(file)
        except FileNotFoundError:
            history = []

        history.append(interaction)

        with open(path, 'w') as file:
            json.dump(history, file, indent=4)

    run_time = end_time - start_time
    
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d-%H-%M-%S")
    model_name = structured_data.get('modelName', 'unknown_model')
    save_run_time(model_name, f'repair-time_{timestamp}', run_time)

    return repaired_schema

def _verification_ok(verification_result):
    return (
        verification_result.get("status").upper() == "OK" and
        verification_result.get("returncode") == 0 and
        not any(m in verification_result.get("stdout", "") for m in (
            "INVARIANT VIOLATION",
            "invariant violated",
            "invariant_violation",
            "deadlock",
            "Loading Specification Failed",
            "parse_error",
            "*** error occurred ***"
        ))
    )

if __name__ == '__main__':
    # read ./input-dataset.json
    with open('./input-dataset.json', 'r') as file:
        structured_data_list = json.load(file)
        # assert len(structured_data_list) == 6, "The number of entries in the JSON file should be 6."
        for i in range(len(structured_data_list)): # Iterate through all entries in the JSON
            current_structured_data = structured_data_list[i]
            model_name = current_structured_data.get('modelName', 'N/A')
            print(f"Processing data entry {i} with model name: {model_name}")

            # Stage 1: Generate Context
            print(f"getting context for entry {i}")
            gen_context(current_structured_data)

            # Stage 2: Generate Machine
            print(f"getting machine for entry {i}")
            gen_machine(current_structured_data)

            # Stage 3: Generate Schema
            print(f"generating schema for entry {i}")
            gen_schema(current_structured_data)

            # Stage 4: Converting Schema to Code
            model_file = gen_code(current_structured_data)
            verification_result = verify_code(model_file)
            print(f"Verification result for entry {i}: {verification_result}")

            if _verification_ok(verification_result):
                print(f"[{model_name}] Verification OK. No repair needed.")
                model_path = Path(f"./generated_code/{model_name}").resolve()
                # Look for the current .eventb file
                current_eventb = next(model_path.glob("*.eventb"), None)
                if current_eventb:
                    verified_copy = model_path / "verifiedCode.eventb"
                    shutil.copy2(str(current_eventb), str(verified_copy))
                    print(f"[{model_name}] Verified model saved as {verified_copy}")
                else:
                    print(f"[{model_name}] No .eventb file found to mark as verified.")
                continue

            max_repairs = 5
            for attempt in range(1, max_repairs + 1):
                print(f"[{model_name}] Repair attempt {attempt}/{max_repairs}")

                repaired_schema = repair_code(current_structured_data, verification_result)
                print(f"Repaired schema for entry {i}, attempt {attempt}.")

                code = gen_code(current_structured_data)
                verification_result = verify_code(code)
                print(f"[{model_name}] Post-repair verification status: {verification_result.get('status')}")

                if _verification_ok(verification_result):
                    print(f"[{model_name}] Verification OK after {attempt} repair(s).")
                    model_path = Path(f"./generated_code/{model_name}").resolve()
                    # Look for the current .eventb file
                    current_eventb = next(model_path.glob("*.eventb"), None)
                    if current_eventb:
                        verified_copy = model_path / "verifiedCode.eventb"
                        shutil.copy2(str(current_eventb), str(verified_copy))
                        print(f"[{model_name}] Verified model saved as {verified_copy}")
                    else:
                        print(f"[{model_name}] No .eventb file found to mark as verified.")
                    break
            else:
                # Only hits if loop didn't break (i.e., 5 unsuccessful repairs)
                print(f"[{model_name}] Reached max repairs ({max_repairs}) without success.")
            
            print(f"Finished processing entry {i}.")