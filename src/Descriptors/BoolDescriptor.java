package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;

public class BoolDescriptor extends Descriptor{
	private final Type type;
	
	/**
	 * This is the constructor for a BoolDescriptor. 
	 * It simply takes in the truth value of the boolean it will represent as its parameter.
	 * @param value
	 */
	public BoolDescriptor(){
		this.type = Type.BOOL;
	}
    @Override
	public long getLength() throws UnsupportedOperationException {
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
    public void setIR(IR_Node IR) {
        throw new UnsupportedOperationException();
    }
	
	@Override
	public IR_Node getIR() throws UnsupportedOperationException {
		System.err.println("A boolean does not keep a record of its IR_Node.");
		throw new UnsupportedOperationException("A boolean does not keep a record of its IR_Node.");
	}

}
