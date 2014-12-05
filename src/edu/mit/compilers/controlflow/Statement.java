package edu.mit.compilers.controlflow;

import java.util.Arrays;
import java.util.HashSet;

import edu.mit.compilers.regalloc.GraphNode;
import edu.mit.compilers.regalloc.ReachingDefinition;
import edu.mit.compilers.regalloc.Web;

public abstract class Statement {
	
	protected Bitvector liveMap;
	protected GraphNode node;
	protected ReachingDefinition rd;
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
	
	public ReachingDefinition getReachingDefinition() {
		return rd;
	}
	
	public void setReachingDefinition(ReachingDefinition rd) {
		this.rd = new ReachingDefinition(rd);
	}
	
	public void setWeb(Web web) {
		String varName = web.getVarName();
		this.rd.setWebs(varName, new HashSet<Web>(Arrays.asList(web)));
	}
}
