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

import java.io.Serializable;


/**
 * A generic 4 element tuple that is represented by single precision floating
 * point x,y,z and w coordinates.
 * 
 * @version specification 1.1, implementation $Revision: 1.9 $, $Date:
 *          2006/07/28 17:01:32 $
 * @author Kenji hiranabe
 * 
 * additions by Bob Hanson hansonr@stolaf.edu 9/30/2012
 * for unique constructor and method names
 * for the optimization of compiled JavaScript using Java2Script
 */
public abstract class Tuple4f implements Serializable {

  /**
   * The x coordinate.
   */
  public float x;

  /**
   * The y coordinate.
   */
  public float y;

  /**
   * The z coordinate.
   */
  public float z;

  /**
   * The w coordinate.
   */
  public float w;

  /**
   * Constructs and initializes a Tuple4f to (0,0,0,0).
   */
  public Tuple4f() {
  }

  /**
   * Sets the value of this tuple to the specified xyzw coordinates.
   * 
   * @param x
   *        the x coordinate
   * @param y
   *        the y coordinate
   * @param z
   *        the z coordinate
   * @param w
   *        the w coordinate
   */
  public final void set(float x, float y, float z, float w) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
  }

  /**
   * Sets the value of this tuple to the scalar multiplication of itself.
   * 
   * @param s
   *        the scalar value
   */
  public final void scale(float s) {
    x *= s;
    y *= s;
    z *= s;
    w *= s;
  }

  /**
   * Returns a hash number based on the data values in this object. Two
   * different Tuple4f objects with identical data values (ie, returns true for
   * equals(Tuple4f) ) will return the same hash number. Two vectors with
   * different data members may return the same hash value, although this is not
   * likely.
   */
  @Override
  public int hashCode() {
    return Tuple3f.floatToIntBits0(x) ^ Tuple3f.floatToIntBits0(y)
        ^ Tuple3f.floatToIntBits0(z) ^ Tuple3f.floatToIntBits0(w);
  }

  /**
   * Returns true if all of the data members of Object are equal to the
   * corresponding data members in this
   * 
   * @param o
   *        the vector with which the comparison is made.
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Tuple4f))
      return false;
    Tuple4f t = (Tuple4f) o;
    return (this.x == t.x && this.y == t.y && this.z == t.z && this.w == t.w);
  }

  /**
   * Returns a string that contains the values of this Tuple4f. The form is
   * (x,y,z,w).
   * 
   * @return the String representation
   */
  @Override
  public String toString() {
    return "(" + x + ", " + y + ", " + z + ", " + w + ")";
  }

}
