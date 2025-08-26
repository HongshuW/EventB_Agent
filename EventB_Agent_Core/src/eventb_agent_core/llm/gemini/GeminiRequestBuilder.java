package eventb_agent_core.llm.gemini;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.RequestBuilder;

public class GeminiRequestBuilder extends RequestBuilder {

	public GeminiRequestBuilder(LLMModels llmModel) {
		super(llmModel);
	}

	@Override
	protected String getSchemaFileNameFromType(LLMRequestTypes requestType) {
		switch (requestType) {
		// TODO: case REFINE_STRATEGY
		// TODO: case FIX_PROOF_NO_STRATEGY
		case SYNTHESIS:
		case REFINE_MODEL:
			return "gemini_eventb_schema.json";
		default:
			return "gemini_eventb_schema.json";
		}
	}

	@Override
	protected String[] getFunctionFileNamesFromType(LLMRequestTypes requestType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestWithSchema(String prompt, LLMRequestTypes requestType) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> request = new LinkedHashMap<>();

		LinkedHashMap<String, Object> content = new LinkedHashMap<>();
		LinkedHashMap<String, Object> requestMessage = new LinkedHashMap<>();
		requestMessage.put("text", prompt);
		content.put("parts", Arrays.asList(requestMessage));
		request.put("contents", Arrays.asList(content));

		Map<String, Object> jsonSchema = getSchema(requestType);
		LinkedHashMap<String, Object> generationConfig = new LinkedHashMap<>();
		generationConfig.put("responseMimeType", "application/json");
		generationConfig.put("responseSchema", jsonSchema);
//		generationConfig.put("temperature", Constants.TEMPERATURE);
//		generationConfig.put("topP", Constants.TOP_P);
//		generationConfig.put("max_output_tokens", Constants.TOKEN_LIMIT * 2);
		request.put("generationConfig", generationConfig);

		String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		return jsonStr;
	}

	@Override
	public String getRequestWithTools(String prompt, LLMRequestTypes requestType,
			List<LinkedHashMap<String, Object>> history) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestPlain(String prompt) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpURLConnection getURLConnection(String apiEndpoint, String apiKey) throws IOException {
		URL url = new URL(apiEndpoint + llmModel.getModelTypeAPI() + ":generateContent?key=" + apiKey);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);
		return conn;
	}

	@Override
	public void addRequestHistory(String prompt, String message, List<LinkedHashMap<String, Object>> history,
			JSONObject functionCall) {
		// TODO Auto-generated method stub
		
	}

}
