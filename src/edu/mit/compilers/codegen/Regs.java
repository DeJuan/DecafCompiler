package edu.mit.compilers.codegen;

public enum Regs {
	RAX("%rax"), RBX("%rbx"), 
	RCX("%rcx"), RDX("%rdx"), RSP("%rsp"),
	RBP("%rbp"), RSI("%rsi"), RDI("%rdi"),
	R8("%r8"),   R9("%r9"),   R10("%r10"), R11("%r11"),
	R12("%r12"), R13("%r13"), R14("%r14"), R15("%r15"),
	RIP("%rip"),
	/**@brieflower bits of rax. used for comparison.*/
	AL("%al");
	

private final String text;

private Regs(final String text) {
    this.text = text;
}

@Override
public String toString() {
    return text;
}
}