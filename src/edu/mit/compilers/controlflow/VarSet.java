package edu.mit.compilers.controlflow;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.ir.Ops;

public class VarSet {
	public Set<Descriptor> variables;
	private List<Ops> opsChecker = Arrays.asList(Ops.MINUS, Ops.PLUS, Ops.DIVIDE, Ops.TIMES, Ops.MOD, Ops.AND, Ops.OR);
	public Ops operator;
	
	
	public VarSet(List<Descriptor> varList, Ops op){
		this.variables = new LinkedHashSet<Descriptor>(varList);
		if (this.opsChecker.contains(op)){
			this.operator = op;
		}
		else{
			throw new UnsupportedOperationException("Tried to initialize a VarSet with an invalid op.");
		}
	}
	
	public boolean contains(Expression express){
		Var variable = (Var)express; // Not sure if needed or if this breaks things because I'm not doing certain checks. May need try/catch block.
		
		if(this.variables.contains(variable.getVarDescriptor())){
			return true;
		}
		return false;
	}
	
}
