/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 * Copyright (C) 2009  Piero Canepa, University of Kent, UK
 *
 * Contact: pc229@kent.ac.uk or pieremanuele.canepa@gmail.com
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
 *
 */

package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.util.BS;
import org.jmol.util.Eigen;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.P3;
import org.jmol.util.Quaternion;
import org.jmol.util.SB;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;

import org.jmol.util.JmolList;
import java.util.Arrays;




/**
 * 
 * A reader of OUT and OUTP files for CRYSTAL
 * 
 * http://www.crystal.unito.it/
 * 
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.4
 * 
 * 
 *          for a specific model in the set, use
 * 
 *          load "xxx.out" n
 * 
 *          as for all readers, where n is an integer > 0
 * 
 *          for final optimized geometry use
 * 
 *          load "xxx.out" 0
 * 
 *          (that is, "read the last model") as for all readers
 * 
 *          for conventional unit cell -- input coordinates only, use
 * 
 *          load "xxx.out" filter "conventional"
 * 
 *          to NOT load vibrations, use
 * 
 *          load "xxx.out" FILTER "novibrations"
 * 
 *          to load just the input deck exactly as indicated, use
 * 
 *          load "xxx.out" FILTER "input"
 * 
 *          now allows reading of frequencies and atomic values with
 *          conventional as long as this is not an optimization.
 * 
 * 
 * 
 * 
 */

public class CrystalReader extends AtomSetCollectionReader {

  private boolean isVersion3;
  private boolean isPrimitive;
  private boolean isPolymer;
  private boolean isSlab;
  private boolean isMolecular;
  private boolean haveCharges;
  private boolean isFreqCalc;
  private boolean inputOnly;
  private boolean isLongMode;
  private boolean getLastConventional;
  private boolean havePrimitiveMapping;
  private boolean isProperties;

  private int atomCount;
  private int atomIndexLast;
  private int[] atomFrag;
  private int[] primitiveToIndex;
  private float[] nuclearCharges;
  private JmolList<String> vInputCoords;

  private Double energy;
  private P3 ptOriginShift = new P3();
  private Matrix3f primitiveToCryst;
  private V3[] directLatticeVectors;
  private String spaceGroupName;

  @Override
  protected void initializeReader() throws Exception {
    doProcessLines = false;
    inputOnly = checkFilterKey("INPUT");
    isPrimitive = !inputOnly && !checkFilterKey("CONV");
    addVibrations &= !inputOnly && isPrimitive; //
    getLastConventional = (!isPrimitive && desiredModelNumber == 0);
    setFractionalCoordinates(readHeader());
  }

  @Override
  protected boolean checkLine() throws Exception {

    if (line.startsWith(" LATTICE PARAMETER")) {
      boolean isConvLattice = (line.indexOf("- CONVENTIONAL") >= 0);
      if (isConvLattice) {
        // skip if we want primitive and this is the conventional lattice
        if (isPrimitive)
          return true;
        readCellParams(true);
      } else if (!isPrimitive && !havePrimitiveMapping && !getLastConventional) {
        if (readPrimitiveMapping())
          return true; // just for properties
        // no input coordinates -- continue;
      }
      readCellParams(true);
      if (!isPrimitive) {
        discardLinesUntilContains(" TRANSFORMATION");
        readTransformationMatrix();
        discardLinesUntilContains(" CRYSTALLOGRAPHIC");
        readCellParams(false);
        discardLinesUntilContains(" CRYSTALLOGRAPHIC");
        readCrystallographicCoords();
        if (modelNumber == 1) {
          // done here
        } else if (!isFreqCalc) {
          // conventional cell and now the lattice has changed.
          // ignore? Can we convert a new primitive cell to conventional cell?
          //continuing = false;
          //Logger.error("Ignoring structure " + modelNumber + " due to FILTER \"conventional\"");
          //return true;
        }
        if (!getLastConventional) {
          if (!doGetModel(++modelNumber, null)) {
            vInputCoords = null;
            checkLastModel();
          }
          processInputCoords();
        }
      }
      return true;
    }

    if (isPrimitive) {
      if (line.indexOf("VOLUME=") >= 0 && line.indexOf("- DENSITY") >= 0)
        return readVolumePrimCell();
      //if (line.startsWith(" TRANSFORMATION MATRIX"))
      //return getOrientationMatrix();      
    } else {
      if (line.startsWith(" SHIFT OF THE ORIGIN"))
        return readShift();
      if (line.startsWith(" INPUT COORDINATES")) {
        readCrystallographicCoords(); // note, these will not be the full set of atoms, so we IGNORE VIBRATIONS
        if (inputOnly)
          continuing = false;
        return true;
      }
    }

    if (line
        .startsWith(" DIRECT LATTICE VECTOR"))
      return setDirect();

    if (line.indexOf("DIMENSIONALITY OF THE SYSTEM") >= 0) {
      if (line.indexOf("2") >= 0)
        isSlab = true;
      if (line.indexOf("1") >= 0)
        isPolymer = true;
      return true;
    }

    if (line.indexOf("FRQFRQ") >= 0) {
      isFreqCalc = true;
      return true;
    }

    if (line.startsWith(" FREQUENCIES COMPUTED ON A FRAGMENT"))
      return readFragments();

    if (line.indexOf("CONSTRUCTION OF A NANOTUBE FROM A SLAB") >= 0) {
      isPolymer = true;
      isSlab = false;
      return true;
    }

    if (line.indexOf("* CLUSTER CALCULATION") >= 0) {
      isMolecular = true;
      isSlab = false;
      isPolymer = false;
      return true;
    }

    if ((isPrimitive && line.startsWith(" ATOMS IN THE ASYMMETRIC UNIT"))
        || isProperties && line.startsWith("   ATOM N.AT.")) {
      if (!doGetModel(++modelNumber, null))
        return checkLastModel();
      return readAtoms();
    }

    if (!doProcessLines)
      return true;

    if (line.startsWith(" TOTAL ENERGY")) {
      readEnergy();
      readLine();
      if (line.startsWith(" ********"))
        discardLinesUntilContains("SYMMETRY ALLOWED");
      else if (line.startsWith(" TTTTTTTT"))
        discardLinesUntilContains2("PREDICTED ENERGY CHANGE", "HHHHHHH");
      return true;
    }

    if (line.startsWith(" TYPE OF CALCULATION")) {
      calculationType = line.substring(line.indexOf(":") + 1).trim();
      return true;
    }

    if (line.startsWith(" MULLIKEN POPULATION ANALYSIS"))
      return readPartialCharges();

    if (line.startsWith(" TOTAL ATOMIC CHARGES"))
      return readTotalAtomicCharges();

    if (addVibrations
        && line.contains(isVersion3 ? "EIGENVALUES (EV) OF THE MASS"
            : "EIGENVALUES (EIGV) OF THE MASS")
            || line.indexOf("LONGITUDINAL OPTICAL (LO)") >= 0) {
      if (vInputCoords != null)
        processInputCoords();
      isLongMode = (line.indexOf("LONGITUDINAL OPTICAL (LO)") >= 0);
      return readFrequencies();
    }

    if (line.startsWith(" MAX GRADIENT"))
      return readGradient();

    if (line.startsWith(" ATOMIC SPINS SET"))
      return readSpins();

    if (line.startsWith(" TOTAL ATOMIC SPINS  :"))
      return readMagneticMoments();

    if (!isProperties)
      return true;
    
    /// From here on we are considering only keywords of properties output files

    if (line.startsWith(" DEFINITION OF TRACELESS"))
      return getPropertyTensors();
   
    if (line.startsWith(" MULTIPOLE ANALYSIS BY ATOMS")) {
      appendLoadNote("Multipole Analysis");
      return true;
    }
    
    return true;
  }

  @Override
  protected void finalizeReader() throws Exception {
    if (vInputCoords != null)
      processInputCoords();
    if (energy != null)
      setEnergy();
    super.finalizeReader();
  }

  /*
  DIRECT LATTICE VECTORS CARTESIAN COMPONENTS (ANGSTROM)
          X                    Y                    Z
   0.290663292155E+01   0.000000000000E+00   0.460469095849E+01
  -0.145331646077E+01   0.251721794953E+01   0.460469095849E+01
  -0.145331646077E+01  -0.251721794953E+01   0.460469095849E+01
  
  or
  
   DIRECT LATTICE VECTOR COMPONENTS (BOHR)
        11.12550    0.00000    0.00000
         0.00000   10.45091    0.00000
         0.00000    0.00000    8.90375


   */

  private boolean setDirect() throws Exception {
    boolean isBohr = (line.indexOf("(BOHR") >= 0);
    directLatticeVectors = read3Vectors(isBohr);
    if (Logger.debugging) {
      addJmolScript("draw va vector {0 0 0} "
          + Escape.eP(directLatticeVectors[0]) + " color red");
      if (!isPolymer) {
        addJmolScript("draw vb vector {0 0 0} "
            + Escape.eP(directLatticeVectors[1]) + " color green");
        if (!isSlab)
          addJmolScript("draw vc vector {0 0 0} "
              + Escape.eP(directLatticeVectors[2]) + " color blue");
      }
    }
    V3 a = new V3();
    V3 b = new V3();
    if (isPrimitive) {
      a = directLatticeVectors[0];
      b = directLatticeVectors[1];
    } else {
      if (primitiveToCryst == null)
        return true;
      Matrix3f mp = new Matrix3f();
      mp.setColumnV(0, directLatticeVectors[0]);
      mp.setColumnV(1, directLatticeVectors[1]);
      mp.setColumnV(2, directLatticeVectors[2]);
      mp.mul(primitiveToCryst);
      a = new V3();
      b = new V3();
      mp.getColumnV(0, a);
      mp.getColumnV(1, b);
    }
    matUnitCellOrientation = Quaternion.getQuaternionFrame(new P3(), a, b)
    .getMatrix();
    Logger.info("oriented unit cell is in model "
        + atomSetCollection.getAtomSetCount());
    return !isProperties;
  }

  /*
   * 
  TRANSFORMATION MATRIX PRIMITIVE-CRYSTALLOGRAPHIC CELL
  1.0000  0.0000  1.0000 -1.0000  1.0000  1.0000  0.0000 -1.0000  1.0000

   */

  private void readTransformationMatrix() throws Exception {
    primitiveToCryst = Matrix3f.newA(fillFloatArray(null, 0, new float[9]));
  }

  private boolean readShift() {
    //  SHIFT OF THE ORIGIN                  :    3/4    1/4      0
    String[] tokens = getTokens();
    int pt = tokens.length - 3;
    ptOriginShift.set(fraction(tokens[pt++]), fraction(tokens[pt++]),
        fraction(tokens[pt]));
    return true;
  }

  private float fraction(String f) {
    String[] ab = TextFormat.split(f, '/');
    return (ab.length == 2 ? parseFloatStr(ab[0]) / parseFloatStr(ab[1]) : 0);
  }

  private boolean readGradient() throws Exception {
    /*MAX GRADIENT      0.000967  THRESHOLD             
      RMS GRADIENT      0.000967  THRESHOLD              
      MAX DISPLAC.      0.005733  THRESHOLD             
      RMS DISPLAC.      0.005733  THRESHOLD */

    String key = null;
    while (line != null) {
      String[] tokens = getTokens();
      if (line.indexOf("MAX GRAD") >= 0)
        key = "maxGradient";
      else if (line.indexOf("RMS GRAD") >= 0)
        key = "rmsGradient";
      else if (line.indexOf("MAX DISP") >= 0)
        key = "maxDisplacement";
      else if (line.indexOf("RMS DISP") >= 0)
        key = "rmsDisplacement";
      else
        break;
      if (atomSetCollection.getAtomCount() > 0)
        atomSetCollection.setAtomSetModelProperty(key, tokens[2]);
      readLine();
    }
    return true;
  }

  private boolean readVolumePrimCell() {
    // line looks like:  PRIMITIVE CELL - CENTRING CODE 1/0 VOLUME=   113.054442 - DENSITY 2.642 g/cm^3
    String[] tokens = getTokensStr(line);
    String volumePrim = tokens[7];
    // this is to avoid misreading 
    //PRIMITIVE CELL - CENTRING CODE 5/0 VOLUME=    30.176529 - DENSITY11.444 g/cm^3
    if (tokens[9].length() > 7) {
      line = TextFormat.simpleReplace(line, "DENSITY", "DENSITY ");
    }
    String densityPrim = tokens[10];
    atomSetCollection.setAtomSetModelProperty("volumePrimitive",
        TextFormat.formatDecimal(parseFloatStr(volumePrim), 3));
    atomSetCollection.setAtomSetModelProperty("densityPrimitive",
        TextFormat.formatDecimal(parseFloatStr(densityPrim), 3));
    return true;
  }

  /*
  SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  ATOMIC SPINS SET TO (ATOM, AT. N., SPIN)
    1  26-1   2   8 0   3   8 0   4   8 0   5  26 1   6  26 1   7   8 0   8   8 0
    9   8 0  10  26-1  11  26-1  12   8 0  13   8 0  14   8 0  15  26 1  16  26 1
   17   8 0  18   8 0  19   8 0  20  26-1  21  26-1  22   8 0  23   8 0  24   8 0
   25  26 1  26  26 1  27   8 0  28   8 0  29   8 0  30  26-1
  ALPHA-BETA ELECTRONS LOCKED TO   0 FOR  50 SCF CYCLES

   */
  private boolean readSpins() throws Exception {
    String data = "";
    while (readLine() != null && line.indexOf("ALPHA") < 0)
      data += line;
    data = TextFormat.simpleReplace(data, "-", " -");
    setData("spin", data, 2, 3);
    return true;
  }

  private boolean readMagneticMoments() throws Exception {
    String data = "";
    while (readLine() != null && line.indexOf("TTTTTT") < 0)
      data += line;
    setData("magneticMoment", data, 0, 1);
    return true;
  }

  private void setData(String name, String data, int pt, int dp)
  throws Exception {
    if (vInputCoords != null)
      processInputCoords();
    String[] s = new String[atomCount];
    for (int i = 0; i < atomCount; i++)
      s[i] = "0";
    String[] tokens = getTokensStr(data);
    for (int i = 0; i < atomCount; i++, pt += dp) {
      int iConv = getAtomIndexFromPrimitiveIndex(i);
      if (iConv >= 0)
        s[iConv] = tokens[pt];
    }
    data = TextFormat.join(s, '\n', 0);
    atomSetCollection.setAtomSetAtomProperty(name, data, -1);
  }

  private boolean readHeader() throws Exception {
    discardLinesUntilContains("*                                CRYSTAL");
    
    isVersion3 = (line.indexOf("CRYSTAL03") >= 0);
    discardLinesUntilContains("EEEEEEEEEE");
    String name;
    if (readLine().length() == 0) {
      name = readLines(2).trim();
    } else {
      name = line.trim();
      readLine();
    }
    String type = readLine().trim();
    /*
     * This is when the initial geometry is read from an external file GEOMETRY
     * INPUT FROM EXTERNAL FILE (FORTRAN UNIT 34)
     */
    int pt = type.indexOf("- PROPERTIES"); 
    if (pt >= 0) {
      isProperties = true;
      type = type.substring(0, pt).trim();
    }
    if (type.indexOf("EXTERNAL FILE") >= 0) {
      type = readLine().trim();
      isPolymer = (type.equals("1D - POLYMER"));
      isSlab = (type.equals("2D - SLAB"));
    } else {
      isPolymer = (type.equals("POLYMER CALCULATION"));
      isSlab = (type.equals("SLAB CALCULATION"));
    }
    atomSetCollection.setCollectionName(name
        + (!isProperties && desiredModelNumber == 0 ? " (optimized)" : ""));
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("symmetryType", type);
    if ((isPolymer || isSlab) && !isPrimitive) {
      Logger.error("Cannot use FILTER \"conventional\" with POLYMER or SLAB");
      isPrimitive = true;
    }
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("unitCellType",
        (isPrimitive ? "primitive" : "conventional"));

    if (type.indexOf("MOLECULAR") >= 0) {
      isMolecular = doProcessLines = true;
      readLine();
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo(
          "molecularCalculationPointGroup",
          line.substring(line.indexOf(" OR ") + 4).trim());
      return false;
    }
    spaceGroupName = "P1";
    if (!isPrimitive) {
      readLines(5);
      pt = line.indexOf(":"); 
      if (pt >= 0)
        spaceGroupName =  line.substring(pt + 1).trim();
    }
    doApplySymmetry = isProperties;
    return !isProperties;
  }

  private void readCellParams(boolean isNewSet) throws Exception {
    float f = (line.indexOf("(BOHR") >= 0 ? ANGSTROMS_PER_BOHR : 1);
    if (isNewSet)
      newAtomSet();
    if (isPolymer && !isPrimitive) {
      setUnitCell(parseFloatStr(line.substring(line.indexOf("CELL") + 4)) * f, -1, -1, 90, 90, 90);
    } else {
      discardLinesUntilContains("GAMMA");
      String[] tokens = getTokensStr(readLine());
      if (isSlab) {
        if (isPrimitive)
          setUnitCell(parseFloatStr(tokens[0]) * f, parseFloatStr(tokens[1]) * f, -1,
              parseFloatStr(tokens[3]), parseFloatStr(tokens[4]),
              parseFloatStr(tokens[5]));
        else
          setUnitCell(parseFloatStr(tokens[0]) * f, parseFloatStr(tokens[1]) * f, -1, 90, 90,
              parseFloatStr(tokens[2]));
      } else if (isPolymer) {
        setUnitCell(parseFloatStr(tokens[0]) * f, -1, -1, parseFloatStr(tokens[3]),
            parseFloatStr(tokens[4]), parseFloatStr(tokens[5]));
      } else {
        setUnitCell(parseFloatStr(tokens[0]) * f, parseFloatStr(tokens[1]) * f,
            parseFloatStr(tokens[2]) * f, parseFloatStr(tokens[3]),
            parseFloatStr(tokens[4]), parseFloatStr(tokens[5]));
      }
    }
  }

  /**
   * create arrays that maps primitive atoms to conventional atoms in a 1:1
   * fashion. Creates:
   * 
   * int[] primitiveToIndex -- points to model-based atomIndex
   * 
   * @return TRUE
   * 
   * @throws Exception
   */
  private boolean readPrimitiveMapping() throws Exception {
    if (vInputCoords == null)
      return false;
    havePrimitiveMapping = true;
    BS bsInputAtomsIgnore = new BS();
    int n = vInputCoords.size();
    int[] indexToPrimitive = new int[n];
    primitiveToIndex = new int[n];
    for (int i = 0; i < n; i++)
      indexToPrimitive[i] = -1;

    readLines(3);
    while (readLine() != null && line.indexOf(" NOT IRREDUCIBLE") >= 0) {
      // example HA_BULK_PBE_FREQ.OUT
      // we remove unnecessary atoms. This is important, because
      // these won't get properties, and we don't know exactly which
      // other atom to associate with them.

      bsInputAtomsIgnore.set(parseIntRange(line, 21, 25) - 1);
      readLine();
    }

    // COORDINATES OF THE EQUIVALENT ATOMS
    readLines(3);
    int iPrim = 0;
    int nPrim = 0;
    while (readLine() != null && line.indexOf("NUMBER") < 0) {
      if (line.length() == 0)
        continue;
      nPrim++;
      int iAtom = parseIntRange(line, 4, 8) - 1;
      if (indexToPrimitive[iAtom] < 0) {
        // no other primitive atom is mapped to a given conventional atom.
        indexToPrimitive[iAtom] = iPrim++;
      }
    }

    if (bsInputAtomsIgnore.nextSetBit(0) >= 0)
      for (int i = n; --i >= 0;)
        if (bsInputAtomsIgnore.get(i))
          vInputCoords.remove(i);
    atomCount = vInputCoords.size();
    Logger.info(nPrim + " primitive atoms and " + atomCount
        + " conventionalAtoms");
    primitiveToIndex = new int[nPrim];
    for (int i = 0; i < nPrim; i++)
      primitiveToIndex[i] = -1;
    for (int i = atomCount; --i >= 0;) {
      iPrim = indexToPrimitive[parseIntStr(vInputCoords.get(i).substring(0, 4)) - 1];
      if (iPrim >= 0)
        primitiveToIndex[iPrim] = i;
    }
    return true;
  }

  /*
  ATOMS IN THE ASYMMETRIC UNIT    2 - ATOMS IN THE UNIT CELL:    4
     ATOM              X/A                 Y/B                 Z/C    
  *******************************************************************************
   1 T 282 PB    0.000000000000E+00  5.000000000000E-01  2.385000000000E-01
   2 F 282 PB    5.000000000000E-01  0.000000000000E+00 -2.385000000000E-01
   3 T   8 O     0.000000000000E+00  0.000000000000E+00  0.000000000000E+00
   4 F   8 O     5.000000000000E-01  5.000000000000E-01  0.000000000000E+00
   
   or
   
      ATOM N.AT.  SHELL    X(A)      Y(A)      Z(A)      EXAD       N.ELECT.
  *******************************************************************************
    1  282 PB    4     1.331    -0.077     1.178   1.934E-01       2.891
    2  282 PB    4    -1.331     0.077    -1.178   1.934E-01       2.891
    3  282 PB    4     1.331    -2.688    -1.178   1.934E-01       2.891
    4  282 PB    4    -1.331     2.688     1.178   1.934E-01       2.891
    5    8 O     5    -0.786     0.522     1.178   6.500E-01       9.109
    6    8 O     5     0.786    -0.522    -1.178   6.500E-01       9.109
    7    8 O     5    -0.786     2.243    -1.178   6.500E-01       9.109
    8    8 O     5     0.786    -2.243     1.178   6.500E-01       9.109

   */
  private boolean readAtoms() throws Exception {
    if (isMolecular)
      newAtomSet();
    while (readLine() != null && line.indexOf("*") < 0) {
      if (line.indexOf("X(ANGSTROM") >= 0) {
        // fullerene from slab has this.
        setFractionalCoordinates(false);
        isMolecular = true;
      }
    }
    int i = atomIndexLast;
    // I turned off normalization -- proper way to do this is to 
    // add the "packed" keyword. As it was, it was impossible to
    // load the file with its original coordinates, which in many
    // cases are VERY interesting and far better (in my opinion!)
    
    boolean doNormalizePrimitive = false;// && isPrimitive && !isMolecular && !isPolymer && !isSlab && (!doApplySymmetry || latticeCells[2] != 0);
    atomIndexLast = atomSetCollection.getAtomCount();

    while (readLine() != null && line.length() > 0 && line.indexOf(isPrimitive ? "*" : "=") < 0) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      int pt = (isProperties ? 1 : 2);
      atom.elementSymbol = getElementSymbol(getAtomicNumber(tokens[pt++]));
      atom.atomName = getAtomName(tokens[pt++]);
      if (isProperties)
        pt++; // skip SHELL
      float x = parseFloatStr(tokens[pt++]);
      float y = parseFloatStr(tokens[pt++]);
      float z = parseFloatStr(tokens[pt]);
      if (haveCharges)
        atom.partialCharge = atomSetCollection.getAtom(i++).partialCharge;
      if (iHaveFractionalCoordinates && !isProperties) {
        // note: this normalization is unique to this reader -- all other
        //       readers operate through symmetry application
        if (x < 0 && (isPolymer || isSlab || doNormalizePrimitive))
          x += 1;
        if (y < 0 && (isSlab || doNormalizePrimitive))
          y += 1;
        if (z < 0 && doNormalizePrimitive)
          z += 1;
      }
      setAtomCoordXYZ(atom, x, y, z);
    }
    atomCount = atomSetCollection.getAtomCount() - atomIndexLast;
    return true;
  }

  private String getAtomName(String s) {
    String atomName = s;
    if (atomName.length() > 1 && Character.isLetter(atomName.charAt(1)))
      atomName = atomName.substring(0, 1)
      + Character.toLowerCase(atomName.charAt(1)) + atomName.substring(2);
    return atomName;
  }

  /*
   * Crystal adds 100 to the atomic number when the same atom will be described
   * with different basis sets. It also adds 200 when ECP are used:
   * 
   * 1 T 282 PB 0.000000000000E+00 0.000000000000E+00 0.000000000000E+00 2 T 16
   * S -5.000000000000E-01 -5.000000000000E-01 -5.000000000000E-01
   */
  private int getAtomicNumber(String token) {
    int atomicNumber = parseIntStr(token);
    while (atomicNumber >= 100)
      atomicNumber -= 100;
    return atomicNumber;
  }

  /*
   * INPUT COORDINATES
   * 
   * ATOM AT. N. COORDINATES 
   * 1 12 0.000000000000E+00 0.000000000000E+00 0.000000000000E+00 
   * 2  8 5.000000000000E-01 5.000000000000E-01 5.000000000000E-01
   */
  private void readCrystallographicCoords() throws Exception {
    // we only store them, because we may want to delete some
    readLine();
    readLine();
    vInputCoords = new  JmolList<String>();
    while (readLine() != null && line.length() > 0)
      vInputCoords.addLast(line);
  }

  private void processInputCoords() throws Exception {
    // here we may have deleted unnecessary input coordinates
    atomCount = vInputCoords.size();
    for (int i = 0; i < atomCount; i++) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokensStr(vInputCoords.get(i));
      int atomicNumber, offset;
      if (tokens.length == 7) {
        atomicNumber = getAtomicNumber(tokens[2]);
        offset = 2;
      } else {
        atomicNumber = getAtomicNumber(tokens[1]);
        offset = 0;
      }
      float x = parseFloatStr(tokens[2 + offset]) + ptOriginShift.x;
      float y = parseFloatStr(tokens[3 + offset]) + ptOriginShift.y;
      float z = parseFloatStr(tokens[4 + offset]) + ptOriginShift.z;
      /*
       * we do not do this, because we have other ways to do it namely, "packed"
       * or "{555 555 1}" In this way, we can check those input coordinates
       * exactly
       * 
       * if (x < 0) x += 1; if (y < 0) y += 1; if (z < 0) z += 1;
       */

      setAtomCoordXYZ(atom, x, y, z);
      atom.elementSymbol = getElementSymbol(atomicNumber);
    }
    vInputCoords = null;
  }

  private void newAtomSet() throws Exception {
    if (atomSetCollection.getAtomCount() > 0) {
      applySymmetryAndSetTrajectory();
      atomSetCollection.newAtomSet();
    }
    if (spaceGroupName != null)
      setSpaceGroupName(spaceGroupName);
  }

  private void readEnergy() {
    line = TextFormat.simpleReplace(line, "( ", "(");
    String[] tokens = getTokens();
    energy = Double.valueOf(Double.parseDouble(tokens[2]));
    setEnergy();
  }

  private void setEnergy() {
    atomSetCollection.setAtomSetEnergy("" + energy, energy.floatValue());
    atomSetCollection.setAtomSetAuxiliaryInfo("Energy", energy);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("Energy", energy);
    atomSetCollection.setAtomSetName("Energy = " + energy + " Hartree");
  }

  /*
   * MULLIKEN POPULATION ANALYSIS - NO. OF ELECTRONS 152.000000
   * 
   * ATOM Z CHARGE A.O. POPULATION
   * 
   * 1 FE 26 23.991 2.000 1.920 2.057 2.057 2.057 0.384 0.674 0.674
   */
  private boolean readPartialCharges() throws Exception {
    if (haveCharges || atomSetCollection.getAtomCount() == 0)
      return true;
    haveCharges = true;
    readLines(3);
    Atom[] atoms = atomSetCollection.getAtoms();
    int i0 = atomSetCollection.getLastAtomSetAtomIndex();
    int iPrim = 0;
    while (readLine() != null && line.length() > 3)
      if (line.charAt(3) != ' ') {
        int iConv = getAtomIndexFromPrimitiveIndex(iPrim);
        if (iConv >= 0)
          atoms[i0 + iConv].partialCharge = parseFloatRange(line, 9, 11)
          - parseFloatRange(line, 12, 18);
        iPrim++;
      }
    return true;
  }

  private boolean readTotalAtomicCharges() throws Exception {
    SB data = new SB();
    while (readLine() != null && line.indexOf("T") < 0)
      // TTTTT or SUMMED SPIN DENSITY
      data.append(line);
    String[] tokens = getTokensStr(data.toString());
    float[] charges = new float[tokens.length];
    if (nuclearCharges == null)
      nuclearCharges = charges;
    if (atomSetCollection.getAtomCount() == 0)
      return true;
    Atom[] atoms = atomSetCollection.getAtoms();
    int i0 = atomSetCollection.getLastAtomSetAtomIndex();
    for (int i = 0; i < charges.length; i++) {
      int iConv = getAtomIndexFromPrimitiveIndex(i);
      if (iConv >= 0) {
        charges[i] = parseFloatStr(tokens[i]);
        atoms[i0 + iConv].partialCharge = nuclearCharges[i] - charges[i];
      }
    }
    return true;
  }

  private int getAtomIndexFromPrimitiveIndex(int iPrim) {
    return (primitiveToIndex == null ? iPrim : primitiveToIndex[iPrim]);
  }

  private boolean readFragments() throws Exception {
    /*
     *   2 (   8 O )     3 (   8 O )     4 (   8 O )    85 (   8 O )    86 (   8 O ) 
     *  87(   8 O )    89(   6 C )    90(   8 O )    91(   8 O )    92(   1 H ) 
     *  93(   1 H )    94(   6 C )    95(   1 H )    96(   8 O )    97(   1 H ) 
     *  98(   8 O )    99(   6 C )   100(   8 O )   101(   8 O )   102(   1 H ) 
     * 103(   1 H )   104(   6 C )   105(   1 H )   106(   8 O )   107(   8 O ) 
     * 108(   1 H )   109(   6 C )   110(   1 H )   111(   8 O )   112(   8 O ) 
     * 113(   1 H )   114(   6 C )   115(   1 H )   116(   8 O )   117(   8 O ) 
     * 118(   1 H ) 
     * 
     */
    int numAtomsFrag = parseIntRange(line, 39, 44);
    if (numAtomsFrag < 0)
      return true;
    atomFrag = new int[numAtomsFrag];
    String Sfrag = "";
    while (readLine() != null && line.indexOf("(") >= 0)
      Sfrag += line;
    Sfrag = TextFormat.simpleReplace(Sfrag, "(", " ");
    Sfrag = TextFormat.simpleReplace(Sfrag, ")", " ");
    String[] tokens = getTokensStr(Sfrag);
    for (int i = 0, pos = 0; i < numAtomsFrag; i++, pos += 3)
      atomFrag[i] = getAtomIndexFromPrimitiveIndex(parseIntStr(tokens[pos]) - 1);

    Arrays.sort(atomFrag); // the frequency module needs these sorted

    // note: atomFrag[i] will be -1 if this atom is being ignored due to FILTER "conventional"

    return true;
  }

  /* 
   * Transverse:
   * 
  0         1         2         3         4         5         6         7         
  01234567890123456789012345678901234567890123456789012345678901234567890123456789
      MODES          EV           FREQUENCIES     IRREP  IR   INTENS    RAMAN
                    (AU)      (CM**-1)     (THZ)             (KM/MOL)
      1-   1   -0.00004031    -32.6352   -0.9784  (A2 )   I (     0.00)   A
      2-   2   -0.00003920    -32.1842   -0.9649  (B2 )   A (  6718.50)   A
      3-   3   -0.00000027     -2.6678   -0.0800  (A1 )   A (     3.62)   A
   * 
   * Longitudinal:
   * 
   *
  0         1         2         3         4         5         6         7         
  01234567890123456789012345678901234567890123456789012345678901234567890123456789
      MODES         EIGV          FREQUENCIES    IRREP IR INTENS       SHIFTS
               (HARTREE**2)   (CM**-1)     (THZ)       (KM/MOL)  (CM**-1)   (THZ)
      4-   6    0.2370E-06    106.8457    3.2032 (F1U)     40.2    7.382   0.2213
     16-  18    0.4250E-06    143.0817    4.2895 (F1U)    181.4   14.234   0.4267
     31-  33    0.5848E-06    167.8338    5.0315 (F1U)     24.5    1.250   0.0375
     41-  43    0.9004E-06    208.2551    6.2433 (F1U)    244.7   10.821   0.3244
   */
  private boolean readFrequencies() throws Exception {

    energy = null; // don't set energy for these models
    discardLinesUntilContains("MODES");
    // This line is always there
    boolean haveIntensities = (line.indexOf("INTENS") >= 0);
    readLine();
    JmolList<String[]> vData = new  JmolList<String[]>();
    int freqAtomCount = atomCount;
    while (readLine() != null && line.length() > 0) {
      int i0 = parseIntRange(line, 1, 5);
      int i1 = parseIntRange(line, 6, 10);
      String irrep = (isLongMode ? line.substring(48, 51) : line.substring(49,
          52)).trim();
      String intens = (!haveIntensities ? "not available" : (isLongMode ? line
          .substring(53, 61) : line.substring(59, 69).replace(')', ' ')).trim());

      // not all crystal calculations include intensities values
      // this feature is activated when the keyword INTENS is on the input

      String irActivity = (isLongMode ? "A" : line.substring(55, 58).trim());
      String ramanActivity = (isLongMode ? "I" : line.substring(71, 73).trim());

      String[] data = new String[] { irrep, intens, irActivity, ramanActivity };
      for (int i = i0; i <= i1; i++)
        vData.addLast(data);
    }
    discardLinesUntilContains(isLongMode ? "LO MODES FOR IRREP"
        : isVersion3 ? "THE CORRESPONDING MODES"
            : "NORMAL MODES NORMALIZED TO CLASSICAL AMPLITUDES");
    readLine();
    int lastAtomCount = -1;
    while (readLine() != null && line.startsWith(" FREQ(CM**-1)")) {
      String[] tokens = getTokensStr(line.substring(15));
      float[] frequencies = new float[tokens.length];
      int frequencyCount = frequencies.length;
      for (int i = 0; i < frequencyCount; i++) {
        frequencies[i] = parseFloatStr(tokens[i]);
        if (Logger.debugging)
          Logger.debug((vibrationNumber + i) + " frequency=" + frequencies[i]);
      }
      boolean[] ignore = new boolean[frequencyCount];
      int iAtom0 = 0;
      int nData = vData.size();
      for (int i = 0; i < frequencyCount; i++) {
        tokens = vData.get(vibrationNumber % nData);
        ignore[i] = (!doGetVibration(++vibrationNumber) || tokens == null);
        if (ignore[i])
          continue;
        applySymmetryAndSetTrajectory();
        lastAtomCount = cloneLastAtomSet(atomCount, null);
        if (i == 0)
          iAtom0 = atomSetCollection.getLastAtomSetAtomIndex();
        setFreqValue(frequencies[i], tokens);
      }
      readLine();
      fillFrequencyData(iAtom0, freqAtomCount, lastAtomCount, ignore, false,
          14, 10, atomFrag, 0);
      readLine();
    }
    return true;
  }

  private void setFreqValue(float freq, String[] data) {
    String activity = "IR: " + data[2] + ", Ram.: " + data[3];
    atomSetCollection.setAtomSetFrequency(null, activity, "" + freq, null);
    atomSetCollection.setAtomSetModelProperty("IRintensity", data[1] + " km/Mole");
    atomSetCollection.setAtomSetModelProperty("vibrationalSymmetry", data[0]);
    atomSetCollection.setAtomSetModelProperty("IRactivity", data[2]);
    atomSetCollection.setAtomSetModelProperty("Ramanactivity", data[3]);
    atomSetCollection.setAtomSetName((isLongMode ? "LO " : "") + data[0] + " "
        + TextFormat.formatDecimal(freq, 2) + " cm-1 ("
        + TextFormat.formatDecimal(Float.parseFloat(data[1]), 0)
        + " km/Mole), " + activity);
  }

  /*
  DEFINITION OF TRACELESS QUADRUPOLE TENSORS:

  (3XX-RR)/2=(2,2)/4-(2,0)/2
  (3YY-RR)/2=-(2,2)/4-(2,0)/2
  (3ZZ-RR)/2=(2,0)
  3XY/2=(2,-2)/4     3XZ/2=(2,1)/2     3YZ/2=(2,-1)/2

  *** ATOM N.     1 (Z=282) PB

  TENSOR IN PRINCIPAL AXIS SYSTEM
  AA  6.265724E-01 BB -2.651563E-01 CC -3.614161E-01

  *** ATOM N.     2 (Z=282) PB

  TENSOR IN PRINCIPAL AXIS SYSTEM
  AA  6.265724E-01 BB -2.651563E-01 CC -3.614161E-01

  *** ATOM N.     3 (Z=282) PB

  TENSOR IN PRINCIPAL AXIS SYSTEM
  AA  6.265724E-01 BB -2.651563E-01 CC -3.614161E-01

  *** ATOM N.     4 (Z=282) PB

  TENSOR IN PRINCIPAL AXIS SYSTEM
  AA  6.265724E-01 BB -2.651563E-01 CC -3.614161E-01

  *** ATOM N.     5 (Z=  8) O 

  TENSOR IN PRINCIPAL AXIS SYSTEM
  AA  3.258167E-01 BB -1.529525E-01 CC -1.728642E-01

  *** ATOM N.     6 (Z=  8) O 

  TENSOR IN PRINCIPAL AXIS SYSTEM
  AA  3.258167E-01 BB -1.529525E-01 CC -1.728642E-01

  *** ATOM N.     7 (Z=  8) O 

  TENSOR IN PRINCIPAL AXIS SYSTEM
  AA  3.258167E-01 BB -1.529525E-01 CC -1.728642E-01

  *** ATOM N.     8 (Z=  8) O 

  TENSOR IN PRINCIPAL AXIS SYSTEM
  AA  3.258167E-01 BB -1.529525E-01 CC -1.728642E-01
  TTTTTTTTTTTTTTTTTTTTTTTTTTTTTT POLI        TELAPSE        0.05 TCPU        0.04
    */
   private boolean getPropertyTensors() throws Exception {
     readLines(6);
     Atom[] atoms = atomSetCollection.getAtoms();
     while (readLine() != null  && line.startsWith(" *** ATOM")) {
       String[] tokens = getTokens();
       int index = parseIntStr(tokens[3]) - 1;
       tokens = getTokensStr(readLines(3));
       atoms[index].setEllipsoid(Eigen.getEllipsoid(directLatticeVectors, 
           new float[] {parseFloatStr(tokens[1]), 
           parseFloatStr(tokens[3]), 
           parseFloatStr(tokens[5]) }, false));
       readLine();
     }
     return true;
   }


}
