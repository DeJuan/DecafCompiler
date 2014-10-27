package edu.mit.compilers.controlflow;

import java.util.Stack;

import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.controlflow.Branch;
import edu.mit.compilers.ir.SymbolTable;

/**@brief Information used throughout the control flow.
 */
public class ControlflowContext {
	
	/**@brief symbol table for locations of variables*/
	public SymbolTable<Descriptor> symbol = new SymbolTable<Descriptor>();
	
	// Stack implementation that keeps track of which for/while loop we are in.
	private Stack<Branch> loopScope = new Stack<Branch>();
	
	public ControlflowContext() {
		symbol.incScope();
	}
	
	/**
	 * @param name
	 * @param d
	 * @return true if there is a duplicated variable in the same scope.
	 */
	public boolean putSymbol(String name, Descriptor d){
		return symbol.put(name, d);
	}
	
	/**
	 * @param name
	 * @return first Descriptor in the deepest scope.
	 * null if symbol does not exist.
	 */
	public Descriptor findSymbol(String name){
		return symbol.lookup(name);
	}
	
	/**@brief convenience functions for symbol table.
	 * Push a new symbol table when entering a block.
	 */
	public void incScope(){
		symbol.incScope();
	}
	
	public void decScope(){
		symbol.decScope();
	}	
	
	// Returns the Branch object for the innermost loop we are in. Used for continue/break.
	public Branch getInnermostLoop() {
		return loopScope.peek();
	}
	
	// Entering a 'for' or 'while' loop; push to stack.
	public void enterLoop(Branch loop) {
		loopScope.push(loop);
	}
	
	// Exiting a 'for' or 'while' loop; pop from stack.
	public void exitLoop() {
		loopScope.pop();
	}
	
}
