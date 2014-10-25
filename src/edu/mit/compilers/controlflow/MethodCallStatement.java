package edu.mit.compilers.codegen;

public class MethodCallStatement extends Statement {
	private MethodCall method;
	
	public MethodCallStatement(MethodCall callingMethod){
		this.method = callingMethod;
	}

	@Override
	public StatementType getStatementType() {
		return StatementType.METHOD_CALL_STATEMENT;
	}
	
	public MethodCall getMethodCallStatement(){
		return this.method;
	}
	
}
