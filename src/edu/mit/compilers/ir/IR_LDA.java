package edu.mit.compilers.ir;

/**
 * IR_Node to load an array.
 * 
 */
public class IR_LDA extends IR_Node {
    private IR_Var array;
    
    public IR_LDA(IR_Var array) {
        this.array = array;
    }
    
    public IR_Var getArray() {
        return array;
    }
    
    public IR_Node getIndexExpr() {
        return array.getIndex();
    }

    @Override
    public Type getType() {
        if (array.getType() == Type.BOOLARR) {
            return Type.BOOL;
        } else if (array.getType() == Type.INTARR) {
            return Type.INT;
        } else {
            // This should NEVER happen if code is written right.
            throw new Error("Mistakes were made: LDA has a nonarray type as its array");
        }
    }

    @Override
    public String toString() {
        return "Loading from array " + array.toString();
    }

}
