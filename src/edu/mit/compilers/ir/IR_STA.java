package edu.mit.compilers.ir;

/**
 * IR_Node to store a value to an array location.
 * 
 */
public class IR_STA extends IR_Node {
    private IR_Var array;
    private IR_Node value;
    
    public IR_STA(IR_Var array, IR_Node value) {
        this.array = array;
        this.value = value;
    }
    
    public IR_Var getArray() {
        return array;
    }
    
    public IR_Node getIndexExpr() {
        return array.getIndex();
    }
    
    public IR_Node getValue() {
        return value;
    }

    @Override
    public Type evaluateType() {
        return Type.NONE;
    }

    @Override
    public String toString() {
        return "Storing " + value.toString() + " to array " + array.toString();
    }

    @Override
    public boolean isValid() {
        return array.getIndex().evaluateType() == Type.INT 
                && ((array.evaluateType() == Type.INTARR && value.evaluateType() == Type.INT) 
                        || (array.evaluateType() == Type.BOOLARR && value.evaluateType() == Type.BOOL));
    }

}
