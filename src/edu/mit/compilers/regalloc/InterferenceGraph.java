package edu.mit.compilers.regalloc;

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
import edu.mit.compilers.ir.Ops;

public class InterferenceGraph {
	
	private List<GraphNode> nodes = new ArrayList<GraphNode>();
	
	private HashMap<GraphNode, Set<GraphNode>> adjList = new HashMap<GraphNode, Set<GraphNode>>();
	private HashSet<HashSet<GraphNode>> bitMatrix = new HashSet<HashSet<GraphNode>>();
	
	private HashMap<Web, GraphNode> webToNode = new HashMap<Web, GraphNode>();
	
	private ControlflowContext context;
	private List<IR_MethodDecl> calloutList;
	private List<IR_FieldDecl> globalList;
	private HashMap<String, START> flowNodes;
	private Optimizer optimizer;
	
	public InterferenceGraph(ControlflowContext context, 
		List<IR_MethodDecl> callouts, List<IR_FieldDecl> globals, HashMap<String, START> flowNodes){
		this.context = context;
		this.calloutList = callouts;
		this.globalList = globals;
		this.flowNodes = flowNodes;
		this.optimizer = new Optimizer(context, callouts, globals, flowNodes);
	}
	/*
	private void setupGlobals() {
		varToNodes = new HashMap<String, HashMap<Integer, GraphNode>>();
		for(IR_FieldDecl global : globalList) {
			HashMap<Integer, GraphNode> graphNodes = new HashMap<Integer, GraphNode>();
			graphNodes.put(0, new GraphNode(global.getName(), 0, true, false));
			varToNodes.put(global.getName(), graphNodes);
		}
	}
	
	private void setupParams(START node) {
		setupGlobals();
		for(IR_FieldDecl parameter : node.getArguments()){
			HashMap<Integer, GraphNode> graphNodes = new HashMap<Integer, GraphNode>();
			graphNodes.put(1, new GraphNode(parameter.getName(), 1, true, false));
			varToNodes.put(parameter.getName(), graphNodes);
		}
	}
	*/
 
	private List<IR_FieldDecl> getLiveVars(Bitvector liveMap) {
		List<IR_FieldDecl> liveVars = new ArrayList<IR_FieldDecl>(); 
		for (IR_FieldDecl decl : liveMap.getVectorMap().keySet()) {
			if (liveMap.get(decl) == 1) {
				liveVars.add(decl);
			}
		}
		return liveVars;
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
	public Map<START, Map<FlowNode, Bitvector>> generateLivenessMap(){
		Map<START, Map<FlowNode, Bitvector>> liveStorage = new HashMap<START, Map<FlowNode, Bitvector>>();
		for(START methodStart : flowNodes.values()){
			Map<FlowNode, Integer> ticksForRevisit = new HashMap<FlowNode, Integer>();
			Map<FlowNode, Bitvector> vectorStorageIN = new HashMap<FlowNode, Bitvector>(); //set up place to store maps for input from children
			Map<FlowNode, Bitvector> vectorStorageOUT = new HashMap<FlowNode, Bitvector>(); //set up place to store maps for output from blocks
			//First things first: We will be called from DCE or another optimization, so reset visits before we do anything else.
			methodStart.totalVisitReset();
			List<FlowNode> scanning = new ArrayList<FlowNode>(); //Need to find all the ENDs before we can do anything more.
			scanning.add(methodStart);
			Set<END> endNodes = new LinkedHashSet<END>();
			Set<IR_FieldDecl> allVars = optimizer.getAllFieldDeclsInMethod(methodStart);
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
					for(IR_FieldDecl returnVar : optimizer.getVarIRsFromExpression(initialNode.getReturnExpression())){
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
								// EDIT BY YOUYANG:
								System.out.println("Live vars: " + getLiveVars(liveVector));
								currentState.setLiveMap(liveVector.copyBitvector());
								// END EDIT BY YOUYANG
								Assignment assign = (Assignment)currentState;
								IR_FieldDecl lhs = assign.getDestVar().getFieldDecl();
								List<IR_FieldDecl> varsInRHS = optimizer.getVarIRsFromExpression(assign.getValue());
								Set<IR_FieldDecl> rhsDecls = new LinkedHashSet<IR_FieldDecl>();
								//LEFT HAND FIRST LOGIC
								if(liveVector.get(lhs) == 1 || (liveVector.get(lhs) == 0 && optimizer.containsMethodCall(assign.getValue()))){ //If this is alive, MAY need to flip the bit
									for(IR_FieldDecl varDecl : varsInRHS){
										liveVector.setVectorVal(varDecl, 1); //rhs if we changed it is not alive, because the assignment as a whole is dead.
										//System.err.printf("Bitvector entry for variable %s has been set to 1 in building phase due to use in live assigment OR one with method call." + System.getProperty("line.separator"), varName);
										rhsDecls.add(varDecl);
									}
									if(assign.getDestVar().getIndex() != null){
										for (IR_FieldDecl index : optimizer.getVarIRsFromExpression(assign.getDestVar().getIndex()))
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
									varsInArgs.addAll(optimizer.getVarIRsFromExpression(expr));
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
						for(IR_FieldDecl varDecl : optimizer.getVarIRsFromExpression(cBranch.getExpr())){
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
							for(IR_FieldDecl varDecl : optimizer.getVarIRsFromExpression(cEnd.getReturnExpression())){
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
							if(!parent.visited()){
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
	
	private boolean addEdges(Web curWeb, GraphNode node, Statement st) {
		boolean done = true;
		ReachingDefinition rd = node.getReachingDefinition();
		for (Web web : rd.getAllWebs()) {
			if (!curWeb.equals(web)) {
				if (!webToNode.containsKey(web)) {
					// This happens when the RD contains a web that has not been put in the webToNode map yet. For example:
					// int a,b,c;
					// for (b=0, 10) {
					//   a = 1;  <-- RD contains web of c, even though it hasn't been put in map yet.
					//   c = 1;
					// }
					// Thus, we need to process it later.
					System.out.println("We do not yet have the web for var: " + web.getFieldDecl().getName() + ". Need to reprocess.");
					done = false;
					continue;
					//throw new RuntimeException("webToNode should always contain the key for web: " + web.getFieldDecl().getName() + ". " + web);
				}
				System.out.println("Considering adding edge to var: " + web.getFieldDecl().getName());
				System.out.println("Live map: " + getLiveVars(st.getLiveMap()));
				GraphNode otherNode = webToNode.get(web);
				if (st.getLiveMap().get(web.getFieldDecl()) == 1) {
					// Variable will be live later, so we want to add an edge to that web
					System.out.println("Live web. Adding edge");
					addEdge(node, otherNode);
				} else {
					System.out.println("Dead web.");
				}
				
			}
		}
		// if no edges are added, we must still put an empty HashSet to adjList
		if (!adjList.containsKey(node)) {
			adjList.put(node, new HashSet<GraphNode>());
		}
		return done;
	}
	
	/**
	 * Process codeBlocks sequentially.
	 * @param listOfCodeblocks
	 * @param currentNode
	 * @param currentLevel
	 */
	public void addCodeBlocks(Set<Codeblock> listOfCodeblocks, FlowNode currentNode, int currentLevel) {
		currentNode.visit();
		if(currentNode instanceof Codeblock){
			listOfCodeblocks.add((Codeblock) currentNode);
		}
		for (FlowNode child : currentNode.getChildren()) {
			if ((child instanceof NoOp) && (!child.visited()) && (child.getParents().size() == 2)) {
				child.visit();
				continue;
			}
			if(!child.visited() || (child instanceof NoOp)) {
				int nextLevel = currentLevel;
				if (currentNode instanceof NoOp) {
					nextLevel--;
				}
				if (child instanceof Branch) {
					nextLevel++;
				}
				//System.out.println("Level for child " + child.toString() + ": " + nextLevel);
				//nodeToLevel.put(child, nextLevel);
				addCodeBlocks(listOfCodeblocks, child, nextLevel);
			}
		}
		
	}
	
	public void buildGraph() {
		for (START initialNode : flowNodes.values()) {
			//setupParams(initialNode);
			Set<Codeblock> listOfCodeblocks = new LinkedHashSet<Codeblock>();
			List<FlowNode> scanning = new ArrayList<FlowNode>(); //Need to find all the Codeblocks
			scanning.add(initialNode);
			addCodeBlocks(listOfCodeblocks, initialNode, 1);
			
			initialNode.resetVisit(); //fix the visited parameters.
			
			boolean complete = false;
			while (!complete) {
				complete = true;
				Iterator<Codeblock> codeblockIter = listOfCodeblocks.iterator();
				while (codeblockIter.hasNext()) {
					Codeblock cblock = codeblockIter.next();
					List<Statement> statementList = cblock.getStatements();
					System.out.println("\n====== Processing codeblock: " + cblock.toString());
					
					Iterator<Statement> statementIter = statementList.iterator();
					while(statementIter.hasNext()){
						Statement st = statementIter.next();
						ReachingDefinition rd = st.getReachingDefinition();
						System.out.println("RD size: " + rd.getAllWebs().size());
						if (st instanceof Assignment){	
							Assignment assignment = (Assignment) st;
							String varName = assignment.getDestVar().getName();
							if (assignment.getDestVar().isArray()) {
								System.out.println("\"" + varName + "\" is an array. Skipping.");
								continue;
							}
							IR_FieldDecl decl = assignment.getDestVar().getFieldDecl();
							System.out.println("Variable being processed: " + varName);
							Web curWeb = (new ArrayList<Web>(rd.getWebsMap().get(decl))).get(0);
							GraphNode node;
							if (webToNode.containsKey(curWeb)) {
								System.out.println("Retrieving web for var " + varName + " to webToNode: " + curWeb);
								node = webToNode.get(curWeb);
							} else {
								node = new GraphNode(assignment);
								System.out.println("Putting web for var " + varName + " to webToNode: " + curWeb);
								webToNode.put(curWeb, node);
								nodes.add(node);
							}
							assignment.setNode(node);
							
							if (rd.getWebsMap().get(decl).size() != 1) {
								throw new RuntimeException("There should only be one Web for varName: " + varName);
							}
							if (!addEdges(curWeb, node, st)) {
								// there are webs that are needed that haven't been defined yet.
								complete = false;
							}
						}
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
