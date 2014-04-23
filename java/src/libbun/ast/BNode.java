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


package libbun.ast;

import libbun.ast.decl.BunFunctionNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunBooleanNode;
import libbun.ast.statement.BunIfNode;
import libbun.ast.statement.BunWhileNode;
import libbun.encode.LibBunGenerator;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunGamma;
import libbun.parser.LibBunSource;
import libbun.parser.LibBunTypeChecker;
import libbun.parser.LibBunVisitor;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.BField;
import libbun.util.LibBunSystem;
import libbun.util.Nullable;
import libbun.util.Var;

public abstract class BNode {
	public final static int _AppendIndex = -1;
	public final static int _Nop =      -3;
	public final static boolean _EnforcedParent = true;
	public final static boolean _PreservedParent = false;

	@BField public BNode   ParentNode;
	@BField public BToken  SourceToken;
	@BField public BNode   AST[] = null;
	@BField public BType   Type = BType.VarType;
	@BField public boolean HasUntyped = true;

	public BNode(@Nullable BNode ParentNode, int Size) {
		assert(this != ParentNode);
		this.ParentNode = ParentNode;
		if(Size > 0) {
			this.AST = LibBunSystem._NewNodeArray(Size);
		}
	}

	protected BNode DupField(boolean TypedClone, BNode NewNode) {
		@Var int i = 0;
		while(i < this.AST.length) {
			if(this.AST[i] != null) {
				NewNode.AST[i] = this.AST[i].Dup(TypedClone, NewNode);
				assert(NewNode.AST[i].getClass() == this.AST[i].getClass());
			}
			else {
				NewNode.AST[i] = null;
			}
			i = i + 1;
		}
		NewNode.SourceToken = this.SourceToken;
		if(TypedClone) {
			NewNode.Type = this.Type;
			NewNode.HasUntyped = this.HasUntyped;
		}
		if(NewNode instanceof AbstractListNode) {
			((AbstractListNode)NewNode).ListStartIndex = ((AbstractListNode)this).ListStartIndex;
		}
		return NewNode;
	}

	//	public abstract BNode Dup(boolean TypedClone, BNode ParentNode);
	public BNode Dup(boolean TypedClone, BNode ParentNode) {
		throw new RuntimeException("TODO: Implement Dup method for " + this.getClass());
	}

	public final String GetSourceLocation() {
		if(this.SourceToken != null) {
			return "(" + this.SourceToken.GetFileName() + ":" + this.SourceToken.GetLineNumber() + ")";
		}
		return null;
	}

	@Override public String toString() {
		@Var String Self = "#" + LibBunSystem._GetClassName(this);
		if(!this.Type.IsVarType()) {
			Self = Self + ":" + this.Type;
		}
		else {
			Self = Self + ":?";
		}
		if(this.AST != null) {
			@Var int i = 0;
			Self = Self + "[";
			while(i < this.AST.length) {
				if(i > 0) {
					Self = Self + ",";
				}
				if(this.AST[i] == null) {
					Self = Self + "null";
				}
				else {
					if(this.AST[i].ParentNode == this) {
						Self = Self + this.AST[i].toString();
					}
					else {
						Self = Self + "*" + LibBunSystem._GetClassName(this.AST[i])+"*";
					}
				}
				i = i + 1;
			}
			Self = Self + "]";
		}
		return Self;
	}

	// AST[]

	public final int GetAstSize() {
		if(this.AST == null) {
			return 0;
		}
		return this.AST.length;
	}

	public final BNode SetChild(BNode Node, boolean EnforcedParent) {
		assert(this != Node);
		if(EnforcedParent || Node.ParentNode == null) {
			Node.ParentNode = this;
		}
		return Node;
	}

	public final BNode SetParent(BNode Node, boolean Enforced) {
		if(Enforced || this.ParentNode == null) {
			this.ParentNode = Node;
		}
		return this;
	}

	public final BNode SetNode(int Index, BNode Node, boolean EnforcedParent) {
		if(Index >= 0) {
			assert(Index < this.GetAstSize());
			this.AST[Index] = this.SetChild(Node, EnforcedParent);
		}
		else if(Index == BNode._AppendIndex) {
			@Var BNode ListNode = this;
			if(ListNode instanceof AbstractListNode) {
				((AbstractListNode)ListNode).Append(Node);
			}
		}
		return Node;
	}

	public final void SetNode(int Index, BNode Node) {
		this.SetNode(Index, Node, BNode._EnforcedParent);
	}

	public final void SetNullableNode(int Index, @Nullable BNode Node) {
		if(Node != null) {
			this.SetNode(Index, Node, BNode._EnforcedParent);
		}
	}

	public final void Append(BNode Node, boolean EnforcedParent) {
		if(this.AST == null) {
			this.AST = LibBunSystem._NewNodeArray(1);
			this.SetNode(0, Node, EnforcedParent);
		}
		else {
			@Var BNode[] newAST = LibBunSystem._NewNodeArray(this.AST.length+1);
			LibBunSystem._ArrayCopy(this.AST, 0, newAST, 0, this.AST.length);
			this.AST = newAST;
			this.SetNode(this.AST.length - 1, Node, EnforcedParent);
		}
	}

	public final void Append(BNode Node) {
		if(Node instanceof ContainerNode) {
			@Var ContainerNode Container = (ContainerNode)Node;
			@Var int i = 0;
			while(i < Container.AST.length) {
				this.Append(Container.AST[i], BNode._EnforcedParent);
				i = i + 1;
			}
		}
		else {
			this.Append(Node, BNode._EnforcedParent);
		}
	}

	public final BType GetAstType(int Index) {
		if(Index < this.AST.length) {
			return this.AST[Index].Type.GetRealType();
		}
		return BType.VoidType;  // to retrieve RecvType
	}

	public final void SetAstType(int Index, BType Type) {
		if(this.AST[Index] != null) {
			this.AST[Index].Type = Type;
		}
	}

	public final BToken GetAstToken(int TokenIndex) {
		if(TokenIndex >= 0 && this.AST[TokenIndex] != null) {
			return this.AST[TokenIndex].SourceToken;
		}
		return this.SourceToken;
	}

	// ParentNode

	public final boolean IsTopLevel() {
		@Var @Nullable BNode Cur = this.ParentNode;
		while(Cur != null) {
			if(Cur instanceof BunFunctionNode) {
				return false;
			}
			Cur = Cur.ParentNode;
		}
		return true;
	}

	@Nullable public final BunFunctionNode GetDefiningFunctionNode() {
		@Var @Nullable BNode Cur = this;
		while(Cur != null) {
			if(Cur instanceof BunFunctionNode) {
				return (BunFunctionNode)Cur;
			}
			Cur = Cur.ParentNode;
		}
		return null;
	}

	@Nullable public final BunBlockNode GetScopeBlockNode() {
		@Var int SafeCount = 0;
		@Var BNode Node = this;
		while(Node != null) {
			if(Node instanceof BunBlockNode) {
				return (BunBlockNode)Node;
			}
			assert(!(Node == Node.ParentNode));
			//System.out.println("node: " + Node.getClass() + ", " + Node.hashCode() + ", " + SafeCount);
			Node = Node.ParentNode;
			if(LibBunSystem.DebugMode) {
				SafeCount = SafeCount + 1;
				assert(SafeCount < 100);
			}
		}
		return null;
	}

	public final LibBunGamma GetGamma() {
		@Var int SafeCount = 0;
		@Var BunBlockNode BlockNode = this.GetScopeBlockNode();
		while(BlockNode.NullableGamma == null) {
			@Var BunBlockNode ParentBlockNode = BlockNode.ParentNode.GetScopeBlockNode();
			BlockNode = ParentBlockNode;
			if(LibBunSystem.DebugMode) {
				SafeCount = SafeCount + 1;
				assert(SafeCount < 100);
			}
		}
		return BlockNode.NullableGamma;
	}

	public final boolean IsErrorNode() {
		return (this instanceof ErrorNode);
	}

	public abstract void Accept(LibBunVisitor Visitor);

	public final boolean IsUntyped() {
		return !(this.Type instanceof BFuncType) && this.Type.IsVarType();
	}

	public final boolean HasUntypedNode() {
		if(this.HasUntyped) {
			if(!this.IsUntyped()) {
				@Var int i = 0;
				while(i < this.GetAstSize()) {
					if(this.AST[i] != null && this.AST[i].HasUntypedNode()) {
						//LibZen._PrintLine("@Untyped " + LibZen._GetClassName(this) + "[" + i + "] " + this.AST[i]);
						return true;
					}
					i = i + 1;
				}
				this.HasUntyped = false;
			}
		}
		return this.HasUntyped;
	}

	// Convenient short cut interface
	public final GetNameNode SetNewGetNameNode(int Index, LibBunTypeChecker Typer, String Name, BType Type) {
		@Var GetNameNode Node = Typer.CreateGetNameNode(null, Name, Type);
		this.SetNode(Index, Node);
		return Node;
	}

	public final BunBlockNode SetNewBlockNode(int Index, LibBunTypeChecker Typer) {
		@Var BunBlockNode Node = Typer.CreateBlockNode(null);
		this.SetNode(Index, Node);
		return Node;
	}

	public final BunWhileNode SetNewWhileNode(int Index, LibBunTypeChecker Typer) {
		@Var BunWhileNode Node = new BunWhileNode(null);
		this.SetNode(Index, Node);
		return Node;

	}

	public final BNode ParseExpression(String SourceText) {
		LibBunGenerator Generator = this.GetGamma().Generator;
		LibBunSource Source = new LibBunSource("(sugar)", 1, SourceText, Generator.Logger);
		BTokenContext TokenContext = new BTokenContext(Generator.RootParser, Generator, Source, 0, SourceText.length());
		return TokenContext.ParsePattern(this, "$Expression$", BTokenContext._Required);
	}

	public final void ReplaceNode(String Name, BNode Node) {
		if(Node instanceof BunBlockNode) {
			BunIfNode IfNode = new BunIfNode(null);  // {block} => if(true) {block}
			IfNode.SetNode(BunIfNode._Cond, new BunBooleanNode(null, null, true));
			IfNode.AST[BunIfNode._Then] = Node;
			Node = IfNode;
		}
		@Var int i = 0;
		while(i < this.GetAstSize()) {
			BNode SubNode = this.AST[i];
			if(SubNode instanceof GetNameNode) {
				@Var String NodeName = ((GetNameNode)SubNode).GivenName;
				if(NodeName.equals(Name)) {
					this.AST[i] = Node;
				}
			}
			else if(SubNode != null) {
				SubNode.ReplaceNode(Name, Node);
			}
			i = i + 1;
		}
	}

}

