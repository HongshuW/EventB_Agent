package eventb_agent_core.refinement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import eventb_agent_core.utils.FileUtils;

public class SystemRequirements {

	private List<Requirement> requirements;

	public SystemRequirements() {
		this.requirements = new ArrayList<>();
	}

	public SystemRequirements(List<Requirement> requirements) {
		this.requirements = requirements;
	}

	public SystemRequirements(Path path) {
		this.requirements = new ArrayList<>();
		JSONObject requirementsJSON = FileUtils.readJSON(path);
		for (String key : requirementsJSON.keySet()) {
			String reqText = requirementsJSON.getString(key);
			String typeString = key.split("-")[0];
			RequirementType reqType = RequirementType.valueOf(typeString);
			String reqID = typeString + "_" + key.split("-")[1];
			Requirement req = new Requirement(reqType, reqText, reqID);
			requirements.add(req);
		}
	}

	public void addRequirement(Requirement req) {
		this.requirements.add(req);
	}

	public List<Requirement> getRequirements() {
		return this.requirements;
	}

	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();

		int funID = 1;
		int eqpID = 1;
		int envID = 1;
		int safID = 1;

		for (Requirement req : this.requirements) {
			String reqString = "";
			if (req.hasID()) {
				reqString = req.toString();
			} else {
				switch (req.getRequirementType()) {
				case FUN:
					reqString = req.toString(funID);
					funID += 1;
					break;
				case EQP:
					reqString = req.toString(eqpID);
					eqpID += 1;
					break;
				case ENV:
					reqString = req.toString(envID);
					envID += 1;
					break;
				case SAF:
					reqString = req.toString(safID);
					safID += 1;
					break;
				}
			}
			output.append(reqString + "\n");
		}

		return output.toString();
	}

}
