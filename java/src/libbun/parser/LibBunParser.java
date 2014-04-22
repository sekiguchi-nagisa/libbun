package libbun.parser;

import libbun.util.BField;
import libbun.util.BMatchFunction;
import libbun.util.BTokenFunction;
import libbun.util.BunMap;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public final class LibBunParser {
	@BField LibBunParser StackedParser;
	@BField LibBunTokenFuncChain[]         TokenMatrix = null;
	@BField BunMap<LibBunSyntax>           SyntaxTable = null;
	@BField BunMap<LibBunSyntax>           BinaryTable = null;

	public LibBunParser(LibBunParser StackedParser) {
		this.StackedParser = StackedParser;
		this.Init();
	}

	public void Init() {
		this.TokenMatrix = LibBunSystem._NewTokenMatrix();
		this.SyntaxTable = new BunMap<LibBunSyntax>(null);
		this.BinaryTable = new BunMap<LibBunSyntax>(null);
	}

	public LibBunParser Pop() {
		return this.StackedParser;
	}

	// TokenMatrix
	public final LibBunTokenFuncChain GetTokenFunc(int ZenChar) {
		return this.TokenMatrix[ZenChar];
	}

	private final LibBunTokenFuncChain JoinParentFunc(BTokenFunction Func, LibBunTokenFuncChain Parent) {
		if(Parent != null && Parent.Func == Func) {
			return Parent;
		}
		return new LibBunTokenFuncChain(Func, Parent);
	}

	public final void AppendTokenFunc(String keys, BTokenFunction TokenFunc) {
		@Var int i = 0;
		while(i < keys.length()) {
			@Var int kchar = LibBunSystem._GetTokenMatrixIndex(LibBunSystem._GetChar(keys, i));
			this.TokenMatrix[kchar] = this.JoinParentFunc(TokenFunc, this.TokenMatrix[kchar]);
			i = i + 1;
		}
	}

	public final LibBunSyntax GetSyntaxPattern(String PatternName) {
		return this.SyntaxTable.GetOrNull(PatternName);
	}

	public final LibBunSyntax GetRightSyntaxPattern(String PatternName) {
		return this.BinaryTable.GetOrNull(PatternName);
	}

	private void AppendSyntaxPattern(BunMap<LibBunSyntax> Table, String PatternName, BMatchFunction MatchFunc, int Flag) {
		@Var int Alias = PatternName.indexOf(" ");
		@Var String Name = PatternName;
		if(Alias != -1) {
			Name = PatternName.substring(0, Alias);
		}
		@Var LibBunSyntax NewPattern = new LibBunSyntax(Name, MatchFunc, Flag);
		NewPattern.ParentPattern = Table.GetOrNull(Name);
		Table.put(PatternName, NewPattern);
		if(Alias != -1) {
			this.AppendSyntaxPattern(Table, PatternName.substring(Alias+1), MatchFunc, Flag);
		}
	}

	public final void DefineStatement(String PatternName, BMatchFunction MatchFunc) {
		this.AppendSyntaxPattern(this.SyntaxTable, PatternName, MatchFunc, LibBunSyntax._Statement);
	}

	public final void DefineExpression(String PatternName, BMatchFunction MatchFunc) {
		this.AppendSyntaxPattern(this.SyntaxTable, PatternName, MatchFunc, 0);
	}

	public final void DefineBinaryOperator(String PatternName, BMatchFunction MatchFunc) {
		this.AppendSyntaxPattern(this.BinaryTable, PatternName, MatchFunc, LibBunSyntax._BinaryOperator);
	}

	public final void DefineExpressionSuffix(String PatternName, BMatchFunction MatchFunc) {
		this.AppendSyntaxPattern(this.BinaryTable, PatternName, MatchFunc, LibBunSyntax._SuffixExpression);
	}



}
