package edu.mit.compilers.codegen;

import edu.mit.compilers.ir.IR_Node;
import edu.mit.compilers.ir.Type;

/**
 * THis class is an abstract representation of a Descriptor. It contains almost all of the methods that will be use in the 
 * descriptors that inherit from it, but since many descriptors don't need all of these fields, the fields not needed
 * will throw UnsupportedOperationExceptions for each unused method if it is called.
 * 
 * @author DeJuan
 *
 */
public class Descriptor {
	protected LocationMem loc;
	protected IR_Node node;
	
	public Descriptor(IR_Node n){
		node = n;
	}
	
	public Type getType(){
		return node.getType();
	}
	
	public void setIR(IR_Node IR){
		node = IR;
	}
	
	public IR_Node getIR(){
		return node;
	}
	
	public void setLocation(LocationMem location)
	{
		this.loc = location;
	}
	
	public LocationMem getLocation()
	{
		return this.loc;
	}
	
}
