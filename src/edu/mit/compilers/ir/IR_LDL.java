package edu.mit.compilers.ir;

/**
 * IR_Node to load a local variable.
 * 
 */
public class IR_LDL extends IR_Node {
    private IR_Var local_var;
    
    public IR_LDL(IR_Var local_var){
        this.local_var = local_var;
    }
    
    public IR_Var getLocalVar(){
        return local_var;
    }

    @Override
    public Type getType() {
        return local_var.getType();
    }

    @Override
    public String toString() {
        return "Loading " + local_var.toString();
    }

}
