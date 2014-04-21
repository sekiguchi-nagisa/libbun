//***************************************************************************
//Copyright (c) 2013-2014, Libbun project authors. All rights reserved.
//Redistribution and use in source and binary forms, with or without
//modification, are permitted provided that the following conditions are met:
//
//*  Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//*  Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
//"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
//TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
//PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
//CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
//EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
//PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
//OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
//WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
//OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
//ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//**************************************************************************

package libbun.parser;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.ast.decl.TopLevelNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.error.TypeErrorNode;
import libbun.ast.expression.BunFormNode;
import libbun.ast.expression.BunFuncNameNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunAsmNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.unary.BunCastNode;
import libbun.encode.LibBunGenerator;
import libbun.type.BFormFunc;
import libbun.type.BFunc;
import libbun.type.BFuncType;
import libbun.type.BGreekType;
import libbun.type.BType;
import libbun.type.BVarScope;
import libbun.util.BField;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public abstract class LibBunTypeChecker extends BunVisitor {
	public final static int _DefaultTypeCheckPolicy			= 0;
	public final static int _NoCheckPolicy                  = 1;

	@BField private BType      StackedContextType;
	@BField private BNode      ReturnedNode;

	@BField public LibBunGenerator  Generator;
	@BField public LibBunLogger     Logger;
	@BField public BVarScope   VarScope;
	@BField public boolean     IsSupportNullable = false;
	@BField public boolean     IsSupportMutable  = false;

	public LibBunTypeChecker(LibBunGenerator Generator) {
		this.Generator = Generator;
		this.Logger = Generator.Logger;
		this.StackedContextType = null;
		this.ReturnedNode = null;
		this.VarScope = new BVarScope(null, this.Logger, null);
	}

	public final BType GetContextType() {
		return this.StackedContextType;
	}

	public final void ReturnNode(BNode Node) {
		if(this.ReturnedNode != null) {
			LibBunSystem._PrintDebug("previous returned node " + Node);
		}
		this.ReturnedNode = Node;
	}

	public final void ReturnTypeNode(BNode Node, BType Type) {
		this.VarScope.TypeNode(Node, Type);
		this.ReturnNode(Node);
	}

	public final void ReturnBinaryTypeNode(BinaryOperatorNode Node, BType Type) {
		if(!Node.GetAstType(BinaryOperatorNode._Left).IsVarType() && !Node.GetAstType(BinaryOperatorNode._Right).IsVarType()) {
			@Var String Op = Node.GetOperator();
			@Var BFunc Func = this.Generator.GetDefinedFunc(Op, Node.GetAstType(BinaryOperatorNode._Left), 2);
			if(Func instanceof BFormFunc) {
				@Var BunFormNode NewNode = new BunFormNode(Node.ParentNode, Node.SourceToken, (BFormFunc)Func);
				NewNode.Append(Node.LeftNode());
				NewNode.Append(Node.RightNode());
				this.ReturnTypeNode(NewNode, Type);
				return;
			}
		}
		this.ReturnTypeNode(Node, Type);
	}

	public final void ReturnErrorNode(BNode Node, BToken ErrorToken, String Message) {
		if(ErrorToken == null) {
			ErrorToken = Node.SourceToken;
		}
		this.ReturnNode(new ErrorNode(Node.ParentNode, ErrorToken, Message));
	}

	public final void ReturnTypeErrorNode(String Message, BNode ErrorNode) {
		this.ReturnNode(new TypeErrorNode(Message, ErrorNode));
	}

	protected final BNode CreateStupidCastNode(BType Requested, BNode Node, BToken SourceToken, String Message) {
		@Var BNode ErrorNode = new TypeErrorNode(Message + ": " + Node.Type.GetName() + " must be " + Requested.GetName(), Node);
		ErrorNode.SourceToken = SourceToken;
		ErrorNode.Type = Requested;
		return ErrorNode;
	}

	protected final BNode CreateStupidCastNode(BType Requested, BNode Node) {
		return this.CreateStupidCastNode(Requested, Node, Node.SourceToken, "type error");
	}

	protected final BNode EnforceNodeType(BNode Node, BType EnforcedType) {
		@Var BFunc Func = this.Generator.LookupConverterFunc(Node.Type, EnforcedType);
		if(Func == null && EnforcedType.IsStringType()) {
			Func = this.Generator.LookupFunc("toString", Node.Type, 1);
		}
		if(Func != null) {
			@Var AbstractListNode FuncNode = this.CreateDefinedFuncCallNode(Node.ParentNode, null, Func);
			FuncNode.Append(Node);
			return this.TypeListNodeAsFuncCall(FuncNode, Func.GetFuncType());
		}
		return this.CreateStupidCastNode(EnforcedType, Node);
	}

	private final BNode TypeCheckImpl(BNode Node, BType ContextType, int TypeCheckPolicy) {
		if(Node.IsErrorNode()) {
			if(!ContextType.IsVarType()) {
				this.VarScope.TypeNode(Node, ContextType);
			}
			return Node;
		}
		if(Node.IsUntyped() || ContextType.IsVarType() || LibBunSystem._IsFlag(TypeCheckPolicy, LibBunTypeChecker._NoCheckPolicy)) {
			return Node;
		}
		if(Node.Type == ContextType || ContextType.Accept(Node.Type)) {
			return Node;
		}
		if(ContextType.IsVoidType() && !Node.Type.IsVoidType()) {
			return new BunCastNode(Node.ParentNode, BType.VoidType, Node);
		}
		if(ContextType.IsFloatType() && Node.Type.IsIntType()) {
			return this.EnforceNodeType(Node, ContextType);
		}
		if(ContextType.IsIntType() && Node.Type.IsFloatType()) {
			return this.EnforceNodeType(Node, ContextType);
		}
		return this.CreateStupidCastNode(ContextType, Node);
	}

	private BNode VisitNode(BNode Node, BType ContextType) {
		@Var BNode ParentNode = Node.ParentNode;
		this.StackedContextType = ContextType;
		this.ReturnedNode = null;
		Node.Accept(this);
		if(this.ReturnedNode == null) {  /* debug check */
			LibBunSystem._PrintDebug("!! returns no value: " + Node);
		}
		else {
			Node = this.ReturnedNode;
		}
		if(ParentNode != Node.ParentNode && ParentNode != null) {
			if(Node.ParentNode != null) {
				LibBunSystem._PrintDebug("Preserving parent of typed new node: " + Node);
			}
			ParentNode.SetChild(Node, BNode._PreservedParent);
		}
		return Node;
	}

	private final BNode TypeCheck(BNode Node, BType ContextType, int TypeCheckPolicy) {
		if(this.IsVisitable() && Node != null) {
			if(Node.HasUntypedNode()) {
				Node = this.VisitNode(Node, ContextType);
				this.VarScope.InferType(ContextType, Node);
			}
			Node = this.TypeCheckImpl(Node, ContextType, TypeCheckPolicy);
			this.VarScope.InferType(ContextType, Node);
		}
		this.ReturnedNode = null;
		return Node;
	}


	public final BNode TryType(BNode Node, BType ContextType) {
		return this.TypeCheck(Node, ContextType, LibBunTypeChecker._NoCheckPolicy);
	}

	public final void TryTypeAt(BNode Node, int Index, BType ContextType) {
		//		@Var ZNode N = Node.AST[Index];
		Node.SetNode(Index, this.TypeCheck(Node.AST[Index], ContextType, LibBunTypeChecker._NoCheckPolicy));
		//		if(N != Node.AST[Index]) {
		//			System.out.println("Node="+Node+"\n\tFrom="+N+"\n\tTo="+Node.AST[Index]);
		//		}
	}

	public final BNode CheckType(BNode Node, BType ContextType) {
		return this.TypeCheck(Node, ContextType, LibBunTypeChecker._DefaultTypeCheckPolicy);
	}

	public final void CheckTypeAt(BNode Node, int Index, BType ContextType) {
		//		@Var ZNode N = Node.AST[Index];
		Node.SetNode(Index, this.TypeCheck(Node.AST[Index], ContextType, LibBunTypeChecker._DefaultTypeCheckPolicy));
		//		if(N != Node.AST[Index]) {
		//			System.out.println("Node="+Node+"\n\tFrom="+N+"\n\tTo="+Node.AST[Index]);
		//		}
	}

	public final void TypeCheckNodeList(AbstractListNode List) {
		@Var int i = 0;
		while(i < List.GetListSize()) {
			@Var BNode SubNode = List.GetListAt(i);
			SubNode = this.CheckType(SubNode, BType.VarType);
			List.SetListAt(i, SubNode);
			i = i + 1;
		}
	}

	public final BNode TypeListNodeAsFuncCall(AbstractListNode FuncNode, BFuncType FuncType) {
		@Var int i = 0;
		@Var BType[] Greek = BGreekType._NewGreekTypes(null);
		//		if(FuncNode.GetListSize() != FuncType.GetFuncParamSize()) {
		//			System.err.println(ZLogger._LogError(FuncNode.SourceToken, "mismatch " + FuncType + ", " + FuncNode.GetListSize()+": " + FuncNode));
		//		}
		while(i < FuncNode.GetListSize()) {
			@Var BNode SubNode = FuncNode.GetListAt(i);
			@Var BType ParamType =  FuncType.GetFuncParamType(i);
			SubNode = this.TryType(SubNode, ParamType);
			if(!SubNode.IsUntyped() || !ParamType.IsVarType()) {
				if(!ParamType.AcceptValueType(SubNode.Type, false, Greek)) {
					SubNode = this.CreateStupidCastNode(ParamType.GetGreekRealType(Greek), SubNode);
				}
			}
			FuncNode.SetListAt(i, SubNode);
			i = i + 1;
		}
		this.VarScope.TypeNode(FuncNode, FuncType.GetReturnType().GetGreekRealType(Greek));
		return FuncNode;
	}

	public abstract void DefineFunction(BunFunctionNode FunctionNode, boolean Enforced);

	@Override public final void VisitAsmNode(BunAsmNode Node) {
		this.ReturnTypeNode(Node, Node.FormType());
	}

	@Override public final void VisitTopLevelNode(TopLevelNode Node) {
		Node.Perform(Node.GetGamma());
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public final void VisitErrorNode(ErrorNode Node) {
		@Var BType ContextType = this.GetContextType();
		if(!ContextType.IsVarType()) {
			this.ReturnTypeNode(Node, ContextType);
		}
		else {
			this.ReturnNode(Node);
		}
	}

	@Override public void VisitSyntaxSugarNode(SyntaxSugarNode Node) {
		@Var BType ContextType = this.GetContextType();
		@Var DesugarNode DesugarNode = Node.DeSugar(this.Generator.TypeChecker);
		@Var int i = 0;
		while(i < DesugarNode.GetAstSize()) {
			this.CheckTypeAt(DesugarNode, i, ContextType);
			i = i + 1;
		}
		this.ReturnTypeNode(DesugarNode, DesugarNode.GetAstType(DesugarNode.GetAstSize()-1));
	}

	// ----------------------------------------------------------------------
	/* Note : the CreateNode serise are designed to treat typed node */

	public BunFunctionNode CreateFunctionNode(BNode ParentNode, String FuncName, BNode Node) {
		@Var BunFunctionNode FuncNode = new BunFunctionNode(ParentNode);
		FuncNode.GivenName = FuncName;
		FuncNode.GivenType = Node.Type;
		@Var BunBlockNode BlockNode = this.CreateBlockNode(FuncNode);
		FuncNode.SetNode(BunFunctionNode._Block, BlockNode);
		if(Node.Type.IsVoidType()) {
			BlockNode.Append(Node);
			BlockNode.Append(this.CreateReturnNode(BlockNode));
		}
		else {
			BlockNode.Append(this.CreateReturnNode(BlockNode, Node));
		}
		FuncNode.Type = BType.VoidType;
		return FuncNode;
	}

	public BunBlockNode CreateBlockNode(BNode ParentNode) {
		@Var BunBlockNode BlockNode = new BunBlockNode(ParentNode, null);
		BlockNode.Type = BType.VoidType;
		return BlockNode;
	}

	public BunReturnNode CreateReturnNode(BNode ParentNode) {
		@Var BunReturnNode ReturnNode = new BunReturnNode(ParentNode);
		ReturnNode.Type = BType.VoidType;
		return ReturnNode;
	}

	public BunReturnNode CreateReturnNode(BNode ParentNode, BNode ExprNode) {
		@Var BunReturnNode ReturnNode = new BunReturnNode(ParentNode);
		ReturnNode.SetNode(BunReturnNode._Expr, ExprNode);
		ReturnNode.Type = BType.VoidType;
		return ReturnNode;
	}

	public BunVarBlockNode CreateVarNode(BNode ParentNode, String Name, BType DeclType, BNode InitNode) {
		@Var BunLetVarNode VarNode = new BunLetVarNode(null, 0, null, null);
		VarNode.GivenName   = Name;
		VarNode.GivenType   = DeclType;
		VarNode.SetNode(BunLetVarNode._InitValue, InitNode);
		VarNode.Type = BType.VoidType;
		return new BunVarBlockNode(ParentNode, VarNode);
	}

	public GetNameNode CreateGetNameNode(BNode ParentNode, String Name, BType Type) {
		@Var GetNameNode NameNode = new GetNameNode(ParentNode, null, Name);
		NameNode.Type = Type;
		return NameNode;
	}

	public FuncCallNode CreateFuncCallNode(BNode ParentNode, BToken SourceToken, String FuncName, BFuncType FuncType) {
		@Var FuncCallNode FuncNode = new FuncCallNode(ParentNode, new BunFuncNameNode(null, SourceToken, FuncName, FuncType));
		FuncNode.Type = FuncType.GetReturnType();
		return FuncNode;
	}

	public final AbstractListNode CreateDefinedFuncCallNode(BNode ParentNode, BToken SourceToken, BFunc Func) {
		@Var AbstractListNode FuncNode = null;
		if(Func instanceof BFormFunc) {
			FuncNode = new BunFormNode(ParentNode, SourceToken, (BFormFunc)Func);
		}
		else {
			FuncNode = this.CreateFuncCallNode(ParentNode, SourceToken, Func.FuncName, Func.GetFuncType());
		}
		//		FuncNode.Type = Func.GetFuncType().GetRealType();
		return FuncNode;
	}

}

