package edu.mit.compilers.codegen;

/**@brief Location represented by a jump label.
 * can be destination of jump instructions and also function names.
 */
public class LocJump extends LocationMem {
	public String label;
	public LocJump(String l){
		label = l;
	}
	public String toString(){
		return label;
	}
	@Override
	public LocType getType() {
		// TODO Auto-generated method stub
		return LocType.JUMP_LOC;
	}

}
