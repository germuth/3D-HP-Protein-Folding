/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-02 11:48:43 -0500 (Wed, 02 Aug 2006) $
 * $Revision: 5364 $
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
package org.jmol.adapter.readers.xml;

import org.jmol.adapter.smarter.Atom;
import org.jmol.api.Interface;
import org.jmol.api.VolumeDataInterface;

import org.jmol.util.JmolList;
import java.util.Hashtable;

import java.util.Map;

import org.jmol.util.Logger;

/**
 * An chem3d c3xml reader
 */

public class XmlChem3dReader extends XmlReader {

  private JmolList<Map<String, Object>> orbitals = new  JmolList<Map<String, Object>>();

  XmlChem3dReader() {
  }

  @Override
  protected String[] getDOMAttributes() {
    return new String[] { "id", //general 
      "symbol", "cartCoords", //atoms
      "bondAtom1", "bondAtom2", "bondOrder", //bond
      "gridDatXDim", "gridDatYDim", "gridDatZDim",    
      "gridDatXSize", "gridDatYSize", "gridDatZSize",    
      "gridDatOrigin", "gridDatData",   // grid cube data
      "calcPartialCharges", "calcAtoms" // electronicStructureCalculation 
    };
  }
  
  @Override
  protected void processXml(XmlReader parent,
                            Object saxReader) throws Exception {
    super.processXml(parent, saxReader);
    setMOData(moData);
  }

  @Override
  public void processStartElement(String localName) {
    String[] tokens;
    //System.out.println("xmlchem3d: start " + localName);
    if ("model".equals(localName)) {
      atomSetCollection.newAtomSet();
      return;
    }

    if ("atom".equals(localName)) {
      atom = new Atom();
      atom.atomName = atts.get("id");
      atom.elementSymbol = atts.get("symbol");
      if (atts.containsKey("cartCoords")) {
        String xyz = atts.get("cartCoords");
        tokens = getTokensStr(xyz);
        atom.set(parseFloatStr(tokens[0]), parseFloatStr(tokens[1]),
            parseFloatStr(tokens[2]));
      }
      return;
    }
    if ("bond".equals(localName)) {
      String atom1 = atts.get("bondAtom1");
      String atom2 = atts.get("bondAtom2");
      int order = 1;
      if (atts.containsKey("bondOrder"))
        order = parseIntStr(atts.get("bondOrder"));
      atomSetCollection.addNewBondFromNames(atom1, atom2, order);
      return;
    }

    if ("electronicStructureCalculation".equalsIgnoreCase(localName)) {
      tokens = getTokensStr(atts.get("calcPartialCharges"));
      String[] tokens2 = getTokensStr(atts.get("calcAtoms"));
      for (int i = parseIntStr(tokens[0]); --i >= 0;)
        atomSetCollection.mapPartialCharge(tokens2[i + 1],
            parseFloatStr(tokens[i + 1]));
    }

    if ("gridData".equalsIgnoreCase(localName)) {
      int nPointsX = parseIntStr(atts.get("gridDatXDim"));
      int nPointsY = parseIntStr(atts.get("gridDatYDim"));
      int nPointsZ = parseIntStr(atts.get("gridDatZDim"));
      float xStep = parseFloatStr(atts.get("gridDatXSize")) / (nPointsX);
      float yStep = parseFloatStr(atts.get("gridDatYSize")) / (nPointsY);
      float zStep = parseFloatStr(atts.get("gridDatZSize")) / (nPointsZ);
      tokens = getTokensStr(atts.get("gridDatOrigin"));
      float ox = parseFloatStr(tokens[0]);
      float oy = parseFloatStr(tokens[1]);
      float oz = parseFloatStr(tokens[2]);

      tokens = getTokensStr(atts.get("gridDatData"));
      int pt = 1;
      float[][][] voxelData = new float[nPointsX][nPointsY][nPointsZ];

      /* from adeptscience:
       *
       * 
      "Here is what we can tell you:

      In Chem3D, all grid data in following format:

      for (int z = 0; z < ZDim; z++)
      for (int y = 0; y < YDim; y++)
      for (int x = 0; x < XDim; x++)"
      
       */

      float sum = 0;
      for (int z = 0; z < nPointsZ; z++)
        for (int y = 0; y < nPointsY; y++)
          for (int x = 0; x < nPointsX; x++) {
            float f = parseFloatStr(tokens[pt++]);
            voxelData[x][y][z] = f;
            sum += f * f;
          }

      // normalizing!
      sum = (float) (1 / Math.sqrt(sum));
      for (int z = 0; z < nPointsZ; z++)
        for (int y = 0; y < nPointsY; y++)
          for (int x = 0; x < nPointsX; x++) {
            voxelData[x][y][z] *= sum;
          }
      VolumeDataInterface vd = (VolumeDataInterface) Interface
          .getOptionInterface("jvxl.data.VolumeData");
      vd.setVoxelCounts(nPointsX, nPointsY, nPointsZ);
      vd.setVolumetricVector(0, xStep, 0, 0);
      vd.setVolumetricVector(1, 0, yStep, 0);
      vd.setVolumetricVector(2, 0, 0, zStep);
      vd.setVolumetricOrigin(ox, oy, oz);
      vd.setVoxelDataAsArray(voxelData);
      if (moData == null) {
        moData = new Hashtable<String, Object>();
        moData.put("defaultCutoff", Float.valueOf((float) 0.01));
        moData.put("haveVolumeData", Boolean.TRUE);
        moData.put("calculationType", "Chem3D");
        orbitals = new  JmolList<Map<String, Object>>();
        moData.put("mos", orbitals);
      }
      Map<String, Object> mo = new Hashtable<String, Object>();
      mo.put("volumeData", vd);
      orbitals.addLast(mo);
      Logger
          .info("Chem3D molecular orbital data displayable using ISOSURFACE MO "
              + orbitals.size());
      return;
    }
  }

  private Map<String, Object> moData;
  
  @Override
  void processEndElement(String localName) {
    //System.out.println("xmlchem3d: end " + localName);
    if ("atom".equals(localName)) {
      if (atom.elementSymbol != null && !Float.isNaN(atom.z)) {
        parent.setAtomCoord(atom);
        atomSetCollection.addAtomWithMappedName(atom);
      }
      atom = null;
      return;
    }
    keepChars = false;
    chars = null;
  }

}
