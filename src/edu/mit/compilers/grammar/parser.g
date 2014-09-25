header {
package edu.mit.compilers.grammar;
}

options
{
  mangleLiteralPrefix = "TK_";
  language = "Java";
}

class DecafParser extends Parser;
options
{
  importVocab = DecafScanner;
  k = 3;
  buildAST = true;
}

// Java glue code that makes error reporting easier.
// You can insert arbitrary Java code into your parser/lexer this way.
{
  // Do our own reporting of errors so the parser can return a non-zero status
  // if any errors are detected.
  /** Reports if any errors were reported during parse. */
  private boolean error;

  @Override
  public void reportError (RecognitionException ex) {
    // Print the error via some kind of error reporting mechanism.
    error = true;
  }
  @Override
  public void reportError (String s) {
    // Print the error via some kind of error reporting mechanism.
    error = true;
  }
  public boolean getError () {
    return error;
  }

  // Selectively turns on debug mode.

  /** Whether to display debug information. */
  private boolean trace = false;

  public void setTrace(boolean shouldTrace) {
    trace = shouldTrace;
  }
  @Override
  public void traceIn(String rname) throws TokenStreamException {
    if (trace) {
      super.traceIn(rname);
    }
  }
  @Override
  public void traceOut(String rname) throws TokenStreamException {
    if (trace) {
      super.traceOut(rname);
    }
  }
}

// Grammar for DECAF.
// Naming convention follows the spec page very closely.
// CAPITAL CASE = tokenized by scanner.

program: (callout_decl)* (field_decl)* (method_decl)* EOF;
callout_decl: TK_callout ID SEMICOLON;
field_decl: type (ID | ID LBRACKET INT_LITERAL RBRACKET) (COMMA (ID | ID LBRACKET INT_LITERAL RBRACKET))* SEMICOLON;
method_decl: (type | TK_void) ID LPAREN ((type ID) (COMMA type ID)*)? RPAREN block;
block: LCURLY (field_decl)* (statement)* RCURLY;
type: TK_int | TK_boolean;

statement: location assign_op expr SEMICOLON
		 | method_call SEMICOLON
		 | TK_if LPAREN expr RPAREN block (TK_else block)?
		 | TK_for LPAREN ID ASSIGN expr COMMA expr RPAREN block
		 | TK_while LPAREN expr RPAREN (COLON INT_LITERAL)? block
		 | TK_return (expr)? SEMICOLON
		 | TK_break SEMICOLON
		 | TK_continue SEMICOLON;
		 
assign_op: ASSIGN | ASSIGN_MINUS | ASSIGN_PLUS;
method_call: method_name LPAREN (callout_arg (COMMA callout_arg)*)? RPAREN;
method_name: ID;
location: ID (LBRACKET expr RBRACKET)?;

// This section is the only one that needed "hacking" to remove the 
// left recursion... for now.
expr: bin_op_expr ( options {greedy=true;}: QUESTION expr COLON expr)?;
bin_op_expr: base_expr ( options {greedy=true;}: bin_op bin_op_expr)?; 
base_expr: location
	     | method_call
	     | literal
	     | AT ID
	     | MINUS expr
	     | BANG expr
	     | LPAREN expr RPAREN;

callout_arg: expr | STRING_LITERAL;

bin_op: arith_op | rel_op | eq_op | cond_op;
arith_op: PLUS | MINUS | TIMES | DIVIDE | MOD;
rel_op: LT | GT | LTE | GTE;
eq_op: EQUALS | NOT_EQUALS;
cond_op: AND | OR;

literal: INT_LITERAL | CHAR_LITERAL | bool_literal;
bool_literal: TK_true | TK_false;
