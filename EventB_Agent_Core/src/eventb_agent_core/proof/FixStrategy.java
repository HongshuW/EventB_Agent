package eventb_agent_core.proof;

/**
 * This class defines all the strategies for fixing an existing Event-B model.
 */
public enum FixStrategy {
	ADD_AXIOM("Add axiom in context"), // Add hypothesis function
	ADD_HYP_GUARD("Add hypothesis in guard"), //
	STRENGTHEN_INV("Strengthen an invariant"), //
	WEAKEN_INV("Weaken an invariant"), //
	STRENGTHEN_GUARD("Strengthen a guard"), //
	WEAKEN_GUARD("Weaken a guard"), //
	ADAPT_SUBSTITUTION("Adapt substitution"); //

	private final String stringValue;

	FixStrategy(String stringValue) {
		this.stringValue = stringValue;
	}

	@Override
	public String toString() {
		return stringValue;
	}

}
