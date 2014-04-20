package libbun.encode.jvm;

import libbun.ast.BNode;
import libbun.ast.decl.TopLevelNode;
import libbun.parser.LibBunLogger;
import libbun.parser.LibBunGamma;
import libbun.type.BType;
import libbun.util.Var;

public class JavaImportNode extends TopLevelNode {
	public final static int _Path  = 0;

	public JavaImportNode(BNode ParentNode) {
		super(ParentNode, 1);
	}

	//	private String ParsePath(String Path) {
	//		@Var int loc = Path.lastIndexOf('.');
	//		if(loc != -1) {
	//			return Path.substring(0, loc);
	//		}
	//		return Path;
	//	}

	private String ParseSymbol(String Path) {
		@Var int loc = Path.lastIndexOf('.');
		if(loc != -1) {
			return Path.substring(loc+1);
		}
		return Path;
	}

	@Override public void Perform(LibBunGamma Gamma) {
		@Var String ResourcePath = this.AST[JavaImportNode._Path].SourceToken.GetTextAsName();
		try {
			Class<?> jClass = Class.forName(ResourcePath);
			BType Type = JavaTypeTable.GetBunType(jClass);
			String Alias = this.ParseSymbol(ResourcePath);

			Gamma.SetTypeName(Alias, Type, this.SourceToken);
		} catch (ClassNotFoundException e) {
			LibBunLogger._LogError(this.GetAstToken(JavaImportNode._Path), "unfound resource: "+ ResourcePath);
		}
	}
}
