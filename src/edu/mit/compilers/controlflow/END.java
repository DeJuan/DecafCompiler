package edu.mit.compilers.controlflow;

import java.util.List;

public class END extends FlowNode {
	private Expression returnStatement = null;
	private List<FlowNode> parent;
	
	public END(FlowNode parentNode){this.parent.add(parentNode);}
	
	public END(FlowNode parentNode, Expression returnVal){
		this.parent.add(parentNode);
		this.returnStatement = returnVal;
	}
	
	
	@Override
	public NodeType getType() {
		return NodeType.END;
	}

	@Override
	public List<FlowNode> getParents() {
		return parent;
	}
	
	public void setReturnStatement(Expression newReturnStatement){
		this.returnStatement = newReturnStatement;
	}
	
	public Expression getReturnStatement(){
		return returnStatement;
	}
	@Override
	public List<FlowNode> getChildren(){
		throw new UnsupportedOperationException("The End of Nodes bears no children, mortal.");
	}

	@Override
	public void addParent(FlowNode newParent) {
		throw new UnsupportedOperationException("You can't add another parent. You gave me mommy when you initialized me.");
	}

	@Override
	public void addChild(FlowNode newChild) {
		throw new UnsupportedOperationException("I already warned you. No mere programmer can force me to have a child. I am the END!");
		
	}
	
}
