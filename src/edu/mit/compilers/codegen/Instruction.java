package edu.mit.compilers.codegen;

import java.util.ArrayList;

/**@brief an assembly instruction
 * 
 * @author desaic
 *
 */
public class Instruction {
	
	public String cmd;
	
	public ArrayList<String> args;
	
	/**@brief jump labels
	 */
	public String label;
	public Instruction(String c, String a1){
		cmd = c;
		args = new ArrayList<String> ();
		args.add(a1);
	}
	public Instruction(String c, String a1, String a2){
		cmd = c;
		args = new ArrayList<String> ();
		args.add(a1);
		args.add(a2);
	}
}
