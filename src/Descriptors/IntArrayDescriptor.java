package Descriptors;

import java.util.ArrayList;

import javax.activity.InvalidActivityException;

public class IntArrayDescriptor extends Descriptor{

	private final int arrayLength;
	private final Type type;
	private ArrayList<IntDescriptor> memberVariables;
	
	/**
	 * This is the first constructor for an IntArrayDescriptor. 
	 * This constructor assumes that the integer array is being declared and initialized in the same line, with definitive values.
	 * It takes in a parameter, args, which is just an int array containing the integers this array should contain.
	 * 
	 * This is the constructor you should call for something like int[] x = {1, 2, 3};
	 * 
	 * Note that it is a normal int array (int[]), not an ArrayList<int>!
	 * @param args : int[] containing the integers this array should hold
	 */
	public IntArrayDescriptor(int[] args)
	{
		this.arrayLength = args.length;
		this.type = Type.INT_ARRAY; //inherited from superclass Descriptor
		for (int i = 0; i < args.length; i++) //For each argument in the integer array
		{
			memberVariables.add(new IntDescriptor(args[i])); //Create a new integer descriptor and put it in the local array list
		}
	}
	
	/**
	 * This is the second constructor for an IntArrayDescriptor.
	 * This constructor assumes that the array has been declared, but not initialized; only the length of the array has been finalized.
	 * It takes in this desired length as its lone parameter, and zero-initializes the array.
	 * 
	 * This is the constructor you should call for something like int[10] x; .
	 * 
	 * @param length : int representing how long this array should be
	 */
	public IntArrayDescriptor(int length)
	{
		this.arrayLength = length;
		this.type = Type.INT_ARRAY;
		for (int i = 0; i < length; i++) //For each argument in the integer array
		{
			memberVariables.add(new IntDescriptor(0)); //Create a new integer descriptor initialized to 0 and put it in the local array list
		}
	}
	
	@Override
	public int getLength() throws InvalidActivityException {
		return this.arrayLength;
	}

	@Override
	public ArrayList<Boolean> getArgTypes() throws InvalidActivityException {
		System.err.println("An integer array does not take arguments like a method.");
		throw new InvalidActivityException();
	}

	@Override
	public String getReturnType() throws InvalidActivityException {
		System.err.println("An integer array does not have a return type.");
		throw new InvalidActivityException();
	}

	@Override
	public Type getType() {
		return this.type;
	}

	@Override
	public int getValue() throws InvalidActivityException {
		System.err.println("You cannot getValue on an int array as a whole without an index. Specify an index.");
		throw new InvalidActivityException();
	}
	
	public int getValue(int index) throws InvalidActivityException{	
		try{
		assert index < arrayLength;
		assert index >= 0;
		return memberVariables.get(index).getValue();
		}
		catch(AssertionError a){
			System.err.println("You are attempting to access an invalid array index, which was " + index + ".");
			System.err.println("The size of the array you tried to access is " + arrayLength + ".");
			throw new InvalidActivityException();
		}
		
	}

	@Override
	public boolean getTruthValue() throws InvalidActivityException {
		System.err.println("An integer array does not have a truth value, nor is one contained within.");
		throw new InvalidActivityException();
	}
	
	public void setValue(int index, int newValue){ //Arrays are mutable, so we need this. 
		try{
		assert index < arrayLength;
		assert index >= 0;
		memberVariables.set(index, new IntDescriptor(newValue)); //Ints, however, are not mutable, so we need to make a new descriptor.
		}
		catch(AssertionError a){ //Catch if the index is invalid
			System.err.println("You are attempting to change an invalid array index, which is " + index + ".");
			System.err.println("The size of the array you tried to change is " + arrayLength + ".");
		}
	}

	@Override
	public void setValue(int index, boolean newTruthValue) throws InvalidActivityException {
		System.err.println("You may not override an integer array value with a boolean.");
		throw new InvalidActivityException();
		
	}
	
	//TODO INSERT IR METHODS

}
