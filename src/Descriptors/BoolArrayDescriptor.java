package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;

public class BoolArrayDescriptor extends Descriptor{

	private Type type;
	private long len;
	
/**
 * This is the second constructor for a boolean array descriptor.
 * This constructor assumes that the array has been declared, but not initialized; only the length of the array has been finalized.
 * It takes in this desired length as its lone parameter, and initializes the array to contain all "false" values.
 * 
 * This is the constructor you should call for something like boolean[10] y; .
 * 
 * @param length : int representing how long this array should be
 * 
 */
	public BoolArrayDescriptor(long length)
	{
		this.len = length;
		this.type = Type.BOOL_ARRAY; //inherited from superclass Descriptor
	}
	
	@Override
	public long getLength() {
		return len;
	}

	@Override
	public ArrayList<Boolean> getArgTypes() {
		System.err.println("An boolean array does not take arguments like a method.");
		throw new UnsupportedOperationException();
	}

	@Override
	public String getReturnType() {
		System.err.println("An boolean array does not have a return type, unlike a method.");
		throw new UnsupportedOperationException();
	}

	@Override
	public Type getType() {
		return type;
	}
	//TODO INSERT IR METHODS

    @Override
    public IR_Node getIR() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIR(IR_Node IR) {
        throw new UnsupportedOperationException();
        
    }

}
