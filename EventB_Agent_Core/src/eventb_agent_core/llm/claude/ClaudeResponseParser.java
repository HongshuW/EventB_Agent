package eventb_agent_core.llm.claude;

import org.json.JSONObject;

import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.utils.ParserUtils;

public class ClaudeResponseParser extends LLMResponseParser {

	@Override
	public JSONObject getResponseContent(String response) {
		JSONObject obj = new JSONObject(response);
		String answer = obj.getJSONArray("content").getJSONObject(0).getString("text");

		answer = ParserUtils.addEscape(answer);

		return new JSONObject(answer);
	}

}
