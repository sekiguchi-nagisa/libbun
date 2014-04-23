package libbun.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.statement.BunWhileNode;
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

	@Override public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		TypeChecker.CheckTypeAt(this, BunWhileNode._Cond, BType.BooleanType);
		TypeChecker.CheckTypeAt(this, BunWhileNode._Block, BType.VoidType);
		if(this.HasNextNode()) {
			TypeChecker.CheckTypeAt(this, BunWhileNode._Next, BType.VoidType);
			this.BlockNode().Append(this.NextNode());
		}
		TypeChecker.ReturnTypeNode(this, BType.VoidType);
	}

	/**
	do {
        A;
        continue;
	}while(EXPR);
	==
	while(true) {
		A;
		if(!EXPR) {
	        break;
	    }
	}
	 **/


	@Override public DesugarNode PerformDesugar(LibBunTypeChecker TypeChekcer) {
		@Var String SugarCode = ""     +
				"while(true) {\n" +
				"  Block::X;\n"          +
				"  if(!Expr::Y) {\n"    +
				"    break;\n"    +
				"  }\n"           +
				"}";
		@Var BNode ParentNode = this.ParentNode;
		@Var BNode WhileNode = ParentNode.ParseExpression(SugarCode);
		WhileNode.ReplaceNode("Block::X", this.BlockNode());
		WhileNode.ReplaceNode("Expr::Y", this.CondNode());
		System.out.println("WhileNode: " + WhileNode);
		return new DesugarNode(this, WhileNode);
	}

}
