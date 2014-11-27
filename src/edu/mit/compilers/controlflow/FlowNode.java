package edu.mit.compilers.controlflow;

import java.util.List;

public abstract class FlowNode {
	
	protected Bitvector liveMap;
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
	
}
