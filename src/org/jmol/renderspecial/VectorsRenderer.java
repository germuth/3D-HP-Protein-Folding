/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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


import org.jmol.modelset.Atom;
import org.jmol.render.ShapeRenderer;
import org.jmol.shape.Shape;
import org.jmol.shapespecial.Vectors;
import org.jmol.util.GData;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.V3;

public class VectorsRenderer extends ShapeRenderer {

  private final static float arrowHeadOffset = -0.2f;
  private final V3 vector2 = new V3();
  private final P3 pointVectorEnd = new P3();
  private final P3 pointArrowHead = new P3();
  private final P3i screenVectorEnd = new P3i();
  private final P3i screenArrowHead = new P3i();
  private final V3 headOffsetVector = new V3();
  
  private int diameter;
  //float headWidthAngstroms;
  private int headWidthPixels;
  private float vectorScale;
  private boolean vectorSymmetry;
  private float headScale;
  private boolean doShaft;


  @Override
  protected boolean render() {
    Vectors vectors = (Vectors) shape;
    if (!vectors.isActive)
      return false;
    short[] mads = vectors.mads;
    if (mads == null)
      return false;
    Atom[] atoms = vectors.atoms;
    short[] colixes = vectors.colixes;
    boolean needTranslucent = false;
    for (int i = modelSet.getAtomCount(); --i >= 0;) {
      Atom atom = atoms[i];
      if (!atom.isVisible(myVisibilityFlag))
        continue;
      V3 vibrationVector = viewer.getVibrationVector(i);
      if (vibrationVector == null)
        continue;
      vectorScale = viewer.getVectorScale();
      vectorSymmetry = viewer.getVectorSymmetry();
      if (!transform(mads[i], atom, vibrationVector))
        continue;
      if (!g3d.setColix(Shape.getColix(colixes, i, atom))) {
        needTranslucent = true;
        continue;
      }
      renderVector(atom);
      if (vectorSymmetry) {
        vector2.setT(vibrationVector);
        vector2.scale(-1);
        transform(mads[i], atom, vector2);
        renderVector(atom);
      }
    }
    return needTranslucent;
  }

  private boolean transform(short mad, Atom atom, V3 vibrationVector) {
    float len = vibrationVector.length();
    // to have the vectors move when vibration is turned on
    if (Math.abs(len * vectorScale) < 0.01)
      return false;
    headScale = arrowHeadOffset;
    if (vectorScale < 0)
      headScale = -headScale;
    doShaft = (0.1 + Math.abs(headScale/len) < Math.abs(vectorScale));
    headOffsetVector.setT(vibrationVector);
    headOffsetVector.scale(headScale / len);
    pointVectorEnd.scaleAdd2(vectorScale, vibrationVector, atom);
    pointArrowHead.setT(pointVectorEnd);
    pointArrowHead.add(headOffsetVector);
    screenArrowHead.setT(viewer.transformPtVib(pointArrowHead, vibrationVector));
    screenVectorEnd.setT(viewer.transformPtVib(pointVectorEnd, vibrationVector));
    diameter = (mad < 1 ? 1 : mad <= 20 ? mad : viewer.scaleToScreen(screenVectorEnd.z, mad));
    headWidthPixels = Math.round(diameter * 2.0f);
    if (headWidthPixels < diameter + 2)
      headWidthPixels = diameter + 2;
    return true;
  }
  
  private void renderVector(Atom atom) {
    if (doShaft)
      g3d.fillCylinderScreen(GData.ENDCAPS_OPEN, diameter, atom.screenX,
          atom.screenY, atom.screenZ, screenArrowHead.x, screenArrowHead.y,
          screenArrowHead.z);
    g3d.fillConeScreen(GData.ENDCAPS_FLAT, headWidthPixels, screenArrowHead,
        screenVectorEnd, false);
  }
}
