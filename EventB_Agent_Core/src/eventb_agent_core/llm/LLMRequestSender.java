package eventb_agent_core.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import eventb_agent_core.llm.RequestBuilder.SchemaType;

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

	private RequestBuilder getRequestBuilder() {
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

	public String sendRequest(String prompt, String[] contentInPlaceHolders, LLMRequestTypes[] requestTypes)
			throws IOException {
		int length = Math.min(contentInPlaceHolders.length, requestTypes.length);
		RequestBuilder requestBuilder = getRequestBuilder();
		boolean isStructuredRequest = true;
		SchemaType schemaType = null;
		for (int i = 0; i < length; i++) {
			String contentInPlaceHolder = contentInPlaceHolders[i];
			LLMRequestTypes requestType = requestTypes[i];
			prompt = prompt.replace(requestType.getPlaceHolder(), contentInPlaceHolder);
			if (!requestType.isStructuredRequest()) {
				isStructuredRequest = false;
			} else {
				schemaType = requestType.getSchemaType();
			}
		}

		String request = "";
		if (isStructuredRequest) {
			request = requestBuilder.getRequestWithSchema(prompt, schemaType);
		} else {
			request = requestBuilder.getRequestPlain(prompt);
		}

		System.out.println(request);

		return getRequest(requestBuilder, request);
	}

}
