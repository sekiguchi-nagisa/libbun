package libbun.encode.devel;

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
import libbun.ast.error.TypeErrorNode;
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
import libbun.type.BClassType;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.util.Var;

public class LuaGenerator extends LibBunSourceGenerator {

	public LuaGenerator() {
		super(new LibBunLangInfo("Lua-5.2", "lua"));
		this.LoadInlineLibrary("inline.lua", "##");
		this.SetNativeType(BType.BooleanType, "boolean");
		this.SetNativeType(BType.IntType, "number");
		this.SetNativeType(BType.FloatType, "number");
		this.SetNativeType(BType.StringType, "string");
		this.SetNativeType(BType.VarType, "table"); //FIXME
	}

	@Override protected void GenerateImportLibrary(String LibName) {
		// TODO Auto-generated method stub

	}

	@Override protected void GenerateStatementEnd(BNode Node) {
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		this.Source.Append("nil");
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		if(Node.BooleanValue) {
			this.Source.Append("true");
		}
		else {
			this.Source.Append("false");
		}
	}

	@Override public void VisitIntNode(BunIntNode Node) {
		this.Source.Append(""+Node.IntValue);
	}

	@Override public void VisitFloatNode(BunFloatNode Node) {
		this.Source.Append(""+Node.FloatValue);
	}

	@Override public void VisitStringNode(BunStringNode Node) {
		this.Source.AppendQuotedText(Node.StringValue);
	}

	@Override
	public void VisitNotNode(BunNotNode Node) {
		this.Source.Append("not ");
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitPlusNode(BunPlusNode Node) {
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitMinusNode(BunMinusNode Node) {
		this.Source.Append("-");
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitComplementNode(BunComplementNode Node) {
		this.Source.Append("bit32.bnot(");
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(")");
	}

	private void GenerateAddOperatorExpression(BinaryOperatorNode Node) {
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		BType LeftType = Node.LeftNode().Type;
		BType RightType = Node.RightNode().Type;
		if(LeftType == BType.StringType || RightType == BType.StringType) {
			this.Source.Append("tostring(");
			this.GenerateExpression(Node.LeftNode());
			this.Source.Append(")");
			this.Source.Append(" .. ");
			this.Source.Append("tostring(");
			this.GenerateExpression(Node.RightNode());
			this.Source.Append(")");
		} else {
			this.GenerateExpression(Node.LeftNode());
			this.Source.AppendWhiteSpace("+", " ");
			this.GenerateExpression(Node.RightNode());
		}
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
	}

	private void GenerateBinaryOperatorExpression(BinaryOperatorNode Node, String Operator) {
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		this.GenerateExpression(Node.LeftNode());
		this.Source.AppendWhiteSpace(Operator, " ");
		this.GenerateExpression(Node.RightNode());
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "and");
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		this.GenerateBinaryOperatorExpression(Node, "or");
	}

	@Override public void VisitAddNode(BunAddNode Node) {
		this.GenerateAddOperatorExpression(Node);
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
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		this.Source.Append("bit32.lshift(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(", ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode Node) {
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append("(");
		}
		this.Source.Append("bit32.rshift(");
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(", ");
		this.GenerateExpression(Node.RightNode());
		this.Source.Append(")");
		if (Node.ParentNode instanceof BinaryOperatorNode) {
			this.Source.Append(")");
		}
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
		this.GenerateBinaryOperatorExpression(Node, "~=");
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

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		this.GenerateListNode("{", Node, ",", "}");
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		this.Source.Append("{");
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BunMapEntryNode Entry = Node.GetMapEntryNode(i);
			@Var BunStringNode KeyNode = (BunStringNode)Entry.KeyNode(); //FIXME
			this.GenerateExpression(KeyNode.StringValue + "= ", Entry.ValueNode(), "");
			i = i + 1;
			if(i < Node.GetListSize()) {
				this.Source.Append(",");
			}
		}
		this.Source.Append("} ");  // space is needed to distinguish block
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
		this.GenerateTypeName(Node.Type);
		this.Source.Append(".new");
		this.GenerateListNode("(", Node, ",", ")");
	}

	protected final void GenerateFuncName(BunFuncNameNode Node) {
		if(this.LangInfo.AllowFunctionOverloading) {
			this.Source.Append(Node.FuncName);
		}
		else {
			this.Source.Append(Node.GetSignature());
		}
	}

	@Override public void VisitFuncCallNode(FuncCallNode Node) {
		@Var BunFuncNameNode FuncNameNode = Node.FuncNameNode();
		if(FuncNameNode != null) {
			this.GenerateFuncName(FuncNameNode);
		}
		else {
			this.GenerateExpression(Node.FunctorNode());
		}
		this.GenerateListNode("(", Node, ", ", ")");
	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		@Var BNode ResolvedNode = Node.ResolvedNode;
		if(ResolvedNode == null && !this.LangInfo.AllowUndefinedSymbol) {
			LibBunLogger._LogError(Node.SourceToken, "undefined symbol: " + Node.GivenName);
		}
		this.Source.Append(Node.GetUniqueName(this));
	}

	@Override public void VisitAssignNode(AssignNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" = ");
		this.GenerateExpression(Node.RightNode());
	}

	@Override public void VisitGetFieldNode(GetFieldNode Node) {
		this.GenerateExpression(Node.RecvNode());
		if(Node.Type instanceof BFuncType) {
			this.Source.Append(":", Node.GetName());
		} else {
			this.Source.Append(".", Node.GetName());
		}
	}

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.Source.Append(":", Node.MethodName());
		this.GenerateListNode("(", Node, ",", ")");
	}

	@Override public void VisitUnaryNode(UnaryOperatorNode Node) {
		this.Source.Append(Node.GetOperator());
		this.GenerateExpression(Node.RecvNode());
	}

	@Override public void VisitCastNode(BunCastNode Node) {
		this.GenerateExpression(Node.ExprNode());
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
		this.Source.OpenIndent(); //FIXME
		this.VisitStmtList(Node);
		this.Source.CloseIndent();
	}

	@Override
	public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void VisitBinaryNode(BinaryOperatorNode Node) {
		// TODO Auto-generated method stub

	}

	@Override public void VisitVarBlockNode(BunVarBlockNode Node) {
		@Var BunLetVarNode VarNode = Node.VarDeclNode();
		this.Source.Append("local ", VarNode.GetUniqueName(this), " = ");
		this.GenerateExpression(VarNode.InitValueNode());
		this.VisitStmtList(Node);
	}

	@Override public void VisitIfNode(BunIfNode Node) {
		this.GenerateExpression("if ", Node.CondNode(), " then");
		this.GenerateExpression(Node.ThenNode());
		if (Node.HasElseNode()) {
			this.Source.AppendNewLine("else ");
			this.GenerateExpression(Node.ElseNode());
		}
		this.Source.AppendNewLine("end");
	}

	@Override public void VisitReturnNode(BunReturnNode Node) {
		this.Source.Append("return");
		if (Node.HasReturnExpr()) {
			this.Source.Append(" ");
			this.GenerateExpression(Node.ExprNode());
		}
	}

	@Override public void VisitWhileNode(BunWhileNode Node) {
		if(Node.HasNextNode()) {
			this.GenerateExpression("while ", Node.CondNode(), " do");
			this.GenerateExpression(Node.BlockNode());
			this.Source.OpenIndent();
			this.Source.AppendNewLine();
			this.GenerateExpression(Node.NextNode());
			this.Source.CloseIndent();
			this.Source.AppendNewLine("end");
		} else {
			this.GenerateExpression("while ", Node.CondNode(), " do");
			this.GenerateExpression(Node.BlockNode());
			this.Source.AppendNewLine("end");
		}
	}

	@Override public void VisitBreakNode(BunBreakNode Node) {
		this.Source.OpenIndent("do");
		this.Source.AppendNewLine();
		this.Source.Append("break");
		this.Source.CloseIndent("end");
	}

	@Override public void VisitThrowNode(BunThrowNode Node) {
		this.Source.Append("error(");
		this.GenerateExpression(Node.ExprNode());
		this.Source.Append(")");
	}

	@Override
	public void VisitTryNode(BunTryNode Node) {
		this.ImportLibrary("@try");
		this.Source.OpenIndent("libbun_try({");
		this.Source.AppendNewLine("try = function()");
		this.GenerateExpression(Node.TryBlockNode());
		this.Source.AppendNewLine("end");
		if(Node.HasCatchBlockNode()) {
			this.Source.Append(",");
			this.Source.AppendNewLine("catch = function(e)"); //FIXME e
			this.GenerateExpression(Node.CatchBlockNode());
			this.Source.AppendNewLine("end");
		}
		if(Node.HasFinallyBlockNode()) {
			this.Source.Append(",");
			this.Source.AppendNewLine("finally = function()");
			this.GenerateExpression(Node.FinallyBlockNode());
			this.Source.AppendNewLine("end");
		}
		this.Source.CloseIndent("})");
	}

	@Override
	public void VisitLetNode(BunLetVarNode Node) {
		if(Node.IsParamNode()) {
			this.Source.Append(Node.GetUniqueName(this));
		}
		else {
			this.Source.Append(Node.GetUniqueName(this), " = ");
			this.GenerateExpression(Node.InitValueNode());
		}
	}

	private String CreateMethoName(BType ClassType, String MethodName) {
		return this.NameClass(ClassType) + "." + MethodName;
	}

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		if(!Node.IsTopLevelDefineFunction()) {
			@Var String FuncName = Node.GetUniqueName(this);
			this.Source = this.InsertNewSourceBuilder();
			this.Source.AppendNewLine("function ", FuncName);
			this.GenerateListNode("(", Node, ",", ")");
			this.GenerateExpression(Node.BlockNode());
			this.Source.AppendNewLine("end");
			this.Source = this.Source.Pop();
			this.Source.Append(FuncName);
		}
		else {
			@Var BFuncType FuncType = Node.GetFuncType();
			this.Source.AppendNewLine("function ", Node.GetSignature());
			this.GenerateListNode("(", Node, ",", ")");
			this.GenerateExpression(Node.BlockNode());
			this.Source.AppendNewLine("end");
			if(Node.IsExport) {
				this.Source.AppendNewLine(Node.FuncName(), " = ", FuncType.StringfySignature(Node.FuncName()));
				if(Node.FuncName().equals("main")) {
					this.HasMainFunction = true;
				}
			}
			if(this.IsMethod(Node.FuncName(), FuncType)) {
				this.Source.AppendNewLine(this.CreateMethoName(FuncType.GetRecvType(), Node.FuncName()));
				this.Source.Append(" = ", FuncType.StringfySignature(Node.FuncName()));
			}
		}
	}

	@Override public void VisitClassNode(BunClassNode Node) {
		@Var BType SuperType = Node.ClassType.GetSuperType();
		@Var String ClassName = this.NameClass(Node.ClassType);
		this.Source.AppendNewLine(ClassName, " = {}");
		this.Source.AppendNewLine(ClassName, "_mt = { __index = " , ClassName + " }");
		this.Source.AppendNewLine();
		this.Source.AppendNewLine("function ", ClassName, ":new()");
		this.Source.OpenIndent();
		this.Source.AppendNewLine("local __initObject = ");
		if(!SuperType.Equals(BClassType._ObjectType)) {
			this.Source.Append(this.NameClass(SuperType), ".new()");
		}
		else {
			this.Source.Append("{}");
		}
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			if(!FieldNode.DeclType().IsFuncType()) {
				this.Source.AppendNewLine("__initObject.", FieldNode.GetGivenName(), " = ");
				this.GenerateExpression(FieldNode.InitValueNode());
			}
			i = i + 1;
		}
		this.Source.AppendNewLine("return setmetatable(__initObject, ", ClassName,"_mt)");
		this.Source.CloseIndent("end");
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		@Var BType RecvType = Node.GetAstType(GetIndexNode._Recv);
		if(RecvType.IsMapType()) {
			this.GenerateExpression("", Node.RecvNode(), "[", Node.IndexNode(), "]");
		}
		else if(RecvType.IsArrayType()) {
			this.GenerateExpression(Node.RecvNode());
			this.GenerateExpression("[", Node.IndexNode(), " + 1]");
		}
		else {
			this.GenerateExpression("string.sub(", Node.RecvNode(), ", ", Node.IndexNode(), " + 1,", Node.IndexNode(), " + 1)");
		}
	}

	@Override
	public void VisitErrorNode(ErrorNode Node) {
		if(Node instanceof TypeErrorNode) {
			@Var TypeErrorNode ErrorNode = (TypeErrorNode)Node;
			this.GenerateExpression(ErrorNode.ErrorNode);
		}
		else {
			@Var String Message = LibBunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
			this.Source.Append("libbun_error(");
			this.Source.AppendQuotedText(Message);
			this.Source.Append(")");
			this.ImportLibrary("@error");
		}
	}

}
