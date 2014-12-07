package edu.mit.compilers.regalloc;

import java.util.HashMap;
import java.util.ArrayList;

public class RegisterTable<V> {
	
	private ArrayList<HashMap<String, V> > tables;
	
	public RegisterTable(){
		tables = new ArrayList<HashMap<String, V>>();
	}
	
	public void incScope(){
		tables.add(new HashMap<String, V>());
	}

	public void decScope(){
		if(tables.size()==0){
			System.err.println("Symbol table error: Already at outmost scope.");
		}
		tables.remove(tables.size()-1);
	}

	public V lookupLocal(String name){
		HashMap<String, V> t = tables.get(tables.size()-1);
		if (t.containsKey(name)) {
			return t.get(name);
		}
		return null;
	}
	
	public V lookup(String name){
		for(int ii = tables.size()-1; ii>=0 ; ii--){
			HashMap<String, V> t = tables.get(ii);
			if(t.containsKey(name)){
				return t.get(name);
			}
		}
		return null;
	}
	
	public boolean put(String name, V node){
		HashMap<String, V> t = tables.get(tables.size()-1);
		t.put(name, node);
		return true;
	}
	
	public HashMap<String, V> getTable(int idx){
		return tables.get(idx);
	}
	
	public void clear(){
		tables.clear();
	}
	
	public int getNumScopes(){
		return tables.size();
	}
}
