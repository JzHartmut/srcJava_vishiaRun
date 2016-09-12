package org.vishia.inspcPC;

import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Debugutil;
import org.vishia.util.ReplaceAlias_ifc;

public class InspcReplAlias implements ReplaceAlias_ifc
{

  Map<String, String> repl = new TreeMap<String, String>();
  
  /**It supports usage of an alias in the data path. See {@link #replaceDataPathPrefix(String)}.
   * @param src this map will added to the existing one.
   */
  @Override public void addDataReplace(Map<String, String> src)
  { repl.putAll(src);    
  }

  /**It supports usage of an alias in the data path. See {@link #replaceDataPathPrefix(String)}.
   * @param alias Any shorter alias
   * @param value The complete value.
   */
  @Override public void addDataReplace(String alias, String value)
  {
    repl.put(alias, value);    
  }

  /**It supports usage of an alias in the data path.
   * @param path may contain "alias:restOfPath"
   * @return if "alias" is found in {@link #addDataReplace(String, String)} the it is replaced
   *   inclusively ":". If alias is not found, it is not replaced.
   *   Note that another meaning of "prefix:restOfPath" is possible.
   */
  @Override public String replaceDataPathPrefix(String path)
  {
    if(repl == null) return path;
    int sep = path.indexOf(':');
    if(sep >0) {
      String st = repl.get(path.substring(0, sep));
      if(st !=null) { 
        return st + path.substring(sep+1);
      } else {
        return path;  //no replacement done.
      }
    }
    else return path;
  }
  
  
  @Override public String searchAliasForValue(final String path) {
    int maxPos = 0;
    String alias = null;
    for(Map.Entry<String, String> e: repl.entrySet()) {
      String repl = e.getValue();
      if(repl.charAt(repl.length()-1) == '.') {
        repl = repl.substring(0, repl.length()-1);  //without the '.'
      }
      if(path.startsWith(repl)) {
        if(repl.length() > maxPos) {
          maxPos = repl.length();
          alias = e.getKey();
        }
      }
    }
    if(alias == null) { alias = path; } //no replacement
    else {
      if(path.length() == maxPos) {
        alias += ':';
      }
      else {
        if(path.charAt(maxPos) == '.') { maxPos += 1; }
        else {
          Debugutil.stop();
        }
        alias += ':' + path.substring(maxPos);
      }
    }
    return alias;
  }
  

  

}
