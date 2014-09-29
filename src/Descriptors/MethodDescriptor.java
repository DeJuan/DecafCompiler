package Descriptors;

import java.util.ArrayList;

import javax.activity.InvalidActivityException;



import Descriptors.Descriptor.ReturnType;
import Descriptors.Descriptor.Type;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class MethodDescriptor extends Descriptor {

	private String methodType;
	private String returnType;
	private ArrayList<Boolean> argumentTypes;
	private ReturnType type;
	
	
	/**
	 * This is the constructor for a new MethodDescriptor. 
	 * It takes in a string which denotes the return type of the method,
	 * and an arrayList of booleans that denote whether each argument is 
	 * an integer or a boolean. 
	 * If the supplied type string does not match a valid type, a runtime exception is thrown. 
	 * 
	 * @param type : A string indicating return type of method
	 * @param argTypes: ArrayList<Boolean> that indicates whether each arg is a bool or int
	 * @throws Exception
	 */
	public MethodDescriptor(String type, ArrayList<Boolean> argTypes) throws Exception
	{
		//TODO NOT SURE IF THIS IS THE BEST WAY TO HANDLE THIS, ASK GROUP
		this.returnType = type;
		switch(type)
		{
		
		case "bool": //Fall though, just here for safety
		case "boolean":
		this.type = ReturnType.BOOL;
		break;
		
		case "int":
		this.type = ReturnType.INT;
		break;
		
		case "void":
		this.type = ReturnType.VOID;
		break;
		
		default:
		System.err.print("Invalid string  supplied for method type.");
			Exception RuntimeException = null;
			throw RuntimeException;
		}
		
		
		this.argumentTypes = argTypes;
	}
	
	@Override
	public ArrayList<Boolean> getArgTypes() {
		return argumentTypes;
	}

	@Override
	public String getReturnType() {
		return this.returnType;
	}

	@Override
	public Type getType() throws InvalidActivityException {
		System.err.println("Methods do not have a type, but they have a returnType.");
		System.err.println("These are seperate types because they are based on different enums.");
		throw new InvalidActivityException();
	}


	@Override
	public int getLength() throws InvalidActivityException {
		System.err.println("Methods do not have a length in that sense.");
		throw new InvalidActivityException();
	}

	@Override
	public int getValue() throws InvalidActivityException {
		System.err.println("Methods do not have a value field like an integer.");
		throw new InvalidActivityException();
	}

	@Override
	public boolean getTruthValue() throws InvalidActivityException {
		System.err.println("Methods do not have a truth value.");
		throw new InvalidActivityException();
	}

	@Override
	public void setValue(int index, int newValue) throws InvalidActivityException {
		System.err.println("Methods do not have a value that can be set.");
		throw new InvalidActivityException();	
	}

	@Override
	public void setValue(int index, boolean newValue) throws InvalidActivityException {
		System.err.println("Methods do not have a value that can be set.");
		throw new InvalidActivityException();
	}
	
	//TODO INSERT IR METHODS
}
