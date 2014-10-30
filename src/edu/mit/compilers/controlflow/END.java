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
	private boolean visited = false;
	private String label;
	
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
	 * Traverse this FlowNode and mark visited as true.
	 */
	@Override
	public void visit() {
		visited = true;
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
	
	/**
	 * Returns whether or not this FlowNode has been traversed already.
	 */
	@Override
	public boolean visited() {
		return visited;
	}

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * SHOULD ONLY BE CALLED ONCE
     */
    @Override
    public void setLabel(String label) {
        // Enforce called once?
        this.label = label;
    }
	
}
