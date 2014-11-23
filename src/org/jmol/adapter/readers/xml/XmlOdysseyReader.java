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
import org.jmol.api.JmolAdapter;
import org.jmol.util.P3;

/**
 * An Odyssey xodydata reader
 */

public class XmlOdysseyReader extends XmlReader {

  private String modelName = null;
  private String formula = null;
  private String phase = null;
  
  private String[] myAttributes = { "id", "label", //general 
      "xyz", "element", "hybrid", //atoms
      "a", "b", "order", //bond
      "box" // boundary
  };

  XmlOdysseyReader() {
  }

  @Override
  protected String[] getDOMAttributes() {
    return myAttributes;
  }

  @Override
  protected void processStartElement(String localName) {

    if ("structure".equals(localName)) {
      atomSetCollection.newAtomSet();
      return;
    }

    if ("atom".equals(localName)) {
      atom = new Atom();
      if (atts.containsKey("label"))
        atom.atomName = atts.get("label");
      else
        atom.atomName = atts.get("id");
      if (atts.containsKey("xyz")) {
        String xyz = atts.get("xyz");
        String[] tokens = getTokensStr(xyz);
        atom.set(parseFloatStr(tokens[0]), parseFloatStr(tokens[1]),
            parseFloatStr(tokens[2]));
      }
      if (atts.containsKey("element")) {
        atom.elementSymbol = atts.get("element");
      }

      return;
    }
    if ("bond".equals(localName)) {
      String atom1 = atts.get("a");
      String atom2 = atts.get("b");
      int order = 1;
      if (atts.containsKey("order"))
        order = parseBondToken(atts.get("order"));
      atomSetCollection.addNewBondFromNames(atom1, atom2, order);
      return;
    }

    if ("boundary".equals(localName)) {
      String[] boxDim = getTokensStr(atts.get("box"));
      float x = parseFloatStr(boxDim[0]);
      float y = parseFloatStr(boxDim[1]);
      float z = parseFloatStr(boxDim[2]);
      parent.setUnitCellItem(0, x);
      parent.setUnitCellItem(1, y);
      parent.setUnitCellItem(2, z);
      parent.setUnitCellItem(3, 90);
      parent.setUnitCellItem(4, 90);
      parent.setUnitCellItem(5, 90);
      P3 pt = P3.new3(-x / 2, -y / 2, -z / 2);
      atomSetCollection.setAtomSetAuxiliaryInfo("periodicOriginXyz", pt);
      Atom[] atoms = atomSetCollection.getAtoms();
      for (int i = atomSetCollection.getAtomCount(); --i >= 0;) {
        atoms[i].sub(pt);
        parent.setAtomCoord(atoms[i]);
      }
      if (parent.latticeCells[0] == 0)
        parent.latticeCells[0] = parent.latticeCells[1] = parent.latticeCells[2] = 1;
      parent.setSymmetryOperator("x,y,z");
      parent.setSpaceGroupName("P1");
      parent.applySymmetryAndSetTrajectory();
      return;
    }

    if ("odyssey_simulation".equals(localName)) {
      if (modelName != null && phase != null)
        modelName += " - " + phase;
      if (modelName != null)
        atomSetCollection.setAtomSetName(modelName);
      if (formula != null)
        atomSetCollection.setAtomSetAuxiliaryInfo("formula", formula);
    }
    if ("title".equals(localName) || "formula".equals(localName)
        || "phase".equals(localName))
      keepChars = true;
  }

  private int parseBondToken(String str) {
    if (str.length() >= 1) {
      switch (str.charAt(0)) {
      case 's':
        return 1;
      case 'd':
        return 2;
      case 't':
        return 3;
      case 'a':
        return JmolAdapter.ORDER_AROMATIC;
      }
      return parseIntStr(str);
    }
    return 1;
  }

  @Override
  void processEndElement(String localName) {
    if ("atom".equals(localName)) {
      if (atom.elementSymbol != null && !Float.isNaN(atom.z)) {
        atomSetCollection.addAtomWithMappedName(atom);
      }
      atom = null;
      return;
    }
    if ("title".equals(localName)) {
      modelName = chars;
    }
    if ("formula".equals(localName)) {
      formula = chars;
    }
    if ("phase".equals(localName)) {
      phase = chars;
    }
    keepChars = false;
    chars = null;
  }

}
