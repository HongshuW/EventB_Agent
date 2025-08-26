package eventb_agent_core.llm.gpt;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.RequestBuilder;
import eventb_agent_core.utils.Constants;

public class GPTRequestBuilder extends RequestBuilder {

	public GPTRequestBuilder(LLMModels llmModel) {
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
		switch (requestType) {
		case FIX_PROOF:
			return new String[] { "gpt_ae.json", "gpt_ah.json", "gpt_apply_proof_tactic.json", "gpt_dc.json",
					"gpt_instantiation.json", "gpt_strengthen_guard.json", "gpt_strengthen_invariant.json" };
		default:
			return new String[] {};
		}
	}

	@Override
	public String getRequestWithSchema(String prompt, LLMRequestTypes requestType) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> request = new LinkedHashMap<>();

		request.put("model", llmModel.getModelTypeAPI());

		LinkedHashMap<String, Object> requestMessage = new LinkedHashMap<>();
		requestMessage.put("role", "user");
		LinkedHashMap<String, Object> content = new LinkedHashMap<>();
		content.put("type", "input_text");
		content.put("text", prompt);
		requestMessage.put("content", Arrays.asList(content));
		request.put("input", Arrays.asList(requestMessage));

		Map<String, Object> jsonSchema = getSchema(requestType);
		LinkedHashMap<String, Object> textFormat = new LinkedHashMap<>();
		textFormat.put("format", jsonSchema);
		request.put("text", textFormat);

		request.put("temperature", Constants.TEMPERATURE);
		request.put("top_p", Constants.TOP_P);
		request.put("max_output_tokens", Constants.TOKEN_LIMIT);

		String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		return jsonStr;
	}

	@Override
	public String getRequestWithTools(String prompt, LLMRequestTypes requestType,
			List<LinkedHashMap<String, Object>> history) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> request = new LinkedHashMap<>();

		request.put("model", llmModel.getModelTypeAPI());

		LinkedHashMap<String, Object> requestMessage = new LinkedHashMap<>();
		requestMessage.put("role", "user");
		LinkedHashMap<String, Object> content = new LinkedHashMap<>();
		content.put("type", "input_text");
		content.put("text", prompt);
		requestMessage.put("content", Arrays.asList(content));

		List<LinkedHashMap<String, Object>> inputs = new ArrayList<>();
		inputs.addAll(history);
		inputs.add(requestMessage);
		request.put("input", inputs);

		request.put("tools", getFunctionSchemas(requestType));

		request.put("temperature", Constants.TEMPERATURE);
		request.put("top_p", Constants.TOP_P);
		request.put("max_output_tokens", Constants.TOKEN_LIMIT);

		String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		return jsonStr;
	}

	@Override
	public String getRequestPlain(String prompt) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		LinkedHashMap<String, Object> request = new LinkedHashMap<>();

		request.put("model", llmModel.getModelTypeAPI());

		LinkedHashMap<String, Object> requestMessage = new LinkedHashMap<>();
		requestMessage.put("role", "user");
		LinkedHashMap<String, Object> content = new LinkedHashMap<>();
		content.put("type", "input_text");
		content.put("text", prompt);
		requestMessage.put("content", Arrays.asList(content));
		request.put("input", Arrays.asList(requestMessage));

		request.put("temperature", Constants.TEMPERATURE);
		request.put("top_p", Constants.TOP_P);
		request.put("max_output_tokens", Constants.TOKEN_LIMIT);

		String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		return jsonStr;
	}

	@Override
	public HttpURLConnection getURLConnection(String apiEndpoint, String apiKey) throws IOException {
		URL url = new URL(apiEndpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + apiKey);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);
		return conn;
	}

	@Override
	public void addRequestHistory(String prompt, String message, List<LinkedHashMap<String, Object>> history,
			JSONObject functionCall) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			LinkedHashMap<String, Object> functionCallMap = mapper.readValue(functionCall.toString(),
					LinkedHashMap.class);
			history.add(functionCallMap);

			LinkedHashMap<String, Object> functionCallOutputMap = new LinkedHashMap<>();
			functionCallOutputMap.put("type", "function_call_output");
			functionCallOutputMap.put("call_id", functionCall.get("call_id"));
			LinkedHashMap<String, Object> functionCallOutput = new LinkedHashMap<>();
			if (message == null) {
				functionCallOutput.put("status", "success");
			} else {
				functionCallOutput.put("status", "failure");
				functionCallOutput.put("message", message);
			}
			functionCallOutputMap.put("output", mapper.writeValueAsString(functionCallOutput));
			history.add(functionCallOutputMap);

			LinkedHashMap<String, Object> nextMessageMap = new LinkedHashMap();
			nextMessageMap.put("role", "user");
			LinkedHashMap<String, Object> nextMessage = new LinkedHashMap();
			nextMessage.put("type", "input_text");
			nextMessage.put("text", prompt);
			nextMessageMap.put("content", Arrays.asList(nextMessage));
			history.add(nextMessageMap);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

}
