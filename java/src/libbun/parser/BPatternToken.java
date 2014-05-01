package libbun.parser;

import libbun.util.BField;

public class BPatternToken extends BToken {
	@BField public LibBunSyntax	PresetPattern;
	public BPatternToken(ParserSource Source, int StartIndex, int EndIndex, LibBunSyntax	PresetPattern) {
		super(Source, StartIndex, EndIndex);
		this.PresetPattern = PresetPattern;
	}

}
