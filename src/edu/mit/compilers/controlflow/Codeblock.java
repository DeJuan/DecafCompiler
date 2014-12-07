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
	List<Statement> statements = new ArrayList<Statement>();
	private List<FlowNode> children = new ArrayList<FlowNode>();
	private List<FlowNode> parents = new ArrayList<FlowNode>();
	private boolean isBreak = false;;
	
	/**
	 * This is one of two constructors. It assumes you know all relevant info about the Codeblock at initialization. 
	 * @param parentList : List<FlowNode> that contains all immediately preceding FlowNodes. 
	 * @param childList : List<FlowNode> containing all immediate successor FlowNodes.
	 * @param statementList : List<Statement> containing all statements in this particular block of code. 
	 */
	public Codeblock(List<FlowNode> parentList, List<FlowNode> childList, List<Statement> statementList){
		this.parents = parentList;
		this.children = childList;
		this.statements = statementList;
		if (children.size() > 1) {
            throw new RuntimeException("Codeblocks shall have no more than one child");
        }
	}
	
	/**
	 * This is the second constructor. It assumes you know nothing about the block and will fill in the relevant information over time. 
	 */
	public Codeblock(){}

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
		if (children.size() > 1) {
		    throw new RuntimeException("Codeblocks shall have no more than one child");
		}
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
	
	void prependDeclaration(Declaration newDeclare) {
	    statements.add(0, newDeclare);
	}
	
	public void setIsBreak(boolean isBreak) {
	    this.isBreak = isBreak;
	}
	
	public boolean getIsBreak() {
	    return isBreak;
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
