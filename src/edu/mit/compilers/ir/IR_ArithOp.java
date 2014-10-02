/**
 * 
 */
package edu.mit.compilers.ir;

/**
 * @author yygu
 *
 */
abstract class IR_ArithOp extends IR_Node {
	protected IR_Node left;
	protected IR_Node right;
	
	abstract IR_Node getLeft();
	abstract IR_Node getRight();
	
	public static class IR_ArithOp_Plus extends IR_ArithOp {
		
		public IR_ArithOp_Plus(IR_Node left_child, IR_Node right_child) {
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
			return Type.INT;
		}
		
		@Override
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		@Override
		public String toString() {
			return left.toString() + " + " + right.toString();
		}
	}
	
	public static class IR_ArithOp_Sub extends IR_ArithOp {
		
		public IR_ArithOp_Sub(IR_Node left_child, IR_Node right_child) {
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
			return Type.INT;
		}
		
		@Override
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		@Override
		public String toString() {
			return left.toString() + " - " + right.toString();
		}
	}
	
	public static class IR_ArithOp_Mult extends IR_ArithOp {
		
		public IR_ArithOp_Mult(IR_Node left_child, IR_Node right_child) {
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
			return Type.INT;
		}
		
		@Override
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		@Override
		public String toString() {
			return left.toString() + " * "+ right.toString();
		}
	}
	
	public static class IR_ArithOp_Div extends IR_ArithOp {
		
		public IR_ArithOp_Div(IR_Node left_child, IR_Node right_child) {
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
			return Type.INT;
		}
		
		@Override
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		@Override
		public String toString() {
			return left.toString() + " / " + right.toString();
		}
	}
	
	public static class IR_ArithOp_Mod extends IR_ArithOp {
		
		public IR_ArithOp_Mod(IR_Node left_child, IR_Node right_child) {
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
			return Type.INT;
		}
		
		@Override
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		@Override
		public String toString() {
			return left.toString() + " % " + right.toString();
		}
	}
}
