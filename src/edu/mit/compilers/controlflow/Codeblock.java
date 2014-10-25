package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the class representing individual code blocks. 
 * @author DeJuan
 *
 */
public class Codeblock extends FlowNode {
	private int temp = -1;
	private List<Statement> statements = new ArrayList<Statement>();
	private List<FlowNode> children = new ArrayList<FlowNode>();
	private List<FlowNode> parents = new ArrayList<FlowNode>();
	
	/**
	 * This is one of two constructors. It assumes you know all relevant info about the Codeblock at initialization. 
	 * @param parentList : List<FlowNode> that contains all immediately preceeding flownodes. 
	 * @param childList : List<FlowNode> containing all immediate successor flownodes.
	 * @param statementList : List<Statement> containing all statements in this particular block of code. 
	 */
	public Codeblock(List<FlowNode> parentList, List<FlowNode> childList, List<Statement> statementList){
		this.parents = parentList;
		this.children = childList;
		this.statements = statementList;
	}
	
	/**
	 * This is the second constructor. It assumes you know nothing about the block and will fill in the relevant information over time. 
	 */
	public Codeblock(){}
	
	@Override
	/**
	 * Tells you you're working with a code block.
	 * @return NodeType : CODEBLOCK
	 */
	public NodeType getType() {
		return NodeType.CODEBLOCK;
	}

	@Override
	/**
	 * Gets list of parent FlowNodes.
	 * @return parents : List<FlowNode> containing immediate predecessors.
	 */
	public List<FlowNode> getParents() {
		return parents;
	}

	/**
	 * Allows you to add parents to this codeblock, in case you used the second constructor.
	 * @param newParent : FlowNode that will be appended to the parent list. 
	 */
	public void addParent(FlowNode newParent){
		parents.add(newParent);
	}
	
	/**
	 * Gets list of child FlowNodes.
	 * @return children : List<FlowNode> containing immediate successors.
	 */
	@Override
	public List<FlowNode> getChildren() {
		return children;
	}
	
	/**
	 * Allows you to add children to this codeblock, in case you didn't initialize them at construction.
	 * @param newChild : FlowNode that will be appended to the child list. 
	 */
	public void addChild(FlowNode newChild){
		children.add(newChild);
	}
	
	/**
	 * Allows you to retrieve the next temporary variable. It increments the local counter used to keep track of current temp,
	 * and then returns the new value. 
	 * @return temp : monotonically increasing integer representing a yet-unused temporary variable's number. 
	 */
	public int getNextTemp(){
		temp +=1;
		return temp;
	}
	
	/**
	 * Allows you to get the list of statements contained in this Codeblock. 
	 * @return statements : List<Statement> that contains all statements given to this Codeblock so far. 
	 */
	public List<Statement> getStatements(){
		return statements;
	}
	
	/**
	 * Allows you to append new statements to this Codeblock. 
	 * @param newStatement : Statement that will be appended to the end of the statements list. 
	 */
	public void addStatement(Statement newStatement){
		statements.add(newStatement);
	}

}