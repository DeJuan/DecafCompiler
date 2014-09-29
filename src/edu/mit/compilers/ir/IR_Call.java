package edu.mit.compilers.ir;

import java.util.ArrayList;
import java.util.List;

public class IR_Call extends IR_Node {
    private String name;
    private Type type;
    private List<IR_Node> args = new ArrayList<IR_Node>();

    
    public IR_Call(String name, Type type, List<IR_Node> args) {
        this.name = name;
        this.type = type;
        this.args = args;
    }
    
    public String getName() {
        return name;
    }
    
    public List<IR_Node> getArgs() {
        return args;
    }
    
    @Override
    public Type evaluateType() {
        return type;
    }

    @Override
    public String toString() {
        return "Calling method " + name;
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
