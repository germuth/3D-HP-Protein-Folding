
/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-09-11 19:29:26 -0500 (Tue, 11 Sep 2012) $
 * $Revision: 17556 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.appletjs;

import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolScriptInterface;
import org.jmol.api.JmolSyncInterface;
import org.jmol.api.JmolViewer;
import org.jmol.constant.EnumCallback;
import org.jmol.i18n.GT;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;


import java.awt.Event;
import java.net.URL;
import java.net.MalformedURLException;
import org.jmol.util.JmolList;
import java.util.Hashtable;
import java.util.Map;


/**
 * Java2Script rendition of Jmol using HTML5-only or WebGL-based graphics
 * 
 * @author Bob Hanson hansonr@stolaf.edu, Takanori Nakane, with the assistance of Jhou Renjian
 * 
 */

public class Jmol implements JmolSyncInterface {

  private final static int SCRIPT_CHECK = 0;
  private final static int SCRIPT_WAIT = 1;
  private final static int SCRIPT_NOWAIT = 2;

  private String language;
  //private String statusForm;
  //private String statusText;
  //private String statusTextarea;

  protected boolean doTranslate = true;
  protected boolean haveDocumentAccess;
  protected boolean isStereoSlave;
  protected boolean loading;
  protected boolean mayScript = true;
  protected String htmlName;
  protected String fullName;
  protected String syncId;
  protected SB outputBuffer;

  protected Object gRight;
  protected JmolViewer viewer;
  protected Map<EnumCallback, String> callbacks = new Hashtable<EnumCallback, String>();
  
  private Map<String, Object>viewerOptions;
	private Map<String, Object> htParams = new Hashtable<String, Object>();
	
	Jmol jmol;

  public Jmol(Map<String, Object>viewerOptions) {
  	if (viewerOptions == null)
  		viewerOptions = new Hashtable<String, Object>();
  	this.viewerOptions = viewerOptions;
  	for (Map.Entry<String, Object> entry : viewerOptions.entrySet())
  		htParams.put(entry.getKey().toLowerCase(), entry.getValue());
    init();
  }
  
  public void jmolReady() {
    System.out.println("Jmol applet " + fullName + " ready");
    viewer.getBooleanProperty("__appletReady");
  }
  
  public void destroy() {
    gRight = null;
    JmolAppletRegistry.checkOut(fullName);
    viewer.setModeMouse(JC.MOUSE_NONE);
    viewer.getBooleanProperty("__appletDestroyed");
    viewer = null;
    System.out.println("Jmol applet " + fullName + " destroyed");
  }

  public Object setStereoGraphics(boolean isStereo) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println(isStereo);
    }
    return null;
  }

  //protected void finalize() throws Throwable {
  //  System.out.println("Jmol finalize " + this);
  //  super.finalize();
  //}

  public void init() {
  	jmol = this;
    htmlName = getParameter("name");
    syncId = getParameter("syncId");
    fullName = htmlName + "__" + syncId + "__";
    System.out.println("Jmol JavaScript applet " + fullName + " initializing");
    setLogging();
  	viewerOptions.remove("debug");
  	viewerOptions.put("fullName", fullName);

    mayScript = true;
    JmolAppletRegistry.checkIn(fullName, this);
    initWindows();
    initApplication();
  }

  private void initWindows() {
  	viewerOptions.put("applet", Boolean.TRUE);
  	if (getParameter("statusListener") == null)
  		viewerOptions.put("statusListener", new MyStatusListener());
  	viewer = new Viewer(viewerOptions);
    mayScript = true;
  }

  private void initApplication() {
    viewer.pushHoldRepaint();
    String emulate = getValueLowerCase("emulate", "jmol");
    setStringProperty("defaults", emulate.equals("chime") ? "RasMol" : "Jmol");
    setStringProperty("backgroundColor", getValue("bgcolor", getValue(
        "boxbgcolor", "black")));

    //viewer.setBooleanProperty("frank", true);
    loading = true;
    for (EnumCallback item : EnumCallback.values()) {
      setValue(item.name() + "Callback", null);
    }
    loading = false;
    /*
    			language = getParameter("language");
    			if (language != null) {
    				System.out.print("requested language=" + language + "; ");
    				new GT(language);
    			}
    			doTranslate = (!"none".equals(language) && getBooleanValue("doTranslate",
    					true));
    */
    language = GT.getLanguage();
    System.out.println("language=" + language);

    //boolean haveCallback = false;
    // these are set by viewer.setStringProperty() from setValue
    //for (EnumCallback item : EnumCallback.values()) {
    //	if (callbacks.get(item) != null) {
    //		haveCallback = true;
    //		break;
    //	}
    //}
    //if (haveCallback || statusForm != null || statusText != null) {
    //	if (!mayScript)
    //		Logger
    //				.warn("MAYSCRIPT missing -- all applet JavaScript calls disabled");
    //}
    //if (callbacks.get(EnumCallback.SCRIPT) == null
    //		&& callbacks.get(EnumCallback.ERROR) == null)
    //	if (callbacks.get(EnumCallback.MESSAGE) != null /* || statusForm != null
    //			|| statusText != null */) {
    //		if (doTranslate && (getValue("doTranslate", null) == null)) {
    //			doTranslate = false;
    //			Logger
    //					.warn("Note -- Presence of message callback disables translation;"
    //							+ " to enable message translation use jmolSetTranslation(true) prior to jmolApplet()");
    //		}
    //		if (doTranslate)
    //			Logger
    //					.warn("Note -- Automatic language translation may affect parsing of message callbacks"
    //							+ " messages; use scriptCallback or errorCallback to process errors");
    //	}

    //if (!doTranslate) {
    //	GT.setDoTranslate(false);
    //	Logger.warn("Note -- language translation disabled");
    //}

    //statusForm = getValue("StatusForm", null);
    //statusText = getValue("StatusText", null); // text
    //statusTextarea = getValue("StatusTextarea", null); // textarea

    //if (statusForm != null && statusText != null) {
    //	Logger.info("applet text status will be reported to document."
    //			+ statusForm + "." + statusText);
    //}

    // should the popupMenu be loaded ?
    //if (!getBooleanValue("popupMenu", true))
    //	viewer.getProperty("DATA_API", "disablePopupMenu", null);

    String scriptParam = getValue("script", "");
    viewer.popHoldRepaint();
    if (scriptParam.length() > 0)
      scriptProcessor(scriptParam, null, SCRIPT_WAIT);
    jmolReady();
  }

  private void setLogging() {
    int iLevel = (getValue("logLevel", (getBooleanValue("debug", false) ? "5"
        : "4"))).charAt(0) - '0';
    if (iLevel != 4)
      System.out.println("setting logLevel=" + iLevel
          + " -- To change, use script \"set logLevel [0-5]\"");
    Logger.setLogLevel(iLevel);
  }

  private String getParameter(String paramName) {
    Object o = htParams.get(paramName.toLowerCase());
    return (o == null ? (String) null : new String(o.toString()));
  }

  private boolean getBooleanValue(String propertyName, boolean defaultValue) {
    String value = getValue(propertyName, defaultValue ? "true" : "");
    return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on") || value
        .equalsIgnoreCase("yes"));
  }

  private String getValue(String propertyName, String defaultValue) {
    String stringValue = getParameter(propertyName);
    System.out.println("getValue " + propertyName + " = " + stringValue);
    if (stringValue != null)
      return stringValue;
    return defaultValue;
  }

  private String getValueLowerCase(String paramName, String defaultValue) {
    String value = getValue(paramName, defaultValue);
    if (value != null) {
      value = value.trim().toLowerCase();
      if (value.length() == 0)
        value = null;
    }
    return value;
  }

  private void setValue(String name, String defaultValue) {
    setStringProperty(name, getValue(name, defaultValue));
  }

  private void setStringProperty(String name, String value) {
    if (value == null)
      return;
    Logger.info(name + " = \"" + value + "\"");
    viewer.setStringProperty(name, value);
  }

  protected void sendJsTextStatus(String message) {
    System.out.println(message);
  	// not implemented
  }

  protected void sendJsTextareaStatus(String message) {
    System.out.println(message);
  	// not implemented
  }

  public boolean handleEvent(Event e) {
    if (viewer == null)
      return false;
    return viewer.handleOldJvm10Event(e.id, e.x, e.y, e.modifiers, e.when);
  }

  private String scriptProcessor(String script, String statusParams,
                                 int processType) {
    /*
     * Idea here is to provide a single point of entry
     * Synchronization may not work, because it is possible for the NOWAIT variety of
     * scripts to return prior to full execution 
     * 
     */
    //System.out.println("Jmol.script: " + script);
    if (script == null || script.length() == 0)
      return "";
    switch (processType) {
    case SCRIPT_CHECK:
      Object err = viewer.scriptCheck(script);
      return (err instanceof String ? (String) err : "");
    case SCRIPT_WAIT:
      if (statusParams != null)
        return viewer.scriptWaitStatus(script, statusParams).toString();
      return viewer.scriptWait(script);
    case SCRIPT_NOWAIT:
    default:
      return viewer.script(script);
    }
  }

  public void script(String script) {
    scriptNoWait(script);
  }

  public String scriptNoWait(String script) {
    if (script == null || script.length() == 0)
      return "";
    return scriptProcessor(script, null, SCRIPT_NOWAIT);
  }

  public String scriptCheck(String script) {
    if (script == null || script.length() == 0)
      return "";
    return scriptProcessor(script, null, SCRIPT_CHECK);
  }

  public String scriptWait(String script) {
    return scriptWait(script, null);
  }

  public String scriptWait(String script, String statusParams) {
    if (script == null || script.length() == 0)
      return "";
    outputBuffer = null;
    return scriptProcessor(script, statusParams, SCRIPT_WAIT);
  }

  public String scriptWaitOutput(String script) {
    if (script == null || script.length() == 0)
      return "";
    outputBuffer = new SB();
    viewer.scriptWaitStatus(script, "");
    String str = (outputBuffer == null ? "" : outputBuffer.toString());
    outputBuffer = null;
    return str;
  }

  synchronized public void syncScript(String script) {
    viewer.syncScript(script, "~", 0);
  }

  public String getAppletInfo() {
    return GT
        ._(
            "Jmol Applet version {0} {1}.\n\nAn OpenScience project.\n\nSee http://www.jmol.org for more information",
            new Object[] { JC.version, JC.date })
        + "\nhtmlName = "
        + Escape.eS(htmlName)
        + "\nsyncId = "
        + Escape.eS(syncId)
        + "\ndocumentBase = "
        + Escape.eS("" + getProperty("documentBase"))
        + "\ncodeBase = "
        + Escape.eS("" + getProperty("codeBase"));
  }

  public Object getProperty(String infoType) {
    return viewer.getProperty(null, infoType, "");
  }

  public Object getProperty(String infoType, String paramInfo) {
    return viewer.getProperty(null, infoType, paramInfo);
  }

  public String getPropertyAsString(String infoType) {
    return viewer.getProperty("readable", infoType, "").toString();
  }

  public String getPropertyAsString(String infoType, String paramInfo) {
    return viewer.getProperty("readable", infoType, paramInfo).toString();
  }

  public String getPropertyAsJSON(String infoType) {
    return viewer.getProperty("JSON", infoType, "").toString();
  }

  public String getPropertyAsJSON(String infoType, String paramInfo) {
    return viewer.getProperty("JSON", infoType, paramInfo).toString();
  }

  public String loadInlineString(String strModel, String script, boolean isAppend) {
    String errMsg = viewer.loadInline(strModel, isAppend);
    if (errMsg == null)
      script(script);
    return errMsg;
  }

  public String loadInlineArray(String[] strModels, String script,
                              boolean isAppend) {
    if (strModels == null || strModels.length == 0)
      return null;
    String errMsg = viewer.loadInline(strModels, isAppend);
    if (errMsg == null)
      script(script);
    return errMsg;
  }

  class MyStatusListener implements JmolStatusListener {

    public Map<String, Object>  getRegistryInfo() {
      JmolAppletRegistry.checkIn(null, null); //cleans registry
      return JmolAppletRegistry.htRegistry;
    }

    public void resizeInnerPanel(String data) {
      // application only?
    }

    public boolean notifyEnabled(EnumCallback type) {
      switch (type) {
      case ANIMFRAME:
      case ECHO:
      case ERROR:
      case EVAL:
      case LOADSTRUCT:
      case MEASURE:
      case MESSAGE:
      case PICK:
      case SYNC:
      case SCRIPT:
        return true;
      case APPLETREADY:  // Jmol 12.1.48
      case ATOMMOVED:  // Jmol 12.1.48
      case CLICK:
      case HOVER:
      case MINIMIZATION:
      case RESIZE:
        break;
      }
      return (callbacks.get(type) != null);
    }

    private boolean haveNotifiedError;

    @SuppressWarnings("incomplete-switch")
    public void notifyCallback(EnumCallback type, Object[] data) {
      String callback = callbacks.get(type);
      boolean doCallback = (callback != null && (data == null || data[0] == null));
      boolean toConsole = false;
      if (data != null)
        data[0] = htmlName;
      String strInfo = (data == null || data[1] == null ? null : data[1]
          .toString());

      //System.out.println("Jmol.java notifyCallback " + type + " " + callback
       //+ " " + strInfo);
      switch (type) {
      case APPLETREADY:
        data[3] = jmol;
        break;
      case ERROR:
      case EVAL:
      case HOVER:
      case MINIMIZATION:
      case RESIZE:
        // just send it
        break;
      case CLICK:
        // x, y, action, int[] {action}
        // the fourth parameter allows an application to change the action
        if ("alert".equals(callback))
          strInfo = "x=" + data[1] + " y=" + data[2] + " action=" + data[3] + " clickCount=" + data[4];
        break;
      case ANIMFRAME:
        // Note: twos-complement. To get actual frame number, use
        // Math.max(frameNo, -2 - frameNo)
        // -1 means all frames are now displayed
        int[] iData = (int[]) data[1];
        int frameNo = iData[0];
        int fileNo = iData[1];
        int modelNo = iData[2];
        int firstNo = iData[3];
        int lastNo = iData[4];
        boolean isAnimationRunning = (frameNo <= -2);
        int animationDirection = (firstNo < 0 ? -1 : 1);
        int currentDirection = (lastNo < 0 ? -1 : 1);

        /*
         * animationDirection is set solely by the "animation direction +1|-1"
         * script command currentDirection is set by operations such as
         * "anim playrev" and coming to the end of a sequence in
         * "anim mode palindrome"
         * 
         * It is the PRODUCT of these two numbers that determines what direction
         * the animation is going.
         */
        if (doCallback) {
          data = new Object[] { htmlName,
              Integer.valueOf(Math.max(frameNo, -2 - frameNo)),
              Integer.valueOf(fileNo), Integer.valueOf(modelNo),
              Integer.valueOf(Math.abs(firstNo)), Integer.valueOf(Math.abs(lastNo)),
              Integer.valueOf(isAnimationRunning ? 1 : 0),
              Integer.valueOf(animationDirection), Integer.valueOf(currentDirection) };
        }
        break;
      case ECHO:
        boolean isPrivate = (data.length == 2);
        boolean isScriptQueued = (isPrivate || ((Integer) data[2]).intValue() == 1);
        if (!doCallback) {
          if (isScriptQueued)
            toConsole = true;
          doCallback = (!isPrivate && 
              (callback = callbacks.get((type = EnumCallback.MESSAGE))) != null);
        }
        if (!toConsole)
          output(strInfo);
        break;
      case LOADSTRUCT:
        String errorMsg = (String) data[4];
        if (errorMsg != null) {
          errorMsg = (errorMsg.indexOf("NOTE:") >= 0 ? "" : GT
              ._("File Error:")) + errorMsg;
          showStatus(errorMsg);
          notifyCallback(EnumCallback.MESSAGE, new Object[] { "", errorMsg });
          return;
        }
        break;
      case MEASURE:
        // pending, deleted, or completed
        if (!doCallback)
          doCallback = ((callback = callbacks.get((type = EnumCallback.MESSAGE))) != null);
        String status = (String) data[3];
        if (status.indexOf("Picked") >= 0 || status.indexOf("Sequence") >= 0) {// picking mode
          showStatus(strInfo); // set picking measure distance
          toConsole = true;
        } else if (status.indexOf("Completed") >= 0) {
          strInfo = status + ": " + strInfo;
          toConsole = true;
        }
        break;
      case MESSAGE:
        toConsole = !doCallback;
        doCallback &= (strInfo != null);
        if (!toConsole)
          output(strInfo);
        break;
      case PICK:
        showStatus(strInfo);
        toConsole = true;
        break;
      case SCRIPT:
        int msWalltime = ((Integer) data[3]).intValue();
        // general message has msWalltime = 0
        // special messages have msWalltime < 0
        // termination message has msWalltime > 0 (1 + msWalltime)
        if (msWalltime > 0) {
          // termination -- button legacy
          notifyScriptTermination();
        } else if (!doCallback) {
          // termination messsage ONLY if script callback enabled -- not to
          // message queue
          // for compatibility reasons
          doCallback = ((callback = callbacks.get((type = EnumCallback.MESSAGE))) != null);
        }
        output(strInfo);
        showStatus(strInfo);
        break;
      case SYNC:
        sendScript(strInfo, (String) data[2], true, doCallback);
        return;
      }
      if (toConsole) {
        JmolCallbackListener appConsole = (JmolCallbackListener) viewer.getProperty("DATA_API", "getAppConsole", null);
        if (appConsole != null) {
          appConsole.notifyCallback(type, data);
          output(strInfo);
          sendJsTextareaStatus(strInfo);
        }
      }
      if (!doCallback || !mayScript)
        return;
      try {
          sendCallback(strInfo, callback, data);
      } catch (Exception e) {
        if (!haveNotifiedError)
          if (Logger.debugging) {
            Logger.debug(type.name()
                + "Callback call error to " + callback + ": " + e);
          }
        haveNotifiedError = true;
      }
    }

    private void output(String s) {
      if (outputBuffer != null && s != null)
        outputBuffer.append(s).appendC('\n');
    }

    private void notifyScriptTermination() {
      // this had to do with button callbacks
    }

		private String notifySync(String info, String appletName) {
			String syncCallback = callbacks.get(EnumCallback.SYNC);
			if (!mayScript || syncCallback == null)
				return info;
			try {
				/**
				 * @j2sNative 
				 * 
				 * return eval(syncCallback)(this.htmlName, info, appletName);
				 */
				{
				  System.out.println(appletName);
				}
			} catch (Exception e) {
				if (!haveNotifiedError)
					if (Logger.debugging) {
						Logger.debug("syncCallback call error to " + syncCallback + ": "
								+ e);
					}
				haveNotifiedError = true;
			}
			return info;
		}

    public void setCallbackFunction(String callbackName, String callbackFunction) {
      if (callbackName.equalsIgnoreCase("modelkit"))
        return;
      //also serves to change language for callbacks and menu
      if (callbackName.equalsIgnoreCase("language")) {
        consoleMessage(""); // clear
        consoleMessage(null); // show default message
        return;
      }
      EnumCallback callback = EnumCallback.getCallback(callbackName);
      if (callback != null && (loading || callback != EnumCallback.EVAL)) {
        if (callbackFunction == null)
          callbacks.remove(callback);
        else         
          callbacks.put(callback, callbackFunction);
        return;
      }
      consoleMessage("Available callbacks include: " + EnumCallback.getNameList().replace(';',' ').trim());
    }

    public String eval(String strEval) {
      // may be appletName\1script
      int pt = strEval.indexOf("\1");
      if (pt >= 0)
        return sendScript(strEval.substring(pt + 1), strEval.substring(0, pt),
            false, false);
      if (callbacks.get(EnumCallback.EVAL) != null) {
        notifyCallback(EnumCallback.EVAL, new Object[] { null, strEval });
        return "";
      }
      try {
      	/**
      	 * @j2sNative
      	 *   return "" + eval(a);// strEval -- Java2Script is compressing this file for some reason
      	 */
      	{}
      } catch (Exception e) {
        Logger.error("# error evaluating " + strEval + ":" + e.toString());
      }
      return "";
    }

    /**
     * 
     * @param fileName
     * @param type
     * @param text_or_bytes
     * @param quality
     * @return          null (canceled) or a message starting with OK or an error message
     */
    public String createImage(String fileName, String type, Object text_or_bytes,
                              int quality) {
      return null;
    }

    public float[][] functionXY(String functionName, int nX, int nY) {
      /*three options:
       * 
       *  nX > 0  and  nY > 0        return one at a time, with (slow) individual function calls
       *  nX < 0  and  nY > 0        return a string that can be parsed to give the list of values
       *  nX < 0  and  nY < 0        fill the supplied float[-nX][-nY] array directly in JavaScript 
       *  
       */

      //System.out.println("functionXY" + nX + " " + nY  + " " + functionName);
      float[][] fxy = new float[Math.abs(nX)][Math.abs(nY)];
      if (!mayScript || nX == 0 || nY == 0)
        return fxy;
      try {
        if (nX > 0 && nY > 0) { // fill with individual function calls (slow)
          for (int i = 0; i < nX; i++)
            for (int j = 0; j < nY; j++) {
            	/**
            	 * @j2sNative
            	 * 
            	 *   fxy[i][j] = eval(functionName)(this.htmlName, i, j);
            	 */
            	{
            		
            	}
            }
        } else if (nY > 0) { // fill with parsed values from a string (pretty fast)
        	String data;
        	/**
        	 * @j2sNative
        	 * 
        	 *           	data = eval(functionName)(this.htmlName, nX, nY);
        	 *           
        	 */
          {
          	data = "";
          }
          nX = Math.abs(nX);
          float[] fdata = new float[nX * nY];
          Parser.parseStringInfestedFloatArray(data, null, fdata);
          for (int i = 0, ipt = 0; i < nX; i++) {
            for (int j = 0; j < nY; j++, ipt++) {
              fxy[i][j] = fdata[ipt];
            }
          }
        } else { // fill float[][] directly using JavaScript
        	/**
        	 * @j2sNative
        	 * 
        	 *           	data = eval(functionName)(htmlName, nX, nY, fxy);
        	 *           
        	 */
        	{
        		
        	}
        }
      } catch (Exception e) {
        Logger.error("Exception " + e + " with nX, nY: " + nX
            + " " + nY);
      }
     // for (int i = 0; i < nX; i++)
       // for (int j = 0; j < nY; j++)
         //System.out.println("i j fxy " + i + " " + j + " " + fxy[i][j]);
      return fxy;
    }

    public float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
      float[][][] fxyz = new float[Math.abs(nX)][Math.abs(nY)][Math.abs(nZ)];
      if (!mayScript || nX == 0 || nY == 0 || nZ == 0)
        return fxyz;
      try {
      	/**
      	 * @j2sNative
      	 * 
      	 *           	eval(functionName)(this.htmlName, nX, nY, nZ, fxyz);
      	 *           
      	 */
      	{
      	}
      } catch (Exception e) {
        Logger.error("Exception " + e + " for " + functionName + " with nX, nY, nZ: " + nX
            + " " + nY + " " + nZ);
      }
     // for (int i = 0; i < nX; i++)
      // for (int j = 0; j < nY; j++)
        // for (int k = 0; k < nZ; k++)
         //System.out.println("i j k fxyz " + i + " " + j + " " + k + " " + fxyz[i][j][k]);
      return fxyz;
    }

    public void showUrl(String urlString) {
      if (Logger.debugging) {
        Logger.debug("showUrl(" + urlString + ")");
      }
      if (urlString != null && urlString.length() > 0) {
        try {
          URL url = new URL((URL) null, urlString, null);
          /**
           * @j2sNative
           *   window.open(url);
           */
          {
          	System.out.println(url);
          }
        } catch (MalformedURLException mue) {
          consoleMessage("Malformed URL:" + urlString);
        }
      }
    }

    @Override
    protected void finalize() throws Throwable {
      Logger.debug("MyStatusListener finalize " + this);
      super.finalize();
    }

    private void showStatus(String message) {
      try {
        System.out.println(message);
        //appletWrapper.showStatus(TextFormat.simpleReplace(TextFormat.split(message, "\n")[0], "'", "\\'"));
        //sendJsTextStatus(message);
      } catch (Exception e) {
        //ignore if page is closing
      }
    }

    private void consoleMessage(String message) {
      notifyCallback(EnumCallback.ECHO, new Object[] {"", message });
    }

    private String sendScript(String script, String appletName, boolean isSync,
                              boolean doCallback) {
      if (doCallback) {
        script = notifySync(script, appletName);
        // if the notified JavaScript function returns "" or 0, then 
        // we do NOT continue to notify the other applets
        if (script == null || script.length() == 0 || script.equals("0"))
          return "";
      }

      JmolList<String> apps = new  JmolList<String>();
      JmolAppletRegistry.findApplets(appletName, syncId, fullName, apps);
      int nApplets = apps.size();
      if (nApplets == 0) {
        if (!doCallback && !appletName.equals("*"))
          Logger.error(fullName + " couldn't find applet " + appletName);
        return "";
      }
      SB sb = (isSync ? null : new SB());
      boolean getGraphics = (isSync && script.equals(Viewer.SYNC_GRAPHICS_MESSAGE));
      boolean setNoGraphics = (isSync && script.equals(Viewer.SYNC_NO_GRAPHICS_MESSAGE));
      if (getGraphics)
        gRight = null;
      for (int i = 0; i < nApplets; i++) {
        String theApplet = apps.get(i);
        JmolSyncInterface app = (JmolSyncInterface) JmolAppletRegistry.htRegistry
            .get(theApplet);
        boolean isScriptable = (app instanceof JmolScriptInterface);
        if (Logger.debugging)
          Logger.debug(fullName + " sending to " + theApplet + ": " + script);
        try {
          if (isScriptable && (getGraphics || setNoGraphics)) {
            gRight = ((JmolScriptInterface)app).setStereoGraphics(getGraphics);
            return "";
          }
          if (isSync)
            app.syncScript(script);
          else if (isScriptable)
            sb.append(((JmolScriptInterface)app).scriptWait(script, "output")).append("\n");
        } catch (Exception e) {
          String msg = htmlName + " couldn't send to " + theApplet + ": "
              + script + ": " + e;
          Logger.error(msg);
          if (!isSync)
            sb.append(msg);
        }
      }
      return (isSync ? "" : sb.toString());
    }

    public Map<String, Object> getProperty(String type) {
      // only used on JSpecView side
      return null;
    }

  }

  protected static String sendCallback(String strInfo, String callback, Object[] data) {
    if (callback == null || callback.length() == 0) {
    } else if (callback.equals("alert")) {
    	/**
    	 * @j2sNative
    	 * 	alert(strInfo);
    	 *  return "";
    	 */
    	{
    	  System.out.println(strInfo);
    	}
    } else {
    	 String[] tokens = TextFormat.split(callback, '.');
    	/**
    	 * @j2sNative
    	 * 
    	 * try{
    	 *   var o = window[tokens[0]]
    	 *   for (i = 1; i < tokens.length; i++){
    	 *     o = o[tokens[i]]
    	 *   }
    	 *   return o(data[0],data[1],data[2],data[3],data[4],data[5],data[6],data[7]);
    	 * } catch (e) {
    	 *	 System.out.println(callback + " failed " + e);
    	 * }
    	 */
    	{
    		System.out.println(tokens + " " + data);
    	}
    }
    return "";
  }

  public void register(String id, JmolSyncInterface jsi) {
    JmolAppletRegistry.checkIn(id, jsi); 
  }
}
