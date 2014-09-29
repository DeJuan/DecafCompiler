package Descriptors;

import java.util.ArrayList;

import javax.activity.InvalidActivityException;

import Descriptors.Descriptor.Type;

public class BoolArrayDescriptor extends Descriptor{

	private Type type;
	private int len;
	private ArrayList<BoolDescriptor> contents;
	
	/**
	 * This is the first constructor for a boolean array descriptor. 
	 * This constructor assumes that the boolean array is being declared and initialized in the same line, with actual truth values.
	 * It takes in a parameter, values, which is just an boolean array containing the truth values this array should contain.
	 * 
	 * This is the constructor you should call for something like bool[] y = {true, false, true}; .
	 * 
	 * Note that it is a normal boolean array (boolean[]), not an ArrayList<Boolean>!
	 * @param values : boolean[] containing the desired truth values for this array
	 */
	public BoolArrayDescriptor(boolean[] values)
	{
		this.len = values.length;
		this.type = Type.BOOL_ARRAY; //inherited from superclass Descriptor 
		for (int i = 0; i < values.length; i++) //For each boolean in the array
		{
			contents.add(new BoolDescriptor(values[i])); //Create a new descriptor and put it in the local array list
		}
	}
	
/**
 * This is the second constructor for a boolean array descriptor.
 * This constructor assumes that the array has been declared, but not initialized; only the length of the array has been finalized.
 * It takes in this desired length as its lone parameter, and initializes the array to contain all "false" values.
 * 
 * This is the constructor you should call for something like boolean[10] y; .
 * 
 * @param length : int representing how long this array should be
 * 
 */
	public BoolArrayDescriptor(int length)
	{
		this.len = length;
		this.type = Type.BOOL_ARRAY; //inherited from superclass Descriptor
		//this.argTypes = argsRepresentation; Had been using this to hold the boolean array, then realized it wasn't needed if we're doing this. 
		for (int i = 0; i < length; i++) //For each boolean space in the array
		{
			contents.add(new BoolDescriptor(false)); //Create a new boolean descriptor set to false, and put it in the local array list.
		}
	}
	
	@Override
	public int getLength() throws InvalidActivityException {
		return len;
	}

	@Override
	public ArrayList<Boolean> getArgTypes() throws InvalidActivityException {
		System.err.println("An boolean array does not take arguments like a method.");
		throw new InvalidActivityException();
	}

	@Override
	public String getReturnType() throws InvalidActivityException {
		System.err.println("An boolean array does not have a return type, unlike a method.");
		throw new InvalidActivityException();
	}

	@Override
	public Type getType() throws InvalidActivityException {
		return type;
	}

	@Override
	public int getValue() throws InvalidActivityException {
		System.err.println("An boolean array does not hold values, it holds truth values.");
		System.err.println("Please use getTruthValue(int index) to access a truth value in this array.");
		throw new InvalidActivityException();
	}

	@Override
	public boolean getTruthValue() throws InvalidActivityException {
		System.err.println("An boolean array does not have a truth value as a whole, but its entries do. Specify an entry.");
		throw new InvalidActivityException();
	}
	
	public boolean getTruthValue(int index) throws InvalidActivityException {
		try{
			assert index < len;
			assert index >= 0;
			return contents.get(index).getTruthValue();
			}
		catch(AssertionError a){
			System.err.println("You are attempting to access an invalid array index, which is " + index + ".");
			System.err.println("The size of the boolean array you tried to access is " + len + ".");
			throw new InvalidActivityException();
			}
	}

	public void setValue(int index, boolean newTruthValue) throws InvalidActivityException
	{
		try{
			assert index < len;
			assert index >= 0;
			contents.set(index, new BoolDescriptor(newTruthValue));
		}
		catch(AssertionError a) //Catch if the index is invalid 
		{
			System.err.println("You are attempting to set an invalid array index, which is " + index + ".");
			System.err.println("The size of the boolean array you tried to change is " + len + ".");
			throw new InvalidActivityException();
		}
	}

	@Override
	public void setValue(int index, int newValue) throws InvalidActivityException {
		System.err.println("You may not set a boolean array value to an integer.");
		throw new InvalidActivityException();
		
	}
	
	//TODO INSERT IR METHODS

}
