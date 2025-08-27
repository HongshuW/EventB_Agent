package eventb_agent_core.refinement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RefinementStep {

	private int refinementNo;
	private List<String> requirementIDs;
	private List<String> requirementList;
	private String modelDesc;
	private Map<String, String> gluingInvariants;

	public RefinementStep(int refinementNo, List<String> requirementIDs, String modelDesc,
			Map<String, String> gluingInvariants, SystemRequirements requirements) {
		this.refinementNo = refinementNo;
		this.requirementIDs = requirementIDs;
		this.requirementList = initRequirementList(requirements);
		this.modelDesc = modelDesc;
		this.gluingInvariants = gluingInvariants;
	}

	private List<String> initRequirementList(SystemRequirements systemReqs) {
		List<String> reqList = new ArrayList<>();
		if (systemReqs == null) {
			return reqList;
		}
		List<Requirement> requirements = systemReqs.getRequirements();
		for (Requirement req : requirements) {
			String reqID = req.getRequirementID();
			for (String id : requirementIDs) {
				if (id.equals(reqID)) {
					reqList.add(id + ":" + req.getRequirementText());
					break;
				}
			}
		}
		return reqList;
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

	public String getSysReqString() {
		StringBuilder systemRequirements = new StringBuilder();
		for (String req : requirementList) {
			systemRequirements.append(req + "\n");
		}
		return systemRequirements.toString();
	}
	
	public String getGluingInvariantsString() {
		StringBuilder gluingInvs = new StringBuilder();
		for (String key : gluingInvariants.keySet()) {
			gluingInvs.append(key + ": ");
			gluingInvs.append(gluingInvariants.get(key) + "\n");
		}
		return gluingInvs.toString();
	}

}
