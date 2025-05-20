package eventb_agent.example;

import org.eventb.core.basis.EventBElement;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IInternalElementType;
import org.rodinp.core.IRodinElement;

public class Bound extends EventBElement implements IBound {

	/**
	 * Constructor used by the Rodin database.
	 */
	public Bound(String name, IRodinElement parent) {
		super(name, parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.rodinp.core.basis.InternalElement#getElementType()
	 */
	@Override
	public IInternalElementType<? extends IInternalElement> getElementType() {
		return IBound.ELEMENT_TYPE;
	}

}
