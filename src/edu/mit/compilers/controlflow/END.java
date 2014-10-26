package edu.mit.compilers.controlflow;

import java.util.List;

/**
 * This class represents the end of a control flow section. 
 * @author DeJuan
 *
 */
public class END extends FlowNode {
	private Expression returnStatement = null;
	private List<FlowNode> parent;
	
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
	 * Tells you you're working with an END.
	 * @return NodeType : END
	 */
	public NodeType getType() {
		return NodeType.END;
	}

	@Override
	/**
	 * Gets the parent list of length 1 containing the lone parent to this node.
	 */
	public List<FlowNode> getParents() {
		return parent;
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
		throw new UnsupportedOperationException("The End of Nodes bears no children, mortal.");
	}

	@Override
	public void addParent(FlowNode newParent) {
		parent.add(newParent);
	}

	@Override
	public void addChild(FlowNode newChild) {
		throw new UnsupportedOperationException("I already warned you. No mere programmer can force me to have a child. I am the END!");
		
	}
	
}
