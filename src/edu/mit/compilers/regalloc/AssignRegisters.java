package edu.mit.compilers.regalloc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.mit.compilers.codegen.Regs;
import edu.mit.compilers.controlflow.Assignment;
import edu.mit.compilers.controlflow.Branch;
import edu.mit.compilers.controlflow.Codeblock;
import edu.mit.compilers.controlflow.ControlflowContext;
import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.NoOp;
import edu.mit.compilers.controlflow.Optimizer;
import edu.mit.compilers.controlflow.START;
import edu.mit.compilers.controlflow.Statement;
import edu.mit.compilers.controlflow.Var;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;

public class AssignRegisters {
	
	private List<GraphNode> assignRegisters;
	private ControlflowContext context;
	private List<IR_MethodDecl> calloutList;
	private List<IR_FieldDecl> globalList;
	private HashMap<String, START> flowNodes;
	
	private HashMap<Var, Stack<Regs>> varToNodes = new HashMap<Var, Stack<Regs>>();
	
	public AssignRegisters(List<GraphNode> assignRegisters, ControlflowContext context, 
		List<IR_MethodDecl> callouts, List<IR_FieldDecl> globals, HashMap<String, START> flowNodes){
		this.assignRegisters = assignRegisters;
		this.context = context;
		this.calloutList = callouts;
		this.globalList = globals;
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
	
	public void addToMap(Var var, Regs reg) {
		if (varToNodes.containsKey(var)) {
			Stack<Regs> registers = varToNodes.get(var);
			registers.add(reg);
		} else {
			Stack<Regs> registers = new Stack<Regs>();
			registers.push(reg);
			varToNodes.put(var, registers);
		}
	}
	
	public void assignCodeblock(Codeblock block) {
		for (Statement st : block.getStatements()) {
			if (st instanceof Assignment) {
				Assignment assign = (Assignment) st;
				Var var = assign.getDestVar();
				GraphNode node = assign.getNode();
				Regs register = node.getRegister();
				addToMap(var, register);
			}
		}
	}
	
	public void run() {
		for (START initialNode : flowNodes.values()) {
			final List<FlowNode> listFlowNodes = getAllFlowNodes(initialNode);

			LinkedHashSet<FlowNode> changed = new LinkedHashSet<FlowNode>(); // this will change
			initialNode.resetVisit(); //fix the visited parameters.
			
			for (FlowNode flowNode : listFlowNodes) {
				//OUT.put(flowNode, new ReachingDefinition());
				flowNode.setOUT(new ReachingDefinition());
				changed.add(flowNode);
			}
			changed.remove(initialNode);
			
			while (!changed.isEmpty()) {
				System.out.println("\n========== ");
				Iterator<FlowNode> it = changed.iterator();
				FlowNode n = it.next();
				it.remove();
				System.out.println(n.getClass());
				
				if (n instanceof Codeblock) {
					assignCodeblock((Codeblock) n);
				} else {
					
				}
			}
		}
	}

}
