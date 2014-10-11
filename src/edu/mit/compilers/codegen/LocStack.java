package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.LocationMem.LocType;

public class LocStack extends LocationMem{
	/**@brief with respect to rbp. Measured in bytes.
	 */
	public long offset;
	public LocStack(){
		offset = 0;
	}
	public LocStack(long o){
		offset = o;
	}
	public String toString(){
		return offset+"(%rbp)";
	}
	
	public LocType getType(){
		return LocType.STACK_LOC;
	}
	
	public long getValue(){
		return offset;
	}
}
