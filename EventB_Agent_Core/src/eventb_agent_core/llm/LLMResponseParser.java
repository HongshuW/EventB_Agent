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
import eventb_agent_core.refinement.SystemRequirements;
import eventb_agent_core.utils.llm.ParserUtils;

public abstract class LLMResponseParser {

	protected LLMModels llmModel;

	public LLMResponseParser(LLMModels llmModel) {
		this.llmModel = llmModel;
	}

	public abstract long getTokens(String response);

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
			String[] labeledPredInfo = new String[3];
			JSONObject labeledObject = array.getJSONObject(i);
			String label = labeledObject.getString(SchemaKeys.LABEL);
			String predicate = labeledObject.getString(keyForFormulae);
			String comments = labeledObject.getString(SchemaKeys.CMT);
			labeledPredInfo[0] = label;
			labeledPredInfo[1] = ParserUtils.lex(predicate);
			labeledPredInfo[2] = comments;
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

	public List<Hypothesis> getHypotheses(JSONObject argumentsJSON, String key) {
		return getHypotheses(argumentsJSON, key, SchemaKeys.PRED);
	}

	public List<Hypothesis> getHypotheses(JSONObject argumentsJSON, String key, String innerKey) {
		List<Hypothesis> hypotheses = new ArrayList<>();

		JSONObject hypothesisJSON = argumentsJSON.getJSONObject(key);

		String label = hypothesisJSON.getString(SchemaKeys.LABEL);
		String predicate = hypothesisJSON.getString(innerKey);

		if (argumentsJSON.has(SchemaKeys.INSTANTIATIONS)) {
			JSONArray instantiations = argumentsJSON.getJSONArray(SchemaKeys.INSTANTIATIONS);
			String[] insts = new String[instantiations.length()];
			for (int i = 0; i < instantiations.length(); i++) {
				insts[i] = instantiations.getString(i);
			}
			Hypothesis hypothesis = new Hypothesis(label, predicate, insts);
			hypotheses.add(hypothesis);
		} else {
			Hypothesis hypothesis = new Hypothesis(label, predicate);
			hypotheses.add(hypothesis);
		}

		return hypotheses;
	}

	/* refinement strategy methods */

	public RefinementStep getRefinementStep(JSONObject refStepJSON) {
		return getRefinementStep(refStepJSON, null);
	}

	public RefinementStep getRefinementStep(JSONObject refStepJSON, SystemRequirements systemReqs) {
		int refNo = refStepJSON.getInt(SchemaKeys.REF_NO);
		JSONArray reqIDs = refStepJSON.getJSONArray(SchemaKeys.REQUIREMENT_IDS);
		String modelDesc = refStepJSON.getString(SchemaKeys.MODEL_DESC);
		JSONArray gluingInvs = refStepJSON.getJSONArray(SchemaKeys.GLUING_INVS);

		JSONArray symbols = null;
		if (refStepJSON.has(SchemaKeys.SYMBOLS)) {
			symbols = refStepJSON.getJSONArray(SchemaKeys.SYMBOLS);
		}

		List<String> reqIDList = new ArrayList<>();
		for (int i = 0; i < reqIDs.length(); i++) {
			reqIDList.add(reqIDs.getString(i));
		}

		Map<String, String> gluingInvsMap = new HashMap<>();
		for (int i = 0; i < gluingInvs.length(); i++) {
			JSONObject inv = gluingInvs.getJSONObject(i);
			String key = inv.getString(SchemaKeys.LABEL);
			String val = inv.getString(SchemaKeys.INV);
			gluingInvsMap.put(key, val);
		}

		List<String> symbolList = new ArrayList<>();
		if (symbols != null) {
			for (int i = 0; i < symbols.length(); i++) {
				symbolList.add(symbols.getString(i));
			}
		}

		return new RefinementStep(refNo, reqIDList, modelDesc, gluingInvsMap, systemReqs, symbolList);
	}

}
