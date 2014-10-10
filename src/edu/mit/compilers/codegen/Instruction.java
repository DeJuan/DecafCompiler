package edu.mit.compilers.codegen;

import java.util.ArrayList;

/**@brief an assembly instruction
 * 
 * @author desaic
 *
 */
public class Instruction {
	
	public String cmd;
	
	public ArrayList<MemLocation> args;
	
	/**@brief jump labels
	 */
	public String label;
	
	public Instruction(String c, MemLocation a1){
		label="";
		cmd = c;
		args = new ArrayList<MemLocation> ();
		args.add(a1);
	}
	
	public Instruction(String c, MemLocation a1, MemLocation a2){
		label="";
		cmd = c;
		args = new ArrayList<MemLocation> ();
		args.add(a1);
		args.add(a2);
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		if(label.length()>0){
			sb.append(label+":\n");
		}
		sb.append(cmd+" ");
		for(int ii = 0; ii<args.size();ii++){
			sb.append(args.get(ii).toString());
			if(ii<args.size()-1){
				sb.append(", ");
			}else{
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	
}
