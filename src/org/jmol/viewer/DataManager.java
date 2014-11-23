/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-03 20:53:36 -0500 (Wed, 03 Oct 2007) $
 * $Revision: 8351 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development Team
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
package org.jmol.viewer;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.jmol.constant.EnumVdw;
import org.jmol.script.T;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.SB;


/*
 * a class for storing and retrieving user data,
 * including atom-related and color-related data
 * 
 */

class DataManager {

  Map<String, Object[]> dataValues = new Hashtable<String, Object[]>();

  Viewer viewer;
  DataManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void clear() {
    dataValues.clear();
  }
  
  void setData(String type, Object[] data, int arrayCount, int actualAtomCount,
               int matchField, int matchFieldColumnCount, int field,
               int fieldColumnCount) {
    //Eval
    /*
     * data[0] -- label
     * data[1] -- string or float[] or float[][] or float[][][]
     * data[2] -- selection bitset or int[] atomMap when field > 0
     * data[3] -- arrayDepth 0(String),1(float[]),2,3(float[][][])
     * 
     * matchField = data must match atomNo in this column, >= 1
     * field = column containing the data, >= 1:
     *   0 ==> values are a simple list; clear the data
     *   Integer.MAX_VALUE ==> values are a simple list; don't clear the data
     *   Integer.MIN_VALUE ==> one SINGLE data value should be used for all selected atoms
     */
    if (type == null) {
      clear();
      return;
    }
    type = type.toLowerCase();
    if (type.equals("element_vdw")) {
      String stringData = ((String) data[1]).trim();
      if (stringData.length() == 0) {
        userVdwMars = null;
        userVdws = null;
        bsUserVdws = null;
        return;
      }
      if (bsUserVdws == null)
        setUserVdw(defaultVdw);
      Parser.parseFloatArrayFromMatchAndField(stringData, bsUserVdws, 1, 0,
          (int[]) data[2], 2, 0, userVdws, 1);
      for (int i = userVdws.length; --i >= 0;)
        userVdwMars[i] = (int) Math.floor(userVdws[i] * 1000);
      return;
    }
    if (data[2] != null && arrayCount > 0) {
      boolean createNew = (matchField != 0 || field != Integer.MIN_VALUE
          && field != Integer.MAX_VALUE);
      Object[] oldData = dataValues.get(type);
      BS bs;
      float[] f = (oldData == null || createNew ? new float[actualAtomCount]
          : ArrayUtil.ensureLengthA(((float[]) oldData[1]), actualAtomCount));

      // check to see if the data COULD be interpreted as a string of float values
      // and if so, do that. This pre-fetches the tokens in that case.

      int depth = ((Integer)data[3]).intValue();
      String stringData = (depth == 0 ? (String) data[1] : null);
      float[] floatData = (depth == 1 ? (float[]) data[1] : null);
      String[] strData = null;
      if (field == Integer.MIN_VALUE
          && (strData = Parser.getTokens(stringData)).length > 1)
        field = 0;

      if (field == Integer.MIN_VALUE) {
        // set the selected data elements to a single value
        bs = (BS) data[2];
        Parser.setSelectedFloats(Parser.parseFloatStr(stringData), bs, f);
      } else if (field == 0 || field == Integer.MAX_VALUE) {
        // just get the selected token values
        bs = (BS) data[2];
        if (floatData != null) {
          if (floatData.length == bs.cardinality())
            for (int i = bs.nextSetBit(0), pt = 0; i >= 0; i = bs
                .nextSetBit(i + 1), pt++)
              f[i] = floatData[pt];
          else
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
              f[i] = floatData[i];
        } else {
          Parser.parseFloatArrayBsData(strData == null ? Parser.getTokens(stringData)
              : strData, bs, f);
        }
      } else if (matchField <= 0) {
        // get the specified field >= 1 for the selected atoms
        bs = (BS) data[2];
        Parser.parseFloatArrayFromMatchAndField(stringData, bs, 0, 0, null,
            field, fieldColumnCount, f, 1);
      } else {
        // get the selected field, with an integer match in a specified field
        // in this case, bs is created and indicates which data points were set
        int[] iData = (int[]) data[2];
        Parser.parseFloatArrayFromMatchAndField(stringData, null, matchField,
            matchFieldColumnCount, iData, field, fieldColumnCount, f, 1);
        bs = new BS();
        for (int i = iData.length; --i >= 0;)
          if (iData[i] >= 0)
            bs.set(iData[i]);
      }
      if (oldData != null && oldData[2] instanceof BS && !createNew)
        bs.or((BS) (oldData[2]));
      data[3] = Integer.valueOf(1);
      data[2] = bs;
      data[1] = f;
      if (type.indexOf("property_atom.") == 0) {
        int tok = T.getSettableTokFromString(type = type.substring(14));
        if (tok == T.nada) {
          Logger.error("Unknown atom property: " + type);
          return;
        }
        int nValues = bs.cardinality();
        float[] fValues = new float[nValues];
        for (int n = 0, i = bs.nextSetBit(0); n < nValues; i = bs
            .nextSetBit(i + 1))
          fValues[n++] = f[i];
        viewer.setAtomProperty(bs, tok, 0, 0, null, fValues, null);
        return;
      }
    }
    dataValues.put(type, data);
  }

  Object[] getData(String type) {
    if (dataValues == null || type == null)
      return null;
    if (type.equalsIgnoreCase("types")) {
      String[] info = new String[2];
      info[0] = "types";
      info[1] = "";
      int n = 0;
      Iterator<String> e = dataValues.keySet().iterator();
      while (e.hasNext())
        info[1] += (n++ > 0 ? "\n" : "") + e.next();
      return info;
    }
    return dataValues.get(type);
  }

  float[] getDataFloatA(String label) {
    if (dataValues == null)
      return null;
    Object[] data = getData(label);
    if (data == null || ((Integer)data[3]).intValue() != 1)//!(data[1] instanceof float[]))
      return null;
    return (float[]) data[1];
  }

  float getDataFloat(String label, int atomIndex) {
    if (dataValues != null) {
      Object[] data = getData(label);
      if (data != null && ((Integer)data[3]).intValue() == 1) {
        float[] f = (float[]) data[1];
        if (atomIndex < f.length)
          return f[atomIndex];
      }
    }
    return Float.NaN;
  }

  float[][] getDataFloat2D(String label) {
    if (dataValues == null)
      return null;
    Object[] data = getData(label);
    if (data == null || ((Integer)data[3]).intValue() != 2)
      return null;
    return (float[][]) data[1];
  }

  float[][][] getDataFloat3D(String label) {
    if (dataValues == null)
      return null;
    Object[] data = getData(label);
    if (data == null || ((Integer)data[3]).intValue() != 3)
      return null;
    return (float[][][]) data[1];
  }

  void deleteModelAtoms(int firstAtomIndex, int nAtoms, BS bsDeleted) {
    if (dataValues == null)
      return;
    Iterator<String> e = dataValues.keySet().iterator();
    while (e.hasNext()) {
      String name = e.next();
      if (name.indexOf("property_") == 0) {
        Object[] obj = dataValues.get(name);
        BSUtil.deleteBits((BS) obj[2], bsDeleted);
        switch (((Integer)obj[3]).intValue()) {
        case 1:
          obj[1] = ArrayUtil.deleteElements(obj[1], firstAtomIndex, nAtoms);
          break;
        case 2:
          obj[1] = ArrayUtil.deleteElements(obj[1], firstAtomIndex, nAtoms);
          break;
        default:
          // is there anything else??
          break;
        }
      }
    }    
  }

  float[] userVdws;
  int[] userVdwMars;
  EnumVdw defaultVdw = EnumVdw.JMOL;
  BS bsUserVdws;
  
  private void setUserVdw(EnumVdw mode) {
    userVdwMars = new int[Elements.elementNumberMax];
    userVdws = new float[Elements.elementNumberMax];
    bsUserVdws = new BS();
    if (mode == EnumVdw.USER)
      mode = EnumVdw.JMOL;
    for (int i = 1; i < Elements.elementNumberMax; i++) {
      userVdwMars[i] = Elements.getVanderwaalsMar(i, mode);
      userVdws[i] = userVdwMars[i] / 1000f;
    }
  }

  void setDefaultVdw(EnumVdw type) {
    // only allowed types here are VDW_JMOL, VDW_BABEL, VDW_RASMOL, VDW_USER, VDW_AUTO
    switch (type) {
    case JMOL:
    case BABEL:
    case RASMOL:
    case AUTO:
    case USER:
      break;
    default:
      type = EnumVdw.JMOL;
    }
    if (type != defaultVdw && type == EnumVdw.USER  
        && bsUserVdws == null)
      setUserVdw(defaultVdw);
    defaultVdw = type;    
  }

  String getDefaultVdwNameOrData(int mode, EnumVdw type, BS bs) {
    // called by getDataState and via Viewer: Eval.calculate,
    // Eval.show, StateManager.getLoadState, Viewer.setDefaultVdw
    switch (mode) {
    case Integer.MIN_VALUE:
      // iMode Integer.MIN_VALUE -- just the name
      return defaultVdw.getVdwLabel();
    case Integer.MAX_VALUE:
      // iMode = Integer.MAX_VALUE -- user, only selected
      if ((bs = bsUserVdws) == null)
        return "";
      type = EnumVdw.USER;
      break;
    }
    if (type == null || type == EnumVdw.AUTO)
     type = defaultVdw;
    if (type == EnumVdw.USER && bsUserVdws == null)
      setUserVdw(defaultVdw);
    SB sb = new SB();
    sb.append(type.getVdwLabel()).append("\n");
    boolean isAll = (bs == null);
    int i0 = (isAll ? 1 : bs.nextSetBit(0));
    int i1 = (isAll ? Elements.elementNumberMax : bs.length());
    for (int i = i0; i < i1 && i >= 0; i = (isAll ? i + 1 : bs
        .nextSetBit(i + 1)))
      sb.appendI(i).appendC('\t').appendF(
          type == EnumVdw.USER ? userVdws[i] : Elements
              .getVanderwaalsMar(i, type) / 1000f).appendC('\t').append(
          Elements.elementSymbolFromNumber(i)).appendC('\n');
    return (bs == null ? sb.toString() : "\n  DATA \"element_vdw\"\n"
        + sb.append("  end \"element_vdw\";\n\n").toString());
  }

  static void getInlineData(SB loadScript, String strModel, boolean isAppend, String loadFilter) {
    String tag = (isAppend ? "append" : "model") + " inline";
    loadScript.append("load /*data*/ data \"").append(tag).append("\"\n")
        .append(strModel).append("end \"").append(tag)
        .append(loadFilter == null || loadFilter.length() == 0 ? "" : " filter" + Escape.eS(loadFilter))
        .append("\";");
  }
}
