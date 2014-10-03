package edu.mit.compilers.ir;

/**
 * IR_Node for the 'if' statement.
 * Grammar: if ( expr ) block [else block]
 * It takes in IR nodes for expr, the true block, and the false block.
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
	
	@Override
	public Type evaluateType() {
		return Type.NONE;
	}
	
	@Override
	public boolean isValid() {
		return expr.evaluateType() == Type.BOOL 
				&& expr.isValid() && trueBlock.isValid() && falseBlock.isValid();
	}
	
	@Override
	public String toString() {
		return "if(" + expr.toString() + ")" + "{" + trueBlock.toString() + "}" + 
			   "else {" + falseBlock.toString() + "}";
	}

}
