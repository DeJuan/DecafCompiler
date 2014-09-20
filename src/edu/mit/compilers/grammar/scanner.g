header {
package edu.mit.compilers.grammar;
}

options
{
  mangleLiteralPrefix = "TK_";
  language = "Java";
}

{@SuppressWarnings("unchecked")}
class DecafScanner extends Lexer;
options
{
  k = 2;
}

tokens 
{
  "boolean";
  "break";
  "callout";
  "class";
  "continue";
  "else";
  "false";
  "for";
  "if";
  "int";
  "return";
  "true";
  "void";
  "while";
}

// Selectively turns on debug tracing mode.
// You can insert arbitrary Java code into your parser/lexer this way.
{
  /** Whether to display debug information. */
  private boolean trace = false;

  public void setTrace(boolean shouldTrace) {
    trace = shouldTrace;
  }
  @Override
  public void traceIn(String rname) throws CharStreamException {
    if (trace) {
      super.traceIn(rname);
    }
  }
  @Override
  public void traceOut(String rname) throws CharStreamException {
    if (trace) {
      super.traceOut(rname);
    }
  }
}
LPAREN : "(";
RPAREN : ")";

LCURLY : "{";
RCURLY : "}";

LBRACKET : "[";
RBRACKET : "]";

ARITH_OP : "+" | "-" | "*" | "/" | "%";
ASSIGN_OP : "=" | "+=" | "-=";
REL_OP : "<" | ">" | "<=" | ">=";
EQ_OP : "==" | "!=";
COND_OP : "&&" | "||";

SEMICOLON : ";";
COMMA 	  : ",";
PERIOD    : ".";
COLON 	  : ":";
QUESTION  : "?";

ID options { paraphrase = "an identifier"; } : 
  ALPHA (ALPHA_NUM)*;

// Note that here, the {} syntax allows you to literally command the lexer
// to skip mark this token as skipped, or to advance to the next line
// by directly adding Java commands.
WS_ : (' ' | '\t' | ('\r' | "\r\n" | '\n' {newline();})) {_ttype = Token.SKIP; };
SL_COMMENT : "//" (~'\n')* '\n' {_ttype = Token.SKIP; newline(); };

CHAR_LITERAL : '\'' CHAR '\'';
STRING_LITERAL : '"' (CHAR)* '"';
INT_LITERAL : DIGIT (DIGIT)* | "0x" HEXDIGIT (HEXDIGIT)*;

protected
ESC :  '\\' ( 'n' | '"' | 't' | '\'' | '\\' );

protected
DIGIT : '0'..'9';

protected
HEXDIGIT : DIGIT | 'a'..'f' | 'A'..'F';

protected
CHAR : ESC | ' '..'!' | '#'..'&' | '('..'[' | ']'..'~';

protected
ALPHA : 'a'..'z' | 'A'..'Z' | '_';

protected
ALPHA_NUM : ALPHA | DIGIT;
