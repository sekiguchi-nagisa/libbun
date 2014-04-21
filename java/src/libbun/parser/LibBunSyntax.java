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

import libbun.util.BField;
import libbun.util.BMatchFunction;
import libbun.util.LibBunSystem;

public final class LibBunSyntax {
	public final static int _Statement					= 1;
	public final static int _BinaryOperator             = 1 << 1;
	public final static int _SuffixExpression	     	= 1 << 2;

	@BField public String		              PatternName;
	@BField public BMatchFunction             MatchFunc;
	@BField public LibBunSyntax               ParentPattern = null;
	@BField public int				          SyntaxFlag = 0;

	public LibBunSyntax(String PatternName, BMatchFunction MatchFunc, int Flag) {
		this.PatternName = PatternName;
		this.MatchFunc = MatchFunc;
		this.SyntaxFlag = Flag;
	}

	@Override public String toString() {
		return this.PatternName  /* + "{" + this.MatchFunc + "}"*/;
	}

	public final boolean IsStatement() {
		return LibBunSystem._IsFlag(this.SyntaxFlag, LibBunSyntax._Statement);
	}

	public final boolean IsBinaryOperator() {
		return LibBunSystem._IsFlag(this.SyntaxFlag, LibBunSyntax._BinaryOperator);
	}

	public final boolean IsSuffixExpression() {
		return LibBunSystem._IsFlag(this.SyntaxFlag, LibBunSyntax._SuffixExpression);
	}


	//	public final boolean IsBinaryOperator() {
	//		return LibBunSystem._IsFlag(this.SyntaxFlag, LibBunSyntax._BinaryOperator);
	//	}
	//
	//	public final boolean IsRightJoin(LibBunSyntax Right) {
	//		@Var int left = this.SyntaxFlag;
	//		@Var int right = Right.SyntaxFlag;
	//		return (left < right || (left == right && !LibBunSystem._IsFlag(left, LibBunSyntax._LeftJoin) && !LibBunSystem._IsFlag(right, LibBunSyntax._LeftJoin)));
	//	}
	//
	//	public final static LibBunSyntax MergeSyntaxPattern(LibBunSyntax Pattern, LibBunSyntax Parent) {
	//		if(Parent == null) {
	//			return Pattern;
	//		}
	//		@Var LibBunSyntax MergedPattern = new LibBunSyntax(Pattern.PatternName, Pattern.MatchFunc, Pattern.SyntaxFlag);
	//		MergedPattern.ParentPattern = Parent;
	//		return MergedPattern;
	//	}

}
