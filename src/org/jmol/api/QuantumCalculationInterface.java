package org.jmol.api;





import org.jmol.util.BS;
import org.jmol.util.JmolList;
import org.jmol.util.P3;


public interface QuantumCalculationInterface {

  public abstract boolean setupCalculation(VolumeDataInterface volumeData, BS bsSelected,
                                 BS bsExclude,
                                 BS[] bsMolecules,
                                 String calculationType, P3[] atomCoordAngstroms,
                                 int firstAtomOffset, JmolList<int[]> shells,
                                 float[][] gaussians,
                                 int[][] dfCoefMaps, 
                                 Object slaters, float[] moCoefficients,
                                 float[] linearCombination, boolean isSquaredLinear, float[][] coefs, float[] partialCharges, boolean doNormalize, P3[] points, float[] parameters, int testFlags);
  
  public abstract void createCube();
  public abstract float process(P3 pt);
}
