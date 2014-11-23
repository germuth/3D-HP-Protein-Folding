 /* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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
package org.jmol.viewer;


import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import org.jmol.constant.EnumAxesMode;
import org.jmol.constant.EnumCallback;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumStereoMode;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.Escape;

import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.P3;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;

import java.util.Arrays;

public class StateManager {

  /* steps in adding a global variable:
   
   In Viewer:
   
   1. add a check in setIntProperty or setBooleanProperty or setFloat.. or setString...
   2. create new set/get methods
   
   In StateManager
   
   3. create the global.xxx variable
   4. in registerParameter() register it so that it shows up as having a value in math
   
   */

  public final static int OBJ_BACKGROUND = 0;
  public final static int OBJ_AXIS1 = 1;
  public final static int OBJ_AXIS2 = 2;
  public final static int OBJ_AXIS3 = 3;
  public final static int OBJ_BOUNDBOX = 4;
  public final static int OBJ_UNITCELL = 5;
  public final static int OBJ_FRANK = 6;
  public final static int OBJ_MAX = 8;
  private final static String objectNameList = "background axis1      axis2      axis3      boundbox   unitcell   frank      ";

  public static String getVariableList(Map<String, SV> htVariables, int nMax,
                                       boolean withSites, boolean definedOnly) {
    SB sb = new SB();
    // user variables only:
    int n = 0;

    String[] list = new String[htVariables.size()];
    for (Map.Entry<String, SV> entry : htVariables.entrySet()) {
      String key = entry.getKey();
      SV var = entry.getValue();
      if ((withSites || !key.startsWith("site_")) && (!definedOnly || key.charAt(0) == '@'))
        list[n++] = key
            + (key.charAt(0) == '@' ? " " + var.asString() : " = "
                + varClip(key, var.escape(), nMax));
    }
    Arrays.sort(list, 0, n);
    for (int i = 0; i < n; i++)
      if (list[i] != null)
        sb.append("  ").append(list[i]).append(";\n");
    if (n == 0 && !definedOnly)
      sb.append("# --no global user variables defined--;\n");
    return sb.toString();
  }
  
  public static int getObjectIdFromName(String name) {
    if (name == null)
      return -1;
    int objID = objectNameList.indexOf(name.toLowerCase());
    return (objID < 0 ? objID : objID / 11);
  }

  static String getObjectNameFromId(int objId) {
    if (objId < 0 || objId >= OBJ_MAX)
      return null;
    return objectNameList.substring(objId * 11, objId * 11 + 11).trim();
  }

  Viewer viewer;
  Map<String, Object> saved = new Hashtable<String, Object>();
  
  String lastOrientation = "";
  String lastConnections = "";
  String lastSelected = "";
  String lastState = "";
  String lastShape = "";
  String lastCoordinates = "";

  StateManager(Viewer viewer) {
    this.viewer = viewer;
  }
  
  GlobalSettings getGlobalSettings(GlobalSettings gsOld, boolean clearUserVariables) {
    return new GlobalSettings(gsOld, clearUserVariables);
  }

  void clear(GlobalSettings global) {
    viewer.setShowAxes(false);
    viewer.setShowBbcage(false);
    viewer.setShowUnitCell(false);
    global.clear();
  }

  void setCrystallographicDefaults() {
    //axes on and mode unitCell; unitCell on; perspective depth off;
    viewer.setAxesModeUnitCell(true);
    viewer.setShowAxes(true);
    viewer.setShowUnitCell(true);
    viewer.setBooleanProperty("perspectiveDepth", false);
  }

  private void setCommonDefaults() {
    viewer.setBooleanProperty("perspectiveDepth", true);
    viewer.setFloatProperty("bondTolerance",
        JC.DEFAULT_BOND_TOLERANCE);
    viewer.setFloatProperty("minBondDistance",
        JC.DEFAULT_MIN_BOND_DISTANCE);
  }

  void setJmolDefaults() {
    setCommonDefaults();
    viewer.setStringProperty("defaultColorScheme", "Jmol");
    viewer.setBooleanProperty("axesOrientationRasmol", false);
    viewer.setBooleanProperty("zeroBasedXyzRasmol", false);
    viewer.setIntProperty("percentVdwAtom",
        JC.DEFAULT_PERCENT_VDW_ATOM);
    viewer.setIntProperty("bondRadiusMilliAngstroms",
        JC.DEFAULT_BOND_MILLIANGSTROM_RADIUS);
    viewer.setDefaultVdw("auto");
  }

  void setRasMolDefaults() {
    setCommonDefaults();
    viewer.setStringProperty("defaultColorScheme", "RasMol");
    viewer.setBooleanProperty("axesOrientationRasmol", true);
    viewer.setBooleanProperty("zeroBasedXyzRasmol", true);
    viewer.setIntProperty("percentVdwAtom", 0);
    viewer.setIntProperty("bondRadiusMilliAngstroms", 1);
    viewer.setDefaultVdw("Rasmol");
  }

  String listSavedStates() {
    String names = "";
    Iterator<String> e = saved.keySet().iterator();
    while (e.hasNext())
      names += "\n" + e.next();
    return names;
  }

  private void deleteSavedType(String type) {
    Iterator<String> e = saved.keySet().iterator();
    while (e.hasNext()) {
      String name = e.next();
      if (name.startsWith(type)) {
        e.remove();
        Logger.debug("deleted " + name);
      }
    }
  }

  void deleteSaved(String name) {
    saved.remove(name);
  }
  
  void saveSelection(String saveName, BS bsSelected) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Selected_");
      return;
    }
    saveName = lastSelected = "Selected_" + saveName;
    saved.put(saveName, BSUtil.copy(bsSelected));
  }

  boolean restoreSelection(String saveName) {
    String name = (saveName.length() > 0 ? "Selected_" + saveName
        : lastSelected);
    BS bsSelected = (BS) saved.get(name);
    if (bsSelected == null) {
      viewer.select(new BS(), false, null, false);
      return false;
    }
    viewer.select(bsSelected, false, null, false);
    return true;
  }

  void saveState(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("State_");
      return;
    }
    saveName = lastState = "State_" + saveName;
    saved.put(saveName, viewer.getStateInfo());
  }

  String getSavedState(String saveName) {
    String name = (saveName.length() > 0 ? "State_" + saveName : lastState);
    String script = (String) saved.get(name);
    return (script == null ? "" : script);
  }

  /*  
   boolean restoreState(String saveName) {
   //not used -- more efficient just to run the script 
   String name = (saveName.length() > 0 ? "State_" + saveName
   : lastState);
   String script = (String) saved.get(name);
   if (script == null)
   return false;
   viewer.script(script + CommandHistory.NOHISTORYATALL_FLAG);
   return true;
   }
   */
  void saveStructure(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Shape_");
      return;
    }
    saveName = lastShape = "Shape_" + saveName;
    saved.put(saveName, viewer.getStructureState());
  }

  String getSavedStructure(String saveName) {
    String name = (saveName.length() > 0 ? "Shape_" + saveName : lastShape);
    String script = (String) saved.get(name);
    return (script == null ? "" : script);
  }

  void saveCoordinates(String saveName, BS bsSelected) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Coordinates_");
      return;
    }
    saveName = lastCoordinates = "Coordinates_" + saveName;
    saved.put(saveName, viewer.getCoordinateState(bsSelected));
  }

  String getSavedCoordinates(String saveName) {
    String name = (saveName.length() > 0 ? "Coordinates_" + saveName
        : lastCoordinates);
    String script = (String) saved.get(name);
    return (script == null ? "" : script);
  }

  Orientation getOrientation() {
    return new Orientation(false);
  }

  String getSavedOrientationText(String saveName) {
    Orientation o;
    if (saveName != null) {
      o = getOrientation(saveName);
      return (o == null ? "" : o.getMoveToText(true));      
    } 
    SB sb = new SB();
    Iterator<String> e = saved.keySet().iterator();
    while (e.hasNext()) {
       String name = e.next();
       if (!name.startsWith("Orientation_")) {
         continue;
       }
       sb.append(((Orientation) saved.get(name)).getMoveToText(true));
    }
    return sb.toString(); 
  }


  void saveOrientation(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Orientation_");
      return;
    }
    Orientation o = new Orientation(saveName.equals("default"));
    o.saveName = lastOrientation = "Orientation_" + saveName;
    saved.put(o.saveName, o);
  }
  
  boolean restoreOrientation(String saveName, float timeSeconds, boolean isAll) {
    Orientation o = getOrientation(saveName);
    if (o == null)
      return false;
    o.restore(timeSeconds, isAll);
    //    Logger.info(listSavedStates());
    return true;
  }

  private Orientation getOrientation(String saveName) {
    String name = (saveName.length() > 0 ? "Orientation_" + saveName
        : lastOrientation);    
    return (Orientation) saved.get(name);
  }

  public class Orientation {

    String saveName;

    Matrix3f rotationMatrix = new Matrix3f();
    float xTrans, yTrans;
    float zoom, rotationRadius;
    P3 center = new P3();
    P3 navCenter = new P3();
    float xNav = Float.NaN;
    float yNav = Float.NaN;
    float navDepth = Float.NaN;
    boolean windowCenteredFlag;
    boolean navigationMode;
    //boolean navigateSurface;
    String moveToText;
    

    Orientation(boolean asDefault) {
      if (asDefault) {
        Matrix3f rotationMatrix = (Matrix3f) viewer
          .getModelSetAuxiliaryInfoValue("defaultOrientationMatrix");
        if (rotationMatrix == null)
          this.rotationMatrix.setIdentity();
        else
          this.rotationMatrix.setM(rotationMatrix);
      } else {
        viewer.getRotation(this.rotationMatrix);
      }
      xTrans = viewer.getTranslationXPercent();
      yTrans = viewer.getTranslationYPercent();
      zoom = viewer.getZoomSetting();
      center.setT(viewer.getRotationCenter());
      windowCenteredFlag = viewer.isWindowCentered();
      rotationRadius = viewer.getRotationRadius();
      navigationMode = viewer.getNavigationMode();
      //navigateSurface = viewer.getNavigateSurface();
      moveToText = viewer.getMoveToText(-1);
      if (navigationMode) {
        xNav = viewer.getNavigationOffsetPercent('X');
        yNav = viewer.getNavigationOffsetPercent('Y');
        navDepth = viewer.getNavigationDepthPercent();
        navCenter = P3.newP(viewer.getNavigationCenter());
      }
    }

    public String getMoveToText(boolean asCommand) {
      return (asCommand ? "  " + moveToText + "\n  save orientation \"" 
          + saveName.substring(12) + "\";\n" : moveToText);
    }
    
    public void restore(float timeSeconds, boolean isAll) {
      if (!isAll) {
        viewer.setRotationMatrix(rotationMatrix);
        return;
      }
      viewer.setBooleanProperty("windowCentered", windowCenteredFlag);
      viewer.setBooleanProperty("navigationMode", navigationMode);
      //viewer.setBooleanProperty("navigateSurface", navigateSurface);
      viewer.moveTo(viewer.eval, timeSeconds, center, null, Float.NaN, rotationMatrix, zoom, xTrans,
          yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
    }
  }

  void saveBonds(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Bonds_");
      return;
    }
    Connections b = new Connections();
    b.saveName = lastConnections = "Bonds_" + saveName;
    saved.put(b.saveName, b);
  }

  boolean restoreBonds(String saveName) {
    String name = (saveName.length() > 0 ? "Bonds_" + saveName
        : lastConnections);
    Connections c = (Connections) saved.get(name);
    if (c == null)
      return false;
    c.restore();
    //    Logger.info(listSavedStates());
    return true;
  }

  class Connections {

    String saveName;
    int bondCount;
    Connection[] connections;

    Connections() {
      ModelSet modelSet = viewer.getModelSet();
      if (modelSet == null)
        return;
      bondCount = modelSet.bondCount;
      connections = new Connection[bondCount + 1];
      Bond[] bonds = modelSet.bonds;
      for (int i = bondCount; --i >= 0;) {
        Bond b = bonds[i];
        connections[i] = new Connection(b.getAtomIndex1(), b.getAtomIndex2(), b
            .mad, b.colix, b.order, b.getEnergy(), b.getShapeVisibilityFlags());
      }
    }

    void restore() {
      ModelSet modelSet = viewer.getModelSet();
      if (modelSet == null)
        return;
      modelSet.deleteAllBonds();
      for (int i = bondCount; --i >= 0;) {
        Connection c = connections[i];
        int atomCount = modelSet.getAtomCount();
        if (c.atomIndex1 >= atomCount || c.atomIndex2 >= atomCount)
          continue;
        Bond b = modelSet.bondAtoms(modelSet.atoms[c.atomIndex1],
            modelSet.atoms[c.atomIndex2], c.order, c.mad, null, c.energy, false, true);
        b.setColix(c.colix);
        b.setShapeVisibilityFlags(c.shapeVisibilityFlags);
      }
      for (int i = bondCount; --i >= 0;)
        modelSet.getBondAt(i).setIndex(i);
      viewer.setShapeProperty(JC.SHAPE_STICKS, "reportAll", null);
    }
  }

  static class Connection {
    int atomIndex1;
    int atomIndex2;
    short mad;
    short colix;
    int order;
    float energy;
    int shapeVisibilityFlags;

    Connection(int atom1, int atom2, short mad, short colix, int order, float energy,
        int shapeVisibilityFlags) {
      atomIndex1 = atom1;
      atomIndex2 = atom2;
      this.mad = mad;
      this.colix = colix;
      this.order = order;
      this.energy = energy;
      this.shapeVisibilityFlags = shapeVisibilityFlags;
    }
  }

  static boolean doReportProperty(String name) {
    return (name.charAt(0) != '_' && unreportedProperties.indexOf(";" + name
        + ";") < 0);
  }
  
  protected final static String unreportedProperties =
    //these are handled individually in terms of reporting for the state
    //NOT EXCLUDING the load state settings, because although we
    //handle these specially for the CURRENT FILE, their current
    //settings won't be reflected in the load state, which is determined
    //earlier, when the file loads. 
    //
    //place any parameter here you do NOT want to have in the state
    //
    // _xxxxx variables are automatically exempt
    //
    (";ambientpercent;animationfps"
        + ";antialiasdisplay;antialiasimages;antialiastranslucent;appendnew;axescolor"
        + ";axesposition;axesmolecular;axesorientationrasmol;axesunitcell;axeswindow;axis1color;axis2color"
        + ";axis3color;backgroundcolor;backgroundmodel;bondsymmetryatoms;boundboxcolor;cameradepth"
        + ";debug;debugscript;defaultlatttice;defaults;defaultdropscript;diffusepercent;exportdrivers"
        + ";_filecaching;_filecache;fontcaching;fontscaling;forcefield;language"
        + ";legacyautobonding"
        + ";loglevel;logfile;loggestures;logcommands;measurestylechime"
        + ";loadformat;loadligandformat;smilesurlformat;pubchemformat;nihresolverformat;edsurlformat;edsurlcutoff;multiprocessor;navigationmode;"
        + ";pathforallfiles;perspectivedepth;phongexponent;perspectivemodel;preservestate;refreshing;repaintwaitms;rotationradius"
        + ";showaxes;showaxis1;showaxis2;showaxis3;showboundbox;showfrank;showtiming;showunitcell"
        + ";slabenabled;slab;slabrange;depth;zshade;zshadepower;specular;specularexponent;specularpercent;celshading;specularpower;stateversion"
        + ";statusreporting;stereo;stereostate;vibrationperiod"
        + ";unitcellcolor;visualrange;windowcentered;zerobasedxyzrasmol;zoomenabled;mousedragfactor;mousewheelfactor"
        //    saved in the hash table but not considered part of the state:
        + ";scriptqueue;scriptreportinglevel;syncscript;syncmouse;syncstereo;" 
        + ";defaultdirectory;currentlocalpath;defaultdirectorylocal"
        //    more settable Jmol variables    
        + ";ambient;bonds;colorrasmol;diffuse;frank;hetero;hidenotselected"
        + ";hoverlabel;hydrogen;languagetranslation;measurementunits;navigationdepth;navigationslab"
        + ";picking;pickingstyle;propertycolorschemeoverload;radius;rgbblue;rgbgreen;rgbred"
        + ";scaleangstromsperinch;selectionhalos;showscript;showselections;solvent;strandcount"
        + ";spinx;spiny;spinz;spinfps;navx;navy;navz;navfps;" + EnumCallback.getNameList()
        + ";undo;bondpicking;modelkitmode;allowgestures;allowkeystrokes;allowmultitouch;allowmodelkit"
        + ";").toLowerCase();

  protected static int getJmolVersionInt() {
    // 11.9.999 --> 1109999
    String s = JC.version;
    int version = -1;

    try {
      // Major number
      int i = s.indexOf(".");
      if (i < 0) {
        version = 100000 * Integer.parseInt(s);
        return version;
      }
      version = 100000 * Integer.parseInt(s.substring(0, i));

      // Minor number
      s = s.substring(i + 1);
      i = s.indexOf(".");
      if (i < 0) {
        version += 1000 * Integer.parseInt(s);
        return version;
      }
      version += 1000 * Integer.parseInt(s.substring(0, i));

      // Revision number
      s = s.substring(i + 1);
      i = s.indexOf("_");
      if (i >= 0)
        s = s.substring(0, i);
      i = s.indexOf(" ");
      if (i >= 0)
        s = s.substring(0, i);
      version += Integer.parseInt(s);
    } catch (NumberFormatException e) {
      // We simply keep the version currently found
    }

    return version;
  }

  public class GlobalSettings {

    Map<String, Object> htNonbooleanParameterValues;
    Map<String, Boolean> htBooleanParameterFlags;
    Map<String, Boolean> htPropertyFlagsRemoved;
    Map<String, SV> htUserVariables = new Hashtable<String, SV>();
    Map<String, String> databases;

    /*
     *  Mostly these are just saved and restored directly from Viewer.
     *  They are collected here for reference and to ensure that no 
     *  methods are written that bypass viewer's get/set methods.
     *  
     *  Because these are not Frame variables, they (mostly) should persist past
     *  a new file loading. There is some question in my mind whether all
     *  should be in this category.
     *  
     */

    GlobalSettings(GlobalSettings gsOld, boolean clearUserVariables) {
      registerAllValues(gsOld, clearUserVariables);
    }

    void clear() {
      Iterator<String> e = htUserVariables.keySet().iterator();
      while (e.hasNext()) {
        String key = e.next();
        if (key.charAt(0) == '@' || key.startsWith("site_"))
          e.remove();
      }

      // PER-zap settings made
      setPicked(-1);
      setI("_atomhovered", -1);
      setS("_pickinfo", "");
      setB("selectionhalos", false);
      setB("hidenotselected", false); // to synchronize with selectionManager
      setB("measurementlabels", measurementLabels = true);
      setB("drawHover", drawHover = false);
      

    }

    void registerAllValues(GlobalSettings g, boolean clearUserVariables) {
      htNonbooleanParameterValues = new Hashtable<String, Object>();
      htBooleanParameterFlags = new Hashtable<String, Boolean>();
      htPropertyFlagsRemoved = new Hashtable<String, Boolean>();
      if (g != null) {
        //persistent values not reset with the "initialize" command
        if (!clearUserVariables)
          htUserVariables = g.htUserVariables; // 12.3.7, 12.2.7
        debugScript = g.debugScript;
        disablePopupMenu = g.disablePopupMenu;
        messageStyleChime = g.messageStyleChime;
        defaultDirectory = g.defaultDirectory;
        allowGestures = g.allowGestures;
        allowModelkit = g.allowModelkit;
        allowMultiTouch = g.allowMultiTouch;
        allowKeyStrokes = g.allowKeyStrokes;
        legacyAutoBonding = g.legacyAutoBonding;
        useScriptQueue = g.useScriptQueue;
        useArcBall = g.useArcBall;
        databases = g.databases;
      }
      if (databases == null) {
        databases = new Hashtable<String, String>();
        getDataBaseList(JC.databases);
        getDataBaseList(userDatabases);
      }
      loadFormat = databases.get("pdb");
      loadLigandFormat = databases.get("ligand");
      nmrUrlFormat = databases.get("nmr");
      smilesUrlFormat = databases.get("nci") + "/file?format=sdf&get3d=True";
      nihResolverFormat = databases.get("nci");
      pubChemFormat = databases.get("pubchem");

      // beyond these six, they are just in the form load =xxx/id

      for (EnumCallback item : EnumCallback.values())
        resetValue(item.name() + "Callback", g);

      setI("historyLevel", 0); //deprecated ? doesn't do anything

      // These next are just placeholders so that the math processor
      // knows they are Jmol variables. They are held by other managers.
      // This is NOT recommended, because it is easy to forget they are 
      // here and then not reset them properly. Basically it means that
      // the other manager must ensure that the value changed there is
      // updated here, AND when an initialization occurs, they remain in
      // sync. This is difficult to manage and should be changed.
      // The good news is that this manager is initialized FIRST, so 
      // we really just have to make sure that all these values are definitely
      // also initialized within the managers. 

      setI("depth", 0); // maintained by TransformManager
      setF("gestureSwipeFactor", ActionManager.DEFAULT_GESTURE_SWIPE_FACTOR);
      setB("hideNotSelected", false); //maintained by the selectionManager
      setS("hoverLabel", ""); // maintained by the Hover shape
      setB("isKiosk", viewer.isKiosk()); // maintained by Viewer
      setS("logFile", viewer.getLogFile()); // maintained by Viewer
      setI("logLevel", Logger.getLogLevel());
      setF("mouseWheelFactor", ActionManager.DEFAULT_MOUSE_WHEEL_FACTOR);
      setF("mouseDragFactor", ActionManager.DEFAULT_MOUSE_DRAG_FACTOR);
      setI("navFps", TransformManager.DEFAULT_NAV_FPS);
      setI("navigationDepth", 0); // maintained by TransformManager
      setI("navigationSlab", 0); // maintained by TransformManager
      setI("navX", 0); // maintained by TransformManager
      setI("navY", 0); // maintained by TransformManager
      setI("navZ", 0); // maintained by TransformManager
      setS("pathForAllFiles", "");
      setI("perspectiveModel", TransformManager.DEFAULT_PERSPECTIVE_MODEL);
      setS("picking", "identify"); // maintained by ActionManager
      setS("pickingStyle", "toggle"); // maintained by ActionManager
      setB("refreshing", true); // maintained by Viewer
      setI("rotationRadius", 0); // maintained by TransformManager
      setI("scaleAngstromsPerInch", 0); // maintained by TransformManager
      setI("scriptReportingLevel", 0); // maintained by ScriptEvaluator
      setB("selectionHalos", false); // maintained by ModelSet
      setB("showaxes", false); // maintained by Axes
      setB("showboundbox", false); // maintained by Bbcage
      setB("showfrank", false); // maintained by Viewer
      setB("showUnitcell", false); // maintained by Uccage
      setI("slab", 100); // maintained by TransformManager
      setB("slabEnabled", false); // maintained by TransformManager     
      setF("slabrange", 0f); // maintained by TransformManager
      setI("spinX", 0); // maintained by TransformManager
      setI("spinY", TransformManager.DEFAULT_SPIN_Y);
      setI("spinZ", 0); // maintained by TransformManager
      setI("spinFps", TransformManager.DEFAULT_SPIN_FPS);
      setI("stereoDegrees", EnumStereoMode.DEFAULT_STEREO_DEGREES);
      setI("stateversion", 0); // only set by a saved state being recalled
      setB("syncScript", viewer.getStatusManager().syncingScripts);
      setB("syncMouse", viewer.getStatusManager().syncingMouse);
      setB("syncStereo", viewer.getStatusManager().stereoSync);
      setB("windowCentered", true); // maintained by TransformManager
      setB("zoomEnabled", true); // maintained by TransformManager
      setI("zDepth", 0); // maintained by TransformManager
      setB("zShade", false); // maintained by TransformManager
      setI("zSlab", 50); // maintained by TransformManager

      // These next values have no other place than the global Hashtables.
      // This just means that a call to viewer.getXxxxProperty() is necessary.
      // Otherwise, it's the same as if they had a global variable. 
      // It's just an issue of speed of access. Generally, these should only be
      // accessed by the user. 

      setI("_version", getJmolVersionInt());

      setB("axesWindow", true);
      setB("axesMolecular", false);
      setB("axesPosition", false);
      setB("axesUnitcell", false);
      setI("backgroundModel", 0);
      setB("colorRasmol", false);
      setS("currentLocalPath", "");
      setS("defaultLattice", "{0 0 0}");
      setS("defaultColorScheme", "Jmol");
      setS("defaultDirectoryLocal", "");
      setS("defaults", "Jmol");
      setS("defaultVDW", "Jmol");
      setS("exportDrivers", JC.EXPORT_DRIVER_LIST);
      setI("propertyAtomNumberColumnCount", 0);
      setI("propertyAtomNumberField", 0);
      setI("propertyDataColumnCount", 0);
      setI("propertyDataField", 0);
      setB("undo", true);

      // OK, all of the rest of these are maintained here as global values (below)

      setB("allowEmbeddedScripts", allowEmbeddedScripts);
      setB("allowGestures", allowGestures);
      setB("allowKeyStrokes", allowKeyStrokes);
      setB("allowModelkit", allowModelkit);
      setB("allowMultiTouch", allowMultiTouch);
      setB("allowRotateSelected", allowRotateSelected);
      setB("allowMoveAtoms", allowMoveAtoms);
      setI("ambientPercent", ambientPercent);
      setI("animationFps", animationFps);
      setB("antialiasImages", antialiasImages);
      setB("antialiasDisplay", antialiasDisplay);
      setB("antialiasTranslucent", antialiasTranslucent);
      setB("appendNew", appendNew);
      setS("appletProxy", appletProxy);
      setB("applySymmetryToBonds", applySymmetryToBonds);
      setB("atomPicking", atomPicking);
      setS("atomTypes", atomTypes);
      setB("autoBond", autoBond);
      setB("autoFps", autoFps);
      //      setParameterValue("autoLoadOrientation", autoLoadOrientation);
      setI("axesMode", axesMode.getCode());
      setF("axesScale", axesScale);
      setB("axesOrientationRasmol", axesOrientationRasmol);
      setB("bondModeOr", bondModeOr);
      setB("bondPicking", bondPicking);
      setI("bondRadiusMilliAngstroms", bondRadiusMilliAngstroms);
      setF("bondTolerance", bondTolerance);
      setF("cameraDepth", cameraDepth);
      setB("cartoonBaseEdges", cartoonBaseEdges);
      setB("cartoonFancy", cartoonFancy);
      setB("cartoonRockets", cartoonRockets);
      setB("chainCaseSensitive", chainCaseSensitive);
      setB("celShading", celShading);
      setS("dataSeparator", dataSeparator);
      setB("debugScript", debugScript);
      setS("defaultAngleLabel", defaultAngleLabel);
      setF("defaultDrawArrowScale", defaultDrawArrowScale);
      setS("defaultDirectory", defaultDirectory);
      setS("defaultDistanceLabel", defaultDistanceLabel);
      setS("defaultDropScript", defaultDropScript);
      setS("defaultLabelPDB", defaultLabelPDB);
      setS("defaultLabelXYZ", defaultLabelXYZ);
      setS("defaultLoadFilter", defaultLoadFilter);
      setS("defaultLoadScript", defaultLoadScript);
      setB("defaultStructureDSSP", defaultStructureDSSP);
      setS("defaultTorsionLabel", defaultTorsionLabel);
      setF("defaultTranslucent", defaultTranslucent);
      setI("delayMaximumMs", delayMaximumMs);
      setI("diffusePercent", diffusePercent);
      setF("dipoleScale", dipoleScale);
      setB("disablePopupMenu", disablePopupMenu);
      setB("displayCellParameters", displayCellParameters);
      setI("dotDensity", dotDensity);
      setI("dotScale", dotScale);
      setB("dotsSelectedOnly", dotsSelectedOnly);
      setB("dotSurface", dotSurface);
      setB("dragSelected", dragSelected);
      setB("drawHover", drawHover);
      setB("drawPicking", drawPicking);
      setB("dsspCalculateHydrogenAlways", dsspCalcHydrogen);
      setB("dynamicMeasurements", dynamicMeasurements);
      setS("edsUrlFormat", edsUrlFormat);
      //setParameterValue("edsUrlOptions", edsUrlOptions);
      setS("edsUrlCutoff", edsUrlCutoff);
      setB("ellipsoidArcs", ellipsoidArcs);
      setB("ellipsoidAxes", ellipsoidAxes);
      setF("ellipsoidAxisDiameter", ellipsoidAxisDiameter);
      setB("ellipsoidBall", ellipsoidBall);
      setI("ellipsoidDotCount", ellipsoidDotCount);
      setB("ellipsoidDots", ellipsoidDots);
      setB("ellipsoidFill", ellipsoidFill);
      setS("energyUnits", energyUnits);
      //      setParameterValue("_fileCaching", _fileCaching);
      //      setParameterValue("_fileCache", _fileCache);
      setB("fontScaling", fontScaling);
      setB("fontCaching", fontCaching);
      setB("forceAutoBond", forceAutoBond);
      setS("forceField", forceField);
      setB("fractionalRelative", fractionalRelative);
      setB("greyscaleRendering", greyscaleRendering);
      setF("hbondsAngleMinimum", hbondsAngleMinimum);
      setF("hbondsDistanceMaximum", hbondsDistanceMaximum);
      setB("hbondsBackbone", hbondsBackbone);
      setB("hbondsRasmol", hbondsRasmol);
      setB("hbondsSolid", hbondsSolid);
      setI("helixStep", helixStep);
      setS("helpPath", helpPath);
      setI("hermiteLevel", hermiteLevel);
      setB("hideNameInPopup", hideNameInPopup);
      setB("hideNavigationPoint", hideNavigationPoint);
      setB("highResolution", highResolutionFlag);
      setF("hoverDelay", hoverDelayMs / 1000f);
      setB("imageState", imageState);
      setB("isosurfaceKey", isosurfaceKey);
      setB("isosurfacePropertySmoothing", isosurfacePropertySmoothing);
      setI("isosurfacePropertySmoothingPower", isosurfacePropertySmoothingPower);
      setB("justifyMeasurements", justifyMeasurements);
      setB("legacyAutoBonding", legacyAutoBonding);
      setF("loadAtomDataTolerance", loadAtomDataTolerance);
      setS("loadFormat", loadFormat);
      setS("loadLigandFormat", loadLigandFormat);
      setB("logCommands", logCommands);
      setB("logGestures", logGestures);
      setB("measureAllModels", measureAllModels);
      setB("measurementLabels", measurementLabels);
      setS("measurementUnits", measureDistanceUnits);
      setI("meshScale", meshScale);
      setB("messageStyleChime", messageStyleChime);
      setF("minBondDistance", minBondDistance);
      setI("minPixelSelRadius", minPixelSelRadius);
      setI("minimizationSteps", minimizationSteps);
      setB("minimizationRefresh", minimizationRefresh);
      setB("minimizationSilent", minimizationSilent);
      setF("minimizationCriterion", minimizationCriterion);
      setB("modelKitMode", modelKitMode);
      setB("monitorEnergy", monitorEnergy);
      setF("multipleBondRadiusFactor", multipleBondRadiusFactor);
      setF("multipleBondSpacing", multipleBondSpacing);
      setB("multiProcessor", multiProcessor && (Viewer.nProcessors > 1));
      setB("navigationMode", navigationMode);
      //setParamB("navigateSurface", navigateSurface);
      setB("navigationPeriodic", navigationPeriodic);
      setF("navigationSpeed", navigationSpeed);
      setS("nmrUrlFormat", nmrUrlFormat);
      setB("partialDots", partialDots);
      setB("pdbAddHydrogens", pdbAddHydrogens); // new 12.1.51
      setB("pdbGetHeader", pdbGetHeader); // new 11.5.39
      setB("pdbSequential", pdbSequential); // new 11.5.39
      setB("perspectiveDepth", perspectiveDepth);
      setI("percentVdwAtom", percentVdwAtom);
      setI("phongExponent", phongExponent);
      setI("pickingSpinRate", pickingSpinRate);
      setS("pickLabel", pickLabel);
      setF("pointGroupLinearTolerance", pointGroupLinearTolerance);
      setF("pointGroupDistanceTolerance", pointGroupDistanceTolerance);
      setB("preserveState", preserveState);
      setS("propertyColorScheme", propertyColorScheme);
      setS("quaternionFrame", quaternionFrame);
      setB("rangeSelected", rangeSelected);
      setI("repaintWaitMs", repaintWaitMs);
      setI("ribbonAspectRatio", ribbonAspectRatio);
      setB("ribbonBorder", ribbonBorder);
      setB("rocketBarrels", rocketBarrels);
      setB("saveProteinStructureState", saveProteinStructureState);
      setB("scriptqueue", useScriptQueue);
      setB("selectAllModels", selectAllModels);
      setB("selectHetero", rasmolHeteroSetting);
      setB("selectHydrogen", rasmolHydrogenSetting);
      setF("sheetSmoothing", sheetSmoothing);
      setB("showHiddenSelectionHalos", showHiddenSelectionHalos);
      setB("showHydrogens", showHydrogens);
      setB("showKeyStrokes", showKeyStrokes);
      setB("showMeasurements", showMeasurements);
      setB("showMultipleBonds", showMultipleBonds);
      setB("showNavigationPointAlways", showNavigationPointAlways);
      setI("showScript", scriptDelay);
      setB("showtiming", showTiming);
      setB("slabByMolecule", slabByMolecule);
      setB("slabByAtom", slabByAtom);
      setB("smartAromatic", smartAromatic);
      setI("smallMoleculeMaxAtoms", smallMoleculeMaxAtoms);
      setS("smilesUrlFormat", smilesUrlFormat);
      setS("nihResolverFormat", nihResolverFormat);
      setS("pubChemFormat", pubChemFormat);
      setB("solventProbe", solventOn);
      setF("solventProbeRadius", solventProbeRadius);
      setB("specular", specular);
      setI("specularExponent", specularExponent);
      setI("specularPercent", specularPercent);
      setI("specularPower", specularPower);
      setB("ssbondsBackbone", ssbondsBackbone);
      setB("statusReporting", statusReporting);
      setI("strandCount", strandCountForStrands);
      setI("strandCountForStrands", strandCountForStrands);
      setI("strandCountForMeshRibbon", strandCountForMeshRibbon);
      setF("strutDefaultRadius", strutDefaultRadius);
      setF("strutLengthMaximum", strutLengthMaximum);
      setI("strutSpacing", strutSpacing);
      setB("strutsMultiple", strutsMultiple);
      setB("testFlag1", testFlag1);
      setB("testFlag2", testFlag2);
      setB("testFlag3", testFlag3);
      setB("testFlag4", testFlag4);
      setB("traceAlpha", traceAlpha);
      setB("twistedSheets", twistedSheets);
      setB("useArcBall", useArcBall);
      setB("useMinimizationThread", useMinimizationThread);
      setB("useNumberLocalization", useNumberLocalization);
      setF("vectorScale", vectorScale);
      setB("vectorSymmetry", vectorSymmetry);
      setF("vibrationPeriod", vibrationPeriod);
      setF("vibrationScale", vibrationScale);
      setF("visualRange", visualRange);
      setB("waitForMoveTo", waitForMoveTo);
      setB("wireframeRotation", wireframeRotation);
      setI("zDepth", zDepth);
      setB("zeroBasedXyzRasmol", zeroBasedXyzRasmol);
      setB("zoomLarge", zoomLarge);
      setI("zShadePower", zShadePower);
      setI("zSlab", zSlab);
    }

    //lighting (see GData.Shade3D

    int ambientPercent = 45;
    int diffusePercent = 84;
    boolean specular = true;
    int specularExponent = 6;  // log2 of phongExponent
    int phongExponent = 64;    // 2^specularExponent
    int specularPercent = 22;
    int specularPower = 40;
    int zDepth = 0;
    int zShadePower = 3;  // increased to 3 from 1 for Jmol 12.1.49
    int zSlab = 50; // increased to 50 from 0 in Jmol 12.3.6 and Jmol 12.2.6
     
    boolean slabByMolecule = false;
    boolean slabByAtom = false;

    //file loading

    boolean allowEmbeddedScripts = true;
    boolean appendNew = true;
    String appletProxy = "";
    boolean applySymmetryToBonds = false; //new 11.1.29
    String atomTypes = "";
    boolean autoBond = true;
//    boolean autoLoadOrientation = false; // 11.7.30 for Spartan and Sygress/CAChe loading with or without rotation
       // starting with Jmol 12.0.RC10, this setting is ignored, and FILTER "NoOrient" is required if the file
       // is to be loaded without reference to the orientation saved in the file.
    boolean axesOrientationRasmol = false;
    short bondRadiusMilliAngstroms = JC.DEFAULT_BOND_MILLIANGSTROM_RADIUS;
    float bondTolerance = JC.DEFAULT_BOND_TOLERANCE;
    String defaultDirectory = "";
    boolean defaultStructureDSSP = true; // Jmol 12.1.15
    final P3 ptDefaultLattice = new P3();
    String defaultLoadScript = "";
    String defaultLoadFilter = "";
    String defaultDropScript = "zap; load SYNC %FILE;if (%ALLOWCARTOONS && _loadScript == '' && defaultLoadScript == '' && _filetype == 'Pdb') {if ({(protein or nucleic)&*/1.1} && {*/1.1}[1].groupindex != {*/1.1}[0].groupindex){select protein or nucleic;cartoons only;}if ({visible}){color structure}else{wireframe -0.1};if (!{visible}){spacefill 23%};select *}";
//    boolean _fileCaching = false;
//    String _fileCache = "";
    boolean forceAutoBond = false;
    boolean fractionalRelative = false; // true: UNITCELL offset will change meaning of {1/2 1/2 1/2} 
    char inlineNewlineChar = '|'; //pseudo static
    String loadFormat, loadLigandFormat, nmrUrlFormat, smilesUrlFormat, nihResolverFormat, pubChemFormat;

    String edsUrlFormat = "http://eds.bmc.uu.se/eds/dfs/%LC13/%LCFILE/%LCFILE.omap";
    String edsUrlCutoff = "load('http://eds.bmc.uu.se/eds/dfs/%LC13/%LCFILE/%LCFILE.sfdat').lines.find('MAP_SIGMA').split(' ')[2]";
    String edsUrlOptions = "within 2.0 {*}";
    float minBondDistance = JC.DEFAULT_MIN_BOND_DISTANCE;
    int minPixelSelRadius = 6;
    boolean pdbAddHydrogens = false; // true to add hydrogen atoms
    boolean pdbGetHeader = false; // true to get PDB header in auxiliary info
    boolean pdbSequential = false; // true for no bonding check
    int percentVdwAtom = JC.DEFAULT_PERCENT_VDW_ATOM;
    int smallMoleculeMaxAtoms = 40000;
    boolean smartAromatic = true;
    boolean zeroBasedXyzRasmol = false;
    boolean legacyAutoBonding = false;

    void setDefaultLattice(P3 ptLattice) {
      ptDefaultLattice.setT(ptLattice);
    }

    P3 getDefaultLattice() {
      return ptDefaultLattice;
    }

    //centering and perspective

    boolean allowRotateSelected = false;
    boolean allowMoveAtoms = false;
    boolean perspectiveDepth = true;
    float visualRange = 5f;

    //solvent

    boolean solventOn = false;

    //measurements

    String defaultAngleLabel = "%VALUE %UNITS";
    String defaultDistanceLabel = "%VALUE %UNITS"; //also %_ and %a1 %a2 %m1 %m2, etc.
    String defaultTorsionLabel = "%VALUE %UNITS";
    boolean justifyMeasurements = false;
    boolean measureAllModels = false;

    // minimization  // 11.5.21 03/2008

    int minimizationSteps = 100;
    boolean minimizationRefresh = true;
    boolean minimizationSilent = false;
    float minimizationCriterion = 0.001f;

    //rendering

    boolean antialiasDisplay = false;
    boolean antialiasImages = true;
    boolean imageState = true;
    boolean antialiasTranslucent = true;
    boolean displayCellParameters = true;
    boolean dotsSelectedOnly = false;
    boolean dotSurface = true;
    int dotDensity = 3;
    int dotScale = 1;
    int meshScale = 1;
    boolean dynamicMeasurements = false;
    boolean greyscaleRendering = false;
    boolean isosurfaceKey = false;
    boolean isosurfacePropertySmoothing = true;
    int isosurfacePropertySmoothingPower = 7;
    int repaintWaitMs = 1000;
    boolean showHiddenSelectionHalos = false;
    boolean showKeyStrokes = true;
    boolean showMeasurements = true;
    boolean showTiming = false;
    boolean zoomLarge = true; //false would be like Chime
    String backgroundImageFileName;
    
    //atoms and bonds

    boolean partialDots = false;
    boolean bondModeOr = false;
    boolean hbondsBackbone = false;
    float hbondsAngleMinimum = 90f;
    float hbondsDistanceMaximum = 3.25f;
    boolean hbondsRasmol = true; // 12.0.RC3
    boolean hbondsSolid = false;
    byte modeMultipleBond = JC.MULTIBOND_NOTSMALL;
    boolean showHydrogens = true;
    boolean showMultipleBonds = true;
    boolean ssbondsBackbone = false;
    float multipleBondSpacing = -1;     // 0.35?
    float multipleBondRadiusFactor = 0; // 0.75?

    //secondary structure + Rasmol

    boolean cartoonBaseEdges = false;
    boolean cartoonRockets = false;
    boolean cartoonFancy = false;
    boolean chainCaseSensitive = false;
    int hermiteLevel = 0;
    boolean highResolutionFlag = false;
    boolean rangeSelected = false;
    boolean rasmolHydrogenSetting = true;
    boolean rasmolHeteroSetting = true;
    int ribbonAspectRatio = 16;
    boolean ribbonBorder = false;
    boolean rocketBarrels = false;
    float sheetSmoothing = 1; // 0: traceAlpha on alphas for helix, 1 on midpoints
    boolean traceAlpha = true;
    boolean twistedSheets = false;

    //misc

    boolean allowGestures = false;
    boolean allowModelkit = true;
    boolean allowMultiTouch = true; // but you still need to set the parameter multiTouchSparshUI=true
    boolean allowKeyStrokes = false;
    int animationFps = 10;
    boolean atomPicking = true;
    boolean autoFps = false;
    EnumAxesMode axesMode = EnumAxesMode.BOUNDBOX;
    float axesScale = 2;
    boolean bondPicking = false;
    float cameraDepth = 3.0f;
    boolean celShading = false;
    String dataSeparator = "~~~";
    boolean debugScript = false;
    float defaultDrawArrowScale = 0.5f;
    String defaultLabelXYZ = "%a";
    String defaultLabelPDB = "%m%r";
    float defaultTranslucent = 0.5f;
    int delayMaximumMs = 0;
    float dipoleScale = 1.0f;
    boolean disablePopupMenu = false;
    boolean dragSelected = false;
    boolean drawHover = false;
    boolean drawPicking = false;
    boolean dsspCalcHydrogen = true;
    String energyUnits = "kJ";
    String helpPath = JC.DEFAULT_HELP_PATH;
    boolean fontScaling = false;
    boolean fontCaching = true;
    String forceField = "MMFF";
    int helixStep = 1;
    boolean hideNameInPopup = false;
    int hoverDelayMs = 500;
    float loadAtomDataTolerance = 0.01f;
    boolean logCommands = false;
    boolean logGestures = false;
    String measureDistanceUnits = "nanometers";
    boolean measurementLabels = true;
    boolean messageStyleChime = false;
    boolean monitorEnergy = false;
    boolean multiProcessor = true;
    int pickingSpinRate = 10;
    String pickLabel = "";
    float pointGroupDistanceTolerance = 0.2f;
    float pointGroupLinearTolerance = 8.0f;
    public boolean preserveState = true;
    String propertyColorScheme = "roygb";
    String quaternionFrame = "p"; // was c prior to Jmol 11.7.47
    boolean saveProteinStructureState = true;
    float solventProbeRadius = 1.2f;
    int scriptDelay = 0;
    boolean selectAllModels = true;
    boolean statusReporting = true;
    int strandCountForStrands = 5;
    int strandCountForMeshRibbon = 7;
    int strutSpacing = 6;
    float strutLengthMaximum = 7.0f;
    float strutDefaultRadius = JC.DEFAULT_STRUT_RADIUS;
    boolean strutsMultiple = false; //on a single position    
    boolean useArcBall = false;
    boolean useMinimizationThread = true;
    boolean useNumberLocalization = true;
    boolean useScriptQueue = true;
    boolean waitForMoveTo = true; // Jmol 11.9.24
    float vectorScale = 1f;
    boolean vectorSymmetry = false; // Jmol 12.3.2
    float vibrationPeriod = 1f;
    float vibrationScale = 1f;
    boolean wireframeRotation = false;

    // window

    boolean hideNavigationPoint = false;
    boolean navigationMode = false;
    //boolean navigateSurface = false;
    boolean navigationPeriodic = false;
    float navigationSpeed = 5;
    boolean showNavigationPointAlways = false;
    String stereoState = null;
    boolean modelKitMode = false;

    // special persistent object characteristics -- bbcage, uccage, axes:

    int[] objColors = new int[OBJ_MAX];
    boolean[] objStateOn = new boolean[OBJ_MAX];
    int[] objMad = new int[OBJ_MAX];

    boolean ellipsoidAxes = false;
    boolean ellipsoidDots = false;
    boolean ellipsoidArcs = false;
    boolean ellipsoidFill = false;
    boolean ellipsoidBall = true;

    int ellipsoidDotCount = 200;
    float ellipsoidAxisDiameter = 0.02f;

    //testing

    boolean testFlag1 = false;
    boolean testFlag2 = false;
    boolean testFlag3 = false;
    boolean testFlag4 = false;

    //controlled access:

    void setUnits(String units) {
      String mu = measureDistanceUnits;
      String eu = energyUnits;
      if (units.equalsIgnoreCase("angstroms"))
        measureDistanceUnits = "angstroms";
      else if (units.equalsIgnoreCase("nanometers")
          || units.equalsIgnoreCase("nm"))
        measureDistanceUnits = "nanometers";
      else if (units.equalsIgnoreCase("picometers")
          || units.equalsIgnoreCase("pm"))
        measureDistanceUnits = "picometers";
      else if (units.equalsIgnoreCase("bohr") || units.equalsIgnoreCase("au"))
        measureDistanceUnits = "au";
      else if (units.equalsIgnoreCase("vanderwaals") || units.equalsIgnoreCase("vdw"))
        measureDistanceUnits = "vdw";
      else if (units.equalsIgnoreCase("kj"))
        energyUnits = "kJ";
      else if (units.equalsIgnoreCase("kcal"))
        energyUnits = "kcal";
      if (!mu.equalsIgnoreCase(measureDistanceUnits))
        setS("measurementUnits", measureDistanceUnits);
      else if (!eu.equalsIgnoreCase(energyUnits)) 
        setS("energyUnits", energyUnits);
    }

    boolean isJmolVariable(String key) {
      return key.charAt(0) == '_'
          || htNonbooleanParameterValues.containsKey(key = key.toLowerCase())
          || htBooleanParameterFlags.containsKey(key)
          || unreportedProperties.indexOf(";" + key + ";") >= 0;
    }

    private void resetValue(String name, GlobalSettings g) {
      setS(name, g == null ? "" : (String) g.getParameter(name));
    }
    
    public void setB(String name, boolean value) {
      name = name.toLowerCase();
      if (htNonbooleanParameterValues.containsKey(name))
        return; // don't allow setting boolean of a numeric
      htBooleanParameterFlags.put(name, value ? Boolean.TRUE : Boolean.FALSE);
    }

    void setI(String name, int value) {
      name = name.toLowerCase();
      if (htBooleanParameterFlags.containsKey(name))
        return; // don't allow setting numeric of a boolean
      htNonbooleanParameterValues.put(name, Integer.valueOf(value));
    }

    public void setF(String name, float value) {
      if (Float.isNaN(value))
        return;
      name = name.toLowerCase();
      if (htBooleanParameterFlags.containsKey(name))
        return; // don't allow setting numeric of a boolean
      htNonbooleanParameterValues.put(name, Float.valueOf(value));
    }

    void setS(String name, String value) {
      name = name.toLowerCase();
      if (value == null || htBooleanParameterFlags.containsKey(name))
        return; // don't allow setting string of a boolean
      htNonbooleanParameterValues.put(name, value);
    }

    void removeParam(String key) {
      // used by resetError to remove _errorMessage
      // used by setSmilesString to remove _smilesString
      // used by setAxesModeMolecular to remove axesUnitCell
      //   and either axesWindow or axesMolecular
      // used by setAxesModeUnitCell to remove axesMolecular
      //   and either remove axesWindow or axesUnitCell

      key = key.toLowerCase();
      if (htBooleanParameterFlags.containsKey(key)) {
        htBooleanParameterFlags.remove(key);
        if (!htPropertyFlagsRemoved.containsKey(key))
          htPropertyFlagsRemoved.put(key, Boolean.FALSE);
        return;
      }
      if (htNonbooleanParameterValues.containsKey(key))
        htNonbooleanParameterValues.remove(key);
    }

    SV setUserVariable(String key, SV var) {
      if (var == null) 
        return null;
//      System.out.println("stateman setting user variable " + key );
      key = key.toLowerCase();
      htUserVariables.put(key, var.setName(key).setGlobal());
      return var;
    }

    void unsetUserVariable(String key) {
      if (key.equals("all") || key.equals("variables")) {
        htUserVariables.clear();
        Logger.info("all user-defined variables deleted");
      } else if (htUserVariables.containsKey(key)) {
        Logger.info("variable " + key + " deleted");
        htUserVariables.remove(key);
      }
    }

    void removeUserVariable(String key) {
      htUserVariables.remove(key);
    }

    SV getUserVariable(String name) {
      if (name == null)
        return null;
      name = name.toLowerCase();
      return htUserVariables.get(name);
    }

    String getParameterEscaped(String name, int nMax) {
      name = name.toLowerCase();
      if (htNonbooleanParameterValues.containsKey(name)) {
        Object v = htNonbooleanParameterValues.get(name);
        return varClip(name, Escape.e(v), nMax);
      }
      if (htBooleanParameterFlags.containsKey(name))
        return htBooleanParameterFlags.get(name).toString();
      if (htUserVariables.containsKey(name))
        return htUserVariables.get(name).escape();
      if (htPropertyFlagsRemoved.containsKey(name))
        return "false";
      return "<not defined>";
    }

    /**
     * 
     * strictly a getter -- returns "" if not found
     * 
     * @param name
     * @return      a Integer, Float, String, BitSet, or Variable
     */
    Object getParameter(String name) {
      Object v = getParam(name, false);
      return (v == null ? "" : v);
    }

    /**
     *  
     * 
     * @param name
     * @param doSet
     * @return     a new variable if possible, but null if "_xxx"
     * 
     */
    SV getOrSetNewVariable(String name, boolean doSet) {
      if (name == null || name.length() == 0)
        name = "x";
      Object v = getParam(name, true);
      return (v == null && doSet && name.charAt(0) != '_' ?
        setUserVariable(name, SV.newVariable(T.string, ""))
         : SV.getVariable(v));
    }

    Object getParam(String name, boolean asVariable) {
      name = name.toLowerCase();
      if (name.equals("_memory")) {
      	float bTotal = 0;
      	float bFree = 0;
      	try {
          Runtime runtime = Runtime.getRuntime();
          bTotal = runtime.totalMemory() / 1000000f;
          bFree = runtime.freeMemory() / 1000000f;
        } catch (Throwable e) {
      		// Runtime absent (JavaScript)
      	}
        String value = TextFormat.formatDecimal(bTotal - bFree, 1) + "/"
            + TextFormat.formatDecimal(bTotal, 1);
        htNonbooleanParameterValues.put("_memory", value);
      }
      if (htNonbooleanParameterValues.containsKey(name))
        return htNonbooleanParameterValues.get(name);
      if (htBooleanParameterFlags.containsKey(name))
        return htBooleanParameterFlags.get(name);
      if (htPropertyFlagsRemoved.containsKey(name))
        return Boolean.FALSE;
      if (htUserVariables.containsKey(name)) {
        SV v = htUserVariables.get(name);
        return (asVariable ? v : SV.oValue(v));
      }
      return null;
    }

    String getVariableList() {
      return StateManager.getVariableList(htUserVariables, 0, true, false);
    }

    // static because we don't plan to be changing these
    Map<EnumStructure, float[]> structureList = new Hashtable<EnumStructure, float[]>();
    
    {
      structureList.put(EnumStructure.TURN, 
          new float[] { // turn
              30, 90, -15, 95,
          });
      structureList.put(EnumStructure.SHEET, 
      new float[] { // sheet
          -180, -10,   70,  180, 
          -180, -45, -180, -130, 
           140, 180,   90, 180, 
        });
      structureList.put(EnumStructure.HELIX, 
      new float[] {  // helix
        -160, 0, -100, 45,
      });
    }
    
    boolean haveSetStructureList;
    private String[] userDatabases;
   
    public void setStructureList(float[] list, EnumStructure type) {
      haveSetStructureList = true;
      structureList.put(type, list);
    }
    
    public Map<EnumStructure, float[]> getStructureList() {
      return structureList;
    }

    void setPicked(int atomIndex) {
      SV pickedSet = null;
      if (atomIndex >= 0) {
        setI("_atompicked", atomIndex);
        pickedSet = (SV) getParam("picked", true);
      }
      if (pickedSet == null || pickedSet.tok != T.bitset) {
        pickedSet = SV.newVariable(T.bitset, new BS());
        setUserVariable("picked", pickedSet);
      }
      if (atomIndex >= 0)
        SV.getBitSet(pickedSet, false).set(atomIndex);
    }

    public String resolveDataBase(String database, String id) {
      String format = databases.get(database.toLowerCase());
      if (format == null)
        return null;
      if (id.indexOf("/") < 0) {
        if (database.equals("pubchem"))
          id = "name/" + id;
        else if (database.equals("nci"))
          id += "/file?format=sdf&get3d=True";
      }
      return (format.indexOf("%FILE") < 0 ? format + id : TextFormat
          .formatStringS(format, "FILE", id));
    }

    private void getDataBaseList(String[] list) {
      if (list == null)
        return;
      for (int i = 0; i < list.length; i += 2)
        databases.put(list[i].toLowerCase(), list[i + 1]);
    }
  }

  public static String varClip(String name, String sv, int nMax) {
    if (nMax > 0 && sv.length() > nMax)
      sv = sv.substring(0, nMax) + " #...more (" + sv.length()
          + " bytes -- use SHOW " + name + " or MESSAGE @" + name
          + " to view)";
    return sv;
  }

  ///////// state serialization 

}
