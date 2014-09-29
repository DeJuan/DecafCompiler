package edu.mit.compilers.ir;

public class IR_Var extends IR_Node {
    private String name;
    private Type type;
    
    public IR_Var(String name, Type type) {
        this.name = name;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public Type evaluateType() {
        // TODO Auto-generated method stub
        return type;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return name;
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return true;
    }

}
