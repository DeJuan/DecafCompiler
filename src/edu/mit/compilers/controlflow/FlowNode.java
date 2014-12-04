package edu.mit.compilers.controlflow;

import java.util.List;

import edu.mit.compilers.regalloc.ReachingDefinition;

public abstract class FlowNode {
	
	protected Bitvector liveMap;
	protected ReachingDefinition rd;
	protected boolean visited = false;
	protected String label;
	
	public abstract List<FlowNode> getParents();
	public abstract List<FlowNode> getChildren();
	public abstract void addParent(FlowNode newParent);
	public abstract void addChild(FlowNode newChild);
	public abstract void resetVisit();
	
	/**
	 * Traverse this FlowNode and mark visited as true.
	 */
	public void visit() {
		visited = true;
	}
	
	public void makeVisitFalse(){
		visited = false;
	}
	
	/**
	 * Returns whether or not this FlowNode has been traversed already.
	 */
	public boolean visited() {
		return visited;
	}
	
    public String getLabel() {
        return label;
    }
    
    /**
     * SHOULD ONLY BE CALLED ONCE
     */
    public void setLabel(String label) {
        // Enforce called once?
        this.label = label;
    }
	
	public Bitvector getLiveMap() {
		return liveMap;
	}
	
	public void setLiveMap(Bitvector bv) {
		this.liveMap = bv;
	}
	
	public ReachingDefinition getReachingDefinition() {
		return this.rd;
	}
	
	public void setReachingDefinition(ReachingDefinition rd) {
		this.rd = rd;
	}
	
}
