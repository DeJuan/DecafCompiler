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
}
