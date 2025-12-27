package eventb_agent_ui.popups;

import static org.rodinp.core.RodinCore.asRodinElement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eventb.core.IEventBProject;
import org.eventb.internal.ui.Pair;
import org.eventb.internal.ui.RodinProjectSelectionDialog;
import org.eventb.internal.ui.UIUtils;
import org.eventb.internal.ui.wizards.EventBProjectValidator;
import org.eventb.ui.EventBUIPlugin;
import org.rodinp.core.IRodinElement;
import org.rodinp.core.IRodinProject;

import eventb_agent_core.refinement.Requirement;
import eventb_agent_core.refinement.RequirementType;
import eventb_agent_core.refinement.SystemRequirements;

/**
 * This page is the UI layout for {@code NewModelWizard}.
 */
public class NewModelWizardPage extends WizardPage {

	// Some text areas.
	private Text projectText;
	EventBProjectValidator projectValidator;

	private String systemDesc;
	private SystemRequirements systemRequirements;
	private List<Pair<Combo, Text>> requirements;
	private Button addRequirementButton;

	// The selection when the wizard is launched.
	private ISelection selection;

	/**
	 * Constructor for NewComponentWizardPage.
	 * <p>
	 * 
	 * @param selection The selection when the wizard is launched
	 */
	public NewModelWizardPage(ISelection selection) {
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

		/* Requirements */
		Label requirementsLabel = new Label(composite, SWT.NULL);
		requirementsLabel.setText("Requirements of the System:");
		GridData reqsGD = new GridData(GridData.FILL_HORIZONTAL);
		reqsGD.horizontalSpan = 2;
		requirementsLabel.setLayoutData(reqsGD);

		Button browseReqButton = new Button(composite, SWT.PUSH);
		browseReqButton.setText("Browse...");
		browseReqButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleBrowseRequirements(composite, listener);
			}
		});

		requirements = new ArrayList<>();
		addReqButton(composite, listener);

		initialize();
		setControl(composite);
	}

	private void addNewRequirement(Composite composite, TextModifyListener listener, boolean addButton) {
		addNewRequirement(composite, listener, addButton, null, null);
	}

	private void addNewRequirement(Composite composite, TextModifyListener listener, boolean addButton,
			RequirementType selectedType, String reqText) {
		Combo reqTypeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData reqTypeGD = new GridData(SWT.LEFT, SWT.FILL, false, false);
		reqTypeGD.horizontalSpan = 1;
		reqTypeCombo.setLayoutData(reqTypeGD);

		int selectedIndex = 0;
		for (int i = 0; i < RequirementType.values().length; i++) {
			RequirementType reqType = RequirementType.values()[i];
			reqTypeCombo.add(reqType.toString());
			if (selectedType == reqType) {
				selectedIndex = i;
			}
		}

		reqTypeCombo.select(selectedIndex);

		Text requirement = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		requirement.setLayoutData(gd);
		requirement.addModifyListener(listener);

		if (reqText != null) {
			requirement.setText(reqText);
		}

		requirements.add(new Pair<>(reqTypeCombo, requirement));

		if (addButton) {
			addReqButton(composite, listener);
		}
	}

	private void addReqButton(Composite composite, TextModifyListener listener) {
		Button addButton = new Button(composite, SWT.PUSH);
		addButton.setText("+");
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addNewRequirement(composite, listener, true);
				((Button) e.widget).dispose();
				// refresh the composite
				composite.layout(true, true);
				composite.getShell().pack();
			}
		});
		this.addRequirementButton = addButton;
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
		} else if (requirements != null && requirements.size() > 0) {
			// Project is valid, focus on component control
			setFocusAndSelectAll(requirements.get(0).getSecond());
		}
	}

	private void setFocusAndSelectAll(Text text) {
		text.selectAll();
		text.setFocus();
	}

	private void initialize() {

		final IRodinProject project;
		project = getProjectFromSelection();

		if (UIUtils.DEBUG)
			System.out.println("Project " + project);

		if (project != null) {
			projectText.setText(project.getElementName());
		}
	}

	private void buildSystemDescription() {
		SystemRequirements sysReq = new SystemRequirements();
		for (Pair<Combo, Text> pair : requirements) {
			String reqText = pair.getSecond().getText();
			if (reqText == null || reqText.equals("")) {
				continue;
			}
			RequirementType reqType = RequirementType.values()[pair.getFirst().getSelectionIndex()];
			Requirement req = new Requirement(reqType, reqText);
			sysReq.addRequirement(req);
		}

		systemDesc = sysReq.toString();
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

	private void handleBrowseRequirements(Composite composite, TextModifyListener listener) {
		Shell shell = getShell();

		FileDialog dlg = new FileDialog(shell, SWT.OPEN);
		dlg.setText("Select Requirement File");
		dlg.setFilterExtensions(new String[] { "*.json" });
		dlg.setFilterNames(new String[] { "JSON files (*.json)" });

		String selected = dlg.open();
		if (selected != null) {
			Path path = Path.of(selected);
			SystemRequirements requirements = new SystemRequirements(path);
			List<Requirement> reqList = requirements.getRequirements();
			addRequirementButton.dispose();
			for (int i = 0; i < reqList.size(); i++) {
				Requirement req = reqList.get(i);
				addNewRequirement(composite, listener, i == reqList.size() - 1, req.getRequirementType(),
						req.getRequirementText());
			}
			composite.layout(true, true);
			composite.getShell().pack();
			this.systemDesc = requirements.toString();
			this.systemRequirements = requirements;
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
			final String sysDesc = getSystemDesc();
			if (sysDesc.length() == 0) {
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

	public String getSystemDesc() {
		buildSystemDescription();
		return systemDesc;
	}

	public SystemRequirements getSystemReqs() {
		return systemRequirements;
	}

}