package edu.mit.compilers.ir;

/**
 * IR_Node for the 'continue' statement.
 *
 */
public class IR_Continue extends IR_Node {
	
	public IR_Continue() {}
	
	@Override
	public Type evaluateType() {
		return Type.NONE;
	}
	
	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override
	public String toString() {
		return "continue";
	}
}
