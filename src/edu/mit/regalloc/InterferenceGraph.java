package edu.mit.regalloc;

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
import java.util.Stack;

import edu.mit.compilers.controlflow.Assignment;
import edu.mit.compilers.controlflow.Bitvector;
import edu.mit.compilers.controlflow.Branch;
import edu.mit.compilers.controlflow.Codeblock;
import edu.mit.compilers.controlflow.ControlflowContext;
import edu.mit.compilers.controlflow.Declaration;
import edu.mit.compilers.controlflow.END;
import edu.mit.compilers.controlflow.Expression;
import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.MethodCallStatement;
import edu.mit.compilers.controlflow.NoOp;
import edu.mit.compilers.controlflow.Optimizer;
import edu.mit.compilers.controlflow.START;
import edu.mit.compilers.controlflow.Statement;
import edu.mit.compilers.controlflow.Var;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;

public class InterferenceGraph {
	
	private Optimizer optimizer;
	private List<GraphNode> nodes = new ArrayList<GraphNode>();
	private HashMap<String, Stack<GraphNode>> varToNodes;
	
	private HashMap<GraphNode, Set<GraphNode>> adjList = new HashMap<GraphNode, Set<GraphNode>>();
	private HashSet<HashSet<GraphNode>> bitMatrix = new HashSet<HashSet<GraphNode>>();
	
	private ControlflowContext context;
	private List<IR_MethodDecl> calloutList;
	private List<IR_FieldDecl> globalList;
	private HashMap<String, START> flowNodes;
	
	public InterferenceGraph(ControlflowContext context, 
		List<IR_MethodDecl> callouts, List<IR_FieldDecl> globals, HashMap<String, START> flowNodes){
		this.context = context;
		this.calloutList = callouts;
		this.globalList = globals;
		this.flowNodes = flowNodes;
		this.optimizer = new Optimizer(context, callouts, globals, flowNodes);
	}
	
	private void setupGlobals() {
		varToNodes = new HashMap<String, Stack<GraphNode>>();
		for(IR_FieldDecl global : globalList) {
			Stack<GraphNode> graphNodes = new Stack<GraphNode>();
			graphNodes.push(new GraphNode(global.getName(), true, false));
			varToNodes.put(global.getName(), graphNodes);
		}
	}
	
	private void setupParams(START node) {
		setupGlobals();
		for(IR_FieldDecl parameter : node.getArguments()){
			Stack<GraphNode> graphNodes = new Stack<GraphNode>();
			graphNodes.push(new GraphNode(parameter.getName(), false, true));
			varToNodes.put(parameter.getName(), graphNodes);
		}
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
	 * @param 
	 * @return 
	 */
	public Map<FlowNode, Bitvector> generateLivenessMap(){
		Map<FlowNode, Bitvector> vectorStorageIN = new HashMap<FlowNode, Bitvector>(); //set up place to store maps for input from children
		Map<FlowNode, Bitvector> vectorStorageOUT = new HashMap<FlowNode, Bitvector>(); //set up place to store maps for output from block.
		Map<FlowNode, Integer> ticksForRevisit = new HashMap<FlowNode, Integer>();
		for(START methodStart : flowNodes.values()){
			//First things first: We will be called from DCE or another optimization, so reset visits before we do anything else.
			methodStart.resetVisit();
			List<FlowNode> scanning = new ArrayList<FlowNode>(); //Need to find all the ENDs before we can do anything more.
			scanning.add(methodStart);
			Set<END> endNodes = new LinkedHashSet<END>();
			Set<String> allVars = optimizer.getAllVarNamesInMethod(methodStart); //Get all the var names from within the method
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
				methodStart.resetVisit(); //Need to fix the visits since we just tampered with them.
				for(FlowNode node : vectorStorageIN.keySet()){
					ticksForRevisit.put(node, 0); //set up a ticker so we can see if we want to do revisits
				}
				Bitvector liveVector = vectorStorageIN.get(initialNode); //set up the bitvector. Initialized to any current values.
				if(initialNode.getReturnExpression() != null){
					for(Var returnVar : optimizer.getVarsFromExpression(initialNode.getReturnExpression())){
						liveVector.setVectorVal(returnVar.getName(), 1); //things returned must be alive on exit, so set their vector to 1
					}
				}
				//Since we move from top to bottom, OUT is what propagates upward, where it is part of the IN of the next block.
				vectorStorageOUT.put(initialNode, liveVector.copyBitvector().vectorUnison(vectorStorageOUT.get(initialNode)));
				//Now we've set up everything from the end of the program, assuming working on only one END at a time. Now we walk backwards. 
				List<FlowNode> processing = new ArrayList<FlowNode>();
				processing.addAll(initialNode.getParents());
				FlowNode previousNode = initialNode;
				while(!processing.isEmpty()){
					FlowNode currentNode = processing.remove(0);
					currentNode.visit();
					if(currentNode.getChildren().size() == 1){
						liveVector = vectorStorageOUT.get(currentNode.getChildren().get(0)).copyBitvector();
					}
					else{
						liveVector = Bitvector.childVectorUnison(currentNode.getChildren(), vectorStorageOUT, vectorStorageIN.get(currentNode));
						System.err.println("We are processing a node with more than one child.");
					}
					Bitvector previousIN = vectorStorageIN.get(currentNode).copyBitvector();
					vectorStorageIN.put(currentNode, liveVector.copyBitvector().vectorUnison(vectorStorageIN.get(currentNode)));
					boolean needToReprocessFlag = previousIN.compareBitvectorEquality(vectorStorageIN.get(currentNode));
					boolean skipNode = false;
					if(!needToReprocessFlag){
						if(ticksForRevisit.get(currentNode).equals(0)){
							System.err.println("The current node's IN has not changed since last processing. We will compute at most twice to be safe.");
							ticksForRevisit.put(currentNode, 1);
						}
						else if(ticksForRevisit.get(currentNode).equals(1)){
							System.err.println("The current node's IN has not changed since last processing, and has been processed once. One more computation to be safe.");
							ticksForRevisit.put(currentNode, 2);
						}
						else if(ticksForRevisit.get(currentNode).equals(2)){
							System.err.println("We have processed this node at least twice, and no changes have occurred to its IN. No need to reprocess.");
							skipNode = true;
						}
					}
					if (previousNode == currentNode){
						ticksForRevisit.put(currentNode, ticksForRevisit.get(currentNode)+1);
					}
					if(skipNode){
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
								String lhs = assign.getDestVar().getName();
								List<String> changedVectorEntry = new ArrayList<String>();
								List<Var> varsInRHS = optimizer.getVarsFromExpression(assign.getValue());
								List<String> rhsNames = new ArrayList<String>();
								//LEFT HAND FIRST LOGIC
								if(liveVector.get(lhs) == 1){ //If this is alive, MAY need to flip the bit
									for(Var varia : varsInRHS){
										String varName = varia.getName();
										liveVector.setVectorVal(varName, 1); //rhs if we changed it is not alive, because the assignment as a whole is dead.
										System.err.printf("Bitvector entry for variable %s has been set to 1 in building phase due to use in live assigment." + System.getProperty("line.separator"), varName);
										rhsNames.add(varName);
									}
									if(!rhsNames.contains(lhs)){
										liveVector.setVectorVal(lhs, 0);
										System.err.printf("Bitvector entry for variable %s has been flipped from 1 to 0 in building phase by an assignment that does not expose an upwards use." + System.getProperty("line.separator"), lhs);
									}
									else{
										System.err.printf("Bitvector entry for variable %s has not been flipped and remains 1 due to exposed upward use in RHS.", lhs);
									}
								}
							}

							/**
							 * TODO : Reason this is commented out: Consider the following scenario being read from bottom to top:
							 * 
							 * int x;
							 * x = 5;
							 * z = x + 9;
							 * 
							 * When we see x on the rhs of the definition for Z, X would be alive. 
							 * When we then see the definition for X, we'd set x to 0 in the bitvector.
							 * Then we'd hit the declaration for x. The bit vector is 0 for x, so with this type of mentality, 
							 * we'd delete the declaration! This is bad, so don't do it! Maybe just leave declarations alone?
							 * Probably not worth the complexity increase to deal with them, actually...Would need to seperately track an 
							 * "if ever used" quality and use that instead; if we ever set the vector to 1, then ifEverUsed becomes true.
							 * If we see the declaration, check ifEverUsed ; if true, leave decl alone, else, remove it. Would get iffy around branches. 
							 */
							else if(currentState instanceof Declaration){ 
								//if var declared isn't ever alive, could remove the decl; but have to make sure... 
							}

							else if(currentState instanceof MethodCallStatement){ //set liveness vectors for the args
								MethodCallStatement mcall = (MethodCallStatement) currentState;
								List<Expression> args = mcall.getMethodCall().getArguments();
								Set<Var> varsInArgs = new LinkedHashSet<Var>();
								for(Expression expr : args){
									varsInArgs.addAll(optimizer.getVarsFromExpression(expr));
								}
								for(Var varia : varsInArgs){
									liveVector.setVectorVal(varia.getName(), 1); //If not already alive, mark an argument as alive. 
									System.err.printf("Bitvector entry for variable %s has been set to 1 in building phase by a method call." + System.getProperty("line.separator"), varia.getName());
								}
							}
							currentState.setLiveMap(liveVector.copyBitvector());
						}
						Collections.reverse(statementList); //We reversed the list to iterate through it backwards, so fix it before we move on!
					}

					else if (currentNode instanceof Branch){ //join point. Live vector would be handled before we got here by unisonVector, so assume we're golden.
						Branch cBranch = (Branch)currentNode;
						for(Var varia : optimizer.getVarsFromExpression(cBranch.getExpr())){
							liveVector.setVectorVal(varia.getName(), 1); //anything showing up in a branch expression is used by definition, otherwise prog is invalid.
							System.err.printf("Just set variable %s 's bitvector to 1 in building phase due to usage in a branch condition." + System.getProperty("line.separator"), varia.getName());
						}
						cBranch.setLiveMap(liveVector.copyBitvector());
					}

					else if(currentNode instanceof NoOp){ //split point. No expr here, so don't have to scan it.
						//Do nothing, just move onward to post-block processing.
					}

					else if(currentNode instanceof START){
						START cStart = (START)currentNode;
						List<IR_FieldDecl> args = cStart.getArguments();
						for (IR_FieldDecl arg : args){
							liveVector.setVectorVal(arg.getName(), 1); 
							System.err.printf("Just set variable %s 's bitvector to 1 in building phase due to a START." + System.getProperty("line.separator"), arg.getName());
						}
						cStart.setLiveMap(liveVector.copyBitvector());
					}

					else if(currentNode instanceof END){
						END cEnd = (END)currentNode;
						if(cEnd.getReturnExpression() != null){
							for(Var varia : optimizer.getVarsFromExpression(cEnd.getReturnExpression())){
								liveVector.setVectorVal(varia.getName(), 1);
								System.err.printf("Just set variable %s 's bitvector to 1 in building phase. due to an END." + System.getProperty("line.separator"), varia.getName());
							}
						}
						cEnd.setLiveMap(liveVector.copyBitvector());
					}
					
					boolean changed;
					Bitvector previousOut = vectorStorageOUT.get(currentNode).copyBitvector();
					Bitvector newOut = liveVector.vectorUnison(previousOut);
					changed = previousOut.compareBitvectorEquality(newOut);
					vectorStorageOUT.put(currentNode, newOut);
					if(!changed){
						System.err.println("Finished processing a FlowNode whose bitvector OUT did not change.");
						for(FlowNode parent : currentNode.getParents()){
							if(!parent.visited()){
								processing.add(parent);
							}
						}
					}
					else{
						if(currentNode instanceof START || currentNode instanceof NoOp){
							ticksForRevisit.put(currentNode, ticksForRevisit.get(currentNode)+1);
						}
						System.err.println("Finished processing a FlowNode whose bitvector OUT did change; Will now visit all parents.");
						for(FlowNode parent : currentNode.getParents()){
							processing.add(parent);
						}
					}
				
				previousNode = currentNode;
				}
			}
			methodStart.resetVisit(); //fix all the visited nodes before we go to next START.
		}
		return vectorStorageIN;
	}
	
	private void addEdge(GraphNode from, GraphNode to) {
		if (!adjList.containsKey(from)) {
			adjList.put(from, new HashSet<GraphNode>(Arrays.asList(to)));
		} else {
			adjList.get(from).add(to);
		}
		// Add reverse
		if (!adjList.containsKey(to)) {
			adjList.put(to, new HashSet<GraphNode>(Arrays.asList(from)));
		} else {
			adjList.get(to).add(from);
		}
		bitMatrix.add(new HashSet<GraphNode>(Arrays.asList(from, to)));
	}
	
	private void removeEdge(GraphNode from, GraphNode to) {
		Set<GraphNode> nodeList = adjList.get(from);
		nodeList.remove(to);
		bitMatrix.remove(new HashSet<GraphNode>(Arrays.asList(from, to)));
	}
	
	private List<String> getLiveVars(Bitvector liveMap) {
		List<String> liveVars = new ArrayList<String>(); 
		for (String var : liveMap.getVectorMap().keySet()) {
			if (liveMap.get(var) == 1) {
				liveVars.add(var);
			}
		}
		return liveVars;
	}
	
	private void addEdges(GraphNode node, Bitvector liveMap) {
		List<String> liveVars = getLiveVars(liveMap);
		String curVar = node.getVarName();
		System.out.println("live vars: " + liveVars);
		for (String var : liveVars) {
			GraphNode varNode = null;
			if (varToNodes.containsKey(var)) {
				if (var.equals(curVar)) {
					// get second to last element (since last element is the current node)
					// TODO: Rewriting to same var can be assigned the same register
					System.out.println("Same variable! VarToNodes has size: " + varToNodes.get(var).size());
					varNode = varToNodes.get(var).get(varToNodes.get(var).size()-2); 
				} else {
					varNode = varToNodes.get(var).lastElement();
				}
			} else {
				throw new RuntimeException("The live variable " + var + " is not found in the map. What happened??");
			}
			addEdge(node, varNode);
		}
		if (!adjList.containsKey(node)) {
			adjList.put(node, new HashSet<GraphNode>());
		}
	}
	
	public void buildGraph() {
		for (START initialNode : flowNodes.values()) {
			setupParams(initialNode);
			Set<Codeblock> listOfCodeblocks = new LinkedHashSet<Codeblock>();
			List<FlowNode> scanning = new ArrayList<FlowNode>(); //Need to find all the Codeblocks
			scanning.add(initialNode);
			while(!scanning.isEmpty()){ //scan through all nodes and create listing.
				FlowNode currentNode = scanning.remove(0);
				currentNode.visit();
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
			
			for (Codeblock cblock : listOfCodeblocks){
				List<Statement> statementList = cblock.getStatements();
				Iterator<Statement> statementIter = statementList.iterator();
				while(statementIter.hasNext()){
					Statement st = statementIter.next();
					Bitvector liveMap = st.getLiveMap();
					if (st instanceof Assignment){
						Assignment assignment = (Assignment) st;
						GraphNode node = new GraphNode(assignment);
						assignment.setNode(node);
						String varName = assignment.getDestVar().getName();
						System.out.println("Variable being assigned: " + varName);
						if (varToNodes.containsKey(varName)) {
							varToNodes.get(varName).push(node);
						} else {
							Stack<GraphNode> varNodes = new Stack<GraphNode>();
							varNodes.push(node);
							varToNodes.put(varName, varNodes);
						}
						nodes.add(node);
						addEdges(node, liveMap);
					}
				}
			}
		}
	}
	
	public List<GraphNode> getNodes() {
		return nodes;
	}
	
	public int getNumEdges(GraphNode node) {
		if (adjList.get(node).size() == 0)
			return 0;
		int count = 0;
		for (GraphNode curNode : adjList.get(node)) {
			if (!curNode.isRemoved())
				count++;
		}
		return count;
	}
	
	public HashMap<GraphNode, Set<GraphNode>> getAdjList() {
		return adjList;
	}
	
	public HashSet<HashSet<GraphNode>> getBitMatrix() {
		return bitMatrix;
	}

}
