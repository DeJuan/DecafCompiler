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
import edu.mit.compilers.controlflow.*;
import edu.mit.compilers.grammar.*;
import edu.mit.compilers.ir.*;
import edu.mit.compilers.tools.CLI;
import edu.mit.compilers.tools.CLI.Action;

class Main {
	
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
			  System.out.println(indents + "Child " + i);
			  printFlowNode(child, indent+1);
			  i++;
		  }
	  }
  }
  
  public static void printIR(HashMap<String, FlowNode> irMap) {
	  for (Map.Entry<String, FlowNode> entry : irMap.entrySet()) {
		    String key = entry.getKey();
		    START node = (START) entry.getValue();
		    
		    System.out.println("============================ " + key + " ============================");
		    List<IR_FieldDecl> args = node.getArguments();
		    if (args.size() == 0) {
		    	System.out.println("No arguments");
		    } else {
		    	System.out.print("Arguments: ");
			    for (IR_FieldDecl arg : args)
			    	System.out.print(arg.getName() + " ");
			    System.out.println("");
		    }
		    printFlowNode(node, 0);
	  }
	  System.out.println("");
  }
  
  
  public static void main(String[] args) {
    try {
      String[] optimizations = {"cse"};
      CLI.parse(args, optimizations);
      InputStream inputStream = args.length == 0 ?
          System.in : new java.io.FileInputStream(CLI.infile);
      PrintStream outputStream = CLI.outfile == null ? System.out : new java.io.PrintStream(new java.io.FileOutputStream(CLI.outfile));
      if (CLI.target == Action.SCAN) {
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
			    	  
			    	  if (CLI.opts[0]) {
			    		  // common subexpression elimination optimization is turned on.
			    		  // =============== GENERATE LOW-LEVEL IR =================
			    		  System.out.println("Generating low-level IR.");
			    		  ControlflowContext context = new ControlflowContext();
			    		  List<IR_Node> callouts = new ArrayList<IR_Node>(); // type IR_MethodDecl
			    		  List<IR_Node> globals = new ArrayList<IR_Node>();  // type IR_FieldDecl
			    		  HashMap<String, FlowNode> flowNodes = new HashMap<String, FlowNode>();
			    		  GenerateFlow.generateProgram(root, context, callouts, globals, flowNodes);
			    		  
			    		  // TODO: Process flowNodes and generate assembly code.
			    		  
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
			    	  } 
			    	  else {
			    		  // =============== DIRECT TO ASSEMBLY =================
			    		  CodegenContext context = new CodegenContext();
				    	  Codegen.generateProgram(root, context);
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
    }
  }
}
