package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Ops;

public class CondExpr extends BinExpr {
	public Ops operator;
	
	public CondExpr(Expression lhs, Ops op, Expression rhs) {
		super(lhs, op, rhs);
		if(op != Ops.AND && op != Ops.OR){
			throw new UnsupportedOperationException("You initialized a CondExpr without an AND or OR operator.");
		}
		this.operator = op;
	}
	
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.COND_EXPR;
	}
	
}
