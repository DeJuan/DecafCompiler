package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Ops;

public class NegateExpr extends Expression {
	private Expression negatedExpr;
	private Expression oppositeOf;
	
	public NegateExpr(Expression expr){
		IntLit negativeOne = new IntLit(-1);
		Ops operator = Ops.TIMES;
		this.negatedExpr = new MultExpr(negativeOne, operator, expr);
		this.oppositeOf = expr;
	}
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.NEGATE;
	}
	
	public Expression getNegatedExpr(){
		return this.negatedExpr;
	}
	
	/**
	 * 
	 * @return the thing that is to be negated - so Negate(1) gives 1
	 */
	public Expression getExpression(){
	    return this.oppositeOf;
	}
}
