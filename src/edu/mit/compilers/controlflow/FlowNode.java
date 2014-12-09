package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.regalloc.ReachingDefinition;
import edu.mit.compilers.regalloc.Web;

public abstract class FlowNode {
	
	protected Bitvector liveMap;
	protected ReachingDefinition IN;
	protected ReachingDefinition OUT;
	protected boolean visited = false;
	protected String label;
	
	public abstract List<FlowNode> getParents();
	public abstract List<FlowNode> getChildren();
	public abstract void addParent(FlowNode newParent);
	public abstract void addChild(FlowNode newChild);
	public abstract void replaceParent(FlowNode newParent, FlowNode oldParent);
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
	
	public ReachingDefinition getOUT() {
		return this.OUT;
	}
	
	public void setOUT(ReachingDefinition OUT) {
		this.OUT = OUT;
	}
	
	public ReachingDefinition getIN() {
		return this.IN;
	}
	
	public void setIN(ReachingDefinition IN) {
		this.IN = IN;
	}
	
	public void setWeb(Web newWeb, HashSet<Web> oldWebs) {
		IR_FieldDecl decl = newWeb.getFieldDecl();
		System.out.println("Size of oldWebs: " + oldWebs);
		if (IN.getWebsMap().containsKey(decl) && 
				oldWebs.contains((new ArrayList<Web>(IN.getWebsMap().get(decl)).get(0)))) {
			// only set IN if original IN contains old web.
			System.out.println("Replacing IN");
			IN.setWebs(decl, new HashSet<Web>(Arrays.asList(newWeb)));
		}
		if (OUT.getWebsMap().containsKey(decl) && 
				oldWebs.contains((new ArrayList<Web>(OUT.getWebsMap().get(decl)).get(0)))) {
			// only set OUT if original OUT contains old web.
			System.out.println("Replacing OUT");
			OUT.setWebs(decl, new HashSet<Web>(Arrays.asList(newWeb)));
		}
	}
	
}
