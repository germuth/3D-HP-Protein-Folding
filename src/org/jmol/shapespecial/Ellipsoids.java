/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-22 07:48:05 -0500 (Tue, 22 May 2007) $
 * $Revision: 7806 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


import org.jmol.api.JmolStateCreator;
import org.jmol.modelset.Atom;
import org.jmol.shape.AtomShape;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Eigen;
import org.jmol.util.Escape;
import org.jmol.util.Matrix3f;
import org.jmol.util.P3;
import org.jmol.util.Quadric;
import org.jmol.util.SB;
import org.jmol.util.V3;


public class Ellipsoids extends AtomShape {

  public Map<String, Ellipsoid> htEllipsoids = new Hashtable<String, Ellipsoid>();
  public boolean haveEllipsoids;
  public short[][] colixset;
  byte[][] paletteIDset;
  public short[][] madset;

  public static class Ellipsoid {

    String id;
    public V3[] axes;
    public float[] lengths;
    public P3 center = P3.new3(0, 0, 0);
    double[] coef;
    public short colix = C.GOLD;
    int modelIndex;
    float scale = 1;
    public boolean visible;
    public boolean isValid;
    boolean isOn = true;

    Ellipsoid(String id, int modelIndex) {
      this.id = id;
      this.modelIndex = modelIndex;
    }

  }

  @Override
  public int getIndexFromName(String thisID) {
    return ((ellipsoid = htEllipsoids.get(thisID)) == null ? -1 : 1);
  }

  Ellipsoid ellipsoid;
  private int iSelect;

  @Override
  protected void setSize(int size, BS bsSelected) {
    setSize2(size, bsSelected);
    checkSets();
    madset[iSelect] = mads;
    for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
        .nextSetBit(i + 1)) {
      if (size != 0)
        atoms[i].scaleEllipsoid(size, iSelect);
      boolean isVisible = (madset[0] != null && madset[0].length > i && madset[0][i] > 0
          || madset[1] != null && madset[1].length > i && madset[1][i] > 0
          || madset[2] != null && madset[2].length > i && madset[2][i] > 0);
      bsSizeSet.setBitTo(i, isVisible);
      atoms[i].setShapeVisibility(myVisibilityFlag, isVisible);
    }
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if (propertyName == "thisID") {
      ellipsoid = (value == null ? null : (Ellipsoid) htEllipsoids.get(value));
      if (value == null)
        return;
      if (ellipsoid == null) {
        String id = (String) value;
        ellipsoid = new Ellipsoid(id, viewer.getCurrentModelIndex());
        htEllipsoids.put(id, ellipsoid);
        haveEllipsoids = true;
      }
      return;
    }
    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
      while (e.hasNext()) {
        Ellipsoid ellipsoid = e.next();
        if (ellipsoid.modelIndex > modelIndex)
          ellipsoid.modelIndex--;
        else if (ellipsoid.modelIndex == modelIndex)
          e.remove();
      }
      haveEllipsoids = !htEllipsoids.isEmpty();
      ellipsoid = null;
      return;
    }
    if (ellipsoid != null) {
      if ("delete" == propertyName) {
        htEllipsoids.remove(ellipsoid.id);
        haveEllipsoids = !htEllipsoids.isEmpty();
        return;
      }
      if ("modelindex" == propertyName) {
        ellipsoid.modelIndex = ((Integer) value).intValue();
        return;
      }
      if ("on" == propertyName) {
        ellipsoid.isOn = ((Boolean) value).booleanValue();
        return;
      }
      if ("atoms" == propertyName) {
        setAtoms((BS) value);
        return;
      }
      if ("points" == propertyName) {
        setPoints((Object[])value);
        return;
      }
      if ("axes" == propertyName) {
        ellipsoid.isValid = false;
        ellipsoid.axes = (V3[]) value;
        ellipsoid.lengths = new float[3];
        ellipsoid.scale = 1;
        for (int i = 0; i < 2; i++) {
          if (ellipsoid.axes[i].length() > ellipsoid.axes[i + 1].length()) {
            V3 v = ellipsoid.axes[i];
            ellipsoid.axes[i] = ellipsoid.axes[i + 1];
            ellipsoid.axes[i + 1] = v;
            if (i == 1)
              i = -1;
          }
        }
        for (int i = 0; i < 3; i++) {
          ellipsoid.lengths[i] = ellipsoid.axes[i].length();
          if (ellipsoid.lengths[i] == 0)
            return;
          ellipsoid.axes[i].normalize();
        }
        if (Math.abs(ellipsoid.axes[0].dot(ellipsoid.axes[1])) > 0.0001f
            || Math.abs(ellipsoid.axes[0].dot(ellipsoid.axes[1])) > 0.0001f
            || Math.abs(ellipsoid.axes[0].dot(ellipsoid.axes[1])) > 0.0001f)
          return;
        updateEquation(ellipsoid);
        return;
      }
      if ("equation" == propertyName) {
        ellipsoid.coef = (double[]) value;
        ellipsoid.axes = new V3[3];
        ellipsoid.lengths = new float[3];
        Quadric.getAxesForEllipsoid(ellipsoid.coef, ellipsoid.axes,
            ellipsoid.lengths);
        return;
      }
      if ("center" == propertyName) {
        ellipsoid.center = (P3) value;
        updateEquation(ellipsoid);
        return;
      }
      if ("scale" == propertyName) {
        float scale = ((Float) value).floatValue();
        if (scale <= 0 || ellipsoid.lengths == null) {
          ellipsoid.isValid = false;
        } else {
          for (int i = 0; i < 3; i++)
            ellipsoid.lengths[i] *= scale / ellipsoid.scale;
          ellipsoid.scale = scale;
          updateEquation(ellipsoid);
        }
        return;
      }
      if ("color" == propertyName) {
        ellipsoid.colix = C.getColixO(value);
        return;
      }
      if ("translucentLevel" == propertyName) {
        setPropAS(propertyName, value, bs);
        return;
      }
      if ("translucency" == propertyName) {
        boolean isTranslucent = (value.equals("translucent"));
        ellipsoid.colix = C.getColixTranslucent3(ellipsoid.colix,
            isTranslucent, translucentLevel);
        return;
      }
    }

    if ("select" == propertyName) {
      iSelect = ((Integer) value).intValue() - 1;
      checkSets();
      colixes = colixset[iSelect];
      paletteIDs = paletteIDset[iSelect];
      mads = madset[iSelect];
      return;
    }

    setPropAS(propertyName, value, bs);

    if (colixset != null) {
      if ("color" == propertyName || "translucency" == propertyName
          || "deleteModelAtoms" == propertyName) {
        colixset[iSelect] = colixes;
        paletteIDset[iSelect] = paletteIDs;
        madset[iSelect] = mads;
      }
    }
  }

  private void setPoints(Object[] data) {
    P3[] value = (P3[]) data[1];
    if (value == null)
      return;
    BS bs = (BS) data[2];
    int n = bs.cardinality();
    if (n < 3)
      return;
    P3 ptCenter = new P3();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      ptCenter.add(value[i]);
    ptCenter.scale(1.0f/n);
    ellipsoid.center = ptCenter;
    double Sxx = 0, Syy = 0, Szz = 0, Sxy = 0, Sxz = 0, Syz = 0;
    P3 pt = new P3();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      pt.setT(value[i]);
      pt.sub(ptCenter);
      Sxx += (double) pt.x * (double) pt.x;
      Syy += (double) pt.y * (double) pt.y;
      Szz += (double) pt.z * (double) pt.z;
      Sxy += (double) pt.x * (double) pt.y;
      Sxz += (double) pt.x * (double) pt.z;
      Syz += (double) pt.y * (double) pt.z;
    }
    double[][] N = new double[3][3];
    N[0][0] = Syy + Szz;
    N[0][1] = N[1][0] = -Sxy;
    N[0][2] = N[2][0] = -Sxz;
    N[1][1] = Sxx + Szz;
    N[1][2] = N[2][1] = -Syz;
    N[2][2] = Sxx + Syy;

    Eigen eigen = Eigen.newM(N);

    ellipsoid.axes = eigen.getEigenVectors3();
    double[] v = eigen.getEigenvalues();
    ellipsoid.lengths = new float[3];
    for (int i = 0; i < 3; i++)
      ellipsoid.lengths[i] = (float) v[i] / n / 3;
    ellipsoid.scale = 1;
    updateEquation(ellipsoid);
  }


  private void setAtoms(BS bs) {
    int n = bs.cardinality();
    if (n == 0)
      return;
    Atom[] atoms = viewer.modelSet.atoms;
    P3 ptCenter = new P3();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      ptCenter.add(atoms[i]);
    ptCenter.scale(1.0f/n);
    ellipsoid.center = ptCenter;
    double Sxx = 0, Syy = 0, Szz = 0, Sxy = 0, Sxz = 0, Syz = 0;
    P3 pt = new P3();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      pt.setT(atoms[i]);
      pt.sub(ptCenter);
      Sxx += (double) pt.x * (double) pt.x;
      Syy += (double) pt.y * (double) pt.y;
      Szz += (double) pt.z * (double) pt.z;
      Sxy += (double) pt.x * (double) pt.y;
      Sxz += (double) pt.x * (double) pt.z;
      Syz += (double) pt.y * (double) pt.z;
    }
    double[][] N = new double[3][3];
    N[0][0] = Syy + Szz;
    N[0][1] = N[1][0] = -Sxy;
    N[0][2] = N[2][0] = -Sxz;
    N[1][1] = Sxx + Szz;
    N[1][2] = N[2][1] = -Syz;
    N[2][2] = Sxx + Syy;

    Eigen eigen = Eigen.newM(N);

    ellipsoid.axes = eigen.getEigenVectors3();
    double[] v = eigen.getEigenvalues();
    ellipsoid.lengths = new float[3];
    for (int i = 0; i < 3; i++)
      ellipsoid.lengths[i] = (float) v[i] / n / 3;
    ellipsoid.scale = 1;
    updateEquation(ellipsoid);
  }


  private void checkSets() {
    if (colixset == null) {
      colixset = ArrayUtil.newShort2(3);
      paletteIDset = ArrayUtil.newByte2(3);
      madset = ArrayUtil.newShort2(3);
    }
  }

  private static void updateEquation(Ellipsoid ellipsoid) {
    if (ellipsoid.axes == null || ellipsoid.lengths == null)
      return;
    Matrix3f mat = new Matrix3f();
    Matrix3f mTemp = new Matrix3f();
    V3 v1 = new V3();
    ellipsoid.coef = new double[10];
    Quadric.getEquationForQuadricWithCenter(ellipsoid.center.x,
        ellipsoid.center.y, ellipsoid.center.z, mat, v1, mTemp, ellipsoid.coef,
        null);
    ellipsoid.isValid = true;
  }

  @Override
  public String getShapeState() {
    SB sb = new SB();
    getStateID(sb);
    getStateAtoms(sb);
    return sb.toString();
  }

  private void getStateID(SB sb) {
    if (!haveEllipsoids)
      return;
    Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
    V3 v1 = new V3();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      if (ellipsoid.axes == null || ellipsoid.lengths == null)
        continue;
      sb.append("  Ellipsoid ID ").append(ellipsoid.id).append(" modelIndex ")
          .appendI(ellipsoid.modelIndex).append(" center ").append(
              Escape.eP(ellipsoid.center)).append(" axes");
      for (int i = 0; i < 3; i++) {
        v1.setT(ellipsoid.axes[i]);
        v1.scale(ellipsoid.lengths[i]);
        sb.append(" ").append(Escape.eP(v1));
      }
      sb.append(" " + getColorCommandUnk("", ellipsoid.colix, translucentAllowed));
      if (!ellipsoid.isOn)
        sb.append(" off");
      sb.append(";\n");
    }
  }

  private void getStateAtoms(SB sb) {
    if (madset == null)
      return;
    JmolStateCreator sc = viewer.getStateCreator();
    if (sc == null)
      return;
    for (int ii = 0; ii < 3; ii++) {
      if (madset[ii] == null)
        continue;
      appendCmd(sb, "Ellipsoids set " + (ii + 1) + "\n");
      Map<String, BS> temp = new Hashtable<String, BS>();
      Map<String, BS> temp2 = new Hashtable<String, BS>();
      if (bsSizeSet != null)
        for (int i = bsSizeSet.nextSetBit(0); i >= 0; i = bsSizeSet
            .nextSetBit(i + 1))
          BSUtil.setMapBitSet(temp, i, i, "Ellipsoids " + madset[ii][i]);
      if (bsColixSet != null && colixset[ii] != null)
        for (int i = bsColixSet.nextSetBit(0); i >= 0; i = bsColixSet
            .nextSetBit(i + 1))
          BSUtil.setMapBitSet(temp2, i, i, getColorCommand("Ellipsoids",
              paletteIDset[ii][i], colixset[ii][i], translucentAllowed));
      sb.append(sc.getCommands(temp, temp2, "select"));
    }
  }

  @Override
  public void setVisibilityFlags(BS bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     *      
     */
    if (!haveEllipsoids)
      return;
    Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      ellipsoid.visible = ellipsoid.isOn
          && (ellipsoid.modelIndex < 0 || bs.get(ellipsoid.modelIndex));
    }
  }

}
