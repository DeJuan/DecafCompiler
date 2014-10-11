package edu.mit.compilers.codegen;

import edu.mit.compilers.ir.Type;

/**@brief not just memory locations.
 * A super class of things that can be argument of assembly commands,
 * in particular literals.
 */
abstract public class LocationMem {
	
	public enum LocType{
		STACK_LOC, LABEL_LOC, LITERAL_LOC, ARRAY_LOC,
		REG_LOC, JUMP_LOC
	};
	
	abstract public LocType getType(); 
	
	public long getValue(){
		return 0;
	}
		
	public class ArrayLoc extends LocationMem{
		/**@brief there are 4 combinations of acceptable 
		 * array addressing:
		 * label + literal  
		 * label + register
		 * stack + literal
		 * stack + register
		 */
		public LocationMem array;
		/**@brief if offset is a literal
		 * its units is bytes.
		 */
		public LocationMem offset;
		public Type type;
		
		public String toString(){
			StringBuilder sb = new StringBuilder();
			switch(array.getType()){
			case LABEL_LOC:
				sb.append(array.toString());
				if(offset.getType()==LocType.REG_LOC){
					sb.append("(,"+offset.toString()+", "+CodegenConst.INT_SIZE +")");
				}else if(offset.getType()==LocType.LITERAL_LOC){
					long len = offset.getValue();
					sb.append("+" + len + "("+Regs.RIP+")" );
				}
				break;
			case STACK_LOC:
				long len = array.getValue();
				if(offset.getType()==LocType.REG_LOC){
					sb.append(len+"("+Regs.RBP+", "+offset.toString()+", "+
							CodegenConst.INT_SIZE+")");
				}else if(offset.getType()==LocType.LITERAL_LOC){
					len += offset.getValue();
					sb.append(len+"("+Regs.RBP+")");
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
	}
}
