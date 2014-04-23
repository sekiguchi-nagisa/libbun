package libbun.lang.bun.extra;

import libbun.ast.BNode;
import libbun.ast.ContainerNode;
import libbun.ast.binary.AssignNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.binary.BunBitwiseAndNode;
import libbun.ast.binary.BunBitwiseOrNode;
import libbun.ast.binary.BunBitwiseXorNode;
import libbun.ast.binary.BunDivNode;
import libbun.ast.binary.BunLeftShiftNode;
import libbun.ast.binary.BunModNode;
import libbun.ast.binary.BunMulNode;
import libbun.ast.binary.BunRightShiftNode;
import libbun.ast.binary.BunSubNode;
import libbun.ast.literal.BunIntNode;
import libbun.ast.statement.BunWhileNode;
import libbun.ast.sugar.BunContinueNode;
import libbun.ast.sugar.BunDoWhileNode;
import libbun.ast.sugar.BunForInNode;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunGamma;
import libbun.util.BMatchFunction;
import libbun.util.Var;

// continue
class ContinuePatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode ContinueNode = new BunContinueNode(ParentNode);
		ContinueNode = TokenContext.MatchToken(ContinueNode, "continue", BTokenContext._Required);
		return ContinueNode;
	}
}

class ForPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode InitStmtNode = null;
		@Var BNode WhileNode = new BunWhileNode(ParentNode);
		WhileNode = TokenContext.MatchToken(WhileNode, "for", BTokenContext._Required);
		WhileNode = TokenContext.MatchToken(WhileNode, "(", BTokenContext._Required);
		if(!TokenContext.IsToken(";")) {
			InitStmtNode = TokenContext.ParsePattern(ParentNode, "$Expression$", BTokenContext._Required);
		}
		WhileNode = TokenContext.MatchToken(WhileNode, ";", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Cond, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		WhileNode = TokenContext.MatchToken(WhileNode, ";", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Next, "$Expression$", BTokenContext._Optional, BTokenContext._AllowSkipIndent);
		WhileNode = TokenContext.MatchToken(WhileNode, ")", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Block, "$Block$", BTokenContext._Required);
		if(InitStmtNode == null) {
			return WhileNode;
		}
		return new ContainerNode(InitStmtNode, WhileNode);
	}
}

class ForInPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode ForNode = new BunForInNode(ParentNode);
		ForNode = TokenContext.MatchToken(ForNode, "for", BTokenContext._Required);
		ForNode = TokenContext.MatchToken(ForNode, "(", BTokenContext._Required);
		ForNode = TokenContext.MatchPattern(ForNode, BunForInNode._Var, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		ForNode = TokenContext.MatchToken(ForNode, "in", BTokenContext._Required);
		ForNode = TokenContext.MatchPattern(ForNode, BunForInNode._List, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		ForNode = TokenContext.MatchToken(ForNode, ")", BTokenContext._Required);
		ForNode = TokenContext.MatchPattern(ForNode, BunForInNode._Block, "$Block$", BTokenContext._Required);
		return ForNode;
	}
}

class DoWhilePatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode WhileNode = new BunDoWhileNode(ParentNode);
		WhileNode = TokenContext.MatchToken(WhileNode, "do", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunDoWhileNode._Block, "$Block$", BTokenContext._Required);
		if(TokenContext.MatchNewLineToken("while")) {
			WhileNode = TokenContext.MatchToken(WhileNode, "(", BTokenContext._Required);
			WhileNode = TokenContext.MatchPattern(WhileNode, BunDoWhileNode._Cond, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
			if(TokenContext.MatchNewLineToken("whatever")) {
				WhileNode = TokenContext.MatchPattern(WhileNode, BunDoWhileNode._Next, "$InStatement$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
			}
			WhileNode = TokenContext.MatchToken(WhileNode, ")", BTokenContext._Required);
			return WhileNode;
		}
		return TokenContext.CreateExpectedErrorNode(null, "while");
	}
}

class SelfAddPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunAddNode(null));
	}
}

class SelfSubPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunSubNode(null));
	}
}

class SelfMulPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunMulNode(null));
	}
}

class SelfDivPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunDivNode(null));
	}
}

class SelfModPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunModNode(null));
	}
}

class SelfBitwiseAndPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunBitwiseAndNode(null));
	}
}

class SelfBitwiseOrPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunBitwiseOrNode(null));
	}
}

class SelfBitwiseXorPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunBitwiseXorNode(null));
	}
}

class SelfRightShiftPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunRightShiftNode(null));
	}
}

class SelfLeftShiftPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		return BunExtraGrammar.DesugarSelfAssignment(ParentNode, TokenContext, LeftNode, new BunLeftShiftNode(null));
	}
}

// ++i
class IncPrefixPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode AssignNode = new AssignNode(ParentNode);
		AssignNode = TokenContext.MatchToken(AssignNode, "++", BTokenContext._Required);
		AssignNode = TokenContext.MatchPattern(AssignNode, BinaryOperatorNode._Left, "$RightExpression$", BTokenContext._Required);
		if(!AssignNode.IsErrorNode()) {
			@Var BinaryOperatorNode BinaryNode = new BunAddNode(AssignNode);
			BinaryNode.SetLeftNode(AssignNode.AST[BinaryOperatorNode._Left]);
			BinaryNode.SetRightNode(new BunIntNode(null, null, 1));
			AssignNode.SetNode(BinaryOperatorNode._Right, BinaryNode);
		}
		return AssignNode;
	}
}

// i++
class IncSuffixPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		//System.out.println("LeftNode: " + LeftNode);
		@Var BNode AssignNode = new AssignNode(ParentNode);
		AssignNode = TokenContext.MatchToken(AssignNode, "++", BTokenContext._Required);
		if(!AssignNode.IsErrorNode()) {
			AssignNode.SetNode(BinaryOperatorNode._Left, LeftNode);
			@Var BinaryOperatorNode BinaryNode = new BunAddNode(AssignNode);
			BinaryNode.SourceToken = AssignNode.SourceToken;
			BinaryNode.SetLeftNode(LeftNode);
			BinaryNode.SetRightNode(new BunIntNode(null, null, 1));
			AssignNode.SetNode(BinaryOperatorNode._Right, BinaryNode);
		}
		return AssignNode;
	}
}

//--i
class DecPrefixPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode AssignNode = new AssignNode(ParentNode);
		AssignNode = TokenContext.MatchToken(AssignNode, "--", BTokenContext._Required);
		AssignNode = TokenContext.MatchPattern(AssignNode, BinaryOperatorNode._Left, "$RightExpression$", BTokenContext._Required);
		if(!AssignNode.IsErrorNode()) {
			@Var BinaryOperatorNode BinaryNode = new BunSubNode(AssignNode);
			BinaryNode.SetLeftNode(AssignNode.AST[BinaryOperatorNode._Left]);
			BinaryNode.SetRightNode(new BunIntNode(null, null, 1));
			AssignNode.SetNode(BinaryOperatorNode._Right, BinaryNode);
		}
		return AssignNode;
	}
}

//i--
class DecSuffixPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		//System.out.println("LeftNode: " + LeftNode);
		@Var BNode AssignNode = new AssignNode(ParentNode);
		AssignNode = TokenContext.MatchToken(AssignNode, "--", BTokenContext._Required);
		if(!AssignNode.IsErrorNode()) {
			AssignNode.SetNode(BinaryOperatorNode._Left, LeftNode);
			@Var BinaryOperatorNode BinaryNode = new BunSubNode(AssignNode);
			BinaryNode.SourceToken = AssignNode.SourceToken;
			BinaryNode.SetLeftNode(LeftNode);
			BinaryNode.SetRightNode(new BunIntNode(null, null, 1));
			AssignNode.SetNode(BinaryOperatorNode._Right, BinaryNode);
		}
		return AssignNode;
	}
}

public class BunExtraGrammar {
	public final static BMatchFunction SelfAddPattern = new SelfAddPatternFunction();
	public final static BMatchFunction SelfSubPattern = new SelfSubPatternFunction();
	public final static BMatchFunction SelfMulPattern = new SelfMulPatternFunction();
	public final static BMatchFunction SelfDivPattern = new SelfDivPatternFunction();
	public final static BMatchFunction SelfModPattern = new SelfModPatternFunction();
	public final static BMatchFunction SelfBitwiseAndPattern = new SelfBitwiseAndPatternFunction();
	public final static BMatchFunction SelfBitwiseOrPattern = new SelfBitwiseOrPatternFunction();
	public final static BMatchFunction SelfBitwiseXorPattern = new SelfBitwiseXorPatternFunction();
	public final static BMatchFunction SelfRightShiftPattern = new SelfRightShiftPatternFunction();
	public final static BMatchFunction SelfLeftShiftPattern = new SelfLeftShiftPatternFunction();

	public final static BMatchFunction IncPrefixPattern = new IncPrefixPatternFunction();
	public final static BMatchFunction IncSuffixPattern = new IncSuffixPatternFunction();
	public final static BMatchFunction DecPrefixPattern = new DecPrefixPatternFunction();
	public final static BMatchFunction DecSuffixPattern = new DecSuffixPatternFunction();

	/* statement */
	public final static BMatchFunction ContinuePattern = new ContinuePatternFunction();
	public final static BMatchFunction DoWhilePattern  = new DoWhilePatternFunction();

	public static void LoadGrammar(LibBunGamma Gamma) {
		//Gamma.SetTypeName(BType.VoidType,  null);
		//Gamma.AppendTokenFunc(" \t", WhiteSpaceToken);
		//Gamma.DefineExpression("null", NullPattern);
		//Gamma.DefineRightExpression("instanceof", InstanceOfPattern);
		Gamma.DefineExpression("++", IncPrefixPattern);
		Gamma.DefineExpressionSuffix("++", IncSuffixPattern);
		Gamma.DefineExpression("--", DecPrefixPattern);
		Gamma.DefineExpressionSuffix("--", DecSuffixPattern);

		Gamma.DefineBinaryOperator("+", SelfAddPattern);
		Gamma.DefineBinaryOperator("-", SelfSubPattern);
		Gamma.DefineBinaryOperator("*", SelfMulPattern);
		Gamma.DefineBinaryOperator("/", SelfDivPattern);
		Gamma.DefineBinaryOperator("%", SelfModPattern);
		Gamma.DefineBinaryOperator("&", SelfBitwiseAndPattern);
		Gamma.DefineBinaryOperator("|", SelfBitwiseOrPattern);
		Gamma.DefineBinaryOperator("^", SelfBitwiseXorPattern);
		Gamma.DefineBinaryOperator("<<", SelfLeftShiftPattern);
		Gamma.DefineBinaryOperator(">>", SelfRightShiftPattern);

		Gamma.DefineStatement("continue", ContinuePattern);
		Gamma.DefineStatement("do", DoWhilePattern);
	}

	public final static BNode DesugarSelfAssignment(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode, BinaryOperatorNode BinaryNode) {
		TokenContext.SkipToken();
		if(TokenContext.IsToken("=")) {
			@Var AssignNode AssignNode = new AssignNode(ParentNode);
			AssignNode.SetLeftNode(LeftNode);
			@Var BNode RightNode = BinaryNode.SetParsedNode(AssignNode, LeftNode, "=", TokenContext);
			AssignNode.SetRightNode(RightNode);
			AssignNode.SourceToken = RightNode.SourceToken;
			return AssignNode;
		}
		return null;
	}

}
