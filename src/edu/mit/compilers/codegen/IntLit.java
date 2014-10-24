package edu.mit.compilers.codegen;

public class IntLit extends Expression {
	private int value;
	
	public IntLit(int val){
		this.value = val;
	}
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.INT_LIT;
	}
	
	public int getValue(){
		return value;
	}

	public void updateValue(int newVal){
		this.value = newVal;
	}
}
