package edu.mit.compilers.controlflow;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.Ops;

public class SPSet {
	public Set<SPSet> SPSets;
	public Set<IR_FieldDecl> varSet;
	private List<Ops> opsChecker = Arrays.asList(Ops.MINUS, Ops.PLUS, Ops.DIVIDE, Ops.TIMES, Ops.MOD, Ops.AND, Ops.OR);
	public Ops operator;
	
	public SPSet(Ops op){
		SPSets =  new LinkedHashSet<SPSet>();
		varSet = new LinkedHashSet<IR_FieldDecl>();
		if(this.opsChecker.contains(op)){
			this.operator = op;
		}
		else{
			throw new UnsupportedOperationException("Tried to initialize an SPSet with an invalid operator.");
		}
	}
	
	public SPSet(Set<SPSet> initialSPSet, Set<IR_FieldDecl> initialVarSet, Ops op){
		this.SPSets = initialSPSet;
		this.varSet = initialVarSet;
		if(this.opsChecker.contains(op)){
			this.operator = op;
		}
		else{
			throw new UnsupportedOperationException("Tried to initialize an SPSet with an invalid operator.");
		}
	}
	
	public void addToSPSets(SPSet newSP){
		this.SPSets.add(newSP);
	}
	
	public void addToVarSet(IR_FieldDecl newVar){
		this.varSet.add(newVar);
	}
	
	public boolean contains(Expression expr){
		for (SPSet currentSP : SPSets){
			if (currentSP.contains(expr)){
				return true;
			}
		}	
		if (varSet.contains(expr)){
			return true;
		}
		return false;
	}
}
