package edu.mit.compilers.codegen;
import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.*;
import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;
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
		context.incScope();
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
		context.incScope();
		for(int ii = 0; ii<decl.args.size(); ii++){
			IR_FieldDecl a = decl.args.get(ii);
			Descriptor argd = new Descriptor(a);
			context.putSymbol(a.getName(), argd);
			argd.setLocation(argLoc(ii));
		}
		context.localvarSize=0;
		List<Instruction> ins= generateBlock(decl.body, context);
		LocLiteral loc= new LocLiteral(context.localvarSize);
		Instruction ii;
		context.addIns(new Instruction(".global", new LocJump(name)));
		ii = Instruction.labelInstruction(name);
		context.addIns(ii);
		ii = new Instruction("enter", loc, new LocLiteral(0));
		context.addIns(ii);
		context.addIns(ins);
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
	
	public static void generateFieldDecl(IR_FieldDecl decl, CodegenContext context){
		
	}
		
	/**@brief expression nodes should return location of the result
	 * 
	 * @param expr
	 * @param context
	 * @return
	 */
	public static List<Instruction> generateExpr(IR_Node expr, CodegenContext context){
		List<Instruction> ins = null;
		
		if(expr instanceof IR_Var){
			IR_Var var = (IR_Var)expr;
			ins=generateVarExpr(var, context);
		}
		
		return ins;
	}

	public static List<Instruction> generateVarExpr(IR_Var var, CodegenContext context){
		List<Instruction> ins = null;
		Descriptor d = context.findSymbol(var.getName());
		switch (var.getType()) {
		case INT:
			ins = context.push(d.getLocation());
			break;
		case BOOL:
			ins = context.push(d.getLocation());
			break;
		case INTARR:
			IR_Node index_int = var.getIndex();
			if (index_int instanceof IR_IntLiteral) {
				LocArray loc_array = new LocArray(d.getLocation(), 
						new LocLiteral(((IR_IntLiteral) index_int).getValue()), Type.INTARR);
				ins = context.push(loc_array);
			} else {
				// evaluate index and push index location to stack
				ins = generateExpr(index_int, context);
				LocReg r11 = new LocReg(Regs.R11);
				// convert index to offset (in bytes)
				ins.add(new Instruction("imul", new LocLiteral(CodegenConst.INT_SIZE), r11));
				// saves offset at R11
				ins.add(new Instruction("pop", r11));
				LocArray loc_array = new LocArray(d.getLocation(), r11, Type.INTARR);
				ins.addAll(context.push(loc_array));
			}	
			break;
		case BOOLARR:
			IR_Node index_bool = var.getIndex();
			if (index_bool instanceof IR_IntLiteral) {
				LocArray loc_array = new LocArray(d.getLocation(), 
						new LocLiteral(((IR_IntLiteral) index_bool).getValue()), Type.BOOLARR);
				ins = context.push(loc_array);
			} else {
				// evaluate index and push index location to stack
				ins = generateExpr(index_bool, context);
				LocReg r11 = new LocReg(Regs.R11);
				// convert index to offset (in bytes)
				ins.add(new Instruction("imul", new LocLiteral(CodegenConst.INT_SIZE), r11));
				// saves offset at R11
				ins.add(new Instruction("pop", r11));
				LocArray loc_array = new LocArray(d.getLocation(), r11, Type.BOOLARR);
				ins.addAll(context.push(loc_array));
			}	
			break;
		default:
			break;
		}
		return ins;
	}

	public static List<Instruction> generateBlock(IR_Seq block, CodegenContext context){
		ArrayList<Instruction> ins = new ArrayList<Instruction>();
		List<IR_Node> stmt = block.getStatements();
		for(int ii = 0;ii<stmt.size(); ii++){
			IR_Node st = stmt.get(ii);
			if (st instanceof IR_Call){
				IR_Call call = (IR_Call)st;
				List<Instruction> stIns = generateCall(call,context);
				ins.addAll(stIns);
			}
		}
		return ins;
	}
	
	public static List<Instruction>  generateCall(IR_Call call, CodegenContext context ){
		ArrayList<Instruction> ins = new ArrayList<Instruction>();
		List<IR_Node> args = call.getArgs();
		for(int ii = 0;ii<args.size();ii++){
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
				argSrc = new LocJump("$"+CodegenContext.StringLiteralLoc(idx));
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
			ins.add(new Instruction("mov ", new LocLiteral(0),  new LocReg(Regs.RAX)));			
		}
		ins.add(new Instruction("call ", new LocJump(call.getName()) ));
		
		//pop all arguments on the stack
		if(args.size()>CodegenConst.N_REG_ARG){
			long stackArgSize = CodegenConst.INT_SIZE * (args.size()-CodegenConst.N_REG_ARG);
			ins.add(new Instruction("sub", new LocLiteral(stackArgSize), new LocReg(Regs.RSP)));
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
			ins.add(new Instruction("mov", argSrc, argDst));
		}else{
			ins.addAll(context.push(argSrc));
		}
		return ins;
	}
}
