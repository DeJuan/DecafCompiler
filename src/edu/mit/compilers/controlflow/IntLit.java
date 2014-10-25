package edu.mit.compilers.controlflow;

/**
 * This class is a representation for integer literals 
 * @author DeJuan
 *
 */
public class IntLit extends Expression {
	private int value;
	/**
	 * This constructor simply takes the value that this literal represents. 
	 * @param val
	 */
	public IntLit(int val){
		this.value = val;
	}
	
	@Override
	/**
	 * Tells you that you're working with an int literal.
	 * @return ExpressionType : INT_LIT
	 */
	public ExpressionType getExprType() {
		return ExpressionType.INT_LIT;
	}
	
	/**
	 * Getter method for the value stored within this literal.
	 * @return value : int representing the value of the literal
	 */
	public int getValue(){
		return value;
	}

	/**
	 * Update function that lets you change the value of the int literal.
	 * @param newVal : int that will replace the currently stored value. 
	 */
	public void updateValue(int newVal){
		this.value = newVal;
	}
}
