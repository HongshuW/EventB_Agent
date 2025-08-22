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

import eventb_agent_core.utils.proof.ProofUtils;

/**
 * This class is responsible for querying the PO list, and run auto provers on
 * all POs.
 */
public class POManager {

	private static final String PO_OWNER_NAME = "POManager";

	public POManager() {
	}

	public IPOSequent[] getAllPOs(IMachineRoot machineRoot) throws RodinDBException {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(machineRoot);
		IPORoot poRoot = proofComponent.getPORoot();
		return poRoot.getSequents();
	}

	public List<IPOSequent> getOpenPOs(IMachineRoot machineRoot) throws CoreException {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(machineRoot);
		IPORoot poRoot = proofComponent.getPORoot();
		IPSRoot psRoot = proofComponent.getPSRoot();

		Map<String, IPSStatus> statusByPO = getStatusByPOMap(psRoot);

		statusByPO = getStatusByPOMap(psRoot);

		// collect undischarged POs
		List<IPOSequent> open = new ArrayList<>();
		for (IPOSequent poSequent : poRoot.getSequents()) {
			IPSStatus psStatus = statusByPO.get(poSequent.getElementName());
			if (!ProofUtils.isDischarged(machineRoot, psStatus.getElementName())) {
				open.add(poSequent);
			}
		}

		return open;
	}

	public void runAutoProvers(IMachineRoot machineRoot) throws Exception {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(machineRoot);
		IPORoot poRoot = proofComponent.getPORoot();
		IPSRoot psRoot = proofComponent.getPSRoot();

		Map<String, IPSStatus> statusByPO = getStatusByPOMap(psRoot);

		// try to discharge POs by running auto provers, PP, and SMT solvers
		for (IPOSequent poSequent : poRoot.getSequents()) {
			IPSStatus psStatus = statusByPO.get(poSequent.getElementName());
			if (!ProofUtils.isDischarged(machineRoot, psStatus.getElementName())) {
				FixProofStrategyRunner fixer = new FixProofStrategyRunner(poSequent, machineRoot, PO_OWNER_NAME);
				fixer.applyLasoo();
				fixer.runAutoProvers();
			}
		}

		System.out.println(getOpenPOs(machineRoot).size());
	}

	private Map<String, IPSStatus> getStatusByPOMap(IPSRoot psRoot) throws RodinDBException {
		Map<String, IPSStatus> statusByPO = new HashMap<>();
		for (IPSStatus st : psRoot.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
			statusByPO.put(st.getPOSequent().getElementName(), st);
		}
		return statusByPO;
	}

}
