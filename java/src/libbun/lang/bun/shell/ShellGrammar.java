package libbun.lang.bun.shell;

import java.util.ArrayList;

import libbun.ast.BNode;
import libbun.ast.EmptyNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.BPatternToken;
import libbun.parser.BSourceContext;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.LibBunGamma;
import libbun.parser.LibBunSyntax;
import libbun.util.BArray;
import libbun.util.BMatchFunction;
import libbun.util.BTokenFunction;
import libbun.util.LibBunSystem;
import libbun.util.Var;

// Token
class ShellStyleCommentTokenFunction extends BTokenFunction {
	@Override public boolean Invoke(BSourceContext SourceContext) {
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

class CommandSymbolTokenFunction extends BTokenFunction {
	@Override public boolean Invoke(BSourceContext SourceContext) {
		@Var int StartIndex = SourceContext.GetPosition();
		@Var StringBuilder SymbolBuilder = new StringBuilder();
		while(SourceContext.HasChar()) {
			@Var char ch = SourceContext.GetCurrentChar();
			if(!LibBunSystem._IsDigitOrLetter(ch) && ch != '-' && ch != '+' && ch != '_') {
				break;
			}
			SymbolBuilder.append(ch);
			SourceContext.MoveNext();
		}
		if(ShellUtils.IsCommand(SymbolBuilder.toString())) {
			SourceContext.Tokenize(CommandSymbolPatternFunction._PatternName, StartIndex, SourceContext.GetPosition());
			return true;
		}
		return false;
	}
}

// Syntax Pattern
class ImportPatternFunction extends BMatchFunction {
	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		TokenContext.MoveNext();
		BToken Token = TokenContext.GetToken();
		if(Token.EqualsText("command")) {
			return TokenContext.ParsePattern(ParentNode, ImportCommandPatternFunction._PatternName, BTokenContext._Required);
		}
		return this.MatchEnvPattern(ParentNode, TokenContext, Token);
	}

	public BNode MatchEnvPattern(BNode ParentNode, BTokenContext TokenContext, BToken Token) {
		return null;	//do not support it
	}
}

class ImportCommandPatternFunction extends BMatchFunction {
	public final static String _PatternName = "$ImportCommand$";

	public String ResolveHome(String Path) {
		return ShellUtils._ResolveHome(Path);
	}

	public boolean IsFileExecutable(String Path) {
		return ShellUtils._IsFileExecutable(Path);
	}

	public String GetUnixCommand(String cmd) {
		return ShellUtils._GetUnixCommand(cmd);
	}

	private BToken ToCommandToken(ArrayList<BToken> TokenList) {
		if(TokenList.isEmpty()) {
			return null;
		}
		@Var int StartIndex = TokenList.get(0).StartIndex;
		@Var int EndIndex = TokenList.get(TokenList.size() - 1).EndIndex;
		@Var BToken CommandToken = new BToken(TokenList.get(0).Source, StartIndex, EndIndex);
		TokenList.clear();
		return CommandToken;
	}

	private void CheckDuplicationAndSetCommand(LibBunGamma Gamma, String Command, String CommandPath) {
		@Var LibBunSyntax Syntax = Gamma.GetSyntaxPattern(Command);
		if(Syntax != null) {
			if(LibBunSystem.DebugMode) {
				System.err.println("found duplicated syntax pattern: " + Syntax);
			}
		}
		else if(!ShellUtils.SetCommand(Command, CommandPath)) {
			if(LibBunSystem.DebugMode) {
				System.err.println("found duplicated symbol: " + Command);
			}
		}
	}

	private void SetCommandSymbol(BNode ParentNode, BTokenContext TokenContext, ArrayList<BToken> TokenList) {	//TODO: command scope
		@Var BToken CommandToken = this.ToCommandToken(TokenList);
		if(CommandToken == null) {
			return;
		}
		@Var String CommandPath = this.ResolveHome(CommandToken.GetText());
		@Var LibBunGamma Gamma = ParentNode.GetGamma();
		@Var int loc = CommandPath.lastIndexOf('/');
		@Var String Command = CommandPath;
		if(loc != -1) {
			if(!this.IsFileExecutable(CommandPath)) {
				System.err.println("[warning] unknown command: " + CommandPath);
				return;
			}
			Command = CommandPath.substring(loc + 1);
		}
		else {
			@Var String FullPath = this.GetUnixCommand(CommandPath);
			if(FullPath == null) {
				System.err.println("[warning] unknown command: " + CommandPath);
				return;
			}
			CommandPath = FullPath;
		}
		this.CheckDuplicationAndSetCommand(Gamma, Command, CommandPath);
	}

	@Override
	public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var ArrayList<BToken> TokenList = new ArrayList<BToken>();
		TokenContext.MoveNext();
		while(TokenContext.HasNext()) {
			@Var BToken Token = TokenContext.GetToken();
			if(Token.EqualsText(";") || Token.IsIndent()) {
				break;
			}
			if(!Token.EqualsText(",")) {
				TokenList.add(Token);
			}
			if(Token.IsNextWhiteSpace()) {
				this.SetCommandSymbol(ParentNode, TokenContext, TokenList);
			}
			TokenContext.MoveNext();
		}
		this.SetCommandSymbol(ParentNode, TokenContext, TokenList);
		return new EmptyNode(ParentNode);
	}
}

class CommandSymbolPatternFunction extends BMatchFunction {
	public final static String _PatternName = "$CommandSymbol$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BToken CommandToken = TokenContext.GetToken(BTokenContext._MoveNext);
		@Var String Command = ShellUtils.GetCommand(CommandToken.GetText());
		if(Command == null) {
			return new ErrorNode(ParentNode, CommandToken, "undefined command symbol");
		}
		@Var CommandNode CommandNode = new CommandNode(ParentNode, CommandToken, Command);
		while(TokenContext.HasNext()) {
			if(TokenContext.MatchToken("|")) {
				// Match Prefix Option
				@Var BNode PrefixOptionNode = TokenContext.ParsePatternAfter(ParentNode, CommandNode, PrefixOptionPatternFunction._PatternName, BTokenContext._Optional);
				if(PrefixOptionNode != null) {
					return CommandNode.AppendPipedNextNode((CommandNode)PrefixOptionNode);
				}
				// Match Command Symbol
				@Var BNode PipedNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunction._PatternName, BTokenContext._Required);
				if(PipedNode.IsErrorNode()) {
					return PipedNode;
				}
				return CommandNode.AppendPipedNextNode((CommandNode)PipedNode);
			}
			// Match Redirect
			@Var BNode RedirectNode = TokenContext.ParsePattern(ParentNode, RedirectPatternFunction._PatternName, BTokenContext._Optional);
			if(RedirectNode != null) {
				CommandNode.AppendPipedNextNode((CommandNode)RedirectNode);
				continue;
			}
			// Match Suffix Option
			@Var BNode SuffixOptionNode = TokenContext.ParsePattern(ParentNode, SuffixOptionPatternFunction._PatternName, BTokenContext._Optional);
			if(SuffixOptionNode != null) {
				if(SuffixOptionNode.IsErrorNode()) {
					return SuffixOptionNode;
				}
				return CommandNode.AppendPipedNextNode((CommandNode)SuffixOptionNode);
			}
			// Match Argument
			@Var BNode ArgNode = TokenContext.ParsePattern(ParentNode, SimpleArgumentPatternFunction._PatternName, BTokenContext._Optional);
			if(ArgNode == null) {
				break;
			}
			CommandNode.AppendArgNode(ArgNode);
		}
		return CommandNode;
	}
}

class SimpleArgumentPatternFunction extends BMatchFunction {	// subset of CommandArgPatternFunc
	public final static String _PatternName = "$CommandArg$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		if(ShellUtils._MatchStopToken(TokenContext)) {
			return null;
		}
		@Var boolean FoundSubstitution = false;
		@Var boolean FoundEscape = false;
		@Var BArray<BToken> TokenList = new BArray<BToken>(new BToken[]{});
		@Var BArray<BNode> NodeList = new BArray<BNode>(new BNode[]{});
		while(!ShellUtils._MatchStopToken(TokenContext)) {
			@Var BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
			if(Token instanceof BPatternToken && ((BPatternToken)Token).PresetPattern.equals("$StringLiteral$")) {
				this.Flush(TokenContext, NodeList, TokenList);
				NodeList.add(new BunStringNode(ParentNode, null, LibBunSystem._UnquoteString(Token.GetText())));
			}
			else {
				TokenList.add(Token);
			}
			if(Token.IsNextWhiteSpace()) {
				break;
			}
			FoundEscape = this.CheckEscape(Token, FoundEscape);
		}
		this.Flush(TokenContext, NodeList, TokenList);
		@Var BNode ArgNode = new ArgumentNode(ParentNode, FoundSubstitution ? ArgumentNode._Substitution : ArgumentNode._Normal);
		ArgNode.SetNode(ArgumentNode._Expr, ShellUtils._ToNode(ParentNode, TokenContext, NodeList));
		return ArgNode;
	}

	private boolean CheckEscape(BToken Token, boolean FoundEscape) {
		if(Token.EqualsText("\\") && !FoundEscape) {
			return true;
		}
		return false;
	}

	private void Flush(BTokenContext TokenContext, BArray<BNode> NodeList, BArray<BToken> TokenList) {
		@Var int size = TokenList.size();
		if(size == 0) {
			return;
		}
		@Var int StartIndex = 0;
		@Var int EndIndex = 0;
		for(int i = 0; i < size; i++) {
			if(i == 0) {
				StartIndex = BArray.GetIndex(TokenList, i).StartIndex;
			}
			if(i == size - 1) {
				EndIndex = BArray.GetIndex(TokenList, i).EndIndex;
			}
		}
		@Var BToken Token = new BToken(TokenContext.SourceContext.Source, StartIndex, EndIndex);
		NodeList.add(new BunStringNode(null, Token, LibBunSystem._UnquoteString(this.ResolveHome(Token.GetText()))));
		TokenList.clear(0);
	}

	public String ResolveHome(String Path) {
		return ShellUtils._ResolveHome(Path);
	}
}

class RedirectPatternFunction extends BMatchFunction {
	public final static String _PatternName = "$Redirect$";

	// <, >, >>, >&, 1>, 2>, 1>>, 2>>, &>, &>>
	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
		@Var String RedirectSymbol = Token.GetText();
		if(Token.EqualsText(">>") || Token.EqualsText("<")) {
			return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("&")) {
			@Var BToken Token2 = TokenContext.GetToken(BTokenContext._MoveNext);
			if(Token2.EqualsText(">") || Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		else if(Token.EqualsText(">")) {
			@Var BToken Token2 = TokenContext.GetToken();
			if(Token2.EqualsText("&")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
		}
		else if(Token.EqualsText("1") || Token.EqualsText("2")) {
			@Var BToken Token2 = TokenContext.GetToken(BTokenContext._MoveNext);
			if(Token2.EqualsText(">>")) {
				RedirectSymbol += Token2.GetText();
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
			else if(Token2.EqualsText(">")) {
				RedirectSymbol += Token2.GetText();
				if(RedirectSymbol.equals("2>") && TokenContext.MatchToken("&")) {
					if(TokenContext.MatchToken("1")) {
						return this.CreateRedirectNode(ParentNode, TokenContext, "2>&1", false);
					}
					return null;
				}
				return this.CreateRedirectNode(ParentNode, TokenContext, RedirectSymbol, true);
			}
		}
		return null;
	}

	private BNode CreateRedirectNode(BNode ParentNode, BTokenContext TokenContext, String RedirectSymbol, boolean existTarget) {
		@Var CommandNode Node = new CommandNode(ParentNode, null, RedirectSymbol);
		if(existTarget) {
			@Var BNode TargetNode = TokenContext.ParsePattern(Node, SimpleArgumentPatternFunction._PatternName, BTokenContext._Required);
			if(TargetNode.IsErrorNode()) {
				return TargetNode;
			}
			Node.AppendArgNode(TargetNode);
		}
		return Node;
	}
}

class PrefixOptionPatternFunction extends BMatchFunction {
	public final static String _PatternName = "$PrefixOption$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BToken Token = TokenContext.GetToken(BTokenContext._MoveNext);
		@Var String Symbol = Token.GetText();
		if(Symbol.equals(ShellUtils._trace)) {
			@Var BNode CommandNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunction._PatternName, BTokenContext._Required);
			if(CommandNode.IsErrorNode()) {
				return CommandNode;
			}
			@Var CommandNode Node = new CommandNode(ParentNode, Token, Symbol);
			return Node.AppendPipedNextNode((CommandNode) CommandNode);
		}
		if(Symbol.equals(ShellUtils._timeout) && LeftNode == null) {
			@Var BNode TimeNode = this.ParseTimeout(ParentNode, TokenContext);
			if(TimeNode.IsErrorNode()) {
				return TimeNode;
			}
			@Var BNode CommandNode = TokenContext.ParsePattern(ParentNode, CommandSymbolPatternFunction._PatternName, BTokenContext._Required);
			if(CommandNode.IsErrorNode()) {
				return CommandNode;
			}
			@Var CommandNode Node = new CommandNode(ParentNode, Token, Symbol);
			Node.AppendArgNode(TimeNode);
			return Node.AppendPipedNextNode((CommandNode) CommandNode);
		}
		return null;
	}

	public BNode ParseTimeout(BNode ParentNode, BTokenContext TokenContext) {
		@Var BToken NumToken = TokenContext.GetToken(BTokenContext._MoveNext);
		if((NumToken instanceof BPatternToken)) {
			if(((BPatternToken)NumToken).PresetPattern.PatternName.equals(("$IntegerLiteral$"))) {
				@Var long Num = LibBunSystem._ParseInt(NumToken.GetText());
				if(Num > 0) {
					if(NumToken.IsNextWhiteSpace()) {
						return new ArgumentNode(ParentNode, Long.toString(Num));
					}
					@Var BToken UnitToken = TokenContext.GetToken(BTokenContext._MoveNext);
					@Var String UnitSymbol = UnitToken.GetText();
					if(UnitSymbol.equals("ms")) {
						return new ArgumentNode(ParentNode, Long.toString(Num));
					}
					if(UnitSymbol.equals("s")) {
						return new ArgumentNode(ParentNode, Long.toString(Num * 1000));
					}
					if(UnitSymbol.equals("m")) {
						return new ArgumentNode(ParentNode, Long.toString(Num * 1000 * 60));
					}
					return TokenContext.CreateExpectedErrorNode(UnitToken, "{ms, s, m}");
				}
			}
		}
		return TokenContext.CreateExpectedErrorNode(NumToken, "Integer Number Symbol");
	}
}

class SuffixOptionPatternFunction extends BMatchFunction {
	public final static String _PatternName = "$SuffixOption$";

	@Override public BNode Invoke(BNode ParentNode, BTokenContext TokenContext, BNode LeftNode) {
		@Var BToken Token = TokenContext.GetToken();
		TokenContext.MoveNext();
		@Var String OptionSymbol = Token.GetText();
		if(Token.EqualsText(ShellUtils._background)) {	// set background job
			return new CommandNode(ParentNode, Token, OptionSymbol);
		}
		return null;
	}
}

public class ShellGrammar {
	// token func
	public final static BTokenFunction ShellStyleCommentToken = new ShellStyleCommentTokenFunction();
	public final static BTokenFunction CommandSymbolToken = new CommandSymbolTokenFunction();

	// pattern func
	public final static BMatchFunction ImportPattern = new ImportPatternFunction();
	public final static BMatchFunction ImportCommandPattern = new ImportCommandPatternFunction();
	public final static BMatchFunction CommandSymbolPattern = new CommandSymbolPatternFunction();
	public final static BMatchFunction SimpleArgumentPattern = new SimpleArgumentPatternFunction();
	public final static BMatchFunction RedirectPattern = new RedirectPatternFunction();
	public final static BMatchFunction PrefixOptionPattern = new PrefixOptionPatternFunction();
	public final static BMatchFunction SuffixOptionPattern = new SuffixOptionPatternFunction();

	public static void LoadGrammar(LibBunGamma Gamma) {
		Gamma.DefineToken("#", ShellStyleCommentToken);
		Gamma.DefineToken("Aa_", CommandSymbolToken);
		Gamma.DefineToken("1", CommandSymbolToken);

		Gamma.DefineStatement("import", ImportPattern);
		Gamma.DefineExpression("command", ImportCommandPattern);
		Gamma.DefineExpression(ImportCommandPatternFunction._PatternName, ImportCommandPattern);
		Gamma.DefineExpression(CommandSymbolPatternFunction._PatternName, CommandSymbolPattern);
		Gamma.DefineExpression(SimpleArgumentPatternFunction._PatternName, SimpleArgumentPattern);
		Gamma.DefineExpression(RedirectPatternFunction._PatternName, RedirectPattern);
		Gamma.DefineExpression(ShellUtils._timeout, PrefixOptionPattern);
		Gamma.DefineExpression(ShellUtils._trace, PrefixOptionPattern);
		Gamma.DefineExpression(PrefixOptionPatternFunction._PatternName, PrefixOptionPattern);
		Gamma.DefineExpression(SuffixOptionPatternFunction._PatternName, SuffixOptionPattern);

		Gamma.Generator.LangInfo.AppendGrammarInfo("shell");
	}
}


/**

if you use shell grammar, you must implement following class and function

# argument class definition
## normal argument
class CommandArg {
	var value : String
}

function createCommandArg(value : String) : CommandArg

## substitution argument
class SubstitutedArg extends CommandArg {
	var values : String[]
}

function createSubstitutedArg(value : String) : SubstitutedArg

# command executor definition
function ExecCommandInt(argsList : CommandArg[][]) : int          // return exit status
function ExecCommandBoolean(argsList : CommandArg[][]) : boolean  // return true if exit status is 0 or false if exit status is not 0
function ExecCommandString(argsList : CommandArg[][]) : String    // return command standard out

 **/