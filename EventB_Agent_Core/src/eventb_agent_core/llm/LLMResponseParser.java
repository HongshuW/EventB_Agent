package eventb_agent_core.llm;

import org.json.JSONObject;

public class LLMResponseParser {

	public LLMResponseParser() {
	}

	public JSONObject getContextJSON(JSONObject json) {
		return json.getJSONObject("context");
	}

	public JSONObject getMachineJSON(JSONObject json) {
		return json.getJSONObject("machine");
	}

	public String getContextName(JSONObject json) {
		JSONObject context = getContextJSON(json);
		return context.getString("CONTEXT");
	}

	public String getMachineName(JSONObject json) {
		JSONObject machine = getMachineJSON(json);
		return machine.getString("MACHINE");
	}

}
