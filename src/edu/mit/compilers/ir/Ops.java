package edu.mit.compilers.ir;

public enum Ops {
ASSIGN("="), ASSIGN_PLUS("+="), ASSIGN_MINUS("-="), PLUS("+"), MINUS("-"), TIMES("*"), DIVIDE("/"), MOD("%"),
LT("<"),LTE("<="),GT(">"),GTE(","),EQUALS("=="),NOT_EQUALS("!="), AND("&&"), OR("||"), NOT("!"), NEGATE("-");

private final String text;

/**
 * @param text
 */
private Ops(final String text) {
    this.text = text;
}

/* (non-Javadoc)
 * @see java.lang.Enum#toString()
 */
@Override
public String toString() {
    return text;
}
}
