package edu.mit.compilers.ir;

/**
 *  IR_Node for the comparison operators (<, >, <=, >=).
 *  Grammar: expr comp_op expr
 *  Each subclass of IR_CompareOp takes in a left IR Node and a right IR Node.
 *
 */
public class IR_CompareOp extends IR_Node {
	protected IR_Node left;
	protected IR_Node right;
	Ops op;

	public Ops getOp(){
		return op;
	}
	
	public IR_CompareOp(IR_Node l, IR_Node r, Ops o) {
		left = l;
		right = r;
		op = o;
	}

	public IR_Node getLeft() {
		return this.left;
	}

	public IR_Node getRight() {
		return this.right;
	}

	@Override
	public Type getType() {
		return Type.BOOL;
	}

	@Override
	public String toString() {
		return op + left.toString() + right.toString();
	}

}
