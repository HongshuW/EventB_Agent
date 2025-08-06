package eventb_agent_core.llm.claude;

import org.json.JSONObject;

import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.utils.llm.ParserUtils;

public class ClaudeResponseParser extends LLMResponseParser {

	@Override
	public String getResponseString(String response) {
		JSONObject obj = new JSONObject(response);
		String answer = obj.getJSONArray("content").getJSONObject(0).getString("text");

		return ParserUtils.addEscape(answer);
	}

	@Override
	public JSONObject getResponseWithTools(String response) {
		// TODO Auto-generated method stub
		return null;
	}

}
