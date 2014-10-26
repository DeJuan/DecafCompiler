package edu.mit.compilers;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import antlr.ASTFactory;
import antlr.Token;
import antlr.collections.AST;
import edu.mit.compilers.ast.CommonASTWithLines;
import edu.mit.compilers.codegen.Codegen;
import edu.mit.compilers.codegen.CodegenContext;
import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.GenerateFlow;
import edu.mit.compilers.grammar.DecafParser;
import edu.mit.compilers.grammar.DecafParserTokenTypes;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.DecafScannerTokenTypes;
import edu.mit.compilers.ir.IRMaker;
import edu.mit.compilers.ir.IR_Node;
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
    		  // Stop
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
	    		  // Stop
	    	  }
	    	  else {
	    		  if (CLI.target == Action.ASSEMBLY || CLI.target == Action.DEFAULT) {
		    		  // =============== GENERATE ASSEMBLY =================
			    	  String outFile = "a.s";
			    	  if(CLI.outfile!=null){
			    		  outFile = CLI.outfile;
			    	  }
			    	  CodegenContext context = new CodegenContext();
			    	  if (CLI.opts[0]) {
			    		  // temp hack to turn on optimization (by setting --opt=all)
			    		  // =============== GENERATE LOW-LEVEL IR =================
			    		  System.out.println("Generating low-level IR.");
			    		  List<IR_Node> callouts = new ArrayList<IR_Node>();
			    		  List<IR_Node> globals = new ArrayList<IR_Node>();
			    		  HashMap<String, FlowNode> flowNodes = new HashMap<String, FlowNode>();
			    		  GenerateFlow.generateProgram(root, context, callouts, globals, flowNodes);
			    	  } else {
			    		  // =============== DIRECT TO ASSEMBLY =================
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
