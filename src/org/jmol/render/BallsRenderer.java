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


import org.jmol.modelset.Atom;
import org.jmol.util.BS;

public class BallsRenderer extends ShapeRenderer {

  @Override
  protected boolean render() {
    boolean needTranslucent = false;
    if (!viewer.getWireframeRotation() || !viewer.getInMotion()) {
      Atom[] atoms = modelSet.atoms;
      BS bsOK = viewer.getRenderableBitSet();
      for (int i = bsOK.nextSetBit(0); i >= 0; i = bsOK.nextSetBit(i + 1)) {
        Atom atom = atoms[i];
        if (atom.screenDiameter > 0
            && (atom.getShapeVisibilityFlags() & myVisibilityFlag) != 0) {
          if (g3d.setColix(atom.getColix())) {
            g3d.drawAtom(atom);
          } else {
            needTranslucent = true;
          }
        }
      }
    }
    return needTranslucent;
  }
}
