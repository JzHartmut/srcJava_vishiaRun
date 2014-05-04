package org.vishia.inspectorAccessor;

//import org.vishia.gral.ifc.GralPlugUser2Gral_ifc;

/**This interface is the plug from a user plugin to the Inspc. 
 * 
 */
public interface UserInspcPlug_ifc // extends GralPlugUser2Gral_ifc
{
  /**Version, history and license
   * <ul>
   * <li>2012-04-07 Hartmut It is not possible to extend GralPlugUser2Gral_ifc because that is
   *   in an non-dependent component. 
   * <li>2012-04-01 Hartmut removed from the component srcJava_vishiaGUI to this component.
   * <li>2011-08-00 Hartmut created to adapt some user classes without link on compile time.
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
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  public static final int version = 20120409;

  /**Replaces the prefix of the path with a possible replacement. 
   * @param path the path given in scripts. It may have the form PREFIX:PATH or TARGET:PATH
   * @param target A String[1] to return the target part. 
   * @return The path with dissolved prefix and dissolved target from path or prefix.
   */
  String XXXreplacePathPrefix(String path, String[] target); 
  
  
  InspcVarPathStructAcc getTargetFromPath(String sDataPath);

  
}
