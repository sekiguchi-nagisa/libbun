package libbun.parser.peg;

import libbun.encode.LibBunSourceBuilder;
import libbun.parser.BToken;
import libbun.util.BArray;

public abstract class PegObject {
	BToken debugSource;
	Peg createdPeg;
	int startIndex;
	int endIndex;

	PegObject(Peg createdPeg, int startIndex, int endIndex) {
		this.createdPeg = createdPeg;
		this.startIndex = startIndex;
		this.endIndex   = endIndex;
	}

	public final boolean isErrorNode() {
		return this instanceof PegFailureNode;
	}

	public abstract void set(int index, PegObject childNode);
	abstract void stringfy(BToken source, LibBunSourceBuilder sb);

	String toString(BToken source) {
		LibBunSourceBuilder sb = new LibBunSourceBuilder(null, null);
		this.stringfy(source, sb);
		return sb.toString();
	}

}

class PegParsedNode extends PegObject {
	BArray<PegObject> elementList;

	PegParsedNode(Peg createdPeg, int startIndex, int endIndex) {
		super(createdPeg, startIndex, endIndex);
		this.elementList = null;
	}


	@Override public void set(int index, PegObject childNode) {
		if(this.elementList == null) {
			this.elementList = new BArray<PegObject>(new PegObject[2]);
		}
		this.elementList.add(childNode);
	}

	@Override public String toString() {
		if(this.elementList != null) {
			String s = "{'" + this.debugSource.substring(this.startIndex, this.endIndex) + "' ";
			for(int i = 0; i < this.elementList.size(); i++) {
				if(i > 0) {
					s = s + ",";
				}
				s = s + this.elementList.ArrayValues[i].toString();
			}
			return s + "}";
		}
		else {
			if(this.endIndex > this.startIndex) {
				return this.debugSource.substring(this.startIndex, this.endIndex);
			}
			return "{}";
		}
	}

	@Override
	void stringfy(BToken source, LibBunSourceBuilder sb) {
		if(this.elementList == null) {
			sb.AppendNewLine(source.substring(this.startIndex, this.endIndex), "   ## by " + this.createdPeg);
		}
		else {
			sb.AppendNewLine("node");
			sb.OpenIndent(" {            ## by " + this.createdPeg);
			sb.AppendNewLine("'", source.substring(this.startIndex, this.endIndex), "'");
			for(int i = 0; i < this.elementList.size(); i++) {
				this.elementList.ArrayValues[i].stringfy(source, sb);
			}
			sb.CloseIndent("}");
		}
	}
}

class PegFailureNode extends PegObject {
	String errorMessage;
	PegFailureNode(Peg createdPeg, int startIndex, String errorMessage) {
		super(createdPeg, startIndex, startIndex);
		this.errorMessage = errorMessage;
	}
	@Override public void set(int index, PegObject childNode) {

	}

	@Override public String toString() {
		return "!!ERROR: " + this.errorMessage + "!!";
	}

	@Override void stringfy(BToken source, LibBunSourceBuilder sb) {
		sb.AppendNewLine(this.debugSource.Source.FormatErrorMarker("error", this.debugSource.StartIndex, this.errorMessage + "   ## by " + this.createdPeg));
	}

}