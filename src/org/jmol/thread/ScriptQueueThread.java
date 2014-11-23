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



import org.jmol.api.JmolScriptManager;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class ScriptQueueThread extends JmolThread {
  /**
   * 
   */
  private final JmolScriptManager scriptManager;
  private boolean startedByCommandThread = false;
  private int pt;

  public ScriptQueueThread(JmolScriptManager scriptManager, Viewer viewer, boolean startedByCommandThread, int pt) {
    super();
    setViewer(viewer, "QueueThread" + pt);
    this.scriptManager = scriptManager;
    this.viewer = viewer;
    this.startedByCommandThread = startedByCommandThread;
    this.pt = pt;
  }

  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        mode = MAIN;
        break;
      case MAIN:
        if (stopped || scriptManager.getScriptQueue().size() == 0) {
          mode = FINISH;
          break;
        }
        /*  System.out.println("run while size != 0: " + this + " pt=" + this.pt + " size=" + scriptQueue.size());
        for (int i = 0; i < scriptQueue.size(); i++)
        System.out.println("queue: " + i + " " + scriptQueue.get(i));
        System.out.println("running: " + scriptQueueRunning[0] + " "  + queueThreads[0]);
        System.out.println("running: " + scriptQueueRunning[1] + " "  + queueThreads[1]);
        */
        if (!runNextScript() && !runSleep(100, MAIN))
          return;
        break;
      case FINISH:
        scriptManager.queueThreadFinished(pt);
        return;
      }
  }

  private boolean runNextScript() {
    JmolList<JmolList<Object>> queue = scriptManager.getScriptQueue();
    if (queue.size() == 0)
      return false;
    //Logger.info("SCRIPT QUEUE BUSY" +  scriptQueue.size());
    JmolList<Object> scriptItem = scriptManager.getScriptItem(false, startedByCommandThread);
    if (scriptItem == null)
      return false; 
    String script = (String) scriptItem.get(0);
    String statusList = (String) scriptItem.get(1);
    String returnType = (String) scriptItem.get(2);
    boolean isScriptFile = ((Boolean) scriptItem.get(3)).booleanValue();
    boolean isQuiet = ((Boolean) scriptItem.get(4)).booleanValue();
    if (Logger.debugging) {
      Logger.info("Queue[" + pt + "][" + queue.size()
          + "] scripts; running: " + script);
    }
    //System.out.println("removing: " + scriptItem + " " + script);
    queue.remove(0);
    //System.out.println("removed: " + scriptItem);
    viewer.evalStringWaitStatusQueued(returnType, script, statusList, isScriptFile, isQuiet, true);
    if (queue.size() == 0) {// might have been cleared with an exit
      //Logger.info("SCRIPT QUEUE READY", 0);
      return false;
    }
    return true;
  }


}