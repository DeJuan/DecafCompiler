package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Ops;

public class NegateExpr extends Expression {
	Expression negatedExpr;
	
	public NegateExpr(Expression expr){
		IntLit negativeOne = new IntLit(-1);
		Ops operator = Ops.TIMES;
		this.negatedExpr = new MultExpr(negativeOne, operator, expr);
	}
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.NEGATE;
	}
	
	public Expression getNegatedExpr(){
		return this.negatedExpr;
	}
}
