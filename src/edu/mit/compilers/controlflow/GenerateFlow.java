package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.mit.compilers.codegen.*;
import edu.mit.compilers.ir.*;

public class GenerateFlow {
	/**@brief generate control flow nodes given the root node of IR.
	 * 
	 * @param node root node
	 * @param context
	 */
	public static void generateProgram(IR_Node root, CodegenContext context, 
			List<IR_Node> callouts, List<IR_Node> globals, HashMap<String, FlowNode> flowNodes) {
		IR_Seq seq = (IR_Seq) root;
		List<IR_Node> statements = seq.getStatements();
		for (int i = 0; i < statements.size(); i++) {
			IR_Node node = statements.get(i);
			if (node.getType()==Type.METHOD) {
				FlowNode start = generateMethodDecl(node, context);
				String name = ((IR_MethodDecl) node).getName();
				flowNodes.put(name, start);
			} else if(node.getType()==Type.CALLOUT) {
				callouts.add(node);
			} else if(node instanceof IR_FieldDecl){
				generateFieldDeclGlobal((IR_FieldDecl) node, context);
				globals.add(node);
			} else {
				System.err.println("Unrecognized type");
			}
		}
	}
	
	public static void generateFieldDeclGlobal(IR_FieldDecl decl, CodegenContext context){
		Descriptor d = new Descriptor(decl);
		d.setLocation(new LocLabel(decl.getName()));
		context.putSymbol(decl.getName(), d);
	}
	
	public static FlowNode generateMethodDecl(IR_Node node, CodegenContext context) {
		IR_MethodDecl decl = (IR_MethodDecl) node;
		String name = decl.name;
		Descriptor d = new Descriptor(node);
		context.putSymbol(name, d);
		context.enterFun();
		context.incScope();
		
		for (int i = 0; i < decl.args.size(); i++) {
			IR_FieldDecl a = decl.args.get(i);
			Descriptor argd = new Descriptor(a);
			context.putSymbol(a.getName(), argd);
		}
		
		START start = new START(decl.args);
		
		FlowNode head = generateBlock(decl.body, context);
		start.addChild(head);
		head.addParent(start);

		context.decScope();
		
		return start;	
	}
	
	public static FlowNode generateBlock(IR_Seq seq, CodegenContext context) {
		context.incScope();
		List<IR_Node> statements = seq.getStatements();
		FlowNode node = null;
		for (int i = 0; i < statements.size(); i++) {
			IR_Node st = statements.get(i);
			if (st instanceof IR_If) {
				node = generateIf((IR_If) st, context);
			} else if (st instanceof IR_For) {
				node = generateFor((IR_For) st, context);
            } else if (st instanceof IR_While) {
            	node = generateWhile((IR_While) st, context);
			} else {
				node = new Codeblock();
			}
		}
		context.decScope();
		return node;
	}
	
	public static Branch generateIf(IR_If if_node, CodegenContext context) {
		IR_Node ir_expr = if_node.getExpr();
		Expression expr = generateExpr(ir_expr, context);
		Branch if_branch = new Branch(expr);
		return if_branch;
	}
	
	public static Branch generateFor(IR_For for_node, CodegenContext context) {
		//IR_Node ir_expr = for_node.getExpr();
		IR_CompareOp comp = new IR_CompareOp(for_node.getVar(), for_node.getEnd(), Ops.LT);
		Expression expr = generateExpr(comp, context);
		Branch for_branch = new Branch(expr);
		return for_branch;
	}

	public static Branch generateWhile(IR_While while_node, CodegenContext context) {
		IR_Node ir_expr = while_node.getExpr();
		Expression expr = generateExpr(ir_expr, context);
		Branch while_branch = new Branch(expr);
		return while_branch;
	}
	
	public static Expression generateExpr(IR_Node ir_expr, CodegenContext context) {
		Expression expr = null;
		if (ir_expr instanceof IR_Call) {
			IR_Call ir_call = (IR_Call) ir_expr;
			List<Expression> args = convertArguments(ir_call.getArgs());
			expr = new MethodCall(ir_call.getName(), args);
		}
		return expr;
	}
	
	/**
	 * Converts list of IR_Nodes arguments to list of Expression arguments
	 * @return expr: list of Expression objects for arguments
	 */
	public static List<Expression> convertArguments(List<IR_Node> ir_args) {
		List<Expression> expr = new ArrayList<Expression>();
		
		return expr;
	}
	

}
