// ***************************************************************************
// Copyright (c) 2013, JST/CREST DEOS project authors. All rights reserved.
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
import libbun.type.BFunc;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class CGenerator extends LibBunSourceGenerator {

	public CGenerator() {
		super(new LibBunLangInfo("C99", "c"));
		//this.TopType = "ZObject *";
		this.SetNativeType(BType.BooleanType, "int");
		this.SetNativeType(BType.IntType, "long");
		this.SetNativeType(BType.FloatType, "double");
		this.SetNativeType(BType.StringType, "const char *");
		this.LoadInlineLibrary("common.c", "//");
		//		this.Header.AppendNewLine("/* end of header */", this.LineFeed);
	}

	@Override protected void GenerateExpression(BNode Node) {
		if(Node.IsUntyped() &&
				!Node.IsErrorNode() &&
				!(Node instanceof BunFuncNameNode) &&
				!(Node instanceof BunLetVarNode && ((BunLetVarNode)(Node)).IsParamNode())) {
			this.Source.Append("/*untyped*/NULL");
			LibBunLogger._LogError(Node.SourceToken, "untyped error: " + Node);
			return;
		}
		Node.Accept(this);
	}

	@Override protected void GenerateImportLibrary(String LibName) {
		this.Header.AppendNewLine("#include <", LibName, ">");
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		this.Source.Append("NULL");
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		if (Node.BooleanValue) {
			this.Source.Append("1/*true*/");
		} else {
			this.Source.Append("0/*false*/");
		}
	}

	@Override public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(String.valueOf(Node.IntValue));
	}

	@Override public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(String.valueOf(Node.FloatValue));
	}

	@Override public void VisitStringNode(BunStringNode Node) {
		this.Source.Append(LibBunSystem._QuoteString(Node.StringValue));
	}

	private void GenerateUnaryNode(UnaryOperatorNode Node, String Operator) {
		this.Source.Append(Operator + "(");
		Node.RecvNode().Accept(this);
		this.Source.Append(")");
	}

	private void GenerateBinaryNode(BinaryOperatorNode Node, String Operator) {
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		Node.LeftNode().Accept(this);
		this.Source.Append(" " + Operator + " ");
		Node.RightNode().Accept(this);
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.GenerateUnaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitPlusNode(BunPlusNode Node) {
		this.GenerateUnaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitMinusNode(BunMinusNode Node) {
		this.GenerateUnaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitComplementNode(BunComplementNode Node) {
		this.GenerateUnaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitAddNode(BunAddNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitSubNode(BunSubNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitMulNode(BunMulNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitDivNode(BunDivNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitModNode(BunModNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitLeftShiftNode(BunLeftShiftNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitBitwiseAndNode(BunBitwiseAndNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitBitwiseOrNode(BunBitwiseOrNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitBitwiseXorNode(BunBitwiseXorNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitEqualsNode(BunEqualsNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitNotEqualsNode(BunNotEqualsNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitLessThanNode(BunLessThanNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitLessThanEqualsNode(BunLessThanEqualsNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitGreaterThanNode(BunGreaterThanNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override public void VisitGroupNode(GroupNode Node) {
		this.Source.Append("(");
		Node.ExprNode().Accept(this);
		this.Source.Append(")");
	}

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		this.ImportLibrary("libbun.h");
		@Var BType ParamType = Node.Type.GetParamType(0);
		if(ParamType.IsIntType() || ParamType.IsBooleanType()) {
			this.Source.Append("LibZen_NewIntArray(");
		}
		else if(ParamType.IsFloatType()) {
			this.Source.Append("LibZen_NewFloatArray(");
		}
		else if(ParamType.IsStringType()) {
			this.Source.Append("LibZen_NewStringArray(");
		}
		else {
			this.Source.Append("LibZen_NewArray(");
		}
		this.Source.Append(String.valueOf(Node.GetListSize()));
		if(Node.GetListSize() > 0) {
			this.Source.Append(", ");
		}
		this.GenerateListNode("", Node, ",", ")");
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		this.ImportLibrary("libbun.h");
		@Var BType ParamType = Node.Type.GetParamType(0);
		if(ParamType.IsIntType() || ParamType.IsBooleanType()) {
			this.Source.Append("LibZen_NewIntMap(");
		}
		else if(ParamType.IsFloatType()) {
			this.Source.Append("LibZen_NewFloatMap(");
		}
		else if(ParamType.IsStringType()) {
			this.Source.Append("LibZen_NewStringMap(");
		}
		else {
			this.Source.Append("LibZen_NewMap(");
		}
		this.Source.Append(String.valueOf(Node.GetListSize()));
		if(Node.GetListSize() > 0) {
			this.Source.Append(", ");
		}
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BunMapEntryNode Entry = Node.GetMapEntryNode(i);
			this.GenerateExpression("", Entry.KeyNode(), ", ", Entry.ValueNode(), ", ");
			i = i + 1;
		}
		this.Source.Append(")");
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
		this.Source.Append("_New"+this.NameClass(Node.Type));
		this.GenerateListNode("(", Node, ",", ")");
	}

	private void GenerateFuncName(BunFuncNameNode Node) {
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
		this.GenerateListNode("(", Node, ",", ")");
	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		@Var BNode ResolvedNode = Node.ResolvedNode;
		if(ResolvedNode == null && !this.LangInfo.AllowUndefinedSymbol) {
			LibBunLogger._LogError(Node.SourceToken, "undefined symbol: " + Node.GivenName);
		}
		this.Source.Append(Node.GetUniqueName(this));
	}

	@Override public void VisitAssignNode(AssignNode Node) {
		if(Node.LeftNode() instanceof GetIndexNode) {
			GetIndexNode Left = (GetIndexNode) Node.LeftNode();
			BType ThisType = Left.GetAstType(GetIndexNode._Recv);
			if(ThisType.IsArrayType()) {
				this.Source.Append("ARRAY_set(");
			} else if(ThisType.IsMapType()) {
				this.Source.Append("MAP_set(");
			} else {
				assert(false); // unreachable
			}
			this.Source.Append(this.ParamTypeName(ThisType.GetParamType(0)) + ", ");
			this.GenerateExpression(Left.RecvNode());
			this.Source.Append(", ");
			this.GenerateExpression(Left.IndexNode());
			this.Source.Append(", ");
			this.GenerateExpression(Node.RightNode());
			this.Source.Append(")");
		}
		else {
			this.GenerateExpression(Node.LeftNode());
			this.Source.Append(" = ");
			this.GenerateExpression(Node.RightNode());
		}
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		BType ThisType = Node.GetAstType(GetIndexNode._Recv);
		if(ThisType.IsArrayType() || ThisType.IsMapType()) {
			if(ThisType.IsArrayType()) {
				this.Source.Append("ARRAY_get(");
			} else if(ThisType.IsMapType()) {
				this.Source.Append("MAP_get(");
			}
			this.Source.Append(this.ParamTypeName(ThisType.GetParamType(0)) + ", ");
			this.GenerateExpression(Node.RecvNode());
			this.Source.Append(", ");
			this.GenerateExpression(Node.IndexNode());
			this.Source.Append(")");
		} else {
			this.GenerateExpression(Node.RecvNode());
			this.Source.Append("[");
			this.GenerateExpression(Node.IndexNode());
			this.Source.Append("]");
		}
	}

	@Override
	public void VisitGetFieldNode(GetFieldNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append("->", Node.GetName());
	}

	@Override
	public void VisitMethodCallNode(MethodCallNode Node) {
		/* do nothing */
	}

	@Override
	public void VisitUnaryNode(UnaryOperatorNode Node) {
		this.GenerateUnaryNode(Node, Node.GetOperator());
	}

	@Override
	public void VisitCastNode(BunCastNode Node) {
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

	@Override
	public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.Source.Append("LibZen_Is(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(",");
		this.Source.AppendInt(Node.TargetType().TypeId);
		this.Source.Append(")");
	}

	@Override
	public void VisitBinaryNode(BinaryOperatorNode Node) {
		this.GenerateBinaryNode(Node, Node.GetOperator());
	}

	@Override
	protected void GenerateStatementEnd(BNode Node) {
	}

	protected void GenerateStmtListNode(BunBlockNode Node) {
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BNode SubNode = Node.GetListAt(i);
			this.GenerateStatement(SubNode);
			this.Source.Append(";");
			i = i + 1;
		}
		this.GenerateStatementEnd(Node);
	}

	@Override
	public void VisitBlockNode(BunBlockNode Node) {
		this.Source.AppendWhiteSpace();
		this.Source.OpenIndent("{");
		this.GenerateStmtListNode(Node);
		this.Source.CloseIndent("}");
	}

	@Override
	public void VisitVarBlockNode(BunVarBlockNode Node) {
		@Var BunLetVarNode VarNode = Node.VarDeclNode();
		this.GenerateTypeName(VarNode.GivenType);
		this.Source.Append(" ");
		this.Source.Append(VarNode.GetUniqueName(this));
		this.GenerateExpression(" = ", VarNode.InitValueNode(), ";");
		this.GenerateStmtListNode(Node);
	}

	@Override
	public void VisitIfNode(BunIfNode Node) {
		this.GenerateExpression("if (", Node.CondNode(), ")");
		this.GenerateExpression(Node.ThenNode());
		if (Node.HasElseNode()) {
			this.Source.AppendNewLine();
			this.Source.Append("else ");
			this.GenerateExpression(Node.ElseNode());
		}
	}

	@Override
	public void VisitReturnNode(BunReturnNode Node) {
		this.Source.Append("return");
		if (Node.HasReturnExpr()) {
			this.Source.Append(" ");
			this.GenerateExpression(Node.ExprNode());
		}
	}

	@Override
	public void VisitWhileNode(BunWhileNode Node) {
		this.GenerateExpression("while (", Node.CondNode(), ")");
		this.GenerateExpression(Node.BlockNode());
	}

	@Override
	public void VisitBreakNode(BunBreakNode Node) {
		this.Source.Append("break");
	}

	@Override
	public void VisitThrowNode(BunThrowNode Node) {
		this.GenerateExpression(Node.ExprNode());
		this.Source.Append("longjump(1)"); // FIXME
	}

	@Override
	public void VisitTryNode(BunTryNode Node) {
		//		this.CurrentBuilder.Append("try");
		//		this.GenerateCode(Node.TryNode());
		//		if(Node.CatchNode() != null) {
		//			this.GenerateCode(Node.CatchNode());
		//		}
		//		if (Node.FinallyNode() != null) {
		//			this.CurrentBuilder.Append("finally");
		//			this.GenerateCode(Node.FinallyNode());
		//		}
	}


	private String ParamTypeName(BType Type) {
		if(Type.IsArrayType()) {
			return "ARRAY(" + this.ParamTypeName(Type.GetParamType(0)) + ")";
		}
		if(Type.IsMapType()) {
			return "MapOf" + this.ParamTypeName(Type.GetParamType(0));
		}
		if(Type.IsFuncType()) {
			@Var String s = "FuncOf";
			@Var int i = 0;
			while(i < Type.GetParamSize()) {
				s = s +  this.ParamTypeName(Type.GetParamType(i));
				i = i + 1;
			}
			return s + "_";
		}
		if(Type.IsIntType()) {
			return "long";
		}
		if(Type.IsFloatType()) {
			return "double";
		}
		if(Type.IsVoidType()) {
			return "void";
		}
		if(Type.IsVarType()) {
			return "Var";
		}
		return Type.ShortName;
	}

	private String GetCTypeName(BType Type) {
		@Var String TypeName = null;
		if(Type.IsArrayType() || Type.IsMapType()) {
			TypeName = this.ParamTypeName(Type) + " *";
		}
		if(Type instanceof BClassType) {
			TypeName = "struct " + this.NameClass(Type) + " *";
		}
		if(TypeName == null) {
			TypeName = this.GetNativeTypeName(Type);
		}
		return TypeName;
	}

	protected void GenerateFuncTypeName(BType Type, String FuncName) {
		this.GenerateTypeName(Type.GetParamType(0));
		this.Source.Append(" (*" + FuncName + ")");
		@Var int i = 1;
		this.Source.Append("(");
		while(i < Type.GetParamSize()) {
			if(i > 1) {
				this.Source.Append(",");
			}
			this.GenerateTypeName(Type.GetParamType(i));
			i = i + 1;
		}
		this.Source.Append(")");
	}

	@Override protected void GenerateTypeName(BType Type) {
		if(Type.IsFuncType()) {
			this.GenerateFuncTypeName(Type, "");
		}
		else {
			this.Source.Append(this.GetCTypeName(Type.GetRealType()));
		}
	}

	@Override public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			if(Node.DeclType().IsFuncType()) {
				this.GenerateFuncTypeName(Node.DeclType(), Node.GetUniqueName(this));
			}
			else {
				this.GenerateTypeName(Node.DeclType());
				this.Source.Append(" ");
				this.Source.Append(Node.GetUniqueName(this));
			}
		}
		else {
			this.Source.Append("static ");
			if(Node.DeclType().IsFuncType()) {
				this.GenerateFuncTypeName(Node.GetAstType(BunLetVarNode._InitValue), Node.GetUniqueName(this));
			}
			else {
				this.GenerateTypeName(Node.GetAstType(BunLetVarNode._InitValue));
				this.Source.Append(" ");
				this.Source.Append(Node.GetUniqueName(this));
			}
			this.GenerateExpression(" = ", Node.InitValueNode(), ";");
		}
	}

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		if(!Node.IsTopLevelDefineFunction()) {
			@Var String FuncName = Node.GetUniqueName(this);
			this.Source = this.InsertNewSourceBuilder();
			this.Source.AppendNewLine("static ");
			this.GenerateTypeName(Node.ReturnType());
			this.Source.Append(" ", FuncName);
			this.GenerateListNode("(", Node, ",", ")");
			this.GenerateExpression(Node.BlockNode());
			//			this.CurrentBuilder.AppendLineFeed();
			this.Source = this.Source.Pop();
			this.Source.Append(FuncName);
		}
		else {
			@Var int StartIndex = this.Source.GetPosition();
			this.Source.AppendNewLine("static ");
			this.GenerateTypeName(Node.ReturnType());
			this.Source.Append(" ", Node.GetSignature());
			this.GenerateListNode("(", Node, ",", ")");
			@Var String Prototype = this.Source.CopyString(StartIndex, this.Source.GetPosition());
			this.GenerateExpression(Node.BlockNode());

			this.Header.AppendNewLine(Prototype);
			this.Header.Append(";");
			@Var BFuncType FuncType = Node.GetFuncType();
			if(Node.IsExport) {
				this.GenerateExportFunction(Node);
			}
			if(this.IsMethod(Node.FuncName(), FuncType)) {
				this.Header.AppendNewLine("#define _" + this.NameMethod(FuncType.GetRecvType(), Node.FuncName()));
			}
		}
	}

	private void GenerateWrapperCall(String OpenToken, BunFunctionNode FuncNode, String CloseToken) {
		this.Source.Append(OpenToken);
		@Var int i = 0;
		while(i < FuncNode.GetListSize()) {
			@Var BunLetVarNode ParamNode = FuncNode.GetParamNode(i);
			if (i > 0) {
				this.Source.Append(",");
			}
			this.Source.Append(ParamNode.GetUniqueName(this));
			i = i + 1;
		}
		this.Source.Append(CloseToken);
	}

	private void GenerateExportFunction(BunFunctionNode Node) {
		this.Source.AppendNewLine();
		if(Node.FuncName().equals("main")) {
			this.Source.Append("int");
		}
		else {
			this.GenerateTypeName(Node.ReturnType());
		}
		this.Source.Append(" ", Node.FuncName());
		this.GenerateListNode("(", Node, ",", ")");
		this.Source.OpenIndent(" {");
		if(!Node.ReturnType().IsVoidType()) {
			this.Source.AppendNewLine("return ", Node.GetSignature());
		}
		else {
			this.Source.AppendNewLine(Node.GetSignature());
		}
		this.GenerateWrapperCall("(", Node, ")");
		this.Source.Append(";");
		if(Node.FuncName().equals("main")) {
			this.Source.AppendNewLine("return 0;");
		}
		this.Source.CloseIndent("}");
	}

	private void GenerateCField(String CType, String FieldName) {
		this.Source.AppendNewLine(CType, " ");
		this.Source.Append(FieldName, ";");
	}

	private void GenerateField(BType DeclType, String FieldName) {
		this.Source.AppendNewLine();
		if(DeclType.IsFuncType()) {
			this.GenerateFuncTypeName(DeclType, FieldName);
		}
		else {
			this.GenerateTypeName(DeclType);
			this.Source.Append(" ", FieldName);
		}
		this.Source.Append(";");
	}

	private void GenerateFields(BClassType ClassType, BType ThisType) {
		@Var BType SuperType = ThisType.GetSuperType();
		if(!SuperType.Equals(BClassType._ObjectType)) {
			this.GenerateFields(ClassType, SuperType);
		}
		@Var int i = 0;
		this.GenerateCField("int", "_classId" + ThisType.TypeId);
		this.GenerateCField("int", "_delta" + ThisType.TypeId);
		while (i < ClassType.GetFieldSize()) {
			@Var BClassField ClassField = ClassType.GetFieldAt(i);
			if(ClassField.ClassType == ThisType) {
				this.GenerateField(ClassField.FieldType, ClassField.FieldName);
			}
			i = i + 1;
		}
	}

	@Override public void VisitClassNode(BunClassNode Node) {
		this.ImportLibrary("libbun.h");

		String ClassName = this.NameClass(Node.ClassType);
		String StructName = "struct " + ClassName;
		this.Source.AppendNewLine(StructName);
		this.Source.OpenIndent(" {");
		this.GenerateFields(Node.ClassType, Node.ClassType);
		this.GenerateCField("int", "_nextId");
		this.Source.CloseIndent("};");

		this.Source.AppendNewLine("static void _Init", ClassName, "(");
		this.GenerateTypeName(Node.ClassType);
		this.Source.OpenIndent(" o) {");
		@Var BType SuperType = Node.ClassType.GetSuperType();
		if(!SuperType.Equals(BClassType._ObjectType)) {
			this.Source.AppendNewLine("_Init", this.NameClass(SuperType), "((");
			this.GenerateTypeName(SuperType);
			this.Source.Append(")o);");
		}
		this.Source.AppendNewLine("o->_classId" + Node.ClassType.TypeId, " = " + Node.ClassType.TypeId, ";");
		this.Source.AppendNewLine("o->_delta" + Node.ClassType.TypeId, " = sizeof(" + StructName + ") - ");
		if(SuperType.Equals(BClassType._ObjectType)) {
			this.Source.Append("sizeof(int);");
		}
		else {
			this.Source.Append("sizeof(struct " + this.NameClass(SuperType) + ");");
		}
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			this.Source.AppendNewLine("o->", FieldNode.GetGivenName(), " = ");
			if(FieldNode.DeclType().IsFuncType()) {
				this.Source.Append("NULL");
			}
			else {
				this.GenerateExpression(FieldNode.InitValueNode());
			}
			this.Source.Append(";");
			i = i + 1;
		}

		i = 0;
		while (i < Node.ClassType.GetFieldSize()) {
			@Var BClassField ClassField = Node.ClassType.GetFieldAt(i);
			if(ClassField.FieldType.IsFuncType()) {
				@Var int IndentLevel = this.Source.SetIndentLevel(0);
				this.Source.AppendNewLine("#ifdef ", this.NameMethod(Node.ClassType, ClassField.FieldName));
				this.Source.SetIndentLevel(IndentLevel);
				this.Source.AppendNewLine("o->", ClassField.FieldName, " = ");
				this.Source.Append(BFunc._StringfySignature(ClassField.FieldName, ClassField.FieldType.GetParamSize()-1, Node.ClassType));
				IndentLevel = this.Source.SetIndentLevel(0);
				this.Source.AppendNewLine("#endif");
				this.Source.SetIndentLevel(IndentLevel);
			}
			i = i + 1;
		}

		this.Source.AppendNewLine("o->_nextId = 0;");
		this.Source.CloseIndent("}");

		this.Source.AppendNewLine("static ");
		this.GenerateTypeName(Node.ClassType);
		this.Source.Append(" _New", ClassName);
		this.Source.OpenIndent("(void) {");
		this.Source.AppendNewLine();
		this.GenerateTypeName(Node.ClassType);
		this.Source.Append("o =  (" + StructName + "*) LibZen_Malloc(sizeof(" + StructName + "));");
		this.Source.AppendNewLine("_Init", ClassName, "(o);");
		this.Source.AppendNewLine("return o;");
		this.Source.CloseIndent("}");
	}

	@Override
	public void VisitErrorNode(ErrorNode Node) {
		@Var String Message = LibBunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
		this.Source.Append("perror(");
		this.Source.Append(LibBunSystem._QuoteString(Message));
		this.Source.Append(")");
	}
}
