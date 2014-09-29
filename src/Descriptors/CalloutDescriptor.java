package Descriptors;

import java.util.ArrayList;

import javax.activity.InvalidActivityException;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
	public int getLength()
	{
		System.err.println("Callouts are handled by C, not our code.");
		throw new NotImplementedException();
	}

	@Override
	public ArrayList<Boolean> getArgTypes() {
		System.err.println("Callouts are handled by C, not our code.");
		throw new NotImplementedException();
	}

	@Override
	public String getReturnType() {
		System.err.println("Callouts are handled by C, not our code.");
		throw new NotImplementedException();
	}

	@Override
	public Type getType() {
		return this.type;
	}
	@Override
	public int getValue() throws InvalidActivityException {
		System.err.println("Callouts are handled by C, not our code.");
		throw new NotImplementedException();
	}
	@Override
	public boolean getTruthValue() throws InvalidActivityException {
		System.err.println("Callouts are handled by C, not our code.");
		throw new NotImplementedException();
	}
	
	public String getName(){
		return this.name;
	}
	@Override
	public void setValue(int index, int newValue) throws InvalidActivityException {
		System.err.println("Callouts are handled by C, not our code.");
		throw new NotImplementedException();		
	}
	@Override
	public void setValue(int index, boolean newValue) throws InvalidActivityException {
		System.err.println("Callouts are handled by C, not our code.");
		throw new NotImplementedException();
	}
	
}


/*
	@Override
	IRNode getIR() {
		// TODO Auto-generated method stub
		return null;
	}
*/

