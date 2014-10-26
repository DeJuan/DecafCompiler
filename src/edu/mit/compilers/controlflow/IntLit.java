package edu.mit.compilers.controlflow;

/**
 * This class is a representation for integer literals 
 * @author DeJuan
 *
 */
public class IntLit extends Expression {
	private long value;
	/**
	 * This constructor simply takes the value that this literal represents. 
	 * @param val
	 */
	public IntLit(long val){
		this.value = val;
	}
	
	@Override
	/**
	 * Tells you that you're working with an long literal.
	 * @return ExpressionType : INT_LIT
	 */
	public ExpressionType getExprType() {
		return ExpressionType.INT_LIT;
	}
	
	/**
	 * Getter method for the value stored within this literal.
	 * @return value : long representing the value of the literal
	 */
	public long getValue(){
		return value;
	}

	/**
	 * Update function that lets you change the value of the long literal.
	 * @param newVal : long that will replace the currently stored value. 
	 */
	public void updateValue(long newVal){
		this.value = newVal;
	}
}
