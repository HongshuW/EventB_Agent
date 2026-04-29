package eventb_agent_core.llm.claude;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.RequestBuilder;
import eventb_agent_core.utils.Constants;

public class ClaudeRequestBuilder extends RequestBuilder {

	public ClaudeRequestBuilder(LLMModels llmModel) {
		super(llmModel);
	}

	@Override
	protected String getSchemaFileNameFromType(LLMRequestTypes requestType) {
		switch (requestType) {
		case REFINE_STRATEGY:
			return "gpt_refine_strategy_schema.json";
		case SYNTHESIS:
			return "gpt_eventb_base_schema.json";
		case FIX_COMPILATION_ERRS:
			return "gpt_eventb_schema.json";
		case REFINE_MODEL:
			return "gpt_eventb_schema.json";
		case MODEL_CHECKING_PARAMS:
			return "gpt_model_checking_schema.json";
		case FIX_MODEL_CHECKING:
			return "gpt_eventb_schema.json";
		case FIX_PROOF_NO_STRATEGY:
			return "gpt_eventb_schema.json";
		default:
			return "gpt_eventb_schema.json";
		}
	}

	@Override
	protected String[] getFunctionFileNamesFromType(LLMRequestTypes requestType) {
		// TODO: implement this later
		return null;
	}

	@Override
	public String getRequestWithSchema(String prompt, LLMRequestTypes requestType) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> request = new LinkedHashMap<>();

		request.put("model", llmModel.getModelTypeAPI());
		request.put("max_tokens", Constants.TOKEN_LIMIT);

		LinkedHashMap<String, Object> requestMessage = new LinkedHashMap<>();
		requestMessage.put("role", "user");
		requestMessage.put("content", prompt);
		request.put("messages", Arrays.asList(requestMessage));

		LinkedHashMap<String, Object> outputConfig = new LinkedHashMap<>();
		Map<String, Object> jsonSchema = getSchema(requestType);
		jsonSchema.remove("name");
		jsonSchema.remove("strict");
		outputConfig.put("format", jsonSchema);
		request.put("output_config", outputConfig);

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
		URL url = new URL(apiEndpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("x-api-key", apiKey);
		conn.setRequestProperty("anthropic-version", Constants.ANTHROPIC_VERSION);
		conn.setRequestProperty("content-type", "application/json");
		conn.setDoOutput(true);
		return conn;
	}

	@Override
	public void addRequestHistory(String prompt, String message, List<LinkedHashMap<String, Object>> history,
			JSONObject functionCall) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addReasoningHistory(List<LinkedHashMap<String, Object>> history, JSONObject reasoning) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getRequestWithFileInput(String prompt, Path inputFilePath, String fileID, LLMRequestTypes requestType)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestUploadFile(Path inputFilePath) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestWithSimplifiedPrompt(String prompt, LLMRequestTypes requestType) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
