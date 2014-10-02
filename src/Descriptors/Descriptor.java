package Descriptors;

import java.util.List;

import edu.mit.compilers.ir.IR_Node;

/**
 * THis class is an abstract representation of a Descriptor. It contains almost all of the methods that will be use in the 
 * descriptors that inherit from it, but since many descriptors don't need all of these fields, the fields not needed
 * will throw UnsupportedOperationExceptions for each unused method if it is called.
 * 
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
	
	
	private List<Boolean> argTypes;
	private long length;
	//TODO UNCOMMENT THIS WHEN THE IR IS READY
	private IR_Node methodIRNode;
	//TODO Insert enum RETURNTYPE and TYPE?
	/**
	 * This method gets the length of the object represented by the descriptor. 
	 * This is only used by Array descriptors.
	 * 
	 * @return integer : represents length of array
	 * @throws UnsupportedOperationException
	 */
	public abstract long getLength();
	
	/**
	 * This will be used in MethodDescriptor.
	 * @return ArrayList<Boolean> : each entry is true for a boolean argument and false for an integer argument. 
	 * @throws UnsupportedOperationException for non methods.
	 */
	
	public abstract List<Boolean> getArgTypes();
	
	/**
	 * This is also another action really only useful for MethodDescriptors. 
	 * It gives you a string representing the expected return type of the method. 
	 * @return "bool", "boolean", "int", or "void".
	 * @throws UnsupportedOperationException 
	 */

	public abstract String getReturnType();
	
	/**
	 * This gives you the Type of the descriptor. This is used by the enums so that we have a set
	 * type that anything could be; if it is not one of these types, we have an error. 
	 * @return Type instance which describes what kind of descriptor is being used.
	 */
	public abstract Type getType();
	
	public abstract void setIR(IR_Node IR);
	
	/**
	 * This method is only used for MethodDescriptors. 
	 * It returns the IR_Node that was used when the method descriptor was initialized.
	 * 
	 * Any other descriptor will throw an exception if this is used. 
	 * @return IR_Node passed when the MethodDescriptor was initialized
	 * @throws UnsupportedOperationException if not a MethodDescriptor
	 */
	public abstract IR_Node getIR() throws UnsupportedOperationException;
	
	

}
