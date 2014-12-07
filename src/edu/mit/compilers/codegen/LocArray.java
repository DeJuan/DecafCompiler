package edu.mit.compilers.codegen;

public class LocArray extends LocationMem{
	/**@brief there are 4 combinations of acceptable 
	 * array addressing:
	 * label + literal  
	 * label + register
	 * stack + literal
	 * stack + register
	 */
	public LocationMem array;
	/**@brief Unit is number of items.
	 */
	public LocationMem offset;
	public int itemSize;
	
	public LocArray(LocationMem array, LocationMem offset, int s) {
		this.array = array;
		this.offset = offset;
		itemSize = s;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		switch(array.getType()){
		case LABEL_LOC:
			sb.append(array.toString());
			if (offset.getType() == LocType.REG_LOC) {
				sb.append("(," + offset.toString() + ", " + itemSize +")");
			} else if (offset.getType() == LocType.LITERAL_LOC) {
				long len = itemSize * offset.getValue();
				//sb.append("+" + len + "(" + Regs.RIP + ")");
        sb.append("+" + len);
			}
			break;
		case STACK_LOC:
			long len = array.getValue();
			if(offset.getType() == LocType.REG_LOC){
				sb.append(len + "(" + Regs.RBP + ", " + offset.toString() + ", " +
						itemSize + ")");
			} else if (offset.getType() == LocType.LITERAL_LOC) {
				len += itemSize * offset.getValue();
				sb.append(len + "(" + Regs.RBP + ")");
			}
			break;
		default:
			//This shouldn't be reachable when we compile any program.
			System.out.println("Internal error: invalid array location.");
			break;
		}
		return sb.toString();
	}
	
	public LocType getType(){
		return LocType.ARRAY_LOC;
	}

	@Override
	public boolean equals(LocationMem other) {
		if(!(other instanceof LocArray)){
			return false;
		}
		LocArray otherLoc = (LocArray)other;
		
		if(!otherLoc.array.equals(this.array)){
			return false;
		}
		
		if (otherLoc.offset.equals(this.offset)){
			return true;
		}
		return false;
	}
}
