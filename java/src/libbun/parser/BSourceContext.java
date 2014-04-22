package libbun.parser;

import libbun.util.BArray;
import libbun.util.BField;
import libbun.util.BTokenFunction;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public final class BSourceContext /*extends LibBunSource*/ {
	@BField public final LibBunSource Source;
	@BField final LibBunParser Parser;
	@BField final BArray<BToken>  ParsedTokenList;
	@BField int SourcePosition = 0;
	@BField final int EndPosition;

	public BSourceContext(LibBunSource Source, int StartIndex, int EndIndex, BTokenContext TokenContext) {
		this.Source = Source;
		this.SourcePosition = StartIndex;
		this.EndPosition = EndIndex;
		this.Parser = TokenContext.Parser;
		this.ParsedTokenList = TokenContext.TokenList;
	}

	public final boolean HasChar() {
		return this.SourcePosition < this.EndPosition;
	}

	public final int GetCharCode() {
		return LibBunSystem._GetTokenMatrixIndex(this.GetCurrentChar());
	}

	public final int GetPosition() {
		return this.SourcePosition;
	}

	public final char GetCurrentChar() {
		return LibBunSystem._GetChar(this.Source.SourceText, this.SourcePosition);
	}

	public final char GetCharAtFromCurrentPosition(int n) {
		if(this.SourcePosition+n < this.EndPosition) {
			return this.Source.GetCharAt(this.SourcePosition+n);
		}
		return '\0';
	}

	public final void MoveNext() {
		this.SourcePosition = this.SourcePosition + 1;
	}

	public final void SkipWhiteSpace() {
		while(this.HasChar()) {
			@Var char ch = this.GetCurrentChar();
			if(ch != ' ' && ch != '\t') {
				break;
			}
			this.MoveNext();
		}
	}

	public final void FoundIndent(int StartIndex, int EndIndex) {
		@Var BToken Token = new BIndentToken(this.Source, StartIndex, EndIndex);
		this.SourcePosition = EndIndex;
		this.ParsedTokenList.add(Token);
	}

	public final void Tokenize(int StartIndex, int EndIndex) {
		this.SourcePosition = EndIndex;
		if(StartIndex < EndIndex && EndIndex <= this.EndPosition) {
			@Var BToken Token = new BToken(this.Source, StartIndex, EndIndex);
			this.ParsedTokenList.add(Token);
		}
	}

	public final void Tokenize(String PatternName, int StartIndex, int EndIndex) {
		this.SourcePosition = EndIndex;
		if(StartIndex <= EndIndex && EndIndex <= this.EndPosition) {
			@Var LibBunSyntax Pattern = this.Parser.GetSyntaxPattern(PatternName);
			if(Pattern == null) {
				@Var BToken Token = new BToken(this.Source, StartIndex, EndIndex);
				LibBunLogger._LogInfo(Token, "unregistered token pattern: " + PatternName);
				this.ParsedTokenList.add(Token);
			}
			else {
				@Var BToken Token = new BPatternToken(this.Source, StartIndex, EndIndex, Pattern);
				this.ParsedTokenList.add(Token);
			}
		}
	}

	public final boolean IsDefinedSyntax(int StartIndex, int EndIndex) {
		if(EndIndex < this.EndPosition) {
			@Var String Token = this.Source.SourceText.substring(StartIndex, EndIndex);
			@Var LibBunSyntax Pattern = this.Parser.GetRightSyntaxPattern(Token);
			if(Pattern != null) {
				return true;
			}
		}
		return false;
	}

	public final void TokenizeDefinedSymbol(int StartIndex) {
		//		@Var int StartIndex = this.SourcePosition;
		@Var int EndIndex = StartIndex + 2;
		while(this.IsDefinedSyntax(StartIndex, EndIndex)) {
			EndIndex = EndIndex + 1;
		}
		this.Tokenize(StartIndex, EndIndex-1);
	}

	public boolean ApplyFunc(BTokenFunction Func) {
		if(LibBunSystem._ApplyTokenFunc(Func, this)) {
			return true;
		}
		return false;
	}

	private final void ApplyTokenFunc(LibBunTokenFuncChain TokenFunc) {
		@Var int RollbackPosition = this.SourcePosition;
		while(TokenFunc != null) {
			this.SourcePosition = RollbackPosition;
			if(LibBunSystem._ApplyTokenFunc(TokenFunc.Func, this)) {
				return;
			}
			TokenFunc = TokenFunc.ParentFunc;
		}
		this.TokenizeDefinedSymbol(RollbackPosition);
	}

	public final boolean DoTokenize() {
		@Var int TokenSize = this.ParsedTokenList.size();
		@Var int CheckPosition = this.SourcePosition;
		while(this.HasChar()) {
			@Var int CharCode = this.GetCharCode();
			@Var LibBunTokenFuncChain TokenFunc = this.Parser.GetTokenFunc(CharCode);
			this.ApplyTokenFunc(TokenFunc);
			if(this.ParsedTokenList.size() > TokenSize) {
				break;
			}
			if(this.SourcePosition == CheckPosition) {
				//LibZen._PrintLine("Buggy TokenFunc: " + TokenFunc);
				this.MoveNext();
			}
		}
		//this.Dump();
		if(this.ParsedTokenList.size() > TokenSize) {
			return true;
		}
		return false;
	}

	public Object Tokenize(BTokenFunction Func) {
		// TODO Auto-generated method stub
		return null;
	}


	public final void LogWarning(int Position, String Message) {
		this.Source.Logger.Report(this.Source.FormatErrorMarker("warning", Position, Message));
	}

}
