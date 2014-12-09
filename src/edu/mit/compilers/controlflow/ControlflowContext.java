package edu.mit.compilers.controlflow;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.mit.compilers.codegen.CodegenConst;
import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.codegen.Instruction;
import edu.mit.compilers.codegen.LocLabel;
import edu.mit.compilers.codegen.LocLiteral;
import edu.mit.compilers.codegen.LocReg;
import edu.mit.compilers.codegen.LocStack;
import edu.mit.compilers.codegen.LocationMem;
import edu.mit.compilers.codegen.Regs;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;
import edu.mit.compilers.ir.IR_Node;
import edu.mit.compilers.ir.SymbolTable;
import edu.mit.compilers.regalloc.RegisterTable;

/**@brief Information used throughout the control flow.
 */
public class ControlflowContext {

    /**@brief flat list of assembly code.
     * Append to this list as appropriate.
     */
    public ArrayList<Instruction> ins;

    /**@brief symbol table for locations of variables*/
    public SymbolTable<Descriptor> symbol;
    
    public RegisterTable<LocReg> registers;

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
    
    ArrayList<Boolean> isLoop;

    public ControlflowContext() {
        stringLiterals = new HashMap<String,Long>();
        ins = new ArrayList<Instruction>();
        symbol = new SymbolTable<Descriptor>();
        registers = new RegisterTable<LocReg>();
        loopScope = new Stack<Branch>();
        symbol.incScope();
        registers.incScope();
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
    
    public boolean putRegister(String name, LocReg reg){
        return registers.put(name, reg);
    }

    /**
     * @param name
     * @return first Descriptor in the deepest scope.
     * null if symbol does not exist.
     */
    public Descriptor findSymbol(String name){
        return symbol.lookup(name);
    }
    
    public LocReg findRegister(String name){
        return registers.lookup(name);
    }

    public void addIns(Instruction ii){
        ins.add(ii);
    }

    public void addIns(List<Instruction>ll){
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
        isLoop = new ArrayList<Boolean>();
        isLoop.add(false);
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
    public void incScope(Boolean isLoop){
        symbol.incScope();
        registers.incScope();
        localVarSize.add(0L);
        this.isLoop.add(isLoop);
    }

    public void decScopeWithSideEffects(){
        symbol.decScope();
        registers.decScope();
        int idx = localVarSize.size()-1;
        long locals = localVarSize.get(idx);
        localVarSize.remove(idx);
        if(totalLocalSize>maxLocalSize){
            maxLocalSize = totalLocalSize;
        }
        totalLocalSize -= locals;
        isLoop.remove(isLoop.size() - 1);
    }
    
    public Instruction decScopeIdempotent(){
        long locals = localVarSize.get(localVarSize.size() - 1);
        Instruction instr = new Instruction("addq", new LocLiteral(locals), new LocReg(Regs.RSP));
        return instr;
        
    }
    
    public List<Instruction> decScopeToLoop() {
        List<Instruction> dealloc = new ArrayList<Instruction>();
        int i = isLoop.size() - 1;
        boolean done = false;
        while (!done) {
            done = isLoop.get(i);
            long locals = localVarSize.get(i);
            Instruction instr = new Instruction("addq", new LocLiteral(locals), new LocReg(Regs.RSP));
            dealloc.add(instr);
            i--;
        }
        return dealloc;
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
        clearJumps();
        //header
        //global variables
        boolean firstField = true;
        //get the global table.
        HashMap<String, Descriptor> globals = symbol.getTable(0);
        for(Descriptor d: globals.values()){
            IR_Node node = d.getIR();
            if(! (node instanceof IR_FieldDecl)){
                continue;
            }
            IR_FieldDecl decl = (IR_FieldDecl)node;
            String name = decl.getName();
            ps.println("\t.global\t" + name);
            if(firstField){
                ps.println("\t.bss");
                firstField = false;
            }
            ps.println("\t.align\t" + CodegenConst.ALIGN_SIZE);
            ps.println("\t.type\t" + name +", @object");
            //length in bytes
            long len = CodegenConst.INT_SIZE;
            IR_IntLiteral ir_len = decl.getLength();
            if(ir_len!=null){
                len = CodegenConst.INT_SIZE * ir_len.getValue();
            }
            ps.println("\t.size\t" + name +", "+len);
            ps.println(name+":");
            ps.println("\t.zero\t"+len);    
        }
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

        //if needs array bound checking
        if(arrayBoundLabel != null){
            ins.add(Instruction.labelInstruction(arrayBoundLabel));
            ins.add(new Instruction("movq", 
                    new LocLiteral(CodegenConst.ERR_ARRAY_BOUND), new LocReg(Regs.RDI)));
            ins.add(new Instruction("call", new LocLabel("exit")));
        }
        
        
        instructionEfficiencyHack(ins);
        
        for(int ii = 0;ii<ins.size();ii++){
            ps.println(ins.get(ii));
        }
    }
    
    /**
     * Directly hack the assembly to reduce number of instructions.
     * -DeJuan
     * 
     * @param ins : ArrayList<Instruction> of all instructions.
     */
    private void instructionEfficiencyHack(ArrayList<Instruction> ins){
    	//Get rid of add 0 Location.
    	//Also do (delete adjacent push pop) instructions.
    	//We'll also do combine push 1 pop 2 instructions into movq.
    	//What we're looking for: push(loc1) pop(loc1) or push(loc1) pop(loc2)
    	Set<Instruction> instructionsToDelete = new HashSet<Instruction>();
    	Iterator<Instruction> scanner = ins.iterator();
    	while(scanner.hasNext()){
    		Instruction currentInstruction = scanner.next();
    		String currentIns = currentInstruction.cmd;
    		if(currentIns == null){continue;}
    		if(!currentIns.startsWith("ad") && !currentIns.startsWith("su")){continue;}
    		if(currentInstruction.args.get(0).equals(new LocLiteral(0))){scanner.remove();}
    	}
    	
    	instructionsToDelete = new HashSet<Instruction>();
    	for(int ii = 0; ii < ins.size()-2; ii++){
    		Instruction currentInstruction = ins.get(ii);
    		String currentIns = currentInstruction.cmd;
    		if(currentIns == null || !currentIns.startsWith("j")){continue;}
    		Instruction nextInstruction = ins.get(ii+1);
    		if(nextInstruction.label.equals("")){continue;}
    		if(((LocLabel)currentInstruction.args.get(0)).label.equals(nextInstruction.label)){
    			instructionsToDelete.add(currentInstruction);
    			ii++;
    		}
    	}
    	
    	for(Instruction unneededJump : instructionsToDelete){
    		ins.remove(unneededJump);
    	}
    	
    	instructionsToDelete = new HashSet<Instruction>();
    	LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
    	for(int ii = 0; ii < ins.size()-2; ii++){
    		Instruction currentInstruction = ins.get(ii);
    		String currentIns = currentInstruction.cmd;
    		if(currentIns == null || !currentIns.equals("movq")){continue;}
    		Instruction nextInstruction = ins.get(ii+1);
    		String nextIns = nextInstruction.cmd;
    		if(nextIns == null || !nextIns.equals("movq")){continue;}
    		//have proven both are move instructions.
    		LocationMem locationBA = currentInstruction.args.get(1);
    		LocationMem locationBB = nextInstruction.args.get(0);
    		if(!locationBA.equals(locationBB)){continue;}
    		if(!locationBA.equals(r10) && !locationBA.equals(r11)){continue;}
    		ins.set(ii, new Instruction("movq", currentInstruction.args.get(0), nextInstruction.args.get(1)));
    		instructionsToDelete.add(nextInstruction);
    		ii++;
    	}
    		
    	for (Instruction redundantMovement : instructionsToDelete){
    		ins.remove(redundantMovement);
    	}
    	
    	instructionsToDelete = new HashSet<Instruction>();
    	for(int ii = 0; ii < ins.size()-2; ii++){
    		Instruction currentInstruction = ins.get(ii);
    		String currentIns = currentInstruction.cmd;
    		if(currentIns == null || instructionsToDelete.contains(currentInstruction)){continue;}
    		Instruction nextInstruction = ins.get(ii+1);
    		String nextIns = nextInstruction.cmd;
    		if(nextIns == null || instructionsToDelete.contains(nextInstruction)){continue;}
    		if(currentIns.startsWith("pu")){ //efficiency hack; check pu instead of full push
    			if(!nextIns.startsWith("po")){ //same as above, po vs pop
    				continue;
    			}
    		}
    		else if(currentIns.startsWith("po")){
    			if(!nextIns.startsWith("pu")){
    				continue;
    			}
    		}
    		else{continue;}
    		//By this point, we've proved these are push and pop instructions,
    		//so check that they push and pop the same location. 
    		LocationMem loc1 = currentInstruction.args.get(0);
    		LocationMem loc2 = nextInstruction.args.get(0);
    		if(loc1.equals(loc2)){
    			instructionsToDelete.add(currentInstruction);
    			instructionsToDelete.add(nextInstruction);
    			//infinite search horizon logic
    			int leftBound = ii - 1;
    			int rightBound = ii+2;
    			if(leftBound < 0 || rightBound >= ins.size()){continue;}
    			while(true){
    				boolean needToBreak = false;
    				Instruction leftInst = ins.get(leftBound); //left instruction
    					while(instructionsToDelete.contains(leftInst)){
    						leftBound-=1;
    						if(leftBound < 0){
    							needToBreak = true; 
    							break;
    						}
    						leftInst = ins.get(leftBound);
    					}
    				if(needToBreak){break;}
    				String leftCmd = leftInst.cmd; //left command
    				if(leftCmd == null){break;}
    				Instruction rightInst = ins.get(rightBound);
    					while(instructionsToDelete.contains(rightInst)){
    						rightBound+=1;
    						if(rightBound >= ins.size()){
    							needToBreak = true;
    							break;
    						}
    						rightInst = ins.get(rightBound);
    					}
    				if(needToBreak){break;}
    				String rightCmd = rightInst.cmd;
    				if(rightCmd == null){break;}
    				if(leftCmd.startsWith("pu")){ 
    	    			if(!rightCmd.startsWith("po")){ break;}
    	    		}
    	    		else if(leftCmd.startsWith("po")){
    	    			if(!rightCmd.startsWith("pu")){break;}
    	    		}
    	    		else{break;}
    				LocationMem locL = leftInst.args.get(0);
    	    		LocationMem locR = rightInst.args.get(0);
    	    		if(locL.equals(locR)){
    	    			instructionsToDelete.add(leftInst);
    	    			instructionsToDelete.add(rightInst);
    	    			leftBound-=1;
    	    			rightBound+=1;
    	    			if(leftBound < 0 || rightBound >= ins.size()){break;}
    	    		}
    	    		else{ break;}
    			}
    		}
    		else{continue;}
    	}

    	for (Instruction redundantMovement : instructionsToDelete){
    		ins.remove(redundantMovement);
    	}
    	
    	instructionsToDelete = new HashSet<Instruction>();
    	
    	//Now do combination logic: push x pop y --> moveq x y
    	for(int ii = 0; ii < ins.size()-2; ii++){
    		Instruction currentInstruction = ins.get(ii);
    		String currentIns = currentInstruction.cmd;
    		if(currentIns == null || instructionsToDelete.contains(currentInstruction)){continue;}
    		Instruction nextInstruction = ins.get(ii+1);
    		String nextIns = nextInstruction.cmd;
    		if(nextIns == null){continue;}
    		if(currentIns.startsWith("pu")){
    			if(!nextIns.startsWith("po")){ //It's gotta be pupo!
    				continue;
    			}
    		}
    		else{continue;}
    		//By this point, we've proved these are push and pop instructions,
    		//so check that they push and pop different locations! 
    		LocationMem loc1 = currentInstruction.args.get(0);
    		LocationMem loc2 = nextInstruction.args.get(0);
    		if(!(loc1.equals(loc2))){
    			ins.set(ii, new Instruction("movq", loc1, loc2));
    			instructionsToDelete.add(nextInstruction);
    			//search infinitely in both directions for hidden replacements if possible.
    			int leftBound = ii - 1;
    			int rightBound = ii+2;
    			if(leftBound < 0 || rightBound > ins.size()){
    				continue;
    			}
    			while(true){
    				boolean needToBreak = false;
    				Instruction leftInst = ins.get(leftBound); //left instruction
    					while(instructionsToDelete.contains(leftInst)){
    						leftBound-=1;
    						if(leftBound < 0){
    							needToBreak = true; 
    							break;
    						}
    						leftInst = ins.get(leftBound);
    					}
    				if(needToBreak){break;}
    				String leftCmd = leftInst.cmd; //left command
    				if(leftCmd == null){break;}
    				Instruction rightInst = ins.get(rightBound);
    					while(instructionsToDelete.contains(rightInst)){
    						rightBound+=1;
    						if(rightBound >= ins.size()){
    							needToBreak = true;
    							break;
    						}
    						rightInst = ins.get(rightBound);
    					}
    				if(needToBreak){break;}
    				String rightCmd = rightInst.cmd;
    				if(rightCmd == null){break;}
    				if(leftCmd.startsWith("pu")){ 
    	    			if(!rightCmd.startsWith("po")){ break;}
    	    		}
    	    		else{ break;}
    				LocationMem locL = leftInst.args.get(0);
    	    		LocationMem locR = rightInst.args.get(0);
    	    		if(!locL.equals(locR)){
    	    			ins.set(leftBound, new Instruction("movq", locL, locR));
    	    			instructionsToDelete.add(rightInst);
    	    			leftBound-=1;
    	    			rightBound+=1;
    	    			if(leftBound < 0 || rightBound >= ins.size()){
    	    				break;
    	    			}
    	    		}
    	    		else{break;}
    			}
    		}
    		else{continue;}
    	}
    	
    	for(Instruction unneededPop : instructionsToDelete){
    		ins.remove(unneededPop);
    	}
    	
    	
    }
    
    private void clearJumps() {
        //TODO: Implement
        // will eventually clear out unneeded labels
    }

    public String getArrayBoundLabel(){
        if(arrayBoundLabel == null){
            arrayBoundLabel = genLabel();
        }
        return arrayBoundLabel;
    }

}
