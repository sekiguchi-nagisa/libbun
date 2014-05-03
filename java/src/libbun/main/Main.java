// ***************************************************************************
// Copyright (c) 2013-2014, Libbun project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// *  Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// *  Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// **************************************************************************

package libbun.main;

import java.io.IOException;

import libbun.encode.LibBunGenerator;
import libbun.parser.LibBunConst;
import libbun.parser.peg.PegBunGrammar;
import libbun.util.BArray;
import libbun.util.LibBunSystem;
import libbun.util.Var;


public class Main {

	// -t bun
	private static String Target = "bun";

	// -p konoha
	private static String Parser = "syntax::bun";

	// -o
	private static String OutputFileName = null;

	// --peg
	private static String PegFileName = null;

	//	// -l
	//	private static boolean ShowListMode = false;

	// -i
	private static boolean ShellMode = false;

	private static BArray<String> ARGV = null;

	//
	private static String InputFileName = null;


	public final static void ParseCommand(String[] Args) {
		@Var int Index = 0;
		@Var String GivenTarget = null;
		@Var String GuessTarget = null;
		@Var String GivenParser = null;
		while (Index < Args.length) {
			@Var String Argument = Args[Index];
			if (!Argument.startsWith("-")) {
				break;
			}
			Index = Index + 1;
			if ((Argument.equals("-o") || Argument.equals("--out")) && (Index < Args.length)) {
				if (!Args[Index].endsWith(".bun")) { // for safety
					OutputFileName = Args[Index];
					@Var int loc = OutputFileName.lastIndexOf('.');
					if(loc > 0) {
						GuessTarget = OutputFileName.substring(loc+1);
					}
					Index += 1;
				}
				continue;
			}
			if ((Argument.equals("-t") || Argument.equals("--target")) && (Index < Args.length)) {
				GivenTarget = Args[Index];
				Index += 1;
				continue;
			}
			if ((Argument.equals("-p") || Argument.equals("--parser")) && (Index < Args.length)) {
				GivenParser = Args[Index];
				Index += 1;
				continue;
			}
			if ((Argument.equals("--peg")) && (Index < Args.length)) {
				PegFileName = Args[Index];
				Index += 1;
				continue;
			}
			//			if (Argument.equals("-l")) {
			//				ShowListMode = true;
			//				continue;
			//			}
			if (Argument.equals("-i")) {
				ShellMode = true;
				continue;
			}
			if (Argument.equals("--verbose")) {
				LibBunSystem._SetDebugVerbose(true);
				continue;
			}
			ShowUsage("unknown option: " + Argument);
		}
		if(GivenTarget == null) {
			GivenTarget = GuessTarget;
		}
		if(GivenTarget != null) {
			Target = GivenTarget;
		}
		if(GivenParser != null) {
			Parser = GivenParser;
		}
		ARGV = new BArray<String>(new String[4]);
		while (Index < Args.length) {
			ARGV.add(Args[Index]);
			Index += 1;
		}
		if (ARGV.size() > 0) {
			InputFileName = ARGV.ArrayValues[0];
		}
		else {
			ShellMode = true;
		}
	}

	public final static void ShowUsage(String Message) {
		System.out.println("libbun usage :");
		System.out.println("  --out|-o  FILE          Output filename");
		System.out.println("  --verbose               Printing Debug infomation");
		LibBunSystem._Exit(0, Message);
	}

	public final static void InvokeLibBun(String[] Args) {
		ParseCommand(Args);
		if(PegFileName != null) {
			PegBunGrammar.test(PegFileName);
		}
		@Var LibBunGenerator Generator = LibBunSystem._InitGenerator(Target, Parser);
		if(InputFileName != null) {
			Generator.LoadFile(InputFileName, null);
		}
		Generator.WriteTo(OutputFileName);
		Generator.ExecMain();
		if (ShellMode) {
			PerformShell(Generator);
		}
	}

	public final static void main(String[] Args) {
		Main.InvokeLibBun(Args);
	}

	public final static void PerformShell(LibBunGenerator Generator) {
		LibBunSystem._PrintLine(LibBunConst.ProgName + LibBunConst.Version + " (" + LibBunConst.CodeName + ") on " + LibBunSystem._GetPlatform());
		LibBunSystem._PrintLine(LibBunConst.Copyright);
		LibBunSystem._PrintLine("Accept: " + Generator.LangInfo.GetGrammarInfo());
		LibBunSystem._PrintLine("Produce: " + Generator.LangInfo.LangVersion);
		@Var int linenum = 1;
		@Var String Line = null;
		while ((Line = Main.ReadLine2(">>> ", "    ")) != null) {
			try {
				if(Generator.LoadScript(Line, "(stdin)", linenum)) {
					Generator.Perform();
				}
				linenum = linenum + 1;
			}
			catch (Exception e) {
				Main.PrintStackTrace(e, linenum);
				linenum = linenum + 1;
			}
		}
		LibBunSystem._PrintLine("");
	}

	private static jline.ConsoleReader ConsoleReader = null;

	private final static String ReadLine2(String Prompt, String Prompt2) {
		if(ConsoleReader == null) {
			try {
				ConsoleReader = new jline.ConsoleReader();
				//ConsoleReader.setExpandEvents(false);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		@Var String Line;
		try {
			Line = ConsoleReader.readLine(Prompt);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		if(Line == null) {
			System.exit(0);
		}
		if(Prompt2 != null) {
			@Var int level = 0;
			while((level = Main.CheckBraceLevel(Line)) > 0) {
				String Line2;
				try {
					Line2 = ConsoleReader.readLine(Prompt2);
					//Line2 = ConsoleReader.readLine(Prompt2 + ZenUtils.JoinStrings("  ", level));
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
				Line += "\n" + Line2;
			}
			if(level < 0) {
				Line = "";
				LibBunSystem._PrintLine(" .. canceled");
			}
		}
		ConsoleReader.getHistory().addToHistory(Line);
		return Line;
	}

	private static void PrintStackTrace(Exception e, int linenum) {
		@Var StackTraceElement[] elements = e.getStackTrace();
		@Var int size = elements.length + 1;
		@Var StackTraceElement[] newElements = new StackTraceElement[size];
		@Var int i = 0;
		for(; i < size; i++) {
			if(i == size - 1) {
				newElements[i] = new StackTraceElement("<TopLevel>", "TopLevelEval", "stdin", linenum);
				break;
			}
			newElements[i] = elements[i];
		}
		e.setStackTrace(newElements);
		e.printStackTrace();
	}

	private final static int CheckBraceLevel(String Text) {
		@Var int level = 0;
		@Var int i = 0;
		while(i < Text.length()) {
			@Var char ch = Text.charAt(i);
			if(ch == '{' || ch == '[') {
				level = level + 1;
			}
			if(ch == '}' || ch == ']') {
				level = level - 1;
			}
			i = i + 1;
		}
		return level;
	}

	//	private final static void WriteSource(String OutputFile, ArrayList<ZSourceBuilder> SourceList) {
	//		try {
	//			@Var PrintStream Stream = null;
	//			if(OutputFile.equals("-")) {
	//				Stream = System.out;
	//				Stream.flush();
	//			}
	//			else {
	//				Stream = new PrintStream(OutputFile);
	//			}
	//			for(ZSourceBuilder Builder : SourceList) {
	//				for(String Source : Builder.SourceList.ArrayValues) {
	//					Stream.print(Source);
	//				}
	//				Stream.println();
	//			}
	//			Stream.flush();
	//			if(Stream != System.out) {
	//				Stream.close();
	//			}
	//		} catch (IOException e) {
	//			System.err.println("Cannot write: " + OutputFile);
	//			System.exit(1);
	//		}
	//	}
	//
	//	private static java.io.Console Console = null;
	//	private static java.io.BufferedReader Reader = null;
	//	private static boolean ConsoleInitialized = false;
	//
	//	static private String ReadLine(String format, Object... args) {
	//		if(!ConsoleInitialized){
	//			Console = System.console();
	//			if (Console == null) {
	//				Reader = new BufferedReader(new InputStreamReader(System.in));
	//			}
	//			ConsoleInitialized = true;
	//		}
	//		if (Console != null) {
	//			return System.console().readLine(format, args);
	//		}
	//		System.out.print(String.format(format, args));
	//		try {
	//			return Reader.readLine();
	//		}
	//		catch (IOException e) {
	//			e.printStackTrace();
	//			return "";
	//		}
	//	}
	//
	//	private final static String ReadLine(String Prompt, String Prompt2) {
	//		@Var String Line = ReadLine(Prompt);
	//		if(Line == null) {
	//			System.exit(0);
	//		}
	//		if(Prompt2 != null) {
	//			@Var int level = 0;
	//			while((level = Main.CheckBraceLevel(Line)) > 0) {
	//				@Var String Line2 = ReadLine(Prompt2 + LibZen._JoinStrings("  ", level));
	//				Line += "\n" + Line2;
	//			}
	//			if(level < 0) {
	//				Line = "";
	//				LibZen._PrintLine(" .. canceled");
	//			}
	//		}
	//		return Line;
	//	}


}
