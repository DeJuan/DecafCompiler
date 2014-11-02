package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ir.Ops;

public class SPSet {
    List<SPSet> SPSets;
    List<ValueID> varSet;
    List<Long> intSet;
    List<Boolean> boolSet;
    List<SPTern> ternSet;
    List<SPComp> comparisons;
    List<MethodCall> methodCalls; 
    public Ops operator;

    public SPSet(Ops op){
        SPSets =  new ArrayList<SPSet>();
        varSet = new ArrayList<ValueID>();
        intSet = new ArrayList<Long>();
        boolSet = new ArrayList<Boolean>();
        ternSet = new ArrayList<SPTern>();
        methodCalls = new ArrayList<MethodCall>();
        comparisons = new ArrayList<SPComp>();
        operator = op;

    }

    public SPSet(List<SPSet> initialSPSet, List<ValueID> initialVarSet, 
            List<IntLit> intSet, List<BoolLit> boolSet, List<SPTern> initialTerns, 
            List<MethodCall> initialMethods, List<SPComp> initialComps, Ops op){
        operator = op;
        SPSets =  new ArrayList<SPSet>();
        varSet = new ArrayList<ValueID>();
        this.intSet = new ArrayList<Long>();
        this.boolSet = new ArrayList<Boolean>();
        ternSet = new ArrayList<SPTern>();
        methodCalls = new ArrayList<MethodCall>();
        comparisons = new ArrayList<SPComp>();
        for (SPSet curSet : initialSPSet) {
            SPSets.add(curSet);
        }
        for (ValueID var : initialVarSet) {
            varSet.add(var);
        }
        for (IntLit lit : intSet) {
            this.intSet.add(lit.getValue());
        }
        for (BoolLit bool : boolSet) {
            this.boolSet.add(bool.getTruthValue());
        }
        for (SPTern tern: initialTerns) {
            ternSet.add(tern);
        }
        for (MethodCall method: initialMethods) {
            methodCalls.add(method);
        }
        for (SPComp comp : initialComps) {
            comparisons.add(comp);
        }
    }

    public SPSet(Expression expr){
        //INT_LIT, VAR, BOOL_LIT, STRING_LIT, BIN_EXPR, METHOD_CALL, NOT,
        //NEGATE, TERNARY, ADD_EXPR, COMP_EXPR, COND_EXPR, EQ_EXPR, MULT_EXPR,
        //DIV_EXPR, MOD_EXPR;SPSets =  new ArrayList<SPSet>();
        varSet = new ArrayList<ValueID>();
        intSet = new ArrayList<Long>();
        boolSet = new ArrayList<Boolean>();
        ternSet = new ArrayList<SPTern>();
        methodCalls = new ArrayList<MethodCall>();
        comparisons = new ArrayList<SPComp>();
        if (expr instanceof NotExpr) {
            operator = Ops.NOT;
            NotExpr not = (NotExpr) expr;
            Expression inner = not.getUnresolvedExpression();
            if (inner instanceof Var) {
                Var innerVar = (Var) inner;
                if (innerVar.getValueID() == null) {
                    throw new RuntimeException("valueID must be set in advance");
                }
                varSet.add(innerVar.getValueID());
            } else if (inner instanceof IntLit) {
                intSet.add(((IntLit) inner).getValue());
            } else if (inner instanceof BoolLit) {
                boolSet.add(((BoolLit) inner).getTruthValue());
            } else if (inner instanceof Ternary) {
                ternSet.add(new SPTern((Ternary) inner));
            } else if (inner instanceof MethodCall) {
                methodCalls.add((MethodCall) inner);
            } else {
                SPSets.add(new SPSet(inner));
            }
        } else if (expr instanceof NegateExpr) {
            operator = Ops.NEGATE;
            NegateExpr neg = (NegateExpr) expr;
            Expression inner = neg.getExpression();
            if (inner instanceof Var) {
                Var innerVar = (Var) inner;
                if (innerVar.getValueID() == null) {
                    throw new RuntimeException("valueID must be set in advance");
                }
                varSet.add(innerVar.getValueID());
            } else if (inner instanceof IntLit) {
                intSet.add(((IntLit) inner).getValue());
            } else if (inner instanceof BoolLit) {
                boolSet.add(((BoolLit) inner).getTruthValue());
            } else if (inner instanceof Ternary) {
                ternSet.add(new SPTern((Ternary) inner));
            } else if (inner instanceof MethodCall) {
                methodCalls.add((MethodCall) inner);
            } else {
                SPSets.add(new SPSet(inner));
            }
        } else if (expr instanceof BinExpr) {
            BinExpr binEx = (BinExpr) expr;
            operator = binEx.getOperator();
            Expression lhs = binEx.getLeftSide();
            Expression rhs = binEx.getRightSide();
            if (binEx instanceof CompExpr) {
                comparisons.add(new SPComp((CompExpr) binEx));
            } else {
                if (lhs instanceof Var) {
                    varSet.add(((Var) lhs).getValueID());
                } else if (lhs instanceof Ternary) {
                    ternSet.add(new SPTern((Ternary) lhs));
                } else if (lhs instanceof MethodCall) {
                    methodCalls.add((MethodCall) lhs);
                } else {
                    SPSets.add(new SPSet(lhs));
                }
                if (rhs instanceof Var) {
                    Var varRHS = (Var) rhs;
                    varSet.add(varRHS.getValueID());
                } else if (rhs instanceof Ternary) {
                    Ternary ternRHS = (Ternary) rhs;
                    SPTern tern = new SPTern(ternRHS);
                    ternSet.add(tern);
                } else if (rhs instanceof MethodCall) {
                    methodCalls.add((MethodCall) rhs);
                } else {
                    SPSet SPSetRHS = new SPSet(rhs);
                    SPSets.add(SPSetRHS);
                }
            }
        } else {
            throw new UnsupportedOperationException("Tried to initialize an SPSet with an invalid operator.");
        }
    }

    public SPSet copy(SPSet original) {
        List<SPSet> SPCopy = new ArrayList<SPSet>();
        for (SPSet set : original.SPSets) {
            SPCopy.add(copy(set));
        }
        List<ValueID> varCopy = new ArrayList<ValueID>(original.varSet);
        List<IntLit> intCopy = new ArrayList<IntLit>();
        for (Long val : original.intSet) {
            intCopy.add(new IntLit(val));
        }
        List<BoolLit> boolCopy = new ArrayList<BoolLit>();
        for (Boolean val : original.boolSet) {
            boolCopy.add(new BoolLit(val));
        }
        List<SPTern> ternCopy = new ArrayList<SPTern>();
        for (SPTern tern : original.ternSet) {
            ternCopy.add(new SPTern(copy(tern.cond), copy(tern.trueBranch), copy(tern.falseBranch)));
        }
        List<MethodCall> methodCopy = new ArrayList<MethodCall>(original.methodCalls);
        List<SPComp> compCopy = new ArrayList<SPComp>();
        for (SPComp comp : original.comparisons) {
            compCopy.add(new SPComp(comp.getOperator(), copy(comp.getLhs()), copy(comp.getRhs())));
        }
        return new SPSet(SPCopy, varCopy, intCopy, boolCopy, ternCopy, methodCopy, compCopy, original.operator);
    }

    public void addToSPSets(SPSet newSP){
        if(operator == Ops.NOT || operator == Ops.NEGATE){
            if(!(SPSets.isEmpty() && varSet.isEmpty() && intSet.isEmpty() && boolSet.isEmpty())){
                throw new UnsupportedOperationException("Tried to add to an SPSet when it wasn't empty for a NOT.");
            }
        } 
        SPSets.add(newSP);
    }

    public void addToVarSet(ValueID newVar){
        if(operator == Ops.NOT || operator == Ops.NEGATE){
            if(!(SPSets.isEmpty() && varSet.isEmpty() && intSet.isEmpty() && boolSet.isEmpty())){
                throw new UnsupportedOperationException("Tried to add to an VarSet when it wasn't empty for a NOT.");
            }

        }
        varSet.add(newVar);
    }

    public void addToIntSet(IntLit newInt){
        if(operator == Ops.NOT || operator == Ops.NEGATE){
            if(!(SPSets.isEmpty() && varSet.isEmpty() && intSet.isEmpty() && boolSet.isEmpty())){
                throw new UnsupportedOperationException("Tried to add to an intSet when it wasn't empty for a NOT.");
            }

        }
        intSet.add(newInt.getValue());
    }

    public void addToBoolSet(BoolLit newBool){
        if(operator == Ops.NOT || operator == Ops.NEGATE){
            if(!(SPSets.isEmpty() && varSet.isEmpty() && intSet.isEmpty() && boolSet.isEmpty())){
                throw new UnsupportedOperationException("Tried to add to an boolSet when it wasn't empty for a NOT.");
            }
        }
        boolSet.add(newBool.getTruthValue());

    }

    // *(A,B) + (stuff) 
    //Looking at Mult(A,B)
    //Check whether any SPSets contain complete expressions within themselves.
    //Check each SPSet for a matching operator, and if one is found, then check the equivalence for the lhs and rhs.
    public boolean contains(Expression expr){
        if (expr instanceof CompExpr) {
            // Handle the case where the expression is not commutative
            SPComp target = new SPComp((CompExpr) expr);
            if (comparisons.contains(target)) {
                return true;
            }
            for (SPSet curSet : SPSets) {
                if (curSet.contains(expr)) {
                    return true;
                }
            }
            for (SPTern tern : ternSet) {
                if (tern.cond.contains(expr) || tern.falseBranch.contains(expr) || tern.trueBranch.contains(expr)){
                    return true;
                }
            }
            for (SPComp comp : comparisons) {
                if (comp.getLhs().contains(expr) || comp.getRhs().contains(expr)) {
                    return true;
                }
            }
            return false;
        } else {
            // Expression is commutative
            if(expr instanceof Var){
                Var varia = (Var)expr;
                return varSet.contains(varia.getValueID());
            } else if (expr instanceof IntLit) {
                return intSet.contains(((IntLit) expr).getValue());
            } else if (expr instanceof BoolLit) {
                return boolSet.contains(((BoolLit) expr).getTruthValue());
            }

            else if(expr instanceof BinExpr){
                BinExpr binex = (BinExpr)expr;
                Ops searchOp = binex.getOperator();
                if (operator == searchOp) {
                    boolean hasLeftSide = false;
                    if (binex.getLeftSide() instanceof Var) {
                        hasLeftSide = varSet.contains(((Var) binex.getLeftSide()).getValueID());
                    } else if (binex.getLeftSide() instanceof Ternary) {
                        hasLeftSide = ternSet.contains(new SPTern((Ternary) binex.getLeftSide()));
                    } else if (binex.getLeftSide() instanceof CompExpr) {
                        hasLeftSide = comparisons.contains(new SPComp((CompExpr) binex.getLeftSide()));
                    } else {
                        hasLeftSide = SPSets.contains(new SPSet(binex.getLeftSide()));
                    }
                    if (hasLeftSide) {
                        SPSet copy = copy(this);
                        copy.remove(binex.getLeftSide());
                        boolean hasRightSide;
                        if (binex.getRightSide() instanceof Var) {
                            hasRightSide = varSet.contains(((Var) binex.getRightSide()).getValueID());
                        } else if (binex.getRightSide() instanceof Ternary) {
                            hasRightSide = ternSet.contains(new SPTern((Ternary) binex.getRightSide()));
                        } else if (binex.getRightSide() instanceof CompExpr) {
                            hasRightSide = comparisons.contains(new SPComp((CompExpr) binex.getRightSide()));
                        } else {
                            hasRightSide = SPSets.contains(new SPSet(binex.getRightSide()));
                        }
                        if (hasRightSide) {
                            return true;
                        }
                    }
                }
                for (SPSet currentSP : SPSets){
                    if (currentSP.operator == searchOp){
                        if (currentSP.contains(binex.getLeftSide())) {
                            SPSet copy = copy(currentSP);
                            copy.remove(binex.getLeftSide());
                            if (copy.contains(binex.getRightSide())) {
                                return true;
                            }
                        }
                    }
                }
                
                for (SPTern tern : ternSet) {
                    if (tern.cond.contains(expr) || tern.falseBranch.contains(expr) || tern.trueBranch.contains(expr)){
                        return true;
                    }
                }
                for (SPComp comp : comparisons) {
                    if (comp.getLhs().contains(expr) || comp.getRhs().contains(expr)) {
                        return true;
                    }
                }
                return false;
            }
            else if(expr instanceof NotExpr || expr instanceof NegateExpr){
                if ((operator == Ops.NOT && expr instanceof NotExpr) || (operator == Ops.NEGATE && expr instanceof NegateExpr)) {
                    Expression inner;
                    if (expr instanceof NotExpr) {
                        inner = ((NotExpr) expr).getUnresolvedExpression();
                    } else {
                        inner = ((NegateExpr) expr).getExpression();
                    }
                    boolean found;
                    if (inner instanceof Var) {
                        found = varSet.contains(((Var) inner).getValueID());
                    } else if (inner instanceof Ternary) {
                        found = ternSet.contains(new SPTern((Ternary) inner));
                    } else if (inner instanceof CompExpr) {
                        found = comparisons.contains(new SPComp((CompExpr) inner));
                    } else {
                        found = SPSets.contains(new SPSet(inner));
                    }
                    if (found) {
                        return true;
                    }
                    
                }
                if (SPSets.contains(new SPSet(expr))) {
                    return true;
                }
                for (SPSet curSet : SPSets) {
                    if (curSet.contains(expr)) {
                        return true;
                    }
                }
                for (SPTern tern : ternSet) {
                    if (tern.cond.contains(expr) || tern.falseBranch.contains(expr) || tern.trueBranch.contains(expr)){
                        return true;
                    }
                }
                for (SPComp comp : comparisons) {
                    if (comp.getLhs().contains(expr) || comp.getRhs().contains(expr)) {
                        return true;
                    }
                }
                return false;

            }

            else if(expr instanceof Ternary){
                if (ternSet.contains(new SPTern((Ternary) expr))) {
                    return true;
                }
                for (SPSet curSet : SPSets) {
                    if (curSet.contains(expr)) {
                        return true;
                    }
                }
                for (SPTern tern : ternSet) {
                    if (tern.cond.contains(expr) || tern.falseBranch.contains(expr) || tern.trueBranch.contains(expr)){
                        return true;
                    }
                }
                for (SPComp comp : comparisons) {
                    if (comp.getLhs().contains(expr) || comp.getRhs().contains(expr)) {
                        return true;
                    }
                }
                return false;
            } else if (expr instanceof MethodCall) {
                return false;
            }
            throw new RuntimeException("Should never reach here");
        }
    }

    private void remove(Expression expr) {
        if (!contains(expr)) {
            throw new RuntimeException("Can't remove what isn't there");
        }
        if (expr instanceof Var) {
            varSet.remove(((Var) expr).getValueID());
        } else if (expr instanceof IntLit) {
            intSet.remove(((IntLit) expr).getValue());
        } else if (expr instanceof BoolLit) {
            boolSet.remove(((BoolLit) expr).getTruthValue());
        } else if (expr instanceof Ternary) {
            ternSet.remove(new SPTern((Ternary) expr));
        } else if (expr instanceof CompExpr) {
            if (comparisons.contains(new SPComp((CompExpr) expr))) {
                comparisons.remove(new SPComp((CompExpr) expr));
            } else {
                boolean found = false;
                for (SPSet searching : SPSets) {
                    if (searching.contains(expr)) {
                        searching.remove(expr);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    for (SPTern tern : ternSet) {
                        if (tern.cond.contains(expr)) {
                            found = true;
                            tern.cond.remove(expr);
                            break;
                        } else if (tern.trueBranch.contains(expr)) {
                            found = true;
                            tern.trueBranch.remove(expr);
                            break;
                        } else if (tern.falseBranch.contains(expr)) {
                            found = true;
                            tern.falseBranch.remove(expr);
                            break;
                        }
                    }
                }
                if (!found) {
                    for (SPComp comp : comparisons) {
                        if (comp.getLhs().contains(expr)) {
                            comp.getLhs().remove(expr);
                            found = true;
                            break;
                        } else if (comp.getRhs().contains(expr)) {
                            comp.getRhs().remove(expr);
                            found = true;
                            break;
                        }
                    }
                }
            }
        } else if (expr instanceof BinExpr) {
            BinExpr binex = (BinExpr)expr;
            Ops searchOp = binex.getOperator();
            boolean found = false;
            if (operator == searchOp) {
                int leftLoc;
                boolean hasLeftSide = false;
                if (binex.getLeftSide() instanceof Var) {
                    hasLeftSide = varSet.contains(((Var) binex.getLeftSide()).getValueID());
                    leftLoc = 1;
                } else if (binex.getLeftSide() instanceof Ternary) {
                    hasLeftSide = ternSet.contains(new SPTern((Ternary) binex.getLeftSide()));
                    leftLoc = 2;
                } else if (binex.getLeftSide() instanceof CompExpr) {
                    hasLeftSide = comparisons.contains(new SPComp((CompExpr) binex.getLeftSide()));
                    leftLoc = 3;
                } else {
                    hasLeftSide = SPSets.contains(new SPSet(binex.getLeftSide()));
                    leftLoc = 4;
                }
                if (hasLeftSide) {
                    SPSet copy = copy(this);
                    copy.remove(binex.getLeftSide());
                    int rightLoc;
                    boolean hasRightSide;
                    if (binex.getRightSide() instanceof Var) {
                        hasRightSide = varSet.contains(((Var) binex.getRightSide()).getValueID());
                        rightLoc = 1;
                    } else if (binex.getRightSide() instanceof Ternary) {
                        hasRightSide = ternSet.contains(new SPTern((Ternary) binex.getRightSide()));
                        rightLoc = 2;
                    } else if (binex.getRightSide() instanceof CompExpr) {
                        hasRightSide = comparisons.contains(new SPComp((CompExpr) binex.getRightSide()));
                        rightLoc = 3;
                    } else {
                        hasRightSide = SPSets.contains(new SPSet(binex.getRightSide()));
                        rightLoc = 4;
                    }
                    if (hasRightSide) {
                        found = true;
                        switch(leftLoc) {
                        case 1:
                            varSet.remove(((Var) binex.getLeftSide()).getValueID());
                            break;
                        case 2:
                            ternSet.remove(new SPTern((Ternary) binex.getLeftSide()));
                            break;
                        case 3:
                            comparisons.remove(new SPComp((CompExpr) binex.getLeftSide()));
                            break;
                        case 4:
                            SPSets.remove(new SPSet(binex.getLeftSide()));
                            break;
                        default:
                            throw new RuntimeException("this line should be unreachable");
                        }
                        switch(rightLoc) {
                        case 1:
                            varSet.remove(((Var) binex.getRightSide()).getValueID());
                            break;
                        case 2:
                            ternSet.remove(new SPTern((Ternary) binex.getRightSide()));
                            break;
                        case 3:
                            comparisons.remove(new SPComp((CompExpr) binex.getRightSide()));
                            break;
                        case 4:
                            SPSets.remove(new SPSet(binex.getRightSide()));
                            break;
                        default:
                            throw new RuntimeException("this line should be unreachable");
                        }
                    }
                }
            }
            if (!found) {
                for (SPSet currentSP : SPSets){
                    if (currentSP.operator == searchOp){
                        if (currentSP.contains(binex.getLeftSide())) {
                            SPSet copy = copy(currentSP);
                            copy.remove(binex.getLeftSide());
                            if (copy.contains(binex.getRightSide())) {
                                currentSP.remove(binex.getLeftSide());
                                currentSP.remove(binex.getLeftSide());
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!found) {
                for (SPTern tern : ternSet) {
                    if (tern.cond.contains(expr)) {
                        found = true;
                        tern.cond.remove(expr);
                        break;
                    } else if (tern.falseBranch.contains(expr)) {
                        found = true;
                        tern.falseBranch.remove(expr);
                        break;
                    } else if (tern.trueBranch.contains(expr)){
                        found = true;
                        tern.trueBranch.remove(expr);
                        break;
                    }

                }
            }

            if (!found) {
                for (SPComp comp : comparisons) {
                    if (comp.getLhs().contains(expr)) {
                        found = true;
                        comp.getLhs().remove(expr);
                        break;
                    } else if (comp.getRhs().contains(expr)) {
                        found = true;
                        comp.getRhs().remove(expr);
                        break;
                    }
                }
            }
        } else if(expr instanceof NotExpr || expr instanceof NegateExpr){
            if ((operator == Ops.NOT && expr instanceof NotExpr) || (operator == Ops.NEGATE && expr instanceof NegateExpr)) {
                Expression inner;
                if (expr instanceof NotExpr) {
                    inner = ((NotExpr) expr).getUnresolvedExpression();
                } else {
                    inner = ((NegateExpr) expr).getExpression();
                }
                boolean found;
                int loc;
                if (inner instanceof Var) {
                    found = varSet.contains(((Var) inner).getValueID());
                    loc = 1;
                } else if (inner instanceof Ternary) {
                    found = ternSet.contains(new SPTern((Ternary) inner));
                    loc = 2;
                } else if (inner instanceof CompExpr) {
                    found = comparisons.contains(new SPComp((CompExpr) inner));
                    loc = 3;
                } else {
                    found = SPSets.contains(new SPSet(inner));
                    loc = 4;
                }
                if (found) {
                    switch(loc) {
                    case 1:
                        varSet.remove(((Var) inner).getValueID());
                        break;
                    case 2:
                        ternSet.remove(new SPTern((Ternary) inner));
                        break;
                    case 3:
                        comparisons.remove(new SPComp((CompExpr) inner));
                        break;
                    case 4:
                        SPSets.remove(new SPSet(inner));
                        break;
                    default:
                        throw new RuntimeException("this line should be unreachable");
                    }
                }
            }
            if (SPSets.contains(new SPSet(expr))) {
                SPSets.remove(new SPSet(expr));
                return;
            }
            for (SPSet curSet : SPSets) {
                if (curSet.contains(expr)) {
                    curSet.remove(expr);
                    return;
                }
            }
            for (SPTern tern : ternSet) {
                if (tern.cond.contains(expr)) {
                    tern.cond.remove(expr);
                    return;
                } else if (tern.falseBranch.contains(expr)) {
                    tern.falseBranch.remove(expr);
                    return;
                } else if (tern.trueBranch.contains(expr)) {
                    tern.trueBranch.remove(expr);
                    return;
                }
            }
            for (SPComp comp : comparisons) {
                if (comp.getLhs().contains(expr)) {
                    comp.getLhs().remove(expr);
                    return;
                } else if (comp.getRhs().contains(expr)) {
                    comp.getRhs().remove(expr);
                    return;
                }
            }
        } else {
            throw new RuntimeException("Should be impossible to get here");
        }

    }

    @Override
    public int hashCode(){
        int hash = operator.hashCode();
        for (SPSet cset : SPSets){
            hash = (hash + cset.hashCode()) % Integer.MAX_VALUE;
        }
        for (ValueID decl : varSet){
            hash = (hash + decl.hashCode()) % Integer.MAX_VALUE;
        }
        for (Long il : intSet){
            hash = (hash + il.hashCode()) % Integer.MAX_VALUE;
        }
        for (Boolean bl : boolSet){
            hash = (hash + bl.hashCode()) % Integer.MAX_VALUE;
        } for (SPTern tern : ternSet){
            hash = (hash + tern.hashCode()) % Integer.MAX_VALUE;
        }
        for (MethodCall call : methodCalls) {
            hash = (hash + call.hashCode()) % Integer.MAX_VALUE;
        } for (SPComp comp : comparisons) {
            hash = (hash + comp.hashCode()) % Integer.MAX_VALUE;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof SPSet)){
            return false;
        }
        SPSet sp = (SPSet)obj;
        if(sp.operator != this.operator){
            return false;
        }
        List<SPSet> SPCopy = new ArrayList<SPSet>(SPSets);
        if (SPSets.size() != sp.SPSets.size()) {
            return false;
        }
        for (SPSet i : sp.SPSets) {
            if (!SPCopy.contains(i)) {
                return false;
            }
            SPCopy.remove(i);
        }
        List<ValueID> valCopy = new ArrayList<ValueID>(varSet);
        if (varSet.size() != sp.varSet.size()) {
            return false;
        }
        for (ValueID i : sp.varSet) {
            if (!valCopy.contains(i)) {
                return false;
            }
            valCopy.remove(i);
        }
        List<Long> intCopy = new ArrayList<Long>(intSet);
        if (intSet.size() != sp.intSet.size()) {
            return false;
        }
        for (Long i : sp.intSet) {
            if (!intCopy.contains(i)){
                return false;
            }
            intCopy.remove(i);
        }
        List<Boolean> boolCopy = new ArrayList<Boolean>(boolSet);
        if (boolSet.size() != sp.boolSet.size()) {
            return false;
        }
        for (Boolean i : sp.boolSet) {
            if (!boolCopy.contains(i)) {
                return false;
            }
            boolCopy.remove(i);
        }
        List<SPTern> ternCopy = new ArrayList<SPTern>(ternSet);
        if (ternSet.size() != sp.ternSet.size()) {
            return false;
        }
        for (SPTern i : sp.ternSet) {
            if (!ternCopy.contains(i)) {
                return false;
            }
            ternCopy.remove(i);
        }
        List<MethodCall> callCopy = new ArrayList<MethodCall>(methodCalls);
        if (methodCalls.size() != sp.methodCalls.size()) {
            return false;
        }
        for (MethodCall i : sp.methodCalls) {
            if (!callCopy.contains(i)) {
                return false;
            }
            callCopy.remove(i);
        }
        List<SPComp> compCopy = new ArrayList<SPComp>(comparisons);
        if (comparisons.size() != sp.comparisons.size()) {
            return false;
        }
        for (SPComp i : sp.comparisons) {
            if (!compCopy.contains(i)) {
                return false;
            }
            compCopy.remove(i);
        }
        return true;
    }

    public String toString(){
        return operator.toString() + "(" + "SPSets: " + SPSets.toString() + " | VarSets: " + varSet.toString() + ")" + System.getProperty("line.separator");
    }
}
