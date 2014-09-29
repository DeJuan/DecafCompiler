/**
 * 
 */
package edu.mit.compilers.ir;

import edu.mit.compilers.ir.IR_CompareOp.IR_CompareOp_LT;
import edu.mit.compilers.ir.IR_STL;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yygu
 *
 */
public class IR_For extends IR_Node {
	protected IR_Seq preLoop;
	protected IR_CompareOp_LT lt;
	protected IR_Seq block;
	
	public IR_For(IR_Var id, IR_Node expr1, IR_Node expr2, IR_Seq block) {
		// TODO: is id a ID_Var or a IR_LDX?
		List<IR_Node> statements = new ArrayList<IR_Node>();
		statements.add(new IR_STL(id, expr1));
		this.preLoop = new IR_Seq(statements);
		
		this.lt = new IR_CompareOp_LT((IR_Node) id, expr2);
		
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
	
	public Type evaluateType() {
		return Type.NONE;
	}
	
	public boolean isValid() {
		return preLoop.isValid() && lt.isValid() && block.isValid();
	}
	
	public String toString() {
		return "for(" + preLoop.toString() + ", " + lt.toString() + ")" + "{" + block.toString() + "}";
	}

}
