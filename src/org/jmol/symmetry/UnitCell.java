/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package org.jmol.symmetry;

import org.jmol.util.BoxInfo;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.Quadric;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tuple3f;

/**
 * a class private to the org.jmol.symmetry package
 * to be accessed only through the SymmetryInterface API
 * 
 * adds vertices and offsets orientation, 
 * and a variety of additional calculations that in 
 * principle could be put in SimpleUnitCell
 * if desired, but for now are in this optional package.
 * 
 */

class UnitCell extends SimpleUnitCell {
  
  private P3[] vertices; // eight corners
  private P3 cartesianOffset = new P3();
  private P3 fractionalOffset = new P3();
  
  UnitCell() {
    
  }
  
  static UnitCell newP(Tuple3f[] points) {
    UnitCell c = new UnitCell();
    float[] parameters = new float[] { -1, 0, 0, 0, 0, 0, points[1].x,
        points[1].y, points[1].z, points[2].x, points[2].y, points[2].z,
        points[3].x, points[3].y, points[3].z };
    c.set(parameters);
    c.allFractionalRelative = true;
    c.calcUnitcellVertices();
    c.setCartesianOffset(points[0]);
    return c;
  }
  
  public static UnitCell newA(float[] notionalUnitcell) {
    UnitCell c = new UnitCell();
    c.set(notionalUnitcell);
    c.calcUnitcellVertices();
    return  c;
  }

  void setOrientation(Matrix3f mat) {
    if (mat == null)
      return;
    Matrix4f m = new Matrix4f();
    m.setM3(mat);
    matrixFractionalToCartesian.mul2(m, matrixFractionalToCartesian);
    matrixCartesianToFractional.invertM(matrixFractionalToCartesian);
    calcUnitcellVertices();
  }

  /**
   * when offset is null, 
   * @param pt
   * @param offset
   */
  final void toUnitCell(P3 pt, P3 offset) {
    if (matrixCartesianToFractional == null)
      return;
    if (offset == null) {
      // used redefined unitcell 
      matrixCartesianToFractional.transform(pt);
      switch (dimension) {
      case 3:
        pt.z = toFractional(pt.z);  
        //$FALL-THROUGH$
      case 2:
        pt.y = toFractional(pt.y);
        //$FALL-THROUGH$
      case 1:
        pt.x = toFractional(pt.x);
      }
      matrixFractionalToCartesian.transform(pt);
    } else {
      // use original unit cell
      matrixCtoFAbsolute.transform(pt);
      switch (dimension) {
      case 3:
        pt.z = toFractional(pt.z);  
        //$FALL-THROUGH$
      case 2:
        pt.y = toFractional(pt.y);
        //$FALL-THROUGH$
      case 1:
        pt.x = toFractional(pt.x);
      }
      pt.add(offset);      
      matrixFtoCAbsolute.transform(pt);
    }
  }
  
  private boolean allFractionalRelative = false;
  private P3 unitCellMultiplier = null;
  
  void setAllFractionalRelative(boolean TF) {
    allFractionalRelative = TF;
  }  
  
  void setOffset(P3 pt) {
    if (pt == null)
      return;
    // from "unitcell {i j k}" via uccage
    if (pt.x >= 100 || pt.y >= 100) {
      unitCellMultiplier = P3.newP(pt);
      return;
    }
    if (pt.x == 0 && pt.y == 0 && pt.z == 0)
      unitCellMultiplier = null;
    fractionalOffset.setT(pt);
    matrixCartesianToFractional.m03 = -pt.x;
    matrixCartesianToFractional.m13 = -pt.y;
    matrixCartesianToFractional.m23 = -pt.z;
    cartesianOffset.setT(pt);
    matrixFractionalToCartesian.m03 = 0;
    matrixFractionalToCartesian.m13 = 0;
    matrixFractionalToCartesian.m23 = 0;
    matrixFractionalToCartesian.transform(cartesianOffset);
    matrixFractionalToCartesian.m03 = cartesianOffset.x;
    matrixFractionalToCartesian.m13 = cartesianOffset.y;
    matrixFractionalToCartesian.m23 = cartesianOffset.z;
    if (allFractionalRelative) {
      matrixCtoFAbsolute.setM(matrixCartesianToFractional);
      matrixFtoCAbsolute.setM(matrixFractionalToCartesian);
    }
  }

  public void setCartesianOffset(Tuple3f origin) {
    cartesianOffset.setT(origin);
    matrixFractionalToCartesian.m03 = cartesianOffset.x;
    matrixFractionalToCartesian.m13 = cartesianOffset.y;
    matrixFractionalToCartesian.m23 = cartesianOffset.z;
    fractionalOffset.setT(cartesianOffset);
    matrixCartesianToFractional.m03 = 0;
    matrixCartesianToFractional.m13 = 0;
    matrixCartesianToFractional.m23 = 0;
    matrixCartesianToFractional.transform(fractionalOffset);
    matrixCartesianToFractional.m03 = -fractionalOffset.x;
    matrixCartesianToFractional.m13 = -fractionalOffset.y;
    matrixCartesianToFractional.m23 = -fractionalOffset.z;
    if (allFractionalRelative) {
      matrixCtoFAbsolute.setM(matrixCartesianToFractional);
      matrixFtoCAbsolute.setM(matrixFractionalToCartesian);
    }
  }

  void setMinMaxLatticeParameters(P3i minXYZ, P3i maxXYZ) {
    if (maxXYZ.x <= 555 && maxXYZ.y >= 555) {
      //alternative format for indicating a range of cells:
      //{111 666}
      //555 --> {0 0 0}
      P3 pt = new P3();
      ijkToPoint3f(maxXYZ.x, pt, 0);
      minXYZ.x = (int) pt.x;
      minXYZ.y = (int) pt.y;
      minXYZ.z = (int) pt.z;
      ijkToPoint3f(maxXYZ.y, pt, 1);
      //555 --> {1 1 1}
      maxXYZ.x = (int) pt.x;
      maxXYZ.y = (int) pt.y;
      maxXYZ.z = (int) pt.z;
    }
    switch (dimension) {
    case 1: // polymer
      minXYZ.y = 0;
      maxXYZ.y = 1;
      //$FALL-THROUGH$
    case 2: // slab
      minXYZ.z = 0;
      maxXYZ.z = 1;
    }
  }

  final String dumpInfo(boolean isFull) {
    return "a=" + a + ", b=" + b + ", c=" + c + ", alpha=" + alpha + ", beta=" + beta + ", gamma=" + gamma
       + (isFull ? "\nfractional to cartesian: " + matrixFractionalToCartesian 
       + "\ncartesian to fractional: " + matrixCartesianToFractional : "");
  }

  P3[] getVertices() {
    return vertices; // does not include offsets
  }
  
  P3 getCartesianOffset() {
    // for slabbing isosurfaces and rendering the ucCage
    return cartesianOffset;
  }
  
  P3 getFractionalOffset() {
    // no references??
    return fractionalOffset;
  }
  
  final private static double twoP2 = 2 * Math.PI * Math.PI;
  
  Quadric getEllipsoid(float[] parBorU) {
    if (parBorU == null)
      return null;
    /*
     * 
     * returns {Vector3f[3] unitVectors, float[3] lengths} from J.W. Jeffery,
     * Methods in X-Ray Crystallography, Appendix VI, Academic Press, 1971
     * 
     * comparing with Fischer and Tillmanns, Acta Cryst C44 775-776, 1988, these
     * are really BETA values. Note that
     * 
     * T = exp(-2 pi^2 (a*b* U11h^2 + b*b* U22k^2 + c*c* U33l^2 + 2 a*b* U12hk +
     * 2 a*c* U13hl + 2 b*c* U23kl))
     * 
     * (ORTEP type 8) is the same as
     * 
     * T = exp{-2 pi^2^ sum~i~[sum~j~(U~ij~ h~i~ h~j~ a*~i~ a*~j~)]}
     * 
     * http://ndbserver.rutgers.edu/mmcif/dictionaries/html/cif_mm.dic/Items/
     * _atom_site.aniso_u[1][2].html
     * 
     * Ortep: http://www.ornl.gov/sci/ortep/man_pdf.html
     * 
     * Anisotropic temperature factor Types 0, 1, 2, 3, and 10 use the following
     * formula for the complete temperature factor.
     * 
     * Base^(-D(b11h2 + b22k2 + b33l2 + cb12hk + cb13hl + cb23kl))
     * 
     * The coefficients bij (i,j = 1,2,3) of the various types are defined with
     * the following constant settings.
     * 
     * Type 0: Base = e, c = 2, D = 1 Type 1: Base = e, c = 1, D = l Type 2:
     * Base = 2, c = 2, D = l Type 3: Base = 2, c = 1, D = l
     * 
     * Anisotropic temperature factor Types 4, 5, 8, and 9 use the following
     * formula for the complete temperature factor, in which a1* , a2*, a3* are
     * reciprocal cell dimensions.
     * 
     * exp[ -D(a1*a1*U11hh + a2*a2*U22kk + a3*a3*U33ll + C a1*a2*U12hk + C a1*a3
     * * U13hl + C a2*a3 * U23kl)]
     * 
     * The coefficients Uij (i,j = 1,2,3) of the various types are defined with
     * the following constant settings.
     * 
     * Type 4: C = 2, D = 1/4 Type 5: C = 1, D = 1/4 Type 8: C = 2, D = 2pi2
     * Type 9: C = 1, D = 2pi2
     * 
     * 
     * For beta, we use definitions at
     * http://www.iucr.org/iucr-top/comm/cnom/adp/finrepone/finrepone.html
     * 
     * that betaij = 2pi^2ai*aj* Uij
     * 
     * So if Type 8 is
     * 
     * exp[ -2pi^2(a1*a1*U11hh + a2*a2*U22kk + a3*a3*U33ll + 2a1*a2*U12hk +
     * 2a1*a3 * U13hl + 2a2*a3 * U23kl)]
     * 
     * then we have
     * 
     * exp[ -pi^2(beta11hh + beta22kk + beta33ll + 2beta12hk + 2beta13hl +
     * 2beta23kl)]
     * 
     * and the betaij should be entered as Type 0.
     */

    if (parBorU[0] == 0) { // this is iso
      float[] lengths = new float[3];
      lengths[0] = lengths[1] = lengths[2] = (float) Math.sqrt(parBorU[7]);
      return new Quadric(null, lengths, true);
    }

    double[] Bcart = new double[6];

    int ortepType = (int) parBorU[6];

    if (ortepType == 12) {
      // macromolecular Cartesian

      Bcart[0] = parBorU[0] * twoP2;
      Bcart[1] = parBorU[1] * twoP2;
      Bcart[2] = parBorU[2] * twoP2;
      Bcart[3] = parBorU[3] * twoP2 * 2;
      Bcart[4] = parBorU[4] * twoP2 * 2;
      Bcart[5] = parBorU[5] * twoP2 * 2;

      parBorU[7] = (parBorU[0] + parBorU[1] + parBorU[3]) / 3;

    } else {

      boolean isFractional = (ortepType == 4 || ortepType == 5
          || ortepType == 8 || ortepType == 9);
      double cc = 2 - (ortepType % 2);
      double dd = (ortepType == 8 || ortepType == 9 || ortepType == 10 ? twoP2
          : ortepType == 4 || ortepType == 5 ? 0.25 
          : ortepType == 2 || ortepType == 3 ? Math.log(2) 
          : 1);
      // types 6 and 7 not supported

      //System.out.println("ortep type " + ortepType + " isFractional=" +
      // isFractional + " D = " + dd + " C=" + cc);
      double B11 = parBorU[0] * dd * (isFractional ? a_ * a_ : 1);
      double B22 = parBorU[1] * dd * (isFractional ? b_ * b_ : 1);
      double B33 = parBorU[2] * dd * (isFractional ? c_ * c_ : 1);
      double B12 = parBorU[3] * dd * (isFractional ? a_ * b_ : 1) * cc;
      double B13 = parBorU[4] * dd * (isFractional ? a_ * c_ : 1) * cc;
      double B23 = parBorU[5] * dd * (isFractional ? b_ * c_ : 1) * cc;

      // set bFactor = (U11*U22*U33)
      parBorU[7] = (float) Math.pow(B11 / twoP2 / a_ / a_ * B22 / twoP2 / b_
          / b_ * B33 / twoP2 / c_ / c_, 0.3333);

      Bcart[0] = a * a * B11 + b * b * cosGamma * cosGamma * B22 + c * c
          * cosBeta * cosBeta * B33 + a * b * cosGamma * B12 + b * c * cosGamma
          * cosBeta * B23 + a * c * cosBeta * B13;
      Bcart[1] = b * b * sinGamma * sinGamma * B22 + c * c * cA_ * cA_ * B33
          + b * c * cA_ * sinGamma * B23;
      Bcart[2] = c * c * cB_ * cB_ * B33;
      Bcart[3] = 2 * b * b * cosGamma * sinGamma * B22 + 2 * c * c * cA_
          * cosBeta * B33 + a * b * sinGamma * B12 + b * c
          * (cA_ * cosGamma + sinGamma * cosBeta) * B23 + a * c * cA_ * B13;
      Bcart[4] = 2 * c * c * cB_ * cosBeta * B33 + b * c * cosGamma * B23 + a
          * c * cB_ * B13;
      Bcart[5] = 2 * c * c * cA_ * cB_ * B33 + b * c * cB_ * sinGamma * B23;

    }

    //System.out.println("UnitCell Bcart=" + Bcart[0] + " " + Bcart[1] + " "
      //  + Bcart[2] + " " + Bcart[3] + " " + Bcart[4] + " " + Bcart[5]);
    return new Quadric(Bcart);
  }
  
  P3[] getCanonicalCopy(float scale) {
    P3[] pts = new P3[8];
    for (int i = 0; i < 8; i++) {
      pts[i] = P3.newP(BoxInfo.unitCubePoints[i]);
      matrixFractionalToCartesian.transform(pts[i]);
      //pts[i].add(cartesianOffset);
    }
    return BoxInfo.getCanonicalCopy(pts, scale);
  }

  /// private methods
  
  
  private static float toFractional(float x) {
    // introduced in Jmol 11.7.36
    x = (float) (x - Math.floor(x));
    if (x > 0.9999f || x < 0.0001f) 
      x = 0;
    return x;
  }
  
  private void calcUnitcellVertices() {
    if (matrixFractionalToCartesian == null)
      return;
    matrixCtoFAbsolute = Matrix4f.newM(matrixCartesianToFractional);
    matrixFtoCAbsolute = Matrix4f.newM(matrixFractionalToCartesian);
    vertices = new P3[8];
    for (int i = 8; --i >= 0;) {
      vertices[i] = new P3(); 
      matrixFractionalToCartesian.transform2(BoxInfo.unitCubePoints[i], vertices[i]);
      //System.out.println("UNITCELL " + vertices[i] + " " + BoxInfo.unitCubePoints[i]);
    }
  }

  /**
   * 
   * @param f1
   * @param f2
   * @param distance
   * @param dx
   * @param iRange
   * @param jRange
   * @param kRange
   * @param ptOffset TODO
   * @return       TRUE if pt has been set.
   */
  public boolean checkDistance(P3 f1, P3 f2, float distance, float dx,
                              int iRange, int jRange, int kRange, P3 ptOffset) {
    P3 p1 = P3.newP(f1);
    toCartesian(p1, true);
    for (int i = -iRange; i <= iRange; i++)
      for (int j = -jRange; j <= jRange; j++)
        for (int k = -kRange; k <= kRange; k++) {
          ptOffset.set(f2.x + i, f2.y + j, f2.z + k);
          toCartesian(ptOffset, true);
          float d = p1.distance(ptOffset);
          if (dx > 0 ? Math.abs(d - distance) <= dx : d <= distance && d > 0.1f) {
            ptOffset.set(i, j, k);
            return true;
          }
        }
    return false;
  }

  public P3 getUnitCellMultiplier() {
    return unitCellMultiplier ;
  }

  public P3[] getUnitCellVectors() {
    Matrix4f m = matrixFractionalToCartesian;
    return new P3[] { 
        P3.newP(cartesianOffset),
        P3.new3(m.m00, m.m10, m.m20), 
        P3.new3(m.m01, m.m11, m.m21), 
        P3.new3(m.m02, m.m12, m.m22) };
  }

}
