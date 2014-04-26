package libbun.lang.python;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunEqualsNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.statement.BunIfNode;
import libbun.ast.statement.BunWhileNode;
import libbun.ast.sugar.BunForInNode;
import libbun.ast.unary.BunNotNode;
import libbun.ast.unary.UnaryOperatorNode;
import libbun.lang.bun.BunGrammar;
import libbun.lang.bun.extra.BunExtraGrammar;
import libbun.parser.BSourceContext;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunGamma;
import libbun.type.BFuncType;
import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.util.BMatchFunction;
import libbun.util.BTokenFunction;
import libbun.util.Var;

class PythonStringLiteralTokenFunction extends BTokenFunction {
	@Override public boolean Invoke(BSourceContext SourceContext) {
		String PatternName = "$StringLiteral$";
		@Var int StartIndex = SourceContext.GetPosition();
		SourceContext.MoveNext();
		while(SourceContext.HasChar()) {
			@Var char ch = SourceContext.GetCurrentChar();
			if(ch == '\"') {
				SourceContext.MoveNext(); // eat '"'
				SourceContext.Tokenize(PatternName, StartIndex, SourceContext.GetPosition());
				return true;
			}
			if(ch == '\n') {
				break;
			}
			if(ch == '\\') {
				SourceContext.MoveNext();
			}
			SourceContext.MoveNext();
		}
		SourceContext.LogWarning(StartIndex, "unclosed \"");
		SourceContext.Tokenize(PatternName, StartIndex, SourceContext.GetPosition());
		return false;
	}
}

class PythonNotPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode UnaryNode = new BunNotNode(ParentNode);
		UnaryNode = TokenContext.MatchToken(UnaryNode, "not", BTokenContext._Required);
		UnaryNode = TokenContext.MatchPattern(UnaryNode, UnaryOperatorNode._Recv, "$RightExpression$", BTokenContext._Required);
		return UnaryNode;
	}
}
class PythonEqualsPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BinaryOperatorNode BinaryNode = new BunEqualsNode(ParentNode);
		return BinaryNode.SetParsedNode(ParentNode, LeftNode, BinaryNode.GetOperator(), TokenContext);
	}
}

class PythonParamPatternFunction extends BMatchFunction {

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode ParamNode = new BunLetVarNode(ParentNode, BunLetVarNode._IsReadOnly, null, null);
		ParamNode = TokenContext.MatchPattern(ParamNode, BunLetVarNode._NameInfo, "$Name$", BTokenContext._Required);
		return ParamNode;
	}

}

class PythonStatementPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var boolean Remembered = TokenContext.SetParseFlag(BTokenContext._AllowSkipIndent);
		TokenContext.SetParseFlag(BTokenContext._NotAllowSkipIndent);
		@Var BNode StmtNode = BunGrammar._DispatchPattern(ParentNode, TokenContext, null, true);
		TokenContext.SetParseFlag(Remembered);
		return StmtNode;
	}
}

class PythonIfPatternFunction extends BMatchFunction{
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode IfNode = new BunIfNode(ParentNode);
		IfNode = TokenContext.MatchToken(IfNode, "if", BTokenContext._Required);
		IfNode = TokenContext.MatchPattern(IfNode, BunIfNode._Cond, "$Expression$", BTokenContext._Required, BTokenContext._AllowNewLine);
		IfNode = TokenContext.MatchPattern(IfNode, BunIfNode._Then, "$Block$", BTokenContext._Required);
		//FIXME elif
		if(TokenContext.MatchNewLineToken("else")) {
			IfNode = TokenContext.MatchPattern(IfNode, BunIfNode._Else, "$Block$", BTokenContext._Required);
		}
		return IfNode;
	}
}

class PythonFunctionPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode FuncNode = new BunFunctionNode(ParentNode);
		FuncNode = TokenContext.MatchToken(FuncNode, "def", BTokenContext._Required);
		FuncNode = TokenContext.MatchPattern(FuncNode, BunFunctionNode._NameInfo, "$Name$", BTokenContext._Optional);
		FuncNode = TokenContext.MatchNtimes(FuncNode, "(", "$Param$", ",", ")");
		FuncNode = TokenContext.MatchPattern(FuncNode, BunFunctionNode._Block, "$Block$", BTokenContext._Required);
		return FuncNode;
	}
}

class PythonCommentFunction extends BTokenFunction {

	@Override
	public boolean Invoke(BSourceContext SourceContext) {
		while(SourceContext.HasChar()) {
			@Var char ch = SourceContext.GetCurrentChar();
			if(ch == '\n') {
				break;
			}
			SourceContext.MoveNext();
		}
		return true;
	}

}

class PythonBlockPatternFunction extends BMatchFunction {

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode BlockNode = new BunBlockNode(ParentNode, ParentNode.GetGamma());
		@Var BToken SkipToken = TokenContext.GetToken();
		BlockNode = TokenContext.MatchToken(BlockNode, ":", BTokenContext._Required);
		if(!BlockNode.IsErrorNode()) {
			@Var boolean Remembered = TokenContext.SetParseFlag(BTokenContext._AllowSkipIndent); // init
			@Var int IndentSize = 0;
			while(TokenContext.HasNext()) {
				@Var BToken Token = TokenContext.GetToken();
				if(IndentSize > Token.GetIndentSize()) {
					break;
				}
				IndentSize = Token.GetIndentSize();
				BlockNode = TokenContext.MatchPattern(BlockNode, BNode._AppendIndex, "$Statement$", BTokenContext._Required);
				if(BlockNode.IsErrorNode()) {
					//FIXME: SkipError was deprecated
					//TokenContext.SkipError(SkipToken);
					break;
				}
			}
			TokenContext.SetParseFlag(Remembered);
		}
		return BlockNode;
	}

}

class PythonWhilePatternFunction extends BMatchFunction {

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode WhileNode = new BunWhileNode(ParentNode);
		WhileNode = TokenContext.MatchToken(WhileNode, "while", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Cond, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Block, "$Block$", BTokenContext._Required);
		return WhileNode;
	}

}

class PythonForPatternFunction extends BMatchFunction {

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode ForNode = new BunForInNode(ParentNode);
		ForNode = TokenContext.MatchToken(ForNode, "for", BTokenContext._Required);
		//FIXME Type check
		ForNode = TokenContext.MatchPattern(ForNode, BunForInNode._Var, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		ForNode = TokenContext.MatchToken(ForNode, "in", BTokenContext._Required);
		ForNode = TokenContext.MatchPattern(ForNode, BunForInNode._List, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		ForNode = TokenContext.MatchPattern(ForNode, BunForInNode._Block, "$Block$", BTokenContext._Required);
		return ForNode;
	}

}

class PythonWithPatternFunction extends BMatchFunction {

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		//FIXME
		return null;
	}

}

class PythonPassPatternFunction extends BMatchFunction {

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		TokenContext.MatchToken("pass");//(null, "pass", BTokenContext._Required);
		if(TokenContext.IsToken("pass")) {
			TokenContext.MoveNext();
		}
		return null;
	}

}

public class PythonGrammar {

	public static void LoadGrammar(LibBunGamma Gamma) {
		Gamma.Generator.RootParser.Init();
		Gamma.SetTypeName(BType.VoidType,  null);
		Gamma.SetTypeName(BType.BooleanType, null);
		Gamma.SetTypeName(BType.IntType, null);
		Gamma.SetTypeName(BType.FloatType, null);
		Gamma.SetTypeName(BType.StringType, null);
		Gamma.SetTypeName(BGenericType._AlphaType, null);
		Gamma.SetTypeName(BGenericType._ArrayType, null);
		Gamma.SetTypeName(BGenericType._MapType, null);
		Gamma.SetTypeName(BFuncType._FuncType, null);

		Gamma.DefineToken(" \t", BunGrammar.WhiteSpaceToken);
		Gamma.DefineToken("\n",  BunGrammar.NewLineToken);
		Gamma.DefineToken("{}()[]<>.,;?:+-*/%=&|!@~^$", BunGrammar.OperatorToken);
		Gamma.DefineToken("Aa_", BunGrammar.NameToken);

		Gamma.DefineToken("\"", new PythonStringLiteralTokenFunction());
		Gamma.DefineToken("1",  BunGrammar.NumberLiteralToken);

		Gamma.DefineExpression("None",  BunGrammar.NullPattern);
		Gamma.DefineExpression("True",  BunGrammar.TruePattern);
		Gamma.DefineExpression("False", BunGrammar.FalsePattern);

		Gamma.DefineExpression("+", BunGrammar.PlusPattern);
		Gamma.DefineExpression("-", BunGrammar.MinusPattern);
		Gamma.DefineExpression("~", BunGrammar.ComplementPattern);
		Gamma.DefineExpression("not", new PythonNotPatternFunction());

		Gamma.DefineBinaryOperator("=",  BunGrammar.AssignPattern);
		Gamma.DefineBinaryOperator("==", BunGrammar.EqualsPattern);
		Gamma.DefineBinaryOperator("!=", BunGrammar.NotEqualsPattern);
		Gamma.DefineBinaryOperator("<",  BunGrammar.LessThanPattern);
		Gamma.DefineBinaryOperator("<=", BunGrammar.LessThanEqualsPattern);
		Gamma.DefineBinaryOperator(">",  BunGrammar.GreaterThanPattern);
		Gamma.DefineBinaryOperator(">=", BunGrammar.GreaterThanEqualsPattern);
		Gamma.DefineBinaryOperator("is", new PythonEqualsPatternFunction());

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
		Gamma.DefineBinaryOperator("and", BunGrammar.AndPattern);
		Gamma.DefineBinaryOperator("or",  BunGrammar.OrPattern);
		//TODO is, is not
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
		Gamma.DefineExpression("$FloatLiteral$",   BunGrammar.FloatLiteralPattern);
		Gamma.DefineExpression("$StringLiteral$",  BunGrammar.StringLiteralPattern);

		Gamma.DefineExpressionSuffix(".", BunGrammar.GetFieldPattern);
		Gamma.DefineExpressionSuffix(".", BunGrammar.MethodCallPattern);

		Gamma.DefineExpressionSuffix("(", BunGrammar.FuncCallPattern);
		Gamma.DefineExpression("(", BunGrammar.GroupPattern);

		Gamma.DefineExpressionSuffix("[", BunGrammar.GetIndexPattern);
		Gamma.DefineExpression("[", BunGrammar.ArrayLiteralPattern);
		Gamma.DefineExpression("$MapEntry$", BunGrammar.MapEntryPattern);
		Gamma.DefineExpression("{", BunGrammar.MapLiteralPattern);

		//Gamma.DefineExpression("$Annotation$", AnnotationPattern); //FIXME
		Gamma.DefineExpression("$SymbolExpression$", BunGrammar.SymbolExpressionPattern);
		Gamma.DefineExpression("$Expression$",       BunGrammar.ExpressionPattern);
		Gamma.DefineExpression("$RightExpression$",  BunGrammar.RightExpressionPattern);

		Gamma.DefineExpression("$Name$", BunGrammar.NamePattern);
		Gamma.DefineStatement("var", BunGrammar.VarPattern);
		Gamma.DefineExpression("$Param$", BunGrammar.ParamPattern);

		//FIXME class decl
		//Gamma.DefineStatement("class", ClassPattern);
		//Gamma.DefineExpression("$FieldDecl$", BunGrammar.ClassFieldPattern);

		Gamma.DefineExpression("$Block$", new PythonBlockPatternFunction());
		Gamma.DefineExpression("$Statement$", new PythonStatementPatternFunction());
		Gamma.DefineExpression("$Param$", new PythonParamPatternFunction());

		Gamma.DefineStatement("return", BunGrammar.ReturnPattern);

		Gamma.DefineExpression("def", new PythonFunctionPatternFunction());
		Gamma.DefineExpression("if", new PythonIfPatternFunction());
		Gamma.DefineExpression("while", new PythonWhilePatternFunction());
		Gamma.DefineExpression("for", new PythonForPatternFunction());
		Gamma.DefineExpression("with", new PythonWithPatternFunction());
		Gamma.DefineExpression("pass", new PythonPassPatternFunction());

		Gamma.DefineToken("#", new PythonCommentFunction());


		Gamma.Generator.LangInfo.AppendGrammarInfo("python");
	}
}
