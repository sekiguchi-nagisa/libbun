package libbun.encode.release;

public class PythonGenerator extends libbun.encode.playground.PythonGenerator {
	public PythonGenerator() {
		super();
		this.Source.AppendNewLine("import sys, codecs");
		this.Source.AppendNewLine("sys.stdout = codecs.getwriter('utf-8')(sys.stdout)");
	}
}
