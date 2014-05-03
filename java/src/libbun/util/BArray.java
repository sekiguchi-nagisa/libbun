package libbun.util;

import java.lang.reflect.Array;

public class BArray<T> {
	@BField private int    Size;
	@BField public T[] ArrayValues;

	public BArray(T[] Values) {
		this.ArrayValues = Values;
		this.Size = 0;
	}

	@SuppressWarnings("unchecked")
	public BArray(int TypeId, T[] Values) {
		if(Values == null || Values.length == 0) {
			this.ArrayValues = (T[]) new Object[1];
			this.Size = 0;
		}
		else {
			this.ArrayValues = Values;
			this.Size = Values.length;
		}
	}

	@Override public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("[");
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sBuilder.append(", ");
			}
			sBuilder.append(this.Stringify(this.ArrayValues[i]));
		}
		sBuilder.append("]");
		return sBuilder.toString();
	}

	protected String Stringify(Object Value) {
		if(Value instanceof String) {
			return LibBunSystem._QuoteString((String) Value);
		}
		return (String) Value;
	}

	public final int size() {
		return this.Size;
	}

	public final static<T> T GetIndex(BArray<T> a, long Index) {
		if(!(0 <= Index && Index < a.Size)) {
			BArray.ThrowOutOfArrayIndex(a.Size, Index);
		}
		return a.ArrayValues[(int)Index];
	}

	public final static<T> void SetIndex(BArray<T> a, long Index, T Value) {
		if(!(0 <= Index && Index < a.Size)) {
			BArray.ThrowOutOfArrayIndex(a.Size, Index);
		}
		a.ArrayValues[(int)Index] = Value;
	}

	private T[] NewArray(int CopySize, int NewSize) {
		@SuppressWarnings("unchecked")
		T[] newValues = (T[])Array.newInstance(this.ArrayValues.getClass().getComponentType(), NewSize);
		System.arraycopy(this.ArrayValues, 0, newValues, 0, CopySize);
		return newValues;
	}

	private void Reserve(int Size) {
		int CurrentCapacity = this.ArrayValues.length;
		if(Size < CurrentCapacity) {
			return;
		}
		int NewCapacity = CurrentCapacity * 2;
		if(NewCapacity < Size) {
			NewCapacity = Size;
		}
		this.ArrayValues = this.NewArray(this.Size, NewCapacity);
	}

	public final void add(T Value) {
		this.Reserve(this.Size + 1);
		this.ArrayValues[this.Size] = Value;
		this.Size = this.Size + 1;
	}

	public final void add(int Index, T Value) {
		this.Reserve(this.Size + 1);
		System.arraycopy(this.ArrayValues, Index, this.ArrayValues, Index+1, this.Size - Index);
		this.ArrayValues[Index] = Value;
		this.Size = this.Size + 1;
	}

	public void clear(int Index) {
		this.Size = Index;
	}

	public T[] CompactArray() {
		if(this.Size == this.ArrayValues.length) {
			return this.ArrayValues;
		}
		else {
			@SuppressWarnings("unchecked")
			T[] newValues = (T[])Array.newInstance(this.ArrayValues.getClass().getComponentType(), this.Size);
			System.arraycopy(this.ArrayValues, 0, newValues, 0, this.Size);
			return newValues;
		}
	}

	public static void ThrowOutOfArrayIndex(int Size, long Index) {
		throw new RuntimeException("out of array index " + Index + " < " + Size);
	}
}
