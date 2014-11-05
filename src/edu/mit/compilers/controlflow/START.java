package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.Type;
/**
 * This class represents the starting point for a given sequence of FlowNode, 
 * and contains argument information for method calls. 
 * @author DeJuan
 *
 */
public class START extends FlowNode {
	private List<FlowNode> children = new ArrayList<FlowNode>();
	private List<FlowNode> parents = new ArrayList<FlowNode>();
	private List<IR_FieldDecl> arguments = new ArrayList<IR_FieldDecl>();
	private Type retType;
	private boolean visited = false;
	private String label;
	
	/**
	 * This constructor assumes you want a blank start that will be updated later. 
	 */
	public START() {}
	
	/**
	 * This constructor allows you to store a list of arguments. Used to initialize methods.
	 * @param args : List<IR_FieldDecl> that correspond to the arguments being passed into this node. 
	 */
	public START(List<IR_FieldDecl> args, Type type) {
		this.arguments = args;
		this.retType = type;
	}

	@Override
	public List<FlowNode> getParents(){
		return parents;
	}

	@Override
	/**
	 * Returns the list of length one that contains the lone successor to the start node.
	 * @return child : List<FlowNode> of length one containing first meaningful FlowNode in the current method.  
	 */
	public List<FlowNode> getChildren() {
		return children;
	}

	@Override
	public void addParent(FlowNode newParent) {
		parents.add(newParent);
	}
	
	/**
	 * Allows you to remove a child. This is done when we want to replace a child
	 * codeblock or branch with an optimized version. 
	 * 
	 * @param child : The node we want to remove. 
	 */
	public void removeChild(FlowNode child){
		if(children.contains(child)){
			children.remove(child);
		}
	}

	@Override
	/**
	 * Adder method allowing you to append a child to the list of children. Will error if the list already has a child. 
	 */
	public void addChild(FlowNode newChild) {
		if (children.isEmpty()){
			children.add(newChild);
		} else {
		    throw new UnsupportedOperationException("This START node already has a child. No START should have more than one child.");
		}
	}
	
	/**
	 * Gives you back the list of IR_FieldDecl objects you initialized this node with. 
	 * @return arguments : list of IR_FieldDecl arguments for the method.
	 */
	public List<IR_FieldDecl> getArguments(){
		return arguments;
	}
	
	/**
	 * Returns the return type of the method. It 
	 * @return
	 */
	public Type getRetType() {
		return retType;
	}
	
	/**
	 * Traverse this FlowNode and mark visited as true.
	 */
	@Override
	public void visit() {
		visited = true;
	}
	
	/**
	 * Reset the visited flag of this FlowNode and its children.
	 * 
	 * Note: It will only reset the child if the child has been visited,
	 * meaning that resetVisit will successfully reset all visited nodes
	 * assuming that all traversals started from the ROOT.
	 */
	@Override
	public void resetVisit() {
		visited = false;
		if (children.size() > 0) {
			for (FlowNode child : children) {
				if (child.visited())
					child.resetVisit();
			}
		}
	}
	
	/**
	 * Returns whether or not this FlowNode has been traversed already.
	 */
	@Override
	public boolean visited() {
		return visited;
	}
	
	@Override
    public String getLabel() {
        return label;
    }

    /**
     * SHOULD ONLY BE CALLED ONCE
     */
    @Override
    public void setLabel(String label) {
        // Enforce called once?
        this.label = label;
    }
	
}
