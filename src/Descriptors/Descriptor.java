package Descriptors;

import java.util.ArrayList;

import javax.activity.InvalidActivityException;

/**
 * THis class is an abstract representation of a Descriptor. It contains almost all of the methods that will be use in the 
 * descriptors that inherit from it, but since many descriptors don't need all of these fields, the fields not needed
 * will throw InvalidActivityExceptions for each unused method if it is called.
 * 
 * IR methods have yet to be implemented; these will be added after Monday. 
 * @author DeJuan
 *
 */
public abstract class Descriptor {
	
	public enum Type
	{
		CALLOUT("callout"), 
		METHOD("method"), 
		INT("int"), 
		BOOL("bool"),
		INT_ARRAY("int[]"),
		BOOL_ARRAY("bool[]");
		
		private String ID;

		private Type(String s){
			this.ID = s;
		}
		
		
	};
	
	
	public enum ReturnType
	{
		INT("int"), BOOL("bool"), VOID("void");
		
		private final String returnType;
		
		ReturnType(String s){
			this.returnType = s;
		}
		
	}
	
	
	private ArrayList<Boolean> argTypes;
	private int length;
	//TODO UNCOMMENT THIS WHEN THE IR IS READY
	//private IRNode IR;
	//TODO Insert enum RETURNTYPE and TYPE?
	/**
	 * This method gets the length of the object represented by the descriptor. 
	 * This is only used by Array descriptors.
	 * 
	 * @return integer : represents length of array
	 * @throws InvalidActivityException
	 */
	public abstract int getLength() throws InvalidActivityException;
	
	/**
	 * This may not actually be needed, but is here due to the group discussion we had about passing around
	 * types as a boolean array list. If needed, this will be used in MethodDescriptor.
	 * @return ArrayList<Boolean> : each entry is true for a boolean argument and false for an integer argument. 
	 * @throws InvalidActivityException for non methods.
	 */
	public abstract ArrayList<Boolean> getArgTypes() throws InvalidActivityException;
	
	/**
	 * This is also another action really only useful for MethodDescriptors. 
	 * It gives you a string representing the expected return type of the method. 
	 * @return "bool", "boolean", "int", or "void".
	 * @throws InvalidActivityException 
	 */
	public abstract String getReturnType() throws InvalidActivityException;
	
	/**
	 * This gives you the Type of the descriptor. This is used by the enums so that we have a set
	 * type that anything could be; if it is not one of these types, we have an error. 
	 * @return Type instance which describes what kind of descriptor is being used.
	 * @throws InvalidActivityException
	 */
	public abstract Type getType() throws InvalidActivityException;
	
	/**
	 * This is used for int and int array descriptors. It allows you to get the value
	 * of the integer and use it as you need, while keeping a final version locally stored inside the descriptor.
	 * @return integer equal to the value of the integer the descriptor represents
	 * @throws InvalidActivityException
	 */
	public abstract int getValue() throws InvalidActivityException;
	
	
	/**
	 * This is the equivalent of the getValue() method, but for booleans.
	 * These are seperate methods to make sure everything is very explicit, and so that later on,
	 * we don't confuse methods. It's also not possible to call both of these methods the same name,
	 * so this is a good name that specifies we're dealing with booleans.
	 * @return boolean true or false : the value of the boolean stored within the descriptor
	 * @throws InvalidActivityException
	 */
	public abstract boolean getTruthValue() throws InvalidActivityException;
	
	/**
	 * This method is used only in the integer array descriptors. 
	 * Even though the primitive type int is immutable, an array of integers is certainly mutable. 
	 * This method allows you to set a new value for any index in an int array.
	 * 
	 * 
	 * There is validity checking built in for the index, and an exception will be thrown if the index is invalid.
	 * It actually works by making a new IntDescriptor and replacing the previous one with the new one containing
	 * the desired value. 
	 * @param index : The index in the array you wish to update
	 * @param newValue : The integer you want to set the index to 
	 * @throws InvalidActivityException
	 */
	public abstract void setValue(int index, int newValue) throws InvalidActivityException;
	
	
	/**
	 * This method is only used in boolean array descriptors, and is the counterpart to the integer version.
	 * Booleans by themselves are immutable, but an array of them is certainly mutable. 
	 * This allows you to override a boolean in an array with a new truth value. 
	 * 
	 * There is validity checking built in for the index, and an exception will be thrown if an invalid index is received.
	 * The replacement actually makes a new BoolDescriptor and replaces the old one with the updated, newly created one. 
	 * @param index : The index in the array you wish to update
	 * @param newValue : the new truth value you wish to input
	 * @throws InvalidActivityException
	 */
	public abstract void setValue(int index, boolean newTruthValue) throws InvalidActivityException;
	
	//TODO UNCOMMENT THIS WHEN THE IR IS READY
	//public abstract IRNode getIR();
	
	

}
