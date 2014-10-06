package edu.mit.compilers.ir;

public class IR_Assign extends IR_Node {

	IR_Var lhs;
	IR_Node rhs;
	Ops op;

	public IR_Assign(IR_Var l, IR_Node r, Ops o){
		lhs = l;
		rhs = r;
		op  = o;
	}
	
	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return Type.VOID;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return op.toString();
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

}
