/**
 * 
 */
package edu.mit.compilers.ir;

/**
 * @author yygu
 *
 */
public class IR_Not extends IR_Node {
	protected IR_Node expr;
	
	public IR_Not(IR_Node expression) {
		expr = expression;
	}
	
	public IR_Node getExpr() {
		return this.expr;
	}
	
	@Override
	public Type evaluateType() {
		return Type.BOOL;
	}
	
	@Override
	public boolean isValid() {
		return expr.evaluateType() == Type.BOOL;
	}
	
	@Override
	public String toString() {
		return "!" + expr.toString();
	}

}
