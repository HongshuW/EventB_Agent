package eventb_agent_core.proof;

public enum ProofFixingStrategies {

	addAbstractExpression, addHypothesesToContext, addHypothesesToGuard, applyProofTactic, caseDistinction,
	instantiation, selectHypothesisFromContext, strengthenGuard, strengthenInvariant, updateAction, // strategies with
																									// parameters

	arithmeticRewrite, cardinalityDefinition, conjunction, disjunctionToImplication, equality, equivalence,
	finiteDefinition, functionalImageDefinition, implicationAnd, implicationOr, partition, removeInclusion,
	removeMembership, removeNegation, setEqual, setMinus, strictInclusion, relationOverwriteDefinition, //

	applySMT, applyLasoo, //

	others

}
