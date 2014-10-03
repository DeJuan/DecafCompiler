package edu.mit.compilers;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.PrintStream;

import antlr.ASTFactory;
import antlr.Token;
import antlr.collections.AST;
import edu.mit.compilers.ast.CommonASTWithLines;
import edu.mit.compilers.grammar.DecafParser;
import edu.mit.compilers.grammar.DecafParserTokenTypes;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.DecafScannerTokenTypes;
import edu.mit.compilers.ir.IRMaker;
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
      CLI.parse(args, new String[0]);
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
      } else if (CLI.target == Action.PARSE ||
                 CLI.target == Action.DEFAULT) {
        DecafScanner scanner =
            new DecafScanner(new DataInputStream(inputStream));
        DecafParser parser = new DecafParser(scanner);
        parser.setTrace(CLI.debug);
        
        ASTFactory factory = new ASTFactory();                         
        factory.setASTNodeClass(CommonASTWithLines.class);
        parser.setASTFactory(factory);
        
        parser.program();
        if(parser.getError()) {
          System.exit(1);
        }
      } else if (CLI.target == Action.INTER) {
    	  DecafScanner scanner =
    	            new DecafScanner(new DataInputStream(inputStream));
    	  DecafParser parser = new DecafParser(scanner);
    	  parser.setTrace(CLI.debug);
    	        
    	  ASTFactory factory = new ASTFactory();                         
    	  factory.setASTNodeClass(CommonASTWithLines.class);
    	  parser.setASTFactory(factory);
    	        
    	  parser.program();
    	  if(parser.getError()) {
          System.out.println("parse error");
    		  System.exit(1);
    	  }
    	  
    	  AST ast = parser.getAST();
    	  IRMaker ir_maker = new IRMaker();
    	  if (ir_maker.GenerateProgram(ast) == null) {
    	      System.exit(1);
    	  }
      }
    } catch(Exception e) {
      // print the error:
      System.err.println(CLI.infile+" "+e);
    }
  }
}
