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
			RequirementType reqType = key.contains("FUN") ? RequirementType.FUN : RequirementType.EQP;
			Requirement req = new Requirement(reqType, reqText);
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

		for (Requirement req : this.requirements) {
			String reqString = "";
			if (req.hasID()) {
				reqString = req.toString();
			} else {
				if (req.isFunType()) {
					reqString = req.toString(funID);
					funID += 1;
				} else {
					reqString = req.toString(eqpID);
					eqpID += 1;
				}
			}
			output.append(reqString + "\n");
		}

		return output.toString();
	}

}
