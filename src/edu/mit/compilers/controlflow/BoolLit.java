package edu.mit.compilers.codegen;

public class BoolLit extends Expression {
	private boolean truthValue;
	
	public BoolLit(boolean trueFalseValue){
		this.truthValue = trueFalseValue;
	}
	
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.BOOL_LIT;
	}
	
	public boolean getTruthValue(){
		return truthValue;
	}
	
	public void updateTruthValue(boolean newVal){
		this.truthValue = newVal;
	}

}
