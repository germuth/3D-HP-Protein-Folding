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
 * A 3-element vector that is represented by single precision floating point
 * x,y,z coordinates. If this value represents a normal, then it should be
 * normalized.
 * 
 * @version specification 1.1, implementation $Revision: 1.10 $, $Date:
 *          2006/10/03 19:52:30 $
 * @author Kenji hiranabe
 * 
 * additions by Bob Hanson hansonr@stolaf.edu 9/30/2012
 * for unique constructor and method names
 * for the optimization of compiled JavaScript using Java2Script
 */
public class V3 extends Tuple3f {

  public static V3 newV(Tuple3f t) {
    V3 v = new V3();
    v.x = t.x;
    v.y = t.y;
    v.z = t.z;
    return v;
  }

  public static V3 new3(float x, float y, float z) {
    V3 v = new V3();
    v.x = x;
    v.y = y;
    v.z = z;
    return v;
  }

  /**
   * Returns the squared length of this vector.
   * 
   * @return the squared length of this vector
   */
  public final float lengthSquared() {
    return x * x + y * y + z * z;
  }

  /**
   * Returns the length of this vector.
   * 
   * @return the length of this vector
   */
  public final float length() {
    return (float) Math.sqrt(lengthSquared());
  }

  /**
   * Sets this vector to be the vector cross product of vectors v1 and v2.
   * 
   * @param v1
   *        the first vector
   * @param v2
   *        the second vector
   */
  public final void cross(V3 v1, V3 v2) {
    set(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y
        - v1.y * v2.x);
  }

  /**
   * Computes the dot product of the this vector and vector v.
   * 
   * @param v
   *        the other vector
   * @return this.dot.v
   */
  public final float dot(V3 v) {
    return x * v.x + y * v.y + z * v.z;
  }

  /**
   * Normalizes this vector in place.
   */
  public final void normalize() {
    double d = length();

    // zero-div may occur.
    x /= d;
    y /= d;
    z /= d;
  }

  /**
   * Returns the angle in radians between this vector and the vector parameter;
   * the return value is constrained to the range [0,PI].
   * 
   * @param v1
   *        the other vector
   * @return the angle in radians in the range [0,PI]
   */
  public final float angle(V3 v1) {
    // return (double)Math.acos(dot(v1)/v1.length()/v.length());
    // Numerically, near 0 and PI are very bad condition for acos.
    // In 3-space, |atan2(sin,cos)| is much stable.

    double xx = y * v1.z - z * v1.y;
    double yy = z * v1.x - x * v1.z;
    double zz = x * v1.y - y * v1.x;
    double cross = Math.sqrt(xx * xx + yy * yy + zz * zz);

    return (float) Math.abs(Math.atan2(cross, dot(v1)));
  }
}
