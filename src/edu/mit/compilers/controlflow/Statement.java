package edu.mit.compilers.controlflow;

import java.util.Arrays;
import java.util.HashSet;

import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.regalloc.GraphNode;
import edu.mit.compilers.regalloc.ReachingDefinition;
import edu.mit.compilers.regalloc.Web;

public abstract class Statement {
	
	protected GraphNode node;
	protected ReachingDefinition rd;
	protected Bitvector liveMap;
	
	public enum StatementType{
		ASSIGNMENT, METHOD_CALL_STATEMENT, DECLARATION
	};
	
	public abstract StatementType getStatementType();
	
	public void setNode(GraphNode node) {
		this.node = node;
	}
	
	public GraphNode getNode() {
		return node;
	}
	
	public Bitvector getLiveMap() {
		return liveMap;
	}
	
	public void setLiveMap(Bitvector liveMap) {
		this.liveMap = liveMap;
	}
	
	public ReachingDefinition getReachingDefinition() {
		return rd;
	}
	
	public void setReachingDefinition(ReachingDefinition rd) {
		this.rd = new ReachingDefinition(rd);
		if (this.rd == null) {
			throw new RuntimeException("NULL RD");
		}
	}
	
	public void setWeb(Web web) {
		IR_FieldDecl decl = web.getFieldDecl();
		System.out.println("Statement: " + this + ". Old web: " + this.rd.getWebsMap().get(decl) + " new web: " + web);
		this.rd.setWebs(decl, new HashSet<Web>(Arrays.asList(web)));
	}
}
