/**
 * 
 */
package edu.mit.compilers.ir;

/**
 * @author yygu
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
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return (left.evaluateType() == Type.INT || left.evaluateType() == Type.BOOL) && 
				   (right.evaluateType() == Type.INT || right.evaluateType() == Type.BOOL);
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
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return (left.evaluateType() == Type.INT || left.evaluateType() == Type.BOOL) && 
				   (right.evaluateType() == Type.INT || right.evaluateType() == Type.BOOL);
		}
		
		@Override
		public String toString() {
			return "!=" + left.toString() + right.toString();
		}
	}

}
