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

package libbun.ast.binary;

import libbun.ast.BNode;
import libbun.lang.bun.BunPrecedence;
import libbun.parser.BTokenContext;
import libbun.util.BField;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public abstract class BinaryOperatorNode extends BNode {
	public final static int _Left = 0;
	public final static int _Right = 1;
	@BField public int Precedence = 0;

	public BinaryOperatorNode(BNode ParentNode, int Precedence) {
		super(ParentNode, 2);
		this.Precedence = Precedence;
	}

	public abstract String GetOperator();

	public final BNode LeftNode() {
		return this.AST[BinaryOperatorNode._Left];
	}

	public final BNode SetLeftNode(BNode LeftNode) {
		this.SetNode(BinaryOperatorNode._Left, LeftNode);
		return LeftNode;
	}

	public final BNode RightNode() {
		return this.AST[BinaryOperatorNode._Right];
	}

	public final BNode SetRightNode(BNode RightNode) {
		this.SetNode(BinaryOperatorNode._Right, RightNode);
		return RightNode;
	}

	public final boolean IsRightJoin(int left, int right) {
		return (left < right || (left == right && !LibBunSystem._IsFlag(left, BunPrecedence._LeftJoin) && !LibBunSystem._IsFlag(right, BunPrecedence._LeftJoin)));
	}

	private final boolean IsRightJoin(BNode RightNode) {
		if(RightNode instanceof BinaryOperatorNode) {
			return this.IsRightJoin(this.Precedence, ((BinaryOperatorNode)RightNode).Precedence);
		}
		return false;
	}

	private final BNode RightJoin(BinaryOperatorNode RightNode) {
		@Var BNode RightLeftNode = RightNode.LeftNode();
		if(this.IsRightJoin(RightLeftNode)) {
			RightNode.SetNode(BinaryOperatorNode._Left, this.RightJoin((BinaryOperatorNode) RightLeftNode));
		}
		else {
			RightNode.SetNode(BinaryOperatorNode._Left, this);
			this.SetNode(BinaryOperatorNode._Right, RightLeftNode);
		}
		return RightNode;
	}

	public BNode SetRightBinaryNode(BNode RightNode) {
		if(this.IsRightJoin(RightNode)) {
			return this.RightJoin((BinaryOperatorNode) RightNode);
		}
		this.SetRightNode(RightNode);
		return this;
	}


	private final BNode RightJoin(BNode ParentNode, BinaryOperatorNode RightNode) {
		@Var BNode RightLeftNode = RightNode.LeftNode();
		if(this.IsRightJoin(RightLeftNode)) {
			RightNode.SetNode(BinaryOperatorNode._Left, this.RightJoin(ParentNode, (BinaryOperatorNode) RightLeftNode));
		}
		else {
			RightNode.SetNode(BinaryOperatorNode._Left, this);
			this.SetNode(BinaryOperatorNode._Right, RightLeftNode);
		}
		return RightNode;
	}


	//	public final BNode AppendParsedRightNode(BNode ParentNode, BTokenContext TokenContext) {
	//		@Var BNode RightNode = TokenContext.ParsePattern(ParentNode, "$Expression$", BTokenContext._Required);
	//		if(RightNode.IsErrorNode()) {
	//			return RightNode;
	//		}
	//		if(this.IsRightJoin(RightNode)) {
	//			return this.RightJoin(ParentNode, (BinaryOperatorNode) RightNode);
	//		}
	//		this.SetNode(BinaryOperatorNode._Right, RightNode);
	//		return this;
	//	}

	public final BNode SetParsedNode(BNode ParentNode, BNode LeftNode, String Op, BTokenContext TokenContext) {
		return this.SetParsedNode(ParentNode, LeftNode, Op, TokenContext, "$Expression$");
	}

	public final BNode SetParsedNode(BNode ParentNode, BNode LeftNode, String Op, BTokenContext TokenContext, String Pattern) {
		this.SetLeftNode(LeftNode);
		TokenContext.MatchToken(this, Op, BTokenContext._Required);
		@Var BNode RightNode = TokenContext.ParsePattern(ParentNode, Pattern, BTokenContext._Required);
		if(RightNode.IsErrorNode()) {
			return RightNode;
		}
		if(this.IsRightJoin(RightNode)) {
			return this.RightJoin(ParentNode, (BinaryOperatorNode) RightNode);
		}
		this.SetRightNode(RightNode);
		return this;
	}

	public final boolean IsDifferentlyTyped() {
		return !(this.GetAstType(BinaryOperatorNode._Left).Equals(this.GetAstType(BinaryOperatorNode._Right)));
	}

}