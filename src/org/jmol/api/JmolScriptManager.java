package org.jmol.api;



import org.jmol.util.BS;
import org.jmol.util.JmolList;
import org.jmol.viewer.Viewer;

public interface JmolScriptManager {

  void setViewer(Viewer viewer);
  
  void startCommandWatcher(boolean isStart);

  void clear(boolean isAll);

  void clearQueue();
  
  boolean isScriptQueued();
  
  void waitForQueue();

  JmolList<JmolList<Object>> getScriptQueue();

  void queueThreadFinished(int pt);

  JmolList<Object> getScriptItem(boolean b, boolean startedByCommandThread);

  String evalStringQuietSync(String strScript, boolean isQuiet,
                             boolean allowSyncScript);

  Object evalStringWaitStatusQueued(String returnType, String strScript,
                                           String statusList,
                                           boolean isScriptFile,
                                           boolean isQuiet, boolean isQueued);

  String addScript(String strScript, boolean isScriptFile, boolean isQuiet);

  boolean checkHalt(String str, boolean isInsert);

  BS getAtomBitSetEval(JmolScriptEvaluator eval, Object atomExpression);

  Object scriptCheckRet(String strScript, boolean returnContext);

  JmolScriptEvaluator getEval();

}

