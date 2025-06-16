package eventb_agent_core.llm.schemas;

public class SchemaKeys {

	/* context keys */

	public static final String CONTEXT_OBJ_KEY = "context";
	public static final String CONTEXT = "CONTEXT";
	public static final String EXTENDS = "EXTENDS";
	public static final String SETS = "SETS";
	public static final String CONSTANTS = "CONSTANTS";
	public static final String AXIOMS = "AXIOMS";

	/* machine keys */

	public static final String MACHINE_OBJ_KEY = "machine";
	public static final String MACHINE = "MACHINE";
	public static final String REFINES = "REFINES";
	public static final String SEES = "SEES";
	public static final String VARIABLES = "VARIABLES";
	public static final String INVARIANTS = "INVARIANTS";
	public static final String VARIANTS = "VARIANTS";
	public static final String EVENTS = "EVENTS";

	/* internal keys for labeled predicates */

	public static final String LABEL = "label_name";
	public static final String PRED = "predicate";
	public static final String ASSIGN = "assignment";
	public static final String EXPR = "expression";

	/* internal keys for events */

	public static final String EVENT_NAME = "event_name";
	// REFINES defined above
	public static final String ANY = "ANY";
	public static final String WHERE = "WHERE";
	public static final String WITH = "WITH";
	public static final String THEN = "THEN";

}
