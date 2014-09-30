package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;
import Descriptors.Descriptor.Type;

public class BoolArrayDescriptor extends Descriptor{

	private Type type;
	private int len;
	///@TODO: remove this and related code//
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
 * It throws a RuntimeException if you give a non-positive length.
 * 
 * This is the constructor you should call for something like boolean[10] y; .
 * 
 * @param length : int representing how long this array should be
 * @throws RuntimeException when given non-positive length
 */
	public BoolArrayDescriptor(int length) throws RuntimeException
	{
		try{
			assert length > 0;
			this.len = length;
			this.type = Type.BOOL_ARRAY; //inherited from superclass Descriptor
			for (int i = 0; i < length; i++){ //For each boolean space in the array
				contents.add(new BoolDescriptor(false)); //Create a new boolean descriptor set to false, and put it in the local array list.
			}
		}
		catch(AssertionError a)
		{
			System.err.println("You just tried to declare a boolean array of either negative length or 0.");
			throw new RuntimeException("BoolArrayDescriptor initialization attempted with non-positive argument");
		}
	}
	
	@Override
	public int getLength() throws UnsupportedOperationException {
		return len;
	}

	@Override
	public ArrayList<Boolean> getArgTypes() throws UnsupportedOperationException {
		System.err.println("An boolean array does not take arguments like a method.");
		throw new UnsupportedOperationException("An boolean array does not take arguments like a method.");
	}

	@Override
	public String getReturnType() throws UnsupportedOperationException {
		System.err.println("An boolean array does not have a return type, unlike a method.");
		throw new UnsupportedOperationException("An boolean array does not have a return type, unlike a method.");
	}

	@Override
	public Type getType() throws UnsupportedOperationException {
		return type;
	}

	@Override
	public IR_Node getIR() throws java.lang.UnsupportedOperationException {
		System.err.println("A boolean array does not keep a record of its IR_Node.");
		throw new UnsupportedOperationException("A boolean array does not keep a record of its IR_Node.");
	}
	

}
