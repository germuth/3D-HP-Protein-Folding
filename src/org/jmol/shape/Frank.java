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

package org.jmol.shape;

import org.jmol.i18n.GT;
import org.jmol.util.BS;
import org.jmol.util.JmolFont;


public class Frank extends FontShape {

  // Axes, Bbcage, Frank, Uccage

  final static String defaultFontName = "SansSerif";
  final static String defaultFontStyle = "Plain";
  final static int defaultFontSize = 16;
  public final static int frankMargin = 4;

  public String frankString = "Jmol";
  JmolFont currentMetricsFont3d;
  JmolFont baseFont3d;
  public int frankWidth;
  public int frankAscent;
  public int frankDescent;
  int x, y, dx, dy;

  @Override
  public void initShape() {
    super.initShape();
    myType = "frank";
    baseFont3d = font3d = gdata.getFont3DFSS(defaultFontName, defaultFontStyle, defaultFontSize);
    calcMetrics();
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    setPropFS(propertyName, value);
  }
  
  @Override
  public boolean wasClicked(int x, int y) {
    int width = viewer.getScreenWidth();
    int height = viewer.getScreenHeight();
    return (width > 0 && height > 0 
        && x > width - frankWidth - frankMargin
        && y > height - frankAscent - frankMargin);
  }

  @Override
  public boolean checkObjectHovered(int x, int y, BS bsVisible) {
    if (!viewer.getShowFrank() || !wasClicked(x, y) || !viewer.menuEnabled())
      return false;
    if (gdata.isDisplayAntialiased() && !viewer.isSingleThreaded) {
      //because hover rendering is done in FIRST pass only
      x <<= 1;
      y <<= 1;
    }      
    viewer.hoverOnPt(x, y, GT._("Click for menu..."), null, null);
    return true;
  }
  
  void calcMetrics() {
    if (viewer.isJS2D || viewer.isJS3D)
      frankString = "JSmol";
    else if (viewer.isSignedApplet())
      frankString = "Jmol_S";
    if (font3d == currentMetricsFont3d) 
      return;
    currentMetricsFont3d = font3d;
    frankWidth = font3d.stringWidth(frankString);
    frankDescent = font3d.getDescent();
    frankAscent = font3d.getAscent();
  }

  public void getFont(float imageFontScaling) {
    font3d = gdata.getFont3DScaled(baseFont3d, imageFontScaling);
    calcMetrics();
  }
  
  @Override
  public String getShapeState() {
    return viewer.getFontState(myType, font3d);
  }
  
}
