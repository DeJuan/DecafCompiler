package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.codegen.LocLabel;
import edu.mit.compilers.ir.IR_FieldDecl;

public class MapContainer {
    Map<IR_FieldDecl, ValueID> varToVal = new HashMap<IR_FieldDecl, ValueID>();
    Map<SPSet, ValueID> expToVal = new HashMap<SPSet, ValueID>();
    Map<SPSet, Var> expToTemp = new HashMap<SPSet, Var>();
    Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents = new HashMap<IR_FieldDecl, Map<SPSet, ValueID>>();
    Map<ValueID, List<Var>> valToVar = new HashMap<ValueID, List<Var>>();
    boolean complete;

    public MapContainer(Map<IR_FieldDecl, ValueID> varToVal, 
            Map<SPSet, ValueID> expToVal,
            Map<SPSet, Var> expToTemp,
            Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponents,
            Map<ValueID, List<Var>> valToVar, boolean complete){
        this.varToVal = varToVal;
        this.expToVal = expToVal;
        this.expToTemp = expToTemp;
        this.varToValForArrayComponents = varToValForArrayComponents;
        this.valToVar = valToVar;
        this.complete = complete;
        
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
    
    public static MapContainer makeEmptyContainer(){
    	Map<IR_FieldDecl, ValueID> varToValN = new HashMap<IR_FieldDecl, ValueID>();
        Map<SPSet, ValueID> expToValN = new HashMap<SPSet, ValueID>();
        Map<SPSet, Var> expToTempN = new HashMap<SPSet, Var>();
        Map<IR_FieldDecl, Map<SPSet, ValueID>> varToValForArrayComponentsN = new HashMap<IR_FieldDecl, Map<SPSet, ValueID>>();
        Map<ValueID, List<Var>> valToVarN = new HashMap<ValueID, List<Var>>();
        return new MapContainer(varToValN, expToValN, expToTempN, varToValForArrayComponentsN, valToVarN, false);
    }
    
    public static MapContainer keepGlobals(MapContainer startingContainer, List<IR_FieldDecl> globals){
        MapContainer newContainer = makeEmptyContainer();
        if (startingContainer == null) {
            return newContainer;
        }
        for (IR_FieldDecl glob : globals) {
            ValueID id = startingContainer.varToVal.get(glob);
            if (glob.getLength() != null || id != null) {
                if (glob.getLength() != null && id == null) {
                    throw new RuntimeException("ArrayID should have been set in original MapContainer");
                }
                newContainer.varToVal.put(glob, id);
                if (!newContainer.valToVar.containsKey(id)) {
                    newContainer.valToVar.put(id, new ArrayList<Var>());
                }
                Descriptor globDesc = new Descriptor(glob);
                globDesc.setLocation(new LocLabel(glob.getName()));
                newContainer.valToVar.get(id).add(new Var(globDesc, null));
            }
            if (glob.getLength() != null) {
                newContainer.varToValForArrayComponents.put(glob, startingContainer.varToValForArrayComponents.get(glob));
            }
        }
        return newContainer;
    }
    
    public MapContainer calculateIntersection(MapContainer otherContainer){
    	if (otherContainer.equals(this)){
    		return new MapContainer(new HashMap<IR_FieldDecl, ValueID>(varToVal), new HashMap<SPSet, ValueID>(expToVal), new HashMap<SPSet, Var>(expToTemp), 
    		        new HashMap<IR_FieldDecl, Map<SPSet, ValueID>>(varToValForArrayComponents), new HashMap<ValueID, List<Var>>(valToVar), complete);
    	}
        Map<IR_FieldDecl, ValueID> newVarToVal = new HashMap<IR_FieldDecl, ValueID>();
        for (IR_FieldDecl localDecl: this.varToVal.keySet()){
            if(varToVal.get(localDecl) == otherContainer.varToVal.get(localDecl)){
                ValueID id = varToVal.get(localDecl);
                newVarToVal.put(localDecl, id);
            }
        }

        Map<SPSet, ValueID> newExprToVal = new HashMap<SPSet, ValueID>();
        for (SPSet sp : expToVal.keySet()){
            ValueID ownVal = expToVal.get(sp);
            ValueID otherVal = otherContainer.expToVal.get(sp);
            if(ownVal == otherVal){
                newExprToVal.put(SPSet.copy(sp), ownVal);
            }
        }

        Map<SPSet, Var> newExprToTemp = new HashMap<SPSet, Var>();
        for(SPSet sp : expToTemp.keySet()){
            Var myVar = expToTemp.get(sp);
            Var otherVar = otherContainer.expToTemp.get(sp);
            if(otherVar != null && myVar.getVarDescriptor().getIR() == otherVar.getVarDescriptor().getIR()){
                newExprToTemp.put(SPSet.copy(sp), myVar);
            }
        }

        Set<IR_FieldDecl> otherVarToValForArrayComponents = otherContainer.varToValForArrayComponents.keySet();
        Map<IR_FieldDecl, Map<SPSet, ValueID>> newComponents = new HashMap<IR_FieldDecl, Map<SPSet, ValueID>>();
        for(IR_FieldDecl declKey : varToValForArrayComponents.keySet()){
            if(!otherVarToValForArrayComponents.contains(declKey)){
                continue; //Don't need to check the sets, if the mapping's not there at all it wouldn't matter anyway
            }//However, this is not enough alone; what if there are keys that are contained but their sets have changed? INVESTIGATE!
            else {
                Map<SPSet, ValueID> currentMap = varToValForArrayComponents.get(declKey);
                Map<SPSet, ValueID> otherMap = otherContainer.varToValForArrayComponents.get(declKey);
                Map<SPSet, ValueID> newMap = new HashMap<SPSet, ValueID>();
                boolean valid = true;
                for (SPSet sp : currentMap.keySet()) {
                    if (currentMap.get(sp) == otherMap.get(sp)) {
                        newMap.put(SPSet.copy(sp), currentMap.get(sp));
                    } else {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    newComponents.put(declKey, newMap);
                }
            }
        }

        Map<ValueID, List<Var>> newValToVar = new HashMap<ValueID, List<Var>>();
        for(ValueID valID : valToVar.keySet()){
            List<Var> newList = new ArrayList<Var>();
            for (Var v : valToVar.get(valID)) {
                if (v.getIndex() == null) {
                    if (newVarToVal.get(v.getDecl()) == valID) {
                        newList.add(v);
                    }
                } else {
                    if (Optimizer.setVarIDs(newVarToVal, newComponents, v.getIndex())) {
                        if (newComponents.containsKey(v.getDecl()) && newComponents.get(v.getDecl()).get(new SPSet(v.getIndex()))  == valID) {
                            newList.add(v);
                        }
                    }
                }
            }
            newValToVar.put(valID, newList);
        }
        return new MapContainer(newVarToVal, newExprToVal, newExprToTemp, newComponents, newValToVar, complete && otherContainer.complete);
    }
}
