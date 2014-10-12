package edu.mit.compilers.codegen;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.*;
import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;
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
		List<Instruction> ins = new ArrayList<Instruction>();
		List<List<Instruction>> intermediates = new ArrayList<List<Instruction>>();
		
		if (expr instanceof IR_ArithOp){
			ins = generateArithExpr((IR_ArithOp) expr, context);
			return ins;
		}
		
		else if(expr instanceof IR_CompareOp){
			IR_CompareOp compare = (IR_CompareOp) expr;
			ins = generateCompareOp(compare, context);
			return ins;
		}
		
		else if (expr instanceof IR_CondOp){
			IR_CondOp conditional = (IR_CondOp)expr;
			ins = generateCondOp(conditional, context);
			return ins;
		}
		
		else if (expr instanceof IR_EqOp){
			IR_EqOp equivalence = (IR_EqOp)expr;
		}
		
		else if (expr instanceof IR_Negate){
			IR_Negate negation = (IR_Negate)expr;
			//%TODO Ask group - Want to do new Instruction("neg", ????) where ???? needs to be resolved.
		}
		
		else if (expr instanceof IR_Ternary){
			IR_Ternary ternary = (IR_Ternary)expr;
			ins = generateTernaryOp(ternary, context);
			return ins;
		}
		
		if(expr instanceof IR_Var){
			IR_Var var = (IR_Var)expr;
			ins=generateVarExpr(var, context);
			return ins;
		}

		else{
			System.err.println("Unexpected Node type passed to generateExpr.");
			System.err.println("The node passed in was of type " + expr.getType().toString());
		}
		ins = null;
		return ins;
		
		
		
		
	}


	private static List<Instruction> generateTernaryOp(IR_Ternary ternary,CodegenContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	public static List<Instruction> generateVarExpr(IR_Var var, CodegenContext context){
		List<Instruction> ins=null;
		switch(var.getType()){
		case INT:
			Descriptor d = context.findSymbol(var.getName());
			ins = context.push(d.getLocation());
			break;
		default:
			break;
		}
		return ins;
	}

	public static List<Instruction> handleRHSisIR_Var(List<Instruction> ins, CodegenContext context, IR_Node right, String operation)
	{
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		List<Instruction> rhs = generateVarExpr((IR_Var)right, context);
		ins.addAll(rhs);
		ins.add(new Instruction("pop", r11)); //Pop rhs value into r11
		ins.add(new Instruction("pop", r10)); //pop lhs value into r10
		
		if (operation != "idiv"){
			ins.add(new Instruction(operation, r11, r10)); // Use the operator on the two and put result in r10
			if (!operation.startsWith("cmp")){
				ins.add(new Instruction("push", r10)); //put r10 contents on stack
			}
			
			else
			{ 	//Instruction is cmpXX
				LocReg rax = new LocReg(Regs.RAX);
				//IMPORTANT: FOR "mov**" commands, THE OPERANDS MUST BE TWO REGISTERS.
				switch(operation){
					case "cmpeq":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmove", r11, rax)); //If equal, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovne", r11, rax)); //If not equal, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
					
					case "cmpgt":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovg", r11, rax)); //If greater, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovle", r11, rax)); //If less than or equal, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
					
					case "cmpge":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovge", r11, rax)); //If greater or equal, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovl", r11, rax)); //If less than, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
						
					case "cmplt":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovl", r11, rax)); //If less, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovge", r11, rax)); //If greater than or equal, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
					
					case "cmple":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovle", r11, rax)); //If less or equal, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovg", r11, rax)); //If greater than , result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
				}
			}
		}
		
		else{ //Instruction is idiv
			LocReg rdx = new LocReg(Regs.RDX);
			LocReg rax = new LocReg(Regs.RAX);
	
			ins.add(new Instruction("mov", r10, rax));
			ins.add(new Instruction("push", rdx));
			ins.add(new Instruction("cqto"));
			ins.add(new Instruction("idiv", r11)); //Divide rdx:rax by r11 contents - i.e divide lhs by rhs.
			//Restoration of rdx and pushing of the proper result is done back in the main method. 
		}
		return ins; //Result is in R10 and on the stack.
	}
	
	public static List<Instruction> handleRHSisIR_ArithOp(List<Instruction> ins, CodegenContext context, IR_Node right, String operation)
	{
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		List<Instruction> rhs = generateArithExpr((IR_ArithOp)right, context);
		ins.addAll(rhs); //Will be in R10 and on stack
		ins.add(new Instruction("pop", r11)); //Pop rhs value into r11
		ins.add(new Instruction("pop", r10)); //pop lhs value into r10
		if (operation != "idiv")
		{
			ins.add(new Instruction(operation, r11, r10)); // Use the operator on the two and put result in r10
			if (!operation.startsWith("cmp"))
			{
				ins.add(new Instruction("push", r10)); //put r10 contents on stack
			}
			
			else
			{ 	//Instruction is cmpXX
				LocReg rax = new LocReg(Regs.RAX);
				//IMPORTANT: FOR "mov**" commands, THE OPERANDS MUST BE TWO REGISTERS.
				switch(operation){
					case "cmpeq":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmove", r11, rax)); //If equal, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovne", r11, rax)); //If not equal, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
					
					case "cmpgt":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovg", r11, rax)); //If greater, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovle", r11, rax)); //If less than or equal, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
					
					case "cmpge":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovge", r11, rax)); //If greater or equal, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovl", r11, rax)); //If less than, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
						
					case "cmplt":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovl", r11, rax)); //If less, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovge", r11, rax)); //If greater than or equal, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
					
					case "cmple":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovle", r11, rax)); //If less or equal, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovg", r11, rax)); //If greater than , result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
				}
			}
		}
		
		else{
			LocReg rdx = new LocReg(Regs.RDX);
			LocReg rax = new LocReg(Regs.RAX);
	
			ins.add(new Instruction("mov", r10, rax));
			ins.add(new Instruction("push", rdx));
			ins.add(new Instruction("cqto"));
			ins.add(new Instruction("idiv", r11)); //Divide rdx:rax by r11 contents - i.e divide lhs by rhs.
			//Restoration of rdx and pushing of the proper result is done back in the main method. 
		}
		return ins; //Result is in R10 and on the stack.
	}
	
	public static List<Instruction> handleRHSisIR_Literal(List<Instruction> ins, CodegenContext context, IR_Node right, String operation)
	{
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		IR_Literal.IR_IntLiteral rhs = (IR_Literal.IR_IntLiteral)right;
		ins.add(new Instruction("pop", r10)); //put lhs result in r10
		ins.add(new Instruction("mov", new LocLiteral(rhs.getValue()), r11)); //Put literal in r11
		if (operation != "idiv"){
			ins.add(new Instruction(operation, r11, r10)); // Use the operator on the two and put result in r10
			if (!operation.startsWith("cmp")){
				ins.add(new Instruction("push", r10)); //put r10 contents on stack
			}
			
			else
			{ 	//Instruction is cmpXX
				LocReg rax = new LocReg(Regs.RAX);
				//IMPORTANT: FOR "mov**" commands, THE OPERANDS MUST BE TWO REGISTERS.
				switch(operation){
					case "cmpeq":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmove", r11, rax)); //If equal, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovne", r11, rax)); //If not equal, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
					
					case "cmpgt":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovg", r11, rax)); //If greater, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovle", r11, rax)); //If less than or equal, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
					
					case "cmpge":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovge", r11, rax)); //If greater or equal, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovl", r11, rax)); //If less than, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
						
					case "cmplt":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovl", r11, rax)); //If less, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovge", r11, rax)); //If greater than or equal, result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
					
					case "cmple":
						ins.add(new Instruction("mov", new LocLiteral(1), r11)); //Overwrite r11 with 1
						ins.add(new Instruction("cmovle", r11, rax)); //If less or equal, result is "1"
						ins.add(new Instruction("mov", new LocLiteral(0), r11)); //Overwrite r11 with 0
						ins.add(new Instruction("cmovg", r11, rax)); //If greater than , result is "0"
						ins.add(new Instruction("push", rax)); //Push the result to the stack.
						return ins;
				}
			}
		}
		
		else{
			LocReg rdx = new LocReg(Regs.RDX);
			LocReg rax = new LocReg(Regs.RAX);
	
			ins.add(new Instruction("mov", r10, rax));
			ins.add(new Instruction("push", rdx));
			ins.add(new Instruction("cqto"));
			ins.add(new Instruction("idiv", r11)); //Divide rdx:rax by r11 contents - i.e divide lhs by rhs.
			//Restoration of rdx and pushing of the proper result is done back in the main method. 
		}
		return ins; //Result is in R10 and on the stack.
	}
	
	public static List<Instruction> restoreRDXForDiv(List<Instruction> ins)
	{
		//Since division, result is stored in RAX.
		//RDX is currently floating on top of stack. Recover it!
		LocReg r10 = new LocReg(Regs.R10);
		LocReg rdx = new LocReg(Regs.RDX);
		LocReg rax = new LocReg(Regs.RAX);
		ins.add(new Instruction("mov", rax, r10)); //put quotient into r10
		ins.add(new Instruction("pop", rdx));
		ins.add(new Instruction("push", r10));
		return ins;
	}
	
	public static List<Instruction> restoreRDXForMod(List<Instruction> ins)
	{
		//Since mod, result is stored in RDX.
		//RDX is currently floating on top of stack. Recover it!
				LocReg r10 = new LocReg(Regs.R10);
				LocReg rdx = new LocReg(Regs.RDX);
				ins.add(new Instruction("mov", rdx, r10)); //Put mod in r10
				ins.add(new Instruction("pop", rdx)); //restore rdx
				ins.add(new Instruction("push", r10)); //put mod on stack
				return ins;
	}
	
	public static List<Instruction> generateArithExpr(IR_ArithOp arith, CodegenContext context){
		
		List<Instruction> ins = new ArrayList<Instruction>();
		
		Ops op = arith.getOp();
		if (arith.getLeft().getType() != Type.INT || arith.getRight().getType() != Type.INT){
			System.err.println("Non integer arguments detected in generateArithExpr. Returning null list.");
			ins = null;
			return ins;
		}
		IR_Node left = arith.getLeft();
		IR_Node right = arith.getRight();
		
		switch (op){ //PLUS, MINUS, TIMES, DIVIDE, MOD
		
			case PLUS:
				if (left instanceof IR_Var)
				{
					List<Instruction> lhs = generateVarExpr((IR_Var)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						return handleRHSisIR_Var(ins, context, right, "add");
					}
				
					if (right instanceof IR_ArithOp){
						return handleRHSisIR_ArithOp(ins, context, right, "add");
					}
				
				
					if (right instanceof IR_Literal){
						return handleRHSisIR_Literal(ins, context, right, "add");
					}
				}
				
				if (left instanceof IR_ArithOp) 
				{
					List<Instruction> lhs = generateArithExpr((IR_ArithOp)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						return handleRHSisIR_Var(ins, context, right, "add");
					}
					
					if (right instanceof IR_ArithOp){
						return handleRHSisIR_ArithOp(ins, context, right, "add");
					}
					
					if (right instanceof IR_Literal){
						return handleRHSisIR_Literal(ins, context, right, "add");
					}
				}
				
					
				
				if (left instanceof IR_Literal)
				{
					IR_Literal.IR_IntLiteral lhs = (IR_Literal.IR_IntLiteral)left;
					ins.add(new Instruction("push", new LocLiteral(lhs.getValue()))); //Push the literal onto the stack so helpers can pop it
					
					if (right instanceof IR_Var){
						return handleRHSisIR_Var(ins, context, right, "add");
					}
				
					if (right instanceof IR_ArithOp){
						return handleRHSisIR_ArithOp(ins, context, right, "add");
					}
					
					if (right instanceof IR_Literal){
						return handleRHSisIR_Literal(ins, context, right, "add");
					}
				}
				break;
				
			case MINUS:
				if (left instanceof IR_Var)
				{
					List<Instruction> lhs = generateVarExpr((IR_Var)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						return handleRHSisIR_Var(ins, context, right, "sub");
					}
				
					if (right instanceof IR_ArithOp){
						return handleRHSisIR_ArithOp(ins, context, right, "sub");
					}
				
				
					if (right instanceof IR_Literal){
						return handleRHSisIR_Literal(ins, context, right, "sub");
					}
				}
				
				if (left instanceof IR_ArithOp) 
				{
					List<Instruction> lhs = generateArithExpr((IR_ArithOp)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						return handleRHSisIR_Var(ins, context, right, "sub");
					}
					
					if (right instanceof IR_ArithOp){
						return handleRHSisIR_ArithOp(ins, context, right, "sub");
					}
					
					if (right instanceof IR_Literal){
						return handleRHSisIR_Literal(ins, context, right, "sub");
					}
				}
				
					
				
				if (left instanceof IR_Literal)
				{
					IR_Literal.IR_IntLiteral lhs = (IR_Literal.IR_IntLiteral)left;
					ins.add(new Instruction("push", new LocLiteral(lhs.getValue()))); //Push the literal onto the stack so helpers can pop it
					
					if (right instanceof IR_Var){
						return handleRHSisIR_Var(ins, context, right, "sub");
					}
				
					if (right instanceof IR_ArithOp){
						return handleRHSisIR_ArithOp(ins, context, right, "sub");
					}
					
					if (right instanceof IR_Literal){
						return handleRHSisIR_Literal(ins, context, right, "sub");
					}
				}
				
				break;
				
			case TIMES:
				if (left instanceof IR_Var)
				{
					List<Instruction> lhs = generateVarExpr((IR_Var)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						return handleRHSisIR_Var(ins, context, right, "imul");
					}
				
					if (right instanceof IR_ArithOp){
						return handleRHSisIR_ArithOp(ins, context, right, "imul");
					}
				
				
					if (right instanceof IR_Literal){
						return handleRHSisIR_Literal(ins, context, right, "imul");
					}
				}
				
				if (left instanceof IR_ArithOp) 
				{
					List<Instruction> lhs = generateArithExpr((IR_ArithOp)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						return handleRHSisIR_Var(ins, context, right, "imul");
					}
					
					if (right instanceof IR_ArithOp){
						return handleRHSisIR_ArithOp(ins, context, right, "imul");
					}
					
					if (right instanceof IR_Literal){
						return handleRHSisIR_Literal(ins, context, right, "imul");
					}
				}
				
					
				
				if (left instanceof IR_Literal)
				{
					IR_Literal.IR_IntLiteral lhs = (IR_Literal.IR_IntLiteral)left;
					ins.add(new Instruction("push", new LocLiteral(lhs.getValue()))); //Push the literal onto the stack so helpers can pop it
					
					if (right instanceof IR_Var){
						return handleRHSisIR_Var(ins, context, right, "imul");
					}
				
					if (right instanceof IR_ArithOp){
						return handleRHSisIR_ArithOp(ins, context, right, "imul");
					}
					
					if (right instanceof IR_Literal){
						return handleRHSisIR_Literal(ins, context, right, "imul");
					}
				}
				break;
				
			case DIVIDE:
				if (left instanceof IR_Var)
				{
					List<Instruction> lhs = generateVarExpr((IR_Var)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						ins =  handleRHSisIR_Var(ins, context, right, "idiv");
						return restoreRDXForDiv(ins);
					}
				
					if (right instanceof IR_ArithOp){
						ins = handleRHSisIR_ArithOp(ins, context, right, "idiv");
						return restoreRDXForDiv(ins);
					}
				
				
					if (right instanceof IR_Literal){
						ins = handleRHSisIR_Literal(ins, context, right, "idiv");
						return restoreRDXForDiv(ins);
					}
				}
				
				if (left instanceof IR_ArithOp) 
				{
					List<Instruction> lhs = generateArithExpr((IR_ArithOp)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						ins = handleRHSisIR_Var(ins, context, right, "idiv");
						return restoreRDXForDiv(ins);
					}
					
					if (right instanceof IR_ArithOp){
						ins = handleRHSisIR_ArithOp(ins, context, right, "idiv");
						return restoreRDXForDiv(ins);
					}
					
					if (right instanceof IR_Literal){
						ins = handleRHSisIR_Literal(ins, context, right, "idiv");
						return restoreRDXForDiv(ins);
					}
				}
				
					
				
				if (left instanceof IR_Literal)
				{
					IR_Literal.IR_IntLiteral lhs = (IR_Literal.IR_IntLiteral)left;
					ins.add(new Instruction("push", new LocLiteral(lhs.getValue()))); //Push the literal onto the stack so helpers can pop it
					
					if (right instanceof IR_Var){
						ins = handleRHSisIR_Var(ins, context, right, "idiv");
						return restoreRDXForDiv(ins);
					}
				
					if (right instanceof IR_ArithOp){
						ins = handleRHSisIR_ArithOp(ins, context, right, "idiv");
						return restoreRDXForDiv(ins);
					}
					
					if (right instanceof IR_Literal){
						ins = handleRHSisIR_Literal(ins, context, right, "idiv");
						return restoreRDXForDiv(ins);
					}
				}
				break;
				
			case MOD:
				if (left instanceof IR_Var)
				{
					List<Instruction> lhs = generateVarExpr((IR_Var)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						ins =  handleRHSisIR_Var(ins, context, right, "idiv");
						return restoreRDXForMod(ins);
					}
				
					if (right instanceof IR_ArithOp){
						ins = handleRHSisIR_ArithOp(ins, context, right, "idiv");
						return restoreRDXForMod(ins);
					}
				
				
					if (right instanceof IR_Literal){
						ins = handleRHSisIR_Literal(ins, context, right, "idiv");
						return restoreRDXForMod(ins);
					}
				}
				
				if (left instanceof IR_ArithOp) 
				{
					List<Instruction> lhs = generateArithExpr((IR_ArithOp)left, context);
					ins.addAll(lhs);
					
					if (right instanceof IR_Var){
						ins = handleRHSisIR_Var(ins, context, right, "idiv");
						return restoreRDXForMod(ins);
					}
					
					if (right instanceof IR_ArithOp){
						ins = handleRHSisIR_ArithOp(ins, context, right, "idiv");
						return restoreRDXForMod(ins);
					}
					
					if (right instanceof IR_Literal){
						ins = handleRHSisIR_Literal(ins, context, right, "idiv");
						return restoreRDXForMod(ins);
					}
				}
				
					
				
				if (left instanceof IR_Literal)
				{
					IR_Literal.IR_IntLiteral lhs = (IR_Literal.IR_IntLiteral)left;
					ins.add(new Instruction("push", new LocLiteral(lhs.getValue()))); //Push the literal onto the stack so helpers can pop it
					
					if (right instanceof IR_Var){
						ins = handleRHSisIR_Var(ins, context, right, "idiv");
						return restoreRDXForMod(ins);
					}
				
					if (right instanceof IR_ArithOp){
						ins = handleRHSisIR_ArithOp(ins, context, right, "idiv");
						return restoreRDXForMod(ins);
					}
					
					if (right instanceof IR_Literal){
						ins = handleRHSisIR_Literal(ins, context, right, "idiv");
						return restoreRDXForMod(ins);
					}
				}
				break;
			
				default:
					System.err.println("Somehow, no cases matched. Debug the switch statement!");
					return null;
		}
		
		return ins;
		
	}
	
	//TODO SHOULD THIS BE CREATING AN IR_BOOL OR SOMETHING? JUST WONDERING...
	private static List<Instruction> generateCompareOp(IR_CompareOp compare, CodegenContext context) {
			List<Instruction> ins = new ArrayList<Instruction>();
			IR_Node left = compare.getLeft();
			IR_Node right = compare.getRight();
			if (left.getType() != right.getType()){
				System.err.println("Incomparable arguments passed into generateCompareOp.");
				return null;
			}
			
			Ops op = compare.getOp();
			String operator;
			switch(op){
			case GT:
				operator = "cmpgt";
				break;
			case GTE:
				operator = "cmpge";
				break;
			case EQUALS:
				operator = "cmpeq";
				break;
			case LT:
				operator = "cmplt";
				break;
			case LTE:
				operator = "cmple";
				break;
			default:
				return null; //Irrecoverable, can't compare with an incorrect op
			}
			
			
			if (left instanceof IR_Var)
			{
				List<Instruction> lhs = generateVarExpr((IR_Var)left, context);
				ins.addAll(lhs);

				if (right instanceof IR_Var){
					return handleRHSisIR_Var(ins, context, right, operator);
				}

				if (right instanceof IR_ArithOp){
					return handleRHSisIR_ArithOp(ins, context, right, operator);
				}


				if (right instanceof IR_Literal){
					return handleRHSisIR_Literal(ins, context, right, operator);
				}
				
				if (right instanceof IR_CompareOp){
					ins.addAll(generateCompareOp((IR_CompareOp)right, context));
					return ins;
				}
			}

			if (left instanceof IR_ArithOp) 
			{
				List<Instruction> lhs = generateArithExpr((IR_ArithOp)left, context);
				ins.addAll(lhs);

				if (right instanceof IR_Var){
					return handleRHSisIR_Var(ins, context, right, operator);
				}

				if (right instanceof IR_ArithOp){
					return handleRHSisIR_ArithOp(ins, context, right, operator);
				}

				if (right instanceof IR_Literal){
					return handleRHSisIR_Literal(ins, context, right, operator);
				}
				
				if (right instanceof IR_CompareOp){
					ins.addAll(generateCompareOp((IR_CompareOp)right, context));
					return ins;
				}
			}


			if (left instanceof IR_Literal)
			{
				IR_Literal.IR_IntLiteral lhs = (IR_Literal.IR_IntLiteral)left;
				ins.add(new Instruction("push", new LocLiteral(lhs.getValue()))); //Push the literal onto the stack so helpers can pop it

				if (right instanceof IR_Var){
					return handleRHSisIR_Var(ins, context, right, operator);
				}

				if (right instanceof IR_ArithOp){
					return handleRHSisIR_ArithOp(ins, context, right, operator);
				}

				if (right instanceof IR_Literal){
					return handleRHSisIR_Literal(ins, context, right, operator);
				}
				
				if (right instanceof IR_CompareOp){
					ins.addAll(generateCompareOp((IR_CompareOp)right, context));
					return ins;
				}
			}
			
			
			if (left instanceof IR_CompareOp){
				List<Instruction> lhs = generateCompareOp((IR_CompareOp)left, context);
				ins.addAll(lhs);
				
				if (right instanceof IR_Var){
					return handleRHSisIR_Var(ins, context, right, operator);
				}

				if (right instanceof IR_ArithOp){
					return handleRHSisIR_ArithOp(ins, context, right, operator);
				}

				if (right instanceof IR_Literal){
					return handleRHSisIR_Literal(ins, context, right, operator);
				}
				
				if (right instanceof IR_CompareOp){
					ins.addAll(generateCompareOp((IR_CompareOp)right, context));
					return ins;
				}
			}
			
			return ins;
	}
			
	private static List<Instruction> generateCondOp(IR_CondOp conditional, CodegenContext context) {
		// TODO Auto-generated method stub
		List<Instruction> ins = new ArrayList<Instruction>();
		IR_Node left = conditional.getLeft();
		IR_Node right = conditional.getRight();
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		Ops op = conditional.getOp();
		
		if( left instanceof IR_CompareOp)
		{
			List<Instruction> lhs = generateCompareOp((IR_CompareOp)left, context);
			ins.addAll(lhs);
			ins.add(new Instruction("pop", r10)); //Get value of comparison from stack. 1 = true, 0 = false.
			if (right instanceof IR_Literal.IR_BoolLiteral){
				IR_Literal.IR_BoolLiteral rhs = (IR_Literal.IR_BoolLiteral)right;
				//TODO rhs.getValue() is a Boolean...how to use it? 1 for true 0 for false? And do cmp on the 0 and 1?
				
			}
			
			//TODO What other cases are there? What do we need to worry about comparing?
			
			if (right instanceof IR_CompareOp){
				List<Instruction> rhs = generateCompareOp((IR_CompareOp)left, context);
				ins.addAll(rhs);
			}
		}
		
		
		
		
		
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
		ins.add(new Instruction("call ", new LocJump(call.getName()) ));
		
		//pop all arguments on the stack
		if(args.size()>CodegenConst.N_REG_ARG){
			long stackArgSize = CodegenConst.INT_SIZE * (args.size()-CodegenConst.N_REG_ARG);
			ins.add(new Instruction("sub", new LocLiteral(stackArgSize), new LocReg(Regs.RSP)));
		}
		return ins;
	}
	
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
