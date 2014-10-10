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
		cmd = c;
		args = new ArrayList<MemLocation> ();
		args.add(a1);
	}
	public Instruction(String c, MemLocation a1, MemLocation a2){
		cmd = c;
		args = new ArrayList<MemLocation> ();
		args.add(a1);
		args.add(a2);
	}
}
