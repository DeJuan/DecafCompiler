package edu.mit.compilers.ir;

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
    public Type evaluateType() {
        // TODO Auto-generated method stub
        return true_expr.evaluateType();
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "if " + condition.toString() + " then " + true_expr.toString() + " else " + false_expr.toString();
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return condition.evaluateType() == Type.BOOL && true_expr.evaluateType() == false_expr.evaluateType();
    }

}
