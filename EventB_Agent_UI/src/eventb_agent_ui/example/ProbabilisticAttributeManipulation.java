package eventb_agent_ui.example;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eventb.core.IEvent;
import org.eventb.ui.manipulation.IAttributeManipulation;
import org.rodinp.core.IAttributeType;
import org.rodinp.core.IRodinElement;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import eventb_agent_core.Activator;

public class ProbabilisticAttributeManipulation implements IAttributeManipulation {

	private static String STANDARD = "standard";

	private static String PROBABILISTIC = "probabilistic";

	public static IAttributeType.Boolean PROB_ATTRIBUTE = RodinCore
			.getBooleanAttrType(Activator.PLUGIN_ID + ".probabilistic");

	public ProbabilisticAttributeManipulation() {
	}

	private IEvent asEvent(IRodinElement element) {
		assert element instanceof IEvent;
		return (IEvent) element;
	}

	@Override
	public String[] getPossibleValues(IRodinElement element, IProgressMonitor monitor) {
		return new String[] { STANDARD, PROBABILISTIC };
	}

	@Override
	public String getValue(IRodinElement element, IProgressMonitor monitor) throws RodinDBException {
		return asEvent(element).getAttributeValue(PROB_ATTRIBUTE) ? PROBABILISTIC : STANDARD;
	}

	@Override
	public boolean hasValue(IRodinElement element, IProgressMonitor monitor) throws RodinDBException {
		return asEvent(element).hasAttribute(PROB_ATTRIBUTE);
	}

	@Override
	public void removeAttribute(IRodinElement element, IProgressMonitor monitor) throws RodinDBException {
		asEvent(element).removeAttribute(PROB_ATTRIBUTE, monitor);
	}

	@Override
	public void setDefaultValue(IRodinElement element, IProgressMonitor monitor) throws RodinDBException {
		asEvent(element).setAttributeValue(PROB_ATTRIBUTE, false, monitor);

	}

	@Override
	public void setValue(IRodinElement element, String value, IProgressMonitor monitor) throws RodinDBException {
		final boolean isProbabilistic = value.equals(PROBABILISTIC);
		asEvent(element).setAttributeValue(PROB_ATTRIBUTE, isProbabilistic, monitor);
	}

}
