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

// reserved words can be designated as tokens to save some typing.
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
  BLOCK;
  FIELD_DECL;
  LOCATION;
  METHOD_CALL;
  METHOD_DECL;
  PROGRAM;
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

// Symbols are added on a need basis
LPAREN     : "(";
RPAREN     : ")";

LCURLY     : "{";
RCURLY     : "}";

LBRACKET   : "[";
RBRACKET   : "]";

ASSIGN_PLUS  : "+=";
ASSIGN_MINUS : "-=";
ASSIGN     : "=";

PLUS 	   : "+";
MINUS      : "-";
TIMES      : "*";
DIVIDE     : "/";
MOD        : "%";

LT         : "<";
GT         : ">";
LTE        : "<=";
GTE        : ">=";
EQUALS     : "==";
NOT_EQUALS : "!=";

AND        : "&&";
OR         : "||";
BANG	   : "!";

SEMICOLON  : ";";
COMMA 	   : ",";
COLON 	   : ":";
QUESTION   : "?";
AT         : "@";

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
