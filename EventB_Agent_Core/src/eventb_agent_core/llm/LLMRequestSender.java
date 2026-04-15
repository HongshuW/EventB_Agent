package eventb_agent_core.llm;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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

	protected abstract String getFileUploadAPIEndpoint();

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

	private String getFileUploadRequest(String filePath) throws IOException {
		File pdf = new File(filePath);

		RequestBody fileBody = RequestBody.create(pdf, MediaType.parse("application/pdf"));

		MultipartBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("purpose", "assistants").addFormDataPart("file", pdf.getName(), fileBody)
				.addFormDataPart("expires_after[anchor]", "created_at")
				.addFormDataPart("expires_after[seconds]", "10800") // TODO: change this
				.build();

		Request request = new Request.Builder().url(getFileUploadAPIEndpoint())
				.addHeader("Authorization", "Bearer " + getAPIKey()).post(requestBody).build();

		OkHttpClient client = new OkHttpClient();
		try (Response response = client.newCall(request).execute()) {
			System.out.println("HTTP " + response.code());

			String responseString = response.body() != null ? response.body().string() : "";
			return responseString;
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

	/**
	 * This function uploads a file and sends a request with this file as input.
	 * 
	 * @param inputFilePath
	 * @param requestType
	 * @return
	 * @throws IOException
	 */
	public String sendRequestWithFile(String fileID, String[] contentInPlaceHolders, LLMRequestTypes requestType)
			throws IOException {
		String[] placeHolders = requestType.getSimplifiedPlaceHolders();
		int length = Math.min(contentInPlaceHolders.length, placeHolders.length);
		RequestBuilder requestBuilder = getRequestBuilder();
		String prompt = requestType.getSimplifiedPrompt();

		for (int i = 0; i < length; i++) {
			String contentInPlaceHolder = ParserUtils.reverseLex(contentInPlaceHolders[i]);
			prompt = prompt.replace(placeHolders[i], contentInPlaceHolder);
		}

		String request = requestBuilder.getRequestWithFileInput(prompt, null, fileID, requestType);
		return getRequest(requestBuilder, request);
	}

	public String sendRequestUploadFile(Path inputFilePath) throws IOException {
		return getFileUploadRequest(inputFilePath.toString());
	}

	public String sendRequestWithNewSchema(String[] contentInPlaceHolders, LLMRequestTypes requestType)
			throws IOException {
		String[] placeHolders = requestType.getSimplifiedPlaceHolders();
		int length = Math.min(contentInPlaceHolders.length, placeHolders.length);
		RequestBuilder requestBuilder = getRequestBuilder();
		String prompt = requestType.getSimplifiedPrompt();

		for (int i = 0; i < length; i++) {
			String contentInPlaceHolder = ParserUtils.reverseLex(contentInPlaceHolders[i]);
			prompt = prompt.replace(placeHolders[i], contentInPlaceHolder);
		}

		String request = requestBuilder.getRequestWithSimplifiedPrompt(prompt, requestType);
		return getRequest(requestBuilder, request);
	}
}
