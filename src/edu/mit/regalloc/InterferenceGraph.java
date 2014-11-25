package edu.mit.regalloc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.controlflow.Assignment;
import edu.mit.compilers.controlflow.Bitvector;
import edu.mit.compilers.controlflow.Codeblock;
import edu.mit.compilers.controlflow.ControlflowContext;
import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.START;
import edu.mit.compilers.controlflow.Statement;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;

public class InterferenceGraph {
	
	private List<GraphNode> nodes;
	
	private HashMap<GraphNode, List<GraphNode>> adjList;
	private HashMap<List<GraphNode>, Boolean> bitMatrix;
	
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
	}
	
	private void addEdge(GraphNode from, GraphNode to) {
		adjList.get(from).add(to);
		bitMatrix.put(Arrays.asList(from, to), true);
	}
	
	private void removeEdge(GraphNode from, GraphNode to) {
		List<GraphNode> nodeList = adjList.get(from);
		nodeList.remove(to);
		bitMatrix.remove(Arrays.asList(from, to));
	}
	
	private void addEdges(GraphNode node, Bitvector liveMap) {
		for (String var : liveMap.getVectorMap().keySet()) {
			GraphNode varNode = new GraphNode(var);
			addEdge(node, varNode);
		}
	}
	
	public void buildGraph() {
		for (START initialNode : flowNodes.values()) {
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
						GraphNode node = new GraphNode((Assignment) st);
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
		return adjList.get(node).size();
	}
	
	

}
