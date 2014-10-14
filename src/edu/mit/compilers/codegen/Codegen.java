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
		List<Instruction> ins = new ArrayList<Instruction>();
		
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
			IR_EqOp eq= (IR_EqOp)expr; //There's no real difference between CondOp and EqOp except operators
			IR_CompareOp cmp = new IR_CompareOp(eq.getLeft(), eq.getRight(), eq.getOp());
			ins = generateCompareOp(cmp, context); //I've combined all the comparison operations in my one method; it can take and resolve EqOp operators.
			return ins;
		}
		
		else if (expr instanceof IR_Negate){
			IR_Negate negation = (IR_Negate)expr;
			LocReg r10 = new LocReg(Regs.R10);
			ins = generateExpr(negation.getExpr(), context);
			ins.add(new Instruction("pop", r10)); //Get whatever that expr was off stack
			ins.add(new Instruction("not", r10)); //negate it
			ins.add(new Instruction("push", r10)); //push it back to stack
			return ins;
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

		else if (expr instanceof IR_Literal) {
			IR_Literal literal = (IR_Literal) expr;
			ins = generateLiteral(literal, context);
			return ins;
		}
		
		else{
			System.err.println("Unexpected Node type passed to generateExpr.");
			System.err.println("The node passed in was of type " + expr.getType().toString());
		}
		ins = null; 
		
		
	
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

	private static List<Instruction> generateTernaryOp(IR_Ternary ternary, CodegenContext context) {
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		String labelForFalse = context.genLabel();
		Instruction wasFalse = Instruction.labelInstruction(labelForFalse);
		String labelForDone = context.genLabel();
		Instruction doneHere = Instruction.labelInstruction(labelForDone);
		
		List<Instruction> ins = new ArrayList<Instruction>();
		ins.addAll(generateExpr(ternary.getCondition(), context)); //Get result of conditional onto the stack by resolving it. 
		ins.add(new Instruction("pop", r10)); //pop result into r10.
		ins.add(new Instruction("mov", new LocLiteral(1), r11)); // put 1, or true, into r11
		ins.add(new Instruction("cmp", r10, r11)); //Compare r10 against truth
		ins.add(new Instruction("jne", new LocLabel(labelForFalse))); //If result isn't equal, r10 is 0, meaning we take the false branch.
		ins.addAll(generateExpr(ternary.getTrueExpr(), context)); //If we don't jump, resolve the true branch 
		ins.add(new Instruction("jmp", new LocLabel(labelForDone))); //jump to being done
		ins.add(wasFalse); //If we jump, we jump here.
		ins.addAll(generateExpr(ternary.getFalseExpr(), context)); //Resolve the false branch. 
		ins.add(doneHere); //This is where we'd jump to if we resolved the true version, which skips over the whole false branch. 
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
	
	//TODO SHOULD THIS BE CREATING AN IR_BOOL OR SOMETHING? JUST WONDERING...
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
			
	private static List<Instruction> generateCondOp(IR_CondOp conditional, CodegenContext context) {
		// TODO Auto-generated method stub
		List<Instruction> ins = new ArrayList<Instruction>();
		IR_Node left = conditional.getLeft();
		IR_Node right = conditional.getRight();
		LocReg r10 = new LocReg(Regs.R10);
		LocReg r11 = new LocReg(Regs.R11);
		LocReg rax = new LocReg(Regs.RAX);
		Ops op = conditional.getOp();
		
		if( left instanceof IR_CompareOp)
		{
			List<Instruction> lhs = generateCompareOp((IR_CompareOp)left, context);
			ins.addAll(lhs);
			ins.add(new Instruction("pop", r10)); //Get value of comparison from stack. 1 = true, 0 = false.
			String labelForDone = context.genLabel(); //label for end of compare op, want to be consistent across cases.
			Instruction doneHere = Instruction.labelInstruction(labelForDone);
			switch(op){
			
			case AND: //Try to short-circuit on left.
				String labelForTrue = context.genLabel(); //Label for if left is true
				Instruction wasTrue = Instruction.labelInstruction(labelForTrue); //turn label for true into an instruction
				ins.add(new Instruction("mov", new LocLiteral(0), r11)); 
				ins.add(new Instruction("cmp", r10, r11)); //Compare result of lhs and 0
				ins.add(new Instruction("jne", new LocLabel(labelForTrue))); //If it's not 0, it was true, so jump to label for eval rhs
				ins.add(new Instruction("push", new LocLiteral(0))); //If we didn't jump past this, it was false, so push 0 on the stack.
				ins.add(new Instruction("jmp", new LocLabel(labelForDone))); //Jump to done, no need to even try looking at the right. 
				ins.add(wasTrue); //We need to evaluate the right, so worry about what it could be. 
				
				if (right instanceof IR_Literal.IR_BoolLiteral){
					IR_Literal.IR_BoolLiteral rhs = (IR_Literal.IR_BoolLiteral)right;
					boolean valCheck = rhs.getValue(); //handle it here in the Java code
					if(valCheck){ //if right is true, we have True  && True, push a 1 and be done
						ins.add(new Instruction("push", new LocLiteral(1)));
						ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
					}
					else{ //Else we have True && False, push a 0 and be done
						ins.add(new Instruction("push", new LocLiteral(0)));
						ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
					}
					
				}
				
				else if (right instanceof IR_CompareOp){
					List<Instruction> rhs = generateCompareOp((IR_CompareOp)left, context);
					ins.addAll(rhs); // instruction results will be on stack; don't need to know the result of lhs anymore.
					
				}
				
				else if (right instanceof IR_CondOp){
					List<Instruction> rhs = generateCondOp((IR_CondOp)right, context);
					ins.addAll(rhs); //instruction results will be on stack; don't need to know the result of lhs anymore.
				}
				
				
				
				ins.add(doneHere);
				return ins;
			
			
			
			//TODO? What other cases are there? What do we need to worry about comparing?
			
			case OR:
				String labelForFalse = context.genLabel();
				Instruction wasFalse = Instruction.labelInstruction(labelForFalse);
				ins.add(new Instruction("mov", new LocLiteral(1), r11));
				ins.add(new Instruction("cmp", r10, r11));
				ins.add(new Instruction("jne", new LocLabel(labelForFalse))); //IF it was false, we need to check the right hand side.
				ins.add(new Instruction("push", new LocLiteral(1))); //If we didn't jump, it was true, so don't even bother with the rhs
				ins.add(new Instruction("jmp", new LocLabel(labelForDone))); //Push one to stack and be done. 
				ins.add(wasFalse);
				
				if (right instanceof IR_Literal.IR_BoolLiteral){
					IR_Literal.IR_BoolLiteral rhs = (IR_Literal.IR_BoolLiteral)right;
					boolean valCheck = rhs.getValue(); //handle it here in the Java code
					if(valCheck){ //if right is true, we have False || True, push a 1 and be done
						ins.add(new Instruction("push", new LocLiteral(1)));
						ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
					}
					else{ //Else we have False || False, push a 0 and be done
						ins.add(new Instruction("push", new LocLiteral(0)));
						ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
					}
					
				}
				
				else if (right instanceof IR_CompareOp){
					List<Instruction> rhs = generateCompareOp((IR_CompareOp)left, context);
					ins.addAll(rhs); // instruction results will be on stack; don't need to know the result of lhs anymore.
					
				}
				
				else if (right instanceof IR_CondOp){
					List<Instruction> rhs = generateCondOp((IR_CondOp)right, context);
					ins.addAll(rhs); //instruction results will be on stack; don't need to know the result of lhs anymore.
				}
				
			
				ins.add(doneHere);
				return ins;
				
			default:
				System.err.println("Should never reach here - a non AND or OR was passed into CondOp.");
				return null;
			}
		}
		
		
		else if (left instanceof IR_Literal.IR_BoolLiteral){
			IR_Literal.IR_BoolLiteral lhs = (IR_Literal.IR_BoolLiteral) left;
			String labelForDone = context.genLabel(); //label for end of compare op, want to be consistent across cases.
			Instruction doneHere = Instruction.labelInstruction(labelForDone);
			boolean valCheckLeft = lhs.getValue();
			switch(op)
			{
			case AND:
				
				if (!valCheckLeft){//If false, we have False && stuff. Ignore rhs. Push 0 and be done. 
					ins.add(new Instruction("push", new LocLiteral(0)));
					return ins;
				}
				
				else{ //valCheck was True, so answer depends on rhs alone.
					if (right instanceof IR_Literal.IR_BoolLiteral){
						IR_Literal.IR_BoolLiteral rhs = (IR_Literal.IR_BoolLiteral)right;
						boolean valCheck = rhs.getValue(); //handle it here in the Java code
						if(valCheck){ //if right is true, we have True  && True, push a 1 and be done
							ins.add(new Instruction("push", new LocLiteral(1)));
							ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
						}
						else{ //Else we have True && False, push a 0 and be done
							ins.add(new Instruction("push", new LocLiteral(0)));
							ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
						}
						
					}
					
					else if (right instanceof IR_CompareOp){
						List<Instruction> rhs = generateCompareOp((IR_CompareOp)left, context);
						ins.addAll(rhs); // instruction results will be on stack; don't need to know the result of lhs anymore.
						
					}
					
					else if (right instanceof IR_CondOp){
						List<Instruction> rhs = generateCondOp((IR_CondOp)right, context);
						ins.addAll(rhs); //instruction results will be on stack; don't need to know the result of lhs anymore.
					}
				}

				ins.add(doneHere);
				return ins;
				
			
			case OR:
				if (valCheckLeft){ //We have True || stuff. Ignore rhs and return True.
					ins.add(new Instruction("push", new LocLiteral(1)));
					return ins;
				}
				
				else{//We have False || stuff. Answer is only dependent on stuff.
					if (right instanceof IR_Literal.IR_BoolLiteral){
						IR_Literal.IR_BoolLiteral rhs = (IR_Literal.IR_BoolLiteral)right;
						boolean valCheck = rhs.getValue(); //handle it here in the Java code
						if(valCheck){ //if right is true, we have False || True, push a 1 and be done
							ins.add(new Instruction("push", new LocLiteral(1)));
							ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
						}
						else{ //Else we have False || False, push a 0 and be done
							ins.add(new Instruction("push", new LocLiteral(0)));
							ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
						}
						
					}
					
					else if (right instanceof IR_CompareOp){
						List<Instruction> rhs = generateCompareOp((IR_CompareOp)left, context);
						ins.addAll(rhs); // instruction results will be on stack; don't need to know the result of lhs anymore.
						
					}
					
					else if (right instanceof IR_CondOp){
						List<Instruction> rhs = generateCondOp((IR_CondOp)right, context);
						ins.addAll(rhs); //instruction results will be on stack; don't need to know the result of lhs anymore.
					}
				}
			ins.add(doneHere);
			return ins;
			
			
			default:
				System.err.println("Should never reach here - a non AND or OR was passed into CondOp.");
				return null;
			}
		}
		
		else if (left instanceof IR_CondOp){
			List<Instruction> lhs = generateCondOp((IR_CondOp) left, context);
			ins.add(new Instruction("pop", r10)); //Result of lhs was on stack, get it off and into r10. From here on, it's a copy of what I did before for comp op.
			String labelForDone = context.genLabel(); //label for end of compare op, want to be consistent across cases.
			Instruction doneHere = Instruction.labelInstruction(labelForDone);
			switch(op){
			
			case AND: //Try to short-circuit on left.
				String labelForTrue = context.genLabel(); //Label for if left is true
				Instruction wasTrue = Instruction.labelInstruction(labelForTrue); //turn label for true into an instruction
				ins.add(new Instruction("mov", new LocLiteral(0), r11)); 
				ins.add(new Instruction("cmp", r10, r11)); //Compare result of lhs and 0
				ins.add(new Instruction("jne", new LocLabel(labelForTrue))); //If it's not 0, it was true, so jump to label for eval rhs
				ins.add(new Instruction("push", new LocLiteral(0))); //If we didn't jump past this, it was false, so push 0 on the stack.
				ins.add(new Instruction("jmp", new LocLabel(labelForDone))); //Jump to done, no need to even try looking at the right. 
				ins.add(wasTrue); //We need to evaluate the right, so worry about what it could be. 
				
				if (right instanceof IR_Literal.IR_BoolLiteral){
					IR_Literal.IR_BoolLiteral rhs = (IR_Literal.IR_BoolLiteral)right;
					boolean valCheck = rhs.getValue(); //handle it here in the Java code
					if(valCheck){ //if right is true, we have True  && True, push a 1 and be done
						ins.add(new Instruction("push", new LocLiteral(1)));
						ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
					}
					else{ //Else we have True && False, push a 0 and be done
						ins.add(new Instruction("push", new LocLiteral(0)));
						ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
					}
					
				}
				
				else if (right instanceof IR_CompareOp){
					List<Instruction> rhs = generateCompareOp((IR_CompareOp)left, context);
					ins.addAll(rhs); // instruction results will be on stack; don't need to know the result of lhs anymore.
					
				}
				
				else if (right instanceof IR_CondOp){
					List<Instruction> rhs = generateCondOp((IR_CondOp)right, context);
					ins.addAll(rhs); //instruction results will be on stack; don't need to know the result of lhs anymore.
				}
				
				
				
				ins.add(doneHere);
				return ins;
			
			
			//TODO? What other cases are there? What do we need to worry about comparing? Did we miss something?!?! Still an issue! 
			
			case OR:
				String labelForFalse = context.genLabel();
				Instruction wasFalse = Instruction.labelInstruction(labelForFalse);
				ins.add(new Instruction("mov", new LocLiteral(1), r11));
				ins.add(new Instruction("cmp", r10, r11));
				ins.add(new Instruction("jne", new LocLabel(labelForFalse))); //IF it was false, we need to check the right hand side.
				ins.add(new Instruction("push", new LocLiteral(1))); //If we didn't jump, it was true, so don't even bother with the rhs
				ins.add(new Instruction("jmp", new LocLabel(labelForDone))); //Push one to stack and be done. 
				ins.add(wasFalse);
				
				if (right instanceof IR_Literal.IR_BoolLiteral){
					IR_Literal.IR_BoolLiteral rhs = (IR_Literal.IR_BoolLiteral)right;
					boolean valCheck = rhs.getValue(); //handle it here in the Java code
					if(valCheck){ //if right is true, we have False || True, push a 1 and be done
						ins.add(new Instruction("push", new LocLiteral(1)));
						ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
					}
					else{ //Else we have False || False, push a 0 and be done
						ins.add(new Instruction("push", new LocLiteral(0)));
						ins.add(new Instruction("jmp", new LocLabel(labelForDone)));
					}
					
				}
				
				else if (right instanceof IR_CompareOp){
					List<Instruction> rhs = generateCompareOp((IR_CompareOp)left, context);
					ins.addAll(rhs); // instruction results will be on stack; don't need to know the result of lhs anymore.
					
				}
				
				else if (right instanceof IR_CondOp){
					List<Instruction> rhs = generateCondOp((IR_CondOp)right, context);
					ins.addAll(rhs); //instruction results will be on stack; don't need to know the result of lhs anymore.
				}
				
			
				ins.add(doneHere);
				return ins;

				
			
			default:
				System.err.println("Should never reach here - a non AND or OR was passed into CondOp.");
				return null;
			}
		}
		
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
