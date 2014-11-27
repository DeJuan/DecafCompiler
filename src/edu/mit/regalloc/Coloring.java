package edu.mit.regalloc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.mit.compilers.codegen.Regs;

public class Coloring {
	
	InterferenceGraph graph;
	List<GraphNode> spillList;
	List<GraphNode> workList;
	
	Stack<GraphNode> removedNodes;
	Stack<GraphNode> spillNodes;
	
	HashMap<GraphNode, Double> nodeToSpillCost;
	
	int k; // maximum number of colors
	
	public Coloring(InterferenceGraph graph, int k) {
		this.graph = graph;
		this.k = k;
	}
	
	private Set<Regs> getAssignedRegisters(Set<GraphNode> neighbors) {
		HashSet<Regs> regs = new HashSet<Regs>();
		for (GraphNode node : neighbors) {
			if (node.hasAssignedRegister()) {
				regs.add(node.getRegister());
			}
		}
		return regs;
	}
	
	private Boolean isGenPurposeReg(Regs reg) {
		return reg.toString().substring(1).matches("\\d+");
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
			Set<GraphNode> neighbors = graph.getAdjList().get(node);
			Set<Regs> asssignedRegisters = getAssignedRegisters(neighbors);
			Regs assignedRegister = assignRegister(node, asssignedRegisters);
			if (assignedRegister == null) {
				// cannot assign an empty register; must spill instead.
				spillNodes.push(node);
			} else {
				node.setRegister(assignedRegister);
			}
		}
	}
	
	public Boolean removeNodes(List<GraphNode> nodesToProcess) {
		Boolean changed = false;
		if (nodesToProcess.size() == 0)
			return false;
		for (Iterator<GraphNode> iterator = nodesToProcess.iterator(); iterator.hasNext();) {
			GraphNode node = iterator.next();
			if (graph.getNumEdges(node) < k) {
				removedNodes.push(node);
				iterator.remove();
				changed = true;
			}
		}
		return changed;
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
				spillNodes.push(nodeToSpill);
				nodesToProcess.remove(nodeToSpill);
				changed = true;
			}
		}
		assignColors();
		return graph.getNodes();
	}
	
	public List<GraphNode> getSpilledNodes() {
		return spillList;
	}
	
	public double calcSpillCost(GraphNode node) {
		// Simple heuristic for now.
		return 1;
	}
}
