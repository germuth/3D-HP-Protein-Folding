/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-11 14:30:16 -0500 (Sun, 11 Mar 2007) $
 * $Revision: 7068 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.modelsetbio.ProteinStructure;
import org.jmol.script.T;
import org.jmol.shapebio.BioShape;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.P3;
import org.jmol.util.P3i;


public class CartoonRenderer extends RocketsRenderer {

  private boolean newRockets = true;
  private boolean renderAsRockets;
  private boolean renderEdges;
  
  @Override
  protected void renderBioShape(BioShape bioShape) {
    if (bioShape.wingVectors == null || isCarbohydrate)
      return;
    getScreenControlPoints();
    if (isNucleic) {
      renderNucleic();
      return;
    }
    boolean val = viewer.getCartoonFlag(T.cartoonrockets);
    if (renderAsRockets != val) {
      bioShape.falsifyMesh();
      renderAsRockets = val;
    }
    val = !viewer.getCartoonFlag(T.rocketbarrels);
    if (renderArrowHeads != val) {
      bioShape.falsifyMesh();
      renderArrowHeads = val;
    }
    ribbonTopScreens = calcScreens(0.5f);
    ribbonBottomScreens = calcScreens(-0.5f);
    calcRopeMidPoints(newRockets);
    if (!renderArrowHeads) {
      calcScreenControlPoints(cordMidPoints);
      controlPoints = cordMidPoints;
    }
    render1();
    viewer.freeTempPoints(cordMidPoints);
    viewer.freeTempScreens(ribbonTopScreens);
    viewer.freeTempScreens(ribbonBottomScreens);
  }

  P3i ptConnectScr = new P3i();
  P3 ptConnect = new P3();
  void renderNucleic() {
    renderEdges = viewer.getCartoonFlag(T.cartoonbaseedges);
    boolean isTraceAlpha = viewer.getTraceAlpha();
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1)) {
      if (isTraceAlpha) {
        ptConnectScr.set(
            (controlPointScreens[i].x + controlPointScreens[i + 1].x) / 2,
            (controlPointScreens[i].y + controlPointScreens[i + 1].y) / 2,
            (controlPointScreens[i].z + controlPointScreens[i + 1].z) / 2);
        ptConnect.setT(controlPoints[i]);
        ptConnect.scale(0.5f);
        ptConnect.scaleAdd2(0.5f, controlPoints[i + 1], ptConnect);
      } else {
        ptConnectScr.setT(controlPointScreens[i + 1]);
        ptConnect.setT(controlPoints[i + 1]);
      }
      renderHermiteConic(i, false);
      colix = getLeadColix(i);
      if (setBioColix(colix))
        renderNucleicBaseStep((NucleicMonomer) monomers[i], mads[i], ptConnectScr, ptConnect);
    }
  }

  @Override
  protected void render1() {
    boolean lastWasSheet = false;
    boolean lastWasHelix = false;
    ProteinStructure previousStructure = null;
    ProteinStructure thisStructure;

    // Key structures that must render properly
    // include 1crn and 7hvp

    // this loop goes monomerCount --> 0, because
    // we want to hit the heads first

    for (int i = monomerCount; --i >= 0;) {
      // runs backwards, so it can render the heads first
      thisStructure = monomers[i].getProteinStructure();
      if (thisStructure != previousStructure) {
        if (renderAsRockets)
          lastWasHelix = false;
        lastWasSheet = false;
      }
      previousStructure = thisStructure;
      boolean isHelix = isHelix(i);
      boolean isSheet = isSheet(i);
      boolean isHelixRocket = (renderAsRockets || !renderArrowHeads ? isHelix : false);
      if (bsVisible.get(i)) {
        if (isHelixRocket) {
          //next pass
        } else if (isSheet || isHelix) {
          if (lastWasSheet && isSheet || lastWasHelix && isHelix) {
            //uses topScreens
            renderHermiteRibbon(true, i, true);
          } else {
            renderHermiteArrowHead(i);
          }
        } else {
          renderHermiteConic(i, true);
        }
      }
      lastWasSheet = isSheet;
      lastWasHelix = isHelix;
    }
    if (renderAsRockets || !renderArrowHeads)
      renderRockets();
  }

  private void renderRockets() {
    // doing the cylinders separately because we want to connect them if we can.

    // Key structures that must render properly
    // include 1crn and 7hvp

    // this loop goes 0 --> monomerCount, because
    // the special segments routine takes care of heads
    tPending = false;
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1))
      if (isHelix(i))
        renderSpecialSegment(monomers[i], getLeadColix(i), mads[i]);
    renderPending();
  }
  
  //// nucleic acid base rendering
  
  private final P3[] ring6Points = new P3[6];
  private final P3i[] ring6Screens = new P3i[6];
  private final P3[] ring5Points = new P3[5];
  private final P3i[] ring5Screens = new P3i[5];

  {
    ring6Screens[5] = new P3i();
    for (int i = 5; --i >= 0; ) {
      ring5Screens[i] = new P3i();
      ring6Screens[i] = new P3i();
    }
  }

  private void renderNucleicBaseStep(NucleicMonomer nucleotide,
                             short thisMad, P3i backboneScreen, P3 ptConnect) {
    if (renderEdges) {
      renderLeontisWesthofEdges(nucleotide, thisMad);
      return;
    }
    nucleotide.getBaseRing6Points(ring6Points);
    viewer.transformPoints(ring6Points, ring6Screens);
    renderRing6();
    boolean hasRing5 = nucleotide.maybeGetBaseRing5Points(ring5Points);
    P3i stepScreen;
    P3 stepPt;
    if (hasRing5) {
      viewer.transformPoints(ring5Points, ring5Screens);
      renderRing5();
      stepScreen = ring5Screens[3];//was 2
      stepPt = ring5Points[3];
    } else {
      stepScreen = ring6Screens[2];//was 1
      stepPt = ring6Points[2];
    }
    mad = (short) (thisMad > 1 ? thisMad / 2 : thisMad);
    g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL,
                     viewer.scaleToScreen(backboneScreen.z,
                                          mad),
                     backboneScreen, stepScreen, ptConnect, stepPt, mad / 2000f);
    --ring6Screens[5].z;
    for (int i = 5; --i >= 0; ) {
      --ring6Screens[i].z;
      if (hasRing5)
        --ring5Screens[i].z;
    }
    for (int i = 6; --i > 0; )
      g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3,
                       ring6Screens[i], ring6Screens[i - 1], ring6Points[i], ring6Points[i - 1], 0.005f);
    if (hasRing5) {
      for (int i = 5; --i > 0; )
        g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3,
                         ring5Screens[i], ring5Screens[i - 1], ring5Points[i], ring5Points[i - 1], 0.005f);
    } else {
      g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3,
                       ring6Screens[5], ring6Screens[0], ring6Points[5], ring6Points[0], 0.005f);
    }
  }

  private void renderLeontisWesthofEdges(NucleicMonomer nucleotide,
                                         short thisMad) {
    //                Nasalean L, Strombaugh J, Zirbel CL, and Leontis NB in 
    //                Non-Protein Coding RNAs, 
    //                Nils G. Walter, Sarah A. Woodson, Robert T. Batey, Eds.
    //                Chapter 1, p 6.
    // http://books.google.com/books?hl=en&lr=&id=se5JVEqO11AC&oi=fnd&pg=PR11&dq=Non-Protein+Coding+RNAs&ots=3uTkn7m3DA&sig=6LzQREmSdSoZ6yNrQ15zjYREFNE#v=onepage&q&f=false

    if (!nucleotide.getEdgePoints(ring6Points))
      return;
    viewer.transformPoints(ring6Points, ring6Screens);
    renderTriangle();
    mad = (short) (thisMad > 1 ? thisMad / 2 : thisMad);
    g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3, ring6Screens[0],
        ring6Screens[1], ring6Points[0], ring6Points[1], 0.005f);
    g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3, ring6Screens[1],
        ring6Screens[2], ring6Points[1], ring6Points[2], 0.005f);
    boolean isTranslucent = C.isColixTranslucent(colix);
    float tl = C.getColixTranslucencyLevel(colix);
    short colixSugarEdge = C.getColixTranslucent3(C.RED, isTranslucent,
        tl);
    short colixWatsonCrickEdge = C.getColixTranslucent3(C.GREEN,
        isTranslucent, tl);
    short colixHoogsteenEdge = C.getColixTranslucent3(C.BLUE,
        isTranslucent, tl);
    g3d.setColix(colixSugarEdge);
    g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3, ring6Screens[2],
        ring6Screens[3], ring6Points[2], ring6Points[3], 0.005f);
    g3d.setColix(colixWatsonCrickEdge);
    g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3, ring6Screens[3],
        ring6Screens[4], ring6Points[3], ring6Points[4], 0.005f);
    g3d.setColix(colixHoogsteenEdge);
    g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3, ring6Screens[4],
        ring6Screens[5], ring6Points[4], ring6Points[5], 0.005f);
  }

  private void renderTriangle() {
    g3d.setNoisySurfaceShade(ring6Screens[2], ring6Screens[3], ring6Screens[4]);
    g3d.fillTriangle3i(ring6Screens[2], ring6Screens[3], ring6Screens[4], ring6Points[2], ring6Points[3], ring6Points[4]);
  }

  private void renderRing6() {
    g3d.setNoisySurfaceShade(ring6Screens[0], ring6Screens[2], ring6Screens[4]);
    g3d.fillTriangle3i(ring6Screens[0], ring6Screens[2], ring6Screens[4], ring6Points[0], ring6Points[2], ring6Points[4]);
    g3d.fillTriangle3i(ring6Screens[0], ring6Screens[1], ring6Screens[2], ring6Points[0], ring6Points[1], ring6Points[2]);
    g3d.fillTriangle3i(ring6Screens[0], ring6Screens[4], ring6Screens[5], ring6Points[0], ring6Points[4], ring6Points[5]);
    g3d.fillTriangle3i(ring6Screens[2], ring6Screens[3], ring6Screens[4], ring6Points[2], ring6Points[3], ring6Points[4]); 
  }

  private void renderRing5() {
    // shade was calculated previously by renderRing6();
    g3d.fillTriangle3i(ring5Screens[0], ring5Screens[2], ring5Screens[3], ring5Points[0], ring5Points[2], ring5Points[3]);
    g3d.fillTriangle3i(ring5Screens[0], ring5Screens[1], ring5Screens[2], ring5Points[0], ring5Points[1], ring5Points[2]);
    g3d.fillTriangle3i(ring5Screens[0], ring5Screens[3], ring5Screens[4], ring5Points[0], ring5Points[3], ring5Points[4]);
  }  
  
}
