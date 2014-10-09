package edu.mit.compilers.ir;

/**
 * IR_Node for the 'continue' statement.
 *
 */
public class IR_Continue extends IR_Node {
	
	public IR_Continue() {}
	
	@Override
	public Type getType() {
		return Type.VOID;
	}
	
	@Override
	public String toString() {
		return "continue";
	}
}
