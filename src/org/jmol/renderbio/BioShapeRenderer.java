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

package org.jmol.renderbio;

import org.jmol.constant.EnumStructure;
import org.jmol.modelset.Atom; //import org.jmol.modelsetbio.AlphaMonomer;
import org.jmol.modelsetbio.CarbohydratePolymer;
import org.jmol.modelsetbio.Monomer;
import org.jmol.modelsetbio.NucleicPolymer; //import org.jmol.modelsetbio.ProteinStructure;
import org.jmol.render.MeshRenderer;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shapebio.BioShape;
import org.jmol.shapebio.BioShapeCollection;
import org.jmol.util.AxisAngle4f;
import org.jmol.util.BS;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.Hermite;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Normix;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.V3;

/**
   * @author Alexander Rose
   * @author Bob Hanson
   * 
 */
abstract class BioShapeRenderer extends MeshRenderer {

  //ultimately this renderer calls MeshRenderer.render1(mesh)

  private boolean invalidateMesh;
  private boolean invalidateSheets;
  private boolean isHighRes;
  private boolean isTraceAlpha;
  private boolean ribbonBorder = false;
  private boolean haveControlPointScreens;
  private float aspectRatio;
  private int hermiteLevel;
  private float sheetSmoothing;
  protected boolean cartoonFancy;


  private Mesh[] meshes;
  private boolean[] meshReady;
  private BS bsRenderMesh;


  protected int monomerCount;
  protected Monomer[] monomers;

  protected boolean isNucleic;
  protected boolean isCarbohydrate;
  protected BS bsVisible = new BS();
  protected P3i[] ribbonTopScreens;
  protected P3i[] ribbonBottomScreens;
  protected P3[] controlPoints;
  protected P3i[] controlPointScreens;

  protected int[] leadAtomIndices;
  protected V3[] wingVectors;
  protected short[] mads;
  protected short[] colixes;
  protected short[] colixesBack;
  protected EnumStructure[] structureTypes;
  
  protected boolean isPass2;

  protected abstract void renderBioShape(BioShape bioShape);

  @Override
  protected boolean render() {
    if (shape == null)
      return false;
    isPass2 = g3d.isPass2();
    invalidateMesh = false;
    needTranslucent = false;
    boolean TF = isExport || viewer.getHighResolution();
    if (TF != isHighRes)
      invalidateMesh = true;
    isHighRes = TF;

    boolean v = viewer.getCartoonFlag(T.cartoonfancy);
    if (cartoonFancy != v) {
      invalidateMesh = true;
      cartoonFancy = v;
    }
    int val1 = viewer.getHermiteLevel();
    val1 = (val1 <= 0 ? -val1 : viewer.getInMotion() ? 0 : val1);
    if (cartoonFancy)
      val1 = Math.max(val1, 3); // at least HermiteLevel 3 for "cartoonFancy"
    //else if (val1 == 0 && exportType == GData.EXPORT_CARTESIAN)
      //val1 = 5; // forces hermite for 3D exporters
    if (val1 != hermiteLevel)// && val1 != 0)
      invalidateMesh = true;
    hermiteLevel = Math.min(val1, 8);

    int val = viewer.getRibbonAspectRatio();
    val = Math.min(Math.max(0, val), 20);
    if (cartoonFancy && val >= 16)
      val = 4; // at most 4 for elliptical cartoonFancy
    if (hermiteLevel == 0)
      val = 0;

    if (val != aspectRatio && val != 0 && val1 != 0)
      invalidateMesh = true;
    aspectRatio = val;


    TF = (viewer.getTraceAlpha());
    if (TF != isTraceAlpha)
      invalidateMesh = true;
    isTraceAlpha = TF;

    invalidateSheets = false;
    float fval = viewer.getSheetSmoothing();
    if (fval != sheetSmoothing && isTraceAlpha) {
      sheetSmoothing = fval;
      invalidateMesh = true;
      invalidateSheets = true;
    }

    BioShapeCollection mps = (BioShapeCollection) shape;
    for (int c = mps.bioShapes.length; --c >= 0;) {
      BioShape bioShape = mps.getBioShape(c);
      if ((bioShape.modelVisibilityFlags & myVisibilityFlag) == 0)
        continue;
      if (bioShape.monomerCount >= 2 && initializePolymer(bioShape)) {
        bsRenderMesh.clearAll();    
        renderBioShape(bioShape);
        renderMeshes();
        freeTempArrays();
      }
    }
    return needTranslucent;
  }

  protected boolean setBioColix(short colix) {
    if (g3d.setColix(colix))
      return  true;
    needTranslucent = true;
    return false;
  }

  private void freeTempArrays() {
    if (haveControlPointScreens)
      viewer.freeTempScreens(controlPointScreens);
    viewer.freeTempEnum(structureTypes);
  }

  private boolean initializePolymer(BioShape bioShape) {
    if (viewer.isJmolDataFrameForModel(bioShape.modelIndex)) {
      controlPoints = bioShape.bioPolymer.getControlPoints(true, 0, false);
    } else {
      controlPoints = bioShape.bioPolymer.getControlPoints(isTraceAlpha,
          sheetSmoothing, invalidateSheets);
    }
    monomerCount = bioShape.monomerCount;
    bsRenderMesh = BS.newN(monomerCount);
    monomers = bioShape.monomers;
    reversed = bioShape.bioPolymer.reversed;
    leadAtomIndices = bioShape.bioPolymer.getLeadAtomIndices();

    bsVisible.clearAll();
    boolean haveVisible = false;
    if (invalidateMesh)
      bioShape.falsifyMesh();
    for (int i = monomerCount; --i >= 0;) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0
          || modelSet.isAtomHidden(leadAtomIndices[i]))
        continue;
      Atom lead = modelSet.atoms[leadAtomIndices[i]];
      if (!g3d.isInDisplayRange(lead.screenX, lead.screenY))
        continue;
      bsVisible.set(i);
      haveVisible = true;
    }
    if (!haveVisible)
      return false;
    ribbonBorder = viewer.getRibbonBorder();

    // note that we are not treating a PhosphorusPolymer
    // as nucleic because we are not calculating the wing
    // vector correctly.
    // if/when we do that then this test will become
    // isNucleic = bioShape.bioPolymer.isNucleic();

    isNucleic = bioShape.bioPolymer instanceof NucleicPolymer;
    isCarbohydrate = bioShape.bioPolymer instanceof CarbohydratePolymer;
    haveControlPointScreens = false;
    wingVectors = bioShape.wingVectors;
    meshReady = bioShape.meshReady;
    meshes = bioShape.meshes;
    mads = bioShape.mads;
    colixes = bioShape.colixes;
    colixesBack = bioShape.colixesBack;
    setStructureTypes();
    return true;
  }

  private void setStructureTypes() {
    structureTypes = viewer.allocTempEnum(monomerCount + 1);
    for (int i = monomerCount; --i >= 0;) {
      structureTypes[i] = monomers[i].getProteinStructureType();
      if (structureTypes[i] == EnumStructure.TURN)
        structureTypes[i] = EnumStructure.NONE;
    }
    structureTypes[monomerCount] = structureTypes[monomerCount - 1];
  }

  protected boolean isHelix(int i) {
    return structureTypes[i] == EnumStructure.HELIX;
  }

  protected void getScreenControlPoints() {
    calcScreenControlPoints(controlPoints);
  }

  protected void calcScreenControlPoints(P3[] points) {
    int count = monomerCount + 1;
    controlPointScreens = viewer.allocTempScreens(count);
    for (int i = count; --i >= 0;) {
      viewer.transformPtScr(points[i], controlPointScreens[i]);
    }
    haveControlPointScreens = true;
  }

  /**
   * calculate screen points based on control points and wing positions
   * (cartoon, strand, meshRibbon, and ribbon)
   * 
   * @param offsetFraction
   * @return Point3i array THAT MUST BE LATER FREED
   */
  protected P3i[] calcScreens(float offsetFraction) {
    int count = controlPoints.length;
    P3i[] screens = viewer.allocTempScreens(count);
    if (offsetFraction == 0) {
      for (int i = count; --i >= 0;)
        viewer.transformPtScr(controlPoints[i], screens[i]);
    } else {
      float offset_1000 = offsetFraction / 1000f;
      for (int i = count; --i >= 0;)
        calc1Screen(controlPoints[i], wingVectors[i],
            (mads[i] == 0 && i > 0 ? mads[i - 1] : mads[i]), offset_1000,
            screens[i]);
    }
    return screens;
  }

  private final P3 pointT = new P3();

  private void calc1Screen(P3 center, V3 vector, short mad,
                           float offset_1000, P3i screen) {
    pointT.setT(vector);
    float scale = mad * offset_1000;
    pointT.scaleAdd(scale, center);
    viewer.transformPtScr(pointT, screen);
  }

  protected short getLeadColix(int i) {
    return C.getColixInherited(colixes[i], monomers[i].getLeadAtom()
        .getColix());
  }

  protected short getLeadColixBack(int i) {
    return (colixesBack == null || colixesBack.length <= i ? 0 : colixesBack[i]);
  }

  //// cardinal hermite constant cylinder (meshRibbon, strands)

  private int iPrev, iNext, iNext2, iNext3;
  private int diameterBeg, diameterMid, diameterEnd;
  private boolean doCap0, doCap1;
  protected short colixBack;
  private BS reversed;

  private void setNeighbors(int i) {
    iPrev = Math.max(i - 1, 0);
    iNext = Math.min(i + 1, monomerCount);
    iNext2 = Math.min(i + 2, monomerCount);
    iNext3 = Math.min(i + 3, monomerCount);
  }

  /**
   * set diameters for a bioshape
   * 
   * @param i
   * @param thisTypeOnly true for Cartoon but not MeshRibbon
   * @return true if a mesh is needed
   */
  private boolean setMads(int i, boolean thisTypeOnly) {
    madMid = madBeg = madEnd = mads[i];
    if (isTraceAlpha) {
      if (!thisTypeOnly || structureTypes[i] == structureTypes[iNext]) {
        madEnd = mads[iNext];
        if (madEnd == 0) {
          if (this instanceof TraceRenderer) {
            madEnd = madBeg;
          } else {
            madEnd = madBeg;
          }
        }
        madMid = (short) ((madBeg + madEnd) >> 1);
      }
    } else {
      if (!thisTypeOnly || structureTypes[i] == structureTypes[iPrev])
        madBeg = (short) (((mads[iPrev] == 0 ? madMid : mads[iPrev]) + madMid) >> 1);
      if (!thisTypeOnly || structureTypes[i] == structureTypes[iNext])
        madEnd = (short) (((mads[iNext] == 0 ? madMid : mads[iNext]) + madMid) >> 1);
    }
    diameterBeg = viewer.scaleToScreen(controlPointScreens[i].z, madBeg);
    diameterMid = viewer.scaleToScreen(monomers[i].getLeadAtom().screenZ,
        madMid);
    diameterEnd = viewer.scaleToScreen(controlPointScreens[iNext].z, madEnd);
    doCap0 = (i == iPrev || thisTypeOnly
        && structureTypes[i] != structureTypes[iPrev]);
    doCap1 = (iNext == iNext2 || thisTypeOnly
        && structureTypes[i] != structureTypes[iNext]);
    return ((aspectRatio > 0 && (exportType == GData.EXPORT_CARTESIAN 
        || checkDiameter(diameterBeg)
        || checkDiameter(diameterMid) 
        || checkDiameter(diameterEnd))));
  }

  private boolean checkDiameter(int d) {
    return (isHighRes & d > ABSOLUTE_MIN_MESH_SIZE || d >= MIN_MESH_RENDER_SIZE);
  }

  protected void renderHermiteCylinder(P3i[] screens, int i) {
    //strands
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    setNeighbors(i);
    g3d.drawHermite4(isNucleic ? 4 : 7, screens[iPrev], screens[i],
        screens[iNext], screens[iNext2]);
  }

  protected void renderHermiteConic(int i, boolean thisTypeOnly) {
    //cartoons, rockets, trace
    setNeighbors(i);
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    if (setMads(i, thisTypeOnly) || isExport) {
      try {
        if ((meshes[i] == null || !meshReady[i])
            && !createMesh(i, madBeg, madMid, madEnd, 1))
          return;
        meshes[i].setColix(colix);
        bsRenderMesh.set(i);
        return;
      } catch (Exception e) {
        bsRenderMesh.clear(i);
        meshes[i] = null;
        Logger.error("render mesh error hermiteConic: " + e.toString());
        //System.out.println(e.getMessage());
      }
    }
    g3d.fillHermite(isNucleic ? 4 : 7, diameterBeg, diameterMid, diameterEnd,
        controlPointScreens[iPrev], controlPointScreens[i],
        controlPointScreens[iNext], controlPointScreens[iNext2]);
  }

  /**
   * 
   * @param doFill
   * @param i
   * @param thisTypeOnly true for Cartoon but not MeshRibbon
   */
  protected void renderHermiteRibbon(boolean doFill, int i, boolean thisTypeOnly) {
    // cartoons and meshRibbon
    setNeighbors(i);
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    colixBack = getLeadColixBack(i);
    if (doFill && (aspectRatio != 0 || isExport)) {
      if (setMads(i, thisTypeOnly) || isExport) {
        try {
          if ((meshes[i] == null || !meshReady[i])
              && !createMesh(i, madBeg, madMid, madEnd, aspectRatio))
            return;
          meshes[i].setColix(colix);
          meshes[i].setColixBack(colixBack);
          bsRenderMesh.set(i);
          return;
        } catch (Exception e) {
          bsRenderMesh.clear(i);
          meshes[i] = null;
          Logger.error("render mesh error hermiteRibbon: " + e.toString());
          //System.out.println(e.getMessage());
        }
      }
    }
    g3d.drawHermite7(doFill, ribbonBorder, (reversed.get(i) ? -1 : 1) * (isNucleic ? 4 : 7),
        ribbonTopScreens[iPrev], ribbonTopScreens[i], ribbonTopScreens[iNext],
        ribbonTopScreens[iNext2], ribbonBottomScreens[iPrev],
        ribbonBottomScreens[i], ribbonBottomScreens[iNext],
        ribbonBottomScreens[iNext2], (int) aspectRatio, colixBack);
  }

  //// cardinal hermite (box or flat) arrow head (cartoon)

  private final P3i screenArrowTop = new P3i();
  private final P3i screenArrowTopPrev = new P3i();
  private final P3i screenArrowBot = new P3i();
  private final P3i screenArrowBotPrev = new P3i();

  protected void renderHermiteArrowHead(int i) {
    // cartoons only
    colix = getLeadColix(i);
    if (!setBioColix(colix))
      return;
    colixBack = getLeadColixBack(i);
    setNeighbors(i);
    if (setMads(i, false) || isExport) {
      try {
        doCap0 = true;
        doCap1 = false;
        if ((meshes[i] == null || !meshReady[i])
            && !createMesh(i, (int) Math.floor(madBeg * 1.2), (int) Math.floor(madBeg * 0.6), 0,
                (aspectRatio == 1 ? aspectRatio : aspectRatio / 2)))
          return;
        meshes[i].setColix(colix);
        bsRenderMesh.set(i);
        return;
      } catch (Exception e) {
        bsRenderMesh.clear(i);
        meshes[i] = null;
        Logger.error("render mesh error hermiteArrowHead: " + e.toString());
        //System.out.println(e.getMessage());
      }
    }

    calc1Screen(controlPoints[i], wingVectors[i], madBeg, .0007f,
        screenArrowTop);
    calc1Screen(controlPoints[i], wingVectors[i], madBeg, -.0007f,
        screenArrowBot);
    calc1Screen(controlPoints[i], wingVectors[i], madBeg, 0.001f,
        screenArrowTopPrev);
    calc1Screen(controlPoints[i], wingVectors[i], madBeg, -0.001f,
        screenArrowBotPrev);
    g3d.drawHermite7(true, ribbonBorder, isNucleic ? 4 : 7, screenArrowTopPrev,
        screenArrowTop, controlPointScreens[iNext],
        controlPointScreens[iNext2], screenArrowBotPrev, screenArrowBot,
        controlPointScreens[iNext], controlPointScreens[iNext2], (int) aspectRatio, colixBack);
    if (ribbonBorder && aspectRatio == 0) {
      g3d.fillCylinderXYZ(colix, colix,
          GData.ENDCAPS_SPHERICAL,
          (exportType == GData.EXPORT_CARTESIAN ? 50 : 3), //may not be right 0.05 
          screenArrowTop.x, screenArrowTop.y, screenArrowTop.z,
          screenArrowBot.x, screenArrowBot.y, screenArrowBot.z);
    }
  }

  //  rockets --not satisfactory yet
  /**
   * @param i
   *        IGNORED
   * @param pointBegin
   *        IGNORED
   * @param pointEnd
   *        IGNORED
   * @param screenPtBegin
   * @param screenPtEnd
   * 
   */
  protected void renderCone(int i, P3 pointBegin, P3 pointEnd,
                            P3 screenPtBegin, P3 screenPtEnd) {
    int coneDiameter = mad + (mad >> 2);
    coneDiameter = viewer.scaleToScreen((int) Math.floor(screenPtBegin.z),
        coneDiameter);
    g3d.fillConeSceen3f(GData.ENDCAPS_FLAT, coneDiameter, screenPtBegin,
        screenPtEnd);
  }

  //////////////////////////// mesh 

  // Bob Hanson 11/04/2006 - mesh rendering of secondary structure.
  // mesh creation occurs at rendering time, because we don't
  // know what all the options are, and they aren't important,
  // until it gets rendered, if ever

  private final static int ABSOLUTE_MIN_MESH_SIZE = 3;
  private final static int MIN_MESH_RENDER_SIZE = 8;

  private P3[] controlHermites;
  private V3[] wingHermites;
  private P3[] radiusHermites;

  private V3 norm = new V3();
  private final V3 wing = new V3();
  private final V3 wing1 = new V3();
  private final V3 wingT = new V3();
  private final AxisAngle4f aa = new AxisAngle4f();
  private final P3 pt = new P3();
  private final P3 pt1 = new P3();
  private final P3 ptPrev = new P3();
  private final P3 ptNext = new P3();
  private final Matrix3f mat = new Matrix3f();
  private final static int MODE_TUBE = 0;
  private final static int MODE_FLAT = 1;
  private final static int MODE_ELLIPTICAL = 2;
  private final static int MODE_NONELLIPTICAL = 3;

  /**
   * 
   * @param i
   * @param madBeg
   * @param madMid
   * @param madEnd
   * @param aspectRatio
   * @return true if deferred rendering is required due to normals averaging
   */
  private boolean createMesh(int i, int madBeg, int madMid, int madEnd,
                             float aspectRatio) {
    setNeighbors(i);
    if (controlPoints[i].distance(controlPoints[iNext]) == 0)
      return false;

    // options:

    // isEccentric == not just a tube    
    boolean isEccentric = (aspectRatio != 1 && wingVectors != null);
    // isFlatMesh == using mesh even for hermiteLevel = 0 (for exporters)
    boolean isFlatMesh = (aspectRatio == 0);
    // isElliptical == newer cartoonFancy business
    boolean isElliptical = (cartoonFancy || this.hermiteLevel >= 6);

    // parameters:

    int hermiteLevel = Math.max(this.hermiteLevel, 5);
    int nHermites = (hermiteLevel + 1) * 2 + 1; // 4 for hermiteLevel = 1; 13 for hermitelevel 5
    int nPer = (isFlatMesh ? 4 : (hermiteLevel + 1) * 4 - 2); // 6 for hermiteLevel 1; 22 for hermiteLevel 5
    float angle = (float) ((isFlatMesh ? Math.PI / (nPer - 1) : 2 * Math.PI
        / nPer));
    Mesh mesh = meshes[i] = new Mesh("mesh_" + shapeID + "_" + i, (short) 0, i);
    boolean variableRadius = (madBeg != madMid || madMid != madEnd);

    // control points and vectors:

    if (controlHermites == null || controlHermites.length < nHermites + 1) {
      controlHermites = new P3[nHermites + 1];
    }
    Hermite.getHermiteList(isNucleic ? 4 : 7, controlPoints[iPrev],
        controlPoints[i], controlPoints[iNext], controlPoints[iNext2],
        controlPoints[iNext3], controlHermites, 0, nHermites, true);
    // wing hermites determine the orientation of the cartoon
    if (wingHermites == null || wingHermites.length < nHermites + 1) {
      wingHermites = new V3[nHermites + 1];
    }

    wing.setT(wingVectors[iPrev]);
    if (madEnd == 0)
      wing.scale(2.0f); //adds a flair to an arrow
    Hermite.getHermiteList(isNucleic ? 4 : 7, wing, wingVectors[i],
        wingVectors[iNext], wingVectors[iNext2], wingVectors[iNext3],
        wingHermites, 0, nHermites, false);
    //    }
    // radius hermites determine the thickness of the cartoon
    float radius1 = madBeg / 2000f;
    float radius2 = madMid / 2000f;
    float radius3 = madEnd / 2000f;
    if (variableRadius) {
      if (radiusHermites == null
          || radiusHermites.length < ((nHermites + 1) >> 1) + 1) {
        radiusHermites = new P3[((nHermites + 1) >> 1) + 1];
      }
      ptPrev.set(radius1, radius1, 0);
      pt.set(radius1, radius2, 0);
      pt1.set(radius2, radius3, 0);
      ptNext.set(radius3, radius3, 0);
      // two for the price of one!
      Hermite.getHermiteList(4, ptPrev, pt, pt1, ptNext, ptNext,
          radiusHermites, 0, (nHermites + 1) >> 1, true);
    }

    // now create the cartoon polygon

    int nPoints = 0;
    int iMid = nHermites >> 1;
    int kpt1 = (nPer + 2) / 4;
    int kpt2 = (3 * nPer + 2) / 4;
    int mode = (!isEccentric ? MODE_TUBE : isFlatMesh ? MODE_FLAT
        : isElliptical ? MODE_ELLIPTICAL : MODE_NONELLIPTICAL);
    boolean useMat = (mode == MODE_TUBE || mode == MODE_NONELLIPTICAL);
    for (int p = 0; p < nHermites; p++) {
      norm.sub2(controlHermites[p + 1], controlHermites[p]);
      float scale = (!variableRadius ? radius1 : p < iMid ? radiusHermites[p].x
          : radiusHermites[p - iMid].y);
      wing.setT(wingHermites[p]);
      wing1.setT(wing);
      switch (mode) {
      case MODE_FLAT:
        // hermiteLevel = 0 and not exporting
        break;
      case MODE_ELLIPTICAL:
        // cartoonFancy 
        wing1.cross(norm, wing);
        wing1.normalize();
        wing1.scale(wing.length() / aspectRatio);
        break;
      case MODE_NONELLIPTICAL:
        // older nonelliptical hermiteLevel > 0
        wing.scale(2 / aspectRatio);
        wing1.sub(wing);
        break;
      case MODE_TUBE:
        // not helix or sheet
        wing.cross(wing, norm);
        wing.normalize();
        break;
      }
      wing.scale(scale);
      wing1.scale(scale);
      if (useMat) {
        aa.setVA(norm, angle);
        mat.setAA(aa);
      }
      pt1.setT(controlHermites[p]);
      float theta = (isFlatMesh ? 0 : angle);
      for (int k = 0; k < nPer; k++, theta += angle) {
        if (useMat && k > 0)
          mat.transform(wing);
        switch (mode) {
        case MODE_FLAT:
          wingT.setT(wing1);
          wingT.scale((float) Math.cos(theta));
          break;
        case MODE_ELLIPTICAL:
          wingT.setT(wing1);
          wingT.scale((float) Math.sin(theta));
          wingT.scaleAdd2((float) Math.cos(theta), wing, wingT);
          break;
        case MODE_NONELLIPTICAL:
          wingT.setT(wing);
          if (k == kpt1 || k == kpt2)
            wing1.scale(-1);
          wingT.add(wing1);
          break;
        case MODE_TUBE:
          wingT.setT(wing);
          break;
        }
        pt.add2(pt1, wingT);
        mesh.addV(pt);
      }
      if (p > 0) {
        int nLast = (isFlatMesh ? nPer - 1 : nPer);
        for (int k = 0; k < nLast; k++) {
          // draw the triangles of opposing quads congruent, so they won't clip 
          // esp. for high ribbonAspectRatio values 
          int a = nPoints - nPer + k;
          int b = nPoints - nPer + ((k + 1) % nPer);
          int c = nPoints + ((k + 1) % nPer);
          int d = nPoints + k;
          if (k < nLast / 2)
            mesh.addQuad(a, b, c, d);
          else
            mesh.addQuad(b, c, d, a);
        }
      }
      nPoints += nPer;
    }
    if (!isFlatMesh) {
      if (doCap0)
        for (int k = hermiteLevel * 2; --k >= 0;)
          mesh.addQuad(k + 2, k + 1, (nPer - k) % nPer, nPer - k - 1);
      if (doCap1)
        for (int k = hermiteLevel * 2; --k >= 0;)
          mesh.addQuad(nPoints - k - 1, nPoints - nPer + (nPer - k) % nPer,
              nPoints - nPer + k + 1, nPoints - nPer + k + 2);
    }
    meshReady[i] = true;
    adjustCartoonSeamNormals(i, nPer);
    mesh.setVisibilityFlags(1);
    return true;
  }

  private BS bsTemp;
  private final V3 norml = new V3();

  /**
   * Matches normals for adjacent mesh sections to create a seamless overall
   * mesh. We use temporary normals here. We will convert normals to normixes
   * later.
   * 
   * @param i
   * @param nPer
   */
  void adjustCartoonSeamNormals(int i, int nPer) {
    if (bsTemp == null)
      bsTemp = Normix.newVertexBitSet();
    if (i == iNext - 1 && iNext < monomerCount
        && monomers[i].getStrucNo() == monomers[iNext].getStrucNo()
        && meshReady[i] && meshReady[iNext]) {
      try {
        V3[] normals2 = meshes[iNext].getNormalsTemp();
        V3[] normals = meshes[i].getNormalsTemp();
        int normixCount = normals.length;
        for (int j = 1; j <= nPer; ++j) {
          norml.add2(normals[normixCount - j], normals2[nPer - j]);
          norml.normalize();
          meshes[i].normalsTemp[normixCount - j].setT(norml);
          meshes[iNext].normalsTemp[nPer - j].setT(norml);
        }
      } catch (Exception e) {
      }
    }
  }

  private void renderMeshes() {
    for (int i = bsRenderMesh.nextSetBit(0); i >= 0; i = bsRenderMesh
        .nextSetBit(i + 1)) {
      if (meshes[i].normalsTemp != null) {
        meshes[i].setNormixes(meshes[i].normalsTemp);
        meshes[i].normalsTemp = null;
      } else if (meshes[i].normixes == null) {
        meshes[i].initialize(T.frontlit, null, null);
      }
      renderMesh(meshes[i]);
    }
  }

  /*
  private void dumpPoint(Point3f pt, short color) {
    Point3i pt1 = viewer.transformPoint(pt);
    g3d.fillSphereCentered(color, 20, pt1);
  }

  private void dumpVector(Point3f pt, Vector3f v, short color) {
    Point3f p1 = new Point3f();
    Point3i pt1 = new Point3i();
    Point3i pt2 = new Point3i();
    p1.add(pt, v);
    pt1.set(viewer.transformPoint(pt));
    pt2.set(viewer.transformPoint(p1));
    System.out.print("draw pt" + ("" + Math.random()).substring(3, 10) + " {"
        + pt.x + " " + pt.y + " " + pt.z + "} {" + p1.x + " " + p1.y + " "
        + p1.z + "}" + ";" + " ");
    g3d.fillCylinder(color, GData.ENDCAPS_FLAT, 2, pt1.x, pt1.y, pt1.z,
        pt2.x, pt2.y, pt2.z);
    g3d.fillSphereCentered(color, 5, pt2);
  }
  */

}
