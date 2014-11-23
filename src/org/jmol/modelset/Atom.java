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

package org.jmol.modelset;


import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.constant.EnumPalette;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumVdw;
import org.jmol.script.T;
import org.jmol.util.BS;
import org.jmol.util.C;
import org.jmol.util.ColorUtil;
import org.jmol.util.Elements;
import org.jmol.util.JmolList;
import org.jmol.util.P3;
import org.jmol.util.Quadric;
import org.jmol.util.JmolEdge;
import org.jmol.util.JmolNode;
import org.jmol.util.Point3fi;
import org.jmol.util.SB;
import org.jmol.util.Tuple3f;
import org.jmol.util.V3;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;




final public class Atom extends Point3fi implements JmolNode {

  private final static byte VIBRATION_VECTOR_FLAG = 1;
  private final static byte IS_HETERO_FLAG = 2;
  private final static byte FLAG_MASK = 3;
  
  public static final int RADIUS_MAX = 16;

  public char alternateLocationID = '\0';
  public byte atomID;
  int atomSite;
  public Group group;
  private float userDefinedVanDerWaalRadius;
  byte valence;
  
  private short atomicAndIsotopeNumber;
  private BS atomSymmetry;
  private byte formalChargeAndFlags;

  public byte getAtomID() {
    return atomID;
  }
  
  public short madAtom;

  public short colixAtom;
  byte paletteID = EnumPalette.CPK.id;

  Bond[] bonds;
  
  /**
   * 
   * @return  bonds -- WHICH MAY BE NULL
   * 
   */
  public Bond[] getBonds() {
    return bonds;
  }

  public void setBonds(Bond[] bonds) {
    this.bonds = bonds;  // for Smiles equating
  }
  
  int nBondsDisplayed = 0;
  int nBackbonesDisplayed = 0;
  
  public int getNBackbonesDisplayed() {
    return nBackbonesDisplayed;
  }
  
  public int clickabilityFlags;
  public int shapeVisibilityFlags;
  public static final int BACKBONE_VISIBILITY_FLAG = JC.getShapeVisibilityFlag(JC.SHAPE_BACKBONE);

  /**
   * @j2sIgnoreSuperConstructor
   * @j2sIgnoreParameters
   * 
   * @param modelIndex
   * @param atomIndex
   * @param x
   * @param y
   * @param z
   * @param radius
   * @param atomSymmetry
   * @param atomSite
   * @param atomicAndIsotopeNumber
   * @param formalCharge
   * @param isHetero
   */
  public Atom(int modelIndex, int atomIndex,
        float x, float y, float z, float radius,
        BS atomSymmetry, int atomSite,
        short atomicAndIsotopeNumber, int formalCharge, 
        boolean isHetero) {
    this.modelIndex = (short)modelIndex;
    this.atomSymmetry = atomSymmetry;
    this.atomSite = atomSite;
    this.index = atomIndex;
    this.atomicAndIsotopeNumber = atomicAndIsotopeNumber;
    if (isHetero)
      formalChargeAndFlags = IS_HETERO_FLAG;
    setFormalCharge(formalCharge);
    userDefinedVanDerWaalRadius = radius;
    set(x, y, z);
  }

  public void setAltLoc(char altLoc) {
    alternateLocationID = altLoc;
  }
  
  public final void setShapeVisibilityFlags(int flag) {
    shapeVisibilityFlags = flag;
  }

  public final void setShapeVisibility(int flag, boolean isVisible) {
    if(isVisible) {
      shapeVisibilityFlags |= flag;        
    } else {
      shapeVisibilityFlags &=~flag;
    }
  }
  
  public boolean isCovalentlyBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].isCovalent() 
            && bonds[i].getOtherAtom(this) == atomOther)
          return true;
    return false;
  }

  public boolean isBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(this) == atomOther)
          return true;
    return false;
  }

  public Bond getBond(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(atomOther) != null)
          return bonds[i];
    return null;
  }

  void addDisplayedBond(int stickVisibilityFlag, boolean isVisible) {
    nBondsDisplayed += (isVisible ? 1 : -1);
    setShapeVisibility(stickVisibilityFlag, (nBondsDisplayed > 0));
  } 
  
  public void addDisplayedBackbone(int backboneVisibilityFlag, boolean isVisible) {
    nBackbonesDisplayed += (isVisible ? 1 : -1);
    setShapeVisibility(backboneVisibilityFlag, isVisible);
  }
  
  void deleteBond(Bond bond) {
    // this one is used -- from Bond.deleteAtomReferences
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i] == bond) {
          deleteBondAt(i);
          return;
        }
  }

  private void deleteBondAt(int i) {
    int newLength = bonds.length - 1;
    if (newLength == 0) {
      bonds = null;
      return;
    }
    Bond[] bondsNew = new Bond[newLength];
    int j = 0;
    for ( ; j < i; ++j)
      bondsNew[j] = bonds[j];
    for ( ; j < newLength; ++j)
      bondsNew[j] = bonds[j + 1];
    bonds = bondsNew;
  }

  void clearBonds() {
    bonds = null;
  }

  public int getBondedAtomIndex(int bondIndex) {
    return bonds[bondIndex].getOtherAtom(this).index;
  }

  /*
   * What is a MAR?
   *  - just a term that Miguel made up
   *  - an abbreviation for Milli Angstrom Radius
   * that is:
   *  - a *radius* of either a bond or an atom
   *  - in *millis*, or thousandths of an *angstrom*
   *  - stored as a short
   *
   * However! In the case of an atom radius, if the parameter
   * gets passed in as a negative number, then that number
   * represents a percentage of the vdw radius of that atom.
   * This is converted to a normal MAR as soon as possible
   *
   * (I know almost everyone hates bytes & shorts, but I like them ...
   *  gives me some tiny level of type-checking ...
   *  a rudimentary form of enumerations/user-defined primitive types)
   */

  public void setMadAtom(Viewer viewer, RadiusData rd) {
    madAtom = calculateMad(viewer, rd);
  }
  
  public short calculateMad(Viewer viewer, RadiusData rd) {
    if (rd == null)
      return 0;
    float f = rd.value;
    if (f == 0)
      return 0;
    switch (rd.factorType) {
    case SCREEN:
       return (short) f;
    case FACTOR:
    case OFFSET:
      float r = 0;
      switch (rd.vdwType) {
      case TEMP:
        float tmax = viewer.getBfactor100Hi();
        r = (tmax > 0 ? getBfactor100() / tmax : 0);
        break;
      case HYDRO:
        r = Math.abs(getHydrophobicity());
        break;
      case IONIC:
        r = getBondingRadiusFloat();
        break;
      case ADPMIN:
      case ADPMAX:
        r = getADPMinMax(rd.vdwType == EnumVdw.ADPMAX);
        break;
      default:
        r = getVanderwaalsRadiusFloat(viewer, rd.vdwType);
      }
      if (rd.factorType == EnumType.FACTOR)
        f *= r;
      else
        f += r;
      break;
    case ABSOLUTE:
      break;
    }
    short mad = (short) (f < 0 ? f: f * 2000);
    if (mad < 0 && f > 0)
      mad = 0;
    return mad; 
  }

  public float getADPMinMax(boolean isMax) {
    Quadric[] ellipsoid = getEllipsoid();
    return (ellipsoid == null ? 0 : ellipsoid[0] == null ? 
        ellipsoid[1].lengths[isMax ? 2 : 0] * ellipsoid[1].scale 
        : ellipsoid[0].lengths[isMax ? 2 : 0] * ellipsoid[0].scale);
  }

  public int getRasMolRadius() {
    return Math.abs(madAtom / 8); //  1000r = 1000d / 2; rr = (1000r / 4);
  }

  public int getCovalentBondCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    Bond b;
    for (int i = bonds.length; --i >= 0; )
      if (((b = bonds[i]).order & JmolEdge.BOND_COVALENT_MASK) != 0
          && !b.getOtherAtom(this).isDeleted())
        ++n;
    return n;
  }

  public int getCovalentHydrogenCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; ) {
      if ((bonds[i].order & JmolEdge.BOND_COVALENT_MASK) == 0)
        continue;
      Atom a = bonds[i].getOtherAtom(this);
      if (a.valence >= 0 && a.getElementNumber() == 1)
        ++n;
    }
    return n;
  }

  public JmolEdge[] getEdges() {
    return bonds;
  }
  
  public void setColixAtom(short colixAtom) {
    this.colixAtom = colixAtom;
  }

  public void setPaletteID(byte paletteID) {
    this.paletteID = paletteID;
  }

  public void setTranslucent(boolean isTranslucent, float translucentLevel) {
    colixAtom = C.getColixTranslucent3(colixAtom, isTranslucent, translucentLevel);    
  }

  public boolean isTranslucent() {
    return C.isColixTranslucent(colixAtom);
  }

  public short getElementNumber() {
    return Elements.getElementNumber(atomicAndIsotopeNumber);
  }
  
  public short getIsotopeNumber() {
    return Elements.getIsotopeNumber(atomicAndIsotopeNumber);
  }
  
  public short getAtomicAndIsotopeNumber() {
    return atomicAndIsotopeNumber;
  }

  public void setAtomicAndIsotopeNumber(int n) {
    if (n < 0 || (n % 128) >= Elements.elementNumberMax || n > Short.MAX_VALUE)
      n = 0;
    atomicAndIsotopeNumber = (short) n;
  }

  public String getElementSymbolIso(boolean withIsotope) {
    return Elements.elementSymbolFromNumber(withIsotope ? atomicAndIsotopeNumber : atomicAndIsotopeNumber % 128);    
  }
  
  public String getElementSymbol() {
    return getElementSymbolIso(true);
  }

  public char getAlternateLocationID() {
    return alternateLocationID;
  }
  
  boolean isAlternateLocationMatch(String strPattern) {
    if (strPattern == null)
      return (alternateLocationID == '\0');
    if (strPattern.length() != 1)
      return false;
    char ch = strPattern.charAt(0);
    return (ch == '*' 
        || ch == '?' && alternateLocationID != '\0' 
        || alternateLocationID == ch);
  }

  public boolean isHetero() {
    return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
  }

  public boolean hasVibration() {
    return (formalChargeAndFlags & VIBRATION_VECTOR_FLAG) != 0;
  }

  public void setFormalCharge(int charge) {
    formalChargeAndFlags = (byte)((formalChargeAndFlags & FLAG_MASK) 
        | ((charge == Integer.MIN_VALUE ? 0 : charge > 7 ? 7 : charge < -3 ? -3 : charge) << 2));
  }
  
  void setVibrationVector() {
    formalChargeAndFlags |= VIBRATION_VECTOR_FLAG;
  }
  
  public int getFormalCharge() {
    //System.out.println("Atom " + this + " " + this.formalChargeAndFlags);
    return formalChargeAndFlags >> 2;
  }

  // a percentage value in the range 0-100
  public int getOccupancy100() {
    byte[] occupancies = group.chain.model.modelSet.occupancies;
    return occupancies == null ? 100 : occupancies[index];
  }

  // This is called bfactor100 because it is stored as an integer
  // 100 times the bfactor(temperature) value
  public int getBfactor100() {
    short[] bfactor100s = group.chain.model.modelSet.bfactor100s;
    if (bfactor100s == null)
      return 0;
    return bfactor100s[index];
  }

  private float getHydrophobicity() {
    float[] values = group.chain.model.modelSet.hydrophobicities;
    if (values == null)
      return Elements.getHydrophobicity(group.getGroupID());
    return values[index];
  }

  public boolean setRadius(float radius) {
    return !Float.isNaN(userDefinedVanDerWaalRadius = (radius > 0 ? radius : Float.NaN));  
  }
  
  public void delete(BS bsBonds) {
    valence = -1;
    if (bonds != null)
      for (int i = bonds.length; --i >= 0; ) {
        Bond bond = bonds[i];
        bond.getOtherAtom(this).deleteBond(bond);
        bsBonds.set(bond.index);
      }
    bonds = null;
  }

  public boolean isDeleted() {
    return (valence < 0);
  }

  public void setValence(int nBonds) {
    if (isDeleted()) // no resurrection
      return;
    valence = (byte) (nBonds < 0 ? 0 : nBonds < 0xEF ? nBonds : 0xEF);
  }

  public int getValence() {
    if (isDeleted())
      return -1;
    int n = valence;
    if (n == 0 && bonds != null)
      for (int i = bonds.length; --i >= 0;)
        n += bonds[i].getValence();
    return n;
  }

  public int getImplicitHydrogenCount() {
    return group.chain.model.modelSet.getImplicitHydrogenCount(this);
  }

  int getTargetValence() {
    switch (getElementNumber()) {
    case 6: //C
    case 14: //Si      
      return 4;
    case 5:  // B
    case 7:  // N
    case 15: // P
      return 3;
    case 8: //O
    case 16: //S
      return 2;
    case 9: // F
    case 17: // Cl
    case 35: // Br
    case 53: // I
      return 1;
    }
    return -1;
  }


  public float getDimensionValue(int dimension) {
    return (dimension == 0 ? x : (dimension == 1 ? y : z));
  }

  public float getVanderwaalsRadiusFloat(Viewer viewer, EnumVdw type) {
    // called by atomPropertyFloat as VDW_AUTO,
    // AtomCollection.fillAtomData with VDW_AUTO or VDW_NOJMOL
    // AtomCollection.findMaxRadii with VDW_AUTO
    // AtomCollection.getAtomPropertyState with VDW_AUTO
    // AtomCollection.getVdwRadius with passed on type
    return (Float.isNaN(userDefinedVanDerWaalRadius) 
        ? viewer.getVanderwaalsMarType(atomicAndIsotopeNumber % 128, getVdwType(type)) / 1000f
        : userDefinedVanDerWaalRadius);
  }

  /**
   * 
   * @param type 
   * @return if VDW_AUTO, will return VDW_AUTO_JMOL, VDW_AUTO_RASMOL, or VDW_AUTO_BABEL
   *         based on the model type
   */
  @SuppressWarnings("incomplete-switch")
  private EnumVdw getVdwType(EnumVdw type) {
    switch (type) {
    case AUTO:
      type = group.chain.model.modelSet.getDefaultVdwType(modelIndex);
      break;
    case NOJMOL:
      type = group.chain.model.modelSet.getDefaultVdwType(modelIndex);
      if (type == EnumVdw.AUTO_JMOL)
        type = EnumVdw.AUTO_BABEL;
      break;
    }
    return type;
  }

  private float getCovalentRadiusFloat() {
    return Elements.getBondingRadiusFloat(atomicAndIsotopeNumber, 0);
  }

  public float getBondingRadiusFloat() {
    float[] ionicRadii = group.chain.model.modelSet.ionicRadii;
    float r = (ionicRadii == null ? 0 : ionicRadii[index]);
    return (r == 0 ? Elements.getBondingRadiusFloat(atomicAndIsotopeNumber,
        getFormalCharge()) : r);
  }

  float getVolume(Viewer viewer, EnumVdw vType) {
    float r1 = (vType == null ? userDefinedVanDerWaalRadius : Float.NaN);
    if (Float.isNaN(r1))
      r1 = viewer.getVanderwaalsMarType(getElementNumber(), getVdwType(vType)) / 1000f;
    double volume = 0;
    if (bonds != null)
      for (int j = 0; j < bonds.length; j++) {
        if (!bonds[j].isCovalent())
          continue;
        Atom atom2 = bonds[j].getOtherAtom(this);
        float r2 = (vType == null ? atom2.userDefinedVanDerWaalRadius : Float.NaN);
        if (Float.isNaN(r2))
          r2 = viewer.getVanderwaalsMarType(atom2.getElementNumber(), atom2
              .getVdwType(vType)) / 1000f;
        float d = distance(atom2);
        if (d > r1 + r2)
          continue;
        if (d + r1 <= r2)
          return 0;

        // calculate hidden spherical cap height and volume
        // A.Bondi, J. Phys. Chem. 68, 1964, 441-451.

        double h = r1 - (r1 * r1 + d * d - r2 * r2) / (2.0 * d);
        volume -= Math.PI / 3 * h * h * (3 * r1 - h);
      }
    return (float) (volume + 4 * Math.PI / 3 * r1 * r1 * r1);
  }

  int getCurrentBondCount() {
    return bonds == null ? 0 : bonds.length;
  }

  public short getColix() {
    return colixAtom;
  }

  public byte getPaletteID() {
    return paletteID;
  }

  public float getRadius() {
    return Math.abs(madAtom / (1000f * 2));
  }

  public int getIndex() {
    return index;
  }

  public int getAtomSite() {
    return atomSite;
  }

  public void setAtomSymmetry(BS bsSymmetry) {
    atomSymmetry = bsSymmetry;
  }

  public BS getAtomSymmetry() {
    return atomSymmetry;
  }

   void setGroup(Group group) {
     this.group = group;
   }

   public Group getGroup() {
     return group;
   }
   
   public void getGroupBits(BS bs) {
     group.selectAtoms(bs);
   }
   
   public String getAtomName() {
     return (atomID > 0 ? JC.getSpecialAtomName(atomID) 
         : group.chain.model.modelSet.atomNames[index]);
   }
   
   public String getAtomType() {
    String[] atomTypes = group.chain.model.modelSet.atomTypes;
    String type = (atomTypes == null ? null : atomTypes[index]);
    return (type == null ? getAtomName() : type);
  }
   
   public int getAtomNumber() {
     int[] atomSerials = group.chain.model.modelSet.atomSerials;
     // shouldn't ever be null.
     return (atomSerials != null ? atomSerials[index] : index);
//        : group.chain.model.modelSet.isZeroBased ? atomIndex : atomIndex);
   }

   public boolean isInFrame() {
     return ((shapeVisibilityFlags & JC.ATOM_IN_FRAME) != 0);
   }

   public int getShapeVisibilityFlags() {
     return shapeVisibilityFlags;
   }
   
   public boolean isShapeVisible(int shapeVisibilityFlag) {
     return ((shapeVisibilityFlags & shapeVisibilityFlag) != 0);
   }

   public float getPartialCharge() {
     float[] partialCharges = group.chain.model.modelSet.partialCharges;
     return partialCharges == null ? 0 : partialCharges[index];
   }

   public Quadric[] getEllipsoid() {
     return group.chain.model.modelSet.getEllipsoid(index);
   }

   public void scaleEllipsoid(int size, int iSelect) {
     Quadric[] ellipsoid = getEllipsoid();
     if (ellipsoid == null || iSelect >= ellipsoid.length || ellipsoid[iSelect] == null)
       return;
     ellipsoid[iSelect].setSize(size);
   }

   /**
    * Given a symmetry operation number, the set of cells in the model, and the
    * number of operations, this method returns either 0 or the cell number (555, 666)
    * of the translated symmetry operation corresponding to this atom.
    * 
    * atomSymmetry is a bitset that is created in adapter.smarter.AtomSetCollection
    * 
    * It is arranged as follows:
    * 
    * |--overall--|---cell1---|---cell2---|---cell3---|...
    * 
    * |012..nOps-1|012..nOps-1|012..nOp-1s|012..nOps-1|...
    * 
    * If a bit is set, it means that the atom was created using that operator
    * operating on the base file set and translated for that cell.
    * 
    * If any bit is set in any of the cell blocks, then the same
    * bit will also be set in the overall block. This allows for
    * rapid determination of special positions and also of
    * atom membership in any operation set.
    * 
    *  Note that it is not necessarily true that an atom is IN the designated
    *  cell, because one can load {nnn mmm 0}, and then, for example, the {-x,-y,-z}
    *  operator sends atoms from 555 to 444. Still, those atoms would be marked as
    *  cell 555 here, because no translation was carried out. 
    *  
    *  That is, the numbers 444 in symop=3444 do not refer to a cell, per se. 
    *  What they refer to is the file-designated operator plus a translation of
    *  {-1 -1 -1/1}. 
    * 
    * @param symop        = 0, 1, 2, 3, ....
    * @param cellRange    = {444, 445, 446, 454, 455, 456, .... }
    * @param nOps         = 2 for x,y,z;-x,-y,-z, for example
    * @return cell number such as 565
    */
   public int getSymmetryTranslation(int symop, int[] cellRange, int nOps) {
     int pt = symop;
     for (int i = 0; i < cellRange.length; i++)
       if (atomSymmetry.get(pt += nOps))
         return cellRange[i];
     return 0;
   }
   
   /**
    * Looks for a match in the cellRange list for this atom within the specified translation set
    * select symop=0NNN for this
    * 
    * @param cellNNN
    * @param cellRange
    * @param nOps
    * @return     matching cell number, if applicable
    */
   public int getCellTranslation(int cellNNN, int[] cellRange, int nOps) {
     int pt = nOps;
     for (int i = 0; i < cellRange.length; i++)
       for (int j = 0; j < nOps;j++, pt++)
       if (atomSymmetry.get(pt) && cellRange[i] == cellNNN)
         return cellRange[i];
     return 0;
   }
   
   String getSymmetryOperatorList() {
    String str = "";
    ModelSet f = group.chain.model.modelSet;
    int nOps = f.getModelSymmetryCount(modelIndex);
    if (nOps == 0 || atomSymmetry == null)
      return "";
    int[] cellRange = f.getModelCellRange(modelIndex);
    int pt = nOps;
    int n = (cellRange == null ? 1 : cellRange.length);
    for (int i = 0; i < n; i++)
      for (int j = 0; j < nOps; j++)
        if (atomSymmetry.get(pt++))
          str += "," + (j + 1) + "" + cellRange[i];
    return str.substring(1);
  }
   
  public int getModelIndex() {
    return modelIndex;
  }
   
  int getMoleculeNumber(boolean inModel) {
    return (group.chain.model.modelSet.getMoleculeIndex(index, inModel) + 1);
  }
   
  private float getFractionalCoord(char ch, boolean asAbsolute) {
    P3 pt = getFractionalCoordPt(asAbsolute);
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }
    
  private P3 getFractionalCoordPt(boolean asAbsolute) {
    // asAbsolute TRUE uses the original unshifted matrix
    SymmetryInterface c = group.chain.model.modelSet.getUnitCell(modelIndex);
    if (c == null) 
      return this;
    P3 pt = P3.newP(this);
    c.toFractional(pt, asAbsolute);
    return pt;
  }
  
  private float getFractionalUnitCoord(char ch) {
    P3 pt = getFractionalUnitCoordPt(false);
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }

  P3 getFractionalUnitCoordPt(boolean asCartesian) {
    SymmetryInterface c = group.chain.model.modelSet.getUnitCell(modelIndex);
    if (c == null)
      return this;
    P3 pt = P3.newP(this);
    if (group.chain.model.isJmolDataFrame) {
      c.toFractional(pt, false);
      if (asCartesian)
        c.toCartesian(pt, false);
    } else {
      c.toUnitCell(pt, null);
      if (!asCartesian)
        c.toFractional(pt, false);
    }
    return pt;
  }
  
  float getFractionalUnitDistance(P3 pt, P3 ptTemp1, P3 ptTemp2) {
    SymmetryInterface c = group.chain.model.modelSet.getUnitCell(modelIndex);
    if (c == null) 
      return distance(pt);
    ptTemp1.setT(this);
    ptTemp2.setT(pt);
    if (group.chain.model.isJmolDataFrame) {
      c.toFractional(ptTemp1, true);
      c.toFractional(ptTemp2, true);
    } else {
      c.toUnitCell(ptTemp1, null);
      c.toUnitCell(ptTemp2, null);
    }
    return ptTemp1.distance(ptTemp2);
  }
  
  void setFractionalCoord(int tok, float fValue, boolean asAbsolute) {
    SymmetryInterface c = group.chain.model.modelSet.getUnitCell(modelIndex);
    if (c != null)
      c.toFractional(this, asAbsolute);
    switch (tok) {
    case T.fux:
    case T.fracx:
      x = fValue;
      break;
    case T.fuy:
    case T.fracy:
      y = fValue;
      break;
    case T.fuz:
    case T.fracz:
      z = fValue;
      break;
    }
    if (c != null)
      c.toCartesian(this, asAbsolute);
  }
  
  void setFractionalCoordTo(P3 ptNew, boolean asAbsolute) {
    setFractionalCoordPt(this, ptNew, asAbsolute);
  }
  
  public void setFractionalCoordPt(P3 pt, P3 ptNew, boolean asAbsolute) {
    pt.setT(ptNew);
    SymmetryInterface c = group.chain.model.modelSet.getUnitCell(modelIndex);
    if (c != null)
      c.toCartesian(pt, asAbsolute && !group.chain.model.isJmolDataFrame);
  }

  boolean isCursorOnTopOf(int xCursor, int yCursor,
                        int minRadius, Atom competitor) {
    int r = screenDiameter / 2;
    if (r < minRadius)
      r = minRadius;
    int r2 = r * r;
    int dx = screenX - xCursor;
    int dx2 = dx * dx;
    if (dx2 > r2)
      return false;
    int dy = screenY - yCursor;
    int dy2 = dy * dy;
    int dz2 = r2 - (dx2 + dy2);
    if (dz2 < 0)
      return false;
    if (competitor == null)
      return true;
    int z = screenZ;
    int zCompetitor = competitor.screenZ;
    int rCompetitor = competitor.screenDiameter / 2;
    if (z < zCompetitor - rCompetitor)
      return true;
    int dxCompetitor = competitor.screenX - xCursor;
    int dx2Competitor = dxCompetitor * dxCompetitor;
    int dyCompetitor = competitor.screenY - yCursor;
    int dy2Competitor = dyCompetitor * dyCompetitor;
    int r2Competitor = rCompetitor * rCompetitor;
    int dz2Competitor = r2Competitor - (dx2Competitor + dy2Competitor);
    return (z - Math.sqrt(dz2) < zCompetitor - Math.sqrt(dz2Competitor));
  }

  /*
   *  DEVELOPER NOTE (BH):
   *  
   *  The following methods may not return 
   *  correct values until after modelSet.finalizeGroupBuild()
   *  
   */
   
  public String getInfo() {
    return getIdentity(true);
  } 

  String getInfoXYZ(boolean useChimeFormat) {
    // for atom picking
    if (useChimeFormat) {
      String group3 = getGroup3(true);
      char chainID = getChainID();
      P3 pt = getFractionalCoordPt(true);
      return "Atom: " + (group3 == null ? getElementSymbol() : getAtomName()) + " " + getAtomNumber() 
          + (group3 != null && group3.length() > 0 ? 
              (isHetero() ? " Hetero: " : " Group: ") + group3 + " " + getResno() 
              + (chainID != 0 && chainID != ' ' ? " Chain: " + chainID : "")              
              : "")
          + " Model: " + getModelNumber()
          + " Coordinates: " + x + " " + y + " " + z
          + (pt == null ? "" : " Fractional: "  + pt.x + " " + pt.y + " " + pt.z); 
    }
    return getIdentityXYZ(true);
  }

  String getIdentityXYZ(boolean allInfo) {
    P3 pt = (group.chain.model.isJmolDataFrame ? getFractionalCoordPt(false) : this);
    return getIdentity(allInfo) + " " + pt.x + " " + pt.y + " " + pt.z;  
  }
  
  String getIdentity(boolean allInfo) {
    SB info = new SB();
    String group3 = getGroup3(true);
    if (group3 != null && group3.length() > 0) {
      info.append("[");
      info.append(group3);
      info.append("]");
      String seqcodeString = getSeqcodeString();
      if (seqcodeString != null)
        info.append(seqcodeString);
      char chainID = getChainID();
      if (chainID != 0 && chainID != ' ') {
        info.append(":");
        info.appendC(chainID);
      }
      if (!allInfo)
        return info.toString();
      info.append(".");
    }
    info.append(getAtomName());
    if (info.length() == 0) {
      // since atomName cannot be null, this is unreachable
      info.append(getElementSymbolIso(false));
      info.append(" ");
      info.appendI(getAtomNumber());
    }
    if (alternateLocationID != 0) {
      info.append("%");
      info.appendC(alternateLocationID);
    }
    if (group.chain.model.modelSet.modelCount > 1) {
      info.append("/");
      info.append(getModelNumberForLabel());
    }
    info.append(" #");
    info.appendI(getAtomNumber());
    return info.toString();
  }

  public String getGroup3(boolean allowNull) {
    String group3 = group.getGroup3();
    return (allowNull 
        || group3 != null && group3.length() > 0 
        ? group3 : "UNK");
  }

  public String getGroup1(char c0) {
    char c = group.getGroup1();
    return (c != '\0' ? "" + c : c0 != '\0' ? "" + c0 : "");
  }

  public boolean isProtein() {
    return group.isProtein();
  }

  boolean isCarbohydrate() {
    return group.isCarbohydrate();
  }

  public boolean isNucleic() {
    return group.isNucleic();
  }

  public boolean isDna() {
    return group.isDna();
  }
  
  public boolean isRna() {
    return group.isRna();
  }

  public boolean isPurine() {
    return group.isPurine();
  }

  public boolean isPyrimidine() {
    return group.isPyrimidine();
  }

  int getSeqcode() {
    return group.getSeqcode();
  }

  public int getResno() {
    return group.getResno();   
  }

  public boolean isClickable() {
    // certainly if it is not visible, then it can't be clickable
    if (!isVisible(0))
      return false;
    int flags = shapeVisibilityFlags | group.shapeVisibilityFlags;
    return ((flags & clickabilityFlags) != 0);
  }

  public int getClickabilityFlags() {
    return clickabilityFlags;
  }
  
  public void setClickable(int flag) {
    if (flag == 0)
      clickabilityFlags = 0;
    else
      clickabilityFlags |= flag;
  }
  
  /**
   * determine if an atom or its PDB group is visible
   * @param flags TODO
   * @return true if the atom is in the "select visible" set
   */
  public boolean isVisible(int flags) {
    // Is the atom's model visible? Is the atom NOT hidden?
    if (!isInFrame() || group.chain.model.modelSet.isAtomHidden(index))
      return false;
    // Is any shape associated with this atom visible?
    if (flags != 0)
      return (isShapeVisible(flags));  
    flags = shapeVisibilityFlags;
    // Is its PDB group visible in any way (cartoon, e.g.)?
    //  An atom is considered visible if its PDB group is visible, even
    //  if it does not show up itself as part of the structure
    //  (this will be a difference in terms of *clickability*).
    // except BACKBONE -- in which case we only see the lead atoms
    if (group.shapeVisibilityFlags != Atom.BACKBONE_VISIBILITY_FLAG
        || isLeadAtom())
      flags |= group.shapeVisibilityFlags;

    // We know that (flags & AIM), so now we must remove that flag
    // and check to see if any others are remaining.
    // Only then is the atom considered visible.
    return ((flags & ~JC.ATOM_IN_FRAME) != 0);
  }

  public boolean isLeadAtom() {
    return group.isLeadAtom(index);
  }
  
  public float getGroupParameter(int tok) {
    return group.getGroupParameter(tok);
  }

  public char getChainID() {
    return group.chain.chainID;
  }

  public int getSurfaceDistance100() {
    return group.chain.model.modelSet.getSurfaceDistance100(index);
  }

  public V3 getVibrationVector() {
    return group.chain.model.modelSet.getVibrationVector(index, false);
  }

  public float getVibrationCoord(char ch) {
    return group.chain.model.modelSet.getVibrationCoord(index, ch);
  }


  public int getPolymerLength() {
    return group.getBioPolymerLength();
  }

  public int getPolymerIndexInModel() {
    return group.getBioPolymerIndexInModel();
  }

  public int getMonomerIndex() {
    return group.getMonomerIndex();
  }
  
  public int getSelectedGroupCountWithinChain() {
    return group.chain.selectedGroupCount;
  }

  public int getSelectedGroupIndexWithinChain() {
    return group.getSelectedGroupIndex();
  }

  public int getSelectedMonomerCountWithinPolymer() {
    return group.getSelectedMonomerCount();
  }

  public int getSelectedMonomerIndexWithinPolymer() {
    return group.getSelectedMonomerIndex();
  }

  public Chain getChain() {
    return group.chain;
  }

  public String getModelNumberForLabel() {
    return group.chain.model.modelSet.getModelNumberForAtomLabel(modelIndex);
  }
  
  public int getModelNumber() {
    return group.chain.model.modelSet.getModelNumber(modelIndex) % 1000000;
  }
  
  public int getModelFileIndex() {
    return group.chain.model.fileIndex;
  }
  
  public int getModelFileNumber() {
    return group.chain.model.modelSet.getModelFileNumber(modelIndex);
  }
  
  public String getBioStructureTypeName() {
    return getProteinStructureType().getBioStructureTypeName(true);
  }
  
  public EnumStructure getProteinStructureType() {
    return group.getProteinStructureType();
  }
  
  public EnumStructure getProteinStructureSubType() {
    return group.getProteinStructureSubType();
  }
  
  public int getStrucNo() {
    return group.getStrucNo();
  }

  public String getStructureId() {
    return group.getStructureId();
  }

  public String getProteinStructureTag() {
    return group.getProteinStructureTag();
  }

  public short getGroupID() {
    return group.groupID;
  }

  public String getSeqcodeString() {
    return group.getSeqcodeString();
  }

  public int getSeqNumber() {
    return group.getSeqNumber();
  }

  public char getInsertionCode() {
    return group.getInsertionCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    return (this == obj);
  }

  @Override
  public int hashCode() {
    //this overrides the Point3fi hashcode, which would
    //give a different hashcode for an atom depending upon
    //its screen location! Bug fix for 11.1.43 Bob Hanson
    return index;
  }
  
  public Atom findAromaticNeighbor(int notAtomIndex) {
    if (bonds == null)
      return null;
    for (int i = bonds.length; --i >= 0; ) {
      Bond bondT = bonds[i];
      Atom a = bondT.getOtherAtom(this);
      if (bondT.isAromatic() && a.index != notAtomIndex)
        return a;
    }
    return null;
  }

  /**
   * called by isosurface and int comparator via atomProperty()
   * and also by getBitsetProperty() 
   * 
   * @param atom
   * @param tokWhat
   * @return         int value or Integer.MIN_VALUE
   */
  public static int atomPropertyInt(Atom atom, int tokWhat) {
    switch (tokWhat) {
    case T.atomno:
      return atom.getAtomNumber();
    case T.atomid:
      return atom.atomID;
    case T.atomindex:
      return atom.getIndex();
    case T.bondcount:
      return atom.getCovalentBondCount();
    case T.color:
      return atom.group.chain.model.modelSet.viewer.getColorArgbOrGray(atom.getColix());
    case T.element:
    case T.elemno:
      return atom.getElementNumber();
    case T.elemisono:
      return atom.atomicAndIsotopeNumber;
    case T.file:
      return atom.getModelFileIndex() + 1;
    case T.formalcharge:
      return atom.getFormalCharge();
    case T.groupid:
      return atom.getGroupID(); //-1 if no group
    case T.groupindex:
      return atom.group.getGroupIndex();
    case T.model:
      //integer model number -- could be PDB/sequential adapter number
      //or it could be a sequential model in file number when multiple files
      return atom.getModelNumber();
    case -T.model:
      //float is handled differently
      return atom.getModelFileNumber();
    case T.modelindex:
      return atom.modelIndex;
    case T.molecule:
      return atom.getMoleculeNumber(true);
    case T.occupancy:
      return atom.getOccupancy100();
    case T.polymer:
      return atom.getGroup().getBioPolymerIndexInModel() + 1;
    case T.polymerlength:
      return atom.getPolymerLength();
    case T.radius:
      // the comparator uses rasmol radius, unfortunately, for integers
      return atom.getRasMolRadius();        
    case T.resno:
      return atom.getResno();
    case T.site:
      return atom.getAtomSite();
    case T.structure:
      return atom.getProteinStructureType().getId();
    case T.substructure:
      return atom.getProteinStructureSubType().getId();
    case T.strucno:
      return atom.getStrucNo();
    case T.valence:
      return atom.getValence();
    }
    return 0;      
  }

  /**
   * called by isosurface and int comparator via atomProperty() and also by
   * getBitsetProperty()
   * 
   * @param viewer
   * 
   * @param atom
   * @param tokWhat
   * @return float value or value*100 (asInt=true) or throw an error if not
   *         found
   * 
   */
  public static float atomPropertyFloat(Viewer viewer, Atom atom, int tokWhat) {
    switch (tokWhat) {
    case T.radius:
      return atom.getRadius();
    case T.selected:
      return (viewer.isAtomSelected(atom.index) ? 1 : 0);
    case T.surfacedistance:
      atom.group.chain.model.modelSet.getSurfaceDistanceMax();
      return atom.getSurfaceDistance100() / 100f;
    case T.temperature: // 0 - 9999
      return atom.getBfactor100() / 100f;
    case T.hydrophobic:
      return atom.getHydrophobicity();
    case T.volume:
      return atom.getVolume(viewer, EnumVdw.AUTO);

      // these next have to be multiplied by 100 if being compared
      // note that spacefill here is slightly different than radius -- no integer option

    case T.adpmax:
      return atom.getADPMinMax(true);
    case T.adpmin:
      return atom.getADPMinMax(false);
    case T.atomx:
    case T.x:
      return atom.x;
    case T.atomy:
    case T.y:
      return atom.y;
    case T.atomz:
    case T.z:
      return atom.z;
    case T.covalent:
      return atom.getCovalentRadiusFloat();
    case T.fracx:
      return atom.getFractionalCoord('X', true);
    case T.fracy:
      return atom.getFractionalCoord('Y', true);
    case T.fracz:
      return atom.getFractionalCoord('Z', true);
    case T.fux:
      return atom.getFractionalCoord('X', false);
    case T.fuy:
      return atom.getFractionalCoord('Y', false);
    case T.fuz:
      return atom.getFractionalCoord('Z', false);
    case T.screenx:
      return atom.screenX;
    case T.screeny:
      return atom.group.chain.model.modelSet.viewer.getScreenHeight() - atom.screenY;
    case T.screenz:
      return atom.screenZ;
    case T.ionic:
      return atom.getBondingRadiusFloat();
    case T.mass:
      return atom.getMass();
    case T.occupancy:
      return atom.getOccupancy100() / 100f;
    case T.partialcharge:
      return atom.getPartialCharge();
    case T.phi:
    case T.psi:
    case T.omega:
      if (atom.group.chain.model.isJmolDataFrame
          && atom.group.chain.model.jmolFrameType
              .startsWith("plot ramachandran")) {
        switch (tokWhat) {
        case T.phi:
          return atom.getFractionalCoord('X', false);
        case T.psi:
          return atom.getFractionalCoord('Y', false);
        case T.omega:
          if (atom.group.chain.model.isJmolDataFrame
              && atom.group.chain.model.jmolFrameType
                  .equals("plot ramachandran")) {
            float omega = atom.getFractionalCoord('Z', false) - 180;
            return (omega < -180 ? 360 + omega : omega);
          }
        }
      }
      return atom.getGroupParameter(tokWhat);
    case T.eta:
    case T.theta:
    case T.straightness:
      return atom.getGroupParameter(tokWhat);
    case T.spacefill:
      return atom.getRadius();
    case T.backbone:
    case T.cartoon:
    case T.dots:
    case T.ellipsoid:
    case T.geosurface:
    case T.halo:
    case T.meshRibbon:
    case T.ribbon:
    case T.rocket:
    case T.star:
    case T.strands:
    case T.trace:
      return viewer.getAtomShapeValue(tokWhat, atom.group, atom.index);
    case T.unitx:
      return atom.getFractionalUnitCoord('X');
    case T.unity:
      return atom.getFractionalUnitCoord('Y');
    case T.unitz:
      return atom.getFractionalUnitCoord('Z');
    case T.vanderwaals:
      return atom.getVanderwaalsRadiusFloat(viewer, EnumVdw.AUTO);
    case T.vibx:
      return atom.getVibrationCoord('X');
    case T.viby:
      return atom.getVibrationCoord('Y');
    case T.vibz:
      return atom.getVibrationCoord('Z');
    case T.vectorscale:
      V3 v = atom.getVibrationVector();
      return (v == null ? 0 : v.length() * viewer.getVectorScale());

    }
    return atomPropertyInt(atom, tokWhat);
  }

  private float getMass() {
    float mass = getIsotopeNumber();
    return (mass > 0 ? mass : Elements.getAtomicMass(getElementNumber()));
  }

  public static String atomPropertyString(Viewer viewer, Atom atom, int tokWhat) {
    char ch;
    switch (tokWhat) {
    case T.altloc:
      ch = atom.getAlternateLocationID();
      return (ch == '\0' ? "" : "" + ch);
    case T.atomname:
      return atom.getAtomName();
    case T.atomtype:
      return atom.getAtomType();
    case T.chain:
      ch = atom.getChainID();
      return (ch == '\0' ? "" : "" + ch);
    case T.sequence:
      return atom.getGroup1('?');
    case T.group1:
      return atom.getGroup1('\0');
    case T.group:
      return atom.getGroup3(false);
    case T.element:
      return atom.getElementSymbolIso(true);
    case T.identify:
      return atom.getIdentity(true);
    case T.insertion:
      ch = atom.getInsertionCode();
      return (ch == '\0' ? "" : "" + ch);
    case T.label:
    case T.format:
      String s = atom.group.chain.model.modelSet.getAtomLabel(atom.getIndex());
      if (s == null)
        s = "";
      return s;
    case T.structure:
      return atom.getProteinStructureType().getBioStructureTypeName(false);
    case T.substructure:
      return atom.getProteinStructureSubType().getBioStructureTypeName(false);
    case T.strucid:
      return atom.getStructureId();
    case T.shape:
      return viewer.getHybridizationAndAxes(atom.index, null, null, "d");
    case T.symbol:
      return atom.getElementSymbolIso(false);
    case T.symmetry:
      return atom.getSymmetryOperatorList();
    }
    return ""; 
  }

  public static Tuple3f atomPropertyTuple(Atom atom, int tok) {
    switch (tok) {
    case T.fracxyz:
      return atom.getFractionalCoordPt(!atom.group.chain.model.isJmolDataFrame);
    case T.fuxyz:
      return atom.getFractionalCoordPt(false);
    case T.unitxyz:
      return (atom.group.chain.model.isJmolDataFrame ? atom.getFractionalCoordPt(false) 
          : atom.getFractionalUnitCoordPt(false));
    case T.screenxyz:
      return P3.new3(atom.screenX, atom.group.chain.model.modelSet.viewer.getScreenHeight() - atom.screenY, atom.screenZ);
    case T.vibxyz:
      V3 v = atom.getVibrationVector();
      if (v == null)
        v = new V3();
      return v;
    case T.xyz:
      return atom;
    case T.color:
      return ColorUtil.colorPointFromInt2(
          atom.group.chain.model.modelSet.viewer.getColorArgbOrGray(atom.getColix())
          );
    }
    return null;
  }

  boolean isWithinStructure(EnumStructure type) {
    return group.isWithinStructure(type);
  }
  
  public int getOffsetResidueAtom(String name, int offset) {
    return group.chain.model.modelSet.getGroupAtom(this, offset, name);
  }
  
  public boolean isCrossLinked(JmolNode node) {
    return group.isCrossLinked(((Atom) node).getGroup());
  }

  public boolean getCrossLinkLeadAtomIndexes(JmolList<Integer> vReturn) {
    return group.getCrossLinkLead(vReturn);
  }
  
  @Override
  public String toString() {
    return getInfo();
  }

  public boolean isWithinFourBonds(Atom atomOther) {
    if (modelIndex != atomOther.modelIndex)
      return  false;
    if (isCovalentlyBonded(atomOther))
      return true; 
    Bond[] bondsOther = atomOther.bonds;
    for (int i = 0; i < bondsOther.length; i++) {
      Atom atom2 = bondsOther[i].getOtherAtom(atomOther);
      if (isCovalentlyBonded(atom2))
        return true;
      for (int j = 0; j < bonds.length; j++)
        if (bonds[j].getOtherAtom(this).isCovalentlyBonded(atom2))
          return true;
    }
    return false;
  }

  public BS findAtomsLike(String atomExpression) {
    // for SMARTS searching
    return group.chain.model.modelSet.viewer.getAtomBitSet(atomExpression);
  }

}
