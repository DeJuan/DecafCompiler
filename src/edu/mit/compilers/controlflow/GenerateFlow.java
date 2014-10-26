package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.mit.compilers.codegen.*;
import edu.mit.compilers.ir.*;
import edu.mit.compilers.ir.IR_Literal.*;

public class GenerateFlow {
	
	/**@brief generate control flow nodes given the root node of IR.
	 * 
	 * @param node: root IR node
	 * @param context : context object
	 * @param callouts : list of IR nodes containing callouts
	 * @param globals : list of IR nodes containing global declarations
	 * @param flowNodes : HashMap of method name to FlowNode
	 */
	public static void generateProgram(IR_Node root, ControlflowContext context, 
			List<IR_Node> callouts, List<IR_Node> globals, HashMap<String, FlowNode> flowNodes) {
		IR_Seq seq = (IR_Seq) root;
		List<IR_Node> statements = seq.getStatements();
		for (int i = 0; i < statements.size(); i++) {
			IR_Node node = statements.get(i);
			if (node.getType() == Type.METHOD) {
				// method declaration
				FlowNode start = generateMethodDecl(node, context);
				String name = ((IR_MethodDecl) node).getName();
				flowNodes.put(name, start);
			} else if (node.getType() == Type.CALLOUT) {
				// callout
				callouts.add(node);
			} else if (node instanceof IR_FieldDecl) {
				// global field declaration
				IR_FieldDecl decl = (IR_FieldDecl) node;
				Descriptor d = new Descriptor(decl);
				context.putSymbol(decl.getName(), d);
				globals.add(node);
			} else {
				System.err.println("Unrecognized type");
			}
		}
	}
	
	public static FlowNode generateMethodDecl(IR_Node node, ControlflowContext context) {
		IR_MethodDecl decl = (IR_MethodDecl) node;
		String name = decl.name;
		Descriptor d = new Descriptor(node);
		context.putSymbol(name, d);
		context.incScope();
		
		for (int i = 0; i < decl.args.size(); i++) {
			IR_FieldDecl a = decl.args.get(i);
			Descriptor argd = new Descriptor(a);
			context.putSymbol(a.getName(), argd);
		}
		
		START start = new START(decl.args);
		FlowNode returnNode = generateFlow(start, decl.body, context);
		if (!(returnNode instanceof END)) {
			// No specified return value --> assume void
			END end = new END();
			returnNode.addChild(end);
			end.addParent(returnNode);
		}
		context.decScope();
		return start;	
	}
	
	public static FlowNode generateFlow(FlowNode prevNode, IR_Seq seq, ControlflowContext context) {
		context.incScope();
		List<IR_Node> statements = seq.getStatements();
		FlowNode curNode = prevNode;
		for (int i = 0; i < statements.size(); i++) {
			IR_Node st = statements.get(i);
			if (st instanceof IR_If || st instanceof IR_For || st instanceof IR_While) {
				// This is a branch.
				FlowNode branchEnd = null;
				if (st instanceof IR_If) {
					branchEnd = generateIf(curNode, (IR_If) st, context);
				} else if (st instanceof IR_For) {
					branchEnd = generateFor(curNode, (IR_For) st, context);
	            } else if (st instanceof IR_While) {
	            	branchEnd = generateWhile(curNode, (IR_While) st, context);
				}
				curNode = branchEnd;
				if (curNode == null)
					return null;
			} 
			else if (st instanceof IR_Break) {
				Branch br = context.getInnermostLoop();
				curNode.addChild(br.getFalseBranch());
				return null;
			}
			else if (st instanceof IR_Continue) {
				Branch br = context.getInnermostLoop();
				curNode.addChild(br);
				return null;
			}
			else if (st instanceof IR_Return) {
				// This is a return.
				Expression returnExpr = generateExpr(((IR_Return) st).getExpr(), context);
				END end = new END(returnExpr);	
				curNode.addChild(end);
				end.addParent(curNode);
				curNode = end;
				// we skip all nodes after the "return" statement, as they will never be reached.
				return end;
			}
			else {
				// This is a FieldDecl, Assignment, or MethodCall. They are all statements that 
				// belong in a singular Codeblock.
				Statement newStatement = generateStatement(st, context);
				if (!(curNode instanceof Codeblock)) {
					Codeblock newBlock = new Codeblock();
					curNode.addChild(newBlock);
					newBlock.addParent(curNode);
					curNode = newBlock;
				} 
				((Codeblock) curNode).addStatement(newStatement);
			}
		}
		context.decScope();
		return curNode;
	}
	
	public static FlowNode generateIf(FlowNode prevNode, IR_If ifNode, ControlflowContext context) {
		IR_Node IrExpr = ifNode.getExpr();
		Expression expr = generateExpr(IrExpr, context);
		Branch ifBranch = new Branch(expr);
		prevNode.addChild(ifBranch);
		ifBranch.addParent(prevNode);
		
		START beginTrueBranch = new START();
		ifBranch.setTrueBranch(beginTrueBranch);
		beginTrueBranch.addParent(ifBranch);
		START beginFalseBranch = new START();
		ifBranch.setFalseBranch(beginFalseBranch);
		beginFalseBranch.addParent(ifBranch);
		
		FlowNode endTrueBranch = generateFlow(beginTrueBranch, ifNode.getTrueBlock(), context);
		FlowNode endFalseBranch = generateFlow(beginFalseBranch, ifNode.getFalseBlock(), context);
		
		NoOp exitIf = new NoOp();
		if ((endTrueBranch == null || endTrueBranch instanceof END) && 
				(endFalseBranch == null || endFalseBranch instanceof END))
			return null;
		if (endTrueBranch != null && !(endTrueBranch instanceof END)) {
			exitIf.addParent(endTrueBranch);
			endTrueBranch.addChild(exitIf);
		}
		if (endFalseBranch != null && !(endFalseBranch instanceof END)) {
			exitIf.addParent(endFalseBranch);
			endFalseBranch.addChild(exitIf);
		}
		return exitIf;
	}
	
	public static FlowNode generateFor(FlowNode prevNode, IR_For forNode, ControlflowContext context) {
		// generate the compare IR node for the ending condition.
		IR_CompareOp comp = new IR_CompareOp(forNode.getVar(), forNode.getEnd(), Ops.LT);
		Expression expr = generateExpr(comp, context);
		Branch forBranch = new Branch(expr);
		context.enterLoop(forBranch);
		prevNode.addChild(forBranch);
		forBranch.addParent(prevNode);
		
		// Flow node when expr evaluates to false.
		NoOp exitFor = new NoOp();
		exitFor.addParent(forBranch);
		forBranch.setFalseBranch(exitFor);
		
		START beginForBlock = new START();
		forBranch.setTrueBranch(beginForBlock);
		beginForBlock.addParent(forBranch);
		FlowNode endFor = generateFlow(beginForBlock, forNode.getBlock(), context);
		if (!(endFor instanceof END) && endFor != null ) {
			// Previous flow block did not end in return, continue, or break. We return to branch cond.
			endFor.addChild(forBranch);
		}
		context.exitLoop();
		return exitFor;
	}

	public static FlowNode generateWhile(FlowNode prevNode, IR_While whileNode, ControlflowContext context) {
		IR_Node IrExpr = whileNode.getExpr();
		Expression expr = generateExpr(IrExpr, context);
		Branch whileBranch = new Branch(expr);
		context.enterLoop(whileBranch);
		prevNode.addChild(whileBranch);
		whileBranch.addParent(prevNode);
		
		// Flow node when expr evaluates to false.
		NoOp noOp = new NoOp();
		noOp.addParent(whileBranch);
		whileBranch.setFalseBranch(noOp);
		
		START beginWhileBlock = new START();
		whileBranch.setTrueBranch(beginWhileBlock);
		beginWhileBlock.addParent(whileBranch);
		FlowNode endWhile = generateFlow(beginWhileBlock, whileNode.getBlock(), context);
		endWhile.addChild(whileBranch);
		if (!(endWhile instanceof END) && endWhile != null ) {
			// Previous flow block did not end in return, continue, or break. We return to branch cond.
			endWhile.addChild(whileBranch);
		}
		context.exitLoop();
		return noOp;
	}
	
	public static Expression generateExpr(IR_Node node, ControlflowContext context) {
		if (node == null)
			return null;
		Expression expr = null;
		if (node instanceof IR_Call) {
			IR_Call callNode = (IR_Call) node;
			expr = generateMethodCall(callNode, context);
		}
		// Why did we reduce the detail of the binary ops? I think we should still differentiate b/w them.
		else if (node instanceof IR_ArithOp) {
			IR_ArithOp arith = (IR_ArithOp) node;
			expr = new BinExpr(generateExpr(arith.getLeft(), context), arith.getOp(), 
					generateExpr(arith.getRight(), context));
		} 
		else if (node instanceof IR_CompareOp) {
			IR_CompareOp compare = (IR_CompareOp) node;
			expr = new BinExpr(generateExpr(compare.getLeft(), context), compare.getOp(), 
					generateExpr(compare.getRight(), context));
		} 
		else if (node instanceof IR_CondOp) {
			IR_CondOp cond = (IR_CondOp) node;
			expr = new BinExpr(generateExpr(cond.getLeft(), context), cond.getOp(), 
					generateExpr(cond.getRight(), context));
		}
		else if (node instanceof IR_EqOp){
			IR_EqOp eq = (IR_EqOp) node; //There's no real difference between CondOp and EqOp except operators
			expr = new BinExpr(generateExpr(eq.getLeft(), context), eq.getOp(), 
					generateExpr(eq.getRight(), context));
		}
		else if (node instanceof IR_Negate) {
			// TODO: Implement
			IR_Negate negation = (IR_Negate) node;
			Expression negExpr = generateExpr(negation.getExpr(), context);
		}
		else if (node instanceof IR_Not) {
			// TODO: Implement
			IR_Not not = (IR_Not) node;
		}
		else if (node instanceof IR_Ternary) {
			// TODO: Implement
			IR_Ternary ternary = (IR_Ternary) node;
		}
		else if (node instanceof IR_Var) {
			IR_Var var = (IR_Var) node;
			Descriptor d = context.findSymbol(var.getName());
			expr = new Var(d);
		}
		else if (node instanceof IR_Literal) {
			IR_Literal literal = (IR_Literal) node;
			if (literal instanceof IR_BoolLiteral)
				expr = new BoolLit(((IR_BoolLiteral) literal).getValue());
			else if (literal instanceof IR_IntLiteral)
				expr = new IntLit(((IR_IntLiteral) literal).getValue());
			else if (literal instanceof IR_StringLiteral)
				// TODO: Implement string literals
				;
		}
		else {
			System.err.println("Unexpected Node type passed to generateExpr: " + node.getClass().getSimpleName());
			System.err.println("The node passed in was of type " + node.getType().toString());
		}
		return expr;
	}
	
	/**
	 * Generate statement for FieldDecl, Assignment, and MethodCall.
	 * @param node : IR Node representing a statement
	 * @param context : ControlflowContext object
	 * @return Statement object for control flow
	 */
	public static Statement generateStatement(IR_Node node, ControlflowContext context) {
		if (node == null)
			return null;
		Statement st = null;
		if (node instanceof IR_FieldDecl) {
			IR_FieldDecl fieldDecl = (IR_FieldDecl) node;
			Descriptor d = new Descriptor(node);
			context.putSymbol(fieldDecl.getName(), d);
			st = new Declaration((IR_FieldDecl) node);
		} 
		else if (node instanceof IR_Assign) {
			IR_Assign assignNode = (IR_Assign) node;
			Descriptor d = context.findSymbol(assignNode.getLhs().getName());
			Var loc = new Var(d);
			Expression expr = generateExpr(assignNode.getRhs(), context);
			st = new Assignment(loc, assignNode.getOp(), expr);
		} 
		else if (node instanceof IR_Call) {
			IR_Call callNode = (IR_Call) node;
			st = new MethodCallStatement(generateMethodCall(callNode, context));
		}
		return st;	
	}
	
	/**
	 * Given a IR_Call node, convert to a MethodCall Statement node.
	 * @param callNode : IR_Call node
	 * @param context : ControlflowContext object
	 * @return MethodCall Statement node
	 */
	public static MethodCall generateMethodCall(IR_Call callNode, ControlflowContext context) {
		List<Expression> exprs = new ArrayList<Expression>();
		List<IR_Node> args = callNode.getArgs();
		for (int i=0; i<args.size(); i++) {
			exprs.add(generateExpr(args.get(i), context));
		}
		return new MethodCall(callNode.getName(), exprs);
	}
	
}
