package libbun.parser.peg;

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
		if(e instanceof PegChoice) {
			return ((PegChoice) e).firstExpr;
		}
		return e;
	}

	PegObject lazyMatchAll(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		Peg e = this;
		PegObject node = parentNode;
		while(e.nextExpr != null) {
			node = e.lazyMatch(node, sourceContext, hasNextChoice);
			if(node.isErrorNode()) {
				return node;
			}
			e = e.nextExpr;
		}
		return e.lazyMatch(node, sourceContext, hasNextChoice);
	}

	protected PegObject debugMatch(PegObject node, ParserContext sourceContext, boolean hasNextChoice) {
		PegObject node2 = this.lazyMatch(node, sourceContext, hasNextChoice);
		if(!node2.isErrorNode()) {
			String line = sourceContext.debugToken.Source.FormatErrorMarker("matched", sourceContext.getPosition(), "...");
			this.debug(line + "\n\tnode " + node + " => " + node2 + ".    ");
		}
		return node2;
	}
	protected abstract PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice);

	public String firstChars(BunMap<Peg> m) {
		return "";
	}

	@Override public String toString() {
		if(this.nextExpr != null) {
			String s = "";
			Peg e = this;
			while(e != null) {
				if(e instanceof PegChoice) {
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
		if(e.nextExpr != null && e instanceof PegToken) {
			return e.toString();
		}
		else {
			return "(" + e.toString() + ")";
		}
	}
	protected abstract String stringfy();

	private String joinPrintableString(String s, Peg e) {
		if(e instanceof PegChoice) {
			s = s + ((PegChoice)e).firstExpr;
			s = s + "\n\t/ ";
			s = this.joinPrintableString(s, ((PegChoice)e).secondExpr);
		}
		else {
			s = s + e.toString();
		}
		return s;
	}

	public final String toPrintableString(String name) {
		return (this.joinPrintableString(name + " <- ", this));
	}

	final boolean checkAll(PegParser p, String leftName, int order) {
		Peg e = this;
		boolean checkResult = true;
		while(e != null) {
			if(!e.check(p, leftName, order)) {
				checkResult = false;
			}
			order = order + 1;
			e = e.nextExpr;
		}
		return checkResult;
	}
	boolean check(PegParser p, String leftName, int order) {
		return true; //
	}

	private static boolean sliceGroup(ParserContext sourceContext, BToken token, int openChar, int closeChar) {
		int order = 1;
		while(sourceContext.hasChar()) {
			char ch = sourceContext.nextChar();
			if(ch == closeChar) {
				order = order - 1;
				if(order == 0) {
					token.EndIndex = sourceContext.getPosition() - 1;
					return true;
				}
			}
			if(ch == openChar) {
				order = order + 1;
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
				right = new PegLabel(source, source.GetText());
				return Peg._ParsePostfix(sourceContext, right);
			}
		}
		sourceContext.consume(1);
		if(ch == '\'' || ch == '"') {
			if(sourceContext.sliceQuotedTextUntil(source, ch, "")) {
				source.EndIndex = sourceContext.consume(1);
				right = new PegToken(source, LibBunSystem._UnquoteString(source.GetText()));
				return Peg._ParsePostfix(sourceContext, right);
			}
		}
		if(ch == '.') {
			right = new PegAny(source);
			return Peg._ParsePostfix(sourceContext, right);
		}
		if(ch == '[') {
			source.StartIndex = sourceContext.getPosition();
			if(sourceContext.sliceQuotedTextUntil(source, ']', "")) {
				source.EndIndex = sourceContext.getPosition();
				sourceContext.consume(1);
				right = new PegCharacter(source, LibBunSystem._UnquoteString(source.GetText()));
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
				right = new PegAndPredicate(source, right);
			}
			return right;
		}
		if(ch == '!') {
			right = Peg._ParseSingleExpr(sourceContext);
			if(right != null) {
				right = new PegNotPredicate(source, right);
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
					right = new PegNewObject(source, leftJoin, right, name);
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
				return new PegChoice(sourceContext.newToken(), left, right);
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
		System.out.println("debug: " + msg + "   by " + this.stringfy() + " <" + this + ">");
	}

	protected void debugMatched(String msg) {
		//System.out.println("matched: " + msg + "   @" + this);
	}

	protected void debugUnmatched(String msg) {
		//System.out.println("unmatched: " + msg + "   @" + this);
	}
}

abstract class PegAbstractSymbol extends Peg {
	String symbol;
	public PegAbstractSymbol (BToken source, String symbol) {
		super(source);
		this.symbol = symbol;
	}
}

class PegToken extends PegAbstractSymbol {
	public PegToken(BToken source, String symbol) {
		super(source, symbol);
	}
	@Override protected String stringfy() {
		return LibBunSystem._QuoteString("'", this.symbol, "'");
	}

	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		//sourceContext.skipWhiteSpace(false);
		BToken token = sourceContext.newToken();
		//System.out.println("? ch = " + sourceContext.getChar() + " in " + this.symbol + " at pos = " + sourceContext.getPosition());
		if(sourceContext.sliceMatchedText(token, this.symbol)) {
			if(parentNode.endIndex == 0) {
				parentNode.startIndex = token.StartIndex;
				parentNode.endIndex = token.EndIndex;
			}
			return parentNode;
		}
		return sourceContext.newExpectedErrorNode(this, this, hasNextChoice);
	}

	@Override public String firstChars(BunMap<Peg> m) {
		return ""+this.symbol.charAt(0);
	}

	@Override boolean check(PegParser p, String leftName, int order) {
		p.keywordCache.put(this.symbol, this.symbol);
		if(p.enableFirstCharChache && order == 0) {
			String ch = this.firstChars(null);
			p.setFirstCharSet(ch, this);
		}
		return true;
	}
}

class PegAny extends PegAbstractSymbol {
	public PegAny(BToken source) {
		super(source, ".");
	}
	@Override protected String stringfy() {
		return ".";
	}

	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		if(sourceContext.hasChar()) {
			sourceContext.consume(1);
			return parentNode;
		}
		return sourceContext.newExpectedErrorNode(this, this, hasNextChoice);
	}

	@Override boolean check(PegParser p, String leftName, int order) {
		if(p.enableFirstCharChache && order == 0) {
			p.setFirstCharSet("", this);
		}
		return true;
	}

}

class PegCharacter extends PegAbstractSymbol {
	String charSet;
	public PegCharacter(BToken source, String token) {
		super(source, token);
		token = token.replaceAll("A-Z", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		token = token.replaceAll("a-z", "abcdefghijklmnopqrstuvwxyz");
		token = token.replaceAll("A-z", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
		token = token.replaceAll("0-9", "0123456789");
		token = token.replaceAll("\\-", "-");
		this.charSet = token;
	}
	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		char ch = sourceContext.getChar();
		//System.out.println("? ch = " + ch + " in " + this.charSet + " at pos = " + sourceContext.getPosition());
		if(this.charSet.indexOf(ch) == -1) {
			return sourceContext.newExpectedErrorNode(this, this, hasNextChoice);
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
	@Override boolean check(PegParser p, String leftName, int order) {
		if(p.enableFirstCharChache && order == 0) {
			p.setFirstCharSet(this.charSet, this);
		}
		return true;
	}
}


class PegLabel extends PegAbstractSymbol {
	public PegLabel(BToken source, String token) {
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

	@Override boolean check(PegParser p, String leftName, int order) {
		if(!p.hasPattern(this.symbol)) {
			LibBunSystem._PrintLine("undefined label: " + this.symbol);
		}
		if(order == 0 && this.nextExpr != null) {
			Peg e = p.pegMap.GetValue(this.symbol, null);
			if(this.hasFirstLabel(e, p, leftName)) {
				System.out.println("find indirect left recursion" + leftName + " <- " + this.symbol + "...");
				return false;
			}
		}
		return true;
	}

	private boolean hasFirstLabel(Peg e, PegParser p, String leftName) {
		boolean result = false;
		while(e != null) {
			if(e instanceof PegChoice) {
				if(this.hasFirstLabel(((PegChoice) e).firstExpr, p, leftName)) {
					result = true;
				}
				e = ((PegChoice) e).secondExpr;
			}
			else if(e instanceof PegLabel) {
				String symbol = ((PegLabel) e).symbol;
				if(symbol.equals(leftName)) {
					return true;
				}
				e = p.pegMap.GetValue(symbol, null);
			}
			else {
				break;
			}
		}
		return result;
	}

	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		PegObject left = sourceContext.parsePegNode(parentNode, this.symbol, hasNextChoice);
		if(left.isErrorNode()) {
			return left;
		}
		if(sourceContext.isLeftRecursion(this.symbol)) {
			int stackPosition = sourceContext.getStackPosition(this);
			//System.out.println("trying left recursion of " + this.token + " ...");
			PegObject rightNode = sourceContext.parseRightPegNode(left, this.symbol);
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
	@Override boolean check(PegParser p, String leftName, int order) {
		return this.innerExpr.checkAll(p, leftName, order);
	}

}

class PegOptionalExpr extends PegPredicate {
	public PegOptionalExpr(BToken source, Peg e) {
		super(source, e);
	}
	@Override protected String stringfy() {
		return this.groupfy(this.innerExpr) + "?";
	}
	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		PegObject node = parentNode;
		int stackPosition = sourceContext.getStackPosition(this);
		node = this.innerExpr.lazyMatchAll(node, sourceContext, true);
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
	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		PegObject prevNode = parentNode;
		int count = 0;
		while(true) {
			boolean aChoice = true;
			if(count < this.min) {
				aChoice = hasNextChoice;
			}
			PegObject node = this.innerExpr.lazyMatchAll(prevNode, sourceContext, aChoice);
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
			return sourceContext.newExpectedErrorNode(this, this.innerExpr, hasNextChoice);
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
}

class PegAndPredicate extends PegPredicate {
	PegAndPredicate(BToken source, Peg e) {
		super(source, e);
	}
	@Override protected String stringfy() {
		return "&" + this.groupfy(this.innerExpr);
	}
	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		PegObject node = parentNode;
		int stackPosition = sourceContext.getStackPosition(this);
		node = this.innerExpr.lazyMatchAll(node, sourceContext, true);
		sourceContext.popBack(stackPosition, Peg._BackTrack);
		return node;
	}
	@Override public String firstChars(BunMap<Peg> m) {
		return this.innerExpr.firstChars(m);
	}

}

class PegNotPredicate extends PegPredicate {
	PegNotPredicate(BToken source, Peg e) {
		super(source, e);
	}
	@Override protected String stringfy() {
		return "!" + this.groupfy(this.innerExpr);
	}

	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		PegObject node = parentNode;
		int stackPosition = sourceContext.getStackPosition(this);
		node = this.innerExpr.lazyMatchAll(node, sourceContext, hasNextChoice);
		sourceContext.popBack(stackPosition, Peg._BackTrack);
		if(node.isErrorNode()) {
			return parentNode;
		}
		return sourceContext.newUnexpectedErrorNode(this, this.innerExpr, hasNextChoice);
	}
}


class PegChoice extends Peg {
	Peg firstExpr;
	Peg secondExpr;
	PegChoice(BToken source, Peg e, Peg e2) {
		super(source);
		this.firstExpr = e;
		this.secondExpr = e2;
	}
	@Override protected String stringfy() {
		return this.firstExpr + " / " + this.secondExpr;
	}

	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		Peg e = this;
		int stackPosition = sourceContext.getStackPosition(this);
		while(e instanceof PegChoice) {
			PegObject node = parentNode;
			node = ((PegChoice) e).firstExpr.lazyMatchAll(node, sourceContext, true);
			if(!node.isErrorNode()) {
				return node;
			}
			sourceContext.popBack(stackPosition, Peg._BackTrack);
			e = ((PegChoice) e).secondExpr;
		}
		return this.secondExpr.lazyMatchAll(parentNode, sourceContext, hasNextChoice);
	}

	@Override public String firstChars(BunMap<Peg> m) {
		return this.firstExpr.firstChars(m) + this.secondExpr.firstChars(m);
	}
	@Override boolean check(PegParser p, String leftName, int order) {
		boolean checkResult = true;
		if(!this.firstExpr.checkAll(p, leftName, order)) {
			checkResult = false;
		}
		if(!this.secondExpr.checkAll(p, leftName, order)) {
			checkResult = false;
		}
		return checkResult;
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

	@Override boolean check(PegParser p, String leftName, int order) {
		return this.innerExpr.checkAll(p, leftName, order);
	}

	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		int pos = sourceContext.getPosition();
		PegObject node = this.innerExpr.lazyMatchAll(parentNode, sourceContext, hasNextChoice);
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

class PegNewObject extends PegPredicate {
	boolean leftJoin = false;
	String nodeName = null;

	public PegNewObject(BToken source, boolean leftJoin, Peg e, String nodeName) {
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

	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		// prefetch first node..
		//sourceContext.skipWhiteSpace(true);
		int stack = sourceContext.getStackPosition(this);
		if(this.innerExpr instanceof PegToken) {
			PegObject node = this.innerExpr.lazyMatch(parentNode, sourceContext, hasNextChoice);
			sourceContext.popBack(stack, Peg._BackTrack);
			if(node.isErrorNode()) {
				return node;
			}
			stack = sourceContext.getStackPosition(this);
		}
		PegParsedNode newnode = sourceContext.newPegNode(this, sourceContext.getPosition(), 0);
		if(this.leftJoin) {
			sourceContext.push(this, newnode, 0, parentNode);
		}
		PegObject node = this.innerExpr.lazyMatchAll(newnode, sourceContext, hasNextChoice);
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
						newnode.set(log.index, (PegObject)log.childNode);
					}
				}
			}
			if(newnode.endIndex == 0 || newnode.elementList == null) {
				newnode.endIndex = sourceContext.getPosition();
			}
		}
		return newnode;
	}

	@Override public String firstChars(BunMap<Peg> m) {
		return this.innerExpr.firstChars(m);
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

	@Override public PegObject lazyMatch(PegObject parentNode, ParserContext sourceContext, boolean hasNextChoice) {
		PegObject node = this.f.Invoke(parentNode, sourceContext);
		if(node == null) {
			return sourceContext.newFunctionErrorNode(this, this.f, hasNextChoice);
		}
		node.createdPeg = this;
		return node;
	}

	@Override boolean check(PegParser p, String leftName, int order) {
		return true;
	}
}
