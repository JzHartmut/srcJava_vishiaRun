package org.vishia.inspectorAccessor;

public class InspcVarPathStructAcc extends InspcTargetAccess
{
  public final InspcStruct itsStruct;
  
  public InspcVarPathStructAcc(InspcTargetAccessor targetAccessor, String sDataPath, String sPathInTarget, String sName, InspcStruct itsStruct)
  { super(targetAccessor, sDataPath, sPathInTarget, "", sName);
    this.itsStruct = itsStruct;
  }
  
  
}
