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

package libbun.parser;

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

public abstract class LibBunVisitor {

	public abstract void VisitGroupNode(GroupNode Node);
	public abstract void VisitAsmNode(BunAsmNode Node);

	public abstract void VisitLiteralNode(LiteralNode Node);

	public abstract void VisitArrayLiteralNode(BunArrayLiteralNode Node);
	public abstract void VisitMapLiteralNode(BunMapLiteralNode Node);
	public abstract void VisitNewObjectNode(NewObjectNode Node);
	//	public abstract void VisitNewArrayNode(ZNewArrayNode Node);

	public abstract void VisitFuncCallNode(FuncCallNode Node);
	public abstract void VisitFormNode(BunFormNode FuncNode);

	public abstract void VisitGetNameNode(GetNameNode Node);
	public abstract void VisitGetFieldNode(GetFieldNode Node);
	public abstract void VisitGetIndexNode(GetIndexNode Node);
	public abstract void VisitAssignNode(AssignNode Node);
	//	public abstract void VisitSetIndexNode(SetIndexNode Node);
	//	public abstract void VisitAssignNode(AssignNode Node);
	//	public abstract void VisitSetFieldNode(SetFieldNode Node);
	public abstract void VisitMethodCallNode(MethodCallNode Node);

	public abstract void VisitUnaryNode(UnaryOperatorNode Node);
	public abstract void VisitCastNode(BunCastNode Node);
	public abstract void VisitInstanceOfNode(BunInstanceOfNode Node);
	public abstract void VisitBinaryNode(BinaryOperatorNode Node);

	public abstract void VisitBlockNode(BunBlockNode Node);
	public abstract void VisitVarBlockNode(BunVarBlockNode Node);
	public abstract void VisitIfNode(BunIfNode Node);
	public abstract void VisitReturnNode(BunReturnNode Node);
	public abstract void VisitWhileNode(BunWhileNode Node);
	public abstract void VisitBreakNode(BunBreakNode Node);
	public abstract void VisitThrowNode(BunThrowNode Node);
	public abstract void VisitTryNode(BunTryNode Node);

	public abstract void VisitLetNode(BunLetVarNode Node);
	public abstract void VisitFunctionNode(BunFunctionNode Node);
	public abstract void VisitClassNode(BunClassNode Node);

	public abstract void VisitErrorNode(ErrorNode Node);

	public abstract void VisitTopLevelNode(TopLevelNode Node);
	public abstract void VisitSyntaxSugarNode(SyntaxSugarNode Node);
	public abstract void VisitDesugarNode(DesugarNode Node);
	public abstract void VisitLocalDefinedNode(LocalDefinedNode Node);

	private boolean StoppedVisitor;
	public final void EnableVisitor() {
		this.StoppedVisitor = false;
	}

	public final void StopVisitor() {
		this.StoppedVisitor = true;
	}

	public final boolean IsVisitable() {
		return !this.StoppedVisitor;
	}
	//
	//	public abstract void EnableVisitor();
	//	public abstract void StopVisitor();
	//	public abstract boolean IsVisitable();
}