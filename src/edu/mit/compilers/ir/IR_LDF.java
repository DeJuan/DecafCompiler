package edu.mit.compilers.ir;

/**
 * IR_Node to load a field variable.
 * 
 */
public class IR_LDF extends IR_Node {
    private IR_Var field;
    
    public IR_LDF(IR_Var field) {
        this.field = field;
    }
    
    public IR_Var getField(){
        return field;
    }

    @Override
    public Type getType() {
        return field.getType();
    }

    @Override
    public String toString() {
        return "Loading " + field.toString();
    }

}
