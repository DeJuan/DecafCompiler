package edu.mit.compilers.ir;

import edu.mit.compilers.ir.IR_CompareOp.IR_CompareOp_LT;
import edu.mit.compilers.ir.IR_STL;

import java.util.ArrayList;
import java.util.List;

/**
 * IR_Node for the 'for' statement.
 * Grammar: for ( id = expr, expr ) block
 * The constructor takes in a IR_Seq for a series of pre-loop processing statements.
 * It also takes in a comparison IR node and a IR_Seq of statements.
 *
 */
public class IR_For extends IR_Node {
	protected IR_Seq preLoop;
	protected IR_CompareOp_LT lt;
	protected IR_Seq block;
	
	public IR_For(IR_Seq preloop, IR_CompareOp_LT cond, IR_Seq block) {
	    this.preLoop = preloop;
	    this.lt = cond;
	    this.block = block;
	}
	
	public IR_Node getPreLoop() {
		return this.preLoop;
	}
	
	public IR_CompareOp_LT getLt() {
		return this.lt;
	}
	
	public IR_Seq getBlock() {
		return this.block;
	}
	
	@Override
	public Type getType() {
		return Type.VOID;
	}
	
	@Override
	public boolean isValid() {
		return preLoop.isValid() && lt.isValid() && block.isValid();
	}
	
	@Override
	public String toString() {
		return "for(" + preLoop.toString() + ", " + lt.toString() + ")" + "{" + block.toString() + "}";
	}

}
