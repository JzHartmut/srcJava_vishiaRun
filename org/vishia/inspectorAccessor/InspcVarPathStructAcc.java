package org.vishia.inspectorAccessor;

public class InspcVarPathStructAcc
{
  public final InspcTargetAccessor targetAccessor;
  public final String sPathInTarget;
  public final String sName;
  /**Path like it is given, maybe with Alias:PATH. */
  public final String sDataPath;
  public final InspcStruct itsStruct;
  
  public InspcVarPathStructAcc(InspcTargetAccessor targetAccessor, String sDataPath, String sPathInTarget, String sName, InspcStruct itsStruct)
  { this.targetAccessor = targetAccessor;
    this.sDataPath = sDataPath;
    this.sPathInTarget = sPathInTarget;
    this.sName = sName;
    this.itsStruct = itsStruct;
  }
  
  
}
