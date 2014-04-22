package libbun.ast;

import libbun.util.BField;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public abstract class AbstractListNode extends BNode {
	@BField public int ListStartIndex;
	public AbstractListNode(BNode ParentNode, int Size) {
		super(ParentNode, Size);
		this.ListStartIndex = Size;
	}

	public final int GetListSize() {
		return this.GetAstSize() - this.ListStartIndex;
	}

	public final BNode GetListAt(int Index) {
		return this.AST[this.ListStartIndex + Index];
	}

	public final void SetListAt(int Index, BNode Node) {
		this.SetNode(Index + this.ListStartIndex, Node);
	}

	public final void InsertListAt(int Index, BNode Node) {
		if(this.AST == null || Index < 0 || this.AST.length == Index) {
			this.Append(Node);
		} else {
			@Var BNode[] newAST = LibBunSystem._NewNodeArray(this.AST.length + 1);
			@Var BNode[] oldAST = this.AST;
			Index = this.ListStartIndex + Index;
			this.AST = newAST;
			LibBunSystem._ArrayCopy(oldAST, 0, newAST, 0, Index);
			this.SetNode(Index, Node);
			LibBunSystem._ArrayCopy(oldAST, Index, newAST, Index + 1, oldAST.length - Index);
		}
	}

	public final BNode RemoveListAt(int Index) {
		@Var BNode Removed = this.GetListAt(Index);
		@Var BNode[] newAST = LibBunSystem._NewNodeArray(this.AST.length - 1);
		@Var int RemovedIndex = this.ListStartIndex + Index;
		LibBunSystem._ArrayCopy(this.AST, 0, newAST, 0, RemovedIndex);
		LibBunSystem._ArrayCopy(this.AST, RemovedIndex + 1, newAST, RemovedIndex, this.AST.length - (RemovedIndex + 1));
		this.AST = newAST;
		return Removed;
	}

	public final void ClearListToSize(int Size) {
		if(Size < this.GetListSize()) {
			@Var int newsize = this.ListStartIndex + Size;
			if(newsize == 0) {
				this.AST = null;
			}
			else {
				@Var BNode[] newAST = LibBunSystem._NewNodeArray(newsize);
				LibBunSystem._ArrayCopy(this.AST, 0, newAST, 0, newsize);
				this.AST = newAST;
			}
		}
	}

}
