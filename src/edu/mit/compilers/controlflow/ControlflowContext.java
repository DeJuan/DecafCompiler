package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.controlflow.Branch;
import edu.mit.compilers.ir.SymbolTable;

/**@brief Information used throughout the control flow.
 */
public class ControlflowContext {
	
	/**@brief symbol table for locations of variables*/
	public SymbolTable<Descriptor> symbol;
	
	// Stack implementation that keeps track of which for/while loop we are in.
	private List<Branch> loopScope;
	
	public ControlflowContext() {
		symbol = new SymbolTable<Descriptor>();
		symbol.incScope();
		loopScope = new ArrayList<Branch>();
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
		return loopScope.get(loopScope.size() - 1);
	}
	
	public void enterLoop(Branch loop) {
		loopScope.add(loop);
	}
	
	public void exitLoop() {
		loopScope.remove(loopScope.size() - 1);
	}
	
}
