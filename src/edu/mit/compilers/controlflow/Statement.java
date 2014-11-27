package edu.mit.compilers.controlflow;

public abstract class Statement {
	
	protected Bitvector liveMap;
	public enum StatementType{
		ASSIGNMENT, METHOD_CALL_STATEMENT, DECLARATION
	};
	
	public abstract StatementType getStatementType();
	
	public Bitvector getLiveMap() {
		return liveMap;
	}
	
	public void setLiveMap(Bitvector bv) {
		this.liveMap = bv;
	}
}
