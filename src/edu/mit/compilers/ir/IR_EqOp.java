package edu.mit.compilers.ir;

/**
 *  IR_Node for the equality operators (==, !=).
 *  Grammar: expr eq_op expr
 *  Each subclass of IR_EqOp takes in a left IR Node and a right IR Node.
 *
 */
abstract class IR_EqOp extends IR_Node {
	
	protected IR_Node left;
	protected IR_Node right;
	
	abstract IR_Node getLeft();
	abstract IR_Node getRight();
	
	public static class IR_EqOp_Equals extends IR_EqOp {
		
		public IR_EqOp_Equals(IR_Node left_child, IR_Node right_child) {
			left = left_child;
			right = right_child;
		}
		
		@Override
		public IR_Node getLeft() {
			return this.left;
		}
		
		@Override
		public IR_Node getRight() {
			return this.right;
		}
		
		@Override
		public Type getType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return (left.getType() == Type.INT || left.getType() == Type.BOOL) && 
				   (right.getType() == Type.INT || right.getType() == Type.BOOL);
		}
		
		@Override
		public String toString() {
			return "==" + left.toString() + right.toString();
		}
	}
	
	public static class IR_EqOp_NotEquals extends IR_EqOp {
		
		public IR_EqOp_NotEquals(IR_Node left_child, IR_Node right_child) {
			left = left_child;
			right = right_child;
		}
		
		@Override
		public IR_Node getLeft() {
			return this.left;
		}
		
		@Override
		public IR_Node getRight() {
			return this.right;
		}
		
		@Override
		public Type getType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return (left.getType() == Type.INT || left.getType() == Type.BOOL) && 
				   (right.getType() == Type.INT || right.getType() == Type.BOOL);
		}
		
		@Override
		public String toString() {
			return "!=" + left.toString() + right.toString();
		}
	}

}
