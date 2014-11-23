package org.jmol.api;


import org.jmol.modelset.Atom;
import org.jmol.util.JmolFont;
import org.jmol.util.GData;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.MeshSurface;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.V3;
import org.jmol.viewer.Viewer;

public interface JmolRendererInterface extends JmolGraphicsInterface {

  // exporting  

  public abstract int getExportType();

  public abstract String getExportName();

  public abstract Object initializeExporter(String type, Viewer viewer,
                                             double privateKey, GData gdata,
                                             Object output);

  public abstract boolean initializeOutput(String type, Viewer viewer,
                                        double privateKey, GData gdata,
                                        Object object);

  public abstract short[] getBgColixes(short[] bgcolixes);

  public abstract String finalizeOutput();

  // rendering or exporting

  public abstract boolean checkTranslucent(boolean isAlphaTranslucent);

  public abstract boolean haveTranslucentObjects();

  public abstract void setFontFid(byte fid);

  public abstract JmolFont getFont3DCurrent();

  public abstract void setNoisySurfaceShade(P3i screenA, P3i screenB,
                                            P3i screenC);

  public abstract byte getFontFidFS(String fontFace, float fontSize);

  public abstract boolean isDirectedTowardsCamera(short normix);

  public abstract V3[] getTransformedVertexVectors();

  public abstract void drawAtom(Atom atom);

  /**
   * draws a ring and filled circle (halos, draw CIRCLE, draw handles)
   * 
   * @param colixRing
   * @param colixFill
   * @param diameter
   * @param x
   *        center x
   * @param y
   *        center y
   * @param z
   *        center z
   */
  public abstract void drawFilledCircle(short colixRing, short colixFill,
                                        int diameter, int x, int y, int z);

  /**
   * fills a solid sphere
   * 
   * @param diameter
   *        pixel count
   * @param x
   *        center x
   * @param y
   *        center y
   * @param z
   *        center z
   */
  public abstract void fillSphereXYZ(int diameter, int x, int y, int z);

  /**
   * fills a solid sphere
   * 
   * @param diameter
   *        pixel count
   * @param center
   *        javax.vecmath.Point3i defining the center
   */

  public abstract void fillSphereI(int diameter, P3i center);

  /**
   * fills a solid sphere
   * 
   * @param diameter
   *        pixel count
   * @param center
   *        a javax.vecmath.Point3f ... floats are casted to ints
   */
  public abstract void fillSphere(int diameter, P3 center);

  /**
   * draws a rectangle
   * 
   * @param x
   *        upper left x
   * @param y
   *        upper left y
   * @param z
   *        upper left z
   * @param zSlab
   *        z for slab check (for set labelsFront)
   * @param rWidth
   *        pixel count
   * @param rHeight
   *        pixel count
   */
  public abstract void drawRect(int x, int y, int z, int zSlab, int rWidth,
                                int rHeight);

  /**
   * fills background rectangle for label
   *<p>
   * 
   * @param x
   *        upper left x
   * @param y
   *        upper left y
   * @param z
   *        upper left z
   * @param zSlab
   *        z value for slabbing
   * @param widthFill
   *        pixel count
   * @param heightFill
   *        pixel count
   */
  public abstract void fillRect(int x, int y, int z, int zSlab, int widthFill,
                                int heightFill);

  /**
   * draws the specified string in the current font. no line wrapping -- axis,
   * labels, measures
   * 
   * @param str
   *        the String
   * @param font3d
   *        the Font3D
   * @param xBaseline
   *        baseline x
   * @param yBaseline
   *        baseline y
   * @param z
   *        baseline z
   * @param zSlab
   *        z for slab calculation
   * @param bgColix TODO
   */

  public abstract void drawString(String str, JmolFont font3d, int xBaseline,
                                  int yBaseline, int z, int zSlab, short bgColix);

  public abstract void plotImagePixel(int argb, int x, int y, int z, int shade, int bgargb);

  /**
   * draws the specified string in the current font. no line wrapping -- echo,
   * frank, hover, molecularOrbital, uccage
   * 
   * @param str
   *        the String
   * @param font3d
   *        the Font3D
   * @param xBaseline
   *        baseline x
   * @param yBaseline
   *        baseline y
   * @param z
   *        baseline z
   * @param bgColix TODO
   */

  public abstract void drawStringNoSlab(String str, JmolFont font3d,
                                        int xBaseline, int yBaseline, int z, short bgColix);

  public abstract void fillEllipsoid(P3 center, P3[] points, int x,
                                     int y, int z, int diameter,
                                     Matrix3f mToEllipsoidal, double[] coef,
                                     Matrix4f mDeriv, int selectedOctant,
                                     P3i[] octantPoints);

  public abstract void drawImage(Object image, int x, int y, int z, int zslab,
                                 short bgcolix, int width, int height);

  public abstract void drawPixel(int x, int y, int z);

  public abstract void plotPixelClippedP3i(P3i a);

  public abstract void drawPoints(int count, int[] coordinates, int scale);

  public abstract void drawDashedLine(int run, int rise, P3i pointA,
                                      P3i pointB);

  public abstract void drawDottedLine(P3i pointA, P3i pointB);

  public abstract void drawLineXYZ(int x1, int y1, int z1, int x2, int y2, int z2);

  public abstract void drawLineAB(P3i pointA, P3i pointB);

  public abstract void drawLine(short colixA, short colixB, int x1, int y1,
                                int z1, int x2, int y2, int z2);

  public abstract void drawBond(P3 atomA, P3 atomB, short colixA,
                                short colixB, byte endcaps, short mad, int bondOrder);

  public abstract void fillCylinderXYZ(short colixA, short colixB, byte endcaps,
                                    int diameter, int xA, int yA, int zA,
                                    int xB, int yB, int zB);

  public abstract void fillCylinder(byte endcaps, int diameter,
                                    P3i screenA, P3i screenB);

  public abstract void fillCylinderBits(byte endcaps, int diameter,
                                        P3 screenA, P3 screenB);

  public abstract void fillCylinderScreen(byte endcaps, int diameter, int xA,
                                          int yA, int zA, int xB, int yB, int zB);

  public abstract void fillCylinderScreen3I(byte endcapsOpenend, int diameter,
                                          P3i pt0i, P3i pt1i, P3 pt0f, P3 pt1f, float radius);

  public abstract void fillConeScreen(byte endcap, int screenDiameter,
                                      P3i screenBase, P3i screenTip,
                                      boolean isBarb);

  public abstract void fillConeSceen3f(byte endcap, int screenDiameter,
                                     P3 screenBase, P3 screenTip);

  public abstract void drawHermite4(int tension, P3i s0, P3i s1,
                                   P3i s2, P3i s3);

  public abstract void drawHermite7(boolean fill, boolean border, int tension,
                                   P3i s0, P3i s1, P3i s2,
                                   P3i s3, P3i s4, P3i s5,
                                   P3i s6, P3i s7, int aspectRatio, short colixBack);

  public abstract void fillHermite(int tension, int diameterBeg,
                                   int diameterMid, int diameterEnd,
                                   P3i s0, P3i s1, P3i s2,
                                   P3i s3);

  // isosurface "mesh" option -- color mapped vertices
  public abstract void drawTriangle3C(P3i screenA, short colixA,
                                    P3i screenB, short colixB,
                                    P3i screenC, short colixC, int check);

  // isosurface and other meshes -- preset colix
  public abstract void drawTriangle3I(P3i screenA, P3i screenB,
                                    P3i screenC, int check);

  /* was for stereo -- not implemented
  public abstract void drawfillTriangle(int xA, int yA, int zA, int xB, int yB,
                                        int zB, int xC, int yC, int zC);
  */

  // isosurface colored triangles
  public abstract void fillTriangle3CN(P3i screenA, short colixA,
                                    short normixA, P3i screenB,
                                    short colixB, short normixB,
                                    P3i screenC, short colixC, short normixC);

  // polyhedra
  public abstract void fillTriangleTwoSided(short normix, int xScreenA,
                                            int yScreenA, int zScreenA,
                                            int xScreenB, int yScreenB,
                                            int zScreenB, int xScreenC,
                                            int yScreenC, int zScreenC);

  public abstract void fillTriangle3f(P3 screenA, P3 screenB,
                                    P3 screenC, boolean setNoisy);

  public abstract void fillTriangle3i(P3i screenA, P3i screenB,
                                    P3i screenC, P3 ptA, P3 ptB, P3 ptC);

  public abstract void fillTriangle(P3i screenA, short colixA,
                                    short normixA, P3i screenB,
                                    short colixB, short normixB,
                                    P3i screenC, short colixC,
                                    short normixC, float factor);

  public abstract void drawQuadrilateral(short colix, P3i screenA,
                                         P3i screenB, P3i screenC,
                                         P3i screenD);

  public abstract void fillQuadrilateral(P3 screenA, P3 screenB,
                                         P3 screenC, P3 screenD);

  public abstract void fillQuadrilateral3i(P3i screenA, short colixA,
                                         short normixA, P3i screenB,
                                         short colixB, short normixB,
                                         P3i screenC, short colixC,
                                         short normixC, P3i screenD,
                                         short colixD, short normixD);

  public abstract void drawSurface(MeshSurface meshSurface, short colix);

  public abstract void setTranslucentCoverOnly(boolean TF);

  public abstract boolean drawEllipse(P3 ptAtom, P3 ptX, P3 ptY,
                                      boolean fillArc, boolean wireframeOnly);

  public abstract void volumeRender(boolean TF);

  public abstract void volumeRender4(int diam, int x, int y, int z);

  /**
   * sets current color from colix color index
   * 
   * @param colix
   *        the color index
   * @return true or false if this is the right pass
   */
  public abstract boolean setColix(short colix);

  public abstract void setColor(int color);

  public abstract boolean isPass2();

  public abstract void renderBackground(JmolRendererInterface jre);

  public abstract GData getGData();

  // g3d only
  
  public abstract boolean currentlyRendering();

  public abstract void renderCrossHairs(int[] minMax, int screenWidth,
                                        int screenHeight,
                                        P3 navigationOffset,
                                        float navigationDepthPercent);

}
