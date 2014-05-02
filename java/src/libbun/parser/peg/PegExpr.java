package libbun.parser.peg;

import libbun.ast.BNode;
import libbun.ast.error.ErrorNode;
import libbun.parser.BToken;
import libbun.util.BunMap;
import libbun.util.LibBunSystem;

public abstract class PegExpr {
	public final static boolean _BackTrack = true;
	public BToken source;
	public int serialNumber = 0;
	public PegExpr nextExpr = null;


	PegExpr(BToken source) {
		this.source = source;
	}

	public void appendNext(PegExpr e) {
		PegExpr list = this;
		while(list.nextExpr != null) {
			list = list.nextExpr;
		}
		list.nextExpr = e;
	}

	PegExpr shorten(PegExpr e) {
		if(e instanceof PegOrElseExpr) {
			return ((PegOrElseExpr) e).firstExpr;
		}
		return e;
	}

	public final BNode matchAll(BNode parentNode, ParserContext sourceContext) {
		PegExpr e = this;
		BNode node = parentNode;
		while(e.nextExpr != null) {
			node = e.match(node, sourceContext);
			if(node == null || node.IsErrorNode()) {
				return node;
			}
			e = e.nextExpr;
			//this.debug(this.shorten(e) + "    @" + node + " char='" + sourceContext.getChar() + "'");
		}
		return e.match(node, sourceContext);
	}

	protected abstract BNode match(BNode ParentNode, ParserContext Context);

	public String firstChars(BunMap<PegExpr> m) {
		return "";
	}

	@Override public String toString() {
		if(this.nextExpr != null) {
			String s = "";
			PegExpr e = this;
			while(e != null) {
				if(e instanceof PegOrElseExpr) {
					s = s + "(" + e.stringfy() + ")";
				}
				else {
					s = s + e.stringfy();
				}
				if(e.nextExpr != null) {
					s = s + " ";
				}
				e = e.nextExpr;
			}
			return s + "";
		}
		else {
			return this.stringfy();
		}
	}

	public final String groupfy(PegExpr e) {
		if(e.nextExpr != null && e instanceof PegTokenExpr) {
			return e.toString();
		}
		else {
			return "(" + e.toString() + ")";
		}
	}
	protected abstract String stringfy();

	private String joinPrintableString(String s, PegExpr e) {
		if(e instanceof PegOrElseExpr) {
			s = s + ((PegOrElseExpr)e).firstExpr;
			s = s + "\n\t/ ";
			s = this.joinPrintableString(s, ((PegOrElseExpr)e).secondExpr);
		}
		else {
			s = s + e.toString();
		}
		return s;
	}

	public final String toPrintableString(String name) {
		return (this.joinPrintableString(name + " <- ", this));
	}

	public void setLeftRecursion(String leftLabel) {
	}

	final void checkAll(PegParser p, int level) {
		PegExpr e = this;
		while(e != null) {
			e.check(p, level);
			level = level + 1;
			e = e.nextExpr;
		}
	}
	abstract void check(PegParser p, int level);

	private static boolean sliceGroup(ParserContext sourceContext, BToken token, int openChar, int closeChar) {
		int level = 1;
		while(sourceContext.hasChar()) {
			char ch = sourceContext.nextChar();
			if(ch == closeChar) {
				level = level - 1;
				if(level == 0) {
					token.EndIndex = sourceContext.getPosition() - 1;
					return true;
				}
			}
			if(ch == openChar) {
				level = level + 1;
			}
			if(ch == '"' || ch == '\'') {
				if(!sourceContext.sliceQuotedTextUntil(token, ch, "")) {
					return false;
				}
				sourceContext.consume(1);
			}
			if(ch == '[') {
				sourceContext.consume(1);
				if(!sourceContext.sliceQuotedTextUntil(token, ']', "")) {
					return false;
				}
				sourceContext.consume(1);
			}
		}
		return false;
	}

	private static PegExpr _ParsePostfix(ParserContext sourceContext, PegExpr left) {
		if(left != null && sourceContext.hasChar()) {
			char ch = sourceContext.getChar();
			if(ch == ' ') {
				return left;
			}
			BToken source = sourceContext.newToken();
			if(ch == '*') {
				sourceContext.consume(1);
				return new PegOneMoreExpr(source, left, 0);
			}
			if(ch == '?') {
				sourceContext.consume(1);
				return new PegOptionalExpr(source, left);
			}
			if(ch == '+') {  // a+  => a
				sourceContext.consume(1);
				return new PegOneMoreExpr(source, left, 1);
			}
			System.out.println("unknown postfix = '" + ch + "'");
		}
		return left;
	}

	private static PegExpr _ParseSingleExpr(ParserContext sourceContext) {
		PegExpr right = null;
		sourceContext.skipWhiteSpace(false);
		BToken source = sourceContext.newToken();
		char ch = sourceContext.getChar();
		//System.out.println(">> " + ch + " next=" + Context.GetPosition());
		if(ch == '\0') {
			return null;
		}
		if(sourceContext.isSymbolLetter(ch)) {
			if(sourceContext.sliceSymbol(source, ".")) {
				right = new PegLabelExpr(source, source.GetText());
				return PegExpr._ParsePostfix(sourceContext, right);
			}
		}
		sourceContext.consume(1);
		if(ch == '\'' || ch == '"') {
			if(sourceContext.sliceQuotedTextUntil(source, ch, "")) {
				source.EndIndex = sourceContext.consume(1);
				right = new PegTokenExpr(source, LibBunSystem._UnquoteString(source.GetText()));
				return PegExpr._ParsePostfix(sourceContext, right);
			}
		}
		if(ch == '.') {
			right = new PegAnyExpr(source);
			return PegExpr._ParsePostfix(sourceContext, right);
		}
		if(ch == '[') {
			source.StartIndex = sourceContext.getPosition();
			if(sourceContext.sliceQuotedTextUntil(source, ']', "")) {
				source.EndIndex = sourceContext.getPosition();
				sourceContext.consume(1);
				right = new PegCharUnionExpr(source, LibBunSystem._UnquoteString(source.GetText()));
				return PegExpr._ParsePostfix(sourceContext, right);
			}
		}
		if(ch == '$') {
			char n = sourceContext.nextChar();
			if(sourceContext.sliceMatchedText(source, "=")) {
				right = PegExpr._ParseSingleExpr(sourceContext);
				if(right != null) {
					right = new PegPushExpr(source, n, right);
				}
				return right;
			}
			System.out.println("unfound '='");
			return null;
		}
		if(ch == '&') {
			right = PegExpr._ParseSingleExpr(sourceContext);
			if(right != null) {
				right = new PegAndExpr(source, right);
			}
			return right;
		}
		if(ch == '!') {
			right = PegExpr._ParseSingleExpr(sourceContext);
			if(right != null) {
				right = new PegNotExpr(source, right);
			}
			return right;
		}
		if(ch == '(') {
			source.StartIndex = sourceContext.getPosition();
			if(PegExpr.sliceGroup(sourceContext, source, ch, ')')) {
				ParserContext sub = source.newParserContext(sourceContext.parser);
				right = PegExpr._ParsePegExpr(sub);
				if(right != null) {
					right = PegExpr._ParsePostfix(sourceContext, right);
				}
				return right;
			}
			System.out.println("unclosed '" + ch + "'");
			return null;
		}
		if(ch == '{') {
			source.StartIndex = sourceContext.getPosition();
			if(PegExpr.sliceGroup(sourceContext, source, ch, '}')) {
				ParserContext sub = source.newParserContext(sourceContext.parser);
				right = PegExpr._ParsePegExpr(sub);
				if(right != null) {
					right = new PegNodeExpr(source, right);
				}
				return right;
			}
			System.out.println("unclosed '" + ch + "'");
			return null;
		}
		System.out.println("unknown char = '" + ch + "'");
		return right;
	}

	private final static PegExpr _ParseSequenceExpr(ParserContext sourceContext) {
		PegExpr left = PegExpr._ParseSingleExpr(sourceContext);
		if(left == null) {
			return left;
		}
		sourceContext.skipWhiteSpace(false);
		if(sourceContext.hasChar()) {
			sourceContext.skipWhiteSpace(false);
			char ch = sourceContext.getChar();
			if(ch == '/') {
				sourceContext.consume(1);
				sourceContext.skipWhiteSpace(false);
				return left;
			}
			PegExpr right = PegExpr._ParseSequenceExpr(sourceContext);
			if(right != null) {
				left.appendNext(right);
			}
		}
		return left;
	}

	public final static PegExpr _ParsePegExpr(ParserContext sourceContext) {
		PegExpr left = PegExpr._ParseSequenceExpr(sourceContext);
		sourceContext.skipWhiteSpace(false);
		if(sourceContext.hasChar()) {
			PegExpr right = PegExpr._ParsePegExpr(sourceContext);
			if(right != null) {
				return new PegOrElseExpr(sourceContext.newToken(), left, right);
			}
		}
		return left;
	}

	protected void dump(String msg) {
		if(this.source != null) {
			System.out.println(this.source.Source.FormatErrorMarker("*", this.source.StartIndex, msg));
		}
		else {
			System.out.println("unknown source: " + msg);
		}
	}

	protected void warning(String msg) {
		LibBunSystem._PrintLine("PEG warning: " + msg);
	}

	protected void debug(String msg) {
		System.out.println("debug: " + msg + "   @" + this);
	}

	protected void debugMatched(String msg) {
		//System.out.println("matched: " + msg + "   @" + this);
	}

	protected void debugUnmatched(String msg) {
		//System.out.println("unmatched: " + msg + "   @" + this);
	}
}

abstract class PegSymbolExpr extends PegExpr {
	String symbol;
	public PegSymbolExpr (BToken source, String symbol) {
		super(source);
		this.symbol = symbol;
	}
}

class PegTokenExpr extends PegSymbolExpr {
	public PegTokenExpr(BToken source, String symbol) {
		super(source, symbol);
	}
	@Override protected String stringfy() {
		return LibBunSystem._QuoteString("'", this.symbol, "'");
	}
	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		BToken token = sourceContext.newToken();
		if(sourceContext.sliceMatchedText(token, this.symbol)) {
			if(parentNode.SourceToken == null) {
				parentNode.SourceToken = token;
			}
			return parentNode;
		}
		return sourceContext.createExpectedErrorNode(parentNode, token, this.symbol);
	}
	@Override public String firstChars(BunMap<PegExpr> m) {
		return ""+this.symbol.charAt(0);
	}
	@Override void check(PegParser p, int level) {
		p.keywordCache.put(this.symbol, this.symbol);
		if(level == 0) {
			String ch = this.firstChars(null);
			p.firstCharCache.put(ch, ch);
		}
	}
}

class PegAnyExpr extends PegSymbolExpr {
	public PegAnyExpr(BToken source) {
		super(source, ".");
	}
	@Override protected String stringfy() {
		return ".";
	}
	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		if(sourceContext.hasChar()) {
			sourceContext.consume(1);
			return parentNode;
		}
		return new ErrorNode(parentNode, sourceContext.newToken(), "no more characters");
	}
	@Override public String firstChars(BunMap<PegExpr> m) {
		return "";
	}
	@Override void check(PegParser p, int level) {
	}
}

class PegCharUnionExpr extends PegTokenExpr {
	String charSet;
	public PegCharUnionExpr(BToken source, String token) {
		super(source, token);
		token = token.replaceAll("A-Z", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		token = token.replaceAll("a-z", "abcdefghijklmnopqrstuvwxyz");
		token = token.replaceAll("A-z", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
		token = token.replaceAll("0-9", "0123456789");
		token = token.replaceAll("\\-", "-");
		this.charSet = token;
	}
	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		char ch = sourceContext.nextChar();
		if(this.charSet.indexOf(ch) == -1) {
			return sourceContext.CreateUnexpectedCharacterNode(parentNode, ch);
		}
		//this.debugMatched("char '"+ch+"'");
		return parentNode;
	}
	@Override protected String stringfy() {
		return "[" + this.symbol + "]";
	}
	@Override public String firstChars(BunMap<PegExpr> m) {
		return this.charSet;
	}
	@Override void check(PegParser p, int level) {
	}
}

class PegPushExpr extends PegExpr {
	PegExpr innerExpr;
	int nodeAppendIndex = -2;
	public PegPushExpr(BToken source, char n, PegExpr e) {
		super(source);
		this.innerExpr = e;
		if('0' <= n && n <= '9') {
			this.nodeAppendIndex = (int)LibBunSystem._ParseInt(""+n);
		}
		else {
			this.nodeAppendIndex = -1;
		}
	}
	@Override protected String stringfy() {
		if(this.nodeAppendIndex == -1) {
			return "$n="+this.groupfy(this.innerExpr);
		}
		return "$" + this.nodeAppendIndex + "=" + this.groupfy(this.innerExpr);
	}

	@Override public String firstChars(BunMap<PegExpr> m) {
		return this.innerExpr.firstChars(m);
	}

	@Override void check(PegParser p, int level) {
		this.innerExpr.checkAll(p, level);
	}

	public final BNode appendNode(BNode parentNode, BNode childNode, ParserContext sourceContext) {
		if(parentNode == childNode) {
			this.warning("node was not created: " + childNode);
			return parentNode;
		}
		if(this.nodeAppendIndex != -2) {
			//			sourceContext.pushLog("push " + this.nodeAppendIndex + " " + parentNode + "/" + childNode + " for " + this.innerExpr);
			//			this.innerExpr.dump("push " + this.nodeAppendIndex + " " + parentNode + "/" + childNode + " for " + this.innerExpr);
			sourceContext.push(this, parentNode, this.nodeAppendIndex, childNode);
			//			if(this.nodeAppendIndex == 1 && parentNode instanceof BinaryOperatorNode) {
			//				return ((BinaryOperatorNode)parentNode).SetRightBinaryNode(childNode);
			//			}
			//			if(parentNode instanceof PegNode) {
			//				((PegNode) parentNode).setOrAppend(this.nodeAppendIndex, childNode);
			//			}
			//			else {
			//				parentNode.SetNode(this.nodeAppendIndex, childNode);
			//			}
			return parentNode;
		}
		return childNode;
	}

	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		BNode node = this.innerExpr.matchAll(parentNode, sourceContext);
		//System.out.println("matched push" + node);
		if(node == null) {
			node = sourceContext.createExpectedErrorNode(parentNode, null, this.innerExpr.toString());
			return node;
		}
		if(node.IsErrorNode()) {
			return node;
		}
		return this.appendNode(parentNode, node, sourceContext);
	}
}

class PegLabelExpr extends PegTokenExpr {
	public PegLabelExpr(BToken source, String token) {
		super(source, token);
	}
	@Override protected String stringfy() {
		return this.symbol;
	}

	@Override public String firstChars(BunMap<PegExpr> m) {
		if(m != null) {
			PegExpr e = m.GetValue(this.symbol, null);
			if(e != null) {
				return e.firstChars(m);
			}
		}
		return "";
	}
	@Override void check(PegParser p, int level) {
		if(!p.hasPattern(this.symbol)) {
			LibBunSystem._PrintLine("undefined label: " + this.symbol);
		}
	}

	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		//System.out.println("matching " + parentNode + "   " + this.toString());
		BNode left = sourceContext.parseBunNode(parentNode, this.symbol);
		if(left == null || left.IsErrorNode()) {
			return left;
		}
		if(sourceContext.isLeftRecursion(this.symbol)) {
			int stackPosition = sourceContext.getStackPosition(this);
			//System.out.println("trying left recursion of " + this.token + " ...");
			BNode rightNode = sourceContext.matchRightPattern(left, this.symbol);
			if(rightNode != null && !rightNode.IsErrorNode()) {
				//System.out.println("ok left recursion of " + this.token + " ..." + rightNode);
				return rightNode;
			}
			sourceContext.popBack(stackPosition, PegExpr._BackTrack);
		}
		return left;
	}
}

abstract class PegPredicate extends PegExpr {
	PegExpr innerExpr;
	public PegPredicate(BToken source, PegExpr e) {
		super(source);
		this.innerExpr = e;
	}
}

class PegNodeExpr extends PegPredicate {
	String nodeName = null;
	String leftLabel = null;

	public PegNodeExpr(BToken source, PegExpr e) {
		super(source, e);
		if(e instanceof PegSymbolExpr) {
			this.nodeName = ((PegSymbolExpr)e).symbol;
		}
	}

	@Override protected String stringfy() {
		return "{" + this.innerExpr + "}"; // + this.nodeName;
	}

	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		// prefetch first node..
		int stack = sourceContext.getStackPosition(this);
		BNode node = this.innerExpr.match(parentNode, sourceContext);
		sourceContext.popBack(stack, PegExpr._BackTrack);
		if(node == null || node.IsErrorNode()) {
			return node;
		}
		stack = sourceContext.getStackPosition(this);
		if(this.leftLabel != null) {
			BNode LeftNode = parentNode;
			parentNode = sourceContext.newNode(this.nodeName, LeftNode.ParentNode);
			//sourceContext.pushLog(this, "create: " + this.nodeName + " -> " + parentNode);
			//			parentNode.SetNode(0, LeftNode);
			sourceContext.push(this, parentNode, 0, LeftNode);
			//			System.out.println("new left: " + parentNode);
		}
		else {
			parentNode = sourceContext.newNode(this.nodeName, parentNode);
			//sourceContext.pushLog(this, "create: " + this.nodeName + " -> " + parentNode);
		}
		node = this.innerExpr.matchAll(parentNode, sourceContext);
		if(node == null || node.IsErrorNode()) {
			sourceContext.popBack(stack, PegExpr._BackTrack);
		}
		else {
			int top = sourceContext.getStackPosition(this);
			for(int i = stack; i < top; i++) {
				Log log = sourceContext.logStack.ArrayValues[i];
				if(log.type == 'p') {
					if(log.parentNode == parentNode) {
						if(parentNode instanceof PegNode) {
							((PegNode) parentNode).setOrAppend(log.index, log.childNode);
						}
						else{
							parentNode.SetNode(log.index, log.childNode);
						}
					}
					else {
						this.warning("different parent by" + log.trace + "\n\tcreated: " + parentNode + "\n\tset " + log.parentNode + "\n\tat " + log.index + ": " + log.childNode);
					}
				}
			}
			//			sourceContext.pushLog("created: " + this.nodeName + " -> " + node);
			//			int top = sourceContext.getStackPosition(this);
			//			for(int i = 0; i < top; i++) {
			//				if(i < stack) {
			//					System.out.println("LOG : " + sourceContext.logStack.ArrayValues[i]);
			//				}
			//				else {
			//					System.out.println("LOG*: " + sourceContext.logStack.ArrayValues[i]);
			//				}
			//			}
			//			sourceContext.popBack(stack, false);
			//			sourceContext.pushLog("closeNode: " + pos);
		}
		return node;
	}

	@Override public String firstChars(BunMap<PegExpr> m) {
		return this.innerExpr.firstChars(m);
	}
	@Override void check(PegParser p, int level) {
		this.innerExpr.checkAll(p, level);
	}

	@Override public void setLeftRecursion(String left) {
		if(this.leftLabel == null) {
			this.nodeName = left + ":" + this.nodeName;
		}
		this.leftLabel = left;
	}
}

class PegOptionalExpr extends PegPredicate {
	public PegOptionalExpr(BToken source, PegExpr e) {
		super(source, e);
	}
	@Override protected String stringfy() {
		return this.groupfy(this.innerExpr) + "?";
	}
	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		BNode node = parentNode;
		int stackPosition = sourceContext.getStackPosition(this);
		node = this.innerExpr.matchAll(node, sourceContext);
		if(node == null || node.IsErrorNode()) {
			sourceContext.popBack(stackPosition, PegExpr._BackTrack);
			node = parentNode;
		}
		return node;
	}
	@Override public String firstChars(BunMap<PegExpr> m) {
		if(this.nextExpr == null) {
			return this.innerExpr.firstChars(m);
		}
		else {
			return this.innerExpr.firstChars(m) + this.nextExpr.firstChars(m);
		}
	}
	@Override void check(PegParser p, int level) {
		this.innerExpr.checkAll(p, level);
	}

}

class PegOneMoreExpr extends PegPredicate {
	int min = 0;
	public PegOneMoreExpr(BToken source, PegExpr e, int min) {
		super(source, e);
		this.min = min;
	}
	@Override protected String stringfy() {
		if(this.min == 0) {
			return this.groupfy(this.innerExpr) + "*";
		}
		else {
			return this.groupfy(this.innerExpr) + "+";
		}
	}
	@Override public BNode match(BNode parentNode, ParserContext context) {
		BNode node = parentNode;
		int count = 0;
		while(node != null && !node.IsErrorNode()) {
			if(node != parentNode) {
				this.warning("ignored result of " + this.innerExpr);
				node = new PegNode(parentNode, node);
			}
			node = this.innerExpr.matchAll(node, context);
			count = count + 1;
		}
		if(!(count > this.min)) {
			this.debug("less than min=" + this.min);
			return null;
		}
		return parentNode;
	}
	@Override public String firstChars(BunMap<PegExpr> m) {
		if(this.nextExpr == null) {
			return this.innerExpr.firstChars(m);
		}
		else {
			return this.innerExpr.firstChars(m) + this.nextExpr.firstChars(m);
		}
	}
	@Override void check(PegParser p, int level) {
		this.innerExpr.checkAll(p, level);
	}
}

class PegOrElseExpr extends PegExpr {
	PegExpr firstExpr;
	PegExpr secondExpr;
	PegOrElseExpr(BToken source, PegExpr e, PegExpr e2) {
		super(source);
		this.firstExpr = e;
		this.secondExpr = e2;
	}
	@Override protected String stringfy() {
		return this.firstExpr + " / " + this.secondExpr;
	}

	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		PegExpr e = this;
		int stackPosition = sourceContext.getStackPosition(this);

		while(e instanceof PegOrElseExpr) {
			BNode node = parentNode;
			node = ((PegOrElseExpr) e).firstExpr.matchAll(node, sourceContext);
			if(node != null && !node.IsErrorNode()) {
				return node;
			}
			sourceContext.popBack(stackPosition, PegExpr._BackTrack);
			e = ((PegOrElseExpr) e).secondExpr;
		}
		return this.secondExpr.matchAll(parentNode, sourceContext);
	}
	@Override public String firstChars(BunMap<PegExpr> m) {
		return this.firstExpr.firstChars(m) + this.secondExpr.firstChars(m);
	}
	@Override void check(PegParser p, int level) {
		this.firstExpr.checkAll(p, level);
		this.secondExpr.checkAll(p, level);
	}
}

class PegAndExpr extends PegPredicate {
	PegAndExpr(BToken source, PegExpr e) {
		super(source, e);
	}
	@Override protected String stringfy() {
		return "&" + this.groupfy(this.innerExpr);
	}
	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		BNode node = parentNode;
		int stackPosition = sourceContext.getStackPosition(this);
		node = this.innerExpr.matchAll(node, sourceContext);
		sourceContext.popBack(stackPosition, PegExpr._BackTrack);
		return node;
	}
	@Override public String firstChars(BunMap<PegExpr> m) {
		return this.innerExpr.firstChars(m);
	}
	@Override void check(PegParser p, int level) {
		this.innerExpr.checkAll(p, level);
	}

}

class PegNotExpr extends PegPredicate {
	PegNotExpr(BToken source, PegExpr e) {
		super(source, e);
	}
	@Override protected String stringfy() {
		return "!" + this.groupfy(this.innerExpr);
	}
	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		BNode Node = parentNode;
		int stackPosition = sourceContext.getStackPosition(this);
		Node = this.innerExpr.matchAll(Node, sourceContext);
		sourceContext.popBack(stackPosition, PegExpr._BackTrack);
		if(Node == null || Node.IsErrorNode()) {
			return parentNode;
		}
		return null;
	}
	@Override void check(PegParser p, int level) {
		this.innerExpr.checkAll(p, level);
	}
}

class PegFunctionExpr extends PegExpr {
	PegFunction f;
	PegFunctionExpr(PegFunction f) {
		super(null);
		this.f = f;
	}
	@Override protected String stringfy() {
		return "(peg function " + this.f + ")";
	}
	@Override public BNode match(BNode parentNode, ParserContext sourceContext) {
		BNode node = this.f.Invoke(parentNode, sourceContext);
		//sourceContext.pushLog(this, "create " + parentNode + "/" + node + " by " + this.f);
		return node;
	}
	@Override void check(PegParser p, int level) {

	}
}
