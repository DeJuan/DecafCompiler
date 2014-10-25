package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

public class Codeblock extends FlowNode {
	private int temp = -1;
	private List<Statement> statements = new ArrayList<Statement>();
	private List<FlowNode> children = new ArrayList<FlowNode>();
	private List<FlowNode> parents = new ArrayList<FlowNode>();
	
	public Codeblock(List<FlowNode> parentList, List<FlowNode> childList, List<Statement> statementList){
		this.parents = parentList;
		this.children = childList;
		this.statements = statementList;
	}
	
	public Codeblock(){}
	
	@Override
	public NodeType getType() {
		return NodeType.CODEBLOCK;
	}

	@Override
	public List<FlowNode> getParents() {
		return parents;
	}

	public void addParent(FlowNode newParent){
		parents.add(newParent);
	}
	
	@Override
	public List<FlowNode> getChildren() {
		return children;
	}
	
	public void addChild(FlowNode newChild){
		children.add(newChild);
	}
	
	public int getNextTemp(){
		temp +=1;
		return temp;
	}
	
	public List<Statement> getStatements(){
		return statements;
	}
	
	public void addStatement(Statement newStatement){
		statements.add(newStatement);
	}

}
