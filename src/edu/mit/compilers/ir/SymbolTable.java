package edu.mit.compilers.ir;

import java.util.HashMap;
import java.util.ArrayList;

public class SymbolTable {
	
	private ArrayList<HashMap<String, IR_Node> > tables;
	
	public SymbolTable(){
		tables = new ArrayList<HashMap<String, IR_Node>>();
	}
	
	public void incScope(){
		tables.add(new HashMap<String, IR_Node>());
	}

	public void decScope(){
		if(tables.size()==0){
			System.err.println("Symbol table error: Already at outmost scope.");
		}
		tables.remove(tables.size()-1);
	}

	public IR_Node lookupLocal(String name){
		HashMap<String, IR_Node> t = tables.get(tables.size()-1);
		if (t.containsKey(name)) {
			return t.get(name);
		}
		return null;
	}
	
	public IR_Node lookup(String name){
		for(int ii = tables.size()-1; ii>=0 ; ii--){
			HashMap<String, IR_Node> t = tables.get(ii);
			if(t.containsKey(name)){
				return t.get(name);
			}
		}
		return null;
	}
	
	public boolean put(String name, IR_Node node){
		HashMap<String, IR_Node> t = tables.get(tables.size()-1);
		if(t.containsKey(name)){
			return false;
		}
		t.put(name, node);
		return true;
	}
	
	public HashMap<String, IR_Node> getTable(int idx){
		return tables.get(idx);
	}
	
	public void clear(){
		tables.clear();
	}
	
	public int getNumScopes(){
		return tables.size();
	}
}
