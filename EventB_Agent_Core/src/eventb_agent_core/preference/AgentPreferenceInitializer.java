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
	public static final String PREF_DATASET_LOC = "dataset_location";
	public static final String PREF_RESULTS_LOC = "results_location";
	public static final String PREF_ENABLE_REF = "enable_refinement";
	public static final String PREF_ENABLE_FIX = "enable_fix_strategy";
	public static final String PREF_MAX_ATTEMPTS_SYNTH = "max_attempts_synth";
	public static final String PREF_MAX_ATTEMPTS_PROOF = "max_attempts_proof";
	public static final String PREF_IS_PDF_INPUT = "is_pdf_input";

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		prefs.put(PREF_LLM_MODEL, Constants.DEFAULT_MODEL);
		prefs.put(PREF_GPT_KEY, "");
		prefs.put(PREF_CLAUDE_KEY, "");
		prefs.put(PREF_GEMINI_KEY, "");
		prefs.put(PREF_DATASET_LOC, "");
		prefs.put(PREF_RESULTS_LOC, "");
		prefs.putBoolean(PREF_ENABLE_REF, true);
		prefs.putBoolean(PREF_ENABLE_FIX, true);
		prefs.putInt(PREF_MAX_ATTEMPTS_SYNTH, 5);
		prefs.putInt(PREF_MAX_ATTEMPTS_PROOF, 5);
		prefs.putBoolean(PREF_IS_PDF_INPUT, false);
	}

}
