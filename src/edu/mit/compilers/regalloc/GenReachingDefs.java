package edu.mit.compilers.regalloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.controlflow.Assignment;
import edu.mit.compilers.controlflow.Codeblock;
import edu.mit.compilers.controlflow.ControlflowContext;
import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.Optimizer;
import edu.mit.compilers.controlflow.START;
import edu.mit.compilers.controlflow.Statement;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;

public class GenReachingDefs {
	
	private ControlflowContext context;
	private List<IR_MethodDecl> calloutList;
	private List<IR_FieldDecl> globalList;
	private HashMap<String, START> flowNodes;
	private Optimizer optimizer;
	
	private HashMap<FlowNode, ReachingDefinition> IN = new HashMap<FlowNode, ReachingDefinition>();
	private HashMap<FlowNode, ReachingDefinition> OUT = new HashMap<FlowNode, ReachingDefinition>();
	
	
	public GenReachingDefs(ControlflowContext context, 
			List<IR_MethodDecl> callouts, List<IR_FieldDecl> globals, HashMap<String, START> flowNodes){
		this.context = context;
		this.calloutList = callouts;
		this.globalList = globals;
		this.flowNodes = flowNodes;
		this.optimizer = new Optimizer(context, callouts, globals, flowNodes);
	}
	
	public List<FlowNode> getAllFlowNodes(START initialNode) {
		List<FlowNode> scanning = new ArrayList<FlowNode>(); //Need to find all the Codeblocks
		List<FlowNode> listFlowNodes = new ArrayList<FlowNode>();
		scanning.add(initialNode);
		while(!scanning.isEmpty()){ //scan through all nodes and create listing.
			FlowNode currentNode = scanning.remove(0);
			listFlowNodes.add(currentNode);
			currentNode.visit();
			//System.err.println("Now visiting " + currentNode);
			for (FlowNode child : currentNode.getChildren()){
				if(!child.visited()){
					scanning.add(child);
				}
			}
		}
		initialNode.resetVisit(); //fix the visited parameters.
		return listFlowNodes;
	}
	
	public ReachingDefinition union(ReachingDefinition A, ReachingDefinition B) {
		ReachingDefinition newRD = new ReachingDefinition(A);
		for (Web web : B.getAllWebs()) {
			newRD.addWeb(web);
		}
		Boolean didMerge = newRD.merge();
		if (didMerge) {
			System.out.println("Did merge!!");
		} else {
			System.out.println("Did not do merge.");
		}
		return newRD;
	}
	
	public ReachingDefinition subtract(ReachingDefinition A, ReachingDefinition B) {
		ReachingDefinition newRD = new ReachingDefinition(A);
		for (Web web : B.getAllWebs()) {
			newRD.removeWeb(web);
		}
		return newRD;
	}
	
	public ReachingDefinition generateCodeblockOUT(Codeblock node, ReachingDefinition RDin) {
		ReachingDefinition rd = new ReachingDefinition(RDin);
		for (Statement st : node.getStatements()) {
			if (st instanceof Assignment) {
				Assignment assign = (Assignment) st;
				String varName = assign.getDestVar().getName();
				Web gen = new Web(varName);
				if (rd.getWebsMap().containsKey(varName)) {
					System.out.println("Killing var: " + varName);
					rd.removeWeb(varName);
				}
				System.out.println("Before adding web: " + rd);
				rd.addWeb(gen);
				System.out.println("After adding web: " + rd);
			}
			System.out.println("RD: " + rd);
			st.setReachingDefinition(rd);
		}
		return rd;
	}
	
	public void run() {
		for (START initialNode : flowNodes.values()) {
			final List<FlowNode> listFlowNodes = getAllFlowNodes(initialNode); // this will not change
			System.out.println("Number of FlowNodes: " + listFlowNodes.size());
			LinkedHashSet<FlowNode> changed = new LinkedHashSet<FlowNode>(); // this will change
			IN.put(initialNode, new ReachingDefinition());
			for (FlowNode flowNode : listFlowNodes) {
				OUT.put(flowNode, new ReachingDefinition());
				changed.add(flowNode);
			}
			changed.remove(initialNode);
			
			int loopLimit = 100;
			while (!changed.isEmpty() && loopLimit > 0) {
				System.out.println(loopLimit);
				Iterator<FlowNode> it = changed.iterator();
				FlowNode n = it.next();
				it.remove();
				System.out.println(n.getClass());
				
				IN.put(n, new ReachingDefinition());
				for (FlowNode p : n.getParents()) {
					ReachingDefinition unionReachDef = union(IN.get(n), OUT.get(p));
					IN.put(n, unionReachDef);
				}
				System.out.println("IN after union with parents: " + IN.get(n));
				
				ReachingDefinition OUTn;
				if (n instanceof Codeblock) {
					OUTn = generateCodeblockOUT((Codeblock) n, IN.get(n));
				} else {
					OUTn = IN.get(n);
					n.setReachingDefinition(OUTn);
				}
				System.out.println("OLD OUT: " + OUT.get(n));
				System.out.println("NEW OUT: " + OUTn);
				if (OUT.get(n).changed(OUTn)) {
					System.out.println("Changed!");
					for (FlowNode s : n.getChildren()) {
						changed.add(s);
					}
				}
				OUT.put(n, OUTn);
				loopLimit--;
				
			}
			
		}
	}

}
