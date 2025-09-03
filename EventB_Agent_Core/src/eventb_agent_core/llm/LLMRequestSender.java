package eventb_agent_core.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

import eventb_agent_core.proof.ProofScenarioType;
import eventb_agent_core.utils.llm.ParserUtils;

/**
 * This interface contains necessary method declarations for calling LLMs.
 */
public abstract class LLMRequestSender {

	protected LLMModels modelType;

	public LLMRequestSender(LLMModels modelType) {
		this.modelType = modelType;
	}

	protected abstract String getAPIKey();

	protected abstract String getAPIEndpoint();

	public RequestBuilder getRequestBuilder() {
		return LLMInstanceFactory.getRequestBuilder(modelType);
	}

	private String getRequest(RequestBuilder requestBuilder, String request) throws IOException {
		HttpURLConnection conn = requestBuilder.getURLConnection(getAPIEndpoint(), getAPIKey());

		try (OutputStream os = conn.getOutputStream()) {
			os.write(request.getBytes(StandardCharsets.UTF_8));
			os.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

		int responseCode = conn.getResponseCode();

		InputStreamReader reader;
		if (responseCode == HttpURLConnection.HTTP_OK) {
			reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
		} else {
			reader = new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8);
		}

		try (BufferedReader in = new BufferedReader(reader)) {
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			return response.toString();
		}
	}

	public String sendRequest(String[] contentInPlaceHolders, LLMRequestTypes requestType,
			List<LinkedHashMap<String, Object>> history, ProofScenarioType poType) throws IOException {
		String[] placeHolders = requestType.getPlaceHolders();
		int length = Math.min(contentInPlaceHolders.length, placeHolders.length);
		RequestBuilder requestBuilder = getRequestBuilder();
		String prompt = requestType.getPrompt();
		if (poType != null) {
			prompt += poType.getRules();
		}

		for (int i = 0; i < length; i++) {
			String contentInPlaceHolder = ParserUtils.reverseLex(contentInPlaceHolders[i]);
			prompt = prompt.replace(placeHolders[i], contentInPlaceHolder);
		}

		String request = "";
		if (requestType.isStructuredRequest()) {
			request = requestBuilder.getRequestWithSchema(prompt, requestType);
		} else if (requestType.areToolsEnabled()) {
			request = requestBuilder.getRequestWithTools(prompt, requestType, history);
		} else {
			request = requestBuilder.getRequestPlain(prompt);
		}

		return getRequest(requestBuilder, request);
	}

}
