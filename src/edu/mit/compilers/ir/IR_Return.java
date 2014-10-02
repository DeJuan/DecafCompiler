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
		return expr == null || expr.isValid();
	}
	
	@Override
	public String toString() {
	    if (expr == null) {
	        return "return";
	    }
		return "return" + expr.toString();
	}
}
