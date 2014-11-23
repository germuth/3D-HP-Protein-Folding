/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-12 07:58:28 -0500 (Fri, 12 Jun 2009) $
 * $Revision: 11009 $
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

import org.jmol.util.JmolList;
import java.util.Arrays;

import java.util.Date;
import java.util.Hashtable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.constant.EnumVdw;
import org.jmol.modelset.Atom;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.util.ArrayUtil;
import org.jmol.util.AxisAngle4f;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.ColorEncoder;
import org.jmol.util.ColorUtil;
import org.jmol.util.Escape;
import org.jmol.util.JmolEdge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.util.P3;
import org.jmol.util.Point3fi;
import org.jmol.util.Point4f;
import org.jmol.util.Quaternion;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.Tuple3f;
import org.jmol.util.V3;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

class ScriptMathProcessor {
  /**
   * Reverse Polish Notation Engine for IF, SET, and %{...} -- Bob Hanson
   * 2/16/2007 Just a (not so simple?) RPN processor that can handle boolean,
   * int, float, String, Point3f, BitSet, Array, Hashtable, Matrix3f, Matrix4f
   * 
   * hansonr@stolaf.edu
   * 
   */

  private boolean isSyntaxCheck;
  private boolean wasSyntaxCheck;
  private boolean logMessages;
  private ScriptEvaluator eval;
  private Viewer viewer;

  private T[] oStack = new T[8];
  private SV[] xStack = new SV[8];
  private char[] ifStack = new char[8];
  private int ifPt = -1;
  private int oPt = -1;
  private int xPt = -1;
  private int parenCount;
  private int squareCount;
  private int braceCount;
  private boolean wasX;
  private int incrementX;
  private boolean isArrayItem;
  private boolean asVector;
  private boolean asBitSet;
  private int ptid = 0;
  private int ptx = Integer.MAX_VALUE;

  ScriptMathProcessor(ScriptEvaluator eval, boolean isArrayItem,
      boolean asVector, boolean asBitSet) {
    this.eval = eval;
    this.viewer = eval.viewer;
    this.logMessages = eval.logMessages;
    this.isSyntaxCheck = wasSyntaxCheck = eval.chk;
    this.isArrayItem = isArrayItem;
    this.asVector = asVector || isArrayItem;
    this.asBitSet = asBitSet;
    wasX = isArrayItem;
    if (logMessages)
      Logger.info("initialize RPN");
  }

  @SuppressWarnings("unchecked")
  SV getResult(boolean allowUnderflow)
      throws ScriptException {
    boolean isOK = true;
    while (isOK && oPt >= 0)
      isOK = operate();
    if (isOK) {
      if (asVector) {
        JmolList<SV> result = new  JmolList<SV>();
        for (int i = 0; i <= xPt; i++)
          result.addLast(SV.selectItemVar(xStack[i]));
        return SV.newVariable(T.vector, result);
      }
      if (xPt == 0) {
        SV x = xStack[0];
        if (x.tok == T.bitset || x.tok == T.varray
            || x.tok == T.string || x.tok == T.matrix3f
            || x.tok == T.matrix4f)
          x = SV.selectItemVar(x);
        if (asBitSet && x.tok == 
          T.varray)
          x = SV.newVariable(T.bitset, SV.unEscapeBitSetArray((JmolList<SV>)x.value, false));
        return x;
      }
    }
    if (!allowUnderflow && (xPt >= 0 || oPt >= 0)) {
      // iToken--;
      eval.error(ScriptEvaluator.ERROR_invalidArgument);
    }
    return null;
  }

  private void putX(SV x) {
    //System.out.println("putX skipping : " + skipping + " " + x);
    if (skipping)
      return;
    if (++xPt == xStack.length)
      xStack = (SV[]) ArrayUtil.doubleLength(xStack);
    if (logMessages) {
      Logger.info("\nputX: " + x);
    }

    xStack[xPt] = x;
    ptx = ++ptid;
  }

  private void putOp(T op) {
    if (++oPt >= oStack.length)
      oStack = (T[]) ArrayUtil.doubleLength(oStack);
    oStack[oPt] = op;
    ptid++;
  }

  private void putIf(char c) {
    if (++ifPt >= ifStack.length)
      ifStack = (char[]) ArrayUtil.doubleLength(ifStack);
    ifStack[ifPt] = c;
  }

  boolean addXVar(SV x) {
    // the standard entry point
    putX(x);
    return wasX = true;
  }

  boolean addXObj(Object x) {
    // the generic, slower, entry point
    SV v = SV.getVariable(x);
    if (v == null)
      return false;
    putX(v);
    return wasX = true;
  }

  boolean addXStr(String x) {
    putX(SV.newVariable(T.string, x));
    return wasX = true;
  }

  private boolean addXBool(boolean x) {
    putX(SV.getBoolean(x));
    return wasX = true;
  }

  private boolean addXInt(int x) {
    // no check for unary minus
    putX(new ScriptVariableInt(x));
    return wasX = true;
  }

  private boolean addXList(JmolList<?> x) {
    putX(SV.getVariableList(x));
    return wasX = true;
  }

  private boolean addXMap(Object x) {
    putX(SV.getVariableMap(x));
    return wasX = true;
  }

  private boolean addXM3(Matrix3f x) {
    putX(SV.newVariable(T.matrix3f, x));
    return wasX = true;
  }

  private boolean addXM4(Matrix4f x) {
    putX(SV.newVariable(T.matrix4f, x));
    return wasX = true;
  }

  private boolean addXFloat(float x) {
    if (Float.isNaN(x))
      return addXStr("NaN");
    putX(SV.newVariable(T.decimal, Float.valueOf(x)));
    return wasX = true;
  }

  boolean addXBs(BS bs) {
    // the standard entry point for bit sets
    putX(SV.newVariable(T.bitset, bs));
    return wasX = true;
  }

  boolean addXPt(P3 pt) {
    putX(SV.newVariable(T.point3f, pt));
    return wasX = true;
  }

  boolean addXPt4(Point4f pt) {
    putX(SV.newVariable(T.point4f, pt));
    return wasX = true;
  }

  boolean addXNum(SV x) throws ScriptException {
    // corrects for x -3 being x - 3
    // only when coming from expression() or parameterExpression()
    if (wasX)
      switch (x.tok) {
      case T.integer:
        if (x.intValue < 0) {
          addOp(T.tokenMinus);
          x = new ScriptVariableInt(-x.intValue);
        }
        break;
      case T.decimal:
        float f = ((Float) x.value).floatValue();
        if (f < 0 || f == 0 && 1 / f == Float.NEGATIVE_INFINITY) {
          addOp(T.tokenMinus);
          x = SV.newVariable(T.decimal, Float.valueOf(-f));
        }
        break;
      }
    putX(x);
    return wasX = true;
  }

  boolean addXAV(SV[] x) {
    putX(SV.getVariableAV(x));
    return wasX = true;
  }

  boolean addXAD(double[] x) {
    putX(SV.getVariableAD(x));
    return wasX = true;
  }

  boolean addXAS(String[] x) {
    putX(SV.getVariableAS(x));
    return wasX = true;
  }

  boolean addXAI(int[] x) {
    putX(SV.getVariableAI(x));
    return wasX = true;
  }

  boolean addXAII(int[][] x) {
    putX(SV.getVariableAII(x));
    return wasX = true;
  }

  boolean addXAF(float[] x) {
    putX(SV.getVariableAF(x));
    return wasX = true;
  }

  boolean addXAFF(float[][] x) {
    putX(SV.getVariableAFF(x));
    return wasX = true;
  }

  private static boolean isOpFunc(T op) {
    return (T.tokAttr(op.tok, T.mathfunc) && op != T.tokenArraySquare 
        || op.tok == T.propselector
        && T.tokAttr(op.intValue, T.mathfunc));
  }

  private boolean skipping;

  /**
   * addOp The primary driver of the Reverse Polish Notation evaluation engine.
   * 
   * This method loads operators onto the oStack[] and processes them based on a
   * precedence system. Operands are added by addX() onto the xStack[].
   * 
   * We check here for syntax issues that were not caught in the compiler. I
   * suppose that should be done at compilation stage, but this is how it is for
   * now.
   * 
   * The processing of functional arguments and (___?___:___) constructs is
   * carried out by pushing markers onto the stacks that later can be used to
   * fill argument lists or turn "skipping" on or off. Note that in the case of
   * skipped sections of ( ? : ) no attempt is made to do syntax checking.
   * [That's not entirely true -- when syntaxChecking is true, that is, when the
   * user is typing at the Jmol application console, then this code is being
   * traversed with dummy variables. That could be improved, for sure.
   * 
   * Actually, there's plenty of room for improvement here. I did this based on
   * what I learned in High School in 1974 -- 35 years ago! -- when I managed to
   * build a mini FORTRAN compiler from scratch in machine code. That was fun.
   * (This was fun, too.)
   * 
   * -- Bob Hanson, hansonr@stolaf.edu 6/9/2009
   * 
   * 
   * @param op
   * @return false if an error condition arises
   * @throws ScriptException
   */
  boolean addOp(T op) throws ScriptException {
    return addOpAllowMath(op, true);
  }

  private boolean haveSpaceBeforeSquare;
  private int equalCount;
  
  boolean addOpAllowMath(T op, boolean allowMathFunc) throws ScriptException {

    if (logMessages) {

      Logger.info("addOp entry\naddOp: " + op ); //+ " oPt=" + oPt + " ifPt = "
         // + ifPt + " skipping=" + skipping + " wasX=" + wasX);
    }

    // are we skipping due to a ( ? : ) construct?
    int tok0 = (oPt >= 0 ? oStack[oPt].tok : 0);
    skipping = (ifPt >= 0 && (ifStack[ifPt] == 'F' || ifStack[ifPt] == 'X'));
    if (skipping) {
      switch (op.tok) {
      case T.leftparen:
        putOp(op);
        return true;
      case T.colon:
        // dumpStacks("skipping -- :");
        if (tok0 != T.colon || ifStack[ifPt] == 'X')
          return true; // ignore if not a clean opstack or T already processed
        // no object here because we were skipping
        // set to flag end of this parens
        ifStack[ifPt] = 'T';
        wasX = false;
        // dumpStacks("(..False...? .skip.. :<--here.... )");
        skipping = false;
        return true;
      case T.rightparen:
        if (tok0 == T.leftparen) {
          oPt--; // clear opstack
          return true;
        }
        // dumpStacks("skipping -- )");
        if (tok0 != T.colon) {
          putOp(op);
          return true;
        }
        wasX = true;
        // and remove markers
        ifPt--;
        oPt -= 2;
        skipping = false;
        // dumpStacks("(..True...? ... : ...skip...)<--here ");
        return true;
      default:
        return true;
      }
    }

    // Do we have the appropriate context for this operator?

    T newOp = null;
    int tok;
    boolean isLeftOp = false;
    boolean isDotSelector = (op.tok == T.propselector);

    if (isDotSelector && !wasX)
      return false;

    boolean isMathFunc = (allowMathFunc && isOpFunc(op));

    // the word "plane" can also appear alone, not as a function
    if (oPt >= 1 && op.tok != T.leftparen && tok0 == T.plane)
      tok0 = oStack[--oPt].tok;

    // math functions as arguments appear without a prefixing operator
    boolean isArgument = (oPt >= 1 && tok0 == T.leftparen);

    switch (op.tok) {
    case T.spacebeforesquare:
      haveSpaceBeforeSquare = true;
      return true;
    case T.comma:
      if (!wasX)
        return false;
      break;
    case T.min:
    case T.max:
    case T.average:
    case T.sum:
    case T.sum2:
    case T.stddev:
    case T.minmaxmask:
      tok = (oPt < 0 ? T.nada : tok0);
      if (!wasX
          || !(tok == T.propselector || tok == T.bonds || tok == T.atoms))
        return false;
      oStack[oPt].intValue |= op.tok;
      return true;
    case T.leftsquare: // {....}[n][m]
      isLeftOp = true;
      if (!wasX || haveSpaceBeforeSquare) {
        squareCount++;
        op = newOp = T.tokenArraySquare;
        haveSpaceBeforeSquare = false;
      }
      break;
    case T.rightsquare:
      break;
    case T.minusMinus:
    case T.plusPlus:
      incrementX = (op.tok == T.plusPlus ? 1 : -1);
      if (ptid == ptx) {
        if (isSyntaxCheck)
          return true;
        SV x = xStack[xPt];
        xStack[xPt] = SV.newVariable(T.string, "").set(x, false);
        return x.increment(incrementX);
      }
      break;
    case T.minus:
      if (wasX)
        break;
      addXInt(0);
      op = SV.newVariable(T.unaryMinus, "-");
      break;
    case T.rightparen: // () without argument allowed only for math funcs
      if (!wasX && oPt >= 1 && tok0 == T.leftparen
          && !isOpFunc(oStack[oPt - 1]))
        return false;
      break;
    case T.opNot:
    case T.leftparen:
      isLeftOp = true;
      //$FALL-THROUGH$
    default:
      if (isMathFunc) {
        if (!isDotSelector && wasX && !isArgument)
          return false;
        newOp = op;
        isLeftOp = true;
        break;
      }
      if (wasX == isLeftOp && tok0 != T.propselector) // for now, because
        // we have .label
        // and .label()
        return false;
      break;
    }

    // do we need to operate?

    while (oPt >= 0
        && tok0 != T.colon
        && (!isLeftOp || tok0 == T.propselector
            && (op.tok == T.propselector || op.tok == T.leftsquare))
        && T.getPrecedence(tok0) >= T.getPrecedence(op.tok)) {

      if (logMessages) {
        Logger.info("\noperating, oPt=" + oPt + " isLeftOp=" + isLeftOp
            + " oStack[oPt]=" + T.nameOf(tok0) + "        prec="
            + T.getPrecedence(tok0) + " pending op=\""
            + T.nameOf(op.tok) + "\" prec=" + T.getPrecedence(op.tok));
        dumpStacks("operating");
      }
      // ) and ] must wait until matching ( or [ is found
      if (op.tok == T.rightparen && tok0 == T.leftparen) {
        // (x[2]) finalizes the selection
        if (xPt >= 0)
          xStack[xPt] = SV.selectItemVar(xStack[xPt]);
        break;
      }
      if (op.tok == T.rightsquare && tok0 == T.array) {
        break;
      }
      if (op.tok == T.rightsquare && tok0 == T.leftsquare) {
        if (isArrayItem && squareCount == 1 && equalCount == 0) {
          addXVar(SV.newScriptVariableToken(T.tokenArraySelector));
          break;
        }
        if (!doBitsetSelect())
          return false;
        break;
      }

      // if not, it's time to operate

      if (!operate())
        return false;
      tok0 = (oPt >= 0 ? oStack[oPt].tok : 0);
    }

    // now add a marker on the xStack if necessary

    if (newOp != null)
      addXVar(SV.newVariable(T.opEQ, newOp));

    // fix up counts and operand flag
    // right ) and ] are not added to the stack

    switch (op.tok) {
    case T.leftparen:
      //System.out.println("----------(----------");
      parenCount++;
      wasX = false;
      break;
    case T.opIf:
      //System.out.println("---------IF---------");
      boolean isFirst = getX().asBoolean();
      if (tok0 == T.colon)
        ifPt--;
      else
        putOp(T.tokenColon);
      putIf(isFirst ? 'T' : 'F');
      skipping = !isFirst;
      wasX = false;
      // dumpStacks("(.." + isFirst + "...?<--here ... :...skip...) ");
      return true;
    case T.colon:
      //System.out.println("----------:----------");
      if (tok0 != T.colon)
        return false;
      if (ifPt < 0)
        return false;
      ifStack[ifPt] = 'X';
      wasX = false;
      skipping = true;
      // dumpStacks("(..True...? ... :<--here ...skip...) ");
      return true;
    case T.rightparen:
      //System.out.println("----------)----------");
      wasX = true;
      if (parenCount-- <= 0)
        return false;
      if (tok0 == T.colon) {
        // remove markers
        ifPt--;
        oPt--;
        // dumpStacks("(..False...? ...skip... : ...)<--here ");
      }
      oPt--;
      if (oPt < 0)
        return true;
      if (isOpFunc(oStack[oPt]) && !evaluateFunction(0))
        return false;
      skipping = (ifPt >= 0 && ifStack[ifPt] == 'X');
      return true;
    case T.comma:
      wasX = false;
      return true;
    case T.leftsquare:
      squareCount++;
      wasX = false;
      break;
    case T.rightsquare:
      wasX = true;
      if (squareCount-- <= 0 || oPt < 0)
        return false;
      if (oStack[oPt].tok == T.array)
        return evaluateFunction(T.leftsquare);
      oPt--;
      return true;
    case T.propselector:
      wasX = (!allowMathFunc || !T.tokAttr(op.intValue, T.mathfunc));
      break;
    case T.leftbrace:
      braceCount++;
      wasX = false;
      break;
    case T.rightbrace:
      if (braceCount-- <= 0)
        return false;
      wasX = false;
      break;
    case T.opAnd:
    case T.opOr:
      if (!wasSyntaxCheck && xPt < 0)
        return false;
      if (!wasSyntaxCheck && xStack[xPt].tok != T.bitset && xStack[xPt].tok != T.varray) {
        // check to see if we need to evaluate the second operand or not
        // if not, then set this to syntax check in order to skip :)
        // Jmol 12.0.4, Jmol 12.1.2
        boolean tf = getX().asBoolean();
        addXVar(SV.getBoolean(tf));
        if (tf == (op.tok == T.opOr)) { // TRUE or.. FALSE and...
          isSyntaxCheck = true;
          op = (op.tok == T.opOr ? T.tokenOrTRUE : T.tokenAndFALSE);
        }
      }
      wasX = false;
      break;
    case T.opEQ:
      if (squareCount == 0)
        equalCount++;
      wasX = false;
      break;
    default:
      wasX = false;
    }

    // add the operator if possible

    putOp(op);

    // dumpStacks("putOp complete");
    if (op.tok == T.propselector
        && (op.intValue & ~T.minmaxmask) == T.function
        && op.intValue != T.function) {
      return evaluateFunction(0);
    }
    return true;
  }

  private boolean doBitsetSelect() {
    if (xPt < 0 || xPt == 0 && !isArrayItem) {
      return false;
    }
    SV var1 = xStack[xPt--];
    SV var = xStack[xPt];
    if (var.tok == T.varray && var1.tok == T.string 
        && var.intValue != Integer.MAX_VALUE) {
      // allow for x[1]["test"][1]["here"]
      // common in getproperty business
      // prior to 12.2/3.18, x[1]["id"] was misread as x[1][0]
      var = SV.selectItemVar2(var, Integer.MIN_VALUE);
    }
    if (var.tok == T.hash) {
      SV v = var.mapValue(SV.sValue(var1));
      xStack[xPt] = (v == null ? SV.newVariable(T.string, "") : v);
      return true;
    }
    int i = var1.asInt();
    switch (var.tok) {
    default:
      var = SV.newVariable(T.string, SV.sValue(var));
      //$FALL-THROUGH$
    case T.bitset:
    case T.varray:
    case T.string:
    case T.matrix3f:
    case T.matrix4f:
      xStack[xPt] = SV.selectItemVar2(var, i);
      break;
    }
    return true;
  }

  void dumpStacks(String message) {
    Logger.info("\n\n------------------\nRPN stacks: " + message + "\n");
    for (int i = 0; i <= xPt; i++)
      Logger.info("x[" + i + "]: " + xStack[i]);
    Logger.info("\n");
    for (int i = 0; i <= oPt; i++)
      Logger.info("o[" + i + "]: " + oStack[i] + " prec="
          + T.getPrecedence(oStack[i].tok));
    Logger.info(" ifStack = " + (new String(ifStack)).substring(0, ifPt + 1));
  }

  private SV getX() throws ScriptException {
    if (xPt < 0)
      eval.error(ScriptEvaluator.ERROR_endOfStatementUnexpected);
    SV v = SV.selectItemVar(xStack[xPt]);
    xStack[xPt--] = null;
    return v;
  }

  private boolean evaluateFunction(int tok) throws ScriptException {
    T op = oStack[oPt--];
    // for .xxx or .xxx() functions
    // we store the token in the intValue field of the propselector token
    if (tok == 0)
      tok = (op.tok == T.propselector ? op.intValue & ~T.minmaxmask
        : op.tok);

    int nParamMax = T.getMaxMathParams(tok); // note - this is NINE for
    // dot-operators
    int nParam = 0;
    int pt = xPt;
    while (pt >= 0 && xStack[pt--].value != op)
      nParam++;
    if (nParamMax > 0 && nParam > nParamMax)
      return false;
    SV[] args = new SV[nParam];
    for (int i = nParam; --i >= 0;)
      args[i] = getX();
    xPt--;
    // no script checking of functions because
    // we cannot know what variables are real
    // if this is a property selector, as in x.func(), then we
    // just exit; otherwise we add a new TRUE to xStack
    if (isSyntaxCheck)
      return (op.tok == T.propselector ? true : addXBool(true));
    switch (tok) {
    case T.abs:
    case T.acos:
    case T.cos:
    case T.now:
    case T.sin:
    case T.sqrt:
      return evaluateMath(args, tok);
    case T.add:
    case T.div:
    case T.mul:
    case T.sub:
      return evaluateList(op.intValue, args);
    case T.array:
    case T.leftsquare:
      return evaluateArray(args, tok == T.leftsquare);
    case T.axisangle:
    case T.quaternion:
      return evaluateQuaternion(args, tok);
    case T.bin:
      return evaluateBin(args);
    case T.col:
    case T.row:
      return evaluateRowCol(args, tok);
    case T.color:
      return evaluateColor(args);
    case T.compare:
      return evaluateCompare(args);
    case T.connected:
      return evaluateConnected(args);
    case T.cross:
      return evaluateCross(args);
    case T.data:
      return evaluateData(args);
    case T.angle:
    case T.distance:
    case T.dot:
    case T.measure:
      if ((tok == T.distance || tok == T.dot) 
          && op.tok == T.propselector)
        return evaluateDot(args, tok);
      return evaluateMeasure(args, op.tok);
    case T.file:
    case T.load:
      return evaluateLoad(args, tok);
    case T.find:
      return evaluateFind(args);
    case T.function:
      return evaluateUserFunction((String) op.value, args, op.intValue,
          op.tok == T.propselector);
    case T.format:
    case T.label:
      return evaluateLabel(op.intValue, args);
    case T.getproperty:
      return evaluateGetProperty(args);
    case T.helix:
      return evaluateHelix(args);
    case T.hkl:
    case T.plane:
    case T.intersection:
      return evaluatePlane(args, tok);
    case T.javascript:
    case T.script:
      return evaluateScript(args, tok);
    case T.join:
    case T.split:
    case T.trim:
      return evaluateString(op.intValue, args);
    case T.point:
      return evaluatePoint(args);
    case T.prompt:
      return evaluatePrompt(args);
    case T.random:
      return evaluateRandom(args);
    case T.replace:
      return evaluateReplace(args);
    case T.search:
    case T.smiles:
    case T.substructure:
      return evaluateSubstructure(args, tok);
    case T.cache:
      return evaluateCache(args);
    case T.sort:
    case T.count:
      return evaluateSort(args, tok);
    case T.symop:
      return evaluateSymop(args, op.tok == T.propselector);
//    case Token.volume:
  //    return evaluateVolume(args);
    case T.within:
      return evaluateWithin(args);
    case T.contact:
      return evaluateContact(args);
    case T.write:
      return evaluateWrite(args);
    }
    return false;
  }

  private boolean evaluateCache(SV[] args) {
    if (args.length > 0)
      return false;
    return addXMap(viewer.cacheList());
  }

  private boolean evaluateCompare(SV[] args) throws ScriptException {
    // compare({bitset} or [{positions}],{bitset} or [{positions}] [,"stddev"])
    // compare({bitset},{bitset}[,"SMARTS"|"SMILES"],smilesString [,"stddev"])
    // returns matrix4f for rotation/translation or stddev
    // compare({bitset},{bitset},"ISOMER")  12.1.5

    if (args.length < 2 || args.length > 5)
      return false;
    float stddev;
    String sOpt = SV.sValue(args[args.length - 1]);
    boolean isStdDev = sOpt.equalsIgnoreCase("stddev");
    boolean isIsomer = sOpt.equalsIgnoreCase("ISOMER");
    boolean isSmiles = (!isIsomer && args.length > (isStdDev ? 3 : 2));
    BS bs1 = (args[0].tok == T.bitset ? (BS) args[0].value : null);
    BS bs2 = (args[1].tok == T.bitset ? (BS) args[1].value : null);
    String smiles1 = (bs1 == null ? SV.sValue(args[0]) : "");
    String smiles2 = (bs2 == null ? SV.sValue(args[1]) : "");
    Matrix4f m = new Matrix4f();
    stddev = Float.NaN;
    JmolList<P3> ptsA, ptsB;
    if (isSmiles) {
      if (bs1 == null || bs2 == null)
        return false;
    }
    if (isIsomer) {
      if (args.length != 3)
        return false;
      if (bs1 == null && bs2 == null) 
        return addXStr(viewer.getSmilesMatcher().getRelationship(smiles1, smiles2).toUpperCase());
      String mf1 = (bs1 == null ? viewer.getSmilesMatcher()
          .getMolecularFormula(smiles1, false) : JmolMolecule.getMolecularFormula(
          viewer.getModelSet().atoms, bs1, false));
      String mf2 = (bs2 == null ? viewer.getSmilesMatcher()
          .getMolecularFormula(smiles2, false) : JmolMolecule.getMolecularFormula(
          viewer.getModelSet().atoms, bs2, false));
      if (!mf1.equals(mf2))
        return addXStr("NONE");
      if (bs1 != null)
        smiles1 = (String) eval.getSmilesMatches("", null, bs1, null, false, true);
      boolean check;
      if (bs2 == null) {
        // note: find smiles1 IN smiles2 here
        check = (viewer.getSmilesMatcher().areEqual(smiles2, smiles1) > 0);
      } else {
        check = (((BS) eval.getSmilesMatches(smiles1, null, bs2, null,
            false, true)).nextSetBit(0) >= 0);
      }
      if (!check) {
        // MF matched, but didn't match SMILES
        String s = smiles1 + smiles2;
        if (s.indexOf("/") >= 0 || s.indexOf("\\") >= 0 || s.indexOf("@") >= 0) {
          if (smiles1.indexOf("@") >= 0 && (bs2 != null || smiles2.indexOf("@") >= 0)) {
            // reverse chirality centers
            smiles1 = viewer.getSmilesMatcher().reverseChirality(smiles1);
            if (bs2 == null) {
              check = (viewer.getSmilesMatcher().areEqual(smiles1, smiles2) > 0);
            } else {
              check = (((BS) eval.getSmilesMatches(smiles1, null, bs2,
                  null, false, true)).nextSetBit(0) >= 0);
            }
            if (check)
              return addXStr("ENANTIOMERS");
          }
          // remove all stereochemistry from SMILES string
          if (bs2 == null) {
            check = (viewer.getSmilesMatcher().areEqual("/nostereo/" + smiles2, smiles1) > 0);
          } else {
            Object ret = eval.getSmilesMatches("/nostereo/" + smiles1, null, bs2, null,
                false, true);
            check = (((BS) ret).nextSetBit(0) >= 0);
          }
          if (check)
            return addXStr("DIASTERIOMERS");
        }
        // MF matches, but not enantiomers or diasteriomers
        return addXStr("CONSTITUTIONAL ISOMERS");
      }
      //identical or conformational 
      if (bs1 == null || bs2 == null)
        return addXStr("IDENTICAL");
      stddev = eval.getSmilesCorrelation(bs1, bs2, smiles1, null, null, null,
          null, false, false);
      return addXStr(stddev < 0.2f ? "IDENTICAL"
          : "IDENTICAL or CONFORMATIONAL ISOMERS (RMSD=" + stddev + ")");
    } else if (isSmiles) {
      ptsA = new  JmolList<P3>();
      ptsB = new  JmolList<P3>();
      sOpt = SV.sValue(args[2]);
      boolean isMap = sOpt.equalsIgnoreCase("MAP");
      isSmiles = (sOpt.equalsIgnoreCase("SMILES"));
      boolean isSearch = (isMap || sOpt.equalsIgnoreCase("SMARTS"));
      if (isSmiles || isSearch)
        sOpt = (args.length > 3 ? SV.sValue(args[3]) : null);
      if (sOpt == null)
        return false;
      stddev = eval.getSmilesCorrelation(bs1, bs2, sOpt, ptsA, ptsB, m, null,
          !isSmiles, isMap);
      if (isMap) {
        int nAtoms = ptsA.size();
        if (nAtoms == 0)
          return addXStr("");
        int nMatch = ptsB.size() / nAtoms;
        JmolList<int[][]> ret = new  JmolList<int[][]>();
        for (int i = 0, pt = 0; i < nMatch; i++) {
          int[][] a = ArrayUtil.newInt2(nAtoms);
          ret.addLast(a);
          for (int j = 0; j < nAtoms; j++, pt++)
            a[j] = new int[] { ((Atom)ptsA.get(j)).index, ((Atom)ptsB.get(pt)).index};
        }
        return addXList(ret);
      }
    } else {
      ptsA = eval.getPointVector(args[0], 0);
      ptsB = eval.getPointVector(args[1], 0);
      if (ptsA != null && ptsB != null)
        stddev = Measure.getTransformMatrix4(ptsA, ptsB, m, null);
    }
    return (isStdDev || Float.isNaN(stddev) ? addXFloat(stddev) : addXM4(m));
  }

//  private boolean evaluateVolume(ScriptVariable[] args) throws ScriptException {
//    ScriptVariable x1 = getX();
//    if (x1.tok != Token.bitset)
//      return false;
//    String type = (args.length == 0 ? null : ScriptVariable.sValue(args[0]));
//    return addX(viewer.getVolume((BitSet) x1.value, type));
//  }

  private boolean evaluateSort(SV[] args, int tok)
      throws ScriptException {
    if (args.length > 1)
      return false;
    if (tok == T.sort) {
      int n = (args.length == 0 ? 0 : args[0].asInt());
      return addXVar(getX().sortOrReverse(n));
    }
    SV x = getX();
    SV match = (args.length == 0 ? null : args[0]);
    if (x.tok == T.string) {
      int n = 0;
      String s = SV.sValue(x);
      if (match == null)
        return addXInt(0);
      String m = SV.sValue(match);
      for (int i = 0; i < s.length(); i++) {
        int pt = s.indexOf(m, i);
        if (pt < 0)
          break;
        n++;
        i = pt;
      }
      return addXInt(n);
    }
    JmolList<SV> counts = new  JmolList<SV>();
    SV last = null;
    SV count = null;
    JmolList<SV> xList = SV.getVariable(x.value)
        .sortOrReverse(0).getList();
    if (xList == null)
      return (match == null ? addXStr("") : addXInt(0));
    for (int i = 0, nLast = xList.size(); i <= nLast; i++) {
      SV a = (i == nLast ? null : xList.get(i));
      if (match != null && a != null && !SV.areEqual(a, match))
        continue;
      if (SV.areEqual(a, last)) {
        count.intValue++;
        continue;
      } else if (last != null) {
        JmolList<SV> y = new  JmolList<SV>();
        y.addLast(last);
        y.addLast(count);
        counts.addLast(SV.getVariableList(y));
      }
      count = new ScriptVariableInt(1);
      last = a; 
    }
    if (match == null)
      return addXVar(SV.getVariableList(counts));
    if (counts.isEmpty())
      return addXInt(0);
    return addXVar(counts.get(0).getList().get(1));

  }

  private boolean evaluateSymop(SV[] args, boolean haveBitSet)
      throws ScriptException {
    if (args.length == 0)
      return false;
    SV x1 = (haveBitSet ? getX() : null);
    if (x1 != null && x1.tok != T.bitset)
      return false;
    BS bs = (x1 != null ? (BS) x1.value : args.length > 2
        && args[1].tok == T.bitset ? (BS) args[1].value : viewer
        .getModelUndeletedAtomsBitSet(-1));
    String xyz;
    switch (args[0].tok) {
    case T.string:
      xyz = SV.sValue(args[0]);
      break;
    case T.matrix4f:
      xyz = args[0].escape();
      break;
    default:
      xyz = null;
    }
    int iOp = (xyz == null ? args[0].asInt() : 0);
    P3 pt = (args.length > 1 ? ptValue(args[1], true) : null);
    if (args.length == 2 && !Float.isNaN(pt.x))
      return addXObj(viewer.getSymmetryInfo(bs, xyz, iOp, pt, null, null,
          T.point));
    String desc = (args.length == 1 ? "" : SV
        .sValue(args[args.length - 1])).toLowerCase();
    int tok = T.draw;
    if (args.length == 1 || desc.equalsIgnoreCase("matrix")) {
      tok = T.matrix4f;
    } else if (desc.equalsIgnoreCase("array") || desc.equalsIgnoreCase("list")) {
      tok = T.list;
    } else if (desc.equalsIgnoreCase("description")) {
      tok = T.label;
    } else if (desc.equalsIgnoreCase("xyz")) {
      tok = T.info;
    } else if (desc.equalsIgnoreCase("translation")) {
      tok = T.translation;
    } else if (desc.equalsIgnoreCase("axis")) {
      tok = T.axis;
    } else if (desc.equalsIgnoreCase("plane")) {
      tok = T.plane;
    } else if (desc.equalsIgnoreCase("angle")) {
      tok = T.angle;
    } else if (desc.equalsIgnoreCase("axispoint")) {
      tok = T.point;
    } else if (desc.equalsIgnoreCase("center")) {
      tok = T.center;
    }
    return addXObj(viewer.getSymmetryInfo(bs, xyz, iOp, pt, null, desc, tok));
  }

  private boolean evaluateBin(SV[] args) throws ScriptException {
    if (args.length != 3)
      return false;
    SV x1 = getX();
    boolean isListf = (x1.tok == T.listf);
    if (!isListf && x1.tok != T.varray)
      return addXVar(x1);
    float f0 = SV.fValue(args[0]);
    float f1 = SV.fValue(args[1]);
    float df = SV.fValue(args[2]);
    float[] data;
    if (isListf) {
      data = (float[]) x1.value;
    } else {
      JmolList<SV> list = x1.getList();
      data = new float[list.size()];
      for (int i = list.size(); --i >= 0; )
        data[i] = SV.fValue(list.get(i));
    }
    int nbins = (int) Math.floor((f1 - f0) / df + 0.01f);
    int[] array = new int[nbins];
    int nPoints = data.length;
    for (int i = 0; i < nPoints; i++) {
      float v = data[i];
      int bin = (int) Math.floor((v - f0) / df);
      if (bin < 0)
        bin = 0;
      else if (bin >= nbins)
        bin = nbins - 1;
      array[bin]++;
    }
    return addXAI(array);
  }

  private boolean evaluateHelix(SV[] args) throws ScriptException {
    if (args.length < 1 || args.length > 5)
      return false;
    // helix({resno=3})
    // helix({resno=3},"point|axis|radius|angle|draw|measure|array")
    // helix(resno,"point|axis|radius|angle|draw|measure|array")
    // helix(pt1, pt2, dq, "point|axis|radius|angle|draw|measure|array|")
    // helix(pt1, pt2, dq, "draw","someID")
    // helix(pt1, pt2, dq)
    int pt = (args.length > 2 ? 3 : 1);
    String type = (pt >= args.length ? "array" : SV
        .sValue(args[pt]));
    int tok = T.getTokFromName(type);
    if (args.length > 2) {
      // helix(pt1, pt2, dq ...)
      P3 pta = ptValue(args[0], true);
      P3 ptb = ptValue(args[1], true);
      if (args[2].tok != T.point4f)
        return false;
      Quaternion dq = Quaternion.newP4((Point4f) args[2].value);
      switch (tok) {
      case T.nada:
        break;
      case T.point:
      case T.axis:
      case T.radius:
      case T.angle:
      case T.measure:
        return addXObj(Measure.computeHelicalAxis(null, tok, pta, ptb, dq));
      case T.array:
        String[] data = (String[]) Measure.computeHelicalAxis(null, T.list,
            pta, ptb, dq);
        if (data == null)
          return false;
        return addXAS(data);
      default:
        return addXObj(Measure.computeHelicalAxis(type, T.draw, pta, ptb,
            dq));
      }
    } else {
      BS bs = (args[0].value instanceof BS ? (BS) args[0].value
          : eval.compareInt(T.resno, T.opEQ, args[0].asInt()));
      switch (tok) {
      case T.point:
        return addXObj(viewer.getHelixData(bs, T.point));
      case T.axis:
        return addXObj(viewer.getHelixData(bs, T.axis));
      case T.radius:
        return addXObj(viewer.getHelixData(bs, T.radius));
      case T.angle:
        return addXFloat(((Float) viewer.getHelixData(bs, T.angle))
            .floatValue());
      case T.draw:
      case T.measure:
        return addXObj(viewer.getHelixData(bs, tok));
      case T.array:
        String[] data = (String[]) viewer.getHelixData(bs, T.list);
        if (data == null)
          return false;
        return addXAS(data);
      }
    }
    return false;
  }

  private boolean evaluateDot(SV[] args, int tok)
      throws ScriptException {
    if (args.length != 1)
      return false;
    SV x1 = getX();
    SV x2 = args[0];
    P3 pt2 = ptValue(x2, true);
    Point4f plane2 = planeValue(x2);
    if (x1.tok == T.bitset && tok != T.dot)
      return addXObj(eval.getBitsetProperty(SV.bsSelectVar(x1),
          T.distance, pt2, plane2, x1.value, null, false, x1.index, false));
    P3 pt1 = ptValue(x1, true);
    Point4f plane1 = planeValue(x1);
    if (tok == T.dot) {
      if (plane1 != null && plane2 != null)
        // q1.dot(q2) assume quaternions
        return addXFloat(plane1.x * plane2.x + plane1.y * plane2.y + plane1.z
            * plane2.z + plane1.w * plane2.w);
      // plane.dot(point) =
      if (plane1 != null)
        pt1 = P3.new3(plane1.x, plane1.y, plane1.z);
      // point.dot(plane)
      if (plane2 != null)
        pt2 = P3.new3(plane2.x, plane2.y, plane2.z);
      return addXFloat(pt1.x * pt2.x + pt1.y * pt2.y + pt1.z * pt2.z);
    }

    if (plane1 == null)
      return addXFloat(plane2 == null ? pt2.distance(pt1) : Measure.distanceToPlane(
          plane2, pt1));
    return addXFloat(Measure.distanceToPlane(plane1, pt2));
  }

  public P3 ptValue(SV x, boolean allowFloat)
      throws ScriptException {
    Object pt;
    if (isSyntaxCheck)
      return new P3();
    switch (x.tok) {
    case T.point3f:
      return (P3) x.value;
    case T.bitset:
      return (P3) eval
          .getBitsetProperty(SV.bsSelectVar(x), T.xyz, null, null,
              x.value, null, false, Integer.MAX_VALUE, false);
    case T.string:
      pt = Escape.uP(SV.sValue(x));
      if (pt instanceof P3)
        return (P3) pt;
      break;
    case T.varray:
      pt = Escape.uP("{" + SV.sValue(x) + "}");
      if (pt instanceof P3)
        return (P3) pt;
      break;
    }
    if (!allowFloat)
      return null;
    float f = SV.fValue(x);
    return P3.new3(f, f, f);
  }

  private Point4f planeValue(T x) {
    if (isSyntaxCheck)
      return new Point4f();
    switch (x.tok) {
    case T.point4f:
      return (Point4f) x.value;
    case T.varray:
    case T.string:
      Object pt = Escape.uP(SV.sValue(x));
      return (pt instanceof Point4f ? (Point4f) pt : null);
    case T.bitset:
      // ooooh, wouldn't THIS be nice!
      break;
    }
    return null;
  }

  private boolean evaluateMeasure(SV[] args, int tok)
      throws ScriptException {
    int nPoints = 0;
    switch (tok) {
    case T.measure:
      // note: min/max are always in Angstroms
      // note: order is not important (other than min/max)
      // measure({a},{b},{c},{d}, min, max, format, units)
      // measure({a},{b},{c}, min, max, format, units)
      // measure({a},{b}, min, max, format, units)
      // measure({a},{b},{c},{d}, min, max, format, units)
      // measure({a} {b} "minArray") -- returns array of minimum distance values
      JmolList<Object> points = new  JmolList<Object>();
      float[] rangeMinMax = new float[] { Float.MAX_VALUE, Float.MAX_VALUE };
      String strFormat = null;
      String units = null;
      boolean isAllConnected = false;
      boolean isNotConnected = false;
      int rPt = 0;
      boolean isNull = false;
      RadiusData rd = null;
      int nBitSets = 0;
      float vdw = Float.MAX_VALUE;
      boolean asArray = false;
      for (int i = 0; i < args.length; i++) {
        switch (args[i].tok) {
        case T.bitset:
          BS bs = (BS) args[i].value;
          if (bs.length() == 0)
            isNull = true;
          points.addLast(bs);
          nPoints++;
          nBitSets++;
          break;
        case T.point3f:
          Point3fi v = new Point3fi();
          v.setT((P3) args[i].value);
          points.addLast(v);
          nPoints++;
          break;
        case T.integer:
        case T.decimal:
          rangeMinMax[rPt++ % 2] = SV.fValue(args[i]);
          break;

        case T.string:
          String s = SV.sValue(args[i]);
          if (s.equalsIgnoreCase("vdw") || s.equalsIgnoreCase("vanderwaals"))
            vdw = (i + 1 < args.length && args[i + 1].tok == T.integer ? args[++i]
                .asInt()
                : 100) / 100f;
          else if (s.equalsIgnoreCase("notConnected"))
            isNotConnected = true;
          else if (s.equalsIgnoreCase("connected"))
            isAllConnected = true;
          else if (s.equalsIgnoreCase("minArray"))
            asArray = (nBitSets >= 1);
          else if (Parser.isOneOf(s.toLowerCase(),
              "nm;nanometers;pm;picometers;angstroms;ang;au"))
            units = s.toLowerCase();
          else
            strFormat = nPoints + ":" + s;
          break;
        default:
          return false;
        }
      }
      if (nPoints < 2 || nPoints > 4 || rPt > 2 || isNotConnected
          && isAllConnected)
        return false;
      if (isNull)
        return addXStr("");
      if (vdw != Float.MAX_VALUE && (nBitSets != 2 || nPoints != 2))
        return addXStr("");
      rd = (vdw == Float.MAX_VALUE ? new RadiusData(rangeMinMax, 0, null, null)
          : new RadiusData(null, vdw, EnumType.FACTOR, EnumVdw.AUTO));
      return addXObj((new MeasurementData(viewer, points)).set(0, rd, strFormat, units, null, isAllConnected,
          isNotConnected, null, true).getMeasurements(asArray));
    case T.angle:
      if ((nPoints = args.length) != 3 && nPoints != 4)
        return false;
      break;
    default: // distance
      if ((nPoints = args.length) != 2)
        return false;
    }
    P3[] pts = new P3[nPoints];
    for (int i = 0; i < nPoints; i++)
      pts[i] = ptValue(args[i], true);
    switch (nPoints) {
    case 2:
      return addXFloat(pts[0].distance(pts[1]));
    case 3:
      return addXFloat(Measure.computeAngleABC(pts[0], pts[1], pts[2], true));
    case 4:
      return addXFloat(Measure.computeTorsion(pts[0], pts[1], pts[2], pts[3],
          true));
    }
    return false;
  }

  private boolean evaluateUserFunction(String name, SV[] args,
                                       int tok, boolean isSelector)
      throws ScriptException {
    SV x1 = null;
    if (isSelector) {
      x1 = getX();
      if (x1.tok != T.bitset)
        return false;
    }
    wasX = false;
    JmolList<SV> params = new  JmolList<SV>();
    for (int i = 0; i < args.length; i++) {
      params.addLast(args[i]);
    }
    if (isSelector) {
      return addXObj(eval.getBitsetProperty(SV.bsSelectVar(x1), tok,
          null, null, x1.value, new Object[] { name, params }, false, x1.index,
          false));
    }
    SV var = eval.runFunctionRet(null, name, params, null, true, true, false);
    return (var == null ? false : addXVar(var));
  }

  private boolean evaluateFind(SV[] args) throws ScriptException {
    if (args.length == 0)
      return false;

    // {*}.find("MF")
    // {*}.find("SEQENCE")
    // {*}.find("SMARTS", "CCCC")
    // "CCCC".find("SMARTS", "CC")
    // "CCCC".find("SMILES", "MF")
    // {2.1}.find("CCCC",{1.1}) // find pattern "CCCC" in {2.1} with conformation given by {1.1}

    SV x1 = getX();
    String sFind = SV.sValue(args[0]);
    String flags = (args.length > 1 && args[1].tok != T.on
        && args[1].tok != T.off ? SV.sValue(args[1]) : "");
    boolean isSequence = sFind.equalsIgnoreCase("SEQUENCE");
    boolean isSmiles = sFind.equalsIgnoreCase("SMILES");
    boolean isSearch = sFind.equalsIgnoreCase("SMARTS");
    boolean isMF = sFind.equalsIgnoreCase("MF");
    if (isSmiles || isSearch || x1.tok == T.bitset) {
      int iPt = (isSmiles || isSearch ? 2 : 1);
      BS bs2 = (iPt < args.length && args[iPt].tok == T.bitset ? (BS) args[iPt++].value
          : null);
      boolean isAll = (args[args.length - 1].tok == T.on);
      Object ret = null;
      switch (x1.tok) {
      case T.string:
        String smiles = SV.sValue(x1);
        if (bs2 != null)
          return false;
        if (flags.equalsIgnoreCase("mf")) {
          ret = viewer.getSmilesMatcher()
              .getMolecularFormula(smiles, isSearch);
          if (ret == null)
            eval.evalError(viewer.getSmilesMatcher().getLastException(), null);
        } else {
          ret = eval.getSmilesMatches(flags, smiles, null, null, isSearch, !isAll);
        }
        break;
      case T.bitset:
        if (isMF)
          return addXStr(JmolMolecule.getMolecularFormula(
              viewer.getModelSet().atoms, (BS) x1.value, false));
        if (isSequence)
          return addXStr(viewer.getSmiles(-1, -1, (BS) x1.value, true, isAll, isAll, false));
        if (isSmiles || isSearch)
          sFind = flags;
        BS bsMatch3D = bs2;
        ret = eval.getSmilesMatches(sFind, null, (BS) x1.value, bsMatch3D, !isSmiles,
            !isAll);
        break;
      }
      if (ret == null)
        eval.error(ScriptEvaluator.ERROR_invalidArgument); 
      return addXObj(ret);
    }
    boolean isReverse = (flags.indexOf("v") >= 0);
    boolean isCaseInsensitive = (flags.indexOf("i") >= 0);
    boolean asMatch = (flags.indexOf("m") >= 0);
    boolean isList = (x1.tok == T.varray);
    boolean isPattern = (args.length == 2);
    if (isList || isPattern) {
      Pattern pattern = null;
      try {
        pattern = Pattern.compile(sFind,
            isCaseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
      } catch (Exception e) {
        eval.evalError(e.toString(), null);
      }
      String[] list = SV.listValue(x1);
      if (Logger.debugging)
        Logger.debug("finding " + sFind);
      BS bs = new BS();
      int ipt = 0;
      int n = 0;
      Matcher matcher = null;
      JmolList<String> v = (asMatch ? new  JmolList<String>() : null);
      for (int i = 0; i < list.length; i++) {
        String what = list[i];
        matcher = pattern.matcher(what);
        boolean isMatch = matcher.find();
        if (asMatch && isMatch || !asMatch && isMatch == !isReverse) {
          n++;
          ipt = i;
          bs.set(i);
          if (asMatch)
            v.addLast(isReverse ? what.substring(0, matcher.start())
                + what.substring(matcher.end()) : matcher.group());
        }
      }
      if (!isList) {
        return (asMatch ? addXStr(v.size() == 1 ? (String) v.get(0) : "")
            : isReverse ? addXBool(n == 1) : asMatch ? addXStr(n == 0 ? "" : matcher
                .group()) : addXInt(n == 0 ? 0 : matcher.start() + 1));
      }
      if (n == 1)
        return addXStr(asMatch ? (String) v.get(0) : list[ipt]);
      String[] listNew = new String[n];
      if (n > 0)
        for (int i = list.length; --i >= 0;)
          if (bs.get(i)) {
            --n;
            listNew[n] = (asMatch ? (String) v.get(n) : list[i]);
          }
      return addXAS(listNew);
    }
    return addXInt(SV.sValue(x1).indexOf(sFind) + 1);
  }

  private boolean evaluateGetProperty(SV[] args) {
    int pt = 0;
    String propertyName = (args.length > pt ? SV.sValue(args[pt++])
        .toLowerCase() : "");
    if (propertyName.startsWith("$")) {
      // TODO
    }
    Object propertyValue;
    if (propertyName.equalsIgnoreCase("fileContents") && args.length > 2) {
      String s = SV.sValue(args[1]);
      for (int i = 2; i < args.length; i++)
        s += "|" + SV.sValue(args[i]);
      propertyValue = s;
      pt = args.length;
    } else {
      propertyValue = (args.length > pt && args[pt].tok == T.bitset ? (Object) SV
          .bsSelectVar(args[pt++])
          : args.length > pt && args[pt].tok == T.string
              && viewer.checkPropertyParameter(propertyName) ? args[pt++].value
              : (Object) "");
    }
    Object property = viewer.getProperty(null, propertyName, propertyValue);
    if (pt < args.length)
      property = viewer.extractProperty(property, args, pt);
    return addXObj(SV.isVariableType(property) ? property : Escape
        .toReadable(propertyName, property));
  }

  private boolean evaluatePlane(SV[] args, int tok)
      throws ScriptException {
    if (tok == T.hkl && args.length != 3 
        || tok == T.intersection && args.length != 2 && args.length != 3 
        || args.length == 0 || args.length > 4)
      return false;
    P3 pt1, pt2, pt3;
    Point4f plane;
    V3 norm, vTemp;

    switch (args.length) {
    case 1:
      if (args[0].tok == T.bitset) {
        BS bs = SV.getBitSet(args[0], false);
        if (bs.cardinality() == 3) {
          JmolList<P3> pts = viewer.getAtomPointVector(bs);
          V3 vNorm = new V3();
          V3 vAB = new V3();
          V3 vAC = new V3();
          plane = new Point4f();
          Measure.getPlaneThroughPoints(pts.get(0), pts.get(1), pts.get(2), vNorm , vAB, vAC, plane);
          return addXPt4(plane);
        }
      }
      Object pt = Escape.uP(SV.sValue(args[0]));
      if (pt instanceof Point4f)
        return addXPt4((Point4f)pt);
      return addXStr("" + pt);
    case 2:
      if (tok == T.intersection) {
        // intersection(plane, plane)
        // intersection(point, plane)
        if (args[1].tok != T.point4f)
          return false;
        pt3 = new P3();
        norm = new V3();
        vTemp = new V3();

        plane = (Point4f) args[1].value;
        if (args[0].tok == T.point4f) {
          JmolList<Object> list = Measure.getIntersectionPP((Point4f) args[0].value,
              plane);
          if (list == null)
            return addXStr("");
          return addXList(list);
        }
        pt2 = ptValue(args[0], false);
        if (pt2 == null)
          return addXStr("");
        return addXPt(Measure.getIntersection(pt2, null, plane, pt3, norm, vTemp));
      }
      //$FALL-THROUGH$
    case 3:
    case 4:
      switch (tok) {
      case T.hkl:
        // hkl(i,j,k)
        return addXPt4(eval.getHklPlane(P3.new3(
            SV.fValue(args[0]), SV.fValue(args[1]),
            SV.fValue(args[2]))));
      case T.intersection:
        pt1 = ptValue(args[0], false);
        pt2 = ptValue(args[1], false);
        if (pt1 == null || pt2 == null)
          return addXStr("");
        V3 vLine = V3.newV(pt2);
        vLine.normalize();
        if (args[2].tok == T.point4f) {
          // intersection(ptLine, vLine, plane)
          pt3 = new P3();
          norm = new V3();
          vTemp = new V3();
          pt1 = Measure.getIntersection(pt1, vLine, (Point4f) args[2].value, pt3, norm, vTemp);
          if (pt1 == null)
            return addXStr("");
          return addXPt(pt1);
        }
        pt3 = ptValue(args[2], false);
        if (pt3 == null)
          return addXStr("");
        // interesection(ptLine, vLine, pt2); // IE intersection of plane perp to line through pt2
        V3 v = new V3();
        Measure.projectOntoAxis(pt3, pt1, vLine, v);
        return addXPt(pt3);
      }
      switch (args[0].tok) {
      case T.integer:
      case T.decimal:
        if (args.length == 3) {
          // plane(r theta phi)
          float r = SV.fValue(args[0]); 
          float theta = SV.fValue(args[1]);  // longitude, azimuthal, in xy plane
          float phi = SV.fValue(args[2]);    // 90 - latitude, polar, from z
          // rotate {0 0 r} about y axis need to stay in the x-z plane
          norm = V3.new3(0, 0, 1);
          pt2 = P3.new3(0, 1, 0);
          Quaternion q = Quaternion.newVA(pt2, phi);
          q.getMatrix().transform(norm);
          // rotate that vector around z
          pt2.set(0, 0, 1);
          q = Quaternion.newVA(pt2, theta);
          q.getMatrix().transform(norm);
          pt2.setT(norm);
          pt2.scale(r);
          plane = new Point4f();
          Measure.getPlaneThroughPoint(pt2, norm, plane);
          return addXPt4(plane);          
        }
        break;
      case T.bitset:
      case T.point3f:
        pt1 = ptValue(args[0], false);
        pt2 = ptValue(args[1], false);
        if (pt2 == null)
          return false;
        pt3 = (args.length > 2
            && (args[2].tok == T.bitset || args[2].tok == T.point3f) ? ptValue(
            args[2], false)
            : null);
        norm = V3.newV(pt2);
        if (pt3 == null) {
          plane = new Point4f();
          if (args.length == 2 || !args[2].asBoolean()) {
            // plane(<point1>,<point2>) or 
            // plane(<point1>,<point2>,false)
            pt3 = P3.newP(pt1);
            pt3.add(pt2);
            pt3.scale(0.5f);
            norm.sub(pt1);
            norm.normalize();
          } else {
            // plane(<point1>,<vLine>,true)
            pt3 = pt1;
          }
          Measure.getPlaneThroughPoint(pt3, norm, plane);
          return addXPt4(plane);
        }
        // plane(<point1>,<point2>,<point3>)
        // plane(<point1>,<point2>,<point3>,<pointref>)
        V3 vAB = new V3();
        V3 vAC = new V3();
        float nd = Measure.getDirectedNormalThroughPoints(pt1, pt2, pt3,
            (args.length == 4 ? ptValue(args[3], true) : null), norm, vAB, vAC);
        return addXPt4(Point4f.new4(norm.x, norm.y, norm.z, nd));
      }
    }
    if (args.length != 4)
      return false;
    float x = SV.fValue(args[0]);
    float y = SV.fValue(args[1]);
    float z = SV.fValue(args[2]);
    float w = SV.fValue(args[3]);
    return addXPt4(Point4f.new4(x, y, z, w));
  }

  private boolean evaluatePoint(SV[] args) {
    if (args.length != 1 && args.length != 3 && args.length != 4)
      return false;
    switch (args.length) {
    case 1:
      if (args[0].tok == T.decimal || args[0].tok == T.integer)
        return addXInt(args[0].asInt());
      String s = SV.sValue(args[0]);
      if (args[0].tok == T.varray)
        s = "{" + s + "}";
      Object pt = Escape.uP(s);
      if (pt instanceof P3)
        return addXPt((P3) pt);
      return addXStr("" + pt);
    case 3:
      return addXPt(P3.new3(args[0].asFloat(), args[1].asFloat(), args[2].asFloat()));
    case 4:
      return addXPt4(Point4f.new4(args[0].asFloat(), args[1].asFloat(), args[2].asFloat(), args[3].asFloat()));
    }
    return false;
  }

  private boolean evaluatePrompt(SV[] args) {
    //x = prompt("testing")
    //x = prompt("testing","defaultInput")
    //x = prompt("testing","yes|no|cancel", true)
    //x = prompt("testing",["button1", "button2", "button3"])

    if (args.length != 1 && args.length != 2 && args.length != 3)
      return false;
    String label = SV.sValue(args[0]);
    String[] buttonArray = (args.length > 1 && args[1].tok == T.varray ?
        SV.listValue(args[1]) : null);
    boolean asButtons = (buttonArray != null || args.length == 1 || args.length == 3 && args[2].asBoolean());
    String input = (buttonArray != null ? null : args.length >= 2 ? SV.sValue(args[1]) : "OK");
    String s = viewer.prompt(label, input, buttonArray, asButtons);
    return (asButtons && buttonArray != null ? addXInt(Integer.parseInt(s) + 1) : addXStr(s));
  }

  private boolean evaluateReplace(SV[] args) throws ScriptException {
    if (args.length != 2)
      return false;
    SV x = getX();
    String sFind = SV.sValue(args[0]);
    String sReplace = SV.sValue(args[1]);
    String s = (x.tok == T.varray ? null : SV.sValue(x));
    if (s != null)
      return addXStr(TextFormat.simpleReplace(s, sFind, sReplace));
    String[] list = SV.listValue(x);
    for (int i = list.length; --i >= 0;)
      list[i] = TextFormat.simpleReplace(list[i], sFind, sReplace);
    return addXAS(list);
  }

  private boolean evaluateString(int tok, SV[] args)
      throws ScriptException {
    if (args.length > 1)
      return false;
    SV x = getX();
    String s = (tok == T.split && x.tok == T.bitset
        || tok == T.trim && x.tok == T.varray ? null : SV
        .sValue(x));
    String sArg = (args.length == 1 ? SV.sValue(args[0])
        : tok == T.trim ? "" : "\n");
    switch (tok) {
    case T.split:
      if (x.tok == T.bitset) {
        BS bsSelected = SV.bsSelectVar(x);
        sArg = "\n";
        int modelCount = viewer.getModelCount();
        s = "";
        for (int i = 0; i < modelCount; i++) {
          s += (i == 0 ? "" : "\n");
          BS bs = viewer.getModelUndeletedAtomsBitSet(i);
          bs.and(bsSelected);
          s += Escape.e(bs);
        }
      }
      return addXAS(TextFormat.splitChars(s, sArg));
    case T.join:
      if (s.length() > 0 && s.charAt(s.length() - 1) == '\n')
        s = s.substring(0, s.length() - 1);
      return addXStr(TextFormat.simpleReplace(s, "\n", sArg));
    case T.trim:
      if (s != null)
        return addXStr(TextFormat.trim(s, sArg));      
      String[] list = SV.listValue(x);
      for (int i = list.length; --i >= 0;)
        list[i] = TextFormat.trim(list[i], sArg);
      return addXAS(list);
    }
    return addXStr("");
  }

  private boolean evaluateList(int tok, SV[] args)
      throws ScriptException {
    if (args.length != 1
        && !(tok == T.add && (args.length == 0 || args.length == 2)))
      return false;
    SV x1 = getX();
    SV x2;
    int len;
    String[] sList1 = null, sList2 = null, sList3 = null;

    if (args.length == 2) {
      // [xxxx].add("\t", [...])
      int itab = (args[0].tok == T.string ? 0 : 1);
      String tab = SV.sValue(args[itab]);
      sList1 = (x1.tok == T.varray ? SV.listValue(x1)
          : TextFormat.split(SV.sValue(x1), '\n'));
      x2 = args[1 - itab];
      sList2 = (x2.tok == T.varray ? SV.listValue(x2)
          : TextFormat.split(SV.sValue(x2), '\n'));
      sList3 = new String[len = Math.max(sList1.length, sList2.length)];
      for (int i = 0; i < len; i++)
        sList3[i] = (i >= sList1.length ? "" : sList1[i]) + tab
            + (i >= sList2.length ? "" : sList2[i]);
      return addXAS(sList3);
    }
    x2 = (args.length == 0 ? SV.newVariable(T.all, "all") : args[0]);
    boolean isAll = (x2.tok == T.all);
    if (x1.tok != T.varray && x1.tok != T.string) {
      wasX = false;
      addOp(T.tokenLeftParen);
      addXVar(x1);
      switch (tok) {
      case T.add:
        addOp(T.tokenPlus);
        break;
      case T.sub:
        addOp(T.tokenMinus);
        break;
      case T.mul:
        addOp(T.tokenTimes);
        break;
      case T.div:
        addOp(T.tokenDivide);
        break;
      }
      addXVar(x2);
      return addOp(T.tokenRightParen);
    }
    boolean isScalar = (x2.tok != T.varray && SV.sValue(x2)
        .indexOf("\n") < 0);

    float[] list1 = null;
    float[] list2 = null;
    JmolList<SV> alist1 = x1.getList();
    JmolList<SV> alist2 = x2.getList();

    if (x1.tok == T.varray) {
      len = alist1.size();
    } else {
      sList1 = (TextFormat.splitChars((String) x1.value, "\n"));
      list1 = new float[len = sList1.length];
      Parser.parseFloatArrayData(sList1, list1);
    }

    if (isAll) {
      float sum = 0f;
      if (x1.tok == T.varray) {
        for (int i = len; --i >= 0;)
          sum += SV.fValue(alist1.get(i));
      } else {
        for (int i = len; --i >= 0;)
          sum += list1[i];
      }
      return addXFloat(sum);
    }

    SV scalar = null;

    if (isScalar) {
      scalar = x2;
    } else if (x2.tok == T.varray) {
      len = Math.min(len, alist2.size());
    } else {
      sList2 = TextFormat.splitChars((String) x2.value, "\n");
      list2 = new float[sList2.length];
      Parser.parseFloatArrayData(sList2, list2);
      len = Math.min(list1.length, list2.length);
    }
    
    T token = null;
    switch (tok) {
    case T.add:
      token = T.tokenPlus;
      break;
    case T.sub:
      token = T.tokenMinus;
      break;
    case T.mul:
      token = T.tokenTimes;
      break;
    case T.div:
      token = T.tokenDivide;
      break;
    }

    SV[] olist = new SV[len];
    
    for (int i = 0; i < len; i++) {
      if (x1.tok == T.varray)
        addXVar(alist1.get(i));
      else if (Float.isNaN(list1[i]))
        addXObj(SV.unescapePointOrBitsetAsVariable(sList1[i]));
      else
        addXFloat(list1[i]);

      if (isScalar)
        addXVar(scalar);
      else if (x2.tok == T.varray)
        addXVar(alist2.get(i));
      else if (Float.isNaN(list2[i]))
        addXObj(SV.unescapePointOrBitsetAsVariable(sList2[i]));
      else
        addXFloat(list2[i]);
      
      if (!addOp(token) || !operate())
        return false;
      olist[i] = xStack[xPt--];
    }
    return addXAV(olist);
  }

  private boolean evaluateRowCol(SV[] args, int tok)
      throws ScriptException {
    if (args.length != 1)
      return false;
    int n = args[0].asInt() - 1;
    SV x1 = getX();
    float[] f;
    switch (x1.tok) {
    case T.matrix3f:
      if (n < 0 || n > 2)
        return false;
      Matrix3f m = (Matrix3f) x1.value;
      switch (tok) {
      case T.row:
        f = new float[3];
        m.getRow(n, f);
        return addXAF(f);
      case T.col:
      default:
        f = new float[3];
        m.getColumn(n, f);
        return addXAF(f);
      }
    case T.matrix4f:
      if (n < 0 || n > 2)
        return false;
      Matrix4f m4 = (Matrix4f) x1.value;
      switch (tok) {
      case T.row:
        f = new float[4];
        m4.getRow(n, f);
        return addXAF(f);
      case T.col:
      default:
        f = new float[4];
        m4.getColumn(n, f);
        return addXAF(f);
      }
    }
    return false;

  }

  private boolean evaluateArray(SV[] args, boolean allowMatrix) {
    int len = args.length;
    if (allowMatrix && (len == 4 || len == 3)) {
      boolean isMatrix = true;
      for (int i = 0; i < len && isMatrix; i++)
        isMatrix = (args[i].tok == T.varray && args[i].getList().size() == len);
      if (isMatrix) {
        float[] m = new float[len * len];
        int pt = 0;
        for (int i = 0; i < len && isMatrix; i++) {
          JmolList<SV> list = args[i].getList();
          for (int j = 0; j < len; j++) {
            float x = SV.fValue(list.get(j));
            if (Float.isNaN(x)) {
              isMatrix = false;
              break;
            }
            m[pt++] = x;
          }
        }
        if (isMatrix) {
          if (len == 3)
            return addXM3(Matrix3f.newA(m));
          return addXM4(Matrix4f.newA(m));
        }
      }
    }
    SV[] a = new SV[args.length];
    for (int i = a.length; --i >= 0;)
      a[i] = SV.newScriptVariableToken(args[i]);
    return addXAV(a);
  }

  private boolean evaluateMath(SV[] args, int tok) {
    if (tok == T.now) {
      if (args.length == 1 && args[0].tok == T.string)
        return addXStr((new Date()) + "\t" + SV.sValue(args[0]));
      return addXInt(((int) System.currentTimeMillis() & 0x7FFFFFFF)
          - (args.length == 0 ? 0 : args[0].asInt()));
    }
    if (args.length != 1)
      return false;
    if (tok == T.abs) {
      if (args[0].tok == T.integer)
        return addXInt(Math.abs(args[0].asInt()));
      return addXFloat(Math.abs(args[0].asFloat()));
    }
    double x = SV.fValue(args[0]);
    switch (tok) {
    case T.acos:
      return addXFloat((float) (Math.acos(x) * 180 / Math.PI));
    case T.cos:
      return addXFloat((float) Math.cos(x * Math.PI / 180));
    case T.sin:
      return addXFloat((float) Math.sin(x * Math.PI / 180));
    case T.sqrt:
      return addXFloat((float) Math.sqrt(x));
    }
    return false;
  }

  private boolean evaluateQuaternion(SV[] args, int tok)
      throws ScriptException {
    P3 pt0 = null;
    // quaternion([quaternion array]) // mean
    // quaternion([quaternion array1], [quaternion array2], "relative") //
    // difference array
    // quaternion(matrix)
    // quaternion({atom1}) // quaternion (1st if array)
    // quaternion({atomSet}, nMax) // nMax quaternions, by group; 0 for all
    // quaternion({atom1}, {atom2}) // difference 
    // quaternion({atomSet1}, {atomset2}, nMax) // difference array, by group; 0 for all
    // quaternion(vector, theta)
    // quaternion(q0, q1, q2, q3)
    // quaternion("{x, y, z, w"})
    // quaternion(center, X, XY)
    // quaternion(mcol1, mcol2)
    // quaternion(q, "id", center) // draw code
    // axisangle(vector, theta)
    // axisangle(x, y, z, theta)
    // axisangle("{x, y, z, theta"})
    int nArgs = args.length;
    int nMax = Integer.MAX_VALUE;
    boolean isRelative = false;
    if (tok == T.quaternion) {
      if (nArgs > 1 && args[nArgs - 1].tok == T.string
          && ((String) args[nArgs - 1].value).equalsIgnoreCase("relative")) {
        nArgs--;
        isRelative = true;
      }
      if (nArgs > 1 && args[nArgs - 1].tok == T.integer 
          && args[0].tok == T.bitset) {
        nMax = args[nArgs - 1].asInt();
        if (nMax <= 0)
          nMax = Integer.MAX_VALUE - 1;
        nArgs--;
      }
    }

    switch (nArgs) {
    case 0:
    case 1:
    case 4:
      break;
    case 2:
      if (tok == T.quaternion) {
        if (args[0].tok == T.varray && args[1].tok == T.varray)
          break;
        if (args[0].tok == T.bitset
            && (args[1].tok == T.integer || args[1].tok == T.bitset))
          break;
      }
      if ((pt0 = ptValue(args[0], false)) == null || tok != T.quaternion
          && args[1].tok == T.point3f)
        return false;
      break;
    case 3:
      if (tok != T.quaternion)
        return false;
      if (args[0].tok == T.point4f) {
        if (args[2].tok != T.point3f && args[2].tok != T.bitset)
          return false;
        break;
      }
      for (int i = 0; i < 3; i++)
        if (args[i].tok != T.point3f && args[i].tok != T.bitset)
          return false;
      break;
    default:
      return false;
    }
    Quaternion q = null;
    Quaternion[] qs = null;
    Point4f p4 = null;
    switch (nArgs) {
    case 0:
      return addXPt4(Quaternion.newQ(viewer.getRotationQuaternion()).toPoint4f());
    case 1:
    default:
      if (tok == T.quaternion && args[0].tok == T.varray) {
        Quaternion[] data1 = getQuaternionArray(args[0].getList(), T.list);
        Object mean = Quaternion.sphereMean(data1, null, 0.0001f);
        q = (mean instanceof Quaternion ? (Quaternion) mean : null);
        break;
      } else if (tok == T.quaternion && args[0].tok == T.bitset) {
        qs = viewer.getAtomGroupQuaternions((BS) args[0].value, nMax);
      } else if (args[0].tok == T.matrix3f) {
        q = Quaternion.newM((Matrix3f) args[0].value);
      } else if (args[0].tok == T.point4f) {
        p4 = (Point4f) args[0].value;
      } else {
        Object v = Escape.uP(SV.sValue(args[0]));
        if (!(v instanceof Point4f))
          return false;
        p4 = (Point4f) v;
      }
      if (tok == T.axisangle)
        q = Quaternion.newVA(P3.new3(p4.x, p4.y, p4.z), p4.w);
      break;
    case 2:
      if (tok == T.quaternion) {
        if (args[0].tok == T.varray && args[1].tok == T.varray) {
          Quaternion[] data1 = getQuaternionArray(args[0].getList(), T.list);
          Quaternion[] data2 = getQuaternionArray(args[1].getList(), T.list);
          qs = Quaternion.div(data2, data1, nMax, isRelative);
          break;
        }
        if (args[0].tok == T.bitset && args[1].tok == T.bitset) {
          Quaternion[] data1 = viewer.getAtomGroupQuaternions(
              (BS) args[0].value, Integer.MAX_VALUE);
          Quaternion[] data2 = viewer.getAtomGroupQuaternions(
              (BS) args[1].value, Integer.MAX_VALUE);
          qs = Quaternion.div(data2, data1, nMax, isRelative);
          break;
        }
      }
      P3 pt1 = ptValue(args[1], false);
      p4 = planeValue(args[0]);
      if (pt1 != null)
        q = Quaternion.getQuaternionFrame(P3.new3(0, 0, 0), pt0, pt1);
      else
        q = Quaternion.newVA(pt0, SV.fValue(args[1]));
      break;
    case 3:
      if (args[0].tok == T.point4f) {
        P3 pt = (args[2].tok == T.point3f ? (P3) args[2].value
            : viewer.getAtomSetCenter((BS) args[2].value));
        return addXStr((Quaternion.newP4((Point4f) args[0].value)).draw("q",
            SV.sValue(args[1]), pt, 1f));
      }
      P3[] pts = new P3[3];
      for (int i = 0; i < 3; i++)
        pts[i] = (args[i].tok == T.point3f ? (P3) args[i].value
            : viewer.getAtomSetCenter((BS) args[i].value));
      q = Quaternion.getQuaternionFrame(pts[0], pts[1], pts[2]);
      break;
    case 4:
      if (tok == T.quaternion)
        p4 = Point4f.new4(SV.fValue(args[1]), SV
            .fValue(args[2]), SV.fValue(args[3]), SV
            .fValue(args[0]));
      else
        q = Quaternion.newVA(P3.new3(SV.fValue(args[0]),
            SV.fValue(args[1]), SV.fValue(args[2])),
            SV.fValue(args[3]));
      break;
    }
    if (qs != null) {
      if (nMax != Integer.MAX_VALUE) {
        JmolList<Point4f> list = new  JmolList<Point4f>();
        for (int i = 0; i < qs.length; i++)
          list.addLast(qs[i].toPoint4f());
        return addXList(list);
      }
      q = (qs.length > 0 ? qs[0] : null);
    }
    return addXPt4((q == null ? Quaternion.newP4(p4) : q).toPoint4f());
  }

  private boolean evaluateRandom(SV[] args) {
    if (args.length > 2)
      return false;
    float lower = (args.length < 2 ? 0 : SV.fValue(args[0]));
    float range = (args.length == 0 ? 1 : SV
        .fValue(args[args.length - 1]));
    range -= lower;
    return addXFloat((float) (Math.random() * range) + lower);
  }

  private boolean evaluateCross(SV[] args) {
    if (args.length != 2)
      return false;
    SV x1 = args[0];
    SV x2 = args[1];
    if (x1.tok != T.point3f || x2.tok != T.point3f)
      return false;
    V3 a = V3.newV((P3) x1.value);
    V3 b = V3.newV((P3) x2.value);
    a.cross(a, b);
    return addXPt(P3.newP(a));
  }

  private boolean evaluateLoad(SV[] args, int tok) {
    if (args.length > 2 || args.length < 1)
      return false;
    String file = SV.sValue(args[0]);
    int nBytesMax = (args.length == 2 ? args[1].asInt()
        : Integer.MAX_VALUE);
    return addXStr(tok == T.load ? viewer.getFileAsString4(file, nBytesMax,
        false, false) : viewer.getFilePath(file, false));
  }

  private boolean evaluateWrite(SV[] args) throws ScriptException {
    if (args.length == 0)
      return false;
    return addXStr(eval.write(args));
  }

  private boolean evaluateScript(SV[] args, int tok)
      throws ScriptException {
    if (tok == T.javascript && args.length != 1 || args.length == 0
        || args.length > 2)
      return false;
    String s = SV.sValue(args[0]);
    SB sb = new SB();
    switch (tok) {
    case T.script:
      String appID = (args.length == 2 ? SV.sValue(args[1]) : ".");
      // options include * > . or an appletID with or without "jmolApplet"
      if (!appID.equals("."))
        sb.append(viewer.jsEval(appID + "\1" + s));
      if (appID.equals(".") || appID.equals("*"))
        eval.runScriptBuffer(s, sb);
      break;
    case T.javascript:
      sb.append(viewer.jsEval(s));
      break;
    }
    s = sb.toString();
    float f;
    return (Float.isNaN(f = Parser.parseFloatStrict(s)) ? addXStr(s) : s
        .indexOf(".") >= 0 ? addXFloat(f) : addXInt(Parser.parseInt(s)));
  }

  private boolean evaluateData(SV[] args) {

    // x = data("somedataname") # the data
    // x = data("data2d_xxxx") # 2D data (x,y paired values)
    // x = data("data2d_xxxx", iSelected) # selected row of 2D data, with <=0
    // meaning "relative to the last row"
    // x = data("property_x", "property_y") # array addition of two property
    // sets
    // x = data({atomno < 10},"xyz") # (or "pdb" or "mol") coordinate data in
    // xyz, pdb, or mol format
    // x = data(someData,ptrFieldOrColumn,nBytes,firstLine) # extraction of a
    // column of data based on a field (nBytes = 0) or column range (nBytes >
    // 0)
    if (args.length != 1 && args.length != 2 && args.length != 4)
      return false;
    String selected = SV.sValue(args[0]);
    String type = (args.length == 2 ? SV.sValue(args[1]) : "");

    if (args.length == 4) {
      int iField = args[1].asInt();
      int nBytes = args[2].asInt();
      int firstLine = args[3].asInt();
      float[] f = Parser.extractData(selected, iField, nBytes, firstLine);
      return addXStr(Escape.escapeFloatA(f, false));
    }

    if (selected.indexOf("data2d_") == 0) {
      // tab, newline separated data
      float[][] f1 = viewer.getDataFloat2D(selected);
      if (f1 == null)
        return addXStr("");
      if (args.length == 2 && args[1].tok == T.integer) {
        int pt = args[1].intValue;
        if (pt < 0)
          pt += f1.length;
        if (pt >= 0 && pt < f1.length)
          return addXStr(Escape.escapeFloatA(f1[pt], false));
        return addXStr("");
      }
      return addXStr(Escape.escapeFloatAA(f1, false));
    }

    // parallel addition of float property data sets

    if (selected.indexOf("property_") == 0) {
      float[] f1 = viewer.getDataFloat(selected);
      if (f1 == null)
        return addXStr("");
      float[] f2 = (type.indexOf("property_") == 0 ? viewer.getDataFloat(type)
          : null);
      if (f2 != null) {
        f1 = ArrayUtil.arrayCopyF(f1, -1);
        for (int i = Math.min(f1.length, f2.length); --i >= 0;)
          f1[i] += f2[i];
      }
      return addXStr(Escape.escapeFloatA(f1, false));
    }

    // some other data type -- just return it

    if (args.length == 1) {
      Object[] data = viewer.getData(selected);
      return addXStr(data == null ? "" : "" + data[1]);
    }
    // {selected atoms} XYZ, MOL, PDB file format
    return addXStr(viewer.getData(selected, type));
  }

  private boolean evaluateLabel(int intValue, SV[] args)
      throws ScriptException {
    // NOT {xxx}.label
    // {xxx}.label("....")
    // {xxx}.yyy.format("...")
    // (value).format("...")
    // format("....",a,b,c...)

    SV x1 = (args.length < 2 ? getX() : null);
    String format = (args.length == 0 ? "%U" : SV.sValue(args[0]));
    boolean asArray = T.tokAttr(intValue, T.minmaxmask);
    if (x1 == null)
      return addXStr(SV.sprintfArray(args));
    BS bs = SV.getBitSet(x1, true);
    if (bs == null)
      return addXObj(SV.sprintf(TextFormat.formatCheck(format), x1));
    return addXObj(eval.getBitsetIdent(bs, format,
          x1.value, true, x1.index, asArray));
  }

  private boolean evaluateWithin(SV[] args) throws ScriptException {
    if (args.length < 1 || args.length > 5)
      return false;
    int i = args.length;
    float distance = 0;
    Object withinSpec = args[0].value;
    String withinStr = "" + withinSpec;
    int tok = args[0].tok;
    if (tok == T.string)
      tok = T.getTokFromName(withinStr);
    boolean isVdw = (tok == T.vanderwaals);
    if (isVdw) {
      distance = 100;
      withinSpec = null;
    }
    BS bs;
    boolean isWithinModelSet = false;
    boolean isWithinGroup = false;
    boolean isDistance = (isVdw || tok == T.decimal || tok == T.integer);
    RadiusData rd = null;
    switch (tok) {
    case T.branch:
      if (i != 3 || !(args[1].value instanceof BS)
          || !(args[2].value instanceof BS))
        return false;
      return addXBs(viewer.getBranchBitSet(
          ((BS) args[2].value).nextSetBit(0), ((BS) args[1].value)
              .nextSetBit(0)));
    case T.smiles:
    case T.substructure:  // same as "SMILES"
    case T.search:
      // within("smiles", "...", {bitset})
      // within("smiles", "...", {bitset})
      BS bsSelected = null;
      boolean isOK = true;
      switch (i) {
      case 2:
        break;
      case 3:
        isOK = (args[2].tok == T.bitset);
        if (isOK)
          bsSelected = (BS) args[2].value;
        break;
      default:
        isOK = false;
      }
      if (!isOK)
        eval.error(ScriptEvaluator.ERROR_invalidArgument);
      return addXObj(eval.getSmilesMatches(SV
          .sValue(args[1]), null, bsSelected, null, tok == T.search, asBitSet));
    }
    if (withinSpec instanceof String) {
      if (tok == T.nada) {
        tok = T.spec_seqcode;
        if (i > 2)
          return false;
        i = 2;
      }
    } else if (isDistance) {
      if (!isVdw)
        distance = SV.fValue(args[0]); 
      if (i < 2)
        return false;
      switch (tok = args[1].tok) {
      case T.on:
      case T.off:
        isWithinModelSet = args[1].asBoolean();
        i = 0;
        break;
      case T.string:
        String s = SV.sValue(args[1]);
        if (s.startsWith("$"))
          return addXBs(eval.getAtomsNearSurface(distance, s.substring(1)));
        isWithinGroup = (s.equalsIgnoreCase("group"));
        isVdw = (s.equalsIgnoreCase("vanderwaals"));
        if (isVdw) {
          withinSpec = null;
          tok = T.vanderwaals;
        } else {
          tok = T.group;
        }
        break;
      }
    } else {
      return false;
    }
    P3 pt = null;
    Point4f plane = null;
    switch (i) {
    case 1:
      // within (sheet)
      // within (helix)
      // within (boundbox)
      switch (tok) {
      case T.helix:
      case T.sheet:
      case T.boundbox:
        return addXBs(viewer.getAtomBits(tok, null));
      case T.basepair:
        return addXBs(viewer.getAtomBits(tok, ""));
      case T.spec_seqcode:
        return addXBs(viewer.getAtomBits(T.sequence,
            withinStr));
      }
      return false;
    case 2:
      // within (atomName, "XX,YY,ZZZ")
      switch (tok) {
      case T.spec_seqcode:
        tok = T.sequence;
        break;
      case T.atomname:
      case T.atomtype:
      case T.basepair:
      case T.sequence:
        return addXBs(viewer.getAtomBits(tok, SV
            .sValue(args[args.length - 1])));
      }
      break;
    case 3:
      switch (tok) {
      case T.on:
      case T.off:
      case T.group:
      case T.vanderwaals:
      case T.plane:
      case T.hkl:
      case T.coord:
        break;
      case T.sequence:
        // within ("sequence", "CII", *.ca)
        withinStr = SV.sValue(args[2]);
        break;
      default:
        return false;
      }
      // within (distance, group, {atom collection})
      // within (distance, true|false, {atom collection})
      // within (distance, plane|hkl, [plane definition] )
      // within (distance, coord, [point or atom center] )
      break;
    }
    i = args.length - 1;
    if (args[i].value instanceof Point4f) {
      plane = (Point4f) args[i].value;
    } else if (args[i].value instanceof P3) {
      pt = (P3) args[i].value;
      if (SV.sValue(args[1]).equalsIgnoreCase("hkl"))
        plane = eval.getHklPlane(pt);
    }
    if (i > 0 && plane == null && pt == null
        && !(args[i].value instanceof BS))
      return false;
    if (plane != null)
      return addXBs(viewer.getAtomsNearPlane(distance, plane));
    if (pt != null)
      return addXBs(viewer.getAtomsNearPt(distance, pt));
    bs = (args[i].tok == T.bitset ? SV.bsSelectVar(args[i]) : null);
    if (tok == T.sequence)
      return addXBs(viewer.getSequenceBits(withinStr, bs));
    if (bs == null)
      bs = new BS();
    if (!isDistance)
      return addXBs(viewer.getAtomBits(tok, bs));
    if (isWithinGroup)
      return addXBs(viewer.getGroupsWithin((int) distance, bs));
    if (isVdw)
      rd = new RadiusData(null, 
          (distance > 10 ? distance / 100 : distance), 
          (distance > 10 ? EnumType.FACTOR : EnumType.OFFSET), EnumVdw.AUTO);
    return addXBs(viewer.getAtomsWithinRadius(distance, bs, isWithinModelSet, rd));
  }

  private boolean evaluateContact(SV[] args) {
    if (args.length < 1 || args.length > 3)
      return false;
    int i = 0;
    float distance = 100;
    int tok = args[0].tok;
    switch (tok) {
    case T.decimal:
    case T.integer:
      distance = SV.fValue(args[i++]);
      break;
    case T.bitset:
      break;
    default:
      return false;
    }
    if (i == args.length || !(args[i].value instanceof BS))
      return false;
    BS bsA = BSUtil.copy(SV.bsSelectVar(args[i++]));
    if (isSyntaxCheck)
      return addXBs(new BS());
    BS bsB = (i < args.length ? BSUtil.copy(SV
        .bsSelectVar(args[i])) : null);
    RadiusData rd = new RadiusData(null,
        (distance > 10 ? distance / 100 : distance),
        (distance > 10 ? EnumType.FACTOR : EnumType.OFFSET), EnumVdw.AUTO);
    bsB = eval.setContactBitSets(bsA, bsB, true, Float.NaN, rd, false);
    bsB.or(bsA);
    return addXBs(bsB);
  }

  private boolean evaluateColor(SV[] args) {
    // color("hsl", {r g b})         # r g b in 0 to 255 scale 
    // color("rwb")                  # "" for most recently used scheme for coloring by property
    // color("rwb", min, max)        # min/max default to most recent property mapping 
    // color("rwb", min, max, value) # returns color
    // color("$isosurfaceId")        # info for a given isosurface
    // color("$isosurfaceId", value) # color for a given mapped isosurface value
    
    String colorScheme = (args.length > 0 ? SV.sValue(args[0])
        : "");
    if (colorScheme.equalsIgnoreCase("hsl") && args.length == 2) {
      P3 pt = P3.newP(SV.ptValue(args[1]));
      float[] hsl = new float[3];
      ColorEncoder.RGBtoHSL(pt.x, pt.y, pt.z, hsl);
      pt.set(hsl[0]*360, hsl[1]*100, hsl[2]*100);
      return addXPt(pt);
    }
    boolean isIsosurface = colorScheme.startsWith("$");
    ColorEncoder ce = (isIsosurface ? null : viewer.getColorEncoder(colorScheme));
    if (!isIsosurface && ce == null)
      return addXStr("");
    float lo = (args.length > 1 ? SV.fValue(args[1])
        : Float.MAX_VALUE);
    float hi = (args.length > 2 ? SV.fValue(args[2])
        : Float.MAX_VALUE);
    float value = (args.length > 3 ? SV.fValue(args[3])
        : Float.MAX_VALUE);
    boolean getValue = (value != Float.MAX_VALUE || lo != Float.MAX_VALUE
        && hi == Float.MAX_VALUE);
    boolean haveRange = (hi != Float.MAX_VALUE);
    if (!haveRange && colorScheme.length() == 0) {
      value = lo;
      float[] range = viewer.getCurrentColorRange();
      lo = range[0];
      hi = range[1];
    }
    if (isIsosurface) {
      // isosurface color scheme      
      String id = colorScheme.substring(1);
      Object[] data = new Object[] { id, null};
      if (!viewer.getShapePropertyData(JC.SHAPE_ISOSURFACE, "colorEncoder", data))
        return addXStr("");
      ce = (ColorEncoder) data[1];
    } else {
      ce.setRange(lo, hi, lo > hi);
    }
    Map<String, Object> key = ce.getColorKey();
    if (getValue)
      return addXPt(ColorUtil.colorPointFromInt2(ce
          .getArgb(hi == Float.MAX_VALUE ? lo : value)));
    return addXVar(SV.getVariableMap(key));
  }

  private boolean evaluateConnected(SV[] args) {
    /*
     * Two options here:
     * 
     * connected(1, 3, "single", {carbon})
     * 
     * connected(1, 3, "partial 3.1", {carbon})
     * 
     * means "atoms connected to carbon by from 1 to 3 single bonds"
     * 
     * connected(1.0, 1.5, "single", {carbon}, {oxygen})
     * 
     * means "single bonds from 1.0 to 1.5 Angstroms between carbon and oxygen"
     * 
     * the first returns an atom bitset; the second returns a bond bitset.
     */

    if (args.length > 5)
      return false;
    float min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
    float fmin = 0, fmax = Float.MAX_VALUE;

    int order = JmolEdge.BOND_ORDER_ANY;
    BS atoms1 = null;
    BS atoms2 = null;
    boolean haveDecimal = false;
    boolean isBonds = false;
    for (int i = 0; i < args.length; i++) {
      SV var = args[i];
      switch (var.tok) {
      case T.bitset:
        isBonds = (var.value instanceof BondSet);
        if (isBonds && atoms1 != null)
          return false;
        if (atoms1 == null)
          atoms1 = SV.bsSelectVar(var);
        else if (atoms2 == null)
          atoms2 = SV.bsSelectVar(var);
        else
          return false;
        break;
      case T.string:
        String type = SV.sValue(var);
        if (type.equalsIgnoreCase("hbond"))
          order = JmolEdge.BOND_HYDROGEN_MASK;
        else
          order = ScriptEvaluator.getBondOrderFromString(type);
        if (order == JmolEdge.BOND_ORDER_NULL)
          return false;
        break;
      case T.decimal:
        haveDecimal = true;
        //$FALL-THROUGH$
      default:
        int n = var.asInt();
        float f = var.asFloat();
        if (max != Integer.MAX_VALUE)
          return false;

        if (min == Integer.MIN_VALUE) {
          min = Math.max(n, 0);
          fmin = f;
        } else {
          max = n;
          fmax = f;
        }
      }
    }
    if (min == Integer.MIN_VALUE) {
      min = 1;
      max = 100;
      fmin = JC.DEFAULT_MIN_CONNECT_DISTANCE;
      fmax = JC.DEFAULT_MAX_CONNECT_DISTANCE;
    } else if (max == Integer.MAX_VALUE) {
      max = min;
      fmax = fmin;
      fmin = JC.DEFAULT_MIN_CONNECT_DISTANCE;
    }
    if (atoms1 == null)
      atoms1 = viewer.getModelUndeletedAtomsBitSet(-1);
    if (haveDecimal && atoms2 == null)
      atoms2 = atoms1;
    if (atoms2 != null) {
      BS bsBonds = new BS();
      viewer
          .makeConnections(fmin, fmax, order,
              T.identify, atoms1, atoms2, bsBonds,
              isBonds, false, 0);
      return addXVar(SV.newVariable(T.bitset, new BondSet(bsBonds, viewer
          .getAtomIndices(viewer.getAtomBits(T.bonds, bsBonds)))));
    }
    return addXBs(viewer.getAtomsConnected(min, max, order, atoms1));
  }

  private boolean evaluateSubstructure(SV[] args, int tok)
      throws ScriptException {
    // select substucture(....) legacy - was same as smiles(), now search()
    // select smiles(...)
    // select search(...)  now same as substructure
    if (args.length == 0)
      return false;
    BS bs = new BS();
    String pattern = SV.sValue(args[0]);
    if (pattern.length() > 0)
      try {
        BS bsSelected = (args.length == 2 && args[1].tok == T.bitset ? SV
            .bsSelectVar(args[1])
            : null);
        bs = viewer.getSmilesMatcher().getSubstructureSet(pattern,
            viewer.getModelSet().atoms, viewer.getAtomCount(), bsSelected,
            tok != T.smiles && tok != T.substructure, false);
      } catch (Exception e) {
        eval.evalError(e.toString(), null);
      }
    return addXBs(bs);
  }

  @SuppressWarnings("unchecked")
  private boolean operate() throws ScriptException {

    T op = oStack[oPt--];
    P3 pt;
    Point4f pt4;
    Matrix3f m;
    String s;
    float f;

    if (logMessages) {
      dumpStacks("operate: " + op);
    }

    // check for a[3][2] 
    if (isArrayItem && squareCount == 0 && equalCount == 1 && oPt < 0
        && (op.tok == T.opEQ)) {
      return true;
    }

    SV x2 = getX();
    if (x2 == T.tokenArraySelector)
      return false;

    // unary:

    if (x2.tok == T.varray || x2.tok == T.matrix3f
        || x2.tok == T.matrix4f)
      x2 = SV.selectItemVar(x2);

    if (op.tok == T.minusMinus || op.tok == T.plusPlus) {
      if (!isSyntaxCheck && !x2.increment(incrementX))
        return false;
      wasX = true;
      putX(x2); // reverse getX()
      return true;
    }
    if (op.tok == T.opNot) {
      if (isSyntaxCheck)
        return addXBool(true);
      switch (x2.tok) {
      case T.point4f: // quaternion
        return addXPt4((Quaternion.newP4((Point4f) x2.value)).inv().toPoint4f());
      case T.matrix3f:
        m = Matrix3f.newM((Matrix3f) x2.value);
        m.invert();
        return addXM3(m);
      case T.matrix4f:
        Matrix4f m4 = Matrix4f.newM((Matrix4f) x2.value);
        m4.invert();
        return addXM4(m4);
      case T.bitset:
        return addXBs(BSUtil.copyInvert(SV.bsSelectVar(x2),
            (x2.value instanceof BondSet ? viewer.getBondCount() : viewer
                .getAtomCount())));
      default:
        return addXBool(!x2.asBoolean());
      }
    }
    int iv = op.intValue & ~T.minmaxmask;
    if (op.tok == T.propselector) {
      switch (iv) {
      case T.identifier:
        return getAllProperties(x2, (String) op.value);
      case T.length:
      case T.count:
      case T.size:
        if (iv == T.length && x2.value instanceof BondSet)
          break;
        return addXInt(SV.sizeOf(x2));
      case T.type:
        return addXStr(typeOf(x2));
      case T.keys:
        if (x2.tok != T.hash)
          return addXStr("");
        Object[] keys = ((Map<String, SV>) x2.value).keySet()
            .toArray();
        Arrays.sort(keys);
        String[] ret = new String[keys.length];
        for (int i = 0; i < keys.length; i++)
          ret[i] = (String) keys[i];
        return addXAS(ret);
      case T.lines:
        switch (x2.tok) {
        case T.matrix3f:
        case T.matrix4f:
          s = SV.sValue(x2);
          s = TextFormat.simpleReplace(s.substring(1, s.length() - 1), "],[",
              "]\n[");
          break;
        case T.string:
          s = (String) x2.value;
          break;
        default:
          s = SV.sValue(x2);
        }
        s = TextFormat.simpleReplace(s, "\n\r", "\n").replace('\r', '\n');
        return addXAS(TextFormat.split(s, '\n'));
      case T.color:
        switch (x2.tok) {
        case T.string:
        case T.varray:
          s = SV.sValue(x2);
          pt = new P3();
          return addXPt(ColorUtil.colorPointFromString(s, pt));
        case T.integer:
        case T.decimal:
          return addXPt(viewer.getColorPointForPropertyValue(SV
              .fValue(x2)));
        case T.point3f:
          return addXStr(Escape.escapeColor(ColorUtil
              .colorPtToInt((P3) x2.value)));
        default:
          // handle bitset later
        }
        break;
      case T.boundbox:
        return (isSyntaxCheck ? addXStr("x") : getBoundBox(x2));
      }
      if (isSyntaxCheck)
        return addXStr(SV.sValue(x2));
      if (x2.tok == T.string) {
        Object v = SV
            .unescapePointOrBitsetAsVariable(SV.sValue(x2));
        if (!(v instanceof SV))
          return false;
        x2 = (SV) v;
      }
      if (op.tok == x2.tok)
        x2 = getX();
      return getPointOrBitsetOperation(op, x2);
    }

    // binary:
    SV x1 = getX();
    if (isSyntaxCheck) {
      if (op == T.tokenAndFALSE || op == T.tokenOrTRUE)
        isSyntaxCheck = false;
      return addXVar(SV.newScriptVariableToken(x1));
    }
    switch (op.tok) {
    case T.opAND:
    case T.opAnd:
      switch (x1.tok) {
      case T.bitset:
        BS bs = SV.bsSelectVar(x1);
        switch (x2.tok) {
        case T.bitset:
          bs = BSUtil.copy(bs);
          bs.and(SV.bsSelectVar(x2));
          return addXBs(bs);
        case T.integer:
          int x = x2.asInt();
          return (addXBool(x < 0 ? false : bs.get(x)));
        }
        break;
      }
      return addXBool(x1.asBoolean() && x2.asBoolean());
    case T.opOr:
      switch (x1.tok) {
      case T.bitset:
        BS bs = BSUtil.copy(SV.bsSelectVar(x1));
        switch (x2.tok) {
        case T.bitset:
          bs.or(SV.bsSelectVar(x2));
          return addXBs(bs);
        case T.integer:
          int x = x2.asInt();
          if (x < 0)
            break;
          bs.set(x);
          return addXBs(bs);
        case T.varray:
          JmolList<SV> sv = (JmolList<SV>) x2.value;
          for (int i = sv.size(); --i >= 0;) {
            int b = sv.get(i).asInt();
            if (b >= 0)
              bs.set(b); 
          }
          return addXBs(bs);
        }
        break;
      case T.varray:
        return addXVar(SV.concatList(x1, x2, false));
      }
      return addXBool(x1.asBoolean() || x2.asBoolean());
    case T.opXor:
      if (x1.tok == T.bitset && x2.tok == T.bitset) {
        BS bs = BSUtil.copy(SV.bsSelectVar(x1));
        bs.xor(SV.bsSelectVar(x2));
        return addXBs(bs);
      }
      boolean a = x1.asBoolean();
      boolean b = x2.asBoolean();
      return addXBool(a && !b || b && !a);
    case T.opToggle:
      if (x1.tok != T.bitset || x2.tok != T.bitset)
        return false;
      return addXBs(BSUtil.toggleInPlace(BSUtil.copy(SV
          .bsSelectVar(x1)), SV.bsSelectVar(x2)));
    case T.opLE:
      return addXBool(x1.asFloat() <= x2.asFloat());
    case T.opGE:
      return addXBool(x1.asFloat() >= x2.asFloat());
    case T.opGT:
      return addXBool(x1.asFloat() > x2.asFloat());
    case T.opLT:
      return addXBool(x1.asFloat() < x2.asFloat());
    case T.opEQ:
      return addXBool(SV.areEqual(x1, x2));
    case T.opNE:
      return addXBool(!SV.areEqual(x1, x2));
    case T.plus:
      switch (x1.tok) {
      default:
        return addXFloat(x1.asFloat() + x2.asFloat());
      case T.varray:
        return addXVar(SV.concatList(x1, x2, true));
      case T.integer:
        switch (x2.tok) {
        case T.string:
          if ((s = (SV.sValue(x2)).trim()).indexOf(".") < 0
              && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
            return addXInt(x1.intValue + x2.asInt());
          break;
        case T.decimal:
          return addXFloat(x1.intValue + x2.asFloat());
        }
        return addXInt(x1.intValue + x2.asInt());
      case T.string:
        return addXVar(SV.newVariable(T.string,
            SV.sValue(x1) + SV.sValue(x2)));
      case T.point4f:
        Quaternion q1 = Quaternion.newP4((Point4f) x1.value);
        switch (x2.tok) {
        default:
          return addXPt4(q1.add(x2.asFloat()).toPoint4f());
        case T.point4f:
          return addXPt4(q1.mulQ(Quaternion.newP4((Point4f) x2.value))
              .toPoint4f());
        }
      case T.point3f:
        pt = P3.newP((P3) x1.value);
        switch (x2.tok) {
        case T.point3f:
          pt.add((P3) x2.value);
          return addXPt(pt);
        case T.point4f:
          // extract {xyz}
          pt4 = (Point4f) x2.value;
          pt.add(P3.new3(pt4.x, pt4.y, pt4.z));
          return addXPt(pt);
        default:
          f = x2.asFloat();
          return addXPt(P3.new3(pt.x + f, pt.y + f, pt.z + f));
        }
      case T.matrix3f:
        switch (x2.tok) {
        default:
          return addXFloat(x1.asFloat() + x2.asFloat());
        case T.matrix3f:
          m = Matrix3f.newM((Matrix3f) x1.value);
          m.add((Matrix3f) x2.value);
          return addXM3(m);
        case T.point3f:
          return addXM4(getMatrix4f((Matrix3f) x1.value, (P3) x2.value));
        }
      }
    case T.minus:
      if (x1.tok == T.integer) {
        if (x2.tok == T.string) {
          if ((s = (SV.sValue(x2)).trim()).indexOf(".") < 0
              && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
            return addXInt(x1.intValue - x2.asInt());
        } else if (x2.tok != T.decimal)
          return addXInt(x1.intValue - x2.asInt());
      }
      if (x1.tok == T.string && x2.tok == T.integer) {
        if ((s = (SV.sValue(x1)).trim()).indexOf(".") < 0
            && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
          return addXInt(x1.asInt() - x2.intValue);
      }
      switch (x1.tok) {
      default:
        return addXFloat(x1.asFloat() - x2.asFloat());
      case T.hash:
        Map<String, SV> ht = new Hashtable<String, SV>(
            (Map<String, SV>) x1.value);
        ht.remove(SV.sValue(x2));
        return addXVar(SV.getVariableMap(ht));
      case T.matrix3f:
        switch (x2.tok) {
        default:
          return addXFloat(x1.asFloat() - x2.asFloat());
        case T.matrix3f:
          m = Matrix3f.newM((Matrix3f) x1.value);
          m.sub((Matrix3f) x2.value);
          return addXM3(m);
        }
      case T.matrix4f:
        switch (x2.tok) {
        default:
          return addXFloat(x1.asFloat() - x2.asFloat());
        case T.matrix4f:
          Matrix4f m4 = Matrix4f.newM((Matrix4f) x1.value);
          m4.sub((Matrix4f) x2.value);
          return addXM4(m4);
        }
      case T.point3f:
        pt = P3.newP((P3) x1.value);
        switch (x2.tok) {
        default:
          f = x2.asFloat();
          return addXPt(P3.new3(pt.x - f, pt.y - f, pt.z - f));
        case T.point3f:
          pt.sub((P3) x2.value);
          return addXPt(pt);
        case T.point4f:
          // extract {xyz}
          pt4 = (Point4f) x2.value;
          pt.sub(P3.new3(pt4.x, pt4.y, pt4.z));
          return addXPt(pt);
        }
      case T.point4f:
        Quaternion q1 = Quaternion.newP4((Point4f) x1.value);
        switch (x2.tok) {
        default:
          return addXPt4(q1.add(-x2.asFloat()).toPoint4f());
        case T.point4f:
          Quaternion q2 = Quaternion.newP4((Point4f) x2.value);
          return addXPt4(q2.mulQ(q1.inv()).toPoint4f());
        }
      }
    case T.unaryMinus:
      switch (x2.tok) {
      default:
        return addXFloat(-x2.asFloat());
      case T.integer:
        return addXInt(-x2.asInt());
      case T.point3f:
        pt = P3.newP((P3) x2.value);
        pt.scale(-1f);
        return addXPt(pt);
      case T.point4f:
        pt4 = Point4f.newPt((Point4f) x2.value);
        pt4.scale(-1f);
        return addXPt4(pt4);
      case T.matrix3f:
        m = Matrix3f.newM((Matrix3f) x2.value);
        m.transpose();
        return addXM3(m);
      case T.matrix4f:
        Matrix4f m4 = Matrix4f.newM((Matrix4f) x2.value);
        m4.transpose();
        return addXM4(m4);
      case T.bitset:
        return addXBs(BSUtil.copyInvert(SV.bsSelectVar(x2),
            (x2.value instanceof BondSet ? viewer.getBondCount() : viewer
                .getAtomCount())));
      }
    case T.times:
      if (x1.tok == T.integer && x2.tok != T.decimal)
        return addXInt(x1.intValue * x2.asInt());
      pt = (x1.tok == T.matrix3f ? ptValue(x2, false)
          : x2.tok == T.matrix3f ? ptValue(x1, false) : null);
      pt4 = (x1.tok == T.matrix4f ? planeValue(x2)
          : x2.tok == T.matrix4f ? planeValue(x1) : null);
      // checking here to make sure arrays remain arrays and
      // points remain points with matrix operations.
      // we check x2, because x1 could be many things.
      switch (x2.tok) {
      case T.matrix3f:
        if (pt != null) {
          // pt * m
          Matrix3f m3b = Matrix3f.newM((Matrix3f) x2.value);
          m3b.transpose();
          m3b.transform(pt);
          if (x1.tok == T.varray)
            return addXVar(SV.getVariableAF(new float[] { pt.x, pt.y,
                pt.z }));
          return addXPt(pt);
        }
        if (pt4 != null) {
          // q * m --> q
          return addXPt4((Quaternion.newP4(pt4).mulQ(Quaternion
              .newM((Matrix3f) x2.value))).toPoint4f());
        }
        break;
      case T.matrix4f:
        // pt4 * m4
        // [a b c d] * m4
        if (pt4 != null) {
          Matrix4f m4b = Matrix4f.newM((Matrix4f) x2.value);
          m4b.transpose();
          m4b.transform4(pt4);
          if (x1.tok == T.varray)
            return addXVar(SV.getVariableAF(new float[] { pt4.x,
                pt4.y, pt4.z, pt4.w }));
          return addXPt4(pt4);
        }
        break;
      }
      switch (x1.tok) {
      default:
        return addXFloat(x1.asFloat() * x2.asFloat());
      case T.matrix3f:
        Matrix3f m3 = (Matrix3f) x1.value;
        if (pt != null) {
          m3.transform(pt);
          if (x2.tok == T.varray)
            return addXVar(SV.getVariableAF(new float[] { pt.x, pt.y,
                pt.z }));
          return addXPt(pt);
        }
        switch (x2.tok) {
        case T.matrix3f:
          m = Matrix3f.newM((Matrix3f) x2.value);
          m.mul2(m3, m);
          return addXM3(m);
        case T.point4f:
          // m * q
          return addXM3(Quaternion.newM(m3).mulQ(
              Quaternion.newP4((Point4f) x2.value)).getMatrix());
        default:
          f = x2.asFloat();
          AxisAngle4f aa = new AxisAngle4f();
          aa.setM(m3);
          aa.angle *= f;
          Matrix3f m2 = new Matrix3f();
          m2.setAA(aa);
          return addXM3(m2);
        }
      case T.matrix4f:
        Matrix4f m4 = (Matrix4f) x1.value;
        if (pt != null) {
          m4.transform(pt);
          if (x2.tok == T.varray)
            return addXVar(SV.getVariableAF(new float[] { pt.x, pt.y,
                pt.z }));
          return addXPt(pt);
        }
        if (pt4 != null) {
          m4.transform4(pt4);
          if (x2.tok == T.varray)
            return addXVar(SV.getVariableAF(new float[] { pt4.x,
                pt4.y, pt4.z, pt4.w }));
          return addXPt4(pt4);
        }
        switch (x2.tok) {
        case T.matrix4f:
          Matrix4f m4b = Matrix4f.newM((Matrix4f) x2.value);
          m4b.mul2(m4, m4b);
          return addXM4(m4b);
        default:
          return addXStr("NaN");
        }
      case T.point3f:
        pt = P3.newP((P3) x1.value);
        switch (x2.tok) {
        case T.point3f:
          P3 pt2 = ((P3) x2.value);
          return addXFloat(pt.x * pt2.x + pt.y * pt2.y + pt.z * pt2.z);
        default:
          f = x2.asFloat();
          return addXPt(P3.new3(pt.x * f, pt.y * f, pt.z * f));
        }
      case T.point4f:
        switch (x2.tok) {
        case T.point4f:
          // quaternion multiplication
          // note that Point4f is {x,y,z,w} so we use that for
          // quaternion notation as well here.
          return addXPt4(Quaternion.newP4((Point4f) x1.value)
              .mulQ(Quaternion.newP4((Point4f) x2.value)).toPoint4f());
        }
        return addXPt4(Quaternion.newP4((Point4f) x1.value).mul(
            x2.asFloat()).toPoint4f());
      }
    case T.percent:
      // more than just modulus

      // float % n round to n digits; n = 0 does "nice" rounding
      // String % -n trim to width n; left justify
      // String % n trim to width n; right justify
      // Point3f % n ah... sets to multiple of unit cell!
      // bitset % n
      // Point3f * Point3f does dot product
      // Point3f / Point3f divides by magnitude
      // float * Point3f gets m agnitude
      // Point4f % n returns q0, q1, q2, q3, or theta
      // Point4f % Point4f
      s = null;
      int n = x2.asInt();
      switch (x1.tok) {
      case T.on:
      case T.off:
      case T.integer:
      default:
        if (n == 0)
          return addXInt(0);
        return addXInt(x1.asInt() % n);
      case T.decimal:
        f = x1.asFloat();
        // neg is scientific notation
        if (n == 0)
          return addXInt(Math.round(f));
        s = TextFormat.formatDecimal(f, n);
        return addXStr(s);
      case T.string:
        s = (String) x1.value;
        if (n == 0)
          return addXStr(TextFormat.trim(s, "\n\t "));
        if (n == 9999)
          return addXStr(s.toUpperCase());
        if (n == -9999)
          return addXStr(s.toLowerCase());
        if (n > 0)
          return addXStr(TextFormat.formatS(s, n, n, false, false));
        return addXStr(TextFormat.formatS(s, n, n - 1, true, false));
      case T.varray:
        String[] list = SV.listValue(x1);
        for (int i = 0; i < list.length; i++) {
          if (n == 0)
            list[i] = list[i].trim();
          else if (n > 0)
            list[i] = TextFormat.formatS(list[i], n, n, true, false);
          else
            list[i] = TextFormat.formatS(s, -n, n, false, false);
        }
        return addXAS(list);
      case T.point3f:
        pt = P3.newP((P3) x1.value);
        viewer.toUnitCell(pt, P3.new3(n, n, n));
        return addXPt(pt);
      case T.point4f:
        pt4 = (Point4f) x1.value;
        if (x2.tok == T.point3f)
          return addXPt((Quaternion.newP4(pt4)).transformPt((P3) x2.value));
        if (x2.tok == T.point4f) {
          Point4f v4 = Point4f.newPt((Point4f) x2.value);
          (Quaternion.newP4(pt4)).getThetaDirected(v4);
          return addXPt4(v4);
        }
        switch (n) {
        // q%0 w
        // q%1 x
        // q%2 y
        // q%3 z
        // q%4 normal
        // q%-1 vector(1)
        // q%-2 theta
        // q%-3 Matrix column 0
        // q%-4 Matrix column 1
        // q%-5 Matrix column 2
        // q%-6 AxisAngle format
        // q%-9 Matrix format
        case 0:
          return addXFloat(pt4.w);
        case 1:
          return addXFloat(pt4.x);
        case 2:
          return addXFloat(pt4.y);
        case 3:
          return addXFloat(pt4.z);
        case 4:
          return addXPt(P3.newP((Quaternion.newP4(pt4)).getNormal()));
        case -1:
          return addXPt(P3.newP(Quaternion.newP4(pt4).getVector(-1)));
        case -2:
          return addXFloat((Quaternion.newP4(pt4)).getTheta());
        case -3:
          return addXPt(P3.newP((Quaternion.newP4(pt4)).getVector(0)));
        case -4:
          return addXPt(P3.newP((Quaternion.newP4(pt4)).getVector(1)));
        case -5:
          return addXPt(P3.newP((Quaternion.newP4(pt4)).getVector(2)));
        case -6:
          AxisAngle4f ax = (Quaternion.newP4(pt4)).toAxisAngle4f();
          return addXPt4(Point4f.new4(ax.x, ax.y, ax.z,
              (float) (ax.angle * 180 / Math.PI)));
        case -9:
          return addXM3((Quaternion.newP4(pt4)).getMatrix());
        default:
          return addXPt4(pt4);
        }
      case T.matrix4f:
        Matrix4f m4 = (Matrix4f) x1.value;
        switch (n) {
        case 1:
          Matrix3f m3 = new Matrix3f();
          m4.getRotationScale(m3);
          return addXM3(m3);
        case 2:
          V3 v3 = new V3();
          m4.get(v3);
          return addXPt(P3.newP(v3));
        default:
          return false;
        }
      case T.bitset:
        return addXBs(SV.bsSelectRange(x1, n));
      }
    case T.divide:
      if (x1.tok == T.integer && x2.tok == T.integer
          && x2.intValue != 0)
        return addXInt(x1.intValue / x2.intValue);
      float f2 = x2.asFloat();
      switch (x1.tok) {
      default:
        float f1 = x1.asFloat();
        return addXFloat(f1 / f2);
      case T.point3f:
        pt = P3.newP((P3) x1.value);
        if (f2 == 0)
          return addXPt(P3.new3(Float.NaN, Float.NaN, Float.NaN));
        return addXPt(P3.new3(pt.x / f2, pt.y / f2, pt.z / f2));
      case T.point4f:
        if (x2.tok == T.point4f)
          return addXPt4(Quaternion.newP4((Point4f) x1.value).div(
              Quaternion.newP4((Point4f) x2.value)).toPoint4f());
        if (f2 == 0)
          return addXPt4(Point4f.new4(Float.NaN, Float.NaN, Float.NaN,
              Float.NaN));
        return addXPt4(Quaternion.newP4((Point4f) x1.value).mul(1 / f2)
            .toPoint4f());
      }
    case T.leftdivide:
      f = x2.asFloat();
      switch (x1.tok) {
      default:
        return addXInt(f == 0 ? 0
            : (int) Math.floor(x1.asFloat() / x2.asFloat()));
      case T.point4f:
        if (f == 0)
          return addXPt4(Point4f.new4(Float.NaN, Float.NaN, Float.NaN,
              Float.NaN));
        if (x2.tok == T.point4f)
          return addXPt4(Quaternion.newP4((Point4f) x1.value).divLeft(
              Quaternion.newP4((Point4f) x2.value)).toPoint4f());
        return addXPt4(Quaternion.newP4((Point4f) x1.value).mul(1 / f)
            .toPoint4f());
      }
    case T.timestimes:
      f = (float) Math
          .pow(x1.asFloat(), x2.asFloat());
      return (x1.tok == T.integer && x2.tok == T.integer ? addXInt((int) f)
          : addXFloat(f));
    }
    return true;
  }

  static private String typeOf(SV x) {
    int tok = (x == null ? T.nada : x.tok);
    switch (tok) {
    case T.on:
    case T.off:
      return "boolean";
    case T.bitset:
      return (x.value instanceof BondSet ? "bondset" : "bitset");
    case T.integer:
    case T.decimal:
    case T.point3f:
    case T.point4f:
    case T.string:
    case T.varray:
    case T.hash:
    case T.matrix3f:
    case T.matrix4f:
      return T.astrType[tok];
    }
    return "?";
  }

  private boolean getAllProperties(SV x2, String abbr)
      throws ScriptException {
    if (x2.tok != T.bitset)
      return false;
    if (isSyntaxCheck)
      return addXStr("");
    BS bs = SV.bsSelectVar(x2);
    JmolList<T> tokens;
    int n = bs.cardinality();
    if (n == 0
        || (tokens = T.getAtomPropertiesLike(abbr.substring(0, abbr
            .length() - 1))) == null)
      return addXStr("");
    Map<String, Object> ht = new Hashtable<String, Object>();
    int index = (n == 1 ? bs.nextSetBit(0) : Integer.MAX_VALUE);
    for (int i = tokens.size(); --i >= 0;) {
      T t = tokens.get(i);
      int tok = t.tok;
      switch (tok) {
      case T.configuration:
      case T.cell:
        continue;
      default:
        if (index == Integer.MAX_VALUE)
          tok |= T.minmaxmask;
        ht.put((String) t.value, SV.getVariable(
            eval.getBitsetProperty(bs, tok, null, null, null, null, false, index, true)));
      }
    }
    return addXMap(ht);
  }

  static Matrix4f getMatrix4f(Matrix3f matRotate, Tuple3f vTranslate) {
    return Matrix4f.newMV(matRotate, vTranslate == null ? new V3() : V3.newV(vTranslate));
  }

  private boolean getBoundBox(SV x2) {
    if (x2.tok != T.bitset)
      return false;
    if (isSyntaxCheck)
      return addXStr("");
    BoxInfo b = viewer.getBoxInfo(SV.bsSelectVar(x2), 1);
    P3[] pts = b.getBoundBoxPoints(true);
    JmolList<P3> list = new  JmolList<P3>();
    for (int i = 0; i < 4; i++)
      list.addLast(pts[i]);
    return addXList(list);
  }

  private boolean getPointOrBitsetOperation(T op, SV x2)
      throws ScriptException {
    switch (x2.tok) {
    case T.varray:
      switch (op.intValue) {
      case T.min:
      case T.max:
      case T.average:
      case T.stddev:
      case T.sum:
      case T.sum2:
        return addXObj(getMinMax(x2.getList(), op.intValue));
      case T.sort:
      case T.reverse:
        return addXVar(x2
            .sortOrReverse(op.intValue == T.reverse ? Integer.MIN_VALUE : 1));
      }
      SV[] list2 = new SV[x2.getList().size()];
      for (int i = 0; i < list2.length; i++) {
        Object v = SV.unescapePointOrBitsetAsVariable(x2.getList()
            .get(i));
        if (!(v instanceof SV)
            || !getPointOrBitsetOperation(op, (SV) v))
          return false;
        list2[i] = xStack[xPt--];
      }
      return addXAV(list2);
    case T.point3f:
      switch (op.intValue) {
      case T.atomx:
      case T.x:
        return addXFloat(((P3) x2.value).x);
      case T.atomy:
      case T.y:
        return addXFloat(((P3) x2.value).y);
      case T.atomz:
      case T.z:
        return addXFloat(((P3) x2.value).z);
      case T.xyz:
        P3 pt = P3.newP((P3) x2.value);
        // assumes a fractional coordinate
        viewer.toCartesian(pt, true);
        return addXPt(pt);
      case T.fracx:
      case T.fracy:
      case T.fracz:
      case T.fracxyz:
        P3 ptf = P3.newP((P3) x2.value);
        viewer.toFractional(ptf, true);
        return (op.intValue == T.fracxyz ? addXPt(ptf)
            : addXFloat(op.intValue == T.fracx ? ptf.x
                : op.intValue == T.fracy ? ptf.y : ptf.z));
      case T.fux:
      case T.fuy:
      case T.fuz:
      case T.fuxyz:
        P3 ptfu = P3.newP((P3) x2.value);
        viewer.toFractional(ptfu, false);
        return (op.intValue == T.fracxyz ? addXPt(ptfu)
            : addXFloat(op.intValue == T.fux ? ptfu.x
                : op.intValue == T.fuy ? ptfu.y : ptfu.z));
      case T.unitx:
      case T.unity:
      case T.unitz:
      case T.unitxyz:
        P3 ptu = P3.newP((P3) x2.value);
        viewer.toUnitCell(ptu, null);
        viewer.toFractional(ptu, false);
        return (op.intValue == T.unitxyz ? addXPt(ptu)
            : addXFloat(op.intValue == T.unitx ? ptu.x
                : op.intValue == T.unity ? ptu.y : ptu.z));
      }
      break;
    case T.point4f:
      switch (op.intValue) {
      case T.atomx:
      case T.x:
        return addXFloat(((Point4f) x2.value).x);
      case T.atomy:
      case T.y:
        return addXFloat(((Point4f) x2.value).y);
      case T.atomz:
      case T.z:
        return addXFloat(((Point4f) x2.value).z);
      case T.w:
        return addXFloat(((Point4f) x2.value).w);
      }
      break;
    case T.bitset:
      if (op.intValue == T.bonds && x2.value instanceof BondSet)
        return addXVar(x2);
      BS bs = SV.bsSelectVar(x2);
      if (bs.cardinality() == 1 && (op.intValue & T.minmaxmask) == 0)
        op.intValue |= T.min;
      Object val = eval.getBitsetProperty(bs, op.intValue, null, null,
          x2.value, op.value, false, x2.index, true);
      if (op.intValue != T.bonds)
        return addXObj(val);
      return addXVar(SV.newVariable(T.bitset, new BondSet(
          (BS) val, viewer.getAtomIndices(bs))));
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private static Object getMinMax(Object floatOrSVArray, int tok) {
    float[] data = null;
    JmolList<SV> sv = null;
    int ndata = 0;
    while (true) {
      if (Escape.isAF(floatOrSVArray)) {
        data = (float[]) floatOrSVArray;
        ndata = data.length;
        if (ndata == 0)
          break;
      } else if (floatOrSVArray instanceof JmolList<?>) {
        sv = (JmolList<SV>) floatOrSVArray;
        ndata = sv.size();
        if (ndata == 0)
          break;
        SV sv0 = sv.get(0);
        if (sv0.tok == T.string && ((String) sv0.value).startsWith("{")) {
          Object pt = SV.ptValue(sv0);
          if (pt instanceof P3)
            return getMinMaxPoint(sv, tok);
          if (pt instanceof Point4f)
            return getMinMaxQuaternion(sv, tok);
          break;
        }
      } else {
        break;
      }
      double sum;
      switch (tok) {
      case T.min:
        sum = Float.MAX_VALUE;
        break;
      case T.max:
        sum = -Float.MAX_VALUE;
        break;
      default:
        sum = 0;
      }
      double sum2 = 0;
      int n = 0;
      for (int i = ndata; --i >= 0;) {
        float v = (data == null ? SV.fValue(sv.get(i)) : data[i]);
        if (Float.isNaN(v))
          continue;
        n++;
        switch (tok) {
        case T.sum2:
        case T.stddev:
          sum2 += ((double) v) * v;
          //$FALL-THROUGH$
        case T.sum:
        case T.average:
          sum += v;
          break;
        case T.min:
          if (v < sum)
            sum = v;
          break;
        case T.max:
          if (v > sum)
            sum = v;
          break;
        }
      }
      if (n == 0)
        break;
      switch (tok) {
      case T.average:
        sum /= n;
        break;
      case T.stddev:
        if (n == 1)
          break;
        sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
        break;
      case T.min:
      case T.max:
      case T.sum:
        break;
      case T.sum2:
        sum = sum2;
        break;
      }
      return Float.valueOf((float) sum);
    }
    return "NaN";
  }

  /**
   * calculates the statistical value for x, y, and z independently
   * 
   * @param pointOrSVArray
   * @param tok
   * @return Point3f or "NaN"
   */
  @SuppressWarnings("unchecked")
  private static Object getMinMaxPoint(Object pointOrSVArray, int tok) {
    P3[] data = null;
    JmolList<SV> sv = null;
    int ndata = 0;
    if (pointOrSVArray instanceof Quaternion[]) {
      data = (P3[]) pointOrSVArray;
      ndata = data.length;
    } else if (pointOrSVArray instanceof JmolList<?>) {
      sv = (JmolList<SV>) pointOrSVArray;
      ndata = sv.size();
    }
    if (sv != null || data != null) {
      P3 result = new P3();
      float[] fdata = new float[ndata];
      boolean ok = true;
      for (int xyz = 0; xyz < 3 && ok; xyz++) {
        for (int i = 0; i < ndata; i++) {
          P3 pt = (data == null ? SV.ptValue(sv.get(i)) : data[i]);
          if (pt == null) {
            ok = false;
            break;
          }
          switch (xyz) {
          case 0:
            fdata[i] = pt.x;
            break;
          case 1:
            fdata[i] = pt.y;
            break;
          case 2:
            fdata[i] = pt.z;
            break;
          }
        }
        if (!ok)
          break;
        Object f = getMinMax(fdata, tok);
        if (f instanceof Float) {
          float value = ((Float) f).floatValue();
          switch (xyz) {
          case 0:
            result.x = value;
            break;
          case 1:
            result.y = value;
            break;
          case 2:
            result.z = value;
            break;
          }
        } else {
          break;
        }
      }
      return result;
    }
    return "NaN";
  }

  private static Object getMinMaxQuaternion(JmolList<SV> svData, int tok) {
    Quaternion[] data;
    switch (tok) {
    case T.min:
    case T.max:
    case T.sum:
    case T.sum2:
      return "NaN";
    }

    // only stddev and average

    while (true) {
      data = getQuaternionArray(svData, T.list);
      if (data == null)
        break;
      float[] retStddev = new float[1];
      Quaternion result = Quaternion.sphereMean(data, retStddev, 0.0001f);
      switch (tok) {
      case T.average:
        return result;
      case T.stddev:
        return Float.valueOf(retStddev[0]);
      }
      break;
    }
    return "NaN";
  }

  @SuppressWarnings("unchecked")
  protected static Quaternion[] getQuaternionArray(Object quaternionOrSVData, int itype) {
    Quaternion[] data;
    switch (itype) {
    case T.quaternion:
      data = (Quaternion[]) quaternionOrSVData;
      break;
    case T.point4f:
      Point4f[] pts = (Point4f[]) quaternionOrSVData;
      data = new Quaternion[pts.length];
      for (int i = 0; i < pts.length; i++)
        data[i] = Quaternion.newP4(pts[i]);
      break;
    case T.list:
      JmolList<SV> sv = (JmolList<SV>) quaternionOrSVData;
      data = new Quaternion[sv.size()];
      for (int i = 0; i < sv.size(); i++) {
        Point4f pt = SV.pt4Value(sv.get(i));
        if (pt == null)
          return null;
        data[i] = Quaternion.newP4(pt);
      }
      break;
    default:
      return null;
    }
    return data;
  }
  
}
