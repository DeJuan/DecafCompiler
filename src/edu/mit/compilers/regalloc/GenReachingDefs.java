package edu.mit.compilers.regalloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import edu.mit.compilers.controlflow.Assignment;
import edu.mit.compilers.controlflow.Branch;
import edu.mit.compilers.controlflow.Branch.BranchType;
import edu.mit.compilers.controlflow.Codeblock;
import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.START;
import edu.mit.compilers.controlflow.Statement;
import edu.mit.compilers.ir.IR_FieldDecl;

public class GenReachingDefs {
	
	private List<IR_FieldDecl> globals;
	private HashMap<String, START> flowNodes;
	
	private HashMap<START, HashSet<Web>> websForEachMethod = new HashMap<START, HashSet<Web>>();
	private HashMap<FlowNode, FlowNode> whileParent = new HashMap<FlowNode, FlowNode>();
	
	public GenReachingDefs(List<IR_FieldDecl> globals, HashMap<String, START> flowNodes){
		this.globals = globals;
		this.flowNodes = flowNodes;
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
	
	public ReachingDefinition union(ReachingDefinition A, ReachingDefinition B, HashSet<Web> allWebs) {
		ReachingDefinition newRD = new ReachingDefinition(A);
		for (Web web : B.getAllWebs()) {
			newRD.addWeb(web);
		}
		Boolean didMerge = newRD.merge(allWebs);
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

	public ReachingDefinition generateCodeblockOUT(Codeblock node, ReachingDefinition RDin, HashSet<Web> allWebs) {
		ReachingDefinition rd = new ReachingDefinition(RDin);
		for (Statement st : node.getStatements()) {
			if (st instanceof Assignment && !globals.contains(((Assignment) st).getDestVar().getFieldDecl())) {
				// Non-global assignment
				System.out.println("\n=======");
				System.out.println("Statement: " + st);
				Assignment assign = (Assignment) st;
				String varName = assign.getDestVar().getName();
				if (assign.getDestVar().isArray()) {
					System.out.println("RD: " + rd);
					if (rd == null) {
						throw new RuntimeException("NULL RD");
					}
					st.setReachingDefinition(rd);
					rd.setStatements(st);
					System.out.println(rd.getAllWebs());
					System.out.println("\"" + varName + "\" is an array. Skipping.");
					continue;
				}
				IR_FieldDecl decl = assign.getDestVar().getFieldDecl();
				System.out.println("Assigning to: " + varName);
				Boolean addWeb = true;
				if (rd.getWebsMap().containsKey(decl)) {
					Web existingWeb = (new ArrayList<Web>(rd.getWebsMap().get(decl))).get(0);
					System.out.println("Web: " + existingWeb + " size: " + rd.getWebsMap().get(decl).size());
					
					if (st.getReachingDefinition() != null && st.getReachingDefinition().getWebsMap().containsKey(decl)) {
						Web origAssignmentWeb = (new ArrayList<Web>(st.getReachingDefinition().getWebsMap().get(decl))).get(0);
						if (!origAssignmentWeb.equals(existingWeb)) {
							// Must union the two webs because they overlap
							// Used for the following case:
							// int a, b;
							// for (b=0, 10) {
							//   a = 1;  <-- An alternate web for a could technically exist before assignment
							//   a = 2;
							// }
							// Note: if a is assigned the for loop, then there would be no issue because the webs would be merged.
							// Note 2: Union is probably not completely necessary - we could probably just use the original web.
							System.out.println("Weird case happened here where we have to union mid-statement.");
							rd = union(st.getReachingDefinition(), rd, allWebs);
							addWeb = false;
						}
					}
					if (addWeb) {
						if (!(existingWeb.getStartingStatements().contains(st))) {
							// Not the same web.
							System.out.println("Size of starting Statements: " + existingWeb.getStartingStatements().size());
							for (Statement s : existingWeb.getStartingStatements()) {
								System.out.println(((Assignment) s).getDestVar().getName() + ": " + s);
							}
							System.out.println("Killing var: " + varName);
							rd.removeWeb(decl);
						} else {
							addWeb = false;
							System.out.println("This is actually the same web. No change to webs.");
						}
					}
				}
				System.out.println("Before adding web: " + rd);
				if (addWeb) {
					Web web = new Web(decl, st, (FlowNode) node);
					System.out.println("Created web " + web);
					rd.addWeb(web);
					allWebs.add(web);
				}
				System.out.println("After adding web: " + rd);
			}
			System.out.println("RD: " + rd);
			if (rd == null) {
				throw new RuntimeException("NULL RD");
			}
			st.setReachingDefinition(rd);
			rd.setStatements(st);
			System.out.println(rd.getAllWebs());
		}
		return rd;
	}
	
	public HashMap<START, HashSet<Web>> run() {
		for (START initialNode : flowNodes.values()) {
			websForEachMethod.put(initialNode, new HashSet<Web>());
			final List<FlowNode> listFlowNodes = getAllFlowNodes(initialNode); // this will not change
			System.out.println("Number of FlowNodes: " + listFlowNodes.size());
			LinkedHashSet<FlowNode> changed = new LinkedHashSet<FlowNode>(); // this will change
			initialNode.setIN(new ReachingDefinition());
			for (FlowNode flowNode : listFlowNodes) {
				flowNode.setOUT(new ReachingDefinition());
				changed.add(flowNode);
			}
			changed.remove(initialNode);
			
			int loopLimit = 1;
			while (!changed.isEmpty()) {
				System.out.println("\n" + loopLimit + " ===================================");
				Iterator<FlowNode> it = changed.iterator();
				FlowNode n = it.next();
				it.remove();
				System.out.println(n.getClass());
				
				n.setIN(new ReachingDefinition());
				for (FlowNode p : n.getParents()) {
					System.out.println("Parents class: " + p.getClass());
					ReachingDefinition unionReachDef = union(n.getIN(), p.getOUT(), websForEachMethod.get(initialNode));
					n.setIN(unionReachDef);
				}
				// Deal with issue where While doesn't have a parent (when it should be there)
				if ((n instanceof Branch) && ((Branch) n).getType() == BranchType.WHILE) {
					if (whileParent.containsKey(n)) {
						System.out.println("Substituting extra while FlowNode");
						FlowNode p = whileParent.get(n);
						ReachingDefinition unionReachDef = union(n.getIN(), p.getOUT(), websForEachMethod.get(initialNode));
						n.setIN(unionReachDef);
					}
				}
				System.out.println("IN after union with parents: " + n.getIN());
				n.getIN().setFlowNodes(n);
				
				ReachingDefinition OUTn;
				if (n instanceof Codeblock) {
					OUTn = generateCodeblockOUT((Codeblock) n, n.getIN(), websForEachMethod.get(initialNode));
				} else {
					OUTn = n.getIN();
				}
				System.out.println("OLD OUT: " + n.getOUT());
				System.out.println("NEW OUT: " + OUTn);
				if (n.getOUT().changed(OUTn)) {
					System.out.println("Changed!");
					for (FlowNode s : n.getChildren()) {
						if ((s instanceof Branch) && ((Branch) s).getType() == BranchType.WHILE) {
							if (!((Branch) s).getParents().contains(n)) {
								System.out.println("Generating temp while parent");
								whileParent.put(s, n);
							}
						}
						changed.add(s);
					}
				}
				n.setOUT(OUTn);
				n.getOUT().setFlowNodes(n);
				loopLimit++;
			}
		}
		return websForEachMethod;
	}
}
