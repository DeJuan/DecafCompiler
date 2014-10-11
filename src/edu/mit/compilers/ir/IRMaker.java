package edu.mit.compilers.ir;

import edu.mit.compilers.grammar.DecafScannerTokenTypes;
import edu.mit.compilers.ir.IR_Literal.IR_BoolLiteral;
import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;
import edu.mit.compilers.ir.IR_Literal.IR_StringLiteral;
import antlr.collections.AST;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public class IRMaker {
	private SymbolTable<IR_Node> symbols;
	private Type _returnType;
	private boolean valid = true;
	private ArrayList<String> loops;
	
	public IRMaker() {
		symbols = new SymbolTable<IR_Node>();
		loops = new ArrayList<String> ();
	}

	/**@brief The main function that builds the IR tree.
	 * Builds a sequence of field declarations, method declarations
	 * and callout declarations.
	 * @param ast
	 * @return Root of the IR tree of type IR_Seq. Returns
	 * null of IR is illegal.
	 * 
	 */
	public IR_Node make(AST ast) {
		valid = true;
		symbols.clear();
		loops.clear();
		// global scope
		symbols.incScope();
		_returnType=null;
		
		IR_Seq root = new IR_Seq();

		if (!ast.getText().equals("program")) {
			System.out
					.println("IR builder Error: parser did not return a program.");
			return null;
		}

		AST child = ast.getFirstChild();
		while (child != null) {
			IR_Node decl = null;
			switch (child.getType()) {
			case DecafScannerTokenTypes.FIELD_DECL:
				ArrayList<IR_Node> decls = makeFieldDeclList(child);
				root.addNodes(decls);
				break;
			case DecafScannerTokenTypes.METHOD_DECL:
				decl = makeMethodDecl(child);
				if (decl != null) {
					root.addNode(decl);
				}
				break;
			case DecafScannerTokenTypes.TK_callout:
				decl = makeCalloutDecl(child);
				if (decl != null) {
					root.addNode(decl);
				}
				break;
			}
			child = child.getNextSibling();
		}
		checkMain();
		return root;
	}

	public boolean checkMain(){
		HashMap<String, IR_Node> globalt = symbols.getTable(0);
		IR_Node n = globalt.get("main");
		if(n==null || !(n.getType()==Type.METHOD) ){
			valid = false;
			System.err.println("No main function declared.");
			return false;
		}
		IR_MethodDecl decl = (IR_MethodDecl)n;
		if(decl.getNumArgs()>0){
			valid = false;
			System.err.println("main should not take arguments.");
			return false;
			
		}
		return true;
	}
	
	/**@brief After building the IR. One can check the validity.
	 * 
	 * @return True if the IR is legal.
	 */
	public boolean isValid(){
		return valid;
	}
	
	private IR_Node makeMethodDecl(AST root) {
		AST typeNode = root.getFirstChild();
		Type t = tokenToType(typeNode.getType());
		AST idNode = typeNode.getNextSibling();
		String funName = idNode.getText();
		if(checkDupSymbol(funName, idNode)){
			valid = false;
		}
		_returnType = t;
		IR_MethodDecl ir_method = new IR_MethodDecl(t, funName);
		symbols.put(funName, ir_method);
		// local and func param the same scope
		symbols.incScope();

		AST paramNode = idNode.getNextSibling();
		while (paramNode.getType() != DecafScannerTokenTypes.BLOCK) {
			Type argt = tokenToType(paramNode.getType());
			paramNode = paramNode.getNextSibling();
			String paramName = paramNode.getText();
			if (checkDupSymbol(paramName, paramNode)) {
				paramNode = paramNode.getNextSibling();
				valid = false;
				continue;
			}
			ir_method.addArg(argt, paramName);
			IR_FieldDecl ir_param = new IR_FieldDecl(argt, paramName);
			symbols.put(paramName, ir_param);
			paramNode = paramNode.getNextSibling();
		}

		IR_Seq body = makeBlock(paramNode, false);
		ir_method.setBody(body);
		symbols.decScope();
		_returnType=null;
		return ir_method;
	}

	IR_Seq makeBlock(AST root){
		return makeBlock(root, true);
	}
	
	IR_Seq makeBlock(AST root, boolean newScope) {
		if(newScope){
			symbols.incScope();
		}
		
		IR_Seq ir_block = new IR_Seq();
		AST child = root.getFirstChild();
		while (child != null) {
			switch (child.getType()) {
			case DecafScannerTokenTypes.FIELD_DECL:
				ArrayList<IR_Node> decls = makeFieldDeclList(child);
				ir_block.addNodes(decls);
				break;
			default:
				IR_Node statement = makeStatement(child);
				ir_block.addNode(statement);
				break;
			}
			child = child.getNextSibling();
		}
		
		if(newScope){
			symbols.decScope();
		}
		return ir_block;
	}

	IR_Node makeStatement(AST root) {
		IR_Node statement = null;
		int tokenType = root.getType();
		switch (tokenType) {
		case DecafScannerTokenTypes.ASSIGN:
		case DecafScannerTokenTypes.ASSIGN_PLUS:
		case DecafScannerTokenTypes.ASSIGN_MINUS:
			AST lnode = root.getFirstChild();
			IR_Var ir_var = makeLocation(lnode);
			if (ir_var == null) {
				return null;
			}
			if (tokenType == DecafScannerTokenTypes.ASSIGN_PLUS
					|| tokenType == DecafScannerTokenTypes.ASSIGN_MINUS) {
				if (ir_var.getType() != Type.INT) {
					System.err.println("Undefined operator for "
							+ ir_var.getName());
					printLineCol(System.err, root);
					valid = false;
					return null;
				}
			}
			AST rnode = lnode.getNextSibling();
			IR_Node expr = makeExpr(rnode);
			if (expr == null) {
				return null;
			}
			if (expr.getType() != ir_var.getType()) {
				System.err.println("Assign to "+ir_var.getName()+" with wrong type " );
				printLineCol(System.err, root);
				valid = false;
				return null;
			}

			statement = new IR_Assign(ir_var, expr, getOp(tokenType));
			break;
		case DecafScannerTokenTypes.METHOD_CALL:
			statement = makeMethodCall(root);
			break;
		case DecafScannerTokenTypes.TK_if:
			statement = makeIf(root);
			break;
		case DecafScannerTokenTypes.TK_for:
			statement = makeFor(root);
			break;
		case DecafScannerTokenTypes.TK_while:
			statement=makeWhile(root);
			break;
		case DecafScannerTokenTypes.TK_return:
			statement = makeReturn(root);
			break;
		case DecafScannerTokenTypes.TK_break:
			statement = makeBreak(root);
			break;
		case DecafScannerTokenTypes.TK_continue:
			statement = makeContinue(root);
			break;
		}
		return statement;
	}

	private IR_Continue makeContinue(AST root){
		if(loops.size()==0){
			System.err.println("Tried continue outside a loop.");
			printLineCol(System.err, root);
			valid = false;
			return null;
		}
		return new IR_Continue();		
	}
	
	private IR_Break makeBreak(AST root){
		if(loops.size()==0){
			System.err.println("Tried break outside a loop.");
			printLineCol(System.err, root);
			valid = false;
			return null;
		}
		return new IR_Break();
	}
	
	private IR_Return makeReturn(AST root){
		AST exprNode = root.getFirstChild();
		if(_returnType==null){
			//shouldn't happen
			System.err.println("Internal error in make return");
			return null;
		}
		if(_returnType==Type.VOID){
			if(exprNode != null){
				System.err.println("Return value in a void function");
				printLineCol(System.err,root);
				valid = false;
				return null;
			}
			return new IR_Return(null);
		}
		if(exprNode == null){
			System.err.println("Need return value in function");
			printLineCol(System.err,root);
			valid = false;
			return null;
			
		}
		IR_Node expr = makeExpr(exprNode);
		if(expr == null){
			return null;
		}
		
		if(expr.getType() != _returnType){
			System.err.println("Wrong return type.");
			printLineCol(System.err, root);
			valid = false;
			return null;
		}
		return new IR_Return(expr);
	}
	
	private IR_While makeWhile(AST root){
		IR_Node cond = makeExpr(root.getFirstChild());
		if(cond == null){
			return null;
		}
		if(cond.getType() != Type.BOOL){
			System.out.println("While loop condition must be boolean");
			printLineCol(System.err, root);
			valid = false;
			return null;
		}
		AST boundNode = root.getFirstChild().getNextSibling();
		IR_IntLiteral literal=null ;
		if(boundNode.getType() == DecafScannerTokenTypes.INT_LITERAL){
			literal = makeIntLiteral(boundNode);
			if(literal.getValue()<=0){
				System.err.println("Optional bound of while loop must be positive.");
				printLineCol(System.err, boundNode);
				valid = false;
				return null;
			}
			boundNode = boundNode.getNextSibling();
		}
		loops.add("while");
		IR_Seq block = makeBlock(boundNode);
		loops.remove(loops.size()-1);
		IR_While ret = new IR_While(cond, literal, block);
		return ret;
	}
	
	private IR_For makeFor(AST root){
		AST varNode = root.getFirstChild();
		String varName = varNode.getText();
		IR_Node varDecl = lookupSymbol(varName,varNode);
		if(varDecl==null){
			return null;
		}
		if(varDecl.getType()!=Type.INT){
			System.err.println("Loop variable must be int");
			printLineCol(System.err, varNode);
			valid = false;
			return null;
		}
		IR_Var ir_var= new IR_Var(varDecl.getType(), varName,null);
		AST exprNode = varNode.getNextSibling();
		IR_Node expr[] = new IR_Node[2];
		for (int ii = 0; ii < 2; ii++) {
			expr[ii]= makeExpr(exprNode);
			if (expr[ii] == null) {
				return null;
			}
			if (expr[ii].getType() != Type.INT) {
				System.err.println("Loop bounds must be int");
				printLineCol(System.err, varNode);
				valid = false;
				return null;
			}
			exprNode = exprNode.getNextSibling();
		}
		loops.add("for");
		IR_Seq block = makeBlock(exprNode);
		loops.remove(loops.size()-1);
		IR_For ret = new IR_For(ir_var, expr[0], expr[1], block);
		return ret;
	}
	
	private IR_If makeIf(AST root){
		AST condNode = root.getFirstChild();
		IR_Node expr = makeExpr(condNode);
		if(expr==null){
			return null;
		}
		if(expr.getType() != Type.BOOL){
			System.err.println("If condition must be a boolean.");
			printLineCol(System.err, condNode );
			valid = false;
			return null;
		}
		AST blockNode = condNode.getNextSibling();
		IR_Seq trueBlock = makeBlock(blockNode);
		blockNode = blockNode.getNextSibling() ;
		if(blockNode== null){
			return new IR_If(expr, trueBlock, null);
		}
		blockNode = blockNode.getNextSibling();
		IR_Seq falseBlock = makeBlock(blockNode);
		
		return new IR_If(expr, trueBlock, falseBlock);
	}
	
	private IR_Var makeLocation(AST root) {
		AST idNode = root.getFirstChild();
		String varName = idNode.getText();
		IR_Node varDecl = lookupSymbol(varName, idNode);
		if (varDecl == null) {
			return null;
		}

		if (!(varDecl instanceof IR_FieldDecl)) {
			System.err.println("Location has to be variables");
			printLineCol(System.err, root);
			valid = false;
			return null;
		}

		Type t = varDecl.getType();
		IR_Var ir_var = null;
		IR_Node expr = null;
		if (isArrType(t)) {
			AST exprNode = idNode.getFirstChild();
			if (exprNode != null) {
				expr = makeExpr(exprNode);
				if (expr == null) {
					return null;
				}
				if (expr.getType() != Type.INT) {
					System.err.println("Array index must be an integer.");
					printLineCol(System.err, idNode);
					valid = false;
					return null;
				}
				t = toEntryType(t);
			}
		} else {
			if (idNode.getFirstChild() != null) {
				System.err.println("Can not index into " + varName);
				printLineCol(System.err, idNode);
				valid = false;
				return null;
			}
		}
		ir_var = new IR_Var(t, varName, expr);
		return ir_var;
	}

	private IR_Node makeExpr(AST root) {
		AST lhs;
		IR_Node expr = null;
		Type t ;
		switch (root.getType()) {
		case DecafScannerTokenTypes.LOCATION:
			return makeLocation(root);
		case DecafScannerTokenTypes.INT_LITERAL:
			return makeIntLiteral(root);
		case DecafScannerTokenTypes.CHAR_LITERAL:
			return new IR_IntLiteral((long)root.getText().charAt(1));
		case DecafScannerTokenTypes.METHOD_CALL:
			IR_Call ir_call = makeMethodCall(root);
			if(ir_call==null){
				return null;
			}
			if (ir_call.getType() == Type.VOID) {
				System.err.println("Function " + ir_call.getName()
						+ " in expression must return a result");
				printLineCol(System.err, root.getFirstChild());
				valid = false;
				return null;
			}
			return ir_call;
		case DecafScannerTokenTypes.TK_true:
			return new IR_BoolLiteral(true);
		case DecafScannerTokenTypes.TK_false:
			return new IR_BoolLiteral(false);
		case DecafScannerTokenTypes.AT:
			AST idNode = root.getFirstChild();
			String varName = idNode.getText();
			IR_Node ir_node = lookupSymbol(varName, idNode);
			if(ir_node ==null){
				return null;
			}
			t = ir_node.getType();
			if(!isArrType(t)){
				System.err.println("Cannot apply @ to "+varName);
				printLineCol(System.err, idNode);
				valid = false;
				return null;
			}
			IR_FieldDecl arr = (IR_FieldDecl)ir_node;
			return arr.getLength();
		case DecafScannerTokenTypes.MINUS:
			lhs = root.getFirstChild();
			if(lhs.getNextSibling() != null){
				//process only if unary minus
				break;
			}
			if(lhs.getType() == DecafScannerTokenTypes.INT_LITERAL){
				return makeIntLiteral(root);
			}
			expr = makeExpr(lhs);
			if(expr==null){
				return null;
			}
			if(expr.getType() != Type.INT){
				System.err.println("Can only negate integers.");
				printLineCol(System.err, root);
				valid = false;
				return null;
			}
			return new IR_Negate(expr);
		case DecafScannerTokenTypes.BANG:
			lhs = root.getFirstChild();
			expr = null;
			expr = makeExpr(lhs);
			if(expr==null){
				return null;
			}
			if(expr.getType() != Type.BOOL){
				System.err.println("Can only apply NOT to booleans.");
				printLineCol(System.err, root);
				valid = false;
				return null;
			}
			return new IR_Not(expr);
		case DecafScannerTokenTypes.QUESTION:
			return makeQuestion(root);
		default:
			break;
		}
		
		Ops op = getOp(root.getType());
		lhs = root.getFirstChild();
		IR_Node lexp = makeExpr(lhs);
		if(lexp == null){
			return null;
		}
		AST rhs = lhs.getNextSibling();
		IR_Node rexp = makeExpr(rhs);
		if(rexp==null){
			return null;
		}
		if(lexp.getType() != rexp.getType()){
			System.err.println("Binary operands differ.");
			printLineCol(System.err, root);
			valid = false;
			return null;
		}
		if(isArithOp(op)){
			if(lexp.getType()!=Type.INT){
				System.err.println("Arithmetic operators only takes integers.");
				printLineCol(System.err, root);
				valid = false;
				return null;
			}
			expr = new IR_ArithOp(lexp, rexp, op);
		}else if(isCompareOp(op)){
			if(lexp.getType()!=Type.INT){
				System.err.println("Comparison operators only takes integers.");
				printLineCol(System.err, root);
				valid = false;
				return null;
			}
			expr = new IR_CompareOp(lexp, rexp, op);			
		}else if(isEqOp(op)){
			if(lexp.getType()!=Type.INT && lexp.getType()!=Type.BOOL){
				System.err.println("Equality operators only takes integers or booleans.");
				printLineCol(System.err, root);
				valid = false;
				return null;
			}
			expr = new IR_EqOp(lexp, rexp, op);			
		}else if(isCondOp(op)){
			if(lexp.getType()!=Type.BOOL){
				System.err.println("Conditional operators only takes booleans.");
				printLineCol(System.err, root);
				valid = false;
				return null;
			}
			expr = new IR_CondOp(lexp, rexp, op);			
		}
		
		return expr;
	}

	private IR_Ternary makeQuestion(AST root){
		Type t;
		AST condNode = root.getFirstChild();
		IR_Node cond = makeExpr(condNode);
		if(cond == null){
			return null;
		}
		if(cond.getType()!=Type.BOOL){
			System.err.println("? needs a boolean condition.");
			printLineCol(System.err, root);
			valid = false;
			return null;
		}
		AST valNode = condNode.getNextSibling();
		IR_Node trueExpr = makeExpr(valNode);
		if(trueExpr == null){
			return null;
		}
		valNode = valNode.getNextSibling();
		IR_Node falseExpr = makeExpr(valNode);
		if(falseExpr==null){
			return null;
		}
		t = trueExpr.getType();
		if(t!=Type.INT && t!=Type.BOOL){
			System.err.println("'?' can only evaluate to int or bool.");
			printLineCol(System.err, root);
			valid = false;
			return null;
		}
		if(t!=falseExpr.getType()){
			System.err.println("Alternates of '?' have different types.");
			printLineCol(System.err, root);
			valid = false;
			return null;
		}
		return new IR_Ternary(cond, trueExpr, falseExpr);
	}
	
	private IR_Call makeMethodCall(AST root){
		AST idNode = root.getFirstChild();
		String funName = idNode.getText();
		IR_Node decl = lookupSymbol(funName, root);
		if(decl == null){
			return null;
		}
		if( decl.getType() != Type.METHOD &&
				decl.getType() != Type.CALLOUT){
			System.err.println(funName + " is not a function");
			printLineCol(System.err, idNode);
			valid = false;
			return null;
		}
		IR_MethodDecl funDecl = (IR_MethodDecl)(decl);
		Type t = funDecl.getRetType();
		
		IR_Call ir_call = new IR_Call(t, funName);
		AST argNode=idNode.getNextSibling();
		
		if(funDecl.getType() == Type.CALLOUT){
			while(argNode !=null){
				if(argNode.getType() == DecafScannerTokenTypes.STRING_LITERAL){
					IR_StringLiteral literal = new IR_StringLiteral(argNode.getText());
					ir_call.addArg(literal);
				}else{
					IR_Node expr = makeExpr(argNode);
					if(expr==null){
						return null;
					}
					ir_call.addArg(expr);
				}
				argNode = argNode.getNextSibling();
			}
			return ir_call;
		}
		
		for(int ii = 0; ii<funDecl.getNumArgs(); ii++){
			if(argNode == null){
				System.err.println("Too few arguments for "+funName);
				printLineCol(System.err,idNode);
				valid = false;
				return null;
			}
			Type expected = funDecl.getArgType(ii);
			IR_Node expr = makeExpr(argNode);
			if(expr == null){
				return null;
			}
			if (expr.getType() == expected) {
				ir_call.addArg(expr);
			} else {
				System.err.println("Wrong argument for function "+funName);
				printLineCol(System.err, argNode);
				valid = false;
				return null;
			}	
			argNode = argNode.getNextSibling();
		}
		
		if(argNode!=null){
			System.err.println("Too many arguments for "+funName);
			printLineCol(System.err, argNode);
			valid = false;
			return null;
		}
		
		return ir_call;
	}
	
	private ArrayList<IR_Node> makeFieldDeclList(AST root) {
		ArrayList<IR_Node> decls = new ArrayList<IR_Node>();
		AST typeNode = root.getFirstChild();
		Type t = tokenToType(typeNode.getType());
		AST declNode = typeNode.getNextSibling();
		while (declNode != null) {
			boolean hasLen = false;
			AST lenNode = declNode.getNextSibling();
			if (lenNode != null
					&& lenNode.getType() == DecafScannerTokenTypes.INT_LITERAL) {
				hasLen = true;
			} else {
				lenNode = null;
			}
			IR_Node decl = makeFieldDecl(t, declNode, lenNode);
			if (decl != null) {
				decls.add(decl);
			}
			if (hasLen) {
				declNode = declNode.getNextSibling();
			}
			declNode = declNode.getNextSibling();
		}

		return decls;
	}

	private IR_Node makeFieldDecl(Type type, AST root, AST len) {
		String name = root.getText();
		if (checkDupSymbol(name, root)) {
			valid = false;
			return null;
		}
		IR_IntLiteral literal = null;
		if (len != null) {
			literal = makeIntLiteral(len);
			if (literal == null) {
				return null;
			} else if (literal.getValue() <= 0) {
				System.err.println("Array must have positive length.");
				printLineCol(System.err, root);
				valid = false;
				return null;
			} else {
				type = toArrayType(type);
			}
		}

		IR_Node r = new IR_FieldDecl(type, name, literal);
		symbols.put(name, r);
		return r;
	}

	private IR_IntLiteral makeIntLiteral(AST root) {
		boolean negate = false;
		String text = null;
		if (root.getType() == DecafScannerTokenTypes.MINUS) {
			negate = true;
			text = root.getFirstChild().getText();
		} else {
			text = root.getText();
		}
		long val[] = new long[1];
		if (checkIntSize(text, negate, val)) {
			return new IR_IntLiteral(val[0]);
		}
		valid = false;
		System.err.println("Invalid Integer range.");
		printLineCol(System.err, root);
		return null;
	}

	private IR_Node makeCalloutDecl(AST root) {
		AST id = root.getFirstChild();
		String name = id.getText();
		if (checkDupSymbol(name, root)) {
			valid = false;
			return null;
		}
		// callout returns int according to spec
		IR_MethodDecl node = new IR_MethodDecl(Type.INT, name, true);
		symbols.put(name, node);
		return node;
	}

	private boolean checkIntSize(String text, boolean negate, long val[]) {
		BigInteger largest_allowed = new BigInteger(
				Long.toString(Long.MAX_VALUE));
		BigInteger smallest_allowed = new BigInteger(
				Long.toString(Long.MIN_VALUE));
		BigInteger checking;
		if (text.startsWith("0x")) {
			if (text.length() > 18) {
				// more than 16 hex digits long.
				return false;
			}
			checking = new BigInteger(text.substring(2), 16);
			checking = checking.compareTo(largest_allowed) > 0 ? checking
					.subtract((new BigInteger("2").pow(64))) : checking;
		} else {
			checking = new BigInteger(text);
		}

		if (negate) {
			checking = checking.negate();
		}

		if (checking.compareTo(largest_allowed) <= 0
				&& checking.compareTo(smallest_allowed) >= 0) {
			val[0] = checking.longValue();
			return true;
		}
		return false;
	}

	private static void printLineCol(PrintStream ps, AST ast) {
		ps.println("Line: " + ast.getLine() + " column " + ast.getColumn());
	}
	
	boolean checkDupSymbol(String name, AST node) {
		if (symbols.lookupLocal(name) != null) {
			valid = false;
			System.err.println(name + " already declared in the same scope.");
			printLineCol(System.err, node);
			return true;
		}
		return false;
	}

	IR_Node lookupSymbol(String name, AST node) {
		IR_Node ir_node = symbols.lookup(name);
		if (ir_node == null) {
			valid = false;
			System.err.println(name + " undeclared.");
			printLineCol(System.err, node);
			return null;
		}
		return ir_node;
	}

	
	private boolean isArrType(Type t) {
		return t == Type.BOOLARR || t == Type.INTARR;
	}
	
	private Type toEntryType(Type t) {
		switch (t) {
		case BOOLARR:
			return Type.BOOL;
		case INTARR:
			return Type.INT;
		default:
			System.out.println("Can't make an array of type " + t);
			break;
		}
		return Type.VOID;
	}
	
	private Type toArrayType(Type t) {
		switch (t) {
		case BOOL:
			return Type.BOOLARR;
		case INT:
			return Type.INTARR;
		default:
			System.out.println("Can't make an array of type " + t);
			break;
		}
		return Type.VOID;
	}

	private Type tokenToType(int token) {
		Type t = Type.VOID;
		switch (token) {
		case DecafScannerTokenTypes.TK_int:
			t = Type.INT;
			break;
		case DecafScannerTokenTypes.TK_boolean:
			t = Type.BOOL;
			break;
		}
		return t;
	}
	
	Ops getOp(int type) {
		switch (type) {
		case DecafScannerTokenTypes.ASSIGN:
			return Ops.ASSIGN;
		case DecafScannerTokenTypes.ASSIGN_PLUS:
			return Ops.ASSIGN_PLUS;
		case DecafScannerTokenTypes.ASSIGN_MINUS:
			return Ops.ASSIGN_MINUS;
		case DecafScannerTokenTypes.PLUS:
			return Ops.PLUS;
		case DecafScannerTokenTypes.MINUS:
			return Ops.MINUS;
		case DecafScannerTokenTypes.TIMES:
			return Ops.TIMES;
		case DecafScannerTokenTypes.DIVIDE:
			return Ops.DIVIDE;
		case DecafScannerTokenTypes.MOD:
			return Ops.MOD;
		case DecafScannerTokenTypes.LT:
			return Ops.LT;
		case DecafScannerTokenTypes.LTE:
			return Ops.LTE;
		case DecafScannerTokenTypes.GT:
			return Ops.GT;
		case DecafScannerTokenTypes.GTE:
			return Ops.GTE;
		case DecafScannerTokenTypes.EQUALS:
			return Ops.EQUALS;
		case DecafScannerTokenTypes.NOT_EQUALS:
			return Ops.NOT_EQUALS;
		case DecafScannerTokenTypes.AND:
			return Ops.AND;
		case DecafScannerTokenTypes.OR:
			return Ops.OR;
		}
		return null;
	}

	private boolean isArithOp(Ops op){
		switch(op){
		case PLUS:
		case MINUS:
		case TIMES:
		case DIVIDE:
		case MOD:
		return true;
		default:
			break;
		}
		return false;
	}
	
	private boolean isCompareOp(Ops op){
		switch(op){
		case LT:
		case LTE:
		case GT:
		case GTE:
		return true;
		default:
			break;
		}
		return false;		
	}
	
	private boolean isEqOp(Ops op){
		switch(op){
		case EQUALS:
		case NOT_EQUALS:
		return true;
		default:
			break;
		}
		return false;		
	}
	
	private boolean isCondOp(Ops op){
		switch(op){
		case AND:
		case OR:
		return true;
		default:
			break;
		}
		return false;		
	}

}
