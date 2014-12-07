package edu.mit.compilers.regalloc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.controlflow.Assignment;
import edu.mit.compilers.controlflow.BinExpr;
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
import edu.mit.compilers.controlflow.START;
import edu.mit.compilers.controlflow.Statement;
import edu.mit.compilers.controlflow.Ternary;
import edu.mit.compilers.controlflow.Var;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;

public class CountUses {
	
	private double LOOP_COST = 10.0; // multiplier to spill cost for each nested loop.
	
	private ControlflowContext context;
	private List<IR_MethodDecl> calloutList;
	private List<IR_FieldDecl> globalList;
	private HashMap<String, START> flowNodes;
	
	// set of all IR_FieldDecls
	private Set<IR_FieldDecl> fieldDecls = new HashSet<IR_FieldDecl>();
	// map of FieldDecl to approx spill cost
	private HashMap<IR_FieldDecl, Double> fieldDeclToSpillCost = new HashMap<IR_FieldDecl, Double>();
	// number of loops the FlowNode is under (approximately)
	private HashMap<FlowNode, Double> numLoops = new HashMap<FlowNode, Double>();
	
	public CountUses(ControlflowContext context, 
			List<IR_MethodDecl> callouts, List<IR_FieldDecl> globals, HashMap<String, START> flowNodes){
		this.context = context;
		this.calloutList = callouts;
		this.globalList = globals;
		this.flowNodes = flowNodes;
	}
	
	/**
     * This method allows you to get the IR_FieldDecl objects representing the variables in a given Expression. 
     * It recursively searches the expression until it finds just the variables, and gets them for you. 
     * 
     * @param expr : The expression whose variables you want to isolate
     * @return List<IR_FieldDecl> : List of the IR_FieldDecl objects for all variables in the given expression
     */
    public List<IR_FieldDecl> getFieldDeclsFromExpression(Expression expr){
        List<IR_FieldDecl> allFieldDecls = new ArrayList<IR_FieldDecl>();
        if(expr instanceof BinExpr){
            BinExpr bin = (BinExpr)expr;
            Expression lhs = bin.getLeftSide();
            Expression rhs = bin.getRightSide();
            allFieldDecls.addAll(getFieldDeclsFromExpression(lhs));
            allFieldDecls.addAll(getFieldDeclsFromExpression(rhs));
        }
        else if (expr instanceof Var){
            Var varia = (Var)expr;
            allFieldDecls.add(varia.getFieldDecl());
            if(varia.getIndex() != null){
            	allFieldDecls.addAll(getFieldDeclsFromExpression(varia.getIndex()));
            }
        }	
        else if(expr instanceof NotExpr){
            NotExpr nope = (NotExpr)expr;
            allFieldDecls.addAll(getFieldDeclsFromExpression(nope.getUnresolvedExpression()));
        }
        else if(expr instanceof NegateExpr){
            NegateExpr negate = (NegateExpr)expr;
            allFieldDecls.addAll(getFieldDeclsFromExpression(negate.getExpression()));
        }
        else if(expr instanceof Ternary){
            Ternary tern = (Ternary)expr;
            allFieldDecls.addAll(getFieldDeclsFromExpression(tern.getTernaryCondition()));
            allFieldDecls.addAll(getFieldDeclsFromExpression(tern.getTrueBranch()));
            allFieldDecls.addAll(getFieldDeclsFromExpression(tern.getFalseBranch()));
        }
        else if(expr instanceof MethodCall){
            MethodCall MCHammer = (MethodCall)expr;
            for(Expression arg : MCHammer.getArguments()){
            	allFieldDecls.addAll(getFieldDeclsFromExpression(arg));
            }
        }
        return allFieldDecls;
    }
    
    public Set<IR_FieldDecl> getAllFieldDecls(START node){
        Set<IR_FieldDecl> allVarDecls = new LinkedHashSet<IR_FieldDecl>();
        List<FlowNode> processing = new ArrayList<FlowNode>();
        allVarDecls.addAll(globalList);
        allVarDecls.addAll(node.getArguments());
        processing.add(node.getChildren().get(0));
        while (!processing.isEmpty()){
            FlowNode currentNode = processing.remove(0);
            currentNode.visit();
            if(currentNode instanceof Codeblock){
                Codeblock cblock = (Codeblock)currentNode;
                for(Statement state : cblock.getStatements()){
                    if(state instanceof Declaration){
                        Declaration decl = (Declaration)state;
                        allVarDecls.add(decl.getFieldDecl());
                    }
                }
            }
            else if(currentNode instanceof Branch){
                Branch bblock = (Branch)currentNode;
                allVarDecls.addAll(getFieldDeclsFromExpression(bblock.getExpr()));
            }
            else if(currentNode instanceof START){
                START sBlock = (START)currentNode;
                allVarDecls.addAll(sBlock.getArguments());
            }
            else if(currentNode instanceof END){
                END eBlock = (END)currentNode;
                if(eBlock.getReturnExpression() != null){
                	allVarDecls.addAll(getFieldDeclsFromExpression(eBlock.getReturnExpression()));
                }
            }
            for(FlowNode child : currentNode.getChildren()){
                if(!child.visited()){
                    processing.add(child);
                }
            }
        }
        node.resetVisit();
        return allVarDecls;
    }
	
	
	public List<FlowNode> getAllFlowNodes(START initialNode) {
		List<FlowNode> scanning = new ArrayList<FlowNode>(); //Need to find all the Codeblocks
		List<FlowNode> listFlowNodes = new ArrayList<FlowNode>();
		scanning.add(initialNode);
		numLoops.put(initialNode, 1.0);
		while(!scanning.isEmpty()){ //scan through all nodes and create listing.
			FlowNode currentNode = scanning.remove(0);
			double curLoops = numLoops.get(currentNode);
			listFlowNodes.add(currentNode);
			currentNode.visit();
			//System.err.println("Now visiting " + currentNode);
			for (FlowNode child : currentNode.getChildren()){
				if(!child.visited()){
					if ((child instanceof Branch) && ( !(((Branch) child).getType() == Branch.BranchType.IF))) {
						// It's a for or while loop.
						numLoops.put(child, curLoops*LOOP_COST);
					} else if ((child instanceof NoOp) && (child.getChildren().size() == 1)) {
						// Got out of a for or while loop.
						numLoops.put(child, curLoops/LOOP_COST);
					} else {
						// Unchanged in number of loops.
						numLoops.put(child, curLoops);
					}
					scanning.add(child);
				}
			}
		}
		initialNode.resetVisit(); //fix the visited parameters.
		return listFlowNodes;
	}
	
	public void countExpressions(Expression expr, double spillCost) {
		for (IR_FieldDecl decl : getFieldDeclsFromExpression(expr)) {
			System.out.println("Adding spill cost to var: " + decl.getName() + ", " + spillCost);
			fieldDeclToSpillCost.put(decl, fieldDeclToSpillCost.get(decl) + spillCost);
		}
	}
	
	public void countCodeblock(Codeblock block) {
		double spillCost = numLoops.get((FlowNode) block);
		System.out.println(spillCost);
		for (Statement st : block.getStatements()) {
			if (st instanceof Assignment) {
				Var lhs = ((Assignment) st).getDestVar();
				if (lhs.getIndex() == null) {
					// not an array.
					System.out.println("Adding spill cost to var: " + lhs.getName() + ", " + spillCost);
					fieldDeclToSpillCost.put(lhs.getFieldDecl(), 
							fieldDeclToSpillCost.get(lhs.getFieldDecl()) + spillCost);
				}
				countExpressions(((Assignment) st).getValue(), spillCost);
			} else if (st instanceof MethodCallStatement) {
				for (Expression expr : ((MethodCallStatement) st).getMethodCall().getArguments()) {
					countExpressions(expr, spillCost);
				}
			} // skip Declarations.
		}
	}
	
	public void countBranch(Branch branch) {
		double spillCost = numLoops.get((FlowNode) branch);
		countExpressions(branch.getExpr(), spillCost);
	}
	
	public void run() {
		// Get all IR_FieldDecls
        fieldDecls.addAll(globalList);
		for (START initialNode : flowNodes.values()) {
			fieldDecls.addAll(getAllFieldDecls(initialNode));
		}
		// Populate fieldDecl HashMap with initial spill cost.
		for (IR_FieldDecl decl : fieldDecls) {
			fieldDeclToSpillCost.put(decl, 0.0);
		}
		
		// Walk through graph and count uses.
		for (START initialNode : flowNodes.values()) {
			final List<FlowNode> listFlowNodes = getAllFlowNodes(initialNode); // this will not change
			System.out.println("Number of FlowNodes: " + listFlowNodes.size());
			for (FlowNode flowNode : listFlowNodes) {
				System.out.println("Current FlowNode has loops: " + numLoops.get(flowNode) + ". Type: " + flowNode.getClass());
				if (flowNode instanceof Codeblock) {
					countCodeblock((Codeblock) flowNode);
				} else if (flowNode instanceof Branch) {
					countBranch((Branch) flowNode);
				}
			}
			
		}
	}
	
	public HashMap<IR_FieldDecl, Double> getFieldDeclToSpillCost() {
		return fieldDeclToSpillCost;
	}

}
