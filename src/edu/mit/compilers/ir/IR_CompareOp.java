/**
 * 
 */
package edu.mit.compilers.ir;


/**
 * @author yygu
 *
 */
abstract class IR_CompareOp extends IR_Node {
	protected IR_Node left;
	protected IR_Node right;
	
	abstract IR_Node getLeft();
	abstract IR_Node getRight();
	
	public class IR_CompareOp_LT extends IR_CompareOp {
		public IR_CompareOp_LT(IR_Node left_child, IR_Node right_child) {
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
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		public String toString() {
			return "<" + left.toString() + right.toString();
		}
	}
	
	public class IR_CompareOp_GT extends IR_CompareOp {
		public IR_CompareOp_GT(IR_Node left_child, IR_Node right_child) {
			left = left_child;
			right = right_child;
		}
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		public IR_Node getLeft() {
			return this.left;
		}
		
		public IR_Node getRight() {
			return this.right;
		}
		
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		public String toString() {
			return "<" + left.toString() + right.toString();
		}
	}
	
	public class IR_CompareOp_LTE extends IR_CompareOp {
		public IR_CompareOp_LTE(IR_Node left_child, IR_Node right_child) {
			left = left_child;
			right = right_child;
		}
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		public IR_Node getLeft() {
			return this.left;
		}
		
		public IR_Node getRight() {
			return this.right;
		}
		
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		public String toString() {
			return "<=" + left.toString() + right.toString();
		}
	}
	
	public class IR_CompareOp_GTE extends IR_CompareOp {
		public IR_CompareOp_GTE(IR_Node left_child, IR_Node right_child) {
			left = left_child;
			right = right_child;
		}
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		public IR_Node getLeft() {
			return this.left;
		}
		
		public IR_Node getRight() {
			return this.right;
		}
		
		public boolean isValid() {
			return left.evaluateType() == Type.INT && right.evaluateType() == Type.INT;
		}
		
		public String toString() {
			return ">=" + left.toString() + right.toString();
		}
	}

}
