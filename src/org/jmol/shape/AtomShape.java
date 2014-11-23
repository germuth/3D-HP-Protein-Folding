/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-22 03:13:40 -0500 (Tue, 22 Aug 2006) $
 * $Revision: 5412 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shape;

import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.constant.EnumPalette;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Group;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;

public abstract class AtomShape extends Shape {

  // Balls, Dots, Ellipsoids, Halos, Labels, Polyhedra, Stars, Vectors

  public short mad = (short)-1;
  public short[] mads;
  public short[] colixes;
  public byte[] paletteIDs;
  public int atomCount;
  public Atom[] atoms;
  public boolean isActive;
  
  public int monomerCount;
  public BS bsSizeDefault;
  
  public Group[] getMonomers() {
    return null;
  }

  @Override
  protected void initModelSet() {
    atoms = modelSet.atoms;
    atomCount = modelSet.getAtomCount();
    // in case this is due to "load append"
    if (mads != null)
      mads = ArrayUtil.arrayCopyShort(mads, atomCount);
    if (colixes != null)
      colixes = ArrayUtil.arrayCopyShort(colixes, atomCount);
    if (paletteIDs != null)
      paletteIDs = ArrayUtil.arrayCopyByte(paletteIDs, atomCount);
  }

  @Override
  public int getSize(int atomIndex) {
    return (mads == null ? 0 : mads[atomIndex]);
  }
  
  @Override
  protected void setSize(int size, BS bsSelected) {
    setSize2(size, bsSelected);
  }

  protected void setSize2(int size, BS bsSelected) {
    setSizeRD(size == 0 ? null : new RadiusData(null, size, EnumType.SCREEN,
        null), bsSelected);
  }

  @Override
  protected void setSizeRD(RadiusData rd, BS bsSelected) {
    // Halos Stars Vectors Ellipsoids
    if (atoms == null)  // vector values are ignored if there are none for a model 
      return;
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = new BS();
    boolean isVisible = (rd != null && rd.value != 0);
    boolean isAll = (bsSelected == null);
    int i0 = (isAll ? atomCount - 1 : bsSelected.nextSetBit(0));
    if (mads == null && i0 >= 0)
      mads = new short[atomCount];
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsSelected.nextSetBit(i + 1))) {
      Atom atom = atoms[i];
      mads[i] = atom.calculateMad(viewer, rd);
      //System.out.println("atomshape - setSize " + i + " " + rd);
//      System.out.println("atomSHape " + atom + " mad=" + mads[i]);
      bsSizeSet.setBitTo(i, isVisible);
      atom.setShapeVisibility(myVisibilityFlag, isVisible);
    }
  }

  protected void setPropAS(String propertyName, Object value, BS bs) {
    if ("color" == propertyName) {
      isActive = true;
      short colix = C.getColixO(value);
      byte pid = EnumPalette.pidOf(value);
      if (bsColixSet == null)
        bsColixSet = new BS();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        setColixAndPalette(colix, pid, i);
      return;
    }
    if ("colors" == propertyName) {
      isActive = true;
      Object[] data = (Object[]) value;
      short[] colixes = (short[]) data[0];
      float translucency  = ((Float) data[1]).floatValue();
      if (bsColixSet == null)
        bsColixSet = new BS();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        short colix = colixes[i];
        if (translucency > 0.01f)
          colix = C.getColixTranslucent3(colix, true, translucency);
        setColixAndPalette(colix, EnumPalette.UNKNOWN.id, i);
      }
      return;
    }
    if ("translucency" == propertyName) {
      isActive = true;
      boolean isTranslucent = (value.equals("translucent"));
      if (bsColixSet == null)
        bsColixSet = new BS();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (colixes == null) {
          colixes = new short[atomCount];
          paletteIDs = new byte[atomCount];
        }
        colixes[i] = C.getColixTranslucent3(colixes[i], isTranslucent,
            translucentLevel);
        if (isTranslucent)
          bsColixSet.set(i);
      }
      return;
    }
    if (propertyName == "deleteModelAtoms") {
      atoms = (Atom[]) ((Object[]) value)[1];
      int[] info = (int[]) ((Object[]) value)[2];
      atomCount = modelSet.getAtomCount();
      int firstAtomDeleted = info[1];
      int nAtomsDeleted = info[2];
      mads = (short[]) ArrayUtil.deleteElements(mads, firstAtomDeleted,
          nAtomsDeleted);
      colixes = (short[]) ArrayUtil.deleteElements(colixes, firstAtomDeleted,
          nAtomsDeleted);
      paletteIDs = (byte[]) ArrayUtil.deleteElements(paletteIDs,
          firstAtomDeleted, nAtomsDeleted);
      BSUtil.deleteBits(bsSizeSet, bs);
      BSUtil.deleteBits(bsColixSet, bs);
      return;
    }
    setPropS(propertyName, value, bs);
  }

  protected void setColixAndPalette(short colix, byte paletteID, int atomIndex) {
    if (colixes == null || atomIndex >= colixes.length) {
      if (colix == C.INHERIT_ALL)
        return;
      colixes = ArrayUtil.ensureLengthShort(colixes, atomIndex + 1);
      paletteIDs = ArrayUtil.ensureLengthByte(paletteIDs, atomIndex + 1);
    }
    if (bsColixSet == null)
      bsColixSet = BS.newN(atomCount);
    colixes[atomIndex] = colix = getColixI(colix, paletteID, atomIndex);
    bsColixSet.setBitTo(atomIndex, colix != C.INHERIT_ALL);
    paletteIDs[atomIndex] = paletteID;
  }

  @Override
  public void setModelClickability() {
    if (!isActive)
      return;
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.getShapeVisibilityFlags() & myVisibilityFlag) == 0
          || modelSet.isAtomHidden(i))
        continue;
      atom.setClickable(myVisibilityFlag);
    }
  }

  @Override
  public String getShapeState() {
    // stars and vectors will do this
    return (isActive ? viewer.getAtomShapeState(this) : "");
  }

  /**
   * @param i  
   * @return script, but only for Measures
   */
  public String getInfoAsString(int i) {
    // only in Measures
    return null;
  }

}
