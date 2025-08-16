package eventb_agent_core.proof;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPORoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.IPSRoot;
import org.eventb.core.IPSStatus;
import org.eventb.core.pm.IProofComponent;
import org.eventb.internal.core.pm.ProofManager;
import org.rodinp.core.RodinDBException;

public class POManager {

	public POManager() {
	}

	public List<IPOSequent> getOpenPOs(IMachineRoot machineRoot) throws Exception {
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		IPORoot poRoot = pc.getPORoot();
		IPSRoot psRoot = pc.getPSRoot();

		Map<String, IPSStatus> statusByPO = new HashMap<>();
		for (IPSStatus st : psRoot.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
			statusByPO.put(st.getPOSequent().getElementName(), st);
		}

		List<IPOSequent> open = new ArrayList<>();
		for (IPOSequent seq : poRoot.getSequents()) {
			IPSStatus st = statusByPO.get(seq.getElementName());
			if (!isDischarged(st)) {
				open.add(seq);
			}
		}

		return open;
	}

	public boolean isDischarged(IMachineRoot machineRoot, String poName) throws RodinDBException, CoreException {
		IProofComponent pc = ProofManager.getDefault().getProofComponent(machineRoot);
		IPSRoot psRoot = pc.getPSRoot();

		for (IPSStatus st : psRoot.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
			String stPOName = st.getPOSequent().getElementName();
			if (stPOName.equals(poName) || stPOName == poName) {
				if (!isDischarged(st)) {
					return false;
				}
			}
		}

		return true;
	}

	private boolean isDischarged(IPSStatus poStatus) throws CoreException {
		return poStatus.getProof().getProofTree(null).isClosed();
	}

}
