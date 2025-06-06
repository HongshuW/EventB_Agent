package eventb_agent_ui.preference;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;

/**
 * This class records the preferences for Event-B Agent Plug-in.
 */
public class EventBAgentPreference extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String LLM_KEY = AgentPreferenceInitializer.PREF_LLM_KEY;

	private Text llmKeyText;

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
		ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				Constants.PREF_NODE_ID);
		setPreferenceStore(store);

		this.llmKeyText = null;
		setDescription("Preference Page for Event-B Agent Plug-in.");
		setTitle("Event-B Agent");
		setMessage("Enter your LLM Key:");
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);

		composite.setLayout(layout);

		createSettingGroup(composite);

		return composite;
	}

	private void createSettingGroup(final Composite parent) {
		String title = "LLM Setting";
		Group experimentSettingGroup = initGroup(parent, title);

		String llmKeyLabel = "LLM Key";
		this.llmKeyText = createText(experimentSettingGroup, llmKeyLabel, getPreferenceStore().getString(LLM_KEY));
	}

	private Group initGroup(final Composite parent, String title) {
		final Group group = new Group(parent, SWT.NONE);
		group.setText(title);

		GridLayout layout = new GridLayout(2, false);
		group.setLayout(layout);

		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		return group;
	}

	private Text createText(Group settingGroup, String textLabel, String textContent) {
		final Label label = new Label(settingGroup, SWT.NONE);
		label.setText(textLabel);

		Text text = new Text(settingGroup, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		text.setText(textContent);

		GridData textData = new GridData(SWT.FILL, SWT.LEFT, true, false);
		text.setLayoutData(textData);

		return text;
	}

	@Override
	public boolean performOk() {
		getPreferenceStore().setValue(LLM_KEY, llmKeyText.getText());
		try {
			((ScopedPreferenceStore) getPreferenceStore()).save();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	protected void performDefaults() {
		getPreferenceStore().setToDefault(LLM_KEY);
		llmKeyText.setText(getPreferenceStore().getString(LLM_KEY));
		super.performDefaults();
	}

}
