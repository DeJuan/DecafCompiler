package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;

public class IntArrayDescriptor extends Descriptor{

	private final long arrayLength;
	private final Type type;
	
	/**
	 * This is the constructor for an IntArrayDescriptor.
	 * This constructor assumes that the array has been declared, but not initialized; only the length of the array has been finalized.
	 * It takes in this desired length as its lone parameter, and zero-initializes the array.
	 * If the length is non-positive, it throws a RuntimeException.
	 * 
	 * This is the constructor you should call for something like int[10] x; .
	 * 
	 * @param length : long representing how long this array should be
	 */
	public IntArrayDescriptor(long length)
	{
		this.arrayLength = length;
		this.type = Type.INT_ARRAY;
	}
	
	@Override
	public long getLength() {
		return this.arrayLength;
	}

	public ArrayList<Boolean> getArgTypes() throws UnsupportedOperationException {
		System.err.println("An integer array does not take arguments like a method.");
		throw new UnsupportedOperationException("An integer array does not take arguments like a method.");
	}

	@Override
	public String getReturnType() throws UnsupportedOperationException {
		System.err.println("An integer array does not have a return type.");
		throw new UnsupportedOperationException("An integer array does not have a return type.");
	}

	@Override
	public Type getType() {
		return this.type;
	}


    @Override
    public void setIR(IR_Node IR) {
        throw new UnsupportedOperationException();
    }
    
	@Override
	public IR_Node getIR() throws UnsupportedOperationException {
		System.err.println("A integer array does not keep a record of its IR_Node.");
		throw new UnsupportedOperationException("A integer array does not keep a record of its IR_Node.");
	}
	

}
