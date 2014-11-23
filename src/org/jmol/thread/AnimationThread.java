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

import org.jmol.util.Logger;
import org.jmol.viewer.AnimationManager;
import org.jmol.viewer.Viewer;

public class AnimationThread extends JmolThread {
  /**
   * 
   */
  private final AnimationManager animationManager;
  private final int framePointer;
  private final int framePointer2;
  private int intThread;
  private boolean isFirst;
  

  public AnimationThread(AnimationManager animationManager, Viewer viewer, int framePointer, int framePointer2, int intAnimThread) {
    super();
    setViewer(viewer, "AnimationThread");
    this.animationManager = animationManager;
    this.framePointer = framePointer;
    this.framePointer2 = framePointer2;
    intThread = intAnimThread;
    viewer.startHoverWatcher(false);
  }

  @Override
  public void interrupt() {
    if (stopped)
      return;
    stopped = true;
    Logger.debug("animation thread interrupted!");
    try {
      animationManager.setAnimationOn(false);
    } catch (Exception e) {
      // null pointer -- don't care;
    }
    super.interrupt();
  }
  
  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        if (Logger.debugging)
          Logger.debug("animation thread " + intThread + " running");
        viewer.requestRepaintAndWait();
        viewer.startHoverWatcher(false);
        isFirst = true;
        mode = MAIN;
        break;
      case MAIN:
        //System.out.println("anim thred " + animationManager.getCurrentFrame() +" "+ framePointer);
        if (checkInterrupted() || !animationManager.animationOn) {
          mode = FINISH;
          break;
        }
        if (animationManager.currentFrameIs(framePointer)) {
          targetTime += animationManager.firstFrameDelayMs;
          sleepTime = (int) (targetTime - (System.currentTimeMillis() - startTime));
          if (!runSleep(sleepTime, CHECK1))
            return;
        }
        mode = CHECK1;
        break;
      case CHECK1:
        if (animationManager.currentFrameIs(framePointer2)) {
          targetTime += animationManager.lastFrameDelayMs;
          sleepTime = (int) (targetTime - (System.currentTimeMillis() - startTime));
          if (!runSleep(sleepTime, CHECK2))
            return;
        }
        mode = CHECK2;
        break;
      case CHECK2:
        if (!isFirst
            && animationManager.currentIsLast()
            && !animationManager.setAnimationNext()) {
          mode = FINISH;
          break;
        }
        isFirst = false;
        targetTime += (int) ((1000f / animationManager.animationFps) + viewer
            .getFrameDelayMs(animationManager.getCurrentFrame()));
        mode = CHECK3;
        break;
      case CHECK3:
        while (animationManager.animationOn && !checkInterrupted()
            && !viewer.getRefreshing()) {
          if (!runSleep(10, CHECK3))
            return;
        }
        if (!viewer.getSpinOn())
          viewer.refresh(1, "animationThread");
        sleepTime = (int) (targetTime - (System.currentTimeMillis() - startTime));
        if (!runSleep(sleepTime, MAIN))
          return;
        mode = MAIN;
        break;
      case FINISH:
        Logger.debug("animation thread " + intThread + " exiting");
        animationManager.stopThread(false);
        return;
      }
  }

}