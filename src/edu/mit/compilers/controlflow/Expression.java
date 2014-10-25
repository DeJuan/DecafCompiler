package edu.mit.compilers.controlflow;

public abstract class Expression {
	public enum ExpressionType{
		INT_LIT, VAR, BOOL_LIT, BIN_EXPR, METHOD_CALL
	}
	
	public abstract ExpressionType getExprType();
}
