package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.controlflow.Expression.ExpressionType;
import edu.mit.compilers.controlflow.FlowNode.NodeType;
import edu.mit.compilers.controlflow.Statement.StatementType;

/**
 * This class represents an object we can create to call our optimization methods. 
 * @author DeJuan
 *
 */
public class Optimizer {
	/**
	 * This is an empty constructor. For now, I was thinking of using just new Optimizer() and calling its methods on FlowNodes, etc. 
	 */
	public Optimizer(){}
	
	
	public List<Expression> simpleAlgebraicSimplifier(List<Expression> exprList){
		List<Expression> simplifiedExprs = new ArrayList<Expression>();
		for (Expression expr : exprList){
			switch(expr.getExprType()){
				case BIN_EXPR:
				BinExpr bin = (BinExpr)expr;
				switch(bin.getOperator()){
					case TIMES:
						if(bin.getRightSide().getType == ExpressionType.INT_LIT)
				}
				
			default:
				break;
			}
		}
	}
	
	/**
	 * This method takes in a FlowNode and allows you to check all variables assigned within the FlowNode.
	 * @param node : FlowNode that you wish to investigate
	 * @return assignedVars : List<Var> containing all variables that are assigned in this Node. Guaranteed empty if the node is not a Codeblock! 
	 */
	public List<Var> checkVariablesAssigned(FlowNode node){
		List<Var> assignedVars = new ArrayList<Var>();
		NodeType checkFlowType = node.getType();
		if (checkFlowType == NodeType.CODEBLOCK){
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
	 * This is a helper method designed to allow an easy way to check if a given 
	 * FlowNode is a Codeblock without having to repeatedly write the code needed to do so.
	 * @param node : The FlowNode whose type you want to confirm.
	 * @return boolean : true if the given node is a Codeblock, false otherwise. 
	 */
	public boolean checkIfCodeblock(FlowNode node){
		switch(node.getType()){
		case CODEBLOCK:
			return true;
		default:
			break;
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
	 * This is a helper method allowing us to deep copy any Collection.
	 */
	public <T> LinkedHashSet<T> deepCopyHashSet(HashMap<T, T> setToCopy){
		LinkedHashSet<T> copy = new LinkedHashSet<T>();
		for (T element : setToCopy){
			copy.add(element);
		}
		return copy;
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
	public Set<Statement> getKilledStatements(FlowNode node){
		Set<Statement> killedStatements = new LinkedHashSet<Statement>();
		HashMap<Var, Statement> availableStatements = new HashMap<Var, Statement>();
		if(checkIfCodeblock(node)){
			Codeblock cblock = (Codeblock)node;
			List<Statement> statementList = cblock.getStatements();
			for (Statement currentState : statementList){
				if(checkIfAssignment(currentState)){
					Assignment currentAssign = (Assignment)currentState;
					Var currentDestVar = currentAssign.getDestVar();
					if(availableStatements.containsKey(currentDestVar)){
						killedStatements.add(availableStatements.get(currentDestVar));
						availableStatements.put(currentDestVar, currentState);
						continue;
					}
					else{
						availableStatements.put(currentDestVar, currentState);
					}
				}
				else if (currentState.getStatementType() == StatementType.METHOD_CALL_STATEMENT){
					//TODO : Set all global variables to a killed state
				}
			}
		}
		return killedStatements;
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
		HashMap<FlowNode, Set<Statement>> OUT = new HashMap<FlowNode, Set<Statement>>();
		HashMap<FlowNode, Set<Statement>> IN = new HashMap<FlowNode, Set<Statement>>();
		Set<FlowNode> Changed = new LinkedHashSet<FlowNode>();
		for(FlowNode n: currentMethodFlownodes){ //First, set up the output: OUT[node] = all expressions in the node
			if (checkIfCodeblock(n)){
				Codeblock cblock = (Codeblock)n;
				LinkedHashSet<Statement> statementSet = new LinkedHashSet<Statement>(cblock.getStatements());
				// TODO Add getting righthand side if assignment and downcast to Expression
				OUT.put(n, statementSet); //Out[node] = set of all statements in node
				Changed.add(cblock); //Put the codeblock in the Changed set we'll use to do fixed point.
			}
		}
		
		//Next, actually carry out the changed iteration part of the algorithm.
		//This part is most likely so full of bugs it is scary and I am scared looking at it. 
		while(!Changed.isEmpty()){
			Object[] changedArray = Changed.toArray(); //Can't get a member of a set, so change it to an array
			Codeblock currentNode = (Codeblock) changedArray[0]; //Get whatever the first codeblock in the set is. 
			Changed.remove(currentNode); //TODO : MAY CAUSE MODIFICATION ERROR AND NEED ITERATOR USE, but take that codeblock out of the set.
			Set<Statement> currentStatementSet = IN.get(currentNode); //TODO : Check if type change causes get to fail. This is trying to do IN[node] = E
			for (FlowNode parentNode : currentNode.getParents()){
				if(checkIfCodeblock(parentNode)){
					Codeblock parent = (Codeblock)parentNode;
					currentStatementSet.retainAll(parent.getStatements()); //Set intersection
					IN.put(currentNode, currentStatementSet);
				}	
			}
			Set<Expression> genUnisonFactor = deepCopyHashSet(IN);
			Set<Expression> genUnisonFactor= IN.get(currentNode).removeAll(getKilledStatements(currentNode)); // This is IN[Node] - KILL[Node]
			
			if(OUT.get(currentNode).addAll(IN.get(currentNode))){ //That addAll gives a boolean that is true if the set changed as a result of the add.
				for (FlowNode childNode : currentNode.getChildren()){
					if(checkIfCodeblock(childNode)){
						Codeblock child = (Codeblock)childNode;
						Changed.add(child);
					}
				}
			}
		}
		
		//Now we have to get all the statements in OUT into one set so we can finally return it..
		Set<Statement> availableStatements = new HashSet<Statement>();
		Set<FlowNode> listOfOutKeys = OUT.keySet();
		for(FlowNode key : listOfOutKeys){
			availableStatements.addAll(OUT.get(key));
		}
		
		return availableStatements; //Probably really buggy at this point, need to show group and debug 
	}
}
