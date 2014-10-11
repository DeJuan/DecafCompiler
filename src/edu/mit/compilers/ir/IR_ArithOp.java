package edu.mit.compilers.ir;

/**
 * IR_Node for arithmetic operations (+, -, *, /, %). 
 * Grammar: expr arith_op expr
 * Each subclass of IR_ArithOp takes in a left IR Node and a right IR Node.
 * 
 */
public class IR_ArithOp extends IR_Node {
	protected IR_Node left;
	protected IR_Node right;
	Ops op;
	public IR_ArithOp(IR_Node l, IR_Node r, Ops o){
		left = l;
		right = r;
		op = o;
	}
	
	public Ops getOp(){
		return op;
	}
	
	public IR_Node getLeft(){
		return left;
	}
	public IR_Node getRight(){
		return right;
	}

	@Override
	public Type getType() {
		return Type.INT;
	}

	@Override
	public String toString() {
		return left.toString() + op + right.toString();
	}

}
