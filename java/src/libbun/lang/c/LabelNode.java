package libbun.lang.c;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.parser.LibBunTypeChecker;
import libbun.type.BType;

public class LabelNode extends SyntaxSugarNode {
	public final static int _Label = 0;

	public LabelNode(BNode ParentNode) {
		super(ParentNode, 1);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		//TODO:
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer) {
		// TODO:
		return null;
	}
}
