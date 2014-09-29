package edu.mit.compilers.ir;

public class IR_LDL extends IR_Node {
    private IR_Var local_var;
    
    public IR_LDL(IR_Var local_var){
        this.local_var = local_var;
    }
    
    public IR_Var getLocalVar(){
        return local_var;
    }

    @Override
    public Type evaluateType() {
        // TODO Auto-generated method stub
        return local_var.evaluateType();
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "Loading " + local_var.toString();
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return evaluateType() == Type.BOOL || evaluateType() == Type.INT;
    }

}
