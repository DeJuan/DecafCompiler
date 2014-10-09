package edu.mit.compilers.codegen;
import edu.mit.compilers.ir.*;
public class Codegen {
	/**@brief generate code for root node if IR
	 * 
	 * @param node root node
	 * @param context
	 */
	public static void generateProgram(IR_Node node, CodegenContext context){
		
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
