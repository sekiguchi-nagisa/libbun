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

package libbun.type;

import libbun.parser.BToken;
import libbun.util.BField;
import libbun.util.Var;
import libbun.util.BArray;
import libbun.util.BIgnored;

public class BVarType extends BType {

	@BField public final BArray<BVarType> VarList;
	@BField public BToken SourceToken;
	@BField public int GreekId;

	public BVarType(BArray<BVarType> VarList, String Name, BToken SourceToken) {
		super(0, Name, BType.VarType);
		this.VarList = VarList;
		this.SourceToken = SourceToken;
		this.GreekId = VarList.size();
		this.TypeId = this.RefType.TypeId;
		VarList.add(this);
	}

	@Override public final BType GetRealType() {
		return this.RefType;
	}

	@Override public int GetParamSize() {
		return this.RefType.GetParamSize();
	}

	@Override public BType GetParamType(int Index) {
		return this.RefType.GetParamType(Index);
	}

	@Override public boolean IsFuncType() {
		return this.RefType.IsFuncType();
	}

	@Override public boolean IsVarType() {
		return this.RefType.IsVarType();
	}

	@BIgnored @Override public String toString() {
		return "typeof(" + this.ShortName + "): " + this.RefType;
	}

	public void Infer(BType ContextType, BToken SourceToken) {
		if(this.RefType.IsVarType()) {
			if(ContextType instanceof BVarType && ContextType.IsVarType()) {
				@Var BVarType VarType = (BVarType)ContextType;
				if(this.GreekId < VarType.GreekId) {
					VarType.GreekId = this.GreekId;
				}
				else {
					this.GreekId = VarType.GreekId;
				}
			}
			else {
				this.RefType = ContextType.GetRealType();
				this.SourceToken = SourceToken;
				this.TypeId = this.RefType.TypeId;
				this.TypeFlag = this.RefType.TypeFlag;
			}
		}
	}

	@Override public void Maybe(BType T, BToken SourceToken) {
		if(this.RefType.IsVarType()) {
			if(T instanceof BVarType && T.IsVarType()) {
				@Var BVarType VarType = (BVarType)T;
				if(this.GreekId < VarType.GreekId) {
					VarType.GreekId = this.GreekId;
				}
				else {
					this.GreekId = VarType.GreekId;
				}
			}
			else {
				this.RefType = T.GetRealType();
				this.SourceToken = SourceToken;
				this.TypeId = T.TypeId;
				this.TypeFlag = T.TypeFlag;
			}
		}
	}

}
