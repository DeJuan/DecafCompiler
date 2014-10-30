package edu.mit.compilers.controlflow;

import java.util.List;

public abstract class FlowNode {
	
	public abstract List<FlowNode> getParents();
	public abstract List<FlowNode> getChildren();
	public abstract void addParent(FlowNode newParent);
	public abstract void addChild(FlowNode newChild);
	public abstract void visit();
	public abstract boolean visited();
	public abstract void resetVisit();
	public abstract String getLabel();
	public abstract void setLabel(String label);
	
}
