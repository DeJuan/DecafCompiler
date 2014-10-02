package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;

public class IntArrayDescriptor extends Descriptor{

	private final long arrayLength;
	private final Type type;
	
	/**
	 * This is the second constructor for an IntArrayDescriptor.
	 * This constructor assumes that the array has been declared, but not initialized; only the length of the array has been finalized.
	 * It takes in this desired length as its lone parameter, and zero-initializes the array.
	 * 
	 * This is the constructor you should call for something like int[10] x; .
	 * 
	 * @param length : int representing how long this array should be
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

	@Override
	public ArrayList<Boolean> getArgTypes() {
		System.err.println("An integer array does not take arguments like a method.");
		throw new UnsupportedOperationException();
	}

	@Override
	public String getReturnType() {
		System.err.println("An integer array does not have a return type.");
		throw new UnsupportedOperationException();
	}

	@Override
	public Type getType() {
		return this.type;
	}

    @Override
    public IR_Node getIR() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIR(IR_Node IR) {
        throw new UnsupportedOperationException();
    }
	
	//TODO INSERT IR METHODS

}
