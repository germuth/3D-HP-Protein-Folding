package org.jmol.api;

import java.util.Map;

import org.jmol.io.DataReader;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

public interface JmolFilesReaderInterface extends Runnable {
  Object getBufferedReaderOrBinaryDocument(int i, boolean isBinary);
  
  public Object getAtomSetCollection();

  public void set(FileManager fileManager, Viewer viewer, String[] fullPathNames,
           String[] namesAsGiven, String[] fileTypes, DataReader[] readers,
           Map<String, Object> htParams, boolean isAppend);

}