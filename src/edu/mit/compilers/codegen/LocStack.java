package edu.mit.compilers.codegen;

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
	
	public void setValue(long v){
		offset = v;
	}
	
	public LocStack clone(){
		return new LocStack(offset);
	}
	
	public boolean equals(LocationMem other){
		if(!(other instanceof LocStack)){
			return false;
		}
		else{
			LocStack otherLoc = (LocStack)other;
			if(otherLoc.getValue() == (this.offset)){
				return true;
			}
		}
		return false;
	}
}
