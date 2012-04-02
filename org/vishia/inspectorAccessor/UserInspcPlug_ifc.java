package org.vishia.inspectorAccessor;

import org.vishia.gral.ifc.GralPlugUser2Gral_ifc;

/**This interface is the plug from a user plugin to the Inspc. 
 * 
 */
public interface UserInspcPlug_ifc extends GralPlugUser2Gral_ifc
{
  /**Replaces the prefix of the path with a possible replacement. 
   * @param path the path given in scripts. It may have the form PREFIX:PATH or TARGET:PATH
   * @param target A String[1] to return the target part. 
   * @return The path with dissolved prefix and dissolved target from path or prefix.
   */
  String replacePathPrefix(String path, String[] target); 
}
