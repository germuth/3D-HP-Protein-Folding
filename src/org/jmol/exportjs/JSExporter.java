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


import java.util.Hashtable;
import java.util.Map;


import org.jmol.util.BS;
import org.jmol.util.GData;
import org.jmol.util.J2SRequireImport;
import org.jmol.util.JmolFont;
import org.jmol.util.P3;
import org.jmol.util.Tuple3f;
import org.jmol.util.V3;

@J2SRequireImport( { org.jmol.exportjs.Exporter.class,
    org.jmol.exportjs.CartesianExporter.class,
    org.jmol.exportjs.Export3D.class })
public class JSExporter extends CartesianExporter {

  private UseTable useTable;

  public JSExporter() {
  }

  private Map<String, Boolean> htSpheresRendered = new Hashtable<String, Boolean>();

  private Map<String, Object[]> htObjects = new Hashtable<String, Object[]>();

  Object applet;

  @Override
  protected void outputHeader() {
    // note -- not compatible yet with multiple applets.
    /**
     * @j2sNative
     * 
     * this.applet = this.viewer.applet
     * 
     */
    {}
    useTable = new UseTable();
    htSpheresRendered.clear(); 
    htObjects.clear();
    /**
     * @j2sNative
     * 
     * this.jsInitExport(this.applet);
     * 
     */
    {
    // implemented in JavaScript only
    }
  }

  @Override
  protected void outputFooter() {
    /**
     * @j2sNative
     * 
     * this.jsEndExport(this.applet);
     * 
     */
    {
    // implemented in JavaScript only
    }
    htSpheresRendered.clear(); 
    htObjects.clear();
    useTable = null;
  }

  private void jsSphere(Object applet, String id, boolean isNew, P3 pt, Object[] o) {
    // implemented in JavaScript only
    System.out.println(applet + " " + id + " " + isNew + " " + pt + " " + o);
  }

  private void jsCylinder(Object applet, String id, boolean isNew, P3 pt1, P3 pt2,
                           Object[] o) {
    // implemented in JavaScript only
    System.out.println(applet + " " + id + " " + isNew + " " + pt1 + " " + pt2 + " " + o);
  }

  /**
   * @param applet 
   * @param color  
   * @param pt1 
   * @param pt2 
   * @param pt3 
   */
  void jsTriangle(Object applet, int color, P3 pt1, P3 pt2, P3 pt3) {
    System.out.println("jsTriangle ");
  }

  @Override
  protected void outputSphere(P3 ptCenter, float radius, short colix,
                              boolean checkRadius) {
    int iRad = Math.round(radius * 100);
    String check = round(ptCenter) + (checkRadius ? " " + iRad : "");
    if (htSpheresRendered.get(check) != null)
      return;
    htSpheresRendered.put(check, Boolean.TRUE);
    boolean found = useTable.getDef("S" + colix + "_" + iRad, ret);
    Object[] o;
    if (found)
      o = htObjects.get(ret[0]);
    else
      htObjects.put(ret[0], o = new Object[] { getColor(colix),
          Float.valueOf(radius) });
    jsSphere(applet, ret[0], !found, ptCenter, o);
  }

  private String[] ret = new String[1];

  @Override
  protected boolean outputCylinder(P3 ptCenter, P3 pt1, P3 pt2,
                                   short colix, byte endcaps, float radius,
                                   P3 ptX, P3 ptY, boolean checkRadius) {
    // ptX and ptY are ellipse major and minor axes
    // not implemented yet
    if (ptX != null)
      return false;
    float length = pt1.distance(pt2);
    boolean found = useTable.getDef("C" + colix + "_" + Math.round(length * 100)
        + "_" + radius + "_" + endcaps, ret);
    Object[] o;
    if (found)
      o = htObjects.get(ret[0]);
    else
      htObjects.put(ret[0], o = new Object[] { getColor(colix),
          Float.valueOf(length), Float.valueOf(radius) });
    jsCylinder(applet, ret[0], !found, pt1, pt2, o);
    return true;
  }

  @Override
  protected void outputCircle(P3 pt1, P3 pt2, float radius,
                              short colix, boolean doFill) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void outputCone(P3 ptBase, P3 ptTip, float radius,
                            short colix) {
    outputCylinder(null, ptBase, ptTip, colix, GData.ENDCAPS_NONE, radius, null, null, false);
  }

  @Override
  protected void outputEllipsoid(P3 center, P3[] points, short colix) {
    // TODO Auto-generated method stub

  }

  private Integer getColor(short colix) {
    return Integer.valueOf(g3d.getColorArgbOrGray(colix));
  }

  @Override
  protected void outputSurface(P3[] vertices, V3[] normals,
                               short[] vertexColixes, int[][] indices,
                               short[] polygonColixes,
                               int nVertices, int nPolygons, int nFaces, BS bsPolygons,
                               int faceVertexMax, short colix, P3 offset) {
    int[] vertexColors = getColors(vertexColixes);
    int[] polygonColors = getColors(polygonColixes);
    jsSurface(applet, vertices, normals, indices, nVertices, nPolygons, nFaces, bsPolygons, 
        faceVertexMax, g3d.getColorArgbOrGray(colix), vertexColors, polygonColors);
  }
  
  /**
   * @param applet 
   * @param vertices  
   * @param normals 
   * @param indices 
   * @param nVertices 
   * @param nPolygons 
   * @param nFaces 
   * @param bsPolygons 
   * @param faceVertexMax 
   * @param color 
   * @param vertexColors 
   * @param polygonColors 
   */
  protected void jsSurface(Object applet, P3[] vertices, V3[] normals,
                         int[][] indices, int nVertices, int nPolygons,
                         int nFaces, BS bsPolygons, int faceVertexMax,
                         int color, int[] vertexColors, int[] polygonColors) {
      System.out.println("jsSurface -- nV=" + nVertices + " nPoly=" + nPolygons + " nFaces=" + nFaces + " faceVertexMax=" + faceVertexMax);
    // JavaScript only    
  }

  private int[] getColors(short[] colixes) {
    if (colixes == null)
      return null;
    int[] colors = new int[colixes.length];
    for (int i = colors.length; --i >= 0;) {
      colors[i] = g3d.getColorArgbOrGray(colixes[i]);
    }
    return colors;
  }

  @Override
  protected void outputTriangle(P3 pt1, P3 pt2, P3 pt3,
                                short colix) {
    jsTriangle(applet, g3d.getColorArgbOrGray(colix), pt1, pt2, pt3);
  }

  @Override
  protected void output(Tuple3f pt) {
    // TODO Auto-generated method stub

  }

  @Override
  void plotText(int x, int y, int z, short colix, String text, JmolFont font3d) {
    // TODO -- not sure how to handle z exactly. 
    // These are screen coordinates. You have to use
    // viewer.unTransformPoint(pointScreen, pointAngstroms) 
    // to return that to actual coordinates.
  }

}


