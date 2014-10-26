package edu.mit.compilers.controlflow;

import java.util.List;

public abstract class FlowNode {
	
	public enum NodeType{
		START, END, CODEBLOCK, BRANCH, NOOP
	};
	
	
	public abstract NodeType getType();
	public abstract List<FlowNode> getParents();
	public abstract List<FlowNode> getChildren();
	public abstract void addParent(FlowNode newParent);
	public abstract void addChild(FlowNode newChild);
	
}
