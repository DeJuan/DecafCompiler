package edu.mit.compilers.regalloc;

import java.util.Arrays;
import java.util.HashSet;

import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.Statement;

public class Web {
	private String varName;
	private HashSet<Statement> startingStatements; // initial assignment statement
	// all statements in the live range
	private HashSet<Statement> statements = new HashSet<Statement>();
	private HashSet<FlowNode> nodes = new HashSet<FlowNode>(); // all FlowNodes the web is live in.
	
	public Web(String varName) {
		this.varName = varName;
	}
	
	public Web(String varName, Statement st, FlowNode node) {
		this.varName = varName;
		this.startingStatements = new HashSet<Statement>(Arrays.asList(st));
		this.statements = new HashSet<Statement>(Arrays.asList(st));
		this.nodes = new HashSet<FlowNode>(Arrays.asList(node));
	}
	
	public Web(String varName, HashSet<Statement> statements) {
		this.varName = varName;
		this.startingStatements = statements;
	}
	
	public void addStatement(Statement st) {
		statements.add(st);
	}
	
	public void addNode(FlowNode node) {
		nodes.add(node);
	}
	
	public HashSet<Statement> getStartingStatements() {
		return this.startingStatements;
	}
	
	public HashSet<FlowNode> getNodes() {
		return this.nodes;
	}
	
	public void setStartingStatements(HashSet<Statement> statements) {
		this.startingStatements = statements;
	}
	
	public HashSet<Statement> getStatements() {
		return this.statements;
	}
	
	public String getVarName() {
		return this.varName;
	}

}
