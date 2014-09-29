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
	
	@Override
	public Type evaluateType() {
		return Type.NONE;
	}
	
	@Override
	public boolean isValid() {
		return expr.isValid();
	}
	
	@Override
	public String toString() {
		return "return" + expr.toString();
	}
}
