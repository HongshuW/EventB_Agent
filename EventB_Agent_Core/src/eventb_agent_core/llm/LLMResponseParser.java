package eventb_agent_core.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_core.utils.ParserUtils;

public abstract class LLMResponseParser {

	public abstract String getResponseString(String response);

	public JSONObject getResponseContent(String response) {
		String answer = getResponseString(response);
		return new JSONObject(answer);
	}

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

	/* helper methods */

	private List<String> getArrayOfStrings(JSONObject json, String key) {
		JSONArray array = json.getJSONArray(key);
		List<String> results = new ArrayList<>();
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

}
