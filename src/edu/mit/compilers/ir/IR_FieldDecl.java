package edu.mit.compilers.ir;

import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;

public class IR_FieldDecl extends IR_Node {
	Type type;
	IR_IntLiteral len;
	String name;
	int index;
	
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
	
	public IR_FieldDecl(Type t, String n, IR_IntLiteral l, int index){
		type = t;
		name = n;
		len = l;
		this.index = index;
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
	
	public int getIndex(){
		return index;
	}
	
	@Override
	public String toString() {
		return name;
	}

}
