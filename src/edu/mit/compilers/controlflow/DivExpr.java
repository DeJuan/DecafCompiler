package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Ops;

public class DivExpr extends BinExpr {
	
	public DivExpr(Expression lhs, Ops op, Expression rhs) {
		super(lhs, op, rhs);
		if (op != Ops.DIVIDE){
			throw new UnsupportedOperationException("You tried to initialize a DivExpr with an invalid operator.");
		}
	}

	@Override
	public ExpressionType getExprType() {
		return ExpressionType.DIV_EXPR;
	}
	
	
}
