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
 */
package org.jmol.render;

import java.util.Iterator;


import org.jmol.modelset.Atom;
import org.jmol.shape.Echo;
import org.jmol.shape.Object2d;
import org.jmol.shape.Text;
import org.jmol.util.C;
import org.jmol.util.P3i;

public class EchoRenderer extends ShapeRenderer {

  float imageFontScaling;
  Atom ptAtom;
  P3i pt = new P3i();

  @Override
  protected boolean render() {
    if (viewer.isPreviewOnly())
      return false;
    Echo echo = (Echo) shape;
    Iterator<Text> e = echo.objects.values().iterator();
    float scalePixelsPerMicron = (viewer.getFontScaling() ? viewer
        .getScalePixelsPerAngstrom(true) * 10000 : 0);
    imageFontScaling = viewer.getImageFontScaling();
    boolean haveTranslucent = false;
    while (e.hasNext()) {
      Text t = e.next();
      if (!t.visible || t.hidden) {
        continue;
      }
      if (t.valign == Object2d.VALIGN_XYZ) {
        viewer.transformPtScr(t.xyz, pt);
        t.setXYZs(pt.x, pt.y, pt.z, pt.z);
      } else if (t.movableZPercent != Integer.MAX_VALUE) {
        int z = viewer.zValueFromPercent(t.movableZPercent);
        t.setZs(z, z);
      }
      TextRenderer.render(t, g3d, scalePixelsPerMicron, imageFontScaling,
          false, null);
      if (C.isColixTranslucent(t.bgcolix) || C.isColixTranslucent(t.colix))
        haveTranslucent = true;
    }
    if (!isExport) {
      String frameTitle = viewer.getFrameTitle();
      if (frameTitle != null && frameTitle.length() > 0) {
        if (g3d.setColix(viewer.getColixBackgroundContrast())) {
          if (frameTitle.indexOf("%{") >= 0 || frameTitle.indexOf("@{") >= 0)
            frameTitle = viewer.formatText(frameTitle);
          renderFrameTitle(frameTitle);
        }
      }
    }
    return haveTranslucent;
  }
  
  private void renderFrameTitle(String frameTitle) {
    byte fid = g3d.getFontFidFS("Serif", 14 * imageFontScaling);
    g3d.setFontFid(fid);
    int y = (int) Math.floor(viewer.getScreenHeight() * (g3d.isAntialiased() ? 2 : 1) - 10 * imageFontScaling);
    int x = (int) Math.floor(5 * imageFontScaling);
    g3d.drawStringNoSlab(frameTitle, null, x, y, 0, (short) 0);
  }
}
