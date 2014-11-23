/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 16:04:16 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5304 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
import org.jmol.modelset.Atom;
import org.jmol.modelset.Chain;

public class CarbohydrateMonomer extends Monomer {

  /**
   * @j2sIgnoreSuperConstructor
   * 
   */
  private CarbohydrateMonomer() {
  }
  
  final static byte[] alphaOffsets = { 0 };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstIndex, int lastIndex) {
    return new CarbohydrateMonomer().set2(chain, group3, seqcode,
                            firstIndex, lastIndex, alphaOffsets);
  }
  
  ////////////////////////////////////////////////////////////////

  @Override
  public boolean isCarbohydrate() { return true; }

  @Override
  public EnumStructure getProteinStructureType() {
    return EnumStructure.CARBOHYDRATE;
  }

  @Override
  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    for (int i = firstAtomIndex; i <= lastAtomIndex; i++)
      for (int j = possiblyPreviousMonomer.firstAtomIndex; j <= possiblyPreviousMonomer.lastAtomIndex; j++) {
        Atom a = chain.getAtom(i);
        Atom b = chain.getAtom(j);
        if (a.getElementNumber() + b.getElementNumber() == 14
            && a.distanceSquared(b) < 3.24) // C and O; d < 1.8 (very generous)
          return true;
      }
    return false;
  }

  @Override
  void findNearestAtomIndex(int x, int y, Atom[] closest,
                            short madBegin, short madEnd) {    
    Atom competitor = closest[0];
    Atom anomericO = getLeadAtom();
    short marBegin = (short) (madBegin / 2);
    if (marBegin < 1200)
      marBegin = 1200;
    if (anomericO.screenZ == 0)
      return;
    int radiusBegin = scaleToScreen(anomericO.screenZ, marBegin);
    if (radiusBegin < 4)
      radiusBegin = 4;
    if (isCursorOnTopOf(anomericO, x, y, radiusBegin, competitor))
      closest[0] = anomericO;
  }
  
  @Override
  public boolean isConnectedPrevious() {
    if (monomerIndex <= 0)
      return false;
      for (int i = firstAtomIndex; i <= lastAtomIndex; i++) {
        // called by subclassed getCrossLinkAtomIndexes in
        // AminoMonomer, NucleicMonomer, and CarbohydrateMonomer
        if (getCrossLink(i, null))
          return true;
    }  
    return false;
  }

}
