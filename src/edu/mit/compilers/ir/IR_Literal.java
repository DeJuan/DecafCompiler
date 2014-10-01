/**
 * 
 */
package edu.mit.compilers.ir;


/**
 * @author yygu
 *
 */
abstract class IR_Literal extends IR_Node {
	
	public static class IR_IntLiteral extends IR_Literal {
		private Integer value;
		
		public IR_IntLiteral(Integer value) {
			this.value = value;
		}
		
		@Override
		public Type evaluateType() {
			return Type.INT;
		}
		
		@Override
		public boolean isValid() {
			return true;
		}
		
		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}
	
	public static class IR_BoolLiteral extends IR_Literal {
		private boolean value;
		
		public IR_BoolLiteral(boolean value) {
			this.value = value;
		}
		
		@Override
		public Type evaluateType() {
			return Type.BOOL;
		}
		
		@Override
		public boolean isValid() {
			return true;
		}
		
		@Override
		public String toString() {
			return Boolean.toString(value);
		}
	}
	
	public static class IR_StringLiteral extends IR_Literal {
	    private String value;
	    
	    public IR_StringLiteral(String text) {
	        value = text.substring(1, text.length() - 1);
	    }
	    
	    @Override
	    public Type evaluateType() {
	        return Type.STRING;
	    }
	    
	    @Override
	    public boolean isValid() {
	        return true;
	    }
	    
	    @Override
	    public String toString() {
	        return value;
	    }
	}
	
}
