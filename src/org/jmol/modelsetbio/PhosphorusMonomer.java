/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-15 11:45:59 -0600 (Thu, 15 Feb 2007) $
 * $Revision: 6834 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
import org.jmol.util.P3;
import org.jmol.util.Quaternion;
import org.jmol.util.V3;
import org.jmol.viewer.JC;

public class PhosphorusMonomer extends Monomer {

  protected final static byte P = 0;

  private final static byte[] phosphorusOffsets = { P };

  private static float MAX_ADJACENT_PHOSPHORUS_DISTANCE = 8.0f;
 
  protected boolean isPurine;
  protected boolean isPyrimidine;

  @Override
  public final boolean isNucleic() {return true;}

  /**
   * @j2sIgnoreSuperConstructor
   * 
   */
  protected PhosphorusMonomer() {}
  
  static Monomer validateAndAllocateP(Chain chain, String group3, int seqcode,
                                      int firstIndex, int lastIndex,
                                      int[] specialAtomIndexes) {
    //Logger.debug("PhosphorusMonomer.validateAndAllocate");
    return (firstIndex != lastIndex
        || specialAtomIndexes[JC.ATOMID_NUCLEIC_PHOSPHORUS] != firstIndex ? null
        : new PhosphorusMonomer().set3(chain, group3, seqcode, firstIndex,
            lastIndex, phosphorusOffsets));
  }
  
  ////////////////////////////////////////////////////////////////

  protected PhosphorusMonomer set3(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    set2(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
    if (group3.indexOf('T') >= 0)
      chain.isDna = true;
    if (group3.indexOf('U') + group3.indexOf('I') > -2)
        chain.isRna = true;
    isPurine = (group3.indexOf('A') + group3.indexOf('G') + group3.indexOf('I') > -3);
    isPyrimidine = (group3.indexOf('T') + group3.indexOf('C') + group3.indexOf('U') > -3);
    return this;
  }

  Atom getP() {
    return getAtomFromOffsetIndex(P);
  }

  boolean isPhosphorusMonomer() { return true; }

  @Override
  public boolean isDna() { return chain.isDna; }

  @Override
  public boolean isRna() { return chain.isRna; }

  @Override
  public boolean isPurine() { return isPurine; }
  @Override
  public boolean isPyrimidine() { return isPyrimidine; }

  @Override
  public Object getStructure() { return chain; }

  @Override
  public EnumStructure getProteinStructureType() {
    return EnumStructure.NONE;
  }

  @Override
  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    return isCA2(possiblyPreviousMonomer);
  }
  
  protected boolean isCA2(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    // 1PN8 73:d and 74:d are 7.001 angstroms apart
    // but some P atoms are up to 7.4 angstroms apart
    float distance =
      getLeadAtom().distance(possiblyPreviousMonomer.getLeadAtom());
    return distance <= MAX_ADJACENT_PHOSPHORUS_DISTANCE;
  }

  @Override
  public Quaternion getQuaternion(char qType) {
    return getQuaternionP();
  }
  
  protected Quaternion getQuaternionP() {
    //vA = ptP(i+1) - ptP
    //vB = ptP(i-1) - ptP
    int i = monomerIndex;
    if (i == 0 || i >= bioPolymer.monomerCount - 1)
      return null;
    P3 ptP = bioPolymer.monomers[i].getAtomFromOffsetIndex(P);
    P3 ptA, ptB;
    ptA = bioPolymer.monomers[i + 1].getAtomFromOffsetIndex(P);
    ptB = bioPolymer.monomers[i - 1].getAtomFromOffsetIndex(P);
    if (ptP == null || ptA == null || ptB == null)
      return null;
    V3 vA = new V3();
    V3 vB = new V3();
    vA.sub2(ptA, ptP);
    vB.sub2(ptB, ptP);
    return Quaternion.getQuaternionFrameV(vA, vB, null, false);
  }

  @Override
  P3 getQuaternionFrameCenter(char qType) {
    return getAtomFromOffsetIndex(P);
  }
  
  @Override
  public Object getHelixData(int tokType, char qType, int mStep) {
    return getHelixData2(tokType, qType, mStep);
  }
  

}
