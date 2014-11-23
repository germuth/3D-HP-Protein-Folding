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

package org.jmol.render;

import org.jmol.modelset.Measurement;
import org.jmol.modelset.MeasurementPending;
import org.jmol.shape.Measures;
import org.jmol.util.AxisAngle4f;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.Matrix3f;
import org.jmol.util.P3;
import org.jmol.util.Point3fi;
import org.jmol.util.P3i;


public class MeasuresRenderer extends FontLineShapeRenderer {

  private Measurement measurement;
  private boolean doJustify;
  @Override
  protected boolean render() {
    if (!g3d.checkTranslucent(false))
      return false;
    Measures measures = (Measures) shape;
    doJustify = viewer.getJustifyMeasurements();
    mad = measures.mad;
    // note that this COULD be screen pixels if <= 20. 
    imageFontScaling = viewer.getImageFontScaling();
    font3d = g3d.getFont3DScaled(measures.font3d, imageFontScaling);
    renderPendingMeasurement(measures.measurementPending);
    if (!viewer.getShowMeasurements())
      return false;
    //clearBox();
    boolean showMeasurementLabels = viewer.getShowMeasurementLabels();
    boolean dynamicMeasurements = viewer.getDynamicMeasurements();
    measures.setVisibilityInfo();
    for (int i = measures.measurementCount; --i >= 0;) {
      Measurement m = measures.measurements.get(i);
      if (dynamicMeasurements || m.isDynamic)
        m.refresh();
      if (!m.isVisible)
        continue;
      colix = m.colix;
      if (colix == 0)
        colix = measures.colix;
      if (colix == 0)
        colix = viewer.getColixBackgroundContrast();
      g3d.setColix(colix);
      renderMeasurement(m.getCount(), m, showMeasurementLabels);
    }
    return false;
  }

  private Point3fi getAtom(int i) {
    Point3fi a = measurement.getAtom(i);
    if (a.screenDiameter < 0) {
      viewer.transformPtScr(a, pt0);
      a.screenX = pt0.x;
      a.screenY = pt0.y;
      a.screenZ = pt0.z;
    }
    return a;
  }
  
  private void renderMeasurement(int count, Measurement measurement,
                                 boolean renderLabel) {
    this.measurement = measurement;
    switch (count) {
    case 1:
      if (measurement.traceX != Integer.MIN_VALUE) {
        atomA = getAtom(1);
        drawLine(atomA.screenX, atomA.screenY, atomA.screenZ,
            measurement.traceX, measurement.traceY, atomA.screenZ, mad);
      }
      break;
    case 2:
      atomA = getAtom(1);
      atomB = getAtom(2);
      renderDistance(renderLabel);
      break;
    case 3:
      atomA = getAtom(1);
      atomB = getAtom(2);
      atomC = getAtom(3);
      renderAngle(renderLabel);
      break;
    case 4:
      atomA = getAtom(1);
      atomB = getAtom(2);
      atomC = getAtom(3);
      atomD = getAtom(4);
      renderTorsion(renderLabel);
      break;
    }
    atomA = atomB = atomC = atomD = null;
  }

  void renderDistance(boolean renderLabel) {
    tickInfo = measurement.getTickInfo();
    if (tickInfo != null) {
      drawLine(atomA.screenX, atomA.screenY, atomA.screenZ, 
          atomB.screenX, atomB.screenY, atomB.screenZ, mad);
      if (tickInfo != null)
        drawTicks(atomA, atomB, mad, renderLabel);
      return;
    }
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int radius = drawLine(atomA.screenX, atomA.screenY, zA, atomB.screenX,
        atomB.screenY, zB, mad);
    if (!renderLabel)
      return;
    String s = measurement.getString();
    if (s == null || s.length() == 0)
      return;
    if (mad > 0)
      radius <<= 1;
    int z = (zA + zB) / 2;
    if (z < 1)
      z = 1;
    int x = (atomA.screenX + atomB.screenX) / 2;
    int y = (atomA.screenY + atomB.screenY) / 2;
    drawString(x, y, z, radius, doJustify
        && (x - atomA.screenX) * (y - atomA.screenY) > 0, false, false,
        (doJustify ? 0 : Integer.MAX_VALUE), s);
  }
                           

  private AxisAngle4f aaT = new AxisAngle4f();
  private Matrix3f matrixT = new Matrix3f();

  private void renderAngle(boolean renderLabel) {
    int zOffset = atomB.screenDiameter + 10;
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - zOffset;
    int zC = atomC.screenZ - atomC.screenDiameter - 10;
    int radius = drawLine(atomA.screenX, atomA.screenY, zA,
                             atomB.screenX, atomB.screenY, zB, mad);
    radius += drawLine(atomB.screenX, atomB.screenY, zB,
                          atomC.screenX, atomC.screenY, zC, mad);    
    if (!renderLabel)
      return;
    radius = (radius + 1) / 2;

    AxisAngle4f aa = measurement.getAxisAngle();
    if (aa == null) { // 180 degrees
      int offset = (int) Math.floor(5 * imageFontScaling);
      drawString(atomB.screenX + offset, atomB.screenY - offset,
                             zB, radius, false, false, false, 
                             (doJustify ? 0 : Integer.MAX_VALUE), 
                             measurement.getString());
      return;
    }
    int dotCount = (int)Math.floor((aa.angle / (2 * Math.PI)) * 64);
    float stepAngle = aa.angle / dotCount;
    aaT.setAA(aa);
    int iMid = dotCount / 2;
    P3 ptArc = measurement.getPointArc();
    for (int i = dotCount; --i >= 0; ) {
      aaT.angle = i * stepAngle;
      matrixT.setAA(aaT);
      pointT.setT(ptArc);
      matrixT.transform(pointT);
      pointT.add(atomB);
      // NOTE! Point3i screen is just a pointer 
      //  to viewer.transformManager.point3iScreenTemp
      P3i point3iScreenTemp = viewer.transformPt(pointT);
      int zArc = point3iScreenTemp.z - zOffset;
      if (zArc < 0) zArc = 0;
      g3d.drawPixel(point3iScreenTemp.x, point3iScreenTemp.y, zArc);
      if (i == iMid) {
        pointT.setT(ptArc);
        pointT.scale(1.1f);
        // next line modifies Point3i point3iScreenTemp
        matrixT.transform(pointT);
        pointT.add(atomB);
        viewer.transformPt(pointT);
        int zLabel = point3iScreenTemp.z - zOffset;
        drawString(point3iScreenTemp.x, 
            point3iScreenTemp.y, zLabel, radius, 
            point3iScreenTemp.x < atomB.screenX, false,
            false, 
            (doJustify ? atomB.screenY : Integer.MAX_VALUE), measurement.getString());
      }
    }
  }

  private void renderTorsion(boolean renderLabel) {
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int zC = atomC.screenZ - atomC.screenDiameter - 10;
    int zD = atomD.screenZ - atomD.screenDiameter - 10;
    int radius = drawLine(atomA.screenX, atomA.screenY, zA, atomB.screenX, atomB.screenY, zB, mad);
    radius += drawLine(atomB.screenX, atomB.screenY, zB, atomC.screenX, atomC.screenY, zC, mad);
    radius += drawLine(atomC.screenX, atomC.screenY, zC, atomD.screenX, atomD.screenY, zD, mad);
    if (!renderLabel)
      return;
    radius /= 3;
    drawString((atomA.screenX + atomB.screenX + atomC.screenX + atomD.screenX) / 4,
                           (atomA.screenY + atomB.screenY + atomC.screenY + atomD.screenY) / 4,
                           (zA + zB + zC + zD) / 4, radius, false, false,
                           false, 
                           (doJustify ? 0 : Integer.MAX_VALUE), 
                           measurement.getString());
  }

  private void renderPendingMeasurement(MeasurementPending measurementPending) {
    if (isExport || measurementPending == null)
      return;
    int count = measurementPending.getCount();
    if (count == 0)
      return;
    g3d.setColix(measurementPending.traceX == Integer.MIN_VALUE ? viewer.getColixRubberband()
        : count == 2 ? C.MAGENTA : C.GOLD);
    measurementPending.refresh();
    if (measurementPending.haveTarget())
      renderMeasurement(count, measurementPending, measurementPending.traceX == Integer.MIN_VALUE);
    else
      renderPendingWithCursor(count, measurementPending);
  }
  
  private void renderPendingWithCursor(int count, MeasurementPending measurementPending) {
    if (count > 1)
      renderMeasurement(count, measurementPending, false);
    measurement = measurementPending;
    Point3fi atomLast = getAtom(count);
    int lastZ = atomLast.screenZ - atomLast.screenDiameter - 10;
    int x = viewer.getCursorX();
    int y = viewer.getCursorY();
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    drawLine(atomLast.screenX, atomLast.screenY, lastZ, x, y, 0, mad);
  }  
 
  //TODO: I think the 20 here is the cutoff for pixels -- check this
  @Override
  protected int drawLine(int x1, int y1, int z1, int x2, int y2, int z2,
                         int mad) {
    // small numbers refer to pixels already? 
    int diameter = (mad >= 20 && exportType != GData.EXPORT_CARTESIAN ?
      viewer.scaleToScreen((z1 + z2) / 2, mad) : mad);
    return drawLine2(x1, y1, z1, x2, y2, z2, diameter);
  }
}
