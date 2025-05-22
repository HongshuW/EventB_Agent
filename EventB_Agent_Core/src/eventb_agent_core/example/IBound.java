package eventb_agent_core.example;

import org.eventb.core.ICommentedElement;
import org.eventb.core.IExpressionElement;
import org.rodinp.core.IInternalElementType;
import org.rodinp.core.RodinCore;

import eventb_agent_core.Activator;

public interface IBound extends ICommentedElement, IExpressionElement {

	IInternalElementType<IBound> ELEMENT_TYPE = RodinCore.getInternalElementType(Activator.PLUGIN_ID + ".bound");

}
