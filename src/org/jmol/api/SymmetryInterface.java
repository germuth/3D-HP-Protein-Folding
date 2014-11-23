package org.jmol.api;

import java.util.Map;


import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.util.BS;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.Quadric;
import org.jmol.util.Tuple3f;
import org.jmol.util.V3;

public interface SymmetryInterface {

  public SymmetryInterface setPointGroup(
                                     SymmetryInterface pointGroupPrevious,
                                     Atom[] atomset, BS bsAtoms,
                                     boolean haveVibration,
                                     float distanceTolerance,
                                     float linearTolerance);

  public String getPointGroupName();

  public Object getPointGroupInfo(int modelIndex, boolean asDraw,
                                           boolean asInfo, String type,
                                           int index, float scale);

  public void setSpaceGroup(boolean doNormalize);

  public int addSpaceGroupOperation(String xyz, int opId);

  /**
   * set symmetry lattice type using Hall rotations
   * 
   * @param latt SHELX index or character lattice character P I R F A B C S T or \0
   * 
   */
  public void setLattice(int latt);

  public String getSpaceGroupName();

  public Object getSpaceGroup();

  public void setSpaceGroupS(SymmetryInterface symmetry);

  public boolean createSpaceGroup(int desiredSpaceGroupIndex,
                                           String name,
                                           float[] notionalUnitCell);

  public boolean haveSpaceGroup();

  public String getSpaceGroupInfo(String name, SymmetryInterface cellInfo);

  public Object getLatticeDesignation();

  public void setFinalOperations(P3[] atoms, int iAtomFirst,
                                          int noSymmetryCount,
                                          boolean doNormalize);

  public int getSpaceGroupOperationCount();

  public Matrix4f getSpaceGroupOperation(int i);

  public String getSpaceGroupXyz(int i, boolean doNormalize);

  public void newSpaceGroupPoint(int i, P3 atom1, P3 atom2,
                                          int transX, int transY, int transZ);

  public V3[] rotateEllipsoid(int i, P3 ptTemp,
                                         V3[] axes, P3 ptTemp1,
                                         P3 ptTemp2);

  public void setUnitCellAllFractionalRelative(boolean TF);
  
  public void setUnitCell(float[] notionalUnitCell);

  public void toCartesian(P3 pt, boolean asAbsolue);

  public Quadric getEllipsoid(float[] parBorU);

  public P3 ijkToPoint3f(int nnn);

  public void toFractional(P3 pt, boolean isAbsolute);

  public P3[] getUnitCellVertices();

  public P3[] getCanonicalCopy(float scale);

  public P3 getCartesianOffset();

  public float[] getNotionalUnitCell();

  public float[] getUnitCellAsArray(boolean vectorsOnly);

  public void toUnitCell(P3 pt, P3 offset);

  public void setOffsetPt(P3 pt);

  public void setOffset(int nnn);

  public P3 getUnitCellMultiplier();

  public float getUnitCellInfoType(int infoType);

  public boolean getCoordinatesAreFractional();

  public int[] getCellRange();

  public String getSymmetryInfoString();

  public String[] getSymmetryOperations();

  public boolean haveUnitCell();

  public String getUnitCellInfo();

  public boolean isPeriodic();

  public void setSymmetryInfo(int modelIndex, Map<String, Object> modelAuxiliaryInfo);

  public Object[] getSymmetryOperationDescription(int iSym,
                                                         SymmetryInterface cellInfo, 
                                                         P3 pt1, P3 pt2, String id);

  public boolean isPolymer();

  public boolean isSlab();

  public void addSpaceGroupOperationM(Matrix4f mat);

  public void setMinMaxLatticeParameters(P3i minXYZ, P3i maxXYZ);

  public void setUnitCellOrientation(Matrix3f matUnitCellOrientation);

  public String getMatrixFromString(String xyz, float[] temp, boolean allowScaling);

  public boolean checkDistance(P3 f1, P3 f2, float distance, 
                                        float dx, int iRange, int jRange, int kRange, P3 ptOffset);

  public P3 getFractionalOffset();

  public String fcoord(Tuple3f p);

  public void setCartesianOffset(Tuple3f origin);

  public P3[] getUnitCellVectors();

  public SymmetryInterface getUnitCell(Tuple3f[] points);

  public P3 toSupercell(P3 fpt);

  public boolean isSupercell();

  public String getSymmetryOperationInfo(Map<String, Object> sginfo, int symOp, String drawID, boolean labelOnly);

  public Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, int modelIndex,
                                               String spaceGroup, int symOp,
                                               P3 pt1, P3 pt2,
                                               String drawID);

  public Object getSymmetryInfo(ModelSet modelSet, int iModel, int iAtom, SymmetryInterface uc, String xyz, int op,
                                P3 pt, P3 pt2, String id, int type);

  public void setCentroid(ModelSet modelSet, int iAtom0, int iAtom1,
                          int[] minmax);

}
