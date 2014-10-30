package edu.mit.compilers.controlflow;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.ir.Ops;

public class SPSet {
	public Set<SPSet> SPSets;
	public Set<VarSet> varSets;
	private List<Ops> opsChecker = Arrays.asList(Ops.MINUS, Ops.PLUS, Ops.DIVIDE, Ops.TIMES, Ops.MOD, Ops.AND, Ops.OR);
	public Ops operator;
	
	public SPSet(Ops op){
		SPSets =  new LinkedHashSet<SPSet>();
		varSets = new LinkedHashSet<VarSet>();
		if(this.opsChecker.contains(op)){
			this.operator = op;
		}
		else{
			throw new UnsupportedOperationException("Tried to initialize an SPSet with an invalid operator.");
		}
	}
	
	public SPSet(Set<SPSet> initialSPSet, Set<VarSet> initialVarSet, Ops op){
		this.SPSets = initialSPSet;
		this.varSets = initialVarSet;
		if(this.opsChecker.contains(op)){
			this.operator = op;
		}
		else{
			throw new UnsupportedOperationException("Tried to initialize an SPSet with an invalid operator.");
		}
	}
	
	public void addToSPSet(SPSet newSP){
		this.SPSets.add(newSP);
	}
	
	public void addToVarSet(VarSet newVar){
		this.varSets.add(newVar);
	}
	
	public boolean contains(Expression expr){
		for (SPSet currentSP : SPSets){
			if (currentSP.contains(expr)){
				return true;
			}
			else{
				continue;
			}
		}
		
		for (VarSet currentVarSet : varSets){
			if (currentVarSet.contains(expr)){
				return true;
			}
			else{
				continue;
			}
		}
		return false;
	}
	
	
}
