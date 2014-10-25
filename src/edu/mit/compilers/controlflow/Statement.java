package edu.mit.compilers.controlflow;

public abstract class Statement {
	
	public enum StatementType{
		ASSIGNMENT, METHOD_CALL_STATEMENT
	};
	
	public abstract StatementType getStatementType();
}
