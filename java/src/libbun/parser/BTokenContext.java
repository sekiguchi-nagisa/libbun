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
import libbun.ast.BNode;
import libbun.ast.EmptyNode;
import libbun.ast.error.ErrorNode;
import libbun.encode.LibBunGenerator;
import libbun.util.BArray;
import libbun.util.BField;
import libbun.util.BTokenFunction;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public final class BTokenContext {
	public final static boolean     _Required          = true;
	public final static boolean     _Optional          = false;
	public final static boolean     _AllowSkipIndent   = true;
	public final static boolean     _NotAllowSkipIndent   = false;
	public final static boolean     _AllowNewLine   = true;
	public final static boolean     _MoveNext       = true;

	@BField public LibBunParser    Parser;
	@BField public LibBunGenerator Generator;
	@BField public BSourceContext  SourceContext;
	@BField public BArray<BToken>  TokenList = new BArray<BToken>(new BToken[128]);

	@BField private int CurrentPosition = 0;
	@BField private boolean IsAllowSkipIndent = false;
	@BField public BToken LatestToken = null;
	@BField private LibBunSyntax ApplyingPattern = null;

	@Deprecated
	public BTokenContext(LibBunParser Parser, LibBunGenerator Generator, String FileName, int LineNumber, String SourceText) {
		this.Generator = Generator;
		this.Parser = Parser;
		this.SourceContext = new BSourceContext(new LibBunSource(FileName, LineNumber, SourceText, Generator.Logger), 0, SourceText.length(), this);
	}

	public BTokenContext(LibBunParser Parser, LibBunGenerator Generator, LibBunSource Source, int StartIndex, int EndIndex) {
		this.Generator = Generator;
		this.Parser = Parser;
		this.SourceContext = new BSourceContext(Source, StartIndex, EndIndex, this);
	}

	public BTokenContext SubContext(int StartIndex, int EndIndex) {
		return new BTokenContext(this.Parser, this.Generator, this.SourceContext.Source, StartIndex, EndIndex);
	}

	public boolean SetParseFlag(boolean AllowSkipIndent) {
		@Var boolean OldFlag = this.IsAllowSkipIndent;
		this.IsAllowSkipIndent = AllowSkipIndent;
		return OldFlag;
	}

	private BToken GetBeforeToken() {
		@Var int MovingPos = this.CurrentPosition - 1;
		while(MovingPos >= 0 && MovingPos < this.TokenList.size()) {
			@Var BToken Token = this.TokenList.ArrayValues[MovingPos];
			if(!Token.IsIndent()) {
				return Token;
			}
			MovingPos = MovingPos - 1;
		}
		return this.LatestToken;
	}

	public BNode CreateExpectedErrorNode(BToken SourceToken, String ExpectedTokenText) {
		if(SourceToken == null || SourceToken.IsNull()) {
			SourceToken = this.GetBeforeToken();
			SourceToken = new BToken(SourceToken.Source, SourceToken.EndIndex, SourceToken.EndIndex);
			return new ErrorNode(null, SourceToken, ExpectedTokenText + " is expected");
		}
		return new ErrorNode(null, SourceToken, ExpectedTokenText + " is expected");
	}

	public void Vacume() {
	}

	public BToken ParseTokenBy(BTokenFunction TokenFunc) {
		@Var int ThisIndex = this.TokenList.size();
		this.SourceContext.ApplyFunc(TokenFunc);
		if(this.TokenList.size() > ThisIndex) {
			this.CurrentPosition = ThisIndex + 1;
			return this.TokenList.ArrayValues[ThisIndex];
		}
		return null;
	}

	public final void MoveNext() {
		this.CurrentPosition = this.CurrentPosition + 1;
	}

	public BToken GetToken(boolean EnforceMoveNext) {
		while(true) {
			if(!(this.CurrentPosition < this.TokenList.size())) {
				if(!this.SourceContext.DoTokenize()) {
					break;
				}
			}
			@Var BToken Token = this.TokenList.ArrayValues[this.CurrentPosition];
			if((this.IsAllowSkipIndent) && Token.IsIndent()) {
				this.CurrentPosition = this.CurrentPosition + 1;
			}
			else {
				this.LatestToken = Token;
				if(EnforceMoveNext) {
					this.CurrentPosition = this.CurrentPosition + 1;
				}
				return Token;
			}
		}
		return BToken._NullToken;
	}

	public BToken GetToken() {
		return this.GetToken(false);
	}

	public boolean HasNext() {
		return (this.GetToken() != BToken._NullToken);
	}

	public void SkipIndent() {
		@Var BToken Token = this.GetToken();
		while(Token.IsIndent()) {
			this.CurrentPosition = this.CurrentPosition + 1;
			Token = this.GetToken();
		}
	}

	public final void SkipToken() {
		this.GetToken(BTokenContext._MoveNext);
	}

	public final boolean IsToken(String TokenText) {
		@Var BToken Token = this.GetToken();
		if(Token.EqualsText(TokenText)) {
			return true;
		}
		return false;
	}

	public final boolean IsNewLineToken(String TokenText) {
		@Var int RollbackPos = this.CurrentPosition;
		this.SkipIndent();
		@Var BToken Token = this.GetToken();
		if(Token.EqualsText(TokenText)) {
			return true;
		}
		this.CurrentPosition = RollbackPos;
		return false;
	}

	public final boolean MatchToken(String TokenText) {
		@Var int RollbackPos = this.CurrentPosition;
		@Var BToken Token = this.GetToken(BTokenContext._MoveNext);
		if(Token.EqualsText(TokenText)) {
			return true;
		}
		this.CurrentPosition = RollbackPos;
		return false;
	}

	public final boolean MatchToken(char TokenText) {
		@Var int RollbackPos = this.CurrentPosition;
		@Var BToken Token = this.GetToken(BTokenContext._MoveNext);
		if(Token.EqualsText(TokenText)) {
			return true;
		}
		this.CurrentPosition = RollbackPos;
		return false;
	}

	public final boolean MatchNewLineToken(String TokenText) {
		@Var int RollbackPos = this.CurrentPosition;
		this.SkipIndent();
		@Var BToken Token = this.GetToken(BTokenContext._MoveNext);
		if(Token.EqualsText(TokenText)) {
			return true;
		}
		this.CurrentPosition = RollbackPos;
		return false;
	}

	public BToken ParseLargeToken() {
		@Var BToken Token = this.GetToken(BTokenContext._MoveNext);
		if(Token.IsNextWhiteSpace()) {
			return Token;
		}
		@Var int StartIndex = Token.StartIndex;
		@Var int EndIndex = Token.EndIndex;
		while(this.HasNext() && !Token.IsNextWhiteSpace()) {
			@Var int RollbackPosition = this.CurrentPosition;
			Token = this.GetToken(BTokenContext._MoveNext);
			if(Token.IsIndent() || Token.EqualsText(';') || Token.EqualsText(',')) {
				this.CurrentPosition = RollbackPosition;
				break;
			}
			EndIndex = Token.EndIndex;
		}
		return new BToken(Token.Source, StartIndex, EndIndex);
	}

	public BNode MatchToken(BNode ParentNode, String TokenText, boolean IsRequired) {
		if(!ParentNode.IsErrorNode()) {
			@Var int RollbackPosition = this.CurrentPosition;
			@Var BToken Token = this.GetToken(BTokenContext._MoveNext);
			if(Token.EqualsText(TokenText)) {
				if(ParentNode.SourceToken == null) {
					ParentNode.SourceToken = Token;
				}
			}
			else {
				if(IsRequired) {
					return this.CreateExpectedErrorNode(Token, TokenText);
				}
				else {
					this.CurrentPosition = RollbackPosition;
				}
			}
		}
		return ParentNode;
	}

	public final LibBunSyntax GetApplyingSyntax() {
		return this.ApplyingPattern;
	}

	public final BNode ApplyMatchPattern(BNode ParentNode, BNode LeftNode, LibBunSyntax Pattern, boolean IsRequired) {
		@Var int RollbackPosition = this.CurrentPosition;
		@Var LibBunSyntax CurrentPattern = Pattern;
		@Var BToken TopToken = this.GetToken();
		@Var BNode ParsedNode = null;
		while(CurrentPattern != null) {
			@Var boolean Remembered = this.IsAllowSkipIndent;
			this.CurrentPosition = RollbackPosition;
			this.ApplyingPattern  = CurrentPattern;
			//			System.out.println("B "+Pattern + "," + ParentNode);
			ParsedNode = LibBunSystem._ApplyMatchFunc(CurrentPattern.MatchFunc, ParentNode, this, LeftNode);
			assert(ParsedNode != ParentNode);
			//			System.out.println("E "+ ParsedNode);
			this.ApplyingPattern  = null;
			this.IsAllowSkipIndent = Remembered;
			if(ParsedNode != null && !ParsedNode.IsErrorNode()) {
				return ParsedNode;
			}
			CurrentPattern = CurrentPattern.ParentPattern;
		}
		if(!IsRequired) {
			this.CurrentPosition = RollbackPosition;
			return null;
		}
		if(ParsedNode == null) {
			ParsedNode = this.CreateExpectedErrorNode(TopToken, Pattern.PatternName);
		}
		return ParsedNode;
	}

	public final BNode ParsePatternAfter(BNode ParentNode, BNode LeftNode, String PatternName, boolean IsRequired) {
		@Var LibBunSyntax Pattern = this.Parser.GetSyntaxPattern(PatternName);
		@Var BNode ParsedNode = this.ApplyMatchPattern(ParentNode, LeftNode, Pattern, IsRequired);
		return ParsedNode;
	}

	public final BNode ParsePattern(BNode ParentNode, String PatternName, boolean IsRequired) {
		return this.ParsePatternAfter(ParentNode, null, PatternName, IsRequired);
	}

	public BNode MatchPattern(BNode ParentNode, int Index, String PatternName, boolean IsRequired, boolean AllowSkipIndent) {
		if(!ParentNode.IsErrorNode()) {
			@Var boolean Rememberd = this.SetParseFlag(AllowSkipIndent);
			@Var BNode ParsedNode = this.ParsePattern(ParentNode, PatternName, IsRequired);
			this.SetParseFlag(Rememberd);
			if(ParsedNode != null) {
				if(ParsedNode.IsErrorNode()) {
					return ParsedNode;
				}
				else {
					if(!(ParsedNode instanceof EmptyNode)) {
						ParentNode.SetNode(Index, ParsedNode);
					}
				}
			}
		}
		return ParentNode;
	}

	public BNode MatchPattern(BNode ParentNode, int Index, String PatternName, boolean IsRequired) {
		return this.MatchPattern(ParentNode, Index, PatternName, IsRequired, BTokenContext._NotAllowSkipIndent);
	}

	public BNode MatchOptionaPattern(BNode ParentNode, int Index, boolean AllowNewLine, String TokenText, String PatternName) {
		if(!ParentNode.IsErrorNode()) {
			if(this.MatchToken(TokenText)) {
				return this.MatchPattern(ParentNode, Index, PatternName, BTokenContext._Optional, BTokenContext._NotAllowSkipIndent);
			}
		}
		return ParentNode;
	}

	public BNode MatchNtimes(BNode ParentNode, String StartToken, String PatternName, String DelimToken, String StopToken) {
		@Var boolean Rememberd = this.SetParseFlag(true);
		@Var boolean IsRequired =   BTokenContext._Optional;
		if(StartToken != null) {
			ParentNode = this.MatchToken(ParentNode, StartToken, BTokenContext._Required);
		}
		while(!ParentNode.IsErrorNode()) {
			if(StopToken != null) {
				@Var BToken Token = this.GetToken();
				if(Token.EqualsText(StopToken)) {
					break;
				}
				IsRequired = BTokenContext._Required;
			}
			@Var BNode ParsedNode = this.ParsePattern(ParentNode, PatternName, IsRequired);
			if(ParsedNode == null) {
				break;
			}
			if(ParsedNode.IsErrorNode()) {
				return ParsedNode;
			}
			if(!(ParsedNode instanceof EmptyNode)) {
				ParentNode.SetNode(BNode._AppendIndex, ParsedNode);
			}
			if(DelimToken != null) {
				if(!this.MatchToken(DelimToken)) {
					break;
				}
			}
		}
		if(StopToken != null) {
			ParentNode = this.MatchToken(ParentNode, StopToken, BTokenContext._Required);
		}
		this.SetParseFlag(Rememberd);
		return ParentNode;
	}

	public final boolean StartsWithToken(String TokenText) {
		@Var BToken Token = this.GetToken();
		if(Token.EqualsText(TokenText)) {
			this.CurrentPosition = this.CurrentPosition + 1;
			return true;
		}
		if(Token.StartsWith(TokenText)) {
			Token = new BToken(Token.Source, Token.StartIndex + TokenText.length(), Token.EndIndex);
			this.CurrentPosition = this.CurrentPosition + 1;
			this.TokenList.add(this.CurrentPosition, Token);
			return true;
		}
		return false;
	}

	public final void SkipEmptyStatement() {
		while(this.HasNext()) {
			@Var BToken Token = this.GetToken();
			if(Token.IsIndent() || Token.EqualsText(';')) {
				this.CurrentPosition = this.CurrentPosition + 1;
			}
			else {
				break;
			}
		}
	}

	public final void Dump() {
		@Var int Position = this.CurrentPosition;
		while(Position < this.TokenList.size()) {
			@Var BToken Token = this.TokenList.ArrayValues[Position];
			@Var String DumpedToken = "[";
			DumpedToken = DumpedToken + Position+"] " + Token.toString();
			LibBunSystem._PrintDebug(DumpedToken);
			Position = Position + 1;
		}
	}




}
