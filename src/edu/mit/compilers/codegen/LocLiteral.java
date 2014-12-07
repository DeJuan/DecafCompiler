package edu.mit.compilers.codegen;

/**@brief for long literals
 */
public class LocLiteral extends LocationMem{
	private long val;
	public LocLiteral(long v){
		val = v;
	}
	public String toString(){
		return "$"+val;
	}
	public LocType getType(){
		return LocType.LITERAL_LOC;
	}
	public long getValue(){
		return val;
	}
	@Override
	public boolean equals(LocationMem other) {
		if(!(other instanceof LocLiteral)){
			return false;
		}
		else{
			LocLiteral otherLoc = (LocLiteral)other;
			if(otherLoc.getValue() == this.getValue()){
				return true;
			}
		}
		return false;
	}
}

