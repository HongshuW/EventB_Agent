package eventb_agent_core.llminteractor;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IAxiom;
import org.eventb.core.IContextRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.internal.core.pm.ProofManager;
import org.json.JSONObject;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.proof.Hypothesis;
import eventb_agent_core.utils.RetrieveModelUtils;
import eventb_agent_core.utils.llm.ParserUtils;
import eventb_agent_core.utils.proof.ProofTreeUtils;

public class POFixer extends AbstractLLMInteractor {

	public POFixer(LLMRequestSender llmRequestSender, LLMResponseParser llmResponseParser) {
		super(llmRequestSender, llmResponseParser);
	}

	/**
	 * Fix model without pre-defined fix strategies.
	 * 
	 * @param machineRoot
	 * @param poSequent
	 * @return
	 * @throws RodinDBException
	 * @throws IOException
	 */
	public JSONObject autoFixPOWithoutStrategy(IMachineRoot machineRoot, IPOSequent poSequent)
			throws RodinDBException, IOException {
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		IProofAttempt proofAttempt = pc.getProofAttempt(poSequent.getElementName(), "POFixer");
		if (proofAttempt == null) {
			proofAttempt = pc.createProofAttempt(poSequent.getElementName(), "POFixer", null);
		}

		// retrieve information from workspace
		IProofTree tree = proofAttempt.getProofTree();
		IProofTreeNode node = ProofTreeUtils.getLastNodeFromTree(proofAttempt);
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
			return getFixedModel(modelJSON, tree);
		} else {
			System.out.println("Proof tree is null.");
			return null;
		}

	}

	private JSONObject getFixedModel(String modelJSON, IProofTree tree) throws IOException {
		String[] placeHolderContents = new String[] { ParserUtils.reverseLex(modelJSON), tree.toString() };

		String response = llmRequestSender.sendRequest(placeHolderContents, LLMRequestTypes.FIX_PROOF_NO_STRATEGY);
		return llmResponseParser.getResponseContent(response);
	}

	/**
	 * Fix the model based on fix strategy.
	 * 
	 * @param machineRoot
	 * @param poSequent
	 * @throws RodinDBException
	 */
	public void autoFixPO(IMachineRoot machineRoot, IPOSequent poSequent) throws RodinDBException {

		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		IProofAttempt proofAttempt = pc.createProofAttempt(poSequent.getElementName(), "POFixer", null);

		// retrieve information from workspace
		IProofTree tree = proofAttempt.getProofTree();
		IProofTreeNode node = ProofTreeUtils.getLastNodeFromTree(proofAttempt);
		IContextRoot contextRoot = null;
		try {
			contextRoot = RetrieveModelUtils.getContextRoot(tree);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		String poName = proofAttempt.getName();

		String modelJSON = null;
		try {
			modelJSON = RetrieveModelUtils.getModelJSON(machineRoot, contextRoot);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		if (tree != null) {
			try {
				JSONObject answer = selectFixStrategy(modelJSON, tree);
				modifyModel(answer, machineRoot, contextRoot, proofAttempt, node, poName);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("Proof tree is null.");
		}

	}

	private JSONObject selectFixStrategy(String modelJSON, IProofTree tree) throws IOException {
		String[] placeHolderContents = new String[] { ParserUtils.reverseLex(modelJSON), tree.toString() };

		// TODO: get LLMRequestType from fix strategy
		String response = llmRequestSender.sendRequest(placeHolderContents, LLMRequestTypes.FIX_PROOF);
		return llmResponseParser.getResponseWithTools(response);
	}

	private void modifyModel(JSONObject answer, IMachineRoot machineRoot, IContextRoot contextRoot,
			IProofAttempt proofAttempt, IProofTreeNode node, String poName) throws CoreException {
		// TODO: modify model based on fix strategy
		List<Hypothesis> hypotheses = llmResponseParser.getHypotheses(answer);
		addHypothesesToContext(contextRoot, hypotheses);
		for (Hypothesis hypothesis : hypotheses) {
			String predicate = ParserUtils.lex(hypothesis.getPredicate());
			String[] instantiations = hypothesis.getInstantiations();
			ProofTreeUtils.addHypothesis(proofAttempt, node, poName, machineRoot, predicate, instantiations);
			ProofTreeUtils.applyPostTacticAndSave(proofAttempt, node, poName, machineRoot);
		}
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
						break;
					}
				}
				if (!axiomExists) {
					// add new axiom
					IAxiom newAxiom = contextRoot.createChild(IAxiom.ELEMENT_TYPE, null, null);
					newAxiom.setLabel(label, null);
					newAxiom.setPredicateString(predicate, null);
				}

				rodinFile.save(null, false);
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}
	}

}
