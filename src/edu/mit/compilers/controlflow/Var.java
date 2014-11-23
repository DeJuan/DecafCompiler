package edu.mit.compilers.controlflow;

import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.ir.IR_FieldDecl;

/**
 * This class represents variables. 
 * @author DeJuan
 *
 */
public class Var extends Expression {
	private Descriptor var;
	private String name;
	private IR_FieldDecl decl;
	private Expression index;
	private ValueID val;
	private boolean isCompilerTemp;
	
	/**
	 * This constructor takes in the Descriptor associated with the variable we want to store and keeps it locally. 
	 * @param variable
	 * @param index : the index into an array, or null if int/bool
	 */
	public Var(Descriptor variable, Expression index) {
	    this.var = variable;
	    IR_FieldDecl ir = (IR_FieldDecl) variable.getIR();
	    this.decl = ir;
	    this.name = ir.getName();
	    this.index = index;
	    isCompilerTemp = false;
	}
	
	/**
     * This constructor takes in the Descriptor associated with the variable we want to store and keeps it locally. 
     * @param variable
     * @param index : the index into an array, or null if int/bool
     */
    public Var(Descriptor variable, Expression index, boolean temp) {
        this.var = variable;
        IR_FieldDecl ir = (IR_FieldDecl) variable.getIR();
        this.decl = ir;
        this.name = ir.getName();
        this.index = index;
        isCompilerTemp = temp;
    }
	
	@Override
	/**
	 * Tells you that you're working with a variable.
	 * @return ExpressionType : VAR
	 */
	public ExpressionType getExprType() {
		return ExpressionType.VAR;
	}
	
	/**
	 * Gives you back the descriptor that is stored locally.
	 * @return var : Descriptor for the variable this object represents
	 */
	public Descriptor getVarDescriptor(){
		return var;
	}
	
	public IR_FieldDecl getDecl(){
	    return decl;
	}
	
	/**
	 * Gets the index into an array
	 * @return Expression: index into the array, or null if an int/bool
	 */
	public Expression getIndex() {
	    return index;
	}
	
	public void setValueID(ValueID val){
	    this.val = val;
	}
	
	public ValueID getValueID() {
	    return val;
	}
	
	public boolean isCompilerTemp(){
	    return isCompilerTemp;
	}
	
	/**
	 * Returns the name of the variable
	 * @return String: the name of the variable
	 */
	public String getName(){
	    return name;
	}
}
