package org.jmol.adapter.readers.xtal;

/**
 * Piero Canepa
 * 
 * Quantum Espresso
 * http://www.quantum-espresso.org and http://qe-forge.org/frs/?group_id=10
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.0
 */

import org.jmol.util.TextFormat;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.util.Eigen;

public class MagResReader extends AtomSetCollectionReader {

  private float[] cellParams;
  private float maxIso = 10000;

  @Override
  protected void initializeReader() {
    setFractionalCoordinates(false);
    doApplySymmetry = false;
    atomSetCollection.newAtomSet();
  }

  @Override
  protected boolean checkLine() throws Exception {
    if (line.startsWith("lattice")) {
      readCellParams();
    } else if (line.contains("Coordinates")) {
        readAtom();
    } else if (line.contains("J-coupling Total") || line.contains("TOTAL tensor")) {
        readTensor();
    }
    return true;
  }


  private void readCellParams() throws Exception {
    String[] tokens = getTokens();
        cellParams = new float[9];
    for (int i = 0; i < 9; i++)
      cellParams[i] = parseFloatStr(tokens[i + 1]) * ANGSTROMS_PER_BOHR;
    addPrimitiveLatticeVector(0, cellParams, 0);
    addPrimitiveLatticeVector(1, cellParams, 3);
    addPrimitiveLatticeVector(2, cellParams, 6);
    setSpaceGroupName("P1");
  }

  /*
    C    1 Coordinates      2.054    0.000    0.000   A
   */

  private Atom atom;
  private void readAtom() throws Exception {
    float f = line.trim().endsWith("A") ? 1 : ANGSTROMS_PER_BOHR;
    String[] tokens = getTokens();
    atom = atomSetCollection.addNewAtom();
    atom.elementSymbol = tokens[0];
    atom.atomName = tokens[0] + tokens[1];
    float x = parseFloatStr(tokens[3]) * f;
    float y = parseFloatStr(tokens[4]) * f;
    float z = parseFloatStr(tokens[5]) * f;
    atom.set(x, y, z);
    setAtomCoord(atom);
  }

  /*
         J-coupling Total

  W    1 Eigenvalue  sigma_xx -412163.5628
  W    1 Eigenvector sigma_xx       0.1467     -0.9892      0.0000
  W    1 Eigenvalue  sigma_yy -412163.6752
  W    1 Eigenvector sigma_yy       0.9892      0.1467      0.0000
  W    1 Eigenvalue  sigma_zz -432981.4974
  W    1 Eigenvector sigma_zz       0.0000      0.0000      1.0000
  
TOTAL tensor

              -0.0216     -0.1561     -0.0137
              -0.1561     -0.1236     -0.0359
              -0.0137     -0.0359      0.1452

   */
  private void readTensor() throws Exception {
    boolean isJ = (line.indexOf("J-") >= 0);
    atomSetCollection.setAtomSetName(line.trim());
    float[] data = new float[9];
    readLine();
    String s = TextFormat.simpleReplace(readLine() + readLine() + readLine(), "-", " -");
    fillFloatArray(s, 0, data);
    float f = 3;
    if (isJ) {
      discardLinesUntilContains("Isotropic");
      float iso = parseFloatStr(getTokens()[3]);
      if (Math.abs(iso) > maxIso)
        return;
      f = 0.04f;
    }
    double[][] a = new double[3][3];
    for (int i = 0, pt = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
      a[i][j] = data[pt++];
    atom.setEllipsoid(Eigen.getEllipsoidDD(a));
    atom.ellipsoid[0].scale(f);
  }
}
