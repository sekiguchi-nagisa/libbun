package libbun.ast.statement;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.parser.LibBunTypeChecker;
import libbun.util.Var;

public final class BunForInNode extends SyntaxSugarNode {
	public final static int _Var   = 0;
	public final static int _List  = 1;
	public final static int _Block = 2;

	public BunForInNode(BNode ParentNode) {
		super(ParentNode, 3);
	}

	@Override public BNode Dup(boolean TypedClone, BNode ParentNode) {
		return this.DupField(TypedClone, new BunForInNode(ParentNode));
	}

	public final BNode VarNode() {
		return this.AST[BunForInNode._Var];
	}

	public final BNode ListNode() {
		return this.AST[BunForInNode._List];
	}

	public final BunBlockNode BlockNode() {
		@Var BNode BlockNode = this.AST[BunForInNode._Block];
		if(BlockNode instanceof BunBlockNode) {
			return (BunBlockNode)BlockNode;
		}
		assert(BlockNode == null); // this must not happen
		return null;
	}

	@Override public DesugarNode DeSugar(LibBunTypeChecker TypeChekcer) {
		// TODO Auto-generated method stub
		return null;
	}
}