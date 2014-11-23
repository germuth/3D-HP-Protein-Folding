/* $RCSfile$
* $Author: hansonr $
* $Date: 2007-04-24 08:15:07 -0500 (Tue, 24 Apr 2007) $
* $Revision: 7479 $
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
package org.jmol.modelsetbio;

import org.jmol.constant.EnumStructure;
import org.jmol.io.OutputStringBuilder;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.util.Escape;

import org.jmol.util.BS;
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.Quaternion;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;
import org.jmol.viewer.Viewer;
import org.jmol.script.T;


import org.jmol.util.JmolList;
import java.util.Hashtable;

import java.util.Map;

public abstract class BioPolymer {

  Monomer[] monomers;

  // these arrays will be one longer than the polymerCount
  // we probably should have better names for these things
  // holds center points between alpha carbons or sugar phosphoruses

  public Model model;
  protected P3[] leadMidpoints;
  protected P3[] leadPoints;
  protected P3[] controlPoints;
  // holds the vector that runs across the 'ribbon'
  protected V3[] wingVectors;

  protected int[] leadAtomIndices;

  protected int type = TYPE_NOBONDING;
  public int bioPolymerIndexInModel;
  public int monomerCount;

  protected final static int TYPE_NOBONDING = 0; // could be phosphorus or alpha
  protected final static int TYPE_AMINO = 1;
  protected final static int TYPE_NUCLEIC = 2;
  protected final static int TYPE_CARBOHYDRATE = 3;

  public Group[] getGroups() {
    return monomers;
  }

  BioPolymer(Monomer[] monomers) {
    this.monomers = monomers;
    monomerCount = monomers.length;
    for (int i = monomerCount; --i >= 0;)
      monomers[i].setBioPolymer(this, i);
    model = monomers[0].getModel();
  }

  public void getRange(BS bs) {
    // this is OK -- doesn't relate to added hydrogens
    if (monomerCount == 0)
      return;
    bs.setBits(monomers[0].firstAtomIndex,
        monomers[monomerCount - 1].lastAtomIndex + 1);
  }

  public void clearStructures() {
    for (int i = 0; i < monomerCount; i++)
      monomers[i].setStructure(null);
  }

  protected void removeProteinStructure(int monomerIndex, int count) {
    Monomer m = monomers[monomerIndex];
    EnumStructure type = m.getProteinStructureType();
    int mLast = -1;
    for (int i = 0, pt = monomerIndex; i < count && pt < monomerCount; i++, pt++) {
      monomers[pt].setStructure(null);
      mLast = monomers[pt].setProteinStructureType(type, mLast);
    }
  }

  public int[] getLeadAtomIndices() {
    if (leadAtomIndices == null) {
      leadAtomIndices = new int[monomerCount];
      invalidLead = true;
    }
    if (invalidLead) {
      for (int i = monomerCount; --i >= 0;)
        leadAtomIndices[i] = monomers[i].leadAtomIndex;
      invalidLead = false;
    }
    return leadAtomIndices;
  }

  protected int getIndex(char chainID, int seqcode) {
    int i;
    for (i = monomerCount; --i >= 0;)
      if (monomers[i].getChainID() == chainID)
        if (monomers[i].getSeqcode() == seqcode)
          break;
    return i;
  }

  final P3 getLeadPoint(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtom();
  }

//  void getLeadPoint2(int groupIndex, Point3f midPoint) {
//    if (groupIndex == monomerCount)
//      --groupIndex;
//    midPoint.setT(getLeadPoint(groupIndex));
//  }

  private final P3 getInitiatorPoint() {
    return monomers[0].getInitiatorAtom();
  }

  private final P3 getTerminatorPoint() {
    return monomers[monomerCount - 1].getTerminatorAtom();
  }

  /*
   * public final Atom getLeadAtom(int monomerIndex) { return
   * monomers[monomerIndex].getLeadAtom(); }
   */
  void getLeadMidPoint(int groupIndex, P3 midPoint) {
    if (groupIndex == monomerCount) {
      --groupIndex;
    } else if (groupIndex > 0) {
      midPoint.setT(getLeadPoint(groupIndex));
      midPoint.add(getLeadPoint(groupIndex - 1));
      midPoint.scale(0.5f);
      return;
    }
    midPoint.setT(getLeadPoint(groupIndex));
  }

  // this might change in the future ... if we calculate a wing point
  // without an atom for an AlphaPolymer
  final P3 getWingPoint(int polymerIndex) {
    return monomers[polymerIndex].getWingAtom();
  }

  public void getConformation(BS bsConformation, int conformationIndex) {
    Atom[] atoms = model.getModelSet().atoms;
    for (int i = monomerCount; --i >= 0;)
      monomers[i].getConformation(atoms, bsConformation, conformationIndex);
    recalculateLeadMidpointsAndWingVectors();
  }

  public void setConformation(BS bsSelected) {
    Atom[] atoms = model.getModelSet().atoms;
    for (int i = monomerCount; --i >= 0;)
      monomers[i].updateOffsetsForAlternativeLocations(atoms, bsSelected);
    recalculateLeadMidpointsAndWingVectors();
  }

  private boolean invalidLead;
  protected boolean invalidControl = false;

  public void recalculateLeadMidpointsAndWingVectors() {
    //System.out.println("biopo recalcLeadMP");
    invalidLead = invalidControl = true;
    getLeadAtomIndices();
    resetHydrogenPoints();
    calcLeadMidpointsAndWingVectors();
  }

  protected void resetHydrogenPoints() {
    // amino polymer only
  }

  public P3[] getLeadMidpoints() {
    if (leadMidpoints == null)
      calcLeadMidpointsAndWingVectors();
    return leadMidpoints;
  }

  P3[] getLeadPoints() {
    if (leadPoints == null)
      calcLeadMidpointsAndWingVectors();
    return leadPoints;
  }

  public P3[] getControlPoints(boolean isTraceAlpha, float sheetSmoothing,
                                    boolean invalidate) {
    if (invalidate)
      invalidControl = true;
    return (!isTraceAlpha ? leadMidpoints : sheetSmoothing == 0 ? leadPoints
        : getControlPoints2(sheetSmoothing));
  }

  protected float sheetSmoothing;

  private P3[] getControlPoints2(float sheetSmoothing) {
    if (!invalidControl && sheetSmoothing == this.sheetSmoothing)
      return controlPoints;
    getLeadPoints();
    V3 v = new V3();
    if (controlPoints == null)
      controlPoints = new P3[monomerCount + 1];
    if (!Float.isNaN(sheetSmoothing))
      this.sheetSmoothing = sheetSmoothing;
    for (int i = 0; i < monomerCount; i++)
      controlPoints[i] = getControlPoint(i, v);
    controlPoints[monomerCount] = getTerminatorPoint();
    //controlPoints[monomerCount] = controlPoints[monomerCount - 1];
    invalidControl = false;
    return controlPoints;
  }

  /**
   * 
   * @param i
   * @param v
   * @return the leadPoint unless a protein sheet residue (see AlphaPolymer)
   */
  protected P3 getControlPoint(int i, V3 v) {
    return leadPoints[i];
  }

  public final V3[] getWingVectors() {
    if (leadMidpoints == null) // this is correct ... test on leadMidpoints
      calcLeadMidpointsAndWingVectors();
    return wingVectors; // wingVectors might be null ... before autocalc
  }

  protected boolean hasWingPoints; // true for nucleic and SOME amino

  public BS reversed;

  public boolean twistedSheets;

  private final void calcLeadMidpointsAndWingVectors() {
    if (leadMidpoints == null) {
      leadMidpoints = new P3[monomerCount + 1];
      leadPoints = new P3[monomerCount + 1];
      wingVectors = new V3[monomerCount + 1];
      sheetSmoothing = Float.MIN_VALUE;
    }
    if (reversed == null)
      reversed = BS.newN(monomerCount);
    else
      reversed.clearAll();
    twistedSheets = model.modelSet.viewer.getCartoonFlag(T.twistedsheets);
    V3 vectorA = new V3();
    V3 vectorB = new V3();
    V3 vectorC = new V3();
    V3 vectorD = new V3();

    P3 leadPointPrev, leadPoint;
    leadMidpoints[0] = getInitiatorPoint();
    leadPoints[0] = leadPoint = getLeadPoint(0);
    V3 previousVectorD = null;
    // proteins:
    // C O (wing)
    // \ |
    // CA--N--C O (wing)
    // (lead) \ |
    // CA--N--C
    // (lead) \
    // CA--N
    // (lead)
    // mon# 2 1 0
    for (int i = 1; i < monomerCount; ++i) {
      leadPointPrev = leadPoint;
      leadPoints[i] = leadPoint = getLeadPoint(i);
      P3 midpoint = P3.newP(leadPoint);
      midpoint.add(leadPointPrev);
      midpoint.scale(0.5f);
      leadMidpoints[i] = midpoint;
      if (hasWingPoints) {
        vectorA.sub2(leadPoint, leadPointPrev);
        vectorB.sub2(leadPointPrev, getWingPoint(i - 1));
        vectorC.cross(vectorA, vectorB);
        vectorD.cross(vectorA, vectorC);
        vectorD.normalize();
        if (!twistedSheets && previousVectorD != null
            && previousVectorD.angle(vectorD) > Math.PI / 2) {
          reversed.set(i);
          vectorD.scale(-1);
        }
        previousVectorD = wingVectors[i] = V3.newV(vectorD);
        //System.out.println("draw v" + i + " vector @{point" + midpoint + "}  @{point" + vectorD + "}"); 
      }
    }
    leadPoints[monomerCount] = leadMidpoints[monomerCount] = getTerminatorPoint();
    if (!hasWingPoints) {
      if (monomerCount < 3) {
        wingVectors[1] = unitVectorX;
      } else {
        // auto-calculate wing vectors based upon lead atom positions only
        V3 previousVectorC = null;
        for (int i = 1; i < monomerCount; ++i) {
          // perfect for traceAlpha on; reasonably OK for traceAlpha OFF
          vectorA.sub2(leadMidpoints[i], leadPoints[i]);
          vectorB.sub2(leadPoints[i], leadMidpoints[i + 1]);
          vectorC.cross(vectorA, vectorB);
          vectorC.normalize();
          if (previousVectorC != null
              && previousVectorC.angle(vectorC) > Math.PI / 2)
            vectorC.scale(-1);
          previousVectorC = wingVectors[i] = V3.newV(vectorC);
        }
      }
    }
    wingVectors[0] = wingVectors[1];
    wingVectors[monomerCount] = wingVectors[monomerCount - 1];
  }

  private final V3 unitVectorX = V3.new3(1, 0, 0);

  public void findNearestAtomIndex(int xMouse, int yMouse, Atom[] closest,
                                   short[] mads, int myVisibilityFlag,
                                   BS bsNot) {
    for (int i = monomerCount; --i >= 0;) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0)
        continue;
      Atom a = monomers[i].getLeadAtom();
      if (!a.isVisible(0) || bsNot != null && bsNot.get(a.index))
        continue;
      if (mads[i] > 0 || mads[i + 1] > 0)
        monomers[i].findNearestAtomIndex(xMouse, yMouse, closest, mads[i],
            mads[i + 1]);
    }
  }

  private int selectedMonomerCount;

  int getSelectedMonomerCount() {
    return selectedMonomerCount;
  }

  BS bsSelectedMonomers;

  public void calcSelectedMonomersCount(BS bsSelected) {
    selectedMonomerCount = 0;
    if (bsSelectedMonomers == null)
      bsSelectedMonomers = new BS();
    bsSelectedMonomers.clearAll();
    for (int i = 0; i < monomerCount; i++) {
      if (monomers[i].isSelected(bsSelected)) {
        ++selectedMonomerCount;
        bsSelectedMonomers.set(i);
      }
    }
  }

  boolean isMonomerSelected(int i) {
    return (i >= 0 && bsSelectedMonomers.get(i));
  }

  public int getPolymerPointsAndVectors(int last, BS bs,
                                        JmolList<P3[]> vList,
                                        boolean isTraceAlpha,
                                        float sheetSmoothing) {
    P3[] points = getControlPoints(isTraceAlpha, sheetSmoothing, false);
    V3[] vectors = getWingVectors();
    int count = monomerCount;
    for (int j = 0; j < count; j++)
      if (bs.get(monomers[j].leadAtomIndex)) {
        vList.addLast(new P3[] { points[j], P3.newP(vectors[j]) });
        last = j;
      } else if (last != Integer.MAX_VALUE - 1) {
        vList.addLast(new P3[] { points[j], P3.newP(vectors[j]) });
        last = Integer.MAX_VALUE - 1;
      }
    if (last + 1 < count)
      vList.addLast(new P3[] { points[last + 1],
          P3.newP(vectors[last + 1]) });
    return last;
  }

  public String getSequence() {
    char[] buf = new char[monomerCount];
    for (int i = 0; i < monomerCount; i++)
      buf[i] = monomers[i].getGroup1();
    return String.valueOf(buf);
  }

  public Map<String, Object> getPolymerInfo(BS bs) {
    Map<String, Object> returnInfo = new Hashtable<String, Object>();
    JmolList<Map<String, Object>> info = new  JmolList<Map<String, Object>>();
    JmolList<Map<String, Object>> structureInfo = null;
    ProteinStructure ps;
    ProteinStructure psLast = null;
    int n = 0;
    for (int i = 0; i < monomerCount; i++) {
      if (bs.get(monomers[i].leadAtomIndex)) {
        Map<String, Object> monomerInfo = monomers[i].getMyInfo();
        monomerInfo.put("monomerIndex", Integer.valueOf(i));
        info.addLast(monomerInfo);
        if ((ps = getProteinStructure(i)) != null && ps != psLast) {
          Map<String, Object> psInfo = new Hashtable<String, Object>();
          (psLast = ps).getInfo(psInfo);
          if (structureInfo == null) {
            structureInfo = new  JmolList<Map<String, Object>>();
          }
          psInfo.put("index", Integer.valueOf(n++));
          structureInfo.addLast(psInfo);
        }
      }
    }
    if (info.size() > 0) {
      returnInfo.put("sequence", getSequence());
      returnInfo.put("monomers", info);
      if (structureInfo != null)
        returnInfo.put("structures", structureInfo);
    }
    return returnInfo;
  }

  public void getPolymerSequenceAtoms(int group1, int nGroups,
                                      BS bsInclude, BS bsResult) {
    for (int i = Math.min(monomerCount, group1 + nGroups); --i >= group1;)
      monomers[i].getMonomerSequenceAtoms(bsInclude, bsResult);
  }

  public ProteinStructure getProteinStructure(int monomerIndex) {
    return monomers[monomerIndex].getProteinStructure();
  }

  public boolean haveParameters;

  public boolean calcParameters() {
    haveParameters = true;
    return calcEtaThetaAngles() || calcPhiPsiAngles();
  }

  protected boolean calcEtaThetaAngles() {
    return false;
  }

  protected boolean calcPhiPsiAngles() {
    return false;
  }

  final private static String[] qColor = { "yellow", "orange", "purple" };

  final public static void getPdbData(Viewer viewer, BioPolymer p, char ctype,
                                      char qtype, int mStep, int derivType,
                                      BS bsAtoms, BS bsSelected,
                                      boolean bothEnds, boolean isDraw,
                                      boolean addHeader, LabelToken[] tokens,
                                      OutputStringBuilder pdbATOM,
                                      SB pdbCONECT, BS bsWritten) {
    boolean calcRamachandranStraightness = (qtype == 'C' || qtype == 'P');
    boolean isRamachandran = (ctype == 'R' || ctype == 'S'
        && calcRamachandranStraightness);
    if (isRamachandran && !p.calcPhiPsiAngles())
      return;
    /*
     * A quaternion visualization involves assigning a frame to each amino acid
     * residue or nucleic acid base. This frame is an orthonormal x-y-z axis
     * system, which can be defined any number of ways.
     * 
     * 'c' C-alpha, as defined by Andy Hanson, U. of Indiana (unpublished
     * results)
     * 
     * X: CA->C (carbonyl carbon) Z: X x (CA->N) Y: Z x X
     * 
     * 'p' Peptide plane as defined by Bob Hanson, St. Olaf College (unpublished
     * results)
     * 
     * X: C->CA Z: X x (C->N') Y: Z x X
     * 
     * 'n' NMR frame using Beta = 17 degrees (Quine, Cross, et al.)
     * 
     * Y: (N->H) x (N->CA) X: R[Y,-17](N->H) Z: X x Y
     * 
     * quaternion types:
     * 
     * w, x, y, z : which of the q-terms to expunge in order to display the
     * other three.
     * 
     * 
     * a : absolute (standard) derivative r : relative (commuted) derivative s :
     * same as w but for calculating straightness
     */

    boolean isAmino = (p instanceof AminoPolymer);
    boolean isRelativeAlias = (ctype == 'r');
    boolean quaternionStraightness = (!isRamachandran && ctype == 'S');
    if (derivType == 2 && isRelativeAlias)
      ctype = 'w';
    if (quaternionStraightness)
      derivType = 2;
    boolean useQuaternionStraightness = (ctype == 'S');
    boolean writeRamachandranStraightness = ("rcpCP".indexOf(qtype) >= 0);
    if (Logger.debugging
        && (quaternionStraightness || calcRamachandranStraightness)) {
      Logger.debug("For straightness calculation: useQuaternionStraightness = "
          + useQuaternionStraightness + " and quaternionFrame = " + qtype);
    }
    if (addHeader && !isDraw) {
      pdbATOM.append("REMARK   6    AT GRP CH RESNO  ");
      switch (ctype) {
      default:
      case 'w':
        pdbATOM.append("x*10___ y*10___ z*10___      w*10__       ");
        break;
      case 'x':
        pdbATOM.append("y*10___ z*10___ w*10___      x*10__       ");
        break;
      case 'y':
        pdbATOM.append("z*10___ w*10___ x*10___      y*10__       ");
        break;
      case 'z':
        pdbATOM.append("w*10___ x*10___ y*10___      z*10__       ");
        break;
      case 'R':
        if (writeRamachandranStraightness)
          pdbATOM.append("phi____ psi____ theta         Straightness");
        else
          pdbATOM.append("phi____ psi____ omega-180    PartialCharge");
        break;
      }
      pdbATOM.append("    Sym   q0_______ q1_______ q2_______ q3_______");
      pdbATOM.append("  theta_  aaX_______ aaY_______ aaZ_______");
      if (ctype != 'R')
        pdbATOM.append("  centerX___ centerY___ centerZ___");
      if (qtype == 'n')
        pdbATOM.append("  NHX_______ NHY_______ NHZ_______");
      pdbATOM.append("\n\n");
    }
    float factor = (ctype == 'R' ? 1f : 10f);
    bothEnds = false;//&= !isDraw && !isRamachandran;
    for (int j = 0; j < (bothEnds ? 2 : 1); j++, factor *= -1)
      for (int i = 0; i < (mStep < 1 ? 1 : mStep); i++)
        getData(viewer, i, mStep, p, ctype, qtype, derivType, bsAtoms,
            bsSelected, isDraw, isRamachandran, calcRamachandranStraightness,
            useQuaternionStraightness, writeRamachandranStraightness,
            quaternionStraightness, factor, isAmino, isRelativeAlias, tokens,
            pdbATOM, pdbCONECT, bsWritten);
  }

  /**
   * 
   * @param viewer
   * @param m0
   * @param mStep
   * @param p
   * @param ctype
   * @param qtype
   * @param derivType
   * @param bsAtoms
   * @param bsSelected
   * @param isDraw
   * @param isRamachandran
   * @param calcRamachandranStraightness
   * @param useQuaternionStraightness
   * @param writeRamachandranStraightness
   * @param quaternionStraightness
   *        NOT USED
   * @param factor
   * @param isAmino
   * @param isRelativeAlias
   * @param tokens
   * @param pdbATOM
   * @param pdbCONECT
   * @param bsWritten
   */
  private static void getData(Viewer viewer, int m0, int mStep, BioPolymer p,
                              char ctype, char qtype, int derivType,
                              BS bsAtoms, BS bsSelected,
                              boolean isDraw, boolean isRamachandran,
                              boolean calcRamachandranStraightness,
                              boolean useQuaternionStraightness,
                              boolean writeRamachandranStraightness,
                              boolean quaternionStraightness, float factor,
                              boolean isAmino, boolean isRelativeAlias,
                              LabelToken[] tokens, OutputStringBuilder pdbATOM,
                              SB pdbCONECT, BS bsWritten) {
    String prefix = (derivType > 0 ? "dq" + (derivType == 2 ? "2" : "") : "q");
    Quaternion q;
    Atom aprev = null;
    Quaternion qprev = null;
    Quaternion dq = null;
    Quaternion dqprev = null;
    Quaternion qref = null;
    Atom atomLast = null;
    float x = 0, y = 0, z = 0, w = 0;
    String strExtra = "";
    float val1 = Float.NaN;
    float val2 = Float.NaN;
    P3 pt = (isDraw ? new P3() : null);

    int dm = (mStep <= 1 ? 1 : mStep);
    for (int m = m0; m < p.monomerCount; m += dm) {
      Monomer monomer = p.monomers[m];
      if (bsAtoms == null || bsAtoms.get(monomer.leadAtomIndex)) {
        Atom a = monomer.getLeadAtom();
        String id = monomer.getUniqueID();
        if (isRamachandran) {
          if (ctype == 'S')
            monomer.setGroupParameter(T.straightness, Float.NaN);
          x = monomer.getGroupParameter(T.phi);
          y = monomer.getGroupParameter(T.psi);
          z = monomer.getGroupParameter(T.omega);
          if (z < -90)
            z += 360;
          z -= 180; // center on 0
          if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) {
            if (bsAtoms != null)
              bsAtoms.clear(a.getIndex());
            continue;
          }
          float angledeg = (writeRamachandranStraightness ? p
              .calculateRamachandranHelixAngle(m, qtype) : 0);
          float straightness = (calcRamachandranStraightness
              || writeRamachandranStraightness ? getStraightness((float) Math
              .cos(angledeg / 2 / 180 * Math.PI)) : 0);
          if (ctype == 'S') {
            monomer.setGroupParameter(T.straightness, straightness);
            continue;
          }
          if (isDraw) {
            if (bsSelected != null && !bsSelected.get(a.getIndex()))
              continue;
            // draw arrow arc {3.N} {3.ca} {3.C} {131 -131 0.5} "phi -131"
            // draw arrow arc {3.CA} {3.C} {3.N} {0 133 0.5} "psi 133"
            // as looked DOWN the bond, with {pt1} in the back, using
            // standard dihedral/Jmol definitions for anticlockwise positive
            // angles
            AminoMonomer aa = (AminoMonomer) monomer;
            pt.set(-x, x, 0.5f);
            pdbATOM.append("draw ID \"phi").append(id).append("\" ARROW ARC ")
                .append(Escape.eP(aa.getNitrogenAtom())).append(
                    Escape.eP(a)).append(
                    Escape.eP(aa.getCarbonylCarbonAtom())).append(
                    Escape.eP(pt)).append(" \"phi = ").append(
                    String.valueOf(Math.round(x))).append("\" color ").append(
                    qColor[2]).append("\n");
            pt.set(0, y, 0.5f);
            pdbATOM.append("draw ID \"psi").append(id).append("\" ARROW ARC ")
                .append(Escape.eP(a)).append(
                    Escape.eP(aa.getCarbonylCarbonAtom())).append(
                    Escape.eP(aa.getNitrogenAtom())).append(
                    Escape.eP(pt)).append(" \"psi = ").append(
                    String.valueOf(Math.round(y))).append("\" color ").append(
                    qColor[1]).append("\n");
            pdbATOM.append("draw ID \"planeNCC").append(id).append("\" ")
                .append(Escape.eP(aa.getNitrogenAtom())).append(
                    Escape.eP(a)).append(
                    Escape.eP(aa.getCarbonylCarbonAtom())).append(
                    " color ").append(qColor[0]).append("\n");
            pdbATOM.append("draw ID \"planeCNC").append(id).append("\" ")
                .append(
                    Escape.eP(((AminoMonomer) p.monomers[m - 1])
                        .getCarbonylCarbonAtom())).append(
                    Escape.eP(aa.getNitrogenAtom())).append(
                    Escape.eP(a)).append(" color ").append(qColor[1])
                .append("\n");
            pdbATOM.append("draw ID \"planeCCN").append(id).append("\" ")
                .append(Escape.eP(a)).append(
                    Escape.eP(aa.getCarbonylCarbonAtom())).append(
                    Escape.eP(((AminoMonomer) p.monomers[m + 1])
                        .getNitrogenAtom())).append(" color ")
                .append(qColor[2]).append("\n");
            continue;
          }
          if (Float.isNaN(angledeg)) {
            strExtra = "";
            if (writeRamachandranStraightness)
              continue;
          } else {
            q = Quaternion.newVA(P3.new3(1, 0, 0), angledeg);
            strExtra = q.getInfo();
            if (writeRamachandranStraightness) {
              z = angledeg;
              w = straightness;
            } else {
              w = a.getPartialCharge();
            }

          }
        } else {
          // quaternion
          q = monomer.getQuaternion(qtype);
          if (q != null) {
            q.setRef(qref);
            qref = Quaternion.newQ(q);
          }
          if (derivType == 2)
            monomer.setGroupParameter(T.straightness, Float.NaN);
          if (q == null) {
            qprev = null;
            qref = null;
          } else if (derivType > 0) {
            Atom anext = a;
            Quaternion qnext = q;
            if (qprev == null) {
              q = null;
              dqprev = null;
            } else {

              // we now have two quaternionions, q and qprev
              // the revised procedure assigns dq for these to group a, not aprev
              // a/anext: q
              // aprev: qprev

              // back up to previous frame pointer - no longer
              //a = aprev;
              //q = qprev;
              //monomer = (Monomer) a.getGroup();
              // get dq or dq* for PREVIOUS atom
              if (isRelativeAlias) {
                // ctype = 'r';
                // dq*[i] = q[i-1] \ q[i]
                // R(v) = q[i-1] \ q(i) * (0, v) * q[i] \ q[i-1]
                // used for aligning all standard amino acids along X axis
                // in the second derivative and in an ellipse in the first
                // derivative
                //PRE 11.7.47:
                // dq*[i] = q[i] \ q[i+1]
                // R(v) = q[i] \ q(i+1) * (0, v) * q[i+1] \ q[i]
                // used for aligning all standard amino acids along X axis
                // in the second derivative and in an ellipse in the first
                // derivative
                dq = qprev.leftDifference(q);// qprev.inv().mul(q) = qprev \ q
              } else {
                // ctype = 'a' or 'w' or 's'

                // OLD:
                // the standard "absolute" difference dq
                // dq[i] = q[i+1] / q[i]
                // R(v) = q[i+1] / q[i] * (0, v) * q[i] / q[i+1]
                // used for definition of the local helical axis

                // NEW:
                // the standard "absolute" difference dq
                // dq[i] = q[i] / q[i-1]
                // R(v) = q[i] / q[i-1] * (0, v) * q[i-1] / q[i]
                // used for definition of the local helical axis

                dq = q.rightDifference(qprev);// q.mul(qprev.inv());
              }
              if (derivType == 1) {
                // first deriv:
                q = dq;
              } else if (dqprev == null) {
                q = null;
              } else {
                /*
                 * standard second deriv.
                 * 
                 * OLD:
                 * 
                 * ddq[i] =defined= (q[i+1] \/ q[i]) / (q[i] \/ q[i-1])
                 * 
                 * Relative to the previous atom as "i" (which is now "a"), we
                 * have:
                 * 
                 * dqprev = q[i] \/ q[i-1] dq = q[i+1] \/ q[i]
                 * 
                 * and so
                 * 
                 * ddq[i] = dq / dqprev
                 * 

                 * NEW: dq = q[i] \/ q[i-1]    dqprev = q[i-1] \/ q[i-2]
                 * 
                 * and so
                 * 
                 * ddq[iprev] = dq / dqprev
                 * 
                 * 
                 * 
                 * Looks odd, perhaps, because it is written "dq[i] / dq[i-1]"
                 * but this is correct; we are assigning ddq to the correct
                 * atom.
                 * 
                 * 5.8.2009 -- bh -- changing quaternion straightness to be
                 * assigned to PREVIOUS aa
                 * 
                 */
                q = dq.rightDifference(dqprev); // q = dq.mul(dqprev.inv());
                val1 = getQuaternionStraightness(id, dqprev, dq);
                val2 = get3DStraightness(id, dqprev, dq);
                aprev.getGroup().setGroupParameter(T.straightness,
                    useQuaternionStraightness ? val1 : val2);
              }
              dqprev = dq;
            }
            aprev = anext; //(a)
            qprev = qnext; //(q)
          }
          if (q == null) {
            atomLast = null;
            continue;
          }
          switch (ctype) {
          default:
            x = q.q1;
            y = q.q2;
            z = q.q3;
            w = q.q0;
            break;
          case 'x':
            x = q.q0;
            y = q.q1;
            z = q.q2;
            w = q.q3;
            break;
          case 'y':
            x = q.q3;
            y = q.q0;
            z = q.q1;
            w = q.q2;
            break;
          case 'z':
            x = q.q2;
            y = q.q3;
            z = q.q0;
            w = q.q1;
            break;
          }
          P3 ptCenter = monomer.getQuaternionFrameCenter(qtype);
          if (ptCenter == null)
            ptCenter = new P3();
          if (isDraw) {
            if (bsSelected != null && !bsSelected.get(a.getIndex()))
              continue;
            int deg = (int) Math.floor(Math.acos(w) * 360 / Math.PI);
            if (derivType == 0) {
              pdbATOM.append(q.draw(prefix, id, ptCenter, 1f));
              if (qtype == 'n' && isAmino) {
                P3 ptH = ((AminoMonomer) monomer)
                    .getNitrogenHydrogenPoint();
                if (ptH != null)
                  pdbATOM.append("draw ID \"").append(prefix).append("nh")
                      .append(id).append("\" width 0.1 ")
                      .append(Escape.eP(ptH)).append("\n");
              }
            }
            if (derivType == 1) {
              pdbATOM.append(
                  (String) monomer.getHelixData(T.draw, qtype, mStep))
                  .append("\n");
              continue;
            }
            pt.set(x * 2, y * 2, z * 2);
            pdbATOM.append("draw ID \"").append(prefix).append("a").append(id)
                .append("\" VECTOR ").append(
                    Escape.eP(ptCenter)).append(Escape.eP(pt))
                .append(" \">").append(String.valueOf(deg)).append("\" color ")
                .append(qColor[derivType]).append("\n");
            continue;
          }
          strExtra = q.getInfo()
              + TextFormat.sprintf("  %10.5p %10.5p %10.5p",
                  "p", new Object[] { ptCenter });
          if (qtype == 'n' && isAmino) {
            strExtra += TextFormat.sprintf("  %10.5p %10.5p %10.5p",
                "p", new Object[] { ((AminoMonomer) monomer)
                    .getNitrogenHydrogenPoint() });
          } else if (derivType == 2 && !Float.isNaN(val1)) {
            strExtra += TextFormat.sprintf(" %10.5f %10.5f",
                "F", new Object[] { new float[] { val1, val2 } });
          }
        }
        if (pdbATOM == null)// || bsSelected != null && !bsSelected.get(a.getIndex()))
          continue;
        bsWritten.set(((Monomer) a.getGroup()).leadAtomIndex);
        pdbATOM.append(LabelToken.formatLabelAtomArray(viewer, a, tokens, '\0',
            null));
        pdbATOM.append(TextFormat
            .sprintf("%8.2f%8.2f%8.2f      %6.3f          %2s    %s\n",
                "ssF", new Object[] {
                    a.getElementSymbolIso(false).toUpperCase(),
                    strExtra,
                    new float[] { x * factor, y * factor, z * factor,
                        w * factor } }));
        if (atomLast != null
            && atomLast.getPolymerIndexInModel() == a.getPolymerIndexInModel()) {
          pdbCONECT.append("CONECT").append(
              TextFormat.formatStringI("%5i", "i", atomLast.getAtomNumber()))
              .append(TextFormat.formatStringI("%5i", "i", a.getAtomNumber()))
              .appendC('\n');
        }
        atomLast = a;
      }
    }
  }

  /**
   * 
   * @param m
   * @param qtype
   * @return calculated value
   */
  protected float calculateRamachandranHelixAngle(int m, char qtype) {
    return Float.NaN;
  }

  // starting with Jmol 11.7.47, dq is defined so as to LEAD TO
  // the target atom, not LEAD FROM it. 
  /**
   * @param id
   *        for debugging only
   * @param dq
   * @param dqnext
   * @return calculated straightness
   * 
   */
  private static float get3DStraightness(String id, Quaternion dq,
                                         Quaternion dqnext) {
    // 
    // Normal-only simple dot-product straightness = dq1.normal.DOT.dq2.normal
    //
    return dq.getNormal().dot(dqnext.getNormal());
  }

  /**
   * 
   * @param id
   *        for debugging only
   * @param dq
   * @param dqnext
   * @return straightness
   */
  private static float getQuaternionStraightness(String id, Quaternion dq,
                                                 Quaternion dqnext) {
    // 
    // Dan Kohler's quaternion straightness = 1 - acos(|dq1.dq2|)/(PI/2)
    //
    // alignment = near 0 or near 180 --> same - just different rotations.
    // It's a 90-degree change in direction that corresponds to 0.
    //
    return getStraightness(dq.dot(dqnext));
  }

  private static float getStraightness(float cosHalfTheta) {
    return (float) (1 - 2 * Math.acos(Math.abs(cosHalfTheta)) / Math.PI);
  }

  public boolean isDna() {
    return (monomerCount > 0 && monomers[0].isDna());
  }

  public boolean isRna() {
    return (monomerCount > 0 && monomers[0].isRna());
  }

  public void getRangeGroups(int nResidues, BS bsAtoms, BS bsResult) {
    BS bsTemp = new BS();
    for (int i = 0; i < monomerCount; i++) {
      if (!monomers[i].isSelected(bsAtoms))
        continue;
      bsTemp.setBits(Math.max(0, i - nResidues), i + nResidues + 1);
      i += nResidues - 1;
    }
    for (int i = bsTemp.nextSetBit(0); i >= 0 && i < monomerCount; i = bsTemp
        .nextSetBit(i + 1))
      monomers[i].selectAtoms(bsResult);
  }

  public String calculateDssp(BioPolymer[] bioPolymers,
                                    int bioPolymerCount, JmolList<Bond> vHBonds,
                                    boolean doReport,
                                    boolean dsspIgnoreHydrogens,
                                    boolean setStructure) {
    // Here because we are calling a static method in AminoPolymer for the 
    // entire SET of polymers, just using the first one, which may or may not
    // be an AminoPolymer.
    return AminoPolymer.calculateStructuresDssp(bioPolymers, bioPolymerCount,
        vHBonds, doReport, dsspIgnoreHydrogens, setStructure);
  }

  /**
   * 
   * @param type
   * @param structureID
   * @param serialID
   * @param strandCount
   * @param startChainID
   * @param startSeqcode
   * @param endChainID
   * @param endSeqcode
   */
  public void addStructure(EnumStructure type, String structureID,
                                    int serialID, int strandCount,
                                    char startChainID, int startSeqcode,
                                    char endChainID, int endSeqcode) {
    // overridden by each subclass
  }

  /**
   * @param alphaOnly
   */
  public void calculateStructures(boolean alphaOnly) {
  }

  /**
   * 
   * @param polymer
   * @param bsA
   * @param bsB
   * @param vHBonds
   * @param nMaxPerResidue
   * @param min
   * @param checkDistances
   * @param dsspIgnoreHydrogens
   */
  public void calcRasmolHydrogenBonds(BioPolymer polymer, BS bsA,
                                      BS bsB, JmolList<Bond> vHBonds,
                                      int nMaxPerResidue, int[][][] min,
                                      boolean checkDistances,
                                      boolean dsspIgnoreHydrogens) {
    // subclasses should override if they know how to calculate hbonds
  }

  /**
   * @param structureList
   *        protein only -- helix, sheet, turn definitions
   */
  public void setStructureList(Map<EnumStructure, float[]> structureList) {
  }

  /**
   * 
   * @param viewer
   * @param ctype
   * @param qtype
   * @param mStep
   * @param derivType
   * @param bsAtoms
   * @param bsSelected
   * @param bothEnds
   * @param isDraw
   * @param addHeader
   * @param tokens
   * @param pdbATOM
   * @param pdbCONECT
   * @param bsWritten
   */
  public void getPdbData(Viewer viewer, char ctype, char qtype, int mStep,
                         int derivType, BS bsAtoms, BS bsSelected,
                         boolean bothEnds, boolean isDraw, boolean addHeader,
                         LabelToken[] tokens, OutputStringBuilder pdbATOM,
                         SB pdbCONECT, BS bsWritten) {
    return;
  }

  public int getType() {
    return type;
  }

  /**
   * 
   * @param modelSet
   * @param bs1
   * @param bs2
   * @param vCA
   * @param thresh
   * @param delta
   * @param allowMultiple
   * @return List [ {atom1, atom2}, {atom1, atom2}...]
   */
  public JmolList<Atom[]> calculateStruts(ModelSet modelSet, BS bs1,
                                      BS bs2, JmolList<Atom> vCA, float thresh,
                                      int delta, boolean allowMultiple) {
    return null;
  }

}
