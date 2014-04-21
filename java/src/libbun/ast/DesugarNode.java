package libbun.ast;

import libbun.parser.LibBunTypeChecker;
import libbun.util.BField;
import libbun.util.Var;

public class DesugarNode extends SyntaxSugarNode {
	//	public final static int _NewNode = 0;
	@BField BNode OriginalNode;

	public DesugarNode(BNode OriginalNode, BNode DesugardNode) {
		super(OriginalNode.ParentNode, 1);
		this.OriginalNode = OriginalNode;
		this.SetChild(OriginalNode, BNode._EnforcedParent);
		this.SetNode(0, DesugardNode);
	}

	public DesugarNode(BNode OriginalNode, BNode[] DesugarNodes) {
		super(OriginalNode.ParentNode, DesugarNodes.length);
		this.OriginalNode = OriginalNode;
		if(OriginalNode != null) {
			this.SetChild(OriginalNode, BNode._EnforcedParent);
		}
		@Var int i = 0;
		while(i < DesugarNodes.length) {
			this.SetNode(i, DesugarNodes[i]);
			i = i + 1;
		}
	}

	private DesugarNode(BNode ParentNode, BNode OriginalNode, int Size) {
		super(ParentNode, Size);
		this.OriginalNode = OriginalNode;
	}

	@Override public BNode Dup(boolean TypedClone, BNode ParentNode) {
		if(TypedClone) {
			return this.DupField(TypedClone, new DesugarNode(ParentNode, this.OriginalNode.Dup(TypedClone, ParentNode), this.AST.length));
		}
		else {
			return this.OriginalNode.Dup(TypedClone, ParentNode);
		}
	}

	@Override public DesugarNode DeSugar(LibBunTypeChecker TypeChekcer) {
		return this;
	}

}
