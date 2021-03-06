package edu.mit.compilers.controlflow;

/**
 * This class represents Boolean Literals.
 * @author DeJuan
 *
 */
public class BoolLit extends Expression {
	private boolean truthValue;
	
	/**
	 * This is the constructor for a BoolLiteral. It accepts the value for the boolean.
	 * @param trueFalseValue : A boolean which is the truth value for this literal.
	 */
	public BoolLit(boolean trueFalseValue){
		this.truthValue = trueFalseValue;
	}
	
	@Override
	/**
	 * Tells you that you're working with a boolean.
	 * @return ExpressionType : BOOL_LIT
	 */
	public ExpressionType getExprType() {
		return ExpressionType.BOOL_LIT;
	}
	
	/**
	 * Returns the boolean truth value for this literal.
	 * @return boolean : True or False depending on the value given when this literal was constructed, assuming no overwrites.
	 */
	public boolean getTruthValue(){
		return truthValue;
	}
	
	/**
	 * Allows you to override the original value given to this literal with a new value. 
	 * @param newVal : The new truth value for this boolean literal. 
	 */
	public void updateTruthValue(boolean newVal){
		this.truthValue = newVal;
	}

}
