package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Arrays;
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
	private ControlflowContext context;
	private List<IR_Node> calloutList;
	private List<IR_FieldDecl> globalList;
	private HashMap<String, FlowNode> flowNodes;
	/**
	 * This is an constructor for optimizer. Once optimizations are done, it will call generateProgram with these parameters.  
	 *  
	 */
	public Optimizer(ControlflowContext context, 
			List<IR_Node> callouts, List<IR_FieldDecl> globals, HashMap<String, FlowNode> flowNodes){
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
	
	public List<IR_FieldDecl> getVarsFromExpression(Expression expr){
		List<IR_FieldDecl> allVars = new ArrayList<IR_FieldDecl>();
		if(expr instanceof BinExpr){
			BinExpr bin = (BinExpr)expr;
			Expression lhs = bin.getLeftSide();
			Expression rhs = bin.getRightSide();
			allVars.addAll(getVarsFromExpression(lhs));
			allVars.addAll(getVarsFromExpression(rhs));
		}
		else if (expr instanceof Var){
			Var varia = (Var)expr;
			allVars.add((IR_FieldDecl)varia.getVarDescriptor().getIR());
		}	
		else if(expr instanceof NotExpr){
			NotExpr nope = (NotExpr)expr;
			allVars.addAll(getVarsFromExpression(nope.getUnresolvedExpression()));
		}
		else if(expr instanceof NegateExpr){
			NegateExpr negate = (NegateExpr)expr;
			allVars.addAll(getVarsFromExpression(negate.negatedExpr));
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
					allExprs.addAll(mcState.getMethodCallStatement().getArguments());// TODO : FIX THIS WITH UPDATED VERSION, using new set code
				}
				//else if(currentStatement.getStatementType() == StatementType.DECLARATION)
			}
		}
		else if (node instanceof Branch){
			Branch splitNode = (Branch)node;
			allExprs.add(splitNode.getExpr());
		}
		return allExprs;
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
	/*
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
	*/
	public SPSet analyzeAndDispatch(Expression expr){
		switch(expr.getExprType()){
		case ADD_EXPR:
			return computePlusSets((AddExpr)expr);
		case COMP_EXPR: 
			return computeComparisonSets((CompExpr)expr);
		case COND_EXPR: 
			return computeCondSets((CondExpr) expr);
		case MOD_EXPR:
			return computeModSets((ModExpr)expr);
		case MULT_EXPR:
			return computeMultSets((MultExpr)expr);
		case DIV_EXPR: 
			return computeDivSets((DivExpr)expr);
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
			plusSet.SPSets.add(analyzeAndDispatch(lhs));
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
			plusSet.SPSets.add(analyzeAndDispatch(rhs));
		}
		return plusSet;
	}
	
	public SPSet computeModSets(ModExpr modding) {
		SPSet modSet = new SPSet(Ops.MOD);
		
		Expression lhs = modding.getLeftSide();
		if(lhs instanceof Var){
			Var varia = (Var) lhs;
			modSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(lhs instanceof ModExpr){
			ModExpr lhsMod = (ModExpr)lhs;
			SPSet recursiveSPSet = computeModSets(lhsMod);
			modSet.SPSets.addAll(recursiveSPSet.SPSets);
			modSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			modSet.SPSets.add(analyzeAndDispatch(lhs));
		}
		
		Expression rhs = modding.getRightSide();
		if(rhs instanceof Var){
			Var varia = (Var) rhs;
			modSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(rhs instanceof ModExpr){
			ModExpr rhsMod = (ModExpr)rhs;
			SPSet recursiveSPSet = computeModSets(rhsMod);
			modSet.SPSets.addAll(recursiveSPSet.SPSets);
			modSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			modSet.SPSets.add(analyzeAndDispatch(rhs));
		}
		return modSet;
	}
	
	public SPSet computeMultSets(MultExpr multing){
		SPSet multSet = new SPSet(Ops.TIMES);
		
		Expression lhs = multing.getLeftSide();
		if(lhs instanceof Var){
			Var varia = (Var) lhs;
			multSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(lhs instanceof MultExpr){
			MultExpr lhsMult = (MultExpr)lhs;
			SPSet recursiveSPSet = computeMultSets(lhsMult);
			multSet.SPSets.addAll(recursiveSPSet.SPSets);
			multSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			multSet.SPSets.add(analyzeAndDispatch(lhs));
		}
		
		Expression rhs = multing.getRightSide();
		if(rhs instanceof Var){
			Var varia = (Var) rhs;
			multSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(rhs instanceof MultExpr){
			MultExpr rhsMult = (MultExpr)rhs;
			SPSet recursiveSPSet = computeMultSets(rhsMult);
			multSet.SPSets.addAll(recursiveSPSet.SPSets);
			multSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			multSet.SPSets.add(analyzeAndDispatch(rhs));
		}
		return multSet;
	}
	
	public SPSet computeDivSets(DivExpr divide){
		SPSet divSet = new SPSet(Ops.DIVIDE);
		
		Expression lhs = divide.getLeftSide();
		if(lhs instanceof Var){
			Var varia = (Var) lhs;
			divSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(lhs instanceof DivExpr){
			DivExpr lhsMult = (DivExpr)lhs;
			SPSet recursiveSPSet = computeDivSets(lhsMult);
			divSet.SPSets.addAll(recursiveSPSet.SPSets);
			divSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			divSet.SPSets.add(analyzeAndDispatch(lhs));
		}
		
		Expression rhs = divide.getRightSide();
		if(rhs instanceof Var){
			Var varia = (Var) rhs;
			divSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(rhs instanceof DivExpr){
			DivExpr rhsMult = (DivExpr)rhs;
			SPSet recursiveSPSet = computeDivSets(rhsMult);
			divSet.SPSets.addAll(recursiveSPSet.SPSets);
			divSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			divSet.SPSets.add(analyzeAndDispatch(rhs));
		}
		return divSet;
	}
	
	public SPSet computeComparisonSets(CompExpr comparing){
		SPSet compSet = new SPSet(comparing.operator);
		
		Expression lhs = comparing.getLeftSide();
		if(lhs instanceof Var){
			Var varia = (Var) lhs;
			compSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(lhs instanceof CompExpr){
			CompExpr lhsComp = (CompExpr)lhs;
			SPSet recursiveSPSet = computeComparisonSets(lhsComp);
			compSet.SPSets.addAll(recursiveSPSet.SPSets);
			compSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			compSet.SPSets.add(analyzeAndDispatch(lhs));
		}
		
		Expression rhs = comparing.getRightSide();
		if(rhs instanceof Var){
			Var varia = (Var) rhs;
			compSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(rhs instanceof CompExpr){
			CompExpr rhsComp = (CompExpr)rhs;
			SPSet recursiveSPSet = computeComparisonSets(rhsComp);
			compSet.SPSets.addAll(recursiveSPSet.SPSets);
			compSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			compSet.SPSets.add(analyzeAndDispatch(rhs));
		}
		return compSet;
	}
	
	public SPSet computeCondSets(CondExpr conditional){
		SPSet condSet = new SPSet(conditional.operator);
		
		Expression lhs = conditional.getLeftSide();
		if(lhs instanceof Var){
			Var varia = (Var) lhs;
			condSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(lhs instanceof CondExpr){
			CondExpr lhsCond = (CondExpr)lhs;
			SPSet recursiveSPSet = computeCondSets(lhsCond);
			condSet.SPSets.addAll(recursiveSPSet.SPSets);
			condSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			condSet.SPSets.add(analyzeAndDispatch(lhs));
		}
		
		Expression rhs = conditional.getRightSide();
		if(rhs instanceof Var){
			Var varia = (Var) rhs;
			condSet.varSet.add((IR_FieldDecl) varia.getVarDescriptor().getIR());
		}
		else if(rhs instanceof CondExpr){
			CondExpr rhsCond = (CondExpr)rhs;
			SPSet recursiveSPSet = computeCondSets(rhsCond);
			condSet.SPSets.addAll(recursiveSPSet.SPSets);
			condSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else{
			condSet.SPSets.add(analyzeAndDispatch(rhs));
		}
		return condSet;
	}

	/**
	 * This is a rough first attempt at finding the statements that were killed in a given Codeblock.
	 * It checks that the given node is in fact a codeblock, and if it is, downcasts it.
	 * It then gets the list of statements from that block, and for each statement, checks if it
	 * was an assignment. 
	 * 
	 * CURRENTLY DOES NOTHING IF A STATEMENT IS NOT AN ASSIGNMENT.
	 * Think:  a = x + y. what should be stored is x+y
	 * x = 4; What should be killed is x+y
	 * 
	 * 
	 * If it was an assignment, downcast, and check the variable being assigned.
	 * If we've already seen it being assigned, then the statement just killed the previous assignment, so look up in the map
	 * of statements we've already processed that previous assignment, move that to the killedStatement list, and then overwrite
	 * it in the livingStatement list.   
	 * 
	 * @param node : FlowNode (really should be a Codeblock) that we want to investigate for killed statements.
	 * @return killedStatements : Set<Statement> containing all statements that were killed by later assignments in this Codeblock.
	 */
	public Set<Expression> getKilledExpressions(FlowNode node, List<Expression> notYetKilledExprs){
		Set<Expression> killedExpressions = new LinkedHashSet<Expression>();
		if(notYetKilledExprs == null){
			notYetKilledExprs = new LinkedList<Expression>(); //Should this maybe be IR_FieldDecls, actually? 
		}
		HashMap<IR_FieldDecl, Set<Expression>> lookupToKillMap = new HashMap<IR_FieldDecl, Set<Expression>>();
		List<IR_FieldDecl> varList = null;
		if(node instanceof Codeblock){
			Codeblock cblock = (Codeblock)node;
			List<Statement> statementList = cblock.getStatements();
			for (Statement currentState : statementList){
				if(currentState instanceof Assignment){
					Assignment currentAssign = (Assignment)currentState; 
					Expression currentExpr = currentAssign.getValue();
					if(!notYetKilledExprs.contains(currentExpr)){
						notYetKilledExprs.add(currentExpr);
						if(currentExpr instanceof BinExpr){
							BinExpr bin = (BinExpr)currentExpr;
							varList = new ArrayList<IR_FieldDecl>();
							varList.addAll(getVarsFromExpression(bin.getLeftSide()));
							varList.addAll(getVarsFromExpression(bin.getRightSide()));
						}
						else if(currentExpr instanceof NotExpr){
							NotExpr not = (NotExpr)currentExpr;
							varList = getVarsFromExpression(not.getUnresolvedExpression());
						}
						
						else if(currentExpr instanceof NegateExpr){
							NegateExpr negate = (NegateExpr)currentExpr;
							varList = getVarsFromExpression(negate.getNegatedExpr());
						}
						
						else if (currentExpr instanceof Ternary){
							Ternary tern = (Ternary)currentExpr;
							varList = getVarsFromExpression(tern.getTernaryCondition());
							varList.addAll(getVarsFromExpression(tern.getTrueBranch()));
							varList.addAll(getVarsFromExpression(tern.getFalseBranch()));
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
					
					else{ //This else is from the if(!notYetKilled.contains(currentExpr)) from so many lines ago. This is where you'd CSE.   
					}
					
					//TODO : Actually kill things now!
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
					
				//Don't have to have a case for declarations, they never kill anything. 	
				
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
			if (n instanceof Codeblock){
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
				if(parentNode instanceof Codeblock){
					Codeblock parent = (Codeblock)parentNode;
					currentExpressionSet.retainAll(OUT.get(parent)); //Set intersection
					IN.put(currentNode, currentExpressionSet);
				}	
			}
			Set<Expression> genUnisonFactor = new LinkedHashSet<Expression>(IN.get(currentNode)); 
			genUnisonFactor.removeAll(getKilledExpressions(currentNode, null)); // This is IN[Node] - KILL[Node], stored in a temp called getUnisonFactor
			
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
		
		//Now we have to get all the statements in OUT into one set so we can finally return it..
		Set<Expression> availableExpressions = new HashSet<Expression>();
		Set<FlowNode> listOfOutKeys = OUT.keySet();
		for(FlowNode key : listOfOutKeys){
			availableExpressions.addAll(OUT.get(key));
		}
		
		return availableExpressions; //Probably really buggy at this point, need to show group and debug 
	}
}
