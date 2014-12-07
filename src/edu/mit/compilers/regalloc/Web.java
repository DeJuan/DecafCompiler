package edu.mit.compilers.regalloc;

import java.util.Arrays;
import java.util.HashSet;

import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.Statement;
import edu.mit.compilers.ir.IR_FieldDecl;

public class Web {
	private IR_FieldDecl decl;
	private HashSet<Statement> startingStatements; // initial assignment statement
	// all statements in the live range
	private HashSet<Statement> statements = new HashSet<Statement>();
	private HashSet<FlowNode> nodes = new HashSet<FlowNode>(); // all FlowNodes the web is live in.
	
	public Web(IR_FieldDecl decl) {
		this.decl = decl;
	}
	
	public Web(IR_FieldDecl decl, Statement st, FlowNode node) {
		System.out.println("New web for var: " + decl.getName() + ", Statement: " + st);
		this.decl = decl;
		this.startingStatements = new HashSet<Statement>(Arrays.asList(st));
		this.statements = new HashSet<Statement>(Arrays.asList(st));
		this.nodes = new HashSet<FlowNode>(Arrays.asList(node));
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
	
	public IR_FieldDecl getFieldDecl() {
		return this.decl;
	}

}
