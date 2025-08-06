package eventb_agent_core.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_core.proof.Hypothesis;
import eventb_agent_core.refinement.RefinementStep;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.llm.ParserUtils;

public abstract class LLMResponseParser {

	public abstract String getResponseString(String response);

	public abstract JSONObject getResponseWithTools(String response);

	public JSONObject getResponseContent(String response) throws JSONException {
		String answer = getResponseString(response);
		return new JSONObject(answer);
	}

	/* Model Synthesis */

	public JSONObject getContextJSON(JSONObject json) {
		return json.getJSONObject(SchemaKeys.CONTEXT_OBJ_KEY);
	}

	public JSONObject getMachineJSON(JSONObject json) {
		return json.getJSONObject(SchemaKeys.MACHINE_OBJ_KEY);
	}

	public String getContextName(JSONObject json) {
		JSONObject context = getContextJSON(json);
		return context.getString(SchemaKeys.CONTEXT);
	}

	public String getMachineName(JSONObject json) {
		JSONObject machine = getMachineJSON(json);
		return machine.getString(SchemaKeys.MACHINE);
	}

	/* Proof Fixing */

	public JSONArray getModificationJSONArray(JSONObject json) {
		return json.getJSONArray(SchemaKeys.MODIFICATION);
	}

	public String getExplanation(JSONObject json) {
		return json.getString(SchemaKeys.EXPLANATION);
	}

	/* Refinement Strategy */

	public JSONArray getRefinementStepsJSONArray(JSONObject json) {
		return json.getJSONArray(SchemaKeys.REF_STRATEGY);
	}

	/* helper methods */

	private List<String> getArrayOfStrings(JSONObject json, String key) {
		List<String> results = new ArrayList<>();

		if (!json.has(key)) {
			return results;
		}

		JSONArray array = json.getJSONArray(key);
		for (int i = 0; i < array.length(); i++) {
			results.add(array.getString(i));
		}
		return results;
	}

	private List<String[]> getArrayOfLabeledFormulae(JSONObject json, String keyForArray, String keyForFormulae) {
		JSONArray array = json.getJSONArray(keyForArray);
		List<String[]> results = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			String[] labeledPredInfo = new String[2];
			JSONObject labeledObject = array.getJSONObject(i);
			String label = labeledObject.getString(SchemaKeys.LABEL);
			String predicate = labeledObject.getString(keyForFormulae);
			labeledPredInfo[0] = label;
			labeledPredInfo[1] = ParserUtils.lex(predicate);
			results.add(labeledPredInfo);
		}
		return results;
	}

	private List<Map<String, Object>> getArrayOfEvents(JSONObject json, String key) {
		JSONArray array = json.getJSONArray(key);
		List<Map<String, Object>> results = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			Map<String, Object> eventInfo = new HashMap<>();
			JSONObject event = array.getJSONObject(i);
			String eventName = event.getString(SchemaKeys.EVENT_NAME);
			List<String> refines = getArrayOfStrings(event, SchemaKeys.REFINES);
			List<String> any = getArrayOfStrings(event, SchemaKeys.ANY);
			List<String[]> where = getArrayOfLabeledFormulae(event, SchemaKeys.WHERE, SchemaKeys.PRED);
			List<String[]> with = getArrayOfLabeledFormulae(event, SchemaKeys.WITH, SchemaKeys.PRED);
			List<String[]> then = getArrayOfLabeledFormulae(event, SchemaKeys.THEN, SchemaKeys.ASSIGN);

			eventInfo.put(SchemaKeys.EVENT_NAME, eventName);
			if (!refines.isEmpty())
				eventInfo.put(SchemaKeys.REFINES, refines);
			if (!any.isEmpty())
				eventInfo.put(SchemaKeys.ANY, any);
			if (!where.isEmpty())
				eventInfo.put(SchemaKeys.WHERE, where);
			if (!with.isEmpty())
				eventInfo.put(SchemaKeys.WITH, with);
			if (!then.isEmpty())
				eventInfo.put(SchemaKeys.THEN, then);

			results.add(eventInfo);
		}
		return results;
	}

	/* context methods */

	public List<String> getExtends(JSONObject contextJSON) {
		return getArrayOfStrings(contextJSON, SchemaKeys.EXTENDS);
	}

	public List<String> getSets(JSONObject contextJSON) {
		return getArrayOfStrings(contextJSON, SchemaKeys.SETS);
	}

	public List<String> getConstants(JSONObject contextJSON) {
		return getArrayOfStrings(contextJSON, SchemaKeys.CONSTANTS);
	}

	public List<String[]> getAxioms(JSONObject contextJSON) {
		return getArrayOfLabeledFormulae(contextJSON, SchemaKeys.AXIOMS, SchemaKeys.PRED);
	}

	/* machine methods */

	public List<String> getRefines(JSONObject machineJSON) {
		return getArrayOfStrings(machineJSON, SchemaKeys.REFINES);
	}

	public List<String> getSees(JSONObject machineJSON) {
		return getArrayOfStrings(machineJSON, SchemaKeys.SEES);
	}

	public List<String> getVariables(JSONObject machineJSON) {
		return getArrayOfStrings(machineJSON, SchemaKeys.VARIABLES);
	}

	public List<String[]> getInvariants(JSONObject machineJSON) {
		return getArrayOfLabeledFormulae(machineJSON, SchemaKeys.INVARIANTS, SchemaKeys.PRED);
	}

	public List<String[]> getVariants(JSONObject machineJSON) {
		return getArrayOfLabeledFormulae(machineJSON, SchemaKeys.VARIANTS, SchemaKeys.EXPR);
	}

	public List<Map<String, Object>> getEvents(JSONObject machineJSON) {
		List<Map<String, Object>> events = getArrayOfEvents(machineJSON, SchemaKeys.EVENTS);
		return events;
	}

	/* proof fixing methods */

	public List<Hypothesis> getHypotheses(JSONArray modificationJSONArray) {
		List<Hypothesis> hypotheses = new ArrayList<>();
		for (int i = 0; i < modificationJSONArray.length(); i++) {
			JSONObject entry = modificationJSONArray.getJSONObject(i);
			JSONObject hypothesisJSON = entry.getJSONObject(SchemaKeys.HYP);
			JSONArray instantiationsJSONArray = entry.getJSONArray(SchemaKeys.INSTANTIATIONS);

			String label = hypothesisJSON.getString(SchemaKeys.LABEL);
			String predicate = hypothesisJSON.getString(SchemaKeys.PRED);
			String[] instantiations = new String[instantiationsJSONArray.length()];
			for (int j = 0; j < instantiationsJSONArray.length(); j++) {
				instantiations[j] = instantiationsJSONArray.getString(j);
			}

			Hypothesis hypothesis = new Hypothesis(label, predicate, instantiations);
			hypotheses.add(hypothesis);
		}
		return hypotheses;
	}

	public List<Hypothesis> getHypotheses(JSONObject modificationJSON) {
		List<Hypothesis> hypotheses = new ArrayList<>();

		JSONObject argumentsJSON = modificationJSON.getJSONObject(Constants.FUNCTION_ARGS);
		JSONObject hypothesisJSON = argumentsJSON.getJSONObject(SchemaKeys.HYP);
		JSONArray instantiationsJSONArray = argumentsJSON.getJSONArray(SchemaKeys.INSTANTIATIONS);

		String label = hypothesisJSON.getString(SchemaKeys.LABEL);
		String predicate = hypothesisJSON.getString(SchemaKeys.PRED);
		String[] instantiations = new String[instantiationsJSONArray.length()];
		for (int j = 0; j < instantiationsJSONArray.length(); j++) {
			instantiations[j] = instantiationsJSONArray.getString(j);
		}

		Hypothesis hypothesis = new Hypothesis(label, predicate, instantiations);
		hypotheses.add(hypothesis);

		return hypotheses;
	}

	/* refinement strategy methods */

	public RefinementStep getRefinementStep(JSONObject refStepJSON) {
		int refNo = refStepJSON.getInt(SchemaKeys.REF_NO);
		JSONArray reqIDs = refStepJSON.getJSONArray(SchemaKeys.REQUIREMENT_IDS);
		String modelDesc = refStepJSON.getString(SchemaKeys.MODEL_DESC);

		List<String> reqIDList = new ArrayList<>();
		for (int i = 0; i < reqIDs.length(); i++) {
			reqIDList.add(reqIDs.getString(i));
		}

		return new RefinementStep(refNo, reqIDList, modelDesc);
	}

}
