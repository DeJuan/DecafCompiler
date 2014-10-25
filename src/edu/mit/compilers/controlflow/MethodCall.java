package edu.mit.compilers.controlflow;

import java.util.List;

public class MethodCall extends Expression {

	private String name;
	private List<Expression> args;
	
	public MethodCall(String methodName, List<Expression> arguments){
		this.name = methodName;
		this.args = arguments;
	}
	
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.METHOD_CALL;
	}
	
	public String getMethodName(){
		return name;
	}
	
	public List<Expression> getArguments(){
		return args;
	}

}
