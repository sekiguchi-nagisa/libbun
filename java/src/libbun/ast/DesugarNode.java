package libbun.ast;

import libbun.parser.LibBunTypeChecker;
import libbun.parser.LibBunVisitor;
import libbun.type.BType;
import libbun.util.BField;
import libbun.util.Var;

public class DesugarNode extends SyntaxSugarNode {
	//	public final static int _NewNode = 0;
	@BField public BNode OriginalNode;

	public DesugarNode(BNode OriginalNode, BNode DesugardNode) {
		super(OriginalNode.ParentNode, 1);
		this.OriginalNode = OriginalNode;
		this.SetChild(OriginalNode, BNode._EnforcedParent);
		this.SetNode(0, DesugardNode);
		this.Type = OriginalNode.Type;
	}

	public DesugarNode(BNode OriginalNode, BNode[] DesugarNodes) {
		super(OriginalNode.ParentNode, DesugarNodes.length);
		this.OriginalNode = OriginalNode;
		@Var int i = 0;
		while(i < DesugarNodes.length) {
			this.SetNode(i, DesugarNodes[i]);
			i = i + 1;
		}
		this.Type = OriginalNode.Type;
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

	@Override public final void Accept(LibBunVisitor Visitor) {
		Visitor.VisitDesugarNode(this);
	}

	public final boolean IsExpression() {
		return this.GetAstSize() == 1;
	}

	@Override public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		if(this.GetAstSize() != 1) {
			@Var int i = 0;
			while(i < this.GetAstSize()) {
				TypeChecker.CheckTypeAt(this, i, BType.VoidType);
				i = i + 1;
			}
			this.Type = BType.VoidType;
		}
		else {
			TypeChecker.CheckTypeAt(this, 0, BType.VarType);
			this.Type = this.AST[0].Type;
		}
	}

	@Override public DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer) {
		return this;
	}

}
