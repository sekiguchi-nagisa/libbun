// ***************************************************************************
// Copyright (c) 2013, JST/CREST DEOS project authors. All rights reserved.
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

package libbun.encode.jvm;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import libbun.type.BFuncType;
import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.util.BunMap;
import libbun.util.LibBunSystem;
import libbun.util.Var;
import libbun.util.BArray;
import libbun.util.BBooleanArray;
import libbun.util.BFloatArray;
import libbun.util.BFunction;
import libbun.util.BIntArray;
import libbun.util.ZNativeType;
import libbun.util.ZObjectArray;
import libbun.util.BStringArray;


public class JavaTypeTable {
	static HashMap<String, Class<?>> ClassMap = new HashMap<String,Class<?>>();
	static HashMap<String, BType> TypeMap = new HashMap<String,BType>();

	static {
		JavaTypeTable.SetTypeTable(BType.VarType, Object.class);
		JavaTypeTable.SetTypeTable(BType.VoidType, void.class);
		JavaTypeTable.SetTypeTable(BType.BooleanType, boolean.class);
		JavaTypeTable.SetTypeTable(BType.IntType, long.class);
		JavaTypeTable.SetTypeTable(BType.FloatType, double.class);
		JavaTypeTable.SetTypeTable(BType.StringType, String.class);
		JavaTypeTable.SetTypeTable(BFuncType._FuncType, BFunction.class);
		JavaTypeTable.SetTypeTable(BGenericType._ArrayType, ZObjectArray.class);
		JavaTypeTable.SetTypeTable(BGenericType._MapType, BunMap.class);

		BType BooleanArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.BooleanType);
		BType IntArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.IntType);
		BType FloatArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.FloatType);
		BType StringArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.StringType);
		JavaTypeTable.SetTypeTable(BooleanArrayType, BBooleanArray.class);
		JavaTypeTable.SetTypeTable(IntArrayType, BIntArray.class);
		JavaTypeTable.SetTypeTable(FloatArrayType, BFloatArray.class);
		JavaTypeTable.SetTypeTable(StringArrayType, BStringArray.class);

		JavaTypeTable.SetTypeTable(BType.BooleanType, Boolean.class);
		JavaTypeTable.SetTypeTable(BType.IntType, Long.class);
		JavaTypeTable.SetTypeTable(BType.FloatType, Double.class);
		JavaTypeTable.SetTypeTable(BType.IntType, int.class);
		JavaTypeTable.SetTypeTable(BType.IntType, Integer.class);
		JavaTypeTable.SetTypeTable(BType.IntType, short.class);
		JavaTypeTable.SetTypeTable(BType.IntType, Short.class);
		JavaTypeTable.SetTypeTable(BType.IntType, byte.class);
		JavaTypeTable.SetTypeTable(BType.IntType, Byte.class);
		JavaTypeTable.SetTypeTable(BType.FloatType, float.class);
		JavaTypeTable.SetTypeTable(BType.FloatType, Float.class);
		JavaTypeTable.SetTypeTable(BType.StringType, char.class);
		JavaTypeTable.SetTypeTable(BType.StringType, Character.class);
	}

	public static void SetTypeTable(BType zType, Class<?> c) {
		if(JavaTypeTable.ClassMap.get(zType.GetUniqueName()) == null) {
			JavaTypeTable.ClassMap.put(zType.GetUniqueName(), c);
		}
		JavaTypeTable.TypeMap.put(c.getCanonicalName(), zType);
	}

	public static Class<?> GetJavaClass(BType zType, Class<?> Default) {
		Class<?> jClass = JavaTypeTable.ClassMap.get(zType.GetUniqueName());
		if(jClass == null) {
			jClass = JavaTypeTable.ClassMap.get(zType.GetBaseType().GetUniqueName());
			if(jClass == null) {
				jClass = Default;
			}
		}
		return jClass;
	}

	public static BType GetBunType(Class<?> JavaClass) {
		BType NativeType = JavaTypeTable.TypeMap.get(JavaClass.getCanonicalName());
		if (NativeType == null) {
			NativeType = new ZNativeType(JavaClass);
			JavaTypeTable.SetTypeTable(NativeType, JavaClass);
		}
		return NativeType;
	}

	public final static BFuncType ConvertToFuncType(Method JMethod) {
		@Var Class<?>[] ParamTypes = JMethod.getParameterTypes();
		@Var BArray<BType> TypeList = new BArray<BType>(new BType[LibBunSystem._Size(ParamTypes) + 2]);
		if (!Modifier.isStatic(JMethod.getModifiers())) {
			TypeList.add(JavaTypeTable.GetBunType(JMethod.getDeclaringClass()));
		}
		if (ParamTypes != null) {
			@Var int j = 0;
			while(j < ParamTypes.length) {
				TypeList.add(JavaTypeTable.GetBunType(ParamTypes[j]));
				j = j + 1;
			}
		}
		TypeList.add(JavaTypeTable.GetBunType(JMethod.getReturnType()));
		return BTypePool._LookupFuncType2(TypeList);
	}

	public final static BFuncType FuncType(Class<?> ReturnT, Class<?> ... paramsT) {
		@Var BArray<BType> TypeList = new BArray<BType>(new BType[10]);
		for(Class<?> C : paramsT) {
			TypeList.add(JavaTypeTable.GetBunType(C));
		}
		TypeList.add(JavaTypeTable.GetBunType(ReturnT));
		return BTypePool._LookupFuncType2(TypeList);
	}

}
