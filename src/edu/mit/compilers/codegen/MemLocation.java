package edu.mit.compilers.codegen;

abstract public class MemLocation {
	
	public enum LocType{
		STACK_LOC, LABEL_LOC, LITERAL_LOC, ARRAY_LOC,
		REG_LOC
	};
	
	public enum Regs{
		RAX, RBX, RCX, RDX, RSP, RBP, RSI, RDI,R8,R9,R10,R11
	};
	
	abstract public LocType getType(); 
	
	public class StackLocation{
		/**@brief with respect to rbp
		 */
		public long offset;
		public String toString(){
			return offset+"(%rbp)";
		}
		
		public LocType getType(){
			return LocType.STACK_LOC;
		}
	}
	
	public class LabelLocation{
		/**@brief label for global variables and arrays
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
	}
	
	public class RegLocation{
		public Regs reg;
		public String toString(){
			return "%"+reg;
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
		public MemLocation offset;
		public String toString(){
			switch(array.getType()){
			case LABEL_LOC:
				if(offset.getType()==LocType.REG_LOC){
					
				}else if(offset.getType()==LocType.LITERAL_LOC){
					
				}
				break;
			case STACK_LOC:
				if(offset.getType()==LocType.REG_LOC){
					
				}else if(offset.getType()==LocType.LITERAL_LOC){
					
				}
				break;
			default:
				break;
			}
			return null;
		}
		
		public LocType getType(){
			return LocType.ARRAY_LOC;
		}
	}
}
