package eventb_agent_ui.handlers;

import java.util.ArrayList;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMInstanceFactory;
import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llminteractor.POFixer;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.proof.FixProofStrategyRunner;
import eventb_agent_core.proof.POManager;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.RetrieveModelUtils;
import eventb_agent_core.utils.proof.ProofUtils;

/**
 * This handler is responsible for manual invocation of agent prover.
 */
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

		POManager poManager = new POManager();
		POFixer poFixer = new POFixer(llmRequestSender, llmResponseParser);

		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			if (first instanceof IProofTreeNode) {

				// select fix strategy
//				ProofStrategySelectionDialog dialog = new ProofStrategySelectionDialog(shell);
//				FixStrategy fixStrategy = FixStrategy.ADD_AXIOM;
//				int dialogCode = dialog.open();
//				if (dialogCode == Window.OK) {
//					fixStrategy = dialog.getSelectedStrategy();
//				} else if (dialogCode == Window.CANCEL) {
//					return null;
//				}

				// retrieve information from workspace
				IProofTreeNode node = (IProofTreeNode) first;
				IProofTree tree = node.getProofTree();

				IMachineRoot machineRoot = RetrieveModelUtils.getMachineRoot(tree);

				IProofAttempt proofAttempt = ProofUtils.getProofAttempt(tree, machineRoot);
				String poName = proofAttempt.getName();

				List<IPOSequent> pos = new ArrayList<>();
				try {
					pos = poManager.getOpenPOs(machineRoot);
				} catch (Exception e) {
					e.printStackTrace();
				}
				IPOSequent undischargedPO = getPO(pos, poName);

				FixProofStrategyRunner fixer = new FixProofStrategyRunner(poName, machineRoot);
				try {
					fixer.runAutoProvers();
				} catch (CoreException e) {
					e.printStackTrace();
				}

				if (undischargedPO != null) {
					try {
						poFixer.autoFixPO(machineRoot, undischargedPO, new ArrayList<>());
					} catch (Exception e) {
						e.printStackTrace();
					}
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

	private IPOSequent getPO(List<IPOSequent> pos, String poName) {
		for (IPOSequent po : pos) {
			if (poName == null) {
				return po;
			}
			String otherPOName = po.getElementName();
			if (otherPOName.equals(poName) || otherPOName == poName) {
				return po;
			}
		}
		return null;
	}

}
