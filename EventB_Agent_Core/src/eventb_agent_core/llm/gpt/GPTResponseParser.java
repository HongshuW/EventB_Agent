package eventb_agent_core.llm.gpt;

import org.json.JSONObject;

import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.utils.Constants;

public class GPTResponseParser extends LLMResponseParser {

	@Override
	public long getTokens(String response) {
		JSONObject obj = new JSONObject(response);
		return (long) obj.getJSONObject("usage").getInt("total_tokens");
	}

	@Override
	public String getResponseString(String response) {
		JSONObject obj = new JSONObject(response);
		return obj.getJSONArray("output").getJSONObject(0).getJSONArray("content").getJSONObject(0).getString("text");
	}

	@Override
	public JSONObject getResponseWithTools(String response) {
		return new JSONObject(response).getJSONArray("output").getJSONObject(0);
	}

}
