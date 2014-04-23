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
import libbun.ast.literal.BunMapEntryNode;
import libbun.ast.literal.BunMapLiteralNode;
import libbun.ast.literal.BunNullNode;
import libbun.ast.literal.BunStringNode;
import libbun.ast.statement.BunBreakNode;
import libbun.ast.statement.BunIfNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.statement.BunThrowNode;
import libbun.ast.statement.BunTryNode;
import libbun.ast.statement.BunWhileNode;
import libbun.ast.sugar.StringInterpolationNode;
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
import libbun.util.LibBunSystem;
import libbun.util.Var;


public class BunGenerator extends LibBunSourceGenerator {
	public BunGenerator() {
		super(new LibBunLangInfo("Bun-1.0", "bun"));
	}

	@Override protected void GenerateImportLibrary(String LibName) {
		this.Header.AppendNewLine("require ", LibName, ";");
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		this.Source.Append("null");
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		if (Node.BooleanValue) {
			this.Source.Append("true");
		} else {
			this.Source.Append("false");
		}
	}

	@Override public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(String.valueOf(Node.IntValue));
	}

	@Override public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(String.valueOf(Node.FloatValue));
	}

	@Override public void VisitStringNode(BunStringNode Node) {
		this.Source.AppendQuotedText(Node.StringValue);
	}

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		this.GenerateListNode("[", Node, ",", "]");
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		this.Source.Append("{");
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BunMapEntryNode Entry = Node.GetMapEntryNode(i);
			this.GenerateExpression("", Entry.KeyNode(), ": ", Entry.ValueNode(), ",");
			i = i + 1;
		}
		this.Source.Append("} ");  // space is needed to distinguish block
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
		this.Source.Append("new ");
		this.GenerateTypeName(Node.Type);
		this.GenerateListNode("(", Node, ",", ")");
	}

	@Override public void VisitGroupNode(GroupNode Node) {
		this.GenerateExpression("(", Node.ExprNode(), ")");
	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		//		@Var BNode ResolvedNode = Node.ResolvedNode;
		//		if(ResolvedNode == null && !this.LangInfo.AllowUndefinedSymbol) {
		//			BLogger._LogError(Node.SourceToken, "undefined symbol: " + Node.GivenName);
		//		}
		this.Source.Append(Node.GetUniqueName(this));
	}

	@Override public void VisitAssignNode(AssignNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" = ");
		this.GenerateExpression(Node.RightNode());
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.GenerateExpression("[", Node.IndexNode(), "]");
	}

	@Override public void VisitGetFieldNode(GetFieldNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.GetName());
	}

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.MethodName());
		this.GenerateListNode("(", Node, ",", ")");
	}

	@Override public void VisitFuncCallNode(FuncCallNode Node) {
		@Var BunFuncNameNode FuncNameNode = Node.FuncNameNode();
		if(FuncNameNode != null) {
			this.Source.Append(FuncNameNode.FuncName);
		}
		else {
			this.GenerateExpression(Node.FunctorNode());
		}
		this.GenerateListNode("(", Node, ",", ")");
	}

	@Override public void VisitUnaryNode(UnaryOperatorNode Node) {
		this.Source.Append(Node.GetOperator());
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitPlusNode(BunPlusNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitMinusNode(BunMinusNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitComplementNode(BunComplementNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitCastNode(BunCastNode Node) {
		if(Node.Type.IsVoidType()) {
			this.GenerateExpression(Node.ExprNode());
		}
		else {
			this.Source.Append("(");
			this.GenerateTypeName(Node.Type);
			this.Source.Append(")");
			this.GenerateExpression(Node.ExprNode());
		}
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" instanceof ");
		this.GenerateTypeName(Node.TargetType());
	}

	@Override public void VisitBinaryNode(BinaryOperatorNode Node) {
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		this.GenerateExpression(Node.LeftNode());
		this.Source.AppendWhiteSpace(Node.GetOperator(), " ");
		this.GenerateExpression(Node.RightNode());
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
	}

	@Override public void VisitAddNode(BunAddNode Node) {
		if(Node.Type.IsStringType()) {
			this.VisitStringInterpolationNode(StringInterpolationNode._ToStringInterpolationNode(Node));
		}
		else {
			this.VisitBinaryNode(Node);
		}
	}

	@Override public void VisitSubNode(BunSubNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitMulNode(BunMulNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitDivNode(BunDivNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitModNode(BunModNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitLeftShiftNode(BunLeftShiftNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitBitwiseAndNode(BunBitwiseAndNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitBitwiseOrNode(BunBitwiseOrNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitBitwiseXorNode(BunBitwiseXorNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitEqualsNode(BunEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitNotEqualsNode(BunNotEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitLessThanNode(BunLessThanNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitLessThanEqualsNode(BunLessThanEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitGreaterThanNode(BunGreaterThanNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.VisitBinaryNode(Node);
	}


	@Override
	protected void GenerateStatementEnd(BNode Node) {
		if(Node instanceof BunIfNode || Node instanceof BunWhileNode || Node instanceof BunTryNode || Node instanceof BunFunctionNode || Node instanceof BunClassNode || Node instanceof BunBlockNode) {
			return;
		}
		if(!this.Source.EndsWith(';')) {
			this.Source.Append(";");
		}
	}

	protected void GenerateStmtListNode(BunBlockNode Node) {
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BNode SubNode = Node.GetListAt(i);
			this.GenerateStatement(SubNode);
			i = i + 1;
		}
	}

	@Override public void VisitBlockNode(BunBlockNode Node) {
		this.Source.AppendWhiteSpace();
		this.Source.OpenIndent("{");
		this.GenerateStmtListNode(Node);
		this.Source.CloseIndent("}");
	}

	protected void VisitVarDeclNode(BunLetVarNode Node) {
		this.Source.Append("var ", Node.GetGivenName());
		this.GenerateTypeAnnotation(Node.DeclType());
		this.GenerateExpression(" = ", Node.InitValueNode(), ";");
	}

	@Override public void VisitVarBlockNode(BunVarBlockNode Node) {
		this.VisitVarDeclNode(Node.VarDeclNode());
		this.GenerateStmtListNode(Node);
	}


	@Override public void VisitIfNode(BunIfNode Node) {
		this.GenerateExpression("if (", Node.CondNode(), ")");
		this.GenerateExpression(Node.ThenNode());
		if (Node.HasElseNode()) {
			this.Source.AppendNewLine("else ");
			this.GenerateExpression(Node.ElseNode());
		}
	}

	@Override public void VisitReturnNode(BunReturnNode Node) {
		this.Source.Append("return");
		if (Node.HasReturnExpr()) {
			this.Source.Append(" ");
			this.GenerateExpression(Node.ExprNode());
		}
	}

	@Override public void VisitWhileNode(BunWhileNode Node) {
		this.GenerateExpression("while (", Node.CondNode(), ")");
		if(Node.HasNextNode()) {
			Node.BlockNode().Append(Node.NextNode());
		}
		this.GenerateExpression(Node.BlockNode());
	}

	@Override public void VisitBreakNode(BunBreakNode Node) {
		this.Source.Append("break");
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		this.Source.Append("throw ");
		this.GenerateExpression(Node.ExprNode());
	}

	@Override public void VisitTryNode(BunTryNode Node) {
		this.Source.Append("try");
		this.GenerateExpression(Node.TryBlockNode());
		if(Node.HasCatchBlockNode()) {
			this.Source.AppendNewLine("catch (", Node.ExceptionName());
			this.Source.Append(") ");
			this.GenerateExpression(Node.CatchBlockNode());
		}
		if (Node.HasFinallyBlockNode()) {
			this.Source.AppendNewLine("finally ");
			this.GenerateExpression(Node.FinallyBlockNode());
		}
	}

	protected void GenerateTypeAnnotation(BType Type) {
		if(!Type.IsVarType()) {
			this.Source.Append(": ");
			this.GenerateTypeName(Type);
		}
	}

	@Override public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			this.Source.Append(Node.GetGivenName());
			this.GenerateTypeAnnotation(Node.DeclType());
		}
		else {
			this.Source.Append("let ", Node.GetGivenName());
			this.GenerateTypeAnnotation(Node.DeclType());
			this.Source.Append(" = ");
			this.GenerateExpression(Node.InitValueNode());
		}
	}

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		if(Node.IsExport) {
			this.Source.Append("export ");
		}
		this.Source.Append("function ");
		if(Node.FuncName() != null) {
			this.Source.Append(Node.FuncName());
		}
		this.GenerateListNode("(", Node, ",", ")");
		this.GenerateTypeAnnotation(Node.ReturnType());
		this.GenerateExpression(Node.BlockNode());
	}

	@Override public void VisitClassNode(BunClassNode Node) {
		this.Source.AppendNewLine("class ", Node.ClassName());
		if(Node.SuperType() != null) {
			this.Source.Append(" extends ");
			this.GenerateTypeName(Node.SuperType());
		}
		this.Source.OpenIndent(" {");
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			this.Source.AppendNewLine("var ", FieldNode.GetGivenName());
			this.GenerateTypeAnnotation(FieldNode.DeclType());
			this.Source.Append(" = ");
			this.GenerateExpression(FieldNode.InitValueNode());
			this.GenerateStatementEnd(FieldNode);
			i = i + 1;
		}
		this.Source.CloseIndent("}");
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
		@Var String Message = LibBunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
		this.Source.Append("/*", Message, "*/");
	}

	// Generation of specialized syntax sugar nodes ==========================

	@Override protected boolean LocallyGenerated(BNode Node) {
		if(Node instanceof StringInterpolationNode) {
			return this.VisitStringInterpolationNode((StringInterpolationNode)Node);
		}
		return false;
	}

	protected boolean VisitStringInterpolationNode(StringInterpolationNode Node) {
		@Var int i = 0;
		this.Source.Append("\"");
		while(i < Node.GetAstSize()) {
			if(i % 2 == 0) {
				this.Source.Append(LibBunSystem._QuoteString("", Node.GetStringLiteralAt(i), ""));
			}
			else {
				this.GenerateExpression("${", Node.AST[i], "}");
			}
			i = i + 1;
		}
		this.Source.Append("\"");
		return true;
	}


}
