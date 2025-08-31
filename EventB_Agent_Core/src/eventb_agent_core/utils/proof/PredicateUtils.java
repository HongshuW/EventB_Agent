package eventb_agent_core.utils.proof;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.eventb.core.IEventBRoot;
import org.eventb.core.ast.Formula;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.IParseResult;
import org.eventb.core.ast.ISealedTypeEnvironment;
import org.eventb.core.ast.Predicate;
import org.eventb.core.ast.QuantifiedPredicate;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.IProverSequent;

import eventb_agent_core.proof.PredicateWrapper;

public class PredicateUtils {

	public static Predicate parsePredicate(IEventBRoot eventbRoot, String predString) {
		IParseResult parseRes = eventbRoot.getFormulaFactory().parsePredicate(predString, null);

		if (parseRes.hasProblem()) {
			throw new IllegalArgumentException("Invalid predicate syntax: " + parseRes.getProblems().toString());
		}

		return parseRes.getParsedPredicate();
	}

	public static List<PredicateWrapper> getAllPredicates(IProofTreeNode node) {
		IProverSequent sequent = node.getSequent();
		List<PredicateWrapper> result = new ArrayList<>();

		int predicateID = 1;
		Iterator<Predicate> selectedPredicates = sequent.selectedHypIterable().iterator();
		while (selectedPredicates.hasNext()) {
			Predicate predicate = selectedPredicates.next();
			if (predicate != null) {
				result.add(new PredicateWrapper(predicate, predicateID));
				predicateID++;
			}
		}

		return result;
	}

	public static Predicate getPredicate(IProofTreeNode node, Predicate targetPred) {
		if (targetPred == null) {
			return null;
		}

		IProverSequent sequent = node.getSequent();

		Optional<Predicate> inHyps = StreamSupport.stream(sequent.selectedHypIterable().spliterator(), false).filter(
				pred -> pred != null && (pred.equals(targetPred) || pred.toString().equals(targetPred.toString())))
				.findFirst();
		if (inHyps.isPresent()) {
			return inHyps.get();
		}

		return null;
	}

	public static Predicate getPredicate(IProofTreeNode node, String predicate) {
		IProverSequent sequent = node.getSequent();
		FormulaFactory formulaFactory = sequent.getFormulaFactory();
		ISealedTypeEnvironment typeEnv = sequent.typeEnvironment();

		Predicate targetPred = formulaFactory.parsePredicate(predicate, typeEnv).getParsedPredicate();

		return getPredicate(node, targetPred);
	}

	public static Predicate parserPredicate(IProofTreeNode node, String predicate) {
		IProverSequent sequent = node.getSequent();
		FormulaFactory formulaFactory = sequent.getFormulaFactory();
		ISealedTypeEnvironment typeEnv = sequent.typeEnvironment();

		return formulaFactory.parsePredicate(predicate, typeEnv).getParsedPredicate();
	}

	public static boolean isForAllPredicate(Predicate predicate) {
		if (!(predicate instanceof QuantifiedPredicate)) {
			return false;
		}

		// Check ∀ tag
		int tag = predicate.getTag();
		return tag == Formula.FIRST_QUANTIFIED_PREDICATE;
	}

}
