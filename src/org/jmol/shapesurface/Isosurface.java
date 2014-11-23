/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-25 11:08:02 -0500 (Wed, 25 Apr 2007) $
 * $Revision: 7492 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net,jmol-developers@lists.sourceforge.net
 * Contact: hansonr@stolaf.edu
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

/*
 * miguel 2005 07 17
 *
 *  System and method for the display of surface structures
 *  contained within the interior region of a solid body
 * United States Patent Number 4,710,876
 * Granted: Dec 1, 1987
 * Inventors:  Cline; Harvey E. (Schenectady, NY);
 *             Lorensen; William E. (Ballston Lake, NY)
 * Assignee: General Electric Company (Schenectady, NY)
 * Appl. No.: 741390
 * Filed: June 5, 1985
 *
 *
 * Patents issuing prior to June 8, 1995 can last up to 17
 * years from the date of issuance.
 *
 * Dec 1 1987 + 17 yrs = Dec 1 2004
 */

/*
 * Bob Hanson May 22, 2006
 * 
 * implementing marching squares; see 
 * http://www.secam.ex.ac.uk/teaching/ug/studyres/COM3404/COM3404-2006-Lecture15.pdf
 *  
 * inventing "Jmol Voxel File" format, *.jvxl
 * 
 * see http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/JVXL-format.pdf
 * 
 * lines through coordinates are identical to CUBE files
 * after that, we have a line that starts with a negative number to indicate this
 * is a JVXL file:
 * 
 * line1:  (int)-nSurfaces  (int)edgeFractionBase (int)edgeFractionRange  
 * (nSurface lines): (float)cutoff (int)nBytesData (int)nBytesFractions
 * 
 * definition1
 * edgedata1
 * fractions1
 * colordata1
 * ....
 * definition2
 * edgedata2
 * fractions2
 * colordata2
 * ....
 * 
 * definitions: a line with detail about what sort of compression follows
 * 
 * edgedata: a list of the count of vertices ouside and inside the cutoff, whatever
 * that may be, ordered by nested for loops for(x){for(y){for(z)}}}.
 * 
 * nOutside nInside nOutside nInside...
 * 
 * fractions: an ascii list of characters represting the fraction of distance each
 * encountered surface point is along each voxel cube edge found to straddle the 
 * surface. The order written is dictated by the reader algorithm and is not trivial
 * to describe. Each ascii character is constructed by taking a base character and 
 * adding onto it the fraction times a range. This gives a character that can be
 * quoted EXCEPT for backslash, which MAY be substituted for by '!'. Jmol uses the 
 * range # - | (35 - 124), reserving ! and } for special meanings.
 * 
 * colordata: same deal here, but with possibility of "double precision" using two bytes.
 * 
 */

package org.jmol.shapesurface;

import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;
import org.jmol.shape.Shape;
import org.jmol.util.Escape;

import org.jmol.util.AxisAngle4f;
import org.jmol.util.BS;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.ArrayUtil;
import org.jmol.util.ColorUtil;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
//import org.jmol.util.Measure;
import org.jmol.util.MeshSurface;
import org.jmol.util.Parser;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.Point4f;
import org.jmol.util.Quaternion;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;
//import org.jmol.viewer.StateManager.Orientation;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import org.jmol.util.JmolList;

import java.util.Hashtable;

import java.util.Map;


import org.jmol.api.JmolDocument;
import org.jmol.io.JmolBinary;
import org.jmol.jvxl.api.MeshDataServer;
import org.jmol.jvxl.data.JvxlCoder;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.readers.Parameters;
import org.jmol.jvxl.readers.SurfaceGenerator;

public class Isosurface extends MeshCollection implements MeshDataServer {

  private IsosurfaceMesh[] isomeshes = new IsosurfaceMesh[4];
  protected IsosurfaceMesh thisMesh;

  @Override
  public void allocMesh(String thisID, Mesh m) {
    int index = meshCount++;
    meshes = isomeshes = (IsosurfaceMesh[]) ArrayUtil.ensureLength(isomeshes,
        meshCount * 2);
    currentMesh = thisMesh = isomeshes[index] = (m == null ? new IsosurfaceMesh(
        thisID, colix, index) : (IsosurfaceMesh) m);
    currentMesh.index = index;
    sg.setJvxlData(jvxlData = thisMesh.jvxlData);
  }

  @Override
  public void initShape() {
    super.initShape();
    myType = "isosurface";
    newSg();
  }

  protected void newSg() {
    sg = new SurfaceGenerator(viewer, this, null, jvxlData = new JvxlData());
    sg.getParams().showTiming = viewer.getShowTiming();
    sg.setVersion("Jmol " + Viewer.getJmolVersion());
  }
  
  protected void clearSg() {
    sg = null; // not Molecular Orbitals
  }
  //private boolean logMessages;
  private String actualID;
  protected boolean iHaveBitSets;
  private boolean explicitContours;
  private int atomIndex;
  private int moNumber;
  private float[] moLinearCombination;
  private short defaultColix;
  private short meshColix;
  private P3 center;
  private float scale3d;
  private boolean isPhaseColored;
  private boolean isColorExplicit;
  private String scriptAppendix = "";

  protected SurfaceGenerator sg;
  protected JvxlData jvxlData;

  private float withinDistance2;
  private boolean isWithinNot;
  private JmolList<P3> withinPoints;
  private float[] cutoffRange;

  //private boolean allowContourLines;
  boolean allowMesh = true;

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    setPropI(propertyName, value, bs);
  }  

  @SuppressWarnings("unchecked")
  protected void setPropI(String propertyName, Object value, BS bs) {

    //System.out.println("isosurface testing " + propertyName + " " + value + (propertyName == "token" ? " " + Token.nameOf(((Integer)value).intValue()) : ""));

    //isosurface-only (no calculation required; no calc parameters to set)

    //    if ("navigate" == propertyName) {
    //      navigate(((Integer) value).intValue());
    //      return;
    //    }
    if ("delete" == propertyName) {
      setPropertySuper(propertyName, value, bs);
      if (!explicitID)
        nLCAO = nUnnamed = 0;
      currentMesh = thisMesh = null;
      return;
    }

    if ("remapInherited" == propertyName) {
      for (int i = meshCount; --i >= 0;) {
        if (isomeshes[i] != null
            && "#inherit;".equals(isomeshes[i].colorCommand))
          isomeshes[i].remapColors(viewer, null, Float.NaN);
      }
      return;
    }

    if ("remapColor" == propertyName) {
      if (thisMesh != null)
        thisMesh.remapColors(viewer, (ColorEncoder) value, translucentLevel);
      return;
    }

    if ("thisID" == propertyName) {
      if (actualID != null)
        value = actualID;
      setPropertySuper("thisID", value, null);
      return;
    }

    if ("atomcolor" == propertyName) {
      if (thisMesh != null) {
        if (thisMesh.vertexSource == null) {
          short colix = (!thisMesh.isColorSolid ? 0 : thisMesh.colix);
          setProperty("init", null, null);
          setProperty("map", Boolean.FALSE, null);
          setProperty("property", new float[viewer.getAtomCount()], null);
          if (colix != 0) {
            thisMesh.colorCommand = "color isosurface "
                + C.getHexCode(colix);
            setProperty("color", Integer.valueOf(C.getArgb(colix)), null);
          }
        }
        thisMesh.colorAtoms(C.getColixO(value), bs);
      }
      return;
    }

    if ("pointSize" == propertyName) {
      if (thisMesh != null) {
        thisMesh.volumeRenderPointSize = ((Float) value).floatValue();
      }
      return;
    }

    if ("vertexcolor" == propertyName) {
      if (thisMesh != null) {
        thisMesh.colorVertices(C.getColixO(value), bs);
      }
      return;
    }

    if ("colorPhase" == propertyName) {
      // from color isosurface phase color1 color2  Jmol 12.3.5
      Object[] colors = (Object[]) value;
      if (thisMesh != null) {
        thisMesh.colorPhased = true;
        thisMesh.colix = thisMesh.jvxlData.minColorIndex = C
            .getColix(((Integer) colors[0]).intValue());
        thisMesh.jvxlData.maxColorIndex = C.getColix(((Integer) colors[1])
            .intValue());
        thisMesh.jvxlData.isBicolorMap = true;
        thisMesh.jvxlData.colorDensity = false;
        thisMesh.isColorSolid = false;
        thisMesh.remapColors(viewer, null, translucentLevel);
      }
      return;
    }
    if ("color" == propertyName) {
      if (thisMesh != null) {
        // thisMesh.vertexColixes = null;
        thisMesh.isColorSolid = true;
        thisMesh.polygonColixes = null;
        thisMesh.colorEncoder = null;
        thisMesh.vertexColorMap = null;
      } else if (!TextFormat.isWild(previousMeshID)) {
        for (int i = meshCount; --i >= 0;) {
          // isomeshes[i].vertexColixes = null;
          isomeshes[i].isColorSolid = true;
          isomeshes[i].polygonColixes = null;
          isomeshes[i].colorEncoder = null;
          isomeshes[i].vertexColorMap = null;
        }
      }
      setPropertySuper(propertyName, value, bs);
      return;
    }

    if ("nocontour" == propertyName) {
      // recontouring
      if (thisMesh != null) {
        thisMesh.deleteContours();
      }
      return;
    }
    if ("fixed" == propertyName) {
      isFixed = ((Boolean) value).booleanValue();
      setMesh();
      return;
    }

    if ("newObject" == propertyName) {
      if (thisMesh != null)
        thisMesh.clear(thisMesh.meshType, false);
      return;
    }

    if ("moveIsosurface" == propertyName) {
      if (thisMesh != null) {
        thisMesh.updateCoordinates((Matrix4f) value, null);
        thisMesh.altVertices = null;
      }
      return;
    }

    if ("refreshTrajectories" == propertyName) {
      for (int i = meshCount; --i >= 0;)
        if (meshes[i].connections != null
            && meshes[i].modelIndex == ((Integer) ((Object[]) value)[0])
                .intValue())
          ((IsosurfaceMesh) meshes[i]).updateCoordinates(
              (Matrix4f) ((Object[]) value)[2], (BS) ((Object[]) value)[1]);
      return;
    }

    if ("modelIndex" == propertyName) {
      if (!iHaveModelIndex) {
        modelIndex = ((Integer) value).intValue();
        isFixed = (modelIndex < 0);
        sg.setModelIndex(Math.abs(modelIndex));
      }
      return;
    }

    if ("lcaoCartoon" == propertyName || "lonePair" == propertyName
        || "radical" == propertyName) {
      // z x center rotationAxis (only one of x, y, or z is nonzero; in radians)
      V3[] info = (V3[]) value;
      if (!explicitID) {
        setPropertySuper("thisID", null, null);
      }
      // center (info[2]) is set in SurfaceGenerator
      if (!sg.setParameter("lcaoCartoonCenter", info[2]))
        drawLcaoCartoon(
            info[0],
            info[1],
            info[3],
            ("lonePair" == propertyName ? 2 : "radical" == propertyName ? 1 : 0));
      return;
    }

    if ("select" == propertyName) {
      if (iHaveBitSets)
        return;
    }

    if ("ignore" == propertyName) {
      if (iHaveBitSets)
        return;
    }

    if ("meshcolor" == propertyName) {
      int rgb = ((Integer) value).intValue();
      meshColix = C.getColix(rgb);
      if (thisMesh != null)
        thisMesh.meshColix = meshColix;
      return;
    }

    if ("offset" == propertyName) {
      P3 offset = P3.newP((P3) value);
      if (offset.equals(JC.center))
        offset = null;
      if (thisMesh != null) {
        thisMesh.rotateTranslate(null, offset, true);
        thisMesh.altVertices = null;
      }
      return;
    }

    if ("rotate" == propertyName) {
      Point4f pt4 = (Point4f) value;
      if (thisMesh != null) {
        thisMesh.rotateTranslate(Quaternion.newP4(pt4), null, true);
        thisMesh.altVertices = null;
      }
      return;
    }

    if ("bsDisplay" == propertyName) {
      bsDisplay = (BS) value;
      return;
    }
    if ("displayWithin" == propertyName) {
      Object[] o = (Object[]) value;
      displayWithinDistance2 = ((Float) o[0]).floatValue();
      isDisplayWithinNot = (displayWithinDistance2 < 0);
      displayWithinDistance2 *= displayWithinDistance2;
      displayWithinPoints = (JmolList<P3>) o[3];
      if (displayWithinPoints.size() == 0)
        displayWithinPoints = viewer.getAtomPointVector((BS) o[2]);
      return;
    }

    if ("finalize" == propertyName) {
      if (thisMesh != null) {
        String cmd = (String) value;
        if (cmd != null && !cmd.startsWith("; isosurface map")) {
          thisMesh.setDiscreteColixes(sg.getParams().contoursDiscrete, sg
              .getParams().contourColixes);
          setJvxlInfo();
        }
        setScriptInfo(cmd);
      }
      clearSg();
      return;
    }

    if ("privateKey" == propertyName) {
      this.privateKey = ((Double) value).doubleValue();
      return;
    }

    if ("connections" == propertyName) {
      if (currentMesh != null) {
        connections = (int[]) value;
        if (connections[0] >= 0 && connections[0] < viewer.getAtomCount())
          currentMesh.connections = connections;
        else
          connections = currentMesh.connections = null;
      }
      return;
    }

    if ("cutoffRange" == propertyName) {
      cutoffRange = (float[]) value;
      return;
    }

    // Isosurface / SurfaceGenerator both interested

    if ("slab" == propertyName) {
      if (value instanceof Integer) {
        if (thisMesh != null)
          thisMesh.jvxlData.slabValue = ((Integer) value).intValue();
        return;
      }
      if (thisMesh != null) {
        Object[] slabInfo = (Object[]) value;
        int tok = ((Integer) slabInfo[0]).intValue();
        switch (tok) {
        case T.mesh:
          Object[] data = (Object[]) slabInfo[1];
          Mesh m = getMesh((String) data[1]);
          if (m == null)
            return;
          data[1] = m;
          break;
        }
        slabPolygons(slabInfo);
        return;
      }
    }

    if ("cap" == propertyName) {
      // for lcaocartoons?
      if (thisMesh != null && thisMesh.polygonCount != 0) {
        thisMesh.slabPolygons((Object[]) value, true);
        thisMesh.initialize(thisMesh.lighting, null, null);
        return;
      }
    }
    if ("map" == propertyName) {
      if (sg != null)
        sg.getParams().isMapped = true;
      setProperty("squareData", Boolean.FALSE, null);
      if (thisMesh == null || thisMesh.vertexCount == 0)
        return;
    }

    if ("deleteVdw" == propertyName) {
      for (int i = meshCount; --i >= 0;)
        if (isomeshes[i].bsVdw != null
            && (bs == null || bs.intersects(isomeshes[i].bsVdw)))
          deleteMeshI(i);
      currentMesh = thisMesh = null;
      return;
    }
    if ("mapColor" == propertyName || "readFile" == propertyName) {
      if (value == null) {
        // ScriptEvaluator has passed the filename to us as the value of the
        // "fileName" property. We retrieve that from the surfaceGenerator
        // and open a BufferedReader for it. Or not. But that would be
        // unlikely since we have just checked it in ScriptEvaluator
        value = viewer.getBufferedReaderOrErrorMessageFromName(
            sg.getFileName(), null, true);
        if (value instanceof String) {
          Logger.error("Isosurface: could not open file " + sg.getFileName()
              + " -- " + value);
          return;
        }
        if (!(value instanceof BufferedReader))
          try {
            value = JmolBinary.getBufferedReader((BufferedInputStream) value,
                "ISO-8859-1");
          } catch (IOException e) {
            // ignore
          }
      }
    } else if ("atomIndex" == propertyName) {
      atomIndex = ((Integer) value).intValue();
    } else if ("center" == propertyName) {
      center.setT((P3) value);
    } else if ("colorRGB" == propertyName) {
      int rgb = ((Integer) value).intValue();
      defaultColix = C.getColix(rgb);
    } else if ("contour" == propertyName) {
      explicitContours = true;
    } else if ("functionXY" == propertyName) {
      //allowContourLines = false;
      if (sg.isStateDataRead())
        setScriptInfo(null); // for script DATA1
    } else if ("init" == propertyName) {
      newSg();
    } else if ("getSurfaceSets" == propertyName) {
      if (thisMesh != null) {
        thisMesh.jvxlData.thisSet = ((Integer) value).intValue();
        thisMesh.calculatedVolume = null;
        thisMesh.calculatedArea = null;
      }
    } else if ("localName" == propertyName) {
      value = viewer.getOutputStream((String) value, null);
      propertyName = "outputStream";
    } else if ("molecularOrbital" == propertyName) {
      if (value instanceof Integer) {
        moNumber = ((Integer) value).intValue();
        moLinearCombination = null;
      } else {
        moLinearCombination = (float[]) value;
        moNumber = 0;
      }
      if (!isColorExplicit)
        isPhaseColored = true;
    } else if ("phase" == propertyName) {
      isPhaseColored = true;
    } else if ("plane" == propertyName) {
      //allowContourLines = false;
    } else if ("pocket" == propertyName) {
      // Boolean pocket = (Boolean) value;
      // lighting = (pocket.booleanValue() ? JmolConstants.FULLYLIT
      //     : JmolConstants.FRONTLIT);
    } else if ("scale3d" == propertyName) {
      scale3d = ((Float) value).floatValue();
      if (thisMesh != null) {
        thisMesh.scale3d = thisMesh.jvxlData.scale3d = scale3d;
        thisMesh.altVertices = null;
      }
    } else if ("title" == propertyName) {
      if (value instanceof String && "-".equals(value))
        value = null;
      setPropertySuper(propertyName, value, bs);
      value = title;
    } else if ("withinPoints" == propertyName) {
      Object[] o = (Object[]) value;
      withinDistance2 = ((Float) o[0]).floatValue();
      isWithinNot = (withinDistance2 < 0);
      withinDistance2 *= withinDistance2;
      withinPoints = (JmolList<P3>) o[3];
      if (withinPoints.size() == 0)
        withinPoints = viewer.getAtomPointVector((BS) o[2]);
    } else if (("nci" == propertyName || "orbital" == propertyName)
        && sg != null) {
      sg.getParams().testFlags = (viewer.getTestFlag(2) ? 2 : 0);
    } else if ("solvent" == propertyName) {
        sg.getParams().testFlags = (viewer.getTestFlag(1) ? 1 : 0);
    }

    // surface Export3D only (return TRUE) or shared (return FALSE)

    if (sg != null && sg.setParameter(propertyName, value, bs)) {
      if (sg.isValid())
        return;
      propertyName = "delete";
    }

    // ///////////// isosurface LAST, shared

    if ("init" == propertyName) {
      explicitID = false;
      scriptAppendix = "";
      String script = (value instanceof String ? (String) value : null);
      int pt = (script == null ? -1 : script.indexOf("# ID="));
      actualID = (pt >= 0 ? Parser.getQuotedStringAt(script, pt) : null);
      setPropertySuper("thisID", MeshCollection.PREVIOUS_MESH_ID, null);
      if (script != null && !(iHaveBitSets = getScriptBitSets(script, null)))
        sg.setParameter("select", bs);
      initializeIsosurface();
      sg.setModelIndex(isFixed ? -1 : modelIndex);
      return;
    }

    if ("clear" == propertyName) {
      discardTempData(true);
      return;
    }

    /*
     * if ("background" == propertyName) { boolean doHide = !((Boolean)
     * value).booleanValue(); if (thisMesh != null) thisMesh.hideBackground =
     * doHide; else { for (int i = meshCount; --i >= 0;)
     * meshes[i].hideBackground = doHide; } return; }
     */

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      int firstAtomDeleted = ((int[]) ((Object[]) value)[2])[1];
      int nAtomsDeleted = ((int[]) ((Object[]) value)[2])[2];
      for (int i = meshCount; --i >= 0;) {
        Mesh m = meshes[i];
        if (m == null)
          continue;
        if (m.connections != null) {
          int iAtom = m.connections[0];
          if (iAtom >= firstAtomDeleted + nAtomsDeleted)
            m.connections[0] = iAtom - nAtomsDeleted;
          else if (iAtom >= firstAtomDeleted)
            m.connections = null;
        }
        m.connections = null; // just no way to 
        if (m.modelIndex == modelIndex) {
          meshCount--;
          if (m == currentMesh)
            currentMesh = thisMesh = null;
          meshes = isomeshes = (IsosurfaceMesh[]) ArrayUtil.deleteElements(
              meshes, i, 1);
        } else if (m.modelIndex > modelIndex) {
          m.modelIndex--;
          if (m.atomIndex >= firstAtomDeleted)
            m.atomIndex -= nAtomsDeleted;
        }
      }
      return;
    }

    // processing by meshCollection:
    setPropertySuper(propertyName, value, bs);
  }

  protected void slabPolygons(Object[] slabInfo) {
    thisMesh.slabPolygons(slabInfo, false);
    thisMesh.reinitializeLightingAndColor(viewer);
  }

  private void setPropertySuper(String propertyName, Object value, BS bs) {
    if (propertyName == "thisID" && currentMesh != null 
        && currentMesh.thisID.equals(value)) {
      checkExplicit((String) value);
      return;
    }
    currentMesh = thisMesh;
    setPropMC(propertyName, value, bs);
    thisMesh = (IsosurfaceMesh) currentMesh;
    jvxlData = (thisMesh == null ? null : thisMesh.jvxlData);
    if (sg != null)
      sg.setJvxlData(jvxlData);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean getPropertyData(String property, Object[] data) {
    if (property == "colorEncoder") {
      IsosurfaceMesh mesh = (IsosurfaceMesh) getMesh((String) data[0]);
      if (mesh == null || (data[1] = mesh.colorEncoder) == null)
        return false;
      return true;
    }
    if (property == "intersectPlane") {
      IsosurfaceMesh mesh = (IsosurfaceMesh) getMesh((String) data[0]);
      if (mesh == null)
        return false;
      data[3] = Integer.valueOf(mesh.modelIndex);
      mesh.getIntersection(0, (Point4f) data[1], null, (JmolList<P3[]>) data[2], null, null, null, false, false, T.plane, false);
      return true;
    }
    if (property == "getBoundingBox") {
      String id = (String) data[0];
      IsosurfaceMesh m = (IsosurfaceMesh) getMesh(id);
      if (m == null || m.vertices == null)
        return false;
      data[2] = m.jvxlData.boundingBox;
      if (m.mat4 != null) {
        P3[] d = new P3[2];
        d[0] = P3.newP(m.jvxlData.boundingBox[0]);
        d[1] = P3.newP(m.jvxlData.boundingBox[1]);
        V3 v = new V3();
        m.mat4.get(v);
        d[0].add(v);
        d[1].add(v);
        data[2] = d;
      }
      return true;
    }
    if (property == "unitCell") {
      IsosurfaceMesh m = (IsosurfaceMesh) getMesh((String) data[0]);
      return (m != null && (data[1] = m.getUnitCell()) != null);
    }
    if (property == "getCenter") {
      int index = ((Integer)data[1]).intValue();
      if (index == Integer.MIN_VALUE) {
        String id = (String) data[0];
        IsosurfaceMesh m = (IsosurfaceMesh) getMesh(id);
        if (m == null || m.vertices == null)
          return false;
        P3 p = P3.newP(m.jvxlData.boundingBox[0]);
        p.add(m.jvxlData.boundingBox[1]);
        p.scale(0.5f);
        if (m.mat4 != null) {
          V3 v = new V3();
          m.mat4.get(v);
          p.add(v);
        }
        data[2] = p;
        return true;
      }
      // continue to super
    }

    return getPropDataMC(property, data);
  }

  @Override
  public Object getProperty(String property, int index) {
    return getPropI(property);
  }

  protected Object getPropI(String property) {
    Object ret = getPropMC(property);
    if (ret != null)
      return ret;
    if (property == "dataRange")
      return (thisMesh == null || jvxlData.jvxlPlane != null 
          && thisMesh.colorEncoder == null 
          ? null 
              : new float[] {
          jvxlData.mappedDataMin, jvxlData.mappedDataMax,
          (jvxlData.isColorReversed ? jvxlData.valueMappedToBlue : jvxlData.valueMappedToRed),
          (jvxlData.isColorReversed ? jvxlData.valueMappedToRed : jvxlData.valueMappedToBlue)});
    if (property == "moNumber")
      return Integer.valueOf(moNumber);
    if (property == "moLinearCombination")
      return moLinearCombination;
    if (property == "nSets")
      return Integer.valueOf(thisMesh == null ? 0 : thisMesh.nSets);
    if (property == "area")
      return (thisMesh == null ? Float.valueOf(Float.NaN) : calculateVolumeOrArea(true));
    if (property == "volume")
      return (thisMesh == null ? Float.valueOf(Float.NaN) : calculateVolumeOrArea(false));
    if (thisMesh == null)
      return null;//"no current isosurface";
    if (property == "cutoff")
      return Float.valueOf(jvxlData.cutoff);
    if (property == "minMaxInfo")
      return new float[] { jvxlData.dataMin, jvxlData.dataMax };
    if (property == "plane")
      return jvxlData.jvxlPlane;
    if (property == "contours")
      return thisMesh.getContours();
    if (property == "jvxlDataXml" || property == "jvxlMeshXml") {
      MeshData meshData = null;
      jvxlData.slabInfo = null;
      if (property == "jvxlMeshXml" || jvxlData.vertexDataOnly || thisMesh.bsSlabDisplay != null && thisMesh.bsSlabGhost == null) {
        meshData = new MeshData();
        fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
        meshData.polygonColorData = getPolygonColorData(meshData.polygonCount, meshData.polygonColixes, meshData.bsSlabDisplay);
      } else if (thisMesh.bsSlabGhost != null) {
        jvxlData.slabInfo = thisMesh.slabOptions.toString();
      }
      SB sb = new SB();
      getMeshCommand(sb, thisMesh.index);
      thisMesh.setJvxlColorMap(true);
      return JvxlCoder.jvxlGetFile(jvxlData, meshData, title, "", true, 1, sb.toString(), null);
    }
    if (property == "jvxlFileInfo") {
      thisMesh.setJvxlColorMap(false);
      return JvxlCoder.jvxlGetInfo(jvxlData);
    }
    if (property == "command") {
      String key = previousMeshID.toUpperCase();
      boolean isWild = TextFormat.isWild(key);
      SB sb = new SB();
      for (int i = meshCount; --i >= 0;) {
        String id = meshes[i].thisID.toUpperCase();
        if (id.equals(key) || isWild && TextFormat.isMatch(id, key, true, true))
            getMeshCommand(sb, i);
      }
      return sb.toString();
    }
    return null;
  }

  private Object calculateVolumeOrArea(boolean isArea) {
    if (isArea) {
      if (thisMesh.calculatedArea != null)
        return thisMesh.calculatedArea;
    } else {
      if (thisMesh.calculatedVolume != null)
        return thisMesh.calculatedVolume;
    }
    MeshData meshData = new MeshData();
    fillMeshData(meshData, MeshData.MODE_GET_VERTICES, null);
    meshData.nSets = thisMesh.nSets;
    meshData.vertexSets = thisMesh.vertexSets;
    if (!isArea && thisMesh.jvxlData.colorDensity) {
      float f = thisMesh.jvxlData.voxelVolume;
      f *= (thisMesh.bsSlabDisplay == null ? thisMesh.vertexCount : thisMesh.bsSlabDisplay.cardinality());
      return  thisMesh.calculatedVolume = Float.valueOf(f); 
    }
    Object ret = meshData.calculateVolumeOrArea(thisMesh.jvxlData.thisSet, isArea, false);
    if (isArea)
      thisMesh.calculatedArea = ret;
    else
      thisMesh.calculatedVolume = ret;
    return ret;
  }

  public static String getPolygonColorData(int ccount, short[] colixes, BS bsSlabDisplay) {
    if (colixes == null)
      return null;
    SB list1 = new SB();
    int count = 0;
    short colix = 0;
    boolean done = false;
    for (int i = 0; i < ccount || (done = true) == true; i++) {
      if (!done && bsSlabDisplay != null && !bsSlabDisplay.get(i))
        continue;
      if (done || colixes[i] != colix) {
        if (count != 0)
          list1.append(" ").appendI(count).append(" ").appendI(
              (colix == 0 ? 0 : C.getArgb(colix)));
        if (done)
          break;
        colix = colixes[i];
        count = 1;
      } else {
        count++;
      }
    }
    list1.append("\n");
    return list1.toString();
  }

  @Override
  public String getShapeState() {
    clean();
    SB sb = new SB();
    sb.append("\n");
    for (int i = 0; i < meshCount; i++)
      getMeshCommand(sb, i);
    return sb.toString();
  }

  private void getMeshCommand(SB sb, int i) {
    IsosurfaceMesh imesh = (IsosurfaceMesh) meshes[i];
    if (imesh == null || imesh.scriptCommand == null)
      return;
    String cmd = imesh.scriptCommand;
    int modelCount = viewer.getModelCount();
    if (modelCount > 1)
      appendCmd(sb, "frame " + viewer.getModelNumberDotted(imesh.modelIndex));
    cmd = TextFormat.simpleReplace(cmd, ";; isosurface map"," map");
    cmd = TextFormat.simpleReplace(cmd, "; isosurface map", " map");
    cmd = cmd.replace('\t', ' ');
    cmd = TextFormat.simpleReplace(cmd, ";#", "; #");
    int pt = cmd.indexOf("; #");
    if (pt >= 0)
      cmd = cmd.substring(0, pt);
    if (imesh.connections != null)
      cmd += " connect " + Escape.e(imesh.connections);
    cmd = TextFormat.trim(cmd, ";");
    if (imesh.linkedMesh != null)
      cmd += " LINK"; // for lcaoCartoon state
    appendCmd(sb, cmd);
    String id = myType + " ID " + Escape.eS(imesh.thisID);
    if (imesh.jvxlData.thisSet >= 0)
      appendCmd(sb, id + " set " + (imesh.jvxlData.thisSet + 1));
    if (imesh.mat4 != null)
      appendCmd(sb, id + " move " + Escape.matrixToScript(imesh.mat4));
    if (imesh.scale3d != 0)
      appendCmd(sb, id + " scale3d " + imesh.scale3d);
    if (imesh.jvxlData.slabValue != Integer.MIN_VALUE)
      appendCmd(sb, id + " slab " + imesh.jvxlData.slabValue);
    if (imesh.slabOptions != null)
      appendCmd(sb, imesh.slabOptions.toString());
    if (cmd.charAt(0) != '#') {
      if (allowMesh)
        appendCmd(sb, imesh.getState(myType));
      if (!imesh.isColorSolid && C.isColixTranslucent(imesh.colix))
        appendCmd(sb, "color " + myType + " " + getTranslucentLabel(imesh.colix));
      if (imesh.colorCommand != null && !imesh.colorCommand.equals("#inherit;")) {
        appendCmd(sb, imesh.colorCommand);
      }
      boolean colorArrayed = (imesh.isColorSolid && imesh.polygonColixes != null);
      if (imesh.isColorSolid && !colorArrayed) {
        appendCmd(sb, getColorCommandUnk(myType, imesh.colix, translucentAllowed));
      } else if (imesh.jvxlData.isBicolorMap && imesh.colorPhased) {
        appendCmd(sb, "color isosurface phase "
            + encodeColor(imesh.jvxlData.minColorIndex) + " "
            + encodeColor(imesh.jvxlData.maxColorIndex));
      }
      if (imesh.vertexColorMap != null)
        for (Map.Entry<String, BS> entry : imesh.vertexColorMap.entrySet()) {
          BS bs = entry.getValue();
          if (!bs.isEmpty())
            appendCmd(sb, "color " + myType + " " + Escape.eB(bs, true)
                + " " + entry.getKey());
        }
    }
  }

  
  private String script;

  private boolean getScriptBitSets(String script, BS[] bsCmd) {
    this.script = script;
    int i;
    iHaveModelIndex = false;
    modelIndex = -1;
    if (script != null && (i = script.indexOf("MODEL({")) >= 0) {
      int j = script.indexOf("})", i);
      if (j > 0) {
        BS bs = Escape.uB(script.substring(i + 3, j + 1));
        modelIndex = (bs == null ? -1 : bs.nextSetBit(0));
        iHaveModelIndex = (modelIndex >= 0);
      }
    }    
    if (script == null)
      return false;
    getCapSlabInfo(script);
    i = script.indexOf("# ({");
    if (i < 0)
      return false;
    int j = script.indexOf("})", i);
    if (j < 0)
      return false;
    BS bs = Escape.uB(script.substring(i + 2, j + 2));
    if (bsCmd == null)
      sg.setParameter("select", bs);
    else
      bsCmd[0] = bs;
    if ((i = script.indexOf("({", j)) < 0)
      return true;
    j = script.indexOf("})", i);
    if (j < 0) 
      return false;
      bs = Escape.uB(script.substring(i + 1, j + 1));
      if (bsCmd == null)
        sg.setParameter("ignore", bs);
      else
        bsCmd[1] = bs;
    if ((i = script.indexOf("/({", j)) == j + 2) {
      if ((j = script.indexOf("})", i)) < 0)
        return false;
      bs = Escape.uB(script.substring(i + 3, j + 1));
      if (bsCmd == null)
        viewer.setTrajectoryBs(bs);
      else
        bsCmd[2] = bs;
    }
    return true;
  }

  protected void getCapSlabInfo(String script) {
    int i = script.indexOf("# SLAB=");
    if (i >= 0)
      sg.setParameter("slab", MeshSurface.getCapSlabObject(Parser.getQuotedStringAt(script, i), false));
    i = script.indexOf("# CAP=");
    if (i >= 0)
      sg.setParameter("slab", MeshSurface.getCapSlabObject(Parser.getQuotedStringAt(script, i), true));
  }

  private boolean iHaveModelIndex;

  private void initializeIsosurface() {
    //System.out.println("isosurface initializing " + thisMesh);
    if (!iHaveModelIndex)
      modelIndex = viewer.getCurrentModelIndex();
    isFixed = (modelIndex < 0);
    if (modelIndex < 0)
      modelIndex = 0; // but note that modelIndex = -1
    // is critical for surfaceGenerator. Setting this equal to 
    // 0 indicates only surfaces for model 0.
    title = null;
    explicitContours = false;
    atomIndex = -1;
    colix = C.ORANGE;
    defaultColix = meshColix = 0;
    isPhaseColored = isColorExplicit = false;
    //allowContourLines = true; //but not for f(x,y) or plane, which use mesh
    center = P3.new3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    scale3d = 0;
    withinPoints = null;
    cutoffRange = null;
    displayWithinPoints = null;
    bsDisplay = null;
    linkedMesh = null;
    connections = null;
    initState();
  }

  private void initState() {
    associateNormals = true;
    sg.initState();
    //TODO   need to pass assocCutoff to sg
  }

  private void setMesh() {
    thisMesh.visible = true;
    if ((thisMesh.atomIndex = atomIndex) >= 0)
      thisMesh.modelIndex = viewer.getAtomModelIndex(atomIndex);
    else if (isFixed)
      thisMesh.modelIndex = -1;
    else if (modelIndex >= 0)
      thisMesh.modelIndex = modelIndex;
    else
      thisMesh.modelIndex = viewer.getCurrentModelIndex();
    thisMesh.scriptCommand = script;
    thisMesh.ptCenter.setT(center);
    thisMesh.scale3d = (thisMesh.jvxlData.jvxlPlane == null ? 0 : scale3d);
//    if (thisMesh.bsSlabDisplay != null)
//      thisMesh.jvxlData.vertexDataOnly = true;
//      thisMesh.bsSlabDisplay = thisMesh.jvxlData.bsSlabDisplay;
  }

  /*
   void checkFlags() {
   if (viewer.getTestFlag2())
   associateNormals = false;
   if (!logMessages)
   return;
   Logger.info("Isosurface using testflag2: no associative grouping = "
   + !associateNormals);
   Logger.info("IsosurfaceRenderer using testflag4: show vertex normals = "
   + viewer.getTestFlag4());
   Logger
   .info("For grid points, use: isosurface delete myiso gridpoints \"\"");
   }
   */

  protected void discardTempData(boolean discardAll) {
    if (!discardAll)
      return;
    title = null;
    if (thisMesh == null)
      return;
    thisMesh.surfaceSet = null;
  }

  ////////////////////////////////////////////////////////////////
  // default color stuff (deprecated in 11.2)
  ////////////////////////////////////////////////////////////////

  private short getDefaultColix() {
    if (defaultColix != 0)
      return defaultColix;
    if (!sg.isCubeData())
      return colix; // orange
    int argb = (sg.getCutoff() >= 0 ? JC.argbsIsosurfacePositive
        : JC.argbsIsosurfaceNegative);
    return C.getColix(argb);
  }

  ///////////////////////////////////////////////////
  ////  LCAO Cartoons  are sets of lobes ////

  private int nLCAO = 0;

  private void drawLcaoCartoon(V3 z, V3 x, V3 rotAxis, int nElectrons) {
    String lcaoCartoon = sg.setLcao();
    //really rotRadians is just one of these -- x, y, or z -- not all
    float rotRadians = rotAxis.x + rotAxis.y + rotAxis.z;
    defaultColix = C.getColix(sg.getColor(1));
    short colixNeg = C.getColix(sg.getColor(-1));
    V3 y = new V3();
    boolean isReverse = (lcaoCartoon.length() > 0 && lcaoCartoon.charAt(0) == '-');
    if (isReverse)
      lcaoCartoon = lcaoCartoon.substring(1);
    int sense = (isReverse ? -1 : 1);
    y.cross(z, x);
    if (rotRadians != 0) {
      AxisAngle4f a = new AxisAngle4f();
      if (rotAxis.x != 0)
        a.setVA(x, rotRadians);
      else if (rotAxis.y != 0)
        a.setVA(y, rotRadians);
      else
        a.setVA(z, rotRadians);
      Matrix3f m = new Matrix3f();
      m.setAA(a);
      m.transform(x);
      m.transform(y);
      m.transform(z);
    }
    if (thisMesh == null && nLCAO == 0)
      nLCAO = meshCount;
    String id = (thisMesh == null ? (nElectrons > 0 ? "lp" : "lcao") + (++nLCAO) + "_" + lcaoCartoon
        : thisMesh.thisID);
    if (thisMesh == null)
      allocMesh(id, null);
    if (lcaoCartoon.equals("px")) {
      thisMesh.thisID += "a";
      Mesh meshA = thisMesh;
      createLcaoLobe(x, sense, nElectrons);
      if (nElectrons > 0) 
        return;
      setProperty("thisID", id + "b", null);
      createLcaoLobe(x, -sense, nElectrons);
      thisMesh.colix = colixNeg;
      linkedMesh = thisMesh.linkedMesh = meshA;
      return;
    }
    if (lcaoCartoon.equals("py")) {
      thisMesh.thisID += "a";
      Mesh meshA = thisMesh;
      createLcaoLobe(y, sense, nElectrons);
      if (nElectrons > 0) 
        return;
      setProperty("thisID", id + "b", null);
      createLcaoLobe(y, -sense, nElectrons);
      thisMesh.colix = colixNeg;
      linkedMesh = thisMesh.linkedMesh = meshA;
      return;
    }
    if (lcaoCartoon.equals("pz")) {
      thisMesh.thisID += "a";
      Mesh meshA = thisMesh;
      createLcaoLobe(z, sense, nElectrons);
      if (nElectrons > 0) 
        return;
      setProperty("thisID", id + "b", null);
      createLcaoLobe(z, -sense, nElectrons);
      thisMesh.colix = colixNeg;
      linkedMesh = thisMesh.linkedMesh = meshA;
      return;
    }
    if (lcaoCartoon.equals("pza") 
        || lcaoCartoon.indexOf("sp") == 0 
        || lcaoCartoon.indexOf("d") == 0 
        || lcaoCartoon.indexOf("lp") == 0) {
      createLcaoLobe(z, sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pzb")) {
      createLcaoLobe(z, -sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pxa")) {
      createLcaoLobe(x, sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pxb")) {
      createLcaoLobe(x, -sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pya")) {
      createLcaoLobe(y, sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("pyb")) {
      createLcaoLobe(y, -sense, nElectrons);
      return;
    }
    if (lcaoCartoon.equals("spacefill") || lcaoCartoon.equals("cpk")) {
      createLcaoLobe(null, 2 * viewer.getAtomRadius(atomIndex), nElectrons);
      return;      
    }

    // assume s
    createLcaoLobe(null, 1, nElectrons);
    return;
  }

  private Point4f lcaoDir = new Point4f();

  private void createLcaoLobe(V3 lobeAxis, float factor, int nElectrons) {
    initState();
    if (Logger.debugging) {
      Logger.debug("creating isosurface ID " + thisMesh.thisID);
    }
    if (lobeAxis == null) {
      setProperty("sphere", Float.valueOf(factor / 2f), null);
    } else {
      lcaoDir.x = lobeAxis.x * factor;
      lcaoDir.y = lobeAxis.y * factor;
      lcaoDir.z = lobeAxis.z * factor;
      lcaoDir.w = 0.7f;
      setProperty(nElectrons == 2 ? "lp" : nElectrons == 1 ? "rad" : "lobe", 
          lcaoDir, null);
    }
    thisMesh.colix = defaultColix;
    setScriptInfo(null);
  }

  /////////////// meshDataServer interface /////////////////

  public void invalidateTriangles() {
    thisMesh.invalidatePolygons();
  }

  private double privateKey;
  
  public void setOutputStream(JmolDocument binaryDoc, OutputStream os) {
    binaryDoc.setOutputStream(os, viewer, privateKey);
  }

  public void fillMeshData(MeshData meshData, int mode, IsosurfaceMesh mesh) {
    if (meshData == null) {
      if (thisMesh == null)
        allocMesh(null, null);
      if (!thisMesh.isMerged)
        thisMesh.clear(myType, sg.getIAddGridPoints());
      thisMesh.connections = connections;
      thisMesh.colix = getDefaultColix();
      thisMesh.meshColix = meshColix;
      if (isPhaseColored || thisMesh.jvxlData.isBicolorMap)
        thisMesh.isColorSolid = false;
      return;
    }
    if (mesh == null)
      mesh = thisMesh;
    if (mesh == null)
      return;
    //System.out.println("isosurface _get " + mode + " " + MeshData.MODE_GET_VERTICES + " " + MeshData.MODE_PUT_VERTICES + " vc=" + mesh.vertexCount + " pc=" + mesh.polygonCount + " " + mesh +" " 
      //  + (mesh.bsSlabDisplay == null ? "" :      
        //" bscard=" + mesh.bsSlabDisplay.cardinality() +
        //" " + mesh.bsSlabDisplay.hashCode() + "  " + mesh.bsSlabDisplay));
    switch (mode) {
    case MeshData.MODE_GET_VERTICES:
      meshData.mergeVertexCount0 = mesh.mergeVertexCount0;
      meshData.vertices = mesh.vertices;
      meshData.vertexSource = mesh.vertexSource;
      meshData.vertexValues = mesh.vertexValues;
      meshData.vertexCount = mesh.vertexCount;
      meshData.vertexIncrement = mesh.vertexIncrement;
      meshData.polygonCount = mesh.polygonCount;
      meshData.polygonIndexes = mesh.polygonIndexes;
      meshData.polygonColixes = mesh.polygonColixes;
      meshData.bsSlabDisplay = mesh.bsSlabDisplay;
      meshData.bsSlabGhost = mesh.bsSlabGhost;
      meshData.slabColix = mesh.slabColix;
      meshData.slabMeshType = mesh.slabMeshType;
      meshData.polygonCount0 = mesh.polygonCount0;
      meshData.vertexCount0 = mesh.vertexCount0;
      meshData.slabOptions = mesh.slabOptions;
      return;
    case MeshData.MODE_GET_COLOR_INDEXES:
      if (mesh.vertexColixes == null
          || mesh.vertexCount > mesh.vertexColixes.length)
        mesh.vertexColixes = new short[mesh.vertexCount];
      meshData.vertexColixes = mesh.vertexColixes;
      //meshData.polygonIndexes = null;
      return;
    case MeshData.MODE_PUT_SETS:
      mesh.surfaceSet = meshData.surfaceSet;
      mesh.vertexSets = meshData.vertexSets;
      mesh.nSets = meshData.nSets;
      return;
    case MeshData.MODE_PUT_VERTICES:
      mesh.vertices = meshData.vertices;
      mesh.vertexValues = meshData.vertexValues;
      mesh.vertexCount = meshData.vertexCount;
      mesh.vertexIncrement = meshData.vertexIncrement;
      mesh.vertexSource = meshData.vertexSource;
      mesh.polygonCount = meshData.polygonCount;
      mesh.polygonIndexes = meshData.polygonIndexes;
      mesh.polygonColixes = meshData.polygonColixes;
      mesh.bsSlabDisplay = meshData.bsSlabDisplay;
      mesh.bsSlabGhost = meshData.bsSlabGhost;
      mesh.slabColix = meshData.slabColix;
      mesh.slabMeshType = meshData.slabMeshType;
      mesh.polygonCount0 = meshData.polygonCount0;
      mesh.vertexCount0 = meshData.vertexCount0;
      mesh.mergeVertexCount0 = meshData.mergeVertexCount0;
      mesh.slabOptions = meshData.slabOptions;
      return;
    }
  }

  public void notifySurfaceGenerationCompleted() {
    setMesh();
    setBsVdw();
    thisMesh.insideOut = sg.isInsideOut();
    thisMesh.vertexSource = sg.getVertexSource();
    thisMesh.spanningVectors = sg.getSpanningVectors();
    thisMesh.calculatedArea = null;
    thisMesh.calculatedVolume = null;
    // from JVXL file:
    Parameters params = sg.getParams();
    if (!thisMesh.isMerged)
      thisMesh.initialize(sg.isFullyLit() ? T.fullylit
        : T.frontlit, null, sg.getPlane());
    if (!params.allowVolumeRender)
      thisMesh.jvxlData.allowVolumeRender = false;
    thisMesh.setColorsFromJvxlData(sg.getParams().colorRgb);
    if (thisMesh.jvxlData.slabInfo != null)
      viewer.runScriptImmediately("isosurface " + thisMesh.jvxlData.slabInfo);
      
    if (sg.getParams().psi_monteCarloCount > 0)
      thisMesh.diameter = -1; // use set DOTSCALE
    
    
  }

  public void notifySurfaceMappingCompleted() {
    if (!thisMesh.isMerged) {
      thisMesh.initialize(sg.isFullyLit() ? T.fullylit
          : T.frontlit, null, sg.getPlane());
      thisMesh.setJvxlDataRendering();
    }
    setBsVdw();
    thisMesh.isColorSolid = false;
    thisMesh.colorDensity = jvxlData.colorDensity;
    thisMesh.colorEncoder = sg.getColorEncoder();
    thisMesh.getContours();
    if (thisMesh.jvxlData.nContours != 0 && thisMesh.jvxlData.nContours != -1)
      explicitContours = true;
    if (explicitContours && thisMesh.jvxlData.jvxlPlane != null)
      thisMesh.havePlanarContours = true;
    setPropertySuper("token", Integer.valueOf(explicitContours ? T.nofill : T.fill), null);
    setPropertySuper("token", Integer.valueOf(explicitContours ? T.contourlines : T.nocontourlines), null);
    JmolList<Object[]> slabInfo = sg.getSlabInfo();
    if (slabInfo != null) {
      thisMesh.slabPolygonsList(slabInfo, false);
      thisMesh.reinitializeLightingAndColor(viewer);
    }
    // may not be the final color scheme, though.
    thisMesh.setColorCommand();
  }

  private void setBsVdw() {
    BS bs = sg.geVdwBitSet();
    if (bs == null)
      return;
    if (thisMesh.bsVdw == null)
      thisMesh.bsVdw = new BS();
    thisMesh.bsVdw.or(bs);
  }

  public P3[] calculateGeodesicSurface(BS bsSelected,
                                            float envelopeRadius) {
    return viewer.calculateSurface(bsSelected, envelopeRadius);
  }

  /////////////  VertexDataServer interface methods ////////////////

  public int getSurfacePointIndexAndFraction(float cutoff, boolean isCutoffAbsolute,
                                  int x, int y, int z, P3i offset, int vA,
                                  int vB, float valueA, float valueB,
                                  P3 pointA, V3 edgeVector,
                                  boolean isContourType, float[] fReturn) {
    return 0;
  }

  private boolean associateNormals;

  public int addVertexCopy(P3 vertexXYZ, float value, int assocVertex) {
    if (cutoffRange != null && (value < cutoffRange[0] || value > cutoffRange[1]))
      return -1;
    return (withinPoints != null && !Mesh.checkWithin(vertexXYZ, withinPoints, withinDistance2, isWithinNot) ? -1
        : thisMesh.addVertexCopy(vertexXYZ, value, assocVertex,
        associateNormals));
  }

  public int addTriangleCheck(int iA, int iB, int iC, int check,
                              int check2, boolean isAbsolute, int color) {
   return (iA < 0 || iB < 0 || iC < 0 
       || isAbsolute && !MeshData.checkCutoff(iA, iB, iC, thisMesh.vertexValues)
       ? -1 : thisMesh.addTriangleCheck(iA, iB, iC, check, check2, color));
  }

  protected void setScriptInfo(String strCommand) {
    // also from lcaoCartoon
    String script = (strCommand == null ? sg.getScript() : strCommand);
    int pt = (script == null ? -1 : script.indexOf("; isosurface map"));
    if (pt == 0) {
      // remapping surface
      if (thisMesh.scriptCommand == null)
        return;
      pt = thisMesh.scriptCommand.indexOf("; isosurface map"); 
      if (pt >= 0)
        thisMesh.scriptCommand = thisMesh.scriptCommand.substring(0, pt);
      thisMesh.scriptCommand += script;
      return;
    }
    thisMesh.title = sg.getTitle();
    thisMesh.dataType = sg.getParams().dataType;
    thisMesh.scale3d = sg.getParams().scale3d;
    if (script != null) {
      if (script.charAt(0) == ' ') {
        script = myType + " ID " + Escape.eS(thisMesh.thisID) + script;
        pt = script.indexOf("; isosurface map");
      }
    }    
    if (pt > 0 && scriptAppendix.length() > 0)
      thisMesh.scriptCommand = script.substring(0, pt) + scriptAppendix + script.substring(pt);
    else
      thisMesh.scriptCommand = script + scriptAppendix;
    if (!explicitID && script != null && (pt = script.indexOf("# ID=")) >= 0)
      thisMesh.thisID = Parser.getQuotedStringAt(script, pt);
  }

  public void addRequiredFile(String fileName) {
    fileName = " # /*file*/\"" + fileName + "\"";
    if (scriptAppendix.indexOf(fileName) < 0)
    scriptAppendix += fileName;
  }

  private void setJvxlInfo() {
    if (sg.getJvxlData() != jvxlData || sg.getJvxlData() != thisMesh.jvxlData)
      jvxlData = thisMesh.jvxlData = sg.getJvxlData();
  }

  @Override
  public JmolList<Map<String, Object>> getShapeDetail() {
    JmolList<Map<String, Object>> V = new  JmolList<Map<String, Object>>();
    for (int i = 0; i < meshCount; i++) {
      Map<String, Object> info = new Hashtable<String, Object>();
      IsosurfaceMesh mesh = isomeshes[i];
      if (mesh == null || mesh.vertices == null 
          || mesh.vertexCount == 0 && mesh.polygonCount == 0)
        continue;
      addMeshInfo(mesh, info);
      V.addLast(info);
    }
    return V;
  }

  protected void addMeshInfo(IsosurfaceMesh mesh, Map<String, Object> info) {
    info.put("ID", (mesh.thisID == null ? "<noid>" : mesh.thisID));
    info.put("vertexCount", Integer.valueOf(mesh.vertexCount));
    if (mesh.calculatedVolume != null)
      info.put("volume", mesh.calculatedVolume);
    if (mesh.calculatedArea != null)
      info.put("area", mesh.calculatedArea);
    if (mesh.ptCenter.x != Float.MAX_VALUE)
      info.put("center", mesh.ptCenter);
    if (mesh.mat4 != null)
      info.put("mat4", mesh.mat4);
    if (mesh.scale3d != 0)
      info.put("scale3d", Float.valueOf(mesh.scale3d));
    info.put("xyzMin", mesh.jvxlData.boundingBox[0]);
    info.put("xyzMax", mesh.jvxlData.boundingBox[1]);
    String s = JvxlCoder.jvxlGetInfo(mesh.jvxlData);
    if (s != null)
      info.put("jvxlInfo", s.replace('\n', ' '));
    info.put("modelIndex", Integer.valueOf(mesh.modelIndex));
    info.put("color", ColorUtil.colorPointFromInt2(C
        .getArgb(mesh.colix)));
    if (mesh.colorEncoder != null)
      info.put("colorKey", mesh.colorEncoder.getColorKey());
    if (mesh.title != null)
      info.put("title", mesh.title);
    if (mesh.jvxlData.contourValues != null
        || mesh.jvxlData.contourValuesUsed != null)
      info.put("contours", mesh.getContourList(viewer));
  }

  public float[] getPlane(int x) {
    // only for surface readers
    return null;
  }
  
  public float getValue(int x, int y, int z, int ptyz) {
    return 0;
  }
  
  @Override
  public boolean checkObjectHovered(int x, int y, BS bsVisible) {
    if (keyXy != null && x >= keyXy[0] && y >= keyXy[1] && x < keyXy[2] && y < keyXy[3]) {
      hoverKey(x, y);
      return true;
    }
    if (!viewer.getDrawHover())
      return false;
    String s = findValue(x, y, false, bsVisible);
    if (s == null)
      return false;
    if (gdata.isDisplayAntialiased()) {
      //because hover rendering is done in FIRST pass only
      x <<= 1;
      y <<= 1;
    }      
    viewer.hoverOnPt(x, y, s, pickedMesh.thisID, pickedPt);
    return true;
  }

  private void hoverKey(int x, int y) {
    try {
      String s;
      float f = 1 - 1.0f * (y - keyXy[1]) / (keyXy[3] - keyXy[1]);
      if (thisMesh.showContourLines) {
        JmolList<Object>[] vContours = thisMesh.getContours();
        if (vContours == null) {
          if (thisMesh.jvxlData.contourValues == null)
            return;
          int i = (int) Math.floor(f * thisMesh.jvxlData.contourValues.length);
          if (i < 0 || i > thisMesh.jvxlData.contourValues.length)
            return;
          s = "" + thisMesh.jvxlData.contourValues[i];
        } else {
          int i = (int) Math.floor(f * vContours.length);
          if (i < 0 || i > vContours.length)
            return;
          s = ""
              + ((Float) vContours[i].get(JvxlCoder.CONTOUR_VALUE))
                  .floatValue();
        }
      } else {
        float g = thisMesh.colorEncoder.quantize(f, true);
        f = thisMesh.colorEncoder.quantize(f, false);
        s = "" + g + " - " + f;
      }
      if (gdata.isAntialiased()) {
        x <<= 1;
        y <<= 1;
      }
      viewer.hoverOnPt(x, y, s, null, null);
    } catch (Exception e) {
      // never mind!
    }
  }
  private final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 10 * 10;
  private final P3i ptXY = new P3i();
  public int[] keyXy;

  @Override
  public Map<String, Object> checkObjectClicked(int x, int y, int action, BS bsVisible) {
    if (!(viewer.getDrawPicking()))// || viewer.getNavigationMode() && viewer.getNavigateSurface())) 
       return null;
    if (!viewer.isBound(action, ActionManager.ACTION_pickIsosurface))
      return null;
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    if (gdata.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    int imesh = -1;
    int jmaxz = -1;
    int jminz = -1;
    int maxz = Integer.MIN_VALUE;
    int minz = Integer.MAX_VALUE;
    boolean pickFront = viewer.getDrawPicking(); // which must be true now
    for (int i = 0; i < meshCount; i++) {
      IsosurfaceMesh m = isomeshes[i];
      if (!isPickable(m, bsVisible))
        continue;
      P3[] centers = (pickFront ? m.vertices : m.getCenters());
      if (centers == null)
        continue;
      for (int j = centers.length; --j >= 0; ) {
          P3 v = centers[j];
          if (v == null)
            continue;
          int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
          if (d2 >= 0) {
            if (ptXY.z < minz) {
              if (pickFront)
                imesh = i;
              minz = ptXY.z;
              jminz = j;
            }
            if (ptXY.z > maxz) {
              if (!pickFront)
                imesh = i;
              maxz = ptXY.z;
              jmaxz = j;
            }
          }
      }
    }
    if (imesh < 0)
      return null;
    pickedMesh = isomeshes[imesh];
    setPropertySuper("thisID", pickedMesh.thisID, null);
    int iFace = pickedVertex = (pickFront ? jminz : jmaxz);
    P3 ptRet = new P3();
    ptRet.setT((pickFront ? pickedMesh.vertices[pickedVertex] : ((IsosurfaceMesh)pickedMesh).centers[iFace]));
    pickedModel = (short) pickedMesh.modelIndex;
//    if (pickFront) {
      setStatusPicked(-4, ptRet);
//    } else {
//      Vector3f vNorm = new Vector3f();
//      ((IsosurfaceMesh)pickedMesh).getFacePlane(iFace, vNorm);
//      // get normal to surface
//      vNorm.scale(-1);
//     // setHeading(ptRet, vNorm, 2);
//    }
    return getPickedPoint(ptRet, pickedModel);
  }

  private boolean isPickable(IsosurfaceMesh m, BS bsVisible) {
    return m.visibilityFlags != 0 && (m.modelIndex < 0
        || bsVisible.get(m.modelIndex)) && !C
        .isColixTranslucent(m.colix);
  }

//  private void navigate(int dz) {
//    if (thisMesh == null)
//      return;
//    Point3f navPt = Point3f.newP(viewer.getNavigationOffset());
//    Point3f toPt = new Point3f();
//    viewer.unTransformPoint(navPt, toPt);
//    navPt.z += dz;
//    viewer.unTransformPoint(navPt, toPt);
//    Point3f ptRet = new Point3f();
//    Vector3f vNorm = new Vector3f();
//    if (!getClosestNormal(thisMesh, toPt, ptRet, vNorm))
//      return;
//    Point3f pt2 = Point3f.newP(ptRet);
//    pt2.add(vNorm);
//    Point3f pt2s = new Point3f();
//    viewer.transformPt3f(pt2, pt2s);
//    if (pt2s.y > navPt.y)
//      vNorm.scale(-1);
//    setHeading(ptRet, vNorm, 0);     
//  }

//  private void setHeading(Point3f pt, Vector3f vNorm, int nSeconds) {
//    // general trick here is to save the original orientation, 
//    // then do all the changes and save the new orientation.
//    // Then just do a timed restore.
//
//    Orientation o1 = viewer.getOrientation();
//    
//    // move to point
//    viewer.navigatePt(pt);
//    
//    Point3f toPts = new Point3f();
//    
//    // get screen point along normal
//    Point3f toPt = Point3f.newP(vNorm);
//    //viewer.script("draw test2 vector " + Escape.escape(pt) + " " + Escape.escape(toPt));
//    toPt.add(pt);
//    viewer.transformPt3f(toPt, toPts);
//    
//    // subtract the navigation point to get a relative point
//    // that we can project into the xy plane by setting z = 0
//    Point3f navPt = Point3f.newP(viewer.getNavigationOffset());
//    toPts.sub(navPt);
//    toPts.z = 0;
//    
//    // set the directed angle and rotate normal into yz plane,
//    // less 20 degrees for the normal upward sloping view
//    float angle = Measure.computeTorsion(JmolConstants.axisNY, 
//        JmolConstants.center, JmolConstants.axisZ, toPts, true);
//    viewer.navigateAxis(JmolConstants.axisZ, angle);        
//    toPt.setT(vNorm);
//    toPt.add(pt);
//    viewer.transformPt3f(toPt, toPts);
//    toPts.sub(navPt);
//    angle = Measure.computeTorsion(JmolConstants.axisNY,
//        JmolConstants.center, JmolConstants.axisX, toPts, true);
//    viewer.navigateAxis(JmolConstants.axisX, 20 - angle);
//    
//    // save this orientation, restore the first, and then
//    // use TransformManager.moveto to smoothly transition to it
//    // a script is necessary here because otherwise the application
//    // would hang.
//    
//    navPt = Point3f.newP(viewer.getNavigationOffset());
//    if (nSeconds <= 0)
//      return;
//    viewer.saveOrientation("_navsurf");
//    o1.restore(0, true);
//    viewer.script("restore orientation _navsurf " + nSeconds);
//  }
  
//  private boolean getClosestNormal(IsosurfaceMesh m, Point3f toPt, Point3f ptRet, Vector3f normalRet) {
//    Point3f[] centers = m.getCenters();
//    float d;
//    float dmin = Float.MAX_VALUE;
//    int imin = -1;
//    for (int i = centers.length; --i >= 0; ) {
//      if ((d = centers[i].distance(toPt)) >= dmin)
//        continue;
//      dmin = d;
//      imin = i;
//    }
//    if (imin < 0)
//      return false;
//    getClosestPoint(m, imin, toPt, ptRet, normalRet);
//    return true;
//  }
  
//  private void getClosestPoint(IsosurfaceMesh m, int imin, Point3f toPt, Point3f ptRet,
//                               Vector3f normalRet) {
//    Point4f plane = m.getFacePlane(imin, normalRet);
//    float dist = Measure.distanceToPlane(plane, toPt);
//    normalRet.scale(-dist);
//    ptRet.setT(toPt);
//    ptRet.add(normalRet);
//    dist = Measure.distanceToPlane(plane, ptRet);
//    if (m.centers[imin].distance(toPt) < ptRet.distance(toPt))
//      ptRet.setT(m.centers[imin]);
//  }

  /**
   * 
   * @param x
   * @param y
   * @param isPicking IGNORED
   * @param bsVisible
   * @return  value found 
   */
  private String findValue(int x, int y, boolean isPicking, BS bsVisible) {
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    if (gdata.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    int pickedVertex = -1;
    JmolList<Object> pickedContour = null;
    IsosurfaceMesh m = null;
    for (int i = 0; i < meshCount; i++) {
      m = isomeshes[i];
      if (!isPickable(m, bsVisible))
        continue;
      JmolList<Object>[] vs = m.jvxlData.vContours;
      int ilast = (m.firstRealVertex < 0 ? 0 : m.firstRealVertex);
      int pickedJ = 0;
      if (vs != null && vs.length > 0) {
        for (int j = 0; j < vs.length; j++) {
          JmolList<Object> vc = vs[j];
          int n = vc.size() - 1;
          for (int k = JvxlCoder.CONTOUR_POINTS; k < n; k++) {
            P3 v = (P3) vc.get(k);
            int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
            if (d2 >= 0) {
              dmin2 = d2;
              pickedContour = vc;
              pickedJ = j;
              pickedMesh = m;
              pickedPt = v; 
            }
          }
        }
        if (pickedContour != null)
          return pickedContour.get(JvxlCoder.CONTOUR_VALUE).toString() + (Logger.debugging ? " " + pickedJ : "");
      } else if (m.jvxlData.jvxlPlane != null && m.vertexValues != null) {
        P3[] vertices = (m.mat4 == null && m.scale3d == 0 
            ? m.vertices : m.getOffsetVertices(m.jvxlData.jvxlPlane)); 
        for (int k = m.vertexCount; --k >= ilast;) {
          P3 v = vertices[k];
          int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
          if (d2 >= 0) {
            dmin2 = d2;
            pickedVertex = k;
            pickedMesh = m;
            pickedPt = v; 
          }
        }
        if (pickedVertex != -1)
          break;
      } else if (m.vertexValues != null) {
        for (int k = m.vertexCount; --k >= ilast;) {
          P3 v = m.vertices[k];
          int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
          if (d2 >= 0) {
            dmin2 = d2;
            pickedVertex = k;
            pickedMesh = m;
            pickedPt = v; 
          }
        }
        if (pickedVertex != -1)
          break;
      }
    }
    return (pickedVertex == -1 ? null 
        : (Logger.debugging ? "$" + m.thisID + "[" + (pickedVertex + 1) + "] "  
            + m.vertices[pickedVertex] + ": " : m.thisID + ": ") + m.vertexValues[pickedVertex]);
  }

  @Override
  public void merge(Shape shape) {
    super.merge(shape);
  }

  public String getCmd(int index){
    SB sb = new SB().append("\n");
//    result = this.isomeshes[index].scriptCommand;
    getMeshCommand(sb, index);
    return (sb.toString());
  }
}
