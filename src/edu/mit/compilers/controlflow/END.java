package edu.mit.compilers.codegen;

import java.util.List;

public class END extends FlowNode {
	Expression returnStatement = null;
	public END(){}
	
	public END(Expression returnVal){
		this.returnStatement = returnVal;
	}
	@Override
	public NodeType getType() {
		return NodeType.END;
	}

	@Override
	public List<FlowNode> getParents() {
		// TODO Auto-generated method stub
		return null;
	}

	public Expression getReturnStatement(){
		return returnStatement;
	}
	@Override
	public List<FlowNode> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
