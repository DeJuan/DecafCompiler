package edu.mit.compilers.ir;

import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;

public class IR_FieldDecl extends IR_Node {
	Type type;
	public IR_IntLiteral len;
	String name;
	
	public IR_FieldDecl(Type t, String n){
		type = t;
		name = n;
		len=null;
	}
	
	public IR_FieldDecl(Type t, String n, IR_IntLiteral l){
		type = t;
		name = n;
		len = l;
	}
	
	public String getName(){
		return name;
	}
	
	public IR_IntLiteral getLength(){
		return len;
	}
	
	@Override
	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return name;
	}

}
