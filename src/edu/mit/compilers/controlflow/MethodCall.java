package edu.mit.compilers.controlflow;

import java.util.List;

/**
 * This class represents the actual important data from a method call, namely, the name of the method and its associated parameters.
 * @author DeJuan
 *
 */
public class MethodCall extends Expression {

	private String name;
	private List<Expression> args;
	private boolean isCallout;
	
	/**
	 * This constructor takes in the name of the method and the arguments that will be passed to it.
	 * @param methodName : String representing name of method being invoked
	 * @param arguments : List<Expression> representing args that will be passed to the method
	 * @param isCallout : true if and only if the method being called is a callout
	 */
	public MethodCall(String methodName, List<Expression> arguments, boolean isCallout){
		this.name = methodName;
		this.args = arguments;
		this.isCallout = isCallout;
	}
	
	@Override
	/**
	 * Tells you that you're working with a method call
	 * @return ExpressionType : METHOD_CALL
	 */
	public ExpressionType getExprType() {
		return ExpressionType.METHOD_CALL;
	}
	
	/**
	 * Returns the string given as the name for the method. 
	 * @return name : String representing method name.
	 */
	public String getMethodName(){
		return name;
	}
	
	public boolean getIsCallout(){
	    return isCallout;
	}
	
	/**
	 * Getter method for the argument list. 
	 * @return args : List<Expression> representing arguments that will be passed to list. 
	 */
	public List<Expression> getArguments(){
		return args;
	}

}
