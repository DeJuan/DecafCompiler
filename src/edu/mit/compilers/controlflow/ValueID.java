package edu.mit.compilers.controlflow;

public class ValueID {
    
    static int nextID = 0;
    private final String ID;
    
    public ValueID(){
        ID = "v" + nextID++;
    }
    
    public String getString(){
        return ID;
    }
}
