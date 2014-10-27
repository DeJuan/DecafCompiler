package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.IR_FieldDecl;
/**
 * This class represents the starting point for a given sequence of FlowNode, 
 * and contains argument information for method calls. 
 * @author DeJuan
 *
 */
public class START extends FlowNode {
	private List<FlowNode> child = new ArrayList<FlowNode>();
	private List<FlowNode> parent = new ArrayList<FlowNode>();
	private List<IR_FieldDecl> arguments = new ArrayList<IR_FieldDecl>();
	
	/**
	 * This constructor assumes you want a blank start that will be updated later. 
	 */
	public START(){}
	
	/**
	 * This constructor allows you to store a list of arguments. Used to initialize methods.
	 * @param args : List<IR_FieldDecl> that correspond to the arguments being passed into this node. 
	 */
	public START(List<IR_FieldDecl> args){
		this.arguments = args;
	}

	public NodeType getType() {
		return NodeType.START;
	}

	@Override
	public List<FlowNode> getParents(){
		return parent;
	}

	@Override
	/**
	 * Returns the list of length one that contains the lone successor to the start node.
	 * @return child : List<FlowNode> of length one containing first meaningful FlowNode in the current method.  
	 */
	public List<FlowNode> getChildren() {
		return this.child;
	}

	@Override
	public void addParent(FlowNode newParent) {
		parent.add(newParent);
	}

	@Override
	/**
	 * Adder method allowing you to append a child to the list of children. Will error if the list already has a child. 
	 */
	public void addChild(FlowNode newChild) {
		if (this.child.isEmpty()){
			this.child.add(newChild);
		}
		else throw new UnsupportedOperationException("This START node already has a child. No START should have more than one child.");
	}
	
	/**
	 * Gives you back the list of IR_FieldDecl objects you initialized this node with. 
	 * @return
	 */
	public List<IR_FieldDecl> getArguments(){
		if(this.arguments.isEmpty()){
			System.err.println("WARNING: You are getting an empty argument list!");
		}
		return this.arguments;
	}
	
}
