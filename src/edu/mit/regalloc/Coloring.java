package edu.mit.regalloc;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class Coloring {
	
	InterferenceGraph graph;
	List<GraphNode> spillList;
	List<GraphNode> workList;
	
	Stack<GraphNode> removedNodes;
	Stack<GraphNode> spillNodes;
	
	HashMap<GraphNode, Integer> assignments;
	
	int k; // maximum number of colors
	
	public Coloring(InterferenceGraph graph, int k) {
		this.graph = graph;
		this.k = k;
	}
	
	public void assignColors() {
		int colors[] = new int[16];
		while (!removedNodes.empty()) {
			GraphNode node = removedNodes.pop();
			// to implement
		}
	}
	
	public HashMap<GraphNode, Integer> run() {
		for (GraphNode node : graph.getNodes()) {
			if (graph.getNumEdges(node) >= k) {
				spillNodes.push(node);
			} else {
				removedNodes.push(node);
			}
		}
		assignColors();
		return assignments;
	}
	
	public double calcSpillCost(GraphNode node) {
		// Simple heuristic for now.
		return 1;
	}
}
