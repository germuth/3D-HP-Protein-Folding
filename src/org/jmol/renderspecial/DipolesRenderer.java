/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-16 15:52:18 -0600 (Thu, 16 Mar 2006) $
 * $Revision: 4635 $
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.renderspecial;


import org.jmol.render.ShapeRenderer;
import org.jmol.shapespecial.Dipole;
import org.jmol.shapespecial.Dipoles;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.V3;

public class DipolesRenderer extends ShapeRenderer {

  private float dipoleVectorScale;

  @Override
  protected boolean render() {
    Dipoles dipoles = (Dipoles) shape;
    dipoleVectorScale = viewer.getDipoleScale();
    boolean needTranslucent = false;
    for (int i = dipoles.dipoleCount; --i >= 0;) {
      Dipole dipole = dipoles.dipoles[i];
      if (dipole.visibilityFlags != 0 && transform(dipole) && renderDipoleVector(dipole))
        needTranslucent = true;
    }
    return needTranslucent;
  }

  private final V3 offset = new V3();
  private final P3i[] screens = new P3i[6];
  private final P3[] points = new P3[6];
  {
    for (int i = 0; i < 6; i++) {
      screens[i] = new P3i();
      points[i] = new P3();
    }
  }
  private P3 cross0 = new P3();
  private P3 cross1 = new P3();
  
  private final static int cylinderBase = 0;
  private final static int cross = 1;
  private final static int crossEnd = 2;
  private final static int center = 3;
  private final static int arrowHeadBase = 4;
  private final static int arrowHeadTip = 5;

  private int diameter;
  private int headWidthPixels;
  private int crossWidthPixels;

  private final static float arrowHeadOffset = 0.9f;
  private final static float arrowHeadWidthFactor = 2f;
  private final static float crossOffset = 0.1f;
  private final static float crossWidth = 0.04f;

  private boolean transform(Dipole dipole) {
    V3 vector = dipole.vector;
    offset.setT(vector);
    if (dipole.center == null) {
      offset.scale(dipole.offsetAngstroms / dipole.dipoleValue);
      if (dipoleVectorScale < 0)
        offset.add(vector);
      points[cylinderBase].setT(dipole.origin);
      points[cylinderBase].add(offset);
    } else {
      offset.scale(-0.5f * dipoleVectorScale);
      points[cylinderBase].setT(dipole.center);
      points[cylinderBase].add(offset);
      if (dipole.offsetAngstroms != 0) {
        offset.setT(vector);
        offset.scale(dipole.offsetAngstroms / dipole.dipoleValue);
        points[cylinderBase].add(offset);
      }
    }

    points[cross].scaleAdd2(dipoleVectorScale * crossOffset, vector,
        points[cylinderBase]);
    points[crossEnd].scaleAdd2(dipoleVectorScale * (crossOffset + crossWidth),
        vector, points[cylinderBase]);
    points[center]
        .scaleAdd2(dipoleVectorScale / 2, vector, points[cylinderBase]);
    points[arrowHeadBase].scaleAdd2(dipoleVectorScale * arrowHeadOffset, vector,
        points[cylinderBase]);
    points[arrowHeadTip].scaleAdd2(dipoleVectorScale, vector,
        points[cylinderBase]);

    if (dipole.atoms[0] != null
        && modelSet.isAtomHidden(dipole.atoms[0].getIndex()))
      return false;
    offset.setT(points[center]);
    offset.cross(offset, vector);
    if (offset.length() == 0) {
      offset.set(points[center].x + 0.2345f, points[center].y + 0.1234f,
          points[center].z + 0.4321f);
      offset.cross(offset, vector);
    }
    offset.scale(dipole.offsetSide / offset.length());
    for (int i = 0; i < 6; i++)
      points[i].add(offset);
    for (int i = 0; i < 6; i++)
      viewer.transformPtScr(points[i], screens[i]);
    viewer.transformPt3f(points[cross], cross0);
    viewer.transformPt3f(points[crossEnd], cross1);
    mad = dipole.mad;
    diameter = viewer.scaleToScreen(screens[center].z, mad);
    headWidthPixels = (int) Math.floor(diameter * arrowHeadWidthFactor);
    if (headWidthPixels < diameter + 5)
      headWidthPixels = diameter + 5;
    crossWidthPixels = headWidthPixels;
    return true;
  }

  private boolean renderDipoleVector(Dipole dipole) {
    short colixA = (dipole.bond == null ? dipole.colix : C
        .getColixInherited(dipole.colix, dipole.bond.colix));
    short colixB = colixA;
    if (dipole.atoms[0] != null) {
      colixA = C.getColixInherited(colixA, dipole.atoms[0].getColix());
      colixB = C.getColixInherited(colixB, dipole.atoms[1].getColix());
    }
    if (colixA == 0)
      colixA = C.ORANGE;
    if (colixB == 0)
      colixB = C.ORANGE;
    if (dipoleVectorScale < 0) {
      short c = colixA;
      colixA = colixB;
      colixB = c;
    }
    colix = colixA;
    if (colix == colixB) {
      if (!g3d.setColix(colix))
        return true;
      g3d.fillCylinder(GData.ENDCAPS_OPEN, diameter,
          screens[cylinderBase], screens[arrowHeadBase]);
      if (!dipole.noCross)
        g3d.fillCylinderBits(GData.ENDCAPS_FLAT, crossWidthPixels, cross0,
            cross1);
      g3d.fillConeScreen(GData.ENDCAPS_FLAT, headWidthPixels,
          screens[arrowHeadBase], screens[arrowHeadTip], false);
      return false;
    }
    boolean needTranslucent = false;
    if (g3d.setColix(colix)) {
      g3d.fillCylinder(GData.ENDCAPS_OPEN, diameter,
          screens[cylinderBase], screens[center]);
      if (!dipole.noCross)
        g3d.fillCylinderBits(GData.ENDCAPS_FLAT, crossWidthPixels, cross0,
            cross1);
    } else {
      needTranslucent = true;
    }
    colix = colixB;
    if (g3d.setColix(colix)) {
      g3d.fillCylinder(GData.ENDCAPS_OPENEND, diameter, screens[center],
          screens[arrowHeadBase]);
      g3d.fillConeScreen(GData.ENDCAPS_FLAT, headWidthPixels,
          screens[arrowHeadBase], screens[arrowHeadTip], false);
    } else {
      needTranslucent = true;
    }
    return needTranslucent;
  }
 
 }
