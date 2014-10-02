package Descriptors;

import java.util.List;

import edu.mit.compilers.ir.IR_Node;


public class MethodDescriptor extends Descriptor {

	private List<Boolean> argumentTypes;
	private Type type;
	private ReturnType returnType;
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
	public MethodDescriptor(String type, List<Boolean> argTypes)
	{
		//TODO NOT SURE IF THIS IS THE BEST WAY TO HANDLE THIS, ASK GROUP
        this.type = Type.METHOD;
		if (type.equals("bool") || type.equals("boolean")) {
		    this.returnType = ReturnType.BOOL;
		} else if (type.equals("int")) {
		    this.returnType = ReturnType.INT;
		} else if (type.equals("void")) {
		    this.returnType = ReturnType.VOID;
		} else {
		    System.err.print("Invalid string  supplied for method type.");
		    throw new RuntimeException("Invalid string  supplied for method type.");
		}
		this.argumentTypes = argTypes;
	}
	
	@Override
	public List<Boolean> getArgTypes() {
		return argumentTypes;
	}

	@Override
	public String getReturnType() {
		return this.returnType.toString();
	}

	@Override
	public Type getType() {
		return this.type;
	}


	@Override
	public long getLength() {
		System.err.println("Methods do not have a length in that sense.");
		throw new UnsupportedOperationException();
	}

    @Override
    public IR_Node getIR() {
        return this.methodIRNode;
    }

    @Override
    public void setIR(IR_Node IR) {
        this.methodIRNode = IR;
        
    }

}
