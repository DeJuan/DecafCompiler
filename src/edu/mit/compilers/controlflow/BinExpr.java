package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Ops;

public class BinExpr extends Expression {
	private Expression leftSide;
	private Ops operator;
	private Expression rightSide;
	
	public BinExpr(Expression lhs, Ops op, Expression rhs){
		this.leftSide = lhs;
		this.operator = op;
		this.rightSide = rhs;
	}
	
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.BIN_EXPR;
	}
	
	public Expression getLeftSide(){
		return leftSide;
	}
	
	public Ops getOperator(){
		return operator;
	}
	
	public Expression getRightSide(){
		return rightSide;
	}

}
