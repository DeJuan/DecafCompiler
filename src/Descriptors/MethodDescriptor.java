package Descriptors;

import java.util.List;

import edu.mit.compilers.ir.IR_Node;


public class MethodDescriptor extends Descriptor {

	private String methodType;
	private String returnType;
	private List<Boolean> argumentTypes;
	private ReturnType type;
	private IR_Node IR;
	
	
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
	public MethodDescriptor(String type, List<Boolean> argTypes)
	{
		//TODO NOT SURE IF THIS IS THE BEST WAY TO HANDLE THIS, ASK GROUP
		this.returnType = type;
		if (returnType.equals("bool") || returnType.equals("boolean")) {
		    this.type = ReturnType.BOOL;
		} else if (returnType.equals("int")) {
		    this.type = ReturnType.INT;
		} else if (returnType.equals("void")) {
		    this.type = ReturnType.VOID;
		} else {
		    System.err.print("Invalid string  supplied for method type.");
		    throw new RuntimeException();
		}	
		this.argumentTypes = argTypes;
	}
	
	@Override
	public List<Boolean> getArgTypes() {
		return argumentTypes;
	}

	@Override
	public String getReturnType() {
		return this.returnType;
	}

	@Override
	public Type getType() {
		return Type.METHOD;
	}


	@Override
	public long getLength() {
		System.err.println("Methods do not have a length in that sense.");
		throw new UnsupportedOperationException();
	}

    @Override
    public IR_Node getIR() {
        return this.IR;
    }

    @Override
    public void setIR(IR_Node IR) {
        this.IR = IR;
        
    }
	
	//TODO INSERT IR METHODS
}
