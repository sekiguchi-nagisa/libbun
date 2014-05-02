package libbun.parser.peg;

import libbun.ast.BNode;
import libbun.parser.BToken;
import libbun.util.BunMap;
import libbun.util.LibBunSystem;

public abstract class Peg {
	public final static boolean _BackTrack = true;
	public BToken source;
	public int serialNumber = 0;
	public Peg nextExpr = null;

	Peg(BToken source) {
		this.source = source;
	}

	public void appendNext(Peg e) {
		Peg list = this;
		while(list.nextExpr != null) {
			list = list.nextExpr;
		}
		list.nextExpr = e;
	}

	Peg shorten(Peg e) {
		if(e instanceof PegOrElseExpr) {
			return ((PegOrElseExpr) e).firstExpr;
		}
		return e;
	}

	protected final BNode match(BNode ParentNode, ParserContext Context) {
		return null;
	}

	PegNode lazyMatchAll(PegNode parentNode, ParserContext sourceContext) {
		Peg e = this;
		PegNode node = parentNode;
		while(e.nextExpr != null) {
			node = e.lazyMatch(node, sourceContext);
			if(node.isErrorNode()) {
				return node;
			}
			e = e.nextExpr;
		}
		return e.lazyMatch(node, sourceContext);
	}

	protected PegNode debugMatch(PegNode node, ParserContext sourceContext) {
		PegNode node2 = this.lazyMatch(node, sourceContext);
		String line = sourceContext.debugToken.Source.FormatErrorMarker("matched", sourceContext.getPosition(), "...");
		this.debug(line + "\n\tnode " + node + " => " + node2 + ".    ");
		return node2;
	}
	protected abstract PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext);

	public String firstChars(BunMap<Peg> m) {
		return "";
	}

	@Override public String toString() {
		if(this.nextExpr != null) {
			String s = "";
			Peg e = this;
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

	public final String groupfy(Peg e) {
		if(e.nextExpr != null && e instanceof PegTokenExpr) {
			return e.toString();
		}
		else {
			return "(" + e.toString() + ")";
		}
	}
	protected abstract String stringfy();

	private String joinPrintableString(String s, Peg e) {
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
		Peg e = this;
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

	private static String sliceName(ParserContext sourceContext) {
		return null;
	}

	private static Peg _ParsePostfix(ParserContext sourceContext, Peg left) {
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

	private static Peg _ParseSingleExpr(ParserContext sourceContext) {
		Peg right = null;
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
				return Peg._ParsePostfix(sourceContext, right);
			}
		}
		sourceContext.consume(1);
		if(ch == '\'' || ch == '"') {
			if(sourceContext.sliceQuotedTextUntil(source, ch, "")) {
				source.EndIndex = sourceContext.consume(1);
				right = new PegTokenExpr(source, LibBunSystem._UnquoteString(source.GetText()));
				return Peg._ParsePostfix(sourceContext, right);
			}
		}
		if(ch == '.') {
			right = new PegAnyExpr(source);
			return Peg._ParsePostfix(sourceContext, right);
		}
		if(ch == '[') {
			source.StartIndex = sourceContext.getPosition();
			if(sourceContext.sliceQuotedTextUntil(source, ']', "")) {
				source.EndIndex = sourceContext.getPosition();
				sourceContext.consume(1);
				right = new PegCharUnionExpr(source, LibBunSystem._UnquoteString(source.GetText()));
				return Peg._ParsePostfix(sourceContext, right);
			}
		}
		//		if(ch == '$') {
		//			char n = sourceContext.nextChar();
		//			if(sourceContext.sliceMatchedText(source, "=")) {
		//				right = Peg._ParseSingleExpr(sourceContext);
		//				if(right != null) {
		//					right = new PegPushExpr(source, n, right);
		//				}
		//				return right;
		//			}
		//			System.out.println("unfound '='");
		//			return null;
		//		}
		if(ch == '$') {
			right = Peg._ParseSingleExpr(sourceContext);
			if(right != null) {
				right = new PegPushExpr(source, right);
			}
			return right;
		}
		if(ch == '&') {
			right = Peg._ParseSingleExpr(sourceContext);
			if(right != null) {
				right = new PegAndExpr(source, right);
			}
			return right;
		}
		if(ch == '!') {
			right = Peg._ParseSingleExpr(sourceContext);
			if(right != null) {
				right = new PegNotExpr(source, right);
			}
			return right;
		}
		if(ch == '(') {
			source.StartIndex = sourceContext.getPosition();
			if(Peg.sliceGroup(sourceContext, source, ch, ')')) {
				ParserContext sub = source.newParserContext(sourceContext.parser);
				right = Peg._ParsePegExpr(sub);
				if(right != null) {
					right = Peg._ParsePostfix(sourceContext, right);
				}
				return right;
			}
			System.out.println("unclosed '" + ch + "'");
			return null;
		}
		if(ch == '{') {
			char ch2 = sourceContext.getChar();
			boolean leftJoin = false;
			if(ch2 == '+') {
				sourceContext.consume(1);
				leftJoin = true;
			}
			source.StartIndex = sourceContext.getPosition();
			if(Peg.sliceGroup(sourceContext, source, ch, '}')) {
				String name = Peg.sliceName(sourceContext);
				ParserContext sub = source.newParserContext(sourceContext.parser);
				right = Peg._ParsePegExpr(sub);
				if(right != null) {
					right = new PegNodeExpr(source, leftJoin, right, name);
					right = Peg._ParsePostfix(sourceContext, right);
				}
				return right;
			}
			System.out.println("unclosed '{'");
			return null;
		}
		System.out.println("unknown char = '" + ch + "'");
		return right;
	}

	private final static Peg _ParseSequenceExpr(ParserContext sourceContext) {
		Peg left = Peg._ParseSingleExpr(sourceContext);
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
			Peg right = Peg._ParseSequenceExpr(sourceContext);
			if(right != null) {
				left.appendNext(right);
			}
		}
		return left;
	}

	public final static Peg _ParsePegExpr(ParserContext sourceContext) {
		Peg left = Peg._ParseSequenceExpr(sourceContext);
		sourceContext.skipWhiteSpace(false);
		if(sourceContext.hasChar()) {
			Peg right = Peg._ParsePegExpr(sourceContext);
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
		System.out.println("debug: " + msg + "   by " + this);
	}

	protected void debugMatched(String msg) {
		//System.out.println("matched: " + msg + "   @" + this);
	}

	protected void debugUnmatched(String msg) {
		//System.out.println("unmatched: " + msg + "   @" + this);
	}
}

abstract class PegSymbolExpr extends Peg {
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

	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		//		sourceContext.skipWhiteSpace(false);
		BToken token = sourceContext.newToken();
		if(sourceContext.sliceMatchedText(token, this.symbol)) {
			if(parentNode.endIndex == 0) {
				parentNode.startIndex = token.StartIndex;
				parentNode.endIndex = token.EndIndex;
			}
			return parentNode;
		}
		return sourceContext.newErrorNode(this, "expected " + this.symbol);
	}

	@Override public String firstChars(BunMap<Peg> m) {
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

	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		if(sourceContext.hasChar()) {
			sourceContext.consume(1);
			return parentNode;
		}
		return sourceContext.newErrorNode(this, "no more characters");
	}

	@Override public String firstChars(BunMap<Peg> m) {
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
	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		char ch = sourceContext.getChar();
		if(this.charSet.indexOf(ch) == -1) {
			return sourceContext.newErrorNode(this, "unexpected character: '" + ch + "'");
		}
		sourceContext.consume(1);
		return parentNode;
	}

	@Override protected String stringfy() {
		return "[" + this.symbol + "]";
	}
	@Override public String firstChars(BunMap<Peg> m) {
		return this.charSet;
	}
	@Override void check(PegParser p, int level) {
	}
}

class PegPushExpr extends Peg {
	Peg innerExpr;
	int nodeAppendIndex = -1;
	//	public PegPushExpr(BToken source, char n, Peg e) {
	//		super(source);
	//		this.innerExpr = e;
	//		if('0' <= n && n <= '9') {
	//			this.nodeAppendIndex = (int)LibBunSystem._ParseInt(""+n);
	//		}
	//		else {
	//			this.nodeAppendIndex = -1;
	//		}
	//	}
	public PegPushExpr(BToken source, Peg e) {
		super(source);
		this.innerExpr = e;
	}
	@Override protected String stringfy() {
		if(this.nodeAppendIndex == -1) {
			return "$"+this.groupfy(this.innerExpr);
		}
		return "$" + this.nodeAppendIndex + "=" + this.groupfy(this.innerExpr);
	}

	@Override public String firstChars(BunMap<Peg> m) {
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

	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		int pos = sourceContext.getPosition();
		PegNode node = this.innerExpr.lazyMatchAll(parentNode, sourceContext);
		if(node.isErrorNode()) {
			return node;
		}
		if(parentNode == node) {
			this.warning("node was not created: " + node);
			node = sourceContext.newPegNode(this, pos, sourceContext.getPosition());
			return parentNode;
		}
		sourceContext.push(this, parentNode, this.nodeAppendIndex, node);
		return parentNode;
	}

}

class PegLabelExpr extends PegTokenExpr {
	public PegLabelExpr(BToken source, String token) {
		super(source, token);
	}
	@Override protected String stringfy() {
		return this.symbol;
	}

	@Override public String firstChars(BunMap<Peg> m) {
		if(m != null) {
			Peg e = m.GetValue(this.symbol, null);
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

	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		PegNode left = sourceContext.parsePegNode(parentNode, this.symbol);
		if(left.isErrorNode()) {
			return left;
		}
		if(sourceContext.isLeftRecursion(this.symbol)) {
			int stackPosition = sourceContext.getStackPosition(this);
			//System.out.println("trying left recursion of " + this.token + " ...");
			PegNode rightNode = sourceContext.parseRightPegNode(left, this.symbol);
			if(!rightNode.isErrorNode()) {
				//System.out.println("ok left recursion of " + this.token + " ..." + rightNode);
				return rightNode;
			}
			sourceContext.popBack(stackPosition, Peg._BackTrack);
		}
		return left;
	}

}

abstract class PegPredicate extends Peg {
	Peg innerExpr;
	public PegPredicate(BToken source, Peg e) {
		super(source);
		this.innerExpr = e;
	}
}

class PegNodeExpr extends PegPredicate {
	boolean leftJoin = false;
	String nodeName = null;

	public PegNodeExpr(BToken source, boolean leftJoin, Peg e, String nodeName) {
		super(source, e);
		this.leftJoin = leftJoin;
		this.nodeName = nodeName;
	}

	@Override protected String stringfy() {
		String s = "{";
		if(this.leftJoin) {
			s = s + "+ ";
		}
		return s + this.innerExpr + "}"; // + this.nodeName;
	}


	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		// prefetch first node..
		//sourceContext.skipWhiteSpace(true);
		int stack = sourceContext.getStackPosition(this);
		PegNode node = this.innerExpr.lazyMatch(parentNode, sourceContext);
		sourceContext.popBack(stack, Peg._BackTrack);
		if(node.isErrorNode()) {
			return node;
		}
		stack = sourceContext.getStackPosition(this);
		PegNode newnode = sourceContext.newPegNode(this, sourceContext.getPosition(), 0);
		if(this.leftJoin) {
			sourceContext.push(this, newnode, 0, parentNode);
		}
		node = this.innerExpr.lazyMatchAll(newnode, sourceContext);
		if(node.isErrorNode()) {
			sourceContext.popBack(stack, Peg._BackTrack);
			return node;
		}
		else {
			int top = sourceContext.getStackPosition(this);
			for(int i = stack; i < top; i++) {
				Log log = sourceContext.logStack.ArrayValues[i];
				if(log.type == 'p') {
					if(log.parentNode == newnode) {
						newnode.set(log.index, (PegNode)log.childNode);
					}
				}
			}
			if(newnode.endIndex == 0) {
				newnode.endIndex = sourceContext.getPosition();
			}
		}
		return newnode;
	}

	@Override public String firstChars(BunMap<Peg> m) {
		return this.innerExpr.firstChars(m);
	}
	@Override void check(PegParser p, int level) {
		this.innerExpr.checkAll(p, level);
	}

}

class PegOptionalExpr extends PegPredicate {
	public PegOptionalExpr(BToken source, Peg e) {
		super(source, e);
	}
	@Override protected String stringfy() {
		return this.groupfy(this.innerExpr) + "?";
	}
	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		PegNode node = parentNode;
		int stackPosition = sourceContext.getStackPosition(this);
		node = this.innerExpr.lazyMatchAll(node, sourceContext);
		if(node.isErrorNode()) {
			sourceContext.popBack(stackPosition, Peg._BackTrack);
			node = parentNode;
		}
		return node;
	}

	@Override public String firstChars(BunMap<Peg> m) {
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
	public PegOneMoreExpr(BToken source, Peg e, int min) {
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
	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		PegNode prevNode = parentNode;
		int count = 0;
		while(true) {
			PegNode node = this.innerExpr.lazyMatchAll(prevNode, sourceContext);
			if(node.isErrorNode()) {
				break;
			}
			if(node != prevNode) {
				this.warning("ignored result of " + this.innerExpr);
			}
			prevNode = node;
			count = count + 1;
		}
		if(count < this.min) {
			return sourceContext.newErrorNode(this, "requiring at most " + this.min + " times");
		}
		//System.out.println("prevNode: " + prevNode + "s,e=" + prevNode.startIndex + ", " + prevNode.endIndex);
		return prevNode;
	}

	@Override public String firstChars(BunMap<Peg> m) {
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

class PegOrElseExpr extends Peg {
	Peg firstExpr;
	Peg secondExpr;
	PegOrElseExpr(BToken source, Peg e, Peg e2) {
		super(source);
		this.firstExpr = e;
		this.secondExpr = e2;
	}
	@Override protected String stringfy() {
		return this.firstExpr + " / " + this.secondExpr;
	}

	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		Peg e = this;
		int stackPosition = sourceContext.getStackPosition(this);
		while(e instanceof PegOrElseExpr) {
			PegNode node = parentNode;
			node = ((PegOrElseExpr) e).firstExpr.lazyMatchAll(node, sourceContext);
			if(!node.isErrorNode()) {
				return node;
			}
			sourceContext.popBack(stackPosition, Peg._BackTrack);
			e = ((PegOrElseExpr) e).secondExpr;
		}
		return this.secondExpr.lazyMatchAll(parentNode, sourceContext);
	}

	@Override public String firstChars(BunMap<Peg> m) {
		return this.firstExpr.firstChars(m) + this.secondExpr.firstChars(m);
	}
	@Override void check(PegParser p, int level) {
		this.firstExpr.checkAll(p, level);
		this.secondExpr.checkAll(p, level);
	}
}

class PegAndExpr extends PegPredicate {
	PegAndExpr(BToken source, Peg e) {
		super(source, e);
	}
	@Override protected String stringfy() {
		return "&" + this.groupfy(this.innerExpr);
	}
	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		PegNode node = parentNode;
		int stackPosition = sourceContext.getStackPosition(this);
		node = this.innerExpr.lazyMatchAll(node, sourceContext);
		sourceContext.popBack(stackPosition, Peg._BackTrack);
		return node;
	}
	@Override public String firstChars(BunMap<Peg> m) {
		return this.innerExpr.firstChars(m);
	}
	@Override void check(PegParser p, int level) {
		this.innerExpr.checkAll(p, level);
	}

}

class PegNotExpr extends PegPredicate {
	PegNotExpr(BToken source, Peg e) {
		super(source, e);
	}
	@Override protected String stringfy() {
		return "!" + this.groupfy(this.innerExpr);
	}

	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		PegNode node = parentNode;
		int stackPosition = sourceContext.getStackPosition(this);
		node = this.innerExpr.lazyMatchAll(node, sourceContext);
		sourceContext.popBack(stackPosition, Peg._BackTrack);
		if(node.isErrorNode()) {
			return parentNode;
		}
		return sourceContext.newErrorNode(this, "unexpected " + node.toString());
	}

	@Override void check(PegParser p, int level) {
		this.innerExpr.checkAll(p, level);
	}
}

class PegFunctionExpr extends Peg {
	PegFunction f;
	PegFunctionExpr(PegFunction f) {
		super(null);
		this.f = f;
	}
	@Override protected String stringfy() {
		return "(peg function " + this.f + ")";
	}

	@Override public PegNode lazyMatch(PegNode parentNode, ParserContext sourceContext) {
		PegNode node = this.f.Invoke2(parentNode, sourceContext);
		if(node == null) {
			return sourceContext.newErrorNode(this, "function " + this.f + " failed");
		}
		node.createdPeg = this;
		return node;
	}

	@Override void check(PegParser p, int level) {

	}
}
