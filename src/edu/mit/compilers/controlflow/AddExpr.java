package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.Ops;

public class AddExpr extends BinExpr {
	private List<Ops> opChecker = new ArrayList<Ops>();
	
	public AddExpr(Expression lhs, Ops op, Expression rhs) {
		super(lhs, op, rhs);
		opChecker.add(Ops.MINUS);
		opChecker.add(Ops.PLUS);
		if (!opChecker.contains(op)){
			throw new UnsupportedOperationException("You initialized an AddExpr with an invalid operator.");
		}
	}

	@Override
	public ExpressionType getExprType() {
		return ExpressionType.ADD_EXPR;
	}
}

