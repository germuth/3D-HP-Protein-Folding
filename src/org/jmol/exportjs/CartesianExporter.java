/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 15:41:42 -0500 (Fri, 18 May 2007) $
 * $Revision: 7752 $

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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.exportjs;

//import java.awt.Image;
import java.util.Hashtable;

import java.util.Map;


import org.jmol.modelset.Atom;
import org.jmol.util.AxisAngle4f;
import org.jmol.util.BS;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.JmolList;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.Tuple3f;

/*
 * for programs that use the standard 3D coordinates.
 * 
 */
abstract public class CartesianExporter extends Exporter {

  public CartesianExporter() {
    exportType = GData.EXPORT_CARTESIAN;
    
    lineWidthMad = 100;
  }

  protected AxisAngle4f viewpoint = new AxisAngle4f();

  protected P3 getModelCenter() {
    // "center" is the center of rotation, not
    // necessary the screen center or the center of the model. 
    // When the user uses ALT-CTRL-drag, Jmol is applying an 
    // XY screen translation AFTER the matrix transformation. 
    // Apparently, that's unusual in this business. 
    // (The rotation center is generally directly
    // in front of the observer -- not allowing, for example,
    // holding the model in one's hand at waist level and rotating it.)

    // But there are good reasons to do it the Jmol way. If you don't, then
    // what happens is that the distortion pans over the moving model
    // and you get an odd lens effect rather than the desired smooth
    // panning. So we must approximate.

    return referenceCenter;
  }

  protected P3 getCameraPosition() {

    // used for VRML/X3D only

    P3 ptCamera = new P3();
    P3 pt = P3.new3(screenWidth / 2, screenHeight / 2, 0);
    viewer.unTransformPoint(pt, ptCamera);
    ptCamera.sub(center);
    // this is NOT QUITE correct when the model has been shifted with CTRL-ALT
    // because in that case the center of distortion is not the screen center,
    // and these simpler perspective models don't allow for that.
    tempP3.set(screenWidth / 2, screenHeight / 2, cameraDistance
        * scalePixelsPerAngstrom);
    viewer.unTransformPoint(tempP3, tempP3);
    tempP3.sub(center);
    ptCamera.add(tempP3);

    //System.out.println(ptCamera + " " + cameraPosition);
    //  return ptCamera;

    return cameraPosition;

  }

  private void setTempPoints(P3 ptA, P3 ptB, boolean isCartesian) {
    if (isCartesian) {
      // really first order -- but actual coord
      tempP1.setT(ptA);
      tempP2.setT(ptB);
    } else {
      viewer.unTransformPoint(ptA, tempP1);
      viewer.unTransformPoint(ptB, tempP2);
    }
  }

  protected int getCoordinateMap(Tuple3f[] vertices, int[] coordMap, BS bsValid) {
    int n = 0;
    for (int i = 0; i < coordMap.length; i++) {
      if (bsValid != null && !bsValid.get(i) || Float.isNaN(vertices[i].x)) {
        if (bsValid != null)
          bsValid.clear(i);
        continue;
      }
      coordMap[i] = n++;
    }
    return n;
  }

  protected int[] getNormalMap(Tuple3f[] normals, int nNormals,
                               BS bsValid, JmolList<String> vNormals) {
    Map<String, Integer> htNormals = new Hashtable<String, Integer>();
    int[] normalMap = new int[nNormals];
    for (int i = 0; i < nNormals; i++) {
      String s;
      if (bsValid != null && !bsValid.get(i) || Float.isNaN(normals[i].x)){
        if (bsValid != null)
          bsValid.clear(i);
        continue;
      }
      s = getTriad(normals[i]) + "\n";
      if (htNormals.containsKey(s)) {
        normalMap[i] = htNormals.get(s).intValue();
      } else {
        normalMap[i] = vNormals.size();
        vNormals.addLast(s);
        htNormals.put(s, Integer.valueOf(normalMap[i]));
      }
    }
    return normalMap;
  }

  protected void outputIndices(int[][] indices, int[] map, int nPolygons,
                               BS bsPolygons, int faceVertexMax) {
    boolean isAll = (bsPolygons == null);
    int i0 = (isAll ? nPolygons - 1 : bsPolygons.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsPolygons.nextSetBit(i + 1)))
      outputFace(indices[i], map, faceVertexMax);
  }

  // these are elaborated in IDTF, MAYA, VRML, or X3D:

  /**
   * @param is  
   * @param coordMap 
   * @param faceVertexMax 
   */
  protected void outputFace(int[] is, int[] coordMap, int faceVertexMax) {
    
  }

  abstract protected void outputCircle(P3 pt1, P3 pt2, float radius,
                                       short colix, boolean doFill);

  abstract protected void outputCone(P3 ptBase, P3 ptTip,
                                     float radius, short colix);

  abstract protected boolean outputCylinder(P3 ptCenter, P3 pt1,
                                            P3 pt2, short colix1,
                                            byte endcaps, float radius,
                                            P3 ptX, P3 ptY, boolean checkRadius);

  abstract protected void outputEllipsoid(P3 center, P3[] points,
                                          short colix);

  abstract protected void outputSphere(P3 ptAtom2, float f, short colix, boolean checkRadius);

  abstract protected void outputTriangle(P3 pt1, P3 pt2, P3 pt3,
                                         short colix);

  // these are called by Export3D:

  @Override
  void drawAtom(Atom atom) {
    short colix = atom.getColix();
    outputSphere(atom, atom.madAtom / 2000f, colix, C.isColixTranslucent(colix));
  }

  @Override
  void drawCircle(int x, int y, int z, int diameter, short colix, boolean doFill) {
    // draw circle
    tempP3.set(x, y, z);
    viewer.unTransformPoint(tempP3, tempP1);
    float radius = viewer.unscaleToScreen(z, diameter) / 2;
    tempP3.set(x, y, z + 1);
    viewer.unTransformPoint(tempP3, tempP3);
    outputCircle(tempP1, tempP3, radius, colix, doFill);
  }

  @Override
  boolean drawEllipse(P3 ptCenter, P3 ptX, P3 ptY, short colix,
                      boolean doFill) {
    tempV1.setT(ptX);
    tempV1.sub(ptCenter);
    tempV2.setT(ptY);
    tempV2.sub(ptCenter);
    tempV2.cross(tempV1, tempV2);
    tempV2.normalize();
    tempV2.scale(doFill ? 0.002f : 0.005f);
    tempP1.setT(ptCenter);
    tempP1.sub(tempV2);
    tempP2.setT(ptCenter);
    tempP2.add(tempV2);
    return outputCylinder(ptCenter, tempP1, tempP2, colix,
        doFill ? GData.ENDCAPS_FLAT : GData.ENDCAPS_NONE, 1.01f, ptX,
        ptY, true);
  }

  @Override
  void drawPixel(short colix, int x, int y, int z, int scale) {
    //measures, meshRibbon, dots
    tempP3.set(x, y, z);
    viewer.unTransformPoint(tempP3, tempP1);
    outputSphere(tempP1, 0.02f * scale, colix, true);
  }

  @Override
  void fillConeScreen(short colix, byte endcap, int screenDiameter,
                      P3 screenBase, P3 screenTip, boolean isBarb) {
    viewer.unTransformPoint(screenBase, tempP1);
    viewer.unTransformPoint(screenTip, tempP2);
    float radius = viewer.unscaleToScreen(screenBase.z, screenDiameter) / 2;
    if (radius < 0.05f)
      radius = 0.05f;
    outputCone(tempP1, tempP2, radius, colix);
  }

  @Override
  void drawCylinder(P3 ptA, P3 ptB, short colix1, short colix2,
                    byte endcaps, int mad, int bondOrder) {
    setTempPoints(ptA, ptB, bondOrder < 0);
    float radius = mad / 2000f;
    if (colix1 == colix2) {
      outputCylinder(null, tempP1, tempP2, colix1, endcaps, radius, null, null, bondOrder != -1);
    } else {
      tempV2.setT(tempP2);
      tempV2.add(tempP1);
      tempV2.scale(0.5f);
      tempP3.setT(tempV2);
      outputCylinder(null, tempP1, tempP3, colix1,
          (endcaps == GData.ENDCAPS_SPHERICAL ? GData.ENDCAPS_NONE
              : endcaps), radius, null, null, true);
      outputCylinder(null, tempP3, tempP2, colix2,
          (endcaps == GData.ENDCAPS_SPHERICAL ? GData.ENDCAPS_NONE
              : endcaps), radius, null, null, true);
      if (endcaps == GData.ENDCAPS_SPHERICAL) {
        outputSphere(tempP1, radius * 1.01f, colix1, bondOrder != -2);
        outputSphere(tempP2, radius * 1.01f, colix2, bondOrder != -2);
      }
    }
  }

  @Override
  void fillCylinderScreenMad(short colix, byte endcaps, int mad,
                             P3 screenA, P3 screenB) {
    float radius = mad / 2000f;
    setTempPoints(screenA, screenB, false);
    outputCylinder(null, tempP1, tempP2, colix, endcaps, radius, null, null, true);
  }

  @Override
  void fillCylinderScreen(short colix, byte endcaps, int screenDiameter,
                          P3 screenA, P3 screenB, P3 ptA, P3 ptB, float radius) {
    if (ptA != null) {
      drawCylinder(ptA, ptB, colix, colix, endcaps, Math.round(radius * 2000f), -1);
      return;
    }
    
    // vectors, polyhedra
    int mad = Math.round(viewer.unscaleToScreen((screenA.z + screenB.z) / 2,
        screenDiameter) * 1000);
    fillCylinderScreenMad(colix, endcaps, mad, screenA, screenB);
  }

  @Override
  void fillEllipsoid(P3 center, P3[] points, short colix, int x,
                     int y, int z, int diameter, Matrix3f toEllipsoidal,
                     double[] coef, Matrix4f deriv, P3i[] octantPoints) {
    outputEllipsoid(center, points, colix);
  }

  @Override
  void fillSphere(short colix, int diameter, P3 pt) {
    viewer.unTransformPoint(pt, tempP1);
    outputSphere(tempP1, viewer.unscaleToScreen(pt.z, diameter) / 2, colix, true);
  }

  @Override
  protected void fillTriangle(short colix, P3 ptA, P3 ptB,
                              P3 ptC, boolean twoSided, boolean isCartesian) {
    if (isCartesian) {
      tempP1.setT(ptA); 
      tempP2.setT(ptB); 
      tempP3.setT(ptC); 
    } else {
      viewer.unTransformPoint(ptA, tempP1);
      viewer.unTransformPoint(ptB, tempP2);
      viewer.unTransformPoint(ptC, tempP3);
    }
    outputTriangle(tempP1, tempP2, tempP3, colix);
    if (twoSided)
      outputTriangle(tempP1, tempP3, tempP2, colix);
  }

//  @Override
//  void plotImage(int x, int y, int z, Image image, short bgcolix, int width,
//                 int height) {
//    g3d.plotImage(x, y, z, image, jmolRenderer, bgcolix, width, height);
//  }

}
