package libbun.lang.c;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.literal.BunIntNode;
import libbun.parser.BToken;
import libbun.parser.LibBunTypeChecker;
import libbun.type.BType;

public class CharNode extends SyntaxSugarNode {
	private final char Value;

	public CharNode(BNode ParentNode, BToken Token, String Value) {
		super(ParentNode, 1);
		this.SourceToken = Token;
		this.Value = Value.charAt(0);
	}

	@Override
	public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
	}

	@Override
	public DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer) {
		BNode Node = new BunIntNode(this.ParentNode, this.SourceToken, this.Value);
		return new DesugarNode(this, Node);
	}
}
