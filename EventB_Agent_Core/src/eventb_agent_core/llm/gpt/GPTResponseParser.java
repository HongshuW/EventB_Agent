package eventb_agent_core.llm.gpt;

import org.json.JSONObject;

import eventb_agent_core.llm.LLMResponseParser;

public class GPTResponseParser extends LLMResponseParser {

	@Override
	public String getResponseString(String response) {
		JSONObject obj = new JSONObject(response);
		return obj.getJSONArray("output").getJSONObject(0).getJSONArray("content").getJSONObject(0).getString("text");
	}

}
