/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-15 07:52:29 -0600 (Wed, 15 Mar 2006) $
 * $Revision: 4614 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.readers.more;

import java.io.BufferedReader;
import java.io.StringReader;
import org.jmol.util.JmolList;



import org.jmol.util.TextFormat;

import org.jmol.adapter.readers.molxyz.MolReader;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Bond;
import org.jmol.adapter.smarter.AtomSetCollection;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.SmarterJmolAdapter;

import org.jmol.util.BS;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.SB;

/**
 * A preliminary reader for JCAMP-DX files having ##$MODELS= and ##$PEAKS= records
 * 
 * Designed by Robert Lancashire and Bob Hanson
 * 
 * specifications (by example here):

##$MODELS=
<Models>
 <ModelData id="acetophenone" type="MOL">
acetophenone
  DSViewer          3D                             0

 17 17  0  0  0  0  0  0  0  0999 V2000
...
 17 14  1  0  0  0
M  END
  </ModelData>
 <ModelData id="irvibs" type="XYZVIB" baseModel="acetophenone" vibrationScale="0.1">
17
1  Energy: -1454.38826  Freq: 3199.35852
C    -1.693100    0.007800    0.000000   -0.000980    0.000120    0.000000
...
  </ModelData>
</Models>

-- All XML data should be line-oriented in the above fashion. Leading spaces will be ignored.
-- Any number of <ModelData> segments can be present
-- The first model is referred to as the "base" model
-- The base model:
   -- will generally be of type MOL, but any known type is acceptable
   -- will be used to generate bonding for later models that have no bonding information
   -- will be the only model for NMR
-- Additional models can represent vibrations (XYZ format) or MS fragmentation (MOL format, probably)

##$PEAKS=
<Peaks type="IR" xUnits="1/cm" yUnits="TRANSMITTANCE" >
<PeakData id="1" title="asymm stretch of aromatic CH group (~3100 cm-1)" peakShape="broad" model="irvibs.1"  xMax="3121" xMin="3081"  yMax="1" yMin="0" />
<PeakData id="2" title="symm stretch of aromatic CH group (~3085 cm-1)" peakShape="broad" model="irvibs.2"  xMax="3101" xMin="3071"  yMax="1" yMin="0" />
...
</Peaks>

-- peak record must be a single line of information because
   Jmol will use line.trim() as a key to pass information to JSpecView. 

 * 
 *<p>
 */

public class JcampdxReader extends MolReader {

  private String modelID;
  private AtomSetCollection models;
  private String modelIdList = "";
  private  JmolList<String> peakData = new  JmolList<String>();
  private String lastModel = "";
  private int selectedModel;
  private int[] peakIndex;
  private String peakFilePath;
  
  
  @Override
  public void initializeReader() throws Exception {
    // trajectories would be OK for IR, but just too complicated for others.
    
    // tells Jmol to start talking with JSpecView
    
    viewer.setBooleanProperty("_jspecview", true);
    if (isTrajectory) {
      Logger.warn("TRAJECTORY keyword ignored");
      isTrajectory = false;
    }
    // forget reversing models!
    if (reverseModels) {
      Logger.warn("REVERSE keyword ignored");
      reverseModels = false;
    }
    selectedModel = desiredModelNumber;
    desiredModelNumber = Integer.MIN_VALUE;
    peakFilePath = Escape.eS(filePath);
    htParams.remove("modelNumber");
    // peakIndex will be passed on to additional files in a ZIP file load
    // the peak file path is stripped of the "|xxxx.jdx" part 
    if (htParams.containsKey("zipSet")) {
      peakIndex = (int[]) htParams.get("peakIndex");
      if (peakIndex == null) {
        peakIndex = new int[1];
        htParams.put("peakIndex", peakIndex);
      }
      if (!htParams.containsKey("subFileName"))
        peakFilePath = Escape.eS(TextFormat.split(filePath, '|')[0]);
    } else {
      peakIndex = new int[1];
    }
    if (!checkFilterKey("NOSYNC"))
      addJmolScript("sync on");
  }

  @Override
  public boolean checkLine() throws Exception {
    int i = line.indexOf("=");
    if (i < 0 || !line.startsWith("##"))
      return true; 
    String label = line.substring(0, i).trim();
    if (label.equals("##$MODELS"))
      return readModels();
    if (label.equals("##$PEAKS"))
      return readPeaks();
    return true;
  }

  @Override
  public void finalizeReader() throws Exception {
    processPeakData();
    super.finalizeReader();
  }
  
  private int findModelById(String modelID) {
    for (int i = atomSetCollection.getAtomSetCount(); --i >= 0;)
      if (modelID.equals(atomSetCollection
          .getAtomSetAuxiliaryInfoValue(i, "modelID")))
        return i;
    return -1;
  }

  private boolean readModels() throws Exception {
    if (line.indexOf("<Models") < 0) {
      discardLinesUntilContains2("<Models", "##");
      if (line.indexOf("<Models") < 0)
        return false;
    }
    // if load xxx.jdx n  then we must temporarily set n to 1 for the base model reading
    // load xxx.jdx 0  will mean "load only the base model(s)"
    models = null;
    line = "";
    modelID = "";
    boolean isFirst = true;
    while (true) {
      int model0 = atomSetCollection.getCurrentAtomSetIndex();
      discardLinesUntilNonBlank();
      if (line == null || !line.contains("<ModelData"))
        break;
      models = getModelAtomSetCollection();
      if (models != null) {
        atomSetCollection.appendAtomSetCollection(-1, models);
      }
      updateModelIDs(model0, isFirst);
      isFirst = false;
    }
    return true;
  }

  /**
   * The first model set is allowed to be a single model and given no extension.
   * All other model sets are given .1 .2 .3 ... extensions to their IDs.
   *   
   * 
   * @param model0
   * @param isFirst
   */
  private void updateModelIDs(int model0, boolean isFirst) {
    int n = atomSetCollection.getAtomSetCount();
    if (isFirst && n == model0 + 2) {
      atomSetCollection.setAtomSetAuxiliaryInfo("modelID", modelID);
      return;
    }
    for (int pt = 0, i = model0; ++i < n;) {
      atomSetCollection.setAtomSetAuxiliaryInfoForSet("modelID", modelID + "."
          + (++pt), i);
    }
  }

  private static String getAttribute(String line, String tag) {
    String attr = Parser.getQuotedAttribute(line, tag);
    return (attr == null ? "" : attr);
  }

  private AtomSetCollection getModelAtomSetCollection() throws Exception {
    lastModel = modelID;
    modelID = getAttribute(line, "id");
    // read model only once for a given ID
    String key = ";" + modelID + ";";
    if (modelIdList.indexOf(key) >= 0) {
      discardLinesUntilContains("</ModelData>");
      return null;
    }
    modelIdList += key;
    String baseModel = getAttribute(line, "baseModel");
    String modelType = getAttribute(line, "type").toLowerCase();
    float vibScale = Parser.parseFloatStr(getAttribute(line, "vibrationScale"));
    if (modelType.equals("xyzvib"))
      modelType = "xyz";
    else if (modelType.length() == 0)
      modelType = null; // let Jmol set the type
    SB sb = new SB();
    while (readLine() != null && !line.contains("</ModelData>"))
      sb.append(line).appendC('\n');
    String data = sb.toString();
    Object ret = SmarterJmolAdapter.staticGetAtomSetCollectionReader(filePath,
        modelType, new BufferedReader(new StringReader(data)), htParams);
    if (ret instanceof String) {
      Logger.warn("" + ret);
      return null;
    }
    ret = SmarterJmolAdapter
        .staticGetAtomSetCollection((AtomSetCollectionReader) ret);
    if (ret instanceof String) {
      Logger.warn("" + ret);
      return null;
    }
    AtomSetCollection a = (AtomSetCollection) ret;
    if (baseModel.length() == 0)
      baseModel = lastModel;
    if (baseModel.length() != 0) {
      int ibase = findModelById(baseModel);
      if (ibase >= 0) {
        atomSetCollection
            .setAtomSetAuxiliaryInfoForSet("jdxModelID", baseModel, ibase);
        for (int i = a.getAtomSetCount(); --i >= 0;)
          a.setAtomSetAuxiliaryInfoForSet("jdxBaseModel", baseModel, i);
        if (a.getBondCount() == 0)
          setBonding(a, ibase);
      }
    }
    if (!Float.isNaN(vibScale)) {
      Logger.info("jdx applying vibrationScale of " + vibScale + " to "
          + a.getAtomCount() + " atoms");
      Atom[] atoms = a.getAtoms();
      for (int i = a.getAtomCount(); --i >= 0;)
        atoms[i].scaleVector(vibScale);
    }
    Logger.info("jdx model=" + modelID + " type=" + a.getFileTypeName());
    return a;
  }

  /**
   * add bonding to a set of ModelData based on a MOL file
   * only if the this set has no bonding already
   * 
   * @param a
   * @param ibase 
   */
  private void setBonding(AtomSetCollection a, int ibase) {
    int n0 = atomSetCollection.getAtomSetAtomCount(ibase);
    int n = a.getAtomCount();
    if (n % n0 != 0) {
      Logger.warn("atom count in secondary model (" + n + ") is not a multiple of " + n0 + " -- bonding ignored");
      return;
    }
    Bond[] bonds = atomSetCollection.getBonds();
    int b0 = 0;
    for (int i = 0; i < ibase; i++)
      b0 += atomSetCollection.getAtomSetBondCount(i);
    int b1 = b0 + atomSetCollection.getAtomSetBondCount(ibase);
    int ii0 = atomSetCollection.getAtomSetAtomIndex(ibase);
    int nModels = a.getAtomSetCount();
    for (int j = 0; j < nModels; j++) {
      int i0 = a.getAtomSetAtomIndex(j) - ii0;
      if (a.getAtomSetAtomCount(j) != n0) {
        Logger.warn("atom set atom count in secondary model (" + a.getAtomSetAtomCount(j) + ") is not equal to " + n0 + " -- bonding ignored");
        return;
      }      
      for (int i = b0; i < b1; i++) 
        a.addNewBondWithOrder(bonds[i].atomIndex1 + i0, bonds[i].atomIndex2 + i0, bonds[i].order);      
    }
  }

  private boolean readPeaks() throws Exception {
    if (line.indexOf("<Peaks") < 0)
      discardLinesUntilContains2("<Peaks", "##");
    if (line.indexOf("<Peaks") < 0)
      return false;
    String type = getAttribute(line, "type").toUpperCase();
    if (type.equals("HNMR"))
      type = "1HNMR";
    else if (type.equals("CNMR"))
      type = "13CNMR";
    while (readLine() != null && !(line = line.trim()).startsWith("</Peaks>"))
      if (line.startsWith("<PeakData"))
        peakData.addLast("<PeakData file=" + peakFilePath + " index=\"" + (++peakIndex[0]) + "\"" + " type=\"" + type + "\" " + line.substring(9).trim());
    return true;
  }

  /**
   * integrate the <PeakAssignment> records into the associated models, and delete unreferenced n.m models
   */
  @SuppressWarnings("unchecked")
  private void processPeakData() {
    if (peakData.size() == 0)
      return;
    BS bsModels = new BS();
    int n = peakData.size();
    boolean havePeaks = (n > 0);
    for (int p = 0; p < n; p++) {
      line = peakData.get(p);
      String type = getAttribute(line, "type");
      modelID = getAttribute(line, "model");
      int i = findModelById(modelID);
      if (i < 0) {
        Logger.warn("cannot find model " + modelID + " required for " + line);
        continue;
      }
      addType(i, type);
      String title = type + ": " + getAttribute(line, "title");
      String key = "jdxAtomSelect_" + getAttribute(line, "type");
      bsModels.set(i);      
      String s;
      if (getAttribute(line, "atoms").length() != 0) {
        JmolList<String> peaks = (JmolList<String>) atomSetCollection
            .getAtomSetAuxiliaryInfoValue(i, key);
        if (peaks == null)
          atomSetCollection.setAtomSetAuxiliaryInfoForSet(key,
              peaks = new  JmolList<String>(), i);
        peaks.addLast(line);
        s = type + ": ";
      } else if (atomSetCollection.getAtomSetAuxiliaryInfoValue(i, "jdxModelSelect") == null) {
        // assign name and jdxModelSelect ONLY if first found.
        atomSetCollection.setAtomSetAuxiliaryInfoForSet("name", title, i);
        atomSetCollection.setAtomSetAuxiliaryInfoForSet("jdxModelSelect", line, i);
        s = "model: ";
      } else {
        s = "ignored: ";
      }
      Logger.info(s + line);
    }
    n = atomSetCollection.getAtomSetCount();
    for (int i = n; --i >= 0;) {
      modelID = (String) atomSetCollection
          .getAtomSetAuxiliaryInfoValue(i, "modelID");
      if (havePeaks && !bsModels.get(i) && modelID.indexOf(".") >= 0) {
        atomSetCollection.removeAtomSet(i);
        n--;
      }
    }
    if (selectedModel == Integer.MIN_VALUE) {
      if (allTypes != null)
        appendLoadNote(allTypes);
    } else {
      if (selectedModel == 0)
        selectedModel = n - 1;
      for (int i = atomSetCollection.getAtomSetCount(); --i >= 0;)
        if (i + 1 != selectedModel)
          atomSetCollection.removeAtomSet(i);
      if (n > 0)
        appendLoadNote((String) atomSetCollection.getAtomSetAuxiliaryInfoValue(0, "name"));
    }
    for (int i = atomSetCollection.getAtomSetCount(); --i >= 0;)
      atomSetCollection.setAtomSetNumber(i, i + 1);
    atomSetCollection.centralize();
  }

  private String allTypes;
  /**
   * sets an auxiliaryInfo string to "HNMR 13CNMR" or "IR" or "MS" 
   *
   * @param imodel
   * @param type
   */
  private void addType(int imodel, String type) {
    String types = addType((String) atomSetCollection.getAtomSetAuxiliaryInfoValue(imodel, "spectrumTypes"), type);
    if (types == null)
      return;
    atomSetCollection.setAtomSetAuxiliaryInfoForSet("spectrumTypes", types, imodel);
    String s = addType(allTypes, type);
    if (s != null)
      allTypes = s;
  }

  private String addType(String types, String type) {    
    if (types != null && types.contains(type))
      return null;
    if (types == null)
      types = "";
    else
      types += ",";
    return types + type;
  }
  
  
}
