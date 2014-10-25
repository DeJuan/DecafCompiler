package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

public class START extends FlowNode {
	private List<FlowNode> child = new ArrayList<FlowNode>();
	
	public START(){}
	
	public START(FlowNode childNode){
		this.child.add(childNode);
	}

	@Override
	public NodeType getType() {
		return NodeType.START;
	}

	@Override
	public List<FlowNode> getParents(){
		throw new UnsupportedOperationException("The Origin of All has no predecessors.");
	}

	@Override
	public List<FlowNode> getChildren() {
		return this.child;
	}

	@Override
	public void addParent(FlowNode newParent) {
		throw new UnsupportedOperationException("You cannot force a START to have parents.");
		
	}

	@Override
	public void addChild(FlowNode newChild) {
		this.child.add(newChild);
	}
	
	
	
}
