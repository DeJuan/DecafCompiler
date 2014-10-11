package edu.mit.compilers.codegen;
import java.util.List;

import edu.mit.compilers.ir.*;
public class Codegen {
	/**@brief generate code for root node of IR.
	 * 
	 * @param node root node
	 * @param context
	 */
	public static void generateProgram(IR_Node node, CodegenContext context){
		IR_Seq seq = (IR_Seq)node;
		List<IR_Node> stmt = seq.getStatements();
		for (int ii =0 ;ii<stmt.size(); ii++){
			IR_Node n = stmt.get(ii);
			if(n.getType()==Type.METHOD){
				generateMethodDecl(node, context);
			}else if(n.getType()==Type.CALLOUT){
				generateCallout(node, context);
			}
		}

	}

	public static void generateCallout(IR_Node node, CodegenContext context){
		//not necessary to add this to symbol table since semantic check is done. For integrity.
		IR_MethodDecl decl = (IR_MethodDecl)node;
		String name = decl.name;
		Descriptor d = new Descriptor(node);
		context.symbol.put(name, d);
	}
	
	public static void generateMethodDecl(IR_Node node, CodegenContext context){
		IR_MethodDecl decl = (IR_MethodDecl)node;
		String name = decl.name;
		Descriptor d = new Descriptor(node);
	}

	
	/**@brief All string literals are replaced with their labels.
	 * Their values stored in codegen context.
	 * 
	 * @param root
	 * @param context
	 */
	public static void generateStringLiteral(IR_Node root, CodegenContext context){
		
	}	
	
	/**@brief expression nodes should return location of the result
	 * 
	 * @param expr
	 * @param context
	 * @return
	 */
	public static MemLocation generateExpr(IR_Node expr, CodegenContext context){
		return null;
	}
	
}
