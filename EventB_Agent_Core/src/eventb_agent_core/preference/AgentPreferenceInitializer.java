package eventb_agent_core.preference;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import eventb_agent_core.utils.Constants;

public class AgentPreferenceInitializer extends AbstractPreferenceInitializer {

	public static final String PREF_LLM_KEY = "llm_key";

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		prefs.put(PREF_LLM_KEY, "");
	}

}
