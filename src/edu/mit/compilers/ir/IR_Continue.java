/**
 * 
 */
package edu.mit.compilers.ir;

/**
 * @author yygu
 *
 */
public class IR_Continue extends IR_Node {
	
	public IR_Continue() {
	}
	
	public Type evaluateType() {
		return Type.NONE;
	}
	
	public boolean isValid() {
		return true;
	}
	
	public String toString() {
		return "continue";
	}
}
