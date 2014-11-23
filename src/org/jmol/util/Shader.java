/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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


/**
 *<p>
 * All functions.
 * Implements the shading of RGB values to support shadow and lighting
 * highlights.
 *</p>
 *<p>
 * Each RGB value has 64 shades. shade[0] represents ambient lighting.
 * shade[63] is white ... a full specular highlight.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 * @author Bob Hanson, hansonr@stolaf.edu
 * @author N David Brown -- cel shading
 * 
 */
public class Shader {

  // there are 64 shades of a given color
  // 0 = ambient
  // 52 = normal
  // 56 = max for 
  // 63 = specular
  private static int shadeIndexMax = 64;
  public static int shadeIndexLast = shadeIndexMax - 1;
  public static byte shadeIndexNormal = 52;
  public static byte shadeIndexNoisyLimit = 56;

  // the viewer vector is always {0 0 1}

  // the light source vector normalized
  private float xLight, yLight, zLight;
  
  public Shader() {
    setLightSource(-1f, -1f, 2.5f); 
  }
  
  V3 lightSource = new V3();
  
  private void setLightSource(float x, float y, float z) {
    lightSource.set(x, y, z);
    lightSource.normalize();
    xLight = lightSource.x;
    yLight = lightSource.y;
    zLight = lightSource.z;
  }

  public boolean specularOn = true; 
  public boolean usePhongExponent = false;
  
  //fractional distance from black for ambient color
  public int ambientPercent = 45;
  
  // df in I = df * (N dot L) + sf * (R dot V)^p
  public int diffusePercent = 84;

  // log_2(p) in I = df * (N dot L) + sf * (R dot V)^p
  // for faster calculation of shades
  public int specularExponent = 6;

  // sf in I = df * (N dot L) + sf * (R dot V)^p
  // not a percent of anything, really
  public int specularPercent = 22;
  
  // fractional distance to white for specular dot
  public int specularPower = 40;

  // p in I = df * (N dot L) + sf * (R dot V)^p
  public int phongExponent = 64;
  
  public float ambientFraction = ambientPercent / 100f;
  public float diffuseFactor = diffusePercent / 100f;
  public float intenseFraction = specularPower / 100f;
  public float specularFactor = specularPercent / 100f;
  
  private int[][] ashades = ArrayUtil.newInt2(128);
  private int[][] ashadesGreyscale;
  private int rgbContrast;

  public void setCel(boolean celOn, int argb) {
    argb = C.getArgb(ColorUtil.getBgContrast(argb));
    if (argb == 0xFF000000)
      argb = 0xFF040404; // problem here is with antialiasDisplay on white background
    if (this.celOn == celOn && rgbContrast == argb)
      return;
    this.celOn = celOn;
    rgbContrast = argb;
    flushCaches();
  }
  
  public void flushCaches() {
    flushShades();
    flushSphereCache();
  }
  
  public boolean getCelOn() {
    return celOn;
  }
  
  public void setLastColix(int argb, boolean asGrey) {
    C.allocateColix(argb);
    checkShades();
    if (asGrey)
      C.setLastGrey(argb);
    ashades[C.LAST_AVAILABLE_COLIX] = getShades2(argb, false);
  }

  public int[] getShades(short colix) {
    checkShades();
    colix &= C.OPAQUE_MASK;
    int[] shades = ashades[colix];
    if (shades == null)
      shades = ashades[colix] = getShades2(C.argbs[colix], false);
    return shades;
  }

  public int[] getShadesG(short colix) {
    checkShades();
    colix &= C.OPAQUE_MASK;
    if (ashadesGreyscale == null)
      ashadesGreyscale = ArrayUtil.newInt2(ashades.length);
    int[] shadesGreyscale = ashadesGreyscale[colix];
    if (shadesGreyscale == null)
      shadesGreyscale = ashadesGreyscale[colix] =
        getShades2(C.argbs[colix], true);
    return shadesGreyscale;
  }

  private void checkShades() {
    if (ashades != null && ashades.length == C.colixMax)
      return;
    ashades = ArrayUtil.arrayCopyII(ashades, C.colixMax);
    if (ashadesGreyscale != null)
      ashadesGreyscale = ArrayUtil.arrayCopyII(ashadesGreyscale, C.colixMax);
  }
  
  public void flushShades() {
    checkShades();
    for (int i = C.colixMax; --i >= 0; )
      ashades[i] = null;
    sphereShadingCalculated = false;
  }

  /*
   * intensity calculation:
   * 
   * af ambientFraction (from ambient percent)
   * if intenseFraction (from specular power)
   * 
   * given a color rr gg bb, consider one of these components x:
   * 
   * int[0:63] shades   [0 .......... 52(normal) ........ 63]
   *                     af*x........ x ..............x+(255-x)*if
   *              black  <---ambient%--x---specular power---->  white
   */

  private int[] getShades2(int rgb, boolean greyScale) {
    int[] shades = new int[shadeIndexMax];
    if (rgb == 0)
      return shades;

    float red0 = ((rgb >> 16) & 0xFF);
    float grn0 = ((rgb >> 8) & 0xFF);
    float blu0 = (rgb & 0xFF);

    float red = 0;
    float grn = 0;
    float blu = 0;

    float f = ambientFraction;

    while (true) {
      red = red0 * f + 0.5f;
      grn = grn0 * f + 0.5f;
      blu = blu0 * f + 0.5f;
      if (f > 0 && red < 4 && grn < 4 && blu < 4) {
        // with antialiasing, black shades with all 
        // components less than 4 will be considered 0
        // so we must adjust things just a bit.
        red0++;
        grn0++;
        blu0++;
        if (f < 0.1f)
          f += 0.1f;
        rgb = ColorUtil.rgb((int) Math.floor(red0), (int) Math.floor(grn0),
            (int) Math.floor(blu0));
        continue;
      }
      break;
    }

    int i;
    
   if (celOn) {

     int max = shadeIndexMax/2;

     f = (1 - f) /shadeIndexNormal;

     float redStep = red0 * f;
     float grnStep = grn0 * f;
     float bluStep = blu0 * f;

     int _rgb = ColorUtil.rgb((int) Math.floor(red), (int) Math.floor(grn),
         (int) Math.floor(blu));
     for (i = 0; i < max; ++i)
       shades[i] = _rgb;

     red += redStep * max;
     grn += grnStep * max;
     blu += bluStep * max;

     _rgb = ColorUtil.rgb((int) Math.floor(red), (int) Math.floor(grn),
         (int) Math.floor(blu));
     for (; i < shadeIndexMax; i++)
       shades[i] = _rgb;

     // Min r,g,b is 4,4,4 or else antialiasing bleeds background colour into edges.
     shades[0] = shades[1] = rgbContrast;

   } else {

      f = (1 - f) / shadeIndexNormal;
      float redStep = red0 * f;
      float grnStep = grn0 * f;
      float bluStep = blu0 * f;

      for (i = 0; i < shadeIndexNormal; ++i) {
        shades[i] = ColorUtil.rgb((int) Math.floor(red), (int) Math.floor(grn),
            (int) Math.floor(blu));
        red += redStep;
        grn += grnStep;
        blu += bluStep;
      }

      shades[i++] = rgb;

      f = intenseFraction / (shadeIndexMax - i);
      redStep = (255.5f - red) * f;
      grnStep = (255.5f - grn) * f;
      bluStep = (255.5f - blu) * f;

      for (; i < shadeIndexMax; i++) {
        red += redStep;
        grn += grnStep;
        blu += bluStep;
        shades[i] = ColorUtil.rgb((int) Math.floor(red), (int) Math.floor(grn),
            (int) Math.floor(blu));
      }
    }
    if (greyScale)
      for (; --i >= 0;)
        shades[i] = ColorUtil.calcGreyscaleRgbFromRgb(shades[i]);
    return shades;
  }

  public int getShadeIndex(float x, float y, float z) {
    // from Cylinder3D.calcArgbEndcap and renderCone
    // from GData.getShadeIndex and getShadeIndex
    double magnitude = Math.sqrt(x * x + y * y + z * z);
    return Math.round(getShadeF((float) (x / magnitude),
        (float) (y / magnitude), (float) (z / magnitude))
        * shadeIndexLast);
  }

  public byte getShadeB(float x, float y, float z) {
    //from Normix3D.setRotationMatrix
    return (byte) Math.round (getShadeF(x, y, z)
                  * shadeIndexLast);
  }

  public int getShadeFp8(float x, float y, float z) {
    //from calcDitheredNoisyShadeIndex (not utilized)
    //and Cylinder.calcRotatedPoint
    double magnitude = Math.sqrt(x*x + y*y + z*z);
    return (int) Math.floor(getShadeF((float)(x/magnitude),
                                              (float)(y/magnitude),
                                              (float)(z/magnitude))
                 * shadeIndexLast * (1 << 8));
  }

  private float getShadeF(float x, float y, float z) {
    float NdotL = x * xLight + y * yLight + z * zLight;
    if (NdotL <= 0)
      return 0;
    // I = k_diffuse * f_diffuse + k_specular * f_specular
    // where
    // k_diffuse = (N dot L)
    // k_specular = {[(2(N dot L)N - L] dot V}^p
    //
    // and in our case V = {0 0 1} so the z component of that is:
    // 
    // k_specular = ( 2 * NdotL * z - zLight )^p
    // 
    // HOWEVER -- Jmol's "specularExponent is log_2(phongExponent)
    //
    // "specularExponent" phong_exponent
    // 0 1
    // 1 2
    // 2 4
    // 3 8
    // 4 16
    // 5 32
    // 5.322 40
    // 6 64
    // 7 128
    // 8 256
    // 9 512
    // 10 1024
    float intensity = NdotL * diffuseFactor;
    if (specularOn) {
      float k_specular = 2 * NdotL * z - zLight;
      if (k_specular > 0) {
        if (usePhongExponent) {
          k_specular = (float) Math.pow(k_specular, phongExponent);
        } else {
          for (int n = specularExponent; --n >= 0
              && k_specular > .0001f;)
            k_specular *= k_specular;
        }
        intensity += k_specular * specularFactor;
      }
    }
    return (celOn && z < 0.5f ? 0f : intensity > 1 ? 1f : intensity);
  }

  /*
   byte getDitheredShadeIndex(float x, float y, float z) {
   //not utilized
   // add some randomness to prevent banding
   int fp8ShadeIndex = getFp8ShadeIndex(x, y, z);
   int shadeIndex = fp8ShadeIndex >> 8;
   // this cannot overflow because the if the float shadeIndex is 1.0
   // then shadeIndex will be == shadeLast
   // but there will be no fractional component, so the next test will fail
   if ((fp8ShadeIndex & 0xFF) > nextRandom8Bit())
   ++shadeIndex;
   int random16bit = seed & 0xFFFF;
   if (random16bit < 65536 / 3 && shadeIndex > 0)
   --shadeIndex;
   else if (random16bit > 65536 * 2 / 3 && shadeIndex < shadeLast)
   ++shadeIndex;
   return (byte)shadeIndex;
   }
   */

  public byte getShadeN(float x, float y, float z, float r) {
    // from Sphere3D only
    // add some randomness to prevent banding
    int fp8ShadeIndex = (int) Math.floor(getShadeF(x / r, y / r, z / r)
        * shadeIndexLast * (1 << 8));
    int shadeIndex = fp8ShadeIndex >> 8;
    // this cannot overflow because the if the float shadeIndex is 1.0
    // then shadeIndex will be == shadeLast
    // but there will be no fractional component, so the next test will fail
    if ((fp8ShadeIndex & 0xFF) > nextRandom8Bit())
      ++shadeIndex;
    int random16bit = seed & 0xFFFF;
    if (random16bit < 65536 / 3 && shadeIndex > 0)
      --shadeIndex;
    else if (random16bit > 65536 * 2 / 3 && shadeIndex < shadeIndexLast)
      ++shadeIndex;
    return (byte) shadeIndex;
  }

  /*
    This is a linear congruential pseudorandom number generator,
    as defined by D. H. Lehmer and described by Donald E. Knuth in
    The Art of Computer Programming,
    Volume 2: Seminumerical Algorithms, section 3.2.1.

  long seed = 1;
  int nextRandom8Bit() {
    seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    //    return (int)(seed >>> (48 - bits));
    return (int)(seed >>> 40);
  }
  */

  
  ////////////////////////////////////////////////////////////////
  // Sphere shading cache for Large spheres
  ////////////////////////////////////////////////////////////////

  public boolean sphereShadingCalculated = false;
  public byte[] sphereShadeIndexes = new byte[256 * 256];

  public synchronized void calcSphereShading() {
    //if (!sphereShadingCalculated) { //unnecessary -- but be careful!
    float xF = -127.5f;
    for (int i = 0; i < 256; ++xF, ++i) {
      float yF = -127.5f;
      for (int j = 0; j < 256; ++yF, ++j) {
        byte shadeIndex = 0;
        float z2 = 130 * 130 - xF * xF - yF * yF;
        if (z2 > 0) {
          float z = (float) Math.sqrt(z2);
          shadeIndex = getShadeN(xF, yF, z, 130);
        }
        sphereShadeIndexes[(j << 8) + i] = shadeIndex;
      }
    }
    sphereShadingCalculated = true;
  }
  
  /*
  byte getSphereshadeIndex(int x, int y, int r) {
    int d = 2*r + 1;
    x += r;
    if (x < 0)
      x = 0;
    int x8 = (x << 8) / d;
    if (x8 > 0xFF)
      x8 = 0xFF;
    y += r;
    if (y < 0)
      y = 0;
    int y8 = (y << 8) / d;
    if (y8 > 0xFF)
      y8 = 0xFF;
    return sphereShadeIndexes[(y8 << 8) + x8];
  }
  */
    
  // this doesn't really need to be synchronized
  // no serious harm done if two threads write seed at the same time
  private int seed = 0x12345679; // turn lo bit on
  /**
   *<p>
   * Implements RANDU algorithm for random noise in lighting/shading.
   *</p>
   *<p>
   * RANDU is the classic example of a poor random number generator.
   * But it is very cheap to calculate and is good enough for our purposes.
   *</p>
   *
   * @return Next random
   */
  public int nextRandom8Bit() {
    int t = seed;
    seed = t = ((t << 16) + (t << 1) + t) & 0x7FFFFFFF;
    return t >> 23;
  }

  private final static int SLIM = 20;
  private final static int SDIM = SLIM * 2;
  public final static int maxSphereCache = 128;
  public int[][] sphereShapeCache = ArrayUtil.newInt2(maxSphereCache);
  public byte[][][] ellipsoidShades;
  public int nOut;
  public int nIn;
  private boolean celOn;

  // Cel shading.
  // @author N David Brown

  public int getEllipsoidShade(float x, float y, float z, int radius,
                                       Matrix4f mDeriv) {
    float tx = mDeriv.m00 * x + mDeriv.m01 * y + mDeriv.m02 * z + mDeriv.m03;
    float ty = mDeriv.m10 * x + mDeriv.m11 * y + mDeriv.m12 * z + mDeriv.m13;
    float tz = mDeriv.m20 * x + mDeriv.m21 * y + mDeriv.m22 * z + mDeriv.m23;
    float f = Math.min(radius/2f, 45) / 
        (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
    // optimized for about 30-100% inclusion
    int i = (int) (-tx * f);
    int j = (int) (-ty * f);
    int k = (int) (tz * f);
    boolean outside = i < -SLIM || i >= SLIM || j < -SLIM || j >= SLIM
        || k < 0 || k >= SDIM;
    if (outside) {
      while (i % 2 == 0 && j % 2 == 0 && k % 2 == 0 && i + j + k > 0) {
        i >>= 1;
        j >>= 1;
        k >>= 1;
      }
      outside = i < -SLIM || i >= SLIM || j < -SLIM || j >= SLIM || k < 0
          || k >= SDIM;
    }
    
    if (outside)
      nOut++;
    else
      nIn++;
  
    return (outside ? getShadeIndex(i, j, k)
        : ellipsoidShades[i + SLIM][j + SLIM][k]);
  }

  public void createEllipsoidShades() {
    
    // we don't need to cache rear-directed normals (kk < 0)
    
    ellipsoidShades = new byte[SDIM][SDIM][SDIM];
    for (int ii = 0; ii < SDIM; ii++)
      for (int jj = 0; jj < SDIM; jj++)
        for (int kk = 0; kk < SDIM; kk++)
          ellipsoidShades[ii][jj][kk] = (byte) getShadeIndex(ii - SLIM, jj
              - SLIM, kk);
  }

  public synchronized void flushSphereCache() {
    for (int i =  maxSphereCache; --i >= 0;)
      sphereShapeCache[i] = null;
    ellipsoidShades = null;
  }
  
}
