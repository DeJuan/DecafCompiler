package edu.mit.compilers.controlflow;

import java.util.Arrays;
import java.util.Collections;

import edu.mit.compilers.ir.Ops;

public class SPComp {
    private Ops operator;
    private SPSet lhs;
    private SPSet rhs;
    
    public SPComp(CompExpr expr) {
        this.operator = expr.getOperator();
        lhs = safeSPConstruct(expr.getLeftSide());
        rhs = safeSPConstruct(expr.getRightSide());
    }
    
    public SPComp(Ops operator, SPSet lhs, SPSet rhs) {
        this.operator = operator;
        this.lhs = lhs;
        this.rhs = rhs;
    }
    
    public Ops getOperator(){
        return operator;
    }
    
    public SPSet getLhs(){
        return lhs;
    }
    
    public SPSet getRhs() {
        return rhs;
    }
    
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SPComp)) {
            return false;
        }
        SPComp otherComp = (SPComp) other;
        return operator == otherComp.operator && lhs.equals(otherComp.lhs) && rhs.equals(otherComp.rhs);
    }
    
    @Override
    public int hashCode(){
        return operator.hashCode() + lhs.hashCode() + rhs.hashCode();
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
