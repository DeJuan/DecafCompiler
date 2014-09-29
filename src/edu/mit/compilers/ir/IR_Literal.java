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
	
	public static class IR_IntLiteral extends IR_Literal {
		private int value;
		
		public IR_IntLiteral(AST node) {
			value = Integer.parseInt(node.getText());
		}
		
		@Override
		public Type evaluateType() {
			return Type.INT;
		}
		
		@Override
		public boolean isValid() {
			return true;
		}
		
		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}
	
	public static class IR_BoolLiteral extends IR_Literal {
		private boolean value;
		
		public IR_BoolLiteral(AST node) {
			if (node.getText().equals("true"))
				value = true;
			else if (node.getText().equals("false"))
				value = false;
		}
		
		@Override
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return true;
		}
		
		@Override
		public String toString() {
			return Boolean.toString(value);
		}
	}
	
}
