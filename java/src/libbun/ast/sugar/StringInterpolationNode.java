package libbun.ast.sugar;

import libbun.ast.BNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.LibBunTypeChecker;
import libbun.type.BType;
import libbun.util.Var;

public class StringInterpolationNode extends SyntaxSugarNode {

	public StringInterpolationNode(BNode ParentNode) {
		super(ParentNode,0);
	}

	@Override public void PerformTyping(LibBunTypeChecker TypeChecker, BType ContextType) {
		@Var int i = 0;
		while(i < this.GetAstSize()) {
			if(i % 2 == 0) {
				assert(this.AST[i] instanceof BunStringNode);
				TypeChecker.CheckTypeAt(this, i, BType.StringType);
			}
			else {
				TypeChecker.TryTypeAt(this, i, BType.VarType);
			}
			i = i + 1;
		}
		TypeChecker.TypeNode(this, BType.StringType);
	}

	@Override public DesugarNode PerformDesugar(LibBunTypeChecker TypeChecker) {
		@Var BNode LeftNode = this.CreateStringConcat(TypeChecker, this.AST[0], this.AST[1]);
		@Var int i = 2;
		while(i < this.GetAstSize()) {
			LeftNode = this.CreateStringConcat(TypeChecker, LeftNode, this.AST[i]);
			i = i + 1;
		}
		return new DesugarNode(this, LeftNode);
	}

	private BNode CreateStringConcat(LibBunTypeChecker TypeChecker, BNode LeftNode, BNode RightNode) {
		@Var BunAddNode BinaryNode = new BunAddNode(null);
		BinaryNode.SetLeftNode(this.EnforceStringTypedNode(TypeChecker, LeftNode));
		BinaryNode.SetRightNode(this.EnforceStringTypedNode(TypeChecker, RightNode));
		TypeChecker.TypeNode(BinaryNode, BType.StringType);
		return BinaryNode;
	}

	private BNode EnforceStringTypedNode(LibBunTypeChecker TypeChecker, BNode Node) {
		if(!Node.Type.IsStringType()) {
			Node = TypeChecker.EnforceNodeType(Node, BType.StringType);
		}
		return Node;
	}
}
