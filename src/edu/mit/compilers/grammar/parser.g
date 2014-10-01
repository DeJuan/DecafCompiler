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

program: (callout_decl)* (field_decl)* (method_decl)* EOF!
{#program = #([PROGRAM,"program"],#program);};

callout_decl: TK_callout^ ID SEMICOLON!;
field_decl: type (ID | ID LBRACKET! INT_LITERAL RBRACKET!) (COMMA! (ID | ID LBRACKET! INT_LITERAL RBRACKET!))* SEMICOLON!
{#field_decl = #([FIELD_DECL,"field_decl"],#field_decl);};
method_decl: (type | TK_void) ID LPAREN! ((type ID) (COMMA! type ID)*)? RPAREN! block
{#method_decl = #([METHOD_DECL,"method_decl"],#method_decl);};

block: LCURLY! (field_decl)* (statement)* RCURLY!
{#block = #([BLOCK,"block"],#block);};
type: TK_int | TK_boolean;

statement: location (ASSIGN^ | ASSIGN_MINUS^ | ASSIGN_PLUS^) expr SEMICOLON!
     | method_call SEMICOLON!
		 | TK_if^ LPAREN! expr RPAREN! block (TK_else block)?
		 | TK_for^ LPAREN! ID ASSIGN expr COMMA! expr RPAREN! block
		 | TK_while^ LPAREN! expr RPAREN! (COLON! INT_LITERAL)? block
		 | TK_return^ (expr)? SEMICOLON!
		 | TK_break^ SEMICOLON!
		 | TK_continue^ SEMICOLON!;

assign_op: ASSIGN | ASSIGN_MINUS | ASSIGN_PLUS;
method_call: method_name LPAREN! (callout_arg (COMMA! callout_arg)*)? RPAREN!
{#method_call = #([METHOD_CALL,"method_call"],#method_call);};

method_name: ID;
location: ID | (ID^ LBRACKET! expr RBRACKET!);

// This section is the only one that needed "hacking" to remove the 
// left recursion... for now.
expr:    or_exp  (options {greedy=true;}:QUESTION^ expr COLON! or_exp)* ;
or_exp:  and_exp (options {greedy=true;}:OR^ and_exp)*;
and_exp: eq_exp  (options {greedy=true;}:AND^ eq_exp)*;
eq_exp:  rel_exp (options {greedy=true;}:(EQUALS^ | NOT_EQUALS^) rel_exp)*;
rel_exp: add_exp (options {greedy=true;}:(LT^ | GT^ | LTE^ | GTE^) add_exp)*;
add_exp: mul_exp (options {greedy=true;}:(PLUS^|MINUS^) mul_exp)*;
mul_exp: base_expr (options {greedy=true;}:(TIMES^|DIVIDE^|MOD^) base_expr)*;

base_expr: location
	     | method_call
	     | literal
	     | AT^ ID
	     | MINUS^ expr
	     | BANG^ expr
	     | LPAREN! expr RPAREN!;

callout_arg: expr | STRING_LITERAL;

bin_op: arith_op | rel_op | eq_op | cond_op;
arith_op: PLUS | MINUS | TIMES | DIVIDE | MOD;
rel_op: LT | GT | LTE | GTE;
eq_op: EQUALS | NOT_EQUALS;
cond_op: AND | OR;

literal: INT_LITERAL | CHAR_LITERAL | bool_literal;
bool_literal: TK_true | TK_false;
