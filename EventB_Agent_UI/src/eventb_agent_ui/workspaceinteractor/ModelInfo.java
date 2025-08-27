package eventb_agent_ui.workspaceinteractor;

public class ModelInfo {

	private String contextFileName;
	private String machineFileName;
	private String systemDescription;
	private String systemRequirement;

	public ModelInfo(String contextFileName, String machineFileName, String previousDescription,
			String previousRequirement) {
		this.contextFileName = contextFileName;
		this.machineFileName = machineFileName;
		this.systemDescription = previousDescription;
		this.systemRequirement = previousRequirement;
	}

	public String getContextFileName() {
		return contextFileName;
	}

	public String getMachineFileName() {
		return machineFileName;
	}

	public String getSystemDescription() {
		return systemDescription;
	}

	public String getSystemRequirement() {
		return systemRequirement;
	}

}
