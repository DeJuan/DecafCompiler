package edu.mit.compilers.controlflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Bitvector {
	private Map<String, Integer> vector = new HashMap<String, Integer>();
	
	public Bitvector(Set<String> allVarNames){
		for (String var : allVarNames){
			this.vector.put(var, 0); //initialize everything to 0s  
		}
	}
	
	public Bitvector(Map<String, Integer> startingZeroVector){
		this.vector = startingZeroVector;
	}
	
	public Bitvector(Bitvector vectorToCopy){
		this.vector = vectorToCopy.copyVectorMap();
	}
	
	public void setVectorVal(String variable, int value){
		this.vector.put(variable, value);
	}
	
	public Map<String, Integer> getVectorMap(){
		return this.vector;
	}
	
	public int get(String variableName){
		return this.vector.get(variableName);
	}
	
	/**
	 * Makes absolutely certain to
	 * deep copy the String, Integer map that I use to track bit vectors.
	 * These are immutables so this is overkill, but whatever.
	 * @param liveVector : The bitvector map we want to copy.
	 */
	public Map<String, Integer> copyVectorMap(){
		Map<String, Integer> vectorCopy = new HashMap<String, Integer>();
		for (String key : vector.keySet()){
			vectorCopy.put(new String(key), new Integer(vector.get(key)));
		}
		return vectorCopy;
	}
	
	public Bitvector copyBitvector(){
		Map<String, Integer> mapCopy = copyVectorMap();
		return new Bitvector(mapCopy);
	}
	
	public boolean compareBitvectorEquality(Bitvector other){
		Map<String, Integer> otherMap = other.getVectorMap();
		for (String key : otherMap.keySet()){
			if (!otherMap.get(key).equals(vector.get(key))){
				return false;
			}
		}
		return true;
	}
	/**
	 * Simple helper method for computing the unison of bitvectors.
	 * It starts with an all zero bitvector, and scans through all the vectors that need to be unisoned; 
	 * If an entry in one of the scanned vectors is one, the originally all-zero bitvector is updated to have a 1 in that position.
	 * Thus, once iteration is finished, if any vector in the set for unification had a one in a given position, the final bitvector will have a one
	 * in that position, otherwise, it will have a zero. This is correct behavior.
	 * 
	 * @param children : List<FlowNode> of the children for the current FlowNode. Since we walk backwards, need children, not parents.
	 * @param vectorStorage : The Map<FlowNode, Map<String, Integer>> we use to keep track of the bit vector for a given FlowNode at exit of that node. 
	 * @param Set<String> allVars : List of all Var names in the program, so we can easily initialize the all-zero bitvector. 
	 */
	public static Bitvector childVectorUnison(List<FlowNode> children, Map<FlowNode, Bitvector> vectorStorageOUT, Bitvector zeroVector) {
		Map<String, Integer> nextMap;
		Bitvector finalMap = zeroVector.copyBitvector(); //make new bit vector of all zeros
		for(int i = 0; i < children.size(); i++){ //For all children of this node
			nextMap = vectorStorageOUT.get(children.get(i)).getVectorMap(); //get their map from storage
			for(String key : nextMap.keySet()){ //iterate through the keys in the map
				if(nextMap.get(key) == 1){ //if we find a key with a value of one
					finalMap.setVectorVal(key, 1); //put 1 in the finalMap in that location. 
				}
			}
		}
		return finalMap; //Any map that had a 1 for a var will have set a 1 for the corresponding place in finalMap; everything else will be 0 as intended.
	}
	
	public Bitvector vectorUnison(Bitvector other){
		Bitvector currentCopy = new Bitvector(this.copyVectorMap());
		Map<String, Integer> otherMap = other.getVectorMap();
		for (String key : otherMap.keySet()){
			if(otherMap.get(key) == 1){
				currentCopy.setVectorVal(key, 1);
			}
		}
		return currentCopy;
	}
	
}
