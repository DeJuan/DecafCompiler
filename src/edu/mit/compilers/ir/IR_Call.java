package edu.mit.compilers.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * IR_Node for method calls. 
 * Grammar: method_name( [expr+] ) | method_name( [callout_args+] )
 * The constructor takes in the name of the method, the type, and a list of IR_Nodes 
 * as the argument.
 *
 */
public class IR_Call extends IR_Node {
    private String name;
    private Type type;
    private List<IR_Node> args;
 
    public IR_Call(Type type, String name) {
        this.name = name;
        this.type = type;
        args=new ArrayList<IR_Node>();
    }
    
    public String getName() {
        return name;
    }
    
    public void addArg(IR_Node a){
    	args.add(a);
    }
    
    public List<IR_Node> getArgs() {
        return args;
    }
    
    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Calling method " + name;
    }
}
