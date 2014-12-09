package edu.mit.compilers.regalloc;

import edu.mit.compilers.codegen.Regs;

/**
 * Each node represents a Web. Edges between nodes mean that those nodes
 * cannot be assigned to the same register.
 *
 */
public class GraphNode {
	
	Web web;
	
	Boolean isGlobal = false;
	Boolean isParam = false;
	
	Regs register = null;
	
	Boolean removed = false; // used to represent that a node has been removed in coloring
	Boolean spill = false;
	
	public GraphNode(Web web) {
		this.web = web;
	}
	
	public Web getWeb() {
		return this.web;
	}
	
	public boolean hasAssignedRegister() {
		return (register != null);
	}
	
	public Regs getRegister() {
		return register;
	}
	
	public void setRegister(Regs register) {
		this.register = register;
	}
	
	public void markAsRemoved() {
		this.removed = true;
	}
	
	public void unmarkAsRemoved() {
		this.removed = false;
	}
	
	public Boolean isRemoved() {
		return removed;
	}
	
	public void spill() {
		this.spill = true;
	}
	
	public void unspill() {
		this.spill = false;
	}
	
	public Boolean isSpill() {
		return this.spill;
	}
	
}
