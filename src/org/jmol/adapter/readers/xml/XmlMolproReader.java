/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-12 00:46:22 -0500 (Tue, 12 Sep 2006) $
 * $Revision: 5501 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 * Copyright (C) 2005  Peter Knowles
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

/**
 * A Molpro 2005 reader
 */

public class XmlMolproReader extends XmlCmlReader {

  String[] myAttributes = { "id", "length", "type", //general
      "x3", "y3", "z3", "elementType", //atoms
      "name", //variable
      "groups", "cartesianLength", "primitives", // basisSet and
      "minL", "maxL", "angular", "contractions", //   basisGroup
      "occupation", "energy", "symmetryID", // orbital 
      "wavenumber", "units", // normalCoordinate
  };
  
  XmlMolproReader() {  
  }
  
  @Override
  protected String[] getDOMAttributes() {
    return myAttributes;
  }

  @Override
  public void processStartElement(String localName) {
    if (!processing)
      return;
    processStart2(localName);
    if (localName.equalsIgnoreCase("normalCoordinate")) {
      keepChars = false;
      if (!parent.doGetVibration(++vibrationNumber))
        return;
      try {
        atomSetCollection.cloneLastAtomSet();
      } catch (Exception e) {
        System.out.println(e.getMessage());
        atomSetCollection.errorMessage = "Error processing normalCoordinate: " + e.getMessage();
        vibrationNumber = 0;
        return;
      }
      if (atts.containsKey("wavenumber")) {
        String wavenumber = atts.get("wavenumber");
        String units = "cm^-1";
        if (atts.containsKey("units")) {
          units = atts.get("units");
          if (units.startsWith("inverseCent"))
            units = "cm^-1";
        }
        atomSetCollection.setAtomSetFrequency(null, null, wavenumber, units);
        keepChars = true;
      }
      return;
    }

    if (localName.equals("vibrations")) {
      vibrationNumber = 0;
      return;
    }
  }

  @Override
  void processEndElement(String localName) {
    if (localName.equalsIgnoreCase("normalCoordinate")) {
      if (!keepChars)
        return;
      int atomCount = atomSetCollection.getLastAtomSetAtomCount();
      int baseAtomIndex = atomSetCollection.getLastAtomSetAtomIndex();
      tokens = getTokensStr(chars);
      for (int offset = tokens.length - atomCount * 3, i = 0; i < atomCount; i++) {
        atomSetCollection.addVibrationVector(i + baseAtomIndex,
            parseFloatStr(tokens[offset++]),
            parseFloatStr(tokens[offset++]),
            parseFloatStr(tokens[offset++])
        );
      }
    }
    processEnd2(localName);
  }

}
