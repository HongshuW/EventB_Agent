package eventb_agent_ui.handlers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eventb.core.EventBPlugin;
import org.eventb.core.IAxiom;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.ITactic;
import org.eventb.core.seqprover.eventbExtensions.Tactics;
import org.eventb.internal.core.pm.ProofManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rodinp.core.IRodinElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.llm.LLMInstanceFactory;
import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.strategies.FixStrategy;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.FileUtils;
import eventb_agent_ui.logging.AgentLogger;
import eventb_agent_ui.popups.ProofStrategySelectionDialog;
import eventb_agent_ui.utils.RetrieveModelUtils;

public class AgentProverHandler extends AbstractHandler implements IHandler {

	private LLMRequestSender llmRequestSender;
	private LLMResponseParser llmResponseParser;
	private String prompt;

	public AgentProverHandler() {
		super();

		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(Constants.PREF_NODE_ID);
		LLMModels modelType = LLMModels
				.getLLMModel(prefs.get(AgentPreferenceInitializer.PREF_LLM_MODEL, Constants.DEFAULT_MODEL));
		llmRequestSender = LLMInstanceFactory.getRequestSender(modelType);
		llmResponseParser = LLMInstanceFactory.getResponseParser(modelType);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			if (first instanceof IProofTreeNode) {

				// select fix strategy
				ProofStrategySelectionDialog dialog = new ProofStrategySelectionDialog(shell);
				FixStrategy fixStrategy = FixStrategy.ADD_AXIOM;
				if (dialog.open() == Window.OK) {
					fixStrategy = dialog.getSelectedStrategy();
				}

				// update prompt
				readPrompt(fixStrategy);

				// retrieve information from workspace
				IProofTreeNode node = (IProofTreeNode) first;
				IProofTree tree = node.getProofTree();

				IMachineRoot machineRoot = RetrieveModelUtils.getMachineRoot(tree);
				IContextRoot contextRoot = null;
				try {
					contextRoot = RetrieveModelUtils.getContextRoot(tree);
				} catch (RodinDBException e) {
					e.printStackTrace();
				}

				IProofAttempt proofAttempt = getProofAttempt(tree, machineRoot);
				String poName = proofAttempt.getName();

				String modelJSON = null;
				try {
					modelJSON = RetrieveModelUtils.getModelJSON(machineRoot, contextRoot);
				} catch (RodinDBException e) {
					e.printStackTrace();
				}

				if (tree != null) {
					String response;
					try {
						String[] placeHolderContents = new String[] { modelJSON, tree.toString() };
						LLMRequestTypes[] requestTypes = new LLMRequestTypes[] { LLMRequestTypes.RETRIEVE_MODEL,
								LLMRequestTypes.FIX_PROOF };
						response = llmRequestSender.sendRequest(prompt, placeHolderContents, requestTypes);
						response = llmResponseParser.getResponseString(response);

						JSONObject answer = new JSONObject(response);
						String explanation = llmResponseParser.getExplanation(answer);

						// display explanation
						String message = "Fix Strategy:\n" + fixStrategy.toString() + "\n\nModification:\n"
								+ explanation;
						AgentLogger.log(message);
						MessageDialog messageDialog = new MessageDialog(shell, "Agent Prover", null, message, 0, 0,
								"confirm");
						messageDialog.open();

						JSONArray modificationJSONArray = llmResponseParser.getModificationJSONArray(answer);

						String[] predicates = modifyContext(contextRoot, modificationJSONArray);
						for (String predicate : predicates) {
							addHypothesisToProofTree(proofAttempt, predicate, poName, machineRoot);
						}

					} catch (IOException | CoreException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Proof tree is null.");
				}
			} else {
				MessageDialog dialog = new MessageDialog(shell, "Agent Prover", null,
						"The Agent Prover is designed to fix a proof tree.\n"
								+ "Please select a node in the proof tree to proceed.",
						0, 0, "OK");
				dialog.open();
			}
		}

		return null;
	}

	private void readPrompt(FixStrategy fixStrategy) {
		Path promptPath = fixStrategy.getPromptPath();
		prompt = FileUtils.readText(promptPath);
	}

	private String[] modifyContext(IContextRoot contextRoot, JSONArray modificationJSONArray) throws CoreException {
		IRodinFile rodinFile = contextRoot.getRodinFile();
		String[] predicates = new String[modificationJSONArray.length()];

		for (int i = 0; i < modificationJSONArray.length(); i++) {
			JSONObject axiom = modificationJSONArray.getJSONObject(i);
			String label = axiom.getString(SchemaKeys.LABEL);
			String predicate = axiom.getString(SchemaKeys.PRED);
			try {
				IAxiom[] axioms = contextRoot.getChildrenOfType(IAxiom.ELEMENT_TYPE);
				boolean axiomExists = false;
				for (IAxiom a : axioms) {
					if (a.getLabel().equals(label)) {
						// modify the existing axiom
						a.setPredicateString(predicate, null);
						axiomExists = true;
						selectModification(a, rodinFile);
						break;
					}
				}
				if (!axiomExists) {
					// add new axiom
					IAxiom newAxiom = contextRoot.createChild(IAxiom.ELEMENT_TYPE, null, null);
					newAxiom.setLabel(label, null);
					newAxiom.setPredicateString(predicate, null);
					selectModification(newAxiom, rodinFile);
				}

				rodinFile.save(null, false);
				predicates[i] = predicate;
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}

		return predicates;
	}

	private void addHypothesisToProofTree(IProofAttempt proofAttempt, String hypothesis, String poName,
			IEventBRoot eventbRoot) throws RodinDBException {

		ITactic insertLemmaTactic = Tactics.insertLemma(hypothesis);
		if (proofAttempt.isDisposed()) {
			proofAttempt = getProofAttemptByPOName(poName, eventbRoot);
		}
		IProofTreeNode node = getLastNode(proofAttempt);

		insertLemmaTactic.apply(node, null);
		proofAttempt.commit(true, true, null);

		ITactic basicTactics = EventBPlugin.getAutoPostTacticManager().getSelectedPostTactics(eventbRoot);
		basicTactics.apply(node, null);
	}

	private void selectModification(IRodinElement element, IRodinFile rodinFile) {
		Display.getDefault().asyncExec(() -> {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			try {
				// Open the file
				IEditorPart part = IDE.openEditor(page, rodinFile.getResource(), true);
				// Select element
				ISelectionProvider provider = part.getSite().getSelectionProvider();
				if (provider != null) {
					provider.setSelection(new StructuredSelection(element));
				}
			} catch (PartInitException e) {
				e.printStackTrace();
			}

		});
	}

	private void selectPage(IRodinFile rodinFile) {
		Display.getDefault().asyncExec(() -> {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			try {
				// Open the file
				IDE.openEditor(page, rodinFile.getResource(), true);
			} catch (PartInitException e) {
				e.printStackTrace();
			}

		});
	}

	private IProofAttempt getProofAttempt(IProofTree tree, IEventBRoot root) {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(root);
		for (IProofAttempt proofAttempt : proofComponent.getProofAttempts()) {
			if (proofAttempt.isDisposed()) {
				continue;
			}
			if (tree == proofAttempt.getProofTree()) {
				return proofAttempt;
			}
		}
		return null;
	}

	private IProofAttempt getProofAttemptByPOName(String poName, IEventBRoot root) {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(root);
		for (IProofAttempt proofAttempt : proofComponent.getProofAttempts()) {
			if (proofAttempt.isDisposed()) {
				continue;
			}
			if (poName.equals(proofAttempt.getName())) {
				return proofAttempt;
			}
		}
		return null;
	}

	private IProofTreeNode getLastNode(IProofAttempt attempt) {
		IProofTree tree = attempt.getProofTree();
		if (tree == null || attempt.isDisposed()) {
			return null;
		}

		IProofTreeNode node = tree.getRoot();
		while (true) {
			IProofTreeNode[] kids = node.getChildNodes();
			if (kids.length == 0) {
				return node;
			}
			node = kids[kids.length - 1];
		}
	}

}
