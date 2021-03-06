package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.mit.compilers.codegen.CodegenConst;
import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.codegen.LocLabel;
import edu.mit.compilers.codegen.LocStack;
import edu.mit.compilers.controlflow.Branch.BranchType;
import edu.mit.compilers.ir.IR_ArithOp;
import edu.mit.compilers.ir.IR_Assign;
import edu.mit.compilers.ir.IR_Break;
import edu.mit.compilers.ir.IR_Call;
import edu.mit.compilers.ir.IR_CompareOp;
import edu.mit.compilers.ir.IR_CondOp;
import edu.mit.compilers.ir.IR_Continue;
import edu.mit.compilers.ir.IR_EqOp;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_For;
import edu.mit.compilers.ir.IR_If;
import edu.mit.compilers.ir.IR_Literal;
import edu.mit.compilers.ir.IR_Literal.IR_BoolLiteral;
import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;
import edu.mit.compilers.ir.IR_Literal.IR_StringLiteral;
import edu.mit.compilers.ir.IR_MethodDecl;
import edu.mit.compilers.ir.IR_Negate;
import edu.mit.compilers.ir.IR_Node;
import edu.mit.compilers.ir.IR_Not;
import edu.mit.compilers.ir.IR_Return;
import edu.mit.compilers.ir.IR_Seq;
import edu.mit.compilers.ir.IR_Ternary;
import edu.mit.compilers.ir.IR_Var;
import edu.mit.compilers.ir.IR_While;
import edu.mit.compilers.ir.Ops;
import edu.mit.compilers.ir.Type;

/**
 * Class that is responsible for the conversion of high-level IR to low-level IR.
 * @author yygu
 *
 */
public class GenerateFlow {
	
	/**
	 * Generate control flow nodes given the root node of IR (representing the 
	 * beginning of the program).
	 * 
	 * @param node: root IR node, containing sequence of methods, callouts, and global field declarations
	 * @param context : ControlflowContext object
	 * @param callouts : list of IR_MethodDecl objects containing callouts
	 * @param globals : list of IR_FieldDecl objects containing global declarations
	 * @param flowNodes : HashMap of method names to FlowNodes
	 */
	public static void generateProgram(IR_Node root, ControlflowContext context, 
		List<IR_MethodDecl> callouts, List<IR_FieldDecl> globals, Map<String, START> flowNodes) {

		IR_Seq seq = (IR_Seq) root;
		List<IR_Node> statements = seq.getStatements();
		for (int i = 0; i < statements.size(); i++) {
			IR_Node node = statements.get(i);
			if (node.getType() == Type.METHOD) {
				// method declaration
				START start = generateMethodDecl(node, context);
				String name = ((IR_MethodDecl) node).getName();
				flowNodes.put(name, start);
			} else if (node.getType() == Type.CALLOUT) {
				// callout
				callouts.add((IR_MethodDecl) node);
			} else if (node instanceof IR_FieldDecl) {
				// global field declaration
				IR_FieldDecl decl = (IR_FieldDecl) node;
				Descriptor d = new Descriptor(decl);
				d.setLocation(new LocLabel(decl.getName()));
				context.putSymbol(decl.getName(), d);
				globals.add(decl);
			} else {
				System.err.println("Unrecognized type");
			}
		}
	}
	/**
	 * Convert a method declaration IR_Node to a FlowNode.
	 * @param node : IR_Node that represents the method declaration
	 * @param context : ControlflowContext object
	 * @return start : START FlowNode object that signals the beginning of a method.
	 */
	public static START generateMethodDecl(IR_Node node, ControlflowContext context) {
		IR_MethodDecl decl = (IR_MethodDecl) node;
		List<IR_FieldDecl> args = decl.args;
		Descriptor d = new Descriptor(node);
		context.putSymbol(decl.name, d);
		context.enterFun();
		context.incScope(null);
		
		for (int i = 0; i < args.size(); i++) {
			IR_FieldDecl a = args.get(i);
			Descriptor argd = new Descriptor(a);
			context.putSymbol(a.getName(), argd);
		}
		
		START start = new START(args, decl.getRetType());
		FlowNode returnNode = generateFlow(start, decl.body, context);
		if (!(returnNode instanceof END) && (returnNode != null)) {
			// No specified return value --> return void
			END end = new END();
			returnNode.addChild(end);
			end.addParent(returnNode);
		} else if (decl.body.getStatements().size() == 0) {
		    // method has an empty body
		    END end = new END();
		    start.addChild(end);
		    end.addParent(start);
		}
		context.decScopeWithSideEffects();
		return start;	
	}
	
	/**
	 * Main function that generates the control flow of a code block.
	 * @param prevNode : parent FlowNode of the current code block.
	 * @param seq : Code block sequence of IR_Nodes that will be converted to FlowNodes.
	 * @param context : ControlflowContext object.
	 * @return curNode : the last FlowNode in the code block.
	 */
	public static FlowNode generateFlow(FlowNode prevNode, IR_Seq seq, ControlflowContext context) {
		if (seq == null)
			return null;
		context.incScope(null);
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
				// A Branch object always generates a new FlowNode. So we must replace curNode.
				curNode = branchEnd;
				if (branchEnd == null)
					// only occurs when 'if' statement returns, so program will never reach 
					// anything afterwards. We end the processing here.
					break;
			} 
			else if (st instanceof IR_Break) {
				// This is a break. We set the pointer to the exit node  (FalseBranch) of the 
				// innermost for/while loop.
				Branch br = context.getInnermostLoop();
                if (!(curNode instanceof Codeblock)) {
                    // Build new Codeblock if one doesn't exist already.
                    Codeblock newBlock = new Codeblock();
                    curNode.addChild(newBlock);
                    newBlock.addParent(curNode);
                    curNode = newBlock;
                }
                br.getFalseBranch().addParent(curNode);
				curNode.addChild(br.getFalseBranch());
				((Codeblock) curNode).setIsBreak(true);
				curNode = null;
				break;
			}
			else if (st instanceof IR_Continue) {
				// This is a continue. We set the pointer to the Branch object of the innermost 
				// for/while loop.
				Branch br = context.getInnermostLoop();
                if (!(curNode instanceof Codeblock)) {
                    // Build new Codeblock if one doesn't exist already.
                    Codeblock newBlock = new Codeblock();
                    curNode.addChild(newBlock);
                    newBlock.addParent(curNode);
                    curNode = newBlock;
                } 
				curNode.addChild(br);
				br.addParent(curNode);
				((Codeblock) curNode).setIsBreak(true);
				curNode = null;
				continue;
			}
			else if (st instanceof IR_Return) {
				// This is a return. We return a END FlowNode with the return expression.
				Expression returnExpr = generateExpr(((IR_Return) st).getExpr(), context);
				END end = new END(returnExpr);	
				curNode.addChild(end);
				end.addParent(curNode);
				curNode = end;
				// we skip all nodes after the "return" statement, as they will never be reached.
				break;
			}
			else {
				// This is a FieldDecl, Assignment, or MethodCall. They are all statements that 
				// belong in a singular Codeblock object.
				Statement newStatement = generateStatement(st, context);
				if (!(curNode instanceof Codeblock)) {
					// Build new Codeblock if one doesn't exist already.
					Codeblock newBlock = new Codeblock();
					curNode.addChild(newBlock);
					newBlock.addParent(curNode);
					curNode = newBlock;
				} 
				((Codeblock) curNode).addStatement(newStatement);
			}
		}
		context.decScopeWithSideEffects();
		return curNode;
	}
	
	/**
	 * Generate an Branch FlowNode representing 'if' from an IR_If node.
	 * @param prevNode : parent FlowNode of the current code block.
	 * @param ifNode : IR_Node representing the 'if' statement.
	 * @param context : ControlflowContext object.
	 * @return exitIf : null if program will not continue afterwards, a NoOp FlowNode representing 
	 * the end of the 'if' Branch otherwise.
	 */
	public static FlowNode generateIf(FlowNode prevNode, IR_If ifNode, ControlflowContext context) {
		// First initialize Branch object with the condition expression.
		Expression expr = generateExpr(ifNode.getExpr(), context);
		Branch ifBranch = new Branch(expr, BranchType.IF);
		prevNode.addChild(ifBranch);
		ifBranch.addParent(prevNode);
		
		// Begin recursively generating true and false blocks.
		START beginTrueBranch = new START();
		ifBranch.setTrueBranch(beginTrueBranch);
		beginTrueBranch.addParent(ifBranch);
		START beginFalseBranch = new START();
		ifBranch.setFalseBranch(beginFalseBranch);
		beginFalseBranch.addParent(ifBranch);
		NoOp exitIf = new NoOp();
		
		if (ifNode.getTrueBlock().getStatements().size() == 0) {
		    beginTrueBranch.addChild(exitIf);
		    exitIf.addParent(beginTrueBranch);
		} else {
		    FlowNode endTrueBranch = generateFlow(beginTrueBranch, ifNode.getTrueBlock(), context);
		    if (endTrueBranch != null && !(endTrueBranch instanceof END)) {
	            exitIf.addParent(endTrueBranch);
	            endTrueBranch.addChild(exitIf);
	        }
		}
		
		if (ifNode.getFalseBlock() == null || ifNode.getFalseBlock().getStatements().size() == 0) {
		    beginFalseBranch.addChild(exitIf);
		    exitIf.addParent(beginFalseBranch);
		} else {
		    FlowNode endFalseBranch = generateFlow(beginFalseBranch, ifNode.getFalseBlock(), context);
            if (endFalseBranch != null && !(endFalseBranch instanceof END)) {
                exitIf.addParent(endFalseBranch);
                endFalseBranch.addChild(exitIf);
            }
		}
		
		
		if (exitIf.getParents().size() == 0) {
			// Program will not run beyond the 'if' branch, so we return null.
			// For example:
			// if (x)
			//    continue;
			// else
			//    return false;
			// [code here will never be reached]
			return null;
		}
		
		return exitIf;
	}
	
	/**
	 * Generate an Branch FlowNode representing 'for' from an IR_for node.
	 * 
	 * Note: generateFor never returns null because it is always possible for the program to 
	 * continue after the 'for' block (e.g. if the cond expr is false).
	 * @param prevNode : parent FlowNode of the current code block.
	 * @param forNode : IR_Node representing the 'for' statement.
	 * @param context : ControlflowContext object.
	 * @return exitIf : a NoOp FlowNode representing the end of the 'for' Branch (when the 
	 * condition returns false).
	 */
	public static FlowNode generateFor(FlowNode prevNode, IR_For forNode, ControlflowContext context) {
		// generate the compare IR node for the ending condition.
		IR_CompareOp comp = new IR_CompareOp(forNode.getVar(), forNode.getEnd(), Ops.LT);
		// Initialize Branch object with the condition expression.
		Expression expr = generateExpr(comp, context);
		Branch forBranch = new Branch(expr, BranchType.FOR);
		Var loopVar = new Var(context.findSymbol(forNode.getVar().getName()), null);
		AddExpr incrementLoopVar = new AddExpr(loopVar, Ops.PLUS, new IntLit(1L));
		Statement increment = new Assignment(loopVar, Ops.ASSIGN, incrementLoopVar);
		context.enterLoop(forBranch);
		
		// initialize loopvar to start
		Assignment initialize = new Assignment(loopVar, Ops.ASSIGN, generateExpr(forNode.getStart(), context));
		Codeblock initializer = new Codeblock();
		initializer.addParent(prevNode);
		prevNode.addChild(initializer);
		initializer.addStatement(initialize);
		forBranch.addParent(initializer);
		initializer.addChild(forBranch);
		
		// Initiate loop block, when expr evaluates to true.
		START beginForBlock = new START();
		beginForBlock.addParent(forBranch);
		forBranch.setTrueBranch(beginForBlock);
		
		// Initiates exit block, when expr evaluates to false.
		NoOp exitFor = new NoOp();
		exitFor.addParent(forBranch);
		forBranch.setFalseBranch(exitFor);
			
		// Begin recursively generating loop code block.
		FlowNode endFor = generateFlow(beginForBlock, forNode.getBlock(), context);
		if (!(endFor instanceof END) && endFor != null ) {
			// Previous flow block did not end in return, continue, or break. We return to branch cond.
		    if (!(endFor instanceof Codeblock)) {
                if (!(endFor instanceof NoOp)) {
                    throw new RuntimeException("Maddie is wrong about things");
                }
                Codeblock newBlock = new Codeblock();
                endFor.addChild(newBlock);
                newBlock.addParent(endFor);
                endFor = newBlock;
            }
			endFor.addChild(forBranch);
			forBranch.addParent(endFor);
			((Codeblock) endFor).setIsBreak(true);
		}
		// increment loop counter on all paths returning to beginning.
		for (FlowNode node : forBranch.getParents()) {
		    if (node instanceof Codeblock) {
		        Codeblock blk = (Codeblock) node;
		        if (blk.getIsBreak()) {
		            blk.addStatement(increment);
		        }
		    }
		}
		context.exitLoop();
		return exitFor;
	}

	/**
	 * Generate an Branch FlowNode representing 'while' from an IR_while node.
	 * 
	 * Note: generateWhile never returns null because it is always possible for the program to 
	 * continue after the 'while' block (e.g. if the cond expr is false).
	 * @param prevNode : parent FlowNode of the current code block.
	 * @param forNode : IR_Node representing the 'while' statement.
	 * @param context : ControlflowContext object.
	 * @return exitIf : a NoOp FlowNode representing the end of the 'while' Branch (when the 
	 * condition returns false).
	 */
	public static FlowNode generateWhile(FlowNode prevNode, IR_While whileNode, ControlflowContext context) {
		// It's probably easier to implement MaxLoops in codegen rather than here.
	    // From Maddie: It's impossible to implement in codegen with the
	    // current structure, since I don't have access to the maxLoops
		IR_FieldDecl loopCounter;
		Declaration loop = null;
		IR_Var loopVar;
		Statement assignNode = null;
		Statement incrNode = null;
		IR_CompareOp checkMaxLoops = null;
		Branch maxCheck = null;
		if (whileNode.getMaxLoops() != null) {
			// Generate loop counter variable to count loops.
		    if (!context.symbol.getTable(context.symbol.getNumScopes() - 1).containsKey("while")) {
			    loopCounter = new IR_FieldDecl(Type.INT, "while");
                loop = generateFieldDecl(loopCounter, context);
		    }
			loopVar = new IR_Var(Type.INT, "while", null);
			
			IR_Assign assign = new IR_Assign(loopVar, new IR_IntLiteral(-1L), Ops.ASSIGN);
			assignNode = generateStatement(assign, context);
			
			IR_Assign increment = new IR_Assign(loopVar, new IR_IntLiteral(1L), Ops.ASSIGN_PLUS);
			incrNode = generateStatement(increment, context);
			
			checkMaxLoops = new IR_CompareOp(loopVar, whileNode.getMaxLoops(), Ops.LT);
			maxCheck = new Branch(generateExpr(checkMaxLoops, context), BranchType.WHILE);
			maxCheck.setIsLimitedWhile(true);
		}
		
		// If while has maxLoops, initialize to zero
		if (!(prevNode instanceof Codeblock)) {
		    Codeblock initializer = new Codeblock();
		    initializer.addParent(prevNode);
		    prevNode.addChild(initializer);
		    prevNode = initializer;
		}
		
		if (whileNode.getMaxLoops() != null) {
		    // safe because of preceding if
		    // if no while loops have occurred at this level
		    if (context.symbol.getTable(context.symbol.getNumScopes() - 1).containsKey("while")) {
		        ((Codeblock) prevNode).addStatement(loop);
		    }
		    ((Codeblock) prevNode).addStatement(assignNode);
		}
		
		// Initialize Branch object with the condition expression.
		Expression expr = generateExpr(whileNode.getExpr(), context);
		Branch whileBranch = new Branch(expr, BranchType.WHILE);
		context.enterLoop(whileBranch);
		prevNode.addChild(whileBranch);
		whileBranch.addParent(prevNode);
		
		// Initiate loop block, when expr evaluates to true.
		START beginWhileBlock = new START();
		beginWhileBlock.addParent(whileBranch);
		whileBranch.setTrueBranch(beginWhileBlock);
		
				
		// Initiates exit block, when expr evaluates to false.
		NoOp exitWhile = new NoOp();
		exitWhile.addParent(whileBranch);
		whileBranch.setFalseBranch(exitWhile);
		
		// Increment counter and check for maxLoops, if has maxLoops
		if (whileNode.getMaxLoops() != null) {
		    whileBranch.setIsLimitedWhile(true);
		    Codeblock next = new Codeblock();
		    next.addStatement(incrNode);
		    next.addParent(beginWhileBlock);
		    beginWhileBlock.addChild(next);
		    next.addChild(maxCheck);
		    maxCheck.addParent(next);
		    maxCheck.setFalseBranch(exitWhile);
		    exitWhile.addParent(maxCheck);
		    START beginInnerWhile = new START();
		    beginInnerWhile.addParent(maxCheck);
		    maxCheck.setTrueBranch(beginInnerWhile);
		    beginWhileBlock = beginInnerWhile;
		}
					
		// Begin recursively generating loop code block.
		FlowNode endWhile = generateFlow(beginWhileBlock, whileNode.getBlock(), context);
		if (!(endWhile instanceof END) && endWhile != null ) {
		    if (!(endWhile instanceof Codeblock)) {
                if (!(endWhile instanceof NoOp)) {
                    throw new RuntimeException("Maddie is wrong about things");
                }
                Codeblock newBlock = new Codeblock();
                endWhile.addChild(newBlock);
                newBlock.addParent(endWhile);
                endWhile = newBlock;
            }
			// Previous flow block did not end in return, continue, or break. We return to branch cond.
			endWhile.addChild(whileBranch);
			whileBranch.addParent(endWhile);
            ((Codeblock) endWhile).setIsBreak(true);
		}
		context.exitLoop();
		return exitWhile;
	}
	
	/**
	 * Generate an Expression FlowNode from an IR_Node.
	 * @param node : IR_Node representing an expression.
	 * @param context : ControlflowContext object.
	 * @return expr : a Expression object representing the expression.
	 */
	public static Expression generateExpr(IR_Node node, ControlflowContext context) {
		if (node == null)
			return null;
		Expression expr = null;
		if (node instanceof IR_Call) {
			IR_Call callNode = (IR_Call) node;
			expr = generateMethodCall(callNode, context);
		}
		else if (node instanceof IR_ArithOp) {
			IR_ArithOp arith = (IR_ArithOp) node;
			Ops op = arith.getOp();
			Expression left = generateExpr(arith.getLeft(), context);
			Expression right = generateExpr(arith.getRight(), context);
			switch (op) {
				case PLUS:
				case MINUS:
					expr = new AddExpr(left, op, right);
					break;
				case TIMES:
				    expr = new MultExpr(left, op, right);
                    break;
				case DIVIDE:
				    expr = new DivExpr(left, op, right);
                    break;
				case MOD:
					expr = new ModExpr(left, op, right);
					break;
				default:
					System.err.println("Should not reach here!");
					break;		
			}
		} 
		else if (node instanceof IR_CompareOp) {
			IR_CompareOp compare = (IR_CompareOp) node;
			expr = new CompExpr(generateExpr(compare.getLeft(), context), compare.getOp(), 
					generateExpr(compare.getRight(), context));
		} 
		else if (node instanceof IR_CondOp) {
			IR_CondOp cond = (IR_CondOp) node;
			expr = new CondExpr(generateExpr(cond.getLeft(), context), cond.getOp(), 
					generateExpr(cond.getRight(), context));
		}
		else if (node instanceof IR_EqOp){
			IR_EqOp eq = (IR_EqOp) node;
			expr = new EqExpr(generateExpr(eq.getLeft(), context), eq.getOp(), 
					generateExpr(eq.getRight(), context));
		}
		else if (node instanceof IR_Negate) {
			IR_Negate negation = (IR_Negate) node;
			expr = new NegateExpr(generateExpr(negation.getExpr(), context));
		}
		else if (node instanceof IR_Not) {
			IR_Not not = (IR_Not) node;
			expr = new NotExpr(generateExpr(not.getExpr(), context));
		}
		else if (node instanceof IR_Ternary) {
			IR_Ternary ternary = (IR_Ternary) node;
			expr = new Ternary(generateExpr(ternary.getCondition(), context), 
					generateExpr(ternary.getTrueExpr(), context), 
					generateExpr(ternary.getFalseExpr(), context));
		}
		else if (node instanceof IR_Var) {
			IR_Var var = (IR_Var) node;
			Descriptor d = context.findSymbol(var.getName());
			expr = new Var(d, generateExpr(var.getIndex(), context));
		}
		else if (node instanceof IR_Literal) {
			IR_Literal literal = (IR_Literal) node;
			if (literal instanceof IR_BoolLiteral)
				expr = new BoolLit(((IR_BoolLiteral) literal).getValue());
			else if (literal instanceof IR_IntLiteral)
				expr = new IntLit(((IR_IntLiteral) literal).getValue());
			else if (literal instanceof IR_StringLiteral)
				expr = new StringLit(((IR_StringLiteral) literal).getValue());
		}
		else {
			System.err.println("Unexpected Node type passed to generateExpr: " + node.getClass().getSimpleName());
			System.err.println("The node passed in was of type " + node.getType().toString());
		}
		return expr;
	}
	
	/**
	 * Generate statement for FieldDecl, Assignment, and MethodCall.
	 * @param node : IR Node representing a statement.
	 * @param context : ControlflowContext object.
	 * @return Statement object for control flow.
	 */
	public static Statement generateStatement(IR_Node node, ControlflowContext context) {
		if (node == null)
			return null;
		Statement st = null;
		if (node instanceof IR_FieldDecl) {
			IR_FieldDecl fieldDecl = (IR_FieldDecl) node;
			st = generateFieldDecl(fieldDecl, context);
		} 
		else if (node instanceof IR_Assign) {
			IR_Assign assignNode = (IR_Assign) node;
			Descriptor d = context.findSymbol(assignNode.getLhs().getName());
			Expression ind = generateExpr(assignNode.getLhs().getIndex(), context);
			Var loc = new Var(d, ind);
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
	 * Generate field declaration from IR_Node, updating the descriptor as necessary.
	 * @param node : IR Node representing a field declaration.
	 * @param context : ControlflowContext object.
	 * @return Declaration object representing a field declaration.
	 */
	public static Declaration generateFieldDecl(IR_FieldDecl node, ControlflowContext context) {
		String name = node.getName();
		Descriptor d = new Descriptor(node);
		Type type = node.getType();
		long size = CodegenConst.INT_SIZE;
		switch (type) {
			case INTARR:
			case BOOLARR:
				size = node.getLength().getValue() * CodegenConst.INT_SIZE;
				break;
			default:
			    break;
		}
		LocStack loc = context.allocLocal(size);
		d.setLocation(loc);
		context.putSymbol(name, d);
		return new Declaration(node);
		
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
		boolean isCallout = callNode.getDecl().getType() == Type.CALLOUT;
		return new MethodCall(callNode.getName(), exprs, isCallout);
	}
	
}
