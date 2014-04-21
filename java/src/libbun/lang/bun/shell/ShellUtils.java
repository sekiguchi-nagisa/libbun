package libbun.lang.bun.shell;

import java.io.File;
import java.util.HashMap;
import java.util.Stack;

import libbun.ast.BNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.util.BArray;

// you must implement this class if you use shell grammar
public class ShellUtils {
	// suffix option symbol
	public final static String _background = "&";
	// prefix option symbol
	public final static String _timeout = "timeout";
	public final static String _trace = "trace";

	public static boolean _MatchStopToken(BTokenContext TokenContext) { // ;,)]}&&||
		BToken Token = TokenContext.GetToken();
		if(!TokenContext.HasNext()) {
			return true;
		}
		if(Token.IsIndent() || Token.EqualsText(";")) {
			return true;
		}
		if(Token.EqualsText(",") || Token.EqualsText(")") || Token.EqualsText("]") ||
				Token.EqualsText("}") || Token.EqualsText("&&") || Token.EqualsText("||") || Token.EqualsText("`")) {
			return true;
		}
		return false;
	}

	public static BNode _ToNode(BNode ParentNode, BTokenContext TokenContext, BArray<BNode> NodeList) {
		BNode Node = new BunStringNode(ParentNode, null, "");
		int size = NodeList.size();
		for(int i = 0; i < size; i++) {
			BNode CurrentNode = BArray.GetIndex(NodeList, i);
			BunAddNode BinaryNode = new BunAddNode(ParentNode);
			BinaryNode.SetLeftNode(Node);
			BinaryNode.SetRightNode(CurrentNode);
			Node = BinaryNode;
		}
		return Node;
	}

	public static boolean _IsFileExecutable(String Path) {
		return new File(Path).canExecute();
	}

	public static String _ResolveHome(String Path) {
		if(Path.equals("~")) {
			return System.getenv("HOME");
		}
		else if(Path.startsWith("~/")) {
			return System.getenv("HOME") + Path.substring(1);
		}
		return Path;
	}

	public static String _GetUnixCommand(String cmd) {
		String[] Paths = System.getenv("PATH").split(":");
		for(String Path : Paths) {
			String FullPath = _ResolveHome(Path + "/" + cmd);
			if(_IsFileExecutable(FullPath)) {
				return FullPath;
			}
		}
		return null;
	}

	public static class CommandScope {
		private final CommandScope ParentScope;
		private final HashMap<String, String> CommandMap;

		public CommandScope(CommandScope ParentScope) {
			this.ParentScope = ParentScope;
			this.CommandMap = new HashMap<String, String>();
		}

		public CommandScope() {
			this(null);
		}

		private CommandScope GetParentScope() {
			return this.ParentScope;
		}

		public boolean SetCommand(String CommandSymbol, String Command) {
			if(this.CommandMap.containsKey(CommandSymbol)) {
				return false;
			}
			this.CommandMap.put(CommandSymbol, Command);
			return true;
		}

		public boolean IsCommand(String CommandSymbol) {
			if(this.CommandMap.containsKey(CommandSymbol)) {
				return true;
			}
			if(this.GetParentScope() == null) {
				return false;
			}
			else {
				return this.GetParentScope().IsCommand(CommandSymbol);
			}
		}

		public String GetCommand(String CommandSymbol) {
			String Command = this.CommandMap.get(CommandSymbol);
			if(Command != null) {
				return Command;
			}
			if(this.GetParentScope() == null) {
				return null;
			}
			else {
				return this.GetParentScope().GetCommand(CommandSymbol);
			}
		}
	}

	public static final Stack<CommandScope> ScopeStack = new Stack<ShellUtils.CommandScope>();
	static {
		ScopeStack.push(new CommandScope());
	}

	public static boolean IsCommand(String CommandSymbol) {
		return ScopeStack.peek().IsCommand(CommandSymbol);
	}

	public static boolean SetCommand(String CommandSymbol, String Command) {
		return ScopeStack.peek().SetCommand(CommandSymbol, Command);
	}

	public static String GetCommand(String CommandSymbol) {
		return ScopeStack.peek().GetCommand(CommandSymbol);
	}

	public static void CreateNewCommandScope() {
		ScopeStack.push(new CommandScope(ScopeStack.peek()));
	}

	public static void RemoveCommandScope() {
		if(ScopeStack.size() > 1) {
			ScopeStack.pop();
		}
	}
}
