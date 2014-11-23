/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

import org.jmol.script.T;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;

import org.jmol.api.JmolSelectionListener;
import org.jmol.i18n.GT;
import org.jmol.modelset.ModelSet;

public class SelectionManager {

  private Viewer viewer;

  private JmolSelectionListener[] listeners = new JmolSelectionListener[0];

  SelectionManager(Viewer viewer) {
    this.viewer = viewer;
  }

  final BS bsHidden = new BS();
  final BS bsSelection = new BS();
  final BS bsFixed = new BS();

  BS bsSubset; // set in Eval and only pointed to here
  BS bsDeleted;

  void deleteModelAtoms(BS bsDeleted) {
    BSUtil.deleteBits(bsHidden, bsDeleted);
    BSUtil.deleteBits(bsSelection, bsDeleted);
    BSUtil.deleteBits(bsSubset, bsDeleted);
    BSUtil.deleteBits(bsFixed, bsDeleted);
    BSUtil.deleteBits(this.bsDeleted, bsDeleted);
  }


  // this is a tri-state. the value -1 means unknown
  private final static int TRUE = 1;
  private final static int FALSE = 0;
  private final static int UNKNOWN = -1;
  private int empty = TRUE;

  boolean hideNotSelected;

  void clear() {
    clearSelection(true);
    hide(null, null, null, true);
    setSelectionSubset(null);
    bsDeleted = null;
    setMotionFixedAtoms(null);
  }

  void hide(ModelSet modelSet, BS bs, Boolean addRemove, boolean isQuiet) {
    if (bs == null) {
      bsHidden.clearAll();
    } else if (addRemove == null) {
      bsHidden.clearAll();
      bsHidden.or(bs);
    } else if (addRemove.booleanValue()) {
      bsHidden.or(bs);
    } else {
      bsHidden.andNot(bs);
    }
    if (modelSet != null)
      modelSet.setBsHidden(bsHidden);
    if (!isQuiet)
      viewer.reportSelection(GT._("{0} atoms hidden", ""
          + bsHidden.cardinality()));
  }

  void display(ModelSet modelSet, BS bs, Boolean addRemove, boolean isQuiet) {
      BS bsAll = modelSet.getModelAtomBitSetIncludingDeleted(-1, false); 
        if (bs == null) {
      bsHidden.clearAll();
    } else if (addRemove == null) {
      bsHidden.or(bsAll);
      bsHidden.andNot(bs);
    } else if (addRemove.booleanValue()) {
      bsHidden.andNot(bs);
    } else {
      bsHidden.or(bs);
    }
    BSUtil.andNot(bsHidden, bsDeleted);
    modelSet.setBsHidden(bsHidden);
    if (!isQuiet)
      viewer.reportSelection(GT._("{0} atoms hidden", ""
          + bsHidden.cardinality()));
  }

  BS getHiddenSet() {
    return bsHidden;
  }

  boolean getHideNotSelected() {
    return hideNotSelected;
  }

  void setHideNotSelected(boolean TF) {
    hideNotSelected = TF;
    if (TF)
      selectionChanged(false);
  }

  boolean isSelected(int atomIndex) {
    return (atomIndex >= 0 && bsSelection.get(atomIndex));
  }

  void select(BS bs, Boolean addRemove, boolean isQuiet) {
    if (bs == null) {
      selectAll(true);
      if (!viewer.getRasmolSetting(T.hydrogen))
        excludeSelectionSet(viewer.getAtomBits(T.hydrogen, null));
      if (!viewer.getRasmolSetting(T.hetero))
        excludeSelectionSet(viewer.getAtomBits(T.hetero, null));
      selectionChanged(false);
    } else {
      setSelectionSet(bs, addRemove);
    }
    boolean reportChime = viewer.getMessageStyleChime();
    if (!reportChime && isQuiet)
      return;
    int n = getSelectionCount();
    if (reportChime)
      viewer.reportSelection((n == 0 ? "No atoms" : n == 1 ? "1 atom" : n
          + " atoms")
          + " selected!");
    else if (!isQuiet)
      viewer.reportSelection(GT._("{0} atoms selected", n));
  }

  void selectAll(boolean isQuiet) {
    int count = viewer.getAtomCount();
    empty = (count == 0) ? TRUE : FALSE;
    for (int i = count; --i >= 0;)
      bsSelection.set(i);
    BSUtil.andNot(bsSelection, bsDeleted);
    selectionChanged(isQuiet);
  }

  void clearSelection(boolean isQuiet) {
    setHideNotSelected(false);
    bsSelection.clearAll();
    empty = TRUE;
    selectionChanged(isQuiet);
  }

  public boolean isAtomSelected(int atomIndex) {
    return (
        (bsSubset == null || bsSubset.get(atomIndex))
        && bsDeleted == null || !bsDeleted.get(atomIndex))
        && bsSelection.get(atomIndex);
  }
  
  public void setSelectedAtom(int atomIndex, boolean TF) {
    if (atomIndex < 0) {
      selectionChanged(true);
      return;
    }
    if (bsSubset != null && !bsSubset.get(atomIndex)
        || bsDeleted != null && bsDeleted.get(atomIndex))
      return;
    bsSelection.setBitTo(atomIndex, TF);
    if (TF)
      empty = FALSE;
    else
      empty = UNKNOWN;
  }

  void setSelectionSet(BS set, Boolean addRemove) {
    if (set == null) {
      bsSelection.clearAll();
    } else if (addRemove == null) {
      bsSelection.clearAll();
      bsSelection.or(set);
    } else if (addRemove.booleanValue()) {
      bsSelection.or(set);
    } else {
      bsSelection.andNot(set);
    }
    empty = UNKNOWN;
    selectionChanged(false);
  }

  void setSelectionSubset(BS bs) {

    // for informational purposes only
    // the real copy is in Eval so that eval operations
    // can all use it directly, and so that all these
    // operations still work properly on the full set of atoms

    bsSubset = bs;
  }

  boolean isInSelectionSubset(int atomIndex) {
    return (atomIndex < 0 || bsSubset == null || bsSubset.get(atomIndex));
  }

  void invertSelection() {
    BSUtil.invertInPlace(bsSelection, viewer.getAtomCount());
    empty = (bsSelection.length() > 0 ? FALSE : TRUE);
    selectionChanged(false);
  }

  private void excludeSelectionSet(BS setExclude) {
    if (setExclude == null || empty == TRUE)
      return;
    bsSelection.andNot(setExclude);
    empty = UNKNOWN;
  }

  private final BS bsTemp = new BS();

  int getSelectionCount() {
    if (empty == TRUE)
      return 0;
    empty = TRUE;
    BS bs;
    if (bsSubset != null) {
      bsTemp.clearAll();
      bsTemp.or(bsSubset);
      bsTemp.and(bsSelection);
      bs = bsTemp;
    } else {
      bs = bsSelection;
    }
    int count = bs.cardinality();
    if (count > 0)
      empty = FALSE;
    return count;
  }

  void addListener(JmolSelectionListener listener) {
    for (int i = listeners.length; --i >= 0;)
      if (listeners[i] == listener) {
        listeners[i] = null;
        break;
      }
    int len = listeners.length;
    for (int i = len; --i >= 0;)
      if (listeners[i] == null) {
        listeners[i] = listener;
        return;
      }
    if (listeners.length == 0)
      listeners = new JmolSelectionListener[1];
    else
      listeners = (JmolSelectionListener[]) ArrayUtil.doubleLength(listeners);
    listeners[len] = listener;
  }

  private void selectionChanged(boolean isQuiet) {
    if (hideNotSelected)
      hide(viewer.getModelSet(), BSUtil.copyInvert(bsSelection, viewer.getAtomCount()), null, isQuiet);
    if (isQuiet || listeners.length == 0)
      return;
    for (int i = listeners.length; --i >= 0;)
      if (listeners[i] != null)
        listeners[i].selectionChanged(bsSelection);
  }

  int deleteAtoms(BS bs) {
    BS bsNew = BSUtil.copy(bs);
    if (bsDeleted == null) {
      bsDeleted = bsNew;
    } else {
      bsNew.andNot(bsDeleted);
      bsDeleted.or(bs);
    }
    bsHidden.andNot(bsDeleted);
    bsSelection.andNot(bsDeleted);
    return bsNew.cardinality();
  }

  BS getDeletedAtoms() {
    return bsDeleted;
  }

  BS getSelectionSet(boolean includeDeleted) {
    if (includeDeleted || bsDeleted == null && bsSubset == null)
      return bsSelection;
    BS bs = new BS();
    bs.or(bsSelection);
    excludeAtoms(bs, false);
    return bs;
  }

  BS getSelectionSubset() {
    return bsSubset;
  }

  void excludeAtoms(BS bs, boolean ignoreSubset) {
    if (bsDeleted != null)
      bs.andNot(bsDeleted);
    if (!ignoreSubset && bsSubset != null)
      bs.and(bsSubset);
  }

  void processDeletedModelAtoms(BS bsAtoms) {
    if (bsDeleted != null)
      BSUtil.deleteBits(bsDeleted, bsAtoms);
    if (bsSubset != null)
      BSUtil.deleteBits(bsSubset, bsAtoms);
    BSUtil.deleteBits(bsFixed, bsAtoms);
    BSUtil.deleteBits(bsHidden, bsAtoms);
    BS bs = BSUtil.copy(bsSelection);
    BSUtil.deleteBits(bs, bsAtoms);
    setSelectionSet(bs, null);
  }

  void setMotionFixedAtoms(BS bs) {
    bsFixed.clearAll();
    if (bs != null)
      bsFixed.or(bs);
  }

  BS getMotionFixedAtoms() {
    return bsFixed;
  }

}
