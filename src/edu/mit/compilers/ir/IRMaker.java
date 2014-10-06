package edu.mit.compilers.ir;

import edu.mit.compilers.grammar.DecafScannerTokenTypes;
import edu.mit.compilers.ir.IR_Literal.IR_BoolLiteral;
import edu.mit.compilers.ir.IR_Literal.IR_IntLiteral;
import edu.mit.compilers.ir.IR_Literal.IR_StringLiteral;
import antlr.collections.AST;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;

public class IRMaker {
	private SymbolTable symbols;
	private Type _returnType;
	private boolean valid = true;

	private static void printLineCol(PrintStream ps, AST ast) {
		ps.println("Line: " + ast.getLine() + " column " + ast.getColumn());
	}

	boolean checkSymbolDup(String name, AST node) {
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

	public IRMaker() {
		symbols = new SymbolTable();
	}

	public IR_Node make(AST ast) {
		valid = true;
		symbols.clear();
		// global scope
		symbols.incScope();

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

		return root;
	}

	private IR_Node makeMethodDecl(AST root) {
		AST typeNode = root.getFirstChild();
		Type t = tokenToType(typeNode.getType());
		AST idNode = typeNode.getNextSibling();
		String funName = idNode.getText();
		_returnType = t;
		IR_MethodDecl ir_method = new IR_MethodDecl(t, funName);
		symbols.put(funName, ir_method);
		// local and func param the same scope
		symbols.incScope();

		AST paramNode = idNode.getNextSibling();
		while (paramNode.getType() != DecafScannerTokenTypes.BLOCK) {
			Type argt = tokenToType(paramNode.getType());
			paramNode = paramNode.getNextSibling();
			ir_method.addArg(argt);
			String paramName = paramNode.getText();
			if (checkSymbolDup(paramName, paramNode)) {
				paramNode = paramNode.getNextSibling();
				continue;
			}
			IR_FieldDecl ir_param = new IR_FieldDecl(argt, paramName);
			symbols.put(paramName, ir_param);
			paramNode = paramNode.getNextSibling();
		}

		IR_Seq body = makeBlock(paramNode);
		ir_method.setBody(body);
		symbols.decScope();
		return ir_method;
	}

	IR_Seq makeBlock(AST root) {
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
			}
			child = child.getNextSibling();
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
				System.err.println("Cannot assign to" + ir_var.getName());
				printLineCol(System.err, root);
				valid = false;
				return null;
			}

			statement = new IR_Assign(ir_var, expr, getOpString(tokenType));
			break;
		case DecafScannerTokenTypes.METHOD_CALL:
			statement = makeMethodCall(root);
		}
		return statement;
	}

	String getOpString(int type) {
		switch (type) {
		case DecafScannerTokenTypes.ASSIGN:
			return "=";
		case DecafScannerTokenTypes.ASSIGN_PLUS:
			return "+=";
		case DecafScannerTokenTypes.ASSIGN_MINUS:
			return "-=";

		}
		return null;
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
		switch (root.getType()) {
		case DecafScannerTokenTypes.LOCATION:
			return makeLocation(root);
		case DecafScannerTokenTypes.INT_LITERAL:
			return makeIntLiteral(root);
		case DecafScannerTokenTypes.METHOD_CALL:
			IR_Call ir_call = makeMethodCall(root);
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
			Type t = ir_node.getType();
			if(!isArrType(t)){
				System.err.println("Cannot apply @ to "+varName);
				printLineCol(System.err, idNode);
				valid = false;
				return null;
			}
			IR_FieldDecl arr = (IR_FieldDecl)ir_node;
			return arr.len;
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
				printLineCol(System.err, lhs);
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
				System.err.println("Can apply NOT to booleans.");
				printLineCol(System.err, lhs);
				valid = false;
				return null;
			}
			return new IR_Not(expr);
		case DecafScannerTokenTypes.QUESTION:
			
		}
		
		
		
		return null;
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
		if (checkSymbolDup(name, root)) {
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
		System.err.println("Invalid Integer range.");
		printLineCol(System.err, root);
		return null;
	}

	private IR_Node makeCalloutDecl(AST root) {
		AST id = root.getFirstChild();
		String name = id.getText();
		if (checkSymbolDup(name, root)) {
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
}
