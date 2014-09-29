/**
 * 
 */
package edu.mit.compilers.ir;

/**
 * @author yygu
 *
 */
public class IR_Break extends IR_Node {
	
	public IR_Break() {
	}
	
	public Type evaluateType() {
		return Type.NONE;
	}
	
	public boolean isValid() {
		return true;
	}
	
	public String toString() {
		return "break";
	}
}
