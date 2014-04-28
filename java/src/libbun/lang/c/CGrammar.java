package libbun.lang.c;

import libbun.ast.BNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunPrototypeNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunTypeNode;
import libbun.lang.bun.BunGrammar;
import libbun.lang.bun.extra.BunExtraGrammar;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunGamma;
import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BMatchFunction;

public class CGrammar {
	public static void LoadGrammar(LibBunGamma Gamma) {
		Gamma.Generator.RootParser.Init();
		// define token func
		Gamma.DefineToken(" \t", BunGrammar.WhiteSpaceToken);
		Gamma.DefineToken("\n",  BunGrammar.NewLineToken);
		Gamma.DefineToken("{}()[]<>.,;?:+-*/%=&|!@~^$", BunGrammar.OperatorToken);
		Gamma.DefineToken("Aa_", BunGrammar.NameToken);
		Gamma.DefineToken("/", BunGrammar.BlockComment); 

		Gamma.DefineToken("\"",  BunGrammar.StringLiteralToken);	//TODO: charactor
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

		Gamma.DefineStatement("$Statement$", new StatementPatternFunction());
		Gamma.DefineStatement("$FuncDecl$", new FuncDefinePatternFunction());
		Gamma.DefineExpression("$ParamDecl$", new ParamDeclPatternFunction());
		Gamma.DefineStatement("$Block$", new BlockPatternFunction());

		Gamma.Generator.LangInfo.AppendGrammarInfo("c");
	}
}

class StatementPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = TokenContext.ParsePattern(ParentNode, "$FuncDecl$", BTokenContext._Optional);
		if(Node != null) {
			return Node;
		}
		return null;
	}
}

class FuncDefinePatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BNode Node = new BunFunctionNode(ParentNode);
		Node = this.MatchReturnTypeAndName(TokenContext, (BunFunctionNode) Node);
		Node = TokenContext.MatchNtimes(Node, "(", "$ParamDecl$", ",", ")");
		Node = TokenContext.MatchPattern(Node, BunFunctionNode._Block, "$Block$", BTokenContext._Optional);
		if(Node.AST[BunFunctionNode._Block] == null) {
			Node = TokenContext.MatchToken(Node, ";", BTokenContext._Required);
			if(!Node.IsErrorNode()) {
				return new BunPrototypeNode((BunFunctionNode) Node);
			}
		}
		return Node;
	}
	// TODO: array type, function pointer, variable argument
	private BNode MatchReturnTypeAndName(BTokenContext TokenContext, BunFunctionNode FuncNode) {
		BArray<BToken> TokenList = new BArray<BToken>(new BToken[5]);
		while(TokenContext.HasNext()) {
			BToken Token = TokenContext.GetToken();
			if(Token.IsNameSymbol() || Token.EqualsText("*")) {
				TokenList.add(Token);
				TokenContext.MoveNext();
				continue;
			}
			else if(Token.EqualsText("(")) {	// TODO: function pointer handling
				break;
			}
			return new ErrorNode(FuncNode.ParentNode, "require type");
		}
		if(TokenList.size() < 2) {
			return new ErrorNode(FuncNode.ParentNode, "require type");
		}
		BToken FuncNameToken = BArray.GetIndex(TokenList, TokenList.size() - 1);
		int StartIndex = BArray.GetIndex(TokenList, 0).StartIndex;
		int EndIndex = BArray.GetIndex(TokenList, TokenList.size() - 2).EndIndex;
		BToken ReturnTypeToken = new BToken(TokenContext.SourceContext.Source, StartIndex, EndIndex);
		BType ReturnType = FuncNode.ParentNode.GetGamma().GetType(ReturnTypeToken.GetText(), ReturnTypeToken, true);
		FuncNode.SetNode(BunFunctionNode._TypeInfo, new BunTypeNode(FuncNode, ReturnTypeToken, ReturnType));
		FuncNode.SetNode(BunFunctionNode._NameInfo, new GetNameNode(FuncNode, FuncNameToken, FuncNameToken.GetText()));
		return FuncNode;
	}
}

class ParamDeclPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		BArray<BToken> TokenList = new BArray<BToken>(new BToken[5]);
		while(TokenContext.HasNext()) {
			BToken Token = TokenContext.GetToken();
			if(Token.IsNameSymbol() || Token.EqualsText("*")) {	// TODO: function pointer handling
				TokenList.add(Token);
				TokenContext.MoveNext();
				continue;
			}
			else if(Token.EqualsText(",") || Token.EqualsText(")")) {
				break;
			}
			return new ErrorNode(ParentNode, "require type");
		}
		if(TokenList.size() < 2) {
			return new ErrorNode(ParentNode, "require type");
		}
		BToken ParamNameToken = BArray.GetIndex(TokenList, TokenList.size() - 1);
		int StartIndex = BArray.GetIndex(TokenList, 0).StartIndex;
		int EndIndex = BArray.GetIndex(TokenList, TokenList.size() - 2).EndIndex;
		BToken ParamTypeToken = new BToken(TokenContext.SourceContext.Source, StartIndex, EndIndex);
		BType ParamType = ParentNode.GetGamma().GetType(ParamTypeToken.GetText(), ParamTypeToken, true);
		BNode ParamNode = new BunLetVarNode(ParentNode, BunLetVarNode._IsReadOnly, null, null);
		ParamNode.SetNode(BunLetVarNode._TypeInfo, new BunTypeNode(ParamNode, ParamTypeToken, ParamType));
		ParamNode.SetNode(BunLetVarNode._NameInfo, new GetNameNode(ParamNode, ParamNameToken, ParamNameToken.GetText()));
		return ParamNode;
	}
}

class BlockPatternFunction extends BMatchFunction {

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return null;
	}
	
}
