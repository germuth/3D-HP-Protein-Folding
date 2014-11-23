package org.jmol.api;


import java.util.Map;

import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.util.BS;
import org.jmol.util.JmolList;
import org.jmol.util.P3;
import org.jmol.viewer.Viewer;

public interface JmolPropertyManager {

  void setViewer(Viewer viewer);

  Object getProperty(String returnType, String infoType, Object paramInfo);

  String getDefaultPropertyParam(int propertyID);

  int getPropertyNumber(String name);

  boolean checkPropertyParameter(String name);

  Object extractProperty(Object property, SV[] args, int pt);

  JmolList<Map<String, Object>> getMoleculeInfo(ModelSet modelSet,
                                            Object atomExpression);

  Map<String, Object> getModelInfo(Object atomExpression);

  Map<String, Object> getLigandInfo(Object atomExpression);

  Object getSymmetryInfo(BS bsAtoms, String xyz, int op, P3 pt,
                         P3 pt2, String id, int type);

  String getModelFileInfo(BS visibleFramesBitSet);

  String getChimeInfo(int tok, BS selectionSet);

  Map<String, JmolList<Map<String, Object>>> getAllChainInfo(BS atomBitSet);

  JmolList<Map<String, Object>> getAllAtomInfo(BS atomBitSet);

  JmolList<Map<String, Object>> getAllBondInfo(BS atomBitSet);

  void getAtomIdentityInfo(int atomIndex, Map<String, Object> info);

  String getModelExtract(BS atomBitSet, boolean doTransform, boolean isModelKit,
                         String type);

}
