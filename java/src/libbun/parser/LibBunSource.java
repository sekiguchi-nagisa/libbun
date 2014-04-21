package libbun.parser;

import libbun.util.BField;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class LibBunSource {
	@BField public final LibBunLogger Logger;
	@BField String FileName;
	@BField int    LineNumber;
	@BField String  SourceText;

	LibBunSource() {
		this.FileName = null;
		this.LineNumber = 0;
		this.SourceText ="";
		this.Logger = null;
	}

	public LibBunSource(String FileName, int LineNumber, String Source, LibBunLogger Logger) {
		this.FileName = FileName;
		this.LineNumber = LineNumber;
		this.SourceText = Source;
		this.Logger = Logger;
	}

	public final int GetLineNumber(int Position) {
		@Var int LineNumber = this.LineNumber;
		@Var int i = 0;
		while(i < Position) {
			@Var char ch = LibBunSystem._GetChar(this.SourceText, i);
			if(ch == '\n') {
				LineNumber = LineNumber + 1;
			}
			i = i + 1;
		}
		return LineNumber;
	}

	public final int GetLineHeadPosition(int Position) {
		@Var String s = this.SourceText;
		@Var int StartIndex = 0;
		@Var int i = Position;
		if(!(i < s.length())) {
			i = s.length() - 1;
		}
		while(i >= 0) {
			@Var char ch = LibBunSystem._GetChar(s, i);
			if(ch == '\n') {
				StartIndex = i + 1;
				break;
			}
			i = i - 1;
		}
		return StartIndex;
	}

	public final int CountIndentSize(int Position) {
		@Var String s = this.SourceText;
		@Var int length = 0;
		@Var int i = Position;
		while(i < s.length()) {
			@Var char ch = LibBunSystem._GetChar(s, i);
			if(ch == '\t') {
				length = length + 8;
			}
			else if(ch == ' ') {
				length = length + 1;
			}
			else {
				break;
			}
			i = i + 1;
		}
		return length;
	}

	public final String GetLineText(int Position) {
		@Var String s = this.SourceText;
		@Var int StartIndex = 0;
		@Var int EndIndex = s.length();
		@Var int i = Position;
		if(!(i < s.length())) {
			i = s.length() - 1;
		}
		while(i >= 0) {
			@Var char ch = LibBunSystem._GetChar(s, i);
			if(ch == '\n') {
				StartIndex = i + 1;
				break;
			}
			i = i - 1;
		}
		i = Position;
		while(i < s.length()) {
			@Var char ch = LibBunSystem._GetChar(s, i);
			if(ch == '\n') {
				EndIndex = i;
				break;
			}
			i = i + 1;
		}
		return s.substring(StartIndex, EndIndex);
	}

	public final String GetLineMarker(int Position) {
		@Var String s = this.SourceText;
		@Var int StartIndex = 0;
		@Var int i = Position;
		if(!(i < s.length())) {
			i = s.length() - 1;
		}
		while(i >= 0) {
			@Var char ch = LibBunSystem._GetChar(s, i);
			if(ch == '\n') {
				StartIndex = i + 1;
				break;
			}
			i = i - 1;
		}
		@Var String Line = "";
		i = StartIndex;
		while(i < Position) {
			@Var char ch = LibBunSystem._GetChar(s, i);
			if(ch == '\n') {
				break;
			}
			if(ch == '\t') {
				Line = Line + "\t";
			}
			else {
				Line = Line + " ";
			}
			i = i + 1;
		}
		return Line + "^";
	}

	public final String FormatErrorHeader(String Error, int Position, String Message) {
		return "(" + this.FileName + ":" + this.GetLineNumber(Position) + ") [" + Error +"] " + Message;
	}

	public final String FormatErrorMarker(String Error, int Position, String Message) {
		@Var String Line = this.GetLineText(Position);
		@Var String Delim = "\n\t";
		if(Line.startsWith("\t") || Line.startsWith(" ")) {
			Delim = "\n";
		}
		@Var String Header = this.FormatErrorHeader(Error, Position, Message);
		@Var String Marker = this.GetLineMarker(Position);
		Message = Header + Delim + Line + Delim + Marker;
		return Message;
	}

	public final char GetCharAt(int n) {
		if(0 <= n && n < this.SourceText.length()) {
			return LibBunSystem._GetChar(this.SourceText, n);
		}
		return '\0';
	}


}
