/**
 * 
 */
package edu.mit.compilers.ir;

/**
 * @author yygu
 *
 */
abstract class IR_CondOp extends IR_Node {
	
	protected IR_Node left;
	protected IR_Node right;
	
	abstract IR_Node getLeft();
	abstract IR_Node getRight();
	
	public static class IR_CondOp_And extends IR_CondOp {
		
		public IR_CondOp_And(IR_Node left_child, IR_Node right_child) {
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
			return left.evaluateType() == Type.BOOL && right.evaluateType() == Type.BOOL;
		}
		
		@Override
		public String toString() {
			return "&&" + left.toString() + right.toString();
		}
	}
	
	public static class IR_CondOp_Or extends IR_CondOp {
		
		public IR_CondOp_Or(IR_Node left_child, IR_Node right_child) {
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
			return left.evaluateType() == Type.BOOL && right.evaluateType() == Type.BOOL;
		}
		
		@Override
		public String toString() {
			return "||" + left.toString() + right.toString();
		}
	}
	
}
