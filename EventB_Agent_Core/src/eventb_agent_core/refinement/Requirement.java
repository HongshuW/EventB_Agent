package eventb_agent_core.refinement;

public class Requirement {

	private RequirementType requirementType;
	private String requirementText;
	private String requirementID;

	public Requirement(RequirementType requirementType, String requirementText) {
		this(requirementType, requirementText, null);
	}

	public Requirement(RequirementType requirementType, String requirementText, String requirementID) {
		this.requirementType = requirementType;
		this.requirementText = requirementText;
		this.requirementID = requirementID;
	}

	@Override
	public String toString() {
		return (this.hasID() ? this.requirementID : this.requirementType.toString()) + ": " + this.requirementText;
	}

	public String toString(int id) {
		return this.requirementType.toString() + "_" + String.valueOf(id) + ": " + this.requirementText;
	}
	
	public String toSimpleString() {
		return (this.hasID() ? this.requirementID : this.requirementType.toString());
	}
	
	public String toSimpleString(int id) {
		return this.requirementType.toString() + "_" + String.valueOf(id);
	}

	public boolean hasID() {
		return this.requirementID != null;
	}

	public RequirementType getRequirementType() {
		return this.requirementType;
	}

	public String getRequirementText() {
		return this.requirementText;
	}

	public String getRequirementID() {
		return this.requirementID;
	}

}
