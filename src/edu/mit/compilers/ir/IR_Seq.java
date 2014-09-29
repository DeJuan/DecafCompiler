package edu.mit.compilers.ir;

import java.util.ArrayList;
import java.util.List;

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
        // TODO Auto-generated method stub
        return Type.NONE;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        String concat = "";
        for (IR_Node child: statements) {
            concat += child.toString() + System.getProperty("line.separator");
        }
        return concat;
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return true;
    }

}
