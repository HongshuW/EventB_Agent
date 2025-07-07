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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eventb.core.IAxiom;
import org.eventb.core.IContextRoot;
import org.eventb.core.IMachineRoot;
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
import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.FileUtils;
import eventb_agent_ui.logging.AgentLogger;
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

		Path promptPath = Paths.get(FileUtils.getCoreDirectoryPath(), "src", "eventb_agent_core", "llm", "prompts",
				"fix_proof.txt");
		prompt = FileUtils.readText(promptPath);
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			if (first instanceof IProofTreeNode) {
				IProofTreeNode node = (IProofTreeNode) first;
				IProofTree tree = node.getProofTree();

				IMachineRoot machineRoot = RetrieveModelUtils.getMachineRoot(tree);
				IContextRoot contextRoot = null;
				try {
					contextRoot = RetrieveModelUtils.getContextRoot(tree);
				} catch (RodinDBException e) {
					e.printStackTrace();
				}

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
						AgentLogger.log("Modification:\n" + explanation);
						JSONObject modificationJSON = llmResponseParser.getModificationJSON(answer);

						if (modificationJSON.has("context")) {
							modifyContext(contextRoot, modificationJSON);
						} else if (modificationJSON.has("machine")) {
							JSONArray machineJSONArray = llmResponseParser.getMachineJSONArray(modificationJSON);
						} else if (modificationJSON.has("proof")) {
							JSONArray proofJSONArray = llmResponseParser.getProofJSONArray(modificationJSON);
						}

					} catch (IOException | CoreException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Proof tree is null.");
				}
			} else {
				System.out.println("Selection is not a proof tree node: " + first.getClass().getName());
			}
		}

		return null;
	}

	private void modifyContext(IContextRoot contextRoot, JSONObject modificationJSON) throws CoreException {
		IRodinFile rodinFile = contextRoot.getRodinFile();
		JSONArray contextJSONArray = llmResponseParser.getContextJSONArray(modificationJSON);
		for (int i = 0; i < contextJSONArray.length(); i++) {
			JSONObject axiom = contextJSONArray.getJSONObject(i);
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
