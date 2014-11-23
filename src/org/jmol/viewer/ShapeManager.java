/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-02-15 07:31:37 -0600 (Mon, 15 Feb 2010) $
 * $Revision: 12396 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development
 *
 * Contact: jmol-developers@lists.sf.net, jmol-developers@lists.sf.net
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
package org.jmol.viewer;


import java.util.Hashtable;

import java.util.Map;


import org.jmol.atomdata.RadiusData;
import org.jmol.constant.EnumPalette;
import org.jmol.constant.EnumVdw;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Group;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.util.BS;
import org.jmol.util.GData;
import org.jmol.util.JmolList;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Matrix4f;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.SB;
import org.jmol.util.V3;

public class ShapeManager {

  private GData gdata;
  private ModelSet modelSet;
  Shape[] shapes;
  public Viewer viewer;

  public ShapeManager(Viewer viewer, ModelSet modelSet) {
    // from ParallelProcessor
    this(viewer);
    resetShapes();
    loadDefaultShapes(modelSet);
  }

  ShapeManager(Viewer viewer) {
    this.viewer = viewer;
    gdata = viewer.getGraphicsData();
  }

  // public methods 
  
  public void findNearestShapeAtomIndex(int x, int y, Atom[] closest, BS bsNot) {
    if (shapes != null)
      for (int i = 0; i < shapes.length && closest[0] == null; ++i)
        if (shapes[i] != null)
          shapes[i].findNearestAtomIndex(x, y, closest, bsNot);
  }

  public Shape[] getShapes() {
    return shapes;
  }
  
  public Object getShapePropertyIndex(int shapeID, String propertyName, int index) {
    if (shapes == null || shapes[shapeID] == null)
      return null;
    viewer.setShapeErrorState(shapeID, "get " + propertyName);
    Object result = shapes[shapeID].getProperty(propertyName, index);
    viewer.setShapeErrorState(-1, null);
    return result;
  }

  public boolean getShapePropertyData(int shapeID, String propertyName, Object[] data) {
    if (shapes == null || shapes[shapeID] == null)
      return false;
    viewer.setShapeErrorState(shapeID, "get " + propertyName);
    boolean result = shapes[shapeID].getPropertyData(propertyName, data);
    viewer.setShapeErrorState(-1, null);
    return result;
  }

  /**
   * Returns the shape type index for a shape object given the object name.
   * @param objectName (string) string name of object
   * @return shapeType (int) integer corresponding to the shape type index
   *                   see ShapeManager.shapes[].
   */
  public int getShapeIdFromObjectName(String objectName) {
    if (shapes != null)
      for (int i = JC.SHAPE_MIN_SPECIAL; i < JC.SHAPE_MAX_MESH_COLLECTION; ++i)
        if (shapes[i] != null && shapes[i].getIndexFromName(objectName) >= 0)
          return i;
    return -1;
  }

  public void loadDefaultShapes(ModelSet newModelSet) {
    modelSet = newModelSet;
    if (shapes != null)
    for (int i = 0; i < shapes.length; ++i)
      if (shapes[i] != null)
        shapes[i].setModelSet(newModelSet);
    loadShape(JC.SHAPE_BALLS);
    loadShape(JC.SHAPE_STICKS);
    loadShape(JC.SHAPE_MEASURES);
    loadShape(JC.SHAPE_BBCAGE);
    loadShape(JC.SHAPE_UCCAGE);
  }

  public Shape loadShape(int shapeID) {
    if (shapes == null)
      return null;
    if (shapes[shapeID] != null)
      return shapes[shapeID];
    if (shapeID == JC.SHAPE_HSTICKS
        || shapeID == JC.SHAPE_SSSTICKS
        || shapeID == JC.SHAPE_STRUTS)
      return null;
    String className = JC.getShapeClassName(shapeID, false);
    try {
      Class<?> shapeClass = Class.forName(className);
      Shape shape = (Shape) shapeClass.newInstance();
      viewer.setShapeErrorState(shapeID, "allocate");
      shape.initializeShape(viewer, gdata, modelSet, shapeID);
      viewer.setShapeErrorState(-1, null);
      return shapes[shapeID] = shape;
    } catch (Exception e) {
      Logger.errorEx("Could not instantiate shape:" + className, e);
      return null;
    }
  }

  public void refreshShapeTrajectories(int baseModel, BS bs, Matrix4f mat) {
    Integer Imodel = Integer.valueOf(baseModel);
    BS bsModelAtoms = viewer.getModelUndeletedAtomsBitSet(baseModel);
    for (int i = 0; i < JC.SHAPE_MAX; i++)
      if (shapes[i] != null)
        setShapePropertyBs(i, "refreshTrajectories", new Object[] { Imodel, bs, mat }, bsModelAtoms);    
  }

  public void releaseShape(int shapeID) {
    if (shapes != null) 
      shapes[shapeID] = null;  
  }
  
  public void resetShapes() {
    if (!viewer.noGraphicsAllowed())
      shapes = new Shape[JC.SHAPE_MAX];
  }
  
  /**
   * @param shapeID
   * @param size in milliangstroms
   * @param rd
   * @param bsSelected
   */
  public void setShapeSizeBs(int shapeID, int size, RadiusData rd, BS bsSelected) {
    if (shapes == null)
      return;
    if (bsSelected == null && 
        (shapeID != JC.SHAPE_STICKS || size != Integer.MAX_VALUE))
      bsSelected = viewer.getSelectionSet(false);
    if (rd != null && rd.value != 0 && rd.vdwType == EnumVdw.TEMP)
      modelSet.getBfactor100Lo();
    viewer.setShapeErrorState(shapeID, "set size");
    if (rd == null ? size != 0 : rd.value != 0)
      loadShape(shapeID);
    if (shapes[shapeID] != null) {
      shapes[shapeID].setShapeSizeRD(size, rd, bsSelected);
    }
    viewer.setShapeErrorState(-1, null);
  }

  public void setLabel(String strLabel, BS bsSelection) {
    if (strLabel == null) {
      if (shapes[JC.SHAPE_LABELS] == null)
        return;
    } else {// force the class to load and display
      loadShape(JC.SHAPE_LABELS);
      setShapeSizeBs(JC.SHAPE_LABELS, 0, null, bsSelection);
    }
    setShapePropertyBs(JC.SHAPE_LABELS, "label", strLabel, bsSelection);
  }

  public void setShapePropertyBs(int shapeID, String propertyName, Object value,
                               BS bsSelected) {
    if (shapes == null || shapes[shapeID] == null)
      return;
    if (bsSelected == null)
      bsSelected = viewer.getSelectionSet(false);
    viewer.setShapeErrorState(shapeID, "set " + propertyName);
    shapes[shapeID].setProperty(propertyName.intern(), value, bsSelected);
    viewer.setShapeErrorState(-1, null);
  }

  // methods local to Viewer and other managers
  
  boolean checkFrankclicked(int x, int y) {
    Shape frankShape = shapes[JC.SHAPE_FRANK];
    return (frankShape != null && frankShape.wasClicked(x, y));
  }

  private final static int[] hoverable = {
    JC.SHAPE_ECHO, 
    JC.SHAPE_CONTACT,
    JC.SHAPE_ISOSURFACE,
    JC.SHAPE_DRAW,
    JC.SHAPE_FRANK,
  };
  
  private static int clickableMax = hoverable.length - 1;
  
  Map<String, Object> checkObjectClicked(int x, int y, int modifiers, BS bsVisible) {
    Shape shape;
    Map<String, Object> map = null;
    if (modifiers != 0
        && viewer.getBondPicking()
        && (map = shapes[JC.SHAPE_STICKS].checkObjectClicked(x, y,
            modifiers, bsVisible)) != null)
      return map;

    for (int i = 0; i < clickableMax; i++)
      if ((shape = shapes[hoverable[i]]) != null
          && (map = shape.checkObjectClicked(x, y, modifiers, bsVisible)) != null)
        return map;
    return null;
  }
 
  boolean checkObjectDragged(int prevX, int prevY, int x, int y, int modifiers,
                             BS bsVisible, int iShape) {
    boolean found = false;
    int n = (iShape > 0 ? iShape + 1 : JC.SHAPE_MAX);
    for (int i = iShape; !found && i < n; ++i)
      if (shapes[i] != null)
        found = shapes[i].checkObjectDragged(prevX, prevY, x, y, modifiers,
            bsVisible);
    return found;
  }

  boolean checkObjectHovered(int x, int y, BS bsVisible, boolean checkBonds) {
    Shape shape = shapes[JC.SHAPE_STICKS];
    if (checkBonds && shape != null
        && shape.checkObjectHovered(x, y, bsVisible))
      return true;
    for (int i = 0; i < hoverable.length; i++) {
      shape = shapes[hoverable[i]];
      if (shape != null && shape.checkObjectHovered(x, y, bsVisible))
        return true;
    }
    return false;
  }

  void deleteShapeAtoms(Object[] value, BS bs) {
    if (shapes != null)
      for (int j = 0; j < JC.SHAPE_MAX; j++)
        if (shapes[j] != null)
          setShapePropertyBs(j, "deleteModelAtoms", value, bs);
  }

  void deleteVdwDependentShapes(BS bs) {
    if (bs == null)
      bs = viewer.getSelectionSet(false);
    if (shapes[JC.SHAPE_ISOSURFACE] != null)
      shapes[JC.SHAPE_ISOSURFACE].setProperty("deleteVdw", null, bs);
    if (shapes[JC.SHAPE_CONTACT] != null)
      shapes[JC.SHAPE_CONTACT].setProperty("deleteVdw", null, bs);
  }
  
  float getAtomShapeValue(int tok, Group group, int atomIndex) {
    int iShape = JC.shapeTokenIndex(tok);
    if (iShape < 0 || shapes[iShape] == null) 
      return 0;
    int mad = shapes[iShape].getSize(atomIndex);
    if (mad == 0) {
      if ((group.shapeVisibilityFlags & shapes[iShape].myVisibilityFlag) == 0)
        return 0;
      mad = shapes[iShape].getSizeG(group);
    }
    return mad / 2000f;
  }

  void getObjectMap(Map<String, T> map, boolean withDollar) {
    if (shapes == null)
      return;
    Boolean bDollar = Boolean.valueOf(withDollar);
      for (int i = JC.SHAPE_MIN_SPECIAL; i < JC.SHAPE_MAX_MESH_COLLECTION; ++i)
          getShapePropertyData(i, "getNames", new Object[] { map , bDollar } );
  }

  Object getProperty(Object paramInfo) {
    if (paramInfo.equals("getShapes"))
      return shapes;
    return null;
  }

  private final BS bsRenderableAtoms = new BS();

  BS getRenderableBitSet() {
    return bsRenderableAtoms;
  }
  
  public Shape getShape(int i) {
    //RepaintManager
    return (shapes == null ? null : shapes[i]);
  }
  
  Map<String, Object> getShapeInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    SB commands = new SB();
    if (shapes != null)
      for (int i = 0; i < JC.SHAPE_MAX; ++i) {
        Shape shape = shapes[i];
        if (shape != null) {
          String shapeType = JC.shapeClassBases[i];
          JmolList<Map<String, Object>> shapeDetail = shape.getShapeDetail();
          if (shapeDetail != null)
            info.put(shapeType, shapeDetail);
        }
      }
    if (commands.length() > 0)
      info.put("shapeCommands", commands.toString());
    return info;
  }

  void mergeShapes(Shape[] newShapes) {
    if (newShapes == null)
      return;
    if (shapes == null)
      shapes = newShapes;
    else
      for (int i = 0; i < newShapes.length; ++i)
        if (newShapes[i] != null) {
          if (shapes[i] == null)
            loadShape(i);
          shapes[i].merge(newShapes[i]);
        }
  }

  void resetBioshapes(BS bsAllAtoms) {
    if (shapes == null)
      return;
    for (int i = 0; i < shapes.length; ++i)
      if (shapes[i] != null && shapes[i].isBioShape) {
        shapes[i].setModelSet(modelSet);
        shapes[i].setShapeSizeRD(0, null, bsAllAtoms);
        shapes[i].setProperty("color", EnumPalette.NONE, bsAllAtoms);
      }
  }

  void setAtomLabel(String strLabel, int i) {
    if (shapes == null)
      return;
    loadShape(JC.SHAPE_LABELS);
    shapes[JC.SHAPE_LABELS].setProperty("label:"+strLabel, Integer.valueOf(i), null);
  }
  
  void setModelVisibility() {
    if (shapes == null || shapes[JC.SHAPE_BALLS] == null)
      return;

    //named objects must be set individually
    //in the future, we might include here a BITSET of models rather than just a modelIndex

    // all these isTranslucent = f() || isTranslucent are that way because
    // in general f() does MORE than just check translucency. 
    // so isTranslucent = isTranslucent || f() would NOT work.

    BS bs = viewer.getVisibleFramesBitSet();
    
    //NOT balls (that is done later)
    for (int i = 1; i < JC.SHAPE_MAX; i++)
      if (shapes[i] != null)
        shapes[i].setVisibilityFlags(bs);
    // BALLS sets the JmolConstants.ATOM_IN_MODEL flag.
    shapes[JC.SHAPE_BALLS].setVisibilityFlags(bs);

    //set clickability -- this enables measures and such
    for (int i = 0; i < JC.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null)
        shape.setModelClickability();
    }
  }

  private final int[] navigationCrossHairMinMax = new int[4];

  public void finalizeAtoms(BS bsAtoms, P3 ptOffset) {
    if (bsAtoms != null) {
      // translateSelected operation
      P3 ptCenter = viewer.getAtomSetCenter(bsAtoms);
      P3 pt = new P3();
      viewer.transformPt3f(ptCenter, pt);
      pt.add(ptOffset);
      viewer.unTransformPoint(pt, pt);
      pt.sub(ptCenter);
      viewer.setAtomCoordRelative(pt, bsAtoms);
      ptOffset.set(0, 0, 0);
    }
    bsRenderableAtoms.clearAll();
    Atom[] atoms = modelSet.atoms;
    for (int i = modelSet.getAtomCount(); --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.getShapeVisibilityFlags() & JC.ATOM_IN_FRAME) == 0)
        continue;
      bsRenderableAtoms.set(i);
    }
  }

  public int[] transformAtoms() {
    V3[] vibrationVectors = modelSet.vibrationVectors;
    Atom[] atoms = modelSet.atoms;
    for (int i = bsRenderableAtoms.nextSetBit(0); i >= 0; i = bsRenderableAtoms.nextSetBit(i + 1)) {
      // note that this vibration business is not compatible with
      // PDB objects such as cartoons and traces, which 
      // use Cartesian coordinates, not screen coordinates
      Atom atom = atoms[i];
      P3i screen = (vibrationVectors != null && atom.hasVibration() ? viewer
          .transformPtVib(atom, vibrationVectors[i])
          : viewer.transformPt(atom));
      atom.screenX = screen.x;
      atom.screenY = screen.y;
      atom.screenZ = screen.z;
      atom.screenDiameter = viewer.scaleToScreen(screen.z, Math
          .abs(atom.madAtom));
    }
    if (viewer.getSlabEnabled()) {
      boolean slabByMolecule = viewer.getSlabByMolecule();
      boolean slabByAtom = viewer.getSlabByAtom();
      int minZ = gdata.getSlab();
      int maxZ = gdata.getDepth();
      if (slabByMolecule) {
        JmolMolecule[] molecules = modelSet.getMolecules();
        int moleculeCount = modelSet.getMoleculeCountInModel(-1);
        for (int i = 0; i < moleculeCount; i++) {
          JmolMolecule m = molecules[i];
          int j = 0;
          int pt = m.firstAtomIndex;
          if (!bsRenderableAtoms.get(pt))
            continue;
          for (; j < m.atomCount; j++, pt++)
            if (gdata.isClippedZ(atoms[pt].screenZ
                - (atoms[pt].screenDiameter >> 1)))
              break;
          if (j != m.atomCount) {
            pt = m.firstAtomIndex;
            for (int k = 0; k < m.atomCount; k++) {
              bsRenderableAtoms.clear(pt);
              atoms[pt++].screenZ = 0;
            }
          }
        }
      }
      for (int i = bsRenderableAtoms.nextSetBit(0); i >= 0; i = bsRenderableAtoms
          .nextSetBit(i + 1)) {
        Atom atom = atoms[i];
        if (gdata.isClippedZ(atom.screenZ
            - (slabByAtom ? atoms[i].screenDiameter >> 1 : 0))) {
          atom.setClickable(0);
          // note that in the case of navigation,
          // maxZ is set to Integer.MAX_VALUE.
          int r = (slabByAtom ? -1 : 1) * atom.screenDiameter / 2;
          if (atom.screenZ + r < minZ || atom.screenZ - r > maxZ
              || !gdata.isInDisplayRange(atom.screenX, atom.screenY)) {
            bsRenderableAtoms.clear(i);
          }
        }
      }
    }
    if (modelSet.getAtomCount() == 0 || !viewer.getShowNavigationPoint())
      return null;
    // set min/max for navigation crosshair rendering
    int minX = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;
    for (int i = bsRenderableAtoms.nextSetBit(0); i >= 0; i = bsRenderableAtoms
        .nextSetBit(i + 1)) {
      Atom atom = atoms[i];
      if (atom.screenX < minX)
        minX = atom.screenX;
      if (atom.screenX > maxX)
        maxX = atom.screenX;
      if (atom.screenY < minY)
        minY = atom.screenY;
      if (atom.screenY > maxY)
        maxY = atom.screenY;
    }
    navigationCrossHairMinMax[0] = minX;
    navigationCrossHairMinMax[1] = maxX;
    navigationCrossHairMinMax[2] = minY;
    navigationCrossHairMinMax[3] = maxY;
    return navigationCrossHairMinMax;
  }

  public void setModelSet(ModelSet modelSet) {
    this.modelSet = viewer.modelSet = modelSet;
  }

  /**
   * starting with Jmol 13.1.13, isosurfaces can use "property color" 
   * to inherit the color of the underlying atoms. This is then dynamic
   * 
   */
  public void checkInheritedShapes() {
    if (shapes[JC.SHAPE_ISOSURFACE] == null)
      return;
    setShapePropertyBs(JC.SHAPE_ISOSURFACE, "remapInherited", null, null);
  }

}
