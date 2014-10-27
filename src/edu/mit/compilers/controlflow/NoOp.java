package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Currently serves as a single node that brings multiple parents together so
 * a single node can be returned and processed without requiring knowledge
 * of later nodes.
 *
 */
public class NoOp extends FlowNode {
	private List<FlowNode> children = new ArrayList<FlowNode>();
	private List<FlowNode> parents = new ArrayList<FlowNode>();
	private boolean visited = false;
	
	public NoOp(){}

	@Override
	public List<FlowNode> getParents(){
		return parents;
	}

	@Override
	/**
	 * Returns the list of length one that contains the lone successor to the start node.
	 * @return child : List<FlowNode> of length one containing first meaningful FlowNode in the current method.  
	 */
	public List<FlowNode> getChildren() {
		return children;
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
		if (children.isEmpty()){
			children.add(newChild);
		}
		else throw new UnsupportedOperationException("NoOp should not have more than one child.");
	}
	
	/**
	 * Traverse this FlowNode and mark visited as true.
	 */
	@Override
	public void visit() {
		visited = true;
	}
	
	/**
	 * Returns whether or not this FlowNode has been traversed already.
	 */
	@Override
	public boolean visited() {
		return visited;
	}

}
