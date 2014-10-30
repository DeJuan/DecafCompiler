package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.Ops;

public class CondExpr extends BinExpr {
	private List<Ops> opChecker = new ArrayList<Ops>();
	
	public CondExpr(Expression lhs, Ops op, Expression rhs) {
		super(lhs, op, rhs);
		if(op != Ops.AND && op != Ops.OR){
			throw new UnsupportedOperationException("You initialized a CondExpr without an AND or OR operator.");
		}
	}
	
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.COND_EXPR;
	}
}
