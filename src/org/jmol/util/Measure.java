/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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
package org.jmol.util;


import org.jmol.util.JmolList;



import org.jmol.viewer.JC;
import org.jmol.modelset.Atom;
import org.jmol.script.T;

final public class Measure {

  public final static float radiansPerDegree = (float) (2 * Math.PI / 360);
  
  public static float computeAngle(Tuple3f pointA, Tuple3f pointB, Tuple3f pointC, V3 vectorBA, V3 vectorBC, boolean asDegrees) {
    vectorBA.sub2(pointA, pointB);
    vectorBC.sub2(pointC, pointB);
    float angle = vectorBA.angle(vectorBC);
    return (asDegrees ? angle / radiansPerDegree : angle);
  }

  public static float computeAngleABC(Tuple3f pointA, Tuple3f pointB, Tuple3f pointC, boolean asDegrees) {
    V3 vectorBA = new V3();
    V3 vectorBC = new V3();        
    return computeAngle(pointA, pointB, pointC, vectorBA, vectorBC, asDegrees);
  }

  public static float computeTorsion(Tuple3f p1, Tuple3f p2, Tuple3f p3, Tuple3f p4, boolean asDegrees) {
  
    float ijx = p1.x - p2.x;
    float ijy = p1.y - p2.y;
    float ijz = p1.z - p2.z;
  
    float kjx = p3.x - p2.x;
    float kjy = p3.y - p2.y;
    float kjz = p3.z - p2.z;
  
    float klx = p3.x - p4.x;
    float kly = p3.y - p4.y;
    float klz = p3.z - p4.z;
  
    float ax = ijy * kjz - ijz * kjy;
    float ay = ijz * kjx - ijx * kjz;
    float az = ijx * kjy - ijy * kjx;
    float cx = kjy * klz - kjz * kly;
    float cy = kjz * klx - kjx * klz;
    float cz = kjx * kly - kjy * klx;
  
    float ai2 = 1f / (ax * ax + ay * ay + az * az);
    float ci2 = 1f / (cx * cx + cy * cy + cz * cz);
  
    float ai = (float) Math.sqrt(ai2);
    float ci = (float) Math.sqrt(ci2);
    float denom = ai * ci;
    float cross = ax * cx + ay * cy + az * cz;
    float cosang = cross * denom;
    if (cosang > 1) {
      cosang = 1;
    }
    if (cosang < -1) {
      cosang = -1;
    }
  
    float torsion = (float) Math.acos(cosang);
    float dot = ijx * cx + ijy * cy + ijz * cz;
    float absDot = Math.abs(dot);
    torsion = (dot / absDot > 0) ? torsion : -torsion;
    return (asDegrees ? torsion / radiansPerDegree : torsion);
  }

  public static Object computeHelicalAxis(String id, int tokType, P3 a, P3 b,
                                    Quaternion dq) {
    /*
                b
           |   /|
           |  / |
           | /  |
           |/   c
         b'+   / \
           |  /   \      Vcb = Vab . n
         n | /     \d    Vda = (Vcb - Vab) / 2
           |/theta  \
         a'+---------a
                r 
    */
    
    V3 vab = new V3();
    vab.sub2(b, a);
    /*
     * testing here to see if directing the normal makes any difference -- oddly
     * enough, it does not. When n = -n and theta = -theta vab.n is reversed,
     * and that magnitude is multiplied by n in generating the A'-B' vector.
     * 
     * a negative angle implies a left-handed axis (sheets)
     */
    float theta = dq.getTheta();
    V3 n = dq.getNormal();
    float v_dot_n = vab.dot(n);
    if (Math.abs(v_dot_n) < 0.0001f)
      v_dot_n = 0;
    if (tokType == T.axis) {
      if (v_dot_n != 0)
        n.scale(v_dot_n);
      return n;
    }
    V3 va_prime_d = new V3();
    va_prime_d.cross(vab, n);
    if (va_prime_d.dot(va_prime_d) != 0)
      va_prime_d.normalize();
    V3 vda = new V3();
    V3 vcb = V3.newV(n);
    if (v_dot_n == 0)
      v_dot_n = Float.MIN_VALUE; // allow for perpendicular axis to vab
    vcb.scale(v_dot_n);
    vda.sub2(vcb, vab);
    vda.scale(0.5f);
    va_prime_d.scale(theta == 0 ? 0 : (float) (vda.length() / Math
        .tan(theta / 2 / 180 * Math.PI)));
    V3 r = V3.newV(va_prime_d);
    if (theta != 0)
      r.add(vda);
    if (tokType == T.radius)
      return r;
    P3 pt_a_prime = P3.newP(a);
    pt_a_prime.sub(r);
    if (tokType == T.point) {
      return pt_a_prime;
    }
    if (v_dot_n != Float.MIN_VALUE)
      n.scale(v_dot_n);
    // must calculate directed angle:
    P3 pt_b_prime = P3.newP(pt_a_prime);
    pt_b_prime.add(n);
    theta = computeTorsion(a, pt_a_prime, pt_b_prime, b, true);
    if (Float.isNaN(theta) || r.length() < 0.0001f)
      theta = dq.getThetaDirectedV(n); // allow for r = 0
    if (tokType == T.angle)
      return Float.valueOf(theta);
    /*
    System.out.println("draw ID test VECTOR " + Escape.escape(pt_a_prime)
          + " " + Escape.escape(n) + " color "
          + (theta < 0 ? "{255.0 200.0 0.0}" : "{255.0 0.0 128.0};")
          +"measure " + Escape.escape(a) + Escape.escape(pt_a_prime) + Escape.escape(pt_b_prime) + Escape.escape(b));
    */
    if (tokType == T.draw)
      return "draw ID \"" + id + "\" VECTOR " + Escape.eP(pt_a_prime)
          + " " + Escape.eP(n) + " color "
          + (theta < 0 ? "{255.0 200.0 0.0}" : "{255.0 0.0 128.0}");
    if (tokType == T.measure)
      return "measure " + Escape.eP(a) + Escape.eP(pt_a_prime) + Escape.eP(pt_b_prime) + Escape.eP(b);
    // for now... array:
    float residuesPerTurn = Math.abs(theta == 0 ? 0 : 360f / theta);
    float pitch = Math.abs(v_dot_n == Float.MIN_VALUE ? 0 : n.length() * (theta == 0 ? 1 : 360f / theta));
    switch (tokType) {
    case T.array:
      return new Object[] {pt_a_prime, n, r,  P3.new3(theta, pitch, residuesPerTurn)};
    case T.list:
      return new String[] { 
          Escape.eP(pt_a_prime), // a' 
          Escape.eP(n), // n
          Escape.eP(r), // r
          Escape.eP(P3.new3(theta /*(degrees)*/,pitch, residuesPerTurn))
          };
    default:
      return null;
    }
  }

  public static void getPlaneThroughPoints(P3 pointA,
                                              P3 pointB,
                                              P3 pointC, V3 vNorm,
                                              V3 vAB, V3 vAC, Point4f plane) {
    float w = getNormalThroughPoints(pointA, pointB, pointC, vNorm, vAB, vAC);
    plane.set(vNorm.x, vNorm.y, vNorm.z, w);
  }
  
  public static void getPlaneThroughPoint(P3 pt, V3 normal, Point4f plane) {
    plane.set(normal.x, normal.y, normal.z, -normal.dot(V3.newV(pt)));
  }
  
  public static float distanceToPlane(Point4f plane, P3 pt) {
    return (plane == null ? Float.NaN 
        : (plane.x * pt.x + plane.y * pt.y + plane.z * pt.z + plane.w)
        / (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z
            * plane.z));
  }

  public static float distanceToPlaneD(Point4f plane, float d, P3 pt) {
    return (plane == null ? Float.NaN : (plane.x * pt.x + plane.y
        * pt.y + plane.z * pt.z + plane.w) / d);
  }

  public static float distanceToPlaneV(V3 norm, float w, P3 pt) {
    return (norm == null ? Float.NaN 
        : (norm.x * pt.x + norm.y * pt.y + norm.z * pt.z + w)
        / (float) Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z
            * norm.z));
  }

  public static void calcNormalizedNormal(P3 pointA, P3 pointB,
         P3 pointC, V3 vNormNorm, V3 vAB, V3 vAC) {
    vAB.sub2(pointB, pointA);
    vAC.sub2(pointC, pointA);
    vNormNorm.cross(vAB, vAC);
    vNormNorm.normalize();
  }

  public static float getDirectedNormalThroughPoints(P3 pointA, 
         P3 pointB, P3 pointC, P3 ptRef, V3 vNorm, 
         V3 vAB, V3 vAC) {
    // for x = plane({atomno=1}, {atomno=2}, {atomno=3}, {atomno=4})
    float nd = getNormalThroughPoints(pointA, pointB, pointC, vNorm, vAB, vAC);
    if (ptRef != null) {
      P3 pt0 = P3.newP(pointA);
      pt0.add(vNorm);
      float d = pt0.distance(ptRef);
      pt0.setT(pointA);
      pt0.sub(vNorm);
      if (d > pt0.distance(ptRef)) {
        vNorm.scale(-1);
        nd = -nd;
      }
    }
    return nd;
  }
  
  public static float getNormalThroughPoints(P3 pointA, P3 pointB,
                                   P3 pointC, V3 vNorm, V3 vAB, V3 vAC) {
    // for Polyhedra
    calcNormalizedNormal(pointA, pointB, pointC, vNorm, vAB, vAC);
    // ax + by + cz + d = 0
    // so if a point is in the plane, then N dot X = -d
    vAB.setT(pointA);
    return -vAB.dot(vNorm);
  }

  public static void getPlaneProjection(P3 pt, Point4f plane, P3 ptProj, V3 vNorm) {
    float dist = distanceToPlane(plane, pt);
    vNorm.set(plane.x, plane.y, plane.z);
    vNorm.normalize();
    vNorm.scale(-dist);
    ptProj.setT(pt);
    ptProj.add(vNorm);
  }

  /**
   * 
   * @param ptCenter
   * @param ptA
   * @param ptB
   * @param ptC
   * @param isOutward
   * @param normal
   * @return        true if winding is proper; false if not
   */
  public static boolean getNormalFromCenter(P3 ptCenter, P3 ptA, P3 ptB,
                            P3 ptC, boolean isOutward, V3 normal) {
    // for Polyhedra
    V3 vAB = new V3();
    V3 vAC = new V3();
    float d = getNormalThroughPoints(ptA, ptB, ptC, normal, vAB, vAC);
    boolean isReversed = (distanceToPlaneV(normal, d, ptCenter) > 0);
    if (isReversed == isOutward)
      normal.scale(-1f);
    //System.out.println("Draw v vector scale 2.0 " + Escape.escape(ptCenter) + Escape.escape(normal));
    return !isReversed;
  }

  public static void getNormalToLine(P3 pointA, P3 pointB,
                                   V3 vNormNorm) {
    // vector in xy plane perpendicular to a line between two points RMH
    vNormNorm.sub2(pointA, pointB);
    vNormNorm.cross(vNormNorm, JC.axisY);
    vNormNorm.normalize();
    if (Float.isNaN(vNormNorm.x))
      vNormNorm.set(1, 0, 0);
  }
  
  public static void getBisectingPlane(P3 pointA, V3 vAB,
                                                 P3 ptTemp, V3 vTemp, Point4f plane) {
    ptTemp.scaleAdd2(0.5f, vAB, pointA);
    vTemp.setT(vAB);
    vTemp.normalize();
    getPlaneThroughPoint(ptTemp, vTemp, plane);
    }
    
  public static void projectOntoAxis(P3 point, P3 axisA,
                                     V3 axisUnitVector,
                                     V3 vectorProjection) {
    vectorProjection.sub2(point, axisA);
    float projectedLength = vectorProjection.dot(axisUnitVector);
    point.setT(axisUnitVector);
    point.scaleAdd(projectedLength, axisA);
    vectorProjection.sub2(point, axisA);
  }
  
  public static void calcBestAxisThroughPoints(P3[] points, P3 axisA,
                                               V3 axisUnitVector,
                                               V3 vectorProjection,
                                               int nTriesMax) {
    // just a crude starting point.

    int nPoints = points.length;
    axisA.setT(points[0]);
    axisUnitVector.sub2(points[nPoints - 1], axisA);
    axisUnitVector.normalize();

    /*
     * We now calculate the least-squares 3D axis
     * through the helix alpha carbons starting with Vo
     * as a first approximation.
     * 
     * This uses the simple 0-centered least squares fit:
     * 
     * Y = M cross Xi
     * 
     * minimizing R^2 = SUM(|Y - Yi|^2) 
     * 
     * where Yi is the vector PERPENDICULAR of the point onto axis Vo
     * and Xi is the vector PROJECTION of the point onto axis Vo
     * and M is a vector adjustment 
     * 
     * M = SUM_(Xi cross Yi) / sum(|Xi|^2)
     * 
     * from which we arrive at:
     * 
     * V = Vo + (M cross Vo)
     * 
     * Basically, this is just a 3D version of a 
     * standard 2D least squares fit to a line, where we would say:
     * 
     * y = m xi + b
     * 
     * D = n (sum xi^2) - (sum xi)^2
     * 
     * m = [(n sum xiyi) - (sum xi)(sum yi)] / D
     * b = [(sum yi) (sum xi^2) - (sum xi)(sum xiyi)] / D
     * 
     * but here we demand that the line go through the center, so we
     * require (sum xi) = (sum yi) = 0, so b = 0 and
     * 
     * m = (sum xiyi) / (sum xi^2)
     * 
     * In 3D we do the same but 
     * instead of x we have Vo,
     * instead of multiplication we use cross products
     * 
     * A bit of iteration is necessary.
     * 
     * Bob Hanson 11/2006
     * 
     */

    calcAveragePointN(points, nPoints, axisA);

    int nTries = 0;
    while (nTries++ < nTriesMax
        && findAxis(points, nPoints, axisA, axisUnitVector, vectorProjection) > 0.001) {
    }

    /*
     * Iteration here gets the job done.
     * We now find the projections of the endpoints onto the axis
     * 
     */

    P3 tempA = P3.newP(points[0]);
    projectOntoAxis(tempA, axisA, axisUnitVector, vectorProjection);
    axisA.setT(tempA);
  }

  public static float findAxis(P3[] points, int nPoints, P3 axisA,
                        V3 axisUnitVector, V3 vectorProjection) {
    V3 sumXiYi = new V3();
    V3 vTemp = new V3();
    P3 pt = new P3();
    P3 ptProj = new P3();
    V3 a = V3.newV(axisUnitVector);

    float sum_Xi2 = 0;
    float sum_Yi2 = 0;
    for (int i = nPoints; --i >= 0;) {
      pt.setT(points[i]);
      ptProj.setT(pt);
      projectOntoAxis(ptProj, axisA, axisUnitVector,
          vectorProjection);
      vTemp.sub2(pt, ptProj);
      sum_Yi2 += vTemp.lengthSquared();
      vTemp.cross(vectorProjection, vTemp);
      sumXiYi.add(vTemp);
      sum_Xi2 += vectorProjection.lengthSquared();
    }
    V3 m = V3.newV(sumXiYi);
    m.scale(1 / sum_Xi2);
    vTemp.cross(m, axisUnitVector);
    axisUnitVector.add(vTemp);
    axisUnitVector.normalize();
    //check for change in direction by measuring vector difference length
    vTemp.setT(axisUnitVector);
    vTemp.sub(a);
    return vTemp.length();
  }
  
  
  public static void calcAveragePoint(P3 pointA, P3 pointB,
                                      P3 pointC) {
    pointC.set((pointA.x + pointB.x) / 2, (pointA.y + pointB.y) / 2,
        (pointA.z + pointB.z) / 2);
  }
  
  public static void calcAveragePointN(P3[] points, int nPoints,
                                P3 averagePoint) {
    averagePoint.setT(points[0]);
    for (int i = 1; i < nPoints; i++)
      averagePoint.add(points[i]);
    averagePoint.scale(1f / nPoints);
  }

  public static P3[] getCenterAndPoints(JmolList<P3> vPts) {
    int n = vPts.size();
    P3[] pts = new P3[n + 1];
    pts[0] = new P3();
    if (n > 0) {
      for (int i = 0; i < n; i++) {
        pts[0].add(pts[i + 1] = vPts.get(i));
      }
      pts[0].scale(1f / n);
    }
    return pts;
  }

  public static float getTransformMatrix4(JmolList<P3> ptsA, JmolList<P3> ptsB, Matrix4f m, P3 centerA) {
    P3[] cptsA = getCenterAndPoints(ptsA);
    P3[] cptsB = getCenterAndPoints(ptsB);
    float[] retStddev = new float[2];
    Quaternion q = calculateQuaternionRotation(new P3[][] { cptsA,
        cptsB }, retStddev, false);
    V3 v = V3.newV(cptsB[0]);
    v.sub(cptsA[0]);
    m.setMV(q.getMatrix(), v);
    if (centerA != null)
      centerA.setT(cptsA[0]);
    return retStddev[1];
  }
  
  public static Quaternion calculateQuaternionRotation(
                                                       P3[][] centerAndPoints,
                                                       float[] retStddev,
                                                       boolean doReport) {

    retStddev[1] = Float.NaN;
    Quaternion q = new Quaternion();
    if (centerAndPoints[0].length == 1
        || centerAndPoints[0].length != centerAndPoints[1].length)
      return q;

    /*
     * see Berthold K. P. Horn,
     * "Closed-form solution of absolute orientation using unit quaternions" J.
     * Opt. Soc. Amer. A, 1987, Vol. 4, pp. 629-642
     * http://www.opticsinfobase.org/viewmedia.cfm?uri=josaa-4-4-629&seq=0
     * 
     * 
     * A similar treatment was developed independently (and later!) 
     * by G. Kramer, in G. R. Kramer,
     * "Superposition of Molecular Structures Using Quaternions"
     * Molecular Simulation, 1991, Vol. 7, pp. 113-119. 
     * 
     *  In that treatment there is a lot of unnecessary calculation 
     *  along the trace of matrix M (eqn 20). 
     *  I'm not sure why the extra x^2 + y^2 + z^2 + x'^2 + y'^2 + z'^2
     *  is in there, but they are unnecessary and only contribute to larger
     *  numerical averaging errors and additional processing time, as far as
     *  I can tell. Adding aI, where a is a scalar and I is the 4x4 identity
     *  just offsets the eigenvalues but doesn't change the eigenvectors.
     * 
     * and Lydia E. Kavraki, "Molecular Distance Measures"
     * http://cnx.org/content/m11608/latest/
     * 
     */

    int n = centerAndPoints[0].length - 1;
    if (doReport)
      for (int i = 1; i <= n; i++) {
        P3 aij = centerAndPoints[0][i];
        P3 bij = centerAndPoints[1][i];
        if (aij instanceof Atom)
          Logger.info(" atom 1 " + ((Atom) aij).getInfo() + "\tatom 2 "
              + ((Atom) bij).getInfo());
        else
          break;
      }

    if (n < 2)
      return q;

    double Sxx = 0, Sxy = 0, Sxz = 0, Syx = 0, Syy = 0, Syz = 0, Szx = 0, Szy = 0, Szz = 0;
    P3 ptA = new P3();
    P3 ptB = new P3();
    for (int i = n + 1; --i >= 1;) {
      P3 aij = centerAndPoints[0][i];
      P3 bij = centerAndPoints[1][i];
      ptA.setT(aij);
      ptA.sub(centerAndPoints[0][0]);
      ptB.setT(bij);
      ptB.sub(centerAndPoints[0][1]);
      Sxx += (double) ptA.x * (double) ptB.x;
      Sxy += (double) ptA.x * (double) ptB.y;
      Sxz += (double) ptA.x * (double) ptB.z;
      Syx += (double) ptA.y * (double) ptB.x;
      Syy += (double) ptA.y * (double) ptB.y;
      Syz += (double) ptA.y * (double) ptB.z;
      Szx += (double) ptA.z * (double) ptB.x;
      Szy += (double) ptA.z * (double) ptB.y;
      Szz += (double) ptA.z * (double) ptB.z;
    }
    retStddev[0] = getRmsd(centerAndPoints, q);
    double[][] N = new double[4][4];
    N[0][0] = Sxx + Syy + Szz;
    N[0][1] = N[1][0] = Syz - Szy;
    N[0][2] = N[2][0] = Szx - Sxz;
    N[0][3] = N[3][0] = Sxy - Syx;

    N[1][1] = Sxx - Syy - Szz;
    N[1][2] = N[2][1] = Sxy + Syx;
    N[1][3] = N[3][1] = Szx + Sxz;

    N[2][2] = -Sxx + Syy - Szz;
    N[2][3] = N[3][2] = Syz + Szy;

    N[3][3] = -Sxx - Syy + Szz;

    Eigen eigen = Eigen.newM(N);
 
    float[] v = eigen.getEigenvectorsFloatTransposed()[3];
    q = Quaternion.newP4(Point4f.new4(v[1], v[2], v[3], v[0]));
    retStddev[1] = getRmsd(centerAndPoints, q);
    //System.out.println("Measure" + q.getInfo());
    return q;
  }

  public static float getRmsd(P3[][] centerAndPoints, Quaternion q) {
    double sum2 = 0;
    P3[] ptsA = centerAndPoints[0];
    P3[] ptsB = centerAndPoints[1];
    P3 cA = ptsA[0];
    P3 cB = ptsB[0];
    int n = ptsA.length - 1;
    P3 ptAnew = new P3();
    
    for (int i = n + 1; --i >= 1;) {
      ptAnew.setT(ptsA[i]);
      ptAnew.sub(cA);
      q.transformP2(ptAnew, ptAnew);
      ptAnew.add(cB);
      sum2 += ptAnew.distanceSquared(ptsB[i]);
    }
    return (float) Math.sqrt(sum2 / n);
  }

  public static JmolList<P3> transformPoints(JmolList<P3> vPts, Matrix4f m4, P3 center) {
    JmolList<P3> v = new  JmolList<P3>();
    for (int i = 0; i < vPts.size(); i++) {
      P3 pt = P3.newP(vPts.get(i));
      pt.sub(center);
      m4.transform2(pt, pt);
      pt.add(center);
      v.addLast(pt);
    }
    return v;
  }

  public static boolean isInTetrahedron(P3 pt, P3 ptA, P3 ptB,
                                        P3 ptC, P3 ptD,
                                        Point4f plane, V3 vTemp,
                                        V3 vTemp2, V3 vTemp3, boolean fullyEnclosed) {
    getPlaneThroughPoints(ptC, ptD, ptA, vTemp, vTemp2, vTemp3, plane);
    boolean b = (distanceToPlane(plane, pt) >= 0);
    getPlaneThroughPoints(ptA, ptD, ptB, vTemp, vTemp2, vTemp3, plane);
    if (b != (distanceToPlane(plane, pt) >= 0))
      return false;
    getPlaneThroughPoints(ptB, ptD, ptC, vTemp, vTemp2, vTemp3, plane);
    if (b != (distanceToPlane(plane, pt) >= 0))
      return false;
    getPlaneThroughPoints(ptA, ptB, ptC, vTemp, vTemp2, vTemp3, plane);
    float d = distanceToPlane(plane, pt);
    if (fullyEnclosed)
      return (b == (d >= 0));
    float d1 = distanceToPlane(plane, ptD);
    return d1 * d <= 0 || Math.abs(d1) > Math.abs(d);
  }


  /**
   * 
   * @param plane1
   * @param plane2
   * @return       [ point, vector ] or []
   */
  public static JmolList<Object> getIntersectionPP(Point4f plane1, Point4f plane2) {
    float a1 = plane1.x;
    float b1 = plane1.y;
    float c1 = plane1.z;
    float d1 = plane1.w;
    float a2 = plane2.x;
    float b2 = plane2.y;
    float c2 = plane2.z;
    float d2 = plane2.w;
    V3 norm1 = V3.new3(a1, b1, c1);
    V3 norm2 = V3.new3(a2, b2, c2);
    V3 nxn = new V3();
    nxn.cross(norm1, norm2);
    float ax = Math.abs(nxn.x);
    float ay = Math.abs(nxn.y);
    float az = Math.abs(nxn.z);
    float x, y, z, diff;
    int type = (ax > ay ? (ax > az ? 1 : 3) : ay > az ? 2 : 3);
    switch(type) {
    case 1:
      x = 0;
      diff = (b1 * c2 - b2 * c1);
      if (Math.abs(diff) < 0.01) return null;
      y = (c1 * d2 - c2 * d1) / diff;
      z = (b2 * d1 - d2 * b1) / diff;
      break;
    case 2:
      diff = (a1 * c2 - a2 * c1);
      if (Math.abs(diff) < 0.01) return null;
      x = (c1 * d2 - c2 * d1) / diff;
      y = 0;
      z = (a2 * d1 - d2 * a1) / diff;
      break;
    case 3:
    default:
      diff = (a1 * b2 - a2 * b1);
      if (Math.abs(diff) < 0.01) return null;
      x = (b1 * d2 - b2 * d1) / diff;
      y = (a2 * d1 - d2 * a1) / diff;
      z = 0;
    }
    JmolList<Object>list = new  JmolList<Object>();
    list.addLast(P3.new3(x, y, z));
    nxn.normalize();
    list.addLast(nxn);
    return list;
  }

  /**
   * 
   * @param pt1  point on line
   * @param v    unit vector of line
   * @param plane 
   * @param ptRet  point of intersection of line with plane
   * @param tempNorm 
   * @param vTemp 
   * @return       ptRte
   */
  public static P3 getIntersection(P3 pt1, V3 v,
                                               Point4f plane, P3 ptRet, V3 tempNorm, V3 vTemp) {
    getPlaneProjection(pt1, plane, ptRet, tempNorm);
    tempNorm.set(plane.x, plane.y, plane.z);
    tempNorm.normalize();
    if (v == null)
      v = V3.newV(tempNorm);
    float l_dot_n = v.dot(tempNorm);
    if (Math.abs(l_dot_n) < 0.01) return null;
    vTemp.setT(ptRet);
    vTemp.sub(pt1);
    ptRet.scaleAdd2(vTemp.dot(tempNorm) / l_dot_n, v, pt1);
    return ptRet;
  }
/*
  public static Point3f getTriangleIntersection(Point3f a1, Point3f a2,
                                               Point3f a3, Point4f plane,
                                               Point3f b1,
                                               Point3f b2, Point3f b3,
                                               Vector3f vNorm, Vector3f vTemp, 
                                               Point3f ptRet, Point3f ptTemp, Vector3f vTemp2, Point4f pTemp, Vector3f vTemp3) {
    
    if (getTriangleIntersection(b1, b2, a1, a2, a3, vTemp, plane, vNorm, vTemp2, vTemp3, ptRet, ptTemp))
      return ptRet;
    if (getTriangleIntersection(b2, b3, a1, a2, a3, vTemp, plane, vNorm, vTemp2, vTemp3, ptRet, ptTemp))
      return ptRet;
    if (getTriangleIntersection(b3, b1, a1, a2, a3, vTemp, plane, vNorm, vTemp2, vTemp3, ptRet, ptTemp))
      return ptRet;
    return null;
  }
*/
/*  
  public static boolean getTriangleIntersection(Point3f b1, Point3f b2,
                                                Point3f a1, Point3f a2,
                                                Point3f a3, Vector3f vTemp,
                                                Point4f plane, Vector3f vNorm,
                                                Vector3f vTemp2, Vector3f vTemp3,
                                                Point3f ptRet,
                                                Point3f ptTemp) {
    if (distanceToPlane(plane, b1) * distanceToPlane(plane, b2) >= 0)
      return false;
    vTemp.sub(b2, b1);
    vTemp.normalize();
    if (getIntersection(b1, vTemp, plane, ptRet, vNorm, vTemp2) != null) {
      if (isInTriangle(ptRet, a1, a2, a3, vTemp, vTemp2, vTemp3))
        return true;
    }
    return false;
  }
  private static boolean isInTriangle(Point3f p, Point3f a, Point3f b,
                                      Point3f c, Vector3f v0, Vector3f v1,
                                      Vector3f v2) {
    // from http://www.blackpawn.com/texts/pointinpoly/default.html
    // Compute barycentric coordinates
    v0.sub(c, a);
    v1.sub(b, a);
    v2.sub(p, a);
    float dot00 = v0.dot(v0);
    float dot01 = v0.dot(v1);
    float dot02 = v0.dot(v2);
    float dot11 = v1.dot(v1);
    float dot12 = v1.dot(v2);
    float invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
    float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
    float v = (dot00 * dot12 - dot01 * dot02) * invDenom;
    return (u > 0 && v > 0 && u + v < 1);
  }
*/

}
