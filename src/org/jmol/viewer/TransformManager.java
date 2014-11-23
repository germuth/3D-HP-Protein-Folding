/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-18 10:29:29 -0600 (Mon, 18 Dec 2006) $
 * $Revision: 6502 $
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
package org.jmol.viewer;

import org.jmol.api.Interface;
import org.jmol.api.JmolNavigatorInterface;
import org.jmol.api.JmolScriptEvaluator;
import org.jmol.constant.EnumStereoMode;
import org.jmol.script.T;
import org.jmol.thread.MoveThread;
import org.jmol.thread.MoveToThread;
import org.jmol.thread.SpinThread;
import org.jmol.thread.VibrationThread;
import org.jmol.util.AxisAngle4f;
import org.jmol.util.BS;
import org.jmol.util.Escape;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.Point4f;
import org.jmol.util.SB;
import org.jmol.util.Tuple3f;
import org.jmol.util.V3;

import org.jmol.util.Quaternion;

import java.util.Hashtable;

import java.util.Map;

public class TransformManager {

  Viewer viewer;

  final static double twoPI = 2 * Math.PI;
  public final static double degreesPerRadian = 180 / Math.PI;

  static final int DEFAULT_NAV_FPS = 10;

  protected int perspectiveModel = DEFAULT_PERSPECTIVE_MODEL;
  protected float cameraScaleFactor;
  public float referencePlaneOffset;
  protected float aperatureAngle;
  protected float cameraDistanceFromCenter;
  public float modelCenterOffset;
  public float modelRadius;
  public float modelRadiusPixels;

  public final P3 navigationCenter = new P3();
  public final P3 navigationOffset = new P3();
  public final P3 navigationShiftXY = new P3();
  public float navigationDepth;

  protected final Matrix4f matrixTemp = new Matrix4f();
  protected final V3 vectorTemp = new V3();

  TransformManager(Viewer viewer, int width, int height) {
    setViewer(viewer, width, height);
  }

  private void setViewer(Viewer viewer, int width, int height) {
    this.viewer = viewer;
    setScreenParameters(width, height, true, false, true, true);
  }

  //  TransformManager getNavigationManager(Viewer viewer, int width, int height) {
  //    return new TransformManager11(viewer, width, height);
  //  }

  /* ***************************************************************
   * GENERAL METHODS
   ***************************************************************/

  public void homePosition(boolean resetSpin) {
    // reset, setNavigationMode, setPerspectiveModel
    if (resetSpin)
      setSpinOff();
    setNavOn(false);
    navFps = DEFAULT_NAV_FPS;
    navX = navY = navZ = 0;
    rotationCenterDefault.setT(viewer.getBoundBoxCenter());
    setFixedRotationCenter(rotationCenterDefault);
    rotationRadiusDefault = setRotationRadius(0, true);
    windowCentered = true;
    setRotationCenterAndRadiusXYZ(null, true);
    matrixRotate.setIdentity(); // no rotations
    //if (viewer.autoLoadOrientation()) {
    Matrix3f m = (Matrix3f) viewer
        .getModelSetAuxiliaryInfoValue("defaultOrientationMatrix");
    if (m != null)
      matrixRotate.setM(m);
    //}
    setZoomEnabled(true);
    zoomToPercent(viewer.isModelKitMode() ? 50 : 100);
    zoomPercent = zoomPercentSetting;
    slabReset();
    resetFitToScreen(true);
    if (viewer.isJmolDataFrame()) {
      fixedRotationCenter.set(0, 0, 0);
    } else {
      if (viewer.getAxesOrientationRasmol())
        rotateX((float) Math.PI);
    }
    viewer.saveOrientation("default");
    if (mode == MODE_NAVIGATION)
      setNavigationMode(true);
  }

  void clear() {
    clearVibration();
    clearSpin();
    stopMotion();
    fixedRotationCenter.set(0, 0, 0);
    navigating = false;
    slabPlane = null;
    depthPlane = null;
    zSlabPoint = null;
    resetNavigationPoint(true);
  }

  protected boolean haveNotifiedNaN = false;

  final static int DEFAULT_SPIN_Y = 30;
  final static int DEFAULT_SPIN_FPS = 30;
  public float spinX;

  public float spinY = DEFAULT_SPIN_Y;

  public float spinZ;

  public float spinFps = DEFAULT_SPIN_FPS;
  public float navX;
  public float navY;
  public float navZ;
  public float navFps = Float.NaN;

  public boolean isSpinInternal = false;
  public boolean isSpinFixed = false;
  boolean isSpinSelected = false;

  public final P3 fixedRotationOffset = new P3();
  public final P3 fixedRotationCenter = new P3();
  protected final P3 perspectiveOffset = new P3();
  protected final P3 perspectiveShiftXY = new P3();

  private final P3 rotationCenterDefault = new P3();
  private float rotationRadiusDefault;

  public final AxisAngle4f fixedRotationAxis = new AxisAngle4f();
  public final AxisAngle4f internalRotationAxis = new AxisAngle4f();
  protected V3 internalTranslation;
  final P3 internalRotationCenter = P3.new3(0, 0, 0);
  private float internalRotationAngle = 0;

  /* ***************************************************************
   * ROTATIONS
   ***************************************************************/

  // this matrix only holds rotations ... no translations
  protected final Matrix3f matrixRotate = new Matrix3f();

  private final Matrix3f matrixTemp3 = new Matrix3f();
  private final Matrix4f matrixTemp4 = new Matrix4f();
  private final AxisAngle4f axisangleT = new AxisAngle4f();
  private final V3 vectorT = new V3();
  private final V3 vectorT2 = new V3();
  private final P3 pointT2 = new P3();

  final static int MAXIMUM_ZOOM_PERCENTAGE = 200000;
  final static int MAXIMUM_ZOOM_PERSPECTIVE_DEPTH = 10000;

  private void setFixedRotationCenter(P3 center) {
    if (center == null)
      return;
    fixedRotationCenter.setT(center);
  }

  void setRotationPointXY(P3 center) {
    P3i newCenterScreen = transformPoint(center);
    fixedTranslation.set(newCenterScreen.x, newCenterScreen.y, 0);
  }

  V3 rotationAxis = new V3();
  float rotationRate = 0;

  void spinXYBy(int xDelta, int yDelta, float speed) {
    // from mouse action
    if (xDelta == 0 && yDelta == 0) {
      if (spinThread != null && spinThread.isGesture())
        clearSpin();
      return;
    }
    clearSpin();
    P3 pt1 = P3.newP(fixedRotationCenter);
    P3 ptScreen = new P3();
    transformPoint2(pt1, ptScreen);
    P3 pt2 = P3.new3(-yDelta, xDelta, 0);
    pt2.add(ptScreen);
    unTransformPoint(pt2, pt2);
    viewer.setInMotion(false);
    rotateAboutPointsInternal(null, pt2, pt1, 10 * speed, Float.NaN, false,
        true, null, true, null, null);
  }

  final V3 arcBall0 = new V3();
  final V3 arcBall1 = new V3();
  final V3 arcBallAxis = new V3();
  final Matrix3f arcBall0Rotation = new Matrix3f();

  void rotateArcBall(float x, float y, float factor) {
    // radius is half the screen pixel count. 
    float radius2 = (screenPixelCount >> 2) * screenPixelCount;
    x -= fixedTranslation.x;
    y -= fixedTranslation.y;
    float z = radius2 - x * x - y * y;
    z = (z < 0 ? -1 : 1) * (float) Math.sqrt(Math.abs(z));
    if (factor == 0) {
      // mouse down sets the initial rotation and point on the sphere
      arcBall0Rotation.setM(matrixRotate);
      arcBall0.set(x, -y, z);
      if (!Float.isNaN(z))
        arcBall0.normalize();
      return;
    }
    if (Float.isNaN(arcBall0.z) || Float.isNaN(z))
      return;
    arcBall1.set(x, -y, z);
    arcBall1.normalize();
    arcBallAxis.cross(arcBall0, arcBall1);
    axisangleT.setVA(arcBallAxis, factor
        * (float) Math.acos(arcBall0.dot(arcBall1)));
    matrixRotate.setM(arcBall0Rotation);
    rotateAxisAngle2(axisangleT, null);
  }

  void rotateXYBy(float xDelta, float yDelta, BS bsAtoms) {
    // from mouse action
    rotateXRadians(yDelta * JC.radiansPerDegree, bsAtoms);
    rotateYRadians(xDelta * JC.radiansPerDegree, bsAtoms);
  }

  void rotateZBy(int zDelta, int x, int y) {
    if (x != Integer.MAX_VALUE && y != Integer.MAX_VALUE)
      resetXYCenter(x, y);
    rotateZRadians((float) (zDelta / degreesPerRadian));
  }

  void rotateFront() {
    matrixRotate.setIdentity();
  }

  void rotateX(float angleRadians) {
    matrixRotate.rotX(angleRadians);
  }

  void rotateY(float angleRadians) {
    matrixRotate.rotY(angleRadians);
  }

  void rotateZ(float angleRadians) {
    matrixRotate.rotZ(angleRadians);
  }

  private void applyRotation(Matrix3f mNew, boolean isInternal, BS bsAtoms,
                             V3 translation) {
    if (bsAtoms == null) {
      matrixRotate.mul2(mNew, matrixRotate);
      return;
    }
    viewer.moveAtoms(mNew, matrixRotate, translation, internalRotationCenter,
        isInternal, bsAtoms);
    if (translation != null) {
      internalRotationCenter.add(translation);
    }
  }

  public synchronized void rotateXRadians(float angleRadians, BS bsAtoms) {
    matrixTemp3.rotX(angleRadians);
    applyRotation(matrixTemp3, false, bsAtoms, null);
  }

  public synchronized void rotateYRadians(float angleRadians, BS bsAtoms) {
    matrixTemp3.rotY(angleRadians);
    applyRotation(matrixTemp3, false, bsAtoms, null);
  }

  public synchronized void rotateZRadians(float angleRadians) {
    matrixTemp3.rotZ(angleRadians);
    applyRotation(matrixTemp3, false, null, null);
  }

  public void rotateAxisAngle(V3 rotAxis, float radians) {
    axisangleT.setVA(rotAxis, radians);
    rotateAxisAngle2(axisangleT, null);
  }

  private synchronized void rotateAxisAngle2(AxisAngle4f axisAngle,
                                             BS bsAtoms) {
    //matrixTemp3.setIdentity();
    matrixTemp3.setAA(axisAngle);
    applyRotation(matrixTemp3, false, bsAtoms, null);
  }

  /*
   * *************************************************************** *THE* TWO
   * VIEWER INTERFACE METHODS
   * **************************************************************
   */

  boolean rotateAxisAngleAtCenter(JmolScriptEvaluator eval, P3 rotCenter,
                                  V3 rotAxis, float degreesPerSecond,
                                  float endDegrees, boolean isSpin,
                                  BS bsAtoms) {

    // *THE* Viewer FIXED frame rotation/spinning entry point
    if (rotCenter != null)
      moveRotationCenter(rotCenter, true);

    setSpinOff();
    setNavOn(false);

    if (viewer.isHeadless()) {
      if (isSpin && endDegrees == Float.MAX_VALUE)
        return false;
      isSpin = false;
    }
    if (Float.isNaN(degreesPerSecond) || degreesPerSecond == 0
        || endDegrees == 0)
      return false;

    if (rotCenter != null) {
      setRotationPointXY(rotCenter);
    }
    setFixedRotationCenter(rotCenter);
    rotationAxis.setT(rotAxis);
    rotationRate = degreesPerSecond;
    if (isSpin) {
      fixedRotationAxis.setVA(rotAxis, degreesPerSecond
          * JC.radiansPerDegree);
      isSpinInternal = false;
      isSpinFixed = true;
      isSpinSelected = (bsAtoms != null);
      setSpin(eval, true, endDegrees, null, bsAtoms, false);
      return false;
    }
    float radians = endDegrees * JC.radiansPerDegree;
    fixedRotationAxis.setVA(rotAxis, endDegrees);
    rotateAxisAngleRadiansFixed(radians, bsAtoms);
    return true;
  }

  public synchronized void rotateAxisAngleRadiansFixed(float angleRadians,
                                                       BS bsAtoms) {
    // for spinning -- reduced number of radians
    axisangleT.setAA(fixedRotationAxis);
    axisangleT.angle = angleRadians;
    rotateAxisAngle2(axisangleT, bsAtoms);
  }

  /*
   * *************************************************************** INTERNAL
   * ROTATIONS**************************************************************
   */

  boolean rotateAboutPointsInternal(JmolScriptEvaluator eval, P3 point1,
                                    P3 point2, float degreesPerSecond,
                                    float endDegrees, boolean isClockwise,
                                    boolean isSpin, BS bsAtoms,
                                    boolean isGesture, V3 translation,
                                    JmolList<P3> finalPoints) {

    // *THE* Viewer INTERNAL frame rotation entry point

    setSpinOff();
    setNavOn(false);

    if (viewer.isHeadless()) {
      if (isSpin && endDegrees == Float.MAX_VALUE)
        return false;
      isSpin = false;
    }

    if ((translation == null || translation.length() < 0.001)
        && (!isSpin || endDegrees == 0 || Float.isNaN(degreesPerSecond) || degreesPerSecond == 0)
        && (isSpin || endDegrees == 0))
      return false;

    V3 axis = V3.newV(point2);
    axis.sub(point1);
    if (isClockwise)
      axis.scale(-1f);
    internalRotationCenter.setT(point1);
    rotationAxis.setT(axis);
    if (translation == null) {
      internalTranslation = null;
    } else {
      internalTranslation = V3.newV(translation);
      //System.out.println("TM TRANSLATE " + internalTranslation);
    }
    boolean isSelected = (bsAtoms != null);
    if (isSpin) {
      // we need to adjust the degreesPerSecond to match a multiple of the frame rate
      //System.out.println(" ?? " + (spinFps / Math.abs(degreesPerSecond) * Math.abs(endDegrees)));
      int nFrames = (int) (Math.abs(endDegrees) / Math.abs(degreesPerSecond)
          * spinFps + 0.5);
      if (Float.isNaN(endDegrees)) {
        rotationRate = degreesPerSecond;
      } else {
        rotationRate = degreesPerSecond = endDegrees / nFrames * spinFps;
        //System.out.println("TM nFrames = " + nFrames);
        if (translation != null)
          internalTranslation.scale(1f / (nFrames));
      }
      internalRotationAxis.setVA(axis, rotationRate
          * JC.radiansPerDegree);
      isSpinInternal = true;
      isSpinFixed = false;
      isSpinSelected = isSelected;
      setSpin(eval, true, endDegrees, finalPoints, bsAtoms, isGesture);
      return false;
    }
    float radians = endDegrees * JC.radiansPerDegree;
    internalRotationAxis.setVA(axis, radians);
    rotateAxisAngleRadiansInternal(radians, bsAtoms);
    return true;
  }

  public synchronized void rotateAxisAngleRadiansInternal(float radians,
                                                          BS bsAtoms) {

    // final matrix rotation when spinning or just rotating

    // trick is to apply the current rotation to the internal rotation axis
    // and then save the angle for generating a new fixed point later

    internalRotationAngle = radians;
    vectorT.set(internalRotationAxis.x, internalRotationAxis.y,
        internalRotationAxis.z);
    matrixRotate.transform2(vectorT, vectorT2);
    axisangleT.setVA(vectorT2, radians);

    // NOW apply that rotation  

    matrixTemp3.setAA(axisangleT);
    applyRotation(matrixTemp3, true, bsAtoms, internalTranslation);
    if (bsAtoms == null)
      getNewFixedRotationCenter();
  }

  void getNewFixedRotationCenter() {

    /*
     * (1) determine vector offset VectorT () 
     * (2) translate old point so trueRotationPt is at [0,0,0] (old - true)
     * (3) do axisangle rotation of -radians (pointT2)
     * (4) translate back (pointT2 + vectorT)
     * 
     * The new position of old point is the new rotation center
     * set this, rotate about it, and it will APPEAR that the 
     * rotation was about the desired point and axis!
     *  
     */

    // fractional OPPOSITE of angle of rotation
    axisangleT.setAA(internalRotationAxis);
    axisangleT.angle = -internalRotationAngle;
    //this is a fraction of the original for spinning
    matrixTemp4.setAA(axisangleT);

    // apply this to the fixed center point in the internal frame

    vectorT.setT(internalRotationCenter);
    pointT2.setT(fixedRotationCenter);
    pointT2.sub(vectorT);
    P3 pt = new P3();
    matrixTemp4.transform2(pointT2, pt);

    // return this point to the fixed frame

    pt.add(vectorT);

    // it is the new fixed rotation center!

    setRotationCenterAndRadiusXYZ(pt, false);
  }

  /* ***************************************************************
   * TRANSLATIONS
   ****************************************************************/
  public final P3 fixedTranslation = new P3();

  float xTranslationFraction = 0.5f;
  float yTranslationFraction = 0.5f;
  protected float prevZoomSetting;

  public float previousX;

  public float previousY;

  void setTranslationFractions() {
    xTranslationFraction = fixedTranslation.x / width;
    yTranslationFraction = fixedTranslation.y / height;
  }

  public void centerAt(int x, int y, P3 pt) {
    if (pt == null) {
      translateXYBy(x, y);
      return;
    }
    if (windowCentered)
      viewer.setBooleanProperty("windowCentered", false);
    //transformPoint2(pt, pointT);
    //float z = pointT.z;
    fixedTranslation.x = x;
    fixedTranslation.y = y;
    setFixedRotationCenter(pt);
    //transformPoint2(pt, pointT);
    //finalizeTransformParameters();
    //System.out.println("trans man ");
  }

  int percentToPixels(char xyz, float percent) {
    switch (xyz) {
    case 'x':
      return (int) Math.floor(percent / 100 * width);
    case 'y':
      return (int) Math.floor(percent / 100 * height);
    case 'z':
      return (int) Math.floor(percent / 100 * screenPixelCount);
    }
    return 0;
  }

  int angstromsToPixels(float distance) {
    return (int) Math.floor(scalePixelsPerAngstrom * distance);
  }

  void translateXYBy(int xDelta, int yDelta) {
    // mouse action or translate x|y|z x.x nm|angstroms|%
    fixedTranslation.x += xDelta;
    fixedTranslation.y += yDelta;
    setTranslationFractions();
  }

  public void translateToPercent(char type, float percent) {
    switch (type) {
    case 'x':
      xTranslationFraction = 0.5f + percent / 100;
      fixedTranslation.x = width * xTranslationFraction;
      return;
    case 'y':
      yTranslationFraction = 0.5f + percent / 100;
      fixedTranslation.y = height * yTranslationFraction;
      return;
    case 'z':
      if (mode == MODE_NAVIGATION)
        setNavigationDepthPercent(percent);
      return;
    }
  }

  public float getTranslationXPercent() {
    return (width == 0 ? 0 : (fixedTranslation.x - width / 2f) * 100 / width);
  }

  public float getTranslationYPercent() {
    return (height == 0 ? 0 : (fixedTranslation.y - height / 2f) * 100 / height);
  }

  public float getTranslationZPercent() {
    return 0;
  }

  String getTranslationScript() {
    String info = "";
    float f = getTranslationXPercent();
    if (f != 0.0)
      info += "translate x " + f + ";";
    f = getTranslationYPercent();
    if (f != 0.0)
      info += "translate y " + f + ";";
    return info;
  }

  String getOrientationText(int type) {
    switch (type) {
    case T.moveto:
      return getMoveToText(1, false);
    case T.rotation:
      return getRotationQuaternion().toString();
    case T.translation:
      SB sb = new SB();
      truncate2(sb, getTranslationXPercent());
      truncate2(sb, getTranslationYPercent());
      return sb.toString();
    default:
      return getMoveToText(1, true) + "\n#OR\n" + getRotateZyzText(true);

    }
  }

  Map<String, Object> getOrientationInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("moveTo", getMoveToText(1, false));
    info.put("center", "center " + getCenterText());
    info.put("centerPt", fixedRotationCenter);
    AxisAngle4f aa = new AxisAngle4f();
    getAxisAngle(aa);
    info.put("axisAngle", aa);
    info.put("quaternion", Quaternion.newAA(aa).toPoint4f());
    info.put("rotationMatrix", matrixRotate);
    info.put("rotateZYZ", getRotateZyzText(false));
    info.put("rotateXYZ", getRotateXyzText());
    info.put("transXPercent", Float.valueOf(getTranslationXPercent()));
    info.put("transYPercent", Float.valueOf(getTranslationYPercent()));
    info.put("zoom", Float.valueOf(zoomPercent));
    info.put("modelRadius", Float.valueOf(modelRadius));
    if (mode == MODE_NAVIGATION) {
      info.put("navigationCenter", "navigate center "
          + Escape.eP(navigationCenter));
      info.put("navigationOffsetXPercent", Float.valueOf(
          getNavigationOffsetPercent('X')));
      info.put("navigationOffsetYPercent", Float.valueOf(
          getNavigationOffsetPercent('Y')));
      info
          .put("navigationDepthPercent", Float.valueOf(getNavigationDepthPercent()));
    }
    return info;
  }

  void getAxisAngle(AxisAngle4f axisAngle) {
    axisAngle.setM(matrixRotate);
  }

  String getTransformText() {
    return matrixRotate.toString();
  }

  public Matrix3f getMatrixRotate() {
    return matrixRotate;
  }

  public void setRotation(Matrix3f matrixRotation) {
    if (!Float.isNaN(matrixRotation.m00))
      matrixRotate.setM(matrixRotation);
  }

  public void getRotation(Matrix3f matrixRotation) {
    // hmm ... I suppose that there could be a race condiditon here
    // if matrixRotate is being modified while this is called
    matrixRotation.setM(matrixRotate);
  }

  /* ***************************************************************
   * ZOOM
   ****************************************************************/
  boolean zoomEnabled = true;
  // zoomPercent is the current displayed zoom value
  public float zoomPercent = 100;
  // zoomPercentSetting is the current setting of zoom
  // if zoom is not enabled then the two values will be different
  float zoomPercentSetting = 100;
  float zoomRatio;

  /**
   * standard response to user mouse vertical shift-drag
   * 
   * @param pixels
   */
  protected void zoomBy(int pixels) {
    if (pixels > 20)
      pixels = 20;
    else if (pixels < -20)
      pixels = -20;
    float deltaPercent = pixels * zoomPercentSetting / 50;
    if (deltaPercent == 0)
      deltaPercent = (pixels > 0 ? 1 : (deltaPercent < 0 ? -1 : 0));
    zoomRatio = (deltaPercent + zoomPercentSetting) / zoomPercentSetting;
    zoomPercentSetting += deltaPercent;
  }

  float getZoomPercentFloat() {
    return zoomPercent;
  }

  public void zoomToPercent(float percentZoom) {
    zoomPercentSetting = percentZoom;
    zoomRatio = 0;
  }

  void translateZBy(int pixels) {
    if (pixels >= screenPixelCount)
      return;
    float sppa = scalePixelsPerAngstrom
        / (1 - pixels * 1.0f / screenPixelCount);
    if (sppa >= screenPixelCount)
      return;
    float newZoomPercent = sppa / scaleDefaultPixelsPerAngstrom * 100f;
    zoomRatio = newZoomPercent / zoomPercentSetting;
    zoomPercentSetting = newZoomPercent;
  }

  void zoomByFactor0(float factor, int x, int y) {
    // overloaded in TransformManager11
    if (factor <= 0 || !zoomEnabled)
      return;
    zoomRatio = factor;
    zoomPercentSetting *= factor;
    resetXYCenter(x, y);
  }

  private void resetXYCenter(int x, int y) {
    if (x == Integer.MAX_VALUE || y == Integer.MAX_VALUE)
      return;
    if (windowCentered)
      viewer.setBooleanProperty("windowCentered", false);
    P3 pt = new P3();
    transformPoint2(fixedRotationCenter, pt);
    pt.set(x, y, pt.z);
    unTransformPoint(pt, pt);
    fixedTranslation.set(x, y, 0);
    setFixedRotationCenter(pt);
  }

  void zoomByPercent(float percentZoom) {
    float deltaPercent = percentZoom * zoomPercentSetting / 100;
    if (deltaPercent == 0)
      deltaPercent = (percentZoom < 0) ? -1 : 1;
    zoomRatio = (deltaPercent + zoomPercentSetting) / zoomPercentSetting;
    zoomPercentSetting += deltaPercent;
  }

  void setScaleAngstromsPerInch(float angstromsPerInch) {
    // not compatible with perspectiveDepth
    scale3D = (angstromsPerInch > 0);
    if (scale3D)
      scale3DAngstromsPerInch = angstromsPerInch;
    perspectiveDepth = !scale3D;
  }

  /* ***************************************************************
   * SLAB
   ****************************************************************/

  /*
   slab is a term defined and used in rasmol.
   it is a z-axis clipping plane. only atoms behind the slab get rendered.
   100% means:
   - the slab is set to z==0
   - 100% of the molecule will be shown
   50% means:
   - the slab is set to the center of rotation of the molecule
   - only the atoms behind the center of rotation are shown
   0% means:
   - the slab is set behind the molecule
   - 0% (nothing, nada, nil, null) gets shown
   */

  public boolean slabEnabled = false;
  boolean internalSlab = false;
  boolean zShadeEnabled = false;

  int slabPercentSetting;
  int depthPercentSetting;
  public int zSlabPercentSetting = 50; // new default for 12.3.6 and 12.2.6
  public int zDepthPercentSetting = 0;
  P3 zSlabPoint;

  void setZslabPoint(P3 pt) {
    zSlabPoint = (pt == null ? null : P3.newP(pt));
  }

  public int slabValue;
  public int depthValue;
  public int zSlabValue;
  int zDepthValue;

  int getZShadeStart() {
    return (zShadeEnabled ? zSlabValue : 0);
  }

  float slabRange = 0f;

  public void setSlabRange(float value) {
    slabRange = value;
  }

  void setSlabEnabled(boolean slabEnabled) {
    this.slabEnabled = slabEnabled;
    viewer.getGlobalSettings().setB("slabEnabled", slabEnabled);
  }

  void setZShadeEnabled(boolean zShadeEnabled) {
    this.zShadeEnabled = zShadeEnabled;
    viewer.getGlobalSettings().setB("zShade", zShadeEnabled);
  }

  void setZoomEnabled(boolean zoomEnabled) {
    this.zoomEnabled = zoomEnabled;
    viewer.getGlobalSettings().setB("zoomEnabled", zoomEnabled);
  }

  Point4f slabPlane = null;
  Point4f depthPlane = null;

  void slabReset() {
    slabToPercent(100);
    depthToPercent(0);
    depthPlane = null;
    slabPlane = null;
    setSlabEnabled(false);
    setZShadeEnabled(false);
  }

  public int getSlabPercentSetting() {
    return slabPercentSetting;
  }

  void slabByPercentagePoints(int percentage) {
    slabPlane = null;
    slabPercentSetting += percentage;
    slabDepthChanged();
    if (depthPercentSetting >= slabPercentSetting)
      depthPercentSetting = slabPercentSetting - 1;
    //    System.out.println("transformManager slab/depthpercentSetting: " + slabPercentSetting + " " + depthPercentSetting);
  }

  private void slabDepthChanged() {
    viewer.getGlobalSettings().setI("slab", slabPercentSetting);
    viewer.getGlobalSettings().setI("depth", depthPercentSetting);
  }

  void depthByPercentagePoints(int percentage) {
    depthPlane = null;
    depthPercentSetting += percentage;
    if (slabPercentSetting <= depthPercentSetting)
      slabPercentSetting = depthPercentSetting + 1;
    slabDepthChanged();
    //    System.out.println("transformManager slab/depthpercentSetting: " + slabPercentSetting + " " + depthPercentSetting);
  }

  void slabDepthByPercentagePoints(int percentage) {
    slabPlane = null;
    depthPlane = null;
    slabPercentSetting += percentage;
    depthPercentSetting += percentage;
    slabDepthChanged();
    //    System.out.println("transformManager slab/depthpercentSetting: " + slabPercentSetting + " " + depthPercentSetting);
  }

  public void slabToPercent(int percentSlab) {
    viewer.setFloatProperty("slabRange", 0);
    slabPercentSetting = percentSlab;
    slabPlane = null;
    if (depthPercentSetting >= slabPercentSetting)
      depthPercentSetting = slabPercentSetting - 1;
    slabDepthChanged();
  }

  void depthToPercent(int percentDepth) {
    viewer.getGlobalSettings().setI("depth", percentDepth);
    depthPercentSetting = percentDepth;
    if (slabPercentSetting <= depthPercentSetting)
      slabPercentSetting = depthPercentSetting + 1;
    slabDepthChanged();
  }

  void zSlabToPercent(int percentSlab) {
    zSlabPercentSetting = percentSlab;
    if (zDepthPercentSetting > zSlabPercentSetting)
      zDepthPercentSetting = percentSlab;
  }

  void zDepthToPercent(int percentDepth) {
    zDepthPercentSetting = percentDepth;
    if (zDepthPercentSetting > zSlabPercentSetting)
      zSlabPercentSetting = percentDepth;
  }

  void slabInternal(Point4f plane, boolean isDepth) {
    //also from viewer
    if (isDepth) {
      depthPlane = plane;
      depthPercentSetting = 0;
    } else {
      slabPlane = plane;
      slabPercentSetting = 100;
    }
  }

  /**
   * set internal slab or depth from screen-based slab or depth
   * 
   * @param isDepth
   */
  void setSlabDepthInternal(boolean isDepth) {
    finalizeTransformParameters();
    if (isDepth)
      depthPlane = null;
    else
      slabPlane = null;
    slabInternal(getSlabDepthPlane(isDepth), isDepth);
  }

  Point4f getSlabDepthPlane(boolean isDepth) {
    // the third row of the matrix defines the Z coordinate, which is all we need
    // and, in fact, it defines the plane. How convenient!
    // eval "slab set"
    if (isDepth) {
      if (depthPlane != null)
        return depthPlane;
    } else {
      if (slabPlane != null)
        return slabPlane;
    }
    Matrix4f m = matrixTransform;
    return Point4f.new4(-m.m20, -m.m21, -m.m22, -m.m23
        + (isDepth ? depthValue : slabValue));
  }

  boolean checkInternalSlab(P3 pt) {
    return (slabPlane != null
        && pt.x * slabPlane.x + pt.y * slabPlane.y + pt.z * slabPlane.z
            + slabPlane.w > 0 || depthPlane != null
        && pt.x * depthPlane.x + pt.y * depthPlane.y + pt.z * depthPlane.z
            + depthPlane.w < 0);
  }

  /* ***************************************************************
   * PERSPECTIVE
   ****************************************************************/

  /* Jmol treatment of perspective   Bob Hanson 12/06
   * 
   * See http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/navigation.pdf
   * 


   DEFAULT SCALE -- (zoom == 100) 

   We start by defining a fixedRotationCenter and a modelRadius that encompasses 
   the model. Then: 

   defaultScalePixelsPerAngstrom = screenPixelCount / (2 * modelRadius)

   where:

   screenPixelCount is 2 less than the larger of height or width when zoomLarge == true
   and the smaller of the two when zoomLarge == false

   modelRadius is a rough estimate of the extent of the molecule.
   This pretty much makes a model span the window.

   This is applied as part of the matrixTransform.
   
   ADDING ZOOM
   
   For zoom, we just apply a zoom factor to the default scaling:
   
   scalePixelsPerAngstrom = zoom * defaultScalePixelsPerAngstrom
   
   
   ADDING PERSPECTIVE
   
   Imagine an old fashioned plate camera. The film surface is in front of the model 
   some distance. Lines of perspective go from the plate (our screen) to infinity 
   behind the model. We define:
   
   cameraDistance  -- the distance of the camera in pixels from the FRONT of the model.
   
   cameraDepth     -- a more scalable version of cameraDistance, 
   measured in multiples of screenPixelCount.
   
   The atom position is transformed into screen-based coordinates as:
   
   Z = modelCenterOffset + atom.z * zoom * defaultScalePixelsPerAngstrom

   where 
   
   modelCenterOffset = cameraDistance + screenPixelCount / 2
   
   Z is thus adjusted for zoom such that the center of the model stays in the same position.     
   Defining the position of a vertical plane p as:
   
   p = (modelRadius + zoom * atom.z) / (2 * modelRadius)

   and using the definitions above, we have:

   Z = cameraDistance + screenPixelCount / 2
   + zoom * atom.z * screenPixelCount / (2 * modelRadius)
   
   or, more simply:      
   
   Z = cameraDistance + p * screenPixelCount
   
   This will prove convenient for this discussion (but is never done in code).
   
   All perspective is, is the multiplication of the x and y coordinates by a scaling
   factor that depends upon this screen-based Z coordinate.
   
   We define:
   
   cameraScaleFactor = (cameraDepth + 0.5) / cameraDepth
   referencePlaneOffset = cameraDistance * cameraScaleFactor
   = (cameraDepth + 0.5) * screenPixelCount
   
   and the overall scaling as a function of distance from the camera is simply:
   
   f = perspectiveFactor = referencePlaneOffset / Z
   
   and thus using c for cameraDepth:

   f = (c + 0.5) * screenPixelCount / Z
   = (c + 0.5) * screenPixelCount / (c * screenPixelCount + p * screenPixelCount)
   
   and we simply have:
   
   f = (c + 0.5) / (c + p)
   
   Thus:

   when p = 0,   (front plane) the scale is cameraScaleFactor.
   when p = 0.5, (midplane) the scale is 1.
   when p = 1,   (rear plane) the scale is (cameraDepth + 0.5) / (cameraDepth + 1)
   
   as p approaches infinity, perspectiveFactor goes to 0; 
   if p goes negative, we ignore it. Those points won't be rendered.

   GRAPHICAL INTERPRETATION 
   
   The simplest way to see what is happening is to consider 1/f instead of f:
   
   1/f = (c + p) / (c + 0.5) = c / (c + 0.5) + p / (c + 0.5)
   
   This is a linear function of p, with 1/f=0 at p = -c, the camera position:
   
   
   
   \----------------0----------------/    midplane, p = 0.5, 1/f = 1
    \        model center           /     viewingRange = screenPixelCount
     \                             /
      \                           /
       \                         /
        \-----------------------/   front plane, p = 0, 1/f = c / (c + 0.5)
         \                     /    viewingRange = screenPixelCount / f
          \                   /
           \                 /
            \               /   The distance across is the distance that is viewable
             \             /    for this Z position. Just magnify a model and place its
              \           /     center at 0. Whatever part of the model is within the
               \         /      triangle will be viewed, scaling each distance so that
                \       /       it ends up screenWidthPixels wide.
                 \     /
                  \   /
                   \ /
                    X  camera position, p = -c, 1/f = 0
                       viewingRange = 0

   VISUAL RANGE
   
   We simply define a fixed visual range that can be seen by the observer. 
   That range is set at the referencePlaneOffset. Any point ahead of this plane is not shown. 

   VERSION 10
   
   In Jmol 10.2 there was a much more complicated formula for perspectiveFactor, namely
   (where "c" is the cameraDepth):
   
   cameraScaleFactor(old) = 1 + 0.5 / c + 0.02
   z = cameraDistance + (modelRadius + z0) * scalePixelsPerAngstrom * cameraScaleFactor * zoom

   Note that the zoom was being applied in such a way that changing the zoom also changed the
   model midplane position and that the camera scaling factor was being applied in the 
   matrix transformation. This lead to a very complicated but subtle error in perspective.
   
   This error was noticed by Charles Xie and amounts to only a few percent for the 
   cameraDepth that was fixed at 3 in Jmol 10.0. The error was 0 at the front of the model, 
   2% at the middle, and 3.5% at the back, roughly.

   Fixing this error now allows us to adjust cameraDepth at will and to do proper navigation.

   */

  protected boolean perspectiveDepth = true;
  protected boolean scale3D = false;
  protected float cameraDepth = Float.NaN;
  protected float cameraDepthSetting = 3f;
  protected float visualRange; // set in stateManager to 5f;
  protected float cameraDistance = 1000f; // prevent divide by zero on startup

  /**
   * This method returns data needed by the VRML, X3D, and IDTF/U3D exporters It
   * also should serve as a valuable resource for anyone adapting Jmol and
   * wanting to know how the Jmol 11+ camera business works.
   * 
   * @return a set of camera data
   */
  P3[] getCameraFactors() {
    aperatureAngle = (float) (Math.atan2(screenPixelCount / 2f,
        referencePlaneOffset) * 2 * 180 / Math.PI);
    cameraDistanceFromCenter = referencePlaneOffset / scalePixelsPerAngstrom;

    P3 ptRef = P3.new3(screenWidth / 2, screenHeight / 2,
        referencePlaneOffset);
    unTransformPoint(ptRef, ptRef);

    // NOTE: Camera position will be approximate.
    // when the model has been shifted with CTRL-ALT
    // the center of distortion is not the screen center.
    // The simpler perspective model in VRML and U3D 
    // doesn't allow for that. (of course, one could argue,
    // that's because they are more REALISTIC). We do it
    // this way so that visual metrics in the model are preserved 
    // when the model is shifted using CTRL-ALT, and it was found
    // that if you didn't do that, moving the model was very odd
    // in that a fish-eye distortion was present as you moved it.

    // note that navigation mode should be EXACTLY reproduced
    // in these renderers. 

    P3 ptCamera = P3.new3(screenWidth / 2, screenHeight / 2, 0);
    viewer.unTransformPoint(ptCamera, ptCamera);
    ptCamera.sub(fixedRotationCenter);
    P3 pt = P3.new3(screenWidth / 2, screenHeight / 2,
        cameraDistanceFromCenter * scalePixelsPerAngstrom);
    viewer.unTransformPoint(pt, pt);
    pt.sub(fixedRotationCenter);
    ptCamera.add(pt);
    /*
        System.out.println("TM no " + navigationOffset + " rpo "
            + referencePlaneOffset + " aa " + aperatureAngle + " sppa "
            + scalePixelsPerAngstrom + " vr " + visualRange + " sw/vr "
            + screenWidth / visualRange + " " + ptRef + " " + fixedRotationCenter);
    */
    return new P3[] {
        ptRef,
        ptCamera,
        fixedRotationCenter,
        P3.new3(cameraDistanceFromCenter, aperatureAngle,
            scalePixelsPerAngstrom) };
  }

  int getFrontPlane() {
    return (int) cameraDistance;
  }

  void setPerspectiveDepth(boolean perspectiveDepth) {
    if (this.perspectiveDepth == perspectiveDepth)
      return;
    this.perspectiveDepth = perspectiveDepth;
    resetFitToScreen(false);
  }

  boolean getPerspectiveDepth() {
    return perspectiveDepth;
  }

  /**
   * either as a percent -300, or as a float 3.0 note this percent is of
   * zoom=100 size of model
   * 
   * @param percent
   */
  void setCameraDepthPercent(float percent) {
    resetNavigationPoint(true);
    float screenMultiples = (percent < 0 ? -percent / 100 : percent);
    if (screenMultiples == 0)
      return;
    cameraDepthSetting = screenMultiples;
    cameraDepth = Float.NaN;
  }

  void setVisualRange(float angstroms) {
    visualRange = angstroms;
  }

  Matrix4f getUnscaledTransformMatrix() {
    //for povray only
    Matrix4f unscaled = new Matrix4f();
    unscaled.setIdentity();
    vectorTemp.setT(fixedRotationCenter);
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    unscaled.sub(matrixTemp);
    matrixTemp.setM3(matrixRotate);
    unscaled.mul2(matrixTemp, unscaled);
    return unscaled;
  }

  /* ***************************************************************
   * SCREEN SCALING
   ****************************************************************/
  public int width;

  public int height;
  public int screenPixelCount;
  float scalePixelsPerAngstrom;
  public float scaleDefaultPixelsPerAngstrom;
  float scale3DAngstromsPerInch;
  protected boolean antialias;
  private boolean useZoomLarge;

  int screenWidth, screenHeight;

  private void setScreenParameters0(int screenWidth, int screenHeight,
                                    boolean useZoomLarge, boolean antialias,
                                    boolean resetSlab, boolean resetZoom) {
    if (screenWidth == Integer.MAX_VALUE)
      return;
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
    this.useZoomLarge = useZoomLarge;
    this.antialias = antialias;
    width = (antialias ? screenWidth * 2 : screenWidth);
    height = (antialias ? screenHeight * 2 : screenHeight);
    scaleFitToScreen(false, useZoomLarge, resetSlab, resetZoom);
    finalizeTransformParameters();
  }

  void setAntialias(boolean TF) {
    boolean isNew = (antialias != TF);
    antialias = TF;
    width = (antialias ? screenWidth * 2 : screenWidth);
    height = (antialias ? screenHeight * 2 : screenHeight);
    if (isNew)
      scaleFitToScreen(false, useZoomLarge, false, false);
  }

  public float defaultScaleToScreen(float radius) {
    /* 
     * 
     * the presumption here is that the rotation center is at pixel
     * (150,150) of a 300x300 window. modelRadius is
     * a rough estimate of the furthest distance from the center of rotation
     * (but not including pmesh, special lines, planes, etc. -- just atoms)
     * 
     * also that we do not want it to be possible for the model to rotate
     * out of bounds of the applet. For internal spinning I had to turn
     * of any calculation that would change the rotation radius.  hansonr
     * 
     */
    return screenPixelCount / 2f / radius;
  }

  private void resetFitToScreen(boolean andCenter) {
    scaleFitToScreen(andCenter, viewer.getZoomLarge(), true, true);
  }

  void scaleFitToScreen(boolean andCenter, boolean zoomLarge,
                        boolean resetSlab, boolean resetZoom) {
    if (width == 0 || height == 0) {
      screenPixelCount = 1;
    } else {

      // translate to the middle of the screen
      fixedTranslation.set(width * (andCenter ? 0.5f : xTranslationFraction),
          height * (andCenter ? 0.5f : yTranslationFraction), 0);
      setTranslationFractions();
      if (resetZoom)
        resetNavigationPoint(resetSlab);
      // 2005 02 22
      // switch to finding larger screen dimension
      // find smaller screen dimension
      screenPixelCount = (zoomLarge == (height > width) ? height : width);
    }
    // ensure that rotations don't leave some atoms off the screen
    // note that this radius is to the furthest outside edge of an atom
    // given the current VDW radius setting. it is currently *not*
    // recalculated when the vdw radius settings are changed
    // leave a very small margin - only 1 on top and 1 on bottom
    if (screenPixelCount > 2)
      screenPixelCount -= 2;
    scaleDefaultPixelsPerAngstrom = defaultScaleToScreen(modelRadius);
  }

  short scaleToScreen(int z, int milliAngstroms) {
    if (milliAngstroms == 0 || z < 2)
      return 0;
    int pixelSize = (int) scaleToPerspective(z, milliAngstroms
        * scalePixelsPerAngstrom / 1000);
    return (short) (pixelSize > 0 ? pixelSize : 1);
  }

  float unscaleToScreen(float z, float screenDistance) {
    float d = screenDistance / scalePixelsPerAngstrom;
    return (perspectiveDepth ? d / getPerspectiveFactor(z) : d);
  }

  float scaleToPerspective(int z, float sizeAngstroms) {
    //DotsRenderer only
    //old: return (perspectiveDepth ? sizeAngstroms * perspectiveFactor(z)
    //: sizeAngstroms);

    return (perspectiveDepth ? sizeAngstroms * getPerspectiveFactor(z)
        : sizeAngstroms);

  }

  /* ***************************************************************
   * TRANSFORMATIONS
   ****************************************************************/

  public final Matrix4f matrixTransform = new Matrix4f();
  public final Matrix4f matrixTransformInv = new Matrix4f();

  Matrix4f getMatrixtransform() {
    return matrixTransform;
  }

  protected final P3 point3fScreenTemp = new P3();
  protected final P3i point3iScreenTemp = new P3i();

  private final P3 point3fVibrationTemp = new P3();

  public boolean navigating = false;
  protected final static int MODE_STANDARD = 0;
  public final static int MODE_NAVIGATION = 1;
  protected final static int MODE_PERSPECTIVE_CENTER = 2;

  static final int DEFAULT_PERSPECTIVE_MODEL = 11;

  public int mode = MODE_STANDARD;
  public int defaultMode = MODE_STANDARD;

  void setNavigationMode(boolean TF) {
    if (TF && canNavigate())
      mode = MODE_NAVIGATION;
    else
      mode = defaultMode;
    resetNavigationPoint(true);
  }

  boolean isNavigating() {
    return navigating || navOn;
  }

  public synchronized void finalizeTransformParameters() {
    haveNotifiedNaN = false;
    fixedRotationOffset.setT(fixedTranslation);
    internalSlab = slabEnabled && (slabPlane != null || depthPlane != null);
    float newZoom = getZoomSetting();
    if (zoomPercent != newZoom) {
      zoomPercent = newZoom;
      if (!viewer.getFontCaching())
        viewer.getGraphicsData().clearFontCache();
    }
    calcCameraFactors();
    calcTransformMatrix();
    if (mode == MODE_NAVIGATION)
      calcNavigationPoint();
    else
      calcSlabAndDepthValues();
  }

  float getZoomSetting() {
    if (zoomPercentSetting < 5)
      zoomPercentSetting = 5;
    if (zoomPercentSetting > MAXIMUM_ZOOM_PERCENTAGE)
      zoomPercentSetting = MAXIMUM_ZOOM_PERCENTAGE;
    return (zoomEnabled || mode == MODE_NAVIGATION ? zoomPercentSetting : 100);
  }

  /**
   * sets slab and depth, possibly using visual range considerations for setting
   * the slab-clipping plane. (slab on; slab 0)
   * 
   * superceded in navigation mode
   * 
   */

  public void calcSlabAndDepthValues() {
    if (slabRange < 1)
      slabValue = zValueFromPercent(slabPercentSetting);
    else
      slabValue = (int) Math.floor(modelCenterOffset * slabRange
          / (2 * modelRadius) * (zoomPercentSetting / 100));
    depthValue = zValueFromPercent(depthPercentSetting);
    if (zSlabPercentSetting == zDepthPercentSetting) {
      zSlabValue = slabValue;
      zDepthValue = depthValue;
    } else {
      zSlabValue = zValueFromPercent(zSlabPercentSetting);
      zDepthValue = zValueFromPercent(zDepthPercentSetting);
    }
    if (zSlabPoint != null) {
      try {
        transformPoint2(zSlabPoint, pointT2);
        zSlabValue = (int) pointT2.z;
      } catch (Exception e) {
        // don't care
      }
    }

    viewer.getGlobalSettings().setS("_slabPlane",
        Escape.e(getSlabDepthPlane(false)));
    viewer.getGlobalSettings().setS("_depthPlane",
        Escape.e(getSlabDepthPlane(true)));
    if (slabEnabled)
      return;
    slabValue = 0;
    depthValue = Integer.MAX_VALUE;
  }

  int zValueFromPercent(int zPercent) {
    return (int) Math.floor((1 - zPercent / 50f) * modelRadiusPixels
        + modelCenterOffset);
  }

  public synchronized void calcTransformMatrix() {
    matrixTransform.setIdentity();

    // first, translate the coordinates back to the center

    vectorTemp.setT(fixedRotationCenter);
    vectorTemp.sub(frameOffset);
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    matrixTransform.sub(matrixTemp);

    // multiply by angular rotations
    // this is *not* the same as  matrixTransform.mul(matrixRotate);
    matrixTemp.setM3(stereoFrame ? matrixStereo : matrixRotate);
    matrixTransform.mul2(matrixTemp, matrixTransform);
    // cale to screen coordinates
    matrixTemp.setIdentity();
    matrixTemp.m00 = matrixTemp.m11 = matrixTemp.m22 = scalePixelsPerAngstrom;
    // negate y (for screen) and z (for zbuf)
    matrixTemp.m11 = matrixTemp.m22 = -scalePixelsPerAngstrom;

    matrixTransform.mul2(matrixTemp, matrixTransform);
    //z-translate to set rotation center at midplane (Nav) or front plane (V10)
    matrixTransform.m23 += modelCenterOffset;
    try {
      matrixTransformInv.invertM(matrixTransform);
    } catch (Exception e) {
      System.out.println("ERROR INVERTING matrixTransform!");
      // ignore -- this is a Mac issue on applet startup
    }
    //System.out.println(matrixTransform + " \n\n " + matrixTransformInv + "\n=---\n");
    // note that the image is still centered at 0, 0 in the xy plane

  }

  void rotatePoint(P3 pt, P3 ptRot) {
    matrixRotate.transform2(pt, ptRot);
    ptRot.y = -ptRot.y;
  }

  void transformPoints(int count, P3[] angstroms, P3i[] screens) {
    for (int i = count; --i >= 0;)
      screens[i].setT(transformPoint(angstroms[i]));
  }

  void transformPointScr(P3 pointAngstroms, P3i pointScreen) {
    pointScreen.setT(transformPoint(pointAngstroms));
  }

  void transformPointNoClip2(P3 pointAngstroms, P3 pointScreen) {
    pointScreen.setT(transformPointNoClip(pointAngstroms));
  }

  /**
   * CAUTION! returns a POINTER TO A TEMPORARY VARIABLE
   * 
   * @param pointAngstroms
   * @return POINTER TO point3iScreenTemp
   */
  synchronized P3i transformPoint(P3 pointAngstroms) {
    if (pointAngstroms.z == Float.MAX_VALUE
        || pointAngstroms.z == -Float.MAX_VALUE)
      return transformScreenPoint(pointAngstroms);
    matrixTransform.transform2(pointAngstroms, point3fScreenTemp);
    adjustTemporaryScreenPoint();
    if (internalSlab && checkInternalSlab(pointAngstroms))
      point3iScreenTemp.z = 1;
    return point3iScreenTemp;
  }

  private final P3 pointTsp = new P3();

  private P3i transformScreenPoint(P3 ptXyp) {
    // just does the processing for [x y] and [x y %]
    if (ptXyp.z == -Float.MAX_VALUE) {
      point3iScreenTemp.x = (int) Math.floor(ptXyp.x / 100 * screenWidth);
      point3iScreenTemp.y = (int) Math
          .floor((1 - ptXyp.y / 100) * screenHeight);
    } else {
      point3iScreenTemp.x = (int) ptXyp.x;
      point3iScreenTemp.y = (screenHeight - (int) ptXyp.y);
    }
    if (antialias) {
      point3iScreenTemp.x <<= 1;
      point3iScreenTemp.y <<= 1;
    }
    matrixTransform.transform2(fixedRotationCenter, pointTsp);
    point3iScreenTemp.z = (int) pointTsp.z;
    return point3iScreenTemp;
  }

  /**
   * CAUTION! returns a POINTER TO A TEMPORARY VARIABLE
   * 
   * @param pointAngstroms
   * @return POINTER TO point3iScreenTemp
   */
  synchronized P3 transformPointNoClip(P3 pointAngstroms) {
    matrixTransform.transform2(pointAngstroms, point3fScreenTemp);
    adjustTemporaryScreenPoint();
    return point3fScreenTemp;
  }

  /**
   * @param pointAngstroms
   * @param vibrationVector
   * @return POINTER TO TEMPORARY VARIABLE (caution!) point3iScreenTemp
   */
  P3i transformPointVib(P3 pointAngstroms, V3 vibrationVector) {
    point3fVibrationTemp.setT(pointAngstroms);
    if (vibrationOn && vibrationVector != null)
      point3fVibrationTemp.scaleAdd2(vibrationAmplitude, vibrationVector,
          pointAngstroms);
    matrixTransform.transform2(point3fVibrationTemp, point3fScreenTemp);
    adjustTemporaryScreenPoint();
    if (internalSlab && checkInternalSlab(pointAngstroms))
      point3iScreenTemp.z = 1;
    return point3iScreenTemp;
  }

  public void transformPoint2(P3 pointAngstroms, P3 screen) {
    matrixTransform.transform2(pointAngstroms, point3fScreenTemp);
    adjustTemporaryScreenPoint();
    if (internalSlab && checkInternalSlab(pointAngstroms))
      point3fScreenTemp.z = 1;
    screen.setT(point3fScreenTemp);
  }

  void transformVector(V3 vectorAngstroms, V3 vectorTransformed) {
    //dots renderer, geodesic only
    matrixTransform.transformV2(vectorAngstroms, vectorTransformed);
  }

  final protected P3 untransformedPoint = new P3();

  public void unTransformPoint(P3 screenPt, P3 coordPt) {
    //draw move2D
    untransformedPoint.setT(screenPt);
    switch (mode) {
    case MODE_NAVIGATION:
      untransformedPoint.x -= navigationOffset.x;
      untransformedPoint.y -= navigationOffset.y;
      break;
    case MODE_PERSPECTIVE_CENTER:
      untransformedPoint.x -= perspectiveOffset.x;
      untransformedPoint.y -= perspectiveOffset.y;
      break;
    case MODE_STANDARD:
      untransformedPoint.x -= fixedRotationOffset.x;
      untransformedPoint.y -= fixedRotationOffset.y;
    }
    if (perspectiveDepth) {
      float factor = getPerspectiveFactor(untransformedPoint.z);
      untransformedPoint.x /= factor;
      untransformedPoint.y /= factor;
    }
    switch (mode) {
    case MODE_NAVIGATION:
      untransformedPoint.x += navigationShiftXY.x;
      untransformedPoint.y += navigationShiftXY.y;
      break;
    case MODE_PERSPECTIVE_CENTER:
      untransformedPoint.x += perspectiveShiftXY.x;
      untransformedPoint.y += perspectiveShiftXY.y;
      break;
    }
    matrixTransformInv.transform2(untransformedPoint, coordPt);
  }

  /* ***************************************************************
   * move/moveTo support
   ****************************************************************/

  void move(JmolScriptEvaluator eval, V3 dRot, float dZoom, V3 dTrans,
            float dSlab, float floatSecondsTotal, int fps) {

    MoveThread motion = new MoveThread(this, viewer);
    motion.set(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
    if (floatSecondsTotal >0)
      motion.setEval(eval);
    motion.run();
  }

  protected final P3 ptTest1 = new P3();
  protected final P3 ptTest2 = new P3();
  protected final P3 ptTest3 = new P3();
  protected final AxisAngle4f aaTest1 = new AxisAngle4f();
  protected final Matrix3f matrixTest = new Matrix3f();

  boolean isInPosition(V3 axis, float degrees) {
    if (Float.isNaN(degrees))
      return true;
    aaTest1.setVA(axis, (float) (degrees / degreesPerRadian));
    ptTest1.set(4.321f, 1.23456f, 3.14159f);
    getRotation(matrixTest);
    matrixTest.transform2(ptTest1, ptTest2);
    matrixTest.setAA(aaTest1);
    matrixTest.transform2(ptTest1, ptTest3);
    return (ptTest3.distance(ptTest2) < 0.1);
  }

  public MoveToThread motion;

  // from Viewer
  void moveTo(JmolScriptEvaluator eval, float floatSecondsTotal, P3 center,
              Tuple3f rotAxis, float degrees, Matrix3f matrixEnd, float zoom,
              float xTrans, float yTrans, float newRotationRadius,
              P3 navCenter, float xNav, float yNav, float navDepth) {
    if (matrixEnd == null) {
      matrixEnd = new Matrix3f();
      V3 axis = V3.newV(rotAxis);
      if (Float.isNaN(degrees)) {
        matrixEnd.m00 = Float.NaN;
      } else if (degrees < 0.01f && degrees > -0.01f) {
        // getRotation(matrixEnd);
        matrixEnd.setIdentity();
      } else {
        if (axis.x == 0 && axis.y == 0 && axis.z == 0) {
          // invalid ... no rotation
          /*
           * why were we then sleeping? int sleepTime = (int) (floatSecondsTotal
           * * 1000) - 30; if (sleepTime > 0) { try { Thread.sleep(sleepTime); }
           * catch (InterruptedException ie) { } }
           */
          return;
        }
        AxisAngle4f aaMoveTo = new AxisAngle4f();
        aaMoveTo.setVA(axis, (float) (degrees / degreesPerRadian));
        matrixEnd.setAA(aaMoveTo);
      }
    }
    try {
      if (motion == null)
        motion = new MoveToThread(this, viewer);
      int nSteps = motion.set(floatSecondsTotal, center, matrixEnd, zoom,
          xTrans, yTrans, newRotationRadius, navCenter, xNav, yNav, navDepth);
      if (nSteps <= 0 || viewer.waitForMoveTo()) {
        if (nSteps > 0)
          motion.setEval(eval);
        motion.run();
        if (!viewer.isSingleThreaded)
          motion = null;
      } else {
        motion.start();
      }
    } catch (Exception e) {
      // ignore
    }
  }

  public void stopMotion() {
    motion = null;
    //setSpinOff();// trouble here with Viewer.checkHalt
  }

  Quaternion getRotationQuaternion() {
    return Quaternion.newM(matrixRotate);
    /*
    axisangleT.set(matrixRotate);
    float degrees = (float) (axisangleT.angle * degreesPerRadian);
    vectorT.set(axisangleT.x, axisangleT.y, axisangleT.z);
    return new Quaternion(vectorT, degrees);
    */
  }

  String getRotationText() {
    axisangleT.setM(matrixRotate);
    float degrees = (float) (axisangleT.angle * degreesPerRadian);
    SB sb = new SB();
    vectorT.set(axisangleT.x, axisangleT.y, axisangleT.z);
    if (degrees < 0.01f)
      return "{0 0 1 0}";
    vectorT.normalize();
    vectorT.scale(1000);
    sb.append("{");
    truncate0(sb, vectorT.x);
    truncate0(sb, vectorT.y);
    truncate0(sb, vectorT.z);
    truncate2(sb, degrees);
    sb.append("}");
    return sb.toString();
  }

  String getMoveToText(float timespan, boolean addComments) {
    SB sb = new SB();
    sb.append("moveto ");
    if (addComments)
      sb.append("/* time, axisAngle */ ");
    sb.appendF(timespan);
    sb.append(" ").append(getRotationText());
    if (addComments)
      sb.append(" /* zoom, translation */ ");
    truncate2(sb, zoomPercentSetting);
    truncate2(sb, getTranslationXPercent());
    truncate2(sb, getTranslationYPercent());
    sb.append(" ");
    if (addComments)
      sb.append(" /* center, rotationRadius */ ");
    sb.append(getCenterText());
    sb.append(" ").appendF(modelRadius);
    sb.append(getNavigationText(addComments));
    sb.append(";");
    return sb.toString();
  }

  private String getCenterText() {
    return Escape.eP(fixedRotationCenter);
  }

  private String getRotateXyzText() {
    SB sb = new SB();
    float m20 = matrixRotate.m20;
    float rY = -(float) (Math.asin(m20) * degreesPerRadian);
    float rX, rZ;
    if (m20 > .999f || m20 < -.999f) {
      rX = -(float) (Math.atan2(matrixRotate.m12, matrixRotate.m11) * degreesPerRadian);
      rZ = 0;
    } else {
      rX = (float) (Math.atan2(matrixRotate.m21, matrixRotate.m22) * degreesPerRadian);
      rZ = (float) (Math.atan2(matrixRotate.m10, matrixRotate.m00) * degreesPerRadian);
    }
    sb.append("reset");
    sb.append(";center ").append(getCenterText());
    if (rX != 0) {
      sb.append("; rotate x");
      truncate2(sb, rX);
    }
    if (rY != 0) {
      sb.append("; rotate y");
      truncate2(sb, rY);
    }
    if (rZ != 0) {
      sb.append("; rotate z");
      truncate2(sb, rZ);
    }
    sb.append(";");
    addZoomTranslationNavigationText(sb);
    return sb.toString();
  }

  private void addZoomTranslationNavigationText(SB sb) {
    if (zoomPercent != 100) {
      sb.append(" zoom");
      truncate2(sb, zoomPercent);
      sb.append(";");
    }
    float tX = getTranslationXPercent();
    if (tX != 0) {
      sb.append(" translate x");
      truncate2(sb, tX);
      sb.append(";");
    }
    float tY = getTranslationYPercent();
    if (tY != 0) {
      sb.append(" translate y");
      truncate2(sb, tY);
      sb.append(";");
    }
    if (modelRadius != rotationRadiusDefault || modelRadius == 10) {
      // after ZAP;load APPEND   we need modelRadius, which is 10
      sb.append(" set rotationRadius");
      truncate2(sb, modelRadius);
      sb.append(";");
    }
    if (mode == MODE_NAVIGATION) {
      sb.append("navigate 0 center ").append(Escape.eP(navigationCenter));
      sb.append(";navigate 0 translate");
      truncate2(sb, getNavigationOffsetPercent('X'));
      truncate2(sb, getNavigationOffsetPercent('Y'));
      sb.append(";navigate 0 depth ");
      truncate2(sb, getNavigationDepthPercent());
      sb.append(";");
    }
  }

  private String getRotateZyzText(boolean iAddComment) {
    SB sb = new SB();
    Matrix3f m = (Matrix3f) viewer
        .getModelSetAuxiliaryInfoValue("defaultOrientationMatrix");
    if (m == null) {
      m = matrixRotate;
    } else {
      m = Matrix3f.newM(m);
      m.invert();
      m.mul2(matrixRotate, m);
    }
    float m22 = m.m22;
    float rY = (float) (Math.acos(m22) * degreesPerRadian);
    float rZ1, rZ2;
    if (m22 > .999f || m22 < -.999f) {
      rZ1 = (float) (Math.atan2(m.m10, m.m11) * degreesPerRadian);
      rZ2 = 0;
    } else {
      rZ1 = (float) (Math.atan2(m.m21, -m.m20) * degreesPerRadian);
      rZ2 = (float) (Math.atan2(m.m12, m.m02) * degreesPerRadian);
    }
    if (rZ1 != 0 && rY != 0 && rZ2 != 0 && iAddComment)
      sb.append("#Follows Z-Y-Z convention for Euler angles\n");
    sb.append("reset");
    sb.append(";center ").append(getCenterText());
    if (rZ1 != 0) {
      sb.append("; rotate z");
      truncate2(sb, rZ1);
    }
    if (rY != 0) {
      sb.append("; rotate y");
      truncate2(sb, rY);
    }
    if (rZ2 != 0) {
      sb.append("; rotate z");
      truncate2(sb, rZ2);
    }
    sb.append(";");
    addZoomTranslationNavigationText(sb);
    return sb.toString();
  }

  static private void truncate0(SB sb, float val) {
    sb.appendC(' ');
    sb.appendI(Math.round(val));
  }

  static private void truncate2(SB sb, float val) {
    sb.appendC(' ');
    sb.appendF(Math.round(val * 100) / 100f);
  }

  /* ***************************************************************
   * Spin support
   ****************************************************************/

  void setSpinXYZ(float x, float y, float z) {
    if (!Float.isNaN(x))
      spinX = x;
    if (!Float.isNaN(y))
      spinY = y;
    if (!Float.isNaN(z))
      spinZ = z;
    if (isSpinInternal || isSpinFixed)
      clearSpin();
  }

  void setSpinFps(int value) {
    if (value <= 0)
      value = 1;
    else if (value > 50)
      value = 50;
    spinFps = value;
  }

  void setNavXYZ(float x, float y, float z) {
    if (!Float.isNaN(x))
      navX = x;
    if (!Float.isNaN(y))
      navY = y;
    if (!Float.isNaN(z))
      navZ = z;
  }

  private void clearSpin() {
    setSpinOff();
    setNavOn(false);
    isSpinInternal = false;
    isSpinFixed = false;
    //back to the Chime defaults
  }

  public boolean spinOn;

  boolean getSpinOn() {
    return spinOn;
  }

  public boolean navOn;

  boolean getNavOn() {
    return navOn;
  }

  private SpinThread spinThread;

  public void setSpinOn() {
    setSpin(null, true, Float.MAX_VALUE, null, null, false);
  }

  public void setSpinOff() {
    setSpin(null, false, Float.MAX_VALUE, null, null, false);
  }

  private void setSpin(JmolScriptEvaluator eval, boolean spinOn, float endDegrees,
                       JmolList<P3> endPositions, BS bsAtoms,
                       boolean isGesture) {
    if (navOn && spinOn)
      setNavOn(false);
    this.spinOn = spinOn;
    viewer.getGlobalSettings().setB("_spinning", spinOn);
    if (spinOn) {
      if (spinThread == null) {
        spinThread = new SpinThread(this, viewer, endDegrees, endPositions,
            bsAtoms, false, isGesture);
        spinThread.setEval(eval);
        if (bsAtoms == null) {
          spinThread.start();
        } else {
          spinThread.run();
        }
      }
    } else if (spinThread != null) {
      spinThread.reset();
      spinThread = null;
    }
  }

  public void setNavOn(boolean navOn) {
    if (Float.isNaN(navFps))
      return;
    boolean wasOn = this.navOn;
    if (navOn && spinOn)
      setSpin(null, false, 0, null, null, false);
    this.navOn = navOn;
    viewer.getGlobalSettings().setB("_navigating", navOn);
    if (!navOn)
      navInterrupt();
    if (navOn) {
      if (navX == 0 && navY == 0 && navZ == 0)
        navZ = 1;
      if (navFps == 0)
        navFps = 10;
      if (spinThread == null) {
        spinThread = new SpinThread(this, viewer, 0, null, null, true, false);
        spinThread.start();
      }
    } else if (wasOn) {
      if (spinThread != null) {
        spinThread.interrupt();
        spinThread = null;
      }
    }
  }

  boolean vibrationOn;
  float vibrationPeriod;
  public int vibrationPeriodMs;
  private float vibrationAmplitude;
  private float vibrationRadians;
  private float vibrationScale;

  void setVibrationScale(float scale) {
    vibrationScale = scale;
  }

  /**
   * sets the period of vibration -- period > 0: sets the period and turns
   * vibration on -- period < 0: sets the period but does not turn vibration on
   * -- period = 0: sets the period to zero and turns vibration off -- period
   * Float.NaN: uses current setting (frame change)
   * 
   * @param period
   */
  void setVibrationPeriod(float period) {
    if (Float.isNaN(period)) {
      // NaN -- new frame check
      period = vibrationPeriod;
    } else if (period == 0) {
      vibrationPeriod = 0;
      vibrationPeriodMs = 0;
    } else {
      vibrationPeriod = Math.abs(period);
      vibrationPeriodMs = (int) (vibrationPeriod * 1000);
      if (period > 0)
        return;
      period = -period;
    }
    setVibrationOn(period > 0
        && viewer.modelHasVibrationVectors(viewer.getCurrentModelIndex()));
  }

  public void setVibrationT(float t) {
    vibrationRadians = (float) (t * twoPI);
    if (vibrationScale == 0)
      vibrationScale = viewer.getVibrationScale();
    vibrationAmplitude = (float) Math.cos(vibrationRadians) * vibrationScale;
  }

  private VibrationThread vibrationThread;

  boolean isVibrationOn() {
    return vibrationOn;
  }

  private void setVibrationOn(boolean vibrationOn) {
    if (!vibrationOn) {
      if (vibrationThread != null) {
        vibrationThread.interrupt();
        vibrationThread = null;
      }
      this.vibrationOn = false;
      return;
    }
    if (viewer.getModelCount() < 1) {
      this.vibrationOn = false;
      return;
    }
    if (vibrationThread == null) {
      vibrationThread = new VibrationThread(this, viewer);
      vibrationThread.start();
    }
    this.vibrationOn = true;
  }

  private void clearVibration() {
    setVibrationOn(false);
    vibrationScale = 0;
  }

  EnumStereoMode stereoMode = EnumStereoMode.NONE;
  int[] stereoColors;

  void setStereoMode2(int[] twoColors) {
    stereoMode = EnumStereoMode.CUSTOM;
    stereoColors = twoColors;
  }

  void setStereoMode(EnumStereoMode stereoMode) {
    stereoColors = null;
    this.stereoMode = stereoMode;
  }

  float stereoDegrees = Float.NaN; // set in state manager
  float stereoRadians;

  void setStereoDegrees(float stereoDegrees) {
    this.stereoDegrees = stereoDegrees;
    stereoRadians = stereoDegrees * JC.radiansPerDegree;
  }

  boolean stereoFrame;

  protected final Matrix3f matrixStereo = new Matrix3f();

  synchronized Matrix3f getStereoRotationMatrix(boolean stereoFrame) {
    this.stereoFrame = stereoFrame;
    if (!stereoFrame)
      return matrixRotate;
    matrixTemp3.rotY(-stereoRadians);
    matrixStereo.mul2(matrixTemp3, matrixRotate);
    return matrixStereo;
  }

  /////////// rotation center ////////////

  //from Frame:

  public boolean windowCentered;

  boolean isWindowCentered() {
    return windowCentered;
  }

  void setWindowCentered(boolean TF) {
    windowCentered = TF;
    resetNavigationPoint(true);
  }

  P3 getRotationCenter() {
    return fixedRotationCenter;
  }

  float getRotationRadius() {
    return modelRadius;
  }

  public float setRotationRadius(float angstroms, boolean doAll) {
    angstroms = (modelRadius = (angstroms <= 0 ? viewer
        .calcRotationRadius(fixedRotationCenter) : angstroms));
    if (doAll)
      viewer.setRotationRadius(angstroms, false);
    return angstroms;
  }

  private void setRotationCenterAndRadiusXYZ(P3 newCenterOfRotation,
                                             boolean andRadius) {
    resetNavigationPoint(false);
    if (newCenterOfRotation == null) {
      setFixedRotationCenter(rotationCenterDefault);
      modelRadius = rotationRadiusDefault;
      return;
    }
    setFixedRotationCenter(newCenterOfRotation);
    if (andRadius && windowCentered)
      modelRadius = viewer.calcRotationRadius(fixedRotationCenter);
  }

  private void setRotCenterRel(String relativeTo, P3 pt) {
    P3 pt1 = P3.newP(pt);
    if (relativeTo == "average")
      pt1.add(viewer.getAverageAtomPoint());
    else if (relativeTo == "boundbox")
      pt1.add(viewer.getBoundBoxCenter());
    else if (relativeTo != "absolute")
      pt1.setT(rotationCenterDefault);
    setRotationCenterAndRadiusXYZ(pt1, true);
  }

  void setNewRotationCenter(P3 center, boolean doScale) {
    // once we have the center, we need to optionally move it to 
    // the proper XY position and possibly scale
    if (center == null)
      center = rotationCenterDefault;
    if (windowCentered) {
      translateToPercent('x', 0);
      translateToPercent('y', 0);///CenterTo(0, 0);
      setRotationCenterAndRadiusXYZ(center, true);
      if (doScale)
        resetFitToScreen(true);
    } else {
      moveRotationCenter(center, true);
    }
  }

  // from Viewer:

  public void moveRotationCenter(P3 center, boolean toXY) {
    setRotationCenterAndRadiusXYZ(center, false);
    if (toXY)
      setRotationPointXY(fixedRotationCenter);
  }

  void setCenter() {
    setRotationCenterAndRadiusXYZ(fixedRotationCenter, true);
  }

  void setCenterAt(String relativeTo, P3 pt) {
    setRotCenterRel(relativeTo, pt);
    resetFitToScreen(true);
  }

  /* ***************************************************************
   * Navigation support
   ****************************************************************/

  P3[] frameOffsets;
  final P3 frameOffset = new P3();

  void setFrameOffset(int modelIndex) {
    if (frameOffsets == null || modelIndex < 0
        || modelIndex >= frameOffsets.length)
      frameOffset.set(0, 0, 0);
    else
      frameOffset.setT(frameOffsets[modelIndex]);
  }

  void setFrameOffsets(P3[] offsets) {
    frameOffsets = offsets;
  }

  /////////// Allow during-rendering mouse operations ///////////

  BS bsSelectedAtoms;
  P3 ptOffset = new P3();

  void setSelectedTranslation(BS bsAtoms, char xyz, int xy) {
    this.bsSelectedAtoms = bsAtoms;
    switch (xyz) {
    case 'X':
    case 'x':
      ptOffset.x += xy;
      break;
    case 'Y':
    case 'y':
      ptOffset.y += xy;
      break;
    case 'Z':
    case 'z':
      ptOffset.z += xy;
      break;
    }
    //System.out.println(xyz + " " + xy + " " + ptOffset);
  }

  /////////////////////////// old TransfomManager11 //////////////////// 

  final public static int NAV_MODE_IGNORE = -2;
  final public static int NAV_MODE_ZOOMED = -1;
  final public static int NAV_MODE_NONE = 0;
  final public static int NAV_MODE_RESET = 1;
  final public static int NAV_MODE_NEWXY = 2;
  final public static int NAV_MODE_NEWXYZ = 3;
  final public static int NAV_MODE_NEWZ = 4;

  public int navMode = NAV_MODE_RESET;
  public float zoomFactor = Float.MAX_VALUE;

  public float navigationSlabOffset;

  protected void setNavFps(int navFps) {
    this.navFps = navFps;
  }

  /**
   * sets all camera and scale factors needed by the specific perspective model
   * instantiated
   * 
   */
  public void calcCameraFactors() {
    // (m) model coordinates
    // (s) screen coordinates = (m) * screenPixelsPerAngstrom
    // (p) plane coordinates = (s) / screenPixelCount

    if (Float.isNaN(cameraDepth)) {
      cameraDepth = cameraDepthSetting;
      zoomFactor = Float.MAX_VALUE;
    }

    // reference point where p=0
    cameraDistance = cameraDepth * screenPixelCount; // (s)

    // distance from camera to midPlane of model (p=0.5)
    // the factor to apply based on screen Z
    referencePlaneOffset = cameraDistance + screenPixelCount / 2f; // (s)

    // conversion factor Angstroms --> pixels
    // so that "full window" is visualRange
    scalePixelsPerAngstrom = (scale3D && !perspectiveDepth
        && mode != MODE_NAVIGATION ? 72 / scale3DAngstromsPerInch
        * (antialias ? 2 : 1) : screenPixelCount / visualRange); // (s/m)

    // model radius in pixels
    modelRadiusPixels = modelRadius * scalePixelsPerAngstrom; // (s)

    // model center offset for zoom 100
    float offset100 = (2 * modelRadius) / visualRange * referencePlaneOffset; // (s)

    //System.out.println("sppA " + scalePixelsPerAngstrom + " pD " +
    // perspectiveDepth
    // + " spC " + screenPixelCount + " vR " + visualRange
    // + " sDPPA " + scaleDefaultPixelsPerAngstrom);

    if (mode == MODE_NAVIGATION) {
      calcNavCameraFactors(offset100);
      return;
    }
    // nonNavigation mode -- to match Jmol 10.2 at midplane (caffeine.xyz)
    // flag that we have left navigation mode
    zoomFactor = Float.MAX_VALUE;
    // we place the model at the referencePlaneOffset offset and then change
    // the scale
    modelCenterOffset = referencePlaneOffset;
    // now factor the scale by distance from camera and zoom
    if (!scale3D || perspectiveDepth)
      scalePixelsPerAngstrom *= (modelCenterOffset / offset100) * zoomPercent
          / 100; // (s/m)

    //System.out.println("sppA revised:" + scalePixelsPerAngstrom);
    // so that's sppa = (spc / vR) * rPO * (vR / 2) / mR * rPO = spc/2/mR

    modelRadiusPixels = modelRadius * scalePixelsPerAngstrom; // (s)
    //System.out.println("transformman scalppa modelrad " +
    // scalePixelsPerAngstrom + " " + modelRadiusPixels + " " + visualRange);
  }

  private void calcNavCameraFactors(float offset100) {
    if (zoomFactor == Float.MAX_VALUE) {
      // entry point
      if (zoomPercent > MAXIMUM_ZOOM_PERSPECTIVE_DEPTH)
        zoomPercent = MAXIMUM_ZOOM_PERSPECTIVE_DEPTH;
      // screen offset to fixed rotation center
      modelCenterOffset = offset100 * 100 / zoomPercent;
    } else if (prevZoomSetting != zoomPercentSetting) {
      if (zoomRatio == 0) // scripted change zoom xxx
        modelCenterOffset = offset100 * 100 / zoomPercentSetting;
      else
        // fractional change by script or mouse
        modelCenterOffset += (1 - zoomRatio) * referencePlaneOffset;
      navMode = NAV_MODE_ZOOMED;
    }
    prevZoomSetting = zoomPercentSetting;
    zoomFactor = modelCenterOffset / referencePlaneOffset;
    // infinite or negative value means there is no corresponding non-navigating
    // zoom setting
    zoomPercent = (zoomFactor == 0 ? MAXIMUM_ZOOM_PERSPECTIVE_DEPTH : offset100
        / modelCenterOffset * 100);

  }

  /**
   * calculate the perspective factor based on z
   * 
   * @param z
   * @return perspectiveFactor
   */
  public float getPerspectiveFactor(float z) {
    // System.out.println (z + " getPerspectiveFactor " + referencePlaneOffset +
    // " " + (z <= 0 ? referencePlaneOffset : referencePlaneOffset / z));
    return (z <= 0 ? referencePlaneOffset : referencePlaneOffset / z);
  }

  /**
   * adjusts the temporary point for perspective and offsets
   * 
   */
  protected void adjustTemporaryScreenPoint() {

    // fixedRotation point is at the origin initially

    float z = point3fScreenTemp.z;

    // this could easily go negative -- behind the screen --
    // but we don't care. In fact, that just makes it easier,
    // because it means we won't render it.
    // we should probably assign z = 0 as "unrenderable"

    if (Float.isNaN(z)) {
      if (!haveNotifiedNaN)
        Logger.debug("NaN seen in TransformPoint");
      haveNotifiedNaN = true;
      z = 1;
    } else if (z <= 0) {
      // just don't let z go past 1 BH 11/15/06
      z = 1;
    }
    point3fScreenTemp.z = z;

    // x and y are moved inward (generally) relative to 0, which
    // is either the fixed rotation center or the navigation center

    // at this point coordinates are centered on rotation center

    switch (mode) {
    case MODE_NAVIGATION:
      // move nav center to 0; refOffset = Nav - Rot
      point3fScreenTemp.x -= navigationShiftXY.x;
      point3fScreenTemp.y -= navigationShiftXY.y;
      break;
    case MODE_PERSPECTIVE_CENTER:
      point3fScreenTemp.x -= perspectiveShiftXY.x;
      point3fScreenTemp.y -= perspectiveShiftXY.y;
      break;
    }
    if (perspectiveDepth) {
      // apply perspective factor
      float factor = getPerspectiveFactor(z);
      point3fScreenTemp.x *= factor;
      point3fScreenTemp.y *= factor;
    }
    switch (mode) {
    case MODE_NAVIGATION:
      point3fScreenTemp.x += navigationOffset.x;
      point3fScreenTemp.y += navigationOffset.y;
      break;
    case MODE_PERSPECTIVE_CENTER:
      point3fScreenTemp.x += perspectiveOffset.x;
      point3fScreenTemp.y += perspectiveOffset.y;
      break;
    case MODE_STANDARD:
      point3fScreenTemp.x += fixedRotationOffset.x;
      point3fScreenTemp.y += fixedRotationOffset.y;
      break;
    }

    if (Float.isNaN(point3fScreenTemp.x) && !haveNotifiedNaN) {
      Logger.debug("NaN found in transformPoint ");
      haveNotifiedNaN = true;
    }

    point3iScreenTemp.set((int) point3fScreenTemp.x, (int) point3fScreenTemp.y,
        (int) point3fScreenTemp.z);
  }

  boolean canNavigate() {
    return true;
  }

  /**
   * something has arisen that requires resetting of the navigation point.
   * 
   * @param doResetSlab
   */
  protected void resetNavigationPoint(boolean doResetSlab) {
    if (zoomPercent < 5 && mode != MODE_NAVIGATION) {
      perspectiveDepth = true;
      mode = MODE_NAVIGATION;
      return;
    }
    if (mode == MODE_NAVIGATION) {
      navMode = NAV_MODE_RESET;
      slabPercentSetting = 0;
      perspectiveDepth = true;
    } else if (doResetSlab) {
      slabPercentSetting = 100;
    }
    viewer.setFloatProperty("slabRange", 0);
    if (doResetSlab) {
      slabEnabled = (mode == MODE_NAVIGATION);
    }
    zoomFactor = Float.MAX_VALUE;
    zoomPercentSetting = zoomPercent;
  }

  /**
   * scripted entry point for navigation
   * 
   * @param pt
   */
  public void setNavigatePt(P3 pt) {
    // from MoveToThread
    navigationCenter.setT(pt);
    navMode = NAV_MODE_NEWXYZ;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  public P3 getNavigationCenter() {
    return navigationCenter;
  }

  public float getNavigationDepthPercent() {
    return navigationDepth;
  }

  void setNavigationSlabOffsetPercent(float percent) {
    viewer.getGlobalSettings().setF("navigationSlab", percent);
    calcCameraFactors(); // current
    navigationSlabOffset = percent / 50 * modelRadiusPixels;
  }

  P3 getNavigationOffset() {
    transformPoint2(navigationCenter, navigationOffset);
    return navigationOffset;
  }

  public float getNavPtHeight() {
    //boolean navigateSurface = viewer.getNavigateSurface();
    return height / 2f;//(navigateSurface ? 1f : 2f);
  }

  public float getNavigationOffsetPercent(char XorY) {
    getNavigationOffset();
    if (width == 0 || height == 0)
      return 0;
    return (XorY == 'X' ? (navigationOffset.x - width / 2f) * 100f / width
        : (navigationOffset.y - getNavPtHeight()) * 100f / height);
  }

  protected String getNavigationText(boolean addComments) {
    getNavigationOffset();
    return (addComments ? " /* navigation center, translation, depth */ " : " ")
        + Escape.eP(navigationCenter)
        + " "
        + getNavigationOffsetPercent('X')
        + " "
        + getNavigationOffsetPercent('Y') + " " + getNavigationDepthPercent();
  }

  void setScreenParameters(int screenWidth, int screenHeight,
                           boolean useZoomLarge, boolean antialias,
                           boolean resetSlab, boolean resetZoom) {
    P3 pt = (mode == MODE_NAVIGATION ? P3.newP(navigationCenter)
        : null);
    P3 ptoff = P3.newP(navigationOffset);
    ptoff.x = ptoff.x / width;
    ptoff.y = ptoff.y / height;
    setScreenParameters0(screenWidth, screenHeight, useZoomLarge, antialias,
        resetSlab, resetZoom);
    if (pt != null) {
      navigationCenter.setT(pt);
      navTranslatePercent(-1, ptoff.x * width, ptoff.y * height);
      setNavigatePt(pt);
    }
  }

  //////////////  optional navigation support ///////////////////////

  private JmolNavigatorInterface nav;

  private void navInterrupt() {
    if (nav != null)
      nav.interrupt();
  }
  
  private boolean getNav() {
    if (nav != null)
      return true;
    nav = (JmolNavigatorInterface) Interface
        .getOptionInterface("navigate.Navigator");
    if (nav == null)
      return false;
    nav.set(this, viewer);
    return true;
  }

  public void navigateList(JmolScriptEvaluator eval, JmolList<Object[]> list) {
    if (getNav())
      nav.navigateList(eval, list);
  }

  /**
   * scripted entry point for navigation
   * 
   * @param rotAxis
   * @param degrees
   */
  public void navigateAxis(V3 rotAxis, float degrees) {
    if (getNav())
      nav.navigateAxis(rotAxis, degrees);
  }

  void zoomByFactor(float factor, int x, int y) {
    if (mode != MODE_NAVIGATION || !zoomEnabled || factor <= 0)
      zoomByFactor0(factor, x, y);
    else if (getNav())
      nav.zoomByFactor(factor, x, y);
  }

  public void setNavigationOffsetRelative() {//boolean navigatingSurface) {
    if (getNav())
      nav.setNavigationOffsetRelative();//navigatingSurface);
  }

  /**
   * entry point for keyboard-based navigation
   * 
   * @param keyCode
   *        0 indicates key released
   * @param modifiers
   *        shift,alt,ctrl
   */
  synchronized void navigateKey(int keyCode, int modifiers) {
    if (getNav())
      nav.navigateKey(keyCode, modifiers);
  }

  /**
   * sets the position of the navigation offset relative to the model (50%
   * center; 0% rear, 100% front; can be <0 or >100)
   * 
   * @param percent
   */
  public void setNavigationDepthPercent(float percent) {
    if (getNav())
      nav.setNavigationDepthPercent(percent);
  }

  /**
   * scripted entry point for navigation
   * 
   * @param seconds
   * @param x
   * @param y
   */
  public void navTranslatePercent(float seconds, float x, float y) {
    if (getNav())
      nav.navTranslatePercent(seconds, x, y);
  }

  /**
   * All the magic happens here. all navigation effects go through this method
   * 
   */
  protected void calcNavigationPoint() {
    if (getNav())
      nav.calcNavigationPoint();
  }

  /**
   * 
   * @return the script that defines the current navigation state
   * 
   */
  protected String getNavigationState() {
    return (mode == MODE_NAVIGATION && getNav() ? nav.getNavigationState() : "");
  }

}
