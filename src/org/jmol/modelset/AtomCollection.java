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
import java.util.Arrays;
import java.util.Comparator;



import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.bspt.Bspf;
import org.jmol.constant.EnumPalette;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumVdw;
import org.jmol.geodesic.EnvelopeCalculation;
import org.jmol.util.ArrayUtil;
import org.jmol.util.AxisAngle4f;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Elements;
import org.jmol.util.GData;
import org.jmol.util.Matrix3f;
import org.jmol.util.P3;
import org.jmol.util.Point4f;
import org.jmol.util.Quadric;
import org.jmol.util.Escape;
import org.jmol.util.JmolEdge;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.Rectangle;
import org.jmol.util.V3;

import org.jmol.util.Measure;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JC;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;

abstract public class AtomCollection {
  
  private static final Float MINUSZERO = Float.valueOf(-0.0f);

  protected void releaseModelSet() {
    atoms = null;
    viewer = null;
    g3d = null;
    bspf = null;
    surfaceDistance100s = null;
    bsSurface = null;
    tainted = null;

    atomNames = null;
    atomTypes = null;
    atomSerials = null;
    vibrationVectors = null;
    occupancies = null;
    bfactor100s = null;
    partialCharges = null;
    ionicRadii = null;
    ellipsoids = null;

  }

  protected void mergeAtomArrays(AtomCollection mergeModelSet) {
    tainted = mergeModelSet.tainted;
    atomNames = mergeModelSet.atomNames;
    atomTypes = mergeModelSet.atomTypes;
    atomSerials = mergeModelSet.atomSerials;
    vibrationVectors = mergeModelSet.vibrationVectors;
    occupancies = mergeModelSet.occupancies;
    bfactor100s = mergeModelSet.bfactor100s;
    ionicRadii = mergeModelSet.ionicRadii;
    partialCharges = mergeModelSet.partialCharges;
    ellipsoids = mergeModelSet.ellipsoids;
    setHaveStraightness(false);
    surfaceDistance100s = null;
  }
  
  public void setHaveStraightness(boolean TF) {
    haveStraightness = TF;
  }
  
  protected boolean getHaveStraightness() {
    return haveStraightness;
  }
  
  public Viewer viewer;
  protected GData g3d;

  public Atom[] atoms;
  public int atomCount;

  public JmolList<P3> getAtomPointVector(BS bs) {
    JmolList<P3> v = new  JmolList<P3>();
    if (bs != null) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
        v.addLast(atoms[i]);
      }
    }
    return v;
  }

  public int getAtomCount() {
    // not established until AFTER model loading
    return atomCount;
  }
  
  ////////////////////////////////////////////////////////////////
  // these may or may not be allocated
  // depending upon the AtomSetCollection characteristics
  //
  // used by Atom:
  //
  String[] atomNames;
  String[] atomTypes;
  int[] atomSerials;
  public V3[] vibrationVectors;
  byte[] occupancies;
  short[] bfactor100s;
  float[] partialCharges;
  float[] ionicRadii;
  float[] hydrophobicities;
  
  protected Quadric[][] ellipsoids;
  protected int[] surfaceDistance100s;

  protected boolean haveStraightness;

  public boolean modelSetHasVibrationVectors(){
    return (vibrationVectors != null);
  }
  
  public String[] getAtomTypes() {
    return atomTypes;
  }

  
  public float[] getPartialCharges() {
    return partialCharges;
  }

  public float[] getIonicRadii() {
    return ionicRadii;
  }
  
  public short[] getBFactors() {
    return bfactor100s;
  }

  public float[] getHydrophobicity() {
    return hydrophobicities;
  }
  

  private BS bsHidden = new BS();

  public void setBsHidden(BS bs) { //from selection manager
    bsHidden = bs;
  }

  public boolean isAtomHidden(int iAtom) {
    return bsHidden.get(iAtom);
  }
  
  //////////// atoms //////////////
  
  public String getAtomInfo(int i, String format) {
    return (format == null ? atoms[i].getInfo() : LabelToken.formatLabel(viewer, atoms[i],format));
  }

  public String getAtomInfoXYZ(int i, boolean useChimeFormat) {
    return atoms[i].getInfoXYZ(useChimeFormat);
  }

  public String getElementSymbol(int i) {
    return atoms[i].getElementSymbol();
  }

  public int getElementNumber(int i) {
    return atoms[i].getElementNumber();
  }

  public String getElementName(int i) {
      return Elements.elementNameFromNumber(atoms[i]
          .getAtomicAndIsotopeNumber());
  }

  public String getAtomName(int i) {
    return atoms[i].getAtomName();
  }

  public int getAtomNumber(int i) {
    return atoms[i].getAtomNumber();
  }

  public P3 getAtomPoint3f(int i) {
    return atoms[i];
  }

  public float getAtomRadius(int i) {
    return atoms[i].getRadius();
  }

  public float getAtomVdwRadius(int i, EnumVdw type) {
    return atoms[i].getVanderwaalsRadiusFloat(viewer, type);
  }

  public short getAtomColix(int i) {
    return atoms[i].getColix();
  }

  public String getAtomChain(int i) {
    return "" + atoms[i].getChainID();
  }

  public Quadric[] getEllipsoid(int i) {
    return (i < 0 || ellipsoids == null || i >= ellipsoids.length ? null
        : ellipsoids[i]);
  }

  public Quaternion getQuaternion(int i, char qtype) {
    return (i < 0 ? null : atoms[i].group.getQuaternion(qtype));
  } 

  public Object getHelixData(BS bs, int tokType) {
    int iAtom = bs.nextSetBit(0);
    return (iAtom < 0 ? "null"
        : atoms[iAtom].group.getHelixData(tokType, 
        viewer.getQuaternionFrame(), viewer.getHelixStep()));
  }
  
  public int getAtomIndexFromAtomNumber(int atomNumber, BS bsVisibleFrames) {
    //definitely want FIRST (model) not last here
    for (int i = 0; i < atomCount; i++) {
      Atom atom = atoms[i];
      if (atom.getAtomNumber() == atomNumber && bsVisibleFrames.get(atom.modelIndex))
        return i;
    }
    return -1;
  }

  public void setFormalCharges(BS bs, int formalCharge) {
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        atoms[i].setFormalCharge(formalCharge);
        taintAtom(i, TAINT_FORMALCHARGE);
      }
  }
  
  public float[] getAtomicCharges() {
    float[] charges = new float[atomCount];
    for (int i = atomCount; --i >= 0; )
      charges[i] = atoms[i].getElementNumber();
    return charges;
  }

  protected float getRadiusVdwJmol(Atom atom) {
    return Elements.getVanderwaalsMar(atom.getElementNumber(),
        EnumVdw.JMOL) / 1000f;
  }
  
  // the maximum BondingRadius seen in this set of atoms
  // used in autobonding
  protected float maxBondingRadius = Float.MIN_VALUE;
  private float maxVanderwaalsRadius = Float.MIN_VALUE;

  public float getMaxVanderwaalsRadius() {
    //Dots
    if (maxVanderwaalsRadius == Float.MIN_VALUE)
      findMaxRadii();
    return maxVanderwaalsRadius;
  }

  protected void findMaxRadii() {
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float bondingRadius = atom.getBondingRadiusFloat();
      if (bondingRadius > maxBondingRadius)
        maxBondingRadius = bondingRadius;
      float vdwRadius = atom.getVanderwaalsRadiusFloat(viewer, EnumVdw.AUTO);
      if (vdwRadius > maxVanderwaalsRadius)
        maxVanderwaalsRadius = vdwRadius;
    }
  }

  private boolean hasBfactorRange;
  private int bfactor100Lo;
  private int bfactor100Hi;

  public void clearBfactorRange() {
    hasBfactorRange = false;
  }

  private void calcBfactorRange(BS bs) {
    if (hasBfactorRange)
      return;
    bfactor100Lo = Integer.MAX_VALUE;
    bfactor100Hi = Integer.MIN_VALUE;
    if (bs == null) {
      for (int i = 0; i < atomCount; i++)
        setBf(i);
    } else {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
        setBf(i);
    }
    hasBfactorRange = true;
  }

  private void setBf(int i) {
    int bf = atoms[i].getBfactor100();
    if (bf < bfactor100Lo)
      bfactor100Lo = bf;
    else if (bf > bfactor100Hi)
      bfactor100Hi = bf;    
  }
  
  public int getBfactor100Lo() {
    //ColorManager
    if (!hasBfactorRange) {
      if (viewer.isRangeSelected()) {
        calcBfactorRange(viewer.getSelectionSet(false));
      } else {
        calcBfactorRange(null);
      }
    }
    return bfactor100Lo;
  }

  public int getBfactor100Hi() {
    //ColorManager
    getBfactor100Lo();
    return bfactor100Hi;
  }

  private int surfaceDistanceMax;

  public int getSurfaceDistanceMax() {
    //ColorManager, Eval
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistanceMax;
  }

  public float calculateVolume(BS bs, EnumVdw vType) {
    // Eval
    float volume = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        volume += atoms[i].getVolume(viewer, vType);
    return volume;
  }
  
  private BS bsSurface;
  private int nSurfaceAtoms;

  int getSurfaceDistance100(int atomIndex) {
    //atom
    if (nSurfaceAtoms == 0)
      return -1;
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistance100s[atomIndex];
  }

  private void calcSurfaceDistances() {
    calculateSurface(null, -1);
  }
  
  public P3[] calculateSurface(BS bsSelected, float envelopeRadius) {
    if (envelopeRadius < 0)
      envelopeRadius = EnvelopeCalculation.SURFACE_DISTANCE_FOR_CALCULATION;
    EnvelopeCalculation ec = new EnvelopeCalculation(viewer, atomCount, null);
    ec.calculate(new RadiusData(null, envelopeRadius, EnumType.ABSOLUTE, null), 
        Float.MAX_VALUE, 
        bsSelected, BSUtil.copyInvert(bsSelected, atomCount), 
        false, false, false, true);
    P3[] points = ec.getPoints();
    surfaceDistanceMax = 0;
    bsSurface = ec.getBsSurfaceClone();
    surfaceDistance100s = new int[atomCount];
    nSurfaceAtoms = BSUtil.cardinalityOf(bsSurface);
    if (nSurfaceAtoms == 0 || points == null || points.length == 0)
      return points;
    float radiusAdjust = (envelopeRadius == Float.MAX_VALUE ? 0 : envelopeRadius);
    for (int i = 0; i < atomCount; i++) {
      //surfaceDistance100s[i] = Integer.MIN_VALUE;
      if (bsSurface.get(i)) {
        surfaceDistance100s[i] = 0;
      } else {
        float dMin = Float.MAX_VALUE;
        Atom atom = atoms[i];
        for (int j = points.length; --j >= 0;) {
          float d = Math.abs(points[j].distance(atom) - radiusAdjust);
          if (d < 0 && Logger.debugging)
            Logger.debug("draw d" + j + " " + Escape.eP(points[j])
                + " \"" + d + " ? " + atom.getInfo() + "\"");
          dMin = Math.min(d, dMin);
        }
        int d = surfaceDistance100s[i] = (int) Math.floor(dMin * 100);
        surfaceDistanceMax = Math.max(surfaceDistanceMax, d);
      }
    }
    return points;
  }

  @SuppressWarnings("unchecked")
  protected void setAtomCoord2(BS bs, int tokType, Object xyzValues) {
    P3 xyz = null;
    P3[] values = null;
    JmolList<P3> v = null;
    int type = 0;
    int nValues = 1;
    if (xyzValues instanceof P3) {
      xyz = (P3) xyzValues;
    } else if (xyzValues instanceof JmolList<?>) {
      v = (JmolList<P3>) xyzValues;
      if ((nValues = v.size()) == 0)
        return;
      type = 1;
    } else if (Escape.isAP(xyzValues)){
      values = (P3[]) xyzValues;
      if ((nValues = values.length) == 0)
        return;
      type = 2;
    } else {
      return;
    }
    int n = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) { 
        switch (type) {
        case 1:
          if (n >= nValues)
            return;
          xyz = v.get(n++);
          break;
        case 2:
          if (n >= nValues)
            return;
          xyz = values[n++];
          break;
        }
        switch (tokType) {
        case T.xyz:
          setAtomCoord(i, xyz.x, xyz.y, xyz.z);
          break;
        case T.fracxyz:
          atoms[i].setFractionalCoordTo(xyz, true);
          taintAtom(i, TAINT_COORD);
          break;
        case T.fuxyz:
          atoms[i].setFractionalCoordTo(xyz, false);
          taintAtom(i, TAINT_COORD);
          break;
        case T.vibxyz:
          setAtomVibrationVector(i, xyz.x, xyz.y, xyz.z);
          break;
        }
      }
  }

  private void setAtomVibrationVector(int atomIndex, float x, float y, float z) {
    setVibrationVector(atomIndex, x, y, z);  
    taintAtom(atomIndex, TAINT_VIBRATION);
  }
  
  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    atoms[atomIndex].x = x;
    atoms[atomIndex].y = y;
    atoms[atomIndex].z = z;
    taintAtom(atomIndex, TAINT_COORD);
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    atoms[atomIndex].x += x;
    atoms[atomIndex].y += y;
    atoms[atomIndex].z += z;
    taintAtom(atomIndex, TAINT_COORD);
  }

  protected void setAtomsCoordRelative(BS bs, float x, float y,
                                      float z) {
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
        setAtomCoordRelative(i, x, y, z);
  }

  protected void setAPa(BS bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    int n = 0;

    if (values != null && values.length == 0 || bs == null)
      return;
    boolean isAll = (values != null && values.length == atomCount 
        || list != null && list.length == atomCount);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (isAll)
        n = i;
      if (values != null) {
        if (n >= values.length)
          return;
        fValue = values[n++];
        iValue = (int) fValue;
      } else if (list != null) {
        if (n >= list.length)
          return;
        sValue = list[n++];
      }
      Atom atom = atoms[i];
      switch (tok) {
      case T.atomname:
        taintAtom(i, TAINT_ATOMNAME);
        setAtomName(i, sValue);
        break;
      case T.atomno:
        taintAtom(i, TAINT_ATOMNO);
        setAtomNumber(i, iValue);
        break;
      case T.atomtype:
        taintAtom(i, TAINT_ATOMTYPE);
        setAtomType(i, sValue);
        break;
      case T.atomx:
      case T.x:
        setAtomCoord(i, fValue, atom.y, atom.z);
        break;
      case T.atomy:
      case T.y:
        setAtomCoord(i, atom.x, fValue, atom.z);
        break;
      case T.atomz:
      case T.z:
        setAtomCoord(i, atom.x, atom.y, fValue);
        break;
      case T.vibx:
      case T.viby:
      case T.vibz:
        setVibrationVector(i, tok, fValue);
        break;
      case T.fracx:
      case T.fracy:
      case T.fracz:
        atom.setFractionalCoord(tok, fValue, true);
        taintAtom(i, TAINT_COORD);
        break;
      case T.fux:
      case T.fuy:
      case T.fuz:
        atom.setFractionalCoord(tok, fValue, false);
        taintAtom(i, TAINT_COORD);
        break;
      case T.elemno:
      case T.element:
        setElement(atom, iValue);
        break;
      case T.formalcharge:
        atom.setFormalCharge(iValue);
        taintAtom(i, TAINT_FORMALCHARGE);
        break;
      case T.hydrophobic:
        if (setHydrophobicity(i, fValue))
          taintAtom(i, TAINT_HYDROPHOBICITY);
        break;
      case T.label:
      case T.format:
        viewer.setAtomLabel(sValue, i);
        break;
      case T.occupancy:
        if (iValue < 2)
          iValue = (int) Math.floor(100 * fValue);
        if (setOccupancy(i, iValue))
          taintAtom(i, TAINT_OCCUPANCY);
        break;
      case T.partialcharge:
        if (setPartialCharge(i, fValue))
          taintAtom(i, TAINT_PARTIALCHARGE);
        break;
      case T.ionic:
        if (setIonicRadius(i, fValue))
          taintAtom(i, TAINT_IONICRADIUS);
        break;
      case T.radius:
      case T.spacefill:
        if (fValue < 0)
          fValue = 0;
        else if (fValue > Atom.RADIUS_MAX)
          fValue = Atom.RADIUS_MAX;
        atom.madAtom = ((short) (fValue * 2000));
        break;
      case T.selected:
        viewer.setSelectedAtom(atom.index, (fValue != 0));
        break;
      case T.temperature:
        if (setBFactor(i, fValue))
          taintAtom(i, TAINT_TEMPERATURE);
        break;
      case T.valence:
        atom.setValence(iValue);
        taintAtom(i, TAINT_VALENCE);
        break;
      case T.vanderwaals:
        if (atom.setRadius(fValue))
          taintAtom(i, TAINT_VANDERWAALS);
        else
          untaint(i, TAINT_VANDERWAALS);
        break;
      default:
        Logger.error("unsettable atom property: " + T.nameOf(tok));
        break;
      }
    }
    if (tok == T.selected)
      viewer.setSelectedAtom(-1, false);
  }

  protected void setElement(Atom atom, int atomicNumber) {
    taintAtom(atom.index, TAINT_ELEMENT);
    atom.setAtomicAndIsotopeNumber(atomicNumber);
    atom.setPaletteID(EnumPalette.CPK.id);
    atom.setColixAtom(viewer.getColixAtomPalette(atom,
        EnumPalette.CPK.id));
  }

  public float getVibrationCoord(int atomIndex, char c) {
    if (vibrationVectors == null || vibrationVectors[atomIndex] == null)
      return 0;
    switch (c) {
    case 'X':
      return vibrationVectors[atomIndex].x;
    case 'Y':
      return vibrationVectors[atomIndex].y;
    default:
      return vibrationVectors[atomIndex].z;
    }
  }

  public V3 getVibrationVector(int atomIndex, boolean forceNew) {
    V3 v = (vibrationVectors == null ? null : vibrationVectors[atomIndex]);
    return (v == null && forceNew ? new V3() : v);
  }

  protected void setVibrationVector(int atomIndex, float x, float y, float z) {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
      return;
    if (vibrationVectors == null || vibrationVectors.length < atomIndex)
      vibrationVectors = new V3[atoms.length];
    if (vibrationVectors[atomIndex] == null)
      vibrationVectors[atomIndex] = V3.new3(x, y, z);
    else
      vibrationVectors[atomIndex].set(x, y, z);
    atoms[atomIndex].setVibrationVector();
  }

  private void setVibrationVector(int atomIndex, int tok, float fValue) {
    V3 v = getVibrationVector(atomIndex, true);
    if (v == null)
      v = new V3();
    switch(tok) {
    case T.vibx:
      v.x = fValue;
      break;
    case T.viby:
      v.y = fValue;
      break;
    case T.vibz:
      v.z = fValue;
      break;
    }
    setAtomVibrationVector(atomIndex, v.x, v.y, v.z);
  }

  public void setAtomName(int atomIndex, String name) {
    byte id = JC.lookupSpecialAtomID(name);
    atoms[atomIndex].atomID = id;
    if (id > 0 && ((ModelCollection)this).models[atoms[atomIndex].modelIndex].isBioModel)
      return;
    if (atomNames == null)
      atomNames = new String[atoms.length];
    atomNames[atomIndex] = name;
  }

  protected void setAtomType(int atomIndex, String type) {
      if (atomTypes == null)
        atomTypes = new String[atoms.length];
      atomTypes[atomIndex] = type;
  }
  
  public boolean setAtomNumber(int atomIndex, int atomno) {
    if (atomSerials == null) {
      atomSerials = new int[atoms.length];
    }
    atomSerials[atomIndex] = atomno;
    return true;
  }
  
  protected boolean setOccupancy(int atomIndex, int occupancy) {
    if (occupancies == null) {
      if (occupancy == 100)
        return false; // 100 is the default;
      occupancies = new byte[atoms.length];
      for (int i = atoms.length; --i >= 0;)
        occupancies[i] = 100;
    }
    occupancies[atomIndex] = (byte) (occupancy > 255 ? 255 : occupancy < 0 ? 0 : occupancy);
    return true;
  }
  
  protected boolean setPartialCharge(int atomIndex, float partialCharge) {
    if (Float.isNaN(partialCharge))
      return false;
    if (partialCharges == null) {
      if (partialCharge == 0 && !Float.valueOf(partialCharge).equals(MINUSZERO))
        return false; // no need to store a 0.
      partialCharges = new float[atoms.length];
    }
    partialCharges[atomIndex] = partialCharge;
    return true;
  }

  protected boolean setIonicRadius(int atomIndex, float radius) {
    if (Float.isNaN(radius))
      return false;
    if (ionicRadii == null) {
      ionicRadii = new float[atoms.length];
    }
    ionicRadii[atomIndex] = radius;
    return true;
  }

  protected boolean setBFactor(int atomIndex, float bfactor) {
    if (Float.isNaN(bfactor))
      return false;
    if (bfactor100s == null) {
      if (bfactor == 0 && bfactor100s == null) // there's no need to store a 0.
        return false;
      bfactor100s = new short[atoms.length];
    }
    bfactor100s[atomIndex] = (short) ((bfactor < -327.68f ? -327.68f
        : bfactor > 327.67 ? 327.67 : bfactor) * 100 + (bfactor < 0 ? -0.5 : 0.5));
    return true;
  }

  protected boolean setHydrophobicity(int atomIndex, float value) {
    if (Float.isNaN(value))
      return false;
    if (hydrophobicities == null) {
      hydrophobicities = new float[atoms.length];
      for (int i = 0; i < atoms.length; i++)
        hydrophobicities[i] = Elements.getHydrophobicity(atoms[i].getGroupID());
    }
    hydrophobicities[atomIndex] = value;
    return true;
  }

  protected void setEllipsoid(int atomIndex, Quadric[] ellipsoid) {
    if (ellipsoid == null)
      return;
    if (ellipsoids == null)
      ellipsoids = new Quadric[atoms.length][];
    ellipsoids[atomIndex] = ellipsoid;
  }

  // loading data
  
  public void setAtomData(int type, String name, String dataString, boolean isDefault) {
    float[] fData = null;
    BS bs = null;
    switch (type) {
    case TAINT_COORD:
      loadCoordinates(dataString, false, !isDefault);
      return;
    case TAINT_VIBRATION:
      loadCoordinates(dataString, true, true);
      return;
    case TAINT_MAX:
      fData = new float[atomCount];
      bs = BSUtil.newBitSet(atomCount);
      break;
    }
    int[] lines = Parser.markLines(dataString, ';');
    int n = 0;
    try {
      int nData = Parser.parseInt(dataString.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = Parser.getTokens(Parser.parseTrimmed(dataString.substring(
            lines[i], lines[i + 1] - 1)));
        int atomIndex = Parser.parseInt(tokens[0]) - 1;
        if (atomIndex < 0 || atomIndex >= atomCount)
          continue;
        Atom atom = atoms[atomIndex];
        n++;
        int pt = tokens.length - 1;
        float x = Parser.parseFloatStr(tokens[pt]);
        switch (type) {
        case TAINT_MAX:
          fData[atomIndex] = x;
          bs.set(atomIndex);
          continue;
        case TAINT_ATOMNO:
          setAtomNumber(atomIndex, (int) x);
          break;
        case TAINT_ATOMNAME:
          setAtomName(atomIndex, tokens[pt]);
          break;
        case TAINT_ATOMTYPE:
          setAtomType(atomIndex, tokens[pt]);
          break;
        case TAINT_ELEMENT:
          atom.setAtomicAndIsotopeNumber((int)x);
          atom.setPaletteID(EnumPalette.CPK.id);
          atom.setColixAtom(viewer.getColixAtomPalette(atom, EnumPalette.CPK.id));
          break;
        case TAINT_FORMALCHARGE:
          atom.setFormalCharge((int)x);          
          break;
        case TAINT_HYDROPHOBICITY:
          setHydrophobicity(atomIndex, x);
          break;
        case TAINT_IONICRADIUS:
          setIonicRadius(atomIndex, x);          
          break;
        case TAINT_PARTIALCHARGE:
          setPartialCharge(atomIndex, x);          
          break;
        case TAINT_TEMPERATURE:
          setBFactor(atomIndex, x);
          break;
        case TAINT_VALENCE:
          atom.setValence((int)x);     
          break;
        case TAINT_VANDERWAALS:
          atom.setRadius(x);          
          break;
        }
        taintAtom(atomIndex, (byte) type);
      }
      if (type == TAINT_MAX && n > 0)
        viewer.setData(name, new Object[] {name, fData, bs, Integer.valueOf(1)}, 0, 0, 0, 0, 0);
        
    } catch (Exception e) {
      Logger.error("AtomCollection.loadData error: " + e);
    }    
  }
  
  private void loadCoordinates(String data, boolean isVibrationVectors, boolean doTaint) {
    int[] lines = Parser.markLines(data, ';');
    try {
      int nData = Parser.parseInt(data.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = Parser.getTokens(Parser.parseTrimmed(data.substring(
            lines[i], lines[i + 1])));
        int atomIndex = Parser.parseInt(tokens[0]) - 1;
        float x = Parser.parseFloatStr(tokens[3]);
        float y = Parser.parseFloatStr(tokens[4]);
        float z = Parser.parseFloatStr(tokens[5]);
        if (isVibrationVectors) {
          setAtomVibrationVector(atomIndex, x, y, z);
        } else {
          setAtomCoord(atomIndex, x, y, z);
          if (!doTaint)
            untaint(atomIndex, TAINT_COORD);
        }
      }
    } catch (Exception e) {
      Logger.error("Frame.loadCoordinate error: " + e);
    }
  }


  // Binary Space Partitioning Forest
  
  protected Bspf bspf = null;

  void validateBspf(boolean isValid) {
    if (bspf != null)
      bspf.validate(isValid);
  }

  void validateBspfForModel(int modelIndex, boolean isValid) {
    if (bspf != null)
      bspf.validateModel(modelIndex, isValid);
  }

  // state tainting
  
  protected boolean preserveState = true;
  
  public void setPreserveState(boolean TF) {
    preserveState = TF;
  }
  ////  atom coordinate and property changing  //////////
  
  // be sure to add the name to the list below as well!
  final public static byte TAINT_ATOMNAME = 0;
  final public static byte TAINT_ATOMTYPE = 1;
  final public static byte TAINT_COORD = 2;
  final public static byte TAINT_ELEMENT = 3;
  final public static byte TAINT_FORMALCHARGE = 4;
  final public static byte TAINT_HYDROPHOBICITY = 5;
  final public static byte TAINT_IONICRADIUS = 6;
  final public static byte TAINT_OCCUPANCY = 7;
  final public static byte TAINT_PARTIALCHARGE = 8;
  final public static byte TAINT_TEMPERATURE = 9;
  final public static byte TAINT_VALENCE = 10;
  final public static byte TAINT_VANDERWAALS = 11;
  final public static byte TAINT_VIBRATION = 12;
  final public static byte TAINT_ATOMNO = 13;
  final public static byte TAINT_MAX = 14; // 1 more than last number, above
  
  public final static String[] userSettableValues = {
    "atomName",
    "atomType",
    "coord",
    "element",
    "formalCharge",
    "hydrophobicity",
    "ionic",
    "occupany",
    "partialCharge",
    "temperature",
    "valence",
    "vanderWaals",
    "vibrationVector",
    "atomNo"
  };
  
  static {
   if (userSettableValues.length != TAINT_MAX)
     Logger.error("AtomCollection.java userSettableValues is not length TAINT_MAX!");
  }
  
  public BS[] tainted;  // not final -- can be set to null
  public boolean canSkipLoad = true;

  public static int getUserSettableType(String dataType) {
    boolean isExplicit = (dataType.indexOf("property_") == 0);
    String check = (isExplicit ? dataType.substring(9) : dataType);
    for (int i = 0; i < TAINT_MAX; i++)
      if (userSettableValues[i].equalsIgnoreCase(check))
        return i;
    return (isExplicit ? TAINT_MAX : -1);
  }

  public BS getTaintedAtoms(byte type) {
    return tainted == null ? null : tainted[type];
  }
  
  public void taintAtoms(BS bsAtoms, byte type) {
    canSkipLoad = false;
    if (!preserveState)
      return;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      taintAtom(i, type);
  }

  protected void taintAtom(int atomIndex, byte type) {
    if (!preserveState)
      return;
    if (tainted == null)
      tainted = new BS[TAINT_MAX];
    if (tainted[type] == null)
      tainted[type] = BSUtil.newBitSet(atomCount);
    tainted[type].set(atomIndex);
    if (type  == TAINT_COORD)
      validateBspfForModel(atoms[atomIndex].modelIndex, false);
  }

  private void untaint(int atomIndex, byte type) {
    if (!preserveState)
      return;
    if (tainted == null || tainted[type] == null)
      return;
    tainted[type].clear(atomIndex);
  }

  public void setTaintedAtoms(BS bs, byte type) {
    if (!preserveState)
      return;
    if (bs == null) {
      if (tainted == null)
        return;
      tainted[type] = null;
      return;
    }
    if (tainted == null)
      tainted = new BS[TAINT_MAX];
    if (tainted[type] == null)
      tainted[type] = BSUtil.newBitSet(atomCount);
    BSUtil.copy2(bs, tainted[type]);
  }

  public void unTaintAtoms(BS bs, byte type) {
    if (tainted == null || tainted[type] == null)
      return;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
      tainted[type].clear(i);
    if (tainted[type].nextSetBit(0) < 0)
      tainted[type] = null;
  }

  ///////////////////////////////////////////

  /*
   * generalized; not just balls
   * 
   * This algorithm assumes that atoms are circles at the z-depth
   * of their center point. Therefore, it probably has some flaws
   * around the edges when dealing with intersecting spheres that
   * are at approximately the same z-depth.
   * But it is much easier to deal with than trying to actually
   * calculate which atom was clicked
   *
   * A more general algorithm of recording which object drew
   * which pixel would be very expensive and not worth the trouble
   */
  protected void findNearest2(int x, int y, Atom[] closest, BS bsNot) {
    Atom champion = null;
    int min = viewer.getMinPixelSelRadius();
    for (int i = atomCount; --i >= 0;) {
      if (bsNot != null && bsNot.get(i))
        continue;
      Atom contender = atoms[i];
      if (contender.isClickable()
          && isCursorOnTopOf(contender, x, y, min,
              champion))
        champion = contender;
    }
    closest[0] = champion;
  }

  /**
   * used by Frame and AminoMonomer and NucleicMonomer -- does NOT check for clickability
   * @param contender
   * @param x
   * @param y
   * @param radius
   * @param champion
   * @return true if user is pointing to this atom
   */
  boolean isCursorOnTopOf(Atom contender, int x, int y, int radius,
                          Atom champion) {
    return contender.screenZ > 1 && !g3d.isClippedZ(contender.screenZ)
        && g3d.isInDisplayRange(contender.screenX, contender.screenY)
        && contender.isCursorOnTopOf(x, y, radius, champion);
  }

  // jvm < 1.4 does not have a BitSet.clear();
  // so in order to clear you "and" with an empty bitset.
  private final BS bsEmpty = new BS();
  private final BS bsFoundRectangle = new BS();

  public BS findAtomsInRectangle(Rectangle rect, BS bsModels) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if (bsModels.get(atom.modelIndex) && atom.isVisible(0) 
          && rect.contains(atom.screenX, atom.screenY))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  protected void fillADa(AtomData atomData, int mode) {
    atomData.atomXyz = atoms;
    atomData.atomCount = atomCount;
    atomData.atomicNumber = new int[atomCount];
    boolean includeRadii = ((mode & AtomData.MODE_FILL_RADII) != 0);
    if (includeRadii)
      atomData.atomRadius = new float[atomCount];
    boolean isMultiModel = ((mode & AtomData.MODE_FILL_MULTIMODEL) != 0);
    for (int i = 0; i < atomCount; i++) {
      Atom atom = atoms[i];
      if (atom.isDeleted() || !isMultiModel && atomData.modelIndex >= 0
          && atom.modelIndex != atomData.firstModelIndex) {
        if (atomData.bsIgnored == null)
          atomData.bsIgnored = new BS();
        atomData.bsIgnored.set(i);
        continue;
      }
      atomData.atomicNumber[i] = atom.getElementNumber();
      atomData.lastModelIndex = atom.modelIndex;
      if (includeRadii)
        atomData.atomRadius[i] = getWorkingRadius(atom, atomData); 
    }
  }
  
  ////// hybridization ///////////

  @SuppressWarnings("incomplete-switch")
  private float getWorkingRadius(Atom atom, AtomData atomData) {
    float r = 0;
    RadiusData rd = atomData.radiusData;
    switch (rd.factorType) {
    case ABSOLUTE:
      r = rd.value;
      break;
    case FACTOR:
    case OFFSET:
      switch (rd.vdwType) {
      case IONIC:
        r = atom.getBondingRadiusFloat();
        break;
      case ADPMAX:
        r = atom.getADPMinMax(true);
        break;
      case ADPMIN:
        r = atom.getADPMinMax(false);
        break;
      default:
        r = atom.getVanderwaalsRadiusFloat(viewer,
            atomData.radiusData.vdwType);
      }
      if (rd.factorType == EnumType.FACTOR)
        r *= rd.value;
      else
        r += rd.value;
    }
    return r + rd.valueExtended;
  }

  /**
   * get a list of potential H atom positions based on 
   * elemental valence and formal charge
   * 
   * @param bs
   * @param nTotal
   * @param doAll       -- whether we add to C that already have H or not.
   * @param justCarbon
   * @param vConnect 
   * @return     array of arrays of points added to specific atoms
   */
  public P3[][] calculateHydrogens(BS bs, int[] nTotal,
                                            boolean doAll, boolean justCarbon,
                                            JmolList<Atom> vConnect) {
    V3 z = new V3();
    V3 x = new V3();
    P3[][] hAtoms = new P3[atomCount][];
    BS bsDeleted = viewer.getDeletedAtoms();
    P3 pt;
    int nH = 0;
    
    // just not doing aldehydes here -- all A-X-B bent == sp3 for now
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (bsDeleted != null && bsDeleted.get(i))
          continue;
        Atom atom = atoms[i];
        int atomicNumber = atom.getElementNumber();
        if (justCarbon && atomicNumber != 6)
          continue;
        float dHX = (atomicNumber <= 6 ? 1.1f // B, C
            : atomicNumber <= 10 ? 1.0f       // N, O
            : 1.3f);                          // S
        switch (atomicNumber) {
        case 7:
        case 8:
          dHX = 1.0f;
          break;
        case 6:
        }
        if (doAll && atom.getCovalentHydrogenCount() > 0)
          continue;
        int n = getImplicitHydrogenCount(atom);
        if (n == 0)
          continue;
        int targetValence = aaRet[0];
        int hybridization = aaRet[2];
        int nBonds = aaRet[3];

        hAtoms[i] = new P3[n];
        int hPt = 0;
        if (nBonds == 0) {
          switch (n) {
          case 4:
            z.set(0.635f, 0.635f, 0.635f);
            pt = P3.newP(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            //$FALL-THROUGH$
          case 3:
            z.set(-0.635f, -0.635f, 0.635f);
            pt = P3.newP(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            //$FALL-THROUGH$
          case 2:
            z.set(-0.635f, 0.635f, -0.635f);
            pt = P3.newP(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            //$FALL-THROUGH$
          case 1:
            z.set(0.635f, -0.635f, -0.635f);
            pt = P3.newP(z);
            pt.add(atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
          }
        } else {
          switch (n) {
          default:
            break;
          case 3: // three bonds needed RC
            getHybridizationAndAxes(i, atomicNumber, z, x, "sp3b", false, true);
            pt = new P3();
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            getHybridizationAndAxes(i, atomicNumber, z, x, "sp3c", false, true);
            pt = new P3();
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            getHybridizationAndAxes(i, atomicNumber, z, x, "sp3d", false, true);
            pt = new P3();
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            break;
          case 2:
            // 2 bonds needed R2C or R-N or R2C=C or O
            //                    or RC=C or C=C
            boolean isEne = (hybridization == 2 || atomicNumber == 5 || nBonds == 1
                && targetValence == 4 || atomicNumber == 7 && isAdjacentSp2(atom));
            getHybridizationAndAxes(i, atomicNumber, z, x, (isEne ? "sp2b"
                : targetValence == 3 ? "sp3c" : "lpa"), false, true);
            pt = P3.newP(z);
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            getHybridizationAndAxes(i, atomicNumber, z, x, (isEne ? "sp2c"
                : targetValence == 3 ? "sp3d" : "lpb"), false, true);
            pt = P3.newP(z);
            pt.scaleAdd2(dHX, z, atom);
            hAtoms[i][hPt++] = pt;
            if (vConnect != null)
              vConnect.addLast(atom);
            break;
          case 1:
            // one bond needed R2B, R3C, R-N-R, R-O R=C-R R=N R-3-C
            // nbonds ......... 2 .. 3 .. 2 ... 1 ... 2 .. 1 .. 1
            // nval ........... 2 .. 3 .. 2 ... 1 ... 3 .. 2 .. 3
            // targetValence .. 3 .. 4 .. 3 ... 2 ... 4 .. 3 .. 4
            // tV - nbonds   .. 1    1    1     1     2    2    3
            // ................ sp2c sp3d sp3d  sp3b  sp2c sp2b sp
            switch (targetValence - nBonds) {
            case 1:
              // sp3 or Boron sp2 or N sp2
              if (atomicNumber == 8 && atom == atom.getGroup().getCarbonylOxygenAtom()) {
                hAtoms[i] = null;
                continue;
              }
              if (getHybridizationAndAxes(i, atomicNumber, z, x, (hybridization == 2 || atomicNumber == 5 
                  || atomicNumber == 7 && isAdjacentSp2(atom) 
                  ? "sp2c"
                  : "sp3d"), true, false) != null) {
                pt = P3.newP(z);
                pt.scaleAdd2(dHX, z, atom);
                hAtoms[i][hPt++] = pt;
                if (vConnect != null)
                  vConnect.addLast(atom);
              } else {
                hAtoms[i] = new P3[0];
              }
              break;
            case 2:
              // sp2
              getHybridizationAndAxes(i, atomicNumber, z, x, (targetValence == 4 ? "sp2c"
                  : "sp2b"), false, false);
              pt = P3.newP(z);
              pt.scaleAdd2(dHX, z, atom);
              hAtoms[i][hPt++] = pt;
              if (vConnect != null)
                vConnect.addLast(atom);
              break;
            case 3:
              // sp
              getHybridizationAndAxes(i, atomicNumber, z, x, "spb", false, true);
              pt = P3.newP(z);
              pt.scaleAdd2(dHX, z, atom);
              hAtoms[i][hPt++] = pt;
              if (vConnect != null)
                vConnect.addLast(atom);
              break;
            }
          }
        }
        nH += hPt;
      }
    nTotal[0] = nH;
    return hAtoms;
  }

  private boolean isAdjacentSp2(Atom atom) {
    Bond[] bonds = atom.bonds;
    for (int i = 0; i < bonds.length; i++) {
      Bond[] b2 = bonds[i].getOtherAtom(atom).bonds;
      for (int j = 0; j < b2.length; j++)
        switch (b2[j].order) {
        case JmolEdge.BOND_AROMATIC:
        case JmolEdge.BOND_AROMATIC_DOUBLE:
        case JmolEdge.BOND_COVALENT_DOUBLE:
        case JmolEdge.BOND_COVALENT_TRIPLE:
          return true;
        }
    }
    return false;
  }

  private int[] aaRet;

  int getImplicitHydrogenCount(Atom atom) {
    int targetValence = atom.getTargetValence();
    int charge = atom.getFormalCharge();
    if (aaRet == null)
      aaRet = new int[4];
    aaRet[0] = targetValence;
    aaRet[1] = charge;
    aaRet[2] = 0;
    aaRet[3] = atom.getCovalentBondCount();
    Model model = ((ModelCollection) this).models[atom.modelIndex];
    String s = (model.isBioModel && !model.isPdbWithMultipleBonds ? atom.group.getGroup3() : null);
    if (s != null && charge == 0) {
      if (JC.getAminoAcidValenceAndCharge(s, atom.getAtomName(),
          aaRet)) {
        targetValence = aaRet[0];
        charge = aaRet[1];
      }
    }
    if (charge != 0) {
      targetValence += (targetValence == 4 ? -Math.abs(charge) : charge);
      aaRet[0] = targetValence;
    }
    int n = targetValence - atom.getValence();
    return (n < 0 ? 0 : n);
  }

  private final static float sqrt3_2 = (float) (Math.sqrt(3) / 2);
  private final static V3 vRef = V3.new3(3.14159f, 2.71828f, 1.41421f);
  private final static float almost180 = (float) Math.PI * 0.95f;

  public String getHybridizationAndAxes(int atomIndex, int atomicNumber, V3 z, V3 x,
                                        String lcaoTypeRaw,
                                        boolean hybridizationCompatible,
                                        boolean doAlignZ) {

    String lcaoType = (lcaoTypeRaw.length() > 0 && lcaoTypeRaw.charAt(0) == '-' ? lcaoTypeRaw
        .substring(1)
        : lcaoTypeRaw);

    if (lcaoTypeRaw.indexOf("d") >= 0 && !lcaoTypeRaw.equals("sp3d"))
      return getHybridizationAndAxesD(atomIndex, z, x, lcaoType);

    Atom atom = atoms[atomIndex];
    if (atomicNumber == 0)
      atomicNumber = atom.getElementNumber();
    Atom[] attached = getAttached(atom, 4, hybridizationCompatible);
    int nAttached = attached.length;
    int pt = lcaoType.charAt(lcaoType.length() - 1) - 'a';
    if (pt < 0 || pt > 6)
      pt = 0;
    V3 vTemp = new V3();
    z.set(0, 0, 0);
    x.set(0, 0, 0);
    V3[] v = new V3[4];
    for (int i = 0; i < nAttached; i++) {
      v[i] = V3.newV(atom);
      v[i].sub(attached[i]);
      v[i].normalize();
      z.add(v[i]);
    }
    if (nAttached > 0)
      x.setT(v[0]);
    boolean isPlanar = false;
    if (nAttached >= 3) {
      if (x.angle(v[1]) < almost180)
        vTemp.cross(x, v[1]);
      else
        vTemp.cross(x, v[2]);
      vTemp.normalize();
      V3 vTemp2 = new V3();
      if (v[1].angle(v[2]) < almost180)
        vTemp2.cross(v[1], v[2]);
      else
        vTemp2.cross(x, v[2]);
      vTemp2.normalize();
      isPlanar = (Math.abs(vTemp2.dot(vTemp)) >= 0.95f);
    }

    boolean isSp3 = (lcaoType.indexOf("sp3") == 0);
    boolean isSp2 = (!isSp3 && lcaoType.indexOf("sp2") == 0);
    boolean isSp = (!isSp3 && !isSp2 && lcaoType.indexOf("sp") == 0);
    boolean isP = (lcaoType.indexOf("p") == 0);
    boolean isLp = (lcaoType.indexOf("lp") == 0);
    
    String hybridization = null;
    if (hybridizationCompatible) {
      if (nAttached == 0)
        return null;
      if (isSp3) {
        if (pt > 3 || nAttached > 4)
          return null;
      } else if (isSp2) {
        if (pt > 2 || nAttached > 3)
          return null;
      } else if (isSp) {
        if (pt > 1 || nAttached > 2)
          return null;
      }
      switch (nAttached) {
      case 1:
        if (atomicNumber == 1 && !isSp3)
          return null;
        if (isSp3) {
          hybridization = "sp3";
          break;
        }
        switch (attached[0].getCovalentBondCount()) {
        case 1:
          if (attached[0].getValence() != 2) {
            // C-t-C
            hybridization = "sp";
            break;
          }
          // C=C, no other atoms
          //$FALL-THROUGH$
        case 2:
          hybridization = (isSp ? "sp" : "sp2");
          break;
        case 3:
          // special case, for example R2C=O oxygen
          if (!isSp2 && !isP)
            return null;
          hybridization = "sp2";
          break;
        }
        break;
      case 2:
        if (z.length() < 0.1f) {
          // linear A--X--B
          if (lcaoType.indexOf("2") >= 0 || lcaoType.indexOf("3") >= 0)
            return null;
          hybridization = "sp";
          break;
        }
        // bent A--X--B
        hybridization = (isSp3 ? "sp3" : "sp2");
        if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
          break;
        }
        if (isLp) { // align z as lone pair
          hybridization = "lp"; // any is OK
          break;
        }
        hybridization = lcaoType;
        break;
      default:
        // 3 or 4 bonds
        if (isPlanar) {
          hybridization = "sp2";
        } else {
          if (isLp && nAttached == 3) {
            hybridization = "lp";
            break;
          }
          hybridization = "sp3";
        }
      }
      if (hybridization == null)
        return null;
      if (lcaoType.indexOf("p") == 0) {
        if (hybridization == "sp3")
          return null;
      } else if (lcaoType.indexOf(hybridization) < 0) {
        return null;
      }
    }

    if (pt < nAttached && !lcaoType.startsWith("p")
        && !lcaoType.startsWith("l")) {
      z.sub2(attached[pt], atom);
      z.normalize();
      return hybridization;
    }

    switch (nAttached) {
    case 0:
      if (lcaoType.equals("sp3c") || lcaoType.equals("sp2d")
          || lcaoType.equals("lpa")) {
        z.set(-0.5f, -0.7f, 1);
        x.set(1, 0, 0);
      } else if (lcaoType.equals("sp3b") || lcaoType.equals("lpb")) {
        z.set(0.5f, -0.7f, -1f);
        x.set(1, 0, 0);
      } else if (lcaoType.equals("sp3a")) {
        z.set(0, 1, 0);
        x.set(1, 0, 0);
      } else {
        z.set(0, 0, 1);
        x.set(1, 0, 0);
      }
      break;
    case 1:
      // X-C
      vTemp.setT(vRef);        
      x.cross(vTemp, z);
      if (isSp3) {
        // align z as sp3 orbital
        // with reference to atoms connected to connecting atom.
        // vRef is a pseudo-random vector
        // z is along the bond
        for (int i = 0; i < attached[0].bonds.length; i++) {
          if (attached[0].bonds[i].isCovalent()
              && attached[0].getBondedAtomIndex(i) != atom.index) {
            x.sub2(attached[0], attached[0].bonds[i].getOtherAtom(attached[0]));
            x.cross(z, x);
            if (x.length() == 0)
              continue;
            x.cross(x, z);
            break;
          }
        }
        x.normalize();
        if (Float.isNaN(x.x)) {
          x.setT(vRef);
          x.cross(x, z);
        }
        // x is perp to bond
        vTemp.cross(z, x);
        vTemp.normalize();
        // y1 is perp to bond and x
        z.normalize();
        x.scaleAdd2(2.828f, x, z); // 2*sqrt(2)
        if (pt != 3) {
          x.normalize();
          AxisAngle4f a = AxisAngle4f.new4(z.x, z.y, z.z,
              (pt == 2 ? 1 : -1) * 2.09439507f); // PI*2/3
          Matrix3f m = new Matrix3f();
          m.setIdentity();
          m.setAA(a);
          m.transform(x);
        }
        z.setT(x);
        x.cross(vTemp, z);
        break;
      }
      // not "sp3" -- sp2 or lone pair
      vTemp.cross(x, z); //x and vTemp are now perpendicular to z
      switch (attached[0].getCovalentBondCount()) {
      case 1:
        if (attached[0].getValence() != 2) {
          // C-t-C
          break;
        }
        // C=C, no other atoms
        //$FALL-THROUGH$
      case 2:
        // R-C=C* or C=C=C*
        // get third atom
        boolean isCumulated = false;
        Atom a0 = attached[0];
        x.setT(z);
        vTemp.setT(vRef);        
        while (a0 != null && a0.getCovalentBondCount() == 2) {
          Bond[] bonds = a0.bonds;
          Atom a = null;
          isCumulated = !isCumulated;
          for (int i = 0; i < bonds.length; i++)
            if (bonds[i].isCovalent()) {
              a = bonds[i].getOtherAtom(a0);
              if (a != atom) {
                vTemp.sub2(a, a0);
                break;
              }
            }
          vTemp.cross(vTemp, x);
          if (vTemp.length() > 0.1f || a.getCovalentBondCount() != 2)
            break;
          atom = a0;
          a0 = a;
        }
        if (vTemp.length() > 0.1f) {
          z.cross(vTemp, x);
          // C=C or RC=C
          z.normalize();
          if (pt == 1)
            z.scale(-1);
          z.scale(sqrt3_2);
          z.scaleAdd2(0.5f, x, z);
          if (isP) {
            vTemp.cross(z, x);
            z.setT(vTemp);
            vTemp.setT(x);
          } 
          x.cross(vTemp, z);
        } else {
          z.setT(x);
          x.cross(vRef, x);
        }
        break;
      case 3:
        // special case, for example R2C=O oxygen
        getHybridizationAndAxes(attached[0].index, 0, x, vTemp, "pz", false,
            doAlignZ);
        vTemp.setT(x);
        if (isSp2) { // align z as sp2 orbital
          x.cross(x, z);
          if (pt == 1)
            x.scale(-1);
          x.scale(sqrt3_2);
          z.scaleAdd2(0.5f, z, x);
        } else {
          vTemp.setT(z);
          z.setT(x);
        }
        x.cross(vTemp, z);
        break;
      }
      break;
    case 2:
      // two attached atoms -- check for linearity
      if (z.length() < 0.1f) {
        // linear A--X--B
        if (!lcaoType.equals("pz")) {
          Atom a = attached[0];
          boolean ok = (a.getCovalentBondCount() == 3);
          if (!ok)
            ok = ((a = attached[1]).getCovalentBondCount() == 3);
          if (ok) {
            // special case, for example R2C=C=CR2 central carbon
            getHybridizationAndAxes(a.index, 0, x, z, "pz", false, doAlignZ);
            if (lcaoType.equals("px"))
              x.scale(-1);
            z.setT(v[0]);
            break;
          }
          // O-C*-O
          vTemp.setT(vRef);    
          z.cross(vTemp, x);
          vTemp.cross(z, x);
        }
        z.setT(x);
        x.cross(vTemp, z);
        break;
      }
      // bent A--X--B
      vTemp.cross(z, x);
      if (isSp2) { // align z as sp2 orbital
        x.cross(z, vTemp);
        break;
      }
      if (isSp3 || isLp) { // align z as lone pair
        vTemp.normalize();
        z.normalize();
        if (!lcaoType.equals("lp")) {
          if (pt == 0 || pt == 2) 
            z.scaleAdd2(-1.2f, vTemp, z);
          else
            z.scaleAdd2(1.2f, vTemp, z);
        }
        x.cross(z, vTemp);
        break;
      }
      // align z as p orbital
      x.cross(z, vTemp);
      z.setT(vTemp);
      if (z.z < 0) {
        z.scale(-1);
        x.scale(-1);
      }
      break;
    default:
      // 3 bonds, sp3 or sp2 and lp/p
      if (isSp3)
        break;
      if (!isPlanar) {
        // not aligned -- really sp3
        x.cross(z, x);
        break;
      }
      // align z as p orbital
      z.setT(vTemp);
      if (z.z < 0 && doAlignZ) {
        z.scale(-1);
        x.scale(-1);
      }
    }

    x.normalize();
    z.normalize();

    //  System.out.println(atom.getInfo() + " nAttached=" + nAttached + " "
    //      + hybridization);
    return hybridization;
  }
  
  /**
   * dsp3 (trigonal bipyramidal, see-saw, T-shaped) 
   * or d2sp3 (square planar, square pyramidal, octahedral)
   *  
   * @param atomIndex  
   * @param z 
   * @param x 
   * @param lcaoType
   * @return valid hybridization or null
   */
  private String getHybridizationAndAxesD(int atomIndex, V3 z, V3 x,
                                         String lcaoType) {
    // note -- d2sp3, not sp3d2; dsp3, not sp3d
    if (lcaoType.startsWith("sp3d2"))
      lcaoType = "d2sp3"
          + (lcaoType.length() == 5 ? "a" : lcaoType.substring(5));
    if (lcaoType.startsWith("sp3d"))
      lcaoType = "dsp3"
          + (lcaoType.length() == 4 ? "a" : lcaoType.substring(4));
    if (lcaoType.equals("d2sp3") || lcaoType.equals("dsp3"))
      lcaoType += "a";
    boolean isTrigonal = lcaoType.startsWith("dsp3");
    int pt = lcaoType.charAt(lcaoType.length() - 1) - 'a';
    if (z != null && (!isTrigonal && (pt > 5 || !lcaoType.startsWith("d2sp3"))
        || isTrigonal && pt > 4))
      return null;
    
    // pt: a 0   b 1   c 2   d 3   e 4   f 5
    
    Atom atom = atoms[atomIndex];
    Atom[] attached = getAttached(atom, 6, true);
    if (attached == null)
      return (z == null ? null : "?");
    int nAttached = attached.length;
    if (nAttached < 3 && z != null)
      return null;
    boolean isLP = (pt >= nAttached);

    // determine geometry

    int nAngles = nAttached * (nAttached - 1) / 2;
    int[][] angles = ArrayUtil.newInt2(nAngles);
    
    // all attached angles must be around 180, 120, or 90 degrees
    
    int[] ntypes = new int[3];
    int[][] typePtrs = new int[3][nAngles];
    
    int n = 0;
    int _90 = 0;
    int _120 = 1;
    int _180 = 2;
    int n120_atom0 = 0;
    for (int i = 0; i < nAttached - 1; i++)
      for (int j = i + 1; j < nAttached; j++) {
        float angle = Measure
            .computeAngleABC(attached[i], atom, attached[j], true);
        // cutoffs determined empirically and meant to be generous
        int itype = (angle < 105 ? _90 : angle >= 150 ? _180 : _120);
        typePtrs[itype][ntypes[itype]] = n;
        ntypes[itype]++;
        angles[n++] = new int[] { i, j };
        if (i == 0 && itype == _120)
          n120_atom0++;
      }
    // categorization is done simply by listing 
    // the number of 90, 120, and 180 angles.
    n = ntypes[_90] * 100 + ntypes[_120] * 10 + ntypes[_180];
    if (z == null) {
      // just return geometry
      switch (n) {
      default:
        return "";
      case 0:
        return "";// just ignore atoms with only one bond? (atom.getElementNumber() == 1 ? "s" : "");
      case 1:
        return "linear";
      case 100:
      case 10:
        return "bent";
      case 111:
      case 201:
        return "T-shaped";// -- AX3E or AX3E2 or AX3E3
      case 30:
      case 120:
      case 210:
      case 300:
        if (Math.abs(Measure.computeTorsion(attached[0], atom, attached[1], attached[2], true)) > 162)
            return "trigonal planar";// -- AX3
        return "trigonal pyramidal";// -- AX3E
      case 330: 
        // may just have a rather distorted tetrahedron, as in "$phosphorus pentoxide"
        // in that case, each atom will have 1 or 3 120o angles, not 0 or 2, as in trigonal pyramid
        return (n120_atom0 % 2 == 1 ? "tetrahedral" : "uncapped trigonal pyramid");// -- AX4 or AX4E
      case 60:
      case 150:
      case 240:
        return "tetrahedral";// -- AX4
      case 402:
        return "square planar";// -- AX4E2
      case 411:
      case 501:
        return "see-saw";// -- AX4E
      case 631:
        return "trigonal bipyramidal";// -- AX5
      case 802:
        return "uncapped square pyramid";// -- AX5E
      case 1203:
        return "octahedral";// -- AX6
      }
    }

    switch (n) {
    default:
      return null;
      // 111 is also possible, but quite odd
    case 201:
      // 201 T-shaped -- could be either
      break;
    case 210:
    case 330: 
    case 411:
    case 631:
      // 210 no name (90-90-120)
      // 411 see-saw
      // 330 trigonal pyramid
      // 631 trigonal bipyramidal 
     if (!isTrigonal)
       return null;
     break;
    case 300:
    case 402:
    case 501:
    case 802:
    case 1203:
     // 300 no name (90-90-90)   
     // 402 square planar
     // 501 no name (see-saw like, but with 90o angle)
     // 802 square pyramidal
     // 1203 octahedral
      if (isTrigonal)
        return null;
     break;
    }
    // if subType a-f is pointing to an attached atom, use it
    // otherwise, we need to find the position
    if (isLP) {
      int[] a;
      BS bs;
      if (isTrigonal) {
        switch (ntypes[_120]) {
        case 0:
          // T-shaped
          z.sub2(attached[angles[typePtrs[_90][0]][0]], atom);
          x.sub2(attached[angles[typePtrs[_90][0]][1]], atom);
          z.cross(z, x);
          z.normalize();
          if (pt == 4)
            z.scale(-1);
          bs = findNotAttached(nAttached, angles, typePtrs[_180], ntypes[_180]);
          int i = bs.nextSetBit(0);
          x.sub2(attached[i], atom);
          x.normalize();
          x.scale(0.5f);
          z.scaleAdd2(sqrt3_2, z, x);
          pt = -1;
          break;
        case 1:
          // see-saw
          if (pt == 4) {
            a = angles[typePtrs[_120][0]];
            z.add2(attached[a[0]], attached[a[1]]);
            z.scaleAdd2(-2, atom, z);
            pt = -1;
          } else {
            bs = findNotAttached(nAttached, angles, typePtrs[_120], ntypes[_120]);
            pt = bs.nextSetBit(0);            
          }
          break;
        default:
          // unobserved nor-apical trigonal bipyramid
          // or highly distorted trigonal pyramid (PH3)
          bs = findNotAttached(nAttached, angles, typePtrs[_120], ntypes[_120]);
          pt = bs.nextSetBit(0);
        }
      } else {
        boolean isPlanar = false;
        if (nAttached == 4) {
          switch (ntypes[_180]) {
          case 1:
            // unobserved cis-nor-octahedron
            bs = findNotAttached(nAttached, angles, typePtrs[_180],
                ntypes[_180]);
            int i = bs.nextSetBit(0);
            if (pt == 4)
              pt = i;
            else
              pt = bs.nextSetBit(i + 1);
            break;
          default:
            // square planar
            isPlanar = true;
          }
        } else {
          // square pyramidal
          bs = findNotAttached(nAttached, angles, typePtrs[_180], ntypes[_180]);
          int i = bs.nextSetBit(0);
          for (int j = nAttached; j < pt && i >= 0; j++)
            i = bs.nextSetBit(i + 1);
          if (i == -1)
            isPlanar = true;
          else
            pt = i;
        }
        if (isPlanar) {
          // square planar or T-shaped
          z.sub2(attached[angles[typePtrs[_90][0]][0]], atom);
          x.sub2(attached[angles[typePtrs[_90][0]][1]], atom);
          z.cross(z, x);
          if (pt == 4)
            z.scale(-1);
          pt = -1;
        }
      }
    }
    if (pt >= 0)
      z.sub2(attached[pt], atom);
    if (isLP)
      z.scale(-1);
    z.normalize();
    return (isTrigonal ? "dsp3" : "d2sp3");
  }

  private Atom[] getAttached(Atom atom, int nMax, boolean doSort) {
    int nAttached = atom.getCovalentBondCount();
    if (nAttached > nMax)
      return null;
    Atom[] attached = new Atom[nAttached];
    if (nAttached > 0) {
      Bond[] bonds = atom.bonds;
      int n = 0;
      for (int i = 0; i < bonds.length; i++)
        if (bonds[i].isCovalent())
          attached[n++] = bonds[i].getOtherAtom(atom);
      if (doSort)
        Arrays.sort(attached, new AtomSorter());
    }
    return attached;
  }

  private BS findNotAttached(int nAttached, int[][] angles, int[] ptrs, int nPtrs) {
    BS bs = BSUtil.newBitSet(nAttached);
    bs.setBits(0, nAttached);
    for (int i = 0; i < nAttached; i++)
      for (int j = 0; j < nPtrs; j++) {
        int[] a = angles[ptrs[j]];
        if (a[0] == i || a[1] == i)
          bs.clear(i);
      }
    return bs;
  }

  class AtomSorter implements Comparator<Atom>{
    public int compare(Atom a1, Atom a2) {
      return (a1.index > a2.index ? 1 : a1.index < a2.index ? -1 : 0);
    }    
  }
  
  /*
   * ******************************************************
   * 
   * These next methods are used by Eval to select for specific atom sets. They
   * all return a BitSet
   * 
   * ******************************************************
   */

  /**
   * general unqualified lookup of atom set type
   * 
   * @param tokType
   * @param specInfo
   * @return BitSet; or null if we mess up the type
   */
  protected BS getAtomBitsMDa(int tokType, Object specInfo) {
    BS bs = new BS()  ;
    BS bsInfo;
    BS bsTemp;
    int iSpec;
    
    // this first set does not assume sequential order in the file

    int i = 0;
    switch (tokType) {
    case T.atomno:
      iSpec = ((Integer) specInfo).intValue();
      for (i = atomCount; --i >= 0;)
        if (atoms[i].getAtomNumber() == iSpec)
          bs.set(i);
      break;
    case T.atomname:
      String names = "," + specInfo + ",";
      for (i = atomCount; --i >= 0;) {
        String name = atoms[i].getAtomName();
        if (names.indexOf(name) >= 0)
          if (names.indexOf("," + name + ",") >= 0)
            bs.set(i);
      }
      break;
    case T.atomtype:
      String types = "," + specInfo + ",";
      for (i = atomCount; --i >= 0;) {
        String type = atoms[i].getAtomType();
        if (types.indexOf(type) >= 0)
          if (types.indexOf("," + type + ",") >= 0)
            bs.set(i);
      }
      break;
    case T.spec_resid:
      iSpec = ((Integer) specInfo).intValue();
      for (i = atomCount; --i >= 0;)
        if (atoms[i].getGroupID() == iSpec)
          bs.set(i);
      break;
    case T.spec_chain:
      return BSUtil.copy(getChainBits((char) ((Integer) specInfo).intValue()));
    case T.spec_seqcode:
      return BSUtil.copy(getSeqcodeBits(((Integer) specInfo).intValue(), true));
    case T.hetero:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isHetero())
          bs.set(i);
      break;
    case T.hydrogen:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].getElementNumber() == 1)
          bs.set(i);
      break;
    case T.protein:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isProtein())
          bs.set(i);
      break;
    case T.carbohydrate:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isCarbohydrate())
          bs.set(i);
      break;
    case T.helix: // WITHIN -- not ends
    case T.sheet: // WITHIN -- not ends
      EnumStructure type = (tokType == T.helix ? EnumStructure.HELIX
          : EnumStructure.SHEET);
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isWithinStructure(type))
          bs.set(i);
      break;
    case T.nucleic:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isNucleic())
          bs.set(i);
      break;
    case T.dna:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isDna())
          bs.set(i);
      break;
    case T.rna:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isRna())
          bs.set(i);
      break;
    case T.purine:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isPurine())
          bs.set(i);
      break;
    case T.pyrimidine:
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isPyrimidine())
          bs.set(i);
      break;
    case T.element:
      bsInfo = (BS) specInfo;
      bsTemp = new BS();
      for (i = bsInfo.nextSetBit(0); i >= 0; i = bsInfo.nextSetBit(i + 1))
        bsTemp.set(getElementNumber(i));
      for (i = atomCount; --i >= 0;)
        if (bsTemp.get(getElementNumber(i)))
          bs.set(i);
      break;
    case T.site:
      bsInfo = (BS) specInfo;
      bsTemp = new BS();
      for (i = bsInfo.nextSetBit(0); i >= 0; i = bsInfo.nextSetBit(i + 1))
        bsTemp.set(atoms[i].atomSite);
      for (i = atomCount; --i >= 0;)
        if (bsTemp.get(atoms[i].atomSite))
          bs.set(i);
      break;
    case T.identifier:
      return getIdentifierOrNull((String) specInfo);
    case T.spec_atom:
      String atomSpec = ((String) specInfo).toUpperCase();
      if (atomSpec.indexOf("\\?") >= 0)
        atomSpec = TextFormat.simpleReplace(atomSpec, "\\?", "\1");
      // / here xx*yy is NOT changed to "xx??????????yy"
      for (i = atomCount; --i >= 0;)
        if (isAtomNameMatch(atoms[i], atomSpec, false))
          bs.set(i);
      break;
    case T.spec_alternate:
      String spec = (String) specInfo;
      for (i = atomCount; --i >= 0;)
        if (atoms[i].isAlternateLocationMatch(spec))
          bs.set(i);
      break;
    case T.spec_name_pattern:
      return getSpecName((String) specInfo);
    }
    if (i < 0)
      return bs;

    // these next assume sequential position in the file
    // speeding delivery -- Jmol 11.9.24

    bsInfo = (BS) specInfo;
    int iModel, iPolymer;
    int i0 = bsInfo.nextSetBit(0);
    if (i0 < 0)
      return bs;
    i = 0;
    switch (tokType) {
    case T.group:
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        int j = atoms[i].getGroup().selectAtoms(bs);
        if (j > i)
          i = j;
      }
      break;
    case T.model:
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        if (bs.get(i))
          continue;
        iModel = atoms[i].modelIndex;
        bs.set(i);
        for (int j = i; --j >= 0;)
          if (atoms[j].modelIndex == iModel)
            bs.set(j);
          else
            break;
        for (; ++i < atomCount;)
          if (atoms[i].modelIndex == iModel)
            bs.set(i);
          else
            break;
      }
      break;
    case T.chain:
      bsInfo = BSUtil.copy((BS) specInfo);
      for (i = bsInfo.nextSetBit(0); i >= 0; i = bsInfo.nextSetBit(i + 1)) {
        Chain chain = atoms[i].getChain();
        chain.setAtomBitSet(bs);
        bsInfo.andNot(bs);
      }
      break;
    case T.polymer:
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        if (bs.get(i))
          continue;
        iPolymer = atoms[i].getPolymerIndexInModel();
        bs.set(i);
        for (int j = i; --j >= 0;)
          if (atoms[j].getPolymerIndexInModel() == iPolymer)
            bs.set(j);
          else
            break;
        for (; ++i < atomCount;)
          if (atoms[i].getPolymerIndexInModel() == iPolymer)
            bs.set(i);
          else
            break;
      }
      break;
    case T.structure:
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        if (bs.get(i))
          continue;
        Object structure = atoms[i].getGroup().getStructure();
        bs.set(i);
        for (int j = i; --j >= 0;)
          if (atoms[j].getGroup().getStructure() == structure)
            bs.set(j);
          else
            break;
        for (; ++i < atomCount;)
          if (atoms[i].getGroup().getStructure() == structure)
            bs.set(i);
          else
            break;
      }
      break;
    }
    if (i == 0)
      Logger.error("MISSING getAtomBits entry for " + T.nameOf(tokType));
    return bs;
  }
  
   /**
   * overhauled by RMH Nov 1, 2006.
   * 
   * @param identifier
   * @return null or bs
   */
  private BS getIdentifierOrNull(String identifier) {
    //a primitive lookup scheme when [ ] are not used
    //nam
    //na?
    //nam45
    //nam45C
    //nam45^
    //nam45^A
    //nam45^AC -- note, no colon here -- if present, handled separately
    //nam4? does NOT match anything for PDB files, but might for others
    //atom specifiers:
    //H?
    //H32
    //H3?

    //in the case of a ?, we take the whole thing
    // * can be used here, but not with ?
    //first check with * option OFF
    BS bs = getSpecNameOrNull(identifier, false);
    
    if (identifier.indexOf("\\?") >= 0)
      identifier = TextFormat.simpleReplace(identifier, "\\?","\1");
    if (bs != null || identifier.indexOf("?") > 0)
      return bs;
    // now check with * option ON
    if (identifier.indexOf("*") > 0) 
      return getSpecNameOrNull(identifier, true);
    
    int len = identifier.length();
    int pt = 0;
    while (pt < len && Character.isLetter(identifier.charAt(pt)))
      ++pt;
    bs = getSpecNameOrNull(identifier.substring(0, pt), false);
    if (pt == len)
      return bs;
    if (bs == null)
      bs = new BS();
    //
    // look for a sequence number or sequence number ^ insertion code
    //
    int pt0 = pt;
    while (pt < len && Character.isDigit(identifier.charAt(pt)))
      ++pt;
    int seqNumber = 0;
    try {
      seqNumber = Integer.parseInt(identifier.substring(pt0, pt));
    } catch (NumberFormatException nfe) {
      return null;
    }
    char insertionCode = ' ';
    if (pt < len && identifier.charAt(pt) == '^')
      if (++pt < len)
        insertionCode = identifier.charAt(pt);
    int seqcode = Group.getSeqcodeFor(seqNumber, insertionCode);
    BS bsInsert = getSeqcodeBits(seqcode, false);
    if (bsInsert == null) {
      if (insertionCode != ' ')
        bsInsert = getSeqcodeBits(Character.toUpperCase(identifier.charAt(pt)),
            false);
      if (bsInsert == null)
        return null;
      pt++;
    }
    bs.and(bsInsert);
    if (pt >= len)
      return bs;
    //
    // look for a chain spec -- no colon
    //
    char chainID = identifier.charAt(pt++);
    bs.and(getChainBits(chainID));
    if (pt == len)
      return bs;
    //
    // not applicable
    //
    return null;
  }

  private BS getSpecName(String name) {
    // * can be used here with ?
    BS bs = getSpecNameOrNull(name, false);
    if (bs != null)
      return bs;
    if (name.indexOf("*") > 0)     
      bs = getSpecNameOrNull(name, true);
    return (bs == null ? new BS() : bs);
  }

  private BS getSpecNameOrNull(String name, boolean checkStar) {
    /// here xx*yy is changed to "xx??????????yy" when coming from getSpecName
    /// but not necessarily when coming from getIdentifierOrNull
    BS bs = null;
    name = name.toUpperCase();
    if (name.indexOf("\\?") >= 0)
      name = TextFormat.simpleReplace(name, "\\?","\1");
    for (int i = atomCount; --i >= 0;) {
      String g3 = atoms[i].getGroup3(true);
      if (g3 != null && g3.length() > 0) {
        if (TextFormat.isMatch(g3, name, checkStar, true)) {
          if (bs == null)
            bs = BSUtil.newBitSet(i + 1);
          bs.set(i);
          while (--i >= 0 && atoms[i].getGroup3(true).equals(g3))
            bs.set(i);
          i++;
        }
      } else if (isAtomNameMatch(atoms[i], name, checkStar)) {
        if (bs == null)
          bs = BSUtil.newBitSet(i + 1);
        bs.set(i);
      }
    }
    return bs;
  }

  private boolean isAtomNameMatch(Atom atom, String strPattern, boolean checkStar) {
    /// here xx*yy is changed to "xx??????????yy" when coming from getSpecName
    /// but not necessarily when coming from getIdentifierOrNull
    /// and NOT when coming from getAtomBits with Token.spec_atom
    /// because it is presumed that some names can include "*"
    return TextFormat.isMatch(atom.getAtomName().toUpperCase(), strPattern,
        checkStar, false);
  }
  
  protected BS getSeqcodeBits(int seqcode, boolean returnEmpty) {
    BS bs = new BS();
    int seqNum = Group.getSequenceNumber(seqcode);
    boolean haveSeqNumber = (seqNum != Integer.MAX_VALUE);
    boolean isEmpty = true;
    char insCode = Group.getInsertionCode(seqcode);
    switch (insCode) {
    case '?':
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if (!haveSeqNumber 
            || seqNum == Group.getSequenceNumber(atomSeqcode)
            && Group.getInsertionCodeValue(atomSeqcode) != 0) {
          bs.set(i);
          isEmpty = false;
        }
      }
      break;
    default:
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if (seqcode == atomSeqcode || 
            !haveSeqNumber && seqcode == Group.getInsertionCodeValue(atomSeqcode) 
            || insCode == '*' && seqNum == Group.getSequenceNumber(atomSeqcode)) {
          bs.set(i);
          isEmpty = false;
        }
      }
    }
    return (!isEmpty || returnEmpty ? bs : null);
  }

  protected BS getChainBits(char chainId) {
    boolean caseSensitive = viewer.getChainCaseSensitive();
    if (!caseSensitive)
      chainId = Character.toUpperCase(chainId);
    BS bs = new BS();
    BS bsDone = BSUtil.newBitSet(atomCount);
    for (int i = bsDone.nextClearBit(0); i < atomCount; i = bsDone.nextClearBit(i + 1)) {
      Chain chain = atoms[i].getChain();
      if (chainId == (caseSensitive ? chain.chainID : Character.toUpperCase(chain.chainID))) {
        chain.setAtomBitSet(bs);
        bsDone.or(bs);
      } else {
        chain.setAtomBitSet(bsDone);
      }
    }
    return bs;
  }

  public int[] getAtomIndices(BS bs) {
    int n = 0;
    int[] indices = new int[atomCount];
    for (int j = bs.nextSetBit(0); j >= 0 && j < atomCount; j = bs.nextSetBit(j + 1))
      indices[j] = ++n;
    return indices;
  }

  public BS getAtomsWithin(float distance, Point4f plane) {
    BS bsResult = new BS();
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float d = Measure.distanceToPlane(plane, atom);
      if (distance > 0 && d >= -0.1 && d <= distance || distance < 0
          && d <= 0.1 && d >= distance || distance == 0 && Math.abs(d) < 0.01)
        bsResult.set(atom.index);
    }
    return bsResult;
  }
  
  public BS getAtomsWithinBs(float distance, P3[] points,
                               BS bsInclude) {
    BS bsResult = new BS();
    if (points.length == 0 || bsInclude != null && bsInclude.cardinality() == 0)
      return bsResult;
    if (bsInclude == null)
      bsInclude = BSUtil.setAll(points.length);
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      for (int j = bsInclude.nextSetBit(0); j >= 0; j = bsInclude
          .nextSetBit(j + 1))
        if (atom.distance(points[j]) < distance) {
          bsResult.set(i);
          break;
        }
    }
    return bsResult;
  }

  public BS getVisibleSet() {
    BS bs = new BS();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isVisible(0))
        bs.set(i);
    return bs;
  }

  public BS getClickableSet() {
    BS bs = new BS();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isClickable())
        bs.set(i);
    return bs;
  }

  protected void deleteModelAtoms(int firstAtomIndex, int nAtoms, BS bs) {
    // all atoms in the model are being deleted here
    atoms = (Atom[]) ArrayUtil.deleteElements(atoms, firstAtomIndex, nAtoms);
    atomCount = atoms.length;
    for (int j = firstAtomIndex; j < atomCount; j++) {
      atoms[j].index = j;
      atoms[j].modelIndex--;
    }
    atomNames = (String[]) ArrayUtil.deleteElements(atomNames, firstAtomIndex,
        nAtoms);
    atomTypes = (String[]) ArrayUtil.deleteElements(atomTypes, firstAtomIndex,
        nAtoms);
    atomSerials = (int[]) ArrayUtil.deleteElements(atomSerials, firstAtomIndex,
        nAtoms);
    bfactor100s = (short[]) ArrayUtil.deleteElements(bfactor100s,
        firstAtomIndex, nAtoms);
    hasBfactorRange = false;
    occupancies = (byte[]) ArrayUtil.deleteElements(occupancies,
        firstAtomIndex, nAtoms);
    partialCharges = (float[]) ArrayUtil.deleteElements(partialCharges,
        firstAtomIndex, nAtoms);
    ellipsoids = (Quadric[][]) ArrayUtil.deleteElements(ellipsoids,
        firstAtomIndex, nAtoms);
    vibrationVectors = (V3[]) ArrayUtil.deleteElements(vibrationVectors,
        firstAtomIndex, nAtoms);
    nSurfaceAtoms = 0;
    bsSurface = null;
    surfaceDistance100s = null;
    if (tainted != null)
      for (int i = 0; i < TAINT_MAX; i++)
        BSUtil.deleteBits(tainted[i], bs);
    // what about data?
  }

}

