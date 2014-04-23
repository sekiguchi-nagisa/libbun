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
import libbun.ast.unary.BunCastNode;
import libbun.ast.unary.BunComplementNode;
import libbun.ast.unary.BunMinusNode;
import libbun.ast.unary.BunNotNode;
import libbun.ast.unary.BunPlusNode;
import libbun.ast.unary.UnaryOperatorNode;
import libbun.encode.LibBunSourceGenerator;
import libbun.parser.LibBunLangInfo;
import libbun.parser.LibBunLogger;
import libbun.type.BClassType;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.LibBunSystem;
import libbun.util.Var;
import libbun.util.ZenMethod;

public class JavaScriptGenerator extends LibBunSourceGenerator {

	public JavaScriptGenerator() {
		super(new LibBunLangInfo("JavaScript-1.4", "js"));
		this.SetNativeType(BType.BooleanType, "Boolean");
		this.SetNativeType(BType.IntType, "Number");
		this.SetNativeType(BType.FloatType, "Number");
		this.SetNativeType(BType.StringType, "String");
		this.SetNativeType(BType.VarType, "Object");
		this.LoadInlineLibrary("inline.js", "//");
		this.SetReservedName("this", "self");
	}

	@Override protected void GenerateImportLibrary(String LibName) {
		this.Header.AppendNewLine(LibName);
	}

	@Override @ZenMethod protected void Finish(String FileName) {
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
		this.Source.Append("new ", this.NameClass(Node.Type));
		this.GenerateListNode("(", Node, ", ", ")");
	}

	@Override public void VisitCastNode(BunCastNode Node) {
		this.GenerateExpression(Node.ExprNode());
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.Source.Append("(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(").constructor.name === ", this.NameClass(Node.TargetType()), ".name");
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		this.Source.Append("throw ");
		this.GenerateExpression(Node.ExprNode());
	}

	@Override public void VisitTryNode(BunTryNode Node) {
		this.Source.Append("try");
		this.GenerateExpression(Node.TryBlockNode());
		if(Node.HasCatchBlockNode()){
			@Var String VarName = Node.ExceptionName();
			this.ImportLibrary("@catch");
			this.Source.Append("catch(", VarName, "){ ");
			this.Source.Append(VarName, " = libbun_catch(", VarName);
			this.Source.Append("); ");
			this.GenerateExpression(Node.CatchBlockNode());
			this.Source.Append("}");
		}
		if (Node.HasFinallyBlockNode()) {
			this.Source.Append("finally");
			this.GenerateExpression(Node.FinallyBlockNode());
		}
	}


	private void VisitAnonymousFunctionNode(BunFunctionNode Node) {
		this.GenerateListNode("(function(", Node, ", ", ")");
		this.GenerateExpression(Node.BlockNode());
		this.Source.Append(")");
	}

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		if(Node.FuncName() == null) {
			this.VisitAnonymousFunctionNode(Node);
			return;
		}
		@Var BFuncType FuncType = Node.GetFuncType();
		this.Source.AppendNewLine("function ", Node.GetSignature());
		this.GenerateListNode("(", Node, ",", ")");
		this.GenerateExpression(Node.BlockNode());
		if(Node.IsExport) {
			this.Source.AppendNewLine(Node.FuncName(), " = ", FuncType.StringfySignature(Node.FuncName()));
			if(Node.FuncName().equals("main")) {
				this.HasMainFunction = true;
			}
			this.Source.Append(";");
		}
		if(this.IsMethod(Node.FuncName(), FuncType)) {
			this.Source.AppendNewLine(this.NameClass(FuncType.GetRecvType()), ".prototype.", Node.FuncName());
			this.Source.Append(" = ", FuncType.StringfySignature(Node.FuncName()));
			this.Source.Append(";");
		}
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		this.Source.Append("{");
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BunMapEntryNode Entry = Node.GetMapEntryNode(i);
			this.GenerateExpression("", Entry.KeyNode(), ": ", Entry.ValueNode(), ",");
			i = i + 1;
		}
		this.Source.Append("}");
	}

	@Override public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			this.Source.Append(Node.GetUniqueName(this));
		}
		else {
			this.Source.Append("var ", Node.GetUniqueName(this), " = ");
			this.GenerateExpression(Node.InitValueNode());
		}
	}

	@Override public void VisitClassNode(BunClassNode Node) {
		/* var ClassName = (function(_super) {
		 *  __extends(ClassName, _super);
		 * 	function ClassName(params) {
		 * 		_super.call(this, params);
		 * 	}
		 * 	ClassName.prototype.MethodName = function(){ };
		 * 	return ClassName;
		 * })(_super);
		 */
		@Var boolean HasSuperType = Node.SuperType() != null && !Node.SuperType().Equals(BClassType._ObjectType);
		@Var String ClassName = this.NameClass(Node.ClassType);
		if(HasSuperType) {
			this.ImportLibrary("@extend");
		}
		this.Source.AppendNewLine("var ", ClassName, " = ");
		this.Source.Append("(function(");
		if(HasSuperType) {
			this.Source.OpenIndent("_super) {");
			this.Source.AppendNewLine("__extends(", ClassName, ", ");
			this.Source.Append("_super)", ";");
		} else {
			this.Source.OpenIndent(") {");
		}
		this.Source.AppendNewLine("function ", ClassName);
		this.Source.OpenIndent("() {");
		if(HasSuperType) {
			this.Source.AppendNewLine("_super.call(this)", ";");
		}

		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			@Var BNode ValueNode = FieldNode.InitValueNode();
			if(!(ValueNode instanceof BunNullNode)) {
				this.Source.AppendNewLine("this.");
				this.Source.Append(FieldNode.GetGivenName());
				this.Source.Append(" = ");
				this.GenerateExpression(FieldNode.InitValueNode());
				this.Source.Append(";");
			}
			i = i + 1;
		}
		this.Source.CloseIndent("}");

		this.Source.AppendNewLine("return ", ClassName, ";");
		this.Source.CloseIndent("})(");
		if(HasSuperType) {
			this.Source.Append(this.NameClass(Node.SuperType()));
		}
		this.Source.Append(")");
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
		if(Node instanceof TypeErrorNode) {
			@Var TypeErrorNode ErrorNode = (TypeErrorNode)Node;
			this.GenerateExpression(ErrorNode.ErrorNode);
		}
		else {
			this.ImportLibrary("@SoftwareFault");
			@Var String Message = LibBunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
			this.Source.AppendWhiteSpace();
			this.Source.Append("(function(){ throw new SoftwareFault(");
			this.Source.Append(LibBunSystem._QuoteString(Message));
			this.Source.Append("); })()");
		}
	}

	@Override
	public void VisitNullNode(BunNullNode Node) {
		this.Source.Append("null");
	}

	@Override
	public void VisitBooleanNode(BunBooleanNode Node) {
		this.Source.Append(Node.BooleanValue ? "true" : "false");
	}

	@Override
	public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(Long.toString(Node.IntValue));
	}

	@Override
	public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(Double.toString(Node.FloatValue));
	}

	@Override
	public void VisitStringNode(BunStringNode Node) {
		this.Source.AppendQuotedText(Node.StringValue);
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.Source.Append("!(");
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(")");
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
		this.Source.Append(" ", Operator, " ");
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
		this.GenerateBinaryOperatorExpression(Node, "+");
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
		if(Node.LeftNode() instanceof GetIndexNode){
			@Var GetIndexNode Indexer = (GetIndexNode)Node.LeftNode();
			@Var BType RecvType = Indexer.GetAstType(GetIndexNode._Recv);
			if(RecvType.IsArrayType()) {
				this.ImportLibrary("@arrayset");
				this.GenerateExpression("libbun_arrayset(", Indexer.RecvNode(), ", ", Indexer.IndexNode(), ", ");
				this.GenerateExpression("", Node.RightNode(), ")");
				return;
			}else if(RecvType.IsMapType()) {
				this.GenerateExpression("", Indexer.RecvNode(), "[", Indexer.IndexNode(), "]");
				this.GenerateExpression(" = ", Node.RightNode(), "");
				return;
			}
		}
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" = ");
		this.GenerateExpression(Node.RightNode());
	}

	@Override public void VisitGetFieldNode(GetFieldNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.GetName());
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		@Var BType RecvType = Node.GetAstType(GetIndexNode._Recv);
		if(RecvType.IsMapType()) {
			this.ImportLibrary("@mapget");
			this.GenerateExpression("libbun_mapget(", Node.RecvNode(), ", ", Node.IndexNode(), ")");
		}
		else if(RecvType.IsArrayType()) {
			this.ImportLibrary("@arrayget");
			this.GenerateExpression("libbun_arrayget(", Node.RecvNode(), ", ", Node.IndexNode(), ")");
		}
		else {
			this.GenerateExpression(Node.RecvNode());
			this.GenerateExpression("[", Node.IndexNode(), "]");
		}
	}

	//	@Override
	//	public void VisitSetIndexNode(SetIndexNode Node) {
	//		this.GenerateExpression(Node.RecvNode());
	//		this.GenerateExpression("[", Node.IndexNode(), "] = ");
	//		this.GenerateExpression(Node.ExprNode());
	//	}

	@Override
	public void VisitMethodCallNode(MethodCallNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.MethodName());
		this.GenerateListNode("(", Node, ",", ")");
	}

	@Override
	public void VisitUnaryNode(UnaryOperatorNode Node) {
		this.Source.Append(Node.GetOperator());
		this.GenerateExpression(Node.RecvNode());
	}

	@Override
	public void VisitBinaryNode(BinaryOperatorNode Node) {
		// TODO Auto-generated method stub

	}

	private void VisitStmtList(BunBlockNode BlockNode) {
		@Var int i = 0;
		while (i < BlockNode.GetListSize()) {
			@Var BNode SubNode = BlockNode.GetListAt(i);
			this.GenerateStatement(SubNode);
			i = i + 1;
		}
	}

	@Override public void VisitBlockNode(BunBlockNode Node) {
		this.Source.OpenIndent("{");
		this.VisitStmtList(Node);
		this.Source.CloseIndent("}");
	}

	@Override
	public void VisitVarBlockNode(BunVarBlockNode Node) {
		@Var BunLetVarNode VarNode = Node.VarDeclNode();
		this.Source.AppendNewLine("var ", VarNode.GetUniqueName(this), " = ");
		this.GenerateExpression(VarNode.InitValueNode());
		this.Source.Append(";");
		this.VisitStmtList(Node);
	}

	@Override
	public void VisitIfNode(BunIfNode Node) {
		this.GenerateExpression("if (", Node.CondNode(), ")");
		this.GenerateExpression(Node.ThenNode());
		if (Node.HasElseNode()) {
			this.Source.AppendNewLine("else ");
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
	protected void GenerateStatementEnd(BNode Node) {
		this.Source.Append(";");
	}

}
