package edu.mit.compilers.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * IR_Node that contains a list of statements to execute in a sequence.
 * Commonly used in blocks or parameters where the number of statements is unknown ahead of time.
 * 
 */
public class IR_Seq extends IR_Node {
    private List<IR_Node> statements;
    
    public IR_Seq(){
        statements = new ArrayList<IR_Node> ();
    }
    
    public void addNode(IR_Node n){
    	statements.add(n);
    }
    
    public void addNodes(ArrayList<IR_Node> s){
    	statements.addAll(s);
    }
    
    public List<IR_Node> getStatements(){
        return statements;
    }

    @Override
    public Type getType() {
        return Type.VOID;
    }

    @Override
    public String toString() {
        String concat = "";
        for (IR_Node child: statements) {
            concat += child.toString() + System.getProperty("line.separator");
        }
        return concat;
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
