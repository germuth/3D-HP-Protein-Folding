/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-24 20:49:07 -0500 (Tue, 24 Apr 2007) $
 * $Revision: 7483 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sf.net
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

package org.jmol.shape;


import java.util.Hashtable;

import java.util.Map;

import org.jmol.script.T;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.JmolList;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.Measure;
import org.jmol.util.MeshSurface;
import org.jmol.util.Normix;
import org.jmol.util.P3;
import org.jmol.util.Point4f;
import org.jmol.util.Quaternion;
import org.jmol.util.SB;
import org.jmol.util.Tuple3f;
import org.jmol.util.V3;
import org.jmol.api.SymmetryInterface;

//import javax.vecmath.Matrix3f;

public class Mesh extends MeshSurface {
  
  public final static String PREVIOUS_MESH_ID = "+PREVIOUS_MESH+";

  public String[] title;
  
  public short meshColix;
  public short[] normixes;
  public JmolList<P3[]> lineData;
  public String thisID;
  public boolean isValid = true;
  public String scriptCommand;
  public String colorCommand;
  public P3 lattice;
  public boolean visible = true;
  public int lighting = T.frontlit;

  public float scale = 1;
  public boolean haveXyPoints;
  public int diameter;
  public float width;
  public P3 ptCenter = P3.new3(0,0,0);
  public Mesh linkedMesh; //for lcaoOrbitals
  public Map<String, BS> vertexColorMap;
  
  public int color;
  public SymmetryInterface unitCell;
  
  public float scale3d = 0;

  public int index;
  public int atomIndex = -1;
  public int modelIndex = -1;  // for Isosurface and Draw
  public int visibilityFlags;
  public boolean insideOut;
  public int checkByteCount;

  public void setVisibilityFlags(int n) {
    visibilityFlags = n;//set to 1 in mps
  }

  public boolean showContourLines = false;
  public boolean showPoints = false;
  public boolean drawTriangles = false;
  public boolean fillTriangles = true;
  public boolean showTriangles = false; //as distinct entitities
  public boolean frontOnly = false;
  public boolean isTwoSided = true;
  public boolean havePlanarContours = false;
  
  /**
   * 
   * @param thisID
   * @param colix
   * @param index
   */
  public Mesh(String thisID, short colix, int index) {
    if (PREVIOUS_MESH_ID.equals(thisID))
      thisID = null;
    this.thisID = thisID;
    this.colix = colix;
    this.index = index;
    //System.out.println("Mesh " + this + " constructed");
  }

  //public void finalize() {
  //  System.out.println("Mesh " + this + " finalized");
  //}
  

  public void clear(String meshType) {
    altVertices = null;
    bsDisplay = null;
    bsSlabDisplay = null;
    bsSlabGhost = null;
    cappingObject = null;
    colix = C.GOLD;
    colorDensity = false;
    connections = null;
    diameter = 0;
    drawTriangles = false;
    fillTriangles = true;
    frontOnly = false;
    havePlanarContours = false;
    haveXyPoints = false;
    isTriangleSet = false;
    isTwoSided = false;
    lattice = null;
    mat4 = null;
    normixes = null;
    scale3d = 0;
    polygonIndexes = null;
    scale = 1;
    showContourLines = false;
    showPoints = false;
    showTriangles = false; //as distinct entities
    slabbingObject = null;
    slabOptions = null;
    title = null;
    unitCell = null;
    vertexCount0 = polygonCount0 = vertexCount = polygonCount = 0;
    vertices = null;
    spanningVectors = null;    
    this.meshType = meshType;
  }

  private BS bsTemp;
  
  public void initialize(int lighting, P3[] vertices, Point4f plane) {
    if (vertices == null)
      vertices = this.vertices;
    V3[] normals = getNormals(vertices, plane);
    setNormixes(normals);
    this.lighting = T.frontlit;
    if (insideOut)
      invertNormixes();
    setLighting(lighting);
  }

  public void setNormixes(V3[] normals) {
    normixes = new short[normixCount];
    if (bsTemp == null)
      bsTemp = Normix.newVertexBitSet();
    if (haveXyPoints)
      for (int i = normixCount; --i >= 0;)
        normixes[i] = Normix.NORMIX_NULL;
    else
      for (int i = normixCount; --i >= 0;)
        normixes[i] = Normix.getNormixV(normals[i], bsTemp);
  }

  public V3[] getNormals(P3[] vertices, Point4f plane) {
    normixCount = (isTriangleSet ? polygonCount : vertexCount);
    V3[] normals = new V3[normixCount];
    for (int i = normixCount; --i >= 0;)
      normals[i] = new V3();
    if (plane == null) {
      sumVertexNormals(vertices, normals);
    }else {
      V3 normal = V3.new3(plane.x, plane.y, plane.z); 
      for (int i = normixCount; --i >= 0;)
        normals[i] = normal;
    }
    if (!isTriangleSet)
      for (int i = normixCount; --i >= 0;) {
        normals[i].normalize();
      }
    return normals;
  }
  
  public void setLighting(int lighting) {
    isTwoSided = (lighting == T.fullylit);
    if (lighting == this.lighting)
      return;
    flipLighting(this.lighting);
    flipLighting(this.lighting = lighting);
  }
  
  private void flipLighting(int lighting) {
    if (lighting == T.fullylit) // this will not be a WebGL option
      for (int i = normixCount; --i >= 0;)
        normixes[i] = (short)~normixes[i];
    else if ((lighting == T.frontlit) == insideOut)
      invertNormixes();
  }

  private void invertNormixes() {
    Normix.setInverseNormixes();
    for (int i = normixCount; --i >= 0;)
      normixes[i] = Normix.getInverseNormix(normixes[i]);
  }

  public void setTranslucent(boolean isTranslucent, float iLevel) {
    colix = C.getColixTranslucent3(colix, isTranslucent, iLevel);
  }

  public final V3 vAB = new V3();
  public final V3 vAC = new V3();
  public final V3 vTemp = new V3();

  //public Vector data1;
  //public Vector data2;
  //public JmolList<Object> xmlProperties;
  public boolean colorDensity;
  public Object cappingObject;
  public Object slabbingObject;

  public int[] connections;

  public boolean recalcAltVertices;
  
  protected void sumVertexNormals(P3[] vertices, V3[] normals) {
    sumVertexNormals2(vertices, normals);
  }

  protected void sumVertexNormals2(P3[] vertices, V3[] normals) {
    // subclassed in IsosurfaceMesh
    int adjustment = checkByteCount;
    float min = getMinDistance2ForVertexGrouping();
    for (int i = polygonCount; --i >= 0;) {
      try {
        if (!setABC(i))
          continue;
        P3 vA = vertices[iA];
        P3 vB = vertices[iB];
        P3 vC = vertices[iC];
        // no skinny triangles
        if (vA.distanceSquared(vB) < min || vB.distanceSquared(vC) < min
            || vA.distanceSquared(vC) < min)
          continue;
        Measure.calcNormalizedNormal(vA, vB, vC, vTemp, vAB, vAC);
        if (isTriangleSet) {
          normals[i].setT(vTemp);
          continue;
        }
        float l = vTemp.length();
        if (l > 0.9 && l < 1.1) // test for not infinity or -infinity or isNaN
          for (int j = polygonIndexes[i].length - adjustment; --j >= 0;) {
            int k = polygonIndexes[i][j];
            normals[k].add(vTemp);
          }
      } catch (Exception e) {
        System.out.println(e);
      }
    }
  }

  protected float getMinDistance2ForVertexGrouping() {
    return 1e-8f; // different for an isosurface
  }

  public String getState(String type) {
    //String sxml = null; // problem here is that it can be WAY to large. Shape.getXmlPropertyString(xmlProperties, type);
    SB s = new SB();
    //if (sxml != null)
      //s.append("/** XML ** ").append(sxml).append(" ** XML **/\n");
    s.append(type);
    if (!type.equals("mo"))
      s.append(" ID ").append(Escape.eS(thisID));
    if (lattice != null)
      s.append(" lattice ").append(Escape.eP(lattice));
    if (meshColix != 0)
      s.append(" color mesh ").append(C.getHexCode(meshColix));
    s.append(getRendering());
    if (!visible)
      s.append(" hidden");
    if (bsDisplay != null) {
      s.append(";\n  ").append(type);
      if (!type.equals("mo"))
        s.append(" ID ").append(Escape.eS(thisID));
      s.append(" display " + Escape.e(bsDisplay));
    }
    return s.toString();
  }

  protected String getRendering() {
    SB s = new SB();
    s.append(fillTriangles ? " fill" : " noFill");
    s.append(drawTriangles ? " mesh" : " noMesh");
    s.append(showPoints ? " dots" : " noDots");
    s.append(frontOnly ? " frontOnly" : " notFrontOnly");
    if (showContourLines)
      s.append(" contourlines");
    if (showTriangles)
      s.append(" triangles");
    s.append(" ").append(T.nameOf(lighting));
    return s.toString();
  }

  public P3[] getOffsetVertices(Point4f thePlane) {
    if (altVertices != null && !recalcAltVertices)
      return (P3[]) altVertices;
    altVertices = new P3[vertexCount];
    for (int i = 0; i < vertexCount; i++)
      altVertices[i] = P3.newP(vertices[i]);
    V3 normal = null;
    float val = 0;
    if (scale3d != 0 && vertexValues != null && thePlane != null) {
        normal = V3.new3(thePlane.x, thePlane.y, thePlane.z);
        normal.normalize();
        normal.scale(scale3d);
        if (mat4 != null) {
          Matrix3f m3 = new Matrix3f();
          mat4.getRotationScale(m3); 
          m3.transform(normal);
        }
    }
    for (int i = 0; i < vertexCount; i++) {
      if (vertexValues != null && Float.isNaN(val = vertexValues[i]))
        continue;
      if (mat4 != null)
        mat4.transform((P3) altVertices[i]);
      P3 pt = (P3) altVertices[i];
      if (normal != null && val != 0)
        pt.scaleAdd2(val, normal, pt);
    }
    
    initialize(lighting, (P3[]) altVertices, null);
    recalcAltVertices = false;
    return (P3[]) altVertices;
  }

  /**
   * 
   * @param showWithinPoints
   * @param showWithinDistance2
   * @param isWithinNot
   */
  public void setShowWithin(JmolList<P3> showWithinPoints,
                            float showWithinDistance2, boolean isWithinNot) {
    if (showWithinPoints.size() == 0) {
      bsDisplay = (isWithinNot ? BSUtil.newBitSet2(0, vertexCount) : null);
      return;
    }
    bsDisplay = new BS();
    for (int i = 0; i < vertexCount; i++)
      if (checkWithin(vertices[i], showWithinPoints, showWithinDistance2, isWithinNot))
        bsDisplay.set(i);
  }

  public static boolean checkWithin(P3 pti, JmolList<P3> withinPoints,
                                    float withinDistance2, boolean isWithinNot) {
    if (withinPoints.size() != 0)
      for (int i = withinPoints.size(); --i >= 0;)
        if (pti.distanceSquared(withinPoints.get(i)) <= withinDistance2)
          return !isWithinNot;
    return isWithinNot;
  }

  public int getVertexIndexFromNumber(int vertexIndex) {
    if (--vertexIndex < 0)
      vertexIndex = vertexCount + vertexIndex;
    return (vertexCount <= vertexIndex ? vertexCount - 1
        : vertexIndex < 0 ? 0 : vertexIndex);
  }

  public BS getVisibleVertexBitSet() {
    return getVisibleVBS();
  }

  protected BS getVisibleVBS() {
    BS bs = new BS();
    if (polygonCount == 0 && bsSlabDisplay != null)
      BSUtil.copy2(bsSlabDisplay, bs);
    else
      for (int i = polygonCount; --i >= 0;)
        if (bsSlabDisplay == null || bsSlabDisplay.get(i)) {
          int[] vertexIndexes = polygonIndexes[i];
          if (vertexIndexes == null)
            continue;
          bs.set(vertexIndexes[0]);
          bs.set(vertexIndexes[1]);
          bs.set(vertexIndexes[2]);
        }
    return bs;
  }

  BS getVisibleGhostBitSet() {
    BS bs = new BS();
    if (polygonCount == 0 && bsSlabGhost != null)
      BSUtil.copy2(bsSlabGhost, bs);
    else
      for (int i = polygonCount; --i >= 0;)
        if (bsSlabGhost == null || bsSlabGhost.get(i)) {
          int[] vertexIndexes = polygonIndexes[i];
          if (vertexIndexes == null)
            continue;
          bs.set(vertexIndexes[0]);
          bs.set(vertexIndexes[1]);
          bs.set(vertexIndexes[2]);
        }
    return bs;
  }

  public void setTokenProperty(int tokProp, boolean bProp) {
    switch (tokProp) {
    case T.notfrontonly:
    case T.frontonly:
      frontOnly = (tokProp == T.frontonly ? bProp : !bProp);
      return;
    case T.frontlit:
    case T.backlit:
    case T.fullylit:
      setLighting(tokProp);
      return;
    case T.nodots:
    case T.dots:
      showPoints =  (tokProp == T.dots ? bProp : !bProp);
      return;
    case T.nomesh:
    case T.mesh:
      drawTriangles =  (tokProp == T.mesh ? bProp : !bProp);
      return;
    case T.nofill:
    case T.fill:
      fillTriangles =  (tokProp == T.fill ? bProp : !bProp);
      return;
    case T.notriangles:
    case T.triangles:
      showTriangles =  (tokProp == T.triangles ? bProp : !bProp);
      return;
    case T.nocontourlines:
    case T.contourlines:
      showContourLines =  (tokProp == T.contourlines ? bProp : !bProp);
      return;
    }
  }
  
  Object getInfo(boolean isAll) {
    Hashtable<String, Object> info = new Hashtable<String, Object>();
    info.put("id", thisID);
    info.put("vertexCount", Integer.valueOf(vertexCount));
    info.put("polygonCount", Integer.valueOf(polygonCount));
    info.put("haveQuads", Boolean.valueOf(haveQuads));
    info.put("haveValues", Boolean.valueOf(vertexValues != null));
    if (vertexCount > 0 && isAll)
      info.put("vertices", ArrayUtil.arrayCopyPt(vertices, vertexCount));
    if (vertexValues != null && isAll)
      info.put("vertexValues", ArrayUtil.arrayCopyF(vertexValues, vertexCount));
    if (polygonCount > 0 && isAll)
      info.put("polygons", ArrayUtil.arrayCopyII(polygonIndexes, polygonCount));
    return info;
  }

  public P3[] getBoundingBox() {
    return null;
  }

  public SymmetryInterface getUnitCell() {
    // isosurface only
    return null;
  }

  public void rotateTranslate(Quaternion q, Tuple3f offset, boolean isAbsolute) {
    if (q == null && offset == null) {
      mat4 = null;
      return;
    }
    Matrix3f m3 = new Matrix3f();
    V3 v = new V3();
    if (mat4 == null) {
      mat4 = new Matrix4f();
      mat4.setIdentity();
    }
    mat4.getRotationScale(m3);
    mat4.get(v);
    if (q == null) {
      if (isAbsolute)
        v.setT(offset);
      else
        v.add(offset);
    } else {
      m3.mul(q.getMatrix());
    }
    mat4 = Matrix4f.newMV(m3, v);
    recalcAltVertices = true;
  }

  public V3[] getNormalsTemp() {
    return (normalsTemp == null ? (normalsTemp = getNormals(vertices, null))
        : normalsTemp);
  }

}
