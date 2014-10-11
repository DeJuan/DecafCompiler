package edu.mit.compilers.ir;


/**
 * IR_Node for literals (string, boolean, integer).
 * The constructor assigns the value of the literal to the 'value' field.
 *
 */
public abstract class IR_Literal extends IR_Node {
	
	public static class IR_IntLiteral extends IR_Literal {
		private Long value;
		
		public IR_IntLiteral(Long value) {
			this.value = value;
		}
		
		public long getValue() {
		    return value;
		}
		
		@Override
		public Type getType() {
			return Type.INT;
		}
	
		@Override
		public String toString() {
			return Long.toString(value);
		}
	}
	
	public static class IR_BoolLiteral extends IR_Literal {
		private boolean value;
		
		public IR_BoolLiteral(boolean value) {
			this.value = value;
		}
		
		public boolean getValue() {
		    return value;
		}
		
		@Override
		public Type getType() {
			return Type.BOOL;
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
	    
	    public String getValue() {
	        return value;
	    }
	    
	    @Override
	    public Type getType() {
	        return Type.STRING;
	    }
	    
	    @Override
	    public String toString() {
	        return value;
	    }
	}
	
}
