package libbun.parser.peg;

import libbun.ast.BNode;
import libbun.ast.error.ErrorNode;
import libbun.parser.BToken;
import libbun.parser.ParserSource;
import libbun.util.BArray;
import libbun.util.BField;
import libbun.util.BunMap;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public final class ParserContext  {
	@BField public final  ParserSource source;
	@BField private int   currentPosition = 0;
	@BField private final int endPosition;
	@BField public  PegParser    parser;
	@BField private final boolean IsAllowSkipIndent = false;

	public final BArray<Log> logStack = new BArray<Log>(new Log[64]);
	int stackTop = 0;
	BToken debugToken ;

	public ParserContext(PegParser Parser, ParserSource Source, int StartIndex, int EndIndex) {
		this.parser = Parser;
		this.source = Source;
		this.currentPosition = StartIndex;
		this.endPosition = EndIndex;
		this.debugToken = this.newToken(StartIndex, EndIndex);
	}

	ParserContext(PegParser Parser, String source) {
		this(Parser, new ParserSource("", 0, source, null), 0, source.length());
	}

	public ParserContext subContext(int startIndex, int endIndex) {
		return new ParserContext(this.parser, this.source, startIndex, endIndex);
	}

	@Override public String toString() {
		BToken token = this.newToken(this.currentPosition, this.endPosition);
		return token.GetText();
	}

	//	private boolean SetParseFlag(boolean AllowSkipIndent) {
	//		@Var boolean OldFlag = this.IsAllowSkipIndent;
	//		this.IsAllowSkipIndent = AllowSkipIndent;
	//		return OldFlag;
	//	}

	public int getPosition() {
		return this.currentPosition;
	}

	public void rollback(int pos) {
		//		if(pos == 0 && this.currentPosition > pos) {
		//			System.out.println("rollback first");
		//			new Exception().printStackTrace();
		//		}
		this.currentPosition = pos;
	}

	public final boolean hasChar() {
		return this.currentPosition < this.endPosition;
	}

	public final char charAt(int n) {
		return LibBunSystem._GetChar(this.source.SourceText, n);
	}

	public final char getChar() {
		if(this.hasChar()) {
			return this.charAt(this.currentPosition);
		}
		return '\0';
	}

	public final char getChar(int n) {
		int pos = this.currentPosition + n;
		if(pos >= 0 && pos < this.endPosition) {
			return this.charAt(pos);
		}
		return '\0';
	}

	public final int consume(int plus) {
		this.currentPosition = this.currentPosition + plus;
		return this.currentPosition;
	}

	public final char nextChar() {
		if(this.hasChar()) {
			int pos = this.currentPosition;
			this.consume(1);
			return this.charAt(pos);
		}
		return '\0';
	}

	public final int skipWhiteSpace(boolean IncludeNewLine) {
		if(IncludeNewLine) {
			while(this.hasChar()) {
				@Var char ch = this.charAt(this.currentPosition);
				if(ch != ' ' && ch != '\t' && ch != '\n') {
					break;
				}
				this.consume(1);
			}
		}
		else {
			while(this.hasChar()) {
				@Var char ch = this.charAt(this.currentPosition);
				if(ch != ' ' && ch != '\t') {
					break;
				}
				this.consume(1);
			}
		}
		return this.currentPosition;
	}

	public final BToken newToken() {
		return new BToken(this.source, this.currentPosition, this.currentPosition);
	}

	public final BToken newToken(int startIndex, int endIndex) {
		return new BToken(this.source, startIndex, endIndex);
	}

	public final boolean sliceNumber(BToken token) {
		char ch = this.nextChar();
		if(LibBunSystem._IsDigit(ch)) {
			for(;this.hasChar(); this.consume(1)) {
				ch = this.charAt(this.currentPosition);
				if(!LibBunSystem._IsDigit(ch)) {
					break;
				}
			}
			token.EndIndex = this.currentPosition;
			return true;
		}
		return false;
	}

	public final boolean isSymbolLetter(char ch) {
		return (LibBunSystem._IsLetter(ch)  || ch == '_');
	}

	public final boolean sliceSymbol(BToken token, String allowedChars) {
		char ch = this.nextChar();
		if(this.isSymbolLetter(ch) || allowedChars.indexOf(ch) != -1) {
			for(;this.hasChar(); this.consume(1)) {
				ch = this.charAt(this.currentPosition);
				if(!this.isSymbolLetter(ch) && !LibBunSystem._IsDigit(ch) && allowedChars.indexOf(ch) == -1) {
					break;
				}
			}
			token.EndIndex = this.currentPosition;
			return true;
		}
		return false;
	}

	public final boolean sliceMatchedText(BToken token, String text) {
		if(this.endPosition - this.currentPosition >= text.length()) {
			for(int i = 0; i < text.length(); i++) {
				//System.out.println("i="+i+", '"+text.charAt(i) + "', '"+this.charAt(this.currentPosition + i));
				if(text.charAt(i) != this.charAt(this.currentPosition + i)) {
					return false;
				}
			}
			this.consume(text.length());
			token.EndIndex = this.currentPosition;
			return true;
		}
		return false;
	}

	public final boolean sliceQuotedTextUntil(BToken token, char endChar, String stopChars) {
		for(; this.hasChar(); this.consume(1)) {
			char ch = this.charAt(this.currentPosition);
			if(ch == endChar) {
				token.EndIndex = this.currentPosition;
				return true;
			}
			if(stopChars.indexOf(ch) != -1) {
				break;
			}
			if(ch == '\\') {
				this.consume(1);  // skip next char;
			}
		}
		token.EndIndex = this.currentPosition;
		return false;
	}

	public final boolean sliceUntilWhiteSpace(BToken token, String stopChars) {
		for(; this.hasChar(); this.consume(1)) {
			char ch = this.charAt(this.currentPosition);
			if(ch == '\\') {
				this.consume(1);  // skip next char;
			}
			else {
				if(ch == ' ' || ch == '\t' || ch == '\n') {
					token.EndIndex = this.currentPosition;
					return true;
				}
				if(stopChars.indexOf(ch) != 0) {
					break;
				}
			}
		}
		token.EndIndex = this.currentPosition;
		return false;
	}

	public String getFirstChar() {
		this.skipWhiteSpace(this.IsAllowSkipIndent);
		return String.valueOf(this.getChar());
	}

	public BNode newNode(String NodeName, BNode ParentNode) {
		return this.parser.newNode(NodeName, ParentNode);
	}

	public boolean isLeftRecursion(String PatternName) {
		Peg e = this.parser.getRightPattern(PatternName, this.getFirstChar());
		return e != null;
	}

	//	public BNode matchRightPattern(BNode ParentNode, String PatternName) {
	//		Peg e = this.parser.getRightPattern(PatternName, this.getFirstChar());
	//		if(e != null) {
	//			int pos = this.currentPosition;
	//			BNode Node = e.matchAll(ParentNode, this);
	//			if(Node == null || Node.IsErrorNode()) {
	//				this.rollback(pos);
	//			}
	//			return Node;
	//		}
	//		return null;
	//	}

	private final BunMap<PegNode> memoMap = new BunMap<PegNode>(null);
	private final PegNode trueNode = new PegParsedNode(null, 0, 0);
	int memoHit = 0;
	int memoMiss = 0;
	int memoSize = 0;

	public final PegNode parsePegNode(PegNode parentNode, String pattern) {
		int pos = this.getPosition();
		String key = pattern + ":" + pos;
		PegNode node = this.memoMap.GetValue(key, null);
		if(node != null) {
			this.memoHit = this.memoHit + 1;
			if(node == this.trueNode) {
				return parentNode;
			}
			return node;
		}
		Peg e = this.parser.getPattern(pattern, this.getFirstChar());
		this.memoMiss = this.memoMiss + 1;
		//System.out.println("matching " + parentNode + "   " + pattern + "... " + e);
		if(e != null) {
			node = e.lazyMatchAll(parentNode, this);
		}
		//System.out.println("matched " + parentNode + "   " + pattern + "... " + node);
		if(node == parentNode) {
			this.memoMap.put(key, this.trueNode);
			return node;
		}
		this.memoMap.put(key, node);
		return node;
	}


	public PegNode parseRightPegNode(PegNode left, String symbol) {
		// TODO Auto-generated method stub
		return null;
	}

	public BNode createExpectedErrorNode(BNode parentNode, BToken token, String text) {
		if(token == null) {
			token = this.newToken();
		}
		return new ErrorNode(parentNode, token, "expected " + text);
	}

	public BNode CreateUnexpectedCharacterNode(BNode parentNode, char ch) {
		// TODO Auto-generated method stub
		return null;
	}

	final int getStackPosition(Peg trace) {
		this.pushImpl(trace, null, '\0', null, 0, null);
		return this.stackTop;
	}

	private void pushImpl(Peg trace, String msg, char type, Object parentNode, int index, Object childNode) {
		Log log = null;
		if(this.stackTop < this.logStack.size()) {
			if(this.logStack.ArrayValues[this.stackTop] == null) {
				this.logStack.ArrayValues[this.stackTop] = new Log();
			}
			log = this.logStack.ArrayValues[this.stackTop];
		}
		else {
			log = new Log();
			this.logStack.add(log);
		}
		log.trace = trace;
		log.sourcePosition = this.currentPosition;
		log.msg = msg;
		log.type = type;
		log.parentNode = parentNode;
		log.index = index;
		log.childNode = childNode;
		this.stackTop = this.stackTop + 1;
	}

	void pushLog(Peg trace, String msg) {
		this.pushImpl(trace, msg, 'm', null, 0, null);
	}

	void push(Peg trace, BNode parentNode, int index, BNode childNode) {
		this.pushImpl(trace, "", 'p', parentNode, index, childNode);
	}

	void popBack(int stackPostion, boolean backtrack) {
		this.stackTop = stackPostion-1;
		Log log = this.logStack.ArrayValues[stackPostion-1];
		if(backtrack) {
			this.rollback(log.sourcePosition);
		}
	}

	public void push(Peg trace, PegNode parentNode, int index, PegNode node) {
		this.pushImpl(trace, "", 'p', parentNode, index, node);
	}

	public PegNode newPegNode(Peg created, int startIndex, int endIndex) {
		PegNode node = new PegParsedNode(created, startIndex, endIndex);
		node.debugSource = this.debugToken;
		return node;
	}

	public PegNode newErrorNode(Peg created, String msg) {
		PegNode node = new PegFailureNode(created, this.currentPosition, msg);
		node.debugSource = this.debugToken;
		return node;
	}


}

class Log {
	int sourcePosition;
	Peg trace;
	String msg;
	char type;
	Object parentNode;
	int index;
	Object childNode;

	@Override public String toString() {
		return "" + this.sourcePosition + " " + this.msg;
	}
}
