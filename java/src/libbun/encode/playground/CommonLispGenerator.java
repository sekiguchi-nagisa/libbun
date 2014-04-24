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
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.Var;
import libbun.util.ZenMethod;

public class CommonLispGenerator extends LibBunSourceGenerator {
	private boolean hasMain = false;
	public CommonLispGenerator() {
		super(new LibBunLangInfo("CommonLisp", "cl"));
	}

	@Override
	protected void GenerateImportLibrary(String LibName) {
		//		this.Header.AppendNewLine("require ", LibName, ";");
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		this.Source.Append("nil");
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		if (Node.BooleanValue) {
			this.Source.Append("t");
		} else {
			this.Source.Append("nil");
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
		//		this.Source.Append("(let ((a (make-array 8 :adjustable T :fill-pointer 0 :initial-element 'e)))");
		this.Source.Append("(let ((a (make-array ");
		this.Source.AppendInt(Node.GetListSize());
		this.Source.Append(" :initial-element ", this.InitArrayValue(Node));
		this.Source.Append(" :adjustable T :fill-pointer 0)))");
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			this.GenerateExpression(" (vector-push ", Node.GetListAt(i), " a)");
			i = i + 1;
		}
		this.Source.Append(" a)");
	}

	private String InitArrayValue(BunArrayLiteralNode Node) {
		@Var BType ParamType = Node.Type.GetParamType(0);
		if(ParamType.IsIntType()) {
			return "0";
		}
		if(ParamType.IsFloatType()) {
			return "0.0";
		}
		return "nil";
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		this.Source.Append("(let ((m (make-hash-table :test #'equal)))");
		this.Source.Append("(setf");
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BunMapEntryNode Entry = Node.GetMapEntryNode(i);
			this.GenerateExpression(" (gethash ", Entry.KeyNode(), " m)", Entry.ValueNode(), "");
			i = i + 1;
		}
		this.Source.Append(")");/*setf*/
		this.Source.Append(" m)");/*let*/
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
		// FIXME
		this.Source.Append("new ");
		this.GenerateTypeName(Node.Type);
		this.GenerateListNode("(", Node, ",", ")");
	}

	@Override public void VisitGroupNode(GroupNode Node) {
		this.GenerateExpression(Node.ExprNode());
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		@Var BType RecvType = Node.RecvNode().Type;
		if (RecvType.IsStringType()) {
			this.GenerateExpression("(string (aref ", Node.RecvNode(), " ", Node.IndexNode(), "))");
		}
		else if(RecvType.IsMapType()) {
			this.GenerateExpression("(gethash ", Node.IndexNode(), " ", Node.RecvNode(), ")");
		}
		else {
			this.GenerateExpression("(nth ", Node.IndexNode(), " ", Node.RecvNode(), ")");
		}
	}

	//	@Override public void VisitSetIndexNode(SetIndexNode Node) {
	//		@Var BType RecvType = Node.RecvNode().Type;
	//		if(RecvType.IsMapType()) {
	//			this.GenerateExpression("(setf (gethash ", Node.IndexNode(), " ", Node.RecvNode(), ") ", Node.ExprNode(), ")");
	//		}
	//		else {
	//			this.GenerateExpression("(setf (nth ", Node.IndexNode(), " ", Node.RecvNode(), ") ", Node.ExprNode(), ")");
	//		}
	//	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		this.Source.Append(Node.GetUniqueName(this));
	}

	@Override public void VisitAssignNode(AssignNode Node) {
		this.Source.Append("(setf ");
		this.GenerateExpression(Node.LeftNode());
		this.GenerateExpression(" ", Node.RightNode(), ")");
	}

	@Override public void VisitGetFieldNode(GetFieldNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.GetName());
	}

	//	@Override public void VisitSetFieldNode(SetFieldNode Node) {
	//		this.GenerateExpression(Node.RecvNode());
	//		this.Source.Append(".", Node.GetName(), " = ");
	//		this.GenerateExpression(Node.ExprNode());
	//	}

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.MethodName());
		this.GenerateListNode("(", Node, ",", ")");
	}

	@Override public void VisitFuncCallNode(FuncCallNode Node) {
		this.Source.Append("(");
		@Var BunFuncNameNode FuncNameNode = Node.FuncNameNode();
		if(FuncNameNode != null) {
			this.Source.Append(FuncNameNode.GetSignature());
		}
		else {
			this.Source.Append("funcall ");
			this.GenerateExpression(Node.FunctorNode());
		}
		this.GenerateListNode(" ", Node, " ", ")");
	}

	@Override public void VisitUnaryNode(UnaryOperatorNode Node) {
		this.Source.Append("(", Node.GetOperator(), " ");
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(")");
	}

	@Override public void VisitPlusNode(BunPlusNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitMinusNode(BunMinusNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitComplementNode(BunComplementNode Node) {
		this.GenerateExpression("(lognot ", Node.RecvNode(), ")");
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.GenerateExpression("(not ", Node.RecvNode(), ")");
	}

	@Override public void VisitCastNode(BunCastNode Node) {
		//		if(Node.Type.IsVoidType()) {
		this.GenerateExpression(Node.ExprNode());
		//		}
		//		else {
		//			this.Source.Append("(");
		//			this.GenerateTypeName(Node.Type);
		//			this.Source.Append(")");
		//			this.GenerateExpression(Node.ExprNode());
		//		}
	}

	public void GenerateBinaryNode(String Op, BinaryOperatorNode Node, String Extra) {
		this.Source.Append("(", Op, " ");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" ");
		this.GenerateExpression(Node.RightNode());
		if(Extra != null) {
			this.Source.Append(Extra);
		}
		this.Source.Append(")");
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" instanceof ");
		this.GenerateTypeName(Node.TargetType());
	}

	@Override public void VisitAddNode(BunAddNode Node) {
		this.GenerateBinaryNode(Node.GetOperator(), Node, null);
	}

	@Override public void VisitSubNode(BunSubNode Node) {
		this.GenerateBinaryNode(Node.GetOperator(), Node, null);
	}

	@Override public void VisitMulNode(BunMulNode Node) {
		this.GenerateBinaryNode(Node.GetOperator(), Node, null);
	}

	@Override public void VisitDivNode(BunDivNode Node) {
		this.GenerateBinaryNode(Node.GetOperator(), Node, null);
	}

	@Override public void VisitModNode(BunModNode Node) {
		this.GenerateBinaryNode("mod", Node, null);
	}

	@Override public void VisitLeftShiftNode(BunLeftShiftNode Node) {
		this.GenerateBinaryNode("ash", Node, null);
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode Node) {
		this.GenerateBinaryNode("ash (-", Node, ")");
	}

	@Override public void VisitBitwiseAndNode(BunBitwiseAndNode Node) {
		this.GenerateBinaryNode("logand", Node, null);
	}

	@Override public void VisitBitwiseOrNode(BunBitwiseOrNode Node) {
		this.GenerateBinaryNode("logor", Node, null);
	}

	@Override public void VisitBitwiseXorNode(BunBitwiseXorNode Node) {
		this.GenerateBinaryNode("logxor", Node, null);
	}

	@Override public void VisitEqualsNode(BunEqualsNode Node) {
		this.GenerateBinaryNode("equal", Node, null);
	}

	@Override public void VisitNotEqualsNode(BunNotEqualsNode Node) {
		this.GenerateBinaryNode("not (equal", Node, ")");
	}

	@Override public void VisitLessThanNode(BunLessThanNode Node) {
		this.GenerateBinaryNode(Node.GetOperator(), Node, null);
	}

	@Override public void VisitLessThanEqualsNode(BunLessThanEqualsNode Node) {
		this.GenerateBinaryNode(Node.GetOperator(), Node, null);
	}

	@Override public void VisitGreaterThanNode(BunGreaterThanNode Node) {
		this.GenerateBinaryNode(Node.GetOperator(), Node, null);
	}

	@Override public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode Node) {
		this.GenerateBinaryNode(Node.GetOperator(), Node, null);
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.GenerateBinaryNode("and", Node, null);
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.GenerateBinaryNode("or", Node, null);
	}


	@Override protected void GenerateStatementEnd(BNode Node) {
	}

	protected void GenerateStmtListNode(BunBlockNode BlockNode) {
		@Var int Size = BlockNode.GetListSize();
		if(Size == 0) {
			this.Source.Append("()");
		}
		else if(Size == 1) {
			this.GenerateStatement(BlockNode.GetListAt(0));
		}
		else {
			this.Source.OpenIndent("(progn");
			@Var int i = 0;
			while (i < BlockNode.GetListSize()) {
				@Var BNode SubNode = BlockNode.GetListAt(i);
				this.GenerateStatement(SubNode);
				i = i + 1;
			}
			this.Source.CloseIndent(")");
		}
	}

	@Override public void VisitBlockNode(BunBlockNode Node) {
		this.GenerateStmtListNode(Node);
	}

	@Override public void VisitVarBlockNode(BunVarBlockNode Node) {
		this.Source.Append("(let (");
		this.Source.Append("(", Node.VarDeclNode().GetUniqueName(this), " ");
		this.GenerateExpression(Node.VarDeclNode().InitValueNode());
		this.Source.Append("))");
		this.GenerateStmtListNode(Node);
		this.Source.Append(")");
	}

	@Override public void VisitIfNode(BunIfNode Node) {
		this.Source.Append("(if  ");
		this.GenerateExpression(Node.CondNode());
		this.Source.Append(" ");
		this.GenerateExpression(Node.ThenNode());
		this.Source.Append(" ");
		if(Node.HasElseNode()) {
			this.GenerateExpression(Node.ElseNode());
		}
		else {
			this.Source.Append("nil");
		}
		this.Source.Append(")");
	}

	@Override public void VisitWhileNode(BunWhileNode Node) {
		this.Source.Append("(loop while ");
		this.GenerateExpression(Node.CondNode());
		this.Source.AppendNewLine("do");
		this.GenerateExpression(Node.BlockNode());
		this.Source.Append(")");
	}

	@Override public void VisitReturnNode(BunReturnNode Node) {
		@Var BunFunctionNode FuncNode = this.LookupFunctionNode(Node);
		if(FuncNode != null) {
			this.Source.Append("(return-from ", this.ClFunctionName(FuncNode), " ");
		}
		else {
			this.Source.Append("(return ");
		}
		if(Node.HasReturnExpr()) {
			this.GenerateExpression(Node.ExprNode());
		}
		else {
			this.Source.Append("nil");
		}
		this.Source.Append(")");
	}

	private BunFunctionNode LookupFunctionNode(BNode Node) {
		while(Node != null) {
			if(Node instanceof BunFunctionNode) {
				return (BunFunctionNode)Node;
			}
			Node = Node.ParentNode;
		}
		return null;
	}

	private String ClFunctionName(BunFunctionNode FuncNode) {
		String FuncName = FuncNode.FuncName();
		if (FuncName != null) {
			if (FuncName.equals("main")) {
				return "main";
			}
			else {
				return FuncNode.GetSignature();
			}
		}
		else {
			return "nil";
		}
	}

	@Override public void VisitBreakNode(BunBreakNode Node) {
		this.Source.Append("(return)");
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		this.Source.Append("(throw nil ");
		this.GenerateExpression(Node.ExprNode());
		this.Source.Append(")");
	}

	@Override public void VisitTryNode(BunTryNode Node) {
		this.Source.Append("(unwind-protect ");
		this.Source.Append("(handler-case ");
		this.GenerateExpression(Node.TryBlockNode());
		if(Node.HasCatchBlockNode()) {
			@Var String VarName = this.NameUniqueSymbol("e");
			this.Source.AppendNewLine("(error (", VarName, ")");
			this.GenerateStmtListNode(Node.CatchBlockNode());
			this.Source.AppendNewLine(")");
		}
		this.Source.Append(")");
		if(Node.HasFinallyBlockNode()) {
			this.GenerateExpression(Node.FinallyBlockNode());
		}
		this.Source.Append(")");
	}


	protected void GenerateTypeAnnotation(BType Type) {
		if(!Type.IsVarType()) {
			this.Source.Append(": ");
			this.GenerateTypeName(Type);
		}
	}


	@Override public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			this.Source.Append(Node.GetUniqueName(this));
		}
		else {
			this.Source.AppendNewLine("(setf ", Node.GetUniqueName(this), "");
			this.Source.Append(" ");
			this.GenerateExpression(Node.InitValueNode());
			this.Source.Append(")");

		}
	}

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		if(!Node.IsTopLevelDefineFunction()) {
			this.Source.Append("#'(lambda ");
			this.GenerateListNode("(", Node, " ", ")");
			this.Source.Append("(block nil ");
			this.GenerateExpression(Node.BlockNode());
			this.Source.Append("))");
		}
		else {
			@Var BFuncType FuncType = Node.GetFuncType();
			this.Source.Append("(defun ");
			this.Source.Append(this.ClFunctionName(Node));
			this.GenerateListNode("(", Node, " ", ")");
			this.GenerateExpression(Node.BlockNode());
			this.Source.Append(")");
			if(Node.IsExport) {
				if(Node.FuncName().equals("main")) {
					this.hasMain = true;
				}
			}
			if(this.IsMethod(Node.FuncName(), FuncType)) {
				//				this.CurrentBuilder.Append(this.NameMethod(FuncType.GetRecvType(), Node.FuncName));
				//				this.CurrentBuilder.Append(" = ", FuncType.StringfySignature(Node.FuncName));
				//				this.CurrentBuilder.AppendLineFeed();
			}
		}
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

	@Override @ZenMethod protected void Finish(String FileName) {
		if(this.hasMain) {
			this.Source.AppendNewLine("(main)");
			this.Source.AppendLineFeed();
		}
	}

	@Override
	public void VisitBinaryNode(BinaryOperatorNode Node) {
		// TODO Auto-generated method stub

	}




}
