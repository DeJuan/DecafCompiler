package edu.mit.compilers.codegen;

import edu.mit.compilers.ir.Type;

abstract public class MemLocation {
	
	public enum LocType{
		STACK_LOC, LABEL_LOC, LITERAL_LOC, ARRAY_LOC,
		REG_LOC
	};
	
	abstract public LocType getType(); 
	
	public long getValue(){
		return 0;
	}
	
	public class StackLocation{
		/**@brief with respect to rbp. Measured in bytes.
		 */
		public long offset;
		public String toString(){
			return offset+"(%rbp)";
		}
		
		public LocType getType(){
			return LocType.STACK_LOC;
		}
		
		public long getValue(){
			return offset;
		}
	}
	
	public class LabelLocation{
		/**@brief label for global variables, arrays and jump labels
		 */
		public String label;
		public String toString(){
			return label;
		}
		public LocType getType(){
			return LocType.LABEL_LOC;
		}
	}
	
	/**@brief for int literals
	 * 
	 * @author desaic
	 *
	 */
	public class LiteralLocation{
		public long val;
		public String toString(){
			return "$"+val;
		}
		public LocType getType(){
			return LocType.LITERAL_LOC;
		}
		public long getValue(){
			return val;
		}
	}
	
	public class RegLocation{
		public Regs reg;
		public String toString(){
			return reg.toString();
		}
		public LocType getType(){
			return LocType.REG_LOC;
		}
	}
	
	public class ArrayLocation{
		/**@brief there are 4 combinations of acceptable 
		 * array addressing:
		 * label + literal  
		 * label + register
		 * stack + literal
		 * stack + register
		 */
		public MemLocation array;
		/**@brief if offset is a literal
		 * its units is bytes.
		 */
		public MemLocation offset;
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
