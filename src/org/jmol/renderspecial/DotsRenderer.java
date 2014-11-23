/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.renderspecial;

import org.jmol.modelset.Atom;
import org.jmol.render.ShapeRenderer;
import org.jmol.shapespecial.Dots;
import org.jmol.util.BS;
import org.jmol.util.C;
import org.jmol.util.Geodesic;
import org.jmol.util.V3;



public class DotsRenderer extends ShapeRenderer {

  public boolean iShowSolid;
  
  V3[] verticesTransformed;
  public int screenLevel;
  public int screenDotCount;
  public int[] screenCoordinates;
  public int[] faceMap = null; // used only by GeoSurface, but set here

  private int dotScale;
  
  @Override
  protected void initRenderer() {
    screenLevel = Dots.MAX_LEVEL;
    screenDotCount = Geodesic.getVertexCount(Dots.MAX_LEVEL);
    verticesTransformed = new V3[screenDotCount];
    for (int i = screenDotCount; --i >= 0; )
      verticesTransformed[i] = new V3();
    screenCoordinates = new int[3 * screenDotCount];
  }

  @Override
  protected boolean render() {
    Dots dots = (Dots) shape;
    render1(dots);
    return false;
  }

  protected float testRadiusAdjust;
  
  protected void render1(Dots dots) {
    //dots.timeBeginExecution = System.currentTimeMillis();
    if (!iShowSolid && !g3d.setColix(C.BLACK)) // no translucent for dots
      return;
    int sppa = (int) viewer.getScalePixelsPerAngstrom(true);
    screenLevel = (iShowSolid || sppa > 20 ? 3 : sppa > 10 ? 2 : sppa > 5 ? 1
        : 0);
    if (!iShowSolid)
      screenLevel += viewer.getDotDensity() - 3;
    screenLevel = Math.max(Math.min(screenLevel, Dots.MAX_LEVEL), 0);
    screenDotCount = Geodesic.getVertexCount(screenLevel);
    dotScale = viewer.getDotScale();
    for (int i = screenDotCount; --i >= 0;)
      viewer.transformVector(Geodesic.getVertexVector(i),
          verticesTransformed[i]);
    BS[] maps = dots.ec.getDotsConvexMaps();
    for (int i = dots.ec.getDotsConvexMax(); --i >= 0;) {
      Atom atom = modelSet.atoms[i];
      BS map = maps[i];
      if (map == null || !atom.isVisible(myVisibilityFlag)
          || !g3d.isInDisplayRange(atom.screenX, atom.screenY))
        continue;
      try {
        int nPoints = calcScreenPoints(map, dots.ec.getAppropriateRadius(i) + testRadiusAdjust,
            atom.screenX, atom.screenY, atom.screenZ);
        if (nPoints != 0)
          renderConvex(C.getColixInherited(dots.colixes[i],
              atom.getColix()), map, nPoints);
      } catch (Exception e) {
        System.out.println("Dots rendering error");
        System.out.println(e.toString());
        // ignore -- some sort of fluke
      }
    }
    //dots.timeEndExecution = System.currentTimeMillis();
    //Logger.debug("dots rendering time = "+ gs.getExecutionWalltime());
  }
  
  /**
   * calculates the screen xy coordinates for the dots or faces
   * 
   * @param visibilityMap
   * @param radius
   * @param x
   * @param y
   * @param z
   * @return number of points
   */
  private int calcScreenPoints(BS visibilityMap, float radius, int x, int y, int z) {
    int nPoints = 0;
    int i = 0;
    float scaledRadius = viewer.scaleToPerspective(z, radius);
    int iDot = Math.min(visibilityMap.size(), screenDotCount); 
    while (--iDot >= 0) {
      if (!visibilityMap.get(iDot))
        continue;
      V3 vertex = verticesTransformed[iDot];
      if (faceMap != null)
        faceMap[iDot] = i;
      screenCoordinates[i++] = x
          + Math.round (scaledRadius * vertex.x);
      screenCoordinates[i++] = y
          + Math.round(scaledRadius * vertex.y);
      screenCoordinates[i++] = z
          + Math.round(scaledRadius * vertex.z);
      ++nPoints;
    }
    return nPoints;
  }

  /**
   * generic renderer -- dots and geosurface
   * 
   * @param colix
   * @param map
   * @param nPoints
   */
  protected void renderConvex(short colix, BS map, int nPoints) {
    this.colix = C.getColixTranslucent3(colix, false, 0);
    renderDots(nPoints);
  }

  /**
   * also called by GeoSurface when in motion
   * 
   * @param nPoints
   */
  protected void renderDots(int nPoints) {
    g3d.setColix(colix);
    g3d.drawPoints(nPoints, screenCoordinates, dotScale);
  }
}

