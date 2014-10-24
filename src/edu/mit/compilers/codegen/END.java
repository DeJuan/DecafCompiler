package edu.mit.compilers.codegen;

import java.util.List;

public class END extends FlowNode {

	public END(){}
	
	@Override
	public NodeType getType() {
		return NodeType.END;
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
