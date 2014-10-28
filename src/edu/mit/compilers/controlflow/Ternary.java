package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Type;

public class Ternary extends Expression {
	Expression cond;
	Expression trueBranch;
	Expression falseBranch;
	
	public Ternary(Expression condition, Expression truePath, Expression falsePath){
		switch(condition.getExprType()){
		case BOOL_LIT:
			this.cond = (BoolLit)condition;
			break;
		case COND_EXPR:
			this.cond = (CondExpr)condition;
			break;
		case COMP_EXPR:
			this.cond = (CompExpr)condition;
			break;
		case VAR:
			Var varCond = (Var)condition;
			if(varCond.getVarDescriptor().getType() != Type.BOOL){
				throw new UnsupportedOperationException("Tried to use a non-boolean expression as the condition for a ternary expression.");
			}
			break;
		default:
			throw new UnsupportedOperationException("Tried to use something that could never resolve to a truth value as the condition for a ternary expression.");
		}
		
		this.trueBranch = truePath;
		this.falseBranch = falsePath;
	}
	
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.TERNARY;
	}
	
	public Expression getTernaryCondition(){
		return this.cond;
	}
	
	public Expression getTrueBranch(){
		return this.trueBranch;
	}
	
	public Expression getFalseBranch(){
		return this.falseBranch;
	}
}
