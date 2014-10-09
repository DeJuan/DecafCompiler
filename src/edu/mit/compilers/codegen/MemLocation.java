package edu.mit.compilers.codegen;

abstract public class MemLocation {
	abstract public String getLocation();
	
	public static class StackLocation{
		/**@brief with respect to rbp
		 */
		public long offset;
	}
	
	public static class GlobalLocation{
		/**@brief label for global variables and arrays
		 */
		public String label;
	}
}
