package Descriptors;

import java.util.ArrayList;

import edu.mit.compilers.ir.IR_Node;
import Descriptors.Descriptor.ReturnType;
import Descriptors.Descriptor.Type;

///@TODO: switch to UnsupportedOperationException//
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class MethodDescriptor extends Descriptor {

	private String methodType;
	private ArrayList<Boolean> argumentTypes;
	private ReturnType returnType;
	private Type type;
	private IR_Node methodIRNode;
	
	
	/**
	 * This is the constructor for a new MethodDescriptor. 
	 * It takes in a string which denotes the return type of the method,
	 * an arrayList of booleans that denote whether each argument is 
	 * an integer or a boolean,
	 * and the IR_Node for the method. 
	 * If the supplied type string does not match a valid type, a runtime exception is thrown. 
	 * 
	 * @param type : A string indicating return type of method
	 * @param argTypes : ArrayList<Boolean> that indicates whether each arg is a bool or int
	 * @param methodNode : an IR_Node representation of the method. 
	 * @throws Exception
	 */
	public MethodDescriptor(String retType, ArrayList<Boolean> argTypes, IR_Node methodNode) throws Exception
	{
		//TODO NOT SURE IF THIS IS THE BEST WAY TO HANDLE THIS, ASK GROUP
		this.type = Type.METHOD;
		this.methodIRNode = methodNode;
		switch(retType)
		{
		
		case "bool": //Fall though, just here for safety
		case "boolean":
		this.returnType = ReturnType.BOOL;
		break;
		
		case "int":
		this.returnType = ReturnType.INT;
		break;
		
		case "void":
		this.returnType = ReturnType.VOID;
		break;
		
		default:
		System.err.print("Invalid string  supplied for method type.");
			throw new RuntimeException("Invalid string  supplied for method type.");
		}
		
		
		this.argumentTypes = argTypes;
	}
	
	@Override
	public ArrayList<Boolean> getArgTypes() {
		return argumentTypes;
	}

	@Override
	public String getReturnType() {
		return this.returnType.toString();
	}

	@Override
	public Type getType(){
		return this.type;
	}


	@Override
	public int getLength() throws UnsupportedOperationException {
		System.err.println("Methods do not have a length in that sense.");
		throw new UnsupportedOperationException("Methods do not have a length for this operation.");
	}

	@Override
	public int getValue() throws UnsupportedOperationException {
		System.err.println("Methods do not have a value field like an integer.");
		throw new UnsupportedOperationException("Methods do not have a value field like an integer.");
	}

	@Override
	public boolean getTruthValue() throws UnsupportedOperationException {
		System.err.println("Methods do not have a truth value.");
		throw new UnsupportedOperationException("Methods do not have a truth value.");
	}

	@Override
	public void setValue(int index, int newValue) throws UnsupportedOperationException {
		System.err.println("Methods do not have a value that can be set.");
		throw new UnsupportedOperationException("Methods do not have a value that can be set.");	
	}

	@Override
	public void setValue(int index, boolean newValue) throws UnsupportedOperationException {
		System.err.println("Methods do not have a value that can be set.");
		throw new UnsupportedOperationException("Methods do not have a value that can be set.");
	}

	@Override
	public IR_Node getIR(){
		return this.methodIRNode;
	}

}
