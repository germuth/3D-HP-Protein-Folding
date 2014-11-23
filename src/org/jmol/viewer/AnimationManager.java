/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 10:56:39 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11127 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;


import java.util.Map;

import org.jmol.thread.AnimationThread;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.JmolList;

import org.jmol.constant.EnumAnimationMode;
import org.jmol.modelset.ModelSet;

public class AnimationManager {

  protected Viewer viewer;
  
  AnimationManager(Viewer viewer) {
    this.viewer = viewer;
  }

  EnumAnimationMode animationReplayMode = EnumAnimationMode.ONCE;

  public boolean animationOn;
  boolean animationPaused;
  boolean inMotion;
  
  public int animationFps;  // set in stateManager
  int animationDirection = 1;
  int currentDirection = 1;
  public int currentModelIndex;
  int firstFrameIndex;
  int lastFrameIndex;
  int frameStep;
  public int morphCount;
   
  public int firstFrameDelayMs;
  public int lastFrameDelayMs;
  
  private int lastFramePainted;
  
  private AnimationThread animationThread;
  int backgroundModelIndex = -1;
  final BS bsVisibleFrames = new BS();
  BS bsDisplay;
  float firstFrameDelay;
  private int intAnimThread;
  float lastFrameDelay = 1;

  private Map<String, Object> movie;

  private int currentFrameIndex;
  float currentMorphFrame;

  void clear() {
    setMovie(null);
    initializePointers(0);
    setAnimationOn(false);
    setCurrentModelIndex(0, true);
    currentDirection = 1;
    setAnimationDirection(1);
    setAnimationFps(10);
    setAnimationReplayMode(EnumAnimationMode.ONCE, 0, 0);
    initializePointers(0);
  }
  
  private void setFrame(int frameIndex) {
    setCurrentFrame(frameIndex, true);
 }
  
  /**
   * @param frameIndex 
   * @param isAll  
   */
  @SuppressWarnings("unchecked")
  private void setCurrentFrame(int frameIndex, boolean isAll) {
    //System.out.println("currentframe " + frameIndex);
    if (movie == null) {
      setCurrentModelIndex(frameIndex, true);
      return;
    }
    if (frameIndex == -1)
      frameIndex = ((Integer) movie.get("currentFrame")).intValue();
    currentFrameIndex = frameIndex;
    int iState = getMovieState(frameIndex);
    if (iState < 0)
      return;
    setModelIndex(iState, true);
    JmolList<BS> states = (JmolList<BS>) movie.get("states");
    if (states == null || iState < 0 || iState >= states.size())
      return;
    BS bs = states.get(iState);
    if (bsDisplay != null) {
      bs = BSUtil.copy(bs);
      bs.and(bsDisplay);
    }
    viewer.displayAtoms(bs, true, false, null, true);
    //setViewer(true);
  }

  @SuppressWarnings("unchecked")
  private int getMovieState(int frameIndex) {
    JmolList<Object> frames = (JmolList<Object>) movie.get("frames");
    //System.out.println(frames);
    return (frames == null || frameIndex >= frames.size() ? -1
        : ((Integer) frames.get(frameIndex)).intValue());
  }

  public void morph(float frame) {
    System.out.println("morph " + frame);
    int m = (int) frame;
    if (Math.abs(m - frame) < 0.001f)
      frame = m;
    else if (Math.abs(m - frame) > 0.999f)
      frame = m = m + 1;
    float f = frame - m;
    m -= 1;
    if (f == 0) {
      currentMorphFrame = m;
      setFrame(m);
      return;
    }
    int m1;
    if (movie == null) {
      setCurrentModelIndex(m, true);
      m1 = m + 1;
      currentMorphFrame = m + f;
    } else {
      setCurrentFrame(m, false);
      currentMorphFrame = m + f;
      m = getMovieState(m);
      m1 = getMovieState(getFrameStep(animationDirection) + getCurrentFrame());
    }
    if (m1 == m || m1 < 0 || m < 0)
      return;
    viewer.modelSet.morphTrajectories(m, m1, f);
  }
  


  void setCurrentModelIndex(int modelIndex, boolean clearBackgroundModel) {
    if (movie != null) {
      setFrame(modelIndex);
      return;
    }
    currentFrameIndex = 0;
    setModelIndex(modelIndex, clearBackgroundModel);
  }

  private void setModelIndex(int modelIndex, boolean clearBackgroundModel) {
    if (modelIndex < 0)
      stopThread(false);
    int formerModelIndex = currentModelIndex;
    ModelSet modelSet = viewer.getModelSet();
    int modelCount = (modelSet == null ? 0 : modelSet.modelCount);
    if (modelCount == 1)
      currentModelIndex = modelIndex = 0;
    else if (modelIndex < 0 || modelIndex >= modelCount)
      modelIndex = -1;
    String ids = null;
    boolean isSameSource = false;
    if (currentModelIndex != modelIndex) {
      if (modelCount > 0) {
        boolean toDataFrame = isJmolDataFrameForModel(modelIndex);
        boolean fromDataFrame = isJmolDataFrameForModel(currentModelIndex);
        if (fromDataFrame)
          viewer.setJmolDataFrame(null, -1, currentModelIndex);
        if (currentModelIndex != -1)
          viewer.saveModelOrientation();
        if (fromDataFrame || toDataFrame) {
          ids = viewer.getJmolFrameType(modelIndex) 
          + " "  + modelIndex + " <-- " 
          + " " + currentModelIndex + " " 
          + viewer.getJmolFrameType(currentModelIndex);
          
          isSameSource = (viewer.getJmolDataSourceFrame(modelIndex) == viewer
              .getJmolDataSourceFrame(currentModelIndex));
        }
      }
      currentModelIndex = modelIndex;
      if (ids != null) {
        if (modelIndex >= 0)
          viewer.restoreModelOrientation(modelIndex);
        if (isSameSource && ids.indexOf("quaternion") >= 0 
            && ids.indexOf("plot") < 0
            && ids.indexOf("ramachandran") < 0
            && ids.indexOf(" property ") < 0) {
          viewer.restoreModelRotation(formerModelIndex);
        }
      }
    }
    setViewer(clearBackgroundModel);
  }

  private void setViewer(boolean clearBackgroundModel) {
    viewer.setTrajectory(currentModelIndex);
    viewer.setFrameOffset(currentModelIndex);
    if (currentModelIndex == -1 && clearBackgroundModel)
      setBackgroundModelIndex(-1);  
    viewer.setTainted(true);
    setFrameRangeVisible();
    viewer.setStatusFrameChanged(false);
    if (viewer.modelSet != null && !viewer.getSelectAllModels())
        viewer.setSelectionSubset(viewer.getModelUndeletedAtomsBitSet(currentModelIndex));
  }

  private boolean isJmolDataFrameForModel(int i) {
    return movie == null && viewer.isJmolDataFrameForModel(i);
  }

  void setBackgroundModelIndex(int modelIndex) {
    ModelSet modelSet = viewer.getModelSet();
    if (modelSet == null || modelIndex < 0 || modelIndex >= modelSet.modelCount)
      modelIndex = -1;
    backgroundModelIndex = modelIndex;
    if (modelIndex >= 0)
      viewer.setTrajectory(modelIndex);
    viewer.setTainted(true);
    setFrameRangeVisible(); 
  }
  
  private void setFrameRangeVisible() {
    bsVisibleFrames.clearAll();
    if (movie != null) {
      bsVisibleFrames.setBits(0, viewer.getModelCount());
      return;
    }
    if (backgroundModelIndex >= 0)
      bsVisibleFrames.set(backgroundModelIndex);
    if (currentModelIndex >= 0) {
      bsVisibleFrames.set(currentModelIndex);
      return;
    }
    if (frameStep == 0)
      return;
    int nDisplayed = 0;
    int frameDisplayed = 0;
    for (int i = firstFrameIndex; i != lastFrameIndex; i += frameStep)
      if (!isJmolDataFrameForModel(i)) {
        bsVisibleFrames.set(i);
        nDisplayed++;
        frameDisplayed = i;
      }
    if (firstFrameIndex == lastFrameIndex || !isJmolDataFrameForModel(lastFrameIndex)
        || nDisplayed == 0) {
      bsVisibleFrames.set(lastFrameIndex);
      if (nDisplayed == 0)
        firstFrameIndex = lastFrameIndex;
      nDisplayed = 0;
    }
    if (nDisplayed == 1 && currentModelIndex < 0)
      setFrame(frameDisplayed);
    //System.out.println(bsVisibleFrames + "  " + frameDisplayed + " " + currentModelIndex);
   
  }

  void initializePointers(int frameStep) {
    firstFrameIndex = 0;
    lastFrameIndex = (frameStep == 0 ? 0 : getFrameCount()) - 1;
    this.frameStep = frameStep;
    viewer.setFrameVariables();
  }

  int getFrameCount() {
    return (movie == null ? viewer.getModelCount() : ((Integer) movie.get("frameCount")).intValue());
  }

  void setAnimationDirection(int animationDirection) {
    this.animationDirection = animationDirection;
    //if (animationReplayMode != ANIMATION_LOOP)
      //currentDirection = 1;
  }

  void setAnimationFps(int animationFps) {
    this.animationFps = animationFps;
  }

  // 0 = once
  // 1 = loop
  // 2 = palindrome
  
  void setAnimationReplayMode(EnumAnimationMode animationReplayMode,
                                     float firstFrameDelay,
                                     float lastFrameDelay) {
    this.firstFrameDelay = firstFrameDelay > 0 ? firstFrameDelay : 0;
    firstFrameDelayMs = (int)(this.firstFrameDelay * 1000);
    this.lastFrameDelay = lastFrameDelay > 0 ? lastFrameDelay : 0;
    lastFrameDelayMs = (int)(this.lastFrameDelay * 1000);
    this.animationReplayMode = animationReplayMode;
    viewer.setFrameVariables();
  }

  void setAnimationRange(int framePointer, int framePointer2) {
    int frameCount = getFrameCount();
    if (framePointer < 0) framePointer = 0;
    if (framePointer2 < 0) framePointer2 = frameCount;
    if (framePointer >= frameCount) framePointer = frameCount - 1;
    if (framePointer2 >= frameCount) framePointer2 = frameCount - 1;
    firstFrameIndex = framePointer;
    lastFrameIndex = framePointer2;
    frameStep = (framePointer2 < framePointer ? -1 : 1);
    rewindAnimation();
  }

  private void animation(boolean TF) {
    animationOn = TF; 
    viewer.setBooleanProperty("_animating", TF);
  }
  
  public void setAnimationOn(boolean animationOn) {
    if (!animationOn || !viewer.haveModelSet() || viewer.isHeadless()) {
      stopThread(false);
      return;
    }
    if (!viewer.getSpinOn())
      viewer.refresh(3, "Viewer:setAnimationOn");
    setAnimationRange(-1, -1);
    resumeAnimation();
  }

  public void stopThread(boolean isPaused) {
    if (animationThread != null) {
      animationThread.interrupt();
      animationThread = null;
    }
    animationPaused = isPaused;
    if (!viewer.getSpinOn())
      viewer.refresh(3, "Viewer:setAnimationOff");
    animation(false);
    viewer.setStatusFrameChanged(false);
  }

  void pauseAnimation() {
    stopThread(true);
  }
  
  void reverseAnimation() {
    currentDirection = -currentDirection;
    if (!animationOn)
      resumeAnimation();
  }
  
  void repaintDone() {
    lastFramePainted = getCurrentFrame();
  }
  
  void resumeAnimation() {
    if(currentModelIndex < 0)
      setAnimationRange(firstFrameIndex, lastFrameIndex);
    if (getFrameCount() <= 1) {
      animation(false);
      return;
    }
    animation(true);
    animationPaused = false;
    if (animationThread == null) {
      intAnimThread++;
      animationThread = new AnimationThread(this, viewer, firstFrameIndex, lastFrameIndex, intAnimThread);
      animationThread.start();
    }
  }
  
  public boolean setAnimationNext() {
    return setAnimationRelative(animationDirection);
  }

  void setAnimationLast() {
    setFrame(animationDirection > 0 ? lastFrameIndex : firstFrameIndex);
  }

  void rewindAnimation() {
    setFrame(animationDirection > 0 ? firstFrameIndex : lastFrameIndex);
    currentDirection = 1;
    viewer.setFrameVariables();
  }
  
  boolean setAnimationPrevious() {
    return setAnimationRelative(-animationDirection);
  }

  private boolean setAnimationRelative(int direction) {
    int frameStep = getFrameStep(direction);
    int thisFrame = getCurrentFrame();
    int frameNext = thisFrame + frameStep;
    float morphStep = 0f, nextMorphFrame = 0f;
    boolean isDone;
    if (morphCount > 0) {
      morphStep = 1f / (morphCount + 1);
      nextMorphFrame = currentMorphFrame + frameStep * morphStep;
      isDone = isNotInRange(nextMorphFrame);
    } else {
      isDone = isNotInRange(frameNext);
    }
    if (isDone) {
      switch (animationReplayMode) {
      case ONCE:
        return false;
      case LOOP:
        nextMorphFrame = frameNext = (animationDirection == currentDirection ? firstFrameIndex
            : lastFrameIndex);
        break;
      case PALINDROME:
        currentDirection = -currentDirection;
        frameNext -= 2 * frameStep;
        nextMorphFrame -= 2 * frameStep * morphStep;
      }
    }
    //Logger.debug("next="+modelIndexNext+" dir="+currentDirection+" isDone="+isDone);
    if (morphCount < 1) {
      if (frameNext < 0 || frameNext >= getFrameCount())
        return false;
      setFrame(frameNext);
      return true;
    }
    morph(nextMorphFrame + 1);
    return true;
  }

  private boolean isNotInRange(float frameNext) {
    float f = frameNext - 0.001f;
    return (f > firstFrameIndex && f > lastFrameIndex 
        || (f = frameNext + 0.001f) < firstFrameIndex
        && f < lastFrameIndex);
  }

  private int getFrameStep(int direction) {
    return frameStep * direction * currentDirection;
  }

  float getAnimRunTimeSeconds() {
    int frameCount = getFrameCount();
    if (firstFrameIndex == lastFrameIndex
        || lastFrameIndex < 0 || firstFrameIndex < 0
        || lastFrameIndex >= frameCount
        || firstFrameIndex >= frameCount)
      return  0;
    int i0 = Math.min(firstFrameIndex, lastFrameIndex);
    int i1 = Math.max(firstFrameIndex, lastFrameIndex);
    float nsec = 1f * (i1 - i0) / animationFps + firstFrameDelay
        + lastFrameDelay;
    for (int i = i0; i <= i1; i++)
      nsec += viewer.getFrameDelayMs(i) / 1000f;
    return nsec;
  }

  public void setMovie(Map<String, Object> info) {
    movie = info;
    if (movie == null) {
      bsDisplay = null;
      currentMorphFrame = morphCount = 0;
    } else {
      // this next is important. Without it, not all the atoms get 
      // assigned coordinates, and the zoom ends up wrong, and the
      // movie start frame is not shown
      setFrame(-1);
    }
  }

  public int getCurrentFrame() {
    return (movie == null ? currentModelIndex : currentFrameIndex);
  }

  public boolean isMovie() {
    return (movie != null);
  }

  public boolean currentIsLast() {
    return lastFramePainted == getCurrentFrame();
  }

  public String getModelNumber(int i) {
    switch (i) {
    case -1:
      i = firstFrameIndex;
      break;
    case 0:
      if (morphCount > 0)
        return "-" + (1 + currentMorphFrame);
      i = currentFrameIndex;
      break;
    case 1:
      i = lastFrameIndex;
      break;
    }
    return (movie == null ? viewer.getModelNumberDotted(i) : "" + (i + 1));
  }

  public void setDisplay(BS bs) {
    bsDisplay = (bs == null || bs.cardinality() == 0? null : BSUtil.copy(bs));
  }

  public boolean currentFrameIs(int f) {
    int i = getCurrentFrame();
    return (morphCount == 0 ? i == f : Math.abs(currentMorphFrame - f) < 0.001f);
  }

}
