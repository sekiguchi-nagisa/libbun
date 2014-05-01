package libbun.parser.peg;

import libbun.ast.BNode;
import libbun.util.BFunction;

public abstract class PegFunction extends BFunction {
	public PegFunction(int TypeId, String Name) {
		super(TypeId, Name);
	}
	protected PegFunction() {
		super(0, null);
	}
	public abstract BNode Invoke(BNode parentNode, ParserContext sourceContext);
}

