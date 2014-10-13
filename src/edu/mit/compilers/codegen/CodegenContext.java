package edu.mit.compilers.codegen;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.mit.compilers.ir.SymbolTable;

/**@brief Information used throughout codegen
 */
public class CodegenContext {
	/**@brief flat list of assembly code.
	 * Append to this list as appropriate.
	 */
	public ArrayList<Instruction> ins;
	
	/**@brief symbol table for locations of variables*/
	public SymbolTable<Descriptor> symbol;
	
	/**@brief String literals are replaced with their corresponding
	 * labels
	 */
	public HashMap<String, Long> stringLiterals;
	
	/**@brief size of local variables in a function*/
	ArrayList<Long> localVarSize;
	
	/**@brief maximum stack size required by local variables in a function.
	 */
	long totalLocalSize, maxLocalSize;
	
	/**@brief location of rsp with respect to rbp
	 * used to compute statically local variable location on stack.
	 */
	private LocStack rsp;
	
	private int numLabels;
	
	public CodegenContext(){
		stringLiterals = new HashMap<String,Long>();
		symbol = new SymbolTable<Descriptor>();
		ins = new ArrayList<Instruction>();
		rsp = new LocStack();
		symbol.incScope();
		numLabels=0;
	}
	
	/**@brief generate a unique jump label.
	 * 
	 * @return String for the label.
	 */
	public String genLabel(){
		String label = ".L"+numLabels;
		numLabels++;
		return label;
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
	
	void addIns(Instruction ii){
		ins.add(ii);
	}
	
	void addIns(List<Instruction>ll){
		ins.addAll(ll);
	}
	
	public static String StringLiteralLoc(long idx){
		return ".LC"+idx;
	}
	
	/**@brief print instructions to a file.
	 * 
	 * @param ps target of print. System.out or an output file stream.
	 */
	public void printInstructions(PrintStream ps){
		//header
		
		//string literals
		long nString = stringLiterals.keySet().size();
		if(nString>0){
			ps.println(".section  .rodata");
		}
		String ss[] = new String[(int) nString];
		
		for(String k: stringLiterals.keySet()){
			long idx = stringLiterals.get(k);
			ss[(int) idx] = k;
		}
		
		for(int ii =0 ;ii<ss.length;ii++){
			ps.print (StringLiteralLoc(ii) + ": \n.string \"" + ss[ii]+ "\"\n");
		}
		
		for(int ii = 0;ii<ins.size();ii++){
			ps.println(ins.get(ii));
		}
	}
	
	/**@brief initializes rsp, maxLocalSize and localVarSize.
	 * 
	 */
	public void enterFun(){
		//rsp at the same place as rbp
		rsp.setValue(0);
		maxLocalSize=0;
		totalLocalSize=0;
		localVarSize = new ArrayList<Long>();
		localVarSize.add(0L);
	}
	
	public LocStack getRsp(){
		return rsp.clone();
	}
	
	public void setRsp(long offset){
		rsp.setValue(offset);
	}
	
	/**@brief Called when entering a block.
	 * Field declaration always appears before other statements.
	 * @param size byte size
	 * @return
	 */
	public LocStack allocLocal(long size){
		long offset = rsp.getValue();
		offset-=size;
		rsp.setValue(offset);
		
		int idx = localVarSize.size()-1;
		long blockSize = localVarSize.get(idx);
		blockSize+= size;
		localVarSize.set(idx, blockSize);
		totalLocalSize += size;
		return rsp.clone();
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

		long offset = rsp.getValue();
		offset+=locals;
		rsp.setValue(offset);		
	}
	
	/**@brief Push value stored in loc to the stack.
	 * The backend may do something more than just push and pop.
	 * @param val
	 */
	public List<Instruction> push(LocationMem loc){
		ArrayList<Instruction> il = new ArrayList<Instruction>();
		il.add(new Instruction("push", loc));
		rsp.setValue(rsp.getValue() - CodegenConst.INT_SIZE);
		return il;
	}

	/**@brief 
	 * @param loc pop the top of stack pointed to by rsp and store the value in loc
	 */
	public List<Instruction> pop(LocationMem loc){
		ArrayList<Instruction> il = new ArrayList<Instruction>();
		il.add(new Instruction("pop", loc));
		rsp.setValue(rsp.getValue() + CodegenConst.INT_SIZE);
		return il;
	}
}
