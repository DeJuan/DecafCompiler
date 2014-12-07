package edu.mit.compilers.regalloc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.mit.compilers.controlflow.FlowNode;
import edu.mit.compilers.controlflow.Statement;
import edu.mit.compilers.ir.IR_FieldDecl;

public class ReachingDefinition {
	
	private HashMap<IR_FieldDecl, HashSet<Web>> webs = new HashMap<IR_FieldDecl, HashSet<Web>>();
	public ReachingDefinition() {}
	
	public ReachingDefinition(ReachingDefinition rd) {
		// copy everything but the Web.
		HashMap<IR_FieldDecl, HashSet<Web>> webMapToCopy = rd.getWebsMap();
		for (IR_FieldDecl key : webMapToCopy.keySet()) {
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
			System.out.println("Web: " + web.getFieldDecl().getName());
			if (!thisAllWebs.contains(web)) {
				System.out.println("Different!");
				return true;
			}
		}
		return false;
		
	}
	
	public Web mergeWebs(IR_FieldDecl decl) {
		Web newWeb = new Web(decl);
		System.out.println("Created web " + newWeb);
		HashSet<Statement> startingStatements = new HashSet<Statement>();
		for (Web web : webs.get(decl)) {
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
		for (IR_FieldDecl decl : webs.keySet()) {
			Set<Web> webSet = webs.get(decl);
			if (webSet.size() > 1) {
				System.out.println("Merging webs for var: " + decl.getName());
				Web newWeb = mergeWebs(decl);
				setWebs(decl, new HashSet<Web>(Arrays.asList(newWeb)));
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
	
	public void setWebs(IR_FieldDecl decl, HashSet<Web> newWebs) {
		System.out.println("Setting web for var: " + decl.getName());
		webs.put(decl, newWebs);
	}
	
	public void addWeb(Web web) {
		IR_FieldDecl decl = web.getFieldDecl();
		HashSet<Web> webSet;
		if (webs.containsKey(decl)) {
			webSet = webs.get(decl);
			webSet.add(web);
		} else {
			webSet = new HashSet<Web>();
			webSet.add(web);
			webs.put(decl, webSet);
		}
	}
	
	public void removeWeb(Web web) {
		IR_FieldDecl decl = web.getFieldDecl();
		if (webs.containsKey(decl)) {
			System.out.println("I'm removing a web!!!");
			webs.get(decl).remove(web);
		}
	}
	
	public void removeWeb(IR_FieldDecl decl) {
		HashSet<Web> curWebs = webs.get(decl);
		int count = 1;
		Iterator<Web> it = curWebs.iterator();
		while (it.hasNext()) {
			Web web = it.next();
			System.out.println("Removing web #" + count + ": " + web);
			it.remove();
			count++;
		}
	}
	
	public HashMap<IR_FieldDecl, HashSet<Web>> getWebsMap() {
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
			sb.append(web.getFieldDecl().getName() + " ");
		}
		return sb.toString();
	}

}
