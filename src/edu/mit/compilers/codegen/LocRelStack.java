package edu.mit.compilers.codegen;


public class LocRelStack extends LocationMem {

    /**@brief with respect to rbp. Measured in bytes.
     */
    public long offset;
    public LocRelStack(){
        offset = 0;
    }
    public LocRelStack(long o){
        offset = o;
    }
    public String toString(){
        return offset+"(%rsp)";
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
    
    public LocRelStack clone(){
        return new LocRelStack(offset);
    }
	
    public boolean equals(LocationMem other) {
		if(!(other instanceof LocRelStack)){
			return false;
		}
		else{
			LocRelStack otherLoc = (LocRelStack)other;
			if(otherLoc.getValue() == this.offset){
				return true;
			}
		}
		return false;
	}

}
