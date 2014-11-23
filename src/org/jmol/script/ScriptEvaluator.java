/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.script;

import java.io.IOException;
import java.io.OutputStream;
import org.jmol.util.JmolList;

import java.util.Hashtable;

import java.util.Map;


import org.jmol.api.JmolScriptEvaluator;
import org.jmol.api.JmolScriptFunction;
import org.jmol.api.MinimizerInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.constant.EnumAnimationMode;
import org.jmol.constant.EnumPalette;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumStereoMode;
import org.jmol.constant.EnumVdw;
import org.jmol.i18n.GT;
import org.jmol.io.JmolBinary;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.ModelCollection;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.modelset.ModelCollection.StateScript;
import org.jmol.shape.MeshCollection;
import org.jmol.shape.Object2d;
import org.jmol.shape.Shape;
import org.jmol.thread.ScriptDelayThread;
import org.jmol.util.BSUtil;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;

import org.jmol.util.ArrayUtil;
import org.jmol.util.AxisAngle4f;
import org.jmol.util.BS;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.ColorUtil;
import org.jmol.util.Elements;
import org.jmol.util.JmolFont;
import org.jmol.util.GData;
import org.jmol.util.JmolEdge;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.Measure;
import org.jmol.util.MeshSurface;
import org.jmol.util.Parser;
import org.jmol.util.P3;
import org.jmol.util.Point3fi;
import org.jmol.util.Point4f;
import org.jmol.util.Quaternion;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.Tuple3f;
import org.jmol.util.V3;
import org.jmol.modelset.TickInfo;
import org.jmol.api.JmolParallelProcessor;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.Viewer;
import org.jmol.viewer.Viewer.ACCESS;

public class ScriptEvaluator implements JmolScriptEvaluator {

  /*
   * The ScriptEvaluator class, the Viewer, the xxxxManagers, the Graphics3D
   * rendering engine, the ModelSet and Shape classes, and the Adapter file
   * reader classes form the core of the Jmol molecular visualization framework.
   * 
   * The ScriptEvaluator has just a few entry points, which you will find
   * immediately following this comment. They include:
   * 
   * public boolean compileScriptString(String script, boolean tQuiet)
   * 
   * public boolean compileScriptFile(String filename, boolean tQuiet)
   * 
   * public void evaluateCompiledScript(boolean isCmdLine_c_or_C_Option, boolean
   * isCmdLine_C_Option, boolean historyDisabled, boolean listCommands)
   * 
   * Essentially ANYTHING can be done using these three methods. A variety of
   * other methods are available via Viewer, which is the the true portal to
   * Jmol (via the JmolViewer interface) for application developers who want
   * faster, more direct processing.
   * 
   * A little Jmol history:
   * 
   * General history notes can be found at our ConfChem paper, which can be
   * found at
   * http://chemapps.stolaf.edu/jmol/presentations/confchem2006/jmol-confchem
   * .htm
   * 
   * This ScriptEvaluator class was initially written by Michael (Miguel) Howard
   * as Eval.java as an efficient means of reproducing the RasMol scripting
   * language for Jmol. Key additions there included:
   * 
   * - tokenization of commands via the Compiler class (now ScriptCompiler and
   * ScriptCompilationTokenParser) - ScriptException error handling - a flexible
   * yet structured command parameter syntax - implementations of RasMol
   * secondary structure visualizations - isosurfaces, dots, labels, polyhedra,
   * draw, stars, pmesh, more
   * 
   * Other Miguel contributions include:
   * 
   * - the structural bases of the Adapter, ModelSet, and ModelSetBio classes -
   * creation of Manager classes - absolutely amazing raw pixel bitmap rendering
   * code (org.jmol.g3d) - popup context menu - inline model loading
   * 
   * Bob Hanson (St. Olaf College) found out about Jmol during the spring of
   * 2004. After spending over a year working on developing online interactive
   * documentation, he started actively writing code early in 2006. During the
   * period 2006-2009 Bob completely reworked the script processor (and much of
   * the rest of Jmol) to handle a much broader range of functionality. Notable
   * improvements include:
   * 
   * - display/hide commands - dipole, ellipsoid, geosurface, lcaoCartoon
   * visualizations - quaternion and ramachandran commands - much expanded
   * isosurface / draw commands - configuration, disorder, and biomolecule
   * support - broadly 2D- and 3D-positionable echos - translateSelected and
   * rotateSelected commands - getProperty command, providing access to more
   * file information - data and write commands - writing of high-resolution
   * JPG, PNG, and movie-sequence JPG - generalized export to Maya and PovRay
   * formats
   * 
   * - multiple file loading, including trajectories - minimization using the
   * Universal Force Field (UFF) - atom/model deletion and addition - direct
   * loading of properties such as partial charge or coordinates - several new
   * file readers, including manifested zip file reading - default directory, CD
   * command, and pop-up file open/save dialogs
   * 
   * - "internal" molecular coordinate-based rotations - full support for
   * crystallographic formats, including space groups, symmetry, unit cells, and
   * fractional coordinates - support for point groups and molecular symmetry -
   * navigation mode - antialiasing of display and imaging - save/restore/write
   * exact Jmol state - JVXL file format for compressed rapid generation of
   * isosurfaces
   * 
   * - user-defined variables - addition of a Reverse Polish Notation (RPN)
   * expression processor - extension of the RPN processor to user variables -
   * user-defined functions - flow control commands if/else/endif, for, and
   * while - JavaScript/Java-like brace syntax - key stroke-by-key stroke
   * command syntax checking - integrated help command - user-definable popup
   * menu - language switching
   * 
   * - fully functional signed applet - applet-applet synchronization, including
   * two-applet geoWall stereo rendering - JSON format for property delivery to
   * JavaScript - jmolScriptWait, dual-threaded queued JavaScript scripting
   * interface - extensive callback development - script editor panel (work in
   * progress, June 2009)
   * 
   * Several other people have contributed. Perhaps they will not be too shy to
   * add their claim to victory here. Please add your contributions.
   * 
   * - Jmol application (Egon Willighagen) - smiles support (Nico Vervelle) -
   * readers (Rene Kanter, Egon, several others) - initial VRML export work
   * (Nico Vervelle) - WebExport (Jonathan Gutow) - internationalization (Nico,
   * Egon, Angel Herriez) - Jmol Wiki and user guide book (Angel Herriez)
   * 
   * While this isn't necessarily the best place for such discussion, open
   * source principles require proper credit given to those who have
   * contributed. This core class seems to me a place to acknowledge this core
   * work of the Jmol team.
   * 
   * Bob Hanson, 6/2009 hansonr@stolaf.edu
   */

  private boolean allowJSThreads = true;
  
  public boolean getAllowJSThreads() {
    return allowJSThreads;
  }

  private boolean listCommands;
  private boolean isJS;

  public JmolScriptEvaluator setViewer(Viewer viewer) {
    this.viewer = viewer;
    this.compiler = (compiler == null ? (ScriptCompiler) viewer.compiler : compiler);
    isJS = viewer.isSingleThreaded;
    definedAtomSets = viewer.definedAtomSets;
    return this;
  }

  public ScriptEvaluator() {
    currentThread = Thread.currentThread();
  }

  public void setCompiler() {
    viewer.compiler = compiler = new ScriptCompiler(viewer);
  }

  // //////////////// primary interfacing methods //////////////////

  /*
   * see Viewer.evalStringWaitStatus for how these are implemented
   */
  public boolean compileScriptString(String script, boolean tQuiet) {
    clearState(tQuiet);
    contextPath = "[script]";
    return compileScript(null, script, debugScript);
  }

  public boolean compileScriptFile(String filename, boolean tQuiet) {
    clearState(tQuiet);
    contextPath = filename;
    return compileScriptFileInternal(filename, null, null, null);
  }

  public void evaluateCompiledScript(boolean isCmdLine_c_or_C_Option,
                                     boolean isCmdLine_C_Option,
                                     boolean historyDisabled,
                                     boolean listCommands,
                                     SB outputBuffer, 
                                     boolean allowThreads) {
    boolean tempOpen = this.isCmdLine_C_Option;
    this.isCmdLine_C_Option = isCmdLine_C_Option;
    executionStopped = executionPaused = false;
    executionStepping = false;
    executing = true;
    chk = this.isCmdLine_c_or_C_Option = isCmdLine_c_or_C_Option;
    timeBeginExecution = System.currentTimeMillis();
    this.historyDisabled = historyDisabled;
    this.outputBuffer = outputBuffer;
    currentThread = Thread.currentThread();
    this.allowJSThreads = allowThreads;
    this.listCommands = listCommands;
    startEval();
    this.isCmdLine_C_Option = tempOpen;
    viewer.setStateScriptVersion(null); // set by compiler
  }

  /**
   * for the ISOSURFACE command
   * 
   * @param fname
   * @param xyz
   * @param ret
   * @return [ ScriptFunction, Params ]
   */
  private Object[] createFunction(String fname, String xyz, String ret) {
    ScriptEvaluator e = (new ScriptEvaluator());
    e.setViewer(viewer);
    try {
      e.compileScript(null, "function " + fname + "(" + xyz + ") { return "
          + ret + "}", false);
      JmolList<SV> params = new  JmolList<SV>();
      for (int i = 0; i < xyz.length(); i += 2)
        params.addLast(SV.newVariable(T.decimal, Float.valueOf(0f)).setName(
            xyz.substring(i, i + 1)));
      return new Object[] { e.aatoken[0][1].value, params };
    } catch (Exception ex) {
      return null;
    }
  }

  private boolean useThreads() {
    return (!viewer.autoExit && viewer.haveDisplay && outputBuffer == null);
  }
  
  private void startEval() {
    viewer.pushHoldRepaintWhy("runEval");
    setScriptExtensions();
    executeCommands(false);
  }

  private void executeCommands(boolean isTry) {
    boolean haveError = false;
    try {
      if (!dispatchCommands(false, false))
        return;
    } catch (Error er) {
      viewer.handleError(er, false);
      setErrorMessage("" + er + " " + viewer.getShapeErrorState());
      errorMessageUntranslated = "" + er;
      scriptStatusOrBuffer(errorMessage);
      haveError = true;
    } catch (ScriptException e) {
      if (e instanceof ScriptInterruption) {
        // ScriptInterruption will be called in JavaScript to 
        // stop this thread and initiate a setTimeout sequence 
        // that is responsible for getting us back to the
        // current point using resumeEval again.
        // it's not a real exception, but it has that 
        // property so that it can be caught here.
        return;
      }
      if (isTry) {
        viewer.setStringProperty("_errormessage", "" + e);
        return;
      }
      setErrorMessage(e.toString());
      errorMessageUntranslated = e.getErrorMessageUntranslated();
      scriptStatusOrBuffer(errorMessage);
      viewer.notifyError((errorMessage != null
          && errorMessage.indexOf("java.lang.OutOfMemoryError") >= 0 ? "Error"
          : "ScriptException"), errorMessage, errorMessageUntranslated);
      haveError = true;
    }
    if (haveError || !isJS || !allowJSThreads) {
      viewer.setTainted(true);
      viewer.popHoldRepaintWhy("runEval");
    }
    timeEndExecution = System.currentTimeMillis();
    if (errorMessage == null && executionStopped)
      setErrorMessage("execution interrupted");
    else if (!tQuiet && !chk)
      viewer.scriptStatus(JC.SCRIPT_COMPLETED);
    executing = chk = this.isCmdLine_c_or_C_Option = this.historyDisabled = false;
    String msg = getErrorMessageUntranslated();
    viewer.setErrorMessage(errorMessage, msg);
    if (!tQuiet)
      viewer.setScriptStatus("Jmol script terminated", errorMessage,
          1 + getExecutionWalltime(), msg);
  }

  /**
   * From dispatchCommands and JmolThread resumeEval.
   * 
   * After throwing a ScriptInterruption, all statements following the current
   * one are lost. When a JavaScript timeout returns from a DELAY, MOVE, MOVETO,
   * or other sleep-requiring command, it is the ScriptContext that contains all
   * the required information for restarting that "thread". In Java, we don't
   * have to worry about this, because the current thread is just put to sleep,
   * not stopped, but in JavaScript, where we only have one thread, we need to 
   * manage this more carefully.
   * 
   * We re-enter the halted script here, using a saved script context. The
   * program counter is incremented to skip the initiating statement, and all
   * parent contexts up the line are set with mustResumeEval = true.
   * 
   * @param sc
   */
  
  public void resumeEval(ScriptContext sc) {

    // 
    //
    //      |
    //      |
    //     INTERRUPT---
    //     (1)         |
    //      |          |
    //      |          |
    //      |      INTERRUPT----------------     
    //      |         (2)                   |     
    //      |          |                    |      
    //      |          |                    |
    //      |     resumeEval-->(1)     MOVETO_INIT
    //   (DONE)           
    //                                 (new thread) 
    //                                 MOVETO_FINISH
    //                                      |
    //                                 resumeEval-->(2)
    //                                   
    //  In Java, this is one overall thread that sleeps
    //  during the MOVETO. But in JavaScript, the setTimeout()
    //  starts a new thread and (1) and (2) are never executed.
    //  We must run resumeEval at the end of each dispatch loop.
    //
    //  Thus, it is very important that nothing is ever executed 
    //  after dispatchCommands.
    //

    setErrorMessage(null);
    if (executionStopped || sc == null || !sc.mustResumeEval) {
      viewer.setTainted(true);
      viewer.popHoldRepaintWhy("runEval");
      viewer.queueOnHold = false;
      return;
    }
    if (!executionPaused)
      sc.pc++;
    thisContext = sc;
    if (sc.scriptLevel > 0)
      scriptLevel = sc.scriptLevel - 1;
    restoreScriptContext(sc, true, false, false);
    executeCommands(sc.isTryCatch);
  }

  /**
   * runs a script and sends selected output to a provided StringXBuilder
   * 
   * @param script
   * @param outputBuffer
   * @throws ScriptException
   */
  public void runScriptBuffer(String script, SB outputBuffer)
      throws ScriptException {
    pushContext(null);
    contextPath += " >> script() ";
    this.outputBuffer = outputBuffer;
    allowJSThreads = false;
    if (compileScript(null, script + JC.SCRIPT_EDITOR_IGNORE, false))
      dispatchCommands(false, false);
    popContext(false, false);
  }

  /**
   * a method for just checking a script
   * 
   * @param script
   * @return a ScriptContext that indicates errors and provides a tokenized
   *         version of the script that has passed all syntax checking, both in
   *         the compiler and the evaluator
   * 
   */
  public ScriptContext checkScriptSilent(String script) {
    ScriptContext sc = compiler.compile(null, script, false, true, false, true);
    if (sc.errorType != null)
      return sc;
    restoreScriptContext(sc, false, false, false);
    chk = true;
    isCmdLine_c_or_C_Option = isCmdLine_C_Option = false;
    pc = 0;
    try {
      dispatchCommands(false, false);
    } catch (ScriptException e) {
      setErrorMessage(e.toString());
      sc = getScriptContext();
    }
    chk = false;
    return sc;
  }

  static SB getContextTrace(ScriptContext sc, SB sb, boolean isTop) {
    if (sb == null)
      sb = new SB();
    sb.append(setErrorLineMessage(sc.functionName, sc.scriptFileName,
        sc.lineNumbers[sc.pc], sc.pc, ScriptEvaluator.statementAsString(sc.statement, (isTop ? sc.iToken : 9999), false)));
    if (sc.parentContext != null)
      getContextTrace(sc.parentContext, sb, false);
    return sb;
  }

  // //////////////////////// script execution /////////////////////

  private boolean tQuiet;
  protected boolean chk;
  private boolean isCmdLine_C_Option;
  protected boolean isCmdLine_c_or_C_Option;
  private boolean historyDisabled;
  protected boolean logMessages;
  private boolean debugScript;

  public void setDebugging() {
    debugScript = viewer.getDebugScript();
    logMessages = (debugScript && Logger.debugging);
  }

  private boolean executionStopped;
  private boolean executionPaused;
  private boolean executionStepping;
  private boolean executing;

  private long timeBeginExecution;
  private long timeEndExecution;

  private boolean mustResumeEval;  // see resumeEval

  private int getExecutionWalltime() {
    return (int) (timeEndExecution - timeBeginExecution);
  }

  public void haltExecution() {
    resumePausedExecution();
    executionStopped = true;
  }

  public void pauseExecution(boolean withDelay) {
    if (chk || viewer.isHeadless())
      return;
    if (withDelay && !isJS )
      viewer.delayScript(this, -100);
    viewer.popHoldRepaintWhy("pauseExecution");
    executionStepping = false;
    executionPaused = true;
  }

  public void stepPausedExecution() {
    executionStepping = true;
    executionPaused = false;
    // releases a paused thread but
    // sets it to pause for the next command.
  }

  public void resumePausedExecution() {
    executionPaused = false;
    executionStepping = false;
  }

  public boolean isExecuting() {
    return executing && !executionStopped;
  }

  public boolean isPaused() {
    return executionPaused;
  }

  public boolean isStepping() {
    return executionStepping;
  }

  public boolean isStopped() {
    return executionStopped || !isJS && currentThread != Thread.currentThread();
  }

  /**
   * when paused, indicates what statement will be next
   * 
   * @return a string indicating the statement
   */
  public String getNextStatement() {
    return (pc < aatoken.length ? setErrorLineMessage(functionName, scriptFileName,
        getLinenumber(null), pc, statementAsString(aatoken[pc], -9999,
            logMessages)) : "");
  }

  /**
   * used for recall of commands in the application console
   * 
   * @param pc
   * @param allThisLine
   * @param addSemi
   * @return a string representation of the command
   */
  private String getCommand(int pc, boolean allThisLine, boolean addSemi) {
    if (pc >= lineIndices.length)
      return "";
    if (allThisLine) {
      int pt0 = -1;
      int pt1 = script.length();
      for (int i = 0; i < lineNumbers.length; i++)
        if (lineNumbers[i] == lineNumbers[pc]) {
          if (pt0 < 0)
            pt0 = lineIndices[i][0];
          pt1 = lineIndices[i][1];
        } else if (lineNumbers[i] == 0 || lineNumbers[i] > lineNumbers[pc]) {
          break;
        }
      if (pt1 == script.length() - 1 && script.endsWith("}"))
        pt1++;
      return (pt0 == script.length() || pt1 < pt0 ? "" : script.substring(Math
          .max(pt0, 0), Math.min(script.length(), pt1)));
    }
    int ichBegin = lineIndices[pc][0];
    int ichEnd = lineIndices[pc][1];
    // (pc + 1 == lineIndices.length || lineIndices[pc + 1][0] == 0 ? script
    // .length()
    // : lineIndices[pc + 1]);
    String s = "";
    if (ichBegin < 0 || ichEnd <= ichBegin || ichEnd > script.length())
      return "";
    try {
      s = script.substring(ichBegin, ichEnd);
      if (s.indexOf("\\\n") >= 0)
        s = TextFormat.simpleReplace(s, "\\\n", "  ");
      if (s.indexOf("\\\r") >= 0)
        s = TextFormat.simpleReplace(s, "\\\r", "  ");
      // int i;
      // for (i = s.length(); --i >= 0 && !ScriptCompiler.eol(s.charAt(i), 0);
      // ){
      // }
      // s = s.substring(0, i + 1);
      if (s.length() > 0 && !s.endsWith(";")/*
                                             * && !s.endsWith("{") &&
                                             * !s.endsWith("}")
                                             */)
        s += ";";
    } catch (Exception e) {
      Logger.error("darn problem in Eval getCommand: ichBegin=" + ichBegin
          + " ichEnd=" + ichEnd + " len = " + script.length() + "\n" + e);
    }
    return s;
  }

  private void logDebugScript(int ifLevel) {
    if (logMessages) {
      if (st.length > 0)
        Logger.debug(st[0].toString());
      for (int i = 1; i < slen; ++i)
        Logger.debug(st[i].toString());
    }
    iToken = -9999;
    if (logMessages) {
      SB strbufLog = new SB();
      String s = (ifLevel > 0 ? "                          ".substring(0,
          ifLevel * 2) : "");
      strbufLog.append(s).append(
          statementAsString(st, iToken, logMessages));
      viewer.scriptStatus(strbufLog.toString());
    } else {
      String cmd = getCommand(pc, false, false);
      if (cmd != "")
        viewer.scriptStatus(cmd);
    }

  }

  // /////////////// string-based evaluation support /////////////////////

  private final static String EXPRESSION_KEY = "e_x_p_r_e_s_s_i_o_n";

  /**
   * a general-use method to evaluate a "SET" type expression.
   * 
   * @param expr
   * @param asVariable
   * @return an object of one of the following types: Boolean, Integer, Float,
   *         String, Point3f, BitSet
   */

  public Object evaluateExpression(Object expr, boolean asVariable) {
    // Text.formatText for MESSAGE and ECHO
    // prior to 12.[2/3].32 was not thread-safe for compilation.
    ScriptEvaluator e = (ScriptEvaluator) (new ScriptEvaluator()).setViewer(viewer);
    try {
      // disallow end-of-script message and JavaScript script queuing
      e.pushContext(null);
      e.allowJSThreads = false;
    } catch (ScriptException e1) {
      //ignore
    }
    return (e.evaluate(expr, asVariable));
  }
  
  private Object evaluate(Object expr, boolean asVariable) {
    try {
      if (expr instanceof String) {
        if (compileScript(null, EXPRESSION_KEY + " = " + expr, false)) {
          contextVariables = viewer.getContextVariables();
          setStatement(0);
          return (asVariable ? parameterExpressionList(2, -1, false).get(0) : parameterExpressionString(2, 0));
        }
      } else if (expr instanceof T[]) {
        contextVariables = viewer.getContextVariables();
        BS bs = atomExpression((T[]) expr, 0, 0, true, false, true, false);
        return (asVariable ? SV.newScriptVariableBs(bs, -1): bs);
          
      }
    } catch (Exception ex) {
      Logger.error("Error evaluating: " + expr + "\n" + ex);
    }
    return (asVariable ? SV.getVariable("ERROR") : "ERROR");
  }

  ShapeManager sm;

  /**
   * used for TRY command
   * 
   * @param context
   * @param shapeManager
   * @return  true if successful; false if not
   */
  public boolean evaluateParallel(ScriptContext context,
                                        ShapeManager shapeManager) {
    ScriptEvaluator e = new ScriptEvaluator();
    e.setViewer(viewer);
    e.historyDisabled = true;
    e.compiler = new ScriptCompiler(viewer);
    e.sm = shapeManager;
    try {
      e.restoreScriptContext(context, true, false, false);
      // TODO: This will disallow some motion commands
      //       within a TRY/CATCH block in JavaScript, and
      //       the code will block. 
      e.allowJSThreads = false;
      e.dispatchCommands(false, false);
    } catch (Exception ex) {
      viewer.setStringProperty("_errormessage", "" + ex);
      if (e.thisContext == null) {
        Logger.error("Error evaluating context");
        ex.printStackTrace();
      }
      return false;
    }
    return true;
  }

  /**
   * a general method to evaluate a string representing an atom set.
   * 
   * @param atomExpression
   * @return is a bitset indicating the selected atoms
   * 
   */
  public BS getAtomBitSet(Object atomExpression) {
    if (atomExpression instanceof BS)
      return (BS) atomExpression;
    BS bs = new BS();
    try {
      pushContext(null);
      String scr = "select (" + atomExpression + ")";
      scr = TextFormat.replaceAllCharacters(scr, "\n\r", "),(");
      scr = TextFormat.simpleReplace(scr, "()", "(none)");
      if (compileScript(null, scr, false)) {
        st = aatoken[0];
        bs = atomExpression(st, 1, 0, false, false, true, true);
      }
      popContext(false, false);
    } catch (Exception ex) {
      Logger.error("getAtomBitSet " + atomExpression + "\n" + ex);
    }
    return bs;
  }

  /**
   * just provides a vector list of atoms in a string-based expression
   * 
   * @param atomCount
   * @param atomExpression
   * @return vector list of selected atoms
   */
  public JmolList<Integer> getAtomBitSetVector(int atomCount, Object atomExpression) {
    JmolList<Integer> V = new  JmolList<Integer>();
    BS bs = getAtomBitSet(atomExpression);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      V.addLast(Integer.valueOf(i));
    }
    return V;
  }

  @SuppressWarnings("unchecked")
  private JmolList<SV> parameterExpressionList(int pt, int ptAtom,
                                                       boolean isArrayItem)
      throws ScriptException {
    return (JmolList<SV>) parameterExpression(pt, -1, null, true, true,
        ptAtom, isArrayItem, null, null);
  }

  private String parameterExpressionString(int pt, int ptMax)
      throws ScriptException {
    return (String) parameterExpression(pt, ptMax, "", true, false, -1, false,
        null, null);
  }

  private boolean parameterExpressionBoolean(int pt, int ptMax)
      throws ScriptException {
    return ((Boolean) parameterExpression(pt, ptMax, null, true, false, -1,
        false, null, null)).booleanValue();
  }

  private SV parameterExpressionToken(int pt)
      throws ScriptException {
    JmolList<SV> result = parameterExpressionList(pt, -1, false);
    return (result.size() > 0 ? result.get(0) : SV.newVariable(T.string, ""));
  }

  /**
   * This is the primary driver of the RPN (reverse Polish notation) expression
   * processor. It handles all math outside of a "traditional" Jmol
   * SELECT/RESTRICT context. [Object atomExpression() takes care of that, and
   * also uses the RPN class.]
   * 
   * @param pt
   *        token index in statement start of expression
   * @param ptMax
   *        token index in statement end of expression
   * @param key
   *        variable name for debugging reference only -- null indicates return
   *        Boolean -- "" indicates return String
   * @param ignoreComma
   *        TODO
   * @param asVector
   *        a flag passed on to RPN;
   * @param ptAtom
   *        this is a for() or select() function with a specific atom selected
   * @param isArrayItem
   *        we are storing A[x] = ... so we need to deliver "x" as well
   * @param localVars
   *        see below -- lists all nested for(x, {exp}, select(y, {ex},...))
   *        variables
   * @param localVar
   *        x or y in above for(), select() examples
   * @return either a vector or a value, caller's choice.
   * @throws ScriptException
   *         errors are thrown directly to the Eval error system.
   */
  private Object parameterExpression(int pt, int ptMax, String key,
                                     boolean ignoreComma, boolean asVector,
                                     int ptAtom, boolean isArrayItem,
                                     Map<String, SV> localVars, String localVar)
      throws ScriptException {

    /*
     * localVar is a variable designated at the beginning of the select(x,...)
     * or for(x,...) construct that will be implicitly used for properties. So,
     * for example, "atomno" will become "x.atomno". That's all it is for.
     * localVars provides a localized context variable set for a given nested
     * set of for/select.
     * 
     * Note that localVars has nothing to do standard if/for/while flow
     * contexts, just these specialized functions. Any variable defined in for
     * or while is simply added to the context for a given script or function.
     * These assignments are made by the compiler when seeing a VAR keyword.
     */
    Object v, res;
    boolean isImplicitAtomProperty = (localVar != null);
    boolean isOneExpressionOnly = (pt < 0);
    boolean returnBoolean = (!asVector && key == null);
    boolean returnString = (!asVector && key != null && key.length() == 0);
    int nSquare = 0;
    if (isOneExpressionOnly)
      pt = -pt;
    int nParen = 0;
    ScriptMathProcessor rpn = new ScriptMathProcessor(this, isArrayItem,
        asVector, false);
    if (pt == 0 && ptMax == 0) // set command with v[...] = ....
      pt = 2;
    if (ptMax < pt)
      ptMax = slen;
    out: for (int i = pt; i < ptMax; i++) {
      v = null;
      int tok = getToken(i).tok;
      if (isImplicitAtomProperty && tokAt(i + 1) != T.per) {
        SV token = (localVars != null
            && localVars.containsKey(theToken.value) ? null
            : getBitsetPropertySelector(i, false));
        if (token != null) {
          rpn.addXVar(localVars.get(localVar));
          if (!rpn.addOpAllowMath(token, (tokAt(i + 1) == T.leftparen)))
            error(ERROR_invalidArgument);
          if ((token.intValue == T.function || token.intValue == T.parallel)
              && tokAt(iToken + 1) != T.leftparen) {
            rpn.addOp(T.tokenLeftParen);
            rpn.addOp(T.tokenRightParen);
          }
          i = iToken;
          continue;
        }
      }
      switch (tok) {
      case T.define:
        // @{@x} or @{@{x}} or @{@1} -- also userFunction(@1)
        if (tokAt(++i) == T.expressionBegin) {
          v = parameterExpressionToken(++i);
          i = iToken;
        } else if (tokAt(i) == T.integer) {
          v = viewer.getAtomBits(T.atomno, Integer
              .valueOf(st[i].intValue));
          break;
        } else {
          v = getParameter(SV.sValue(st[i]), T.variable);
        }
        v = getParameter(((SV) v).asString(), T.variable);
        break;
      case T.ifcmd:
        if (getToken(++i).tok != T.leftparen)
          error(ERROR_invalidArgument);
        if (localVars == null)
          localVars = new Hashtable<String, SV>();
        res = parameterExpression(++i, -1, null, ignoreComma, false, -1, false,
            localVars, localVar);
        boolean TF = ((Boolean) res).booleanValue();
        int iT = iToken;
        if (getToken(iT++).tok != T.semicolon)
          error(ERROR_invalidArgument);
        parameterExpressionBoolean(iT, -1);
        int iF = iToken;
        if (tokAt(iF++) != T.semicolon)
          error(ERROR_invalidArgument);
        parameterExpression(-iF, -1, null, ignoreComma, false, 1, false,
            localVars, localVar);
        int iEnd = iToken;
        if (tokAt(iEnd) != T.rightparen)
          error(ERROR_invalidArgument);
        v = parameterExpression(TF ? iT : iF, TF ? iF : iEnd, "XXX",
            ignoreComma, false, 1, false, localVars, localVar);
        i = iEnd;
        break;
      case T.forcmd:
      case T.select:
        boolean isFunctionOfX = (pt > 0);
        boolean isFor = (isFunctionOfX && tok == T.forcmd);
        // it is important to distinguish between the select command:
        // select {atomExpression} (mathExpression)
        // and the select(dummy;{atomExpression};mathExpression) function:
        // select {*.ca} (phi < select(y; {*.ca}; y.resno = _x.resno + 1).phi)
        String dummy;
        // for(dummy;...
        // select(dummy;...
        if (isFunctionOfX) {
          if (getToken(++i).tok != T.leftparen
              || !T.tokAttr(getToken(++i).tok, T.identifier))
            error(ERROR_invalidArgument);
          dummy = parameterAsString(i);
          if (getToken(++i).tok != T.semicolon)
            error(ERROR_invalidArgument);
        } else {
          dummy = "_x";
        }
        // for(dummy;{atom expr};...
        // select(dummy;{atom expr};...
        v = parameterExpressionToken(-(++i)).value;
        if (!(v instanceof BS))
          error(ERROR_invalidArgument);
        BS bsAtoms = (BS) v;
        i = iToken;
        if (isFunctionOfX && getToken(i++).tok != T.semicolon)
          error(ERROR_invalidArgument);
        // for(dummy;{atom expr};math expr)
        // select(dummy;{atom expr};math expr)
        // bsX is necessary because there are a few operations that still
        // are there for now that require it; could go, though.
        BS bsSelect = new BS();
        BS bsX = new BS();
        String[] sout = (isFor ? new String[BSUtil.cardinalityOf(bsAtoms)]
            : null);
        if (localVars == null)
          localVars = new Hashtable<String, SV>();
        bsX.set(0);
        SV t = SV.newScriptVariableBs(bsX, 0);
        localVars.put(dummy, t.setName(dummy));
        // one test just to check for errors and get iToken
        int pt2 = -1;
        if (isFunctionOfX) {
          pt2 = i - 1;
          int np = 0;
          int tok2;
          while (np >= 0 && ++pt2 < ptMax) {
            if ((tok2 = tokAt(pt2)) == T.rightparen)
              np--;
            else if (tok2 == T.leftparen)
              np++;
          }
        }
        int p = 0;
        int jlast = 0;
        int j = bsAtoms.nextSetBit(0);
        if (j < 0) {
          iToken = pt2 - 1;
        } else if (!chk) {
          for (; j >= 0; j = bsAtoms.nextSetBit(j + 1)) {
            if (jlast >= 0)
              bsX.clear(jlast);
            jlast = j;
            bsX.set(j);
            t.index = j;
            res = parameterExpression(i, pt2, (isFor ? "XXX" : null),
                ignoreComma, isFor, j, false, localVars, isFunctionOfX ? null
                    : dummy);
            if (isFor) {
              if (res == null || ((JmolList<?>) res).size() == 0)
                error(ERROR_invalidArgument);
              sout[p++] = ((SV) ((JmolList<?>) res).get(0)).asString();
            } else if (((Boolean) res).booleanValue()) {
              bsSelect.set(j);
            }
          }
        }
        if (isFor) {
          v = sout;
        } else if (isFunctionOfX) {
          v = bsSelect;
        } else {
          return bitsetVariableVector(bsSelect);
        }
        i = iToken + 1;
        break;
      case T.semicolon: // for (i = 1; i < 3; i=i+1)
        break out;
      case T.decimal:
        rpn.addXNum(SV.newVariable(T.decimal, theToken.value));
        break;
      case T.spec_seqcode:
      case T.integer:
        rpn.addXNum(new ScriptVariableInt(theToken.intValue));
        break;
      // these next are for the within() command
      case T.plane:
        if (tokAt(iToken + 1) == T.leftparen) {
          if (!rpn.addOpAllowMath(theToken, true))
            error(ERROR_invalidArgument);
          break;
        }
        rpn.addXVar(SV.newScriptVariableToken(theToken));
        break;
      // for within:
      case T.atomname:
      case T.atomtype:
      case T.branch:
      case T.boundbox:
      case T.chain:
      case T.coord:
      case T.element:
      case T.group:
      case T.model:
      case T.molecule:
      case T.sequence:
      case T.site:
      case T.search:
      case T.smiles:
      case T.substructure:
      case T.structure:
        ////
      case T.on:
      case T.off:
      case T.string:
      case T.point3f:
      case T.point4f:
      case T.matrix3f:
      case T.matrix4f:
      case T.bitset:
      case T.hash:
        rpn.addXVar(SV.newScriptVariableToken(theToken));
        break;
      case T.dollarsign:
        ignoreError = true;
        P3 ptc;
        try{
          ptc = centerParameter(i);
          rpn.addXVar(SV.newVariable(T.point3f, ptc));
        } catch (Exception e) {
          rpn.addXStr("");
        }
        ignoreError = false;
        i = iToken;
        break;
      case T.leftbrace:
        if (tokAt(i + 1) == T.string)
          v = getHash(i);
        else
          v = getPointOrPlane(i, false, true, true, false, 3, 4);
        i = iToken;
        break;
      case T.expressionBegin:
        if (tokAt(i + 1) == T.expressionEnd) {
          v = new Hashtable<String, Object>();
          i++;
          break;
        } else if (tokAt(i + 1) == T.all
            && tokAt(i + 2) == T.expressionEnd) {
          tok = T.all;
          iToken += 2;
        }
        //$FALL-THROUGH$
      case T.all:
        if (tok == T.all)
          v = viewer.getModelUndeletedAtomsBitSet(-1);
        else
          v = atomExpression(st, i, 0, true, true, true, true);
        i = iToken;
        if (nParen == 0 && isOneExpressionOnly) {
          iToken++;
          return bitsetVariableVector(v);
        }
        break;
      case T.spacebeforesquare:
        rpn.addOp(theToken);
        continue;
      case T.expressionEnd:
        i++;
        break out;
      case T.rightbrace:
        if (!ignoreComma && nParen == 0 && nSquare == 0)
          break out;
        error(ERROR_invalidArgument);
        break;
      case T.comma: // ignore commas
        if (!ignoreComma && nParen == 0 && nSquare == 0) {
          break out;
        }
        if (!rpn.addOp(theToken))
          error(ERROR_invalidArgument);
        break;
      case T.per:
        SV token = getBitsetPropertySelector(i + 1, false);
        if (token == null)
          error(ERROR_invalidArgument);
        // check for added min/max modifier
        boolean isUserFunction = (token.intValue == T.function);
        boolean allowMathFunc = true;
        int tok2 = tokAt(iToken + 2);
        if (tokAt(iToken + 1) == T.per) {
          switch (tok2) {
          case T.all:
            tok2 = T.minmaxmask;
            if (tokAt(iToken + 3) == T.per
                && tokAt(iToken + 4) == T.bin)
              tok2 = T.selectedfloat;
            //$FALL-THROUGH$
          case T.min:
          case T.max:
          case T.stddev:
          case T.sum:
          case T.sum2:
          case T.average:
            allowMathFunc = (isUserFunction || tok2 == T.minmaxmask || tok2 == T.selectedfloat);
            token.intValue |= tok2;
            getToken(iToken + 2);
          }
        }
        allowMathFunc &= (tokAt(iToken + 1) == T.leftparen || isUserFunction);
        if (!rpn.addOpAllowMath(token, allowMathFunc))
          error(ERROR_invalidArgument);
        i = iToken;
        if (token.intValue == T.function && tokAt(i + 1) != T.leftparen) {
          rpn.addOp(T.tokenLeftParen);
          rpn.addOp(T.tokenRightParen);
        }
        break;
      default:
        if (T.tokAttr(theTok, T.mathop)
            || T.tokAttr(theTok, T.mathfunc)
            && tokAt(iToken + 1) == T.leftparen) {
          if (!rpn.addOp(theToken)) {
            if (ptAtom >= 0) {
              // this is expected -- the right parenthesis
              break out;
            }
            error(ERROR_invalidArgument);
          }
          switch (theTok) {
          case T.leftparen:
            nParen++;
            break;
          case T.rightparen:
            if (--nParen <= 0 && nSquare == 0 && isOneExpressionOnly) {
              iToken++;
              break out;
            }
            break;
          case T.leftsquare:
            nSquare++;
            break;
          case T.rightsquare:
            if (--nSquare == 0 && nParen == 0 && isOneExpressionOnly) {
              iToken++;
              break out;
            }
            break;
          }
        } else {
          // first check to see if the variable has been defined already
          String name = parameterAsString(i).toLowerCase();
          boolean haveParens = (tokAt(i + 1) == T.leftparen);
          if (chk) {
            v = name;
          } else if (!haveParens
              && (localVars == null || (v = localVars.get(name)) == null)) {
            v = getContextVariableAsVariable(name);
          }
          if (v == null) {
            if (T.tokAttr(theTok, T.identifier)
                && viewer.isFunction(name)) {
              if (!rpn
                  .addOp(SV.newVariable(T.function, theToken.value)))
                error(ERROR_invalidArgument);
              if (!haveParens) {
                rpn.addOp(T.tokenLeftParen);
                rpn.addOp(T.tokenRightParen);
              }
            } else {
              rpn.addXVar(viewer.getOrSetNewVariable(name, false));
            }
          }
        }
      }
      if (v != null) {
        if (v instanceof BS)
          rpn.addXBs((BS) v);
        else
          rpn.addXObj(v);
      }
    }
    SV result = rpn.getResult(false);
    if (result == null) {
      if (!chk)
        rpn.dumpStacks("null result");
      error(ERROR_endOfStatementUnexpected);
    }
    if (result.tok == T.vector)
      return result.value;
    if (returnBoolean)
      return Boolean.valueOf(result.asBoolean());
    if (returnString) {
      if (result.tok == T.string)
        result.intValue = Integer.MAX_VALUE;
      return result.asString();
    }
    switch (result.tok) {
    case T.on:
    case T.off:
      return Boolean.valueOf(result.intValue == 1);
    case T.integer:
      return Integer.valueOf(result.intValue);
    case T.bitset:
    case T.decimal:
    case T.string:
    case T.point3f:
    default:
      return result.value;
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getHash(int i) throws ScriptException {
    Map<String, Object> ht = new Hashtable<String, Object>();
    for (i = i + 1; i < slen; i++) {
      if (tokAt(i) == T.rightbrace)
        break;
      String key = stringParameter(i++);
      if (tokAt(i++) != T.colon)
        error(ERROR_invalidArgument);
      JmolList<SV> v = (JmolList<SV>) parameterExpression(i, 0,
          null, false, true, -1, false, null, null);
      ht.put(key, v.get(0));
      i = iToken;
      if (tokAt(i) != T.comma)
        break;
    }
    iToken = i;
    if (tokAt(i) != T.rightbrace)
      error(ERROR_invalidArgument);
    return ht;
  }

  JmolList<SV> bitsetVariableVector(Object v) {
    JmolList<SV> resx = new  JmolList<SV>();
    if (v instanceof BS) {
      resx.addLast(SV.newVariable(T.bitset, v));
    }
    return resx;
  }

  Object getBitsetIdent(BS bs, String label, Object tokenValue,
                        boolean useAtomMap, int index, boolean isExplicitlyAll) {
    boolean isAtoms = !(tokenValue instanceof BondSet);
    if (isAtoms) {
      if (label == null)
        label = viewer.getStandardLabelFormat(0);
      else if (label.length() == 0)
        label = "%[label]";
    }
    int pt = (label == null ? -1 : label.indexOf("%"));
    boolean haveIndex = (index != Integer.MAX_VALUE);
    if (bs == null || chk || isAtoms && pt < 0) {
      if (label == null)
        label = "";
      return isExplicitlyAll ? new String[] { label } : (Object) label;
    }
    ModelSet modelSet = viewer.modelSet;
    int n = 0;
    int[] indices = (isAtoms || !useAtomMap ? null : ((BondSet) tokenValue)
        .getAssociatedAtoms());
    if (indices == null && label != null && label.indexOf("%D") > 0)
      indices = viewer.getAtomIndices(bs);
    boolean asIdentity = (label == null || label.length() == 0);
    Map<String, Object> htValues = (isAtoms || asIdentity ? null : LabelToken
        .getBondLabelValues());
    LabelToken[] tokens = (asIdentity ? null : isAtoms ? LabelToken.compile(
        viewer, label, '\0', null) : LabelToken.compile(viewer, label, '\1',
        htValues));
    int nmax = (haveIndex ? 1 : BSUtil.cardinalityOf(bs));
    String[] sout = new String[nmax];
    for (int j = (haveIndex ? index : bs.nextSetBit(0)); j >= 0; j = bs
        .nextSetBit(j + 1)) {
      String str;
      if (isAtoms) {
        if (asIdentity)
          str = modelSet.atoms[j].getInfo();
        else
          str = LabelToken.formatLabelAtomArray(viewer, modelSet.atoms[j], tokens, '\0',
              indices);
      } else {
        Bond bond = modelSet.getBondAt(j);
        if (asIdentity)
          str = bond.getIdentity();
        else
          str = LabelToken.formatLabelBond(viewer, bond, tokens, htValues, indices);
      }
      str = TextFormat.formatStringI(str, "#", (n + 1));
      sout[n++] = str;
      if (haveIndex)
        break;
    }
    return nmax == 1 && !isExplicitlyAll ? sout[0] : (Object) sout;
  }

  private SV getBitsetPropertySelector(int i, boolean mustBeSettable)
      throws ScriptException {
    int tok = getToken(i).tok;
    switch (tok) {
    case T.min:
    case T.max:
    case T.average:
    case T.stddev:
    case T.sum:
    case T.sum2:
    case T.property:
      break;
    default:
      if (T.tokAttrOr(tok, T.atomproperty, T.mathproperty))
        break;
      if (tok != T.opIf && !T.tokAttr(tok, T.identifier))
        return null;
      String name = parameterAsString(i);
      if (!mustBeSettable && viewer.isFunction(name)) {
        tok = T.function;
        break;
      }
      if (!name.endsWith("?"))
        return null;
      tok = T.identifier;
    }
    if (mustBeSettable && !T.tokAttr(tok, T.settable))
      return null;
    return SV.newScriptVariableIntValue(T.propselector, tok, parameterAsString(i)
        .toLowerCase());
  }

  private float[] getBitsetPropertyFloat(BS bs, int tok, float min,
                                         float max) throws ScriptException {
    float[] data = (float[]) getBitsetProperty(bs, tok, null, null, null, null,
        false, Integer.MAX_VALUE, false);
    if (!Float.isNaN(min))
      for (int i = 0; i < data.length; i++)
        if (data[i] < min)
          data[i] = Float.NaN;
    if (!Float.isNaN(max))
      for (int i = 0; i < data.length; i++)
        if (data[i] > max)
          data[i] = Float.NaN;
    return data;
  }

  @SuppressWarnings("unchecked")
  protected Object getBitsetProperty(BS bs, int tok, P3 ptRef,
                                     Point4f planeRef, Object tokenValue,
                                     Object opValue, boolean useAtomMap,
                                     int index, boolean asVectorIfAll)
      throws ScriptException {

    // index is a special argument set in parameterExpression that
    // indicates we are looking at only one atom within a for(...) loop
    // the bitset cannot be a BondSet in that case

    boolean haveIndex = (index != Integer.MAX_VALUE);

    boolean isAtoms = haveIndex || !(tokenValue instanceof BondSet);
    // check minmax flags:

    int minmaxtype = tok & T.minmaxmask;
    boolean selectedFloat = (minmaxtype == T.selectedfloat);
    int atomCount = viewer.getAtomCount();
    float[] fout = (minmaxtype == T.allfloat ? new float[atomCount] : null);
    boolean isExplicitlyAll = (minmaxtype == T.minmaxmask || selectedFloat);
    tok &= ~T.minmaxmask;
    if (tok == T.nada)
      tok = (isAtoms ? T.atoms : T.bonds);

    // determine property type:

    boolean isPt = false;
    boolean isInt = false;
    boolean isString = false;
    switch (tok) {
    case T.xyz:
    case T.vibxyz:
    case T.fracxyz:
    case T.fuxyz:
    case T.unitxyz:
    case T.color:
    case T.screenxyz:
      isPt = true;
      break;
    case T.function:
    case T.distance:
      break;
    default:
      isInt = T.tokAttr(tok, T.intproperty)
          && !T.tokAttr(tok, T.floatproperty);
      // occupancy and radius considered floats here
      isString = !isInt && T.tokAttr(tok, T.strproperty);
      // structure considered int; for the name, use .label("%[structure]")
    }

    // preliminarty checks we only want to do once:

    P3 zero = (minmaxtype == T.allfloat ? new P3() : null);
    P3 pt = (isPt || !isAtoms ? new P3() : null);
    if (isExplicitlyAll || isString && !haveIndex
        && minmaxtype != T.allfloat && minmaxtype != T.min)
      minmaxtype = T.all;
    JmolList<Object> vout = (minmaxtype == T.all ? new  JmolList<Object>()
        : null);
    BS bsNew = null;
    String userFunction = null;
    JmolList<SV> params = null;
    BS bsAtom = null;
    SV tokenAtom = null;
    P3 ptT = null;
    float[] data = null;

    switch (tok) {
    case T.atoms:
    case T.bonds:
      if (chk)
        return bs;
      bsNew = (tok == T.atoms ? (isAtoms ? bs : viewer.getAtomBits(
          T.bonds, bs)) : (isAtoms ? (BS) new BondSet(viewer
          .getBondsForSelectedAtoms(bs)) : bs));
      int i;
      switch (minmaxtype) {
      case T.min:
        i = bsNew.nextSetBit(0);
        break;
      case T.max:
        i = bsNew.length() - 1;
        break;
      case T.stddev:
      case T.sum:
      case T.sum2:
        return Float.valueOf(Float.NaN);
      default:
        return bsNew;
      }
      bsNew.clearAll();
      if (i >= 0)
        bsNew.set(i);
      return bsNew;
    case T.identify:
      switch (minmaxtype) {
      case 0:
      case T.all:
        return getBitsetIdent(bs, null, tokenValue, useAtomMap, index,
            isExplicitlyAll);
      }
      return "";
    case T.function:
      userFunction = (String) ((Object[]) opValue)[0];
      params = (JmolList<SV>) ((Object[]) opValue)[1];
      bsAtom = BSUtil.newBitSet(atomCount);
      tokenAtom = SV.newVariable(T.bitset, bsAtom);
      break;
    case T.straightness:
    case T.surfacedistance:
      viewer.autoCalculate(tok);
      break;
    case T.distance:
      if (ptRef == null && planeRef == null)
        return new P3();
      break;
    case T.color:
      ptT = new P3();
      break;
    case T.property:
      data = viewer.getDataFloat((String) opValue);
      break;
    }

    int n = 0;
    int ivvMinMax = 0;
    int ivMinMax = 0;
    float fvMinMax = 0;
    double sum = 0;
    double sum2 = 0;
    switch (minmaxtype) {
    case T.min:
      ivMinMax = Integer.MAX_VALUE;
      fvMinMax = Float.MAX_VALUE;
      break;
    case T.max:
      ivMinMax = Integer.MIN_VALUE;
      fvMinMax = -Float.MAX_VALUE;
      break;
    }
    ModelSet modelSet = viewer.modelSet;
    int mode = (isPt ? 3 : isString ? 2 : isInt ? 1 : 0);
    if (isAtoms) {
      boolean haveBitSet = (bs != null);
      int iModel = -1;
      int i0, i1;
      if (haveIndex) {
        i0 = index;
        i1 = index + 1;
      } else if (haveBitSet) {
        i0 = bs.nextSetBit(0);
        i1 = Math.min(atomCount, bs.length());
      } else {
        i0 = 0;
        i1 = atomCount;
      }
      if (chk)
        i1 = 0;
      for (int i = i0; i >= 0 && i < i1; i = (haveBitSet ? bs.nextSetBit(i + 1)
          : i + 1)) {
        n++;
        Atom atom = modelSet.atoms[i];
        switch (mode) {
        case 0: // float
          float fv = Float.MAX_VALUE;
          switch (tok) {
          case T.function:
            bsAtom.set(i);
            fv = SV.fValue(runFunctionRet(null, userFunction, params,
                tokenAtom, true, true, false));
            bsAtom.clear(i);
            break;
          case T.property:
            fv = (data == null ? 0 : data[i]);
            break;
          case T.distance:
            if (planeRef != null)
              fv = Measure.distanceToPlane(planeRef, atom);
            else
              fv = atom.distance(ptRef);
            break;
          default:
            fv = Atom.atomPropertyFloat(viewer, atom, tok);
          }
          if (fv == Float.MAX_VALUE || Float.isNaN(fv)
              && minmaxtype != T.all) {
            n--; // don't count this one
            continue;
          }
          switch (minmaxtype) {
          case T.min:
            if (fv < fvMinMax)
              fvMinMax = fv;
            break;
          case T.max:
            if (fv > fvMinMax)
              fvMinMax = fv;
            break;
          case T.allfloat:
            fout[i] = fv;
            break;
          case T.all:
            vout.addLast(Float.valueOf(fv));
            break;
          case T.sum2:
          case T.stddev:
            sum2 += ((double) fv) * fv;
            //$FALL-THROUGH$
          case T.sum:
          default:
            sum += fv;
          }
          break;
        case 1: // isInt
          int iv = 0;
          switch (tok) {
          case T.symop:
            // a little weird:
            // First we determine how many operations we have in this model.
            // Then we get the symmetry bitset, which shows the assignments
            // of symmetry for this atom.
            if (atom.getModelIndex() != iModel)
              iModel = atom.getModelIndex();
            BS bsSym = atom.getAtomSymmetry();
            if (bsSym == null)
              break;
            int p = 0;
            switch (minmaxtype) {
            case T.min:
              ivvMinMax = Integer.MAX_VALUE;
              break;
            case T.max:
              ivvMinMax = Integer.MIN_VALUE;
              break;
            }
            for (int k = bsSym.nextSetBit(0); k >= 0; k = bsSym
                .nextSetBit(k + 1)) {
              iv += k + 1;
              switch (minmaxtype) {
              case T.min:
                ivvMinMax = Math.min(ivvMinMax, k + 1);
                break;
              case T.max:
                ivvMinMax = Math.max(ivvMinMax, k + 1);
                break;
              }
              p++;
            }
            switch (minmaxtype) {
            case T.min:
            case T.max:
              iv = ivvMinMax;
            }
            n += p - 1;
            break;
          case T.configuration:
          case T.cell:
            errorStr(ERROR_unrecognizedAtomProperty, T.nameOf(tok));
            break;
          default:
            iv = Atom.atomPropertyInt(atom, tok);
          }
          switch (minmaxtype) {
          case T.min:
            if (iv < ivMinMax)
              ivMinMax = iv;
            break;
          case T.max:
            if (iv > ivMinMax)
              ivMinMax = iv;
            break;
          case T.allfloat:
            fout[i] = iv;
            break;
          case T.all:
            vout.addLast(Integer.valueOf(iv));
            break;
          case T.sum2:
          case T.stddev:
            sum2 += ((double) iv) * iv;
            //$FALL-THROUGH$
          case T.sum:
          default:
            sum += iv;
          }
          break;
        case 2: // isString
          String s = Atom.atomPropertyString(viewer, atom, tok);
          switch (minmaxtype) {
          case T.allfloat:
            fout[i] = Parser.parseFloatStr(s);
            break;
          default:
            if (vout == null)
              return s;
            vout.addLast(s);
          }
          break;
        case 3: // isPt
          Tuple3f t = Atom.atomPropertyTuple(atom, tok);
          if (t == null)
            errorStr(ERROR_unrecognizedAtomProperty, T.nameOf(tok));
          switch (minmaxtype) {
          case T.allfloat:
            fout[i] = (float) Math.sqrt(t.x * t.x + t.y * t.y + t.z * t.z);
            break;
          case T.all:
            vout.addLast(P3.newP(t));
            break;
          default:
            pt.add(t);
          }
          break;
        }
        if (haveIndex)
          break;
      }
    } else { // bonds
      boolean isAll = (bs == null);
      int i0 = (isAll ? 0 : bs.nextSetBit(0));
      int i1 = viewer.getBondCount();
      for (int i = i0; i >= 0 && i < i1; i = (isAll ? i + 1 : bs
          .nextSetBit(i + 1))) {
        n++;
        Bond bond = modelSet.getBondAt(i);
        switch (tok) {
        case T.length:
          float fv = bond.getAtom1().distance(bond.getAtom2());
          switch (minmaxtype) {
          case T.min:
            if (fv < fvMinMax)
              fvMinMax = fv;
            break;
          case T.max:
            if (fv > fvMinMax)
              fvMinMax = fv;
            break;
          case T.all:
            vout.addLast(Float.valueOf(fv));
            break;
          case T.sum2:
          case T.stddev:
            sum2 += (double) fv * fv;
            //$FALL-THROUGH$
          case T.sum:
          default:
            sum += fv;
          }
          break;
        case T.xyz:
          switch (minmaxtype) {
          case T.all:
            pt.setT(bond.getAtom1());
            pt.add(bond.getAtom2());
            pt.scale(0.5f);
            vout.addLast(P3.newP(pt));
            break;
          default:
            pt.add(bond.getAtom1());
            pt.add(bond.getAtom2());
            n++;
          }
          break;
        case T.color:
          ColorUtil.colorPointFromInt(viewer.getColorArgbOrGray(bond
              .colix), ptT);
          switch (minmaxtype) {
          case T.all:
            vout.addLast(P3.newP(ptT));
            break;
          default:
            pt.add(ptT);
          }
          break;
        default:
          errorStr(ERROR_unrecognizedBondProperty, T.nameOf(tok));
        }
      }
    }
    if (minmaxtype == T.allfloat)
      return fout;
    if (minmaxtype == T.all) {
      if (asVectorIfAll)
        return vout;
      int len = vout.size();
      if (isString && !isExplicitlyAll && len == 1)
        return vout.get(0);
      if (selectedFloat) {
        fout = new float[len];
        for (int i = len; --i >= 0;) {
          Object v = vout.get(i);
          switch (mode) {
          case 0:
            fout[i] = ((Float) v).floatValue();
            break;
          case 1:
            fout[i] = ((Integer) v).floatValue();
            break;
          case 2:
            fout[i] = Parser.parseFloatStr((String) v);
            break;
          case 3:
            fout[i] = ((P3) v).distance(zero);
            break;
          }
        }
        return fout;
      }
      if (tok == T.sequence) {
        SB sb = new SB();
        for (int i = 0; i < len; i++)
          sb.append((String) vout.get(i));
        return sb.toString();
      }
      String[] sout = new String[len];
      for (int i = len; --i >= 0;) {
        Object v = vout.get(i);
        if (v instanceof P3)
          sout[i] = Escape.eP((P3) v);
        else
          sout[i] = "" + vout.get(i);
      }
      return sout; // potential j2s issue here
    }
    if (isPt)
      return (n == 0 ? pt : P3.new3(pt.x / n, pt.y / n, pt.z / n));
    if (n == 0 || n == 1 && minmaxtype == T.stddev)
      return Float.valueOf(Float.NaN);
    if (isInt) {
      switch (minmaxtype) {
      case T.min:
      case T.max:
        return Integer.valueOf(ivMinMax);
      case T.sum2:
      case T.stddev:
        break;
      case T.sum:
        return Integer.valueOf((int) sum);
      default:
        if (sum / n == (int) (sum / n))
          return Integer.valueOf((int) (sum / n));
        return Float.valueOf((float) (sum / n));
      }
    }
    switch (minmaxtype) {
    case T.min:
    case T.max:
      sum = fvMinMax;
      break;
    case T.sum:
      break;
    case T.sum2:
      sum = sum2;
      break;
    case T.stddev:
      // because SUM (x_i - X_av)^2 = SUM(x_i^2) - 2X_av SUM(x_i) + SUM(X_av^2)
      // = SUM(x_i^2) - 2nX_av^2 + nX_av^2
      // = SUM(x_i^2) - nX_av^2
      // = SUM(x_i^2) - [SUM(x_i)]^2 / n
      sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
      break;
    default:
      sum /= n;
      break;
    }
    return Float.valueOf((float) sum);
  }

  private void setBitsetProperty(BS bs, int tok, int iValue, float fValue,
                                 T tokenValue) throws ScriptException {
    if (chk || BSUtil.cardinalityOf(bs) == 0)
      return;
    String[] list = null;
    String sValue = null;
    float[] fvalues = null;
    P3 pt;
    JmolList<SV> sv = null;
    int nValues = 0;
    boolean isStrProperty = T.tokAttr(tok, T.strproperty);
    if (tokenValue.tok == T.varray) {
      sv = ((SV) tokenValue).getList();
      if ((nValues = sv.size()) == 0)
        return;
    }
    switch (tok) {
    case T.xyz:
    case T.fracxyz:
    case T.fuxyz:
    case T.vibxyz:
      switch (tokenValue.tok) {
      case T.point3f:
        viewer.setAtomCoord(bs, tok, tokenValue.value);
        break;
      case T.varray:
        theToken = tokenValue;
        viewer.setAtomCoord(bs, tok, getPointArray(-1, nValues));
        break;
      }
      return;
    case T.color:
      Object value = null;
      String prop = "color";
      switch (tokenValue.tok) {
      case T.varray:
        int[] values = new int[nValues];
        for (int i = nValues; --i >= 0;) {
          SV svi = sv.get(i);
          pt = SV.ptValue(svi);
          if (pt != null) {
            values[i] = ColorUtil.colorPtToInt(pt);
          } else if (svi.tok == T.integer) {
            values[i] = svi.intValue;
          } else {
            values[i] = ColorUtil.getArgbFromString(svi.asString());
            if (values[i] == 0)
              values[i] = svi.asInt();
          }
          if (values[i] == 0)
            errorStr2(ERROR_unrecognizedParameter, "ARRAY", svi.asString());
        }
        value = values;
        prop = "colorValues";
        break;
      case T.point3f:
        value = Integer.valueOf(ColorUtil
            .colorPtToInt((P3) tokenValue.value));
        break;
      case T.string:
        value = tokenValue.value;
        break;
      default:
        value = Integer.valueOf(SV.iValue(tokenValue));
        break;
      }
      setShapePropertyBs(JC.SHAPE_BALLS, prop, value, bs);
      return;
    case T.label:
    case T.format:
      if (tokenValue.tok != T.varray)
        sValue = SV.sValue(tokenValue);
      break;
    case T.element:
    case T.elemno:
      clearDefinedVariableAtomSets();
      isStrProperty = false;
      break;
    }
    switch (tokenValue.tok) {
    case T.varray:
      if (isStrProperty)
        list = SV.listValue(tokenValue);
      else
        fvalues = SV.flistValue(tokenValue, nValues);
      break;
    case T.string:
      if (sValue == null)
        list = Parser.getTokens(SV.sValue(tokenValue));
      break;
    }
    if (list != null) {
      nValues = list.length;
      if (!isStrProperty) {
        fvalues = new float[nValues];
        for (int i = nValues; --i >= 0;)
          fvalues[i] = (tok == T.element ? Elements
              .elementNumberFromSymbol(list[i], false) : Parser
              .parseFloatStr(list[i]));
      }
      if (tokenValue.tok != T.varray && nValues == 1) {
        if (isStrProperty)
          sValue = list[0];
        else
          fValue = fvalues[0];
        iValue = (int) fValue;
        list = null;
        fvalues = null;
      }
    }
    viewer.setAtomProperty(bs, tok, iValue, fValue, sValue, fvalues, list);
  }

  // ///////////////////// general fields //////////////////////

  private final static int scriptLevelMax = 100;

  private Thread currentThread;
  protected Viewer viewer;
  protected ScriptCompiler compiler;
  private Map<String, Object> definedAtomSets;
  private SB outputBuffer;

  private String contextPath = "";
  private String scriptFileName;
  private String functionName;
  private boolean isStateScript;
  private int scriptLevel;
  
  private int scriptReportingLevel = 0;
  private int commandHistoryLevelMax = 0;

  // created by Compiler:
  private T[][] aatoken;
  private short[] lineNumbers;
  private int[][] lineIndices;
  private Map<String, SV> contextVariables;

  public Map<String, SV> getContextVariables() {
    return contextVariables;
  }

  private String script;

  public String getScript() {
    return script;
  }

  // specific to current statement
  protected int pc; // program counter
  private String thisCommand;
  private String fullCommand;
  private T[] st;
  private int slen;
  private int iToken;
  private int lineEnd;
  private int pcEnd;
  private String scriptExtensions;
  private boolean forceNoAddHydrogens;

  // ////////////////////// supporting methods for compilation and loading
  // //////////

  private boolean compileScript(String filename, String strScript,
                                boolean debugCompiler) {
    scriptFileName = filename;
    strScript = fixScriptPath(strScript, filename);
    restoreScriptContext(compiler.compile(filename, strScript, false, false,
        debugCompiler, false), false, false, false);
    isStateScript = (script.indexOf(JC.STATE_VERSION_STAMP) >= 0);
    forceNoAddHydrogens = (isStateScript && script.indexOf("pdbAddHydrogens") < 0);
    String s = script;
    pc = setScriptExtensions();
    if (!chk && viewer.scriptEditorVisible
        && strScript.indexOf(JC.SCRIPT_EDITOR_IGNORE) < 0)
      viewer.scriptStatus("");
    script = s;
    return !error;
  }

  private String fixScriptPath(String strScript, String filename) {
    if (filename != null && strScript.indexOf("$SCRIPT_PATH$") >= 0) {
      String path = filename;
      // we first check for paths into ZIP files and adjust accordingly
      int pt = Math.max(filename.lastIndexOf("|"), filename.lastIndexOf("/"));
      path = path.substring(0, pt + 1);
      strScript = TextFormat.simpleReplace(strScript, "$SCRIPT_PATH$/", path);
      // now replace the variable itself
      strScript = TextFormat.simpleReplace(strScript, "$SCRIPT_PATH$", path);
    }
    return strScript;
  }

  private int setScriptExtensions() {
    String extensions = scriptExtensions;
    if (extensions == null)
      return 0;
    int pt = extensions.indexOf("##SCRIPT_STEP");
    if (pt >= 0) {
      executionStepping = true;
    }
    pt = extensions.indexOf("##SCRIPT_START=");
    if (pt < 0)
      return 0;
    pt = Parser.parseInt(extensions.substring(pt + 15));
    if (pt == Integer.MIN_VALUE)
      return 0;
    for (pc = 0; pc < lineIndices.length; pc++) {
      if (lineIndices[pc][0] > pt || lineIndices[pc][1] >= pt)
        break;
    }
    if (pc > 0 && pc < lineIndices.length && lineIndices[pc][0] > pt)
      --pc;
    return pc;
  }

  public void runScript(String script) throws ScriptException {
    if (!viewer.isPreviewOnly())
      runScriptBuffer(script, outputBuffer);
  }

  private boolean compileScriptFileInternal(String filename, String localPath,
                                            String remotePath, String scriptPath) {
    // from "script" command, with push/pop surrounding or viewer
    if (filename.toLowerCase().indexOf("javascript:") == 0)
      return compileScript(filename, viewer.jsEval(filename.substring(11)),
          debugScript);
    String[] data = new String[2];
    data[0] = filename;
    if (!viewer.getFileAsStringBin(data, Integer.MAX_VALUE, false)) { // first opening
      setErrorMessage("io error reading " + data[0] + ": " + data[1]);
      return false;
    }
    if (("\n" + data[1]).indexOf("\nJmolManifest.txt\n") >= 0) {
      String path;
      if (filename.endsWith(".all.pngj") || filename.endsWith(".all.png")) {
        path = "|state.spt";
        filename += "|";
      } else {
        data[0] = filename += "|JmolManifest.txt";
        if (!viewer.getFileAsStringBin(data, Integer.MAX_VALUE, false)) { // second entry
          setErrorMessage("io error reading " + data[0] + ": " + data[1]);
          return false;
        }
        path = JmolBinary.getManifestScriptPath(data[1]);
      }
      if (path != null && path.length() > 0) {
        data[0] = filename = filename.substring(0, filename.lastIndexOf("|"))
            + path;
        if (!viewer.getFileAsStringBin(data, Integer.MAX_VALUE, false)) { // third entry
          setErrorMessage("io error reading " + data[0] + ": " + data[1]);
          return false;
        }
      }
    }
    scriptFileName = filename;
    data[1] = JmolBinary.getEmbeddedScript(data[1]);
    String script = fixScriptPath(data[1], data[0]);
    if (scriptPath == null) {
      scriptPath = viewer.getFilePath(filename, false);
      scriptPath = scriptPath.substring(0, Math.max(
          scriptPath.lastIndexOf("|"), scriptPath.lastIndexOf("/")));
    }
    script = FileManager.setScriptFileReferences(script, localPath, remotePath,
        scriptPath);
    return compileScript(filename, script, debugScript);
  }

  // ///////////// Jmol parameter / user variable / function support
  // ///////////////

  @SuppressWarnings("unchecked")
  private Object getParameter(String key, int tokType) {
    Object v = getContextVariableAsVariable(key);
    if (v == null)
      v = viewer.getParameter(key);
    switch (tokType) {
    case T.variable:
      return SV.getVariable(v);
    case T.string:
      if (!(v instanceof JmolList<?>))
        break;
      JmolList<SV> sv = (JmolList<SV>) v;
      SB sb = new SB();
      for (int i = 0; i < sv.size(); i++)
        sb.append(sv.get(i).asString()).appendC('\n');
      return sb.toString();
    }
    return (v instanceof SV ? SV
        .oValue((SV) v) : v);
  }

  private String getParameterEscaped(String var) {
    SV v = getContextVariableAsVariable(var);
    return (v == null ? "" + viewer.getParameterEscaped(var) : v.escape());
  }

  private String getStringParameter(String var, boolean orReturnName) {
    SV v = getContextVariableAsVariable(var);
    if (v != null)
      return v.asString();
    String val = "" + viewer.getParameter(var);
    return (val.length() == 0 && orReturnName ? var : val);
  }

  private Object getNumericParameter(String var) {
    if (var.equalsIgnoreCase("_modelNumber")) {
      int modelIndex = viewer.getCurrentModelIndex();
      return Integer.valueOf(modelIndex < 0 ? 0 : viewer
          .getModelFileNumber(modelIndex));
    }
    SV v = getContextVariableAsVariable(var);
    if (v == null) {
      Object val = viewer.getParameter(var);
      if (!(val instanceof String))
        return val;
      v = SV.newVariable(T.string, val);
    }
    return SV.nValue(v);
  }

  private SV getContextVariableAsVariable(String var) {
    if (var.equals("expressionBegin"))
      return null;
    var = var.toLowerCase();
    if (contextVariables != null && contextVariables.containsKey(var))
      return contextVariables.get(var);
    ScriptContext context = thisContext;
    while (context != null) {
      if (context.isFunction == true)
        return null;
      if (context.contextVariables != null
          && context.contextVariables.containsKey(var))
        return context.contextVariables.get(var);
      context = context.parentContext;
    }
    return null;
  }

  private Object getStringObjectAsVariable(String s, String key) {
    if (s == null || s.length() == 0)
      return s;
    Object v = SV.unescapePointOrBitsetAsVariable(s);
    if (v instanceof String && key != null)
      v = viewer.setUserVariable(key, SV.newVariable(T.string, v));
    return v;
  }

  private JmolParallelProcessor parallelProcessor;

  @SuppressWarnings("unchecked")
  public float evalFunctionFloat(Object func, Object params, float[] values) {
    try {
      JmolList<SV> p = (JmolList<SV>) params;
      for (int i = 0; i < values.length; i++)
        p.get(i).value = Float.valueOf(values[i]);
      ScriptFunction f = (ScriptFunction) func;
      return SV
          .fValue(runFunctionRet(f, f.name, p, null, true, false, false));
    } catch (Exception e) {
      return Float.NaN;
    }

  }

  static int tryPt;
  
  SV runFunctionRet(JmolScriptFunction function, String name,
                                JmolList<SV> params,
                                SV tokenAtom, boolean getReturn,
                                boolean setContextPath, boolean allowThreads)
      throws ScriptException {
    if (function == null) {
      // general function call
      function = viewer.getFunction(name);
      if (function == null)
        return null;
      if (setContextPath)
        contextPath += " >> function " + name;
    } else if (setContextPath) {
      // "try"; not from evalFunctionFloat
      contextPath += " >> " + name;
    }

    pushContext(null);
    if (allowJSThreads)
      allowJSThreads = allowThreads;
    boolean isTry = (function.getTok() == T.trycmd);
    thisContext.isTryCatch = isTry;
    thisContext.isFunction = !isTry;
    functionName = name;
    if (isTry) {
      viewer.resetError();
      thisContext.displayLoadErrorsSave = viewer.displayLoadErrors;
      thisContext.tryPt = ++tryPt;
      viewer.displayLoadErrors = false;
      restoreFunction(function, params, tokenAtom);
      contextVariables.put("_breakval",
          new ScriptVariableInt(Integer.MAX_VALUE));
      contextVariables.put("_errorval", SV.newVariable(
          T.string, ""));
      Map<String, SV> cv = contextVariables;
      executeCommands(true);
      //JavaScript will not return here after DELAY
      while (thisContext.tryPt != tryPt)
        popContext(false, false);
      processTry(cv);
      return null;
    } else if (function instanceof JmolParallelProcessor) {
      synchronized (function) // can't do this -- too general
      {
        parallelProcessor = (JmolParallelProcessor) function;
        restoreFunction(function, params, tokenAtom);
        dispatchCommands(false, true); // to load the processes
        ((JmolParallelProcessor) function).runAllProcesses(viewer);
      }
    } else {
      restoreFunction(function, params, tokenAtom);
      dispatchCommands(false, true);
      //JavaScript will not return here after DELAY or after what???
    }
    SV v = (getReturn ? getContextVariableAsVariable("_retval")
        : null);
    popContext(false, false);
    return v;
  }

  private void processTry(Map<String, SV> cv) throws ScriptException {
    viewer.displayLoadErrors = thisContext.displayLoadErrorsSave;
    popContext(false, false);
    String err = (String) viewer.getParameter("_errormessage");
    if (err.length() > 0) {
      cv.put("_errorval", SV.newVariable(
          T.string, err));
      viewer.resetError();
    }
    cv.put("_tryret", cv.get("_retval"));
    SV ret = cv.get("_tryret");
    if (ret.value != null || ret.intValue != Integer.MAX_VALUE) {
      returnCmd(ret);
      return;
    }
    String errMsg = (String) (cv.get("_errorval")).value;
    if (errMsg.length() == 0) {
      int iBreak = (cv.get("_breakval")).intValue;
      if (iBreak != Integer.MAX_VALUE) {
        breakCmd(pc - iBreak);
        return;
      }
    }
    // normal return will skip the catch
    if (pc + 1 < aatoken.length && aatoken[pc + 1][0].tok == T.catchcmd) {
      // set the intValue positive to indicate "not done" for the IF evaluation
      ContextToken ct = (ContextToken) aatoken[pc + 1][0];
      if (ct.contextVariables != null && ct.name0 != null)
        ct.contextVariables.put(ct.name0, SV
            .newVariable(T.string, errMsg));
      ct.intValue = (errMsg.length() > 0 ? 1 : -1) * Math.abs(ct.intValue);
    }
  }

  /**
   * note that functions requiring motion cannot be run 
   * in JavaScript
   * 
   * @param f
   * @param params
   * @param tokenAtom
   * @throws ScriptException
   */
  private void restoreFunction(JmolScriptFunction f,
                           JmolList<SV> params, 
                           SV tokenAtom)
      throws ScriptException {
    ScriptFunction function = (ScriptFunction) f;
    aatoken = function.aatoken;
    lineNumbers = function.lineNumbers;
    lineIndices = function.lineIndices;
    script = function.script;
    pc = 0;
    if (function.names != null) {
      contextVariables = new Hashtable<String, SV>();
      function.setVariables(contextVariables, params);
    }
    if (tokenAtom != null)
      contextVariables.put("_x", tokenAtom);
  }

  private void clearDefinedVariableAtomSets() {
    definedAtomSets.remove("# variable");
  }

  /**
   * support for @xxx or define xxx commands
   * 
   */
  private void defineSets() {
    if (!definedAtomSets.containsKey("# static")) {
      for (int i = 0; i < JC.predefinedStatic.length; i++)
        defineAtomSet(JC.predefinedStatic[i]);
      defineAtomSet("# static");
    }
    if (definedAtomSets.containsKey("# variable"))
      return;
    for (int i = 0; i < JC.predefinedVariable.length; i++)
      defineAtomSet(JC.predefinedVariable[i]);
    // Now, define all the elements as predefined sets

    // name ==> elemno=n for all standard elements, isotope-blind
    // _Xx ==> elemno=n for of all elements, isotope-blind
    for (int i = Elements.elementNumberMax; --i >= 0;) {
      String definition = " elemno=" + i;
      defineAtomSet("@" + Elements.elementNameFromNumber(i) + definition);
      defineAtomSet("@_" + Elements.elementSymbolFromNumber(i) + definition);
    }
    // name ==> _e=nn for each alternative element
    for (int i = Elements.firstIsotope; --i >= 0;) {
      String definition = "@" + Elements.altElementNameFromIndex(i) + " _e="
          + Elements.altElementNumberFromIndex(i);
      defineAtomSet(definition);
    }
    // these variables _e, _x can't be more than two characters
    // name ==> _isotope=iinn for each isotope
    // _T ==> _isotope=iinn for each isotope
    // _3H ==> _isotope=iinn for each isotope
    for (int i = Elements.altElementMax; --i >= Elements.firstIsotope;) {
      short ei = Elements.altElementNumberFromIndex(i);
      String def = " _e=" + ei;
      String definition = "@_" + Elements.altElementSymbolFromIndex(i);
      defineAtomSet(definition + def);
      
      definition = "@_" + Elements.altIsotopeSymbolFromIndex(i);
      defineAtomSet(definition + def);
      definition = "@_" + Elements.altIsotopeSymbolFromIndex2(i);
      defineAtomSet(definition + def);
      
      definition = "@" + Elements.altElementNameFromIndex(i);
      if (definition.length() > 1)
        defineAtomSet(definition + def);
      
      // @_12C _e=6
      // @_C12 _e=6
      short e = Elements.getElementNumber(ei);
      ei = (short) Elements.getNaturalIsotope(e);
      if (ei > 0) {
        def = Elements.elementSymbolFromNumber(e);
        defineAtomSet("@_" + def + ei + " _e=" + e);
        defineAtomSet("@_" + ei + def + " _e=" + e);
      }
    }
    defineAtomSet("# variable");
  }

  private void defineAtomSet(String script) {
    if (script.indexOf("#") == 0) {
      definedAtomSets.put(script, Boolean.TRUE);
      return;
    }
    ScriptContext sc = compiler.compile("#predefine", script, true, false,
        false, false);
    if (sc.errorType != null) {
      viewer
          .scriptStatus("JmolConstants.java ERROR: predefined set compile error:"
              + script + "\ncompile error:" + sc.errorMessageUntranslated);
      return;
    }

    if (sc.aatoken.length != 1) {
      viewer
          .scriptStatus("JmolConstants.java ERROR: predefinition does not have exactly 1 command:"
              + script);
      return;
    }
    T[] statement = sc.aatoken[0];
    if (statement.length <= 2) {
      viewer.scriptStatus("JmolConstants.java ERROR: bad predefinition length:"
          + script);
      return;
    }
    int tok = statement[1].tok;
    if (!T.tokAttr(tok, T.identifier)
        && !T.tokAttr(tok, T.predefinedset)) {
      viewer.scriptStatus("JmolConstants.java ERROR: invalid variable name:"
          + script);
      return;
    }
    String name = ((String) statement[1].value).toLowerCase();
    if (name.startsWith("dynamic_"))
      name = "!" + name.substring(8);
    definedAtomSets.put(name, statement);
  }

  private BS lookupIdentifierValue(String identifier)
      throws ScriptException {
    // all variables and possible residue names for PDB
    // or atom names for non-pdb atoms are processed here.

    // priority is given to a defined variable.

    BS bs = lookupValue(identifier, false);
    if (bs != null)
      return BSUtil.copy(bs);

    // next we look for names of groups (PDB) or atoms (non-PDB)
    bs = getAtomBits(T.identifier, identifier);
    return (bs == null ? new BS() : bs);
  }

  private BS lookupValue(String setName, boolean plurals)
      throws ScriptException {
    if (chk) {
      return new BS();
    }
    defineSets();
    setName = setName.toLowerCase();
    Object value = definedAtomSets.get(setName);
    boolean isDynamic = false;
    if (value == null) {
      value = definedAtomSets.get("!" + setName);
      isDynamic = (value != null);
    }
    if (value instanceof BS)
      return (BS) value;
    if (value instanceof T[]) { // j2s OK -- any Array here
      pushContext(null);
      BS bs = atomExpression((T[]) value, -2, 0, true, false, true,
          true);
      popContext(false, false);
      if (!isDynamic)
        definedAtomSets.put(setName, bs);
      return bs;
    }
    if (plurals)
      return null;
    int len = setName.length();
    if (len < 5) // iron is the shortest
      return null;
    if (setName.charAt(len - 1) != 's')
      return null;
    if (setName.endsWith("ies"))
      setName = setName.substring(0, len - 3) + 'y';
    else
      setName = setName.substring(0, len - 1);
    return lookupValue(setName, true);
  }

  public void deleteAtomsInVariables(BS bsDeleted) {
    for (Map.Entry<String, Object> entry : definedAtomSets.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof BS) {
        BSUtil.deleteBits((BS) value, bsDeleted);
        if (!entry.getKey().startsWith("!"))
          viewer.setUserVariable("@" + entry.getKey(), SV
              .newVariable(T.bitset, value));
      }
    }
  }

  /**
   * provides support for @x and @{....} in statements. The compiler passes on
   * these, because they must be integrated with the statement dynamically.
   * 
   * @param pc
   * @return a fixed token set -- with possible overrun of unused null tokens
   * 
   * @throws ScriptException
   */
  @SuppressWarnings("unchecked")
  private boolean setStatement(int pc) throws ScriptException {
    st = aatoken[pc];
    slen = st.length;
    if (slen == 0)
      return true;
    T[] fixed;
    int i;
    int tok;
    for (i = 1; i < slen; i++) {
      if (st[i] == null) {
        slen = i;
        return true;
      }
      if (st[i].tok == T.define)
        break;
    }
    if (i == slen)// || isScriptCheck)
      return i == slen;
    switch (st[0].tok) {
    case T.parallel:
    case T.function:
    case T.identifier:
      if (tokAt(1) == T.leftparen)
        return true;
    }
    fixed = new T[slen];
    fixed[0] = st[0];
    boolean isExpression = false;
    int j = 1;
    for (i = 1; i < slen; i++) {
      if (st[i] == null)
        continue;
      switch (tok = getToken(i).tok) {
      default:
        fixed[j] = st[i];
        break;
      case T.expressionBegin:
      case T.expressionEnd:
        // @ in expression will be taken as SELECT
        isExpression = (tok == T.expressionBegin);
        fixed[j] = st[i];
        break;
      case T.define:
        if (++i == slen)
          error(ERROR_invalidArgument);
        Object v;
        // compiler can indicate that a definition MUST
        // be interpreted as a String
        boolean forceString = (theToken.intValue == T.string);
        // Object var_set;
        String s;
        String var = parameterAsString(i);
        boolean isClauseDefine = (tokAt(i) == T.expressionBegin);
        boolean isSetAt = (j == 1 && st[0] == T.tokenSetCmd);
        if (isClauseDefine) {
          SV vt = parameterExpressionToken(++i);
          i = iToken;
          v = (vt.tok == T.varray ? vt : SV.oValue(vt));
        } else {
          if (tokAt(i) == T.integer) {
            v = viewer.getAtomBits(T.atomno, Integer
                .valueOf(st[i].intValue));
          } else {
            v = getParameter(var, 0);
          }
          if (!isExpression && !isSetAt)
            isClauseDefine = true;
        }
        tok = tokAt(0);
        forceString |= (T.tokAttr(tok, T.implicitStringCommand) || tok == T.script); // for the file names
        if (v instanceof SV) {
          // select @{...}
          fixed[j] = (T) v;
          if (isExpression && fixed[j].tok == T.varray) {
            BS bs = SV.getBitSet((SV) v, true);
            // I can't remember why we have to be checking list variables
            // for atom names. 
            fixed[j] = SV.newVariable(T.bitset,
                bs == null ? getAtomBitSet(SV
                    .sValue(fixed[j])) : bs);
          }
        } else if (v instanceof Boolean) {
          fixed[j] = (((Boolean) v).booleanValue() ? T.tokenOn
              : T.tokenOff);
        } else if (v instanceof Integer) {
          // if (isExpression && !isClauseDefine
          // && (var_set = getParameter(var + "_set", false)) != null)
          // fixed[j] = new Token(Token.define, "" + var_set);
          // else
          fixed[j] = T.t(T.integer, ((Integer) v).intValue(), v);

        } else if (v instanceof Float) {
          fixed[j] = T.t(T.decimal, getFloatEncodedInt("" + v), v);
        } else if (v instanceof String) {
          if (!forceString) {
            if ((tok != T.set || j > 1 && st[1].tok != T.echo)
                && T.tokAttr(tok, T.mathExpressionCommand)) {
              v = getParameter((String) v, T.variable);
            }
            if (v instanceof String) {
              v = getStringObjectAsVariable((String) v, null);
            }
          }
          if (v instanceof SV) {
            // was a bitset 
            fixed[j] = (T) v;
          } else {
            s = (String) v;
            if (isExpression && !forceString) {
              // select @x  where x is "arg", for example
              fixed[j] = T.o(T.bitset, getAtomBitSet(s));
            } else {
              if (!isExpression) {
                //print @x
              }

              // bit of a hack here....
              // identifiers cannot have periods; file names can, though
              // TODO: this is still a hack
              // what we really need to know is what the compiler
              // expects here -- a string or an identifier, because
              // they will be processed differently.
              // a filename with only letters and numbers will be
              // read incorrectly here as an identifier.

              // note that command keywords cannot be implemented as variables
              // because they are not Token.identifiers in the first place.
              // but the identifier tok is important here because just below
              // there is a check for SET parameter name assignments.
              // even those may not work...

              tok = (isSetAt ? T.getTokFromName(s) : isClauseDefine
                  || forceString || s.length() == 0 || s.indexOf(".") >= 0
                  || s.indexOf(" ") >= 0 || s.indexOf("=") >= 0
                  || s.indexOf(";") >= 0 || s.indexOf("[") >= 0
                  || s.indexOf("{") >= 0 ? T.string : T.identifier);
              fixed[j] = T.o(tok, v);
            }
          }
        } else if (v instanceof BS) {
          fixed[j] = SV.newVariable(T.bitset, v);
        } else if (v instanceof P3) {
          fixed[j] = SV.newVariable(T.point3f, v);
        } else if (v instanceof Point4f) {
          fixed[j] = SV.newVariable(T.point4f, v);
        } else if (v instanceof Matrix3f) {
          fixed[j] = SV.newVariable(T.matrix3f, v);
        } else if (v instanceof Matrix4f) {
          fixed[j] = SV.newVariable(T.matrix4f, v);
        } else if (v instanceof Map<?, ?>) {
          fixed[j] = SV.newVariable(T.hash, v);
        } else if (v instanceof JmolList<?>) {
          JmolList<SV> sv = (JmolList<SV>) v;
          BS bs = null;
          for (int k = 0; k < sv.size(); k++) {
            SV svk = sv.get(k);
            if (svk.tok != T.bitset) {
              bs = null;
              break;
            }
            if (bs == null)
              bs = new BS();
            bs.or((BS) svk.value);
          }
          fixed[j] = (bs == null ? SV.getVariable(v) : T.o(
              T.bitset, bs));
        } else {
          P3 center = getObjectCenter(var, Integer.MIN_VALUE,
              Integer.MIN_VALUE);
          if (center == null)
            error(ERROR_invalidArgument);
          fixed[j] = T.o(T.point3f, center);
        }
        if (isSetAt && !T.tokAttr(fixed[j].tok, T.setparam))
          error(ERROR_invalidArgument);
        break;
      }

      j++;
    }
    st = fixed;
    for (i = j; i < st.length; i++)
      st[i] = null;
    slen = j;

    return true;
  }

  // ///////////////// Script context support //////////////////////

  private void clearState(boolean tQuiet) {
    thisContext = null;
    scriptLevel = 0;
    setErrorMessage(null);
    contextPath = "";
    this.tQuiet = tQuiet;
  }

  private ScriptContext thisContext;
  
  public ScriptContext getThisContext() {
    return thisContext;
  }

  public void pushContextDown() {
    scriptLevel--;
    pushContext2(null);
  }
  
  private void pushContext(ContextToken token) throws ScriptException {
    if (scriptLevel == scriptLevelMax)
      error(ERROR_tooManyScriptLevels);
    pushContext2(token);
  }
  
  private void pushContext2(ContextToken token) {
    thisContext = getScriptContext();
    thisContext.token = token;
    if (token == null) {
      scriptLevel = ++thisContext.scriptLevel;
    } else {
      thisContext.scriptLevel = -1;
      contextVariables = new Hashtable<String, SV>();
      if (token.contextVariables != null)
        for (String key : token.contextVariables.keySet())
          ScriptCompiler.addContextVariable(contextVariables, key);
    }
    if (debugScript || isCmdLine_c_or_C_Option)
      Logger.info("-->>-------------".substring(0, Math
          .max(17, scriptLevel + 5))
          + scriptLevel + " " + scriptFileName + " " + token + " " + thisContext);
  }

  public ScriptContext getScriptContext() {
    ScriptContext context = new ScriptContext();
    context.scriptLevel = scriptLevel;
    context.parentContext = thisContext;
    context.contextPath = contextPath;
    context.scriptFileName = scriptFileName;
    context.parallelProcessor = parallelProcessor;
    context.functionName = functionName;
    context.script = script;
    context.lineNumbers = lineNumbers;
    context.lineIndices = lineIndices;
    context.aatoken = aatoken;

    context.statement = st;
    context.statementLength = slen;
    context.pc = pc;
    context.lineEnd = lineEnd;
    context.pcEnd = pcEnd;
    context.iToken = iToken;
    context.outputBuffer = outputBuffer;
    context.contextVariables = contextVariables;
    context.isStateScript = isStateScript;

    context.errorMessage = errorMessage;
    context.errorType = errorType;
    context.iCommandError = iCommandError;
    context.chk = chk;
    context.executionStepping = executionStepping;
    context.executionPaused = executionPaused;
    context.scriptExtensions = scriptExtensions;

    context.mustResumeEval = mustResumeEval;
    context.allowJSThreads = allowJSThreads;
    return context;
  }

  void popContext(boolean isFlowCommand, boolean statementOnly) {
    if (thisContext == null)
      return;
    if (thisContext.scriptLevel > 0)
      scriptLevel = thisContext.scriptLevel - 1;
    // we must save (and thus NOT restore) the current statement
    // business when doing push/pop for commands like FOR and WHILE
    ScriptContext scTemp = (isFlowCommand ? getScriptContext() : null);
    restoreScriptContext(thisContext, true, isFlowCommand, statementOnly);
    if (scTemp != null)
      restoreScriptContext(scTemp, true, false, true);
    if (debugScript || isCmdLine_c_or_C_Option)
      Logger.info("--<<-------------".substring(0, Math
          .max(17, scriptLevel + 5))
          + scriptLevel
          + " "
          + scriptFileName
          + " "
          + (thisContext == null ? "" : "" + thisContext.token)
          + " "
          + thisContext);
  }

  private void restoreScriptContext(ScriptContext context,
                                    boolean isPopContext,
                                    boolean isFlowCommand, boolean statementOnly) {

    if (context == null)
      return;
    if (!isFlowCommand) {
      st = context.statement;
      slen = context.statementLength;
      pc = context.pc;
      lineEnd = context.lineEnd;
      pcEnd = context.pcEnd;
      if (statementOnly)
        return;
    }
    mustResumeEval = context.mustResumeEval;
    script = context.script;
    lineNumbers = context.lineNumbers;
    lineIndices = context.lineIndices;
    aatoken = context.aatoken;
    contextVariables = context.contextVariables;
    scriptExtensions = context.scriptExtensions;
    
    if (isPopContext) {
      contextPath = context.contextPath;
      scriptFileName = context.scriptFileName;
      parallelProcessor = context.parallelProcessor;
      functionName = context.functionName;
      iToken = context.iToken;
      outputBuffer = context.outputBuffer;
      isStateScript = context.isStateScript;
      thisContext = context.parentContext;
      allowJSThreads = context.allowJSThreads;
    } else {
      error = (context.errorType != null);
      errorMessage = context.errorMessage;
      errorMessageUntranslated = context.errorMessageUntranslated;
      iCommandError = context.iCommandError;
      errorType = context.errorType;
    }
  }

  private String getContext(boolean withVariables) {
    SB sb = new SB();
    ScriptContext context = thisContext;
    while (context != null) {
      if (withVariables) {
        if (context.contextVariables != null) {
          sb.append(getScriptID(context));
          sb.append(StateManager.getVariableList(context.contextVariables, 80,
              true, false));
        }
      } else {
        sb.append(setErrorLineMessage(context.functionName, context.scriptFileName,
            getLinenumber(context), context.pc, statementAsString(
                context.statement, -9999, logMessages)));
      }
      context = context.parentContext;
    }
    if (withVariables) {
      if (contextVariables != null) {
        sb.append(getScriptID(null));
        sb.append(StateManager.getVariableList(contextVariables, 80, true,
            false));
      }
    } else {
      sb.append(setErrorLineMessage(functionName, scriptFileName,
          getLinenumber(null), pc, statementAsString(st, -9999,
              logMessages)));
    }

    return sb.toString();
  }

  private int getLinenumber(ScriptContext c) {
    return (c == null ? lineNumbers[pc] : c.lineNumbers[c.pc]);
  }

  private String getScriptID(ScriptContext context) {
    String fuName = (context == null ? functionName : "function "
        + context.functionName);
    String fiName = (context == null ? scriptFileName : context.scriptFileName);
    return "\n# " + fuName + " (file " + fiName + ")\n";
  }

  // /////////////// error message support /////////////////

  public void setException(ScriptException sx, String msg, String untranslated) {
    // from ScriptException, while initializing
    sx.untranslated = (untranslated == null ? msg : untranslated);
    errorType = msg;
    iCommandError = pc;
    if (sx.message == null) {
      sx.message = "";
      return;
    }
    String s = ScriptEvaluator.getContextTrace(getScriptContext(), null, true).toString();
    while (thisContext != null && !thisContext.isTryCatch)
      popContext(false, false);
    sx.message += s;
    sx.untranslated += s;
    if (thisContext != null || chk
        || msg.indexOf("file recognized as a script file:") >= 0)
      return;
    Logger.error("eval ERROR: " + toString());
    if (viewer.autoExit)
      viewer.exitJmol();
  }

  private boolean error;
  private String errorMessage;
  protected String errorMessageUntranslated;
  protected String errorType;
  protected int iCommandError;

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getErrorMessageUntranslated() {
    return errorMessageUntranslated == null ? errorMessage
        : errorMessageUntranslated;
  }

  private void setErrorMessage(String err) {
    errorMessageUntranslated = null;
    if (err == null) {
      error = false;
      errorType = null;
      errorMessage = null;
      iCommandError = -1;
      return;
    }
    error = true;
    if (errorMessage == null) // there could be a compiler error from a script
      // command
      errorMessage = GT._("script ERROR: ");
    errorMessage += err;
  }

  private boolean ignoreError;

  private void planeExpected() throws ScriptException {
    errorMore(ERROR_planeExpected, "{a b c d}",
        "\"xy\" \"xz\" \"yz\" \"x=...\" \"y=...\" \"z=...\"", "$xxxxx");
  }

  private void integerOutOfRange(int min, int max) throws ScriptException {
    errorStr2(ERROR_integerOutOfRange, "" + min, "" + max);
  }

  private void numberOutOfRange(float min, float max) throws ScriptException {
    errorStr2(ERROR_numberOutOfRange, "" + min, "" + max);
  }

  void errorAt(int iError, int i) throws ScriptException {
    iToken = i;
    errorOrWarn(iError, null, null, null, false);
  }

  void error(int iError) throws ScriptException {
    errorOrWarn(iError, null, null, null, false);
  }

  void errorStr(int iError, String value) throws ScriptException {
    errorOrWarn(iError, value, null, null, false);
  }

  void errorStr2(int iError, String value, String more) throws ScriptException {
    errorOrWarn(iError, value, more, null, false);
  }

  void errorMore(int iError, String value, String more, String more2)
      throws ScriptException {
    errorOrWarn(iError, value, more, more2, false);
  }

  private void warning(int iError, String value, String more)
      throws ScriptException {
    errorOrWarn(iError, value, more, null, true);
  }

  void errorOrWarn(int iError, String value, String more, String more2,
             boolean warningOnly) throws ScriptException {
    String strError = ignoreError ? null : errorString(iError, value, more,
        more2, true);
    String strUntranslated = (!ignoreError && GT.getDoTranslate() ? errorString(
        iError, value, more, more2, false)
        : null);
    if (!warningOnly)
      evalError(strError, strUntranslated);
    showString(strError);
  }

  void evalError(String message, String strUntranslated) throws ScriptException {
    if (ignoreError)
      throw new NullPointerException();
    if (!chk) {
      // String s = viewer.getSetHistory(1);
      // viewer.addCommand(s + CommandHistory.ERROR_FLAG);
      setCursorWait(false);
      viewer.setBooleanProperty("refreshing", true);
      viewer.setStringProperty("_errormessage", strUntranslated);
    }
    throw new ScriptException(this, message, strUntranslated, true);
  }

  final static int ERROR_axisExpected = 0;
  final static int ERROR_backgroundModelError = 1;
  final static int ERROR_badArgumentCount = 2;
  final static int ERROR_badMillerIndices = 3;
  final static int ERROR_badRGBColor = 4;
  final static int ERROR_booleanExpected = 5;
  final static int ERROR_booleanOrNumberExpected = 6;
  final static int ERROR_booleanOrWhateverExpected = 7;
  final static int ERROR_colorExpected = 8;
  final static int ERROR_colorOrPaletteRequired = 9;
  final static int ERROR_commandExpected = 10;
  final static int ERROR_coordinateOrNameOrExpressionRequired = 11;
  final static int ERROR_drawObjectNotDefined = 12;
  final static int ERROR_endOfStatementUnexpected = 13;
  final static int ERROR_expressionExpected = 14;
  final static int ERROR_expressionOrIntegerExpected = 15;
  final static int ERROR_filenameExpected = 16;
  final static int ERROR_fileNotFoundException = 17;
  final static int ERROR_incompatibleArguments = 18;
  final static int ERROR_insufficientArguments = 19;
  final static int ERROR_integerExpected = 20;
  final static int ERROR_integerOutOfRange = 21;
  final static int ERROR_invalidArgument = 22;
  final static int ERROR_invalidParameterOrder = 23;
  final static int ERROR_keywordExpected = 24;
  final static int ERROR_moCoefficients = 25;
  final static int ERROR_moIndex = 26;
  final static int ERROR_moModelError = 27;
  final static int ERROR_moOccupancy = 28;
  final static int ERROR_moOnlyOne = 29;
  final static int ERROR_multipleModelsDisplayedNotOK = 30;
  final static int ERROR_noData = 31;
  final static int ERROR_noPartialCharges = 32;
  final static int ERROR_noUnitCell = 33;
  final static int ERROR_numberExpected = 34;
  final static int ERROR_numberMustBe = 35;
  final static int ERROR_numberOutOfRange = 36;
  final static int ERROR_objectNameExpected = 37;
  final static int ERROR_planeExpected = 38;
  final static int ERROR_propertyNameExpected = 39;
  final static int ERROR_spaceGroupNotFound = 40;
  final static int ERROR_stringExpected = 41;
  final static int ERROR_stringOrIdentifierExpected = 42;
  final static int ERROR_tooManyPoints = 43;
  final static int ERROR_tooManyScriptLevels = 44;
  final static int ERROR_unrecognizedAtomProperty = 45;
  final static int ERROR_unrecognizedBondProperty = 46;
  final static int ERROR_unrecognizedCommand = 47;
  final static int ERROR_unrecognizedExpression = 48;
  final static int ERROR_unrecognizedObject = 49;
  final static int ERROR_unrecognizedParameter = 50;
  final static int ERROR_unrecognizedParameterWarning = 51;
  final static int ERROR_unrecognizedShowParameter = 52;
  final static int ERROR_what = 53;
  final static int ERROR_writeWhat = 54;
  final static int ERROR_multipleModelsNotOK = 55;
  final static int ERROR_cannotSet = 56;

  /**
   * @param iError
   * @param value
   * @param more
   * @param more2
   * @param translated
   * @return constructed error string
   * 
   */
  static String errorString(int iError, String value, String more,
                            String more2, boolean translated) {
    boolean doTranslate = false;
    if (!translated && (doTranslate = GT.getDoTranslate()) == true)
      GT.setDoTranslate(false);
    String msg;
    switch (iError) {
    default:
      msg = "Unknown error message number: " + iError;
      break;
    case ERROR_axisExpected:
      msg = GT._("x y z axis expected");
      break;
    case ERROR_backgroundModelError:
      msg = GT._("{0} not allowed with background model displayed");
      break;
    case ERROR_badArgumentCount:
      msg = GT._("bad argument count");
      break;
    case ERROR_badMillerIndices:
      msg = GT._("Miller indices cannot all be zero.");
      break;
    case ERROR_badRGBColor:
      msg = GT._("bad [R,G,B] color");
      break;
    case ERROR_booleanExpected:
      msg = GT._("boolean expected");
      break;
    case ERROR_booleanOrNumberExpected:
      msg = GT._("boolean or number expected");
      break;
    case ERROR_booleanOrWhateverExpected:
      msg = GT._("boolean, number, or {0} expected");
      break;
    case ERROR_cannotSet:
      msg = GT._("cannot set value");
      break;
    case ERROR_colorExpected:
      msg = GT._("color expected");
      break;
    case ERROR_colorOrPaletteRequired:
      msg = GT._("a color or palette name (Jmol, Rasmol) is required");
      break;
    case ERROR_commandExpected:
      msg = GT._("command expected");
      break;
    case ERROR_coordinateOrNameOrExpressionRequired:
      msg = GT._("{x y z} or $name or (atom expression) required");
      break;
    case ERROR_drawObjectNotDefined:
      msg = GT._("draw object not defined");
      break;
    case ERROR_endOfStatementUnexpected:
      msg = GT._("unexpected end of script command");
      break;
    case ERROR_expressionExpected:
      msg = GT._("valid (atom expression) expected");
      break;
    case ERROR_expressionOrIntegerExpected:
      msg = GT._("(atom expression) or integer expected");
      break;
    case ERROR_filenameExpected:
      msg = GT._("filename expected");
      break;
    case ERROR_fileNotFoundException:
      msg = GT._("file not found");
      break;
    case ERROR_incompatibleArguments:
      msg = GT._("incompatible arguments");
      break;
    case ERROR_insufficientArguments:
      msg = GT._("insufficient arguments");
      break;
    case ERROR_integerExpected:
      msg = GT._("integer expected");
      break;
    case ERROR_integerOutOfRange:
      msg = GT._("integer out of range ({0} - {1})");
      break;
    case ERROR_invalidArgument:
      msg = GT._("invalid argument");
      break;
    case ERROR_invalidParameterOrder:
      msg = GT._("invalid parameter order");
      break;
    case ERROR_keywordExpected:
      msg = GT._("keyword expected");
      break;
    case ERROR_moCoefficients:
      msg = GT._("no MO coefficient data available");
      break;
    case ERROR_moIndex:
      msg = GT._("An MO index from 1 to {0} is required");
      break;
    case ERROR_moModelError:
      msg = GT._("no MO basis/coefficient data available for this frame");
      break;
    case ERROR_moOccupancy:
      msg = GT._("no MO occupancy data available");
      break;
    case ERROR_moOnlyOne:
      msg = GT._("Only one molecular orbital is available in this file");
      break;
    case ERROR_multipleModelsDisplayedNotOK:
      msg = GT._("{0} require that only one model be displayed");
      break;
    case ERROR_multipleModelsNotOK:
      msg = GT._("{0} requires that only one model be loaded");
      break;
    case ERROR_noData:
      msg = GT._("No data available");
      break;
    case ERROR_noPartialCharges:
      msg = GT
          ._("No partial charges were read from the file; Jmol needs these to render the MEP data.");
      break;
    case ERROR_noUnitCell:
      msg = GT._("No unit cell");
      break;
    case ERROR_numberExpected:
      msg = GT._("number expected");
      break;
    case ERROR_numberMustBe:
      msg = GT._("number must be ({0} or {1})");
      break;
    case ERROR_numberOutOfRange:
      msg = GT._("decimal number out of range ({0} - {1})");
      break;
    case ERROR_objectNameExpected:
      msg = GT._("object name expected after '$'");
      break;
    case ERROR_planeExpected:
      msg = GT
          ._("plane expected -- either three points or atom expressions or {0} or {1} or {2}");
      break;
    case ERROR_propertyNameExpected:
      msg = GT._("property name expected");
      break;
    case ERROR_spaceGroupNotFound:
      msg = GT._("space group {0} was not found.");
      break;
    case ERROR_stringExpected:
      msg = GT._("quoted string expected");
      break;
    case ERROR_stringOrIdentifierExpected:
      msg = GT._("quoted string or identifier expected");
      break;
    case ERROR_tooManyPoints:
      msg = GT._("too many rotation points were specified");
      break;
    case ERROR_tooManyScriptLevels:
      msg = GT._("too many script levels");
      break;
    case ERROR_unrecognizedAtomProperty:
      msg = GT._("unrecognized atom property");
      break;
    case ERROR_unrecognizedBondProperty:
      msg = GT._("unrecognized bond property");
      break;
    case ERROR_unrecognizedCommand:
      msg = GT._("unrecognized command");
      break;
    case ERROR_unrecognizedExpression:
      msg = GT._("runtime unrecognized expression");
      break;
    case ERROR_unrecognizedObject:
      msg = GT._("unrecognized object");
      break;
    case ERROR_unrecognizedParameter:
      msg = GT._("unrecognized {0} parameter");
      break;
    case ERROR_unrecognizedParameterWarning:
      msg = GT
          ._("unrecognized {0} parameter in Jmol state script (set anyway)");
      break;
    case ERROR_unrecognizedShowParameter:
      msg = GT._("unrecognized SHOW parameter --  use {0}");
      break;
    case ERROR_what:
      msg = "{0}";
      break;
    case ERROR_writeWhat:
      msg = GT._("write what? {0} or {1} \"filename\"");
      break;
    }
    if (msg.indexOf("{0}") < 0) {
      if (value != null)
        msg += ": " + value;
    } else {
      msg = TextFormat.simpleReplace(msg, "{0}", value);
      if (msg.indexOf("{1}") >= 0)
        msg = TextFormat.simpleReplace(msg, "{1}", more);
      else if (more != null)
        msg += ": " + more;
      if (msg.indexOf("{2}") >= 0)
        msg = TextFormat.simpleReplace(msg, "{2}", more);
    }
    if (doTranslate)
      GT.setDoTranslate(true);
    return msg;
  }

  static String setErrorLineMessage(String functionName, String filename,
                                    int lineCurrent, int pcCurrent,
                                    String lineInfo) {
    String err = "\n----";
    if (filename != null || functionName != null)
      err += "line "
          + lineCurrent
          + " command "
          + (pcCurrent + 1)
          + " of "
          + (functionName == null ? filename
              : functionName.equals("try") ? "try" : "function " + functionName)
          + ":";
    err += "\n         " + lineInfo;
    return err;
  }

  @Override
  public String toString() {
    SB str = new SB();
    str.append("Eval\n pc:");
    str.appendI(pc);
    str.append("\n");
    str.appendI(aatoken.length);
    str.append(" statements\n");
    for (int i = 0; i < aatoken.length; ++i) {
      str.append("----\n");
      T[] atoken = aatoken[i];
      for (int j = 0; j < atoken.length; ++j) {
        str.appendO(atoken[j]);
        str.appendC('\n');
      }
      str.appendC('\n');
    }
    str.append("END\n");
    return str.toString();
  }

  static String statementAsString(T[] statement, int iTok,
                                  boolean doLogMessages) {
    if (statement.length == 0)
      return "";
    SB sb = new SB();
    int tok = statement[0].tok;
    switch (tok) {
    case T.nada:
      return (String) statement[0].value;
    case T.end:
      if (statement.length == 2
          && (statement[1].tok == T.function || statement[1].tok == T.parallel))
        return ((ScriptFunction) (statement[1].value)).toString();
    }
    boolean useBraces = true;// (!Token.tokAttr(tok,
    // Token.atomExpressionCommand));
    boolean inBrace = false;
    boolean inClauseDefine = false;
    boolean setEquals = (statement.length > 1 && tok == T.set
        && statement[0].value.equals("")
        && (statement[0].intValue == '=' || statement[0].intValue == '#') && statement[1].tok != T.expressionBegin);
    int len = statement.length;
    for (int i = 0; i < len; ++i) {
      T token = statement[i];
      if (token == null) {
        len = i;
        break;
      }
      if (iTok == i - 1)
        sb.append(" <<");
      if (i != 0)
        sb.appendC(' ');
      if (i == 2 && setEquals) {
        if ((setEquals = (token.tok != T.opEQ))
            || statement[0].intValue == '#') {
          sb.append(setEquals ? "= " : "== ");
          if (!setEquals)
            continue;
        }
      }
      if (iTok == i && token.tok != T.expressionEnd)
        sb.append(">> ");
      switch (token.tok) {
      case T.expressionBegin:
        if (useBraces)
          sb.append("{");
        continue;
      case T.expressionEnd:
        if (inClauseDefine && i == statement.length - 1)
          useBraces = false;
        if (useBraces)
          sb.append("}");
        continue;
      case T.leftsquare:
      case T.rightsquare:
        break;
      case T.leftbrace:
      case T.rightbrace:
        inBrace = (token.tok == T.leftbrace);
        break;
      case T.define:
        if (i > 0 && ((String) token.value).equals("define")) {
          sb.append("@");
          if (i + 1 < statement.length
              && statement[i + 1].tok == T.expressionBegin) {
            if (!useBraces)
              inClauseDefine = true;
            useBraces = true;
          }
          continue;
        }
        break;
      case T.on:
        sb.append("true");
        continue;
      case T.off:
        sb.append("false");
        continue;
      case T.select:
        break;
      case T.integer:
        sb.appendI(token.intValue);
        continue;
      case T.point3f:
      case T.point4f:
      case T.bitset:
        sb.append(SV.sValue(token)); // list
        continue;
      case T.varray:
      case T.hash:
        sb.append(((SV) token).escape()); // list
        continue;
      case T.seqcode:
        sb.appendC('^');
        continue;
      case T.spec_seqcode_range:
        if (token.intValue != Integer.MAX_VALUE)
          sb.appendI(token.intValue);
        else
          sb.append(Group.getSeqcodeString(getSeqCode(token)));
        token = statement[++i];
        sb.appendC(' ');
        // if (token.intValue == Integer.MAX_VALUE)
        sb.append(inBrace ? "-" : "- ");
        //$FALL-THROUGH$
      case T.spec_seqcode:
        if (token.intValue != Integer.MAX_VALUE)
          sb.appendI(token.intValue);
        else
          sb.append(Group.getSeqcodeString(getSeqCode(token)));
        continue;
      case T.spec_chain:
        sb.append("*:");
        sb.appendC((char) token.intValue);
        continue;
      case T.spec_alternate:
        sb.append("*%");
        if (token.value != null)
          sb.append(token.value.toString());
        continue;
      case T.spec_model:
        sb.append("*/");
        //$FALL-THROUGH$
      case T.spec_model2:
      case T.decimal:
        if (token.intValue < Integer.MAX_VALUE) {
          sb.append(Escape.escapeModelFileNumber(token.intValue));
        } else {
          sb.append("" + token.value);
        }
        continue;
      case T.spec_resid:
        sb.appendC('[');
        sb.append(Group.getGroup3((short) token.intValue));
        sb.appendC(']');
        continue;
      case T.spec_name_pattern:
        sb.appendC('[');
        sb.appendO(token.value);
        sb.appendC(']');
        continue;
      case T.spec_atom:
        sb.append("*.");
        break;
      case T.cell:
        if (token.value instanceof P3) {
          P3 pt = (P3) token.value;
          sb.append("cell=").append(Escape.eP(pt));
          continue;
        }
        break;
      case T.string:
        sb.append("\"").appendO(token.value).append("\"");
        continue;
      case T.opEQ:
      case T.opLE:
      case T.opGE:
      case T.opGT:
      case T.opLT:
      case T.opNE:
        // not quite right -- for "inmath"
        if (token.intValue == T.property) {
          sb.append((String) statement[++i].value).append(" ");
        } else if (token.intValue != Integer.MAX_VALUE)
          sb.append(T.nameOf(token.intValue)).append(" ");
        break;
      case T.trycmd:
        continue;
      case T.end:
        sb.append("end");
        continue;
      default:
        if (T.tokAttr(token.tok, T.identifier) || !doLogMessages)
          break;
        sb.appendC('\n').append(token.toString()).appendC('\n');
        continue;
      }
      if (token.value != null)
        sb.append(token.value.toString());
    }
    if (iTok >= len - 1 && iTok != 9999)
      sb.append(" <<");
    return sb.toString();
  }

  ///////////// shape get/set properties ////////////////

  private Object getShapeProperty(int shapeType, String propertyName) {
    return sm.getShapePropertyIndex(shapeType, propertyName,
        Integer.MIN_VALUE);
  }

  private boolean getShapePropertyData(int shapeType, String propertyName,
                                   Object[] data) {
    return sm.getShapePropertyData(shapeType, propertyName, data);
  }

  private Object getShapePropertyIndex(int shapeType, String propertyName, int index) {
    return sm.getShapePropertyIndex(shapeType, propertyName, index);
  }

  private void addShapeProperty(JmolList<Object[]> propertyList, String key,
                                Object value) {
    if (chk)
      return;
    //System.out.println("addshapeprop " + key + " " + value);
    propertyList.addLast(new Object[] { key, value });
  }

  private void setObjectMad(int iShape, String name, int mad) {
    if (chk)
      return;
    viewer.setObjectMad(iShape, name, mad);
  }

  private void setObjectArgb(String str, int argb) {
    if (chk)
      return;
    viewer.setObjectArgb(str, argb);
  }

  private void setShapeProperty(int shapeType, String propertyName,
                                Object propertyValue) {
    if (chk)
      return;
    sm.setShapePropertyBs(shapeType, propertyName, propertyValue, null);
  }

  private void setShapePropertyBs(int iShape, String propertyName,
                                Object propertyValue, BS bs) {
    if (chk)
      return;
    sm.setShapePropertyBs(iShape, propertyName, propertyValue, bs);
  }

  private void setShapeSizeBs(int shapeType, int size, BS bs) {
    // stars, halos, balls only
    if (chk)
      return;
    sm.setShapeSizeBs(shapeType, size, null, bs);
  }

  private void setShapeSize(int shapeType, RadiusData rd) {
    if (chk)
      return;
    sm.setShapeSizeBs(shapeType, 0, rd, null);
  }

  ////////////////////  setting properties ////////////////////////

  private void setBooleanProperty(String key, boolean value) {
    if (!chk)
      viewer.setBooleanProperty(key, value);
  }

  private boolean setIntProperty(String key, int value) {
    if (!chk)
      viewer.setIntProperty(key, value);
    return true;
  }

  private boolean setFloatProperty(String key, float value) {
    if (!chk)
      viewer.setFloatProperty(key, value);
    return true;
  }

  private void setStringProperty(String key, String value) {
    if (!chk)
      viewer.setStringProperty(key, value);
  }

  //////////////////// showing strings /////////////////

  private void showString(String str) {
    showStringPrint(str, false);
  }

  private void showStringPrint(String str, boolean isPrint) {
    if (chk || str == null)
      return;
    if (outputBuffer != null)
      outputBuffer.append(str).appendC('\n');
    else
      viewer.showString(str, isPrint);
  }

  private void scriptStatusOrBuffer(String s) {
    if (chk)
      return;
    if (outputBuffer != null) {
      outputBuffer.append(s).appendC('\n');
      return;
    }
    viewer.scriptStatus(s);
  }

  ///////////////// expression processing ///////////////////

  private T[] tempStatement;
  private boolean isBondSet;
  private Object expressionResult;

  private BS atomExpressionAt(int index) throws ScriptException {
    if (!checkToken(index))
      errorAt(ERROR_badArgumentCount, index);
    return atomExpression(st, index, 0, true, false, true, true);
  }

  /**
   * @param code
   * @param pcStart
   * @param pcStop
   * @param allowRefresh
   * @param allowUnderflow
   * @param mustBeBitSet
   * @param andNotDeleted
   *        IGNORED
   * @return atom bitset
   * @throws ScriptException
   */
  @SuppressWarnings("unchecked")
  private BS atomExpression(T[] code, int pcStart, int pcStop,
                                boolean allowRefresh, boolean allowUnderflow,
                                boolean mustBeBitSet, boolean andNotDeleted)
      throws ScriptException {
    // note that this is general -- NOT just statement[]
    // errors reported would improperly access statement/line context
    // there should be no errors anyway, because this is for
    // predefined variables, but it is conceivable that one could
    // have a problem.

    isBondSet = false;
    if (code != st) {
      tempStatement = st;
      st = code;
    }
    ScriptMathProcessor rpn = new ScriptMathProcessor(this, false, false,
        mustBeBitSet);
    Object val;
    int comparisonValue = Integer.MAX_VALUE;
    boolean refreshed = false;
    iToken = 1000;
    boolean ignoreSubset = (pcStart < 0);
    boolean isInMath = false;
    int nExpress = 0;
    int atomCount = viewer.getAtomCount();
    if (ignoreSubset)
      pcStart = -pcStart;
    ignoreSubset |= chk;
    if (pcStop == 0 && code.length > pcStart)
      pcStop = pcStart + 1;
    // if (logMessages)
    // viewer.scriptStatus("start to evaluate expression");
    expression_loop: for (int pc = pcStart; pc < pcStop; ++pc) {
      iToken = pc;
      T instruction = code[pc];
      if (instruction == null)
        break;
      Object value = instruction.value;
      // if (logMessages)
      // viewer.scriptStatus("instruction=" + instruction);
      switch (instruction.tok) {
      case T.expressionBegin:
        pcStart = pc;
        pcStop = code.length;
        nExpress++;
        break;
      case T.expressionEnd:
        nExpress--;
        if (nExpress > 0)
          continue;
        break expression_loop;
      case T.leftbrace:
        if (isPoint3f(pc)) {
          P3 pt = getPoint3f(pc, true);
          if (pt != null) {
            rpn.addXPt(pt);
            pc = iToken;
            break;
          }
        }
        break; // ignore otherwise
      case T.rightbrace:
        if (pc > 0 && code[pc - 1].tok == T.leftbrace)
          rpn.addXBs(new BS());
        break;
      case T.leftsquare:
        isInMath = true;
        rpn.addOp(instruction);
        break;
      case T.rightsquare:
        isInMath = false;
        rpn.addOp(instruction);
        break;
      case T.define:
        rpn.addXBs(getAtomBitSet(value));
        break;
      case T.hkl:
        rpn.addXVar(SV.newScriptVariableToken(instruction));
        rpn.addXVar(SV.newVariable(T.point4f, hklParameter(pc + 2)));
        pc = iToken;
        break;
      case T.plane:
        rpn.addXVar(SV.newScriptVariableToken(instruction));
        rpn.addXVar(SV.newVariable(T.point4f, planeParameter(pc + 2)));
        pc = iToken;
        break;
      case T.coord:
        rpn.addXVar(SV.newScriptVariableToken(instruction));
        rpn.addXPt(getPoint3f(pc + 2, true));
        pc = iToken;
        break;
      case T.string:
        String s = (String) value;
        if (s.indexOf("({") == 0) {
          BS bs = Escape.uB(s);
          if (bs != null) {
            rpn.addXBs(bs);
            break;
          }
        }
        rpn.addXVar(SV.newScriptVariableToken(instruction));
        // note that the compiler has changed all within() types to strings.
        if (s.equals("hkl")) {
          rpn.addXVar(SV.newVariable(T.point4f, hklParameter(pc + 2)));
          pc = iToken;
        }
        break;
      case T.smiles:
      case T.search:
      case T.substructure:
      case T.within:
      case T.contact:
      case T.connected:
      case T.comma:
        rpn.addOp(instruction);
        break;
      case T.all:
        rpn.addXBs(viewer.getModelUndeletedAtomsBitSet(-1));
        break;
      case T.none:
        rpn.addXBs(new BS());
        break;
      case T.on:
      case T.off:
        rpn.addXVar(SV.newScriptVariableToken(instruction));
        break;
      case T.selected:
        rpn.addXBs(BSUtil.copy(viewer.getSelectionSet(false)));
        break;
      case T.subset:
        BS bsSubset = viewer.getSelectionSubset();
        rpn.addXBs(bsSubset == null ? viewer.getModelUndeletedAtomsBitSet(-1)
            : BSUtil.copy(bsSubset));
        break;
      case T.hidden:
        rpn.addXBs(BSUtil.copy(viewer.getHiddenSet()));
        break;
      case T.fixed:
        rpn.addXBs(BSUtil.copy(viewer.getMotionFixedAtoms()));
        break;
      case T.displayed:
        rpn.addXBs(BSUtil.copyInvert(viewer.getHiddenSet(), atomCount));
        break;
      case T.basemodel:
        rpn.addXBs(viewer.getBaseModelBitSet());
        break;
      case T.visible:
        if (!chk && !refreshed)
          viewer.setModelVisibility();
        refreshed = true;
        rpn.addXBs(viewer.getVisibleSet());
        break;
      case T.clickable:
        // a bit different, because it requires knowing what got slabbed
        if (!chk && allowRefresh)
          refresh();
        rpn.addXBs(viewer.getClickableSet());
        break;
      case T.spec_atom:
        if (viewer.allowSpecAtom()) {
          int atomID = instruction.intValue;
          if (atomID > 0)
            rpn.addXBs(compareInt(T.atomid, T.opEQ, atomID));
          else
            rpn.addXBs(getAtomBits(instruction.tok, value));
        } else {
          // Chime legacy hack.  *.C for _C
          rpn.addXBs(lookupIdentifierValue("_" + value));
        }
        break;
      case T.carbohydrate:
      case T.dna:
      case T.hetero:
      case T.isaromatic:
      case T.nucleic:
      case T.protein:
      case T.purine:
      case T.pyrimidine:
      case T.rna:
      case T.spec_name_pattern:
      case T.spec_alternate:
      case T.specialposition:
      case T.symmetry:
      case T.unitcell:
        rpn.addXBs(getAtomBits(instruction.tok, value));
        break;
      case T.spec_model:
        // from select */1002 or */1000002 or */1.2
        // */1002 is equivalent to 1.2 when more than one file is present
      case T.spec_model2:
        // from just using the number 1.2
        int iModel = instruction.intValue;
        if (iModel == Integer.MAX_VALUE && value instanceof Integer) {
          // from select */n
          iModel = ((Integer) value).intValue();
          if (!viewer.haveFileSet()) {
            rpn.addXBs(getAtomBits(T.spec_model, Integer.valueOf(iModel)));
            break;
          }
          if (iModel <= 2147) // file number
            iModel = iModel * 1000000;
        }
        rpn.addXBs(bitSetForModelFileNumber(iModel));
        break;
      case T.spec_resid:
      case T.spec_chain:
        rpn
            .addXBs(getAtomBits(instruction.tok,
                Integer.valueOf(instruction.intValue)));
        break;
      case T.spec_seqcode:
        if (isInMath)
          rpn.addXNum(new ScriptVariableInt(instruction.intValue));
        else
          rpn.addXBs(getAtomBits(T.spec_seqcode, Integer.valueOf(
              getSeqCode(instruction))));
        break;
      case T.spec_seqcode_range:
        if (isInMath) {
          rpn.addXNum(new ScriptVariableInt(instruction.intValue));
          // TODO -- in 13.0 had addXObj this adds a "-" to the X stack. 
          rpn.addOp(T.tokenMinus);
          rpn.addXNum(new ScriptVariableInt(code[++pc].intValue));
          break;
        }
        int chainID = (pc + 3 < code.length && code[pc + 2].tok == T.opAND
            && code[pc + 3].tok == T.spec_chain ? code[pc + 3].intValue
            : 9);
        rpn.addXBs(getAtomBits(T.spec_seqcode_range, new int[] {
            getSeqCode(instruction), getSeqCode(code[++pc]), chainID }));
        if (chainID != 9)
          pc += 2;
        break;
      case T.cell:
        P3 pt = (P3) value;
        rpn.addXBs(getAtomBits(T.cell, new int[] { (int) Math.floor(pt.x * 1000),
            (int) Math.floor(pt.y * 1000), (int) Math.floor(pt.z * 1000) }));
        break;
      case T.thismodel:
        rpn.addXBs(viewer.getModelUndeletedAtomsBitSet(viewer
            .getCurrentModelIndex()));
        break;
      case T.hydrogen:
      case T.amino:
      case T.backbone:
      case T.solvent:
      case T.helix:
      case T.helixalpha:
      case T.helix310:
      case T.helixpi:
      case T.sidechain:
      case T.surface:
        rpn.addXBs(lookupIdentifierValue((String) value));
        break;
      case T.opLT:
      case T.opLE:
      case T.opGE:
      case T.opGT:
      case T.opEQ:
      case T.opNE:
        if (pc + 1 == code.length)
          error(ERROR_invalidArgument);
        val = code[++pc].value;
        int tokOperator = instruction.tok;
        int tokWhat = instruction.intValue;
        String property = (tokWhat == T.property ? (String) val : null);
        if (property != null) {
          if (pc + 1 == code.length)
            error(ERROR_invalidArgument);
          val = code[++pc].value;
        }
        if (tokWhat == T.configuration && tokOperator != T.opEQ)
          error(ERROR_invalidArgument);
        if (chk) {
          rpn.addXBs(new BS());
          break;
        }
        boolean isModel = (tokWhat == T.model);
        boolean isIntProperty = T.tokAttr(tokWhat, T.intproperty);
        boolean isFloatProperty = T.tokAttr(tokWhat, T.floatproperty);
        boolean isIntOrFloat = isIntProperty && isFloatProperty;
        boolean isStringProperty = !isIntProperty
            && T.tokAttr(tokWhat, T.strproperty);
        if (tokWhat == T.element)
          isIntProperty = !(isStringProperty = false);
        int tokValue = code[pc].tok;
        comparisonValue = code[pc].intValue;
        float comparisonFloat = Float.NaN;
        if (val instanceof P3) {
          if (tokWhat == T.color) {
            comparisonValue = ColorUtil.colorPtToInt((P3) val);
            tokValue = T.integer;
            isIntProperty = true;
          }
        } else if (val instanceof String) {
          if (tokWhat == T.color) {
            comparisonValue = ColorUtil.getArgbFromString((String) val);
            if (comparisonValue == 0
                && T.tokAttr(tokValue, T.identifier)) {
              val = getStringParameter((String) val, true);
              if (((String) val).startsWith("{")) {
                val = Escape.uP((String) val);
                if (val instanceof P3)
                  comparisonValue = ColorUtil.colorPtToInt((P3) val);
                else
                  comparisonValue = 0;
              } else {
                comparisonValue = ColorUtil.getArgbFromString((String) val);
              }
            }
            tokValue = T.integer;
            isIntProperty = true;
          } else if (isStringProperty) {
            if (T.tokAttr(tokValue, T.identifier))
              val = getStringParameter((String) val, true);
          } else {
            if (T.tokAttr(tokValue, T.identifier))
              val = getNumericParameter((String) val);
            if (val instanceof String) {
              if (tokWhat == T.structure || tokWhat == T.substructure
                  || tokWhat == T.element)
                isStringProperty = !(isIntProperty = (comparisonValue != Integer.MAX_VALUE));
              else
                val = SV.nValue(code[pc]);
            }
            if (val instanceof Integer)
              comparisonFloat = comparisonValue = ((Integer) val).intValue();
            else if (val instanceof Float && isModel)
              comparisonValue = ModelCollection
                  .modelFileNumberFromFloat(((Float) val).floatValue());
          }
        }
        if (isStringProperty && !(val instanceof String)) {
          val = "" + val;
        }
        if (val instanceof Integer || tokValue == T.integer) {
          if (isModel) {
            if (comparisonValue >= 1000000)
              tokWhat = -T.model;
          } else if (isIntOrFloat) {
            isFloatProperty = false;
          } else if (isFloatProperty) {
            comparisonFloat = comparisonValue;
          }
        } else if (val instanceof Float) {
          if (isModel) {
            tokWhat = -T.model;
          } else {
            comparisonFloat = ((Float) val).floatValue();
            if (isIntOrFloat) {
              isIntProperty = false;
            } else if (isIntProperty) {
              comparisonValue = (int)(comparisonFloat);
            }
          }
        } else if (!isStringProperty) {
          iToken++;
          error(ERROR_invalidArgument);
        }
        if (isModel && comparisonValue >= 1000000
            && comparisonValue % 1000000 == 0) {
          comparisonValue /= 1000000;
          tokWhat = T.file;
          isModel = false;
        }
        if (tokWhat == -T.model && tokOperator == T.opEQ) {
          rpn.addXBs(bitSetForModelFileNumber(comparisonValue));
          break;
        }
        if (value != null && ((String) value).indexOf("-") >= 0) {
          if (isIntProperty)
            comparisonValue = -comparisonValue;
          else if (!Float.isNaN(comparisonFloat))
            comparisonFloat = -comparisonFloat;
        }
        float[] data = (tokWhat == T.property ? viewer
            .getDataFloat(property) : null);
        rpn.addXBs(isIntProperty ? compareInt(tokWhat, tokOperator,
            comparisonValue) : isStringProperty ? compareString(tokWhat,
            tokOperator, (String) val) : compareFloat(tokWhat, data,
            tokOperator, comparisonFloat));
        break;
      case T.decimal:
      case T.integer:
        rpn.addXNum(SV.newScriptVariableToken(instruction));
        break;
      case T.bitset:
        BS bs1 = BSUtil.copy((BS) value);
        //System.out.println(Escape.escape(bs1));
        //if (isStateScript && viewer.getTestFlag(1))
        //BitSetUtil.deleteBits(bs1, (BitSet) viewer.getModelSetAuxiliaryInfo("bsDeletedAtoms"));
        //System.out.println(Escape.escape(bs1));
        rpn.addXBs(bs1);
        break;
      case T.point3f:
        rpn.addXPt((P3) value);
        break;
      default:
        if (T.tokAttr(instruction.tok, T.mathop)) {
          if (!rpn.addOp(instruction))
            error(ERROR_invalidArgument);
          break;
        }
        if (!(value instanceof String)) {
          // catch-all: point4f, hash, list, etc.
          rpn.addXObj(value);
          break;
        }
        val = getParameter((String) value, 0);
        if (isInMath) {
          rpn.addXObj(val);
          break;
        }
        if (val instanceof String)
          val = getStringObjectAsVariable((String) val, null);
        if (val instanceof JmolList<?>) {
          BS bs = SV.unEscapeBitSetArray(
              (JmolList<SV>) val, true);
          if (bs == null)
            val = value;
          else
            val = bs;
        }
        if (val instanceof String)
          val = lookupIdentifierValue((String) value);
        rpn.addXObj(val);
        break;
      }
    }
    expressionResult = rpn.getResult(allowUnderflow);
    if (expressionResult == null) {
      if (allowUnderflow)
        return null;
      if (!chk)
        rpn.dumpStacks("after getResult");
      error(ERROR_endOfStatementUnexpected);
    }
    expressionResult = ((SV) expressionResult).value;
    if (expressionResult instanceof String
        && (mustBeBitSet || ((String) expressionResult).startsWith("({"))) {
      // allow for select @{x} where x is a string that can evaluate to a bitset
      expressionResult = (chk ? new BS() : getAtomBitSet(expressionResult));
    }
    if (!mustBeBitSet && !(expressionResult instanceof BS))
      return null; // because result is in expressionResult in that case
    BS bs = (expressionResult instanceof BS ? (BS) expressionResult
        : new BS());
    isBondSet = (expressionResult instanceof BondSet);
    if (!isBondSet) {
      viewer.excludeAtoms(bs, ignoreSubset);
      if (bs.length() > viewer.getAtomCount())
        bs.clearAll();
    }
    if (tempStatement != null) {
      st = tempStatement;
      tempStatement = null;
    }
    return bs;
  }

  private BS compareFloat(int tokWhat, float[] data, int tokOperator,
                              float comparisonFloat) {
    BS bs = new BS();
    int atomCount = viewer.getAtomCount();
    ModelSet modelSet = viewer.modelSet;
    Atom[] atoms = modelSet.atoms;
    float propertyFloat = 0;
    viewer.autoCalculate(tokWhat);
    for (int i = atomCount; --i >= 0;) {
      boolean match = false;
      Atom atom = atoms[i];
      switch (tokWhat) {
      default:
        propertyFloat = Atom.atomPropertyFloat(viewer, atom, tokWhat);
        break;
      case T.property:
        if (data == null || data.length <= i)
          continue;
        propertyFloat = data[i];
      }
      match = compareFloat(tokOperator, propertyFloat, comparisonFloat);
      if (match)
        bs.set(i);
    }
    return bs;
  }

  private BS compareString(int tokWhat, int tokOperator,
                               String comparisonString) throws ScriptException {
    BS bs = new BS();
    Atom[] atoms = viewer.modelSet.atoms;
    int atomCount = viewer.getAtomCount();
    boolean isCaseSensitive = (tokWhat == T.chain && viewer
        .getChainCaseSensitive());
    if (!isCaseSensitive)
      comparisonString = comparisonString.toLowerCase();
    for (int i = atomCount; --i >= 0;) {
      String propertyString = Atom
          .atomPropertyString(viewer, atoms[i], tokWhat);
      if (!isCaseSensitive)
        propertyString = propertyString.toLowerCase();
      if (compareStringValues(tokOperator, propertyString, comparisonString))
        bs.set(i);
    }
    return bs;
  }

  protected BS compareInt(int tokWhat, int tokOperator, int comparisonValue) {
    int propertyValue = Integer.MAX_VALUE;
    BS propertyBitSet = null;
    int bitsetComparator = tokOperator;
    int bitsetBaseValue = comparisonValue;
    int atomCount = viewer.getAtomCount();
    ModelSet modelSet = viewer.modelSet;
    Atom[] atoms = modelSet.atoms;
    int imax = -1;
    int imin = 0;
    int iModel = -1;
    int[] cellRange = null;
    int nOps = 0;
    BS bs;
    // preliminary setup
    switch (tokWhat) {
    case T.symop:
      switch (bitsetComparator) {
      case T.opGE:
      case T.opGT:
        imax = Integer.MAX_VALUE;
        break;
      }
      break;
    case T.atomindex:
      try {
        switch (tokOperator) {
        case T.opLT:
          return BSUtil.newBitSet2(0, comparisonValue);
        case T.opLE:
          return BSUtil.newBitSet2(0, comparisonValue + 1);
        case T.opGE: 
          return BSUtil.newBitSet2(comparisonValue, atomCount);
        case T.opGT:
          return BSUtil.newBitSet2(comparisonValue + 1, atomCount);
        case T.opEQ:
          return (comparisonValue < atomCount ? BSUtil.newBitSet2(
              comparisonValue, comparisonValue + 1) : new BS());
        case T.opNE:
        default:
          bs = BSUtil.setAll(atomCount);
          if (comparisonValue >= 0)
            bs.clear(comparisonValue);
          return bs;
        }
      } catch (Exception e) {
        return new BS();
      }
    }
    bs = BSUtil.newBitSet(atomCount);
    for (int i = 0; i < atomCount; ++i) {
      boolean match = false;
      Atom atom = atoms[i];
      switch (tokWhat) {
      default:
        propertyValue = Atom.atomPropertyInt(atom, tokWhat);
        break;
      case T.configuration:
        // these are all-inclusive; no need to do a by-atom comparison
        return BSUtil.copy(viewer.getConformation(-1, comparisonValue - 1,
            false));
      case T.symop:
        propertyBitSet = atom.getAtomSymmetry();
        if (propertyBitSet == null)
          continue;
        if (atom.getModelIndex() != iModel) {
          iModel = atom.getModelIndex();
          cellRange = modelSet.getModelCellRange(iModel);
          nOps = modelSet.getModelSymmetryCount(iModel);
        }
        if (bitsetBaseValue >= 200) {
          if (cellRange == null)
            continue;
          /*
           * symop>=1000 indicates symop*1000 + lattice_translation(555) for
           * this the comparision is only with the translational component; the
           * symop itself must match thus: select symop!=1655 selects all
           * symop=1 and translation !=655 select symop>=2555 selects all
           * symop=2 and translation >555 symop >=200 indicates any symop in the
           * specified translation (a few space groups have > 100 operations)
           * 
           * Note that when normalization is not done, symop=1555 may not be in
           * the base unit cell. Everything is relative to wherever the base
           * atoms ended up, usually in 555, but not necessarily.
           * 
           * The reason this is tied together an atom may have one translation
           * for one symop and another for a different one.
           * 
           * Bob Hanson - 10/2006
           */
          comparisonValue = bitsetBaseValue % 1000;
          int symop = bitsetBaseValue / 1000 - 1;
          if (symop < 0) {
            match = true;
          } else if (nOps == 0 || symop >= 0
              && !(match = propertyBitSet.get(symop))) {
            continue;
          }
          bitsetComparator = T.none;
          if (symop < 0)
            propertyValue = atom.getCellTranslation(comparisonValue, cellRange,
                nOps);
          else
            propertyValue = atom.getSymmetryTranslation(symop, cellRange, nOps);
        } else if (nOps > 0) {
          if (comparisonValue > nOps) {
            if (bitsetComparator != T.opLT
                && bitsetComparator != T.opLE)
              continue;
          }
          if (bitsetComparator == T.opNE) {
            if (comparisonValue > 0 && comparisonValue <= nOps
                && !propertyBitSet.get(comparisonValue)) {
              bs.set(i);
            }
            continue;
          }
        }
        switch (bitsetComparator) {
        case T.opLT:
          imax = comparisonValue - 1;
          break;
        case T.opLE:
          imax = comparisonValue;
          break;
        case T.opGE:
          imin = comparisonValue - 1;
          break;
        case T.opGT:
          imin = comparisonValue;
          break;
        case T.opEQ:
          imax = comparisonValue;
          imin = comparisonValue - 1;
          break;
        case T.opNE:
          match = !propertyBitSet.get(comparisonValue);
          break;
        }
        if (imin < 0)
          imin = 0;
        if (imin < imax) {
          int pt = propertyBitSet.nextSetBit(imin);
          if (pt >= 0 && pt < imax)
            match = true;
        }
        // note that a symop property can be both LE and GT !
        if (!match || propertyValue == Integer.MAX_VALUE)
          tokOperator = T.none;
      }
      switch (tokOperator) {
      case T.none:
        break;
      case T.opLT:
        match = (propertyValue < comparisonValue);
        break;
      case T.opLE:
        match = (propertyValue <= comparisonValue);
        break;
      case T.opGE:
        match = (propertyValue >= comparisonValue);
        break;
      case T.opGT:
        match = (propertyValue > comparisonValue);
        break;
      case T.opEQ:
        match = (propertyValue == comparisonValue);
        break;
      case T.opNE:
        match = (propertyValue != comparisonValue);
        break;
      }
      if (match)
        bs.set(i);
    }
    return bs;
  }

  private boolean compareStringValues(int tokOperator, String propertyValue,
                                String comparisonValue) throws ScriptException {
    switch (tokOperator) {
    case T.opEQ:
    case T.opNE:
      return (TextFormat.isMatch(propertyValue, comparisonValue, true, true) == (tokOperator == T.opEQ));
    default:
      error(ERROR_invalidArgument);
    }
    return false;
  }

  private static boolean compareFloat(int tokOperator, float propertyFloat,
                                      float comparisonFloat) {
    switch (tokOperator) {
    case T.opLT:
      return propertyFloat < comparisonFloat;
    case T.opLE:
      return propertyFloat <= comparisonFloat;
    case T.opGE:
      return propertyFloat >= comparisonFloat;
    case T.opGT:
      return propertyFloat > comparisonFloat;
    case T.opEQ:
      return propertyFloat == comparisonFloat;
    case T.opNE:
      return propertyFloat != comparisonFloat;
    }
    return false;
  }

  private BS getAtomBits(int tokType, Object specInfo) {
    return (chk ? new BS() : viewer
        .getAtomBits(tokType, specInfo));
  }

  private static int getSeqCode(T instruction) {
    return (instruction.intValue != Integer.MAX_VALUE ? Group.getSeqcodeFor(
        instruction.intValue, ' ') : ((Integer) instruction.value).intValue());
  }

  /*
   * ****************************************************************************
   * ============================================================== checks and
   * parameter retrieval
   * ==============================================================
   */

  private int checkLast(int i) throws ScriptException {
    return checkLength(i + 1) - 1;
  }

  private int checkLength(int length) throws ScriptException {
    if (length >= 0)
      return checkLengthErrorPt(length, 0);
    // max
    if (slen > -length) {
      iToken = -length;
      error(ERROR_badArgumentCount);
    }
    return slen;
  }

  private int checkLengthErrorPt(int length, int errorPt) throws ScriptException {
    if (slen != length) {
      iToken = errorPt > 0 ? errorPt : slen;
      error(errorPt > 0 ? ERROR_invalidArgument : ERROR_badArgumentCount);
    }
    return slen;
  }

  private int checkLength23() throws ScriptException {
    iToken = slen;
    if (slen != 2 && slen != 3)
      error(ERROR_badArgumentCount);
    return slen;
  }

  private int checkLength34() throws ScriptException {
    iToken = slen;
    if (slen != 3 && slen != 4)
      error(ERROR_badArgumentCount);
    return slen;
  }

  private int theTok;
  private T theToken;

  private T getToken(int i) throws ScriptException {
    if (!checkToken(i))
      error(ERROR_endOfStatementUnexpected);
    theToken = st[i];
    theTok = theToken.tok;
    return theToken;
  }

  private int tokAt(int i) {
    return (i < slen && st[i] != null ? st[i].tok
        : T.nada);
  }

  private static int tokAtArray(int i, T[] args) {
    return (i < args.length && args[i] != null ? args[i].tok : T.nada);
  }

  private T tokenAt(int i, T[] args) {
    return (i < args.length ? args[i] : null);
  }

  private boolean checkToken(int i) {
    return (iToken = i) < slen;
  }

  private int modelNumberParameter(int index) throws ScriptException {
    int iFrame = 0;
    boolean useModelNumber = false;
    switch (tokAt(index)) {
    case T.integer:
      useModelNumber = true;
      //$FALL-THROUGH$
    case T.decimal:
      iFrame = getToken(index).intValue; // decimal Token intValue is
      // model/frame number encoded
      break;
    case T.string:
      iFrame = getFloatEncodedInt(stringParameter(index));
      break;
    default:
      error(ERROR_invalidArgument);
    }
    return viewer.getModelNumberIndex(iFrame, useModelNumber, true);
  }

  private String optParameterAsString(int i) throws ScriptException {
    if (i >= slen)
      return "";
    return parameterAsString(i);
  }

  private String parameterAsString(int i) throws ScriptException {
    getToken(i);
    if (theToken == null)
      error(ERROR_endOfStatementUnexpected);
    return SV.sValue(theToken);
  }

  private int intParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == T.integer)
        return theToken.intValue;
    error(ERROR_integerExpected);
    return 0;
  }

  private int intParameterRange(int i, int min, int max) throws ScriptException {
    int val = intParameter(i);
    if (val < min || val > max)
      integerOutOfRange(min, max);
    return val;
  }

  private boolean isFloatParameter(int index) {
    switch (tokAt(index)) {
    case T.integer:
    case T.decimal:
      return true;
    }
    return false;
  }

  private float floatParameterRange(int i, float min, float max)
      throws ScriptException {
    float val = floatParameter(i);
    if (val < min || val > max)
      numberOutOfRange(min, max);
    return val;
  }

  private float floatParameter(int index) throws ScriptException {
    if (checkToken(index)) {
      getToken(index);
      switch (theTok) {
      case T.spec_seqcode_range:
        return -theToken.intValue;
      case T.spec_seqcode:
      case T.integer:
        return theToken.intValue;
      case T.spec_model2:
      case T.decimal:
        return ((Float) theToken.value).floatValue();
      }
    }
    error(ERROR_numberExpected);
    return 0;
  }

  /**
   * process a general string or set of parameters as an array of floats,
   * allowing for relatively free form input
   * 
   * @param i
   * @param nMin
   * @param nMax
   * @return array of floats
   * @throws ScriptException
   */
  private float[] floatParameterSet(int i, int nMin, int nMax)
      throws ScriptException {
    int tok = tokAt(i);
    if (tok == T.spacebeforesquare)
      tok = tokAt(++i);
    boolean haveBrace = (tok == T.leftbrace);
    boolean haveSquare = (tok == T.leftsquare);
    float[] fparams = null;
    JmolList<Float> v = new  JmolList<Float>();
    int n = 0;
    if (haveBrace || haveSquare)
      i++;
    P3 pt;
    String s = null;
    switch (tokAt(i)) {
    case T.string:
      s = SV.sValue(st[i]);
      s = TextFormat.replaceAllCharacter(s, "{},[]\"'", ' ');
      fparams = Parser.parseFloatArray(s);
      n = fparams.length;
      break;
    case T.varray:
      fparams = SV.flistValue(st[i++], 0);
      n = fparams.length;
      break;
    default:
      while (n < nMax) {
        tok = tokAt(i);
        if (haveBrace && tok == T.rightbrace || haveSquare
            && tok == T.rightsquare)
          break;
        switch (tok) {
        case T.comma:
        case T.leftbrace:
        case T.rightbrace:
          break;
        case T.string:
          break;
        case T.point3f:
          pt = getPoint3f(i, false);
          v.addLast(Float.valueOf(pt.x));
          v.addLast(Float.valueOf(pt.y));
          v.addLast(Float.valueOf(pt.z));
          n += 3;
          break;
        case T.point4f:
          Point4f pt4 = getPoint4f(i);
          v.addLast(Float.valueOf(pt4.x));
          v.addLast(Float.valueOf(pt4.y));
          v.addLast(Float.valueOf(pt4.z));
          v.addLast(Float.valueOf(pt4.w));
          n += 4;
          break;
        default:
          v.addLast(Float.valueOf(floatParameter(i)));
          n++;
          if (n == nMax && haveSquare && tokAt(i + 1) == T.rightbrace)
            i++;
        }
        i++;
      }
    }
    if (haveBrace && tokAt(i++) != T.rightbrace || haveSquare
        && tokAt(i++) != T.rightsquare)
      error(ERROR_invalidArgument);
    iToken = i - 1;
    if (n < nMin || n > nMax)
      error(ERROR_invalidArgument);
    if (fparams == null) {
      fparams = new float[n];
      for (int j = 0; j < n; j++)
        fparams[j] = v.get(j).floatValue();
    }
    return fparams;
  }

  private boolean isArrayParameter(int i) {
    switch (tokAt(i)) {
    case T.varray:
    case T.matrix3f:
    case T.matrix4f:
    case T.spacebeforesquare:
    case T.leftsquare:
      return true;
    }
    return false;
  }

  private P3[] getPointArray(int i, int nPoints) throws ScriptException {
    P3[] points = (nPoints < 0 ? null : new P3[nPoints]);
    JmolList<P3> vp = (nPoints < 0 ? new  JmolList<P3>() : null);
    int tok = (i < 0 ? T.varray : getToken(i++).tok);
    switch (tok) {
    case T.varray:
      JmolList<SV> v = ((SV) theToken).getList();
      if (nPoints >= 0 && v.size() != nPoints)
        error(ERROR_invalidArgument);
      nPoints = v.size();
      if (points == null)
        points = new P3[nPoints];
      for (int j = 0; j < nPoints; j++)
        if ((points[j] = SV.ptValue(v.get(j))) == null)
          error(ERROR_invalidArgument);
      return points;
    case T.spacebeforesquare:
      tok = tokAt(i++);
      break;
    }
    if (tok != T.leftsquare)
      error(ERROR_invalidArgument);
    int n = 0;
    while (tok != T.rightsquare && tok != T.nada) {
      tok = getToken(i).tok;
      switch (tok) {
      case T.nada:
      case T.rightsquare:
        break;
      case T.comma:
        i++;
        break;
      default:
        if (nPoints >= 0 && n == nPoints) {
          tok = T.nada;
          break;
        }
        P3 pt = getPoint3f(i, true);
        if (points == null)
          vp.addLast(pt);
        else
          points[n] = pt;
        n++;
        i = iToken + 1;
      }
    }
    if (tok != T.rightsquare)
      error(ERROR_invalidArgument);
    if (points == null)
      points = vp.toArray(new P3[vp.size()]);
    return points;
  }

  private float[][] floatArraySet(int i, int nX, int nY) throws ScriptException {
    int tok = tokAt(i++);
    if (tok == T.spacebeforesquare)
      tok = tokAt(i++);
    if (tok != T.leftsquare)
      error(ERROR_invalidArgument);
    float[][] fparams = ArrayUtil.newFloat2(nX);
    int n = 0;
    while (tok != T.rightsquare) {
      tok = getToken(i).tok;
      switch (tok) {
      case T.spacebeforesquare:
      case T.rightsquare:
        continue;
      case T.comma:
        i++;
        break;
      case T.leftsquare:
        i++;
        float[] f = new float[nY];
        fparams[n++] = f;
        for (int j = 0; j < nY; j++) {
          f[j] = floatParameter(i++);
          if (tokAt(i) == T.comma)
            i++;
        }
        if (tokAt(i++) != T.rightsquare)
          error(ERROR_invalidArgument);
        tok = T.nada;
        if (n == nX && tokAt(i) != T.rightsquare)
          error(ERROR_invalidArgument);
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    return fparams;
  }

  private float[][][] floatArraySetXYZ(int i, int nX, int nY, int nZ)
      throws ScriptException {
    int tok = tokAt(i++);
    if (tok == T.spacebeforesquare)
      tok = tokAt(i++);
    if (tok != T.leftsquare || nX <= 0)
      error(ERROR_invalidArgument);
    float[][][] fparams = ArrayUtil.newFloat3(nX, -1);
    int n = 0;
    while (tok != T.rightsquare) {
      tok = getToken(i).tok;
      switch (tok) {
      case T.spacebeforesquare:
      case T.rightsquare:
        continue;
      case T.comma:
        i++;
        break;
      case T.leftsquare:
        fparams[n++] = floatArraySet(i, nY, nZ);
        i = ++iToken;
        tok = T.nada;
        if (n == nX && tokAt(i) != T.rightsquare)
          error(ERROR_invalidArgument);
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    return fparams;
  }

  private String stringParameter(int index) throws ScriptException {
    if (!checkToken(index) || getToken(index).tok != T.string)
      error(ERROR_stringExpected);
    return (String) theToken.value;
  }

  private String[] stringParameterSet(int i) throws ScriptException {
    switch (tokAt(i)) {
    case T.string:
      String s = stringParameter(i);
      if (s.startsWith("[\"")) {
        Object o = viewer.evaluateExpression(s);
        if (o instanceof String)
          return TextFormat.split((String) o, '\n');
      }
      return new String[] { s };
    case T.spacebeforesquare:
      i += 2;
      break;
    case T.leftsquare:
      ++i;
      break;
    case T.varray:
      return SV.listValue(getToken(i));
    default:
      error(ERROR_invalidArgument);
    }
    int tok;
    JmolList<String> v = new  JmolList<String>();
    while ((tok = tokAt(i)) != T.rightsquare) {
      switch (tok) {
      case T.comma:
        break;
      case T.string:
        v.addLast(stringParameter(i));
        break;
      default:
      case T.nada:
        error(ERROR_invalidArgument);
      }
      i++;
    }
    iToken = i;
    int n = v.size();
    String[] sParams = new String[n];
    for (int j = 0; j < n; j++) {
      sParams[j] = v.get(j);
    }
    return sParams;
  }

  private String objectNameParameter(int index) throws ScriptException {
    if (!checkToken(index))
      error(ERROR_objectNameExpected);
    return parameterAsString(index);
  }

  private boolean booleanParameter(int i) throws ScriptException {
    if (slen == i)
      return true;
    switch (getToken(checkLast(i)).tok) {
    case T.on:
      return true;
    case T.off:
      return false;
    default:
      error(ERROR_booleanExpected);
    }
    return false;
  }

  private P3 atomCenterOrCoordinateParameter(int i) throws ScriptException {
    switch (getToken(i).tok) {
    case T.bitset:
    case T.expressionBegin:
      BS bs = atomExpression(st, i, 0, true, false, false, true);
      if (bs != null)
        return viewer.getAtomSetCenter(bs);
      if (expressionResult instanceof P3)
        return (P3) expressionResult;
      error(ERROR_invalidArgument);
      break;
    case T.leftbrace:
    case T.point3f:
      return getPoint3f(i, true);
    }
    error(ERROR_invalidArgument);
    // impossible return
    return null;
  }

  private boolean isCenterParameter(int i) {
    int tok = tokAt(i);
    return (tok == T.dollarsign || tok == T.leftbrace
        || tok == T.expressionBegin || tok == T.point3f || tok == T.bitset);
  }

  private P3 centerParameter(int i) throws ScriptException {
    return centerParameterForModel(i, Integer.MIN_VALUE);
  }

  private P3 centerParameterForModel(int i, int modelIndex) throws ScriptException {
    P3 center = null;
    expressionResult = null;
    if (checkToken(i)) {
      switch (getToken(i).tok) {
      case T.dollarsign:
        String id = objectNameParameter(++i);
        int index = Integer.MIN_VALUE;
        // allow for $pt2.3 -- specific vertex
        if (tokAt(i + 1) == T.leftsquare) {
          index = parameterExpressionList(-i - 1, -1, true).get(0).asInt();
          if (getToken(--iToken).tok != T.rightsquare)
            error(ERROR_invalidArgument);
        }
        if (chk)
          return new P3();
        if (tokAt(i + 1) == T.per
            && (tokAt(i + 2) == T.length || tokAt(i + 2) == T.size)) {
          index = Integer.MAX_VALUE;
          iToken = i + 2;
        }
        if ((center = getObjectCenter(id, index, modelIndex)) == null)
          errorStr(ERROR_drawObjectNotDefined, id);
        break;
      case T.bitset:
      case T.expressionBegin:
      case T.leftbrace:
      case T.point3f:
        center = atomCenterOrCoordinateParameter(i);
        break;
      }
    }
    if (center == null)
      error(ERROR_coordinateOrNameOrExpressionRequired);
    return center;
  }

  private Point4f planeParameter(int i) throws ScriptException {
    V3 vAB = new V3();
    V3 vAC = new V3();
    Point4f plane = null;
    boolean isNegated = (tokAt(i) == T.minus);
    if (isNegated)
      i++;
    if (i < slen)
      switch (getToken(i).tok) {
      case T.point4f:
        plane = Point4f.newPt((Point4f) theToken.value);
        break;
      case T.dollarsign:
        String id = objectNameParameter(++i);
        if (chk)
          return new Point4f();
        int shapeType = sm.getShapeIdFromObjectName(id);
        switch (shapeType) {
        case JC.SHAPE_DRAW:
          setShapeProperty(JC.SHAPE_DRAW, "thisID", id);
          P3[] points = (P3[]) getShapeProperty(
              JC.SHAPE_DRAW, "vertices");
          if (points == null || points.length < 3 || points[0] == null
              || points[1] == null || points[2] == null)
            break;
          Measure.getPlaneThroughPoints(points[0], points[1], points[2],
              new V3(), vAB, vAC, plane = new Point4f());
          break;
        case JC.SHAPE_ISOSURFACE:
          setShapeProperty(JC.SHAPE_ISOSURFACE, "thisID", id);
          plane = (Point4f) getShapeProperty(JC.SHAPE_ISOSURFACE,
              "plane");
          break;
        }
        break;
      case T.x:
        if (!checkToken(++i) || getToken(i++).tok != T.opEQ)
          evalError("x=?", null);
        plane = Point4f.new4(1, 0, 0, -floatParameter(i));
        break;
      case T.y:
        if (!checkToken(++i) || getToken(i++).tok != T.opEQ)
          evalError("y=?", null);
        plane = Point4f.new4(0, 1, 0, -floatParameter(i));
        break;
      case T.z:
        if (!checkToken(++i) || getToken(i++).tok != T.opEQ)
          evalError("z=?", null);
        plane = Point4f.new4(0, 0, 1, -floatParameter(i));
        break;
      case T.identifier:
      case T.string:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("xy"))
          return Point4f.new4(0, 0, 1, 0);
        if (str.equalsIgnoreCase("xz"))
          return Point4f.new4(0, 1, 0, 0);
        if (str.equalsIgnoreCase("yz"))
          return Point4f.new4(1, 0, 0, 0);
        iToken += 2;
        break;
      case T.leftbrace:
        if (!isPoint3f(i)) {
          plane = getPoint4f(i);
          break;
        }
        //$FALL-THROUGH$
      case T.bitset:
      case T.expressionBegin:
        P3 pt1 = atomCenterOrCoordinateParameter(i);
        if (getToken(++iToken).tok == T.comma)
          ++iToken;
        P3 pt2 = atomCenterOrCoordinateParameter(iToken);
        if (getToken(++iToken).tok == T.comma)
          ++iToken;
        P3 pt3 = atomCenterOrCoordinateParameter(iToken);
        i = iToken;
        V3 norm = new V3();
        float w = Measure.getNormalThroughPoints(pt1, pt2, pt3, norm, vAB, vAC);
        plane = new Point4f();
        plane.set(norm.x, norm.y, norm.z, w);
        if (!chk && Logger.debugging)
          Logger.debug("points: " + pt1 + pt2 + pt3 + " defined plane: "
              + plane);
        break;
      }
    if (plane == null)
      planeExpected();
    if (isNegated) {
      plane.scale(-1);
    }
    return plane;
  }

  private Point4f hklParameter(int i) throws ScriptException {
    if (!chk && viewer.getCurrentUnitCell() == null)
      error(ERROR_noUnitCell);
    P3 pt = (P3) getPointOrPlane(i, false, true, false, true, 3, 3);
    Point4f p = getHklPlane(pt);
    if (p == null)
      error(ERROR_badMillerIndices);
    if (!chk && Logger.debugging)
      Logger.info("defined plane: " + p);
    return p;
  }

  protected Point4f getHklPlane(P3 pt) {
    V3 vAB = new V3();
    V3 vAC = new V3();
    P3 pt1 = P3.new3(pt.x == 0 ? 1 : 1 / pt.x, 0, 0);
    P3 pt2 = P3.new3(0, pt.y == 0 ? 1 : 1 / pt.y, 0);
    P3 pt3 = P3.new3(0, 0, pt.z == 0 ? 1 : 1 / pt.z);
    // trick for 001 010 100 is to define the other points on other edges
    if (pt.x == 0 && pt.y == 0 && pt.z == 0) {
      return null;
    } else if (pt.x == 0 && pt.y == 0) {
      pt1.set(1, 0, pt3.z);
      pt2.set(0, 1, pt3.z);
    } else if (pt.y == 0 && pt.z == 0) {
      pt2.set(pt1.x, 0, 1);
      pt3.set(pt1.x, 1, 0);
    } else if (pt.z == 0 && pt.x == 0) {
      pt3.set(0, pt2.y, 1);
      pt1.set(1, pt2.y, 0);
    } else if (pt.x == 0) {
      pt1.set(1, pt2.y, 0);
    } else if (pt.y == 0) {
      pt2.set(0, 1, pt3.z);
    } else if (pt.z == 0) {
      pt3.set(pt1.x, 0, 1);
    }
    // base this one on the currently defined unit cell
    viewer.toCartesian(pt1, false);
    viewer.toCartesian(pt2, false);
    viewer.toCartesian(pt3, false);
    V3 plane = new V3();
    float w = Measure.getNormalThroughPoints(pt1, pt2, pt3, plane, vAB, vAC);
    Point4f pt4 = new Point4f();
    pt4.set(plane.x, plane.y, plane.z, w);
    return pt4;
  }

  private int getMadParameter() throws ScriptException {
    // wireframe, ssbond, hbond, struts
    int mad = 1;
    switch (getToken(1).tok) {
    case T.only:
      restrictSelected(false, false);
      break;
    case T.on:
      break;
    case T.off:
      mad = 0;
      break;
    case T.integer:
      int radiusRasMol = intParameterRange(1, 0, 750);
      mad = radiusRasMol * 4 * 2;
      break;
    case T.decimal:
      mad = (int) Math.floor(floatParameterRange(1, -3, 3) * 1000 * 2);
      if (mad < 0) {
        restrictSelected(false, false);
        mad = -mad;
      }
      break;
    default:
      error(ERROR_booleanOrNumberExpected);
    }
    return mad;
  }

  private int getSetAxesTypeMad(int index) throws ScriptException {
    if (index == slen)
      return 1;
    switch (getToken(checkLast(index)).tok) {
    case T.on:
      return 1;
    case T.off:
      return 0;
    case T.dotted:
      return -1;
    case T.integer:
      return intParameterRange(index, -1, 19);
    case T.decimal:
      float angstroms = floatParameterRange(index, 0, 2);
      return (int)Math.floor(angstroms * 1000 * 2);
    }
    errorStr(ERROR_booleanOrWhateverExpected, "\"DOTTED\"");
    return 0;
  }

  private boolean isColorParam(int i) {
    int tok = tokAt(i);
    return (tok == T.navy || tok == T.spacebeforesquare
        || tok == T.leftsquare || tok == T.varray
        || tok == T.point3f || isPoint3f(i) || (tok == T.string || T
        .tokAttr(tok, T.identifier))
        && ColorUtil.getArgbFromString((String) st[i].value) != 0);
  }

  private int getArgbParam(int index) throws ScriptException {
    return getArgbParamOrNone(index, false);
  }

  private int getArgbParamLast(int index, boolean allowNone)
      throws ScriptException {
    int icolor = getArgbParamOrNone(index, allowNone);
    checkLast(iToken);
    return icolor;
  }

  private int getArgbParamOrNone(int index, boolean allowNone) throws ScriptException {
    P3 pt = null;
    if (checkToken(index)) {
      switch (getToken(index).tok) {
      default:
        if (!T.tokAttr(theTok, T.identifier))
          break;
        //$FALL-THROUGH$
      case T.navy:
      case T.string:
        return ColorUtil.getArgbFromString(parameterAsString(index));
      case T.spacebeforesquare:
        return getColorTriad(index + 2);
      case T.leftsquare:
        return getColorTriad(++index);
      case T.varray:
        float[] rgb = SV.flistValue(theToken, 3);
        if (rgb != null && rgb.length != 3)
          pt = P3.new3(rgb[0], rgb[1], rgb[2]);
        break;
      case T.point3f:
        pt = (P3) theToken.value;
        break;
      case T.leftbrace:
        pt = getPoint3f(index, false);
        break;
      case T.none:
        if (allowNone)
          return 0;
      }
    }
    if (pt == null)
      error(ERROR_colorExpected);
    return ColorUtil.colorPtToInt(pt);
  }

  private int getColorTriad(int i) throws ScriptException {
    float[] colors = new float[3];
    int n = 0;
    String hex = "";
    getToken(i);
    P3 pt = null;
    float val = 0;
    out: switch (theTok) {
    case T.integer:
    case T.spec_seqcode:
    case T.decimal:
      for (; i < slen; i++) {
        switch (getToken(i).tok) {
        case T.comma:
          continue;
        case T.identifier:
          if (n != 1 || colors[0] != 0)
            error(ERROR_badRGBColor);
          hex = "0" + parameterAsString(i);
          break out;
        case T.decimal:
          if (n > 2)
            error(ERROR_badRGBColor);
          val = floatParameter(i);
          break;
        case T.integer:
          if (n > 2)
            error(ERROR_badRGBColor);
          val = theToken.intValue;
          break;
        case T.spec_seqcode:
          if (n > 2)
            error(ERROR_badRGBColor);
          val = ((Integer) theToken.value).intValue() % 256;
          break;
        case T.rightsquare:
          if (n != 3)
            error(ERROR_badRGBColor);
          --i;
          pt = P3.new3(colors[0], colors[1], colors[2]);
          break out;
        default:
          error(ERROR_badRGBColor);
        }
        colors[n++] = val;
      }
      error(ERROR_badRGBColor);
      break;
    case T.point3f:
      pt = (P3) theToken.value;
      break;
    case T.identifier:
      hex = parameterAsString(i);
      break;
    default:
      error(ERROR_badRGBColor);
    }
    if (getToken(++i).tok != T.rightsquare)
      error(ERROR_badRGBColor);
    if (pt != null)
      return ColorUtil.colorPtToInt(pt);
    if ((n = ColorUtil.getArgbFromString("[" + hex + "]")) == 0)
      error(ERROR_badRGBColor);
    return n;
  }

  private boolean coordinatesAreFractional;

  private boolean isPoint3f(int i) {
    // first check for simple possibilities:
    boolean isOK;
    if ((isOK = (tokAt(i) == T.point3f)) || tokAt(i) == T.point4f
        || isFloatParameter(i + 1) && isFloatParameter(i + 2)
        && isFloatParameter(i + 3) && isFloatParameter(i + 4))
      return isOK;
    ignoreError = true;
    int t = iToken;
    isOK = true;
    try {
      getPoint3f(i, true);
    } catch (Exception e) {
      isOK = false;
    }
    ignoreError = false;
    iToken = t;
    return isOK;
  }

  private P3 getPoint3f(int i, boolean allowFractional)
      throws ScriptException {
    return (P3) getPointOrPlane(i, false, allowFractional, true, false, 3,
        3);
  }

  private Point4f getPoint4f(int i) throws ScriptException {
    return (Point4f) getPointOrPlane(i, false, false, false, false, 4, 4);
  }

  private P3 fractionalPoint;

  private Object getPointOrPlane(int index, boolean integerOnly,
                                 boolean allowFractional, boolean doConvert,
                                 boolean implicitFractional, int minDim,
                                 int maxDim) throws ScriptException {
    // { x y z } or {a/b c/d e/f} are encoded now as seqcodes and model numbers
    // so we decode them here. It's a bit of a pain, but it isn't too bad.
    float[] coord = new float[6];
    int n = 0;
    coordinatesAreFractional = implicitFractional;
    if (tokAt(index) == T.point3f) {
      if (minDim <= 3 && maxDim >= 3)
        return /*Point3f*/getToken(index).value;
      error(ERROR_invalidArgument);
    }
    if (tokAt(index) == T.point4f) {
      if (minDim <= 4 && maxDim >= 4)
        return /*Point4f*/getToken(index).value;
      error(ERROR_invalidArgument);
    }
    int multiplier = 1;
    out: for (int i = index; i < st.length; i++) {
      switch (getToken(i).tok) {
      case T.leftbrace:
      case T.comma:
      case T.opAnd:
      case T.opAND:
        break;
      case T.rightbrace:
        break out;
      case T.minus:
        multiplier = -1;
        break;
      case T.spec_seqcode_range:
        if (n == 6)
          error(ERROR_invalidArgument);
        coord[n++] = theToken.intValue;
        multiplier = -1;
        break;
      case T.integer:
      case T.spec_seqcode:
        if (n == 6)
          error(ERROR_invalidArgument);
        coord[n++] = theToken.intValue * multiplier;
        multiplier = 1;
        break;
      case T.divide:
      case T.spec_model: // after a slash
        if (!allowFractional)
          error(ERROR_invalidArgument);
        if (theTok == T.divide)
          getToken(++i);
        n--;
        if (n < 0 || integerOnly)
          error(ERROR_invalidArgument);
        if (theToken.value instanceof Integer || theTok == T.integer) {
          coord[n++] /= (theToken.intValue == Integer.MAX_VALUE ? ((Integer) theToken.value)
              .intValue()
              : theToken.intValue);
        } else if (theToken.value instanceof Float) {
          coord[n++] /= ((Float) theToken.value).floatValue();
        }
        coordinatesAreFractional = true;
        break;
      case T.decimal:
      case T.spec_model2:
        if (integerOnly)
          error(ERROR_invalidArgument);
        if (n == 6)
          error(ERROR_invalidArgument);
        coord[n++] = ((Float) theToken.value).floatValue();
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (n < minDim || n > maxDim)
      error(ERROR_invalidArgument);
    if (n == 3) {
      P3 pt = P3.new3(coord[0], coord[1], coord[2]);
      if (coordinatesAreFractional && doConvert) {
        fractionalPoint = P3.newP(pt);
        if (!chk)
          viewer.toCartesian(pt, !viewer.getFractionalRelative());
      }
      return pt;
    }
    if (n == 4) {
      if (coordinatesAreFractional) // no fractional coordinates for planes (how
        // to convert?)
        error(ERROR_invalidArgument);
      Point4f plane = Point4f.new4(coord[0], coord[1], coord[2], coord[3]);
      return plane;
    }
    return coord;
  }

  private P3 xypParameter(int index) throws ScriptException {
    // [x y] or [x,y] refers to an xy point on the screen
    //     return a Point3f with z = Float.MAX_VALUE
    // [x y %] or [x,y %] refers to an xy point on the screen
    // as a percent
    //     return a Point3f with z = -Float.MAX_VALUE

    int tok = tokAt(index);
    if (tok == T.spacebeforesquare)
      tok = tokAt(++index);
    if (tok != T.leftsquare || !isFloatParameter(++index))
      return null;
    P3 pt = new P3();
    pt.x = floatParameter(index);
    if (tokAt(++index) == T.comma)
      index++;
    if (!isFloatParameter(index))
      return null;
    pt.y = floatParameter(index);
    boolean isPercent = (tokAt(++index) == T.percent);
    if (isPercent)
      ++index;
    if (tokAt(index) != T.rightsquare)
      return null;
    iToken = index;
    pt.z = (isPercent ? -1 : 1) * Float.MAX_VALUE;
    return pt;
  }

  /*
   * ****************************************************************
   * =============== command dispatch ===============================
   */

  /**
   * provides support for the script editor
   * 
   * @param i
   * @return true if displayable
   */
  private boolean isCommandDisplayable(int i) {
    if (i >= aatoken.length || i >= pcEnd || aatoken[i] == null)
      return false;
    return (lineIndices[i][1] > lineIndices[i][0]);
  }

  /**
   * checks to see if there is a pause condition, during which commands can
   * still be issued, but with the ! first.
   * 
   * @return false if there was a problem
   * @throws ScriptException
   */
  private boolean checkContinue() throws ScriptException {
    if (executionStopped)
      return false;
    if (executionStepping && isCommandDisplayable(pc)) {
      viewer.setScriptStatus("Next: " + getNextStatement(),
          "stepping -- type RESUME to continue", 0, null);
      executionPaused = true;
    } else if (!executionPaused) {
      return true;
    }
    if (Logger.debugging) {
      Logger.info("script execution paused at command " + (pc + 1) + " level "
          + scriptLevel + ": " + thisCommand);
    }
    refresh();
    while (executionPaused) {
      viewer.popHoldRepaintWhy("pause"); 
      // does not actually do a repaint
      // but clears the way for interaction
      String script = viewer.getInsertedCommand();
      if (script != "") {
        resumePausedExecution();
        setErrorMessage(null);
        ScriptContext scSave = getScriptContext();
        pc--; // in case there is an error, we point to the PAUSE command
        try {
          runScript(script);
        } catch (Exception e) {
          setErrorMessage("" + e);
        } catch (Error er) {
          setErrorMessage("" + er);
        }
        if (error) {
          scriptStatusOrBuffer(errorMessage);
          setErrorMessage(null);
        }
        restoreScriptContext(scSave, true, false, false);
        pauseExecution(false);
      }
      doDelay(ScriptDelayThread.PAUSE_DELAY);
      // JavaScript will not reach this point, 
      // but no need to pop anyway, because
      // we will be out of this thread.
      viewer.pushHoldRepaintWhy("pause");        
    }
    notifyResumeStatus();
    // once more to trap quit during pause
    return !error && !executionStopped;
  }

  public void notifyResumeStatus() {
    if (!chk && !executionStopped && !executionStepping) {
      viewer.scriptStatus("script execution "
          + (error || executionStopped ? "interrupted" : "resumed"));
    }
    Logger.debug("script execution resumed");
  }

  /**
   * 
   * @param millis  negative here bypasses max check 
   * @throws ScriptException
   */
  private void doDelay(int millis) throws ScriptException {
    if (!useThreads())
      return;
    if (isJS && allowJSThreads)
      throw new ScriptInterruption(this, "delay", millis);
    viewer.delayScript(this, millis);  
  }

  /**
   * 
   * @param isSpt
   * @param fromFunc TODO
   * @return false only when still working through resumeEval
   * @throws ScriptException
   */
  private boolean dispatchCommands(boolean isSpt, boolean fromFunc)
      throws ScriptException {
    long timeBegin = 0;
    if (sm == null)
      sm = viewer.getShapeManager();
    debugScript = logMessages = false;
    if (!chk)
      setDebugging();
    if (logMessages) {
      timeBegin = System.currentTimeMillis();
      viewer.scriptStatus("Eval.dispatchCommands():" + timeBegin);
      viewer.scriptStatus(script);
    }
    if (pcEnd == 0)
      pcEnd = Integer.MAX_VALUE;
    if (lineEnd == 0)
      lineEnd = Integer.MAX_VALUE;
    if (aatoken == null)
      return true;
    commandLoop(fromFunc);
    if (chk)
      return true;
    String script = viewer.getInsertedCommand();
    if (script != "") {
      runScriptBuffer(script, null);
    } else if (isSpt && debugScript && viewer.getMessageStyleChime()) {
      // specifically for ProteinExplorer
      viewer.scriptStatus("script <exiting>");
    }
    if (!isJS || !allowJSThreads || fromFunc)
      return true;
    if (mustResumeEval || thisContext == null) {
      boolean done = (thisContext == null);
      resumeEval(thisContext);
      mustResumeEval = false;
      return done;
    }
    return true;
  }

  private void commandLoop(boolean fromFunc) throws ScriptException {
    String lastCommand = "";
    boolean isForCheck = false; // indicates the stage of the for command loop
    JmolList<T[]> vProcess = null;
    long lastTime = System.currentTimeMillis();
    for (; pc < aatoken.length && pc < pcEnd; pc++) {
      if (!chk && isJS && allowJSThreads && !fromFunc) {
        // every 100-ms check for interruptions
        if (!executionPaused && System.currentTimeMillis() - lastTime > 100) {
          pc--;
          doDelay(-1);
        }
        lastTime = System.currentTimeMillis();
      }
      if (!chk && !checkContinue())
        break;
      if (lineNumbers[pc] > lineEnd)
        break;
      theToken = (aatoken[pc].length == 0 ? null : aatoken[pc][0]);
      // when checking scripts, we can't check statments
      // containing @{...}
      if (!historyDisabled && !chk
          && scriptLevel <= commandHistoryLevelMax && !tQuiet) {
        String cmdLine = getCommand(pc, true, true);
        if (theToken != null
            && cmdLine.length() > 0
            && !cmdLine.equals(lastCommand)
            && (theToken.tok == T.function
                || theToken.tok == T.parallel || !T.tokAttr(
                theToken.tok, T.flowCommand)))
          viewer.addCommand(lastCommand = cmdLine);
      }
      if (!chk) {
        String script = viewer.getInsertedCommand();
        if (script != "")
          runScript(script);
      }
      if (!setStatement(pc)) {
        Logger.info(getCommand(pc, true, false)
            + " -- STATEMENT CONTAINING @{} SKIPPED");
        continue;
      }
      thisCommand = getCommand(pc, false, true);
      fullCommand = thisCommand + getNextComment();
      getToken(0);
      iToken = 0;
      if ((listCommands || !chk && scriptLevel > 0) && !isJS) {
        int milliSecDelay = viewer.getScriptDelay();
        if (listCommands || milliSecDelay > 0) {
          if (milliSecDelay > 0)
            viewer.delayScript(this, -milliSecDelay);
          viewer.scriptEcho("$[" + scriptLevel + "." + lineNumbers[pc] + "."
              + (pc + 1) + "] " + thisCommand);
        }
      }
      if (vProcess != null
          && (theTok != T.end || slen < 2 || st[1].tok != T.process)) {
        vProcess.addLast(st);
        continue;
      }
      if (chk) {
        if (isCmdLine_c_or_C_Option)
          Logger.info(thisCommand);
        if (slen == 1 && st[0].tok != T.function
            && st[0].tok != T.parallel)
          continue;
      } else {
        if (debugScript)
          logDebugScript(0);
        if (scriptLevel == 0 && viewer.logCommands())
          viewer.log(thisCommand);
        if (logMessages && theToken != null)
          Logger.debug(theToken.toString());
      }
      if (theToken == null)
        continue;
      if (T.tokAttr(theToken.tok, T.shapeCommand))
        processShapeCommand(theToken.tok);
      else
        switch (theToken.tok) {
        case T.nada:
          if (chk || !viewer.getMessageStyleChime())
            break;
          String s = (String) theToken.value;
          if (s == null)
            break;
          if (outputBuffer == null)
            viewer.showMessage(s);
          scriptStatusOrBuffer(s);
          break;
        case T.push:
          pushContext((ContextToken) theToken);
          break;
        case T.pop:
          popContext(true, false);
          break;
        case T.colon:
          break;
        case T.gotocmd:
        case T.loop:
          if (viewer.isHeadless())
            break;
          //$FALL-THROUGH$
        case T.catchcmd:
        case T.breakcmd:
        case T.continuecmd:
        case T.elsecmd:
        case T.elseif:
        case T.end:
        case T.endifcmd:
        case T.forcmd:
        case T.ifcmd:
        case T.switchcmd:
        case T.casecmd:
        case T.defaultcmd:
        case T.whilecmd:
          isForCheck = flowControl(theToken.tok, isForCheck, vProcess);
          if (theTok == T.process)
            vProcess = null; // "end process"
          break;
        case T.animation:
          animation();
          break;
        case T.assign:
          assign();
          break;
        case T.background:
          background(1);
          break;
        case T.bind:
          bind();
          break;
        case T.bondorder:
          bondorder();
          break;
        case T.calculate:
          calculate();
          break;
        case T.cache:
          cache();
          break;
        case T.cd:
          cd();
          break;
        case T.center:
          center(1);
          break;
        case T.centerAt:
          centerAt();
          break;
        case T.color:
          color();
          break;
        case T.compare:
          compare();
          break;
        case T.configuration:
          configuration();
          break;
        case T.connect:
          connect(1);
          break;
        case T.console:
          console();
          break;
        case T.data:
          data();
          break;
        case T.define:
          define();
          break;
        case T.delay:
          delay();
          break;
        case T.delete:
          delete();
          break;
        case T.depth:
          slab(true);
          break;
        case T.display:
          display(true);
          break;
        case T.exit: // flush the queue and...
        case T.quit: // quit this only if it isn't the first command
          if (chk)
            break;
          if (pc > 0 && theToken.tok == T.exit)
            viewer.clearScriptQueue();
          executionStopped = (pc > 0 || !viewer.usingScriptQueue());
          break;
        case T.exitjmol:
          if (chk)
            return;
          viewer.exitJmol();
          break;
        case T.file:
          file();
          break;
        case T.fixed:
          fixed();
          break;
        case T.font:
          font(-1, 0);
          break;
        case T.frame:
        case T.model:
          frame(1);
          break;
        case T.parallel: // not actually found 
        case T.function:
        case T.identifier:
          function(); // when a function is a command
          break;
        case T.getproperty:
          getProperty();
          break;
        case T.help:
          help();
          break;
        case T.hide:
          display(false);
          break;
        case T.hbond:
          hbond();
          break;
        case T.history:
          history(1);
          break;
        case T.hover:
          hover();
          break;
        case T.initialize:
          if (!chk)
            viewer.initialize(!isStateScript);
          break;
        case T.invertSelected:
          invertSelected();
          break;
        case T.javascript:
          script(T.javascript, null);
          break;
        case T.load:
          load();
          break;
        case T.log:
          log();
          break;
        case T.mapProperty:
          mapProperty();
          break;
        case T.message:
          message();
          break;
        case T.minimize:
          minimize();
          break;
        case T.move:
          move();
          break;
        case T.moveto:
          moveto();
          break;
        case T.navigate:
          navigate();
          break;
        case T.pause: // resume is done differently
          pause();
          break;
        case T.plot:
        case T.quaternion:
        case T.ramachandran:
          plot(st);
          break;
        case T.print:
          print();
          break;
        case T.process:
          pushContext((ContextToken) theToken);
          if (parallelProcessor != null)
            vProcess = new  JmolList<T[]>();
          break;
        case T.prompt:
          prompt();
          break;
        case T.redomove:
        case T.undomove:
          undoRedoMove();
          break;
        case T.refresh:
          refresh();
          break;
        case T.reset:
          reset();
          break;
        case T.restore:
          restore();
          break;
        case T.restrict:
          restrict();
          break;
        case T.resume:
          if (!chk)
            resumePausedExecution();
          break;
        case T.returncmd:
          returnCmd(null);
          break;
        case T.rotate:
          rotate(false, false);
          break;
        case T.rotateSelected:
          rotate(false, true);
          break;
        case T.save:
          save();
          break;
        case T.set:
          set();
          break;
        case T.script:
          script(T.script, null);
          break;
        case T.select:
          select(1);
          break;
        case T.selectionhalos:
          selectionHalo(1);
          break;
        case T.show:
          show();
          break;
        case T.slab:
          slab(false);
          break;
        //case Token.slice:
        // slice();
        //break;
        case T.spin:
          rotate(true, false);
          break;
        case T.ssbond:
          ssbond();
          break;
        case T.step:
          if (pause())
            stepPausedExecution();
          break;
        case T.stereo:
          stereo();
          break;
        case T.structure:
          structure();
          break;
        case T.subset:
          subset();
          break;
        case T.sync:
          sync();
          break;
        case T.timeout:
          timeout(1);
          break;
        case T.translate:
          translate(false);
          break;
        case T.translateSelected:
          translate(true);
          break;
        case T.unbind:
          unbind();
          break;
        case T.vibration:
          vibration();
          break;
        case T.write:
          write(null);
          break;
        case T.zap:
          zap(true);
          break;
        case T.zoom:
          zoom(false);
          break;
        case T.zoomTo:
          zoom(true);
          break;
        default:
          error(ERROR_unrecognizedCommand);
        }
      setCursorWait(false);
      // at end because we could use continue to avoid it
      if (executionStepping) {
        executionPaused = (isCommandDisplayable(pc + 1));
      }
    }
  }

  private void cache() throws ScriptException {
    checkLength(3);
    int tok = tokAt(1);
    String fileName = parameterAsString(2);
    switch (tok) {
    case T.add:
    case T.remove:
      if (!chk) {
        if (tok == T.remove && tokAt(2) == T.all)
          fileName = null;
        int nBytes = viewer.cacheFileByName(fileName, tok == T.add);
        showString(nBytes < 0 ? "cache cleared" : nBytes + " bytes "
            + (tok == T.add ? " cached" : " removed"));
      }
      break;
    default:
      error(ERROR_invalidArgument);
    }
  }

  private void setCursorWait(boolean TF) {
    if (!chk)
      viewer.setCursor(TF ? JC.CURSOR_WAIT : JC.CURSOR_DEFAULT);
  }

  private void processShapeCommand(int tok) throws ScriptException {
    int iShape = 0;
    switch (tok) {
    case T.axes:
      iShape = JC.SHAPE_AXES;
      break;
    case T.backbone:
      iShape = JC.SHAPE_BACKBONE;
      break;
    case T.boundbox:
      iShape = JC.SHAPE_BBCAGE;
      break;
    case T.cartoon:
      iShape = JC.SHAPE_CARTOON;
      break;
    case T.contact:
      iShape = JC.SHAPE_CONTACT;
      break;
    case T.dipole:
      iShape = JC.SHAPE_DIPOLES;
      break;
    case T.dots:
      iShape = JC.SHAPE_DOTS;
      break;
    case T.draw:
      iShape = JC.SHAPE_DRAW;
      break;
    case T.echo:
      iShape = JC.SHAPE_ECHO;
      break;
    case T.ellipsoid:
      iShape = JC.SHAPE_ELLIPSOIDS;
      break;
    case T.frank:
      iShape = JC.SHAPE_FRANK;
      break;
    case T.geosurface:
      iShape = JC.SHAPE_GEOSURFACE;
      break;
    case T.halo:
      iShape = JC.SHAPE_HALOS;
      break;
    case T.isosurface:
      iShape = JC.SHAPE_ISOSURFACE;
      break;
    case T.label:
      iShape = JC.SHAPE_LABELS;
      break;
    case T.lcaocartoon:
      iShape = JC.SHAPE_LCAOCARTOON;
      break;
    case T.measurements:
    case T.measure:
      iShape = JC.SHAPE_MEASURES;
      break;
    case T.meshRibbon:
      iShape = JC.SHAPE_MESHRIBBON;
      break;
    case T.mo:
      iShape = JC.SHAPE_MO;
      break;
    case T.plot3d:
      iShape = JC.SHAPE_PLOT3D;
      break;
    case T.pmesh:
      iShape = JC.SHAPE_PMESH;
      break;
    case T.polyhedra:
      iShape = JC.SHAPE_POLYHEDRA;
      break;
    case T.ribbon:
      iShape = JC.SHAPE_RIBBONS;
      break;
    case T.rocket:
      iShape = JC.SHAPE_ROCKETS;
      break;
    case T.spacefill: // aka cpk
      iShape = JC.SHAPE_BALLS;
      break;
    case T.star:
      iShape = JC.SHAPE_STARS;
      break;
    case T.strands:
      iShape = JC.SHAPE_STRANDS;
      break;
    case T.struts:
      iShape = JC.SHAPE_STRUTS;
      break;
    case T.trace:
      iShape = JC.SHAPE_TRACE;
      break;
    case T.unitcell:
      iShape = JC.SHAPE_UCCAGE;
      break;
    case T.vector:
      iShape = JC.SHAPE_VECTORS;
      break;
    case T.wireframe:
      iShape = JC.SHAPE_STICKS;
      break;
    default:
      error(ERROR_unrecognizedCommand);
    }

    // check for "OFF/delete/NONE" with no shape to avoid loading it at all
    if (sm.getShape(iShape) == null && slen == 2) {
      switch (st[1].tok) {
      case T.off:
      case T.delete:
      case T.none:
        return;
      }
    }
    
    // atom objects:

    switch (tok) {
    case T.backbone:
    case T.cartoon:
    case T.meshRibbon:
    case T.ribbon:
    case T.rocket:
    case T.strands:
    case T.trace:
      proteinShape(iShape);
      return;
    case T.dots:
    case T.geosurface:
      dots(iShape);
      return;
    case T.ellipsoid:
      ellipsoid();
      return;
    case T.halo:
    case T.spacefill: // aka cpk
    case T.star:
      setAtomShapeSize(iShape, (tok == T.halo ? -1f : 1f));
      return;
    case T.label:
      label(1);
      return;
    case T.lcaocartoon:
      lcaoCartoon();
      return;
    case T.polyhedra:
      polyhedra();
      return;
    case T.struts:
      struts();
      return;
    case T.vector:
      vector();
      return;
    case T.wireframe:
      wireframe();
      return;
    }

    // other objects:

    switch (tok) {
    case T.axes:
      axes(1);
      return;
    case T.boundbox:
      boundbox(1);
      return;
    case T.contact:
      contact();
      return;
    case T.dipole:
      dipole();
      return;
    case T.draw:
      draw();
      return;
    case T.echo:
      echo(1, null, false);
      return;
    case T.frank:
      frank(1);
      return;
    case T.isosurface:
    case T.plot3d:
    case T.pmesh:
      isosurface(iShape);
      return;
    case T.measurements:
    case T.measure:
      measure();
      return;
    case T.mo:
      mo(false);
      return;
    case T.unitcell:
      unitcell(1);
      return;
    }
  }

  private boolean flowControl(int tok, boolean isForCheck, JmolList<T[]> vProcess)
      throws ScriptException {
    ContextToken ct;
    switch (tok) {
    case T.gotocmd:
      gotoCmd(parameterAsString(checkLast(1)));
      return isForCheck;
    case T.loop:
      // back to the beginning of this script
      if (!chk)
        pc = -1;
      delay();
      // JavaScript will not get here
      return isForCheck;
    }
    int pt = st[0].intValue;
    boolean isDone = (pt < 0 && !chk);
    boolean isOK = true;
    int ptNext = 0;
    switch (tok) {
    case T.catchcmd:
      ct = (ContextToken) theToken;
      pushContext(ct);
      if (!isDone && ct.name0 != null)
        contextVariables.put(ct.name0, ct.contextVariables.get(ct.name0));
      isOK = !isDone;
      break;
    case T.switchcmd:
    case T.defaultcmd:
    case T.casecmd:
      ptNext = Math.abs(aatoken[Math.abs(pt)][0].intValue);
      switch (isDone ? 0 : switchCmd((ContextToken) theToken, tok)) {
      case 0:
        // done
        ptNext = -ptNext;
        isOK = false;
        break;
      case -1:
        // skip this case
        isOK = false;
        break;
      case 1:
        // do this one
      }
      aatoken[pc][0].intValue = Math.abs(pt);
      theToken = aatoken[Math.abs(pt)][0];
      if (theToken.tok != T.end)
        theToken.intValue = ptNext;
      break;
    case T.ifcmd:
    case T.elseif:
      isOK = (!isDone && ifCmd());
      if (chk)
        break;
      ptNext = Math.abs(aatoken[Math.abs(pt)][0].intValue);
      ptNext = (isDone || isOK ? -ptNext : ptNext);
      aatoken[Math.abs(pt)][0].intValue = ptNext;
      if (tok == T.catchcmd)
        aatoken[pc][0].intValue = -pt; // reset to "done" state
      break;
    case T.elsecmd:
      checkLength(1);
      if (pt < 0 && !chk)
        pc = -pt - 1;
      break;
    case T.endifcmd:
      checkLength(1);
      break;
    case T.whilecmd:
      if (!isForCheck)
        pushContext((ContextToken) theToken);
      isForCheck = false;
      if (!ifCmd() && !chk) {
        pc = pt;
        popContext(true, false);
      }
      break;
    case T.breakcmd:
      if (!chk) {
        breakCmd(pt);
        break;
      }
      if (slen == 1)
        break;
      int n = intParameter(checkLast(1));
      if (chk)
        break;
      for (int i = 0; i < n; i++)
        popContext(true, false);
      break;
    case T.continuecmd:
      isForCheck = true;
      if (!chk)
        pc = pt - 1;
      if (slen > 1)
        intParameter(checkLast(1));
      break;
    case T.forcmd:
      // for (i = 1; i < 3; i = i + 1);
      // for (var i = 1; i < 3; i = i + 1);
      // for (;;;);
      // for (var x in {...}) { xxxxx }
      // for (var x in y) { xxxx }
      T token = theToken;
      int[] pts = new int[2];
      int j = 0;
      Object bsOrList = null;
      for (int i = 1, nSkip = 0; i < slen && j < 2; i++) {
        switch (tokAt(i)) {
        case T.semicolon:
          if (nSkip > 0)
            nSkip--;
          else
            pts[j++] = i;
          break;
        case T.in:
          nSkip -= 2;
          if (tokAt(++i) == T.expressionBegin || tokAt(i) == T.bitset) {
            bsOrList = atomExpressionAt(i);
            if (isBondSet)
              bsOrList = new BondSet((BS) bsOrList);
          } else {
            JmolList<SV> what = parameterExpressionList(-i, 1, false);
            if (what == null || what.size() < 1)
              error(ERROR_invalidArgument);
            SV vl = what.get(0);
            switch (vl.tok) {
            case T.bitset:
              bsOrList = SV.getBitSet(vl, false);
              break;
            case T.varray:
              bsOrList = vl.getList();
              break;
            default:
              error(ERROR_invalidArgument);
            }
          }
          i = iToken;
          break;
        case T.select:
          nSkip += 2;
          break;
        }

      }
      if (isForCheck) {
        j = (bsOrList == null ? pts[1] + 1 : 2);
      } else {
        pushContext((ContextToken) token);
        j = 2;
      }
      if (tokAt(j) == T.var)
        j++;
      String key = parameterAsString(j);
      boolean isMinusMinus = key.equals("--") || key.equals("++");
      if (isMinusMinus) {
        key = parameterAsString(++j);
      }
      SV v = null;
      if (T.tokAttr(tokAt(j), T.misc)
          || (v = getContextVariableAsVariable(key)) != null) {
        if (bsOrList == null && !isMinusMinus
            && getToken(++j).tok != T.opEQ)
          error(ERROR_invalidArgument);
        if (bsOrList == null) {
          if (isMinusMinus)
            j -= 2;
          setVariable(++j, slen - 1, key, 0);
        } else {
          // for (var x in {xx})....
          isOK = true;
          String key_incr = (key + "_incr");
          if (v == null)
            v = getContextVariableAsVariable(key_incr);
          if (v == null) {
            if (key.startsWith("_"))
              error(ERROR_invalidArgument);
            v = viewer.getOrSetNewVariable(key_incr, true);
          }
          if (!isForCheck || v.tok != T.bitset && v.tok != T.varray
              || v.intValue == Integer.MAX_VALUE) {
            if (isForCheck) {
              // someone messed with this variable -- do not continue!
              isOK = false;
            } else {
              v.set(SV.getVariable(bsOrList), false);
              v.intValue = 1;
            }
          } else {
            v.intValue++;
          }
          isOK = isOK
              && (bsOrList instanceof BS ? SV.bsSelectVar(v)
                  .cardinality() == 1 : v.intValue <= v.getList().size());
          if (isOK) {
            v = SV.selectItemVar(v);
            SV t = getContextVariableAsVariable(key);
            if (t == null)
              t = viewer.getOrSetNewVariable(key, true);
            t.set(v, false);
          }
        }
      }
      if (bsOrList == null)
        isOK = parameterExpressionBoolean(pts[0] + 1, pts[1]);
      pt++;
      if (!isOK)
        popContext(true, false);
      isForCheck = false;
      break;
    case T.end: // function, if, for, while, catch, switch
      switch (getToken(checkLast(1)).tok) {
      case T.trycmd:
        ScriptFunction trycmd = (ScriptFunction) getToken(1).value;
        if (chk)
          return false;
        runFunctionRet(trycmd, "try", null, null, true, true, true);
        return false;
      case T.catchcmd:
        popContext(true, false);
        break;
      case T.function:
      case T.parallel:
        viewer.addFunction((ScriptFunction) theToken.value);
        return isForCheck;
      case T.process:
        addProcess(vProcess, pt, pc);
        popContext(true, false);
        break;
      case T.switchcmd:
        if (pt > 0 && switchCmd((ContextToken) aatoken[pt][0], 0) == -1) {
          // check for the default position
          for (; pt < pc; pt++)
            if ((tok = aatoken[pt][0].tok) != T.defaultcmd
                && tok != T.casecmd)
              break;
          isOK = (pc == pt);
        }
        break;
      }
      if (isOK)
        isOK = (theTok == T.catchcmd || theTok == T.process
            || theTok == T.ifcmd || theTok == T.switchcmd);
      isForCheck = (theTok == T.forcmd || theTok == T.whilecmd);
      break;
    }
    if (!isOK && !chk)
      pc = Math.abs(pt) - 1;
    return isForCheck;
  }

  private void gotoCmd(String strTo) throws ScriptException {
    int pcTo = (strTo == null ? aatoken.length - 1 : -1);
    String s = null;
    for (int i = pcTo + 1; i < aatoken.length; i++) {
      T[] tokens = aatoken[i];
      int tok = tokens[0].tok;
      switch (tok) {
      case T.message:
      case T.nada:
        s = (String) tokens[tokens.length - 1].value;
        if (tok == T.nada)
          s = s.substring(s.startsWith("#") ? 1 : 2);
        break;
      default:
        continue;
      }
      if (s.equalsIgnoreCase(strTo)) {
        pcTo = i;
        break;
      }
    }
    if (pcTo < 0)
      error(ERROR_invalidArgument);
    if (strTo == null)
      pcTo = 0;
    int di = (pcTo < pc ? 1 : -1);
    int nPush = 0;
    for (int i = pcTo; i != pc; i += di) {
      switch (aatoken[i][0].tok) {
      case T.push:
      case T.process:
      case T.forcmd:
      case T.catchcmd:
      case T.whilecmd:
        nPush++;
        break;
      case T.pop:
        nPush--;
        break;
      case T.end:
        switch (aatoken[i][1].tok) {
        case T.process:
        case T.forcmd:
        case T.catchcmd:
        case T.whilecmd:
          nPush--;
        }
        break;
      }
    }
    if (strTo == null) {
      pcTo = Integer.MAX_VALUE;
      for (; nPush > 0; --nPush)
        popContext(false, false);
    }
    if (nPush != 0)
      error(ERROR_invalidArgument);
    if (!chk)
      pc = pcTo - 1; // ... resetting the program counter
  }

  private void breakCmd(int pt) {
    // pt is a backward reference
    if (pt < 0) {
      // this is a break within a try{...} block
      getContextVariableAsVariable("_breakval").intValue = -pt;
      pcEnd = pc;
      return;
    }
    pc = Math.abs(aatoken[pt][0].intValue);
    int tok = aatoken[pt][0].tok;
    if (tok == T.casecmd || tok == T.defaultcmd) {
      theToken = aatoken[pc--][0];
      int ptNext = Math.abs(theToken.intValue);
      if (theToken.tok != T.end)
        theToken.intValue = -ptNext;
    } else {
      while (thisContext != null
          && !ScriptCompiler.isBreakableContext(thisContext.token.tok))
        popContext(true, false);
      popContext(true, false);
    }
  }

  static int iProcess;

  private void addProcess(JmolList<T[]> vProcess, int pc, int pt) {
    if (parallelProcessor == null)
      return;
    T[][] statements = new T[pt][];
    for (int i = 0; i < vProcess.size(); i++)
      statements[i + 1 - pc] = vProcess.get(i);
    ScriptContext context = getScriptContext();
    context.aatoken = statements;
    context.pc = 1 - pc;
    context.pcEnd = pt;
    parallelProcessor.addProcess("p" + (++iProcess), context);
  }

  private int switchCmd(ContextToken c, int tok) throws ScriptException {
    if (tok == T.switchcmd)
      c.addName("_var");
    SV var = c.contextVariables.get("_var");
    if (var == null)
      return 1; // OK, case found -- no more testing
    if (tok == 0) {
      // end: remove variable and do default
      //      this causes all other cases to
      //      skip
      c.contextVariables.remove("_var");
      return -1;
    }
    if (tok == T.defaultcmd) // never do the default one directly
      return -1;
    SV v = parameterExpressionToken(1);
    if (tok == T.casecmd) {
      boolean isOK = SV.areEqual(var, v);
      if (isOK)
        c.contextVariables.remove("_var");
      return isOK ? 1 : -1;
    }
    c.contextVariables.put("_var", v);
    return 1;
  }

  private boolean ifCmd() throws ScriptException {
    return parameterExpressionBoolean(1, 0);
  }

  private void returnCmd(SV tv) throws ScriptException {
    SV t = getContextVariableAsVariable("_retval");
    if (t == null) {
      if (!chk)
        gotoCmd(null);
      return;
    }
    SV v = (tv != null || slen == 1 ? null
        : parameterExpressionToken(1));
    if (chk)
      return;
    if (tv == null)
      tv = (v == null ? new ScriptVariableInt(0) : v);
    t.value = tv.value;
    t.intValue = tv.intValue;
    t.tok = tv.tok;
    gotoCmd(null);
  }

  private void help() throws ScriptException {
    if (chk)
      return;
    String what = optParameterAsString(1).toLowerCase();
    int pt = 0;
    if (what.startsWith("mouse") && (pt = what.indexOf(" ")) >= 0
        && pt == what.lastIndexOf(" ")) {
      showString(viewer.getBindingInfo(what.substring(pt + 1)));
      return;
    }
    if (T.tokAttr(T.getTokFromName(what), T.scriptCommand))
      what = "?command=" + what;
    viewer.getHelp(what);
  }

  private void move() throws ScriptException {
    if (slen > 11)
      error(ERROR_badArgumentCount);
    // rotx roty rotz zoom transx transy transz slab seconds fps
    V3 dRot = V3.new3(floatParameter(1), floatParameter(2),
        floatParameter(3));
    float dZoom = floatParameter(4);
    V3 dTrans = V3.new3(intParameter(5), intParameter(6),
        intParameter(7));
    float dSlab = floatParameter(8);
    float floatSecondsTotal = floatParameter(9);
    int fps = (slen == 11 ? intParameter(10) : 30);
    if (chk)
      return;
    refresh();
    if (!useThreads())
      floatSecondsTotal = 0;
    viewer.move(this, dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
    if (floatSecondsTotal > 0 && isJS)
      throw new ScriptInterruption(this,"move", 1);
  }

  private void moveto() throws ScriptException {
    // moveto time
    // moveto [time] { x y z deg} zoom xTrans yTrans (rotCenter) rotationRadius
    // (navCenter) xNav yNav navDepth
    // moveto [time] { x y z deg} 0 xTrans yTrans (rotCenter) [zoom factor]
    // (navCenter) xNav yNav navDepth
    // moveto [time] { x y z deg} (rotCenter) [zoom factor] (navCenter) xNav
    // yNav navDepth
    // where [zoom factor] is [0|n|+n|-n|*n|/n|IN|OUT]
    // moveto [time] front|back|left|right|top|bottom
    if (slen == 2 && tokAt(1) == T.stop) {
      if (!chk)
        viewer.stopMotion();
      return;
    }
    float floatSecondsTotal;
    if (slen == 2 && isFloatParameter(1)) {
      floatSecondsTotal = floatParameter(1);
      if (chk)
        return;
      if (!useThreads())
        floatSecondsTotal = 0;
      if (floatSecondsTotal > 0)
        refresh();
      viewer.moveTo(this, floatSecondsTotal, null, JC.axisZ, 0, null, 100, 0, 0, 0,
          null, Float.NaN, Float.NaN, Float.NaN);
      if (isJS && floatSecondsTotal > 0 && viewer.waitForMoveTo())
        throw new ScriptInterruption(this, "moveTo", 1);
      return;
    }
    V3 axis = V3.new3(Float.NaN, 0, 0);
    P3 center = null;
    int i = 1;
    floatSecondsTotal = (isFloatParameter(i) ? floatParameter(i++) : 2.0f);
    float degrees = 90;
    BS bsCenter = null;
    switch (getToken(i).tok) {
    case T.quaternion:
      Quaternion q;
      boolean isMolecular = false;
      if (tokAt(++i) == T.molecular) {
        // see comment below
        isMolecular = true;
        i++;
      }
      if (tokAt(i) == T.bitset || tokAt(i) == T.expressionBegin) {
        isMolecular = true;
        center = centerParameter(i);
        if (!(expressionResult instanceof BS))
          error(ERROR_invalidArgument);
        bsCenter = (BS) expressionResult;
        q = (chk ? new Quaternion() : viewer
            .getAtomQuaternion(bsCenter.nextSetBit(0)));
      } else {
        q = getQuaternionParameter(i);
      }
      i = iToken + 1;
      if (q == null)
        error(ERROR_invalidArgument);
      AxisAngle4f aa = q.toAxisAngle4f();
      axis.set(aa.x, aa.y, aa.z);
      /*
       * The quaternion angle for an atom represents the angle by which the
       * reference frame must be rotated to match the frame defined for the
       * residue.
       * 
       * However, to "moveTo" this frame as the REFERENCE frame, what we have to
       * do is take that quaternion frame and rotate it BACKWARD by that many
       * degrees. Then it will match the reference frame, which is ultimately
       * our window frame.
       * 
       * We only apply this for molecular-type quaternions, because in general
       * the orientation quaternion refers to how the reference plane has been
       * changed (the orientation matrix)
       */
      degrees = (isMolecular ? -1 : 1) * (float) (aa.angle * 180.0 / Math.PI);
      break;
    case T.point4f:
    case T.point3f:
    case T.leftbrace:
      // {X, Y, Z} deg or {x y z deg}
      if (isPoint3f(i)) {
        axis.setT(getPoint3f(i, true));
        i = iToken + 1;
        degrees = floatParameter(i++);
      } else {
        Point4f pt4 = getPoint4f(i);
        i = iToken + 1;
        axis.set(pt4.x, pt4.y, pt4.z);
        degrees = (pt4.x == 0 && pt4.y == 0 && pt4.z == 0 ? Float.NaN : pt4.w);
      }
      break;
    case T.front:
      axis.set(1, 0, 0);
      degrees = 0f;
      checkLength(++i);
      break;
    case T.back:
      axis.set(0, 1, 0);
      degrees = 180f;
      checkLength(++i);
      break;
    case T.left:
      axis.set(0, 1, 0);
      checkLength(++i);
      break;
    case T.right:
      axis.set(0, -1, 0);
      checkLength(++i);
      break;
    case T.top:
      axis.set(1, 0, 0);
      checkLength(++i);
      break;
    case T.bottom:
      axis.set(-1, 0, 0);
      checkLength(++i);
      break;
    default:
      // X Y Z deg
      axis = V3.new3(floatParameter(i++), floatParameter(i++),
          floatParameter(i++));
      degrees = floatParameter(i++);
    }
    if (Float.isNaN(axis.x) || Float.isNaN(axis.y) || Float.isNaN(axis.z))
      axis.set(0, 0, 0);
    else if (axis.length() == 0 && degrees == 0)
      degrees = Float.NaN;
    boolean isChange = !viewer.isInPosition(axis, degrees);
    // optional zoom
    float zoom = (isFloatParameter(i) ? floatParameter(i++) : Float.NaN);
    // optional xTrans yTrans
    float xTrans = 0;
    float yTrans = 0;
    if (isFloatParameter(i) && !isCenterParameter(i)) {
      xTrans = floatParameter(i++);
      yTrans = floatParameter(i++);
      if (!isChange && Math.abs(xTrans - viewer.getTranslationXPercent()) >= 1)
        isChange = true;
      if (!isChange && Math.abs(yTrans - viewer.getTranslationYPercent()) >= 1)
        isChange = true;
    }
    if (bsCenter == null && i != slen) {
      // if any more, required (center)
      center = centerParameter(i);
      if (expressionResult instanceof BS)
        bsCenter = (BS) expressionResult;
      i = iToken + 1;
    }
    float rotationRadius = Float.NaN;
    float zoom0 = viewer.getZoomSetting();
    if (center != null) {
      if (!isChange && center.distance(viewer.getRotationCenter()) >= 0.1)
        isChange = true;
      // optional {center} rotationRadius
      if (isFloatParameter(i))
        rotationRadius = floatParameter(i++);
      if (!isCenterParameter(i)) {
        if ((rotationRadius == 0 || Float.isNaN(rotationRadius))
            && (zoom == 0 || Float.isNaN(zoom))) {
          // alternative (atom expression) 0 zoomFactor
          float newZoom = Math
              .abs(getZoom(0, i, bsCenter, (zoom == 0 ? 0 : zoom0)));
          i = iToken + 1;
          zoom = newZoom;
        } else {
          if (!isChange
              && Math.abs(rotationRadius - viewer.getRotationRadius()) >= 0.1)
            isChange = true;
        }
      }
    }
    if (zoom == 0 || Float.isNaN(zoom))
      zoom = 100;
    if (Float.isNaN(rotationRadius))
      rotationRadius = 0;

    if (!isChange && Math.abs(zoom - zoom0) >= 1)
      isChange = true;
    // (navCenter) xNav yNav navDepth

    P3 navCenter = null;
    float xNav = Float.NaN;
    float yNav = Float.NaN;
    float navDepth = Float.NaN;

    if (i != slen) {
      navCenter = centerParameter(i);
      i = iToken + 1;
      if (i != slen) {
        xNav = floatParameter(i++);
        yNav = floatParameter(i++);
      }
      if (i != slen)
        navDepth = floatParameter(i++);
    }

    if (i != slen)
      error(ERROR_badArgumentCount);

    if (chk)
      return;
    if (!isChange)
      floatSecondsTotal = 0;
    if (floatSecondsTotal > 0)
      refresh();
    if (!useThreads())
        floatSecondsTotal = 0;
    viewer.moveTo(this, floatSecondsTotal, center, axis, degrees, null, zoom,
        xTrans, yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
    if (isJS && floatSecondsTotal > 0 && viewer.waitForMoveTo())
      throw new ScriptInterruption(this, "moveTo", 1);
  }

  private void navigate() throws ScriptException {
    /*
     * navigation on/off navigation depth p # would be as a depth value, like
     * slab, in percent, but could be negative navigation nSec translate X Y #
     * could be percentages navigation nSec translate $object # could be a draw
     * object navigation nSec translate (atom selection) #average of values
     * navigation nSec center {x y z} navigation nSec center $object navigation
     * nSec center (atom selection) navigation nSec path $object navigation nSec
     * path {x y z theta} {x y z theta}{x y z theta}{x y z theta}... navigation
     * nSec trace (atom selection)
     */
    if (slen == 1) {
      setBooleanProperty("navigationMode", true);
      return;
    }
    V3 rotAxis = V3.new3(0, 1, 0);
    JmolList<Object[]> list = new  JmolList<Object[]>();
    P3 pt;
    if (slen == 2) {
      switch (getToken(1).tok) {
      case T.on:
      case T.off:
        if (chk)
          return;
        setObjectMad(JC.SHAPE_AXES, "axes", 1);
        setShapeProperty(JC.SHAPE_AXES, "position", P3.new3(50,
            50, Float.MAX_VALUE));
        setBooleanProperty("navigationMode", true);
        viewer.setNavOn(theTok == T.on);
        return;
      case T.stop:
        if (!chk)
          viewer.setNavXYZ(0, 0, 0);
        return;
      case T.point3f:
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (!viewer.getNavigationMode())
      setBooleanProperty("navigationMode", true);
    for (int i = 1; i < slen; i++) {
      float timeSec = (isFloatParameter(i) ? floatParameter(i++) : 2f);
      if (timeSec < 0)
        error(ERROR_invalidArgument);
      if (!chk && timeSec > 0)
        refresh();
      switch (getToken(i).tok) {
      case T.point3f:
      case T.leftbrace:
        // navigate {x y z}
        pt = getPoint3f(i, true);
        iToken++;
        if (iToken != slen)
          error(ERROR_invalidArgument);
        if (chk)
          return;
        viewer.setNavXYZ(pt.x, pt.y, pt.z);
        return;
      case T.depth:
        float depth = floatParameter(++i);
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.depth), Float.valueOf(timeSec), Float.valueOf(depth)});
          //viewer.setNavigationDepthPercent(timeSec, depth);
        continue;
      case T.center:
        pt = centerParameter(++i);
        i = iToken;
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.point), Float.valueOf(timeSec), pt});
          //viewer.navigatePt(timeSec, pt);
        continue;
      case T.rotate:
        switch (getToken(++i).tok) {
        case T.x:
          rotAxis.set(1, 0, 0);
          i++;
          break;
        case T.y:
          rotAxis.set(0, 1, 0);
          i++;
          break;
        case T.z:
          rotAxis.set(0, 0, 1);
          i++;
          break;
        case T.point3f:
        case T.leftbrace:
          rotAxis.setT(getPoint3f(i, true));
          i = iToken + 1;
          break;
        case T.identifier:
          error(ERROR_invalidArgument); // for now
          break;
        }
        float degrees = floatParameter(i);
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.rotate), Float.valueOf(timeSec), rotAxis, Float.valueOf(degrees)});
//          viewer.navigateAxis(timeSec, rotAxis, degrees);
        continue;
      case T.translate:
        float x = Float.NaN;
        float y = Float.NaN;
        if (isFloatParameter(++i)) {
          x = floatParameter(i);
          y = floatParameter(++i);
        } else {
          switch (tokAt(i)) {
          case T.x:
            x = floatParameter(++i);
            break;
          case T.y:
            y = floatParameter(++i);
            break;
          default:
            pt = centerParameter(i);
            i = iToken;
            if (!chk)
              list.addLast(new Object[] { Integer.valueOf(T.translate), Float.valueOf(timeSec), pt});
              //viewer.navTranslate(timeSec, pt);
            continue;
          }
        }
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.percent), Float.valueOf(timeSec), 
              Float.valueOf(x), Float.valueOf(y) });
          //viewer.navTranslatePercent(timeSec, x, y);
        continue;
      case T.divide:
        continue;
      case T.trace:
        P3[][] pathGuide;
        JmolList<P3[]> vp = new  JmolList<P3[]>();
        BS bs = atomExpressionAt(++i);
        i = iToken;
        if (chk)
          return;
        viewer.getPolymerPointsAndVectors(bs, vp);
        int n;
        if ((n = vp.size()) > 0) {
          pathGuide = new P3[n][];
          for (int j = 0; j < n; j++) {
            pathGuide[j] = vp.get(j);
          }
          list.addLast(new Object[] { Integer.valueOf(T.trace), Float.valueOf(timeSec), pathGuide});
          //viewer.navigateGuide(timeSec, pathGuide);
          continue;
        }
        break;
     case T.path:
        P3[] path;
        float[] theta = null; // orientation; null for now
        if (getToken(i + 1).tok == T.dollarsign) {
          i++;
          // navigate timeSeconds path $id indexStart indexEnd
          String pathID = objectNameParameter(++i);
          if (chk)
            return;
          setShapeProperty(JC.SHAPE_DRAW, "thisID", pathID);
          path = (P3[]) getShapeProperty(JC.SHAPE_DRAW,
              "vertices");
          refresh();
          if (path == null)
            error(ERROR_invalidArgument);
          int indexStart = (int) (isFloatParameter(i + 1) ? floatParameter(++i)
              : 0);
          int indexEnd = (int) (isFloatParameter(i + 1) ? floatParameter(++i)
              : Integer.MAX_VALUE);
          if (!chk)
            list.addLast(new Object[] {Integer.valueOf(T.path), Float.valueOf(timeSec), path, theta, new int[] {indexStart, indexEnd}});
            //viewer.navigatePath(timeSec, path, theta, indexStart, indexEnd);
          continue;
        }
        JmolList<P3> v = new  JmolList<P3>();
        while (isCenterParameter(i + 1)) {
          v.addLast(centerParameter(++i));
          i = iToken;
        }
        if (v.size() > 0) {
          path = v.toArray(new P3[v.size()]);
          if (!chk)
            list.addLast(new Object[] {Integer.valueOf(T.path), Float.valueOf(timeSec), path, theta, new int[] {0, Integer.MAX_VALUE}});
            //viewer.navigatePath(timeSec, path, theta, 0, Integer.MAX_VALUE);
          continue;
        }
        //$FALL-THROUGH$
      default:
        error(ERROR_invalidArgument);
      }
    }
    viewer.navigateList(this, list);
  }

  private void bondorder() throws ScriptException {
    checkLength(-3);
    int order = 0;
    switch (getToken(1).tok) {
    case T.integer:
    case T.decimal:
      if ((order = JmolEdge.getBondOrderFromFloat(floatParameter(1))) == JmolEdge.BOND_ORDER_NULL)
        error(ERROR_invalidArgument);
      break;
    default:
      if ((order = getBondOrderFromString(parameterAsString(1))) == JmolEdge.BOND_ORDER_NULL)
        error(ERROR_invalidArgument);
      // generic partial can be indicated by "partial n.m"
      if (order == JmolEdge.BOND_PARTIAL01 && tokAt(2) == T.decimal) {
        order = getPartialBondOrderFromFloatEncodedInt(st[2].intValue);
      }
    }
    setShapeProperty(JC.SHAPE_STICKS, "bondOrder", Integer
        .valueOf(order));
  }

  private void console() throws ScriptException {
    switch (getToken(1).tok) {
    case T.off:
      if (!chk)
        viewer.showConsole(false);
      break;
    case T.on:
      if (!chk)
        viewer.showConsole(true);
      break;
    case T.clear:
      if (!chk)
        viewer.clearConsole();
      break;
    case T.write:
      showString(stringParameter(2));
      break;
    default:
      error(ERROR_invalidArgument);
    }
  }

  private void centerAt() throws ScriptException {
    String relativeTo = null;
    switch (getToken(1).tok) {
    case T.absolute:
      relativeTo = "absolute";
      break;
    case T.average:
      relativeTo = "average";
      break;
    case T.boundbox:
      relativeTo = "boundbox";
      break;
    default:
      error(ERROR_invalidArgument);
    }
    P3 pt = P3.new3(0, 0, 0);
    if (slen == 5) {
      // centerAt xxx x y z
      pt.x = floatParameter(2);
      pt.y = floatParameter(3);
      pt.z = floatParameter(4);
    } else if (isCenterParameter(2)) {
      pt = centerParameter(2);
      checkLast(iToken);
    } else {
      checkLength(2);
    }
    if (!chk)
      viewer.setCenterAt(relativeTo, pt);
  }

  private void stereo() throws ScriptException {
    EnumStereoMode stereoMode = EnumStereoMode.DOUBLE;
    // see www.usm.maine.edu/~rhodes/0Help/StereoViewing.html
    // stereo on/off
    // stereo color1 color2 6
    // stereo redgreen 5

    float degrees = EnumStereoMode.DEFAULT_STEREO_DEGREES;
    boolean degreesSeen = false;
    int[] colors = null;
    int colorpt = 0;
    for (int i = 1; i < slen; ++i) {
      if (isColorParam(i)) {
        if (colorpt > 1)
          error(ERROR_badArgumentCount);
        if (colorpt == 0)
          colors = new int[2];
        if (!degreesSeen)
          degrees = 3;
        colors[colorpt] = getArgbParam(i);
        if (colorpt++ == 0)
          colors[1] = ~colors[0];
        i = iToken;
        continue;
      }
      switch (getToken(i).tok) {
      case T.on:
        checkLast(iToken = 1);
        iToken = 1;
        break;
      case T.off:
        checkLast(iToken = 1);
        stereoMode = EnumStereoMode.NONE;
        break;
      case T.integer:
      case T.decimal:
        degrees = floatParameter(i);
        degreesSeen = true;
        break;
      case T.identifier:
        if (!degreesSeen)
          degrees = 3;
        stereoMode = EnumStereoMode.getStereoMode(parameterAsString(i));
        if (stereoMode != null)
          break;
        //$FALL-THROUGH$
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (chk)
      return;
    viewer.setStereoMode(colors, stereoMode, degrees);
  }

  private void compare() throws ScriptException {
    // compare {model1} {model2} [atoms] {bsAtoms1} {bsAtoms2}
    // compare {model1} {model2} orientations
    // compare {model1} {model2} orientations {bsAtoms1} {bsAtoms2}
    // compare {model1} {model2} [orientations] [quaternionList1] [quaternionList2]
    // compare {model1} {model2} SMILES "....."
    // compare {model1} {model2} SMARTS "....."
    // compare {model1} {model2} FRAMES 

    boolean isQuaternion = false;
    boolean doRotate = false;
    boolean doTranslate = false;
    boolean doAnimate = false;
    float nSeconds = Float.NaN;
    Quaternion[] data1 = null, data2 = null;
    BS bsAtoms1 = null, bsAtoms2 = null;
    JmolList<BS[]> vAtomSets = null;
    JmolList<Object[]> vQuatSets = null;
    BS bsFrom = (tokAt(1) == T.subset ? null : atomExpressionAt(1));
    BS bsTo = (tokAt(++iToken) == T.subset ? null : atomExpressionAt(iToken));
    if (bsFrom == null || bsTo == null)
      error(ERROR_invalidArgument);
    BS bsSubset = null;
    boolean isSmiles = false;
    String strSmiles = null;
    BS bs = BSUtil.copy(bsFrom);
    bs.or(bsTo);
    boolean isToSubsetOfFrom = bs.equals(bsFrom);
    boolean isFrames = isToSubsetOfFrom;
    for (int i = iToken + 1; i < slen; ++i) {
      switch (getToken(i).tok) {
      case T.frame:
        isFrames = true;
        break;
      case T.smiles:
        isSmiles = true;
        //$FALL-THROUGH$
      case T.search:
        strSmiles = stringParameter(++i);
        break;
      case T.decimal:
      case T.integer:
        nSeconds = Math.abs(floatParameter(i));
        if (nSeconds > 0)
          doAnimate = true;
        break;
      case T.comma:
        break;
      case T.subset:
        bsSubset = atomExpressionAt(++i);
        i = iToken;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (vQuatSets != null)
          error(ERROR_invalidArgument);
        bsAtoms1 = atomExpressionAt(iToken);
        int tok = (isToSubsetOfFrom ? 0 : tokAt(iToken + 1));
        bsAtoms2 = (tok == T.bitset || tok == T.expressionBegin ? atomExpressionAt(++iToken)
            : BSUtil.copy(bsAtoms1));
        if (bsSubset != null) {
          bsAtoms1.and(bsSubset);
          bsAtoms2.and(bsSubset);
        }
        bsAtoms2.and(bsTo);
        if (vAtomSets == null)
          vAtomSets = new  JmolList<BS[]>();
        vAtomSets.addLast(new BS[] { bsAtoms1, bsAtoms2 });
        i = iToken;
        break;
      case T.varray:
        if (vAtomSets != null)
          error(ERROR_invalidArgument);
        isQuaternion = true;
        data1 = ScriptMathProcessor
            .getQuaternionArray(((SV) theToken).getList(), T.list);
        getToken(++i);
        data2 = ScriptMathProcessor
            .getQuaternionArray(((SV) theToken).getList(), T.list);
        if (vQuatSets == null)
          vQuatSets = new  JmolList<Object[]>();
        vQuatSets.addLast(new Object[] { data1, data2 });
        break;
      case T.orientation:
        isQuaternion = true;
        break;
      case T.point:
      case T.atoms:
        isQuaternion = false;
        break;
      case T.rotate:
        doRotate = true;
        break;
      case T.translate:
        doTranslate = true;
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (chk)
      return;

    if (isFrames)
      nSeconds = 0;
    if (Float.isNaN(nSeconds) || nSeconds < 0)
      nSeconds = 1;
    else if (!doRotate && !doTranslate)
      doRotate = doTranslate = true;
    doAnimate = (nSeconds != 0);

    boolean isAtoms = (!isQuaternion && strSmiles == null);
    if (vAtomSets == null && vQuatSets == null) {
      if (bsSubset == null) {
        bsAtoms1 = (isAtoms ? viewer.getAtomBitSet("spine") : new BS());
        if (bsAtoms1.nextSetBit(0) < 0) {
          bsAtoms1 = bsFrom;
          bsAtoms2 = bsTo;
        } else {
          bsAtoms2 = BSUtil.copy(bsAtoms1);
          bsAtoms1.and(bsFrom);
          bsAtoms2.and(bsTo);
        }
      } else {
        bsAtoms1 = BSUtil.copy(bsFrom);
        bsAtoms2 = BSUtil.copy(bsTo);
        bsAtoms1.and(bsSubset);
        bsAtoms2.and(bsSubset);
        bsAtoms1.and(bsFrom);
        bsAtoms2.and(bsTo);
      }
      vAtomSets = new  JmolList<BS[]>();
      vAtomSets.addLast(new BS[] { bsAtoms1, bsAtoms2 });
    }

    BS[] bsFrames;
    if (isFrames) {
      BS bsModels = viewer.getModelBitSet(bsFrom, false);
      bsFrames = new BS[bsModels.cardinality()];
      for (int i = 0, iModel = bsModels.nextSetBit(0); iModel >= 0; iModel = bsModels.nextSetBit(iModel + 1), i++)
        bsFrames[i] = viewer.getModelUndeletedAtomsBitSet(iModel);       
    } else {
      bsFrames = new BS[] { bsFrom };
    }
    for (int iFrame = 0; iFrame < bsFrames.length; iFrame++) {
      bsFrom = bsFrames[iFrame];
      float[] retStddev = new float[2]; // [0] final, [1] initial for atoms
      Quaternion q = null;
      JmolList<Quaternion> vQ = new  JmolList<Quaternion>();
      P3[][] centerAndPoints = null;
      JmolList<BS[]> vAtomSets2 = (isFrames ? new  JmolList<BS[]>()
          : vAtomSets);
      for (int i = 0; i < vAtomSets.size(); ++i) {
        BS[] bss = vAtomSets.get(i);
        if (isFrames)
          vAtomSets2
              .addLast(bss = new BS[] { BSUtil.copy(bss[0]), bss[1] });
        bss[0].and(bsFrom);
      }
      if (isAtoms) {
        centerAndPoints = viewer.getCenterAndPoints(vAtomSets2, true);
        q = Measure.calculateQuaternionRotation(centerAndPoints, retStddev,
            true);
        float r0 = (Float.isNaN(retStddev[1]) ? Float.NaN
            : Math.round(retStddev[0] * 100) / 100f);
        float r1 = (Float.isNaN(retStddev[1]) ? Float.NaN
            : Math.round(retStddev[1] * 100) / 100f);
        showString("RMSD " + r0 + " --> " + r1 + " Angstroms");
      } else if (isQuaternion) {
        if (vQuatSets == null) {
          for (int i = 0; i < vAtomSets2.size(); i++) {
            BS[] bss = vAtomSets2.get(i);
            data1 = viewer.getAtomGroupQuaternions(bss[0], Integer.MAX_VALUE);
            data2 = viewer.getAtomGroupQuaternions(bss[1], Integer.MAX_VALUE);
            for (int j = 0; j < data1.length && j < data2.length; j++) {
              vQ.addLast(data2[j].div(data1[j]));
            }
          }
        } else {
          for (int j = 0; j < data1.length && j < data2.length; j++) {
            vQ.addLast(data2[j].div(data1[j]));
          }
        }
        retStddev[0] = 0;
        data1 = vQ.toArray(new Quaternion[vQ.size()]);
        q = Quaternion.sphereMean(data1, retStddev, 0.0001f);
        showString("RMSD = " + retStddev[0] + " degrees");
      } else {
        // SMILES
        /* not sure why this was like this:
        if (vAtomSets == null) {
          vAtomSets = new  JmolList<BitSet[]>();
        }
        bsAtoms1 = BitSetUtil.copy(bsFrom);
        bsAtoms2 = BitSetUtil.copy(bsTo);
        vAtomSets.add(new BitSet[] { bsAtoms1, bsAtoms2 });
        */

        Matrix4f m4 = new Matrix4f();
        float stddev = getSmilesCorrelation(bsFrom, bsTo, strSmiles, null,
            null, m4, null, !isSmiles, false);
        if (Float.isNaN(stddev))
          error(ERROR_invalidArgument);
        V3 translation = new V3();
        m4.get(translation);
        Matrix3f m3 = new Matrix3f();
        m4.getRotationScale(m3);
        q = Quaternion.newM(m3);
      }
      if (centerAndPoints == null)
        centerAndPoints = viewer.getCenterAndPoints(vAtomSets2, true);
      P3 pt1 = new P3();
      float endDegrees = Float.NaN;
      V3 translation = null;
      if (doTranslate) {
        translation = V3.newV(centerAndPoints[1][0]);
        translation.sub(centerAndPoints[0][0]);
        endDegrees = 0;
      }
      if (doRotate) {
        if (q == null)
          evalError("option not implemented", null);
        pt1.setT(centerAndPoints[0][0]);
        pt1.add(q.getNormal());
        endDegrees = q.getTheta();
      }
      if (Float.isNaN(endDegrees) || Float.isNaN(pt1.x))
        continue;
      JmolList<P3> ptsB = null;
      if (doRotate && doTranslate && nSeconds != 0) {
        JmolList<P3> ptsA = viewer.getAtomPointVector(bsFrom);
        Matrix4f m4 = ScriptMathProcessor.getMatrix4f(q.getMatrix(),
            translation);
        ptsB = Measure.transformPoints(ptsA, m4, centerAndPoints[0][0]);
      }
      if (!useThreads())
        doAnimate = false;
      viewer.rotateAboutPointsInternal(this, centerAndPoints[0][0], pt1, endDegrees
          / nSeconds, endDegrees, doAnimate, bsFrom, translation, ptsB);
      if (doAnimate && isJS)
        throw new ScriptInterruption(this, "compare", 1);
    }
  }

  float getSmilesCorrelation(BS bsA, BS bsB, String smiles,
                             JmolList<P3> ptsA, JmolList<P3> ptsB,
                             Matrix4f m, JmolList<BS> vReturn, 
                             boolean isSmarts, boolean asMap)
      throws ScriptException {
    float tolerance = 0.1f; // TODO
    try {
      if (ptsA == null) {
        ptsA = new  JmolList<P3>();
        ptsB = new  JmolList<P3>();
      }
      if (m == null)
        m = new Matrix4f();

      Atom[] atoms = viewer.modelSet.atoms;
      int atomCount = viewer.getAtomCount();
      int[][] maps = viewer.getSmilesMatcher().getCorrelationMaps(smiles,
          atoms, atomCount, bsA, isSmarts, true);
      if (maps == null)
        evalError(viewer.getSmilesMatcher().getLastException(), null);
      if (maps.length == 0)
        return Float.NaN;
      for (int i = 0; i < maps[0].length; i++)
        ptsA.addLast(atoms[maps[0][i]]);
      maps = viewer.getSmilesMatcher().getCorrelationMaps(smiles, atoms,
          atomCount, bsB, isSmarts, false);
      if (maps == null)
        evalError(viewer.getSmilesMatcher().getLastException(), null);
      if (maps.length == 0)
        return Float.NaN;
      if (asMap) {
        for (int i = 0; i < maps.length; i++)
          for (int j = 0; j < maps[i].length; j++)
            ptsB.addLast(atoms[maps[i][j]]);
        return 0;
      }
      float lowestStdDev = Float.MAX_VALUE;
      int[] mapB = null;
      for (int i = 0; i < maps.length; i++) {
        ptsB.clear();
        for (int j = 0; j < maps[i].length; j++)
          ptsB.addLast(atoms[maps[i][j]]);
        float stddev = Measure.getTransformMatrix4(ptsA, ptsB, m, null);
        Logger.info("getSmilesCorrelation stddev=" + stddev);
        if (vReturn != null) {
          if (stddev < tolerance) {
            BS bs = new BS();
            for (int j = 0; j < maps[i].length; j++)
              bs.set(maps[i][j]);
            vReturn.addLast(bs);
          }
        }
        if (stddev < lowestStdDev) {
          mapB = maps[i];
          lowestStdDev = stddev;
        }
      }
      for (int i = 0; i < mapB.length; i++)
        ptsB.addLast(atoms[mapB[i]]);
      return lowestStdDev;
    } catch (Exception e) {
      //System.out.println(e.getMessage());
      evalError(e.toString(), null);
      return 0; // unattainable
    }
  }

  Object getSmilesMatches(String pattern, String smiles, BS bsSelected,
                          BS bsMatch3D, boolean isSmarts,
                          boolean asOneBitset) throws ScriptException {
    if (chk) {
      if (asOneBitset)
        return new BS();
      return new String[] { "({})" };
    }

    // just retrieving the SMILES or bioSMILES string

    if (pattern.length() == 0) {
      boolean isBioSmiles = (!asOneBitset);
      Object ret = viewer.getSmiles(0, 0, bsSelected, isBioSmiles, false, true,
          true);
      if (ret == null)
        evalError(viewer.getSmilesMatcher().getLastException(), null);
      return ret;
    }

    boolean asAtoms = true;
    BS[] b;
    if (bsMatch3D == null) {

      // getting a BitSet or BitSet[] from a set of atoms or a pattern.

      asAtoms = (smiles == null);
      if (asAtoms)
        b = viewer.getSmilesMatcher().getSubstructureSetArray(pattern,
            viewer.modelSet.atoms, viewer.getAtomCount(), bsSelected,
            null, isSmarts, false);
      else
        b = viewer.getSmilesMatcher().find(pattern, smiles, isSmarts, false);

      if (b == null) {
        showStringPrint(viewer.getSmilesMatcher().getLastException(), false);
        if (!asAtoms && !isSmarts)
          return Integer.valueOf(-1);
        return "?";
      }
    } else {

      // getting a correlation

      JmolList<BS> vReturn = new  JmolList<BS>();
      float stddev = getSmilesCorrelation(bsMatch3D, bsSelected, pattern, null,
          null, null, vReturn, isSmarts, false);
      if (Float.isNaN(stddev)) {
        if (asOneBitset)
          return new BS();
        return new String[] {};
      }
      showString("RMSD " + stddev + " Angstroms");
      b = vReturn.toArray(new BS[vReturn.size()]);
    }
    if (asOneBitset) {
      // sum total of all now, not just first
      BS bs = new BS();
      for (int j = 0; j < b.length; j++)
        bs.or(b[j]);
      if (asAtoms)
        return bs;
      if (!isSmarts)
        return Integer.valueOf(bs.cardinality());
      int[] iarray = new int[bs.cardinality()];
      int pt = 0;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        iarray[pt++] = i + 1;
      return iarray;
    }
    String[] matches = new String[b.length];
    for (int j = 0; j < b.length; j++)
      matches[j] = Escape.eB(b[j], asAtoms);
    return matches;
  }

  private void connect(int index) throws ScriptException {

    final float[] distances = new float[2];
    BS[] atomSets = new BS[2];
    atomSets[0] = atomSets[1] = viewer.getSelectionSet(false);
    float radius = Float.NaN;
    int color = Integer.MIN_VALUE;
    int distanceCount = 0;
    int bondOrder = JmolEdge.BOND_ORDER_NULL;
    int bo;
    int operation = T.modifyorcreate;
    boolean isDelete = false;
    boolean haveType = false;
    boolean haveOperation = false;
    String translucency = null;
    float translucentLevel = Float.MAX_VALUE;
    boolean isColorOrRadius = false;
    int nAtomSets = 0;
    int nDistances = 0;
    BS bsBonds = new BS();
    boolean isBonds = false;
    int expression2 = 0;
    int ptColor = 0;
    float energy = 0;
    boolean addGroup = false;
    /*
     * connect [<=2 distance parameters] [<=2 atom sets] [<=1 bond type] [<=1
     * operation]
     */

    if (slen == 1) {
      if (!chk)
        viewer.rebondState(isStateScript); 
      return;
    }

    for (int i = index; i < slen; ++i) {
      switch (getToken(i).tok) {
      case T.on:
      case T.off:
        checkLength(2);
        if (!chk)
          viewer.rebondState(isStateScript);
        return;
      case T.integer:
      case T.decimal:
        if (nAtomSets > 0) {
          if (haveType || isColorOrRadius)
            error(ERROR_invalidParameterOrder);
          bo = JmolEdge.getBondOrderFromFloat(floatParameter(i));
          if (bo == JmolEdge.BOND_ORDER_NULL)
            error(ERROR_invalidArgument);
          bondOrder = bo;
          haveType = true;
          break;
        }
        if (++nDistances > 2)
          error(ERROR_badArgumentCount);
        float dist = floatParameter(i);
        if (tokAt(i + 1) == T.percent) {
          dist = -dist / 100f;
          i++;
        }
        distances[distanceCount++] = dist;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (nAtomSets > 2 || isBonds && nAtomSets > 0)
          error(ERROR_badArgumentCount);
        if (haveType || isColorOrRadius)
          error(ERROR_invalidParameterOrder);
        atomSets[nAtomSets++] = atomExpressionAt(i);
        isBonds = isBondSet;
        if (nAtomSets == 2) {
          int pt = iToken;
          for (int j = i; j < pt; j++)
            if (tokAt(j) == T.identifier
                && parameterAsString(j).equals("_1")) {
              expression2 = i;
              break;
            }
          iToken = pt;
        }
        i = iToken;
        break;
      case T.group:
        addGroup = true;
        break;
      case T.color:
        int tok = tokAt(i + 1);
        if (tok != T.translucent && tok != T.opaque)
          ptColor = i + 1;
        continue;
      case T.translucent:
      case T.opaque:
        if (translucency != null)
          error(ERROR_invalidArgument);
        isColorOrRadius = true;
        translucency = parameterAsString(i);
        if (theTok == T.translucent && isFloatParameter(i + 1))
          translucentLevel = getTranslucentLevel(++i);
        ptColor = i + 1;
        break;
      case T.pdb:
        boolean isAuto = (tokAt(2) == T.auto);
        checkLength(isAuto ? 3 : 2);
        if (!chk)
          viewer.setPdbConectBonding(isAuto, isStateScript);
        return;
      case T.adjust:
      case T.auto:
      case T.create:
      case T.modify:
      case T.modifyorcreate:
        // must be an operation and must be last argument
        haveOperation = true;
        if (++i != slen)
          error(ERROR_invalidParameterOrder);
        operation = theTok;
        if (theTok == T.auto
            && !(bondOrder == JmolEdge.BOND_ORDER_NULL
                || bondOrder == JmolEdge.BOND_H_REGULAR || bondOrder == JmolEdge.BOND_AROMATIC))
          error(ERROR_invalidArgument);
        break;
      case T.struts:
        if (!isColorOrRadius) {
          color = 0xFFFFFF;
          translucency = "translucent";
          translucentLevel = 0.5f;
          radius = viewer.getStrutDefaultRadius();
          isColorOrRadius = true;
        }
        if (!haveOperation)
          operation = T.modifyorcreate;
        haveOperation = true;
        //$FALL-THROUGH$
      case T.identifier:
      case T.aromatic:
      case T.hbond:
        if (i > 0) {
          if (ptColor == i)
            break;
          // I know -- should have required the COLOR keyword
          if (isColorParam(i)) {
            ptColor = -i;
            break;
          }
        }
        String cmd = parameterAsString(i);
        if ((bo = getBondOrderFromString(cmd)) == JmolEdge.BOND_ORDER_NULL) {
          error(ERROR_invalidArgument);
        }
        // must be bond type
        if (haveType)
          error(ERROR_incompatibleArguments);
        haveType = true;
        switch (bo) {
        case JmolEdge.BOND_PARTIAL01:
          switch (tokAt(i + 1)) {
          case T.decimal:
            bo = getPartialBondOrderFromFloatEncodedInt(st[++i].intValue);
            break;
          case T.integer:
            bo = (short) intParameter(++i);
            break;
          }
          break;
        case JmolEdge.BOND_H_REGULAR:
          if (tokAt(i + 1) == T.integer) {
            bo = (short) (intParameter(++i) << JmolEdge.BOND_HBOND_SHIFT);
            energy = floatParameter(++i);
          }
          break;
        }
        bondOrder = bo;
        break;
      case T.radius:
        radius = floatParameter(++i);
        isColorOrRadius = true;
        break;
      case T.none:
      case T.delete:
        if (++i != slen)
          error(ERROR_invalidParameterOrder);
        operation = T.delete;
        // if (isColorOrRadius) / for struts automatic color
        // error(ERROR_invalidArgument);
        isDelete = true;
        isColorOrRadius = false;
        break;
      default:
        ptColor = i;
        break;
      }
      // now check for color -- -i means we've already checked
      if (i > 0) {
        if (ptColor == -i || ptColor == i && isColorParam(i)) {
          color = getArgbParam(i);
          i = iToken;
          isColorOrRadius = true;
        } else if (ptColor == i) {
          error(ERROR_invalidArgument);
        }
      }
    }
    if (chk)
      return;
    if (distanceCount < 2) {
      if (distanceCount == 0)
        distances[0] = JC.DEFAULT_MAX_CONNECT_DISTANCE;
      distances[1] = distances[0];
      distances[0] = JC.DEFAULT_MIN_CONNECT_DISTANCE;
    }
    if (translucency != null || !Float.isNaN(radius)
        || color != Integer.MIN_VALUE) {
      if (!haveType)
        bondOrder = JmolEdge.BOND_ORDER_ANY;
      if (!haveOperation)
        operation = T.modify;
    }
    int nNew = 0;
    int nModified = 0;
    int[] result;
    if (expression2 > 0) {
      BS bs = new BS();
      definedAtomSets.put("_1", bs);
      BS bs0 = atomSets[0];
      for (int atom1 = bs0.nextSetBit(0); atom1 >= 0; atom1 = bs0
          .nextSetBit(atom1 + 1)) {
        bs.set(atom1);
        result = viewer.makeConnections(distances[0], distances[1], bondOrder,
            operation, bs, atomExpressionAt(expression2), bsBonds, isBonds,
            false, 0);
        nNew += Math.abs(result[0]);
        nModified += result[1];
        bs.clear(atom1);
      }
    } else {
      result = viewer.makeConnections(distances[0], distances[1], bondOrder,
          operation, atomSets[0], atomSets[1], bsBonds, isBonds, addGroup,
          energy);
      nNew += Math.abs(result[0]);
      nModified += result[1];
    }
    if (isDelete) {
      if (!(tQuiet || scriptLevel > scriptReportingLevel))
        scriptStatusOrBuffer(GT._("{0} connections deleted", nModified));
      return;
    }
    if (isColorOrRadius) {
      viewer.selectBonds(bsBonds);
      if (!Float.isNaN(radius))
        setShapeSizeBs(JC.SHAPE_STICKS, Math.round(radius * 2000), null);
      if (color != Integer.MIN_VALUE)
        setShapePropertyBs(JC.SHAPE_STICKS, "color", Integer
            .valueOf(color), bsBonds);
      if (translucency != null) {
        if (translucentLevel == Float.MAX_VALUE)
          translucentLevel = viewer.getDefaultTranslucent();
        setShapeProperty(JC.SHAPE_STICKS, "translucentLevel", Float
            .valueOf(translucentLevel));
        setShapePropertyBs(JC.SHAPE_STICKS, "translucency",
            translucency, bsBonds);
      }
      viewer.selectBonds(null);
    }
    if (!(tQuiet || scriptLevel > scriptReportingLevel))
      scriptStatusOrBuffer(GT._("{0} new bonds; {1} modified", new Object[] {
          Integer.valueOf(nNew), Integer.valueOf(nModified) }));
  }

  private float getTranslucentLevel(int i) throws ScriptException {
    float f = floatParameter(i);
    return (theTok == T.integer && f > 0 && f < 9 ? f + 1 : f);
  }

  private void getProperty() throws ScriptException {
    if (chk)
      return;
    String retValue = "";
    String property = optParameterAsString(1);
    String name = property;
    if (name.indexOf(".") >= 0)
      name = name.substring(0, name.indexOf("."));
    if (name.indexOf("[") >= 0)
      name = name.substring(0, name.indexOf("["));
    int propertyID = viewer.getPropertyNumber(name);
    String param = optParameterAsString(2);
    int tok = tokAt(2);
    BS bs = (tok == T.expressionBegin || tok == T.bitset ? atomExpressionAt(2)
        : null);
    if (property.length() > 0 && propertyID < 0) {
      // no such property
      property = ""; // produces a list from Property Manager
      param = "";
    } else if (propertyID >= 0 && slen < 3) {
      param = viewer.getDefaultPropertyParam(propertyID);
      if (param.equals("(visible)")) {
        viewer.setModelVisibility();
        bs = viewer.getVisibleSet();
      }
    } else if (propertyID == viewer.getPropertyNumber("fileContents")) {
      for (int i = 3; i < slen; i++)
        param += parameterAsString(i);
    }
    retValue = (String) viewer.getProperty("readable", property,
        (bs == null ? (Object) param : (Object) bs));
    showString(retValue);
  }

  private void background(int i) throws ScriptException {
    getToken(i);
    int argb;
    if (theTok == T.image) {
      // background IMAGE "xxxx.jpg"
      String file = parameterAsString(checkLast(++i));
      if (!chk && !file.equalsIgnoreCase("none") && file.length() > 0)
        viewer.loadImage(file, null);
      return;
    }
    if (isColorParam(i) || theTok == T.none) {
      argb = getArgbParamLast(i, true);
      if (chk)
        return;
      setObjectArgb("background", argb);
      viewer.setBackgroundImage(null, null);
      return;
    }
    int iShape = getShapeType(theTok);
    colorShape(iShape, i + 1, true);
  }

  private void center(int i) throws ScriptException {
    // from center (atom) or from zoomTo under conditions of not
    // windowCentered()
    if (slen == 1) {
      viewer.setNewRotationCenter(null);
      return;
    }
    P3 center = centerParameter(i);
    if (center == null)
      error(ERROR_invalidArgument);
    if (!chk)
      viewer.setNewRotationCenter(center);
  }

  private String setObjectProperty() throws ScriptException {
    String s = "";
    String id = getShapeNameParameter(2);
    Object[] data = new Object[] { id, null };
    if (chk)
      return "";
    int iTok = iToken;
    int tokCommand = tokAt(0);
    boolean isWild = TextFormat.isWild(id);
    for (int iShape = JC.SHAPE_DIPOLES;;) {
      if (iShape != JC.SHAPE_MO
          && getShapePropertyData(iShape, "checkID", data)) {
        setShapeProperty(iShape, "thisID", id);
        switch (tokCommand) {
        case T.delete:
          setShapeProperty(iShape, "delete", null);
          break;
        case T.hide:
        case T.display:
          setShapeProperty(iShape, "hidden",
              tokCommand == T.display ? Boolean.FALSE : Boolean.TRUE);
          break;
        case T.show:
          //if (iShape == JmolConstants.SHAPE_ISOSURFACE && !isWild)
            //return getIsosurfaceJvxl(false, JmolConstants.SHAPE_ISOSURFACE);
          //else if (iShape == JmolConstants.SHAPE_PMESH && !isWild)
            //return getIsosurfaceJvxl(true, JmolConstants.SHAPE_PMESH);
          s += (String) getShapeProperty(iShape, "command") + "\n";
          break;
        case T.color:
          colorShape(iShape, iTok + 1, false);
          break;
        }
        if (!isWild)
          break;
      }
      if (iShape == JC.SHAPE_DIPOLES)
        iShape = JC.SHAPE_MAX_HAS_ID;
      if (--iShape < JC.SHAPE_MIN_HAS_ID)
        break;
    }
    return s;
  }

  private void color() throws ScriptException {
    int i = 1;
    if (isColorParam(1)) {
      theTok = T.atoms;
    } else {
      int argb = 0;
      i = 2;
      int tok = getToken(1).tok;
      switch (tok) {
      case T.dollarsign:
        setObjectProperty();
        return;
      case T.altloc:
      case T.amino:
      case T.chain:
      case T.fixedtemp:
      case T.formalcharge:
      case T.group:
      case T.hydrophobic:
      case T.insertion:
      case T.jmol:
      case T.molecule:
      case T.monomer:
      case T.none:
      case T.opaque:
      case T.partialcharge:
      case T.polymer:
      case T.property:
      case T.rasmol:
      case T.spacefill:
      case T.shapely:
      case T.straightness:
      case T.structure:
      case T.surfacedistance:
      case T.temperature:
      case T.translucent:
      case T.user:
      case T.vanderwaals:
        theTok = T.atoms;
        i = 1;
        break;
      case T.string:
        i = 1;
        String strColor = stringParameter(i++);
        if (isArrayParameter(i)) {
          strColor = strColor += "=" + SV.sValue(SV.getVariableAS(stringParameterSet(i))).replace('\n',' ');
          i = iToken + 1;
        }
        boolean isTranslucent = (tokAt(i) == T.translucent);
        if (!chk)
          viewer.setPropertyColorScheme(strColor, isTranslucent, true);
        if (isTranslucent)
          ++i;
        if (tokAt(i) == T.range || tokAt(i) == T.absolute) {
          float min = floatParameter(++i);
          float max = floatParameter(++i);
          if (!chk)
            viewer.setCurrentColorRange(min, max);
        }
        return;
      case T.range:
      case T.absolute:
        float min = floatParameter(2);
        float max = floatParameter(checkLast(3));
        if (!chk)
          viewer.setCurrentColorRange(min, max);
        return;
      case T.background:
        argb = getArgbParamLast(2, true);
        if (!chk)
          setObjectArgb("background", argb);
        return;
      case T.bitset:
      case T.expressionBegin:
        i = -1;
        theTok = T.atoms;
        break;
      case T.rubberband:
        argb = getArgbParamLast(2, false);
        if (!chk)
          viewer.setRubberbandArgb(argb);
        return;
      case T.highlight:
      case T.selectionhalos:
        i = 2;
        if (tokAt(2) == T.opaque)
          i++;
        argb = getArgbParamLast(i, true);
        if (chk)
          return;
        sm.loadShape(JC.SHAPE_HALOS);
        setShapeProperty(JC.SHAPE_HALOS,
            (tok == T.selectionhalos ? "argbSelection" : "argbHighlight"),
            Integer.valueOf(argb));
        return;
      case T.axes:
      case T.boundbox:
      case T.unitcell:
      case T.identifier:
      case T.hydrogen:
        // color element
        String str = parameterAsString(1);
        if (checkToken(2)) {
          switch (getToken(2).tok) {
          case T.rasmol:
            argb = T.rasmol;
            break;
          case T.none:
          case T.jmol:
            argb = T.jmol;
            break;
          default:
            argb = getArgbParam(2);
          }
        }
        if (argb == 0)
          error(ERROR_colorOrPaletteRequired);
        checkLast(iToken);
        if (str.equalsIgnoreCase("axes")
            || StateManager.getObjectIdFromName(str) >= 0) {
          setObjectArgb(str, argb);
          return;
        }
        if (changeElementColor(str, argb))
          return;
        error(ERROR_invalidArgument);
        break;
      case T.isosurface:
      case T.contact:
        setShapeProperty(JC.shapeTokenIndex(tok), "thisID",
            MeshCollection.PREVIOUS_MESH_ID);
        break;
      }
    }
    colorShape(getShapeType(theTok), i, false);
  }

  private boolean changeElementColor(String str, int argb) {
    for (int i = Elements.elementNumberMax; --i >= 0;) {
      if (str.equalsIgnoreCase(Elements.elementNameFromNumber(i))) {
        if (!chk)
          viewer.setElementArgb(i, argb);
        return true;
      }
    }
    for (int i = Elements.altElementMax; --i >= 0;) {
      if (str.equalsIgnoreCase(Elements.altElementNameFromIndex(i))) {
        if (!chk)
          viewer.setElementArgb(Elements.altElementNumberFromIndex(i), argb);
        return true;
      }
    }
    if (str.charAt(0) != '_')
      return false;
    for (int i = Elements.elementNumberMax; --i >= 0;) {
      if (str.equalsIgnoreCase("_" + Elements.elementSymbolFromNumber(i))) {
        if (!chk)
          viewer.setElementArgb(i, argb);
        return true;
      }
    }
    for (int i = Elements.altElementMax; --i >= Elements.firstIsotope;) {
      if (str.equalsIgnoreCase("_" + Elements.altElementSymbolFromIndex(i))) {
        if (!chk)
          viewer.setElementArgb(Elements.altElementNumberFromIndex(i), argb);
        return true;
      }
      if (str.equalsIgnoreCase("_" + Elements.altIsotopeSymbolFromIndex(i))) {
        if (!chk)
          viewer.setElementArgb(Elements.altElementNumberFromIndex(i), argb);
        return true;
      }
    }
    return false;
  }

  private void colorShape(int shapeType, int index, boolean isBackground)
      throws ScriptException {
    String translucency = null;
    Object colorvalue = null;
    Object colorvalue1 = null;
    BS bs = null;
    String prefix = "";
    boolean isColor = false;
    boolean isIsosurface = (shapeType == JC.SHAPE_ISOSURFACE || shapeType == JC.SHAPE_CONTACT);
    int typeMask = 0;
    boolean doClearBondSet = false;
    float translucentLevel = Float.MAX_VALUE;
    if (index < 0) {
      bs = atomExpressionAt(-index);
      index = iToken + 1;
      if (isBondSet) {
        doClearBondSet = true;
        shapeType = JC.SHAPE_STICKS;
      }
    }
    if (isBackground)
      getToken(index);
    else if ((isBackground = (getToken(index).tok == T.background)) == true)
      getToken(++index);
    if (isBackground)
      prefix = "bg";
    else if (isIsosurface) {
      switch (theTok) {
      case T.mesh:
        getToken(++index);
        prefix = "mesh";
        break;
      case T.phase:
        int argb = getArgbParamOrNone(++index, false);
        colorvalue1 = (argb == 0 ? null : Integer.valueOf(argb));
        getToken(index = iToken + 1);
        break;
      case T.bitset:
      case T.expressionBegin:
        if (theToken.value instanceof BondSet) {
          bs = (BondSet) theToken.value;
          prefix = "vertex";
        } else {
          bs = atomExpressionAt(index);
          // don't allow isosurface partial translucency (yet)
          prefix = "atom";
        }
        translucentLevel = Float.MIN_VALUE;
        getToken(index = iToken + 1);
        break;
      }
    }
    if (!chk && shapeType == JC.SHAPE_MO && !mo(true))
      return;
    boolean isTranslucent = (theTok == T.translucent);
    if (isTranslucent || theTok == T.opaque) {
      if (translucentLevel == Float.MIN_VALUE)
        error(ERROR_invalidArgument);
      translucency = parameterAsString(index++);
      if (isTranslucent && isFloatParameter(index))
        translucentLevel = getTranslucentLevel(index++);
    }
    int tok = 0;
    if (index < slen && tokAt(index) != T.on
        && tokAt(index) != T.off) {
      isColor = true;
      tok = getToken(index).tok;
      if ((!isIsosurface || tokAt(index + 1) != T.to)
          && isColorParam(index)) {
        int argb = getArgbParamOrNone(index, false);
        colorvalue = (argb == 0 ? null : Integer.valueOf(argb));
        if (translucency == null && tokAt(index = iToken + 1) != T.nada) {
          getToken(index);
          isTranslucent = (theTok == T.translucent);
          if (isTranslucent || theTok == T.opaque) {
            translucency = parameterAsString(index);
            if (isTranslucent && isFloatParameter(index + 1))
              translucentLevel = getTranslucentLevel(++index);
          } else if (isColorParam(index)){
            argb = getArgbParamOrNone(index, false);
            colorvalue1 = (argb == 0 ? null : Integer.valueOf(argb));            
          }
          // checkLength(index + 1);
          // iToken = index;
        }
      } else if (shapeType == JC.SHAPE_LCAOCARTOON) {
        iToken--; // back up one
      } else {
        // must not be a color, but rather a color SCHEME
        // this could be a problem for properties, which can't be
        // checked later -- they must be turned into a color NOW.

        // "cpk" value would be "spacefill"
        String name = parameterAsString(index).toLowerCase();
        boolean isByElement = (name.indexOf(ColorEncoder.BYELEMENT_PREFIX) == 0);
        boolean isColorIndex = (isByElement || name
            .indexOf(ColorEncoder.BYRESIDUE_PREFIX) == 0);
        EnumPalette pal = (isColorIndex || isIsosurface ? EnumPalette.PROPERTY
            : tok == T.spacefill ? EnumPalette.CPK
                : EnumPalette.getPalette(name));
        // color atoms "cpkScheme"
        if (pal == EnumPalette.UNKNOWN
            || (pal == EnumPalette.TYPE || pal == EnumPalette.ENERGY)
            && shapeType != JC.SHAPE_HSTICKS)
          error(ERROR_invalidArgument);
        Object data = null;
        BS bsSelected = (pal != EnumPalette.PROPERTY
            && pal != EnumPalette.VARIABLE
            || !viewer.isRangeSelected() ? null : viewer.getSelectionSet(false));
        if (pal == EnumPalette.PROPERTY) {
          if (isColorIndex) {
            if (!chk) {
              data = getBitsetPropertyFloat(
                  bsSelected,
                  (isByElement ? T.elemno : T.groupid) | T.allfloat,
                  Float.NaN, Float.NaN);
            }
          } else {
            if (!isColorIndex && !isIsosurface)
              index++;
            if (name.equals("property")
                && T.tokAttr((tok = getToken(index).tok),
                    T.atomproperty)
                && !T.tokAttr(tok, T.strproperty)) {
              if (!chk) {
                data = getBitsetPropertyFloat(bsSelected, getToken(index++).tok
                    | T.allfloat, Float.NaN, Float.NaN);
              }
            }
          }
        } else if (pal == EnumPalette.VARIABLE) {
          index++;
          name = parameterAsString(index++);
          data = new float[viewer.getAtomCount()];
          Parser.parseStringInfestedFloatArray(""
              + getParameter(name, T.string), null, (float[]) data);
          pal = EnumPalette.PROPERTY;
        }
        if (pal == EnumPalette.PROPERTY) {
          String scheme = null;
          if (tokAt(index) == T.string) {
            scheme = parameterAsString(index++).toLowerCase();
            if (isArrayParameter(index)) {
              scheme += "=" + SV.sValue(SV.getVariableAS(stringParameterSet(index))).replace('\n',' ');
              index = iToken + 1;
            }
          } else if (isIsosurface && isColorParam(index)) {
            scheme = getColorRange(index);
            index = iToken + 1;
          }
          if (scheme != null && !isIsosurface) {
            setStringProperty("propertyColorScheme", (isTranslucent
                && translucentLevel == Float.MAX_VALUE ? "translucent " : "")
                + scheme);
            isColorIndex = (scheme.indexOf(ColorEncoder.BYELEMENT_PREFIX) == 0 || scheme
                .indexOf(ColorEncoder.BYRESIDUE_PREFIX) == 0);
          }
          float min = 0;
          float max = Float.MAX_VALUE;
          if (!isColorIndex
              && (tokAt(index) == T.absolute || tokAt(index) == T.range)) {
            min = floatParameter(index + 1);
            max = floatParameter(index + 2);
            index += 3;
            if (min == max && isIsosurface) {
              float[] range = (float[]) getShapeProperty(shapeType, "dataRange");
              if (range != null) {
                min = range[0];
                max = range[1];
              }
            } else if (min == max) {
              max = Float.MAX_VALUE;
            }
          }
          if (!chk) {
            if (isIsosurface) {
            } else if (data == null) {
              viewer.setCurrentColorRange(name);
            } else {
              viewer.setCurrentColorRangeData((float[]) data, bsSelected);
            }
            if (isIsosurface) {
              checkLength(index);
              isColor = false;
              ColorEncoder ce = viewer.getColorEncoder(scheme);
              if (ce == null)
                return;
              ce.isTranslucent = (isTranslucent && translucentLevel == Float.MAX_VALUE);
              ce.setRange(min, max, min > max);
              if (max == Float.MAX_VALUE)
                ce.hi = max;
              setShapeProperty(shapeType, "remapColor", ce);
              showString(getIsosurfaceDataRange(shapeType, ""));
              if (translucentLevel == Float.MAX_VALUE)
                return;
            } else if (max != Float.MAX_VALUE) {
              viewer.setCurrentColorRange(min, max);
            }
          }
        } else {
          index++;
        }
        checkLength(index);
        colorvalue = pal;
      }
    }
    if (chk || shapeType < 0)
      return;
    switch (shapeType) {
    case JC.SHAPE_STRUTS:
      typeMask = JmolEdge.BOND_STRUT;
      break;
    case JC.SHAPE_HSTICKS:
      typeMask = JmolEdge.BOND_HYDROGEN_MASK;
      break;
    case JC.SHAPE_SSSTICKS:
      typeMask = JmolEdge.BOND_SULFUR_MASK;
      break;
    case JC.SHAPE_STICKS:
      typeMask = JmolEdge.BOND_COVALENT_MASK;
      break;
    default:
      typeMask = 0;
    }
    if (typeMask == 0) {
      sm.loadShape(shapeType);
      if (shapeType == JC.SHAPE_LABELS)
        setShapeProperty(JC.SHAPE_LABELS, "setDefaults", viewer
            .getNoneSelected());
    } else {
      if (bs != null) {
        viewer.selectBonds(bs);
        bs = null;
      }
      shapeType = JC.SHAPE_STICKS;
      setShapeProperty(shapeType, "type", Integer.valueOf(typeMask));
    }
    if (isColor) {
      // ok, the following options require precalculation.
      // the state must not save them as paletteIDs, only as pure
      // color values.
      switch (tok) {
      case T.surfacedistance:
      case T.straightness:
        viewer.autoCalculate(tok);
        break;
      case T.temperature:
        if (viewer.isRangeSelected())
          viewer.clearBfactorRange();
        break;
      case T.group:
        viewer.calcSelectedGroupsCount();
        break;
      case T.polymer:
      case T.monomer:
        viewer.calcSelectedMonomersCount();
        break;
      case T.molecule:
        viewer.calcSelectedMoleculesCount();
        break;
      }
      if (colorvalue1 != null 
          && (isIsosurface || shapeType == JC.SHAPE_CARTOON || shapeType == JC.SHAPE_RIBBONS))
        setShapeProperty(shapeType, "colorPhase", new Object[] { colorvalue1, colorvalue });
      else if (bs == null)
        setShapeProperty(shapeType, prefix + "color", colorvalue);
      else
        setShapePropertyBs(shapeType, prefix + "color", colorvalue, bs);
    }
    if (translucency != null)
      setShapeTranslucency(shapeType, prefix, translucency, translucentLevel,
          bs);
    if (typeMask != 0)
      setShapeProperty(JC.SHAPE_STICKS, "type", Integer
          .valueOf(JmolEdge.BOND_COVALENT_MASK));
    if (doClearBondSet)
      viewer.selectBonds(null);
    if (shapeType == JC.SHAPE_BALLS)
      viewer.checkInheritedShapes();
  }

  private void setShapeTranslucency(int shapeType, String prefix,
                                    String translucency,
                                    float translucentLevel, BS bs) {
    if (translucentLevel == Float.MAX_VALUE)
      translucentLevel = viewer.getDefaultTranslucent();
    setShapeProperty(shapeType, "translucentLevel", Float
        .valueOf(translucentLevel));
    if (prefix == null)
      return;
    if (bs == null)
      setShapeProperty(shapeType, prefix + "translucency", translucency);
    else if (!chk)
      setShapePropertyBs(shapeType, prefix + "translucency", translucency, bs);
  }

  private void cd() throws ScriptException {
    if (chk)
      return;
    String dir = (slen == 1 ? null : parameterAsString(1));
    showString(viewer.cd(dir));
  }

  private Object[] data;

  private void mapProperty() throws ScriptException {
    // map {1.1}.straightness  {2.1}.property_x resno
    BS bsFrom, bsTo;
    String property1, property2, mapKey;
    int tokProp1 = 0;
    int tokProp2 = 0;
    int tokKey = 0;
    while (true) {
      if (tokAt(1) == T.selected) {
        bsFrom = viewer.getSelectionSet(false);
        bsTo = atomExpressionAt(2);
        property1 = property2 = "selected";
      } else {
        bsFrom = atomExpressionAt(1);
        if (tokAt(++iToken) != T.per
            || !T.tokAttr(tokProp1 = tokAt(++iToken), T.atomproperty))
          break;
        property1 = parameterAsString(iToken);
        bsTo = atomExpressionAt(++iToken);
        if (tokAt(++iToken) != T.per
            || !T.tokAttr(tokProp2 = tokAt(++iToken), T.settable))
          break;
        property2 = parameterAsString(iToken);
      }
      if (T.tokAttr(tokKey = tokAt(iToken + 1), T.atomproperty))
        mapKey = parameterAsString(++iToken);
      else
        mapKey = T.nameOf(tokKey = T.atomno);
      checkLast(iToken);
      if (chk)
        return;
      BS bsOut = null;
      showString("mapping " + property1.toUpperCase() + " for "
          + bsFrom.cardinality() + " atoms to " + property2.toUpperCase()
          + " for " + bsTo.cardinality() + " atoms using "
          + mapKey.toUpperCase());
      if (T.tokAttrOr(tokProp1, T.intproperty, T.floatproperty)
          && T.tokAttrOr(tokProp2, T.intproperty, T.floatproperty)
          && T.tokAttrOr(tokKey, T.intproperty, T.floatproperty)) {
        float[] data1 = getBitsetPropertyFloat(bsFrom, tokProp1
            | T.selectedfloat, Float.NaN, Float.NaN);
        float[] data2 = getBitsetPropertyFloat(bsFrom, tokKey
            | T.selectedfloat, Float.NaN, Float.NaN);
        float[] data3 = getBitsetPropertyFloat(bsTo, tokKey
            | T.selectedfloat, Float.NaN, Float.NaN);
        boolean isProperty = (tokProp2 == T.property);
        float[] dataOut = new float[isProperty ? viewer.getAtomCount()
            : data3.length];
        bsOut = new BS();
        if (data1.length == data2.length) {
          Map<Float, Float> ht = new Hashtable<Float, Float>();
          for (int i = 0; i < data1.length; i++) {
            ht.put(Float.valueOf(data2[i]), Float.valueOf(data1[i]));
          }
          int pt = -1;
          int nOut = 0;
          for (int i = 0; i < data3.length; i++) {
            pt = bsTo.nextSetBit(pt + 1);
            Float F = ht.get(Float.valueOf(data3[i]));
            if (F == null)
              continue;
            bsOut.set(pt);
            dataOut[(isProperty ? pt : nOut)] = F.floatValue();
            nOut++;
          }
          if (isProperty)
            viewer.setData(property2,
                new Object[] { property2, dataOut, bsOut, Integer.valueOf(0) }, viewer
                    .getAtomCount(), 0, 0, Integer.MAX_VALUE, 0);
          else
            viewer.setAtomProperty(bsOut, tokProp2, 0, 0, null, dataOut, null);
        }
      }
      if (bsOut == null) {
        String format = "{" + mapKey + "=%[" + mapKey + "]}." + property2
            + " = %[" + property1 + "]";
        String[] data = (String[]) getBitsetIdent(bsFrom, format, null, false,
            Integer.MAX_VALUE, false);
        SB sb = new SB();
        for (int i = 0; i < data.length; i++)
          if (data[i].indexOf("null") < 0)
            sb.append(data[i]).appendC('\n');
        if (Logger.debugging)
          Logger.info(sb.toString());
        BS bsSubset = BSUtil.copy(viewer.getSelectionSubset());
        viewer.setSelectionSubset(bsTo);
        try {
          runScript(sb.toString());
        } catch (Exception e) {
          viewer.setSelectionSubset(bsSubset);
          errorStr(-1, "Error: " + e.toString());
        } catch (Error er) {
          viewer.setSelectionSubset(bsSubset);
          errorStr(-1, "Error: " + er.toString());
        }
        viewer.setSelectionSubset(bsSubset);
      }
      showString("DONE");
      return;
    }
    error(ERROR_invalidArgument);
  }

  private void data() throws ScriptException {
    String dataString = null;
    String dataLabel = null;
    boolean isOneValue = false;
    int i;
    switch (iToken = slen) {
    case 5:
      // parameters 3 and 4 are just for the ride: [end] and ["key"]
      dataString = parameterAsString(2);
      //$FALL-THROUGH$
    case 4:
    case 2:
      dataLabel = parameterAsString(1);
      if (dataLabel.equalsIgnoreCase("clear")) {
        if (!chk)
          viewer.setData(null, null, 0, 0, 0, 0, 0);
        return;
      }
      if ((i = dataLabel.indexOf("@")) >= 0) {
        dataString = ""
            + getParameter(dataLabel.substring(i + 1), T.string);
        dataLabel = dataLabel.substring(0, i).trim();
      } else if (dataString == null && (i = dataLabel.indexOf(" ")) >= 0) {
        dataString = dataLabel.substring(i + 1).trim();
        dataLabel = dataLabel.substring(0, i).trim();
        isOneValue = true;
      }
      break;
    default:
      error(ERROR_badArgumentCount);
    }
    String dataType = dataLabel + " ";
    dataType = dataType.substring(0, dataType.indexOf(" ")).toLowerCase();
    if (dataType.equals("model") || dataType.equals("append")) {
      load();
      return;
    }
    if (chk)
      return;
    boolean isDefault = (dataLabel.toLowerCase().indexOf("(default)") >= 0);
    data = new Object[4];
    if (dataType.equals("element_vdw")) {
      // vdw for now
      data[0] = dataType;
      data[1] = dataString.replace(';', '\n');
      int n = Elements.elementNumberMax;
      int[] eArray = new int[n + 1];
      for (int ie = 1; ie <= n; ie++)
        eArray[ie] = ie;
      data[2] = eArray;
      data[3] = Integer.valueOf(0);
      viewer.setData("element_vdw", data, n, 0, 0, 0, 0);
      return;
    }
    if (dataType.equals("connect_atoms")) {
      viewer.connect(Parser.parseFloatArray2d(dataString));
      return;
    }
    if (dataType.indexOf("ligand_") == 0) {
      // ligand structure for pdbAddHydrogen
      viewer.setLigandModel(dataLabel.substring(7), dataString.trim());
      return;
    }
    if (dataType.indexOf("data2d_") == 0) {
      // data2d_someName
      data[0] = dataLabel;
      data[1] = Parser.parseFloatArray2d(dataString);
      data[3] = Integer.valueOf(2);
      viewer.setData(dataLabel, data, 0, 0, 0, 0, 0);
      return;
    }
    if (dataType.indexOf("data3d_") == 0) {
      // data3d_someName
      data[0] = dataLabel;
      data[1] = Parser.parseFloatArray3d(dataString);
      data[3] = Integer.valueOf(3);
      viewer.setData(dataLabel, data, 0, 0, 0, 0, 0);
      return;
    }
    String[] tokens = Parser.getTokens(dataLabel);
    if (dataType.indexOf("property_") == 0
        && !(tokens.length == 2 && tokens[1].equals("set"))) {
      BS bs = viewer.getSelectionSet(false);
      data[0] = dataType;
      int atomNumberField = (isOneValue ? 0 : ((Integer) viewer
          .getParameter("propertyAtomNumberField")).intValue());
      int atomNumberFieldColumnCount = (isOneValue ? 0 : ((Integer) viewer
          .getParameter("propertyAtomNumberColumnCount")).intValue());
      int propertyField = (isOneValue ? Integer.MIN_VALUE : ((Integer) viewer
          .getParameter("propertyDataField")).intValue());
      int propertyFieldColumnCount = (isOneValue ? 0 : ((Integer) viewer
          .getParameter("propertyDataColumnCount")).intValue());
      if (!isOneValue && dataLabel.indexOf(" ") >= 0) {
        if (tokens.length == 3) {
          // DATA "property_whatever [atomField] [propertyField]"
          dataLabel = tokens[0];
          atomNumberField = Parser.parseInt(tokens[1]);
          propertyField = Parser.parseInt(tokens[2]);
        }
        if (tokens.length == 5) {
          // DATA
          // "property_whatever [atomField] [atomFieldColumnCount] [propertyField] [propertyDataColumnCount]"
          dataLabel = tokens[0];
          atomNumberField = Parser.parseInt(tokens[1]);
          atomNumberFieldColumnCount = Parser.parseInt(tokens[2]);
          propertyField = Parser.parseInt(tokens[3]);
          propertyFieldColumnCount = Parser.parseInt(tokens[4]);
        }
      }
      if (atomNumberField < 0)
        atomNumberField = 0;
      if (propertyField < 0)
        propertyField = 0;
      int atomCount = viewer.getAtomCount();
      int[] atomMap = null;
      BS bsTemp = BSUtil.newBitSet(atomCount);
      if (atomNumberField > 0) {
        atomMap = new int[atomCount + 2];
        for (int j = 0; j <= atomCount; j++)
          atomMap[j] = -1;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          int atomNo = viewer.getAtomNumber(j);
          if (atomNo > atomCount + 1 || atomNo < 0 || bsTemp.get(atomNo))
            continue;
          bsTemp.set(atomNo);
          atomMap[atomNo] = j;
        }
        data[2] = atomMap;
      } else {
        data[2] = BSUtil.copy(bs);
      }
      data[1] = dataString;
      data[3] = Integer.valueOf(0);
      viewer.setData(dataType, data, atomCount, atomNumberField,
          atomNumberFieldColumnCount, propertyField, propertyFieldColumnCount);
      return;
    }
    int userType = AtomCollection.getUserSettableType(dataType);
    if (userType >= 0) {
      // this is a known settable type or "property_xxxx"
      viewer.setAtomData(userType, dataType, dataString, isDefault);
      return;
    }
    // this is just information to be stored.
    data[0] = dataLabel;
    data[1] = dataString;
    data[3] = Integer.valueOf(0);
    viewer.setData(dataType, data, 0, 0, 0, 0, 0);
  }

  private void define() throws ScriptException {
    // note that the standard definition depends upon the
    // current state. Once defined, a setName is the set
    // of atoms that matches the definition at that time.
    // adding DYMAMIC_ to the beginning of the definition
    // allows one to create definitions that are recalculated
    // whenever they are used. When used, "DYNAMIC_" is dropped
    // so, for example:
    // define DYNAMIC_what selected and visible
    // and then
    // select what
    // will return different things at different times depending
    // upon what is selected and what is visible
    // but
    // define what selected and visible
    // will evaluate the moment it is defined and then represent
    // that set of atoms forever.

    if (slen < 3 || !(getToken(1).value instanceof String))
      error(ERROR_invalidArgument);
    String setName = ((String) getToken(1).value).toLowerCase();
    if (Parser.parseInt(setName) != Integer.MIN_VALUE)
      error(ERROR_invalidArgument);
    if (chk)
      return;
    boolean isSite = setName.startsWith("site_");
    boolean isDynamic = (setName.indexOf("dynamic_") == 0);
    if (isDynamic || isSite) {
      T[] code = new T[slen];
      for (int i = slen; --i >= 0;)
        code[i] = st[i];
      definedAtomSets
          .put("!" + (isSite ? setName : setName.substring(8)), code);
      //if (!isSite)
      //viewer.addStateScript(thisCommand, false, true); removed for 12.1.16
    } else {
      BS bs = atomExpressionAt(2);
      definedAtomSets.put(setName, bs);
      if (!chk)
        viewer.setUserVariable("@" + setName, SV.newVariable(T.bitset, bs));
    }
  }

  private void echo(int index, String id, boolean isImage) throws ScriptException {
    if (chk)
      return;
    String text = optParameterAsString(index);
    if (viewer.getEchoStateActive()) {
      if (isImage) {
        viewer.loadImage(text, id);
        return;
      } else if (text.startsWith("\1")) {
        // no reporting, just screen echo, from mouseManager key press
        text = text.substring(1);
        isImage = true;
      }
      if (text != null)
        setShapeProperty(JC.SHAPE_ECHO, "text", text);
    }
    if (!isImage && viewer.getRefreshing())
      showString(viewer.formatText(text));
  }

  private void message() throws ScriptException {
    String text = parameterAsString(checkLast(1));
    if (chk)
      return;
    String s = viewer.formatText(text);
    if (outputBuffer == null)
      viewer.showMessage(s);
    if (!s.startsWith("_"))
      scriptStatusOrBuffer(s);
  }

  private void log() throws ScriptException {
    if (slen == 1)
      error(ERROR_badArgumentCount);
    if (chk)
      return;
    String s = parameterExpressionString(1, 0);
    if (tokAt(1) == T.off)
      setStringProperty("logFile", "");
    else
      viewer.log(s);
  }

  private void label(int index) throws ScriptException {
    if (chk)
      return;
    sm.loadShape(JC.SHAPE_LABELS);
    String strLabel = null;
    switch (getToken(index).tok) {
    case T.on:
      strLabel = viewer.getStandardLabelFormat(0);
      break;
    case T.off:
      break;
    case T.hide:
    case T.display:
      setShapeProperty(JC.SHAPE_LABELS, "display",
          theTok == T.display ? Boolean.TRUE : Boolean.FALSE);
      return;
    default:
      strLabel = parameterAsString(index);
    }
    sm.setLabel(strLabel, viewer.getSelectionSet(false));
  }

  private void hover() throws ScriptException {
    if (chk)
      return;
    String strLabel = parameterAsString(1);
    if (strLabel.equalsIgnoreCase("on"))
      strLabel = "%U";
    else if (strLabel.equalsIgnoreCase("off"))
      strLabel = null;
    viewer.setHoverLabel(strLabel);
  }

  private void load() throws ScriptException {
    boolean doLoadFiles = (!chk || isCmdLine_C_Option);
    boolean isAppend = false;
    boolean isInline = false;
    boolean isSmiles = false;
    boolean isData = false;
    BS bsModels;
    int i = (tokAt(0) == T.data ? 0 : 1);
    boolean appendNew = viewer.getAppendNew();
    String filter = null;
    JmolList<Object> firstLastSteps = null;
    int modelCount0 = viewer.getModelCount()
        - (viewer.getFileName().equals("zapped") ? 1 : 0);
    int atomCount0 = viewer.getAtomCount();
    SB loadScript = new SB().append("load");
    int nFiles = 1;
    Map<String, Object> htParams = new Hashtable<String, Object>();
    // ignore optional file format
    if (isStateScript) {
      htParams.put("isStateScript", Boolean.TRUE);
      if (forceNoAddHydrogens)
        htParams.put("doNotAddHydrogens", Boolean.TRUE);
    }
    String modelName = null;
    String[] filenames = null;
    String[] tempFileInfo = null;
    String errMsg = null;
    String sOptions = "";
    int tokType = 0;
    int tok;

    // check for special parameters

    if (slen == 1) {
      i = 0;
    } else {
      modelName = parameterAsString(i);
      if (slen == 2 && !chk) {
        // spt, png, and pngj files may be
        // run using the LOAD command, but
        // we transfer them to the script command
        // if it is just LOAD "xxxx.xxx"
        // so as to avoid the ZAP in case these
        // do not contain a full state script
        if (modelName.endsWith(".spt") 
            || modelName.endsWith(".png")
            || modelName.endsWith(".pngj")) {
          script(0, modelName);
          return;
        }
      }

      // load MENU
      // load DATA "xxx" ...(data here)...END "xxx"
      // load DATA "append xxx" ...(data here)...END "append xxx"
      // load DATA "@varName"
      // load APPEND (moves pointer forward)
      // load XYZ
      // load VXYZ
      // load VIBRATION
      // load TEMPERATURE
      // load OCCUPANCY
      // load PARTIALCHARGE
      switch (tok = tokAt(i)) {
      case T.menu:
        String m = parameterAsString(checkLast(2));
        if (!chk)
          viewer.setMenu(m, true);
        return;
      case T.data:
        isData = true;
        loadScript.append(" /*data*/ data");
        String key = stringParameter(++i).toLowerCase();
        loadScript.append(" ").append(Escape.eS(key));
        isAppend = key.startsWith("append");
        String strModel = (key.indexOf("@") >= 0 ? ""
            + getParameter(key.substring(key.indexOf("@") + 1), T.string)
            : parameterAsString(++i));
        strModel = viewer.fixInlineString(strModel, viewer.getInlineChar());
        htParams.put("fileData", strModel);
        htParams.put("isData", Boolean.TRUE);
        //note: ScriptCompiler will remove an initial \n if present
        loadScript.appendC('\n');
        loadScript.append(strModel);
        if (key.indexOf("@") < 0) {
          loadScript.append(" end ").append(Escape.eS(key));
          i += 2; // skip END "key"
        }
        break;
      case T.append:
        isAppend = true;
        loadScript.append(" append");
        modelName = optParameterAsString(++i);
        tok = T.getTokFromName(modelName);
        break;
      case T.identifier:
        i++;
        loadScript.append(" " + modelName);
        tokType = (tok == T.identifier
            && Parser.isOneOf(modelName.toLowerCase(),
                JC.LOAD_ATOM_DATA_TYPES) ? T
            .getTokFromName(modelName) : T.nada);
        if (tokType != T.nada) {
          // loading just some data here
          // xyz vxyz vibration temperature occupancy partialcharge
          htParams.put("atomDataOnly", Boolean.TRUE);
          htParams.put("modelNumber", Integer.valueOf(1));
          if (tokType == T.vibration)
            tokType = T.vibxyz;
          tempFileInfo = viewer.getFileInfo();
          isAppend = true;
        }
      }
      // LOAD [[APPEND]] FILE
      // LOAD [[APPEND]] INLINE
      // LOAD [[APPEND]] SMILES
      // LOAD [[APPEND]] TRAJECTORY
      // LOAD [[APPEND]] MODEL
      // LOAD SYNC  (asynchronous -- flag for RecentFileDialog)
      // LOAD [[APPEND]] "fileNameInQuotes"

      switch (tok) {
      case T.file:
      case T.inline:
        isInline = (tok == T.inline);
        i++;
        loadScript.append(" " + modelName);
        break;
      case T.smiles:
        isSmiles = true;
        i++;
        break;
      case T.sync:
        htParams.put("async", Boolean.TRUE);
        i++;
        break;
      case T.trajectory:
      case T.model:
        i++;
        loadScript.append(" " + modelName);
        if (tok == T.trajectory)
          htParams.put("isTrajectory", Boolean.TRUE);
        if (isPoint3f(i)) {
          P3 pt = getPoint3f(i, false);
          i = iToken + 1;
          // first last stride
          htParams.put("firstLastStep", new int[] { (int) pt.x, (int) pt.y,
              (int) pt.z });
          loadScript.append(" " + Escape.eP(pt));
        } else if (tokAt(i) == T.bitset) {
          bsModels = (BS) getToken(i++).value;
          htParams.put("bsModels", bsModels);
          loadScript.append(" " + Escape.e(bsModels));
        } else {
          htParams.put("firstLastStep", new int[] { 0, -1, 1 });
        }
        break;
      case T.identifier:
        // i has been incremented; continue...
        break;
      default:
        modelName = "fileset";
      }
      if (getToken(i).tok != T.string)
        error(ERROR_filenameExpected);
    }
    // long timeBegin = System.currentTimeMillis();

    // file name is next

    // LOAD ... "xxxx"
    // LOAD ... "xxxx" AS "yyyy"

    int filePt = i;
    String localName = null;
    if (tokAt(filePt + 1) == T.as) {
      localName = stringParameter(i = i + 2);
      if (viewer.getPathForAllFiles() != "") {
        // we use the LOCAL name when reading from a local path only (in the case of JMOL files)
        localName = null;
        filePt = i;
      }
    }

    String filename = null;
    String appendedData = null;
    String appendedKey = null;
    
    if (slen == i + 1) {

      // end-of-command options:
      // LOAD SMILES "xxxx" --> load "$xxxx"
      // LOAD "['xxxxx','yyyyy','zzzzz']"

      if (i == 0 || (filename = parameterAsString(filePt)).length() == 0)
        filename = viewer.getFullPathName();
      if (filename == null) {
        zap(false);
        return;
      }
      if (isSmiles) {
        filename = "$" + filename;
      } else if (!isInline) {
        if (filename.indexOf("[]") >= 0)
          return;
        if (filename.indexOf("[") == 0) {
          filenames = Escape.unescapeStringArray(filename);
          if (filenames != null) {
            if (i == 1)
              loadScript.append(" files");
            if (loadScript.indexOf(" files") < 0)
              error(ERROR_invalidArgument);
            for (int j = 0; j < filenames.length; j++)
              loadScript.append(" /*file*/")
                  .append(Escape.eS(filenames[j]));
          }
        }
      }
    } else if (getToken(i + 1).tok == T.manifest
        // model/vibration index or list of model indices
        || theTok == T.integer || theTok == T.varray
        || theTok == T.leftsquare || theTok == T.spacebeforesquare
        // {i j k} (lattice)
        || theTok == T.leftbrace || theTok == T.point3f
        // PACKED/CENTROID, either order
        || theTok == T.packed || theTok == T.centroid
        // SUPERCELL {i j k}
        || theTok == T.supercell
        // RANGE x.x or RANGE -x.x
        || theTok == T.range
        // SPACEGROUP "nameOrNumber" 
        // or SPACEGROUP "IGNOREOPERATORS" 
        // or SPACEGROUP "" (same as current)
        || theTok == T.spacegroup
        // UNITCELL [a b c alpha beta gamma]
        // or UNITCELL [ax ay az bx by bz cx cy cz] 
        // or UNITCELL "" (same as current)
        // UNITCELL "..." or UNITCELL ""
        || theTok == T.unitcell
        // OFFSET {x y z}
        || theTok == T.offset
        // FILTER "..."
        || theTok == T.filter && tokAt(i + 3) != T.coord
        // Jmol 13.1.5 -- APPEND "data..."
        || theTok == T.append
        // don't remember what this is:
        || theTok == T.identifier && tokAt(i + 3) != T.coord

    ) {

      // more complicated command options, in order
      // (checking the tokens after "....") 

      // LOAD "" --> prevous file      

      if ((filename = parameterAsString(filePt)).length() == 0
          && (filename = viewer.getFullPathName()) == null) {
        // no previously loaded file
        zap(false);
        return;
      }
      if (filePt == i)
        i++;

      // for whatever reason, we don't allow a filename with [] in it.
      if (filename.indexOf("[]") >= 0)
        return;

      // MANIFEST "..."

      if ((tok = tokAt(i)) == T.manifest) {
        String manifest = stringParameter(++i);
        htParams.put("manifest", manifest);
        sOptions += " MANIFEST " + Escape.eS(manifest);
        tok = tokAt(++i);
      }
      // n >= 0: model number
      // n < 0: vibration number
      // [index1, index2, index3,...]

      switch (tok) {
      case T.integer:
        int n = intParameter(i);
        sOptions += " " + n;
        if (n < 0)
          htParams.put("vibrationNumber", Integer.valueOf(-n));
        else
          htParams.put("modelNumber", Integer.valueOf(n));
        tok = tokAt(++i);
        break;
      case T.varray:
      case T.leftsquare:
      case T.spacebeforesquare:
        float[] data = floatParameterSet(i, 1, Integer.MAX_VALUE);
        i = iToken;
        BS bs = new BS();
        for (int j = 0; j < data.length; j++)
          if (data[j] >= 1 && data[j] == (int) data[j])
            bs.set((int) data[j] - 1);
        htParams.put("bsModels", bs);
        int[] iArray = new int[bs.cardinality()];
        for (int pt = 0, j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
          iArray[pt++] = j + 1;
        sOptions += " " + Escape.escapeAI(iArray);
        tok = tokAt(i);
        break;
      }

      // {i j k}

      P3 lattice = null;
      if (tok == T.leftbrace || tok == T.point3f) {
        lattice = getPoint3f(i, false);
        i = iToken + 1;
        tok = tokAt(i);
      }

      // default lattice {555 555 -1} (packed) 
      // for PACKED, CENTROID, SUPERCELL, RANGE, SPACEGROUP, UNITCELL

      switch (tok) {
      case T.packed:
      case T.centroid:
      case T.supercell:
      case T.range:
      case T.spacegroup:
      case T.unitcell:
        if (lattice == null)
          lattice = P3.new3(555, 555, -1);
        iToken = i - 1;
      }
      P3 offset = null;
      if (lattice != null) {
        htParams.put("lattice", lattice);
        i = iToken + 1;
        sOptions += " {" + (int) lattice.x + " " + (int) lattice.y + " "
            + (int) lattice.z + "}";

        // {i j k} PACKED, CENTROID -- either or both; either order

        if (tokAt(i) == T.packed) {
          htParams.put("packed", Boolean.TRUE);
          sOptions += " PACKED";
          i++;
        }
        if (tokAt(i) == T.centroid) {
          htParams.put("centroid", Boolean.TRUE);
          sOptions += " CENTROID";
          i++;
          if (tokAt(i) == T.packed && !htParams.containsKey("packed")) {
            htParams.put("packed", Boolean.TRUE);
            sOptions += " PACKED";
            i++;
          }
        }

        // {i j k} ... SUPERCELL {i' j' k'}

        if (tokAt(i) == T.supercell) {
          Object supercell;
          if (isPoint3f(++i)) {
            P3 pt = getPoint3f(i, false);
            if (pt.x != (int) pt.x || pt.y != (int) pt.y || pt.z != (int) pt.z
                || pt.x < 1 || pt.y < 1 || pt.z < 1) {
              iToken = i;
              error(ERROR_invalidArgument);
            }
            supercell = pt;
            i = iToken + 1;
          } else {
            supercell = stringParameter(i++);
          }
          htParams.put("supercell", supercell);
        }

        // {i j k} ... RANGE x.y  (from full unit cell set)
        // {i j k} ... RANGE -x.y (from non-symmetry set)

        float distance = 0;
        if (tokAt(i) == T.range) {
          /*
           * # Jmol 11.3.9 introduces the capability of visualizing the close
           * contacts around a crystalline protein (or any other cyrstal
           * structure) that are to atoms that are in proteins in adjacent unit
           * cells or adjacent to the protein itself. The option RANGE x, where x
           * is a distance in angstroms, placed right after the braces containing
           * the set of unit cells to load does this. The distance, if a positive
           * number, is the maximum distance away from the closest atom in the {1
           * 1 1} set. If the distance x is a negative number, then -x is the
           * maximum distance from the {not symmetry} set. The difference is that
           * in the first case the primary unit cell (555) is first filled as
           * usual, using symmetry operators, and close contacts to this set are
           * found. In the second case, only the file-based atoms ( Jones-Faithful
           * operator x,y,z) are initially included, then close contacts to that
           * set are found. Depending upon the application, one or the other of
           * these options may be desirable.
           */
          i++;
          distance = floatParameter(i++);
          sOptions += " range " + distance;
        }
        htParams.put("symmetryRange", Float.valueOf(distance));

        // {i j k} ... SPACEGROUP "nameOrNumber"
        // {i j k} ... SPACEGROUP "IGNOREOPERATORS"
        // {i j k} ... SPACEGROUP ""

        String spacegroup = null;
        SymmetryInterface sg;
        int iGroup = Integer.MIN_VALUE;
        if (tokAt(i) == T.spacegroup) {
          ++i;
          spacegroup = TextFormat.simpleReplace(parameterAsString(i++), "''",
              "\"");
          sOptions += " spacegroup " + Escape.eS(spacegroup);
          if (spacegroup.equalsIgnoreCase("ignoreOperators")) {
            iGroup = -999;
          } else {
            if (spacegroup.length() == 0) {
              sg = viewer.getCurrentUnitCell();
              if (sg != null)
                spacegroup = sg.getSpaceGroupName();
            } else {
              if (spacegroup.indexOf(",") >= 0) // Jones Faithful
                if ((lattice.x < 9 && lattice.y < 9 && lattice.z == 0))
                  spacegroup += "#doNormalize=0";
            }
            htParams.put("spaceGroupName", spacegroup);
            iGroup = -2;
          }
        }

        // {i j k} ... UNITCELL [a b c alpha beta gamma]
        // {i j k} ... UNITCELL [ax ay az bx by bz cx cy cz] 
        // {i j k} ... UNITCELL ""  // same as current

        float[] fparams = null;
        if (tokAt(i) == T.unitcell) {
          ++i;
          if (optParameterAsString(i).length() == 0) {
            // unitcell "" -- use current unit cell
            sg = viewer.getCurrentUnitCell();
            if (sg != null) {
              fparams = sg.getUnitCellAsArray(true);
              offset = sg.getCartesianOffset();
            }
          } else {
            fparams = floatParameterSet(i, 6, 9);
          }
          if (fparams == null || fparams.length != 6 && fparams.length != 9)
            error(ERROR_invalidArgument);
          sOptions += " unitcell {";
          for (int j = 0; j < fparams.length; j++)
            sOptions += (j == 0 ? "" : " ") + fparams[j];
          sOptions += "}";
          htParams.put("unitcell", fparams);
          if (iGroup == Integer.MIN_VALUE)
            iGroup = -1;
        }
        i = iToken + 1;
        if (iGroup != Integer.MIN_VALUE)
          htParams.put("spaceGroupIndex", Integer.valueOf(iGroup));
      }

      // OFFSET {x y z} (fractional or not) (Jmol 12.1.17)

      if (offset != null)
        coordinatesAreFractional = false;
      else if (tokAt(i) == T.offset)
        offset = getPoint3f(++i, true);
      if (offset != null) {
        if (coordinatesAreFractional) {
          offset.setT(fractionalPoint);
          htParams.put("unitCellOffsetFractional",
              (coordinatesAreFractional ? Boolean.TRUE : Boolean.FALSE));
          sOptions += " offset {" + offset.x + " " + offset.y + " " + offset.z
              + "/1}";
        } else {
          sOptions += " offset " + Escape.eP(offset);
        }
        htParams.put("unitCellOffset", offset);
        i = iToken + 1;
      }

      // .... APPEND DATA "appendedData" .... end "appendedData"
      // option here to designate other than "appendedData"
      // .... APPEND "appendedData" @x ....
      

      if (tokAt(i) == T.append) {
        // for CIF reader -- experimental
        if (tokAt(++i) == T.data) {
          i += 2;
          appendedData = (String) getToken(i++).value;
          appendedKey = stringParameter(++i);
          ++i;
        } else {
          appendedKey = stringParameter(i++);
          appendedData = stringParameter(i++);
        }
        htParams.put(appendedKey, appendedData);
      }

      if (tokAt(i) == T.filter)
        filter = stringParameter(++i);

    } else {

      // list of file names 
      // or COORD {i j k} "fileName" 
      // or COORD ({bitset}) "fileName"
      // or FILTER "xxxx"

      if (i == 1) {
        i++;
        loadScript.append(" " + modelName);
      }

      P3 pt = null;
      BS bs = null;
      JmolList<String> fNames = new  JmolList<String>();
      while (i < slen) {
        switch (tokAt(i)) {
        case T.filter:
          filter = stringParameter(++i);
          ++i;
          continue;
        case T.coord:
          htParams.remove("isTrajectory");
          if (firstLastSteps == null) {
            firstLastSteps = new  JmolList<Object>();
            pt = P3.new3(0, -1, 1);
          }
          if (isPoint3f(++i)) {
            pt = getPoint3f(i, false);
            i = iToken + 1;
          } else if (tokAt(i) == T.bitset) {
            bs = (BS) getToken(i).value;
            pt = null;
            i = iToken + 1;
          }
          break;
        case T.identifier:
          error(ERROR_invalidArgument);
        }
        fNames.addLast(filename = parameterAsString(i++));
        if (pt != null) {
          firstLastSteps.addLast(new int[] { (int) pt.x, (int) pt.y, (int) pt.z });
          loadScript.append(" COORD " + Escape.eP(pt));
        } else if (bs != null) {
          firstLastSteps.addLast(bs);
          loadScript.append(" COORD " + Escape.e(bs));
        }
        loadScript.append(" /*file*/$FILENAME" + fNames.size() + "$");
      }
      if (firstLastSteps != null) {
        htParams.put("firstLastSteps", firstLastSteps);
      }
      nFiles = fNames.size();
      filenames = new String[nFiles];
      for (int j = 0; j < nFiles; j++)
        filenames[j] = fNames.get(j);
      filename = "fileSet";
    }

    // end of parsing

    if (!doLoadFiles)
      return;

    // get default filter if necessary

    if (appendedData != null) {
      sOptions += " APPEND data \"" + appendedKey + "\"\n" + appendedData 
      + (appendedData.endsWith("\n") ? "" : "\n") + "end \"" + appendedKey + "\""; 
    }
    if (filter == null)
      filter = viewer.getDefaultLoadFilter();
    if (filter.length() > 0) {
      htParams.put("filter", filter);
      if (filter.equalsIgnoreCase("2d")) // MOL file hack
        filter = "2D-noMin";
      sOptions += " FILTER " + Escape.eS(filter);
    }

    // store inline data or variable data in htParams

    boolean isVariable = false;
    if (filenames == null) {
      if (isInline) {
        htParams.put("fileData", filename);
      } else if (filename.startsWith("@") && filename.length() > 1) {
        isVariable = true;
        String s = getStringParameter(filename.substring(1), false);
        htParams.put("fileData", s);
        loadScript = new SB().append("{\n    var ")
        .append(filename.substring(1)).append(" = ")
        .append(Escape.eS(s)).append(";\n    ").appendSB(loadScript);
      }
    }

    // set up the output stream from AS keyword

    OutputStream os = null;
    if (localName != null) {
      if (localName.equals("."))
        localName = viewer.getFilePath(filename, true);
      if (localName.length() == 0
          || viewer.getFilePath(localName, false).equalsIgnoreCase(
              viewer.getFilePath(filename, false)))
        error(ERROR_invalidArgument);
      String[] fullPath = new String[] { localName };
      os = viewer.getOutputStream(localName, fullPath);
      if (os == null)
        Logger.error("Could not create output stream for " + fullPath[0]);
      else
        htParams.put("OutputStream", os);
    }

    if (filenames == null && tokType == 0) {
      // a single file or string -- complete the loadScript
      loadScript.append(" ");
      if (isVariable || isInline) {
        loadScript.append(Escape.eS(filename));
      } else if (!isData) {
        if (!filename.equals("string") && !filename.equals("string[]"))
          loadScript.append("/*file*/");
        if (localName != null)
          localName = viewer.getFilePath(localName, false);
        loadScript.append((localName != null ? Escape.eS(localName)
            : "$FILENAME$"));
      }
      if (sOptions.length() > 0)
        loadScript.append(" /*options*/ ").append(sOptions);
      if (isVariable)
        loadScript.append("\n  }");
      htParams.put("loadScript", loadScript);
    }
    setCursorWait(true);
    errMsg = viewer.loadModelFromFile(null, filename, filenames, null,
        isAppend, htParams, loadScript, tokType);
    if (os != null)
      try {
        viewer.setFileInfo(new String[] { localName, localName, localName });
        Logger.info(GT._("file {0} created", localName));
        showString(viewer.getFilePath(localName, false) + " created");
        os.close();
      } catch (IOException e) {
        Logger.error("error closing file " + e.toString());
      }
    if (tokType > 0) {
      // we are just loading an atom property
      // reset the file info in FileManager, check for errors, and return
      viewer.setFileInfo(tempFileInfo);
      if (errMsg != null && !isCmdLine_c_or_C_Option)
        evalError(errMsg, null);
      return;
    }
    if (errMsg != null && !isCmdLine_c_or_C_Option) {
      if (errMsg.indexOf(JC.NOTE_SCRIPT_FILE) == 0) {
        filename = errMsg.substring(JC.NOTE_SCRIPT_FILE.length())
            .trim();
        script(0, filename);
        return;
      }
      evalError(errMsg, null);
    }
    if (isAppend && (appendNew || nFiles > 1)) {
      viewer.setAnimationRange(-1, -1);
      viewer.setCurrentModelIndex(modelCount0);
    }
    if (scriptLevel == 0 && !isAppend && nFiles < 2)
      showString((String) viewer.getModelSetAuxiliaryInfoValue("modelLoadNote"));
    if (logMessages)
      scriptStatusOrBuffer("Successfully loaded:"
          + (filenames == null ? htParams.get("fullPathName") : modelName));
    Map<String, Object> info = viewer.getModelSetAuxiliaryInfo();
    if (info != null && info.containsKey("centroidMinMax")
        && viewer.getAtomCount() > 0)
      viewer.setCentroid(isAppend ? atomCount0 : 0, viewer.getAtomCount() - 1,
          (int[]) info.get("centroidMinMax"));
    String script = viewer.getDefaultLoadScript();
    String msg = "";
    if (script.length() > 0)
      msg += "\nUsing defaultLoadScript: " + script;
    if (info != null && viewer.getAllowEmbeddedScripts()) {
      String embeddedScript = (String) info.remove("jmolscript");
      if (embeddedScript != null && embeddedScript.length() > 0) {
        msg += "\nAdding embedded #jmolscript: " + embeddedScript;
        script += ";" + embeddedScript;
        setStringProperty("_loadScript", script);
        script = "allowEmbeddedScripts = false;try{" + script
            + "} allowEmbeddedScripts = true;";
      }

    }
    logLoadInfo(msg);

    String siteScript = (info == null ? null : (String) info
        .remove("sitescript"));
    if (siteScript != null)
      script = siteScript + ";" + script;
    if (script.length() > 0 && !isCmdLine_c_or_C_Option)
      // NOT checking embedded scripts in some cases
      runScript(script);
  }

  @SuppressWarnings("unchecked")
  private void logLoadInfo(String msg) {
    if (msg.length() > 0)
      Logger.info(msg);
    SB sb = new SB();
    int modelCount = viewer.getModelCount();
    if (modelCount > 1)
      sb.append((viewer.isMovie() ? viewer.getFrameCount() + " frames" : modelCount + " models")+"\n");
    for (int i = 0; i < modelCount; i++) {
      Map<String, Object> moData = (Map<String, Object>) viewer
          .getModelAuxiliaryInfoValue(i, "moData");
      if (moData == null)
        continue;
      sb.appendI(((JmolList<Map<String, Object>>) moData.get("mos")).size()).append(
          " molecular orbitals in model ").append(
          viewer.getModelNumberDotted(i)).append("\n");
    }
    if (sb.length() > 0)
      showString(sb.toString());
  }

  private String getFullPathName() throws ScriptException {
    String filename = (!chk || isCmdLine_C_Option ? viewer
        .getFullPathName() : "test.xyz");
    if (filename == null)
      error(ERROR_invalidArgument);
    return filename;
  }

  private void measure() throws ScriptException {
    if (tokAt(1) == T.search) {
      String smarts = stringParameter(slen == 3 ? 2 : 4);
      if (chk)
        return;
      Atom[] atoms = viewer.modelSet.atoms;
      int atomCount = viewer.getAtomCount();
      int[][] maps = viewer.getSmilesMatcher().getCorrelationMaps(smarts,
          atoms, atomCount, viewer.getSelectionSet(false), true, false);
      if (maps == null)
        return;
      setShapeProperty(JC.SHAPE_MEASURES, "maps", maps);
      return;
    }
    switch (slen) {
    case 1:
    case 2:
      switch (getToken(1).tok) {
      case T.nada:
      case T.on:
        setShapeProperty(JC.SHAPE_MEASURES, "hideAll", Boolean.FALSE);
        return;
      case T.off:
        setShapeProperty(JC.SHAPE_MEASURES, "hideAll", Boolean.TRUE);
        return;
      case T.list:
        if (!chk)
          showStringPrint(viewer.getMeasurementInfoAsString(), false);
        return;
      case T.delete:
        if (!chk)
          viewer.clearAllMeasurements();
        return;
      case T.string:
        setShapeProperty(JC.SHAPE_MEASURES, "setFormats",
            stringParameter(1));
        return;
      }
      errorStr(ERROR_keywordExpected, "ON, OFF, DELETE");
      break;
    case 3: // measure delete N
      // search "smartsString"
      switch (getToken(1).tok) {
      case T.delete:
        if (getToken(2).tok == T.all) {
          if (!chk)
            viewer.clearAllMeasurements();
        } else {
          int i = intParameter(2) - 1;
          if (!chk)
            viewer.deleteMeasurement(i);
        }
        return;
      }
    }

    int nAtoms = 0;
    int expressionCount = 0;
    int modelIndex = -1;
    int atomIndex = -1;
    int ptFloat = -1;
    int[] countPlusIndexes = new int[5];
    float[] rangeMinMax = new float[] { Float.MAX_VALUE, Float.MAX_VALUE };
    boolean isAll = false;
    boolean isAllConnected = false;
    boolean isNotConnected = false;
    boolean isRange = true;
    RadiusData rd = null;
    Boolean intramolecular = null;
    int tokAction = T.opToggle;
    String strFormat = null;
    JmolList<Object> points = new  JmolList<Object>();
    BS bs = new BS();
    Object value = null;
    TickInfo tickInfo = null;
    int nBitSets = 0;
    for (int i = 1; i < slen; ++i) {
      switch (getToken(i).tok) {
      case T.identifier:
        errorStr(ERROR_keywordExpected, "ALL, ALLCONNECTED, DELETE");
        break;
      default:
        error(ERROR_expressionOrIntegerExpected);
        break;
      case T.opNot:
        if (tokAt(i + 1) != T.connected)
          error(ERROR_invalidArgument);
        i++;
        isNotConnected = true;
        break;
      case T.connected:
      case T.allconnected:
      case T.all:
        isAllConnected = (theTok == T.allconnected);
        atomIndex = -1;
        isAll = true;
        if (isAllConnected && isNotConnected)
          error(ERROR_invalidArgument);
        break;
      case T.decimal:
        if (rd != null)
          error(ERROR_invalidArgument);
        isAll = true;
        isRange = true;
        ptFloat = (ptFloat + 1) % 2;
        rangeMinMax[ptFloat] = floatParameter(i);
        break;
      case T.delete:
        if (tokAction != T.opToggle)
          error(ERROR_invalidArgument);
        tokAction = T.delete;
        break;
      case T.integer:
        int iParam = intParameter(i);
        if (isAll) {
          isRange = true; // irrelevant if just four integers
          ptFloat = (ptFloat + 1) % 2;
          rangeMinMax[ptFloat] = iParam;
        } else {
          atomIndex = viewer.getAtomIndexFromAtomNumber(iParam);
          if (!chk && atomIndex < 0)
            return;
          if (value != null)
            error(ERROR_invalidArgument);
          if ((countPlusIndexes[0] = ++nAtoms) > 4)
            error(ERROR_badArgumentCount);
          countPlusIndexes[nAtoms] = atomIndex;
        }
        break;
      case T.modelindex:
        modelIndex = intParameter(++i);
        break;
      case T.off:
        if (tokAction != T.opToggle)
          error(ERROR_invalidArgument);
        tokAction = T.off;
        break;
      case T.on:
        if (tokAction != T.opToggle)
          error(ERROR_invalidArgument);
        tokAction = T.on;
        break;
      case T.range:
        isAll = true;
        isRange = true; // unnecessary
        atomIndex = -1;
        break;
      case T.intramolecular:
      case T.intermolecular:
        intramolecular = Boolean.valueOf(theTok == T.intramolecular);
        isAll = true;
        isNotConnected = (theTok == T.intermolecular);
        break;
      case T.vanderwaals:
        if (ptFloat >= 0)
          error(ERROR_invalidArgument);
        rd = encodeRadiusParameter(i, false, true);
        rd.values = rangeMinMax;
        i = iToken;
        isNotConnected = true;
        isAll = true;
        intramolecular = Boolean.valueOf(false);
        if (nBitSets == 1) {
          nBitSets++;
          nAtoms++;
          BS bs2 = BSUtil.copy(bs);
          BSUtil.invertInPlace(bs2, viewer.getAtomCount());
          bs2.and(viewer.getAtomsWithinRadius(5, bs, false, null));
          points.addLast(bs2);
        }
        break;
      case T.bitset:
      case T.expressionBegin:
      case T.leftbrace:
      case T.point3f:
      case T.dollarsign:
        if (theTok == T.bitset || theTok == T.expressionBegin)
          nBitSets++;
        if (atomIndex >= 0)
          error(ERROR_invalidArgument);
        expressionResult = Boolean.FALSE;
        value = centerParameter(i);
        if (expressionResult instanceof BS) {
          value = bs = (BS) expressionResult;
          if (!chk && bs.length() == 0)
            return;
        }
        if (value instanceof P3) {
          Point3fi v = new Point3fi();
          v.setT((P3) value);
          v.modelIndex = (short) modelIndex;
          value = v;
        }
        if ((nAtoms = ++expressionCount) > 4)
          error(ERROR_badArgumentCount);
        i = iToken;
        points.addLast(value);
        break;
      case T.string:
        // measures "%a1 %a2 %v %u"
        strFormat = stringParameter(i);
        break;
      case T.ticks:
        tickInfo = checkTicks(i, false, true, true);
        i = iToken;
        tokAction = T.define;
        break;
      }
    }
    if (rd != null && (ptFloat >= 0 || nAtoms != 2) || nAtoms < 2
        && (tickInfo == null || nAtoms == 1))
      error(ERROR_badArgumentCount);
    if (strFormat != null && strFormat.indexOf(nAtoms + ":") != 0)
      strFormat = nAtoms + ":" + strFormat;
    if (isRange) {
      if (rangeMinMax[1] < rangeMinMax[0]) {
        rangeMinMax[1] = rangeMinMax[0];
        rangeMinMax[0] = (rangeMinMax[1] == Float.MAX_VALUE ? Float.MAX_VALUE
            : -200);
      }
    }
    if (chk)
      return;
    if (value != null || tickInfo != null) {
      if (rd == null)
        rd = new RadiusData(rangeMinMax, 0, null, null);
      if (value == null)
        tickInfo.id = "default";
      if (value != null && strFormat != null && tokAction == T.opToggle)
        tokAction = T.define;
      setShapeProperty(JC.SHAPE_MEASURES, "measure", 
          (new MeasurementData(viewer, points)).set(tokAction, rd, strFormat, null, tickInfo,
              isAllConnected, isNotConnected, intramolecular, isAll));
      return;
    }
    switch (tokAction) {
    case T.delete:
      setShapeProperty(JC.SHAPE_MEASURES, "delete", countPlusIndexes);
      break;
    case T.on:
      setShapeProperty(JC.SHAPE_MEASURES, "show", countPlusIndexes);
      break;
    case T.off:
      setShapeProperty(JC.SHAPE_MEASURES, "hide", countPlusIndexes);
      break;
    default:
      setShapeProperty(JC.SHAPE_MEASURES,
          (strFormat == null ? "toggle" : "toggleOn"), countPlusIndexes);
      if (strFormat != null)
        setShapeProperty(JC.SHAPE_MEASURES, "setFormats", strFormat);
    }
  }

  private String plot(T[] args) throws ScriptException {
    // also used for draw [quaternion, helix, ramachandran] 
    // and write quaternion, ramachandran, plot, ....
    // and plot property propertyX, propertyY, propertyZ //
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      errorStr(ERROR_multipleModelsDisplayedNotOK, "plot");
    modelIndex = viewer.getJmolDataSourceFrame(modelIndex);
    int pt = args.length - 1;
    boolean isReturnOnly = (args != st);
    T[] statementSave = st;
    if (isReturnOnly)
      st = args;
    int tokCmd = (isReturnOnly ? T.show : args[0].tok);
    int pt0 = (isReturnOnly || tokCmd == T.quaternion
        || tokCmd == T.ramachandran ? 0 : 1);
    String filename = null;
    boolean makeNewFrame = true;
    boolean isDraw = false;
    switch (tokCmd) {
    case T.plot:
    case T.quaternion:
    case T.ramachandran:
      break;
    case T.draw:
      makeNewFrame = false;
      isDraw = true;
      break;
    case T.show:
      makeNewFrame = false;
      break;
    case T.write:
      makeNewFrame = false;
      if (tokAtArray(pt, args) == T.string) {
        filename = stringParameter(pt--);
      } else if (tokAtArray(pt - 1, args) == T.per) {
        filename = parameterAsString(pt - 2) + "." + parameterAsString(pt);
        pt -= 3;
      } else {
        st = statementSave;
        iToken = st.length;
        error(ERROR_endOfStatementUnexpected);
      }
      break;
    }
    String qFrame = "";
    Object[] parameters = null;
    String stateScript = "";
    boolean isQuaternion = false;
    boolean isDerivative = false;
    boolean isSecondDerivative = false;
    boolean isRamachandranRelative = false;
    int propertyX = 0, propertyY = 0, propertyZ = 0;
    BS bs = BSUtil.copy(viewer.getSelectionSet(false));
    String preSelected = "; select " + Escape.e(bs) + ";\n ";
    String type = optParameterAsString(pt).toLowerCase();
    P3 minXYZ = null;
    P3 maxXYZ = null;
    int tok = tokAtArray(pt0, args);
    if (tok == T.string)
      tok = T.getTokFromName((String) args[pt0].value);
    switch (tok) {
    default:
      iToken = 1;
      error(ERROR_invalidArgument);
      break;
    case T.data:
      iToken = 1;
      type = "data";
      preSelected = "";
      break;
    case T.property:
      iToken = pt0 + 1;
      if (!T.tokAttr(propertyX = tokAt(iToken++), T.atomproperty)
          || !T.tokAttr(propertyY = tokAt(iToken++), T.atomproperty))
        error(ERROR_invalidArgument);
      if (T.tokAttr(propertyZ = tokAt(iToken), T.atomproperty))
        iToken++;
      else
        propertyZ = 0;
      if (tokAt(iToken) == T.min) {
        minXYZ = getPoint3f(++iToken, false);
        iToken++;
      }
      if (tokAt(iToken) == T.max) {
        maxXYZ = getPoint3f(++iToken, false);
        iToken++;
      }
      type = "property " + T.nameOf(propertyX) + " "
          + T.nameOf(propertyY)
          + (propertyZ == 0 ? "" : " " + T.nameOf(propertyZ));
      if (bs.nextSetBit(0) < 0)
        bs = viewer.getModelUndeletedAtomsBitSet(modelIndex);
      stateScript = "select " + Escape.e(bs) + ";\n ";
      break;
    case T.ramachandran:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = optParameterAsString(--pt).toLowerCase();
      }
      isRamachandranRelative = (pt > pt0 && type.startsWith("r"));
      type = "ramachandran" + (isRamachandranRelative ? " r" : "")
          + (tokCmd == T.draw ? " draw" : "");
      break;
    case T.quaternion:
    case T.helix:
      qFrame = " \"" + viewer.getQuaternionFrame() + "\"";
      stateScript = "set quaternionFrame" + qFrame + ";\n  ";
      isQuaternion = true;
      // working backward this time:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = optParameterAsString(--pt).toLowerCase();
      }
      isDerivative = (type.startsWith("deriv") || type.startsWith("diff"));
      isSecondDerivative = (isDerivative && type.indexOf("2") > 0);
      if (isDerivative)
        pt--;
      if (type.equalsIgnoreCase("helix") || type.equalsIgnoreCase("axis")) {
        isDraw = true;
        isDerivative = true;
        pt = -1;
      }
      type = ((pt <= pt0 ? "" : optParameterAsString(pt)) + "w")
          .substring(0, 1);
      if (type.equals("a") || type.equals("r"))
        isDerivative = true;
      if (!Parser.isOneOf(type, "w;x;y;z;r;a")) // a absolute; r relative
        evalError("QUATERNION [w,x,y,z,a,r] [difference][2]", null);
      type = "quaternion " + type + (isDerivative ? " difference" : "")
          + (isSecondDerivative ? "2" : "") + (isDraw ? " draw" : "");
      break;
    }
    st = statementSave;
    if (chk) // just in case we later add parameter options to this
      return "";

    // if not just drawing check to see if there is already a plot of this type

    if (makeNewFrame) {
      stateScript += "plot " + type;
      int ptDataFrame = viewer.getJmolDataFrameIndex(modelIndex, stateScript);
      if (ptDataFrame > 0 && tokCmd != T.write && tokCmd != T.show) {
        // no -- this is that way we switch frames. viewer.deleteAtoms(viewer.getModelUndeletedAtomsBitSet(ptDataFrame), true);
        // data frame can't be 0.
        viewer.setCurrentModelIndexClear(ptDataFrame, true);
        // BitSet bs2 = viewer.getModelAtomBitSet(ptDataFrame);
        // bs2.and(bs);
        // need to be able to set data directly as well.
        // viewer.display(BitSetUtil.setAll(viewer.getAtomCount()), bs2, tQuiet);
        return "";
      }
    }

    // prepare data for property plotting

    float[] dataX = null, dataY = null, dataZ = null;
    P3 factors = P3.new3(1, 1, 1);
    if (tok == T.property) {
      dataX = getBitsetPropertyFloat(bs, propertyX | T.selectedfloat,
          (minXYZ == null ? Float.NaN : minXYZ.x), (maxXYZ == null ? Float.NaN
              : maxXYZ.x));
      dataY = getBitsetPropertyFloat(bs, propertyY | T.selectedfloat,
          (minXYZ == null ? Float.NaN : minXYZ.y), (maxXYZ == null ? Float.NaN
              : maxXYZ.y));
      if (propertyZ != 0)
        dataZ = getBitsetPropertyFloat(bs, propertyZ | T.selectedfloat,
            (minXYZ == null ? Float.NaN : minXYZ.z),
            (maxXYZ == null ? Float.NaN : maxXYZ.z));
      if (minXYZ == null)
        minXYZ = P3.new3(getMinMax(dataX, false, propertyX), getMinMax(
            dataY, false, propertyY), getMinMax(dataZ, false, propertyZ));
      if (maxXYZ == null)
        maxXYZ = P3.new3(getMinMax(dataX, true, propertyX), getMinMax(
            dataY, true, propertyY), getMinMax(dataZ, true, propertyZ));
      Logger.info("plot min/max: " + minXYZ + " " + maxXYZ);
      P3 center = P3.newP(maxXYZ);
      center.add(minXYZ);
      center.scale(0.5f);
      factors.setT(maxXYZ);
      factors.sub(minXYZ);
      factors.set(factors.x / 200, factors.y / 200, factors.z / 200);
      if (T.tokAttr(propertyX, T.intproperty)) {
        factors.x = 1;
        center.x = 0;
      } else if (factors.x > 0.1 && factors.x <= 10) {
        factors.x = 1;
      }
      if (T.tokAttr(propertyY, T.intproperty)) {
        factors.y = 1;
        center.y = 0;
      } else if (factors.y > 0.1 && factors.y <= 10) {
        factors.y = 1;
      }
      if (T.tokAttr(propertyZ, T.intproperty)) {
        factors.z = 1;
        center.z = 0;
      } else if (factors.z > 0.1 && factors.z <= 10) {
        factors.z = 1;
      }
      if (propertyZ == 0)
        center.z = minXYZ.z = maxXYZ.z = factors.z = 0;
      for (int i = 0; i < dataX.length; i++)
        dataX[i] = (dataX[i] - center.x) / factors.x;
      for (int i = 0; i < dataY.length; i++)
        dataY[i] = (dataY[i] - center.y) / factors.y;
      if (propertyZ != 0)
        for (int i = 0; i < dataZ.length; i++)
          dataZ[i] = (dataZ[i] - center.z) / factors.z;
      parameters = new Object[] { bs, dataX, dataY, dataZ, minXYZ, maxXYZ,
          factors, center };
    }

    // all set...

    if (tokCmd == T.write)
      return viewer.streamFileData(filename, "PLOT", type, modelIndex,
          parameters);
    
    String data = (type.equals("data") ? "1 0 H 0 0 0 # Jmol PDB-encoded data" : viewer.getPdbData(modelIndex, type, parameters));
    
    if (tokCmd == T.show)
      return data;

    if (Logger.debugging)
      Logger.info(data);

    if (tokCmd == T.draw) {
      runScript(data);
      return "";
    }

    // create the new model

    String[] savedFileInfo = viewer.getFileInfo();
    boolean oldAppendNew = viewer.getAppendNew();
    viewer.setAppendNew(true);
    boolean isOK = (data != null && viewer.loadInline(data, true) == null);
    viewer.setAppendNew(oldAppendNew);
    viewer.setFileInfo(savedFileInfo);
    if (!isOK)
      return "";
    int modelCount = viewer.getModelCount();
    viewer.setJmolDataFrame(stateScript, modelIndex, modelCount - 1);
    if (tok != T.property)
      stateScript += ";\n" + preSelected;
    StateScript ss = viewer.addStateScript(stateScript, true, false);

    // get post-processing script

    float radius = 150;
    String script;
    switch (tok) {
    default:
      script = "frame 0.0; frame last; reset;select visible;wireframe only;";
      radius = 10;
      break;
    case T.property:
      viewer.setFrameTitle(modelCount - 1, type + " plot for model "
          + viewer.getModelNumberDotted(modelIndex));
      float f = 3;
      script = "frame 0.0; frame last; reset;" + "select visible; spacefill "
          + f + "; wireframe 0;" + "draw plotAxisX" + modelCount
          + " {100 -100 -100} {-100 -100 -100} \"" + T.nameOf(propertyX)
          + "\";" + "draw plotAxisY" + modelCount
          + " {-100 100 -100} {-100 -100 -100} \"" + T.nameOf(propertyY)
          + "\";";
      if (propertyZ != 0)
        script += "draw plotAxisZ" + modelCount
            + " {-100 -100 100} {-100 -100 -100} \"" + T.nameOf(propertyZ)
            + "\";";
      break;
    case T.ramachandran:
      viewer.setFrameTitle(modelCount - 1, "ramachandran plot for model "
          + viewer.getModelNumberDotted(modelIndex));
      script = "frame 0.0; frame last; reset;"
          + "select visible; color structure; spacefill 3.0; wireframe 0;"
          + "draw ramaAxisX" + modelCount + " {100 0 0} {-100 0 0} \"phi\";"
          + "draw ramaAxisY" + modelCount + " {0 100 0} {0 -100 0} \"psi\";";
      break;
    case T.quaternion:
    case T.helix:
      viewer.setFrameTitle(modelCount - 1, type.replace('w', ' ') + qFrame
          + " for model " + viewer.getModelNumberDotted(modelIndex));
      String color = (C
          .getHexCode(viewer.getColixBackgroundContrast()));
      script = "frame 0.0; frame last; reset;"
          + "select visible; wireframe 0; spacefill 3.0; "
          + "isosurface quatSphere" + modelCount + " color " + color
          + " sphere 100.0 mesh nofill frontonly translucent 0.8;"
          + "draw quatAxis" + modelCount
          + "X {100 0 0} {-100 0 0} color red \"x\";" + "draw quatAxis"
          + modelCount + "Y {0 100 0} {0 -100 0} color green \"y\";"
          + "draw quatAxis" + modelCount
          + "Z {0 0 100} {0 0 -100} color blue \"z\";" + "color structure;"
          + "draw quatCenter" + modelCount + "{0 0 0} scale 0.02;";
      break;
    }

    // run the post-processing script and set rotation radius and display frame title
    runScript(script + preSelected);
    ss.setModelIndex(viewer.getCurrentModelIndex());
    viewer.setRotationRadius(radius, true);
    sm.loadShape(JC.SHAPE_ECHO);
    showString("frame " + viewer.getModelNumberDotted(modelCount - 1)
        + (type.length() > 0 ? " created: " + type + (isQuaternion ? qFrame : "") : ""));
    return "";
  }

  private static float getMinMax(float[] data, boolean isMax, int tok) {
    if (data == null)
      return 0;
    switch (tok) {
    case T.omega:
    case T.phi:
    case T.psi:
      return (isMax ? 180 : -180);
    case T.eta:
    case T.theta:
      return (isMax ? 360 : 0);
    case T.straightness:
      return (isMax ? 1 : -1);
    }
    float fmax = (isMax ? -1E10f : 1E10f);
    for (int i = data.length; --i >= 0;) {
      float f = data[i];
      if (Float.isNaN(f))
        continue;
      if (isMax == (f > fmax))
        fmax = f;
    }
    return fmax;
  }

  private boolean pause() throws ScriptException {
    if (chk || isJS && !allowJSThreads)
      return false;
    String msg = optParameterAsString(1);
    if (!viewer.getBooleanProperty("_useCommandThread")) {
      // showString("Cannot pause thread when _useCommandThread = FALSE: " +
      // msg);
      // return;
    }
    if (viewer.autoExit || !viewer.haveDisplay && !viewer.isJS3D)
      return false;
    if (scriptLevel == 0 && pc == aatoken.length - 1) {
      viewer.scriptStatus("nothing to pause: " + msg);
      return false;
    }
    msg = (msg.length() == 0 ? ": RESUME to continue." : ": "
        + viewer.formatText(msg));
    pauseExecution(true);
    viewer.scriptStatusMsg("script execution paused" + msg,
        "script paused for RESUME");
    return true;
  }

  private void print() throws ScriptException {
    if (slen == 1)
      error(ERROR_badArgumentCount);
    showStringPrint(parameterExpressionString(1, 0), true);
  }

  private void prompt() throws ScriptException {
    String msg = null;
    if (slen == 1) {
      if (!chk)
        msg = getContextTrace(getScriptContext(), null, true).toString();
    } else {
      msg = parameterExpressionString(1, 0);
    }
    if (!chk)
      viewer.prompt(msg, "OK", null, true);
  }

  private void refresh() {
    if (chk)
      return;
    viewer.setTainted(true);
    viewer.requestRepaintAndWait();
  }

  private void reset() throws ScriptException {
    if (slen == 3 && tokAt(1) == T.function) {
      if (!chk)
        viewer.removeFunction(stringParameter(2));
      return;
    }
    checkLength(-2);
    if (chk)
      return;
    if (slen == 1) {
      viewer.reset(false);
      return;
    }
    // possibly "all"
    switch (tokAt(1)) {
    case T.cache:
      viewer.cacheClear();
      return;
    case T.error:
      viewer.resetError();
      return;
    case T.shape:
      viewer.resetShapes(true);
      return;
    case T.function:
      viewer.clearFunctions();
      return;
    case T.structure:
      BS bsAllAtoms = new BS();
      runScript(viewer.getDefaultStructure(null, bsAllAtoms));
      viewer.resetBioshapes(bsAllAtoms);
      return;
    case T.vanderwaals:
      viewer.setData("element_vdw", new Object[] { null, "" }, 0, 0, 0, 0, 0);
      return;
    case T.aromatic:
      viewer.resetAromatic();
      return;
    case T.spin:
      viewer.reset(true);
      return;
    }
    String var = parameterAsString(1);
    if (var.charAt(0) == '_')
      error(ERROR_invalidArgument);
    viewer.unsetProperty(var);
  }

  private void restrict() throws ScriptException {
    boolean isBond = (tokAt(1) == T.bonds);
    select(isBond ? 2 : 1);
    restrictSelected(isBond, true);
  }

  private void restrictSelected(boolean isBond, boolean doInvert) {
    if (chk)
      return;
    BS bsSelected = BSUtil.copy(viewer.getSelectionSet(true));
    if (doInvert) {
      viewer.invertSelection();
      BS bsSubset = viewer.getSelectionSubset();
      if (bsSubset != null) {
        bsSelected = BSUtil.copy(viewer.getSelectionSet(true));
        bsSelected.and(bsSubset);
        viewer.select(bsSelected, false, null, true);
        BSUtil.invertInPlace(bsSelected, viewer.getAtomCount());
        bsSelected.and(bsSubset);
      }
    }
    BSUtil.andNot(bsSelected, viewer.getDeletedAtoms());
    boolean bondmode = viewer.getBondSelectionModeOr();

    if (!isBond)
      setBooleanProperty("bondModeOr", true);
    setShapeSizeBs(JC.SHAPE_STICKS, 0, null);
    // wireframe will not operate on STRUTS even though they are
    // a form of bond order (see BondIteratoSelected)
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_STRUT));
    setShapeSizeBs(JC.SHAPE_STICKS, 0, null);
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_COVALENT_MASK));
    // also need to turn off backbones, ribbons, strands, cartoons
    BS bs = viewer.getSelectionSet(false);
    for (int iShape = JC.SHAPE_MAX_SIZE_ZERO_ON_RESTRICT; --iShape >= 0;)
      if (iShape != JC.SHAPE_MEASURES && sm.getShape(iShape) != null)
        setShapeSizeBs(iShape, 0, bs);
    if (sm.getShape(JC.SHAPE_POLYHEDRA) != null)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "delete", bs);
    sm.setLabel(null, bs);
    if (!isBond)
      setBooleanProperty("bondModeOr", bondmode);
    viewer.select(bsSelected, false, null, true);
  }

  private void rotate(boolean isSpin, boolean isSelected)
      throws ScriptException {

    // rotate is a full replacement for spin
    // spin is DEPRECATED

    /*
     * The Chime spin method:
     * 
     * set spin x 10;set spin y 30; set spin z 10; spin | spin ON spin OFF
     * 
     * Jmol does these "first x, then y, then z" I don't know what Chime does.
     * 
     * spin and rotate are now consolidated here.
     * 
     * far simpler is
     * 
     * spin x 10 spin y 10
     * 
     * these are pure x or y spins or
     * 
     * spin axisangle {1 1 0} 10
     * 
     * this is the same as the old "spin x 10; spin y 10" -- or is it? anyway,
     * it's better!
     * 
     * note that there are many defaults
     * 
     * spin # defaults to spin y 10 
     * spin 10 # defaults to spin y 10 
     * spin x # defaults to spin x 10
     * 
     * and several new options
     * 
     * spin -x 
     * spin axisangle {1 1 0} 10 
     * spin 10 (atomno=1)(atomno=2) 
     * spin 20 {0 0 0} {1 1 1}
     * 
     * spin MOLECULAR {0 0 0} 20
     * 
     * The MOLECULAR keyword indicates that spins or rotations are to be carried
     * out in the internal molecular coordinate frame, not the fixed room frame.
     * 
     * In the case of rotateSelected, all rotations are molecular and the
     * absense of the MOLECULAR keyword indicates to rotate about the geometric
     * center of the molecule, not {0 0 0}
     * 
     * Fractional coordinates may be indicated:
     * 
     * spin 20 {0 0 0/} {1 1 1/}
     * 
     * In association with this, TransformManager and associated functions are
     * TOTALLY REWRITTEN and consolideated. It is VERY clean now - just two
     * methods here -- one fixed and one molecular, two in Viewer, and two in
     * TransformManager. All the centering stuff has been carefully inspected
     * are reorganized as well.
     * 
     * Bob Hanson 5/21/06
     */

    if (slen == 2)
      switch (getToken(1).tok) {
      case T.on:
        if (!chk)
          viewer.setSpinOn(true);
        return;
      case T.off:
        if (!chk)
          viewer.setSpinOn(false);
        return;
      }

    BS bsAtoms = null;
    float degreesPerSecond = Float.MIN_VALUE;
    int nPoints = 0;
    float endDegrees = Float.MAX_VALUE;
    boolean isMolecular = false;
    boolean haveRotation = false;
    JmolList<P3> ptsA = null;
    P3[] points = new P3[2];
    V3 rotAxis = V3.new3(0, 1, 0);
    V3 translation = null;
    Matrix4f m4 = null;
    Matrix3f m3 = null;
    int direction = 1;
    int tok;
    Quaternion q = null;
    boolean helicalPath = false;
    JmolList<P3> ptsB = null;
    BS bsCompare = null;
    P3 invPoint = null;
    Point4f invPlane = null;
    boolean axesOrientationRasmol = viewer.getAxesOrientationRasmol();
    for (int i = 1; i < slen; ++i) {
      switch (tok = getToken(i).tok) {
      case T.bitset:
      case T.expressionBegin:
      case T.leftbrace:
      case T.point3f:
      case T.dollarsign:
        if (tok == T.bitset || tok == T.expressionBegin) {
          if (translation != null || q != null || nPoints == 2) {
            bsAtoms = atomExpressionAt(i);
            ptsB = null;
            isSelected = true;
            break;
          }
        }
        haveRotation = true;
        if (nPoints == 2)
          nPoints = 0;
        // {X, Y, Z}
        // $drawObject[n]
        P3 pt1 = centerParameterForModel(i, viewer.getCurrentModelIndex());
        if (!chk && tok == T.dollarsign
            && tokAt(i + 2) != T.leftsquare) {
          // rotation about an axis such as $line1
          isMolecular = true;
          rotAxis = getDrawObjectAxis(objectNameParameter(++i), viewer
              .getCurrentModelIndex());
        }
        points[nPoints++] = pt1;
        break;
      case T.spin:
        isSpin = true;
        continue;
      case T.internal:
      case T.molecular:
        isMolecular = true;
        continue;
      case T.selected:
        isSelected = true;
        break;
      case T.comma:
        continue;
      case T.integer:
      case T.decimal:
        if (isSpin) {
          // rotate spin ... [degreesPerSecond]
          // rotate spin ... [endDegrees] [degreesPerSecond]
          if (degreesPerSecond == Float.MIN_VALUE) {
            degreesPerSecond = floatParameter(i);
            continue;
          } else if (endDegrees == Float.MAX_VALUE) {
            endDegrees = degreesPerSecond;
            degreesPerSecond = floatParameter(i);
            continue;
          }
        } else {
          // rotate ... [endDegrees]
          // rotate ... [endDegrees] [degreesPerSecond]
          if (endDegrees == Float.MAX_VALUE) {
            endDegrees = floatParameter(i);
            continue;
          } else if (degreesPerSecond == Float.MIN_VALUE) {
            degreesPerSecond = floatParameter(i);
            isSpin = true;
            continue;
          }
        }
        error(ERROR_invalidArgument);
        break;
      case T.minus:
        direction = -1;
        continue;
      case T.x:
        haveRotation = true;
        rotAxis.set(direction, 0, 0);
        continue;
      case T.y:
        haveRotation = true;
        rotAxis.set(0, direction, 0);
        continue;
      case T.z:
        haveRotation = true;
        rotAxis.set(0, 0, (axesOrientationRasmol && !isMolecular ? -direction
            : direction));
        continue;

        // 11.6 options

      case T.point4f:
      case T.quaternion:
        if (tok == T.quaternion)
          i++;
        haveRotation = true;
        q = getQuaternionParameter(i);
        rotAxis.setT(q.getNormal());
        endDegrees = q.getTheta();
        break;
      case T.axisangle:
        haveRotation = true;
        if (isPoint3f(++i)) {
          rotAxis.setT(centerParameter(i));
          break;
        }
        Point4f p4 = getPoint4f(i);
        rotAxis.set(p4.x, p4.y, p4.z);
        endDegrees = p4.w;
        q = Quaternion.newVA(rotAxis, endDegrees);
        break;
      case T.branch:
        haveRotation = true;
        int iAtom1 = atomExpressionAt(++i).nextSetBit(0);
        int iAtom2 = atomExpressionAt(++iToken).nextSetBit(0);
        if (iAtom1 < 0 || iAtom2 < 0)
          return;
        bsAtoms = viewer.getBranchBitSet(iAtom2, iAtom1);
        isSelected = true;
        isMolecular = true;
        points[0] = viewer.getAtomPoint3f(iAtom1);
        points[1] = viewer.getAtomPoint3f(iAtom2);
        nPoints = 2;
        break;

      // 12.0 options

      case T.translate:
        translation = V3.newV(centerParameter(++i));
        isMolecular = isSelected = true;
        break;
      case T.helix:
        // screw motion, for quaternion-based operations
        helicalPath = true;
        continue;
      case T.symop:
        int symop = intParameter(++i);
        if (chk)
          continue;
        Map<String, Object> info = viewer.getSpaceGroupInfo(null);
        Object[] op = (info == null ? null : (Object[]) info.get("operations"));
        if (symop == 0 || op == null || op.length < Math.abs(symop))
          error(ERROR_invalidArgument);
        op = (Object[]) op[Math.abs(symop) - 1];
        translation = (V3) op[5];
        invPoint = (P3) op[6];
        points[0] = (P3) op[7];
        if (op[8] != null)
          rotAxis = (V3) op[8];
        endDegrees = ((Integer) op[9]).intValue();
        if (symop < 0) {
          endDegrees = -endDegrees;
          if (translation != null)
            translation.scale(-1);
        }
        if (endDegrees == 0 && points[0] != null) {
          // glide plane
          rotAxis.normalize();
          Measure.getPlaneThroughPoint(points[0], rotAxis,
              invPlane = new Point4f());
        }
        q = Quaternion.newVA(rotAxis, endDegrees);
        nPoints = (points[0] == null ? 0 : 1);
        isMolecular = true;
        haveRotation = true;
        isSelected = true;
        continue;
      case T.compare:
      case T.matrix4f:
      case T.matrix3f:
        haveRotation = true;
        if (tok == T.compare) {
          bsCompare = atomExpressionAt(++i);
          ptsA = viewer.getAtomPointVector(bsCompare);
          if (ptsA == null)
            errorAt(ERROR_invalidArgument, i);
          i = iToken;
          ptsB = getPointVector(getToken(++i), i);
          if (ptsB == null || ptsA.size() != ptsB.size())
            errorAt(ERROR_invalidArgument, i);
          m4 = new Matrix4f();
          points[0] = new P3();
          nPoints = 1;
          float stddev = (chk ? 0 : Measure.getTransformMatrix4(ptsA,
              ptsB, m4, points[0]));
          // if the standard deviation is very small, we leave ptsB
          // because it will be used to set the absolute final positions
          if (stddev > 0.001)
            ptsB = null;
        } else if (tok == T.matrix4f) {
          m4 = (Matrix4f) theToken.value;
        }
        m3 = new Matrix3f();
        if (m4 != null) {
          translation = new V3();
          m4.get(translation);
          m4.getRotationScale(m3);
        } else {
          m3 = (Matrix3f) theToken.value;
        }
        q = (chk ? new Quaternion() : Quaternion.newM(m3));
        rotAxis.setT(q.getNormal());
        endDegrees = q.getTheta();
        isMolecular = true;
        break;
      default:
        error(ERROR_invalidArgument);
      }
      i = iToken;
    }
    if (chk)
      return;
    if (isSelected && bsAtoms == null)
      bsAtoms = viewer.getSelectionSet(false);
    if (bsCompare != null) {
      isSelected = true;
      if (bsAtoms == null)
        bsAtoms = bsCompare;
    }
    float rate = (degreesPerSecond == Float.MIN_VALUE ? 10
        : endDegrees == Float.MAX_VALUE ? degreesPerSecond 
        : (degreesPerSecond < 0) == (endDegrees > 0) ?
        // -n means number of seconds, not degreesPerSecond
        -endDegrees / degreesPerSecond
            : degreesPerSecond);
    if (q != null) {
      // only when there is a translation (4x4 matrix or TRANSLATE)
      // do we set the rotation to be the center of the selected atoms or model
      if (nPoints == 0 && translation != null)
        points[0] = viewer.getAtomSetCenter(bsAtoms != null ? bsAtoms
            : isSelected ? viewer.getSelectionSet(false) : viewer
                .getModelUndeletedAtomsBitSet(-1));
      if (helicalPath && translation != null) {
        points[1] = P3.newP(points[0]);
        points[1].add(translation);
        Object[] ret = (Object[]) Measure.computeHelicalAxis(null, T.array,
            points[0], points[1], q);
        points[0] = (P3) ret[0];
        float theta = ((P3) ret[3]).x;
        if (theta != 0) {
          translation = (V3) ret[1];
          rotAxis = V3.newV(translation);
          if (theta < 0)
            rotAxis.scale(-1);
        }
        m4 = null;
      }
      if (isSpin && m4 == null)
        m4 = ScriptMathProcessor.getMatrix4f(q.getMatrix(), translation);
      if (points[0] != null)
        nPoints = 1;
    }
    if (invPoint != null) {
      viewer.invertAtomCoordPt(invPoint, bsAtoms);
      if (rotAxis == null)
        return;
    }
    if (invPlane != null) {
      viewer.invertAtomCoordPlane(invPlane, bsAtoms);
      if (rotAxis == null)
        return;
    }
    if (nPoints < 2) {
      if (!isMolecular) {
        // fixed-frame rotation
        // rotate x 10 # Chime-like
        // rotate axisangle {0 1 0} 10
        // rotate x 10 (atoms) # point-centered
        // rotate x 10 $object # point-centered
        if (isSpin && bsAtoms == null && !useThreads())
          return;
        if (viewer.rotateAxisAngleAtCenter(this, points[0], rotAxis, rate, endDegrees,
            isSpin, bsAtoms) && isJS && isSpin && bsAtoms == null)
          throw new ScriptInterruption(this, "rotate", 1);
        return;
      }
      if (nPoints == 0)
        points[0] = new P3();
      // rotate MOLECULAR
      // rotate MOLECULAR (atom1)
      // rotate MOLECULAR x 10 (atom1)
      // rotate axisangle MOLECULAR (atom1)
      points[1] = P3.newP(points[0]);
      points[1].add(rotAxis);
      nPoints = 2;
    }
    if (nPoints == 0)
      points[0] = new P3();
    if (nPoints < 2 || points[0].distance(points[1]) == 0) {
      points[1] = P3.newP(points[0]);
      points[1].y += 1.0;
    }
    if (endDegrees == Float.MAX_VALUE)
      endDegrees = 0;
    if (endDegrees != 0 && translation != null && !haveRotation)
      translation.scale(endDegrees / translation.length());
    if (isSpin && translation != null
        && (endDegrees == 0 || degreesPerSecond == 0)) {
      // need a token rotation
      endDegrees = 0.01f;
      rate = (degreesPerSecond == Float.MIN_VALUE ? 0.01f
          : degreesPerSecond < 0 ?
          // -n means number of seconds, not degreesPerSecond
          -endDegrees / degreesPerSecond
              : degreesPerSecond * 0.01f / translation.length());
      degreesPerSecond = 0.01f;
    }
    if (bsAtoms != null && isSpin && ptsB == null && m4 != null) {
      ptsA = viewer.getAtomPointVector(bsAtoms);
      ptsB = Measure.transformPoints(ptsA, m4, points[0]);
    }
    if (bsAtoms != null && !isSpin && ptsB != null) {
      viewer.setAtomCoord(bsAtoms, T.xyz, ptsB);
    } else {
      if (!useThreads())
        return;
      if (viewer.rotateAboutPointsInternal(this, points[0], points[1], rate, endDegrees,
          isSpin, bsAtoms, translation, ptsB) && isJS && isSpin)
        throw new ScriptInterruption(this, "rotate", 1);
    }
  }

  private Quaternion getQuaternionParameter(int i) throws ScriptException {
    if (tokAt(i) == T.varray) {
      JmolList<SV> sv = ((SV) getToken(i)).getList();
      Point4f p4 = null;
      if (sv.size() == 0 || (p4 = SV.pt4Value(sv.get(0))) == null)
        error(ERROR_invalidArgument);
      return Quaternion.newP4(p4);
    }
    return Quaternion.newP4(getPoint4f(i));
  }

  JmolList<P3> getPointVector(T t, int i) throws ScriptException {
    switch (t.tok) {
    case T.bitset:
      return viewer.getAtomPointVector((BS) t.value);
    case T.varray:
      JmolList<P3> data = new  JmolList<P3>();
      P3 pt;
      JmolList<SV> pts = ((SV) t).getList();
      for (int j = 0; j < pts.size(); j++)
        if ((pt = SV.ptValue(pts.get(j))) != null)
          data.addLast(pt);
        else
          return null;
      return data;
    }
    if (i > 0)
      return viewer.getAtomPointVector(atomExpressionAt(i));
    return null;
  }

  private P3 getObjectCenter(String axisID, int index, int modelIndex) {
    Object[] data = new Object[] { axisID, Integer.valueOf(index),
        Integer.valueOf(modelIndex) };
    return (getShapePropertyData(JC.SHAPE_DRAW, "getCenter", data)
        || getShapePropertyData(JC.SHAPE_ISOSURFACE, "getCenter", data)
        || getShapePropertyData(JC.SHAPE_PMESH, "getCenter", data)
        || getShapePropertyData(JC.SHAPE_CONTACT, "getCenter", data)
        || getShapePropertyData(JC.SHAPE_MO, "getCenter", data) ? (P3) data[2]
        : null);
  }

  private P3[] getObjectBoundingBox(String id) {
    Object[] data = new Object[] { id, null, null };
    return (getShapePropertyData(JC.SHAPE_ISOSURFACE, "getBoundingBox",
        data)
        || getShapePropertyData(JC.SHAPE_PMESH, "getBoundingBox", data)
        || getShapePropertyData(JC.SHAPE_CONTACT, "getBoundingBox", data)
        || getShapePropertyData(JC.SHAPE_MO, "getBoundingBox", data) ? (P3[]) data[2]
        : null);
  }

  private V3 getDrawObjectAxis(String axisID, int index) {
    Object[] data = new Object[] { axisID, Integer.valueOf(index), null };
    return (getShapePropertyData(JC.SHAPE_DRAW, "getSpinAxis", data) ? (V3) data[2]
        : null);
  }

  private void script(int tok, String filename)
      throws ScriptException {
    boolean loadCheck = true;
    boolean isCheck = false;
    boolean doStep = false;
    int lineNumber = 0;
    int pc = 0;
    int lineEnd = 0;
    int pcEnd = 0;
    int i = 2;
    String theScript = null;
    String localPath = null;
    String remotePath = null;
    String scriptPath = null;
    JmolList<SV> params = null;
    
    if (tok == T.javascript) {
      checkLength(2);
      if (!chk)
        viewer.jsEval(parameterAsString(1));
      return;
    }
    if (filename == null) {
      tok = tokAt(1);
      if (tok != T.string)
        error(ERROR_filenameExpected);
      filename = parameterAsString(1);
      if (filename.equalsIgnoreCase("applet")) {
        // script APPLET x "....."
        String appID = parameterAsString(2);
        theScript = parameterExpressionString(3, 0); // had _script variable??
        checkLast(iToken);
        if (chk)
          return;
        if (appID.length() == 0 || appID.equals("all"))
          appID = "*";
        if (!appID.equals(".")) {
          viewer.jsEval(appID + "\1" + theScript);
          if (!appID.equals("*"))
            return;
        }
      } else {
        tok = tokAt(slen - 1);
        doStep = (tok == T.step);
        if (filename.equalsIgnoreCase("inline")) {
          theScript = parameterExpressionString(2,
              (doStep ? slen - 1 : 0));
          i = iToken + 1;
        }
        while (filename.equalsIgnoreCase("localPath")
            || filename.equalsIgnoreCase("remotePath")
            || filename.equalsIgnoreCase("scriptPath")) {
          if (filename.equalsIgnoreCase("localPath"))
            localPath = parameterAsString(i++);
          else if (filename.equalsIgnoreCase("scriptPath"))
            scriptPath = parameterAsString(i++);
          else
            remotePath = parameterAsString(i++);
          filename = parameterAsString(i++);
        }
        if ((tok = tokAt(i)) == T.check) {
          isCheck = true;
          tok = tokAt(++i);
        }
        if (tok == T.noload) {
          loadCheck = false;
          tok = tokAt(++i);
        }
        if (tok == T.line || tok == T.lines) {
          i++;
          lineEnd = lineNumber = Math.max(intParameter(i++), 0);
          if (checkToken(i)) {
            if (getToken(i).tok == T.minus)
              lineEnd = (checkToken(++i) ? intParameter(i++) : 0);
            else
              lineEnd = -intParameter(i++);
            if (lineEnd <= 0)
              error(ERROR_invalidArgument);
          }
        } else if (tok == T.command || tok == T.commands) {
          i++;
          pc = Math.max(intParameter(i++) - 1, 0);
          pcEnd = pc + 1;
          if (checkToken(i)) {
            if (getToken(i).tok == T.minus)
              pcEnd = (checkToken(++i) ? intParameter(i++) : 0);
            else
              pcEnd = -intParameter(i++);
            if (pcEnd <= 0)
              error(ERROR_invalidArgument);
          }
        }
        if (tokAt(i) == T.leftparen) {
          params = parameterExpressionList(i, -1, false);
          i = iToken + 1;
        }
        checkLength(doStep ? i + 1 : i);
      }
    }

    // processing

    if (chk && !isCmdLine_c_or_C_Option)
      return;
    if (isCmdLine_c_or_C_Option)
      isCheck = true;
    boolean wasSyntaxCheck = chk;
    boolean wasScriptCheck = isCmdLine_c_or_C_Option;
    if (isCheck)
      chk = isCmdLine_c_or_C_Option = true;
    pushContext(null);
    contextPath += " >> " + filename;
    if (theScript == null ? compileScriptFileInternal(filename, localPath,
        remotePath, scriptPath) : compileScript(null, theScript, false)) {
      this.pcEnd = pcEnd;
      this.lineEnd = lineEnd;
      while (pc < lineNumbers.length && lineNumbers[pc] < lineNumber)
        pc++;
      this.pc = pc;
      boolean saveLoadCheck = isCmdLine_C_Option;
      isCmdLine_C_Option &= loadCheck;
      executionStepping |= doStep;
      
      contextVariables = new Hashtable<String, SV>();
      contextVariables.put("_arguments", (params == null ? SV.getVariableAI(new int[]{})
          : SV.getVariableList(params)));
      
      if (isCheck)
        listCommands = true;
      dispatchCommands(false, false);
      isCmdLine_C_Option = saveLoadCheck;
      popContext(false, false);
    } else {
      Logger.error(GT._("script ERROR: ") + errorMessage);
      popContext(false, false);
      if (wasScriptCheck) {
        setErrorMessage(null);
      } else {
        evalError(null, null);
      }
    }

    chk = wasSyntaxCheck;
    isCmdLine_c_or_C_Option = wasScriptCheck;
  }

  private void function() throws ScriptException {
    if (chk && !isCmdLine_c_or_C_Option)
      return;
    String name = (String) getToken(0).value;
    if (!viewer.isFunction(name))
      error(ERROR_commandExpected);
    JmolList<SV> params = (slen == 1 || slen == 3
        && tokAt(1) == T.leftparen && tokAt(2) == T.rightparen ? null
        : parameterExpressionList(1, -1, false));
    if (chk)
      return;
    runFunctionRet(null, name, params, null, false, true, true);
  }

  private void sync() throws ScriptException {
    // new 11.3.9
    checkLength(-3);
    String text = "";
    String applet = "";
    switch (slen) {
    case 1:
      applet = "*";
      text = "ON";
      break;
    case 2:
      applet = parameterAsString(1);
      if (applet.indexOf("jmolApplet") == 0 || Parser.isOneOf(applet, "*;.;^")) {
        text = "ON";
        if (!chk)
          viewer.syncScript(text, applet, 0);
        applet = ".";
        break;
      }
      if (tokAt(1) == T.integer) {
        // start/stop server on port <nnnn>
        if (!chk)
          viewer.syncScript(null, null, intParameter(1));
        return;
      }
      text = applet;
      applet = "*";
      break;
    case 3:
      if (chk)
        return;
      applet = parameterAsString(1);
      text = (tokAt(2) == T.stereo ? Viewer.SYNC_GRAPHICS_MESSAGE
          : parameterAsString(2));
      if (tokAt(1) == T.integer) {
        // send to server on port <nnnn>
        viewer.syncScript(text, null, intParameter(1));
        return;
      }
      break;
    }
    if (chk)
      return;
    viewer.syncScript(text, applet, 0);
  }

  private void history(int pt) throws ScriptException {
    // history or set history
    if (slen == 1) {
      // show it
      showString(viewer.getSetHistory(Integer.MAX_VALUE));
      return;
    }
    if (pt == 2) {
      // set history n; n' = -2 - n; if n=0, then set history OFF
      int n = intParameter(checkLast(2));
      if (n < 0)
        error(ERROR_invalidArgument);
      if (!chk)
        viewer.getSetHistory(n == 0 ? 0 : -2 - n);
      return;
    }
    switch (getToken(checkLast(1)).tok) {
    // pt = 1 history ON/OFF/CLEAR
    case T.on:
    case T.clear:
      if (!chk)
        viewer.getSetHistory(Integer.MIN_VALUE);
      return;
    case T.off:
      if (!chk)
        viewer.getSetHistory(0);
      break;
    default:
      errorStr(ERROR_keywordExpected, "ON, OFF, CLEAR");
    }
  }

  private void display(boolean isDisplay) throws ScriptException {
    BS bs = null;
    Boolean addRemove = null;
    int i = 1;
    int tok;
    switch (tok = tokAt(1)) {
    case T.add:
    case T.remove:
      addRemove = Boolean.valueOf(tok == T.add);
      tok = tokAt(++i);
      break;
    }
    boolean isGroup = (tok == T.group);
    if (isGroup)
      tok = tokAt(++i);
    switch (tok) {
    case T.dollarsign:
      setObjectProperty();
      return;
    case T.nada:
      break;
    default:
      if (slen == 4 && tokAt(2) == T.bonds)
        bs = new BondSet(BSUtil.newBitSet2(0, viewer.modelSet
            .bondCount));
      else
        bs = atomExpressionAt(i);
    }
    if (chk)
      return;
    if (bs instanceof BondSet) {
      viewer.displayBonds((BondSet) bs, isDisplay);
      return;
    }
    viewer.displayAtoms(bs, isDisplay, isGroup, addRemove, tQuiet);
  }

  private void delete() throws ScriptException {
    if (slen == 1) {
      zap(true);
      return;
    }
    if (tokAt(1) == T.dollarsign) {
      setObjectProperty();
      return;
    }
    BS bs = atomExpression(st, 1, 0, true, false, true, false);
    if (chk)
      return;
    int nDeleted = viewer.deleteAtoms(bs, false);
    if (!(tQuiet || scriptLevel > scriptReportingLevel))
      scriptStatusOrBuffer(GT._("{0} atoms deleted", nDeleted));
  }

  private void minimize() throws ScriptException {
    BS bsSelected = null;
    int steps = Integer.MAX_VALUE;
    float crit = 0;
    boolean addHydrogen = false;
    boolean isSilent = false;
    BS bsFixed = null;
    MinimizerInterface minimizer = viewer.getMinimizer(false);
    // may be null
    for (int i = 1; i < slen; i++)
      switch (getToken(i).tok) {
      case T.addhydrogens:
        addHydrogen = true;
        continue;
      case T.cancel:
      case T.stop:
        checkLength(2);
        if (chk || minimizer == null)
          return;
        minimizer.setProperty(parameterAsString(i), null);
        return;
      case T.clear:
        checkLength(2);
        if (chk || minimizer == null)
          return;
        minimizer.setProperty("clear", null);
        return;
      case T.constraint:
        if (i != 1)
          error(ERROR_invalidArgument);
        int n = 0;
        float targetValue = 0;
        int[] aList = new int[5];
        if (tokAt(++i) == T.clear) {
          checkLength(3);
        } else {
          while (n < 4 && !isFloatParameter(i)) {
            aList[++n] = atomExpressionAt(i).nextSetBit(0);
            i = iToken + 1;
          }
          aList[0] = n;
          if (n == 1)
            error(ERROR_invalidArgument);
          targetValue = floatParameter(checkLast(i));
        }
        if (!chk)
          viewer.getMinimizer(true).setProperty("constraint",
              new Object[] { aList, new int[n], Float.valueOf(targetValue) });
        return;
      case T.criterion:
        crit = floatParameter(++i);
        continue;
      case T.energy:
        steps = 0;
        continue;
      case T.fixed:
        if (i != 1)
          error(ERROR_invalidArgument);
        bsFixed = atomExpressionAt(++i);
        if (bsFixed.nextSetBit(0) < 0)
          bsFixed = null;
        i = iToken;
        if (!chk)
          viewer.getMinimizer(true).setProperty("fixed", bsFixed);
        if (i + 1 == slen)
          return;
        continue;
      case T.select:
        bsSelected = atomExpressionAt(++i);
        i = iToken;
        continue;
      case T.silent:
        isSilent = true;
        break;
      case T.step:
        steps = intParameter(++i);
        continue;
      default:
        error(ERROR_invalidArgument);
        break;
      }
    if (!chk)
      viewer.minimize(steps, crit, bsSelected, bsFixed, 0, addHydrogen,
          isSilent, false);
  }

  private void select(int i) throws ScriptException {
    // NOTE this is called by restrict()
    if (slen == 1) {
      viewer.select(null, false, null, tQuiet
          || scriptLevel > scriptReportingLevel);
      return;
    }
    if (slen == 2 && tokAt(1) == T.only)
      return; // coming from "cartoon only"
    // select beginexpr none endexpr
    viewer.setNoneSelected(slen == 4 && tokAt(2) == T.none);
    // select beginexpr bonds ( {...} ) endex pr
    if (tokAt(2) == T.bitset && getToken(2).value instanceof BondSet
        || getToken(2).tok == T.bonds && getToken(3).tok == T.bitset) {
      if (slen == iToken + 2) {
        if (!chk)
          viewer.selectBonds((BS) theToken.value);
        return;
      }
      error(ERROR_invalidArgument);
    }
    if (getToken(2).tok == T.measure) {
      if (slen == 5 && getToken(3).tok == T.bitset) {
        if (!chk)
          setShapeProperty(JC.SHAPE_MEASURES, "select",
              theToken.value);
        return;
      }
      error(ERROR_invalidArgument);
    }
    BS bs = null;
    Boolean addRemove = null;
    boolean isGroup = false;
    if (getToken(1).intValue == 0) {
      Object v = parameterExpressionToken(0).value;
      if (!(v instanceof BS))
        error(ERROR_invalidArgument);
      checkLast(iToken);
      bs = (BS) v;
    } else {
      int tok = tokAt(i);
      switch (tok) {
      case T.add:
      case T.remove:
        addRemove = Boolean.valueOf(tok == T.add);
        tok = tokAt(++i);
      }
      isGroup = (tok == T.group);
      if (isGroup)
        tok = tokAt(++i);
      bs = atomExpressionAt(i);
    }
    if (chk)
      return;
    if (isBondSet) {
      viewer.selectBonds(bs);
    } else {
      if (bs.length() > viewer.getAtomCount()) {
        BS bs1 = viewer.getModelUndeletedAtomsBitSet(-1);
        bs1.and(bs);
        bs = bs1;
      }
      viewer.select(bs, isGroup, addRemove, tQuiet
          || scriptLevel > scriptReportingLevel);
    }
  }

  private void subset() throws ScriptException {
    BS bs = null;
    if (!chk)
      viewer.setSelectionSubset(null);
    if (slen != 1
        && (slen != 4 || !getToken(2).value.equals("off")))
      bs = atomExpressionAt(1);
    if (!chk)
      viewer.setSelectionSubset(bs);
  }

  private void invertSelected() throws ScriptException {
    // invertSelected POINT
    // invertSelected PLANE
    // invertSelected HKL
    // invertSelected STEREO {sp3Atom} {one or two groups)
    P3 pt = null;
    Point4f plane = null;
    BS bs = null;
    int iAtom = Integer.MIN_VALUE;
    switch (tokAt(1)) {
    case T.nada:
      if (chk)
        return;
      bs = viewer.getSelectionSet(false);
      pt = viewer.getAtomSetCenter(bs);
      viewer.invertAtomCoordPt(pt, bs);
      return;
    case T.stereo:
      iAtom = atomExpressionAt(2).nextSetBit(0);
      // and only these:
      bs = atomExpressionAt(iToken + 1);
      break;
    case T.point:
      pt = centerParameter(2);
      break;
    case T.plane:
      plane = planeParameter(2);
      break;
    case T.hkl:
      plane = hklParameter(2);
      break;
    }
    checkLengthErrorPt(iToken + 1, 1);
    if (plane == null && pt == null && iAtom == Integer.MIN_VALUE)
      error(ERROR_invalidArgument);
    if (chk)
      return;
    if (iAtom == -1)
      return;
    viewer.invertSelected(pt, plane, iAtom, bs);
  }

  private void translate(boolean isSelected) throws ScriptException {
    // translate [selected] X|Y|Z x.x [NM|ANGSTROMS]
    // translate [selected] X|Y x.x%
    // translate [selected] X|Y|Z x.x [NM|ANGSTROMS]
    // translate [selected] X|Y x.x%
    // translate {x y z} [{atomExpression}]
    BS bs = null;
    int i = 1;
    int i0 = 0;
    if (tokAt(1) == T.selected) {
      isSelected = true;
      i0 = 1;
      i = 2;
    }
    if (isPoint3f(i)) {
      P3 pt = getPoint3f(i, true);
      bs = (!isSelected && iToken + 1 < slen ? atomExpressionAt(++iToken)
          : null);
      checkLast(iToken);
      if (!chk)
        viewer.setAtomCoordRelative(pt, bs);
      return;
    }
    char xyz = parameterAsString(i).toLowerCase().charAt(0);
    if ("xyz".indexOf(xyz) < 0)
      error(ERROR_axisExpected);
    float amount = floatParameter(++i);
    char type;
    switch (tokAt(++i)) {
    case T.nada:
    case T.bitset:
    case T.expressionBegin:
      type = '\0';
      break;
    default:
      type = (optParameterAsString(i).toLowerCase() + '\0').charAt(0);
    }
    if (amount == 0 && type != '\0')
      return;
    iToken = i0 + (type == '\0' ? 2 : 3);
    bs = (isSelected ? viewer.getSelectionSet(false)
        : iToken + 1 < slen ? atomExpressionAt(++iToken) : null);
    checkLast(iToken);
    if (!chk)
      viewer.translate(xyz, amount, type, bs);
  }

  private void zap(boolean isZapCommand) throws ScriptException {
    if (slen == 1 || !isZapCommand) {
      viewer.zap(true, isZapCommand && !isStateScript, true);
      refresh();
      return;
    }
    BS bs = atomExpressionAt(1);
    if (chk)
      return;
    int nDeleted = viewer.deleteAtoms(bs, true);
    boolean isQuiet = (tQuiet || scriptLevel > scriptReportingLevel);
    if (!isQuiet)
      scriptStatusOrBuffer(GT._("{0} atoms deleted", nDeleted));
    viewer.select(null, false, null, isQuiet);
  }

  private void zoom(boolean isZoomTo) throws ScriptException {
    if (!isZoomTo) {
      // zoom
      // zoom on|off
      int tok = (slen > 1 ? getToken(1).tok : T.on);
      switch (tok) {
      case T.in:
      case T.out:
        break;
      case T.on:
      case T.off:
        if (slen > 2)
          error(ERROR_badArgumentCount);
        if (!chk)
          setBooleanProperty("zoomEnabled", tok == T.on);
        return;
      }
    }
    P3 center = null;
    //Point3f currentCenter = viewer.getRotationCenter();
    int i = 1;
    // zoomTo time-sec
    float floatSecondsTotal = (isZoomTo ? (isFloatParameter(i) ? floatParameter(i++) : 2f)
        : 0f);
    if (floatSecondsTotal < 0) {
      // zoom -10
      i--;
      floatSecondsTotal = 0;
    }
    // zoom {x y z} or (atomno=3)
    int ptCenter = 0;
    BS bsCenter = null;
    if (isCenterParameter(i)) {
      ptCenter = i;
      center = centerParameter(i);
      if (expressionResult instanceof BS)
        bsCenter = (BS) expressionResult;
      i = iToken + 1;
    } else if (tokAt(i) == T.integer && getToken(i).intValue == 0) {
      bsCenter = viewer.getAtomBitSet("visible");
      center = viewer.getAtomSetCenter(bsCenter);
    }

    // disabled sameAtom stuff -- just too weird
    boolean isSameAtom = false;// && (center != null && currentCenter.distance(center) < 0.1);
    // zoom/zoomTo [0|n|+n|-n|*n|/n|IN|OUT]
    // zoom/zoomTo percent|-factor|+factor|*factor|/factor | 0
    float zoom = viewer.getZoomSetting();

    float newZoom = getZoom(ptCenter, i, bsCenter, zoom);
    i = iToken + 1;
    float xTrans = Float.NaN;
    float yTrans = Float.NaN;
    if (i != slen) {
      xTrans = floatParameter(i++);
      yTrans = floatParameter(i++);
    }
    if (i != slen)
      error(ERROR_invalidArgument);
    if (newZoom < 0) {
      newZoom = -newZoom; // currentFactor
      if (isZoomTo) {
        // no factor -- check for no center (zoom out) or same center (zoom in)
        if (slen == 1 || isSameAtom)
          newZoom *= 2;
        else if (center == null)
          newZoom /= 2;
      }
    }
    float max = viewer.getMaxZoomPercent();
    if (newZoom < 5 || newZoom > max)
      numberOutOfRange(5, max);
    if (!viewer.isWindowCentered()) {
      // do a smooth zoom only if not windowCentered
      if (center != null) {
        BS bs = atomExpressionAt(ptCenter);
        if (!chk)
          viewer.setCenterBitSet(bs, false);
      }
      center = viewer.getRotationCenter();
      if (Float.isNaN(xTrans))
        xTrans = viewer.getTranslationXPercent();
      if (Float.isNaN(yTrans))
        yTrans = viewer.getTranslationYPercent();
    }
    if (chk)
      return;
    if (Float.isNaN(xTrans))
      xTrans = 0;
    if (Float.isNaN(yTrans))
      yTrans = 0;
    if (isSameAtom && Math.abs(zoom - newZoom) < 1)
      floatSecondsTotal = 0;
    viewer.moveTo(this, floatSecondsTotal, center, JC.center, Float.NaN, null,
        newZoom, xTrans, yTrans, Float.NaN, null, Float.NaN, Float.NaN, Float.NaN);
    if (isJS && floatSecondsTotal > 0 && viewer.waitForMoveTo())
      throw new ScriptInterruption(this, "zoomTo", 1);

  }

  private float getZoom(int ptCenter, int i, BS bs, float currentZoom)
      throws ScriptException {
    // where [zoom factor] is [0|n|+n|-n|*n|/n|IN|OUT]

    float zoom = (isFloatParameter(i) ? floatParameter(i++) : Float.NaN);
    if (zoom == 0 || currentZoom == 0) {
      // moveTo/zoom/zoomTo {center} 0
      float r = Float.NaN;
      if (bs == null) {
        if (tokAt(ptCenter) == T.dollarsign) {
          P3[] bbox = getObjectBoundingBox(objectNameParameter(ptCenter + 1));
          if (bbox == null || (r = bbox[0].distance(bbox[1]) / 2) == 0)
            error(ERROR_invalidArgument);
        }
      } else {
        r = viewer.calcRotationRadiusBs(bs);       
      }
      if (Float.isNaN(r))
        error(ERROR_invalidArgument);        
      currentZoom = viewer.getRotationRadius() / r * 100;
      zoom = Float.NaN;
    }
    if (zoom < 0) {
      // moveTo/zoom/zoomTo -factor
      zoom += currentZoom;
    } else if (Float.isNaN(zoom)) {
      // moveTo/zoom/zoomTo [optional {center}] percent|+factor|*factor|/factor
      // moveTo/zoom/zoomTo {center} 0 [optional
      // -factor|+factor|*factor|/factor]
      int tok = tokAt(i);
      switch (tok) {
      case T.out:
      case T.in:
        zoom = currentZoom * (tok == T.out ? 0.5f : 2f);
        i++;
        break;
      case T.divide:
      case T.times:
      case T.plus:
        float value = floatParameter(++i);
        i++;
        switch (tok) {
        case T.divide:
          zoom = currentZoom / value;
          break;
        case T.times:
          zoom = currentZoom * value;
          break;
        case T.plus:
          zoom = currentZoom + value;
          break;
        }
        break;
      default:
        // indicate no factor indicated
        zoom = (bs == null ? -currentZoom : currentZoom);
      }
    }
    iToken = i - 1;
    return zoom;
  }

  private void delay() throws ScriptException {
    int millis = 0;
    switch (getToken(1).tok) {
    case T.on: // this is auto-provided as a default
      millis = 1;
      break;
    case T.integer:
      millis = intParameter(1) * 1000;
      break;
    case T.decimal:
      millis = (int) (floatParameter(1) * 1000);
      break;
    default:
      error(ERROR_numberExpected);
    }
    if (chk || viewer.isHeadless() || viewer.autoExit)
      return;
    refresh();
    doDelay(Math.abs(millis));
  }

  private void slab(boolean isDepth) throws ScriptException {
    boolean TF = false;
    Point4f plane = null;
    String str;
    if (isCenterParameter(1) || tokAt(1) == T.point4f)
      plane = planeParameter(1);
    else
      switch (getToken(1).tok) {
      case T.integer:
        int percent = intParameter(checkLast(1));
        if (!chk)
          if (isDepth)
            viewer.depthToPercent(percent);
          else
            viewer.slabToPercent(percent);
        return;
      case T.on:
        checkLength(2);
        TF = true;
        //$FALL-THROUGH$
      case T.off:
        checkLength(2);
        setBooleanProperty("slabEnabled", TF);
        return;
      case T.reset:
        checkLength(2);
        if (chk)
          return;
        viewer.slabReset();
        setBooleanProperty("slabEnabled", true);
        return;
      case T.set:
        checkLength(2);
        if (chk)
          return;
        viewer.setSlabDepthInternal(isDepth);
        setBooleanProperty("slabEnabled", true);
        return;
      case T.minus:
        str = parameterAsString(2);
        if (str.equalsIgnoreCase("hkl"))
          plane = hklParameter(3);
        else if (str.equalsIgnoreCase("plane"))
          plane = planeParameter(3);
        if (plane == null)
          error(ERROR_invalidArgument);
        plane.scale(-1);
        break;
      case T.plane:
        switch (getToken(2).tok) {
        case T.none:
          break;
        default:
          plane = planeParameter(2);
        }
        break;
      case T.hkl:
        plane = (getToken(2).tok == T.none ? null : hklParameter(2));
        break;
      case T.reference:
        // only in 11.2; deprecated
        return;
      default:
        error(ERROR_invalidArgument);
      }
    if (!chk)
      viewer.slabInternal(plane, isDepth);
  }

  /*
  private void slice() throws ScriptException{
    if(!chk && viewer.slicer==null){
     viewer.createSlicer();
    }
    int tok1 = getToken(1).tok;
    if(tok1==Token.left||tok1==Token.right){
      switch (getToken(2).tok){
      case Token.on:
        if(chk) return;
        viewer.slicer.drawSlicePlane(tok1, true);
        return;
      case Token.off:
        if(chk) return;
        viewer.slicer.drawSlicePlane(tok1, false);
        return;
      default:
        error(ERROR_invalidArgument);
      break;
      }
    }else{//command to slice object, not show slice planes
      String name = (String)getToken(1).value;
      //TODO - should accept "all"  for now "all" will fail silently.
      // Should check it is a valid  isosurface name
      //Should be followed by two angles, and two percents (float values)
      float[] param = new float[4];
      for (int i=2;i<6;++i){
        if(getToken(i).tok == Token.decimal){
          param[i-2]=floatParameter(i);
        } else{
          error(ERROR_invalidArgument);  
        }
      }
      if(!chk){
        viewer.slicer.setSlice(param[0], param[1], param[2], param[3]);
        viewer.slicer.sliceObject(name);
      }
      return; 
    }
  }
  
  */

  private void ellipsoid() throws ScriptException {
    int mad = 0;
    int i = 1;
    switch (getToken(1).tok) {
    case T.on:
      mad = 50;
      break;
    case T.off:
      break;
    case T.integer:
      mad = intParameter(1);
      break;
    case T.set:
      checkLength(3);
      sm.loadShape(JC.SHAPE_ELLIPSOIDS);
      setShapeProperty(JC.SHAPE_ELLIPSOIDS, "select", Integer.valueOf(intParameterRange(2, 1, 3)));      
      return;
    case T.id:
    case T.times:
    case T.identifier:
      sm.loadShape(JC.SHAPE_ELLIPSOIDS);
      if (theTok == T.id)
        i++;
      setShapeId(JC.SHAPE_ELLIPSOIDS, i, false);
      i = iToken;
      while (++i < slen) {
        String key = parameterAsString(i);
        Object value = null;
        switch (tokAt(i)) {
        case T.dollarsign:
          key = "points";
          Object[] data = new Object[3];
          data[0] = objectNameParameter(++i);
          if (chk)
            continue;
          getShapePropertyData(JC.SHAPE_ISOSURFACE, "getVertices", data);
          value = data;
          break;
        case T.bitset:
        case T.expressionBegin:
          key = "atoms";
          value = atomExpressionAt(i);
          i = iToken;
          break;
        case T.axes:
          V3[] axes = new V3[3];
          for (int j = 0; j < 3; j++) {
            axes[j] = new V3();
            axes[j].setT(centerParameter(++i));
            i = iToken;
          }
          value = axes;
          break;
        case T.center:
          value = centerParameter(++i);
          i = iToken;
          break;
        case T.color:
          float translucentLevel = Float.NaN;
          if (tokAt(i) == T.color)
            i++;
          if ((theTok = tokAt(i)) == T.translucent) {
            value = "translucent";
            if (isFloatParameter(++i))
              translucentLevel = getTranslucentLevel(i++);
            else
              translucentLevel = viewer.getDefaultTranslucent();
          } else if (theTok == T.opaque) {
            value = "opaque";
            i++;
          }
          if (isColorParam(i)) {
            setShapeProperty(JC.SHAPE_ELLIPSOIDS, "color", Integer
                .valueOf(getArgbParam(i)));
            i = iToken;
          }
          if (value == null)
            continue;
          if (!Float.isNaN(translucentLevel))
            setShapeProperty(JC.SHAPE_ELLIPSOIDS,
                "translucentLevel", Float.valueOf(translucentLevel));
          key = "translucency";
          break;
        case T.delete:
          value = Boolean.TRUE;
          checkLength(3);
          break;
        case T.modelindex:
          value = Integer.valueOf(intParameter(++i));
          break;
        case T.on:
          value = Boolean.TRUE;
          break;
        case T.off:
          key = "on";
          value = Boolean.FALSE;
          break;
        case T.scale:
          value = Float.valueOf(floatParameter(++i));
          break;
        }
        if (value == null)
          error(ERROR_invalidArgument);
        setShapeProperty(JC.SHAPE_ELLIPSOIDS, key.toLowerCase(),
            value);
      }
      setShapeProperty(JC.SHAPE_ELLIPSOIDS, "thisID", null);
      return;
    default:
      error(ERROR_invalidArgument);
    }
    setShapeSizeBs(JC.SHAPE_ELLIPSOIDS, mad, null);
  }

  private String getShapeNameParameter(int i) throws ScriptException {
    String id = parameterAsString(i);
    boolean isWild = id.equals("*");
    if (id.length() == 0)
      error(ERROR_invalidArgument);
    if (isWild) {
      switch (tokAt(i + 1)) {
      case T.nada:
      case T.on:
      case T.off:
      case T.displayed:
      case T.hidden:
      case T.color:
      case T.delete:
        break;
      default:
        if (setMeshDisplayProperty(-1, 0, tokAt(i + 1)))
          break;
        id += optParameterAsString(++i);
      }
    }
    if (tokAt(i + 1) == T.times)
      id += parameterAsString(++i);
    iToken = i;
    return id;
  }

  private String setShapeId(int iShape, int i, boolean idSeen)
      throws ScriptException {
    if (idSeen)
      error(ERROR_invalidArgument);
    String name = getShapeNameParameter(i).toLowerCase();
    setShapeProperty(iShape, "thisID", name);
    return name;
  }

  private void setAtomShapeSize(int shape, float scale) throws ScriptException {
    // halo star spacefill
    RadiusData rd = null;
    int tok = tokAt(1);
    boolean isOnly = false;
    switch (tok) {
    case T.only:
      restrictSelected(false, false);
      break;
    case T.on:
      break;
    case T.off:
      scale = 0;
      break;
    case T.decimal:
      isOnly = (floatParameter(1) < 0);
      //$FALL-THROUGH$
    case T.integer:
    default:
      rd = encodeRadiusParameter(1, isOnly, true);
      if (Float.isNaN(rd.value))
        error(ERROR_invalidArgument);
    }
    if (rd == null)
      rd = new RadiusData(null, scale, EnumType.FACTOR, EnumVdw.AUTO);
    if (isOnly)
      restrictSelected(false, false);
    setShapeSize(shape, rd);
  }

  /*
   * Based on the form of the parameters, returns and encoded radius as follows:
   * 
   * script meaning range
   * 
   * +1.2 offset [0 - 10] 
   * -1.2 offset 0) 
   * 1.2 absolute (0 - 10] 
   * -30% 70% (-100 - 0) 
   * +30% 130% (0 
   * 80% percent (0
   */

  private RadiusData encodeRadiusParameter(int index, boolean isOnly,
                                           boolean allowAbsolute)
      throws ScriptException {

    float value = Float.NaN;
    EnumType factorType = EnumType.ABSOLUTE;
    EnumVdw vdwType = null;

    int tok = (index == -1 ? T.vanderwaals : getToken(index).tok);
    switch (tok) {
    case T.adpmax:
    case T.adpmin:
    case T.ionic:
    case T.hydrophobic:
    case T.temperature:
    case T.vanderwaals:
      value = 1;
      factorType = EnumType.FACTOR;
      vdwType = (tok == T.vanderwaals ? null : EnumVdw.getVdwType2(T.nameOf(tok)));
      tok = tokAt(++index);
      break;
    }
    switch (tok) {
    case T.reset:
      return viewer.getDefaultRadiusData();
    case T.auto:
    case T.rasmol:
    case T.babel:
    case T.babel21:
    case T.jmol:
      value = 1;
      factorType = EnumType.FACTOR;
      iToken = index - 1;
      break;
    case T.plus:
    case T.integer:
    case T.decimal:
      if (tok == T.plus) {
        index++;
      } else if (tokAt(index + 1) == T.percent) {
        value = Math.round(floatParameter(index));
        iToken = ++index;
        factorType = EnumType.FACTOR;
        if (value < 0 || value > 200)
          integerOutOfRange(0, 200);
        value /= 100;
        break;
      } else if (tok == T.integer) {
        value = intParameter(index);
        // rasmol 250-scale if positive or percent (again), if negative
        // (deprecated)
        if (value > 749 || value < -200)
          integerOutOfRange(-200, 749);
        if (value > 0) {
          value /= 250;
          factorType = EnumType.ABSOLUTE;
        } else {
          value /= -100;
          factorType = EnumType.FACTOR;
        }
        break;
      }
      value = floatParameterRange(index,
          (isOnly || !allowAbsolute ? -Atom.RADIUS_MAX : 0), Atom.RADIUS_MAX);
      if (tok == T.plus || !allowAbsolute) {
        factorType = EnumType.OFFSET;
      } else {
        factorType = EnumType.ABSOLUTE;
        vdwType = EnumVdw.NADA;
      }
      if (isOnly)
        value = -value;
      break;
    default:
      if (value == 1)
        index--;
    }
    if (vdwType == null) {
      vdwType = EnumVdw.getVdwType(optParameterAsString(++iToken));
      if (vdwType == null) {
        iToken = index;
        vdwType = EnumVdw.AUTO;
      }
    }
    return new RadiusData(null, value, factorType, vdwType);
  }

  private void structure() throws ScriptException {
    EnumStructure type = EnumStructure.getProteinStructureType(parameterAsString(1));
    if (type == EnumStructure.NOT)
      error(ERROR_invalidArgument);
    BS bs = null;
    switch (tokAt(2)) {
    case T.bitset:
    case T.expressionBegin:
      bs = atomExpressionAt(2);
      checkLast(iToken);
      break;
    default:
      checkLength(2);
    }
    if (chk)
      return;
    clearDefinedVariableAtomSets();
    viewer.setProteinType(type, bs);
  }

  private void wireframe() throws ScriptException {
    int mad = Integer.MIN_VALUE;
    if (tokAt(1) == T.reset)
      checkLast(1);
    else
      mad = getMadParameter();
    if (chk)
      return;
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_COVALENT_MASK));
    setShapeSizeBs(
        JC.SHAPE_STICKS,
        mad == Integer.MIN_VALUE ? 2 * JC.DEFAULT_BOND_MILLIANGSTROM_RADIUS
            : mad, null);
  }

  private void ssbond() throws ScriptException {
    int mad = getMadParameter();
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_SULFUR_MASK));
    setShapeSizeBs(JC.SHAPE_STICKS, mad, null);
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_COVALENT_MASK));
  }

  private void struts() throws ScriptException {
    boolean defOn = (tokAt(1) == T.only || tokAt(1) == T.on || slen == 1);
    int mad = getMadParameter();
    if (defOn)
      mad = Math.round (viewer.getStrutDefaultRadius() * 2000f);
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_STRUT));
    setShapeSizeBs(JC.SHAPE_STICKS, mad, null);
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_COVALENT_MASK));
  }

  private void hbond() throws ScriptException {
    if (slen == 2 && getToken(1).tok == T.calculate) {
      if (chk)
        return;
      int n = viewer.autoHbond(null, null, false);
      scriptStatusOrBuffer(GT._("{0} hydrogen bonds", Math.abs(n)));
      return;
    }
    if (slen == 2 && getToken(1).tok == T.delete) {
      if (chk)
        return;
      connect(0);
      return;
    }
    int mad = getMadParameter();
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_HYDROGEN_MASK));
    setShapeSizeBs(JC.SHAPE_STICKS, mad, null);
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_COVALENT_MASK));
  }

  private void configuration() throws ScriptException {
    // if (!chk && viewer.getDisplayModelIndex() <= -2)
    // error(ERROR_backgroundModelError, "\"CONFIGURATION\"");
    BS bsAtoms;
    if (slen == 1) {
      bsAtoms = viewer.setConformation();
      viewer.addStateScriptRet("select", null, viewer.getSelectionSet(false),
          null, "configuration", true, false);
    } else {
      int n = intParameter(checkLast(1));
      if (chk)
        return;
      bsAtoms = viewer.getConformation(viewer.getCurrentModelIndex(), n - 1,
          true);
      viewer.addStateScript("configuration " + n + ";", true, false);
    }
    if (chk)
      return;
    setShapeProperty(JC.SHAPE_STICKS, "type", Integer
        .valueOf(JmolEdge.BOND_HYDROGEN_MASK));
    setShapeSizeBs(JC.SHAPE_STICKS, 0, bsAtoms);
    viewer.autoHbond(bsAtoms, bsAtoms, true);
    viewer.select(bsAtoms, false, null, tQuiet);
  }

  private void vector() throws ScriptException {
    EnumType type = EnumType.SCREEN;
    float value = 1;
    checkLength(-3);
    switch (iToken = slen) {
    case 1:
      break;
    case 2:
      switch (getToken(1).tok) {
      case T.on:
        break;
      case T.off:
        value = 0;
        break;
      case T.integer:
        // diameter Pixels
        value = intParameterRange(1, 0, 19);
        break;
      case T.decimal:
        // radius angstroms
        type = EnumType.ABSOLUTE;
        value = floatParameterRange(1, 0, 3);
        break;
      default:
        error(ERROR_booleanOrNumberExpected);
      }
      break;
    case 3:
      if (tokAt(1) == T.scale) {
        setFloatProperty("vectorScale", floatParameterRange(2, -100, 100));
        return;
      }
    }
    setShapeSize(JC.SHAPE_VECTORS, new RadiusData(null, value, type, null));
  }

  private void dipole() throws ScriptException {
    // dipole intWidth floatMagnitude OFFSET floatOffset {atom1} {atom2}
    String propertyName = null;
    Object propertyValue = null;
    boolean iHaveAtoms = false;
    boolean iHaveCoord = false;
    boolean idSeen = false;

    sm.loadShape(JC.SHAPE_DIPOLES);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_DIPOLES))
      return;
    setShapeProperty(JC.SHAPE_DIPOLES, "init", null);
    if (slen == 1) {
      setShapeProperty(JC.SHAPE_DIPOLES, "thisID", null);
      return;
    }
    for (int i = 1; i < slen; ++i) {
      propertyName = null;
      propertyValue = null;
      switch (getToken(i).tok) {
      case T.on:
        propertyName = "on";
        break;
      case T.off:
        propertyName = "off";
        break;
      case T.delete:
        propertyName = "delete";
        break;
      case T.integer:
      case T.decimal:
        propertyName = "value";
        propertyValue = Float.valueOf(floatParameter(i));
        break;
      case T.bitset:
        propertyName = "atomBitset";
        //$FALL-THROUGH$
      case T.expressionBegin:
        if (propertyName == null)
          propertyName = (iHaveAtoms || iHaveCoord ? "endSet" : "startSet");
        propertyValue = atomExpressionAt(i);
        i = iToken;
        iHaveAtoms = true;
        break;
      case T.leftbrace:
      case T.point3f:
        // {X, Y, Z}
        P3 pt = getPoint3f(i, true);
        i = iToken;
        propertyName = (iHaveAtoms || iHaveCoord ? "endCoord" : "startCoord");
        propertyValue = pt;
        iHaveCoord = true;
        break;
      case T.bonds:
        propertyName = "bonds";
        break;
      case T.calculate:
        propertyName = "calculate";
        break;
      case T.id:
        setShapeId(JC.SHAPE_DIPOLES, ++i, idSeen);
        i = iToken;
        break;
      case T.cross:
        propertyName = "cross";
        propertyValue = Boolean.TRUE;
        break;
      case T.nocross:
        propertyName = "cross";
        propertyValue = Boolean.FALSE;
        break;
      case T.offset:
        float v = floatParameter(++i);
        if (theTok == T.integer) {
          propertyName = "offsetPercent";
          propertyValue = Integer.valueOf((int) v);
        } else {
          propertyName = "offset";
          propertyValue = Float.valueOf(v);
        }
        break;
      case T.offsetside:
        propertyName = "offsetSide";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;

      case T.val:
        propertyName = "value";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.width:
        propertyName = "width";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      default:
        if (theTok == T.times || T.tokAttr(theTok, T.identifier)) {
          setShapeId(JC.SHAPE_DIPOLES, i, idSeen);
          i = iToken;
          break;
        }
        error(ERROR_invalidArgument);
      }
      idSeen = (theTok != T.delete && theTok != T.calculate);
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_DIPOLES, propertyName,
            propertyValue);
    }
    if (iHaveCoord || iHaveAtoms)
      setShapeProperty(JC.SHAPE_DIPOLES, "set", null);
  }

  private void animationMode() throws ScriptException {
    float startDelay = 1, endDelay = 1;
    if (slen > 5)
      error(ERROR_badArgumentCount);
    EnumAnimationMode animationMode = null;
    switch (getToken(2).tok) {
    case T.once:
      animationMode = EnumAnimationMode.ONCE;
      startDelay = endDelay = 0;
      break;
    case T.loop:
      animationMode = EnumAnimationMode.LOOP;
      break;
    case T.palindrome:
      animationMode = EnumAnimationMode.PALINDROME;
      break;
    default:
      error(ERROR_invalidArgument);
    }
    if (slen >= 4) {
      startDelay = endDelay = floatParameter(3);
      if (slen == 5)
        endDelay = floatParameter(4);
    }
    if (!chk)
      viewer.setAnimationReplayMode(animationMode, startDelay, endDelay);
  }

  private void vibration() throws ScriptException {
    checkLength(-3);
    float period = 0;
    switch (getToken(1).tok) {
    case T.on:
      checkLength(2);
      period = viewer.getVibrationPeriod();
      break;
    case T.off:
      checkLength(2);
      period = 0;
      break;
    case T.integer:
    case T.decimal:
      checkLength(2);
      period = floatParameter(1);
      break;
    case T.scale:
      setFloatProperty("vibrationScale", floatParameterRange(2, -10, 10));
      return;
    case T.period:
      setFloatProperty("vibrationPeriod", floatParameter(2));
      return;
    case T.identifier:
      error(ERROR_invalidArgument);
      break;
    default:
      period = -1;
    }
    if (period < 0)
      error(ERROR_invalidArgument);
    if (chk)
      return;
    if (period == 0) {
      viewer.setVibrationOff();
      return;
    }
    viewer.setVibrationPeriod(-period);
  }

  private void animationDirection() throws ScriptException {
    int i = 2;
    int direction = 0;
    switch (tokAt(i)) {
    case T.minus:
      direction = -intParameter(++i);
      break;
    case T.plus:
      direction = intParameter(++i);
      break;
    case T.integer:
      direction = intParameter(i);
      if (direction > 0)
        direction = 0;
      break;
    default:
      error(ERROR_invalidArgument);
    }
    checkLength(++i);
    if (direction != 1 && direction != -1)
      errorStr2(ERROR_numberMustBe, "-1", "1");
    if (!chk)
      viewer.setAnimationDirection(direction);
  }

  private void calculate() throws ScriptException {
    boolean isSurface = false;
    boolean asDSSP = false;
    BS bs;
    BS bs2 = null;
    int n = Integer.MIN_VALUE;
    if ((iToken = slen) >= 2) {
      clearDefinedVariableAtomSets();
      switch (getToken(1).tok) {
      case T.identifier:
        checkLength(2);
        break;
      case T.aromatic:
        checkLength(2);
        if (!chk)
          viewer.assignAromaticBonds();
        return;
      case T.hbond:
        if (slen == 2) {
          if (!chk) {
            n = viewer.autoHbond(null, null, false);
            break;
          }
          return;
        }
        BS bs1 = null;
        // calculate hbonds STRUCTURE -- only the DSSP structurally-defining H bonds
        asDSSP = (tokAt(++iToken) == T.structure);
        if (asDSSP)
          bs1 = viewer.getSelectionSet(false);
        else
          bs1 = atomExpressionAt(iToken);
        if (!asDSSP && !(asDSSP = (tokAt(++iToken) == T.structure)))
          bs2 = atomExpressionAt(iToken);
        if (!chk) {
          n = viewer.autoHbond(bs1, bs2, false);
          break;
        }
        return;
      case T.hydrogen:
        bs = (slen == 2 ? null : atomExpressionAt(2));
        checkLast(iToken);
        if (!chk)
          viewer.addHydrogens(bs, false, false);
        return;
      case T.partialcharge:
        iToken = 1;
        bs = (slen == 2 ? null : atomExpressionAt(2));
        checkLast(iToken);
        if (!chk)
          viewer.calculatePartialCharges(bs);
        return;
      case T.pointgroup:
        pointGroup();
        return;
      case T.straightness:
        checkLength(2);
        if (!chk) {
          viewer.calculateStraightness();
          viewer.addStateScript("set quaternionFrame '"
              + viewer.getQuaternionFrame() + "'; calculate straightness",
              false, true);
        }
        return;
      case T.structure:
        bs = (slen < 4 ? null : atomExpressionAt(2));
        switch (tokAt(++iToken)) {
        case T.ramachandran:
          break;
        case T.dssp:
          asDSSP = true;
          break;
        case T.nada:
          asDSSP = viewer.getDefaultStructureDSSP();
          break;
        default:
          error(ERROR_invalidArgument);
        }
        if (!chk)
          showString(viewer.calculateStructures(bs, asDSSP, true));
        return;
      case T.struts:
        bs = (iToken + 1 < slen ? atomExpressionAt(++iToken) : null);
        bs2 = (iToken + 1 < slen ? atomExpressionAt(++iToken) : null);
        checkLength(++iToken);
        if (!chk) {
          n = viewer.calculateStruts(bs, bs2);
          if (n > 0) {
            setShapeProperty(JC.SHAPE_STICKS, "type", Integer
                .valueOf(JmolEdge.BOND_STRUT));
            setShapePropertyBs(JC.SHAPE_STICKS, "color", Integer
                .valueOf(0x0FFFFFF), null);
            setShapeTranslucency(JC.SHAPE_STICKS, "", "translucent",
                0.5f, null);
            setShapeProperty(JC.SHAPE_STICKS, "type", Integer
                .valueOf(JmolEdge.BOND_COVALENT_MASK));
          }
          showString(GT._("{0} struts added", n));
        }
        return;
      case T.surface:
        isSurface = true;
        // deprecated
        //$FALL-THROUGH$
      case T.surfacedistance:
        /*
         * preferred:
         * 
         * calculate surfaceDistance FROM {...} calculate surfaceDistance WITHIN
         * {...}
         */
        boolean isFrom = false;
        switch (tokAt(2)) {
        case T.within:
          iToken++;
          break;
        case T.nada:
          isFrom = !isSurface;
          break;
        case T.from:
          isFrom = true;
          iToken++;
          break;
        default:
          isFrom = true;
        }
        bs = (iToken + 1 < slen ? atomExpressionAt(++iToken)
            : viewer.getSelectionSet(false));
        checkLength(++iToken);
        if (!chk)
          viewer.calculateSurface(bs, (isFrom ? Float.MAX_VALUE : -1));
        return;
      }
      if (n != Integer.MIN_VALUE) {
        scriptStatusOrBuffer(GT._("{0} hydrogen bonds", Math.abs(n)));
        return;
      }
    }
    errorStr2(
        ERROR_what,
        "CALCULATE",
        "aromatic? hbonds? hydrogen? partialCharge? pointgroup? straightness? structure? struts? surfaceDistance FROM? surfaceDistance WITHIN?");
  }

  private void pointGroup() throws ScriptException {
    switch (tokAt(0)) {
    case T.calculate:
      if (!chk)
        showString(viewer.calculatePointGroup());
      return;
    case T.show:
      if (!chk)
        showString(viewer.getPointGroupAsString(false, null, 0, 0));
      return;
    }
    // draw pointgroup [C2|C3|Cs|Ci|etc.] [n] [scale x]
    int pt = 2;
    String type = (tokAt(pt) == T.scale ? "" : optParameterAsString(pt));
    float scale = 1;
    int index = 0;
    if (type.length() > 0) {
      if (isFloatParameter(++pt))
        index = intParameter(pt++);
    }
    if (tokAt(pt) == T.scale)
      scale = floatParameter(++pt);
    if (!chk)
      runScript(viewer.getPointGroupAsString(true, type, index, scale));
  }

  private void dots(int iShape) throws ScriptException {
    if (!chk)
      sm.loadShape(iShape);
    setShapeProperty(iShape, "init", null);
    float value = Float.NaN;
    EnumType type = EnumType.ABSOLUTE;
    int ipt = 1;
    switch (getToken(ipt).tok) {
    case T.only:
      restrictSelected(false, false);
      value = 1;
      type = EnumType.FACTOR;
      break;
    case T.on:
      value = 1;
      type = EnumType.FACTOR;
      break;
    case T.off:
      value = 0;
      break;
    case T.integer:
      int dotsParam = intParameter(ipt);
      if (tokAt(ipt + 1) == T.radius) {
        ipt++;
        setShapeProperty(iShape, "atom", Integer.valueOf(dotsParam));
        setShapeProperty(iShape, "radius", Float.valueOf(floatParameter(++ipt)));
        if (tokAt(++ipt) == T.color) {
          setShapeProperty(iShape, "colorRGB", Integer
              .valueOf(getArgbParam(++ipt)));
          ipt++;
        }
        if (getToken(ipt).tok != T.bitset)
          error(ERROR_invalidArgument);
        setShapeProperty(iShape, "dots", st[ipt].value);
        return;
      }
      break;
    }
    RadiusData rd = (Float.isNaN(value) ? encodeRadiusParameter(ipt, false,
        true) : new RadiusData(null, value, type, EnumVdw.AUTO));
    if (Float.isNaN(rd.value))
      error(ERROR_invalidArgument);
    setShapeSize(iShape, rd);
  }

  private void proteinShape(int shapeType) throws ScriptException {
    int mad = 0;
    // token has ondefault1
    switch (getToken(1).tok) {
    case T.only:
      if (chk)
        return;
      restrictSelected(false, false);
      mad = -1;
      break;
    case T.on:
      mad = -1; // means take default
      break;
    case T.off:
      break;
    case T.structure:
      mad = -2;
      break;
    case T.temperature:
    case T.displacement:
      mad = -4;
      break;
    case T.integer:
      mad = (intParameterRange(1, 0, 1000) * 8);
      break;
    case T.decimal:
      mad = Math.round(floatParameterRange(1, -Shape.RADIUS_MAX, Shape.RADIUS_MAX) * 2000);
      if (mad < 0) {
        restrictSelected(false, false);
        mad = -mad;
      }
      break;
    case T.bitset:
      if (!chk)
        sm.loadShape(shapeType);
      setShapeProperty(shapeType, "bitset", theToken.value);
      return;
    default:
      error(ERROR_booleanOrNumberExpected);
    }
    setShapeSizeBs(shapeType, mad, null);
  }

  private void animation() throws ScriptException {
    boolean animate = false;
    switch (getToken(1).tok) {
    case T.on:
      animate = true;
      //$FALL-THROUGH$
    case T.off:
      if (!chk)
        viewer.setAnimationOn(animate);
      break;
    case T.morph:
      int morphCount = (int) floatParameter(2);
      if (!chk)
        viewer.setAnimMorphCount(Math.abs(morphCount));
      break;
    case T.display:
      iToken = 2;
      BS bs = (tokAt(2) == T.all ? null : atomExpressionAt(2));
      checkLength(iToken + 1);
      if (!chk)
        viewer.setAnimDisplay(bs);
      return;
    case T.frame:
      frame(2);
      break;
    case T.mode:
      animationMode();
      break;
    case T.direction:
      animationDirection();
      break;
    case T.fps:
      setIntProperty("animationFps", intParameter(checkLast(2)));
      break;
    default:
      frameControl(1);
    }
  }

  private void assign() throws ScriptException {
    int atomsOrBonds = tokAt(1);
    int index = atomExpressionAt(2).nextSetBit(0);
    int index2 = -1;
    String type = null;
    if (index < 0)
      error(ERROR_invalidArgument);
    if (atomsOrBonds == T.connect) {
      index2 = atomExpressionAt(++iToken).nextSetBit(0);
    } else {
      type = parameterAsString(++iToken);
    }
    P3 pt = (++iToken < slen ? centerParameter(iToken) : null);
    if (chk)
      return;
    switch (atomsOrBonds) {
    case T.atoms:
      viewer.assignAtom(index, pt, type);
      break;
    case T.bonds:
      viewer.assignBond(index, (type + "p").charAt(0));
      break;
    case T.connect:
      viewer.assignConnect(index, index2);
    }
  }

  private void file() throws ScriptException {
    int file = intParameter(checkLast(1));
    if (chk)
      return;
    int modelIndex = viewer.getModelNumberIndex(file * 1000000 + 1, false,
        false);
    int modelIndex2 = -1;
    if (modelIndex >= 0) {
      modelIndex2 = viewer.getModelNumberIndex((file + 1) * 1000000 + 1, false,
          false);
      if (modelIndex2 < 0)
        modelIndex2 = viewer.getModelCount();
      modelIndex2--;
    }
    viewer.setAnimationOn(false);
    viewer.setAnimationDirection(1);
    viewer.setAnimationRange(modelIndex, modelIndex2);
    viewer.setCurrentModelIndex(-1);
  }

  private void fixed() throws ScriptException {
    BS bs = (slen == 1 ? null : atomExpressionAt(1));
    if (chk)
      return;
    viewer.setMotionFixedAtoms(bs);
  }

  private void frame(int offset) throws ScriptException {
    boolean useModelNumber = true;
    // for now -- as before -- remove to implement
    // frame/model difference
    if (slen == 1 && offset == 1) {
      int modelIndex = viewer.getCurrentModelIndex();
      int m;
      if (!chk && modelIndex >= 0
          && (m = viewer.getJmolDataSourceFrame(modelIndex)) >= 0)
        viewer.setCurrentModelIndex(m == modelIndex ? Integer.MIN_VALUE : m);
      return;
    }
    switch (tokAt(1)) {
    case T.expressionBegin:
    case T.bitset:
      int i = atomExpressionAt(1).nextSetBit(0);
      checkLength(iToken + 1);
      if (chk || i < 0)
        return;
      BS bsa = new BS();
      bsa.set(i);
      viewer.setCurrentModelIndex(viewer.getModelBitSet(bsa, false).nextSetBit(
          0));
      return;
    case T.id:
      checkLength(3);
      String id = stringParameter(2);
      if (!chk)
        viewer.setCurrentModelID(id);
      return;
    case T.delay:
      long millis = 0;
      checkLength(3);
      switch (getToken(2).tok) {
      case T.integer:
      case T.decimal:
        millis = (long) (floatParameter(2) * 1000);
        break;
      default:
        error(ERROR_integerExpected);
      }
      if (!chk)
        viewer.setFrameDelayMs(millis);
      return;
    case T.title:
      if (checkLength23() > 0)
        if (!chk)
          viewer.setFrameTitleObj(slen == 2 ? "@{_modelName}"
              : (tokAt(2) == T.varray ? SV
                  .listValue(st[2]) : parameterAsString(2)));
      return;
    case T.align:
      BS bs = (slen == 2 || tokAt(2) == T.none ? null
          : atomExpressionAt(2));
      if (!chk)
        viewer.setFrameOffsets(bs);
      return;
    }
    if (getToken(offset).tok == T.minus) {
      ++offset;
      if (getToken(checkLast(offset)).tok != T.integer
          || intParameter(offset) != 1)
        error(ERROR_invalidArgument);
      if (!chk)
        viewer.setAnimation(T.prev);
      return;
    }
    boolean isPlay = false;
    boolean isRange = false;
    boolean isAll = false;
    boolean isHyphen = false;
    int[] frameList = new int[] { -1, -1 };
    int nFrames = 0;
    float fFrame = 0;
    boolean haveFileSet = viewer.haveFileSet();

    for (int i = offset; i < slen; i++) {
      switch (getToken(i).tok) {
      case T.all:
      case T.times:
        checkLength(offset + (isRange ? 2 : 1));
        isAll = true;
        break;
      case T.minus: // ignore
        if (nFrames != 1)
          error(ERROR_invalidArgument);
        isHyphen = true;
        break;
      case T.none:
        checkLength(offset + 1);
        break;
      case T.decimal:
        useModelNumber = false;
        if ((fFrame = floatParameter(i)) < 0) {
          checkLength(i + 1);
          if (!chk)
            viewer.morph(-fFrame);
          return;
        }
        //$FALL-THROUGH$
      case T.integer:
      case T.string:
        if (nFrames == 2)
          error(ERROR_invalidArgument);
        int iFrame = (theTok == T.string ? getFloatEncodedInt((String) theToken.value)
            : theToken.intValue);
        if (iFrame < 0 && nFrames == 1) {
          isHyphen = true;
          iFrame = -iFrame;
          if (haveFileSet && iFrame < 1000000)
            iFrame *= 1000000;
        }
        if (theTok == T.decimal && haveFileSet && fFrame == (int) fFrame)
          iFrame = (int) fFrame * 1000000;
        if (iFrame == Integer.MAX_VALUE) {
          if (i == 1) {
            String id = theToken.value.toString();
            int modelIndex = (chk ? -1 : viewer
                .getModelIndexFromId(id));
            if (modelIndex >= 0) {
              checkLength(2);
              viewer.setCurrentModelIndex(modelIndex);
              return;
            }
          }
          iFrame = 0; // frame 0.0
        }
        if (iFrame == -1) {
          checkLength(offset + 1);
          if (!chk)
            viewer.setAnimation(T.prev);
          return;
        }
        if (iFrame >= 1000 && iFrame < 1000000 && haveFileSet)
          iFrame = (iFrame / 1000) * 1000000 + (iFrame % 1000); // initial way
        if (!useModelNumber && iFrame == 0 && nFrames == 0)
          isAll = true; // 0.0 means ALL; 0 means "all in this range
        if (iFrame >= 1000000)
          useModelNumber = false;
        frameList[nFrames++] = iFrame;
        break;
      case T.play:
        isPlay = true;
        break;
      case T.range:
        isRange = true;
        break;
      default:
        frameControl(offset);
        return;
      }
    }
    if (isRange && nFrames == 0)
      isAll = true;
    if (chk)
      return;
    if (isAll) {
      viewer.setAnimationOn(false);
      viewer.setAnimationRange(-1, -1);
      if (!isRange)
        viewer.setCurrentModelIndex(-1);
      return;
    }
    if (nFrames == 2 && !isRange)
      isHyphen = true;
    if (haveFileSet)
      useModelNumber = false;
    else if (useModelNumber)
      for (int i = 0; i < nFrames; i++)
        if (frameList[i] >= 0)
          frameList[i] %= 1000000;
    int modelIndex = viewer.getModelNumberIndex(frameList[0], useModelNumber,
        false);
    int modelIndex2 = -1;
    if (haveFileSet && modelIndex < 0 && frameList[0] != 0) {
      // may have frame 2.0 or frame 2 meaning the range of models in file 2
      // or frame 2.0 - 3.1   or frame 2.0 - 3.0
      if (frameList[0] < 1000000)
        frameList[0] *= 1000000;
      if (nFrames == 2 && frameList[1] < 1000000)
        frameList[1] *= 1000000;
      if (frameList[0] % 1000000 == 0) {
        frameList[0]++;
        modelIndex = viewer.getModelNumberIndex(frameList[0], false, false);
        if (modelIndex >= 0) {
          int i2 = (nFrames == 1 ? frameList[0] + 1000000
              : frameList[1] == 0 ? -1
                  : frameList[1] % 1000000 == 0 ? frameList[1] + 1000001
                      : frameList[1] + 1);
          modelIndex2 = viewer.getModelNumberIndex(i2, false, false);
          if (modelIndex2 < 0)
            modelIndex2 = viewer.getModelCount();
          modelIndex2--;
          if (isRange)
            nFrames = 2;
          else if (!isHyphen && modelIndex2 != modelIndex)
            isHyphen = true;
          isRange = isRange || modelIndex == modelIndex2;// (isRange ||
          // !isHyphen &&
          // modelIndex2 !=
          // modelIndex);
        }
      } else {
        // must have been a bad frame number. Just return.
        return;
      }
    }

    if (!isPlay && !isRange || modelIndex >= 0)
      viewer.setCurrentModelIndexClear(modelIndex, false);
    if (isPlay && nFrames == 2 || isRange || isHyphen) {
      if (modelIndex2 < 0)
        modelIndex2 = viewer.getModelNumberIndex(frameList[1], useModelNumber,
            false);
      viewer.setAnimationOn(false);
      viewer.setAnimationDirection(1);
      viewer.setAnimationRange(modelIndex, modelIndex2);
      viewer.setCurrentModelIndexClear(isHyphen && !isRange ? -1
          : modelIndex >= 0 ? modelIndex : 0, false);
    }
    if (isPlay)
      viewer.setAnimation(T.resume);
  }

  BS bitSetForModelFileNumber(int m) {
    // where */1.0 or */1.1 or just 1.1 is processed
    BS bs = BSUtil.newBitSet(viewer.getAtomCount());
    if (chk)
      return bs;
    int modelCount = viewer.getModelCount();
    boolean haveFileSet = viewer.haveFileSet();
    if (m < 1000000 && haveFileSet)
      m *= 1000000;
    int pt = m % 1000000;
    if (pt == 0) {
      int model1 = viewer.getModelNumberIndex(m + 1, false, false);
      if (model1 < 0)
        return bs;
      int model2 = (m == 0 ? modelCount : viewer.getModelNumberIndex(
          m + 1000001, false, false));
      if (model1 < 0)
        model1 = 0;
      if (model2 < 0)
        model2 = modelCount;
      if (viewer.isTrajectory(model1))
        model2 = model1 + 1;
      for (int j = model1; j < model2; j++)
        bs.or(viewer.getModelUndeletedAtomsBitSet(j));
    } else {
      int modelIndex = viewer.getModelNumberIndex(m, false, true);
      if (modelIndex >= 0)
        bs.or(viewer.getModelUndeletedAtomsBitSet(modelIndex));
    }
    return bs;
  }

  private void frameControl(int i) throws ScriptException {
    switch (getToken(checkLast(i)).tok) {
    case T.playrev:
    case T.play:
    case T.resume:
    case T.pause:
    case T.next:
    case T.prev:
    case T.rewind:
    case T.first:
    case T.last:
      if (!chk)
        viewer.setAnimation(theTok);
      return;
    }
    error(ERROR_invalidArgument);
  }

  private int getShapeType(int tok) throws ScriptException {
    int iShape = JC.shapeTokenIndex(tok);
    if (iShape < 0)
      error(ERROR_unrecognizedObject);
    return iShape;
  }

  private void font(int shapeType, float fontsize) throws ScriptException {
    String fontface = "SansSerif";
    String fontstyle = "Plain";
    int sizeAdjust = 0;
    float scaleAngstromsPerPixel = -1;
    switch (iToken = slen) {
    case 6:
      scaleAngstromsPerPixel = floatParameter(5);
      if (scaleAngstromsPerPixel >= 5) // actually a zoom value
        scaleAngstromsPerPixel = viewer.getZoomSetting()
            / scaleAngstromsPerPixel / viewer.getScalePixelsPerAngstrom(false);
      //$FALL-THROUGH$
    case 5:
      if (getToken(4).tok != T.identifier)
        error(ERROR_invalidArgument);
      fontstyle = parameterAsString(4);
      //$FALL-THROUGH$
    case 4:
      if (getToken(3).tok != T.identifier)
        error(ERROR_invalidArgument);
      fontface = parameterAsString(3);
      if (!isFloatParameter(2))
        error(ERROR_numberExpected);
      fontsize = floatParameter(2);
      shapeType = getShapeType(getToken(1).tok);
      break;
    case 3:
      if (!isFloatParameter(2))
        error(ERROR_numberExpected);
      if (shapeType == -1) {
        shapeType = getShapeType(getToken(1).tok);
        fontsize = floatParameter(2);
      } else {// labels --- old set fontsize N
        if (fontsize >= 1)
          fontsize += (sizeAdjust = 5);
      }
      break;
    case 2:
    default:
      if (shapeType == JC.SHAPE_LABELS) {
        // set fontsize
        fontsize = JC.LABEL_DEFAULT_FONTSIZE;
        break;
      }
      error(ERROR_badArgumentCount);
    }
    if (shapeType == JC.SHAPE_LABELS) {
      if (fontsize < 0
          || fontsize >= 1
          && (fontsize < JC.LABEL_MINIMUM_FONTSIZE || fontsize > JC.LABEL_MAXIMUM_FONTSIZE))
        integerOutOfRange(JC.LABEL_MINIMUM_FONTSIZE - sizeAdjust,
            JC.LABEL_MAXIMUM_FONTSIZE - sizeAdjust);
      setShapeProperty(JC.SHAPE_LABELS, "setDefaults", viewer
          .getNoneSelected());
    }
    if (chk)
      return;
    if (GData.getFontStyleID(fontface) >= 0) {
      fontstyle = fontface;
      fontface = "SansSerif";
    }
    JmolFont font3d = viewer.getFont3D(fontface, fontstyle, fontsize);
    sm.loadShape(shapeType);
    setShapeProperty(shapeType, "font", font3d);
    if (scaleAngstromsPerPixel >= 0)
      setShapeProperty(shapeType, "scalereference", Float
          .valueOf(scaleAngstromsPerPixel));
  }

  private void set() throws ScriptException {
    /*
     * The SET command now allows only the following:
     * 
     * SET SET xxx? SET [valid Jmol Token.setparam keyword] SET labelxxxx SET
     * xxxxCallback
     * 
     * All other variables must be assigned using
     * 
     * x = ....
     * 
     * The processing goes as follows:
     * 
     * check for SET check for SET xx? check for SET xxxx where xxxx is a
     * command --- deprecated (all other settings may alternatively start with x
     * = y) check for SET xxxx where xxxx requires special checking (all other
     * settings may alternatively start with x = (math expression) check for
     * context variables var x = ... check for deprecated SET words such as
     * "radius"
     */
    if (slen == 1) {
      showString(viewer.getAllSettings(null));
      return;
    }
    boolean isJmolSet = (parameterAsString(0).equals("set"));
    String key = optParameterAsString(1);
    if (isJmolSet && slen == 2 && key.indexOf("?") >= 0) {
      showString(viewer.getAllSettings(key.substring(0, key.indexOf("?"))));
      return;
    }
    int tok = getToken(1).tok;

    int newTok = 0;
    String sval;
    int ival = Integer.MAX_VALUE;

    boolean showing = (!chk && !tQuiet
        && scriptLevel <= scriptReportingLevel && !((String) st[0].value)
        .equals("var"));

    // THESE FIRST ARE DEPRECATED AND HAVE THEIR OWN COMMAND
    // anything in this block MUST RETURN

    switch (tok) {
    case T.axes:
      axes(2);
      return;
    case T.background:
      background(2);
      return;
    case T.boundbox:
      boundbox(2);
      return;
    case T.frank:
      frank(2);
      return;
    case T.history:
      history(2);
      return;
    case T.label:
      label(2);
      return;
    case T.unitcell:
      unitcell(2);
      return;
    case T.highlight:
      sm.loadShape(JC.SHAPE_HALOS);
      setShapeProperty(JC.SHAPE_HALOS, "highlight",
          (tokAt(2) == T.off ? null : atomExpressionAt(2)));
      return;
    case T.display:// deprecated
    case T.selectionhalos:
      selectionHalo(2);
      return;
    case T.timeout:
      timeout(2);
      return;
    }

    // THESE HAVE MULTIPLE CONTEXTS AND
    // SO DO NOT ALLOW CALCULATIONS xxx = a + b...
    // and are thus "setparam" only

    // anything in this block MUST RETURN

    switch (tok) {
    case T.structure:
      EnumStructure type = EnumStructure.getProteinStructureType(parameterAsString(2));
      if (type == EnumStructure.NOT)
        error(ERROR_invalidArgument);
      float[] data = floatParameterSet(3, 0, Integer.MAX_VALUE);
      if (data.length % 4 != 0)
        error(ERROR_invalidArgument);
      viewer.setStructureList(data, type);
      checkLast(iToken);
      return;
    case T.axescolor:
      ival = getArgbParam(2);
      if (!chk)
        setObjectArgb("axes", ival);
      return;
    case T.bondmode:
      setBondmode();
      return;
    case T.debug:
      if (chk)
        return;
      int iLevel = (tokAt(2) == T.off || tokAt(2) == T.integer
          && intParameter(2) == 0 ? 4 : 5);
      Logger.setLogLevel(iLevel);
      setIntProperty("logLevel", iLevel);
      if (iLevel == 4) {
        viewer.setDebugScript(false);
        if (showing)
          viewer.showParameter("debugScript", true, 80);
      }
      setDebugging();
      if (showing)
        viewer.showParameter("logLevel", true, 80);
      return;
    case T.echo:
      setEcho();
      return;
    case T.fontsize:
      font(JC.SHAPE_LABELS, checkLength23() == 2 ? 0
          : floatParameter(2));
      return;
    case T.hbond:
      setHbond();
      return;
    case T.measure:
    case T.measurements:
      setMonitor();
      return;
    case T.ssbond: // ssBondsBackbone
      setSsbond();
      return;
    case T.togglelabel:
      setLabel("toggle");
      return;
    case T.usercolorscheme:
      setUserColors();
      return;
    case T.zslab:
      setZslab();
      return;
    }

    // these next may report a value
    // require special checks
    // math expressions are allowed in most cases.

    boolean justShow = true;

    switch (tok) {
    case T.backgroundmodel:
      if (slen > 2) {
        String modelDotted = stringSetting(2, false);
        int modelNumber;
        boolean useModelNumber = false;
        if (modelDotted.indexOf(".") < 0) {
          modelNumber = Parser.parseInt(modelDotted);
          useModelNumber = true;
        } else {
          modelNumber = getFloatEncodedInt(modelDotted);
        }
        if (chk)
          return;
        int modelIndex = viewer.getModelNumberIndex(modelNumber,
            useModelNumber, true);
        viewer.setBackgroundModelIndex(modelIndex);
        return;
      }
      break;
    case T.vanderwaals:
      if (chk)
        return;
      viewer.setAtomProperty(viewer.getModelUndeletedAtomsBitSet(-1),
          T.vanderwaals, -1, Float.NaN, null, null, null);
      switch (tokAt(2)) {
      case T.probe:
        runScript(Elements.VdwPROBE);
        return;
      }
      newTok = T.defaultvdw;
      //$FALL-THROUGH$
    case T.defaultvdw:
      // allows unquoted string for known vdw type
      if (slen > 2) {
        sval = (slen == 3
            && EnumVdw.getVdwType(parameterAsString(2)) == null ? stringSetting(
            2, false)
            : parameterAsString(2));
        if (EnumVdw.getVdwType(sval) == null)
          error(ERROR_invalidArgument);
        setStringProperty(key, sval);
      }
      break;
    case T.defaultlattice:
      if (slen > 2) {
        P3 pt;
        SV var = parameterExpressionToken(2);
        if (var.tok == T.point3f)
          pt = (P3) var.value;
        else {
          int ijk = var.asInt();
          if (ijk < 555)
            pt = new P3();
          else
            pt = viewer.getSymmetry().ijkToPoint3f(ijk + 111);
        }
        if (!chk)
          viewer.setDefaultLattice(pt);
      }
      break;
    case T.defaults:
    case T.defaultcolorscheme:
      // allows unquoted "jmol" or "rasmol"
      if (slen > 2) {
        if ((theTok = tokAt(2)) == T.jmol || theTok == T.rasmol) {
          sval = parameterAsString(checkLast(2));
        } else {
          sval = stringSetting(2, false);
        }
        setStringProperty(key, sval);
      }
      break;
    case T.formalcharge:
      ival = intSetting(2);
      if (ival == Integer.MIN_VALUE)
        error(ERROR_invalidArgument);
      if (!chk)
        viewer.setFormalCharges(ival);
      return;
    case T.historylevel:
      // save value locally as well
      ival = intSetting(2);
      if (!chk) {
        if (ival != Integer.MIN_VALUE)
          commandHistoryLevelMax = ival;
        setIntProperty(key, ival);
      }
      break;
    case T.language:
      // language can be used without quotes in a SET context
      // set language en
      if (slen > 2)
        setStringProperty(key, stringSetting(2, isJmolSet));
      break;
    case T.measurementunits:
    case T.energyunits:
      if (slen > 2)
        setUnits(stringSetting(2, isJmolSet), tok);
      break;
    case T.picking:
      if (!chk)
        viewer.setPicked(-1);
      if (slen > 2) {
        setPicking();
        return;
      }
      break;
    case T.pickingstyle:
      if (slen > 2) {
        setPickingStyle();
        return;
      }
      break;
    case T.property: // compiler may give different values to this token
      // set property_xxxx will be handled in setVariable
      break;
    case T.scriptreportinglevel:
      // save value locally as well
      ival = intSetting(2);
      if (!chk && ival != Integer.MIN_VALUE)
        setIntProperty(key, scriptReportingLevel = ival);
      break;
    case T.specular:
      ival = intSetting(2);
      if (ival == Integer.MIN_VALUE || ival == 0 || ival == 1) {
        justShow = false;
        break;
      }
      tok = T.specularpercent;
      key = "specularPercent";
      setIntProperty(key, ival);
      break;
    case T.strands:
      tok = T.strandcount;
      key = "strandCount";
      setIntProperty(key, intSetting(2));
      break;
    default:
      justShow = false;
    }

    if (justShow && !showing)
      return;

    // var xxxx = xxx can supercede set xxxx

    boolean isContextVariable = (!justShow && !isJmolSet && getContextVariableAsVariable(key) != null);

    if (!justShow && !isContextVariable) {

      // THESE NEXT are deprecated:

      switch (tok) {
      case T.bonds:
        newTok = T.showmultiplebonds;
        break;
      case T.hetero:
        newTok = T.selecthetero;
        break;
      case T.hydrogen:
        newTok = T.selecthydrogen;
        break;
      case T.measurementnumbers:
        newTok = T.measurementlabels;
        break;
      case T.radius:
        newTok = T.solventproberadius;
        setFloatProperty("solventProbeRadius", floatSetting(2));
        justShow = true;
        break;
      case T.scale3d:
        newTok = T.scaleangstromsperinch;
        break;
      case T.solvent:
        newTok = T.solventprobe;
        break;
      case T.color:
        newTok = T.defaultcolorscheme;
        break;
      case T.spin:
        sval = parameterAsString(2).toLowerCase();
        switch ("x;y;z;fps".indexOf(sval + ";")) {
        case 0:
          newTok = T.spinx;
          break;
        case 2:
          newTok = T.spiny;
          break;
        case 4:
          newTok = T.spinz;
          break;
        case 6:
          newTok = T.spinfps;
          break;
        default:
          errorStr2(ERROR_unrecognizedParameter, "set SPIN ", sval);
        }
        if (!chk)
          viewer.setSpin(sval, (int) floatParameter(checkLast(3)));
        justShow = true;
        break;
      }
    }

    if (newTok != 0) {
      key = T.nameOf(tok = newTok);
    } else if (!justShow && !isContextVariable) {
      // special cases must be checked
      if (key.length() == 0 || key.charAt(0) == '_') // these cannot be set by user
        error(ERROR_cannotSet);

      // these next are not reported and do not allow calculation xxxx = a + b

      String lckey = key.toLowerCase();
      if (lckey.indexOf("label") == 0
          && Parser
              .isOneOf(key.substring(5).toLowerCase(),
                  "front;group;atom;offset;offsetexact;pointer;alignment;toggle;scalereference")) {
        if (setLabel(key.substring(5)))
          return;
      }
      if (lckey.endsWith("callback"))
        tok = T.setparam;
    }
    if (isJmolSet && !T.tokAttr(tok, T.setparam)) {
      iToken = 1;
      if (!isStateScript)
        errorStr2(ERROR_unrecognizedParameter, "SET", key);
      warning(ERROR_unrecognizedParameterWarning, "SET", key);
    }

    if (!justShow && isJmolSet) {
      // simple cases
      switch (slen) {
      case 2:
        // set XXXX;
        // too bad we allow this...
        setBooleanProperty(key, true);
        justShow = true;
        break;
      case 3:
        // set XXXX val;
        // check for int and NONE just in case
        if (ival != Integer.MAX_VALUE) {
          // keep it simple
          setIntProperty(key, ival);
          justShow = true;
        }
        break;
      }
    }

    if (!justShow && !isJmolSet && tokAt(2) == T.none) {
      if (!chk)
        viewer.removeUserVariable(key.toLowerCase());
      justShow = true;
    }

    if (!justShow) {
      int tok2 = (tokAt(1) == T.expressionBegin ? 0 : tokAt(2));
      int setType = st[0].intValue;
      // recasted by compiler:
      // var c.xxx =
      // c.xxx =
      // {...}[n].xxx =
      // not supported:
      // a[...][...].xxx =
      // var a[...][...].xxx =

      int pt = (tok2 == T.opEQ ? 3
      // set x = ...
          : setType == '=' && !key.equals("return") && tok2 != T.opEQ ? 0
          // {c}.xxx =
              // {...}.xxx =
              // {{...}[n]}.xxx =
              : 2
      // var a[...].xxx =
      // a[...].xxx =
      // var c = ...
      // var c = [
      // c = [
      // c = ...
      // set x ...
      // a[...] =
      );

      setVariable(pt, 0, key, setType);
      if (!isJmolSet)
        return;
    }
    if (showing)
      viewer.showParameter(key, true, 80);
  }

  private void setZslab() throws ScriptException {
    // sets zSlab either based on a percent value or an atom position
    P3 pt = null;
    if (isFloatParameter(2)) {
      checkLength(3);
      setIntProperty("zSlab", (int) floatParameter(2));
    } else {
      if (!isCenterParameter(2))
        error(ERROR_invalidArgument);
      pt = centerParameter(2);
      checkLength(iToken + 1);
    }
    if (!chk)
      viewer.setZslabPoint(pt);
  }

  private void setBondmode() throws ScriptException {
    boolean bondmodeOr = false;
    switch (getToken(checkLast(2)).tok) {
    case T.opAnd:
      break;
    case T.opOr:
      bondmodeOr = true;
      break;
    default:
      error(ERROR_invalidArgument);
    }
    setBooleanProperty("bondModeOr", bondmodeOr);
  }

  private void setEcho() throws ScriptException {
    String propertyName = null;
    Object propertyValue = null;
    String id = null;
    boolean echoShapeActive = true;
    // set echo xxx
    int pt = 2;

    // check for ID name or just name
    // also check simple OFF, NONE
    switch (getToken(2).tok) {
    case T.off:
      id = propertyName = "allOff";
      checkLength(++pt);
      break;
    case T.none:
      echoShapeActive = false;
      //$FALL-THROUGH$
    case T.all:
      // all and none get NO additional parameters;
      id = parameterAsString(2);
      checkLength(++pt);
      break;
    case T.left:
    case T.center:
    case T.right:
    case T.top:
    case T.middle:
    case T.bottom:
    case T.identifier:
    case T.string:
    case T.id:
      if (theTok == T.id)
        pt++;
      id = parameterAsString(pt++);
      break;
    }

    if (!chk) {
      viewer.setEchoStateActive(echoShapeActive);
      sm.loadShape(JC.SHAPE_ECHO);
      if (id != null)
        setShapeProperty(JC.SHAPE_ECHO,
            propertyName == null ? "target" : propertyName, id);
    }

    if (pt < slen) {
      // set echo name xxx
      // pt is usually 3, but could be 4 if ID used
      switch (getToken(pt++).tok) {
      case T.align:
        propertyName = "align";
        switch (getToken(pt).tok) {
        case T.left:
        case T.right:
        case T.center:
          propertyValue = parameterAsString(pt++);
          break;
        default:
          error(ERROR_invalidArgument);
        }
        break;
      case T.center:
      case T.left:
      case T.right:
        propertyName = "align";
        propertyValue = parameterAsString(pt - 1);
        break;
      case T.depth:
        propertyName = "%zpos";
        propertyValue = Integer.valueOf((int) floatParameter(pt++));
        break;
      case T.display:
      case T.displayed:
      case T.on:
        propertyName = "hidden";
        propertyValue = Boolean.FALSE;
        break;
      case T.hide:
      case T.hidden:
        propertyName = "hidden";
        propertyValue = Boolean.TRUE;
        break;
      case T.model:
        int modelIndex = (chk ? 0 : modelNumberParameter(pt++));
        if (modelIndex >= viewer.getModelCount())
          error(ERROR_invalidArgument);
        propertyName = "model";
        propertyValue = Integer.valueOf(modelIndex);
        break;
      case T.leftsquare:
      case T.spacebeforesquare:
        // [ x y ] with or without %
        propertyName = "xypos";
        propertyValue = xypParameter(--pt);
        if (propertyValue == null)
          pt--;
        else
          pt = iToken + 1;
        break;
      case T.integer:
        // x y without brackets
        pt--;
        int posx = intParameter(pt++);
        String namex = "xpos";
        if (tokAt(pt) == T.percent) {
          namex = "%xpos";
          pt++;
        }
        propertyName = "ypos";
        propertyValue = Integer.valueOf(intParameter(pt++));
        if (tokAt(pt) == T.percent) {
          propertyName = "%ypos";
          pt++;
        }
        checkLength(pt);
        setShapeProperty(JC.SHAPE_ECHO, namex, Integer.valueOf(posx));
        break;
      case T.off:
        propertyName = "off";
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(pt++));
        break;
      case T.script:
        propertyName = "script";
        propertyValue = parameterAsString(pt++);
        break;
      case T.string:
      case T.image:
        boolean isImage = (theTok == T.image);
        if (isImage)
          pt++;
        checkLength(pt);
        if (id == null && isImage) {
          String[] data = new String[1];
          getShapePropertyData(JC.SHAPE_ECHO, "currentTarget", data);
          id = data[0];          
        }
        echo(pt - 1, id, isImage);
        return;
      default:
        if (isCenterParameter(pt - 1)) {
          propertyName = "xyz";
          propertyValue = centerParameter(pt - 1);
          pt = iToken + 1;
          break;
        }
        error(ERROR_invalidArgument);
      }
    }
    checkLength(pt);
    if (!chk && propertyName != null)
      setShapeProperty(JC.SHAPE_ECHO, propertyName, propertyValue);
  }

  private int intSetting(int pt) throws ScriptException {
    if (pt == slen)
      return Integer.MIN_VALUE;
    return parameterExpressionToken(pt).asInt();
  }

  private float floatSetting(int pt)
      throws ScriptException {
    if (pt == slen)
      return Float.NaN;
    return SV.fValue(parameterExpressionToken(pt));
  }

  private String stringSetting(int pt, boolean isJmolSet)
      throws ScriptException {
    if (isJmolSet && slen == pt + 1)
      return parameterAsString(pt);
    return parameterExpressionToken(pt).asString();
  }

  private boolean setLabel(String str) throws ScriptException {
    sm.loadShape(JC.SHAPE_LABELS);
    Object propertyValue = null;
    setShapeProperty(JC.SHAPE_LABELS, "setDefaults", viewer
        .getNoneSelected());
    while (true) {
      if (str.equals("scalereference")) {
        float scaleAngstromsPerPixel = floatParameter(2);
        if (scaleAngstromsPerPixel >= 5) // actually a zoom value
          scaleAngstromsPerPixel = viewer.getZoomSetting()
              / scaleAngstromsPerPixel
              / viewer.getScalePixelsPerAngstrom(false);
        propertyValue = Float.valueOf(scaleAngstromsPerPixel);
        break;
      }
      if (str.equals("offset") || str.equals("offsetexact")) {
        int xOffset = intParameterRange(2, -127, 127);
        int yOffset = intParameterRange(3, -127, 127);
        propertyValue = Integer.valueOf(Object2d.getOffset(xOffset, yOffset));
        break;
      }
      if (str.equals("alignment")) {
        switch (getToken(2).tok) {
        case T.left:
        case T.right:
        case T.center:
          str = "align";
          propertyValue = theToken.value;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        break;
      }
      if (str.equals("pointer")) {
        int flags = Object2d.POINTER_NONE;
        switch (getToken(2).tok) {
        case T.off:
        case T.none:
          break;
        case T.background:
          flags |= Object2d.POINTER_BACKGROUND;
          //$FALL-THROUGH$
        case T.on:
          flags |= Object2d.POINTER_ON;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        propertyValue = Integer.valueOf(flags);
        break;
      }
      if (str.equals("toggle")) {
        iToken = 1;
        BS bs = (slen == 2 ? null : atomExpressionAt(2));
        checkLast(iToken);
        if (!chk)
          viewer.togglePickingLabel(bs);
        return true;
      }
      iToken = 1;
      boolean TF = (slen == 2 || getToken(2).tok == T.on);
      if (str.equals("front") || str.equals("group")) {
        if (!TF && tokAt(2) != T.off)
          error(ERROR_invalidArgument);
        if (!TF)
          str = "front";
        propertyValue = (TF ? Boolean.TRUE : Boolean.FALSE);
        break;
      }
      if (str.equals("atom")) {
        if (!TF && tokAt(2) != T.off)
          error(ERROR_invalidArgument);
        str = "front";
        propertyValue = (TF ? Boolean.FALSE : Boolean.TRUE);
        break;
      }
      return false;
    }
    BS bs = (iToken + 1 < slen ? atomExpressionAt(++iToken) : null);
    checkLast(iToken);
    if (chk)
      return true;
    if (bs == null)
      setShapeProperty(JC.SHAPE_LABELS, str, propertyValue);
    else
      setShapePropertyBs(JC.SHAPE_LABELS, str, propertyValue, bs);
    return true;
  }

  private void setMonitor() throws ScriptException {
    // on off here incompatible with "monitor on/off" so this is just a SET
    // option.
    int tok = tokAt(checkLast(2));
    switch (tok) {
    case T.on:
    case T.off:
      setBooleanProperty("measurementlabels", tok == T.on);
      return;
    case T.dotted:
    case T.integer:
    case T.decimal:
      setShapeSizeBs(JC.SHAPE_MEASURES, getSetAxesTypeMad(2), null);
      return;
    }
    setUnits(parameterAsString(2), T.measurementunits);
  }

  private boolean setUnits(String units, int tok) throws ScriptException {
    if (tok == T.measurementunits && Parser.isOneOf(units.toLowerCase(),
        "angstroms;au;bohr;nanometers;nm;picometers;pm;vanderwaals;vdw")) {
      if (!chk)
        viewer.setUnits(units, true); 
    } else if (tok == T.energyunits && Parser.isOneOf(units.toLowerCase(), "kcal;kj")) {
      if (!chk)
        viewer.setUnits(units, false);
    } else {
      errorStr2(ERROR_unrecognizedParameter, "set " + T.nameOf(tok), units);
    }
    return true;
  }

  /*
   * private void setProperty() throws ScriptException { // what possible good
   * is this? // set property foo bar is identical to // set foo bar if
   * (getToken(2).tok != Token.identifier) error(ERROR_propertyNameExpected);
   * String propertyName = parameterAsString(2); switch
   * (getToken(checkLast(3)).tok) { case Token.on:
   * setBooleanProperty(propertyName, true); break; case Token.off:
   * setBooleanProperty(propertyName, false); break; case Token.integer:
   * setIntProperty(propertyName, intParameter(3)); break; case Token.decimal:
   * setFloatProperty(propertyName, floatParameter(3)); break; case
   * Token.string: setStringProperty(propertyName, stringParameter(3)); break;
   * default: error(ERROR_unrecognizedParameter, "SET " +
   * propertyName.toUpperCase(), parameterAsString(3)); } }
   */

  private void setSsbond() throws ScriptException {
    boolean ssbondsBackbone = false;
    // shapeManager.loadShape(JmolConstants.SHAPE_SSSTICKS);
    switch (tokAt(checkLast(2))) {
    case T.backbone:
      ssbondsBackbone = true;
      break;
    case T.sidechain:
      break;
    default:
      error(ERROR_invalidArgument);
    }
    setBooleanProperty("ssbondsBackbone", ssbondsBackbone);
  }

  private void setHbond() throws ScriptException {
    boolean bool = false;
    switch (tokAt(checkLast(2))) {
    case T.backbone:
      bool = true;
      //$FALL-THROUGH$
    case T.sidechain:
      setBooleanProperty("hbondsBackbone", bool);
      break;
    case T.solid:
      bool = true;
      //$FALL-THROUGH$
    case T.dotted:
      setBooleanProperty("hbondsSolid", bool);
      break;
    default:
      error(ERROR_invalidArgument);
    }
  }

  private void setPicking() throws ScriptException {
    // set picking
    if (slen == 2) {
      setStringProperty("picking", "identify");
      return;
    }
    // set picking @{"xxx"} or some large length, ignored
    if (slen > 4 || tokAt(2) == T.string) {
      setStringProperty("picking", stringSetting(2, false));
      return;
    }
    int i = 2;
    // set picking select ATOM|CHAIN|GROUP|MOLECULE|MODEL|SITE
    // set picking measure ANGLE|DISTANCE|TORSION
    // set picking spin fps
    String type = "SELECT";
    switch (getToken(2).tok) {
    case T.select:
    case T.measure:
    case T.spin:
      if (checkLength34() == 4) {
        type = parameterAsString(2).toUpperCase();
        if (type.equals("SPIN"))
          setIntProperty("pickingSpinRate", intParameter(3));
        else
          i = 3;
      }
      break;
    case T.delete:
      break;
    default:
      checkLength(3);
    }

    // set picking on
    // set picking normal
    // set picking identify
    // set picking off
    // set picking select
    // set picking bonds
    // set picking dragselected

    String str = parameterAsString(i);
    switch (getToken(i).tok) {
    case T.on:
    case T.normal:
      str = "identify";
      break;
    case T.off:
    case T.none:
      str = "off";
      break;
    case T.select:
      str = "atom";
      break;
    case T.label:
      str = "label";
      break;
    case T.bonds: // not implemented
      str = "bond";
      break;
    case T.delete:
      checkLength(4);
      if (tokAt(3) != T.bonds)
        error(ERROR_invalidArgument);
      str = "deleteBond";
      break;
    }
    int mode = ((mode = str.indexOf("_")) >= 0 ? mode : str.length());
    mode = ActionManager.getPickingMode(str.substring(0, mode));
    if (mode < 0)
      errorStr2(ERROR_unrecognizedParameter, "SET PICKING " + type, str);
    setStringProperty("picking", str);
  }

  private void setPickingStyle() throws ScriptException {
    if (slen > 4 || tokAt(2) == T.string) {
      setStringProperty("pickingStyle", stringSetting(2, false));
      return;
    }
    int i = 2;
    boolean isMeasure = false;
    String type = "SELECT";
    switch (getToken(2).tok) {
    case T.measure:
      isMeasure = true;
      type = "MEASURE";
      //$FALL-THROUGH$
    case T.select:
      if (checkLength34() == 4)
        i = 3;
      break;
    default:
      checkLength(3);
    }
    String str = parameterAsString(i);
    switch (getToken(i).tok) {
    case T.none:
    case T.off:
      str = (isMeasure ? "measureoff" : "toggle");
      break;
    case T.on:
      if (isMeasure)
        str = "measure";
      break;
    }
    if (ActionManager.getPickingStyle(str) < 0)
      errorStr2(ERROR_unrecognizedParameter, "SET PICKINGSTYLE " + type, str);
    setStringProperty("pickingStyle", str);
  }

  private void timeout(int index) throws ScriptException {
    // timeout ID "mytimeout" mSec "script"
    // msec < 0 --> repeat indefinitely
    // timeout ID "mytimeout" 1000 // milliseconds
    // timeout ID "mytimeout" 0.1 // seconds
    // timeout ID "mytimeout" OFF
    // timeout ID "mytimeout" // flag to trigger waiting timeout repeat
    // timeout OFF
    String name = null;
    String script = null;
    int mSec = 0;
    if (slen == index) {
      showString(viewer.showTimeout(null));
      return;
    }
    for (int i = index; i < slen; i++)
      switch (getToken(i).tok) {
      case T.id:
        name = parameterAsString(++i);
        if (slen == 3) {
          if (!chk)
            viewer.triggerTimeout(name);
          return;
        }
        break;
      case T.off:
        break;
      case T.integer:
        mSec = intParameter(i);
        break;
      case T.decimal:
        mSec = Math.round(floatParameter(i) * 1000);
        break;
      default:
        if (name == null)
          name = parameterAsString(i);
        else if (script == null)
          script = parameterAsString(i);
        else
          error(ERROR_invalidArgument);
        break;
      }
    if (!chk)
      viewer.setTimeout(name, mSec, script);
  }

  private void setUserColors() throws ScriptException {
    JmolList<Integer> v = new  JmolList<Integer>();
    for (int i = 2; i < slen; i++) {
      int argb = getArgbParam(i);
      v.addLast(Integer.valueOf(argb));
      i = iToken;
    }
    if (chk)
      return;
    int n = v.size();
    int[] scale = new int[n];
    for (int i = n; --i >= 0;)
      scale[i] = v.get(i).intValue();
    viewer.setUserScale(scale);
  }

  /**
   * 
   * @param pt
   * @param ptMax
   * @param key
   * @param setType
   * @throws ScriptException
   */
  @SuppressWarnings("unchecked")
  private void setVariable(int pt, int ptMax, String key, int setType)
      throws ScriptException {
    BS bs = null;
    String propertyName = "";
    int tokProperty = T.nada;
    boolean isArrayItem = (setType == '[');
    boolean settingProperty = false;
    boolean isExpression = false;
    boolean settingData = (key.startsWith("property_"));
    SV t = (settingData ? null : getContextVariableAsVariable(key));
    boolean isUserVariable = (t != null);
    if (pt > 0 && tokAt(pt - 1) == T.expressionBegin) {
      bs = atomExpressionAt(pt - 1);
      pt = iToken + 1;
      isExpression = true;
    }
    if (tokAt(pt) == T.per) {
      settingProperty = true;
      SV token = getBitsetPropertySelector(++pt, true);
      if (token == null)
        error(ERROR_invalidArgument);
      if (tokAt(++pt) != T.opEQ)
        error(ERROR_invalidArgument);
      pt++;
      tokProperty = token.intValue;
      propertyName = (String) token.value;
    }
    if (isExpression && !settingProperty)
      error(ERROR_invalidArgument);

    // get value

    JmolList<SV> v = (JmolList<SV>) parameterExpression(pt,
        ptMax, key, true, true, -1, isArrayItem, null, null);
    int nv = v.size();
    if (nv == 0 || !isArrayItem && nv > 1 || isArrayItem
        && (nv < 3 || nv % 2 != 1))
      error(ERROR_invalidArgument);
    if (chk)
      return;
    // x[3][4] = ??
    SV tv = v.get(isArrayItem ? v.size() - 1 : 0);

    // create user variable if needed for list now, so we can do the copying

    boolean needVariable = (!isUserVariable && !isExpression && !settingData && (isArrayItem
        || settingProperty || !(tv.value instanceof String
        || tv.tok == T.integer || tv.value instanceof Integer
        || tv.value instanceof Float || tv.value instanceof Boolean)));

    if (needVariable) {
      if (key.startsWith("_"))
        errorStr(ERROR_invalidArgument, key);
      t = viewer.getOrSetNewVariable(key, true);
      isUserVariable = true;
    }

    if (isArrayItem) {
      SV tnew = (SV.newVariable(T.string, "")).set(tv, false);
      int nParam = v.size() / 2;
      for (int i = 0; i < nParam; i++) {
        boolean isLast = (i + 1 == nParam);
        SV vv = v.get(i * 2);
        // stack is selector [ selector [ selector [ ... VALUE 
        if (t.tok == T.bitset) {
          t.tok = T.hash;
          t.value = new Hashtable<String, SV>();
        }
        if (t.tok == T.hash) {
          String hkey = vv.asString();
          Map<String, SV> tmap = (Map<String, SV>) t.value;
          if (isLast) {
            tmap.put(hkey, tnew);
            break;
          }
          t = tmap.get(hkey);
        } else {
          int ipt = vv.asInt();
          // in the case of for (x in y) where y is an array, we need to select the item before continuing
          if (t.tok == T.varray)
            t = SV.selectItemVar(t);
          switch (t.tok) {
          case T.varray:
            JmolList<SV> list = t.getList();
            if (ipt > list.size() || isLast)
              break;
            if (ipt <= 0)
              ipt = list.size() + ipt;
            if (--ipt < 0)
              ipt = 0;
            t = list.get(ipt);
            continue;
          case T.matrix3f:
          case T.matrix4f:
            // check for row/column replacement
            int dim = (t.tok == T.matrix3f ? 3 : 4);
            if (nParam == 1 && Math.abs(ipt) >= 1 && Math.abs(ipt) <= dim
                && tnew.tok == T.varray && tnew.getList().size() == dim)
              break;
            if (nParam == 2) {
              int ipt2 = v.get(2).asInt();
              if (ipt2 >= 1 && ipt2 <= dim
                  && (tnew.tok == T.integer || tnew.tok == T.decimal)) {
                i++;
                ipt = ipt * 10 + ipt2;
                break;
              }
            }
            // change to an array and continue;
            t.toArray();
            --i;
            continue;
          }
          t.setSelectedValue(ipt, tnew);
          break;
        }
      }
      return;
    }
    if (settingProperty) {
      if (!isExpression) {
        bs = SV.getBitSet(t, true);
        if (bs == null)
          error(ERROR_invalidArgument);
      }
      if (propertyName.startsWith("property_")) {
        viewer.setData(propertyName, new Object[] {
            propertyName,
            (tv.tok == T.varray ? SV.flistValue(tv,((JmolList<?>)tv.value).size() == bs.cardinality() ? bs.cardinality() : viewer.getAtomCount()) : tv.asString()), 
             BSUtil.copy(bs), Integer.valueOf(tv.tok == T.varray ? 1 : 0) }, 
             viewer.getAtomCount(), 0, 0, tv.tok == T.varray ? Integer.MAX_VALUE
            : Integer.MIN_VALUE, 0);
        return;
      }
      setBitsetProperty(bs, tokProperty, tv.asInt(), tv.asFloat(), tv);
      return;
    }

    if (isUserVariable) {
      t.set(tv, false);
      return;
    }

    Object vv = SV.oValue(tv);

    if (key.startsWith("property_")) {
      if (tv.tok == T.varray)
        vv = tv.asString();
      viewer.setData(key, new Object[] { key, "" + vv,
          BSUtil.copy(viewer.getSelectionSet(false)), Integer.valueOf(0) }, 
          viewer.getAtomCount(), 0, 0,
          Integer.MIN_VALUE, 0);
      return;
    }

    if (vv instanceof Boolean) {
      setBooleanProperty(key, ((Boolean) vv).booleanValue());
    } else if (vv instanceof Integer) {
      setIntProperty(key, ((Integer) vv).intValue());
    } else if (vv instanceof Float) {
      setFloatProperty(key, ((Float) vv).floatValue());
    } else if (vv instanceof String) {
      setStringProperty(key, (String) vv);
    } else if (vv instanceof BondSet) {
      setStringProperty(key, Escape.eB((BS) vv, false));
    } else if (vv instanceof BS || vv instanceof P3
        || vv instanceof Point4f) {
      setStringProperty(key, Escape.e(vv));
    } else {
      Logger.error("ERROR -- return from propertyExpression was " + vv);
    }
  }

  private void axes(int index) throws ScriptException {
    // axes (index==1) or set axes (index==2)
    TickInfo tickInfo = checkTicks(index, true, true, false);
    index = iToken + 1;
    int tok = tokAt(index);
    String type = optParameterAsString(index).toLowerCase();
    if (slen == index + 1
        && Parser.isOneOf(type, "window;unitcell;molecular")) {
      setBooleanProperty("axes" + type, true);
      return;
    }
    switch (tok) {
    case T.center:
      P3 center = centerParameter(index + 1);
      setShapeProperty(JC.SHAPE_AXES, "origin", center);
      checkLast(iToken);
      return;
    case T.scale:
      setFloatProperty("axesScale", floatParameter(checkLast(++index)));
      return;
    case T.label:
      switch (tok = tokAt(index + 1)) {
      case T.off:
      case T.on:
        checkLength(index + 2);
        setShapeProperty(JC.SHAPE_AXES, "labels"
            + (tok == T.on ? "On" : "Off"), null);
        return;
      }
      String sOrigin = null;
      switch (slen - index) {
      case 7:
        // axes labels "X" "Y" "Z" "-X" "-Y" "-Z"
        setShapeProperty(JC.SHAPE_AXES, "labels", new String[] {
            parameterAsString(++index), parameterAsString(++index),
            parameterAsString(++index), parameterAsString(++index),
            parameterAsString(++index), parameterAsString(++index) });
        break;
      case 5:
        sOrigin = parameterAsString(index + 4);
        //$FALL-THROUGH$
      case 4:
        // axes labels "X" "Y" "Z" [origin]
        setShapeProperty(JC.SHAPE_AXES, "labels", new String[] {
            parameterAsString(++index), parameterAsString(++index),
            parameterAsString(++index), sOrigin });
        break;
      default:
        error(ERROR_badArgumentCount);
      }
      return;
    }
    // axes position [x y %]
    if (type.equals("position")) {
      P3 xyp;
      if (tokAt(++index) == T.off) {
        xyp = new P3();
      } else {
        xyp = xypParameter(index);
        if (xyp == null)
          error(ERROR_invalidArgument);
        index = iToken;
      }
      setShapeProperty(JC.SHAPE_AXES, "position", xyp);
      return;
    }
    int mad = getSetAxesTypeMad(index);
    if (chk)
      return;
    setObjectMad(JC.SHAPE_AXES, "axes", mad);
    if (tickInfo != null)
      setShapeProperty(JC.SHAPE_AXES, "tickInfo", tickInfo);
  }

  private void boundbox(int index) throws ScriptException {
    TickInfo tickInfo = checkTicks(index, false, true, false);
    index = iToken + 1;
    float scale = 1;
    if (tokAt(index) == T.scale) {
      scale = floatParameter(++index);
      if (!chk && scale == 0)
        error(ERROR_invalidArgument);
      index++;
      if (index == slen) {
        if (!chk)
          viewer.setBoundBox(null, null, true, scale);
        return;
      }
    }
    boolean byCorner = (tokAt(index) == T.corners);
    if (byCorner)
      index++;
    if (isCenterParameter(index)) {
      expressionResult = null;
      int index0 = index;
      P3 pt1 = centerParameter(index);
      index = iToken + 1;
      if (byCorner || isCenterParameter(index)) {
        // boundbox CORNERS {expressionOrPoint1} {expressionOrPoint2}
        // boundbox {expressionOrPoint1} {vector}
        P3 pt2 = (byCorner ? centerParameter(index) : getPoint3f(index,
            true));
        index = iToken + 1;
        if (!chk)
          viewer.setBoundBox(pt1, pt2, byCorner, scale);
      } else if (expressionResult != null && expressionResult instanceof BS) {
        // boundbox {expression}
        if (!chk)
          viewer.calcBoundBoxDimensions((BS) expressionResult, scale);
      } else if (expressionResult == null && tokAt(index0) == T.dollarsign) {
        if (chk)
          return;
        P3[] bbox = getObjectBoundingBox(objectNameParameter(++index0));
        if (bbox == null)
          error(ERROR_invalidArgument);
        viewer.setBoundBox(bbox[0], bbox[1], true, scale);
        index = iToken + 1;
      } else {
        error(ERROR_invalidArgument);
      }
      if (index == slen)
        return;
    }
    int mad = getSetAxesTypeMad(index);
    if (chk)
      return;
    if (tickInfo != null)
      setShapeProperty(JC.SHAPE_BBCAGE, "tickInfo", tickInfo);
    setObjectMad(JC.SHAPE_BBCAGE, "boundbox", mad);
  }

  /**
   * 
   * @param index
   * @param allowUnitCell
   *        IGNORED
   * @param allowScale
   * @param allowFirst
   * @return TickInfo
   * @throws ScriptException
   */
  private TickInfo checkTicks(int index, boolean allowUnitCell,
                              boolean allowScale, boolean allowFirst)
      throws ScriptException {
    iToken = index - 1;
    if (tokAt(index) != T.ticks)
      return null;
    TickInfo tickInfo;
    String str = " ";
    switch (tokAt(index + 1)) {
    case T.x:
    case T.y:
    case T.z:
      str = parameterAsString(++index).toLowerCase();
      break;
    case T.identifier:
      error(ERROR_invalidArgument);
    }
    if (tokAt(++index) == T.none) {
      tickInfo = new TickInfo(null);
      tickInfo.type = str;
      iToken = index;
      return tickInfo;
    }
    tickInfo = new TickInfo((P3) getPointOrPlane(index, false, true,
        false, false, 3, 3));
    if (coordinatesAreFractional || tokAt(iToken + 1) == T.unitcell) {
      tickInfo.scale = P3.new3(Float.NaN, Float.NaN, Float.NaN);
      allowScale = false;
    }
    if (tokAt(iToken + 1) == T.unitcell)
      iToken++;
    tickInfo.type = str;
    if (tokAt(iToken + 1) == T.format)
      tickInfo.tickLabelFormats = stringParameterSet(iToken + 2);
    if (!allowScale)
      return tickInfo;
    if (tokAt(iToken + 1) == T.scale) {
      if (isFloatParameter(iToken + 2)) {
        float f = floatParameter(iToken + 2);
        tickInfo.scale = P3.new3(f, f, f);
      } else {
        tickInfo.scale = getPoint3f(iToken + 2, true);
      }
    }
    if (allowFirst)
      if (tokAt(iToken + 1) == T.first)
        tickInfo.first = floatParameter(iToken + 2);
    // POINT {x,y,z} reference point not implemented
    //if (tokAt(iToken + 1) == Token.point)
    // tickInfo.reference = centerParameter(iToken + 2);
    return tickInfo;
  }

  private void unitcell(int index) throws ScriptException {
    int icell = Integer.MAX_VALUE;
    int mad = Integer.MAX_VALUE;
    P3 pt = null;
    TickInfo tickInfo = checkTicks(index, true, false, false);
    index = iToken;
    String id = null;
    P3[] points = null;
    switch (tokAt(index + 1)) {
    case T.string:
      id = objectNameParameter(++index);
      break;
    case T.dollarsign:
      index++;
      id = objectNameParameter(++index);
      break;
    default:
      if (isArrayParameter(index + 1)) {
        points = getPointArray(++index, 4);
        index = iToken;
      }else if (slen == index + 2) {
        if (getToken(index + 1).tok == T.integer
            && intParameter(index + 1) >= 111)
          icell = intParameter(++index);
      } else if (slen > index + 1) {
        pt = (P3) getPointOrPlane(++index, false, true, false, true, 3, 3);
        index = iToken;
      }
    }
    mad = getSetAxesTypeMad(++index);
    checkLast(iToken);
    if (chk)
      return;
    if (icell != Integer.MAX_VALUE)
      viewer.setCurrentUnitCellOffset(icell);
    else if (id != null)
      viewer.setCurrentCage(id);
    else if (points != null)
      viewer.setCurrentCagePts(points);
    setObjectMad(JC.SHAPE_UCCAGE, "unitCell", mad);
    if (pt != null)
      viewer.setCurrentUnitCellOffsetPt(pt);
    if (tickInfo != null)
      setShapeProperty(JC.SHAPE_UCCAGE, "tickInfo", tickInfo);
  }

  private void frank(int index) throws ScriptException {
    setBooleanProperty("frank", booleanParameter(index));
  }

  private void selectionHalo(int pt) throws ScriptException {
    boolean showHalo = false;
    switch (pt == slen ? T.on : getToken(pt).tok) {
    case T.on:
    case T.selected:
      showHalo = true;
      //$FALL-THROUGH$
    case T.off:
    case T.none:
    case T.normal:
      setBooleanProperty("selectionHalos", showHalo);
      break;
    default:
      error(ERROR_invalidArgument);
    }
  }

  private void save() throws ScriptException {
    if (slen > 1) {
      String saveName = optParameterAsString(2);
      switch (tokAt(1)) {
      case T.rotation:
        if (!chk)
          viewer.saveOrientation(saveName);
        return;
      case T.orientation:
        if (!chk)
          viewer.saveOrientation(saveName);
        return;
      case T.bonds:
        if (!chk)
          viewer.saveBonds(saveName);
        return;
      case T.state:
        if (!chk)
          viewer.saveState(saveName);
        return;
      case T.structure:
        if (!chk)
          viewer.saveStructure(saveName);
        return;
      case T.coord:
        if (!chk)
          viewer.saveCoordinates(saveName, viewer.getSelectionSet(false));
        return;
      case T.selection:
        if (!chk)
          viewer.saveSelection(saveName);
        return;
      }
    }
    errorStr2(ERROR_what, "SAVE",
        "bonds? coordinates? orientation? selection? state? structure?");
  }

  private void restore() throws ScriptException {
    // restore orientation name time
    if (slen > 1) {
      String saveName = optParameterAsString(2);
      if (getToken(1).tok != T.orientation)
        checkLength23();
      float floatSecondsTotal;
      switch (getToken(1).tok) {
      case T.rotation:
        floatSecondsTotal = (slen > 3 ? floatParameter(3) : 0);
        if (floatSecondsTotal < 0)
          error(ERROR_invalidArgument);
        if (!chk) {
          viewer.restoreRotation(saveName, floatSecondsTotal);
          if (isJS && floatSecondsTotal > 0 && viewer.waitForMoveTo())
            throw new ScriptInterruption(this, "restoreRotation", 1);
        }
        return;
      case T.orientation:
        floatSecondsTotal = (slen > 3 ? floatParameter(3) : 0);
        if (floatSecondsTotal < 0)
          error(ERROR_invalidArgument);
        if (!chk) {
          viewer.restoreOrientation(saveName, floatSecondsTotal);
          if (isJS && floatSecondsTotal > 0 && viewer.waitForMoveTo())
            throw new ScriptInterruption(this, "restoreOrientation", 1);
        }
        return;
      case T.bonds:
        if (!chk)
          viewer.restoreBonds(saveName);
        return;
      case T.coord:
        if (chk)
          return;
        String script = viewer.getSavedCoordinates(saveName);
        if (script == null)
          error(ERROR_invalidArgument);
        runScript(script);
        viewer.checkCoordinatesChanged();
        return;
      case T.state:
        if (chk)
          return;
        String state = viewer.getSavedState(saveName);
        if (state == null)
          error(ERROR_invalidArgument);
        runScript(state);
        return;
      case T.structure:
        if (chk)
          return;
        String shape = viewer.getSavedStructure(saveName);
        if (shape == null)
          error(ERROR_invalidArgument);
        runScript(shape);
        return;
      case T.selection:
        if (!chk)
          viewer.restoreSelection(saveName);
        return;
      }
    }
    errorStr2(ERROR_what, "RESTORE",
        "bonds? coords? orientation? selection? state? structure?");
  }

  String write(T[] args) throws ScriptException {
    int pt = 0, pt0 = 0;
    boolean isCommand, isShow;
    if (args == null) {
      args = st;
      pt = pt0 = 1;
      isCommand = true;
      isShow = (viewer.isApplet() && !viewer.isSignedApplet()
          || !viewer.isRestricted(ACCESS.ALL) || viewer.getPathForAllFiles()
          .length() > 0);
    } else {
      isCommand = false;
      isShow = true;
    }
    int argCount = (isCommand ? slen : args.length);
    int len = 0;
    int nVibes = 0;
    int width = -1;
    int height = -1;
    int quality = Integer.MIN_VALUE;
    String driverList = viewer.getExportDriverList();
    String sceneType = "PNGJ";
    String data = null;
    String type2 = "";
    String fileName = null;
    String localPath = null;
    String remotePath = null;
    String val = null;
    String msg = null;
    String[] fullPath = new String[1];
    boolean isCoord = false;
    boolean isExport = false;
    boolean isImage = false;
    BS bsFrames = null;
    String[] scripts = null;
    String type = "SPT";
    int tok = (isCommand && args.length == 1 ? T.clipboard : tokAtArray(pt,
        args));
    switch (tok) {
    case T.nada:
      break;
    case T.script:
      if (isArrayParameter(pt + 1)) {
        scripts = stringParameterSet(++pt);
        localPath = ".";
        remotePath = ".";
        pt0 = pt = iToken + 1;
        tok = tokAt(pt);
      }
      break;
    default:
      type = SV.sValue(tokenAt(pt, args)).toUpperCase();
    }
    switch (tok) {
    case T.nada:
      break;
    case T.quaternion:
    case T.ramachandran:
    case T.property:
      msg = plot(args);
      if (!isCommand)
        return msg;
      break;
    case T.inline:
      type = "INLINE";
      data = SV.sValue(tokenAt(++pt, args));
      pt++;
      break;
    case T.pointgroup:
      type = "PGRP";
      pt++;
      type2 = SV.sValue(tokenAt(pt, args)).toLowerCase();
      if (type2.equals("draw"))
        pt++;
      break;
    case T.coord:
      pt++;
      isCoord = true;
      break;
    case T.state:
    case T.script:
      val = SV.sValue(tokenAt(++pt, args)).toLowerCase();
      while (val.equals("localpath") || val.equals("remotepath")) {
        if (val.equals("localpath"))
          localPath = SV.sValue(tokenAt(++pt, args));
        else
          remotePath = SV.sValue(tokenAt(++pt, args));
        val = SV.sValue(tokenAt(++pt, args)).toLowerCase();
      }
      type = "SPT";
      break;
    case T.file:
    case T.function:
    case T.history:
    case T.isosurface:
    case T.menu:
    case T.mesh:
    case T.mo:
    case T.pmesh:
      pt++;
      break;
    case T.jmol:
      type = "ZIPALL";
      pt++;
      break;
    case T.var:
      type = "VAR";
      pt += 2;
      break;
    case T.frame:
    case T.identifier:
    case T.image:
    case T.scene:
    case T.string:
    case T.vibration:
      switch (tok) {
      case T.image:
        pt++;
        break;
      case T.vibration:
        nVibes = intParameterRange(++pt, 1, 10);
        if (!chk) {
          viewer.setVibrationOff();
          if (!isJS)
            viewer.delayScript(this, 100);          
        }
        pt++;
        break;
      case T.frame:
        BS bsAtoms;
        if (pt + 1 < argCount && args[++pt].tok == T.expressionBegin
            || args[pt].tok == T.bitset) {
          bsAtoms = atomExpression(args, pt, 0, true, false, true, true);
          pt = iToken + 1;
        } else {
          bsAtoms = viewer.getModelUndeletedAtomsBitSet(-1);
        }
        if (!chk)
          bsFrames = viewer.getModelBitSet(bsAtoms, true);
        break;
      case T.scene:
        val = SV.sValue(tokenAt(++pt, args)).toUpperCase();
        if (Parser.isOneOf(val, "PNG;PNGJ")) {
          sceneType = val;
          pt++;
        }
        break;
      default:
      case T.string:
        T t = T.getTokenFromName(SV.sValue(args[pt])
            .toLowerCase());
        if (t != null) {
          tok = t.tok;
          type = SV.sValue(t).toUpperCase();
        }
        if (Parser.isOneOf(type, driverList.toUpperCase())) {
          // povray, maya, vrml, idtf
          pt++;
          type = type.substring(0, 1).toUpperCase()
              + type.substring(1).toLowerCase();
          // Povray, Maya, Vrml, Idtf
          isExport = true;
          if (isCommand)
            fileName = "Jmol." + type;
        } else if (type.equals("ZIP")) {
          pt++;
        } else if (type.equals("ZIPALL")) {
          pt++;
        } else {
          type = "(image)";
        }
        break;
      }
      if (tokAtArray(pt, args) == T.integer) {
        width = SV.iValue(tokenAt(pt++, args));
        height = SV.iValue(tokenAt(pt++, args));
      }
      break;
    }

    if (msg == null) {
      val = SV.sValue(tokenAt(pt, args));
      if (val.equalsIgnoreCase("clipboard")) {
        if (chk)
          return "";
        // if (isApplet)
        // evalError(GT._("The {0} command is not available for the applet.",
        // "WRITE CLIPBOARD"));
      } else if (Parser.isOneOf(val.toLowerCase(),
          "png;pngj;pngt;jpg;jpeg;jpg64;jpeg64")
          && tokAtArray(pt + 1, args) == T.integer) {
        quality = SV.iValue(tokenAt(++pt, args));
      } else if (Parser.isOneOf(val.toLowerCase(),
          "xyz;xyzrn;xyzvib;mol;sdf;v2000;v3000;cd;pdb;pqr;cml")) {
        type = val.toUpperCase();
        if (pt + 1 == argCount)
          pt++;
      }

      // write [image|history|state] clipboard

      // write [optional image|history|state] [JPG quality|JPEG quality|JPG64
      // quality|PNG|PPM|SPT] "filename"
      // write script "filename"
      // write isosurface t.jvxl

      if (type.equals("(image)")
          && Parser.isOneOf(val.toUpperCase(),
              "GIF;JPG;JPG64;JPEG;JPEG64;PNG;PNGJ;PNGT;PPM")) {
        type = val.toUpperCase();
        pt++;
      }

      if (pt + 2 == argCount) {
        data = SV.sValue(tokenAt(++pt, args));
        if (data.length() > 0 && data.charAt(0) != '.')
          type = val.toUpperCase();
      }
      switch (tokAtArray(pt, args)) {
      case T.nada:
        isShow = true;
        break;
      case T.clipboard:
        break;
      case T.identifier:
      case T.string:
        fileName = SV.sValue(tokenAt(pt, args));
        if (pt == argCount - 3 && tokAtArray(pt + 1, args) == T.per) {
          // write filename.xxx gets separated as filename .spt
          // write isosurface filename.xxx also
          fileName += "." + SV.sValue(tokenAt(pt + 2, args));
        }
        if (type != "VAR" && pt == pt0)
          type = "IMAGE";
        else if (fileName.length() > 0 && fileName.charAt(0) == '.'
            && (pt == pt0 + 1 || pt == pt0 + 2)) {
          fileName = SV.sValue(tokenAt(pt - 1, args)) + fileName;
          if (type != "VAR" && pt == pt0 + 1)
            type = "IMAGE";
        }
        if (fileName.equalsIgnoreCase("clipboard")
            || !viewer.isRestricted(ACCESS.ALL))
          fileName = null;
        break;
      default:
        error(ERROR_invalidArgument);
      }
      if (type.equals("IMAGE") || type.equals("FRAME")
          || type.equals("VIBRATION")) {
        type = (fileName != null && fileName.indexOf(".") >= 0 ? fileName
            .substring(fileName.lastIndexOf(".") + 1).toUpperCase() : "JPG");
      }
      if (type.equals("MNU")) {
        type = "MENU";
      } else if (type.equals("WRL") || type.equals("VRML")) {
        type = "Vrml";
        isExport = true;
      } else if (type.equals("X3D")) {
        type = "X3d";
        isExport = true;
      } else if (type.equals("IDTF")) {
        type = "Idtf";
        isExport = true;
      } else if (type.equals("MA")) {
        type = "Maya";
        isExport = true;
      } else if (type.equals("JS")) {
        type = "Js";
        isExport = true;
      } else if (type.equals("OBJ")) {
        type = "Obj";
        isExport = true;
      } else if (type.equals("JVXL")) {
        type = "ISOSURFACE";
      } else if (type.equals("XJVXL")) {
        type = "ISOSURFACE";
      } else if (type.equals("JMOL")) {
        type = "ZIPALL";
      } else if (type.equals("HIS")) {
        type = "HISTORY";
      }
      if (type.equals("COORD"))
        type = (fileName != null && fileName.indexOf(".") >= 0 ? fileName
            .substring(fileName.lastIndexOf(".") + 1).toUpperCase() : "XYZ");
      isImage = Parser.isOneOf(type,
          "GIF;JPEG64;JPEG;JPG64;JPG;PPM;PNG;PNGJ;PNGT;SCENE");
      if (scripts != null) {
        if (type.equals("PNG"))
          type = "PNGJ";
        if (!type.equals("PNGJ") && !type.equals("ZIPALL"))
          error(ERROR_invalidArgument);
      }
      if (isImage && isShow)
        type = "JPG64";
      else if (!isImage
          && !isExport
          && !Parser
              .isOneOf(
                  type,
                  "SCENE;JMOL;ZIP;ZIPALL;SPT;HISTORY;MO;ISOSURFACE;MESH;PMESH;VAR;FILE;FUNCTION;CD;CML;XYZ;XYZRN;XYZVIB;MENU;MOL;PDB;PGRP;PQR;QUAT;RAMA;SDF;V2000;V3000;INLINE"))
        errorStr2(
            ERROR_writeWhat,
            "COORDS|FILE|FUNCTIONS|HISTORY|IMAGE|INLINE|ISOSURFACE|JMOL|MENU|MO|POINTGROUP|QUATERNION [w,x,y,z] [derivative]"
                + "|RAMACHANDRAN|SPT|STATE|VAR x|ZIP|ZIPALL  CLIPBOARD",
            "CML|GIF|JPG|JPG64|JMOL|JVXL|MESH|MOL|PDB|PMESH|PNG|PNGJ|PNGT|PPM|PQR|SDF|V2000|V3000|SPT|XJVXL|XYZ|XYZRN|XYZVIB|ZIP"
                + driverList.toUpperCase().replace(';', '|'));
      if (chk)
        return "";
      Object bytes = null;
      boolean doDefer = false;
      if (data == null || isExport) {
        data = type.intern();
        if (isExport) {
          // POV-Ray uses a BufferedWriter instead of a StringXBuilder.
          // todo -- there's no reason this data has to be done this way. 
          // we could send all of them out to file directly
          fullPath[0] = fileName;
          data = viewer.generateOutputForExport(data,
              isCommand || fileName != null ? fullPath : null, width, height);
          if (data == null || data.length() == 0)
            return "";
          if (!isCommand)
            return data;
          if ((type.equals("Povray") || type.equals("Idtf"))
              && fullPath[0] != null) {
            String ext = (type.equals("Idtf") ? ".tex" : ".ini");
            fileName = fullPath[0] + ext;
            msg = viewer.createImageSet(fileName, ext, data, null, null,
                Integer.MIN_VALUE, 0, 0, null, 0, fullPath);
            if (type.equals("Idtf"))
              data = data.substring(0, data.indexOf("\\begin{comment}"));
            data = "Created " + fullPath[0] + ":\n\n" + data;
          } else {
            msg = data;
          }
          if (msg != null) {
            if (!msg.startsWith("OK"))
              evalError(msg, null);
            scriptStatusOrBuffer(data);
          }
          return "";
        } else if (data == "MENU") {
          data = viewer.getMenu("");
        } else if (data == "PGRP") {
          data = viewer.getPointGroupAsString(type2.equals("draw"), null, 0,
              1.0f);
        } else if (data == "PDB" || data == "PQR") {
          if (isShow) {
            data = viewer.getPdbData(null, null);
          } else {
            doDefer = true;
            /*
             * OutputStream os = viewer.getOutputStream(fileName, fullPath); msg =
             * viewer.getPdbData(null, new BufferedOutputStream(os)); if (msg !=
             * null) msg = "OK " + msg + " " + fullPath[0]; try { os.close(); }
             * catch (IOException e) { // TODO }
             */
          }
        } else if (data == "FILE") {
          if (isShow)
            data = viewer.getCurrentFileAsString();
          else
            doDefer = true;
          if ("?".equals(fileName))
            fileName = "?Jmol." + viewer.getParameter("_fileType");
        } else if ((data == "SDF" || data == "MOL" || data == "V2000"
            || data == "V3000" || data == "CD")
            && isCoord) {
          data = viewer.getModelExtract("selected", true, false, data);
          if (data.startsWith("ERROR:"))
            bytes = data;
        } else if (data == "XYZ" || data == "XYZRN" || data == "XYZVIB"
            || data == "MOL" || data == "SDF" || data == "V2000"
            || data == "V3000" || data == "CML" || data == "CD") {
          data = viewer.getData("selected", data);
          if (data.startsWith("ERROR:"))
            bytes = data;
        } else if (data == "FUNCTION") {
          data = viewer.getFunctionCalls(null);
          type = "TXT";
        } else if (data == "VAR") {
          data = ((SV) getParameter(SV.sValue(tokenAt(
              isCommand ? 2 : 1, args)), T.variable)).asString();
          type = "TXT";
        } else if (data == "SPT") {
          if (isCoord) {
            BS tainted = viewer.getTaintedAtoms(AtomCollection.TAINT_COORD);
            viewer.setAtomCoordRelative(P3.new3(0, 0, 0), null);
            data = viewer.getStateInfo();
            viewer.setTaintedAtoms(tainted, AtomCollection.TAINT_COORD);
          } else {
            data = viewer.getStateInfo();
            if (localPath != null || remotePath != null)
              data = FileManager.setScriptFileReferences(data, localPath,
                  remotePath, null);
          }
        } else if (data == "ZIP" || data == "ZIPALL") {
          data = viewer.getStateInfo();
          bytes = viewer.createZip(fileName, type, data, scripts);
        } else if (data == "HISTORY") {
          data = viewer.getSetHistory(Integer.MAX_VALUE);
          type = "SPT";
        } else if (data == "MO") {
          data = getMoJvxl(Integer.MAX_VALUE);
          type = "XJVXL";
        } else if (data == "PMESH") {
          if ((data = getIsosurfaceJvxl(true, JC.SHAPE_PMESH)) == null)
            error(ERROR_noData);
          type = "XJVXL";
        } else if (data == "ISOSURFACE" || data == "MESH") {
          if ((data = getIsosurfaceJvxl(data == "MESH",
              JC.SHAPE_ISOSURFACE)) == null)
            error(ERROR_noData);
          type = (data.indexOf("<?xml") >= 0 ? "XJVXL" : "JVXL");
          if (!isShow)
            showString((String) getShapeProperty(
                JC.SHAPE_ISOSURFACE, "jvxlFileInfo"));
        } else {
          // image
          len = -1;
          if (quality < 0)
            quality = -1;
        }
        if (data == null && !doDefer)
          data = "";
        if (len == 0 && !doDefer)
          len = (bytes == null ? data.length()
              : bytes instanceof String ? ((String) bytes).length()
                  : ((byte[]) bytes).length);
        if (isImage) {
          refresh();
          if (width < 0)
            width = viewer.getScreenWidth();
          if (height < 0)
            height = viewer.getScreenHeight();
        }
      }
      if (!isCommand)
        return data;
      if (isShow) {
        showStringPrint(data, true);
        return "";
      }
      if (bytes != null && bytes instanceof String) {
        // load error or completion message here
        scriptStatusOrBuffer((String) bytes);
        return (String) bytes;
      }
      if (type.equals("SCENE"))
        bytes = sceneType;
      else if (bytes == null && (!isImage || fileName != null))
        bytes = data;
      if (doDefer)
        msg = viewer.streamFileData(fileName, type, type2, 0, null);
      else
        msg = viewer.createImageSet(fileName, type, 
            (bytes instanceof String ? (String) bytes : null), 
            (bytes instanceof byte[] ? (byte[]) bytes : null), 
            scripts, quality,
            width, height, bsFrames, nVibes, fullPath);
    }
    if (!chk && msg != null) {
      if (!msg.startsWith("OK"))
        evalError(msg, null);
      scriptStatusOrBuffer(msg
          + (isImage ? "; width=" + width + "; height=" + height : ""));
      return msg;
    }
    return "";
  }

  
  private void show() throws ScriptException {
    String value = null;
    String str = parameterAsString(1);
    String msg = null;
    String name = null;
    int len = 2;
    T token = getToken(1);
    int tok = (token instanceof SV ? T.nada : token.tok);
    if (tok == T.string) {
      token = T.getTokenFromName(str.toLowerCase());
      if (token != null)
        tok = token.tok;
    }
    if (tok != T.symop && tok != T.state)
      checkLength(-3);
    if (slen == 2 && str.indexOf("?") >= 0) {
      showString(viewer.getAllSettings(str.substring(0, str.indexOf("?"))));
      return;
    }
    switch (tok) {
    case T.nada:
      if (!chk)
        msg = ((SV) theToken).escape();
      break;
    case T.cache:
      if (!chk)
        msg = Escape.e(viewer.cacheList());
      break;
    case T.dssp:
      checkLength(2);
      if (!chk)
        msg = viewer.calculateStructures(null, true, false);
      break;
    case T.pathforallfiles:
      checkLength(2);
      if (!chk)
        msg = viewer.getPathForAllFiles();
      break;
    case T.nmr:
    case T.smiles:
    case T.drawing:
    case T.chemical:
      checkLength(tok == T.chemical ? 3 : 2);
      if (chk)
        return;
      msg = viewer.getSmiles(0, 0, viewer.getSelectionSet(false), false, true,
          false, false);
      switch (tok) {
      case T.drawing:
        if (msg.length() > 0) {
          viewer.show2D(msg);
          return;
        }
        msg = "Could not show drawing -- Either insufficient atoms are selected or the model is a PDB file.";
        break;
      case T.nmr:
        if (msg.length() > 0) {
          viewer.showNMR(msg);
          return;
        }
        msg = "Could not show nmr -- Either insufficient atoms are selected or the model is a PDB file.";
        break;
      case T.chemical:
        len = 3;
        String info = null;
        if (msg.length() > 0) {
          char type = '/';
          switch (getToken(2).tok) {
          case T.inchi:
            type = 'I';
            break;
          case T.inchikey:
            type = 'K';
            break;
          case T.name:
            type = 'N';
            break;
          default:
            info = parameterAsString(2);
          }
          msg = viewer.getChemicalInfo(msg, type, info);
          if (msg.indexOf("FileNotFound") >= 0)
            msg = "?";
        } else {
          msg = "Could not show name -- Either insufficient atoms are selected or the model is a PDB file.";
        }
      }
      break;
    case T.symop:
      if (slen > 3) {
        P3 pt1 = centerParameter(2);
        P3 pt2 = centerParameter(++iToken);
        if (!chk)
          msg = viewer.getSymmetryOperation(null, 0, pt1, pt2, false);
        len = ++iToken;
      } else {
        int iop = (checkLength23() == 2 ? 0 : intParameter(2));
        if (!chk)
          msg = viewer.getSymmetryOperation(null, iop, null, null, false);
        len = -3;
      }
      break;
    case T.vanderwaals:
      EnumVdw vdwType = null;
      if (slen > 2) {
        vdwType = EnumVdw.getVdwType(parameterAsString(2));
        if (vdwType == null)
          error(ERROR_invalidArgument);
      }
      if (!chk)
        showString(viewer.getDefaultVdwTypeNameOrData(0, vdwType));
      return;
    case T.function:
      checkLength23();
      if (!chk)
        showString(viewer.getFunctionCalls(optParameterAsString(2)));
      return;
    case T.set:
      checkLength(2);
      if (!chk)
        showString(viewer.getAllSettings(null));
      return;
    case T.url:
      // in a new window
      if ((len = slen) == 2) {
        if (!chk)
          viewer.showUrl(getFullPathName());
        return;
      }
      name = parameterAsString(2);
      if (!chk)
        viewer.showUrl(name);
      return;
    case T.color:
      str = "defaultColorScheme";
      break;
    case T.scale3d:
      str = "scaleAngstromsPerInch";
      break;
    case T.quaternion:
    case T.ramachandran:
      if (chk)
        return;
      int modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        errorStr(ERROR_multipleModelsDisplayedNotOK, "show " + theToken.value);
      msg = plot(st);
      len = slen;
      break;
    case T.trace:
      if (!chk)
        msg = getContext(false);
      break;
    case T.colorscheme:
      name = optParameterAsString(2);
      if (name.length() > 0)
        len = 3;
      if (!chk)
        value = viewer.getColorSchemeList(name);
      break;
    case T.variables:
      if (!chk)
        msg = viewer.getAtomDefs(definedAtomSets)
          + viewer.getVariableList() + getContext(true);
      break;
    case T.trajectory:
      if (!chk)
        msg = viewer.getTrajectoryState();
      break;
    case T.historylevel:
      value = "" + commandHistoryLevelMax;
      break;
    case T.loglevel:
      value = "" + Logger.getLogLevel();
      break;
    case T.debugscript:
      value = "" + viewer.getDebugScript();
      break;
    case T.strandcount:
      msg = "set strandCountForStrands "
          + viewer.getStrandCount(JC.SHAPE_STRANDS)
          + "; set strandCountForMeshRibbon "
          + viewer.getStrandCount(JC.SHAPE_MESHRIBBON);
      break;
    case T.timeout:
      msg = viewer.showTimeout((len = slen) == 2 ? null
          : parameterAsString(2));
      break;
    case T.defaultlattice:
      value = Escape.eP(viewer.getDefaultLattice());
      break;
    case T.minimize:
      if (!chk)
        msg = viewer.getMinimizationInfo();
      break;
    case T.axes:
      switch (viewer.getAxesMode()) {
      case UNITCELL:
        msg = "set axesUnitcell";
        break;
      case BOUNDBOX:
        msg = "set axesWindow";
        break;
      default:
        msg = "set axesMolecular";
      }
      break;
    case T.bondmode:
      msg = "set bondMode " + (viewer.getBondSelectionModeOr() ? "OR" : "AND");
      break;
    case T.strands:
      if (!chk)
        msg = "set strandCountForStrands "
            + viewer.getStrandCount(JC.SHAPE_STRANDS)
            + "; set strandCountForMeshRibbon "
            + viewer.getStrandCount(JC.SHAPE_MESHRIBBON);
      break;
    case T.hbond:
      msg = "set hbondsBackbone " + viewer.getHbondsBackbone()
          + ";set hbondsSolid " + viewer.getHbondsSolid();
      break;
    case T.spin:
      if (!chk)
        msg = viewer.getSpinState();
      break;
    case T.ssbond:
      msg = "set ssbondsBackbone " + viewer.getSsbondsBackbone();
      break;
    case T.display:// deprecated
    case T.selectionhalos:
      msg = "selectionHalos "
          + (viewer.getSelectionHaloEnabled(false) ? "ON" : "OFF");
      break;
    case T.hetero:
      msg = "set selectHetero " + viewer.getRasmolSetting(tok);
      break;
    case T.addhydrogens:
      msg = Escape.escapeAP(viewer.getAdditionalHydrogens(null, true, true,
          null));
      break;
    case T.hydrogen:
      msg = "set selectHydrogens " + viewer.getRasmolSetting(tok);
      break;
    case T.ambientpercent:
    case T.diffusepercent:
    case T.specular:
    case T.specularpower:
    case T.specularexponent:
    case T.lighting:
      if (!chk)
        msg = viewer.getSpecularState();
      break;
    case T.save:
      if (!chk)
        msg = viewer.listSavedStates();
      break;
    case T.unitcell:
      if (!chk)
        msg = viewer.getUnitCellInfoText();
      break;
    case T.coord:
      if ((len = slen) == 2) {
        if (!chk)
          msg = viewer.getCoordinateState(viewer.getSelectionSet(false));
        break;
      }
      String nameC = parameterAsString(2);
      if (!chk)
        msg = viewer.getSavedCoordinates(nameC);
      break;
    case T.state:
      if (!chk)
        viewer.clearConsole();
      if ((len = slen) == 2) {
        if (!chk)
          msg = viewer.getStateInfo();
        break;
      }
      name = parameterAsString(2);
      if (name.equals("/") && (len = slen) == 4) {
        name = parameterAsString(3).toLowerCase();
        if (!chk) {
          String[] info = TextFormat.split(viewer.getStateInfo(), '\n');
          SB sb = new SB();
          for (int i = 0; i < info.length; i++)
            if (info[i].toLowerCase().indexOf(name) >= 0)
              sb.append(info[i]).appendC('\n');
          msg = sb.toString();
        }
        break;
      } else if (tokAt(2) == T.file && (len = slen) == 4) {
        if (!chk)
          msg = viewer.getEmbeddedFileState(parameterAsString(3));
        break;
      }
      len = 3;
      if (!chk)
        msg = viewer.getSavedState(name);
      break;
    case T.structure:
      if ((len = slen) == 2) {
        if (!chk)
          msg = viewer.getProteinStructureState();
        break;
      }
      String shape = parameterAsString(2);
      if (!chk)
        msg = viewer.getSavedStructure(shape);
      break;
    case T.data:
      String type = ((len = slen) == 3 ? parameterAsString(2) : null);
      if (!chk) {
        Object[] data = (type == null ? this.data : viewer.getData(type));
        msg = (data == null ? "no data" : 
          Escape.encapsulateData((String)data[0], data[1], ((Integer)data[3]).intValue()));
      }
      break;
    case T.spacegroup:
      Map<String, Object> info = null;
      if ((len = slen) == 2) {
        if (!chk) {
          info = viewer.getSpaceGroupInfo(null);
        }
      } else {
        String sg = parameterAsString(2);
        if (!chk)
          info = viewer.getSpaceGroupInfo(TextFormat.simpleReplace(sg, "''",
              "\""));
      }
      if (info != null)
        msg = "" + info.get("spaceGroupInfo") + info.get("symmetryInfo");
      break;
    case T.dollarsign:
      len = 3;
      msg = setObjectProperty();
      break;
    case T.boundbox:
      if (!chk) {
        msg = viewer.getBoundBoxCommand(true);
      }
      break;
    case T.center:
      if (!chk)
        msg = "center " + Escape.eP(viewer.getRotationCenter());
      break;
    case T.draw:
      if (!chk)
        msg = (String) getShapeProperty(JC.SHAPE_DRAW, "command");
      break;
    case T.file:
      // as a string
      if (!chk)
        viewer.clearConsole();
      if (slen == 2) {
        if (!chk)
          msg = viewer.getCurrentFileAsString();
        if (msg == null)
          msg = "<unavailable>";
        break;
      }
      len = 3;
      value = parameterAsString(2);
      if (!chk)
        msg = viewer.getFileAsString(value);
      break;
    case T.frame:
      if (tokAt(2) == T.all && (len = 3) > 0)
        msg = viewer.getModelFileInfoAll();
      else
        msg = viewer.getModelFileInfo();
      break;
    case T.history:
      int n = ((len = slen) == 2 ? Integer.MAX_VALUE
          : intParameter(2));
      if (n < 1)
        error(ERROR_invalidArgument);
      if (!chk) {
        viewer.clearConsole();
        if (scriptLevel == 0)
          viewer.removeCommand();
        msg = viewer.getSetHistory(n);
      }
      break;
    case T.isosurface:
      if (!chk)
        msg = (String) getShapeProperty(JC.SHAPE_ISOSURFACE,
            "jvxlDataXml");
      break;
    case T.mo:
      if (optParameterAsString(2).equalsIgnoreCase("list")) {
        msg = viewer.getMoInfo(-1);
        len = 3;
      } else {
        int ptMO = ((len = slen) == 2 ? Integer.MIN_VALUE
            : intParameter(2));
        if (!chk)
          msg = getMoJvxl(ptMO);
      }
      break;
    case T.model:
      if (!chk)
        msg = viewer.getModelInfoAsString();
      break;
    case T.measurements:
      if (!chk)
        msg = viewer.getMeasurementInfoAsString();
      break;
    case T.translation:
    case T.rotation:
    case T.moveto:
      if (!chk)
        msg = viewer.getOrientationText(tok, null);
      break;
    case T.orientation:
      len = 2;
      if (slen > 3)
        break;
      switch (tok = tokAt(2)) {
      case T.translation:
      case T.rotation:
      case T.moveto:
      case T.nada:
        if (!chk)
          msg = viewer.getOrientationText(tok, null);
        break;
      default:
        name = optParameterAsString(2);
        msg = viewer.getOrientationText(0, name);
      }
      len = slen;
      break;
    case T.pdbheader:
      if (!chk)
        msg = viewer.getPDBHeader();
      break;
    case T.pointgroup:
      pointGroup();
      return;
    case T.symmetry:
      if (!chk)
        msg = viewer.getSymmetryInfoAsString();
      break;
    case T.transform:
      if (!chk)
        msg = "transform:\n" + viewer.getTransformText();
      break;
    case T.zoom:
      msg = "zoom "
          + (viewer.getZoomEnabled() ? ("" + viewer.getZoomSetting()) : "off");
      break;
    case T.frank:
      msg = (viewer.getShowFrank() ? "frank ON" : "frank OFF");
      break;
    case T.radius:
      str = "solventProbeRadius";
      break;
    // Chime related
    case T.basepair:
    case T.chain:
    case T.sequence:
    case T.residue:
    case T.selected:
    case T.group:
    case T.atoms:
    case T.info:
    case T.bonds:
      msg = viewer.getChimeInfo(tok);
      break;
    // not implemented
    case T.echo:
    case T.fontsize:
    case T.property: // huh? why?
    case T.help:
    case T.solvent:
      value = "?";
      break;
    case T.identifier:
      if (str.equalsIgnoreCase("fileHeader")) {
        if (!chk)
          msg = viewer.getPDBHeader();
      } else if (str.equalsIgnoreCase("menu")) {
        if (!chk)
          value = viewer.getMenu("");
      } else if (str.equalsIgnoreCase("mouse")) {
        String qualifiers = ((len = slen) == 2 ? null
            : parameterAsString(2));
        if (!chk)
          msg = viewer.getBindingInfo(qualifiers);
      }
      break;
    }
    checkLength(len);
    if (chk)
      return;
    if (msg != null)
      showString(msg);
    else if (value != null)
      showString(str + " = " + value);
    else if (str != null) {
      if (str.indexOf(" ") >= 0)
        showString(str);
      else
        showString(str + " = " + getParameterEscaped(str));
    }
  }

  private String getIsosurfaceJvxl(boolean asMesh, int iShape) {
    if (chk)
      return "";
    return (String) getShapeProperty(iShape, asMesh ? "jvxlMeshX"
        : "jvxlDataXml");
  }

  @SuppressWarnings("unchecked")
  private String getMoJvxl(int ptMO) throws ScriptException {
    // 0: all; Integer.MAX_VALUE: current;
    sm.loadShape(JC.SHAPE_MO);
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      errorStr(ERROR_multipleModelsDisplayedNotOK, "MO isosurfaces");
    Map<String, Object> moData = (Map<String, Object>) viewer
        .getModelAuxiliaryInfoValue(modelIndex, "moData");
    if (moData == null)
      error(ERROR_moModelError);
    Integer n = (Integer) getShapeProperty(JC.SHAPE_MO, "moNumber");
    if (n == null || n.intValue() == 0) {
      setShapeProperty(JC.SHAPE_MO, "init", Integer
          .valueOf(modelIndex));
    } else if (ptMO == Integer.MAX_VALUE) {
    }
    setShapeProperty(JC.SHAPE_MO, "moData", moData);
    return (String) getShapePropertyIndex(JC.SHAPE_MO, "showMO", ptMO);
  }

  private void draw() throws ScriptException {
    sm.loadShape(JC.SHAPE_DRAW);
    switch (tokAt(1)) {
    case T.list:
      if (listIsosurface(JC.SHAPE_DRAW))
        return;
      break;
    case T.pointgroup:
      pointGroup();
      return;
    case T.helix:
    case T.quaternion:
    case T.ramachandran:
      plot(st);
      return;
    }
    boolean havePoints = false;
    boolean isInitialized = false;
    boolean isSavedState = false;
    boolean isTranslucent = false;
    boolean isIntersect = false;
    boolean isFrame = false;
    Point4f plane;
    int tokIntersect = 0;
    float translucentLevel = Float.MAX_VALUE;
    int colorArgb = Integer.MIN_VALUE;
    int intScale = 0;
    String swidth = "";
    int iptDisplayProperty = 0;
    P3 center = null;
    String thisId = initIsosurface(JC.SHAPE_DRAW);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_DRAW, "ID") == null);
    int[] connections = null;
    int iConnect = 0;
    for (int i = iToken; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case T.unitcell:
      case T.boundbox:
        if (chk)
          break;
        JmolList<Object> vp = viewer.getPlaneIntersection(theTok, null,
            intScale / 100f, 0);
        intScale = 0;
        propertyName = "polygon";
        propertyValue = vp;
        havePoints = true;
        break;
      case T.connect:
        connections = new int[4];
        iConnect = 4;
        float[] farray = floatParameterSet(++i, 4, 4);
        i = iToken;
        for (int j = 0; j < 4; j++)
          connections[j] = (int) farray[j];
        havePoints = true;
        break;
      case T.bonds:
      case T.atoms:
        if (connections == null
            || iConnect > (theTok == T.bondcount ? 2 : 3)) {
          iConnect = 0;
          connections = new int[] { -1, -1, -1, -1 };
        }
        connections[iConnect++] = atomExpressionAt(++i).nextSetBit(0);
        i = iToken;
        connections[iConnect++] = (theTok == T.bonds ? atomExpressionAt(++i)
            .nextSetBit(0) : -1);
        i = iToken;
        havePoints = true;
        break;
      case T.slab:
        switch (getToken(++i).tok) {
        case T.dollarsign:
          propertyName = "slab";
          propertyValue = objectNameParameter(++i);
          i = iToken;
          havePoints = true;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        break;
      case T.intersection:
        switch (getToken(++i).tok) {
        case T.unitcell:
        case T.boundbox:
          tokIntersect = theTok;
          isIntersect = true;
          continue;
        case T.dollarsign:
          propertyName = "intersect";
          propertyValue = objectNameParameter(++i);
          i = iToken;
          isIntersect = true;
          havePoints = true;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        break;
      case T.polygon:
        propertyName = "polygon";
        havePoints = true;
        JmolList<Object> v = new  JmolList<Object>();
        int nVertices = 0;
        int nTriangles = 0;
        P3[] points = null;
        JmolList<SV> vpolygons = null;
        if (isArrayParameter(++i)) {
          points = getPointArray(i, -1);
          nVertices = points.length;
        } else {
          nVertices = Math.max(0, intParameter(i));
          points = new P3[nVertices];
          for (int j = 0; j < nVertices; j++)
            points[j] = centerParameter(++iToken);
        }
        switch (getToken(++iToken).tok) {
        case T.matrix3f:
        case T.matrix4f:
          SV sv = SV.newScriptVariableToken(theToken);
          sv.toArray();
          vpolygons = sv.getList();
          nTriangles = vpolygons.size();
          break;
        case T.varray:
          vpolygons = ((SV) theToken).getList();
          nTriangles = vpolygons.size();
          break;
        default:
          nTriangles = Math.max(0, intParameter(iToken));
        }
        int[][] polygons = ArrayUtil.newInt2(nTriangles);
        for (int j = 0; j < nTriangles; j++) {
          float[] f = (vpolygons == null ? floatParameterSet(++iToken, 3, 4)
              : SV.flistValue(vpolygons.get(j), 0));
          if (f.length < 3 || f.length > 4)
            error(ERROR_invalidArgument);
          polygons[j] = new int[] { (int) f[0], (int) f[1], (int) f[2],
              (f.length == 3 ? 7 : (int) f[3]) };
        }
        if (nVertices > 0) {
          v.addLast(points);
          v.addLast(polygons);
        } else {
          v = null;
        }
        propertyValue = v;
        i = iToken;
        break;
      case T.symop:
        String xyz = null;
        int iSym = 0;
        plane = null;
        P3 target = null;
        switch (tokAt(++i)) {
        case T.string:
          xyz = stringParameter(i);
          break;
        case T.matrix4f:
          xyz = SV.sValue(getToken(i));
          break;
        case T.integer:
        default:
          if (!isCenterParameter(i))
            iSym = intParameter(i++);
          if (isCenterParameter(i))
            center = centerParameter(i);
          if (isCenterParameter(iToken + 1))
            target = centerParameter(++iToken);
          if (chk)
            return;
          i = iToken;
        }
        BS bsAtoms = null;
        if (center == null && i + 1 < slen) {
          center = centerParameter(++i);
          // draw ID xxx symop [n or "x,-y,-z"] [optional {center}]
          // so we also check here for the atom set to get the right model
          bsAtoms = (tokAt(i) == T.bitset
              || tokAt(i) == T.expressionBegin ? atomExpressionAt(i) : null);
          i = iToken + 1;
        }
        checkLast(iToken);
        if (!chk)
          runScript((String) viewer.getSymmetryInfo(bsAtoms, xyz, iSym, center,
              target, thisId, T.draw));
        return;
      case T.frame:
        isFrame = true;
        // draw ID xxx frame {center} {q1 q2 q3 q4}
        continue;
      case T.leftbrace:
      case T.point4f:
      case T.point3f:
        // {X, Y, Z}
        if (theTok == T.point4f || !isPoint3f(i)) {
          propertyValue = getPoint4f(i);
          if (isFrame) {
            checkLast(iToken);
            if (!chk)
              runScript((Quaternion.newP4((Point4f) propertyValue)).draw(
                  (thisId == null ? "frame" : thisId), " " + swidth,
                  (center == null ? new P3() : center), intScale / 100f));
            return;
          }
          propertyName = "planedef";
        } else {
          propertyValue = center = getPoint3f(i, true);
          propertyName = "coord";
        }
        i = iToken;
        havePoints = true;
        break;
      case T.hkl:
      case T.plane:
        if (!havePoints && !isIntersect && tokIntersect == 0
            && theTok != T.hkl) {
          propertyName = "plane";
          break;
        }
        if (theTok == T.plane) {
          plane = planeParameter(++i);
        } else {
          plane = hklParameter(++i);
        }
        i = iToken;
        if (tokIntersect != 0) {
          if (chk)
            break;
          JmolList<Object> vpc = viewer.getPlaneIntersection(tokIntersect, plane,
              intScale / 100f, 0);
          intScale = 0;
          propertyName = "polygon";
          propertyValue = vpc;
        } else {
          propertyValue = plane;
          propertyName = "planedef";
        }
        havePoints = true;
        break;
      case T.linedata:
        propertyName = "lineData";
        propertyValue = floatParameterSet(++i, 0, Integer.MAX_VALUE);
        i = iToken;
        havePoints = true;
        break;
      case T.bitset:
      case T.expressionBegin:
        propertyName = "atomSet";
        propertyValue = atomExpressionAt(i);
        if (isFrame)
          center = centerParameter(i);
        i = iToken;
        havePoints = true;
        break;
      case T.varray:
        propertyName = "modelBasedPoints";
        propertyValue = SV.listValue(theToken);
        havePoints = true;
        break;
      case T.spacebeforesquare:
      case T.comma:
        break;
      case T.leftsquare:
        // [x y] or [x y %]
        propertyValue = xypParameter(i);
        if (propertyValue != null) {
          i = iToken;
          propertyName = "coord";
          havePoints = true;
          break;
        }
        if (isSavedState)
          error(ERROR_invalidArgument);
        isSavedState = true;
        break;
      case T.rightsquare:
        if (!isSavedState)
          error(ERROR_invalidArgument);
        isSavedState = false;
        break;
      case T.reverse:
        propertyName = "reverse";
        break;
      case T.string:
        propertyValue = stringParameter(i);
        propertyName = "title";
        break;
      case T.vector:
        propertyName = "vector";
        break;
      case T.length:
        propertyValue = Float.valueOf(floatParameter(++i));
        propertyName = "length";
        break;
      case T.decimal:
        // $drawObject
        propertyValue = Float.valueOf(floatParameter(i));
        propertyName = "length";
        break;
      case T.modelindex:
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(intParameter(++i));
        break;
      case T.integer:
        if (isSavedState) {
          propertyName = "modelIndex";
          propertyValue = Integer.valueOf(intParameter(i));
        } else {
          intScale = intParameter(i);
        }
        break;
      case T.scale:
        if (++i >= slen)
          error(ERROR_numberExpected);
        switch (getToken(i).tok) {
        case T.integer:
          intScale = intParameter(i);
          continue;
        case T.decimal:
          intScale = Math.round(floatParameter(i) * 100);
          continue;
        }
        error(ERROR_numberExpected);
        break;
      case T.id:
        thisId = setShapeId(JC.SHAPE_DRAW, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_DRAW, "ID") == null);
        i = iToken;
        break;
      case T.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        break;
      case T.fixed:
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case T.offset:
        P3 pt = getPoint3f(++i, true);
        i = iToken;
        propertyName = "offset";
        propertyValue = pt;
        break;
      case T.crossed:
        propertyName = "crossed";
        break;
      case T.width:
        propertyValue = Float.valueOf(floatParameter(++i));
        propertyName = "width";
        swidth = propertyName + " " + propertyValue;
        break;
      case T.line:
        propertyName = "line";
        propertyValue = Boolean.TRUE;
        break;
      case T.curve:
        propertyName = "curve";
        break;
      case T.arc:
        propertyName = "arc";
        break;
      case T.arrow:
        propertyName = "arrow";
        break;
      case T.circle:
        propertyName = "circle";
        break;
      case T.cylinder:
        propertyName = "cylinder";
        break;
      case T.vertices:
        propertyName = "vertices";
        break;
      case T.nohead:
        propertyName = "nohead";
        break;
      case T.barb:
        propertyName = "isbarb";
        break;
      case T.rotate45:
        propertyName = "rotate45";
        break;
      case T.perpendicular:
        propertyName = "perp";
        break;
      case T.radius:
      case T.diameter:
        boolean isRadius = (theTok == T.radius);
        float f = floatParameter(++i);
        if (isRadius)
          f *= 2;
        propertyValue = Float.valueOf(f);
        propertyName = (isRadius || tokAt(i) == T.decimal ? "width"
            : "diameter");
        swidth = propertyName
            + (tokAt(i) == T.decimal ? " " + f : " " + ((int) f));
        break;
      case T.dollarsign:
        // $drawObject[m]
        if ((tokAt(i + 2) == T.leftsquare || isFrame)) {
          P3 pto = center = centerParameter(i);
          i = iToken;
          propertyName = "coord";
          propertyValue = pto;
          havePoints = true;
          break;
        }
        // $drawObject
        propertyValue = objectNameParameter(++i);
        propertyName = "identifier";
        havePoints = true;
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        if (theTok != T.color)
          --i;
        if (tokAt(i + 1) == T.translucent) {
          i++;
          isTranslucent = true;
          if (isFloatParameter(i + 1))
            translucentLevel = getTranslucentLevel(++i);
        } else if (tokAt(i + 1) == T.opaque) {
          i++;
          isTranslucent = true;
          translucentLevel = 0;
        }
        if (isColorParam(i + 1)) {
          colorArgb = getArgbParam(++i);
          i = iToken;
        } else if (!isTranslucent) {
          error(ERROR_invalidArgument);
        }
        idSeen = true;
        continue;
      default:
        if (!setMeshDisplayProperty(JC.SHAPE_DRAW, 0, theTok)) {
          if (theTok == T.times || T.tokAttr(theTok, T.identifier)) {
            thisId = setShapeId(JC.SHAPE_DRAW, i, idSeen);
            i = iToken;
            break;
          }
          error(ERROR_invalidArgument);
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = iToken;
        continue;
      }
      idSeen = (theTok != T.delete);
      if (havePoints && !isInitialized && !isFrame) {
        setShapeProperty(JC.SHAPE_DRAW, "points", Integer
            .valueOf(intScale));
        isInitialized = true;
        intScale = 0;
      }
      if (havePoints && isWild)
        error(ERROR_invalidArgument);
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_DRAW, propertyName, propertyValue);
    }
    if (havePoints) {
      setShapeProperty(JC.SHAPE_DRAW, "set", connections);
    }
    if (colorArgb != Integer.MIN_VALUE)
      setShapeProperty(JC.SHAPE_DRAW, "color", Integer
          .valueOf(colorArgb));
    if (isTranslucent)
      setShapeTranslucency(JC.SHAPE_DRAW, "", "translucent",
          translucentLevel, null);
    if (intScale != 0) {
      setShapeProperty(JC.SHAPE_DRAW, "scale", Integer
          .valueOf(intScale));
    }
    if (iptDisplayProperty > 0) {
      if (!setMeshDisplayProperty(JC.SHAPE_DRAW, iptDisplayProperty,
          0))
        error(ERROR_invalidArgument);
    }
  }

  private void polyhedra() throws ScriptException {
    /*
     * needsGenerating:
     * 
     * polyhedra [number of vertices and/or basis] [at most two selection sets]
     * [optional type and/or edge] [optional design parameters]
     * 
     * OR else:
     * 
     * polyhedra [at most one selection set] [type-and/or-edge or on/off/delete]
     */
    boolean needsGenerating = false;
    boolean onOffDelete = false;
    boolean typeSeen = false;
    boolean edgeParameterSeen = false;
    boolean isDesignParameter = false;
    int lighting = 0;
    int nAtomSets = 0;
    sm.loadShape(JC.SHAPE_POLYHEDRA);
    setShapeProperty(JC.SHAPE_POLYHEDRA, "init", null);
    String setPropertyName = "centers";
    String decimalPropertyName = "radius_";
    boolean isTranslucent = false;
    float translucentLevel = Float.MAX_VALUE;
    int color = Integer.MIN_VALUE;
    for (int i = 1; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case T.delete:
      case T.on:
      case T.off:
        if (i + 1 != slen || needsGenerating || nAtomSets > 1
            || nAtomSets == 0 && "to".equals(setPropertyName))
          error(ERROR_incompatibleArguments);
        propertyName = (theTok == T.off ? "off" : theTok == T.on ? "on"
            : "delete");
        onOffDelete = true;
        break;
      case T.opEQ:
      case T.comma:
        continue;
      case T.bonds:
        if (nAtomSets > 0)
          error(ERROR_invalidParameterOrder);
        needsGenerating = true;
        propertyName = "bonds";
        break;
      case T.radius:
        decimalPropertyName = "radius";
        continue;
      case T.integer:
      case T.decimal:
        if (nAtomSets > 0 && !isDesignParameter)
          error(ERROR_invalidParameterOrder);
        if (theTok == T.integer) {
          if (decimalPropertyName == "radius_") {
            propertyName = "nVertices";
            propertyValue = Integer.valueOf(intParameter(i));
            needsGenerating = true;
            break;
          }
        }
        propertyName = (decimalPropertyName == "radius_" ? "radius"
            : decimalPropertyName);
        propertyValue = Float.valueOf(floatParameter(i));
        decimalPropertyName = "radius_";
        isDesignParameter = false;
        needsGenerating = true;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (typeSeen)
          error(ERROR_invalidParameterOrder);
        if (++nAtomSets > 2)
          error(ERROR_badArgumentCount);
        if ("to".equals(setPropertyName))
          needsGenerating = true;
        propertyName = setPropertyName;
        setPropertyName = "to";
        propertyValue = atomExpressionAt(i);
        i = iToken;
        break;
      case T.to:
        if (nAtomSets > 1)
          error(ERROR_invalidParameterOrder);
        if (tokAt(i + 1) == T.bitset 
            || tokAt(i + 1) == T.expressionBegin && !needsGenerating) {
          propertyName = "toBitSet";
          propertyValue = atomExpressionAt(++i);
          i = iToken;
          needsGenerating = true;
          break;
        } else if (!needsGenerating) {
          error(ERROR_insufficientArguments);
        }
        setPropertyName = "to";
        continue;
      case T.facecenteroffset:
        if (!needsGenerating)
          error(ERROR_insufficientArguments);
        decimalPropertyName = "faceCenterOffset";
        isDesignParameter = true;
        continue;
      case T.distancefactor:
        if (!needsGenerating)
          error(ERROR_insufficientArguments);
        decimalPropertyName = "distanceFactor";
        isDesignParameter = true;
        continue;
      case T.color:
      case T.translucent:
      case T.opaque:
        isTranslucent = false;
        if (theTok != T.color)
          --i;
        if (tokAt(i + 1) == T.translucent) {
          i++;
          isTranslucent = true;
          if (isFloatParameter(i + 1))
            translucentLevel = getTranslucentLevel(++i);
        } else if (tokAt(i + 1) == T.opaque) {
          i++;
          isTranslucent = true;
          translucentLevel = 0;
        }
        if (isColorParam(i + 1)) {
          color = getArgbParam(i);
          i = iToken;
        } else if (!isTranslucent)
          error(ERROR_invalidArgument);
        continue;
      case T.collapsed:
      case T.flat:
        propertyName = "collapsed";
        propertyValue = (theTok == T.collapsed ? Boolean.TRUE
            : Boolean.FALSE);
        if (typeSeen)
          error(ERROR_incompatibleArguments);
        typeSeen = true;
        break;
      case T.noedges:
      case T.edges:
      case T.frontedges:
        if (edgeParameterSeen)
          error(ERROR_incompatibleArguments);
        propertyName = parameterAsString(i);
        edgeParameterSeen = true;
        break;
      case T.fullylit:
        lighting = theTok;
        continue;
      default:
        if (isColorParam(i)) {
          color = getArgbParam(i);
          i = iToken;
          continue;
        }
        error(ERROR_invalidArgument);
      }
      setShapeProperty(JC.SHAPE_POLYHEDRA, propertyName,
          propertyValue);
      if (onOffDelete)
        return;
    }
    if (!needsGenerating && !typeSeen && !edgeParameterSeen && lighting == 0)
      error(ERROR_insufficientArguments);
    if (needsGenerating)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "generate", null);
    if (color != Integer.MIN_VALUE)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "colorThis", Integer
          .valueOf(color));
    if (isTranslucent)
      setShapeTranslucency(JC.SHAPE_POLYHEDRA, "", "translucentThis",
          translucentLevel, null);
    if (lighting != 0)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "token", Integer.valueOf(lighting));
    setShapeProperty(JC.SHAPE_POLYHEDRA, "init", null);
  }

  private void contact() throws ScriptException {
    sm.loadShape(JC.SHAPE_CONTACT);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_CONTACT))
      return;
    int iptDisplayProperty = 0;
    iToken = 1;
    String thisId = initIsosurface(JC.SHAPE_CONTACT);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_CONTACT,
        "ID") == null);
    BS bsA = null;
    BS bsB = null;
    BS bs = null;
    RadiusData rd = null;
    float[] params = null;
    boolean colorDensity = false;
    SB sbCommand = new SB();
    int minSet = Integer.MAX_VALUE;
    int displayType = T.plane;
    int contactType = T.nada;
    float distance = Float.NaN;
    float saProbeRadius = Float.NaN;
    boolean localOnly = true;
    Boolean intramolecular = null;
    Object userSlabObject = null;
    int colorpt = 0;
    boolean colorByType = false;
    int tok;
    boolean okNoAtoms = (iToken > 1);
    for (int i = iToken; i < slen; ++i) {
      switch (tok = getToken(i).tok) {
      // these first do not need atoms defined
      default:
        okNoAtoms = true;
        if (!setMeshDisplayProperty(JC.SHAPE_CONTACT, 0, theTok)) {
          if (theTok != T.times && !T.tokAttr(theTok, T.identifier))
            error(ERROR_invalidArgument);
          thisId = setShapeId(JC.SHAPE_CONTACT, i, idSeen);
          i = iToken;
          break;
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = iToken;
        continue;
      case T.id:
        okNoAtoms = true;
        setShapeId(JC.SHAPE_CONTACT, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_CONTACT, "ID") == null);
        i = iToken;
        break;
      case T.color:
        switch (tokAt(i + 1)) {
        case T.density:
          tok = T.nada;
          colorDensity = true;
          sbCommand.append(" color density");
          i++;
          break;
        case T.type:
          tok = T.nada;
          colorByType = true;
          sbCommand.append(" color type");
          i++;
          break;
        }
        if (tok == T.nada)
          break;
        //$FALL-THROUGH$ to translucent
      case T.translucent:
      case T.opaque:
        okNoAtoms = true;
        if (colorpt == 0)
          colorpt = i;
        setMeshDisplayProperty(JC.SHAPE_CONTACT, i, theTok);
        i = iToken;
        break;
      case T.slab:
        okNoAtoms = true;
        userSlabObject = getCapSlabObject(i, false);
        setShapeProperty(JC.SHAPE_CONTACT, "slab", userSlabObject);
        i = iToken;
        break;

      // now after this you need atoms

      case T.density:
        colorDensity = true;
        sbCommand.append(" density");
        if (isFloatParameter(i + 1)) {
          if (params == null)
            params = new float[1];
          params[0] = -Math.abs(floatParameter(++i));
          sbCommand.append(" " + -params[0]);
        }
        break;
      case T.resolution:
        float resolution = floatParameter(++i);
        if (resolution > 0) {
          sbCommand.append(" resolution ").appendF(resolution);
          setShapeProperty(JC.SHAPE_CONTACT, "resolution", Float
              .valueOf(resolution));
        }
        break;
      case T.within:
      case T.distance:
        distance = floatParameter(++i);
        sbCommand.append(" within ").appendF(distance);
        break;
      case T.plus:
      case T.integer:
      case T.decimal:
        rd = encodeRadiusParameter(i, false, false);
        sbCommand.append(" ").appendO(rd);
        i = iToken;
        break;
      case T.intermolecular:
      case T.intramolecular:
        intramolecular = (tok == T.intramolecular ? Boolean.TRUE
            : Boolean.FALSE);
        sbCommand.append(" ").appendO(theToken.value);
        break;
      case T.minset:
        minSet = intParameter(++i);
        break;
      case T.hbond:
      case T.clash:
      case T.vanderwaals:
        contactType = tok;
        sbCommand.append(" ").appendO(theToken.value);
        break;
      case T.sasurface:
        if (isFloatParameter(i + 1))
          saProbeRadius = floatParameter(++i);
        //$FALL-THROUGH$
      case T.cap:
      case T.nci:
      case T.surface:
        localOnly = false;
        //$FALL-THROUGH$
      case T.trim:
      case T.full:
      case T.plane:
      case T.connect:
        displayType = tok;
        sbCommand.append(" ").appendO(theToken.value);
        if (tok == T.sasurface)
          sbCommand.append(" ").appendF(saProbeRadius);
        break;
      case T.parameters:
        params = floatParameterSet(++i, 1, 10);
        i = iToken;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (isWild || bsB != null)
          error(ERROR_invalidArgument);
        bs = BSUtil.copy(atomExpressionAt(i));
        i = iToken;
        if (bsA == null)
          bsA = bs;
        else
          bsB = bs;
        sbCommand.append(" ").append(Escape.e(bs));
        break;
      }
      idSeen = (theTok != T.delete);
    }
    if (!okNoAtoms && bsA == null)
      error(ERROR_endOfStatementUnexpected);
    if (chk)
      return;

    if (bsA != null) {
      // bond mode, intramolec set here
      RadiusData rd1 = (rd == null ? new RadiusData(null,
          0.26f, EnumType.OFFSET, EnumVdw.AUTO) : rd);
      if (displayType == T.nci && bsB == null && intramolecular != null
          && intramolecular.booleanValue())
        bsB = bsA;
      else
        bsB = setContactBitSets(bsA, bsB, localOnly, distance, rd1, true);
      switch (displayType) {
      case T.cap:
      case T.sasurface:
        BS bsSolvent = lookupIdentifierValue("solvent");
        bsA.andNot(bsSolvent);
        bsB.andNot(bsSolvent);
        bsB.andNot(bsA);
        break;
      case T.surface:
        bsB.andNot(bsA);
        break;
      case T.nci:
        if (minSet == Integer.MAX_VALUE)
          minSet = 100;
        setShapeProperty(JC.SHAPE_CONTACT, "minset", Integer
            .valueOf(minSet));
        sbCommand.append(" minSet ").appendI(minSet);
        if (params == null)
          params = new float[] { 0.5f, 2 };
      }

      if (intramolecular != null) {
        params = (params == null ? new float[2] : ArrayUtil.ensureLengthA(
            params, 2));
        params[1] = (intramolecular.booleanValue() ? 1 : 2);
      }

      if (params != null)
        sbCommand.append(" parameters ").append(Escape.e(params));

      // now adjust for type -- HBOND or HYDROPHOBIC or MISC
      // these are just "standard shortcuts" they are not necessary at all
      setShapeProperty(JC.SHAPE_CONTACT, "set", new Object[] {
          Integer.valueOf(contactType), Integer.valueOf(displayType),
          Boolean.valueOf(colorDensity), Boolean.valueOf(colorByType), bsA,
          bsB, rd, Float.valueOf(saProbeRadius), params, sbCommand.toString() });
      if (colorpt > 0)
        setMeshDisplayProperty(JC.SHAPE_CONTACT, colorpt, 0);
    }
    if (iptDisplayProperty > 0) {
      if (!setMeshDisplayProperty(JC.SHAPE_CONTACT,
          iptDisplayProperty, 0))
        error(ERROR_invalidArgument);
    }
    if (userSlabObject != null && bsA != null)
      setShapeProperty(JC.SHAPE_CONTACT, "slab", userSlabObject);
    if (bsA != null && (displayType == T.nci || localOnly)) {
      Object volume = getShapeProperty(JC.SHAPE_CONTACT, "volume");
      if (Escape.isAD(volume)) {
        double[] vs = (double[]) volume;
        double v = 0;
        for (int i = 0; i < vs.length; i++)
          v += Math.abs(vs[i]);
        volume = Float.valueOf((float) v);
      }
      int nsets = ((Integer) getShapeProperty(JC.SHAPE_CONTACT, "nSets")).intValue();
      
      if (colorDensity || displayType != T.trim) {
        showString((nsets == 0 ? "" : nsets + " contacts with ") + "net volume " + volume + " A^3");
      }
    }
  }

  BS setContactBitSets(BS bsA, BS bsB, boolean localOnly,
                           float distance, RadiusData rd, boolean warnMultiModel) {
    boolean withinAllModels;
    BS bs;
    if (bsB == null) {
      // default is within just one model when {B} is missing
      bsB = BSUtil.setAll(viewer.getAtomCount());
      BSUtil.andNot(bsB, viewer.getDeletedAtoms());
      bsB.andNot(bsA);
      withinAllModels = false;
    } else {
      // two atom sets specified; within ALL MODELS here
      bs = BSUtil.copy(bsA);
      bs.or(bsB);
      int nModels = viewer.getModelBitSet(bs, false).cardinality();
      withinAllModels = (nModels > 1);
      if (warnMultiModel && nModels > 1 && !tQuiet)
        showString(GT._("Note: More than one model is involved in this contact!"));
    }
    // B always within some possibly extended VDW of A or just A itself
    if (!bsA.equals(bsB)) {
      boolean setBfirst = (!localOnly || bsA.cardinality() < bsB.cardinality());
      if (setBfirst) {
        bs = viewer.getAtomsWithinRadius(distance, bsA, withinAllModels, (Float
            .isNaN(distance) ? rd : null));
        bsB.and(bs);
      }
      if (localOnly) {
        // we can just get the near atoms for A as well.
        bs = viewer.getAtomsWithinRadius(distance, bsB, withinAllModels, (Float
            .isNaN(distance) ? rd : null));
        bsA.and(bs);
        if (!setBfirst) {
          bs = viewer.getAtomsWithinRadius(distance, bsA, withinAllModels, (Float
              .isNaN(distance) ? rd : null));
          bsB.and(bs);
        }
        // If the two sets are not the same,
        // we AND them and see if that is A. 
        // If so, then the smaller set is
        // removed from the larger set.
        bs = BSUtil.copy(bsB);
        bs.and(bsA);
        if (bs.equals(bsA))
          bsB.andNot(bsA);
        else if (bs.equals(bsB))
          bsA.andNot(bsB);
      }
    }
    return bsB;
  }

  private void lcaoCartoon() throws ScriptException {
    sm.loadShape(JC.SHAPE_LCAOCARTOON);
    if (tokAt(1) == T.list
        && listIsosurface(JC.SHAPE_LCAOCARTOON))
      return;
    setShapeProperty(JC.SHAPE_LCAOCARTOON, "init", fullCommand);
    if (slen == 1) {
      setShapeProperty(JC.SHAPE_LCAOCARTOON, "lcaoID", null);
      return;
    }
    boolean idSeen = false;
    String translucency = null;
    for (int i = 1; i < slen; i++) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case T.cap:
      case T.slab:
        propertyName = (String) theToken.value;
        if (tokAt(i + 1) == T.off)
          iToken = i + 1;
        propertyValue = getCapSlabObject(i, true);
        i = iToken;
        break;
      case T.center:
        // serialized lcaoCartoon in isosurface format
        isosurface(JC.SHAPE_LCAOCARTOON);
        return;
      case T.rotate:
        float degx = 0;
        float degy = 0;
        float degz = 0;
        switch (getToken(++i).tok) {
        case T.x:
          degx = floatParameter(++i) * JC.radiansPerDegree;
          break;
        case T.y:
          degy = floatParameter(++i) * JC.radiansPerDegree;
          break;
        case T.z:
          degz = floatParameter(++i) * JC.radiansPerDegree;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        propertyName = "rotationAxis";
        propertyValue = V3.new3(degx, degy, degz);
        break;
      case T.on:
      case T.display:
      case T.displayed:
        propertyName = "on";
        break;
      case T.off:
      case T.hide:
      case T.hidden:
        propertyName = "off";
        break;
      case T.delete:
        propertyName = "delete";
        break;
      case T.bitset:
      case T.expressionBegin:
        propertyName = "select";
        propertyValue = atomExpressionAt(i);
        i = iToken;
        break;
      case T.color:
        translucency = setColorOptions(null, i + 1,
            JC.SHAPE_LCAOCARTOON, -2);
        if (translucency != null)
          setShapeProperty(JC.SHAPE_LCAOCARTOON, "settranslucency",
              translucency);
        i = iToken;
        idSeen = true;
        continue;
      case T.translucent:
      case T.opaque:
        setMeshDisplayProperty(JC.SHAPE_LCAOCARTOON, i, theTok);
        i = iToken;
        idSeen = true;
        continue;
      case T.spacefill:
      case T.string:
        propertyValue = parameterAsString(i).toLowerCase();
        if (propertyValue.equals("spacefill"))
          propertyValue = "cpk";
        propertyName = "create";
        if (optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case T.select:
        if (tokAt(i + 1) == T.bitset
            || tokAt(i + 1) == T.expressionBegin) {
          propertyName = "select";
          propertyValue = atomExpressionAt(i + 1);
          i = iToken;
        } else {
          propertyName = "selectType";
          propertyValue = parameterAsString(++i);
          if (propertyValue.equals("spacefill"))
            propertyValue = "cpk";
        }
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.lonepair:
      case T.lp:
        propertyName = "lonePair";
        break;
      case T.radical:
      case T.rad:
        propertyName = "radical";
        break;
      case T.molecular:
        propertyName = "molecular";
        break;
      case T.create:
        propertyValue = parameterAsString(++i);
        propertyName = "create";
        if (optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case T.id:
        propertyValue = getShapeNameParameter(++i);
        i = iToken;
        if (idSeen)
          error(ERROR_invalidArgument);
        propertyName = "lcaoID";
        break;
      default:
        if (theTok == T.times || T.tokAttr(theTok, T.identifier)) {
          if (theTok != T.times)
            propertyValue = parameterAsString(i);
          if (idSeen)
            error(ERROR_invalidArgument);
          propertyName = "lcaoID";
          break;
        }
        break;
      }
      if (theTok != T.delete)
        idSeen = true;
      if (propertyName == null)
        error(ERROR_invalidArgument);
      setShapeProperty(JC.SHAPE_LCAOCARTOON, propertyName,
          propertyValue);
    }
    setShapeProperty(JC.SHAPE_LCAOCARTOON, "clear", null);
  }

  private Object getCapSlabObject(int i, boolean isLcaoCartoon)
      throws ScriptException {
    if (i < 0) {
      // standard range -100 to 0
      return MeshSurface.getSlabWithinRange(i, 0);
    }
    Object data = null;
    int tok0 = tokAt(i);
    boolean isSlab = (tok0 == T.slab);
    int tok = tokAt(i + 1);
    Point4f plane = null;
    P3[] pts = null;
    float d, d2;
    BS bs = null;
    Short slabColix = null;
    Integer slabMeshType = null;
    if (tok == T.translucent) {
      float slabTranslucency = (isFloatParameter(++i + 1) ? floatParameter(++i)
          : 0.5f);
      if (isColorParam(i + 1)) {
        slabColix = Short.valueOf(C.getColixTranslucent3(C
            .getColix(getArgbParam(i + 1)), slabTranslucency != 0,
            slabTranslucency));
        i = iToken;
      } else {
        slabColix = Short.valueOf(C.getColixTranslucent3(
            C.INHERIT_COLOR, slabTranslucency != 0, slabTranslucency));
      }
      switch (tok = tokAt(i + 1)) {
      case T.mesh:
      case T.fill:
        slabMeshType = Integer.valueOf(tok);
        tok = tokAt(++i + 1);
        break;
      default:
        slabMeshType = Integer.valueOf(T.fill);
        break;
      }
    }
    //TODO: check for compatibility with LCAOCARTOONS
    switch (tok) {
    case T.bitset:
    case T.expressionBegin:
      data = atomExpressionAt(i + 1);
      tok = T.decimal;
      iToken++;
      break;
    case T.off:
      iToken = i + 1;
      return Integer.valueOf(Integer.MIN_VALUE);
    case T.none:
      iToken = i + 1;
      break;
    case T.dollarsign:
      // do we need distance here? "-" here?
      i++;
      data = new Object[] { Float.valueOf(1), parameterAsString(++i) };
      tok = T.mesh;
      break;
    case T.within:
      // isosurface SLAB WITHIN RANGE f1 f2
      i++;
      if (tokAt(++i) == T.range) {
        d = floatParameter(++i);
        d2 = floatParameter(++i);
        data = new Object[] { Float.valueOf(d), Float.valueOf(d2) };
        tok = T.range;
      } else if (isFloatParameter(i)) {
        // isosurface SLAB WITHIN distance {atomExpression}|[point array]
        d = floatParameter(i);
        if (isCenterParameter(++i)) {
          P3 pt = centerParameter(i);
          if (chk || !(expressionResult instanceof BS)) {
            pts = new P3[] { pt };
          } else {
            Atom[] atoms = viewer.modelSet.atoms;
            bs = (BS) expressionResult;
            pts = new P3[bs.cardinality()];
            for (int k = 0, j = bs.nextSetBit(0); j >= 0; j = bs
                .nextSetBit(j + 1), k++)
              pts[k] = atoms[j];
          }
        } else {
          pts = getPointArray(i, -1);
        }
        if (pts.length == 0) {
          iToken = i;
          error(ERROR_invalidArgument);
        }
        data = new Object[] { Float.valueOf(d), pts, bs };
      } else {
        data = getPointArray(i, 4);
        tok = T.boundbox;
      }
      break;
    case T.boundbox:
      iToken = i + 1;
      data = BoxInfo.getCriticalPoints(viewer.getBoundBoxVertices(), null);
      break;
    //case Token.slicebox:
    // data = BoxInfo.getCriticalPoints(((JmolViewer)(viewer)).slicer.getSliceVert(), null);
    //iToken = i + 1;
    //break;  
    case T.brillouin:
    case T.unitcell:
      iToken = i + 1;
      SymmetryInterface unitCell = viewer.getCurrentUnitCell();
      if (unitCell == null) {
        if (tok == T.unitcell)
          error(ERROR_invalidArgument);
      } else {
        pts = BoxInfo.getCriticalPoints(unitCell.getUnitCellVertices(),
            unitCell.getCartesianOffset());
        int iType = (int) unitCell
            .getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
        V3 v1 = null;
        V3 v2 = null;
        switch (iType) {
        case 3:
          break;
        case 1: // polymer
          v2 = V3.newV(pts[2]);
          v2.sub(pts[0]);
          v2.scale(1000f);
          //$FALL-THROUGH$
        case 2: // slab
          // "a b c" is really "z y x"
          v1 = V3.newV(pts[1]);
          v1.sub(pts[0]);
          v1.scale(1000f);
          pts[0].sub(v1);
          pts[1].scale(2000f);
          if (iType == 1) {
            pts[0].sub(v2);
            pts[2].scale(2000f);
          }
          break;
        }
        data = pts;
      }
      break;
    default:
      // isosurface SLAB n
      // isosurface SLAB -100. 0.  as "within range" 
      if (!isLcaoCartoon && isSlab && isFloatParameter(i + 1)) {
        d = floatParameter(++i);
        if (!isFloatParameter(i + 1))
          return Integer.valueOf((int) d);
        d2 = floatParameter(++i);
        data = new Object[] { Float.valueOf(d), Float.valueOf(d2) };
        tok = T.range;
        break;
      }
      // isosurface SLAB [plane]
      plane = planeParameter(++i);
      float off = (isFloatParameter(iToken + 1) ? floatParameter(++iToken)
          : Float.NaN);
      if (!Float.isNaN(off))
        plane.w -= off;
      data = plane;
      tok = T.plane;
    }
    Object colorData = (slabMeshType == null ? null : new Object[] {
        slabMeshType, slabColix });
    return MeshSurface.getSlabObject(tok, data, !isSlab, colorData);
  }

  private boolean mo(boolean isInitOnly) throws ScriptException {
    int offset = Integer.MAX_VALUE;
    boolean isNegOffset = false;
    BS bsModels = viewer.getVisibleFramesBitSet();
    JmolList<Object[]> propertyList = new  JmolList<Object[]>();
    int i0 = 1;
    if (tokAt(1) == T.model || tokAt(1) == T.frame) {
      i0 = modelNumberParameter(2);
      if (i0 < 0)
        error(ERROR_invalidArgument);
      bsModels.clearAll();
      bsModels.set(i0);
      i0 = 3;
    }
    for (int iModel = bsModels.nextSetBit(0); iModel >= 0; iModel = bsModels
        .nextSetBit(iModel + 1)) {
      sm.loadShape(JC.SHAPE_MO);
      int i = i0;
      if (tokAt(i) == T.list && listIsosurface(JC.SHAPE_MO))
        return true;
      setShapeProperty(JC.SHAPE_MO, "init", Integer.valueOf(iModel));
      String title = null;
      int moNumber = ((Integer) getShapeProperty(JC.SHAPE_MO,
          "moNumber")).intValue();
      float[] linearCombination = (float[]) getShapeProperty(
          JC.SHAPE_MO, "moLinearCombination");
      if (isInitOnly)
        return true;// (moNumber != 0);
      if (moNumber == 0)
        moNumber = Integer.MAX_VALUE;
      String propertyName = null;
      Object propertyValue = null;

      switch (getToken(i).tok) {
      case T.cap:
      case T.slab:
        propertyName = (String) theToken.value;
        propertyValue = getCapSlabObject(i, false);
        i = iToken;
        break;
      case T.density:
        propertyName = "squareLinear";
        propertyValue = Boolean.TRUE;
        linearCombination = new float[] {1};
        offset = moNumber = 0;
        break;
      case T.integer:
        moNumber = intParameter(i);
        linearCombination = moCombo(propertyList);
        if (linearCombination == null && moNumber < 0)
          linearCombination = new float[] { -100, -moNumber };
        break;
      case T.minus:
        switch (tokAt(++i)) {
        case T.homo:
        case T.lumo:
          break;
        default:
          error(ERROR_invalidArgument);
        }
        isNegOffset = true;
        //$FALL-THROUGH$
      case T.homo:
      case T.lumo:
        if ((offset = moOffset(i)) == Integer.MAX_VALUE)
          error(ERROR_invalidArgument);
        moNumber = 0;
        linearCombination = moCombo(propertyList);
        break;
      case T.next:
        moNumber = T.next;
        linearCombination = moCombo(propertyList);
        break;
      case T.prev:
        moNumber = T.prev;
        linearCombination = moCombo(propertyList);
        break;
      case T.color:
        setColorOptions(null, i + 1, JC.SHAPE_MO, 2);
        break;
      case T.plane:
        // plane {X, Y, Z, W}
        propertyName = "plane";
        propertyValue = planeParameter(i + 1);
        break;
      case T.point:
        addShapeProperty(propertyList, "randomSeed",
            tokAt(i + 2) == T.integer ? Integer
                .valueOf(intParameter(i + 2)) : null);
        propertyName = "monteCarloCount";
        propertyValue = Integer.valueOf(intParameter(i + 1));
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(i + 1));
        break;
      case T.cutoff:
        if (tokAt(i + 1) == T.plus) {
          propertyName = "cutoffPositive";
          propertyValue = Float.valueOf(floatParameter(i + 2));
        } else {
          propertyName = "cutoff";
          propertyValue = Float.valueOf(floatParameter(i + 1));
        }
        break;
      case T.debug:
        propertyName = "debug";
        break;
      case T.noplane:
        propertyName = "plane";
        break;
      case T.pointsperangstrom:
      case T.resolution:
        propertyName = "resolution";
        propertyValue = Float.valueOf(floatParameter(i + 1));
        break;
      case T.squared:
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        break;
      case T.titleformat:
        if (i + 1 < slen && tokAt(i + 1) == T.string) {
          propertyName = "titleFormat";
          propertyValue = parameterAsString(i + 1);
        }
        break;
      case T.identifier:
        error(ERROR_invalidArgument);
        break;
      default:
        if (isArrayParameter(i)) {
          linearCombination = floatParameterSet(i, 1, Integer.MAX_VALUE);
          if (tokAt(iToken + 1) == T.squared) {
            addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
            iToken++;
          }
          break;
        }
        int ipt = iToken;
        if (!setMeshDisplayProperty(JC.SHAPE_MO, 0, theTok))
          error(ERROR_invalidArgument);
        setShapeProperty(JC.SHAPE_MO, "setProperties", propertyList);
        setMeshDisplayProperty(JC.SHAPE_MO, ipt, tokAt(ipt));
        return true;
      }
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
      if (moNumber != Integer.MAX_VALUE || linearCombination != null) {
        if (tokAt(iToken + 1) == T.string)
          title = parameterAsString(++iToken);
        setCursorWait(true);
        setMoData(propertyList, moNumber, linearCombination, offset,
            isNegOffset, iModel, title);
        addShapeProperty(propertyList, "finalize", null);
      }
      if (propertyList.size() > 0)
        setShapeProperty(JC.SHAPE_MO, "setProperties", propertyList);
      propertyList.clear();
    }
    return true;
  }

  private float[] moCombo(JmolList<Object[]> propertyList) {
    if (tokAt(iToken + 1) != T.squared)
      return null;
    addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
    iToken++;
    return new float[0];
  }

  private int moOffset(int index) throws ScriptException {
    boolean isHomo = (getToken(index).tok == T.homo);
    int offset = (isHomo ? 0 : 1);
    int tok = tokAt(++index);
    if (tok == T.integer && intParameter(index) < 0)
      offset += intParameter(index);
    else if (tok == T.plus)
      offset += intParameter(++index);
    else if (tok == T.minus)
      offset -= intParameter(++index);
    return offset;
  }

  @SuppressWarnings("unchecked")
  private void setMoData(JmolList<Object[]> propertyList, int moNumber, float[] lc,
                         int offset, boolean isNegOffset, int modelIndex,
                         String title) throws ScriptException {
    if (chk)
      return;
    if (modelIndex < 0) {
      modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        errorStr(ERROR_multipleModelsDisplayedNotOK, "MO isosurfaces");
    }
    Map moData = (Map) viewer.getModelAuxiliaryInfoValue(modelIndex, "moData");
    JmolList<Map<String, Object>> mos = null;
    Map<String, Object> mo;
    Float f;
    int nOrb = 0;
    if (lc == null || lc.length < 2) {
      if (lc != null && lc.length == 1)
        offset = 0;
      if (moData == null)
        error(ERROR_moModelError);
      int lastMoNumber = (moData.containsKey("lastMoNumber") ? ((Integer) moData
          .get("lastMoNumber")).intValue()
          : 0);
      int lastMoCount = (moData.containsKey("lastMoCount") ? ((Integer) moData
          .get("lastMoCount")).intValue() : 1);
      if (moNumber == T.prev)
        moNumber = lastMoNumber - 1;
      else if (moNumber == T.next)
        moNumber = lastMoNumber + lastMoCount;
      mos = (JmolList<Map<String, Object>>) (moData.get("mos"));
      nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        error(ERROR_moCoefficients);
      if (nOrb == 1 && moNumber > 1)
        error(ERROR_moOnlyOne);
      if (offset != Integer.MAX_VALUE) {
        // 0: HOMO;
        if (moData.containsKey("HOMO")) {
          moNumber = ((Integer) moData.get("HOMO")).intValue() + offset;
        } else {
          moNumber = -1;
          for (int i = 0; i < nOrb; i++) {
            mo = mos.get(i);
            if ((f = (Float) mo.get("occupancy")) != null) {
              if (f.floatValue() < 0.5f) {
                // go for LUMO = first unoccupied
                moNumber = i;
                break;
              }
              continue;
            } else if ((f = (Float) mo.get("energy")) != null) {
              if (f.floatValue() > 0) {
                // go for LUMO = first positive
                moNumber = i;
                break;
              }
              continue;
            }
            break;
          }
          if (moNumber < 0)
            error(ERROR_moOccupancy);
          moNumber += offset;
        }
        Logger.info("MO " + moNumber);
      }
      if (moNumber < 1 || moNumber > nOrb)
        errorStr(ERROR_moIndex, "" + nOrb);
    }
    moNumber = Math.abs(moNumber);
    moData.put("lastMoNumber", Integer.valueOf(moNumber));
    moData.put("lastMoCount", Integer.valueOf(1));
    if (isNegOffset && lc == null)
      lc = new float[] { -100, moNumber };
    if (lc != null && lc.length < 2) {
      mo = mos.get(moNumber - 1);
      if ((f = (Float) mo.get("energy")) == null) {
        lc = new float[] { 100, moNumber };
      } else {

        // constuct set of equivalent energies and square this

        float energy = f.floatValue();
        BS bs = BS.newN(nOrb);
        int n = 0;
        boolean isAllElectrons = (lc.length == 1 && lc[0] == 1);
        for (int i = 0; i < nOrb; i++) {
          if ((f = (Float) mos.get(i).get("energy")) == null)
            continue;
          float e = f.floatValue();
          if (isAllElectrons ? e <= energy : e == energy) {
            bs.set(i + 1);
            n += 2;
          }
        }
        lc = new float[n];
        for (int i = 0, pt = 0; i < n; i += 2) {
          lc[i] = 1;
          lc[i + 1] = (pt = bs.nextSetBit(pt + 1));
        }
        moData.put("lastMoNumber", Integer.valueOf(bs.nextSetBit(0)));
        moData.put("lastMoCount", Integer.valueOf(n / 2));
      }
      addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
    }
    addShapeProperty(propertyList, "moData", moData);
    if (title != null)
      addShapeProperty(propertyList, "title", title);
    addShapeProperty(propertyList, "molecularOrbital", lc != null ? lc
        : Integer.valueOf(Math.abs(moNumber)));
    addShapeProperty(propertyList, "clear", null);
  }

  private String initIsosurface(int iShape) throws ScriptException {

    // handle isosurface/mo/pmesh delete and id delete here

    setShapeProperty(iShape, "init", fullCommand);
    iToken = 0;
    int tok1 = tokAt(1);
    int tok2 = tokAt(2);
    if (tok1 == T.delete || tok2 == T.delete
        && tokAt(++iToken) == T.all) {
      setShapeProperty(iShape, "delete", null);
      iToken += 2;
      if (slen > iToken) {
        setShapeProperty(iShape, "init", fullCommand);
        setShapeProperty(iShape, "thisID", MeshCollection.PREVIOUS_MESH_ID);
      }
      return null;
    }
    iToken = 1;
    if (!setMeshDisplayProperty(iShape, 0, tok1)) {
      setShapeProperty(iShape, "thisID", MeshCollection.PREVIOUS_MESH_ID);
      if (iShape != JC.SHAPE_DRAW)
        setShapeProperty(iShape, "title", new String[] { thisCommand });
      if (tok1 != T.id
          && (tok2 == T.times || tok1 == T.times
              && setMeshDisplayProperty(iShape, 0, tok2))) {
        String id = setShapeId(iShape, 1, false);
        iToken++;
        return id;
      }
    }
    return null;
  }

  private String getNextComment() {

    String nextCommand = getCommand(pc + 1, false, true);
    return (nextCommand.startsWith("#") ? nextCommand : "");
  }

  private boolean listIsosurface(int iShape) throws ScriptException {
    checkLength23();
    if (!chk)
      showString((String) getShapeProperty(iShape, "list"
          + (tokAt(2) == T.nada ? "" : " " + getToken(2).value)));
    return true;
  }

  private void isosurface(int iShape) throws ScriptException {
    // also called by lcaoCartoon
    sm.loadShape(iShape);
    if (tokAt(1) == T.list && listIsosurface(iShape))
      return;
    int iptDisplayProperty = 0;
    boolean isIsosurface = (iShape == JC.SHAPE_ISOSURFACE);
    boolean isPmesh = (iShape == JC.SHAPE_PMESH);
    boolean isPlot3d = (iShape == JC.SHAPE_PLOT3D);
    boolean isLcaoCartoon = (iShape == JC.SHAPE_LCAOCARTOON);
    boolean surfaceObjectSeen = false;
    boolean planeSeen = false;
    boolean isMapped = false;
    boolean isBicolor = false;
    boolean isPhased = false;
    boolean doCalcArea = false;
    boolean doCalcVolume = false;
    boolean isCavity = false;
    boolean haveRadius = false;
    boolean toCache = false;
    boolean isFxy = false;
    boolean haveSlab = false;
    boolean haveIntersection = false;
    float[] data = null;
    String cmd = null;
    int thisSetNumber = -1;
    int nFiles = 0;
    int nX, nY, nZ, ptX, ptY;
    float sigma = Float.NaN;
    float cutoff = Float.NaN;
    int ptWithin = 0;
    Boolean smoothing = null;
    int smoothingPower = Integer.MAX_VALUE;
    BS bs = null;
    BS bsSelect = null;
    BS bsIgnore = null;
    SB sbCommand = new SB();
    P3 pt;
    Point4f plane = null;
    P3 lattice = null;
    P3[] pts;
    String str = null;
    int modelIndex = (chk ? 0 : Integer.MIN_VALUE);
    setCursorWait(true);
    boolean idSeen = (initIsosurface(iShape) != null);
    boolean isWild = (idSeen && getShapeProperty(iShape, "ID") == null);
    boolean isColorSchemeTranslucent = false;
    boolean isInline;
    Object onlyOneModel = null;
    String translucency = null;
    String colorScheme = null;
    String mepOrMlp = null;
    short[] discreteColixes = null;
    JmolList<Object[]> propertyList = new  JmolList<Object[]>();
    boolean defaultMesh = false;
    if (isPmesh || isPlot3d)
      addShapeProperty(propertyList, "fileType", "Pmesh");

    for (int i = iToken; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      getToken(i);
      if (theTok == T.identifier)
        str = parameterAsString(i);
      switch (theTok) {
      // settings only
      case T.isosurfacepropertysmoothing:
        smoothing = (getToken(++i).tok == T.on ? Boolean.TRUE
            : theTok == T.off ? Boolean.FALSE : null);
        if (smoothing == null)
          error(ERROR_invalidArgument);
        continue;
      case T.isosurfacepropertysmoothingpower:
        smoothingPower = intParameter(++i);
        continue;
        // offset, rotate, and scale3d don't need to be saved in sbCommand
        // because they are display properties
      case T.move: // Jmol 13.0.RC2 -- required for state saving after coordinate-based translate/rotate
        propertyName = "moveIsosurface";
        if (tokAt(++i) != T.matrix4f)
          error(ERROR_invalidArgument);
        propertyValue = getToken(i++).value;
        break;
      case T.offset:
        propertyName = "offset";
        propertyValue = centerParameter(++i);
        i = iToken;
        break;
      case T.rotate:
        propertyName = "rotate";
        propertyValue = (tokAt(iToken = ++i) == T.none ? null
            : getPoint4f(i));
        i = iToken;
        break;
      case T.scale3d:
        propertyName = "scale3d";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.period:
        sbCommand.append(" periodic");
        propertyName = "periodic";
        break;
      case T.origin:
      case T.step:
      case T.point:
        propertyName = theToken.value.toString();
        sbCommand.append(" ").appendO(theToken.value);
        propertyValue = centerParameter(++i);
        sbCommand.append(" ").append(Escape.e(propertyValue));
        i = iToken;
        break;
      case T.boundbox:
        if (fullCommand.indexOf("# BBOX=") >= 0) {
          String[] bbox = TextFormat.split(Parser.getQuotedAttribute(
              fullCommand, "# BBOX"), ',');
          pts = new P3[] { (P3) Escape.uP(bbox[0]),
              (P3) Escape.uP(bbox[1]) };
        } else if (isCenterParameter(i + 1)) {
          pts = new P3[] { getPoint3f(i + 1, true),
              getPoint3f(iToken + 1, true) };
          i = iToken;
        } else {
          pts = viewer.getBoundBoxVertices();
        }
        sbCommand.append(" boundBox " + Escape.eP(pts[0]) + " "
            + Escape.eP(pts[pts.length - 1]));
        propertyName = "boundingBox";
        propertyValue = pts;
        break;
      case T.pmesh:
        isPmesh = true;
        sbCommand.append(" pmesh");
        propertyName = "fileType";
        propertyValue = "Pmesh";
        break;
      case T.intersection:
        // isosurface intersection {A} {B} VDW....
        // isosurface intersection {A} {B} function "a-b" VDW....
        bsSelect = atomExpressionAt(++i);
        if (chk) {
          bs = new BS();
        } else if (tokAt(iToken + 1) == T.expressionBegin
            || tokAt(iToken + 1) == T.bitset) {
          bs = atomExpressionAt(++iToken);
          bs.and(viewer.getAtomsWithinRadius(5.0f, bsSelect, false, null));
        } else {
          // default is "within(5.0, selected) and not within(molecule,selected)"
          bs = viewer.getAtomsWithinRadius(5.0f, bsSelect, true, null);
          bs.andNot(viewer.getAtomBits(T.molecule, bsSelect));
        }
        bs.andNot(bsSelect);
        sbCommand.append(" intersection ").append(Escape.e(bsSelect))
            .append(" ").append(Escape.e(bs));
        i = iToken;
        if (tokAt(i + 1) == T.function) {
          i++;
          String f = (String) getToken(++i).value;
          sbCommand.append(" function ").append(Escape.eS(f));
          if (!chk)
            addShapeProperty(propertyList, "func", (f.equals("a+b")
                || f.equals("a-b") ? f : createFunction("__iso__", "a,b", f)));
        } else {
          haveIntersection = true;
        }
        propertyName = "intersection";
        propertyValue = new BS[] { bsSelect, bs };
        break;
      case T.display:
      case T.within:
        boolean isDisplay = (theTok == T.display);
        if (isDisplay) {
          sbCommand.append(" display");
          iptDisplayProperty = i;
          int tok = tokAt(i + 1);
          if (tok == T.nada)
            continue;
          i++;
          addShapeProperty(propertyList, "token", Integer.valueOf(T.on));
          if (tok == T.bitset || tok == T.all) {
            propertyName = "bsDisplay";
            if (tok == T.all) {
              sbCommand.append(" all");
            } else {
              propertyValue = st[i].value;
              sbCommand.append(" ").append(Escape.e(propertyValue));
            }
            checkLast(i);
            break;
          } else if (tok != T.within) {
            iToken = i;
            error(ERROR_invalidArgument);
          }
        } else {
          ptWithin = i;
        }
        float distance;
        P3 ptc = null;
        bs = null;
        boolean havePt = false;
        if (tokAt(i + 1) == T.expressionBegin) {
          // within ( x.x , .... )
          distance = floatParameter(i + 3);
          if (isPoint3f(i + 4)) {
            ptc = centerParameter(i + 4);
            havePt = true;
            iToken = iToken + 2;
          } else if (isPoint3f(i + 5)) {
            ptc = centerParameter(i + 5);
            havePt = true;
            iToken = iToken + 2;
          } else {
            bs = atomExpression(st, i + 5, slen, true, false,
                false, true);
            if (bs == null)
              error(ERROR_invalidArgument);
          }
        } else {
          distance = floatParameter(++i);
          ptc = centerParameter(++i);
        }
        if (isDisplay)
          checkLast(iToken);
        i = iToken;
        if (fullCommand.indexOf("# WITHIN=") >= 0)
          bs = Escape.uB(Parser.getQuotedAttribute(fullCommand,
              "# WITHIN"));
        else if (!havePt)
          bs = (expressionResult instanceof BS ? (BS) expressionResult
              : null);
        if (!chk) {
          if (bs != null && modelIndex >= 0) {
            bs.and(viewer.getModelUndeletedAtomsBitSet(modelIndex));
          }
          if (ptc == null)
            ptc = viewer.getAtomSetCenter(bs);

          getWithinDistanceVector(propertyList, distance, ptc, bs, isDisplay);
          sbCommand.append(" within ").appendF(distance).append(" ").append(
              bs == null ? Escape.eP(ptc) : Escape.e(bs));
        }
        continue;
      case T.parameters:
        propertyName = "parameters";
        // if > 1 parameter, then first is assumed to be the cutoff. 
        float[] fparams = floatParameterSet(++i, 1, 10);
        i = iToken;
        propertyValue = fparams;
        sbCommand.append(" parameters ").append(Escape.e(fparams));
        break;
      case T.property:
      case T.variable:
        onlyOneModel = theToken.value;
        boolean isVariable = (theTok == T.variable);
        int tokProperty = tokAt(i + 1);
        if (mepOrMlp == null) { // not mlp or mep
          if (!surfaceObjectSeen && !isMapped && !planeSeen) {
            addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
            //if (surfaceObjectSeen)
            sbCommand.append(" vdw");
            surfaceObjectSeen = true;
          }
          propertyName = "property";
          if (smoothing == null) {
            boolean allowSmoothing = true;
            switch (tokProperty) {
            case T.atomindex:
            case T.atomno:
            case T.elemno:
            case T.color:
            case T.resno:
            case T.temperature:
              allowSmoothing = false;
              break;
            }
            smoothing = (allowSmoothing
                && viewer.getIsosurfacePropertySmoothing(false) == 1 ? Boolean.TRUE
                : Boolean.FALSE);
          }
          addShapeProperty(propertyList, "propertySmoothing", smoothing);
          sbCommand.append(" isosurfacePropertySmoothing " + smoothing);
          if (smoothing == Boolean.TRUE) {
            if (smoothingPower == Integer.MAX_VALUE)
              smoothingPower = viewer.getIsosurfacePropertySmoothing(true);
            addShapeProperty(propertyList, "propertySmoothingPower", Integer
                .valueOf(smoothingPower));
            sbCommand.append(" isosurfacePropertySmoothingPower "
                + smoothingPower);
          }
          if (viewer.isRangeSelected())
            addShapeProperty(propertyList, "rangeSelected", Boolean.TRUE);
        } else {
          propertyName = mepOrMlp;
        }
        str = parameterAsString(i);
        //        if (surfaceObjectSeen)
        sbCommand.append(" ").append(str);

        if (str.toLowerCase().indexOf("property_") == 0) {
          data = new float[viewer.getAtomCount()];
          if (chk)
            continue;
          data = viewer.getDataFloat(str);
          if (data == null)
            error(ERROR_invalidArgument);
          addShapeProperty(propertyList, propertyName, data);
          continue;
        }

        int atomCount = viewer.getAtomCount();
        data = new float[atomCount];

        if (isVariable) {
          String vname = parameterAsString(++i);
          if (vname.length() == 0) {
            data = floatParameterSet(i, atomCount, atomCount);
          } else {
            data = new float[atomCount];
            if (!chk)
              Parser.parseStringInfestedFloatArray(""
                  + getParameter(vname, T.string), null, data);
          }
          if (!chk/* && (surfaceObjectSeen)*/)
            sbCommand.append(" \"\" ").append(Escape.e(data));
        } else {
          getToken(++i);
          if (!chk) {
            sbCommand.append(" " + theToken.value);
            Atom[] atoms = viewer.modelSet.atoms;
            viewer.autoCalculate(tokProperty);
            if (tokProperty != T.color)
              for (int iAtom = atomCount; --iAtom >= 0;)
                data[iAtom] = Atom.atomPropertyFloat(viewer, atoms[iAtom],
                    tokProperty);
          }
          if (tokProperty == T.color)
            colorScheme = "inherit";
          if (tokAt(i + 1) == T.within) {
            float d = floatParameter(i = i + 2);
            sbCommand.append(" within " + d);
            addShapeProperty(propertyList, "propertyDistanceMax", Float
                .valueOf(d));
          }
        }
        propertyValue = data;
        break;
      case T.model:
        if (surfaceObjectSeen)
          error(ERROR_invalidArgument);
        modelIndex = modelNumberParameter(++i);
        sbCommand.append(" model " + modelIndex);
        if (modelIndex < 0) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(modelIndex);
        break;
      case T.select:
        // in general, viewer.getCurrentSelection() is used, but we may
        // override that here. But we have to be careful that
        // we PREPEND the selection to the command if no surface object
        // has been seen yet, and APPEND it if it has.
        propertyName = "select";
        BS bs1 = atomExpressionAt(++i);
        propertyValue = bs1;
        i = iToken;
        boolean isOnly = (tokAt(i + 1) == T.only);
        if (isOnly) {
          i++;
          BS bs2 = BSUtil.copy(bs1);
          BSUtil.invertInPlace(bs2, viewer.getAtomCount());
          addShapeProperty(propertyList, "ignore", bs2);
          sbCommand.append(" ignore ").append(Escape.e(bs2));
        }
        if (surfaceObjectSeen || isMapped) {
          sbCommand.append(" select " + Escape.e(propertyValue));
        } else {
          bsSelect = (BS) propertyValue;
          if (modelIndex < 0 && bsSelect.nextSetBit(0) >= 0)
            modelIndex = viewer.getAtomModelIndex(bsSelect.nextSetBit(0));
        }
        break;
      case T.set:
        thisSetNumber = intParameter(++i);
        break;
      case T.center:
        propertyName = "center";
        propertyValue = centerParameter(++i);
        sbCommand.append(" center " + Escape.e(propertyValue));
        i = iToken;
        break;
      case T.sign:
      case T.color:
        int color;
        idSeen = true;
        boolean isSign = (theTok == T.sign);
        if (isSign) {
          sbCommand.append(" sign");
          addShapeProperty(propertyList, "sign", Boolean.TRUE);
        } else {
          if (tokAt(i + 1) == T.density) {
            i++;
            propertyName = "colorDensity";
            sbCommand.append(" color density");
            break;
          }
          /*
           * "color" now is just used as an equivalent to "sign" and as an
           * introduction to "absolute" any other use is superfluous; it has
           * been replaced with MAP for indicating "use the current surface"
           * because the term COLOR is too general.
           */

          if (getToken(i + 1).tok == T.string) {
            colorScheme = parameterAsString(++i);
            if (colorScheme.indexOf(" ") > 0) {
              discreteColixes = C.getColixArray(colorScheme);
              if (discreteColixes == null)
                error(ERROR_badRGBColor);
            }
          } else if (theTok == T.mesh) {
            i++;
            sbCommand.append(" color mesh");
            color = getArgbParam(++i);
            addShapeProperty(propertyList, "meshcolor", Integer.valueOf(color));
            sbCommand.append(" ").append(Escape.escapeColor(color));
            i = iToken;
            continue;
          }
          if ((theTok = tokAt(i + 1)) == T.translucent
              || theTok == T.opaque) {
            sbCommand.append(" color");
            translucency = setColorOptions(sbCommand, i + 1,
                JC.SHAPE_ISOSURFACE, -2);
            i = iToken;
            continue;
          }
          switch (tokAt(i + 1)) {
          case T.absolute:
          case T.range:
            getToken(++i);
            sbCommand.append(" color range");
            addShapeProperty(propertyList, "rangeAll", null);
            if (tokAt(i + 1) == T.all) {
              i++;
              sbCommand.append(" all");
              continue;
            }
            float min = floatParameter(++i);
            float max = floatParameter(++i);
            addShapeProperty(propertyList, "red", Float.valueOf(min));
            addShapeProperty(propertyList, "blue", Float.valueOf(max));
            sbCommand.append(" ").appendF(min).append(" ").appendF(max);
            continue;
          }
          if (isColorParam(i + 1)) {
            color = getArgbParam(i + 1);
            if (tokAt(i + 2) == T.to) {
              colorScheme = getColorRange(i + 1);
              i = iToken;
              break;
            }
          }
          sbCommand.append(" color");
        }
        if (isColorParam(i + 1)) {
          color = getArgbParam(++i);
          sbCommand.append(" ").append(Escape.escapeColor(color));
          i = iToken;
          addShapeProperty(propertyList, "colorRGB", Integer.valueOf(color));
          idSeen = true;
          if (isColorParam(i + 1)) {
            color = getArgbParam(++i);
            i = iToken;
            addShapeProperty(propertyList, "colorRGB", Integer.valueOf(color));
            sbCommand.append(" ").append(Escape.escapeColor(color));
            isBicolor = true;
          } else if (isSign) {
            error(ERROR_invalidParameterOrder);
          }
        } else if (!isSign && discreteColixes == null) {
          error(ERROR_invalidParameterOrder);
        }
        continue;
      case T.cache:
        if (!isIsosurface)
          error(ERROR_invalidArgument);
        toCache = !chk;
        continue;
      case T.file:
        if (tokAt(i + 1) != T.string)
          error(ERROR_invalidParameterOrder);
        continue;
      case T.ionic:
      case T.vanderwaals:
        //if (surfaceObjectSeen)
        sbCommand.append(" ").appendO(theToken.value);
        RadiusData rd = encodeRadiusParameter(i, false, true);
        //if (surfaceObjectSeen)
        sbCommand.append(" ").appendO(rd);
        if (Float.isNaN(rd.value))
          rd.value = 100;
        propertyValue = rd;
        propertyName = "radius";
        haveRadius = true;
        if (isMapped)
          surfaceObjectSeen = false;
        i = iToken;
        break;
      case T.plane:
        // plane {X, Y, Z, W}
        planeSeen = true;
        propertyName = "plane";
        propertyValue = planeParameter(++i);
        i = iToken;
        //if (surfaceObjectSeen)
        sbCommand.append(" plane ").append(Escape.e(propertyValue));
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(++i));
        sbCommand.append(" scale ").appendO(propertyValue);
        break;
      case T.all:
        if (idSeen)
          error(ERROR_invalidArgument);
        propertyName = "thisID";
        break;
      case T.ellipsoid:
        // ellipsoid {xc yc zc f} where a = b and f = a/c
        // OR ellipsoid {u11 u22 u33 u12 u13 u23}
        surfaceObjectSeen = true;
        ++i;
        try {
          propertyValue = getPoint4f(i);
          propertyName = "ellipsoid";
          i = iToken;
          sbCommand.append(" ellipsoid ").append(Escape.e(propertyValue));
          break;
        } catch (ScriptException e) {
        }
        try {
          propertyName = "ellipsoid";
          propertyValue = floatParameterSet(i, 6, 6);
          i = iToken;
          sbCommand.append(" ellipsoid ").append(Escape.e(propertyValue));
          break;
        } catch (ScriptException e) {
        }
        bs = atomExpressionAt(i);
        sbCommand.append(" ellipsoid ").append(Escape.e(bs));
        int iAtom = bs.nextSetBit(0);
        Atom[] atoms = viewer.modelSet.atoms;
        if (iAtom >= 0)
          propertyValue = atoms[iAtom].getEllipsoid();
        if (propertyValue == null)
          return;
        i = iToken;
        propertyName = "ellipsoid";
        if (!chk)
          addShapeProperty(propertyList, "center", viewer.getAtomPoint3f(iAtom));
        break;
      case T.hkl:
        // miller indices hkl
        planeSeen = true;
        propertyName = "plane";
        propertyValue = hklParameter(++i);
        i = iToken;
        sbCommand.append(" plane ").append(Escape.e(propertyValue));
        break;
      case T.lcaocartoon:
        surfaceObjectSeen = true;
        String lcaoType = parameterAsString(++i);
        addShapeProperty(propertyList, "lcaoType", lcaoType);
        sbCommand.append(" lcaocartoon ").append(Escape.eS(lcaoType));
        switch (getToken(++i).tok) {
        case T.bitset:
        case T.expressionBegin:
          // automatically selects just the model of the first atom in the set.
          propertyName = "lcaoCartoon";
          bs = atomExpressionAt(i);
          i = iToken;
          if (chk)
            continue;
          int atomIndex = bs.nextSetBit(0);
          if (atomIndex < 0)
            error(ERROR_expressionExpected);
          sbCommand.append(" ({").appendI(atomIndex).append("})");
          modelIndex = viewer.getAtomModelIndex(atomIndex);
          addShapeProperty(propertyList, "modelIndex", Integer
              .valueOf(modelIndex));
          V3[] axes = { new V3(), new V3(),
              V3.newV(viewer.getAtomPoint3f(atomIndex)), new V3() };
          if (!lcaoType.equalsIgnoreCase("s")
              && viewer.getHybridizationAndAxes(atomIndex, axes[0], axes[1],
                  lcaoType) == null)
            return;
          propertyValue = axes;
          break;
        default:
          error(ERROR_expressionExpected);
        }
        break;
      case T.mo:
        // mo 1-based-index
        int moNumber = Integer.MAX_VALUE;
        int offset = Integer.MAX_VALUE;
        boolean isNegOffset = (tokAt(i + 1) == T.minus);
        if (isNegOffset)
          i++;
        float[] linearCombination = null;
        switch (tokAt(++i)) {
        case T.nada:
          error(ERROR_badArgumentCount);
          break;
        case T.density:
          sbCommand.append("mo [1] squared ");
          addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
          linearCombination = new float[] { 1 };
          offset = moNumber = 0;
          i++;
          break;
        case T.homo:
        case T.lumo:
          offset = moOffset(i);
          moNumber = 0;
          i = iToken;
          //if (surfaceObjectSeen) {
          sbCommand.append(" mo " + (isNegOffset ? "-" : "") + "HOMO ");
          if (offset > 0)
            sbCommand.append("+");
          if (offset != 0)
            sbCommand.appendI(offset);
          //}
          break;
        case T.integer:
          moNumber = intParameter(i);
          //if (surfaceObjectSeen)
          sbCommand.append(" mo ").appendI(moNumber);
          break;
        default:
          if (isArrayParameter(i)) {
            linearCombination = floatParameterSet(i, 1, Integer.MAX_VALUE);
            i = iToken;
          }
        }
        boolean squared = (tokAt(i + 1) == T.squared);
        if (squared) {
          addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
          sbCommand.append(" squared");
          if (linearCombination == null)
            linearCombination = new float[0];
        } else if (tokAt(i + 1) == T.point) {
          ++i;
          int monteCarloCount = intParameter(++i);
          int seed = (tokAt(i + 1) == T.integer ? intParameter(++i)
              : ((int) -System.currentTimeMillis()) % 10000);
          addShapeProperty(propertyList, "monteCarloCount", Integer
              .valueOf(monteCarloCount));
          addShapeProperty(propertyList, "randomSeed", Integer.valueOf(seed));
          sbCommand.append(" points ").appendI(monteCarloCount).appendC(' ')
              .appendI(seed);
        }
        setMoData(propertyList, moNumber, linearCombination, offset,
            isNegOffset, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case T.nci:
        propertyName = "nci";
        //if (surfaceObjectSeen)
        sbCommand.append(" " + propertyName);
        int tok = tokAt(i + 1);
        boolean isPromolecular = (tok != T.file && tok != T.string && tok != T.mrc);
        propertyValue = Boolean.valueOf(isPromolecular);
        if (isPromolecular)
          surfaceObjectSeen = true;
        break;
      case T.mep:
      case T.mlp:
        boolean isMep = (theTok == T.mep);
        propertyName = (isMep ? "mep" : "mlp");
        //if (surfaceObjectSeen)
        sbCommand.append(" " + propertyName);
        String fname = null;
        int calcType = -1;
        surfaceObjectSeen = true;
        if (tokAt(i + 1) == T.integer) {
          calcType = intParameter(++i);
          sbCommand.append(" " + calcType);
          addShapeProperty(propertyList, "mepCalcType", Integer
              .valueOf(calcType));
        }
        if (tokAt(i + 1) == T.string) {
          fname = stringParameter(++i);
          //if (surfaceObjectSeen)
          sbCommand.append(" /*file*/" + Escape.eS(fname));
        } else if (tokAt(i + 1) == T.property) {
          mepOrMlp = propertyName;
          continue;
        }
        if (!chk)
          try {
            data = (fname == null && isMep ? viewer.getPartialCharges()
                : viewer.getAtomicPotentials(isMep, bsSelect, bsIgnore, fname));
          } catch (Exception e) {
            // ignore
          }
        if (!chk && data == null)
          error(ERROR_noPartialCharges);
        propertyValue = data;
        break;
      case T.volume:
        doCalcVolume = !chk;
        sbCommand.append(" volume");
        break;
      case T.id:
        setShapeId(iShape, ++i, idSeen);
        isWild = (getShapeProperty(iShape, "ID") == null);
        i = iToken;
        break;
      case T.colorscheme:
        // either order NOT OK -- documented for TRANSLUCENT "rwb"
        if (tokAt(i + 1) == T.translucent) {
          isColorSchemeTranslucent = true;
          i++;
        }
        colorScheme = parameterAsString(++i).toLowerCase();
        if (colorScheme.equals("sets")) {
          sbCommand.append(" colorScheme \"sets\"");
        } else if (isColorParam(i)) {
          colorScheme = getColorRange(i);
          i = iToken;
        }
        break;
      case T.addhydrogens:
        propertyName = "addHydrogens";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" addHydrogens");
        break;
      case T.angstroms:
        propertyName = "angstroms";
        sbCommand.append(" angstroms");
        break;
      case T.anisotropy:
        propertyName = "anisotropy";
        propertyValue = getPoint3f(++i, false);
        sbCommand.append(" anisotropy").append(
            Escape.eP((P3) propertyValue));
        i = iToken;
        break;
      case T.area:
        doCalcArea = !chk;
        sbCommand.append(" area");
        break;
      case T.atomicorbital:
      case T.orbital:
        surfaceObjectSeen = true;
        if (isBicolor && !isPhased) {
          sbCommand.append(" phase \"_orb\"");
          addShapeProperty(propertyList, "phase", "_orb");
        }
        float[] nlmZprs = new float[7];
        nlmZprs[0] = intParameter(++i);
        nlmZprs[1] = intParameter(++i);
        nlmZprs[2] = intParameter(++i);
        nlmZprs[3] = (isFloatParameter(i + 1) ? floatParameter(++i) : 6f);
        //if (surfaceObjectSeen)
        sbCommand.append(" atomicOrbital ").appendI((int) nlmZprs[0]).append(
            " ").appendI((int) nlmZprs[1]).append(" ")
            .appendI((int) nlmZprs[2]).append(" ").appendF(nlmZprs[3]);
        if (tokAt(i + 1) == T.point) {
          i += 2;
          nlmZprs[4] = intParameter(i);
          nlmZprs[5] = (tokAt(i + 1) == T.decimal ? floatParameter(++i) : 0);
          nlmZprs[6] = (tokAt(i + 1) == T.integer ? intParameter(++i)
              : ((int) -System.currentTimeMillis()) % 10000);
          //if (surfaceObjectSeen)
          sbCommand.append(" points ").appendI((int) nlmZprs[4]).appendC(' ')
              .appendF(nlmZprs[5]).appendC(' ').appendI((int) nlmZprs[6]);
        }
        propertyName = "hydrogenOrbital";
        propertyValue = nlmZprs;
        break;
      case T.binary:
        sbCommand.append(" binary");
        // for PMESH, specifically
        // ignore for now
        continue;
      case T.blockdata:
        sbCommand.append(" blockData");
        propertyName = "blockData";
        propertyValue = Boolean.TRUE;
        break;
      case T.cap:
      case T.slab:
        haveSlab = true;
        propertyName = (String) theToken.value;
        propertyValue = getCapSlabObject(i, false);
        i = iToken;
        break;
      case T.cavity:
        if (!isIsosurface)
          error(ERROR_invalidArgument);
        isCavity = true;
        if (chk)
          continue;
        float cavityRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 1.2f);
        float envelopeRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 10f);
        if (envelopeRadius > 10f)
          integerOutOfRange(0, 10);
        sbCommand.append(" cavity ").appendF(cavityRadius).append(" ").appendF(
            envelopeRadius);
        addShapeProperty(propertyList, "envelopeRadius", Float
            .valueOf(envelopeRadius));
        addShapeProperty(propertyList, "cavityRadius", Float
            .valueOf(cavityRadius));
        propertyName = "cavity";
        break;
      case T.contour:
      case T.contours:
        propertyName = "contour";
        sbCommand.append(" contour");
        switch (tokAt(i + 1)) {
        case T.discrete:
          propertyValue = floatParameterSet(i + 2, 1, Integer.MAX_VALUE);
          sbCommand.append(" discrete ").append(Escape.e(propertyValue));
          i = iToken;
          break;
        case T.increment:
          pt = getPoint3f(i + 2, false);
          if (pt.z <= 0 || pt.y < pt.x)
            error(ERROR_invalidArgument); // from to step
          if (pt.z == (int) pt.z && pt.z > (pt.y - pt.x))
            pt.z = (pt.y - pt.x) / pt.z;
          propertyValue = pt;
          i = iToken;
          sbCommand.append(" increment ").append(Escape.eP(pt));
          break;
        default:
          propertyValue = Integer
              .valueOf(tokAt(i + 1) == T.integer ? intParameter(++i) : 0);
          sbCommand.append(" ").appendO(propertyValue);
        }
        break;
      case T.decimal:
      case T.integer:
      case T.plus:
      case T.cutoff:
        sbCommand.append(" cutoff ");
        if (theTok == T.cutoff)
          i++;
        if (tokAt(i) == T.plus) {
          propertyName = "cutoffPositive";
          propertyValue = Float.valueOf(cutoff = floatParameter(++i));
          sbCommand.append("+").appendO(propertyValue);
        } else if (isFloatParameter(i)) {
          propertyName = "cutoff";
          propertyValue = Float.valueOf(cutoff = floatParameter(i));
          sbCommand.appendO(propertyValue);
        } else {
          propertyName = "cutoffRange";
          propertyValue = floatParameterSet(i, 2, 2);
          addShapeProperty(propertyList, "cutoff", Float.valueOf(0));
          sbCommand.append(Escape.e(propertyValue));
          i = iToken;
        }
        break;
      case T.downsample:
        propertyName = "downsample";
        propertyValue = Integer.valueOf(intParameter(++i));
        //if (surfaceObjectSeen)
        sbCommand.append(" downsample ").appendO(propertyValue);
        break;
      case T.eccentricity:
        propertyName = "eccentricity";
        propertyValue = getPoint4f(++i);
        //if (surfaceObjectSeen)
        sbCommand.append(" eccentricity ").append(Escape.e(propertyValue));
        i = iToken;
        break;
      case T.ed:
        sbCommand.append(" ed");
        // electron density - never documented
        setMoData(propertyList, -1, null, 0, false, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case T.debug:
      case T.nodebug:
        sbCommand.append(" ").appendO(theToken.value);
        propertyName = "debug";
        propertyValue = (theTok == T.debug ? Boolean.TRUE : Boolean.FALSE);
        break;
      case T.fixed:
        sbCommand.append(" fixed");
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case T.fullplane:
        sbCommand.append(" fullPlane");
        propertyName = "fullPlane";
        propertyValue = Boolean.TRUE;
        break;
      case T.functionxy:
      case T.functionxyz:
        // isosurface functionXY "functionName"|"data2d_xxxxx"
        // isosurface functionXYZ "functionName"|"data3d_xxxxx"
        // {origin} {ni ix iy iz} {nj jx jy jz} {nk kx ky kz}
        // or
        // isosurface origin.. step... count... functionXY[Z] = "x + y + z"
        boolean isFxyz = (theTok == T.functionxyz);
        propertyName = "" + theToken.value;
        JmolList<Object> vxy = new  JmolList<Object>();
        propertyValue = vxy;
        isFxy = surfaceObjectSeen = true;
        //if (surfaceObjectSeen)
        sbCommand.append(" ").append(propertyName);
        String name = parameterAsString(++i);
        if (name.equals("=")) {
          //if (surfaceObjectSeen)
          sbCommand.append(" =");
          name = parameterAsString(++i);
          //if (surfaceObjectSeen)
          sbCommand.append(" ").append(Escape.eS(name));
          vxy.addLast(name);
          if (!chk)
            addShapeProperty(propertyList, "func", createFunction("__iso__",
                "x,y,z", name));
          //surfaceObjectSeen = true;
          break;
        }
        // override of function or data name when saved as a state
        String dName = Parser.getQuotedAttribute(fullCommand, "# DATA"
            + (isFxy ? "2" : ""));
        if (dName == null)
          dName = "inline";
        else
          name = dName;
        boolean isXYZ = (name.indexOf("data2d_") == 0);
        boolean isXYZV = (name.indexOf("data3d_") == 0);
        isInline = name.equals("inline");
        //if (!surfaceObjectSeen)
        sbCommand.append(" inline");
        vxy.addLast(name); // (0) = name
        P3 pt3 = getPoint3f(++i, false);
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP(pt3));
        vxy.addLast(pt3); // (1) = {origin}
        Point4f pt4;
        ptX = ++iToken;
        vxy.addLast(pt4 = getPoint4f(ptX)); // (2) = {ni ix iy iz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.e(pt4));
        nX = (int) pt4.x;
        ptY = ++iToken;
        vxy.addLast(pt4 = getPoint4f(ptY)); // (3) = {nj jx jy jz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.e(pt4));
        nY = (int) pt4.x;
        vxy.addLast(pt4 = getPoint4f(++iToken)); // (4) = {nk kx ky kz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.e(pt4));
        nZ = (int) pt4.x;

        if (nX == 0 || nY == 0 || nZ == 0)
          error(ERROR_invalidArgument);
        if (!chk) {
          float[][] fdata = null;
          float[][][] xyzdata = null;
          if (isFxyz) {
            if (isInline) {
              nX = Math.abs(nX);
              nY = Math.abs(nY);
              nZ = Math.abs(nZ);
              xyzdata = floatArraySetXYZ(++iToken, nX, nY, nZ);
            } else if (isXYZV) {
              xyzdata = viewer.getDataFloat3D(name);
            } else {
              xyzdata = viewer.functionXYZ(name, nX, nY, nZ);
            }
            nX = Math.abs(nX);
            nY = Math.abs(nY);
            nZ = Math.abs(nZ);
            if (xyzdata == null) {
              iToken = ptX;
              errorStr(ERROR_what, "xyzdata is null.");
            }
            if (xyzdata.length != nX || xyzdata[0].length != nY
                || xyzdata[0][0].length != nZ) {
              iToken = ptX;
              errorStr(ERROR_what, "xyzdata[" + xyzdata.length + "]["
                  + xyzdata[0].length + "][" + xyzdata[0][0].length
                  + "] is not of size [" + nX + "][" + nY + "][" + nZ + "]");
            }
            vxy.addLast(xyzdata); // (5) = float[][][] data
            //if (!surfaceObjectSeen)
            sbCommand.append(" ").append(Escape.e(xyzdata));
          } else {
            if (isInline) {
              nX = Math.abs(nX);
              nY = Math.abs(nY);
              fdata = floatArraySet(++iToken, nX, nY);
            } else if (isXYZ) {
              fdata = viewer.getDataFloat2D(name);
              nX = (fdata == null ? 0 : fdata.length);
              nY = 3;
            } else {
              fdata = viewer.functionXY(name, nX, nY);
              nX = Math.abs(nX);
              nY = Math.abs(nY);
            }
            if (fdata == null) {
              iToken = ptX;
              errorStr(ERROR_what, "fdata is null.");
            }
            if (fdata.length != nX && !isXYZ) {
              iToken = ptX;
              errorStr(ERROR_what, "fdata length is not correct: "
                  + fdata.length + " " + nX + ".");
            }
            for (int j = 0; j < nX; j++) {
              if (fdata[j] == null) {
                iToken = ptY;
                errorStr(ERROR_what, "fdata[" + j + "] is null.");
              }
              if (fdata[j].length != nY) {
                iToken = ptY;
                errorStr(ERROR_what, "fdata[" + j
                    + "] is not the right length: " + fdata[j].length + " "
                    + nY + ".");
              }
            }
            vxy.addLast(fdata); // (5) = float[][] data
            //if (!surfaceObjectSeen)
            sbCommand.append(" ").append(Escape.e(fdata));
          }
        }
        i = iToken;
        break;
      case T.gridpoints:
        propertyName = "gridPoints";
        sbCommand.append(" gridPoints");
        break;
      case T.ignore:
        propertyName = "ignore";
        propertyValue = bsIgnore = atomExpressionAt(++i);
        sbCommand.append(" ignore ").append(Escape.e(propertyValue));
        i = iToken;
        break;
      case T.insideout:
        propertyName = "insideOut";
        sbCommand.append(" insideout");
        break;
      case T.internal:
      case T.interior:
      case T.pocket:
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").appendO(theToken.value);
        propertyName = "pocket";
        propertyValue = (theTok == T.pocket ? Boolean.TRUE : Boolean.FALSE);
        break;
      case T.lobe:
        // lobe {eccentricity}
        propertyName = "lobe";
        propertyValue = getPoint4f(++i);
        i = iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" lobe ").append(Escape.e(propertyValue));
        surfaceObjectSeen = true;
        break;
      case T.lonepair:
      case T.lp:
        // lp {eccentricity}
        propertyName = "lp";
        propertyValue = getPoint4f(++i);
        i = iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" lp ").append(Escape.e(propertyValue));
        surfaceObjectSeen = true;
        break;
      case T.mapProperty:
        if (isMapped || slen == i + 1)
          error(ERROR_invalidArgument);
        isMapped = true;
        if ((isCavity || haveRadius || haveIntersection) && !surfaceObjectSeen) {
          surfaceObjectSeen = true;
          addShapeProperty(propertyList, "bsSolvent", (haveRadius
              || haveIntersection ? new BS()
              : lookupIdentifierValue("solvent")));
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        }
        if (sbCommand.length() == 0) {
          plane = (Point4f) getShapeProperty(JC.SHAPE_ISOSURFACE,
              "plane");
          if (plane == null) {
            if (getShapeProperty(JC.SHAPE_ISOSURFACE, "contours") != null) {
              addShapeProperty(propertyList, "nocontour", null);
            }
          } else {
            addShapeProperty(propertyList, "plane", plane);
            sbCommand.append("plane ").append(Escape.e(plane));
            planeSeen = true;
            plane = null;
          }
        } else if (!surfaceObjectSeen && !planeSeen) {
          error(ERROR_invalidArgument);
        }
        sbCommand.append("; isosurface map");
        addShapeProperty(propertyList, "map", (surfaceObjectSeen ? Boolean.TRUE
            : Boolean.FALSE));
        break;
      case T.maxset:
        propertyName = "maxset";
        propertyValue = Integer.valueOf(intParameter(++i));
        sbCommand.append(" maxSet ").appendO(propertyValue);
        break;
      case T.minset:
        propertyName = "minset";
        propertyValue = Integer.valueOf(intParameter(++i));
        sbCommand.append(" minSet ").appendO(propertyValue);
        break;
      case T.radical:
        // rad {eccentricity}
        surfaceObjectSeen = true;
        propertyName = "rad";
        propertyValue = getPoint4f(++i);
        i = iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" radical ").append(Escape.e(propertyValue));
        break;
      case T.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        sbCommand.append(" modelBased");
        break;
      case T.molecular:
      case T.sasurface:
      case T.solvent:
        onlyOneModel = theToken.value;
        float radius;
        if (theTok == T.molecular) {
          propertyName = "molecular";
          //if (!surfaceObjectSeen)
          sbCommand.append(" molecular");
          radius = 1.4f;
        } else {
          addShapeProperty(propertyList, "bsSolvent",
              lookupIdentifierValue("solvent"));
          propertyName = (theTok == T.sasurface ? "sasurface" : "solvent");
          //if (!surfaceObjectSeen)
          sbCommand.append(" ").appendO(theToken.value);
          radius = (isFloatParameter(i + 1) ? floatParameter(++i) : viewer
              .getSolventProbeRadius());
          //if (!surfaceObjectSeen)
          sbCommand.append(" ").appendF(radius);
        }
        propertyValue = Float.valueOf(radius);
        if (tokAt(i + 1) == T.full) {
          addShapeProperty(propertyList, "doFullMolecular", null);
          //if (!surfaceObjectSeen)
          sbCommand.append(" full");
          i++;
        }
        surfaceObjectSeen = true;
        break;
      case T.mrc:
        addShapeProperty(propertyList, "fileType", "Mrc");
        sbCommand.append(" mrc");
        continue;
      case T.object:
      case T.obj:
        addShapeProperty(propertyList, "fileType", "Obj");
        sbCommand.append(" obj");
        continue;
      case T.msms:
        addShapeProperty(propertyList, "fileType", "Msms");
        sbCommand.append(" msms");
        continue;
      case T.phase:
        if (surfaceObjectSeen)
          error(ERROR_invalidArgument);
        propertyName = "phase";
        isPhased = true;
        propertyValue = (tokAt(i + 1) == T.string ? stringParameter(++i)
            : "_orb");
        sbCommand.append(" phase ").append(Escape.e(propertyValue));
        break;
      case T.pointsperangstrom:
      case T.resolution:
        propertyName = "resolution";
        propertyValue = Float.valueOf(floatParameter(++i));
        sbCommand.append(" resolution ").appendO(propertyValue);
        break;
      case T.reversecolor:
        propertyName = "reverseColor";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" reversecolor");
        break;
      case T.sigma:
        propertyName = "sigma";
        propertyValue = Float.valueOf(sigma = floatParameter(++i));
        sbCommand.append(" sigma ").appendO(propertyValue);
        break;
      case T.geosurface:
        // geosurface [radius]
        propertyName = "geodesic";
        propertyValue = Float.valueOf(floatParameter(++i));
        //if (!surfaceObjectSeen)
        sbCommand.append(" geosurface ").appendO(propertyValue);
        surfaceObjectSeen = true;
        break;
      case T.sphere:
        // sphere [radius]
        propertyName = "sphere";
        propertyValue = Float.valueOf(floatParameter(++i));
        //if (!surfaceObjectSeen)
        sbCommand.append(" sphere ").appendO(propertyValue);
        surfaceObjectSeen = true;
        break;
      case T.squared:
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" squared");
        break;
      case T.inline:
      case T.string:
        String filename = parameterAsString(i);
        String sType = null;
        isInline = filename.equalsIgnoreCase("inline");
        if (tokAt(i + 1) == T.string) {
          sType = stringParameter(++i);
          if (!isInline)
            addShapeProperty(propertyList, "calculationType", sType);
        }
        boolean firstPass = (!surfaceObjectSeen && !planeSeen);
        propertyName = (firstPass ? "readFile" : "mapColor");
        if (isInline) {
          if (sType == null)
            error(ERROR_invalidArgument);
          // inline PMESH data
          if (isPmesh)
            sType = TextFormat.replaceAllCharacter(sType, "{,}|", ' ');
          if (logMessages)
            Logger.debug("pmesh inline data:\n" + sType);
          propertyValue = (chk ? null : sType);
          addShapeProperty(propertyList, "fileName", "");
          //if (!surfaceObjectSeen)
          sbCommand.append(" INLINE");
          surfaceObjectSeen = true;
        } else {
          if (filename.startsWith("=") && filename.length() > 1) {
            // Uppsala Electron Density Server (default, at least)
            String[] info = (String[]) viewer.setLoadFormat(filename, '_',
                false);
            filename = info[0];
            String strCutoff = (!firstPass || !Float.isNaN(cutoff) ? null
                : info[1]);
            if (strCutoff != null && !chk) {
              cutoff = SV.fValue(SV.getVariable(viewer
                  .evaluateExpression(strCutoff)));
              if (cutoff > 0) {
                if (!Float.isNaN(sigma)) {
                  cutoff *= sigma;
                  sigma = Float.NaN;
                  addShapeProperty(propertyList, "sigma", Float.valueOf(sigma));
                }
                addShapeProperty(propertyList, "cutoff", Float.valueOf(cutoff));
                //if (!surfaceObjectSeen)
                sbCommand.append(" cutoff ").appendF(cutoff);
              }
            }
            if (ptWithin == 0) {
              onlyOneModel = "=xxxx";
              if (modelIndex < 0)
                modelIndex = viewer.getCurrentModelIndex();
              bs = viewer.getModelUndeletedAtomsBitSet(modelIndex);
              getWithinDistanceVector(propertyList, 2.0f, null, bs, false);
              //if (!surfaceObjectSeen)
              sbCommand.append(" within 2.0 ").append(Escape.e(bs));
            }
            if (firstPass)
              defaultMesh = true;
          }
          if (firstPass && viewer.getParameter("_fileType").equals("Pdb")
              && Float.isNaN(sigma) && Float.isNaN(cutoff)) {
            // negative sigma just indicates that 
            addShapeProperty(propertyList, "sigma", Float.valueOf(-1));
            //if (!surfaceObjectSeen)
            sbCommand.append(" sigma -1.0");
          }
          /*
           * a file name, optionally followed by an integer file index. OR empty.
           * In that case, if the model auxiliary info has the data stored in it,
           * we use that. There are two possible structures:
           * 
           * jmolSurfaceInfo jmolMappedDataInfo
           * 
           * Both can be present, but if jmolMappedDataInfo is missing, then
           * jmolSurfaceInfo is used by default.
           */

          if (filename.equals("TESTDATA") && testData != null) {
            propertyValue = testData;
            break;
          }
          if (filename.equals("TESTDATA2") && testData2 != null) {
            propertyValue = testData2;
            break;
          }
          if (filename.length() == 0) {
            // "" 
            if (modelIndex < 0)
              modelIndex = viewer.getCurrentModelIndex();
            if (surfaceObjectSeen || planeSeen)
              propertyValue = viewer.getModelAuxiliaryInfoValue(modelIndex,
                  "jmolMappedDataInfo");
            if (propertyValue == null)
              propertyValue = viewer.getModelAuxiliaryInfoValue(modelIndex,
                  "jmolSurfaceInfo");
            if (propertyValue != null) {
              surfaceObjectSeen = true;
              break;
            }
            filename = getFullPathName();
          }
          int fileIndex = -1;
          if (tokAt(i + 1) == T.integer)
            addShapeProperty(propertyList, "fileIndex", Integer
                .valueOf(fileIndex = intParameter(++i)));
          if (!chk) {
            String[] fullPathNameOrError;
            String localName = null;
            if (fullCommand.indexOf("# FILE" + nFiles + "=") >= 0) {
              filename = Parser.getQuotedAttribute(fullCommand, "# FILE"
                  + nFiles);
              if (tokAt(i + 1) == T.as)
                i += 2; // skip that
            } else if (tokAt(i + 1) == T.as) {
              localName = viewer.getFilePath(
                  stringParameter(iToken = (i = i + 2)), false);
              fullPathNameOrError = viewer.getFullPathNameOrError(localName);
              localName = fullPathNameOrError[0];
              if (viewer.getPathForAllFiles() != "") {
                // we use the LOCAL name when reading from a local path only (in the case of JMOL files)
                filename = localName;
                localName = null;
              } else {
                addShapeProperty(propertyList, "localName", localName);
                viewer.setPrivateKeyForShape(iShape); // for the "AS" keyword to work
              }
            }
            // just checking here, and getting the full path name
            if (!filename.startsWith("cache://")) {
              fullPathNameOrError = viewer.getFullPathNameOrError(filename);
              filename = fullPathNameOrError[0];
              if (fullPathNameOrError[1] != null)
                errorStr(ERROR_fileNotFoundException, filename + ":"
                    + fullPathNameOrError[1]);
            }
            Logger.info("reading isosurface data from " + filename);
            addShapeProperty(propertyList, "fileName", filename);
            if (localName != null)
              filename = localName;
            //if (!surfaceObjectSeen)
            sbCommand.append(" /*file*/").append(Escape.eS(filename));
            // null propertyValue indicates that we need a reader based on the fileName
          }
          //if (!surfaceObjectSeen)
          if (fileIndex >= 0)
            sbCommand.append(" ").appendI(fileIndex);
        }
        //if (!surfaceObjectSeen)
        if (sType != null)
          sbCommand.append(" ").append(Escape.eS(sType));
        surfaceObjectSeen = true;
        break;
      case T.connect:
        propertyName = "connections";
        switch (tokAt(++i)) {
        case T.bitset:
        case T.expressionBegin:
          propertyValue = new int[] { atomExpressionAt(i).nextSetBit(0) };
          break;
        default:
          propertyValue = new int[] { (int) floatParameterSet(i, 1, 1)[0] };
          break;
        }
        i = iToken;
        break;
      case T.link:
        propertyName = "link";
        //if (!surfaceObjectSeen)
        sbCommand.append(" link");
        break;
      case T.lattice:
        if (iShape != JC.SHAPE_ISOSURFACE)
          error(ERROR_invalidArgument);
        pt = getPoint3f(iToken + 1, false);
        i = iToken;
        if (pt.x <= 0 || pt.y <= 0 || pt.z <= 0)
          break;
        pt.x = (int) pt.x;
        pt.y = (int) pt.y;
        pt.z = (int) pt.z;
        sbCommand.append(" lattice ").append(Escape.eP(pt));
        if (isMapped) {
          propertyName = "mapLattice";
          propertyValue = pt;
        } else {
          lattice = pt;
        }
        break;
      default:
        if (theTok == T.identifier) {
          propertyName = "thisID";
          propertyValue = str;
        }
        /* I have no idea why this is here....
        if (planeSeen && !surfaceObjectSeen) {
          addShapeProperty(propertyList, "nomap", Float.valueOf(0));
          surfaceObjectSeen = true;
        }
        */
        if (!setMeshDisplayProperty(iShape, 0, theTok)) {
          if (T.tokAttr(theTok, T.identifier) && !idSeen) {
            setShapeId(iShape, i, idSeen);
            i = iToken;
            break;
          }
          error(ERROR_invalidArgument);
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = slen - 1;
        break;
      }
      idSeen = (theTok != T.delete);
      if (isWild && surfaceObjectSeen)
        error(ERROR_invalidArgument);
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
    }

    // OK, now send them all

    if (!chk) {
      if ((isCavity || haveRadius) && !surfaceObjectSeen) {
        surfaceObjectSeen = true;
        addShapeProperty(propertyList, "bsSolvent", (haveRadius ? new BS()
            : lookupIdentifierValue("solvent")));
        addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
      }
      if (planeSeen && !surfaceObjectSeen && !isMapped) {
        // !isMapped added 6/14/2012 12.3.30
        // because it was preventing planes from being mapped properly
        addShapeProperty(propertyList, "nomap", Float.valueOf(0));
        surfaceObjectSeen = true;
      }
      if (thisSetNumber >= 0)
        addShapeProperty(propertyList, "getSurfaceSets", Integer
            .valueOf(thisSetNumber - 1));
      if (discreteColixes != null) {
        addShapeProperty(propertyList, "colorDiscrete", discreteColixes);
      } else if ("sets".equals(colorScheme)) {
        addShapeProperty(propertyList, "setColorScheme", null);
      } else if (colorScheme != null) {
        ColorEncoder ce = viewer.getColorEncoder(colorScheme);
        if (ce != null) {
          ce.isTranslucent = isColorSchemeTranslucent;
          ce.hi = Float.MAX_VALUE;
          addShapeProperty(propertyList, "remapColor", ce);
        }
      }
      if (surfaceObjectSeen && !isLcaoCartoon && sbCommand.indexOf(";") != 0) {
        propertyList.add(0, new Object[] { "newObject", null });
        boolean needSelect = (bsSelect == null);
        if (needSelect)
          bsSelect = BSUtil.copy(viewer.getSelectionSet(false));
        if (modelIndex < 0)
          modelIndex = viewer.getCurrentModelIndex();
        bsSelect.and(viewer.getModelUndeletedAtomsBitSet(modelIndex));
        if (onlyOneModel != null) {
          BS bsModels = viewer.getModelBitSet(bsSelect, false);
          if (bsModels.cardinality() != 1)
            errorStr(ERROR_multipleModelsDisplayedNotOK, "ISOSURFACE "
                + onlyOneModel);
          if (needSelect) {
            propertyList.add(0, new Object[] { "select", bsSelect });
            if (sbCommand.indexOf("; isosurface map") == 0) {
              sbCommand = new SB().append(
                  "; isosurface map select ").append(Escape.e(bsSelect))
                  .append(sbCommand.substring(16));
            }
          }
        }
      }
      if (haveIntersection && !haveSlab) {
        if (!surfaceObjectSeen)
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        if (!isMapped) {
          addShapeProperty(propertyList, "map", Boolean.TRUE);
          addShapeProperty(propertyList, "select", bs);
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        }
        addShapeProperty(propertyList, "slab", getCapSlabObject(-100, false));
      }

      setShapeProperty(iShape, "setProperties", propertyList);

      if (defaultMesh) {
        setShapeProperty(iShape, "token", Integer.valueOf(T.mesh));
        setShapeProperty(iShape, "token", Integer.valueOf(T.nofill));
        setShapeProperty(iShape, "token", Integer.valueOf(T.frontonly));
        sbCommand.append(" mesh nofill frontOnly");
      }
    }
    if (lattice != null) // before MAP, this is a display option
      setShapeProperty(JC.SHAPE_ISOSURFACE, "lattice", lattice);
    if (iptDisplayProperty > 0) {
      if (!setMeshDisplayProperty(iShape, iptDisplayProperty, 0))
        error(ERROR_invalidArgument);
    }

    if (chk)
      return;
    Object area = null;
    Object volume = null;
    if (doCalcArea) {
      area = getShapeProperty(iShape, "area");
      if (area instanceof Float)
        viewer.setFloatProperty("isosurfaceArea", ((Float) area).floatValue());
      else
        viewer.setUserVariable("isosurfaceArea", SV
            .getVariableAD((double[]) area));
    }
    if (doCalcVolume) {
      volume = (doCalcVolume ? getShapeProperty(iShape, "volume") : null);
      if (volume instanceof Float)
        viewer.setFloatProperty("isosurfaceVolume", ((Float) volume)
            .floatValue());
      else
        viewer.setUserVariable("isosurfaceVolume", SV
            .getVariableAD((double[]) volume));
    }
    if (!isLcaoCartoon) {
      String s = null;
      if (isMapped && !surfaceObjectSeen) {
        setShapeProperty(iShape, "finalize", sbCommand.toString());
      } else if (surfaceObjectSeen) {
        cmd = sbCommand.toString();
        setShapeProperty(iShape, "finalize",
            (cmd.indexOf("; isosurface map") == 0 ? "" : " select "
                + Escape.e(bsSelect) + " ")
                + cmd);
        s = (String) getShapeProperty(iShape, "ID");
        if (s != null && !tQuiet) {
          cutoff = ((Float) getShapeProperty(iShape, "cutoff")).floatValue();
          if (Float.isNaN(cutoff) && !Float.isNaN(sigma)) {
            Logger.error("sigma not supported");
          }
          s += " created";
          if (isIsosurface)
            s += " with cutoff=" + cutoff;
          float[] minMax = (float[]) getShapeProperty(iShape, "minMaxInfo");
          if (minMax[0] != Float.MAX_VALUE)
            s += " min=" + minMax[0] + " max=" + minMax[1];
          s += "; " + JC.shapeClassBases[iShape].toLowerCase()
              + " count: " + getShapeProperty(iShape, "count");
          s += getIsosurfaceDataRange(iShape, "\n");
        }
      }
      String sarea, svol;
      if (doCalcArea || doCalcVolume) {
        sarea = (doCalcArea ? "isosurfaceArea = "
            + (area instanceof Float ? area : Escape.escapeAF(area)) : null);
        svol = (doCalcVolume ? "isosurfaceVolume = "
            + (volume instanceof Float ? volume : Escape.escapeAF(volume))
            : null);
        if (s == null) {
          if (doCalcArea)
            showString(sarea);
          if (doCalcVolume)
            showString(svol);
        } else {
          if (doCalcArea)
            s += "\n" + sarea;
          if (doCalcVolume)
            s += "\n" + svol;
        }
      }
      if (s != null)
        showString(s);
    }
    if (translucency != null)
      setShapeProperty(iShape, "translucency", translucency);
    setShapeProperty(iShape, "clear", null);
    if (toCache) {
      String id = (String) getShapeProperty(iShape, "ID");
      viewer.cachePut("cache://isosurface_" + id, getShapeProperty(iShape,
          "jvxlDataXml"));
      runScript("isosurface ID \"" + id + "\" delete;isosurface ID \"" + id
          + "\"" + (modelIndex >= 0 ? " model " + modelIndex : "")
          + " \"cache://isosurface_" + getShapeProperty(iShape, "ID") + "\"");
    }
  }

  private String setColorOptions(SB sb, int index, int iShape,
                                 int nAllowed) throws ScriptException {
    getToken(index);
    String translucency = "opaque";
    if (theTok == T.translucent) {
      translucency = "translucent";
      if (nAllowed < 0) {
        float value = (isFloatParameter(index + 1) ? floatParameter(++index)
            : Float.MAX_VALUE);
        setShapeTranslucency(iShape, null, "translucent", value, null);
        if (sb != null) {
          sb.append(" translucent");
          if (value != Float.MAX_VALUE)
            sb.append(" ").appendF(value);
        }
      } else {
        setMeshDisplayProperty(iShape, index, theTok);
      }
    } else if (theTok == T.opaque) {
      if (nAllowed >= 0)
        setMeshDisplayProperty(iShape, index, theTok);
    } else {
      iToken--;
    }
    nAllowed = Math.abs(nAllowed);
    for (int i = 0; i < nAllowed; i++) {
      if (isColorParam(iToken + 1)) {
        int color = getArgbParam(++iToken);
        setShapeProperty(iShape, "colorRGB", Integer.valueOf(color));
        if (sb != null)
          sb.append(" ").append(Escape.escapeColor(color));
      } else if (iToken < index) {
        error(ERROR_invalidArgument);
      } else {
        break;
      }
    }
    return translucency;
  }

  private String getColorRange(int i) throws ScriptException {
    int color1 = getArgbParam(i);
    if (tokAt(++iToken) != T.to)
      error(ERROR_invalidArgument);
    int color2 = getArgbParam(++iToken);
    int nColors = (tokAt(iToken + 1) == T.integer ? intParameter(++iToken)
        : 0);
    return ColorEncoder.getColorSchemeList(ColorEncoder.getPaletteAtoB(color1,
        color2, nColors));
  }

  private String getIsosurfaceDataRange(int iShape, String sep) {
    float[] dataRange = (float[]) getShapeProperty(iShape, "dataRange");
    return (dataRange != null && dataRange[0] != Float.MAX_VALUE
        && dataRange[0] != dataRange[1] ? sep + "isosurface"
        + " full data range " + dataRange[0] + " to " + dataRange[1]
        + " with color scheme spanning " + dataRange[2] + " to " + dataRange[3]
        : "");
  }

  private static Object testData; // for isosurface
  private static Object testData2; // for isosurface

  private void getWithinDistanceVector(JmolList<Object[]> propertyList,
                                       float distance, P3 ptc, BS bs,
                                       boolean isShow) {
    JmolList<P3> v = new  JmolList<P3>();
    P3[] pts = new P3[2];
    if (bs == null) {
      P3 pt1 = P3.new3(distance, distance, distance);
      P3 pt0 = P3.newP(ptc);
      pt0.sub(pt1);
      pt1.add(ptc);
      pts[0] = pt0;
      pts[1] = pt1;
      v.addLast(ptc);
    } else {
      BoxInfo bbox = viewer.getBoxInfo(bs, -Math.abs(distance));
      pts[0] = bbox.getBboxVertices()[0];
      pts[1] = bbox.getBboxVertices()[7];
      if (bs.cardinality() == 1)
        v.addLast(viewer.getAtomPoint3f(bs.nextSetBit(0)));
    }
    if (v.size() == 1 && !isShow) {
      addShapeProperty(propertyList, "withinDistance", Float.valueOf(distance));
      addShapeProperty(propertyList, "withinPoint", v.get(0));
    }
    addShapeProperty(propertyList, (isShow ? "displayWithin" : "withinPoints"),
        new Object[] { Float.valueOf(distance), pts, bs, v });
  }

  /**
   * @param shape
   * @param i
   * @param tok
   * @return true if successful
   * @throws ScriptException
   */
  private boolean setMeshDisplayProperty(int shape, int i, int tok)
      throws ScriptException {
    String propertyName = null;
    Object propertyValue = null;
    boolean allowCOLOR = (shape == JC.SHAPE_CONTACT);
    boolean checkOnly = (i == 0);
    // these properties are all processed in MeshCollection.java
    if (!checkOnly)
      tok = getToken(i).tok;
    switch (tok) {
    case T.color:
      if (allowCOLOR)
        iToken++;
      else
        break;
      //$FALL-THROUGH$
    case T.opaque:
    case T.translucent:
      if (!checkOnly)
        colorShape(shape, iToken, false);
      return true;
    case T.nada:
    case T.delete:
    case T.on:
    case T.off:
    case T.hide:
    case T.hidden:
    case T.display:
    case T.displayed:
      if (iToken == 1 && shape >= 0 && tokAt(2) == T.nada)
        setShapeProperty(shape, "thisID", null);
      if (tok == T.nada)
        return (iToken == 1);
      if (checkOnly)
        return true;
      switch (tok) {
      case T.delete:
        setShapeProperty(shape, "delete", null);
        return true;
      case T.hidden:
      case T.hide:
        tok = T.off;
        break;
      case T.displayed:
        tok = T.on;
        break;
      case T.display:
        if (i + 1 == slen)
          tok = T.on;
        break;
      }
      //$FALL-THROUGH$ for on/off/display
    case T.frontlit:
    case T.backlit:
    case T.fullylit:
    case T.contourlines:
    case T.nocontourlines:
    case T.dots:
    case T.nodots:
    case T.mesh:
    case T.nomesh:
    case T.fill:
    case T.nofill:
    case T.triangles:
    case T.notriangles:
    case T.frontonly:
    case T.notfrontonly:
      propertyName = "token";
      propertyValue = Integer.valueOf(tok);
      break;
    }
    if (propertyName == null)
      return false;
    if (checkOnly)
      return true;
    setShapeProperty(shape, propertyName, propertyValue);
    if ((tokAt(iToken + 1)) != T.nada) {
      if (!setMeshDisplayProperty(shape, ++iToken, 0))
        --iToken;
    }
    return true;
  }

  private void bind() throws ScriptException {
    /*
     * bind "MOUSE-ACTION" actionName bind "MOUSE-ACTION" "script" 
     *   not implemented: range [xyrange] [xyrange]
     */
    String mouseAction = stringParameter(1);
    String name = parameterAsString(2);
    P3 range1 = null;
    P3 range2 = null;
    //if (tokAt(3) == Token.range) {
    //  range1 = xypParameter(4);
    //  range2 = xypParameter(++iToken);
    //  checkLast(iToken);
    //} else {
    checkLength(3);
    //}
    if (!chk)
      viewer.bindAction(mouseAction, name, range1, range2);
  }

  private void unbind() throws ScriptException {
    /*
     * unbind "MOUSE-ACTION"|all ["...script..."|actionName|all]
     */
    if (slen != 1)
      checkLength23();
    String mouseAction = optParameterAsString(1);
    String name = optParameterAsString(2);
    if (mouseAction.length() == 0 || tokAt(1) == T.all)
      mouseAction = null;
    if (name.length() == 0 || tokAt(2) == T.all)
      name = null;
    if (name == null && mouseAction != null
        && ActionManager.getActionFromName(mouseAction) >= 0) {
      name = mouseAction;
      mouseAction = null;
    }
    if (!chk)
      viewer.unBindAction(mouseAction, name);
  }

  private void undoRedoMove() throws ScriptException {
    // Jmol 12.1.46
    int n = 1;
    int len = 2;
    switch (tokAt(1)) {
    case T.nada:
      len = 1;
      break;
    case T.all:
      n = 0;
      break;
    case T.integer:
      n = intParameter(1);
      break;
    default:
      error(ERROR_invalidArgument);
    }
    checkLength(len);
    if (!chk)
      viewer.undoMoveAction(tokAt(0), n);
  }

  BS getAtomsNearSurface(float distance, String surfaceId) {
    Object[] data = new Object[] { surfaceId, null, null };
    if (chk)
      return new BS();
    if (getShapePropertyData(JC.SHAPE_ISOSURFACE, "getVertices",
            data))
    return viewer.getAtomsNearPts(distance, (P3[]) data[1],
        (BS) data[2]);
    data[1] = Integer.valueOf(0);
    data[2] = Integer.valueOf(-1);
    if (getShapePropertyData(JC.SHAPE_DRAW, "getCenter", data))
      return viewer.getAtomsNearPt(distance, (P3) data[2]);
    return new BS();
  }

  /**
   * Encodes a string such as "2.10" as an integer instead of a float
   * so as to distinguish "2.1" from "2.10"
   * used for model numbers and partial bond orders.
   * 2147483647 is maxvalue, so this allows loading
   * simultaneously up to 2147 files, each with 999999 models (or trajectories)
   * 
   * @param strDecimal
   * @return float encoded as an integer
   */
  static int getFloatEncodedInt(String strDecimal) {
    int pt = strDecimal.indexOf(".");
    if (pt < 1 || strDecimal.charAt(0) == '-'
        || strDecimal.endsWith(".") 
        || strDecimal.contains(".0"))
      return Integer.MAX_VALUE;
    int i = 0;
    int j = 0;
    if (pt > 0) {
      try {
        i = Integer.parseInt(strDecimal.substring(0, pt));
        if (i < 0)
          i = -i;
      } catch (NumberFormatException e) {
        i = -1;
      }
    }
    if (pt < strDecimal.length() - 1)
      try {
        j = Integer.parseInt(strDecimal.substring(pt + 1));
      } catch (NumberFormatException e) {
        // not a problem
      }
    i = i * 1000000 + j;
    return (i < 0 ? Integer.MAX_VALUE : i);
  }

  /**
   * reads standard n.m float-as-integer n*1000000 + m and returns (n % 6) << 5
   * + (m % 0x1F)
   * 
   * @param bondOrderInteger
   * @return Bond order partial mask
   */
  static int getPartialBondOrderFromFloatEncodedInt(
                                                 int bondOrderInteger) {
    return (((bondOrderInteger / 1000000) % 6) << 5)
        + ((bondOrderInteger % 1000000) & 0x1F);
  }

  static int getBondOrderFromString(String s) {
    return (s.indexOf(' ') < 0 ? JmolEdge.getBondOrderFromString(s)
        : s.toLowerCase().indexOf("partial ") == 0 ? getPartialBondOrderFromString(s
            .substring(8).trim()) : JmolEdge.BOND_ORDER_NULL);
  }

  private static int getPartialBondOrderFromString(String s) {
    return getPartialBondOrderFromFloatEncodedInt(getFloatEncodedInt(s));
  }

  public BS addHydrogensInline(BS bsAtoms, JmolList<Atom> vConnections, P3[] pts) throws Exception {
    int modelIndex = viewer.getAtomModelIndex(bsAtoms.nextSetBit(0));
    if (modelIndex != viewer.modelSet.modelCount - 1)
      return new BS();

    // must be added to the LAST data set only

    BS bsA = viewer.getModelUndeletedAtomsBitSet(modelIndex);
    viewer.setAppendNew(false);
    // BitSet bsB = getAtomBits(Token.hydrogen, null);
    // bsA.andNot(bsB);
    int atomIndex = viewer.modelSet.getAtomCount();
    int atomno = viewer.modelSet.getAtomCountInModel(modelIndex);
    SB sbConnect = new SB();
    for (int i = 0; i < vConnections.size(); i++) {
      Atom a = vConnections.get(i);
      sbConnect.append(";  connect 0 100 ")
          .append("({" + (atomIndex++) + "}) ").append(
              "({" + a.index + "}) group;");
    }
    SB sb = new SB();
    sb.appendI(pts.length).append("\n").append(JC.ADD_HYDROGEN_TITLE)
        .append("#noautobond").append("\n");
    for (int i = 0; i < pts.length; i++)
      sb.append("H ").appendF(pts[i].x).append(" ").appendF(pts[i].y).append(" ")
          .appendF(pts[i].z).append(" - - - - ").appendI(++atomno).appendC('\n');
    viewer.loadInlineScript(sb.toString(), '\n', true, null);
    runScriptBuffer(sbConnect.toString(), null);
    BS bsB = viewer.getModelUndeletedAtomsBitSet(modelIndex);
    bsB.andNot(bsA);
    return bsB;
  }

}
