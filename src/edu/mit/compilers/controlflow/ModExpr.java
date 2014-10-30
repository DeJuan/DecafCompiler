package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.Ops;

public class ModExpr extends BinExpr {
    private List<Ops> opChecker = new ArrayList<Ops>();
    
    public ModExpr(Expression lhs, Ops op, Expression rhs) {
        super(lhs, op, rhs);
        opChecker.add(Ops.MOD);
        if (!opChecker.contains(op)){
            throw new UnsupportedOperationException("You initialized a ModExpr with an invalid operator.");
        }
    }

    @Override
    public ExpressionType getExprType() {
        return ExpressionType.MOD_EXPR;
    }
}