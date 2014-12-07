 package edu.mit.compilers.codegen;

public class LocLabel extends LocationMem{
	/**@brief label for global variables, arrays and jump labels
	 */
	public String label;
	
	public LocLabel(String l){
		label = l;
	}
	
	public String toString(){
		return label;
	}
	public LocType getType(){
		return LocType.LABEL_LOC;
	}
	
	public boolean equals(LocationMem other){
		if(!(other instanceof LocLabel)){
			return false;
		}
		else{
			LocLabel otherLoc = (LocLabel)other;
			if(otherLoc.toString().equals(this.toString())){
				return true;
			}
		}
		return false;
	}
}

