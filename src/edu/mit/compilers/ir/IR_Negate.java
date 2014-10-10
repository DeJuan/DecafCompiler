package edu.mit.compilers.ir;

public class IR_Negate extends IR_Node {
	private IR_Node expr;
	public IR_Negate(IR_Node e){
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