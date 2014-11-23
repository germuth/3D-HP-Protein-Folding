package org.jmol.api;

import org.jmol.viewer.Viewer;

public interface JmolJSpecView {

  void setViewer(Viewer viewer);
  
  void atomPicked(int atomIndex);

  void setModel(int modelIndex);

  int getBaseModelIndex(int modelIndex);

}
