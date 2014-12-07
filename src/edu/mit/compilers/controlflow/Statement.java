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
	
	public ReachingDefinition getReachingDefinition() {
		return rd;
	}
	
	public void setReachingDefinition(ReachingDefinition rd) {
		this.rd = new ReachingDefinition(rd);
	}
	
	public void setWeb(Web web) {
		IR_FieldDecl decl = web.getFieldDecl();
		this.rd.setWebs(decl, new HashSet<Web>(Arrays.asList(web)));
	}
}
