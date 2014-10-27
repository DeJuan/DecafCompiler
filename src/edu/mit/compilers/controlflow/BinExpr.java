package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Ops;

/**
 * This class represents Expr Op Expr; in other words, any binary expression. 
 * It is a subclass of Expression. 
 * @author DeJuan
 *
 */
public abstract class BinExpr extends Expression {
	private Expression leftSide;
	private Ops operator;
	private Expression rightSide;
	
	/**
	 * Constructor for our representation of binary expressions. 
	 * @param lhs : Left hand side of the expression
	 * @param op : The operator joining the two sides of the operation 
	 * @param rhs : The right hand side of the expression. 
	 */
	public BinExpr(Expression lhs, Ops op, Expression rhs){
		this.leftSide = lhs;
		this.operator = op;
		this.rightSide = rhs;
	}
	
	@Override
	/**
	 * Tells you this is a binary expression
	 * @return ExpressionType : BIN_EXPR
	 */
	public abstract ExpressionType getExprType();
	
	
	/**
	 * Gets you the Expression on the left side of the operator.
	 * @return Expression : Whatever's on the left of the operator.
	 */
	public Expression getLeftSide(){
		return leftSide;
	}
	
	/**
	 * Gets the operator for the expression.
	 * @return Ops : Member of the Ops enum that represents the operator in the expression. 
	 */
	public Ops getOperator(){
		return operator;
	}
	/**
	 * Gets you the Expression on the right side of the operator.
	 * @return Expression : Whatever's on the right of the operator.
	 */
	public Expression getRightSide(){
		return rightSide;
	}

}
