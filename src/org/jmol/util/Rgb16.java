/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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


public final class Rgb16 {
  public int rScaled;
  public int gScaled;
  public int bScaled;
    
  public Rgb16() {
  }

  public static Rgb16 newI(int argb) {
    Rgb16 c = new Rgb16();
    c.setInt(argb);
    return c;
  }

  public void setInt(int argb) {
    rScaled = ((argb >> 8) & 0xFF00) | 0x80;
    gScaled = ((argb     ) & 0xFF00) | 0x80;
    bScaled = ((argb << 8) & 0xFF00) | 0x80;
  }

  public void setRgb(Rgb16 other) {
    rScaled = other.rScaled;
    gScaled = other.gScaled;
    bScaled = other.bScaled;
  }

  public void diffDiv(Rgb16 rgb16A, Rgb16 rgb16B, int divisor) {
    rScaled = (rgb16A.rScaled - rgb16B.rScaled) / divisor;
    gScaled = (rgb16A.gScaled - rgb16B.gScaled) / divisor;
    bScaled = (rgb16A.bScaled - rgb16B.bScaled) / divisor;
  }

  /*
  void add(Rgb16 other) {
    rScaled += other.rScaled;
    gScaled += other.gScaled;
    bScaled += other.bScaled;
  }
  */
  
  /*
  void add(Rgb16 base, Rgb16 other) {
    rScaled = base.rScaled + other.rScaled;
    gScaled = base.gScaled + other.gScaled;
    bScaled = base.bScaled + other.bScaled;
  }
  */
  
  public void setAndIncrement(Rgb16 base, Rgb16 other) {
    rScaled = base.rScaled;
    base.rScaled += other.rScaled;
    gScaled = base.gScaled;
    base.gScaled += other.gScaled;
    bScaled = base.bScaled;
    base.bScaled += other.bScaled;
  }

  public int getArgb() {
    return (                 0xFF000000 |
           ((rScaled << 8) & 0x00FF0000)|
           (gScaled        & 0x0000FF00)|
           (bScaled >> 8));
  }

  @Override
  public String toString() {
    return new SB()
    .append("Rgb16(").appendI(rScaled).appendC(',')
    .appendI(gScaled).appendC(',')
    .appendI(bScaled).append(" -> ")
    .appendI((rScaled >> 8) & 0xFF).appendC(',')
    .appendI((gScaled >> 8) & 0xFF).appendC(',')
    .appendI((bScaled >> 8) & 0xFF).appendC(')').toString();
  }
}

