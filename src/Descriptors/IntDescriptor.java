package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;

public class IntDescriptor extends Descriptor{

	private final Type type;
	
	/**
	 * This is the constructor for an IntDescriptor. It simply takes in the value of the integer as its only parameter.
	 * @param val : The value of the integer this descriptor represents
	 */
	public IntDescriptor()
	{
		this.type = Type.INT;
	}
	
	@Override
	public int getLength() {
		System.err.println("An int is 8 bytes, but does not have a length field.");
		throw new UnsupportedOperationException();
	}

	@Override
	public ArrayList<Boolean> getArgTypes() {
		System.err.println("An int doesn't have arguments.");
		throw new UnsupportedOperationException();
	}

	@Override
	public String getReturnType() {
		System.err.println("An int doesn't have a returnType.");
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


}
