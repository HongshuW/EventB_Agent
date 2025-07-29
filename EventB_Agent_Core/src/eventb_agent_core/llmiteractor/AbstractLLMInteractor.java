package eventb_agent_core.llmiteractor;

import java.io.IOException;

import org.json.JSONObject;

import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;

public abstract class AbstractLLMInteractor {

	protected LLMRequestSender llmRequestSender;
	protected LLMResponseParser llmResponseParser;

	public AbstractLLMInteractor(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		this.llmRequestSender = llmRequestSender;
		this.llmResponseParser = llmResponseParser;
	}

	public JSONObject getLLMResponse(String[] placeHolderContents, LLMRequestTypes requestType) {
		String response;
		try {
			response = llmRequestSender.sendRequest(placeHolderContents, requestType);
			return llmResponseParser.getResponseContent(response);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
