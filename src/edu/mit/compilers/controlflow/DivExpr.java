package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.Ops;

public class DivExpr extends BinExpr {
    private List<Ops> opChecker = new ArrayList<Ops>();
    
    public DivExpr(Expression lhs, Ops op, Expression rhs) {
        super(lhs, op, rhs);
        opChecker.add(Ops.DIVIDE);
        if (!opChecker.contains(op)){
            throw new UnsupportedOperationException("You initialized a DivExpr with an invalid operator.");
        }
    }

    @Override
    public ExpressionType getExprType() {
        return ExpressionType.DIV_EXPR;
    }
}
