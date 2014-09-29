/**
 * 
 */
package edu.mit.compilers.ir;

/**
 * @author yygu
 *
 */
public class IR_If extends IR_Node {
	protected IR_Node expr;
	protected IR_Seq trueBlock;
	protected IR_Seq falseBlock;
	
	public IR_If(IR_Node expr, IR_Seq trueBlock, IR_Seq falseBlock) {
		this.expr = expr;
		this.trueBlock = trueBlock;
		this.falseBlock = falseBlock;
	}
	
	public IR_Node getExpr() {
		return this.expr;
	}
	
	public IR_Seq getTrueBlock() {
		return this.trueBlock;
	}
	
	public IR_Seq getFalseBlock() {
		return this.falseBlock;
	}
	
	public Type evaluateType() {
		return Type.NONE;
	}
	
	public boolean isValid() {
		return expr.evaluateType() == Type.BOOL 
				&& expr.isValid() && trueBlock.isValid() && falseBlock.isValid();
	}
	
	public String toString() {
		return "if(" + expr.toString() + ")" + "{" + trueBlock.toString() + "}" + 
			   "else {" + falseBlock.toString() + "}";
	}

}
