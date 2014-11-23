/*
   Copyright (C) 1997,1998,1999
   Kenji Hiranabe, Eiwa System Management, Inc.

   This program is free software.
   Implemented by Kenji Hiranabe(hiranabe@esm.co.jp),
   conforming to the Java(TM) 3D API specification by Sun Microsystems.

   Permission to use, copy, modify, distribute and sell this software
   and its documentation for any purpose is hereby granted without fee,
   provided that the above copyright notice appear in all copies and
   that both that copyright notice and this permission notice appear
   in supporting documentation. Kenji Hiranabe and Eiwa System Management,Inc.
   makes no representations about the suitability of this software for any
   purpose.  It is provided "AS IS" with NO WARRANTY.
*/
package org.jmol.util;


/**
 * A 3 element point that is represented by single precision floating point
 * x,y,z coordinates.
 * 
 * @version specification 1.1, implementation $Revision: 1.10 $, $Date:
 *          2006/09/08 20:20:20 $
 * @author Kenji hiranabe
 * 
 * additions by Bob Hanson hansonr@stolaf.edu 9/30/2012
 * for unique constructor and method names
 * for the optimization of compiled JavaScript using Java2Script
 * 
 */
public class P3 extends Tuple3f {

  public static P3 newP(Tuple3f t) {
    P3 p = new P3();
    p.x = t.x;
    p.y = t.y;
    p.z = t.z;
    return p;
  }

  public static P3 new3(float x, float y, float z) {
    P3 p = new P3();
    p.x = x;
    p.y = y;
    p.z = z;
    return p;
  }

  /**
   * Computes the square of the distance between this point and point p1.
   * 
   * @param p1
   *        the other point
   * @return the square of distance between these two points as a float
   */
  public final float distanceSquared(P3 p1) {
    double dx = x - p1.x;
    double dy = y - p1.y;
    double dz = z - p1.z;
    return (float) (dx * dx + dy * dy + dz * dz);
  }

  /**
   * Returns the distance between this point and point p1.
   * 
   * @param p1
   *        the other point
   * @return the distance between these two points
   */
  public final float distance(P3 p1) {
    return (float) Math.sqrt(distanceSquared(p1));
  }

}
