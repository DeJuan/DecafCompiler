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
			
		}
		
		else if(expr instanceof Ternary){
			
		}
		
		else if(expr instanceof NegateExpr){
			
		}
		return false;
	}
	
	public String toString(){
		return operator.toString() + "(" + "SPSets: " + SPSets.toString() + " | VarSets: " + varSet.toString() + ")" + System.getProperty("line.separator");
	}
}
