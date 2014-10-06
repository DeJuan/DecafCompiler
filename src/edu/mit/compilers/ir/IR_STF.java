package edu.mit.compilers.ir;

/**
 * IR_Node to store a value to a field variable location.
 * 
 */
public class IR_STF extends IR_Node {
    private IR_Var field;
    private IR_Node value;
    
    public IR_STF(IR_Var field, IR_Node value) {
        this.field = field;
        this.value = value;
    }
    
    public IR_Var getField() {
        return field;
    }
    
    public IR_Node getValue() {
        return value;
    }

    @Override
    public Type getType() {
        return Type.VOID;
    }

    @Override
    public String toString() {
        return "Storing to " + field.toString();
    }

    @Override
    public boolean isValid() {
        return field.getType() == value.getType() && (field.getType() == Type.BOOL || field.getType() == Type.INT);
    }

}
