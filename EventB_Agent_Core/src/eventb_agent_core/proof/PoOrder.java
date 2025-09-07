package eventb_agent_core.proof;

public class PoOrder {

	/**
	 * Returns true if s1 should be proven before s2, false otherwise.
	 */
	public static boolean shouldProveBefore(String s1, String s2) {
		return getPriority(s1) < getPriority(s2);
	}

	/**
	 * Assigns a numeric priority to each PO type. Lower number = earlier to prove.
	 */
	private static int getPriority(String poName) {
		if (poName.contains("WD")) {
			return 1;
		} else if (poName.contains("gluing_inv_") || poName.contains("REF") || poName.contains("WIT")) {
			return 2;
		} else if (poName.contains("EQL") || poName.contains("FIS")) {
			return 3;
		} else if (poName.contains("INV")) {
			return 4;
		}

		return Integer.MAX_VALUE;
	}

}
