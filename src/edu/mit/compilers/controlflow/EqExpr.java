package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.Ops;

public class EqExpr extends BinExpr {
	private List<Ops> opChecker = new ArrayList<Ops>();
	
	public EqExpr(Expression lhs, Ops op, Expression rhs) {
		super(lhs, op, rhs);
		opChecker.add(Ops.EQUALS);
		opChecker.add(Ops.NOT_EQUALS);
		if (!opChecker.contains(op)){
			throw new UnsupportedOperationException("You initialized a EqExpr without a comparison operator.");
		}
	}

	@Override
	public ExpressionType getExprType() {
		return ExpressionType.EQ_EXPR;
	}
}
