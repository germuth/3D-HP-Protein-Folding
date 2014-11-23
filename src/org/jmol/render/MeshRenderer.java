/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-25 09:53:35 -0500 (Wed, 25 Apr 2007) $
 * $Revision: 7491 $
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



import org.jmol.api.SymmetryInterface;
import org.jmol.script.T;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.GData;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.Point4f;
import org.jmol.util.V3;

public abstract class MeshRenderer extends ShapeRenderer {

  protected Mesh mesh;
  protected P3[] vertices;
  protected short[] normixes;
  protected P3i[] screens;
  protected V3[] transformedVectors;
  protected int vertexCount;
  
  protected float imageFontScaling;
  protected float scalePixelsPerMicron;
  protected int diameter;
  protected float width;
  

  protected boolean isTranslucent;
  protected boolean frontOnly;
  protected boolean antialias;
  protected boolean haveBsDisplay;
  protected boolean haveBsSlabDisplay;
  protected boolean haveBsSlabGhost;

  protected Point4f thePlane;
  protected P3 latticeOffset = new P3();

  protected final P3 pt1f = new P3();
  protected final P3 pt2f = new P3();

  protected final P3i pt1i = new P3i();
  protected final P3i pt2i = new P3i();
  protected final P3i pt3i = new P3i();
  protected int exportPass;
  protected boolean needTranslucent;

  @Override
  protected boolean render() {
    needTranslucent = false;
    antialias = g3d.isAntialiased(); 
    MeshCollection mc = (MeshCollection) shape;
    for (int i = mc.meshCount; --i >= 0;)
      renderMesh(mc.meshes[i]);
    return needTranslucent;
  }
  
  // draw, isosurface, molecular orbitals
  public boolean renderMesh(Mesh mesh) { // used by mps renderer
    return renderMesh2(mesh);
  }

  protected boolean renderMesh2(Mesh mesh) {
    this.mesh = mesh;
    if (!setVariables())
      return false;
    if (!doRender)
      return mesh.title != null;
    latticeOffset.set(0, 0, 0);
    for (int i = vertexCount; --i >= 0;)
      if (vertices[i] != null)
        viewer.transformPtScr(vertices[i], screens[i]);
    if (mesh.lattice == null || mesh.modelIndex < 0) {
      render2(isExport);
    } else {
      SymmetryInterface unitcell = mesh.unitCell;
      if (unitcell == null)
        unitcell = viewer.getModelUnitCell(mesh.modelIndex);
      if (unitcell == null) 
        unitcell = mesh.getUnitCell();
      if (unitcell != null) {
        P3 vTemp = new P3();
        P3i minXYZ = new P3i();
        P3i maxXYZ = P3i.new3((int) mesh.lattice.x,
            (int) mesh.lattice.y, (int) mesh.lattice.z);
        unitcell.setMinMaxLatticeParameters(minXYZ, maxXYZ);
        for (int tx = minXYZ.x; tx < maxXYZ.x; tx++)
          for (int ty = minXYZ.y; ty < maxXYZ.y; ty++)
            for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
              latticeOffset.set(tx, ty, tz);
              unitcell.toCartesian(latticeOffset, false);
              for (int i = vertexCount; --i >= 0;) {
                vTemp.setT(vertices[i]);
                vTemp.add(latticeOffset);
                viewer.transformPtScr(vTemp, screens[i]);
              }
              render2(isExport);
            }
      }
    }

    if (screens != null)
      viewer.freeTempScreens(screens);
    return true;
  }

  private boolean doRender;
  protected boolean volumeRender;
  protected BS bsSlab;
  
  
  private boolean setVariables() {
    if (mesh.visibilityFlags == 0)
      return false;
    if (mesh.bsSlabGhost != null)
      g3d.setColix(mesh.slabColix); // forces a second pass
    haveBsSlabGhost = (mesh.bsSlabGhost != null && (isExport ? exportPass == 2
        : g3d.isPass2()));
    isTranslucent = haveBsSlabGhost
        || C.isColixTranslucent(mesh.colix);
    if (isTranslucent || volumeRender)
      needTranslucent = true;
    doRender = (setColix(mesh.colix) || mesh.showContourLines);
    if (!doRender || haveBsSlabGhost && !(doRender = g3d.setColix(mesh.slabColix))) {
      vertices = mesh.vertices;      
      return true;
    }
    vertices = (mesh.scale3d == 0 && mesh.mat4 == null ? mesh.vertices : mesh.getOffsetVertices(thePlane));
    if (mesh.lineData == null) {
      if ((vertexCount = mesh.vertexCount) == 0)
        return false;
      normixes = mesh.normixes;
      if (normixes == null || vertices == null)
        return false;
      // this can happen when user switches windows
      // during a surface calculation

      haveBsDisplay = (mesh.bsDisplay != null);
      haveBsSlabDisplay = (haveBsSlabGhost || mesh.bsSlabDisplay != null);
      bsSlab = (haveBsSlabGhost ? mesh.bsSlabGhost
          : haveBsSlabDisplay ? mesh.bsSlabDisplay : null);
      frontOnly = !viewer.getSlabEnabled() && mesh.frontOnly
          && !mesh.isTwoSided && !haveBsSlabDisplay;
      screens = viewer.allocTempScreens(vertexCount);
      if (frontOnly)
        transformedVectors = g3d.getTransformedVertexVectors();
      if (transformedVectors == null)
        frontOnly = false;
    }
    return true;
  }

  protected boolean setColix(short colix) {
    if (haveBsSlabGhost)
      return true;
    if (volumeRender && !isTranslucent)
      colix = C.getColixTranslucent3(colix, true, 0.8f);
    this.colix = colix;
    if (C.isColixLastAvailable(colix))
      g3d.setColor(mesh.color);
    return g3d.setColix(colix);
  }

  // all of the following methods are overridden in subclasses
  // DO NOT change parameters without first checking for the
  // same method in a subclass.
  
  /**
   * @param i 
   * @return T/F
   * 
   */
  protected boolean isPolygonDisplayable(int i) {
    return true;
  }

  //isosurface,meshRenderer::render1 (just about everything)
  protected void render2(boolean generateSet) {
    render2b(generateSet);
  }
  
  protected void render2b(boolean generateSet) {
    if (!g3d.setColix(haveBsSlabGhost ? mesh.slabColix : colix))
      return;
    if (mesh.showPoints || mesh.polygonCount == 0)
      renderPoints();    
    if (haveBsSlabGhost ? mesh.slabMeshType == T.mesh : mesh.drawTriangles)
      renderTriangles(false, mesh.showTriangles, false);
    if (haveBsSlabGhost ? mesh.slabMeshType == T.fill : mesh.fillTriangles)
      renderTriangles(true, mesh.showTriangles, generateSet);
  }

  protected void renderPoints() {
    if (mesh.isTriangleSet) {
      int[][] polygonIndexes = mesh.polygonIndexes;
      BS bsPoints = BSUtil.newBitSet(mesh.vertexCount);
      if (haveBsDisplay) {
        bsPoints.setBits(0, mesh.vertexCount);
        bsPoints.andNot(mesh.bsDisplay);
      }
      for (int i = mesh.polygonCount; --i >= 0;) {
        if (!isPolygonDisplayable(i))
          continue;
        int[] p = polygonIndexes[i];
        if (frontOnly && transformedVectors[normixes[i]].z < 0)
          continue;
        for (int j = p.length - 1; --j >= 0;) {
          int pt = p[j];
          if (bsPoints.get(pt))
            continue;
          bsPoints.set(pt);
          g3d.fillSphereI(4, screens[pt]);
        }
      }
      return;
    }
    for (int i = vertexCount; --i >= 0;)
      if (!frontOnly || transformedVectors[normixes[i]].z >= 0)
        g3d.fillSphereI(4, screens[i]);
  }

  protected BS bsPolygons = new BS();

  protected void renderTriangles(boolean fill, boolean iShowTriangles,
                                 boolean generateSet) {
    int[][] polygonIndexes = mesh.polygonIndexes;
    colix = (haveBsSlabGhost ? mesh.slabColix : mesh.colix);
    // vertexColixes are only isosurface properties of IsosurfaceMesh, not Mesh
    g3d.setColix(colix);
    if (generateSet) {
      if (frontOnly && fill)
        frontOnly = false;
      bsPolygons.clearAll();
    }
    for (int i = mesh.polygonCount; --i >= 0;) {
      if (!isPolygonDisplayable(i))
        continue;
      int[] vertexIndexes = polygonIndexes[i];
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      if (haveBsDisplay
          && (!mesh.bsDisplay.get(iA) || !mesh.bsDisplay.get(iB) || !mesh.bsDisplay
              .get(iC)))
        continue;
      if (iB == iC) {
        // line or point
        drawLine(iA, iB, fill, vertices[iA], vertices[iB], screens[iA],
            screens[iB]);
        continue;
      }
      int check;
      if (mesh.isTriangleSet) {
        short normix = normixes[i];
        if (!g3d.isDirectedTowardsCamera(normix))
          continue;
        if (fill) {
          if (isExport) {
            g3d.fillTriangle3CN(screens[iC], colix, normix, screens[iB], colix,
                normix, screens[iA], colix, normix);
          } else if (iShowTriangles) {
            g3d.fillTriangle(screens[iA], colix, normix, screens[iB], colix,
                normix, screens[iC], colix, normix, 0.1f);
          } else {
            g3d.fillTriangle3CN(screens[iA], colix, normix, screens[iB], colix,
                normix, screens[iC], colix, normix);
          }
          continue;
        }
        check = vertexIndexes[3];
        if (iShowTriangles)
          check = 7;
        if ((check & 1) == 1)
          drawLine(iA, iB, true, vertices[iA], vertices[iB], screens[iA],
              screens[iB]);
        if ((check & 2) == 2)
          drawLine(iB, iC, true, vertices[iB], vertices[iC], screens[iB],
              screens[iC]);
        if ((check & 4) == 4)
          drawLine(iA, iC, true, vertices[iA], vertices[iC], screens[iA],
              screens[iC]);
        continue;
      }
      short nA = normixes[iA];
      short nB = normixes[iB];
      short nC = normixes[iC];
      check = checkNormals(nA, nB, nC);
      if (fill && check != 7)
        continue;
      switch (vertexIndexes.length) {
      case 3:
        if (fill) {
          if (generateSet) {
            bsPolygons.set(i);
            continue;
          }
          if (iShowTriangles) {
            g3d.fillTriangle(screens[iA], colix, nA, screens[iB], colix, nB,
                screens[iC], colix, nC, 0.1f);
            continue;
          }
          g3d.fillTriangle3CN(screens[iA], colix, nA, screens[iB], colix, nB,
              screens[iC], colix, nC);
          continue;
        }
        drawTriangle(screens[iA], colix, screens[iB], colix, screens[iC], colix, check, 1);
        continue;
      case 4:
        int iD = vertexIndexes[3];
        short nD = normixes[iD];
        if (frontOnly && (check != 7 || transformedVectors[nD].z < 0))
          continue;
        if (fill) {
          if (generateSet) {
            bsPolygons.set(i);
            continue;
          }
          g3d.fillQuadrilateral3i(screens[iA], colix, nA, screens[iB], colix, nB,
              screens[iC], colix, nC, screens[iD], colix, nD);
          continue;
        }
        g3d.drawQuadrilateral(colix, screens[iA], screens[iB], screens[iC],
            screens[iD]);
      }
    }
    if (generateSet)
      exportSurface(colix);
  }

  protected void drawTriangle(P3i screenA, short colixA, 
                            P3i screenB, short colixB, 
                            P3i screenC, short colixC, int check, int diam) {
    if (antialias || diam != 1) {
      if (antialias)
        diam <<= 1;
      if ((check & 1) == 1)
        g3d.fillCylinderXYZ(colixA, colixB, GData.ENDCAPS_OPEN, diam, 
            screenA.x, screenA.y, screenA.z, screenB.x, screenB.y, screenB.z);
      if ((check & 2) == 2)
        g3d.fillCylinderXYZ(colixB, colixC, GData.ENDCAPS_OPEN, diam, 
            screenB.x, screenB.y, screenB.z, screenC.x, screenC.y, screenC.z);
      if ((check & 4) == 4)
        g3d.fillCylinderXYZ(colixA, colixC, GData.ENDCAPS_OPEN, diam, 
            screenA.x, screenA.y, screenA.z, screenC.x, screenC.y, screenC.z);
    } else {
      g3d.drawTriangle3C(screenA, colixA, screenB, colixB, screenC, colixC, check);
    }
  }

  protected int checkNormals(short nA, short nB, short nC) {
    int check = 7;
    if (frontOnly) {
      if (transformedVectors[nA].z < 0)
        check ^= 1;
      if (transformedVectors[nB].z < 0)
        check ^= 2;
      if (transformedVectors[nC].z < 0)
        check ^= 4;
    }
    return check;
  }

  protected void drawLine(int iA, int iB, boolean fill, 
                          P3 vA, P3 vB, 
                          P3i sA, P3i sB) {
    byte endCap = (iA != iB  && !fill ? GData.ENDCAPS_NONE 
        : width < 0 || width == -0.0 || iA != iB && isTranslucent ? GData.ENDCAPS_FLAT
        : GData.ENDCAPS_SPHERICAL);
    if (width == 0) {
      if (diameter == 0)
        diameter = (mesh.diameter > 0 ? mesh.diameter : iA == iB ? 7 : 3);
      if (exportType == GData.EXPORT_CARTESIAN) {
        pt1f.setT(vA);
        pt1f.add(vB);
        pt1f.scale(1f / 2f);
        viewer.transformPtScr(pt1f, pt1i);
        diameter = (int) Math.floor(viewer.unscaleToScreen(pt1i.z, diameter) * 1000);
      }
      if (iA == iB) {
        g3d.fillSphereI(diameter, sA);
      } else {
        g3d.fillCylinder(endCap, diameter, sA, sB);
      }
    } else {
      pt1f.setT(vA);
      pt1f.add(vB);
      pt1f.scale(1f / 2f);
      viewer.transformPtScr(pt1f, pt1i);
      int mad = (int) Math.floor(Math.abs(width) * 1000); 
      diameter = (exportType == GData.EXPORT_CARTESIAN ? mad 
          : viewer.scaleToScreen(pt1i.z, mad));
      if (diameter == 0)
        diameter = 1;
      viewer.transformPt3f(vA, pt1f);
      viewer.transformPt3f(vB, pt2f);
      g3d.fillCylinderBits(endCap, diameter, pt1f, pt2f);
    }
  }

  protected void exportSurface(short colix) {
    mesh.normals = mesh.getNormals(vertices, null);
    mesh.bsPolygons = bsPolygons;
    mesh.offset = latticeOffset;
    g3d.drawSurface(mesh, colix);
    mesh.normals = null;
    mesh.bsPolygons = null;
  }
  
}
