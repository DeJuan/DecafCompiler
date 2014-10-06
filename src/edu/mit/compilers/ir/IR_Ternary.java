package edu.mit.compilers.ir;

/**
 * IR_Node for a ternary expression.
 * Grammar: expr ? expr : expr
 * Constructor takes in 3 expressions as IR_Nodes, matching the logic from the grammar above.
 * 
 */
public class IR_Ternary extends IR_Node {
    private IR_Node condition;
    private IR_Node true_expr;
    private IR_Node false_expr;
    
    public IR_Ternary(IR_Node condition, IR_Node true_expr, IR_Node false_expr) {
        this.condition = condition;
        this.true_expr = true_expr;
        this.false_expr = false_expr;
    }
    
    public IR_Node getCondition() {
        return condition;
    }
    
    public IR_Node getTrueExpr() {
        return true_expr;
    }
    
    public IR_Node getFalseExpr() {
        return false_expr;
    }

    @Override
    public Type getType() {
        return true_expr.getType();
    }

    @Override
    public String toString() {
        return "if " + condition.toString() + " then " + true_expr.toString() + " else " + false_expr.toString();
    }

    @Override
    public boolean isValid() {
        return condition.getType() == Type.BOOL && true_expr.getType() == false_expr.getType();
    }

}
