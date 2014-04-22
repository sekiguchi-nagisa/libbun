package libbun.ast;

import libbun.parser.LibBunTypeChecker;
import libbun.parser.LibBunVisitor;
import libbun.type.BType;


public abstract class SyntaxSugarNode extends BNode {

	public SyntaxSugarNode(BNode ParentNode, int Size) {
		super(ParentNode, Size);
	}

	@Override public void Accept(LibBunVisitor Visitor) {
		Visitor.VisitSyntaxSugarNode(this);
	}

	public abstract void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType);
	public abstract DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer);

}
