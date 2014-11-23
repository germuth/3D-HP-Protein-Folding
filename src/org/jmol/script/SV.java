/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-05 07:42:12 -0500 (Fri, 05 Jun 2009) $
 * $Revision: 10958 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.script;

import org.jmol.util.JmolList;
import java.util.Arrays;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;

import java.util.Map;


import org.jmol.modelset.Bond.BondSet;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.Parser;
import org.jmol.util.P3;
import org.jmol.util.Point4f;
import org.jmol.util.SB;
import org.jmol.util.V3;

import org.jmol.util.Measure;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;

public class SV extends T {

  final private static SV vT = newScriptVariableIntValue(on, 1, "true");
  final private static SV vF = newScriptVariableIntValue(off, 0, "false");

  int index = Integer.MAX_VALUE;    

  private final static int FLAG_CANINCREMENT = 1;
  private final static int FLAG_LOCALVAR = 2;

  private int flags = ~FLAG_CANINCREMENT & FLAG_LOCALVAR;
  private String myName;

  SV(int tok) {
    super(tok);
  }

  public static SV newVariable(int tok, Object value) {
    SV sv = new SV(tok);
    sv.value = value;
    return sv;
  }

  static SV newScriptVariableBs(BS bs, int index) {
    SV sv = new SV(bitset);
    sv.value = bs;
    if (index >= 0)
      sv.index = index;
    return sv;
  }

  static SV newScriptVariableToken(T x) {
    SV sv = new SV(x.tok);
    sv.intValue = x.intValue;
    sv.value = x.value;
    return sv;
  }
  
  static SV newScriptVariableIntValue(int tok, int intValue, Object value) {
    SV sv = new SV(tok);
    sv.intValue = intValue;
    sv.value = value;
    return sv;
  }

  @SuppressWarnings("unchecked")
  static int sizeOf(T x) {
    switch (x == null ? nada : x.tok) {
    case bitset:
      return BSUtil.cardinalityOf(bsSelectToken(x));
    case on:
    case off:
      return -1;
    case integer:
      return -2;
    case decimal:
      return -4;
    case point3f:
      return -8;
    case point4f:
      return -16;
    case matrix3f:
      return -32;
    case matrix4f:
      return -64;
    case string:
      return ((String) x.value).length();
    case varray:
      return x.intValue == Integer.MAX_VALUE ? ((SV)x).getList().size()
          : sizeOf(selectItemTok(x));
    case hash:
      return ((Map<String, SV>) x.value).size();
    default:
      return 0;
    }
  }

  static boolean isVariableType(Object x) {
    return (x instanceof SV
        || x instanceof BS
        || x instanceof Boolean
        || x instanceof Float
        || x instanceof Integer
        || x instanceof P3    // stored as point3f
        || x instanceof V3   // stored as point3f
        || x instanceof Point4f    // stored as point4f
        || x instanceof Quaternion // stored as point4f
        || x instanceof String
        || x instanceof Map<?, ?>  // stored as Map<String, ScriptVariable>
        || x instanceof JmolList<?>    // stored as list
    // in JavaScript, all these will be "Array" which is fine
        || x instanceof SV[] // stored as list
        || x instanceof double[]   // stored as list
        || x instanceof float[]    // stored as list
        || x instanceof float[][]  // stored as list
        || x instanceof Float[]    // stored as list
        || x instanceof int[]      // stored as list
        || x instanceof int[][]    // stored as list
        || x instanceof P3[]  // stored as list
        || x instanceof String[]); // stored as list
  }

  /**
   * @param x
   * @return a ScriptVariable of the input type, or if x is null, then a new
   *         ScriptVariable, or, if the type is not found, a string version
   */
  @SuppressWarnings("unchecked")
  public
  static SV getVariable(Object x) {
    if (x == null)
      return newVariable(T.string, "");
    if (x instanceof SV)
      return (SV) x;

    // the eight basic types are:
    // boolean, integer, decimal, string, point3f, point4f, bitset, and list
    // listf is a special temporary type for storing results
    // of .all in preparation for .bin in the case of xxx.all.bin
    // but with some work, this could be developed into a storage class

    if (x instanceof Boolean)
      return getBoolean(((Boolean) x).booleanValue());
    if (x instanceof Integer)
      return new ScriptVariableInt(((Integer) x).intValue());
    if (x instanceof Float)
      return newVariable(decimal, x);
    if (x instanceof String) {
      x = unescapePointOrBitsetAsVariable(x);
      if (x instanceof SV)
        return (SV) x;
      return newVariable(string, x);
    }
    if (x instanceof P3)
      return newVariable(point3f, x);
    if (x instanceof V3) // point3f is not mutable anyway
      return newVariable(point3f, P3.newP((V3) x));
    if (x instanceof BS)
      return newVariable(bitset, x);
    if (x instanceof Point4f)
      return newVariable(point4f, x);
    // note: for quaternions, we save them {q1, q2, q3, q0} 
    // While this may seem odd, it is so that for any point4 -- 
    // planes, axisangles, and quaternions -- we can use the 
    // first three coordinates to determine the relavent axis
    // the fourth then gives us offset to {0,0,0} (plane), 
    // rotation angle (axisangle), and cos(theta/2) (quaternion).
    if (x instanceof Quaternion)
      return newVariable(point4f, ((Quaternion) x).toPoint4f());
    if (x instanceof Matrix3f)
      return newVariable(matrix3f, x);
    if (x instanceof Matrix4f)
      return newVariable(matrix4f, x);
    if (Escape.isAFloat(x))
      return newVariable(listf, x);
    if (x instanceof Map)
      return getVariableMap(x);
    if (x instanceof JmolList)
      return getVariableList((JmolList) x);
    if (Escape.isAI(x))
      return getVariableAI((int[]) x);
    if (Escape.isAF(x))
      return getVariableAF((float[]) x);
    if (Escape.isAD(x))
      return getVariableAD((double[]) x);
    if (Escape.isAII(x))
      return getVariableAII((int[][]) x);
    if (Escape.isAFF(x))
      return getVariableAFF((float[][]) x);
    if (Escape.isAS(x))
      return getVariableAS((String[]) x);
    if (Escape.isAV(x))
      return getVariableAV((SV[]) x);
    if (Escape.isAP(x))
      return getVariableAP((P3[]) x);
    return newVariable(string, Escape.toReadable(null, x));
  }

  @SuppressWarnings("unchecked")
  static SV getVariableMap(Object x) {
    Map<String, Object> ht = (Map<String, Object>) x;
    Iterator<String> e = ht.keySet().iterator();
    while (e.hasNext()) {
      if (!(ht.get(e.next()) instanceof SV)) {
        Map<String, SV> x2 = new Hashtable<String, SV>();
        for (Map.Entry<String, Object> entry : ht.entrySet()) {
          String key = entry.getKey();
          Object o = entry.getValue();
          if (isVariableType(o))
            x2.put(key, getVariable(o));
          else
            x2.put(key, newVariable(string, Escape.toReadable(null,
                o)));
        }
        x = x2;
      }
      break; // just need to check the first one
    }
    return newVariable(hash, x);
  }

  static SV getVariableList(JmolList<?> v) {
    int len = v.size();
    if (len > 0 && v.get(0) instanceof SV)
      return newVariable(varray, v);
    JmolList<SV> objects = new  JmolList<SV>();
    for (int i = 0; i < len; i++)
      objects.addLast(getVariable(v.get(i)));
    return newVariable(varray, objects);
  }

  static SV getVariableAV(SV[] v) {
    JmolList<SV> objects = new  JmolList<SV>();
    for (int i = 0; i < v.length; i++)
      objects.addLast(v[i]);
    return newVariable(varray, objects);
  }

  static SV getVariableAD(double[] f) {
    JmolList<SV> objects = new  JmolList<SV>();
    for (int i = 0; i < f.length; i++)
      objects.addLast(newVariable(decimal, Float.valueOf((float) f[i])));
    return newVariable(varray, objects);
  }

  static SV getVariableAS(String[] s) {
    JmolList<SV> objects = new  JmolList<SV>();
    for (int i = 0; i < s.length; i++)
      objects.addLast(newVariable(string, s[i]));
    return newVariable(varray, objects);
  }

  static SV getVariableAP(P3[] p) {
    JmolList<SV> objects = new  JmolList<SV>();
    for (int i = 0; i < p.length; i++)
      objects.addLast(newVariable(point3f, p[i]));
    return newVariable(varray, objects);
  }

  static SV getVariableAFF(float[][] fx) {
    JmolList<SV> objects = new  JmolList<SV>();
    for (int i = 0; i < fx.length; i++)
      objects.addLast(getVariableAF(fx[i]));
    return newVariable(varray, objects);
  }

  static SV getVariableAII(int[][] ix) {
    JmolList<SV> objects = new  JmolList<SV>();
    for (int i = 0; i < ix.length; i++)
      objects.addLast(getVariableAI(ix[i]));
    return newVariable(varray, objects);
  }

  static SV getVariableAF(float[] f) {
    JmolList<SV> objects = new  JmolList<SV>();
    for (int i = 0; i < f.length; i++)
      objects.addLast(newVariable(decimal, Float.valueOf(f[i])));
    return newVariable(varray, objects);
  }

  static SV getVariableAI(int[] ix) {
    JmolList<SV> objects = new  JmolList<SV>();
    for (int i = 0; i < ix.length; i++)
      objects.addLast(newVariable(integer, Integer.valueOf(ix[i])));
    return newVariable(varray, objects);
  }

  /**
   * creates a NEW version of the variable
   * 
   * 
   * @param v
   * @param asCopy  create a new set of object pointers
   *                for an array; copies an associative array 
   * @return  new ScriptVariable
   */
  @SuppressWarnings("unchecked")
  SV set(SV v, boolean asCopy) {
    // note: bitset, point3f ,point4f will not be copied
    //       because they are essentially immutable here
    index = v.index;
    intValue = v.intValue;
    tok = v.tok;
    value = v.value;
    if (asCopy) {
      switch (tok) {
      case hash:
        value = new Hashtable<String, SV>(
            (Map<String, SV>) v.value);
        break;
      case varray:
        JmolList<SV> o2 = new  JmolList<SV>();
        JmolList<SV> o1 = v.getList();
        for (int i = 0; i < o1.size(); i++)
          o2.addLast(o1.get(i));
        value = o2;
        break;
      }
    }
    return this;
  }

  public SV setName(String name) {
    this.myName = name;
    flags |= FLAG_CANINCREMENT;
    //System.out.println("Variable: " + name + " " + intValue + " " + value);
    return this;
  }

  public SV setGlobal() {
    flags &= ~FLAG_LOCALVAR;
    return this;
  }

  boolean canIncrement() {
    return tokAttr(flags, FLAG_CANINCREMENT);
  }

  boolean increment(int n) {
    if (!canIncrement())
      return false;
    switch (tok) {
    case integer:
      intValue += n;
      break;
    case decimal:
      value = Float.valueOf(((Float) value).floatValue() + n);
      break;
    default:
      value = nValue(this);
      if (value instanceof Integer) {
        tok = integer;
        intValue = ((Integer) value).intValue();
      } else {
        tok = decimal;
      }
    }
    return true;
  }

  public boolean asBoolean() {
    return bValue(this);
  }

  public int asInt() {
    return iValue(this);
  }

  float asFloat() {
    return fValue(this);
  }

  public String asString() {
    return sValue(this);
  }

  // math-related Token static methods

  private final static P3 pt0 = new P3();

  /**
   * 
   * @param x
   * @return   Object-wrapped value
   */
  
  public static Object oValue(SV x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return Boolean.TRUE;
    case nada:
    case off:
      return Boolean.FALSE;
    case integer:
      return Integer.valueOf(x.intValue);
    case bitset:
//      return bsSelect(x);
    case array:
      return selectItemVar(x).value;
    default:
      return x.value;
    }
  }

  /**
   * 
   * @param x
   * @return  numeric value -- integer or decimal
   */
  static Object nValue(T x) {
    int iValue;
    switch (x == null ? nada : x.tok) {
    case decimal:
      return x.value;
    case integer:
      iValue = x.intValue;
      break;
    case string:
      if (((String) x.value).indexOf(".") >= 0)
        return Float.valueOf(toFloat((String) x.value));
      iValue = (int) toFloat((String) x.value);
      break;
    default:
      iValue = 0;
    }
    return Integer.valueOf(iValue);
  }

  // there are reasons to use Token here rather than ScriptVariable
  // some of these functions, in particular iValue, fValue, and sValue
  
  private static boolean bValue(T x) {
    switch (x == null ? nada : x.tok) {
    case on:
    case hash:
      return true;
    case off:
      return false;
    case integer:
      return x.intValue != 0;
    case decimal:
    case string:
    case varray:
      return fValue(x) != 0;
    case bitset:
      return iValue(x) != 0;
    case point3f:
    case point4f:
    case matrix3f:
    case matrix4f:
      return Math.abs(fValue(x)) > 0.0001f;
    default:
      return false;
    }
  }

  static int iValue(T x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
    case varray:
    case string:
    case point3f:
    case point4f:
    case matrix3f:
    case matrix4f:
      return (int) fValue(x);
    case bitset:
      return BSUtil.cardinalityOf(bsSelectToken(x));
    default:
      return 0;
    }
  }

  static float fValue(T x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
      return ((Float) x.value).floatValue();
    case varray:
      int i = x.intValue;
      if (i == Integer.MAX_VALUE)
        return ((SV)x).getList().size();
      //$FALL-THROUGH$
    case string:
      return toFloat(sValue(x));
    case bitset:
      return iValue(x);
    case point3f:
      return ((P3) x.value).distance(pt0);
    case point4f:
      return Measure.distanceToPlane((Point4f) x.value, pt0);
    case matrix3f:
      P3 pt = new P3();
      ((Matrix3f) x.value).transform(pt);
      return pt.distance(pt0);
    case matrix4f:
      P3 pt1 = new P3();
      ((Matrix4f) x.value).transform(pt1);
      return pt1.distance(pt0);
    default:
      return 0;
    }
  }

  static String sValue(T x) {
    if (x == null)
      return "";
    int i;
    SB sb;
    Map<Object, Boolean> map;
    switch (x.tok) {
    case on:
      return "true";
    case off:
      return "false";
    case integer:
      return "" + x.intValue;
    case bitset:
      return Escape.eB(bsSelectToken(x), !(x.value instanceof BondSet));
    case varray:
      JmolList<SV> sv = ((SV) x).getList();
      i = x.intValue;
      if (i <= 0)
        i = sv.size() - i;
      if (i != Integer.MAX_VALUE)
        return (i < 1 || i > sv.size() ? "" : sValue(sv.get(i - 1)));
      //$FALL-THROUGH$
    case hash:
      sb = new SB();
      map = new Hashtable<Object, Boolean>();
      sValueArray(sb, (SV) x, map, 0, false);
      return sb.toString();
    case string:
      String s = (String) x.value;
      i = x.intValue;
      if (i <= 0)
        i = s.length() - i;
      if (i == Integer.MAX_VALUE)
        return s;
      if (i < 1 || i > s.length())
        return "";
      return "" + s.charAt(i - 1);
    case point3f:
    case point4f:
    case matrix3f:
    case matrix4f:
      return Escape.e(x.value);
    default:
      return x.value.toString();
    }
  }

  @SuppressWarnings("unchecked")
  private static void sValueArray(SB sb, SV vx,
                                  Map<Object, Boolean> map, int level,
                                  boolean isEscaped) {
    switch (vx.tok) {
    case hash:
      if (map.containsKey(vx)) {
        sb.append(isEscaped ? "{}" : vx.myName == null ? "<circular reference>"
            : "<" + vx.myName + ">");
        break;
      }
      map.put(vx, Boolean.TRUE);
      Map<String, SV> ht = (Map<String, SV>) vx.value;
      Object[] keys = ht.keySet().toArray();
      Arrays.sort(keys);

      if (isEscaped) {
        sb.append("{ ");
        String sep = "";
        for (int i = 0; i < keys.length; i++) {
          String key = (String) keys[i];
          sb.append(sep).append(Escape.eS(key)).appendC(':');
          sValueArray(sb, ht.get(key), map, level + 1, true);
          sep = ", ";
        }
        sb.append(" }");
        break;
      }
      for (int i = 0; i < keys.length; i++) {
        sb.append((String) keys[i]).append("\t:");
        SV v = ht.get(keys[i]);
        SB sb2 = new SB();
        sValueArray(sb2, v, map, level + 1, isEscaped);
        String value = sb2.toString();
        sb.append(value.indexOf("\n") >= 0 ? "\n" : "\t");
        sb.append(value).append("\n");
      }
      break;
    case varray:
      if (map.containsKey(vx)) {
        sb.append(isEscaped ? "[]" : vx.myName == null ? "<circular reference>"
            : "<" + vx.myName + ">");
        break;
      }
      map.put(vx, Boolean.TRUE);
      if (isEscaped)
        sb.append("[");
      JmolList<SV> sx = vx.getList();
      for (int i = 0; i < sx.size(); i++) {
        if (isEscaped && i > 0)
          sb.append(",");
        SV sv = sx.get(i);
        sValueArray(sb, sv, map, level + 1, isEscaped);
        if (!isEscaped)
          sb.append("\n");
      }
      if (isEscaped)
        sb.append("]");
      break;
    default:
      if (!isEscaped)
        for (int j = 0; j < level - 1; j++)
          sb.append("\t");
      sb.append(isEscaped ? vx.escape() : sValue(vx));
    }
  }

  static P3 ptValue(SV x) {
    switch (x.tok) {
    case point3f:
      return (P3) x.value;
    case string:
      Object o = Escape.uP((String) x.value);
      if (o instanceof P3)
        return (P3) o;
    }
    return null;
  }  

  static Point4f pt4Value(SV x) {
    switch (x.tok) {
    case point4f:
      return (Point4f) x.value;
    case string:
      Object o = Escape.uP((String) x.value);
      if (!(o instanceof Point4f))
        break;
      return (Point4f) o;
    }
    return null;
  }

  private static float toFloat(String s) {
    if (s.equalsIgnoreCase("true"))
      return 1;
    if (s.equalsIgnoreCase("false") || s.length() == 0)
      return 0;
    return Parser.parseFloatStrict(s);
  }

  static SV concatList(SV x1, SV x2,
                                          boolean asNew) {
    JmolList<SV> v1 = x1.getList();
    JmolList<SV> v2 = x2.getList();
    if (!asNew) {
      if (v2 == null)
        v1.addLast(newScriptVariableToken(x2));
      else
        for (int i = 0; i < v2.size(); i++)
          v1.addLast(v2.get(i));
      return x1;
    }
    JmolList<SV> vlist = new JmolList<SV>();
    //(v1 == null ? 1 : v1.size()) + (v2 == null ? 1 : v2.size())
    if (v1 == null)
      vlist.addLast(x1);
    else
      for (int i = 0; i < v1.size(); i++)
        vlist.addLast(v1.get(i));
    if (v2 == null)
      vlist.addLast(x2);
    else
      for (int i = 0; i < v2.size(); i++)
        vlist.addLast(v2.get(i));
    return getVariableList(vlist);
  }

  static BS bsSelectToken(T x) {
    x = selectItemTok2(x, Integer.MIN_VALUE);
    return (BS) x.value;
  }

  static BS bsSelectVar(SV var) {
    if (var.index == Integer.MAX_VALUE)
      var = selectItemVar(var);
    return (BS) var.value;
  }

  static BS bsSelectRange(T x, int n) {
    x = selectItemTok(x);
    x = selectItemTok2(x, (n <= 0 ? n : 1));
    x = selectItemTok2(x, (n <= 0 ? Integer.MAX_VALUE - 1 : n));
    return (BS) x.value;
  }

  static SV selectItemVar(SV var) {
    // pass bitsets created by the select() or for() commands
    // and all arrays by reference
    if (var.index != Integer.MAX_VALUE || 
        var.tok == varray && var.intValue == Integer.MAX_VALUE)
      return var;
    return selectItemVar2(var, Integer.MIN_VALUE);
  }

  static T selectItemTok(T var) {
    return selectItemTok2(var, Integer.MIN_VALUE);
  }

  static SV selectItemVar2(SV var, int i2) {
    return (SV) selectItemTok2(var, i2);
  }

  static T selectItemTok2(T tokenIn, int i2) {
    switch (tokenIn.tok) {
    case matrix3f:
    case matrix4f:
    case bitset:
    case varray:
    case string:
      break;
    default:
      return tokenIn;
    }

    // negative number is a count from the end

    BS bs = null;
    String s = null;

    int i1 = tokenIn.intValue;
    if (i1 == Integer.MAX_VALUE) {
      // no selections have been made yet --
      // we just create a new token with the
      // same bitset and now indicate either
      // the selected value or "ALL" (max_value)
      if (i2 == Integer.MIN_VALUE)
        i2 = i1;
      SV v = newScriptVariableIntValue(tokenIn.tok, i2, tokenIn.value);
      return v;
    }
    int len = 0;
    boolean isInputSelected = (tokenIn instanceof SV && ((SV) tokenIn).index != Integer.MAX_VALUE);
    SV tokenOut = newScriptVariableIntValue(tokenIn.tok, Integer.MAX_VALUE, null);

    switch (tokenIn.tok) {
    case bitset:
      if (tokenIn.value instanceof BondSet) {
        bs = new BondSet((BS) tokenIn.value,
            ((BondSet) tokenIn.value).getAssociatedAtoms());
        len = BSUtil.cardinalityOf(bs);
      } else {
        bs = BSUtil.copy((BS) tokenIn.value);
        len = (isInputSelected ? 1 : BSUtil.cardinalityOf(bs));
      }
      break;
    case varray:
      len = ((SV)tokenIn).getList().size();
      break;
    case string:
      s = (String) tokenIn.value;
      len = s.length();
      break;
    case matrix3f:
      len = -3;
      break;
    case matrix4f:
      len = -4;
      break;
    }

    if (len < 0) {
      // matrix mode [1][3] or [13]
      len = -len;
      if (i1 > 0 && Math.abs(i1) > len) {
        int col = i1 % 10;
        int row = (i1 - col) / 10;
        if (col > 0 && col <= len && row <= len) {
          if (tokenIn.tok == matrix3f)
            return newVariable(decimal, Float.valueOf(
                ((Matrix3f) tokenIn.value).getElement(row - 1, col - 1)));
          return newVariable(decimal, Float.valueOf(
              ((Matrix4f) tokenIn.value).getElement(row - 1, col - 1)));
        }
        return newVariable(string, "");
      }
      if (Math.abs(i1) > len)
        return newVariable(string, "");
      float[] data = new float[len];
      if (len == 3) {
        if (i1 < 0)
          ((Matrix3f) tokenIn.value).getColumn(-1 - i1, data);
        else
          ((Matrix3f) tokenIn.value).getRow(i1 - 1, data);
      } else {
        if (i1 < 0)
          ((Matrix4f) tokenIn.value).getColumn(-1 - i1, data);
        else
          ((Matrix4f) tokenIn.value).getRow(i1 - 1, data);
      }
      if (i2 == Integer.MIN_VALUE)
        return getVariableAF(data);
      if (i2 < 1 || i2 > len)
        return newVariable(string, "");
      return newVariable(decimal, Float.valueOf(data[i2 - 1]));
    }

    // "testing"[0] gives "g"
    // "testing"[-1] gives "n"
    // "testing"[3][0] gives "sting"
    // "testing"[-1][0] gives "ng"
    // "testing"[0][-2] gives just "g" as well
    if (i1 <= 0)
      i1 = len + i1;
    if (i1 < 1)
      i1 = 1;
    if (i2 == 0)
      i2 = len;
    else if (i2 < 0)
      i2 = len + i2;

    if (i2 > len)
      i2 = len;
    else if (i2 < i1)
      i2 = i1;

    switch (tokenIn.tok) {
    case bitset:
      tokenOut.value = bs;
      if (isInputSelected) {
        if (i1 > 1)
          bs.clearAll();
        break;
      }
      int n = 0;
      for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
        if (++n < i1 || n > i2)
          bs.clear(j);
      break;
    case string:
      if (i1 < 1 || i1 > len)
        tokenOut.value = "";
      else
        tokenOut.value = s.substring(i1 - 1, i2);
      break;
    case varray:
      if (i1 < 1 || i1 > len || i2 > len)
        return newVariable(string, "");
      if (i2 == i1)
        return ((SV) tokenIn).getList().get(i1 - 1);
      JmolList<SV> o2 = new  JmolList<SV>();
      JmolList<SV> o1 = ((SV) tokenIn).getList();
      n = i2 - i1 + 1;
      for (int i = 0; i < n; i++)
        o2.addLast(newScriptVariableToken(o1.get(i + i1 - 1)));
      tokenOut.value = o2;
      break;
    }
    return tokenOut;
  }

  boolean setSelectedValue(int selector, SV var) {
    if (selector == Integer.MAX_VALUE)
      return false;
    int len;
    switch (tok) {
    case matrix3f:
    case matrix4f:
      len = (tok == matrix3f ? 3 : 4);
      if (selector > 10) {
        int col = selector % 10;
        int row = (selector - col) / 10;
        if (col > 0 && col <= len && row <= len) {
          if (tok == matrix3f)
            ((Matrix3f) value).setElement(row - 1, col - 1, fValue(var));
          else
            ((Matrix4f) value).setElement(row - 1, col - 1, fValue(var));
          return true;
        }
      }
      if (selector != 0 && Math.abs(selector) <= len
          && var.tok == varray) {
        JmolList<SV> sv = var.getList();
        if (sv.size() == len) {
          float[] data = new float[len];
          for (int i = 0; i < len; i++)
            data[i] = fValue(sv.get(i));
          if (selector > 0) {
            if (tok == matrix3f)
              ((Matrix3f) value).setRowA(selector - 1, data);
            else
              ((Matrix4f) value).setRow(selector - 1, data);
          } else {
            if (tok == matrix3f)
              ((Matrix3f) value).setColumnA(-1 - selector, data);
            else
              ((Matrix4f) value).setColumn(-1 - selector, data);
          }
          return true;
        }
      }
      return false;
    case string:
      String str = (String) value;
      int pt = str.length();
      if (selector <= 0)
        selector = pt + selector;
      if (--selector < 0)
        selector = 0;
      while (selector >= str.length())
        str += " ";
      value = str.substring(0, selector) + sValue(var)
          + str.substring(selector + 1);
      return true;
    case varray:
      len = getList().size();
      if (selector <= 0)
        selector = len + selector;
      if (--selector < 0)
        selector = 0;
      if (len <= selector) {
        for (int i = len; i <= selector; i++)
          getList().addLast(newVariable(string, ""));
      }
      getList().set(selector, var);
      return true;
    }
    return false;
  }

  public String escape() {
    switch (tok) {
    case string:
      return Escape.e(value);
    case varray:
    case hash:
      SB sb = new SB();
      Map<Object,Boolean>map = new Hashtable<Object,Boolean>();
      sValueArray(sb, this, map, 0, true);
      return sb.toString();
    default:
      return sValue(this);
    }
  }

  static Object unescapePointOrBitsetAsVariable(Object o) {
    if (o == null)
      return o;
    Object v = null;
    String s = null;
    if (o instanceof SV) {
      SV sv = (SV) o;
      switch (sv.tok) {
      case point3f:
      case point4f:
      case matrix3f:
      case matrix4f:
      case bitset:
        v = sv.value;
        break;
      case string:
        s = (String) sv.value;
        break;
      default:
        s = sValue(sv);
        break;
      }
    } else if (o instanceof String) {
      s = (String) o;
    }
    if (s != null && s.length() == 0)
      return s;
    if (v == null)
      v = Escape.unescapePointOrBitsetOrMatrixOrArray(s);
    if (v instanceof P3)
      return (newVariable(point3f, v));
    if (v instanceof Point4f)
      return newVariable(point4f, v);
    if (v instanceof BS) {
      if (s != null && s.indexOf("[{") == 0)
        v = new BondSet((BS) v);
      return newVariable(bitset, v);
    }
    if (v instanceof Matrix3f)
      return (newVariable(matrix3f, v));
    if (v instanceof Matrix4f)
      return newVariable(matrix4f, v);
    return o;
  }

  public static SV getBoolean(boolean value) {
    return newScriptVariableToken(value ? vT : vF);
  }
  
  static Object sprintf(String strFormat, SV var) {
    if (var == null)
      return strFormat;
    int[] vd = (strFormat.indexOf("d") >= 0 || strFormat.indexOf("i") >= 0 ? new int[1]
        : null);
    float[] vf = (strFormat.indexOf("f") >= 0 ? new float[1] : null);
    double[] ve = (strFormat.indexOf("e") >= 0 ? new double[1] : null);
    boolean getS = (strFormat.indexOf("s") >= 0);
    boolean getP = (strFormat.indexOf("p") >= 0 && var.tok == point3f);
    boolean getQ = (strFormat.indexOf("q") >= 0 && var.tok == point4f);
    Object[] of = new Object[] { vd, vf, ve, null, null, null};
    if (var.tok != varray)
      return sprintf(strFormat, var, of, vd, vf, ve, getS, getP, getQ);
    JmolList<SV> sv = var.getList();
    String[] list2 = new String[sv.size()];
    for (int i = 0; i < list2.length; i++)
      list2[i] = sprintf(strFormat, sv.get(i), of, vd, vf, ve, getS, getP, getQ);
    return list2;
  }

  private static String sprintf(String strFormat, SV var, Object[] of, 
                                int[] vd, float[] vf, double[] ve, boolean getS, boolean getP, boolean getQ) {
    if (vd != null)
      vd[0] = iValue(var);
    if (vf != null)
      vf[0] = fValue(var);
    if (ve != null)
      ve[0] = fValue(var);
    if (getS)
      of[3] = sValue(var);
    if (getP)
      of[4]= var.value;
    if (getQ)
      of[5]= var.value;
    return TextFormat.sprintf(strFormat, "IFDspq", of );
  }

  /**
   * sprintf       accepts arguments from the format() function
   *               First argument is a format string.
   * @param args
   * @return       formatted string
   */
  static String sprintfArray(SV[] args) {
    switch(args.length){
    case 0:
      return "";
    case 1:
      return sValue(args[0]);
    }
    String[] format = TextFormat.split(TextFormat.simpleReplace(sValue(args[0]), "%%","\1"), '%');
    SB sb = new SB();
    sb.append(format[0]);
    for (int i = 1; i < format.length; i++) {
      Object ret = sprintf(TextFormat.formatCheck("%" + format[i]), (i < args.length ? args[i] : null));
      if (Escape.isAS(ret)) {
        String[] list = (String[]) ret;
        for (int j = 0; j < list.length; j++)
          sb.append(list[j]).append("\n");
        continue;
      }
      sb.append((String) ret);
    }
    return sb.toString();
  }
  
  @Override
  public String toString() {
    return super.toString() + "[" + myName + " index =" + index + " intValue=" + intValue + "]";
  }

  @SuppressWarnings("unchecked")
  public
  static BS getBitSet(SV x, boolean allowNull) {
    switch (x.tok) {
    case bitset:
      return bsSelectVar(x);
    case varray:
      BS bs = new BS();
      JmolList<SV> sv = (JmolList<SV>) x.value;
      for (int i = 0; i < sv.size(); i++)
        if (!sv.get(i).unEscapeBitSetArray(bs) && allowNull)
          return null;
      return bs;
    }
    return (allowNull ? null : new BS());
  }

  static boolean areEqual(SV x1, SV x2) {
    if (x1 == null || x2 == null)
      return false;
    if (x1.tok == string && x2.tok == string)
      return sValue(x1).equalsIgnoreCase(
          sValue(x2));
    if (x1.tok == point3f && x2.tok == point3f)
      return (((P3) x1.value).distance((P3) x2.value) < 0.000001);
    if (x1.tok == point4f && x2.tok == point4f)
      return (((Point4f) x1.value).distance((Point4f) x2.value) < 0.000001);
    return (Math.abs(fValue(x1)
        - fValue(x2)) < 0.000001);
  }

  protected class Sort implements Comparator<SV> {
    private int arrayPt;
    
    protected Sort(int arrayPt) {
      this.arrayPt = arrayPt;
    }
    
    public int compare(SV x, SV y) {
      if (x.tok != y.tok) {
        if (x.tok == T.decimal || x.tok == T.integer
            || y.tok == T.decimal || y.tok == T.integer) {
          float fx = fValue(x);
          float fy = fValue(y);
          return (fx < fy ? -1 : fx > fy ? 1 : 0);
        }
        if (x.tok == T.string || y.tok == T.string)
          return sValue(x).compareTo(sValue(y));
      }
      switch (x.tok) {
      case string:
        return sValue(x).compareTo(sValue(y));
      case varray:
        JmolList<SV> sx = x.getList();
        JmolList<SV> sy = y.getList();
        if (sx.size() != sy.size())
          return (sx.size() < sy.size() ? -1 : 1);
        int iPt = arrayPt;
        if (iPt < 0)
          iPt += sx.size();
        if (iPt < 0 || iPt >= sx.size())
          return 0;
        return compare(sx.get(iPt), sy.get(iPt));
      default:
        float fx = fValue(x);
        float fy = fValue(y);
        return (fx < fy ? -1 : fx > fy ? 1 : 0);
      }
    } 
  }
  
  /**
   * 
   * @param arrayPt   1-based or Integer.MIN_VALUE to reverse
   * @return sorted or reversed array
   */
  SV sortOrReverse(int arrayPt) {
    JmolList<SV> x = getList();
    if (x == null || x.size() < 2) 
      return this;
    if (arrayPt == Integer.MIN_VALUE) {
      // reverse
      int n = x.size();
      for (int i = 0; i < n; i++) {
        SV v = x.get(i);
        x.set(i, x.get(--n));
        x.set(n, v);
      }
    } else {
      Collections.sort(getList(), new Sort(--arrayPt));
    }
    return this;
  }

  boolean unEscapeBitSetArray(BS bs) {
    switch(tok) {
    case string:
      BS bs1 = Escape.uB((String) value);
      if (bs1 == null)
        return false;
      bs.or(bs1);
      return true;
    case bitset:
      bs.or((BS) value);
      return true;
    }
    return false;   
  }

  static BS unEscapeBitSetArray(ArrayList<SV> x, boolean allowNull) {
    BS bs = new BS();
    for (int i = 0; i < x.size(); i++)
      if (!x.get(i).unEscapeBitSetArray(bs) && allowNull)
        return null;
    return bs;
  }

  static String[] listValue(T x) {
    if (x.tok != varray)
      return new String[] { sValue(x) };
    JmolList<SV> sv = ((SV) x).getList();
    String[] list = new String[sv.size()];
    for (int i = sv.size(); --i >= 0;)
      list[i] = sValue(sv.get(i));
    return list;
  }

  static float[] flistValue(T x, int nMin) {
    if (x.tok != varray)
      return new float[] { fValue(x) };
    JmolList<SV> sv = ((SV) x).getList();
    float[] list;
    list = new float[Math.max(nMin, sv.size())];
    if (nMin == 0)
      nMin = list.length;
    for (int i = Math.min(sv.size(), nMin); --i >= 0;)
      list[i] = fValue(sv.get(i));
    return list;
  }

  void toArray() {
    int dim;
    Matrix3f m3 = null;
    Matrix4f m4 = null;
    switch (tok) {
    case matrix3f:
      m3 = (Matrix3f) value;
      dim = 3;
      break;
    case matrix4f:
      m4 = (Matrix4f) value;
      dim = 4;
      break;
    default:
      return;
    }
    tok = varray;
    JmolList<SV> o2 = new  JmolList<SV>(); //dim;
    for (int i = 0; i < dim; i++) {
      float[] a = new float[dim];
      if (m3 == null)
        m4.getRow(i, a);
      else
        m3.getRow(i, a);
      o2.set(i,getVariableAF(a));
    }
    value = o2;
  }

  @SuppressWarnings("unchecked")
  SV mapValue(String key) {
    return (tok == hash ? ((Map<String, SV>) value).get(key) : null);
  }

  @SuppressWarnings("unchecked")
  public
  JmolList<SV> getList() {
    return (tok == varray ? (JmolList<SV>) value : null);
  }

}
