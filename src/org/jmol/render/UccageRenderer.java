/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

//import java.text.NumberFormat;

import org.jmol.api.SymmetryInterface;
import org.jmol.shape.Uccage;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.P3;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.TextFormat;
import org.jmol.viewer.StateManager;

public class UccageRenderer extends CageRenderer {

  //NumberFormat nf;
  byte fid;
  //boolean doLocalize;
  
  @Override
  protected void setEdges() {
    tickEdges = BoxInfo.uccageTickEdges;    
  }

  private final P3[] verticesT = new P3[8];  
  {
    for (int i = 8; --i >= 0; ) {
      verticesT[i] = new P3();
    }
  }

  @Override
  protected void initRenderer() {
    super.initRenderer();
    draw000 = false;
  }
  
  @Override
  protected boolean render() {
    imageFontScaling = viewer.getImageFontScaling();
    font3d = g3d.getFont3DScaled(((Uccage) shape).font3d, imageFontScaling);
    int mad = viewer.getObjectMad(StateManager.OBJ_UNITCELL);
    if (mad == 0 || viewer.isJmolDataFrame() || viewer.isNavigating()
        && viewer.getNavigationPeriodic())
      return false;
    colix = viewer.getObjectColix(StateManager.OBJ_UNITCELL);
    boolean needTranslucent = C.isColixTranslucent(colix);
    if (!isExport && needTranslucent != g3d.isPass2())
      return needTranslucent;
    //doLocalize = viewer.getUseNumberLocalization();
    render1(mad);
    return false;
  }

  private P3 fset0 = P3.new3(555,555,1);
  private P3 cell0 = new P3();
  private P3 cell1 = new P3();
  private P3 offset = new P3();
  private P3 offsetT = new P3();
  
  private void render1(int mad) {
    g3d.setColix(colix);
    SymmetryInterface unitcell = viewer.getCurrentUnitCell();
    if (unitcell == null)
      return;
    isPolymer = unitcell.isPolymer();
    isSlab = unitcell.isSlab();
    P3[] vertices = unitcell.getUnitCellVertices();
    offset.setT(unitcell.getCartesianOffset());
    P3 fset = unitcell.getUnitCellMultiplier();
    boolean haveMultiple = (fset != null);
    if (!haveMultiple) 
      fset = fset0;

    SimpleUnitCell.ijkToPoint3f((int) fset.x, cell0, 0);
    SimpleUnitCell.ijkToPoint3f((int) fset.y, cell1, 1);
    int firstLine, allow0, allow1;
    if (fset.z < 0) {
      cell0.scale (-1/fset.z);
      cell1.scale (-1/fset.z);
    }
    P3[] axisPoints = viewer.getAxisPoints();
    boolean drawAllLines = (viewer.getObjectMad(StateManager.OBJ_AXIS1) == 0
        || viewer.getAxesScale() < 2 || axisPoints == null);
    P3[] aPoints = axisPoints;
    for (int x = (int) cell0.x; x < cell1.x; x++) {
      for (int y = (int) cell0.y; y < cell1.y; y++) {
        for (int z = (int) cell0.z; z < cell1.z; z++) {
          if (haveMultiple) {
            offsetT.set(x, y, z);
            offsetT.scale(Math.abs(fset.z));
            unitcell.toCartesian(offsetT, true);
            offsetT.add(offset);
            aPoints = (x == 0 && y == 0 && z == 0 ? axisPoints : null);
            firstLine = (drawAllLines || aPoints == null ? 0 : 3);
            allow0 = 0xFF;
            allow1 = 0xFF;            
          } else {
            offsetT.setT(offset);
            firstLine = (drawAllLines ? 0 : 3);
            allow0 = 0xFF;
            allow1 = 0xFF;
          }
          for (int i = 8; --i >= 0;)
            verticesT[i].add2(vertices[i], offsetT);
          render(mad, verticesT, aPoints, firstLine, allow0, allow1, Math.abs(fset.z));
        }
      }
    }

    if (viewer.getDisplayCellParameters() && !viewer.isPreviewOnly()
        && !unitcell.isPeriodic())
      renderInfo(unitcell);
  }
  
  private String nfformat(float x) {
    return (/*doLocalize && nf != null ? nf.format(x) :*/ TextFormat.formatDecimal(x, 3));
  }

  private void renderInfo(SymmetryInterface symmetry) {
    if (isExport
        || !g3d.setColix(viewer.getColixBackgroundContrast()))
      return;
    fid = g3d.getFontFidFS("Monospaced", 14 * imageFontScaling);

//    if (nf == null) {
//      nf = NumberFormat.getInstance();
//    }
//    if (nf != null) {
//      nf.setMaximumFractionDigits(3);
//      nf.setMinimumFractionDigits(3);
//    }
    g3d.setFontFid(fid);

    int lineheight = (int) Math.floor(15 * imageFontScaling);
    int x = (int) Math.floor(5 * imageFontScaling);
    int y = lineheight;

    String spaceGroup = symmetry.getSpaceGroupName();
    if (isPolymer)
      spaceGroup = "polymer";
    else if (isSlab)
      spaceGroup = "slab";
    if (spaceGroup != null & !spaceGroup.equals("-- [--]")) {
      y += lineheight;
      g3d.drawStringNoSlab(spaceGroup, null, x, y, 0, (short) 0);
    }
    y += lineheight;
    g3d.drawStringNoSlab("a="
        + nfformat(symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_A)) + "\u00C5",
        null, x, y, 0, (short) 0);
    if (!isPolymer) {
      y += lineheight;
      g3d.drawStringNoSlab(
          "b=" + nfformat(symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_B))
              + "\u00C5", null, x, y, 0, (short) 0);
    }
    if (!isPolymer && !isSlab) {
      y += lineheight;
      g3d.drawStringNoSlab(
          "c=" + nfformat(symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_C))
              + "\u00C5", null, x, y, 0, (short) 0);
    }
    //if (nf != null)
      //nf.setMaximumFractionDigits(1);
    if (!isPolymer) {
      if (!isSlab) {
        y += lineheight;
        g3d.drawStringNoSlab("\u03B1="
            + nfformat(symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_ALPHA))
            + "\u00B0", null, x, y, 0, (short) 0);
        y += lineheight;
        g3d.drawStringNoSlab("\u03B2="
            + nfformat(symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_BETA))
            + "\u00B0", null, x, y, 0, (short) 0);
      }
      y += lineheight;
      g3d.drawStringNoSlab("\u03B3="
          + nfformat(symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_GAMMA))
          + "\u00B0", null, x, y, 0, (short) 0);
    }
  }

}

