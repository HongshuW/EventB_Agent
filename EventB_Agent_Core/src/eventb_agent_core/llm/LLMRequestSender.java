package eventb_agent_core.llm;

import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.json.JSONObject;

import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;

/**
 * This class is responsible for sending requests to LLM and receiving the
 * responses.
 */
public class LLMRequestSender {

	private String apiKey;

	public LLMRequestSender() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		apiKey = prefs.get(AgentPreferenceInitializer.PREF_LLM_KEY, "");
	}

	public String sendRequest(String prompt, String systemDesc) throws IOException {
		String endpoint = Constants.GPT_ENDPOINT;

		prompt = prompt.replace(Constants.SYS_DESC_PLACE_HOLDER, systemDesc);
		RequestBuilder requestBuilder = new RequestBuilder();
		String request = requestBuilder.getRequest(prompt);

		URL url = new URL(endpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + apiKey);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);

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

}
