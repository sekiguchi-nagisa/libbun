package libbun.parser.peg;

import libbun.ast.BNode;
import libbun.parser.LibBunVisitor;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class PegNode extends BNode {
	public PegNode(BNode ParentNode, int Size) {
		super(ParentNode, Size);
	}
	public BNode Tokneize(ParserContext SourceContext) {
		return this;
	}
	@Override public void Accept(LibBunVisitor Visitor) {
		// TODO Auto-generated method stub
	}
	@Override public String toString() {
		@Var String Self = "(peg ";
		if(this.SourceToken != null) {
			Self = Self + this.SourceToken.GetText() + " ";
		}
		if(this.AST != null) {
			@Var int i = 0;
			while(i < this.AST.length) {
				if(i > 0) {
					Self = Self + ",";
				}
				if(this.AST[i] == null) {
					Self = Self + "null";
				}
				else {
					if(this.AST[i].ParentNode == this) {
						Self = Self + this.AST[i].toString();
					}
					else {
						Self = Self + "*" + LibBunSystem._GetClassName(this.AST[i])+"*";
					}
				}
				i = i + 1;
			}
		}
		Self = Self + ")";
		return Self;
	}

}

