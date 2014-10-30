package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.Ops;

public class MultExpr extends BinExpr {
	private List<Ops> opChecker = new ArrayList<Ops>();
	
	public MultExpr(Expression lhs, Ops op, Expression rhs) {
		super(lhs, op, rhs);
		opChecker.add(Ops.TIMES);
		if (!opChecker.contains(op)){
			throw new UnsupportedOperationException("You tried to initialize a MultExpr with an invalid operator.");
		}
	}

	@Override
	public ExpressionType getExprType() {
		return ExpressionType.MULT_EXPR;
	}
	
	
}
