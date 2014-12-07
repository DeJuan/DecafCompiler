package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.codegen.CodegenConst;
import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.codegen.LocLabel;
import edu.mit.compilers.codegen.LocationMem;
import edu.mit.compilers.controlflow.Branch.BranchType;
import edu.mit.compilers.controlflow.Expression.ExpressionType;
import edu.mit.compilers.controlflow.Statement.StatementType;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;
import edu.mit.compilers.ir.Ops;
import edu.mit.compilers.ir.Type;


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
            if(varia.getIndex() != null){
                allVars.addAll(getVarIRsFromExpression(varia.getIndex()));
            }
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
            if(varia.getIndex() != null){
                allVars.addAll(getVarsFromExpression(varia.getIndex()));
            }
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

    public Set<IR_FieldDecl> getAllFieldDeclsInMethod(START node){
        Set<IR_FieldDecl> allVarDecls = new LinkedHashSet<IR_FieldDecl>();
        List<FlowNode> processing = new ArrayList<FlowNode>();
        allVarDecls.addAll(globalList);
        allVarDecls.addAll(node.getArguments());
        processing.add(node.getChildren().get(0));
        while (!processing.isEmpty()){
            FlowNode currentNode = processing.remove(0);
            currentNode.visit();
            if(currentNode instanceof Codeblock){
                Codeblock cblock = (Codeblock)currentNode;
                for(Statement state : cblock.getStatements()){
                    if(state instanceof Declaration){
                        Declaration decl = (Declaration)state;
                        allVarDecls.add(decl.getFieldDecl());
                    }
                }
            }
            else if(currentNode instanceof Branch){
                Branch bblock = (Branch)currentNode;
                for (Var varia : getVarsFromExpression(bblock.getExpr())){
                    allVarDecls.add(varia.getFieldDecl());
                }
            }
            else if(currentNode instanceof START){
                START sBlock = (START)currentNode;
                allVarDecls.addAll(sBlock.getArguments());
            }
            else if(currentNode instanceof END){
                END eBlock = (END)currentNode;
                if(eBlock.getReturnExpression() != null){
                    for(Var varia : getVarsFromExpression(eBlock.getReturnExpression())){
                        allVarDecls.add(varia.getFieldDecl());
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
        return allVarDecls;
    }

    public boolean containsMethodCall(Expression expr){
        if(expr instanceof BinExpr){
            BinExpr bin = (BinExpr)expr;
            Expression lhs = bin.getLeftSide();
            Expression rhs = bin.getRightSide();
            boolean b1 = containsMethodCall(lhs);
            boolean b2 = containsMethodCall(rhs);
            return b1 || b2;
        }
        else if(expr instanceof NotExpr){
            NotExpr nope = (NotExpr)expr;
            return containsMethodCall(nope.getUnresolvedExpression());
        }
        else if(expr instanceof NegateExpr){
            NegateExpr negate = (NegateExpr)expr;
            return containsMethodCall(negate.getExpression());
        }
        else if(expr instanceof Ternary){
            Ternary tern = (Ternary)expr;
            boolean b1 = containsMethodCall(tern.getTernaryCondition());
            boolean b2 = containsMethodCall(tern.trueBranch);
            boolean b3 =  containsMethodCall(tern.falseBranch);
            return b1 || b2 || b3;
        }
        else if(expr instanceof MethodCall){
            return true;
        }
        else if(expr instanceof Var || expr instanceof IntLit || expr instanceof BoolLit || expr instanceof StringLit ){
            return false;
        }
        else{
            throw new UnsupportedOperationException("I missed a case in containsMethodCall.");
        }
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
        node.totalVisitReset();
        return allVarNames;
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

    public Map<FlowNode, Bitvector> vectorMapCopy(Map<FlowNode, Bitvector> mapToCopy){
        Map<FlowNode, Bitvector> copy = new HashMap<FlowNode, Bitvector>();
        for( FlowNode node : mapToCopy.keySet()){
            copy.put(node, mapToCopy.get(node).copyBitvector());
        }
        return copy;
    }
    
    public boolean checkDominanceSetEquivalence(Set<FlowNode> older, Set<FlowNode> newer){
		if(older.size() != newer.size()){
			return false;
		}
		
		//If they're the same size, check that all the entries are the same
		Iterator<FlowNode> oldIter = older.iterator();
		while(oldIter.hasNext()){
			FlowNode nextCheck = oldIter.next();
			if(!newer.contains(nextCheck)){
				return false;
			}
		}
		return true;
	}
	
	public Map<FlowNode, Set<FlowNode>> computeDominationMap(List<START> startsForMethods){
		Map<FlowNode, Set<FlowNode>> dominanceMap = new HashMap<FlowNode, Set<FlowNode>>();
		for(START methodStart : startsForMethods){
			methodStart.totalVisitReset(); //for safety's sake
			List<FlowNode> scanning = new ArrayList<FlowNode>();
			Set<FlowNode> startDominatedBy = new LinkedHashSet<FlowNode>();
			startDominatedBy.add(methodStart);
			dominanceMap.put(methodStart, startDominatedBy); //D[N_0] = N_0
			scanning.add(methodStart);
			Set<FlowNode> allNodes = new LinkedHashSet<FlowNode>();
			
			//Need to initialize all other nodes to have dominance[node] = {all nodes}.
			//First, find out what {all nodes} is!
			while(!scanning.isEmpty()){
				FlowNode currentNode = scanning.remove(0);
				currentNode.visit();
				allNodes.add(currentNode);
				for (FlowNode child : currentNode.getChildren()){
					if(!child.visited()){
						scanning.add(child);
					}
				}
			}
			methodStart.resetVisit(); //reset visit
			scanning.addAll(methodStart.getChildren()); // get the children of method start since it's actually already completely done, don't want to reprocess
			//First step in domination algorithm; set dominance[non-start node] = {all other nodes}
			while(!scanning.isEmpty()){
				FlowNode currentNode = scanning.remove(0);
				currentNode.visit();
				Set<FlowNode> listForNode = new LinkedHashSet<FlowNode>();
				listForNode.addAll(allNodes);
				dominanceMap.put(currentNode, listForNode);
				for (FlowNode child : currentNode.getChildren()){
					if(!child.visited()){
						scanning.add(child);
					}
				}
			}
			methodStart.resetVisit();
			
			//At this point, we've completed the initialization phase; for the start node, it is only dominated by itself,
			//the other nodes are all dominated by all other nodes. Now we iterate.
			scanning.addAll(methodStart.getChildren());
			while(!scanning.isEmpty()){
				FlowNode currentNode = scanning.remove(0);
				currentNode.visit();
				Set<FlowNode> oldDominatedBy = dominanceMap.get(currentNode); //record what the old dominance listing is
				Set<FlowNode> newDominatedBy = new LinkedHashSet<FlowNode>(); //make a brand new one we'll be replacing old with
				newDominatedBy.add(currentNode); //put self in new dominance listing
				List<FlowNode> predecessorIntersect = new ArrayList<FlowNode>(); //make new list we'll do the intersect part of the algorithm with
				predecessorIntersect.addAll(dominanceMap.get(currentNode.getParents().get(0))); //get dominance mapping for first parent
				for(FlowNode parent : currentNode.getParents()){ //for all the parents,
					predecessorIntersect.retainAll(dominanceMap.get(parent)); //intersect their dominatedBy vectors
				}
				newDominatedBy.addAll(predecessorIntersect); //Set union: self U dominators of predecessors	
				dominanceMap.put(currentNode, newDominatedBy);
				boolean changed = !checkDominanceSetEquivalence(oldDominatedBy, newDominatedBy); //if they are equivalent, they have not changed.
				
				if(changed){
					for(FlowNode child : currentNode.getChildren()){
						scanning.add(child); //if changed, visit children regardless, may change them too, such as in loops
					}
				}
				
				else{
					for(FlowNode child : currentNode.getChildren()){
						if(!child.visited){
							scanning.add(child); //if no change, don't need to revisit children that we've already seen.
						}
					}
				}
			}
			//Before we move to next start, do a reset.
			methodStart.resetVisit();
		}
		return dominanceMap;
	}
	
	
	
	/**
	 * In order to do loop optimizations, one must first find the loops.
	 * This method takes in startsForMethods and for each branch, finds the loop 
	 * it begins, if one exists. It needed such a specific return type because apparently triple nesting things is too hard
	 * for Eclipse to figure out.
	 * 
	 * 
	 * @param startsForMethods
	 * @return
	 */
	public List<HashMap<Branch, List<FlowNode>>> findLoops(List<START> startsForMethods){
		List<HashMap<Branch, List<FlowNode>>> loopContainer = new ArrayList<HashMap<Branch, List<FlowNode>>>();
		//TODO TODO TODO TODO TODO TODO NOT DONE YET TODO TODO
		for(START methodStart : startsForMethods){
			methodStart.totalVisitReset(); //just in case
			List<FlowNode> scanning = new ArrayList<FlowNode>(); //Need to find all the Branches before we can do anything more.
			scanning.add(methodStart);
			Set<Branch> branchNodes = new LinkedHashSet<Branch>();
			while(!scanning.isEmpty()){
				FlowNode currentNode = scanning.remove(0);
				currentNode.visit();
				if(currentNode instanceof Branch){
					branchNodes.add((Branch)currentNode);
				}
				
				for (FlowNode child : currentNode.getChildren()){
					if(!child.visited()){
						scanning.add(child);
					}
				}
			}
			methodStart.resetVisit();
			//TODO
			//At this point, we've acquired a list of all branches. Some of these will not be loops; need a way to tell.
			//Due to time constraints, we'll use naive solution of jus tsearching till we find  the path.
		}
		return loopContainer;
	}
    
   /**
	 * This is the method we'll be actually using to generate liveness vectors to do DCE. 
	 * It works backwards in reverse from each END in the program. 
	 * It uses a bit vector representation of liveness and traces through the code,
	 * calculating liveness until there is convergence.
	 * 
	 * Currently does NOT handle dead declarations (there's a justification for that, actually)
	 * and has not yet been tested. 
	 * 
	 * @param startsForMethods : List of START nodes for the given methods in the program. 
	 * @return 
	 */
	public Map<START, Map<FlowNode, Bitvector>> generateLivenessMap(List<START> startsForMethods){
		Map<START, Map<FlowNode, Bitvector>> liveStorage = new HashMap<START, Map<FlowNode, Bitvector>>();
		for(START methodStart : startsForMethods){
			Map<FlowNode, Integer> ticksForRevisit = new HashMap<FlowNode, Integer>();
			Map<FlowNode, Bitvector> vectorStorageIN = new HashMap<FlowNode, Bitvector>(); //set up place to store maps for input from children
			Map<FlowNode, Bitvector> vectorStorageOUT = new HashMap<FlowNode, Bitvector>(); //set up place to store maps for output from blocks
			//First things first: We will be called from DCE or another optimization, so reset visits before we do anything else.
			methodStart.totalVisitReset();
			List<FlowNode> scanning = new ArrayList<FlowNode>(); //Need to find all the ENDs before we can do anything more.
			scanning.add(methodStart);
			Set<END> endNodes = new LinkedHashSet<END>();
			Set<IR_FieldDecl> allVars = getAllFieldDeclsInMethod(methodStart);
			Bitvector zeroVector = new Bitvector(allVars); //Initializes all slots to 0 in constructor.
			while(!scanning.isEmpty()){ //scan through all nodes and track which ones are ENDs.
				FlowNode currentNode = scanning.remove(0);
				currentNode.visit();
				if(!vectorStorageIN.containsKey(currentNode)){
					vectorStorageIN.put(currentNode, zeroVector.copyBitvector()); //Set bitvectors to all zeros
				}
				if(!vectorStorageOUT.containsKey(currentNode)){
					vectorStorageOUT.put(currentNode, zeroVector.copyBitvector());
				}
				if(currentNode instanceof END){ 
					endNodes.add((END)currentNode);
				}
				for (FlowNode child : currentNode.getChildren()){
					if(!child.visited()){
						scanning.add(child);
					}
				}
			}
			
			for(END initialNode : endNodes){
				methodStart.totalVisitReset(); //Need to fix the visits since we just tampered with them.
				for(FlowNode node : vectorStorageIN.keySet()){
					ticksForRevisit.put(node, 0); //set up/ reset the ticker so we can see if we want to do revisits
				}
				Bitvector liveVector = vectorStorageIN.get(initialNode); //set up the bitvector. Initialized to any current values.
				if(initialNode.getReturnExpression() != null){
					for(IR_FieldDecl returnVar : getVarIRsFromExpression(initialNode.getReturnExpression())){
						liveVector.setVectorVal(returnVar, 1); //things returned must be alive on exit, so set their vector to 1
						//System.err.printf("Set variable %s's bitvector entry to 1 due to use in END return statement." + System.getProperty("line.separator"), returnVar.getName());
					}
				}
				for (IR_FieldDecl global : globalList){
					liveVector.setVectorVal(global, 1);
				}
				for(IR_FieldDecl argument : methodStart.getArguments()){
					liveVector.setVectorVal(argument, 1);
				}
				//Since we move from top to bottom, OUT is what propagates upward, where it is part of the IN of the next block.
				vectorStorageOUT.put(initialNode, liveVector.copyBitvector().vectorUnison(vectorStorageOUT.get(initialNode)));
				//Now we've set up everything from the end of the program, assuming working on only one END at a time. Now we walk backwards. 
				List<FlowNode> processing = new ArrayList<FlowNode>();
				for(FlowNode parent : initialNode.getParents()){
					processing.add(parent);
				}
				FlowNode previousNode = initialNode;
				while(!processing.isEmpty()){
					FlowNode currentNode = processing.remove(0);
					currentNode.visit();
					if(currentNode.getChildren().size() == 1){
						liveVector = vectorStorageOUT.get(currentNode.getChildren().get(0)).copyBitvector();
					}
					else{
						liveVector = Bitvector.childVectorUnison(currentNode.getChildren(), vectorStorageOUT, vectorStorageIN.get(currentNode));
					}
					Bitvector previousIN = vectorStorageIN.get(currentNode).copyBitvector();
					vectorStorageIN.put(currentNode, liveVector.copyBitvector().vectorUnison(vectorStorageIN.get(currentNode)));
					boolean canSkipReprocessFlag = previousIN.compareBitvectorEquality(vectorStorageIN.get(currentNode));
					boolean skipNode = false;
					if(canSkipReprocessFlag){
						if(ticksForRevisit.get(currentNode).equals(0)){
							//System.err.println("The current node's IN has not changed since last processing. We will compute at most three times to be safe.");
							ticksForRevisit.put(currentNode, 1);
						}
						else if(ticksForRevisit.get(currentNode).equals(1)){
							//System.err.println("The current node's IN has not changed since last processing, and has been processed once. Two more computations to be safe.");
							ticksForRevisit.put(currentNode, 2);
						}
						else if(ticksForRevisit.get(currentNode).equals(2)){
							//System.err.println("We have processed this node at least twice, and no changes have occurred to its IN. Last attempt at reprocessing.");
							ticksForRevisit.put(currentNode, 3);
						}
						else if(ticksForRevisit.get(currentNode).equals(3) || ticksForRevisit.get(currentNode) > 3){
							//System.err.println("We have processed this node at least three times. No further reprocessing is required.");
							skipNode = true;
							ticksForRevisit.put(currentNode, 4);
						}
					}
					if (previousNode == currentNode){
						ticksForRevisit.put(currentNode, ticksForRevisit.get(currentNode)+1);
						if(ticksForRevisit.get(currentNode).equals(4) || ticksForRevisit.get(currentNode) > 4){
							//System.err.println("We have processed this node at least three times, and have detected self-looping. No further reprocessing is required.");
							skipNode = true;
						}
					}
					if(skipNode){
						//System.err.println("SKIPPING NODE " + currentNode);
						for(FlowNode parent : currentNode.getParents()){
							if(!parent.visited()){
								if(!processing.contains(parent)){
									processing.add(parent);
									//System.err.println("Just added parent " + parent + "of current node " + currentNode);
								}
							}
						}
						continue;
					}
					if(currentNode instanceof Codeblock){
						Codeblock cblock = (Codeblock)currentNode;
						List<Statement> statementList = cblock.getStatements();
						Collections.reverse(statementList); //reverse the list so we can go backwards through it
						Iterator<Statement> statementIter = statementList.iterator();
						while(statementIter.hasNext()){
							Statement currentState = statementIter.next();
							if(currentState instanceof Assignment){ 
								Assignment assign = (Assignment)currentState;
								IR_FieldDecl lhs = assign.getDestVar().getFieldDecl();
								List<IR_FieldDecl> varsInRHS = getVarIRsFromExpression(assign.getValue());
								Set<IR_FieldDecl> rhsDecls = new LinkedHashSet<IR_FieldDecl>();
								//LEFT HAND FIRST LOGIC
								if(liveVector.get(lhs) == 1 || (liveVector.get(lhs) == 0 && containsMethodCall(assign.getValue()))){ //If this is alive, MAY need to flip the bit
									for(IR_FieldDecl varDecl : varsInRHS){
										liveVector.setVectorVal(varDecl, 1); //rhs if we changed it is not alive, because the assignment as a whole is dead.
										//System.err.printf("Bitvector entry for variable %s has been set to 1 in building phase due to use in live assigment OR one with method call." + System.getProperty("line.separator"), varName);
										rhsDecls.add(varDecl);
									}
									if(assign.getDestVar().getIndex() != null){
										for (IR_FieldDecl index : getVarIRsFromExpression(assign.getDestVar().getIndex()))
										liveVector.setVectorVal(index, 1);
									}
									if(!rhsDecls.contains(lhs) && assign.getDestVar().getIndex() == null && assign.getOperator() != Ops.ASSIGN_MINUS && assign.getOperator() != Ops.ASSIGN_PLUS){
										liveVector.setVectorVal(lhs, 0);
										//System.err.printf("Bitvector entry for variable %s has been flipped from 1 to 0 in building phase by an assignment that does not expose an upwards use and is not an array." + System.getProperty("line.separator"), lhs);
									}
									else{
										//System.err.printf("Bitvector entry for variable %s has not been flipped and remains 1 due to exposed upward use in RHS, or being an array." + System.getProperty("line.separator"), lhs);
									}
								}									
							}

							else if(currentState instanceof Declaration){ 
								//if var declared isn't ever alive, could remove the decl; but no time to work it out and debug 
							}

							else if(currentState instanceof MethodCallStatement){ //set liveness vectors for the args
								MethodCallStatement mcall = (MethodCallStatement)currentState;
								List<Expression> args = mcall.getMethodCall().getArguments();
								Set<IR_FieldDecl> varsInArgs = new LinkedHashSet<IR_FieldDecl>();
								for(Expression expr : args){
									varsInArgs.addAll(getVarIRsFromExpression(expr));
								}
								for(IR_FieldDecl varDecl : varsInArgs){
									liveVector.setVectorVal(varDecl, 1); //If not already alive, mark an argument as alive. 
									//System.err.printf("Bitvector entry for variable %s has been set to 1 in building phase by a method call." + System.getProperty("line.separator"), varDecl.getName());
								}
							}
						}
						Collections.reverse(statementList); //We reversed the list to iterate through it backwards, so fix it before we move on!
					}

					else if (currentNode instanceof Branch){ //join point. Live vector would be handled before we got here by unisonVector, so assume we're golden.
						Branch cBranch = (Branch)currentNode;
						for(IR_FieldDecl varDecl : getVarIRsFromExpression(cBranch.getExpr())){
							liveVector.setVectorVal(varDecl, 1); //anything showing up in a branch expression is used by definition, otherwise prog is invalid.
							//System.err.printf("Just set variable %s 's bitvector to 1 in building phase due to usage in a branch condition." + System.getProperty("line.separator"), varDecl.getName());
						}
					}

					else if(currentNode instanceof NoOp){ //split point. No expr here, so don't have to scan it.
						//Do nothing, just move onward to post-block processing.
					}

					else if(currentNode instanceof START){
						START cStart = (START)currentNode;
						List<IR_FieldDecl> args = cStart.getArguments();
						for (IR_FieldDecl arg : args){
							liveVector.setVectorVal(arg, 1); 
							//System.err.printf("Just set variable %s 's bitvector to 1 in building phase due to a START." + System.getProperty("line.separator"), arg.getName());
						}	
					}

					else if(currentNode instanceof END){
						END cEnd = (END)currentNode;
						if(cEnd.getReturnExpression() != null){
							for(IR_FieldDecl varDecl : getVarIRsFromExpression(cEnd.getReturnExpression())){
								liveVector.setVectorVal(varDecl, 1);
								//System.err.printf("Just set variable %s 's bitvector to 1 in building phase. due to an END." + System.getProperty("line.separator"), varDecl.getName());
							}
						}
					}
					
					boolean changed;
					Bitvector previousOut = vectorStorageOUT.get(currentNode).copyBitvector();
					Bitvector newOut = liveVector.vectorUnison(previousOut);
					changed = !previousOut.compareBitvectorEquality(newOut);
					vectorStorageOUT.put(currentNode, newOut);
					if(!changed){
						for(FlowNode parent : currentNode.getParents()){
							if(!parent.visited){
								if(!processing.contains(parent)){
									processing.add(parent);
									//System.err.println("Just added parent " + parent + "of current node " + currentNode);
								}
							}
						}
					}
					else{						
						//System.err.println("Finished processing " + currentNode + "whose bitvector OUT did change.");
						for(FlowNode parent : currentNode.getParents()){
							if(!processing.contains(parent)){
								processing.add(parent);
								//System.err.println("Just added parent " + parent + "of current node " + currentNode);
							}
						}
					}
					previousNode = currentNode;
				}
			}
			liveStorage.put(methodStart, vectorStorageIN);
			methodStart.totalVisitReset(); //fix all the visited nodes before we go to next START.
		}
		return liveStorage;
		//return vectorStorageIN;
	}
	
	/**
	 * This is the method we'll be actually using to do DCE. 
	 * It works backwards in reverse from each END in the program. 
	 * It uses a bit vector representation of liveness and traces through the code,
	 * deleting any statements that are unneeded.
	 * 
	 * Currently does NOT handle dead declarations (there's a justification for that, actually)
	 * and has not yet been tested. 
	 * 
	 * @param startsForMethods : List of START nodes for the given methods in the program. 
	 * @return 
	 */
	public boolean applyDCE(List<START> startsForMethods){
		boolean anythingRemoved = false;
		//System.err.println("====================================ENTERING MAP SETUP PHASE===================================");
		Map<START, Map<FlowNode, Bitvector>> livenessMap = generateLivenessMap(startsForMethods);
		//System.err.println("====================================MAP SETUP COMPLETE. NOW EXECUTING==========================");
		for (START initialNode : startsForMethods){
			Set<Codeblock> listOfCodeblocks = new LinkedHashSet<Codeblock>();
			Map<FlowNode, Bitvector> liveness = livenessMap.get(initialNode);
			if(liveness == null){
				System.err.println("BUG DETECTED BUG DETECTED!!! liveness for this particular initialNode is NULL.");
			}
			List<FlowNode> scanning = new ArrayList<FlowNode>(); //Need to find all the Codeblocks
			scanning.add(initialNode);
			while(!scanning.isEmpty()){ //scan through all nodes and create listing.
				FlowNode currentNode = scanning.remove(0);
				currentNode.visit();
				//System.err.println("Now visiting " + currentNode);
				if(currentNode instanceof Codeblock){
					listOfCodeblocks.add((Codeblock)currentNode);
				}
				for (FlowNode child : currentNode.getChildren()){
					if(!child.visited()){
						scanning.add(child);
					}
				}
			}
			initialNode.resetVisit(); //fix the visited parameters.
			///System.err.println("BEFORE ITERATION");
			//System.err.println(liveness.get(initialNode.getChildren().get(0)));
			//System.err.println(initialNode.getChildren().get(0));
			for (Codeblock cblock : listOfCodeblocks){
				//System.err.println("NOW CHECKING " + cblock);
				Bitvector liveCheck = liveness.get(cblock);
				List<Statement> statementList = cblock.getStatements();
				Collections.reverse(statementList);
				Iterator<Statement> statementIter = statementList.iterator();
				while(statementIter.hasNext()){
					Statement currentState = statementIter.next();
					if(currentState instanceof Assignment){
						Assignment assign = (Assignment)currentState;
						IR_FieldDecl lhs = assign.getDestVar().getFieldDecl();
						if(liveCheck.get(lhs) == null){
							throw new UnsupportedOperationException("liveCheck.get(" +  lhs.getName() + ") is null!");
						}
						else if(liveCheck.get(lhs) == 0 && !(containsMethodCall(assign.getValue()))){
							statementIter.remove();
							anythingRemoved = true;
							//System.err.printf("Assignment to variable %s has been removed; it was a dead assignment with no method call." + System.getProperty("line.separator"), assign.getDestVar().getName());
						}
						else{
							List<IR_FieldDecl> rhsDecls = new ArrayList<IR_FieldDecl>();
							for(IR_FieldDecl varDecl : getVarIRsFromExpression(assign.getValue())){
								liveCheck.setVectorVal(varDecl, 1);
								//System.err.printf("Bitvector entry for variable %s has been set to 1 by use in assignment." + System.getProperty("line.separator"), varName);
								rhsDecls.add(varDecl);
							}
							if(assign.getDestVar().getIndex() != null){
								for (IR_FieldDecl index : getVarIRsFromExpression(assign.getDestVar().getIndex())){
								liveCheck.setVectorVal(index, 1);
								//System.err.printf("Bitvector entry for variable %s has been set to 1 by use in array index on lhs." + System.getProperty("line.separator"), index.getName());
								}
							}
							if(!rhsDecls.contains(lhs) && assign.getDestVar().getIndex() == null && assign.getOperator() != Ops.ASSIGN_MINUS && assign.getOperator() != Ops.ASSIGN_PLUS){
								liveCheck.setVectorVal(lhs, 0);
								//System.err.printf("Bitvector entry for variable %s has been flipped from 1 to 0 in execution phase phase by an assignment that does not expose an upwards use and isn't an array." + System.getProperty("line.separator"), lhs);
							}
							else{
								//System.err.printf("Bitvector entry for variable %s has not been flipped and remains 1 due to exposed upward use in RHS." + System.getProperty("line.separator"), nameOfVar);
							}
						}
					}
					
					else if(currentState instanceof MethodCallStatement){
						MethodCallStatement mcall = (MethodCallStatement)currentState;
						List<Expression> args = mcall.getMethodCall().getArguments();
						List<IR_FieldDecl> varsInArgs = new ArrayList<IR_FieldDecl>();
						for(Expression expr : args){
							varsInArgs.addAll(getVarIRsFromExpression(expr));
						}
						for(IR_FieldDecl varDecl : varsInArgs){
							liveCheck.setVectorVal(varDecl, 1); //If not already alive, mark an argument as alive.
							//System.err.printf("Bitvector entry for variable %s has been set to 1 by a method call." + System.getProperty("line.separator"), varDecl.getName());
						}
					}
				}
				Collections.reverse(statementList);
			}
			clearUnusedDeclarations(initialNode);
		}
		return anythingRemoved;
		//return Assembler.generateProgram(calloutList, globalList, flowNodes);
	}

    /**
     * Helper method which may be completely unnecessary; makes absolutely certain to
     * deep copy the String, Integer map that I use to track bit vectors.
     * @param liveVector : The bitvector map we want to copy.
     */
    public Map<String, Integer> copyVectorMap(Map<String, Integer> liveVector){
        Map<String, Integer> vectorCopy = new HashMap<String, Integer>();
        for (String key : liveVector.keySet()){
            vectorCopy.put(key, new Integer(liveVector.get(key)));
        }
        return vectorCopy;
    }

    /**
     * Computes correct type of a temp variable, given the type of an assignment destination
     * INTARR, INT --> INT
     * BOOLARR, BOOL --> BOOL
     */
    private Type getTempType(Type destType) {
        if (destType == Type.BOOL || destType == Type.BOOLARR) {
            return Type.BOOL;
        } else if (destType == Type.INT || destType == Type.INTARR) {
            return Type.INT;
        } else {
            throw new RuntimeException("What the actual hell are you doing - don't use this method");
        }
    }

    /**
     * This method allows us to actually set the variable IDs, which is a requirement for initializing an SPSet
     * for any Expression containing those variables. It takes in a Map<IR_FieldDecl, ValueID>, varToVal, which is generated when
     * we attempt to do CSE, and an Expression, and sets up the ValueIDs for all variables in the given expression.
     * 
     * @param varToVal : Mapping of IR_FieldDecl to ValueID, used to assign ValueID to a Var, given its descriptor
     * @param expr The expression whose Vars we want to set the ValueIDs for
     */
    public static boolean setVarIDs(Map<IR_FieldDecl, ValueID> varToVal, Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents, Expression expr){
        if(expr instanceof BinExpr){
            BinExpr bin = (BinExpr)expr;
            Expression lhs = bin.getLeftSide();
            Expression rhs = bin.getRightSide();
            boolean lhsWorked = setVarIDs(varToVal, varToValForArrayComponents, lhs);
            boolean rhsWorked = setVarIDs(varToVal, varToValForArrayComponents, rhs);
            return lhsWorked && rhsWorked;
        }
        else if (expr instanceof Var){
            Var varia = (Var)expr;
            ValueID valID = varToVal.get((IR_FieldDecl)varia.getVarDescriptor().getIR());
            if(varia.getIndex() != null){
                IR_FieldDecl varDecl = (IR_FieldDecl) varia.getVarDescriptor().getIR();
                if (!setVarIDs(varToVal, varToValForArrayComponents, varia.getIndex())) {
                    return false;
                }
                SPSet varArraySP = new SPSet(varia.getIndex());
                if (!varToValForArrayComponents.containsKey(varDecl)) {
                    return false;
                }
                valID = varToValForArrayComponents.get(varDecl).get(varArraySP);
            }
            if (valID == null){
                return false;
            }
            else{
                varia.setValueID(valID);
                return true;
            }
        }   
        else if(expr instanceof NotExpr){
            NotExpr nope = (NotExpr)expr;
            return setVarIDs(varToVal, varToValForArrayComponents, nope.getUnresolvedExpression());
        }
        else if(expr instanceof NegateExpr){
            NegateExpr negate = (NegateExpr)expr;
            return setVarIDs(varToVal, varToValForArrayComponents, negate.getExpression());
        }
        else if(expr instanceof Ternary){
            Ternary tern = (Ternary)expr;
            boolean condWorked = setVarIDs(varToVal, varToValForArrayComponents, tern.getTernaryCondition());
            boolean trueWorked = setVarIDs(varToVal, varToValForArrayComponents, tern.trueBranch);
            boolean falseWorked = setVarIDs(varToVal, varToValForArrayComponents, tern.falseBranch);
            return condWorked && trueWorked && falseWorked;
        }
        else if(expr instanceof MethodCall){
            MethodCall MCHammer = (MethodCall)expr;
            boolean worked = true;
            for(int i = 0; i < MCHammer.getArguments().size(); i++){
                Expression arg = MCHammer.getArguments().get(i);
                if (arg.getExprType() != ExpressionType.STRING_LIT) {
                    worked = worked && setVarIDs(varToVal, varToValForArrayComponents, arg);
                    if (worked) {
                        MCHammer.setArgument(i, arg);
                    }
                }
            }
            return worked;
        } else if (expr instanceof IntLit || expr instanceof BoolLit) {
            return true;
        } else {
            throw new RuntimeException("Should be unreachable");
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
        if (old.getIsBreak()) {
            newBlock.setIsBreak(true);
        }
        if (old.visited()) {
            newBlock.visit();
        }
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
        IR_FieldDecl killVar = (IR_FieldDecl) assignLhs.getVarDescriptor().getIR();
        if (assignLhs.getIndex() != null) {
            varToValForArrayComponents.put(killVar, new HashMap<SPSet, ValueID>());
        } else {

            ValueID killValID = varToVal.get(killVar);
            varToVal.remove(killVar);
            if(valToVar.get(killValID)!= null){
                for (Var v : valToVar.get(killValID)) {
                    if (v.getFieldDecl() == killVar) {
                        valToVar.get(killValID).remove(v);
                        break;
                    }
                }
            }
        }

    }

    private void resetGlobals(Map<ValueID, List<Var>> valToVar, Map<IR_FieldDecl, ValueID> varToVal, Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents) {
        for (IR_FieldDecl glob : globalList) {
            Descriptor globDesc = new Descriptor(glob);
            globDesc.setLocation(new LocLabel(glob.getName()));
            ValueID newGlobID = new ValueID();
            List<Var> varList = new ArrayList<Var>();
            varList.add(new Var(globDesc, null));
            ValueID oldID = varToVal.get(glob);
            if (glob.getLength() == null) {
                varToVal.put(glob, newGlobID);
                if (oldID != null) {
                    valToVar.get(oldID).remove(glob);
                }
                valToVar.put(newGlobID, varList);
            } else {
                varToValForArrayComponents.put(glob, new HashMap<SPSet, ValueID>());
                if (oldID == null) {
                    varToVal.put(glob, newGlobID);
                    valToVar.put(newGlobID, varList);
                }
            }
        }
    }

    private void killGlobals(Map<ValueID, List<Var>> valToVar, Map<IR_FieldDecl, ValueID> varToVal, Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents) {
        for (IR_FieldDecl glob : globalList) {
            ValueID oldID = varToVal.get(glob);
            if (glob.getLength() == null) {
                varToVal.remove(glob);
                if (oldID != null) {
                    valToVar.get(oldID).remove(glob);
                }
            } else {
                varToValForArrayComponents.put(glob, new HashMap<SPSet, ValueID>());
            }
        }
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
    public boolean applyCSE (List<START> startsForMethods){
    	boolean anythingReplaced = false;
        for(START initialNode : startsForMethods){
            //Set up tables and lists we'll need. 
            //First thing we should do is reset visits, in case we're called after another optimization.
            initialNode.totalVisitReset(); 
            Set<String> allVarNames = getAllVarNamesInMethod(initialNode);
            Map<IR_FieldDecl, ValueID> varToVal = new HashMap<IR_FieldDecl, ValueID>();
            Map<SPSet, ValueID> expToVal = new HashMap<SPSet, ValueID>();
            Map<SPSet, Var> expToTemp = new HashMap<SPSet, Var>();
            Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents = new HashMap<IR_FieldDecl, Map<SPSet, ValueID>>();
            Map<ValueID, List<Var>> valToVar = new HashMap<ValueID, List<Var>>();
            Map<FlowNode, MapContainer> containerForNode = new HashMap<FlowNode, MapContainer>();
            resetGlobals(valToVar, varToVal, varToValForArrayComponents);
            for(int i = 0; i < initialNode.getArguments().size(); i++){
                IR_FieldDecl arg = initialNode.getArguments().get(i);
                Descriptor argd = new Descriptor(arg);
                context.putSymbol(arg.getName(), argd);

                LocationMem argSrc = Assembler.argLoc(i);
                LocationMem argDst = argSrc;
                if(i<CodegenConst.N_REG_ARG){
                    //save register arguments on the stack
                    context.push(argSrc);
                    argDst = context.getRsp();
                    context.allocLocal(CodegenConst.INT_SIZE);
                }
                argd.setLocation(argDst);
                ValueID parameterID = new ValueID();
                List<Var> paramList = new ArrayList<Var>();
                paramList.add(new Var(argd, null));
                varToVal.put(arg, parameterID);
                valToVar.put(parameterID, paramList);
            }
            MapContainer initialStateContainer = new MapContainer(varToVal, expToVal, expToTemp, varToValForArrayComponents, valToVar, true);
            containerForNode.put(initialNode, initialStateContainer);
            FlowNode firstNodeInProgram = initialNode.getChildren().get(0);
            Set<FlowNode> processing = new LinkedHashSet<FlowNode>();
            processing.add(firstNodeInProgram);
            while(!processing.isEmpty()){ //list of nodes to process
                FlowNode currentNode = processing.iterator().next(); //get node out of set
                processing.remove(currentNode);
                currentNode.visit(); //set its visited attribute so we don't loop back to it
                //Set up the maps for this particular node, regardless of type.
                boolean reset = false;
                MapContainer thisNodeContainer = containerForNode.get(currentNode.getParents().get(0)); //want something we can intersect with, so take first parent's set.
                for(FlowNode parent: currentNode.getParents()){
                    if(containerForNode.get(parent) == null){
                        currentNode.resetVisit();
                        reset = true;
                    } else {
                        if (thisNodeContainer == null) {
                            thisNodeContainer = containerForNode.get(parent).calculateIntersection(containerForNode.get(parent), globalList);
                        } else {
                            thisNodeContainer = thisNodeContainer.calculateIntersection(containerForNode.get(parent), globalList);
                        }
                    }
                }
                if (reset) {
                    processing.addAll(currentNode.getChildren());
                    processing.add(currentNode);
                    if (thisNodeContainer == null) {
                        throw new RuntimeException("HOW DID YOU GET IN PROCESSING IF NONE OF YOUR PARENTS HAVE BEEN PROCESSED");
                    }
                    thisNodeContainer = MapContainer.keepGlobals(thisNodeContainer, globalList);
                    containerForNode.put(currentNode, thisNodeContainer);
                    thisNodeContainer.complete = false;
                    continue;
                }
                boolean changedAtAll = !thisNodeContainer.complete;
                thisNodeContainer.complete = true;
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
                    List<Declaration> tempsUsed = new ArrayList<Declaration>();
                    for(Var current : allTheVarsInBlock){
                        String nextTemp = generateNextTemp(allVarNames);
                        Descriptor d = new Descriptor(new IR_FieldDecl(getTempType(current.getVarDescriptor().getIR().getType()), nextTemp));
                        d.setLocation(new LocLabel(nextTemp));
                        context.putSymbol(nextTemp, d);
                        nextTempHolder.add(nextTemp);
                    }
                    for(Statement currentStatement : cblock.getStatements()){ //for each statement 
                        if(currentStatement instanceof Assignment){
                            Assignment currentAssign = (Assignment)currentStatement; //if assignment, downcast
                            Expression assignExprValue = currentAssign.getValue(); // get expression on rhs
                            Var currentDestVar = currentAssign.getDestVar(); //get the lhs for this assignment
                            if (currentAssign.getOperator() == Ops.ASSIGN_PLUS) {
                                assignExprValue = new AddExpr(currentDestVar, Ops.PLUS, assignExprValue);
                            } else if (currentAssign.getOperator() == Ops.ASSIGN_MINUS) {
                                assignExprValue = new AddExpr(currentDestVar, Ops.MINUS, assignExprValue);
                            }
                            boolean canApply = setVarIDs(varToVal, varToValForArrayComponents, assignExprValue); //set rhs VarIDS if any Vars exist there
                            if (currentDestVar.getIndex() != null) {
                                // make sure array is set up
                                IR_FieldDecl arrayDecl = currentDestVar.getFieldDecl();
                                if(!varToVal.containsKey(arrayDecl)){
                                    ValueID newID = new ValueID();
                                    varToVal.put(arrayDecl, newID);
                                    if (!valToVar.containsKey(newID)) {
                                        valToVar.put(newID, new ArrayList<Var>());
                                    }
                                    valToVar.get(newID).add(new Var(new Descriptor(arrayDecl), null));
                                }
                                Map<SPSet, ValueID> innerMap;
                                if(!varToValForArrayComponents.containsKey(arrayDecl)){
                                    innerMap = new HashMap<SPSet, ValueID>();
                                    varToValForArrayComponents.put(arrayDecl, innerMap);
                                }
                            }
                            if (!canApply) {
                                newCodeblock.addStatement(currentStatement);
                                continue;
                            }
                            ValueID currentValID = new ValueID(); //make a new value ID we'll use when we put things in the map/make a new temp.
                            SPSet rhs = new SPSet(assignExprValue); //Construct an SPSet from the expression.
                            if (rhs.containsMethodCalls()) {
                                killGlobals(valToVar, varToVal, varToValForArrayComponents);
                                if (!setVarIDs(varToVal, varToValForArrayComponents, assignExprValue)) {
                                    // Skipping statement because contains reference to globals AND at least one method call
                                    newCodeblock.addStatement(currentStatement);
                                    resetGlobals(valToVar, varToVal, varToValForArrayComponents);
                                    continue;
                                }
                            }
                            Set<SPSet> keySet = expToVal.keySet(); //Get the keys for the expToVal set.
                            List<Var> varList = valToVar.get(currentValID);
                            if (varList == null) {
                                varList = new ArrayList<Var>();
                                valToVar.put(currentValID, varList);
                            }
                            boolean changed = true; //we want to run repeated checks over the expression.
                            changedAtAll = false;
                            while(changed){ //Until we reach a fixed point
                                changed = false; //say we haven't
                                for (SPSet key : keySet){ //Look at all keys in expToVal
                                    while (rhs.contains(key, valToVar)){ //if we have any of those keys in our current expression
                                        rhs.replace(key, expToVal.get(key), valToVar);
                                        changed = true; //Need to repass over, one substitution could lead to another
                                        changedAtAll = true;
                                        anythingReplaced = true;
                                    }
                                }
                            }
                            newCodeblock.addStatement(new Assignment(currentDestVar, Ops.ASSIGN, rhs.toExpression(valToVar))); //put the optimized expression in the codeblock
                            // kill old mapping only after doing the assignment (j = j + 1 should use the old value of j on the right side)
                            IR_FieldDecl lhs = (IR_FieldDecl)currentDestVar.getVarDescriptor().getIR();
                            killMappings(currentDestVar, varToValForArrayComponents, varToVal, valToVar); //kill all newly invalid mappings and handle fixing ArrayComponent stuff
                            IR_FieldDecl rhsTempDecl = null;
                            // Update valToVar for temp, expToVal for expression, and expToTemp
                            if (!keySet.contains(rhs)){ //If the rhs is something new that we haven't seen yet,
                                if(((rhs.SPSets.size() +rhs.intSet.size() +rhs.boolSet.size() + rhs.ternSet.size() + rhs.comparisons.size() + rhs.varSet.size()) > 1 
                                        || (rhs.SPSets.size() + rhs.ternSet.size() + rhs.comparisons.size() != 0)) && rhs.methodCalls.size() == 0){
                                    expToVal.put(rhs, currentValID); // put the rhs in the expToVal table with the ID we made earlier
                                    //Next line creates a new IR_FieldDecl for the compiler-generated temp, and makes the temp equal the assigned variable above.
                                    //So if we had a = x + y, we now have a temp value temp1 = a.
                                    rhsTempDecl = new IR_FieldDecl(getTempType(currentDestVar.getVarDescriptor().getType()), nextTempHolder.remove(0));
                                    Descriptor tempDescriptor = context.findSymbol(rhsTempDecl.getName());
                                    Var tempVar = new Var(tempDescriptor, null, true);
                                    expToTemp.put(rhs, tempVar);
                                    varList.add(tempVar);
                                    tempsUsed.add(new Declaration((IR_FieldDecl) tempDescriptor.getIR()));
                                }
                            }
                            // Update valToVar to dest
                            if (currentDestVar.getIndex() == null || setVarIDs(varToVal, varToValForArrayComponents, currentDestVar.getIndex())) {
                                varList.add(currentDestVar);
                                if(currentDestVar.getIndex() == null){ //Changed this from != when simulating execution. 
                                    varToVal.put(lhs, currentValID); 
                                }
                                else{
                                    boolean indexChanged = true;
                                    SPSet indexSet = new SPSet(currentDestVar.getIndex());
                                    boolean madeChange = false;
                                    while(indexChanged){ //Until we reach a fixed point
                                        indexChanged = false; //say we haven't
                                        for (SPSet key : keySet){ //Look at all keys in expToVal
                                            while (indexSet.contains(key, valToVar)){ //if we have any of those keys in our current expression
                                                indexSet.replace(key, expToVal.get(key), valToVar);
                                                indexChanged = true; //Need to repass over, one substitution could lead to another
                                                madeChange = true;
                                            }
                                        }
                                    }
                                    if(!varToVal.containsKey(lhs)){
                                        ValueID newID = new ValueID();
                                        varToVal.put(lhs, newID);
                                        if (!valToVar.containsKey(newID)) {
                                            valToVar.put(newID, new ArrayList<Var>());
                                        }
                                        valToVar.get(newID).add(new Var(new Descriptor(lhs), null));
                                    }
                                    Map<SPSet, ValueID> innerMap;
                                    if(varToValForArrayComponents.containsKey(lhs)){
                                        innerMap = varToValForArrayComponents.get(lhs);
                                    }
                                    else{
                                        innerMap = new HashMap<SPSet, ValueID>();
                                        varToValForArrayComponents.put(lhs, innerMap);
                                    }
                                    innerMap.put(indexSet, currentValID);
                                    if (madeChange) {
                                        currentDestVar.setIndex(indexSet.toExpression(valToVar));
                                    }
                                }
                            }
                            if(rhsTempDecl != null){
                                newCodeblock.addStatement(new Assignment(expToTemp.get(rhs), Ops.ASSIGN, currentDestVar)); //t1 = previous variable
                            }
                            if (rhs.containsMethodCalls() 
                                    || (currentDestVar.getIndex() != null && (!setVarIDs(varToVal, varToValForArrayComponents, currentDestVar.getIndex()) 
                                            || (new SPSet(currentDestVar.getIndex())).containsMethodCalls()))) {
                                resetGlobals(valToVar, varToVal, varToValForArrayComponents);
                            }
                        }

                        else if(currentStatement instanceof MethodCallStatement){ //if method call or declaration, just put it in the new block
                            MethodCallStatement mcs = (MethodCallStatement)currentStatement;
                            List<Expression> args = mcs.getMethodCall().getArguments();
                            for(int i = 0; i < args.size(); i++){
                                if(args.get(i).getExprType() != ExpressionType.STRING_LIT){
                                    Expression expr = args.get(i);
                                    boolean worked = setVarIDs(varToVal, varToValForArrayComponents, expr);
                                    if (worked) {
                                        boolean changed = true; //we want to run repeated checks over the expression.
                                        SPSet arg = new SPSet(expr);
                                        if (arg.containsMethodCalls()) {
                                            killGlobals(valToVar, varToVal, varToValForArrayComponents);
                                            if (!setVarIDs(varToVal, varToValForArrayComponents, expr)) {
                                                // Skipping argument because contains reference to globals AND at least one method call
                                                resetGlobals(valToVar, varToVal, varToValForArrayComponents);
                                                continue;
                                            }
                                        }
                                        while(changed){ //Until we reach a fixed point
                                            changed = false; //say we haven't
                                            for (SPSet key : expToVal.keySet()){ //Look at all keys in expToVal
                                                while (arg.contains(key, valToVar)){ //if we have any of those keys in our current expression
                                                    arg.replace(key, expToVal.get(key), valToVar);
                                                    changed = true; //Need to repass over, one substitution could lead to another
                                                    changedAtAll = true;
                                                    anythingReplaced = true;
                                                }
                                            }
                                        }
                                        if (changedAtAll) {
                                            mcs.getMethodCall().setArgument(i, arg.toExpression(valToVar));
                                        }
                                    }
                                }
                            }
                            newCodeblock.addStatement(mcs);
                            resetGlobals(valToVar, varToVal, varToValForArrayComponents);

                        } else{
                            newCodeblock.addStatement(currentStatement);
                        }
                    }

                    swapCodeblocks(cblock, newCodeblock);
                    newCodeblock.visit();
                    Codeblock topOfScope = findTopOfScope(newCodeblock, containerForNode);
                    if (topOfScope != newCodeblock ) {
                        processing.add(topOfScope);
                    }
                    MapContainer currentNodeContainer = new MapContainer(varToVal, expToVal, expToTemp, varToValForArrayComponents, valToVar, true);
                    containerForNode.put(newCodeblock, currentNodeContainer);
                    containerForNode.remove(cblock);
                    anythingReplaced = changedAtAll || anythingReplaced || tempsUsed.size() > 0;
                    if(changedAtAll){
                        for(FlowNode child : newCodeblock.getChildren()){
                            processing.add(child);
                        }
                    }
                    else{
                        for(FlowNode child : newCodeblock.getChildren()){
                            if(!child.visited()){
                                processing.add(child);
                            }
                        }
                    }
                    
                    for (Declaration newTemp : tempsUsed) {
                        topOfScope.prependDeclaration(newTemp);
                    }
                }

                else if (currentNode instanceof Branch){
                    Branch cbranch = (Branch)currentNode;
                    Expression branchExpr = cbranch.getExpr();
                    boolean worked = setVarIDs(varToVal, varToValForArrayComponents, branchExpr);
                    if (worked) {
                        SPSet branchExprSP = new SPSet(cbranch.getExpr());
                        boolean changed = true; //we want to run repeated checks over the expression.
                        while(changed){ //Until we reach a fixed point
                            changed = false; //say we haven't
                            for (SPSet key : expToVal.keySet()){ //Look at all keys in expToVal
                                while (branchExprSP.contains(key, valToVar)){ //if we have any of those keys in our current expression
                                    branchExprSP.replace(key, expToVal.get(key), valToVar);
                                    changed = true; //Need to repass over, one substitution could lead to another
                                    changedAtAll = true;
                                }
                            }
                        }
                        anythingReplaced = changedAtAll || anythingReplaced;
                        if(changedAtAll){ //don't do anything if we never changed the expr, no need to do busywork
                            cbranch.setExpr(branchExprSP.toExpression(valToVar)); //in place modification on block. No need to make a new one.
                        }
                    }
                    containerForNode.put(currentNode, thisNodeContainer);
                    if(changedAtAll){
                        for(FlowNode child : currentNode.getChildren()){
                            processing.add(child);
                        }
                    }
                    else{
                        for(FlowNode child : currentNode.getChildren()){
                            if(!child.visited()){
                                processing.add(child);
                            }
                        }
                    }
                }

                else if(currentNode instanceof NoOp){
                    currentNode.visit();
                    if(changedAtAll){
                        for(FlowNode child : currentNode.getChildren()){
                            processing.add(child);
                        }
                    }
                    else{
                        for(FlowNode child : currentNode.getChildren()){
                            if(!child.visited()){
                                processing.add(child);
                            }
                        }
                    }
                    containerForNode.put(currentNode, thisNodeContainer);

                }

                else if(currentNode instanceof START){
                    currentNode.visit();
                    if(changedAtAll){
                        for(FlowNode child : currentNode.getChildren()){
                            processing.add(child);
                        }
                    }
                    else{
                        for(FlowNode child : currentNode.getChildren()){
                            if(!child.visited()){
                                processing.add(child);
                            }
                        }
                    }
                    containerForNode.put(currentNode, thisNodeContainer);
                }

                else if(currentNode instanceof END){
                    END theEnd = (END)currentNode;
                    Expression returnExpr = theEnd.getReturnExpression();
                    if(returnExpr != null){
                        boolean canOpt = setVarIDs(varToVal, varToValForArrayComponents, returnExpr);
                        if (canOpt) {
                            SPSet retSP = new SPSet(returnExpr);
                            boolean changed = true; //we want to run repeated checks over the expression.
                            while(changed){ //Until we reach a fixed point
                                changed = false; //say we haven't
                                for (SPSet key : expToVal.keySet()){ //Look at all keys in expToVal
                                    while (retSP.contains(key, valToVar)){ //if we have any of those keys in our current expression
                                        retSP.remove(key, valToVar); //remove it
                                        retSP.addToVarSet(expToVal.get(key)); //replace it with the already-computed value. 
                                        changed = true; //Need to repass over, one substitution could lead to another
                                        changedAtAll = true;
                                    }
                                }
                            }
                            anythingReplaced = changedAtAll || anythingReplaced;

                            if(changedAtAll){
                                theEnd.setReturnExpression(retSP.toExpression(valToVar));
                            }
                        }
                    }
                    containerForNode.put(currentNode, thisNodeContainer);
                }
            }
            initialNode.resetVisit();
        }
        //return Assembler.generateProgram(calloutList, globalList, flowNodes);
        return anythingReplaced;
    }

    private Codeblock findTopOfScope(Codeblock cblock, Map<FlowNode, MapContainer> nodeToContainer) {
        FlowNode currentNode = cblock;
        while (!(currentNode instanceof START)) {
            if (currentNode instanceof END) {
                throw new RuntimeException("this should have been impossible");
            } else if (currentNode instanceof Codeblock) {
                currentNode = currentNode.getParents().get(0);
            } else if (currentNode instanceof Branch) {
                currentNode = findUpperParent((Branch) currentNode);
            } else if (currentNode instanceof NoOp) {
                currentNode = findBranch((NoOp) currentNode);
            } else {
                throw new RuntimeException("missing case");
            }
        }
        FlowNode nextChild = currentNode.getChildren().get(0);
        Codeblock topOfScope;
        if (nextChild instanceof Codeblock) {
            topOfScope = (Codeblock) nextChild;
        } else {
            topOfScope = new Codeblock();
            topOfScope.addParent(currentNode);
            ((START) currentNode).removeChild(nextChild);
            currentNode.addChild(topOfScope);
            topOfScope.addChild(nextChild);
            nextChild.replaceParent(topOfScope, currentNode);
            MapContainer startContainer = nodeToContainer.get(currentNode);
            MapContainer newContainer = startContainer.calculateIntersection(startContainer, globalList);
            nodeToContainer.put(topOfScope, newContainer);
        }
        return topOfScope;
    }
    
    private Branch findBranch(NoOp nop) {
        for (FlowNode p : nop.getParents()) {
            if (p instanceof Branch) {
                // must have been the nop of a while/for
                return (Branch) p;
            }
        }
        if (nop.getParents().size() > 2) {
            throw new RuntimeException("I think I'm an IF-NOP, but I have more than two parents");
        }
        FlowNode next = nop.getParents().get(0);
        while (!(next instanceof START)) {
            if (next instanceof Codeblock) {
                next = next.getParents().get(0);
            } else if (next instanceof Branch) {
                next = findUpperParent((Branch) next);
            } else if (next instanceof END) {
                throw new RuntimeException("this should never occur");
            } else if (next instanceof NoOp) {
                next = findBranch((NoOp) next);
            } else {
                throw new RuntimeException("missing case");
            }
        }
        if (next.getParents().size() != 1 || !(next.getParents().get(0) instanceof Branch)) {
            throw new RuntimeException("This start should've had one branch for its parent");
        }
        return (Branch) next.getParents().get(0);
    }
    
    private FlowNode findUpperParent(Branch br) {
        if (br.getType() == BranchType.IF) {
            if (br.getParents().size() != 1) {
                throw new RuntimeException("IF branches should have exactly one parent");
            }
            return br.getParents().get(0);
        }
        List<FlowNode> processing = new ArrayList<FlowNode>();
        Set<FlowNode> nodesInTrueBranch = new HashSet<FlowNode>();
        processing.add(br.getTrueBranch());
        nodesInTrueBranch.add(br.getFalseBranch());
        while (!processing.isEmpty()) {
            FlowNode current = processing.remove(0);
            for (FlowNode c : current.getChildren()) {
                if (nodesInTrueBranch.add(c)) {
                    processing.add(c);
                }
            }
        }
        for (FlowNode p : br.getParents()) {
            if (!nodesInTrueBranch.contains(p)) {
                return p;
            }
        }
        throw new RuntimeException("No upper parent found");
    }
    
    private void clearUnusedDeclarations(START beginMethod){
        Set<FlowNode> seen = new HashSet<FlowNode>();
        Set<IR_FieldDecl> assigned = getAllFieldDeclsInMethod(beginMethod);
        List<FlowNode> processing = new ArrayList<FlowNode>();
        processing.add(beginMethod.getChildren().get(0));
        seen = new HashSet<FlowNode>();
        while (!processing.isEmpty()) {
            FlowNode current = processing.remove(0);
            if (seen.add(current)) {
                processing.addAll(current.getChildren());
            }
            if (current instanceof Codeblock) {
                Codeblock cBlock = (Codeblock) current;
                List<Declaration> toRemove = new ArrayList<Declaration>();
                for (Statement s : cBlock.getStatements()) {
                    if (s instanceof Declaration) {
                        Declaration decl = (Declaration) s;
                        if (!assigned.contains(decl.getFieldDecl())) {
                            toRemove.add(decl);
                        }
                    }
                }
                for (Declaration d : toRemove) {
                    cBlock.statements.remove(d);
                }
            }
        }
    }
}
