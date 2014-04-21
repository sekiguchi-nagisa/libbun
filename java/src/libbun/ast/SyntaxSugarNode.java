package libbun.ast;

import libbun.parser.LibBunTypeChecker;
import libbun.parser.LibBunVisitor;


public abstract class SyntaxSugarNode extends BNode {

	public SyntaxSugarNode(BNode ParentNode, int Size) {
		super(ParentNode, Size);
	}

	@Override public final void Accept(LibBunVisitor Visitor) {
		Visitor.VisitSyntaxSugarNode(this);
	}

	public abstract DesugarNode DeSugar(LibBunTypeChecker TypeChekcer);

}
