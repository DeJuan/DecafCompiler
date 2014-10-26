package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Currently serves as a single node that brings multiple parents together so
 * a single node can be returned.
 *
 */
public class NoOp extends FlowNode {
	private List<FlowNode> child = new ArrayList<FlowNode>();
	private List<FlowNode> parents = new ArrayList<FlowNode>();
	
	public NoOp(){}
	
	@Override
	/**
	 * Tells you that you're working with a START.
	 * @return NodeType : START
	 */
	public NodeType getType() {
		return NodeType.NOOP;
	}

	@Override
	public List<FlowNode> getParents(){
		return this.parents;
	}

	@Override
	/**
	 * Returns the list of length one that contains the lone successor to the start node.
	 * @return child : List<FlowNode> of length one containing first meaningful FlowNode in the current method.  
	 */
	public List<FlowNode> getChildren() {
		return this.child;
	}

	@Override
	public void addParent(FlowNode newParent) {
		this.parents.add(newParent);
	}

	@Override
	/**
	 * Adder method allowing you to append a child to the list of children. Will error if the list already has a child. 
	 */
	public void addChild(FlowNode newChild) {
		if (this.child.isEmpty()){
			this.child.add(newChild);
		}
		else throw new UnsupportedOperationException("NoOp should have more than one child.");
	}

}
