package eventb_agent_ui.workspaceinteractor;

public class ModelInfo {

	private String contextFileName;
	private String machineFileName;
	private String systemDescription;

	public ModelInfo(String contextFileName, String machineFileName, String previousDescription) {
		this.contextFileName = contextFileName;
		this.machineFileName = machineFileName;
		this.systemDescription = previousDescription;
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

}
