package edu.mit.compilers.ir;


/**
 * Abstract class for an Intermediate Representation Node.
 * All IR nodes extends from this base class.
 * 
 */
public abstract class IR_Node {
	
	public abstract Type getType();
	public abstract String toString();
	public abstract boolean isValid();

}
