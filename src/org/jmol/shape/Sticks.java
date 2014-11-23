/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

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

package org.jmol.shape;

import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.JmolEdge;
import org.jmol.util.P3;
import org.jmol.util.P3i;

import java.util.Hashtable;
import java.util.Map;


import org.jmol.constant.EnumPalette;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondIterator;

public class Sticks extends Shape {

  private int myMask;
  private boolean reportAll;
  
  private BS bsOrderSet;
  private BS selectedBonds;

  @Override
  public void initShape() {
    super.initShape();
    myMask = JmolEdge.BOND_COVALENT_MASK;
    reportAll = false;
  }

  /**
   * sets the size of a bond, or sets the selectedBonds set
   * 
   * @param size
   * @param bsSelected
   */
  @Override
  protected void setSize(int size, BS bsSelected) {
    if (size == Integer.MAX_VALUE) {
      selectedBonds = BSUtil.copy(bsSelected);
      return;
    }
    if (size == Integer.MIN_VALUE) { // smartaromatic has set the orders directly 
      if (bsOrderSet == null)
        bsOrderSet = new BS();
      bsOrderSet.or(bsSelected);
      return;
    }
    if (bsSizeSet == null)
      bsSizeSet = new BS();
    BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
        : modelSet.getBondIteratorForType(myMask, bsSelected));
    short mad = (short) size;
    while (iter.hasNext()) {
      bsSizeSet.set(iter.nextIndex());
      iter.next().setMad(mad);
    }
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("type" == propertyName) {
      myMask = ((Integer) value).intValue();
      return;
    }
    if ("reportAll" == propertyName) {
      // when connections are restored, all we can do is report them all
      reportAll = true;
      return;
    }

    if ("reset" == propertyName) {
      // all bonds have been deleted -- start over
      bsOrderSet = null;
      bsSizeSet = null;
      bsColixSet = null;
      selectedBonds = null;
      return;
    }

    if ("bondOrder" == propertyName) {
      if (bsOrderSet == null)
        bsOrderSet = new BS();
      int order = ((Integer) value).shortValue();
      BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
          : modelSet.getBondIteratorForType(JmolEdge.BOND_ORDER_ANY, bs));
      while (iter.hasNext()) {
        bsOrderSet.set(iter.nextIndex());
        iter.next().setOrder(order);
      }
      return;
    }
    if ("color" == propertyName) {
      if (bsColixSet == null)
        bsColixSet = new BS();
      short colix = C.getColixO(value);
      EnumPalette pal = (value instanceof EnumPalette ? (EnumPalette) value : null);
      if (pal == EnumPalette.TYPE || pal == EnumPalette.ENERGY) {
        //only for hydrogen bonds
        boolean isEnergy = (pal == EnumPalette.ENERGY);
        BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
            : modelSet.getBondIteratorForType(myMask, bs));
        while (iter.hasNext()) {
          bsColixSet.set(iter.nextIndex());
          Bond bond = iter.next();
          if (isEnergy) {
              bond.setColix(getColixB(colix, pal.id, bond));
              bond.setPaletteID(pal.id);
          } else {
            bond.setColix(C.getColix(JmolEdge.getArgbHbondType(bond.order)));
          }
        }
        return;
      }
      if (colix == C.USE_PALETTE && pal != EnumPalette.CPK)
        return; //palettes not implemented for bonds
      BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
          : modelSet.getBondIteratorForType(myMask, bs));
      while (iter.hasNext()) {
        int iBond = iter.nextIndex();
        Bond bond = iter.next();
        bond.setColix(colix);
        bsColixSet.setBitTo(iBond, (colix != C.INHERIT_ALL
            && colix != C.USE_PALETTE));
      }
      return;
    }
    if ("translucency" == propertyName) {
      if (bsColixSet == null)
        bsColixSet = new BS();
      boolean isTranslucent = (((String) value).equals("translucent"));
      BondIterator iter = (selectedBonds != null ? modelSet.getBondIterator(selectedBonds)
          : modelSet.getBondIteratorForType(myMask, bs));
      while (iter.hasNext()) {
        bsColixSet.set(iter.nextIndex());
        iter.next().setTranslucent(isTranslucent, translucentLevel);
      }
      return;
    }
    
    if ("deleteModelAtoms" == propertyName) {
      return;
    }
    
    setPropS(propertyName, value, bs);
  }

  @Override
  public Object getProperty(String property, int index) {
    if (property.equals("selectionState"))
      return (selectedBonds != null ? "select BONDS " + Escape.e(selectedBonds) + "\n":"");
    if (property.equals("sets"))
      return new BS[] { bsOrderSet, bsSizeSet, bsColixSet };
    return null;
  }

  @Override
  public void setModelClickability() {
    Bond[] bonds = modelSet.bonds;
    for (int i = modelSet.bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if ((bond.getShapeVisibilityFlags() & myVisibilityFlag) == 0
          || modelSet.isAtomHidden(bond.getAtomIndex1())
          || modelSet.isAtomHidden(bond.getAtomIndex2()))
        continue;
      bond.getAtom1().setClickable(myVisibilityFlag);
      bond.getAtom2().setClickable(myVisibilityFlag);
    }
  }

  @Override
  public String getShapeState() {
    return viewer.getBondState(this, bsOrderSet, reportAll);
  }
  
  @Override
  public boolean checkObjectHovered(int x, int y, BS bsVisible) {
    P3 pt = new P3();
    Bond bond = findPickedBond(x, y, bsVisible, pt);
    if (bond == null)
      return false;
    viewer.highlightBond(bond.index, true);
    return true;
  }
  

  @Override
  public Map<String, Object> checkObjectClicked(int x, int y, int modifiers,
                                    BS bsVisible) {
    P3 pt = new P3();
    Bond bond = findPickedBond(x, y, bsVisible, pt);
    if (bond == null)
      return null;
    int modelIndex = bond.getAtom1().modelIndex;
    String info = bond.getIdentity();
    Map<String, Object> map = new Hashtable<String, Object>();
    map.put("pt", pt);
    map.put("index", Integer.valueOf(bond.index));
    map.put("modelIndex", Integer.valueOf(modelIndex));
    map.put("model", viewer.getModelNumberDotted(modelIndex));
    map.put("type", "bond");
    map.put("info", info);
    viewer.setStatusAtomPicked(-3, "[\"bond\",\"" + bond.getIdentity() + "\"," + pt.x + "," + pt.y + "," + pt.z + "]");
    return map;
  }

  private final static int MAX_BOND_CLICK_DISTANCE_SQUARED = 10 * 10;
  private final P3i ptXY = new P3i();

  /**
   * 
   * @param x
   * @param y
   * @param bsVisible  UNUSED?
   * @param pt
   * @return picked bond or null
   */
  private Bond findPickedBond(int x, int y, BS bsVisible, P3 pt) {
    int dmin2 = MAX_BOND_CLICK_DISTANCE_SQUARED;
    if (gdata.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    Bond pickedBond = null;
    P3 v = new P3();
    Bond[] bonds = modelSet.bonds;
    for (int i = modelSet.bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if (bond.getShapeVisibilityFlags() == 0)
        continue;
      Atom atom1 = bond.getAtom1();
      Atom atom2 = bond.getAtom2();
      if (!atom1.isVisible(0) || !atom2.isVisible(0))
        continue;
      v.setT(atom1);
      v.add(atom2);
      v.scale(0.5f);
      int d2 = coordinateInRange(x, y, v, dmin2, ptXY);
      if (d2 >= 0) {
        float f = 1f * (ptXY.x - atom1.screenX) / (atom2.screenX - atom1.screenX);
        if (f < 0.4f || f > 0.6f)
          continue;
        dmin2 = d2;
        pickedBond = bond;
        pt.setT(v);
      }
    }
    return pickedBond;
  }
}
