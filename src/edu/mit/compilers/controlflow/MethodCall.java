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
	
	/**
	 * This constructor takes in the name of the method and the arguments that will be passed to it.
	 * @param methodName : String representing name of method being invoked
	 * @param arguments : List<Expression> representing args that will be passed to the method
	 */
	public MethodCall(String methodName, List<Expression> arguments){
		this.name = methodName;
		this.args = arguments;
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
	 * @return name : String reprsenting method name.
	 */
	public String getMethodName(){
		return name;
	}
	
	/**
	 * Getter method for the argument list. 
	 * @return args : List<Expression> representing arguments that will be passed to list. 
	 */
	public List<Expression> getArguments(){
		return args;
	}

}
