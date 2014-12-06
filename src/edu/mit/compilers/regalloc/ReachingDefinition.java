package edu.mit.compilers.regalloc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.Statement;

public class ReachingDefinition {
	
	private HashMap<String, HashSet<Web>> webs = new HashMap<String, HashSet<Web>>();
	public ReachingDefinition() {}
	
	public ReachingDefinition(ReachingDefinition rd) {
		// copy everything but the Web.
		HashMap<String, HashSet<Web>> webMapToCopy = rd.getWebsMap();
		for (String key : webMapToCopy.keySet()) {
			HashSet<Web> setCopy = new HashSet<Web>(webMapToCopy.get(key));
			webs.put(key, setCopy);
		}
	}
	
	public boolean changed(ReachingDefinition other) {
		HashSet<Web> thisAllWebs = this.getAllWebs();
		HashSet<Web> otherAllWebs = other.getAllWebs();
		if (thisAllWebs.size() != otherAllWebs.size()) {
			return true;
		}
		System.out.println("This webs: " + thisAllWebs);
		System.out.println("Other webs: " + otherAllWebs);
		for (Web web : otherAllWebs) {
			System.out.println("Web: " + web.getVarName());
			if (!thisAllWebs.contains(web)) {
				System.out.println("Different!");
				return true;
			}
		}
		return false;
		
	}
	
	public Web mergeWebs(String varName) {
		Web newWeb = new Web(varName);
		System.out.println("Created web " + newWeb);
		HashSet<Statement> startingStatements = new HashSet<Statement>();
		for (Web web : webs.get(varName)) {
			for (Statement st : web.getStatements()) {
				newWeb.addStatement(st);
				st.setWeb(newWeb);
			}
			for (FlowNode node : web.getNodes()) {
				newWeb.addNode(node);
				node.setWeb(newWeb);
			}
			startingStatements.addAll(web.getStartingStatements());
		}
		newWeb.setStartingStatements(startingStatements);
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
	
	public void setStatements(Statement st) {
		for (Web web : getAllWebs()) {
			web.addStatement(st);
		}
	}
	
	public void setFlowNodes(FlowNode node) {
		for (Web web : getAllWebs()) {
			web.addNode(node);
		}
	}
	
	public void setWebs(String varName, HashSet<Web> newWebs) {
		System.out.println("I tried to set the web!!!");
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
			System.out.println("I'm removing a web!!!");
			webs.get(varName).remove(web);
		}
	}
	
	public void removeWeb(String varName) {
		HashSet<Web> curWebs = webs.get(varName);
		int count = 1;
		Iterator<Web> it = curWebs.iterator();
		while (it.hasNext()) {
			Web web = it.next();
			System.out.println("Removing web #" + count + ": " + web);
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
