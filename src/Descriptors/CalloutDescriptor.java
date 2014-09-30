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
	public int getLength(){
		System.err.println("Callouts are handled by C, not our code.");
		throw new UnsupportedOperationException("Callouts are handled by C, not our code.");
	}

	@Override
	public ArrayList<Boolean> getArgTypes() {
		System.err.println("Callouts are handled by C, not our code.");
		throw new UnsupportedOperationException("Callouts are handled by C, not our code.");
	}

	@Override
	public String getReturnType() {
		System.err.println("Callouts are handled by C, not our code.");
		throw new UnsupportedOperationException("Callouts are handled by C, not our code.");
	}

	@Override
	public Type getType() {
		return this.type;
	}
	
	public String getName(){
		return this.name;
	}
	
	@Override
	public IR_Node getIR() throws UnsupportedOperationException {
		System.err.println("A CalloutDescriptor does not locally store its IR node.");
		throw new UnsupportedOperationException("A CalloutDescriptor does not locally store its IR node.");
	}
	
}



