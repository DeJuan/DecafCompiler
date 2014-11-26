package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.IR_FieldDecl;

/**
 * This class is a convenient way to pass field declarations around in our newer control flow structures. 
 * @author DeJuan
 *
 */
public class Declaration extends Statement {
	private IR_FieldDecl fieldDecl;
	
	/**
	 * This is the constructor for declarations. It just stores the IR_FieldDecl created when we traversed the AST for this declaration. 
	 * @param fieldDeclaration : IR_FieldDecl 
	 */
	public Declaration(IR_FieldDecl fieldDeclaration){
		this.fieldDecl = fieldDeclaration;
	}
	
	
	@Override
	/**
	 * Lets you know that you're working with a declaration
	 * @return StatementType : DECLARATION
	 */
	public StatementType getStatementType() {
		return StatementType.DECLARATION;
	}
	
	/**
	 * Lets you get back the IR_FieldDecl that was given during construction
	 * @return fieldDecl : IR_FieldDecl that was passed in during construction.
	 */
	public IR_FieldDecl getFieldDecl(){
		return this.fieldDecl;
	}
	
	public String getName(){
		return fieldDecl.getName();
	}


	@Override
	public Bitvector getLiveMap() {
		// TODO Auto-generated method stub
		return null;
	}

}
