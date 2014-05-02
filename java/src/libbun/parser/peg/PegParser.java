package libbun.parser.peg;

import libbun.ast.BNode;
import libbun.parser.BToken;
import libbun.parser.LibBunLogger;
import libbun.parser.ParserSource;
import libbun.util.BArray;
import libbun.util.BunMap;
import libbun.util.LibBunSystem;

public final class PegParser {
	public LibBunLogger logger;
	PegParser stackedParser;
	private int serialNumberCount = 1;
	BunMap<Peg>  pegMap;
	BunMap<BNode>    nodeMap;
	BunMap<Peg>  pegCache = null;
	BunMap<String>   keywordCache = null;
	BunMap<String>   firstCharCache = null;

	public PegParser(LibBunLogger logger, PegParser StackedParser) {
		this.logger = logger;
		this.stackedParser = StackedParser;
		this.Init();
	}

	public void Init() {
		this.pegMap = new BunMap<Peg>(null);
		this.nodeMap = new BunMap<BNode>(null);
	}

	public PegParser Pop() {
		return this.stackedParser;
	}

	public final boolean loadPegFile(String file) {
		String text = LibBunSystem._LoadTextFile(file);
		if(text == null) {
			LibBunSystem._Exit(1, "file not found: " + file);
		}
		ParserContext sourceContext = new ParserContext(this, new ParserSource(file, 1, text, this.logger), 0, text.length());
		BToken token = sourceContext.newToken();
		for(;sourceContext.sliceQuotedTextUntil(token, '\n', "");) {
			int loc = token.indexOf("<-");
			if(loc > 0) {
				String name = token.substring(0,loc).trim();
				ParserContext sub = token.newParserContext(this, loc+2);
				Peg e = Peg._ParsePegExpr(sub);
				if(e != null) {
					this.setPegRule(name, e);
				}
			}
			token.StartIndex = sourceContext.consume(1);
			token.EndIndex = token.StartIndex;
		}
		this.check();
		return true;
	}

	//	private final void define(String peg) {
	//		int loc = peg.indexOf("<-");
	//		if(loc > 0) {
	//			String name = peg.substring(0, loc).trim();
	//			PegExpr e = builder.parse(peg.substring(loc+2).trim());
	//			this.setPegRule(name, e);
	//			this.pegCache = null;
	//		}
	//	}
	//
	//	public final void define(String peg) {
	//		this.define(new PegBuilder(), peg);
	//	}

	public void setPegRule(String name, Peg e) {
		if(e instanceof PegOrElseExpr) {
			this.setPegRule(name, ((PegOrElseExpr) e).secondExpr);
			this.setPegRule(name, ((PegOrElseExpr) e).firstExpr);
		}
		else {
			this.setImpl(name, e);
		}
	}

	private void setImpl(String name, Peg e) {
		String key = name;
		if(e instanceof PegLabelExpr) {
			String label = ((PegLabelExpr) e).symbol;
			//System.out.println("first name: " + name + ", " + label);
			if(label.equals(name) && e.nextExpr != null) {
				key = " " + key;  // left recursion
				e = e.nextExpr;
				e.setLeftRecursion(name);
			}
		}
		//System.out.println("'"+ key + "' <- " + e + " ## first_chars=" + e.firstChars());
		e.serialNumber = this.serialNumberCount;
		this.serialNumberCount = this.serialNumberCount + 1;
		Peg Defined = this.pegMap.GetValue(key, null);
		if(Defined != null) {
			e = new PegOrElseExpr(null, e, Defined);
		}
		this.pegMap.put(key, e);
	}

	private void initCache() {
		this.pegCache = new BunMap<Peg>(null);
		this.keywordCache = new BunMap<String>(null);
		this.firstCharCache = new BunMap<String>(null);
	}

	public final void check() {
		this.initCache();
		BArray<String> list = this.pegMap.keys();
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			Peg e = this.pegMap.GetValue(key, null);
			e.checkAll(this, 0);
			this.check(key, e);
			System.out.println(e.toPrintableString(key));
		}
		list = this.keywordCache.keys();
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			System.out.println("keyword: " + key);
		}
		list = this.firstCharCache.keys();
		for(int i = 0; i < list.size(); i++) {
			String key = list.ArrayValues[i];
			System.out.println("cache: " + key);
		}

		//		list = this.pegCache.keys();
		//		for(int i = 0; i < list.size(); i++) {
		//			String key = list.ArrayValues[i];
		//			PegExpr e = this.pegCache.GetValue(key, null);
		//			System.out.println("" + key + " <- " + e);
		//		}
	}

	private boolean contains(Peg defined, Peg e) {
		if(defined instanceof PegOrElseExpr) {
			if(this.contains(((PegOrElseExpr) defined).firstExpr, e)) {
				return true;
			}
			return this.contains(((PegOrElseExpr) defined).secondExpr, e);
		}
		return defined == e;
	}

	private void setcache(String key, Peg e) {
		Peg defined = this.pegCache.GetValue(key, null);
		if(defined != null) {
			if(this.contains(defined, e)) {
				return;
			}
			if(e.serialNumber < defined.serialNumber) {
				e = new PegOrElseExpr(null, defined, e);
			}
			else {
				e = new PegOrElseExpr(null, e, defined);
			}
		}
		//System.out.println("duplicated key='"+key+"': " + e + " #" + e.SerialNumber);
		this.pegCache.put(key, e);
	}

	private void setcache(String key, String chars, Peg e) {
		if(chars == null || chars.length() == 0) {
			this.setcache(key, e);
			System.out.println("uncached key key='"+key+"': " + e + " " + e.getClass());
		}
		else {
			for(int i = 0; i < chars.length(); i++) {
				this.setcache(key + '$' + chars.charAt(i), e);
			}
		}
	}

	private void check(String key, Peg e) {
		//		System.out.println(">> key='"+key+"': " + e);
		if(e instanceof PegOrElseExpr) {
			this.check(key, ((PegOrElseExpr) e).firstExpr);
			this.check(key, ((PegOrElseExpr) e).secondExpr);
			return;
		}
		this.setcache(key, e.firstChars(this.pegMap), e);
	}

	public final boolean hasPattern(String name) {
		return this.pegMap.GetValue(name, null) != null;
	}


	public final Peg getPattern(String name, String firstChar) {
		Peg p = this.pegCache.GetValue(name, null);
		if(p != null) {
			return this.pegMap.GetValue(name, null);
		}
		return this.pegCache.GetValue(name+"$"+firstChar, null);
	}

	public final Peg getRightPattern(String name, String firstChar) {
		return this.getPattern(" "+ name, firstChar);
	}

	public final void setNode(String name, BNode node) {
		this.nodeMap.put(name, node);
	}

	public final BNode newNode(String name, BNode parent) {
		BNode node = this.nodeMap.GetValue(name, null);
		if(node instanceof PegBunNode) {
			return node;
		}
		if(node != null) {
			BNode newnode = node.Dup(false, parent);
			assert(newnode.getClass() == node.getClass());
			//System.out.println("new node: " + name + ", " + newnode);
			newnode.ParentNode = parent;
			return newnode;
		}
		//System.out.println("undefined node: " + name);
		return new PegBunNode(parent, 0);
	}




}
