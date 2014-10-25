package edu.mit.compilers.controlflow;

import java.util.List;

/**
 * This class is our representation of branches in control flow. 
 * @author DeJuan
 *
 */
public class Branch extends FlowNode {
	private Expression expr;
	private FlowNode trueBranch;
	private FlowNode falseBranch;
	private List<FlowNode> parents;
	private List<FlowNode> children;
	
	/**
	 * This is the first of the two constructors for our branch representation. This assumes you know everything about the branch when you try to make it.
	 * @param parentList : The list of parent FlowNodes who link to this branch.
	 * @param ifTrue : FlowNode representing the path we take if this branch evaluates to true.
	 * @param ifFalse : FlowNode representing the path we take if this branch evaluates to false.
	 * @param express : Expression representing the actual branch condition. (i.e. if Y < 3 would have Y < 3 as the expression.)
	 */
	public Branch(List<FlowNode> parentList, FlowNode ifTrue, FlowNode ifFalse, Expression express){
		this.trueBranch = ifTrue;
		this.falseBranch = ifFalse;
		this.parents = parentList;
		this.children.add(trueBranch);
		this.children.add(falseBranch);
		this.expr = express;
	}
	
	/**
	 * This is the second of the constructors for Branch. This assumes you know the bare minimum about the branch, which is its condition.
	 * @param express : Expression representing the branch condition (i.e. if X >= 5 would have X >= 5 as the expression.
	 */
	public Branch(Expression express){this.expr = express;}
	
	@Override
	/**
	 * Tells you that you're working with a branch.
	 * @return NodeType : BRANCH
	 */
	public NodeType getType() {
		return NodeType.BRANCH;
	}

	@Override
	/**
	 * Gets you the list of parents of this Branch.
	 * @return parents : List<FlowNode> that contains all flownodes directly preceding this one. 
	 */
	public List<FlowNode> getParents() {
		return parents;
	}
	
	/**
	 * Adder method that allows you to update the parent list.
	 * @param newParent : FlowNode that will be appended to the parent list.  
	 */
	public void addParent(FlowNode newParent){
		parents.add(newParent);
	}
	
	@Override
	/**
	 * Getter method for the children of this branch. This is a list that combines both the false and true branches
	 * in one returned list. 
	 * 
	 *  @return children : List<FlowNode> that combines the true and false branches. True precedes false. 
	 */
	public List<FlowNode> getChildren() {
		return children;
	}
	
	/**
	 * Adder method that allows you to add a new child to the child list.
	 * @param newChild : FlowNode that will be appended to the child list.  
	 */
	public void addChild(FlowNode newChild){
		children.add(newChild);
	}
	
	/**
	 * Method that allows you to retrieve the true branch outcome.
	 * @return trueBranch : FlowNode representing path taken if branch evaluates to True
	 */
	public FlowNode getTrueBranch(){
		return this.trueBranch;
	}
	
	/**
	 * Method that allows you to set the true branch, in case you didn't know it at initialization.
	 * @param newTrueBranch : FlowNode representing new path taken if branch evaluates to True
	 */
	public void setTrueBranch(FlowNode newTrueBranch){
		this.trueBranch = newTrueBranch;		
	}
	
	/**
	 * Method that allows you to retrieve the false branch outcome.
	 * @return falseBranch : FlowNode representing path taken if branch evaluates to False
	 */
	public FlowNode getFalseBranch(){
		return this.falseBranch;
	}
	
	/**
	 * Method that allows you to set the false branch, in case it wasn't known at initialization.
	 * @param newFalseBranch : FlowNode representing new false path taken if branch evaluates to False. 
	 */
	public void setFalseBranch(FlowNode newFalseBranch){
		this.falseBranch = newFalseBranch;
	}
	
	/**
	 * Method that allows you to get the conditional used to resolve the branch path taken.
	 * @return expr : Expression representing the condition used to determine whether the true or false path of the branch will be used.
	 */
	public Expression getExpr(){
		return this.expr;
	}
	
	/**
	 * Method that allows you to change the conditional used to resolve the branch path taken.
	 * You do not need this until you try to do copy propagation, and is unneeded for CSE.  
	 * @param newExpression : Expression representing condition used to resolve branch direction. 
	 */
	public void setExpr(Expression newExpression){
		this.expr = newExpression;
	}
}
