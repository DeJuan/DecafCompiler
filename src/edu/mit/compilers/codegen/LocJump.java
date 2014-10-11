package edu.mit.compilers.codegen;

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
