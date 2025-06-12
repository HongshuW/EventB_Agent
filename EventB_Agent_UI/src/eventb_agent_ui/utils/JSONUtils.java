package eventb_agent_ui.utils;

import java.io.FileReader;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONUtils {

	public static JSONObject readJSON(String path) {
		try (FileReader reader = new FileReader(path)) {
			JSONTokener tokener = new JSONTokener(reader);
			JSONObject obj = new JSONObject(tokener);
			return obj;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.json.JSONException e) {
			System.err.println("Invalid JSON format: " + e.getMessage());
		}
		return null;
	}
	
}
