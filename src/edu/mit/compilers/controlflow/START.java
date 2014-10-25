package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.IR_FieldDecl;
/**
 * This class represents the starting point for a given sequence of Codeblocks, and contains argument information for method calls. 
 * @author DeJuan
 *
 */
public class START extends FlowNode {
	private List<FlowNode> child = new ArrayList<FlowNode>();
	private List<IR_FieldDecl> arguments = new ArrayList<IR_FieldDecl>();
	
	/**
	 * This is the first of four constructors for the START node. It assumes you want a blank start that will be updated later. 
	 */
	public START(){}
	
	/**
	 * This is the second of four constructors for the START node. It assumes you already know the child node that should be associated with this Start, but have no arguments to pass in.
	 * @param childNode : FlowNode representing immediate successor to the start node, the first meaningful FlowNode in the sequence representing a method. 
	 */
	public START(FlowNode childNode){
		this.child.add(childNode);
	}
	
	/**
	 * This is the third of four constructors for the START node. It assumes you do not yet know the child node, but do want to store a list of arguments inside this Start.
	 * @param args : List<IR_FieldDecl> that correspond to the arguments being passed into this node. 
	 */
	public START(List<IR_FieldDecl> args){
		this.arguments = args;
	}
	
	/**
	 * This is the fourth of four constructors for the START node. It assumes you know both the child node and have a list of arguments you wish to store inside the Start.
	 * @param args : List<IR_FieldDecl> representing arguments to the method this START node begins.
	 * @param childNode : FlowNode representing first meaningful FlowNode in the sequence representing the current method.
	 */
	public START(List<IR_FieldDecl> args, FlowNode childNode){
		this.child.add(childNode);
		this.arguments = args;
	}
	@Override
	/**
	 * Tells you that you're working with a START.
	 * @return NodeType : START
	 */
	public NodeType getType() {
		return NodeType.START;
	}

	@Override
	public List<FlowNode> getParents(){
		throw new UnsupportedOperationException("The Origin of All has no predecessors.");
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
		throw new UnsupportedOperationException("You cannot force a START to have parents.");
		
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
