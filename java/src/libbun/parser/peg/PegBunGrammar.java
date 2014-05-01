package libbun.parser.peg;

import java.io.IOException;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.binary.BunMulNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.literal.BunBooleanNode;
import libbun.ast.literal.BunFloatNode;
import libbun.ast.literal.BunIntNode;
import libbun.ast.literal.BunNullNode;
import libbun.ast.literal.BunStringNode;
import libbun.ast.unary.BunPlusNode;
import libbun.parser.BToken;
import libbun.parser.LibBunConst;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class PegBunGrammar {
	public final static void test(String FileName) {
		PegParser p = new PegParser(null, null);
		p.setPegRule("number", new PegFunctionExpr(new NumberLiteral()));
		p.setPegRule("const", new PegFunctionExpr(new ConstLiteral()));
		p.setPegRule("string", new PegFunctionExpr(new StringLiteral()));
		p.setPegRule("variable", new PegFunctionExpr(new Variable()));
		p.loadPegFile(FileName);
		p.setNode("+",  new BunPlusNode(null));
		p.setNode("Expr:+", new BunAddNode(null));
		p.setNode("Expr:*", new BunMulNode(null));
		//		p.setNode("null",  new PegNullNode(null));
		//		p.setNode("true",  new PegTrueNode(null));
		//		p.setNode("false", new PegFalseNode(null));
		//		p.setNode("0-9", new PegNumberNode(null));
		PerformShell(p);
		//		ParserContext source = new ParserContext(p, "+123");
		//		BNode node = source.parseBunNode(new BunBlockNode(null, null), "Stmt");
		//		System.out.println("parsed node: " + node);
		//		source = new ParserContext(p, "1.2+123");
		//		node = source.parseBunNode(new BunBlockNode(null, null), "Stmt");
		//		System.out.println("parsed node: " + node);
	}

	public final static void PerformShell(PegParser p) {
		LibBunSystem._PrintLine(LibBunConst.ProgName + LibBunConst.Version + " (" + LibBunConst.CodeName + ") on " + LibBunSystem._GetPlatform());
		LibBunSystem._PrintLine(LibBunConst.Copyright);
		LibBunSystem._PrintLine("PEG debug special!!");
		@Var int linenum = 1;
		@Var String Line = null;
		while ((Line = ReadLine2(">>> ", "    ")) != null) {
			try {
				ParserContext source = new ParserContext(p, Line);
				BNode node = source.parseBunNode(new BunBlockNode(null, null), "Stmt");
				System.out.println("parsed node: " + node);
				if(source.hasChar()) {
					System.out.println("uncosumed: '" + source + "'");
				}
				linenum = linenum + 1;
			}
			catch (Exception e) {
				PrintStackTrace(e, linenum);
				linenum = linenum + 1;
			}
		}
		LibBunSystem._PrintLine("");
	}

	private static jline.ConsoleReader ConsoleReader = null;

	private final static String ReadLine2(String Prompt, String Prompt2) {
		if(ConsoleReader == null) {
			try {
				ConsoleReader = new jline.ConsoleReader();
				//ConsoleReader.setExpandEvents(false);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		@Var String Line;
		try {
			Line = ConsoleReader.readLine(Prompt);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(Line == null) {
			System.exit(0);
		}
		if(Prompt2 != null) {
			@Var int level = 0;
			while((level = CheckBraceLevel(Line)) > 0) {
				String Line2;
				try {
					Line2 = ConsoleReader.readLine(Prompt2);
					//Line2 = ConsoleReader.readLine(Prompt2 + ZenUtils.JoinStrings("  ", level));
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				Line += "\n" + Line2;
			}
			if(level < 0) {
				Line = "";
				LibBunSystem._PrintLine(" .. canceled");
			}
		}
		ConsoleReader.getHistory().addToHistory(Line);
		return Line;
	}

	private static void PrintStackTrace(Exception e, int linenum) {
		@Var StackTraceElement[] elements = e.getStackTrace();
		@Var int size = elements.length + 1;
		@Var StackTraceElement[] newElements = new StackTraceElement[size];
		@Var int i = 0;
		for(; i < size; i++) {
			if(i == size - 1) {
				newElements[i] = new StackTraceElement("<TopLevel>", "TopLevelEval", "stdin", linenum);
				break;
			}
			newElements[i] = elements[i];
		}
		e.setStackTrace(newElements);
		e.printStackTrace();
	}

	private final static int CheckBraceLevel(String Text) {
		@Var int level = 0;
		@Var int i = 0;
		while(i < Text.length()) {
			@Var char ch = Text.charAt(i);
			if(ch == '{' || ch == '[') {
				level = level + 1;
			}
			if(ch == '}' || ch == ']') {
				level = level - 1;
			}
			i = i + 1;
		}
		return level;
	}
}


class ConstLiteral extends PegFunction {
	@Override public BNode Invoke(BNode parentNode, ParserContext sourceContext) {
		BToken token = sourceContext.newToken();
		if(sourceContext.sliceMatchedText(token, "null")) {
			return new BunNullNode(parentNode, token);
		}
		if(sourceContext.sliceMatchedText(token, "true")) {
			return new BunBooleanNode(parentNode, token, true);
		}
		if(sourceContext.sliceMatchedText(token, "false")) {
			return new BunBooleanNode(parentNode, token, true);
		}
		return null;
	}
}

class NumberLiteral extends PegFunction {
	@Override public BNode Invoke(BNode parentNode, ParserContext sourceContext) {
		BToken token = sourceContext.newToken();
		if(!sourceContext.sliceNumber(token)) {
			return null;
		}
		if(sourceContext.sliceMatchedText(token, ".")) {
			sourceContext.sliceNumber(token);
			return new BunFloatNode(parentNode, token, LibBunSystem._ParseFloat(token.GetText()));
		}
		//System.out.println("token: " + token);
		return new BunIntNode(parentNode, token, LibBunSystem._ParseInt(token.GetText()));
	}
}

class StringLiteral extends PegFunction {
	@Override public BNode Invoke(BNode parentNode, ParserContext sourceContext) {
		BToken token = sourceContext.newToken();
		char quote = sourceContext.nextChar();
		if(sourceContext.sliceQuotedTextUntil(token, quote, "\n")) {
			token.EndIndex = token.EndIndex + 1;
			//System.out.println("token: " + token.GetText());
			return new BunStringNode(parentNode, token, LibBunSystem._UnquoteString(token.GetText()));
		}
		return null;
	}
}

class Variable extends PegFunction {
	@Override public BNode Invoke(BNode parentNode, ParserContext sourceContext) {
		BToken token = sourceContext.newToken();
		char ch = sourceContext.getChar(-1);
		if(LibBunSystem._IsSymbol(ch)) {  // check before token is not symbol
			System.out.println("not ahead symbol");
			return null;
		}
		if(sourceContext.sliceSymbol(token, "_")) {
			return new GetNameNode(parentNode, token, token.GetText());
		}
		System.out.println("not symbol");
		return null;
	}

}

