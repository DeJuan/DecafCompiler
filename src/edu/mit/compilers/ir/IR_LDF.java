package edu.mit.compilers.ir;

public class IR_LDF extends IR_Node {
    private IR_Var field;
    
    public IR_LDF(IR_Var field) {
        this.field = field;
    }
    
    public IR_Var getField(){
        return field;
    }

    @Override
    public Type evaluateType() {
        return field.evaluateType();
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "Loading " + field.toString();
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return evaluateType() == Type.BOOL || evaluateType() == Type.INT;
    }

}
