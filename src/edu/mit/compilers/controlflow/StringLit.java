package edu.mit.compilers.controlflow;

/**
 * This class represents String Literals.
 *
 */
public class StringLit extends Expression {
	private String value;
	
	/**
	 * This is the constructor for a StringLiteral. It accepts the value for the string.
	 * @param value : A string which is the value for this literal.
	 */
	public StringLit(String value){
		this.value = value;
	}
	
	@Override
	/**
	 * Tells you that you're working with a string.
	 * @return ExpressionType : STRING_LIT
	 */
	public ExpressionType getExprType() {
		return ExpressionType.STRING_LIT;
	}
	
	/**
	 * Returns the string value for this literal.
	 * @return value : value of the string.
	 */
	public String getValue(){
		return value;
	}
	
	/**
	 * Allows you to override the original value given to this literal with a new value. 
	 * @param newVal : The new value for this string literal. 
	 */
	public void updateValue(String newVal){
		this.value = newVal;
	}

}
