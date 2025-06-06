package eventb_agent_core.llm;

import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

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

	public String sendRequest(String prompt) throws IOException {
		String endpoint = "https://api.openai.com/v1/chat/completions";
		String payload = "{" + "\"model\": \"gpt-3.5-turbo\"," + "\"messages\": [{\"role\": \"user\", \"content\": \""
				+ prompt.replace("\"", "\\\"") + "\"}]" + "}";

		URL url = new URL(endpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + apiKey);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);

		try (OutputStream os = conn.getOutputStream()) {
			os.write(payload.getBytes());
			os.flush();
		}

		int responseCode = conn.getResponseCode();
		InputStreamReader reader;
		if (responseCode == HttpURLConnection.HTTP_OK) {
			reader = new InputStreamReader(conn.getInputStream());
		} else {
			reader = new InputStreamReader(conn.getErrorStream());
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
