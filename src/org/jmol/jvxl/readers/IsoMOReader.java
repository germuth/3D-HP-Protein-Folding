/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import java.util.Map;
import java.util.Random;




import org.jmol.util.ArrayUtil;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.P3;
import org.jmol.util.TextFormat;
import org.jmol.util.V3;
import org.jmol.api.Interface;
import org.jmol.api.QuantumCalculationInterface;
import org.jmol.api.QuantumPlaneCalculationInterface;
import org.jmol.constant.EnumQuantumShell;
import org.jmol.jvxl.data.VolumeData;

class IsoMOReader extends AtomDataReader {

  private Random random;
  
  IsoMOReader(){}
  
  @Override
  void init(SurfaceGenerator sg) {
    super.init(sg);
    isNci = (params.qmOrbitalType == Parameters.QM_TYPE_NCI_PRO);
    if (isNci) {
      // NCI analysis org.jmol.quantum.NciCalculation
      // allows for progressive plane reading, which requires
      // isXLowToHigh to be TRUE
      isXLowToHigh = hasColorData = true;
      precalculateVoxelData = false; // will process as planes
      params.insideOut = !params.insideOut;
    }
  }
  
  /////// ab initio/semiempirical quantum mechanical orbitals ///////

  @Override
  @SuppressWarnings("unchecked")
  protected void setup(boolean isMapData) {
    mos = (JmolList<Map<String, Object>>) params.moData.get("mos");
    linearCombination = params.qm_moLinearCombination;
    Map<String, Object> mo = (mos != null && linearCombination == null ? mos
        .get(params.qm_moNumber - 1) : null);
    boolean haveVolumeData = params.moData.containsKey("haveVolumeData");
    if (haveVolumeData && mo != null)
      params.volumeData = (VolumeData) mo.get("volumeData");
    super.setup(isMapData);
    doAddHydrogens = false;
    getAtoms(params.bsSelected, doAddHydrogens, !isNci, isNci, isNci, false,
        false, params.qm_marginAngstroms);
    if (isNci)
      setHeader(
          "NCI (promolecular)",
          "see NCIPLOT: A Program for Plotting Noncovalent Interaction Regions, Julia Contreras-Garcia, et al., J. of Chemical Theory and Computation, 2011, 7, 625-632");
    else
      setHeader("MO", "calculation type: "
          + params.moData.get("calculationType"));
    setRanges(params.qm_ptsPerAngstrom, params.qm_gridMax, 0);
    String className = (isNci ? "quantum.NciCalculation"
        : "quantum.MOCalculation");
    if (haveVolumeData) {
      for (int i = params.title.length; --i >= 0;)
        fixTitleLine(i, mo);
    } else {
      q = (QuantumCalculationInterface) Interface.getOptionInterface(className);
      if (isNci) {
        qpc = (QuantumPlaneCalculationInterface) q;
      } else if (linearCombination == null) {
        for (int i = params.title.length; --i >= 0;)
          fixTitleLine(i, mo);
        coef = (float[]) mo.get("coefficients");
        dfCoefMaps = (int[][]) mo.get("dfCoefMaps");
      } else {
        coefs = ArrayUtil.newFloat2(mos.size());
        for (int i = 1; i < linearCombination.length; i += 2) {
          int j = (int) linearCombination[i];
          if (j > mos.size() || j < 1)
            return;
          coefs[j - 1] = (float[]) mos.get(j - 1).get("coefficients");
        }
        for (int i = params.title.length; --i >= 0;)
          fixTitleLine(i, null);
      }
      isElectronDensityCalc = (coef == null && linearCombination == null && !isNci);
    }
    volumeData.sr = null;
    if (isMapData && !isElectronDensityCalc && !haveVolumeData) {
      volumeData.doIterate = false;
      volumeData.setVoxelDataAsArray(voxelData = new float[1][1][1]);
      volumeData.sr = this;
      points = new P3[1];
      points[0] = new P3();
      if (!setupCalculation())
        q = null;
    } else if (params.psi_monteCarloCount > 0) {
      vertexDataOnly = true;
      random = new Random(params.randomSeed);
    }
  }
  
  @Override
  protected boolean readVolumeParameters(boolean isMapData) {
    setup(isMapData);
    if (volumeData.sr == null)
      initializeVolumetricData();
    return true;
  }

  private void fixTitleLine(int iLine, Map<String, Object> mo) {
    // see Parameters.Java for defaults here. 
    if (!fixTitleLine(iLine))
      return;
    String line = params.title[iLine];
    int pt = line.indexOf("%");
    if (line.length() == 0 || pt < 0)
      return;
    int rep = 0;
    if (line.indexOf("%F") >= 0)
      line = TextFormat.formatStringS(line, "F", params.fileName);
    if (line.indexOf("%I") >= 0)
      line = TextFormat.formatStringS(line, "I",
          params.qm_moLinearCombination == null ? "" + params.qm_moNumber
              : EnumQuantumShell.getMOString(params.qm_moLinearCombination));
    if (line.indexOf("%N") >= 0)
      line = TextFormat.formatStringS(line, "N", "" + params.qmOrbitalCount);
    Float energy = null;
    if (mo == null) {
      // check to see if all orbitals have the same energy
      for (int i = 0; i < linearCombination.length; i += 2)
        if (linearCombination[i] != 0) {
          mo = mos.get((int) linearCombination[i + 1] - 1);
          Float e = (Float) mo.get("energy");
          if (energy == null) { 
            if (e == null)
              break;
            energy = e;
          } else if (!energy.equals(e)) {
            energy = null;
            break;
          }
        }
    } else {
      if (mo.containsKey("energy"))
        energy = (Float) mo.get("energy");
    }

    if (line.indexOf("%E") >= 0)
      line = TextFormat.formatStringS(line, "E",
          energy != null && ++rep != 0 ? "" + energy : "");
    if (line.indexOf("%U") >= 0)
      line = TextFormat.formatStringS(line, "U",
          energy != null && params.moData.containsKey("energyUnits")
              && ++rep != 0 ? (String) params.moData.get("energyUnits") : "");
    if (line.indexOf("%S") >= 0)
      line = TextFormat.formatStringS(line, "S", mo != null
          && mo.containsKey("symmetry") && ++rep != 0 ? "" + mo.get("symmetry")
          : "");
    if (line.indexOf("%O") >= 0)
      line = TextFormat.formatStringS(line, "O", mo != null
          && mo.containsKey("occupancy") && ++rep != 0 ? ""
          + mo.get("occupancy") : "");
    if (line.indexOf("%T") >= 0)
      line = TextFormat.formatStringS(line, "T", mo != null
          && mo.containsKey("type") && ++rep != 0 ? "" + mo.get("type") : "");
    if (line.equals("string")) {
      params.title[iLine] = "";
      return;
    }
    boolean isOptional = (line.indexOf("?") == 0);
    params.title[iLine] = (!isOptional ? line : rep > 0
        && !line.trim().endsWith("=") ? line.substring(1) : "");
  }
  
  private final float[] vDist = new float[3];
  
  @Override
  protected void readSurfaceData(boolean isMapData) throws Exception {
    if (volumeData.sr != null)
      return;
    if (params.psi_monteCarloCount <= 0) {
      super.readSurfaceData(isMapData);
      return;
    }
    if (points != null)
      return; // already done
    points = new P3[1000];
    for (int j = 0; j < 1000; j++)
      points[j] = new P3();
    if (params.thePlane != null)
      vTemp = new V3();
    // presumes orthogonal
    for (int i = 0; i < 3; i++)
      vDist[i] = volumeData.volumetricVectorLengths[i]
          * volumeData.voxelCounts[i];
    volumeData.setVoxelDataAsArray(voxelData = new float[1000][1][1]);
    getValues();
    float value;
    float f = 0;
    for (int j = 0; j < 1000; j++)
      if ((value = Math.abs(voxelData[j][0][0])) > f)
        f = value;
    if (f < 0.0001f)
      return;
    //minMax = new float[] {(params.mappedDataMin  = -f / 2), 
    //(params.mappedDataMax = f / 2)};
    for (int i = 0; i < params.psi_monteCarloCount;) {
      getValues();
      for (int j = 0; j < 1000; j++) {
        value = voxelData[j][0][0];
        float absValue = Math.abs(value);
        if (absValue <= getRnd(f))
          continue;
        addVC(points[j], value, 0);
        if (++i == params.psi_monteCarloCount)
          break;
      }
    }
  }

  @Override
  protected void postProcessVertices() {
    // not clear that this is a good idea...
    //if (params.thePlane != null && params.slabInfo == null)
      //params.addSlabInfo(MeshSurface.getSlabWithinRange(0.01f, -0.01f));
      
  }

  private void getValues() {        
    for (int j = 0; j < 1000; j++) {
      voxelData[j][0][0] = 0;
      points[j].set(volumeData.volumetricOrigin.x + getRnd(vDist[0]), 
          volumeData.volumetricOrigin.y + getRnd(vDist[1]), 
          volumeData.volumetricOrigin.z + getRnd(vDist[2]));
      if (params.thePlane != null)
        Measure.getPlaneProjection(points[j], params.thePlane, points[j], vTemp);
    }
    createOrbital();
  }

  @Override
  public float getValueAtPoint(P3 pt) {
    return (q == null ? 0 : q.process(pt));
  }
  

  private float getRnd(float f) {
    return random.nextFloat() * f;
  }

  //mapping mos fails
  
  @Override
  protected void generateCube() {
    if (params.volumeData != null)
      return;
    newVoxelDataCube();
    createOrbital();
  }

  private P3[] points;
  private V3 vTemp;
  QuantumCalculationInterface q;
  JmolList<Map<String, Object>> mos;
  boolean isNci;
  float[] coef; 
  int[][] dfCoefMaps;
  float[] linearCombination;
  float[][] coefs;
  private boolean isElectronDensityCalc;
  
  protected void createOrbital() {
    boolean isMonteCarlo = (params.psi_monteCarloCount > 0);
    if (isElectronDensityCalc) {
      // electron density calc
      if (mos == null || isMonteCarlo)
        return;
      for (int i = params.qm_moNumber; --i >= 0; ) {
        Logger.info(" generating isosurface data for MO " + (i + 1));
        Map<String, Object> mo = mos.get(i);
        coef = (float[]) mo.get("coefficients");
        dfCoefMaps = (int[][]) mo.get("dfCoefMaps");
        if (!setupCalculation())
          return;
        q.createCube();
      }
    } else {
      if (!isMonteCarlo)
        Logger.info("generating isosurface data for MO using cutoff " + params.cutoff);
      if (!setupCalculation())
        return;
      q.createCube();
    }
  }
  
  @Override
  public float[] getPlane(int x) {
    if (!qSetupDone) 
      setupCalculation();
    return getPlane2(x); 
  }

  private boolean qSetupDone;
  
  @SuppressWarnings("unchecked")
  private boolean setupCalculation() {
    qSetupDone = true;
    switch (params.qmOrbitalType) {
    case Parameters.QM_TYPE_VOLUME_DATA:
      break;
    case Parameters.QM_TYPE_GAUSSIAN:
      return q.setupCalculation(volumeData, bsMySelected, null, null, (String) params.moData
                      .get("calculationType"),
          atomData.atomXyz, atomData.firstAtomIndex, (JmolList<int[]>) params.moData.get("shells"), (float[][]) params.moData
                          .get("gaussians"), dfCoefMaps, null,
          coef, linearCombination, params.isSquaredLinear, coefs,
          null, params.moData.get("isNormalized") == null, points, params.parameters, params.testFlags);
    case Parameters.QM_TYPE_SLATER:
      return q.setupCalculation(volumeData, bsMySelected, null, null, (String) params.moData
                      .get("calculationType"),
          atomData.atomXyz, atomData.firstAtomIndex, null, null, null, params.moData.get("slaters"),
          coef, linearCombination, params.isSquaredLinear, coefs, 
          null, true, points, params.parameters, params.testFlags);
    case Parameters.QM_TYPE_NCI_PRO:
      return q.setupCalculation(volumeData, bsMySelected, params.bsSolvent,
          atomData.bsMolecules, null, atomData.atomXyz, atomData.firstAtomIndex, null, null,
          null, null, null, null, params.isSquaredLinear, null,
          null, true, points, params.parameters, params.testFlags);
    }
    return false;
  }
  
  @Override
  protected float getSurfacePointAndFraction(float cutoff,
                                             boolean isCutoffAbsolute,
                                             float valueA, float valueB,
                                             P3 pointA,
                                             V3 edgeVector, int x, int y,
                                             int z, int vA, int vB,
                                             float[] fReturn, P3 ptReturn) {
      float zero = getSPF(cutoff, isCutoffAbsolute, valueA,
          valueB, pointA, edgeVector, x, y, z, vA, vB, fReturn, ptReturn);
      if (q != null && !Float.isNaN(zero)) {
      zero = q.process(ptReturn);
      if (params.isSquared)
        zero *= zero;
      }
      return zero;
  }
}
