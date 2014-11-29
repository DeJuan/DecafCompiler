package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import edu.mit.compilers.ir.Type;

public class SPTern {
    SPSet cond;
    SPSet trueBranch;
    SPSet falseBranch;
    Boolean containsMethodCalls;
    
    public SPTern(SPSet cond, SPSet trueBranch, SPSet falseBranch) {
        this.cond = cond;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }
    
    public SPTern(Ternary tern){
        switch(tern.getTernaryCondition().getExprType()){
        case BOOL_LIT:
            this.cond = new SPSet(Collections.<SPSet> emptyList(), Collections.<ValueID> emptyList(), Collections.<IntLit> emptyList(), 
                    Arrays.asList((BoolLit) tern.getTernaryCondition()), Collections.<SPTern> emptyList(),
                    Collections.<MethodCall> emptyList(), Collections.<SPComp> emptyList(), null);
            break;
        case COND_EXPR:
            this.cond = new SPSet(tern.getTernaryCondition());
            break;
        case COMP_EXPR:
            List<SPComp> conds = new ArrayList<SPComp>();
            conds.add(new SPComp((CompExpr) tern.getTernaryCondition()));
            this.cond = new SPSet(Collections.<SPSet> emptyList(), Collections.<ValueID> emptyList(), Collections.<IntLit> emptyList(), 
                    Collections.<BoolLit> emptyList(), Collections.<SPTern> emptyList(),
                    Collections.<MethodCall> emptyList(), conds, null);
            break;
        case VAR:
            Var varCond = (Var)tern.getTernaryCondition();
            if(varCond.getVarDescriptor().getType() != Type.BOOL){
                throw new UnsupportedOperationException("Tried to use a non-boolean expression as the condition for a ternary expression.");
            }
            this.cond = new SPSet(Collections.<SPSet> emptyList(), Arrays.asList(((Var) tern.getTernaryCondition()).getValueID()), 
                    Collections.<IntLit> emptyList(), Collections.<BoolLit> emptyList(), Collections.<SPTern> emptyList(),
                    Collections.<MethodCall> emptyList(), Collections.<SPComp> emptyList(), null);
            break;
        case TERNARY:
            this.cond = new SPSet(Collections.<SPSet> emptyList(), Collections.<ValueID> emptyList(), Collections.<IntLit> emptyList(), 
                    Collections.<BoolLit> emptyList(), Arrays.asList(new SPTern((Ternary) tern.getTernaryCondition())),
                    Collections.<MethodCall> emptyList(), Collections.<SPComp> emptyList(), null);
        default:
            throw new UnsupportedOperationException("Tried to use something that could never resolve to a truth value as the condition " +
            		                                "for a ternary expression.");
        }
        
        this.trueBranch = safeSPConstruct(tern.getTrueBranch());
        this.falseBranch = safeSPConstruct(tern.getFalseBranch());
    }
    
    public SPSet getTernaryCondition(){
        return cond;
    }
    
    public SPSet getTrueBranch(){
        return trueBranch;
    }
    
    public SPSet getFalseBranch(){
        return falseBranch;
    }
    
    public boolean containsMethodCalls(){
        if (containsMethodCalls == null) {
          containsMethodCalls = cond.containsMethodCalls() || trueBranch.containsMethodCalls() || falseBranch.containsMethodCalls();
        }
        return containsMethodCalls;
    }
    
    @Override
    public boolean equals(Object other){
        if (!(other instanceof SPTern)){
            return false;
        }
        SPTern otherTern = (SPTern) other;
        return (otherTern.getTernaryCondition().equals(cond) 
                && otherTern.getFalseBranch().equals(falseBranch) 
                && otherTern.getTrueBranch().equals(trueBranch));
    }
    
    @Override
    public int hashCode(){
        return cond.hashCode() + falseBranch.hashCode() + trueBranch.hashCode();
    }
    
    public Ternary toExpression(Map<ValueID, List<Var>> valToVar){
        return new Ternary(cond.toExpression(valToVar), trueBranch.toExpression(valToVar), falseBranch.toExpression(valToVar));
    }
    
    private SPSet safeSPConstruct(Expression expr) {
        if (expr instanceof Var) {
            return new SPSet(Collections.<SPSet> emptyList(), Arrays.asList(((Var) expr).getValueID()), Collections.<IntLit> emptyList(), 
                    Collections.<BoolLit> emptyList(), Collections.<SPTern> emptyList(),
                    Collections.<MethodCall> emptyList(), Collections.<SPComp> emptyList(), null);
        } else if (expr instanceof Ternary) {
            return new SPSet(Collections.<SPSet> emptyList(), Collections.<ValueID> emptyList(), Collections.<IntLit> emptyList(), 
                    Collections.<BoolLit> emptyList(), Arrays.asList(new SPTern((Ternary) expr)),
                    Collections.<MethodCall> emptyList(), Collections.<SPComp> emptyList(), null);
        } else if (expr instanceof CompExpr) {
            return new SPSet(Collections.<SPSet> emptyList(), Collections.<ValueID> emptyList(), Collections.<IntLit> emptyList(), 
                    Collections.<BoolLit> emptyList(), Collections.<SPTern> emptyList(),
                    Collections.<MethodCall> emptyList(), Arrays.asList(new SPComp((CompExpr) expr)), null);
        } else if (expr instanceof IntLit) {
            return new SPSet(Collections.<SPSet> emptyList(), Collections.<ValueID> emptyList(), Arrays.asList((IntLit) expr), 
                    Collections.<BoolLit> emptyList(), Collections.<SPTern> emptyList(),
                    Collections.<MethodCall> emptyList(), Collections.<SPComp> emptyList(), null);
        } else if (expr instanceof BoolLit) {
            return new SPSet(Collections.<SPSet> emptyList(), Collections.<ValueID> emptyList(), Collections.<IntLit> emptyList(), 
                    Arrays.asList((BoolLit) expr), Collections.<SPTern> emptyList(),
                    Collections.<MethodCall> emptyList(), Collections.<SPComp> emptyList(), null);
        }
        return new SPSet(expr);
    }
}
