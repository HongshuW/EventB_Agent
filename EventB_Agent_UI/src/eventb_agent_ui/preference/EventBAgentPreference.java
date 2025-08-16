package eventb_agent_ui.preference;

import java.util.stream.Stream;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;

/**
 * This class records the preferences for Event-B Agent Plug-in.
 */
public class EventBAgentPreference extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String LLM_MODEL = AgentPreferenceInitializer.PREF_LLM_MODEL;
	public static final String GPT_KEY = AgentPreferenceInitializer.PREF_GPT_KEY;
	public static final String CLAUDE_KEY = AgentPreferenceInitializer.PREF_CLAUDE_KEY;
	public static final String GEMINI_KEY = AgentPreferenceInitializer.PREF_GEMINI_KEY;
	public static final String DATASET_LOCATION = AgentPreferenceInitializer.PREF_DATASET_LOC;
	public static final String RESULTS_LOCATION = AgentPreferenceInitializer.PREF_RESULTS_LOC;
	public static final String ENABLE_REFINEMENT = AgentPreferenceInitializer.PREF_ENABLE_REF;
	public static final String ENABLE_FIX_STRATEGY = AgentPreferenceInitializer.PREF_ENABLE_FIX;
	public static final String MAX_ATTEMPTS = AgentPreferenceInitializer.PREF_MAX_ATTEMPTS;

	/* llm config */
	private Combo llmModelCombo;
	private Text gptKeyText;
	private Text claudeKeyText;
	private Text geminiKeyText;

	/* experiment config */
	private Text datasetLocationText;
	private Text resultsLocationText;
	private Button enableRefinementButton;
	private Button enableFixStrategyButton;
	private Text maxAttemptsText;

	private String defaultLLMModel = LLMModels.GPT4_1.toString();

	public EventBAgentPreference() {
	}

	public EventBAgentPreference(String title) {
		super(title);
	}

	public EventBAgentPreference(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, Constants.PREF_NODE_ID);
		setPreferenceStore(store);

		this.llmModelCombo = null;
		this.gptKeyText = null;
		this.claudeKeyText = null;
		this.geminiKeyText = null;

		this.datasetLocationText = null;
		this.resultsLocationText = null;
		this.enableRefinementButton = null;
		this.enableFixStrategyButton = null;
		this.maxAttemptsText = null;

		setDescription("Preference Page for Event-B Agent Plug-in.");
		setTitle("Event-B Agent");
		setMessage("Event-B Agent Settings");
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createLLMGroup(composite);
		createExperimentGroup(composite);

		return composite;
	}

	private void createLLMGroup(final Composite parent) {
		String title = "LLM Setting";
		Group llmSettingGroup = initGroup(parent, title);

		String modelSelectionLabel = "LLM Model";
		String modelString = getPreferenceStore().getString(LLM_MODEL);
		if (modelString == null || modelString.equals("")) {
			modelString = defaultLLMModel;
		}
		LLMModels selectedModel = LLMModels.getLLMModel(modelString);
		this.llmModelCombo = createDropDown(llmSettingGroup, modelSelectionLabel, LLMModels.values(),
				selectedModel.ordinal());

		String gptKeyLabel = "GPT Key";
		this.gptKeyText = createText(llmSettingGroup, gptKeyLabel, getPreferenceStore().getString(GPT_KEY));

		String claudeKeyLabel = "Claude Key";
		this.claudeKeyText = createText(llmSettingGroup, claudeKeyLabel, getPreferenceStore().getString(CLAUDE_KEY));

		String geminiKeyLabel = "Gemini Key";
		this.geminiKeyText = createText(llmSettingGroup, geminiKeyLabel, getPreferenceStore().getString(GEMINI_KEY));
	}

	private void createExperimentGroup(final Composite parent) {
		String title = "Experiment Setting";
		Group experimentSettingGroup = initGroup(parent, title);

		String datasetLocationLabel = "Dataset Location";
		this.datasetLocationText = createText(experimentSettingGroup, datasetLocationLabel,
				getPreferenceStore().getString(DATASET_LOCATION));

		String resultsLocationLabel = "Log Location";
		this.resultsLocationText = createText(experimentSettingGroup, resultsLocationLabel,
				getPreferenceStore().getString(RESULTS_LOCATION));

		String enableRefinementLabel = "Enable Refinement";
		this.enableRefinementButton = createButton(experimentSettingGroup, enableRefinementLabel,
				getPreferenceStore().getBoolean(ENABLE_REFINEMENT));

		String enableProofFixingLabel = "Enable Proof Fixing";
		this.enableFixStrategyButton = createButton(experimentSettingGroup, enableProofFixingLabel,
				getPreferenceStore().getBoolean(ENABLE_FIX_STRATEGY));

		String maxAttemptsLabel = "Max Number of Attempts";
		this.maxAttemptsText = createText(experimentSettingGroup, maxAttemptsLabel,
				getPreferenceStore().getString(MAX_ATTEMPTS));
	}

	private Group initGroup(final Composite parent, String title) {
		final Group group = new Group(parent, SWT.NONE);
		group.setText(title);

		GridLayout layout = new GridLayout(2, false);
		group.setLayout(layout);

		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return group;
	}

	private Text createText(Group settingGroup, String textLabel, String textContent) {
		final Label label = new Label(settingGroup, SWT.NONE);
		label.setText(textLabel);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		Text text = new Text(settingGroup, SWT.BORDER);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = convertWidthInCharsToPixels(30);
		text.setLayoutData(gd);
		text.setText(textContent);

		return text;
	}

	private <T extends Enum<T>> Combo createDropDown(Group settingGroup, String textLabel, T[] enumOptions,
			int defaultSelection) {
		final Label label = new Label(settingGroup, SWT.NONE);
		label.setText(textLabel);

		Combo contentCombo = new Combo(settingGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		final String[] selectionNames = Stream.of(enumOptions).map(Enum::toString).toArray(String[]::new);
		contentCombo.setItems(selectionNames);
		contentCombo.select(defaultSelection);

		GridData comboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		comboData.horizontalAlignment = GridData.END;
		contentCombo.setLayoutData(comboData);

		return contentCombo;
	}

	private Button createButton(Group settingGroup, String textLabel, boolean defaultSelection) {
		Button newButton = new Button(settingGroup, SWT.CHECK);
		newButton.setText(textLabel);
		newButton.setSelection(defaultSelection);
		GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1);
		newButton.setLayoutData(gd);

		return newButton;
	}

	@Override
	public boolean performOk() {
		getPreferenceStore().setValue(LLM_MODEL, llmModelCombo.getText());
		getPreferenceStore().setValue(GPT_KEY, gptKeyText.getText());
		getPreferenceStore().setValue(CLAUDE_KEY, claudeKeyText.getText());
		getPreferenceStore().setValue(GEMINI_KEY, geminiKeyText.getText());
		getPreferenceStore().setValue(DATASET_LOCATION, datasetLocationText.getText());
		getPreferenceStore().setValue(RESULTS_LOCATION, resultsLocationText.getText());
		getPreferenceStore().setValue(ENABLE_REFINEMENT, enableRefinementButton.getSelection());
		getPreferenceStore().setValue(ENABLE_FIX_STRATEGY, enableFixStrategyButton.getSelection());
		getPreferenceStore().setValue(MAX_ATTEMPTS, maxAttemptsText.getText());

		try {
			((ScopedPreferenceStore) getPreferenceStore()).save();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	protected void performDefaults() {
		getPreferenceStore().setToDefault(LLM_MODEL);
		LLMModels selectedModel = LLMModels.valueOf(getPreferenceStore().getString(LLM_MODEL));
		llmModelCombo.select(selectedModel.ordinal());

		getPreferenceStore().setToDefault(GPT_KEY);
		gptKeyText.setText(getPreferenceStore().getString(GPT_KEY));

		getPreferenceStore().setToDefault(CLAUDE_KEY);
		claudeKeyText.setText(getPreferenceStore().getString(CLAUDE_KEY));

		getPreferenceStore().setToDefault(GEMINI_KEY);
		geminiKeyText.setText(getPreferenceStore().getString(GEMINI_KEY));

		getPreferenceStore().setToDefault(DATASET_LOCATION);
		datasetLocationText.setText(getPreferenceStore().getString(DATASET_LOCATION));

		getPreferenceStore().setToDefault(RESULTS_LOCATION);
		resultsLocationText.setText(getPreferenceStore().getString(RESULTS_LOCATION));

		getPreferenceStore().setToDefault(ENABLE_REFINEMENT);
		enableRefinementButton.setSelection(getPreferenceStore().getBoolean(ENABLE_REFINEMENT));

		getPreferenceStore().setToDefault(ENABLE_FIX_STRATEGY);
		enableFixStrategyButton.setSelection(getPreferenceStore().getBoolean(ENABLE_FIX_STRATEGY));

		getPreferenceStore().setToDefault(MAX_ATTEMPTS);
		maxAttemptsText.setText(getPreferenceStore().getString(MAX_ATTEMPTS));

		super.performDefaults();
	}

}
