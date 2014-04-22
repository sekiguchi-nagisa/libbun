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

package libbun.lang.bun;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.GroupNode;
import libbun.ast.LocalDefinedNode;
import libbun.ast.binary.AssignNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BitwiseOperatorNode;
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
import libbun.ast.expression.BunFormNode;
import libbun.ast.expression.BunFuncNameNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetFieldNode;
import libbun.ast.expression.GetIndexNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.expression.MethodCallNode;
import libbun.ast.expression.MutableNode;
import libbun.ast.expression.NewObjectNode;
import libbun.ast.literal.BunArrayLiteralNode;
import libbun.ast.literal.BunAsmNode;
import libbun.ast.literal.BunBooleanNode;
import libbun.ast.literal.BunFloatNode;
import libbun.ast.literal.BunIntNode;
import libbun.ast.literal.BunMapEntryNode;
import libbun.ast.literal.BunMapLiteralNode;
import libbun.ast.literal.BunNullNode;
import libbun.ast.literal.BunStringNode;
import libbun.ast.literal.DefaultValueNode;
import libbun.ast.literal.LiteralNode;
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
import libbun.encode.LibBunGenerator;
import libbun.parser.BNodeUtils;
import libbun.parser.BToken;
import libbun.parser.LibBunGamma;
import libbun.parser.LibBunLogger;
import libbun.parser.LibBunTypeChecker;
import libbun.type.BClassType;
import libbun.type.BFormFunc;
import libbun.type.BFunc;
import libbun.type.BFuncType;
import libbun.type.BGenericType;
import libbun.type.BPrototype;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.type.BVarScope;
import libbun.type.BVarType;
import libbun.util.BField;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class BunTypeSafer extends LibBunTypeChecker {

	@BField protected BunFunctionNode CurrentFunctionNode = null;

	public BunTypeSafer(LibBunGenerator Generator) {
		super(Generator);
	}

	public final boolean IsTopLevel() {
		return (this.CurrentFunctionNode == null);
	}

	@Override public void VisitDefaultValueNode(DefaultValueNode Node) {
		@Var BType Type = this.GetContextType();
		if(Type.IsIntType()) {
			this.ReturnTypeNode(new BunIntNode(Node.ParentNode, null, 0), Type);
			return;
		}
		if(Type.IsBooleanType()) {
			this.ReturnTypeNode(new BunBooleanNode(Node.ParentNode, null, false), Type);
			return;
		}
		if(Type.IsFloatType()) {
			this.ReturnTypeNode(new BunFloatNode(Node.ParentNode, null, 0.0), Type);
			return;
		}
		if(!Type.IsVarType()) {
			this.ReturnTypeNode(new BunNullNode(Node.ParentNode, null), Type);
			return;
		}
		this.ReturnTypeNode(Node, Type);
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		@Var BType Type = this.GetContextType();
		this.ReturnTypeNode(Node, Type);
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	@Override public void VisitIntNode(BunIntNode Node) {
		this.ReturnTypeNode(Node, BType.IntType);
	}

	@Override public void VisitFloatNode(BunFloatNode Node) {
		this.ReturnTypeNode(Node, BType.FloatType);
	}

	@Override public void VisitStringNode(BunStringNode Node) {
		this.ReturnTypeNode(Node, BType.StringType);
	}

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		@Var BType ArrayType = this.GetContextType();
		if(ArrayType.IsMapType() && Node.GetListSize() == 0) {
			/* this is exceptional treatment for map literal */
			this.ReturnTypeNode(new BunMapLiteralNode(Node.ParentNode), ArrayType);
			return;
		}
		@Var BType ElementType = BType.VarType;
		if(ArrayType.IsArrayType()) {
			ElementType = ArrayType.GetParamType(0);
		}
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BNode SubNode = Node.GetListAt(i);
			SubNode = this.CheckType(SubNode, ElementType);
			Node.SetListAt(i, SubNode);
			if(ElementType.IsVarType()) {
				ElementType = SubNode.Type;
			}
			i = i + 1;
		}
		if(!ElementType.IsVarType()) {
			this.ReturnTypeNode(Node,BTypePool._GetGenericType1(BGenericType._ArrayType, ElementType));
			return;
		}
		this.ReturnTypeNode(Node, BType.VarType);
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		@Var BType ContextType = this.GetContextType();
		@Var BType EntryType = BType.VarType;
		if(ContextType.IsMapType()) {
			EntryType = ContextType.GetParamType(0);
		}
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BunMapEntryNode EntryNode = Node.GetMapEntryNode(i);
			if(EntryNode.Name == null) {
				EntryNode.Name = EntryNode.KeyNode().SourceToken.GetText();
			}
			if(EntryNode.IsUntyped()) {
				this.CheckTypeAt(EntryNode, BunMapEntryNode._Value, EntryType);
				if(EntryType.IsVarType()) {
					EntryType = EntryNode.GetAstType(BunMapEntryNode._Value);
				}
			}
			i = i + 1;
		}
		if(!EntryType.IsVarType()) {
			this.ReturnTypeNode(Node, BTypePool._GetGenericType1(BGenericType._MapType, EntryType));
			return;
		}
		this.ReturnTypeNode(Node, BType.VarType);
	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		@Var LibBunGamma Gamma = Node.GetGamma();
		@Var BunLetVarNode VarNode = Gamma.GetSymbol(Node.GivenName);
		Node.ResolvedNode = VarNode;
		if(VarNode != null) {
			VarNode.Used();
			if(VarNode.InitValueNode() instanceof BunAsmNode) {
				this.ReturnTypeNode(VarNode.InitValueNode(), VarNode.DeclType());
				return;
			}
			this.ReturnTypeNode(Node, VarNode.DeclType());
			return;
		}
		this.ReturnTypeNode(Node, BType.VarType);
	}

	@Override public void VisitAssignNode(AssignNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.VarType);
		@Var BNode LeftNode = Node.LeftNode();
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, LeftNode.Type);
		if(LeftNode instanceof MutableNode) {
			if(!((MutableNode)LeftNode).IsImmutable) {
				this.ReturnTypeNode(Node, BType.VoidType);
				return;
			}
		}
		this.ReturnTypeErrorNode("immutable", LeftNode);
	}

	private BType GetIndexType(LibBunGamma Gamma, BType RecvType) {
		if(RecvType.IsArrayType() || RecvType.IsStringType()) {
			return BType.IntType;
		}
		if(RecvType.IsMapType()) {
			return BType.StringType;
		}
		return BType.VarType;
	}

	private BType GetElementType(LibBunGamma Gamma, BType RecvType) {
		if(RecvType.IsArrayType() || RecvType.IsMapType()) {
			return RecvType.GetParamType(0);
		}
		if(RecvType.IsStringType()) {
			return BType.StringType;
		}
		return BType.VarType;
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		@Var LibBunGamma Gamma = Node.GetGamma();
		this.CheckTypeAt(Node, GetIndexNode._Recv, BType.VarType);
		this.CheckTypeAt(Node, GetIndexNode._Index, this.GetIndexType(Gamma, Node.RecvNode().Type));
		this.ReturnTypeNode(Node, this.GetElementType(Gamma, Node.RecvNode().Type));
	}

	@Override public void VisitGroupNode(GroupNode Node) {
		@Var BType ContextType = this.GetContextType();
		this.CheckTypeAt(Node, GroupNode._Expr, ContextType);
		this.ReturnTypeNode(Node, Node.GetAstType(GroupNode._Expr));
	}

	@Override public void VisitFormNode(BunFormNode FuncNode) {
		this.ReturnNode(this.TypeListNodeAsFuncCall(FuncNode, FuncNode.GetFuncType()));
	}

	@Override public void VisitFuncCallNode(FuncCallNode Node) {
		@Var LibBunGamma Gamma = Node.GetGamma();
		this.TypeCheckNodeList(Node);
		this.CheckTypeAt(Node, FuncCallNode._Functor, BType.VarType);
		@Var BNode FuncNode = Node.FunctorNode();
		@Var BType FuncNodeType = Node.GetAstType(FuncCallNode._Functor);
		if(FuncNodeType instanceof BFuncType) {
			this.ReturnNode(this.TypeListNodeAsFuncCall(Node, (BFuncType)FuncNodeType));
			return;
		}
		//		if(FuncNode instanceof BunTypeNode) {   // TypeName()..;
		//			@Var String FuncName = FuncNode.Type.GetName();
		//			FuncNode = new BunFuncNameNode(Node, FuncNode.SourceToken, FuncName, FuncNode.Type, Node.GetListSize());
		//			Node.SetNode(FuncCallNode._Functor, FuncNode);
		//		}
		if(FuncNode instanceof GetNameNode) {
			@Var String FuncName = ((GetNameNode)FuncNode).GivenName;
			FuncNode = new BunFuncNameNode(Node, FuncNode.SourceToken, FuncName, Node.GetRecvType(), Node.GetListSize());
			Node.SetNode(FuncCallNode._Functor, FuncNode);
		}
		if(FuncNode instanceof BunFuncNameNode) {
			BunFuncNameNode FuncNameNode = (BunFuncNameNode)FuncNode;
			@Var BFunc Func = this.LookupFunc(Gamma, FuncNameNode.FuncName, FuncNameNode.RecvType, FuncNameNode.FuncParamSize);
			if(Func != null) {
				this.VarScope.TypeNode(FuncNameNode, Func.GetFuncType());
			}
			if(Func instanceof BFormFunc) {
				@Var BunFormNode MacroNode = Node.ToFormNode((BFormFunc)Func);
				this.ReturnNode(this.TypeListNodeAsFuncCall(MacroNode, Func.GetFuncType()));
				return;
			}
			if(Func != null) {
				this.ReturnNode(this.TypeListNodeAsFuncCall(Node, Func.GetFuncType()));
				return;
			}
			this.ReturnTypeNode(Node, BType.VarType);
		}
		else {
			this.ReturnNode(new ErrorNode(Node, "not function: " + FuncNodeType + " of node " + Node.FunctorNode()));
		}
	}

	private BType LookupFieldType(LibBunGamma Gamma, BType ClassType, String FieldName) {
		ClassType = ClassType.GetRealType();
		if(ClassType instanceof BClassType) {
			return ((BClassType)ClassType).GetFieldType(FieldName, BType.VoidType);
		}
		return Gamma.Generator.GetFieldType(ClassType, FieldName);
	}

	private BType LookupSetterType(LibBunGamma Gamma, BType ClassType, String FieldName) {
		ClassType = ClassType.GetRealType();
		if(ClassType instanceof BClassType) {
			return ((BClassType)ClassType).GetFieldType(FieldName, BType.VoidType);
		}
		return Gamma.Generator.GetSetterType(ClassType, FieldName);
	}

	private BNode UndefinedFieldNode(BNode Node, String Name) {
		return new ErrorNode(Node, "undefined field: " + Name + " of " + Node.GetAstType(GetFieldNode._Recv));
	}

	@Override public void VisitGetFieldNode(GetFieldNode Node) {
		this.CheckTypeAt(Node, GetFieldNode._Recv, BType.VarType);
		@Var BNode RecvNode = Node.RecvNode();
		if(!RecvNode.IsUntyped()) {
			@Var BType FieldType = this.LookupFieldType(Node.GetGamma(), Node.GetAstType(GetFieldNode._Recv), Node.GetName());
			if(FieldType.IsVoidType()) {
				this.ReturnNode(this.UndefinedFieldNode(Node, Node.GetName()));
				return;
			}
			this.ReturnTypeNode(Node, FieldType);
			return;
		}
		//		if(RecvNode instanceof BGetNameNode) {
		//			@Var String Symbol = ((BGetNameNode)RecvNode).GetName() + "." + Node.GetName();
		//			@Var BNode VarNode = Node.GetGamma().GetSymbol(Symbol);
		//			if(VarNode instanceof BAsmNode) {
		//				((BGetNameNode) RecvNode).GivenName = Symbol;
		//				((BGetNameNode) RecvNode).ResolvedNode = VarNode;
		//				this.ReturnTypeNode(RecvNode, VarNode.Type);
		//				return;
		//			}
		//		}
		this.ReturnTypeNode(Node, BType.VarType);
	}

	private void VisitListAsNativeMethod(BNode Node, BType RecvType, String MethodName, AbstractListNode List) {
		@Var BFuncType FuncType = this.Generator.GetMethodFuncType(RecvType, MethodName, List);
		if(FuncType != null) {
			if(!FuncType.IsVarType()) {
				@Var int i = 0;
				//@Var int StaticShift = FuncType.GetParamSize() - List.GetListSize();
				@Var int StaticShift = FuncType.GetFuncParamSize() - List.GetListSize();
				while(i < List.GetListSize()) {
					@Var BNode SubNode = List.GetListAt(i);
					SubNode = this.CheckType(SubNode, FuncType.GetFuncParamType(i+StaticShift));
					List.SetListAt(i, SubNode);
					i = i + 1;
				}
			}
			this.ReturnTypeNode(Node, FuncType.GetReturnType());
			return;
		}
		@Var String Message = null;
		if(MethodName == null) {
			Message = "undefined constructor: " + RecvType;
		}
		else {
			Message = "undefined method: " + MethodName + " of " + RecvType;
		}
		this.ReturnErrorNode(Node, null, Message);
	}

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		this.CheckTypeAt(Node, MethodCallNode._Recv, BType.VarType);
		@Var LibBunGamma Gamma = Node.GetGamma();
		@Var BNode RecvNode = Node.RecvNode();
		if(!RecvNode.IsUntyped()) {
			@Var BType FieldType = this.LookupFieldType(Gamma, Node.GetAstType(MethodCallNode._Recv), Node.MethodName());
			if(FieldType instanceof BFuncType) {
				@Var BFuncType FieldFuncType = (BFuncType)FieldType;
				@Var FuncCallNode FuncCall = Node.ToGetterFuncCall(FieldFuncType);
				this.ReturnNode(this.TypeListNodeAsFuncCall(FuncCall, FieldFuncType));
				return;
			}
			@Var int FuncParamSize = Node.GetListSize() + 1;
			@Var BFunc Func = this.LookupFunc(Gamma, Node.MethodName(), Node.GetAstType(MethodCallNode._Recv), FuncParamSize);
			if(Func != null) {
				@Var AbstractListNode FuncCallNode = Node.ToFuncCallNode(this, Func, RecvNode);
				this.ReturnNode(this.TypeListNodeAsFuncCall(FuncCallNode, Func.GetFuncType()));
				return;
			}
			this.VisitListAsNativeMethod(Node, Node.GetAstType(MethodCallNode._Recv), Node.MethodName(), Node);
			return;
		}
		this.TypeCheckNodeList(Node);
		//		if(RecvNode instanceof BGetNameNode) {
		//			@Var String Symbol = ((BGetNameNode)RecvNode).GetName();
		//			@Var String FuncName = Symbol + "." + Node.MethodName();
		//			@Var int FuncParamSize = Node.GetListSize();
		//			@Var ZFunc Func = this.LookupFunc(Gamma, FuncName, Node.GetAstType(ZMethodCallNode._NameInfo+1), FuncParamSize);
		//			if(Func != null) {
		//				@Var ZListNode FuncCallNode = Node.ToFuncCallNode(this, Func, null);
		//				this.ReturnNode(this.TypeListNodeAsFuncCall(FuncCallNode, Func.GetFuncType()));
		//				return;
		//			}
		//			@Var BNode VarNode = Gamma.GetSymbol(Symbol);
		//			if(VarNode instanceof BAsmNode) {
		//				this.ReturnTypeNode(Node, ZType.VarType);
		//				return;
		//			}
		//			//			LibZen._PrintLine("FIXME: undefined function call:" + FuncName);
		//			//			//TODO: undefined function
		//		}
		this.ReturnTypeNode(Node, BType.VarType);
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
		@Var LibBunGamma Gamma = Node.GetGamma();
		@Var BType ContextType = this.GetContextType();
		this.TypeCheckNodeList(Node);
		if(Node.ClassType().IsVarType()) {
			if(ContextType.IsVarType()) {
				this.ReturnTypeNode(Node, BType.VarType);
				return;
			}
			Node.GivenType = ContextType;
		}
		@Var int FuncParamSize = Node.GetListSize() + 1;
		@Var BFunc Func = this.LookupFunc(Gamma, Node.ClassType().GetName(), Node.ClassType(), FuncParamSize);
		if(Func != null) {
			@Var AbstractListNode FuncCall = Node.ToFuncCallNode(Gamma.Generator.TypeChecker, Func);
			this.ReturnNode(this.TypeListNodeAsFuncCall(FuncCall, Func.GetFuncType()));
			return;
		}
		if(FuncParamSize == 1) { /* no argument */
			this.ReturnTypeNode(Node, Node.ClassType());
		}
		else {
			this.VisitListAsNativeMethod(Node, Node.ClassType(), null, Node);
		}
	}

	@Override public void VisitUnaryNode(UnaryOperatorNode Node) {
		this.CheckTypeAt(Node, UnaryOperatorNode._Recv, BType.VarType);
		this.ReturnTypeNode(Node, Node.RecvNode().Type);
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.CheckTypeAt(Node, BunNotNode._Recv, BType.BooleanType);
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	@Override public void VisitPlusNode(BunPlusNode Node) {
		this.CheckTypeAt(Node, UnaryOperatorNode._Recv, BType.VarType);
		this.ReturnTypeNode(Node, Node.RecvNode().Type);
	}

	@Override public void VisitMinusNode(BunMinusNode Node) {
		this.CheckTypeAt(Node, UnaryOperatorNode._Recv, BType.VarType);
		this.ReturnTypeNode(Node, Node.RecvNode().Type);
	}

	@Override public void VisitComplementNode(BunComplementNode Node) {
		this.CheckTypeAt(Node, UnaryOperatorNode._Recv, BType.IntType);
		this.ReturnTypeNode(Node, BType.IntType);
	}

	@Override public void VisitCastNode(BunCastNode Node) {
		@Var BType ContextType = this.GetContextType();
		if(Node.CastType().IsVarType()) {
			Node.Type = ContextType;
		}
		this.TryTypeAt(Node, BunCastNode._Expr, Node.CastType());
		@Var BType ExprType = Node.ExprNode().Type;
		if(Node.Type.IsVarType() || ExprType.IsVarType()) {
			this.ReturnNode(Node);
			return;
		}
		if(ExprType.Equals(Node.Type) || Node.Type.Accept(ExprType)) {
			this.ReturnNode(Node.ExprNode());
			return;
		}
		if(ExprType.Accept(Node.Type)) {
			this.ReturnNode(this.CreateStupidCastNode(Node.Type, Node.ExprNode(), Node.GetAstToken(BunCastNode._TypeInfo), "unsafe downcast"));
			return;
		}
		else {
			@Var BFunc Func = this.Generator.LookupConverterFunc(ExprType, Node.Type);
			if(Func != null) {
				this.ReturnTypeNode(Node.ToFuncCallNode(this, Func), Node.Type);
				return;
			}
		}
		this.ReturnNode(this.CreateStupidCastNode(Node.Type, Node.ExprNode(), Node.GetAstToken(BunCastNode._TypeInfo), "undefined converter"));
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.VarType);
		if(!(Node.TargetType() instanceof BClassType)) {
			LibBunLogger._LogWarning(Node.GetAstToken(BunInstanceOfNode._TypeInfo), "instanceof takes a class type; the result is implementation-dependant.");
		}
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	private BType GuessBinaryLeftType(BToken Op, BType ContextType) {
		if(Op.EqualsText('|') || Op.EqualsText('&') || Op.EqualsText("<<") || Op.EqualsText(">>") || Op.EqualsText('^')) {
			return BType.IntType;
		}
		if(Op.EqualsText('+') || Op.EqualsText('-') || Op.EqualsText('*') || Op.EqualsText('/') || Op.EqualsText('%')) {
			if(ContextType.IsNumberType()) {
				return ContextType;
			}
		}
		return BType.VarType;
	}

	private void TryUnifyBinaryType(BinaryOperatorNode Node, BType Type) {
		if(Node.GetAstType(BinaryOperatorNode._Left).Equals(Type)) {
			this.CheckTypeAt(Node, BinaryOperatorNode._Right, Type);
			return;
		}
		if(Node.GetAstType(BinaryOperatorNode._Right).Equals(Type)) {
			this.CheckTypeAt(Node, BinaryOperatorNode._Left, Type);
		}
	}

	private void UnifyBinaryEnforcedType(BinaryOperatorNode Node, BType Type) {
		if(Node.GetAstType(BinaryOperatorNode._Left).Equals(Type)) {
			Node.SetNode(BinaryOperatorNode._Right, this.EnforceNodeType(Node.RightNode(), Type));
			return;
		}
		if(Node.GetAstType(BinaryOperatorNode._Right).Equals(Type)) {
			Node.SetNode(BinaryOperatorNode._Left, this.EnforceNodeType(Node.LeftNode(), Type));
		}
	}

	@Override public void VisitBinaryNode(BinaryOperatorNode Node) {
		@Var BType ContextType = this.GetContextType();
		@Var BType LeftType = this.GuessBinaryLeftType(Node.SourceToken, ContextType);
		@Var BType RightType = this.GuessBinaryLeftType(Node.SourceToken, ContextType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, LeftType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, RightType);
		if(Node.IsDifferentlyTyped()) {
			if(Node.SourceToken.EqualsText('+')) {
				this.UnifyBinaryEnforcedType(Node, BType.StringType);
			}
			this.TryUnifyBinaryType(Node, BType.FloatType);
			this.CheckTypeAt(Node, BinaryOperatorNode._Left, Node.GetAstType(BinaryOperatorNode._Right));
		}
		this.ReturnBinaryTypeNode(Node, Node.GetAstType(BinaryOperatorNode._Left));
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.BooleanType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, BType.BooleanType);
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.BooleanType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, BType.BooleanType);
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	@Override public void VisitAddNode(BunAddNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.VarType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, BType.VarType);
		if(Node.IsDifferentlyTyped()) {
			this.UnifyBinaryEnforcedType(Node, BType.StringType);
			this.TryUnifyBinaryType(Node, BType.FloatType);
			this.CheckTypeAt(Node, BinaryOperatorNode._Left, Node.GetAstType(BinaryOperatorNode._Right));
		}
		this.ReturnBinaryTypeNode(Node, Node.GetAstType(BinaryOperatorNode._Left));
	}

	@Override public void VisitSubNode(BunSubNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.VarType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, BType.VarType);
		if(Node.IsDifferentlyTyped()) {
			this.TryUnifyBinaryType(Node, BType.FloatType);
			this.CheckTypeAt(Node, BinaryOperatorNode._Left, Node.GetAstType(BinaryOperatorNode._Right));
		}
		this.ReturnBinaryTypeNode(Node, Node.GetAstType(BinaryOperatorNode._Left));
	}

	@Override public void VisitMulNode(BunMulNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.VarType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, BType.VarType);
		if(Node.IsDifferentlyTyped()) {
			this.TryUnifyBinaryType(Node, BType.FloatType);
			this.CheckTypeAt(Node, BinaryOperatorNode._Left, Node.GetAstType(BinaryOperatorNode._Right));
		}
		this.ReturnBinaryTypeNode(Node, Node.GetAstType(BinaryOperatorNode._Left));
	}

	@Override public void VisitDivNode(BunDivNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.VarType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, BType.VarType);
		if(Node.IsDifferentlyTyped()) {
			this.TryUnifyBinaryType(Node, BType.FloatType);
			this.CheckTypeAt(Node, BinaryOperatorNode._Left, Node.GetAstType(BinaryOperatorNode._Right));
		}
		this.ReturnBinaryTypeNode(Node, Node.GetAstType(BinaryOperatorNode._Left));
	}

	@Override public void VisitModNode(BunModNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.VarType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, BType.VarType);
		if(Node.IsDifferentlyTyped()) {
			this.TryUnifyBinaryType(Node, BType.FloatType);
			this.CheckTypeAt(Node, BinaryOperatorNode._Left, Node.GetAstType(BinaryOperatorNode._Right));
		}
		this.ReturnBinaryTypeNode(Node, Node.GetAstType(BinaryOperatorNode._Left));
	}

	private void VisitBitwiseOpreator(BitwiseOperatorNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.IntType);
		this.CheckTypeAt(Node, BinaryOperatorNode._Right, BType.IntType);
		this.ReturnBinaryTypeNode(Node, BType.IntType);
	}

	@Override public void VisitLeftShiftNode(BunLeftShiftNode Node) {
		this.VisitBitwiseOpreator(Node);
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode Node) {
		this.VisitBitwiseOpreator(Node);
	}

	@Override public void VisitBitwiseAndNode(BunBitwiseAndNode Node) {
		this.VisitBitwiseOpreator(Node);
	}

	@Override public void VisitBitwiseOrNode(BunBitwiseOrNode Node) {
		this.VisitBitwiseOpreator(Node);
	}

	@Override public void VisitBitwiseXorNode(BunBitwiseXorNode Node) {
		this.VisitBitwiseOpreator(Node);
	}

	private void VisitComparatorNode(ComparatorNode Node) {
		this.CheckTypeAt(Node, BinaryOperatorNode._Left, BType.VarType);
		this.TryTypeAt(Node, BinaryOperatorNode._Right, Node.GetAstType(BinaryOperatorNode._Left));
		this.TryUnifyBinaryType(Node, BType.FloatType);
		//this.CheckTypeAt(Node, ZBinaryNode._Right, Node.GetAstType(ZBinaryNode._Left));
		this.ReturnBinaryTypeNode(Node, BType.BooleanType);
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

	@Override public void VisitLiteralNode(LiteralNode Node) {
		// TODO Auto-generated method stub

	}

	protected void VisitVarDeclNode(LibBunGamma Gamma, BunLetVarNode CurNode) {
		CurNode.InitValueNode();
		this.CheckTypeAt(CurNode, BunLetVarNode._InitValue, CurNode.DeclType());
		if(CurNode.DeclType().IsVarType()) {
			CurNode.SetDeclType(CurNode.GetAstType(BunLetVarNode._InitValue));
		}
		CurNode.SetDeclType(this.VarScope.NewVarType(CurNode.DeclType(), CurNode.GetGivenName(), CurNode.SourceToken));
		Gamma.SetSymbol(CurNode.GetGivenName(), CurNode);
	}

	@Override public void VisitBlockNode(BunBlockNode Node) {
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BNode SubNode = Node.GetListAt(i);
			@Var BNode TypedNode = this.CheckType(SubNode, BType.VoidType);
			@Var BNode CheckNode = Node.GetListAt(i);
			while(SubNode != CheckNode) {  // detecting replacement
				SubNode = CheckNode;
				TypedNode = this.CheckType(SubNode, BType.VoidType);
				CheckNode = Node.GetListAt(i);
			}
			Node.SetListAt(i, TypedNode);
			if(BNodeUtils._IsBlockBreak(SubNode)) {
				Node.ClearListToSize(i+1);
				break;
			}
			i = i + 1;
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitVarBlockNode(BunVarBlockNode Node) {
		if(this.IsTopLevel()) {
			this.ReturnErrorNode(Node, Node.SourceToken, "only available inside function");
			return;
		}
		this.VisitVarDeclNode(Node.GetBlockGamma(), Node.VarDeclNode());
		this.VisitBlockNode(Node);
		if(Node.GetListSize() == 0) {
			LibBunLogger._LogWarning(Node.SourceToken, "unused variable: " + Node.VarDeclNode().GetGivenName());
		}
	}

	@Override public void VisitIfNode(BunIfNode Node) {
		this.CheckTypeAt(Node, BunIfNode._Cond, BType.BooleanType);
		this.CheckTypeAt(Node, BunIfNode._Then, BType.VoidType);
		if(Node.HasElseNode()) {
			this.CheckTypeAt(Node, BunIfNode._Else, BType.VoidType);
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitReturnNode(BunReturnNode Node) {
		if(this.IsTopLevel()) {
			this.ReturnErrorNode(Node, Node.SourceToken, "only available inside function");
			return;
		}
		@Var BType ReturnType = this.CurrentFunctionNode.ReturnType();
		if(Node.HasReturnExpr() && ReturnType.IsVoidType()) {
			Node.AST[BunReturnNode._Expr] = null;
		}
		else if(!Node.HasReturnExpr() && !ReturnType.IsVarType() && !ReturnType.IsVoidType()) {
			LibBunLogger._LogWarning(Node.SourceToken, "returning default value of " + ReturnType);
			Node.SetNode(BunReturnNode._Expr, new DefaultValueNode(Node));
		}
		if(Node.HasReturnExpr()) {
			this.CheckTypeAt(Node, BunReturnNode._Expr, ReturnType);
		}
		else {
			if(ReturnType instanceof BVarType) {
				((BVarType)ReturnType).Infer(BType.VoidType, Node.SourceToken);
			}
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitWhileNode(BunWhileNode Node) {
		this.CheckTypeAt(Node, BunWhileNode._Cond, BType.BooleanType);
		this.CheckTypeAt(Node, BunWhileNode._Block, BType.VoidType);
		if(Node.HasNextNode()) {
			this.CheckTypeAt(Node, BunWhileNode._Next, BType.VoidType);
			Node.BlockNode().Append(Node.NextNode());
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitBreakNode(BunBreakNode Node) {
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		if(Node.ParentNode != null && Node.ParentNode.ParentNode != null &&
				Node.ParentNode.ParentNode instanceof BunFunctionNode) {
			@Var BunFunctionNode FuncNode = (BunFunctionNode) Node.ParentNode.ParentNode;
			if(FuncNode == Node.GetDefiningFunctionNode()) {
				this.CurrentFunctionNode.SetReturnType(BType.VoidType);
			}
		}
		this.CheckTypeAt(Node, BunThrowNode._Expr, BType.VarType);
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitTryNode(BunTryNode Node) {
		this.CheckTypeAt(Node, BunTryNode._Try, BType.VoidType);
		if(Node.HasCatchBlockNode()) {
			@Var LibBunGamma Gamma = Node.CatchBlockNode().GetBlockGamma();
			@Var BunLetVarNode VarNode = new BunLetVarNode(Node, BunLetVarNode._IsReadOnly, null, null);
			VarNode.GivenName = Node.ExceptionName();
			VarNode.GivenType = BClassType._ObjectType;
			Gamma.SetSymbol(VarNode.GetGivenName(), VarNode);
			this.CheckTypeAt(Node, BunTryNode._Catch, BType.VoidType);
		}
		if(Node.HasFinallyBlockNode()) {
			this.CheckTypeAt(Node, BunTryNode._Finally, BType.VoidType);
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsTopLevel()) {
			@Var BType DeclType = Node.DeclType();
			this.CheckTypeAt(Node, BunLetVarNode._InitValue, DeclType);
			@Var BType ConstType = Node.InitValueNode().Type;
			Node.SetAstType(BunLetVarNode._NameInfo, ConstType);
			if(DeclType.IsVarType()) {
				Node.SetDeclType(ConstType);
			}
			Node.NameIndex = this.Generator.GetUniqueNumber();
			Node.GetGamma().SetSymbol(Node.GetGivenName(), Node);
			this.ReturnTypeNode(Node, BType.VoidType);
		}
		else {
			@Var BType ContextType = this.GetContextType();
			@Var BNode BlockNode = new BunVarBlockNode(Node.ParentNode, Node, Node.GetScopeBlockNode());
			BlockNode = this.CheckType(BlockNode, ContextType);
			this.ReturnNode(BlockNode);
		}
	}

	@Override public void DefineFunction(BunFunctionNode FunctionNode, boolean Enforced) {
		if(FunctionNode.FuncName() != null && FunctionNode.ResolvedFuncType == null) {
			@Var BFuncType FuncType = FunctionNode.GetFuncType();
			if(Enforced || !FuncType.IsVarType()) {
				@Var LibBunGamma Gamma = FunctionNode.GetGamma();
				@Var BPrototype Func = Gamma.Generator.SetPrototype(FunctionNode, FunctionNode.FuncName(), FuncType);
				if(Func != null) {
					Func.Defined();
					if(Func.DefinedCount > 1) {
						LibBunLogger._LogError(FunctionNode.SourceToken, "redefinition of function: " + Func);
					}
				}
			}
		}
	}

	private void PushFunctionNode(LibBunGamma Gamma, BunFunctionNode FunctionNode, BType ContextType) {
		@Var BFuncType FuncType = null;
		if(ContextType instanceof BFuncType) {
			FuncType = (BFuncType)ContextType;
		}
		this.CurrentFunctionNode = FunctionNode.Push(this.CurrentFunctionNode);
		this.VarScope = new BVarScope(this.VarScope, this.Logger, null);
		@Var int i = 0;
		while(i < FunctionNode.GetListSize()) {
			@Var BunLetVarNode ParamNode = FunctionNode.GetParamNode(i);
			ParamNode.SetDeclType(this.VarScope.NewVarType(ParamNode.DeclType(), ParamNode.GetGivenName(), ParamNode.GetAstToken(BunLetVarNode._NameInfo)));
			if(FuncType != null) {
				this.VarScope.InferType(FuncType.GetFuncParamType(i), ParamNode);
			}
			Gamma.SetSymbol(ParamNode.GetGivenName(), ParamNode);
			i = i + 1;
		}
		FunctionNode.SetReturnType(this.VarScope.NewVarType(FunctionNode.ReturnType(), "return", FunctionNode.SourceToken));
		if(FuncType != null) {
			FunctionNode.Type.Maybe(FuncType.GetReturnType(), null);
		}
	}

	private void PopFunctionNode(LibBunGamma Gamma) {
		this.CurrentFunctionNode = this.CurrentFunctionNode.Pop();
		this.VarScope = this.VarScope.Parent;
	}

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		//LibZen._PrintDebug("name="+Node.FuncName+ ", Type=" + Node.Type + ", IsTopLevel=" + this.IsTopLevel());
		@Var BType ContextType = this.GetContextType();
		if(Node.IsUntyped()) {
			Node.Type = ContextType;  // funcdecl is requested with VoidType
		}
		if(Node.Type.IsVoidType()) {
			if(Node.FuncName() == null) {   // function() object
				Node.Type = BType.VarType;
			}
			//			if(!this.IsTopLevel()) {
			//				/* function f() {} ==> var f = function() {} */
			//				@Var ZVarNode VarNode = new ZVarNode(Node.ParentNode);
			//				VarNode.SetNode(ZLetVarNode._NameInfo, Node.AST[ZFunctionNode._NameInfo]);
			//				VarNode.SetNode(ZLetVarNode._InitValue, Node);
			//				@Var ZBlockNode Block = Node.GetScopeBlockNode();
			//				@Var int Index = Block.IndexOf(Node);
			//				Block.CopyTo(Index+1, VarNode);
			//				Block.ClearListAfter(Index+1);   // Block[Index] is set to VarNode
			//				this.VisitVarNode(VarNode);
			//				return;
			//			}
		}
		if(!BNodeUtils._HasFunctionBreak(Node.BlockNode())) {
			//System.out.println("adding return.. ");
			Node.BlockNode().SetNode(BNode._AppendIndex, new BunReturnNode(Node));
		}
		@Var LibBunGamma Gamma = Node.BlockNode().GetBlockGamma();
		this.PushFunctionNode(Gamma, Node, ContextType);
		this.VarScope.TypeCheckFuncBlock(this, Node);
		this.PopFunctionNode(Gamma);
		if(!Node.Type.IsVoidType()) {
			Node.Type = Node.GetFuncType();
		}
		this.ReturnNode(Node);
	}

	@Override public void VisitClassNode(BunClassNode Node) {
		@Var LibBunGamma Gamma = Node.GetGamma();
		@Var BType ClassType = Gamma.GetType(Node.ClassName(), Node.SourceToken, true/*IsCreation*/);
		if(ClassType instanceof BClassType) {
			if(!ClassType.IsOpenType()) {
				this.ReturnNode(new ErrorNode(Node, Node.ClassName() + " has been defined."));
				return;
			}
			Node.ClassType = (BClassType)ClassType;
		}
		else {
			this.ReturnNode(new ErrorNode(Node, Node.ClassName() + " is not a bun class."));
			return;
		}
		//System.out.println(" B NodeClass.ToOpen="+Node.ClassType+", IsOpenType="+Node.ClassType.IsOpenType());
		if(Node.SuperType() != null) {
			if(Node.SuperType() instanceof BClassType && !Node.SuperType().IsOpenType()) {
				Node.ClassType.EnforceSuperClass((BClassType)Node.SuperType());
			}
			else {
				this.ReturnNode(new ErrorNode(Node.ParentNode, Node.GetAstToken(BunClassNode._TypeInfo), "" + Node.SuperType() + " cannot be extended."));
				return;
			}
		}
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			if(!Node.ClassType.HasField(FieldNode.GetGivenName())) {
				FieldNode.InitValueNode();// creation of default value if not given;
				this.CheckTypeAt(FieldNode, BunLetVarNode._InitValue, FieldNode.DeclType());
				if(FieldNode.DeclType().IsVarType()) {
					FieldNode.SetDeclType(FieldNode.InitValueNode().Type);
				}
				if(FieldNode.DeclType().IsVarType()) {
					LibBunLogger._LogError(FieldNode.SourceToken, "type of " + FieldNode.GetGivenName() + " is unspecific");
				}
				else {
					Node.ClassType.AppendField(FieldNode.DeclType(), FieldNode.GetGivenName(), FieldNode.SourceToken);
				}
			}
			else {
				LibBunLogger._LogError(FieldNode.SourceToken, "duplicated field: " + FieldNode.GetGivenName());
			}
			FieldNode.Type = BType.VoidType;
			i = i + 1;
		}
		Node.ClassType.TypeFlag = LibBunSystem._UnsetFlag(Node.ClassType.TypeFlag, BType.OpenTypeFlag);
		//System.out.println(" E NodeClass.ToOpen="+Node.ClassType+", IsOpenType="+Node.ClassType.IsOpenType());
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitLocalDefinedNode(LocalDefinedNode Node) {
		// TODO Auto-generated method stub
		System.out.println("FIXME: " + Node);
	}

	// utils

	public BFunc LookupFunc(LibBunGamma Gamma, String FuncName, BType RecvType, int FuncParamSize) {
		@Var String Signature = BFunc._StringfySignature(FuncName, FuncParamSize, RecvType);
		@Var BFunc Func = this.Generator.GetDefinedFunc(Signature);
		//System.out.println("LookupFunc: " + Func + " by " + Signature);
		if(Func != null) {
			return Func;
		}
		RecvType = RecvType.GetSuperType();
		while(RecvType != null) {
			Signature = BFunc._StringfySignature(FuncName, FuncParamSize, RecvType);
			Func = this.Generator.GetDefinedFunc(Signature);
			if(Func != null) {
				return Func;
			}
			if(RecvType.IsVarType()) {
				break;
			}
			RecvType = RecvType.GetSuperType();
		}
		//		if(Func == null) {
		//			System.err.println("Unfound: " + FuncName + ", " + RecvType + ", " + FuncParamSize);
		//		}
		return null;
	}


	//	private ZFunc LookupFunc2(ZGamma Gamma, String FuncName, ZType RecvType, int FuncParamSize) {
	//		@Var ZFunc Func = this.Generator.LookupFunc(FuncName, RecvType, FuncParamSize);
	//		if(Func == null && RecvType.IsIntType()) {
	//			Func = this.Generator.GetDefinedFunc(FuncName, ZType.FloatType, FuncParamSize);
	//		}
	//		if(Func == null && RecvType.IsFloatType()) {
	//			Func = this.Generator.GetDefinedFunc(FuncName, ZType.IntType, FuncParamSize);
	//		}
	//		if(Func == null) {
	//			System.err.println("Unfound: " + FuncName + ", " + RecvType + ", " + FuncParamSize);
	//		}
	//		return null;
	//	}

}

