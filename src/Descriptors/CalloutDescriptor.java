package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;


public class CalloutDescriptor extends Descriptor{
	private Type type;
	private String name;
	
	/**
	 * This is the constructor for a CalloutDescriptor.
	 * It simply takes in the name of the callout and stores it.
	 * @param name
	 */
	public CalloutDescriptor(String name)
	{
		this.type = Type.CALLOUT;
		this.name = name;
	}
	@Override
	public long getLength()
	{
		System.err.println("Callouts are handled by C, not our code.");
		throw new UnsupportedOperationException();
	}

	@Override
	public ArrayList<Boolean> getArgTypes() {
		System.err.println("Callouts are handled by C, not our code.");
		throw new UnsupportedOperationException();
	}

	@Override
	public String getReturnType() {
		System.err.println("Callouts are handled by C, not our code.");
		throw new UnsupportedOperationException();
	}

	@Override
	public Type getType() {
		return this.type;
	}
	public String getName(){
		return this.name;
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


/*
	@Override
	IRNode getIR() {
		// TODO Auto-generated method stub
		return null;
	}
*/

