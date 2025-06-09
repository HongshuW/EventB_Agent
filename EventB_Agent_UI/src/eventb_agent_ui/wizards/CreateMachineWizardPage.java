package eventb_agent_ui.wizards;

import static org.rodinp.core.RodinCore.asRodinElement;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eventb.core.IEventBProject;
import org.eventb.internal.ui.RodinProjectSelectionDialog;
import org.eventb.internal.ui.UIUtils;
import org.eventb.internal.ui.wizards.EventBProjectValidator;
import org.eventb.ui.EventBUIPlugin;
import org.rodinp.core.IRodinElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;

public class CreateMachineWizardPage extends WizardPage {

	// Some text areas.
	private Text projectText;
	EventBProjectValidator projectValidator;

	private Text promptText;

	// The selection when the wizard is launched.
	private ISelection selection;

	/**
	 * Constructor for NewComponentWizardPage.
	 * <p>
	 * 
	 * @param selection The selection when the wizard is launched
	 */
	public CreateMachineWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("Event-B Agent: Create Event-B Context and Machine");
		setDescription(
				"This wizard creates a new pair of Event-B context and machine that can be opened by a multi-page editor.");
		this.selection = selection;
	}

	/**
	 * Creating the components of the dialog.
	 * <p>
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		/* Project Name */
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(composite, SWT.NULL);
		label.setText("&Project:");

		projectText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		projectValidator = new EventBProjectValidator();
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		projectText.setLayoutData(gd);
		final TextModifyListener listener = new TextModifyListener();
		projectText.addModifyListener(listener);

		Button button = new Button(composite, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

		/* Prompt */
		label = new Label(composite, SWT.NULL);
		label.setText("&System Description:");

		promptText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		promptText.setLayoutData(gd);
		promptText.addModifyListener(listener);

		initialize();
		setControl(composite);
	}

	/**
	 * If we want to focus on a control different from the first one, we need to
	 * post the focus event for future processing because the wizard dialog always
	 * focuses on the first control, whatever was done by the page itself.
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible) {
			return;
		}
		if (projectValidator.hasError()) {
			// Focus on project field
			setFocusAndSelectAll(projectText);
		} else {
			// Project is valid, focus on component control
			setFocusAndSelectAll(promptText);
		}
	}

	private void setFocusAndSelectAll(Text text) {
		text.selectAll();
		text.setFocus();
	}

	/**
	 * Tests if the current workbench selection is a suitable project to use.
	 */
	private void initialize() {
		promptText.setText("Generate an Event-B machine.");

		final IRodinProject project;
		project = getProjectFromSelection();

		if (UIUtils.DEBUG)
			System.out.println("Project " + project);

		if (project != null) {
			projectText.setText(project.getElementName());
			promptText.selectAll();
		}
	}

	private IRodinProject getProjectFromSelection() {
		if (!(selection instanceof IStructuredSelection))
			return null;
		final Iterator<?> iter = ((IStructuredSelection) selection).iterator();
		while (iter.hasNext()) {
			final Object obj = iter.next();
			final IRodinElement element = asRodinElement(obj);
			if (element != null) {
				return element.getRodinProject();
			}
		}
		return null;
	}

	/**
	 * Uses the RODIN project selection dialog to choose the new value for the
	 * project field.
	 */
	void handleBrowse() {
		final String projectName = getProjectName();
		IRodinProject rodinProject;
		if (projectName.equals(""))
			rodinProject = null;
		else
			rodinProject = EventBUIPlugin.getRodinDatabase().getRodinProject(projectName);

		RodinProjectSelectionDialog dialog = new RodinProjectSelectionDialog(getShell(), rodinProject, false,
				"Project Selection", "Select a RODIN project");
		if (dialog.open() == RodinProjectSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				projectText.setText(((IRodinProject) result[0]).getElementName());
			}
		}
	}

	class TextModifyListener implements ModifyListener {

		/**
		 * Ensures that both text fields are set correctly.
		 */
		@Override
		public void modifyText(ModifyEvent e) {
			projectValidator.validate(getProjectName());
			if (projectValidator.hasError()) {
				updateStatus(projectValidator.getErrorMessage());
				return;
			}

			final IEventBProject evbProject = projectValidator.getEventBProject();
			final String prompt = getPrompt();
			if (prompt.length() == 0) {
				updateStatus("System description must be specified");
				return;
			}

//			final IRodinFile machineFile = evbProject.getMachineFile(componentName);
//			final IRodinFile contextFile = evbProject.getContextFile(componentName);
//			if (machineFile == null || contextFile == null) {
//				updateStatus("Component name must be valid");
//				return;
//			}
//			if (machineFile.exists()) {
//				updateStatus("There is already a machine with this name");
//				return;
//			}
//			if (contextFile.exists()) {
//				updateStatus("There is already a context with this name");
//				return;
//			}
			updateStatus(null);
		}
	}

	/**
	 * Update the status of this dialog.
	 * <p>
	 * 
	 * @param message A string message
	 */
	void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	/**
	 * Get the name of the project.
	 * <p>
	 * 
	 * @return The name of the project
	 */
	public String getProjectName() {
		return projectText.getText();
	}

	/**
	 * Get the prompt to LLM.
	 * <p>
	 * 
	 * @return The prompt to LLM
	 */
	public String getPrompt() {
		return promptText.getText();
	}

	public String getMachineFileType() {
		return "bum";
	}

	public String getContextFileType() {
		return "buc";
	}

}