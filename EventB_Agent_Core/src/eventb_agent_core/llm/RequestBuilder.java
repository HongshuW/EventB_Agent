package eventb_agent_core.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import eventb_agent_core.utils.Constants;

/**
 * This class is responsible for generating request.
 */
public class RequestBuilder {

	public RequestBuilder() {
	}

	public JSONObject getRequest(String prompt) throws IOException {
		JSONObject request = new JSONObject();

		request.put("model", Constants.GPT_MODEL);

		JSONArray messages = new JSONArray();
		JSONObject requestMessage = new JSONObject();
		requestMessage.put("role", "user");
		requestMessage.put("content", prompt.replace("\"", "\\\""));
		messages.put(requestMessage);
		request.put("messages", messages);

		JSONObject responseFormat = new JSONObject();
		responseFormat.put("type", "json_schema");
		JSONObject jsonSchema = getSchema();
		responseFormat.put("json_schema", jsonSchema);
		request.put("response_format", responseFormat);

		return request;
	}

	public JSONObject getSchema() throws IOException {
		String jsonText = Files.readString(Paths.get(
				"C:\\Users\\admin\\repos\\EventB_Agent\\EventB_Agent_Core\\src\\eventb_agent_core\\llm\\schemas\\eventb_schema.json"),
				StandardCharsets.UTF_8);
		JSONObject jsonSchema = new JSONObject(jsonText);

		return jsonSchema;
	}

}
