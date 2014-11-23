/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-26 16:57:51 -0500 (Thu, 26 Apr 2007) $
 * $Revision: 7502 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.console;

import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.jmol.api.JmolAbstractButton;
import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.api.JmolViewer;
import org.jmol.constant.EnumCallback;
import org.jmol.i18n.GT;
import org.jmol.script.T;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public abstract class GenericConsole implements JmolAppConsoleInterface, JmolCallbackListener {
  
  protected GenericTextArea input;
  protected GenericTextArea output;

  public Viewer viewer;
  
  protected void setViewer(JmolViewer viewer) {
    this.viewer = (Viewer) viewer;
  }

  protected Map<String, String> labels;
  protected Map<String, Object> menuMap = new Hashtable<String, Object>();
  protected JmolAbstractButton editButton, runButton, historyButton, stateButton;
  protected JmolAbstractButton clearOutButton, clearInButton, loadButton;
   
  abstract protected boolean isMenuItem(Object source);  
  abstract protected void layoutWindow(String enabledButtons);
  abstract protected void setTitle();  
  abstract public void setVisible(boolean visible);
  abstract public JmolScriptEditorInterface getScriptEditor();
  abstract public void dispose();

  abstract protected JmolAbstractButton setButton(String text);
 
  protected JmolAbstractButton addButton(JmolAbstractButton b, String label) {
    b.addConsoleListener(this);
    menuMap.put(label, b);
    return b;    
  }

  protected JmolAbstractButton getLabel1() {
    return null;
  }

  protected void setupLabels() {
    labels.put("help", GT._("&Help"));
    labels.put("search", GT._("&Search..."));
    labels.put("commands", GT._("&Commands"));
    labels.put("functions", GT._("Math &Functions"));
    labels.put("parameters", GT._("Set &Parameters"));
    labels.put("more", GT._("&More"));
    labels.put("Editor", GT._("Editor"));
    labels.put("State", GT._("State"));
    labels.put("Run", GT._("Run"));
    labels.put("Clear Output", GT._("Clear Output"));
    labels.put("Clear Input", GT._("Clear Input"));
    labels.put("History", GT._("History"));
    labels.put("Load", GT._("Load"));
    labels.put("label1", GT
        ._("press CTRL-ENTER for new line or paste model data and press Load"));
    labels.put("default",
        GT._("Messages will appear here. Enter commands in the box below. Click the console Help menu item for on-line help, which will appear in a new browser window."));
  }

  protected void setLabels() {
    boolean doTranslate = GT.setDoTranslate(true);
    editButton = setButton("Editor");
    stateButton = setButton("State");
    runButton = setButton("Run");
    clearOutButton = setButton("Clear Output");
    clearInButton = setButton("Clear Input");
    historyButton = setButton("History");
    loadButton = setButton("Load");
    defaultMessage = getLabel("default");
    setTitle();
    GT.setDoTranslate(false);
    /**
    *
    * no help menu yet
    * 
    * @j2sNative 
    * 
    * this.defaultMessage = this.getLabel("default").split("Click")[0];
    * 
    */
    {
    }
    GT.setDoTranslate(doTranslate);
    defaultMessage = getLabel("default");
  }

  protected String getLabel(String key) {
    if (labels == null) {
      labels = new Hashtable<String, String>();
      labels.put("title", GT._("Jmol Script Console") + " " + Viewer.getJmolVersion());
      setupLabels();
    }
    return labels.get(key);
  }

  protected void displayConsole() {
    layoutWindow(null);
    outputMsg(defaultMessage);
    System.out.println("AppConsole displayConsole");
  }

  protected String defaultMessage;
  protected JmolAbstractButton label1;
  
  protected void updateLabels() {
    return;
  }

  abstract protected String nextFileName(String stub, int nTab);
  public int nTab = 0;
  private String incompleteCmd;
  
  public String completeCommand(String thisCmd) {
    if (thisCmd.length() == 0)
      return null;
    String strCommand = (nTab <= 0 || incompleteCmd == null ? thisCmd
        : incompleteCmd);
    incompleteCmd = strCommand;
    String[] splitCmd = TextFormat.splitCommandLine(thisCmd);
    if (splitCmd == null)
      return null;
    boolean asCommand = splitCmd[2] == null;
    String notThis = splitCmd[asCommand ? 1 : 2];
    String s = splitCmd[1];
    if (notThis.length() == 0)
      return null;
    splitCmd = TextFormat.splitCommandLine(strCommand);
    String cmd = null;
    if (!asCommand && (notThis.charAt(0) == '"' || notThis.charAt(0) == '\'')) {
      char q = notThis.charAt(0);
      notThis = TextFormat.trim(notThis, "\"\'");
      String stub = TextFormat.trim(splitCmd[2], "\"\'");
      cmd = nextFileName(stub, nTab);
      if (cmd != null)
        cmd = splitCmd[0] + splitCmd[1] + q + cmd + q;
    } else {
      Map<String, T> map = null;
      if (!asCommand) {
        //System.out.println(" tsting " + splitCmd[0] + "///" + splitCmd[1] + "///" + splitCmd[2]);
        notThis = s;
        if (splitCmd[2].startsWith("$") 
            || s.equalsIgnoreCase("isosurface ")
            || s.equalsIgnoreCase("contact ")
            || s.equalsIgnoreCase("draw ")
         ) {
          map = new Hashtable<String, T>();
          viewer.getObjectMap(map, splitCmd[2].startsWith("$"));
        }
      }
      cmd = T.completeCommand(map, s.equalsIgnoreCase("set "), asCommand, asCommand ? splitCmd[1]
          : splitCmd[2], nTab);
      cmd = splitCmd[0]
          + (cmd == null ? notThis : asCommand ? cmd : splitCmd[1] + cmd);
    }
    return (cmd == null || cmd.equals(strCommand) ? null : cmd);
  }

  protected void doAction(Object source) {
    if (source == runButton) {
      execute(null);
    } else if (source == editButton) {
      viewer.getProperty("DATA_API","scriptEditor", null);
    } else if (source == historyButton) {
      clearContent(viewer.getSetHistory(Integer.MAX_VALUE));
    } else if (source == stateButton) {
      clearContent(viewer.getStateInfo());
      // problem here is that in some browsers, you cannot clip from
      // the editor.
      //viewer.getProperty("DATA_API","scriptEditor", new String[] { "current state" , viewer.getStateInfo() });
    } else     //System.out.println("AppletConsole.actionPerformed" +  source);
      if (source == clearInButton) {
        input.setText("");
        return;
      }
      if (source == clearOutButton) {
        output.setText("");
        return;
      }
      if (source == loadButton) {
        viewer.loadInline(input.getText(), false);
        return;
      }
      if (isMenuItem(source)) {
        execute(((JmolAbstractButton) source).getName());
        return;
      }
  }

  protected void execute(String strCommand) {
    String cmd = (strCommand == null ? input.getText() : strCommand);
    if (strCommand == null)
      input.setText(null);
    String strErrorMessage = viewer.script(cmd + JC.SCRIPT_EDITOR_IGNORE);
    if (strErrorMessage != null && !strErrorMessage.equals("pending"))
      outputMsg(strErrorMessage);
  }

  protected void destroyConsole() {
    // if the viewer is an applet, when we close the console
    // we 
    if (viewer.isApplet())
      viewer.getProperty("DATA_API", "getAppConsole", Boolean.FALSE);
  }

  public static void setAbstractButtonLabels(Map<String, Object> menuMap,
                               Map<String, String> labels) {
    Iterator<String> e = menuMap.keySet().iterator();
    while (e.hasNext()) {
      String key = e.next();
      JmolAbstractButton m = (JmolAbstractButton) menuMap.get(key);
      String label = labels.get(key);
      if (key.indexOf("Tip") == key.length() - 3) {
        m.setToolTipText(labels.get(key));
      } else {
        char mnemonic = getMnemonic(label);
        if (mnemonic != ' ')
          m.setMnemonic(mnemonic);
        label = getLabelWithoutMnemonic(label);
        m.setText(label);
      }
    }
  }

  public static String getLabelWithoutMnemonic(String label) {
    if (label == null) {
      return null;
    }
    int index = label.indexOf('&');
    if (index == -1) {
      return label;
    }
    return label.substring(0, index) +
      ((index < label.length() - 1) ? label.substring(index + 1) : "");
  }

  static char getMnemonic(String label) {
    if (label == null) {
      return ' ';
    }
    int index = label.indexOf('&');
    if ((index == -1) || (index == label.length() - 1)){
      return ' ';
    }
    return label.charAt(index + 1);
  }

  public static void map(Object button, String key, String label,
                         Map<String, Object> menuMap) {
    char mnemonic = getMnemonic(label);
    if (mnemonic != ' ')
      ((JmolAbstractButton) button).setMnemonic(mnemonic);
    menuMap.put(key, button);
  }

  ///////////// JmolCallbackListener interface

  // Allowing for just the callbacks needed to provide status feedback to the console.
  // For applications that embed Jmol, see the example application Integration.java.

  public boolean notifyEnabled(EnumCallback type) {
    // See org.jmol.viewer.JmolConstants.java for a complete list
    switch (type) {
    case ECHO:
    case MEASURE:
    case MESSAGE:
    case PICK:
      return true;
    case ANIMFRAME:
    case APPLETREADY:
    case ATOMMOVED:
    case CLICK:
    case ERROR:
    case EVAL:
    case HOVER:
    case LOADSTRUCT:
    case MINIMIZATION:
    case RESIZE:
    case SCRIPT:
    case SYNC:
      break;
    }
    return false;
  }

  public String getText() {
    return output.getText();
  }

  public void sendConsoleEcho(String strEcho) {
    if (strEcho == null) {
      // null here means new language
      updateLabels();
      outputMsg(null);
      strEcho = defaultMessage;
    }
    outputMsg(strEcho);
  }

  private void outputMsg(String message) {
    if (message == null || message.length() == 0) {
      output.setText("");
      return;
    }
    if (message.charAt(message.length() - 1) != '\n')
      message += "\n";
    output.append(message);
  }

  protected void clearContent(String text) {
    //System.out.println("AppletConsole.clearContent()");
    output.setText(text);
  }
  
  public void sendConsoleMessage(String strInfo) {
    // null here indicates "clear console"
    if (strInfo != null && output.getText().startsWith(defaultMessage))
      outputMsg(null);
    outputMsg(strInfo);
  }
  
  @SuppressWarnings("incomplete-switch")
  public void notifyCallback(EnumCallback type, Object[] data) {
    String strInfo = (data == null || data[1] == null ? null : data[1]
        .toString());
    switch (type) {
    case ECHO:
      sendConsoleEcho(strInfo);
      break;
    case MEASURE:
      String mystatus = (String) data[3];
      if (mystatus.indexOf("Picked") >= 0 || mystatus.indexOf("Sequence") >= 0) // picking mode
        sendConsoleMessage(strInfo);
      else if (mystatus.indexOf("Completed") >= 0)
        sendConsoleEcho(strInfo.substring(strInfo.lastIndexOf(",") + 2, strInfo
            .length() - 1));
      break;
    case MESSAGE:
      sendConsoleMessage(data == null ? null : strInfo);
      break;
    case PICK:
      sendConsoleMessage(strInfo);
      break;
    }
  }

  public void setCallbackFunction(String callbackType, String callbackFunction) {
    // application-dependent option
  }

  public void zap() {
  }

  // key listener actions
  
  protected void recallCommand(boolean up) {
    String cmd = viewer.getSetHistory(up ? -1 : 1);
    if (cmd == null)
      return;
    input.setText(cmd);
  }

  /**
   * 
   * @param kcode
   * @param kid
   * @param isControlDown
   * @return  1 = consume; 2 = super.process; 3 = both
   */
  protected int processKey(int kcode, int kid, boolean isControlDown) {
    int mode = 0;
    switch (kid) {
    case KeyEvent.KEY_PRESSED:
      switch (kcode) {
      case KeyEvent.VK_TAB:
        mode = 1;
        if (input.getCaretPosition() == input.getText().length()) {
          String cmd = completeCommand(getText());
          if (cmd != null)
            input.setText(cmd.replace('\t',' '));
          nTab++;
          return mode;
        }
        break;
      case KeyEvent.VK_ESCAPE:
        mode = 1;
        input.setText("");
        break;
      }
      nTab = 0;
      if (kcode == KeyEvent.VK_ENTER && !isControlDown) {
        execute(null);
        return mode;
      }
      if (kcode == KeyEvent.VK_UP || kcode == KeyEvent.VK_DOWN) {
        recallCommand(kcode == KeyEvent.VK_UP);
        return mode;
      }
      break;
    case KeyEvent.KEY_RELEASED:
      if (kcode == KeyEvent.VK_ENTER && !isControlDown)
        return mode;
      break;
    }
    return mode | 2;
  }



}
