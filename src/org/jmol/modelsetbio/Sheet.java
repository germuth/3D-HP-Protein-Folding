/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-25 19:38:15 -0600 (Sun, 25 Feb 2007) $
 * $Revision: 6934 $
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
import org.jmol.util.Measure;
import org.jmol.util.P3;
import org.jmol.util.V3;

public class Sheet extends ProteinStructure {

  AlphaPolymer alphaPolymer;

  Sheet(AlphaPolymer alphaPolymer, int monomerIndex, int monomerCount, EnumStructure subtype) {
    super(alphaPolymer, EnumStructure.SHEET, monomerIndex,
        monomerCount);
    this.alphaPolymer = alphaPolymer;
    this.subtype = subtype;
  }

  @Override
  public void calcAxis() {
    if (axisA != null)
      return;
    if (monomerCount == 2) {
      axisA = alphaPolymer.getLeadPoint(monomerIndexFirst);
      axisB = alphaPolymer.getLeadPoint(monomerIndexFirst + 1);
    } else {
      axisA = new P3();
      alphaPolymer.getLeadMidPoint(monomerIndexFirst + 1, axisA);
      axisB = new P3();
      alphaPolymer.getLeadMidPoint(monomerIndexFirst + monomerCount - 1, axisB);
    }

    axisUnitVector = new V3();
    axisUnitVector.sub2(axisB, axisA);
    axisUnitVector.normalize();

    P3 tempA = new P3();
    alphaPolymer.getLeadMidPoint(monomerIndexFirst, tempA);
    if (lowerNeighborIsHelixOrSheet()) {
      //System.out.println("ok"); 
    } else {
      Measure
          .projectOntoAxis(tempA, axisA, axisUnitVector, vectorProjection);
    }
    P3 tempB = new P3();
    alphaPolymer.getLeadMidPoint(monomerIndexFirst + monomerCount, tempB);
    if (upperNeighborIsHelixOrSheet()) {
      //System.out.println("ok");       
    } else {
      Measure
          .projectOntoAxis(tempB, axisA, axisUnitVector, vectorProjection);
    }
    axisA = tempA;
    axisB = tempB;
  }

  V3 widthUnitVector;
  V3 heightUnitVector;

  void calcSheetUnitVectors() {
    if (!(alphaPolymer instanceof AminoPolymer))
      return;
    if (widthUnitVector == null) {
      V3 vectorCO = new V3();
      V3 vectorCOSum = new V3();
      AminoMonomer amino = (AminoMonomer) alphaPolymer.monomers[monomerIndexFirst];
      vectorCOSum.sub2(amino.getCarbonylOxygenAtom(), amino
          .getCarbonylCarbonAtom());
      for (int i = monomerCount; --i > monomerIndexFirst;) {
        amino = (AminoMonomer) alphaPolymer.monomers[i];
        vectorCO.sub2(amino.getCarbonylOxygenAtom(), amino
            .getCarbonylCarbonAtom());
        if (vectorCOSum.angle(vectorCO) < (float) Math.PI / 2)
          vectorCOSum.add(vectorCO);
        else
          vectorCOSum.sub(vectorCO);
      }
      heightUnitVector = vectorCO; // just reuse the same temp vector;
      heightUnitVector.cross(axisUnitVector, vectorCOSum);
      heightUnitVector.normalize();
      widthUnitVector = vectorCOSum;
      widthUnitVector.cross(axisUnitVector, heightUnitVector);
    }
  }

  public V3 getWidthUnitVector() {
    if (widthUnitVector == null)
      calcSheetUnitVectors();
    return widthUnitVector;
  }

  public V3 getHeightUnitVector() {
    if (heightUnitVector == null)
      calcSheetUnitVectors();
    return heightUnitVector;
  }
}
