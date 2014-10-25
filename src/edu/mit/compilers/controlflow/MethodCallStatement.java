package edu.mit.compilers.controlflow;

/**
 * This class represents a method call statement and contains a MethodCall. 
 * @author DeJuan
 *
 */
public class MethodCallStatement extends Statement {
	private MethodCall method;
	
	/**
	 * This constructor just takes in a MethodCall and stores it locally.
	 * @param callingMethod : MethodCall that represents the method we will be calling
	 */
	public MethodCallStatement(MethodCall callingMethod){
		this.method = callingMethod;
	}

	@Override
	/**
	 * Tells you that this is a method call statement.
	 * @return StatementType : METHOD_CALL_STATEMENT
	 */
	public StatementType getStatementType() {
		return StatementType.METHOD_CALL_STATEMENT;
	}
	/**
	 * Getter method for the MethodCall object we stored on construction.
	 * @return method : MethodCall representing the call statement we'll be using. 
	 */
	public MethodCall getMethodCallStatement(){
		return this.method;
	}
	
}
