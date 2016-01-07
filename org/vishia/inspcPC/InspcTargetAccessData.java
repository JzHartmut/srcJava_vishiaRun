package org.vishia.inspcPC;



/**This class assembles all data to access the target for any thing.
 * It contains only data, no functionality. It is used especially for orders of the {@link  org.vishia.inspcPC.accTarget.InspcTargetAccessor}
 * @author Hartmut Schorrig
 *
 */
public class InspcTargetAccessData
{
  /**Version, history and license.
   * <ul>
   * <li>2015-03-21 Hartmut created. It contains all data which was stored in the now deprecated InspcVarPathStructAcc
   *   but not the InspcStruct. Minimize Dependency in {@link  org.vishia.inspcPC.accTarget.InspcTargetAccessor}.  
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final String version = "2015-03-21";

  
  /**Instance of the target accessor gotten from prefix "Target:..." */
  public final InspcAccess_ifc targetAccessor;
  //public final InspcTargetAccessor targetAccessor;
  
  /**Path how it is necessary in the target. Especially without prefix "Target:..." */
  public final String sPathInTarget;
  
  /**Name of the field, only to show. */
  public final String sName;

  /**Path of the structure which contains the variable. */
  public final String sParentPath;
  
  /**Path like it is given, maybe with "Alias:...". */
  public final String sDataPath;

  public InspcTargetAccessData(InspcAccess_ifc targetAccessor, String sDataPath, String sPathInTarget, String sParentPath, String sName)
  { this.targetAccessor = targetAccessor;
    this.sDataPath = sDataPath;
    this.sPathInTarget = sPathInTarget;
    this.sParentPath = sParentPath;
    this.sName = sName;
  }
  
}
