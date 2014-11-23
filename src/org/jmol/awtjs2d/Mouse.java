/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-09-11 19:29:26 -0500 (Tue, 11 Sep 2012) $
 * $Revision: 17556 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.awtjs2d;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.jmol.api.JmolMouseInterface;
import org.jmol.api.Event;
import org.jmol.script.T;
import org.jmol.util.Escape;
import org.jmol.util.J2SRequireImport;
import org.jmol.util.Logger;
import org.jmol.util.V3;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.Viewer;
import org.jmol.viewer.binding.Binding;

/**
 * JavaScript interface from JmolJSmol.js via handleOldJvm10Event (for now)
 * 
 * J2SRequireImport is needed because we want to allow JavaScript access to java.awt.Event constant names
 * 
 */

@J2SRequireImport({org.jmol.api.Event.class})
public class Mouse implements JmolMouseInterface {

  private Viewer viewer;
  private ActionManager actionManager;

  public Mouse(Viewer viewer, ActionManager actionManager) {
    this.viewer = viewer;
    this.actionManager = actionManager;
  }

  public void clear() {
    // nothing to do here now -- see ActionManager
  }

  public void dispose() {
    actionManager.dispose();
  }

  public boolean handleOldJvm10Event(int id, int x, int y, int modifiers, long time) {
    if (id != -1)
      modifiers = applyLeftMouse(modifiers);
    switch (id) {
    case -1: // JavaScript
      wheeled(time, x, modifiers | Binding.WHEEL);
      break;
    case Event.MOUSE_DOWN:
      xWhenPressed = x;
      yWhenPressed = y;
      modifiersWhenPressed10 = modifiers;
      pressed(time, x, y, modifiers, false);
      break;
    case Event.MOUSE_DRAG:
      dragged(time, x, y, modifiers);
      break;
    case Event.MOUSE_ENTER:
      entered(time, x, y);
      break;
    case Event.MOUSE_EXIT:
      exited(time, x, y);
      break;
    case Event.MOUSE_MOVE:
      moved(time, x, y, modifiers);
      break;
    case Event.MOUSE_UP:
      released(time, x, y, modifiers);
      // simulate a mouseClicked event for us
      if (x == xWhenPressed && y == yWhenPressed
          && modifiers == modifiersWhenPressed10) {
        // the underlying code will turn this into dbl clicks for us
        clicked(time, x, y, modifiers, 1);
      }
      break;
    default:
      return false;
    }
    return true;
  }

  /**
   * 
   * called directly by JSmol as applet._applet.viewer.mouse.processTwoPointGesture(canvas.touches);
   * 
   * @param touches
   *     [[finger1 touches],[finger2 touches]]
   *     where finger touches are [[x0,y0],[x1,y1],[x2,y2],...] 
   *    
   */
  public void processTwoPointGesture(float[][][] touches) {
    
    if (touches[0].length < 2)
      return;
    float[][] t1 = touches[0];
    float[][] t2 = touches[1];
    float[] t10 = t1[0];
    float[] t11 = t1[t2.length - 1];
    float x10 = t10[0];
    float x11 = t11[0]; 
    float dx1 = x11 - x10;
    float y10 = t10[1];
    float y11 = t11[1]; 
    float dy1 = y11 - y10;
    V3 v1 = V3.new3(dx1, dy1, 0);
    float d1 = v1.length();
    float[] t20 = t2[0];
    float[] t21 = t2[t2.length - 1];
    float x20 = t20[0];
    float x21 = t21[0]; 
    float dx2 = x21 - x20;
    float y20 = t20[1];
    float y21 = t21[1]; 
    float dy2 = y21 - y20;
    V3 v2 = V3.new3(dx2, dy2, 0);    
    float d2 = v2.length();
    // rooted finger --> zoom (at this position, perhaps?)
    if (d1 < 3 || d2 < 3)
      return;
    v1.normalize();
    v2.normalize();
    float cos12 = (v1.dot(v2));
    // cos12 > 0.8 (same direction) will be required to indicate drag
    // cos12 < -0.8 (opposite directions) will be required to indicate zoom or rotate
    if (cos12 > 0.8) {
      // two co-aligned motions -- translate
      // just use finger 1, last move
      int deltaX = (int) (x11 - t1[t1.length - 2][0]); 
      int deltaY = (int) (y11 - t1[t1.length - 2][1]); 
      viewer.translateXYBy(deltaX, deltaY);
    } else if (cos12 < -0.8) {
      // two classic zoom motions -- zoom
      v1 = V3.new3(x20 - x10, y20 - y10, 0);
      v2 = V3.new3(x21 - x11, y21 - y11, 0);
      float dx = v2.length() - v1.length();
      wheeled(System.currentTimeMillis(), dx < 0 ? -1 : 1, Binding.WHEEL);
    }
  }
  
  public void mouseClicked(MouseEvent e) {
    clicked(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .getClickCount());
  }

  public void mouseEntered(MouseEvent e) {
    entered(e.getWhen(), e.getX(), e.getY());
  }

  public void mouseExited(MouseEvent e) {
    exited(e.getWhen(), e.getX(), e.getY());
  }

  public void mousePressed(MouseEvent e) {
    pressed(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .isPopupTrigger());
  }

  public void mouseReleased(MouseEvent e) {
    released(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseDragged(MouseEvent e) {
    int modifiers = e.getModifiers();
    /****************************************************************
     * Netscape 4.* Win32 has a problem with mouseDragged if you left-drag then
     * none of the modifiers are selected we will try to fix that here
     ****************************************************************/
    if ((modifiers & Binding.LEFT_MIDDLE_RIGHT) == 0)
      modifiers |= Binding.LEFT;
    /****************************************************************/
    dragged(e.getWhen(), e.getX(), e.getY(), modifiers);
  }

  public void mouseMoved(MouseEvent e) {
    moved(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    e.consume();
    wheeled(e.getWhen(), e.getWheelRotation(), e.getModifiers()
        | Binding.WHEEL);
  }

  public void keyTyped(KeyEvent ke) {
    ke.consume();
    if (!viewer.menuEnabled())
      return;
    char ch = ke.getKeyChar();
    int modifiers = ke.getModifiers();
    // for whatever reason, CTRL may also drop the 6- and 7-bits,
    // so we are in the ASCII non-printable region 1-31
    if (Logger.debuggingHigh)
      Logger.debug("MouseManager keyTyped: " + ch + " " + (0+ch) + " " + modifiers);
    if (modifiers != 0 && modifiers != Binding.SHIFT) {
      switch (ch) {
      case (char) 11:
      case 'k': // keystrokes on/off
        boolean isON = !viewer.getBooleanProperty("allowKeyStrokes");
        switch (modifiers) {
        case Binding.CTRL:
          viewer.setBooleanProperty("allowKeyStrokes", isON);
          viewer.setBooleanProperty("showKeyStrokes", true);
          break;
        case Binding.CTRL_ALT:
        case Binding.ALT:
          viewer.setBooleanProperty("allowKeyStrokes", isON);
          viewer.setBooleanProperty("showKeyStrokes", false);
          break;
        }
        clearKeyBuffer();
        viewer.refresh(3, "showkey");
        break;
      case 22:
      case 'v': // paste
        switch (modifiers) {
        case Binding.CTRL:
        	break;
        }
        break;
      case 26:
      case 'z': // undo
        switch (modifiers) {
        case Binding.CTRL:
          viewer.undoMoveAction(T.undomove, 1);
          break;
        case Binding.CTRL_SHIFT:
          viewer.undoMoveAction(T.redomove, 1);
          break;
        }
        break;
      case 25:
      case 'y': // redo
        switch (modifiers) {
        case Binding.CTRL:
          viewer.undoMoveAction(T.redomove, 1);
          break;
        }
        break;        
      }
      return;
    }
    if (!viewer.getBooleanProperty("allowKeyStrokes"))
      return;
    addKeyBuffer(ke.getModifiers() == Binding.SHIFT ? Character.toUpperCase(ch) : ch);
  }

  public void keyPressed(KeyEvent ke) {
    if (viewer.isApplet())
      ke.consume();
    actionManager.keyPressed(ke.getKeyCode(), ke.getModifiers());
  }

  public void keyReleased(KeyEvent ke) {
    ke.consume();
    actionManager.keyReleased(ke.getKeyCode());
  }

  private String keyBuffer = "";

  private void clearKeyBuffer() {
    if (keyBuffer.length() == 0)
      return;
    keyBuffer = "";
    if (viewer.getBooleanProperty("showKeyStrokes"))
      viewer
          .evalStringQuiet("!set echo _KEYSTROKES; set echo bottom left;echo \"\"");
  }

  private void addKeyBuffer(char ch) {
    if (ch == 10) {
      sendKeyBuffer();
      return;
    }
    if (ch == 8) {
      if (keyBuffer.length() > 0)
        keyBuffer = keyBuffer.substring(0, keyBuffer.length() - 1);
    } else {
      keyBuffer += ch;
    }
    if (viewer.getBooleanProperty("showKeyStrokes"))
      viewer
          .evalStringQuiet("!set echo _KEYSTROKES; set echo bottom left;echo "
              + Escape.eS("\1" + keyBuffer));
  }

  private void sendKeyBuffer() {
    String kb = keyBuffer;
    if (viewer.getBooleanProperty("showKeyStrokes"))
      viewer
          .evalStringQuiet("!set echo _KEYSTROKES; set echo bottom left;echo "
              + Escape.eS(keyBuffer));
    clearKeyBuffer();
    viewer.script(kb);
  }

  private void entered(long time, int x, int y) {
    actionManager.mouseEntered(time, x, y);
  }

  private void exited(long time, int x, int y) {
    actionManager.mouseExited(time, x, y);
  }
  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param clickCount
   */
  private void clicked(long time, int x, int y, int modifiers, int clickCount) {
    clearKeyBuffer();
    // clickedCount is not reliable on some platforms
    // so we will just deal with it ourselves
    actionManager.mouseAction(Binding.CLICKED, time, x, y, 1, modifiers);
  }

  private boolean isMouseDown; // Macintosh may not recognize CTRL-SHIFT-LEFT as drag, only move
  
  private void moved(long time, int x, int y, int modifiers) {
    clearKeyBuffer();
    if (isMouseDown)
      actionManager.mouseAction(Binding.DRAGGED, time, x, y, 0, applyLeftMouse(modifiers));
    else
      actionManager.mouseAction(Binding.MOVED, time, x, y, 0, modifiers);
  }

  private void wheeled(long time, int rotation, int modifiers) {
    clearKeyBuffer();
    actionManager.mouseAction(Binding.WHEELED, time, 0, rotation, 0, modifiers);
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param isPopupTrigger
   */
  private void pressed(long time, int x, int y, int modifiers,
                    boolean isPopupTrigger) {
    clearKeyBuffer();
    isMouseDown = true;
    actionManager.mouseAction(Binding.PRESSED, time, x, y, 0, modifiers);
  }

  private void released(long time, int x, int y, int modifiers) {
    isMouseDown = false;
    actionManager.mouseAction(Binding.RELEASED, time, x, y, 0, modifiers);
  }

  private void dragged(long time, int x, int y, int modifiers) {
    if ((modifiers & Binding.MAC_COMMAND) == Binding.MAC_COMMAND)
      modifiers = modifiers & ~Binding.RIGHT | Binding.CTRL; 
    actionManager.mouseAction(Binding.DRAGGED, time, x, y, 0, modifiers);
  }

  private static int applyLeftMouse(int modifiers) {
    // if neither BUTTON2 or BUTTON3 then it must be BUTTON1
    return ((modifiers & Binding.LEFT_MIDDLE_RIGHT) == 0) ? (modifiers | Binding.LEFT)
        : modifiers;
  }

  private int xWhenPressed, yWhenPressed, modifiersWhenPressed10;

}
