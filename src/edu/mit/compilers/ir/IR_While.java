package edu.mit.compilers.ir;

import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;

/**
 * IR_Node for the 'while' statement.
 * Grammar: while ( expr ) [ :int_literal ] block
 * The constructor takes in three IR Nodes that represents the loop condition, the max
 * number of times to loop, and a sequence of statements to execute in loop.
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
	
	@Override
	public Type evaluateType() {
		return Type.NONE;
	}
	
	@Override
	public boolean isValid() {
		return expr_cond.isValid() && expr_cond.evaluateType() == Type.BOOL 
				&& (maxLoops == null || maxLoops.isValid()) && block.isValid();
	}
	
	@Override
	public String toString() {
		return "while(" + expr_cond.toString() + ")" + "{" + block.toString() + "}";
	}

}
