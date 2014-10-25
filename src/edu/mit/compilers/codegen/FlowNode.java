package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.List;

public abstract class FlowNode {
	private List<FlowNode> parents = new ArrayList<FlowNode>();
	private List<FlowNode> children = new ArrayList<FlowNode>();
	
	public enum NodeType{
		START, END, CODEBLOCK, BRANCH
	};
	
	
	public abstract NodeType getType();
	public abstract List<FlowNode> getParents();
	public abstract List<FlowNode> getChildren();
	
	
}