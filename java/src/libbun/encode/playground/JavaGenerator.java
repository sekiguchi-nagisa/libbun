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
import libbun.encode.LibBunSourceGenerator;
import libbun.parser.LibBunLangInfo;
import libbun.parser.LibBunLogger;
import libbun.type.BClassField;
import libbun.type.BClassType;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BField;
import libbun.util.BunMap;
import libbun.util.LibBunSystem;
import libbun.util.Var;
import libbun.util.ZenMethod;

public class JavaGenerator extends LibBunSourceGenerator {

	@BField private BunFunctionNode MainFuncNode = null;
	@BField private final BArray<BunFunctionNode> ExportFunctionList = new BArray<BunFunctionNode>(new BunFunctionNode[4]);

	public JavaGenerator() {
		super(new LibBunLangInfo("Java-1.6", "java"));
		//		this.IntLiteralSuffix="";
		//		this.TopType = "Object";
		this.SetNativeType(BType.BooleanType, "boolean");
		this.SetNativeType(BType.IntType, "int");  // for beautiful code
		this.SetNativeType(BType.FloatType, "double");
		this.SetNativeType(BType.StringType, "String");
		this.SetNativeType(BType.VarType, "Object");  // for safety

		this.SetReservedName("this", "self");

		this.LoadInlineLibrary("common.java", "//");
		//this.HeaderBuilder.AppendNewLine("import zen.util.*;");
		this.Source.AppendNewLine("/* end of header */", this.LineFeed);
	}

	@Override protected void GenerateImportLibrary(String LibName) {
		this.Header.AppendNewLine("import ", LibName, ";");
	}

	@Override @ZenMethod protected void Finish(String FileName) {
		if(FileName == null) {
			FileName = "ZenMain";
		}
		else {
			@Var int loc = FileName.lastIndexOf('/');
			if(loc != -1) {
				FileName = FileName.substring(loc+1);
			}
		}
		this.GenerateClass("public final", FileName, BClassType._ObjectType);
		this.Source.OpenIndent(" {");
		if(this.MainFuncNode != null) {
			this.Source.AppendNewLine("public final static void main(String[] a)");
			this.Source.OpenIndent(" {");
			this.Source.AppendNewLine(this.NameFunctionClass(this.MainFuncNode.FuncName(), BType.VoidType, 0), ".f();");
			this.Source.CloseIndent("}");
		}
		this.Source.CloseIndent("}");
		this.Source.AppendLineFeed();
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		this.Source.Append("null");
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		if (Node.BooleanValue) {
			this.Source.Append("true");
		} else {
			this.Source.Append("false");
		}
	}

	@Override public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(String.valueOf(Node.IntValue));
	}

	@Override public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(String.valueOf(Node.FloatValue));
	}

	@Override public void VisitStringNode(BunStringNode Node) {
		this.Source.AppendQuotedText(Node.StringValue);
	}

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		if(Node.GetListSize() == 0) {
			this.Source.Append("new ", this.GetJavaTypeName(Node.Type, false), "()");
		}
		else {
			@Var BType ParamType = Node.Type.GetParamType(0);
			this.ImportLibrary("java.util.Arrays");
			this.Source.Append("new ", this.GetJavaTypeName(Node.Type, false), "(");
			this.Source.Append("Arrays.asList(new ", this.GetJavaTypeName(ParamType, true), "[]");
			this.GenerateListNode("{", Node, ", ", "}))");
		}
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		this.Source.Append("new ", this.GetJavaTypeName(Node.Type, false), "()");
		if(Node.GetListSize() > 0) {
			@Var int i = 0;
			this.Source.OpenIndent(" {{");
			while(i < Node.GetListSize()) {
				@Var BunMapEntryNode Entry = Node.GetMapEntryNode(i);
				this.Source.AppendNewLine("put");
				this.GenerateExpression("(", Entry.KeyNode(), ", ", Entry.ValueNode(), ");");
				i = i + 1;
			}
			this.Source.CloseIndent("}}");
		}
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
		this.Source.Append("new " + this.NameClass(Node.Type));
		this.GenerateListNode("(", Node, ", ", ")");
	}

	@Override public void VisitGroupNode(GroupNode Node) {
		this.GenerateExpression("(", Node.ExprNode(), ")");
	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		this.Source.Append(Node.GetUniqueName(this));
	}

	@Override public void VisitGetFieldNode(GetFieldNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.GetName());
	}

	@Override public void VisitAssignNode(AssignNode Node) {
		@Var BNode LeftNode = Node.LeftNode();
		if(LeftNode instanceof GetIndexNode) {
			this.GenerateAssignIndex((GetIndexNode)LeftNode, Node.RightNode());
		}
		else {
			this.GenerateExpression(LeftNode);
			this.Source.Append(" = ");
			this.GenerateExpression(Node.RightNode());
		}
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		@Var BType RecvType = Node.GetAstType(GetIndexNode._Recv);
		if(RecvType.IsStringType()) {
			this.GenerateExpression("String.valueOf((", Node.RecvNode(), ")");
			this.GenerateExpression(".charAt(", Node.IndexNode(), "))");
		}
		else if(RecvType.IsMapType()) {
			this.GenerateExpression("LibMap.Get(", Node.RecvNode(), ", ", Node.IndexNode(), ")");
		}
		else {
			this.GenerateExpression(Node.RecvNode());
			this.GenerateExpression(".get(", Node.IndexNode(), ")");
		}
	}

	private void GenerateAssignIndex(GetIndexNode Node, BNode ExprNode) {
		@Var BType RecvType = Node.GetAstType(GetIndexNode._Recv);
		this.GenerateExpression(Node.RecvNode());
		if(RecvType.IsMapType()) {
			this.Source.Append(".put(");
		}
		else {
			this.Source.Append(".set(");
		}
		this.GenerateExpression(Node.IndexNode());
		this.Source.Append(", ");
		this.GenerateExpression(ExprNode);
		this.Source.Append(")");
	}

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(".", Node.MethodName());
		this.GenerateListNode("(", Node, ",", ")");
	}


	@Override public void VisitFuncCallNode(FuncCallNode Node) {
		@Var BunFuncNameNode FuncNameNode = Node.FuncNameNode();
		if(FuncNameNode != null) {
			this.Source.Append(this.NameFunctionClass(FuncNameNode.FuncName, FuncNameNode.RecvType, FuncNameNode.FuncParamSize), ".f");
		}
		else {
			this.GenerateExpression(Node.FunctorNode());
			this.Source.Append(".Invoke");
		}
		this.GenerateListNode("(", Node, ", ", ")");
	}

	@Override public void VisitUnaryNode(UnaryOperatorNode Node) {
		this.Source.Append(Node.GetOperator());
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitPlusNode(BunPlusNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitMinusNode(BunMinusNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitComplementNode(BunComplementNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitCastNode(BunCastNode Node) {
		if(Node.Type.IsVoidType()) {
			this.GenerateExpression(Node.ExprNode());
		}
		else {
			this.Source.Append("(");
			this.GenerateTypeName(Node.Type);
			this.Source.Append(")");
			this.GenerateExpression(Node.ExprNode());
		}
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" instanceof ");
		this.GenerateTypeName(Node.TargetType());
	}

	@Override public void VisitBinaryNode(BinaryOperatorNode Node) {
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		this.GenerateExpression(Node.LeftNode());
		this.Source.AppendWhiteSpace(Node.GetOperator(), " ");
		this.GenerateExpression(Node.RightNode());
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
	}

	@Override public void VisitAddNode(BunAddNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitSubNode(BunSubNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitMulNode(BunMulNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitDivNode(BunDivNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitModNode(BunModNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitLeftShiftNode(BunLeftShiftNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitBitwiseAndNode(BunBitwiseAndNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitBitwiseOrNode(BunBitwiseOrNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitBitwiseXorNode(BunBitwiseXorNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitEqualsNode(BunEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitNotEqualsNode(BunNotEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitLessThanNode(BunLessThanNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitLessThanEqualsNode(BunLessThanEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitGreaterThanNode(BunGreaterThanNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.VisitBinaryNode(Node);
	}


	@Override
	protected void GenerateStatementEnd(BNode Node) {
		if(Node instanceof BunIfNode || Node instanceof BunWhileNode || Node instanceof BunTryNode || Node instanceof BunFunctionNode || Node instanceof BunClassNode) {
			return;
		}
		if(!this.Source.EndsWith(';')) {
			this.Source.Append(";");
		}
	}

	protected void GenerateStmtListNode(BunBlockNode Node) {
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BNode SubNode = Node.GetListAt(i);
			this.GenerateStatement(SubNode);
			i = i + 1;
		}
	}

	@Override public void VisitBlockNode(BunBlockNode Node) {
		this.Source.AppendWhiteSpace();
		this.Source.OpenIndent("{");
		this.GenerateStmtListNode(Node);
		this.Source.CloseIndent("}");
	}

	@Override public void VisitVarBlockNode(BunVarBlockNode Node) {
		this.VisitVarDeclNode(Node.VarDeclNode());
		this.GenerateStmtListNode(Node);
	}

	@Override public void VisitIfNode(BunIfNode Node) {
		this.GenerateExpression("if (", Node.CondNode(), ")");
		this.GenerateExpression(Node.ThenNode());
		if (Node.HasElseNode()) {
			this.Source.AppendNewLine("else ");
			this.GenerateExpression(Node.ElseNode());
		}
	}

	@Override public void VisitReturnNode(BunReturnNode Node) {
		this.Source.Append("return");
		if (Node.HasReturnExpr()) {
			this.Source.Append(" ");
			this.GenerateExpression(Node.ExprNode());
		}
	}

	@Override public void VisitWhileNode(BunWhileNode Node) {
		this.GenerateExpression("while (", Node.CondNode(), ")");
		if(Node.HasNextNode()) {
			Node.BlockNode().Append(Node.NextNode());
		}
		this.GenerateExpression(Node.BlockNode());
	}

	@Override public void VisitBreakNode(BunBreakNode Node) {
		this.Source.Append("break");
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		this.Source.Append("throw ");
		this.GenerateExpression("new RuntimeException((", Node.ExprNode(), ").toString())");
	}

	@Override public void VisitTryNode(BunTryNode Node) {
		this.Source.Append("try ");
		this.GenerateExpression(Node.TryBlockNode());
		if(Node.HasCatchBlockNode()) {
			@Var String VarName = this.NameUniqueSymbol("e");
			this.Source.AppendNewLine("catch (Exception ", VarName, ")");
			this.Source.OpenIndent(" {");
			this.Source.AppendNewLine("Fault ", Node.ExceptionName(), " = LibCatch.");
			this.Source.Append("f(", VarName, ");");
			this.GenerateStmtListNode(Node.CatchBlockNode());
			this.Source.CloseIndent("}");
			this.ImportLibrary("@catch");
		}
		if(Node.HasFinallyBlockNode()) {
			this.Source.AppendNewLine("finally ");
			this.GenerateExpression(Node.FinallyBlockNode());
		}
	}

	/**********************************************************************/

	@Override protected void GenerateExpression(BNode Node) {
		//this.Source.Append("/*untyped*/");
		Node.Accept(this);
	}

	//	@Override public void VisitCastNode(ZCastNode Node) {
	//		this.CurrentBuilder.Append("(");
	//		this.VisitType(Node.Type);
	//		this.CurrentBuilder.Append(")");
	//		this.GenerateSurroundCode(Node.ExprNode());
	//	}

	private String GetJavaTypeName(BType Type, boolean Boxing) {
		if(Type.IsArrayType()) {
			this.ImportLibrary("java.util.ArrayList");
			return "ArrayList<" + this.GetJavaTypeName(Type.GetParamType(0), true) + ">";
		}
		if(Type.IsMapType()) {
			this.ImportLibrary("java.util.HashMap");
			return "HashMap<String," + this.GetJavaTypeName(Type.GetParamType(0), true) + ">";
		}
		if(Type instanceof BFuncType) {
			return this.GetFuncTypeClass((BFuncType)Type);
		}
		if(Type instanceof BClassType) {
			return this.NameClass(Type);
		}
		if(Boxing) {
			if(Type.IsIntType()) {
				return "Integer";
			}
			if(Type.IsFloatType()) {
				return "Double";
			}
			if(Type.IsBooleanType()) {
				return "Boolean";
			}
		}
		return this.GetNativeTypeName(Type);
	}

	@BField private final BunMap<String> FuncNameMap = new BunMap<String>(null);

	String GetFuncTypeClass(BFuncType FuncType) {
		@Var String ClassName = this.FuncNameMap.GetOrNull(FuncType.GetUniqueName());
		if(ClassName == null) {
			ClassName = this.NameType(FuncType);
			this.FuncNameMap.put(FuncType.GetUniqueName(), ClassName);
			this.Source = this.InsertNewSourceBuilder();
			this.Source.AppendNewLine("abstract class ", ClassName, "");
			this.Source.OpenIndent(" {");
			this.Source.AppendNewLine("abstract ");
			this.GenerateTypeName(FuncType.GetReturnType());
			this.Source.Append(" Invoke(");
			@Var int i = 0;
			while(i < FuncType.GetFuncParamSize()) {
				if(i > 0) {
					this.Source.Append(", ");
				}
				this.GenerateTypeName(FuncType.GetFuncParamType(i));
				this.Source.Append(" x"+i);
				i = i + 1;
			}
			this.Source.Append(");");

			this.Source.AppendNewLine(ClassName, "(int TypeId, String Name)");
			this.Source.OpenIndent(" {");
			this.Source.AppendNewLine("//super(TypeId, Name);");
			this.Source.CloseIndent("}");
			this.Source.CloseIndent("}");
			this.Source = this.Source.Pop();
		}
		return ClassName;
	}

	@Override protected void GenerateTypeName(BType Type) {
		if(Type instanceof BFuncType) {
			this.Source.Append(this.GetFuncTypeClass((BFuncType)Type));
		}
		else {
			this.Source.Append(this.GetJavaTypeName(Type.GetRealType(), false));
		}
	}

	protected void VisitVarDeclNode(BunLetVarNode Node) {
		this.GenerateTypeName(Node.DeclType());
		this.Source.Append(" ", Node.GetUniqueName(this), " = ");
		this.GenerateExpression(Node.InitValueNode());
		this.Source.Append(";");
	}

	@Override public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			this.GenerateTypeName(Node.DeclType());
			this.Source.Append(" ", Node.GetUniqueName(this));
		}
		else {
			@Var String ClassName = this.NameGlobalNameClass(Node.GetGivenName());
			//		this.CurrentBuilder = this.InsertNewSourceBuilder();
			this.Source.AppendNewLine("final class ", ClassName, "");
			this.Source.OpenIndent(" {");
			this.GenerateClassField("static", Node.GetAstType(BunLetVarNode._InitValue), "_");
			this.GenerateExpression(" = ", Node.InitValueNode(), ";");
			this.Source.CloseIndent("}");
			Node.GivenName = ClassName+"._";
			Node.NameIndex = 0;
		}
		//		this.CurrentBuilder = this.CurrentBuilder.Pop();
		//			Node.GlobalName = ClassName + "._";
		//			Node.GetGamma().SetLocalSymbol(Node.GetName(), Node.ToGlobalNameNode());
	}

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
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
					this.MainFuncNode = Node;
				}
				else {
					this.ExportFunctionList.add(Node);
				}
			}
			//@Var ZFuncType FuncType = Node.GetFuncType();
			//			if(this.IsMethod(Node.FuncName, FuncType)) {
			//				this.HeaderBuilder.Append("#define _" + this.NameMethod(FuncType.GetRecvType(), Node.FuncName));
			//				this.HeaderBuilder.AppendLineFeed();
			//			}
		}
	}

	private String GenerateFunctionAsClass(String FuncName, BunFunctionNode Node) {
		@Var BFuncType FuncType = Node.GetFuncType();
		@Var String ClassName = this.NameFunctionClass(FuncName, FuncType);
		this.GenerateClass("final", ClassName, FuncType);
		this.Source.OpenIndent(" {");
		this.Source.AppendNewLine("static ");
		this.GenerateTypeName(Node.ReturnType());
		this.Source.Append(" f");
		this.GenerateListNode("(", Node, ", ", ")");
		this.GenerateExpression(Node.BlockNode());

		this.GenerateClassField("static ", FuncType, "function", "new " + ClassName + "();");
		this.Source.AppendNewLine(ClassName, "()");
		this.Source.OpenIndent(" {");

		this.Source.AppendNewLine("super(", ""+FuncType.TypeId, ", ");
		this.Source.Append(LibBunSystem._QuoteString(FuncName), ");");
		this.Source.CloseIndent("}");

		this.Source.AppendNewLine("");
		this.GenerateTypeName(Node.ReturnType());
		this.Source.Append(" Invoke");
		this.GenerateListNode("(", Node, ", ", ")");
		this.Source.OpenIndent(" {");
		if(!FuncType.GetReturnType().IsVoidType()) {
			this.Source.AppendNewLine("return ", ClassName, ".f");
		}
		else {
			this.Source.AppendNewLine(ClassName, ".f");
		}
		this.GenerateWrapperCall("(", Node, ", ", ");");
		this.Source.CloseIndent("}");

		this.Source.CloseIndent("}");
		this.Source.AppendNewLine();
		return ClassName + ".function";
	}

	private void GenerateClass(String Qualifier, String ClassName, BType SuperType) {
		if(Qualifier != null && Qualifier.length() > 0) {
			this.Source.AppendNewLine(Qualifier);
			this.Source.AppendWhiteSpace("class ");
			this.Source.Append(ClassName);
		}
		else {
			this.Source.AppendNewLine("class ", ClassName);
		}
		if(!SuperType.Equals(BClassType._ObjectType)) {
			this.Source.Append(" extends ");
			this.GenerateTypeName(SuperType);
		}
	}

	private void GenerateClassField(String Qualifier, BType FieldType, String FieldName) {
		this.Source.AppendNewLine(Qualifier);
		this.Source.AppendWhiteSpace();
		this.GenerateTypeName(FieldType);
		this.Source.Append(" ", FieldName);
	}

	private void GenerateClassField(String Qualifier, BType FieldType, String FieldName, String Value) {
		this.GenerateClassField(Qualifier, FieldType, FieldName);
		if(Value != null) {
			this.Source.Append(" = ", Value, ";");
		}
		else {
			this.Source.Append(";");
		}
	}

	@Override public void VisitClassNode(BunClassNode Node) {
		@Var BType SuperType = Node.ClassType.GetSuperType();
		@Var String ClassName = this.NameClass(Node.ClassType);
		this.GenerateClass("", ClassName, SuperType);
		this.Source.OpenIndent(" {");
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			this.GenerateClassField("", FieldNode.DeclType(), FieldNode.GetGivenName(), null);
			i = i + 1;
		}
		this.Source.AppendNewLine();

		i = 0;
		while(i < Node.ClassType.GetFieldSize()) {
			@Var BClassField Field = Node.ClassType.GetFieldAt(i);
			if(Field.FieldType.IsFuncType()) {
				this.GenerateClassField("static", Field.FieldType, this.NameMethod(Node.ClassType, Field.FieldName), "null;");
			}
			i = i + 1;
		}

		this.Source.AppendNewLine(this.NameClass(Node.ClassType), "()");
		this.Source.OpenIndent(" {");
		this.Source.AppendNewLine("super();");
		while (i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			this.Source.AppendNewLine("this.", FieldNode.GetGivenName(), " = ");
			this.GenerateExpression(FieldNode.InitValueNode());
			this.Source.Append(";");
			i = i + 1;
		}
		i = 0;
		while(i < Node.ClassType.GetFieldSize()) {
			@Var BClassField Field = Node.ClassType.GetFieldAt(i);
			if(Field.FieldType.IsFuncType()) {
				this.Source.AppendNewLine("if(", this.NameMethod(Node.ClassType, Field.FieldName), " != null) ");
				this.Source.OpenIndent("{");
				this.Source.AppendNewLine("this.", Field.FieldName, " = ");
				this.Source.Append(this.NameMethod(Node.ClassType, Field.FieldName), ";", this.LineFeed);
				this.Source.CloseIndent("}");
			}
			i = i + 1;
		}

		this.Source.CloseIndent("}"); /* end of constructor*/
		this.Source.CloseIndent("}");  /* end of class */
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
		LibBunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
		this.Source.Append("ThrowError(");
		this.Source.Append(LibBunSystem._QuoteString(Node.ErrorMessage));
		this.Source.Append(")");
	}


}
