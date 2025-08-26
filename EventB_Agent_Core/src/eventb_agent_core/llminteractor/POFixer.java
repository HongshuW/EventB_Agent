package eventb_agent_core.llminteractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IAxiom;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.IInvariant;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.internal.core.pm.ProofManager;
import org.eventb.internal.core.seqprover.ReasonerFailure;
import org.json.JSONObject;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.evaluation.EvaluationManager;
import eventb_agent_core.exception.ReachMaxAttemptException;
import eventb_agent_core.llm.LLMRequestSender;
import eventb_agent_core.llm.LLMRequestTypes;
import eventb_agent_core.llm.LLMResponseParser;
import eventb_agent_core.llm.schemas.SchemaKeys;
import eventb_agent_core.preference.AgentPreferenceInitializer;
import eventb_agent_core.proof.FixProofStrategyRunner;
import eventb_agent_core.proof.Hypothesis;
import eventb_agent_core.proof.ProofFixingStrategies;
import eventb_agent_core.utils.Constants;
import eventb_agent_core.utils.RetrieveModelUtils;
import eventb_agent_core.utils.llm.ParserUtils;
import eventb_agent_core.utils.proof.ProofUtils;

public class POFixer extends AbstractLLMInteractor {

	private static final String PO_OWNER_NAME = "POFixer";
	private static String reasonerMessage = null;

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
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, PO_OWNER_NAME);

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
			String[] placeHolderContents = new String[] { ParserUtils.reverseLex(modelJSON), poName,
					ParserUtils.reverseLex(ProofUtils.getProofTreeString(tree), 1) };
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
	 * @param requestHistory TODO
	 * @throws CoreException
	 * @throws ReachMaxAttemptException
	 */
	public void autoFixPO(IMachineRoot machineRoot, IPOSequent poSequent,
			List<LinkedHashMap<String, Object>> requestHistory) throws CoreException {

		String poName = poSequent.getElementName();
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		IProofAttempt proofAttempt = ProofUtils.getProofAttempt(poSequent, machineRoot, PO_OWNER_NAME);

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
				reasonerMessage = null;
				String[] placeHolderContents = new String[] { ParserUtils.reverseLex(modelJSON), poName,
						ParserUtils.reverseLex(ProofUtils.getProofTreeString(tree), 1) };
				JSONObject answer = getLLMResponseWithTools(placeHolderContents, LLMRequestTypes.FIX_PROOF,
						requestHistory);
				modifyModel(answer, machineRoot, contextRoot, poSequent, tree);
				if (!ProofUtils.isDischarged(machineRoot, poName)) {
					EvaluationManager.endLatestAction();
					EvaluationManager.repeatAndStartPrevoiusAction(maxAttemptsProof);
					EvaluationManager.setLastPOActionIndex();
					llmRequestSender.getRequestBuilder().addRequestHistory(
							"The PO is not discharged. The Event-B model and proof tree are updated. What to do next?",
							reasonerMessage, requestHistory, answer);
					autoFixPO(machineRoot, poSequent, requestHistory);
				}
			} catch (CoreException | ReachMaxAttemptException e) {
				System.out.println(e.getMessage());
			}
		} else {
			System.out.println("Proof tree is null.");
		}

	}

	private void modifyModel(JSONObject answer, IMachineRoot machineRoot, IContextRoot contextRoot,
			IPOSequent poSequent, IProofTree tree) throws CoreException {
		String function = answer.getString(Constants.FUNCTION_NAME);
		JSONObject args = new JSONObject(answer.getString(Constants.FUNCTION_ARGS));

		EvaluationManager.setErrorToLatestAction(function + ":" + args.toString());

		ProofFixingStrategies strategy = ProofFixingStrategies.valueOf(function);
		int nodeID = args.getInt(SchemaKeys.NODE_ID);
		FixProofStrategyRunner fixer = new FixProofStrategyRunner(poSequent, machineRoot);

		List<Hypothesis> hypotheses = new ArrayList<>();

		switch (strategy) {
		case applyProofTactic:
			String tacticString = args.getString(SchemaKeys.PROOF_TACTIC);
			ProofFixingStrategies tactic = ProofFixingStrategies.valueOf(tacticString);
			String predicate = ParserUtils.lex(args.getString(SchemaKeys.PRED));
			Object result = fixer.applyProofTactic(predicate, nodeID, tactic);
			if (result instanceof ReasonerFailure) {
				ReasonerFailure fail = (ReasonerFailure) result;
				reasonerMessage = fail.getReason();
			} else {
				fixer.applyPostTactic();
			}
			break;
		case addHypothesesToContext:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.HYP);
			addHypothesesToContext(contextRoot, hypotheses);
			for (Hypothesis hypothesis : hypotheses) {
				String pred = ParserUtils.lex(hypothesis.getPredicate());
				fixer.addHypothesis(pred);
			}
			fixer.applyPostTactic();
			break;
		case strengthenInvariant:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.INV);
			strengthenInvariants(machineRoot, hypotheses);
			fixer.applyPostTactic();
			break;
		case strengthenGuard:
			hypotheses = llmResponseParser.getHypotheses(args, SchemaKeys.GRD);
			String eventName = args.getString(SchemaKeys.EVENT_NAME);
			strengthenGuard(machineRoot, hypotheses, eventName);
			fixer.applyPostTactic();
			break;
		default:
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

	private void strengthenInvariants(IMachineRoot machineRoot, List<Hypothesis> hypotheses) {
		IRodinFile rodinFile = machineRoot.getRodinFile();

		for (int i = 0; i < hypotheses.size(); i++) {
			Hypothesis hyp = hypotheses.get(i);
			String label = hyp.getLabel();
			String predicate = ParserUtils.lex(hyp.getPredicate());
			try {
				IInvariant[] invariants = machineRoot.getChildrenOfType(IInvariant.ELEMENT_TYPE);
				boolean invExists = false;
				for (IInvariant inv : invariants) {
					if (inv.getLabel().equals(label)) {
						// modify the existing invariant
						inv.setPredicateString(predicate, null);
						invExists = true;
						break;
					}
				}
				if (!invExists) {
					// add new invariant
					IInvariant newInv = machineRoot.createChild(IInvariant.ELEMENT_TYPE, null, null);
					newInv.setLabel(label, null);
					newInv.setPredicateString(predicate, null);
				}

				rodinFile.save(null, false);
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}
	}

	private void strengthenGuard(IMachineRoot machineRoot, List<Hypothesis> hypotheses, String eventName)
			throws RodinDBException {
		IRodinFile rodinFile = machineRoot.getRodinFile();

		IEvent targetEvent = null;
		IEvent[] events = machineRoot.getChildrenOfType(IEvent.ELEMENT_TYPE);
		for (IEvent event : events) {
			if (event.getLabel().equals(eventName) || event.getLabel() == eventName) {
				targetEvent = event;
				break;
			}
		}
		if (targetEvent == null) {
			return;
		}

		for (int i = 0; i < hypotheses.size(); i++) {
			Hypothesis hyp = hypotheses.get(i);
			String label = hyp.getLabel();
			String predicate = ParserUtils.lex(hyp.getPredicate());
			try {
				IGuard[] guards = targetEvent.getChildrenOfType(IGuard.ELEMENT_TYPE);
				boolean grdExists = false;
				for (IGuard grd : guards) {
					if (grd.getLabel().equals(label)) {
						// modify the existing guard
						grd.setPredicateString(predicate, null);
						grdExists = true;
						break;
					}
				}
				if (!grdExists) {
					// add new invariant
					IGuard newGrd = targetEvent.createChild(IGuard.ELEMENT_TYPE, null, null);
					newGrd.setLabel(label, null);
					newGrd.setPredicateString(predicate, null);
				}

				rodinFile.save(null, false);
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
		}
	}

}
