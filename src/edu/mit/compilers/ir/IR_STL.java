package edu.mit.compilers.ir;

/**
 * IR_Node to store a value to a local variable location.
 * 
 */
public class IR_STL extends IR_Node {
    private IR_Var local_var;
    private IR_Node value;
    
    public IR_STL(IR_Var local_var, IR_Node value) {
        this.local_var = local_var;
        this.value = value;
    }

    @Override
    public Type getType() {
        return Type.VOID;
    }

    @Override
    public String toString() {
        return "Storing " + value.toString() + " to " + local_var.toString();
    }
}
