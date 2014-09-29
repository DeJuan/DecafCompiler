package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;

public class IntDescriptor extends Descriptor{

	private final int value;
	private final Type type;
	
	/**
	 * This is the constructor for an IntDescriptor. It simply takes in the value of the integer as its only parameter.
	 * @param val : The value of the integer this descriptor represents
	 */
	public IntDescriptor(int val)
	{
		this.value = val;
		this.type = Type.INT;
	}
	
	@Override
	public int getLength() throws UnsupportedOperationException{
		System.err.println("An int is 8 bytes, but does not have a length field.");
		throw new UnsupportedOperationException("An int is 8 bytes, but does not have a length field.");
	}

	@Override
	public ArrayList<Boolean> getArgTypes() throws UnsupportedOperationException {
		System.err.println("An int doesn't have arguments.");
		throw new UnsupportedOperationException("An int doesn't have arguments.");
	}

	@Override
	public String getReturnType() throws UnsupportedOperationException{
		System.err.println("An int doesn't have a returnType.");
		throw new UnsupportedOperationException("An int doesn't have a returnType.");
	}

	@Override
	public Type getType() {
		return this.type;
	}

	@Override
	public int getValue(){
		return this.value;
	}

	@Override
	public boolean getTruthValue() throws UnsupportedOperationException {
		System.err.println("An int doesn't have a truth value.");
		throw new UnsupportedOperationException("An int doesn't have a truth value.");
	}

	@Override
	public void setValue(int index, int newValue) throws UnsupportedOperationException {
		System.err.println("An integer is immutable, and cannot have its value changed.");
		throw new UnsupportedOperationException("An integer is immutable, and cannot have its value changed.");
	}

	@Override
	public void setValue(int index, boolean newValue) throws UnsupportedOperationException {
		System.err.println("An integer is immutable, and cannot have its value changed, especially not to a boolean!");
		throw new UnsupportedOperationException("An integer is immutable, and cannot have its value changed, especially not to a boolean!");
		
	}

	@Override
	public IR_Node getIR() throws UnsupportedOperationException {
		System.err.println("An int doesn't keep its IR_Node.");
		throw new UnsupportedOperationException("An int doesn't keep its IR_Node.");
	}


}
