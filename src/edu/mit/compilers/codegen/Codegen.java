package edu.mit.compilers.codegen;
import edu.mit.compilers.ir.*;
public class Codegen {
	/**@brief generate code for root node of IR.
	 * 
	 * @param node root node
	 * @param context
	 */
	public static void generateProgram(IR_Node node, CodegenContext context){
		
	}

	/**@brief All string literals are replaced with their labels.
	 * Their values stored in codegen context.
	 * 
	 * @param root
	 * @param context
	 */
	public static void generateStringLiteral(IR_Node root, CodegenContext context){
		
	}
	
	public static void generateMethodDecl(IR_Node MethodDecl, CodegenContext context){
		
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
