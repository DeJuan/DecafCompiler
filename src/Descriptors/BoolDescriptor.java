package Descriptors;

import java.util.ArrayList;

import javax.activity.InvalidActivityException;

public class BoolDescriptor extends Descriptor{
	private final Type type;
	private final boolean truthValue;
	
	/**
	 * This is the constructor for a BoolDescriptor. 
	 * It simply takes in the truth value of the boolean it will represent as its parameter.
	 * @param value
	 */
	public BoolDescriptor(boolean truthValue){
		this.type = Type.BOOL;
		this.truthValue = truthValue;
	}
	
	@Override
	public int getLength() throws InvalidActivityException {
		System.err.println("A boolean does not have a length.");
		throw new InvalidActivityException();
	}

	@Override
	public ArrayList<Boolean> getArgTypes() {
		System.err.println("A boolean does not have arguments.");
		throw new UnsupportedOperationException();
	}

	@Override
	public String getReturnType() {
		System.err.println("A boolean does not have a return value.");
		throw new UnsupportedOperationException();
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return this.type;
	}

	@Override
	public int getValue() throws InvalidActivityException {
		System.err.println("A boolean does not have a numerical value in this context.");
		throw new InvalidActivityException();
	}

	@Override
	public boolean getTruthValue() throws InvalidActivityException {
		// TODO Auto-generated method stub
		return this.truthValue;
	}

	@Override
	public void setValue(int index, int newValue) throws InvalidActivityException {
		System.err.println("A boolean is immutable, and definitely should not be getting changed to an int!");
		throw new InvalidActivityException();
		
	}

	@Override
	public void setValue(int index, boolean newValue) throws InvalidActivityException {
		System.err.println("A boolean is immutable, so you may not set a new value for it. Make a new boolean instead.");
		throw new InvalidActivityException();
	}
	
	//TODO INSERT IR METHODS
}
