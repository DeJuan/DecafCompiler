/**
 * 
 */
package edu.mit.compilers.ir;

/**
 * @author yygu
 *
 */
public class IR_Return extends IR_Node {
	protected IR_Node expr;
	
	public IR_Return(IR_Node expression) {
		expr = expression;
	}
	
	public Type evaluateType() {
		return Type.NONE;
	}
	
	public boolean isValid() {
		return expr.isValid();
	}
	
	public String toString() {
		return "return" + expr.toString();
	}
}
