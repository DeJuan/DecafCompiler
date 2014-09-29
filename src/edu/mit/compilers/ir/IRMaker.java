package edu.mit.compilers.ir;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activity.InvalidActivityException;

import Descriptors.Descriptor;
import antlr.collections.AST;
import edu.mit.compilers.grammar.DecafParserTokenTypes;
import edu.mit.compilers.grammar.DecafScannerTokenTypes;

public class IRMaker {
    private static Map<Descriptor.Type, Type> MapMaker() {
        Map<Descriptor.Type, Type> map = new HashMap<Descriptor.Type, Type>();
        map.put(Descriptor.Type.BOOL, Type.BOOL);
        map.put(Descriptor.Type.BOOL_ARRAY, Type.BOOLARR);
        map.put(Descriptor.Type.INT, Type.INT);
        map.put(Descriptor.Type.INT_ARRAY, Type.INTARR);
        return map;
    }
    
    private boolean ValidateVar(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (root.getType() != DecafParserTokenTypes.ID) {
            System.err.println("Parser error must have occured - expected ID at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        Type declaredType = Type.NONE;
        for (Map<String, Type> local_var_map : locals) {
            if (local_var_map.containsKey(root.getText())) {
                declaredType = local_var_map.get(root.getText());
                break;
            }
        }
        if (declaredType == Type.NONE) {
            if (globals.containsKey(root.getText())) {
                Map<Descriptor.Type, Type> mapping = MapMaker();
                declaredType = mapping.get(globals.get(root.getText()).getType());
            }
        }
        if (declaredType == Type.NONE) {
            // Variable never declared
            System.err.println("Variable " + root.getText() + " used before declaration at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        // Is a int/bool or array referenced directly
        if (root.getNumberOfChildren() == 0) {
            return true;
        } else if (root.getNumberOfChildren() == 1 && ValidateExpr(root.getFirstChild()) 
                   && (declaredType == Type.INTARR || declaredType == Type.BOOLARR)) {
            boolean valid = GenerateExpr(root.getFirstChild(), globals, locals).evaluateType() == Type.INT;
            if (!valid) {
                System.err.println("arrays take integer indices only - see " + root.getFirstChild().getLine() + ":" + root.getFirstChild().getColumn());
            }
            return valid;
        }
        System.err.println("Parser error must have occurred - ID with multiple children at " + root.getLine() + ":" + root.getColumn());
        return false;
    }
    
    
    private boolean ValidateArithOp(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (root.getNumberOfChildren() != 2) {
            System.err.println("Parser error must have occured - arith op with other than two children at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        if (root.getType() == DecafScannerTokenTypes.PLUS || root.getType() == DecafScannerTokenTypes.MINUS 
                || root.getType() == DecafScannerTokenTypes.DIVIDE || root.getType() == DecafScannerTokenTypes.TIMES 
                || root.getType() == DecafScannerTokenTypes.MOD) {
            if (ValidateExpr(root.getFirstChild()) && ValidateExpr(root.getFirstChild().getNextSibling())) {
                boolean valid = true;
                if (! ((GenerateExpr(root.getFirstChild(), globals, locals)).evaluateType() == Type.INT)) {
                    System.err.println("Left child of arith op is not of int type at" + root.getLine() + ":" + root.getColumn());
                    valid = false;
                }
                if (! ((GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals)).evaluateType() == Type.INT)) {
                    System.err.println("Right child of arith op is not of int type at" + root.getLine() + ":" + root.getColumn());
                    valid = false;
                }
                return valid;
            } else {
                System.err.println("Parser error must have occured - arith op has invalid expresions at " + root.getLine() + ":" + root.getColumn());
                return false;
            }
        }
        System.err.println("IRMaker error must have occured - tried IR_ArithOP without arith op root at " + root.getLine() + ":" + root.getColumn());
        return false;
    }
    private boolean ValidateExpr(AST firstChild) {
        // TODO Auto-generated method stub
        return false;
    }
    
    private IR_Var GenerateVar(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!ValidateVar(root, globals, locals)) {
            return null;
        }
        Type declaredType = Type.NONE;
        for (Map<String, Type> local_var_map : locals) {
            if (local_var_map.containsKey(root.getText())) {
                declaredType = local_var_map.get(root.getText());
                break;
            }
        }
        if (declaredType == Type.NONE) {
            if (globals.containsKey(root.getText())) {
                Map<Descriptor.Type, Type> mapping = MapMaker();
                declaredType = mapping.get(globals.get(root.getText()).getType());
            }
        }
        IR_Node index = root.getNumberOfChildren() == 1 ? GenerateExpr(root.getFirstChild(), globals, locals) : null;
        return new IR_Var(root.getText(), declaredType, index);
    }
    
    private IR_ArithOp GenerateArithOp(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!ValidateArithOp(root, globals, locals)) {
            return null;
        }
        IR_Node lhs = GenerateExpr(root.getFirstChild(), globals, locals);
        IR_Node rhs = GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals);
        if (root.getText().equals("+")) {
            return new IR_ArithOp.IR_ArithOp_Plus(lhs, rhs);
        } else if (root.getText().equals("-")) {
            return new IR_ArithOp.IR_ArithOp_Sub(lhs, rhs);
        } else if (root.getText().equals("*")) {
            return new IR_ArithOp.IR_ArithOp_Mult(lhs, rhs);
        } else if (root.getText().equals("/")) {
            return new IR_ArithOp.IR_ArithOp_Div(lhs, rhs);
        } else if (root.getText().equals("%")) {
            return new IR_ArithOp.IR_ArithOp_Mod(lhs, rhs);
        } else {
            System.err.println("IRMaker eror - called GenerateArithOp with no arith op at " + root.getLine() + ":" + root.getColumn());
            return null;
        }
    }

    private IR_Node GenerateExpr(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        return null;
    }

}
