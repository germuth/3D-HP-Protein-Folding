/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.thread;


import org.jmol.util.AxisAngle4f;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.P3;
import org.jmol.util.V3;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class MoveToThread extends JmolThread {
  /**
   * 
   */
  private final TransformManager transformManager;

  /**
   * @param transformManager
   * @param viewer 
   */
  public MoveToThread(TransformManager transformManager, Viewer viewer) {
    super();
    setViewer(viewer, "MoveToThread");
    this.transformManager = transformManager;
  }

  private final V3 aaStepCenter = new V3();
  private final V3 aaStepNavCenter = new V3();
  private final AxisAngle4f aaStep = new AxisAngle4f();
  private final AxisAngle4f aaTotal = new AxisAngle4f();
  private final Matrix3f matrixStart = new Matrix3f();
  private final Matrix3f matrixStartInv = new Matrix3f();
  private final Matrix3f matrixStep = new Matrix3f();
  private final Matrix3f matrixEnd = new Matrix3f();

  private P3 center;
  private float zoom; 
  private float xTrans;
  private float yTrans;
  private P3 navCenter;
  private float xNav;
  private float yNav;
  private float navDepth;
  private P3 ptMoveToCenter;
  private float startRotationRadius;
  private float targetPixelScale;
  private int totalSteps;
  private float startPixelScale;
  private float targetRotationRadius;
  private int fps;
  private float rotationRadiusDelta;
  private float pixelScaleDelta;
  private float zoomStart;
  private float zoomDelta;
  private float xTransStart;
  private float xTransDelta;
  private float yTransStart;
  private float yTransDelta;
  private float xNavTransStart;
  private float xNavTransDelta;
  private float yNavTransDelta;
  private float yNavTransStart;
  private float navDepthStart;
  private float navDepthDelta;
  private long frameTimeMillis;
  private int iStep;
  
  private boolean doEndMove;
  private float floatSecondsTotal;
  
  public int set(float floatSecondsTotal, P3 center, Matrix3f end,
                 float zoom, float xTrans, float yTrans,
                 float newRotationRadius, P3 navCenter, float xNav,
                 float yNav, float navDepth) {
    this.center = center;
    matrixEnd.setM(end);
    this.zoom = zoom;
    this.xTrans = xTrans;
    this.yTrans = yTrans;
    this.navCenter = navCenter;
    this.xNav = xNav;
    this.yNav = yNav;
    this.navDepth = navDepth;
    ptMoveToCenter = (center == null ? transformManager.fixedRotationCenter
        : center);
    startRotationRadius = transformManager.modelRadius;
    targetRotationRadius = (center == null || Float.isNaN(newRotationRadius) ? transformManager.modelRadius
        : newRotationRadius <= 0 ? viewer.calcRotationRadius(center)
            : newRotationRadius);
    startPixelScale = transformManager.scaleDefaultPixelsPerAngstrom;
    targetPixelScale = (center == null ? startPixelScale : transformManager
        .defaultScaleToScreen(targetRotationRadius));
    if (Float.isNaN(zoom))
      zoom = transformManager.zoomPercent;
    transformManager.getRotation(matrixStart);
    matrixStartInv.invertM(matrixStart);
    matrixStep.mul2(matrixEnd, matrixStartInv);
    aaTotal.setM(matrixStep);
    fps = 30;
    this.floatSecondsTotal = floatSecondsTotal;
    totalSteps = (int) (floatSecondsTotal * fps);
    if (totalSteps == 0)
      return 0;
    frameTimeMillis = 1000 / fps;
    targetTime = System.currentTimeMillis();
    zoomStart = transformManager.zoomPercent;
    zoomDelta = zoom - zoomStart;
    xTransStart = transformManager.getTranslationXPercent();
    xTransDelta = xTrans - xTransStart;
    yTransStart = transformManager.getTranslationYPercent();
    yTransDelta = yTrans - yTransStart;
    aaStepCenter.setT(ptMoveToCenter);
    aaStepCenter.sub(transformManager.fixedRotationCenter);
    aaStepCenter.scale(1f / totalSteps);
    pixelScaleDelta = (targetPixelScale - startPixelScale);
    rotationRadiusDelta = (targetRotationRadius - startRotationRadius);
    if (navCenter != null
        && transformManager.mode == TransformManager.MODE_NAVIGATION) {
      aaStepNavCenter.setT(navCenter);
      aaStepNavCenter.sub(transformManager.navigationCenter);
      aaStepNavCenter.scale(1f / totalSteps);
    }
    float xNavTransStart = transformManager.getNavigationOffsetPercent('X');
    xNavTransDelta = xNav - xNavTransStart;
    yNavTransStart = transformManager.getNavigationOffsetPercent('Y');
    yNavTransDelta = yNav - yNavTransStart;
    float navDepthStart = transformManager.getNavigationDepthPercent();
    navDepthDelta = navDepth - navDepthStart;
    return totalSteps;
  }
         
  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        if (totalSteps > 0)
          viewer.setInMotion(true);
        mode = MAIN;
        break;
      case MAIN:
        if (stopped || ++iStep >= totalSteps) {
          mode = FINISH;
          break;
        }
        doStepTransform();
        doEndMove = true;
        targetTime += frameTimeMillis;
        currentTime = System.currentTimeMillis();
        boolean doRender = (currentTime < targetTime);
        if (!doRender && isJS) {
          // JavaScript will be slow anyway -- make sure we render
          targetTime = currentTime;
          doRender = true;
        }
        if (doRender)
          viewer.requestRepaintAndWait();
        if (transformManager.motion == null || !isJS && eval != null
            && !viewer.isScriptExecuting()) {
          stopped = true;
          break;
        }
        currentTime = System.currentTimeMillis();
        int sleepTime = (int) (targetTime - currentTime);
        if (!runSleep(sleepTime, MAIN))
          return;
        mode = MAIN;
        break;
      case FINISH:
        if (totalSteps <= 0 || doEndMove && !stopped)
          doFinalTransform();
        if (totalSteps > 0)
          viewer.setInMotion(false);
        viewer.moveUpdate(floatSecondsTotal);
        if (transformManager.motion != null && !stopped) {
          transformManager.motion = null;
          viewer.finalizeTransformParameters();
        }
        resumeEval();
        return;
      }
  }

  private void doStepTransform() {
    if (!Float.isNaN(matrixEnd.m00)) {
      transformManager.getRotation(matrixStart);
      matrixStartInv.invertM(matrixStart);
      matrixStep.mul2(matrixEnd, matrixStartInv);
      aaTotal.setM(matrixStep);
      aaStep.setAA(aaTotal);
      aaStep.angle /= (totalSteps - iStep);
      if (aaStep.angle == 0)
        matrixStep.setIdentity();
      else
        matrixStep.setAA(aaStep);
      matrixStep.mul(matrixStart);
    }
    float fStep = iStep / (totalSteps - 1f);
    transformManager.modelRadius = startRotationRadius + rotationRadiusDelta
        * fStep;
    transformManager.scaleDefaultPixelsPerAngstrom = startPixelScale
        + pixelScaleDelta * fStep;
    if (!Float.isNaN(xTrans)) {
      transformManager.zoomToPercent(zoomStart + zoomDelta * fStep);
      transformManager.translateToPercent('x', xTransStart + xTransDelta
          * fStep);
      transformManager.translateToPercent('y', yTransStart + yTransDelta
          * fStep);
    }
    transformManager.setRotation(matrixStep);
    if (center != null)
      transformManager.fixedRotationCenter.add(aaStepCenter);
    if (navCenter != null
        && transformManager.mode == TransformManager.MODE_NAVIGATION) {
      P3 pt = P3.newP(transformManager.navigationCenter);
      pt.add(aaStepNavCenter);
      transformManager.setNavigatePt(pt);
      if (!Float.isNaN(xNav) && !Float.isNaN(yNav))
        transformManager
            .navTranslatePercent(0, xNavTransStart + xNavTransDelta * fStep,
                yNavTransStart + yNavTransDelta * fStep);
      if (!Float.isNaN(navDepth))
        transformManager.setNavigationDepthPercent(navDepthStart
            + navDepthDelta * fStep);
    }
  }

  private void doFinalTransform() {
    transformManager.setRotationRadius(targetRotationRadius, true);
    transformManager.scaleDefaultPixelsPerAngstrom = targetPixelScale;
    if (center != null)
      transformManager.moveRotationCenter(center,
          !transformManager.windowCentered);
    if (!Float.isNaN(xTrans)) {
      transformManager.zoomToPercent(zoom);
      transformManager.translateToPercent('x', xTrans);
      transformManager.translateToPercent('y', yTrans);
    }
    transformManager.setRotation(matrixEnd);
    if (navCenter != null
        && transformManager.mode == TransformManager.MODE_NAVIGATION) {
      transformManager.navigationCenter.setT(navCenter);
      if (!Float.isNaN(xNav) && !Float.isNaN(yNav))
        transformManager.navTranslatePercent(0, xNav, yNav);
      if (!Float.isNaN(navDepth))
        transformManager.setNavigationDepthPercent(navDepth);
    }
  }

  @Override
  public void interrupt() {
    Logger.debug("moveto thread interrupted!");
    doEndMove = false;
    super.interrupt();
  }
  

}