/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-11 15:47:18 -0500 (Tue, 11 May 2010) $
 * $Revision: 13064 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.jmol.popup;

import org.jmol.api.JmolPopupInterface;
import org.jmol.i18n.GT;
import org.jmol.i18n.Language;
import org.jmol.util.BS;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

import org.jmol.util.JmolList;

import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Hashtable;

abstract public class GenericPopup implements JmolPopupInterface,
    JmolAbstractMenu {

  //list is saved in http://www.stolaf.edu/academics/chemapps/jmol/docs/misc
  protected final static boolean dumpList = false;
  protected final static int UPDATE_NEVER = -1;
  private final static int UPDATE_ALL = 0;
  private final static int UPDATE_CONFIG = 1;
  private final static int UPDATE_SHOW = 2;

  //public void finalize() {
  //  System.out.println("JmolPopup " + this + " finalize");
  //}

  protected Viewer viewer;
  protected Map<String, Object> htCheckbox = new Hashtable<String, Object>();
  protected Properties menuText = new Properties();
  protected Object buttonGroup;
  protected String currentMenuItemId;
  protected String strMenuStructure;
  protected int updateMode;

  protected String menuName;
  private Object frankPopup, popupMenu;
  protected Object thisPopup;
  private int nFrankList = 0;
  private int itemMax = 25;
  private int titleWidthMax = 20;
  private int thisx, thisy;
  private String nullModelSetName, modelSetName;
  private String modelSetFileName, modelSetRoot;
  private String currentFrankId = null;
  private String configurationSelected = "";
  private String altlocs;

  private Object[][] frankList = new Object[10][]; //enough to cover menu drilling

  private Map<String, Object> modelSetInfo;
  private Map<String, Object> modelInfo;

  private Map<String, Object> htMenus = new Hashtable<String, Object>();
  private JmolList<Object> NotPDB = new  JmolList<Object>();
  private JmolList<Object> PDBOnly = new  JmolList<Object>();
  private JmolList<Object> FileUnitOnly = new  JmolList<Object>();
  private JmolList<Object> FileMolOnly = new  JmolList<Object>();
  private JmolList<Object> UnitcellOnly = new  JmolList<Object>();
  private JmolList<Object> SingleModelOnly = new  JmolList<Object>();
  private JmolList<Object> FramesOnly = new  JmolList<Object>();
  private JmolList<Object> VibrationOnly = new  JmolList<Object>();
  private JmolList<Object> SymmetryOnly = new  JmolList<Object>();
  private JmolList<Object> SignedOnly = new  JmolList<Object>();
  private JmolList<Object> AppletOnly = new  JmolList<Object>();
  private JmolList<Object> ChargesOnly = new  JmolList<Object>();
  private JmolList<Object> TemperatureOnly = new  JmolList<Object>();

  private boolean allowSignedFeatures;
  private boolean fileHasUnitCell;
  private boolean haveBFactors;
  private boolean haveCharges;
  private boolean isApplet;
  private boolean isLastFrame;
  private boolean isMultiConfiguration;
  private boolean isMultiFrame;
  private boolean isPDB;
  private boolean isSigned;
  private boolean isSymmetry;
  private boolean isUnitCell;
  private boolean isVibration;
  private boolean isZapped;

  private int modelIndex, modelCount, atomCount;
  private int aboutComputedMenuBaseCount;

  private String group3List;
  private int[] group3Counts;
  private JmolList<String> cnmrPeaks;
  private JmolList<String> hnmrPeaks;

  public GenericPopup() {
    // required by reflection
  }

  ////// JmolPopupInterface methods //////

  private final static int MENUITEM_HEIGHT = 20;

  public void jpiDispose() {
    menuClearListeners(popupMenu);
    menuClearListeners(frankPopup);
    popupMenu = frankPopup = thisPopup = null;
  }

  public Object jpiGetMenuAsObject() {
    return popupMenu;
  }
  public String jpiGetMenuAsString(String title) {
    updateForShow();
    int pt = title.indexOf("|");
    if (pt >= 0) {
      String type = title.substring(pt);
      title = title.substring(0, pt);
      if (type.indexOf("current") >= 0) {
        SB sb = new SB();
        Object menu = htMenus.get(menuName);
        menuGetAsText(sb, 0, menu, "PopupMenu");
        return sb.toString();
      }
    }
    return (new MainPopupResourceBundle(strMenuStructure, null))
        .getMenuAsText(title);
  }

  public void jpiShow(int x, int y) {
    // main entry point from Viewer
    // called via JmolPopupInterface
    if (!viewer.haveDisplay)
      return;
    show(x, y, false);
    if (x < 0) {
      getViewerData();
      setFrankMenu(currentMenuItemId);
      thisx = -x - 50;
      if (nFrankList > 1) {
        thisy = y - nFrankList * MENUITEM_HEIGHT;
        menuShowPopup(frankPopup, thisx, thisy);
        return;
      }
    }
    restorePopupMenu();
    menuShowPopup(popupMenu, thisx, thisy);
  }

  @SuppressWarnings("unchecked")
  public void jpiUpdateComputedMenus() {
    if (updateMode == UPDATE_NEVER)
      return;
    updateMode = UPDATE_ALL;
    getViewerData();
    //System.out.println("jmolPopup updateComputedMenus " + modelSetFileName + " " + modelSetName + " " + atomCount);
    updateSelectMenu();
    updateFileMenu();
    updateElementsComputedMenu(viewer.getElementsPresentBitSet(modelIndex));
    updateHeteroComputedMenu(viewer.getHeteroList(modelIndex));
    updateSurfMoComputedMenu((Map<String, Object>) modelInfo.get("moData"));
    updateFileTypeDependentMenus();
    updatePDBComputedMenus();
    updateMode = UPDATE_CONFIG;
    updateConfigurationComputedMenu();
    updateSYMMETRYComputedMenus();
    updateFRAMESbyModelComputedMenu();
    updateModelSetComputedMenu();
    updateLanguageSubmenu();
    updateAboutSubmenu();
  }

  ///////// protected methods //////////
  
  /**
   * used only by ModelKit
   * 
   * @param ret  
   * @return Object
   */
  protected Object getEntryIcon(String[] ret) {
    return null;
  }
    
  protected void checkMenuFocus(String name, String cmd, boolean isFocus) {
    if (name.indexOf("Focus") < 0)
      return;
    if (isFocus) {
      viewer.script("selectionHalos ON;" + cmd);
    } else {
      viewer.script("selectionHalos OFF");
    }
  }

  protected void checkBoxStateChanged(Object source) {
    restorePopupMenu();
    menuSetCheckBoxValue(source);
    String id = menuGetId(source);
    if (id != null) {
      currentMenuItemId = id;
    }
  }

  static protected void addItemText(SB sb, char type, int level,
                                    String name, String label, String script,
                                    String flags) {
    sb.appendC(type).appendI(level).appendC('\t').append(name);
    if (label == null) {
      sb.append(".\n");
      return;
    }
    sb.append("\t").append(label).append("\t").append(
        script == null || script.length() == 0 ? "-" : script).append("\t")
        .append(flags).append("\n");
  }

  protected String fixScript(String id, String script) {
    int pt;
    if (script == "" || id.endsWith("Checkbox"))
      return script;

    if (script.indexOf("SELECT") == 0) {
      return "select thisModel and (" + script.substring(6) + ")";
    }

    if ((pt = id.lastIndexOf("[")) >= 0) {
      // setSpin
      id = id.substring(pt + 1);
      if ((pt = id.indexOf("]")) >= 0)
        id = id.substring(0, pt);
      id = id.replace('_', ' ');
      if (script.indexOf("[]") < 0)
        script = "[] " + script;
      return TextFormat.simpleReplace(script, "[]", id);
    } else if (script.indexOf("?FILEROOT?") >= 0) {
      script = TextFormat.simpleReplace(script, "FILEROOT?", modelSetRoot);
    } else if (script.indexOf("?FILE?") >= 0) {
      script = TextFormat.simpleReplace(script, "FILE?", modelSetFileName);
    } else if (script.indexOf("?PdbId?") >= 0) {
      script = TextFormat.simpleReplace(script, "PdbId?", "=xxxx");
    }
    return script;
  }

  protected void initialize(Viewer viewer, PopupResource bundle, String title) {
    this.viewer = viewer;
    menuName = title;
    popupMenu = menuCreatePopup(title);
    thisPopup = popupMenu;
    menuSetListeners();
    htMenus.put(title, popupMenu);
    allowSignedFeatures = (!viewer.isApplet() || viewer
        .getBooleanProperty("_signedApplet"));
    addMenuItems("", title, popupMenu, bundle);
    try {
      jpiUpdateComputedMenus();
    } catch (NullPointerException e) {
      // ignore -- the frame just wasn't ready yet;
      // updateComputedMenus() will be called again when the frame is ready; 
    }
  }

  protected void restorePopupMenu() {
    thisPopup = popupMenu;
    if (nFrankList < 2)
      return;
    // first entry is just the main item
    for (int i = nFrankList; --i > 0;) {
      Object[] f = frankList[i];
      /**
       * In JSmol, a menu item can belong to more than one menu, but in Java
       * when it is added to one menu it is lost from another.
       * 
       * @j2sNative
       * 
       *            f[1].parent = f[0];
       * 
       */
      {
        menuInsertSubMenu(f[0], f[1], ((Integer) f[2]).intValue());
      }
    }
    nFrankList = 1;
  }

  /**
   * (1) setOption --> set setOption true or set setOption false
   * 
   * @param item
   * 
   * @param what
   *        option to set
   * @param TF
   *        true or false
   */
  protected void setCheckBoxValue(Object item, String what, boolean TF) {
    checkForCheckBoxScript(item, what, TF);
    if (what.indexOf("#CONFIG") >= 0) {
      configurationSelected = what;
      updateConfigurationComputedMenu();
      updateModelSetComputedMenu();
      return;
    }
  }

  ///////// private methods /////////
  
  private static boolean checkBoolean(Map<String, Object> info, String key) {
    return (info != null && info.get(key) == Boolean.TRUE); // not "Boolean.TRUE.equals(...)" (not working in Java2Script yet)
  }

  @SuppressWarnings("unchecked")
  private void getViewerData() {
    isApplet = viewer.isApplet();
    isSigned = (viewer.getBooleanProperty("_signedApplet"));
    modelSetName = viewer.getModelSetName();
    modelSetFileName = viewer.getModelSetFileName();
    int i = modelSetFileName.lastIndexOf(".");
    isZapped = ("zapped".equals(modelSetName));
    if (isZapped || "string".equals(modelSetFileName)
        || "files".equals(modelSetFileName)
        || "string[]".equals(modelSetFileName))
      modelSetFileName = "";
    modelSetRoot = modelSetFileName.substring(0, i < 0 ? modelSetFileName
        .length() : i);
    if (modelSetRoot.length() == 0)
      modelSetRoot = "Jmol";
    modelIndex = viewer.getDisplayModelIndex();
    modelCount = viewer.getModelCount();
    atomCount = viewer.getAtomCountInModel(modelIndex);
    modelSetInfo = viewer.getModelSetAuxiliaryInfo();
    modelInfo = viewer.getModelAuxiliaryInfo(modelIndex);
    if (modelInfo == null)
      modelInfo = new Hashtable<String, Object>();
    isPDB = checkBoolean(modelSetInfo, "isPDB");
    isMultiFrame = (modelCount > 1);
    isSymmetry = checkBoolean(modelInfo, "hasSymmetry");
    isUnitCell = checkBoolean(modelInfo, "notionalUnitcell");
    fileHasUnitCell = (isPDB && isUnitCell || checkBoolean(modelInfo,
        "fileHasUnitCell"));
    isLastFrame = (modelIndex == modelCount - 1);
    altlocs = viewer.getAltLocListInModel(modelIndex);
    isMultiConfiguration = (altlocs.length() > 0);
    isVibration = (viewer.modelHasVibrationVectors(modelIndex));
    haveCharges = (viewer.havePartialCharges());
    haveBFactors = (viewer.getBooleanProperty("haveBFactors"));
    cnmrPeaks = (JmolList<String>) modelInfo.get("jdxAtomSelect_13CNMR");
    hnmrPeaks = (JmolList<String>) modelInfo.get("jdxAtomSelect_1HNMR");
  }

  private void updateFileTypeDependentMenus() {
    for (int i = 0; i < NotPDB.size(); i++)
      menuEnable(NotPDB.get(i), !isPDB);
    for (int i = 0; i < PDBOnly.size(); i++)
      menuEnable(PDBOnly.get(i), isPDB);
    for (int i = 0; i < UnitcellOnly.size(); i++)
      menuEnable(UnitcellOnly.get(i), isUnitCell);
    for (int i = 0; i < FileUnitOnly.size(); i++)
      menuEnable(FileUnitOnly.get(i), isUnitCell || fileHasUnitCell);
    for (int i = 0; i < FileMolOnly.size(); i++)
      menuEnable(FileMolOnly.get(i), isUnitCell || fileHasUnitCell);
    for (int i = 0; i < SingleModelOnly.size(); i++)
      menuEnable(SingleModelOnly.get(i), isLastFrame);
    for (int i = 0; i < FramesOnly.size(); i++)
      menuEnable(FramesOnly.get(i), isMultiFrame);
    for (int i = 0; i < VibrationOnly.size(); i++)
      menuEnable(VibrationOnly.get(i), isVibration);
    for (int i = 0; i < SymmetryOnly.size(); i++)
      menuEnable(SymmetryOnly.get(i), isSymmetry && isUnitCell);
    for (int i = 0; i < SignedOnly.size(); i++)
      menuEnable(SignedOnly.get(i), isSigned || !isApplet);
    for (int i = 0; i < AppletOnly.size(); i++)
      menuEnable(AppletOnly.get(i), isApplet);
    for (int i = 0; i < ChargesOnly.size(); i++)
      menuEnable(ChargesOnly.get(i), haveCharges);
    for (int i = 0; i < TemperatureOnly.size(); i++)
      menuEnable(TemperatureOnly.get(i), haveBFactors);
  }

  private void addMenuItems(String parentId, String key, Object menu,
                            PopupResource popupResourceBundle) {
    String id = parentId + "." + key;
    String value = popupResourceBundle.getStructure(key);
    //Logger.debug(id + " --- " + value);
    if (value == null) {
      menuCreateItem(menu, "#" + key, "", "");
      return;
    }
    // process predefined @terms
    StringTokenizer st = new StringTokenizer(value);
    String item;
    while (value.indexOf("@") >= 0) {
      String s = "";
      while (st.hasMoreTokens())
        s += " "
            + ((item = st.nextToken()).startsWith("@") ? popupResourceBundle
                .getStructure(item) : item);
      value = s.substring(1);
      st = new StringTokenizer(value);
    }
    while (st.hasMoreTokens()) {
      item = st.nextToken();
      if (!checkKey(item))
        continue;
      String label = popupResourceBundle.getWord(item);
      Object newMenu = null;
      String script = "";
      boolean isCB = false;
      if (label.equals("null")) {
        // user has taken this menu item out
        continue;
      } else if (item.indexOf("Menu") >= 0) {
        if (item.indexOf("more") < 0)
          buttonGroup = null;
        Object subMenu = menuNewSubMenu(label, id + "." + item);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(item, subMenu);
        if (item.indexOf("Computed") < 0)
          addMenuItems(id, item, subMenu, popupResourceBundle);
        checkSpecialMenu(item, subMenu, label);
        newMenu = subMenu;
      } else if ("-".equals(item)) {
        menuAddSeparator(menu);
        continue;
      } else if (item.endsWith("Checkbox")
          || (isCB = (item.endsWith("CB") || item.endsWith("RD")))) {
        // could be "PRD" -- set picking checkbox
        script = popupResourceBundle.getStructure(item);
        String basename = item.substring(0, item.length() - (!isCB ? 8 : 2));
        boolean isRadio = (isCB && item.endsWith("RD"));
        if (script == null || script.length() == 0 && !isRadio)
          script = "set " + basename + " T/F";
        newMenu = menuCreateCheckboxItem(menu, label, basename + ":" + script,
            id + "." + item, false, isRadio);
        rememberCheckbox(basename, newMenu);
        if (isRadio)
          menuAddButtonGroup(newMenu);
      } else {
        script = popupResourceBundle.getStructure(item);
        if (script == null)
          script = item;
        newMenu = menuCreateItem(menu, label, script, id + "." + item);
      }

      if (!allowSignedFeatures && item.startsWith("SIGNED"))
        menuEnable(newMenu, false);
      if (item.indexOf("VARIABLE") >= 0)
        htMenus.put(item, newMenu);
      // menus or menu items:
      if (item.indexOf("!PDB") >= 0) {
        NotPDB.addLast(newMenu);
      } else if (item.indexOf("PDB") >= 0) {
        PDBOnly.addLast(newMenu);
      }
      if (item.indexOf("URL") >= 0) {
        AppletOnly.addLast(newMenu);
      } else if (item.indexOf("CHARGE") >= 0) {
        ChargesOnly.addLast(newMenu);
      } else if (item.indexOf("BFACTORS") >= 0) {
        TemperatureOnly.addLast(newMenu);
      } else if (item.indexOf("UNITCELL") >= 0) {
        UnitcellOnly.addLast(newMenu);
      } else if (item.indexOf("FILEUNIT") >= 0) {
        FileUnitOnly.addLast(newMenu);
      } else if (item.indexOf("FILEMOL") >= 0) {
        FileMolOnly.addLast(newMenu);
      }

      if (item.indexOf("!FRAMES") >= 0) {
        SingleModelOnly.addLast(newMenu);
      } else if (item.indexOf("FRAMES") >= 0) {
        FramesOnly.addLast(newMenu);
      }

      if (item.indexOf("VIBRATION") >= 0) {
        VibrationOnly.addLast(newMenu);
      } else if (item.indexOf("SYMMETRY") >= 0) {
        SymmetryOnly.addLast(newMenu);
      }
      if (item.startsWith("SIGNED"))
        SignedOnly.addLast(newMenu);

      if (dumpList) {
        String str = item.endsWith("Menu") ? "----" : id + "." + item + "\t"
            + label + "\t" + fixScript(id + "." + item, script);
        str = "addMenuItem('\t" + str + "\t')";
        Logger.info(str);
      }
    }
  }

  /**
   * @param key  
   * @return true unless a JAVA-only key in JavaScript
   */
  private boolean checkKey(String key) {
    /**
     * @j2sNative
     *
     * return (key.indexOf("JAVA") < 0 && !(key.indexOf("NOGL") && this.viewer.isJS3D));
     * 
     */
    {
      return true;
    }
  }

  private void rememberCheckbox(String key, Object checkboxMenuItem) {
    htCheckbox.put(key + "::" + htCheckbox.size(), checkboxMenuItem);
  }

  private void checkForCheckBoxScript(Object item, String what, boolean TF) {
    if (what.indexOf("##") < 0) {
      int pt = what.indexOf(":");
      if (pt < 0) {
        Logger.error("check box " + item + " IS " + what);
        return;
      }
      // name:trueAction|falseAction
      String basename = what.substring(0, pt);
      if (viewer.getBooleanProperty(basename) == TF)
        return;
      if (basename.endsWith("P!")) {
        if (basename.indexOf("??") >= 0) {
          what = menuSetCheckBoxOption(item, basename, what);
        } else {
          if (!TF)
            return;
          what = "set picking " + basename.substring(0, basename.length() - 2);
        }
      } else {
        what = what.substring(pt + 1);
        if ((pt = what.indexOf("|")) >= 0)
          what = (TF ? what.substring(0, pt) : what.substring(pt + 1)).trim();
        what = TextFormat.simpleReplace(what, "T/F", (TF ? " TRUE" : " FALSE"));
      }
    }
    viewer.evalStringQuiet(what);
  }

  public void checkMenuClick(Object source, String script) {
    restorePopupMenu();
    if (script == null || script.length() == 0)
      return;
    if (script.equals("MAIN")) {
      show(thisx, thisy, true);
      return;
    }
    String id = menuGetId(source);
    if (id != null) {
      script = fixScript(id, script);
      currentMenuItemId = id;
    }
    viewer.evalStringQuiet(script);
  }

  private Object addMenuItem(Object menuItem, String entry) {
    return menuCreateItem(menuItem, entry, "", null);
  }

  private void checkSpecialMenu(String item, Object subMenu, String word) {
    // these will need tweaking:
    if ("aboutComputedMenu".equals(item)) {
      aboutComputedMenuBaseCount = menuGetItemCount(subMenu);
    } else if ("modelSetMenu".equals(item)) {
      nullModelSetName = word;
      menuEnable(subMenu, false);
    }
  }

  private void updateFileMenu() {
    Object menu = htMenus.get("fileMenu");
    if (menu == null)
      return;
    String text = getMenuText("writeFileTextVARIABLE");
    menu = htMenus.get("writeFileTextVARIABLE");
    if (modelSetFileName.equals("zapped") || modelSetFileName.equals("")) {
      menuSetLabel(menu, GT._("No atoms loaded"));
      menuEnableItem(menu, false);
    } else {
      menuSetLabel(menu, GT._(text, modelSetFileName));
      menuEnableItem(menu, true);
    }
  }

  private String getMenuText(String key) {
    String str = menuText.getProperty(key);
    return (str == null ? key : str);
  }

  private void updateSelectMenu() {
    Object menu = htMenus.get("selectMenuText");
    if (menu == null)
      return;
    menuEnable(menu, atomCount != 0);
    menuSetLabel(menu, GT._(getMenuText("selectMenuText"), viewer
        .getSelectionCount()));
  }

  private void updateElementsComputedMenu(BS elementsPresentBitSet) {
    Object menu = htMenus.get("elementsComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);
    if (elementsPresentBitSet == null)
      return;
    for (int i = elementsPresentBitSet.nextSetBit(0); i >= 0; i = elementsPresentBitSet
        .nextSetBit(i + 1)) {
      String elementName = Elements.elementNameFromNumber(i);
      String elementSymbol = Elements.elementSymbolFromNumber(i);
      String entryName = elementSymbol + " - " + elementName;
      menuCreateItem(menu, entryName, "SELECT " + elementName, null);
    }
    for (int i = Elements.firstIsotope; i < Elements.altElementMax; ++i) {
      int n = Elements.elementNumberMax + i;
      if (elementsPresentBitSet.get(n)) {
        n = Elements.altElementNumberFromIndex(i);
        String elementName = Elements.elementNameFromNumber(n);
        String elementSymbol = Elements.elementSymbolFromNumber(n);
        String entryName = elementSymbol + " - " + elementName;
        menuCreateItem(menu, entryName, "SELECT " + elementName, null);
      }
    }
    menuEnable(menu, true);
  }

  private void updateSpectraMenu() {
    Object menuh = htMenus.get("hnmrMenu");
    Object menuc = htMenus.get("cnmrMenu");
    if (menuh != null)
      menuRemoveAll(menuh, 0);
    if (menuc != null)
      menuRemoveAll(menuc, 0);
    Object menu = htMenus.get("spectraMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    // yes binary | not logical || here -- need to try to set both
    boolean isOK = setSpectraMenu(menuh, hnmrPeaks)
        | setSpectraMenu(menuc, cnmrPeaks);
    if (isOK) {
      if (menuh != null)
        menuAddSubMenu(menu, menuh);
      if (menuc != null)
        menuAddSubMenu(menu, menuc);
    }
    menuEnable(menu, isOK);
  }

  private boolean setSpectraMenu(Object menu, JmolList<String> peaks) {
    if (menu == null)
      return false;
    menuEnable(menu, false);
    int n = (peaks == null ? 0 : peaks.size());
    if (n == 0)
      return false;
    for (int i = 0; i < n; i++) {
      String peak = peaks.get(i);
      String title = Parser.getQuotedAttribute(peak, "title");
      String atoms = Parser.getQuotedAttribute(peak, "atoms");
      if (atoms != null)
        menuCreateItem(menu, title, "select visible & (@"
            + TextFormat.simpleReplace(atoms, ",", " or @") + ")", "Focus" + i);
    }
    menuEnable(menu, true);
    return true;
  }

  private void updateHeteroComputedMenu(Map<String, String> htHetero) {
    Object menu = htMenus.get("PDBheteroComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);
    if (htHetero == null)
      return;
    int n = 0;
    for (Map.Entry<String, String> hetero : htHetero.entrySet()) {
      String heteroCode = hetero.getKey();
      String heteroName = hetero.getValue();
      if (heteroName.length() > 20)
        heteroName = heteroName.substring(0, 20) + "...";
      String entryName = heteroCode + " - " + heteroName;
      menuCreateItem(menu, entryName, "SELECT [" + heteroCode + "]", null);
      n++;
    }
    menuEnable(menu, (n > 0));
  }

  @SuppressWarnings("unchecked")
  private void updateSurfMoComputedMenu(Map<String, Object> moData) {
    Object menu = htMenus.get("surfMoComputedMenuText");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    JmolList<Map<String, Object>> mos = (moData == null ? null
        : (JmolList<Map<String, Object>>) (moData.get("mos")));
    int nOrb = (mos == null ? 0 : mos.size());
    String text = getMenuText("surfMoComputedMenuText");
    if (nOrb == 0) {
      menuSetLabel(menu, GT._(text, ""));
      menuEnable(menu, false);
      return;
    }
    menuSetLabel(menu, GT._(text, nOrb));
    menuEnable(menu, true);
    Object subMenu = menu;
    int nmod = (nOrb % itemMax);
    if (nmod == 0)
      nmod = itemMax;
    int pt = (nOrb > itemMax ? 0 : Integer.MIN_VALUE);
    for (int i = nOrb; --i >= 0;) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        if (pt == nmod + 1)
          nmod = itemMax;
        String id = "mo" + pt + "Menu";
        subMenu = menuNewSubMenu(Math.max(i + 2 - nmod, 1) + "..." + (i + 1),
            menuGetId(menu) + "." + id);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      Map<String, Object> mo = mos.get(i);
      String entryName = "#"
          + (i + 1)
          + " "
          + (mo.containsKey("type") ? (String) mo.get("type") + " " : "")
          + (mo.containsKey("symmetry") ? (String) mo.get("symmetry") + " "
              : "") + (mo.containsKey("energy") ? mo.get("energy") : "");
      String script = "mo " + (i + 1);
      menuCreateItem(subMenu, entryName, script, null);
    }
  }

  private void updatePDBComputedMenus() {

    Object menu = htMenus.get("PDBaaResiduesComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);

    Object menu1 = htMenus.get("PDBnucleicResiduesComputedMenu");
    if (menu1 == null)
      return;
    menuRemoveAll(menu1, 0);
    menuEnable(menu1, false);

    Object menu2 = htMenus.get("PDBcarboResiduesComputedMenu");
    if (menu2 == null)
      return;
    menuRemoveAll(menu2, 0);
    menuEnable(menu2, false);
    if (modelSetInfo == null)
      return;
    int n = (modelIndex < 0 ? 0 : modelIndex + 1);
    String[] lists = ((String[]) modelSetInfo.get("group3Lists"));
    group3List = (lists == null ? null : lists[n]);
    group3Counts = (lists == null ? null : ((int[][]) modelSetInfo
        .get("group3Counts"))[n]);

    if (group3List == null)
      return;
    //next is correct as "<=" because it includes "UNK"
    int nItems = 0;
    for (int i = 1; i < JC.GROUPID_AMINO_MAX; ++i)
      nItems += updateGroup3List(menu, JC.predefinedGroup3Names[i]);
    nItems += augmentGroup3List(menu, "p>", true);
    menuEnable(menu, (nItems > 0));
    menuEnable(htMenus.get("PDBproteinMenu"), (nItems > 0));

    nItems = augmentGroup3List(menu1, "n>", false);
    menuEnable(menu1, nItems > 0);
    menuEnable(htMenus.get("PDBnucleicMenu"), (nItems > 0));

    nItems = augmentGroup3List(menu2, "c>", false);
    menuEnable(menu2, nItems > 0);
    menuEnable(htMenus.get("PDBcarboMenu"), (nItems > 0));
  }

  private int updateGroup3List(Object menu, String name) {
    int nItems = 0;
    int n = group3Counts[group3List.indexOf(name) / 6];
    String script = null;
    if (n > 0) {
      script = "SELECT " + name;
      name += "  (" + n + ")";
      nItems++;
    }
    Object item = menuCreateItem(menu, name, script, menuGetId(menu) + "."
        + name);
    if (n == 0)
      menuEnableItem(item, false);
    return nItems;
  }

  private int augmentGroup3List(Object menu, String type, boolean addSeparator) {
    int pt = JC.GROUPID_AMINO_MAX * 6 - 6;
    // ...... p>AFN]o>ODH]n>+T ]
    int nItems = 0;
    while (true) {
      pt = group3List.indexOf(type, pt);
      if (pt < 0)
        break;
      if (nItems++ == 0 && addSeparator)
        menuAddSeparator(menu);
      int n = group3Counts[pt / 6];
      String heteroCode = group3List.substring(pt + 2, pt + 5);
      String name = heteroCode + "  (" + n + ")";
      menuCreateItem(menu, name, "SELECT [" + heteroCode + "]", menuGetId(menu)
          + "." + name);
      pt++;
    }
    return nItems;
  }

  private void updateSYMMETRYComputedMenus() {
    updateSYMMETRYSelectComputedMenu();
    updateSYMMETRYShowComputedMenu();
  }

  @SuppressWarnings("unchecked")
  private void updateSYMMETRYShowComputedMenu() {
    Object menu = htMenus.get("SYMMETRYShowComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);
    if (!isSymmetry || modelIndex < 0)
      return;
    Map<String, Object> info = (Map<String, Object>) viewer.getProperty(
        "DATA_API", "spaceGroupInfo", null);
    if (info == null)
      return;
    Object[][] infolist = (Object[][]) info.get("operations");
    if (infolist == null)
      return;
    String name = (String) info.get("spaceGroupName");
    menuSetLabel(menu, name == null ? GT._("Space Group") : name);
    Object subMenu = menu;
    int nmod = itemMax;
    int pt = (infolist.length > itemMax ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < infolist.length; i++) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        String id = "drawsymop" + pt + "Menu";
        subMenu = menuNewSubMenu((i + 1) + "..."
            + Math.min(i + itemMax, infolist.length), menuGetId(menu) + "."
            + id);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String entryName = (i + 1) + " " + infolist[i][2] + " (" + infolist[i][0]
          + ")";
      menuEnableItem(menuCreateItem(subMenu, entryName,
          "draw SYMOP " + (i + 1), null), true);
    }
    menuEnable(menu, true);
  }

  private void updateSYMMETRYSelectComputedMenu() {
    Object menu = htMenus.get("SYMMETRYSelectComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuEnable(menu, false);
    if (!isSymmetry || modelIndex < 0)
      return;
    String[] list = (String[]) modelInfo.get("symmetryOperations");
    if (list == null)
      return;
    int[] cellRange = (int[]) modelInfo.get("unitCellRange");
    boolean haveUnitCellRange = (cellRange != null);
    Object subMenu = menu;
    int nmod = itemMax;
    int pt = (list.length > itemMax ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < list.length; i++) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        String id = "symop" + pt + "Menu";
        subMenu = menuNewSubMenu((i + 1) + "..."
            + Math.min(i + itemMax, list.length), menuGetId(menu) + "." + id);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String entryName = "symop=" + (i + 1) + " # " + list[i];
      menuEnableItem(menuCreateItem(subMenu, entryName, "SELECT symop="
          + (i + 1), null), haveUnitCellRange);
    }
    menuEnable(menu, true);
  }

  private void updateFRAMESbyModelComputedMenu() {
    //allowing this in case we move it later
    Object menu = htMenus.get("FRAMESbyModelComputedMenu");
    if (menu == null)
      return;
    menuEnable(menu, (modelCount > 0));
    menuSetLabel(menu, (modelIndex < 0 ? GT._(getMenuText("allModelsText"),
        modelCount) : getModelLabel()));
    menuRemoveAll(menu, 0);
    if (modelCount < 1)
      return;
    if (modelCount > 1)
      menuCreateCheckboxItem(menu, GT._("All"), "frame 0 ##", null,
          (modelIndex < 0), false);

    Object subMenu = menu;
    int nmod = itemMax;
    int pt = (modelCount > itemMax ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < modelCount; i++) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        String id = "model" + pt + "Menu";
        subMenu = menuNewSubMenu((i + 1) + "..."
            + Math.min(i + itemMax, modelCount), menuGetId(menu) + "." + id);
        menuAddSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String script = "" + viewer.getModelNumberDotted(i);
      String entryName = viewer.getModelName(i);
      String spectrumTypes = (String) viewer.getModelAuxiliaryInfoValue(i,
          "spectrumTypes");
      if (spectrumTypes != null && entryName.startsWith(spectrumTypes))
        spectrumTypes = null;
      if (!entryName.equals(script)) {
        int ipt = entryName.indexOf(";PATH");
        if (ipt >= 0)
          entryName = entryName.substring(0, ipt);
        if (entryName.indexOf("Model[") == 0
            && (ipt = entryName.indexOf("]:")) >= 0)
          entryName = entryName.substring(ipt + 2);
        entryName = script + ": " + entryName;
      }
      if (entryName.length() > 60)
        entryName = entryName.substring(0, 55) + "...";
      if (spectrumTypes != null)
        entryName += " (" + spectrumTypes + ")";
      menuCreateCheckboxItem(subMenu, entryName, "model " + script + " ##",
          null, (modelIndex == i), false);
    }
  }

  private void updateConfigurationComputedMenu() {
    Object menu = htMenus.get("configurationComputedMenu");
    if (menu == null)
      return;
    menuEnable(menu, isMultiConfiguration);
    if (!isMultiConfiguration)
      return;
    int nAltLocs = altlocs.length();
    menuSetLabel(menu, GT._(getMenuText("configurationMenuText"), nAltLocs));
    menuRemoveAll(menu, 0);
    String script = "hide none ##CONFIG";
    menuCreateCheckboxItem(menu, GT._("All"), script, null,
        (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)),
        false);
    for (int i = 0; i < nAltLocs; i++) {
      script = "configuration " + (i + 1)
          + "; hide thisModel and not selected ##CONFIG";
      String entryName = "" + (i + 1) + " -- \"" + altlocs.charAt(i) + "\"";
      menuCreateCheckboxItem(
          menu,
          entryName,
          script,
          null,
          (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)),
          false);
    }
  }

  @SuppressWarnings("unchecked")
  private void updateModelSetComputedMenu() {
    Object menu = htMenus.get("modelSetMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    menuSetLabel(menu, nullModelSetName);
    menuEnable(menu, false);
    menuEnable(htMenus.get("surfaceMenu"), !isZapped);
    menuEnable(htMenus.get("measureMenu"), !isZapped);
    menuEnable(htMenus.get("pickingMenu"), !isZapped);
    menuEnable(htMenus.get("computationMenu"), !isZapped);
    if (modelSetName == null || isZapped)
      return;
    if (isMultiFrame) {
      modelSetName = GT._(getMenuText("modelSetCollectionText"), modelCount);
      if (modelSetName.length() > titleWidthMax)
        modelSetName = modelSetName.substring(0, titleWidthMax) + "...";
    } else if (viewer.getBooleanProperty("hideNameInPopup")) {
      modelSetName = getMenuText("hiddenModelSetText");
    } else if (modelSetName.length() > titleWidthMax) {
      modelSetName = modelSetName.substring(0, titleWidthMax) + "...";
    }
    menuSetLabel(menu, modelSetName);
    menuEnable(menu, true);
    // 100 here is totally arbitrary. You can do a minimization on any number of atoms
    menuEnable(htMenus.get("computationMenu"), atomCount <= 100);
    addMenuItem(menu, GT._(getMenuText("atomsText"), atomCount));
    addMenuItem(menu, GT._(getMenuText("bondsText"), viewer
        .getBondCountInModel(modelIndex)));
    if (isPDB) {
      menuAddSeparator(menu);
      addMenuItem(menu, GT._(getMenuText("groupsText"), viewer
          .getGroupCountInModel(modelIndex)));
      addMenuItem(menu, GT._(getMenuText("chainsText"), viewer
          .getChainCountInModel(modelIndex)));
      addMenuItem(menu, GT._(getMenuText("polymersText"), viewer
          .getPolymerCountInModel(modelIndex)));
      Object submenu = htMenus.get("BiomoleculesMenu");
      if (submenu == null) {
        submenu = menuNewSubMenu(GT._(getMenuText("biomoleculesMenuText")),
            menuGetId(menu) + ".biomolecules");
        menuAddSubMenu(menu, submenu);
      }
      menuRemoveAll(submenu, 0);
      menuEnable(submenu, false);
      JmolList<Map<String, Object>> biomolecules;
      if (modelIndex >= 0
          && (biomolecules = (JmolList<Map<String, Object>>) viewer
              .getModelAuxiliaryInfoValue(modelIndex, "biomolecules")) != null) {
        menuEnable(submenu, true);
        int nBiomolecules = biomolecules.size();
        for (int i = 0; i < nBiomolecules; i++) {
          String script = (isMultiFrame ? ""
              : "save orientation;load \"\" FILTER \"biomolecule " + (i + 1)
                  + "\";restore orientation;");
          int nAtoms = ((Integer) biomolecules.get(i).get("atomCount"))
              .intValue();
          String entryName = GT._(getMenuText(isMultiFrame ? "biomoleculeText"
              : "loadBiomoleculeText"), new Object[] { Integer.valueOf(i + 1),
              Integer.valueOf(nAtoms) });
          menuCreateItem(submenu, entryName, script, null);
        }
      }
    }
    if (isApplet && viewer.showModelSetDownload()
        && !viewer.getBooleanProperty("hideNameInPopup")) {
      menuAddSeparator(menu);
      menuCreateItem(menu, GT._(getMenuText("viewMenuText"), modelSetFileName),
          "show url", null);
    }
  }

  private String getModelLabel() {
    return GT._(getMenuText("modelMenuText"), (modelIndex + 1) + "/"
        + modelCount);
  }

  private void updateAboutSubmenu() {
    Object menu = htMenus.get("aboutComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, aboutComputedMenuBaseCount);
    Object subMenu = menuNewSubMenu("About molecule", "modelSetMenu");
    // No need to localize this, as it will be overwritten with the model's name      
    menuAddSubMenu(menu, subMenu);
    htMenus.put("modelSetMenu", subMenu);
    updateModelSetComputedMenu();

    subMenu = menuNewSubMenu("Jmol " + JC.version
        + (isSigned ? " (signed)" : ""), "aboutJmolMenu");
    menuAddSubMenu(menu, subMenu);
    htMenus.put("aboutJmolMenu", subMenu);
    addMenuItem(subMenu, JC.date);
    menuCreateItem(subMenu, "http://www.jmol.org",
        "show url \"http://www.jmol.org\"", null);
    menuCreateItem(subMenu, GT._("Mouse Manual"),
        "show url \"http://wiki.jmol.org/index.php/Mouse_Manual\"", null);
    menuCreateItem(subMenu, GT._("Translations"),
        "show url \"http://wiki.jmol.org/index.php/Internationalisation\"",
        null);

    subMenu = menuNewSubMenu(GT._("System"), "systemMenu");
    menuAddSubMenu(menu, subMenu);
    htMenus.put("systemMenu", subMenu);
    addMenuItem(subMenu, viewer.getOperatingSystemName());
    menuAddSeparator(subMenu);
    addMenuItem(subMenu, GT._("Java version:"));
    addMenuItem(subMenu, viewer.getJavaVendor());
    addMenuItem(subMenu, viewer.getJavaVersion());
    Runtime runtime = null;
    /**
     * @j2sNative
     *   
     */
    {
      runtime = Runtime.getRuntime();
    }
    if (runtime != null) {
      int availableProcessors = runtime.availableProcessors();
      if (availableProcessors > 0)
        addMenuItem(subMenu, (availableProcessors == 1) ? GT._("1 processor")
            : GT._("{0} processors", availableProcessors));
      else
        addMenuItem(subMenu, GT._("unknown processor count"));
      addMenuItem(subMenu, GT._("Java memory usage:"));
      //runtime.gc();
      long mbTotal = convertToMegabytes(runtime.totalMemory());
      long mbFree = convertToMegabytes(runtime.freeMemory());
      long mbMax = convertToMegabytes(runtime.maxMemory());
      addMenuItem(subMenu, GT._("{0} MB total",
          new Object[] { new Long(mbTotal) }));
      addMenuItem(subMenu, GT._("{0} MB free",
          new Object[] { new Long(mbFree) }));
      if (mbMax > 0)
        addMenuItem(subMenu, GT._("{0} MB maximum", new Object[] { new Long(
            mbMax) }));
      else
        addMenuItem(subMenu, GT._("unknown maximum"));
    }
  }

  private void updateLanguageSubmenu() {
    Object menu = htMenus.get("languageComputedMenu");
    if (menu == null)
      return;
    menuRemoveAll(menu, 0);
    String language = GT.getLanguage();
    String id = menuGetId(menu);
    Language[] languages = GT.getLanguageList(null);
    for (int i = 0; i < languages.length; i++) {
      if (language.equals(languages[i].code))
        languages[i].display = true;
      if (languages[i].display) {
        String code = languages[i].code;
        String name = languages[i].language;
        String nativeName = languages[i].nativeLanguage;
        String menuLabel = code + " - " + GT._(name);
        if ((nativeName != null) && (!nativeName.equals(GT._(name)))) {
          menuLabel += " - " + nativeName;
        }
        menuCreateCheckboxItem(menu, menuLabel, "language = \"" + code
            + "\" ##" + name, id + "." + code, language.equals(code), false);
      }
    }
  }

  private long convertToMegabytes(long num) {
    if (num <= Long.MAX_VALUE - 512 * 1024)
      num += 512 * 1024;
    return num / (1024 * 1024);
  }

  private void updateForShow() {
    if (updateMode == UPDATE_NEVER)
      return;
    getViewerData();
    updateMode = UPDATE_SHOW;
    updateSelectMenu();
    updateSpectraMenu();
    updateFRAMESbyModelComputedMenu();
    updateModelSetComputedMenu();
    updateAboutSubmenu();
  }

  private void setFrankMenu(String id) {
    if (currentFrankId != null && currentFrankId == id && nFrankList > 0)
      return;
    if (frankPopup == null)
      frankPopup = menuCreatePopup("Frank");
    thisPopup = frankPopup;
    menuRemoveAll(frankPopup, 0);
    if (id == null)
      return;
    currentFrankId = id;
    nFrankList = 0;
    frankList[nFrankList++] = new Object[] { null, null, null };
    menuCreateItem(frankPopup, getMenuText("mainMenuText"), "MAIN", "");
    for (int i = id.indexOf(".", 2) + 1;;) {
      int iNew = id.indexOf(".", i);
      if (iNew < 0)
        break;
      String strMenu = id.substring(i, iNew);
      Object menu = htMenus.get(strMenu);
      frankList[nFrankList++] = new Object[] { menuGetParent(menu), menu,
          Integer.valueOf(menuGetPosition(menu)) };
      menuAddSubMenu(frankPopup, menu);
      i = iNew + 1;
    }
    thisPopup = popupMenu;
  }

  private void show(int x, int y, boolean doPopup) {
    thisx = x;
    thisy = y;
    updateForShow();
    for (Map.Entry<String, Object> entry : htCheckbox.entrySet()) {
      String key = entry.getKey();
      Object item = entry.getValue();
      String basename = key.substring(0, key.indexOf(":"));
      boolean b = viewer.getBooleanProperty(basename);
      menuSetCheckBoxState(item, b);
    }
    if (doPopup)
      menuShowPopup(popupMenu, thisx, thisy);
  }


}
