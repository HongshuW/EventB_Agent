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
		JSONObject json = new JSONObject(response).getJSONArray("output").getJSONObject(0);
		JSONObject output = new JSONObject();
		output.put(Constants.FUNCTION_NAME, json.getString(Constants.FUNCTION_NAME));
		output.put(Constants.FUNCTION_ARGS, new JSONObject(json.getString(Constants.FUNCTION_ARGS)));
		return output;
	}

}
