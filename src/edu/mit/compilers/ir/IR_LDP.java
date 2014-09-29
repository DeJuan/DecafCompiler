package edu.mit.compilers.ir;

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
        // TODO Auto-generated method stub
        return parameter.evaluateType();
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "Loading " + parameter.toString();
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return evaluateType() == Type.BOOL || evaluateType() == Type.INT;
    }

}
