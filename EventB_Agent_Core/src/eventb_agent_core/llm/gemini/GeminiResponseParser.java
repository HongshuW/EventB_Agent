package eventb_agent_core.llm.gemini;

import org.json.JSONObject;

import eventb_agent_core.llm.LLMResponseParser;

public class GeminiResponseParser extends LLMResponseParser {

	@Override
	public JSONObject getResponseContent(String response) {
		JSONObject obj = new JSONObject(response);
		String answer = obj.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
				.getJSONObject(0).getString("text");

		return new JSONObject(answer);
	}

}
