package eventb_agent_core.llm.gpt;

import org.json.JSONArray;
import org.json.JSONObject;

import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMResponseParser;

public class GPTResponseParser extends LLMResponseParser {

	public GPTResponseParser(LLMModels llmModel) {
		super(llmModel);
	}

	private boolean isGPT4() {
		return llmModel == LLMModels.GPT4_1 || llmModel == LLMModels.GPT4_1_MINI;
	}

	@Override
	public long getTokens(String response) {
		JSONObject obj = new JSONObject(response);
		return (long) obj.getJSONObject("usage").getInt("total_tokens");
	}

	@Override
	public String getResponseString(String response) {
		JSONObject obj = new JSONObject(response);
		return obj.getJSONArray("output").getJSONObject(isGPT4() ? 0 : 1).getJSONArray("content").getJSONObject(0)
				.getString("text");
	}

	@Override
	public JSONObject getResponseWithTools(String response) {
		JSONObject result = new JSONObject();
		JSONObject responseJSON = new JSONObject(response);

		JSONArray results = responseJSON.getJSONArray("output");
		result.put("result", results);
		return result;
	}

}
