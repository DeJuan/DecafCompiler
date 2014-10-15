package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.IR_CondOp;
import edu.mit.compilers.ir.IR_Node;
import edu.mit.compilers.ir.IR_Not;
import edu.mit.compilers.ir.Ops;

public abstract class CFGNode {
	abstract public List<Instruction> codegen (CodegenContext context);
	public String jumpLabel;
	public String getLabel(){
		return jumpLabel;
	}
	public void setLabel(String l){
		jumpLabel = l;
	}
	/**@brief wrapper for IR*/
	public static class CFGLabel extends CFGNode{
		
		public CFGLabel(String label){
			jumpLabel = label;
		}		
		@Override
		public List<Instruction> codegen(CodegenContext context) {
			// TODO Auto-generated method stub
			ArrayList<Instruction> ins = new ArrayList<Instruction>();
			return ins;
		}
	}
	
	public static class CFGBranch extends CFGNode{
		IR_Node c;
		CFGNode t,f;
		public CFGBranch(IR_Node cond, CFGNode trueBlock, CFGNode falseBlock){
			c = cond;
			t = trueBlock;
			f = falseBlock;
			jumpLabel=null;
		}

		@Override
		public List<Instruction> codegen(CodegenContext context) {
			// TODO Auto-generated method stub
			ArrayList<Instruction> ins = new ArrayList<Instruction>();
			if(jumpLabel != null){
				ins.add(Instruction.labelInstruction(jumpLabel));				
			}
			ins.addAll(Codegen.generateExpr(c, context));
			LocReg r10 = new LocReg(Regs.R10);
			LocLiteral zero = new LocLiteral(0);
			ins.addAll(context.pop(r10));
			
			if(t.getLabel() == null){
				t.setLabel(context.genLabel());
			}

			if(f.getLabel() == null){
				f.setLabel(context.genLabel());
			}			
			LocLabel tlabel = new LocLabel(t.getLabel());
			LocLabel flabel = new LocLabel(f.getLabel());
			
			ins.add(new Instruction("cmpq", zero, r10));
			ins.add(new Instruction("je", flabel));
			ins.add(new Instruction("jmp", tlabel));
			ins.addAll(f.codegen(context));
			ins.addAll(t.codegen(context));
			return ins;
		}
	}

	public static CFGNode shortCircuit(IR_Node c, CFGNode t, CFGNode f){
		if(c instanceof IR_CondOp){
			IR_CondOp cond = (IR_CondOp)c;
			IR_Node c1 = cond.getLeft();
			IR_Node c2 = cond.getRight();
			if(cond.getOp() == Ops.AND){
				CFGNode b2 = shortCircuit(c2,  t, f);
				CFGNode b1 = shortCircuit(c1, b2, f);
				return b1;
			}else if(cond.getOp()==Ops.OR){
				CFGNode b2 = shortCircuit(c2,  t, f);
				CFGNode b1 = shortCircuit(c1,  t, b2);
				return b1;
			}
			return null;
		}else if(c instanceof IR_Not){
			IR_Node c1 = ((IR_Not) c).getExpr();
			return shortCircuit(c1,f,t);
		}else{
			return new CFGBranch(c, t, f);
		}
	}
}
