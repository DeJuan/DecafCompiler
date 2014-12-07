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
	
	private boolean addEdges(Web curWeb, GraphNode node) {
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
				addEdge(node, webToNode.get(web));
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
			
			for (Codeblock cblock : listOfCodeblocks){
				List<Statement> statementList = cblock.getStatements();
				boolean complete = false;
				while (!complete) {
					System.out.println("\n====== Processing codeblock: " + cblock.toString());
					complete = true;
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
							if (!addEdges(curWeb, node)) {
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
