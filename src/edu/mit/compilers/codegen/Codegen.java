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
		
		//instructions for potentially saving arguments.
		ArrayList<Instruction> argIns = new ArrayList<Instruction>();
				
		//save register parameters to stack
		for(int ii = 0; ii<decl.args.size(); ii++){
			IR_FieldDecl a = decl.args.get(ii);
			Descriptor argd = new Descriptor(a);
			context.putSymbol(a.getName(), argd);
			
			LocationMem argSrc = argLoc(ii);
			LocationMem argDst = argSrc;
			if(ii<CodegenConst.N_REG_ARG){
				//save register arguments on the stack
				List<Instruction> pushIns = context.push(argSrc);
				argIns.addAll(pushIns);
				argDst = context.getRsp();
			}
			argd.setLocation(argDst);
		}
		
		//generateBlock accumulates static local stack size required. 
		List<Instruction> blockIns = generateBlock(decl.body, context);

		//instructions for entering a function.
		LocLiteral loc= new LocLiteral(context.maxLocalSize);
		Instruction tmpIns;
		context.addIns(new Instruction(".global", new LocLabel(name)));
		tmpIns = Instruction.labelInstruction(name);
		context.addIns(tmpIns);
		tmpIns = new Instruction("enter", loc, new LocLiteral(0));
		context.addIns(tmpIns);
		context.addIns(argIns);
		
		//write instructions for function body.
		context.addIns(blockIns);
		context.addIns(new Instruction("leave"));
		context.addIns(new Instruction("ret"));
		context.decScope();
	}
	
	public static void generateFieldDeclGlobal(IR_FieldDecl decl, CodegenContext context){
		IR_IntLiteral len = decl.getLength();
		long size = CodegenConst.INT_SIZE;
		if(len != null){
			//array
			size = CodegenConst.INT_SIZE * len.getValue();
		}
		context.addIns(new Instruction(".comm "+decl.getName() + ","+size+","+CodegenConst.ALIGN_SIZE));
		Descriptor d = new Descriptor(decl);
		d.setLocation(new LocLabel(decl.getName()));
		context.putSymbol(decl.getName(), d);
	}
	
	public static List<Instruction> generateFieldDecl(IR_FieldDecl decl, CodegenContext context){
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
		return new ArrayList<Instruction>();
	}
		
	/**@brief expression nodes should return location of the result
	 * 
	 * @param expr
	 * @param context
	 * @return
	 */
	public static List<Instruction> generateExpr(IR_Node expr, CodegenContext context){
		List<Instruction> ins = null;
		
		if (expr instanceof IR_Var) {
			IR_Var var = (IR_Var) expr;
			ins = generateVarExpr(var, context);
		} 
		
		else if (expr instanceof IR_Literal) {
			IR_Literal literal = (IR_Literal) expr;
			ins = generateLiteral(literal, context);
		}
	
		return ins;
	}
	
	public static List<Instruction> generateLiteral(IR_Literal literal, CodegenContext context) {
		List<Instruction> ins = null;
		
		if (literal instanceof IR_IntLiteral) {
			IR_IntLiteral int_literal = (IR_IntLiteral) literal;
			ins = context.push(new LocLiteral(int_literal.getValue()));
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

	public static LocationMem generateVarLoc(IR_Var var, CodegenContext context, List<Instruction> ins) {
		Descriptor d = context.findSymbol(var.getName());
		switch (d.getIR().getType()) {
		case INT:
		case BOOL:
			return d.getLocation();
		case INTARR:
		case BOOLARR:
			IR_Node index = var.getIndex();
			LocArray loc_array = null;
			if (index instanceof IR_IntLiteral) {
				IR_IntLiteral index_int = (IR_IntLiteral)var.getIndex();
				loc_array = new LocArray(d.getLocation(), 
						new LocLiteral(index_int.getValue()), CodegenConst.INT_SIZE);
			} else {
				// evaluate index and push index location to stack
				ins.addAll(generateExpr(index, context));
				//must use r11 here since in assign, r10 is used for rhs
				LocReg r11 = new LocReg(Regs.R11);
				// saves offset at R11
				ins.add(new Instruction("pop", r11));
				loc_array = new LocArray(d.getLocation(), r11, CodegenConst.INT_SIZE);
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

	public static List<Instruction> generateBlock(IR_Seq block, CodegenContext context){
		ArrayList<Instruction> ins = new ArrayList<Instruction>();
		List<IR_Node> stmt = block.getStatements();
		context.incScope();
		for(int ii = 0;ii<stmt.size(); ii++){
			IR_Node st = stmt.get(ii);
			List<Instruction> stIns =null;
			if (st instanceof IR_Call){
				IR_Call call = (IR_Call)st;
				stIns = generateCall(call,context);
			}else if(st instanceof IR_FieldDecl){
				IR_FieldDecl decl =(IR_FieldDecl) st;
				stIns = generateFieldDecl(decl,context);
			}else if(st instanceof IR_Assign){
				IR_Assign assign = (IR_Assign) st;
				stIns = generateAssign(assign,context);				
			}
			ins.addAll(stIns);
		}
		context.decScope();
		return ins;
	}
	
	static List<Instruction>generateAssign(IR_Assign assign, CodegenContext context){
		ArrayList<Instruction> ins = new ArrayList<Instruction>();
		Ops op = assign.getOp();
		IR_Var lhs = assign.getLhs();
		IR_Node rhs = assign.getRhs();
		LocationMem dst= generateVarLoc(lhs, context, ins);
		
		ins.addAll(generateExpr(rhs,context));
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		
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
	
	public static List<Instruction>  generateCall(IR_Call call, CodegenContext context ){
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
	/**@brief registers used for function arguments.
	 * 
	 */
	private static final Regs regArg[] = {Regs.RDI, Regs.RSI, Regs.RDX,
			Regs.RCX, Regs.R8, Regs.R9};

	private static LocationMem argLoc(int idx){
		if(idx<CodegenConst.N_REG_ARG){
			return new LocReg(regArg[idx]);
		}
		long offset = 16+(idx-6)*CodegenConst.INT_SIZE;
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
