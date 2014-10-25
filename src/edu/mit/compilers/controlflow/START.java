package edu.mit.compilers.codegen;

import java.util.List;

public class START extends FlowNode {
	
	public START(){}

	@Override
	public NodeType getType() {
		return NodeType.START;
	}

	@Override
	public List<FlowNode> getParents() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FlowNode> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
