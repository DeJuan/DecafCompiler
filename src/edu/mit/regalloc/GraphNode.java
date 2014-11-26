package edu.mit.regalloc;

import edu.mit.compilers.controlflow.Assignment;
import edu.mit.compilers.controlflow.Statement;

public class GraphNode {
	
	String varName;
	
	public GraphNode(Statement st) {
		this.varName = "";
	}
	
	public GraphNode(Assignment assign) {
		this.varName = assign.getDestVar().getName();
	}
	
	public GraphNode(String varName) {
		this.varName = varName;
	}

}
