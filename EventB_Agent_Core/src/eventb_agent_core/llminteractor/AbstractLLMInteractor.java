package eventb_agent_core.llminteractor;

import java.io.IOException;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.json.JSONException;
import org.json.JSONObject;

import eventb_agent_core.evaluation.ComponentType;
import eventb_agent_core.evaluation.EvaluationManager;
import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;

public abstract class AbstractLLMInteractor {

	protected LLMRequestSender llmRequestSender;
	protected LLMResponseParser llmResponseParser;

	private int maxAttemptsSynth;
	private int maxAttemptsProof;

	public AbstractLLMInteractor(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		this.llmRequestSender = llmRequestSender;
		this.llmResponseParser = llmResponseParser;

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		maxAttemptsSynth = Integer.valueOf(prefs.get(AgentPreferenceInitializer.PREF_MAX_ATTEMPTS_SYNTH, "5"));
		maxAttemptsProof = Integer.valueOf(prefs.get(AgentPreferenceInitializer.PREF_MAX_ATTEMPTS_PROOF, "1"));
	}

	public JSONObject getLLMResponse(String[] placeHolderContents, LLMRequestTypes requestType)
			throws ReachMaxAttemptException {
		return getLLMResponse(placeHolderContents, requestType, false);
	}

	public JSONObject getLLMResponseWithTools(String[] placeHolderContents, LLMRequestTypes requestType)
			throws ReachMaxAttemptException {
		return getLLMResponse(placeHolderContents, requestType, true);
	}

	private JSONObject getLLMResponse(String[] placeHolderContents, LLMRequestTypes requestType, boolean useTools)
			throws ReachMaxAttemptException {
		String response;
		try {
			response = llmRequestSender.sendRequest(placeHolderContents, requestType);
			System.out.println(response);
			if (response.matches(".*\\\\+u[0-9A-Fa-f]{4}.*") || containsInvalidXmlChar(response)) {
				String message = "llm contains invalid characters, try again...";
				System.out.println(message);
//				System.out.println(response);

				EvaluationManager.setErrorToLatestAction(message);
				updateTokenCount(response);
				reattemptAction();
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
			String message = "llm returns invalid json, try again...";
			System.out.println(message);

			// token count updated in try block
			EvaluationManager.setErrorToLatestAction(message);
			reattemptAction();
			return getLLMResponse(placeHolderContents, requestType, useTools);
		}

		return null;
	}

	private void updateTokenCount(String response) {
		long tokens = llmResponseParser.getTokens(response);
		EvaluationManager.addTokensToLatestAction(tokens);
	}

	private void reattemptAction() throws ReachMaxAttemptException {
		EvaluationManager.endLatestAction();
		ComponentType type = EvaluationManager.getComponentTypeFromLatestAction();
		String poName = null;

		try {
			if (type == ComponentType.FIX_PROOF) {
				poName = EvaluationManager.getPONameFromLatestProofAction();
				EvaluationManager.repeatAndStartPrevoiusAction(maxAttemptsProof);
			} else {
				EvaluationManager.repeatAndStartPrevoiusAction(maxAttemptsSynth);
			}
		} catch (ReachMaxAttemptException e) {
			System.out.println(e.getMessage());
			EvaluationManager.setErrorToLatestAction(e.getMessage());
			throw new ReachMaxAttemptException(e.componentName, poName);
		}
	}

	public boolean containsInvalidXmlChar(String input) {
		for (int i = 0; i < input.length(); i++) {
			int codePoint = input.codePointAt(i);
			if (!isValidXmlChar(codePoint)) {
				return true;
			}
			if (Character.isSupplementaryCodePoint(codePoint)) {
				i++;
			}
		}
		return false;
	}

	private boolean isValidXmlChar(int codePoint) {
		return (codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD
				|| (codePoint >= 0x20 && codePoint <= 0xD7FF)
				|| (codePoint >= 0xE000 && codePoint <= 0xFFFD) || (codePoint >= 0x10000 && codePoint <= 0x10FFFF));
	}

}
