package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;

public class BoolDescriptor extends Descriptor{
	private final Type type;
	private final boolean truthValue;
	
	/**
	 * This is the constructor for a BoolDescriptor. 
	 * It simply takes in the truth value of the boolean it will represent as its parameter.
	 * @param value
	 */
	public BoolDescriptor(boolean truthValue)
	{
		this.type = Type.BOOL;
		this.truthValue = truthValue;
	}
	
	@Override
	public int getLength() throws UnsupportedOperationException {
		System.err.println("A boolean does not have a length.");
		throw new UnsupportedOperationException("A boolean does not have a length.");
	}

	@Override
	public ArrayList<Boolean> getArgTypes() throws UnsupportedOperationException {
		System.err.println("A boolean does not have arguments.");
		throw new UnsupportedOperationException("A boolean does not have arguments.");
	}

	@Override
	public String getReturnType() throws UnsupportedOperationException {
		System.err.println("A boolean does not have a return value.");
		throw new UnsupportedOperationException("A boolean does not have a return value.");
	}

	@Override
	public Type getType() {
		return this.type;
	}

	@Override
	public int getValue() throws UnsupportedOperationException {
		System.err.println("A boolean does not have a numerical value in this context.");
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getTruthValue(){
		return this.truthValue;
	}

	@Override
	public void setValue(int index, int newValue) throws UnsupportedOperationException {
		System.err.println("A boolean is immutable, and definitely should not be getting changed to an int!");
		throw new UnsupportedOperationException("A boolean is immutable, and definitely should not be getting changed to an int!");
		
	}

	@Override
	public void setValue(int index, boolean newValue) throws UnsupportedOperationException {
		System.err.println("A boolean is immutable, so you may not set a new value for it. Make a new boolean instead.");
		throw new UnsupportedOperationException("A boolean is immutable, so you may not set a new value for it. Make a new boolean instead.");
	}

	@Override
	public IR_Node getIR() throws UnsupportedOperationException {
		System.err.println("A boolean does not keep a record of its IR_Node.");
		throw new UnsupportedOperationException("A boolean does not keep a record of its IR_Node.");
	}

}
