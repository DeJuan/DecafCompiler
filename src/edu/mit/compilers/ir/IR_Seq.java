package edu.mit.compilers.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * IR_Node that contains a list of statements to execute in a sequence.
 * Commonly used in blocks or parameters where the number of statements is unknown ahead of time.
 * 
 */
public class IR_Seq extends IR_Node {
    private List<IR_Node> statements = new ArrayList<IR_Node>();
    
    public IR_Seq(List<IR_Node> statements){
        this.statements = statements;
    }
    
    public List<IR_Node> getStatements(){
        return statements;
    }

    @Override
    public Type evaluateType() {
        return Type.NONE;
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
