/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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


package org.jmol.symmetry;

/*
 * Bob Hanson 9/2006
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * LATT : http://macxray.chem.upenn.edu/LATT.pdf thank you, Patrick Carroll
 * 
 * Hall symbols:
 * 
 * http://cci.lbl.gov/sginfo/hall_symbols.html
 * 
 * and
 * 
 * http://cci.lbl.gov/cctbx/explore_symmetry.html
 * 
 * (-)L   [N_A^T_1]   [N_A^T_2]   ...  [N_A^T_P]   V(Nx Ny Nz)
 * 
 * lattice types S and T are not supported here
 * 
 * NEVER ACCESS THESE METHODS OUTSIDE OF THIS PACKAGE
 * 
 *
 */


import org.jmol.util.Logger;
import org.jmol.util.Matrix4f;
import org.jmol.util.P3i;
import org.jmol.util.SB;

class HallInfo {
  
  String hallSymbol;
  String primitiveHallSymbol;
  char latticeCode = '\0';
  String latticeExtension;
  boolean isCentrosymmetric;
  int nRotations;
  RotationTerm[] rotationTerms = new RotationTerm[16];
  P3i vector12ths;
  String vectorCode;
  
  HallInfo(String hallSymbol) {
    try {
      String str = this.hallSymbol = hallSymbol.trim();
      str = extractLatticeInfo(str);
      if (HallTranslation.getLatticeIndex(latticeCode) == 0)
        return;
      latticeExtension = HallTranslation.getLatticeExtension(latticeCode,
          isCentrosymmetric);
      str = extractVectorInfo(str) + latticeExtension;
      if (Logger.debugging)
        Logger.info("Hallinfo: " + hallSymbol + " " + str);
      int prevOrder = 0;
      char prevAxisType = '\0';
      primitiveHallSymbol = "P";
      while (str.length() > 0 && nRotations < 16) {
        str = extractRotationInfo(str, prevOrder, prevAxisType);
        RotationTerm r = rotationTerms[nRotations - 1];
        prevOrder = r.order;
        prevAxisType = r.axisType;
        primitiveHallSymbol += " " + r.primitiveCode;
      }
      primitiveHallSymbol += vectorCode;
    } catch (Exception e) {
      Logger.error("Invalid Hall symbol");
      nRotations = 0;
    }
  }
  
  String dumpInfo() {
    SB sb =  new SB();
    sb.append("\nHall symbol: ").append(hallSymbol)
        .append("\nprimitive Hall symbol: ").append(primitiveHallSymbol)
        .append("\nlattice type: ").append(getLatticeDesignation());
    for (int i = 0; i < nRotations; i++) {
      sb.append("\n\nrotation term ").appendI(i + 1).append(rotationTerms[i].dumpInfo());
    }
    return sb.toString();
  }

/*  
  String getCanonicalSeitzList() {
    String[] list = new String[nRotations];
    for (int i = 0; i < nRotations; i++)
      list[i] = SymmetryOperation.dumpSeitz(rotationTerms[i].seitzMatrix12ths);
    Arrays.sort(list, 0, nRotations);
    String s = "";
    for (int i = 0; i < nRotations; i++)
      s += list[i];
    s = s.replace('\t',' ').replace('\n',';');
    return s;
  }
*/
  private String getLatticeDesignation() {    
    return HallTranslation.getLatticeDesignation(latticeCode, isCentrosymmetric);
  }  
   
  private String extractLatticeInfo(String name) {
    int i = name.indexOf(" ");
    if (i < 0)
      return "";
    String term = name.substring(0, i).toUpperCase();
    latticeCode = term.charAt(0);
    if (latticeCode == '-') {
      isCentrosymmetric = true;
      latticeCode = term.charAt(1);
    }
    return name.substring(i + 1).trim();
  } 
  
  private String extractVectorInfo(String name) {
    // (nx ny nz)  where n is 1/12 of the edge. 
    // also allows for (nz), though that is not standard
    vector12ths = new P3i();
    vectorCode = "";
    int i = name.indexOf("(");
    int j = name.indexOf(")", i);
    if (i > 0 && j > i) {
      String term = name.substring(i + 1, j);
      vectorCode = " (" + term + ")";
      name = name.substring(0, i).trim();
      i = term.indexOf(" ");
      if (i >= 0) {
        vector12ths.x = Integer.parseInt(term.substring(0, i));
        term = term.substring(i + 1).trim();
        i = term.indexOf(" ");
        if (i >= 0) {
          vector12ths.y = Integer.parseInt(term.substring(0, i));
          term = term.substring(i + 1).trim();
        }
      }
      vector12ths.z = Integer.parseInt(term);
    }
    return name;
  }
  
  private String extractRotationInfo(String name, int prevOrder, char prevAxisType) {
    int i = name.indexOf(" ");
    String code;
    if (i >= 0) {
      code = name.substring(0, i);
      name = name.substring(i + 1).trim();
    } else {
      code = name;
      name = "";
    }
    rotationTerms[nRotations] = new RotationTerm(code, prevOrder, prevAxisType);
    nRotations++;
    return name;
  }
  
  class RotationTerm {
    
    RotationTerm() {      
    }
    
    String inputCode;
    String primitiveCode;
    String lookupCode;
    String translationString;
    HallRotation rotation;
    HallTranslation translation;
    Matrix4f seitzMatrix12ths = new Matrix4f();
    boolean isImproper;
    int order;
    char axisType = '\0';
    char diagonalReferenceAxis = '\0';
    
    RotationTerm(String code, int prevOrder, char prevAxisType) {
      getRotationInfo(code, prevOrder, prevAxisType);
    }
    
    boolean allPositive = true; //for now
    
    String dumpInfo() {
      SB sb= new SB();
      sb.append("\ninput code: ")
           .append(inputCode).append("; primitive code: ").append(primitiveCode)
           .append("\norder: ").appendI(order).append(isImproper ? " (improper axis)" : "");
      if (axisType != '_') {
        sb.append("; axisType: ").appendC(axisType);
        if (diagonalReferenceAxis != '\0')
          sb.appendC(diagonalReferenceAxis);
      }
      if (translationString.length() > 0)
        sb.append("; translation: ").append(translationString);
      if (vectorCode.length() > 0)
        sb.append("; vector offset:").append(vectorCode);
      if (rotation != null)
        sb.append("\noperator: ").append(getXYZ(allPositive)).append("\nSeitz matrix:\n")
            .append(SymmetryOperation.dumpSeitz(seitzMatrix12ths));
      return sb.toString();
    }
    
   String getXYZ(boolean allPositive) {
     return SymmetryOperation.getXYZFromMatrix(seitzMatrix12ths, true, allPositive, true);
   }
   
   private void getRotationInfo(String code, int prevOrder, char prevAxisType) {
      this.inputCode = code;
      code += "   ";
      if (code.charAt(0) == '-') {
        isImproper = true;
        code = code.substring(1);
      }
      primitiveCode = "";
      order = code.charAt(0) - '0';
      diagonalReferenceAxis = '\0';
      axisType = '\0';
      int ptr = 2; // pointing to "c" in 2xc
      char c;
      switch (c = code.charAt(1)) {
      case 'x':
      case 'y':
      case 'z':
        switch (code.charAt(2)) {
        case '\'':
        case '"':
          diagonalReferenceAxis = c;
          c = code.charAt(2);
          ptr++;
        }
        //$FALL-THROUGH$
      case '*':
        axisType = c;
        break;
      case '\'':
      case '"':
        axisType = c;
        switch (code.charAt(2)) {
        case 'x':
        case 'y':
        case 'z':
          diagonalReferenceAxis = code.charAt(2);
          ptr++;
          break;
        default:
          diagonalReferenceAxis = prevAxisType;
        }
        break;
      default:
        // implicit axis type
        axisType = (order == 1 ? '_'// no axis for 1
            : nRotations == 0 ? 'z' // z implied for first rotation
                : nRotations == 2 ? '*' // 3* implied for third rotation
                    : prevOrder == 2 || prevOrder == 4 ? 'x' // x implied for 2
                        // or 4
                        : '\'' // a-b (') implied for 3 or 6 previous
        );
        code = code.substring(0, 1) + axisType + code.substring(1);
      }
      primitiveCode += (axisType == '_' ? "1" : code.substring(0, 2));
      if (diagonalReferenceAxis != '\0') {
        // 2' needs x or y or z designation
        code = code.substring(0, 1) + diagonalReferenceAxis + axisType
            + code.substring(ptr);
        primitiveCode += diagonalReferenceAxis;
        ptr = 3;
      }
      lookupCode = code.substring(0, ptr);
      rotation = HallRotation.lookup(lookupCode);
      if (rotation == null) {
        Logger.error("Rotation lookup could not find " + inputCode + " ? "
            + lookupCode);
        return;
      }

      // now for translational part 1 2 3 4 5 6 a b c n u v w d r
      // The "r" is my addition to handle rhombohedral lattice with
      // primitive notation. This made coding FAR simpler -- all lattice
      // operations indicated by one to three 1xxx or -1 extensions.

      translation = new HallTranslation();
      translationString = "";
      int len = code.length();
      for (int i = ptr; i < len; i++) {
        char translationCode = code.charAt(i);
        HallTranslation t = new HallTranslation(translationCode, order);
        if (t.translationCode != '\0') {
          translationString += "" + t.translationCode;
          translation.rotationShift12ths += t.rotationShift12ths;
          translation.vectorShift12ths.add(t.vectorShift12ths);
        }
      }
      primitiveCode = (isImproper ? "-" : "") + primitiveCode
          + translationString;

      // set matrix, including translations and vector adjustment

      if (isImproper) {
        seitzMatrix12ths.setM(rotation.seitzMatrixInv);
      } else {
        seitzMatrix12ths.setM(rotation.seitzMatrix);
      }
      seitzMatrix12ths.m03 = translation.vectorShift12ths.x;
      seitzMatrix12ths.m13 = translation.vectorShift12ths.y;
      seitzMatrix12ths.m23 = translation.vectorShift12ths.z;
      switch (axisType) {
      case 'x':
        seitzMatrix12ths.m03 += translation.rotationShift12ths;
        break;
      case 'y':
        seitzMatrix12ths.m13 += translation.rotationShift12ths;
        break;
      case 'z':
        seitzMatrix12ths.m23 += translation.rotationShift12ths;
        break;
      }

      if (vectorCode.length() > 0) {
        Matrix4f m1 = new Matrix4f();
        Matrix4f m2 = new Matrix4f();
        m1.setIdentity();
        m2.setIdentity();
        m1.m03 = vector12ths.x;
        m1.m13 = vector12ths.y;
        m1.m23 = vector12ths.z;
        m2.m03 = -vector12ths.x;
        m2.m13 = -vector12ths.y;
        m2.m23 = -vector12ths.z;
        seitzMatrix12ths.mul2(m1, seitzMatrix12ths);
        seitzMatrix12ths.mul(m2);
      }
      if (Logger.debugging) {
        Logger.debug("code = " + code + "; primitive code =" + primitiveCode
            + "\n Seitz Matrix(12ths):" + seitzMatrix12ths);
      }
    }
  }  
}

