/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-08-05 16:26:58 -0500 (Sun, 05 Aug 2007) $
 * $Revision: 8032 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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


import org.jmol.constant.EnumAxesMode;
import org.jmol.modelset.TickInfo;
import org.jmol.util.JmolFont;
import org.jmol.util.GData;
import org.jmol.util.P3;
import org.jmol.util.Point3fi;
import org.jmol.util.P3i;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;

public abstract class FontLineShapeRenderer extends ShapeRenderer {

  // Axes, Bbcage, Measures, Uccage

  protected float imageFontScaling;
  protected Point3fi atomA, atomB, atomC, atomD;
  protected JmolFont font3d;

  final protected P3i pt0 = new P3i();
  final protected P3i pt1 = new P3i();
  final protected P3i pt2 = new P3i();

  final protected P3 pointT = new P3();
  final protected P3 pointT2 = new P3();
  final protected P3 pointT3 = new P3();
  final protected V3 vectorT = new V3();
  final protected V3 vectorT2 = new V3();
  final protected V3 vectorT3 = new V3();

  //final Rectangle box = new Rectangle();

  protected TickInfo tickInfo;

  protected boolean draw000 = true;

  protected byte endcap = GData.ENDCAPS_SPHERICAL;

  //protected void clearBox() {
  //  box.setBounds(0, 0, 0, 0);
  //}
  
  protected int getDiameter(int z, int madOrPixels) {
    int diameter;
    boolean isMad = (madOrPixels > 20);
    switch (exportType) {
    case GData.EXPORT_CARTESIAN:
      diameter = (isMad ? madOrPixels 
          : (int) Math.floor(viewer.unscaleToScreen(z, madOrPixels * 2) * 1000));
      break;
    default:
      if (isMad) {
        // mad
        diameter = viewer.scaleToScreen(z, madOrPixels); 
      } else {
        // pixels, and that's what we want
        if (g3d.isAntialiased())
          madOrPixels += madOrPixels;
        diameter = madOrPixels;
      }
    }
    return diameter;
  }  

  protected void renderLine(P3 p0, P3 p1, int diameter,
                            P3i pt0, P3i pt1, boolean drawTicks) {
    // used by Bbcage, Uccage, and axes
    pt0.set((int) Math.floor(p0.x), (int) Math.floor(p0.y), (int) Math.floor(p0.z));
    pt1.set((int) Math.floor(p1.x), (int) Math.floor(p1.y), (int) Math.floor(p1.z));
    if (diameter < 0)
      g3d.drawDottedLine(pt0, pt1);
    else
      g3d.fillCylinder(endcap, diameter, pt0, pt1);
    if (!drawTicks || tickInfo == null)
      return;
    // AtomA and AtomB molecular coordinates must be set previously
    atomA.screenX = pt0.x;
    atomA.screenY = pt0.y;
    atomA.screenZ = pt0.z;
    atomB.screenX = pt1.x;
    atomB.screenY = pt1.y;
    atomB.screenZ = pt1.z;
    drawTicks(atomA, atomB, diameter, true);
  }

  protected void drawTicks(Point3fi pt1, Point3fi pt2, int diameter, boolean withLabels) {
    if (Float.isNaN(tickInfo.first))
      tickInfo.first = 0;
    drawTicks2(pt1, pt2, tickInfo.ticks.x, 8, diameter, (!withLabels ? null : tickInfo.tickLabelFormats == null ? 
            new String[] { "%0.2f" } : tickInfo.tickLabelFormats));
    drawTicks2(pt1, pt2, tickInfo.ticks.y, 4, diameter, null);
    drawTicks2(pt1, pt2, tickInfo.ticks.z, 2, diameter, null);
  }

  private void drawTicks2(Point3fi ptA, Point3fi ptB, float dx, int length,
                         int diameter, String[] formats) {

    if (dx == 0)
      return;
    /*
    boolean isOut = true;
    if (dx < 0) {
      isOut = false;
      dx = -dx;
    }
    */
    if (g3d.isAntialiased())
      length *= 2;
    // perpendicular to line on screen:
    vectorT2.set(ptB.screenX, ptB.screenY, 0);
    vectorT.set(ptA.screenX, ptA.screenY, 0);
    vectorT2.sub(vectorT);
    if (vectorT2.length() < 50)
      return;

    float signFactor = tickInfo.signFactor;
    vectorT.setT(ptB);
    vectorT.sub(ptA);
    float d0 = vectorT.length();
    if (tickInfo.scale != null) {
      if (Float.isNaN(tickInfo.scale.x)) { // unitcell
        float a = viewer.getUnitCellInfo(SimpleUnitCell.INFO_A);
        if (!Float.isNaN(a))
          vectorT.set(vectorT.x / a, vectorT.y
              / viewer.getUnitCellInfo(SimpleUnitCell.INFO_B), vectorT.z
              / viewer.getUnitCellInfo(SimpleUnitCell.INFO_C));
      } else {
        vectorT.set(vectorT.x * tickInfo.scale.x, vectorT.y * tickInfo.scale.y,
            vectorT.z * tickInfo.scale.z);
      }
    }
    // d is in scaled units
    float d = vectorT.length() + 0.0001f * dx;
    if (d < dx)
      return;
    float f = dx / d * d0 / d;
    vectorT.scale(f);
    float dz = (ptB.screenZ - ptA.screenZ) / (d / dx);
    // vectorT is now the length of the spacing between ticks
    // but we may have an offset.
    d += tickInfo.first;
    float p = ((int) Math.floor(tickInfo.first / dx)) * dx - tickInfo.first;
    pointT.scaleAdd2(p / dx, vectorT, ptA);
    p += tickInfo.first;
    float z = ptA.screenZ;
    if (diameter < 0)
      diameter = 1;
    vectorT2.set(-vectorT2.y, vectorT2.x, 0);
    vectorT2.scale(length / vectorT2.length());
    P3 ptRef = tickInfo.reference; // not implemented
    if (ptRef == null) {
      pointT3.setT(viewer.getBoundBoxCenter());
      if (viewer.getAxesMode() == EnumAxesMode.BOUNDBOX) {
        pointT3.x += 1.0;
        pointT3.y += 1.0;
        pointT3.z += 1.0;
      }
    } else {
      pointT3.setT(ptRef);
    }
    viewer.transformPtScr(pointT3, pt2);
    //too annoying! float tx = vectorT2.x * ((ptA.screenX + ptB.screenX) / 2 - pt2.x);
    //float ty = vectorT2.y * ((ptA.screenY + ptB.screenY) / 2 - pt2.y);
    //if (tx + ty < -0.1)
      //vectorT2.scale(-1);
    //if (!isOut)
      //vectorT2.scale(-1);
    boolean horizontal = (Math.abs(vectorT2.x / vectorT2.y) < 0.2);
    boolean centerX = horizontal;
    boolean centerY = !horizontal;
    boolean rightJustify = !centerX && (vectorT2.x < 0);
    boolean drawLabel = (formats != null && formats.length > 0);
    int x, y;
    Object[] val = new Object[1];
    int i = (draw000 ? 0 : -1);
    while (p < d) {
      if (p >= tickInfo.first) {
        pointT2.setT(pointT);
        viewer.transformPt3f(pointT2, pointT2);
        drawLine((int) Math.floor(pointT2.x), (int) Math.floor(pointT2.y), (int) z,
            (x = (int) Math.floor(pointT2.x + vectorT2.x)),
            (y = (int) Math.floor(pointT2.y + vectorT2.y)), (int) z, diameter);
        if (drawLabel && (draw000 || p != 0)) {
          val[0] = Float.valueOf((p == 0 ? 0 : p * signFactor));
          String s = TextFormat.sprintf(formats[i % formats.length], "f", val);
          drawString(x, y, (int) z, 4, rightJustify, centerX, centerY,
              (int) Math.floor(pointT2.y), s);
        }
      }
      pointT.add(vectorT);
      p += dx;
      z += dz;
      i++;
    }
  }

  protected int drawLine(int x1, int y1, int z1, int x2, int y2, int z2,
                         int diameter) {
    return drawLine2(x1, y1, z1, x2, y2, z2, diameter);
  }

  protected int drawLine2(int x1, int y1, int z1, int x2, int y2, int z2, int diameter) {
    pt0.set(x1, y1, z1);
    pt1.set(x2, y2, z2);
    if (diameter < 0) {
      g3d.drawDashedLine(4, 2, pt0, pt1);
      return 1;
    }    
    g3d.fillCylinder(GData.ENDCAPS_FLAT, diameter, pt0, pt1);
    return (diameter + 1) / 2;
  }

  protected void drawString(int x, int y, int z, int radius,
                            boolean rightJustify, boolean centerX,
                            boolean centerY, int yRef, String sVal) {
    if (sVal == null)
      return;
    int width = font3d.stringWidth(sVal);
    int height = font3d.getAscent();
    int xT = x;
    if (rightJustify)
      xT -= radius / 2 + 2 + width;
    else if (centerX)
      xT -= radius / 2 + 2 + width / 2;
    else
      xT += radius / 2 + 2;
    int yT = y;
    if (centerY)
      yT += height / 2;
    else if (yRef == 0 || yRef < y)
      yT += height;
    else
      yT -= radius / 2;
    int zT = z - radius - 2;
    if (zT < 1)
      zT = 1;
    //if (!box.contains(xT, yT) && !box.contains(xT + width, yT)
      //  && !box.contains(xT, yT + height)
        //&& !box.contains(xT + width, yT + height)) {
      g3d.drawString(sVal, font3d, xT, yT, zT, zT, (short) 0);
   //   box.setBounds(xT, yT, width, height);
   // }
  }

}
