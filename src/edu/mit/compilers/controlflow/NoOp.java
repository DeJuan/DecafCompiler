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
	
	/**
	 * Allows you to remove a parent. This is done when we want to replace a parent 
	 * codeblock or branch with an optimized version.
	 * 
	 * @param parent : The FlowNode you wish to remove from the parent list
	 */
	public void removeParent(FlowNode parent){
		if (parents.contains(parent)){
			parents.remove(parent);
		}
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
	 * Allows you to remove a child. This is done when we want to replace a child
	 * codeblock or branch with an optimized version. 
	 * 
	 * @param child : The node we want to remove. 
	 */
	public void removeChild(FlowNode child){
		if(children.contains(child)){
			children.remove(child);
		}
	}
	
	/**
	 * Reset the visited flag of this FlowNode and its children.
	 * 
	 * Note: It will only reset the child if the child has been visited,
	 * meaning that resetVisit will successfully reset all visited nodes
	 * assuming that all traversals started from the ROOT.
	 */
	@Override
	public void resetVisit() {
		visited = false;
		if (children.size() > 0) {
			for (FlowNode child : children) {
				if (child.visited())
					child.resetVisit();
			}
		}
	}
	
	@Override
    public void replaceParent(FlowNode newParent, FlowNode oldParent) {
        if (!parents.remove(oldParent)) {
            throw new RuntimeException("Provided oldparent not a parent of this node");
        }
        addParent(newParent); 
    }

}
