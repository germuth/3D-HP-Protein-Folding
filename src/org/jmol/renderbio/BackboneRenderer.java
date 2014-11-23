/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-11-17 10:45:52 -0600 (Fri, 17 Nov 2006) $
 * $Revision: 6250 $

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

package org.jmol.renderbio;

import org.jmol.modelset.Atom;
import org.jmol.shapebio.BioShape;
import org.jmol.util.C;
import org.jmol.util.GData;

public class BackboneRenderer extends BioShapeRenderer {

  @Override
  protected void renderBioShape(BioShape bioShape) {
    boolean isDataFrame = viewer.isJmolDataFrameForModel(bioShape.modelIndex);
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1)) {
      Atom atomA = modelSet.atoms[leadAtomIndices[i]];
      Atom atomB = modelSet.atoms[leadAtomIndices[i + 1]];
      if (atomA.getNBackbonesDisplayed() == 0
          || atomB.getNBackbonesDisplayed() == 0
          || modelSet.isAtomHidden(atomB.getIndex()))
        continue;
      if (!isDataFrame && atomA.distance(atomB) > 10)
        continue;
      short colixA = C.getColixInherited(colixes[i], atomA.getColix());
      short colixB = C.getColixInherited(colixes[i + 1], atomB.getColix());
      if (!isExport && !isPass2) {
        boolean doA = !C.isColixTranslucent(colixA);
        boolean doB = !C.isColixTranslucent(colixB);
        if (!doA || !doB) {
          if (!doA && !doB)
            continue;
          needTranslucent = true;
        }
      }
      int xA = atomA.screenX, yA = atomA.screenY, zA = atomA.screenZ;
      int xB = atomB.screenX, yB = atomB.screenY, zB = atomB.screenZ;
      mad = mads[i];
      if (mad < 0) {
        g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
      } else {
        int width = (exportType == GData.EXPORT_CARTESIAN ? mad : viewer
            .scaleToScreen((zA + zB) / 2, mad));
        g3d.fillCylinderXYZ(colixA, colixB, GData.ENDCAPS_SPHERICAL, width, xA,
            yA, zA, xB, yB, zB);
      }
    }
  }  
}
