package libbun.parser.peg;

import libbun.parser.BToken;
import libbun.util.BFunction;

public abstract class PegFunction extends BFunction {
	public PegFunction(int TypeId, String Name) {
		super(TypeId, Name);
	}
	protected PegFunction() {
		super(0, null);
	}

	public PegObject Invoke(PegObject parentNode, ParserContext sourceContext) {
		BToken token = sourceContext.newToken();
		if(sourceContext.sliceNumber(token)) {
			return sourceContext.newPegNode(null, token.StartIndex, token.EndIndex);
		}
		return null;
	}
}

