package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.Ops;

public class CompExpr extends BinExpr {
	private List<Ops> opChecker = new ArrayList<Ops>();
	public Ops operator;
	
	public CompExpr(Expression lhs, Ops op, Expression rhs) {
		super(lhs, op, rhs);
		opChecker.add(Ops.GT);
		opChecker.add(Ops.GTE);
		opChecker.add(Ops.LT);
		opChecker.add(Ops.LTE);
		if (!opChecker.contains(op)){
			throw new UnsupportedOperationException("You initialized a CompExpr without a comparison operator.");
		}
		this.operator = op;
	}

	@Override
	public ExpressionType getExprType() {
		return ExpressionType.COMP_EXPR;
	}
}
