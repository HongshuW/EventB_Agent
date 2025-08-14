package eventb_agent_ui.workspaceinteractor;

public class ModelInfo {

	private String machineFileName;
	private String contextFileName;
	private String systemDescription;

	public ModelInfo(String machineFileName, String contextFileName, String previousDescription) {
		this.machineFileName = machineFileName;
		this.contextFileName = contextFileName;
		this.systemDescription = previousDescription;
	}

	public String getMachineFileName() {
		return machineFileName;
	}

	public String getContextFileName() {
		return contextFileName;
	}

	public String getSystemDescription() {
		return systemDescription;
	}

}
