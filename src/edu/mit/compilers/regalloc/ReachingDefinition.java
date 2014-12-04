package edu.mit.compilers.regalloc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.compilers.controlflow.Statement;

public class ReachingDefinition {
	
	private HashMap<String, HashSet<Web>> webs = new HashMap<String, HashSet<Web>>();
	public ReachingDefinition() {}
	
	public ReachingDefinition(ReachingDefinition rd) {
		webs = new HashMap<String, HashSet<Web>>(rd.getWebsMap());
	}
	
	public boolean changed(ReachingDefinition other) {
		HashSet<Web> thisAllWebs = this.getAllWebs();
		HashSet<Web> otherAllWebs = other.getAllWebs();
		return thisAllWebs.equals(otherAllWebs);
	}
	
	public Web mergeWebs(String varName) {
		Web newWeb = new Web(varName);
		for (Web web : webs.get(varName)) {
			for (Statement st : web.getStatements()) {
				newWeb.addStatement(st);
				st.setWeb(newWeb);
			}
		}
		return newWeb;
	}
	
	/**
	 * Merge two webs if they refer to the same variable name.
	 * @return
	 */
	public boolean merge() {
		boolean didMerge = false;
		for (String varName : webs.keySet()) {
			Set<Web> webSet = webs.get(varName);
			if (webSet.size() > 1) {
				Web newWeb = mergeWebs(varName);
				setWebs(varName, new HashSet<Web>(Arrays.asList(newWeb)));
				didMerge = true;
			}
		}
		return didMerge;
	}
	
	public void setWebs(String varName, HashSet<Web> newWebs) {
		webs.put(varName, newWebs);
	}
	
	public void addWeb(Web web) {
		String varName = web.getVarName();
		HashSet<Web> webSet;
		if (webs.containsKey(varName)) {
			webSet = webs.get(varName);
			webSet.add(web);
		} else {
			webSet = new HashSet<Web>();
			webSet.add(web);
			webs.put(varName, webSet);
		}
	}
	
	public void removeWeb(Web web) {
		String varName = web.getVarName();
		if (webs.containsKey(varName)) {
			webs.get(varName).remove(web);
		}
	}
	
	public void removeWeb(String varName) {
		HashSet<Web> curWebs = webs.get(varName);
		int count = 1;
		Iterator<Web> it = curWebs.iterator();
		while (it.hasNext()) {
			it.next();
			System.out.println("Removing web #" + count);
			it.remove();
			count++;
		}
	}
	
	public HashMap<String, HashSet<Web>> getWebsMap() {
		return this.webs;
	}
	
	public HashSet<Web> getAllWebs() {
		HashSet<Web> allWebs = new HashSet<Web>();
		for (HashSet<Web> websPerVar : webs.values()) {
			allWebs.addAll(websPerVar);
		}
		return allWebs;
	}
	
	@Override
	public String toString() {
		System.out.println("Size of webs: " + getAllWebs().size());
		StringBuilder sb = new StringBuilder();
		for (Web web : getAllWebs()) {
			sb.append(web.getVarName() + " ");
		}
		return sb.toString();
	}

}
