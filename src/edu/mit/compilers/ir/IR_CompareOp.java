package edu.mit.compilers.ir;

/**
 *  IR_Node for the comparison operators (<, >, <=, >=).
 *  Grammar: expr comp_op expr
 *  Each subclass of IR_CompareOp takes in a left IR Node and a right IR Node.
 *
 */
abstract class IR_CompareOp extends IR_Node {
	protected IR_Node left;
	protected IR_Node right;
	
	abstract IR_Node getLeft();
	abstract IR_Node getRight();
	
	public static class IR_CompareOp_LT extends IR_CompareOp {
		
		public IR_CompareOp_LT(IR_Node left_child, IR_Node right_child) {
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
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		@Override
		public String toString() {
			return "<" + left.toString() + right.toString();
		}
	}
	
	public static class IR_CompareOp_GT extends IR_CompareOp {
		
		public IR_CompareOp_GT(IR_Node left_child, IR_Node right_child) {
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
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		@Override
		public String toString() {
			return "<" + left.toString() + right.toString();
		}
	}
	
	public static class IR_CompareOp_LTE extends IR_CompareOp {
		
		public IR_CompareOp_LTE(IR_Node left_child, IR_Node right_child) {
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
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		@Override
		public String toString() {
			return "<=" + left.toString() + right.toString();
		}
	}
	
	public static class IR_CompareOp_GTE extends IR_CompareOp {
		
		public IR_CompareOp_GTE(IR_Node left_child, IR_Node right_child) {
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
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		@Override
		public String toString() {
			return ">=" + left.toString() + right.toString();
		}
	}

}
