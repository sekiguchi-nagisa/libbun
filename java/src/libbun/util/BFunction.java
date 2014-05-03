package libbun.util;

import libbun.type.BType;
import libbun.type.BTypePool;

public abstract class BFunction implements BTypedObject {
	@BField final BType  BunType;
	@BField final String FUNCTION;
	public BFunction(int TypeId, String f) {
		this.BunType  = BTypePool.TypeOf(TypeId);
		if(f == null) {
			f= this.getClass().getSimpleName();
		}
		this.FUNCTION = f;
	}
	@Override public final BType GetBunType() {
		return this.BunType;
	}
	@Override public String toString() {
		return this.FUNCTION;
	}
}
