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
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.seqprover.IProofTree;
import org.eventb.core.seqprover.IProofTreeNode;
import org.eventb.core.seqprover.ProverFactory;
import org.eventb.internal.core.pm.ProofManager;
import org.eventb.smt.core.internal.tactics.SMTAutoTactic;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

public class POManager {

	public POManager() {
	}

	public List<IPOSequent> getOpenPOs(IMachineRoot machineRoot) throws Exception {
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(machineRoot);
		IPORoot poRoot = proofComponent.getPORoot();
		IPSRoot psRoot = proofComponent.getPSRoot();

		Map<String, IPSStatus> statusByPO = getStatusByPOMap(psRoot);

		// try to discharge POs by running SMT solvers
		for (IPOSequent poSequent : poRoot.getSequents()) {
			IPSStatus psStatus = statusByPO.get(poSequent.getElementName());
			if (!isDischarged(psStatus)) {
				runAllEnabledSMT(poSequent, machineRoot);
			}
		}

		statusByPO = getStatusByPOMap(psRoot);

		// collect undischarged POs
		List<IPOSequent> open = new ArrayList<>();
		for (IPOSequent poSequent : poRoot.getSequents()) {
			IPSStatus psStatus = statusByPO.get(poSequent.getElementName());
			if (!isDischarged(psStatus)) {
				open.add(poSequent);
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

	private Map<String, IPSStatus> getStatusByPOMap(IPSRoot psRoot) throws RodinDBException {
		Map<String, IPSStatus> statusByPO = new HashMap<>();
		for (IPSStatus st : psRoot.getChildrenOfType(IPSStatus.ELEMENT_TYPE)) {
			statusByPO.put(st.getPOSequent().getElementName(), st);
		}
		return statusByPO;
	}

	private boolean isDischarged(IPSStatus poStatus) throws CoreException {
		return poStatus.getProof().getProofTree(null).isClosed();
	}

	private void runAllEnabledSMT(IPOSequent poSequent, IMachineRoot machineRoot) throws RodinDBException {
		String poName = poSequent.getElementName();
		IProofComponent proofComponent = ProofManager.getDefault().getProofComponent(machineRoot);
		IProofAttempt proofAttempt = proofComponent.getProofAttempt(poName, "POFixer");
		if (proofAttempt == null) {
			proofAttempt = proofComponent.createProofAttempt(poName, "POFixer", null);
		}

		// retrieve information from workspace
		IProofTree tree = proofAttempt.getProofTree();
		IProofTreeNode root = tree.getRoot();
		SMTAutoTactic smtAutoTactic = new SMTAutoTactic();
		smtAutoTactic.apply(root, null);

		IRodinFile bpo = proofAttempt.getComponent().getPORoot().getRodinFile();
		IRodinFile bps = proofAttempt.getComponent().getPSRoot().getRodinFile();
		proofAttempt.commit(false, false, null);
		bpo.save(null, true);
		bps.save(null, true);

		proofAttempt.dispose();
	}

}
