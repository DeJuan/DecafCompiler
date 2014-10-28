package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Type;

public class NotExpr extends Expression {
	boolean value;
	Expression express;
	
	public NotExpr(Expression expr){
		switch(expr.getExprType()){
		case VAR:
			Var varExpr = (Var)expr;
			if(varExpr.getVarDescriptor().getType() != Type.BOOL){
				throw new UnsupportedOperationException("Tried to NOT a non boolean variable.");
			}
			// TODO : Look up truth value of var and invert it then store it locally
			this.express = varExpr;
			break;
		case COND_EXPR:
			CondExpr conExpr = (CondExpr)expr;
			// TODO : Can't resolve the expr as implemented, would need to look up somehow. So just store the expr.
			this.express = conExpr;
			break;
		case COMP_EXPR:
			CompExpr comExpr = (CompExpr)expr;
			// TODO : Can't resolve the expr as implemented, would need to look up somehow. So just store it.
			this.express = comExpr; 
			break;
		case EQ_EXPR:
			EqExpr eqExpr = (EqExpr) expr;
			// TODO : same as above
			this.express = eqExpr;
			break;
		case BOOL_LIT:
			BoolLit truthValue = (BoolLit)expr;
			this.value = !truthValue.getValue();
			this.express = truthValue;
			break;
		case NOT:
			this.express = (NotExpr) expr;
			break;
		default:
			throw new UnsupportedOperationException("Tried to NOT something that couldn't possibly resolve to a boolean.");
		}
	}
	
	@Override
	public ExpressionType getExprType() {
		return ExpressionType.NOT;
	}
	
	public Expression getUnresolvedExpression(){
		return this.express;
	}

}
