package libbun.lang.bun.shell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Stack;

import libbun.ast.BNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.literal.BunStringNode;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.util.BArray;
import libbun.util.BStringArray;
import libbun.util.ZObjectArray;

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
			return this.GetParentScope().IsCommand(CommandSymbol);
		}

		public String GetCommand(String CommandSymbol) {
			String Command = this.CommandMap.get(CommandSymbol);
			if(Command != null) {
				return Command;
			}
			if(this.GetParentScope() == null) {
				return null;
			}
			return this.GetParentScope().GetCommand(CommandSymbol);
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

	public static long ExecCommandInt(ZObjectArray ArgsList) {
		return ExecCommand(ArgsList, null);
	}

	public static boolean ExecCommandBoolean(ZObjectArray ArgsList) {
		return ExecCommandInt(ArgsList) == 0;
	}

	public static String ExecCommandString(ZObjectArray ArgsList) {
		ByteArrayOutputStream StreamBuffer = new ByteArrayOutputStream();
		ExecCommand(ArgsList, StreamBuffer);
		return ShellUtils.RemoveNewLine(StreamBuffer.toString());
	}

	private static int ExecCommand(ZObjectArray ArgsList, final OutputStream TargetStream) {
		StringBuilder ArgBuilder = new StringBuilder();
		int ListSize = (int) ArgsList.Size();
		for(int i = 0; i < ListSize; i++) {
			BStringArray Args = (BStringArray) ZObjectArray.GetIndex(ArgsList, i);
			int Size = (int) Args.Size();
			if(!MatchRedireSymbol(BStringArray.GetIndex(Args, 0)) && i != 0) {
				ArgBuilder.append("| ");
			}
			for(int j = 0; j < Size; j++) {
				ArgBuilder.append(BStringArray.GetIndex(Args, j));
				ArgBuilder.append(" ");
			}
		}
		ProcessBuilder ProcBuilder = new ProcessBuilder("bash", "-c", ArgBuilder.toString());
		//FIXME: segiguchi
		//		ProcBuilder.inheritIO();
		//		if(TargetStream != null) {
		//			ProcBuilder.redirectOutput(Redirect.PIPE);
		//		}
		try {
			final Process Proc = ProcBuilder.start();
			if(TargetStream != null) {
				Thread StreamHandler = new Thread(){
					@Override public void run() {
						InputStream Input = Proc.getInputStream();
						byte[] buffer = new byte[512];
						int ReadSize = 0;
						try {
							while((ReadSize = Input.read(buffer, 0, buffer.length)) > -1) {
								TargetStream.write(buffer, 0, ReadSize);
							}
							Input.close();
							TargetStream.close();
						}
						catch (IOException e) {
							return;
						}
					}
				};
				StreamHandler.start();
				StreamHandler.join();
			}
			return Proc.waitFor();
		}
		catch (IOException e) {
			System.err.println("executin failed");
			return -1;
		}
		catch (InterruptedException e) {
			return -1;
		}
	}

	private static boolean MatchRedireSymbol(String Symbol) {
		if(Symbol.equals("<")) {
			return true;
		}
		if(Symbol.equals("1>") || Symbol.equals(">") || Symbol.equals("1>>") || Symbol.equals(">>")) {
			return true;
		}
		if(Symbol.equals("2>") || Symbol.equals("2>>") || Symbol.equals("2>&1")) {
			return true;
		}
		if(Symbol.equals("&>") || Symbol.equals(">&") || Symbol.equals("&>>")) {
			return true;
		}
		return false;
	}

	public static String RemoveNewLine(String Value) {
		int Size = Value.length();
		int EndIndex = Size;
		for(int i = Size - 1; i > -1; i--) {
			char ch = Value.charAt(i);
			if(ch != '\n') {
				EndIndex = i + 1;
				break;
			}
		}
		return EndIndex == Size ? Value : Value.substring(0, EndIndex);
	}
}
