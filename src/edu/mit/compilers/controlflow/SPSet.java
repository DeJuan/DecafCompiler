package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.Ops;

public class SPSet {
	public Set<SPSet> SPSets;
	public Set<IR_FieldDecl> varSet;
	public Set<IntLit> intSet;
	public Set<BoolLit> boolSet;
	private List<Ops> opsChecker = Arrays.asList(Ops.MINUS, Ops.PLUS, Ops.DIVIDE, Ops.TIMES, Ops.MOD, Ops.AND, Ops.OR, Ops.NOT);
	public Ops operator;
	
	public SPSet(Ops op){
		SPSets =  new LinkedHashSet<SPSet>();
		varSet = new LinkedHashSet<IR_FieldDecl>();
		intSet = new LinkedHashSet<IntLit>();
		boolSet = new LinkedHashSet<BoolLit>();
		if(this.opsChecker.contains(op)){
			this.operator = op;
		}
		else{
			throw new UnsupportedOperationException("Tried to initialize an SPSet with an invalid operator.");
		}
	}
	
	public SPSet(Set<SPSet> initialSPSet, Set<IR_FieldDecl> initialVarSet, Set<IntLit> intSet, Set<BoolLit> boolSet, Ops op){
		this.SPSets = initialSPSet;
		this.varSet = initialVarSet;
		this.intSet = intSet;
		this.boolSet = boolSet;
		if(this.opsChecker.contains(op)){
			this.operator = op;
		}
		else{
			throw new UnsupportedOperationException("Tried to initialize an SPSet with an invalid operator.");
		}
	}
	
	public void addToSPSets(SPSet newSP){
		if(operator == Ops.NOT){
			if(SPSets.isEmpty() && varSet.isEmpty() && intSet.isEmpty() && boolSet.isEmpty()){
				this.SPSets.add(newSP);
			}
			else{
				throw new UnsupportedOperationException("Tried to add to an SPSet when it wasn't empty for a NOT.");
			}
		}
		else {
			this.SPSets.add(newSP);
		}
	}
	
	public void addToVarSet(IR_FieldDecl newVar){
		if(operator == Ops.NOT){
			if(SPSets.isEmpty() && varSet.isEmpty() && intSet.isEmpty() && boolSet.isEmpty()){
				this.varSet.add(newVar);
			}
			else{
				throw new UnsupportedOperationException("Tried to add to an VarSet when it wasn't empty for a NOT.");
			}
		}
		else {
			this.varSet.add(newVar);
		}
	}
	
	public void addToIntSet(IntLit newInt){
		if(operator == Ops.NOT){
			if(SPSets.isEmpty() && varSet.isEmpty() && intSet.isEmpty() && boolSet.isEmpty()){
				this.intSet.add(newInt);
			}
			else{
				throw new UnsupportedOperationException("Tried to add to an intSet when it wasn't empty for a NOT.");
			}
		}
		else {
			this.intSet.add(newInt);
		}
	}
	
	public void addToBoolSet(BoolLit newBool){
		if(operator == Ops.NOT){
			if(SPSets.isEmpty() && varSet.isEmpty() && intSet.isEmpty() && boolSet.isEmpty()){
				this.boolSet.add(newBool);
			}
			else{
				throw new UnsupportedOperationException("Tried to add to an boolSet when it wasn't empty for a NOT.");
			}
		}
		else {
			this.boolSet.add(newBool);
		}
	}
	
	// *(A,B) + (stuff) 
	//Looking at Mult(A,B)
	//Check whether any SPSets contain complete expressions within themselves.
	//Check each SPSet for a matching operator, and if one is found, then check the equivalence for the lhs and rhs.
	public boolean contains(Expression expr){
		for (SPSet currentSP : SPSets){
			if (currentSP.contains(expr)){
				return true;
			}
		}
		if(expr instanceof Var){
			Var varia = (Var)expr;
			if (varSet.contains(varia.getVarDescriptor())){
				return true;
			}
		}
		
		else if(expr instanceof BinExpr){
			BinExpr binex = (BinExpr)expr;
			Ops searchOp = binex.getOperator();
			List<SPSet> matches = new ArrayList<SPSet>();
			for (SPSet currentSP : SPSets){
				if (currentSP.operator == searchOp){
					matches.add(currentSP);
				}
			}
			for (SPSet matchedSP : matches){
				if(matchedSP.contains(binex.getLeftSide()) && matchedSP.contains(binex.getRightSide())){
					return true;
				}
			}
		}
		else if(expr instanceof NotExpr){
			NotExpr not =(NotExpr)expr;
			
		}
		
		else if(expr instanceof NegateExpr){
			
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		int hash = 0;
		for (SPSet cset : SPSets){
			hash = (hash + cset.hashCode()) % Integer.MAX_VALUE;
		}
		for (IR_FieldDecl decl : varSet){
			hash = (hash + decl.hashCode()) % Integer.MAX_VALUE;
		}
		for (IntLit il : intSet){
			hash = (hash + il.hashCode()) % Integer.MAX_VALUE;
		}
		for (BoolLit bl : boolSet){
			hash = (hash + bl.hashCode()) % Integer.MAX_VALUE;
		}
		return hash;
	}
	
	@Override
	public boolean equals(Object obj){
		if(!(obj instanceof SPSet)){
			return false;
		}
		SPSet sp = (SPSet)obj;
		if(sp.operator == this.operator){
			if(sp.SPSets.equals(this.SPSets)){
				if(!(sp.varSet.size() == this.varSet.size())){
					return false;
				}
				for(IR_FieldDecl currentVar : sp.varSet){
					if(!(this.varSet.contains(currentVar))){
						return false;
					}
				}
				if(!(sp.intSet.size() == this.intSet.size())){
					return false;
				}
				for(IntLit intL : sp.intSet){
				}
			}
		}
		return true;
	}
	
	public String toString(){
		return operator.toString() + "(" + "SPSets: " + SPSets.toString() + " | VarSets: " + varSet.toString() + ")" + System.getProperty("line.separator");
	}
}
