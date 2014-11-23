/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

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

package org.jmol.modelset;

import org.jmol.util.JmolList;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import java.util.Map;
import java.util.Properties;


import org.jmol.api.AtomIndexIterator;
import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.CubeIterator;
import org.jmol.constant.EnumPalette;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumVdw;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;

import org.jmol.util.BS;
import org.jmol.util.BoxInfo;
import org.jmol.util.Elements;
import org.jmol.util.P3;
import org.jmol.util.Point4f;
import org.jmol.util.Quadric;
import org.jmol.util.JmolEdge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;
import org.jmol.util.Quaternion;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.TriangleData;
import org.jmol.util.V3;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;
import org.jmol.io.OutputStringBuilder;
import org.jmol.io.XmlUtil;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;
import org.jmol.viewer.StateManager.Orientation;

abstract public class ModelCollection extends BondCollection {

  /**
   * initial transfer of model data from old to new model set. Note that all new
   * models are added later, AFTER the old ones. This is very important, because
   * all of the old atom numbers must map onto the same numbers in the new model
   * set, or the state script will not run properly, among other problems.
   * 
   * @param mergeModelSet
   */
  protected void mergeModelArrays(ModelSet mergeModelSet) {
    atoms = mergeModelSet.atoms;
    bonds = mergeModelSet.bonds;
    stateScripts = mergeModelSet.stateScripts;
    proteinStructureTainted = mergeModelSet.proteinStructureTainted;
    thisStateModel = -1;
    bsSymmetry = mergeModelSet.bsSymmetry;
    modelFileNumbers = mergeModelSet.modelFileNumbers; // file * 1000000 + modelInFile (1-based)
    modelNumbersForAtomLabel = mergeModelSet.modelNumbersForAtomLabel;
    modelNames = mergeModelSet.modelNames;
    modelNumbers = mergeModelSet.modelNumbers;
    frameTitles = mergeModelSet.frameTitles;
    mergeAtomArrays(mergeModelSet);
  }

  @Override
  protected void releaseModelSet() {
    /*
     * Probably unnecessary, but here for general accounting.
     * 
     * I added this when I was trying to track down a memory bug.
     * I know that you don't have to do this, but I was concerned
     * that somewhere in this mess was a reference to modelSet. 
     * As it turns out, it was in models[i] (which was not actually
     * nulled but, rather, transferred to the new model set anyway).
     * Quite amazing that that worked at all, really. Some models were
     * referencing the old modelset, some the new. Yeiks!
     * 
     * Bob Hanson 11/7/07
     * 
     */
    models = null;
    bsSymmetry = null;
    bsAll = null;
    unitCells = null;
    super.releaseModelSet();
  }

  protected BS bsSymmetry;

  public String modelSetName;
  public Model[] models = new Model[1];
  public int modelCount;
  public SymmetryInterface[] unitCells;
  public boolean haveUnitCells;  
  
  public SymmetryInterface getUnitCell(int modelIndex) {
    if (!haveUnitCells || modelIndex < 0 || modelIndex >= modelCount)
      return null;
    if (models[modelIndex].simpleCage != null)
      return models[modelIndex].simpleCage;
    return (unitCells == null || modelIndex >= unitCells.length
        || !unitCells[modelIndex].haveUnitCell() ? null : unitCells[modelIndex]);
  }

  public void setModelCage(int modelIndex, SymmetryInterface simpleCage) {
    if (modelIndex < 0 || modelIndex >= modelCount)
      return;
    models[modelIndex].simpleCage = simpleCage;
    haveUnitCells = true;
  }

  /**
   * 
   * @param type
   * @param plane
   * @param scale
   * @param modelIndex
   * @param flags
   *        1 -- edges only 2 -- triangles only 3 -- both
   * @return Vector
   */
  public JmolList<Object> getPlaneIntersection(int type, Point4f plane,
                                           float scale, int flags,
                                           int modelIndex) {
    P3[] pts = null;
    switch (type) {
    case T.unitcell:
      SymmetryInterface uc = getUnitCell(modelIndex);
      if (uc == null)
        return null;
      pts = uc.getCanonicalCopy(scale);
      break;
    case T.boundbox:
      pts = boxInfo.getCanonicalCopy(scale);
      break;
    }
    JmolList<Object> v = new  JmolList<Object>();
    v.addLast(pts);
    return TriangleData.intersectPlane(plane, v, flags);
  }

  protected int[] modelNumbers = new int[1]; // from adapter -- possibly PDB MODEL record; possibly modelFileNumber
  protected int[] modelFileNumbers = new int[1]; // file * 1000000 + modelInFile (1-based)
  protected String[] modelNumbersForAtomLabel = new String[1];
  protected String[] modelNames = new String[1];
  public String[] frameTitles = new String[1];

  public String getModelName(int modelIndex) {
    return modelCount < 1 ? "" : modelIndex >= 0 ? modelNames[modelIndex]
        : modelNumbersForAtomLabel[-1 - modelIndex];
  }

  public String getModelTitle(int modelIndex) {
    return (String) getModelAuxiliaryInfoValue(modelIndex, "title");
  }

  public String getModelFileName(int modelIndex) {
    return (String) getModelAuxiliaryInfoValue(modelIndex, "fileName");
  }

  public void setFrameTitle(BS bsFrames, Object title) {
    if (title instanceof String) {
      for (int i = bsFrames.nextSetBit(0); i >= 0; i = bsFrames
          .nextSetBit(i + 1))
        frameTitles[i] = (String) title;
    } else {
      String[] list = (String[]) title;
      for (int i = bsFrames.nextSetBit(0), n = 0; i >= 0; i = bsFrames
          .nextSetBit(i + 1))
        if (n < list.length)
          frameTitles[i] = list[n++];
    }
  }

  public String getFrameTitle(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount ? frameTitles[modelIndex]
        : "");
  }

  public String getModelNumberForAtomLabel(int modelIndex) {
    return modelNumbersForAtomLabel[modelIndex];
  }

  protected BS[] elementsPresent;

  protected boolean isXYZ;
  protected boolean isPDB;

  public Properties modelSetProperties;
  protected Map<String, Object> modelSetAuxiliaryInfo;

  protected void calculatePolymers(Group[] groups, int groupCount,
                                   int baseGroupIndex, BS modelsExcluded) {
    if (!isPDB)
      return;
    
    
    for (int i = 0; i < modelCount; i++)
      if ((modelsExcluded == null || !modelsExcluded.get(i))
          && models[i].isBioModel) {
        models[i].calculatePolymers(groups, groupCount, baseGroupIndex,
            modelsExcluded);
        return;
      }
  }

  /**
   * In versions earlier than 12.1.51, groups[] was a field of ModelCollection.
   * But this is not necessary, and it was wasting space. This method is only
   * called when polymers are recreated.
   * 
   * @return full array of groups in modelSet
   */
  public Group[] getGroups() {
    int n = 0;
    for (int i = 0; i < modelCount; i++)
      n += models[i].getGroupCount();
    Group[] groups = new Group[n];
    for (int i = 0, iGroup = 0; i < modelCount; i++)
      for (int j = 0; j < models[i].chainCount; j++)
        for (int k = 0; k < models[i].chains[j].groupCount; k++) {
          groups[iGroup] = models[i].chains[j].groups[k];
          groups[iGroup].groupIndex = iGroup;
          iGroup++;
        }
    return groups;
  }

  /**
   * deprecated due to multimodel issues, but required by an interface -- do NOT
   * remove.
   * 
   * @return just the first unit cell
   * 
   */
  public float[] getNotionalUnitcell() {
    SymmetryInterface c = getUnitCell(0);
    return (c == null ? null : c.getNotionalUnitCell());
  }

  //new way:

  protected boolean someModelsHaveSymmetry;
  protected boolean someModelsHaveAromaticBonds;
  protected boolean someModelsHaveFractionalCoordinates;

  public boolean setCrystallographicDefaults() {
    return !isPDB && someModelsHaveSymmetry
        && someModelsHaveFractionalCoordinates;
  }

  protected final P3 ptTemp = new P3();

  ////////////////////////////////////////////

  private final P3 averageAtomPoint = new P3();
  private boolean isBbcageDefault;
  private BS bboxModels;
  private BS bboxAtoms;
  private final BoxInfo boxInfo = new BoxInfo();
  {
    boxInfo.addBoundBoxPoint(P3.new3(-10, -10, -10));
    boxInfo.addBoundBoxPoint(P3.new3(10, 10, 10));
  }

  public P3 getAverageAtomPoint() {
    return averageAtomPoint;
  }

  public P3 getBoundBoxCenter(int modelIndex) {
    if (isJmolDataFrameForModel(modelIndex))
      return new P3();
    return boxInfo.getBoundBoxCenter();
  }

  public V3 getBoundBoxCornerVector() {
    return boxInfo.getBoundBoxCornerVector();
  }

  public Point3fi[] getBboxVertices() {
    return boxInfo.getBboxVertices();
  }

  public Map<String, Object> getBoundBoxInfo() {
    return boxInfo.getBoundBoxInfo();
  }

  public BS getBoundBoxModels() {
    return bboxModels;
  }

  public void setBoundBox(P3 pt1, P3 pt2, boolean byCorner,
                          float scale) {
    isBbcageDefault = false;
    bboxModels = null;
    bboxAtoms = null;
    boxInfo.setBoundBox(pt1, pt2, byCorner, scale);
  }

  public String getBoundBoxCommand(boolean withOptions) {
    if (!withOptions && bboxAtoms != null)
      return "boundbox " + Escape.e(bboxAtoms);
    ptTemp.setT(boxInfo.getBoundBoxCenter());
    V3 bbVector = boxInfo.getBoundBoxCornerVector();
    String s = (withOptions ? "boundbox " + Escape.eP(ptTemp) + " "
        + Escape.eP(bbVector) + "\n#or\n" : "");
    ptTemp.sub(bbVector);
    s += "boundbox corners " + Escape.eP(ptTemp) + " ";
    ptTemp.scaleAdd2(2, bbVector, ptTemp);
    float v = Math.abs(8 * bbVector.x * bbVector.y * bbVector.z);
    s += Escape.eP(ptTemp) + " # volume = " + v;
    return s;
  }

  public EnumVdw getDefaultVdwType(int modelIndex) {
    return (!models[modelIndex].isBioModel ? EnumVdw.AUTO_BABEL
        : models[modelIndex].hydrogenCount == 0 ? EnumVdw.AUTO_JMOL
            : EnumVdw.AUTO_BABEL); // RASMOL is too small
  }

  public boolean setRotationRadius(int modelIndex, float angstroms) {
    if (isJmolDataFrameForModel(modelIndex)) {
      models[modelIndex].defaultRotationRadius = angstroms;
      return false;
    }
    return true;
  }

  public float calcRotationRadius(int modelIndex, P3 center) {
    if (isJmolDataFrameForModel(modelIndex)) {
      float r = models[modelIndex].defaultRotationRadius;
      return (r == 0 ? 10 : r);
    }
    float maxRadius = 0;
    for (int i = atomCount; --i >= 0;) {
      if (isJmolDataFrameForAtom(atoms[i])) {
        modelIndex = atoms[i].modelIndex;
        while (i >= 0 && atoms[i].modelIndex == modelIndex)
          i--;
        continue;
      }
      Atom atom = atoms[i];
      float distAtom = center.distance(atom);
      float outerVdw = distAtom + getRadiusVdwJmol(atom);
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  public void calcBoundBoxDimensions(BS bs, float scale) {
    if (bs != null && bs.nextSetBit(0) < 0)
      bs = null;
    if (bs == null && isBbcageDefault || atomCount < 2)
      return;
    bboxModels = getModelBitSet(bboxAtoms = BSUtil.copy(bs), false);
    if (calcAtomsMinMax(bs, boxInfo) == atomCount)
      isBbcageDefault = true;
    if (bs == null) { // from modelLoader or reset
      averageAtomPoint.setT(getAtomSetCenter(null));
      if (unitCells != null)
        calcUnitCellMinMax();
    }
    boxInfo.setBbcage(scale);
  }

  public BoxInfo getBoxInfo(BS bs, float scale) {
    if (bs == null)
      return boxInfo;
    BoxInfo bi = new BoxInfo();
    calcAtomsMinMax(bs, bi);
    bi.setBbcage(scale);
    return bi;
  }

  public int calcAtomsMinMax(BS bs, BoxInfo boxInfo) {
    boxInfo.reset();
    int nAtoms = 0;
    boolean isAll = (bs == null);
    int i0 = (isAll ? atomCount - 1 : bs.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bs.nextSetBit(i + 1))) {
      nAtoms++;
      if (!isJmolDataFrameForAtom(atoms[i]))
        boxInfo.addBoundBoxPoint(atoms[i]);
    }
    return nAtoms;
  }

  private void calcUnitCellMinMax() {
    for (int i = 0; i < modelCount; i++) {
      if (!unitCells[i].getCoordinatesAreFractional())
        continue;
      P3[] vertices = unitCells[i].getUnitCellVertices();
      for (int j = 0; j < 8; j++)
        boxInfo.addBoundBoxPoint(vertices[j]);
    }
  }

  public float calcRotationRadiusBs(BS bs) {
    // Eval getZoomFactor
    P3 center = getAtomSetCenter(bs);
    float maxRadius = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom atom = atoms[i];
      float distAtom = center.distance(atom);
      float outerVdw = distAtom + getRadiusVdwJmol(atom);
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  /**
   * 
   * @param vAtomSets
   * @param addCenters
   * @return array of two lists of points, centers first if desired
   */

  public P3[][] getCenterAndPoints(JmolList<BS[]> vAtomSets,
                                        boolean addCenters) {
    BS bsAtoms1, bsAtoms2;
    int n = (addCenters ? 1 : 0);
    for (int ii = vAtomSets.size(); --ii >= 0;) {
      BS[] bss = vAtomSets.get(ii);
      bsAtoms1 = bss[0];
      bsAtoms2 = bss[1];
      n += Math.min(bsAtoms1.cardinality(), bsAtoms2.cardinality());
    }
    P3[][] points = new P3[2][n];
    if (addCenters) {
      points[0][0] = new P3();
      points[1][0] = new P3();
    }
    for (int ii = vAtomSets.size(); --ii >= 0;) {
      BS[] bss = vAtomSets.get(ii);
      bsAtoms1 = bss[0];
      bsAtoms2 = bss[1];
      for (int i = bsAtoms1.nextSetBit(0), j = bsAtoms2.nextSetBit(0); i >= 0
          && j >= 0; i = bsAtoms1.nextSetBit(i + 1), j = bsAtoms2
          .nextSetBit(j + 1)) {
        points[0][--n] = atoms[i];
        points[1][n] = atoms[j];
        if (addCenters) {
          points[0][0].add(atoms[i]);
          points[1][0].add(atoms[j]);
        }
      }
    }
    if (addCenters) {
      points[0][0].scale(1f / (points[0].length - 1));
      points[1][0].scale(1f / (points[1].length - 1));
    }
    return points;
  }

  public P3 getAtomSetCenter(BS bs) {
    P3 ptCenter = P3.new3(0, 0, 0);
    int nPoints = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (!isJmolDataFrameForAtom(atoms[i])) {
          nPoints++;
          ptCenter.add(atoms[i]);
        }
      }
    if (nPoints > 0)
      ptCenter.scale(1.0f / nPoints);
    return ptCenter;
  }

  protected void setAPm(BS bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    setAPa(bs, tok, iValue, fValue, sValue, values, list);
    switch (tok) {
    case T.valence:
    case T.formalcharge:
      if (viewer.getSmartAromatic())
        assignAromaticBonds();
      break;
    }
  }

  public JmolList<StateScript> stateScripts = new  JmolList<StateScript>();
  /*
   * stateScripts are connect commands that must be executed in sequence.
   * 
   * What I fear is that in deleting models we must delete these connections,
   * and in deleting atoms, the bitsets may not be retrieved properly. 
   * 
   * 
   */
  private int thisStateModel = 0;

  public StateScript addStateScript(String script1, BS bsBonds,
                                    BS bsAtoms1, BS bsAtoms2,
                                    String script2, boolean addFrameNumber,
                                    boolean postDefinitions) {
    int iModel = viewer.getCurrentModelIndex();
    if (addFrameNumber) {
      if (thisStateModel != iModel)
        script1 = "frame "
            + (iModel < 0 ? "all #" + iModel : getModelNumberDotted(iModel))
            + ";\n  " + script1;
      thisStateModel = iModel;
    } else {
      thisStateModel = -1;
    }
    StateScript stateScript = new StateScript(thisStateModel, script1, bsBonds,
        bsAtoms1, bsAtoms2, script2, postDefinitions);
    if (stateScript.isValid()) {
      stateScripts.addLast(stateScript);
    }
    return stateScript;
  }

  public static class StateScript {
    private int modelIndex;
    private BS bsBonds;
    private BS bsAtoms1;
    private BS bsAtoms2;
    private String script1;
    private String script2;
    public boolean inDefinedStateBlock;

    StateScript(int modelIndex, String script1, BS bsBonds,
        BS bsAtoms1, BS bsAtoms2, String script2,
        boolean inDefinedStateBlock) {
      this.modelIndex = modelIndex;
      this.script1 = script1;
      this.bsBonds = BSUtil.copy(bsBonds);
      this.bsAtoms1 = BSUtil.copy(bsAtoms1);
      this.bsAtoms2 = BSUtil.copy(bsAtoms2);
      this.script2 = script2;
      this.inDefinedStateBlock = inDefinedStateBlock;
    }

    public boolean isValid() {
      return script1 != null && script1.length() > 0
          && (bsBonds == null || bsBonds.nextSetBit(0) >= 0)
          && (bsAtoms1 == null || bsAtoms1.nextSetBit(0) >= 0)
          && (bsAtoms2 == null || bsAtoms2.nextSetBit(0) >= 0);
    }

    @Override
    public String toString() {
      if (!isValid())
        return "";
      
      SB sb = SB.newS(script1);
      if (bsBonds != null)
        sb.append(" ").append(Escape.eB(bsBonds, false));
      if (bsAtoms1 != null)
        sb.append(" ").append(Escape.e(bsAtoms1));
      if (bsAtoms2 != null)
        sb.append(" ").append(Escape.e(bsAtoms2));
      if (script2 != null)
        sb.append(" ").append(script2);
      String s = sb.toString();
      if (!s.endsWith(";"))
        s += ";";
      return s;
    }

    public boolean isConnect() {
      return (script1.indexOf("connect") >= 0);
    }

    public boolean deleteAtoms(int modelIndex, BS bsBonds, BS bsAtoms) {
      //false return means delete this script
      if (modelIndex == this.modelIndex)
        return false;
      if (modelIndex > this.modelIndex) {
        //        this.modelIndex--;
        return true;
      }
      BSUtil.deleteBits(this.bsBonds, bsBonds);
      BSUtil.deleteBits(this.bsAtoms1, bsAtoms);
      BSUtil.deleteBits(this.bsAtoms2, bsAtoms);
      return isValid();
    }

    public void setModelIndex(int index) {
      modelIndex = index; // for creating data frames 
    }
  }

  /**
   * allows rebuilding of PDB structures; also accessed by ModelManager from
   * Eval
   * 
   * @param alreadyDefined
   *        set to skip calculation
   * @param asDSSP
   * @param doReport
   * @param dsspIgnoreHydrogen
   * @param setStructure
   * @param includeAlpha
   * @return report
   * 
   */
  protected String calculateStructuresAllExcept(BS alreadyDefined,
                                                boolean asDSSP,
                                                boolean doReport,
                                                boolean dsspIgnoreHydrogen,
                                                boolean setStructure,
                                                boolean includeAlpha) {
    freezeModels();
    String ret = "";
    BS bsModels = BSUtil.copyInvert(alreadyDefined, modelCount);
    //working here -- testing reset
    //TODO bsModels first for not setStructure, after that for setstructure....
    if (setStructure)
      setDefaultStructure(bsModels);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) {
      ret += models[i].calculateStructures(asDSSP, doReport,
          dsspIgnoreHydrogen, setStructure, includeAlpha);
    }
    if (setStructure) {
      setStructureIndexes();
    }
    return ret;
  }

  public void setDefaultStructure(BS bsModels) {
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      if (models[i].isBioModel && models[i].defaultStructure == null)
        models[i].defaultStructure = getProteinStructureState(
            models[i].bsAtoms, false, false, 0);
  }

  public void setProteinType(BS bs, EnumStructure type) {
    int monomerIndexCurrent = -1;
    int iLast = -1;
    BS bsModels = getModelBitSet(bs, false);
    setDefaultStructure(bsModels);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (iLast != i - 1)
        monomerIndexCurrent = -1;
      monomerIndexCurrent = atoms[i].group.setProteinStructureType(type,
          monomerIndexCurrent);
      int modelIndex = atoms[i].modelIndex;
      proteinStructureTainted = models[modelIndex].structureTainted = true;
      iLast = i = atoms[i].group.lastAtomIndex;
    }
    int[] lastStrucNo = new int[modelCount];
    for (int i = 0; i < atomCount;) {
      int modelIndex = atoms[i].modelIndex;
      if (!bsModels.get(modelIndex)) {
        i = models[modelIndex].firstAtomIndex + models[modelIndex].atomCount;
        continue;
      }
      iLast = atoms[i].getStrucNo();
      if (iLast < 1000 && iLast > lastStrucNo[modelIndex])
        lastStrucNo[modelIndex] = iLast;
      i = atoms[i].group.lastAtomIndex + 1;
    }
    for (int i = 0; i < atomCount;) {
      int modelIndex = atoms[i].modelIndex;
      if (!bsModels.get(modelIndex)) {
        i = models[modelIndex].firstAtomIndex + models[modelIndex].atomCount;
        continue;
      }
      if (atoms[i].getStrucNo() > 1000)
        atoms[i].group.setStrucNo(++lastStrucNo[modelIndex]);
      i = atoms[i].group.lastAtomIndex + 1;
    }
  }

  void freezeModels() {
    for (int iModel = modelCount; --iModel >= 0;)
      models[iModel].freeze();
  }

  public Map<EnumStructure, float[]> getStructureList() {
    return viewer.getStructureList();
  }

  public void setStructureList(Map<EnumStructure, float[]> structureList) {
    for (int iModel = modelCount; --iModel >= 0;)
      models[iModel].setStructureList(structureList);
  }

  public BS setConformation(BS bsAtoms) {
    BS bsModels = getModelBitSet(bsAtoms, false);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      models[i].setConformation(bsAtoms);
    return bsAtoms;
  }

  public BS getConformation(int modelIndex, int conformationIndex,
                                boolean doSet) {
    BS bs = new BS();
    for (int i = modelCount; --i >= 0;)
      if (i == modelIndex || modelIndex < 0) {
        String altLocs = getAltLocListInModel(i);
        int nAltLocs = getAltLocCountInModel(i);
        if (conformationIndex > 0 && conformationIndex >= nAltLocs)
          continue;
        BS bsConformation = viewer.getModelUndeletedAtomsBitSet(i);
        if (conformationIndex >= 0) {
          if (!models[i].getPdbConformation(bsConformation, conformationIndex))
            for (int c = nAltLocs; --c >= 0;)
              if (c != conformationIndex)
                bsConformation.andNot(getAtomBitsMDa(
                    T.spec_alternate, altLocs.substring(c, c + 1)));
        }
        if (bsConformation.nextSetBit(0) >= 0) {
          bs.or(bsConformation);
          if (doSet)
            models[i].setConformation(bsConformation);
        }
      }
    return bs;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getHeteroList(int modelIndex) {
    Map<String, String> htFull = new Hashtable<String, String>();
    boolean ok = false;
    for (int i = modelCount; --i >= 0;)
      if (modelIndex < 0 || i == modelIndex) {
        Map<String, String> ht = (Map<String, String>) getModelAuxiliaryInfoValue(i,
            "hetNames");
        if (ht == null)
          continue;
        ok = true;
        for (Map.Entry<String, String> entry : ht.entrySet()) {
          String key = entry.getKey();
          htFull.put(key, entry.getValue());
        }
      }
    return (ok ? htFull
        : (Map<String, String>) getModelSetAuxiliaryInfoValue("hetNames"));
  }

  public Properties getModelSetProperties() {
    return modelSetProperties;
  }

  public Map<String, Object> getModelSetAuxiliaryInfo() {
    return modelSetAuxiliaryInfo;
  }

  public String getModelSetProperty(String propertyName) {
    // no longer used in Jmol
    return (modelSetProperties == null ? null : modelSetProperties
        .getProperty(propertyName));
  }

  public Object getModelSetAuxiliaryInfoValue(String keyName) {
    // the preferred method now
    return (modelSetAuxiliaryInfo == null ? null : modelSetAuxiliaryInfo
        .get(keyName));
  }

  public boolean getModelSetAuxiliaryInfoBoolean(String keyName) {
    Map<String, Object> info = modelSetAuxiliaryInfo;
    return (info != null && info.containsKey(keyName) && ((Boolean) info
        .get(keyName)).booleanValue());    
  }

  /*
    int getModelSetAuxiliaryInfoInt(String keyName) {
      if (modelSetAuxiliaryInfo != null
          && modelSetAuxiliaryInfo.containsKey(keyName)) {
        return ((Integer) modelSetAuxiliaryInfo.get(keyName)).intValue();
      }
      return Integer.MIN_VALUE;
    }
  */

  public JmolList<P3[]> trajectorySteps;
  protected JmolList<V3[]> vibrationSteps;

  protected int mergeTrajectories(boolean isTrajectory) {
    if (trajectorySteps == null) {
      if (!isTrajectory)
        return 0;
      trajectorySteps = new  JmolList<P3[]>();
    }
    for (int i = trajectorySteps.size(); i < modelCount; i++)
      trajectorySteps.addLast(null);
    return modelCount;
  }

  public int getTrajectoryIndex(int modelIndex) {
    return models[modelIndex].trajectoryBaseIndex;
  }

  public boolean isTrajectory(int modelIndex) {
    return models[modelIndex].isTrajectory;
  }

  public boolean isTrajectoryMeasurement(int[] countPlusIndices) {
    if (countPlusIndices == null)
      return false;
    int count = countPlusIndices[0];
    int atomIndex;
    for (int i = 1; i <= count; i++)
      if ((atomIndex = countPlusIndices[i]) >= 0
          && models[atoms[atomIndex].modelIndex].isTrajectory)
        return true;
    return false;
  }

  public BS getModelBitSet(BS atomList, boolean allTrajectories) {
    BS bs = new BS();
    int modelIndex = 0;
    boolean isAll = (atomList == null);
    int i0 = (isAll ? 0 : atomList.nextSetBit(0));
    for (int i = i0; i >= 0 && i < atomCount; i = (isAll ? i + 1 : atomList
        .nextSetBit(i + 1))) {
      bs.set(modelIndex = atoms[i].modelIndex);
      if (allTrajectories) {
        int iBase = models[modelIndex].trajectoryBaseIndex;
        for (int j = 0; j < modelCount; j++)
          if (models[j].trajectoryBaseIndex == iBase)
            bs.set(j);
      }
      i = models[modelIndex].firstAtomIndex + models[modelIndex].atomCount - 1;
    }
    return bs;
  }

  /**
   * only some models can be iterated through. models for which
   * trajectoryBaseIndexes[i] != i are trajectories only
   * 
   * @param allowJmolData
   * @return bitset of models
   */
  public BS getIterativeModels(boolean allowJmolData) {
    BS bs = new BS();
    for (int i = 0; i < modelCount; i++) {
      if (!allowJmolData && isJmolDataFrameForModel(i))
        continue;
      if (models[i].trajectoryBaseIndex == i)
        bs.set(i);
    }
    return bs;
  }

  public boolean isTrajectorySubFrame(int i) {
    return (models[i].isTrajectory && models[i].trajectoryBaseIndex != i);
  }

  public BS selectDisplayedTrajectories(BS bs) {
    //when a trajectory is selected, the atom's modelIndex is
    //switched to that of the selected trajectory
    //even though the underlying model itself is not changed.
    for (int i = 0; i < modelCount; i++) {
      if (models[i].isTrajectory
          && atoms[models[i].firstAtomIndex].modelIndex != i)
        bs.clear(i);
    }
    return bs;
  }

  public void fillAtomData(AtomData atomData, int mode) {
    if ((mode & AtomData.MODE_FILL_MOLECULES) != 0) {
      getMolecules();
      atomData.bsMolecules = new BS[molecules.length];
      atomData.atomMolecule = new int[atomCount];
      BS bs;
      for (int i = 0; i < molecules.length; i++) {
        bs = atomData.bsMolecules[i] = molecules[i].atomList;
        for (int iAtom = bs.nextSetBit(0); iAtom >= 0; iAtom = bs
            .nextSetBit(iAtom + 1))
          atomData.atomMolecule[iAtom] = i;
      }
    }
    if ((mode & AtomData.MODE_GET_ATTACHED_HYDROGENS) != 0) {
      int[] nH = new int[1];
      atomData.hAtomRadius = viewer.getVanderwaalsMar(1) / 1000f;
      atomData.hAtoms = calculateHydrogens(atomData.bsSelected, nH, false,
          true, null);
      atomData.hydrogenAtomCount = nH[0];
      return;
    }
    if (atomData.modelIndex < 0)
      atomData.firstAtomIndex = (atomData.bsSelected == null ? 0 : Math.max(0,
          atomData.bsSelected.nextSetBit(0)));
    else
      atomData.firstAtomIndex = models[atomData.modelIndex].firstAtomIndex;
    atomData.lastModelIndex = atomData.firstModelIndex = (atomCount == 0 ? 0
        : atoms[atomData.firstAtomIndex].modelIndex);
    atomData.modelName = getModelNumberDotted(atomData.firstModelIndex);
    fillADa(atomData, mode);
  }

  public String getModelNumberDotted(int modelIndex) {
    return (modelCount < 1 || modelIndex >= modelCount || modelIndex < 0 ? ""
        : Escape.escapeModelFileNumber(modelFileNumbers[modelIndex]));
  }

  public int getModelNumber(int modelIndex) {
    if (modelIndex == Integer.MAX_VALUE)
      modelIndex = modelCount - 1;
    return modelNumbers[modelIndex];
  }

  public int getModelFileNumber(int modelIndex) {
    return modelFileNumbers[modelIndex];
  }

  public Properties getModelProperties(int modelIndex) {
    return models[modelIndex].properties;
  }

  public String getModelProperty(int modelIndex, String property) {
    Properties props = models[modelIndex].properties;
    return props == null ? null : props.getProperty(property);
  }

  public Map<String, Object> getModelAuxiliaryInfo(int modelIndex) {
    return (modelIndex < 0 ? null : models[modelIndex].auxiliaryInfo);
  }

  public void setModelAuxiliaryInfo(int modelIndex, Object key, Object value) {
    models[modelIndex].auxiliaryInfo.put((String) key, value);
  }

  public Object getModelAuxiliaryInfoValue(int modelIndex, String key) {
    if (modelIndex < 0) {
      return null;
    }
    return models[modelIndex].auxiliaryInfo.get(key);
  }

  protected boolean getModelAuxiliaryInfoBoolean(int modelIndex, String keyName) {
    Map<String, Object> info = models[modelIndex].auxiliaryInfo;
    return (info != null && info.containsKey(keyName) && ((Boolean) info
        .get(keyName)).booleanValue());
  }

  protected int getModelAuxiliaryInfoInt(int modelIndex, String keyName) {
    Map<String, Object> info = models[modelIndex].auxiliaryInfo;
    if (info != null && info.containsKey(keyName)) {
      return ((Integer) info.get(keyName)).intValue();
    }
    return Integer.MIN_VALUE;
  }

  public String getModelAtomProperty(Atom atom, String text) {
    Object data = getModelAuxiliaryInfoValue(atom.modelIndex, text);
    if (!(data instanceof Object[]))
      return "";
    Object[] sdata = (Object[]) data;
    int iatom = atom.index - models[atom.modelIndex].firstAtomIndex;
    return (iatom < sdata.length ? sdata[iatom].toString() : "");
  }

  public int getInsertionCountInModel(int modelIndex) {
    return models[modelIndex].nInsertions;
  }

  public String getModelFileType(int modelIndex) {
    return (String) getModelAuxiliaryInfoValue(modelIndex, "fileType");
  }

  public static int modelFileNumberFromFloat(float fDotM) {
    //only used in the case of select model = someVariable
    //2.1 and 2.10 will be ambiguous and reduce to 2.1  

    int file = (int) Math.floor(fDotM);
    int model = (int) Math.floor((fDotM - file + 0.00001) * 10000);
    while (model != 0 && model % 10 == 0)
      model /= 10;
    return file * 1000000 + model;
  }

  public int getAltLocCountInModel(int modelIndex) {
    return models[modelIndex].nAltLocs;
  }

  public int getChainCount(boolean addWater) {
    int chainCount = 0;
    for (int i = modelCount; --i >= 0;)
      chainCount += models[i].getChainCount(addWater);
    return chainCount;
  }

  public int getBioPolymerCount() {
    int polymerCount = 0;
    for (int i = modelCount; --i >= 0;)
      if (!isTrajectorySubFrame(i))
        polymerCount += models[i].getBioPolymerCount();
    return polymerCount;
  }

  public int getBioPolymerCountInModel(int modelIndex) {
    return (modelIndex < 0 ? getBioPolymerCount()
        : isTrajectorySubFrame(modelIndex) ? 0 : models[modelIndex]
            .getBioPolymerCount());
  }

  public void getPolymerPointsAndVectors(BS bs, JmolList<P3[]> vList) {
    boolean isTraceAlpha = viewer.getTraceAlpha();
    float sheetSmoothing = viewer.getSheetSmoothing();
    for (int i = 0; i < modelCount; ++i)
      models[i].getPolymerPointsAndVectors(bs, vList, isTraceAlpha,
          sheetSmoothing);
  }

  public void recalculateLeadMidpointsAndWingVectors(int modelIndex) {
    if (modelIndex < 0) {
      for (int i = 0; i < modelCount; i++)
        if (!isTrajectorySubFrame(i))
        models[i].recalculateLeadMidpointsAndWingVectors();
      return;
    }
    models[modelIndex].recalculateLeadMidpointsAndWingVectors();
  }

  public P3[] getPolymerLeadMidPoints(int iModel, int iPolymer) {
    return models[iModel].getPolymerLeadMidPoints(iPolymer);
  }

  public int getChainCountInModel(int modelIndex, boolean countWater) {
    if (modelIndex < 0)
      return getChainCount(countWater);
    return models[modelIndex].getChainCount(countWater);
  }

  public int getGroupCount() {
    int groupCount = 0;
    for (int i = modelCount; --i >= 0;)
      groupCount += models[i].getGroupCount();
    return groupCount;
  }

  public int getGroupCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return getGroupCount();
    return models[modelIndex].getGroupCount();
  }

  public void calcSelectedGroupsCount(BS bsSelected) {
    for (int i = modelCount; --i >= 0;)
      models[i].calcSelectedGroupsCount(bsSelected);
  }

  public void calcSelectedMonomersCount(BS bsSelected) {
    for (int i = modelCount; --i >= 0;)
      models[i].calcSelectedMonomersCount(bsSelected);
  }

  /**
   * These are not actual hydrogen bonds. They are N-O bonds in proteins and
   * nucleic acids The method is called by AminoPolymer and NucleicPolymer
   * methods, which are indirectly called by ModelCollection.autoHbond
   * 
   * @param bsA
   * @param bsB
   * @param vHBonds
   *        vector of bonds to fill; if null, creates the HBonds
   * @param nucleicOnly
   * @param nMax
   * @param dsspIgnoreHydrogens
   * @param bsHBonds 
   */

  public void calcRasmolHydrogenBonds(BS bsA, BS bsB,
                                      JmolList<Bond> vHBonds, boolean nucleicOnly,
                                      int nMax, boolean dsspIgnoreHydrogens,
                                      BS bsHBonds) {
    boolean isSame = (bsB == null || bsA.equals(bsB));
    for (int i = modelCount; --i >= 0;)
      if (models[i].isBioModel && models[i].trajectoryBaseIndex == i) {
        if (vHBonds == null) {
          models[i].clearRasmolHydrogenBonds(bsA);
          if (!isSame)
            models[i].clearRasmolHydrogenBonds(bsB);
        }
        models[i].getRasmolHydrogenBonds(bsA, bsB, vHBonds, nucleicOnly, nMax,
            dsspIgnoreHydrogens, bsHBonds);
      }
  }

  public void calculateStraightness() {
    if (getHaveStraightness())
      return;
    char ctype = 'S';//(viewer.getTestFlag3() ? 's' : 'S');
    char qtype = viewer.getQuaternionFrame();
    int mStep = viewer.getHelixStep();
    // testflag3 ON  --> preliminary: Hanson's original normal-based straightness
    // testflag3 OFF --> final: Kohler's new quaternion-based straightness
    for (int i = modelCount; --i >= 0;)
      models[i].calculateStraightness(viewer, ctype, qtype, mStep);
    setHaveStraightness(true);
  }

  public Quaternion[] getAtomGroupQuaternions(BS bsAtoms, int nMax,
                                              char qtype) {
    // run through list, getting quaternions. For simple groups, 
    // go ahead and take first three atoms
    // for PDB files, do not include NON-protein groups.
    int n = 0;
    JmolList<Quaternion> v = new  JmolList<Quaternion>();
    for (int i = bsAtoms.nextSetBit(0); i >= 0 && n < nMax; i = bsAtoms
        .nextSetBit(i + 1)) {
      Group g = atoms[i].group;
      Quaternion q = g.getQuaternion(qtype);
      if (q == null) {
        if (g.seqcode == Integer.MIN_VALUE)
          q = g.getQuaternionFrame(atoms); // non-PDB just use first three atoms
        if (q == null)
          continue;
      }
      n++;
      v.addLast(q);
      i = g.lastAtomIndex;
    }
    return v.toArray(new Quaternion[v.size()]);
  }

  /**
   * PDB or PQR only
   * 
   * @param bs
   *        selected atoms
   * @param sb
   *        StringXBuilder or BufferedWriter
   * @return PDB file data string
   */
  public String getPdbAtomData(BS bs, OutputStringBuilder sb) {
    if (atomCount == 0 || bs.nextSetBit(0) < 0)
      return "";
    if (sb == null)
      sb = new OutputStringBuilder(null);
    int iModel = atoms[bs.nextSetBit(0)].modelIndex;
    int iModelLast = -1;
    boolean isPQR = "PQR".equals(sb.type);
    String occTemp = "%6.2Q%6.2b          ";
    if (isPQR) {
      occTemp = "%8.4P%7.4V       ";
      float charge = 0;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        charge += atoms[i].getPartialCharge();
      sb.append(
          "REMARK   1 PQR file generated by Jmol " + Viewer.getJmolVersion())
          .append("\nREMARK   1 " + "created " + (new Date())).append(
              "\nREMARK   1 Forcefield Used: unknown\nREMARK   1").append(
              "\nREMARK   5").append(
              "\nREMARK   6 Total charge on this protein: " + charge
                  + " e\nREMARK   6\n");
    }
    int lastAtomIndex = bs.length() - 1;
    boolean showModels = (iModel != atoms[lastAtomIndex].modelIndex);
    SB sbCONECT = (showModels ? null : new SB());
    boolean isMultipleBondPDB = models[iModel].isPdbWithMultipleBonds;
    LabelToken[] tokens;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      if (showModels && a.modelIndex != iModelLast) {
        if (iModelLast != -1)
          sb.append("ENDMDL\n");
        iModelLast = a.modelIndex;
        sb.append("MODEL     " + (iModelLast + 1) + "\n");
      }
      String sa = a.getAtomName();
      boolean leftJustify = (a.getElementSymbol().length() == 2
          || sa.length() >= 4 || Character.isDigit(sa.charAt(0)));
      boolean isBiomodel = models[a.modelIndex].isBioModel;
      boolean isHetero = a.isHetero();
      if (!isBiomodel)
        tokens = (leftJustify ? LabelToken.compile(viewer,
            "HETATM%5.-5i %-4.4a%1AUNK %1c   1%1E   %8.3x%8.3y%8.3z" + occTemp,
            '\0', null) : LabelToken
            .compile(viewer,
                "HETATM%5.-5i  %-3.3a%1AUNK %1c   1%1E   %8.3x%8.3y%8.3z"
                    + occTemp, '\0', null)

        );
      else if (isHetero)
        tokens = (leftJustify ? LabelToken.compile(viewer,
            "HETATM%5.-5i %-4.4a%1A%3.-3n %1c%4.-4R%1E   %8.3x%8.3y%8.3z"
                + occTemp, '\0', null) : LabelToken.compile(viewer,
            "HETATM%5.-5i  %-3.3a%1A%3.-3n %1c%4.-4R%1E   %8.3x%8.3y%8.3z"
                + occTemp, '\0', null));
      else
        tokens = (leftJustify ? LabelToken.compile(viewer,
            "ATOM  %5.-5i %-4.4a%1A%3.-3n %1c%4.-4R%1E   %8.3x%8.3y%8.3z"
                + occTemp, '\0', null) : LabelToken.compile(viewer,
            "ATOM  %5.-5i  %-3.3a%1A%3.-3n %1c%4.-4R%1E   %8.3x%8.3y%8.3z"
                + occTemp, '\0', null));
      String XX = a.getElementSymbolIso(false).toUpperCase();
      sb.append(LabelToken.formatLabelAtomArray(viewer, a, tokens, '\0', null)).append(
          XX.length() == 1 ? " " + XX : XX.substring(0, 2)).append("  \n");
      if (!showModels && (!isBiomodel || isHetero || isMultipleBondPDB)) {
        Bond[] bonds = a.getBonds();
        if (bonds != null)
          for (int j = 0; j < bonds.length; j++) {
            int iThis = a.getAtomNumber();
            Atom a2 = bonds[j].getOtherAtom(a);
            if (!bs.get(a2.index))
              continue;
            int n = bonds[j].getCovalentOrder();
            if (n == 1 && isMultipleBondPDB && !isHetero)
              continue;
            int iOther = a2.getAtomNumber();
            switch (n) {
            case 2:
            case 3:
              if (iOther < iThis)
                continue; // only one entry in this case -- pseudo-PDB style
              //$FALL-THROUGH$
            case 1:
              sbCONECT.append("CONECT").append(
                  TextFormat.formatStringI("%5i", "i", iThis));
              for (int k = 0; k < n; k++)
                sbCONECT.append(TextFormat.formatStringI("%5i", "i", iOther));
              sbCONECT.appendC('\n');
              break;
            }
          }
      }
    }
    if (showModels)
      sb.append("ENDMDL\n");
    else
      sb.append(sbCONECT.toString());
    return sb.toString();
  }

  /* **********************
   * 
   * Jmol Data Frame methods
   * 
   *****************************/

  public String getPdbData(int modelIndex, String type, BS bsSelected,
                           Object[] parameters, OutputStringBuilder sb) {
    if (isJmolDataFrameForModel(modelIndex))
      modelIndex = getJmolDataSourceFrame(modelIndex);
    if (modelIndex < 0)
      return "";
    boolean isPDB = models[modelIndex].isBioModel;
    if (parameters == null && !isPDB)
      return null;
    Model model = models[modelIndex];
    if (sb == null)
      sb = new OutputStringBuilder(null);
    SB pdbCONECT = new SB();
    boolean isDraw = (type.indexOf("draw") >= 0);
    BS bsAtoms = null;
    BS bsWritten = new BS();
    char ctype = '\0';
    LabelToken[] tokens = LabelToken.compile(viewer,
        "ATOM  %-6i%4a%1A%3n %1c%4R%1E   ", '\0', null);
    if (parameters == null) {
      ctype = (type.length() > 11 && type.indexOf("quaternion ") >= 0 ? type
          .charAt(11) : 'R');
      model.getPdbData(viewer, type, ctype, isDraw, bsSelected, sb, tokens,
          pdbCONECT, bsWritten);
      bsAtoms = viewer.getModelUndeletedAtomsBitSet(modelIndex);
    } else {
      // plot property x y z....
      bsAtoms = (BS) parameters[0];
      float[] dataX = (float[]) parameters[1];
      float[] dataY = (float[]) parameters[2];
      float[] dataZ = (float[]) parameters[3];
      boolean haveZ = (dataZ != null);
      P3 minXYZ = (P3) parameters[4];
      P3 maxXYZ = (P3) parameters[5];
      P3 factors = (P3) parameters[6];
      P3 center = (P3) parameters[7];
      sb.append("REMARK   6 Jmol PDB-encoded data: ").append(type)
          .append(";\n");
      sb.append("REMARK   6 Jmol data").append(" min = ").append(
          Escape.eP(minXYZ)).append(" max = ")
          .append(Escape.eP(maxXYZ)).append(" unScaledXyz = xyz * ")
          .append(Escape.eP(factors)).append(" + ").append(
              Escape.eP(center)).append(";\n");
      String strExtra = "";
      Atom atomLast = null;
      for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
          .nextSetBit(i + 1), n++) {
        float x = dataX[n];
        float y = dataY[n];
        float z = (haveZ ? dataZ[n] : 0f);
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
          continue;
        Atom a = atoms[i];
        sb.append(LabelToken.formatLabelAtomArray(viewer, a, tokens, '\0', null));
        if (isPDB)
          bsWritten.set(i);
        sb.append(TextFormat.sprintf(
            "%-8.2f%-8.2f%-10.2f    %6.3f          %2s    %s\n", 
            "ssF", new Object[] {
                a.getElementSymbolIso(false).toUpperCase(), strExtra,
                new float[] { x, y, z, 0f } }));
        if (atomLast != null
            && atomLast.getPolymerIndexInModel() == a.getPolymerIndexInModel())
          pdbCONECT.append("CONECT").append(
              TextFormat.formatStringI("%5i", "i", atomLast.getAtomNumber()))
              .append(TextFormat.formatStringI("%5i", "i", a.getAtomNumber()))
              .appendC('\n');
        atomLast = a;
      }
    }
    sb.append(pdbCONECT.toString());
    if (isDraw)
      return sb.toString();
    bsSelected.and(bsAtoms);
    if (isPDB)
      sb.append("\n\n"
          + getProteinStructureState(bsWritten, false, ctype == 'R', 1));
    return sb.toString();
  }

  public boolean isJmolDataFrameForModel(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount && models[modelIndex].isJmolDataFrame);
  }

  private boolean isJmolDataFrameForAtom(Atom atom) {
    return (models[atom.modelIndex].isJmolDataFrame);
  }

  public void setJmolDataFrame(String type, int modelIndex, int modelDataIndex) {
    Model model = models[type == null ? models[modelDataIndex].dataSourceFrame
        : modelIndex];
    if (type == null) {
      //leaving a data frame -- just set generic to this one if quaternion
      type = models[modelDataIndex].jmolFrameType;
    }
    if (modelIndex >= 0) {
      if (model.dataFrames == null) {
        model.dataFrames = new Hashtable<String, Integer>();
      }
      models[modelDataIndex].dataSourceFrame = modelIndex;
      models[modelDataIndex].jmolFrameType = type;
      model.dataFrames.put(type, Integer.valueOf(modelDataIndex));
    }
    if (type.startsWith("quaternion") && type.indexOf("deriv") < 0) { //generic quaternion
      type = type.substring(0, type.indexOf(" "));
      model.dataFrames.put(type, Integer.valueOf(modelDataIndex));
    }
  }

  public int getJmolDataFrameIndex(int modelIndex, String type) {
    if (models[modelIndex].dataFrames == null) {
      return -1;
    }
    Integer index = models[modelIndex].dataFrames.get(type);
    return (index == null ? -1 : index.intValue());
  }

  protected void clearDataFrameReference(int modelIndex) {
    for (int i = 0; i < modelCount; i++) {
      Map<String, Integer> df = models[i].dataFrames;
      if (df == null) {
        continue;
      }
      Iterator<Integer> e = df.values().iterator();
      while (e.hasNext()) {
        if ((e.next()).intValue() == modelIndex) {
          e.remove();
        }
      }
    }
  }

  public String getJmolFrameType(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount ? models[modelIndex].jmolFrameType
        : "modelSet");
  }

  public int getJmolDataSourceFrame(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount ? models[modelIndex].dataSourceFrame
        : -1);
  }

  public void saveModelOrientation(int modelIndex, Orientation orientation) {
    models[modelIndex].orientation = orientation;
  }

  public Orientation getModelOrientation(int modelIndex) {
    return models[modelIndex].orientation;
  }

  /*
   final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
   "MODEL ", "SCALE",  "HETATM", "SEQRES",
   "DBREF ", };
   */

  public String getPDBHeader(int modelIndex) {
    return (models[modelIndex].isBioModel ? models[modelIndex].getFullPDBHeader()
        : getFileHeader(modelIndex));
  }

  public String getFileHeader(int modelIndex) {
    if (modelIndex < 0)
      return "";
    if (models[modelIndex].isBioModel)
      return models[modelIndex].getFullPDBHeader();
    String info = (String) getModelAuxiliaryInfoValue(modelIndex, "fileHeader");
    if (info == null)
      info = modelSetName;
    if (info != null)
      return info;
    return "no header information found";
  }

  //////////////  individual models ////////////////

  public int getAltLocIndexInModel(int modelIndex, char alternateLocationID) {
    if (alternateLocationID == '\0') {
      return 0;
    }
    String altLocList = getAltLocListInModel(modelIndex);
    if (altLocList.length() == 0) {
      return 0;
    }
    return altLocList.indexOf(alternateLocationID) + 1;
  }

  public int getInsertionCodeIndexInModel(int modelIndex, char insertionCode) {
    if (insertionCode == '\0')
      return 0;
    String codeList = getInsertionListInModel(modelIndex);
    if (codeList.length() == 0)
      return 0;
    return codeList.indexOf(insertionCode) + 1;
  }

  public String getAltLocListInModel(int modelIndex) {
    if (modelIndex < 0)
      return "";
    String str = (String) getModelAuxiliaryInfoValue(modelIndex, "altLocs");
    return (str == null ? "" : str);
  }

  private String getInsertionListInModel(int modelIndex) {
    String str = (String) getModelAuxiliaryInfoValue(modelIndex, "insertionCodes");
    return (str == null ? "" : str);
  }

  public int getModelSymmetryCount(int modelIndex) {
    String[] operations;
    return (models[modelIndex].biosymmetryCount > 0 || unitCells == null
        || unitCells[modelIndex] == null
        || (operations = unitCells[modelIndex].getSymmetryOperations()) == null ? models[modelIndex].biosymmetryCount
        : operations.length);
  }

  public String getSymmetryOperation(int modelIndex, String spaceGroup,
                                     int symOp, P3 pt1, P3 pt2,
                                     String drawID, boolean labelOnly) {
    Map<String, Object> sginfo = getSymTemp(true).getSpaceGroupInfo((ModelSet) this, modelIndex, spaceGroup,
        symOp, pt1, pt2, drawID);
    if (sginfo == null)
      return "";
    return symTemp.getSymmetryOperationInfo(sginfo, symOp, drawID, labelOnly);
  }

  public int[] getModelCellRange(int modelIndex) {
    if (unitCells == null)
      return null;
    return unitCells[modelIndex].getCellRange();
  }

  public boolean modelHasVibrationVectors(int modelIndex) {
    if (vibrationVectors != null)
      for (int i = atomCount; --i >= 0;)
        if ((modelIndex < 0 || atoms[i].modelIndex == modelIndex)
            && vibrationVectors[i] != null && vibrationVectors[i].length() > 0)
          return true;
    return false;
  }

  public BS getElementsPresentBitSet(int modelIndex) {
    if (modelIndex >= 0)
      return elementsPresent[modelIndex];
    BS bs = new BS();
    for (int i = 0; i < modelCount; i++)
      bs.or(elementsPresent[i]);
    return bs;
  }

  private String getSymmetryInfoAsStringForModel(int modelIndex) {
    SymmetryInterface unitCell = getUnitCell(modelIndex);
    return (unitCell == null ? "no symmetry information" : unitCell
        .getSymmetryInfoString());
  }

  ///////// molecules /////////

  public int getMoleculeIndex(int atomIndex, boolean inModel) {
    //ColorManager
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < moleculeCount; i++) {
      if (molecules[i].atomList.get(atomIndex))
        return (inModel ? molecules[i].indexInModel : i);
    }
    return 0;
  }

  public BS getMoleculeBitSet(BS bs) {
    // returns cumulative sum of all atoms in molecules containing these atoms
    if (moleculeCount == 0)
      getMolecules();
    BS bsResult = BSUtil.copy(bs);
    BS bsInitial = BSUtil.copy(bs);
    int i = 0;
    BS bsTemp = new BS();
    while ((i = bsInitial.length() - 1) >= 0) {
      bsTemp = getMoleculeBitSetForAtom(i);
      if (bsTemp == null) {
        // atom has been deleted
        bsInitial.clear(i);
        bsResult.clear(i);
        continue;
      }
      bsInitial.andNot(bsTemp);
      bsResult.or(bsTemp);
    }
    return bsResult;
  }

  public BS getMoleculeBitSetForAtom(int atomIndex) {
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < moleculeCount; i++)
      if (molecules[i].atomList.get(atomIndex))
        return molecules[i].atomList;
    return null;
  }

  public V3 getModelDipole(int modelIndex) {
    if (modelIndex < 0)
      return null;
    V3 dipole = (V3) getModelAuxiliaryInfoValue(modelIndex, "dipole");
    if (dipole == null)
      dipole = (V3) getModelAuxiliaryInfoValue(modelIndex, "DIPOLE_VEC");
    return dipole;
  }

  public V3 calculateMolecularDipole(int modelIndex) {
    if (partialCharges == null || modelIndex < 0)
      return null;
    int nPos = 0;
    int nNeg = 0;
    float cPos = 0;
    float cNeg = 0;
    V3 pos = new V3();
    V3 neg = new V3();
    for (int i = 0; i < atomCount; i++) {
      if (atoms[i].modelIndex != modelIndex)
        continue;
      float c = partialCharges[i];
      if (c < 0) {
        nNeg++;
        cNeg += c;
        neg.scaleAdd2(c, atoms[i], neg);
      } else if (c > 0) {
        nPos++;
        cPos += c;
        pos.scaleAdd2(c, atoms[i], pos);
      }
    }
    if (nNeg == 0 || nPos == 0)
      return null;
    pos.scale(1f / cPos);
    neg.scale(1f / cNeg);
    pos.sub(neg);
    Logger
        .warn("CalculateMolecularDipole: this is an approximate result -- needs checking");
    pos.scale(cPos * 4.8f); //1e-10f * 1.6e-19f/ 3.336e-30f;

    // SUM_Q[(SUM_pos Q_iRi) / SUM_Q   -  (SUM_neg Q_iRi) / (-SUM_Q) ]    
    // this is really just SUM_i (Q_iR_i). Don't know why that would not work. 

    //http://www.chemistry.mcmaster.ca/esam/Chapter_7/section_3.html
    // 1 Debye = 3.336e-30 Coulomb-meter; C_e = 1.6022e-19 C
    return pos;
  }

  public int getMoleculeCountInModel(int modelIndex) {
    //ColorManager
    //not implemented for pop-up menu -- will slow it down.
    int n = 0;
    if (moleculeCount == 0)
      getMolecules();
    if (modelIndex < 0)
      return moleculeCount;
    for (int i = 0; i < modelCount; i++) {
      if (modelIndex == i)
        n += models[i].moleculeCount;
    }
    return n;
  }

  private BS selectedMolecules = new BS();
  private int selectedMoleculeCount;

  public void calcSelectedMoleculesCount(BS bsSelected) {
    if (moleculeCount == 0)
      getMolecules();
    selectedMolecules.xor(selectedMolecules);
    selectedMoleculeCount = 0;
    BS bsTemp = new BS();
    for (int i = 0; i < moleculeCount; i++) {
      BSUtil.copy2(bsSelected, bsTemp);
      bsTemp.and(molecules[i].atomList);
      if (bsTemp.length() > 0) {
        selectedMolecules.set(i);
        selectedMoleculeCount++;
      }
    }
  }

  /**
   * deletes molecules based on: CENTROID -- molecular centroid is not in unit
   * cell CENTROID PACKED -- all molecule atoms are not in unit cell
   * 
   * @param iAtom0
   * @param iAtom1
   * @param minmax
   *        fractional [xmin, ymin, zmin, xmax, ymax, zmax, 1=packed]
   */
  public void setCentroid(int iAtom0, int iAtom1, int[] minmax) {
    SymmetryInterface uc = getUnitCell(atoms[iAtom0].modelIndex);
    if (uc == null)
      return;
    uc.setCentroid((ModelSet) this, iAtom0, iAtom1, minmax);
  }

  public JmolMolecule[] getMolecules() {
    if (moleculeCount > 0)
      return molecules;
    if (molecules == null)
      molecules = new JmolMolecule[4];
    moleculeCount = 0;
    Model m = null;
    BS[] bsModelAtoms = new BS[modelCount];
    JmolList<BS> biobranches = null;
    for (int i = 0; i < modelCount; i++) {
      // TODO: Trajectories?
      bsModelAtoms[i] = viewer.getModelUndeletedAtomsBitSet(i);
      m = models[i];
      m.moleculeCount = 0;
      biobranches = m.getBioBranches(biobranches);
    }
    // problem, as with 1gzx, is that this does not include non-protein cofactors that are 
    // covalently bonded. So we indicate a set of "biobranches" in JmolMolecule.getMolecules
    molecules = JmolMolecule.getMolecules(atoms, bsModelAtoms, biobranches,
        null);
    moleculeCount = molecules.length;
    for (int i = moleculeCount; --i >= 0;) {
      m = models[molecules[i].modelIndex];
      m.firstMoleculeIndex = i;
      m.moleculeCount++;
    }
    return molecules;
  }

  //////////// iterators //////////

  //private final static boolean MIX_BSPT_ORDER = false;
  boolean showRebondTimes = true;

  protected void initializeBspf() {
    if (bspf != null && bspf.isInitialized())
      return;
    if (showRebondTimes)
      Logger.startTimer("build bspf");
    Bspf bspf = new Bspf(3);
    if (Logger.debugging)
      Logger.debug("sequential bspt order");
    BS bsNew = BSUtil.newBitSet(modelCount);
    for (int i = atomCount; --i >= 0;) {
      // important that we go backward here, because we are going to 
      // use System.arrayCopy to expand the array ONCE only
      Atom atom = atoms[i];
      if (!atom.isDeleted()) {
        bspf.addTuple(models[atom.modelIndex].trajectoryBaseIndex, atom);
        bsNew.set(atom.modelIndex);
      }
    }
    //      }
    if (showRebondTimes) {
      Logger.checkTimer("build bspf", false);
      bspf.stats();
      //        bspf.dump();
    }
    for (int i = bsNew.nextSetBit(0); i >= 0; i = bsNew.nextSetBit(i + 1))
      bspf.validateModel(i, true);
    bspf.validate(true);
    this.bspf = bspf;

  }

  protected void initializeBspt(int modelIndex) {
    initializeBspf();
    if (bspf.isInitializedIndex(modelIndex))
      return;
    bspf.initialize(modelIndex, atoms, viewer
        .getModelUndeletedAtomsBitSet(modelIndex));
  }

  public void setIteratorForPoint(AtomIndexIterator iterator, int modelIndex,
                                  P3 pt, float distance) {
    if (modelIndex < 0) {
      iterator.setCenter(pt, distance);
      return;
    }
    initializeBspt(modelIndex);
    iterator.setModel(this, modelIndex, models[modelIndex].firstAtomIndex,
        Integer.MAX_VALUE, pt, distance, null);
  }

  public void setIteratorForAtom(AtomIndexIterator iterator, int modelIndex,
                                 int atomIndex, float distance, RadiusData rd) {
    if (modelIndex < 0)
      modelIndex = atoms[atomIndex].modelIndex;
    modelIndex = models[modelIndex].trajectoryBaseIndex;
    initializeBspt(modelIndex);
    iterator.setModel(this, modelIndex, models[modelIndex].firstAtomIndex,
        atomIndex, atoms[atomIndex], distance, rd);
  }

  /**
   * @param bsSelected
   * @param isGreaterOnly
   * @param modelZeroBased
   * @param hemisphereOnly
   * @param isMultiModel
   * @return an iterator
   */
  public AtomIndexIterator getSelectedAtomIterator(BS bsSelected,
                                                   boolean isGreaterOnly,
                                                   boolean modelZeroBased,
                                                   boolean hemisphereOnly,
                                                   boolean isMultiModel) {
    //EnvelopeCalculation, IsoSolventReader
    // This iterator returns only atoms OTHER than the atom specified
    // and with the specified restrictions. 
    // Model zero-based means the index returned is within the model, 
    // not the full atom set. broken in 12.0.RC6; repaired in 12.0.RC15

    initializeBspf();
    AtomIteratorWithinModel iter;
    if (isMultiModel) {
      BS bsModels = getModelBitSet(bsSelected, false);
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1))
        initializeBspt(i);
      iter = new AtomIteratorWithinModelSet(bsModels);
    } else {
      iter = new AtomIteratorWithinModel();
    }
    iter.initialize(bspf, bsSelected, isGreaterOnly, modelZeroBased,
        hemisphereOnly, viewer.isParallel());
    return iter;
  }

  ////////// bonds /////////

  @Override
  public int getBondCountInModel(int modelIndex) {
    return (modelIndex < 0 ? bondCount : models[modelIndex].getBondCount());
  }

  ////////// struts /////////

  /**
   * see comments in org.jmol.modelsetbio.AlphaPolymer.java
   * 
   * Struts are calculated for atoms in bs1 connecting to atoms in bs2. The two
   * bitsets may overlap.
   * 
   * @param bs1
   * @param bs2
   * @return number of struts added
   */
  public int calculateStruts(BS bs1, BS bs2) {
    // select only ONE model
    makeConnections2(0, Float.MAX_VALUE, JmolEdge.BOND_STRUT, T.delete, bs1,
        bs2, null, false, false, 0);
    int iAtom = bs1.nextSetBit(0);
    if (iAtom < 0)
      return 0;
    Model model = models[atoms[iAtom].modelIndex];
    return (model.isBioModel ? model.calculateStruts((ModelSet) this, bs1, bs2)
        : 0);
  }

  public int getAtomCountInModel(int modelIndex) {
    return (modelIndex < 0 ? atomCount : models[modelIndex].atomCount);
  }

  protected BS bsAll;

  protected ShapeManager shapeManager;

  /**
   * note -- this method returns ALL atoms, including deleted.
   * 
   * @param bsModels
   * @return bitset of atoms
   */
  public BS getModelAtomBitSetIncludingDeletedBs(BS bsModels) {
    BS bs = new BS();
    if (bsModels == null && bsAll == null)
      bsAll = BSUtil.setAll(atomCount);
    if (bsModels == null)
      bs.or(bsAll);
    else
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1))
        bs.or(getModelAtomBitSetIncludingDeleted(i, false));
    return bs;
  }

  /**
   * Note that this method returns all atoms, included deleted ones. If you
   * don't want deleted atoms, then use
   * viewer.getModelAtomBitSetUndeleted(modelIndex, TRUE)
   * 
   * @param modelIndex
   * @param asCopy
   *        MUST BE TRUE IF THE BITSET IS GOING TO BE MODIFIED!
   * @return either the actual bitset or a copy
   */
  public BS getModelAtomBitSetIncludingDeleted(int modelIndex,
                                                   boolean asCopy) {
    BS bs = (modelIndex < 0 ? bsAll : models[modelIndex].bsAtoms);
    if (bs == null)
      bs = bsAll = BSUtil.setAll(atomCount);
    return (asCopy ? BSUtil.copy(bs) : bs);
  }

  protected BS getAtomBitsMaybeDeleted(int tokType, Object specInfo) {
    int[] info;
    BS bs;
    P3 pt;
    switch (tokType) {
    default:
      return getAtomBitsMDa(tokType, specInfo);
    case T.bonds:
    case T.isaromatic:
      return getAtomBitsMDb(tokType, specInfo);
    case T.basepair:
      return getBasePairBits((String) specInfo);
    case T.boundbox:
      BoxInfo boxInfo = getBoxInfo((BS) specInfo, 1);
      bs = getAtomsWithin(boxInfo.getBoundBoxCornerVector().length() + 0.0001f,
          boxInfo.getBoundBoxCenter(), null, -1);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        if (!boxInfo.isWithin(atoms[i]))
          bs.clear(i);
      return bs;
    case T.cell:
      // select cell=555 (an absolute quantity)
      bs = new BS();
      info = (int[]) specInfo;
      P3 ptcell = P3.new3(info[0] / 1000f, info[1] / 1000f,
          info[2] / 1000f);
      pt = new P3();
      boolean isAbsolute = !viewer.getFractionalRelative();
      for (int i = atomCount; --i >= 0;)
        if (isInLatticeCell(i, ptcell, pt, isAbsolute))
          bs.set(i);
      return bs;
    case T.molecule:
      return getMoleculeBitSet((BS) specInfo);
    case T.sequence:
      return getSequenceBits((String) specInfo, null);
    case T.spec_seqcode_range:
      info = (int[]) specInfo;
      int seqcodeA = info[0];
      int seqcodeB = info[1];
      char chainID = (char) info[2];
      bs = new BS();
      boolean caseSensitive = viewer.getChainCaseSensitive();
      if (!caseSensitive)
        chainID = Character.toUpperCase(chainID);
      for (int i = modelCount; --i >= 0;)
        if (models[i].isBioModel)
          models[i].selectSeqcodeRange(seqcodeA, seqcodeB, chainID, bs,
              caseSensitive);
      return bs;
    case T.specialposition:
      bs = BSUtil.newBitSet(atomCount);
      int modelIndex = -1;
      int nOps = 0;
      for (int i = atomCount; --i >= 0;) {
        Atom atom = atoms[i];
        BS bsSym = atom.getAtomSymmetry();
        if (bsSym != null) {
          if (atom.modelIndex != modelIndex) {
            modelIndex = atom.modelIndex;
            if (getModelCellRange(modelIndex) == null)
              continue;
            nOps = getModelSymmetryCount(modelIndex);
          }
          // special positions are characterized by
          // multiple operator bits set in the first (overall)
          // block of nOpts bits.
          // only strictly true with load {nnn mmm 1}

          int n = 0;
          for (int j = nOps; --j >= 0;)
            if (bsSym.get(j))
              if (++n > 1) {
                bs.set(i);
                break;
              }
        }
      }
      return bs;
    case T.symmetry:
      return BSUtil.copy(bsSymmetry == null ? bsSymmetry = BSUtil.newBitSet(
          atomCount) : bsSymmetry);
    case T.unitcell:
      // select UNITCELL (a relative quantity)
      bs = new BS();
      SymmetryInterface unitcell = viewer.getCurrentUnitCell();
      if (unitcell == null)
        return bs;
      P3 cell = P3.new3(1, 1, 1);
      pt = new P3();
      for (int i = atomCount; --i >= 0;)
        if (isInLatticeCell(i, cell, pt, false)) 
          bs.set(i);
      return bs;
    }
  }

  private boolean isInLatticeCell(int i, P3 cell, P3 pt,
                                  boolean isAbsolute) {
    // this is the one method that allows for an absolute fractional cell business
    // but it is always called with isAbsolute FALSE.
    // so then it is determining values for select UNITCELL and the like.

    int iModel = atoms[i].modelIndex;
    SymmetryInterface uc = getUnitCell(iModel);
    if (uc == null)
      return false;
    pt.setT(atoms[i]);
    uc.toFractional(pt, isAbsolute);
    float slop = 0.02f;
    // {1 1 1} here is the original cell
    if (pt.x < cell.x - 1f - slop || pt.x > cell.x + slop)
      return false;
    if (pt.y < cell.y - 1f - slop || pt.y > cell.y + slop)
      return false;
    if (pt.z < cell.z - 1f - slop || pt.z > cell.z + slop)
      return false;
    return true;
  }

  /**
   * Get atoms within a specific distance of any atom in a specific set of atoms
   * either within all models or within just the model(s) of those atoms
   * 
   * @param distance
   * @param bs
   * @param withinAllModels
   * @param rd
   * @return the set of atoms
   */
  public BS getAtomsWithinBs(float distance, BS bs,
                               boolean withinAllModels, RadiusData rd) {
    BS bsResult = new BS();
    BS bsCheck = getIterativeModels(false);
    bs = BSUtil.andNot(bs, viewer.getDeletedAtoms());
    AtomIndexIterator iter = getSelectedAtomIterator(null, false, false, false,
        false);
    if (withinAllModels) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        for (int iModel = modelCount; --iModel >= 0;) {
          if (!bsCheck.get(iModel))
            continue;
          if (distance < 0) {
            getAtomsWithin(distance, atoms[i].getFractionalUnitCoordPt(true),
                bsResult, -1);
            continue;
          }
          setIteratorForAtom(iter, iModel, i, distance, rd);
          iter.addAtoms(bsResult);
        }
    } else {
      bsResult.or(bs);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (distance < 0) {
          getAtomsWithin(distance, atoms[i], bsResult, atoms[i].modelIndex);
          continue;
        }
        setIteratorForAtom(iter, -1, i, distance, rd);
        iter.addAtoms(bsResult);
      }
    }
    iter.release();
    return bsResult;
  }

  public BS getGroupsWithin(int nResidues, BS bs) {
    BS bsCheck = getIterativeModels(false);
    BS bsResult = new BS();
    for (int iModel = modelCount; --iModel >= 0;) {
      if (!bsCheck.get(iModel) || !models[iModel].isBioModel)
        continue;
      models[iModel].getGroupsWithin(nResidues, bs, bsResult);
    }
    return bsResult;
  }

  public BS getAtomsWithin(float distance, P3 coord, BS bsResult,
                               int modelIndex) {

    if (bsResult == null)
      bsResult = new BS();

    if (distance < 0) { // check all unitCell distances
      distance = -distance;
      final P3 ptTemp1 = new P3();
      final P3 ptTemp2 = new P3();
      for (int i = atomCount; --i >= 0;) {
        Atom atom = atoms[i];
        if (modelIndex >= 0 && atoms[i].modelIndex != modelIndex)
          continue;
        if (!bsResult.get(i)
            && atom.getFractionalUnitDistance(coord, ptTemp1, ptTemp2) <= distance)
          bsResult.set(atom.index);
      }
      return bsResult;
    }

    BS bsCheck = getIterativeModels(true);
    AtomIndexIterator iter = getSelectedAtomIterator(null, false, false, false,
        false);
    for (int iModel = modelCount; --iModel >= 0;) {
      if (!bsCheck.get(iModel))
        continue;
      setIteratorForAtom(iter, -1, models[iModel].firstAtomIndex, -1, null);
      iter.setCenter(coord, distance);
      iter.addAtoms(bsResult);
    }
    iter.release();
    return bsResult;
  }

  private BS getBasePairBits(String specInfo) {
    BS bs = new BS();
    if (specInfo.length() % 2 != 0)
      return bs;
    BS bsA = null;
    BS bsB = null;
    JmolList<Bond> vHBonds = new  JmolList<Bond>();
    if (specInfo.length() == 0) {
      bsA = bsB = viewer.getModelUndeletedAtomsBitSet(-1);
      calcRasmolHydrogenBonds(bsA, bsB, vHBonds, true, 1, false, null);
    } else {
      for (int i = 0; i < specInfo.length();) {
        bsA = getSequenceBits(specInfo.substring(i, ++i), null);
        if (bsA.cardinality() == 0)
          continue;
        bsB = getSequenceBits(specInfo.substring(i, ++i), null);
        if (bsB.cardinality() == 0)
          continue;
        calcRasmolHydrogenBonds(bsA, bsB, vHBonds, true, 1, false, null);
      }
    }
    BS bsAtoms = new BS();
    for (int i = vHBonds.size(); --i >= 0;) {
      Bond b = vHBonds.get(i);
      bsAtoms.set(b.atom1.index);
      bsAtoms.set(b.atom2.index);
    }
    return getAtomBitsMDb(T.group, bsAtoms);
  }

  public BS getSequenceBits(String specInfo, BS bs) {
    if (bs == null)
      bs = viewer.getModelUndeletedAtomsBitSet(-1);
    BS bsResult = new BS();
    if (specInfo.length() > 0)
      for (int i = 0; i < modelCount; ++i)
        if (models[i].isBioModel)
          models[i].getSequenceBits(specInfo, bs, bsResult);
    return bsResult;
  }

  // ////////// Bonding methods ////////////

  public void deleteBonds(BS bsBonds, boolean isFullModel) {
    if (!isFullModel) {
      BS bsA = new BS();
      BS bsB = new BS();
      for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
        Atom atom1 = bonds[i].atom1;
        if (models[atom1.modelIndex].isModelKit)
          continue;
        bsA.clearAll();
        bsB.clearAll();
        bsA.set(atom1.index);
        bsB.set(bonds[i].getAtomIndex2());
        addStateScript("connect ", null, bsA, bsB, "delete", false, true);
      }
    }
    dBb(bsBonds, isFullModel);
  }

  protected int[] makeConnections2(float minDistance, float maxDistance,
                                  int order, int connectOperation, BS bsA,
                                  BS bsB, BS bsBonds, boolean isBonds,
                                  boolean addGroup, float energy) {
    if (bsBonds == null)
      bsBonds = new BS();
    boolean matchAny = (order == JmolEdge.BOND_ORDER_ANY);
    boolean matchNull = (order == JmolEdge.BOND_ORDER_NULL);
    if (matchNull)
      order = JmolEdge.BOND_COVALENT_SINGLE; //default for setting
    boolean matchHbond = Bond.isOrderH(order);
    boolean identifyOnly = false;
    boolean modifyOnly = false;
    boolean createOnly = false;
    boolean autoAromatize = false;
    float minDistanceSquared = minDistance * minDistance;
    float maxDistanceSquared = maxDistance * maxDistance;
    switch (connectOperation) {
    case T.delete:
      return deleteConnections(minDistance, maxDistance, order, bsA, bsB,
          isBonds, matchNull, minDistanceSquared, maxDistanceSquared);
    case T.legacyautobonding:
    case T.auto:
      if (order != JmolEdge.BOND_AROMATIC)
        return autoBond(bsA, bsB, bsBonds, isBonds, matchHbond, connectOperation == T.legacyautobonding);
      modifyOnly = true;
      autoAromatize = true;
      break;
    case T.identify:
      identifyOnly = true;
      break;
    case T.modify:
      modifyOnly = true;
      break;
    case T.create:
      createOnly = true;
      break;
    }
    defaultCovalentMad = viewer.getMadBond();
    boolean minDistanceIsFractionRadius = (minDistance < 0);
    boolean maxDistanceIsFractionRadius = (maxDistance < 0);
    if (minDistanceIsFractionRadius)
      minDistance = -minDistance;
    if (maxDistanceIsFractionRadius)
      maxDistance = -maxDistance;
    short mad = getDefaultMadFromOrder(order);
    int nNew = 0;
    int nModified = 0;
    Bond bondAB = null;
    int m = (isBonds ? 1 : atomCount);
    Atom atomA = null;
    Atom atomB = null;
    float dAB = 0;
    float dABcalc = 0;
    short newOrder = (short) (order | JmolEdge.BOND_NEW);
    for (int iA = bsA.nextSetBit(0); iA >= 0; iA = bsA.nextSetBit(iA + 1)) {
      if (isBonds) {
        bondAB = bonds[iA];
        atomA = bondAB.atom1;
        atomB = bondAB.atom2;
      } else {
        atomA = atoms[iA];
        if (atomA.isDeleted())
          continue;
      }
      for (int iB = (isBonds ? m : bsB.nextSetBit(0)); iB >= 0; iB = (isBonds ? iB - 1
          : bsB.nextSetBit(iB + 1))) {
        if (!isBonds) {
          if (iB == iA)
            continue;
          atomB = atoms[iB];
          if (atomA.modelIndex != atomB.modelIndex || atomB.isDeleted())
            continue;
          if (atomA.alternateLocationID != atomB.alternateLocationID
              && atomA.alternateLocationID != '\0'
              && atomB.alternateLocationID != '\0')
            continue;
          bondAB = atomA.getBond(atomB);
        }
        if (bondAB == null && (identifyOnly || modifyOnly) || bondAB != null
            && createOnly)
          continue;
        float distanceSquared = atomA.distanceSquared(atomB);
        if (minDistanceIsFractionRadius || maxDistanceIsFractionRadius) {
          dAB = atomA.distance(atomB);
          dABcalc = atomA.getBondingRadiusFloat()
              + atomB.getBondingRadiusFloat();
        }
        if ((minDistanceIsFractionRadius ? dAB < dABcalc * minDistance
            : distanceSquared < minDistanceSquared)
            || (maxDistanceIsFractionRadius ? dAB > dABcalc * maxDistance
                : distanceSquared > maxDistanceSquared))
          continue;
        if (bondAB != null) {
          if (!identifyOnly && !matchAny) {
            bondAB.setOrder(order);
            bsAromatic.clear(bondAB.index);
          }
          if (!identifyOnly || matchAny || order == bondAB.order
              || newOrder == bondAB.order || matchHbond && bondAB.isHydrogen()) {
            bsBonds.set(bondAB.index);
            nModified++;
          }
        } else {
          bsBonds.set(bondAtoms(atomA, atomB, order, mad, bsBonds, energy,
              addGroup, true).index);
          nNew++;
        }
      }
    }
    if (autoAromatize)
      assignAromaticBondsBs(true, bsBonds);
    if (!identifyOnly)
      shapeManager.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MIN_VALUE,
          null, bsBonds);
    return new int[] { nNew, nModified };
  }

  public int autoBondBs4(BS bsA, BS bsB, BS bsExclude, BS bsBonds,
                      short mad, boolean preJmol11_9_24) {
    // unfortunately, 11.9.24 changed the order with which atoms were processed
    // for autobonding. This means that state script prior to that that use
    // select BOND will be misread by later version.
    if (preJmol11_9_24)
      return autoBond_Pre_11_9_24(bsA, bsB, bsExclude, bsBonds, mad);
    if (atomCount == 0)
      return 0;
    if (mad == 0)
      mad = 1;
    // null values for bitsets means "all"
    if (maxBondingRadius == Float.MIN_VALUE)
      findMaxRadii();
    float bondTolerance = viewer.getBondTolerance();
    float minBondDistance = viewer.getMinBondDistance();
    float minBondDistance2 = minBondDistance * minBondDistance;
    int nNew = 0;
    if (showRebondTimes)// && Logger.debugging)
      Logger.startTimer("autobond");
    /*
     * miguel 2006 04 02 note that the way that these loops + iterators are
     * constructed, everything assumes that all possible pairs of atoms are
     * going to be looked at. for example, the hemisphere iterator will only
     * look at atom indexes that are >= (or <= ?) the specified atom. if we are
     * going to allow arbitrary sets bsA and bsB, then this will not work. so,
     * for now I will do it the ugly way. maybe enhance/improve in the future.
     */
    int lastModelIndex = -1;
    boolean isAll = (bsA == null);
    BS bsCheck;
    int i0;
    if (isAll) {
      i0 = 0;
      bsCheck = null;
    } else {
      if (bsA.equals(bsB)) {
        bsCheck = bsA;
      } else {
        bsCheck = BSUtil.copy(bsA);
        bsCheck.or(bsB);
      }
      i0 = bsCheck.nextSetBit(0);
    }
    AtomIndexIterator iter = getSelectedAtomIterator(null, false, false, true,
        false);
    for (int i = i0; i >= 0 && i < atomCount; i = (isAll ? i + 1 : bsCheck
        .nextSetBit(i + 1))) {
      boolean isAtomInSetA = (isAll || bsA.get(i));
      boolean isAtomInSetB = (isAll || bsB.get(i));
      Atom atom = atoms[i];
      if (atom.isDeleted())
        continue;
      int modelIndex = atom.modelIndex;
      // no connections allowed in a data frame
      if (modelIndex != lastModelIndex) {
        lastModelIndex = modelIndex;
        if (isJmolDataFrameForModel(modelIndex)) {
          i = models[modelIndex].firstAtomIndex + models[modelIndex].atomCount
              - 1;
          continue;
        }
      }
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadiusFloat();
      if (myBondingRadius == 0)
        continue;
      boolean isFirstExcluded = (bsExclude != null && bsExclude.get(i));
      float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
      setIteratorForAtom(iter, -1, i, searchRadius, null);
      while (iter.hasNext()) {
        Atom atomNear = atoms[iter.next()];
        if (atomNear.isDeleted())
          continue;
        int atomIndexNear = atomNear.index;
        boolean isNearInSetA = (isAll || bsA.get(atomIndexNear));
        boolean isNearInSetB = (isAll || bsB.get(atomIndexNear));
        // BOTH must be excluded in order to ignore bonding
        if (!isNearInSetA && !isNearInSetB
            || !(isAtomInSetA && isNearInSetB || isAtomInSetB && isNearInSetA)
            || isFirstExcluded && bsExclude.get(atomIndexNear))
          continue;
        short order = getBondOrderFull(myBondingRadius, atomNear
            .getBondingRadiusFloat(), iter.foundDistance2(), minBondDistance2,
            bondTolerance);
        if (order > 0
            && checkValencesAndBond(atom, atomNear, order, mad, bsBonds))
          nNew++;
      }
      iter.release();
    }
    if (showRebondTimes)
      Logger.checkTimer("autoBond", false);
    return nNew;
  }

  private int autoBond_Pre_11_9_24(BS bsA, BS bsB, BS bsExclude,
                                   BS bsBonds, short mad) {
    if (atomCount == 0)
      return 0;
    if (mad == 0)
      mad = 1;
    // null values for bitsets means "all"
    if (maxBondingRadius == Float.MIN_VALUE)
      findMaxRadii();
    float bondTolerance = viewer.getBondTolerance();
    float minBondDistance = viewer.getMinBondDistance();
    float minBondDistance2 = minBondDistance * minBondDistance;
    int nNew = 0;
    initializeBspf();
    /*
     * miguel 2006 04 02
     * note that the way that these loops + iterators are constructed,
     * everything assumes that all possible pairs of atoms are going to
     * be looked at.
     * for example, the hemisphere iterator will only look at atom indexes
     * that are >= (or <= ?) the specified atom.
     * if we are going to allow arbitrary sets bsA and bsB, then this will
     * not work.
     * so, for now I will do it the ugly way.
     * maybe enhance/improve in the future.
     */
    int lastModelIndex = -1;
    for (int i = atomCount; --i >= 0;) {
      boolean isAtomInSetA = (bsA == null || bsA.get(i));
      boolean isAtomInSetB = (bsB == null || bsB.get(i));
      if (!isAtomInSetA && !isAtomInSetB)
        //|| bsExclude != null && bsExclude.get(i))
        continue;
      Atom atom = atoms[i];
      if (atom.isDeleted())
        continue;
      int modelIndex = atom.modelIndex;
      //no connections allowed in a data frame
      if (modelIndex != lastModelIndex) {
        lastModelIndex = modelIndex;
        if (isJmolDataFrameForModel(modelIndex)) {
          for (; --i >= 0;)
            if (atoms[i].modelIndex != modelIndex)
              break;
          i++;
          continue;
        }
      }
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadiusFloat();
      if (myBondingRadius == 0)
        continue;
      float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
      initializeBspt(modelIndex);
      CubeIterator iter = bspf.getCubeIterator(modelIndex);
      iter.initialize(atom, searchRadius, true);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom) iter.nextElement();
        if (atomNear == atom || atomNear.isDeleted())
          continue;
        int atomIndexNear = atomNear.index;
        boolean isNearInSetA = (bsA == null || bsA.get(atomIndexNear));
        boolean isNearInSetB = (bsB == null || bsB.get(atomIndexNear));
        if (!isNearInSetA && !isNearInSetB || bsExclude != null
            && bsExclude.get(atomIndexNear) && bsExclude.get(i) //this line forces BOTH to be excluded in order to ignore bonding
        )
          continue;
        if (!(isAtomInSetA && isNearInSetB || isAtomInSetB && isNearInSetA))
          continue;
        short order = getBondOrderFull(myBondingRadius, atomNear
            .getBondingRadiusFloat(), iter.foundDistance2(), minBondDistance2,
            bondTolerance);
        if (order > 0) {
          if (checkValencesAndBond(atom, atomNear, order, mad, bsBonds))
            nNew++;
        }
      }
      iter.release();
    }
    return nNew;
  }

  private int[] autoBond(BS bsA, BS bsB, BS bsBonds,
                         boolean isBonds, boolean matchHbond, boolean legacyAutoBond) {
    if (isBonds) {
      BS bs = bsA;
      bsA = new BS();
      bsB = new BS();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        bsA.set(bonds[i].atom1.index);
        bsB.set(bonds[i].atom2.index);
      }
    }
    return new int[] {
        matchHbond ? autoHbond(bsA, bsB, false) : autoBondBs4(bsA, bsB, null, bsBonds,
            viewer.getMadBond(), legacyAutoBond), 0 };
  }

  private static float hbondMin = 2.5f;

  /**
   * a generalized formation of HBONDS, carried out in relation to calculate
   * HBONDS {atomsFrom} {atomsTo}. The calculation can create pseudo-H bonds for
   * files that do not contain H atoms.
   * 
   * @param bsA
   *        "from" set (must contain H if that is desired)
   * @param bsB
   *        "to" set
   * @param onlyIfHaveCalculated
   * @return negative number of pseudo-hbonds or number of actual hbonds formed
   */
  public int autoHbond(BS bsA, BS bsB, boolean onlyIfHaveCalculated) {
    if (onlyIfHaveCalculated) {
      BS bsModels = getModelBitSet(bsA, false);
      for (int i = bsModels.nextSetBit(0); i >= 0 && onlyIfHaveCalculated; i = bsModels
          .nextSetBit(i + 1))
        onlyIfHaveCalculated = !models[i].hasRasmolHBonds;
      if (onlyIfHaveCalculated)
        return 0;
    }
    boolean haveHAtoms = false;
    for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1))
      if (atoms[i].getElementNumber() == 1) {
        haveHAtoms = true;
        break;
      }
    BS bsHBonds = new BS();
    boolean useRasMol = viewer.getHbondsRasmol();
    if (bsB == null || useRasMol && !haveHAtoms) {
      Logger.info((bsB == null ? "DSSP " : "RasMol")
          + " pseudo-hbond calculation");
      calcRasmolHydrogenBonds(bsA, bsB, null, false, Integer.MAX_VALUE, false,
          bsHBonds);
      return -BSUtil.cardinalityOf(bsHBonds);
    }
    Logger.info(haveHAtoms ? "Standard Hbond calculation"
        : "Jmol pseudo-hbond calculation");
    BS bsCO = null;
    if (!haveHAtoms) {
      bsCO = new BS();
      for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
        int atomID = atoms[i].atomID;
        switch (atomID) {
        case JC.ATOMID_TERMINATING_OXT:
        case JC.ATOMID_CARBONYL_OXYGEN:
        case JC.ATOMID_CARBONYL_OD1:
        case JC.ATOMID_CARBONYL_OD2:
        case JC.ATOMID_CARBONYL_OE1:
        case JC.ATOMID_CARBONYL_OE2:
          bsCO.set(i);
          break;
        }
      }
    }
    float maxXYDistance = viewer.getHbondsDistanceMax();
    float minAttachedAngle = (float) (viewer.getHbondsAngleMin() * Math.PI / 180);
    float hbondMax2 = maxXYDistance * maxXYDistance;
    float hbondMin2 = hbondMin * hbondMin;
    float hxbondMin2 = 1;
    float hxbondMax2 = (maxXYDistance > hbondMin ? hbondMin2 : hbondMax2);
    float hxbondMax = (maxXYDistance > hbondMin ? hbondMin : maxXYDistance);
    int nNew = 0;
    float d2 = 0;
    V3 v1 = new V3();
    V3 v2 = new V3();
    if (showRebondTimes && Logger.debugging)
      Logger.startTimer("hbond");
    P3 C = null;
    P3 D = null;
    AtomIndexIterator iter = getSelectedAtomIterator(bsB, false, false, false,
        false);

    for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
      Atom atom = atoms[i];
      int elementNumber = atom.getElementNumber();
      boolean isH = (elementNumber == 1);
      if (!isH && (haveHAtoms || elementNumber != 7 && elementNumber != 8)
          || isH && !haveHAtoms)
        continue;
      float min2, max2, dmax;
      boolean firstIsCO;
      if (isH) {
        Bond[] b = atom.bonds;
        if (b == null)
          continue;
        boolean isOK = false;
        for (int j = 0; j < b.length && !isOK; j++) {
          Atom a2 = b[j].getOtherAtom(atom);
          int element = a2.getElementNumber();
          isOK = (element == 7 || element == 8);
        }
        if (!isOK)
          continue;
        dmax = hxbondMax;
        min2 = hxbondMin2;
        max2 = hxbondMax2;
        firstIsCO = false;
      } else {
        dmax = maxXYDistance;
        min2 = hbondMin2;
        max2 = hbondMax2;
        firstIsCO = bsCO.get(i);
      }
      setIteratorForAtom(iter, -1, atom.index, dmax, null);
      while (iter.hasNext()) {
        Atom atomNear = atoms[iter.next()];
        int elementNumberNear = atomNear.getElementNumber();
        if (atomNear == atom || !isH && elementNumberNear != 7
            && elementNumberNear != 8 || isH && elementNumberNear == 1
            || (d2 = iter.foundDistance2()) < min2 || d2 > max2 || firstIsCO
            && bsCO.get(atomNear.index) || atom.isBonded(atomNear)) {
          continue;
        }
        if (minAttachedAngle > 0) {
          v1.sub2(atom, atomNear);
          if ((D = checkMinAttachedAngle(atom, minAttachedAngle, v1, v2,
              haveHAtoms)) == null)
            continue;
          v1.scale(-1);
          if ((C = checkMinAttachedAngle(atomNear, minAttachedAngle, v1, v2,
              haveHAtoms)) == null)
            continue;
        }
        float energy = 0;
        short bo;
        if (isH && !Float.isNaN(C.x) && !Float.isNaN(D.x)) {
          /*
           * A crude calculation based on simple distances. In the NH -- O=C
           * case this reads DH -- A=C
           * 
           * (+) H .......... A (-) | | | | (-) D C (+)
           * 
           * 
           * E = Q/rAH - Q/rAD + Q/rCD - Q/rCH
           */

          bo = JmolEdge.BOND_H_CALC;
          energy = HBond.getEnergy((float) Math.sqrt(d2), C.distance(atom), C
              .distance(D), atomNear.distance(D)) / 1000f;
        } else {
          bo = JmolEdge.BOND_H_REGULAR;
        }
        bsHBonds.set(addHBond(atom, atomNear, bo, energy));
        nNew++;
      }
    }
    iter.release();
    shapeManager.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MIN_VALUE,
        null, bsHBonds);
    if (showRebondTimes)
      Logger.checkTimer("hbond", false);
    return (haveHAtoms ? nNew : -nNew);
  }

  private static P3 checkMinAttachedAngle(Atom atom1, float minAngle,
                                               V3 v1, V3 v2,
                                               boolean haveHAtoms) {
    Bond[] bonds = atom1.bonds;
    if (bonds == null || bonds.length == 0)
      return P3.new3(Float.NaN, 0, 0);
    Atom X = null;
    float dMin = Float.MAX_VALUE;
    for (int i = bonds.length; --i >= 0;)
      if (bonds[i].isCovalent()) {
        Atom atomA = bonds[i].getOtherAtom(atom1);
        if (!haveHAtoms && atomA.getElementNumber() == 1)
          continue;
        v2.sub2(atom1, atomA);
        float d = v2.angle(v1);
        if (d < minAngle)
          return null;
        if (d < dMin) {
          X = atomA;
          dMin = d;
        }
      }
    return X;
  }

  //////////// state definition ///////////

  public boolean proteinStructureTainted = false;

  void setStructureIndexes() {
    int id;
    int idnew = 0;
    int lastid = -1;
    int imodel = -1;
    int lastmodel = -1;
    for (int i = 0; i < atomCount; i++) {
      if ((imodel = atoms[i].modelIndex) != lastmodel) {
        idnew = 0;
        lastmodel = imodel;
        lastid = -1;
      }
      if ((id = atoms[i].getStrucNo()) != lastid && id != 0) {
        atoms[i].getGroup().setStrucNo(++idnew);
        lastid = idnew;
      }
    }
  }

  public String getProteinStructureState(BS bsAtoms, boolean taintedOnly,
                                         boolean needPhiPsi, int mode) {
    if (!isPDB)
      return "";
    for (int i = 0; i < modelCount; i++)
      if (models[i].isBioModel)
        return models[i].getProteinStructureState(bsAtoms, taintedOnly,
            needPhiPsi, mode);
    return "";
  }

  public String getModelInfoAsString() {
    SB sb = new SB().append("<models count=\"");
    sb.appendI(modelCount).append("\" modelSetHasVibrationVectors=\"").append(
        modelSetHasVibrationVectors() + "\">\n<properties>");
    if (modelSetProperties != null) {
      Enumeration<?> e = modelSetProperties.propertyNames();
      while (e.hasMoreElements()) {
        String propertyName = (String) e.nextElement();
        sb.append("\n <property name=\"").append(propertyName).append("\" value=").append(
            Escape.eS(modelSetProperties.getProperty(propertyName))).append(" />");
      }
      sb.append("\n</properties>");
    }
    for (int i = 0; i < modelCount; ++i) {
      sb.append("\n<model index=\"").appendI(i)
          .append("\" n=\"").append(getModelNumberDotted(i))
          .append("\" id=").append(Escape.eS("" + getModelAuxiliaryInfoValue(i, "modelID")));
          int ib = viewer.getJDXBaseModelIndex(i);
          if (ib != i)
            sb.append(" baseModelId=").append(Escape.e(getModelAuxiliaryInfoValue(ib, "jdxModelID")));
          sb.append(" name=").append(Escape.eS(getModelName(i)))
          .append(" title=").append(Escape.eS(
              getModelTitle(i)))
           .append(" hasVibrationVectors=\"").appendB(
              modelHasVibrationVectors(i)).append("\" />");
    }
    sb.append("\n</models>");
    return sb.toString();
  }

  public String getSymmetryInfoAsString() {
    SB sb = new SB().append("Symmetry Information:");
    for (int i = 0; i < modelCount; ++i)
      sb.append("\nmodel #").append(getModelNumberDotted(i)).append("; name=")
          .append(getModelName(i)).append("\n").append(
              getSymmetryInfoAsStringForModel(i));
    return sb.toString();
  }

  public BS getAtomsConnected(float min, float max, int intType, BS bs) {
    BS bsResult = new BS();
    int[] nBonded = new int[atomCount];
    int i;
    boolean ishbond = (intType == JmolEdge.BOND_HYDROGEN_MASK);
    boolean isall = (intType == JmolEdge.BOND_ORDER_ANY);
    for (int ibond = 0; ibond < bondCount; ibond++) {
      Bond bond = bonds[ibond];
      if (isall || bond.is(intType) || ishbond && bond.isHydrogen()) {
        if (bs.get(bond.atom1.index)) {
          nBonded[i = bond.atom2.index]++;
          bsResult.set(i);
        }
        if (bs.get(bond.atom2.index)) {
          nBonded[i = bond.atom1.index]++;
          bsResult.set(i);
        }
      }
    }
    boolean nonbonded = (min == 0);
    for (i = atomCount; --i >= 0;) {
      int n = nBonded[i];
      if (n < min || n > max)
        bsResult.clear(i);
      else if (nonbonded && n == 0)
        bsResult.set(i);
    }
    return bsResult;
  }

  public String getUnitCellInfoText() {
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      return "no single current model";
    SymmetryInterface c = getUnitCell(modelIndex);
    return (c == null ? "not applicable" : c.getUnitCellInfo());
  }

  private SymmetryInterface symTemp;

  public Hashtable<String, BS> htPeaks;


  public SymmetryInterface getSymTemp(boolean forceNew) {
    if (symTemp == null || forceNew)
      symTemp = (SymmetryInterface) Interface
          .getOptionInterface("symmetry.Symmetry");
    return symTemp;
  }

  protected void deleteModel(int modelIndex, int firstAtomIndex, int nAtoms,
                             BS bsAtoms, BS bsBonds) {
    /*
     *   ModelCollection.modelSetAuxiliaryInfo["group3Lists", "group3Counts, "models"]
     * ModelCollection.stateScripts ?????
     */
    if (modelIndex < 0) {
      //final deletions
      validateBspf(false);
      bsAll = null;
      resetMolecules();
      isBbcageDefault = false;
      calcBoundBoxDimensions(null, 1);
      return;
    }

    modelNumbers = (int[]) ArrayUtil
        .deleteElements(modelNumbers, modelIndex, 1);
    modelFileNumbers = (int[]) ArrayUtil.deleteElements(modelFileNumbers,
        modelIndex, 1);
    modelNumbersForAtomLabel = (String[]) ArrayUtil.deleteElements(
        modelNumbersForAtomLabel, modelIndex, 1);
    modelNames = (String[]) ArrayUtil.deleteElements(modelNames, modelIndex, 1);
    frameTitles = (String[]) ArrayUtil.deleteElements(frameTitles, modelIndex,
        1);
    thisStateModel = -1;
    String[] group3Lists = (String[]) getModelSetAuxiliaryInfoValue("group3Lists");
    int[][] group3Counts = (int[][]) getModelSetAuxiliaryInfoValue("group3Counts");
    int ptm = modelIndex + 1;
    if (group3Lists != null && group3Lists[ptm] != null) {
      for (int i = group3Lists[ptm].length() / 6; --i >= 0;)
        if (group3Counts[ptm][i] > 0) {
          group3Counts[0][i] -= group3Counts[ptm][i];
          if (group3Counts[0][i] == 0)
            group3Lists[0] = group3Lists[0].substring(0, i * 6) + ",["
                + group3Lists[0].substring(i * 6 + 2);
        }
    }
    if (group3Lists != null) {
      modelSetAuxiliaryInfo.put("group3Lists", ArrayUtil.deleteElements(
          group3Lists, modelIndex, 1));
      modelSetAuxiliaryInfo.put("group3Counts", ArrayUtil.deleteElements(
          group3Counts, modelIndex, 1));
    }

    //fix cellInfos array
    if (unitCells != null) {
      unitCells = (SymmetryInterface[]) ArrayUtil.deleteElements(unitCells,
          modelIndex, 1);
    }

    // correct stateScripts, particularly CONNECT scripts
    for (int i = stateScripts.size(); --i >= 0;) {
      if (!stateScripts.get(i).deleteAtoms(modelIndex, bsBonds, bsAtoms)) {
        stateScripts.remove(i);
      }
    }

    // set to recreate bounding box
    deleteModelAtoms(firstAtomIndex, nAtoms, bsAtoms);
    viewer.deleteModelAtoms(firstAtomIndex, nAtoms, bsAtoms);
  }

  @SuppressWarnings("unchecked")
  public String getMoInfo(int modelIndex) {
    SB sb = new SB();
    for (int m = 0; m < modelCount; m++) {
      if (modelIndex >= 0 && m != modelIndex) {
        continue;
      }
      Map<String, Object> moData = (Map<String, Object>) viewer
          .getModelAuxiliaryInfoValue(m, "moData");
      if (moData == null) {
        continue;
      }
      JmolList<Map<String, Object>> mos = (JmolList<Map<String, Object>>) (moData
          .get("mos"));
      int nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0) {
        continue;
      }
      for (int i = nOrb; --i >= 0;) {
        Map<String, Object> mo = mos.get(i);
        String type = (String) mo.get("type");
        if (type == null) {
          type = "";
        }
        String units = (String) mo.get("energyUnits");
        if (units == null) {
          units = "";
        }
        Float occ = (Float) mo.get("occupancy");
        if (occ != null) {
          type = "occupancy " + occ.floatValue() + " " + type;
        }
        String sym = (String) mo.get("symmetry");
        if (sym != null) {
          type += sym;
        }
        String energy = "" + mo.get("energy");
        if (Float.isNaN(Parser.parseFloatStr(energy)))
          sb.append(TextFormat.sprintf("model %-2s;  mo %-2i # %s\n",
              "sis", new Object[] {getModelNumberDotted(m), Integer.valueOf(i + 1),
                  type }));
        else
          sb.append(TextFormat.sprintf(
              "model %-2s;  mo %-2i # energy %-8.3f %s %s\n", 
              "sifss", 
              new Object[] {
                  getModelNumberDotted(m), Integer.valueOf(i + 1),
                  mo.get("energy"), units, type }));
      }
    }
    return sb.toString();
  }

  public void assignAtom(int atomIndex, String type, boolean autoBond) {

    if (type == null)
      type = "C";

    // not as simple as just defining an atom.
    // if we click on an H, and C is being defined,
    // this sprouts an sp3-carbon at that position.

    Atom atom = atoms[atomIndex];
    BS bs = new BS();
    boolean wasH = (atom.getElementNumber() == 1);
    int atomicNumber = Elements.elementNumberFromSymbol(type, true);

    // 1) change the element type or charge

    boolean isDelete = false;
    if (atomicNumber > 0) {
      setElement(atom, atomicNumber);
      viewer.setShapeSizeRD(JC.SHAPE_BALLS, viewer
          .getDefaultRadiusData(), BSUtil.newAndSetBit(atomIndex));
      setAtomName(atomIndex, type + atom.getAtomNumber());
      if (!models[atom.modelIndex].isModelKit)
        taintAtom(atomIndex, TAINT_ATOMNAME);
    } else if (type.equals("Pl")) {
      atom.setFormalCharge(atom.getFormalCharge() + 1);
    } else if (type.equals("Mi")) {
      atom.setFormalCharge(atom.getFormalCharge() - 1);
    } else if (type.equals("X")) {
      isDelete = true;
    } else if (!type.equals(".")) {
      return; // uninterpretable
    }

    // 2) delete noncovalent bonds and attached hydrogens for that atom.

    removeUnnecessaryBonds(atom, isDelete);

    // 3) adjust distance from previous atom.

    float dx = 0;
    if (atom.getCovalentBondCount() == 1)
      if (wasH) {
        dx = 1.50f;
      } else if (!wasH && atomicNumber == 1) {
        dx = 1.0f;
      }
    if (dx != 0) {
      V3 v = V3.newV(atom);
      v.sub(atoms[atom.getBondedAtomIndex(0)]);
      float d = v.length();
      v.normalize();
      v.scale(dx - d);
      setAtomCoordRelative(atomIndex, v.x, v.y, v.z);
    }

    BS bsA = BSUtil.newAndSetBit(atomIndex);

    if (atomicNumber != 1 && autoBond) {

      // 4) clear out all atoms within 1.0 angstrom
      validateBspf(false);
      bs = getAtomsWithinBs(1.0f, bsA, false, null);
      bs.andNot(bsA);
      if (bs.nextSetBit(0) >= 0)
        viewer.deleteAtoms(bs, false);

      // 5) attach nearby non-hydrogen atoms (rings)

      bs = viewer.getModelUndeletedAtomsBitSet(atom.modelIndex);
      bs.andNot(getAtomBitsMDa(T.hydrogen, null));
      makeConnections2(0.1f, 1.8f, 1, T.create, bsA, bs, null, false, false,
          0);

      // 6) add hydrogen atoms

    }
    viewer.addHydrogens(bsA, false, true);
  }

  public void deleteAtoms(BS bs) {
    if (bs == null)
      return;
    BS bsBonds = new BS();
    for (int i = bs.nextSetBit(0); i >= 0 && i < atomCount; i = bs
        .nextSetBit(i + 1))
      atoms[i].delete(bsBonds);
    for (int i = 0; i < modelCount; i++) {
      models[i].bsAtomsDeleted.or(bs);
      models[i].bsAtomsDeleted.and(models[i].bsAtoms);
    }
    deleteBonds(bsBonds, false);
  }

  /*
   * <molecule title="acetic_acid.mol"
   * xmlns="http://www.xml-cml.org/schema/cml2/core"
   * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   * xsi:schemaLocation="http://www.xml-cml.org/schema/cml2/core cmlAll.xsd">
   * <atomArray> <atom id="a1" elementType="C" x3="0.1853" y3="0.0096"
   * z3="0.4587"/> <atom id="a2" elementType="O" x3="0.6324" y3="1.0432"
   * z3="0.8951"/> <atom id="a3" elementType="C" x3="-1.0665" y3="-0.1512"
   * z3="-0.3758"/> <atom id="a4" elementType="O" x3="0.7893" y3="-1.1734"
   * z3="0.6766" formalCharge="-1"/> <atom id="a5" elementType="H" x3="-1.7704"
   * y3="-0.8676" z3="0.1055"/> <atom id="a6" elementType="H" x3="-0.8068"
   * y3="-0.5215" z3="-1.3935"/> <atom id="a7" elementType="H" x3="-1.5889"
   * y3="0.8259" z3="-0.4854"/> </atomArray> <bondArray> <bond atomRefs2="a1 a2"
   * order="partial12"/> <bond atomRefs2="a1 a3" order="S"/> <bond
   * atomRefs2="a1 a4" order="partial12"/> <bond atomRefs2="a3 a5" order="S"/>
   * <bond atomRefs2="a3 a6" order="S"/> <bond atomRefs2="a3 a7" order="S"/>
   * </bondArray> </molecule>
   */
  public String getModelCml(BS bs, int atomsMax, boolean addBonds) {
    SB sb = new SB();
    int nAtoms = BSUtil.cardinalityOf(bs);
    if (nAtoms == 0)
      return "";
    XmlUtil.openTag(sb, "molecule");
    XmlUtil.openTag(sb, "atomArray");
    BS bsAtoms = new BS();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (--atomsMax < 0)
        break;
      Atom atom = atoms[i];
      String name = atom.getAtomName();
      TextFormat.simpleReplace(name, "\"", "''");
      bsAtoms.set(atom.index);
      XmlUtil.appendTag(sb, "atom/", new String[] { "id",
          "a" + (atom.index + 1), "title", atom.getAtomName(), "elementType",
          atom.getElementSymbol(), "x3", "" + atom.x, "y3", "" + atom.y, "z3",
          "" + atom.z });
    }
    XmlUtil.closeTag(sb, "atomArray");
    if (addBonds) {
      XmlUtil.openTag(sb, "bondArray");
      for (int i = 0; i < bondCount; i++) {
        Bond bond = bonds[i];
        Atom a1 = bond.atom1;
        Atom a2 = bond.atom2;
        if (!bsAtoms.get(a1.index) || !bsAtoms.get(a2.index))
          continue;
        String order = JmolEdge.getCmlBondOrder(bond.order);
        if (order == null)
          continue;
        XmlUtil.appendTag(sb, "bond/", new String[] { "atomRefs2",
            "a" + (bond.atom1.index + 1) + " a" + (bond.atom2.index + 1),
            "order", order, });
      }
      XmlUtil.closeTag(sb, "bondArray");
    }
    XmlUtil.closeTag(sb, "molecule");
    return sb.toString();
  }

  // atom addition //

  void adjustAtomArrays(int[] map, int i0, int atomCount) {
    // from ModelLoader, after hydrogen atom addition
    this.atomCount = atomCount;
    for (int i = i0; i < atomCount; i++) {
      atoms[i] = atoms[map[i]];
      atoms[i].index = i;
      Model m = models[atoms[i].modelIndex];
      if (m.firstAtomIndex == map[i])
        m.firstAtomIndex = i;
      m.bsAtoms.set(i);
    }
    if (vibrationVectors != null)
      for (int i = i0; i < atomCount; i++)
        vibrationVectors[i] = vibrationVectors[map[i]];
    if (occupancies != null)
      for (int i = i0; i < atomCount; i++)
        occupancies[i] = occupancies[map[i]];
    if (bfactor100s != null)
      for (int i = i0; i < atomCount; i++)
        bfactor100s[i] = bfactor100s[map[i]];
    if (partialCharges != null)
      for (int i = i0; i < atomCount; i++)
        partialCharges[i] = partialCharges[map[i]];
    if (ellipsoids != null)
      for (int i = i0; i < atomCount; i++)
        ellipsoids[i] = ellipsoids[map[i]];
    if (atomNames != null)
      for (int i = i0; i < atomCount; i++)
        atomNames[i] = atomNames[map[i]];
    if (atomTypes != null)
      for (int i = i0; i < atomCount; i++)
        atomTypes[i] = atomTypes[map[i]];
    if (atomSerials != null)
      for (int i = i0; i < atomCount; i++)
        atomSerials[i] = atomSerials[map[i]];
  }

  protected void growAtomArrays(int newLength) {
    atoms = (Atom[]) ArrayUtil.arrayCopyObject(atoms, newLength);
    if (vibrationVectors != null)
      vibrationVectors = (V3[]) ArrayUtil.arrayCopyObject(vibrationVectors,
          newLength);
    if (occupancies != null)
      occupancies = ArrayUtil.arrayCopyByte(occupancies, newLength);
    if (bfactor100s != null)
      bfactor100s = ArrayUtil.arrayCopyShort(bfactor100s, newLength);
    if (partialCharges != null)
      partialCharges = ArrayUtil.arrayCopyF(partialCharges, newLength);
    if (ellipsoids != null)
      ellipsoids = (Quadric[][]) ArrayUtil.arrayCopyObject(ellipsoids, newLength);
    if (atomNames != null)
      atomNames = ArrayUtil.arrayCopyS(atomNames, newLength);
    if (atomTypes != null)
      atomTypes = ArrayUtil.arrayCopyS(atomTypes, newLength);
    if (atomSerials != null)
      atomSerials = ArrayUtil.arrayCopyI(atomSerials, newLength);
  }

  public Atom addAtom(int modelIndex, Group group,
                      short atomicAndIsotopeNumber, String atomName,
                      int atomSerial, int atomSite, float x, float y, float z,
                      float radius, float vectorX, float vectorY,
                      float vectorZ, int formalCharge, float partialCharge,
                      int occupancy, float bfactor, Quadric[] ellipsoid,
                      boolean isHetero, byte specialAtomID, BS atomSymmetry) {
    Atom atom = new Atom(modelIndex, atomCount, x, y, z, radius, atomSymmetry,
        atomSite, atomicAndIsotopeNumber, formalCharge, isHetero);
    models[modelIndex].atomCount++;
    models[modelIndex].bsAtoms.set(atomCount);
    if (atomicAndIsotopeNumber % 128 == 1)
      models[modelIndex].hydrogenCount++;
    if (atomCount >= atoms.length)
      growAtomArrays(atomCount + 100); // only due to added hydrogens

    atoms[atomCount] = atom;
    setBFactor(atomCount, bfactor);
    setOccupancy(atomCount, occupancy);
    setPartialCharge(atomCount, partialCharge);
    if (ellipsoid != null)
      setEllipsoid(atomCount, ellipsoid);
    atom.group = group;
    atom.colixAtom = viewer.getColixAtomPalette(atom, EnumPalette.CPK.id);
    if (atomName != null) {
      int i;
      if ((i = atomName.indexOf('\0')) >= 0) {
        if (atomTypes == null)
          atomTypes = new String[atoms.length];
        atomTypes[atomCount] = atomName.substring(i + 1);
        atomName = atomName.substring(0, i);
      }
      atom.atomID = specialAtomID;
      if (specialAtomID == 0) {
        if (atomNames == null)
          atomNames = new String[atoms.length];
        atomNames[atomCount] = atomName.intern();
      }
    }
    if (atomSerial != Integer.MIN_VALUE) {
      if (atomSerials == null)
        atomSerials = new int[atoms.length];
      atomSerials[atomCount] = atomSerial;
    }
    if (!Float.isNaN(vectorX))
      setVibrationVector(atomCount, vectorX, vectorY, vectorZ);
    atomCount++;
    return atom;
  }

  public String getInlineData(int modelIndex) {
    SB data = null;
    if (modelIndex >= 0)
      data = models[modelIndex].loadScript;
    else
      for (modelIndex = modelCount; --modelIndex >= 0;)
        if ((data = models[modelIndex].loadScript).length() > 0)
          break;
    int pt = data.lastIndexOf("data \"");
    if (pt < 0)
      return null;
    pt = data.indexOf2("\"", pt + 7);
    int pt2 = data.lastIndexOf("end \"");
    if (pt2 < pt || pt < 0)
      return null;
    return data.substring2(pt + 2, pt2);
  }

  public boolean isAtomPDB(int i) {
    return i >= 0 && models[atoms[i].modelIndex].isBioModel;
  }

  public boolean isAtomAssignable(int i) {
    return i >= 0 && atoms[i].modelIndex == modelCount - 1;
  }

  public int getGroupAtom(Atom atom, int offset, String name) {
    Group g = atom.group;
    int monomerIndex = g.getMonomerIndex();
    if (monomerIndex < 0)
      return -1;
    Group[] groups = g.getGroups();
    int ipt = monomerIndex + offset;
    if (ipt >= 0 && ipt < groups.length) {
      Group m = groups[ipt];
      if (offset == 1 && !m.isConnectedPrevious())
        return -1;
      if ("0".equals(name))
        return m.leadAtomIndex;
      // this is OK -- only used for finding special atom by name
      for (int i = m.firstAtomIndex; i <= m.lastAtomIndex; i++)
        if (name == null || name.equalsIgnoreCase(atoms[i].getAtomName()))
          return i;
    }
    return -1;
  }

  public boolean haveModelKit() {
    for (int i = 0; i < modelCount; i++)
      if (models[i].isModelKit)
        return true;
    return false;
  }

  public BS getModelKitStateBitset(BS bs, BS bsDeleted) {
    // task here is to remove bits from bs that are deleted atoms in 
    // models that are model kits.

    BS bs1 = BSUtil.copy(bsDeleted);
    for (int i = 0; i < modelCount; i++)
      if (!models[i].isModelKit)
        bs1.andNot(models[i].bsAtoms);
    return BSUtil.deleteBits(bs, bs1);
  }

  public void setAtomNamesAndNumbers(int iFirst, int baseAtomIndex,
                                     AtomCollection mergeSet) {
    // first, validate that all atomSerials are NaN
    if (baseAtomIndex < 0)
      iFirst = models[atoms[iFirst].modelIndex].firstAtomIndex;
    if (atomSerials == null)
      atomSerials = new int[atomCount];
    if (atomNames == null)
      atomNames = new String[atomCount];
    // now, we'll assign 1-based atom numbers within each model
    boolean isZeroBased = isXYZ && viewer.getZeroBasedXyzRasmol();
    int lastModelIndex = Integer.MAX_VALUE;
    int atomNo = 1;
    for (int i = iFirst; i < atomCount; ++i) {
      Atom atom = atoms[i];
      if (atom.modelIndex != lastModelIndex) {
        lastModelIndex = atom.modelIndex;
        atomNo = (isZeroBased ? 0 : 1);
      }
      // 1) do not change numbers assigned by adapter
      // 2) do not change the number already assigned when merging
      // 3) restart numbering with new atoms, not a continuation of old
      if (atomSerials[i] == 0 || baseAtomIndex < 0)
        atomSerials[i] = (i < baseAtomIndex ? mergeSet.atomSerials[i] : atomNo);
      if (atomNames[i] == null || baseAtomIndex < 0)
        atomNames[i] = (atom.getElementSymbol() + atomSerials[i]).intern();
      if (!models[lastModelIndex].isModelKit || atom.getElementNumber() > 0
          && !atom.isDeleted())
        atomNo++;
    }
  }

  public void setUnitCellOffset(int modelIndex, P3 pt, int ijk) {
    for (int i = modelIndex; i < modelCount; i++) {
      if (i < 0 || modelIndex >= 0 && i != modelIndex
          && models[i].trajectoryBaseIndex != modelIndex)
        continue;
      SymmetryInterface unitCell = getUnitCell(i);
      if (unitCell == null)
        continue;
      if (pt == null)
        unitCell.setOffset(ijk);
      else
        unitCell.setOffsetPt(pt);
    }
  }
  
  public void connect(float[][] connections) {
    // array of [index1 index2 order diameter energy]
    resetMolecules();
    BS bsDelete = new BS();
    for (int i = 0; i < connections.length; i++) {
      float[] f = connections[i];
      if (f == null || f.length < 2)
        continue;
      int index1 = (int) f[0];
      boolean addGroup = (index1 < 0);
      if (addGroup)
        index1 = -1 - index1;
      int index2 = (int) f[1];
      if (index2 < 0 || index1 >= atomCount || index2 >= atomCount)
        continue;
      int order = (f.length > 2 ? (int) f[2] : JmolEdge.BOND_COVALENT_SINGLE);
      if (order < 0)
        order &= 0xFFFF; // 12.0.1 was saving struts as negative numbers
      short mad = (f.length > 3 ? (short) (1000f * connections[i][3]) : getDefaultMadFromOrder(order));
      if (order == 0 || mad == 0 && order != JmolEdge.BOND_STRUT && !Bond.isOrderH(order)) {
        Bond b = atoms[index1].getBond(atoms[index2]);
        if (b != null)
          bsDelete.set(b.index);
        continue; 
      }
      float energy = (f.length > 4 ? f[4] : 0);
      bondAtoms(atoms[index1], atoms[index2], order, mad, null, energy, addGroup, true);
    }
    if (bsDelete.nextSetBit(0) >= 0)
      deleteBonds(bsDelete, false);
  }

  public boolean allowSpecAtom() {
    // old Chime scripts use *.C for _C
    return modelCount != 1 || models[0].isBioModel;
  }

  public void setFrameDelayMs(long millis, BS bsModels) {
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      models[models[i].trajectoryBaseIndex].frameDelay = millis;
  }
  
  public long getFrameDelayMs(int i) {
    return (i < models.length && i >= 0 ? models[models[i].trajectoryBaseIndex].frameDelay : 0);
  }
  
  /**
   * 
   * @param id
   * @return model index if found; 
   *     -2 if file found but model not found
   *     -2 if no file indicated and no model found
   *     -1 if no such file 
   */
  public int getModelIndexFromId(String id) {
    boolean haveFile = (id.indexOf("#") >= 0);
    boolean isBaseModel = id.toLowerCase().endsWith(".basemodel");
    if (isBaseModel) 
      id = id.substring(0, id.length() - 10);
    int errCode = -1;
    String fname = null;
    for (int i = 0; i < modelCount; i++) {
      String mid = (String) getModelAuxiliaryInfoValue(i, "modelID");
      if (mid == null && (mid = getModelTitle(i)) == null)
        continue;
      if (haveFile) {
        fname = getModelFileName(i) + "#";
        mid = fname + mid;
      }
      if (id.equalsIgnoreCase(mid))
        return (isBaseModel ? viewer.getJDXBaseModelIndex(i) : i);
      if (fname != null && id.startsWith(fname))
        errCode = -2;
    }
    return (fname == null && !haveFile ? -2 : errCode);
  }

  public Map<String, Object> getAuxiliaryInfo(BS bsModels) {
    Map<String, Object> info = modelSetAuxiliaryInfo;
    if (info == null)
      return null;
    JmolList<Map<String, Object>> models = new  JmolList<Map<String, Object>>();
    for (int i = 0; i < modelCount; ++i) {
      if (bsModels != null && !bsModels.get(i)) {
        continue;
      }
      Map<String, Object> modelinfo = getModelAuxiliaryInfo(i);
      models.addLast(modelinfo);
    }
    info.put("models", models);
    return info;
  }

  
}
