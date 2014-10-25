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
	 * This constructor takes in just the parent of this End node, and is used for void methods. 
	 * @param parentNode : FlowNode representing predecessor.
	 */
	public END(FlowNode parentNode){this.parent.add(parentNode);}
	
	/**
	 * This constructor takes in the parent of this END node and a return value, used for non-void methods.
	 * @param parentNode : FlowNode representing predecessor.
	 * @param returnVal : Expression representing what we will return. 
	 */
	public END(FlowNode parentNode, Expression returnVal){
		this.parent.add(parentNode);
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
		throw new UnsupportedOperationException("You can't add another parent. You gave me mommy when you initialized me.");
	}

	@Override
	public void addChild(FlowNode newChild) {
		throw new UnsupportedOperationException("I already warned you. No mere programmer can force me to have a child. I am the END!");
		
	}
	
}
