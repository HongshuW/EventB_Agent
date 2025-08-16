package eventb_agent_core.llminteractor;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import eventb_agent_core.evaluation.EvaluationManager;
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
		return getLLMResponse(placeHolderContents, requestType, false);
	}

	public JSONObject getLLMResponseWithTools(String[] placeHolderContents, LLMRequestTypes requestType) {
		return getLLMResponse(placeHolderContents, requestType, true);
	}

	private JSONObject getLLMResponse(String[] placeHolderContents, LLMRequestTypes requestType, boolean useTools) {
		String response;
		try {
			response = llmRequestSender.sendRequest(placeHolderContents, requestType);
			if (response.matches(".*\\\\+u[0-9A-Fa-f]{4}.*")) {
				System.out.println("llm contains invalid characters, try again...");
				System.out.println(response);
				updateTokenCount(response);
				getLLMResponse(placeHolderContents, requestType, useTools);
			}
			updateTokenCount(response);
			if (useTools) {
				return llmResponseParser.getResponseWithTools(response);
			} else {
				return llmResponseParser.getResponseContent(response);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			System.out.println("llm returns invalid json, try again...");
			// token count updated in try block
			return getLLMResponse(placeHolderContents, requestType, useTools);
		}

		return null;
	}

	private void updateTokenCount(String response) {
		long tokens = llmResponseParser.getTokens(response);
		EvaluationManager.addTokensToLatestAction(tokens);
	}

}
