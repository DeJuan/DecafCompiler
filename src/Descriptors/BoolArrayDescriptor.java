package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;

public class BoolArrayDescriptor extends Descriptor{

	private Type type;
	private long len;
	
/**
 * This is the constructor for a boolean array descriptor.
 * This constructor assumes that the array has been declared, but not initialized; only the length of the array has been finalized.
 * It takes in this desired length as its lone parameter, and initializes the array to contain all "false" values.
 * It throws a RuntimeException if you give a non-positive length.
 * 
 * This is the constructor you should call for something like boolean[10] y; .
 * 
 * @param length : long representing how long this array should be
 * @throws RuntimeException when given non-positive length
 */
	public BoolArrayDescriptor(long length)
	{
		this.len = length;
		this.type = Descriptor.Type.BOOL_ARRAY; //inherited from superclass Descriptor
	}
	
	@Override
	public long getLength() {
		return len;
	}
	public ArrayList<Boolean> getArgTypes() throws UnsupportedOperationException {
		System.err.println("An boolean array does not take arguments like a method.");
		throw new UnsupportedOperationException("An boolean array does not take arguments like a method.");
	}

	@Override
	public String getReturnType() throws UnsupportedOperationException {
		System.err.println("An boolean array does not have a return type, unlike a method.");
		throw new UnsupportedOperationException("An boolean array does not have a return type, unlike a method.");
	}

	@Override
	public Type getType() {
		return type;
	}

    @Override
    public void setIR(IR_Node IR) {
        throw new UnsupportedOperationException();
        
    }
	@Override
	public IR_Node getIR() throws java.lang.UnsupportedOperationException {
		System.err.println("A boolean array does not keep a record of its IR_Node.");
		throw new UnsupportedOperationException("A boolean array does not keep a record of its IR_Node.");
	}

}
