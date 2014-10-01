package edu.mit.compilers.ir;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Descriptors.Descriptor;
import antlr.collections.AST;
import edu.mit.compilers.grammar.DecafParserTokenTypes;
import edu.mit.compilers.grammar.DecafScannerTokenTypes;
import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;

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
        } else if (root.getNumberOfChildren() == 1 && ValidateExpr(root.getFirstChild(), globals, locals) 
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
            if (ValidateExpr(root.getFirstChild(), globals, locals) && ValidateExpr(root.getFirstChild().getNextSibling(), globals, locals)) {
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

    private boolean ValidateCall(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (root.getType() != DecafScannerTokenTypes.METHOD_CALL) {
            System.err.println("IRMaker error - tried method call without method call at route at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        if (!(globals.containsKey(root.getFirstChild().getText()))) {
            System.err.println("tried to call undefined function " 
                    + root.getFirstChild().getText() + "at " 
                    + root.getFirstChild().getLine() 
                    + ":" + root.getFirstChild().getColumn());
            return false;
        }
        int num_args = root.getNumberOfChildren() - 1;
        AST arg = root.getFirstChild();
        Descriptor desc = globals.get(arg.getText());
        if (desc.getType() == Descriptor.Type.CALLOUT) {
            for (int i = 0; i < num_args; i++ ) {
                arg = arg.getNextSibling();
                if (!(arg.getType() == DecafParserTokenTypes.STRING_LITERAL) || ValidateExpr(arg, globals, locals)) {
                    System.err.println("arg passed to callout is neither String lit nor a valid exp at " + arg.getLine() + ":" + arg.getColumn());
                    return false;
                }
            }
            return true;
        } else if (desc.getType() == Descriptor.Type.METHOD) {
            List<Boolean> argTypes = desc.getArgTypes();
            for (int i = 0; i < num_args; i++ ) {
                arg = arg.getNextSibling();
                if (!((GenerateExpr(arg, globals, locals).evaluateType() == Type.BOOL && argTypes.get(i)) 
                        || (GenerateExpr(arg, globals, locals).evaluateType() == Type.INT && !argTypes.get(i)))) {
                    System.err.println("Passed args don't match declared types at " 
                            + root.getFirstChild().getLine() + ":" + root.getFirstChild().getColumn());
                    return false;
                }
            }
            return true;
        }
        System.err.println("IRMaker error - method's descriptor is neither callout nor method at " 
                + root.getFirstChild().getLine() + ":" + root.getFirstChild().getColumn());
        return false;
    }
    
    private boolean ValidateCompareOp(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (root.getNumberOfChildren() != 2) {
            System.err.println("Parser error must have occured - compare op with other than two children at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        if (root.getType() == DecafScannerTokenTypes.LT || root.getType() == DecafScannerTokenTypes.LTE 
                || root.getType() == DecafScannerTokenTypes.GT || root.getType() == DecafScannerTokenTypes.GTE) {
            if (ValidateExpr(root.getFirstChild(), globals, locals) && ValidateExpr(root.getFirstChild().getNextSibling(), globals, locals)) {
                boolean valid = true;
                if (! ((GenerateExpr(root.getFirstChild(), globals, locals)).evaluateType() == Type.INT)) {
                    System.err.println("Left child of compare op is not of int type at" + root.getLine() + ":" + root.getColumn());
                    valid = false;
                }
                if (! ((GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals)).evaluateType() == Type.INT)) {
                    System.err.println("Right child of compare op is not of int type at" + root.getLine() + ":" + root.getColumn());
                    valid = false;
                }
                return valid;
            } else {
                return false;
            }
        }
        System.err.println("IRMaker error must have occurred - tried IR_ArithOP without arith op root at " 
                           + root.getLine() + ":" + root.getColumn());
        return false;
    }
    
    private boolean ValidateCondOp(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (root.getNumberOfChildren() != 2) {
            System.err.println("Parser error must have occurred - cond op with other than two children at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        if (root.getType() == DecafScannerTokenTypes.AND || root.getType() == DecafScannerTokenTypes.OR) {
            if (ValidateExpr(root.getFirstChild(), globals, locals) && ValidateExpr(root.getFirstChild().getNextSibling(), globals, locals)) {
                boolean valid = true;
                if (! ((GenerateExpr(root.getFirstChild(), globals, locals)).evaluateType() == Type.BOOL)) {
                    System.err.println("Left child of cond op is not of bool type at" + root.getLine() + ":" + root.getColumn());
                    valid = false;
                }
                if (! ((GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals)).evaluateType() == Type.BOOL)) {
                    System.err.println("Right child of cond op is not of bool type at" + root.getLine() + ":" + root.getColumn());
                    valid = false;
                }
                return valid;
            } else {
                return false;
            }
        }
        System.err.println("IRMaker error must have occured - tried IR_ArithOP without arith op root at " 
                           + root.getLine() + ":" + root.getColumn());
        return false;
    }
    
    private boolean ValidateEqOp(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (root.getNumberOfChildren() != 2) {
            System.err.println("Parser error must have occurred - eq op with other than two children at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        if (root.getType() == DecafScannerTokenTypes.EQUALS || root.getType() == DecafScannerTokenTypes.NOT_EQUALS) {
            if (ValidateExpr(root.getFirstChild(), globals, locals) && ValidateExpr(root.getFirstChild().getNextSibling(), globals, locals)) {
                if (! ((GenerateExpr(root.getFirstChild(), globals, locals).evaluateType() == Type.INT 
                        && GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals).evaluateType() == Type.INT) 
                       || (GenerateExpr(root.getFirstChild(), globals, locals).evaluateType() == Type.BOOL 
                           && GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals).evaluateType() == Type.BOOL))) {
                    System.err.println("Equality can only be applied to two ints or two bools - at " + root.getLine() + ":" + root.getColumn());
                    return false;
                }
                return true;
            }
            return false;
        }
        return false;
    }
    
    private boolean ValidateLoad(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        return ValidateVar(root, globals, locals);
    }
    
    private boolean ValidateStore(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (root.getType() != DecafParserTokenTypes.ASSIGN 
                && root.getType() != DecafParserTokenTypes.ASSIGN_MINUS 
                && root.getType() != DecafParserTokenTypes.ASSIGN_PLUS) {
            System.err.println("IRMaker error - tried store without an assign op root at " + root.getLine() + ":" + root.getColumn());
            return false;
        } if ((ValidateVar(root.getFirstChild(), globals, locals) && ValidateExpr(root.getFirstChild().getNextSibling(), globals, locals))) {
            IR_Var lhs = GenerateVar(root.getFirstChild(), globals, locals);
            IR_Node rhs = GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals);
            if (lhs.getIndex() != null) {
                if ((lhs.evaluateType() == Type.INTARR && rhs.evaluateType() == Type.INT) 
                        || (lhs.evaluateType() == Type.BOOLARR && rhs.evaluateType() == Type.BOOL)) {
                    return true;
                }
                System.err.println("Type mismatch in assignment at " + root.getLine() + ":" + root.getColumn());
                return false;
            }
            if (lhs.evaluateType() == rhs.evaluateType()) {
                return true;
            }
            System.err.println("Type mismatch in assignment at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        return false;
    }
    
    private boolean ValidateLiteral(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (root.getType() == DecafParserTokenTypes.MINUS && root.getNumberOfChildren() == 1 
                && root.getFirstChild().getType() == DecafParserTokenTypes.INT_LITERAL) {
            if (CheckIntSize(root.getFirstChild().getText(), true)) {
                return true;
            }
            System.err.println("integer literal too big at " + root.getLine() + ":" + root.getColumn());
            return false;
        } else if (root.getType() == DecafParserTokenTypes.INT_LITERAL) {
            if (CheckIntSize(root.getText(), false)) {
                return true;
            }
            System.err.println("integer literal too big at " + root.getLine() + ":" + root.getColumn());
            return false;
        } else if (root.getType() == DecafParserTokenTypes.TK_true || root.getType() == DecafParserTokenTypes.TK_false) {
            return true;
        }
        System.err.println("IRMaker error - called validate literal when no literal possible");
        return false;
    }

    private boolean CheckIntSize(String text, boolean flip_sign) {
        BigInteger largest_allowed = new BigInteger(Integer.toString(Integer.MAX_VALUE));
        BigInteger smallest_allowed = new BigInteger(Integer.toString(Integer.MIN_VALUE));
        BigInteger checking;
        if (text.startsWith("0x")) {
            checking = new BigInteger(text.substring(2), 16);
        } else {
            checking = new BigInteger(text);
        }
        if (flip_sign) {
            checking = checking.negate();
        }
        return checking.compareTo(largest_allowed) < 0 && checking.compareTo(smallest_allowed) > 0;
    }
    
    private boolean ValidateBreak(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        boolean inside_loop = false;
        for (Map<String, Type> local_table : locals) {
            if (local_table.containsKey("for") || local_table.containsKey("while")) {
                inside_loop = true;
            }
        }
        if (!inside_loop) {
            System.err.println("break can only be called within a loop - at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        return root.getType() == DecafParserTokenTypes.TK_break && root.getNumberOfChildren() == 0; 
    }
    
    private boolean ValidateContinue(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        boolean inside_loop = false;
        for (Map<String, Type> local_table : locals) {
            if (local_table.containsKey("for") || local_table.containsKey("while")) {
                inside_loop = true;
            }
        }
        if (!inside_loop) {
            System.err.println("break can only be called within a loop - at " + root.getLine() + ":" + root.getColumn());
            return false;
        }
        return root.getType() == DecafParserTokenTypes.TK_continue && root.getNumberOfChildren() == 0;
    }
    
    private boolean ValidateFor(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (! (root.getType() == DecafParserTokenTypes.TK_for)) {
            return false;
        }
        if (! (root.getNumberOfChildren() == 5)) {
            System.err.println("Parser error - for nodes should have five children");
            return false;
        }
        if (! (root.getFirstChild().getNextSibling().getType() == DecafParserTokenTypes.ASSIGN)) {
            System.err.println("Parser error - for nodes must use '='");
            return false;
        }
        String ind_name = root.getFirstChild().getText();
        boolean found = false;
        for (Map<String, Type> local_table : locals) {
            if (local_table.containsKey(ind_name)) {
                if (local_table.get(ind_name) == Type.INT) {
                    found = true;
                }
                System.err.println("index variable must be of integer type - at " + root.getLine() + ":" + root.getColumn());
                return false;
            }
        }
        if (!found) {
            if (!globals.containsKey(ind_name)) {
                System.err.println("index variable must be previously declared - at " + root.getLine() + ":" + root.getColumn());
                return false;
            }
            if (!(globals.get(ind_name).getType() == Descriptor.Type.INT)) {
                System.err.println("index variable must be of integer type - at " + root.getLine() + ":" + root.getColumn());
                return false;
            }
        }
        if (ValidateExpr(root.getFirstChild().getNextSibling().getNextSibling(), globals, locals)) {
            if (!(GenerateExpr(root.getFirstChild().getNextSibling().getNextSibling(), globals, locals).evaluateType() == Type.INT)) {
                System.err.println("Can only assign ints to ints - at " + root.getLine() + ":" + root.getColumn());
                return false;
            }
        } else {
            return false;
        }
        if (ValidateExpr(root.getFirstChild().getNextSibling().getNextSibling().getNextSibling(), globals, locals)) {
            if (!(GenerateExpr(root.getFirstChild().getNextSibling().getNextSibling().getNextSibling(), globals, locals).evaluateType() == Type.INT)) {
                System.err.println("end of for loop must be an integer type - at " + root.getLine() + ":" + root.getColumn());
                return false;
            }
        } else {
            return false;
        }
        int num_elem = root.getNumberOfChildren() - 4;
        List<Map<String, Type>> fake_locals = new ArrayList<Map<String, Type>>();
        Collections.copy(fake_locals, locals);
        Map<String, Type> fake_for = new HashMap<String, Type>();
        fake_for.put("for", Type.NONE);
        fake_locals.add(fake_for);
        if (ValidateBlock(root.getFirstChild().getNextSibling().getNextSibling().getNextSibling().getNextSibling(), globals, fake_locals)) {
            return true;
        }
        return false;
    }

    private boolean ValidateBlock(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!(root.getType() == DecafParserTokenTypes.BLOCK)) {
            System.err.println("block expected but not present - IRMaker error");
            return false;
        }
        List<Map<String, Type>> locals_copy = new ArrayList<Map<String, Type>>();
        Collections.copy(locals_copy, locals);
        Map<String, Type> declared = locals.get(locals.size() - 1);
        boolean finished_fields = false;
        Type declaring = Type.NONE;
        int i = 0;
        AST block_start = root.getFirstChild();
        int num_elem = root.getNumberOfChildren();
        while (i < num_elem) {
            if (!finished_fields) {
                if (block_start.getType() == DecafParserTokenTypes.FIELD_DECL) {
                    int num_in_decl = block_start.getNumberOfChildren() - 1;
                    AST var = block_start.getFirstChild().getNextSibling();
                    if (block_start.getFirstChild().getType() == DecafParserTokenTypes.TK_int) {
                        declaring = Type.INT;
                    } else if (block_start.getType() == DecafParserTokenTypes.TK_boolean) {
                        declaring = Type.BOOL;
                    } else {
                        System.err.println("Parser error - program should have been rejected");
                        return false;
                    }
                    int j = 0;
                    while (j < num_in_decl) {
                        if (var.getType() == DecafParserTokenTypes.ID) {
                            if (declared.containsKey(var.getText())) {
                                System.err.println("variable already declared in same scope - at " + var.getLine() + ":" + var.getColumn());
                                return false;
                            }
                            if (var.getNextSibling().getType() == DecafParserTokenTypes.INT_LITERAL) {
                                if (declaring == Type.INT) {
                                    declared.put(var.getText(), Type.INTARR);
                                    j++; j++;
                                    var = var.getNextSibling().getNextSibling();
                                } else if (declaring == Type.BOOL) {
                                    declared.put(var.getText(), Type.BOOLARR);
                                    j++; j++;
                                    var = var.getNextSibling().getNextSibling();
                                } else {
                                    System.err.println("Parser error - program should have been rejected");
                                    return false;
                                }
                            } 
                            declared.put(root.getText(), declaring);
                            i++;
                            root = root.getNextSibling();
                        }  else {
                            System.err.println("Parser error - program should have been rejected");
                        }
                    }
                    i++;
                    block_start = block_start.getNextSibling();
                } else {
                    finished_fields = true;
                }
            } else {
                if (!ValidateStatement(block_start, globals, locals_copy)) {
                    return false;
                }
                i++;
                block_start = block_start.getNextSibling();
            }
        }
        return true;
    }
    
    private boolean ValidateStatement(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (root.getType() == DecafParserTokenTypes.ASSIGN || root.getType() == DecafParserTokenTypes.ASSIGN_MINUS 
            || root.getType() == DecafParserTokenTypes.ASSIGN_PLUS) {
            return ValidateStore(root, globals, locals);
        } else if (root.getType() == DecafParserTokenTypes.METHOD_CALL) {
            return ValidateCall(root, globals, locals);
        } else if (root.getType() == DecafParserTokenTypes.TK_if) {
            return ValidateIf(root, globals, locals);
        } else if (root.getType() == DecafParserTokenTypes.TK_for) {
            return ValidateFor(root, globals, locals);
        } else if (root.getType() == DecafParserTokenTypes.TK_while) {
            return ValidateWhile(root, globals, locals);
        } else if (root.getType() == DecafParserTokenTypes.TK_return) {
            return ValidateReturn(root, globals, locals);
        } else if (root.getType() == DecafParserTokenTypes.TK_break) {
            return ValidateBreak(root, globals, locals);
        } else if (root.getType() == DecafParserTokenTypes.TK_continue) {
            return ValidateContinue(root, globals, locals);
        }
        System.err.println("Parser error - no statement possible - program should have been rejected");
        return false;
    }

    private boolean ValidateReturn(AST root, Map<String, Descriptor> globals,
            List<Map<String, Type>> locals) {
        if (root.getType() != DecafParserTokenTypes.TK_return) {
            System.err.println("IRMaker error - called validate return without return token");
            return false;
        }
        Type retType = locals.get(0).get("return");
        if (retType == Type.NONE) {
            if (root.getNumberOfChildren() != 0) {
                System.err.println("Can't return a value from a void-type function - at" + root.getLine() + ":" + root.getColumn());
                return false;
            }
            return true;
        } else if (retType == Type.BOOL) {
            if (root.getNumberOfChildren() != 1 || !ValidateExpr(root.getFirstChild(), globals, locals) 
                || !(GenerateExpr(root.getFirstChild(), globals, locals).evaluateType() == Type.BOOL)) {
                System.err.println("Must return a boolean from a boolean-type function - at" + root.getLine() + ":" + root.getColumn());
                return false;
            }
            return true;
        } else if (retType == Type.INT) {
            if (root.getNumberOfChildren() != 1 || !ValidateExpr(root.getFirstChild(), globals, locals) 
                || !(GenerateExpr(root.getFirstChild(), globals, locals).evaluateType() == Type.INT)) {
                System.err.println("Must return an int from an int-type function - at" + root.getLine() + ":" + root.getColumn());
                return false;
            }
            return false;
        } 
        System.err.println("IRMaker error - return type not present");
        return false;
    }

    private boolean ValidateWhile(AST root, Map<String, Descriptor> globals,
            List<Map<String, Type>> locals) {
        if (root.getType() != DecafParserTokenTypes.TK_while) {
            System.err.println("IRMaker error - while token not present");
            return false;
        } if (root.getNumberOfChildren() != 2 && root.getNumberOfChildren() != 3) {
            System.err.println("Parser error - program should have been rejected");
            return false;
        }
        AST expr_node = root.getNextSibling();
        AST limit_node;
        AST block_node;
        if (root.getNumberOfChildren() == 3) {
            limit_node = root.getFirstChild().getNextSibling();
            block_node = limit_node.getNextSibling();
        } else {
            limit_node = null;
            block_node = expr_node.getNextSibling();
        }
        boolean valid = true;
        if (limit_node != null) {
            valid = ValidateLiteral(limit_node, globals, locals) && limit_node.getType() == DecafParserTokenTypes.INT_LITERAL;
        }
        valid = valid && ValidateExpr(expr_node, globals, locals); 
        if (valid) {
            if (!(GenerateExpr(expr_node, globals, locals).evaluateType() == Type.BOOL)) {
                System.err.println("while experession must evaluate to boolean - at " + root.getLine() + ":" + root.getColumn());
                return false;
            }
        }
        List<Map<String, Type>> fake_locals = new ArrayList<Map<String, Type>>();
        Collections.copy(fake_locals, locals);
        Map<String, Type> fake_while = new HashMap<String, Type>();
        fake_while.put("while", Type.NONE);
        fake_locals.add(fake_while);
        return valid && ValidateBlock(block_node, globals, fake_locals);
    }

    private boolean ValidateIf(AST root, Map<String, Descriptor> globals,
            List<Map<String, Type>> locals) {
        if (!(ValidateExpr(root.getFirstChild(), globals, locals))) {
            return false;
        } if (!(GenerateExpr(root.getFirstChild(), globals, locals).evaluateType() == Type.BOOL)) {
            System.err.println("If expressions must be booleans - at" + root.getLine() +":" + root.getColumn());
            return false;
        }
        List<Map<String, Type>> fake_locals = new ArrayList<Map<String, Type>>();
        Collections.copy(fake_locals, locals);
        fake_locals.add(new HashMap<String, Type>());
        if (!(ValidateBlock(root.getFirstChild().getNextSibling(), globals, fake_locals))) {
            return false;
        }
        fake_locals.remove(fake_locals.size() - 1);
        if (root.getNumberOfChildren() == 4) {
            fake_locals.add(new HashMap<String, Type>());
            if (!(ValidateBlock(root.getFirstChild().getNextSibling().getNextSibling().getNextSibling(), globals, fake_locals))) {
                return false;
            }
        }
        return true;
    }

    private boolean ValidateExpr(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
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

    private IR_Call GenerateCall(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!ValidateCall(root, globals, locals)) {
            return null;
        }
        String name = root.getFirstChild().getText();
        Descriptor desc = globals.get(name);
        Type ret;
        List<IR_Node> args = new ArrayList<IR_Node>();
        int num_children = root.getNumberOfChildren() - 1;
        AST arg = root.getFirstChild();
        if (desc.getType() == Descriptor.Type.CALLOUT) {
            ret = Type.INT;
            for (int i = 0; i < num_children; i++) {
                arg = arg.getNextSibling();
                if (arg.getType() == DecafParserTokenTypes.STRING_LITERAL) {
                    args.add(new IR_Literal.IR_StringLiteral(arg.getText()));
                } else {
                    args.add(GenerateExpr(arg, globals, locals));
                }
            }
        } else {
            String retType = desc.getReturnType();
            if (retType.equals("bool")) {
                ret = Type.BOOL;
            } else if (retType.equals("int")) {
                ret = Type.INT;
            } else {
                ret = Type.NONE;
            }
            for (int i = 0; i < num_children; i++) {
                arg = arg.getNextSibling();
                args.add(GenerateExpr(arg, globals, locals));
            }
        }
        return new IR_Call(name, ret, args);
    }
    
    private IR_CompareOp GenerateCompareOp(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!ValidateCompareOp(root, globals, locals)) {
            return null;
        }
        IR_Node lhs = GenerateExpr(root.getFirstChild(), globals, locals);
        IR_Node rhs = GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals);
        if (root.getType() == DecafParserTokenTypes.GT) {
            return new IR_CompareOp.IR_CompareOp_GT(lhs, rhs);
        } else if (root.getType() == DecafParserTokenTypes.GTE) {
            return new IR_CompareOp.IR_CompareOp_GTE(lhs, rhs);
        } else if (root.getType() == DecafParserTokenTypes.LT) {
            return new IR_CompareOp.IR_CompareOp_LT(lhs, rhs);
        } else if (root.getText().equals("LTE")) {
            return new IR_CompareOp.IR_CompareOp_LTE(lhs, rhs);
        } else {
            System.err.println("IRMaker eror - called GenerateCompareOp with no compare op at " + root.getLine() + ":" + root.getColumn());
            return null;
        }
    }
    
    private IR_CondOp GenerateCondOp(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!ValidateCondOp(root, globals, locals)) {
            return null;
        }
        IR_Node lhs = GenerateExpr(root.getFirstChild(), globals, locals);
        IR_Node rhs = GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals);
        if (root.getType() == DecafParserTokenTypes.AND) {
            return new IR_CondOp.IR_CondOp_And(lhs, rhs);
        } else if (root.getType() == DecafParserTokenTypes.OR) {
            return new IR_CondOp.IR_CondOp_Or(lhs, rhs);
        } else {
            System.err.println("IRMaker eror - called GenerateCondOp with no cond op at " + root.getLine() + ":" + root.getColumn());
            return null;
        }
    }
    
    private IR_EqOp GenerateEqOp(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!ValidateCondOp(root, globals, locals)) {
            return null;
        }
        IR_Node lhs = GenerateExpr(root.getFirstChild(), globals, locals);
        IR_Node rhs = GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals);
        if (root.getType() == DecafParserTokenTypes.EQUALS) {
            return new IR_EqOp.IR_EqOp_Equals(lhs, rhs);
        } else if (root.getType() == DecafParserTokenTypes.NOT_EQUALS) {
            return new IR_EqOp.IR_EqOp_NotEquals(lhs, rhs);
        } else {
            System.err.println("IRMaker eror - called GenerateEqOp with no eq op at " + root.getLine() + ":" + root.getColumn());
            return null;
        }
    }
    
    private IR_Node GenerateLoad(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!ValidateLoad(root, globals, locals)) {
            return null;
        }
        IR_Var var = GenerateVar(root, globals, locals);
        if (var.getIndex() != null) {
            return new IR_LDA(var);
        }
        int table_index = locals.size() - 1;
        while (table_index >= 0) {
            if (locals.get(table_index).containsKey(var.getName())) {
                break;
            }
            table_index--;
        }
        if (table_index > 0) {
            return new IR_LDL(var);
        } else if (table_index == 0) {
            return new IR_LDP(var);
        } else if (globals.containsKey(var.getName())) {
            return new IR_LDF(var);
        }
        System.err.println("IRMaker error - called GenerateLoad with undeclared variable at " + root.getLine() + ":" + root.getColumn());
        return null;
    }
    
    private IR_Node GenerateStore(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!ValidateStore(root, globals, locals)) {
            return null;
        }
        IR_Var lhs = GenerateVar(root.getFirstChild(), globals, locals);
        IR_Node rhs = GenerateExpr(root.getFirstChild().getNextSibling(), globals, locals);
        if (lhs.getIndex() != null) {
            return new IR_STA(lhs, rhs);
        }
        int table_index = locals.size() - 1;
        while (table_index >= 0) {
            if (locals.get(table_index).containsKey(lhs.getName())) {
                break;
            }
            table_index--;
        }
        if (table_index > 0) {
            return new IR_STL(lhs, rhs);
        } else if (table_index == 0) {
            return new IR_STP(lhs, rhs);
        } else if (globals.containsKey(lhs.getName())) {
            return new IR_STF(lhs, rhs);
        }
        System.err.println("IRMaker error - called GenerateStore with uninitialized variable at " + root.getLine() + ":" + root.getColumn());
        return null;
    }
    
    private IR_Literal GenerateLiteral(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!(ValidateLiteral(root, globals, locals))) {
            return null;
        }
        if (root.getType() == DecafParserTokenTypes.MINUS) {
            String text = root.getFirstChild().getText();
            if (text.startsWith("0x")) {
                return new IR_Literal.IR_IntLiteral(Integer.parseInt("-" + text.substring(2), 16));
            }
            return new IR_Literal.IR_IntLiteral(Integer.parseInt("-" + text));
        } else if (root.getType() == DecafParserTokenTypes.INT_LITERAL) {
            String text = root.getText();
            if (text.startsWith("0x")) {
                return new IR_Literal.IR_IntLiteral(Integer.parseInt(text.substring(2)));
            } else {
                return new IR_Literal.IR_IntLiteral(Integer.parseInt(text));
            }
        } else if (root.getType() == DecafParserTokenTypes.TK_true) {
            return new IR_Literal.IR_BoolLiteral(true);
        } else if (root.getType() == DecafParserTokenTypes.TK_false) {
            return new IR_Literal.IR_BoolLiteral(false);
        }
        System.err.println("IRMaker error - called Generate Literal with no literal possible at " + root.getLine() + ":" + root.getColumn());
        return null;
    }
    
    private IR_Break GenerateBreak(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!(ValidateBreak(root, globals, locals))) {
            System.err.println("IRMaker error -called GenerateBreak when no break possible");
            return null;
        }
        return new IR_Break();
    }
    
    private IR_Continue GenerateContinue(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!(ValidateContinue(root, globals, locals))) {
            System.err.println("IRMaker error - called GenerateContinue when no continue possible");
            return null;
        }
        return new IR_Continue();
    }
    
    private IR_For GenerateFor(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!(ValidateFor(root, globals, locals))) {
            System.err.println("IRMaker error - called GenerateFor when invalid");
            return null;
        }
        String name = root.getFirstChild().getText();
        IR_Node init_val = GenerateExpr(root.getFirstChild().getNextSibling().getNextSibling(), globals, locals);
        int table_ind = locals.size() - 1;
        while (table_ind >= 0) {
            if (locals.get(table_ind).containsKey(name)) {
                break;
            }
            table_ind--;
        }
        IR_Node init_store;
        IR_Node init_check;
        if (table_ind > 0) {
            init_store = new IR_STL(GenerateVar(root.getFirstChild(), globals, locals), init_val);
            init_check = new IR_LDL(GenerateVar(root.getFirstChild(), globals, locals));
        } else if (table_ind == 0) {
            init_store = new IR_STP(GenerateVar(root.getFirstChild(), globals, locals), init_val);
            init_check = new IR_LDP(GenerateVar(root.getFirstChild(), globals, locals));
        } else if (globals.containsKey(name)) {
            init_store = new IR_STF(GenerateVar(root.getFirstChild(), globals, locals), init_val);
            init_check = new IR_LDF(GenerateVar(root.getFirstChild(), globals, locals));
        } else {
            System.err.println("IRMaker error - undected, unitialized for loop index var");
            return null;
        }
        List<IR_Node> prelist = new ArrayList<IR_Node>();
        prelist.add(init_store);
        IR_Seq preloop = new IR_Seq(prelist);
        IR_CompareOp.IR_CompareOp_LT cond = new IR_CompareOp.IR_CompareOp_LT(init_check, 
                GenerateExpr(root.getFirstChild().getNextSibling().getNextSibling().getNextSibling(), globals, locals));
        Map<String, Type> for_block = new HashMap<String, Type>();
        for_block.put("for", Type.NONE);
        locals.add(for_block);
        IR_Seq block = GenerateBlock(root.getFirstChild().getNextSibling().getNextSibling().getNextSibling().getNextSibling(), 
                                     globals, locals);
        locals.remove(for_block);
        return new IR_For(preloop, cond, block);
    }

    private IR_Seq GenerateBlock(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        // TODO Auto-generated method stub
        if (!(ValidateBlock(root, globals, locals))) {
            return null;
        }
        List<IR_Node> statements = new ArrayList<IR_Node>();
        int i = 0;
        int num_elem = root.getNumberOfChildren();
        AST cur_elem = root.getFirstChild();
        while (i < num_elem) {
            if (cur_elem.getType() == DecafParserTokenTypes.FIELD_DECL) {
                Type declaring;
                AST var = cur_elem.getFirstChild().getNextSibling();
                int j = cur_elem.getNumberOfChildren() - 1;
                if (cur_elem.getFirstChild().getType() == DecafParserTokenTypes.TK_int) {
                    declaring = Type.INT;
                } else if (cur_elem.getFirstChild().getType() == DecafParserTokenTypes.TK_boolean) {
                    declaring = Type.BOOL;
                } else {
                    System.err.println("IRMaker error - should not have tried to generate block");
                    return null;
                }
                while (j >= 0 ) {
                    if (var.getNextSibling().getType() == DecafParserTokenTypes.INT_LITERAL) {
                        if (declaring == Type.INT) {
                            locals.get(locals.size()-1).put(var.getText(), Type.INTARR);
                        } else if (declaring == Type.BOOL) {
                            locals.get(locals.size()-1).put(var.getText(), Type.BOOLARR);
                        }
                        j++; j++;
                        var = var.getNextSibling().getNextSibling();
                    } else {
                        locals.get(locals.size()-1).put(var.getText(), declaring);
                    }
                }
            } else if (cur_elem.getType() == DecafParserTokenTypes.ASSIGN || cur_elem.getType() == DecafParserTokenTypes.ASSIGN_MINUS 
                       || cur_elem.getType() == DecafParserTokenTypes.ASSIGN_PLUS) {
                statements.add(GenerateStore(cur_elem, globals, locals));
            } else if (cur_elem.getType() == DecafParserTokenTypes.METHOD_CALL) {
                statements.add(GenerateCall(cur_elem, globals, locals));
            } else if (cur_elem.getType() == DecafParserTokenTypes.TK_if) {
                statements.add(GenerateIf(cur_elem, globals, locals));
            } else if (cur_elem.getType() == DecafParserTokenTypes.TK_for) {
                statements.add(GenerateFor(cur_elem, globals, locals));
            } else if (cur_elem.getType() == DecafParserTokenTypes.TK_while) {
                statements.add(GenerateWhile(cur_elem, globals, locals));
            } else if (cur_elem.getType() == DecafParserTokenTypes.TK_return) {
                statements.add(GenerateReturn(cur_elem, globals, locals));
            } else if (cur_elem.getType() == DecafParserTokenTypes.TK_break) {
                statements.add(GenerateBreak(cur_elem, globals, locals));
            } else if (cur_elem.getType() == DecafParserTokenTypes.TK_continue) {
                statements.add(GenerateContinue(cur_elem, globals, locals));
            } else {
                System.err.println("IRMaker error - invalid statement in block");
                return null;
            }
            i++;
            cur_elem = cur_elem.getNextSibling();
        }
        return new IR_Seq(statements);
    }

    private IR_Return GenerateReturn(AST root, Map<String, Descriptor> globals,
            List<Map<String, Type>> locals) {
        if (!ValidateReturn(root, globals, locals)) {
            return null;
        }
        IR_Node value = null;
        if (root.getNumberOfChildren() != 0) {
            value = GenerateExpr(root.getFirstChild(), globals, locals);
        }
        return new IR_Return(value);
    }

    private IR_While GenerateWhile(AST root, Map<String, Descriptor> globals,
            List<Map<String, Type>> locals) {
        if (!ValidateWhile(root, globals, locals)) {
            return null;
        }
        IR_Node condition = GenerateExpr(root.getFirstChild(), globals, locals);
        IR_Literal limit = null;
        AST block_root = root.getFirstChild().getNextSibling();
        if (root.getNumberOfChildren() == 3) {
            limit = GenerateLiteral(root.getFirstChild().getNextSibling(), globals, locals);
            block_root = block_root.getNextSibling();
        }
        Map<String, Type> while_block = new HashMap<String, Type>();
        while_block.put("while", Type.NONE);
        locals.add(while_block);
        IR_Seq block = GenerateBlock(block_root, globals, locals);
        return new IR_While(condition, (IR_IntLiteral) limit, block);
    }

    private IR_If GenerateIf(AST root, Map<String, Descriptor> globals,
            List<Map<String, Type>> locals) {
        if (!(ValidateIf(root, globals, locals))) {
            return null;
        }
        IR_Node condition = GenerateExpr(root.getFirstChild(), globals, locals);
        locals.add(new HashMap<String, Type>());
        IR_Seq true_block = GenerateBlock(root.getFirstChild().getNextSibling(), globals, locals);
        locals.remove(locals.size() - 1);
        IR_Seq false_block;
        if (root.getNumberOfChildren() == 4) {
            locals.add(new HashMap<String, Type>());
            false_block = GenerateBlock(root.getFirstChild().getNextSibling().getNextSibling(), globals, locals);
            locals.remove(locals.size() - 1);
        } else {
            false_block = new IR_Seq(new ArrayList<IR_Node>());
        }
        return new IR_If(condition, true_block, false_block);
    }

    private IR_Node GenerateExpr(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        return null;
    }

}
