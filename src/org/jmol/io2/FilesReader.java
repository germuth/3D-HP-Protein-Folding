package org.jmol.io2;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.jmol.api.Interface;
import org.jmol.api.JmolDocument;
import org.jmol.api.JmolFilesReaderInterface;
import org.jmol.io.DataReader;
import org.jmol.io.JmolBinary;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.Viewer;

/**
 * open a set of models residing in different files
 * 
 */
public class FilesReader implements JmolFilesReaderInterface {
  /**
   * 
   */
  private FileManager fm;
  private Viewer viewer;
  private String[] fullPathNamesIn;
  private String[] namesAsGivenIn;
  private String[] fileTypesIn;
  private Object atomSetCollection;
  private DataReader[] dataReaders;
  private Map<String, Object> htParams;
  private boolean isAppend;

  public FilesReader() {
  }

  public void set(FileManager fileManager, Viewer viewer, String[] name,
                  String[] nameAsGiven, String[] types, DataReader[] readers,
                  Map<String, Object> htParams, boolean isAppend) {
    fm = fileManager;
    this.viewer = viewer;
    fullPathNamesIn = name;
    namesAsGivenIn = nameAsGiven;
    fileTypesIn = types;
    dataReaders = readers;
    this.htParams = htParams;
    this.isAppend = isAppend;
  }

  public void run() {

    if (!isAppend && viewer.displayLoadErrors)
      viewer.zap(false, true, false);

    boolean getReadersOnly = !viewer.displayLoadErrors;
    atomSetCollection = viewer.getModelAdapter().getAtomSetCollectionReaders(
        this, fullPathNamesIn, fileTypesIn, htParams, getReadersOnly);
    dataReaders = null;
    if (getReadersOnly && !(atomSetCollection instanceof String)) {
      atomSetCollection = viewer.getModelAdapter().getAtomSetCollectionFromSet(
          atomSetCollection, null, htParams);
    }
    if (atomSetCollection instanceof String) {
      Logger.error("file ERROR: " + atomSetCollection);
      return;
    }
    if (!isAppend && !viewer.displayLoadErrors)
      viewer.zap(false, true, false);

    fm.fullPathName = fm.fileName = fm.nameAsGiven = (dataReaders == null ? "file[]"
        : "String[]");
  }

  /**
   * called by SmartJmolAdapter to request another buffered reader or binary
   * document, rather than opening all the readers at once.
   * 
   * @param i
   *        the reader index
   * @param isBinary
   * @return a BufferedReader or null in the case of an error
   * 
   */
  public Object getBufferedReaderOrBinaryDocument(int i, boolean isBinary) {
    if (dataReaders != null)
      return (isBinary ? null : dataReaders[i].getBufferedReader()); // no binary strings
    String name = fullPathNamesIn[i];
    String[] subFileList = null;
    htParams.remove("subFileList");
    if (name.indexOf("|") >= 0) {
      subFileList = TextFormat.splitChars(name, "|");
      name = subFileList[0];
    }
    Object t = fm.getUnzippedBufferedReaderOrErrorMessageFromName(name, null,
        true, isBinary, false, true);
    if (t instanceof ZipInputStream) {
      if (subFileList != null)
        htParams.put("subFileList", subFileList);
      String[] zipDirectory = fm.getZipDirectory(name, true);
      t = fm.getBufferedInputStreamOrErrorMessageFromName(name,
          fullPathNamesIn[i], false, false, null, false);
      t = JmolBinary.getAtomSetCollectionOrBufferedReaderFromZip(viewer.getModelAdapter(),
          (BufferedInputStream) t, name, zipDirectory, htParams, true, isBinary);
    }
    if (t instanceof BufferedInputStream) {
      JmolDocument jd = (JmolDocument) Interface
          .getOptionInterface("io2.BinaryDocument");
      jd.setStream((BufferedInputStream) t, true);
      return jd;
    }
    return (t instanceof BufferedReader || t instanceof JmolDocument ? t :
      t == null ? "error opening:" + namesAsGivenIn[i] : (String) t);
  }

  public Object getAtomSetCollection() {
    return atomSetCollection;
  }

}
