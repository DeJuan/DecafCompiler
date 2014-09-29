/**
 * 
 */
package edu.mit.compilers.ir;

import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;

/**
 * @author yygu
 *
 */
public class IR_While extends IR_Node {
	protected IR_Node expr_cond;
	protected IR_IntLiteral maxLoops;
	protected IR_Seq block;
	
	public IR_While(IR_Node expr_condition, IR_IntLiteral maxLoops, IR_Seq block) {
		this.expr_cond = expr_condition;
		this.maxLoops = maxLoops;
		this.block = block;
	}
	
	public IR_Node getExpr() {
		return this.expr_cond;
	}
	
	public IR_IntLiteral getMaxLoops() {
		return this.maxLoops;
	}
	
	public IR_Seq getBlock() {
		return this.block;
	}
	
	public Type evaluateType() {
		return Type.NONE;
	}
	
	public boolean isValid() {
		return expr_cond.isValid() && expr_cond.evaluateType() == Type.BOOL 
				&& maxLoops.isValid() && block.isValid();
	}
	
	public String toString() {
		return "while(" + expr_cond.toString() + ")" + "{" + block.toString() + "}";
	}

}
