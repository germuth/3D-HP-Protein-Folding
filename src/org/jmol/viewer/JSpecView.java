package org.jmol.viewer;

import java.util.Hashtable;


import org.jmol.api.JmolJSpecView;
import org.jmol.modelset.Atom;
import org.jmol.util.BS;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.TextFormat;

public class JSpecView implements JmolJSpecView {

  private Viewer viewer;
  public void setViewer(Viewer viewer) {
    this.viewer = viewer;
  }
  
  public void atomPicked(int atomIndex) {
    String peak = getPeakAtomRecord(atomIndex);
    if (peak != null)
      sendJSpecView(peak + " src=\"JmolAtomSelect\"");
  }
  
  @SuppressWarnings("unchecked")
  public String getPeakAtomRecord(int atomIndex) {
    Atom[] atoms = viewer.modelSet.atoms;
    int iModel = atoms[atomIndex].modelIndex;
    String type = null;
    switch (atoms[atomIndex].getElementNumber()) {
    case 1:
      type = "1HNMR";
      break;
    case 6:
      type = "13CNMR";
      break;
    default:
      return null;
    }
    JmolList<String> peaks = (JmolList<String>) viewer.getModelAuxiliaryInfoValue(iModel,
        "jdxAtomSelect_" + type);
    if (peaks == null)
      return null;
    //viewer.modelSet.htPeaks = null;
    //if (viewer.modelSet.htPeaks == null)
    viewer.modelSet.htPeaks = new Hashtable<String, BS>();
    Hashtable<String, BS> htPeaks = viewer.modelSet.htPeaks;
    for (int i = 0; i < peaks.size(); i++) {
      String peak = peaks.get(i);
      BS bsPeak = htPeaks.get(peak);
      if (bsPeak == null) {
        htPeaks.put(peak, bsPeak = new BS());
        String satoms = Parser.getQuotedAttribute(peak, "atoms");
        String select = Parser.getQuotedAttribute(peak, "select");
        String script = "";
        if (satoms != null)
          script += "visible & (atomno="
              + TextFormat.simpleReplace(satoms, ",", " or atomno=") + ")";
        else if (select != null)
          script += "visible & (" + select + ")";
        bsPeak.or(viewer.getAtomBitSet(script));
      }
      if (bsPeak.get(atomIndex))
        return peak;
    }
    return null;
  }


  private void sendJSpecView(String peak) {
    String msg = Parser.getQuotedAttribute(peak, "title");
    if (msg != null)
      viewer.scriptEcho(Logger.debugging ? peak : msg);
    peak = viewer.fullName + "JSpecView: " + peak;
    Logger.info("Jmol>JSV " + peak);
    viewer.statusManager.syncSend(peak, ">", 0);
  }

  public void setModel(int modelIndex) {
    int syncMode = ("sync on".equals(viewer.modelSet
        .getModelSetAuxiliaryInfoValue("jmolscript")) ? StatusManager.SYNC_DRIVER
        : viewer.statusManager.getSyncMode());
    if (syncMode != StatusManager.SYNC_DRIVER)
      return;
    String peak = (String) viewer.getModelAuxiliaryInfoValue(modelIndex, "jdxModelSelect");
    // problem is that SECOND load in jmol will not load new model in JSpecView
    if (peak != null)
      sendJSpecView(peak);
  }

  public int getBaseModelIndex(int modelIndex) {
    String baseModel = (String) viewer.getModelAuxiliaryInfoValue(modelIndex,
        "jdxBaseModel");
    if (baseModel != null)
      for (int i = viewer.getModelCount(); --i >= 0;)
        if (baseModel
            .equals(viewer.getModelAuxiliaryInfoValue(i, "jdxModelID")))
          return i;
    return modelIndex;
  }


}
