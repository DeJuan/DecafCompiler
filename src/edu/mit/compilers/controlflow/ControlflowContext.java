package edu.mit.compilers.controlflow;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import edu.mit.compilers.codegen.CodegenConst;
import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.codegen.Instruction;
import edu.mit.compilers.codegen.LocStack;
import edu.mit.compilers.codegen.LocationMem;
import edu.mit.compilers.ir.SymbolTable;

/**@brief Information used throughout the control flow.
 */
public class ControlflowContext {
	
    /**@brief flat list of assembly code.
     * Append to this list as appropriate.
     */
    public ArrayList<Instruction> ins;
    
	/**@brief symbol table for locations of variables*/
	public SymbolTable<Descriptor> symbol;
	
	// Stack implementation that keeps track of which for/while loop we are in.
	private Stack<Branch> loopScope;
	
	/**@brief size of local variables in a function*/
	ArrayList<Long> localVarSize;
	
	/**@brief maximum stack size required by local variables in a function.
	 */
	long totalLocalSize, maxLocalSize;
	
	private int numLabels;
	
	/**@brief String literals are replaced with their corresponding
     * labels
     */
    public HashMap<String, Long> stringLiterals;
	
	/**@brief location of rsp with respect to rbp
     * Only used to compute statically local variable location on stack.
     */
    private LocStack rsp;
    
    private String arrayBoundLabel;
	
	public ControlflowContext() {
        stringLiterals = new HashMap<String,Long>();
	    ins = new ArrayList<Instruction>();
	    symbol = new SymbolTable<Descriptor>();
	    loopScope = new Stack<Branch>();
		symbol.incScope();
		rsp = new LocStack();
		numLabels = 0;
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
	
	/**@brief initializes rsp, maxLocalSize and localVarSize.
	 */
	public void enterFun(){
	    rsp.setValue(0);
		maxLocalSize = 0;
		totalLocalSize = 0;
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
	
	/**@brief generate a unique jump label.
     * 
     * @return String for the label.
     */
    public String genLabel(){
        String label = ".L"+numLabels;
        numLabels++;
        return label;
    }
    
    /**@brief Push value stored in loc to the stack.
     * The backend may do something more than just push and pop.
     * @param val
     */
    public List<Instruction> push(LocationMem loc){
        ArrayList<Instruction> il = new ArrayList<Instruction>();
        il.add(new Instruction("pushq", loc));
        rsp.setValue(rsp.getValue() - CodegenConst.INT_SIZE);
        return il;
    }

    /**@brief 
     * @param loc pop the top of stack pointed to by rsp and store the value in loc
     */
    public List<Instruction> pop(LocationMem loc){
        ArrayList<Instruction> il = new ArrayList<Instruction>();
        il.add(new Instruction("popq", loc));
        rsp.setValue(rsp.getValue() + CodegenConst.INT_SIZE);
        return il;
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
	
	/**@brief print instructions to a file.
     * 
     * @param ps target of print. System.out or an output file stream.
     */
    public void printInstructions(PrintStream ps){
        // TODO: Implement
        clearJumps();
    }
    
    private void clearJumps() {
        //TODO: Implement
    }
    
    public String getArrayBoundLabel(){
        if(arrayBoundLabel == null){
            arrayBoundLabel = genLabel();
        }
        return arrayBoundLabel;
    }
	
}
