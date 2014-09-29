/**
 * Base case for an IR Node
 */
package edu.mit.compilers.ir;

/**
 * @author yygu
 *
 */
public abstract class IR_Node {
	
	public abstract Type evaluateType();
	public abstract String toString();
	public abstract boolean isValid();

}
