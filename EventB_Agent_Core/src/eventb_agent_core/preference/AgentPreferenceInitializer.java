package eventb_agent_core.preference;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import eventb_agent_core.utils.Constants;

public class AgentPreferenceInitializer extends AbstractPreferenceInitializer {

	public static final String PREF_LLM_MODEL = "llm_model";
	public static final String PREF_GPT_KEY = "gpt_key";
	public static final String PREF_CLAUDE_KEY = "claude_key";
	public static final String PREF_GEMINI_KEY = "gemini_key";

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		prefs.put(PREF_LLM_MODEL, Constants.DEFAULT_MODEL);
		prefs.put(PREF_GPT_KEY, "");
		prefs.put(PREF_CLAUDE_KEY, "");
		prefs.put(PREF_GEMINI_KEY, "");
	}

}
