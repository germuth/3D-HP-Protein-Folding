/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.script;

import java.util.Map;

import org.jmol.api.JmolParallelProcessor;
import org.jmol.util.SB;

public class ScriptContext {
  /**
   * 
   */
  public String fullpath = "";
  public String scriptFileName;
  public JmolParallelProcessor parallelProcessor;
  public String functionName;
  public String script;
  public short[] lineNumbers;
  public int[][] lineIndices;
  public T[][] aatoken;
  public T[] statement;
  public int statementLength;
  public int pc;
  public int pcEnd = Integer.MAX_VALUE;
  public int lineEnd = Integer.MAX_VALUE;
  public int iToken;
  public SB outputBuffer;
  public Map<String, SV> contextVariables;
  public boolean isFunction;
  public boolean isStateScript;
  public boolean isTryCatch;
  public String errorMessage;
  public String errorMessageUntranslated;
  public int iCommandError = -1;
  public String errorType;
  public int scriptLevel;
  public boolean chk;
  public boolean executionStepping;
  public boolean executionPaused;
  public String scriptExtensions;
  public String contextPath = " >> ";
  public ScriptContext parentContext;
  public ContextToken token;
  public boolean mustResumeEval;
  public boolean isJSThread;
  public boolean allowJSThreads;
  public boolean displayLoadErrorsSave;
  public int tryPt;

}