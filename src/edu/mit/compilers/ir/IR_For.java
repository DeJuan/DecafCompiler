package edu.mit.compilers.ir;

/**
 * IR_Node for the 'for' statement.
 * Grammar: for ( id = expr, expr ) block
 * The constructor takes in a IR_Seq for a series of pre-loop processing statements.
 * It also takes in a comparison IR node and a IR_Seq of statements.
 *
 */
public class IR_For extends IR_Node {
	//loop variable
	private IR_Var loopVar;
	protected IR_Node start, end;
	protected IR_Seq block;
	
	public IR_For(IR_Var v, IR_Node s, IR_Node e, IR_Seq b) {
		loopVar = v;
		start= s;
		end = e;
	    block = b;
	}
	
	public IR_Var getVar(){
		return loopVar;
	}
	
	public IR_Node getStart() {
		return start;
	}
	
	public IR_Node getEnd() {
		return end;
	}
	
	public IR_Seq getBlock() {
		return block;
	}
	
	@Override
	public Type getType() {
		return Type.VOID;
	}
		
	@Override
	public String toString() {
		return "for(" + loopVar.toString() + ", " + start.toString()+ ","+ end.toString()+ ")" + "{" + block.toString() + "}";
	}

}
