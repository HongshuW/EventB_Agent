package eventb_agent_core.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FileUtils {

	private static final String CORE_FOLDER_NAME = "EventB_Agent_Core";
	private static final String UI_FOLDER_NAME = "EventB_Agent_UI";

	public static String readText(Path path) {
		StringBuilder stringBuilder = new StringBuilder();

		try {
			List<String> lines = Files.readAllLines(path);
			for (String line : lines) {
				stringBuilder.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringBuilder.toString();
	}

	public static JSONObject readJSON(Path path) {
		try (FileReader reader = new FileReader(path.toString())) {
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

	public static Map<String, Object> readOrderedJSON(Path path) {
		ObjectMapper mapper = new ObjectMapper();

		// Read JSON file as generic Map (order preserved)
		Map<String, Object> json = new LinkedHashMap<>();
		;
		try {
			json = (Map<String, Object>) mapper.readValue(path.toFile(), Map.class);
			return json;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return json;
	}

	private static File getAgentDirectory() {
		try {
			File currentClassFile = new File(
					(new FileUtils()).getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
			File agentDir = currentClassFile.getParentFile();
			return agentDir;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getAgentDirectoryPath() {
		return getAgentDirectory().getAbsolutePath();
	}

	public static String getCoreDirectoryPath() {
		File agentDir = getAgentDirectory();
		if (agentDir.isDirectory()) {
			File[] files = agentDir.listFiles();
			for (File file : files) {
				if (file.getName().equals(CORE_FOLDER_NAME)) {
					return file.getAbsolutePath();
				}
			}
		}
		return "";
	}

	public static String getUIDirectoryPath() {
		File agentDir = getAgentDirectory();
		if (agentDir.isDirectory()) {
			File[] files = agentDir.listFiles();
			for (File file : files) {
				if (file.getName().equals(UI_FOLDER_NAME)) {
					return file.getAbsolutePath();
				}
			}
		}
		return "";
	}

}
