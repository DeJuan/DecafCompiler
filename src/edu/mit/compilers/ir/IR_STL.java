package edu.mit.compilers.ir;

public class IR_STL extends IR_Node {
    private IR_Var local_var;
    private IR_Node value;
    
    public IR_STL(IR_Var local_var, IR_Node value) {
        this.local_var = local_var;
        this.value = value;
    }

    @Override
    public Type evaluateType() {
        // TODO Auto-generated method stub
        return Type.NONE;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "Storing " + value.toString() + " to " + local_var.toString();
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return local_var.evaluateType() == value.evaluateType() && (local_var.evaluateType() == Type.INT || local_var.evaluateType() == Type.BOOL);
    }

}
