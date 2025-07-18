package eventb_agent_core.utils.proof;

import org.eventb.core.IEventBRoot;
import org.eventb.core.ast.Formula;
import org.eventb.core.ast.IParseResult;
import org.eventb.core.ast.Predicate;
import org.eventb.core.ast.QuantifiedPredicate;
import org.eventb.core.seqprover.IProofTreeNode;

public class PredicateUtils {

	public static Predicate parsePredicate(IEventBRoot eventbRoot, String predString) {
		IParseResult parseRes = eventbRoot.getFormulaFactory().parsePredicate(predString, null);

		if (parseRes.hasProblem()) {
			throw new IllegalArgumentException("Invalid predicate syntax: " + parseRes.getProblems().toString());
		}

		return parseRes.getParsedPredicate();
	}
	
	public static Predicate getEquivalentPredicate(IProofTreeNode node, Predicate targetPred) {
		for (Predicate pred : node.getSequent().selectedHypIterable()) {
			if (pred.toString().equals(targetPred.toString())) {
				return pred;
			}
		}
		return null;
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
