package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rits.cloning.Cloner;

import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.controlflow.Expression.ExpressionType;
import edu.mit.compilers.controlflow.Statement.StatementType;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;
import edu.mit.compilers.ir.Ops;

//Code to get OS-independent newlines: System.getProperty("line.separator");

/**
 * This class represents an object we can create to call our optimization methods. 
 * @author DeJuan
 *
 */
public class Optimizer {
	private ControlflowContext context;
	private List<IR_MethodDecl> calloutList;
	private List<IR_FieldDecl> globalList;
	private HashMap<String, START> flowNodes;
	Cloner cloner = new Cloner();

	private int tempCounter = 0;
	/**
	 * This is an constructor for optimizer. Once optimizations are done, it will call generateProgram with these parameters.  
	 *  
	 */
	public Optimizer(ControlflowContext context, 
		List<IR_MethodDecl> callouts, List<IR_FieldDecl> globals, HashMap<String, START> flowNodes){
		this.context = context;
		this.calloutList = callouts;
		this.globalList = globals;
		this.flowNodes = flowNodes;
	}

	/*
	public List<Expression> simpleAlgebraicSimplifier(List<Expression> exprList){
		List<Expression> simplifiedExprs = new ArrayList<Expression>();
		for (Expression expr : exprList){
			switch(expr.getExprType()){
				case BIN_EXPR:
				BinExpr bin = (BinExpr)expr;
				switch(bin.getOperator()){
					case TIMES:
						if(bin.getRightSide().getExprType() == ExpressionType.INT_LIT){
							IntLit rhs = (IntLit)bin.getRightSide();
							if(rhs.getValue() == 2){
								simplifiedExprs.add(new AddExpr(bin.getLeftSide(), Ops.PLUS, bin.getLeftSide()));
							}
						}
				}

			default:
				break;
			}
		}
	}
	 */



	/**
	 * For handling commutativity. Use sets; write a class, SPSet, that is a class which has three fields. These fields are a Set<SPSet>, an Op, and a Set<IR_FieldDecl>.
	 * SPSet is used for any other set that doesn't contain IR_FieldDecls. So we can recursively store sets that are commutative.
	 * Write new methods for compute Mul, Div, Mod, Set, And, Or
	 * When you go to compute the set of commutativity for something, say, A + B + X + Y, do the following.
	 * The tree structure for the above looks like:
	 * 
	 *                  +           Level 1
	 *                /   \
	 *               +     +        Level 2
	 *              / \   / \ 
	 *             A   B X   Y      Level 3
	 *               
	 *   So call computePlusSets on it. computePlusSets locally holds a Set<SPSet>, call it rootSP.
	 *   It will return this SP<Set> when it is done.
	 *   This would create an SPSet with the Level 2 expressions inside of it.
	 *   Since both Lv. 2 expression are plus, we make a recursive call:
	 *    rootSP.SPSets.addAll(computePlusSets(Level 2 LHS))
	 *    rootSP.SPSets.addAll(computePlusSets(Level 2 RHS))
	 *    
	 *    In that recursive call, we need to hit a check for whether or not the LHS and RHS are Vars.
	 *    If they are, then do:
	 *    rootSP.VarSets.add(LHS.getDescriptor); //The descriptors are IR_FieldDecls 
	 *    rootSP.VarSets.add(RHS.getDescriptor);
	 *    
	 *    What if the tree is uneven? EX: X + Y + Z
	 *    
	 *                  +           Level 1
	 *                /   \
	 *               X     +        Level 2
	 *                    / \ 
	 *                   Y   Z      Level 3
	 *                   
	 *   Then at Lv. 1, we check LHS for VarSet. it is, so:
	 *   rootSP.VarSets.add(LHS.getDescriptor); 
	 *   
	 *   However, the RHS is an SPSet, so we fall back to the above behavior, since it is a plus.
	 *   rootSP.SPSets.addAll(computePlusSets(Level 2 RHS));
	 *   
	 *   If it were a multiply, we would do:
	 *   rootSP.SPSets.add(computeMultiplySets(Level 2 RHS));
	 *   
	 *   and so on.
	 */        


	/**
	 * This method takes in a FlowNode and allows you to check all variables assigned within the FlowNode.
	 * @param node : FlowNode that you wish to investigate
	 * @return assignedVars : List<Var> containing all variables that are assigned in this Node. Guaranteed empty if the node is not a Codeblock! 
	 */
	public List<Var> checkVariablesAssigned(FlowNode node){
		List<Var> assignedVars = new ArrayList<Var>();
		if (node instanceof Codeblock){
			Codeblock codeblock = (Codeblock)node;
			List<Statement> statementList = codeblock.getStatements();
			for (Statement currentStatement : statementList){
				if (currentStatement.getStatementType() == StatementType.ASSIGNMENT){
					Assignment currentAssignment = (Assignment)currentStatement;
					assignedVars.add(currentAssignment.getDestVar());
				}
			}
		}
		return assignedVars;
	}

	/**
	 * This method allows you to get the IR_FieldDecls representing the variables in a given Expression. It recursively searches the expression until it finds just
	 * the variables, and gets their IR. 
	 * 
	 * @param expr : The expression whose variables you want to isolate
	 * @return List<IR_FieldDecl> : List of the descriptors for all variables in the given expression
	 */
	public List<IR_FieldDecl> getVarIRsFromExpression(Expression expr){
		List<IR_FieldDecl> allVars = new ArrayList<IR_FieldDecl>();
		if(expr instanceof BinExpr){
			BinExpr bin = (BinExpr)expr;
			Expression lhs = bin.getLeftSide();
			Expression rhs = bin.getRightSide();
			allVars.addAll(getVarIRsFromExpression(lhs));
			allVars.addAll(getVarIRsFromExpression(rhs));
		}
		else if (expr instanceof Var){
			Var varia = (Var)expr;
			allVars.add((IR_FieldDecl)varia.getVarDescriptor().getIR());
		}	
		else if(expr instanceof NotExpr){
			NotExpr nope = (NotExpr)expr;
			allVars.addAll(getVarIRsFromExpression(nope.getUnresolvedExpression()));
		}
		else if(expr instanceof NegateExpr){
			NegateExpr negate = (NegateExpr)expr;
			allVars.addAll(getVarIRsFromExpression(negate.getExpression()));
		}
		else if(expr instanceof Ternary){
			Ternary tern = (Ternary)expr;
			allVars.addAll(getVarIRsFromExpression(tern.getTernaryCondition()));
			allVars.addAll(getVarIRsFromExpression(tern.trueBranch));
			allVars.addAll(getVarIRsFromExpression(tern.falseBranch));
		}
		else if(expr instanceof MethodCall){
			MethodCall MCHammer = (MethodCall)expr;
			for(Expression arg : MCHammer.getArguments()){
				allVars.addAll(getVarIRsFromExpression(arg));
			}
		}
		return allVars;
	}

	/**
	 * This method allows you to get the Var objects representing the variables in a given Expression. It recursively searches the expression until it finds just
	 * the variables, and gets them for you. 
	 * 
	 * @param expr : The expression whose variables you want to isolate
	 * @return List<Var> : List of the Var objects for all variables in the given expression
	 */
	public List<Var> getVarsFromExpression(Expression expr){
		List<Var> allVars = new ArrayList<Var>();
		if(expr instanceof BinExpr){
			BinExpr bin = (BinExpr)expr;
			Expression lhs = bin.getLeftSide();
			Expression rhs = bin.getRightSide();
			allVars.addAll(getVarsFromExpression(lhs));
			allVars.addAll(getVarsFromExpression(rhs));
		}
		else if (expr instanceof Var){
			Var varia = (Var)expr;
			allVars.add(varia);
		}	
		else if(expr instanceof NotExpr){
			NotExpr nope = (NotExpr)expr;
			allVars.addAll(getVarsFromExpression(nope.getUnresolvedExpression()));
		}
		else if(expr instanceof NegateExpr){
			NegateExpr negate = (NegateExpr)expr;
			allVars.addAll(getVarsFromExpression(negate.getExpression()));
		}
		else if(expr instanceof Ternary){
			Ternary tern = (Ternary)expr;
			allVars.addAll(getVarsFromExpression(tern.getTernaryCondition()));
			allVars.addAll(getVarsFromExpression(tern.trueBranch));
			allVars.addAll(getVarsFromExpression(tern.falseBranch));
		}
		else if(expr instanceof MethodCall){
			MethodCall MCHammer = (MethodCall)expr;
			for(Expression arg : MCHammer.getArguments()){
				allVars.addAll(getVarsFromExpression(arg));
			}
		}
		return allVars;
	}

	
	
	/**
	 * This is a method to get all expressions from a given FlowNode. 
	 * If the node is a Codeblock, it is scanned for assignments. 
	 * For all assignments, the right hand side, which is an expression, is recorded in a list.
	 * At the end of the block, this list is returned. 
	 * 
	 * If the node is a Branch, the branch condition, which is itself an expression, is added to the list.
	 * The list is then returned.
	 * 
	 * Note that this does not get all expressions for an entire graph, but only one node. 
	 * 
	 * @param node : A single FlowNode whose expressions you wish to discover.
	 * @return allExprs : A List<Expression> of all expressions found within the single FlowNode given.
	 */
	public Set<Expression> getAllExpressions(FlowNode node){
		Set<Expression> allExprs = new LinkedHashSet<Expression>();
		if (node instanceof Codeblock){
			Codeblock cblock = (Codeblock)node;
			List<Statement> stateList = cblock.getStatements();
			for (Statement currentStatement : stateList){
				if (currentStatement.getStatementType() == StatementType.ASSIGNMENT){
					Assignment currentAssign = (Assignment) currentStatement;
					allExprs.add(currentAssign.getValue());
				}

			}
		}
		return allExprs;
	}
	
	/**
	 * This method takes in a START node and traverses the codeflow representation that can be generated by repeatedly following all
	 * the children of the method. As it traverses the flow, it looks for all variable names, and records each one it sees.
	 * It then returns a list of all the names it found; if we make a temporary variable, it can be valid as long as it is NOT in the
	 * returned list of names that are already taken.
	 *  
	 * @param node : START node for the given method.
	 * @return names : List<String> of all names found in the method.
	 */
	public Set<String> getAllVarNamesInMethod(START node){
		Set<String> allVarNames = new LinkedHashSet<String>();
		List<FlowNode> processing = new ArrayList<FlowNode>();
		for(IR_FieldDecl global : globalList){
			allVarNames.add(global.getName());
		}
		for(IR_FieldDecl parameter : node.getArguments()){
			allVarNames.add(parameter.getName());
		}
		processing.add(node.getChildren().get(0));
		while (!processing.isEmpty()){
			FlowNode currentNode = processing.remove(0);
			currentNode.visit();
			if(currentNode instanceof Codeblock){
				Codeblock cblock = (Codeblock)currentNode;
				for(Statement state : cblock.getStatements()){
					if(state instanceof Declaration){
						Declaration decl = (Declaration)state;
						allVarNames.add(decl.getName());
					}
				}
			}
			for(FlowNode child : currentNode.getChildren()){
				if(!child.visited()){
					processing.add(child);
				}
			}
		}
		node.resetVisit();
		return allVarNames;
	}
	
	/**
	 * This is a compiler helper function which, given the set of all variable names already taken by the method,
	 * returns a new, unique name for a temp variable each time it is called. 
	 * 
	 * @param allVarNames : List of all names in the method being investigated
	 * @return String : unique name to assign to a compiler-generated temporary variable
	 */
	public String generateNextTemp(Set<String> allVarNames){
		String tempName = "temp" + tempCounter++;
		while(allVarNames.contains(tempName)){
			tempName = "temp" + tempCounter++;
		}
		return tempName;
	}
	
	
	/**
	 * This is a helper method which allows an easy way to check if a Statement is an Assignment. 
	 * @param state : The Statement whose type you wish to check
	 * @return boolean : true if the statement is an Assignment, false otherwise. 
	 */
	public boolean checkIfAssignment(Statement state){
		switch(state.getStatementType()){
		case ASSIGNMENT:
			return true;
		default:
			break;
		}
		return false;
	}
	
	/**
	 * This method allows us to actually set the variable IDs, which is a requirement for initializing an SPSet
	 * for any Expression containing those variables. It takes in a Map<IR_FieldDecl, ValueID>, varToVal, which is generated when
	 * we attempt to do CSE, and an Expression, and sets up the ValueIDs for all variables in the given expression.
	 * 
	 * @param varToVal : Mapping of IR_FieldDecl to ValueID, used to assign ValueID to a Var, given its descriptor
	 * @param expr The expression whose Vars we want to set the ValueIDs for
	 */
	public void setVarIDs(Map<IR_FieldDecl, ValueID> varToVal, Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents, Expression expr){
		if(expr instanceof BinExpr){
			BinExpr bin = (BinExpr)expr;
			Expression lhs = bin.getLeftSide();
			Expression rhs = bin.getRightSide();
			setVarIDs(varToVal, varToValForArrayComponents, lhs);
			setVarIDs(varToVal, varToValForArrayComponents, rhs);
		}
		else if (expr instanceof Var){
			Var varia = (Var)expr;
			ValueID valID = varToVal.get((IR_FieldDecl)varia.getVarDescriptor().getIR());
			if(varia.getIndex() != null){
				IR_FieldDecl varDecl = (IR_FieldDecl) varia.getVarDescriptor().getIR();
				SPSet varArraySP = new SPSet(varia.getIndex());
				valID = varToValForArrayComponents.get(varDecl).get(varArraySP);
			}
			if (valID == null){
				throw new RuntimeException("Something went wrong; tried to set a ValueID with a null mapping.");
			}
			else{
				varia.setValueID(valID);
			}
		}	
		else if(expr instanceof NotExpr){
			NotExpr nope = (NotExpr)expr;
			setVarIDs(varToVal, varToValForArrayComponents, nope.getUnresolvedExpression());
		}
		else if(expr instanceof NegateExpr){
			NegateExpr negate = (NegateExpr)expr;
			setVarIDs(varToVal, varToValForArrayComponents, negate.getExpression());
		}
		else if(expr instanceof Ternary){
			Ternary tern = (Ternary)expr;
			setVarIDs(varToVal, varToValForArrayComponents, tern.getTernaryCondition());
			setVarIDs(varToVal, varToValForArrayComponents, tern.trueBranch);
			setVarIDs(varToVal, varToValForArrayComponents, tern.falseBranch);
		}
		else if(expr instanceof MethodCall){
			MethodCall MCHammer = (MethodCall)expr;
			for(Expression arg : MCHammer.getArguments()){
				setVarIDs(varToVal, varToValForArrayComponents, arg);
			}
		}
	}

	/**
	 * This is a helper method we will use to actually make our CSE changes.
	 * In the CSE method, we'll make a new codeblock and just add statements as we go. 
	 * After every assignment, we'll be putting in compiler temporaries, etc. and modifying
	 * the block. Rather than take the time of doing an in-place modification, which carries
	 * several issues of complication but is more efficient, we'll simply swap all the pointers
	 * related to the old block to the new one, effectively replacing the old.
	 * 
	 * @param old : original Codeblock before modifications/optimizations
	 * @param newBlock : new Codeblock after modifications/optimizations that should replace old
	 */
	public void swapCodeblocks(Codeblock old, Codeblock newBlock){
		for (FlowNode oldP : old.getParents()){
			if(!(oldP instanceof Branch)){
				if(oldP instanceof Codeblock){
					Codeblock oldParent = (Codeblock)oldP;
					oldParent.removeChild(old);
					oldParent.addChild(newBlock);
					newBlock.addParent(oldParent);
				}
				else if(oldP instanceof NoOp){
					NoOp oldParent = (NoOp)oldP;
					oldParent.removeChild(old);
					oldParent.addChild(newBlock);
					newBlock.addParent(oldParent);
				}
				else if(oldP instanceof START){
					START oldParent = (START)oldP;
					oldParent.removeChild(old);
					oldParent.addChild(newBlock);
					newBlock.addParent(oldParent);
				}
				else if(oldP instanceof END){
					throw new RuntimeException("Something went wrong: a non-branch has a parent of type END." + System.getProperty("line.separator"));
				}
			}
			
			else{
				Branch oldParent = (Branch)oldP;
				if(old == oldParent.getTrueBranch()){
					oldParent.setTrueBranch(newBlock);
				}
				else{
					oldParent.setFalseBranch(newBlock);
				}
				newBlock.addParent(oldParent);
			}
		}
		
		for(FlowNode oldC: old.getChildren()){
			if(!(oldC instanceof Branch)){
				if(oldC instanceof Codeblock){
					Codeblock oldChild = (Codeblock)oldC;
					oldChild.removeParent(old);
					oldChild.addParent(newBlock);
					newBlock.addChild(oldChild);
				}
				else if (oldC instanceof NoOp){
					NoOp oldChild = (NoOp) oldC;
					oldChild.removeParent(old);
					oldChild.addParent(newBlock);
					newBlock.addChild(oldChild);
				}
				else if(oldC instanceof START){
					throw new RuntimeException("Found a non-branch with a START for a child." + System.getProperty("line.separator"));
				}
				
				else if(oldC instanceof END){
					END oldChild = (END)oldC;
					oldChild.removeParent(old);
					oldChild.addParent(newBlock);
					newBlock.addChild(oldChild);
				}
			}

			else{//mirror of the above, so check omitted
				//Is this sane..?
				Branch oldChild = (Branch)oldC;
				oldChild.removeParent(old);
				oldChild.addParent(newBlock);
				newBlock.addChild(oldChild);
			}
		}
	}
	
	/**
	 * This method is a helper for CSE; given an Assignment's left hand side and the maps that could be affected by this
	 * new assignment, it looks through each of the maps and removes the mappings which have become invalid.
	 * Due to us not knowing where an array variable is modified since we can't evaluate the expressions, we
	 * assume all expressions stored in that array become invalid. 
	 * 
	 * @param assignLhs : the Var indicating left hand side of the assignment we want to kill things for
	 * @param varToValForArrayComponents 
	 * @param exprToTemp
	 * @param varToVal
	 */
	public void killMappings(Var assignLhs, Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents, 
			 Map<IR_FieldDecl, ValueID> varToVal, Map<ValueID, List<Var>> valToVar){
		//TODO: Fix this.  
		System.err.println("Just entered killMappings. The current var being assigned is " + assignLhs.getName() + "." + System.getProperty("line.separator"));
		System.err.printf("Size of varToVal is currently %d" + System.getProperty("line.separator"), varToVal.size());
		IR_FieldDecl killVar = (IR_FieldDecl) assignLhs.getVarDescriptor().getIR();
		ValueID killValID = varToVal.get(killVar);
		if(varToVal.containsKey((IR_FieldDecl) assignLhs.getVarDescriptor().getIR())){
			setVarIDs(varToVal, varToValForArrayComponents, assignLhs);
			SPSet killSet = new SPSet(assignLhs);
		}
		
		varToVal.remove(killVar);
		if(valToVar.get(killValID)!= null){
		valToVar.get(killValID).remove(assignLhs);// Could have different vars mapping to the same valID due to CSE replacements/copy prop; don't want to kill those?
		}
		varToValForArrayComponents.remove(killVar);
		//Look up oldValID in varToVal or varToValForArrayComponents, then do valToVar.get(oldID).remove(assignLhs);
		
	}
	
	
	/**
	 * This is an attempt at finding the statements that were killed in a given Codeblock.
	 * It takes in a Set<Expression> of not yet killed expressions, and a FlowNode. 
	 * It checks that the given node is in fact a codeblock, and if it is, downcasts it.
	 * It then gets the list of statements from that block, and for each statement, checks if it
	 * was an assignment or method call. Declarations never kill anything, so we don't need a case for them.
	 * 
	 * CURRENTLY KILLS ALL GLOBALS IF A STATEMENT IS A METHOD CALL.
	 * 
	 * Think:  a = x + y. what should be stored is x+y
	 * x = 4; What should be killed is x+y.
	 * 
	 * 
	 * 
	 * @param node : FlowNode (really should be a Codeblock) that we want to investigate for killed statements.
	 * @param notKilledExprs : Set<Expression> containing things we know we could possibly kill. 
	 * @return killedStatements : Set<Statement> containing all statements that were killed by later assignments in this Codeblock.
	 */
	public Set<Expression> getKilledExpressions(FlowNode node, Set<Expression> notYetKilledExprs){
		Set<Expression> killedExpressions = new LinkedHashSet<Expression>();
		if(notYetKilledExprs == null){
			notYetKilledExprs = new LinkedHashSet<Expression>(); //Should this maybe be IR_FieldDecls, actually? 
		}
		HashMap<IR_FieldDecl, Set<Expression>> lookupToKillMap = new HashMap<IR_FieldDecl, Set<Expression>>();
		List<IR_FieldDecl> varList = new ArrayList<IR_FieldDecl>();
		if(node instanceof Codeblock){
			Codeblock cblock = (Codeblock)node;
			List<Statement> statementList = cblock.getStatements();
			for (Statement currentState : statementList){
				if(currentState instanceof Assignment){
					Assignment currentAssign = (Assignment)currentState; 
					Expression currentExpr = currentAssign.getValue();
					if(!notYetKilledExprs.contains(currentExpr)){
						notYetKilledExprs.add(currentExpr); //Put it in the list since we just saw it isn't there
						if(currentExpr instanceof BinExpr){
							BinExpr bin = (BinExpr)currentExpr;
							varList.addAll(getVarIRsFromExpression(bin.getLeftSide()));
							varList.addAll(getVarIRsFromExpression(bin.getRightSide()));
						}
						else if(currentExpr instanceof NotExpr){
							NotExpr not = (NotExpr)currentExpr;
							varList = getVarIRsFromExpression(not.getUnresolvedExpression());
						}

						else if(currentExpr instanceof NegateExpr){
							NegateExpr negate = (NegateExpr)currentExpr;
							varList = getVarIRsFromExpression(negate.getNegatedExpr());
						}

						else if (currentExpr instanceof Ternary){
							Ternary tern = (Ternary)currentExpr;
							varList.addAll(getVarIRsFromExpression(tern.getTernaryCondition()));
							varList.addAll(getVarIRsFromExpression(tern.getTrueBranch()));
							varList.addAll(getVarIRsFromExpression(tern.getFalseBranch()));
						}

						else if (currentExpr instanceof MethodCall){
							notYetKilledExprs.remove(currentExpr); //Get rid of the entire method call statement, can't really use that since we assume methods kill things.
						}

						for (IR_FieldDecl binVar : varList){
							if(!lookupToKillMap.containsKey(binVar)){
								lookupToKillMap.put(binVar, new LinkedHashSet<Expression>(Arrays.asList(currentExpr)));
							}
							else{
								lookupToKillMap.get(binVar).add(currentExpr);
							}
						}
					}

					else{ //This else is from the if(!notYetKilled.contains(currentExpr)) from so many lines ago. 
						//This is where you'd CSE, assuming you had the other pieces needed.     
					}

					//Actually kill things now!
					IR_FieldDecl currentAssignTarget = (IR_FieldDecl) currentAssign.getDestVar().getVarDescriptor().getIR();
					if(lookupToKillMap.get(currentAssignTarget) != null){
						killedExpressions.addAll(lookupToKillMap.get(currentAssignTarget));
					}
				}
				else if (currentState.getStatementType() == StatementType.METHOD_CALL_STATEMENT){
					//Set all global variables to a killed state
					for (IR_FieldDecl global : this.globalList){
						if(lookupToKillMap.get(global) != null){
							killedExpressions.addAll(lookupToKillMap.get(global));
						}
					}
				}

				//Don't have to have a case for branches or declarations, they never kill anything. 	
			}
		}
		return killedExpressions;
	}

	/**
	 * This method calculates available Expressions based on the algorithm given on slide 48 of
	 * http://6.035.scripts.mit.edu/sp14/slides/F14-lecture-10.pdf
	 * Update from Infosession: 
	 *  
	 *  Their use of "Expression" refers to X = A OP B, X = A, OR A CompareOp B. 
	 *  Two of those are assignments, which we cover under the heading "Statement", and would be in Codeblocks.
	 *  The last is a branch condition. 
	 *  Also, for method calls, set KILL to be all global variables. 
	 * 
	 * It will be a helper method used frequently in the code for actually doing Common Subexpression Elimination (CSE).
	 * 
	 * @param currentMethodFlownodes : A LinkedList<FlowNode> that contains the flownodes for the method you want to check availability for.
	 * You MUST have the first element in this LinkedList be a START node. 
	 * @return Set<Expression> : A set of the available expressions for the given method flownodes.
	 */
	public Map<FlowNode, Set<Expression>> calculateAvailableExpressions(START initialNode){
		HashMap<FlowNode, Set<Expression>> OUT = new HashMap<FlowNode, Set<Expression>>();
		HashMap<FlowNode, Set<Expression>> IN = new HashMap<FlowNode, Set<Expression>>();
		LinkedHashSet<Expression> allExprsForInitialization = new LinkedHashSet<Expression>();
		LinkedList<Codeblock> Changed = new LinkedList<Codeblock>();	
		List<FlowNode> processing = new ArrayList<FlowNode>();
		List<FlowNode> nodeList = new ArrayList<FlowNode>();
		processing.add(initialNode);
		while(!processing.isEmpty()){
			FlowNode next = processing.remove(0);
			next.visit();
			nodeList.add(next);
			for(FlowNode child : next.getChildren()){
				if(!child.visited()){
					processing.add(child);	
				}
			}
			if (next instanceof Codeblock){
				Codeblock cblock = (Codeblock)next;
				allExprsForInitialization.addAll(getAllExpressions(cblock));
				Changed.add(cblock); //Put the codeblock in the Changed set we'll use to do fixed point.
			}
		}
		initialNode.resetVisit();
		for (FlowNode c : nodeList){
			OUT.put(c, allExprsForInitialization);
		}

		//Next, actually carry out the changed iteration part of the algorithm. 
		while(!Changed.isEmpty()){
			Codeblock currentNode = Changed.pop(); //Get whatever the first codeblock in the set is. 
			Set<Expression> currentExpressionINSet = allExprsForInitialization; 
			for (FlowNode parentNode : currentNode.getParents()){
				if(parentNode instanceof Codeblock){
					Codeblock parent = (Codeblock)parentNode;
					currentExpressionINSet.retainAll(OUT.get(parent)); //Set intersection
				}	
			}
			IN.put(currentNode, currentExpressionINSet);
			Set<Expression> genUnisonFactor = new LinkedHashSet<Expression>(IN.get(currentNode)); 
			genUnisonFactor.removeAll(getKilledExpressions(currentNode, IN.get(currentNode))); // This is IN[Node] - KILL[Node], stored in a temp called getUnisonFactor

			//The below combines OUT[n] = GEN[n] UNION (IN[n] - KILL[n]) and the check for whether or not this changed OUT in one line.  
			if(OUT.get(currentNode).addAll(genUnisonFactor)){ //That addAll gives a boolean that is true if the set changed as a result of the add. 
				for (FlowNode childNode : currentNode.getChildren()){
					if(childNode instanceof Codeblock){
						Codeblock child = (Codeblock)childNode;
						if (Changed.contains(child)){
							continue;
						}
						Changed.add(child);
					}
				}
			}
		}

		return IN;
	}

	/**
	 * This is the method you call to do actual CSE. It takes in a List of
	 * start nodes for all the methods, and iterates through the list.
	 * 
	 * For each START node, the method should get all the var names used in the method, and create three maps:
	 * 
	 * Map<IR_FieldDecl, ValueID> varToVal : Variable to Symbolic Value for that variable's value
	 * Map<Expression, ValueID> expToVal : Expression to Symbolic Value for evaluation of said expression
	 * Map<SPSet, String> expToTemp : SPSet representation of an Expression to the compiler-generated temporary variable for it.
	 * 
	 * This method should do the following things:
	 * 
	 * 1) Step through each Statement in the given FlowNode.
	 * 	1.1) If the Statement is an Assignment: (i.e. x = a + b)
	 *     1.1a) Look at the values being assigned in the statement (a+b). For each component of the value being assigned (a and b), 
	 *     	  1.1ab) Check if we have ever seen this component before.
	 *     		- If we have not, and it is not a Literal, assign a symbolic value to represent this component. This is varToVal. a= v1, b = v2;. 
	 *     			This should never happen unless the component is a literal.
	 *     		+ If we have, replace the component with its symbolic value.
	 *     1.1b) After we have checked all components, check if we have ever seen this combination of symbolic values. 
	 *         - If we have not, then assign a symbolic value to represent the combination of these components : v3 = +(v1, v2). This is an SPSet.
	 *         - Place this symbolic value into expToTemp as a key, and let its value be a compiler-generated temporary variable: +(v1, v2) = temp1.
	 *         + If we have seen this combination, then replace it with the compiler generated temporary iff the expression is available. 
	 *  1.2) If the Statement is a method call, do nothing. 
	 *  1.3) If the Statement is a declaration, do nothing. 
	 * 2) After finishing the current FlowNode, begin the process again with its children. Use getAvailableExpressions to find out what is valid for substitution in the children.
	 * 
	 * Special cases : 
	 * What if child of START is a Branch? (Check for it, just go to children)
	 * What about loops? (check if child visited already)
	 * 
	 * @param startsForMethods
	 */
	public ControlflowContext applyCSE (List<START> startsForMethods){
		for(START initialNode : startsForMethods){
			//Set up tables and lists we'll need.
			Map<IR_FieldDecl, ValueID> varToVal = new HashMap<IR_FieldDecl, ValueID>();
			Map<SPSet, ValueID> expToVal = new HashMap<SPSet, ValueID>();
			Map<SPSet, Var> expToTemp = new HashMap<SPSet, Var>();
			Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents = new HashMap<IR_FieldDecl, Map<SPSet, ValueID>>();
			Map<ValueID, List<Var>> valToVar = new HashMap<ValueID, List<Var>>();
			Map<FlowNode, MapContainer> containerForNode = new HashMap<FlowNode, MapContainer>();
			for(IR_FieldDecl arg : initialNode.getArguments()){
				ValueID parameterID = new ValueID();
				List<Var> paramList = new ArrayList<Var>();
				paramList.add(new Var(new Descriptor(arg),null));
				varToVal.put(arg, parameterID);
				valToVar.put(parameterID, paramList);
			}
			MapContainer initialStateContainer = new MapContainer(varToVal, expToVal, expToTemp, varToValForArrayComponents, valToVar);
			containerForNode.put(initialNode, initialStateContainer);
			Set<String> allVarNames = getAllVarNamesInMethod(initialNode);
			FlowNode firstNodeInProgram = initialNode.getChildren().get(0);
			List<FlowNode> processing = new ArrayList<FlowNode>();
			processing.add(firstNodeInProgram);
			while(!processing.isEmpty()){ //list of nodes to process
				FlowNode currentNode = processing.remove(0); //get first node in list
				currentNode.visit(); //set its visited attribute so we don't loop back to it
				//Set up the maps for this particular node, regardless of type. 
				System.err.printf("We are about to get the parent Containers from the map. The currrent node has %d parent(s)." + System.getProperty("line.separator"), currentNode.getParents().size());
				System.err.printf("The map from nodes to containers currently has size %d" + System.getProperty("line.separator"), containerForNode.size());
				MapContainer thisNodeContainer = containerForNode.get(currentNode.getParents().get(0)); //want something we can intersect with, so take first parent's set.
				//TODO The above takes the parent's set and makes a deep clone of it; this will be non-destructive to the parent's set 
				for(FlowNode parent: currentNode.getParents()){
					thisNodeContainer = thisNodeContainer.calculateIntersection(containerForNode.get(parent)); //redundant on first parent but does nothing in that case, meaningful otherwise.  
				}
				varToVal = thisNodeContainer.varToVal;
				expToVal = thisNodeContainer.expToVal;
				expToTemp = thisNodeContainer.expToTemp;
				varToValForArrayComponents = thisNodeContainer.varToValForArrayComponents;
				valToVar = thisNodeContainer.valToVar;
				if(currentNode instanceof Codeblock){ //if codeblock downcast and make new
					Codeblock cblock = (Codeblock)currentNode; 
					Codeblock newCodeblock = new Codeblock(); 
					//Before we do anything, set up the temporary variable declarations:
					List<String> nextTempHolder = new ArrayList<String>();
					List<Var> allTheVarsInBlock = checkVariablesAssigned(cblock);
					for(Var current : allTheVarsInBlock){
						String nextTemp = generateNextTemp(allVarNames);
						newCodeblock.addStatement(new Declaration(new IR_FieldDecl(current.getVarDescriptor().getType(), nextTemp)));
						nextTempHolder.add(nextTemp);
					}
					for(Statement currentStatement : cblock.getStatements()){ //for each statement 
						if(currentStatement instanceof Assignment){
							Assignment currentAssign = (Assignment)currentStatement; //if assignment, downcast
							Expression assignExprValue = currentAssign.getValue(); // get expression on rhs
							Var currentDestVar = currentAssign.getDestVar(); //get the lhs for this assignment
							System.err.println("We are currently in an Assignment. The currentDestVar is " + currentDestVar.getName() + "." + System.getProperty("line.separator"));
							killMappings(currentDestVar, varToValForArrayComponents, varToVal, valToVar); //kill all newly invalid mappings and handle fixing ArrayComponent stuff
							setVarIDs(varToVal, varToValForArrayComponents, assignExprValue); //set rhs VarIDS if any Vars exist there, and update valToVar.
							ValueID currentValID = new ValueID(); //make a new value ID we'll use when we put things in the map/make a new temp.
							SPSet rhs = new SPSet(assignExprValue); //Construct an SPSet from the expresion.
							IR_FieldDecl lhs = (IR_FieldDecl)currentDestVar.getVarDescriptor().getIR();
							Set<SPSet> keySet = expToVal.keySet(); //Get the keys for the expToVal set.
							if(currentDestVar.getIndex() == null){ //Changed this from != when simulating execution. 
								varToVal.put(lhs, currentValID); 
							}
							else{
								if(!varToVal.containsKey(lhs)){
									varToVal.put(lhs, new ValueID());
								}
								Map<SPSet, ValueID> innerMap;
								if(varToValForArrayComponents.containsKey(lhs)){
									innerMap = varToValForArrayComponents.get(lhs);
								}
								else{
									innerMap = new HashMap<SPSet, ValueID>();
									varToValForArrayComponents.put(lhs, innerMap);
								}
								innerMap.put(new SPSet(currentDestVar.getIndex()), currentValID);
							}
							boolean changed = true; //we want to run repeated checks over the expression.
							while(changed){ //Until we reach a fixed point
								changed = false; //say we haven't
								for (SPSet key : keySet){ //Look at all keys in expToVal
									while (rhs.contains(key)){ //if we have any of those keys in our current expression 
									rhs.remove(key); //remove it
									rhs.addToVarSet(expToVal.get(key)); //replace it with the already-computed value. 
									changed = true; //Need to repass over, one substitution could lead to another
									}
								}
							}
							if (!keySet.contains(rhs)){ //If the rhs is something new that we haven't seen yet,
								if((rhs.SPSets.size() +rhs.intSet.size() +rhs.boolSet.size() + rhs.ternSet.size() + rhs.methodCalls.size() +rhs.comparisons.size()) !=0 || (rhs.varSet.size() > 1)){
									expToVal.put(rhs, currentValID); // put the rhs in the expToVal table with the ID we made earlier
									//Next line creates a new IR_FieldDecl for the compiler-generated temp, and makes the temp equal the assigned variable above.
									//So if we had a = x + y, we now have a temp value temp1 = a.
									IR_FieldDecl rhsTempDecl = new IR_FieldDecl(currentDestVar.getVarDescriptor().getType(), nextTempHolder.remove(0));
									expToTemp.put(rhs, new Var(new Descriptor(rhsTempDecl), null));
									List<Var> varList = valToVar.get(currentValID);
									if (varList == null) {
									    varList = new ArrayList<Var>();
									    valToVar.put(currentValID, varList);
									}
									varList.add(new Var(new Descriptor(rhsTempDecl), null));
								}
							}
							newCodeblock.addStatement(new Assignment(currentDestVar, Ops.ASSIGN, rhs.toExpression(valToVar))); //put the optimized expression in the codeblock
							newCodeblock.addStatement(new Assignment(expToTemp.get(rhs), Ops.ASSIGN, currentDestVar)); //t1 = previous variable
						}
						
						else if(currentStatement instanceof MethodCallStatement){ //if method call or declaration, just put it in the new block
							MethodCallStatement mcs = (MethodCallStatement)currentStatement;
							List<Expression> args = mcs.getMethodCall().getArguments();
							Map<SPSet,Integer> argMap = new HashMap<SPSet, Integer>();
							for(int i = 0; i < args.size(); i++){
								if(args.get(i).getExprType() != ExpressionType.STRING_LIT){
									Expression expr = args.get(i);
									setVarIDs(varToVal, varToValForArrayComponents, expr);
									argMap.put(new SPSet(expr), new Integer(i));
								}
							}
							boolean changedAtAll = false;
							for(SPSet arg : argMap.keySet()){
								boolean changed = true; //we want to run repeated checks over the expression.
								while(changed){ //Until we reach a fixed point
									changed = false; //say we haven't
									for (SPSet key : expToVal.keySet()){ //Look at all keys in expToVal
										while (arg.contains(key)){ //if we have any of those keys in our current expression
										arg.remove(key); //remove it
										arg.addToVarSet(expToVal.get(key)); //replace it with the already-computed value. 
										changed = true; //Need to repass over, one substitution could lead to another
										changedAtAll = true;
										}
									}
								}
							}
							if(changedAtAll){
								for(SPSet arg: argMap.keySet()){
									Expression expr = mcs.getMethodCall().getArguments().get(argMap.get(arg));
									expr = arg.toExpression(valToVar);
								}
							}
							newCodeblock.addStatement(mcs);
						}
						
						else{
							
							System.err.printf("We have reached a declaration. The declaration is for: %s" + System.getProperty("line.separator"), ((Declaration) currentStatement).getName());
							newCodeblock.addStatement(currentStatement);
						}
					}
					
					swapCodeblocks(cblock, newCodeblock);
					for(FlowNode child : newCodeblock.getChildren()){
						if(!child.visited()){
							processing.add(child);
						}
					}					
				}
				else if (currentNode instanceof Branch){
					Branch cbranch = (Branch)currentNode;
					Expression branchExpr = cbranch.getExpr();
					setVarIDs(varToVal, varToValForArrayComponents, branchExpr);
					SPSet branchExprSP = new SPSet(cbranch.getExpr());
					boolean changed = true; //we want to run repeated checks over the expression.
					boolean changedAtAll = false;
					while(changed){ //Until we reach a fixed point
						changed = false; //say we haven't
						for (SPSet key : expToVal.keySet()){ //Look at all keys in expToVal
							while (branchExprSP.contains(key)){ //if we have any of those keys in our current expression
							branchExprSP.remove(key); //remove it
							branchExprSP.addToVarSet(expToVal.get(key)); //replace it with the already-computed value. 
							changed = true; //Need to repass over, one substitution could lead to another
							changedAtAll = true;
							}
						}
					}
					if(changedAtAll){ //don't do anything if we never changed the expr, no need to do busywork
						cbranch.setExpr(branchExprSP.toExpression(valToVar)); //in place modification on block. No need to make a new one.
					}
				}
				
				else if(currentNode instanceof NoOp){
					currentNode.visit();
					for(FlowNode child : currentNode.getChildren()){
						if(!child.visited()){
							processing.add(child);
						}
					}
				}
				
				else if(currentNode instanceof START){
					currentNode.visit();
					for(FlowNode child : currentNode.getChildren()){
						if(!child.visited()){
							processing.add(child);
						}
					}
				}
				
				else if(currentNode instanceof END){
					END theEnd = (END)currentNode;
					Expression returnExpr = theEnd.getReturnExpression();
					setVarIDs(varToVal, varToValForArrayComponents, returnExpr);
					if(returnExpr != null){
						SPSet retSP = new SPSet(returnExpr);
						boolean changed = true; //we want to run repeated checks over the expression.
						boolean changedAtAll = false;
						while(changed){ //Until we reach a fixed point
							changed = false; //say we haven't
							for (SPSet key : expToVal.keySet()){ //Look at all keys in expToVal
								while (retSP.contains(key)){ //if we have any of those keys in our current expression
								retSP.remove(key); //remove it
								retSP.addToVarSet(expToVal.get(key)); //replace it with the already-computed value. 
								changed = true; //Need to repass over, one substitution could lead to another
								changedAtAll = true;
								}
							}
						}
						if(changedAtAll){
							theEnd.setReturnExpression(retSP.toExpression(valToVar));
						}
					}
				}
				
				//TODO : Upload container to the map.
				System.err.println("Finished processing the current FlowNode for the current method.");
				System.err.println("The current map sizes which will be put into the container are as follows:");
				System.err.printf("Size of varToVal: %d" + System.getProperty("line.separator"), varToVal.size());
				System.err.printf("Size of expToVal: %d" + System.getProperty("line.separator"),  expToVal.size());
				System.err.printf("Size of expToTemp: %d" + System.getProperty("line.separator"), expToTemp.size());
				System.err.printf("Size of varToValForArrayComponents: %d" + System.getProperty("line.separator"), varToValForArrayComponents.size());
				System.err.printf("Size of valToVar: %d" + System.getProperty("line.separator"), valToVar.size());
				MapContainer currentNodeContainer = new MapContainer(varToVal, expToVal, expToTemp, varToValForArrayComponents, valToVar);
				containerForNode.put(currentNode, currentNodeContainer);
			}
			System.err.println("Finished one method entirely. Now moving to next method.");
		}
		return Assembler.generateProgram(calloutList, globalList, flowNodes);
	}
}
