package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.codegen.Descriptor;
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
	
	/**
	 * This is the method that allows us to compute SPSets, given a generic Expression.
	 * It acts as a dispatcher that calls helper methods tailored to each type of expression.
	 * 
	 * @param expr : Expression that you want to compute an SPSet for
	 * @return SPSet : The SPSet representing that expression
	 */
	public SPSet analyzeAndDispatch(Expression expr){
		switch(expr.getExprType()){
		case ADD_EXPR:
			return computePlusSets((AddExpr)expr);
		case COMP_EXPR: 
			return computeComparisonSets((CompExpr)expr);
		case EQ_EXPR:
			return computeComparisonSets((EqExpr)expr); //Overloaded method
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


	/**
	 * Helper method for computing an SPSet if we know the expression is an AddExpr.
	 * @param adding : AddExpr we want the SPSet for
	 * @return SPSet : The SPSet for the given AddExpr
	 */
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
			Var varia = (Var) rhs;
			plusSet.varSet.add(varia.getValueID());
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

	/**
	 * Helper method for computing SPSet if we know the expression is a Mod.
	 * @param modding : ModExpr we want the SPSet for
	 * @return SPSet : The SPSet for the ModExpr we were given
	 */
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

	/**
	 * Helper function for computing SPSet if we know the Expression is a MultExpr.
	 * @param multing : MultExpr we want the SPSet for
	 * @return SPSet : SPSet for the given MultExpr
	 */
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

	/**
	 * Helper function for when we want the SPSet of a DivExpr.
	 * @param divide : The DivExpr we want the SPSet for
	 * @return SPSet : The SPSet for the given DivExpr
	 */
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
	
	/**
	 * Helper function for computing SPSet of some comparison which is not an equivalence check.
	 * @param comparing : CompExpr that we want the SPSet for
	 * @return SPSet : The SPSet for that CompExpr
	 */
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
			compSet.boolSet.add(intL.getTruthValue());
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
			compSet.boolSet.add(intR.getTruthValue());
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
	
	/**
	 * This is an overload on the method to handle the case when the comparison is an equivalence check.
	 * @param equivalence : The EqExpr we want the SPSet for
	 * @return SPSet : The SPSet for that EqExpr
	 */
	public SPSet computeComparisonSets(EqExpr equivalence){
		SPSet compSet = new SPSet(equivalence.operator);

		Expression lhs = equivalence.getLeftSide();
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
			compSet.boolSet.add(intL.getTruthValue());
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

		Expression rhs = equivalence.getRightSide();
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
			compSet.boolSet.add(intR.getTruthValue());
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
	
	/**
	 * Helper function used to compute SPSet for a conditional expression.
	 * @param conditional : CondExpr we want the SPSet for
	 * @return SPSet : The generated SPSet for the CondExpr
	 */
	public SPSet computeCondSets(CondExpr conditional){
		SPSet condSet = new SPSet(conditional.operator);

		Expression lhs = conditional.getLeftSide();
		if(lhs instanceof Var){
			Var varia = (Var) lhs;
			condSet.varSet.add(varia.getValueID());
		}
		else if(lhs instanceof BoolLit){
			BoolLit intL = (BoolLit)lhs;
			condSet.boolSet.add(intL.getTruthValue());
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
			condSet.boolSet.add(intR.getTruthValue());
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
	 * This method allows us to actually set the variable IDs, which is a requirement for initializing an SPSet
	 * for any Expression containing those variables. It takes in a Map<IR_FieldDecl, ValueID>, varToVal, which is generated when
	 * we attempt to do CSE, and an Expression, and sets up the ValueIDs for all variables in the given expression.
	 * 
	 * @param varToVal : Mapping of IR_FieldDecl to ValueID, used to assign ValueID to a Var, given its descriptor
	 * @param expr The expression whose Vars we want to set the ValueIDs for
	 */
	public void setVarIDs(Map<IR_FieldDecl, ValueID> varToVal, Expression expr){
		if(expr instanceof BinExpr){
			BinExpr bin = (BinExpr)expr;
			Expression lhs = bin.getLeftSide();
			Expression rhs = bin.getRightSide();
			setVarIDs(varToVal, lhs);
			setVarIDs(varToVal, rhs);
		}
		else if (expr instanceof Var){
			Var varia = (Var)expr;
			ValueID valID = varToVal.get((IR_FieldDecl)varia.getVarDescriptor().getIR());
			if (valID == null){
				throw new RuntimeException("Something went wrong; tried to set a ValueID with a null mapping.");
			}
			varia.setValueID(valID);
		}	
		else if(expr instanceof NotExpr){
			NotExpr nope = (NotExpr)expr;
			setVarIDs(varToVal, nope.getUnresolvedExpression());
		}
		else if(expr instanceof NegateExpr){
			NegateExpr negate = (NegateExpr)expr;
			setVarIDs(varToVal, negate.getExpression());
		}
		else if(expr instanceof Ternary){
			Ternary tern = (Ternary)expr;
			setVarIDs(varToVal, tern.getTernaryCondition());
			setVarIDs(varToVal, tern.trueBranch);
			setVarIDs(varToVal, tern.falseBranch);
		}
		else if(expr instanceof MethodCall){
			MethodCall MCHammer = (MethodCall)expr;
			for(Expression arg : MCHammer.getArguments()){
				setVarIDs(varToVal, arg);
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
					NoOp oldChild = (NoOp)oldC;
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

		return OUT;
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
			Map<IR_FieldDecl, ValueID> varToVal = new HashMap<IR_FieldDecl, ValueID>();
			Set<String> allVarNames = getAllVarNamesInMethod(initialNode);
			Map<SPSet, ValueID> expToVal = new HashMap<SPSet, ValueID>();
			Map<SPSet, Var> expToTemp = new HashMap<SPSet, Var>();
			Map<FlowNode, Set<Expression>> availableExpressionsAtNode = calculateAvailableExpressions(initialNode);
			FlowNode firstNodeInProgram = initialNode.getChildren().get(0);
			List<FlowNode> processing = new ArrayList<FlowNode>();
			processing.add(firstNodeInProgram);
			while(!processing.isEmpty()){
				FlowNode currentNode = processing.remove(0);
				currentNode.visit();
				if(currentNode instanceof Codeblock){
					Codeblock cblock = (Codeblock)currentNode;
					Codeblock newCodeblock = new Codeblock();
					for(Statement currentStatement : cblock.getStatements()){
						newCodeblock.addStatement(currentStatement);
						if(currentStatement instanceof Assignment){
							Assignment currentAssign = (Assignment)currentStatement;
							Expression assignExprValue = currentAssign.getValue();
							setVarIDs(varToVal, assignExprValue);
							SPSet rhs = new SPSet(assignExprValue);
							Set<SPSet> keySet = expToVal.keySet();
							boolean changed = true;
							while(changed){
								for (SPSet key : keySet){
									//while (rhs.contains(key)){
									// TODO : Get code from Maddie with SPSet doing contains on SPSet.
									//This is where the checking for already contained expressions and replacing expressions already seen happens.
									//}
								}
							}
							ValueID currentValID = new ValueID();
							expToVal.put(rhs, currentValID);
							Var currentDestVar = currentAssign.getDestVar();
							IR_FieldDecl rhsTempDecl = new IR_FieldDecl(currentDestVar.getVarDescriptor().getType(), generateNextTemp(allVarNames));
							expToTemp.put(rhs, new Var(new Descriptor(rhsTempDecl), null));
							newCodeblock.addStatement(new Assignment(expToTemp.get(rhs), Ops.ASSIGN, currentDestVar)); //t1 = previous variable
							
							varToVal.put((IR_FieldDecl)currentDestVar.getVarDescriptor().getIR(), currentValID);
						}
					}
					swapCodeblocks(cblock, newCodeblock);
				}
			}
			
		}
		return context;
	}
}
