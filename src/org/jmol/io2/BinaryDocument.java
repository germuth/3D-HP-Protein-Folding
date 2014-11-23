/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: hansonr@stolaf.edu
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
package org.jmol.io2;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.jmol.api.JmolDocument;
import org.jmol.util.Logger;
import org.jmol.util.SB;
import org.jmol.viewer.Viewer;


//import java.io.RandomAccessFile;

/* a basic binary file reader (extended by CompoundDocument). 
 * 
 * random access file info: 
 * http://java.sun.com/docs/books/tutorial/essential/io/rafs.html
 * 
 * SHOOT! random access is only for applications, not applets!
 * 
 * Note that YOU are responsible for determining whether a file
 * is bigEndian or littleEndian; the default is bigEndian.
 * 
 */

public class BinaryDocument implements JmolDocument {

  public BinaryDocument() {  
  }


  // called by reflection
  
  protected DataInputStream stream;
  protected boolean isRandom = false;
  protected boolean isBigEndian = true;

  public void close() {
    if (stream != null)
      try {
        stream.close();
      } catch (Exception e) {
        // ignore
      }
    if (os != null) {
      try {
        os.flush();
        os.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }
  
  public void setStream(BufferedInputStream bis, boolean isBigEndian) {
    if (bis != null)
      stream = new DataInputStream(bis);
    this.isBigEndian = isBigEndian;
  }
  
  public void setStreamData(DataInputStream stream, boolean isBigEndian) {
    if (stream != null)
      this.stream = stream;
    this.isBigEndian = isBigEndian;
  }
  
  public void setRandom(boolean TF) {
    isRandom = TF;
    //CANNOT be random for web 
  }
  
  public byte readByte() throws Exception {
    nBytes++;
    return ioReadByte();
  }

  private byte ioReadByte() throws Exception {
    byte b = stream.readByte();
    if (os != null) {
      /**
       * @j2sNative
       * 
       *  this.os.writeByteAsInt(b);
       * 
       */
      {
      os.write(b);
      }
    }
    return b;
  }

  public int readByteArray(byte[] b, int off, int len) throws Exception {
    int n = ioRead(b, off, len);
    if (n > 0)
      nBytes += n;
    int nBytesRead = n;
    if (n > 0 && n < len) {
      // apparently this is possible over the web
      // it occurred in getting a DNS6B format file from Uppsala
      while (nBytesRead < len && n > 0) {
        n = ioRead(b, nBytesRead, len - nBytesRead);
        if (n > 0) {
          nBytes += n;
          nBytesRead += n;
        }
      }
    }
    return nBytesRead;
  }

  private int ioRead(byte[] b, int off, int len) throws Exception {
    int n = stream.read(b, off, len);
    if (n > 0 && os != null)
      writeBytes(b, off, n);
    return n;
  }

  public void writeBytes(byte[] b, int off, int n) throws Exception {
    os.write(b, off, n);
  }

  public String readString(int nChar) throws Exception {
    byte[] temp = new byte[nChar];
    int n = readByteArray(temp, 0, nChar);
    return new String(temp, 0, n, "UTF-8");
  }
  
  public short readShort() throws Exception {
    nBytes += 2;
    return (isBigEndian ? ioReadShort()
        : (short) ((ioReadByte() & 0xff) 
                 | (ioReadByte() & 0xff) << 8));
  }

  private short ioReadShort() throws Exception {
    short b = stream.readShort();
    if (os != null)
      writeShort(b);
    return b;
  }


  public void writeShort(short i) throws Exception {
    /**
     * @j2sNative
     * 
     *    this.os.writeByteAsInt(i >> 8);
     *    this.os.writeByteAsInt(i);

     * 
     */
    {
      os.write(i >> 8);
      os.write(i);
    }
  }

  public int readInt() throws Exception {
    nBytes += 4;
    return (isBigEndian ? ioReadInt() : readLEInt());
  }
  
  private int ioReadInt() throws Exception {
    int i = stream.readInt();
    if (os != null)
      writeInt(i);
    return i;
  }

  public void writeInt(int i) throws Exception {
    /**
     * @j2sNative
     * 
     *    this.os.writeByteAsInt(i >> 24);
     *    this.os.writeByteAsInt(i >> 16);
     *    this.os.writeByteAsInt(i >> 8);
     *    this.os.writeByteAsInt(i);

     * 
     */
    {
      os.write(i >> 24);
      os.write(i >> 16);
      os.write(i >> 8);
      os.write(i);
    }

  }

  public int swapBytesI(int n) {
    return (((n >> 24) & 0xff)
        | ((n >> 16) & 0xff) << 8
        | ((n >> 8) & 0xff) << 16 
        | (n & 0xff) << 24);
  }

  public short swapBytesS(short n) {
    return (short) ((((n >> 8) & 0xff)
        | (n & 0xff) << 8));
  }

  
  public int readUnsignedShort() throws Exception {
    nBytes += 2;
    int a = (ioReadByte() & 0xff);
    int b = (ioReadByte() & 0xff);
    return (isBigEndian ? (a << 8) + b : (b << 8) + a);
  }
  
  public long readLong() throws Exception {
    nBytes += 8;
    return (isBigEndian ? ioReadLong()
       : ((((long) ioReadByte()) & 0xff)
        | (((long) ioReadByte()) & 0xff) << 8
        | (((long) ioReadByte()) & 0xff) << 16
        | (((long) ioReadByte()) & 0xff) << 24
        | (((long) ioReadByte()) & 0xff) << 32
        | (((long) ioReadByte()) & 0xff) << 40
        | (((long) ioReadByte()) & 0xff) << 48 
        | (((long) ioReadByte()) & 0xff) << 54));
  }

  private long ioReadLong() throws Exception {
    long b = stream.readLong();
    if (os != null)
      writeLong(b);
    return b;
  }

  public void writeLong(long b) throws Exception {
    writeInt((int)((b >> 32) & 0xFFFFFFFFl));
    writeInt((int)(b & 0xFFFFFFFFl));
  }

  public float readFloat() throws Exception {
    int x = readInt();
    /**
     * see http://en.wikipedia.org/wiki/Binary32
     * 
     * [sign]      [8 bits power] [23 bits fraction]
     * 0x80000000  0x7F800000      0x7FFFFF
     * 
     * (untested)
     * 
     * @j2sNative
     * 
     *       if (x == 0) return 0;
     *       var o = org.jmol.io2.BinaryDocument;
     *       if (o.fracIEEE == null);
     *         o.setFracIEEE();
     *       var m = ((x & 0x7F800000) >> 23);
     *       return ((x & 0x80000000) == 0 ? 1 : -1) * o.shiftIEEE((x & 0x7FFFFF) | 0x800000, m - 149);
     *  
     */
    {
    return Float.intBitsToFloat(x);
    }
  }

  private int readLEInt() throws Exception {
    return ((ioReadByte() & 0xff)
          | (ioReadByte() & 0xff) << 8
          | (ioReadByte() & 0xff) << 16 
          | (ioReadByte() & 0xff) << 24);
  }

  byte[] t8 = new byte[8];
  
  public double readDouble() throws Exception {
    /**
     * 
     * reading the float equivalent here in JavaScript
     * 
     * @j2sNative
     * 
     * this.readByteArray(this.t8, 0, 8);
     * return org.jmol.io2.BinaryDocument.bytesToDoubleToFloat(this.t8, 0, this.isBigEndian);
     *  
     */
    {
      nBytes += 8;
      return (isBigEndian ? ioReadDouble() : Double.longBitsToDouble(readLELong()));  
    }
  }
  

  /**
   * see http://en.wikipedia.org/wiki/Binary64
   *  
   * not concerning ourselves with very small or very large numbers and getting
   * this exactly right. Just need a float here.
   * 
   * @param bytes
   * @param j
   * @param isBigEndian
   * @return float
   */
  public static float bytesToDoubleToFloat(byte[] bytes, int j, boolean isBigEndian) {
    {
      // IEEE754: sign (1 bit), exponent (11 bits), fraction (52 bits).
      // seeeeeee eeeeffff ffffffff ffffffff ffffffff xxxxxxxx xxxxxxxx xxxxxxxx
      //     b1      b2       b3       b4       b5    ---------float ignores----

        if (fracIEEE == null)
           setFracIEEE();
        
      /**
       * @j2sNative
       *       var o = org.jmol.io2.BinaryDocument;
       *       var b1, b2, b3, b4, b5;
       *       
       *       if (isBigEndian) {
       *       b1 = bytes[j] & 0xFF;
       *       b2 = bytes[j + 1] & 0xFF;
       *       b3 = bytes[j + 2] & 0xFF;
       *       b4 = bytes[j + 3] & 0xFF;
       *       b5 = bytes[j + 4] & 0xFF;
       *       } else {
       *       b1 = bytes[j + 7] & 0xFF;
       *       b2 = bytes[j + 6] & 0xFF;
       *       b3 = bytes[j + 5] & 0xFF;
       *       b4 = bytes[j + 4] & 0xFF;
       *       b5 = bytes[j + 3] & 0xFF;
       *       }
       *       var s = ((b1 & 0x80) == 0 ? 1 : -1);
       *       var e = (((b1 & 0x7F) << 4) | (b2 >> 4)) - 1026;
       *       b2 = (b2 & 0xF) | 0x10;
       *       return s * (o.shiftIEEE(b2, e) + o.shiftIEEE(b3, e - 8) + o.shiftIEEE(b4, e - 16)
       *         + o.shiftIEEE(b5, e - 24));
       */
      {
        double d;
        
        if (isBigEndian)
          d = Double.longBitsToDouble((((long) bytes[j]) & 0xff) << 56
             | (((long) bytes[j + 1]) & 0xff) << 48
             | (((long) bytes[j + 2]) & 0xff) << 40
             | (((long) bytes[j + 3]) & 0xff) << 32
             | (((long) bytes[j + 4]) & 0xff) << 24
             | (((long) bytes[j + 5]) & 0xff) << 16
             | (((long) bytes[j + 6]) & 0xff) << 8 
             | (((long) bytes[7]) & 0xff));
        else
          d = Double.longBitsToDouble((((long) bytes[j + 7]) & 0xff) << 56
             | (((long) bytes[j + 6]) & 0xff) << 48
             | (((long) bytes[j + 5]) & 0xff) << 40
             | (((long) bytes[j + 4]) & 0xff) << 32
             | (((long) bytes[j + 3]) & 0xff) << 24
             | (((long) bytes[j + 2]) & 0xff) << 16
             | (((long) bytes[j + 1]) & 0xff) << 8 
             | (((long) bytes[j]) & 0xff));
        return (float) d;
      }

    }
  }

  private static float[] fracIEEE;

  static void setFracIEEE() {
    fracIEEE = new float[270];
    for (int i = 0; i < 270; i++)
      fracIEEE[i] = (float) Math.pow(2, i - 141);
    //    System.out.println(fracIEEE[0] + "  " + Float.MIN_VALUE);
    //    System.out.println(fracIEEE[269] + "  " + Float.MAX_VALUE);
  }

  /**
   * only concerned about reasonable float values here
   * 
   * @param f
   * @param i
   * @return f * 2^i
   */
  static double shiftIEEE(double f, int i) {
    if (f == 0 || i < -140)
      return 0;
    if (i > 128)
      return Float.MAX_VALUE;
    return f * fracIEEE[i + 140];
  }

//  static {
//    setFracIEEE();
//    for (int i = -50; i < 50; i++) {
//      float f = i * (float) (Math.random() * Math.pow(2, Math.random() * 100 - 50));
//      int x = Float.floatToIntBits(f);
//      int m = ((x & 0x7F800000) >> 23);
//      float f1 = (float) (f == 0 ? 0 : ((x & 0x80000000) == 0 ? 1 : -1) * shiftIEEE((x & 0x7FFFFF) | 0x800000, m - 149));
//      System.out.println(f + "  " + f1);
//    }
//    System.out.println("binarydo");
//  }


  private double ioReadDouble() throws Exception {
    double d = stream.readDouble();
    if (os != null)
      writeLong(Double.doubleToRawLongBits(d));
    return d;
  }

  private long readLELong() throws Exception {
    return ((((long) ioReadByte()) & 0xff)
          | (((long) ioReadByte()) & 0xff) << 8
          | (((long) ioReadByte()) & 0xff) << 16 
          | (((long) ioReadByte()) & 0xff) << 24
          | (((long) ioReadByte()) & 0xff) << 32
          | (((long) ioReadByte()) & 0xff) << 40
          | (((long) ioReadByte()) & 0xff) << 48
          | (((long) ioReadByte()) & 0xff) << 56);
  }

  public void seek(long offset) {
    // slower, but all that is available using the applet
    try {
      if (offset == nBytes)
        return;
      if (offset < nBytes) {
        stream.reset();
        nBytes = 0;
      } else {
        offset -= nBytes;
      }
      stream.skipBytes((int)offset);
      nBytes += offset;
    } catch (Exception e) {
      Logger.errorEx(null, e);
    }
  }

  long nBytes;
  
  public long getPosition() {
    return nBytes;
  }

  OutputStream os;
  public void setOutputStream(OutputStream os, Viewer viewer, double privateKey) {
    if (viewer.checkPrivateKey(privateKey))
      this.os = os;
  }

  public SB getAllDataFiles(String binaryFileList, String firstFile) {
    return null;
  }

  public void getAllDataMapped(String replace, String string,
                               Map<String, String> fileData) {
  }


/*  random access -- application only:
 * 
    void seekFile(long offset) {
    try {
      file.seek(offset);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
*/
}
