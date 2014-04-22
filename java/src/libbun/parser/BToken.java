package libbun.parser;

import libbun.util.BField;
import libbun.util.BIgnored;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class BToken {
	public final static BToken _NullToken = new BToken();

	@BField public final LibBunSource Source;
	@BField public int  StartIndex;
	@BField public int  EndIndex;

	private BToken() {
		this.Source = new LibBunSource();
		this.StartIndex = 0;
		this.EndIndex = 0;
	}

	public BToken(LibBunSource Source, int StartIndex, int EndIndex) {
		this.Source = Source;
		this.StartIndex = StartIndex;
		this.EndIndex = EndIndex;
	}

	public final int size() {
		return this.EndIndex - this.StartIndex;
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


	public final boolean IsNull() {
		return (this == BToken._NullToken);
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
