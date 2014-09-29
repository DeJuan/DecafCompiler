/**
 * 
 */
package edu.mit.compilers.ir;

import antlr.collections.AST;

/**
 * @author yygu
 *
 */
abstract class IR_Literal extends IR_Node {
	
	public class IntLiteral extends IR_Literal {
		private int value;
		
		public IntLiteral(AST node) {
			value = Integer.parseInt(node.getText());
		}
		
		public boolean isValid() {
			return true;
		}
		
		public Type evaluateType() {
			return Type.INT;
		}
		
		public String toString() {
			return Integer.toString(value);
		}
	}
	
	public class BoolLiteral extends IR_Literal {
		private boolean value;
		
		public BoolLiteral(AST node) {
			if (node.getText().equals("true"))
				value = true;
			else if (node.getText().equals("false"))
				value = false;
		}
		
		public boolean isValid() {
			return true;
		}
		
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		public String toString() {
			return Boolean.toString(value);
		}
	}
	
}
