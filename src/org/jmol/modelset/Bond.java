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

package org.jmol.modelset;



import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.JmolEdge;
import org.jmol.util.JmolNode;
import org.jmol.viewer.JC;

public class Bond extends JmolEdge {

  public static class BondSet extends BS {

    public BondSet() {
    }

    private int[] associatedAtoms;
    
    public int[] getAssociatedAtoms() {
      return associatedAtoms;
    }

    public BondSet(BS bs) {
      BSUtil.copy2(bs, this);
    }

    public BondSet(BS bs, int[] atoms) {
      this(bs);
      associatedAtoms = atoms;
    }
  }

  public Atom atom1;
  public Atom atom2;

  public short mad;
  public short colix;
  
  public Bond(Atom atom1, Atom atom2, int order,
              short mad, short colix) {
    this.atom1 = atom1;
    this.atom2 = atom2;
    this.colix = colix;
    setOrder(order);
    setMad(mad);
  }

  public void setMad(short mad) {
    this.mad = mad;
    setShapeVisibility(mad != 0);
  }

  public int shapeVisibilityFlags;
  
  public void setShapeVisibilityFlags(int shapeVisibilityFlags) {
    this.shapeVisibilityFlags = shapeVisibilityFlags;
  }

  public int getShapeVisibilityFlags() {
    return shapeVisibilityFlags;
  }

  void setShapeVisibility(boolean isVisible) {
    boolean wasVisible = ((shapeVisibilityFlags & myVisibilityFlag) != 0);
    if (wasVisible == isVisible)
      return;
    atom1.addDisplayedBond(myVisibilityFlag, isVisible);
    atom2.addDisplayedBond(myVisibilityFlag, isVisible);
    if (isVisible)
      shapeVisibilityFlags |= myVisibilityFlag;
    else
      shapeVisibilityFlags &= ~myVisibilityFlag;
  }
            
  
  public final static int myVisibilityFlag = JC.getShapeVisibilityFlag(JC.SHAPE_STICKS);

  public String getIdentity() {
    return (index + 1) + " "+ getOrderNumberAsString() + " " + atom1.getInfo() + " -- "
        + atom2.getInfo() + " " + atom1.distance(atom2);
  }

  @Override
  public boolean isCovalent() {
    return (order & BOND_COVALENT_MASK) != 0;
  }

  @Override
  public boolean isHydrogen() {
    return isOrderH(order);
  }

  public static boolean isOrderH(int order) {
    return (order & BOND_HYDROGEN_MASK) != 0;
  }

  boolean isStereo() {
    return (order & BOND_STEREO_MASK) != 0;
  }

  boolean isPartial() {
    return (order & BOND_PARTIAL_MASK) != 0;
  }

  boolean isAromatic() {
    return (order & BOND_AROMATIC_MASK) != 0;
  }

  /**
   * 
   * @param pid
   */
  public void setPaletteID(byte pid) {
    // hbonds only
  }

  public float getEnergy() {
    // hbonds only
    return 0;
  }
  
  public int getValence() {
    return (!isCovalent() ? 0
        : isPartial() || is(BOND_AROMATIC) ? 1
        : order & 7);
  }

  void deleteAtomReferences() {
    if (atom1 != null)
      atom1.deleteBond(this);
    if (atom2 != null)
      atom2.deleteBond(this);
    atom1 = atom2 = null;
  }

  public void setColix(short colix) {
    this.colix = colix;
  }

  public void setTranslucent(boolean isTranslucent, float translucentLevel) {
    colix = C.getColixTranslucent3(colix, isTranslucent, translucentLevel);
  }
  
  public boolean isTranslucent() {
    return C.isColixTranslucent(colix);
    //but may show up translucent anyway!
  }

  public void setOrder(int order) {
    if (atom1.getElementNumber() == 16 && atom2.getElementNumber() == 16)
      order |= BOND_SULFUR_MASK;
    if (order == BOND_AROMATIC_MASK)
      order = BOND_AROMATIC;
    this.order = order | (this.order & BOND_NEW);
  }

  public Atom getAtom1() {
    return atom1;
  }

  public Atom getAtom2() {
    return atom2;
  }

  @Override
  public int getAtomIndex1() {
    return atom1.index;
  }
  
  @Override
  public int getAtomIndex2() {
    return atom2.index;
  }
  
  float getRadius() {
    return mad / 2000f;
  }

  @Override
  public int getCovalentOrder() {
    return JmolEdge.getCovalentBondOrder(order);
  }

  String getOrderName() {
    return JmolEdge.getBondOrderNameFromOrder(order);
  }

  String getOrderNumberAsString() {
    return JmolEdge.getBondOrderNumberFromOrder(order);
  }

  short getColix1() {
    return C.getColixInherited(colix, atom1.colixAtom);
  }

  short getColix2() {
    return C.getColixInherited(colix, atom2.colixAtom);
  }

  public Atom getOtherAtom(Atom thisAtom) {
    return (atom1 == thisAtom ? atom2 : atom2 == thisAtom ? atom1 : null);
  }
  
  ////////////////////////////////////////////////////////////////
  
  public void setIndex(int i) {
    index = i;
  }

  public boolean is(int bondType) {
    return (order & ~BOND_NEW) == bondType;
  }

  @Override
  public JmolNode getOtherAtomNode(JmolNode thisAtom) {
    return (atom1 == thisAtom ? atom2 : atom2 == thisAtom ? atom1 : null);
  }
  
  @Override
  public String toString() {
    return atom1 + " - " + atom2;
  }

}
