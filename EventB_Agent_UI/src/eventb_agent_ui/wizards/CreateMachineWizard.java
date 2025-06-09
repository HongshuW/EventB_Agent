package eventb_agent_ui.wizards;

import static org.eventb.core.IConfigurationElement.DEFAULT_CONFIGURATION;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eventb.core.IAction;
import org.eventb.core.IAxiom;
import org.eventb.core.ICarrierSet;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IConstant;
import org.eventb.core.IConvergenceElement;
import org.eventb.core.IEvent;
import org.eventb.core.IExtendsContext;
import org.eventb.core.IGuard;
import org.eventb.core.IInvariant;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.eventb.core.IVariant;
import org.eventb.core.IWitness;
import org.eventb.internal.ui.UIUtils;
import org.eventb.internal.ui.utils.Messages;
import org.eventb.ui.EventBUIPlugin;
import org.json.JSONObject;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinDB;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_ui.EventBAgentUIPlugin;

public class CreateMachineWizard extends Wizard implements INewWizard {

	public static final String WIZARD_ID = EventBAgentUIPlugin.AGENT_PLUGIN_ID + ".wizards.CreateMachine";

	// The wizard page.
	private CreateMachineWizardPage page;

	// The selection when the wizard is launched.
	private ISelection selection;

	private LLMResponseParser parser;

	/**
	 * Constructor: This wizard needs a progress monitor.
	 */
	public CreateMachineWizard() {
		super();
		setNeedsProgressMonitor(true);
		parser = new LLMResponseParser();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	@Override
	public void addPages() {
		page = new CreateMachineWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We will
	 * create an operation and run it using wizard as execution context.
	 * <p>
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		final String projectName = page.getProjectName();
		final String prompt = page.getPrompt();

		JSONObject response = getLLMResponse(prompt);

		final String contextFileName = parser.getContextName(response) + "." + page.getContextFileType();
		final String machineFileName = parser.getMachineName(response) + "." + page.getMachineFileType();
		JSONObject contextJSON = parser.getContextJSON(response);
		JSONObject machineJSON = parser.getMachineJSON(response);

		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(projectName, contextFileName, monitor, contextJSON);
					doFinish(projectName, machineFileName, monitor, machineJSON);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			UIUtils.showError(Messages.title_error, realException.getMessage());
			return false;
		}
		return true;
	}

	private JSONObject getLLMResponse(String prompt) {
		LLMRequestSender llmRequestSender = new LLMRequestSender();

		String response;
		try {
			response = llmRequestSender.sendRequest(prompt);
			JSONObject obj = new JSONObject(response);
			String answer = obj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

			JSONObject answerJson = new JSONObject(answer);

			return answerJson;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * The worker method. It will find the project, create the file, and open the
	 * editor on the newly created file.
	 * <p>
	 * 
	 * @param projectName the name of the project
	 * @param fileName    the name of the file
	 * @param monitor     a progress monitor
	 * @throws CoreException a core exception when creating the new file
	 */
	void doFinish(String projectName, final String fileName, IProgressMonitor monitor, JSONObject json)
			throws CoreException {

		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(projectName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Project \"" + projectName + "\" does not exist.");
		}

		IRodinDB db = EventBUIPlugin.getRodinDatabase();
		// Creating a project handle
		final IRodinProject rodinProject = db.getRodinProject(projectName);

		RodinCore.run(new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor pMonitor) throws CoreException {
				final IRodinFile rodinFile = rodinProject.getRodinFile(fileName);
				rodinFile.create(false, pMonitor);
				final IInternalElement rodinRoot = rodinFile.getRoot();
				((IConfigurationElement) rodinRoot).setConfiguration(DEFAULT_CONFIGURATION, pMonitor);
				if (rodinRoot instanceof IMachineRoot) {
					/* machine */
					initiateMachine(rodinRoot, pMonitor, json);
				} else {
					/* context */
					initiateContext(rodinRoot, pMonitor, json);
				}
				rodinFile.save(null, true);
			}

		}, monitor);

		monitor.worked(1);

		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				UIUtils.linkToEventBEditor(rodinProject.getRodinFile(fileName));
			}
		});
		monitor.worked(1);
	}

	private void initiateMachine(IInternalElement rodinRoot, IProgressMonitor pMonitor, JSONObject json)
			throws RodinDBException {

		System.out.println(json);

		// parse response
		List<String> refines = parser.getRefines(json);
		List<String> sees = parser.getSees(json);
		List<String> variables = parser.getVariables(json);
		List<String[]> invariants = parser.getInvariants(json);
		List<String[]> variants = parser.getVariants(json);
		List<Map<String, Object>> events = parser.getEvents(json);

		addRefineMachineChildren(rodinRoot, pMonitor, refines);
		addSeeContextChildren(rodinRoot, pMonitor, sees);
		addVariablesChildren(rodinRoot, pMonitor, variables);
		addInvariantsChildren(rodinRoot, pMonitor, invariants);
		addVariantsChildren(rodinRoot, pMonitor, variants);
		addEventsChildren(rodinRoot, pMonitor, events);

//		// init event
//		final IEvent init = rodinRoot.createChild(IEvent.ELEMENT_TYPE, null, pMonitor);
//		init.setLabel(IEvent.INITIALISATION, pMonitor);
//		init.setConvergence(IConvergenceElement.Convergence.ORDINARY, pMonitor);
//		init.setExtended(false, pMonitor);
	}

	private void addRefineMachineChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> refines) throws RodinDBException {
		for (String refIdentifier : refines) {
			IRefinesMachine refinedMachine = internalElement.createChild(IRefinesMachine.ELEMENT_TYPE, null, pMonitor);
			refinedMachine.setAbstractMachineName(refIdentifier, pMonitor);
		}
	}

	private void addSeeContextChildren(IInternalElement internalElement, IProgressMonitor pMonitor, List<String> sees)
			throws RodinDBException {
		for (String seeIdentifer : sees) {
			ISeesContext seenContext = internalElement.createChild(ISeesContext.ELEMENT_TYPE, null, pMonitor);
			seenContext.setSeenContextName(seeIdentifer, pMonitor);
		}
	}

	private void addVariablesChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> variables) throws RodinDBException {
		for (String varIndentifier : variables) {
			IVariable variable = internalElement.createChild(IVariable.ELEMENT_TYPE, null, pMonitor);
			variable.setIdentifierString(varIndentifier, pMonitor);
		}
	}

	private void addInvariantsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String[]> invariants) throws RodinDBException {
		for (String[] labeledInvariant : invariants) {
			IInvariant invariant = internalElement.createChild(IInvariant.ELEMENT_TYPE, null, pMonitor);
			invariant.setLabel(labeledInvariant[0], pMonitor);
			invariant.setPredicateString(labeledInvariant[1], pMonitor);
		}
	}

	private void addVariantsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String[]> variants) throws RodinDBException {
		for (String[] labeledVariant : variants) {
			IVariant variant = internalElement.createChild(IVariant.ELEMENT_TYPE, null, pMonitor);
			variant.setLabel(labeledVariant[0], pMonitor);
			variant.setExpressionString(labeledVariant[1], pMonitor);
		}
	}

	/* helper methods for events */

	private void addEventsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<Map<String, Object>> events) throws RodinDBException {
		for (Map<String, Object> eventInfo : events) {
			IEvent event = internalElement.createChild(IEvent.ELEMENT_TYPE, null, pMonitor);
			for (String key : eventInfo.keySet()) {
				if (key.equals(SchemaKeys.EVENT_NAME))
					event.setLabel((String) eventInfo.get(key), pMonitor);
				if (key.equals(SchemaKeys.REFINES))
					addRefinesEventsChildren(event, pMonitor, (List<String>) eventInfo.get(key));
				if (key.equals(SchemaKeys.ANY))
					addVariablesChildren(event, pMonitor, (List<String>) eventInfo.get(key));
				if (key.equals(SchemaKeys.WHERE))
					addGuardsChildren(event, pMonitor, (List<String[]>) eventInfo.get(key));
				if (key.equals(SchemaKeys.WITH))
					addWitnessesChildren(event, pMonitor, (List<String[]>) eventInfo.get(key));
				if (key.equals(SchemaKeys.THEN))
					addActionsChildren(event, pMonitor, (List<String[]>) eventInfo.get(key));
			}
			event.setConvergence(IConvergenceElement.Convergence.ORDINARY, pMonitor);
			event.setExtended(false, pMonitor);
		}
	}

	private void addRefinesEventsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> refines) throws RodinDBException {
		for (String refIdentifier : refines) {
			IRefinesEvent refinedEvent = internalElement.createChild(IRefinesEvent.ELEMENT_TYPE, null, pMonitor);
			refinedEvent.setAbstractEventLabel(refIdentifier, pMonitor);
		}
	}

	private void addGuardsChildren(IInternalElement internalElement, IProgressMonitor pMonitor, List<String[]> guards)
			throws RodinDBException {
		for (String[] guard : guards) {
			IGuard labeledGuard = internalElement.createChild(IGuard.ELEMENT_TYPE, null, pMonitor);
			labeledGuard.setLabel(guard[0], pMonitor);
			labeledGuard.setPredicateString(guard[1], pMonitor);
		}
	}

	private void addWitnessesChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String[]> witnesses) throws RodinDBException {
		for (String[] witness : witnesses) {
			IWitness labeledWitness = internalElement.createChild(IWitness.ELEMENT_TYPE, null, pMonitor);
			labeledWitness.setLabel(witness[0], pMonitor);
			labeledWitness.setPredicateString(witness[1], pMonitor);
		}
	}

	private void addActionsChildren(IInternalElement internalElement, IProgressMonitor pMonitor, List<String[]> actions)
			throws RodinDBException {
		for (String[] action : actions) {
			IAction labeledAction = internalElement.createChild(IAction.ELEMENT_TYPE, null, pMonitor);
			labeledAction.setLabel(action[0], pMonitor);
			labeledAction.setAssignmentString(action[1], pMonitor);
		}
	}

	private void initiateContext(IInternalElement rodinRoot, IProgressMonitor pMonitor, JSONObject json)
			throws RodinDBException {

		System.out.println(json);

		// parse response
		List<String> extendedContexts = parser.getExtends(json);
		List<String> sets = parser.getSets(json);
		List<String> constants = parser.getConstants(json);
		List<String[]> axioms = parser.getAxioms(json);

		addExtendsChildren(rodinRoot, pMonitor, extendedContexts);
		addSetsChildren(rodinRoot, pMonitor, sets);
		addConstantsChildren(rodinRoot, pMonitor, constants);
		addAxiomsChildren(rodinRoot, pMonitor, axioms);
	}

	private void addExtendsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> extendedContexts) throws RodinDBException {
		for (String contextIndentifier : extendedContexts) {
			IExtendsContext extendedContext = internalElement.createChild(IExtendsContext.ELEMENT_TYPE, null, pMonitor);
			extendedContext.setAbstractContextName(contextIndentifier, pMonitor);
		}
	}

	private void addSetsChildren(IInternalElement internalElement, IProgressMonitor pMonitor, List<String> sets)
			throws RodinDBException {
		for (String setIdentifier : sets) {
			ICarrierSet set = internalElement.createChild(ICarrierSet.ELEMENT_TYPE, null, pMonitor);
			set.setIdentifierString(setIdentifier, pMonitor);
		}
	}

	private void addConstantsChildren(IInternalElement internalElement, IProgressMonitor pMonitor,
			List<String> constants) throws RodinDBException {
		for (String constIdentifier : constants) {
			IConstant constant = internalElement.createChild(IConstant.ELEMENT_TYPE, null, pMonitor);
			constant.setIdentifierString(constIdentifier, pMonitor);
		}
	}

	private void addAxiomsChildren(IInternalElement internalElement, IProgressMonitor pMonitor, List<String[]> axioms)
			throws RodinDBException {
		for (String[] labeledAxiom : axioms) {
			IAxiom axiom = internalElement.createChild(IAxiom.ELEMENT_TYPE, null, pMonitor);
			axiom.setLabel(labeledAxiom[0], pMonitor);
			axiom.setPredicateString(labeledAxiom[1], pMonitor);
		}
	}

	/**
	 * Throw a Core exception.
	 * <p>
	 * 
	 * @param message The message for displaying
	 * @throws CoreException a Core exception with the status contains the input
	 *                       message
	 */
	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "org.eventb.internal.ui", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize
	 * from it.
	 * <p>
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 *      org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection sel) {
		this.selection = sel;
	}

}
