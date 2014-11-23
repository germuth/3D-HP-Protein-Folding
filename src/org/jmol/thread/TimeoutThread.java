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

import java.util.Iterator;
import java.util.Map;

import org.jmol.util.SB;
import org.jmol.viewer.Viewer;

public class TimeoutThread extends JmolThread {
  public String script;
  private int status;
  private boolean triggered = true;
  private Map<String, Object> timeouts;
  
  public TimeoutThread(Viewer viewer, String name, int ms, String script) {
    super();
    setViewer(viewer, name);
    this.name = name; // no appended info
    set(ms, script);
  }
  
  private void set(int ms, String script) {
    sleepTime = ms;
    if (script != null)
      this.script = script; 
  }

  @Override
  public String toString() {
    return "timeout name=" + name + " executions=" + status + " mSec=" + sleepTime 
    + " secRemaining=" + (targetTime - System.currentTimeMillis())/1000f + " script=" + script;      
  }
  
  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true)
      switch (mode) {
      case INIT:
        if (!isJS)
          Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        timeouts = viewer.getTimeouts();
        targetTime = System.currentTimeMillis() + Math.abs(sleepTime);
        mode = MAIN;
        break;
      case MAIN:
        if (checkInterrupted() || script == null || script.length() == 0)
          return;
        // 26-millisecond check allows
        if (!runSleep(26, CHECK1))
          return;
        mode = CHECK1;
        break;
      case CHECK1:
        // JavaScript-only
        mode = (System.currentTimeMillis() < targetTime ? MAIN : CHECK2);
        break;
      case CHECK2:
        // Time's up!
        currentTime = System.currentTimeMillis();
        if (timeouts.get(name) == null)
          return;
        status++;
        boolean continuing = (sleepTime < 0);
        targetTime = System.currentTimeMillis() + Math.abs(sleepTime);
        if (!continuing)
          timeouts.remove(name);
        if (triggered) {
          triggered = false;
          // script execution of "timeout ID <name>;" triggers the timeout again
          viewer.evalStringQuiet((continuing ? script + ";\ntimeout ID \""
              + name + "\";" : script));
        }
        mode = (continuing ? MAIN : FINISH);
        break;
      case FINISH:
        timeouts.remove(name);
        return;
      }
  }

  public static void clear(Map<String, Object> timeouts) {
    Iterator<Object> e = timeouts.values().iterator();
    while (e.hasNext()) {
      TimeoutThread t = (TimeoutThread) e.next();
      if (!t.script.equals("exitJmol"))
        t.interrupt();
    }
    timeouts.clear();
  }

  public static void setTimeout(Viewer viewer, Map<String, Object> timeouts, String name, int mSec, String script) {
    TimeoutThread t = (TimeoutThread) timeouts.get(name);
    if (mSec == 0) {
      if (t != null) {
        t.interrupt();
        timeouts.remove(name);
      }
      return;
    }
    if (t != null) {
      t.set(mSec, script);
      return;
    }
    t = new TimeoutThread(viewer, name, mSec, script);
    timeouts.put(name, t);
    t.start();
  }

  public static void trigger(Map<String, Object> timeouts, String name) {
    TimeoutThread t = (TimeoutThread) timeouts.get(name);
    if (t != null)
      t.triggered = (t.sleepTime < 0);
  }

  public static String showTimeout(Map<String, Object> timeouts, String name) {
    SB sb = new SB();
    if (timeouts != null) {
      Iterator<Object> e = timeouts.values().iterator();
      while (e.hasNext()) {
        TimeoutThread t = (TimeoutThread) e.next();
        if (name == null || t.name.equalsIgnoreCase(name))
          sb.append(t.toString()).append("\n");
      }
    }
    return (sb.length() > 0 ? sb.toString() : "<no timeouts set>");
  }
  
}