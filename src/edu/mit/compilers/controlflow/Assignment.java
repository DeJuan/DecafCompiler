package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Ops;

public class Assignment extends Statement {
	private Var destVar;
	private Ops operator;
	private Expression value;
	
	public Assignment(Var destinationVariable, Ops operator, Expression valueExpr){
		this.destVar = destinationVariable;
		this.operator = operator;
		this.value = valueExpr;
	}
	
	public Var getDestVar(){
		return destVar;
	}
	
	public Ops getOperator(){
		return operator;
	}
	
	public Expression getValue(){
		return value;
	}

	@Override
	public StatementType getStatementType() {
		return StatementType.ASSIGNMENT;
	}
}
