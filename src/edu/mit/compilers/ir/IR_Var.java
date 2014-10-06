package edu.mit.compilers.ir;

/**
 * IR_Node that represents a variable.
 * The constructor takes in a name, a type, and an index.
 * If the variable is not an array, then index is null. Otherwise, it represents an expression.
 *
 */
public class IR_Var extends IR_Node {
    private String name;
    private Type type;
    private IR_Node index; // The index into the array, if indexing into.  null if not accessing the contents of an array.
    
    public IR_Var(Type type, String name, IR_Node index) {
        this.name = name;
        this.type = type;
        this.index = index;
    }
    
    public String getName() {
        return name;
    }
    
    public IR_Node getIndex() {
        return index;
    }

    @Override
    public Type getType() {
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
