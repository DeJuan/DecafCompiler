package edu.mit.compilers.ir;

import java.util.ArrayList;

public class IR_MethodDecl extends IR_Node {
	public Type retType;
	public String name;
	public ArrayList<Type> args;
	public IR_Seq body;
	
	private boolean isCallout;
	
	public int getNumArgs(){
		return args.size();
	}
	
	public Type getArgType(int idx){
		return args.get(idx);
	}
	
	public IR_MethodDecl(Type r, String n){
		retType = r;
		name = n;
		args = new ArrayList<Type>();
		body = new IR_Seq();
		isCallout = false;
	}
	
	public IR_MethodDecl(Type r, String n, boolean callout){
		retType = r;
		name = n;
		args = new ArrayList<Type>();
		body = new IR_Seq();
		isCallout = callout;
	}
	
	public void addArg(Type a){
		args.add(a);
	}
	
	public void setBody(IR_Seq block){
		body = block;
	}
	
	public Type getRetType(){
		return retType;
	}
	
	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		if(isCallout){
			return Type.CALLOUT;
		}else{
			return Type.METHOD;
		}
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "fun " + name;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

}
