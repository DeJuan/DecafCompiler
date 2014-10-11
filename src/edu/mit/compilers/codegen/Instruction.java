package edu.mit.compilers.codegen;

import java.util.ArrayList;

/**@brief an assembly instruction
 * 
 * @author desaic
 *
 */
public class Instruction {
	
	public String cmd;
	
	public ArrayList<LocationMem> args;
	
	/**@brief jump labels
	 */
	public String label;

	public static Instruction labelInstruction(String l){
		Instruction ii = new Instruction();
		ii.label = l;
		return ii;
	}
	
	public Instruction(){
		label="";
		cmd = null;
		args = null;	
	}

	public Instruction(String c){
		label="";
		cmd = c;
		args = new ArrayList<LocationMem> ();	
	}
	
	public Instruction(String c, LocationMem a1){
		label="";
		cmd = c;
		args = new ArrayList<LocationMem> ();
		args.add(a1);
	}
	
	public Instruction(String c, LocationMem a1, LocationMem a2){
		label="";
		cmd = c;
		args = new ArrayList<LocationMem> ();
		args.add(a1);
		args.add(a2);
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		if(label.length()>0){
			sb.append(label+":");
			if(cmd != null){
				sb.append("\n");
			}
		}
		if(cmd != null){
			sb.append("\t"+cmd+" ");
			for(int ii = 0; ii<args.size();ii++){
				sb.append(args.get(ii).toString());
				if(ii<args.size()-1){
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}
	
}
