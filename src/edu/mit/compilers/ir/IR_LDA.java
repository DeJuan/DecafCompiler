package edu.mit.compilers.ir;

public class IR_LDA extends IR_Node {
    private IR_Var array;
    private IR_Node index_expr;
    
    public IR_LDA(IR_Var array, IR_Node index_expr) {
        this.array = array;
        this.index_expr = index_expr;
    }
    
    public IR_Var getArray() {
        return array;
    }
    
    public IR_Node getIndexExpr() {
        return index_expr;
    }

    @Override
    public Type evaluateType() {
        // TODO Auto-generated method stub
        if (array.evaluateType() == Type.BOOLARR) {
            return Type.BOOL;
        } else if (array.evaluateType() == Type.INTARR) {
            return Type.INT;
        } else {
            // This should NEVER happen if code is written right.
            throw new Error("Mistakes were made: LDA has a nonarray type as its array");
        }
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "Loading from array " + array.toString();
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return index_expr.evaluateType() == Type.INT && (array.evaluateType() == Type.BOOLARR || array.evaluateType() == Type.INTARR);
    }

}
