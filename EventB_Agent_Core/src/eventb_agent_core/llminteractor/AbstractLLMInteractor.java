package eventb_agent_core.llminteractor;

import java.io.IOException;

import org.json.JSONException;
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
			if (response.matches(".*\\\\+u[0-9A-Fa-f]{4}.*")) {
				System.out.println("llm contains invalid characters, try again...");
				System.out.println(response);
				getLLMResponse(placeHolderContents, requestType);
			}
			return llmResponseParser.getResponseContent(response);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			System.out.println("llm returns invalid json, try again...");
			return getLLMResponse(placeHolderContents, requestType);
		}

		return null;
	}

}
