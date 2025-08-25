package eventb_agent_core.llminteractor;

import java.io.IOException;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.json.JSONException;
import org.json.JSONObject;

import eventb_agent_core.evaluation.ComponentType;
import eventb_agent_core.evaluation.EvaluationManager;
import eventb_agent_core.exception.InvalidCharacterException;
import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.llm.ParserUtils;

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
			try {
				response = decodeUnicodeEscapes(response);
			} catch (InvalidCharacterException e) {
				System.out.println(e.getMessage());
				EvaluationManager.setErrorToLatestAction(e.getMessage());
				return reattemptDueToInvalidChars(response, placeHolderContents, requestType, useTools);
			}

			System.out.println(response);

			if (containsInvalidXmlChar(response)) {
				String message = "LLM response contains invalid xml character";
				System.out.println(message);
				EvaluationManager.setErrorToLatestAction(message);
				return reattemptDueToInvalidChars(response, placeHolderContents, requestType, useTools);
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

	private JSONObject reattemptDueToInvalidChars(String response, String[] placeHolderContents,
			LLMRequestTypes requestType, boolean useTools) throws ReachMaxAttemptException {
//		System.out.println(response);
		updateTokenCount(response);
		reattemptAction();
		return getLLMResponse(placeHolderContents, requestType, useTools);
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
		return (codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD || (codePoint >= 0x20 && codePoint <= 0xD7FF)
				|| (codePoint >= 0xE000 && codePoint <= 0xFFFD) || (codePoint >= 0x10000 && codePoint <= 0x10FFFF));
	}

	public String decodeUnicodeEscapes(String input) throws InvalidCharacterException {
		StringBuilder result = new StringBuilder();
		int length = input.length();

		for (int i = 0; i < length; i++) {
			char c = input.charAt(i);
			if (c == '\\' && i + 5 < length && input.charAt(i + 1) == 'u') {
				// Extract the next 4 hex digits
				String hex = input.substring(i + 2, i + 6);
				try {
					int codePoint = Integer.parseInt(hex, 16);
					char specialCharacter = (char) codePoint;
					if (isValidChar(specialCharacter)) {
						result.append(specialCharacter);
					} else {
						System.out.println("skip invalid character " + input.substring(i, i + 6));
//						throw new InvalidCharacterException(input.substring(i, i + 6));
					}
					i += 5; // skip \\uXXXX
					continue;
				} catch (NumberFormatException e) {
					// If not valid hex, just keep the literal characters
					result.append(c);
				}
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}

	public boolean isValidChar(char c) {
		return ParserUtils.isValidCharacter(c);
	}

//	public static void main(String[] args) throws InvalidCharacterException {
//		System.out.println(decodeUnicodeEscapes("Hello \\u4F60\\u597D World!"));
//	}

}
