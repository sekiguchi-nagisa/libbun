package libbun.parser;

import libbun.parser.peg.ParserContext;
import libbun.parser.peg.PegParser;
import libbun.util.BField;
import libbun.util.BIgnored;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class BToken {
	public final static BToken _NullToken = new BToken();

	private BToken() {
		this.Source = new ParserSource();
		this.StartIndex = 0;
		this.EndIndex = 0;
	}

	@BField public final ParserSource Source;
	@BField public int  StartIndex;
	@BField public int  EndIndex;

	public BToken(ParserSource Source, int StartIndex, int EndIndex) {
		this.Source = Source;
		this.StartIndex = StartIndex;
		this.EndIndex = EndIndex;
	}

	public final int size() {
		return this.EndIndex - this.StartIndex;
	}

	public final boolean IsNull() {
		return (this.StartIndex == this.EndIndex);
	}

	public final int indexOf(String s) {
		int loc = this.Source.SourceText.indexOf(s, this.StartIndex);
		if(loc != -1 && loc < this.EndIndex) {
			return loc - this.StartIndex;
		}
		return -1;
	}

	public final String substring(int startIndex) {
		return this.Source.SourceText.substring(startIndex + this.StartIndex, this.EndIndex);
	}

	public final String substring(int startIndex, int endIndex) {
		startIndex = startIndex + this.StartIndex;
		endIndex = endIndex + this.StartIndex;
		if(endIndex <= this.EndIndex) {
			return this.Source.SourceText.substring(startIndex, endIndex);
		}
		return null;
	}

	public final ParserContext newParserContext(PegParser parser) {
		return new ParserContext(parser, this.Source, this.StartIndex, this.EndIndex);
	}

	public final ParserContext newParserContext(PegParser parser, int startIndex) {
		return new ParserContext(parser, this.Source, this.StartIndex + startIndex, this.EndIndex);
	}

	public final ParserContext newParserContext(PegParser parser, int startIndex, int endIndex) {
		endIndex = endIndex + this.StartIndex;
		if(endIndex > this.EndIndex) {
			endIndex = this.EndIndex;
		}
		return new ParserContext(parser, this.Source, this.StartIndex + startIndex, endIndex);
	}




	public final String GetFileName() {
		return this.Source.FileName;
	}

	public final int GetLineNumber() {
		return this.Source.GetLineNumber(this.StartIndex);
	}

	public final char GetChar() {
		if(this.Source != null) {
			return LibBunSystem._GetChar(this.Source.SourceText, this.StartIndex);
		}
		return '\0';
	}

	public final String GetText() {
		if(this.Source != null) {
			return this.Source.SourceText.substring(this.StartIndex, this.EndIndex);
		}
		return "";
	}

	public final String GetTextAsName() {
		return this.GetText();
	}

	@Override public final String toString() {
		@Var char ch = this.Source.GetCharAt(this.StartIndex-1);
		if(ch == '\"') {
			return "\"" + this.GetText() + "\"";
		}
		return this.GetText();
	}

	@BIgnored public final boolean EqualsText(char ch) {
		if(this.EndIndex - this.StartIndex == 1) {
			if(LibBunSystem._GetChar(this.Source.SourceText, this.StartIndex) == ch) {
				return true;
			}
		}
		return false;
	}

	public final boolean EqualsText(String Text) {
		if(Text.length() == this.EndIndex - this.StartIndex) {
			@Var String s = this.Source.SourceText;
			@Var int i = 0;
			while(i < Text.length()) {
				if(LibBunSystem._GetChar(s, this.StartIndex+i) != LibBunSystem._GetChar(Text, i)) {
					return false;
				}
				i = i + 1;
			}
			return true;
		}
		return false;
	}

	public final boolean StartsWith(String Text) {
		if(Text.length() <= this.EndIndex - this.StartIndex) {
			@Var String s = this.Source.SourceText;
			@Var int i = 0;
			while(i < Text.length()) {
				if(LibBunSystem._GetChar(s, this.StartIndex+i) != LibBunSystem._GetChar(Text, i)) {
					return false;
				}
				i = i + 1;
			}
			return true;
		}
		return false;
	}

	public final boolean EndsWith(String Text) {
		@Var int i = this.EndIndex - 1;
		@Var int j = Text.length() - 1;
		@Var String s = this.Source.SourceText;
		while(i >= this.StartIndex && j >= 0) {
			if(LibBunSystem._GetChar(s, i) != LibBunSystem._GetChar(Text, j)) {
				return false;
			}
			i = i - 1;
			j = j - 1;
		}
		return true;
	}


	public final boolean IsIndent() {
		return this instanceof BIndentToken;
	}

	public final boolean IsNextWhiteSpace() {
		@Var char ch = this.Source.GetCharAt(this.EndIndex);
		if(ch == ' ' || ch == '\t' || ch == '\n') {
			return true;
		}
		return false;
	}

	public final boolean IsNameSymbol() {
		@Var char ch = this.Source.GetCharAt(this.StartIndex);
		return LibBunSystem._IsSymbol(ch);
	}

	public final int GetIndentSize() {
		if(this.Source != null) {
			return this.Source.CountIndentSize(this.Source.GetLineHeadPosition(this.StartIndex));
		}
		return 0;
	}

}
