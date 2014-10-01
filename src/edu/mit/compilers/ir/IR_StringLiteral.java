/**
 * 
 */
package edu.mit.compilers.ir;

import antlr.collections.AST;

/**
 * @author yygu
 *
 */
public class IR_StringLiteral extends IR_Node {
	protected String value;
	
	public IR_StringLiteral(AST node) {
		value = node.getText();
	}
	
	public String getValue() {
		return this.value;
	}
	
	@Override
	public Type evaluateType() {
		return Type.STRING;
	}
	
	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override
	public String toString() {
		return value;
	}

}
