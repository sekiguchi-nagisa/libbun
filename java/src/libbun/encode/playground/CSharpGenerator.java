package libbun.encode.playground;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.GroupNode;
import libbun.ast.binary.AssignNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.binary.BunAndNode;
import libbun.ast.binary.BunBitwiseAndNode;
import libbun.ast.binary.BunBitwiseOrNode;
import libbun.ast.binary.BunBitwiseXorNode;
import libbun.ast.binary.BunDivNode;
import libbun.ast.binary.BunEqualsNode;
import libbun.ast.binary.BunGreaterThanEqualsNode;
import libbun.ast.binary.BunGreaterThanNode;
import libbun.ast.binary.BunInstanceOfNode;
import libbun.ast.binary.BunLeftShiftNode;
import libbun.ast.binary.BunLessThanEqualsNode;
import libbun.ast.binary.BunLessThanNode;
import libbun.ast.binary.BunModNode;
import libbun.ast.binary.BunMulNode;
import libbun.ast.binary.BunNotEqualsNode;
import libbun.ast.binary.BunOrNode;
import libbun.ast.binary.BunRightShiftNode;
import libbun.ast.binary.BunSubNode;
import libbun.ast.decl.BunClassNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.BunFuncNameNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetFieldNode;
import libbun.ast.expression.GetIndexNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.expression.MethodCallNode;
import libbun.ast.expression.NewObjectNode;
import libbun.ast.literal.BunArrayLiteralNode;
import libbun.ast.literal.BunBooleanNode;
import libbun.ast.literal.BunFloatNode;
import libbun.ast.literal.BunIntNode;
import libbun.ast.literal.BunMapEntryNode;
import libbun.ast.literal.BunMapLiteralNode;
import libbun.ast.literal.BunNullNode;
import libbun.ast.literal.BunStringNode;
import libbun.ast.statement.BunBreakNode;
import libbun.ast.statement.BunIfNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.statement.BunThrowNode;
import libbun.ast.statement.BunTryNode;
import libbun.ast.statement.BunWhileNode;
import libbun.ast.unary.BunCastNode;
import libbun.ast.unary.BunComplementNode;
import libbun.ast.unary.BunMinusNode;
import libbun.ast.unary.BunNotNode;
import libbun.ast.unary.BunPlusNode;
import libbun.ast.unary.UnaryOperatorNode;
import libbun.encode.LibBunSourceBuilder;
import libbun.encode.LibBunSourceGenerator;
import libbun.parser.LibBunLangInfo;
import libbun.parser.LibBunLogger;
import libbun.type.BClassType;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BField;
import libbun.util.BunMap;
import libbun.util.LibBunSystem;
import libbun.util.Var;
import libbun.util.ZenMethod;

public class CSharpGenerator extends LibBunSourceGenerator {

	@BField private final BArray<BunFunctionNode> ExportFunctionList = new BArray<BunFunctionNode>(new BunFunctionNode[4]);
	@BField private final static String NameSpaceName = "LibBunGenerated";
	@BField private final static String MainClassName = "LibBunMain";

	public CSharpGenerator() {
		super(new LibBunLangInfo("C#-5.0", "cs"));
		this.SetNativeType(BType.BooleanType, "bool");
		this.SetNativeType(BType.IntType, "long");
		this.SetNativeType(BType.FloatType, "double");
		this.SetNativeType(BType.StringType, "string");
		this.SetNativeType(BType.VarType, "dynamic");
		this.LoadInlineLibrary("inline.cs", "//");
		this.SetReservedName("this", "@this");
		this.Source.Append("namespace ", CSharpGenerator.NameSpaceName);
		this.Source.OpenIndent(" {");
	}

	@Override protected void GenerateImportLibrary(String LibName) {
		this.Header.AppendNewLine("using ", LibName, ";");
	}

	@Override @ZenMethod protected void Finish(String FileName) {
		this.Source.CloseIndent("}"); // end of namespace
		this.Source.AppendLineFeed();
	}

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		if(Node.GetListSize() == 0) {
			this.Source.Append("new ", this.GetCSharpTypeName(Node.Type, false), "()");
		}
		else {
			this.ImportLibrary("System.Collections.Generic");
			this.Source.Append("new ", this.GetCSharpTypeName(Node.Type, false));
			this.GenerateListNode("{", Node, ", ", "}");
		}
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		this.Source.Append("new ", this.GetCSharpTypeName(Node.Type, false));
		if(Node.GetListSize() > 0) {
			@Var int i = 0;
			this.Source.OpenIndent(" {");
			while(i < Node.GetListSize()) {
				@Var BunMapEntryNode Entry = Node.GetMapEntryNode(i);
				this.Source.AppendNewLine("{");
				this.GenerateExpression("", Entry.KeyNode(), ", ", Entry.ValueNode(), "},");
				i = i + 1;
			}
			this.Source.CloseIndent("}");
		}else{
			this.Source.Append("()");
		}
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
		this.Source.Append("new " + this.NameClass(Node.Type));
		this.GenerateListNode("(", Node, ", ", ")");
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		this.GenerateExpression(Node.RecvNode());
		if(Node.RecvNode().Type == BType.StringType){
			this.GenerateExpression(".Substring(", Node.IndexNode(), ", 1)");
		}else if(Node.IndexNode().Type == BType.IntType){
			this.GenerateExpression("[(int)", Node.IndexNode(), "]");
		}else{
			this.GenerateExpression("[", Node.IndexNode(), "]");
		}
	}

	@Override public void VisitFuncCallNode(FuncCallNode Node) {
		@Var BunFuncNameNode FuncNameNode = Node.FuncNameNode();
		if(FuncNameNode != null) {
			this.Source.Append((Node.FuncNameNode().FuncName));
		}
		else {
			this.GenerateExpression(Node.FunctorNode());
		}
		this.GenerateListNode("(", Node, ", ", ")");
	}

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".");
		this.Source.Append(Node.MethodName());
		this.GenerateListNode("(", Node, ", ", ")");
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		this.ImportLibrary("@Throw");
		this.Source.Append("Lib.Throw(");
		this.GenerateExpression(Node.ExprNode());
		this.Source.Append(")");
	}

	@Override public void VisitTryNode(BunTryNode Node) {
		this.Source.Append("try ");
		this.GenerateExpression(Node.TryBlockNode());
		if(Node.HasCatchBlockNode()) {
			this.ImportLibrary("@SoftwareFault");
			@Var String VarName = Node.ExceptionName();
			this.Source.AppendNewLine("catch (Exception ", VarName, ")");
			this.GenerateExpression(Node.CatchBlockNode());
		}
		if(Node.HasFinallyBlockNode()) {
			this.Source.AppendNewLine("finally ");
			this.GenerateExpression(Node.FinallyBlockNode());
		}
	}

	private String GetCSharpTypeName(BType Type, boolean Boxing) {
		this.ImportLibrary("System");
		this.ImportLibrary("System.Diagnostics");
		this.ImportLibrary("System.Collections.Generic");
		this.ImportLibrary("System.Linq");
		this.ImportLibrary("System.Text");
		if(Type.IsArrayType()) {
			this.ImportLibrary("System.Collections.Generic");
			return "List<" + this.GetCSharpTypeName(Type.GetParamType(0), true) + ">";
		}
		if(Type.IsMapType()) {
			this.ImportLibrary("System.Collections.Generic");
			return "Dictionary<string," + this.GetCSharpTypeName(Type.GetParamType(0), true) + ">";
		}
		if(Type instanceof BFuncType) {
			return this.GetFuncTypeClass((BFuncType)Type);
		}
		if(Type instanceof BClassType) {
			return this.NameClass(Type);
		}
		if(Boxing) {
			if(Type.IsIntType()) {
				return "Int64";
			}
			if(Type.IsFloatType()) {
				return "Double";
			}
			if(Type.IsBooleanType()) {
				return "Bool";
			}
		}
		return this.GetNativeTypeName(Type);
	}


	@BField private final BunMap<String> FuncNameMap = new BunMap<String>(null);

	String GetFuncTypeClass(BFuncType FuncType) {
		@Var String ClassName = this.FuncNameMap.GetOrNull(FuncType.GetUniqueName());
		if(ClassName == null){
			@Var LibBunSourceBuilder MainBuilder = this.Source;
			this.Source = new LibBunSourceBuilder(this, null);
			@Var boolean HasReturnValue = !FuncType.GetReturnType().equals(BType.VoidType);
			if(HasReturnValue){
				this.Source.Append("Func");
			}else{
				this.Source.Append("Action");
			}
			this.Source.Append("<");
			@Var int i = 0;
			while(i < FuncType.GetFuncParamSize()) {
				if(i > 0) {
					this.Source.Append(", ");
				}
				this.GenerateTypeName(FuncType.GetFuncParamType(i));
				i = i + 1;
			}
			if(HasReturnValue){
				this.Source.Append(", ");
				this.GenerateTypeName(FuncType.GetReturnType());
			}
			this.Source.Append(">");
			ClassName = this.Source.toString();
			this.Source = MainBuilder;
			this.FuncNameMap.put(FuncType.GetUniqueName(), ClassName);
		}
		return ClassName;
	}

	@Override protected void GenerateTypeName(BType Type) {
		this.GetNativeTypeName(Type.GetRealType());
		if(Type instanceof BFuncType) {
			this.Source.Append(this.GetFuncTypeClass((BFuncType)Type));
		}
		else {
			this.Source.Append(this.GetCSharpTypeName(Type.GetRealType(), false));
		}
	}

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		@Var boolean IsLambda = (Node.FuncName() == null);
		if(IsLambda){
			this.GenerateLambdaFunction(Node);
			return;
		}
		if(!Node.Type.IsVoidType()) {
			@Var String FuncName = Node.GetUniqueName(this);
			this.Source = this.InsertNewSourceBuilder();
			FuncName = this.GenerateFunctionAsClass(FuncName, Node);
			this.Source.AppendLineFeed();
			this.Source = this.Source.Pop();
			this.Source.Append(FuncName);
		}
		else {
			this.GenerateFunctionAsClass(Node.FuncName(), Node);
			if(Node.IsExport) {
				if(Node.FuncName().equals("main")) {
					this.ImportLibrary("@main");
				}
				else {
					this.ExportFunctionList.add(Node);
				}
			}
		}
	}

	private void GenerateLambdaFunction(BunFunctionNode Node){
		this.GenerateListNode("(", Node, ", ", ") => ");
		if(Node.BlockNode().GetListSize() == 1){
			@Var BNode FirstNode = Node.BlockNode().GetListAt(0);
			if(FirstNode instanceof BunReturnNode){
				this.GenerateExpression(((BunReturnNode)FirstNode).ExprNode());
				return;
			}
		}
		this.GenerateExpression(Node.BlockNode());
	}

	private String GenerateFunctionAsClass(String FuncName, BunFunctionNode Node) {
		@Var BunLetVarNode FirstParam = Node.GetListSize() == 0 ? null : (BunLetVarNode)Node.GetListAt(0);
		@Var boolean IsInstanceMethod = FirstParam != null && FirstParam.GetGivenName().equals("this");

		this.GenerateClass("public static", CSharpGenerator.MainClassName, Node.GetFuncType());
		this.Source.OpenIndent(" { ");
		this.Source.AppendNewLine("public static ");
		this.GenerateTypeName(Node.ReturnType());
		this.Source.Append(" ");
		this.Source.Append(FuncName);
		if(IsInstanceMethod){
			this.GenerateListNode("(this ", Node, ", ", ")");
		}else{
			this.GenerateListNode("(", Node, ", ", ")");
		}

		this.GenerateExpression(Node.BlockNode());

		this.Source.CloseIndent("}");
		return FuncName;
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.GenerateExpression(Node.AST[BunInstanceOfNode._Left]);
		this.Source.Append(" is ");
		this.GenerateTypeName(Node.TargetType());
	}

	private void GenerateClass(String Qualifier, String ClassName, BType SuperType) {
		if(Qualifier != null && Qualifier.length() > 0) {
			this.Source.AppendNewLine(Qualifier);
			this.Source.AppendWhiteSpace("partial class ", ClassName);
		}
		else {
			this.Source.AppendNewLine("partial class ", ClassName);
		}
		if(!SuperType.Equals(BClassType._ObjectType) && !SuperType.IsFuncType()) {
			this.Source.Append(" : ");
			this.GenerateTypeName(SuperType);
		}
	}

	private void GenerateClassField(String Qualifier, BType FieldType, String FieldName, String Value) {
		if(Qualifier.length() > 1){
			this.Source.AppendNewLine(Qualifier);
			this.Source.Append("public ");
		}else{
			this.Source.AppendNewLine("public ");
		}
		this.GenerateTypeName(FieldType);
		this.Source.Append(" ", FieldName);
		if(Value != null) {
			this.Source.Append(" = ", Value, ";");
		}
	}

	@Override public void VisitClassNode(BunClassNode Node) {
		@Var BType SuperType = Node.ClassType.GetSuperType();
		@Var String ClassName = this.NameClass(Node.ClassType);
		this.GenerateClass("public", ClassName, SuperType);
		this.Source.OpenIndent(" {");
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			this.GenerateClassField("", FieldNode.DeclType(), FieldNode.GetGivenName(), null);
			this.Source.Append(";");
			i = i + 1;
		}
		this.Source.AppendNewLine();

		i = 0;

		this.Source.AppendNewLine("public ", this.NameClass(Node.ClassType), "()");
		this.Source.OpenIndent(" {");
		//this.CurrentBuilder.AppendNewLine("super();");
		while (i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			this.Source.AppendNewLine("this.", FieldNode.GetGivenName(), "=");
			this.GenerateExpression(FieldNode.InitValueNode());
			this.Source.Append(";");
			i = i + 1;
		}
		i = 0;

		this.Source.CloseIndent("}"); /* end of constructor*/
		this.Source.CloseIndent("}"); /* end of class */
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
		LibBunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
		this.ImportLibrary("@Error");
		this.Source.Append("Lib.Error(");
		this.Source.Append(LibBunSystem._QuoteString(Node.ErrorMessage));
		this.Source.Append(")");
	}

	@Override
	protected void GenerateStatementEnd(BNode Node) {
		if(!this.Source.EndsWith(';') && !this.Source.EndsWith('}')){
			this.Source.Append(";");
		}
	}

	@Override
	public void VisitNullNode(BunNullNode Node) {
		this.Source.Append("null");
	}

	@Override
	public void VisitBooleanNode(BunBooleanNode Node) {
		this.Source.Append(Node.BooleanValue ? "true" : "false");
	}

	@Override
	public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(""+Node.IntValue);
	}

	@Override
	public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(""+Node.FloatValue);
	}

	@Override
	public void VisitStringNode(BunStringNode Node) {
		this.Source.AppendQuotedText(Node.StringValue);
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.Source.Append("!(");
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(")");
	}

	@Override public void VisitPlusNode(BunPlusNode Node) {
		this.Source.Append("+");
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitMinusNode(BunMinusNode Node) {
		this.Source.Append("-");
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitComplementNode(BunComplementNode Node) {
		this.Source.Append("~");
		this.GenerateExpression(Node.RecvNode());
	}

	private void GenerateBinaryOperatorExpression(BinaryOperatorNode Node, String Operator) {
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" ", Operator, " ");
		this.GenerateExpression(Node.RightNode());
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "&&");
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "||");
	}

	@Override public void VisitAddNode(BunAddNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "+");
	}

	@Override public void VisitSubNode(BunSubNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "-");
	}

	@Override public void VisitMulNode(BunMulNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "*");
	}

	@Override public void VisitDivNode(BunDivNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "/");
	}

	@Override public void VisitModNode(BunModNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "%");
	}

	@Override public void VisitLeftShiftNode(BunLeftShiftNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "<<");
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode Node) {
		this.GenerateBinaryOperatorExpression(Node, ">>");
	}

	@Override public void VisitBitwiseAndNode(BunBitwiseAndNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "&");
	}

	@Override public void VisitBitwiseOrNode(BunBitwiseOrNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "|");
	}

	@Override public void VisitBitwiseXorNode(BunBitwiseXorNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "^");
	}

	@Override public void VisitEqualsNode(BunEqualsNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "==");
	}

	@Override public void VisitNotEqualsNode(BunNotEqualsNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "!=");
	}

	@Override public void VisitLessThanNode(BunLessThanNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "<");
	}

	@Override public void VisitLessThanEqualsNode(BunLessThanEqualsNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "<=");
	}

	@Override public void VisitGreaterThanNode(BunGreaterThanNode Node) {
		this.GenerateBinaryOperatorExpression(Node, ">");
	}

	@Override public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode Node) {
		this.GenerateBinaryOperatorExpression(Node, ">=");
	}

	@Override public void VisitGroupNode(GroupNode Node) {
		this.GenerateExpression("(", Node.ExprNode(), ")");
	}


	private void VisitStmtList(BunBlockNode BlockNode) {
		@Var int i = 0;
		while (i < BlockNode.GetListSize()) {
			@Var BNode SubNode = BlockNode.GetListAt(i);
			this.GenerateStatement(SubNode);
			i = i + 1;
		}
	}

	@Override public void VisitBlockNode(BunBlockNode Node) {
		this.Source.OpenIndent("{");
		this.VisitStmtList(Node);
		this.Source.CloseIndent("}");
	}

	@Override public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			if(Node.Type.Equals(BType.VarType) && ((BunFunctionNode)Node.ParentNode).FuncName() == null){
				this.Source.Append(Node.GetUniqueName(this));
			}else{
				this.Source.Append(this.GetCSharpTypeName(Node.DeclType(), false), " ", Node.GetUniqueName(this));
			}
		}
		else if(Node.IsTopLevel()) {
			this.GenerateClass("public static", CSharpGenerator.MainClassName, BClassType._ObjectType);
			this.Source.OpenIndent("{");
			this.Source.AppendNewLine("public static ");
			this.GenerateTypeName(Node.GetAstType(BunLetVarNode._InitValue));
			this.Source.Append(" ", Node.GetUniqueName(this), " = ");
			this.GenerateExpression(Node.InitValueNode());
			this.Source.Append(";");
			this.Source.CloseIndent("}");
		}
		else {
			this.Source.Append(this.GetCSharpTypeName(Node.DeclType(), false), " ", Node.GetUniqueName(this));
			this.GenerateExpression(" = ", Node.InitValueNode(), "");
		}
	}

	@Override
	public void VisitVarBlockNode(BunVarBlockNode Node) {
		@Var BunLetVarNode VarNode = Node.VarDeclNode();
		this.Source.AppendNewLine(this.GetCSharpTypeName(VarNode.DeclType(), false), " ", VarNode.GetUniqueName(this));
		this.GenerateExpression(" = ", VarNode.InitValueNode(), ";");
		this.VisitStmtList(Node);
	}

	@Override
	public void VisitIfNode(BunIfNode Node) {
		this.GenerateExpression("if (", Node.CondNode(), ")");
		this.GenerateExpression(Node.ThenNode());
		if (Node.HasElseNode()) {
			this.Source.AppendNewLine("else ");
			this.GenerateExpression(Node.ElseNode());
		}
	}

	@Override
	public void VisitReturnNode(BunReturnNode Node) {
		this.Source.Append("return");
		if (Node.HasReturnExpr()) {
			this.Source.Append(" ");
			this.GenerateExpression(Node.ExprNode());
		}
	}

	@Override
	public void VisitWhileNode(BunWhileNode Node) {
		this.GenerateExpression("while (", Node.CondNode(), ")");
		this.GenerateExpression(Node.BlockNode());
	}

	@Override
	public void VisitBreakNode(BunBreakNode Node) {
		this.Source.Append("break");
	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		@Var BNode ResolvedNode = Node.ResolvedNode;
		if(ResolvedNode == null && !this.LangInfo.AllowUndefinedSymbol) {
			LibBunLogger._LogError(Node.SourceToken, "undefined symbol: " + Node.GivenName);
		}
		this.Source.Append(Node.GetUniqueName(this));
	}

	@Override public void VisitGetFieldNode(GetFieldNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.GetName());
	}

	@Override
	public void VisitUnaryNode(UnaryOperatorNode Node) {
		// TODO Auto-generated method stub
	}

	@Override
	public void VisitCastNode(BunCastNode Node) {
		// TODO Auto-generated method stub
	}

	@Override
	public void VisitBinaryNode(BinaryOperatorNode Node) {
		// TODO Auto-generated method stub
	}

	@Override
	public void VisitAssignNode(AssignNode Node) {
		// TODO Auto-generated method stub

	}


}
