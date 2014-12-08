package edu.mit.compilers.regalloc;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.codegen.Instruction;
import edu.mit.compilers.codegen.LocLabel;
import edu.mit.compilers.codegen.LocLiteral;
import edu.mit.compilers.codegen.LocReg;
import edu.mit.compilers.codegen.Regs;
import edu.mit.compilers.controlflow.CondExpr;
import edu.mit.compilers.controlflow.ControlflowContext;
import edu.mit.compilers.controlflow.Expression;
import edu.mit.compilers.controlflow.NotExpr;
import edu.mit.compilers.ir.Ops;

public abstract class ShortCircuitNodeReg {
    abstract public List<Instruction> codegen (ControlflowContext context);
    public String jumpLabel;
    public boolean generated=false;
    public String getLabel(){
        return jumpLabel;
    }
    public void setLabel(String l){
        jumpLabel = l;
    }

    public static class SCLabel extends ShortCircuitNodeReg{
        
        public SCLabel(String label){
            jumpLabel = label;
        }       
        @Override
        public List<Instruction> codegen(ControlflowContext context) {
            ArrayList<Instruction> ins = new ArrayList<Instruction>();
            generated = true;
            return ins;
        }
    }
    
    public static class SCBranch extends ShortCircuitNodeReg{
        Expression c;
        ShortCircuitNodeReg t,f;
        public SCBranch(Expression cond, ShortCircuitNodeReg trueBlock, ShortCircuitNodeReg falseBlock){
            c = cond;
            t = trueBlock;
            f = falseBlock;
            jumpLabel=null;
        }

        @Override
        public List<Instruction> codegen(ControlflowContext context) {
            generated = true;
            ArrayList<Instruction> ins = new ArrayList<Instruction>();
            if(jumpLabel != null){
                ins.add(Instruction.labelInstruction(jumpLabel));               
            }
            ins.addAll(AssignRegisters.generateExpression(c, context));
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
            if(!f.generated){
                ins.addAll(f.codegen(context));
            }
            if(!t.generated){
                ins.addAll(t.codegen(context));
            }
            return ins;
        }
    }
    
    public static class CFGExprBranch extends ShortCircuitNodeReg{
        Expression c;
        ShortCircuitNodeReg t,f;
        public CFGExprBranch(Expression cond, ShortCircuitNodeReg trueBlock, ShortCircuitNodeReg falseBlock){
            c = cond;
            t = trueBlock;
            f = falseBlock;
            jumpLabel=null;
        }

        
        public List<Instruction> codegen(ControlflowContext context) {
            generated = true;
            ArrayList<Instruction> ins = new ArrayList<Instruction>();
            if(jumpLabel != null){
                ins.add(Instruction.labelInstruction(jumpLabel));               
            }
            ins.addAll(AssignRegisters.generateExpression(c, context));
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
            if(!f.generated){
                ins.addAll(f.codegen(context));
            }
            if(!t.generated){
                ins.addAll(t.codegen(context));
            }
            return ins;
        }


        
    }

    public static ShortCircuitNodeReg shortCircuit(Expression expr, ShortCircuitNodeReg t, ShortCircuitNodeReg f) {
        if(expr instanceof CondExpr){
            CondExpr cond = (CondExpr) expr;
            Expression c1 = cond.getLeftSide();
            Expression c2 = cond.getRightSide();
            if(cond.getOperator() == Ops.AND){
            	ShortCircuitNodeReg b2 = shortCircuit(c2,  t, f);
            	ShortCircuitNodeReg b1 = shortCircuit(c1, b2, f);
                return b1;
            }else if(cond.getOperator()==Ops.OR){
            	ShortCircuitNodeReg b2 = shortCircuit(c2,  t, f);
            	ShortCircuitNodeReg b1 = shortCircuit(c1,  t, b2);
                return b1;
            }
            return null;
        }else if(expr instanceof NotExpr){
            Expression c1 = ((NotExpr) expr).getUnresolvedExpression();
            return shortCircuit(c1,f,t);
        }else{
            return new SCBranch(expr, t, f);
        }
    }
}