package libbun.parser.peg;

import libbun.ast.BNode;
import libbun.parser.BToken;
import libbun.util.BFunction;

public abstract class PegFunction extends BFunction {
	public PegFunction(int TypeId, String Name) {
		super(TypeId, Name);
	}
	protected PegFunction() {
		super(0, null);
	}
	public abstract BNode Invoke(BNode parentNode, ParserContext sourceContext);

	public PegNode Invoke2(PegNode parentNode, ParserContext sourceContext) {
		BToken token = sourceContext.newToken();
		if(sourceContext.sliceNumber(token)) {
			return sourceContext.newPegNode(null, token.StartIndex, token.EndIndex);
		}
		return null;
	}
}

