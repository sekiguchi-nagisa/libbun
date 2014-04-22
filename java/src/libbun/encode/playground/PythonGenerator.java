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
import libbun.ast.error.TypeErrorNode;
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
import libbun.type.BClassField;
import libbun.type.BClassType;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class PythonGenerator extends LibBunSourceGenerator {

	public PythonGenerator() {
		super(new LibBunLangInfo("Python-2.7", "py"));
		this.LoadInlineLibrary("inline.py", "##");
		this.Header.Append("#! /usr/bin/env python");
		this.Header.AppendNewLine("# -*- coding: utf-8 -*-");
		//		this.Source.AppendNewLine("import sys, codecs");
		//		this.Source.AppendNewLine("sys.stdout = codecs.getwriter('utf-8')(sys.stdout)");
		//		this.Source.AppendNewLine("## end of header", this.LineFeed);
	}

	@Override protected void GenerateImportLibrary(String LibName) {
		this.Header.AppendNewLine("import ", LibName);
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		this.Source.Append("None");
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		if(Node.BooleanValue) {
			this.Source.Append("True");
		}
		else {
			this.Source.Append("False");
		}
	}

	@Override public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(""+Node.IntValue);
	}

	@Override public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(""+Node.FloatValue);
	}

	@Override public void VisitStringNode(BunStringNode Node) {
		this.Source.Append(LibBunSystem._QuoteString("u'", Node.StringValue, "'"));
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.Source.Append("not ");
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

	@Override public void VisitBinaryNode(BinaryOperatorNode Node) {
		this.GenerateBinaryOperatorExpression(Node, Node.GetOperator());
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "and");
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "or");
	}

	@Override public void VisitAddNode(BunAddNode Node) {
		if(Node.Type.IsStringType()) {
			this.VisitStringInterpolationNode(StringInterpolationNode._ToStringInterpolationNode(Node));
		}
		else {
			this.GenerateBinaryOperatorExpression(Node, "+");
		}
	}

	@Override public void VisitSubNode(BunSubNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "-");
	}

	@Override public void VisitMulNode(BunMulNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "*");
	}

	@Override public void VisitDivNode(BunDivNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "/");
	}

	@Override public void VisitModNode(BunModNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "%");
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
		this.GenerateBinaryOperatorExpression(Node, "==");
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
		this.GenerateTypeName(Node.Type);
		this.GenerateListNode("(", Node, ",", ")");
	}

	protected final void GenerateFuncName(BunFuncNameNode Node) {
		if(this.LangInfo.AllowFunctionOverloading) {
			this.Source.Append(Node.FuncName);
		}
		else {
			this.Source.Append(Node.GetSignature());
		}
	}

	@Override public void VisitFuncCallNode(FuncCallNode Node) {
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
		this.Source.Append(" = ");
		this.GenerateExpression(Node.RightNode());
	}

	@Override public void VisitGetFieldNode(GetFieldNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.GetName());
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.GenerateExpression("[", Node.IndexNode(), "]");
	}

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.MethodName());
		this.GenerateListNode("(", Node, ",", ")");
	}

	@Override public void VisitUnaryNode(UnaryOperatorNode Node) {
		this.Source.Append(Node.GetOperator());
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitCastNode(BunCastNode Node) {
		this.GenerateExpression(Node.ExprNode());
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.Source.Append("isinstance(");
		this.GenerateExpression(Node.LeftNode());
		if(Node.TargetType() instanceof BClassType) {
			this.Source.Append(", ", this.NameClass(Node.TargetType()));
		}
		else {
			this.Source.Append(", ");
			this.GenerateTypeName(Node.TargetType());
		}
		this.Source.Append(")");
	}


	@Override protected void GenerateStatementEnd(BNode Node) {
	}

	private void GenerateStmtList(BunBlockNode BlockNode) {
		@Var int i = 0;
		while (i < BlockNode.GetListSize()) {
			@Var BNode SubNode = BlockNode.GetListAt(i);
			this.GenerateStatement(SubNode);
			i = i + 1;
		}
		if (i == 0) {
			this.Source.AppendNewLine("pass");
		}
	}

	@Override public void VisitBlockNode(BunBlockNode Node) {
		this.Source.OpenIndent(":");
		this.GenerateStmtList(Node);
		this.Source.CloseIndent("");
	}

	@Override public void VisitVarBlockNode(BunVarBlockNode Node) {
		@Var BunLetVarNode VarNode = Node.VarDeclNode();
		this.Source.Append(VarNode.GetUniqueName(this), " = ");
		this.GenerateExpression(VarNode.InitValueNode());
		this.GenerateStmtList(Node);
	}

	@Override public void VisitIfNode(BunIfNode Node) {
		this.Source.Append("if ");
		this.GenerateExpression(Node.CondNode());
		this.GenerateExpression(Node.ThenNode());
		if (Node.HasElseNode()) {
			BNode ElseNode = Node.ElseNode();
			if(ElseNode instanceof BunIfNode) {
				this.Source.AppendNewLine("el");
			}
			else {
				this.Source.AppendNewLine("else");
			}
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
		this.GenerateExpression("while (", Node.CondNode(),")");
		this.GenerateExpression(Node.BlockNode());
	}

	@Override public void VisitBreakNode(BunBreakNode Node) {
		this.Source.Append("break");
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		this.Source.Append("raise ");
		this.GenerateExpression(Node.ExprNode());
	}

	@Override public void VisitTryNode(BunTryNode Node) {
		this.Source.Append("try");
		this.GenerateExpression(Node.TryBlockNode());
		if(Node.HasCatchBlockNode()) {
			@Var String VarName = this.NameUniqueSymbol("e");
			this.Source.AppendNewLine("except Exception as ", VarName);
			this.Source.OpenIndent(":");
			this.Source.AppendNewLine(Node.ExceptionName());
			this.Source.Append(" = libbun_catch(", VarName, ")");
			this.GenerateStmtList(Node.CatchBlockNode());
			this.Source.CloseIndent("");
			this.ImportLibrary("@catch");
		}
		if(Node.HasFinallyBlockNode()) {
			this.Source.AppendNewLine("finally");
			this.GenerateExpression(Node.FinallyBlockNode());
		}
	}

	@Override public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			this.Source.Append(Node.GetUniqueName(this));
		}
		else {
			this.Source.Append(Node.GetUniqueName(this));
			this.Source.Append(" = ");
			this.GenerateExpression(Node.InitValueNode());
		}
	}

	/**
	>>> def f(x):
		...   def g(y):
		...     return x + y
		...   return g
		...
		>>> f(1)(3)
		4
	 **/

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		if(!Node.IsTopLevelDefineFunction()) {
			@Var String FuncName = Node.GetUniqueName(this);
			this.Source = this.InsertNewSourceBuilder();
			this.Source.AppendNewLine("def ", FuncName);
			this.GenerateListNode("(", Node, ", ", ")");
			this.GenerateExpression(Node.BlockNode());
			this.Source = this.Source.Pop();
			this.Source.Append(FuncName);
		}
		else {
			@Var BFuncType FuncType = Node.GetFuncType();
			this.Source.AppendNewLine("def ", Node.GetSignature());
			this.GenerateListNode("(", Node, ", ", ")");
			this.GenerateExpression(Node.BlockNode());
			if(Node.IsExport) {
				this.Source.AppendNewLine(Node.FuncName(), " = ", FuncType.StringfySignature(Node.FuncName()));
				if(Node.FuncName().equals("main")) {
					this.HasMainFunction = true;
				}
			}
			if(this.IsMethod(Node.FuncName(), FuncType)) {
				this.Source.AppendNewLine(this.NameMethod(FuncType.GetRecvType(), Node.FuncName()));
				this.Source.Append(" = ", FuncType.StringfySignature(Node.FuncName()));
			}
		}
	}

	private void GenerateMethodVariables(BunClassNode Node) {
		@Var int i = 0;
		while (i < Node.ClassType.GetFieldSize()) {
			@Var BClassField ClassField = Node.ClassType.GetFieldAt(i);
			if(ClassField.FieldType.IsFuncType()) {
				this.Source.AppendNewLine(this.NameMethod(Node.ClassType, ClassField.FieldName));
				this.Source.Append(" = None");
			}
			i = i + 1;
		}
		if(i > 0) {
			this.Source.AppendNewLine();
		}
	}

	@Override public void VisitClassNode(BunClassNode Node) {
		@Var BType SuperType = Node.ClassType.GetSuperType();
		this.GenerateMethodVariables(Node);
		this.Source.Append("class ", this.NameClass(Node.ClassType));
		if(!SuperType.Equals(BClassType._ObjectType)) {
			this.Source.Append("(", this.NameClass(SuperType), ")");
		}
		this.Source.OpenIndent(":");
		this.Source.AppendNewLine("def __init__(self)");
		this.Source.OpenIndent(":");
		if(!Node.SuperType().Equals(BClassType._ObjectType)) {
			this.Source.AppendNewLine(this.NameClass(SuperType), ".__init__(self)");
		}
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			if(!FieldNode.DeclType().IsFuncType()) {
				this.Source.AppendNewLine("self.", FieldNode.GetGivenName(), " = ");
				this.GenerateExpression(FieldNode.InitValueNode());
			}
			i = i + 1;
		}

		i = 0;
		while (i < Node.ClassType.GetFieldSize()) {
			@Var BClassField ClassField = Node.ClassType.GetFieldAt(i);
			if(ClassField.FieldType.IsFuncType()) {
				this.Source.AppendNewLine("self.", ClassField.FieldName, " = _");
				this.Source.Append(this.NameClass(Node.ClassType), "_", ClassField.FieldName);
			}
			i = i + 1;
		}
		this.Source.CloseIndent(null);
		this.Source.CloseIndent(null);
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
		if(Node instanceof TypeErrorNode) {
			@Var TypeErrorNode ErrorNode = (TypeErrorNode)Node;
			this.GenerateExpression(ErrorNode.ErrorNode);
		}
		else {
			@Var String Message = LibBunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
			this.Source.Append("libbun_error(");
			this.Source.AppendQuotedText(Message);
			this.Source.Append(")");
			this.ImportLibrary("@error");
		}
	}

	// Generation of specialized syntax sugar nodes ==========================

	@Override protected boolean LocallyGenerated(BNode Node) {
		if(Node instanceof StringInterpolationNode) {
			return this.VisitStringInterpolationNode((StringInterpolationNode)Node);
		}
		return false;
	}

	private String NormalizePythonFormat(String s) {
		return s.replaceAll("%", "%%");
	}

	protected boolean VisitStringInterpolationNode(StringInterpolationNode Node) {
		@Var String Format = "";
		@Var int i = 0;
		while(i < Node.GetAstSize()) {
			if(i % 2 == 0) {
				if(Node.AST[i] instanceof BunStringNode) {
					Format = Format + this.NormalizePythonFormat(Node.GetStringLiteralAt(i));
				}
			}
			else {
				@Var BType Type = Node.GetAstType(i);
				@Var String Formatter = "%s";
				if(Type.IsIntType()) {
					Formatter = "%d";
				}
				else if(Type.IsFloatType()) {
					Formatter = "%f";
				}
				Format = Format + Formatter;
			}
			i = i + 1;
		}
		this.Source.Append(LibBunSystem._QuoteString("u'", Format, "'"));
		if(Node.GetAstSize() > 1) {
			this.Source.Append(" % (");
			i = 1;
			while(i < Node.GetAstSize()) {
				if(i > 2) {
					this.Source.Append(", ");
				}
				this.GenerateExpression(Node.AST[i]);
				i = i + 2;
			}
			this.Source.Append(")");
		}
		return true;
	}
}
