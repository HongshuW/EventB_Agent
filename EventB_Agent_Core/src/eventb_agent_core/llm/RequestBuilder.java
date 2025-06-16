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

		JSONArray input = new JSONArray();
		JSONObject requestMessage = new JSONObject();
		requestMessage.put("role", "user");
		JSONArray contentArray = new JSONArray();
		JSONObject content = new JSONObject();
		content.put("type", "input_text");
		content.put("text", prompt);
		contentArray.put(content);
		requestMessage.put("content", contentArray);
		input.put(requestMessage);
		request.put("input", input);

		JSONObject jsonSchema = getSchema();
		JSONObject textFormat = new JSONObject();
		textFormat.put("format", jsonSchema);
		request.put("text", textFormat);
		
		request.put("temperature", 1);
		request.put("top_p", 1);

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
