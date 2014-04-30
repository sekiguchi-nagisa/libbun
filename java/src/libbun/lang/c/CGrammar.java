package libbun.lang.c;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.EmptyNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunPrototypeNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunTypeNode;
import libbun.ast.statement.BunBreakNode;
import libbun.ast.statement.BunIfNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.sugar.BunContinueNode;
import libbun.lang.bun.BunGrammar;
import libbun.lang.bun.extra.BunExtraGrammar;
import libbun.parser.BPatternToken;
import libbun.parser.BSourceContext;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunGamma;
import libbun.parser.LibBunSyntax;
import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BMatchFunction;
import libbun.util.BTokenFunction;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class CGrammar {
	public static BMatchFunction FuncDeclPattern = new FuncDeclPatternFunction();
	public static BMatchFunction VarDeclPattern = new VarDeclPatternFunction();
	public static BMatchFunction BlockPattern = new BlockPatternFunction();
	
	public static void LoadGrammar(LibBunGamma Gamma) {
		Gamma.Generator.RootParser.Init();
		// define token func
		Gamma.DefineToken(" \t", BunGrammar.WhiteSpaceToken);
		Gamma.DefineToken("\n",  BunGrammar.NewLineToken);
		Gamma.DefineToken("{}()[]<>.,;?:+-*/%=&|!@~^$", BunGrammar.OperatorToken);
		Gamma.DefineToken("Aa_", BunGrammar.NameToken);
		Gamma.DefineToken("/", BunGrammar.BlockComment); 
		
		Gamma.DefineToken("'", new CharLiteralTokenFunction());
		Gamma.DefineToken("\"",  new StringLiteralTokenFunction());
		Gamma.DefineToken("1",  BunGrammar.NumberLiteralToken);

		// operator definition
		Gamma.DefineExpression("+", BunGrammar.PlusPattern);
		Gamma.DefineExpression("-", BunGrammar.MinusPattern);
		Gamma.DefineExpression("~", BunGrammar.ComplementPattern);
		Gamma.DefineExpression("!", BunGrammar.NotPattern);

		Gamma.DefineBinaryOperator("=",  BunGrammar.AssignPattern);
		Gamma.DefineBinaryOperator("==", BunGrammar.EqualsPattern);
		Gamma.DefineBinaryOperator("!=", BunGrammar.NotEqualsPattern);
		Gamma.DefineBinaryOperator("<",  BunGrammar.LessThanPattern);
		Gamma.DefineBinaryOperator("<=", BunGrammar.LessThanEqualsPattern);
		Gamma.DefineBinaryOperator(">",  BunGrammar.GreaterThanPattern);
		Gamma.DefineBinaryOperator(">=", BunGrammar.GreaterThanEqualsPattern);

		Gamma.DefineBinaryOperator("+", BunGrammar.AddPattern);
		Gamma.DefineBinaryOperator("-", BunGrammar.SubPattern);
		Gamma.DefineBinaryOperator("*", BunGrammar.MulPattern);
		Gamma.DefineBinaryOperator("/", BunGrammar.DivPattern);
		Gamma.DefineBinaryOperator("%", BunGrammar.ModPattern);

		Gamma.DefineBinaryOperator("<<", BunGrammar.LeftShiftPattern);
		Gamma.DefineBinaryOperator(">>", BunGrammar.RightShiftPattern);

		Gamma.DefineBinaryOperator("&", BunGrammar.BitwiseAndPattern);
		Gamma.DefineBinaryOperator("|", BunGrammar.BitwiseOrPattern);
		Gamma.DefineBinaryOperator("^", BunGrammar.BitwiseXorPattern);

		Gamma.DefineBinaryOperator("&&",  BunGrammar.AndPattern);
		Gamma.DefineBinaryOperator("||",  BunGrammar.OrPattern);

		Gamma.DefineBinaryOperator("+", BunExtraGrammar.SelfAddPattern);
		Gamma.DefineBinaryOperator("-", BunExtraGrammar.SelfSubPattern);
		Gamma.DefineBinaryOperator("*", BunExtraGrammar.SelfMulPattern);
		Gamma.DefineBinaryOperator("/", BunExtraGrammar.SelfDivPattern);

		Gamma.DefineBinaryOperator("%",  BunExtraGrammar.SelfModPattern);
		Gamma.DefineBinaryOperator("&",  BunExtraGrammar.SelfBitwiseAndPattern);
		Gamma.DefineBinaryOperator("|",  BunExtraGrammar.SelfBitwiseOrPattern);
		Gamma.DefineBinaryOperator("^",  BunExtraGrammar.SelfBitwiseXorPattern);
		Gamma.DefineBinaryOperator("<<", BunExtraGrammar.SelfLeftShiftPattern);
		Gamma.DefineBinaryOperator(">>", BunExtraGrammar.SelfRightShiftPattern);

		Gamma.DefineExpression("$IntegerLiteral$", BunGrammar.IntLiteralPattern);
		Gamma.DefineExpression("$FloatLiteral$", BunGrammar.FloatLiteralPattern);
		Gamma.DefineExpression("$StringLiteral$", BunGrammar.StringLiteralPattern);
		Gamma.DefineExpression("$CharLiteral$", new CharLiteralPatternFunction());

		DefineStatement(Gamma, "$Statement$", new StatementPatternFunction());
		DefineStatement(Gamma, "$Statement_Block$", new StatementWithinBlockPatternFunction());
		DefineStatement(Gamma, "$Label$", new LabelPatternFunction());
		DefineStatement(Gamma, "$FuncDecl$", FuncDeclPattern);
		DefineStatement(Gamma, "inline", FuncDeclPattern);
		DefineStatement(Gamma, "static", FuncDeclPattern);
		Gamma.DefineExpression("$ParamDecl$", new ParamDeclPatternFunction());
		DefineStatement(Gamma, "$VarDecl$", VarDeclPattern);
		DefineStatement(Gamma, "static", VarDeclPattern);
		DefineStatement(Gamma, "$Block$", BlockPattern);
		DefineStatement(Gamma, "{", BlockPattern);
		Gamma.DefineExpression("$Expression$", BunGrammar.ExpressionPattern);
		Gamma.DefineExpression("$RightExpression$", BunGrammar.RightExpressionPattern);
		Gamma.DefineExpression("$SymbolExpression$", BunGrammar.SymbolExpressionPattern);
		Gamma.DefineExpression("$Name$", BunGrammar.NamePattern);

		// define statement pattern
		DefineStatement(Gamma, ";", new SemicolonPatternFunction());
		DefineStatement(Gamma, "return", new ReturnPatternFunction());
		DefineStatement(Gamma, "break", new BreakPatternFunction());
		DefineStatement(Gamma, "continue", new ContinuePatternFunction());
		DefineStatement(Gamma, "goto", new GotoPatternFunction());
		DefineStatement(Gamma, "if", new IfPatternFunction());
		
		Gamma.DefineExpressionSuffix("->", BunGrammar.GetFieldPattern);
		Gamma.DefineExpressionSuffix(".", BunGrammar.GetFieldPattern);
		Gamma.DefineExpressionSuffix("[", BunGrammar.GetIndexPattern);
		Gamma.DefineExpression("(", BunGrammar.GroupPattern);
		Gamma.DefineExpression("(", BunGrammar.CastPattern);
		Gamma.DefineExpressionSuffix("(", BunGrammar.FuncCallPattern);

		Gamma.Generator.LangInfo.AppendGrammarInfo("c");
	}

	private static void DefineStatement(LibBunGamma Gamma, String PatternName, BMatchFunction MatchFunc) {
		Gamma.Generator.RootParser.DefineStatement(PatternName, MatchFunc);
	}

	public static BToken MatchArray(BTokenContext TokenContext) {
		BToken OpenBracketToken = TokenContext.GetToken(BTokenContext._Required);
		BToken CloseBracketToken = null;
		while(TokenContext.HasNext()) {
			BToken Token = TokenContext.GetToken();
			if(Token.EqualsText("]")) {
				CloseBracketToken = Token;
				break;
			}
			TokenContext.MoveNext();
		}
		if(CloseBracketToken == null) {
			return null;
		}
		return new BToken(TokenContext.SourceContext.Source, OpenBracketToken.StartIndex, CloseBracketToken.EndIndex);
	}

	public static BNode MatchTypeAndName(BTokenContext TokenContext, BNode Node, int TypeInfo, int NameInfo, String[] endSymbols, boolean RequireArray) {
		BArray<BToken> TokenList = new BArray<BToken>(new BToken[5]);
		int ArrayCount = 0;
		while(TokenContext.HasNext()) {	// TODO: function pointer handling
			BToken Token = TokenContext.GetToken();
			if(Token.IsNameSymbol() || Token.EqualsText("*")) {
				TokenList.add(Token);
				TokenContext.MoveNext();
				continue;
			}
			else if(Token.EqualsText("[") && RequireArray) {
				BToken ArrayToken = CGrammar.MatchArray(TokenContext);
				if(ArrayToken == null) {
					return new ErrorNode(Node.ParentNode, "unclosed [");
				}
				ArrayCount++;
				TokenList.add(ArrayToken);
				TokenContext.MoveNext();
				continue;
			}
			else if(CGrammar.MatchDeclEnd(Token.GetText(), endSymbols)) {
				break;
			}
			return new ErrorNode(Node.ParentNode, "require type: " + Token.GetText());
		}
		if(TokenList.size() < 2) {
			return new ErrorNode(Node.ParentNode, "require type");
		}
		BToken NameToken = BArray.GetIndex(TokenList, TokenList.size() - 1 - ArrayCount);
		int StartIndex = BArray.GetIndex(TokenList, 0).StartIndex;
		int EndIndex = BArray.GetIndex(TokenList, TokenList.size() - 2 - ArrayCount).EndIndex;
		String TypeSymbol = new BToken(TokenContext.SourceContext.Source, StartIndex, EndIndex).GetText();
		if(ArrayCount != 0) {
			int Size =TokenList.size();
			for(int i = Size - ArrayCount; i < Size; i++) {
				TypeSymbol += BArray.GetIndex(TokenList, i).GetText();
			}
		}
		BType Type = Node.ParentNode.GetGamma().GetType(TypeSymbol, null, true);
		Node.SetNode(TypeInfo, new BunTypeNode(Node, null, Type));
		Node.SetNode(NameInfo, new GetNameNode(Node, NameToken, NameToken.GetText()));
		return Node;
	}

	private static boolean MatchDeclEnd(String symbol, String[] endSymbols) {
		for(int i = 0; i < endSymbols.length; i++) {
			if(symbol.equals(endSymbols[i])) {
				return true;
			}
		}
		return false;
	}

	public static BNode DispatchStatementPattern(BNode ParentNode, BTokenContext TokenContext) {
		BToken Token = TokenContext.GetToken();
		if(Token instanceof BPatternToken) {
			return null;
		}
		LibBunSyntax Pattern = ParentNode.GetGamma().GetSyntaxPattern(Token.GetText());
		if(Pattern == null || !Pattern.IsStatement()) {
			return null;
		}
		return TokenContext.ApplyMatchPattern(ParentNode, null, Pattern, BTokenContext._Required);
	}
}

class StringLiteralTokenFunction extends BTokenFunction {
	@Override
	public boolean Invoke(BSourceContext SourceContext) {
		int StartIndex = SourceContext.GetPosition();
		SourceContext.MoveNext();
		while(SourceContext.HasChar()) {
			char ch = SourceContext.GetCurrentChar();
			if(ch == '\"') {
				SourceContext.MoveNext(); // eat '"'
				SourceContext.Tokenize("$StringLiteral$", StartIndex, SourceContext.GetPosition());
				return true;
			}
			else if(ch == '\n') {
				break;
			}
			else if(ch == '\\') {
				SourceContext.MoveNext();
			}
			SourceContext.MoveNext();
		}
		SourceContext.LogWarning(StartIndex, "unclosed \"");
		SourceContext.Tokenize("$StringLiteral$", StartIndex, SourceContext.GetPosition());
		return false;
	}
}

class CharLiteralTokenFunction extends BTokenFunction {
	@Override
	public boolean Invoke(BSourceContext SourceContext) {
		int StartIndex = SourceContext.GetPosition();
		SourceContext.MoveNext();
		SourceContext.MoveNext();
		if(SourceContext.GetCurrentChar() == '\'') {
			SourceContext.MoveNext(); // eat '"'
			SourceContext.Tokenize("$CharLiteral$", StartIndex, SourceContext.GetPosition());
			return true;
		}
		SourceContext.LogWarning(StartIndex, "unclosed \"");
		SourceContext.Tokenize("$CharLiteral$", StartIndex, SourceContext.GetPosition());
		return false;
	}
}

class CharLiteralPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
		return new CharNode(ParentNode, Token, LibBunSystem._UnquoteString(Token.GetText()));
	}
}

class LabelPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new LabelNode(ParentNode);
		Node = TokenContext.MatchPattern(Node, LabelNode._Label, "$Name$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ":", BTokenContext._Required);
		return Node;
	}
}

class StatementPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = TokenContext.ParsePattern(ParentNode, "$FuncDecl$", BTokenContext._Optional);
		if(Node != null) {
			return Node;
		}
		Node = TokenContext.ParsePattern(ParentNode, "$VarDecl$", BTokenContext._Required);
		return Node;
	}
}

class FuncDeclPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new BunFunctionNode(ParentNode);
		Node = CGrammar.MatchTypeAndName(TokenContext, (BunFunctionNode) Node, BunFunctionNode._TypeInfo, BunFunctionNode._NameInfo, new String[]{"("}, false);
		Node = TokenContext.MatchNtimes(Node, "(", "$ParamDecl$", ",", ")");
		Node = TokenContext.MatchPattern(Node, BunFunctionNode._Block, "$Block$", BTokenContext._Optional);
		if(!Node.IsErrorNode() && Node.AST[BunFunctionNode._Block] == null) {
			Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
			if(!Node.IsErrorNode()) {
				return new BunPrototypeNode((BunFunctionNode) Node);
			}
		}
		return Node;
	}
}

class ParamDeclPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BToken Token = TokenContext.GetToken();
		BNode ParamNode = new BunLetVarNode(ParentNode, BunLetVarNode._IsReadOnly, null, null);
		if(Token.EqualsText(".")) {
			if(Token.IsNextWhiteSpace()) {
				return null;
			}
			TokenContext.MoveNext();
			BToken NextToken = TokenContext.GetToken();
			if(!NextToken.EqualsText(".") || NextToken.IsNextWhiteSpace()) {
				return null;
			}
			TokenContext.MoveNext();
			NextToken = TokenContext.GetToken(BTokenContext._MoveNext);
			if(!NextToken.EqualsText(".")) {
				return null;
			}
			BToken VarArgToken = new BToken(TokenContext.SourceContext.Source, Token.StartIndex, NextToken.EndIndex);
			ParamNode.SetNode(BunLetVarNode._NameInfo, new GetNameNode(ParamNode, VarArgToken, VarArgToken.GetText()));
			return ParamNode;
		}
		else if(Token.EqualsText("void")) {
			ParamNode.SetNode(BunLetVarNode._TypeInfo, new BunTypeNode(ParamNode, null, BType.VoidType));
			return ParamNode;
		}
		else {
			return CGrammar.MatchTypeAndName(TokenContext, ParamNode, BunLetVarNode._TypeInfo, BunLetVarNode._NameInfo, new String[]{",", ")"}, true);
		}
	}
}

// TODO: multiple declaration
class VarDeclPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new BunLetVarNode(ParentNode, 0, null, null);
		Node = CGrammar.MatchTypeAndName(TokenContext, Node, BunLetVarNode._TypeInfo, BunLetVarNode._NameInfo, new String[]{"=", ",", ";"}, true);
		if(TokenContext.MatchToken(";")) {
			return Node;
		}
		else if(TokenContext.MatchToken("=")) {
			Node = TokenContext.MatchPattern(Node, BunLetVarNode._InitValue, "$Expression$", BTokenContext._Required);
			Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
		}
		else if(TokenContext.MatchToken(",")) {
			//TODO: multiple declaration
		}
		return null;
	}
}

class BlockPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode BlockNode = null;
		if(LeftNode instanceof BunBlockNode) {
			BlockNode = LeftNode;  // @see var
		}
		else {
			BlockNode = new BunBlockNode(ParentNode, null);
		}
		@Var int SkipStopIndent = TokenContext.GetToken().GetIndentSize();
		BlockNode = TokenContext.MatchToken(BlockNode, "{", BTokenContext._Required);
		if(!BlockNode.IsErrorNode()) {
			@Var boolean Remembered = TokenContext.SetParseFlag(BTokenContext._AllowSkipIndent); // init
			while(TokenContext.HasNext()) {
				if(TokenContext.MatchToken("}")) {
					break;
				}
				@Var BNode BlockNode2 = TokenContext.MatchPattern(BlockNode, BNode._AppendIndex, "$Statement_Block$", BTokenContext._Required);
				if(BlockNode2.IsErrorNode()) {
					BlockNode.SetNode(BNode._AppendIndex, BlockNode2);
					while(TokenContext.HasNext()) {
						@Var BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
						if(Token.EqualsText('}')) {
							//System.out.println("INDENT: " + Token.GetIndentSize());
							if(Token.GetIndentSize() == SkipStopIndent) {
								break;
							}
						}
					}
					break;
				}
				BlockNode = BlockNode2;
			}
			TokenContext.SetParseFlag(Remembered);
		}
		return BlockNode;
	}
}

class StatementWithinBlockPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = TokenContext.ParsePattern(ParentNode, "$VarDecl$", BTokenContext._Optional);
		if(Node != null) {
			return Node;
		}
		Node = TokenContext.ParsePattern(ParentNode, "$Label$", BTokenContext._Optional);
		if(Node != null) {
			return Node;
		}
		Node = CGrammar.DispatchStatementPattern(ParentNode, TokenContext);
		if(Node != null) {
			return Node;
		}
		Node = TokenContext.ParsePattern(ParentNode, "$Expression$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
		return Node;
	}
}

// statement define
class SemicolonPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new EmptyNode(ParentNode);
		Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
		return Node;
	}
}

class ReturnPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode Node = new BunReturnNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "return", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, BunReturnNode._Expr, "$Expression$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
		return Node;
	}
}

class BreakPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode Node = new BunBreakNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "break", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
		return Node;
	}
}

class ContinuePatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode Node = new BunContinueNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "continue", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
		return Node;
	}
}

class IfPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode IfNode = new BunIfNode(ParentNode);
		IfNode = TokenContext.MatchToken(IfNode, "if", BTokenContext._Required);
		IfNode = TokenContext.MatchToken(IfNode, "(", BTokenContext._Required);
		IfNode = TokenContext.MatchPattern(IfNode, BunIfNode._Cond, "$Expression$", BTokenContext._Required, BTokenContext._AllowNewLine);
		IfNode = TokenContext.MatchToken(IfNode, ")", BTokenContext._Required);
		IfNode = TokenContext.MatchPattern(IfNode, BunIfNode._Then, "$Statement_Block$", BTokenContext._Required);
		if(TokenContext.MatchNewLineToken("else")) {
			if(TokenContext.IsNewLineToken("if")) {
				IfNode = TokenContext.MatchPattern(IfNode, BunIfNode._Else, "if", BTokenContext._Required);
			}
			else {
				IfNode = TokenContext.MatchPattern(IfNode, BunIfNode._Else, "$Statement_Block$", BTokenContext._Required);
			}
		}
		return IfNode;
	}
}

class GotoPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new JumpNode(ParentNode);
		Node = TokenContext.MatchToken(Node, "goto", BTokenContext._Required);
		Node = TokenContext.MatchPattern(Node, JumpNode._Label, "$Name$", BTokenContext._Required);
		Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
		return Node;
	}
}
