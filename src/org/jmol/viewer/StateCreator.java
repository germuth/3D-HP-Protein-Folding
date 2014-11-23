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

package org.jmol.viewer;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import org.jmol.util.JmolList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import java.util.Map;

import org.jmol.api.JmolImageCreatorInterface;
import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.api.JmolScriptFunction;
import org.jmol.api.JmolStateCreator;
import org.jmol.api.SymmetryInterface;
import org.jmol.constant.EnumPalette;
import org.jmol.constant.EnumStereoMode;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumVdw;
import org.jmol.io.Base64;
import org.jmol.io.JmolBinary;
import org.jmol.io.OutputStringBuilder;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.TickInfo;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.modelset.ModelCollection.StateScript;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.shape.AtomShape;
import org.jmol.shape.Echo;
import org.jmol.shape.Halos;
import org.jmol.shape.Hover;
import org.jmol.shape.Labels;
import org.jmol.shape.Object2d;
import org.jmol.shape.Shape;
import org.jmol.shape.Text;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;
import org.jmol.util.GData;
import org.jmol.util.JmolEdge;
import org.jmol.util.JmolFont;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.P3;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;
import org.jmol.viewer.StateManager.GlobalSettings;
import org.jmol.viewer.Viewer.ACCESS;

/**
 * Called by reflection only; all state generation script here, for
 * modularization in JavaScript
 * 
 * instantiated by reflection only!
 * 
 */
public class StateCreator implements JmolStateCreator {

  public StateCreator() {

    // by reflection only!

  }

  private Viewer viewer;

  public void setViewer(Viewer viewer) {
    this.viewer = viewer;
  }

  public Object getWrappedState(String fileName, String[] scripts,
                                boolean isImage, boolean asJmolZip, int width,
                                int height) {
    if (isImage && !viewer.global.imageState && !asJmolZip
        || !viewer.global.preserveState)
      return "";
    String s = viewer.getStateInfo3(null, width, height);
    if (asJmolZip) {
      if (fileName != null)
        viewer.fileManager.clearPngjCache(fileName);
      // when writing a file, we need to make sure
      // the pngj cache for that file is cleared
      return JmolBinary.createZipSet(viewer.fileManager, viewer, null, s,
          scripts, true);
    }
    // we remove local file references in the embedded states for images
    try {
      s = JC.embedScript(FileManager.setScriptFileReferences(s, ".",
          null, null));
    } catch (Throwable e) {
      // ignore if this uses too much memory
      Logger.error("state could not be saved: " + e.toString());
      s = "Jmol " + Viewer.getJmolVersion();
    }
    return s;
  }

  public String getStateScript(String type, int width, int height) {
    //System.out.println("viewer getStateInfo " + type);
    boolean isAll = (type == null || type.equalsIgnoreCase("all"));
    SB s = new SB();
    SB sfunc = (isAll ? new SB()
        .append("function _setState() {\n") : null);
    if (isAll)
      s.append(JC.STATE_VERSION_STAMP + Viewer.getJmolVersion()
          + ";\n");
    if (viewer.isApplet() && isAll) {
      appendCmd(s, "# fullName = " + Escape.eS(viewer.fullName));
      appendCmd(s, "# documentBase = "
          + Escape.eS(viewer.appletDocumentBase));
      appendCmd(s, "# codeBase = " + Escape.eS(viewer.appletCodeBase));
      s.append("\n");
    }

    StateManager.GlobalSettings global = viewer.global;
    // window state
    if (isAll || type.equalsIgnoreCase("windowState"))
      s.append(getWindowState(sfunc, width, height));
    //if (isAll)
    //s.append(getFunctionCalls(null)); // removed in 12.1.16; unnecessary in state
    // file state
    if (isAll || type.equalsIgnoreCase("fileState"))
      s.append(getFileState(sfunc));
    // all state scripts (definitions, dataFrames, calculations, configurations,
    // rebonding
    if (isAll || type.equalsIgnoreCase("definedState"))
      s.append(getDefinedState(sfunc, true));
    // numerical values
    if (isAll || type.equalsIgnoreCase("variableState"))
      s.append(getVariableState(global, sfunc)); // removed in 12.1.16; unnecessary in state // ARGH!!!
    if (isAll || type.equalsIgnoreCase("dataState"))
      getDataState(viewer.dataManager, s, sfunc, getAtomicPropertyState(
          (byte) -1, null));
    // connections, atoms, bonds, labels, echos, shapes
    if (isAll || type.equalsIgnoreCase("modelState"))
      s.append(getModelState(sfunc, true, viewer
          .getBooleanProperty("saveProteinStructureState")));
    // color scheme
    if (isAll || type.equalsIgnoreCase("colorState"))
      s.append(getColorState(viewer.colorManager, sfunc));
    // frame information
    if (isAll || type.equalsIgnoreCase("frameState"))
      s.append(getAnimState(viewer.animationManager, sfunc));
    // orientation and slabbing
    if (isAll || type.equalsIgnoreCase("perspectiveState"))
      s.append(getViewState(viewer.transformManager, sfunc));
    // display and selections
    if (isAll || type.equalsIgnoreCase("selectionState"))
      s.append(getSelectionState(viewer.selectionManager, sfunc));
    if (sfunc != null) {
      appendCmd(sfunc, "set refreshing true");
      appendCmd(sfunc, "set antialiasDisplay " + global.antialiasDisplay);
      appendCmd(sfunc, "set antialiasTranslucent "
          + global.antialiasTranslucent);
      appendCmd(sfunc, "set antialiasImages " + global.antialiasImages);
      if (viewer.getSpinOn())
        appendCmd(sfunc, "spin on");
      sfunc.append("}\n\n_setState;\n");
    }
    if (isAll)
      s.appendSB(sfunc);
    return s.toString();
  }

  private String getDefinedState(SB sfunc, boolean isAll) {
    ModelSet ms = viewer.modelSet;
    int len = ms.stateScripts.size();
    if (len == 0)
      return "";

    boolean haveDefs = false;
    SB commands = new SB();
    String cmd;
    for (int i = 0; i < len; i++) {
      StateScript ss = ms.stateScripts.get(i);
      if (ss.inDefinedStateBlock && (cmd = ss.toString()).length() > 0) {
        commands.append("  ").append(cmd).append("\n");
        haveDefs = true;
      }
    }
    if (!haveDefs)
      return "";
    cmd = "";
    if (isAll && sfunc != null) {
      sfunc.append("  _setDefinedState;\n");
      cmd = "function _setDefinedState() {\n\n";
    }

    if (sfunc != null)
      commands.append("\n}\n\n");
    return cmd + commands.toString();
  }

  public String getModelState(SB sfunc, boolean isAll,
                              boolean withProteinStructure) {
    SB commands = new SB();
    if (isAll && sfunc != null) {
      sfunc.append("  _setModelState;\n");
      commands.append("function _setModelState() {\n");
    }
    String cmd;

    // connections

    ModelSet ms = viewer.modelSet;
    Bond[] bonds = ms.bonds;
    Model[] models = ms.models;
    int modelCount = ms.modelCount;

    if (isAll) {

      int len = ms.stateScripts.size();
      for (int i = 0; i < len; i++) {
        StateScript ss = ms.stateScripts.get(i);
        if (!ss.inDefinedStateBlock && (cmd = ss.toString()).length() > 0) {
          commands.append("  ").append(cmd).append("\n");
        }
      }

      SB sb = new SB();
      for (int i = 0; i < ms.bondCount; i++)
        if (!models[bonds[i].atom1.modelIndex].isModelKit)
          if (bonds[i].isHydrogen()
              || (bonds[i].order & JmolEdge.BOND_NEW) != 0) {
            Bond bond = bonds[i];
            int index = bond.atom1.index;
            if (bond.atom1.getGroup().isAdded(index))
              index = -1 - index;
            sb.appendI(index).appendC('\t').appendI(bond.atom2.index).appendC(
                '\t').appendI(bond.order & ~JmolEdge.BOND_NEW).appendC('\t')
                .appendF(bond.mad / 1000f).appendC('\t').appendF(
                    bond.getEnergy()).appendC('\t').append(
                    JmolEdge.getBondOrderNameFromOrder(bond.order)).append(
                    ";\n");
          }
      if (sb.length() > 0)
        commands.append("data \"connect_atoms\"\n").appendSB(sb).append(
            "end \"connect_atoms\";\n");
      commands.append("\n");
    }

    // bond visibility

    if (ms.haveHiddenBonds) {
      BondSet bs = new BondSet();
      for (int i = ms.bondCount; --i >= 0;)
        if (bonds[i].mad != 0
            && (bonds[i].shapeVisibilityFlags & Bond.myVisibilityFlag) == 0)
          bs.set(i);
      if (bs.isEmpty())
        ms.haveHiddenBonds = false;
      else
        commands.append("  hide ").append(Escape.eB(bs, false)).append(
            ";\n");
    }

    // shape construction

    viewer.setModelVisibility();

    // unnecessary. Removed in 11.5.35 -- oops!

    if (withProteinStructure)
      commands.append(ms.getProteinStructureState(null, isAll, false, 0));

    getShapeState(commands, isAll, Integer.MAX_VALUE);

    if (isAll) {
      boolean needOrientations = false;
      for (int i = 0; i < modelCount; i++)
        if (models[i].isJmolDataFrame) {
          needOrientations = true;
          break;
        }
      for (int i = 0; i < modelCount; i++) {
        String fcmd = "  frame " + ms.getModelNumberDotted(i);
        String s = (String) ms.getModelAuxiliaryInfoValue(i, "modelID");
        if (s != null
            && !s.equals(ms.getModelAuxiliaryInfoValue(i, "modelID0")))
          commands.append(fcmd).append("; frame ID ").append(
              Escape.eS(s)).append(";\n");
        String t = ms.frameTitles[i];
        if (t != null && t.length() > 0)
          commands.append(fcmd).append("; frame title ").append(
              Escape.eS(t)).append(";\n");
        if (needOrientations && models[i].orientation != null
            && !ms.isTrajectorySubFrame(i))
          commands.append(fcmd).append("; ").append(
              models[i].orientation.getMoveToText(false)).append(";\n");
        if (models[i].frameDelay != 0 && !ms.isTrajectorySubFrame(i))
          commands.append(fcmd).append("; frame delay ").appendF(
              models[i].frameDelay / 1000f).append(";\n");
        if (models[i].simpleCage != null) {
          commands.append(fcmd).append("; unitcell ").append(
              Escape.e(models[i].simpleCage.getUnitCellVectors())).append(
              ";\n");
          getShapeState(commands, isAll, JC.SHAPE_UCCAGE);
        }
      }

      if (ms.unitCells != null) {
        for (int i = 0; i < modelCount; i++) {
          SymmetryInterface symmetry = ms.getUnitCell(i);
          if (symmetry == null)
            continue;
          commands.append("  frame ").append(ms.getModelNumberDotted(i));
          P3 pt = symmetry.getFractionalOffset();
          if (pt != null)
            commands.append("; set unitcell ").append(Escape.eP(pt));
          pt = symmetry.getUnitCellMultiplier();
          if (pt != null)
            commands.append("; set unitcell ").append(Escape.eP(pt));
          commands.append(";\n");
        }
        getShapeState(commands, isAll, JC.SHAPE_UCCAGE);
        //        if (viewer.getObjectMad(StateManager.OBJ_UNITCELL) == 0)
        //        commands.append("  unitcell OFF;\n");
      }
      commands.append("  set fontScaling " + viewer.getFontScaling() + ";\n");
      if (viewer.isModelKitMode())
        commands.append("  set modelKitMode true;\n");
    }
    if (sfunc != null)
      commands.append("\n}\n\n");
    return commands.toString();
  }

  private void getShapeState(SB commands, boolean isAll, int iShape) {
    Shape[] shapes = viewer.shapeManager.shapes;
    if (shapes == null)
      return;
    String cmd;
    Shape shape;
    int i;
    int imax;
    if (iShape == Integer.MAX_VALUE) {
      i = 0;
      imax = JC.SHAPE_MAX;
    } else {
      imax = (i = iShape) + 1;
    }
    for (; i < imax; ++i)
      if ((shape = shapes[i]) != null
          && (isAll || JC.isShapeSecondary(i))
          && (cmd = shape.getShapeState()) != null && cmd.length() > 1)
        commands.append(cmd);
    commands.append("  select *;\n");
  }

  private String getWindowState(SB sfunc, int width, int height) {
    GlobalSettings global = viewer.global;
    SB str = new SB();
    if (sfunc != null) {
      sfunc
          .append("  initialize;\n  set refreshing false;\n  _setWindowState;\n");
      str.append("\nfunction _setWindowState() {\n");
    }
    if (width != 0)
      str.append("# preferredWidthHeight ").appendI(width).append(" ").appendI(
          height).append(";\n");
    str.append("# width ")
        .appendI(width == 0 ? viewer.getScreenWidth() : width).append(
            ";\n# height ").appendI(
            height == 0 ? viewer.getScreenHeight() : height).append(";\n");
    appendCmd(str, "stateVersion = " + global.getParameter("_version"));
    appendCmd(str, "background " + Escape.escapeColor(global.objColors[0]));
    for (int i = 1; i < StateManager.OBJ_MAX; i++)
      if (global.objColors[i] != 0)
        appendCmd(str, StateManager.getObjectNameFromId(i) + "Color = \""
            + Escape.escapeColor(global.objColors[i]) + '"');
    if (global.backgroundImageFileName != null)
      appendCmd(str, "background IMAGE /*file*/"
          + Escape.eS(global.backgroundImageFileName));
    str.append(getSpecularState());
    appendCmd(str, "statusReporting  = " + global.statusReporting);
    if (sfunc != null)
      str.append("}\n\n");
    return str.toString();
  }

  public String getSpecularState() {
    SB str = new SB();
    GData g = viewer.gdata;
    appendCmd(str, "set ambientPercent " + g.getAmbientPercent());
    appendCmd(str, "set diffusePercent " + g.getDiffusePercent());
    appendCmd(str, "set specular " + g.getSpecular());
    appendCmd(str, "set specularPercent " + g.getSpecularPercent());
    appendCmd(str, "set specularPower " + g.getSpecularPower());
    appendCmd(str, "set celShading " + g.getCel());
    int se = g.getSpecularExponent();
    int pe = g.getPhongExponent();
    if (Math.pow(2, se) == pe)
      appendCmd(str, "set specularExponent " + se);
    else
      appendCmd(str, "set phongExponent " + pe);
    appendCmd(str, "set zShadePower " + viewer.global.zShadePower);
    return str.toString();
  }

  private String getFileState(SB sfunc) {
    SB commands = new SB();
    if (sfunc != null) {
      sfunc.append("  _setFileState;\n");
      commands.append("function _setFileState() {\n\n");
    }
    if (commands.indexOf("append") < 0
        && viewer.getModelSetFileName().equals("zapped"))
      commands.append("  zap;\n");
    appendLoadStates(commands);
    if (sfunc != null)
      commands.append("\n}\n\n");
    return commands.toString();
  }

  private void getDataState(DataManager dm, SB state,
                            SB sfunc, String atomProps) {
    if (dm.dataValues == null)
      return;
    Iterator<String> e = dm.dataValues.keySet().iterator();
    SB sb = new SB();
    int n = 0;
    if (atomProps.length() > 0) {
      n = 1;
      sb.append(atomProps);
    }
    while (e.hasNext()) {
      String name = e.next();
      if (name.indexOf("property_") == 0) {
        n++;
        Object[] obj = dm.dataValues.get(name);
        Object data = obj[1];
        if (data != null && ((Integer) obj[3]).intValue() == 1) {
          getAtomicPropertyStateBuffer(sb, AtomCollection.TAINT_MAX,
              (BS) obj[2], name, (float[]) data);
          sb.append("\n");
        } else {
          sb.append("\n").append(Escape.encapsulateData(name, data, 0));//j2s issue?
        }
      } else if (name.indexOf("data2d") == 0) {
        Object[] obj = dm.dataValues.get(name);
        Object data = obj[1];
        if (data != null && ((Integer) obj[3]).intValue() == 2) {
          n++;
          sb.append("\n").append(Escape.encapsulateData(name, data, 2));
        }
      } else if (name.indexOf("data3d") == 0) {
        Object[] obj = dm.dataValues.get(name);
        Object data = obj[1];
        if (data != null && ((Integer) obj[3]).intValue() == 3) {
          n++;
          sb.append("\n").append(Escape.encapsulateData(name, data, 3));
        }
      }
    }

    if (dm.userVdws != null) {
      String info = dm.getDefaultVdwNameOrData(0, EnumVdw.USER, dm.bsUserVdws);
      if (info.length() > 0) {
        n++;
        sb.append(info);
      }
    }

    if (n == 0)
      return;
    if (sfunc != null)
      state.append("function _setDataState() {\n");
    state.appendSB(sb);
    if (sfunc != null) {
      sfunc.append("  _setDataState;\n");
      state.append("}\n\n");
    }
  }

  private String getColorState(ColorManager cm, SB sfunc) {
    SB s = new SB();
    int n = getCEState(cm.propertyColorEncoder, s);
    //String colors = getColorSchemeList(getColorSchemeArray(USER));
    //if (colors.length() > 0)
    //s.append("userColorScheme = " + colors + ";\n");
    if (n > 0 && sfunc != null)
      sfunc.append("\n  _setColorState\n");
    return (n > 0 && sfunc != null ? "function _setColorState() {\n"
        + s.append("}\n\n").toString() : s.toString());
  }

  private int getCEState(ColorEncoder p, SB s) {
    int n = 0;
    for (Map.Entry<String, int[]> entry : p.schemes.entrySet()) {
      String name = entry.getKey();
      if (name.length() > 0 & n++ >= 0)
        s.append("color \"" + name + "="
            + ColorEncoder.getColorSchemeList(entry.getValue()) + "\";\n");
    }
    return n;
  }

  private String getAnimState(AnimationManager am, SB sfunc) {
    int modelCount = viewer.getModelCount();
    if (modelCount < 2)
      return "";
    SB commands = new SB();
    if (sfunc != null) {
      sfunc.append("  _setFrameState;\n");
      commands.append("function _setFrameState() {\n");
    }
    commands.append("# frame state;\n");

    commands.append("# modelCount ").appendI(modelCount).append(";\n# first ")
        .append(viewer.getModelNumberDotted(0)).append(";\n# last ").append(
            viewer.getModelNumberDotted(modelCount - 1)).append(";\n");
    if (am.backgroundModelIndex >= 0)
      appendCmd(commands, "set backgroundModel "
          + viewer.getModelNumberDotted(am.backgroundModelIndex));
    BS bs = viewer.getFrameOffsets();
    if (bs != null)
      appendCmd(commands, "frame align " + Escape.e(bs));
    appendCmd(commands, "frame RANGE "
        + am.getModelNumber(-1) + " "
        + am.getModelNumber(1));
    appendCmd(commands, "animation DIRECTION "
        + (am.animationDirection == 1 ? "+1" : "-1"));
    appendCmd(commands, "animation FPS " + am.animationFps);
    appendCmd(commands, "animation MODE " + am.animationReplayMode.name() + " "
        + am.firstFrameDelay + " " + am.lastFrameDelay);
    if (am.morphCount > 0)
      appendCmd(commands, "animation MORPH " + am.morphCount);
    appendCmd(commands, "frame "
        + am.getModelNumber(0));
    appendCmd(commands, "animation "
        + (!am.animationOn ? "OFF" : am.currentDirection == 1 ? "PLAY"
            : "PLAYREV"));
    if (am.animationOn && am.animationPaused)
      appendCmd(commands, "animation PAUSE");
    if (sfunc != null)
      commands.append("}\n\n");
    return commands.toString();
  }

  private String getVariableState(StateManager.GlobalSettings global,
                                  SB sfunc) {
    String[] list = new String[global.htBooleanParameterFlags.size()
        + global.htNonbooleanParameterValues.size()];
    SB commands = new SB();
    boolean isState = (sfunc != null);
    if (isState) {
      sfunc.append("  _setVariableState;\n");
      commands.append("function _setVariableState() {\n\n");
    }
    int n = 0;
    Iterator<String> e;
    String key;
    //booleans
    e = global.htBooleanParameterFlags.keySet().iterator();
    while (e.hasNext()) {
      key = e.next();
      if (StateManager.doReportProperty(key))
        list[n++] = "set " + key + " "
            + global.htBooleanParameterFlags.get(key);
    }
    e = global.htNonbooleanParameterValues.keySet().iterator();
    while (e.hasNext()) {
      key = e.next();
      if (StateManager.doReportProperty(key)) {
        Object value = global.htNonbooleanParameterValues.get(key);
        if (key.charAt(0) == '=') {
          //save as =xxxx if you don't want "set" to be there first
          // (=color [element], =frame ...; set unitcell) -- see Viewer.java
          key = key.substring(1);
        } else {
          if (key.indexOf("default") == 0)
            key = " set " + key;
          else
            key = "set " + key;
          value = Escape.e(value);
        }
        list[n++] = key + " " + value;
      }
    }
    switch (global.axesMode) {
    case UNITCELL:
      list[n++] = "set axes unitcell";
      break;
    case BOUNDBOX:
      list[n++] = "set axes window";
      break;
    default:
      list[n++] = "set axes molecular";
    }

    Arrays.sort(list, 0, n);
    for (int i = 0; i < n; i++)
      if (list[i] != null)
        appendCmd(commands, list[i]);

    String s = StateManager.getVariableList(global.htUserVariables, 0, false,
        true);
    if (s.length() > 0) {
      commands.append("\n#user-defined atom sets; \n");
      commands.append(s);
    }

    // label defaults

    viewer.loadShape(JC.SHAPE_LABELS);
    commands
        .append(getDefaultLabelState((Labels) viewer.shapeManager.shapes[JC.SHAPE_LABELS]));

    // structure defaults

    if (global.haveSetStructureList) {
      Map<EnumStructure, float[]> slist = global.structureList;
      commands.append("struture HELIX set "
          + Escape.e(slist.get(EnumStructure.HELIX)));
      commands.append("struture SHEET set "
          + Escape.e(slist.get(EnumStructure.SHEET)));
      commands.append("struture TURN set "
          + Escape.e(slist.get(EnumStructure.TURN)));
    }
    if (sfunc != null)
      commands.append("\n}\n\n");
    return commands.toString();
  }

  private String getDefaultLabelState(Labels l) {
    SB s = new SB().append("\n# label defaults;\n");
    appendCmd(s, "select none");
    appendCmd(s, Shape.getColorCommand("label", l.defaultPaletteID,
        l.defaultColix, l.translucentAllowed));
    appendCmd(s, "background label " + Shape.encodeColor(l.defaultBgcolix));
    appendCmd(s, "set labelOffset " + Object2d.getXOffset(l.defaultOffset)
        + " " + (-Object2d.getYOffset(l.defaultOffset)));
    String align = Object2d.getAlignmentName(l.defaultAlignment);
    appendCmd(s, "set labelAlignment " + (align.length() < 5 ? "left" : align));
    String pointer = Object2d.getPointer(l.defaultPointer);
    appendCmd(s, "set labelPointer "
        + (pointer.length() == 0 ? "off" : pointer));
    if ((l.defaultZPos & Labels.FRONT_FLAG) != 0)
      appendCmd(s, "set labelFront");
    else if ((l.defaultZPos & Labels.GROUP_FLAG) != 0)
      appendCmd(s, "set labelGroup");
    appendCmd(s, Shape.getFontCommand("label", JmolFont
        .getFont3D(l.defaultFontId)));
    return s.toString();
  }

  private String getSelectionState(SelectionManager sm, SB sfunc) {
    SB commands = new SB();
    if (sfunc != null) {
      sfunc.append("  _setSelectionState;\n");
      commands.append("function _setSelectionState() {\n");
    }
    appendCmd(commands, getTrajectoryState());
    Map<String, BS> temp = new Hashtable<String, BS>();
    String cmd = null;
    addBs(commands, "hide ", sm.bsHidden);
    addBs(commands, "subset ", sm.bsSubset);
    addBs(commands, "delete ", sm.bsDeleted);
    addBs(commands, "fix ", sm.bsFixed);
    temp.put("-", sm.bsSelection);
    cmd = getCommands(temp, null, "select");
    if (cmd == null)
      appendCmd(commands, "select none");
    else
      commands.append(cmd);
    appendCmd(commands, "set hideNotSelected " + sm.hideNotSelected);
    commands.append((String) viewer.getShapeProperty(
        JC.SHAPE_STICKS, "selectionState"));
    if (viewer.getSelectionHaloEnabled(false))
      appendCmd(commands, "SelectionHalos ON");
    if (sfunc != null)
      commands.append("}\n\n");
    return commands.toString();
  }

  public String getTrajectoryState() {
    String s = "";
    ModelSet m = viewer.modelSet;
    if (m.trajectorySteps == null)
      return "";
    for (int i = m.modelCount; --i >= 0;) {
      int t = m.models[i].getSelectedTrajectory(); 
      if (t >= 0) {
        s = " or " + m.getModelNumberDotted(t) + s;
        i = m.models[i].trajectoryBaseIndex; //skip other trajectories
      }
    }
    if (s.length() > 0)
      s = "set trajectory {" + s.substring(4) + "}";
    return s;
  }

  private String getViewState(TransformManager tm, SB sfunc) {
    SB commands = new SB();
    if (sfunc != null) {
      sfunc.append("  _setPerspectiveState;\n");
      commands.append("function _setPerspectiveState() {\n");
    }
    appendCmd(commands, "set perspectiveModel " + tm.perspectiveModel);
    appendCmd(commands, "set scaleAngstromsPerInch "
        + tm.scale3DAngstromsPerInch);
    appendCmd(commands, "set perspectiveDepth " + tm.perspectiveDepth);
    appendCmd(commands, "set visualRange " + tm.visualRange);
    if (!tm.isWindowCentered())
      appendCmd(commands, "set windowCentered false");
    appendCmd(commands, "set cameraDepth " + tm.cameraDepth);
    if (tm.mode == TransformManager.MODE_NAVIGATION)
      appendCmd(commands, "set navigationMode true");
    appendCmd(commands, viewer.getBoundBoxCommand(false));
    appendCmd(commands, "center " + Escape.eP(tm.fixedRotationCenter));
    commands.append(viewer.getSavedOrienationText(null));

    appendCmd(commands, tm.getMoveToText(0, false));
    if (tm.stereoMode != EnumStereoMode.NONE)
      appendCmd(commands, "stereo "
          + (tm.stereoColors == null ? tm.stereoMode.getName() : Escape
              .escapeColor(tm.stereoColors[0])
              + " " + Escape.escapeColor(tm.stereoColors[1])) + " "
          + tm.stereoDegrees);
    if (tm.mode != TransformManager.MODE_NAVIGATION && !tm.zoomEnabled)
      appendCmd(commands, "zoom off");
    commands
        .append("  slab ")
        .appendI(tm.slabPercentSetting)
        .append(";depth ")
        .appendI(tm.depthPercentSetting)
        .append(
            tm.slabEnabled && tm.mode != TransformManager.MODE_NAVIGATION ? ";slab on"
                : "").append(";\n");
    commands.append("  set slabRange ").appendF(tm.slabRange).append(";\n");
    if (tm.zShadeEnabled)
      commands.append("  set zShade;\n");
    try {
      if (tm.zSlabPoint != null)
        commands.append("  set zSlab ").append(Escape.eP(tm.zSlabPoint))
            .append(";\n");
    } catch (Exception e) {
      // don't care
    }
    if (tm.slabPlane != null)
      commands.append("  slab plane ").append(Escape.e(tm.slabPlane))
          .append(";\n");
    if (tm.depthPlane != null)
      commands.append("  depth plane ").append(Escape.e(tm.depthPlane))
          .append(";\n");
    commands.append(getSpinState(true)).append("\n");
    if (viewer.modelSetHasVibrationVectors() && tm.vibrationOn)
      appendCmd(commands, "set vibrationPeriod " + tm.vibrationPeriod
          + ";vibration on");
    if (tm.mode == TransformManager.MODE_NAVIGATION) {
      commands.append(tm.getNavigationState());
      if (tm.depthPlane != null || tm.slabPlane != null)
        commands.append("  slab on;\n");
    }
    if (sfunc != null)
      commands.append("}\n\n");
    return commands.toString();
  }

  /**
   * @param isAll
   * @return spin state
   */
  public String getSpinState(boolean isAll) {
    TransformManager tm = viewer.transformManager;
    String s = "  set spinX " + (int) tm.spinX + "; set spinY "
        + (int) tm.spinY + "; set spinZ " + (int) tm.spinZ + "; set spinFps "
        + (int) tm.spinFps + ";";
    if (!Float.isNaN(tm.navFps))
      s += "  set navX " + (int) tm.navX + "; set navY " + (int) tm.navY
          + "; set navZ " + (int) tm.navZ + "; set navFps " + (int) tm.navFps
          + ";";
    if (tm.navOn)
      s += " navigation on;";
    if (!tm.spinOn)
      return s;
    String prefix = (tm.isSpinSelected ? "\n  select "
        + Escape.e(viewer.getSelectionSet(false)) + ";\n  rotateSelected"
        : "\n ");
    if (tm.isSpinInternal) {
      P3 pt = P3.newP(tm.internalRotationCenter);
      pt.sub(tm.rotationAxis);
      s += prefix + " spin " + tm.rotationRate + " "
          + Escape.eP(tm.internalRotationCenter) + " "
          + Escape.eP(pt);
    } else if (tm.isSpinFixed) {
      s += prefix + " spin axisangle " + Escape.eP(tm.rotationAxis) + " "
          + tm.rotationRate;
    } else {
      s += " spin on";
    }
    return s + ";";
  }

  //// info 

  public Map<String, Object> getInfo(Object manager) {
    if (manager instanceof AnimationManager)
      return getAnimationInfo((AnimationManager) manager);
    return null;
  }

  private Map<String, Object> getAnimationInfo(AnimationManager am) {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("firstModelIndex", Integer.valueOf(am.firstFrameIndex));
    info.put("lastModelIndex", Integer.valueOf(am.lastFrameIndex));
    info.put("animationDirection", Integer.valueOf(am.animationDirection));
    info.put("currentDirection", Integer.valueOf(am.currentDirection));
    info.put("displayModelIndex", Integer.valueOf(am.currentModelIndex));
    info.put("displayModelNumber", viewer
        .getModelNumberDotted(am.currentModelIndex));
    info.put("displayModelName", (am.currentModelIndex >= 0 ? viewer
        .getModelName(am.currentModelIndex) : ""));
    info.put("animationFps", Integer.valueOf(am.animationFps));
    info.put("animationReplayMode", am.animationReplayMode.name());
    info.put("firstFrameDelay", Float.valueOf(am.firstFrameDelay));
    info.put("lastFrameDelay", Float.valueOf(am.lastFrameDelay));
    info.put("animationOn", Boolean.valueOf(am.animationOn));
    info.put("animationPaused", Boolean.valueOf(am.animationPaused));
    return info;
  }

  //// utility methods

  public String getCommands(Map<String, BS> htDefine,
                            Map<String, BS> htMore, String selectCmd) {
    SB s = new SB();
    String setPrev = getCommands2(htDefine, s, null, selectCmd);
    if (htMore != null)
      getCommands2(htMore, s, setPrev, "select");
    return s.toString();
  }

  private static String getCommands2(Map<String, BS> ht, SB s,
                                     String setPrev, String selectCmd) {
    if (ht == null)
      return "";
    for (Map.Entry<String, BS> entry : ht.entrySet()) {
      String key = entry.getKey();
      String set = Escape.e(entry.getValue());
      if (set.length() < 5) // nothing selected
        continue;
      set = selectCmd + " " + set;
      if (!set.equals(setPrev))
        appendCmd(s, set);
      setPrev = set;
      if (key.indexOf("-") != 0) // - for key means none required
        appendCmd(s, key);
    }
    return setPrev;
  }

  private static void appendCmd(SB s, String cmd) {
    if (cmd.length() == 0)
      return;
    s.append("  ").append(cmd).append(";\n");
  }

  private static void addBs(SB sb, String key, BS bs) {
    if (bs == null || bs.length() == 0)
      return;
    appendCmd(sb, key + Escape.e(bs));
  }

  public String getFontState(String myType, JmolFont font3d) {
    int objId = StateManager.getObjectIdFromName(myType
        .equalsIgnoreCase("axes") ? "axis" : myType);
    if (objId < 0)
      return "";
    int mad = viewer.getObjectMad(objId);
    SB s = new SB().append("\n");
    appendCmd(s, myType
        + (mad == 0 ? " off" : mad == 1 ? " on" : mad == -1 ? " dotted"
            : mad < 20 ? " " + mad : " " + (mad / 2000f)));
    if (s.length() < 3)
      return "";
    String fcmd = Shape.getFontCommand(myType, font3d);
    if (fcmd.length() > 0)
      fcmd = "  " + fcmd + ";\n";
    return (s + fcmd);
  }

  public String getFontLineShapeState(String s, String myType,
                                      TickInfo[] tickInfos) {
    boolean isOff = (s.indexOf(" off") >= 0);
    SB sb = new SB();
    sb.append(s);
    for (int i = 0; i < 4; i++)
      if (tickInfos[i] != null)
        appendTickInfo(myType, sb, tickInfos[i]);
    if (isOff)
      sb.append("  " + myType + " off;\n");
    return sb.toString();
  }

  private void appendTickInfo(String myType, SB sb, TickInfo t) {
    sb.append("  ");
    sb.append(myType);
    addTickInfo(sb, t, false);
    sb.append(";\n");
  }

  private static void addTickInfo(SB sb, TickInfo tickInfo,
                                  boolean addFirst) {
    sb.append(" ticks ").append(tickInfo.type).append(" ").append(
        Escape.eP(tickInfo.ticks));
    boolean isUnitCell = (tickInfo.scale != null && Float
        .isNaN(tickInfo.scale.x));
    if (isUnitCell)
      sb.append(" UNITCELL");
    if (tickInfo.tickLabelFormats != null)
      sb.append(" format ").append(
          Escape.escapeStrA(tickInfo.tickLabelFormats, false));
    if (!isUnitCell && tickInfo.scale != null)
      sb.append(" scale ").append(Escape.eP(tickInfo.scale));
    if (addFirst && !Float.isNaN(tickInfo.first) && tickInfo.first != 0)
      sb.append(" first ").appendF(tickInfo.first);
    if (tickInfo.reference != null) // not implemented
      sb.append(" point ").append(Escape.eP(tickInfo.reference));
  }

  public void getShapeSetState(AtomShape as, Shape shape, int monomerCount,
                               Group[] monomers, BS bsSizeDefault,
                               Map<String, BS> temp,
                               Map<String, BS> temp2) {
    String type = JC.shapeClassBases[shape.shapeID];
    for (int i = 0; i < monomerCount; i++) {
      int atomIndex1 = monomers[i].firstAtomIndex;
      int atomIndex2 = monomers[i].lastAtomIndex;
      if (as.bsSizeSet != null
          && (as.bsSizeSet.get(i) || as.bsColixSet != null
              && as.bsColixSet.get(i))) {//shapes MUST have been set with a size
        if (bsSizeDefault.get(i))
          BSUtil.setMapBitSet(temp, atomIndex1, atomIndex2, type
              + (as.bsSizeSet.get(i) ? " on" : " off"));
        else
          BSUtil.setMapBitSet(temp, atomIndex1, atomIndex2, type + " "
              + (as.mads[i] / 2000f));
      }
      if (as.bsColixSet != null && as.bsColixSet.get(i))
        BSUtil.setMapBitSet(temp2, atomIndex1, atomIndex2, Shape
            .getColorCommand(type, as.paletteIDs[i], as.colixes[i],
                shape.translucentAllowed));
    }
  }

  public String getMeasurementState(AtomShape as, JmolList<Measurement> mList,
                                    int measurementCount, JmolFont font3d,
                                    TickInfo ti) {
    SB commands = new SB();
    appendCmd(commands, "measures delete");
    for (int i = 0; i < measurementCount; i++) {
      Measurement m = mList.get(i);
      int count = m.getCount();
      SB sb = new SB().append("measure");
      TickInfo tickInfo = m.getTickInfo();
      if (tickInfo != null)
        addTickInfo(sb, tickInfo, true);
      for (int j = 1; j <= count; j++)
        sb.append(" ").append(m.getLabel(j, true, true));
      sb.append("; # " + as.getInfoAsString(i));
      appendCmd(commands, sb.toString());
    }
    appendCmd(commands, "select *; set measures "
        + viewer.getMeasureDistanceUnits());
    appendCmd(commands, Shape.getFontCommand("measures", font3d));
    int nHidden = 0;
    Map<String, BS> temp = new Hashtable<String, BS>();
    BS bs = BSUtil.newBitSet(measurementCount);
    for (int i = 0; i < measurementCount; i++) {
      Measurement m = mList.get(i);
      if (m.isHidden) {
        nHidden++;
        bs.set(i);
      }
      if (as.bsColixSet != null && as.bsColixSet.get(i))
        BSUtil.setMapBitSet(temp, i, i, Shape.getColorCommandUnk("measure",
            m.colix, as.translucentAllowed));
      if (m.getStrFormat() != null)
        BSUtil.setMapBitSet(temp, i, i, "measure "
            + Escape.eS(m.getStrFormat()));
    }
    if (nHidden > 0)
      if (nHidden == measurementCount)
        appendCmd(commands, "measures off; # lines and numbers off");
      else
        for (int i = 0; i < measurementCount; i++)
          if (bs.get(i))
            BSUtil.setMapBitSet(temp, i, i, "measure off");
    if (ti != null) {
      commands.append(" measure ");
      addTickInfo(commands, ti, true);
      commands.append(";\n");
    }
    if (as.mad >= 0)
      commands.append(" set measurements " + (as.mad / 2000f)).append(";\n");
    String s = getCommands(temp, null, "select measures");
    if (s != null && s.length() != 0) {
      commands.append(s);
      appendCmd(commands, "select measures ({null})");
    }

    return commands.toString();
  }

  private Map<String, BS> temp = new Hashtable<String, BS>();
  private Map<String, BS> temp2 = new Hashtable<String, BS>();
  private Map<String, BS> temp3 = new Hashtable<String, BS>();

  public String getBondState(Shape shape, BS bsOrderSet, boolean reportAll) {
    clearTemp();
    ModelSet modelSet = viewer.modelSet;
    boolean haveTainted = false;
    Bond[] bonds = modelSet.bonds;
    int bondCount = modelSet.bondCount;
    short r;

    if (reportAll || shape.bsSizeSet != null) {
      int i0 = (reportAll ? bondCount - 1 : shape.bsSizeSet.nextSetBit(0));
      for (int i = i0; i >= 0; i = (reportAll ? i - 1 : shape.bsSizeSet
          .nextSetBit(i + 1)))
        BSUtil.setMapBitSet(temp, i, i, "wireframe "
            + ((r = bonds[i].mad) == 1 ? "on" : "" + (r / 2000f)));
    }
    if (reportAll || bsOrderSet != null) {
      int i0 = (reportAll ? bondCount - 1 : bsOrderSet.nextSetBit(0));
      for (int i = i0; i >= 0; i = (reportAll ? i - 1 : bsOrderSet
          .nextSetBit(i + 1))) {
        Bond bond = bonds[i];
        if (reportAll || (bond.order & JmolEdge.BOND_NEW) == 0)
          BSUtil.setMapBitSet(temp, i, i, "bondOrder "
              + JmolEdge.getBondOrderNameFromOrder(bond.order));
      }
    }
    if (shape.bsColixSet != null)
      for (int i = shape.bsColixSet.nextSetBit(0); i >= 0; i = shape.bsColixSet
          .nextSetBit(i + 1)) {
        short colix = bonds[i].colix;
        if ((colix & C.OPAQUE_MASK) == C.USE_PALETTE)
          BSUtil.setMapBitSet(temp, i, i, Shape.getColorCommand("bonds",
              EnumPalette.CPK.id, colix, shape.translucentAllowed));
        else
          BSUtil.setMapBitSet(temp, i, i, Shape.getColorCommandUnk("bonds",
              colix, shape.translucentAllowed));
      }

    String s = getCommands(temp, null, "select BONDS") + "\n"
        + (haveTainted ? getCommands(temp2, null, "select BONDS") + "\n" : "");
    clearTemp();
    return s;
  }

  private void clearTemp() {
    temp.clear();
    temp2.clear();
  }

  public String getAtomShapeSetState(Shape shape, AtomShape[] bioShapes) {
    clearTemp();
    for (int i = bioShapes.length; --i >= 0;) {
      AtomShape bs = bioShapes[i];
      if (bs.monomerCount > 0) {
        if (!bs.isActive || bs.bsSizeSet == null && bs.bsColixSet == null)
          continue;
        viewer.getShapeSetState(bs, shape, bs.monomerCount, bs.getMonomers(),
            bs.bsSizeDefault, temp, temp2);
      }
    }
    String s = "\n"
        + getCommands(temp, temp2,
            shape.shapeID == JC.SHAPE_BACKBONE ? "Backbone"
                : "select");
    clearTemp();
    return s;
  }

  public String getShapeState(Shape shape) {
    clearTemp();
    String s;
    switch (shape.shapeID) {
    case JC.SHAPE_ECHO:
      Echo es = (Echo) shape;
      SB sb = new SB();
      sb.append("\n  set echo off;\n");
      Iterator<Text> e = es.objects.values().iterator();
      while (e.hasNext()) {
        Text t = e.next();
        sb.append(t.getState());
        if (t.hidden)
          sb.append("  set echo ID ").append(Escape.eS(t.target))
              .append(" hidden;\n");
      }
      s = sb.toString();
      break;
    case JC.SHAPE_HALOS:
      Halos hs = (Halos) shape;
      s = getAtomShapeState(hs)
          + (hs.colixSelection == C.USE_PALETTE ? ""
              : hs.colixSelection == C.INHERIT_ALL ? "  color SelectionHalos NONE;\n"
                  : Shape.getColorCommandUnk("selectionHalos",
                      hs.colixSelection, hs.translucentAllowed)
                      + ";\n");
      if (hs.bsHighlight != null)
        s += "  set highlight "
            + Escape.e(hs.bsHighlight)
            + "; "
            + Shape.getColorCommandUnk("highlight", hs.colixHighlight,
                hs.translucentAllowed) + ";\n";
      break;
    case JC.SHAPE_HOVER:
      Hover h = (Hover) shape;
      if (h.atomFormats != null)
        for (int i = viewer.getAtomCount(); --i >= 0;)
          if (h.atomFormats[i] != null)
            BSUtil.setMapBitSet(temp, i, i, "set hoverLabel "
                + Escape.eS(h.atomFormats[i]));
      s = "\n  hover "
          + Escape.eS((h.labelFormat == null ? "" : h.labelFormat))
          + ";\n" + getCommands(temp, null, "select");
      break;
    case JC.SHAPE_LABELS:
      Labels l = (Labels) shape;
      for (int i = l.bsSizeSet.nextSetBit(0); i >= 0; i = l.bsSizeSet
          .nextSetBit(i + 1)) {
        BSUtil.setMapBitSet(temp, i, i, "label "
            + Escape.eS(l.formats[i]));
        if (l.bsColixSet != null && l.bsColixSet.get(i))
          BSUtil.setMapBitSet(temp2, i, i, Shape.getColorCommand("label",
              l.paletteIDs[i], l.colixes[i], l.translucentAllowed));
        if (l.bsBgColixSet != null && l.bsBgColixSet.get(i))
          BSUtil.setMapBitSet(temp2, i, i, "background label "
              + Shape.encodeColor(l.bgcolixes[i]));
        Text text = l.getLabel(i);
        float sppm = (text != null ? text.getScalePixelsPerMicron() : 0);
        if (sppm > 0)
          BSUtil.setMapBitSet(temp2, i, i, "set labelScaleReference "
              + (10000f / sppm));
        if (l.offsets != null && l.offsets.length > i) {
          int offsetFull = l.offsets[i];
          BSUtil
              .setMapBitSet(
                  temp2,
                  i,
                  i,
                  "set "
                      + ((offsetFull & Labels.EXACT_OFFSET_FLAG) == Labels.EXACT_OFFSET_FLAG ? "labelOffsetExact "
                          : "labelOffset ")
                      + Object2d.getXOffset(offsetFull >> Labels.FLAG_OFFSET)
                      + " "
                      + (-Object2d.getYOffset(offsetFull >> Labels.FLAG_OFFSET)));
          String align = Object2d.getAlignmentName(offsetFull >> 2);
          String pointer = Object2d.getPointer(offsetFull);
          if (pointer.length() > 0)
            BSUtil.setMapBitSet(temp2, i, i, "set labelPointer " + pointer);
          if ((offsetFull & Labels.FRONT_FLAG) != 0)
            BSUtil.setMapBitSet(temp2, i, i, "set labelFront");
          else if ((offsetFull & Labels.GROUP_FLAG) != 0)
            BSUtil.setMapBitSet(temp2, i, i, "set labelGroup");
          // labelAlignment must come last, so we put it in a separate hash
          // table
          if (align.length() > 0)
            BSUtil.setMapBitSet(temp3, i, i, "set labelAlignment " + align);
        }
        if (l.mads != null && l.mads[i] < 0)
          BSUtil.setMapBitSet(temp2, i, i, "set toggleLabel");
        if (l.bsFontSet != null && l.bsFontSet.get(i))
          BSUtil.setMapBitSet(temp2, i, i, Shape.getFontCommand("label",
              JmolFont.getFont3D(l.fids[i])));
      }
      s = getCommands(temp, temp2, "select")
          + getCommands(null, temp3, "select");
      temp3.clear();
      break;
    case JC.SHAPE_BALLS:
      int atomCount = viewer.getAtomCount();
      Atom[] atoms = viewer.modelSet.atoms;
      float r = 0;
      for (int i = 0; i < atomCount; i++) {
        if (shape.bsSizeSet != null && shape.bsSizeSet.get(i)) {
          if ((r = atoms[i].madAtom) < 0)
            BSUtil.setMapBitSet(temp, i, i, "Spacefill on");
          else
            BSUtil.setMapBitSet(temp, i, i, "Spacefill " + (r / 2000f));
        }
        if (shape.bsColixSet != null && shape.bsColixSet.get(i)) {
          byte pid = atoms[i].getPaletteID();
          if (pid != EnumPalette.CPK.id || atoms[i].isTranslucent())
            BSUtil.setMapBitSet(temp, i, i, Shape.getColorCommand("atoms",
                pid, atoms[i].getColix(), shape.translucentAllowed));
        }
      }
      s = getCommands(temp, null, "select");
      break;
    default:
      s = "";
    }
    clearTemp();
    return s;
  }

  /**
   * these settings are determined when the file is loaded and are kept even
   * though they might later change. So we list them here and ALSO let them be
   * defined in the settings. 10.9.98 missed this.
   * 
   * @param htParams
   * 
   * @return script command
   */
  public String getLoadState(Map<String, Object> htParams) {
    GlobalSettings g = viewer.global;

    // some commands register flags so that they will be 
    // restored in a saved state definition, but will not execute
    // now so that there is no chance any embedded scripts or
    // default load scripts will run and slow things down.
    SB str = new SB();
    appendCmd(str, "set allowEmbeddedScripts false");
    if (g.allowEmbeddedScripts)
      g.setB("allowEmbeddedScripts", true);
    appendCmd(str, "set appendNew " + g.appendNew);
    appendCmd(str, "set appletProxy " + Escape.eS(g.appletProxy));
    appendCmd(str, "set applySymmetryToBonds " + g.applySymmetryToBonds);
    if (g.atomTypes.length() > 0)
      appendCmd(str, "set atomTypes " + Escape.eS(g.atomTypes));
    appendCmd(str, "set autoBond " + g.autoBond);
    //    appendCmd(str, "set autoLoadOrientation " + autoLoadOrientation);
    if (g.axesOrientationRasmol)
      appendCmd(str, "set axesOrientationRasmol true");
    appendCmd(str, "set bondRadiusMilliAngstroms " + g.bondRadiusMilliAngstroms);
    appendCmd(str, "set bondTolerance " + g.bondTolerance);
    appendCmd(str, "set defaultLattice " + Escape.eP(g.ptDefaultLattice));
    appendCmd(str, "set defaultLoadFilter "
        + Escape.eS(g.defaultLoadFilter));
    appendCmd(str, "set defaultLoadScript \"\"");
    if (g.defaultLoadScript.length() > 0)
      g.setS("defaultLoadScript", g.defaultLoadScript);
    appendCmd(str, "set defaultStructureDssp " + g.defaultStructureDSSP);
    String sMode = viewer.getDefaultVdwTypeNameOrData(Integer.MIN_VALUE, null);
    appendCmd(str, "set defaultVDW " + sMode);
    if (sMode.equals("User"))
      appendCmd(str, viewer
          .getDefaultVdwTypeNameOrData(Integer.MAX_VALUE, null));
    appendCmd(str, "set forceAutoBond " + g.forceAutoBond);
    appendCmd(str, "#set defaultDirectory "
        + Escape.eS(g.defaultDirectory));
    appendCmd(str, "#set loadFormat " + Escape.eS(g.loadFormat));
    appendCmd(str, "#set loadLigandFormat "
        + Escape.eS(g.loadLigandFormat));
    appendCmd(str, "#set smilesUrlFormat "
        + Escape.eS(g.smilesUrlFormat));
    appendCmd(str, "#set nihResolverFormat "
        + Escape.eS(g.nihResolverFormat));
    appendCmd(str, "#set pubChemFormat " + Escape.eS(g.pubChemFormat));
    appendCmd(str, "#set edsUrlFormat " + Escape.eS(g.edsUrlFormat));
    appendCmd(str, "#set edsUrlCutoff " + Escape.eS(g.edsUrlCutoff));
    //    if (autoLoadOrientation)
    //      appendCmd(str, "set autoLoadOrientation true");
    appendCmd(str, "set legacyAutoBonding " + g.legacyAutoBonding);
    appendCmd(str, "set minBondDistance " + g.minBondDistance);
    // these next two might be part of a 2D->3D operation
    appendCmd(str, "set minimizationCriterion  " + g.minimizationCriterion);
    appendCmd(str, "set minimizationSteps  " + g.minimizationSteps);
    appendCmd(
        str,
        "set pdbAddHydrogens "
            + (htParams != null && htParams.get("pdbNoHydrogens") == null ? g.pdbAddHydrogens
                : false));
    appendCmd(str, "set pdbGetHeader " + g.pdbGetHeader);
    appendCmd(str, "set pdbSequential " + g.pdbSequential);
    appendCmd(str, "set percentVdwAtom " + g.percentVdwAtom);
    appendCmd(str, "set smallMoleculeMaxAtoms " + g.smallMoleculeMaxAtoms);
    appendCmd(str, "set smartAromatic " + g.smartAromatic);
    if (g.zeroBasedXyzRasmol)
      appendCmd(str, "set zeroBasedXyzRasmol true");
    return str.toString();
  }

  public String getAllSettings(String prefix) {
    GlobalSettings g = viewer.global;
    SB commands = new SB();
    Iterator<String> e;
    String key;
    String[] list = new String[g.htBooleanParameterFlags.size()
        + g.htNonbooleanParameterValues.size() + g.htUserVariables.size()];
    //booleans
    int n = 0;
    String _prefix = "_" + prefix;
    e = g.htBooleanParameterFlags.keySet().iterator();
    while (e.hasNext()) {
      key = e.next();
      if (prefix == null || key.indexOf(prefix) == 0
          || key.indexOf(_prefix) == 0)
        list[n++] = (key.indexOf("_") == 0 ? key + " = " : "set " + key + " ")
            + g.htBooleanParameterFlags.get(key);
    }
    //save as _xxxx if you don't want "set" to be there first
    e = g.htNonbooleanParameterValues.keySet().iterator();
    while (e.hasNext()) {
      key = e.next();
      if (key.charAt(0) != '@'
          && (prefix == null || key.indexOf(prefix) == 0 || key
              .indexOf(_prefix) == 0)) {
        Object value = g.htNonbooleanParameterValues.get(key);
        if (value instanceof String)
          value = chop(Escape.eS((String) value));
        list[n++] = (key.indexOf("_") == 0 ? key + " = " : "set " + key + " ")
            + value;
      }
    }
    e = g.htUserVariables.keySet().iterator();
    while (e.hasNext()) {
      key = e.next();
      if (prefix == null || key.indexOf(prefix) == 0) {
        SV value = g.htUserVariables.get(key);
        String s = value.asString();
        list[n++] = key + " " + (key.startsWith("@") ? "" : "= ")
            + (value.tok == T.string ? chop(Escape.eS(s)) : s);
      }
    }
    Arrays.sort(list, 0, n);
    for (int i = 0; i < n; i++)
      if (list[i] != null)
        appendCmd(commands, list[i]);
    commands.append("\n");
    return commands.toString();
  }

  private static String chop(String s) {
    int len = s.length();
    if (len < 512)
      return s;
    SB sb = new SB();
    String sep = "\"\\\n    + \"";
    int pt = 0;
    for (int i = 72; i < len; pt = i, i += 72) {
      while (s.charAt(i - 1) == '\\')
        i++;
      sb.append((pt == 0 ? "" : sep)).append(s.substring(pt, i));
    }
    sb.append(sep).append(s.substring(pt, len));
    return sb.toString();
  }

  public String getAtomShapeState(AtomShape shape) {
    clearTemp();
    String type = JC.shapeClassBases[shape.shapeID];
    if (shape.bsSizeSet != null)
      for (int i = shape.bsSizeSet.nextSetBit(0); i >= 0; i = shape.bsSizeSet
          .nextSetBit(i + 1))
        BSUtil.setMapBitSet(temp, i, i, type
            + (shape.mads[i] < 0 ? " on" : " " + shape.mads[i] / 2000f));
    if (shape.bsColixSet != null)
      for (int i = shape.bsColixSet.nextSetBit(0); i >= 0; i = shape.bsColixSet
          .nextSetBit(i + 1))
        BSUtil.setMapBitSet(temp2, i, i, Shape.getColorCommand(type,
            shape.paletteIDs[i], shape.colixes[i], shape.translucentAllowed));
    String s = getCommands(temp, temp2, "select");
    clearTemp();
    return s;
  }

  public String getFunctionCalls(String selectedFunction) {
    if (selectedFunction == null)
      selectedFunction = "";
    SB s = new SB();
    int pt = selectedFunction.indexOf("*");
    boolean isGeneric = (pt >= 0);
    boolean isStatic = (selectedFunction.indexOf("static_") == 0);
    boolean namesOnly = (selectedFunction.equalsIgnoreCase("names") || selectedFunction
        .equalsIgnoreCase("static_names"));
    if (namesOnly)
      selectedFunction = "";
    if (isGeneric)
      selectedFunction = selectedFunction.substring(0, pt);
    selectedFunction = selectedFunction.toLowerCase();
    Map<String, JmolScriptFunction> ht = (isStatic ? Viewer.staticFunctions
        : viewer.localFunctions);
    String[] names = new String[ht.size()];
    Iterator<String> e = ht.keySet().iterator();
    int n = 0;
    while (e.hasNext()) {
      String name = e.next();
      if (selectedFunction.length() == 0 && !name.startsWith("_")
          || name.equalsIgnoreCase(selectedFunction) || isGeneric
          && name.toLowerCase().indexOf(selectedFunction) == 0)
        names[n++] = name;
    }
    Arrays.sort(names, 0, n);
    for (int i = 0; i < n; i++) {
      JmolScriptFunction f = ht.get(names[i]);
      s.append(namesOnly ? f.getSignature() : f.toString());
      s.appendC('\n');
    }
    return s.toString();
  }

  private static boolean isTainted(BS[] tainted, int atomIndex, byte type) {
    return (tainted != null && tainted[type] != null && tainted[type]
        .get(atomIndex));
  }

  public String getAtomicPropertyState(byte taintWhat, BS bsSelected) {
    if (!viewer.global.preserveState)
      return "";
    BS bs;
    SB commands = new SB();
    for (byte type = 0; type < AtomCollection.TAINT_MAX; type++)
      if (taintWhat < 0 || type == taintWhat)
        if ((bs = (bsSelected != null ? bsSelected : viewer
            .getTaintedAtoms(type))) != null)
          getAtomicPropertyStateBuffer(commands, type, bs, null, null);
    return commands.toString();
  }

  public void getAtomicPropertyStateBuffer(SB commands, byte type,
                                           BS bs, String label,
                                           float[] fData) {
    if (!viewer.global.preserveState)
      return;
    // see setAtomData()
    SB s = new SB();
    String dataLabel = (label == null ? AtomCollection.userSettableValues[type]
        : label)
        + " set";
    int n = 0;
    boolean isDefault = (type == AtomCollection.TAINT_COORD);
    Atom[] atoms = viewer.modelSet.atoms;
    BS[] tainted = viewer.modelSet.tainted;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        s.appendI(i + 1).append(" ").append(atoms[i].getElementSymbol())
            .append(" ").append(atoms[i].getInfo().replace(' ', '_')).append(
                " ");
        switch (type) {
        case AtomCollection.TAINT_MAX:
          if (i < fData.length) // when data are appended, the array may not
            // extend that far
            s.appendF(fData[i]);
          break;
        case AtomCollection.TAINT_ATOMNO:
          s.appendI(atoms[i].getAtomNumber());
          break;
        case AtomCollection.TAINT_ATOMNAME:
          s.append(atoms[i].getAtomName());
          break;
        case AtomCollection.TAINT_ATOMTYPE:
          s.append(atoms[i].getAtomType());
          break;
        case AtomCollection.TAINT_COORD:
          if (isTainted(tainted, i, AtomCollection.TAINT_COORD))
            isDefault = false;
          s.appendF(atoms[i].x).append(" ").appendF(atoms[i].y).append(" ")
              .appendF(atoms[i].z);
          break;
        case AtomCollection.TAINT_VIBRATION:
          V3 v = atoms[i].getVibrationVector();
          if (v == null)
            v = new V3();
          s.appendF(v.x).append(" ").appendF(v.y).append(" ").appendF(v.z);
          break;
        case AtomCollection.TAINT_ELEMENT:
          s.appendI(atoms[i].getAtomicAndIsotopeNumber());
          break;
        case AtomCollection.TAINT_FORMALCHARGE:
          s.appendI(atoms[i].getFormalCharge());
          break;
        case AtomCollection.TAINT_IONICRADIUS:
          s.appendF(atoms[i].getBondingRadiusFloat());
          break;
        case AtomCollection.TAINT_OCCUPANCY:
          s.appendI(atoms[i].getOccupancy100());
          break;
        case AtomCollection.TAINT_PARTIALCHARGE:
          s.appendF(atoms[i].getPartialCharge());
          break;
        case AtomCollection.TAINT_TEMPERATURE:
          s.appendF(atoms[i].getBfactor100() / 100f);
          break;
        case AtomCollection.TAINT_VALENCE:
          s.appendI(atoms[i].getValence());
          break;
        case AtomCollection.TAINT_VANDERWAALS:
          s.appendF(atoms[i].getVanderwaalsRadiusFloat(viewer, EnumVdw.AUTO));
          break;
        }
        s.append(" ;\n");
        ++n;
      }
    if (n == 0)
      return;
    if (isDefault)
      dataLabel += "(default)";
    commands.append("\n  DATA \"" + dataLabel + "\"\n").appendI(n).append(
        " ;\nJmol Property Data Format 1 -- Jmol ").append(
        Viewer.getJmolVersion()).append(";\n");
    commands.appendSB(s);
    commands.append("  end \"" + dataLabel + "\";\n");
  }

  public void undoMoveAction(int action, int n) {
    switch (action) {
    case T.undomove:
    case T.redomove:
      switch (n) {
      case -2:
        viewer.undoClear();
        break;
      case -1:
        (action == T.undomove ? viewer.actionStates
            : viewer.actionStatesRedo).clear();
        break;
      case 0:
        n = Integer.MAX_VALUE;
        //$FALL-THROUGH$
      default:
        if (n > MAX_ACTION_UNDO)
          n = (action == T.undomove ? viewer.actionStates
              : viewer.actionStatesRedo).size();
        for (int i = 0; i < n; i++)
          undoMoveActionClear(0, action, true);
      }
      break;
    }
  }

  public void undoMoveActionClear(int taintedAtom, int type, boolean clearRedo) {
    // called by actionManager
    if (!viewer.global.preserveState)
      return;
    int modelIndex = (taintedAtom >= 0 ? viewer.modelSet.atoms[taintedAtom].modelIndex
        : viewer.modelSet.modelCount - 1);
    //System.out.print("undoAction " + type + " " + taintedAtom + " modelkit?"
    //    + modelSet.models[modelIndex].isModelkit());
    //System.out.println(" " + type + " size=" + actionStates.size() + " "
    //    + +actionStatesRedo.size());
    switch (type) {
    case T.redomove:
    case T.undomove:
      // from MouseManager
      // CTRL-Z: type = 1 UNDO
      // CTRL-Y: type = -1 REDO
      viewer.stopMinimization();
      String s = "";
      JmolList<String> list1;
      JmolList<String> list2;
      switch (type) {
      default:
      case T.undomove:
        list1 = viewer.actionStates;
        list2 = viewer.actionStatesRedo;
        break;
      case T.redomove:
        list1 = viewer.actionStatesRedo;
        list2 = viewer.actionStates;
        if (viewer.actionStatesRedo.size() == 1)
          return;
        break;
      }
      if (list1.size() == 0 || undoWorking)
        return;
      undoWorking = true;
      list2.add(0, list1.remove(0));
      s = viewer.actionStatesRedo.get(0);
      if (type == T.undomove && list2.size() == 1) {
        // must save current state, coord, etc.
        // but this destroys actionStatesRedo
        int[] pt = new int[] { 1 };
        type = Parser.parseIntNext(s, pt);
        taintedAtom = Parser.parseIntNext(s, pt);
        undoMoveActionClear(taintedAtom, type, false);
      }
      //System.out.println("redo type = " + type + " size=" + actionStates.size()
      //    + " " + +actionStatesRedo.size());
      if (viewer.modelSet.models[modelIndex].isModelkit()
          || s.indexOf("zap ") < 0) {
        if (Logger.debugging)
          viewer.log(s);
        viewer.evalStringQuiet(s);
      } else {
        // if it's not modelkit mode and we are trying to do a zap, then ignore
        // and clear all action states.
        viewer.actionStates.clear();
      }
      break;
    default:
      if (undoWorking && clearRedo)
        return;
      undoWorking = true;
      BS bs;
      SB sb = new SB();
      sb.append("#" + type + " " + taintedAtom + " " + (new Date()) + "\n");
      if (taintedAtom >= 0) {
        bs = viewer.getModelUndeletedAtomsBitSet(modelIndex);
        viewer.modelSet.taintAtoms(bs, (byte) type);
        sb.append(getAtomicPropertyState((byte) -1, null));
      } else {
        bs = viewer.getModelUndeletedAtomsBitSet(modelIndex);
        sb.append("zap ");
        sb.append(Escape.e(bs)).append(";");
        DataManager.getInlineData(sb, viewer.getModelExtract(bs, false, true,
            "MOL"), true, null);
        sb.append("set refreshing false;").append(
            viewer.actionManager.getPickingState()).append(
            viewer.transformManager.getMoveToText(0, false)).append(
            "set refreshing true;");

      }
      if (clearRedo) {
        viewer.actionStates.add(0, sb.toString());
        viewer.actionStatesRedo.clear();
      } else {
        viewer.actionStatesRedo.add(1, sb.toString());
      }
      if (viewer.actionStates.size() == MAX_ACTION_UNDO) {
        viewer.actionStates.remove(MAX_ACTION_UNDO - 1);
      }
    }
    undoWorking = !clearRedo;
  }

  private boolean undoWorking = false;
  private final static int MAX_ACTION_UNDO = 100;

  void appendLoadStates(SB cmds) {
    Map<String, Boolean> ligandModelSet = viewer.ligandModelSet;
    if (ligandModelSet != null) {
      for (String key : ligandModelSet.keySet()) {
        String data = (String) viewer.ligandModels.get(key + "_data");
        if (data != null)
          cmds.append("  ").append(
              Escape.encapsulateData("ligand_" + key, data.trim() + "\n", 0));
      }
    }
    SB commands = new SB();
    ModelSet ms = viewer.modelSet;
    Model[] models = ms.models;
    int modelCount = ms.modelCount;
    for (int i = 0; i < modelCount; i++) {
      if (ms.isJmolDataFrameForModel(i) || ms.isTrajectorySubFrame(i))
        continue;
      Model m = models[i];
      int pt = commands.indexOf(m.loadState);
      if (pt < 0 || pt != commands.lastIndexOf(m.loadState))
        commands.append(models[i].loadState);
      if (models[i].isModelKit) {
        BS bs = ms.getModelAtomBitSetIncludingDeleted(i, false);
        if (ms.tainted != null) {
          if (ms.tainted[AtomCollection.TAINT_COORD] != null)
            ms.tainted[AtomCollection.TAINT_COORD].andNot(bs);
          if (ms.tainted[AtomCollection.TAINT_ELEMENT] != null)
            ms.tainted[AtomCollection.TAINT_ELEMENT].andNot(bs);
        }
        m.loadScript = new SB();
        Viewer.getInlineData(commands, viewer.getModelExtract(bs, false, true,
            "MOL"), i > 0);
      } else {
        commands.appendSB(m.loadScript);
      }
    }
    String s = commands.toString();
    // add a zap command before the first load command.
    int i = s.indexOf("load /*data*/");
    int j = s.indexOf("load /*file*/");
    if (j >= 0 && j < i)
      i = j;
    if ((j = s.indexOf("load \"@")) >= 0 && j < i)
      i = j;
    if (i >= 0)
      s = s.substring(0, i) + "zap;" + s.substring(i);
    cmds.append(s);
  }

  private String createSceneSet(String sceneFile, String type, int width,
                                int height) {
    String script0 = viewer.getFileAsString(sceneFile);
    if (script0 == null)
      return "no such file: " + sceneFile;
    sceneFile = TextFormat.simpleReplace(sceneFile, ".spt", "");
    String fileRoot = sceneFile;
    String fileExt = type.toLowerCase();
    String[] scenes = TextFormat.splitChars(script0, "pause scene ");
    Map<String, String> htScenes = new Hashtable<String, String>();
    JmolList<Integer> list = new  JmolList<Integer>();
    String script = JmolBinary.getSceneScript(scenes, htScenes, list);
    Logger.debug(script);
    script0 = TextFormat.simpleReplace(script0, "pause scene", "delay "
        + viewer.animationManager.lastFrameDelay + " # scene");
    String[] str = new String[] { script0, script, null };
    viewer.saveState("_scene0");
    int nFiles = 0;
    if (scenes[0] != "")
      viewer.zap(true, true, false);
    int iSceneLast = -1;
    for (int i = 0; i < scenes.length - 1; i++) {
      try {
        int iScene = list.get(i).intValue();
        if (iScene > iSceneLast)
          viewer.showString("Creating Scene " + iScene, false);
        viewer.eval.runScript(scenes[i]);
        if (iScene <= iSceneLast)
          continue;
        iSceneLast = iScene;
        str[2] = "all"; // full PNGJ
        String fileName = fileRoot + "_scene_" + iScene + ".all." + fileExt;
        String msg = (String) createImagePathCheck(fileName, "PNGJ", null,
            null, str, null, -1, width, height, null, false);
        str[0] = null; // script0 only saved in first file
        str[2] = "min"; // script only -- for fast loading
        fileName = fileRoot + "_scene_" + iScene + ".min." + fileExt;
        msg += "\n"
            + (String) createImagePathCheck(fileName, "PNGJ", null, null, str,
                null, -1, Math.min(width, 200), Math.min(height, 200), null,
                false);
        viewer.showString(msg, false);
        nFiles += 2;
      } catch (Exception e) {
        return "script error " + e.toString();
      }
    }
    try {
      viewer.eval.runScript(viewer.getSavedState("_scene0"));
    } catch (Exception e) {
      // ignore
    }
    return "OK " + nFiles + " files created";
  }

  public String createImageSet(String fileName, String type, String text,
                               byte[] bytes, String[] scripts, int quality,
                               int width, int height, BS bsFrames,
                               int nVibes, String[] fullPath) {
    if (bsFrames == null && nVibes == 0)
      return (String) createImagePathCheck(fileName, type, text, bytes,
          scripts, null, quality, width, height, fullPath, true);
    String info = "";
    int n = 0;
    fileName = getOutputFileNameFromDialog(fileName, quality);
    if (fullPath != null)
      fullPath[0] = fileName;
    if (fileName == null)
      return null;
    int ptDot = fileName.indexOf(".");
    if (ptDot < 0)
      ptDot = fileName.length();

    String froot = fileName.substring(0, ptDot);
    String fext = fileName.substring(ptDot);
    SB sb = new SB();
    if (bsFrames == null) {
      viewer.transformManager.vibrationOn = true;
      sb = new SB();
      for (int i = 0; i < nVibes; i++) {
        for (int j = 0; j < 20; j++) {
          viewer.transformManager.setVibrationT(j / 20f + 0.2501f);
          if (!writeFrame(++n, froot, fext, fullPath, type, quality, width,
              height, sb))
            return "ERROR WRITING FILE SET: \n" + info;
        }
      }
      viewer.setVibrationOff();
    } else {
      for (int i = bsFrames.nextSetBit(0); i >= 0; i = bsFrames
          .nextSetBit(i + 1)) {
        viewer.setCurrentModelIndex(i);
        if (!writeFrame(++n, froot, fext, fullPath, type, quality, width,
            height, sb))
          return "ERROR WRITING FILE SET: \n" + info;
      }
    }
    if (info.length() == 0)
      info = "OK\n";
    return info + "\n" + n + " files created";
  }

  private boolean writeFrame(int n, String froot, String fext,
                             String[] fullPath, String type, int quality,
                             int width, int height, SB sb) {
    String fileName = "0000" + n;
    fileName = froot + fileName.substring(fileName.length() - 4) + fext;
    if (fullPath != null)
      fullPath[0] = fileName;
    String msg = (String) createImagePathCheck(fileName, type, null, null,
        null, "", quality, width, height, null, false);
    viewer.scriptEcho(msg);
    sb.append(msg).append("\n");
    return msg.startsWith("OK");
  }

  /**
   * general routine for creating an image or writing data to a file
   * 
   * passes request to statusManager to pass along to app or applet
   * jmolStatusListener interface
   * 
   * @param fileName
   *        starts with ? --> use file dialog; null --> to clipboard
   * @param type
   *        PNG, JPG, etc.
   * @param text
   *        String to output
   * @param bytes
   *        byte[] or null if an image
   * @param scripts
   * @param appendix
   *        byte[] or String
   * @param quality
   *        Integer.MIN_VALUE --> not an image
   * @param width
   *        image width
   * @param height
   *        image height
   * @param fullPath
   * @param doCheck
   * @return null (canceled) or a message starting with OK or an error message
   */
  public Object createImagePathCheck(String fileName, String type, String text,
                                     byte[] bytes, String[] scripts,
                                     Object appendix, int quality, int width,
                                     int height, String[] fullPath,
                                     boolean doCheck) {

    /*
     * 
     * org.jmol.export.image.AviCreator does create AVI animations from Jpegs
     * but these aren't read by standard readers, so that's pretty much useless.
     * 
     * files must have the designated width and height
     * 
     * text_or_bytes: new Object[] { (File[]) files, (String) outputFilename,
     * (int[]) params }
     * 
     * where for now we just read param[0] as frames per second
     * 
     * 
     * Note: this method is the gateway to all file writing for the applet.
     */

    if (type.equals("JMOL"))
      type = "ZIPALL";
    int saveWidth = viewer.dimScreen.width;
    int saveHeight = viewer.dimScreen.height;
    viewer.creatingImage = true;
    if (quality != Integer.MIN_VALUE) {
      viewer.mustRender = true;
      viewer.resizeImage(width, height, true, false, false);
      viewer.setModelVisibility();
    }
    Object err = null;

    try {
      if (fileName == null) {
        err = viewer.clipImage(text);
      } else {
        if (doCheck)
          fileName = getOutputFileNameFromDialog(fileName, quality);
        if (fullPath != null)
          fullPath[0] = fileName;
        String localName = (!viewer.isJS && FileManager.isLocal(fileName) ? fileName
            : null);
        if (fileName == null) {
          err = "CANCELED";
        } else if (type.equals("ZIP") || type.equals("ZIPALL")) {
          if (scripts != null && type.equals("ZIP"))
            type = "ZIPALL";
          err = JmolBinary.createZipSet(viewer.fileManager, viewer, localName,
              text, scripts, type.equals("ZIPALL"));
        } else if (type.equals("SCENE")) {
          err = (viewer.isJS ? "ERROR: Not Available" : createSceneSet(
              fileName, text, width, height));
        } else {
          // see if application wants to do it (returns non-null String)
          // both Jmol application and applet return null
          if (!type.equals("OutputStream"))
            err = viewer.statusManager.createImage(fileName, type, text, bytes,
                quality);
          if (err == null) {
            // application can do it itself or allow Jmol to do it here
            JmolImageCreatorInterface c = viewer.getImageCreator();
            err = c.createImage(localName, type, text, bytes, scripts, null,
                quality);
            if (err instanceof String)
              // report error status (text_or_bytes == null)
              viewer.statusManager.createImage((String) err, type, null, null,
                  quality);
          }
        }
        if (err instanceof byte[]) {
          err = JmolBinary.postByteArray(viewer.fileManager, fileName,
              (byte[]) err);
          err = "OK " + err;
        }
      }
    } catch (Throwable er) {
      //er.printStackTrace();
      Logger.error(viewer.setErrorMessage(
          (String) (err = "ERROR creating image??: " + er), null));
    }
    viewer.creatingImage = false;
    if (quality != Integer.MIN_VALUE) {
      viewer.resizeImage(saveWidth, saveHeight, true, false, true);
    }
    return ("CANCELED".equals(err) ? null : err);
  }

  public void syncScript(String script, String applet, int port) {
    StatusManager sm = viewer.statusManager;
    if (Viewer.SYNC_GRAPHICS_MESSAGE.equalsIgnoreCase(script)) {
      sm.setSyncDriver(StatusManager.SYNC_STEREO);
      sm.syncSend(script, applet, 0);
      viewer.setBooleanProperty("_syncMouse", false);
      viewer.setBooleanProperty("_syncScript", false);
      return;
    }
    // * : all applets
    // > : all OTHER applets
    // . : just me
    // ~ : disable send (just me)
    // = : disable send (just me) and force slave
    if ("=".equals(applet)) {
      applet = "~";
      sm.setSyncDriver(StatusManager.SYNC_SLAVE);
    }
    boolean disableSend = "~".equals(applet);
    // null same as ">" -- "all others"
    if (port > 0 || !disableSend && !".".equals(applet)) {
      sm.syncSend(script, applet, port);
      if (!"*".equals(applet) || script.startsWith("{"))
        return;
    }
    if (script.equalsIgnoreCase("on") || script.equalsIgnoreCase("true")) {
      sm.setSyncDriver(StatusManager.SYNC_DRIVER);
      return;
    }
    if (script.equalsIgnoreCase("off") || script.equalsIgnoreCase("false")) {
      sm.setSyncDriver(StatusManager.SYNC_OFF);
      return;
    }
    if (script.equalsIgnoreCase("slave")) {
      sm.setSyncDriver(StatusManager.SYNC_SLAVE);
      return;
    }
    int syncMode = sm.getSyncMode();
    if (syncMode == StatusManager.SYNC_OFF)
      return;
    if (syncMode != StatusManager.SYNC_DRIVER)
      disableSend = false;
    if (Logger.debugging)
      Logger.debug(viewer.htmlName + " syncing with script: " + script);
    // driver is being positioned by another driver -- don't pass on the change
    // driver is being positioned by a mouse movement
    // format is from above refresh(2, xxx) calls
    // Mouse: [CommandName] [value1] [value2]
    if (disableSend)
      sm.setSyncDriver(StatusManager.SYNC_DISABLE);
    if (script.indexOf("Mouse: ") != 0) {
      if (script.startsWith("Select: ")) {
        String filename = Parser.getQuotedAttribute(script, "file");
        String modelID = Parser.getQuotedAttribute(script, "model");
        String baseModel = Parser.getQuotedAttribute(script, "baseModel");
        String atoms = Parser.getQuotedAttribute(script, "atoms");
        String select = Parser.getQuotedAttribute(script, "select");
        String script2 = Parser.getQuotedAttribute(script, "script");
        boolean isNIH = (modelID != null && modelID.startsWith("$"));
        if (isNIH)
          filename = (modelID.substring(1).equals(
              viewer.getParameter("_smilesstring")) ? null : modelID);
        String id = (isNIH || modelID == null ? null : (filename == null ? ""
            : filename + "#")
            + modelID);
        if ("".equals(baseModel))
          id += ".baseModel";
        int modelIndex = (id == null ? -3 : viewer.getModelIndexFromId(id));
        if (modelIndex == -2)
          return; // file was found, or no file was indicated, but not this model -- ignore
        script = (modelIndex == -1 && filename != null ? script = "load "
            + Escape.eS(filename) : "");
        if (id != null)
          script += ";model " + Escape.eS(id);
        if (atoms != null)
          script += ";select visible & (@"
              + TextFormat.simpleReplace(atoms, ",", " or @") + ")";
        else if (select != null)
          script += ";select visible & (" + select + ")";
        if (script2 != null)
          script += ";" + script2;
      } else if (script.toLowerCase().startsWith("jspecview")) {
        if (!disableSend)
          sm.syncSend(viewer.fullName + "JSpecView" + script.substring(9), ">",
              0);
        return;
      }
      //System.out.println("Jmol executing script for JSpecView: " + script);
      viewer.evalStringQuietSync(script, true, false);
      return;
    }
    quickScript(script);
    if (disableSend)
      viewer.setSyncDriver(StatusManager.SYNC_ENABLE);
  }

  public void quickScript(String script) {
    String[] tokens = Parser.getTokens(script);
    String key = tokens[1];
    switch (tokens.length) {
    case 3:
      if (key.equals("zoomByFactor"))
        viewer.zoomByFactor(Parser.parseFloatStr(tokens[2]), Integer.MAX_VALUE,
            Integer.MAX_VALUE);
      else if (key.equals("zoomBy"))
        viewer.zoomBy(Parser.parseInt(tokens[2]));
      else if (key.equals("rotateZBy"))
        viewer.rotateZBy(Parser.parseInt(tokens[2]), Integer.MAX_VALUE,
            Integer.MAX_VALUE);
      break;
    case 4:
      if (key.equals("rotateXYBy"))
        viewer.rotateXYBy(Parser.parseFloatStr(tokens[2]), Parser
            .parseFloatStr(tokens[3]));
      else if (key.equals("translateXYBy"))
        viewer.translateXYBy(Parser.parseInt(tokens[2]), Parser
            .parseInt(tokens[3]));
      else if (key.equals("rotateMolecule"))
        viewer.rotateSelected(Parser.parseFloatStr(tokens[2]), Parser
            .parseFloatStr(tokens[3]), null);
      break;
    case 5:
      if (key.equals("spinXYBy"))
        viewer.spinXYBy(Parser.parseInt(tokens[2]), Parser.parseInt(tokens[3]),
            Parser.parseFloatStr(tokens[4]));
      else if (key.equals("zoomByFactor"))
        viewer.zoomByFactor(Parser.parseFloatStr(tokens[2]), Parser
            .parseInt(tokens[3]), Parser.parseInt(tokens[4]));
      else if (key.equals("rotateZBy"))
        viewer.rotateZBy(Parser.parseInt(tokens[2]),
            Parser.parseInt(tokens[3]), Parser.parseInt(tokens[4]));
      else if (key.equals("rotateArcBall"))
        viewer.rotateArcBall(Parser.parseInt(tokens[2]), Parser
            .parseInt(tokens[3]), Parser.parseFloatStr(tokens[4]));
      break;
    case 7:
      if (key.equals("centerAt"))
        viewer.centerAt(Parser.parseInt(tokens[2]), Parser.parseInt(tokens[3]),
            P3.new3(Parser.parseFloatStr(tokens[4]), Parser
                .parseFloatStr(tokens[5]), Parser.parseFloatStr(tokens[6])));
      break;
    }
  }

  public String generateOutputForExport(String type, String[] fileName,
                                        int width, int height) {
    String fName = null;
    if (fileName != null) {
      fileName[0] = getOutputFileNameFromDialog(fileName[0], Integer.MIN_VALUE);
      if (fileName[0] == null)
        return null;
      fName = fileName[0];
    }
    viewer.mustRender = true;
    int saveWidth = viewer.dimScreen.width;
    int saveHeight = viewer.dimScreen.height;
    viewer.resizeImage(width, height, true, true, false);
    viewer.setModelVisibility();
    String data = viewer.repaintManager.renderExport(type, viewer.gdata,
        viewer.modelSet, fName);
    // mth 2003-01-09 Linux Sun JVM 1.4.2_02
    // Sun is throwing a NullPointerExceptions inside graphics routines
    // while the window is resized.
    viewer.resizeImage(saveWidth, saveHeight, true, true, true);
    return data;
  }

  private String getOutputFileNameFromDialog(String fileName, int quality) {
    if (fileName == null || viewer.isKiosk)
      return null;
    boolean useDialog = (fileName.indexOf("?") == 0);
    if (useDialog)
      fileName = fileName.substring(1);
    useDialog |= viewer.isApplet && (fileName.indexOf("http:") < 0);
    fileName = FileManager.getLocalPathForWritingFile(viewer, fileName);
    if (useDialog)
      fileName = viewer.dialogAsk(quality == Integer.MIN_VALUE ? "save"
          : "saveImage", fileName);
    return fileName;
  }

  public Object getImageAsWithComment(String type, int quality, int width,
                                      int height, String fileName,
                                      String[] scripts, OutputStream os,
                                      String comment) {
    int saveWidth = viewer.dimScreen.width;
    int saveHeight = viewer.dimScreen.height;
    viewer.mustRender = true;
    viewer.resizeImage(width, height, true, false, false);
    viewer.setModelVisibility();
    viewer.creatingImage = true;
    JmolImageCreatorInterface c = null;
    Object bytes = null;
    type = type.toLowerCase();
    if (!Parser.isOneOf(type, "jpg;jpeg;jpg64;jpeg64"))
      try {
        c = viewer.getImageCreator();
      } catch (Error er) {
        // unsigned applet will not have this interface
        // and thus will not use os or filename
      }
    if (c == null) {
      try {
        bytes = viewer.apiPlatform.getJpgImage(viewer, quality, comment);
        if (type.equals("jpg64") || type.equals("jpeg64"))
          bytes = (bytes == null ? "" : Base64.getBase64((byte[]) bytes)
              .toString());
      } catch (Error er) {
        viewer.releaseScreenImage();
        viewer.handleError(er, false);
        viewer.setErrorMessage("Error creating image: " + er, null);
        bytes = viewer.getErrorMessage();
      }
    } else {
      try {
        bytes = c.getImageBytes(type, quality, fileName, scripts, null, null,
            os);
      } catch (IOException e) {
        bytes = e;
        viewer.setErrorMessage("Error creating image: " + e, null);
      } catch (Error er) {
        viewer.handleError(er, false);
        viewer.setErrorMessage("Error creating image: " + er, null);
        bytes = viewer.getErrorMessage();
      }
    }
    viewer.creatingImage = false;
    viewer.resizeImage(saveWidth, saveHeight, true, false, true);
    return bytes;
  }

  public String streamFileData(String fileName, String type, String type2,
                               int modelIndex, Object[] parameters) {
    String msg = null;
    String[] fullPath = new String[1];
    OutputStream os = getOutputStream(fileName, fullPath);
    if (os == null)
      return "";
    OutputStringBuilder sb;
    if (type.equals("PDB") || type.equals("PQR")) {
      sb = new OutputStringBuilder(new BufferedOutputStream(os));
      sb.type = type;
      msg = viewer.getPdbData(null, sb);
    } else if (type.equals("FILE")) {
      msg = writeCurrentFile(os);
      // quality = Integer.MIN_VALUE;
    } else if (type.equals("PLOT")) {
      sb = new OutputStringBuilder(new BufferedOutputStream(os));
      msg = viewer.modelSet.getPdbData(modelIndex, type2, viewer
          .getSelectionSet(false), parameters, sb);
    }
    if (msg != null)
      msg = "OK " + msg + " " + fullPath[0];
    try {
      os.flush();
      os.close();
    } catch (IOException e) {
      // TODO
    }
    return msg;
  }

  private String writeCurrentFile(OutputStream os) {
    String filename = viewer.getFullPathName();
    if (filename.equals("string") || filename.indexOf("[]") >= 0
        || filename.equals("JSNode")) {
      String str = viewer.getCurrentFileAsString();
      BufferedOutputStream bos = new BufferedOutputStream(os);
      OutputStringBuilder sb = new OutputStringBuilder(bos);
      sb.append(str);
      return sb.toString();
    }
    String pathName = viewer.getModelSetPathName();
    return (pathName == null ? "" : (String) viewer
        .getFileAsBytes(pathName, os));
  }

  public OutputStream getOutputStream(String localName, String[] fullPath) {
    if (!viewer.isRestricted(ACCESS.ALL))
      return null;
    Object ret = createImagePathCheck(localName, "OutputStream", null, null,
        null, null, Integer.MIN_VALUE, 0, 0, fullPath, true);
    if (ret instanceof String) {
      Logger.error((String) ret);
      return null;
    }
    return (OutputStream) ret;
  }

  public void openFileAsync(String fileName, boolean pdbCartoons) {
    fileName = fileName.trim();
    boolean allowScript = (!fileName.startsWith("\t"));
    if (!allowScript)
      fileName = fileName.substring(1);
    fileName = fileName.replace('\\', '/');
    if (viewer.isApplet && fileName.indexOf("://") < 0)
      fileName = "file://" + (fileName.startsWith("/") ? "" : "/") + fileName;
    if (fileName.endsWith(".pse")) {
      viewer.evalString("zap;load SYNC " + Escape.eS(fileName)
          + " filter 'DORESIZE'");
      return;
    }
    String cmd = null;
    if (fileName.endsWith("jvxl")) {
      cmd = "isosurface ";
    } else if (!fileName.endsWith(".spt")) {
      String type = viewer.fileManager.getFileTypeName(fileName);
      if (type == null) {
        type = JmolBinary.determineSurfaceTypeIs(viewer
            .getBufferedInputStream(fileName));
        if (type != null) {
          viewer
              .evalString("if (_filetype == 'Pdb') { isosurface sigma 1.0 within 2.0 {*} "
                  + Escape.eS(fileName)
                  + " mesh nofill }; else; { isosurface "
                  + Escape.eS(fileName) + "}");
          return;
        }
      } else if (type.equals("Jmol")) {
        cmd = "load ";
      } else if (type.equals("Cube")) {
        cmd = "isosurface sign red blue ";
      } else if (!type.equals("spt")) {
        cmd = viewer.global.defaultDropScript;
        cmd = TextFormat.simpleReplace(cmd, "%FILE", fileName);
        cmd = TextFormat.simpleReplace(cmd, "%ALLOWCARTOONS", "" + pdbCartoons);
        viewer.evalString(cmd);
        return;
      }
    }
    if (allowScript && viewer.scriptEditorVisible && cmd == null)
      showEditor(new String[] { fileName, viewer.getFileAsString(fileName) });
    else
      viewer.evalString((cmd == null ? "script " : cmd)
          + Escape.eS(fileName));
  }

  public void showEditor(String[] file_text) {
    if (file_text == null)
      file_text = new String[] { null, null };
    if (file_text[1] == null)
      file_text[1] = "<no data>";
    String filename = file_text[0];
    String msg = file_text[1];
    JmolScriptEditorInterface scriptEditor = (JmolScriptEditorInterface) viewer
        .getProperty("DATA_API", "getScriptEditor", Boolean.TRUE);
    if (scriptEditor == null)
      return;
    if (msg != null) {
      scriptEditor.setFilename(filename);
      scriptEditor.output(JmolBinary.getEmbeddedScript(msg));
    }
    scriptEditor.setVisible(true);
  }

  public void log(String data) {
    try {
      boolean doClear = (data.equals("$CLEAR$"));
      if (data.indexOf("$NOW$") >= 0)
        data = TextFormat.simpleReplace(data, "$NOW$", (new Date()).toString());
      if (viewer.logFile == null) {
        System.out.println(data);
        return;
      }
      FileWriter fstream = new FileWriter(viewer.logFile, !doClear);
      BufferedWriter out = new BufferedWriter(fstream);
      if (!doClear) {
        int ptEnd = data.indexOf('\0'); 
        if (ptEnd >= 0)
          data = data.substring(0, ptEnd);
        out.write(data);
        if (ptEnd < 0)
          out.write('\n');
      }
      out.close();
    } catch (Exception e) {
      Logger.debug("cannot log " + data);
    }
  }
  
  public String getAtomDefs(Map<String, Object> names) {
    SB sb = new SB();
    for (Map.Entry<String, Object> e : names.entrySet()) {
      if (e.getValue() instanceof BS)
        sb.append("{" + e.getKey() + "} <" + ((BS) e.getValue()).cardinality()
            + " atoms>\n");
    }
    return sb.append("\n").toString();
  }

}
