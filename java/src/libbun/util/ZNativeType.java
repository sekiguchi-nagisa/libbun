package libbun.util;

import libbun.encode.jvm.JavaTypeTable;
import libbun.type.BType;

public class ZNativeType extends BType {
	@BField public Class<?>          JClass;

	public ZNativeType(Class<?> JType) {
		super(BType.UniqueTypeFlag, JType.getSimpleName(), null);
		this.JClass = JType;
	}

	@Override public BType GetSuperType() {
		return JavaTypeTable.GetBunType(this.JClass.getSuperclass());
	}

}
