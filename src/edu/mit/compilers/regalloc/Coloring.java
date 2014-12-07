package edu.mit.compilers.regalloc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.mit.compilers.codegen.Regs;
import edu.mit.compilers.ir.IR_FieldDecl;

public class Coloring {
	
	InterferenceGraph graph;
	
	Stack<GraphNode> removedNodes = new Stack<GraphNode>();
	Stack<GraphNode> spillNodes = new Stack<GraphNode>();
	
	HashMap<IR_FieldDecl, Integer> fieldDeclToSpillCost = new HashMap<IR_FieldDecl, Integer>();
	HashMap<GraphNode, Double> nodeToSpillCost = new HashMap<GraphNode, Double>();
	
	private final static List<Regs> regsToHoldVars = Arrays.asList(Regs.R12, Regs.R13, Regs.R14, Regs.R15);
	
	int k; // maximum number of colors
	
	public Coloring(InterferenceGraph graph, int k, HashMap<IR_FieldDecl, Integer> fieldDeclToSpillCost) {
		this.graph = graph;
		this.k = k;
		this.fieldDeclToSpillCost = fieldDeclToSpillCost;
	}
	
	private Set<Regs> getAssignedRegisters(Set<GraphNode> neighbors) {
		HashSet<Regs> regs = new HashSet<Regs>();
		if (neighbors.size() == 0)
			return regs;
		for (GraphNode node : neighbors) {
			System.out.println("Register: " + node.getRegister());
			if (!node.isRemoved() && node.hasAssignedRegister()) {
				regs.add(node.getRegister());
			}
		}
		return regs;
	}
	
	private boolean isGenPurposeReg(Regs reg) {
		return regsToHoldVars.contains(reg);
	}
	
	private Regs assignRegister(GraphNode node, Set<Regs> assignedRegisters) {
		for (Regs reg : Regs.values()) {
			if (isGenPurposeReg(reg) && !assignedRegisters.contains(reg)) {
				return reg;
			}
		}
		return null;
	}
	
	public void assignColors() {
		while (!removedNodes.empty()) {
			GraphNode node = removedNodes.pop();
			System.out.println("\n========== Variable: " + node.getWeb().getFieldDecl().getName());
			Set<GraphNode> neighbors = graph.getAdjList().get(node);
			System.out.println("Neighbors size: " + neighbors.size());
			Set<Regs> asssignedRegisters = getAssignedRegisters(neighbors);
			System.out.println("Assigned registers: " + asssignedRegisters);
			Regs assignedRegister = assignRegister(node, asssignedRegisters);
			if (assignedRegister == null) {
				// cannot assign an empty register; must spill instead.
				System.out.println("Spilling during assigning colors");
				spillNodes.push(node);
				node.spill();
			} else {
				System.out.println("Node assigned to register: " + assignedRegister.toString());
				node.setRegister(assignedRegister);
				node.unmarkAsRemoved();
			}
		}
	}
	
	private GraphNode getMaxSpillCost(List<GraphNode> nodes) {
		double maxSpillCost = Integer.MIN_VALUE;
		GraphNode returnNode = null;
		for (GraphNode node : nodes) {
			if (nodeToSpillCost.get(node) > maxSpillCost) {
				returnNode = node;
				maxSpillCost = nodeToSpillCost.get(node);
			}
		}
		return returnNode;
	}
	
	private GraphNode getMinSpillCost(List<GraphNode> nodes) {
		double minSpillCost = Integer.MAX_VALUE;
		GraphNode returnNode = null;
		for (GraphNode node : nodes) {
			if (nodeToSpillCost.get(node) < minSpillCost) {
				returnNode = node;
				minSpillCost = nodeToSpillCost.get(node);
			}
		}
		return returnNode;
	}
	
	public Boolean removeNodes(List<GraphNode> nodesToProcess) {
		Boolean changed = false;
		if (nodesToProcess.size() == 0)
			return false;
		List<GraphNode> copyNodes = new ArrayList<GraphNode>(nodesToProcess);
		while (!copyNodes.isEmpty()) {
			GraphNode node = getMaxSpillCost(copyNodes);
			copyNodes.remove(node);
			if (graph.getNumEdges(node) < k) {
				removedNodes.push(node);
				node.markAsRemoved();
				nodesToProcess.remove(node);
				changed = true;
			}
		}
		return changed;
	}
	
	public List<GraphNode> run() {
		Boolean changed = true;
		List<GraphNode> nodesToProcess = graph.getNodes();
		for (GraphNode node : nodesToProcess) {
			nodeToSpillCost.put(node, calcSpillCost(node));
		}
		while (nodesToProcess.size() > 0) {
			while (changed) {
				changed = removeNodes(nodesToProcess);
			}
			if (nodesToProcess.size() > 0) {
				// must spill one node, then try again.
				GraphNode nodeToSpill = getMinSpillCost(nodesToProcess);
				System.out.println("Spilling var: " + nodeToSpill.getWeb().getFieldDecl().getName());
				spillNodes.push(nodeToSpill);
				nodeToSpill.markAsRemoved();
				nodeToSpill.spill();
				nodesToProcess.remove(nodeToSpill);
				changed = true;
			}
		}
		assignColors();
		return graph.getNodes();
	}
	
	public List<GraphNode> getSpilledNodes() {
		return spillNodes;
	}
	
	public double calcSpillCost(GraphNode node) {
		// Simple heuristic for now.
		IR_FieldDecl decl = node.getWeb().getFieldDecl();
		double spillCost = fieldDeclToSpillCost.get(decl) * 1.0;
		System.out.println("Spill cost for var " + decl.getName() + " is: " + spillCost);
		return spillCost;
	}
}
