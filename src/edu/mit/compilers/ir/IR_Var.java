package edu.mit.compilers.ir;


public class IR_Var extends IR_Node {
    private String name;
    private Type type;
    private IR_Node index; // The index into the array, if indexing into.  null if not accessing the contents of an array.
    
    public IR_Var(String name, Type type, IR_Node index) {
        this.name = name;
        this.type = type;
        this.index = index;
    }
    
    public String getName() {
        return name;
    }
    
    public IR_Node getIndex() {
        if (index == null) {
            throw new UnsupportedOperationException("tried to get the index into a variable when no index exists");
        }
        return index;
    }

    @Override
    public Type evaluateType() {
        return type;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
