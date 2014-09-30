package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;


public class IntArrayDescriptor extends Descriptor{

	private final int arrayLength;
	private final Type type;
	///@TODO: remove this and related code//
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
	 * If the length is non-positive, it throws a RuntimeException.
	 * 
	 * This is the constructor you should call for something like int[10] x; .
	 * 
	 * @param length : int representing how long this array should be
	 * @throws RuntimeException if given non-positive length
	 */
	public IntArrayDescriptor(int length) throws RuntimeException
	{
		try{
			assert length > 0;
			this.arrayLength = length;
			this.type = Type.INT_ARRAY;
			for (int i = 0; i < length; i++){ //For each argument in the integer array
				memberVariables.add(new IntDescriptor(0)); //Create a new integer descriptor initialized to 0 and put it in the local array list
			}
		}
		catch(AssertionError a){
			System.err.println("You just tried to initialize an int array descriptor with a negative length.");
			System.err.println("This is a critical error and one I cannot correct automatically.");
			throw new RuntimeException("You have tried to initialize an int array descriptor with a non-positive length.");
			
		}
	}
	
	@Override
	public int getLength() throws UnsupportedOperationException {
		return this.arrayLength;
	}

	@Override
	public ArrayList<Boolean> getArgTypes() throws UnsupportedOperationException {
		System.err.println("An integer array does not take arguments like a method.");
		throw new UnsupportedOperationException("An integer array does not take arguments like a method.");
	}

	@Override
	public String getReturnType() throws UnsupportedOperationException {
		System.err.println("An integer array does not have a return type.");
		throw new UnsupportedOperationException("An integer array does not have a return type.");
	}

	@Override
	public Type getType() {
		return this.type;
	}

	@Override
	public IR_Node getIR() throws UnsupportedOperationException {
		System.err.println("A integer array does not keep a record of its IR_Node.");
		throw new UnsupportedOperationException("A integer array does not keep a record of its IR_Node.");
	}
	

}
