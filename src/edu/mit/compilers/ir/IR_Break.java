package edu.mit.compilers.ir;

/**
 * IR_Node for the 'break' statement.
 *
 */
public class IR_Break extends IR_Node {
	
	public IR_Break() {}
	
	@Override
	public Type getType() {
		return Type.VOID;
	}
	
	@Override
	public String toString() {
		return "break";
	}
}
