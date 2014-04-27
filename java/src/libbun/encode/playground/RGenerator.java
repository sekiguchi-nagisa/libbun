package libbun.encode.playground;

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
import libbun.parser.LibBunLogger;
import libbun.type.BType;
import libbun.util.BField;
import libbun.util.Var;
import libbun.util.ZenMethod;

public class RGenerator extends LibBunSourceGenerator {

	@BField private BunFunctionNode MainFuncNode = null;

	public RGenerator() {
		super(new LibBunLangInfo("R-3.0.3", "r"));
		this.Source.OpenIndent("assert <- function(condition) {");
		this.Source.AppendNewLine("if (is.na(condition) || condition == FALSE) {");
		this.Source.OpenIndent();
		this.Source.AppendNewLine("stop(\"assert fail\")");
		this.Source.AppendNewLine("return( 0 )");
		this.Source.CloseIndent();
		this.Source.AppendNewLine("}");
		this.Source.CloseIndent("}\n");
		this.Source.OpenIndent("\"+\" <- function(LHS, RHS) {");
		this.Source.AppendNewLine("if (missing(RHS)) {");
		this.Source.OpenIndent();
		this.Source.AppendNewLine("base::\"+\"(LHS)");
		this.Source.CloseIndent("}");
		this.Source.AppendNewLine("else if (is.character(c(LHS, RHS))) {");
		this.Source.OpenIndent();
		this.Source.AppendNewLine("paste(LHS, RHS, sep = \"\")");
		this.Source.CloseIndent();
		this.Source.AppendNewLine("}");
		this.Source.AppendNewLine("else {");
		this.Source.OpenIndent();
		this.Source.AppendNewLine("base::\"+\"(LHS, RHS)");
		this.Source.CloseIndent("}");
		this.Source.CloseIndent("}\n");
	}
	@Override protected void GenerateImportLibrary(String LibName) {
	}

	@Override @ZenMethod protected void Finish(String FileName) {
		if(this.MainFuncNode != null) {
			this.Source.AppendNewLine(this.MainFuncNode.GetSignature() + "();");
			this.Source.AppendLineFeed();
		}
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.GenerateExpression("[", Node.IndexNode(), " + 1]");
	}

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		BNode Reciever = Node.RecvNode();
		String RecieverName = ((GetNameNode)Reciever).GivenName;
		String MethodName = Node.MethodName();
		if(MethodName.equals("add")) {
			this.GenerateExpression(Reciever);
			this.Source.Append(" <- c(");
			this.GenerateListNode(RecieverName + ", ", Node, ",", ")");
		}
		else if(MethodName.equals("size")) {
			this.Source.Append("length(");
			this.GenerateExpression(Reciever);
			this.Source.Append(")");
		}
		else if(MethodName.equals("insert")) {
			this.GenerateExpression(Reciever);
			this.Source.Append(" <- append(" + RecieverName);
			@Var int i = Node.GetListSize();
			while(i-- != 0) {
				this.Source.Append(", ");
				if(i == 0) {
					this.Source.Append("after=");
				}
				@Var BNode ParamNode = Node.GetListAt(i);
				this.GenerateExpression(ParamNode);
			}
			this.Source.Append(")");
		}
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		this.Source.Append("stop(\"throw\")");
	}

	@Override public void VisitTryNode(BunTryNode Node) {
		this.Source.Append("tryCatch");
		this.Source.OpenIndent("(");
		this.GenerateExpression(Node.TryBlockNode());
		this.Source.Append(",");
		if(Node.HasCatchBlockNode()) {
			@Var String VarName = this.NameUniqueSymbol("e");
			this.Source.AppendNewLine("error = function(e) {");
			this.Source.OpenIndent("");
			this.GenerateStmtList(Node.CatchBlockNode());
			this.Source.CloseIndent("},");
		}
		if(Node.HasFinallyBlockNode()) {
			this.Source.AppendNewLine("finally =");
			this.GenerateExpression(Node.FinallyBlockNode());
			this.Source.CloseIndent(")");
		}
	}

	@Override protected void GenerateTypeName(BType Type) {
	}

	private void GenerateStmtList(BunBlockNode BlockNode) {
		@Var int i = 0;
		while (i < BlockNode.GetListSize()) {
			@Var BNode SubNode = BlockNode.GetListAt(i);
			this.GenerateStatement(SubNode);
			i = i + 1;
		}
		if (i == 0) {
		}
	}

	@Override public void VisitVarBlockNode(BunVarBlockNode Node) {
		@Var BunLetVarNode VarNode = Node.VarDeclNode();
		this.Source.Append(VarNode.GetUniqueName(this), " <- ");
		this.GenerateExpression(VarNode.InitValueNode());
		this.GenerateStmtList(Node);
	}

	@Override public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			this.GenerateTypeName(Node.Type);
			this.Source.Append(Node.GetUniqueName(this));
		}
		else {
			this.Source.Append(Node.GetUniqueName(this));
			this.Source.Append(" <- ");
			this.GenerateExpression(Node.InitValueNode());
		}
	}


	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		if(!Node.IsTopLevelDefineFunction()) {
			@Var String FuncName = Node.GetUniqueName(this);
			this.Source.Append(FuncName);
			this.Source.Append(" <- function");
			this.GenerateListNode("(", Node, ", ", ")");
			this.GenerateExpression(Node.BlockNode());
		}
		else {
			if(Node.FuncName().equals("main")) {
				this.MainFuncNode = Node;
			}
			this.Source.Append(Node.GetSignature());
			this.Source.Append(" <- function");
			this.GenerateListNode("(", Node, ", ", ")");
			this.GenerateExpression(Node.BlockNode());
		}
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
	}

	@Override
	protected void GenerateStatementEnd(BNode Node) {
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		this.Source.Append("NULL");
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		if (Node.BooleanValue) {
			this.Source.Append("TRUE");
		} else {
			this.Source.Append("FALSE");
		}
	}

	@Override public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(""+Node.IntValue);
	}

	@Override public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(""+Node.FloatValue);
	}

	@Override public void VisitStringNode(BunStringNode Node) {
		this.Source.AppendQuotedText(Node.StringValue);
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.Source.Append("!");
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitPlusNode(BunPlusNode Node) {
		this.Source.Append("+");
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitMinusNode(BunMinusNode Node) {
		this.Source.Append("-");
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitComplementNode(BunComplementNode Node) {
		this.Source.Append("~");
		this.GenerateExpression(Node.RecvNode());
	}
	private void GenerateAddOperatorExpression(BinaryOperatorNode Node) {
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		BType LeftType = Node.LeftNode().Type;
		BType RightType = Node.RightNode().Type;
		if(LeftType == BType.StringType || RightType == BType.StringType) {
			if(LeftType != BType.StringType) {
				this.Source.Append("as.character(");
				this.GenerateExpression(Node.LeftNode());
				this.Source.Append(")");
			}
			else {
				this.GenerateExpression(Node.LeftNode());
			}
			this.Source.Append(" + ");
			if(RightType != BType.StringType) {
				this.Source.Append("as.character(");
				this.GenerateExpression(Node.RightNode());
				this.Source.Append(")");
			}
			else {
				this.GenerateExpression(Node.RightNode());
			}
		} else {
			this.GenerateExpression(Node.LeftNode());
			this.Source.AppendWhiteSpace("+", " ");
			this.GenerateExpression(Node.RightNode());
		}
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
	}

	private void GenerateBinaryOperatorExpression(BinaryOperatorNode Node, String Operator) {
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		this.GenerateExpression(Node.LeftNode());
		this.Source.AppendWhiteSpace(Operator, " ");
		this.GenerateExpression(Node.RightNode());
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "&&");
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "||");
	}

	@Override public void VisitAddNode(BunAddNode Node) {
//		this.GenerateBinaryOperatorExpression(Node, "+");
		this.GenerateAddOperatorExpression(Node);
	}

	@Override public void VisitSubNode(BunSubNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "-");
	}

	@Override public void VisitMulNode(BunMulNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "*");
	}

	@Override public void VisitDivNode(BunDivNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "%/%");
	}

	@Override public void VisitModNode(BunModNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "%%");
	}

	@Override public void VisitLeftShiftNode(BunLeftShiftNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "<<");
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode Node) {
		this.GenerateBinaryOperatorExpression(Node, ">>");
	}

	@Override public void VisitBitwiseAndNode(BunBitwiseAndNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "&");
	}

	@Override public void VisitBitwiseOrNode(BunBitwiseOrNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "|");
	}

	@Override public void VisitBitwiseXorNode(BunBitwiseXorNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "^");
	}

	@Override public void VisitEqualsNode(BunEqualsNode Node) {
		if(Node.RightNode() instanceof BunNullNode) {
			this.Source.Append("is.null(");
			this.GenerateExpression(Node.LeftNode());
			this.Source.Append(")");
		}
		else {
			this.GenerateBinaryOperatorExpression(Node, "==");
		}
	}

	@Override public void VisitNotEqualsNode(BunNotEqualsNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "!=");
	}

	@Override public void VisitLessThanNode(BunLessThanNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "<");
	}

	@Override public void VisitLessThanEqualsNode(BunLessThanEqualsNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "<=");
	}

	@Override public void VisitGreaterThanNode(BunGreaterThanNode Node) {
		this.GenerateBinaryOperatorExpression(Node, ">");
	}

	@Override public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode Node) {
		this.GenerateBinaryOperatorExpression(Node, ">=");
	}

	@Override public void VisitGroupNode(GroupNode Node) {
		this.GenerateExpression("(", Node.ExprNode(), ")");
	}

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		this.GenerateListNode("c(", Node, ",", ")");
	}

	protected final void GenerateFuncName(BunFuncNameNode Node) {
		if(this.LangInfo.AllowFunctionOverloading) {
			this.Source.Append(Node.FuncName);
		}
		else {
			this.Source.Append(Node.GetSignature());
		}
	}

	@Override
	public void VisitFuncCallNode(FuncCallNode Node) {
		@Var BunFuncNameNode FuncNameNode = Node.FuncNameNode();
		if(FuncNameNode != null) {
			this.GenerateFuncName(FuncNameNode);
		}
		else {
			this.GenerateExpression(Node.FunctorNode());
		}
		this.GenerateListNode("(", Node, ", ", ")");
	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		@Var BNode ResolvedNode = Node.ResolvedNode;
		if(ResolvedNode == null && !this.LangInfo.AllowUndefinedSymbol) {
			LibBunLogger._LogError(Node.SourceToken, "undefined symbol: " + Node.GivenName);
		}
		this.Source.Append(Node.GetUniqueName(this));
	}

	@Override public void VisitAssignNode(AssignNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" <- ");
		this.GenerateExpression(Node.RightNode());
	}

	@Override
	public void VisitGetFieldNode(GetFieldNode Node) {
	}

	@Override
	public void VisitUnaryNode(UnaryOperatorNode Node) {
	}

	@Override
	public void VisitCastNode(BunCastNode Node) {
	}

	@Override
	public void VisitInstanceOfNode(BunInstanceOfNode Node) {
	}

	@Override
	public void VisitBinaryNode(BinaryOperatorNode Node) {
	}

	@Override
	public void VisitBlockNode(BunBlockNode Node) {
		this.Source.OpenIndent(" {");
		this.GenerateStmtList(Node);
		this.Source.CloseIndent("}");
	}

	@Override
	public void VisitIfNode(BunIfNode Node) {
		this.Source.Append("if ");
		this.Source.Append("(");
		this.GenerateExpression(Node.CondNode());
		this.Source.Append(")");
		this.GenerateExpression(Node.ThenNode());
		if (Node.HasElseNode()) {
			BNode ElseNode = Node.ElseNode();
			if(ElseNode instanceof BunIfNode) {
				this.Source.AppendNewLine("else ");
			}
			else {
				this.Source.AppendNewLine("else");
			}
			this.GenerateExpression(Node.ElseNode());
		}
	}

	@Override
	public void VisitReturnNode(BunReturnNode Node) {
		this.Source.Append("return");
		if (Node.HasReturnExpr()) {
			this.Source.Append(" (");
			this.GenerateExpression(Node.ExprNode());
			this.Source.Append(")");
		}
	}

	@Override
	public void VisitWhileNode(BunWhileNode Node) {
		this.GenerateExpression("while (", Node.CondNode(),")");
		if(Node.HasNextNode()) {
			Node.BlockNode().Append(Node.NextNode());
		}
		this.GenerateExpression(Node.BlockNode());
	}

	@Override
	public void VisitBreakNode(BunBreakNode Node) {
		this.Source.Append("break");
	}

	@Override
	public void VisitClassNode(BunClassNode Node) {
	}
}
