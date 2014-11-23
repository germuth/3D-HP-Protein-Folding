/* $RCSfile$
 * $Author: migueljmol $
 * $Date: 2006-03-25 09:27:43 -0600 (Sat, 25 Mar 2006) $
 * $Revision: 4696 $

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

import org.jmol.modelset.Atom;
import org.jmol.shape.Halos;
import org.jmol.util.BS;
import org.jmol.util.C;
import org.jmol.viewer.JC;


public class HalosRenderer extends ShapeRenderer {

  boolean isAntialiased;
  @Override
  protected boolean render() {
    Halos halos = (Halos) shape;
    boolean selectDisplayTrue = viewer.getSelectionHaloEnabled(true);
    boolean showHiddenSelections = (selectDisplayTrue && viewer
        .getShowHiddenSelectionHalos());
    if (halos.mads == null && halos.bsHighlight == null && !selectDisplayTrue)
      return false;
    isAntialiased = g3d.isAntialiased();
    Atom[] atoms = modelSet.atoms;
    BS bsSelected = (selectDisplayTrue ? viewer.getSelectionSet(false) : null);
    boolean needTranslucent = false;
    for (int i = modelSet.getAtomCount(); --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.getShapeVisibilityFlags() & JC.ATOM_IN_FRAME) == 0)
        continue;
      boolean isHidden = modelSet.isAtomHidden(i);
      mad = (halos.mads == null ? 0 : halos.mads[i]);
      colix = (halos.colixes == null || i >= halos.colixes.length ? C.INHERIT_ALL
          : halos.colixes[i]);
      if (selectDisplayTrue && bsSelected.get(i)) {
        if (isHidden && !showHiddenSelections)
          continue;
        if (mad == 0)
          mad = -1; // unsized
        if (colix == C.INHERIT_ALL)
          colix = halos.colixSelection;
        if (colix == C.USE_PALETTE)
          colix = C.GOLD;
        else if (colix == C.INHERIT_ALL)
          colix = C.getColixInherited(colix, atom.getColix());
      } else if (isHidden) {
        continue;
      } else {
        colix = C.getColixInherited(colix, atom.getColix());
      }
      if (mad != 0) {
        if (render1(atom))
          needTranslucent = true;
      }
      if (!isHidden && halos.bsHighlight != null && halos.bsHighlight.get(i)) {
        mad = -2;
        colix = halos.colixHighlight;
        if (render1(atom))
          needTranslucent = true;
      }       
    }
    return needTranslucent;
  }

  boolean render1(Atom atom) {
    short colixFill = (mad == -2 ? 0 : C.getColixTranslucent3(colix, true, 0.5f));
    boolean needTranslucent = (mad != -2);
    if (!g3d.setColix(colix)) {
      needTranslucent = true;
      colix = 0;
      if (colixFill == 0 || !g3d.setColix(colixFill))
        return needTranslucent;      
    }
    int z = atom.screenZ;
    int diameter = mad;
    if (diameter < 0) { //unsized selection
      diameter = atom.screenDiameter;
      if (diameter == 0) {
        float ellipsemax = atom.getADPMinMax(true);
        if (ellipsemax > 0)
          diameter = viewer.scaleToScreen(z, (int) Math.floor(ellipsemax * 2000));
        if (diameter == 0) {
          diameter = viewer.scaleToScreen(z, mad == -2 ? 250 : 500);
        }
      }
    } else {
      diameter = viewer.scaleToScreen(z, mad);
    }
    float d = diameter;
//    System.out.println(atom + "scaleToScreen(" + z + "," + mad +")=" + diameter);
    if (isAntialiased)
      d /= 2;
    float more = (d / 2);
    if (mad == -2)
      more /= 2;
    if (more < 8)
      more = 8;
    if (more > 20)
      more = 20;
    d += more;
    if (isAntialiased)
      d *= 2;
    if (d < 1)
      return false;
    g3d.drawFilledCircle(colix, colixFill, (int) Math.floor(d),
        atom.screenX, atom.screenY, atom.screenZ);
    return needTranslucent;
  }  
}
