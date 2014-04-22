package libbun.parser.ssa;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.GroupNode;
import libbun.ast.LocalDefinedNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.binary.AssignNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunInstanceOfNode;
import libbun.ast.decl.BunClassNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.ast.decl.TopLevelNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.BunFormNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetFieldNode;
import libbun.ast.expression.GetIndexNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.expression.MethodCallNode;
import libbun.ast.expression.NewObjectNode;
import libbun.ast.literal.BunArrayLiteralNode;
import libbun.ast.literal.BunAsmNode;
import libbun.ast.literal.BunMapLiteralNode;
import libbun.ast.literal.LiteralNode;
import libbun.ast.statement.BunBreakNode;
import libbun.ast.statement.BunIfNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.statement.BunThrowNode;
import libbun.ast.statement.BunTryNode;
import libbun.ast.statement.BunWhileNode;
import libbun.ast.unary.BunCastNode;
import libbun.ast.unary.UnaryOperatorNode;
import libbun.parser.LibBunVisitor;
import libbun.util.Var;

public class ZASTTransformer extends LibBunVisitor {
	private BNode TransformedValue;
	public ZASTTransformer() {
		this.TransformedValue = null;
	}

	protected void VisitBefore(BNode Node, int Index) {
	}

	protected void VisitAfter(BNode Node, int Index) {
	}

	protected void Transform(BNode Node, int Index) {
		BNode LastTransformed = this.TransformedValue;
		this.TransformedValue = Node.AST[Index];
		this.VisitBefore(Node, Index);
		Node.AST[Index].Accept(this);
		Node.SetNode(Index, this.TransformedValue);
		this.VisitAfter(Node, Index);
		this.TransformedValue = LastTransformed;
	}

	protected void GenerateListNode(AbstractListNode Node) {
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			this.Transform(Node, i);
			i = i + 1;
		}
	}

	@Override
	public void VisitLiteralNode(LiteralNode Node) {
		/* do nothing */
	}

	@Override
	public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		this.GenerateListNode(Node);
	}

	@Override
	public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		this.GenerateListNode(Node);
	}

	//	@Override
	//	public void VisitNewArrayNode(ZNewArrayNode Node) {
	//		this.GenerateListNode(Node);
	//	}

	@Override
	public void VisitNewObjectNode(NewObjectNode Node) {
		this.GenerateListNode(Node);
	}

	@Override
	public void VisitGetNameNode(GetNameNode Node) {
		/* do nothing */
	}

	@Override
	public void VisitAssignNode(AssignNode Node) {
		this.Transform(Node, BinaryOperatorNode._Left);
		this.Transform(Node, BinaryOperatorNode._Right);
	}

	@Override
	public void VisitGroupNode(GroupNode Node) {
		this.Transform(Node, GroupNode._Expr);
	}

	@Override
	public void VisitGetFieldNode(GetFieldNode Node) {
		this.Transform(Node, GetFieldNode._Recv);
	}

	@Override
	public void VisitGetIndexNode(GetIndexNode Node) {
		this.Transform(Node, GetIndexNode._Recv);
		this.Transform(Node, GetIndexNode._Index);
	}

	@Override
	public void VisitMethodCallNode(MethodCallNode Node) {
		this.Transform(Node, MethodCallNode._Recv);
		this.GenerateListNode(Node);
	}

	@Override
	public void VisitFuncCallNode(FuncCallNode Node) {
		this.Transform(Node, FuncCallNode._Functor);
		this.GenerateListNode(Node);
	}

	@Override
	public void VisitUnaryNode(UnaryOperatorNode Node) {
		this.Transform(Node, UnaryOperatorNode._Recv);
	}

	@Override
	public void VisitCastNode(BunCastNode Node) {
		this.Transform(Node, BunCastNode._Expr);
	}

	@Override
	public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.Transform(Node, BunInstanceOfNode._Left);
	}

	@Override
	public void VisitBinaryNode(BinaryOperatorNode Node) {
		this.Transform(Node, BinaryOperatorNode._Left);
		this.Transform(Node, BinaryOperatorNode._Right);
	}

	@Override
	public void VisitBlockNode(BunBlockNode Node) {
		this.GenerateListNode(Node);
	}

	@Override
	public void VisitVarBlockNode(BunVarBlockNode Node) {
		this.Transform(Node, BunLetVarNode._InitValue);
		this.GenerateListNode(Node);
	}

	@Override
	public void VisitIfNode(BunIfNode Node) {
		this.Transform(Node, BunIfNode._Cond);
		this.Transform(Node, BunIfNode._Then);
		if(Node.HasElseNode()) {
			this.Transform(Node, BunIfNode._Else);
		}
	}

	@Override
	public void VisitReturnNode(BunReturnNode Node) {
		if(Node.ExprNode() != null) {
			this.Transform(Node, BunReturnNode._Expr);
		}
	}

	@Override
	public void VisitWhileNode(BunWhileNode Node) {
		this.Transform(Node, BunWhileNode._Cond);
		this.Transform(Node, BunWhileNode._Block);
	}

	@Override
	public void VisitBreakNode(BunBreakNode Node) {
		/* do nothing */
	}

	@Override
	public void VisitThrowNode(BunThrowNode Node) {
		this.Transform(Node, BunThrowNode._Expr);
	}

	@Override
	public void VisitTryNode(BunTryNode Node) {
		this.Transform(Node, BunTryNode._Try);
		this.Transform(Node, BunTryNode._Catch);
		this.Transform(Node, BunTryNode._Finally);
	}

	//	public void VisitCatchNode(ZCatchNode Node) {
	//		this.Transform(Node, ZCatchNode._Block);
	//	}

	@Override
	public void VisitLetNode(BunLetVarNode Node) {
		this.Transform(Node, BunLetVarNode._InitValue);
	}

	@Override
	public void VisitFunctionNode(BunFunctionNode Node) {
		this.Transform(Node, BunFunctionNode._Block);
	}

	@Override
	public void VisitClassNode(BunClassNode Node) {
		this.GenerateListNode(Node);
	}

	@Override
	public void VisitErrorNode(ErrorNode Node) {
		/* do nothing */
	}

	@Override
	public void VisitFormNode(BunFormNode Node) {
		this.GenerateListNode(Node);
	}

	@Override
	public void VisitAsmNode(BunAsmNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitTopLevelNode(TopLevelNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitSyntaxSugarNode(SyntaxSugarNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitLocalDefinedNode(LocalDefinedNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitDesugarNode(DesugarNode Node) {
		// TODO Auto-generated method stub
	}


}
