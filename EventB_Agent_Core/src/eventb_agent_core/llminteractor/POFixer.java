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

import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.proof.FixProofStrategyRunner;
import eventb_agent_core.proof.Hypothesis;
import eventb_agent_core.utils.RetrieveModelUtils;
import eventb_agent_core.utils.llm.ParserUtils;
import eventb_agent_core.utils.proof.ProofUtils;

public class POFixer extends AbstractLLMInteractor {

	private static final String PO_OWNER_NAME = "POFixer";

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
	 * @throws ReachMaxAttemptException
	 */
	public JSONObject autoFixPOWithoutStrategy(IMachineRoot machineRoot, IPOSequent poSequent)
			throws RodinDBException, IOException, ReachMaxAttemptException {
		String poName = poSequent.getElementName();
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poName);

		// retrieve information from workspace
		IProofTree tree = proofAttempt.getProofTree();
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
			String[] placeHolderContents = new String[] { ParserUtils.reverseLex(modelJSON), poName, tree.toString() };
			return getLLMResponse(placeHolderContents, LLMRequestTypes.FIX_PROOF_NO_STRATEGY);
		} else {
			System.out.println("Proof tree is null.");
			return null;
		}

	}

	/**
	 * Fix the model based on fix strategy.
	 * 
	 * @param machineRoot
	 * @param poSequent
	 * @throws CoreException 
	 */
	public void autoFixPO(IMachineRoot machineRoot, IPOSequent poSequent) throws CoreException {

		String poName = poSequent.getElementName();
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, poName);

		// retrieve information from workspace
		IProofTree tree = proofAttempt.getProofTree();
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
			try {
				String[] placeHolderContents = new String[] { ParserUtils.reverseLex(modelJSON), poName,
						ParserUtils.reverseLex(tree.toString()) };
				JSONObject answer = getLLMResponseWithTools(placeHolderContents, LLMRequestTypes.FIX_PROOF);
				modifyModel(answer, machineRoot, contextRoot, poSequent);
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (ReachMaxAttemptException e) {
				System.out.println(e.getMessage());
			}
		} else {
			System.out.println("Proof tree is null.");
		}

	}

	private void modifyModel(JSONObject answer, IMachineRoot machineRoot, IContextRoot contextRoot,
			IPOSequent poSequent) throws CoreException {
		// TODO: modify model based on fix strategy
		List<Hypothesis> hypotheses = llmResponseParser.getHypotheses(answer);
		addHypothesesToContext(contextRoot, hypotheses);
		for (Hypothesis hypothesis : hypotheses) {
			String predicate = ParserUtils.lex(hypothesis.getPredicate());
			String[] instantiations = hypothesis.getInstantiations();

			FixProofStrategyRunner fixer = new FixProofStrategyRunner(poSequent, machineRoot, PO_OWNER_NAME);
			fixer.addHypothesis(predicate, instantiations);
			fixer.applyPostTactic();
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
