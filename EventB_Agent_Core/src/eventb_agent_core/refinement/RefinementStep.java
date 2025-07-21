package eventb_agent_core.refinement;

import java.util.List;

public class RefinementStep {

	private int refinementNo;
	private List<String> requirementIDs;
	private String modelDesc;

	public RefinementStep(int refinementNo, List<String> requirementIDs, String modelDesc) {
		this.refinementNo = refinementNo;
		this.requirementIDs = requirementIDs;
		this.modelDesc = modelDesc;
	}

	public int getRefinementNo() {
		return refinementNo;
	}

	public List<String> getRequirementIDs() {
		return requirementIDs;
	}

	public String getModelDesc() {
		return modelDesc;
	}

}
