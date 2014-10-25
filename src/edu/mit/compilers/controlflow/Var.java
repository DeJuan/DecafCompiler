package edu.mit.compilers.controlflow;

import edu.mit.compilers.codegen.Descriptor;

/**
 * This class represents variables. 
 * @author DeJuan
 *
 */
public class Var extends Expression {
	private Descriptor var;
	
	/**
	 * This constructor takes in the Descriptor associated with the variable we want to store and keeps it locally. 
	 * @param variable
	 */
	public Var(Descriptor variable){
		this.var = variable;
	}
	
	@Override
	/**
	 * Tells you that you're working with a variable.
	 * @return ExpressionType : VAR
	 */
	public ExpressionType getExprType() {
		return ExpressionType.VAR;
	}
	
	/**
	 * Gives you back the descriptor that is stored locally.
	 * @return var : Descriptor for the variable this object represents
	 */
	public Descriptor getVarDescriptor(){
		return var;
	}

	/**
	 * Allows you to set a new descriptor in case an optimization changes what a variable's descriptor should be. 
	 * @param newVar : The new descriptor for the variable this object represents. 
	 */
	public void setNewDescriptor(Descriptor newVar){
		this.var = newVar;
	}
}
