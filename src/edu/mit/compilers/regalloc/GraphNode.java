package edu.mit.compilers.regalloc;

import edu.mit.compilers.codegen.Regs;
import edu.mit.compilers.controlflow.Assignment;
import edu.mit.compilers.controlflow.Statement;

public class GraphNode {
	
	String varName;
	int level;
	Boolean isGlobal = false;
	Boolean isParam = false;
	
	Regs register = null;
	
	Boolean removed = false; // used to represent that a node has been removed in coloring
	Boolean spill = false;
	
	public GraphNode(Assignment assign, int level) {
		this.varName = assign.getDestVar().getName();
		this.level = level;
	}
	
	public GraphNode(String varName, int level) {
		this.varName = varName;
		this.level = level;
	}
	
	public GraphNode(String varName, int level, Boolean isGlobal, Boolean isParam) {
		this.varName = varName;
		this.level = level;
		this.isGlobal = isGlobal;
		this.isParam = isParam;
	}
	
	public GraphNode(Statement st) {
		if (st instanceof Assignment) {
			this.varName = ((Assignment) st).getDestVar().getName();
		} else {
			// might not need a GraphNode for non-Assignment statements
			this.varName = "";
		}
	}
	
	public String getVarName() {
		return this.varName;
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
	
	public int getLevel() {
		return this.level;
	}
}
