package edu.mit.compilers.controlflow;

import edu.mit.compilers.codegen.Descriptor;

public class Var extends Expression {
	private Descriptor var;
	
	public Var(Descriptor variable){
		this.var = variable;
	}
	
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.VAR;
	}
	
	public Descriptor getVarDescriptor(){
		return var;
	}

	public void setNewDescriptor(Descriptor newVar){
		this.var = newVar;
	}
}
