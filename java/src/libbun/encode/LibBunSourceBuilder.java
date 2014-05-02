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


package libbun.encode;

import libbun.util.BArray;
import libbun.util.BField;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public final class LibBunSourceBuilder {
	@BField public BArray<String> SourceList = new BArray<String>(new String[128]);
	@BField LibBunSourceBuilder Parent;
	@BField LibBunSourceGenerator Template;
	@BField int IndentLevel = 0;
	@BField String CurrentIndentString = "";
	@BField char LastChar = '\n';

	public LibBunSourceBuilder(LibBunSourceGenerator Template, LibBunSourceBuilder Parent) {
		this.Template = Template;
		this.Parent = Parent;
	}

	public final LibBunSourceBuilder Pop() {
		this.AppendLineFeed();
		return this.Parent;
	}

	public final boolean IsEmpty(String Text) {
		return (Text == null || Text.length() == 0);
	}

	public final void Append(String Source) {
		if(!this.IsEmpty(Source)) {
			this.SourceList.add(Source);
			this.LastChar = LibBunSystem._GetChar(Source, Source.length()-1);
		}
	}

	public final void Append(String Text, String Text2) {
		this.SourceList.add(Text);
		this.SourceList.add(Text2);
	}

	public final void Append(String Text, String Text2, String Text3) {
		this.SourceList.add(Text);
		this.SourceList.add(Text2);
		this.SourceList.add(Text3);
	}

	public final void AppendInt(int Value) {
		this.SourceList.add("" + Value);
	}

	public final void AppendQuotedText(String Text) {
		this.SourceList.add(LibBunSystem._QuoteString(Text));
	}

	public final void AppendLineFeed() {
		if(this.LastChar != '\n') {
			this.SourceList.add("\n"/*this.Template.LineFeed*/);
		}
	}

	public final void OpenIndent() {
		this.IndentLevel = this.IndentLevel + 1;
		this.CurrentIndentString = null;
	}

	public final void OpenIndent(String Text) {
		if(Text != null && Text.length() > 0) {
			this.Append(Text);
		}
		this.OpenIndent();
	}

	public final void CloseIndent() {
		this.IndentLevel = this.IndentLevel - 1;
		this.CurrentIndentString = null;
		LibBunSystem._Assert(this.IndentLevel >= 0);
	}

	public final void CloseIndent(String Text) {
		this.CloseIndent();
		if(Text != null && Text.length() > 0) {
			this.AppendNewLine(Text);
		}
	}

	public final int SetIndentLevel(int IndentLevel) {
		int Level = this.IndentLevel;
		this.IndentLevel = IndentLevel;
		this.CurrentIndentString = null;
		return Level;
	}

	private final void AppendIndentString() {
		if (this.CurrentIndentString == null) {
			this.CurrentIndentString = LibBunSystem._JoinStrings("   ", this.IndentLevel);
		}
		this.SourceList.add(this.CurrentIndentString);
	}

	public final void AppendNewLine() {
		this.AppendLineFeed();
		this.AppendIndentString();
	}

	public final void AppendNewLine(String Text) {
		this.AppendNewLine();
		this.Append(Text);
	}

	public final void AppendNewLine(String Text, String Text2) {
		this.AppendNewLine();
		this.Append(Text);
		this.Append(Text2);
	}

	public final void AppendNewLine(String Text, String Text2, String Text3) {
		this.AppendNewLine();
		this.Append(Text);
		this.Append(Text2);
		this.Append(Text3);
	}

	public final boolean EndsWith(char s) {
		return this.LastChar == s;
	}

	public final void AppendWhiteSpace() {
		if(this.LastChar == ' ' || this.LastChar == '\t' || this.LastChar == '\n') {
			return;
		}
		this.SourceList.add(" ");
	}

	public final void AppendWhiteSpace(String Text) {
		this.AppendWhiteSpace();
		this.Append(Text);
	}

	public final void AppendWhiteSpace(String Text, String Text2) {
		this.AppendWhiteSpace();
		this.Append(Text);
		this.Append(Text2);
	}

	public final void AppendWhiteSpace(String Text, String Text2, String Text3) {
		this.AppendWhiteSpace();
		this.Append(Text);
		this.Append(Text2);
		this.Append(Text3);
	}

	public final void AppendCode(String Source) {
		this.LastChar = '\0';
		@Var int StartIndex = 0;
		@Var int i = 0;
		while(i < Source.length()) {
			@Var char ch = LibBunSystem._GetChar(Source, i);
			if(ch == '\n') {
				if(StartIndex < i) {
					this.SourceList.add(Source.substring(StartIndex, i));
				}
				this.AppendNewLine();
				StartIndex = i + 1;
			}
			if(ch == '\t') {
				if(StartIndex < i) {
					this.SourceList.add(Source.substring(StartIndex, i));
				}
				this.Append(this.Template.Tab);
				StartIndex = i + 1;
			}
			i = i + 1;
		}
		if(StartIndex < i) {
			this.SourceList.add(Source.substring(StartIndex, i));
		}
	}

	public final void Clear() {
		this.SourceList.clear(0);
	}

	public final int GetPosition() {
		return this.SourceList.size();
	}

	public final String CopyString(int BeginIndex, int EndIndex) {
		return LibBunSystem._SourceBuilderToString(this, BeginIndex, EndIndex);
	}

	@Override public final String toString() {
		return LibBunSystem._SourceBuilderToString(this);
	}
	////
	////	@Deprecated public final void AppendLine(String Text) {
	////		this.Append(Text);
	////		this.AppendLineFeed();
	//	}

}
