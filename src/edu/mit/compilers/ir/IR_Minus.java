package edu.mit.compilers.ir;

public class IR_Minus extends IR_Node {
	private IR_Node expr;
	
	public Ops getOp(){
		return Ops.MINUS;
	}
	public IR_Minus(IR_Node e){
		expr = e;
	}
	public IR_Node getExpr(){
		return expr;
	}
	@Override
	public Type getType() {
		return Type.INT;
	}

	@Override
	public String toString() {
		return "-";
	}

}
