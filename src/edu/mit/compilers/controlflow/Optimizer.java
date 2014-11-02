package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private int tempCounter = 0;
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

	public Set<String> getAllVarNamesInMethod(START node){
		Set<String> allVarNames = new LinkedHashSet<String>();
		List<FlowNode> processing = new ArrayList<FlowNode>();
		for(IR_FieldDecl global : globalList){
			allVarNames.add(global.getName());
		}
		processing.add(node.getChildren().get(0));
		while (!processing.isEmpty()){
			FlowNode currentNode = processing.remove(0);
			if(currentNode instanceof Codeblock){
				Codeblock cblock = (Codeblock)currentNode;
				for(Statement state : cblock.getStatements()){
					if(state instanceof Declaration){
						Declaration decl = (Declaration)state;
						allVarNames.add(decl.getName());
					}
				}
			}
			processing.addAll(currentNode.getChildren());
		}
		return allVarNames;
	}
	
	
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
			plusSet.varSet.add(varia.getValueID());
		}
		
		if(lhs instanceof IntLit){
			IntLit intLit = (IntLit)lhs;
			plusSet.intSet.add(intLit.getValue());
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
			plusSet.varSet.add(variaR.getValueID());
		}
		if(rhs instanceof IntLit){
			IntLit intLit = (IntLit)rhs;
			plusSet.intSet.add(intLit.getValue());
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
			modSet.varSet.add(varia.getValueID());
		}
		else if(lhs instanceof IntLit){
			IntLit intL = (IntLit)lhs;
			modSet.intSet.add(intL.getValue());
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
			modSet.varSet.add(varia.getValueID());
		}
		else if (rhs instanceof IntLit){
			IntLit intR = (IntLit)rhs;
			modSet.intSet.add(intR.getValue());
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
			multSet.varSet.add(varia.getValueID());
		}
		else if(lhs instanceof IntLit){
			IntLit intL = (IntLit)lhs;
			multSet.intSet.add(intL.getValue());
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
			multSet.varSet.add(varia.getValueID());
		}
		else if(rhs instanceof IntLit){
			IntLit intR = (IntLit)rhs;
			multSet.intSet.add(intR.getValue());
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
			divSet.varSet.add(varia.getValueID());
		}
		else if(lhs instanceof IntLit){
			IntLit intL = (IntLit)lhs;
			divSet.intSet.add(intL.getValue());
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
			divSet.varSet.add(varia.getValueID());
		}
		else if(rhs instanceof IntLit){
			IntLit intR = (IntLit)rhs;
			divSet.intSet.add(intR.getValue());
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
			compSet.varSet.add(varia.getValueID());
		}
		else if (lhs instanceof IntLit){
			IntLit intL = (IntLit)lhs;
			compSet.intSet.add(intL.getValue());
		}
		else if (lhs instanceof BoolLit){
			BoolLit intL = (BoolLit)lhs;
			compSet.boolSet.add(intL.getValue());
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
			compSet.varSet.add(varia.getValueID());
		}
		else if(rhs instanceof IntLit){
			IntLit intR = (IntLit)rhs;
			compSet.intSet.add(intR.getValue());
		}
		else if(rhs instanceof BoolLit){
			BoolLit intR = (BoolLit)rhs;
			compSet.boolSet.add(intR.getValue());
		}
		else if(rhs instanceof CompExpr){
			CompExpr rhsComp = (CompExpr)rhs;
			SPSet recursiveSPSet = computeComparisonSets(rhsComp);
			compSet.SPSets.addAll(recursiveSPSet.SPSets);
			compSet.varSet.addAll(recursiveSPSet.varSet);
		}
		else if(rhs instanceof NotExpr){

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
			condSet.varSet.add(varia.getValueID());
		}
		else if(lhs instanceof BoolLit){
			BoolLit intL = (BoolLit)lhs;
			condSet.boolSet.add(intL.getValue());
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
			condSet.varSet.add(varia.getValueID());
		}
		else if(rhs instanceof BoolLit){
			BoolLit intR = (BoolLit)rhs;
			condSet.boolSet.add(intR.getValue());
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
							varList.addAll(getVarsFromExpression(tern.getTernaryCondition()));
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

				//Don't have to have a case for declarations, they never kill anything. 	
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
			nodeList.add(next);
			for(FlowNode child : next.getChildren()){
				processing.add(child);
			}
			if (next instanceof Codeblock){
				Codeblock cblock = (Codeblock)next;
				allExprsForInitialization.addAll(getAllExpressions(cblock));
				Changed.add(cblock); //Put the codeblock in the Changed set we'll use to do fixed point.
			}
		}
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

		return OUT;
	}

	/**
	 * This is the method you call to do actual CSE. It takes in a List of
	 * start nodes for all the methods, and iterates through the list.
	 * 
	 * For each START node, 
	 * 
	 * @param startsForMethods
	 */
	
	public ControlflowContext applyCSE (List<START> startsForMethods){
		for(START initialNode : startsForMethods){
			Map<IR_FieldDecl, SPSet> varToVal = new HashMap<IR_FieldDecl, SPSet>();
			Set<String> allVarNames = getAllVarNamesInMethod(initialNode);
			Map<Expression, SPSet> expToVal = new HashMap<Expression, SPSet>();
			Map<Expression, String> expToTemp = new HashMap<Expression, String>();
			Map<FlowNode, Set<Expression>> availableExpressionsAtNode = calculateAvailableExpressions(initialNode);
			FlowNode firstNodeInProgram = initialNode.getChildren().get(0);
			Set<Expression> firstAvailableExprs = availableExpressionsAtNode.get(firstNodeInProgram);
			Iterator<Expression> iterForFirstExprs = firstAvailableExprs.iterator();
			while(iterForFirstExprs.hasNext()){
				Expression currentExpr = iterForFirstExprs.next();
				expToTemp.put(currentExpr, generateNextTemp(allVarNames));
				
			}
		}
		return context;
	}
}
