package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.*;
import edu.mit.compilers.ir.IR_Literal.*;
import edu.mit.compilers.codegen.LocationMem;

public class Codegen {
	/**@brief generate code for root node of IR.
	 * 
	 * @param node root node
	 * @param context
	 */
	public static void generateProgram(IR_Node root, CodegenContext context){
		IR_Seq seq = (IR_Seq)root;
		List<IR_Node> stmt = seq.getStatements();
		for (int ii =0 ;ii<stmt.size(); ii++){
			IR_Node n = stmt.get(ii);
			if(n.getType()==Type.METHOD){
				generateMethodDecl(n, context);
			}else if(n.getType()==Type.CALLOUT){
				generateCallout(n, context);
			}else if(n instanceof IR_FieldDecl){
				generateFieldDeclGlobal((IR_FieldDecl)n, context);
			}
		}
	}

	public static void generateCallout(IR_Node node, CodegenContext context){
		//not necessary to add this to symbol table since semantic check is done. For integrity.
		IR_MethodDecl decl = (IR_MethodDecl)node;
		String name = decl.name;
		Descriptor d = new Descriptor(node);
		context.putSymbol(name, d);
	}
	
	/**brief put method argument names and their locations into symbol table.
	 * call generate block to get instructions for the block.
	 * Allocate rsp according to accumulated locvarSize.
	 * @param node
	 * @param context
	 */
	public static void generateMethodDecl(IR_Node node, CodegenContext context){
		IR_MethodDecl decl = (IR_MethodDecl)node;
		String name = decl.name;
		Descriptor d = new Descriptor(node);
		context.putSymbol(name, d);
		context.enterFun();
		context.incScope();

		Instruction tmpIns;
		context.addIns(new Instruction(".type",new LocLabel(name),new LocLabel("@function")));
		context.addIns(new Instruction(".text"));
		context.addIns(new Instruction(".global", new LocLabel(name)));
		tmpIns = Instruction.labelInstruction(name);
		context.addIns(tmpIns);
		
		LocReg rbp = new LocReg(Regs.RBP);
		LocReg rsp = new LocReg(Regs.RSP);
		context.addIns(new Instruction("pushq", rbp));		
		context.addIns(new Instruction("movq", rsp, rbp ));
				
		//instructions for potentially saving arguments.
		ArrayList<Instruction> argIns = new ArrayList<Instruction>();
		//save register parameters to stack
		for(int ii = 0; ii<decl.args.size(); ii++){
			IR_FieldDecl a = decl.args.get(ii);
			Descriptor argd = new Descriptor(a);
			context.putSymbol(a.getName(), argd);
			//TODO
			LocationMem argSrc = argLoc(ii);
			LocationMem argDst = argSrc;
			if(ii<CodegenConst.N_REG_ARG){
				//save register arguments on the stack
				List<Instruction> pushIns = context.push(argSrc);
				argIns.addAll(pushIns);
				argDst = context.getRsp();
			}
			context.allocLocal(CodegenConst.INT_SIZE);
			argd.setLocation(argDst);
		}
		context.addIns(argIns);
		
		//generateBlock accumulates static local stack size required. 
		List<Instruction> blockIns = generateBlock(decl.body, context);

		//instructions for entering a function.
		LocLiteral loc= new LocLiteral(context.maxLocalSize);
		context.addIns(new Instruction("subq", loc, rsp));

		//write instructions for function body.
		context.addIns(blockIns);
		context.decScope();

		//prevent fall through for void functions
		List <IR_Node> st = decl.body.getStatements();
		boolean needReturn = false;
		if(st.size()>0){
			IR_Node lastStatement = st.get(st.size()-1);
			if(!(lastStatement instanceof IR_Return)){
				needReturn = true;
			}
		}else{
			needReturn = true;
		}
		if(needReturn){
			context.addIns(new Instruction("leave"));
			context.addIns(new Instruction("ret"));
		}
	}
	
	public static void generateFieldDeclGlobal(IR_FieldDecl decl, CodegenContext context){
		Descriptor d = new Descriptor(decl);
		d.setLocation(new LocLabel(decl.getName()));
		context.putSymbol(decl.getName(), d);
	}
	
	public static List<Instruction> generateFieldDecl(IR_FieldDecl decl, CodegenContext context){
		ArrayList<Instruction> ins = new ArrayList<Instruction>();
		String name = decl.getName();
		Descriptor d = new Descriptor(decl);
		Type type = decl.getType();
		long size = CodegenConst.INT_SIZE;
		switch(type){
		case INTARR:
		case BOOLARR:
			size = decl.getLength().getValue() * CodegenConst.INT_SIZE;
			break;
		default:
			break;
		}
		LocStack loc = context.allocLocal(size);
		d.setLocation(loc);
		context.putSymbol(name, d);
		LocReg rsp = new LocReg(Regs.RSP);
		if(type == Type.INTARR || type == type.BOOLARR){
			for (long location = rsp.getValue(); location >= rsp.getValue()-size; location= location-CodegenConst.INT_SIZE){
				ins.add(new Instruction("mov", new LocLiteral(0), new LocStack(location)));
			}
		}
		else{
			ins.add(new Instruction("mov", new LocLiteral(0), loc));
		}
		return ins;
	}
		
	/**@brief expression nodes should return location of the result
	 * 
	 * @param expr
	 * @param context
	 * @return
	 */
	public static List<Instruction> generateExpr(IR_Node expr, CodegenContext context){
		List<Instruction> ins = new ArrayList<Instruction>();
		
		if (expr instanceof IR_Call) {
			ins = generateCall((IR_Call) expr, context);
			LocReg rax = new LocReg(Regs.RAX);
			ins.addAll(context.push(rax));
			return ins;
		}
		
		if (expr instanceof IR_ArithOp){
			ins = generateArithExpr((IR_ArithOp) expr, context);
			return ins;
		}
		
		else if(expr instanceof IR_CompareOp){
			IR_CompareOp compare = (IR_CompareOp) expr;
			ins = generateCompareOp(compare, context);
			return ins;
		}
		
		else if ((expr instanceof IR_CondOp) || (expr instanceof IR_Not)) {
			ins = generateCondOp(expr, context);
			return ins;
		}
		
		else if (expr instanceof IR_EqOp){
			IR_EqOp eq= (IR_EqOp)expr; //There's no real difference between CondOp and EqOp except operators
			IR_CompareOp cmp = new IR_CompareOp(eq.getLeft(), eq.getRight(), eq.getOp());
			ins = generateCompareOp(cmp, context); //I've combined all the comparison operations in my one method; it can take and resolve EqOp operators.
			return ins;
		}
		
		else if (expr instanceof IR_Negate) {
			IR_Negate negation = (IR_Negate)expr;
			LocReg r10 = new LocReg(Regs.R10);
			ins = generateExpr(negation.getExpr(), context);
			ins.addAll(context.pop(r10)); //Get whatever that expr was off stack
			ins.add(new Instruction("negq", r10)); //negate it
			ins.addAll(context.push(r10)); //push it back to stack
			return ins;
		}else if (expr instanceof IR_Ternary){
			IR_Ternary ternary = (IR_Ternary)expr;
			ins = generateTernaryOp(ternary, context);
			return ins;
		}
		
		if(expr instanceof IR_Var){
			IR_Var var = (IR_Var)expr;
			ins=generateVarExpr(var, context);
			return ins;
		}

		else if (expr instanceof IR_Literal) {
			IR_Literal literal = (IR_Literal) expr;
			ins = generateLiteral(literal, context);
			return ins;
		}
		
		else {
			System.err.println("Unexpected Node type passed to generateExpr: " + expr.getClass().getSimpleName());
			System.err.println("The node passed in was of type " + expr.getType().toString());
		}
		ins = null; 
		
		
	
		return ins;
	}
	
	public static List<Instruction> generateLiteral(IR_Literal literal, CodegenContext context) {
		List<Instruction> ins = null;
		
		if (literal instanceof IR_IntLiteral) {
			IR_IntLiteral int_literal = (IR_IntLiteral) literal;
			if(int_literal.getValue()>Integer.MAX_VALUE || 
					int_literal.getValue()<Integer.MIN_VALUE ){
				LocReg rax = new LocReg(Regs.RAX);
				ins = new ArrayList<Instruction>(2);
				ins.add(new Instruction("movabsq",new LocLiteral(int_literal.getValue()),rax));
				ins.addAll(context.push(rax));
				
			}else{
				ins = context.push(new LocLiteral(int_literal.getValue()));
			}
		} 
		else if (literal instanceof IR_BoolLiteral) {
			IR_BoolLiteral bool_literal = (IR_BoolLiteral) literal;
			if (bool_literal.getValue()) {
				ins = context.push(new LocLiteral(CodegenConst.BOOL_TRUE));
			} else {
				ins = context.push(new LocLiteral(CodegenConst.BOOL_FALSE));
			}
		}
		return ins;
	}

	private static List<Instruction> generateTernaryOp(IR_Ternary ternary, CodegenContext context) {		
		List<Instruction> ins = new ArrayList<Instruction>();
		LocReg r10 = new LocReg(Regs.R10);
		String labelForFalse = context.genLabel();
		String labelForDone = context.genLabel();
		List<Instruction> trueInstructs = generateExpr(ternary.getTrueExpr(), context);
		List<Instruction> falseInstructs = generateExpr(ternary.getFalseExpr(), context);
		
		ins.addAll(generateExpr(ternary.getCondition(), context)); //Get result of conditional onto the stack by resolving it. 
		ins.addAll(context.pop(r10)); //pop result into r10.
		ins.add(new Instruction("cmp", new LocLiteral(1L), r10)); //Compare r10 against truth
		ins.add(new Instruction("jne", new LocLabel(labelForFalse))); //If result isn't equal, r10 is 0, meaning we take the false branch.
		ins.addAll(trueInstructs); //If we don't jump, resolve the true branch 
		ins.add(new Instruction("jmp", new LocLabel(labelForDone))); //jump to being done
		ins.add(Instruction.labelInstruction(labelForFalse)); //If we jump, we jump here.
		ins.addAll(falseInstructs); //Resolve the false branch. 
		ins.add(Instruction.labelInstruction(labelForDone)); //This is where we'd jump to if we resolved the true version, which skips over the whole false branch. 
		return ins; //We're done, return the list.
	}

	public static LocationMem generateVarLoc(IR_Var var, CodegenContext context, List<Instruction> ins) {
		Descriptor d = context.findSymbol(var.getName());
		switch (d.getIR().getType()) {
		case INT:
		case BOOL:
			return d.getLocation();
		case INTARR:
		case BOOLARR:
			IR_Node index = var.getIndex();
			IR_FieldDecl decl = (IR_FieldDecl)d.getIR();
			LocArray loc_array = null;
			long len = decl.getLength().getValue();
			if (index instanceof IR_IntLiteral) {
				IR_IntLiteral index_int = (IR_IntLiteral)var.getIndex();
				loc_array = new LocArray(d.getLocation(), 
						new LocLiteral(index_int.getValue()), CodegenConst.INT_SIZE);
				
				if(index_int.getValue() >= len){
					//statically throw error
					ins.add(new Instruction("movq", 
							new LocLiteral(CodegenConst.ERR_ARRAY_BOUND), new LocReg(Regs.RDI)));
					ins.add(new Instruction("call", new LocLabel("exit")));
				}
				
			} else {
				// evaluate index and push index location to stack
				ins.addAll(generateExpr(index, context));
				//must not use r11 or r10 here since in assign, they may be used
				LocReg rax = new LocReg(Regs.RAX);
				// saves offset at R11
				ins.add(new Instruction("popq", rax));
				loc_array = new LocArray(d.getLocation(), rax, CodegenConst.INT_SIZE);
				ins.add(new Instruction("cmpq", new LocLiteral(len), rax));
				ins.add(new Instruction("jge", new LocLabel(context.getArrayBoundLabel())));
			}
			return loc_array;
		default:
			return null;
		}
	}		
	
	public static List<Instruction> generateVarExpr(IR_Var var, CodegenContext context) {
		List<Instruction> ins = new ArrayList<Instruction>();
		LocationMem loc = generateVarLoc(var, context, ins);
		ins.addAll(context.push(loc));
		return ins;
	}
	
	public static List<Instruction> generateArithExpr(IR_ArithOp arith, CodegenContext context){
		List<Instruction> ins = new ArrayList<Instruction>();
		Ops op = arith.getOp();
		IR_Node left = arith.getLeft();
		IR_Node right = arith.getRight();
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		ins.addAll(generateExpr(left,context));
		ins.addAll(generateExpr(right, context));
		ins.addAll(context.pop(r11));
		ins.addAll(context.pop(r10));
		
		if(!(op == Ops.DIVIDE || op==Ops.MOD)){
			String cmd = "";
			switch (op){ //PLUS, MINUS, TIMES
			case PLUS:
				cmd = "addq";
				break;
			case MINUS:
				cmd = "subq";
				break;
			case TIMES:
				cmd = "imulq";
				break;
			default:
				break;
			}
			ins.add(new Instruction(cmd, r11, r10));
			ins.addAll(context.push(r10));
		}else{
			LocReg rdx = new LocReg(Regs.RDX);
			LocReg rax = new LocReg(Regs.RAX);
			ins.addAll(context.push(rdx));
			ins.add(new Instruction("movq", r10, rax));
			ins.add(new Instruction("cqto"));
			ins.add(new Instruction("idivq", r11));//Divide rdx:rax by r11 contents - i.e divide lhs by rhs.
			if(op==Ops.MOD){
				ins.add(new Instruction("movq", rdx, rax));
			}
			ins.addAll(context.pop(rdx));
			ins.addAll(context.push(rax));			
		}
		return ins;
	}
	
	private static List<Instruction> generateCompareOp(IR_CompareOp compare, CodegenContext context) {
		List<Instruction> ins = new ArrayList<Instruction>();
		Ops op = compare.getOp();
		IR_Node left = compare.getLeft();
		IR_Node right = compare.getRight();
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		ins.addAll(generateExpr(left,context));
		ins.addAll(generateExpr(right, context));
		ins.addAll(context.pop(r11));
		ins.addAll(context.pop(r10));
		String cmd = "";
		switch(op){
		case GT:
			cmd = "setg";
			break;
		case GTE:
			cmd = "setge";
			break;
		case EQUALS:
			cmd = "sete";
			break;
		case NOT_EQUALS:
			cmd = "setne";
			break;
		case LT:
			cmd = "setl";
			break;
		case LTE:
			cmd = "setle";
			break;
		default:
			return null; //Irrecoverable, can't compare with an incorrect op
		}
		ins.add(new Instruction("cmpq", r11,r10));
		LocReg al = new LocReg(Regs.AL);
		ins.add(new Instruction(cmd, al));
		//zero bit extension
		ins.add(new Instruction("movzbq", al, r10));
		ins.addAll(context.push(r10));
		return ins;
	}
			
	private static List<Instruction> generateCondOp(IR_Node conditional, CodegenContext context) {
		LocLiteral zero = new LocLiteral(0);
		LocLiteral one = new LocLiteral(1);

		String tLabel = context.genLabel();
		String fLabel = context.genLabel();

		CFGNode.CFGLabel t = new CFGNode.CFGLabel(tLabel);
		CFGNode.CFGLabel f = new CFGNode.CFGLabel(fLabel);
		String endLabel = context.genLabel();

		CFGNode cfg = CFGNode.shortCircuit(conditional, t, f);
		
		List<Instruction> ins = cfg.codegen(context);

		ins.add(Instruction.labelInstruction(tLabel));
		ins.add(new Instruction("pushq", one));
		LocLabel end = new LocLabel(endLabel);
		ins.add(new Instruction("jmp",  end));
		ins.add(Instruction.labelInstruction(fLabel));
		ins.add(new Instruction("pushq", zero));

		ins.add(Instruction.labelInstruction(endLabel));
		ins.add(new Instruction("nop"));
		return ins;
	}
	
	public static List<Instruction> generateBlock(IR_Seq block, CodegenContext context){
		ArrayList<Instruction> ins = new ArrayList<Instruction>();
		List<IR_Node> stmt = block.getStatements();
		context.incScope();
		for(int ii = 0;ii<stmt.size(); ii++){
			IR_Node st = stmt.get(ii);
			List<Instruction> stIns =null;
			if (st instanceof IR_FieldDecl) {
				IR_FieldDecl decl =(IR_FieldDecl) st;
				stIns = generateFieldDecl(decl,context);
			} else if (st instanceof IR_Call){
				IR_Call call = (IR_Call)st;
				stIns = generateCall(call,context);
			} else if (st instanceof IR_Assign) {
				IR_Assign assign = (IR_Assign) st;
				stIns = generateAssign(assign,context);				
 		    // TODO: Generate logic for these control flow statements
			} else if (st instanceof IR_If) {
				IR_If if_st = (IR_If) st;
				stIns = generateIf(if_st, context);
			} else if (st instanceof IR_For) {
   			        IR_For for_st = (IR_For) st;
				stIns = generateFor(for_st, context);
                        } else if (st instanceof IR_While) {
				IR_While while_st = (IR_While) st;
				stIns = generateWhile(while_st, context);
			} else if (st instanceof IR_Return) {
   			        IR_Return return_st = (IR_Return) st;
				stIns = generateReturn(return_st, context);
			} else if (st instanceof IR_Break) {
   			        IR_Break break_st = (IR_Break) st;
				stIns = generateBreak(break_st, context);
			} else if (st instanceof IR_Continue) {
   			        IR_Continue continue_st = (IR_Continue) st;
				stIns = generateContinue(continue_st, context);
			} else {
				System.err.println("Should not reach here");
 			}
			if (stIns != null) {
				ins.addAll(stIns);
			}
		}
		context.decScope();
		return ins;
	}
	
	static List<Instruction> generateAssign(IR_Assign assign, CodegenContext context) {
		ArrayList<Instruction> ins = new ArrayList<Instruction>();
		Ops op = assign.getOp();
		IR_Var lhs = assign.getLhs();
		IR_Node rhs = assign.getRhs();
		
		ins.addAll(generateExpr(rhs,context));
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		LocationMem dst= generateVarLoc(lhs, context, ins);
		ins.addAll(context.pop(r10));
		if(op != Ops.ASSIGN){
			String cmd = null;
			switch(op){
			case ASSIGN_PLUS:
				cmd = "addq";
				break;
			case ASSIGN_MINUS:
				cmd = "subq";
				break;
			default:
				break;
			}
			ins.add(new Instruction("movq", dst, r11));
			ins.add(new Instruction(cmd, r10, r11));
			ins.add(new Instruction("movq", r11, dst));
		}else{
			ins.add(new Instruction("movq", r10, dst));
		}
		return ins;
	}
	
	public static List<Instruction> generateCall(IR_Call call, CodegenContext context) {
		ArrayList<Instruction> ins = new ArrayList<Instruction>();
		List<IR_Node> args = call.getArgs();
		for(int ii = args.size()-1; ii>=0; ii--){
			IR_Node arg = args.get(ii);
			//source location of argument
			LocationMem argSrc=null;
			if(arg instanceof IR_Literal.IR_StringLiteral){
				IR_Literal.IR_StringLiteral sl=(IR_Literal.IR_StringLiteral)arg;
				String ss = sl.getValue();
				Long idx = context.stringLiterals.get(ss);
				if(idx==null){
					idx = (long) context.stringLiterals.size();
					context.stringLiterals.put(ss, idx);
				}
				argSrc = new LocLabel("$"+CodegenContext.StringLiteralLoc(idx));
			}else{
				List<Instruction> exprIns = generateExpr(arg, context);
				ins.addAll(exprIns);
				//load argument to temporary register.
				argSrc = new LocReg(Regs.R10);
				ins.addAll(context.pop(argSrc));
			}
			List<Instruction> argIns = setCallArg(argSrc,ii,context);
			ins.addAll(argIns);
		}
		IR_MethodDecl decl = call.getDecl();
		if(decl.getType() == Type.CALLOUT){
			//# of floating point registers is stored in rax
			//need to zero it for callouts.
			ins.add(new Instruction("movq", new LocLiteral(0),  new LocReg(Regs.RAX)));			
		}
		ins.add(new Instruction("call ", new LocLabel(call.getName()) ));
		
		//pop all arguments on the stack
		if(args.size()>CodegenConst.N_REG_ARG){
			long stackArgSize = CodegenConst.INT_SIZE * (args.size()-CodegenConst.N_REG_ARG);
			ins.add(new Instruction("addq", new LocLiteral(stackArgSize), new LocReg(Regs.RSP)));
		}
		return ins;
	}
	
	public static List<Instruction> generateIf(IR_If if_st, CodegenContext context) {
		List<Instruction> stIns = new ArrayList<Instruction>();
		String labelForTrue = context.genLabel();
		String labelForEnd = context.genLabel();
		List<Instruction> trueInstructs = generateBlock(if_st.getTrueBlock(), context);
		List<Instruction> falseInstructs;
	    if (if_st.getFalseBlock() == null) {
	      falseInstructs = new ArrayList<Instruction>();
	    } else {
	      falseInstructs = generateBlock(if_st.getFalseBlock(), context);
	    }
		stIns.addAll(generateExpr(if_st.getExpr(), context));
		LocReg r10 = new LocReg(Regs.R10);
		stIns.addAll(context.pop(r10));
		stIns.add(new Instruction("cmp", new LocLiteral(1L), r10));
		stIns.add(new Instruction("je", new LocLabel(labelForTrue)));
		stIns.addAll(falseInstructs);
		stIns.add(new Instruction("jmp", new LocLabel(labelForEnd)));
		stIns.add(Instruction.labelInstruction(labelForTrue));
		stIns.addAll(trueInstructs);
		stIns.add(Instruction.labelInstruction(labelForEnd));
		return stIns;
	}
	
	
	public static List<Instruction> generateFor(IR_For for_st, CodegenContext context) {
		List<Instruction> stIns = new ArrayList<Instruction>();
		String labelForStart = context.genLabel();
		String labelForEnd = context.genLabel();
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		LocationMem loopVar = generateVarLoc(for_st.getVar(), context, stIns);
        stIns.addAll(generateExpr(for_st.getStart(), context));
        stIns.addAll(context.pop(r10));
        stIns.add(new Instruction("movq", r10, loopVar));
        
        stIns.addAll(generateExpr(for_st.getEnd(), context));
        // Start of loop
		context.enterLoop(labelForStart, labelForEnd);
		stIns.add(Instruction.labelInstruction(labelForStart));
		stIns.addAll(generateExpr(for_st.getVar(), context));
		stIns.addAll(context.pop(r10));  // loop var
		stIns.addAll(context.pop(r11));  // end
		stIns.add(new Instruction("cmp", r10, r11));
		stIns.add(new Instruction("jle", new LocLabel(labelForEnd)));
		stIns.addAll(context.push(r11));
		stIns.addAll(generateBlock(for_st.getBlock(), context));
		// TODO: Is this legal since loopVar is a memory address?
		stIns.add(new Instruction("add", new LocLiteral(1L), loopVar));
		stIns.add(new Instruction("jmp", new LocLabel(labelForStart)));
		
		stIns.add(Instruction.labelInstruction(labelForEnd));
//		stIns.addAll(context.pop(r11));
		context.exitLoop();
		return stIns;
	}
	
	public static List<Instruction> generateWhile(IR_While while_st, CodegenContext context) {
		List<Instruction> stIns = new ArrayList<Instruction>();
		String labelForStart = context.genLabel();
		String labelForEnd = context.genLabel();
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		
		boolean hasBound = (while_st.getMaxLoops() != null); 
		if (hasBound) {
		    stIns.addAll(generateExpr(while_st.getMaxLoops(), context));
		    stIns.addAll(context.push(new LocLiteral(0L)));
		}

		// Start loop here
		context.enterLoop(labelForStart, labelForEnd);
		stIns.add(Instruction.labelInstruction(labelForStart));
		stIns.addAll(generateExpr(while_st.getExpr(), context));
		stIns.addAll(context.pop(r10));
		stIns.add(new Instruction("cmp", new LocLiteral(0L), r10));
		stIns.add(new Instruction("je", new LocLabel(labelForEnd)));
		
		if(hasBound){
			stIns.addAll(context.pop(r10));  // loops count
			stIns.addAll(context.pop(r11));  // max loops
			stIns.add(new Instruction("cmp", r10, r11));
			stIns.add(new Instruction("je", new LocLabel(labelForEnd)));
			stIns.add(new Instruction("add", new LocLiteral(1L), r10));  // increment loop count
			stIns.addAll(context.push(r11));
			stIns.addAll(context.push(r10));
		}
		
		stIns.addAll(generateBlock(while_st.getBlock(), context));
		stIns.add(new Instruction("jmp", new LocLabel(labelForStart)));
		
		// End loop here
		stIns.add(Instruction.labelInstruction(labelForEnd));
		context.exitLoop();
		
		return stIns;
	}
	
	public static List<Instruction> generateBreak(IR_Break break_st, CodegenContext context) {
		List<Instruction> stIns = new ArrayList<Instruction>();
		stIns.add(new Instruction("jmp", new LocLabel(context.getInnermostEnd())));
		return stIns;
	}

	public static List<Instruction> generateContinue(IR_Continue continue_st, CodegenContext context) {
		List<Instruction> stIns = new ArrayList<Instruction>();
		stIns.add(new Instruction("jmp", new LocLabel(context.getInnermostStart())));
		return stIns;
	}
	
	public static List<Instruction> generateReturn(IR_Return return_st, CodegenContext context) {
		List<Instruction> stIns = new ArrayList<Instruction>();
		IR_Node expr = return_st.getExpr();
		// We only have instructions to add if return value is not void.
		if (expr != null) {
			LocReg r10 = new LocReg(Regs.R10);
			stIns = generateExpr(expr, context);
			stIns.add(new Instruction("pop", r10));
			stIns.add(new Instruction("mov", r10, new LocReg(Regs.RAX)));
		}
		stIns.add(new Instruction("leave"));
		stIns.add(new Instruction("ret"));
		return stIns;
	}
	
	
	/**@brief registers used for function arguments.
	 * 
	 */
	private static final Regs regArg[] = {Regs.RDI, Regs.RSI, Regs.RDX,
			Regs.RCX, Regs.R8, Regs.R9};

	private static LocationMem argLoc(int idx){
		if(idx<CodegenConst.N_REG_ARG){
			return new LocReg(regArg[idx]);
		}
		long offset = 16+(idx-CodegenConst.N_REG_ARG)*CodegenConst.INT_SIZE;
		return new LocStack(offset);
	}
	
	/**@brief set the ith method call argument
	 * 
	 * @param argSrc
	 * @param idx
	 * @return
	 */
	private static List<Instruction> setCallArg(LocationMem argSrc, int idx, CodegenContext context){
		ArrayList<Instruction> ins=new ArrayList<Instruction>();
		if(idx<CodegenConst.N_REG_ARG){
			LocationMem argDst = argLoc(idx);
			ins.add(new Instruction("movq", argSrc, argDst));
		}else{
			ins.addAll(context.push(argSrc));
		}
		return ins;
	}
}
