package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents the end of a control flow section. 
 * @author DeJuan
 *
 */
public class END extends FlowNode {
	private Expression returnStatement = null;
	private List<FlowNode> parents = new ArrayList<FlowNode>();
	
	/**
	 * Constructor for void methods.
	 */
	public END() {}
	
	/**
	 * This constructor takes in a return value, used for non-void methods.
	 * @param returnVal : Expression representing what we will return. 
	 */
	public END(Expression returnVal){
		this.returnStatement = returnVal;
	}

	@Override
	/**
	 * Gets the parent list of length 1 containing the lone parent to this node.
	 */
	public List<FlowNode> getParents() {
		return parents;
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
	
	/**
	 * Allows you to set the return value. This isn't needed in CSE but may be used later in copy propagation or something.  
	 * @param newReturnExpression : Expression describing what we'll return instead of what was given earlier.
	 */
	public void setReturnExpression(Expression newReturnExpression){
		this.returnStatement = newReturnExpression;
	}
	/**
	 * Getter method for the value we should be returning.
	 * @return Expression : Expression representing the value for what will be returned. 
	 */
	public Expression getReturnExpression(){
		return returnStatement;
	}
	@Override
	public List<FlowNode> getChildren(){
		return Collections.emptyList();
	}

	
	@Override
	public void addParent(FlowNode newParent) {
		parents.add(newParent);
	}

	@Override
	public void addChild(FlowNode newChild) {
		throw new UnsupportedOperationException("I already warned you. No mere programmer can force me to have a child. I am the END!");
		
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
	}
	
	@Override
    public void replaceParent(FlowNode newParent, FlowNode oldParent) {
        if (!parents.remove(oldParent)) {
            throw new RuntimeException("Provided oldparent not a parent of this node");
        }
        addParent(newParent); 
    }
	
}
