/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-15 11:45:59 -0600 (Thu, 15 Feb 2007) $
 * $Revision: 6834 $
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
import org.jmol.util.P3;
import org.jmol.util.Quaternion;
import org.jmol.util.V3;
import org.jmol.viewer.JC;

public class AlphaMonomer extends Monomer {

  final static byte[] alphaOffsets = { 0 };

  @Override
  public boolean isProtein() {
    return true;
  }
  
  static Monomer
    validateAndAllocateA(Chain chain, String group3, int seqcode,
                        int firstIndex, int lastIndex,
                        int[] specialAtomIndexes) {
    return (firstIndex != lastIndex ||
        specialAtomIndexes[JC.ATOMID_ALPHA_CARBON] != firstIndex ? null : 
          new AlphaMonomer().set2(chain, group3, seqcode,
                            firstIndex, lastIndex, alphaOffsets));
  }
  
  ////////////////////////////////////////////////////////////////

  /**
   * @j2sIgnoreSuperConstructor
   * 
   */
  protected AlphaMonomer () {}
  
  boolean isAlphaMonomer() { return true; }

  protected ProteinStructure proteinStructure;
  protected P3 nitrogenHydrogenPoint;
  
  @Override
  public ProteinStructure getProteinStructure() { return proteinStructure; }

  @Override
  public Object getStructure() { return getProteinStructure(); }

  @Override
  void setStructure(ProteinStructure proteinStructure) {
    this.proteinStructure = proteinStructure;
    if (proteinStructure == null)
      nitrogenHydrogenPoint = null;
  }
  
  @Override
  public void setStrucNo(int n) {
    if (proteinStructure != null)
      proteinStructure.strucNo = n;
  }

  @Override
  public EnumStructure getProteinStructureType() {
    return proteinStructure == null ? EnumStructure.NONE
        : proteinStructure.type;
  }

  @Override
  public EnumStructure getProteinStructureSubType() {
    return proteinStructure == null ? EnumStructure.NONE
        : proteinStructure.subtype;
  }

  @Override
  public int getStrucNo() {
    return proteinStructure != null ? proteinStructure.strucNo : 0;
  }

  @Override
  public boolean isHelix() {
    return proteinStructure != null &&
      proteinStructure.type == EnumStructure.HELIX;
  }

  @Override
  public boolean isSheet() {
    return proteinStructure != null &&
      proteinStructure.type == EnumStructure.SHEET;
  }

  /**
   * 
   * @param type
   * @param monomerIndexCurrent   a pointer to the current ProteinStructure
   * @return                      a pointer to this ProteinStructure
   */
  @SuppressWarnings("incomplete-switch")
  @Override
  public int setProteinStructureType(EnumStructure type, int monomerIndexCurrent) {
    if (monomerIndexCurrent < 0 
        || monomerIndexCurrent > 0 && monomerIndex == 0) {
      if (proteinStructure != null) {
        int nAbandoned = proteinStructure.removeMonomer(monomerIndex);
        if (nAbandoned > 0)
          getBioPolymer().removeProteinStructure(monomerIndex + 1, nAbandoned);
      }
      switch (type) {
      case HELIX:
      case HELIXALPHA:
      case HELIX310:
      case HELIXPI:
        setStructure(new Helix((AlphaPolymer) bioPolymer, monomerIndex, 1, type));
        break;
      case SHEET:
        setStructure(new Sheet((AlphaPolymer) bioPolymer, monomerIndex, 1, type));
        break;
      case TURN:
        setStructure(new Turn((AlphaPolymer) bioPolymer, monomerIndex, 1));
        break;
      case NONE:
        setStructure(null);
      }
    } else {
      setStructure(getBioPolymer().getProteinStructure(monomerIndexCurrent));
      if (proteinStructure != null)
        proteinStructure.addMonomer(monomerIndex);
    }
    return monomerIndex;
  }
  
  final public Atom getAtom(byte specialAtomID) {
    return (specialAtomID == JC.ATOMID_ALPHA_CARBON
            ? getLeadAtom()
            : null);
  }

  final public P3 getAtomPoint(byte specialAtomID) {
    return (specialAtomID == JC.ATOMID_ALPHA_CARBON
            ? getLeadAtom()
            : null);
  }

  @Override
  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    Atom atom1 = getLeadAtom();
    Atom atom2 = possiblyPreviousMonomer.getLeadAtom();
    return atom1.isBonded(atom2) || atom1.distance(atom2) <= 4.2f;
    // jan reichert in email to miguel on 10 May 2004 said 4.2 looked good
  }
  
  @Override
  P3 getQuaternionFrameCenter(char qType) {
    return getQuaternionFrameCenterAlpha(qType);
  }

  protected P3 getQuaternionFrameCenterAlpha(char qType) {
    switch (qType) {
    case 'b':
    case 'c':
    case 'C': // ramachandran
    case 'x':
      return getLeadAtom();
    default:
    case 'a':
    case 'n':
    case 'p':
    case 'P': // ramachandran
    case 'q': // Quine
      return null;
    }
  }

  @Override
  public Object getHelixData(int tokType, char qType, int mStep) {
    return getHelixData2(tokType, qType, mStep);
  }
  
  @Override
  public Quaternion getQuaternion(char qType) {
    return getQuaternionAlpha(qType);
  }

  protected Quaternion getQuaternionAlpha(char qType) {
    /*
     * also NucleicMonomer, AminoMonomer
     * 
     * This definition is only for alpha-only chains
     *   
     */
    
    V3 vA = new V3();
    V3 vB = new V3();
    V3 vC = null;

    switch (qType) {
    default:
    case 'a':
    case 'n':
    case 'p':
    case 'q':
      return null;
    case 'b':
    case 'c':
    case 'x':
      //vA = ptCa(i+1) - ptCa
      //vB = ptCa(i-1) - ptCa
      if (monomerIndex == 0 
          || monomerIndex == bioPolymer.monomerCount - 1)
        return null;
      P3 ptCa = getLeadAtom();
      P3 ptCaNext = bioPolymer.getLeadPoint(monomerIndex + 1);
      P3 ptCaPrev = bioPolymer.getLeadPoint(monomerIndex - 1);
      vA.sub2(ptCaNext, ptCa);
      vB.sub2(ptCaPrev, ptCa);
      break;
    }
    return Quaternion.getQuaternionFrameV(vA, vB, vC, false);
  }
  

}
