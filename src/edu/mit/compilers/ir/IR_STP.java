package edu.mit.compilers.ir;

/**
 * IR_Node to store a value to a parameter location.
 * 
 */
public class IR_STP extends IR_Node {
    private IR_Var parameter;
    private IR_Node value;
    
    public IR_STP(IR_Var parameter, IR_Node value) {
        this.parameter = parameter;
        this.value = value;
    }
    
    public IR_Var getParameter() {
        return parameter;
    }
    
    public IR_Node getValue() {
        return value;
    }

    @Override
    public Type evaluateType() {
        return Type.NONE;
    }

    @Override
    public String toString() {
        return "Storing " + value.toString() + " to " + parameter.toString();
    }

    @Override
    public boolean isValid() {
        return parameter.evaluateType() == value.evaluateType() && (parameter.evaluateType() == Type.INT || parameter.evaluateType() == Type.BOOL);
    }

}
