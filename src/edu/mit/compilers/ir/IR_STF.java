package edu.mit.compilers.ir;

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
    public Type evaluateType() {
        // TODO Auto-generated method stub
        return Type.NONE;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "Storing to " + field.toString();
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return field.evaluateType() == value.evaluateType() && (field.evaluateType() == Type.BOOL || field.evaluateType() == Type.INT);
    }

}
