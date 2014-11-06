package edu.mit.compilers.codegen;

import edu.mit.compilers.ir.IR_Node;
import edu.mit.compilers.ir.Type;

/**
 * This class is our representation of a Descriptor. It takes in an IR_Node n and stores that locally. 
 * setLocation should also be called on this descriptor!
 * 
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
