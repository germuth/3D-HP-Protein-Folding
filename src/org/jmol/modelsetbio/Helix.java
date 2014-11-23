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

public class Helix extends ProteinStructure {

  Helix(AlphaPolymer apolymer, int monomerIndex, int monomerCount, EnumStructure subtype) {
    super(apolymer, EnumStructure.HELIX, monomerIndex,
        monomerCount);
    this.subtype = subtype;
  }

  @Override
  public void calcAxis() {
    if (axisA != null)
      return;
    P3[] points = new P3[monomerCount + 1];
    for (int i = 0; i <= monomerCount; i++) {
      points[i] = new P3();
      apolymer.getLeadMidPoint(monomerIndexFirst + i, points[i]);
    }
    axisA = new P3();
    axisUnitVector = new V3();
    Measure.calcBestAxisThroughPoints(points, axisA, axisUnitVector,
        vectorProjection, 4);
    axisB = P3.newP(points[monomerCount]);
    Measure.projectOntoAxis(axisB, axisA, axisUnitVector, vectorProjection);
  }

  /****************************************************************
   * see also: 
   * (not implemented -- I never got around to reading this -- BH)
   * Defining the Axis of a Helix
   * Peter C Kahn
   * Computers Chem. Vol 13, No 3, pp 185-189, 1989
   *
   * Simple Methods for Computing the Least Squares Line
   * in Three Dimensions
   * Peter C Kahn
   * Computers Chem. Vol 13, No 3, pp 191-195, 1989
   ****************************************************************/

}
