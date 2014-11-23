/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.viewer;

import org.jmol.script.T;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.C;
import org.jmol.util.ColorUtil;
import org.jmol.util.Elements;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.constant.EnumPalette;
import org.jmol.constant.StaticConstants;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.util.ColorEncoder;


class ColorManager {

  /*
   * propertyColorEncoder is a "master" colorEncoded. It will be used
   * for all atom-based schemes (Jmol, Rasmol, shapely, etc.)
   * and it will be the 
   * 
   * 
   */

  ColorEncoder propertyColorEncoder = new ColorEncoder(null);
  private Viewer viewer;
  private GData g3d;

  // for atoms -- color CPK:

  private int[] argbsCpk;
  private int[] altArgbsCpk;

  // for properties.

  private float[] colorData;

  ColorManager(Viewer viewer, GData gdata) {
    this.viewer = viewer;
    g3d = gdata;
    argbsCpk = EnumPalette.argbsCpk;
    altArgbsCpk = ArrayUtil.arrayCopyRangeI(JC.altArgbsCpk, 0, -1);
  }

  void clear() {
    //causes problems? flushCaches();
  }

  private boolean isDefaultColorRasmol;

  boolean getDefaultColorRasmol() {
    return isDefaultColorRasmol;
  }

  void resetElementColors() {
    setDefaultColors(false);
  }

  void setDefaultColors(boolean isRasmol) {
    if (isRasmol) {
      isDefaultColorRasmol = true;
      argbsCpk = ArrayUtil.arrayCopyI(ColorEncoder.getRasmolScale(), -1);
    } else {
      isDefaultColorRasmol = false;
      argbsCpk = EnumPalette.argbsCpk;
    }
    altArgbsCpk = ArrayUtil.arrayCopyRangeI(JC.altArgbsCpk, 0, -1);
    propertyColorEncoder.createColorScheme((isRasmol ? "Rasmol="
        : "Jmol="), true, true);
    for (int i = EnumPalette.argbsCpk.length; --i >= 0;)
      g3d.changeColixArgb((short) i, argbsCpk[i]);
    for (int i = JC.altArgbsCpk.length; --i >= 0;)
      g3d.changeColixArgb((short) (Elements.elementNumberMax + i),
          altArgbsCpk[i]);
  }

  short colixRubberband = C.HOTPINK;

  void setRubberbandArgb(int argb) {
    colixRubberband = (argb == 0 ? 0 : C.getColix(argb));
  }

  /*
   * black or white, whichever contrasts more with the current background
   *
   *
   * @return black or white colix value
   */
  short colixBackgroundContrast;

  void setColixBackgroundContrast(int argb) {
    colixBackgroundContrast = ColorUtil.getBgContrast(argb);
  }

  short getColixBondPalette(Bond bond, int pid) {
    int argb = 0;
    switch (pid) {
    case StaticConstants.PALETTE_ENERGY:
      return propertyColorEncoder.getColorIndexFromPalette(bond.getEnergy(),
          -2.5f, -0.5f, ColorEncoder.BWR, false);
    }
    return (argb == 0 ? C.RED : C.getColix(argb));
  }

  short getColixAtomPalette(Atom atom, byte pid) {
    int argb = 0;
    int index;
    short id;
    ModelSet modelSet;
    int modelIndex;
    float lo, hi;
    // we need to use the byte form here for speed
    switch (pid) {
    case StaticConstants.PALETTE_PROPERTY:
      return (colorData == null || atom.index >= colorData.length
          ? C.GRAY : getColixForPropertyValue(colorData[atom.index]));
    case StaticConstants.PALETTE_NONE:
    case StaticConstants.PALETTE_CPK:
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changeable colix is allocated in this case
      id = atom.getAtomicAndIsotopeNumber();
      if (id < Elements.elementNumberMax)
        return g3d.getChangeableColix(id, argbsCpk[id]);
      id = (short) Elements.altElementIndexFromNumber(id);
      return g3d.getChangeableColix((short) (Elements.elementNumberMax + id),
          altArgbsCpk[id]);
    case StaticConstants.PALETTE_PARTIAL_CHARGE:
      // This code assumes that the range of partial charges is [-1, 1].
      index = ColorEncoder.quantize(atom.getPartialCharge(), -1, 1,
          JC.PARTIAL_CHARGE_RANGE_SIZE);
      return g3d.getChangeableColix(
          (short) (JC.PARTIAL_CHARGE_COLIX_RED + index),
          JC.argbsRwbScale[index]);
    case StaticConstants.PALETTE_FORMAL_CHARGE:
      index = atom.getFormalCharge() - Elements.FORMAL_CHARGE_MIN;
      return g3d.getChangeableColix(
          (short) (JC.FORMAL_CHARGE_COLIX_RED + index),
          JC.argbsFormalCharge[index]);
    case StaticConstants.PALETTE_TEMP:
    case StaticConstants.PALETTE_FIXEDTEMP:
      if (pid == StaticConstants.PALETTE_TEMP) {
        modelSet = viewer.getModelSet();
        lo = modelSet.getBfactor100Lo();
        hi = modelSet.getBfactor100Hi();
      } else {
        lo = 0;
        hi = 100 * 100; // scaled by 100
      }
      return propertyColorEncoder.getColorIndexFromPalette(
          atom.getBfactor100(), lo, hi, ColorEncoder.BWR, false);
    case StaticConstants.PALETTE_STRAIGHTNESS:
      return propertyColorEncoder.getColorIndexFromPalette(atom
          .getGroupParameter(T.straightness), -1, 1, ColorEncoder.BWR,
          false);
    case StaticConstants.PALETTE_SURFACE:
      hi = viewer.getSurfaceDistanceMax();
      return propertyColorEncoder.getColorIndexFromPalette(atom
          .getSurfaceDistance100(), 0, hi, ColorEncoder.BWR, false);
    case StaticConstants.PALETTE_AMINO:
      return propertyColorEncoder.getColorIndexFromPalette(atom.getGroupID(),
          0, 0, ColorEncoder.AMINO, false);
    case StaticConstants.PALETTE_SHAPELY:
      return propertyColorEncoder.getColorIndexFromPalette(atom.getGroupID(),
          0, 0, ColorEncoder.SHAPELY, false);
    case StaticConstants.PALETTE_GROUP:
      // viewer.calcSelectedGroupsCount() must be called first ...
      // before we call getSelectedGroupCountWithinChain()
      // or getSelectedGropuIndexWithinChain
      // however, do not call it here because it will get recalculated
      // for each atom
      // therefore, we call it in Eval.colorObject();
      return propertyColorEncoder.getColorIndexFromPalette(atom
          .getSelectedGroupIndexWithinChain(), 0, atom
          .getSelectedGroupCountWithinChain() - 1, ColorEncoder.BGYOR, false);
    case StaticConstants.PALETTE_POLYMER:
      Model m = viewer.getModelSet().models[atom.modelIndex];
      return propertyColorEncoder.getColorIndexFromPalette(atom
          .getPolymerIndexInModel(), 0, m.getBioPolymerCount() - 1,
          ColorEncoder.BGYOR, false);
    case StaticConstants.PALETTE_MONOMER:
      // viewer.calcSelectedMonomersCount() must be called first ...
      return propertyColorEncoder.getColorIndexFromPalette(atom
          .getSelectedMonomerIndexWithinPolymer(), 0, atom
          .getSelectedMonomerCountWithinPolymer() - 1, ColorEncoder.BGYOR,
          false);
    case StaticConstants.PALETTE_MOLECULE:
      modelSet = viewer.getModelSet();
      return propertyColorEncoder.getColorIndexFromPalette(modelSet
          .getMoleculeIndex(atom.getIndex(), true), 0, modelSet
          .getMoleculeCountInModel(atom.getModelIndex()) - 1,
          ColorEncoder.ROYGB, false);
    case StaticConstants.PALETTE_ALTLOC:
      modelSet = viewer.getModelSet();
      //very inefficient!
      modelIndex = atom.getModelIndex();
      return propertyColorEncoder
          .getColorIndexFromPalette(modelSet.getAltLocIndexInModel(modelIndex,
              atom.getAlternateLocationID()), 0, modelSet
              .getAltLocCountInModel(modelIndex), ColorEncoder.ROYGB, false);
    case StaticConstants.PALETTE_INSERTION:
      modelSet = viewer.getModelSet();
      //very inefficient!
      modelIndex = atom.getModelIndex();
      return propertyColorEncoder.getColorIndexFromPalette(modelSet
          .getInsertionCodeIndexInModel(modelIndex, atom.getInsertionCode()),
          0, modelSet.getInsertionCountInModel(modelIndex), ColorEncoder.ROYGB,
          false);
    case StaticConstants.PALETTE_JMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, T.jmol);
      break;
    case StaticConstants.PALETTE_RASMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, T.rasmol);
      break;
    case StaticConstants.PALETTE_STRUCTURE:
      argb = atom.getProteinStructureSubType().getColor();
      break;
    case StaticConstants.PALETTE_CHAIN:
      int chain = atom.getChainID() & 0x1F;
      if (chain < 0)
        chain = 0;
      if (chain >= JC.argbsChainAtom.length)
        chain = chain % JC.argbsChainAtom.length;
      argb = (atom.isHetero() ? JC.argbsChainHetero
          : JC.argbsChainAtom)[chain];
      break;
    }
    return (argb == 0 ? C.HOTPINK : C.getColix(argb));
  }

  private int getJmolOrRasmolArgb(int id, int argb) {
    switch (argb) {
    case T.jmol:
      if (id >= Elements.elementNumberMax)
        break;
      return propertyColorEncoder.getArgbFromPalette(id, 0, 0,
          ColorEncoder.JMOL);
    case T.rasmol:
      if (id >= Elements.elementNumberMax)
        break;
      return propertyColorEncoder.getArgbFromPalette(id, 0, 0,
          ColorEncoder.RASMOL);
    default:
      return argb;
    }
    return JC.altArgbsCpk[Elements.altElementIndexFromNumber(id)];
  }

  void setElementArgb(int id, int argb) {
    if (argb == T.jmol && argbsCpk == EnumPalette.argbsCpk)
      return;
    argb = getJmolOrRasmolArgb(id, argb);
    if (argbsCpk == EnumPalette.argbsCpk) {
      argbsCpk = ArrayUtil.arrayCopyRangeI(EnumPalette.argbsCpk, 0, -1);
      altArgbsCpk = ArrayUtil.arrayCopyRangeI(JC.altArgbsCpk, 0, -1);
    }
    if (id < Elements.elementNumberMax) {
      argbsCpk[id] = argb;
      g3d.changeColixArgb((short) id, argb);
      return;
    }
    id = Elements.altElementIndexFromNumber(id);
    altArgbsCpk[id] = argb;
    g3d.changeColixArgb((short) (Elements.elementNumberMax + id), argb);
  }

  ///////////////////  propertyColorScheme ///////////////

  float[] getPropertyColorRange() {
    if (propertyColorEncoder.isReversed)
      return new float[] { propertyColorEncoder.hi, propertyColorEncoder.lo };
    return new float[] { propertyColorEncoder.lo, propertyColorEncoder.hi };
  }

  void setPropertyColorRangeData(float[] data, BS bs, String colorScheme) {
    colorData = data;
    propertyColorEncoder.currentPalette = propertyColorEncoder.createColorScheme(
        colorScheme, true, false);
    propertyColorEncoder.hi = Float.MIN_VALUE;
    propertyColorEncoder.lo = Float.MAX_VALUE;
    if (data == null)
      return;
    boolean isAll = (bs == null);
    float d;
    int i0 = (isAll ? data.length - 1 : bs.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bs.nextSetBit(i + 1))) {
      if (Float.isNaN(d = data[i]))
        continue;
      propertyColorEncoder.hi = Math.max(propertyColorEncoder.hi, d);
      propertyColorEncoder.lo = Math.min(propertyColorEncoder.lo, d);
    }
    setPropertyColorRange(propertyColorEncoder.lo, propertyColorEncoder.hi);
  }

  void setPropertyColorRange(float min, float max) {
    propertyColorEncoder.setRange(min, max, min > max);
    Logger.info("ColorManager: color \""
        + propertyColorEncoder.getCurrentColorSchemeName() + "\" range " + min + " "
        + max);
  }

  void setPropertyColorScheme(String colorScheme, boolean isTranslucent,
                              boolean isOverloaded) {
    boolean isReset = (colorScheme.length() == 0);
    if (isReset)
      colorScheme = "="; // reset roygb
    float[] range = getPropertyColorRange();
    propertyColorEncoder.currentPalette = propertyColorEncoder.createColorScheme(
        colorScheme, true, isOverloaded);
    if (!isReset)
      setPropertyColorRange(range[0], range[1]);
    propertyColorEncoder.isTranslucent = isTranslucent;
  }

  void setUserScale(int[] scale) {
    propertyColorEncoder.setUserScale(scale);
  }

  String getColorSchemeList(String colorScheme) {
    // isosurface sets ifDefault FALSE so that any default schemes are returned
    int iPt = (colorScheme == null || colorScheme.length() == 0) ? propertyColorEncoder.currentPalette
        : propertyColorEncoder
            .createColorScheme(colorScheme, true, false);
    return ColorEncoder.getColorSchemeList(propertyColorEncoder
        .getColorSchemeArray(iPt));
  }

  short getColixForPropertyValue(float val) {
    return propertyColorEncoder.getColorIndex(val);
  }

  public ColorEncoder getColorEncoder(String colorScheme) {
    if (colorScheme == null || colorScheme.length() == 0)
      return propertyColorEncoder;
    ColorEncoder ce = new ColorEncoder(propertyColorEncoder);
    ce.currentPalette = ce.createColorScheme(colorScheme, false, true);
    return (ce.currentPalette == Integer.MAX_VALUE ? null : ce);
  }
}
