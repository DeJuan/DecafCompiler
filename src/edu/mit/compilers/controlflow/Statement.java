package edu.mit.compilers.controlflow;

import edu.mit.regalloc.GraphNode;

public abstract class Statement {
	
	protected Bitvector liveMap;
	protected GraphNode node;
	public enum StatementType{
		ASSIGNMENT, METHOD_CALL_STATEMENT, DECLARATION
	};
	
	public abstract StatementType getStatementType();
	
	public Bitvector getLiveMap() {
		return liveMap;
	}
	
	public void setLiveMap(Bitvector bv) {
		this.liveMap = bv;
	}
	
	public void setNode(GraphNode node) {
		this.node = node;
	}
	
	public GraphNode getNode() {
		return node;
	}
}
