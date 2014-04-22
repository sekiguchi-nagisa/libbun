package libbun.encode;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.LocalDefinedNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.TopLevelNode;
import libbun.ast.expression.BunFormNode;
import libbun.ast.literal.BunAsmNode;
import libbun.ast.literal.LiteralNode;
import libbun.ast.unary.BunCastNode;
import libbun.parser.LibBunLangInfo;
import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BField;
import libbun.util.LibBunSystem;
import libbun.util.Nullable;
import libbun.util.Var;
import libbun.util.ZenMethod;

public abstract class LibBunSourceGenerator extends LibBunGenerator {
	@BField protected final BArray<LibBunSourceBuilder> BuilderList = new BArray<LibBunSourceBuilder>(new LibBunSourceBuilder[4]);
	@BField protected LibBunSourceBuilder Header;
	@BField protected LibBunSourceBuilder Source;
	@BField protected String LineFeed = "\n";
	@BField protected String Tab = "   ";
	@BField protected boolean HasMainFunction = false;

	public LibBunSourceGenerator(LibBunLangInfo LangInfo) {
		super(LangInfo);
		this.InitBuilderList();
	}

	@ZenMethod protected void InitBuilderList() {
		this.BuilderList.clear(0);
		this.Header = this.AppendNewSourceBuilder();
		this.Source = this.AppendNewSourceBuilder();
	}

	protected final LibBunSourceBuilder AppendNewSourceBuilder() {
		@Var LibBunSourceBuilder Builder = new LibBunSourceBuilder(this, this.Source);
		this.BuilderList.add(Builder);
		return Builder;
	}

	protected final LibBunSourceBuilder InsertNewSourceBuilder() {
		@Var LibBunSourceBuilder Builder = new LibBunSourceBuilder(this, this.Source);
		@Var int i = 0;
		while(i < this.BuilderList.size()) {
			if(this.BuilderList.ArrayValues[i] == this.Source) {
				this.BuilderList.add(i, Builder);
				return Builder;
			}
			i = i + 1;
		}
		this.BuilderList.add(Builder);
		return Builder;
	}

	protected abstract void GenerateImportLibrary(String LibName);

	protected final void LoadInlineLibrary(String FileName, String Delim) {
		@Var String Path = this.LangInfo.GetLibPath2(FileName);
		LibBunSystem._LoadInlineLibrary(Path, this.SymbolMap, Delim);
	}

	public final void ImportLibrary(@Nullable String LibNames) {
		if(LibNames != null) {
			@Var String LibName = LibNames;
			@Var int loc = LibNames.indexOf(";");
			if(loc > 0) {
				LibName = LibNames.substring(0, loc);
			}
			@Var String Imported = this.ImportedLibraryMap.GetOrNull(LibName);
			if(Imported == null) {
				if(LibName.startsWith("@")) {
					this.ImportLibrary(this.SymbolMap.GetValue(LibName+";", null));
					this.Header.AppendNewLine();
					this.Header.AppendCode(this.SymbolMap.GetValue(LibName, LibName));
				}
				else {
					this.GenerateImportLibrary(LibName);
				}
				this.ImportedLibraryMap.put(LibName, LibName);
			}
			if(loc > 0) {
				this.ImportLibrary(LibNames.substring(loc+1));
			}
		}
	}

	@ZenMethod protected void Finish(String FileName) {
	}

	@Override public final void WriteTo(@Nullable String FileName) {
		this.Finish(FileName);
		if(this.HasMainFunction) {
			this.Source.AppendNewLine(this.SymbolMap.GetValue("@main", "@main"));
		}
		this.Logger.OutputErrorsToStdErr();
		LibBunSystem._WriteTo(this.LangInfo.NameOutputFile(FileName), this.BuilderList);
		this.InitBuilderList();
	}

	@Override public final String GetSourceText() {
		this.Finish(null);
		@Var LibBunSourceBuilder sb = new LibBunSourceBuilder(this, null);
		@Var int i = 0;
		while(i < this.BuilderList.size()) {
			@Var LibBunSourceBuilder Builder = this.BuilderList.ArrayValues[i];
			sb.Append(Builder.toString());
			Builder.Clear();
			sb.Append(this.LineFeed);
			i = i + 1;
		}
		this.InitBuilderList();
		return LibBunSystem._SourceBuilderToString(sb);
	}

	@Override public void Perform() {
		this.Logger.OutputErrorsToStdErr();
		LibBunSystem._PrintLine("---");
		LibBunSystem._PrintLine(this.GetSourceText());
		this.InitBuilderList();
	}



	// Generator
	protected void GenerateTypeName(BType Type) {
		this.Source.Append(this.GetNativeTypeName(Type.GetRealType()));
	}

	protected final void GenerateExpression(String Pre, BNode Node, String Post) {
		if(Pre != null && Pre.length() > 0) {
			this.Source.Append(Pre);
		}
		this.GenerateExpression(Node);
		if(Post != null && Post.length() > 0) {
			this.Source.Append(Post);
		}
	}

	protected final void GenerateExpression(String Pre, BNode Node, String Delim, BNode Node2, String Post) {
		if(Pre != null && Pre.length() > 0) {
			this.Source.Append(Pre);
		}
		this.GenerateExpression(Node);
		if(Delim != null && Delim.length() > 0) {
			this.Source.Append(Delim);
		}
		this.GenerateExpression(Node2);
		if(Post != null && Post.length() > 0) {
			this.Source.Append(Post);
		}
	}

	protected final void GenerateExpression(String Text1, BNode Node1, String Text2, BNode Node2, String Text3, BNode Node3, String Text4) {
		this.Source.Append(Text1);
		this.GenerateExpression(Node1);
		this.Source.Append(Text2);
		this.GenerateExpression(Node2);
		this.Source.Append(Text3);
		this.GenerateExpression(Node3);
		this.Source.Append(Text4);
	}

	protected abstract void GenerateStatementEnd(BNode Node);

	@Override protected void GenerateStatement(BNode Node) {
		this.Source.AppendNewLine();
		if(Node instanceof BunCastNode && Node.Type == BType.VoidType) {
			Node = Node.AST[BunCastNode._Expr];
		}
		Node.Accept(this);
		this.GenerateStatementEnd(Node);
	}

	@Override public final void VisitAsmNode(BunAsmNode Node) {
		this.ImportLibrary(Node.RequiredLibrary);
		this.Source.Append(Node.GetFormText());
	}

	@Override public void VisitFormNode(BunFormNode Node) {
		@Var String FormText = Node.GetFormText();
		//		@Var BFuncType FuncType = Node.GetFuncType();
		@Var int fromIndex = 0;
		@Var int BeginNum = FormText.indexOf("$[", fromIndex);
		while(BeginNum != -1) {
			@Var int EndNum = FormText.indexOf("]", BeginNum + 2);
			if(EndNum == -1) {
				break;
			}
			this.Source.Append(FormText.substring(fromIndex, BeginNum));
			@Var int Index = (int)LibBunSystem._ParseInt(FormText.substring(BeginNum+2, EndNum));
			if(Node.AST[Index] != null) {
				//this.GenerateCode(FuncType.GetFuncParamType(Index), Node.AST[Index]);
				this.GenerateExpression(Node.AST[Index]);
			}
			fromIndex = EndNum + 1;
			BeginNum = FormText.indexOf("$[", fromIndex);
		}
		this.Source.Append(FormText.substring(fromIndex));
		if(Node.FormFunc.RequiredLibrary != null) {
			this.ImportLibrary(Node.FormFunc.RequiredLibrary);
		}
	}

	@Override public void VisitSyntaxSugarNode(SyntaxSugarNode Node) {
		@Var DesugarNode DeNode = Node.PerformDesugar(this.TypeChecker);
		this.GenerateExpression(DeNode.AST[0]);
		if(DeNode.GetAstSize() > 1) {
			@Var int i = 1;
			while(i < DeNode.GetAstSize()) {
				this.GenerateStatement(DeNode.AST[i]);
				i = i + 1;
			}
		}
	}

	@Override public final void VisitLiteralNode(LiteralNode Node) {
	}


	@Override public final void VisitTopLevelNode(TopLevelNode Node) {
	}

	@Override public void VisitLocalDefinedNode(LocalDefinedNode Node) {
	}


	// Generator

	protected void GenerateListNode(String OpenToken, AbstractListNode VargNode, String CommaToken, String CloseToken) {
		this.Source.Append(OpenToken);
		@Var int i = 0;
		while(i < VargNode.GetListSize()) {
			@Var BNode ParamNode = VargNode.GetListAt(i);
			if (i > 0) {
				this.Source.Append(CommaToken);
			}
			this.GenerateExpression(ParamNode);
			i = i + 1;
		}
		this.Source.Append(CloseToken);
	}

	protected void GenerateWrapperCall(String OpenToken, BunFunctionNode FuncNode, String CommaToken, String CloseToken) {
		this.Source.Append(OpenToken);
		@Var int i = 0;
		while(i < FuncNode.GetListSize()) {
			@Var BunLetVarNode ParamNode = FuncNode.GetParamNode(i);
			if (i > 0) {
				this.Source.Append(CommaToken);
			}
			this.Source.Append(ParamNode.GetUniqueName(this));
			i = i + 1;
		}
		this.Source.Append(CloseToken);
	}



}
