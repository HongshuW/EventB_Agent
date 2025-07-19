package eventb_agent_core.proof;

/**
 * This is a corresponding class for hypothesis object in "add hypothesis"
 * schema.
 */
public class Hypothesis {

	private String label;
	private String predicate;
	private String[] instantiations;

	public Hypothesis(String label, String predicate) {
		this.label = label;
		this.predicate = predicate;
		this.instantiations = null;
	}

	public Hypothesis(String label, String predicate, String... instantiations) {
		this.label = label;
		this.predicate = predicate;
		this.instantiations = instantiations;
	}

	public String getLabel() {
		return label;
	}

	public String getPredicate() {
		return predicate;
	}

	public String[] getInstantiations() {
		return instantiations;
	}

}
