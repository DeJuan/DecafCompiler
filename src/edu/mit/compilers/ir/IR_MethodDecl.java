package edu.mit.compilers.ir;

import java.util.ArrayList;

public class IR_MethodDecl extends IR_Node {
	public Type retType;
	public String name;
	public ArrayList<IR_FieldDecl> args;
	public IR_Seq body;
	
	private boolean isCallout;
	
	public int getNumArgs(){
		return args.size();
	}
	
	public Type getArgType(int idx){
		return args.get(idx).getType();
	}
	
	public String getName() {
		return name;
	}
	
	public IR_MethodDecl(Type r, String n){
		retType = r;
		name = n;
		args = new ArrayList<IR_FieldDecl>();
		body = new IR_Seq();
		isCallout = false;
	}
	
	public IR_MethodDecl(Type r, String n, boolean callout){
		retType = r;
		name = n;
		args = new ArrayList<IR_FieldDecl>();
		body = new IR_Seq();
		isCallout = callout;
	}
	
	public void addArg(Type t, String name){
		args.add(new IR_FieldDecl(t,name));
	}
	
	public void setBody(IR_Seq block){
		body = block;
	}
	
	public Type getRetType(){
		return retType;
	}
	
	@Override
	public Type getType() {
		if(isCallout){
			return Type.CALLOUT;
		}else{
			return Type.METHOD;
		}
	}

	@Override
	public String toString() {
		return "fun " + name;
	}

}
