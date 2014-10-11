package edu.mit.compilers.codegen;
import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.*;
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

	public static void generateFieldDecl(IR_FieldDecl decl, CodegenContext context){
		
	}
	
	public static void generateFieldDeclGlobal(IR_FieldDecl decl, CodegenContext context){
		
	}
	
	public static void generateMethodCall(IR_Call call, CodegenContext context){
		
	}

	public static void generateCalloutCall(){
		
	}
	
	/**@brief expression nodes should return location of the result
	 * 
	 * @param expr
	 * @param context
	 * @return
	 */
	public static List<Instruction> generateExpr(IR_Node expr, CodegenContext context){
		
		return null;
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
			if(arg instanceof IR_Literal.IR_StringLiteral){
				IR_Literal.IR_StringLiteral sl=(IR_Literal.IR_StringLiteral)arg;
				String ss = sl.getValue();
				Long idx = context.stringLiterals.get(ss);
				if(idx==null){
					idx = (long) context.stringLiterals.size();
					context.stringLiterals.put(ss, idx);
				}
				LocationMem aa= argLoc(ii);
				Instruction argIns = new Instruction("mov", new LocJump("$"+CodegenContext.StringLiteralLoc(idx)), aa);
				ins.add(argIns);
			}
		}
		ins.add(new Instruction("call ", new LocJump(call.getName()) ));
		return ins;
	}
	
	private static final Regs regArg[] = {Regs.RDI, Regs.RSI, Regs.RDX,
			Regs.RCX, Regs.R8, Regs.R9};

	private static LocationMem argLoc(int idx){
		if(idx<6){
			return new LocReg(regArg[idx]);
		}
		long offset = 16+(idx-6)*CodegenConst.INT_SIZE;
		return new LocStack(offset);
	}
}
