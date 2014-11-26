package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.Ops;

/**
 * This class represents statements that assign values. It is a subclass of Statement.
 * 
 * @author DeJuan
 *
 */
public class Assignment extends Statement {
	private Var destVar;
	private Ops operator;
	private Expression value;
	
	/**
	 * Initialize a new Assignment, with the parameters below
	 * @param destinationVariable : The variable being assigned.
	 * @param operator : The operator being used to do the assignment: =, +=, or -=
	 * @param valueExpr : The value being assigned to the variable. 
	 */
	public Assignment(Var destinationVariable, Ops operator, Expression valueExpr){
		this.destVar = destinationVariable;
		this.operator = operator;
		this.value = valueExpr;
	}
	
	/**
	 * Gets the variable being assigned.
	 * @return Var : the destination variable
	 */
	public Var getDestVar(){
		return destVar;
	}
	
	/**
	 * Gets the operator being used to do the assignment.
	 * @return Ops : a member of the enum Ops. Should be +, +=, or -=. 
	 */
	public Ops getOperator(){
		return operator;
	}
	
	/**
	 * Returns the expression being assigned to the variable. 
	 * @return Expression : representation of the assignment
	 */
	public Expression getValue(){
		return value;
	}
	

	@Override
	/**
	 * Tells you that this is an ASSIGNMENT. 
	 * @return StatementType : ASSIGNMENT
	 */
	public StatementType getStatementType() {
		return StatementType.ASSIGNMENT;
	}

	@Override
	public Bitvector getLiveMap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLiveMap(Bitvector bv) {
		// TODO Auto-generated method stub
		
	}
}
