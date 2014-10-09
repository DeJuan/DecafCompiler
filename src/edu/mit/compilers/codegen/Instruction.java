package edu.mit.compilers.codegen;

import java.util.ArrayList;

public class Instruction {
	public String cmd;
	public ArrayList<String> args;
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
