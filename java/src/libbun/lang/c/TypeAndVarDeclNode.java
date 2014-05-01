package libbun.lang.c;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.parser.LibBunTypeChecker;
import libbun.type.BType;

public class TypeAndVarDeclNode extends SyntaxSugarNode {
	public final static int _TypeDecl = 0;
	public final static int _VarDecl = 1;

	public TypeAndVarDeclNode(BNode ParentNode) {
		super(ParentNode, 2);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		// TODO
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer) {
		return null;
	}
}
