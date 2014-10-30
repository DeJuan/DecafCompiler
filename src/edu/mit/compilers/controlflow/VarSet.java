package edu.mit.compilers.controlflow;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.ir.Ops;

public class VarSet {
	public Set<Var> variables;
	private List<Ops> opsChecker = Arrays.asList(Ops.MINUS, Ops.PLUS, Ops.DIVIDE, Ops.TIMES, Ops.MOD, Ops.AND, Ops.OR);
	public Ops operator;
	
	
	public VarSet(List<Var> varList, Ops op){
		this.variables = new LinkedHashSet<Var>(varList);
		if (this.opsChecker.contains(op)){
			this.operator = op;
		}
		else{
			throw new UnsupportedOperationException("Tried to initialize a VarSet with an invalid op.");
		}
	}
	
}
