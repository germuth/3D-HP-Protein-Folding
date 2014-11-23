/* $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.Escape;
import org.jmol.util.CommandHistory;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.Parser;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.jmol.api.Interface;
import org.jmol.i18n.GT;
import org.jmol.io.JmolBinary;
import org.jmol.modelset.Group;
import org.jmol.modelset.Bond.BondSet;

import org.jmol.util.JmolList;
import java.util.Hashtable;


import java.util.Map;



class ScriptCompiler extends ScriptCompilationTokenParser {

  /*
   * The Compiler class is really two parts -- 
   * 
   * Compiler.class          going from characters to tokens
   * CompilationTokenParser  further syntax checking and modifications
   * 
   * The data structures follow the following sequences:
   * 
   * String script ==> Vector lltoken[][] --> Token[][] aatokenCompiled[][]
   * 
   * A given command goes through the sequence:
   * 
   * String characters --> Token token --> Vector ltoken[] --> Token[][] aatokenCompiled[][]
   * 
   */
  
  ScriptCompiler(Viewer viewer) {
    this.viewer = viewer;
  }
  
  private String filename;
  private boolean isSilent;

  // returns:
  
  private Map<String, SV> contextVariables;
  private T[][] aatokenCompiled;
  private short[] lineNumbers;
  private int[][] lineIndices;
  
  private int lnLength = 8;
  private boolean preDefining;
  private boolean isShowScriptOutput;
  private boolean isCheckOnly;
  private boolean haveComments;

  String scriptExtensions;
  
  private ScriptFunction thisFunction;
  
  private ScriptContext parseScript(boolean doFull) {
    boolean isOK = compile0(doFull);
    if (!isOK)
      handleError();
    ScriptContext sc = new ScriptContext();
    sc.script = script;
    sc.scriptExtensions = scriptExtensions;
    sc.errorType = errorType;
    if (errorType != null) {
      sc.iCommandError = iCommand;
      setAaTokenCompiled();
    }
    sc.aatoken = aatokenCompiled;
    sc.errorMessage = errorMessage;
    sc.errorMessageUntranslated = (errorMessageUntranslated == null 
        ? errorMessage : errorMessageUntranslated);
    sc.lineIndices = lineIndices;
    sc.lineNumbers = lineNumbers;
    sc.contextVariables = contextVariables;
    return sc;
  }

  synchronized ScriptContext compile(String filename, String script, boolean isPredefining,
                  boolean isSilent, boolean debugScript, boolean isCheckOnly) {
    this.isCheckOnly = isCheckOnly;
    this.filename = filename;
    this.isSilent = isSilent;
    this.script = script;
    logMessages = (!isSilent && !isPredefining && debugScript);
    preDefining = (filename == "#predefine");
    return parseScript(true);
  }

  private void addContextVariable(String ident) {
    theToken = T.o(T.identifier, ident);
    if (pushCount > 0) {
      ContextToken ct = (ContextToken) vPush.get(pushCount - 1);
      ct.addName(ident);
      if (ct.tok != T.trycmd)
        return;
    }
    if (thisFunction == null) {
      if (contextVariables == null)
        contextVariables = new Hashtable<String, SV>();
      addContextVariable(contextVariables, ident);
    } else {
      thisFunction.addVariable(ident, false);
    }
  }
  
  static void addContextVariable(Map<String, SV> contextVariables, String name) {
    contextVariables.put(name, SV.newVariable(T.string, "").setName(name));
  }

  private boolean isContextVariable(String ident) {
    for (int i = vPush.size(); --i >= 0;) {
      ContextToken ct = (ContextToken) vPush.get(i);
      if (ct.contextVariables != null && ct.contextVariables.containsKey(ident))
        return true;
    }
    return (thisFunction != null ? thisFunction.isVariable(ident)
      : contextVariables != null && contextVariables.containsKey(ident));
  }
  
  /**
   * allows for three kinds of comments.
   * NOTE: closing involves asterisks and slash together, but that can't be shown here. 
   * 
   * 1) /** .... ** /  super-comment
   * 2) /* ..... * /   may be INSIDE /**....** /).
   * 3)  \n//.....\n   single-line comments -- like #, but removed entirely 
   * The reason is that /* ... * / will appear as standard in MOVETO command
   * but we still might want to escape it, so around that you can have /** .... ** /
   * 
   * The terminator is not necessary -- so you can quickly escape anything in a file 
   * after /** or /*
   * 
   * In addition, we can have [/*|/**] .... **** Jmol Embedded Script ****  [script commands] [** /|* /]
   * Then ONLY that script is taken. This is a powerful and simple way then to include Jmol scripting
   * in any file -- including, for example, HTML as an HTML comment. Just send the whole file to 
   * Jmol, and it will find its script!
   * 
   * @param script
   * @return cleaned script
   */
  private String cleanScriptComments(String script) {
    if (script.indexOf('\u201C') >= 0)
      script = script.replace('\u201C', '"');
    if (script.indexOf('\u201D') >= 0)
      script = script.replace('\u201D', '"');
    if (script.indexOf('\uFEFF') >= 0)
      script = script.replace('\uFEFF', ' ');
    int pt = (script.indexOf("\1##"));
    if (pt >= 0) {
      // these are for jmolConsole and scriptEditor
      scriptExtensions = script.substring(pt + 1);
      script = script.substring(0, pt);
    }
    haveComments = (script.indexOf("#") >= 0); // speeds processing
    return JmolBinary.getEmbeddedScript(script);
  }
  
  private ScriptFlowContext flowContext;
  private JmolList<T> ltoken;
  private JmolList<T[]> lltoken;
  private JmolList<T> vBraces;


  private int ichBrace;
  private int cchToken;
  private int cchScript;

  private int nSemiSkip;
  private int parenCount;
  private int braceCount;
  private int setBraceCount;
  private int bracketCount;
  private int ptSemi;
  private int forPoint3;
  private int setEqualPt;
  private int iBrace;

  private boolean iHaveQuotedString;
  private boolean isEndOfCommand;
  private boolean needRightParen;
  private boolean endOfLine;

  private String comment;

  private void addTokenToPrefix(T token) {
    if (logMessages)
      Logger.info("addTokenToPrefix" + token);
    ltoken.addLast(token);
    if (token.tok != T.nada)
      lastToken = token;
  }

  private final static int OK = 0;
  private final static int OK2 = 1;
  private final static int CONTINUE = 2;
  private final static int EOL = 3;
  private final static int ERROR = 4;

  private int tokLastMath;
  private boolean checkImpliedScriptCmd;
  
  private JmolList<ScriptFunction> vFunctionStack;
  
  private boolean compile0(boolean isFull) {
    vFunctionStack = new  JmolList<ScriptFunction>();
    htUserFunctions = new Hashtable<String, Boolean>();
    script = cleanScriptComments(script);
    ichToken = script.indexOf(JC.STATE_VERSION_STAMP);
    isStateScript = (ichToken >= 0);
    if (isStateScript) {
      ptSemi = script.indexOf(";", ichToken);
      if (ptSemi >= ichToken)
        viewer.setStateScriptVersion(script.substring(
            ichToken + JC.STATE_VERSION_STAMP.length(), ptSemi).trim());
    }
    cchScript = script.length();

    // these four will be returned:
    contextVariables = null;
    lineNumbers = null;
    lineIndices = null;
    aatokenCompiled = null;
    thisFunction = null;
    flowContext = null;
    errorType = null;
    errorMessage = null;
    errorMessageUntranslated = null;
    errorLine = null;

    nSemiSkip = 0;
    ichToken = 0;
    ichCurrentCommand = 0;
    ichComment = 0;
    ichBrace = 0;
    lineCurrent = 1;
    iCommand = 0;
    tokLastMath = 0;
    lastToken = T.tokenOff;
    vBraces = new  JmolList<T>();
    vPush = new  JmolList<T>();
    pushCount = 0;
    iBrace = 0;
    braceCount = 0;
    parenCount = 0;
    ptSemi = -10;
    cchToken = 0;
    lnLength = 8;
    lineNumbers = new short[lnLength];
    lineIndices = new int[lnLength][2];
    isNewSet = isSetBrace = false;
    ptNewSetModifier = 1;
    isShowScriptOutput = false;    
    iHaveQuotedString = false;
    checkImpliedScriptCmd = false;
    lltoken = new  JmolList<T[]>();
    ltoken = new  JmolList<T>();
    tokCommand = T.nada;
    lastFlowCommand = null;
    tokenAndEquals = null;
    tokInitialPlusPlus = T.nada;
    setBraceCount = 0;
    bracketCount = 0;
    forPoint3 = -1;
    setEqualPt = Integer.MAX_VALUE;
    endOfLine = false;
    comment = null;
    isEndOfCommand = false;
    needRightParen = false;
    theTok = T.nada;
    short iLine = 1;
    

    for (; true; ichToken += cchToken) {
      if ((nTokens = ltoken.size()) == 0) { 
        if (thisFunction != null && thisFunction.chpt0 == 0)
          thisFunction.chpt0 = ichToken;
        ichCurrentCommand = ichToken;
        iLine = lineCurrent;
      }
      if (lookingAtLeadingWhitespace())
        continue;
      endOfLine = false;
      if (!isEndOfCommand) {
        endOfLine = lookingAtEndOfLine();
        switch (endOfLine ? OK : lookingAtComment()) {
        case CONTINUE: //  short /*...*/ or comment to completely ignore 
          continue;
        case EOL: // /* .... \n ... */ -- flag as end of line but ignore
          isEndOfCommand = true;
          continue;
        case OK2: // really just line-ending comment -- mark it for later inclusion
          isEndOfCommand = true;
          // start-of line comment -- include as Token.nada 
          comment = script.substring(ichToken, ichToken + cchToken).trim();
          break;
        }
        isEndOfCommand = isEndOfCommand || endOfLine || lookingAtEndOfStatement();
      }
      
      if (isEndOfCommand) {
        isEndOfCommand = false;
        switch (processTokenList(iLine, isFull)) {
        case CONTINUE:
          continue;
        case ERROR:
          return false;
        }
        checkImpliedScriptCmd = false;
        if (ichToken < cchScript)
          continue;
        setAaTokenCompiled();
        return (flowContext == null 
            || errorStr(ERROR_missingEnd, T.nameOf(flowContext.token.tok)));
      }
      
      if (nTokens > 0) {
        switch (checkSpecialParameterSyntax()) {
        case CONTINUE:
          continue;
        case ERROR:
          return false;
        }
      }
      if (lookingAtLookupToken(ichToken)) {        
        String ident = getPrefixToken();
        switch (parseKnownToken(ident)) {
        case CONTINUE:
          continue;
        case ERROR:
          return false;
        }
        switch (parseCommandParameter(ident)) {
        case CONTINUE:
          continue;
        case ERROR:
          return false;
        }
        addTokenToPrefix(theToken);
        continue;
      }
      if (nTokens == 0 || (isNewSet || isSetBrace)
          && nTokens == ptNewSetModifier) {
        if (nTokens == 0) {
          if (lookingAtString(true)) {
            addTokenToPrefix(setCommand(T.tokenScript));
            cchToken = 0;
            continue;
          }
          if (lookingAtImpliedString(true, true, true))
            ichEnd = ichToken + cchToken;
        }
        return commandExpected();
      }
      return errorStr(ERROR_unrecognizedToken, script.substring(ichToken,
          ichToken + 1));
    }
  }
  
  private void setAaTokenCompiled() {
    aatokenCompiled = lltoken.toArray(new T[lltoken.size()][]);
  }

  private boolean lookingAtLeadingWhitespace() {
    int ichT = ichToken;
    while (ichT < cchScript && isSpaceOrTab(script.charAt(ichT)))
      ++ichT;
    if (isLineContinuation(ichT, true))
      ichT += 1 + nCharNewLine(ichT + 1);
    cchToken = ichT - ichToken;
    return cchToken > 0;
  }

  private boolean isLineContinuation(int ichT, boolean checkMathop) {
    boolean isEscaped = (ichT + 2 < cchScript && script.charAt(ichT) == '\\' && nCharNewLine(ichT + 1) > 0 
        || checkMathop && lookingAtMathContinuation(ichT));   
    if (isEscaped)
      lineCurrent++;
    return isEscaped;
  }

  private boolean lookingAtMathContinuation(int ichT) {
    int n;
    if (ichT >= cchScript || (n = nCharNewLine(ichT)) == 0 || lastToken.tok == T.leftbrace)
      return false;
    if (parenCount > 0 || bracketCount > 0)
      return true;
    if ((tokCommand != T.set || !isNewSet) && tokCommand != T.print && tokCommand != T.log)
        return false;
    if (lastToken.tok == tokLastMath)
      return true;
    ichT += n;
    while (ichT < cchScript && isSpaceOrTab(script.charAt(ichT)))
      ++ichT;
    return (lookingAtLookupToken(ichT) 
        && tokLastMath == 1);
  }

  private boolean lookingAtEndOfLine() {
    int ichT = ichEnd = ichToken;
    if (ichToken >= cchScript) {
      ichEnd = cchScript;
      return true;
    }
    int n = nCharNewLine(ichT);
    if (n == 0)
      return false;
    ichEnd = ichToken;
    cchToken = n;
    return true;    
  }
  
  private int nCharNewLine(int ichT) {
    char ch;
    return (ichT >= cchScript ? 0 
        : (ch = script.charAt(ichT)) != '\r' ? (ch == '\n' ? 1 : 0) 
        : ++ichT < cchScript && script.charAt(ichT) == '\n' ? 2 : 1);
  }

  private boolean lookingAtEndOfStatement() {
    boolean isSemi = (script.charAt(ichToken) == ';');
    if (isSemi && nTokens > 0)
      ptSemi = nTokens;
    if (!isSemi || nSemiSkip-- > 0)
      return false;
    cchToken = 1;
    return true;
  }

  private boolean isShowCommand;
  
  private int lookingAtComment() {
    char ch = script.charAt(ichToken);
    int ichT = ichToken;
    int ichFirstSharp = -1;

    // return CONTINUE: totally ignore
    // return EOL: treat as line end, even though it isn't
    // return OK: no comment here

    /*
     * New in Jmol 11.1.9: we allow for output from the set showScript command
     * to be used as input. These lines start with $ and have a [...] phrase
     * after them. Their presence switches us to this new mode where we use
     * those statements as our commands and any line WITHOUT those as comments.
     */
    if (ichToken == ichCurrentCommand && ch == '$') {
      isShowScriptOutput = true;
      isShowCommand = true;
      while (ch != ']' && ichT < cchScript && !eol(ch = script.charAt(ichT)))
        ++ichT;
      cchToken = ichT - ichToken;
      return CONTINUE;
    } else if (isShowScriptOutput && !isShowCommand) {
      ichFirstSharp = ichT;
    }
    if (ch == '/' && ichT + 1 < cchScript)
      switch (script.charAt(++ichT)) {
      case '/':
        ichFirstSharp = ichToken;
        ichEnd = ichT - 1;
        break;
      case '*':
        ichEnd = ichT - 1;
        String terminator = (++ichT < cchScript && (ch = script.charAt(ichT)) == '*' 
            ? "**/" : "*/");
        ichT = script.indexOf(terminator, ichToken + 2);
        if (ichT < 0) {
          ichToken = cchScript;
          return EOL;
        }
        // ichT points at char after /*, whatever that is. So even /***/ will be caught
        incrementLineCount(script.substring(ichToken, ichT));
        cchToken = ichT + (ch == '*' ? 3 : 2) - ichToken;
        return CONTINUE;
      default:
        return OK;
      }

    boolean isSharp = (ichFirstSharp < 0);
    if (isSharp && !haveComments)
      return OK;

    // old way:
    // first, find the end of the statement and scan for # (sharp) signs

    if (ichComment > ichT)
      ichT = ichComment;
    for (; ichT < cchScript; ichT++) {
      if (eol(ch = script.charAt(ichT))) {
        ichEnd = ichT;
        if (ichT > 0 && isLineContinuation(ichT - 1, false)) {
          ichT += nCharNewLine(ichT);
          continue;
        }
        if (!isSharp && ch == ';')
          continue;
        break;
      }
      if (ichFirstSharp >= 0)
        continue;
      if (ch == '#')
        ichFirstSharp = ichT;
    }
    if (ichFirstSharp < 0) // there were no sharps found
      return OK;
    ichComment = ichFirstSharp;
    /****************************************************************
     * check for #jc comment if it occurs anywhere in the statement, then the
     * statement is not executed. This allows statements which are executed in
     * RasMol but are comments in Jmol
     ****************************************************************/

    if (isSharp && nTokens == 0 && cchScript - ichFirstSharp >= 3
        && script.charAt(ichFirstSharp + 1) == 'j'
        && script.charAt(ichFirstSharp + 2) == 'c') {
      // statement contains a #jc before then end ... strip it all
      cchToken = ichT - ichToken;
      return CONTINUE;
    }

    // if the sharp was not the first character then it isn't a comment
    if (ichFirstSharp != ichToken)
      return OK;

    /****************************************************************
     * check for leading #jx <space> or <tab> if you see it, then only strip
     * those 4 characters. if they put in #jx <newline> then they are not going
     * to execute anything, and the regular code will take care of it
     ****************************************************************/
    if (isSharp && cchScript > ichToken + 3 && script.charAt(ichToken + 1) == 'j'
        && script.charAt(ichToken + 2) == 'x'
        && isSpaceOrTab(script.charAt(ichToken + 3))) {
      cchToken = 4; // #jx[\s\t]
      return CONTINUE;
    }
    
    if (ichT == ichToken)
      return OK;

    // first character was a sharp, but was not #jx ... strip it all
    cchToken = ichT - ichToken;
    return (nTokens == 0 ? OK2 : CONTINUE);
  }

  private boolean isComment;
  
  private int processTokenList(short iLine, boolean doCompile) {
    if (nTokens > 0 || comment != null) {
      if (nTokens == 0) {
        // just a comment
        ichCurrentCommand = ichToken;
        if (comment != null) {
          isComment = true;
          addTokenToPrefix(T.o(T.nada, comment));
        }
      } else if (setBraceCount > 0 && endOfLine && ichToken < cchScript) {
        return CONTINUE;
      }
      if (tokCommand == T.script && checkImpliedScriptCmd) {
        String s = (nTokens == 2 ? lastToken.value.toString().toUpperCase() : null);
        if (nTokens > 2 
            && !(tokAt(2) == T.leftparen && ltoken.get(1).value.toString().endsWith(".spt")) 
            || s != null && (s.endsWith(".SORT") || s.endsWith(".REVERSE"))) {
          // check for improperly parsed implied script command:
          // only two tokens:
          //   [implied script] xxx.SORT
          //   [implied script] xxx.REVERSE
          // more than two tokens:
          //   not a script function xxxx.spt(3,4,5)
          ichToken = ichCurrentCommand;
          nTokens = 0;
          ltoken.clear();
          cchToken = 0;
          tokCommand = T.nada;
          return CONTINUE;
        }
      }
      if (isNewSet && nTokens > 2 && tokAt(2) == T.per
          && (tokAt(3) == T.sort || tokAt(3) == T.reverse)) {
        // check for x.sort or x.reverse
        // x.sort / x.reverse ==> x = x.sort / x = x.reverse
        ltoken.set(0, T.tokenSet);
        ltoken.add(1, ltoken.get(1));
      } else if (tokInitialPlusPlus != T.nada) {
        // check for ++x or --x
        if (!isNewSet)
          checkNewSetCommand();
        tokenizePlusPlus(tokInitialPlusPlus, true);
      }
      iCommand = lltoken.size();
      if (thisFunction != null && thisFunction.cmdpt0 < 0) {
        thisFunction.cmdpt0 = iCommand;
      }
      if (nTokens == 1 && braceCount == 1) {
        // ...{...
        if (lastFlowCommand == null) {
          parenCount = setBraceCount = braceCount = 0;
          ltoken.remove(0);
          iBrace++;
          T t = new ContextToken(T.push, 0, "{");
          addTokenToPrefix(setCommand(t));
          pushCount++;
          vPush.addLast(t);
          vBraces.addLast(tokenCommand);
        } else {
          parenCount = setBraceCount = 0;
          setCommand(lastFlowCommand);
          if (lastFlowCommand.tok != T.process
              && (tokAt(0) == T.leftbrace))
            ltoken.remove(0);
          lastFlowCommand = null;
        }
      }
      if (bracketCount > 0 || setBraceCount > 0 || parenCount > 0
          || braceCount == 1 && !checkFlowStartBrace(true)) {
        error(nTokens == 1 ? ERROR_commandExpected
            : ERROR_endOfCommandUnexpected);
        return ERROR;
      }
      if (needRightParen) {
        addTokenToPrefix(T.tokenRightParen);
        needRightParen = false;
      }

      if (ltoken.size() > 0) {
        if (doCompile && !compileCommand())
          return ERROR;
        if (logMessages) {
          Logger.debug("-------------------------------------");
        }
        boolean doEval = true;
        switch (tokCommand) {
        case T.trycmd:
        case T.parallel:
        case T.function: // formerly "noeval"
        case T.end:
          // end switch may have - or + intValue, depending upon default or not
          // end function and the function call itself has intValue 0,
          // but the FUNCTION declaration itself will have MAX_VALUE intValue
          doEval = (atokenInfix.length > 0 && atokenInfix[0].intValue != Integer.MAX_VALUE);
          break;
        }
        if (doEval) {
          if (iCommand == lnLength) {
            lineNumbers = ArrayUtil.doubleLengthShort(lineNumbers);
            int[][] lnI = new int[lnLength * 2][2];
            System.arraycopy(lineIndices, 0, lnI, 0, lnLength);
            lineIndices = lnI;
            lnLength *= 2;
          }
          lineNumbers[iCommand] = iLine;
          lineIndices[iCommand][0] = ichCurrentCommand;
          lineIndices[iCommand][1] = Math.max(ichCurrentCommand, Math.min(
              cchScript, ichEnd == ichCurrentCommand ? ichToken : ichEnd));
          lltoken.addLast(atokenInfix);
          iCommand = lltoken.size();
        }
        if (tokCommand == T.set)
          lastFlowCommand = null;
      }
      setCommand(null);
      comment = null;
      iHaveQuotedString = isNewSet = isSetBrace = needRightParen = false;
      ptNewSetModifier = 1;
      ltoken.clear();
      nTokens = nSemiSkip = 0;
      tokInitialPlusPlus = T.nada;
      tokenAndEquals = null;
      ptSemi = -10;
      forPoint3 = -1;
      setEqualPt = Integer.MAX_VALUE;

    }
    if (endOfLine) {
      if (flowContext != null && flowContext.checkForceEndIf()) {
        if (!isComment)
          forceFlowEnd(flowContext.token);
        isEndOfCommand = true;
        cchToken = 0;
        ichCurrentCommand = ichToken;
        lineCurrent--;
        return CONTINUE;
      }
      isComment = false;
      isShowCommand = false;
      ++lineCurrent;
    }
    if (ichToken >= cchScript) {
      // check for end of all brace work
      setCommand(T.tokenAll);
      theTok = T.nada;
      switch (checkFlowEndBrace()) {
      case ERROR:
        return ERROR;
      case CONTINUE:
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }
      ichToken = cchScript;
      return OK; // main loop exit
    }
    return OK;
  }

  private boolean compileCommand() {
    switch (ltoken.size()) {
    case 0:
      // comment
      atokenInfix = new T[0];
      return true;
    case 4:
      // check for a command name in name.spt
      if (isNewSet && tokenAt(2).value.equals(".") && tokenAt(3).value.equals("spt")) {
        String fname = tokenAt(1).value + "." + tokenAt(3).value;
        ltoken.clear();
        addTokenToPrefix(T.tokenScript);
        addTokenToPrefix(T.o(T.string, fname));
        isNewSet = false;
      }
    }
    setCommand(tokenAt(0));
    int size = ltoken.size();
    if (size == 1 && T.tokAttr(tokCommand, T.defaultON))
      addTokenToPrefix(T.tokenOn);
    if (tokenAndEquals != null) {
      int j;
      int i = 0;
      for (i = 1; i < size; i++) {
        if ((j = tokAt(i)) == T.andequals)
          break;
      }
      size = i;
      i++;
      if (ltoken.size() < i) {
        Logger.error("COMPILER ERROR! - andEquals ");
      } else {
        for (j = 1; j < size; j++, i++)
          ltoken.add(i, tokenAt(j));
        ltoken.set(size, T.tokenEquals);
        ltoken.add(i, tokenAndEquals);
        ltoken.add(++i, T.tokenLeftParen);
        addTokenToPrefix(T.tokenRightParen);
      }
    }

    atokenInfix = ltoken.toArray(new T[size = ltoken.size()]);

    if (logMessages) {
      Logger.debug("token list:");
      for (int i = 0; i < atokenInfix.length; i++)
        Logger.debug(i + ": " + atokenInfix[i]);
      Logger.debug("vBraces list:");
      for (int i = 0; i < vBraces.size(); i++)
        Logger.debug(i + ": " + vBraces.get(i));
      Logger.debug("-------------------------------------");
    }

    // compile expressions  (ScriptCompilerTokenParser.java)
    return compileExpressions();

  }

  private T tokenAt(int i) {
    return ltoken.get(i);
  }

  @Override
  protected int tokAt(int i) {
    return (i < ltoken.size() ? tokenAt(i).tok : T.nada);
  }
  
  private T setCommand(T token) {
    tokenCommand = token;
    if (token == null) {
      tokCommand = T.nada;
    } else {
      tokCommand = tokenCommand.tok;
      isMathExpressionCommand = (tokCommand == T.identifier || T.tokAttr(tokCommand,
          T.mathExpressionCommand));
      isSetOrDefine = (tokCommand == T.set || tokCommand == T.define);
      isCommaAsOrAllowed = T.tokAttr(tokCommand,
          T.atomExpressionCommand);
    }
    return token;
  }

  private void replaceCommand(T token) {
    ltoken.remove(0);
    ltoken.add(0, setCommand(token));
  }

  private boolean isUserToken;
  private int tokInitialPlusPlus;
  
  private String getPrefixToken() {
    String ident = script.substring(ichToken, ichToken + cchToken);
    String identLC = ident.toLowerCase();
    boolean isUserVar = isContextVariable(identLC);
    if (nTokens == 0)
      isUserToken = isUserVar;
    if (nTokens == 1 && (tokCommand == T.function  || tokCommand == T.parallel || tokCommand == T.var)
        || nTokens != 0 && isUserVar
        || isUserFunction(identLC)  && (thisFunction == null || !thisFunction.name.equals(identLC))) {
      // we need to allow:
      
      // var color = "xxx"
      // color @color
      // print color
      // etc. 
      // BUT we also should allow
      // color = ...
      // color += ...
      // color[ ...
      
      ident = identLC;
      theToken = null;
    } else if (ident.length() == 1) {
      // hack to support case sensitive alternate locations and chains
      // if an identifier is a single character long, then
      // allocate a new Token with the original character preserved
      if ((theToken = T.getTokenFromName(ident)) == null
          && (theToken = T.getTokenFromName(identLC)) != null)
        theToken = T.t(theToken.tok, theToken.intValue, ident);
    } else {
      ident = identLC;
      theToken = T.getTokenFromName(ident);
    }
    if (theToken == null) {
      if (ident.indexOf("property_") == 0)
        theToken = T.o(T.property, ident);
      else
        theToken = T.o(T.identifier, ident);
    }    
    theTok = theToken.tok;
    return ident;
  }

  private int checkSpecialParameterSyntax() {
    char ch;
    if (nTokens == ptNewSetModifier) {
      ch = script.charAt(ichToken);
      boolean isAndEquals = ("+-\\*/&|=".indexOf(ch) >= 0);
      boolean isOperation = (isAndEquals || ch == '.' || ch == '[');
      char ch2 = (ichToken + 1 >= cchScript ? 0 : script.charAt(ichToken + 1));
      if (!isNewSet && isUserToken && isOperation
          && (ch == '=' || ch2 == ch || ch2 == '=')) {
        isNewSet = true;
        // Var data = ""
        // data = 5
        // data++
        // data += what

      }
      if (isNewSet || tokCommand == T.set
          || T.tokAttr(tokCommand, T.setparam)) {
        if (ch == '=')
          setEqualPt = ichToken;

        // axes, background, define, display, echo, frank, hbond, history,
        // set, var
        // can all appear with or without "set" in front of them. These
        // are then
        // both commands and parameters for the SET command, but only if
        // they are
        // the FIRST parameter of the set command.
        if (T.tokAttr(tokCommand, T.setparam) && ch == '='
            || (isNewSet || isSetBrace) && isOperation) {
          setCommand(isAndEquals ? T.tokenSet
              : ch == '[' && !isSetBrace ? T.tokenSetArray
                  : T.tokenSetProperty);
          ltoken.add(0, tokenCommand);
          cchToken = 1;
          switch (ch) {
          case '[':
            addTokenToPrefix(T.o(T.leftsquare, "["));
            bracketCount++;
            return CONTINUE;
          case '.':
            addTokenToPrefix(T.o(T.per, "."));
            return CONTINUE;
          case '-':
          case '+':
          case '*':
          case '/':
          case '\\':
          case '&':
          case '|':
            if (ch2 == 0)
              return ERROR(ERROR_endOfCommandUnexpected);
            if (ch2 != ch && ch2 != '=')
              return ERROR(ERROR_badContext, "\"" + ch + "\"");
            break;
          default:
            lastToken = T.tokenMinus; // just to allow for {(....)}
            return CONTINUE;
          }
        }
      }
    }

    // cd, echo, gotocmd, help, hover, javascript, label, message, and pause
    // all are implicitly strings. You CAN use "..." but you don't have to,
    // and you cannot use '...'. This way the introduction of single quotes 
    // as an equivalent of double quotes cannot break existing scripts. -- BH 06/2009

    if (lookingAtString(!T.tokAttr(tokCommand, T.implicitStringCommand))) {
      if (cchToken < 0)
        return ERROR(ERROR_endOfCommandUnexpected);
      String str;
      if ((tokCommand == T.set && nTokens == 2 && lastToken.tok == T.defaultdirectory
          || tokCommand == T.load || tokCommand == T.background || tokCommand == T.script)
          && !iHaveQuotedString) {
        if (lastToken.tok == T.inline) {
          str = getUnescapedStringLiteral();
        } else {
          str = script.substring(ichToken + 1, ichToken + cchToken - 1);
          if (str.indexOf("\\u") >= 0)
            str = Escape.unescapeUnicode(str);
        }
      } else {
        str = getUnescapedStringLiteral();
      }
      iHaveQuotedString = true;
      if (tokCommand == T.load && lastToken.tok == T.data
          || tokCommand == T.data && str.indexOf("@") < 0) {
        if (!getData(str))
          return ERROR(ERROR_missingEnd, "data");
      } else {
        addTokenToPrefix(T.o(T.string, str));
        if (T.tokAttr(tokCommand, T.implicitStringCommand))
          isEndOfCommand = true;
      }
      return CONTINUE;
    }
    if (tokCommand == T.sync && nTokens == 1 && charToken()) {
      String ident = script.substring(ichToken, ichToken + cchToken);
      int iident = Parser.parseInt(ident);
      if (iident == Integer.MIN_VALUE || Math.abs(iident) < 1000)
        addTokenToPrefix(T.o(T.identifier, ident));
      else
        addTokenToPrefix(T.i(iident));
      return CONTINUE;
    }
    switch (tokCommand) {
    case T.load:
    case T.script:
    case T.getproperty:
      if (script.charAt(ichToken) == '@') {
        iHaveQuotedString = true;
        return OK;
      }
      if (tokCommand == T.load) {
        if ((nTokens == 1 || nTokens == 2 && tokAt(1) == T.append) && lookingAtLoadFormat()) {
          String strFormat = script.substring(ichToken, ichToken + cchToken);
          T token = T.getTokenFromName(strFormat.toLowerCase());
          switch (token == null ? T.nada : token.tok) {
          case T.menu:
          case T.append:
            if (nTokens != 1)
              return ERROR;
            //$FALL-THROUGH$
          case T.data:
          case T.file:
          case T.inline:
          case T.model:
          case T.smiles:
          case T.trajectory:
          case T.sync:
            addTokenToPrefix(token);
            break;
          default:
            // skip entirely if not recognized
            int tok = (strFormat.indexOf("=") == 0
                || strFormat.indexOf("$") == 0 ? T.string : Parser.isOneOf(
                strFormat = strFormat.toLowerCase(),
                JC.LOAD_ATOM_DATA_TYPES) ? T.identifier : 0);
            if (tok != 0) {
              addTokenToPrefix(T.o(tok, strFormat));
              iHaveQuotedString = (tok == T.string);
            }
          }
          return CONTINUE;
        }
        BS bs;
        if (script.charAt(ichToken) == '{' || parenCount > 0)
          break;
        if ((bs = lookingAtBitset()) != null) {
          addTokenToPrefix(T.o(T.bitset, bs));
          return CONTINUE;
        }
      }
      if (!iHaveQuotedString
          && lookingAtImpliedString(false, tokCommand == T.load, 
              nTokens > 1 || tokCommand != T.script)) {
        String str = script.substring(ichToken, ichToken + cchToken);
        if (tokCommand == T.script && str.startsWith("javascript:")) {
          lookingAtImpliedString(true, true, true);
          str = script.substring(ichToken, ichToken + cchToken);
        }
        addTokenToPrefix(T.o(T.string, str));
        iHaveQuotedString = true;
        return CONTINUE;
      }
      break;
    case T.write:
      // write image 300 300 filename
      // write script filename
      // write spt filename
      // write jpg filename
      // write filename
      if (nTokens == 2 && lastToken.tok == T.frame)
        iHaveQuotedString = true;
      if (!iHaveQuotedString) {
        if (script.charAt(ichToken) == '@') {
          iHaveQuotedString = true;
          return OK;
        }
        if (lookingAtImpliedString(true, true, true)) {
          int pt = cchToken;
          String str = script.substring(ichToken, ichToken + cchToken);
          if (str.indexOf(" ") < 0) {
            addTokenToPrefix(T.o(T.string, str));
            iHaveQuotedString = true;
            return CONTINUE;
          }
          cchToken = pt;
        }
      }
      break;
    }
    if (T.tokAttr(tokCommand, T.implicitStringCommand)
        && !(tokCommand == T.script && iHaveQuotedString)
        && lookingAtImpliedString(true, true, true)) {
      String str = script.substring(ichToken, ichToken + cchToken);
      if (tokCommand == T.label
          && Parser.isOneOf(str.toLowerCase(), "on;off;hide;display"))
        addTokenToPrefix(T.getTokenFromName(str.toLowerCase()));
      else
        addTokenToPrefix(T.o(T.string, str));
      return CONTINUE;
    }
    float value;
    if (!Float.isNaN(value = lookingAtExponential())) {
      addTokenToPrefix(T.o(T.decimal, Float.valueOf(value)));
      return CONTINUE;
    }
    if (lookingAtObjectID(nTokens == 1)) {
      addTokenToPrefix(T.getTokenFromName("$"));
      addTokenToPrefix(T.o(T.identifier, script.substring(ichToken,
          ichToken + cchToken)));
      return CONTINUE;
    }
    if (lookingAtDecimal()) {
      value =  Parser.fVal(script.substring(ichToken, ichToken + cchToken));
      int intValue = (ScriptEvaluator.getFloatEncodedInt(script.substring(ichToken,
          ichToken + cchToken)));
      addTokenToPrefix(T.t(T.decimal, intValue, Float.valueOf(value)));
      return CONTINUE;
    }
    if (lookingAtSeqcode()) {
      ch = script.charAt(ichToken);
      try {
        int seqNum = (ch == '*' || ch == '^' ? Integer.MAX_VALUE : Integer
            .parseInt(script.substring(ichToken, ichToken + cchToken - 2)));
        char insertionCode = script.charAt(ichToken + cchToken - 1);
        if (insertionCode == '^')
          insertionCode = ' ';
        if (seqNum < 0) {
          seqNum = -seqNum;
          addTokenToPrefix(T.tokenMinus);
        }
        int seqcode = Group.getSeqcodeFor(seqNum, insertionCode);
        addTokenToPrefix(T.t(T.seqcode, seqcode, "seqcode"));
        return CONTINUE;
      } catch (NumberFormatException nfe) {
        return ERROR(ERROR_invalidExpressionToken, "" + ch);
      }
    }
    int val = lookingAtInteger();
    if (val != Integer.MAX_VALUE) {
      String intString = script.substring(ichToken, ichToken + cchToken);
      if (tokCommand == T.breakcmd || tokCommand == T.continuecmd) {
        if (nTokens != 1)
          return ERROR(ERROR_badArgumentCount);
        ScriptFlowContext f = (flowContext == null ? null : flowContext
            .getBreakableContext(val = Math.abs(val)));
        if (f == null)
          return ERROR(ERROR_badContext, (String) tokenCommand.value);
        tokenAt(0).intValue = f.pt0; // copy
      }
      if (val == 0 && intString.equals("-0"))
        addTokenToPrefix(T.tokenMinus);
      addTokenToPrefix(T.t(T.integer, val, intString));
      return CONTINUE;
    }
    if (!isMathExpressionCommand && parenCount == 0
        || lastToken.tok != T.identifier
        && !tokenAttr(lastToken, T.mathfunc)) {
      // here if:
      //   structure helix ({...})
      //   frame align ({...})
      //   polyhedra BONDS ({...})
      //   isosurface select ({...})
      //   isosurface within({...})
      // NOT 
      //   myfunc({...})
      //   mathFunc({...})
      // if you want to use a bitset there, you must use
      // bitsets properly: x.distance( ({1 2 3}) )
      boolean isBondOrMatrix = (script.charAt(ichToken) == '[');
      BS bs = lookingAtBitset();
      if (bs == null) {
        if (isBondOrMatrix) {
          Object m = lookingAtMatrix();
          if (m instanceof Matrix3f || m instanceof Matrix4f) {
            addTokenToPrefix(T.o((m instanceof Matrix3f ? T.matrix3f
                : T.matrix4f), m));
            return CONTINUE;
          }
        }
      } else {
        if (isBondOrMatrix)
          addTokenToPrefix(T.o(T.bitset, new BondSet(bs)));
        else
          addTokenToPrefix(T.o(T.bitset, bs));
        return CONTINUE;
      }
    }
    return OK;
  }

  private Object lookingAtMatrix() {
    int ipt;
    Object m;
    if (ichToken + 4 >= cchScript 
        || script.charAt(ichToken) != '[' || script.charAt(ichToken + 1) != '['
        || (ipt = script.indexOf("]]", ichToken)) < 0
        || (m = Escape.unescapeMatrix(script.substring(ichToken, ipt + 2))) == null)
      return null;
    cchToken = ipt + 2 - ichToken;
    return m;
  }

  private int parseKnownToken(String ident) {

    // specific token-based issues depend upon where we are in the command

    T token;

    if (tokLastMath != 0)
      tokLastMath = theTok;
    if (flowContext != null && flowContext.token.tok == T.switchcmd
        && flowContext.var != null && theTok != T.casecmd
        && theTok != T.defaultcmd && lastToken.tok != T.switchcmd)
      return ERROR(ERROR_badContext, ident);
    switch (theTok) {
    case T.identifier:
      if (nTokens == 0 && !checkImpliedScriptCmd) {
        if (ident.charAt(0) == '\'') {
          addTokenToPrefix(setCommand(T.tokenScript));
          cchToken = 0;
          return CONTINUE;
        }
        if (ichToken + cchToken < cchScript && script.charAt(ichToken + cchToken) == '.') {
          addTokenToPrefix(setCommand(T.tokenScript));
          nTokens = 1;
          cchToken = 0;
          checkImpliedScriptCmd = true;
          return CONTINUE;
        }
      }
      break;
    case T.andequals:
      if (nSemiSkip == forPoint3 && nTokens == ptSemi + 2) {
        token = lastToken;
        addTokenToPrefix(T.tokenEquals);
        addTokenToPrefix(token);
        token = T.getTokenFromName(ident.substring(0, 1));
        addTokenToPrefix(token);
        addTokenToPrefix(T.tokenLeftParen);
        needRightParen = true;
        return CONTINUE;
      }
      // check to see if we have a command name that
      // was not registered yet as a local variable:
      // var color = 3
      // color += 3
      checkNewSetCommand();
      if (tokCommand == T.set) {
        tokenAndEquals = T.getTokenFromName(ident.substring(0, 1));
        setEqualPt = ichToken;
        return OK;
      }
      if (tokCommand == T.slab || tokCommand == T.depth) {
        addTokenToPrefix(tokenCommand);
        replaceCommand(T.tokenSet);
        tokenAndEquals = T.getTokenFromName(ident.substring(0, 1));
        setEqualPt = ichToken;
        return OK;
      }
      // otherwise ignore
      return CONTINUE;
    case T.end:
    case T.endifcmd:
      if (flowContext != null)
        flowContext.forceEndIf = false;
      //$FALL-THROUGH$
    case T.elsecmd:
      if (nTokens > 0) {
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }
      break;
    case T.forcmd:
      if (bracketCount > 0) // ignore [FOR], as in 1C4D 
        break;
      //$FALL-THROUGH$
    case T.casecmd:
    case T.defaultcmd:
    case T.elseif:
    case T.ifcmd:
    case T.switchcmd:
    case T.whilecmd:
    case T.catchcmd:
      if (nTokens > 1 && tokCommand != T.set) {
        isEndOfCommand = true;
        if (flowContext != null)
          flowContext.forceEndIf = true;
        cchToken = 0;
        return CONTINUE;
      }
      break;
    case T.minusMinus:
    case T.plusPlus:
      if (!isNewSet && nTokens == 1)
        checkNewSetCommand();
      if (isNewSet && parenCount == 0 && bracketCount == 0
          && ichToken <= setEqualPt) {
        tokenizePlusPlus(theTok, false);
        return CONTINUE;
      } else if (nSemiSkip == forPoint3 && nTokens == ptSemi + 2) {
        token = lastToken;
        addTokenToPrefix(T.tokenEquals);
        addTokenToPrefix(token);
        addTokenToPrefix(theTok == T.minusMinus ? T.tokenMinus
            : T.tokenPlus);
        addTokenToPrefix(T.i(1));
        return CONTINUE;
      }
      break;
    case T.opEQ:
      if (parenCount == 0 && bracketCount == 0)
        setEqualPt = ichToken;
      break;
    case T.per:
      if (tokCommand == T.set && parenCount == 0 && bracketCount == 0
          && ichToken < setEqualPt) {
        ltoken.add(1, T.tokenExpressionBegin);
        addTokenToPrefix(T.tokenExpressionEnd);
        ltoken.set(0, T.tokenSetProperty);
        setEqualPt = 0;
      }
      break;
    case T.leftbrace:
      braceCount++;
      if (braceCount == 1 && parenCount == 0 && checkFlowStartBrace(false)) {
        isEndOfCommand = true;
        if (flowContext != null)
          flowContext.forceEndIf = false;
        return CONTINUE;
      }
      //$FALL-THROUGH$
    case T.leftparen:
      parenCount++;
      // the select() function uses dual semicolon notation
      // but we must differentiate from isosurface select(...) and set
      // picking select
      if (nTokens > 1
          && (lastToken.tok == T.select || lastToken.tok == T.forcmd || lastToken.tok == T.ifcmd))
        nSemiSkip += 2;
      break;
    case T.rightbrace:
      if (iBrace > 0 && parenCount == 0 && braceCount == 0) {
        ichBrace = ichToken;
        if (nTokens == 0) {
          braceCount = parenCount = 1;
        } else {
          braceCount = parenCount = nSemiSkip = 0;
          if (theToken.tok != T.casecmd && theToken.tok != T.defaultcmd)
            vBraces.addLast(theToken);
          iBrace++;
          isEndOfCommand = true;
          ichEnd = ichToken;
          return CONTINUE;
        }
      }
      braceCount--;
      //$FALL-THROUGH$
    case T.rightparen:
      parenCount--;
      if (parenCount < 0)
        return ERROR(ERROR_tokenUnexpected, ident);
      // we need to remove the semiskip if parentheses or braces have been
      // closed. 11.5.46
      if (parenCount == 0)
        nSemiSkip = 0;
      if (needRightParen) {
        addTokenToPrefix(T.tokenRightParen);
        needRightParen = false;
      }
      break;
    case T.leftsquare:
      if (ichToken > 0 && Character.isWhitespace(script.charAt(ichToken - 1)))
        addTokenToPrefix(T.tokenSpaceBeforeSquare);
      bracketCount++;
      break;
    case T.rightsquare:
      bracketCount--;
      if (bracketCount < 0)
        return ERROR(ERROR_tokenUnexpected, "]");
    }
    return OK;
  }

  private void tokenizePlusPlus(int tok, boolean isPlusPlusX) {
    //   ++ipt;   or ipt++
    if (isPlusPlusX) {
      setCommand(T.tokenSet);
      ltoken.add(0, tokenCommand);
    }
    nTokens = ltoken.size();
    addTokenToPrefix(T.tokenEquals);
    setEqualPt = 0;
    for (int i = 1; i < nTokens; i++)
      addTokenToPrefix(ltoken.get(i));
    addTokenToPrefix(tok == T.minusMinus ? T.tokenMinus
        : T.tokenPlus);
    addTokenToPrefix(T.i(1));
  }

  private boolean checkNewSetCommand() {
    String name = ltoken.get(0).value.toString();
    if (!isContextVariable(name.toLowerCase()))
      return false;
    T t = setNewSetCommand(false, name);
    setCommand(T.tokenSet);
    ltoken.add(0, tokenCommand);
    ltoken.set(1, t);
    return true;
  }

  private int parseCommandParameter(String ident) {
    // PART II:
    //
    // checking tokens based on the current command
    // all command starts are handled by case Token.nada

    nTokens = ltoken.size();
    switch (tokCommand) {
    case T.nada:
      // first token in command
      lastToken = T.tokenOff;
      ichCurrentCommand = ichEnd = ichToken;
      setCommand(theToken);
      
      if (T.tokAttr(tokCommand, T.flowCommand)) {
        lastFlowCommand = tokenCommand;
      }
      // before processing this command, check to see if we have completed
      // a right-brace.
      int ret = checkFlowEndBrace();
      if (ret == ERROR)
        return ERROR;
      else if (ret == CONTINUE) {
        // yes, so re-read this one
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }

      if (T.tokAttr(tokCommand, T.flowCommand)) {
        if (!checkFlowCommand((String) tokenCommand.value))
          return ERROR;
        theToken = tokenCommand;
        if (theTok == T.casecmd) {
          addTokenToPrefix(tokenCommand);
          theToken = T.tokenLeftParen;
        }
        break;
      }
      if (theTok == T.colon) {
        braceCount++;
        isEndOfCommand = true;
        break;
      }
      if (theTok == T.rightbrace) {
        // if }, just push onto vBrace, but NOT onto ltoken
        vBraces.addLast(tokenCommand);
        iBrace++;
        tokCommand = T.nada;
        return CONTINUE;
      }
      if (theTok != T.leftbrace)
        lastFlowCommand = null;

      if (T.tokAttr(tokCommand, T.scriptCommand))
        break;

      // not the standard command
      // isSetBrace: {xxx}.yyy = or {xxx}[xx].
      // isNewSet: xxx =
      // but not xxx = where xxx is a known "set xxx" variant
      // such as "set hetero" or "set hydrogen" or "set solvent"
      
      isSetBrace = (theTok == T.leftbrace);
      if (isSetBrace) {
        if (!lookingAtBraceSyntax()) {
          isEndOfCommand = true;
          if (flowContext != null)
            flowContext.forceEndIf = false;
        }
      } else {
        switch (theTok) {
        case T.plusPlus:
        case T.minusMinus:
          tokInitialPlusPlus = theTok;
          tokCommand = T.nada;
          return CONTINUE;
        case T.identifier:
        case T.var:
        case T.define:
        case T.leftparen:
          break;
        default:
          if (!T.tokAttr(theTok, T.misc)
              && !T.tokAttr(theTok, T.setparam)
              && !isContextVariable(ident)) {
            commandExpected();
            return ERROR;
          }
        }
      }
      theToken = setNewSetCommand(isSetBrace, ident);
      break;
    case T.catchcmd:
      switch(nTokens) {
      case 1:
        if (theTok != T.leftparen)
          return ERROR(ERROR_tokenExpected, "(");
        break; 
      case 2:
        if (theTok != T.rightparen)
          ((ContextToken)tokenCommand).name0 = ident;
        addContextVariable(ident);
        break;
      case 3:
        if (theTok != T.rightparen)
          return ERROR(ERROR_tokenExpected, ")");
        isEndOfCommand = true;
        ichEnd = ichToken + 1;
        flowContext.setLine();
        break;
      default:
        return ERROR(ERROR_badArgumentCount);
      }
      break;
    case T.parallel:
    case T.function:
      if (tokenCommand.intValue == 0) {
        if (nTokens != 1)
          break; // anything after name is ok
        // user has given macro command
        tokenCommand.value = ident;
        return CONTINUE; // don't store name in stack
      }
      if (nTokens == 1) {
        if (thisFunction != null)
          vFunctionStack.add(0, thisFunction);
        thisFunction = (tokCommand == T.parallel ? newScriptParallelProcessor(ident, tokCommand) : new ScriptFunction(ident, tokCommand));
        htUserFunctions.put(ident, Boolean.TRUE);
        flowContext.setFunction(thisFunction);
        break; // function f
      }
      if (nTokens == 2) {
        if (theTok != T.leftparen)
          return ERROR(ERROR_tokenExpected, "(");
        break; // function f (
      }
      if (nTokens == 3 && theTok == T.rightparen)
        break; // function f ( )
      if (nTokens % 2 == 0) {
        // function f ( x , y )
        if (theTok != T.comma && theTok != T.rightparen)
          return ERROR(ERROR_tokenExpected, ")");
        break;
      }
      thisFunction.addVariable(ident, true);
      break;
    case T.casecmd:
      if (nTokens > 1 && parenCount == 0 && braceCount == 0 && theTok == T.colon) {
        addTokenToPrefix(T.tokenRightParen);
        braceCount = 1;
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }
      break;
    case T.defaultcmd:
      if (nTokens > 1) {
        braceCount = 1;
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }
      break;
    case T.elsecmd:
      if (nTokens == 1 && theTok != T.ifcmd) {
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }
      if (nTokens != 1 || theTok != T.ifcmd && theTok != T.leftbrace)
        return ERROR(ERROR_badArgumentCount);
      replaceCommand(flowContext.token = new ContextToken(T.elseif, "elseif"));
      tokCommand = T.elseif;
      return CONTINUE;
    case T.var:
      if (nTokens != 1)
        break;
      addContextVariable(ident);
      replaceCommand(T.tokenSetVar);
      tokCommand = T.set;
      break;
    case T.end:
      if (nTokens != 1)
        return ERROR(ERROR_badArgumentCount);
      if (!checkFlowEnd(theTok, ident, ichCurrentCommand))
        return ERROR;
      if (theTok == T.function || theTok == T.parallel) {
        return CONTINUE;
      }
      break;
    case T.switchcmd:
    case T.whilecmd:
      if (nTokens > 2 && braceCount == 0 && parenCount == 0) {
        isEndOfCommand = true;
        ichEnd = ichToken + 1;
        flowContext.setLine();
      }
      break;
    case T.elseif:
    case T.ifcmd:
      if (nTokens > 2 && braceCount == 0 && parenCount == 0) {
        // so { or : end up new commands
        isEndOfCommand = true;
        ichEnd = ichToken + 1;
        flowContext.setLine();
      }
      break;
    case T.process: 
      isEndOfCommand = true;
      ichEnd = ichToken + 1;
      flowContext.setLine();
      break;
    case T.forcmd:
      if (nTokens == 1) {
        if (theTok != T.leftparen)
          return ERROR(ERROR_unrecognizedToken, ident);
        forPoint3 = nSemiSkip = 0;
        nSemiSkip += 2;
      } else if (nTokens == 3 && tokAt(2) == T.var) {
        addContextVariable(ident);
      } else if ((nTokens == 3 || nTokens == 4) && theTok == T.in) {
        // for ( var x IN
        // for ( x IN
        nSemiSkip -= 2;
        forPoint3 = 2;
        addTokenToPrefix(theToken);
        theToken = T.tokenLeftParen;
      } else if (braceCount == 0 && parenCount == 0) {
        isEndOfCommand = true;
        ichEnd = ichToken + 1;
        flowContext.setLine();
      }
      break;
    case T.set:
      if (theTok == T.leftbrace)
        setBraceCount++;
      else if (theTok == T.rightbrace) {
        setBraceCount--;
        if (isSetBrace && setBraceCount == 0
            && ptNewSetModifier == Integer.MAX_VALUE)
          ptNewSetModifier = nTokens + 1;
      }
      if (nTokens == ptNewSetModifier) { // 1 when { is not present
        T token = tokenAt(0);
        if (theTok == T.leftparen || isUserFunction(token.value.toString())) {
          // mysub(xxx,xxx,xxx)
          ltoken.set(0, setCommand(T.t(T.identifier, 0, token.value)));
          setBraceCount = 0;
          break;
        }
        if (theTok != T.identifier && theTok != T.andequals && theTok != T.define
            && (!T.tokAttr(theTok, T.setparam))) {
          if (isNewSet)
            commandExpected();
          else
            errorIntStr2(ERROR_unrecognizedParameter, "SET", ": " + ident);
          return ERROR;
        }
        if (nTokens == 1
            && (lastToken.tok == T.plusPlus || lastToken.tok == T.minusMinus)) {
          replaceCommand(T.tokenSet);
          addTokenToPrefix(lastToken);
          break;
        }
      }
      break;
    case T.load:
      if (theTok == T.define && (nTokens == 1 || lastToken.tok == T.filter || lastToken.tok == T.spacegroup)) {
        addTokenToPrefix(T.tokenDefineString);
        return CONTINUE;          
      }
      if (theTok == T.as)
        iHaveQuotedString = false;
      break;
    case T.display:
    case T.hide:
    case T.restrict:
    case T.select:
    case T.delete:
    case T.define:
      if (tokCommand == T.define) {
        if (nTokens == 1) {
          // we are looking at the variable name
          if (theTok != T.identifier) {
            if (preDefining) {
              if (!T.tokAttr(theTok, T.predefinedset)) {
                errorStr2(
                    "ERROR IN Token.java or JmolConstants.java -- the following term was used in JmolConstants.java but not listed as predefinedset in Token.java: "
                        + ident, null);
                return ERROR;
              }
            } else if (T.tokAttr(theTok, T.predefinedset)) {
              Logger
                  .warn("WARNING: predefined term '"
                      + ident
                      + "' has been redefined by the user until the next file load.");
            } else if (!isCheckOnly && ident.length() > 1) {
              Logger
                  .warn("WARNING: redefining "
                      + ident
                      + "; was "
                      + theToken
                      + "not all commands may continue to be functional for the life of the applet!");
              theTok = theToken.tok = T.identifier;
              T.addToken(ident, theToken);
            }
          }
          addTokenToPrefix(theToken);
          lastToken = T.tokenComma;
          return CONTINUE;
        }
        if (nTokens == 2) {
          if (theTok == T.opEQ) {
            // we are looking at @x =.... just insert a SET command
            // and ignore the =. It's the same as set @x ...
            ltoken.add(0, T.tokenSet);
            return CONTINUE;
          }
        }
      }
      if (bracketCount == 0 && theTok != T.identifier
          && !T.tokAttr(theTok, T.expression)
          && !T.tokAttr(theTok, T.misc)
          && (theTok & T.minmaxmask) != theTok)
        return ERROR(ERROR_invalidExpressionToken, ident);
      break;
    case T.center:
      if (theTok != T.identifier && theTok != T.dollarsign
          && !T.tokAttr(theTok, T.expression))
        return ERROR(ERROR_invalidExpressionToken, ident);
      break;
    case T.plot3d:
    case T.pmesh:
    case T.isosurface:
      // isosurface ... name.xxx
      char ch = nextChar();
      if (parenCount == 0 && bracketCount == 0
          && ".:/\\+-!?".indexOf(ch) >= 0 && !(ch == '-' && ident.equals("=")))
        checkUnquotedFileName();
    }
    return OK;
  }

  private static ScriptFunction newScriptParallelProcessor(String name, int tok) {
    ScriptFunction jpp = (ScriptFunction) Interface.getOptionInterface("parallel.ScriptParallelProcessor");
    jpp.set(name, tok);
    return jpp;
  }

  private T setNewSetCommand(boolean isSetBrace, String ident) {
    tokCommand = T.set;
    isNewSet = (!isSetBrace && !isUserFunction(ident));
    setBraceCount = (isSetBrace ? 1 : 0);
    bracketCount = 0;
    setEqualPt = Integer.MAX_VALUE;
    ptNewSetModifier = (isNewSet ? (ident.equals("(") ? 2 : 1) : Integer.MAX_VALUE);
    return ((isSetBrace || theToken.tok == T.plusPlus || theToken.tok == T.minusMinus)? theToken : T.o(T.identifier, ident));
  }

  private char nextChar() {
    int ich = ichToken + cchToken;
    return (ich >= cchScript ? ' ' : script.charAt(ich));
  }

  private void checkUnquotedFileName() {
    int ichT = ichToken;
    char ch;
    while (++ichT < cchScript 
        && !Character.isWhitespace(ch = script.charAt(ichT)) 
        && ch != '#' && ch != ';' && ch != '}') {
    }
    String name = script.substring(ichToken, ichT).replace('\\','/');
    cchToken = ichT - ichToken;
    theToken = T.o(T.string, name);   
  }

  private boolean checkFlowStartBrace(boolean atEnd) {
    if ((!T.tokAttr(tokCommand, T.flowCommand)
        || tokCommand == T.breakcmd || tokCommand == T.continuecmd))
      return false;
    if (atEnd) {
      if (tokenCommand.tok != T.casecmd && tokenCommand.tok != T.defaultcmd) {
        iBrace++;
        vBraces.addLast(tokenCommand);
        lastFlowCommand = null;
      }
      parenCount = braceCount = 0;
    }
    return true;
  }

  JmolList<T> vPush = new  JmolList<T>();
  int pushCount;
  
  private int checkFlowEndBrace() {
    
    if (iBrace <= 0
        || vBraces.get(iBrace - 1).tok != T.rightbrace)
      return OK;
    // time to execute end
    vBraces.remove(--iBrace);
    T token = vBraces.remove(--iBrace);
    if (theTok == T.leftbrace) {
      braceCount--;
      parenCount--;
    }
    if (token.tok == T.push) {
      vPush.remove(--pushCount);
      addTokenToPrefix(setCommand(new ContextToken(T.pop, 0, "}")));
      isEndOfCommand = true;
      return CONTINUE;
    }
    switch (flowContext == null ? 0 : 
      flowContext.token.tok) {
    case T.ifcmd:
    case T.elseif:
    case T.elsecmd:      
      if (tokCommand == T.elsecmd || tokCommand == T.elseif)
        return OK;
      break;
    case T.switchcmd:
    case T.casecmd:
    case T.defaultcmd:
      if (tokCommand == T.casecmd || tokCommand == T.defaultcmd)
        return OK;
    }
    return forceFlowEnd(token);
  }

  private int forceFlowEnd(T token) {    
    T t0 = tokenCommand;    
    setCommand(T.o(T.end, "end"));
    if (!checkFlowCommand("end"))
      return T.nada;
    addTokenToPrefix(tokenCommand);
    switch (token.tok) {
    case T.ifcmd:
    case T.elsecmd:
    case T.elseif:
      token = T.tokenIf;
      break;
    case T.defaultcmd:
    case T.casecmd:
      token = T.tokenSwitch;
      break;
    default:
      token = T.getTokenFromName((String)token.value);
      break;
    }
    if (!checkFlowEnd(token.tok, (String)token.value, ichBrace))
      return ERROR;
    if (token.tok != T.function && token.tok != T.parallel
        && token.tok != T.trycmd)
      addTokenToPrefix(token);
    setCommand(t0);
    return CONTINUE;
  }

  static boolean isBreakableContext(int tok) {
    return tok == T.forcmd 
      || tok == T.process
      || tok == T.whilecmd 
      || tok == T.casecmd 
      || tok == T.defaultcmd;
  }

  private boolean checkFlowCommand(String ident) {
    int pt = lltoken.size();
    boolean isEnd = false;
    boolean isNew = true;
    switch (tokCommand) {
    case T.function:
    case T.parallel:
      if (flowContext != null)
        return errorStr(ERROR_badContext, T.nameOf(tokCommand));
      break;
    case T.end:
      if (flowContext == null)
        return errorStr(ERROR_badContext, ident);
      isEnd = true;
      if (flowContext.token.tok != T.function && flowContext.token.tok != T.parallel
          && flowContext.token.tok != T.trycmd)
        setCommand(T.t(tokCommand, (flowContext.ptDefault > 0 ? flowContext.ptDefault : -flowContext.pt0), ident)); //copy
      break;
    case T.trycmd:
    case T.catchcmd:
      break;
    case T.forcmd:
    case T.ifcmd:
    case T.process:
    case T.switchcmd:
    case T.whilecmd:
      break;
    case T.endifcmd:
      isEnd = true;
      if (flowContext == null 
          || flowContext.token.tok != T.ifcmd
          && flowContext.token.tok != T.process
          && flowContext.token.tok != T.elsecmd
          && flowContext.token.tok != T.elseif)
        return errorStr(ERROR_badContext, ident);
      break;
    case T.elsecmd:
      if (flowContext == null || flowContext.token.tok != T.ifcmd
          && flowContext.token.tok != T.elseif)
        return errorStr(ERROR_badContext, ident);
      flowContext.token.intValue = flowContext.setPt0(pt, false);
      break;
    case T.breakcmd:
    case T.continuecmd:
      isNew = false;
      ScriptFlowContext f = (flowContext == null ? null : flowContext.getBreakableContext(0));
      if (tokCommand == T.continuecmd)
        while (f != null  && f.token.tok != T.forcmd && f.token.tok != T.whilecmd)
          f = f.getParent();
      if (f == null)
        return errorStr(ERROR_badContext, ident);
      setCommand( T.t(tokCommand, f.pt0, ident)); //copy
      break;
    case T.defaultcmd:
      if (flowContext == null 
          || flowContext.token.tok != T.switchcmd
          && flowContext.token.tok != T.casecmd
          && flowContext.ptDefault > 0)
        return errorStr(ERROR_badContext, ident);
      flowContext.token.intValue = flowContext.setPt0(pt, true);
      break;
    case T.casecmd:
      if (flowContext == null 
          || flowContext.token.tok != T.switchcmd
          && flowContext.token.tok != T.casecmd
          && flowContext.token.tok != T.defaultcmd)
        return errorStr(ERROR_badContext, ident);
      flowContext.token.intValue = flowContext.setPt0(pt, false);
      break;
    case T.elseif:
      if (flowContext == null || flowContext.token.tok != T.ifcmd
          && flowContext.token.tok != T.elseif
          && flowContext.token.tok != T.elsecmd)
        return errorStr(ERROR_badContext, "elseif");
      flowContext.token.intValue = flowContext.setPt0(pt, false);
      break;
    }
    if (isEnd) {
      flowContext.token.intValue = (tokCommand == T.catchcmd ? -pt : pt);
      if (tokCommand == T.endifcmd)
        flowContext = flowContext.getParent();
      if (tokCommand == T.trycmd) {
        
      }
    } else if (isNew) {
      ContextToken ct = new ContextToken(tokCommand, tokenCommand.value);
      setCommand(ct); //copy
      switch (tokCommand) {
      case T.trycmd:
        flowContext = new ScriptFlowContext(this, ct, pt, flowContext);
        if (thisFunction != null)
          vFunctionStack.add(0, thisFunction);
        thisFunction = newScriptParallelProcessor("", tokCommand);
        flowContext.setFunction(thisFunction);
        pushCount++;
        vPush.addLast(ct);
        break;
      case T.elsecmd:
      case T.elseif:
        flowContext.token = ct;
        break;
      case T.casecmd:
      case T.defaultcmd:
        ct.contextVariables = flowContext.token.contextVariables;
        flowContext.token = ct;
        break;
      case T.process:
      case T.forcmd:
      case T.whilecmd:
      case T.catchcmd:
        pushCount++;
        vPush.addLast(ct);
        //$FALL-THROUGH$
      case T.ifcmd:
      case T.switchcmd:
      default:
        flowContext = new ScriptFlowContext(this, ct, pt, flowContext);
        break;
      }
    }
    return true;
  }

  private boolean checkFlowEnd(int tok, String ident, int pt1) {
    if (flowContext == null || flowContext.token.tok != tok) {
      boolean isOK = true;
      switch(tok) {
      case T.ifcmd:
        isOK = (flowContext.token.tok == T.elsecmd
            || flowContext.token.tok == T.elseif);
        break;
      case T.switchcmd:
        isOK = (flowContext.token.tok == T.casecmd
            || flowContext.token.tok == T.defaultcmd);
        break;
      default:
        isOK = false;
      }
      if (!isOK)
        return errorStr(ERROR_badContext, "end " + ident);
    }
    switch (tok) {
    case T.ifcmd:
    case T.switchcmd:
      break;
    case T.catchcmd:
    case T.forcmd:
    case T.process:
    case T.whilecmd:
      vPush.remove(--pushCount);
      break;
    case T.parallel:
    case T.function:
    case T.trycmd:
      if (!isCheckOnly) {
        addTokenToPrefix(T.o(tok, thisFunction));
        ScriptFunction.setFunction(thisFunction, script, pt1, lltoken.size(),
            lineNumbers, lineIndices, lltoken);
      }
      thisFunction = (vFunctionStack.size() == 0 ? null : (ScriptFunction) vFunctionStack.remove(0));
      tokenCommand.intValue = 0;
      if (tok == T.trycmd)
        vPush.remove(--pushCount);
      break;
    default:
      return errorStr(ERROR_unrecognizedToken, "end " + ident);
    }
    flowContext = flowContext.getParent();
    return true;
  }

  private boolean getData(String key) {
    addTokenToPrefix(T.o(T.string, key));
    ichToken += key.length() + 2;
    if (script.length() > ichToken && script.charAt(ichToken) == '\r') {
      lineCurrent++;ichToken++;
    }
    if (script.length() > ichToken && script.charAt(ichToken) == '\n') {
      lineCurrent++;ichToken++;
    }
    int i = script.indexOf(chFirst + key + chFirst, ichToken) - 4;
    if (i < 0 || !script.substring(i, i + 4).equalsIgnoreCase("END "))
      return false;
    String str = script.substring(ichToken, i);
    incrementLineCount(str);
    addTokenToPrefix(T.o(T.data, str));
    addTokenToPrefix(T.o(T.identifier, "end"));
    addTokenToPrefix(T.o(T.string, key));
    cchToken = i - ichToken + key.length() + 6;
    return true;
  }

  private int incrementLineCount(String str) {
    char ch;
    int pt = str.indexOf('\r');
    int pt2 = str.indexOf('\n');
    if (pt < 0 && pt2 < 0)
      return 0;
    int n = lineCurrent;
    if (pt < 0 || pt2 < pt)
      pt = pt2;
    for (int i = str.length(); --i >= pt;) {
      if ((ch = str.charAt(i)) == '\n' || ch == '\r')
        lineCurrent++;
    }
    return lineCurrent - n;
  }
  
  private static boolean isSpaceOrTab(char ch) {
    return ch == ' ' || ch == '\t';
  }

  private boolean eol(char ch) {
    return (ch == '\r' || ch == '\n' || ch == ';' && nSemiSkip <= 0);  
  }

  private boolean lookingAtBraceSyntax() {
    // isSetBrace: {xxx}.yyy =  or {xxx}[xx].
    int ichT = ichToken;
    int nParen = 1;
    while (++ichT < cchScript && nParen > 0) {
      switch (script.charAt(ichT)) {
      case '{':
        nParen++;
        break;
      case '}':
        nParen--;
      break;
      }
    }
    if (ichT < cchScript && script.charAt(ichT) == '[' && ++nParen == 1)
      while (++ichT < cchScript && nParen > 0) {
        switch (script.charAt(ichT)) {
        case '[':
          nParen++;
          break;
        case ']':
          nParen--;
        break;
        }
      }
    if (ichT < cchScript && script.charAt(ichT) == '.' && nParen == 0) {
      return true;
    }
    
    return false;
  }

  private char chFirst = '\0';
  private boolean lookingAtString(boolean allowPrime) {
    if (ichToken == cchScript)
      return false;
    chFirst = script.charAt(ichToken);
    if (chFirst != '"' && (!allowPrime || chFirst != '\''))
      return false;
    int ichT = ichToken;
    char ch;
    boolean previousCharBackslash = false;
    while (++ichT < cchScript) {
      ch = script.charAt(ichT);
      if (ch == chFirst && !previousCharBackslash)
        break;
      previousCharBackslash = (ch == '\\' ? !previousCharBackslash : false);
    }
    if (ichT == cchScript) {
      cchToken = -1;
      ichEnd = cchScript;
    } else {
      cchToken = ++ichT - ichToken;
    }
    return true;
  }

  String getUnescapedStringLiteral() {
    if (cchToken < 2)
      return "";
    SB sb = SB.newN(cchToken - 2);
    int ichMax = ichToken + cchToken - 1;
    int ich = ichToken + 1;
    while (ich < ichMax) {
      char ch = script.charAt(ich++);
      if (ch == '\\' && ich < ichMax) {
        ch = script.charAt(ich++);
        switch (ch) {
        case 'b':
          ch = '\b';
          break;
        case 'n':
          ch = '\n';
          break;
        case 't':
          ch = '\t';
          break;
        case 'r':
          ch = '\r';
          //$FALL-THROUGH$
        case '"':
        case '\\':
        case '\'':
          break;
        case 'x':
        case 'u':
          int digitCount = ch == 'x' ? 2 : 4;
          if (ich < ichMax) {
            int unicode = 0;
            for (int k = digitCount; --k >= 0 && ich < ichMax;) {
              char chT = script.charAt(ich);
              int hexit = Escape.getHexitValue(chT);
              if (hexit < 0)
                break;
              unicode <<= 4;
              unicode += hexit;
              ++ich;
            }
            ch = (char) unicode;
          }
        }
      }
      sb.appendC(ch);
    }
    return sb.toString();
  }

  private boolean lookingAtLoadFormat() {
    // just allow a simple word or =xxxx or $CCCC
    // old load formats are simple unneeded words like PDB or XYZ -- no numbers
    int ichT = ichToken;
    char ch = '\0';
    boolean allchar = (ichT < cchScript && Viewer.isDatabaseCode(ch = script.charAt(ichT)));
    while (ichT < cchScript
        && (Character.isLetterOrDigit(ch = script.charAt(ichT))
            && (allchar || Character.isLetter(ch))
            || allchar && (!eol(ch) && !Character.isWhitespace(ch))))
      ++ichT;
    if (!allchar && ichT == ichToken || !isSpaceOrTab(ch))
      return false;
    cchToken = ichT - ichToken;
    return true;
  }

  /**
   * An "implied string" is a parameter that is not quoted but because of its
   * position in a command is implied to be a string. First we must exclude
   * the  @xxxx. Then we consume the entire math syntax @{......} or any set of
   * characters not involving white space. echo, hover, label, message, pause
   * are odd-valued; no initial parsing of variables for them.
   * 
   * @param allowSpace
   * @param allowEquals
   *        as in the load command, first parameter load =xxx but NOT any other
   *        command
   * @param allowSptParen
   *        specifically for script/load command, first parameter xxx.spt(3,4,4)
   * 
   * @return true or false
   */
  private boolean lookingAtImpliedString(boolean allowSpace,
                                         boolean allowEquals,
                                         boolean allowSptParen) {
    int ichT = ichToken;
    char ch = script.charAt(ichT);
    boolean parseVariables = !(T.tokAttr(tokCommand,
        T.implicitStringCommand) || (tokCommand & 1) == 0);
    boolean isVariable = (ch == '@');
    boolean isMath = (isVariable && ichT + 3 < cchScript && script
        .charAt(ichT + 1) == '{');
    if (isMath && parseVariables) {
      ichT = TextFormat.ichMathTerminator(script, ichToken + 1, cchScript);
      return (ichT != cchScript && (cchToken = ichT + 1 - ichToken) > 0);
    }
    int ptSpace = -1;
    int ptLastChar = -1;
    // look ahead to \n, \r, terminal ;, or }
    boolean isOK = true;
    int parenpt = 0;
    while (isOK && ichT < cchScript && !eol(ch = script.charAt(ichT))) {
      switch (ch) {
      case '(':
        if (!allowSptParen) {
          // script command
          if (ichT >= 5 && ( 
              script.substring(ichT - 4, ichT).equals(".spt")
              || script.substring(ichT - 4, ichT).equals(".png")
              || script.substring(ichT - 5, ichT).equals(".pngj"))) {
            isOK = false;
            continue;
          }
        }
        break;
      case '=':
        if (!allowEquals) {
          isOK = false;
          continue;
        }
        break;
      case '{':
        parenpt++;
        break;
      case '}':
        // only consider this if it is extra
        parenpt--;
        if (parenpt < 0 && (braceCount > 0 || iBrace > 0)) {
          isOK = false;
          continue;
        }
        break;
      default:
        if (Character.isWhitespace(ch)) {
          if (ptSpace < 0)
            ptSpace = ichT;
        } else {
          ptLastChar = ichT;
        }
        break;
      }
      if (Character.isWhitespace(ch)) {
        if (ptSpace < 0)
          ptSpace = ichT;
      } else {
        ptLastChar = ichT;
      }
      ++ichT;
    }
    // message/echo/label @x
    // message/echo/label @{.....}
    // message/echo/label @{....} testing  NOT ok
    // message/echo/label @x bananas OK -- "@x bananas"
    // {... message/echo label ok }  
    if (allowSpace)
      ichT = ptLastChar + 1;
    else if (ptSpace > 0)
      ichT = ptSpace;
    if (isVariable && ptSpace < 0 && parenpt <= 0 && ichT - ichToken > 1) {
      // if we have @xxx then this is not an implied string
      return false;
    }
    return (cchToken = ichT - ichToken) > 0;
  }

  private float lookingAtExponential() {
    if (ichToken == cchScript)
      return Float.NaN; //end
    int ichT = ichToken;
    int pt0 = ichT;
    if (script.charAt(ichT) == '-')
      ++ichT;
    boolean isOK = false;
    char ch = 'X';
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
      ++ichT;
      isOK = true;
    }
    if (ichT < cchScript && ch == '.')
      ++ichT;
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
      ++ichT;
      isOK = true;
    }
    if (ichT == cchScript || !isOK)
      return Float.NaN; //integer
    isOK = (ch != 'E' && ch != 'e');
    if (isOK || ++ichT == cchScript)
      return Float.NaN;
    ch = script.charAt(ichT);
    // I THOUGHT we only should allow "E+" or "E-" here, not "2E1" because
    // "2E1" might be a PDB het group by that name. BUT it turns out that
    // any HET group starting with a number is unacceptable and must
    // be given as [nXm], in brackets.

    if (ch == '-' || ch == '+')
      ichT++;
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
      ichT++;
      isOK = true;
    }
    if (!isOK)
      return Float.NaN;
    cchToken = ichT - ichToken;
    return (float) Parser.dVal(script.substring(pt0, ichT));
  }

  private boolean lookingAtDecimal() {
    if (ichToken == cchScript)
      return false;
    int ichT = ichToken;
    if (script.charAt(ichT) == '-')
      ++ichT;
    boolean digitSeen = false;
    char ch = 'X';
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT++)))
      digitSeen = true;
    if (ch != '.')
      return false;
    // only here if  "dddd."

    // to support 1.ca, let's check the character after the dot
    // to determine if it is an alpha
    char ch1;
    if (ichT < cchScript && !eol(ch1 = script.charAt(ichT))) {
      if (Character.isLetter(ch1) || ch1 == '?' || ch1 == '*')
        return false;
      //well, guess what? we also have to look for 86.1Na, so...
      //watch out for moveto..... 56.;refresh...
      if (ichT + 1 < cchScript
          && (Character.isLetter(ch1 = script.charAt(ichT + 1)) || ch1 == '?'))
        return false;
    }
    while (ichT < cchScript && Character.isDigit(script.charAt(ichT))) {
      ++ichT;
      digitSeen = true;
    }
    cchToken = ichT - ichToken;
    return digitSeen;
  }

  private boolean lookingAtSeqcode() {
    int ichT = ichToken;
    char ch = ' ';
    if (ichT + 1 < cchScript && script.charAt(ichT) == '*'
        && script.charAt(ichT + 1) == '^') {
      ch = '^';
      ++ichT;
    } else {
      if (script.charAt(ichT) == '-')
        ++ichT;
      while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT)))
        ++ichT;
    }
    if (ch != '^')
      return false;
    ichT++;
    if (ichT == cchScript)
      ch = ' ';
    else
      ch = script.charAt(ichT++);
    if (ch != ' ' && ch != '*' && ch != '?' && !Character.isLetter(ch))
      return false;
    cchToken = ichT - ichToken;
    return true;
  }

  private int lookingAtInteger() {
    if (ichToken == cchScript)
      return Integer.MAX_VALUE;
    int ichT = ichToken;
    if (script.charAt(ichToken) == '-')
      ++ichT;
    int ichBeginDigits = ichT;
    while (ichT < cchScript && Character.isDigit(script.charAt(ichT)))
      ++ichT;
    if (ichBeginDigits == ichT)
      return Integer.MAX_VALUE;
    cchToken = ichT - ichToken;
    try {
      int val = Integer.parseInt(script.substring(ichToken, ichT));
      return val;
    } catch (NumberFormatException e) {
      // ignore
    }
    return Integer.MAX_VALUE;
  }

  BS lookingAtBitset() {
    // ({n n:m n}) or ({null})
    // [{n:m}] is a BOND bitset
    // EXCEPT if the previous token was a function:
    // {carbon}.distance({3 3 3})
    // Yes, I wish I had used {{...}}, but this will work. 
    // WITHIN ({....}) unfortunately has two contexts
    
    if (script.indexOf("({null})", ichToken) == ichToken) {
      cchToken = 8;
      return new BS();
    }
    int ichT;
    if (ichToken + 4 > cchScript 
        || script.charAt(ichToken + 1) != '{'
        || (ichT = script.indexOf("}", ichToken)) < 0
        || ichT + 1 == cchScript)
    return null;
    BS bs = Escape.uB(script.substring(ichToken, ichT + 2));
    if (bs != null)
      cchToken = ichT + 2 - ichToken;
    return bs;
  }
  
  private boolean lookingAtObjectID(boolean allowWildID) {
    int ichT = ichToken;
    if (ichT == cchScript || script.charAt(ichT) != '$')
      return false;
    if (++ichT != cchScript && script.charAt(ichT) == '"')
      return false;
    while (ichT < cchScript) {
      char ch;
      if (Character.isWhitespace(ch = script.charAt(ichT))) {
        if (ichT == ichToken + 1)
          return false;
        break;
      }
      if (!Character.isLetterOrDigit(ch)) {
        switch (ch) {
        default:
          return false;
        case '*':
          if (!allowWildID)
            return false;
          break;
        case '~':
        case '_':
          break;
        }
      }
      ichT++;
    }
    cchToken = ichT - (++ichToken);
    return true;
  }

  private boolean lookingAtLookupToken(int ichT) {
    if (ichT == cchScript)
      return false;
    int ichT0 = ichT;
    tokLastMath = 0;
    char ch;
    switch (ch = script.charAt(ichT++)) {
    case '-':
    case '+':
    case '&':
    case '|':
    case '*':
      if (ichT < cchScript) {
        if (script.charAt(ichT) == ch) {
          ++ichT;
          if (ch == '-' || ch == '+')
            break;
          if (ch == '&' && ichT < cchScript && script.charAt(ichT) == ch)
            ++ichT; // &&&
        } else if (script.charAt(ichT) == '=') {
          ++ichT;
        }
      }
      tokLastMath = 1;
      break;
    case '/':
      if (ichT < cchScript && script.charAt(ichT) == '/')
        break;
      //$FALL-THROUGH$
    case '\\':  // leftdivide
    case '!':
      if (ichT < cchScript && script.charAt(ichT) == '=')
        ++ichT;
      tokLastMath = 1;
      break;
    case ')':
    case ']':
    case '}':
    case '.':
      break;
    case '@':
    case '{':
      tokLastMath = 2; // NOT considered a continuation if at beginning of a line
      break;
    case ':':
      tokLastMath = 1;
      break;
    case '(':
    case ',':
    case '$':
    case ';':
    case '[':
    case '%':
      tokLastMath = 1;
      break;
    case '<':
    case '=':
    case '>':
      if (ichT < cchScript
          && ((ch = script.charAt(ichT)) == '<' || ch == '=' || ch == '>'))
        ++ichT;
      tokLastMath = 1;
      break;
    default:
      if (!Character.isLetter(ch))
        return false;
    //$FALL-THROUGH$
    case '~':
    case '_':
    case '\'':
    case '?': // include question marks in identifier for atom expressions
      if (ch == '?')
        tokLastMath = 1;
      while (ichT < cchScript
          && (Character.isLetterOrDigit(ch = script.charAt(ichT)) 
              || ch == '_' || ch == '?' || ch == '~' || ch == '\'')
          ||
          // hack for insertion codes embedded in an atom expression :-(
          // select c3^a
          (ch == '^' && ichT > ichT0 && Character.isDigit(script
              .charAt(ichT - 1)))
          || ch == '\\' && ichT + 1 < cchScript && script.charAt(ichT + 1) == '?')
        ++ichT;
      break;
    }
    cchToken = ichT - ichT0;
    return true;
  }

  private boolean charToken() {
    char ch;
    if (ichToken == cchScript || (ch = script.charAt(ichToken)) == '"' || ch == '@')
      return false;
    int ichT = ichToken;
    while (ichT < cchScript && !isSpaceOrTab(ch = script.charAt(ichT)) 
        && ch != '#' && ch != '}' && !eol(ch))
        ++ichT;
    cchToken = ichT - ichToken;
    return true;
  }
 
  
  private int ERROR(int error) {
    errorIntStr2(error, null, null);
    return ERROR;
  }
  
  private int ERROR(int error, String value) {
    errorStr(error, value);
    return ERROR;
  }
  
  private boolean handleError() {
    errorType = errorMessage;
    errorLine = script.substring(ichCurrentCommand, ichEnd <= ichCurrentCommand ? ichToken : ichEnd);
    String lineInfo = (ichToken < ichEnd 
        ? errorLine.substring(0, ichToken - ichCurrentCommand)
              + " >>>> " + errorLine.substring(ichToken - ichCurrentCommand) 
        : errorLine)
        + " <<<<";
    errorMessage = GT._("script compiler ERROR: ") + errorMessage
         + ScriptEvaluator.setErrorLineMessage(null, filename, lineCurrent, iCommand, lineInfo);
    if (!isSilent) {
      ichToken = Math.max(ichEnd, ichToken);
      while (!lookingAtEndOfLine() && !lookingAtEndOfStatement())
        ichToken++;
      errorLine = script.substring(ichCurrentCommand, ichToken);      
      viewer.addCommand(errorLine + CommandHistory.ERROR_FLAG);
      Logger.error(errorMessage);
    }
    return false;
  }


}
