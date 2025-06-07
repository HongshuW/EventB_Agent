package eventb_agent_core.llm;

import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class LLMResponseParser {

	public LLMResponseParser() {
	}

	public String getContextString(JSONObject json) {
		StringBuilder contextString = new StringBuilder();

		String contextKey = "CONTEXT";
		String[] keys = new String[] { "EXTENDS", "SETS", "CONSTANTS", "AXIOMS" };

		JSONObject context = json.getJSONObject("context");
		contextString.append(contextKey + "\n\t");
		contextString.append(context.get(contextKey));

		for (String key : keys) {
			parseJSONArray(contextString, context, key, false);
		}

		return contextString.toString();
	}

	public String getMachineString(JSONObject json) {
		StringBuilder machineString = new StringBuilder();

		String machineKey = "MACHINE";
		String[] keys = new String[] { "REFINES", "SEES", "VARIABLES", "INVARIANTS", "VARIANTS", "EVENTS" };

		JSONObject machine = json.getJSONObject("machine");
		machineString.append(machineKey + "\n\t");
		machineString.append(machine.get(machineKey));

		for (String key : keys) {
			if (key.equals("EVENTS")) {
				machineString.append("\nEVENTS");
				machineString.append(parseInitEvent(machine.getJSONObject("INITIALISATION")));
				machineString.append(parseEvents(machine.getJSONArray(key)));
			} else {
				parseJSONArray(machineString, machine, key, false);
			}
		}

		return machineString.toString();
	}

	private void parseJSONArray(StringBuilder stringBuilder, JSONObject context, String key, boolean addTab) {
		JSONArray array = context.getJSONArray(key);

		List<String> predicateKeys = Arrays.asList("REFINES", "ANY", "WHERE", "WITH", "THEN", "AXIOMS", "INVARIANTS",
				"VARIANTS");

		if (array.length() > 0) {
			String tab = addTab ? "\t" : "";
			stringBuilder.append("\n" + tab + key + "\n");
			for (int i = 0; i < array.length(); i++) {
				if (predicateKeys.contains(key)) {
					stringBuilder.append(tab + parseLabeledPredicate(array.getJSONObject(i)));
					if (i < array.length() - 1) {
						stringBuilder.append("\n");
					}
				} else {
					stringBuilder.append("\t" + array.get(i));
					stringBuilder.append("\n");
				}
			}
		}
	}

	private String parseInitEvent(JSONObject initEvent) {
		StringBuilder eventString = new StringBuilder();
		eventString.append("\n\tINITIALISATION:");
		JSONArray array = initEvent.getJSONArray("THEN");
		if (array.length() > 0) {
			eventString.append("\n\tTHEN\n");
			for (int i = 0; i < array.length(); i++) {
				eventString.append("\t" + parseLabeledPredicate(array.getJSONObject(i)));
				eventString.append("\n");
			}
		}
		eventString.append("\tEND\n");
		return eventString.toString();
	}

	private String parseEvents(JSONArray events) {
		StringBuilder eventsString = new StringBuilder();
		if (events.length() > 0) {
			for (int i = 0; i < events.length(); i++) {
				String eventString = parseEvent(events.getJSONObject(i));
				eventsString.append(eventString);
			}
		}
		return eventsString.toString();
	}

	private String parseEvent(JSONObject event) {
		StringBuilder eventString = new StringBuilder();
		String[] keys = new String[] { "REFINES", "ANY", "WHERE", "WITH", "THEN" };

		eventString.append("\n\t");
		eventString.append(event.get("event_name"));
		eventString.append(":");

		for (String key : keys) {
			parseJSONArray(eventString, event, key, true);
		}

		eventString.append("\n\tEND\n");
		return eventString.toString();
	}

	private String parseLabeledPredicate(JSONObject labeledPredicate) {
		String labelName = labeledPredicate.getString("label_name");
		String predicate = labeledPredicate.getString("predicate");

		return "\t" + labelName + ": " + predicate;
	}

}
