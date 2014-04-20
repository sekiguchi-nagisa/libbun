package libbun.lang.bun.extra;

import libbun.ast.BNode;
import libbun.ast.ContainerNode;
import libbun.ast.binary.AssignNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.MutableNode;
import libbun.ast.literal.BunIntNode;
import libbun.ast.statement.BunWhileNode;
import libbun.ast.sugar.BunContinueNode;
import libbun.parser.BToken;
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
			InitStmtNode = TokenContext.ParsePattern(ParentNode, "$InStatement$", BTokenContext._Required);
		}
		WhileNode = TokenContext.MatchToken(WhileNode, ";", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Cond, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		WhileNode = TokenContext.MatchToken(WhileNode, ";", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Next, "$InStatement$", BTokenContext._Optional, BTokenContext._AllowSkipIndent);
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
		@Var BNode WhileNode = new BunWhileNode(ParentNode);
		WhileNode = TokenContext.MatchToken(WhileNode, "for", BTokenContext._Required);
		WhileNode = TokenContext.MatchToken(WhileNode, "(", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Next, "$InStatement$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		WhileNode = TokenContext.MatchToken(WhileNode, "in", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Cond, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		WhileNode = TokenContext.MatchToken(WhileNode, ")", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Block, "$Block$", BTokenContext._Required);
		return WhileNode;
	}
}

class DoWhilePatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BNode WhileNode = new BunWhileNode(ParentNode);
		WhileNode = TokenContext.MatchToken(WhileNode, "do", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Block, "$Block$", BTokenContext._Required);
		WhileNode = TokenContext.MatchToken(WhileNode, "while", BTokenContext._Required);
		WhileNode = TokenContext.MatchToken(WhileNode, "(", BTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Cond, "$Expression$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		if(TokenContext.MatchNewLineToken("whatever")) {
			WhileNode = TokenContext.MatchPattern(WhileNode, BunWhileNode._Next, "$InStatement$", BTokenContext._Required, BTokenContext._AllowSkipIndent);
		}
		WhileNode = TokenContext.MatchToken(WhileNode, ")", BTokenContext._Required);
		return WhileNode;
	}
}

class SelfAddPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		TokenContext.SkipToken();
		if(TokenContext.IsToken("=")) {
			@Var AssignNode AssignNode = new AssignNode(ParentNode);
			@Var BinaryOperatorNode BinaryNode = new BunAddNode(AssignNode);
			AssignNode.SetLeftNode(LeftNode);
			@Var BNode RightNode = BinaryNode.SetParsedNode(AssignNode, LeftNode, "=", TokenContext);
			AssignNode.SetLeftNode(RightNode);
			return AssignNode;
		}
		return null;
	}
}

// ++i
class IncPrefixPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		LeftNode = TokenContext.ParsePattern(ParentNode, "$RightExpression$", BTokenContext._Required);
		if(LeftNode instanceof MutableNode) {
			@Var AssignNode AssignNode = new AssignNode(ParentNode);
			AssignNode.SetLeftNode(LeftNode);
			@Var BinaryOperatorNode BinaryNode = new BunAddNode(AssignNode);
			BinaryNode.SetLeftNode(LeftNode);
			BinaryNode.SetRightNode(new BunIntNode(null, null, 1));
			AssignNode.SetRightNode(BinaryNode);
			return AssignNode;
		}
		if(LeftNode instanceof ErrorNode) {
			return LeftNode;
		}
		return new ErrorNode(LeftNode, "not incremental");
	}
}

// i++
class IncSuffixPatternFunction extends BMatchFunction {
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		if(LeftNode instanceof MutableNode) {
			@Var BToken SourceToken = TokenContext.GetToken(BTokenContext._MoveNext);
			@Var AssignNode AssignNode = new AssignNode(ParentNode);
			AssignNode.SourceToken = SourceToken;
			AssignNode.SetLeftNode(LeftNode);
			@Var BinaryOperatorNode BinaryNode = new BunAddNode(AssignNode);
			BinaryNode.SourceToken = SourceToken;
			BinaryNode.SetLeftNode(LeftNode);
			BinaryNode.SetRightNode(new BunIntNode(null, null, 1));
			AssignNode.SetRightNode(BinaryNode);
			return AssignNode;
		}
		return new ErrorNode(LeftNode, "not incremental");
	}
}

public class BunExtraGrammar {
	public final static BMatchFunction ContinuePattern = new ContinuePatternFunction();
	public final static BMatchFunction IncPrefixPattern = new IncPrefixPatternFunction();
	public final static BMatchFunction IncSuffixPattern = new IncSuffixPatternFunction();
	public final static BMatchFunction SelfAddPattern = new SelfAddPatternFunction();

	public static void LoadGrammar(LibBunGamma Gamma) {
		//Gamma.SetTypeName(BType.VoidType,  null);
		//Gamma.AppendTokenFunc(" \t", WhiteSpaceToken);
		//Gamma.DefineExpression("null", NullPattern);
		//Gamma.DefineRightExpression("instanceof", InstanceOfPattern);
		Gamma.DefineExpression("++", IncPrefixPattern);
		Gamma.DefineRightExpression("++", IncSuffixPattern);

		Gamma.DefineRightExpression("+", SelfAddPattern);
		Gamma.DefineStatement("continue", ContinuePattern);
	}
}
