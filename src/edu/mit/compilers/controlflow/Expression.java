package edu.mit.compilers.controlflow;

public abstract class Expression {
	public enum ExpressionType{
		INT_LIT, VAR, BOOL_LIT, BIN_EXPR, METHOD_CALL, NOT, NEGATE, TERNARY, ADD_EXPR, COMP_EXPR, COND_EXPR, MOD_EXPR, MULT_EXPR, DIV_EXPR; 
	}
	
	public abstract ExpressionType getExprType();
}
