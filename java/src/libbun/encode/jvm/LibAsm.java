package libbun.encode.jvm;

import libbun.type.BType;
import libbun.util.BArray;
import libbun.util.BBooleanArray;
import libbun.util.BFloatArray;
import libbun.util.BIntArray;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;


public class LibAsm {


	static Type AsmType(Class<?> jClass) {
		return Type.getType(jClass);
	}

	static Class<?> AsArrayClass(BType zType) {
		BType zParamType = zType.GetParamType(0);
		if(zParamType.IsBooleanType()) {
			return BBooleanArray.class;
		}
		if(zParamType.IsIntType()) {
			return BIntArray.class;
		}
		if(zParamType.IsFloatType()) {
			return BFloatArray.class;
		}
		return BArray.class;
	}

	static Class<?> AsElementClass(BType zType) {
		BType zParamType = zType.GetParamType(0);
		if(zParamType.IsBooleanType()) {
			return boolean.class;
		}
		if(zParamType.IsIntType()) {
			return long.class;
		}
		if(zParamType.IsFloatType()) {
			return double.class;
		}
		if(zParamType.IsStringType()) {
			return String.class;
		}
		return Object.class;
	}

	static String NewArrayDescriptor(BType ArrayType) {
		BType zParamType = ArrayType.GetParamType(0);
		if(zParamType.IsBooleanType()) {
			return Type.getMethodDescriptor(AsmType(void.class), new Type[] {AsmType(int.class), AsmType(boolean[].class)});
		}
		if(zParamType.IsIntType()) {
			return Type.getMethodDescriptor(AsmType(void.class), new Type[] {AsmType(int.class), AsmType(long[].class)});
		}
		if(zParamType.IsFloatType()) {
			return Type.getMethodDescriptor(AsmType(void.class), new Type[] {AsmType(int.class), AsmType(double[].class)});
		}
		if(zParamType.IsStringType()) {
			return Type.getMethodDescriptor(AsmType(void.class), new Type[] {AsmType(int.class), AsmType(String[].class)});
		}

		return Type.getMethodDescriptor(AsmType(void.class), new Type[] {AsmType(int.class), AsmType(Object[].class)});
	}

	static void PushInt(MethodNode Asm, int n) {
		switch(n) {
		case -1: Asm.visitInsn(Opcodes.ICONST_M1); return;
		case 0: Asm.visitInsn(Opcodes.ICONST_0); return;
		case 1: Asm.visitInsn(Opcodes.ICONST_1); return;
		case 2: Asm.visitInsn(Opcodes.ICONST_2); return;
		case 3: Asm.visitInsn(Opcodes.ICONST_3); return;
		case 4: Asm.visitInsn(Opcodes.ICONST_4); return;
		case 5: Asm.visitInsn(Opcodes.ICONST_5); return;
		default:
			if(n >= Byte.MIN_VALUE && n <= Byte.MAX_VALUE) {
				Asm.visitIntInsn(Opcodes.BIPUSH, n);
			}
			else if(n >= Short.MIN_VALUE && n <= Short.MAX_VALUE) {
				Asm.visitIntInsn(Opcodes.SIPUSH, n);
			}
			else {
				Asm.visitLdcInsn(n);
			}
		}
	}


}
