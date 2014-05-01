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

package libbun.ast.expression;

import libbun.ast.BNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.encode.LibBunGenerator;
import libbun.parser.BToken;
import libbun.parser.LibBunVisitor;
import libbun.util.BField;
import libbun.util.Nullable;
import libbun.util.Var;

public class GetNameNode extends MutableNode {
	@BField public String  GivenName;
	@BField @Nullable public BunLetVarNode ResolvedNode = null;
	@BField public int     VarIndex = 0;

	public GetNameNode(BNode ParentNode, BToken SourceToken, String GivenName) {
		super(ParentNode, 0);
		this.SourceToken = SourceToken;
		this.GivenName = GivenName;
	}

	@Override public BNode Dup(boolean TypedClone, BNode ParentNode) {
		@Var GetNameNode NewNode = new GetNameNode(ParentNode, this.SourceToken, this.GivenName);
		if(TypedClone) {
			NewNode.IsImmutable = this.IsImmutable;
			NewNode.ResolvedNode = this.ResolvedNode;
		}
		return this.DupField(TypedClone, NewNode);
	}

	@Override public final String toString() {
		if(this.IsUntyped()) {
			return this.GivenName;
		}
		else {
			return this.GivenName + ":" + this.Type.GetName();
		}
	}

	@Override public void Accept(LibBunVisitor Visitor) {
		Visitor.VisitGetNameNode(this);
	}

	public final String GetUniqueName(LibBunGenerator Generator) {
		if(this.ResolvedNode != null) {
			return this.ResolvedNode.GetUniqueName(Generator);
		}
		return this.GivenName;
	}


}