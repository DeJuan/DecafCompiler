package edu.mit.compilers.codegen;

import java.io.PrintStream;
import java.util.ArrayList;
import Descriptors.Descriptor;
import edu.mit.compilers.ir.SymbolTable;

public class CodegenContext {
	public ArrayList<Instruction> ins;
	public SymbolTable<Descriptor> symbol;
	public MemLocation rsp;
	/**@brief print instructions to a file.
	 * 
	 * @param ps target of print. System.out or an output file stream.
	 */
	public void printInstructions(PrintStream ps){
		
	}
}
