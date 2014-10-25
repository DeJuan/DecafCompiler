package edu.mit.compilers.controlflow;

import edu.mit.compilers.ir.IR_FieldDecl;

public class Declaration extends Statement {
	private IR_FieldDecl fieldDecl;
	
	public Declaration(IR_FieldDecl fieldDeclaration){
		this.fieldDecl = fieldDeclaration;
	}
	
	@Override
	public StatementType getStatementType() {
		return StatementType.DECLARATION;
	}
	
	public IR_FieldDecl getFieldDecl(){
		return this.fieldDecl;
	}

}
