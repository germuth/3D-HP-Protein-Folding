/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import org.jmol.util.JmolList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import java.util.Map;
import java.util.Properties;

import org.jmol.api.JmolPropertyManager;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Chain;
import org.jmol.modelset.Group;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.script.SV;
import org.jmol.script.ScriptVariableInt;
import org.jmol.script.T;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.JmolEdge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Parser;
import org.jmol.util.P3;
import org.jmol.util.Quaternion;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;

/**
 * 
 * The PropertyManager handles all operations relating to delivery of properties
 * with the getProperty() method, or its specifically cast forms
 * getPropertyString() or getPropertyJSON().
 * 
 * It is instantiated by reflection
 * 
 */

public class PropertyManager implements JmolPropertyManager {

  public PropertyManager() {
    // required for reflection
  }

  Viewer viewer;
  private Map<String, Integer> map = new Hashtable<String, Integer>();

  public void setViewer(Viewer viewer) {
    this.viewer = viewer;
    for (int i = 0, p = 0; i < propertyTypes.length; i += 3)
      map.put(propertyTypes[i].toLowerCase(), Integer.valueOf(p++));
  }

  public int getPropertyNumber(String infoType) {
    Integer n = map.get(infoType == null ? "" : infoType.toLowerCase());
    return (n == null ? -1 : n.intValue());
  }

  public String getDefaultPropertyParam(int propID) {
    return (propID < 0 ? "" : propertyTypes[propID * 3 + 2]);
  }

  public boolean checkPropertyParameter(String name) {
    int propID = getPropertyNumber(name);
    String type = getParamType(propID);
    return (type.length() > 0 && type != atomExpression);
  }

  private final static String atomExpression = "<atom selection>";

  private final static String[] propertyTypes = {
    "appletInfo"      , "", "",
    "fileName"        , "", "",
    "fileHeader"      , "", "",
    "fileContents"    , "<pathname>", "",
    "fileContents"    , "", "",
    "animationInfo"   , "", "",
    "modelInfo"       , atomExpression, "{*}",
    //"X -vibrationInfo", "", "",  //not implemented -- see modelInfo
    "ligandInfo"      , atomExpression, "{*}",
    "shapeInfo"       , "", "",
    "measurementInfo" , "", "",
    
    "centerInfo"      , "", "",
    "orientationInfo" , "", "",
    "transformInfo"   , "", "",
    "atomList"        , atomExpression, "(visible)",
    "atomInfo"        , atomExpression, "(visible)",
    
    "bondInfo"        , atomExpression, "(visible)",
    "chainInfo"       , atomExpression, "(visible)",
    "polymerInfo"     , atomExpression, "(visible)",
    "moleculeInfo"    , atomExpression, "(visible)",
    "stateInfo"       , "<state type>", "all",
    
    "extractModel"    , atomExpression, "(visible)",
    "jmolStatus"      , "statusNameList", "",
    "jmolViewer"      , "", "",
    "messageQueue"    , "", "",
    "auxiliaryInfo"   , atomExpression, "{*}",
    
    "boundBoxInfo"    , "", "",  
    "dataInfo"        , "<data type>", "types",
    "image"           , "", "",
    "evaluate"        , "<expression>", "",
    "menu"            , "<type>", "current",
    "minimizationInfo", "", "",
    "pointGroupInfo"  , atomExpression, "(visible)",
    "fileInfo"        , "<type>", "",
    "errorMessage"    , "", "",
    "mouseInfo"       , "", "",
    "isosurfaceInfo"  , "", "",
    "isosurfaceData"  , "", "",
    "consoleText"     , "", "",
    "jspecView"       , "<key>", "",
  };

  private final static int PROP_APPLET_INFO = 0;
  private final static int PROP_FILENAME = 1;
  private final static int PROP_FILEHEADER = 2;
  private final static int PROP_FILECONTENTS_PATH = 3;
  private final static int PROP_FILECONTENTS = 4;

  private final static int PROP_ANIMATION_INFO = 5;
  private final static int PROP_MODEL_INFO = 6;
  //private final static int PROP_VIBRATION_INFO = 7; //not implemented -- see auxiliaryInfo
  private final static int PROP_LIGAND_INFO = 7;
  private final static int PROP_SHAPE_INFO = 8;
  private final static int PROP_MEASUREMENT_INFO = 9;

  private final static int PROP_CENTER_INFO = 10;
  private final static int PROP_ORIENTATION_INFO = 11;
  private final static int PROP_TRANSFORM_INFO = 12;
  private final static int PROP_ATOM_LIST = 13;
  private final static int PROP_ATOM_INFO = 14;

  private final static int PROP_BOND_INFO = 15;
  private final static int PROP_CHAIN_INFO = 16;
  private final static int PROP_POLYMER_INFO = 17;
  private final static int PROP_MOLECULE_INFO = 18;
  private final static int PROP_STATE_INFO = 19;

  private final static int PROP_EXTRACT_MODEL = 20;
  private final static int PROP_JMOL_STATUS = 21;
  private final static int PROP_JMOL_VIEWER = 22;
  private final static int PROP_MESSAGE_QUEUE = 23;
  private final static int PROP_AUXILIARY_INFO = 24;

  private final static int PROP_BOUNDBOX_INFO = 25;
  private final static int PROP_DATA_INFO = 26;
  private final static int PROP_IMAGE = 27;
  private final static int PROP_EVALUATE = 28;
  private final static int PROP_MENU = 29;
  private final static int PROP_MINIMIZATION_INFO = 30;
  private final static int PROP_POINTGROUP_INFO = 31;
  private final static int PROP_FILE_INFO = 32;
  private final static int PROP_ERROR_MESSAGE = 33;
  private final static int PROP_MOUSE_INFO = 34;
  private final static int PROP_ISOSURFACE_INFO = 35;
  private final static int PROP_ISOSURFACE_DATA = 36;
  private final static int PROP_CONSOLE_TEXT = 37;
  private final static int PROP_JSPECVIEW = 38;
  private final static int PROP_COUNT = 39;

  //// static methods used by Eval and Viewer ////

  public Object getProperty(String returnType, String infoType, Object paramInfo) {
    if (propertyTypes.length != PROP_COUNT * 3)
      Logger.warn("propertyTypes is not the right length: "
          + propertyTypes.length + " != " + PROP_COUNT * 3);
    Object info;
    if (infoType.indexOf(".") >= 0 || infoType.indexOf("[") >= 0) {
      info = getModelProperty(infoType, paramInfo);
    } else {
      info = getPropertyAsObject(infoType, paramInfo, returnType);
    }
    if (returnType == null)
      return info;
    boolean requestedReadable = returnType.equalsIgnoreCase("readable");
    if (requestedReadable)
      returnType = (isReadableAsString(infoType) ? "String" : "JSON");
    if (returnType.equalsIgnoreCase("String"))
      return (info == null ? "" : info.toString());
    if (requestedReadable)
      return Escape.toReadable(infoType, info);
    else if (returnType.equalsIgnoreCase("JSON"))
      return "{" + Escape.toJSON(infoType, info) + "}";
    return info;
  }

  private Object getModelProperty(String propertyName, Object propertyValue) {
    propertyName = propertyName.replace(']', ' ').replace('[', ' ').replace(
        '.', ' ');
    propertyName = TextFormat.simpleReplace(propertyName, "  ", " ");
    String[] names = TextFormat.splitChars(TextFormat.trim(propertyName, " "),
        " ");
    SV[] args = new SV[names.length];
    propertyName = names[0];
    int n;
    for (int i = 1; i < names.length; i++) {
      if ((n = Parser.parseInt(names[i])) != Integer.MIN_VALUE)
        args[i] = new ScriptVariableInt(n);
      else
        args[i] = SV.newVariable(T.string, names[i]);
    }
    return extractProperty(getProperty(null, propertyName, propertyValue),
        args, 1);
  }

  @SuppressWarnings("unchecked")
  public Object extractProperty(Object property, SV[] args, int ptr) {
    if (ptr >= args.length)
      return property;
    int pt;
    SV arg = args[ptr++];
    switch (arg.tok) {
    case T.integer:
      pt = arg.asInt() - 1; //one-based, as for array selectors
      if (property instanceof JmolList<?>) {
        JmolList<Object> v = (JmolList<Object>) property;
        if (pt < 0)
          pt += v.size();
        if (pt >= 0 && pt < v.size())
          return extractProperty(v.get(pt), args, ptr);
        return "";
      }
      if (property instanceof Matrix3f) {
        Matrix3f m = (Matrix3f) property;
        float[][] f = new float[][] { new float[] { m.m00, m.m01, m.m02 },
            new float[] { m.m10, m.m11, m.m12 },
            new float[] { m.m20, m.m21, m.m22 } };
        if (pt < 0)
          pt += 3;
        if (pt >= 0 && pt < 3)
          return extractProperty(f, args, --ptr);
        return "";
      }

      if (Escape.isAI(property)) {
        int[] ilist = (int[]) property;
        if (pt < 0)
          pt += ilist.length;
        if (pt >= 0 && pt < ilist.length)
          return Integer.valueOf(ilist[pt]);
        return "";
      }
      if (Escape.isAF(property)) {
        float[] flist = (float[]) property;
        if (pt < 0)
          pt += flist.length;
        if (pt >= 0 && pt < flist.length)
          return Float.valueOf(flist[pt]);
        return "";
      }
      if (Escape.isAII(property)) {
        int[][] iilist = (int[][]) property;
        if (pt < 0)
          pt += iilist.length;
        if (pt >= 0 && pt < iilist.length)
          return extractProperty(iilist[pt], args, ptr);
        return "";
      }
      if (Escape.isAFF(property)) {
        float[][] fflist = (float[][]) property;
        if (pt < 0)
          pt += fflist.length;
        if (pt >= 0 && pt < fflist.length)
          return extractProperty(fflist[pt], args, ptr);
        return "";
      }
      if (Escape.isAS(property)) {
        String[] slist = (String[]) property;
        if (pt < 0)
          pt += slist.length;
        if (pt >= 0 && pt < slist.length)
          return slist[pt];
        return "";
      }
      if (property instanceof Object[]) {
        Object[] olist = (Object[]) property;
        if (pt < 0)
          pt += olist.length;
        if (pt >= 0 && pt < olist.length)
          return olist[pt];
        return "";
      }
      break;

    case T.string:
      String key = arg.asString();
      if (property instanceof Map<?, ?>) {
        Map<String, Object> h = (Map<String, Object>) property;
        if (key.equalsIgnoreCase("keys")) {
          JmolList<Object> keys = new  JmolList<Object>();
          Iterator<String> e = h.keySet().iterator();
          while (e.hasNext())
            keys.addLast(e.next());
          return extractProperty(keys, args, ptr);
        }
        if (!h.containsKey(key)) {
          Iterator<String> e = h.keySet().iterator();
          String newKey = "";
          while (e.hasNext())
            if ((newKey = e.next()).equalsIgnoreCase(key)) {
              key = newKey;
              break;
            }
        }
        if (h.containsKey(key))
          return extractProperty(h.get(key), args, ptr);
        return "";
      }
      if (property instanceof JmolList<?>) {
        // drill down into vectors for this key
        JmolList<Object> v = (JmolList<Object>) property;
        JmolList<Object> v2 = new  JmolList<Object>();
        ptr--;
        for (pt = 0; pt < v.size(); pt++) {
          Object o = v.get(pt);
          if (o instanceof Map<?, ?>)
            v2.addLast(extractProperty(o, args, ptr));
        }
        return v2;
      }
      break;
    }
    return property;
  }

  //// private static methods ////

  private static String getPropertyName(int propID) {
    return (propID < 0 ? "" : propertyTypes[propID * 3]);
  }

  private static String getParamType(int propID) {
    return (propID < 0 ? "" : propertyTypes[propID * 3 + 1]);
  }

  private final static String[] readableTypes = { "", "stateinfo",
      "extractmodel", "filecontents", "fileheader", "image", "menu",
      "minimizationInfo" };

  private static boolean isReadableAsString(String infoType) {
    for (int i = readableTypes.length; --i >= 0;)
      if (infoType.equalsIgnoreCase(readableTypes[i]))
        return true;
    return false;
  }

  private Object getPropertyAsObject(String infoType, Object paramInfo,
                                     String returnType) {
    //Logger.debug("getPropertyAsObject(\"" + infoType+"\", \"" + paramInfo + "\")");
    if (infoType.equals("tokenList")) {
      return T.getTokensLike((String) paramInfo);
    }
    int id = getPropertyNumber(infoType);
    boolean iHaveParameter = (paramInfo != null && paramInfo.toString()
        .length() > 0);
    Object myParam = (iHaveParameter ? paramInfo : getDefaultPropertyParam(id));
    //myParam may now be a bitset
    switch (id) {
    case PROP_APPLET_INFO:
      return viewer.getAppletInfo();
    case PROP_ANIMATION_INFO:
      return viewer.getAnimationInfo();
    case PROP_ATOM_LIST:
      return viewer.getAtomBitSetVector(myParam);
    case PROP_ATOM_INFO:
      return viewer.getAllAtomInfo(myParam);
    case PROP_AUXILIARY_INFO:
      return viewer.getAuxiliaryInfo(myParam);
    case PROP_BOND_INFO:
      return viewer.getAllBondInfo(myParam);
    case PROP_BOUNDBOX_INFO:
      return viewer.getBoundBoxInfo();
    case PROP_CENTER_INFO:
      return viewer.getRotationCenter();
    case PROP_CHAIN_INFO:
      return viewer.getAllChainInfo(myParam);
    case PROP_CONSOLE_TEXT:
      return viewer.getProperty("DATA_API", "consoleText", null);
    case PROP_JSPECVIEW:
      return viewer.getJspecViewProperties(myParam);
    case PROP_DATA_INFO:
      return viewer.getData(myParam.toString());
    case PROP_ERROR_MESSAGE:
      return viewer.getErrorMessageUn();
    case PROP_EVALUATE:
      return viewer.evaluateExpression(myParam.toString());
    case PROP_EXTRACT_MODEL:
      return viewer.getModelExtract(myParam, true, false, "MOL");
    case PROP_FILE_INFO:
      return getFileInfo(viewer.getFileData(), myParam.toString());
    case PROP_FILENAME:
      return viewer.getFullPathName();
    case PROP_FILEHEADER:
      return viewer.getFileHeader();
    case PROP_FILECONTENTS:
    case PROP_FILECONTENTS_PATH:
      if (iHaveParameter)
        return viewer.getFileAsString(myParam.toString());
      return viewer.getCurrentFileAsString();
    case PROP_IMAGE:
      String params = myParam.toString();
      int height = -1,
      width = -1;
      int pt;
      if ((pt = params.indexOf("height=")) >= 0)
        height = Parser.parseInt(params.substring(pt + 7));
      if ((pt = params.indexOf("width=")) >= 0)
        width = Parser.parseInt(params.substring(pt + 6));
      if (width < 0 && height < 0)
        height = width = -1;
      else if (width < 0)
        width = height;
      else
        height = width;
      return viewer.getImageAs(returnType == null ? "JPEG" : "JPG64", -1,
          width, height, null, null);
    case PROP_ISOSURFACE_INFO:
      return viewer.getShapeProperty(JC.SHAPE_ISOSURFACE, "getInfo");
    case PROP_ISOSURFACE_DATA:
      return viewer.getShapeProperty(JC.SHAPE_ISOSURFACE, "getData");
    case PROP_JMOL_STATUS:
      return viewer.getStatusChanged(myParam.toString());
    case PROP_JMOL_VIEWER:
      return viewer;
    case PROP_LIGAND_INFO:
      return viewer.getLigandInfo(myParam);
    case PROP_MEASUREMENT_INFO:
      return viewer.getMeasurementInfo();
    case PROP_MENU:
      return viewer.getMenu(myParam.toString());
    case PROP_MESSAGE_QUEUE:
      return viewer.getMessageQueue();
    case PROP_MINIMIZATION_INFO:
      return viewer.getMinimizationInfo();
    case PROP_MODEL_INFO:
      return viewer.getModelInfo(myParam);
    case PROP_MOLECULE_INFO:
      return viewer.getMoleculeInfo(myParam);
    case PROP_MOUSE_INFO:
      return viewer.getMouseInfo();
    case PROP_ORIENTATION_INFO:
      return viewer.getOrientationInfo();
    case PROP_POINTGROUP_INFO:
      return viewer.getPointGroupInfo(myParam);
    case PROP_POLYMER_INFO:
      return viewer.getAllPolymerInfo(myParam);
    case PROP_SHAPE_INFO:
      return viewer.getShapeInfo();
    case PROP_STATE_INFO:
      return viewer.getStateInfo3(myParam.toString(), 0, 0);
    case PROP_TRANSFORM_INFO:
      return viewer.getMatrixRotate();
    }
    String[] data = new String[PROP_COUNT];
    for (int i = 0; i < PROP_COUNT; i++) {
      String paramType = getParamType(i);
      String paramDefault = getDefaultPropertyParam(i);
      String name = getPropertyName(i);
      data[i] = (name.charAt(0) == 'X' ? "" : name
          + (paramType != "" ? " "
              + getParamType(i)
              + (paramDefault != "" ? " #default: "
                  + getDefaultPropertyParam(i) : "") : ""));
    }
    Arrays.sort(data);
    SB info = new SB();
    info.append("getProperty ERROR\n").append(infoType).append(
        "?\nOptions include:\n");
    for (int i = 0; i < PROP_COUNT; i++)
      if (data[i].length() > 0)
        info.append("\n getProperty ").append(data[i]);
    return info.toString();
  }

  @SuppressWarnings("unchecked")
  static Object getFileInfo(Object objHeader, String type) {
    Map<String, String> ht = new Hashtable<String, String>();
    if (objHeader == null)
      return ht;
    boolean haveType = (type != null && type.length() > 0);
    if (objHeader instanceof Map) {
      return (haveType ? ((Map<?, ?>) objHeader).get(type) : objHeader);
    }
    String[] lines = TextFormat.split((String) objHeader, '\n');
    String keyLast = "";
    SB sb = new SB();
    if (haveType)
      type = type.toUpperCase();
    String key = "";
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.length() < 12)
        continue;
      key = line.substring(0, 6).trim();
      String cont = line.substring(7, 10).trim();
      if (key.equals("REMARK")) {
        key += cont;
      }
      if (!key.equals(keyLast)) {
        if (haveType && keyLast.equals(type))
          return sb.toString();
        if (!haveType) {
          ht.put(keyLast, sb.toString());
          sb = new SB();
        }
        keyLast = key;
      }
      if (!haveType || key.equals(type))
        sb.append(line.substring(10).trim()).appendC('\n');
    }
    if (!haveType) {
      ht.put(keyLast, sb.toString());
    }
    if (haveType)
      return (key.equals(type) ? sb.toString() : "");
    return ht;
  }

  /// info ///

  public JmolList<Map<String, Object>> getMoleculeInfo(ModelSet modelSet,
                                                   Object atomExpression) {
    BS bsAtoms = viewer.getAtomBitSet(atomExpression);
    if (modelSet.moleculeCount == 0) {
      modelSet.getMolecules();
    }
    JmolList<Map<String, Object>> V = new  JmolList<Map<String, Object>>();
    BS bsTemp = new BS();
    for (int i = 0; i < modelSet.moleculeCount; i++) {
      bsTemp = BSUtil.copy(bsAtoms);
      JmolMolecule m = modelSet.molecules[i];
      bsTemp.and(m.atomList);
      if (bsTemp.length() > 0) {
        Map<String, Object> info = new Hashtable<String, Object>();
        info.put("mf", m.getMolecularFormula(false)); // sets atomCount and nElements
        info.put("number", Integer.valueOf(m.moleculeIndex + 1)); //for now
        info.put("modelNumber", modelSet.getModelNumberDotted(m.modelIndex));
        info.put("numberInModel", Integer.valueOf(m.indexInModel + 1));
        info.put("nAtoms", Integer.valueOf(m.atomCount));
        info.put("nElements", Integer.valueOf(m.nElements));
        V.addLast(info);
      }
    }
    return V;
  }

  public Map<String, Object> getModelInfo(Object atomExpression) {

    BS bsModels = viewer.getModelBitSet(viewer
        .getAtomBitSet(atomExpression), false);

    ModelSet m = viewer.getModelSet();
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("modelSetName", m.modelSetName);
    info.put("modelCount", Integer.valueOf(m.modelCount));
    info.put("isTainted", Boolean.valueOf(m.tainted != null));
    info.put("canSkipLoad", Boolean.valueOf(m.canSkipLoad));
    info.put("modelSetHasVibrationVectors", Boolean.valueOf(m
        .modelSetHasVibrationVectors()));
    if (m.modelSetProperties != null) {
      info.put("modelSetProperties", m.modelSetProperties);
    }
    info.put("modelCountSelected", Integer.valueOf(BSUtil
        .cardinalityOf(bsModels)));
    info.put("modelsSelected", bsModels);
    JmolList<Map<String, Object>> vModels = new  JmolList<Map<String, Object>>();
    m.getMolecules();

    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) {
      Map<String, Object> model = new Hashtable<String, Object>();
      model.put("_ipt", Integer.valueOf(i));
      model.put("num", Integer.valueOf(m.getModelNumber(i)));
      model.put("file_model", m.getModelNumberDotted(i));
      model.put("name", m.getModelName(i));
      String s = m.getModelTitle(i);
      if (s != null)
        model.put("title", s);
      s = m.getModelFileName(i);
      if (s != null)
        model.put("file", s);
      s = (String) m.getModelAuxiliaryInfoValue(i, "modelID");
      if (s != null)
        model.put("id", s);
      model.put("vibrationVectors", Boolean.valueOf(m
          .modelHasVibrationVectors(i)));
      Model mi = m.models[i];
      model.put("atomCount", Integer.valueOf(mi.atomCount));
      model.put("bondCount", Integer.valueOf(mi.getBondCount()));
      model.put("groupCount", Integer.valueOf(mi.getGroupCount()));
      model.put("moleculeCount", Integer.valueOf(mi.moleculeCount));
      model.put("polymerCount", Integer.valueOf(mi.getBioPolymerCount()));
      model.put("chainCount", Integer.valueOf(m.getChainCountInModel(i, true)));
      if (mi.properties != null) {
        model.put("modelProperties", mi.properties);
      }
      Float energy = (Float) m.getModelAuxiliaryInfoValue(i, "Energy");
      if (energy != null) {
        model.put("energy", energy);
      }
      model.put("atomCount", Integer.valueOf(mi.atomCount));
      vModels.addLast(model);
    }
    info.put("models", vModels);
    return info;
  }

  public Map<String, Object> getLigandInfo(Object atomExpression) {
    BS bsAtoms = viewer.getAtomBitSet(atomExpression);
    BS bsSolvent = viewer.getAtomBitSet("solvent");
    Map<String, Object> info = new Hashtable<String, Object>();
    JmolList<Map<String, Object>> ligands = new  JmolList<Map<String, Object>>();
    info.put("ligands", ligands);
    ModelSet ms = viewer.modelSet;
    BS bsExclude = BSUtil.copyInvert(bsAtoms, ms.atomCount);
    bsExclude.or(bsSolvent);
    Atom[] atoms = ms.atoms;
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      if (atoms[i].isProtein() || atoms[i].isNucleic())
        bsExclude.set(i);
    BS[] bsModelAtoms = new BS[ms.modelCount];
    for (int i = ms.modelCount; --i >= 0;) {
      bsModelAtoms[i] = viewer.getModelUndeletedAtomsBitSet(i);
      bsModelAtoms[i].andNot(bsExclude);
    }
    JmolMolecule[] molList = JmolMolecule.getMolecules(atoms, bsModelAtoms,
        null, bsExclude);
    for (int i = 0; i < molList.length; i++) {
      BS bs = molList[i].atomList;
      Map<String, Object> ligand = new Hashtable<String, Object>();
      ligands.addLast(ligand);
      ligand.put("atoms", Escape.e(bs));
      String names = "";
      String sep = "";
      Group lastGroup = null;
      char chainlast = '\0';
      String reslist = "";
      String model = "";
      int resnolast = Integer.MAX_VALUE;
      int resnofirst = Integer.MAX_VALUE;
      for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
        Atom atom = atoms[j];
        if (lastGroup == atom.group)
          continue;
        lastGroup = atom.group;
        int resno = atom.getResno();
        char chain = atom.getChainID();
        if (resnolast != resno - 1) {
          if (reslist.length() != 0 && resnolast != resnofirst)
            reslist += "-" + resnolast;
          chain = '\1';
          resnofirst = resno;
        }
        model = "/" + ms.getModelNumberDotted(atom.modelIndex);
        if (chainlast != '\0' && chain != chainlast)
          reslist += ":" + chainlast + model;
        if (chain == '\1')
          reslist += " " + resno;
        resnolast = resno;
        chainlast = atom.getChainID();
        names += sep + atom.getGroup3(false);
        sep = "-";
      }
      reslist += (resnofirst == resnolast ? "" : "-" + resnolast)
          + (chainlast == '\0' ? "" : ":" + chainlast) + model;
      ligand.put("groupNames", names);
      ligand.put("residueList", reslist.substring(1));
    }
    return info;
  }

  public Object getSymmetryInfo(BS bsAtoms, String xyz, int op, P3 pt,
                                P3 pt2, String id, int type) {
    int iModel = -1;
    if (bsAtoms == null) {
      iModel = viewer.getCurrentModelIndex();
      if (iModel < 0)
        return "";
      bsAtoms = viewer.getModelUndeletedAtomsBitSet(iModel);
    }
    int iAtom = bsAtoms.nextSetBit(0);
    if (iAtom < 0)
      return "";
    iModel = viewer.modelSet.atoms[iAtom].modelIndex;
    SymmetryInterface uc = viewer.modelSet.getUnitCell(iModel);
    if (uc == null)
      return "";
    return uc.getSymmetryInfo(viewer.modelSet, iModel, iAtom, uc, xyz, op, pt,
        pt2, id, type);
  }

  
  public String getModelExtract(BS bs, boolean doTransform,
                                boolean isModelKit, String type) {
    boolean asV3000 = type.equalsIgnoreCase("V3000");
    boolean asSDF = type.equalsIgnoreCase("SDF");
    boolean asXYZVIB = type.equalsIgnoreCase("XYZVIB");
    boolean asChemDoodle = type.equalsIgnoreCase("CD");
    SB mol = new SB();
    ModelSet ms = viewer.modelSet;
    if (!asXYZVIB && !asChemDoodle) {
      mol.append(isModelKit ? "Jmol Model Kit" : viewer.getFullPathName()
          .replace('\\', '/'));
      String version = Viewer.getJmolVersion();
      mol.append("\n__Jmol-").append(version.substring(0, 2));
      int cMM, cDD, cYYYY, cHH, cmm;
      /**
       * @j2sNative
       * 
       * var c = new Date();
       * cMM = c.getMonth();
       * cDD = c.getDate();
       * cYYYY = c.getFullYear();
       * cHH = c.getHours();
       * cmm = c.getMinutes();
       */
      {
        Calendar c = Calendar.getInstance();
        cMM = c.get(Calendar.MONTH);
        cDD = c.get(Calendar.DAY_OF_MONTH);
        cYYYY = c.get(Calendar.YEAR);
        cHH = c.get(Calendar.HOUR_OF_DAY);
        cmm = c.get(Calendar.MINUTE);
      }
      TextFormat.rFill(mol, "_00", "" + (1 + cMM));
      TextFormat.rFill(mol, "00", "" + cDD);
      mol.append(("" + cYYYY).substring(2, 4));
      TextFormat.rFill(mol, "00", "" + cHH);
      TextFormat.rFill(mol, "00", "" + cmm);
      mol.append("3D 1   1.00000     0.00000     0");
      //       This line has the format:
      //  IIPPPPPPPPMMDDYYHHmmddSSssssssssssEEEEEEEEEEEERRRRRR
      //  A2<--A8--><---A10-->A2I2<--F10.5-><---F12.5--><-I6->
      mol.append("\nJmol version ").append(Viewer.getJmolVersion()).append(
          " EXTRACT: ").append(Escape.e(bs)).append("\n");
    }
    BS bsAtoms = BSUtil.copy(bs);
    Atom[] atoms = ms.atoms;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      if (doTransform && atoms[i].isDeleted())
        bsAtoms.clear(i);
    BS bsBonds = getCovalentBondsForAtoms(ms.bonds, ms.bondCount, bsAtoms);
    if (!asXYZVIB && bsAtoms.cardinality() == 0)
      return "";
    boolean isOK = true;
    Quaternion q = (doTransform ? viewer.getRotationQuaternion() : null);
    if (asSDF) {
      String header = mol.toString();
      mol = new SB();
      BS bsModels = viewer.getModelBitSet(bsAtoms, true);
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1)) {
        mol.append(header);
        BS bsTemp = BSUtil.copy(bsAtoms);
        bsTemp.and(ms.getModelAtomBitSetIncludingDeleted(i, false));
        bsBonds = getCovalentBondsForAtoms(ms.bonds, ms.bondCount, bsTemp);
        if (!(isOK = addMolFile(mol, bsTemp, bsBonds, false, false, q)))
          break;
        mol.append("$$$$\n");
      }
    } else if (asXYZVIB) {
      LabelToken[] tokens1 = LabelToken.compile(viewer,
          "%-2e %10.5x %10.5y %10.5z %10.5vx %10.5vy %10.5vz\n", '\0', null);
      LabelToken[] tokens2 = LabelToken.compile(viewer,
          "%-2e %10.5x %10.5y %10.5z\n", '\0', null);
      BS bsModels = viewer.getModelBitSet(bsAtoms, true);
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1)) {
        BS bsTemp = BSUtil.copy(bsAtoms);
        bsTemp.and(ms.getModelAtomBitSetIncludingDeleted(i, false));
        if (bsTemp.cardinality() == 0)
          continue;
        mol.appendI(bsTemp.cardinality()).appendC('\n');
        Properties props = ms.models[i].properties;
        mol.append("Model[" + (i + 1) + "]: ");
        if (ms.frameTitles[i] != null && ms.frameTitles[i].length() > 0) {
          mol.append(ms.frameTitles[i].replace('\n', ' '));
        } else if (props == null) {
          mol.append("Jmol " + Viewer.getJmolVersion());
        } else {
          SB sb = new SB();
          Enumeration<?> e = props.propertyNames();
          String path = null;
          while (e.hasMoreElements()) {
            String propertyName = (String) e.nextElement();
            if (propertyName.equals(".PATH"))
              path = props.getProperty(propertyName);
            else
              sb.append(";").append(propertyName).append("=").append(
                  props.getProperty(propertyName));
          }
          if (path != null)
            sb.append(";PATH=").append(path);
          path = sb.substring(sb.length() > 0 ? 1 : 0);
          mol.append(path.replace('\n', ' '));
        }
        mol.appendC('\n');
        for (int j = bsTemp.nextSetBit(0); j >= 0; j = bsTemp.nextSetBit(j + 1))
          mol.append(LabelToken.formatLabelAtomArray(viewer, atoms[j],
              (ms.getVibrationVector(j, false) == null ? tokens2 : tokens1), '\0',
              null));
      }
    } else {
      isOK = addMolFile(mol, bsAtoms, bsBonds, asV3000, asChemDoodle, q);
    }
    return (isOK ? mol.toString()
        : "ERROR: Too many atoms or bonds -- use V3000 format.");
  }

  private boolean addMolFile(SB mol, BS bsAtoms, BS bsBonds,
                             boolean asV3000, boolean asChemDoodle, Quaternion q) {
    int nAtoms = bsAtoms.cardinality();
    int nBonds = bsBonds.cardinality();
    if (!asV3000 && !asChemDoodle && (nAtoms > 999 || nBonds > 999))
      return false;
    ModelSet ms = viewer.modelSet;
    int[] atomMap = new int[ms.atomCount];
    P3 pTemp = new P3();
    if (asV3000) {
      mol.append("  0  0  0  0  0  0            999 V3000");
    } else if (asChemDoodle) {
      mol.append("{\"mol\":{\"scaling\":[20,-20,20],\"a\":[");
    } else {
      TextFormat.rFill(mol, "   ", "" + nAtoms);
      TextFormat.rFill(mol, "   ", "" + nBonds);
      mol.append("  0  0  0  0              1 V2000");
    }
    if (!asChemDoodle)
      mol.append("\n");
    if (asV3000) {
      mol.append("M  V30 BEGIN CTAB\nM  V30 COUNTS ").appendI(nAtoms)
          .append(" ").appendI(nBonds).append(" 0 0 0\n").append(
              "M  V30 BEGIN ATOM\n");
    }
    P3 ptTemp = new P3();
    for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
        .nextSetBit(i + 1))
      getAtomRecordMOL(ms, mol, atomMap[i] = ++n, ms.atoms[i], q, pTemp, ptTemp, asV3000,
          asChemDoodle);
    if (asV3000) {
      mol.append("M  V30 END ATOM\nM  V30 BEGIN BOND\n");
    } else if (asChemDoodle) {
      mol.append("],\"b\":[");
    }
    for (int i = bsBonds.nextSetBit(0), n = 0; i >= 0; i = bsBonds
        .nextSetBit(i + 1))
      getBondRecordMOL(mol, ++n, ms.bonds[i], atomMap, asV3000, asChemDoodle);
    // 21 21 0 0 0
    if (asV3000) {
      mol.append("M  V30 END BOND\nM  V30 END CTAB\n");
    }
    if (asChemDoodle)
      mol.append("]}}");
    else {
      mol.append("M  END\n");
    }
    if (!asChemDoodle && !asV3000) {
      float[] pc = ms.getPartialCharges();
      if (pc != null) {
        mol.append("> <JMOL_PARTIAL_CHARGES>\n").appendI(nAtoms)
            .appendC('\n');
        for (int i = bsAtoms.nextSetBit(0), n = 0; i >= 0; i = bsAtoms
            .nextSetBit(i + 1))
          mol.appendI(++n).append(" ").appendF(pc[i]).appendC('\n');
      }
    }
    return true;
  }

  private static BS getCovalentBondsForAtoms(Bond[] bonds, int bondCount, BS bsAtoms) {
    BS bsBonds = new BS();
    for (int i = 0; i < bondCount; i++) {
      Bond bond = bonds[i];
      if (bsAtoms.get(bond.atom1.index) && bsAtoms.get(bond.atom2.index)
          && bond.isCovalent())
        bsBonds.set(i);
    }
    return bsBonds;
  }

  /*
  L-Alanine
  GSMACCS-II07189510252D 1 0.00366 0.00000 0
  Figure 1, J. Chem. Inf. Comput. Sci., Vol 32, No. 3., 1992
  0 0 0 0 0 999 V3000
  M  V30 BEGIN CTAB
  M  V30 COUNTS 6 5 0 0 1
  M  V30 BEGIN ATOM
  M  V30 1 C -0.6622 0.5342 0 0 CFG=2
  M  V30 2 C 0.6622 -0.3 0 0
  M  V30 3 C -0.7207 2.0817 0 0 MASS=13
  M  V30 4 N -1.8622 -0.3695 0 0 CHG=1
  M  V30 5 O 0.622 -1.8037 0 0
  M  V30 6 O 1.9464 0.4244 0 0 CHG=-1
  M  V30 END ATOM
  M  V30 BEGIN BOND
  M  V30 1 1 1 2
  M  V30 2 1 1 3 CFG=1
  M  V30 3 1 1 4
  M  V30 4 2 2 5
  M  V30 5 1 2 6
  M  V30 END BOND
  M  V30 END CTAB
  M  END
   */

  private void getAtomRecordMOL(ModelSet ms, SB mol, int n, Atom a, Quaternion q,
                                P3 pTemp, P3 ptTemp, boolean asV3000,
                                boolean asChemDoodle) {
    //   -0.9920    3.2030    9.1570 Cl  0  0  0  0  0
    //    3.4920    4.0920    5.8700 Cl  0  0  0  0  0
    //012345678901234567890123456789012
    
    if (ms.models[a.modelIndex].isTrajectory)
      a.setFractionalCoordPt(ptTemp, ms.trajectorySteps.get(a.modelIndex)[a.index
          - ms.models[a.modelIndex].firstAtomIndex], true);
    else
      pTemp.setT(a);
    if (q != null)
      q.transformP2(pTemp, pTemp);
    int elemNo = a.getElementNumber();
    String sym = (a.isDeleted() ? "Xx" : Elements
        .elementSymbolFromNumber(elemNo));
    int iso = a.getIsotopeNumber();
    int charge = a.getFormalCharge();
    if (asV3000) {
      mol.append("M  V30 ").appendI(n).append(" ").append(sym).append(" ")
          .appendF(pTemp.x).append(" ").appendF(pTemp.y).append(" ").appendF(
              pTemp.z).append(" 0");
      if (charge != 0)
        mol.append(" CHG=").appendI(charge);
      if (iso != 0)
        mol.append(" MASS=").appendI(iso);
      mol.append("\n");
    } else if (asChemDoodle) {
      if (n != 1)
        mol.append(",");
      mol.append("{");
      if (a.getElementNumber() != 6)
        mol.append("\"l\":\"").append(a.getElementSymbol()).append("\",");
      if (charge != 0)
        mol.append("\"c\":").appendI(charge).append(",");
      if (iso != 0 && iso != Elements.getNaturalIsotope(elemNo))
        mol.append("\"m\":").appendI(iso).append(",");
      mol.append("\"x\":").appendF(a.x*20).append(",\"y\":").appendF(-a.y*20).append(
          ",\"z\":").appendF(a.z*20).append("}");
    } else {
      mol.append(TextFormat.sprintf("%10.5p%10.5p%10.5p",
          "p", new Object[] {pTemp }));
      mol.append(" ").append(sym);
      if (sym.length() == 1)
        mol.append(" ");
      if (iso > 0)
        iso -= Elements.getNaturalIsotope(a.getElementNumber());
      mol.append(" ");
      TextFormat.rFill(mol, "  ", "" + iso);
      TextFormat.rFill(mol, "   ", "" + (charge == 0 ? 0 : 4 - charge));
      mol.append("  0  0  0  0\n");
    }
  }

  private void getBondRecordMOL(SB mol, int n, Bond b, int[] atomMap,
                                boolean asV3000, boolean asChemDoodle) {
    //  1  2  1  0
    int a1 = atomMap[b.atom1.index];
    int a2 = atomMap[b.atom2.index];
    int order = b.getValence();
    if (order > 3)
      order = 1;
    switch (b.order & ~JmolEdge.BOND_NEW) {
    case JmolEdge.BOND_AROMATIC:
      order = (asChemDoodle ? 2: 4);
      break;
    case JmolEdge.BOND_PARTIAL12:
      order = (asChemDoodle ? 1: 5);
      break;
    case JmolEdge.BOND_AROMATIC_SINGLE:
      order = (asChemDoodle ? 1: 6);
      break;
    case JmolEdge.BOND_AROMATIC_DOUBLE:
      order = (asChemDoodle ? 2: 7);
      break;
    case JmolEdge.BOND_PARTIAL01:
      order = (asChemDoodle ? 1: 8);
      break;
    }
    if (asV3000) {
      mol.append("M  V30 ").appendI(n).append(" ").appendI(order).append(" ")
          .appendI(a1).append(" ").appendI(a2).appendC('\n');
    } else if (asChemDoodle) {
      if (n != 1)
        mol.append(",");
      mol.append("{\"b\":").appendI(a1 - 1).append(",\"e\":").appendI(a2 - 1);
      if (order != 1)
        mol.append(",\"o\":").appendI(order);
      mol.append("}");
    } else {
      TextFormat.rFill(mol, "   ", "" + a1);
      TextFormat.rFill(mol, "   ", "" + a2);
      mol.append("  ").appendI(order).append("  0  0  0\n");
    }
  }

  public String getChimeInfo(int tok, BS bs) {
    switch (tok) {
    case T.info:
      break;
    case T.basepair:
      return getBasePairInfo(bs);
    default:
      return getChimeInfoA(viewer.modelSet.atoms, tok, bs);
    }
    SB sb = new SB();
    viewer.modelSet.models[0].getChimeInfo(sb, 0);
    return sb.appendC('\n').toString().substring(1);
  }

  private String getChimeInfoA(Atom[] atoms, int tok, BS bs) {
    SB info = new SB();
    info.append("\n");
    char id;
    String s = "";
    Chain clast = null;
    Group glast = null;
    int modelLast = -1;
    int n = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        id = atoms[i].getChainID();
        s = (id == '\0' ? " " : "" + id);
        switch (tok) {
        case T.chain:
          break;
        case T.selected:
          s = atoms[i].getInfo();
          break;
        case T.atoms:
          s = "" + atoms[i].getAtomNumber();
          break;
        case T.group:
          s = atoms[i].getGroup3(false);
          break;
        case T.residue:
          s = "[" + atoms[i].getGroup3(false) + "]"
              + atoms[i].getSeqcodeString() + ":" + s;
          break;
        case T.sequence:
          if (atoms[i].getModelIndex() != modelLast) {
            info.appendC('\n');
            n = 0;
            modelLast = atoms[i].getModelIndex();
            info.append("Model " + atoms[i].getModelNumber());
            glast = null;
            clast = null;
          }
          if (atoms[i].getChain() != clast) {
            info.appendC('\n');
            n = 0;
            clast = atoms[i].getChain();
            info.append("Chain " + s + ":\n");
            glast = null;
          }
          Group g = atoms[i].getGroup();
          if (g != glast) {
            if ((n++) % 5 == 0 && n > 1)
              info.appendC('\n');
            TextFormat.lFill(info, "          ", "["
                + atoms[i].getGroup3(false) + "]" + atoms[i].getResno() + " ");
            glast = g;
          }
          continue;
        default:
          return "";
        }
        if (info.indexOf("\n" + s + "\n") < 0)
          info.append(s).appendC('\n');
      }
    if (tok == T.sequence)
      info.appendC('\n');
    return info.toString().substring(1);
  }

  public String getModelFileInfo(BS frames) {
    ModelSet ms = viewer.modelSet;
    SB sb = new SB();
    for (int i = 0; i < ms.modelCount; ++i) {
      if (frames != null && !frames.get(i))
        continue;
      String s = "[\"" + ms.getModelNumberDotted(i) + "\"] = ";
      sb.append("\n\nfile").append(s).append(Escape.eS(ms.getModelFileName(i)));
      String id = (String) ms.getModelAuxiliaryInfoValue(i, "modelID");
      if (id != null)
        sb.append("\nid").append(s).append(Escape.eS(id));
      sb.append("\ntitle").append(s).append(Escape.eS(ms.getModelTitle(i)));
      sb.append("\nname").append(s).append(Escape.eS(ms.getModelName(i)));
    }
    return sb.toString();
  }

  public JmolList<Map<String, Object>> getAllAtomInfo(BS bs) {
    JmolList<Map<String, Object>> V = new  JmolList<Map<String, Object>>();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      V.addLast(getAtomInfoLong(i));
    }
    return V;
  }

  public void getAtomIdentityInfo(int i, Map<String, Object> info) {
    ModelSet ms = viewer.modelSet;
    info.put("_ipt", Integer.valueOf(i));
    info.put("atomIndex", Integer.valueOf(i));
    info.put("atomno", Integer.valueOf(ms.getAtomNumber(i)));
    info.put("info", ms.getAtomInfo(i, null));
    info.put("sym", ms.getElementSymbol(i));
  }

  private Map<String, Object> getAtomInfoLong(int i) {
    ModelSet ms = viewer.modelSet;
    Atom atom = ms.atoms[i];
    Map<String, Object> info = new Hashtable<String, Object>();
    getAtomIdentityInfo(i, info);
    info.put("element", ms.getElementName(i));
    info.put("elemno", Integer.valueOf(ms.getElementNumber(i)));
    info.put("x", Float.valueOf(atom.x));
    info.put("y", Float.valueOf(atom.y));
    info.put("z", Float.valueOf(atom.z));
    info.put("coord", P3.newP(atom));
    if (ms.vibrationVectors != null && ms.vibrationVectors[i] != null) {
      info.put("vibVector", V3.newV(ms.vibrationVectors[i]));
    }
    info.put("bondCount", Integer.valueOf(atom.getCovalentBondCount()));
    info.put("radius", Float.valueOf((float) (atom.getRasMolRadius() / 120.0)));
    info.put("model", atom.getModelNumberForLabel());
    info.put("shape", Atom.atomPropertyString(viewer, atom, T.shape));
    info.put("visible", Boolean.valueOf(atom.isVisible(0)));
    info.put("clickabilityFlags", Integer.valueOf(atom.clickabilityFlags));
    info.put("visibilityFlags", Integer.valueOf(atom.shapeVisibilityFlags));
    info.put("spacefill", Float.valueOf(atom.getRadius()));
    String strColor = Escape.escapeColor(viewer
        .getColorArgbOrGray(atom.colixAtom));
    if (strColor != null)
      info.put("color", strColor);
    info.put("colix", Integer.valueOf(atom.colixAtom));
    boolean isTranslucent = atom.isTranslucent();
    if (isTranslucent)
      info.put("translucent", Boolean.valueOf(isTranslucent));
    info.put("formalCharge", Integer.valueOf(atom.getFormalCharge()));
    info.put("partialCharge", Float.valueOf(atom.getPartialCharge()));
    float d = atom.getSurfaceDistance100() / 100f;
    if (d >= 0)
      info.put("surfaceDistance", Float.valueOf(d));
    if (ms.models[atom.modelIndex].isBioModel) {
      info.put("resname", atom.getGroup3(false));
      int seqNum = atom.getSeqNumber();
      char insCode = atom.getInsertionCode();
      if (seqNum > 0)
        info.put("resno", Integer.valueOf(seqNum));
      if (insCode != 0)
        info.put("insertionCode", "" + insCode);
      char chainID = atom.getChainID();
      info.put("name", ms.getAtomName(i));
      info.put("chain", (chainID == '\0' ? "" : "" + chainID));
      info.put("atomID", Integer.valueOf(atom.atomID));
      info.put("groupID", Integer.valueOf(atom.getGroupID()));
      if (atom.alternateLocationID != '\0')
        info.put("altLocation", "" + atom.alternateLocationID);
      info.put("structure", Integer.valueOf(atom.getProteinStructureType()
          .getId()));
      info.put("polymerLength", Integer.valueOf(atom.getPolymerLength()));
      info.put("occupancy", Integer.valueOf(atom.getOccupancy100()));
      int temp = atom.getBfactor100();
      info.put("temp", Integer.valueOf(temp / 100));
    }
    return info;
  }

  public JmolList<Map<String, Object>> getAllBondInfo(BS bs) {
    JmolList<Map<String, Object>> v = new  JmolList<Map<String, Object>>();
    ModelSet ms = viewer.modelSet;
    int bondCount = ms.bondCount;
    if (bs instanceof BondSet) {
      for (int i = bs.nextSetBit(0); i >= 0 && i < bondCount; i = bs.nextSetBit(i + 1))
        v.addLast(getBondInfo(i));
      return v;
    }
    int thisAtom = (bs.cardinality() == 1 ? bs.nextSetBit(0) : -1);
    Bond[] bonds = ms.bonds;
    for (int i = 0; i < bondCount; i++) {
      if (thisAtom >= 0 ? (bonds[i].atom1.index == thisAtom || bonds[i].atom2.index == thisAtom)
          : bs.get(bonds[i].atom1.index) && bs.get(bonds[i].atom2.index)) {
        v.addLast(getBondInfo(i));
      }
    }
    return v;
  }

  private Map<String, Object> getBondInfo(int i) {
    Bond bond = viewer.modelSet.bonds[i];
    Atom atom1 = bond.atom1;
    Atom atom2 = bond.atom2;
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("_bpt", Integer.valueOf(i));
    Map<String, Object> infoA = new Hashtable<String, Object>();
    getAtomIdentityInfo(atom1.index, infoA);
    Map<String, Object> infoB = new Hashtable<String, Object>();
    getAtomIdentityInfo(atom2.index, infoB);
    info.put("atom1", infoA);
    info.put("atom2", infoB);
    info.put("order", Float.valueOf(Parser.fVal(JmolEdge
        .getBondOrderNumberFromOrder(bond.order))));
    info.put("radius", Float.valueOf((float) (bond.mad / 2000.)));
    info.put("length_Ang", Float.valueOf(atom1.distance(atom2)));
    info.put("visible", Boolean.valueOf(bond.shapeVisibilityFlags != 0));
    String strColor = Escape.escapeColor(viewer.getColorArgbOrGray(bond.colix));
    if (strColor != null)
      info.put("color", strColor);
    info.put("colix", Integer.valueOf(bond.colix));
    boolean isTranslucent = bond.isTranslucent();
    if (isTranslucent)
      info.put("translucent", Boolean.valueOf(isTranslucent));
    return info;
  }

  public Map<String, JmolList<Map<String, Object>>> getAllChainInfo(BS bs) {
    Map<String, JmolList<Map<String, Object>>> finalInfo = new Hashtable<String, JmolList<Map<String, Object>>>();
    JmolList<Map<String, Object>> modelVector = new  JmolList<Map<String, Object>>();
    int modelCount = viewer.modelSet.modelCount;
    for (int i = 0; i < modelCount; ++i) {
      Map<String, Object> modelInfo = new Hashtable<String, Object>();
      JmolList<Map<String, JmolList<Map<String, Object>>>> info = getChainInfo(i, bs);
      if (info.size() > 0) {
        modelInfo.put("modelIndex", Integer.valueOf(i));
        modelInfo.put("chains", info);
        modelVector.addLast(modelInfo);
      }
    }
    finalInfo.put("models", modelVector);
    return finalInfo;
  }

  private JmolList<Map<String, JmolList<Map<String, Object>>>> getChainInfo(
                                                                    int modelIndex,
                                                                    BS bs) {
    Model model = viewer.modelSet.models[modelIndex];
    int nChains = model.getChainCount(true);
    JmolList<Map<String, JmolList<Map<String, Object>>>> infoChains = new  JmolList<Map<String, JmolList<Map<String, Object>>>>();
    for (int i = 0; i < nChains; i++) {
      Chain chain = model.getChainAt(i);
      JmolList<Map<String, Object>> infoChain = new  JmolList<Map<String, Object>>();
      int nGroups = chain.getGroupCount();
      Map<String, JmolList<Map<String, Object>>> arrayName = new Hashtable<String, JmolList<Map<String, Object>>>();
      for (int igroup = 0; igroup < nGroups; igroup++) {
        Group group = chain.getGroup(igroup);
        if (bs.get(group.firstAtomIndex))
          infoChain.addLast(group.getGroupInfo(igroup));
      }
      if (!infoChain.isEmpty()) {
        arrayName.put("residues", infoChain);
        infoChains.addLast(arrayName);
      }
    }
    return infoChains;
  }

  public Map<String, JmolList<Map<String, Object>>> getAllPolymerInfo(BS bs) {
    Map<String, JmolList<Map<String, Object>>> finalInfo = new Hashtable<String, JmolList<Map<String, Object>>>();
    JmolList<Map<String, Object>> modelVector = new  JmolList<Map<String, Object>>();
    int modelCount = viewer.modelSet.modelCount;
    Model[] models = viewer.modelSet.models;
    for (int i = 0; i < modelCount; ++i)
      if (models[i].isBioModel)
        models[i].getAllPolymerInfo(bs, finalInfo, modelVector);
    finalInfo.put("models", modelVector);
    return finalInfo;
  }

  private String getBasePairInfo(BS bs) {
    SB info = new SB();
    JmolList<Bond> vHBonds = new  JmolList<Bond>();
    viewer.modelSet.calcRasmolHydrogenBonds(bs, bs, vHBonds, true, 1, false, null);
    for (int i = vHBonds.size(); --i >= 0;) {
      Bond b = vHBonds.get(i);
      getAtomResidueInfo(info, b.atom1);
      info.append(" - ");
      getAtomResidueInfo(info, b.atom2);
      info.append("\n");
    }
    return info.toString();
  }

  private static void getAtomResidueInfo(SB info, Atom atom) {
    info.append("[").append(atom.getGroup3(false)).append("]").append(
        atom.getSeqcodeString()).append(":");
    char id = atom.getChainID();
    info.append(id == '\0' ? " " : "" + id);
  }

  
  
  
}
