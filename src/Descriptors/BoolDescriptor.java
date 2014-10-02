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
	public long getLength() {
		System.err.println("A boolean does not have a length.");
		throw new UnsupportedOperationException();
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
