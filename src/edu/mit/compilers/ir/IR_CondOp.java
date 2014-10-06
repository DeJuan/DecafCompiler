package edu.mit.compilers.ir;

/**
 * IR_Node for the conditional operators (&&, ||). Grammar: expr cond_op expr
 * Each subclass of IR_CondOp takes in a left IR Node and a right IR Node.
 *
 */
public class IR_CondOp extends IR_Node {

	protected IR_Node left;
	protected IR_Node right;
	Ops op;

	public IR_CondOp(IR_Node l, IR_Node r, Ops o) {
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
	public boolean isValid() {
		return left.getType() == Type.BOOL && right.getType() == Type.BOOL;
	}

	@Override
	public String toString() {
		return op + left.toString() + right.toString();
	}

}
