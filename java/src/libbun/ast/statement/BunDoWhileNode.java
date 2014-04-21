package libbun.ast.statement;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.parser.LibBunTypeChecker;
import libbun.type.BType;
import libbun.util.Var;

public final class BunDoWhileNode extends SyntaxSugarNode {
	public final static int _Cond  = 0;
	public final static int _Block = 1;
	public final static int _Next  = 2;   // optional iteration statement

	public BunDoWhileNode(BNode ParentNode) {
		super(ParentNode, 3);
	}

	@Override public BNode Dup(boolean TypedClone, BNode ParentNode) {
		return this.DupField(TypedClone, new BunDoWhileNode(ParentNode));
	}

	public BunDoWhileNode(BNode CondNode, BunBlockNode BlockNode) {
		super(null, 3);
		this.SetNode(BunDoWhileNode._Cond, CondNode);
		this.SetNode(BunDoWhileNode._Block, BlockNode);
		this.Type = BType.VoidType;
	}

	public final BNode CondNode() {
		return this.AST[BunDoWhileNode._Cond];
	}

	public final BunBlockNode BlockNode() {
		@Var BNode BlockNode = this.AST[BunDoWhileNode._Block];
		if(BlockNode instanceof BunBlockNode) {
			return (BunBlockNode)BlockNode;
		}
		assert(BlockNode == null); // this must not happen
		return null;
	}

	public final boolean HasNextNode() {
		return (this.AST[BunDoWhileNode._Next] != null);
	}

	public final BNode NextNode() {
		return this.AST[BunDoWhileNode._Next];
	}

	@Override public DesugarNode DeSugar(LibBunTypeChecker TypeChekcer) {
		return null;
	}

}
