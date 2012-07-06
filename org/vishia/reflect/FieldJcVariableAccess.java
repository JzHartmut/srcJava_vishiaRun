package org.vishia.reflect;

import java.lang.reflect.Field;

import org.vishia.byteData.VariableAccess_ifc;

public class FieldJcVariableAccess implements VariableAccess_ifc
{

  /**Version, history and license.
   * <ul>
   * <li>2012-04-22 Hartmut adapt: {@link #requestValue(long)} etc. from {@link VariableAccess_ifc}.
   * <li>2010-06-00 Hartmut created. See {@link InspcMng#version}. 
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
  public static final int version = 20120705;

  
  public FieldJcVariableAccess(Object instance, FieldJc theField){
    
  }

  
  
  @Override
  public int getDimension(int dimension) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getDouble(int... ixArray) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float getFloat(int... ixArray) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getInt(int... ixArray) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long getLastRefreshTime() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getString(int... ixArray) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public char getType() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void requestValue(long timeRequested) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public double setDouble(double value, int... ixArray) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public float setFloat(float value, int... ixArray) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int setInt(int value, int... ixArray) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String setString(String value, int... ixArray) {
    // TODO Auto-generated method stub
    return null;
  }


}
