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
	
	public static class IR_CondOp_And extends IR_EqOp {
		public IR_CondOp_And(IR_Node left_child, IR_Node right_child) {
			left = left_child;
			right = right_child;
		}
		
		public IR_Node getLeft() {
			return this.left;
		}
		
		public IR_Node getRight() {
			return this.right;
		}
		
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		public boolean isValid() {
			return left.evaluateType() == Type.BOOL && right.evaluateType() == Type.BOOL;
		}
		
		public String toString() {
			return "&&" + left.toString() + right.toString();
		}
	}
	
	public static class IR_CondOp_Or extends IR_EqOp {
		public IR_CondOp_Or(IR_Node left_child, IR_Node right_child) {
			left = left_child;
			right = right_child;
		}
		
		public IR_Node getLeft() {
			return this.left;
		}
		
		public IR_Node getRight() {
			return this.right;
		}
		
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		public boolean isValid() {
			return left.evaluateType() == Type.BOOL && right.evaluateType() == Type.BOOL;
		}
		
		public String toString() {
			return "||" + left.toString() + right.toString();
		}
	}
	
}
