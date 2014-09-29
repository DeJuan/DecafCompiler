package Descriptors;

import java.util.ArrayList;

import javax.activity.InvalidActivityException;

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
	public int getLength() throws InvalidActivityException{
		System.err.println("An int is 8 bytes, but does not have a length field.");
		throw new InvalidActivityException();
	}

	@Override
	public ArrayList<Boolean> getArgTypes() throws InvalidActivityException {
		System.err.println("An int doesn't have arguments.");
		throw new InvalidActivityException();
	}

	@Override
	public String getReturnType() throws InvalidActivityException{
		System.err.println("An int doesn't have a returnType.");
		throw new InvalidActivityException();
	}

	@Override
	public Type getType() {
		return this.type;
	}

	@Override
	public int getValue() throws InvalidActivityException {
		return this.value;
	}

	@Override
	public boolean getTruthValue() throws InvalidActivityException {
		System.err.println("An int doesn't have a truth value.");
		throw new InvalidActivityException();
	}

	@Override
	public void setValue(int index, int newValue) throws InvalidActivityException {
		System.err.println("An integer is immutable, and cannot have its value changed.");
		throw new InvalidActivityException();
	}

	@Override
	public void setValue(int index, boolean newValue) throws InvalidActivityException {
		System.err.println("An integer is immutable, and cannot have its value changed, especially not to a boolean!");
		throw new InvalidActivityException();
		
	}


}
