package org.jmol.api;


import java.util.Map;

import org.jmol.modelset.Atom;
import org.jmol.script.ScriptException;

import org.jmol.script.ScriptContext;
import org.jmol.script.SV;
import org.jmol.util.BS;
import org.jmol.util.JmolList;
import org.jmol.util.P3;
import org.jmol.util.SB;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public interface JmolScriptEvaluator {

  JmolScriptEvaluator setViewer(Viewer viewer);

  ScriptContext getThisContext();

  void pushContextDown();

  void resumeEval(ScriptContext sc);

  boolean getAllowJSThreads();

  void setCompiler();

  BS getAtomBitSet(Object atomExpression);

  boolean isStopped();

  void notifyResumeStatus();

  JmolList<Integer> getAtomBitSetVector(int atomCount, Object atomExpression);

  boolean isPaused();

  String getNextStatement();

  void resumePausedExecution();

  void stepPausedExecution();

  void pauseExecution(boolean b);

  boolean isExecuting();

  void haltExecution();

  boolean compileScriptFile(String strScript, boolean isQuiet);

  boolean compileScriptString(String strScript, boolean isQuiet);

  String getErrorMessage();

  String getErrorMessageUntranslated();

  ScriptContext checkScriptSilent(String strScript);

  String getScript();

  void setDebugging();

  boolean isStepping();

  ScriptContext getScriptContext();

  Object evaluateExpression(Object stringOrTokens, boolean asVariable);

  void deleteAtomsInVariables(BS bsDeleted);

  Map<String, SV> getContextVariables();

  boolean evaluateParallel(ScriptContext context, ShapeManager shapeManager);

  void runScript(String script) throws ScriptException;

  void runScriptBuffer(String string, SB outputBuffer) throws ScriptException;

  float evalFunctionFloat(Object func, Object params, float[] values);

  void setException(ScriptException sx, String msg, String untranslated);

  BS addHydrogensInline(BS bsAtoms, JmolList<Atom> vConnections, P3[] pts) throws Exception;

  void evaluateCompiledScript(boolean isSyntaxCheck,
                              boolean isSyntaxAndFileCheck,
                              boolean historyDisabled, boolean listCommands,
                              SB outputBuffer, boolean allowThreads);

}
