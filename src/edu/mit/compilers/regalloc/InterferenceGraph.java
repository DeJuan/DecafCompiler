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
import java.util.Map.Entry;
import java.util.Set;

import edu.mit.compilers.controlflow.Assignment;
import edu.mit.compilers.controlflow.BinExpr;
import edu.mit.compilers.controlflow.Bitvector;
import edu.mit.compilers.controlflow.Branch;
import edu.mit.compilers.controlflow.Codeblock;
import edu.mit.compilers.controlflow.ControlflowContext;
import edu.mit.compilers.controlflow.Declaration;
import edu.mit.compilers.controlflow.END;
import edu.mit.compilers.controlflow.Expression;
import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.MethodCall;
import edu.mit.compilers.controlflow.MethodCallStatement;
import edu.mit.compilers.controlflow.NegateExpr;
import edu.mit.compilers.controlflow.NoOp;
import edu.mit.compilers.controlflow.NotExpr;
import edu.mit.compilers.controlflow.Optimizer;
import edu.mit.compilers.controlflow.START;
import edu.mit.compilers.controlflow.Statement;
import edu.mit.compilers.controlflow.Ternary;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;
import edu.mit.compilers.ir.Ops;

public class InterferenceGraph {
	
	private List<GraphNode> nodes = new ArrayList<GraphNode>();
	
	private HashMap<GraphNode, Set<GraphNode>> adjList = new HashMap<GraphNode, Set<GraphNode>>();
	private HashSet<HashSet<GraphNode>> bitMatrix = new HashSet<HashSet<GraphNode>>();
	
	private HashMap<START, HashSet<Web>> websForEachMethod;
	private HashMap<START, HashSet<START>> methodToMethodCalls = new HashMap<START, HashSet<START>>();
	private HashMap<Web, GraphNode> webToNode = new HashMap<Web, GraphNode>();
	private HashMap<START, String> STARTToName = new HashMap<START, String>();
	
	private ControlflowContext context;
	private List<IR_MethodDecl> calloutList;
	private List<IR_FieldDecl> globalList;
	private HashMap<String, START> flowNodes;
	private Optimizer optimizer;
	
	public InterferenceGraph(ControlflowContext context, 
		List<IR_MethodDecl> callouts, List<IR_FieldDecl> globals, HashMap<String, START> flowNodes, 
		HashMap<START, HashSet<Web>> websForEachMethod){
		this.context = context;
		this.calloutList = callouts;
		this.globalList = globals;
		this.flowNodes = flowNodes;
		for (Entry<String, START> e : flowNodes.entrySet()) {
			STARTToName.put(e.getValue(), e.getKey());
		}
		this.optimizer = new Optimizer(context, callouts, globals, flowNodes);
		this.websForEachMethod = websForEachMethod;
	}
 
	private List<IR_FieldDecl> getLiveVars(Bitvector liveMap) {
		if (liveMap == null)
			return null;
		List<IR_FieldDecl> liveVars = new ArrayList<IR_FieldDecl>(); 
		for (IR_FieldDecl decl : liveMap.getVectorMap().keySet()) {
			if (liveMap.get(decl) == 1) {
				liveVars.add(decl);
			}
		}
		return liveVars;
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
		ReachingDefinition rd = st.getReachingDefinition();
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
					//System.out.println("We do not yet have the web for var: " + web.getFieldDecl().getName() + ". Need to reprocess.");
					done = false;
					continue;
					//throw new RuntimeException("webToNode should always contain the key for web: " + web.getFieldDecl().getName() + ". " + web);
				}
				//System.out.println("Considering adding edge to var: " + web.getFieldDecl().getName());
				//System.out.println("Live map: " + getLiveVars(st.getLiveMap()));
				GraphNode otherNode = webToNode.get(web);
				if (st.getLiveMap() == null){
					throw new UnsupportedOperationException("st.getLiveMap() at line 359 is null. Why? Please either fix it so it doesn't happen, or justify the shortcut to your group members.");
				}
				if(st.getLiveMap().get(web.getFieldDecl()) == 1) {
					// Variable will be live later, so we want to add an edge to that web
					//System.out.println("Live web. Adding edge");
					addEdge(node, otherNode);
				} else {
					//System.out.println("Dead web.");
				}
				
			}
		}
		// if no edges are added, we must still put an empty HashSet to adjList
		if (!adjList.containsKey(node)) {
			adjList.put(node, new HashSet<GraphNode>());
		}
		return done;
	}
	
	private void addEdgesForAllWebsBetweenOverlappingMethods() {
		for (START initialNode : flowNodes.values()) {
			//System.out.println("====== Method name: " + STARTToName.get(initialNode));
			HashSet<Web> allWebsInOtherMethods = new HashSet<Web>();
			for (START method : websForEachMethod.keySet()) {
				// get all methods. All webs between them must have edges.
				//System.out.println(STARTToName.get(method));
				if (method != null && !method.equals(initialNode)) {
					// not own method.
					allWebsInOtherMethods.addAll(websForEachMethod.get(method));
				}
			}
			if (!allWebsInOtherMethods.isEmpty()) {
				for (Web web : websForEachMethod.get(initialNode)) {
					GraphNode from = webToNode.get(web);
					for (Web toWeb : allWebsInOtherMethods) {
						GraphNode to = webToNode.get(toWeb);
						if (to != null && from != null) {
							addEdge(from, to);
						}
					}
				}
			}
		}
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
	
	public START convertMethodCallToSTART(MethodCall mc) {
		return flowNodes.get(mc.getMethodName());
	}
	
	 /**
     * This method allows you to get the START objects representing the variables in a given Expression. It recursively searches the expression until it finds just
     * the method calls, and gets them for you. 
     * 
     * @param expr : The expression whose variables you want to isolate
     * @return List<Var> : List of the START objects for all method calls in the given expression
     */
    public HashSet<START> getMethodCallsFromExpression(Expression expr){
    	HashSet<START> allMethods = new HashSet<START>();
        if(expr instanceof BinExpr){
            BinExpr bin = (BinExpr)expr;
            Expression lhs = bin.getLeftSide();
            Expression rhs = bin.getRightSide();
            allMethods.addAll(getMethodCallsFromExpression(lhs));
            allMethods.addAll(getMethodCallsFromExpression(rhs));
        }
        else if(expr instanceof NotExpr){
            NotExpr nope = (NotExpr)expr;
            allMethods.addAll(getMethodCallsFromExpression(nope.getUnresolvedExpression()));
        }
        else if(expr instanceof NegateExpr){
            NegateExpr negate = (NegateExpr)expr;
            allMethods.addAll(getMethodCallsFromExpression(negate.getExpression()));
        }
        else if(expr instanceof Ternary){
            Ternary tern = (Ternary)expr;
            allMethods.addAll(getMethodCallsFromExpression(tern.getTernaryCondition()));
            allMethods.addAll(getMethodCallsFromExpression(tern.getTrueBranch()));
            allMethods.addAll(getMethodCallsFromExpression(tern.getFalseBranch()));
        }
        else if(expr instanceof MethodCall){
            MethodCall MCHammer = (MethodCall) expr;
            allMethods.add(convertMethodCallToSTART(MCHammer));
        }
        return allMethods;
    }
	
	public HashSet<START> getAllMethodCallsInCurrentMethod(START node) {
		HashSet<START> allMethods = new HashSet<START>();
        List<FlowNode> processing = new ArrayList<FlowNode>();
        processing.add(node.getChildren().get(0));
        while (!processing.isEmpty()){
            FlowNode currentNode = processing.remove(0);
            currentNode.visit();
            if(currentNode instanceof Codeblock){
                Codeblock cblock = (Codeblock)currentNode;
                for(Statement st : cblock.getStatements()){
                    if(st instanceof Assignment){
                    	Expression expr = ((Assignment) st).getValue();
                        allMethods.addAll(getMethodCallsFromExpression(expr));
                    } else if (st instanceof MethodCallStatement) {
                    	allMethods.add(convertMethodCallToSTART(((MethodCallStatement) st).getMethodCall()));
                    }
                }
            }
            else if(currentNode instanceof Branch) {
                Branch bblock = (Branch)currentNode;
                allMethods.addAll(getMethodCallsFromExpression(bblock.getExpr()));
            }
            else if(currentNode instanceof END){
                END eBlock = (END)currentNode;
                if(eBlock.getReturnExpression() != null){
                    allMethods.addAll(getMethodCallsFromExpression(eBlock.getReturnExpression()));
                }
            }
            for(FlowNode child : currentNode.getChildren()){
                if(!child.visited()){
                    processing.add(child);
                }
            }
        }
        if (allMethods.contains(null)) {
        	allMethods.remove(null);
        }
        node.resetVisit();
        return allMethods;
	}
	
	public boolean notGlobalOrParam(IR_FieldDecl decl, START initialNode) {
		return !globalList.contains(decl) && !initialNode.getArguments().contains(decl);
	}
	
	public void buildGraph() {
		for (START initialNode : flowNodes.values()) {
			//setupParams(initialNode);
			methodToMethodCalls.put(initialNode, getAllMethodCallsInCurrentMethod(initialNode));
			if (methodToMethodCalls.get(initialNode).contains(initialNode)) {
				// There is a recursive call. Therefore, we cannot assign registers to any
				// variable in this method.
				continue;
			}
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
					//System.out.println("\n====== Processing codeblock: " + cblock.toString());
					
					Iterator<Statement> statementIter = statementList.iterator();
					while(statementIter.hasNext()){
						Statement st = statementIter.next();
						ReachingDefinition rd = st.getReachingDefinition();
						//System.out.println("RD size: " + rd.getAllWebs().size());
						if (st instanceof Assignment && notGlobalOrParam(((Assignment) st).getDestVar().getFieldDecl(), initialNode)){	
							Assignment assignment = (Assignment) st;
							String varName = assignment.getDestVar().getName();
							if (assignment.getDestVar().isArray()) {
								//System.out.println("\"" + varName + "\" is an array. Skipping.");
								continue;
							}
							IR_FieldDecl decl = assignment.getDestVar().getFieldDecl();
							//System.out.println("Variable being processed: " + varName);
							Web curWeb = (new ArrayList<Web>(rd.getWebsMap().get(decl))).get(0);
							GraphNode node;
							if (webToNode.containsKey(curWeb)) {
								//System.out.println("Retrieving web for var " + varName + " to webToNode: " + curWeb);
								node = webToNode.get(curWeb);
							} else {
								node = new GraphNode(curWeb);
								//System.out.println("Putting web for var " + varName + " to webToNode: " + curWeb);
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
		//addEdgesForAllWebsBetweenOverlappingMethods();
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
