package edu.mit.compilers.codegen;

public class CodegenConst {
	//add more platform dependent constants as necessary.
	
	
	public static final int INT_SIZE=8;
	public static final int BOOL_SIZE=8;
	// alignment for global variables.
	public static final int ALIGN_SIZE=8;
	// number of arguments in registers
	public static final int N_REG_ARG=6;
	
	// representing booleans with int
	public static final int BOOL_FALSE=0;
	public static final int BOOL_TRUE=1;
	
	public static final int ERR_ARRAY_BOUND=-1;
	public static final int ERR_FUN_RET=-2;
}
