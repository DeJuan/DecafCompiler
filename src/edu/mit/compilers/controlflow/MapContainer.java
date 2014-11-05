package edu.mit.compilers.controlflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.ir.IR_FieldDecl;

public class MapContainer {
	Map<IR_FieldDecl, ValueID> varToVal = new HashMap<IR_FieldDecl, ValueID>();
	Map<SPSet, ValueID> expToVal = new HashMap<SPSet, ValueID>();
	Map<SPSet, Var> expToTemp = new HashMap<SPSet, Var>();
	Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents = new HashMap<IR_FieldDecl, Map<SPSet, ValueID>>();
	Map<ValueID, List<Var>> valToVar = new HashMap<ValueID, List<Var>>();
	
	public MapContainer(Map<IR_FieldDecl, ValueID> varToVal, 
	Map<SPSet, ValueID> expToVal,
	Map<SPSet, Var> expToTemp,
	Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents,
	Map<ValueID, List<Var>> valToVar){
		this.varToVal = varToVal;
		this.expToVal = expToVal;
		this.expToTemp = expToTemp;
		this.varToValForArrayComponents = varToValForArrayComponents;
		this.valToVar = valToVar;
	}
	
	/*
	 * 	
		//TODO: make sure keys can be shallow copied 
		for(IR_FieldDecl i : varToVal.keySet()){
			this.varToVal.put(i, varToVal.get(i));
		}
		
		for(SPSet s : expToVal.keySet()){
			this.expToVal.put(s, expToVal.get(s));
		}

		for(SPSet s : expToTemp.keySet()){
			this.expToTemp.put(s,new Var(expToTemp.get(s)));
		}
		
		for(IR_FieldDecl i : varToValForArrayComponents.keySet()){
			Map<SPSet, ValueID> map = varToValForArrayComponents.get(i);
			Map<SPSet, ValueID> newMap = new HashMap<>();
			for(SPSet s : map.keySet()){
				newMap.put(s, newMap.get(s));
			}
			this.varToValForArrayComponents.put(i, newMap);
		}
		
		this.valToVar = valToVar;
	}*/
	
	public void calculateIntersection(MapContainer otherContainer){
		Set<IR_FieldDecl> otherVarToVal =  otherContainer.varToVal.keySet();
		for (IR_FieldDecl localDecl: this.varToVal.keySet()){
			if(!otherVarToVal.contains(localDecl)){
				varToVal.remove(localDecl);
			}
		}
		
		Set<SPSet> otherExpToVal = otherContainer.expToVal.keySet();
		for (SPSet sp : expToVal.keySet()){
			if(!otherExpToVal.contains(sp)){
				expToVal.remove(sp);
			}
		}
		
		Set<SPSet> otherExpToTemp = otherContainer.expToTemp.keySet();
		for(SPSet sp : expToTemp.keySet()){
			if(!otherExpToTemp.contains(sp)){
				expToTemp.remove(sp);
			}
		}
		
		Set<IR_FieldDecl> otherVarToValForArrayComponents = otherContainer.varToValForArrayComponents.keySet();
		for(IR_FieldDecl declKey : varToValForArrayComponents.keySet()){
			if(!otherVarToValForArrayComponents.contains(declKey)){
				varToValForArrayComponents.remove(declKey); //Don't need to check the sets, if the mapping's not there at all it wouldn't matter anyway
			}
		}
		
		Set<ValueID> otherValToVar = otherContainer.valToVar.keySet();
		for(ValueID valID : valToVar.keySet()){
			if(!otherValToVar.contains(valID)){
				valToVar.remove(valID);
			}
		}
	}
}
