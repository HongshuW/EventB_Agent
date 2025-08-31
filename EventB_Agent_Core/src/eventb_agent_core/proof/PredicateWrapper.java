package eventb_agent_core.proof;

import org.eventb.core.ast.Predicate;

public class PredicateWrapper {

	public Predicate predicate;
	public int predicateID;
	public boolean isGoal;

	public PredicateWrapper(Predicate predicate, int predicateID) {
		this(predicate, predicateID, false);
	}

	public PredicateWrapper(Predicate predicate, int predicateID, boolean isGoal) {
		this.predicate = predicate;
		this.predicateID = predicateID;
		this.isGoal = isGoal;
	}

}
