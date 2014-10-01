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

/**
 * This class is the factory which actually makes all the IR_nodes. 
 * It has many methods for validation of expressions. 
 * The types of validation expressions are var, arithOp, 
 * call, compareOp, condOp, EqOp, load, store, literal,
 * break, continue, for, block, statement, return, while, if,
 * and expr.
 * 
 * To call a validate method, append validate followed by the 
 * type in CamelCase; i.e. validateVar, validateCondOp.
 * 
 * There are also generate methods, which all follow the same patterns.
 * 
 * Lastly, there is a checkIntSize method to ensure that
 * no integer specified exceeds the limit for integer storage.
 *  
 *
 */
public class IRMaker {
    private static Map<Descriptor.Type, Type> MapMaker() {
        Map<Descriptor.Type, Type> map = new HashMap<Descriptor.Type, Type>();
        map.put(Descriptor.Type.BOOL, Type.BOOL);
        map.put(Descriptor.Type.BOOL_ARRAY, Type.BOOLARR);
        map.put(Descriptor.Type.INT, Type.INT);
        map.put(Descriptor.Type.INT_ARRAY, Type.INTARR);
        return map;
    }
    /**
     * This method allows you to validate any variable by passing in the AST root alongside the global and local symbol tables. 
     * 
     * @param root - the root of the AST 
     * @param globals - the global symbol table, a Map<String, Descriptor>
     * @param locals - the local symbol table for the scope of this variable, a Map<String, Type>
     * @return boolean : True or False, answering whether or not the variable is valid.
     */
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

    /**
     * This method allows you to check the validity of an arithmetic operation.
     * 
     * @param root : the root of the AST for the operation
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope of this variable, a Map<String, Type>
     * @return boolean : True or False depending on validity of the given root
     */
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

    /**
     * 
     * This method allows you to validate method calls.
     * 
     * @param root : the ast root for the method call
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of method call
     */
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
    
    /**
     * This method allows you to check the validity of a compare operation.
     *  
     * @param root : the ast root for the compare operation
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the comparison 
     */
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
    
    /**
     * This method allows you to check the validity of a conditional operation.
     * 
     * @param root : the ast root for the conditional operation
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the conditional
     */
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
    
    /**
     * This method allows you to check the validity of an equivalence operation.
     * 
     * @param root : the ast root for the equivalence operation
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the equivalence operation
     */
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
    
    /**
     * This method checks the validity of a load operation.
     * 
     * @param root : the ast root for the load operation
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the load
     */
    private boolean ValidateLoad(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        return ValidateVar(root, globals, locals);
    }
    
    /**
     * This method checks the validity of a store operation.
     * 
     * @param root : the ast root for the store operation.
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the store
     */
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
    
    /**
     * This method allows you to check the validity of a literal.
     * 
     * @param root : the ast root for the literal
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the literal
     */
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

    /**
     * This method allows you to check to make sure that an integer lies within Integer.MIN_VALUE and Integer_MAX value.
     * It takes a string, text, which represents the integer (which may be in hex!), and applies checking first.
     * 
     * It turns the string into a BigInteger, then checks the bounds on it. 
     * 
     * @param text : A string that represents the value of the integer before we try type casting.
     * @param flip_sign : A boolean that indicates whether or not we should flip the positive or negative sign on the BigInt after we make it.
     * @return boolean : True or False depending on whether or not the BigInt falls within the valid boundaries of a normal integer
     */
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
    
    /**
     * This method allows you to check for the validity of break statements.
     * It makes sure the break is in a loop.
     * 
     * @param root : the ast root for the break statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the break
     */
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
    
    /**
     * This method allows you to check the validity of a continue statement.
     * It makes sure that the the continue is inside of a loop.
     * 
     * @param root : the ast root for the continue statment
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the continue statement
     */
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
    
    /**
     * This method allows you to check the validity of a for statement.
     * It checks for the proper number of children and that the loop variable is declared correctly.
     * 
     * @param root : the ast root for the "for" statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the "for" statement
     */
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

    /**
     * This lets you check the validity of a BLOCK. 
     * It checks to make sure that the token type of the root of the AST is a BLOCK, then
     * parses through the parts in the method to make sure they are valid. 
     * 
     * @param root : the ast root for the block
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the code block
     */
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
    
    /**
     * This method allows you to check for the validity of a statement.
     * It is a dispatch method; it checks the root for its type and calls the proper verifier for it.
     * 
     * @param root : the ast root for the statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the statement
     */
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

    /**
     * This allows you to check the validity of a return statement. 
     * It checks the root's type, checks for non-void method, and any missing returns/improper type returns
     * 
     * @param root : the ast root for the return statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the return statement
     */
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

    /**
     * This allows you to check the validity of a while statement.
     * It currently returns false and does not seem to be fully implemented; thus, any "while" will fail.
     * 
     * @param root : the ast root for the while statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of while - currently always False
     */
    private boolean ValidateWhile(AST root, Map<String, Descriptor> globals,
            List<Map<String, Type>> locals) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * This allows for checking the validity of an if statement.
     * It checks that the expression in the if is valid,  that the if expression is a boolean,
     * and if the block inside the if is valid.
     * 
     * @param root : the ast root for the if statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals : the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of the if statement
     */
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

    /**
     * This allows for checking the validity of an expression.
     * It is currently unimplemented and returns false.
     * 
     * @param root : the ast root for the expression
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return boolean : True or False depending on validity of expression - currently always False
     */
    private boolean ValidateExpr(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * This method actually generates and returns an IR_Node specified for variables; i.e. an IR_Var.
     * If does some checking to make sure the var is valid, actually initialized, and recorded in the symbol tables. 
     * 
     * @param root : the ast root for the var
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Var : IR_Node specialized for variables
     */
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

    /**
     * This method generates and returns an IR_Node specialized for arithmetic operations, i.e. an IR_ArithOp.
     * It checks the validity of the operation then instantiates a special version of an IR_ArithOp node 
     * specialized for the equation that was found from the given AST root.
     * 
     * @param root : the ast root for the arithmetic operation
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_ArithOp : IR_Node specialized for arithmetic operations - actually a subclass instance, specialized for specific equations
     */
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

    /**
     * This method generates method calls. 
     * It checks the validity of the calls and differentiates handling between callouts and normal methods.
     * 
     * @param root : the ast root for the method call or callout
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Call : IR_Node specialized for either type of method call
     */
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
    
    /**
     * This method generates comparison operations.
     * It generates expressions for the left and right hand sides of the given root node,
     * checks which comparison operation this is, and returns a specific instance of IR_CompareOp
     * specialized for that particular comparison type.
     *  
     * @param root : the ast root for the comparison operator
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_CompareOp : IR_Node specialized for comparison ops - actually a subclass specialized for a particular operation
     */
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
    
    /**
     * This method generates conditional operations. 
     * It generates expressions for the left and right children of the given AST root, and returns a specialized
     * instance of IR_CondOp for AND or OR. 
     * Validity checks are included.
     * 
     * @param root : the ast root for the conditional operation
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_CondOp : IR_Node specialized for comparison operations - actually a subclass specialized for AND or OR
     */
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
    
    /**
     * This method generates equivalence operations.
     * It validates the conditional operator, then makes expressions for the left and right hand side of the given
     * AST root node, then uses those to create a specialized version of IR_EqOp for either Equals or NotEquals.  
     * 
     * @param root : the ast root for the equivalence operation
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_EqOp : IR_Node specialized for equivalence ops; specifically, a subclass specialzed for Equals and NotEquals
     */
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
    
    /**
     * This method generates a load statement.
     * It makes sure the load is valid, then looks up the variable it needs to load in the local table. 
     * If it's not in the local table, it looks in the global table.
     * Returns null if the variable is  not found.
     * For the successful return specified below, L = Local, P = Parameter, F = Field.
     * 
     * @param root : the ast root for the load
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Node : Either an IR_LDL, IR_LDP, or IR_LDF, depending on where the var was found. 
     */
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
    
    /**
     * This method generates a store statement.
     * It makes sure the store is valid, then looks up the variable it needs to store in the local table. 
     * If it's not in the local table, it looks in the global table.
     * Once it's found, it makes a new IR_Node with the stored data. 
     * Returns null if the variable is  not found.
     * For the successful return specified below, L = Local, P = Parameter, F = Field.
     * 
     * @param root : the ast root for the store
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Node : Either an IR_STL, IR_STP, or IR_STF, depending on where the var was found.
     */
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
    
    /**
     * This method generates literals. 
     * It checks the type of the given AST root, and from there does additional inference to determine which specific subclass to return.
     * Returns null if no literal is possible.
     * 
     * @param root : the ast root for the literal
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Literal : IR_Node specialized for literals; specifically, a subclass specialzed for IntLiteral or BoolLiteral
     */
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
    
    /**
     * This method generates break statements.
     * It checks whether or not the break is valid. 
     * If it is not, it returns null. 
     * 
     * @param root : the ast root for the break statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Break : IR_Node specialized for break statements
     */
    private IR_Break GenerateBreak(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!(ValidateBreak(root, globals, locals))) {
            System.err.println("IRMaker error -called GenerateBreak when no break possible");
            return null;
        }
        return new IR_Break();
    }
    
    /**
     * This method generates continue statements.
     * It checks for the validity of said statement, and returns null if it would be invalid.
     * 
     * @param root : the ast root for the continue statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Continue : IR_Node specialized for continue statements
     */
    private IR_Continue GenerateContinue(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        if (!(ValidateContinue(root, globals, locals))) {
            System.err.println("IRMaker error - called GenerateContinue when no continue possible");
            return null;
        }
        return new IR_Continue();
    }
    
    /**
     * This method generates for statements.
     * It checks the validity of the for statement, then performs further checks to decide what the operation in the for is,
     * and uses these to generate an expression before using that to generate a block.
     * An IR_For is then returned containing the preloop data, the condition on which the for is predicated, and the block.
     * 
     * @param root : the ast root for the for statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_For : IR_Node specialized for "for" statements: Contains preloop info, conditional, and block
     */
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

    /**
     * This method generates blocks used inside other codes, like the body of for loops.
     * It checks the validity of the block, and if it is valid, steps through it and sets up the information needed
     * to form an IR_Node with a sequence of statements that makes up the block. Hence, an IR_Seq.
     * 
     * @param root : the ast root for the block
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Seq : IR_Node specialized for blocks : It contains a sequence of statements representing the code in the block. 
     */
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
    
    /**
     *This method generates return statements.
     *It checks the validity of the return statement, and the number of children of the provided root.
     *If the number of children is 0, the IR_Return has a null value. Else, it will have an expression value.
     * 
     * @param root : the ast root for the return statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Return : IR_Node specialized for return statements : contains either an expression or null
     */
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

    /**
     * This method generates while statements. 
     * It is currently unimplemented.
     * 
     * @param root : the ast root for the while statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Node : IR_Node specialized for while statements; however, currently is always null
     */
    private IR_Node GenerateWhile(AST root, Map<String, Descriptor> globals,
            List<Map<String, Type>> locals) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * This method generates if statements.
     * It checks the validity of the if statement, and if that statement is valid, it adds a new hashmap to the local table,
     * generates an expression descripbing the if condition, generates a block for the true and false aspects of the if,
     * and returns an IR_Node that holds all three of the above fields; an IR_If. 
     * 
     * @param root : the ast root for the if statement
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_If : IR_Node specialized for If statements. Contains the condition for the if, and the true/false blocks for that condition.
     */
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
            false_block = new IR_Seq(Collections.EMPTY_LIST);
        }
        return new IR_If(condition, true_block, false_block);
    }

    /**
     * This method generates Expressions.
     * It is currently unimplemented.
     * 
     * @param root : the ast root for the expression
     * @param globals : the global symbol table, a Map<String, Descriptor>
     * @param locals the local symbol table for the scope in which the call is made, a Map<String, Type>
     * @return IR_Node : IR_Node specialized for expressions; however, currently always null.
     */
    private IR_Node GenerateExpr(AST root, Map<String, Descriptor> globals, List<Map<String, Type>> locals) {
        return null;
    }

}
