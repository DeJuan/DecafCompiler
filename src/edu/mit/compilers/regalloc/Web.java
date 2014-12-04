package edu.mit.compilers.regalloc;

import java.util.HashSet;

import edu.mit.compilers.controlflow.Statement;

public class Web {
	private String varName;
	private HashSet<Statement> statements = new HashSet<Statement>();
	
	public Web(String varName) {
		this.varName = varName;
	}
	
	public void addStatement(Statement st) {
		statements.add(st);
	}
	
	public HashSet<Statement> getStatements() {
		return this.statements;
	}
	
	public String getVarName() {
		return this.varName;
	}

}
