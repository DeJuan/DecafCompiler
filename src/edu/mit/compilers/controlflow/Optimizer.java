package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import edu.mit.compilers.controlflow.Expression.ExpressionType;
import edu.mit.compilers.controlflow.Statement.StatementType;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_Node;
import edu.mit.compilers.ir.Ops;

/**
 * This class represents an object we can create to call our optimization methods. 
 * @author DeJuan
 *
 */
public class Optimizer {
	/**
	 * This is an constructor for optimizer. Once optimizations are done, it will call generateProgram with these parameters.  
	 *  
	 */
	public Optimizer(ControlflowContext context, 
			List<IR_Node> callouts, List<IR_Node> globals, HashMap<String, FlowNode> flowNodes){}
	
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
	public List<Expression> getAllExpressions(FlowNode node){
		List<Expression> allExprs = new ArrayList<Expression>();
		if (node instanceof Codeblock){
			Codeblock cblock = (Codeblock)node;
			List<Statement> stateList = cblock.getStatements();
			for (Statement currentStatement : stateList){
				if (currentStatement.getStatementType() == StatementType.ASSIGNMENT){
					Assignment currentAssign = (Assignment) currentStatement;
					allExprs.add(currentAssign.getValue());
				}
				else if(currentStatement.getStatementType() == StatementType.METHOD_CALL_STATEMENT){
					MethodCallStatement mcState = (MethodCallStatement) currentStatement;
					mcState. // TODO : FIX THIS WITH UPDATED VERSION, using new set code
				}
			}
		}
		else if (node instanceof Branch){
			Branch splitNode = (Branch)node;
			allExprs.add(splitNode.getExpr());
		}
		return allExprs;
	}
	
	
	/**
	 * This is a helper method designed to allow an easy way to check if a given 
	 * FlowNode is a Codeblock without having to repeatedly write the code needed to do so.
	 * It is now deprecated. 
	 * 
	 * @param node : The FlowNode whose type you want to confirm.
	 * @return boolean : true if the given node is a Codeblock, false otherwise. 
	 */
	public boolean checkIfCodeblock(FlowNode node){
		if(node instanceof Codeblock){
			return true;
		}
		return false;
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
	 * This is a helper method allowing us to deep copy a HashMap.
	 * It is written using generic types so passing any valid HashMap will work.
	 * 
	 * @param setToCopy : The HashMap<T, K> that you want to copy, where T and K are some types.
	 * @return copy : A HashMap<T, K> that is a deep copy of the parameter to this method. 
	 */
	public <T, K> HashMap<T, K> deepCopyHashMap(HashMap<T, K> setToCopy){
		HashMap<T, K> copy = new HashMap<T, K>();
		Set<T> setToCopyKeys = setToCopy.keySet();
		for (T key : setToCopyKeys){
			copy.put(key, setToCopy.get(key));
		}
		return copy;
	}
	
	public Set<SPSet> computeAllSets(FlowNode node){
		Set<SPSet> allSets = new LinkedHashSet<SPSet>();
		if(node instanceof Codeblock){
			Codeblock cblock = (Codeblock)node;
			List<Expression> exprsInBlock = getAllExpressions(cblock);
			for (Expression expr : exprsInBlock){
				switch(expr.getExprType()){
				case ADD_EXPR:
					allSets.add(computePlusSets((AddExpr)expr));
					break;
				}
			}
		}
		else if(node instanceof Branch){
			Branch branch = (Branch)node;
			Expression expr = branch.getExpr();
			if (expr instanceof AddExpr){
				
			}
		}
	}
	
	public SPSet analyzeAndDispatch(Expression expr){
		switch(expr.getExprType()){
		case ADD_EXPR:
			return computePlusSets((AddExpr)expr);
		case COMP_EXPR: 
			return computeComparisonSet((CompExpr)expr);
		case COND_EXPR: 
			// TODO : Split Cond into AndExpr and OrExpr, check for instance, call respective maker
		case MOD_EXPR:
			return computeModSet((ModExpr)expr);
		case MULT_EXPR:
			return computeMultSet((MultExpr)expr);
		case DIV_EXPR: 
			return computeDivSet((DivExpr)expr);
		default:
			break;
		}
		return null;
	}
	
	public SPSet computePlusSets(AddExpr adding){
		SPSet plusSet = new SPSet(Ops.PLUS);

		Expression lhs = adding.getLeftSide();
		if(lhs instanceof Var){
			Var varia = (Var) lhs;
			plusSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(lhs instanceof AddExpr){
			AddExpr lhsAdd = (AddExpr)lhs;
			SPSet recursiveSPSet = computePlusSets(lhsAdd);
			plusSet.SPSets.addAll(recursiveSPSet.SPSets);
			plusSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			plusSet.SPSets.add(analyzeAndDispatch(lhs).SPSets);
			plusSet.varSet.add(analyzeAndDispatch(lhs).varSet);
		}
		
		Expression rhs = adding.getRightSide();
		if(rhs instanceof Var){
			Var variaR = (Var) rhs;
			plusSet.varSet.add((IR_FieldDecl) variaR.getVarDescriptor().getIR());
		}
		else if(rhs instanceof AddExpr){
			AddExpr rhsAdd = (AddExpr)rhs;
			SPSet recursiveSPSet = computePlusSets(rhsAdd);
			plusSet.SPSets.addAll(recursiveSPSet.SPSets);
			plusSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			plusSet.SPSets.add(analyzeAndDispatch(rhs).SPSets);
			plusSet.varSet.add(analyzeAndDispatch(rhs).varSet);
		}
		return plusSet;
	}
	
	/**
	 * This is a rough first attempt at finding the statements that were killed in a given Codeblock.
	 * It checks that the given node is in fact a codeblock, and if it is, downcasts it.
	 * It then gets the list of statements from that block, and for each statement, checks if it
	 * was an assignment. 
	 * 
	 * CURRENTLY DOES NOTHING IF A STATEMENT IS NOT AN ASSIGNMENT.
	 * 
	 * If it was an assignment, downcast, and check the variable being assigned.
	 * If we've already seen it being assigned, then the statement just killed the previous assignment, so look up in the map
	 * of statements we've already processed that previous assignment, move that to the killedStatement list, and then overwrite
	 * it in the livingStatement list.   
	 * 
	 * @param node : FlowNode (really should be a Codeblock) that we want to investigate for killed statements.
	 * @return killedStatements : Set<Statement> containing all statements that were killed by later assignments in this Codeblock.
	 */
	public Set<Expression> getKilledExpressions(FlowNode node){
		Set<Expression> killedExpressions = new LinkedHashSet<Expression>();
		HashMap<Var, Expression> availableExpressions = new HashMap<Var, Expression>();
		if(checkIfCodeblock(node)){
			Codeblock cblock = (Codeblock)node;
			List<Statement> statementList = cblock.getStatements();
			for (Statement currentState : statementList){
				if(checkIfAssignment(currentState)){
					Assignment currentAssign = (Assignment)currentState;
					Var currentDestVar = currentAssign.getDestVar();
					Expression currentExpr = currentAssign.getValue();
					if(availableExpressions.containsKey(currentDestVar)){
						killedExpressions.add(availableExpressions.get(currentDestVar));
						availableExpressions.put(currentDestVar, currentExpr);
						continue;
					}
					else{
						availableExpressions.put(currentDestVar, currentExpr);
					}
				}
				else if (currentState.getStatementType() == StatementType.METHOD_CALL_STATEMENT){
					//TODO : Set all global variables to a killed state
					LinkedList<FlowNode> parentChainNodes = new LinkedList<FlowNode>();
					while(!parentChainNodes.isEmpty()){
						FlowNode currentParentInChain = parentChainNodes.pop();
						if(!(currentParentInChain instanceof START)){
							parentChainNodes.addAll(currentParentInChain.getParents());
							continue;
						}
						else{
							START origin = (START)currentParentInChain;
							origin.getArguments() 
						}
					}
					
				}
			}
		}
		return killedExpressions;
	}
	
	/**
	 * This method calculates available Expressions based on the algorithm given on slide 48 of
	 * http://6.035.scripts.mit.edu/sp14/slides/F14-lecture-10.pdf
	 * 
	 * Note that in our implementation so far, I've replaced "Expression" with "Statement" because I think that's what we're calling those types
	 * of structures. VERY IMPORTANT NOTE: THIS IS A DRAFT AND ONLY LOOKS AT CODEBLOCKS, AND DOES NOTHING WITH NON-CODEBLOCKS.
	 * 
	 *  Update from Infosession: 
	 *  
	 *  Their use of "Expression" refers to X = A OP B, X = A, OR A CompareOp B.
	 *  For this reason, I'll need to make seperate expression and enum for recognizing comparison expressions. 
	 *  Two of those are assignments, which we cover under the heading "Statement". 
	 *  Also, for method calls, set KILL to be all global variables for that line. 
	 * 
	 * It will be a helper method used frequently in the code for actually doing Common Subexpression Elimination (CSE).
	 * @param currentMethodFlownodes : A Set<FlowNode> that contains the flownodes for the method you want to check availability for.
	 * @return Set<Expression> : A set of the available expressions for the given method flownodes.
	 */
	public Set<Expression> calculateAvailableExpressions(Set<FlowNode> currentMethodFlownodes){
		HashMap<FlowNode, Set<Expression>> OUT = new HashMap<FlowNode, Set<Expression>>();
		HashMap<FlowNode, Set<Expression>> IN = new HashMap<FlowNode, Set<Expression>>();
		LinkedList<FlowNode> Changed = new LinkedList<FlowNode>();
		for(FlowNode n: currentMethodFlownodes){ //First, set up the output: OUT[node] = all expressions in the node
			if (checkIfCodeblock(n)){
				Codeblock cblock = (Codeblock)n;
				LinkedHashSet<Expression> exprSet = new LinkedHashSet<Expression>(getAllExpressions(cblock));
				// TODO Add getting righthand side if assignment and downcast to Expression
				OUT.put(n, exprSet); //Out[node] = set of all statements in node
				Changed.add(cblock); //Put the codeblock in the Changed set we'll use to do fixed point.
			}
		}
		
		//Next, actually carry out the changed iteration part of the algorithm.
		//This part is most likely so full of bugs it is scary and I am scared looking at it. 
		while(!Changed.isEmpty()){
			Codeblock currentNode = (Codeblock)Changed.pop(); //Get whatever the first codeblock in the set is. 
			Set<Expression> currentExpressionSet = IN.get(currentNode); 
			for (FlowNode parentNode : currentNode.getParents()){
				if(checkIfCodeblock(parentNode)){
					Codeblock parent = (Codeblock)parentNode;
					currentExpressionSet.retainAll(OUT.get(parent)); //Set intersection
					IN.put(currentNode, currentExpressionSet);
				}	
			}
			Set<Expression> genUnisonFactor = new LinkedHashSet<Expression>(IN.get(currentNode)); 
			genUnisonFactor.removeAll(getKilledExpressions(currentNode)); // This is IN[Node] - KILL[Node], stored in a temp called getUnisonFactor
			
			//The below combines OUT[n] = GEN[n] UNION (IN[n] - KILL[n]) and the check for whether or not this changed OUT in one line.  
			if(OUT.get(currentNode).addAll(genUnisonFactor)){ //That addAll gives a boolean that is true if the set changed as a result of the add. 
				for (FlowNode childNode : currentNode.getChildren()){
					if(checkIfCodeblock(childNode)){
						Codeblock child = (Codeblock)childNode;
						if (Changed.contains(child)){
							continue;
						}
						Changed.add(child);
					}
				}
			}
		}
		
		//Now we have to get all the statements in OUT into one set so we can finally return it..
		Set<Expression> availableExpressions = new HashSet<Expression>();
		Set<FlowNode> listOfOutKeys = OUT.keySet();
		for(FlowNode key : listOfOutKeys){
			availableExpressions.addAll(OUT.get(key));
		}
		
		return availableExpressions; //Probably really buggy at this point, need to show group and debug 
	}
}
