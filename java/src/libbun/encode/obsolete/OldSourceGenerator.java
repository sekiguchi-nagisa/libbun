// ***************************************************************************
// Copyright (c) 2013-2014, Libbun project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// *  Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// *  Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// **************************************************************************

package libbun.encode.obsolete;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.GroupNode;
import libbun.ast.SyntaxSugarNode;
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
import libbun.ast.binary.ComparatorNode;
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
import libbun.ast.unary.BunCastNode;
import libbun.ast.unary.BunComplementNode;
import libbun.ast.unary.BunMinusNode;
import libbun.ast.unary.BunNotNode;
import libbun.ast.unary.BunPlusNode;
import libbun.ast.unary.UnaryOperatorNode;
import libbun.encode.LibBunSourceGenerator;
import libbun.lang.bun.BunTypeSafer;
import libbun.parser.LibBunGamma;
import libbun.parser.LibBunLangInfo;
import libbun.parser.LibBunLogger;
import libbun.type.BType;
import libbun.util.BField;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class OldSourceGenerator extends LibBunSourceGenerator {

	@BField public boolean IsDynamicLanguage = false;
	@BField public String LineComment = "//";
	@BField public String BeginComment = "/*";
	@BField public String EndComment = "*/";
	@BField public String SemiColon = ";";
	@BField public String Camma = ", ";

	@BField public String StringLiteralPrefix = "";
	@BField public String IntLiteralSuffix = "";

	@BField public String TrueLiteral = "true";
	@BField public String FalseLiteral = "false";
	@BField public String NullLiteral = "null";

	@BField public String NotOperator = "!";
	@BField public String AndOperator = "&&";
	@BField public String OrOperator = "||";

	@BField public String TopType = "var";
	@BField public String ErrorFunc = "perror";

	@BField public boolean ReadableCode = true;

	public OldSourceGenerator(String Extension, String LangVersion) {
		super(new LibBunLangInfo(LangVersion, Extension));
		this.SetTypeChecker(new BunTypeSafer(this));
	}

	@Override protected void GenerateImportLibrary(String LibName) {
		this.Header.AppendNewLine("require ", LibName, this.SemiColon);
	}

	public String NameLocalVariable(LibBunGamma Gamma, String Name) {
		@Var String SafeName = this.SymbolMap.GetOrNull(Name);
		if(SafeName != null) {
			Name = SafeName;
		}
		@Var int NameIndex = Gamma.GetNameIndex(Name);
		if(NameIndex > 0) {
			Name = Name + "__" + NameIndex;
		}
		return Name;
	}

	@Override
	protected void GenerateStatementEnd(BNode Node) {
		if(this.SemiColon != null && (!this.Source.EndsWith('}') || !this.Source.EndsWith(';'))) {
			this.Source.Append(this.SemiColon);
		}
	}

	protected final void GenerateCode2(String Pre, BType ContextType, BNode Node, String Delim, BType ContextType2, BNode Node2, String Post) {
		if(Pre != null && Pre.length() > 0) {
			this.Source.Append(Pre);
		}
		this.GenerateExpression(Node);
		if(Delim != null && Delim.length() > 0) {
			this.Source.Append(Delim);
		}
		this.GenerateExpression(Node2);
		if(Post != null && Post.length() > 0) {
			this.Source.Append(Post);
		}
	}

	final protected boolean IsNeededSurroud(BNode Node) {
		if(Node instanceof BinaryOperatorNode) {
			return true;
		}
		return false;
	}

	@Override
	protected void GenerateExpression(BNode Node) {
		if(this.IsNeededSurroud(Node)) {
			this.GenerateExpression("(", Node, ")");
		}
		else {
			this.GenerateExpression(Node);
		}
	}

	protected void GenerateStatementEnd() {
		if(this.SemiColon != null && (!this.Source.EndsWith('}') || !this.Source.EndsWith(';'))) {
			this.Source.Append(this.SemiColon);
		}
	}

	@Override
	public void GenerateStatement(BNode Node) {
		this.Source.AppendNewLine();
		if(Node instanceof BunCastNode && Node.Type == BType.VoidType) {
			Node.AST[BunCastNode._Expr].Accept(this);
		}
		else {
			Node.Accept(this);
		}
		this.GenerateStatementEnd();
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
		this.Source.Append("var ", Node.GetUniqueName(this));
		this.GenerateTypeAnnotation(Node.DeclType());
		this.GenerateExpression(" = ", Node.InitValueNode(), this.SemiColon);
	}

	@Override public void VisitVarBlockNode(BunVarBlockNode Node) {
		this.Source.AppendWhiteSpace();
		this.VisitVarDeclNode(Node.VarDeclNode());
		this.GenerateStmtListNode(Node);
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		this.Source.Append(this.NullLiteral);
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		if (Node.BooleanValue) {
			this.Source.Append(this.TrueLiteral);
		} else {
			this.Source.Append(this.FalseLiteral);
		}
	}

	@Override public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(String.valueOf(Node.IntValue), this.IntLiteralSuffix);
	}

	@Override public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(String.valueOf(Node.FloatValue));
	}

	@Override public void VisitStringNode(BunStringNode Node) {
		this.Source.Append(this.StringLiteralPrefix, LibBunSystem._QuoteString(Node.StringValue));
	}

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		this.GenerateListNode("[", Node, "]");
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
		this.GenerateListNode("(", Node, ")");
	}

	@Override public void VisitGroupNode(GroupNode Node) {
		this.GenerateExpression("(", Node.ExprNode(), ")");
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.GenerateExpression("[", Node.IndexNode(), "]");
	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		@Var BNode ResolvedNode = Node.ResolvedNode;
		if(ResolvedNode == null && !this.LangInfo.AllowUndefinedSymbol) {
			LibBunLogger._LogError(Node.SourceToken, "undefined symbol: " + Node.GivenName);
		}
		this.Source.Append(this.NameLocalVariable(Node.GetGamma(), Node.GetUniqueName(this)));
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

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.MethodName());
		this.GenerateListNode("(", Node, ")");
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
		this.GenerateListNode("(", Node, ")");
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
		this.Source.Append(this.NotOperator);
		this.GenerateExpression(Node.RecvNode());
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
		this.VisitBinaryNode(Node);
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

	protected void VisitComparatorNode(ComparatorNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.AppendWhiteSpace(Node.GetOperator(), " ");
		this.GenerateExpression(Node.RightNode());
	}

	@Override public void VisitEqualsNode(BunEqualsNode Node) {
		this.VisitComparatorNode(Node);
	}

	@Override public void VisitNotEqualsNode(BunNotEqualsNode Node) {
		this.VisitComparatorNode(Node);
	}

	@Override public void VisitLessThanNode(BunLessThanNode Node) {
		this.VisitComparatorNode(Node);
	}

	@Override public void VisitLessThanEqualsNode(BunLessThanEqualsNode Node) {
		this.VisitComparatorNode(Node);
	}

	@Override public void VisitGreaterThanNode(BunGreaterThanNode Node) {
		this.VisitComparatorNode(Node);
	}

	@Override public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode Node) {
		this.VisitComparatorNode(Node);
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.AppendWhiteSpace(this.AndOperator, " ");
		this.GenerateExpression(Node.RightNode());
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.AppendWhiteSpace(this.OrOperator, " ");
		this.GenerateExpression(Node.RightNode());
	}

	@Override public void VisitIfNode(BunIfNode Node) {
		this.GenerateExpression("if (", Node.CondNode(), ")");
		this.GenerateExpression(Node.ThenNode());
		if (Node.HasElseNode()) {
			this.Source.AppendNewLine();
			this.Source.Append("else ");
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
			this.VisitParamNode(Node);
		}
		else {
			this.Source.AppendNewLine("let ", Node.GetGivenName());
			this.GenerateTypeAnnotation(Node.DeclType());
			this.Source.Append(" = ");
			this.GenerateExpression(Node.InitValueNode());
		}
	}

	protected void VisitParamNode(BunLetVarNode Node) {
		this.Source.Append(Node.GetUniqueName(this));
		this.GenerateTypeAnnotation(Node.DeclType());
	}

	protected void VisitFuncParamNode(String OpenToken, BunFunctionNode VargNode, String CloseToken) {
		this.GenerateListNode(OpenToken, VargNode, this.Camma, CloseToken);
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
			this.Source.Append(this.SemiColon);
			i = i + 1;
		}
		this.Source.CloseIndent("}");
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
		@Var String Message = LibBunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
		this.Source.Append(this.ErrorFunc, "(");
		this.Source.Append(LibBunSystem._QuoteString(Message));
		this.Source.Append(")");
	}

	@Override public void VisitSyntaxSugarNode(SyntaxSugarNode Node) {
		@Var DesugarNode DesugarNode = Node.PerformDesugar(this.TypeChecker);
		this.GenerateExpression(DesugarNode.AST[0]);
		@Var int i = 1;
		while(i < DesugarNode.GetAstSize()) {
			this.Source.Append(this.SemiColon);
			this.Source.AppendNewLine();
			this.GenerateExpression(DesugarNode.AST[i]);
			i = i + 1;
		}
	}

	// Utils
	@Override
	protected void GenerateTypeName(BType Type) {
		this.Source.Append(this.GetNativeTypeName(Type.GetRealType()));
	}

	@Override
	protected void GenerateListNode(String OpenToken, AbstractListNode VargNode, String DelimToken, String CloseToken) {
		this.Source.Append(OpenToken);
		@Var int i = 0;
		while(i < VargNode.GetListSize()) {
			@Var BNode ParamNode = VargNode.GetListAt(i);
			if (i > 0) {
				this.Source.Append(DelimToken);
			}
			this.GenerateExpression(ParamNode);
			i = i + 1;
		}
		this.Source.Append(CloseToken);
	}

	protected void GenerateListNode(String OpenToken, AbstractListNode VargNode, String CloseToken) {
		this.GenerateListNode(OpenToken, VargNode, this.Camma, CloseToken);
	}

	protected void GenerateWrapperCall(String OpenToken, BunFunctionNode FuncNode, String CloseToken) {
		this.Source.Append(OpenToken);
		@Var int i = 0;
		while(i < FuncNode.GetListSize()) {
			@Var BunLetVarNode ParamNode = FuncNode.GetParamNode(i);
			if (i > 0) {
				this.Source.Append(this.Camma);
			}
			this.Source.Append(this.NameLocalVariable(ParamNode.GetGamma(), ParamNode.GetGivenName()));
			i = i + 1;
		}
		this.Source.Append(CloseToken);
	}

}
