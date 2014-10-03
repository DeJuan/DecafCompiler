package edu.mit.compilers.ir;

/**
 * IR_Node to load a parameter variable.
 * 
 */
public class IR_LDP extends IR_Node {
    private IR_Var parameter;

    public IR_LDP(IR_Var parameter){
        this.parameter = parameter;
    }
    
    public IR_Var getParameter(){
        return parameter;
    }
    
    @Override
    public Type evaluateType() {
        return parameter.evaluateType();
    }

    @Override
    public String toString() {
        return "Loading " + parameter.toString();
    }

    @Override
    public boolean isValid() {
        return evaluateType() == Type.BOOL || evaluateType() == Type.INT;
    }

}
