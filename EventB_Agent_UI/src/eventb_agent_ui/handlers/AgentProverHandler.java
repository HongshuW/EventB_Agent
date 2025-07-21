package eventb_agent_ui.handlers;

import java.io.IOException;
import java.util.List;

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
import org.eventb.core.IAxiom;
import org.eventb.core.IContextRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
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
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.proof.FixStrategy;
import eventb_agent_core.proof.Hypothesis;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.llm.ParserUtils;
import eventb_agent_core.utils.proof.ProofTreeUtils;
import eventb_agent_ui.logging.AgentLogger;
import eventb_agent_ui.popups.ProofStrategySelectionDialog;
import eventb_agent_ui.utils.RetrieveModelUtils;

public class AgentProverHandler extends AbstractHandler implements IHandler {

	private LLMRequestSender llmRequestSender;
	private LLMResponseParser llmResponseParser;

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
				int dialogCode = dialog.open();
				if (dialogCode == Window.OK) {
					fixStrategy = dialog.getSelectedStrategy();
				} else if (dialogCode == Window.CANCEL) {
					return null;
				}

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

				IProofAttempt proofAttempt = ProofTreeUtils.getProofAttempt(tree, machineRoot);
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

						// TODO: get LLMRequestType from fix strategy
						LLMRequestTypes[] requestTypes = new LLMRequestTypes[] { LLMRequestTypes.RETRIEVE_MODEL,
								LLMRequestTypes.FIX_PROOF };
						response = llmRequestSender.sendRequest(placeHolderContents, requestTypes);
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

						// TODO: modify model based on fix strategy
						JSONArray modificationJSONArray = llmResponseParser.getModificationJSONArray(answer);
						List<Hypothesis> hypotheses = llmResponseParser.getHypotheses(modificationJSONArray);
						addHypothesesToContext(contextRoot, hypotheses);
						for (Hypothesis hypothesis : hypotheses) {
							String predicate = ParserUtils.lex(hypothesis.getPredicate());
							String[] instantiations = hypothesis.getInstantiations();
							ProofTreeUtils.addHypothesis(proofAttempt, node, poName, machineRoot, predicate,
									instantiations);
							ProofTreeUtils.applyPostTacticAndSave(proofAttempt, node, poName, machineRoot);
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

	private void addHypothesesToContext(IContextRoot contextRoot, List<Hypothesis> hypotheses) throws CoreException {
		IRodinFile rodinFile = contextRoot.getRodinFile();

		for (int i = 0; i < hypotheses.size(); i++) {
			Hypothesis hyp = hypotheses.get(i);
			String label = hyp.getLabel();
			String predicate = ParserUtils.lex(hyp.getPredicate());
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
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}
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

}
