package edu.mit.compilers.codegen;

public class LocReg extends LocationMem{
	public Regs reg;
	public LocReg(Regs r){
		reg =r;
	}
	public String toString(){
		return reg.toString();
	}
	public LocType getType(){
		return LocType.REG_LOC;
	}
	
	public boolean equals(LocationMem other){
		if(!(other instanceof LocReg)){
			return false;
		}
		else{
			LocReg otherLoc = (LocReg)other;
			if(otherLoc.reg.name() == this.reg.name()){
				return true;
			}
		}
		return false;
	}
}
