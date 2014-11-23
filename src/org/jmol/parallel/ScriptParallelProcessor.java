/* $Author: hansonr $
 * $Date: 2010-04-22 13:16:44 -0500 (Thu, 22 Apr 2010) $
 * $Revision: 12904 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.parallel;

import org.jmol.util.JmolList;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jmol.api.JmolParallelProcessor;
import org.jmol.script.ScriptContext;
import org.jmol.script.ScriptFunction;
import org.jmol.util.Logger;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

public class ScriptParallelProcessor extends ScriptFunction implements JmolParallelProcessor {

  /**
   * parallel operations
   * 
   */
  
  public ScriptParallelProcessor() {    
  }
  
  public Object getExecutor() {
    return Executors.newCachedThreadPool();
  }
  
  Viewer viewer;
  public volatile int counter = 0;
  public volatile Error error = null;
  Object lock = new Object() ;
  
  public void runAllProcesses(Viewer viewer) {
    if (processes.size() == 0)
      return;
    this.viewer = viewer;
    boolean inParallel = !viewer.isParallel() && viewer.setParallel(true);
    JmolList<ShapeManager> vShapeManagers = new  JmolList<ShapeManager>();
    error = null;
    counter = 0;
    if (Logger.debugging)
      Logger.debug("running " + processes.size() + " processes on "
          + Viewer.nProcessors + " processesors inParallel=" + inParallel);

    counter = processes.size();
    for (int i = processes.size(); --i >= 0;) {
      ShapeManager shapeManager = null;
      if (inParallel) {
        shapeManager = new ShapeManager(viewer, viewer.getModelSet());
        vShapeManagers.addLast(shapeManager);
      }
      runProcess(processes.remove(0), shapeManager);
    }

    synchronized (lock) {
      while (counter > 0) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
        }
        if (error != null)
          throw error;
      }
    }
    mergeResults(vShapeManagers);
    viewer.setParallel(false);
  }

  void mergeResults(JmolList<ShapeManager> vShapeManagers) {
    try {
      for (int i = 0; i < vShapeManagers.size(); i++)
        viewer.mergeShapes(vShapeManagers.get(i).getShapes());
    } catch (Error e) {
      throw e;
    } finally {
      counter = -1;
      vShapeManagers = null;
    }
  }

  void clearShapeManager(Error er) {
    synchronized (this) {
      this.error = er;
      this.notifyAll();
    }
  }

  private JmolList<ScriptProcess> processes = new  JmolList<ScriptProcess>();

  public void addProcess(String name, ScriptContext context) {
    processes.addLast(new ScriptProcess(name, context));
  }

  private void runProcess(final ScriptProcess process, ShapeManager shapeManager) {
    ScriptProcessRunnable r = new ScriptProcessRunnable(this, process, lock, shapeManager);
    Executor exec = (shapeManager == null ? null : (Executor) viewer.getExecutor());
    if (exec != null) {
      exec.execute(r);
    } else {
      r.run();
    }
  }

  void eval(ScriptContext context, ShapeManager shapeManager) {
    viewer.evalParallel(context, shapeManager);
  }

}
