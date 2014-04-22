package libbun.lang.python;

import libbun.lang.bun.BunGrammar;
import libbun.parser.LibBunGamma;

public class PythonGrammar {
	public static void LoadGrammar(LibBunGamma Gamma) {
		Gamma.Generator.RootParser.Init();
		BunGrammar.LoadGrammar(Gamma);

		Gamma.DefineExpression("None", BunGrammar.NullPattern);
		Gamma.DefineExpression("True", BunGrammar.TruePattern);
		Gamma.DefineExpression("False", BunGrammar.FalsePattern);

		Gamma.DefineExpression("def", new PythonFunctionPatternFunction());
		Gamma.DefineExpression("if", new PythonIfPatternFunction());
		Gamma.DefineExpression("while", new PythonWhilePatternFunction());
		Gamma.DefineExpression("$Block$", new PythonBlockPatternFunction());
		Gamma.DefineExpression("$Statement$", new PythonStatementPatternFunction());
		Gamma.DefineExpression("$Param$", new PythonParamPatternFunction());

		Gamma.DefineBinaryOperator("and", BunGrammar.AndPattern);
		Gamma.DefineBinaryOperator("or", BunGrammar.OrPattern);
		//TODO is, is not

		Gamma.DefineToken("#", new PythonCommentFunction());


		Gamma.Generator.LangInfo.AppendGrammarInfo("python");
	}
}
