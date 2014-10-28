package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Stack;

import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.codegen.LocStack;
import edu.mit.compilers.controlflow.Branch;
import edu.mit.compilers.ir.SymbolTable;

/**@brief Information used throughout the control flow.
 */
public class ControlflowContext {
	
	/**@brief symbol table for locations of variables*/
	public SymbolTable<Descriptor> symbol = new SymbolTable<Descriptor>();
	
	// Stack implementation that keeps track of which for/while loop we are in.
	private Stack<Branch> loopScope = new Stack<Branch>();
	
	/**@brief size of local variables in a function*/
	ArrayList<Long> localVarSize;
	
	/**@brief maximum stack size required by local variables in a function.
	 */
	long totalLocalSize, maxLocalSize;
	
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
	
	/**@brief initializes rsp, maxLocalSize and localVarSize.
	 */
	public void enterFun(){
		maxLocalSize = 0;
		totalLocalSize = 0;
		localVarSize = new ArrayList<Long>();
		localVarSize.add(0L);
	}
	
	/**@brief Called when entering a block.
	 * Field declaration always appears before other statements.
	 * @param size byte size
	 * @return
	 */
	public LocStack allocLocal(long size){
		totalLocalSize += size;
		long offset = totalLocalSize;
		int idx = localVarSize.size()-1;
		long blockSize = localVarSize.get(idx);
		blockSize+= size;
		localVarSize.set(idx, blockSize);
		return new LocStack(-offset);
	}
	
	/**@brief convenience functions for symbol table.
	 * Push a new symbol table when entering a block.
	 */
	public void incScope(){
		symbol.incScope();
		localVarSize.add(0L);
	}
	
	public void decScope(){
		symbol.decScope();
		int idx = localVarSize.size()-1;
		long locals = localVarSize.get(idx);
		localVarSize.remove(idx);
		if(totalLocalSize>maxLocalSize){
			maxLocalSize = totalLocalSize;
		}
		totalLocalSize -= locals;
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
