package eventb_agent_ui.handlers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import org.json.JSONObject;

import eventb_agent_core.llm.LLMInstanceFactory;
import eventb_agent_core.llm.LLMModels;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.FileUtils;

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

				if (tree != null) {
					StringBuilder stringBuilder = new StringBuilder();
					traverse(tree.getRoot(), 0, stringBuilder);

					String response;
					try {
						response = llmRequestSender.sendRequest(prompt, stringBuilder.toString(),
								LLMRequestTypes.FIX_PROOF);
						response = llmResponseParser.getResponseString(response);
						System.out.println(response);
					} catch (IOException e) {
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

	private void traverse(IProofTreeNode node, int depth, StringBuilder stringBuilder) {
		String indent = "  ".repeat(depth);
		System.out.println(indent + "- " + node.getSequent());

		stringBuilder.append(indent + "- " + node.getSequent());

		for (IProofTreeNode child : node.getChildNodes()) {
			traverse(child, depth + 1, stringBuilder);
		}
	}

}
