package edu.mit.compilers.controlflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.codegen.CodegenConst;
import edu.mit.compilers.codegen.CodegenContext;
import edu.mit.compilers.codegen.Descriptor;
import edu.mit.compilers.codegen.Instruction;
import edu.mit.compilers.codegen.LocArray;
import edu.mit.compilers.codegen.LocLabel;
import edu.mit.compilers.codegen.LocLiteral;
import edu.mit.compilers.codegen.LocReg;
import edu.mit.compilers.codegen.LocStack;
import edu.mit.compilers.codegen.LocationMem;
import edu.mit.compilers.codegen.Regs;
import edu.mit.compilers.controlflow.Branch.BranchType;
import edu.mit.compilers.controlflow.Expression.ExpressionType;
import edu.mit.compilers.ir.IR_FieldDecl;
import edu.mit.compilers.ir.IR_MethodDecl;
import edu.mit.compilers.ir.IR_Node;
import edu.mit.compilers.ir.Ops;
import edu.mit.compilers.ir.Type;

public class Assembler {

    public static void setUp(ControlflowContext context, 
            List<IR_Node> callouts, List<IR_FieldDecl> globals,
            Map<String, START> methods) {
        List<FlowNode> processing = new ArrayList<FlowNode>();
        for (START node : methods.values()) {
            processing.add(node);
        }
        Set<FlowNode> seen = new HashSet<FlowNode>();
        while (processing.size() > 0) {
            FlowNode next = processing.remove(0);
            for (FlowNode child : next.getChildren()) {
                if (!seen.contains(child)) {
                    processing.add(child);
                }
                seen.add(child);
            }
            next.setLabel(context.genLabel());
        }
        for (IR_Node node : callouts) {
            generateCallout(node, context);
        }
        for (IR_FieldDecl decl : globals) {
            generateFieldDeclGlobal(decl, context);
        }
    }

    public static ControlflowContext generateProgram(IR_Node root) {
        ControlflowContext context = new ControlflowContext();
        List<IR_Node> callouts = new ArrayList<IR_Node>();
        List<IR_FieldDecl> globals = new ArrayList<IR_FieldDecl> ();
        Map<String, START> methods = new HashMap<String, START>();

        // populate the various internal data structures
        GenerateFlow.generateProgram(root, context, callouts, globals, methods);
        context = new ControlflowContext();
        setUp(context, callouts, globals, methods);
        for (Map.Entry<String, START> entry : methods.entrySet()) {
            generateMethodDecl(entry, context);
        }
        return context;
    }

    public static ControlflowContext generateProgram(List<IR_Node> callouts, List<IR_FieldDecl> globals, Map<String, START> methods){
        ControlflowContext context = new ControlflowContext();
        setUp(context, callouts, globals, methods);
        for (Map.Entry<String, START> entry : methods.entrySet()) {
            generateMethodDecl(entry, context);
        }
        return context;
    }

    public static void generateCallout(IR_Node node, ControlflowContext context){
        IR_MethodDecl decl = (IR_MethodDecl)node;
        String name = decl.name;
        Descriptor d = new Descriptor(node);
        context.putSymbol(name, d);
    }

    public static void generateFieldDeclGlobal(IR_FieldDecl decl, ControlflowContext context){
        Descriptor d = new Descriptor(decl);
        d.setLocation(new LocLabel(decl.getName()));
        context.putSymbol(decl.getName(), d);
    }

    public static void generateMethodDecl(Map.Entry<String, START> decl, ControlflowContext context){
        String name = decl.getKey();
        context.enterFun();
        context.incScope();

        Instruction tmpIns;
        context.addIns(new Instruction(".type",new LocLabel(name),new LocLabel("@function")));
        context.addIns(new Instruction(".text"));
        context.addIns(new Instruction(".global", new LocLabel(name)));
        tmpIns = Instruction.labelInstruction(name);
        context.addIns(tmpIns);

        LocReg rbp = new LocReg(Regs.RBP);
        LocReg rsp = new LocReg(Regs.RSP);
        context.addIns(new Instruction("pushq", rbp));      
        context.addIns(new Instruction("movq", rsp, rbp ));

        //instructions for potentially saving arguments.
        ArrayList<Instruction> argIns = new ArrayList<Instruction>();
        //save register parameters to stack
        List<IR_FieldDecl> args = decl.getValue().getArguments();
        for(int ii = 0; ii<args.size(); ii++){
            IR_FieldDecl a = args.get(ii);
            Descriptor argd = new Descriptor(a);
            context.putSymbol(a.getName(), argd);

            LocationMem argSrc = argLoc(ii);
            LocationMem argDst = argSrc;
            if(ii<CodegenConst.N_REG_ARG){
                //save register arguments on the stack
                List<Instruction> pushIns = context.push(argSrc);
                argIns.addAll(pushIns);
                argDst = context.getRsp();
                context.allocLocal(CodegenConst.INT_SIZE);
            }
            argd.setLocation(argDst);
        }
        context.addIns(argIns);
        //generateBlock accumulates static local stack size required. 
        FlowNode next = decl.getValue().getChildren().get(0);
        boolean isVoid = decl.getValue().getRetType() == Type.VOID;
        boolean done = next == null;
        List<Instruction> blockIns = new ArrayList<Instruction>();
        while (!done) {
            if (next instanceof Codeblock) {
                Codeblock blk = (Codeblock) next;
                blockIns.addAll(generateNode(blk, context, isVoid));
                done = blk.getIsBreak();
                next = next.getChildren().get(0);
            } else if (next instanceof Branch) {
                Branch br = (Branch) next;
                blockIns.addAll(generateBranch(br, context, isVoid));
                NoOp endBranch = findNop(br);
                if (endBranch == null) {
                    done = true;
                } else {
                    next = endBranch.getChildren().get(0);
                }
            } else if (next instanceof NoOp) {
                throw new RuntimeException("Something has gone screwy");
            } else if (next instanceof END) {
                blockIns.addAll(generateEnd((END) next, context, isVoid));
                done = true;
            } else {
                throw new RuntimeException("This ought not have occurred");
            }
        }

        //write instructions for function body.
        context.addIns(blockIns);
        Instruction moveSp = context.decScope();
        context.addIns(moveSp);

        // Since GenerateFlow adds ENDs, all nodes will have an END - must 
        // check in generateReturn that non-void functions don't return 
        // without a return value
    }

    private static List<Instruction> generateNode(FlowNode begin, ControlflowContext context, boolean isVoid) {
        if (begin instanceof Codeblock) {
            return generateBlock((Codeblock) begin, context, isVoid);
        } else if (begin instanceof Branch) {
            return generateBranch((Branch) begin, context, isVoid);
        } else if (begin instanceof END) {
            return generateEnd((END) begin, context, isVoid);
        }
        throw new RuntimeException("No expected node present");
    }

    private static List<Instruction> generateBlock(Codeblock begin, ControlflowContext context, boolean isVoid) {
        List<Instruction> ins = new ArrayList<Instruction>();
        ins.add(Instruction.labelInstruction(begin.getLabel()));
        for (Statement stat : begin.getStatements()) {
            ins.addAll(generateStatement(stat, context));
        }
        FlowNode child = begin.getChildren().get(0);
        ins.add(new Instruction("jmp", new LocLabel(child.getLabel())));
        return ins;
    }

    private static List<Instruction> generateBranch(Branch begin, ControlflowContext context, boolean isVoid) {
        List<Instruction> ins = new ArrayList<Instruction>();
        LocReg r10 = new LocReg(Regs.R10);
        ins.add(Instruction.labelInstruction(begin.getLabel()));
        ins.addAll(generateExpression(begin.getExpr(), context));
        ins.addAll(context.pop(r10));
        ins.add(new Instruction("cmp", new LocLiteral(0L), r10));
        ins.add(new Instruction("je", new LocLabel(begin.getFalseBranch().getLabel())));

        if (begin.getType() == BranchType.IF) {
            // make True branch - either ends at end or when hitting a if with two ends or an unpaired NOp
            context.incScope();
            boolean done = false;
            // will be a START's child
            FlowNode next = begin.getTrueBranch().getChildren().get(0);
            NoOp endBranch = null;
            boolean firstNode = true;
            while (!done) {
                if (next instanceof Codeblock) {
                    Codeblock blk = (Codeblock) next;
                    ins.addAll(generateBlock(blk, context, isVoid));
                    done = blk.getIsBreak();
                    next = next.getChildren().get(0);
                } else if (next instanceof Branch) {
                    Branch br = (Branch) next;
                    ins.addAll(generateBranch(br, context, isVoid));
                    NoOp innerEndBranch = findNop(br);
                    if (innerEndBranch == null) {
                        done = true;
                    } else {
                        next = innerEndBranch.getChildren().get(0);
                    }
                } else if (next instanceof NoOp) {
                    done = true;
                    endBranch = (NoOp) next;
                    if (firstNode) {
                        ins.add(new Instruction("jmp", new LocLabel(endBranch.getLabel())));
                    }
                } else if (next instanceof END) {
                    ins.addAll(generateEnd((END) next, context, isVoid));
                    done = true;
                } else {
                    throw new RuntimeException("This ought not have occurred");
                }
                firstNode = false;
            }
            done = false;
            ins.add(Instruction.labelInstruction(begin.getFalseBranch().getLabel()));
            next = begin.getFalseBranch().getChildren().get(0);
            while (!done) {
                if (next instanceof Codeblock) {
                    Codeblock blk = (Codeblock) next;
                    ins.addAll(generateBlock(blk, context, isVoid));
                    done = blk.getIsBreak();
                    next = next.getChildren().get(0);
                } else if (next instanceof Branch) {
                    Branch br = (Branch) next;
                    ins.addAll(generateBranch(br, context, isVoid));
                    NoOp tempEndBranch = findNop(br);
                    if (tempEndBranch == null) {
                        done = true;
                    } else {
                        next = tempEndBranch.getChildren().get(0);
                    }
                } else if (next instanceof NoOp) {
                    done = true;
                    if (next != endBranch && endBranch != null) {
                        throw new RuntimeException("Something has gone HORRIBLY wrong");
                    }
                } else if (next instanceof END) {
                    ins.addAll(generateEnd((END) next, context, isVoid));
                    done = true;
                } else {
                    throw new RuntimeException("This ought not have occurred");
                }
            }
            if (endBranch != null) {
                ins.add(Instruction.labelInstruction(endBranch.getLabel()));
                ins.add(new Instruction("jmp", new LocLabel(endBranch.getChildren().get(0).getLabel())));
            }
            ins.add(context.decScope());

        } else if (begin.getType() == BranchType.FOR) {
            // make True block
            boolean done = false;
            FlowNode next = begin.getTrueBranch().getChildren().get(0);
            if (next.getParents().size() != 1 || !(next.getParents().get(0) instanceof START)) {
                throw new RuntimeException("Maddie assumed something untrue");
            }
            context.incScope();
            while (!done) {
                if (next instanceof Codeblock) {
                    Codeblock blk = (Codeblock) next;
                    ins.addAll(generateBlock(blk, context, isVoid));
                    done = blk.getIsBreak();
                    next = next.getChildren().get(0);
                } else if (next instanceof Branch) {
                    if (next == begin) {
                        done = true;
                    } else {
                        Branch br = (Branch) next;
                        ins.addAll(generateBranch(br, context, isVoid));
                        NoOp endBranch = findNop(br);
                        if (endBranch == null) {
                            done = true;
                        } else {
                            next = endBranch.getChildren().get(0);
                        }
                    }
                } else if (next instanceof NoOp) {
                    throw new RuntimeException("no NoOps in fors");
                } else if (next instanceof END) {
                    ins.addAll(generateEnd((END) next, context, isVoid));
                    done = true;
                } else {
                    throw new RuntimeException("This ought not have occurred");
                }
            }
            ins.add(Instruction.labelInstruction(begin.getFalseBranch().getLabel()));
            ins.add(context.decScope());
        } else if (begin.getType() == BranchType.WHILE) {
            FlowNode next;
            if (begin.getIsLimitedWhile()) {
                FlowNode incrementer = begin.getTrueBranch().getChildren().get(0);
                ins.addAll(generateNode(incrementer, context, isVoid));
                Branch innerWhile = (Branch) incrementer.getChildren().get(0);
                ins.addAll(generateExpression(innerWhile.getExpr(), context));
                ins.addAll(context.pop(r10));
                ins.add(new Instruction("cmp", new LocLiteral(0L), r10));
                ins.add(new Instruction("je", new LocLabel(innerWhile.getFalseBranch().getLabel())));
                next = innerWhile.getTrueBranch().getChildren().get(0);
            } else {
                next = begin.getTrueBranch().getChildren().get(0);
            }
            boolean done = false;
            context.incScope();
            while (!done) {
                if (next instanceof Codeblock) {
                    Codeblock blk = (Codeblock) next;
                    ins.addAll(generateBlock(blk, context, isVoid));
                    done = blk.getIsBreak();
                    next = next.getChildren().get(0);
                } else if (next instanceof Branch) {
                    if (next == begin) {
                        done = true;
                    } else {
                        Branch br = (Branch) next;
                        ins.addAll(generateBranch(br, context, isVoid));
                        NoOp endBranch = findNop(br);
                        if (endBranch == null) {
                            done = true;
                        } else {
                            next = endBranch.getChildren().get(0);
                        }
                    }
                } else if (next instanceof NoOp) {
                    throw new RuntimeException("no NoOps in whiles");
                } else if (next instanceof END) {
                    ins.addAll(generateEnd((END) next, context, isVoid));
                    done = true;
                } else {
                    throw new RuntimeException("This ought not have occurred");
                }
            }
            ins.add(Instruction.labelInstruction(begin.getFalseBranch().getLabel()));
            ins.add(context.decScope());
        }
        return ins;
    }

    private static List<Instruction> generateEnd(END next, ControlflowContext context, boolean isVoid) {
        List<Instruction> stIns = new ArrayList<Instruction>();
        stIns.add(Instruction.labelInstruction(next.getLabel()));
        Expression expr = next.getReturnExpression();
        // We only have instructions to add if return value is not void.
        if (expr == null && !isVoid) {
            stIns.add(new Instruction("movq", 
                    new LocLiteral(CodegenConst.ERR_FUN_RET), new LocReg(Regs.RDI)));
            stIns.add(new Instruction("call", new LocLabel("exit")));
            return stIns;
        }
        if (expr != null) {
            LocReg r10 = new LocReg(Regs.R10);
            stIns.addAll(generateExpression(expr, context));
            stIns.add(new Instruction("pop", r10));
            stIns.add(new Instruction("mov", r10, new LocReg(Regs.RAX)));
        }
        stIns.add(new Instruction("leave"));
        stIns.add(new Instruction("ret"));
        return stIns;
    }

    private static NoOp findNop(Branch begin) {
        if (begin.getType() != BranchType.IF) {
            return (NoOp) begin.getFalseBranch();
        }
        // search true branch
        FlowNode next = begin.getTrueBranch().getChildren().get(0);
        boolean done = false;
        NoOp target = null;
        while (!done) {
            if (next instanceof Codeblock) {
                next = next.getChildren().get(0);
            } else if (next instanceof Branch) {
                NoOp innerNOP = findNop(((Branch) next));
                if (innerNOP == null) {
                    done = true;
                } else {
                    next = innerNOP.getChildren().get(0);
                }
            } else if (next instanceof END) {
                done = true;
            } else if (next instanceof NoOp) {
                target = (NoOp) next;
                done = true;
            } else {
                throw new RuntimeException("Something has gone horribly wrong in findNop");
            }
        }
        if (target != null) {
            return target;
        }
        //search false branch - all paths in the true branch terminate
        next = begin.getFalseBranch().getChildren().get(0);
        done = false;
        while (!done) {
            if (next instanceof Codeblock) {
                next = next.getChildren().get(0);
            } else if (next instanceof Branch) {
                NoOp innerNOP = findNop(((Branch) next));
                if (innerNOP == null) {
                    done = true;
                } else {
                    next = innerNOP.getChildren().get(0);
                }
            } else if (next instanceof END) {
                done = true;
            } else if (next instanceof NoOp) {
                target = (NoOp) next;
                done = true;
            } else {
                throw new RuntimeException("Something has gone horribly wrong in findNop");
            }
        }
        return target;
    }

    public static List<Instruction> generateExpression(Expression expr, ControlflowContext context) {
        List<Instruction> ins = new ArrayList<Instruction>();

        if (expr instanceof MethodCall) {
            ins = generateCall((MethodCall) expr, context);
            LocReg rax = new LocReg(Regs.RAX);
            ins.addAll(context.push(rax));
            return ins;
        }else if (expr instanceof AddExpr || expr instanceof MultExpr || expr instanceof DivExpr || expr instanceof ModExpr){
            ins = generateArithExpr(expr, context);
            return ins;
        } else if(expr instanceof CompExpr){
            CompExpr compare = (CompExpr) expr;
            ins = generateCompareOp(compare, context);
            return ins;
        } else if ((expr instanceof CondExpr) || (expr instanceof NotExpr)) {
            ins = generateCondOp(expr, context);
            return ins;
        } else if (expr instanceof EqExpr){
            EqExpr eq= (EqExpr) expr;
            ins = generateEqOp(eq, context);
            return ins;
        } else if (expr instanceof NegateExpr) {
            NegateExpr negation = (NegateExpr) expr;
            LocReg r10 = new LocReg(Regs.R10);
            ins = generateExpression(negation.getExpression(), context);
            ins.addAll(context.pop(r10)); //Get whatever that expr was off stack
            ins.add(new Instruction("negq", r10)); //negate it
            ins.addAll(context.push(r10)); //push it back to stack
            return ins;
        } else if (expr instanceof Ternary){
            Ternary ternary = (Ternary)expr;
            ins = generateTernaryOp(ternary, context);
            return ins;
        }else if(expr instanceof Var){
            Var var = (Var) expr;
            ins = generateVarExpr(var, context);
            return ins;
        } else if (expr instanceof IntLit || expr instanceof BoolLit) {
            ins = generateLiteral(expr, context);
            return ins;
        } else {
            System.err.println("Unexpected Node type passed to generateExpr: " + expr.getClass().getSimpleName());
            System.err.println("The node passed in was of type " + expr.getExprType().toString());
        }
        ins = null; 



        return ins;
    }

    private static List<Instruction> generateLiteral(Expression expr,
            ControlflowContext context) {
        List<Instruction> ins = new ArrayList<Instruction>();

        if (expr instanceof IntLit) {
            IntLit int_literal = (IntLit) expr;
            if(int_literal.getValue()>Integer.MAX_VALUE || 
                    int_literal.getValue()<Integer.MIN_VALUE ){
                LocReg rax = new LocReg(Regs.RAX);
                ins.add(new Instruction("movabsq",new LocLiteral(int_literal.getValue()),rax));
                ins.addAll(context.push(rax));

            }else{
                ins.addAll(context.push(new LocLiteral(int_literal.getValue())));
            }
        } 
        else if (expr instanceof BoolLit) {
            BoolLit bool_literal = (BoolLit) expr;
            if (bool_literal.getTruthValue()) {
                ins = context.push(new LocLiteral(CodegenConst.BOOL_TRUE));
            } else {
                ins = context.push(new LocLiteral(CodegenConst.BOOL_FALSE));
            }
        }
        return ins;
    }

    private static List<Instruction>  generateVarExpr(Var var,
            ControlflowContext context) {
        List<Instruction> ins = new ArrayList<Instruction>();
        LocationMem loc = generateVarLoc(var, context, ins);
        ins.addAll(context.push(loc));
        return ins;
    }

    private static List<Instruction> generateTernaryOp(Ternary ternary,
            ControlflowContext context) {
        List<Instruction> ins = new ArrayList<Instruction>();
        LocReg r10 = new LocReg(Regs.R10);
        String labelForFalse = context.genLabel();
        String labelForDone = context.genLabel();
        List<Instruction> trueInstructs = generateExpression(ternary.getTrueBranch(), context);
        List<Instruction> falseInstructs = generateExpression(ternary.getFalseBranch(), context);

        ins.addAll(generateExpression(ternary.getTernaryCondition(), context)); //Get result of conditional onto the stack by resolving it. 
        ins.addAll(context.pop(r10)); //pop result into r10.
        ins.add(new Instruction("cmp", new LocLiteral(1L), r10)); //Compare r10 against truth
        ins.add(new Instruction("jne", new LocLabel(labelForFalse))); //If result isn't equal, r10 is 0, meaning we take the false branch.
        ins.addAll(trueInstructs); //If we don't jump, resolve the true branch 
        ins.add(new Instruction("jmp", new LocLabel(labelForDone))); //jump to being done
        ins.add(Instruction.labelInstruction(labelForFalse)); //If we jump, we jump here.
        ins.addAll(falseInstructs); //Resolve the false branch. 
        ins.add(Instruction.labelInstruction(labelForDone)); //This is where we'd jump to if we resolved the true version, which skips over the whole false branch. 
        return ins; //We're done, return the list.
    }

    private static List<Instruction> generateEqOp(EqExpr eq,
            ControlflowContext context) {
        List<Instruction> ins = new ArrayList<Instruction>();
        LocReg r10 = new LocReg(Regs.R10);
        LocReg r11 = new LocReg(Regs.R11);
        ins.addAll(generateExpression(eq.getLeftSide(), context));
        ins.addAll(generateExpression(eq.getRightSide(), context));
        ins.addAll(context.pop(r10));
        ins.addAll(context.pop(r11));
        ins.add(new Instruction("cmp", r10, r11));
        ins.add(new Instruction("mov", new LocLiteral(0L), r11));
        ins.add(new Instruction("mov", new LocLiteral(1L), r10));
        String op = eq.getOperator() == Ops.EQUALS ? "cmove" : "cmovne";
        ins.add(new Instruction(op, r10, r11));
        ins.addAll(context.push(r11));
        return ins;
    }

    private static List<Instruction> generateCondOp(Expression expr,
            ControlflowContext context) {
        LocLiteral zero = new LocLiteral(0);
        LocLiteral one = new LocLiteral(1);

        String tLabel = context.genLabel();
        String fLabel = context.genLabel();

        ShortCircuitNode.SCLabel t = new ShortCircuitNode.SCLabel(tLabel);
        ShortCircuitNode.SCLabel f = new ShortCircuitNode.SCLabel(fLabel);
        String endLabel = context.genLabel();

        ShortCircuitNode cfg = ShortCircuitNode.shortCircuit(expr, t, f);

        List<Instruction> ins = cfg.codegen(context);

        ins.add(Instruction.labelInstruction(tLabel));
        ins.add(new Instruction("pushq", one));
        LocLabel end = new LocLabel(endLabel);
        ins.add(new Instruction("jmp",  end));
        ins.add(Instruction.labelInstruction(fLabel));
        ins.add(new Instruction("pushq", zero));

        ins.add(Instruction.labelInstruction(endLabel));
        ins.add(new Instruction("nop"));
        return ins;
    }

    private static List<Instruction> generateCompareOp(CompExpr compare,
            ControlflowContext context) {
        List<Instruction> ins = new ArrayList<Instruction>();
        Ops op = compare.getOperator();
        Expression left = compare.getLeftSide();
        Expression right = compare.getRightSide();
        LocReg r10 = new LocReg(Regs.R10);
        LocReg r11 = new LocReg(Regs.R11);
        ins.addAll(generateExpression(left,context));
        ins.addAll(generateExpression(right, context));
        ins.addAll(context.pop(r11));
        ins.addAll(context.pop(r10));
        String cmd = "";
        switch(op){
        case GT:
            cmd = "setg";
            break;
        case GTE:
            cmd = "setge";
            break;
        case EQUALS:
            cmd = "sete";
            break;
        case NOT_EQUALS:
            cmd = "setne";
            break;
        case LT:
            cmd = "setl";
            break;
        case LTE:
            cmd = "setle";
            break;
        default:
            throw new RuntimeException("called generateCompareOp without valid compare op - op was " + compare.getOperator());
        }
        ins.add(new Instruction("cmpq", r11,r10));
        LocReg al = new LocReg(Regs.AL);
        ins.add(new Instruction(cmd, al));
        //zero bit extension
        ins.add(new Instruction("movzbq", al, r10));
        ins.addAll(context.push(r10));
        return ins;
    }

    private static List<Instruction> generateArithExpr(Expression expr,
            ControlflowContext context) {
        List<Instruction> ins = new ArrayList<Instruction>();
        Ops op;
        Expression left, right;
        LocReg r10 = new LocReg(Regs.R10);
        LocReg r11 = new LocReg(Regs.R11);
        if (expr instanceof AddExpr) {
            AddExpr add = (AddExpr) expr;
            op = add.getOperator();
            left = add.getLeftSide();
            right = add.getRightSide();
        } else if (expr instanceof MultExpr) {
            MultExpr mult = (MultExpr) expr;
            op = mult.getOperator();
            left = mult.getLeftSide();
            right = mult.getRightSide();
        } else if (expr instanceof DivExpr) {
            DivExpr div = (DivExpr) expr;
            op = div.getOperator();
            left = div.getLeftSide();
            right = div.getRightSide();
        } else {
            ModExpr mod = (ModExpr) expr;
            op = mod.getOperator();
            left = mod.getLeftSide();
            right = mod.getRightSide();
        }
        ins.addAll(generateExpression(left,context));
        ins.addAll(generateExpression(right, context));
        ins.addAll(context.pop(r11));
        ins.addAll(context.pop(r10));

        if(!(op == Ops.DIVIDE || op==Ops.MOD)){
            String cmd = "";
            switch (op){ //PLUS, MINUS, TIMES
            case PLUS:
                cmd = "addq";
                break;
            case MINUS:
                cmd = "subq";
                break;
            case TIMES:
                cmd = "imulq";
                break;
            default:
                throw new RuntimeException("mistakes were made - should have been plus/minus/times in generateArithExpr of assembler");
            }
            ins.add(new Instruction(cmd, r11, r10));
            ins.addAll(context.push(r10));
        }else{
            LocReg rdx = new LocReg(Regs.RDX);
            LocReg rax = new LocReg(Regs.RAX);
            ins.addAll(context.push(rdx));
            ins.add(new Instruction("movq", r10, rax));
            ins.add(new Instruction("cqto"));
            ins.add(new Instruction("idivq", r11));//Divide rdx:rax by r11 contents - i.e divide lhs by rhs.
            if(op==Ops.MOD){
                ins.add(new Instruction("movq", rdx, rax));
            }
            ins.addAll(context.pop(rdx));
            ins.addAll(context.push(rax));          
        }
        return ins;
    }

    private static List<Instruction> generateStatement(Statement stat, ControlflowContext context) {
        List<Instruction> ins = new ArrayList<Instruction>();
        if (stat instanceof Assignment) {
            ins.addAll(generateAssign((Assignment) stat, context));
        } else if (stat instanceof MethodCallStatement) {
            ins.addAll(generateCall(((MethodCallStatement) stat).getMethodCall(), context));
        } else if (stat instanceof Declaration) {
            ins.addAll(generateFieldDecl((Declaration) stat, context));
        } else {
            throw new RuntimeException("Mistakes were made - generateStatement executed without an assignment, methodcall, or declaration");
        }
        return ins;
    }

    private static List<Instruction> generateAssign(Assignment assign, ControlflowContext context) {
        ArrayList<Instruction> ins = new ArrayList<Instruction>();
        Ops op = assign.getOperator();
        Var lhs = assign.getDestVar();
        Expression rhs = assign.getValue();

        ins.addAll(generateExpression(rhs,context));
        LocReg r10 = new LocReg(Regs.R10);
        LocReg r11 = new LocReg(Regs.R11);
        LocationMem dst= generateVarLoc(lhs, context, ins);
        ins.addAll(context.pop(r10));
        if(op != Ops.ASSIGN){
            String cmd = null;
            switch(op){
            case ASSIGN_PLUS:
                cmd = "addq";
                break;
            case ASSIGN_MINUS:
                cmd = "subq";
                break;
            default:
                break;
            }
            ins.add(new Instruction("movq", dst, r11));
            ins.add(new Instruction(cmd, r10, r11));
            ins.add(new Instruction("movq", r11, dst));
        }else{
            ins.add(new Instruction("movq", r10, dst));
        }
        return ins;
    }

    private static List<Instruction> generateCall(MethodCall call, ControlflowContext context) {
        ArrayList<Instruction> ins = new ArrayList<Instruction>();
        List<Expression> args = call.getArguments();
        for(int ii = args.size()-1; ii>=0; ii--){
            Expression arg = args.get(ii);
            //source location of argument
            LocationMem argSrc=null;
            if(arg.getExprType() == ExpressionType.STRING_LIT){
                StringLit sl= (StringLit) arg;
                String ss = sl.getValue();
                Long idx = context.stringLiterals.get(ss);
                if(idx==null){
                    idx = (long) context.stringLiterals.size();
                    context.stringLiterals.put(ss, idx);
                }
                argSrc = new LocLabel("$"+CodegenContext.StringLiteralLoc(idx));
            }else{
                List<Instruction> exprIns = generateExpression(arg, context);
                ins.addAll(exprIns);
                //load argument to temporary register.
                argSrc = new LocReg(Regs.R10);
                ins.addAll(context.pop(argSrc));
            }
            List<Instruction> argIns = setCallArg(argSrc,ii,context);
            ins.addAll(argIns);
        }
        if(call.getIsCallout()){
            //# of floating point registers is stored in rax
            //need to zero it for callouts.
            ins.add(new Instruction("movq", new LocLiteral(0),  new LocReg(Regs.RAX)));         
        }
        ins.add(new Instruction("call ", new LocLabel(call.getMethodName()) ));

        //pop all arguments on the stack
        if(args.size()>CodegenConst.N_REG_ARG){
            long stackArgSize = CodegenConst.INT_SIZE * (args.size()-CodegenConst.N_REG_ARG);
            ins.add(new Instruction("addq", new LocLiteral(stackArgSize), new LocReg(Regs.RSP)));
        }
        return ins;
    }

    private static List<Instruction> generateFieldDecl(Declaration declare, ControlflowContext context){
        ArrayList<Instruction> ins = new ArrayList<Instruction>();
        IR_FieldDecl decl = declare.getFieldDecl();
        String name = decl.getName();
        Descriptor d = new Descriptor(decl);
        Type type = decl.getType();
        long size = CodegenConst.INT_SIZE;
        switch (type) {
        case INTARR:
        case BOOLARR:
            size = decl.getLength().getValue() * CodegenConst.INT_SIZE;
            break;
        default:
            break;
        }
        LocStack loc = context.allocLocal(size);
        LocLiteral sizeLoc= new LocLiteral(size);
        ins.add(new Instruction("subq", sizeLoc, new LocReg(Regs.RSP)));
        d.setLocation(loc);
        context.putSymbol(name, d);
        if(type == Type.INTARR || type == Type.BOOLARR){
            LocLiteral lenLoc = new LocLiteral(size/8);
            //loop variable
            LocReg rax = new LocReg(Regs.RAX);
            String ll = context.genLabel();
            LocLabel jmpLabel = new LocLabel(ll);
            LocLiteral zero = new LocLiteral(0);
            ins.add(new Instruction("movq", zero,rax));
            ins.add(Instruction.labelInstruction(ll));
            ins.add(new Instruction("movq", zero, new LocArray(loc, rax, CodegenConst.INT_SIZE)));
            ins.add(new Instruction("addq", new LocLiteral(1), rax));
            ins.add(new Instruction("cmpq", lenLoc, rax));
            ins.add(new Instruction("jl", jmpLabel));

        }
        else{
            ins.add(new Instruction("movq", new LocLiteral(0), loc));
        }
        return ins;
    }

    private static LocationMem generateVarLoc(Var var, ControlflowContext context, List<Instruction> ins) {
        Descriptor d = context.findSymbol(var.getName());
        switch (d.getIR().getType()) {
        case INT:
        case BOOL:
            return d.getLocation();
        case INTARR:
        case BOOLARR:
            // should be safe, since var is a Var, and the descriptor of a Var should have an IR_Var
            Expression index = var.getIndex();
            IR_FieldDecl decl = (IR_FieldDecl)d.getIR();
            LocArray loc_array = null;
            long len = decl.getLength().getValue();
            if (index.getExprType() == ExpressionType.INT_LIT) {
                IntLit index_int = (IntLit) var.getIndex();
                loc_array = new LocArray(d.getLocation(), 
                        new LocLiteral(index_int.getValue()), CodegenConst.INT_SIZE);

                if(index_int.getValue() >= len){
                    //statically throw error
                    ins.add(new Instruction("jmp", new LocLabel(context.getArrayBoundLabel())));
                }

            } else {
                // evaluate index and push index location to stack
                ins.addAll(generateExpression(index, context));
                //must not use r11 or r10 here since in assign, they may be used
                LocReg rax = new LocReg(Regs.RAX);
                // saves offset at R11
                ins.add(new Instruction("popq", rax));
                loc_array = new LocArray(d.getLocation(), rax, CodegenConst.INT_SIZE);
                ins.add(new Instruction("cmpq", new LocLiteral(len), rax));
                ins.add(new Instruction("jge", new LocLabel(context.getArrayBoundLabel())));
            }
            return loc_array;
        default:
            return null;
        }
    }

    /**@brief registers used for function arguments.
     * 
     */
    private static final Regs regArg[] = {Regs.RDI, Regs.RSI, Regs.RDX,
        Regs.RCX, Regs.R8, Regs.R9};

    private static LocationMem argLoc(int idx){
        if(idx<CodegenConst.N_REG_ARG){
            return new LocReg(regArg[idx]);
        }
        long offset = 16+(idx-CodegenConst.N_REG_ARG)*CodegenConst.INT_SIZE;
        return new LocStack(offset);
    }

    /**@brief set the ith method call argument
     * 
     * @param argSrc
     * @param idx
     * @return
     */
    private static List<Instruction> setCallArg(LocationMem argSrc, int idx, ControlflowContext context){
        ArrayList<Instruction> ins=new ArrayList<Instruction>();
        if(idx<CodegenConst.N_REG_ARG){
            LocationMem argDst = argLoc(idx);
            ins.add(new Instruction("movq", argSrc, argDst));
        }else{
            ins.addAll(context.push(argSrc));
        }
        return ins;
    }

    public static class LowIR {
        public final List<IR_Node> callouts;
        public final List<IR_Node> globals;
        public final Map<String, START> methods;

        public LowIR(List<IR_Node> callouts, List<IR_Node> globals, Map<String, START> methods) {
            this.callouts = callouts;
            this.globals = globals;
            this.methods = methods;
        }
    }
}
