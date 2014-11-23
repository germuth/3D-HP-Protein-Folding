/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.adapter.smarter;

import org.jmol.constant.EnumStructure;

public class Structure extends AtomSetObject {
  public EnumStructure structureType;
  public EnumStructure substructureType;
  public String structureID;
  public int serialID;
  public int strandCount;
  
  
  public char startChainID = '\0';
  public char startInsertionCode = '\0';
  public char endChainID = '\0';
  public char endInsertionCode = '\0';
  public int startSequenceNumber;
  public int endSequenceNumber;

  public static EnumStructure getHelixType(int type) {
    switch (type) {
    case 1:
      return EnumStructure.HELIXALPHA;
    case 3:
      return EnumStructure.HELIXPI;
    case 5:
      return EnumStructure.HELIX310;
    }
    return EnumStructure.HELIX;
  }
  
  public Structure(int modelIndex, EnumStructure structureType, EnumStructure substructureType,
                   String structureID, int serialID, int strandCount) {
    this.structureType = structureType;
    this.substructureType = substructureType;
    if (structureID == null)
      return;
    this.atomSetIndex = modelIndex;
    this.structureID = structureID;
    this.strandCount = strandCount; // 1 for sheet initially; 0 for helix or turn
    this.serialID = serialID;    
  }
  
  
  public void set(char startChainID, int startSequenceNumber, char startInsertionCode,
            char endChainID, int endSequenceNumber, char endInsertionCode) {
    this.startChainID = startChainID;
    this.startSequenceNumber = startSequenceNumber;
    this.startInsertionCode = startInsertionCode;
    this.endChainID = endChainID;
    this.endSequenceNumber = endSequenceNumber;
    this.endInsertionCode = endInsertionCode;
  }

}
