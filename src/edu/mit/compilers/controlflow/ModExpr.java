package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Ops;

public class ModExpr extends BinExpr {
	
	
	public ModExpr(Expression lhs, Ops op, Expression rhs) {
		super(lhs, op, rhs);
		if (op != Ops.MOD){
			throw new UnsupportedOperationException("You initialized a ModExpr without a modulus operator.");
		}
	}

	@Override
	public ExpressionType getExprType() {
		return ExpressionType.MOD_EXPR;
	}
}
