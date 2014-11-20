package edu.mit.compilers;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import antlr.ASTFactory;
import antlr.Token;
import antlr.collections.AST;
import edu.mit.compilers.ast.CommonASTWithLines;
import edu.mit.compilers.codegen.Codegen;
import edu.mit.compilers.codegen.CodegenContext;
import edu.mit.compilers.controlflow.Assembler;
import edu.mit.compilers.controlflow.Branch;
import edu.mit.compilers.controlflow.Codeblock;
import edu.mit.compilers.controlflow.ControlflowContext;
import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.GenerateFlow;
import edu.mit.compilers.controlflow.Optimizer;
import edu.mit.compilers.controlflow.START;
import edu.mit.compilers.grammar.DecafParser;
import edu.mit.compilers.grammar.DecafParserTokenTypes;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.DecafScannerTokenTypes;
import edu.mit.compilers.ir.IRMaker;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;
import edu.mit.compilers.ir.IR_Node;
import edu.mit.compilers.tools.CLI;
import edu.mit.compilers.tools.CLI.Action;

class Main {
	
  /**
   * Function that recursively prints the AST.
   * @param node : AST root node.
   * @param indent : what level we are relative to the root node of the program.
   */
  public static void printAst(AST node, int indent){
	  for(int ii = 0;ii<indent;ii++){
		  System.out.print("  ");
	  }
	  System.out.println(node.getText()+" " + node.getLine() + " " + node.getColumn());
	  int nChildren =node.getNumberOfChildren();
	  if(nChildren==0){
		  return;
	  }
	  
	  AST child = node.getFirstChild();
	  
	  for(int ii = 0; ii<nChildren; ii++){
		  printAst(child, indent+1);
		  child = child.getNextSibling();
	  }
  }
  
  /**
   * Function that recursively prints a FlowNode.
   * @param node : FlowNode object.
   * @param indent : what level we are relative to the root node (method declaration).
   */
  public static void printFlowNode(FlowNode node, int indent) {
	  if (node == null || node.visited())
		  return;
	  // generate indents
	  char[] chars = new char[indent*2];
	  Arrays.fill(chars, ' ');
	  String indents = new String(chars);
	  
	  node.visit();
	  System.out.println(indents + "========================");
	  System.out.println(indents + "Type: " + node.getClass().getSimpleName());
	  if (node instanceof Codeblock) {
		  System.out.println(indents + "Statements: " + ((Codeblock) node).getStatements().size());
	  } else if (node instanceof Branch) {
		  System.out.println(indents + "Branch type: " + ((Branch) node).getType());
	  }
	  List<FlowNode> children = node.getChildren();
	  if (children != null) {
		  System.out.println(indents + "Children: " + children.size());
		  int i = 1;
		  for (FlowNode child : children) {
			  System.out.println(indents + "Child #" + i);
			  printFlowNode(child, indent+1);
			  i++;
		  }
	  }
  }
  
  /**
   * Given a map of method names to method FlowNodes, print each FlowNode.
   * @param irMap : map of method names to method FlowNodes.
   */
  public static void printIR(HashMap<String, START> irMap) {
	  for (Map.Entry<String, START> entry : irMap.entrySet()) {
		    String key = entry.getKey();
		    START node = entry.getValue();
		    
		    System.out.println("============================ " + key + " ============================");
		    // Print arguments.
		    List<IR_FieldDecl> args = node.getArguments();
		    if (args.size() == 0) {
		    	System.out.println("No arguments");
		    } else {
		    	System.out.print("Arguments: ");
			    for (IR_FieldDecl arg : args)
			    	System.out.print(arg.getName() + " ");
			    System.out.println("");
		    }
		    // Print return type.
		    System.out.println("Return type: " + node.getRetType());
		    // Recursively print FlowNode.
		    printFlowNode(node, 0);
		    // Must reset all the visited flags of the FlowNode.
		    node.resetVisit();
	  }
	  System.out.println("");
  }
  
  public static void main(String[] args) {
    try {
      String[] optimizations = {"cse", "dce"};
      CLI.parse(args, optimizations);
      InputStream inputStream = args.length == 0 ?
          System.in : new java.io.FileInputStream(CLI.infile);
      if (CLI.target == Action.SCAN) {
        PrintStream outputStream = CLI.outfile == null ? System.out : new java.io.PrintStream(new java.io.FileOutputStream(CLI.outfile));
        DecafScanner scanner =
            new DecafScanner(new DataInputStream(inputStream));
        scanner.setTrace(CLI.debug);
        Token token;
        boolean done = false;
        while (!done) {
          try {
            for (token = scanner.nextToken();
                 token.getType() != DecafParserTokenTypes.EOF;
                 token = scanner.nextToken()) {
              String type = "";
              String text = token.getText();
              switch (token.getType()) {
               case DecafScannerTokenTypes.ID:
	             type = " IDENTIFIER";
	             break;
               case DecafScannerTokenTypes.STRING_LITERAL:
            	 type = " STRINGLITERAL";
                 break;
               case DecafScannerTokenTypes.CHAR_LITERAL:
            	 type = " CHARLITERAL";
            	 break;
               case DecafScannerTokenTypes.TK_true:
               case DecafScannerTokenTypes.TK_false:
            	 type = " BOOLEANLITERAL";
            	 break;
               case DecafScannerTokenTypes.INT_LITERAL:
            	 type = " INTLITERAL";
            	 break;
              }
              outputStream.println(token.getLine() + type + " " + text);
            }
            done = true;
          } catch(Exception e) {
            // print the error:
            System.err.println(CLI.infile + " " + e);
            scanner.consume();
          }
        }
        outputStream.close();
      } else {
    	  // =============== PARSE =================
    	  DecafScanner scanner = new DecafScanner(new DataInputStream(inputStream));
    	  DecafParser parser = new DecafParser(scanner);
    	  parser.setTrace(CLI.debug);
        
    	  ASTFactory factory = new ASTFactory();                         
    	  factory.setASTNodeClass(CommonASTWithLines.class);
    	  parser.setASTFactory(factory);
        
    	  parser.program();
    	  if (parser.getError()) {
    		  System.out.println("Parse error");
    		  System.exit(1);
    	  }
    	  if (CLI.target == Action.PARSE) {
    		  // Stop.
    	  } 
    	  else {
    		  // =============== BUILD AST =================
	    	  AST ast = parser.getAST();
	    	  // printAst(ast,0);
	    	  IRMaker ir_maker = new IRMaker();
	    	  IR_Node root = ir_maker.make(ast);
	    	  if ( !ir_maker.isValid() ) {
	    		  System.out.println("symantic error.");
	    	      System.exit(1);
	    	  }
	    	  if (CLI.target == Action.INTER) {
	    		  // Stop.
	    	  }
	    	  else {
	    		  if (CLI.target == Action.ASSEMBLY || CLI.target == Action.DEFAULT) {
		    		  // =============== GENERATE ASSEMBLY =================
			    	  String outFile = "a.s";
			    	  if (CLI.outfile != null) {
			    		  outFile = CLI.outfile;
			    	  }
			    	  for(boolean optEnabled : CLI.opts){
			    		  if(optEnabled){
			    			  System.err.println(optEnabled);
			    		  }
			    	  }
			    	  if (CLI.opts[0]) {
			    		  // common subexpression elimination optimization is turned on.
			    		  // =============== GENERATE LOW-LEVEL IR =================
			    		  System.out.println("Generating low-level IR.");
			    		  ControlflowContext context = new ControlflowContext();
			    		  List<IR_MethodDecl> callouts = new ArrayList<IR_MethodDecl>(); // type IR_MethodDecl
			    		  List<IR_FieldDecl> globals = new ArrayList<IR_FieldDecl>();  // type IR_FieldDecl
			    		  HashMap<String, START> flowNodes = new HashMap<String, START>();
			    		  GenerateFlow.generateProgram(root, context, callouts, globals, flowNodes);
			    		  // TODO: Process flowNodes and generate assembly code.
			    		  context = Assembler.generateProgram(root);
			    		  // Print things for debugging purposes.
			    		  System.out.println("\nCallouts:");
						  for (IR_Node callout : callouts)
							  System.out.println(((IR_MethodDecl) callout).getName());
						  System.out.println("\nGlobal vars:");
						  for (IR_Node global : globals)
							  System.out.println(((IR_FieldDecl) global).getName());
						  System.out.println("\nMethods:");
						  for (String s : flowNodes.keySet())
							  System.out.println(s);
						  System.out.println("");
						  // Traverse all FlowNodes and print them.
						  printIR(flowNodes);
						  Optimizer optimizer = new Optimizer(context, callouts, globals, flowNodes);
						  List<START> startsForMethods = new ArrayList<START>();
						  for(String key : flowNodes.keySet()){
							  startsForMethods.add(flowNodes.get(key));
						  }
						  ControlflowContext optimizeCSE = optimizer.applyCSE(startsForMethods);
						  PrintStream ps = new PrintStream(new FileOutputStream(outFile));
                          optimizeCSE.printInstructions(ps);
                          ps.close();
			    	  } 
			    	  else if (CLI.opts[1]){
			    		  //Dead Code Elimination is turned on, CSE is not. 
			    		  // =============== GENERATE LOW-LEVEL IR =================
			    		  System.out.println("Generating low-level IR.");
			    		  ControlflowContext context = new ControlflowContext();
			    		  List<IR_MethodDecl> callouts = new ArrayList<IR_MethodDecl>(); // type IR_MethodDecl
			    		  List<IR_FieldDecl> globals = new ArrayList<IR_FieldDecl>();  // type IR_FieldDecl
			    		  HashMap<String, START> flowNodes = new HashMap<String, START>();
			    		  GenerateFlow.generateProgram(root, context, callouts, globals, flowNodes);
			    		  //Process flowNodes and generate assembly code.
			    		  context = Assembler.generateProgram(root);
			    		  // Print things for debugging purposes.
			    		  System.out.println("\nCallouts:");
						  for (IR_Node callout : callouts)
							  System.out.println(((IR_MethodDecl) callout).getName());
						  System.out.println("\nGlobal vars:");
						  for (IR_Node global : globals)
							  System.out.println(((IR_FieldDecl) global).getName());
						  System.out.println("\nMethods:");
						  for (String s : flowNodes.keySet())
							  System.out.println(s);
						  System.out.println("");
						  // Traverse all FlowNodes and print them.
						  printIR(flowNodes);
						  Optimizer optimizer = new Optimizer(context, callouts, globals, flowNodes);
						  List<START> startsForMethods = new ArrayList<START>();
						  for(String key : flowNodes.keySet()){
							  startsForMethods.add(flowNodes.get(key));
						  }
						  ControlflowContext optimizeDCE = optimizer.applyDeadCodeElimination(startsForMethods);
						  PrintStream ps = new PrintStream(new FileOutputStream(outFile));
                          optimizeDCE.printInstructions(ps);
                          ps.close();
			    	  }
			    	  else {
			    		  // =============== DIRECT TO ASSEMBLY =================
			    		  //CodegenContext context = new CodegenContext();
			    		  //Codegen.generateProgram(root, context);
			    	      ControlflowContext context = Assembler.generateProgram(root);
				    	  PrintStream ps = new PrintStream(new FileOutputStream(outFile));
				    	  context.printInstructions(ps);
				    	  ps.close();
			    	  }
		    	  }
	    		  else {
	    			  System.err.println("Unrecognized command");
	    		  }
	    	  }
    	  }
      }
    } catch(Exception e) {
      // print the error:
      System.err.println(CLI.infile+" "+e);
      e.printStackTrace();
    } finally {
    }
  }
}
