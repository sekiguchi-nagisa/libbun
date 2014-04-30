package libbun.encode.devel;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.GroupNode;
import libbun.ast.binary.AssignNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.binary.BunAndNode;
import libbun.ast.binary.BunBitwiseAndNode;
import libbun.ast.binary.BunBitwiseOrNode;
import libbun.ast.binary.BunBitwiseXorNode;
import libbun.ast.binary.BunDivNode;
import libbun.ast.binary.BunEqualsNode;
import libbun.ast.binary.BunGreaterThanEqualsNode;
import libbun.ast.binary.BunGreaterThanNode;
import libbun.ast.binary.BunInstanceOfNode;
import libbun.ast.binary.BunLeftShiftNode;
import libbun.ast.binary.BunLessThanEqualsNode;
import libbun.ast.binary.BunLessThanNode;
import libbun.ast.binary.BunModNode;
import libbun.ast.binary.BunMulNode;
import libbun.ast.binary.BunNotEqualsNode;
import libbun.ast.binary.BunOrNode;
import libbun.ast.binary.BunRightShiftNode;
import libbun.ast.binary.BunSubNode;
import libbun.ast.decl.BunClassNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.BunFuncNameNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetFieldNode;
import libbun.ast.expression.GetIndexNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.expression.MethodCallNode;
import libbun.ast.expression.NewObjectNode;
import libbun.ast.literal.BunArrayLiteralNode;
import libbun.ast.literal.BunBooleanNode;
import libbun.ast.literal.BunFloatNode;
import libbun.ast.literal.BunIntNode;
import libbun.ast.literal.BunMapLiteralNode;
import libbun.ast.literal.BunNullNode;
import libbun.ast.literal.BunStringNode;
import libbun.ast.literal.ConstNode;
import libbun.ast.statement.BunBreakNode;
import libbun.ast.statement.BunIfNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.statement.BunThrowNode;
import libbun.ast.statement.BunTryNode;
import libbun.ast.statement.BunWhileNode;
import libbun.ast.unary.BunCastNode;
import libbun.ast.unary.BunComplementNode;
import libbun.ast.unary.BunMinusNode;
import libbun.ast.unary.BunNotNode;
import libbun.ast.unary.BunPlusNode;
import libbun.ast.unary.UnaryOperatorNode;
import libbun.encode.LibBunSourceGenerator;
import libbun.parser.LibBunLangInfo;
import libbun.type.BType;
import libbun.util.Var;

public class SMLSharpGenerator extends LibBunSourceGenerator {

	public SMLSharpGenerator() {
		super(new LibBunLangInfo("SML#-2.0", "sml"));
	}

	@Override
	protected void GenerateStatementEnd(BNode Node) {
	}

	@Override
	protected void GenerateImportLibrary(String LibName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitNullNode(BunNullNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitBooleanNode(BunBooleanNode Node) {
		if (Node.BooleanValue) {
			this.Source.Append("true");
		} else {
			this.Source.Append("false");
		}
	}

	@Override
	public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(String.valueOf(Node.IntValue));
	}

	@Override
	public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(String.valueOf(Node.FloatValue));
	}

	@Override
	public void VisitStringNode(BunStringNode Node) {
		this.Source.AppendQuotedText(Node.StringValue);
	}

	@Override
	public void VisitNotNode(BunNotNode Node) {
		this.Source.Append("(Bool.not ");
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitPlusNode(BunPlusNode Node) {
		this.GenerateExpression(Node.RecvNode());
	}

	@Override
	public void VisitMinusNode(BunMinusNode Node) {
		if(Node.RecvNode() instanceof ConstNode) {
			this.Source.Append("~");
			this.GenerateExpression(Node.RecvNode());
		}
		else {
			this.Source.Append("(~ ");
			this.GenerateExpression(Node.RecvNode());
			this.Source.Append(")");
		}
	}

	@Override
	public void VisitComplementNode(BunComplementNode Node) {
		this.Source.Append("(LargeInt.notb ");
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitAndNode(BunAndNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitOrNode(BunOrNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitAddNode(BunAddNode Node) {
		if(Node.Type.IsStringType()) {
			this.Source.Append("(");
			@Var BNode LeftNode = Node.LeftNode();
			if(!LeftNode.Type.IsStringType()) {
				LeftNode = this.TypeChecker.EnforceNodeType(Node.LeftNode(), BType.StringType);
			}
			this.GenerateExpression(LeftNode);
			this.Source.Append(" ^ ");
			@Var BNode RightNode = Node.RightNode();
			if(!RightNode.Type.IsStringType()) {
				RightNode = this.TypeChecker.EnforceNodeType(Node.RightNode(), BType.StringType);
			}
			this.GenerateExpression(RightNode);
			this.Source.Append(")");
		}
		else {
			this.Source.Append("(");
			this.GenerateExpression(Node.LeftNode());
			this.Source.Append(" + ");
			this.GenerateExpression(Node.RightNode());
			this.Source.Append(")");
		}
	}

	@Override
	public void VisitSubNode(BunSubNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" - ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitMulNode(BunMulNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" * ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitDivNode(BunDivNode Node) {
		if(Node.Type.IsIntType()) {
			this.Source.Append("(");
			this.GenerateExpression(Node.LeftNode());
			this.Source.Append(" div ");
			this.GenerateExpression(Node.RightNode());
			this.Source.Append(")");
		}
		else {
			this.Source.Append("(");
			this.GenerateExpression(Node.LeftNode());
			this.Source.Append(" / ");
			this.GenerateExpression(Node.RightNode());
			this.Source.Append(")");
		}
	}

	@Override
	public void VisitModNode(BunModNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" mod ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitLeftShiftNode(BunLeftShiftNode Node) {
		this.Source.Append("(LargeInt.<< (");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(", (Word.fromLargeInt ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")))");
	}

	@Override
	public void VisitRightShiftNode(BunRightShiftNode Node) {
		this.Source.Append("(LargeInt.~>> (");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(", (Word.fromLargeInt ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")))");
	}

	@Override
	public void VisitBitwiseAndNode(BunBitwiseAndNode Node) {
		this.Source.Append("(LargeInt.andb (");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(", ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append("))");
	}

	@Override
	public void VisitBitwiseOrNode(BunBitwiseOrNode Node) {
		this.Source.Append("(LargeInt.orb (");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(", ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append("))");
	}

	@Override
	public void VisitBitwiseXorNode(BunBitwiseXorNode Node) {
		this.Source.Append("(LargeInt.xorb (");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(", ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append("))");
	}

	@Override
	public void VisitEqualsNode(BunEqualsNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" = ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitNotEqualsNode(BunNotEqualsNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" <> ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitLessThanNode(BunLessThanNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" < ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitLessThanEqualsNode(BunLessThanEqualsNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" <= ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitGreaterThanNode(BunGreaterThanNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" > ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" >= ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitGroupNode(GroupNode Node) {
		this.GenerateExpression(Node.ExprNode());
	}

	@Override
	public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitNewObjectNode(NewObjectNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitFuncCallNode(FuncCallNode Node) {
		@Var BunFuncNameNode FuncNameNode = Node.FuncNameNode();
		this.Source.Append("(");
		if(FuncNameNode != null) {
			this.Source.Append(FuncNameNode.GetSignature());
		}
		else {
			this.GenerateExpression(Node.FunctorNode());
		}
		if(Node.GetListSize() == 0) {
			this.Source.Append(" ())");
		}
		else {
			this.GenerateListNode(" ", Node, " ", ")");
		}
	}

	private boolean DoesNeedReference(BNode Node) {
		/* FIXME */
		return !(Node.ParentNode instanceof AssignNode);
	}

	@Override
	public void VisitGetNameNode(GetNameNode Node) {
		if(!Node.ResolvedNode.IsReadOnly() && this.DoesNeedReference(Node)) {
			this.Source.Append("(!", Node.GetUniqueName(this), ")");
		}
		else {
			this.Source.Append(Node.GetUniqueName(this));
		}
	}

	@Override
	public void VisitAssignNode(AssignNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" := ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitGetFieldNode(GetFieldNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitGetIndexNode(GetIndexNode Node) {
		// TODO Auto-generated method stub

	}


	@Override
	public void VisitMethodCallNode(MethodCallNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitUnaryNode(UnaryOperatorNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitCastNode(BunCastNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitBinaryNode(BinaryOperatorNode Node) {
		// TODO Auto-generated method stub

	}

	private void GenerateStmtListNode(BunBlockNode Node) {
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			if(i > 0) {
				this.Source.Append(";");
			}
			@Var BNode SubNode = Node.GetListAt(i);
			this.GenerateStatement(SubNode);
			i = i + 1;
		}
	}

	@Override
	public void VisitBlockNode(BunBlockNode Node) {
		this.Source.AppendWhiteSpace();
		this.Source.OpenIndent("(");
		this.GenerateStmtListNode(Node);
		this.Source.CloseIndent(")");
	}

	@Override
	public void VisitVarBlockNode(BunVarBlockNode Node) {
		@Var BunLetVarNode LetVarNode = Node.VarDeclNode();
		this.Source.Append("let val ", LetVarNode.GetUniqueName(this), " = ref ");
		this.GenerateExpression(LetVarNode.InitValueNode());
		this.Source.Append(" in");
		this.VisitBlockNode(Node);
		this.Source.Append("end");
	}

	@Override
	public void VisitIfNode(BunIfNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitReturnNode(BunReturnNode Node) {
		if (Node.HasReturnExpr() && !Node.ExprNode().Type.IsVoidType()) {
			this.GenerateExpression(Node.ExprNode());
		}
		else {
			this.Source.Append("()");
		}
	}

	@Override
	public void VisitWhileNode(BunWhileNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitBreakNode(BunBreakNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitThrowNode(BunThrowNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitTryNode(BunTryNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			this.Source.Append(Node.GetUniqueName(this));
		}
		else if(Node.IsTopLevel()){
			this.Source.Append("val ", Node.GetUniqueName(this), " = ");
			this.GenerateExpression(Node.InitValueNode());
			this.Source.Append(";");
		}
		else {
			/* this.Source.OpenIndent("let");
			this.Source.AppendNewLine("val ", Node.GetUniqueName(this), " = ");
			this.GenerateExpression(Node.InitValueNode());
			this.Source.Append(";");
			this.Source.CloseIndent("in");
			this.Source.Append("end"); */
		}
	}

	@Override
	public void VisitFunctionNode(BunFunctionNode Node) {
		//if(Node.IsExport) {
		//}
		if(Node.FuncName() != null) {
			this.Source.Append("fun ", Node.ResolvedFuncType.StringfySignature(Node.FuncName()));
		}
		else {
			this.Source.Append("(fn");
		}
		if(Node.GetListSize() == 0) {
			this.Source.Append(" ()");
		}
		else {
			this.GenerateListNode(" ", Node, " ", "");
		}
		if(Node.FuncName() != null) {
			this.Source.Append(" = ");
		}
		else {
			this.Source.Append(" => ");
		}
		this.GenerateExpression(Node.BlockNode());
		if(Node.IsTopLevelDefineFunction()) {
			this.Source.Append(";");
		}
		else if(Node.FuncName() == null) {
			this.Source.Append(")");
		}
	}

	@Override
	public void VisitClassNode(BunClassNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitErrorNode(ErrorNode Node) {
		// TODO Auto-generated method stub

	}

}
