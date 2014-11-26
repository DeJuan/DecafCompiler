package edu.mit.regalloc;

import edu.mit.compilers.controlflow.Assignment;

public class GraphNode {
	
	String varName;
	
	public GraphNode(Assignment assign) {
		this.varName = assign.getDestVar().getName();
	}
	
	public GraphNode(String varName) {
		this.varName = varName;
	}

}
