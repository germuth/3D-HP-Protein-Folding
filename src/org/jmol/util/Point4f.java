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
 * A 4 element point that is represented by single precision floating point
 * x,y,z,w coordinates.
 * 
 * @version specification 1.1, implementation $Revision: 1.9 $, $Date:
 *          2006/07/28 17:01:32 $
 * @author Kenji hiranabe
 * 
 * additions by Bob Hanson hansonr@stolaf.edu 9/30/2012
 * for unique constructor and method names
 * for the optimization of compiled JavaScript using Java2Script
 */
public class Point4f extends Tuple4f {

  /**
   * Constructs and initializes a Point4f to (0,0,0,0).
   */
  public Point4f() {
    super();
  }

  public static Point4f new4(float x, float y, float z, float w) {
    Point4f pt = new Point4f();
    pt.set(x, y, z, w);
    return pt;
  }

  public static Point4f newPt(Point4f value) {
    Point4f pt = new Point4f();
    pt.set(value.x, value.y, value.z, value.w);
    return pt;
  }

  /**
   * Returns the distance between this point and point p1.
   * 
   * @param p1
   *        the other point
   * @return the distance between these two points
   */
  public final float distance(Point4f p1) {
    double dx = x - p1.x;
    double dy = y - p1.y;
    double dz = z - p1.z;
    double dw = w - p1.w;
    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz + dw * dw);
  }

}
