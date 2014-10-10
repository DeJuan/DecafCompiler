package edu.mit.compilers.codegen;

import java.io.PrintStream;
import java.util.ArrayList;
import Descriptors.Descriptor;
import edu.mit.compilers.ir.SymbolTable;

public class CodegenContext {
	
	/**@brief flat list of assembly code.
	 * Append to this list as appropriate.
	 */
	public ArrayList<Instruction> ins;
	
	/**@brief symbol table for locations of variables*/
	public SymbolTable<Descriptor> symbol;
	
	/**@brief size of local variables in a function*/
	long localvarSize;
	
	public MemLocation rsp;
	
	public CodegenContext(){
		
	}
	
	public void incScope(){
		symbol.incScope();
	}
	
	public void decScope(){
		symbol.decScope();
	}

	/**@brief print instructions to a file.
	 * 
	 * @param ps target of print. System.out or an output file stream.
	 */
	public void printInstructions(PrintStream ps){
		
	}
	
	/**@brief for storing tmps.
	 * The backend may do something more than just push and pop.
	 * @param val
	 */
	public void push(MemLocation loc){
		
	}
	
	public MemLocation pop(){
		return null;
	}
}
