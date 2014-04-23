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

package libbun.ast.decl;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.encode.LibBunGenerator;
import libbun.parser.LibBunVisitor;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.util.BArray;
import libbun.util.BField;
import libbun.util.Var;

public class BunFunctionNode extends AbstractListNode {
	public static final int _NameInfo = 0;
	public static final int _TypeInfo = 1;
	public final static int _Block    = 2;

	@BField public BType  GivenType = null;
	@BField public String GivenName = null;

	@BField public boolean       IsExport = false;
	@BField public BunFunctionNode ParentFunctionNode = null;
	@BField public BFuncType     ResolvedFuncType = null;

	public BunFunctionNode(BNode ParentNode) {
		super(ParentNode, 3);
	}

	@Override public BNode Dup(boolean TypedClone, BNode ParentNode) {
		@Var BunFunctionNode NewNode = new BunFunctionNode(ParentNode);
		NewNode.GivenType = this.GivenType;
		NewNode.GivenName = this.GivenName;
		NewNode.IsExport  = this.IsExport;
		NewNode.ParentFunctionNode = this.ParentFunctionNode;
		NewNode.ResolvedFuncType = this.ResolvedFuncType;
		return this.DupField(TypedClone, NewNode);
	}

	public final BType ReturnType() {
		if(this.GivenType == null) {
			if(this.AST[BunFunctionNode._TypeInfo] != null) {
				this.GivenType = this.AST[BunFunctionNode._TypeInfo].Type;
			}
			else {
				this.GivenType = BType.VarType;
			}
		}
		return this.GivenType;
	}

	public final void SetReturnType(BType Type) {
		this.GivenType = Type;
	}

	public final String FuncName() {
		if(this.GivenName == null && this.AST[BunFunctionNode._NameInfo] != null) {
			this.GivenName = this.AST[BunFunctionNode._NameInfo].SourceToken.GetTextAsName();
		}
		return this.GivenName;
	}

	public final String GetUniqueName(LibBunGenerator Generator) {
		@Var String FuncName = this.FuncName();
		if(FuncName == null) {
			this.GivenName = Generator.NameUniqueSymbol("f");
			FuncName = this.GivenName;
		}
		return FuncName;
	}

	public final BunBlockNode BlockNode() {
		@Var BNode BlockNode = this.AST[BunFunctionNode._Block];
		if(BlockNode instanceof BunBlockNode) {
			return (BunBlockNode)BlockNode;
		}
		assert(BlockNode == null); // this must not happen
		return null;
	}

	@Override public void Accept(LibBunVisitor Visitor) {
		Visitor.VisitFunctionNode(this);
	}

	public final BunLetVarNode GetParamNode(int Index) {
		@Var BNode Node = this.GetListAt(Index);
		if(Node instanceof BunLetVarNode) {
			return (BunLetVarNode)Node;
		}
		return null;
	}

	public final BFuncType GetFuncType() {
		if(this.ResolvedFuncType == null) {
			@Var BArray<BType> TypeList = new BArray<BType>(new BType[this.GetListSize()+2]);
			@Var int i = 0;
			while(i < this.GetListSize()) {
				@Var BunLetVarNode Node = this.GetParamNode(i);
				@Var BType ParamType = Node.DeclType().GetRealType();
				TypeList.add(ParamType);
				i = i + 1;
			}
			TypeList.add(this.ReturnType().GetRealType());
			@Var BFuncType FuncType = BTypePool._LookupFuncType2(TypeList);
			if(!FuncType.IsVarType()) {
				this.ResolvedFuncType = FuncType;
			}
			return FuncType;
		}
		return this.ResolvedFuncType;
	}

	public final String GetSignature() {
		@Var BFuncType FuncType = this.GetFuncType();
		return FuncType.StringfySignature(this.FuncName());
	}

	public final BunFunctionNode Push(BunFunctionNode Parent) {
		this.ParentFunctionNode = Parent;
		return this;
	}

	public final BunFunctionNode Pop() {
		return this.ParentFunctionNode;
	}

	public final boolean IsTopLevelDefineFunction() {
		return (this.Type.IsVoidType());
	}


}