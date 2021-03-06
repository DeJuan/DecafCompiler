package edu.mit.compilers.controlflow;

import edu.mit.compilers.codegen.LocReg;
import edu.mit.compilers.codegen.Regs;
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
	
	public LocReg getRegister() {
		if (this.getNode() == null) {
			return null;
		}
		Regs reg = this.getNode().getRegister();
		if (reg != null) {
			return new LocReg(reg);
		} else{
			return null;
		}
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

}
