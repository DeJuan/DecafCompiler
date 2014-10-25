package edu.mit.compilers.controlflow;

import java.util.List;

public class Branch extends FlowNode {
	private Expression expr;
	private FlowNode trueBranch;
	private FlowNode falseBranch;
	private List<FlowNode> parents;
	private List<FlowNode> children;
	
	public Branch(List<FlowNode> parentList, List<FlowNode> childList, FlowNode ifTrue, FlowNode ifFalse, Expression express){
		this.trueBranch = ifTrue;
		this.falseBranch = ifFalse;
		this.parents = parentList;
		this.children = childList;
		this.expr = express;
	}
	@Override
	public NodeType getType() {
		return NodeType.BRANCH;
	}

	@Override
	public List<FlowNode> getParents() {
		return parents;
	}

	@Override
	public List<FlowNode> getChildren() {
		return children;
	}
	
	public FlowNode getTrueBranch(){
		return this.trueBranch;
	}
	
	public FlowNode getFalseBranch(){
		return this.falseBranch;
	}
	
	public Expression getExpr(){
		return this.expr;
	}

}
