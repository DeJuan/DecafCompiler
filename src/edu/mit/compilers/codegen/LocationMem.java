package edu.mit.compilers.codegen;

/**@brief not just memory locations.
 * A super class of things that can be argument of assembly commands,
 * in particular literals.
 */
abstract public class LocationMem {
	
	public enum LocType{
		STACK_LOC, LABEL_LOC, LITERAL_LOC, ARRAY_LOC,
		REG_LOC
	};
	
	abstract public LocType getType(); 
	
	public long getValue(){
		return 0;
	}
}
