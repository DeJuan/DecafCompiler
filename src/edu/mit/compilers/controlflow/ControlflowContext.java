package edu.mit.compilers.controlflow;

import java.util.Stack;

import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.controlflow.Branch;
import edu.mit.compilers.ir.SymbolTable;

/**@brief Information used throughout the control flow.
 */
public class ControlflowContext {
	
	/**@brief symbol table for locations of variables*/
	public SymbolTable<Descriptor> symbol;
	
	// Stack implementation that keeps track of which for/while loop we are in.
	private Stack<Branch> loopScope;
	
	public ControlflowContext() {
		symbol = new SymbolTable<Descriptor>();
		symbol.incScope();
		loopScope = new Stack<Branch>();
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
	
	public Branch getInnermostLoop() {
		return loopScope.peek();
	}
	
	public void enterLoop(Branch loop) {
		loopScope.push(loop);
	}
	
	public void exitLoop() {
		loopScope.pop();
	}
	
}
