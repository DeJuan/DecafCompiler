package edu.mit.compilers.regalloc;

import edu.mit.compilers.codegen.Regs;
import edu.mit.compilers.controlflow.Assignment;

public class GraphNode {
	
	Assignment st;
	ReachingDefinition rd;
	
	Boolean isGlobal = false;
	Boolean isParam = false;
	
	Regs register = null;
	
	Boolean removed = false; // used to represent that a node has been removed in coloring
	Boolean spill = false;
	
	public GraphNode(Assignment st) {
		this.st = st;
		this.rd = st.getReachingDefinition();
	}
	
	public Assignment getAssignment() {
		return this.st;
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
	
	public ReachingDefinition getReachingDefinition() {
		return this.rd;
	}
}
